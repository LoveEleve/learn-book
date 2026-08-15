# Reasonix 域发现 — 源码分析范围梳理

> 项目:esengine/DeepSeek-Reasonix(main-v2,5,453 commits)
> 生成:2026-08-14
> 输入:SPEC.md(1232 行)+ REASONIX.md + internal/ 94 包目录结构
> 排除:desktop/(前端)、非 Linux 平台分支(_windows.go)、benchmarks、npm、site

---

## 一、架构总览(从 REASONIX.md + SPEC.md)

**一句话**:Reasonix = **单一控制器(control.Controller)驱动的、配置/插件驱动的、缓存优先的、带任务契约和检查点的 Go Agent 引擎。**

### 核心架构决策(Pass 0 收获)

| 决策 | 内容 | 位置 |
|------|------|------|
| **单控制器** | control.Controller 是所有前端(TUI/HTTP/Wails)背后的唯一逻辑层 | REASONIX.md |
| **分层强制** | repolint 工具强制执行;只有前端可 import control | REASONIX.md |
| **缓存优先** | system prompt 前缀必须字节稳定(DeepSeek 前缀缓存) | REASONIX.md |
| **配置/插件驱动** | core 只知道接口,模型/工具按名字从注册表解析 | SPEC §1 |
| **单静态二进制** | CGO_ENABLED=0 | SPEC §1 |
| **双扩展层** | 编译期内置(init 自注册)+ 运行时插件(MCP) | SPEC §1 |

### 依赖方向(SPEC §2)

```
cli → {agent, plugin, config} → {tool, provider}
remote 永不 import cli/agent/serve
```

---

## 二、域清单(94 包收敛后)

### 🔴 核心域(12 个)

| # | 域 | 包 | 设计决策 |
|---|----|-----|---------|
| 1 | **Controller 单控制器** | internal/control/(150+ 文件) | 所有前端共享的逻辑层;approval/recovery/rewind/goal 全在这 |
| 2 | **Agent 会话与循环** | internal/agent/(200+ 文件) | Session+Run 循环;coordinator/arbiter/fleet/subagent 全在这 |
| 3 | **上下文管理(ContextManager)** | internal/agent/(compact_*/context_*) | **content-driven summary**:唯一阈值 compact_ratio 0.85,单次摘要事务 |
| 4 | **双模型协作(Coordinator)** | internal/agent/coordinator.go | planner+executor 独立会话保缓存;planner_route 决策 |
| 5 | **检查点 Checkpoint** | internal/checkpoint/ | 每轮 checkpoint + 原子写 + barrier + 观察者 |
| 6 | **任务契约 TaskContract** | internal/taskcontract/ | 任务生命周期契约 |
| 7 | **权限 Permission** | internal/permission/ | per-call Policy:allow/ask/deny |
| 8 | **工具系统 Tool** | internal/tool/ + tool/builtin/ | Tool 接口 + 注册表 + 内置工具 |
| 9 | **记忆系统 Memory** | internal/memory/(15+ 文件) | 自动记忆:activation/auto_recall/forget/index/freshness |
| 10 | **沙箱 Sandbox** | internal/sandbox/ | Linux 隔离(prepare_linux.go) |
| 11 | **会话存储 Session** | internal/sessioncatalog/ + agent/session* | JSONL 会话 + 目录 + lease |
| 12 | **任务调度 Task** | internal/task/ + taskcatalog/ + taskintent/ + taskmonitor/ + taskpolicy/ | 任务状态机/策略/监控 |

### 🔴 v3 升级(从支撑域升为核心域— review 发现被低估)

| # | 域 | 包 | 设计决策 |
|---|----|-----|---------|
| 13 | **Goal 评估器(Goaleval)** | internal/goaleval/ | **独立无工具/无历史/无压缩的完成评估器**;输出 JSON(outcome: complete/continue/blocked/uncertain);缓存隔离——**产品③验收器完整蓝本** |
| 14 | **计划契约(PlanContract)** | internal/plancontract/(7 文件) | **"计划是数据不是 prose"**;Identity host 分配 + Revision diff;VerifiedFiles vs CandidateFiles 证据分离——**产品①规格书完整蓝本** |

### 🔴 v4 升级(独立审查器架构模式 — review 发现通用模式)

| # | 域 | 包 | 设计决策 |
|---|----|-----|---------|
| 15 | **有界 LLM(BoundedLLM)** | internal/boundedllm/ | **独立审查者的共享基础设施**:temperature-0 单请求 + 硬时间/输出预算(含 DefaultMaxOutputBytes 防 provider 忽略 MaxTokens);usage 归审查者源不污染主缓存——goaleval/guardian 共用 |
| 16 | **守卫(Guardian)** | internal/guardian/(guardian/policy/transcript) | **长期存活的守卫 sub-agent**:跨轮审查工具调用审批;复用同一 Agent 会话;每 50 次审查压缩(compactEvery);策略编译期嵌入 |
| 17 | **能力清单(Capability)** | internal/capability/(5 文件) | 5 Kind(skill/mcp-server/mcp-tool/tool/source)+ 6 Status(ready/configured/disabled/failed/stale)+ AutoUse;能力注册/审计/目录 |

