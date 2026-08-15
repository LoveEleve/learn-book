# q2 — Agent + AgentLoop(深度版:融合分派 + Turn/Step 状态机)

> 域:②执行引擎(核心) | 文件:packages/core/agent/src/(index 529+/dispatch 176/types 27/inbox/runtime-types)+ agent-loop/src/(index 713/agent 496/tool-calls 289/constants)+ docs/architecture.md(Turn flow)
> review 轮次:2 轮(源码全文核心)

---

## 假设

Agent = 接口 + live 注册表 + agent/* 事件;AgentLoop = 默认驱动(ReactLoopAgent)。Turn(0+ steps)/Step(1 request + tools)双层;输入经 InboxTarget(next-turn/next-step)到达。融合分派器保证 subject 与 scope 不分歧。

## 验证

### 1. 融合分派(设计 1:agentEvents 三模式)

```ts
// dispatch.ts:28-82
AgentSubjectEvent:类型级约束——payload 携带 agent 且 handler 的 this 是 Scoped<Agent>
agentEvents(ctx, agent, carrier):三种分派:
  emit: fire-and-forget,非 veto(同步 throw + promise rejection 各自隔离)
  serial: 有序等待(Cordis serial)
  waterfall: 中间件链(参数以 next 结尾;必须调用 next() 委托)
// emit 实现细节(dispatch.ts:120-137):
//   Cordis emit 用 Array.map——一个同步 throw 饿死后续监听器;返回 promise 被丢弃
//   → 自己 resolve 回调集,两种失败独立 contain
// carrier 复用:热路径分派无分配(循环驱动在构造函数建一次)
// subject 注入:payload spread 在前——payload 自带 agent 字段也不能覆盖注入的 subject
```

**产品启示**:②执行引擎的扩展点 = 融合分派(事件订阅者获得正确作用域 + 类型安全注入)。"subject 与 scope key 不能分歧"——比 OpenCode 的事件系统更严格的类型约束。

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
// agent/turn-stopping 是 serial(无 next)
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
// 输入 = 一个收件箱(inbox):部分消息立即唤醒,注入上下文等另一消息
```

### 4. 收件箱(设计 4:InboxTarget 双队列)

```ts
// types.ts:10 + index.ts(SessionEventMap 扩展):
InboxTarget = 'next-turn' | 'next-step'(两个有序 pending 列表)
'agent/inbox/spliced': 归一化 mutation 事件(durable!)
  { target, start, removedCount?, inserted, outcome?: 'canceled' }
// live dispatch 先于投影 mutation(同步观察者能读 splice 前的 inbox 恢复移除消息)
```

### 5. 提示词组装(设计 5:assembleContextFor)

```ts
// dispatch.ts:174-176:assembleContextFor(agent, signal) → { agent, scope: agent, signal? }
// agent-scoped prompt/tool 贡献不能静默遗漏(subject 与 scope 绑定)
```

## 结论

| # | 设计 | 位置 | 产品映射 |
|---|------|------|---------|
| 1 | 融合分派三模式(emit/serial/waterfall) | dispatch.ts:28-148 | ②扩展点类型安全 |
| 2 | Turn/Step 双层 + 0-step 闭合记录 | architecture.md:65-90 | ②执行主干 |
| 3 | Phase 状态机(idle/maintenance/running) | agent.ts:42-51 | ②执行状态 |
| 4 | InboxTarget 双队列 + durable spliced 事件 | types.ts:10 + index.ts | ②收件箱(可重放) |
| 5 | agent/pre-step 决定模型所见 | architecture.md:88 | ①对齐挂点(验收器) |

## 面试弹药

- "subject 与 scope key 不能分歧":融合分派器注入 agent——payload 自带 agent 字段也覆盖不了(类型级保证)
- "0-step turn 也记录":拒绝或空 enter 仍闭合 durable turn——日志记录尝试(与 OpenCode 的初始化阻塞对比,这里记录后关)
- "waterfall 必须 next()":中间件链语义——不调用 next 短路整链(验收器可挂 pre-step 拒绝)
- "Cordis emit 的坑":一个同步 throw 饿死后续监听器——自己 resolve 回调集隔离失败

## 待深挖

- [ ] inbox.ts 的实现(唤醒/注入上下文语义)
- [ ] tool-calls.ts(工具调用执行/并行上限 DEFAULT_MAX_PARALLEL_TOOL_CALLS)
- [ ] agent/request 瀑布的完整参数
