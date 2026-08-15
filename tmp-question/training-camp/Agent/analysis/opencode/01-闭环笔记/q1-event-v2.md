# q1 — EventV2 事件溯源(深度版:定义层 + 提交协议 + 重放 + owner)

> 域:④知识库(理论基础,被全域依赖的共享包) | 文件:core/src/event.ts(638)+ schema/src/event.ts(125)+ schema/src/session-event.ts(521)+ event/sql.ts(25)+ core/test/event.test.ts(1124 行,44 契约)+ opencode/src/event-v2-bridge.ts(71)
> review 轮次:2 轮(源码全文 + 测试契约 + 事件清单 + V1 桥接)

---

## 假设

EventV2 是"事件溯源基础设施"而非"会话事件系统":定义层(define/latest/durable)、提交协议(投影器+commit+落库同事务)、订阅三层(typed/all/durable tail)、重放协议(幂等/分叉检测/owner 栅栏)。会话事件只是它的一个消费者。

## 验证

### 1. 定义层三件套(schema/src/event.ts)

**define**(event.ts:42-70):type + 可选 durable{version, aggregate} + schema(Struct)→ 返回 Effect Schema.Struct(statics 挂 type/durable/data)。

**latest**(event.ts:76-92):同 type 多版本定义 → 选版本最大的;**同 type 同 version 定义不同 → throw "Duplicate latest event definition"**。测试证明与声明顺序无关(event.test.ts:126-142)。

**durable**(event.ts:98-108):`versionedType(type, version)` → definition 只读 Map("session.next.prompted.1")。重复 key throw。

**versionedType**(event.ts:94-96):`${type}.${version}` —— 落库 type 带版本号(event.test.ts:398)。

**设计要点**:事件 = 版本化契约;演进 = 新版本定义,重放按版本解码。产品④知识库 schema 演进的答案。

### 2. 提交协议(设计 1:事务内顺序 + 全回滚)

core/src/event.ts:236-352 commitDurableEvent:

```
事务内顺序(event.test.ts:188 断言 ["projector", "commit:0"]):
1. 读 event_sequence 当前 seq(latest)
2. 校验 seq 连续性:input.seq 必须 = latest+1,否则 die(event.ts:294-302)
3. 校验 event.id 全局唯一,否则 die(event.ts:303-315)
4. 执行投影器(projectors[type])(event.ts:320-322)
5. 执行调用方 commit 回调(event.ts:323)
6. upsert event_sequence + insert event(event.ts:324-348)
```

**回滚契约**(event.test.ts:192-214):commit 回调失败 → 事件行 + 投影器副作用**全部回滚**。

**失败语义**:所有校验失败都是 die(缺陷)——调用方违反协议,不能恢复。

### 3. 通知顺序与错误策略(设计 2:commit → notify)

```
event.ts:369-395 publishEvent:
durable 提交成功 → notify(isolateListeners=true)   ← listener 缺陷隔离(只 logError)
非 durable(live-only)→ notify(false)               ← listener 缺陷 fail-fast
```

- 顺序:projectors → listeners → typed pubsub → all pubsub(event.ts:406-417,测试 11)
- durable 后 listener 缺陷隔离(event.test.ts:271-289,publish 仍成功)
- live-only listener 缺陷 fail-fast(event.test.ts:373-381)
- observer 中断 → publish 失败但**事件已提交**(event.test.ts:354-371)

**设计要点**:错误策略三分——投影器/commit 失败=不落库(强一致);durable 后 listener 失败=隔离(真相已存);live-only listener 失败=传播(无持久真相)。

### 4. durable tail 无窗口订阅(设计 3:先订阅 wake 再读历史)

event.ts:565-604:
```
1. subscribeDurable:PubSub.sliding<void>(1) 注册(合并唤醒;acquireRelease 关闭时移除)
2. 读历史 readAfter(after)(event.ts:541-563,beforeAggregateRead 测试钩子)
3. live = 每次 wake 后重查 DB(Stream.mapEffect(() => read))
4. concat(historical, live)
```

**无窗口证明**(event.test.ts:458-485):历史读取暂停期间 commit 的事件 → wake 保留,恢复后拿到。
**顺序证明**(event.test.ts:442-456):先订阅后读 → handoff 期间发布不丢。
**合并证明**(event.test.ts:487-505):64 事件连续发布 → 一次 drain 全收。
**过滤证明**(event.test.ts:507-519):live-only 事件不出现在 durable 流。

**设计要点**:live 只是信号,DB 是真相;每次唤醒重查,重连不丢。sliding-1 是忙碌期背压。

### 5. 重放协议(设计 4:四级校验)

| 场景 | 判定 | 结果 |
|------|------|------|
| seq ≤ latest 且内容全同 | event.ts:262-283 | 幂等无操作(测试 39) |
| seq ≤ latest 且内容不同 | event.ts:284-289 | die "Replay diverged"(测试 40,不发布) |
| seq > latest 且 ≠ latest+1 | event.ts:294-302 | die "Sequence mismatch"(测试 28) |
| 信封 ≠ payload aggregate | event.ts:228-235 | die "Aggregate mismatch"(测试 27,不污染 payload) |

