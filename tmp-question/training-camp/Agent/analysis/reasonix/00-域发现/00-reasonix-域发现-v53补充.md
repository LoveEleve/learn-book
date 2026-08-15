# Reasonix 域发现 v53 补充(续扫第三十九轮:chat_tui 主体)— 2026-08-14

> 承接:v52。本轮:internal/cli/chat_tui.go(5,569,第 2 大文件)主体。
> 结论:TUI 渲染层(tea 模型 + 事件消息类型),与 event 事件流是消费端关系——无新域。

---

## 一、v53 确认(chat_tui 主体)

| 设计 | 要点 |
|------|------|
| **tea 模型** | chatTUI(bubbletea 状态机)+ tuiState 枚举 + agentEventMsg(event.Event 包装) |
| **消息类型族** | compactDoneMsg/tuiShutdownMsg/elapsedTickMsg/balanceMsg/statuslineMsg/gitStatusMsg/modelSwitchMsg/promptResolvedMsg/refsResolvedMsg/clipboardImageMsg |
| **状态栏命令** | runStatuslineCmd(带超时)——git 状态/余额/状态栏刷新 |
| **粘贴回忆** | rememberSubmittedInput/recallSubmittedInput(输入历史) |
| **环境适配** | isTermuxTerminal/mouseCaptureOffByDefault |

**结论**:chat_tui 是 event 事件流的 TUI 消费端(agentEventMsg 包装事件)——与 v32 的"typed 事件流解耦'发生了什么'与'怎么显示'"验证一致。TUI 渲染层无独立产品设计。

---

## 二、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v52 | — | 102 | 102 |
| v53 | chat_tui 主体 | +0(消费端确认) | **102**(确认) |

> 继续:next 轮 workers(accounts/crash-report/forum)/sdk wire/cmd 工具——工程层按需收尾。
