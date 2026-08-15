# rq8 Controller(transport-agnostic 会话驱动)— 产品②执行引擎内核,三项目同构

> 项目:Reasonix(internal/control/controller.go,6,276 行 — **项目最大文件**)
> 背景:Reasonix 域发现 v1-v5 假收敛(41 域),v6 体量排序复测补漏。本笔记是 controller.go 的闭环——它是产品②执行引擎最直接的完整蓝本。
> 假设:Controller 是"传输无关执行内核 + 事件流 + 前端外壳"模式的第三个独立实现(与 Hermes GatewayRunner+TurnRunner、Pi AgentSessionRuntime 同构),是产品②的架构定论。
> 结论:✅ 成立——Controller 的组件编排(guardian/权限/记忆/预算/评估器)就是产品②"自主跑"的完整组件清单。

---

## 一、架构定位:一个编排层服务每个前端

```
每个前端(TUI / desktop / HTTP-SSE)驱动同一 Controller:
  ┌─────────────────────────────────────────────┐
  │ Controller(transport-agnostic 会话驱动)     │
  │  拥有:agent 运行循环 + 会话生命周期         │
  │  命令:Send/Cancel/Approve/SetPlanMode/      │
  │        Compact/NewSession/Submit/SubmitHTTP │
  │  发射:typed 事件流 → 单一 event.Sink        │
  └─────────────────────────────────────────────┘
  前端不重实现:回合生命周期 / 取消 / 审批
```

**组件依赖图(controller.go:89-150 字段即组件清单)**:
```
Controller 持有:
├── runner(agent.Runner)+ executor(*agent.Agent)
├── guardianSess(长活守卫)+ recoveryGate(自动守卫)
├── taskBudget + goalTokenBudget(花费门)
├── evaluator(goaleval.Evaluator — 有界目标完成评估器)
│   └── nil 时 fail closed:目标暂停而非默认继续
├── goalUsageTee(计费事件 → 目标回合观测 token 总量)
├── sink(event.Sink — 唯一事件出口)
├── policy(permission.Policy)
├── subagentGate(共享子代理门)
├── skills(技能集 + SubagentRunner + ProfileResolver)
├── hooks(hook.Runner)+ hookContexts(一次性钩子上下文)
├── memory(memoryManager:快照 + 轮尾笔记队列 + 写串行化)
└── shell(sandbox.Shell — 用户 "!" 命令解释器)
```

---

## 二、设计 1:命令面 = 前端契约(Submit 族)

**位置**:`controller.go:1041-1259`

```
Send(input)                    — 基础发送
SendWithRaw(input, raw)        — 原始文本
Submit(input)                  — 提交
SubmitHTTP(input)              — HTTP 会话入口
SubmitHTTPFormat(input, fmt)   — 带格式
SubmitDisplay(display, input)  — 显示文本分离
SubmitDeliveryRecovery(...)    — 投递恢复(重试)
SubmitInvocationDisplay(...)   — 调用请求
SubmitEditedDisplay(...)       — 编辑后提交
SubmitUserTurn(input, display) — 用户回合
```

**设计要点**:
- **display 与 input 分离**:显示文本与模型输入分开(前端渲染与实际语义解耦)
- **投递恢复**:SubmitDeliveryRecovery 是崩溃后恢复的提交入口
- 命令面 = 前端必须实现的接口,事件面 = 前端必须渲染的协议

**产品④映射**:产品②执行引擎的"命令面/事件面"契约——前端(CLI/web/API)都走同一命令面,渲染同一事件面,不各自实现回合逻辑。

## 设计 2:守卫回合(spawnGuardedTurn / finishGuardedTurn)

**位置**:`controller.go:932-1040`

```
spawnGuardedTurn(ctx, cancel, body):
  → 带守卫的回合执行(guardian 保护)
finishGuardedTurn(err, completion):
  → 回合完成的收尾(错误分类 + 完成信息)
```

**关键**:guardianSess(长活守卫)+ recoveryGate(自动守卫)是回合执行的保护层——与 Hermes 的 tool_guardrails、Pi 的 output-guard 同族。

## 设计 3:目标循环(goal loop)+ 评估器 fail-closed

**位置**:`controller.go:1109-1150` + `evaluator` 字段

