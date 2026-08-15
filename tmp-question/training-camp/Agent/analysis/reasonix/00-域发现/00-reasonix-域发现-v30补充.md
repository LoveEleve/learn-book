# Reasonix 域发现 v30 补充(续扫第十六轮:agent 核心大文件)— 2026-08-14

> 承接:v29。触发:做"体量排序 × 已打开"覆盖核对,发现 **agent.go(2,857,第 7 大)/task.go(2,076)/usecapability(1,621)/agentpreset 从未真正打开**——又一次"感觉到底"证伪。
> 结论:agent 核心的组件注入面与子代理工具变体确认,深化 ②执行。

---

## 一、v30 深化确认(agent 核心)

| 设计 | 位置 | 要点 |
|------|------|------|
| **执行常量** | agent.go:43-66 | maxToolOutputBytes 32K/maxEmptyFinalBlocks 3/**maxStreamRecoveries 5**/默认推理字节上限 8MB/finishReasonClientReasoningLimit |
| **Context 注入族** | :93-206 | callContext(父 id/sink/asker/planMode)/WithParentSession/**WithSubagentDepth**/WithUserImages——跨调用上下文 |
| **组件注入面** | :429-637 | SetGate/SetExtensions/SetRecoveryGate/SetSandboxEscapeApprover/SetConfigWriteApprover/SetMutationObserver——**gate 全部可注入** |
| **planMode 保缓存** | :315-320 | planMode 切换**不换 system prompt/工具列表**(保 provider-cache 前缀)——"规划模式是协作开关不是权限边界" |
| **readOnlyExecution** | :322-326 | 构造期防御(planner/research 代理,终身只读,代理解析后校验) |
| **子代理工具变体** | task.go:175-243 | **foregroundOnlyBash/readOnlyBash**(bash 工具变体——前台专用/只读包装) |
| **深度感知注册表** | :151-173 | SubagentToolRegistryForDepth(子代理深度 → 工具注册表) |
| **MCPCapabilityRuntime** | usecapability.go:28-58 | 共享 Host 每 agent 独立 UseCapabilityTool 前端(**ledger/audit 不跨 agent 边界**);dispatchMu 线性化(disable/uninstall 等待在途 dispatch) |

---

## 二、关键设计(通用价值)

1. **"planMode 不换 prompt 保缓存"**:规划模式只改协作指令不改 system prompt——**缓存稳定 vs 功能切换的取舍**(与 Hermes 缓存神圣同哲学)
2. **"工具变体而非新工具"**:foregroundOnlyBash/readOnlyBash 是同一 bash 的变体——**工具的多形态**
3. **"每 agent 独立前端,共享底层"**:MCP ledger/audit 不跨 agent 边界——**边界隔离**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v29 | — | 102 | 102 |
| v30 | agent 核心大文件 | +0(深化 4 设计) | **102**(深化) |

> 继续:next 轮 agentpreset(TaskPolicy 的基础!)/event.go 全貌/tool.go 契约。
