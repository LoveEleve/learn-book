# q9 — SessionProjector + MessageUpdater(深度版:事件→消息投影规则表)

> 域:④知识库(投影)+ ②执行(状态) | 文件:core/src/session/projector.ts(458)+ message-updater.ts(397)+ core/test/session-projector.test.ts
> review 轮次:2 轮(源码全文 + 关键测试)

---

## 假设

投影 = 事件→可见消息的**纯规则表**(immer 不可变更新)。Assistant 消息是核心对象:step.started 创建、text/tool/reasoning 追加、step.ended/failed 定稿。工具状态机 pending→running→completed/error 由事件驱动。

## 验证

### 1. 投影器注册(设计 1:事件事务内执行)

```ts
// projector.ts:340-375:events.project(SessionEvent.X, (event) => { ... })
// 在 EventV2 commitDurableEvent 的事务内执行(q1 设计 1)
// 每个投影器先校验 event.durable ≠ undefined(没有 seq 的事件不投影)
```

**特殊投影器**:
- Prompted:SessionInput.projectPrompted(写收件箱 promoted_seq)+ run(db, event)(可见 user 消息)
- PromptAdmitted:SessionInput.projectAdmitted(写收件箱)
- ContextUpdated:SessionContextEpoch.advance(snapshot)(q5)+ run

### 2. run():消息 CRUD + usage 累加(设计 2)

```ts
// projector.ts:112-191 run:
adapter = {
  getCurrentAssistant():最新 assistant 且未 completed(desc seq LIMIT 1)
    —— 注释: "A newer turn supersedes stale incomplete rows; never resume an older assistant projection"
  getAssistant(messageID) / getCurrentShell(callID)
  updateAssistant / updateShell / appendMessage
}
SessionMessageUpdater.update(adapter, event)   ← 规则表
// applyUsage(projector.ts:90-110):step-finish 时 cost/tokens 累加到 session 表(sql 增量, sign 参数支持回滚?)
```

**insertMessage**(projector.ts:193-209):seq = event.durable.seq——**消息顺序 = 事件顺序**(不依赖消息自己的时间戳)。

### 3. Assistant 生命周期(设计 3:step.started → 内容 → step.ended/failed)

```ts
// message-updater.ts:186-229
step.started:完成上一个未完成 assistant + append 新 assistant(agent/model/snapshot.start)
step.ended:  time.completed + finish + cost + tokens + snapshot.end/files
step.failed: time.completed + finish: "error" + error
// text/tool/reasoning 事件:updateOwnedAssistant(messageID)(按 ID 精确更新,不推断)
```

**设计要点**:messageID 由事件携带(runner 的 publisher 生成),投影器不推断——"会话消息保留其来源聚合 seq"(specs/v2/session.md:175)。

### 4. 工具状态机(设计 4:pending→running→completed/error)

```ts
// message-updater.ts:249-342
tool.input.started → append tool{status: "pending", input: ""}
tool.input.ended   → pending → input = text
tool.called        → pending → running{input, structured: {}, content: []} + time.ran
tool.progress      → running → 更新 structured/content(有界 checkpoint)
tool.success       → running → completed{structured, content, outputPaths, result}
tool.failed        → pending|running → error{error, result}
```

**关键语义**:
- 只有 pending/running 才接受 failed(publish-llm-event 保证每个工具恰好一个终态)
- provider 元数据保留:executed OR 已有;resultMetadata 单列(settlement 侧 vs call 侧分离)
- progress 只更新 running(结算后 progress 忽略)

### 5. 文本/推理追加(设计 5:delta 累积 + ended 定稿)

```ts
// message-updater.ts:230-248,343-373
text.started → append{text: ""}
text.delta   → text += delta(内存投影)
text.ended   → text = 完整值(定稿,防 delta 缺失)
// reasoning 同模式 + providerMetadata 保留
```

**设计要点**:delta 是 live 事件(不投影),Ended 是 durable(投影定稿)——重放只看到 Started/Ended,状态由 Ended 全文重建。

### 6. 直通消息(设计 6:append 型)

```ts
// message-updater.ts:103-165,377-389
agent.switched / model.switched / prompted(user) / context.updated(system) / synthetic / shell.started / compaction.ended
→ 直接 append 新消息(不可变,无更新)
```

### 7. 忽略事件(设计 7:无投影副作用)

```ts
// message-updater.ts:125,139,264,374-376,390-392
moved / prompt.admitted / tool.input.delta / retried / compaction.started / compaction.delta / revert.*
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 投影器在事件事务内执行(原子) | projector.ts:340-375 | ④一致性 |
| 2 | run() 消息 CRUD + usage 累加 + seq=事件序 | projector.ts:112-209 | ④事件序=消息序 |
| 3 | Assistant 生命周期(step.started/ended/failed) | message-updater.ts:186-229 | ③消息状态机 |
| 4 | 工具状态机 pending→running→completed/error | message-updater.ts:249-342 | ③工具终态保证 |
| 5 | delta 累积 + ended 定稿 | message-updater.ts:230-248 | ④流式 vs 定稿 |
| 6 | 直通消息(switch/model/prompt/context/shell/compaction) | message-updater.ts:103-165 | ④消息模型 |
| 7 | 忽略事件清单(无投影副作用) | message-updater.ts:264-392 | ④投影最小化 |

## 面试弹药

- "消息顺序 = 事件顺序":insertMessage 用 event.durable.seq,不信任消息自带时间戳——重放和实时路径产出一致顺序
- "投影器不推断身份":messageID 全由事件携带(runner 生成)——"消息保留其来源聚合 seq"
- "新 turn 取代旧行":step.started 完成上一个未完成 assistant——崩溃残留的不完整行不会复活
- "delta live,Ended 定稿":重放只见 Started/Ended,Ended 全文重建——投影确定性
- "工具终态唯一":只有 pending/running 接受 failed;success/failed 后 progress 忽略

## 待深挖

- [ ] session-projector.test.ts 的边界契约
- [ ] SessionMessage 的 schema(消息类型联合,message.ts)
- [ ] applyUsage 的 sign 参数(回滚场景)
