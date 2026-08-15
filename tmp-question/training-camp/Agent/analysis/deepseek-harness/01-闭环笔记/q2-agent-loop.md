# q2 — Agent + AgentLoop(深度版:融合分派 + Turn/Step 状态机 + 测试契约)

> 域:②执行引擎(核心) | 文件:packages/core/agent/src/(index 529+/dispatch 176/types 27/inbox/runtime-types)+ agent-loop/src/(index 713/agent 496/tool-calls 289/constants)+ agent-loop/tests/loop.spec.ts(1465)+ docs/architecture.md(Turn flow)
> review 轮次:3 轮(源码全文核心 + 架构文档 + 测试契约 60+)

---

## 假设

Agent = 接口 + live 注册表 + agent/* 事件;AgentLoop = 默认驱动(ReactLoopAgent)。Turn(0+ steps)/Step(1 request + tools)双层;输入经 InboxTarget(next-turn/next-step)到达。融合分派器保证 subject 与 scope 不分歧。**测试契约(60+)揭示运行时上下文/steering/inject/max-tokens 语义**。

## 验证

### 1. 融合分派(设计 1:agentEvents 三模式)

```ts
// dispatch.ts:28-82
AgentSubjectEvent:类型级约束——payload 携带 agent 且 handler 的 this 是 Scoped<Agent>
agentEvents(ctx, agent, carrier):三种分派:
  emit: fire-and-forget,非 veto(同步 throw + promise rejection 各自隔离)
  serial: 有序等待(Cordis serial)
  waterfall: 中间件链(参数以 next 结尾;必须调用 next() 委托)
// emit 实现细节(dispatch.ts:120-137):Cordis emit 用 Array.map——一个同步 throw 饿死后续监听器
//   → 自己 resolve 回调集,两种失败独立 contain
// carrier 复用:热路径分派无分配(循环驱动在构造函数建一次)
// subject 注入:payload spread 在前——payload 自带 agent 字段也不能覆盖注入的 subject
```

### 2. Turn/Step 语义(设计 2:双层 + 闭合规则)

```ts
// architecture.md:65-90:
turn = 0+ steps;开于首输入被 claim;闭于"无事可做"
step = 1 model request + 它调用的工具
流程:turn/start → claim(next-step input + 1 queued message)
  → 组装 prompt sections + tool schemas
  → agent/pre-step(reject | enter)→ 拒绝或空 enter = 关 turn 且 0 step(日志记录尝试!)
  → step/start → 追加 user/message → 从日志 derive model history
  → agent/request → llm/stream → assistant/chunk* → assistant/message
  → tool/call* → tools/pre-execute → tools/execute → tools/post-execute → tool/result*
  → step/end → 工具欠请求或新输入 → claim → 下一步
  → agent/turn-stopping(串行)→ turn/end
// durable 事件:turn/*、step/*、user/message、assistant/*、tool/*
// live 扩展点:agent/pre-step、agent/request、llm/stream、tools/*(waterfall,必须 next)
```

### 3. ReactLoopAgent(设计 3:Phase 状态机)

```ts
// agent-loop/src/agent.ts:42-51
Phase = idle{lastTurn} | maintenance{abort, lastTurn, wakeRequested} | running{abort, turn, step, wakeRequested}
send(input, target, wake):target = next-turn | next-step
  - waking input 不能加入 aborted activity → 开新 turn
  - idle 时的 wake 总是开 turn 边界
run 循环:while(await this.turn()){}(convergence 时重放停止)
preStep:提案 PreparedStep(reject | enter{messages, assembly})
```

### 4. 测试契约(设计 4:运行时上下文/steering/inject/max-tokens)★ review 轮 3

```ts
// agent-loop/tests/loop.spec.ts(60+ 契约,关键):
1. 运行时上下文(357-504):变化材料化在历史尾不重写 system header;表面替换移除保留快照 → 重发;
   压缩后活动集空 → 清除;无关替换不清;畸形保留消息 → 替换为当前完整快照
2. steering(549-612):step 间注入继续 turn;空闲 steering 同步开始;pre-step 抛错 → 保留后续 steering 至唤醒
3. inject()(640-741):空闲 durable 暂存不开 turn;结构化内容 verbatim + durable 源;
   工具执行期间推迟至工具结果;非 JSON 上下文拒绝(不进 FIFO)
4. turn-stopping(766):/loop 模式——turn-stopping 可 steer 另一步
5. 工具 conclude turn(789-811):尽管欠 follow-up 请求也闭合;期间到达的 steering 继续
6. agent/request 模型切换(844):返回替换 config 切换模型;切换已记录
7. pre-step 时序(867-913):每提议步骤一次;先于边界与请求;抛错失败提议不失败循环
8. cancel/max-tokens(945-1187):cancel 中流 → turn-end reason aborted;max-tokens 截断 step →
   该 turn 结束 reason 为 max-tokens;先前 max-tokens 不跨 turn 泄漏;截断 step 不分派工具;
   空 usage 追加空完成锚;安全内容保留 + 截断工具调用丢弃
9. step/end 观察者失败隔离(1220):不改变 continuation
```

**设计要点**:运行时上下文 = "变化材料化 + 表面替换/压缩边界";inject 推迟到工具结果(边界);max-tokens 语义精确(截断 step 不调度工具 + 不跨 turn 泄漏)。

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 融合分派三模式(emit/serial/waterfall) | dispatch.ts:28-148 | ②扩展点类型安全 |
| 2 | Turn/Step 双层 + 0-step 闭合记录 | architecture.md:65-90 | ②执行主干 |
| 3 | Phase 状态机(idle/maintenance/running) | agent.ts:42-51 | ②执行状态 |
| 4 | 运行时上下文材料化(历史尾/替换/压缩边界) | loop.spec:357-504 | ②上下文管理 |
| 5 | inject 边界(空闲暂存/工具执行推迟) | loop.spec:640-741 | ①对齐注入 |
| 6 | max-tokens 语义(截断不调度/不跨 turn) | loop.spec:963-1187 | ②执行契约 |
| 7 | pre-step 时序 + 抛错隔离 | loop.spec:867-913 | ③验收挂点 |

## 面试弹药

- "subject 与 scope key 不能分歧":融合分派器注入 agent——payload 自带 agent 字段也覆盖不了(类型级保证)
- "0-step turn 也记录":拒绝或空 enter 仍闭合 durable turn——日志记录尝试
- "运行时上下文不重写 header":变化材料化在历史尾——与 OpenCode 的 Epoch 同思想(缓存前缀稳定)
- "inject 推迟到工具结果":工具执行期间注入排队——边界确定性
- "max-tokens 截断不调度工具":截断 step 的工具调用被丢弃(防半执行)
- "step/end 观察者失败隔离":不改变 continuation——观察者不破坏执行

## 待深挖

- [ ] inbox.ts 的实现(已完成,q21)
- [ ] tool-calls.ts 的 runGroup(已完成,q21)
- [ ] agent/request 瀑布的完整参数
