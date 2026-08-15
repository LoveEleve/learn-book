# q10 — SessionV2 Facade + Store + History(深度版:操作面 + 历史过滤规则)

> 域:②执行(入口)+ ④知识库(读取) | 文件:core/src/session.ts(486)+ session/(store.ts 63/history.ts 101/sql.ts 176/revert.ts 121/info.ts 50)+ core/test/(session-create 143/session-history/move-session).test.ts
> review 轮次:2 轮(源码全文 + 测试抽查)

---

## 假设

SessionV2 Facade 是全部 V2 操作的唯一入口(create/list/messages/prompt/resume/interrupt/revert/switch)。历史加载有**确定性过滤规则**(compaction 之后 + baseline 之后的 system 更新),保证 runner 与用户看到的上下文一致。

## 验证

### 1. 操作面(设计 1:18 个操作)

```ts
// session.ts:113-180 Interface
list(workspace/search/limit/order/anchor 分页) / create(id 幂等) / get / messages(游标) / message / context
events(durable stream) / history(有限分页) / switchAgent / switchModel / prompt(wake) / shell*
skill* / compact* / wait* / active / resume / interrupt / revert{stage, clear, commit}
(* = OperationUnavailableError,session.ts:95-100,388-424)
```

**create 幂等**(session.ts:208-262):同 ID 已存在 → 返回现有;并发创建投影竞态 → SessionAlreadyProjected catch → 重读,现有身份胜(session-create.test.ts)。

**prompt**(session.ts:360-386):admit → equivalent 校验 → resume≠false 时 execution.wake。uninterruptible(admit 不能被打断)。

**list 游标**(session.ts:268-303):anchor{time, id} 双向分页(previous 反转 order);搜索 = title LIKE。

### 2. 历史过滤规则(设计 2:compaction + baseline 双重过滤)

```ts
// history.ts:24-53 messageRows
有 compaction → gte(seq, compaction.seq) OR (system 类型 AND gt(seq, baselineSeq))
有 baselineSeq → 排除 system 类型(seq <= baselineSeq)
// 语义:
//   - compaction 之前的消息被压缩替换(不加载)
//   - baseline 之前的 system 消息 = Context Epoch 基线的一部分(不重复进历史)
//   - baseline 之后的 system = 时间序更新(保留)
```

**两条读取路径**:
- `load`(store.context):用户可见上下文
- `loadForRunner`(store.runnerContext, baselineSeq):runner 的投影历史(runner 用它组装请求 + 压缩预估)

### 3. 分页(设计 3:消息游标按 seq)

```ts
// session.ts:304-337 messages
cursor{id, direction} → 解析 anchor 的 seq → 按 seq 边界过滤(不信任调用方时间戳)
previous → 反转结果
// events(346-351):durable stream,按 aggregateID 过滤 durable
// history(352-359):readAggregate 有限分页(after + limit,hasMore)
```

### 4. 消息表(session/sql.ts:119-138)

```sql
session_message: id PK, session_id FK, type, seq, time_created/updated, data(json)
索引:UNIQUE(session_id, seq)  ← seq 唯一硬约束(投影顺序=事件顺序的存储保证)
     (session_id, type, seq)  ← 历史过滤查询
     (session_id, time_created, id)  ← 分页
```

### 5. Revert(设计 4:stage/clear/commit)

```ts
// session.ts:433-453 + revert.ts(121 行)
stage: { session, messageID, files? } → Revert.State(Snapshot 驱动)
clear: 清除回滚状态
commit: 应用回滚(事件驱动:revert.staged/cleared/committed 投影为状态)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 18 操作 Facade + 未实现操作显式 OperationUnavailableError | session.ts:113-180 | ②操作契约 |
| 2 | 历史过滤规则(compaction + baseline 双重) | history.ts:24-53 | ④上下文视图一致性 |
| 3 | 消息分页按 seq 游标 | session.ts:304-337 | ④分页确定性 |
| 4 | session_message 唯一(seq)索引 | session/sql.ts:133 | ④存储硬约束 |
| 5 | revert 三操作(Snapshot 驱动) | session.ts:433-453 | ②回滚 |

## 面试弹药

- "过滤规则 = 视图定义":compaction 后 + baseline 后的 system 更新——runner 与用户看到的上下文由同一规则导出,不会漂移
- "seq 唯一索引":投影顺序 = 事件顺序在存储层是硬约束(UNIQUE(session_id, seq))
- "未实现操作显式报错":shell/skill/compact/wait 返回 OperationUnavailableError——契约先行,实现后补
- "游标双向分页":previous 反转 order + seq 锚点——翻页稳定(新消息插入不破坏)

## 待深挖

- [ ] move-session.test.ts:搬家语义(epoch reset + Location 变更)
- [ ] revert.ts 的 Snapshot diff 细节
- [ ] SessionMessage 类型联合(message.ts)