**replayAll**(event.ts:480-512):同 aggregate + 连续 seq 校验 + 分批续接(测试 31/32)。
**版本解码**(event.test.ts:660-681):replay 先 decode 再投影。

### 6. owner 栅栏(设计 5:三级)

| 机制 | 行为 | 证据 |
|------|------|------|
| claim(aggregateID, ownerID) | 显式更新 owner(event.ts:525-532) | 测试 43:可覆盖 |
| replay(ownerID) 非 strict | 无主 seq 认领;已有 owner 且不同 → 静默跳过 | 测试 33/42 |
| replay(strictOwner=true) | owner 冲突 → die "Replay owner mismatch" | 测试 34/38/35 |

**精妙**(测试 35/37):exact replay 认领无主聚合;本地 publish 后 owner-1 认领成功,owner-2 被栅栏。

### 7. SQL 层(event/sql.ts)

```sql
event_sequence: aggregate_id TEXT PK, seq INTEGER, owner_id TEXT
event: id TEXT PK, aggregate_id FK→event_sequence(cascade), seq, type(versioned), data(json)
索引:uniqueIndex(event_aggregate_seq_idx)   ← seq 唯一性硬约束
     index(event_aggregate_type_seq_idx)    ← 按 type 重放
remove(event.ts:514-523):事务删 sequence+事件 → 聚合可从头重放(测试 44)
```

### 8. durable/live-only 边界(session-event.ts:448-512)

**28 个 durable**:AgentSwitched/ModelSwitched/Moved/Prompted/PromptAdmitted/ContextUpdated/Synthetic/Shell.{Started,Ended}/Step.{Started,Ended,Failed}/Text.{Started,Ended}/Tool.Input.{Started,Ended}/Tool.{Called,Progress,Success,Failed}/Reasoning.{Started,Ended}/Retried/Compaction.{Started,Ended}/Revert.{Staged,Cleared,Committed}

**4 个 live-only**:Text.Delta/Reasoning.Delta/Tool.Input.Delta/Compaction.Delta

**注释即设计**(session-event.ts:209/247/291):"Stream fragments are live-only; Ended is the replayable full-value boundary"。Progress:"checkpoint semantic transitions or at a bounded cadence, not persist every stdout/stderr chunk"。

**模式**:Text/Reasoning/Tool.Input 都是 Started(durable)→ Delta(live-only)→ Ended(durable)。

### 9. 版本演进实证(session-event.ts:44-49)

Step.Ended/Step.Failed 用 stepSettlementOptions(version 2,带 cost/tokens/snapshot/files);其余 26 个 version 1。

### 10. V1 桥接(event-v2-bridge.ts:35-62)

```
listen 所有事件 → GlobalBus.emit("event", {directory, project, workspace, payload:{id, type, properties}})(V1 bus 兼容)
durable 事件 → 再 emit "sync" 事件(syncEvent: id/type/versionedType/seq/aggregateID/data)
publish 覆盖:无显式 location → 从 InstanceRef/WorkspaceRef 附路由实例 location
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 定义层三件套 + versionedType | schema/event.ts:42-108 | ④知识库事件 schema 演进 |
| 2 | 提交协议:投影器→commit→落库同事务,失败全回滚 | event.ts:236-352 | ④原子性(重放即真相) |
| 3 | 通知策略三分 | event.ts:398-417 | ④消费失败语义 |
| 4 | durable tail 无窗口订阅 | event.ts:565-604 | ④跨 session 续接 |
| 5 | 重放四级校验 | event.ts:228-302 | ④确定性重放 |
| 6 | owner 三级栅栏 | event.ts:525-532 | ④多节点扩展点 |
| 7 | SQL 层 + 唯一索引 + cascade remove | event/sql.ts | ④存储 |
| 8 | durable/live-only 边界(28+4) | session-event.ts:448-512 | ④哪些状态值得重放 |
| 9 | 事件族模式 Started→Delta→Ended | session-event.ts:209-291 | ④流式增量 vs 完整值持久 |
| 10 | 版本演进实证(Step.Ended v2) | session-event.ts:44-49 | ④schema 演进 |
| 11 | V1 桥接双通道 | event-v2-bridge.ts:35-62 | ④迁移兼容层 |

## 面试弹药

- "事件 = 版本化契约":type.version 落库,重放按版本解码
- "投影器与事件同事务":commit 失败 → 事件+投影全回滚(测试证明,硬保证)
- "错误策略三分":按"有没有持久真相"分类
- "无窗口 tail":先订阅后读 + 重查 DB + sliding-1
- "重放 = 确定性协议":diverge 报错不覆盖
- "Started→Delta→Ended":重放只消费定稿,不消费过程
- "桥接 = 发布边界转换":V1 bus/sync 兼容在 Bridge 层,事件本身不动
