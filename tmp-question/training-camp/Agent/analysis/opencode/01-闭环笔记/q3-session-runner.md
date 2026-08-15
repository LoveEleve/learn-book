# q3 — SessionRunner 执行引擎(深度版:双层循环 + 事件发布管线 + 44 测试契约)

> 域:②执行引擎 | 文件:core/src/session/runner/(llm.ts 432/publish-llm-event.ts 423/model.ts 218/to-llm-message.ts 171/max-steps.ts 16/index.ts)+ core/test/session-runner.test.ts(3426 行,44 契约)+ session-runner-recorded(193)+ session-runner-tool-events(136)+ session-runner-tool-registry(452)+ session-runner-message(501)+ session-runner-model(347)
> review 轮次:2 轮(源码全文 + 全部测试契约)

---

## 假设

V2 SessionRunner 是"事件溯源执行引擎":循环状态在 DB(收件箱),不是内存;单个 provider turn 是严格 7 步;失败/中断/压缩都有确定性的持久化后果。测试契约(44 个)揭示了代码注释没写的极端行为(中断时工具失败化、压缩边界、step 配额)。

## 验证

### 1. 双层循环(设计 1:shouldRun/needsContinuation)

```ts
// llm.ts:383-406
while (shouldRun) {                 // 外层:queue 驱动(会话级)
  let needsContinuation = true
  let step = 1
  while (needsContinuation) {       // 内层:provider turn 驱动(步骤级)
    const result = yield* runTurn(input.sessionID, promotion, step)
    needsContinuation = result.needsContinuation
    step = result.step + 1
    promotion = "steer"
    if (!needsContinuation) needsContinuation = hasPending(db, sessionID, "steer")
  }
  shouldRun = hasPending(db, sessionID, "queue")
}
```

**测试证据**:
- steer 插入续跑:测试 1872 "steers an active provider turn"、2190 "coalesces multiple active steering prompts into one continuation turn"
- queue 在续跑结束后才提升:测试 1915 "promotes queued input after continuation ends"、2048 "promotes queued inputs one at a time in FIFO"
- steer 优先于 queue:测试 2125
- 中断后保留 durable 输入:测试 1962/2005 "preserves durable queued/steering input for a later wake/resume after interruption"
- 并发 resume 合并:测试 1833 "joins concurrent resume calls into one active provider run"

### 2. runTurn 7 步(设计 2:顺序 + 每步的持久化后果)

```
1. location 校验(llm.ts:180-181):session.location ≠ 当前 → interrupt(测试 690:搬家后旧 runner 中断)
2. agent 选择(llm.ts:182):provider-turn 作用域(测试 888:观察期间 agent 切换不影响本次 turn)
3. epoch initialize/prepare(llm.ts:183,197-198)
4. promotion(llm.ts:187-196):promoted > 0 → step 重置为 1
5. request 组装(llm.ts:205-214):system=[agent.system, baseline] + 权限过滤工具 + max-steps 禁工具
6. llm.stream + 增量持久化(llm.ts:232-275):publish-llm-event 管线
7. 结算(llm.ts:277-347):await 工具 fibers + Step.Ended + 兜底
```

### 3. 事件发布管线(设计 3:publish-llm-event 状态机)

publish-llm-event.ts:239-409 的 switch(12 种 LLMEvent → SessionEvent):

**工具生命周期 5 状态**(publish-llm-event.ts:55-66):
```
inputEnded / called / settled / providerExecuted / providerMetadata
```

**严格协议(全 die)**:Duplicate start / delta before start / name changed / input after end / Duplicate call / result before call / Duplicate result / result name changed / end before start——工具事件顺序是硬契约。

**fragments 缓冲**(publish-llm-event.ts:91-119):text/reasoning/toolInput 的 start→append→end;end 合并 chunks 后发 durable Ended 事件;flush 在 step-finish 时强制 end 所有未结束片段。

**tool-result 语义**(publish-llm-event.ts:337-375):
- 未 called → die;名称变化 → die;重复结算:error 静默 / success die
- providerExecuted 时把 result 附到事件(specs:provider 执行的工具结果需 exact round-trip)

**测试证据**(session-runner-tool-events.test.ts 136 行):tool 事件流完整性。

### 4. 压缩迁移(设计 4:TurnTransitionError 异常转移)

```ts
// llm.ts:152-166,369-381
ContinueAfterCompaction / ContinueAfterOverflowCompaction
// catchDefect 捕获 → 重新 runTurn(step 不变)
```

**测试证据**:
- 自动压缩:1085 "automatically compacts into a completed summary and retained recent turn"(压缩消息 = compaction type,后续 summary 带 prior-summary)
- 压缩边界:1148 "retains only complete serialized messages"(完整消息才序列化,不保留片段)
- 超大新消息:1180 "summarizes an oversized newest message"(recent-context 空)
- 溢出恢复:1209 "forces one compaction and retries after provider context overflow"(3 个请求:overflow→summary→final)
- 二次溢出:1238 "persists a second context overflow after one recovery"(第二次 overflow 成为终止失败)
- 原始溢出失败:1289 "publishes the original overflow when recovery summarization fails"
- 溢出恢复中断:1309 "interrupts overflow recovery while the summary provider is running"