### 🔴 v5 升级(AutoResearch TaskSpec — 产品规格书完整蓝本)

| # | 域 | 包 | 设计决策 |
|---|----|-----|---------|
| 18 | **自动研究(AutoResearch)** | internal/autoresearch/(4 文件) | **TaskSpec 三合一蓝本**:goal/scope/**non_goals**/allowed_operations(write/network/publish)/**success_criteria+evidence_ids**;Progress(stale_count/pivot_count 停滞检测);Finding(source 溯源);Summary(open_criteria/next_required_action 自动进度摘要);ValidationReport 校验 |

**AutoResearch TaskSpec 是产品①规格书 + ②执行契约 + ③验收器的三合一完整蓝本**:
- non_goals = 边界排除维度
- allowed_operations = 权限边界
- success_criteria + evidence_ids = 验收判据 + 证据链
- stale_count/pivot_count = "agent 死循环"的量化检测(停滞/转向)
- Summary.next_required_action = 交接文档自动化
- Finding(source: command/file/manual) = 闭环笔记结构

### 🟡 支撑域(15 个)

| # | 域 | 包 | 设计决策 |
|---|----|-----|---------|
| 13 | **Subagent** | agent/(subagent_* 15+ 文件) | 五概念分离(profile/TaskSpec/CapabilityGrant/ContextRequest/SchedulerPolicy) |
| 14 | **Fleet(小依赖图)** | agent/fleet.go | 子代理依赖图 |
| 15 | **计划契约 PlanContract** | agent/plan_contract.go + internal/plancontract/ | 计划结构化交付 |
| 16 | **Checkpoint 恢复 Recovery** | internal/recovery/ + control/recovery.go | 崩溃恢复 |
| 17 | **历史检索 History** | internal/history/ + historycatalog/ | BM25 会话检索 |
| 18 | **MCP 插件** | internal/plugin/ + mcpregistry/ + mcplaunch/ + mcpdiag/ | stdio JSON-RPC 插件 |
| 19 | **扩展协议** | internal/extension/ + extensioncontract/ | Extension Protocol v1 |
| 20 | **技能 Skill** | internal/skill/ | 技能系统 |
| 21 | **事件 Event** | internal/event/ + eventwire/ | 事件分发 |
| 22 | **遥测 Telemetry** | internal/telemetry/ + stats/ | 统计 |
| 23 | **轨迹 Trajectory** | internal/trajectory/ | 运行轨迹记录 |
| 24 | **远程 Remote** | internal/remote/ | SSH 传输(sftpfs/forward/bootstrap) |
| 25 | **凭证 Secrets** | internal/secrets/ | 密钥管理 |
| 26 | **证据 Evidence** | internal/evidence/ | 证据收集(高 fan-in:236) |
| 27 | **日志/审计** | internal/sessiontemp/ + crashreport/ | 会话临时/崩溃报告 |

### 🟡 v2 新增(review 补全)

| # | 域 | 包 | 设计决策 |
|---|----|-----|---------|
| 28 | **工作区租约 WorkspaceLease** | internal/workspacelease/ | **写者租约持续到任务完成;读者不租约**——防 review 时工作区被改 |
| 29 | **自修复引擎 Repair** | internal/repair/(26 文件) | 修复事务 + 配置快照 + UndoLastRepair + 跨平台路径处理 |
| 30 | **进程管理 Proc** | internal/proc/(13 文件) | 命令运行/进程树 kill/优先级/隐藏窗口 |
| 31 | **Shell 安全** | internal/shellparse/ + shellrun/ + shellsafe/ | 命令解析/执行/安全 |
| 32 | **LSP** | internal/lsp/ | 语言服务器协议 |
| 33 | **完成度报告 Completion** | internal/completion/(claim/report) | 完成声明与报告 |
| 34 | **文件锁 FileLock** | internal/filelock/ | 文件级锁 |
| 35 | **系统代理 SysProxy** | internal/sysproxy/ | 系统代理配置 |

### 🟢 扫描域(按需)

| 域 | 包 | 说明 |
|----|-----|------|
| CLI | internal/cli/ + cmd/reasonix/ | 命令入口 |
| 配置 | internal/config/ | TOML 加载(flag>project>user>defaults) |
| 启动 | internal/boot/ | 组装 |
| 机器人 | internal/bot/ + botruntime/ | 无人值守 bot |
| Billing | internal/billing/ | 计费 |
| 医生 | internal/doctor/ | 诊断 |
| 守卫 | internal/guardian/ | 安全审查 |
| ACP | internal/acp/ | 编辑器协议 |
| 服务器 | internal/serve/ | HTTP/SSE |

---

## 三、排除清单

| 排除 | 原因 |
|------|------|
| desktop/(Wails 前端) | 前端 |
| 非 Linux 平台分支(_windows.go/session_lock_windows) | D10 Linux 基准 |
| benchmarks/ | 评测(产品③参考,不是架构) |
| npm/ site/ scripts/ | 分发/站点 |
| 工具 _test.go(约一半文件是测试) | 测试基础设施(但**契约测试要读**——conformance 价值) |

---

## 四、与产品四组件的初步映射(待深挖验证)

| 产品组件 | Reasonix 对应域 | 待验证问题 |
|---------|----------------|-----------|
| ① 对齐模块 | plan_contract + taskcontract + permission | 计划契约 = 规格书?任务契约怎么定义"完成"? |
| ② 执行引擎 | agent/ + control/ + coordinator | 单控制器怎么组织 turn?双模型怎么协作? |
| ③ 自动验收器 | checkpoint/ + trajectory/ + goaleval | checkpoint 怎么原子化?goal 怎么评估? |
| ④ 书级知识库 | memory/ + history/ + sessioncatalog | BM25 记忆怎么检索?会话怎么组织? |

### v2 补充:WorkspaceLease 对产品④的价值

**WorkspaceLease(workspacelease/lease.go)是产品④的关键参考**:
```
写者从第一次变更保持租约直到所有参与 agent 运行和后台任务完成
读者永不获取租约
目的:review 和验证不会被另一个会话改变工作区而失效
```

**产品映射**:验收器验收章节时,写者保持租约直到验收完成——**防"验收中工作区被改"**。比 Pi 的 writer-lease 更进一步:**明确区分读者/写者 + 租约持续到任务完成**。

---

## 五、Reasonix 独有设计(产品新参考)

1. **缓存优先架构**:system prompt 前缀字节稳定,任何重写都会杀缓存——**产品上下文管理的最高原则**
2. **单次摘要事务**:只在 compact_ratio(0.85)触发一次,摘要 ≤50% 窗口,不加 padding
3. **双模型独立会话**:planner/executor 永不混会话(保各自缓存)
4. **任务契约 + 检查点**:长跑恢复的完整方案
5. **五概念 subagent**:profile/TaskSpec/CapabilityGrant/ContextRequest/SchedulerPolicy
6. **写声明(write claims)强制而非建议**:sub-agent 写完声明的文件
7. **工作区租约(读者/写者分离 + 持续到任务完成)** — v2 新增
8. **自修复引擎(repair 事务 + 快照 + undo)** — v2 新增
9. **独立审查器架构模式**(boundedllm 共享基础设施 + goaleval 每轮 + guardian 长活 + 缓存隔离)— v4 新增
10. **能力清单(capability 5 Kind/6 Status + AutoUse)** — v4 新增

---

## 六、Pass 2 深挖顺序(产品优先级)

1. **AutoResearch TaskSpec**(产品①规格书三合一蓝本)— v5 升优先级
2. **PlanContract**(规格书数据模型)— v3
3. **Goaleval + BoundedLLM**(产品③验收器完整蓝本)— v3/v4
4. **上下文管理(ContextManager + compact_*)** — 产品②核心
5. **Coordinator 双模型** — 多模型编排
6. **Controller** — 单控制器组织
7. **Checkpoint 原子写** — 产品③验收/恢复
8. **TaskContract** — 任务生命周期
9. **Permission** — 产品安全
10. **Memory + History(BM25)** — 产品④
11. **Task 状态机** — 长任务

---

## 七、待确认问题

- [x] Q1(答):ContextManager 与 Pi 的 compaction 有何本质差异?(单次 vs 增量?)——Pi 闭环笔记已对比
- [x] Q2(答):checkpoint 的原子写怎么实现?(barrier/blob/transaction)——rq7 已深挖
- [x] Q3(答):planner_route 的确定性决策怎么组织?(不用分类器模型)——rq2 已深挖
- [x] Q4(答):BM25 记忆检索与 Hermes 的 FTS5 有何差异?——Hermes hq3 可对比
- [x] Q5(答):写声明(write claims)怎么强制?——rq5 已深挖
- [x] Q6(答):WorkspaceLease 的租约生命周期具体怎么管理?(与 Pi 的 writer-lease 对比)——rq1 已深挖
- [x] Q7(答):repair 事务的快照/undo 机制与 checkpoint 的关系?——rq7 已深挖
- [ ] Q8(v6 新增):Controller(transport-agnostic 会话驱动)如何组织运行循环与事件流?
- [ ] Q9(v6 新增):save.go 的侧车锁/guardian 写入安全细节?

## 八、Review 记录

| 轮次 | 发现 | 状态 |
|:--:|------|------|
| v1 | 域发现(12 核心 + 15 支撑 + 8 扫描,94 包全覆盖) | 完成 |
| v2 | 深度 review:94 包全核对,补 8 个支撑域;workspacelease 是产品④关键参考;repair 自修复引擎 | 完成 |
| v3 | 深度 review:**goaleval + plancontract 从支撑域升为核心域**——分别是产品③验收器和产品①规格书的完整蓝本;"计划是数据不是 prose"哲学 + 独立评估器设计 | 完成 |
| v4 | 深度 review:**发现"独立审查器架构模式"**(boundedllm 共享基础设施 + guardian 长活守卫 + goaleval 每轮评估 + 缓存隔离三实例);capability 能力清单;核心域 12→17 | 完成 |
| v5 | 深度 review:**AutoResearch TaskSpec 升为核心域**——产品①规格书的三合一完整蓝本(non_goals/allowed_operations/success_criteria+evidence_ids);stale_count/pivot_count 停滞检测;Summary.next_required_action 交接自动化;Finding 闭环笔记结构;核心域 17→18 | 完成 |
| v6 | 体量排序复测(Hermes/Pi 反哺):966 Go 文件排序 top 70 核对 → **假收敛证伪** | 完成 |

**v6 复测发现(2026-08-14,与 Pi 同教训)**:

按"文件体量排序 + 覆盖核对"复测(966 个 Go 文件,排除 _test.go),发现 v1-v5 的 41 域是**假收敛**:

| 遗漏文件 | 体量 | 设计价值 | 新增域 # |
|---------|:--:|---------|:--:|
| **internal/control/controller.go** | **6,276(项目最大)** | **transport-agnostic 会话驱动**:Controller 拥有 agent 运行循环 + 会话生命周期;命令(Send/Cancel/Approve/SetPlanMode/Compact/NewSession)+ 单一 typed 事件流;每个前端(TUI/desktop/HTTP-SSE)驱动同一 Controller——**产品②执行引擎内核**,与 Hermes GatewayRunner+TurnRunner、Pi AgentSessionRuntime 三项目同构 | 42 |
| **internal/agent/save.go** | 2,288 | 会话持久化正确性:filelock 侧车锁/.lease.json/guardian 侧车/原子写/文件名上限 | 43 |
| **internal/repair/update.go** | 3,712 | 会话文件修复/升级管线 | 44 |
| **internal/jobs/jobs.go** | 2,071 | **session 级后台任务注册表**:Manager 生命周期 = session 非 turn;跨 turn 持续;完成摘要注入下一轮(DrainCompletedNote) | 45 |
| **internal/cli/chat_tui.go** | 5,569 | TUI 交互形态(气泡 tea/textarea/viewport) | 46 |
| cli.go / refs.go / goal.go / inbox.go / subagent_store.go / session_events.go / event.go | 884-2,750 | CLI 编排/引用/goal 状态/收件箱/子代理存储/会话事件/**typed 事件定义** | 47-53 |

**v6 元教训**:
- **与 Pi 完全同模式**:两个项目都是"感觉收敛"(Pi 59/Reasonix 41),最大文件都漏了 6/5 轮(interactive-mode 6,436 / controller.go 6,276)
- **"执行引擎 = 传输无关内核 + 事件流 + 前端外壳"是三项目独立实现的通用模式**(Hermes GatewayRunner / Pi AgentSessionRuntime / Reasonix Controller)
- **D18 方法论双重实证**:文件体量排序是穷尽性检查的第一工具,禁止按目录感觉扫描
- 剩余:top 70 已核对,后续按需深挖(controller 闭环笔记见 rq8)

### 🟢 v15-v29 深化(续扫第 15-29 轮 — taskmonitor/acp/checkpoint 验证/能力/目录/支撑层)

**触发**:用户继续追问("101 域收官"声明再次证伪)。v15 打开从未碰过的 taskmonitor,此后 15 轮覆盖 internal/ 96 包全部实现面。

**v15-v29 深化契约(契约深化 40+)**:
- **taskmonitor 纯观察层**:TaskState 7 态 + RuntimeState 独立("不实现第二状态机,以 jobs.Manager 为唯一真相源")
- **approvalManager 严格叶子**:只动自己状态绝不回调 Controller;ask/auto/yolo 三模式 + 审批超时防卡死
- **MutationBarrier 验证**:generation 代际检测并发变异(不靠墙钟)——四项目共证代际模式
- **acp 会话生命周期**:begin TryLock 非阻塞准入 + 待决配置阻塞新回合
- **capability Entry 18 字段**:触发词/负触发词/依赖/画像/成本/AutoUse 四档——**对齐模块 B 档推荐的数据结构**
- **shellsafe 静态效果分类**:Certainty(无法证明 → fail closed)+ WriteDomain 4 类位掩码
- **capdiag 只读诊断**:永不写 config/cache/state/log
- **目录签名跳过扫描**(sessioncatalog)+ 可丢弃投影族(task/usage/session/history catalog)
- **SubgraphKind 7 类**(增量重建)+ PublishGate 代际排空
- **Volatility 遗忘速度**(memory)+ 代码符号分词(retrieval v2)
- **strictDecode 未知字段拒绝**(pluginpkg)
- **externalizable 事件卸载**(eventwire)
- **ablation 6 模块消融**(评测归因)

**v15-v29 元教训(最终)**:
- **"101 域收官"再次被证伪**:taskmonitor 从未打开(第 15 轮才发现)——**"收官声明"在 Reasonix 上第 2 次失效**
- **"纯观察层不实现第二状态机"**:观察与真相源分离防状态漂移(Hermes session_stall 同哲学)
- **"能力条目 = 路由数据模型"**:18 字段能力清单是产品①对齐模块 B 档的推荐数据结构
- **Reasonix 最终收官**:internal/ 96 包全部覆盖,源码实现面全覆盖(含 15 轮验证),102 域 + 40+ 契约深化;剩余(desktop 前端/i18n 消息/providers 适配)为排除清单或同构

### 🟢 v30-v36 深化(续扫第 30-36 轮 — agent 核心大文件/agentpreset/event/permission/compact)

**触发**:用户继续追问。做"体量排序 × 已打开"覆盖核对,发现 **agent.go(2,857,第 7 大)/task.go(2,076)/usecapability(1,621)/agentpreset 从未真正打开**——第 3 次"收官"证伪。

**v30-v36 深化契约**:
- **agent.go 组件注入面**:SetGate/SetExtensions/SetRecoveryGate/SetSandboxEscapeApprover/SetConfigWriteApprover/SetMutationObserver;执行常量(maxToolOutputBytes 32K/maxStreamRecoveries 5/推理上限 8MB)
- **planMode 保缓存**:规划模式切换不换 system prompt/工具列表(保 provider-cache 前缀)——"规划模式是协作开关不是权限边界"
- **task.go 工具变体**:foregroundOnlyBash/readOnlyBash(同一 bash 的多形态)+ 深度感知注册表 SubagentToolRegistryForDepth
- **usecapability**:MCPCapabilityRuntime(共享 Host 每 agent 独立前端,ledger/audit 不跨 agent 边界)+ dispatchMu 线性化
- **agentpreset 三预设矩阵**:PolicyOf(Light 目标验证/Delivery 全验证+原子契约/Balanced 中间档)——"画像 → 契约"的确定性映射,产品①对齐模块直接参考
- **event Kind 全集**:30+ 种(每种的渲染语义文档化);KindCount 哨兵(新事件自动被完备性测试覆盖);aborted 压缩也发 Done(占位符不悬挂);用户取消不是错误
- **permission 三列表**:Decide(toolName/readOnly/args)+ bash 分段判定 + RuleCoversString 规则覆盖检测
- **compact 实现**:折叠经济学(≥400 token 才折)/CJK 感知 token 估算(字节/字符取大)/固定前缀=system+首 user 回合/保留策略仅最新摘要后生效
- **MCP OAuth 完整实现**:受保护资源发现(RFC 8414)/动态客户端注册/令牌刷新

**v30-v36 元教训(最终)**:
- **"收官声明"在 Reasonix 上第 3 次证伪**:v29 说"96 包全覆盖",v30 覆盖核对发现 agent.go(2,857)/task.go(2,076)/usecapability(1,621)/agentpreset 从未打开——**正确的收敛判据 = 体量排序 × 已打开清单对账,不是感觉**
- **agentpreset 是 TaskPolicy 的地基**:三预设矩阵是"画像 → 契约"的确定性映射——产品①对齐模块的最直接参考
- **Reasonix 最终状态**:internal/ 96 包全覆盖(含 8 个大文件源码级验证:agent/task/usecapability/agentpreset/event/permission/tool/compact),102 域 + 50+ 契约深化

### 🟢 v37-v42 深化(续扫第 37-42 轮 — plugin 宿主/hook 系统/recovery Gate/evidence Receipt/验证族)

**触发**:用户继续追问。v37-v42 打开剩余 1,000+ 行核心文件(plugin.go 1,969/hook.go 1,592/recovery gate 1,415/serve 1,684)与 rq2/rq3 源码验证。

**v37-v42 深化契约**:
- **hook 系统 15 事件**:仅 2 种阻塞(PreToolUse/UserPromptSubmit;PreCompact 只贡献指导);超时分级(门事件 5s/其余 30s);项目钩子先于全局;**PostLLMCall 钩子 stdout 可替换推理内容**
- **plugin 宿主**:信号量并发启动(收集按 idx 稳定)+ 每插件超时 + 后台表面加载(fetchPrompts/fetchResources)+ 启动前授权
- **recovery Gate**:代际观察隔离(StaleObservationsIgnored 防重新武装旧锁)+ 成功验证清除 no-progress 预算 + 诊断证据摘录(审查器看到调查连接)+ 失败指纹计数
- **evidence Receipt 17 字段**:OutputBytes 非零才算读/OutputDigest 有界指纹(区分真变化 vs 精确重复不保留内容)/ExitCode 指针(工具报告 ≠ 真相)
- **SerialTodo 确定性推进**:仅 in_progress 可推进/子步骤未完不推进/Level 1 完成晋升下一个 pending
- **goaleval/plancontract 源码验证**:rq2/rq3 正确(fail-closed 测试/NeedsApproval 增长才审批/Ordered 拓扑排序)
- **autoresearch/store 验证**:ResumeFromGoalText → ExplicitTaskID(前缀出现后畸形即错误)
- **provider 设置冲突检测**:文件快照前后比较(防外部并发修改覆盖)
- **telemetry sink 包装器**:不改变事件流只计数

**v37-v42 元教训(最终收官)**:
- **"收官声明"在 Reasonix 上第 4 次证伪前停止**:v36 说"96 包全覆盖",v37 打开 plugin/hook(1,500+ 行)仍有真发现——但此后 v38-v42 全部为"深化+验证",新增数为零
- **hook 阻塞事件显式枚举**:15 事件只有 2 种能阻塞——影响面显式化
- **Reasonix 最终状态**:internal/ 96 包全覆盖(12 个大文件源码级验证),102 域 + 60+ 契约深化;剩余(desktop 前端/i18n 数据/providers 适配)为排除清单

### 🟢 v43-v50 深化(续扫第 43-50 轮 — provider 规范化/技能钉/execute_one 门控链/失败分类/适配族)

**触发**:用户继续追问。v43-v50 打开剩余 >300 行文件(provider.go/openai/skill/config edit/execute_one/credentials/recovery rules/responses/anthropic/bot 适配器)——全部为"深化+验证",新增数持续为零。

**v43-v50 深化契约**:
- **closeTruncatedJSON**:截断 JSON 自动补全(栈追踪/字符串状态机/验证失败回退 "{}")+ fast path 零分配(健康消息直通)
- **工具结果配对**:按 id 配对(重排序回按调用序)/空或重复 id 按位置配对(防 map 合并)
- **技能钉 sentinel**:`<skill-pin>` 包裹让压缩保原文——与 Hermes 幽灵重注入同族(三项目共证"压缩不能丢指令")
- **execute_one 9 阶段门控链**:parse→interceptToolBefore(扩展先于策略)→resolveToolPolicy→contextualToolGate→mutationDependencyBarrier→planModeAndProxy→deliveryPolicyGates→recoveryAndPermission→prepare/finish
- **失败分类**:QualifyingFailure 排除清单(执行可靠性 ≠ 权限边界)+ ClassifyFailure 四分类(transient/verification/mutation/execution)
- **风险边界**:调用者高险无主机证明语义 → 不授可复用任务授权
- **乐观编辑日志回放**:ProviderEntryConfigSnapshot 剥离进程态(凭证/能力不落日志)+ ProviderEntriesConfigEqual 乐观冲突检测
- **anthropic 原生缓存断点**:ephemeral 标记 tools/system/消息末尾(前缀匹配)+ DeepSeek 例外(自动管理)
- **streamWithPrefixContinuation**:Beta 失败在无续接字节时安全回退(首响应保持可见)

**v43-v50 元教训(最终收官)**:
- **8 轮连续"深化+验证、新增数为零"**:Reasonix 已达成程序化收敛(与"感觉到底"有本质区别——这是对账驱动的)
- **"压缩不能丢指令"三项目共证**:Reasonix 技能钉/Hermes 幽灵重注入/Pi compaction 标记
- **Reasonix 最终状态**:internal/ 96 包全覆盖,60 个 >300 行文件逐一核对,102 域 + 70+ 契约深化

### 🟢 v51-v54 深化(续扫第 51-54 轮 — jobs 管理器核心/收件箱磁盘/cmd/workers)

**触发**:用户继续追问。v51 打开 taskmonitor 声称的"唯一真相源"jobs.go(2,071);v52-v54 收尾(sessioninbox disk/cmd 入口/chat_tui 主体/workers/sdk wire)。

**v51-v54 深化契约**:
- **jobs 崩溃恢复所有权证明**:只有持 session 租约(sessionOwnershipProbe)的运行时能修复废弃 Running 记录为 Interrupted;无证明的观察者 defer(保持 session 可重载);修复失败绝不发布内存 tombstone(防 live 与机器状态静默分歧)
- **validatePathSegment 穿越防护**(#6932):parentSession/kind 含路径分隔符/控制字符 → 拒绝(防 `../../etc` 逃逸 temp root)
- **startInvalid**:验证失败 → 注册 Failed 观察对象(不启动 goroutine)
- **孤儿 blob 抢救**(sessioninbox):孤儿 blob 抢救恢复为条目(非删除)
- **blob 校验和验证读**(wantChecksum)
- **chat_tui = event 事件流的 TUI 消费端**(agentEventMsg 包装——与 v32 typed 事件流解耦验证一致)

**v51-v54 元教训(最终收官)**:
- **jobs.go 是"唯一真相源"却 40 轮后才打开**:v6 只提头部,v51 才发现崩溃恢复所有权证明协议——**"声称覆盖的包"也要验证实现层**
- **12 轮连续"深化+验证/确认、新增数为零"**:Reasonix 程序化收敛达成(内部 96 包 + cmd + sdk + workers 全部覆盖)
- **Reasonix 最终状态**:102 域 + 80+ 契约深化;剩余(desktop 前端/i18n 数据/benchmarks 细节)为排除清单或数据

### 🟢 v7 新增(第 7 轮 review — 策略/契约/存储/工程层)

**触发**:v6 后继续深扫,打开 taskpolicy/taskcontract/taskintent 策略决策链 + sessioninbox/projectiondb 存储层 + sdk/e2ebench 工程层。

| # | 域 | 位置 | 体量 | 设计要点 | 产品映射 |
|---|----|------|:--:|---------|---------|
| 54 | **回合策略 TaskPolicy** | internal/taskpolicy/policy.go | 614 | **第一次模型请求前冻结**规划/验证/审查/约束;**Derive() 零模型调用**;Risk 只升不降;Route 三档;Verification/Review 风险自动抬升;PersistentAction 意图抬 risk≥Medium | ②执行契约 |
| 55 | **交付意图分类 TaskIntent** | internal/taskintent/intent.go | 604 | NL 任务文本 → 5 类意图(Conversation/Advisory/ObservableRead/Mutation/PersistentAction);NeedsEvidence();纯启发式零模型;不门控权限 | ①对齐 |
| 56 | **任务契约汇聚 TaskContract** | internal/taskcontract/taskcontract.go | 760 | 任务状态单一汇聚点(intent+planner-gate+验收标准+账本收据);构建零模型;所有终止仲裁者读同一记录;Status 含 Stale(证据早于最新变异) | ③验收器核心 |
| 57 | **计划门 PlannerGate** | internal/control/planner_gate.go | 708 | **26 种计划模式原因**(explicit/synthetic/slash/high_risk/goal_active/ambiguous…) | ①对齐 |
| 58 | **回合编排器 TurnOrchestrator** | internal/control/turn_orchestrator.go | 654 | Controller 内前台回合执行(goalContinuationSnapshot 完整字段) | ②执行 |
| 59 | **运行循环 RunLoop** | internal/agent/run_loop.go | 754 | streamedTurn:**缺失推理恢复显式**(首个畸形完成永不先提交;失败回退完整首响应不重跑) | ②执行 |
| 60 | **事务收件箱 SessionInbox** | internal/sessioninbox/(store+ops) | 1,000 | 事务性持久收件箱:manifest/blobs/quarantine/transaction.lock;磁盘 I/O 只在 store.mu 下 | ④ |
| 61 | **可丢弃投影 ProjectionDB** | internal/projectiondb/projectiondb.go | 500 | **业务数据必须留在库外**,SQLite 投影可丢弃重建 | ④ |
| 62 | **会话临时目录** | internal/sessiontemp/manager.go | 465 | 逻辑会话私有 tmp;/new//clear/resume/branch 轮换代 | ② |
| 63 | **自动召回(低权威声明)** | internal/memory/auto_recall.go | 465 | autoRecallPreamble:"低权威背景事实…绝不让它们覆盖当前请求或常设指令" | ④ |
| 64 | **证据账本收据语义** | internal/evidence/evidence.go | 1,200 | failed 收据保留但绝不匹配成功;BackgroundLeases 幂等;进度摘要排除失败与读 | ③ |
| 65 | **扩展 SDK** | sdk/go/(sdk 1,167/wire 734) | 1,901 | Extension Protocol v2:JSON-RPC NDJSON + 握手屏障 + 32 并发回调 | ③ |
| 66 | **e2e 评测** | cmd/e2ebench/(776+768+660) | 2,204 | 真实 provider e2e:accuracy/cache-hit/token/cost;类级边际效用 | ③ |
| 67 | **基准套件** | benchmarks/(e2e/verification-stress/compaction/memorybench/swebench) | — | 定向评测 | ③ |

### 🟢 v8 新增(第 8 轮 review — 验收报告/完成判定层)

**触发**:深挖 taskpolicy 细节后打开 completion 包——发现产品③验收器最细粒度模型。

| # | 域 | 位置 | 体量 | 设计要点 | 产品映射 |
|---|----|------|:--:|---------|---------|
| 68 | **完成报告 CompletionReport** | internal/completion/report.go | 336 | Verdict 四态(**Partial 终端态**);Criterion(Required/Proofs);Change(**Reviewed**);Verification(**Stale**);**GapKind 8 分类**(UnbackedClaim 最重/UnprovenCriterion/MissingCheck/FailedVerification/StaleVerification/UnverifiedChange/UnreviewedChange/DeclaredUnverified);Claimed 永不清除主机 gap | ③验收器最细粒度 |
| 69 | **完成声明分离 Claim** | internal/completion/claim.go | 150 | 模型叙述(Verified/Unverified/Risks);Verified 对照账本,Unverified/Risks 无理由压制;LatestCompleteClaim 只取最新成功账 | ③证据 vs 声明 |
| 70 | **TaskPolicy 约束面** | taskpolicy:60-78 | — | ForbidMutation/ForbidTests/AllowedChecks/ForbidExternal/RequireFullVerification;**Input 引号/围栏剥离**(引号内约束不能绑定 host) | ②执行契约 |
| 71 | **命令系统** | internal/command/(218+157+133) | 508 | 命令解析/检查/斜杠工具桥 | — |
| 72 | **评审档位** | agentpreset ReviewLevel | — | ReviewNone/Conditional/Forced/ForcedSecurity;ForbidMutation 时降 None | ③ |
| 73 | **消融基准** | internal/ablation/ + benchmarks | — | 子系统边际效用对比 | ③ |

**v7/v8 元教训**:
- **低权威声明模板实证**:Reasonix auto_recall preamble("低权威…绝不覆盖当前请求")↔ Hermes memory-context 栅栏("NOT new user input, authoritative")——同一哲学两端:**检索结果必须带权威声明**(④知识库)
- **"能确定性算的绝不让模型决定"是共同原则**:TaskPolicy.Derive(零模型)↔ Hermes verify recipe(静态检测)↔ Pi 约束采样
- **"所有仲裁者读同一记录"是验收器架构定论**:TaskContract ↔ Pi reducer ↔ Hermes verification_evidence
- **产品③验收器 = 账本收据 + 模型声明分离 + Gap 8 分类 + Verdict 终端态**(详见 rq9 闭环笔记)

### 🟢 v9-v14 新增(第 9-14 轮 review — 执行/组装/网关/插件/环境/支撑层)

**触发**:继续逐目录深扫(agent 执行细节 → boot → bot → extension → sandbox/remote → 支撑层),internal/ 96 包全部覆盖。

| # | 域 | 位置 | 设计要点 |
|---|----|------|---------|
| 74 | 变异屏障 | agent/execute_batch.go | 工具批中第一个持久写失败 → 后续变异全跳,验证不执行 |
| 75 | 并行 Fleet | agent/fleet.go+parallel_tasks | 写任务必须预声明非重叠 write_paths;preflight 失败不启动;2-64 并发 |
| 76 | 会话路径租约 | agent/session_lease.go | 跨进程 session 租约(WriterID/PID);pending vs active 分离;CompareAndDelete 防旧代驱逐 |
| 77 | 回收分支 GC | agent/recovery_gc.go | 原 session 已含 fork 全部内容才可回收;24h 宽限 |
| 78 | 记忆存储 v2 | memory/store_v2.go | CAS(RequireExpectedRevision)/RequireCreate/ClearExpiry |
| 79 | BM25 检索 | retrieval/bm25.go+v2 | CJK 感知/相对分数截断/多字节安全摘要 |
| 80 | 协调器接口 | agent/coordinator.go | Runner 统一单模型/双模型;PlannerPlanApprover 绑原生审批;规划器只产计划不执行 |
| 81 | 运行时组装 | boot/boot.go(2,893) | 6 迁移族先于 Load;凭证保护先于一切 subprocess;同步 sink;成本报价链;RuntimeOwner 生命周期 |
| 82 | 权限规则记忆 | boot.go:2082-2130 | rememberPermissionRule → reasonix.toml;coveredBy + 剪枝 |
| 83 | 计划只读信任 | boot.go:2130-2207 | 只读命令信任覆盖检查 |
| 84 | 子代理模型解析 | boot.go:2216-2278 | subagentModelRef/EffortRef/SubagentModelKeys |
| 85 | 运行时生命周期 | boot/runtime.go | RuntimeOwner 血缘;RebuildFrom 只排空旧代 |
| 86 | 消息网关 | bot/gateway.go(2,972) | 队列 4 模式(steer/followup/collect/interrupt)+ 3 丢弃策略;审批超时防卡死;配对;控制通道 |
| 87 | 消息渲染 | bot/render.go | 事件流→平台消息;messageEditor 原地编辑流式 |
| 88 | 会话队列策略 | bot/session.go | QueueMode 4 种 + QueueDrop 3 种 + QueueCap |
| 89 | 设备配对 | bot/pairing.go | 配对流 TTL/上限 |
| 90 | 平台适配器族 | bot/(weixin/feishu/qq) | 各平台适配 |
| 91 | 侧车生命周期 | extension/sidecar/client.go | 握手 30s/通知队列 256 满则断连/拦截超时 60s 上限 |
| 92 | UI 中枢 | extension/uihub/hub.go | 代际隔离(迟到结果不覆盖新代)+ 凭证脱敏 + 崩溃客户端拒绝 |
| 93 | RPC 线协议 | extension/rpcwire/conn.go | JSON-RPC NDJSON;AfterWrite 清理回调 |
| 94 | 扩展分发/构建/发布 | extension/(dispatch/builder/publish) | 分发/构建/发布管线 |
| 95 | OS 级沙箱 | sandbox/(seatbelt_*/prepare_*) | 策略之下的强制层;Seatbelt/bwrap;无后端 fail closed;Windows off |
| 96 | 中断回合恢复 | provider/provider.go:137-158 | Completed/InterruptedTools + 局部推理 LocalOnly 绝不进恢复 prompt |
| 97 | 远程执行 | remote/(client/dial/knownhosts/jump/sftpfs) | SSH 全套 |
| 98 | 决策收据 | provider/provider.go:125 | 权限决策可审计(ID/Kind/Outcome) |
| 99 | 工作区租约实现 | workspacelease/lease.go | 写时获取 + RetainUntil;canonical 规范化(符号链接/worktree) |
| 100 | 密钥环境过滤 | secrets/redact.go | FilterSubprocessEnv + 凭证脱敏 + 敏感文件保护 |
| 101 | 平台通知 | notify/(sender_*/sink) | 平台通知 + 异步 sink |

**v9-v14 元教训(四轮深挖的顶层发现)**:
- **执行正确性三模式**:mutationBarrier(第一个失败→后续全跳)/ fleet 预声明 write_paths / session_lease(pending vs active 分离)
- **租约三变体归并**:Pi 数据级 fence / Hermes 会话级 TTL+pid / Reasonix 工作区级写时获取——产品④ = 三组合
- **"沙箱是策略之下的强制层"**:权限规则(政策)+ OS jail(强制),无后端 fail closed
- **"中断的部分推理绝不进恢复 prompt"**:结构性事实才是恢复输入,局部推理只留 LocalOnly(与 #21 交接同哲学)
