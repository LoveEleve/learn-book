# Reasonix 域发现深度 Review 补充报告(2026-08-14)

> 触发:Pi 假收敛证伪后,用同法(文件体量排序 + 覆盖核对)复测 Reasonix。
> 方法:internal/ 966 个 Go 文件(排除 _test.go)体量排序,top 70 逐一核对域发现覆盖率。
> 结论:**Reasonix 域发现同样假收敛**(41 域声明完成,实际遗漏多个 load-bearing 文件,包括项目最大文件 controller.go 6,276 行)。

---

## 一、确认的遗漏(按严重度排序)

### 🔴 高价值遗漏(5 个 — 产品直接相关,域发现 0 覆盖)

| # | 文件 | 体量 | 设计价值 | 产品映射 |
|---|------|:--:|---------|---------|
| 1 | **internal/control/controller.go** | **6,276(项目最大)** | **transport-agnostic 会话驱动**:Controller 拥有 agent 运行循环 + 会话生命周期;命令(Send/Cancel/Approve/SetPlanMode/Compact/NewSession)+ 单一 typed 事件流(event.Sink);**每个前端(TUI/desktop/HTTP-SSE)都驱动同一 Controller**——与 Hermes TurnRunner/gateway 同构 | ②执行引擎核心 |
| 2 | **internal/agent/save.go** | 2,288 | **会话持久化正确性**:filelock 侧车锁(.jsonl.lock/.lease.lock/.lease.json)、guardian 侧车、原子写、文件名上限(255)、recovery 父子 stem 限制 | ④知识库写入安全 |
| 3 | **internal/repair/update.go** | 3,712 | 会话文件修复/升级管线 | ④自愈 |
| 4 | **internal/jobs/jobs.go** | 2,071 | **session 级后台任务注册表**(background tools 基础):Manager 生命周期 = session 非 turn;跨 turn 持续;完成摘要注入下一轮(DrainCompletedNote) | ②执行 |
| 5 | **internal/cli/chat_tui.go** | 5,569 | TUI 交互(气泡 tea/textarea/viewport)——交互形态 | ①对齐 |

### 🟡 中价值遗漏(7 个)

| # | 文件 | 体量 | 说明 |
|---|------|:--:|------|
| 6 | internal/cli/cli.go | 2,750 | CLI 编排 |
| 7 | internal/control/refs.go | 1,358 | 引用管理 |
| 8 | internal/control/goal.go | 1,051 | goal 状态 |
| 9 | internal/control/inbox.go | 787 | 收件箱 |
| 10 | internal/agent/subagent_store.go | 949 | 子代理存储 |
| 11 | internal/agent/session_events.go | 886 | 会话事件 |
| 12 | internal/event/event.go | 884 | **typed 事件定义**(Controller 发射的事件流) |

### 🟢 其他核对(域发现已覆盖或低价值)

- repair/(plan 853/transaction 784)— 域发现 repair 有 4 处引用
- checkpoint/(transaction 1,773/checkpoint 1,042)— 覆盖
- recovery/(gate 1,415/rules 1,011)— 覆盖
- evidence/evidence.go 2,788 — 覆盖(5 处)
- goal 域 10 处引用 — 覆盖
- provider/(openai 1,331/responses 899/anthropic 850)— 域发现有 provider 但具体适配文件未列

---

## 二、遗漏分布规律(与 Pi 完全一致)

| 维度 | Pi | Reasonix |
|------|----|----------|
| 声明完成轮次 | v1-v6(6 轮) | v1-v5(5 轮) |
| 声明域数 | 59 | 41 |
| 最大文件遗漏 | interactive-mode 6,436(6 轮没进视野) | **controller.go 6,276(5 轮没进视野)** |
| 遗漏模式 | 支撑架构叙事的机制层 | **transport 驱动层 + 持久化正确性层** |
| 共同根因 | 按目录感觉扫描,无体量排序核对 | 同左 |

**结论:两个项目都是"感觉收敛"——体量排序是穷尽性检查的第一工具,这条 D18 方法论被 Pi 和 Reasonix 双重实证。**

---

## 三、controller.go 的特别价值(产品②执行引擎最直接蓝本)

```
Controller = 传输无关的会话驱动:
- 拥有:agent 运行循环 + 会话生命周期
- 命令:Send/Cancel/Approve/SetPlanMode/Compact/NewSession
- 发射:reasoning/tool calls/approvals/turn completion → typed 事件流(event.Sink)
- 设计哲学:"一个编排层服务每个前端,前端不重实现回合生命周期/取消/审批"
```

**跨项目印证**:
- Hermes:gateway/run.py 的 GatewayRunner + TurnRunner(事件回调分离)
- Pi:InteractiveMode 持有 AgentSessionRuntime(三模式共享内核)
- Reasonix:Controller 统一驱动 TUI/desktop/HTTP-SSE

**这是"执行引擎 = 传输无关内核 + 事件流 + 前端外壳"的第三个独立实现**——产品②执行引擎架构的最强证据。

---

## 四、处置

- [ ] Reasonix 域发现补 v6(体量排序复测):+5 高价值域(controller/save/update/jobs/chat_tui)+ 7 中价值
- [ ] Reasonix 闭环笔记补:controller(执行引擎内核)+ save(写入安全)+ jobs(后台任务)
- [ ] Reasonix 参考架构检查:是否引用 Controller 模式?(若无 → 补)
- [ ] 方法论 D18 已在 Pi 实证后写入,Reasonix 加强证据链