### 5. 工具结算协议(设计 5:eager + 兜底矩阵)

**Eager 执行**(llm.ts:250-271):每个 tool-call 事件到达 → FiberSet.run 立即启动子 fiber → awaitToolFibers 等全部结算后才续跑。

**测试证据**:
- 1684 "starts recorded local tools eagerly and awaits settlement"(5 工具并行 maxActive=5,上下文里 state: running)
- 1745 "settles repeated provider-local tool call IDs against their owning assistant messages"(provider 的 call ID 跨 turn 重复 → 按 assistantMessageID 归属)
- 2579 "durably settles local tool failures before continuing"
- 2625 "returns unexpected local tool defects to the model and continues"("Tool execution failed: unexpected tool defect")
- 2673 "returns policy-blocked tools to the model and continues"
- 2722 "interrupts runner continuation when permission approval is declined"(DeclinedError → interrupt)
- 2766 "returns permission corrections to the model and continues"(CorrectedError 带反馈进上下文)
- 2815 "interrupts runner continuation when a question is dismissed"
- 2871 "awaits started local tools before surfacing provider stream failure"
- 2905 "durably fails blocked local tools when a provider turn is interrupted"("Tool execution interrupted" 持久化,重放后仍在)
- 2978 "durably fails blocked local tools when interrupted while awaiting settlement"

### 6. 崩溃恢复(设计 6:failInterruptedTools)

```ts
// llm.ts:119-139
run 开始 → 扫描历史 pending/running 工具 → 发布 Tool.Failed("Tool execution interrupted")
```

**测试证据**:
- 2259 "durably fails local tools left running by a prior process before continuing"
- 2319 "durably fails hosted tools left running by a prior process before continuing inline"
- 2379 "durably fails pending tool input left by a prior process before continuing"
- 2438 "retries inbox input after prompt projection rolls back"

### 7. max-steps(设计 7:step 配额)

```ts
// llm.ts:202-213
isLastStep = agent.info.steps 且 currentStep >= steps
最后一步:工具不 materialize + toolChoice "none" + MAX_STEPS_PROMPT 追加
```

**测试证据**:
- 3015 "forces a text response on an agent's configured final step"(第 2 请求 toolChoice none + tools=[] + "MAXIMUM STEPS REACHED" 文本)
- 3063 "resets the configured step allowance when steering input promotes"(steer 提升 → step 重置,配额恢复)

### 8. 其他关键契约

- 系统上下文不可用:658 "retries the first provider turn after system context becomes available"(InitializationBlocked → 收件箱保留 pending,无 epoch;恢复后同 messageID 重试)
- snapshot 解码失败:723 "fails gracefully"(优雅失败)
- 压缩重建基线:1046 "rebuilds the baseline directly after completed compaction"
- 模型切换保留基线:969 "keeps the baseline and chronological System updates after a model switch"
- provider error:3115 "projects provider errors as terminal assistant step failures"(finish: "error")
- 移出:690 "interrupts a source Location runner after a Session moves"(epoch 清除 + 收件箱保留)
- prompt cache key 限制:2518 "bounds 64-character session prompt cache keys"

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 双层循环(shouldRun/needsContinuation)+ 持久化收件箱驱动 | llm.ts:383-406 | ②执行引擎主干 |
| 2 | runTurn 7 步(location/agent/epoch/promotion/request/stream/settle) | llm.ts:173-348 | ②执行步骤定义 |
| 3 | 事件发布管线(12 事件映射 + 工具 5 状态机 + 严格协议) | publish-llm-event.ts:239-409 | ③验收(事件可审计) |
| 4 | 压缩 = 异常转移(一次溢出恢复,绝不循环) | llm.ts:152-166,369-381 | ②失败循环(D17) |
| 5 | Eager 工具结算 + 兜底矩阵(7 种失败场景) | llm.ts:250-271,295-345 | ②工具并行 + 失败语义 |
| 6 | 崩溃恢复:先失败化 running 工具 | llm.ts:119-139 | ②可重放性 |
| 7 | max-steps 禁工具 + step 重置 | llm.ts:202-213 | ②执行契约 |

## 面试弹药

- "循环状态在 DB 不在内存":hasPending 查收件箱决定是否续跑——崩溃后 resume 从 durable 状态继续,不是内存循环
- "异常转移实现压缩":压缩改变 request 内容,用 TurnTransitionError 把执行弹回 turn 开始,主循环零改动
- "工具事件是严格协议":顺序错乱(duplicate/delta-before-start/name-changed)全 die——事件溯源要求确定性
- "失败也要持久化":中断时未结算工具全部失败化("Tool execution interrupted"),重放后仍是 error——没有悬挂的 running
- "steer 重置 step 配额":新输入 = 新一轮执行,配额恢复(防 agent 用完 step 后无法响应用户)

## 待深挖

- [ ] to-llm-message.ts(171 行):V2 消息 → LLM 消息的翻译规则
- [ ] model.ts(218 行):SessionRunnerModel 的模型解析
- [ ] max-steps.ts 的 MAX_STEPS_PROMPT 内容
- [ ] session-runner-message.test.ts(501)的消息投影细节(q9 一起)
