# DeepSeek Harness 域发现(深度探索版)— v6 完成

> 项目:deepseek-ai/deepseek-harness(@deepseek-ai/dsh-root v0.1.0-rc.5,47f943859b)
> 版本:v6 — 2026-08-15(q1-q23 闭环笔记完成,95 设计)
> v1→v2:Review 轮 1(MCP 图谱)+ 轮 2(core/session + core/agent-loop 源码验证)确认核心域 1-6;补充 session 组/session-title 族
> v2→v3:q1-q12(58 设计)/ v3→v4:q13-q15(8 设计)/ v4→v5:q16-q20(16 设计)
> v5→v6:q21-q23 闭环笔记(Inbox+Tool-Calls/Code-Mode+E2B+Workspace+Host/Agent 生命周期,13 设计)
> 方法:Pass 0(AGENTS.md/README/architecture.md 全文)→ Pass 1(219 叶子包全扫描)→ MCP 索引(60,780 节点)→ 核心源码验证
> 核心特征:**一切皆插件**(vendored Cordis)+ 能力缝三角色(Service Definition/Provider/Consumer)+ 会话日志即上下文源

---

## 一、架构一句话

> **dsh = 基于 Cordis 的"一切皆插件"Agent Harness:插件贡献服务/类型化事件/可逆效果到共享上下文;无特权核心;每个能力是一个"能力缝"(Service Definition / Provider / Consumer 三角色);会话日志是上下文之源(模型可见 ⟺ 已记录)。**

**与 4 个已分析项目的本质差异**:
- Pi/OpenCode:单体执行引擎 + 事件发布
- Reasonix:长跑 + checkpoint
- **dsh:框架即产品**——核心是 Cordis 插件运行时,产品行为全部由可替换插件组成(profile/bundle 引导)

---

## 二、域清单(穷尽版,按包逐文件核对)

### 🔴 核心域(Cordis 运行时 + 产品脊柱)