```
runGoalLoopWithRaw(ctx, input, raw)      — 无人值守目标循环
runGoalLoopWithRawDisplay(...)           — 带显示
runEditedGoalLoopWithRawDisplay(...)     — 编辑后
runTurnWithRawDisplay(ctx, input, raw, display)
runSubagentSkillSlash(skill, task, ...)  — 技能斜杠
stopGoal(status)                         — 停止目标
```

**关键设计**:
- `evaluator` 为 nil 时 **fail closed:目标暂停而非默认继续**——"没有评估器就不能自主跑"
- `goalTokenBudget` 无人值守目标循环的 token 上限(0 = 无界,但默认有界)
- `goalUsageTee` 把计费事件计入目标回合观测 token 总量——**自主跑的可观测成本**

**产品④映射**:产品②"自主一直跑"的架构定论:
1. 无人值守循环必须有**评估器**(fail-closed:无评估器 → 暂停)
2. 必须有 **token 预算**(goalTokenBudget)
3. 必须有 **成本观测**(goalUsageTee)

## 设计 4:事件流(单一 event.Sink)

**位置**:controller 全部发射走 `c.sink`

- reasoning / tool calls / approvals / turn completion 全部 → 单一 typed 事件流
- 前端订阅渲染,不各自实现事件采集
- 与 Hermes stream_dispatch(adapter 驱动事件分发)、Pi events.ts 同构

## 设计 5:记忆/技能/钩子的集成模式

- `memory memoryManager`:加载快照 + 轮尾笔记队列 + **写串行化在自有锁下,off c.mu**——"记忆面板保存不会卡审批或状态轮询"(并发正确性)
- `skills skillSet + SubagentRunner + ProfileResolver`:技能发现/子代理运行/画像解析
- `hooks *hook.Runner + hookContexts`:会话钩子;一次性钩子上下文注入下一个真实用户回合,**不改变缓存稳定的 system prompt**

---

## 三、三项目同构确认(执行引擎通用模式)

| 维度 | Hermes | Pi | Reasonix |
|------|--------|-----|----------|
| 执行内核 | GatewayRunner + TurnRunner | AgentSessionRuntime | **Controller** |
| 多形态 | CLI/gateway/TUI/desktop | interactive/print/rpc | TUI/desktop/HTTP-SSE |
| 事件出口 | stream_dispatch/events | events.ts/EventBus | **event.Sink** |
| 守卫 | tool_guardrails | output-guard | **guardian + recoveryGate** |
| 预算 | iteration_budget | max_iterations | **taskBudget + goalTokenBudget** |
| 评估 | — | — | **evaluator(fail-closed)** |
| 记忆集成 | MemoryManager | facts | **memoryManager(写串行化 off 主锁)** |

**架构定论**:产品②执行引擎 = **传输无关内核 + 命令面 + 事件面 + 守卫/预算/评估器组件**,这是三个独立项目的共同答案。

---

## 四、面试弹药

1. **"为什么前端不重实现回合生命周期"**:Command/Event 面契约——前端只发命令渲染事件,取消/审批/生命周期全在 Controller
2. **"evaluator 为 nil 时 fail closed"**:无人值守目标循环没有评估器 → 暂停而非默认继续——"自主跑"的安全性前提
3. **"display 与 input 分离"**:显示文本与模型输入解耦,前端渲染不影响语义
4. **"goalUsageTee"**:自主跑的成本观测是内建的(计费事件计入目标回合 token 总量)
5. **"记忆写串行化 off 主锁"**:记忆保存不卡审批/状态轮询——锁粒度决策

---

## 五、产品映射汇总

| 设计 | 产品②(执行引擎)用法 |
|------|---------------------|
| 命令面/事件面契约 | 前端统一接口(CLI/web/API) |
| spawnGuardedTurn | 回合守卫层 |
| goal loop + evaluator fail-closed | 无人值守自主跑(无评估器 → 暂停) |
| goalTokenBudget | 自主跑 token 预算 |
| goalUsageTee | 自主跑成本观测 |
| 单一事件流 | 全前端统一渲染协议 |
| 记忆写串行化 off 主锁 | 并发正确性(副操作不卡主路径) |

> 覆盖设计数:7(设计 1-5 + 2 子设计/3 子设计)