| # | 域 | 位置 | 设计决策(一句) | 测试/文档 |
|---|----|------|----------------|---------|
| 1 | **Cordis 插件框架(vendored)** | vendor/(@deepseek-ai/cordis) | 插件 = 服务 + 类型化事件 + 可逆效果;registrations 是 effect(卸载自动回滚);waterfall 监听必须 next() | vendor/README + docs/cordis-primer |
| 2 | **Session 日志(事件源)** | core/session | 追加写 SessionEvent 日志(SessionEventMap 声明合并);模型可见 ⟺ 已记录(运行时不变量);deriveMessages() 投影模型历史 | docs/subsystems/session |
| 3 | **Agent + AgentLoop** | core/agent + core/agent-loop | Agent 接口 + live 注册表;turn(0+ steps)/step(1 request + tools);事件瀑布(agent/pre-step、agent/request、llm/stream、tools/*);turn-stopping 串行 | docs/agent-lifecycle |
| 4 | **Tools 注册表 + 执行管线** | core/tools | scoped registry + guarded execution;tool/call* → pre-execute → execute → post-execute → result;工具 UI 渲染意图是设计一部分 | docs/subsystems/tools + tool-execution-pipeline |
| 5 | **SystemPrompt 组装** | core/system-prompt | prompt-section + tool-schema 组装(插件注册 section) | docs/subsystems/system-prompt |
| 6 | **Scope(作用域注册)** | core/scope | per-agent scoped-registration 原语(isolate realm) | docs/subsystems/scope |
| 7 | **LLM 能力缝** | llm/(llm + llm-deepseek + llm-pi-ai + llm-retry + token-meter) | 消息/流词汇 + 适配器缝(ctx.llm);DeepSeek + Pi-AI 双 provider;重试 | docs/subsystems/llm-streaming |
| 8 | **Session 持久化/投影/遥测/标题** | session/(persistence-jsonl + persistence-sqlite + projection + projection-cache + telemetry + telemetry-otel + stats + checkpoint-policy + title + title-llm + title-first-prompt-llm + title-all-prompts-llm) | 双后端(JSONL/SQLite);投影 + 缓存;遥测(含 OTel);会话标题 provider 族(sole provider);SCHEMA_VERSION 单调 | docs/persistence-catalog |
| 9 | **Profile/Bundle 组合** | boot/app-boot + bundle/(base + headless + web-app) | 插件树从有序层组合;profile = 命名组合;bundle = 分发格式;patch 覆盖行配置 | docs/architecture §Profiles |
| 10 | **能力缝机制** | 跨包模式(每能力三包) | Service Definition / Provider / Consumer 三角色;换 provider 换整个产品(fs/subprocess 共享执行世界) | docs/capability-seams |

### 🟡 支撑域(能力缝实现)

| # | 域 | 位置 | 设计决策 |
|---|----|------|---------|
| 11 | **Sandbox** | sandbox/(sandbox + sandbox-local + sandbox-policy + sandbox-windows-acl)+ native/landlock-run | 沙箱能力缝;本地 provider + Landlock(landlock-run);spawn 前包装 argv |
| 12 | **Subprocess** | subprocess/(subprocess + subprocess-local) | 子进程能力 + 本地进程树 provider;shell 通过它 spawn |
| 13 | **Shell** | shell/(shell + bash-local + bash-sandbox + pwsh-local + pwsh-sandbox + shell-env + tool-bash + tool-bash-persistent) | bash/pwsh 双后端 × 本地/沙箱双模式;request/spec 分离模板 |
| 14 | **FS** | fs/(fs + fs-local + fs-sandbox + tool-fs + tool-fs-search + tool-str-replace-editor) | 文件系统能力 + 政策(fs/* 事件);本地/沙箱共享执行世界 |
| 15 | **Terminal** | terminal/ | 持久终端会话(ctx.terminals + dsh-tool-terminal) |
| 16 | **LSP** | lsp/ | 语言服务器能力 |
| 17 | **Skill** | skill/(skill + skill-badge + skill-filesystem + tool-skill) | 技能 provider 注册表 + 本地实现 + catalog/loader 工具 |
| 18 | **Web** | web/(web + web-search-* + web-fetch-* + tool-web) | 搜索/fetch providers + 工具 Consumer |
| 19 | **Subagent** | subagent/(subagent + 8 providers:acp/claude-code/codex/dsh-sdk/fork-in-process/in-process-driver/spawn-in-process) | 子 agent 能力缝;从全新子 agent 到另一产品委托 turn |
| 20 | **Workflow** | workflow/(workflow + worker-thread + tool-workflow + tool-ralph) | 工作流能力 + worker-thread provider + 工具 |
| 21 | **Compaction** | compaction/(compaction + compaction-basic + tool-result-pruner + command-compact) | 压缩能力 + basic provider + 工具结果修剪 |
| 22 | **Interaction(审批/权限/命令/提问)** | interaction/(commands + permission-presets + tool-ask-user + user-approval + user-questions) | 人工审批/交互能力;permission;ask-user |
| 23 | **Context 注入** | context/(agent-instructions + session-reference) | request-context 插件(agent 注入指令/会话引用) |
| 24 | **Guard(循环卫生)** | guard/ | loop-hygiene + tool-timeout 插件 |
| 25 | **Goal(目标)** | goal/ | 同 session 目标(ctx.goals);continue 通过 agent/* |
| 26 | **Jobs(后台任务)** | jobs/ | 后台工作(ctx.jobs);job_* 工具收集/停止 |
| 27 | **Schedule** | schedule/ | 调度 |
| 28 | **Todo** | todo/ | todo_write 工具 |
| 29 | **Plan** | plan/ | plan mode = 日志状态 |
| 30 | **Preset** | preset/ | per-session agent 组合(cordis.yml 预设) |
| 31 | **Settings** | settings/ | 用户设置能力 + file provider |
| 32 | **Credentials** | credentials/ | 凭证引用能力 + env/.env provider |
| 33 | **Identity** | identity/ | 匿名身份 |
| 34 | **Hooks(Claude/Codex 桥)** | hooks/ | Claude Code/Codex hook 桥 + wire-protocol 库 |
| 35 | **ACP** | acp/ | automation-only Agent Client Protocol 服务器 |
| 36 | **MCP** | mcp/ | MCP 集成 |
| 37 | **SDK(JSON-RPC)** | sdk/ | JSON-RPC 协议/服务器/TS 客户端 |
| 38 | **API(BFF + Typert)** | api/(gateway + remotes)+ typert/ | 远程 BFF 组装 + Typert RPC 网关;类型图生成器/运行时注册表 |
| 39 | **Storage/Spill** | storage/ + spill/ | 存储;溢出 |
| 40 | **Session Query** | session-query/ | 会话查询 |
| 41 | **Feedback** | feedback/ | 消息反馈 |
| 42 | **Runtime Diagnostics** | runtime-diagnostics/ | 运行时诊断 |
| 43 | **Self-Modification** | self-modification/(在 packages? 确认) | agent 检查/挂载自己的插件 |
| 44 | **Boot/Cmdline** | boot/(app-boot + cmdline) | 共享 app-bin 胶水;命令行解析 |
| 45 | **Client(Web 前端)** | client/(~30 包:connection/hmr/runtime/schema-form/ui-* 等) | Web 客户端(排除视觉,保留 connection/runtime/schema-form) |
| 46 | **Attachment** | attachment/(attachment + attachment-local) | 附件能力 |
| 47 | **Code-Runtime** | code-runtime/(code-runtime + worker-thread) | 代码运行时(E2B 相关?) |
| 48 | **E2B POC** | e2b/ | E2B 沙箱 POC(sandbox + FS/subprocess 适配器) |
| 49 | **Extensions** | extensions/ | 扩展 |
| 50 | **Host/Workspace** | host/ + workspace/ | 宿主;工作区 |
| 51 | **Util/Support/Examples** | util/ + support/ + examples/ | 零依赖工具;开发测试基建;演示 |

### 🟢 扫描域(简扫)

| 域 | 位置 | 说明 |
|----|------|------|
| Web 应用 | packages/web/ + website/ | 产品 Web UI(排除视觉) |
| Python SDK | python/ | Python SDK + 捆绑运行时 |
| native/landlock-run | native/ | Landlock 原生(系统编程面,D10 保留) |
| vendor/ | vendor/ | 锁定源码副本(了解机制即可) |

---

## 三、排除清单

| 排除 | 原因 |
|------|------|
| client/ui-*(~25 包) | 前端视觉组件(保留 connection/runtime/schema-form 语义) |
| packages/web/ + website/ | 产品 Web UI |
| python/ | Python SDK(如需要单列) |
| vendor/ 内部实现 | 上游 Cordis 源码(机制由 docs/cordis-primer 说明) |
| .agents/notes/ | 决策记录(可按需引用,不深挖) |
| scripts/ | 仓库门禁/生成器 |

---

## 四、关键发现(初步)

### 发现 1:框架即产品(与 4 项目本质差异)
- 核心是 **vendored Cordis**(插件运行时),不是执行引擎
- 产品行为全部 = 可替换插件(模型适配器/工具注册表/会话日志/agent 循环本身都是插件)
- "There is no privileged core to patch"——扩展 = 在旁边挂插件
- **产品启示**:这是"对齐模块/执行引擎/验收器/知识库全插件化"的极端样本——可替换性即架构

### 发现 2:能力缝三角色(换 provider 换产品)
- Service Definition(接口)/ Provider(实现)/ Consumer(工具)
- fs/subprocess 共享执行世界 → 指向远程沙箱则 Bash/PTY/LSP 一起迁移
- subagent 从"子 agent"到"另一产品委托"同接口
- **产品启示**:产品②执行引擎的工具/权限/沙箱可做能力缝(学习模式 vs 写书模式 = provider 切换)

### 发现 3:会话日志 = 上下文之源
- "Model-visible means logged"(运行时不变量)
- 新模型可见输入 ⟹ 新 session 事件(SessionEventMap 声明合并)
- deriveMessages() 投影模型历史;raw assistant/chunk 保留回放保真
- **产品启示**:与 OpenCode 的"事件溯源 + 投影"同哲学,但更严格(不变量强制)

### 发现 4:事件三类
- Session events(durable 日志)/ Agent events(agent/* live)/ Capability events(fs/*、tools/*)
- waterfall(必须 next())vs serial(无 next())
- **产品启示**:验收器可挂 agent/request + tools/* 瀑布

### 发现 5:Turn/Step 双层
- turn(0+ steps,开于首输入,闭于无事可做)/ step(1 request + tools)
- turn 的闭合即使 0 step 也记录(日志记录尝试)
- **产品启示**:与 OpenCode 的 shouldRun/needsContinuation 同构但事件命名不同

### 发现 6:预发布无兼容承诺
- SESSION_FORMAT_VERSION = 0;SQLite SCHEMA_VERSION 单调
- 可自由重命名/重打包
- **产品启示**:这是"还在演变"的项目——参考其机制而非版本稳定性

### Review 轮 1-2 验证(源码证据)

- **Session 事件四类**(core/session/src/index.ts:37-87):`session/created`(同步 veto 回滚)/`session/disposed`(离开 store,含发布回滚)/`session/event`(post-commit fire-and-forget append feed)/`session/flush`(并行耐久检查点)——persistence 是插件关注点(订阅 event,drain 时 flush)
- **Scope 过滤分派**:agent-scoped 监听器只收该 agent 的 session(@deepseek-ai/dsh-scope)
- **ReactLoopAgent 状态机**(core/agent-loop/src/agent.ts):Phase = idle | maintenance | running{turn, step, abort, wakeRequested};InboxTarget = next-turn | next-step;wake 在 idle 时总是开新 turn 边界;"Every request is derived from the session log"
- **SESSION_FORMAT_VERSION = 0**(预发布无兼容承诺)

### 发现 7:会话标题 provider 族(sole provider 模式)
- session-title 是唯一注册者(sessionTitle sole provider 契约)——"generate session titles: register the sole ctx.sessionTitle provider"(architecture.md:124)

---

## 五、闭环笔记清单(已完成,58 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q1 | session-log | 6 | 事件四类(created veto/event feed/flush)/SessionEventMap 声明合并/Surface 投影(append/replace 阴影)/writer 决定 bump/模型可见⟺已记录/chunk 打包 |
| q2 | agent-loop | 5 | 融合分派三模式(subject 与 scope 不分歧)/Turn-Step 双层/Phase 状态机/InboxTarget 双队列/pre-step 决定模型所见 |
| q3 | tools | 5 | 五事件管线(pre/post/execute/code-dispatch-log/result)/守卫管线(取消重查+ask+guardReason)/ToolDefinition/code-dispatch-log 只改日志副本/呈现层 |
| q4 | cordis | 5 | 五想法(插件/上下文/inject/事件/可逆效果)/四分派模式/waterfall 短路即设计/Loader 配置(!!js)/归属规则 |
| q5 | llm | 5 | llm/stream 瀑布/冻结请求(纯函数)/LlmError 分类/双适配器(deepseek+pi-ai)/消息词汇+重试 |
| q6 | profile-bundle | 4 | 六层层序/patch 失败策略(文件硬错行级警告)/最后写赢+整行替换/dump-config |
| q7 | capability-seams | 4 | 三包结构/缝事件(fs/write-intent)/共享执行世界/三角色完整性 |
| q8 | persistence | 5 | WriteBehind 批量+耐久屏障/后端抽象(TornMarker)/双后端(JSONL+zstd vs SQLite)/损坏显式错误/投影+检查点 |
| q9 | interaction | 5 | approval/request 瀑布+四结局/ApprovalPolicy(ask\|never+fail-closed)/durable 审计事件/政策切换 LAST=override/权限预设 |
| q10 | sandbox | 5 | 三模式+每调用政策/严格升级阶梯/ConfinedArgv(方言签名)/fail-closed/Landlock+Windows ACL |
| q11 | compaction-skill-subagent | 4 | 三缝样本(低/中/高复杂度)/subagent 7 providers/compaction agent 上下文/skill 政策+排序 |
| q12 | sdk-api-typert | 5 | SDK JSON-RPC 三件/Typert 类型图生成器(6245)/Hook 桥/ACP 自动化服务器/MCP |

### 第二轮(q13-q15,8 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q13 | guard-goal-jobs | 4 | 超时=execute 瀑布包装+TOOL_TIMEOUT/repeat 提醒(3/5/8 阈值 advisory)/Goal(goal/change 事件)/Jobs+Schedule 事务化 |
| q14 | settings-credentials | 4 | Settings 命名空间+事件(源可追溯)/Credentials 4 操作+空值即缺席/匿名身份/Title sole provider+LLM 变体 |
| q15 | support-domains | 6 | 指令投影(两提交边界+串行化)/Storage 后端注册表+KV/Spill 溢出/Session-Query 检索/小域 |

### 第三轮(q16-q20,16 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q16 | repair-terminal-lsp | 5 | 修复算法(扫描→错误结果→合成边界)/双恢复码+模型可见指导(只读/幂等才重试)/确定性合成/Terminal 缝/LSP 缝 |
| q17 | projection-coordinator | 4 | 投影五元契约(纯折叠+stateVersion)/快照水印+检查点非权威/协调器 seq 校验/遥测 |
| q18 | web-hooks-client | 3 | Web Fetch 政策(URL/同源)/Hook Runner(永不 throw)/Client 连接(JSON-RPC/WS) |
| q19 | llm-adapters | 4 | DeepSeek 预算常量/HTTP→稳定错误码/翻译序列化/Pi-AI 回放状态 |
| q20 | session-query-workflow | 4 | 逻辑语料(live 优先+借源契约)/5 检索操作/Workflow worker-thread/停止三态 |

### 第四轮(q21-q23,13 设计)

| # | 笔记 | 设计数 | 关键发现 |
|---|------|:--:|---------|
| q21 | inbox-tool-calls | 4 | Inbox 增量投影(replay-once)/六突变+归一化校验/durable 先于 live/调度器(屏障+滚动池+abort 合成) |
| q22 | code-mode-e2b | 5 | run_code(语言感知 schema+run-scoped abort)/E2B 远程沙箱 POC/Code-Runtime 保留字安全/Workspace 实体/Host |
| q23 | agent-lifecycle | 4 | 加载时身份冲突预校验/create-resume 工厂+abort 竞速/FactoryOwnership 有序 teardown/每读并行上限 |

**合计:95 设计**(每份 2 轮 review 起,核心域含源码验证)
