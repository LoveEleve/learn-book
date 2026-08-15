# Hermes 域发现 v1(Pass 0+1:设计文档 + 目录结构)

> 项目:NousResearch/hermes-agent(main)
> 版本:v1 — 2026-08-14,基于 AGENTS.md + README + 目录扫描
> 流程:Pass 0(AGENTS.md 全读,含贡献规范/项目结构/各子系统架构)→ Pass 1(目录扫描 + 文件计数 + 关键文件定位)

---

## 一、项目画像(Pass 0 结论)

**一句话**:Hermes 是"self-improving agent"——同一 AIAgent 内核跑在 CLI/消息网关/TUI/桌面四形态,带学习循环(记忆 + 技能 + 会话搜索 + 学习图谱),扩展走插件/技能不进核心。

**两大宪法性设计约束**(AGENTS.md 明确):
1. **Per-conversation prompt caching is sacred**——长会话复用缓存前缀;任何变异历史上下文/换工具集/重建 system prompt 都作废缓存 → 禁止。唯一例外:上下文压缩。
2. **核心是窄腰,能力在边缘**——每个核心工具每轮都随 API 发送;新能力优先走:扩展现有代码 → CLI+技能 → service-gated 工具 → 插件 → MCP catalog → 新核心工具(最后手段)。

**规模**:4,138 个 Python 文件(不含 node_modules/.git)+ TS 桌面/TUI + Rust 安装器。
**测试**:~17k 测试 / ~900 文件(AGENTS.md 声明,May 2026)。

---

## 二、目录结构(Pass 1 扫描)

```
hermes-agent/
├── run_agent.py            # AIAgent 类 — 核心对话循环(8,425 行)
├── cli.py                  # HermesCLI — 交互 CLI 编排(19,269 行)
├── hermes_state.py         # SessionDB — SQLite 会话存储 + FTS5(11,605 行)
├── hermes_state_search.py  # 会话搜索(2,492 行)
├── hermes_state_schema.py  # schema(63,314 字节)
├── model_tools.py          # 工具编排 discover_builtin_tools/handle_function_call(1,617 行)
├── toolsets.py             # 工具集定义 TOOLSETS + _HERMES_CORE_TOOLS(1,040 行)
├── trajectory_compressor.py# 轨迹压缩(训练数据)
├── batch_runner.py         # 并行 batch
├── mcp_serve.py            # MCP 服务器
├── agent/                  # Agent 内部(~118K 行,见下)
├── hermes_cli/             # CLI 子命令/插件加载/皮肤引擎
├── tools/                  # 工具实现(registry 自动发现)
│   └── environments/       # 终端后端:local/docker/ssh/modal/daytona/singularity/vercel_sandbox
├── gateway/                # 消息网关(run.py + session.py + platforms/20+)
├── plugins/                # 插件(memory/context_engine/model-providers/kanban/observability...)
├── cron/                   # 调度器(jobs.py + scheduler.py)
├── skills/                 # 内置技能(15 类)
├── optional-skills/        # 可选技能(23 类)
├── ui-tui/                 # Ink(React)终端 UI
├── tui_gateway/            # Python JSON-RPC 后端(TUI)
├── acp_adapter/            # ACP 服务器(VS Code/Zed/JetBrains)
├── apps/desktop/           # Electron 桌面应用
├── web/                    # 网页 dashboard
├── tests/                  # pytest(~900 文件)
└── docs/                   # ADR/设计/RFC
```

---

## 三、域清单 v1(粗扫,基于目录 + 架构图)

### 🔴 候选核心域(与产品四组件直接相关)

| # | 域 | 位置 | 产品映射 |
|---|----|------|---------|
| 1 | 记忆系统 | tools/memory_tool.py + agent/memory_manager.py + memory_provider.py + plugins/memory/ | ④知识库 |
| 2 | 技能系统 | tools/skills_tool.py + skill_manager_tool.py + skill_commands.py | ④知识库 |
| 3 | 技能生命周期 | agent/curator.py + tools/skill_usage.py | ④知识库治理 |
| 4 | 技能分发 | tools/skills_hub.py + skills_guard.py | ④知识准入 |
| 5 | 会话存储 | hermes_state.py SessionDB | ④知识库存储 |
| 6 | 会话搜索 | hermes_state_search.py + tools/session_search_tool.py | ④全书检索 |
| 7 | 上下文压缩 | agent/context_compressor.py + conversation_compression.py + native_compaction.py | ②执行 |
| 8 | Agent 主循环 | run_agent.py + agent/conversation_loop.py | ②执行 |
| 9 | 工具编排 | model_tools.py + toolsets.py + tools/registry.py + agent/tool_executor.py | ②执行 |
| 10 | 委派/子代理 | tools/delegate_tool.py + async_delegation.py + agent/subagent_lifecycle.py | ②执行 |
| 11 | 验证系统 | agent/verify/(runner/recipes)+ hermes_cli/verify_cmd | ③验收器 |
| 12 | 终端后端 | tools/environments/(7 种) | ②执行环境 |

### 🟡 支撑域(粗扫)

| # | 域 | 位置 |
|---|----|------|
| 13 | 提示词构建/缓存 | agent/prompt_builder.py + system_prompt.py + prompt_caching.py + prompt_cache_boundary.py |
| 14 | 凭证管理 | agent/credential_pool.py + secret_scope.py + credential_sources/ |
| 15 | Provider 适配 | agent/transports/ + plugins/model-providers/(40+ 目录) |
| 16 | 学习图谱 | agent/learning_graph.py + learning_mutations.py + learn_prompt.py |
| 17 | Cron 调度 | cron/jobs.py + scheduler.py + scheduler_provider.py |
| 18 | Kanban 多代理 | plugins/kanban/ + tools/kanban_tools.py + hermes_cli/kanban.py |
| 19 | 网关会话 | gateway/session.py + run.py + session_state.py |
| 20 | 工具守卫/审批 | tools/approval.py + write_approval.py + path_security.py + tirith_security.py + agent/tool_guardrails.py |
| 21 | MoA 聚合 | agent/moa_loop.py + moa_trace.py |
| 22 | 计费/用量 | agent/usage_pricing.py + billing_* + nous_rate_guard.py |
| 23 | 消息规整/脱敏 | agent/message_sanitization.py + redact.py + think_scrubber.py |
| 24 | 背景审查 | agent/background_review.py + verification_evidence.py + verification_stop.py |
| 25 | 轨迹生成 | batch_runner.py + trajectory_compressor.py + agent/trajectory.py |
| 26 | 插件系统 | hermes_cli/plugins.py + agent_plugins.py + plugins/ |
| 27 | CLI 斜杠命令 | hermes_cli/commands.py + cli.py process_command |
| 28 | **运行时支撑共享包(v2 新增)** | agent/agent_runtime_helpers.py(4,199 行) | 
| 29 | **API 调用共享包(v2 新增)** | agent/chat_completion_helpers.py(4,724 行) |
| 30 | **验证证据账本(v3 新增)** | agent/verification_evidence.py + verify_hooks.py + verification_stop.py | **验收结果留痕(record_verify_run: ok/scope=full\|targeted/输出)** — 产品③"验收器可审计"直接蓝本 |
| 31 | **回合租约(v4 新增)** | gateway/turn_lease.py | **每 session 回合租约** — 多 routing key 映射同一 session_id 时串行化 [load history → run → flush];generation-scoped + identity-checked release(#64934) |
| 32 | **会话停滞通知(v4 新增)** | gateway/session_stall.py | 停滞策略门(pending inbound + stale progress),消费共享活动观察契约(observation-only) |
| 33 | **服务端压缩(v5 新增)** | agent/native_compaction.py | OpenAI Responses server-side compaction(gpt-5.6 仅直接路由);本地压缩保持全副武装作 fallback 所有者,原生阈值钳到本地触发器之下 |
| 34 | **评测 harness(v5 新增)** | evals/readtool/(runner/tasks/fixtures/report) | **A/B 评测**:真实 AIAgent 跑确定性 hostile 工作区,测 accuracy/api_turns/tool_calls/total_tokens/wall_s——产品③评测直接蓝本 |
| 35 | **交付义务账本(v6 新增)** | gateway/delivery_ledger.py + delivery.py | **at-least-once 投递**:最终响应生成→交付确认的状态机(pending/attempting/delivered/failed)+ 崩溃恢复 sweep + 可见 recovered-reply 标记(诚实 at-least-once,绝不静默重复,issue #61790) |
| 36 | **生命周期账本(v6 新增)** | gateway/lifecycle_ledger.py + shutdown_forensics.py + shutdown_watchdog.py | **脏死检测状态机**:gateway.lifecycle.json 哨兵(phase=running/exited);SIGKILL/OOM 后下次启动发现"从未到达退出路径";内存采样(<1ms /proc)作临终遥测 |
| 37 | **监控平面(v6 新增)** | agent/monitoring/(emitter/events/otlp_exporter/gateway_health) | **Content-free 事件**:只有 gateway_health/gateway_diagnostic(无 prompt/消息/工具参数);OTLP 导出可选依赖;dispatcher 线程 fail-isolated |
| 38 | **会话洞察引擎(v6 新增)** | agent/insights.py(1,212 行) | 历史 session 数据 → token/成本/工具模式/活动趋势报告(Claude Code /insights 启发,多平台适配) |
| 39 | **错误分类学(v6 新增)** | agent/error_classifier.py(1,905 行) | **FailoverReason 枚举(23 类)**:auth/billing/rate_limit/overloaded/timeout/context_overflow/payload_too_large/thinking_signature…每个失败→恢复策略(auth 永久→abort、context_overflow→compress 不是 failover、ssl→fail fast 不烧重试) |
| 40 | **文件系统检查点(v6 新增)** | tools/checkpoint_manager.py(1,976 行) | **git-backed 自动快照**:每回合每目录最多一个;工具写前 ensure_checkpoint;restore/rollback;容量上限(500MB/20 快照)防爆炸;大小门槛(10MB 文件跳过) |
| 41 | **背景审查 fork(v6 新增)** | agent/background_review.py(1,144 行) | **每轮后记忆/技能自动审查**:fork AIAgent 回放对话快照问"该存什么?";工具白名单限 memory/skill 管理工具,**其余运行时拒绝**;写直达记忆+技能库,主会话/缓存不动 |
| 42 | **NeMo Relay 适配(v6 新增)** | agent/relay_llm.py + relay_runtime.py + relay_tools.py | 物理 provider 尝试的会话级代理:session 隔离/凭证 scope 旋转/subagent 注册/托管执行 |
| 43 | **ACP 权限桥(v6 新增)** | acp_adapter/(permissions/server/tools/session/provenance) | ACP 权限选项↔Hermes 审批结果映射(allow_once/session/always/deny);编辑审批/工具结果格式化/会话管理 |
| 44 | **TurnRunner 协作器(v7 新增)** | gateway/run.py:3834 TurnRunner | 工具进度回调从 GatewayRunner._run_agent_inner 闭包重构为协作器;live status 行/log 队列/首次长工具提示;TurnContext 共享字段 |
| 45 | **网关并发会话治理(v7 新增)** | gateway/run.py(6200 GatewayRunner + mixins) | 并发 session 槽位(_claim_active_session_slot)、fallback 链应用、agent 缓存快照、重启恢复 drain 超时、子代理活性检查 |
| 46 | **SWE 评测 runner(v7 新增)** | mini_swe_runner.py(28K) | Hermes 轨迹格式 SWE runner;local/docker/modal 环境;batch JSONL;与 trajectory_compressor 管线兼容;严格采样契约模型温度处理 |
| 47 | **MCP 服务器(v7 新增)** | mcp_serve.py(37K) | OpenClaw 9 工具 MCP 通道桥面(conversations_list/messages_send/permissions_respond)+ channels_list;让外部客户端(Claude Code/Cursor)通过 MCP 操作 Hermes |
| 48 | **Cron 调度器抽象(v7 新增)** | cron/scheduler_provider.py + scheduler.py + jobs.py + lifecycle_guard.py | **CronScheduler ABC**(Axis-B 触发;执行/交付共享,provider 不重实现);运行中 job 注册/中断;**lifecycle_guard 防网关自杀循环**(拒绝含 gateway restart 命令的 job,command-shaped 锚定) |
| 49 | **工具结果持久化(v7 新增)** | tools/tool_result_storage.py | **三层防溢出**:per-tool cap(工具内截断)/ per-result 持久化(超阈值全量写 sandbox 临时目录,上下文替换为预览+路径)/ per-turn 聚合预算(200K chars,大结果溢出到盘) |
| 50 | **跨代理文件协调(v7 新增)** | tools/file_state.py | 并发子代理写同文件防损坏:read 戳/全局最后写者/每路径锁;subagent B 写 A 已读的文件 → A 的 stale 写被拦截 |
| 51 | **写审批子系统(v7 新增)** | tools/write_approval.py + skill_provenance.py | **通用写门控**:MEMORY/SKILLS 两子系统;stage/list/discard/evaluate;GateDecision 三态(allow/blocked/stage);**写来源 ContextVar**(write_origin → is_background_review 区分自主/交互写) |
| 52 | **网关授权混入(v7 新增)** | gateway/authz_mixin.py + pairing.py | 平台级授权适配器:DM/group 策略、发送者 allowlist、配对存储、upstream 授权 vs 本地策略 |
| 53 | **紧急停止 ESTOP(v8 新增)** | agent/estop.py | **可恢复暂停(只停新工作)**:哨兵文件 $HERMES_HOME/ESTOP;cron/kanban/gateway 新回合跳过,在途工作永不杀;单 os.stat 检查无缓存;损坏文件仍计为已启用(fail safe);移植 gastown estop.go |
| 54 | **回合终结器(v8 新增)** | agent/turn_finalizer.py + turn_retry_state.py + turn_summary.py | run_conversation 尾部(预算耗尽总结/轨迹保存/会话持久化/响应转换/记忆技能审查触发)提取为独立 seam——god-file 分解运动产物 |
| 55 | **子代理生命周期服务(v8 新增)** | agent/subagent_lifecycle.py(540 行) | **launch/status/wait/cancel/reconnect 完整生命周期** + SubagentState 枚举 + 注册表/父代理绑定 |
| 56 | **秘密作用域(v8 新增)** | agent/secret_scope.py + secret_sources/(bitwarden/1password/command) | **profile 隔离的密钥解析**:scope 覆盖 + UnscopedSecretError + 外部密码管理器源 |
| 57 | **Kanban 目标审查器(v8 新增)** | tools/kanban_tools.py:_goal_judge(233-256) | **goal_mode 独立审查器**(auxiliary goal_judge 模型)——与 Reasonix boundedllm 独立审查器同构;不可用时 fail open |
| 58 | **平台一致性契约(v8 新增)** | tests/conformance/vectors/ + scripts/generate_conformance_vectors.py | **原生渲染器即 oracle(可执行规范)**:生成器产出 slack/discord/telegram/whatsapp 向量,connector 一致性 runner 消费——测试即契约的正式化 |
| 59 | **浏览器监督器(v8 新增)** | tools/browser_supervisor.py + browser_cdp_tool.py | 持久 CDP 监督(每 task_id 一个):dialog/框架树检测;不在工具 schema 中,经 browser_snapshot/browser_dialog 双通道到达 agent |
| 60 | **工具搜索桥(v8 新增)** | tools/tool_search.py + tool_dispatch_helpers.py | 工具目录分类(核心/可延迟)+ token 预算 → 延迟加载;桥接 unwrap 让钩子看到真实工具 |
| 61 | **存储 schema 单一真相源(v9 新增)** | hermes_state_common.py(SCHEMA_SQL)+ hermes_state_schema.py + hermes_state_portability.py | **Beets/sqlite-utils 模式**:SCHEMA_SQL 单一真相源,启动时 reconcile 增列;sessions/messages/session_model_usage/compression_locks/async_delegations 全表;messages active/compacted 双标记;portability mixin 导出/导入/血缘 |
| 62 | **会话存储完整实现(v9 新增)** | gateway/session.py(1,444 行)SessionStore | routing 代际(generation 全序)/快照+增量双轨持久化/legacy sessions.json 镜像/测试隔离硬失败(不吞进 JSONL fallback);AsyncSessionStore 门面 |
| 63 | **命令安全审批(v9 新增)** | tools/approval.py(2,500+ 行)+ url_safety.py + website_policy.py | hardline rm/路径折叠/sudo stdin 保护/执行标志扫描($()/backtick)/shell 分段解析;**智能审批**(auxiliary LLM 决策 + 连续拒绝熔断);SSRF/站点策略 |
| 64 | **进程注册表(v9 新增)** | tools/process_registry.py + daemon_pool.py | 进程会话管理(systemd scope/输出监听/模式匹配)+ 守护线程池(解释器退出不阻塞) |
| 65 | **工具输出上限(v9 新增)** | tools/tool_output_limits.py + ansi_strip.py | 工具输出字节/行/行长三上限 + ANSI 剥离——防输出洪泛 |
| 66 | **tirith 供应链安全(v9 新增)** | tools/tirith_security.py | cosign 签名校验(证书/签名/身份正则)才允许自动安装——供应链准入 |
| 67 | **插件族内部(v9 新增)** | plugins/(langfuse 可观测/teams_pipeline 会议管线/security-guidance 模式库/achievements 游戏化/context_engine) | 插件契约实例:plugin.yaml + 事件订阅 + PluginState 配额;teams_pipeline 是完整领域管线(meetings/models/runtime/store/subscriptions) |
| 68 | **辅助客户端路由(v10 新增)** | agent/auxiliary_client.py(10,432 行) | **任务级 provider 解析**:auxiliary.<task>.{provider,model} 覆盖;resolve_provider_client 中央路由(chat_completions/codex/anthropic/bedrock 适配 shim);中断保护/温度契约(_fixed_temperature_for_model)/模型近距目录 |
| 69 | **CLI 编排(v10 新增)** | cli.py(19,269 行)HermesCLI(4324) | **Mixin 架构**(CLIAgentSetupMixin/CLICommandsMixin/CLIBillingMixin);process_command 斜杠分发(10464);ChatConsole(3993)/_SkinAwareAnsi;busy_input_mode(interrupt/queue/steer 三模式) |
| 70 | **平台适配器抽象(v10 新增)** | gateway/platforms/base.py BasePlatformAdapter(4,400+ 行) | **平台能力抽象**:send/edit/delete/draft streaming/审批/clarify/私信/媒体/平台锁(acquire_scoped_lock 防跨 profile 凭证冲突)/消息长度函数/markdown 转平台格式 |
| 71 | **MoA 多层聚合(v10 新增)** | agent/moa_loop.py(2,453 行) | 并行引用槽(slot)+ 聚合器;参考消息裁剪/工具结果预算/隐私模式(redact reference outputs);preset 温度 |
| 72 | **批量 runner 断点续跑(v10 新增)** | batch_runner.py(28K) | **checkpoint 断点续跑**:completed_prompts 索引 + **按内容匹配恢复**(索引对不上时按 prompt 文本扫描,失败条目跳过重试) |
| 73 | **轨迹压缩边界保护(v10 新增)** | trajectory_compressor.py | **边界干净**(不切分 gpt tool_call/tool 响应对)+ 保护首轮/尾 N 轮 + token 化/摘要——与上下文压缩边界对齐同构 |
| 74 | **缩放至零(v11 新增)** | gateway/scale_to_zero.py(232)+ wake.py(184) | **serverless 休眠**:空闲判定(无运行 agent/无入站/无后台工作)+ go_dormant quiesce + 自挂起(own the suspend call,防 Fly 在 in-flight agent 回合中挂机);唤醒双策略(push 适配器注入 synthetic 事件 / stateless 适配器 self-POST 原会话) |
| 75 | **流式事件分发(v11 新增)** | gateway/stream_dispatch.py + stream_events.py | **adapter 驱动的事件分发**:agent 发 typed 事件(Commentary/GatewayNotice/MessageChunk),adapter 决定交付(原生 draft/edit-in-place);adapter 可 eat 事件(无法渲染工具 chrome 的平台);无平台知识/无 asyncio 的薄同步路由 |
| 76 | **推理摘要边界修复(v11 新增)** | agent/reasoning_summaries.py(67) | 推理摘要模型流式边界:chat wire 无 summary_index → 从"delta 开 bold 标题"信号重推导边界(vercel/ai#6742 同问题) |
| 77 | **自仓库保护(v11 新增)** | tools/self_repo_guard.py(722) | **检测会重写本进程支撑 checkout 的 git 操作**:checkout/switch/rebase/merge/pull/clean/bisect 等 worktree 变异 → 拦截(模块版本漂移危害) |
| 78 | **排空控制标记(v11 新增)** | gateway/drain_control.py(370) | dashboard→gateway 的外部排空标记契约(无 HTTP 控制通道,用 marker 文件 + epoch 防跨实例误判) |
| 79 | **富消息回显索引(v11 新增)** | gateway/rich_sent_store.py | Telegram rich message 不回显 content → 本地 message_id→text 索引,reply 时查回显 |
| 80 | **渠道目录(v11 新增)** | gateway/channel_directory.py | 每平台可达渠道缓存(5 分钟刷新)→ send_message 名称解析 |

### 🟢 扫描域(简扫)

| 域 | 位置 | 说明 |
|----|------|------|
| TUI/桌面/网页 | ui-tui/ + apps/desktop/ + web/ + tui_gateway/ | 前端视觉层,保留 RPC 契约参考 |
| ACP 适配器 | acp_adapter/ | VS Code 集成 |
| 语音/TTS/图像 | tts_*/voice_mode/wake_word/image_gen_* | 多媒体边缘 |
| 安全审计 | tools/tirith_security.py + hermes_cli/security_audit | 命令注入扫描 |

---

## 四、排除清单(v1 暂定)

| 排除 | 原因 |
|------|------|
| apps/desktop/src + ui-tui/ + web/ 全部 TSX 组件 | 前端视觉层 |
| gateway/platforms/ 20+ 平台适配器 | 同构重复,保留 base.py/session.py 抽象 |
| plugins/model-providers/ 40+ 目录具体实现 | 同构重复,保留 transports 抽象 |
| 语音/TTS/图像/视频工具 | 产品无关边缘 |
| tests/ | 作为行为契约证据使用,不列为独立域 |

---

## 五、待验证问题(v2 状态)

- [x] Q1(答):agent_runtime_helpers(26 处引用,4,199 行)+ chat_completion_helpers(8 处,4,724 行)是主循环的共享支撑——**已并入域清单**
- [x] Q2(答):plugins/memory/ 8 个外部 provider(honcho/mem0/supermemory/hindsight/holographic/byterover/retaindb/openviking)——深挖 1 个(honcho)作为契约实例,其余按同构排除
- [x] Q3(答):verify 系统 = hermes_cli/verify_cmd.py → agent/verify/(runner/recipes)+ **verification_evidence 证据账本**(record_verify_run:ok/scope full|targeted/output)。产品③直接蓝本:验收结果留痕 + partial 降级语义
- [ ] Q4:conformance 测试目录(tests/conformance/)测什么契约?(已见 vector_generator + 平台向量 discord/slack/telegram/whatsapp)——测试即契约层,深挖 hq 时按需
- [x] Q5(答):ContextEngine ABC 目前只发现 ContextCompressor 一个实现;native_compaction 是 OpenAI 服务端压缩(非 ContextEngine 子类,是窄路由替代)——压缩域 = compressor(本地)+ native(服务端)双路径
- [x] Q6(答):tests/ 2,906 个测试文件定义行为契约;memory/skills/state/verify 各有专测目录——作为"测试即行为契约"证据使用
- [x] Q7(答):turn_lease = 每 session 回合租约(串行化 load→run→flush);session_stall = 停滞通知策略门

---

## 六、v1/v2 review 记录

| 轮次 | 层面 | 新增 | 发现 |
|:--:|------|:--:|------|
| v1 | 目录层 | 27 | 粗扫完成 |
| v2 | 共享包层 | +2 | **agent_runtime_helpers(4,199 行,26 处引用)** — API 客户端创建/凭证池恢复/fallback 恢复/消息序列修复(prompt 缓存断点规划)/轨迹转换;**chat_completion_helpers(4,724 行,8 处)** — 非流式/流式 API 调用/fallback 激活/超时管理/stale-kill |
| v3 | 测试契约层 | +1 | **verification_evidence 证据账本** — 验收结果留痕(ok/scope full|targeted);verify 系统 = cmd → runner/recipes → 证据账本三层;tests/conformance/ 含平台向量(discord/slack/telegram/whatsapp) |
| v4 | 接口/网关层 | +2 | **turn_lease 回合租约** — 多 routing key→同一 session_id 的并发回合串行化(#64934);**session_stall 停滞通知策略门** |
| v5 | JD 关键词覆盖 + 引擎族 | +2 | **native_compaction 服务端压缩**(gpt-5.6 窄路由,本地压缩仍为 fallback 所有者);**evals/readtool 评测 harness**(真实 agent A/B,accuracy/tokens/耗时指标);JD 关键词全命中(memory/skill/context/eval/verify/sandbox/permission) |
| v6 | 深扫层(文档/账本/监控/错误/检查点/审查/中继/ACP) | +9 | **delivery_ledger 交付账本**(at-least-once 状态机 + 崩溃恢复)——Pi records/lanes 同构;**lifecycle_ledger 脏死检测**(哨兵状态机);**监控平面**(content-free 事件 + OTLP);**insights 会话洞察**;**error_classifier 24 类失败分类学**(→恢复策略);**checkpoint_manager git 检查点**(每回合快照/恢复);**background_review 审查 fork**(工具白名单运行时拒绝);**relay 会话代理**;**ACP 权限桥**(allow_once/session/always 映射) |
| v7 | 运行时深扫(网关主体/cron/工具存储/授权) | +9 | **TurnRunner 协作器**(闭包→类重构);**网关并发治理**(session 槽位/fallback 链/agent 缓存);**SWE runner**(轨迹格式评测);**MCP 服务器**(OpenClaw 9 工具桥面);**CronScheduler 抽象**(触发/执行分离 + 防网关自杀);**tool_result_storage 三层防溢出**; **file_state 跨代理文件协调**;**write_approval 通用写审批**(MEMORY/SKILLS + write_origin ContextVar);**网关授权混入**(DM/group 策略) |
| v8 | CLI/生命周期/安全层 | +8 | **ESTOP 可恢复暂停**(哨兵,只停新工作 fail-safe);**turn_finalizer 回合终结器**(god-file 分解 seam);**subagent 生命周期服务**(launch/status/wait/cancel/reconnect);**secret_scope 密钥作用域**(profile 隔离 + 外部密码管理器);**kanban goal_judge 独立审查器**(auxiliary 模型,与 Reasonix boundedllm 同构);**conformance 平台契约**(原生渲染器即 oracle/可执行规范);**browser_supervisor CDP 监督**(不在工具 schema);**tool_search 工具桥**(延迟加载 + token 预算) |
| v9 | 存储/schema/安全审批层 | +7 | **SCHEMA_SQL 单一真相源**(Beets 模式 + 启动 reconcile + messages active/compacted 双标记);**SessionStore 完整实现**(routing 代际全序/双轨持久化/JSONL 镜像);**命令安全审批**(hardline rm/sudo stdin/执行标志扫描 + 智能审批熔断);**进程注册表**(systemd scope/守护线程池);**工具输出上限**(三上限防洪泛);**tirith 供应链安全**(cosign 校验才装);**插件族内部**(langfuse/teams_pipeline/security-guidance) |
| v10 | 最大文件层(自纠错) | +6 | **auxiliary_client(10,432 行)** — 任务级 provider 路由 + 多后端 shim(辅助任务统一入口);**cli.py(19,269 行)** — HermesCLI mixin 架构 + busy_input_mode 三模式;**BasePlatformAdapter(4,400+ 行)** — 平台能力抽象(发送/审批/锁/媒体);**moa_loop(2,453 行)** — 并行引用 + 聚合器;batch_runner **checkpoint 断点续跑**(内容匹配恢复);trajectory_compressor **边界保护**(与上下文压缩同构) |
| v11 | 网关运行态机制层 | +7 | **scale_to_zero 服务器休眠**(空闲判定 + 自挂起,own the suspend call 防在途回合被杀);**wake 双策略唤醒**(push 注入事件 / stateless self-POST 原会话);**stream_dispatch adapter 驱动事件分发**(adapter 可 eat 事件);**reasoning_summaries 推理边界修复**(bold 标题信号重推导);**self_repo_guard 自仓库保护**(worktree 变异 git 拦截);**drain_control 排空标记**(marker + epoch 防跨实例误判);**rich_sent_store 回显索引**;**channel_directory 渠道目录** |

> **收敛判定(v11 更新)**:v11 打开网关运行态机制层(scale_to_zero/wake/stream/self_repo_guard 等),新增 7 域——域发现扩展至 **80 域**(核心 12 + 支撑 68)。
> **诚实评估**:v11 的发现分两类——(a) 有产品价值的:scale_to_zero 休眠唤醒(产品②无人值守长跑)、stream_dispatch(执行事件分发)、self_repo_guard(自保护);(b) 平台边缘:rich_sent_store/channel_directory(Telegram 回显索引)。价值仍在,但已明显进入"机制细节"层。
> **覆盖核对面**:gateway/ 已扫 20+ 文件、agent/ 已扫 30+ 文件、tools/ 已扫 25+ 文件、全部 10K+ 行大文件已开。剩余按同构排除。

### 🟢 v12-v26 深化(续扫第 12-26 轮 — 顶层大文件对账/全覆盖)

**触发**:用户继续追问。v12 做"体量排序 × 已打开"对账,发现 **17 个顶层 >5,000 行文件未打开**(gateway/run.py 29,065 项目最大)——"80 域收官"是假收敛(Hermes 第 2 次证伪)。v12-v26 逐个打开确认。

**v12-v26 深化契约(契约深化 30+)**:
- **gateway/run.py(29,065)**:消息→agent 管线(_run_agent_inner:代理模式先行/线程池不阻塞事件循环/run_generation 当前会话检查)+ hygiene 压缩冷却族
- **CLI 入口 main.py**:profile override 先于 import/mcp add --args 透传边界/sudo 用户解析
- **auth.py**:端点探测(zai 8s)/token 指纹/OAuth 追踪
- **web_server**:6 中间件/WS 用 query token/宿主白名单公网保护/动态配置 schema
- **tui_gateway**:会话槽位协议(claim/release/transfer)/孤儿检测/_SlashWorker 子进程
- **mcp_tool**:父死看门狗(--ppid getppid 检测)/描述威胁扫描
- **kanban_db**:崩溃宽限 30s(fork→/proc 窗口防误判)/**限流退出码 75(EX_TEMPFAIL:不计数失败,熔断器不因瞬态节流跳闸)**
- **plugins.py**:依赖拓扑排序(graphlib)/缺失依赖不硬失败(ctx.has_plugin 运行时探测)/循环回退字母序
- **approval**:智能拒绝后 owner 覆盖受限(只能 allow_once/deny)
- **gateway/session**:双路径共享代际(全量快照+单条保存同一计数器)/折叠语义
- **credential_pool**:耗尽 TTL/sole 例外/从错误提取重试延迟
- **config_defaults**:agent 缓存 LRU 权衡文档化/WAL 平台降级
- **terminal_tool**:sudo 缓存作用域/守卫检查族/docker 宿主访问检测

**v12-v26 元教训(最终)**:
- **"80 域收官"第 2 次证伪**:顶层 17 个 >5,000 行文件未打开(域发现偏重 agent//tools/,顶层 gateway/CLI 只扫结构)
- **15 轮续扫完成**:顶层 20 个 >2,500 行文件全部确认,与内部 80 域合并后源码实现面全覆盖
- **Hermes 最终状态**:80 域 + 30+ 契约深化(顶层对账闭环),剩余(tests 契约已扫/plugins 内部排除/平台适配器同构)

### 🟢 v27-v35 深化(续扫第 27-35 轮 — 目标门控/区间对账/测试契约/插件复核)

**触发**:用户继续追问。v27 做 1,500-2,500 行区间对账,发现 **hermes_cli/goals.py 的 GoalGate 是产品③验收器的直接蓝本**(此前完全未记录);v28-v35 完成全区间对账 + tests 契约 + plugins 复核。

**v27-v35 深化契约**:
- **🔴 新增域:目标门控 GoalGate(81)**——hermes_cli/goals.py(2,156):确定性 shell 门在 LLM 判定前运行;失败门短路判定(有界输出 → 续跑 prompt);**未变工作区跳过**(指纹 git status+HEAD sha256,stuck agent 不能重跑同一红色套件);尝试超限自动暂停;超时杀进程
- **kanban_stop 叙述性结尾检测**:worker 必须以 kanban_complete/block 结束;模型叙述下一步就停 → 有界 nudge 让循环继续(伪完成检测,与 #24 相关)
- **coding_context 姿态单一判定**:RuntimeMode 冻结 + ContextProfile 注册表("整个代码库永不重推导是否在编码");快照一次性进稳定 system-prompt 层
- **gateway/relay 协议族**:RelayTransport 4 关注点(生命周期/握手能力描述/入站/出站)+ **send_interrupt 按 session_key 路由** + HMAC 双签名(TTL token + 投递签名)
- **bounded_response 有界读**:字节上限 + 硬墙钟截止(防恶意服务器悬挂)
- **browser_route 引用失效协议**:driver 会话 id 由适配器注入,变异使 refs 失效需重读
- **issue 编号回归测试族**:tests/gateway 20+ 个 #XXXXX 测试(每个 bug 修复 = 契约锁定)
- **plugins 同构排除确认**:平台适配器(10K+)与记忆插件契约实例(honcho/openviking 多样性)

**v27-v35 元教训(最终)**:
- **GoalGate = 产品③"门先于判定"的直接蓝本**:确定性验证门在完成声明前,失败输出回注修正循环,工作区未变不重跑——与 Reasonix TaskContract 同哲学
- **区间对账方法有效**:1,500-2,500 区间发现 GoalGate(高价值),1,000-1,500/600-1,000/400-600 均无新域——**对账驱动的收敛**
- **Hermes 最终状态**:81 域 + 40+ 契约深化;顶层/中层/抽查/tests/plugins 全部覆盖——程序化收敛达成

### 🟢 v36-v38 深化(续扫第 36-38 轮 — 引导/验证守卫/追踪/小文件收尾)

**触发**:用户继续追问。v36-v38 完成 400 行以下全部区间抽查与顶层剩余。

**v36-v38 深化契约**:
- **hermes_bootstrap**:Windows UTF-8 双修复(PEP 540 子进程 env + 当前进程 reconfigure,入口最顶部,POSIX 不触碰)
- **verification_stop turn-end 验证守卫**:纯策略(从不自己跑检查),把被动验证账本变有界 follow-up(编辑代码后无新鲜证据想结束 → 提示);非代码扩展名不提示
- **think_scrubber 跨 delta 状态机**:MiniMax 流式 `<think>` 分片(per-delta regex 破坏下游状态机;部分标签跨边界保持;端流冲洗)
- **nous_rate_guard 跨会话限流守卫**:共享文件防 429 重试放大(9 次调用/回合全计 RPH)
- **moa_trace 侧通道追踪**:完整 MoA 回合 JSONL(端到端离线审计);**不是 messages 表,永不进历史/重放**(会破坏角色交替);默认零开销
- **100 以下文件全部确认**(prompt_cache_boundary/iteration_budget/stream_single_writer 等已覆盖)

**v36-v38 元教训(最终收官)**:
- **"编辑后必须新鲜验证"三守卫族确认**:kanban_stop(任务必须终端工具结束)/GoalGate(验证先于完成声明)/verification_stop(编辑后无新鲜证据 → 提示)——**产品③完成判定的完整守卫**
- **Hermes 最终状态**:81 域 + 50+ 契约深化;顶层/全部区间/tests/plugins/小文件全覆盖——程序化收敛达成(平台适配器同构/i18n 数据/skills 内容为排除清单)

### 🟢 v39-v42 深化(续扫第 39-42 轮 — tui_gateway/gateway 剩余/cron 蓝图/启动安全)

**触发**:用户继续追问。v39-v42 覆盖 tui_gateway 方法族/gateway 剩余/cron 蓝图与执行账本/hermes_cli 剩余。

**v39-v42 深化契约**:
- **tui_gateway 方法族**:30+ JSON-RPC 方法注册表(`def _(rid, params)` 模式)+ compute_host(SpikeAgent 确定性假 agent 测量宿主)
- **cron/blueprint_catalog 自动化蓝图**:参数化 slot schema 单一真相源(表单/斜杠命令/种子 prompt/深链);fill_blueprint → create_job kwargs(**无第二个 job 引擎**;用户永不输原始 cron)
- **cron/executions 执行审计账本**:非重试队列;中断 attempt 只在 owner 进程证明消失后变 unknown;终端态不可变
- **inventory 单一化实证**:provider/model 清单三调用点合并(消除 2 个隐藏 bug——dashboard 漏 v12+ providers 键/TUI canonical-merge 键错)
- **security_audit_startup 启动安全姿态**:4 项警告不阻塞(root/SSH 密码/容器无持久卷/无认证监听器);fail-safe;2026-06 MCP 持久化活动动机
- **delivery 静默叙述检测**:投递层 _is_silence_narration(与 kanban_stop 伪完成检测同族)
- **prompt_stash 不写盘**:草稿常含凭据/NDA/粘贴秘密,仅内存

**v39-v42 元教训(最终收官)**:
- **"叙述不是完成"跨层共证**:kanban_stop(回合层)/delivery(投递层)/GoalGate(验证层)——产品③的叙述检测贯穿多层
- **"单一真相源消除重复推导"三实证**:inventory(清单)/blueprint(slot schema)/coding_context(姿态)——**每个域一处解析,全库消费**
- **Hermes 最终状态**:81 域 + 60+ 契约深化;gateway/tui_gateway/hermes_cli/cron 全部子目录 + 全部行数区间覆盖——程序化收敛达成

### 🟢 v43 最终确认(续扫第 43 轮 — 排除面复核)

**触发**:用户继续追问。v43 复核此前排除的 plugins/platforms/skills/scripts/mcp-research-data。

**v43 确认**:
- **平台适配器同构排除成立**(telegram 10,542/discord 10,522/slack 9,611 = BasePlatformAdapter 契约实现)
- **skills 内容 = 数据面**(197 个 SKILL.md,frontmatter 规范已由 AGENTS.md HARDLINE 覆盖)
- **scripts = 工具面**(60+ 工程工具)
- **mcp-research-data = 评测数据**

**排除清单最终确认**:平台适配器(同构)/skills 内容(数据)/scripts(工具)/评测数据(数据)。

> **Hermes 最终收官**:81 域 + 60+ 契约深化;v12-v43 共 32 轮续扫(顶层→区间→tests→plugins→子目录→排除面);四轮"深化+确认、新增数为零"——程序化收敛最终达成。

### 🟢 v44-v46 深化(续扫第 44-46 轮 — shutdown 冲刷/transports/契约测试面)

**触发**:用户继续追问。v44-v46 覆盖 gateway 剩余中小文件/agent/transports 完整/tests 契约面。

**v44-v46 深化契约**:
- **shutdown_flush 关闭冲刷协议**:flush_pending_to_file/spool_dropped_transcript_message(丢弃消息先 spool)/**fsync_directory(目录 fsync)**/recover_pending_to_db——**关闭/崩溃时在途数据不丢**
- **hermes_tools_mcp_server 工具即 MCP**:Hermes 工具自动暴露为 MCP 服务器(与 mcp_serve.py 的 MCP→Hermes 构成双向桥)
- **codex_event_projector 确定性 call id**(item_type+item_id 派生防重放错配)
- **契约测试命名族**:`*_contract.py`(主机契约)/`*_invariants.py`(不变量)——契约测试的命名即声明
- **agent_cache_pressure cgroup 感知**:缓存边界随内存限制动态

**v44-v46 元教训(最终收官)**:
- **"关闭路径的数据正确性"**:关闭冲刷 + 目录 fsync 是数据不丢的最后一环(与 Reasonix shutdown_flush 同族)
- **契约测试命名模式**:contract/invariants 命名即声明(三项目共证)
- **Hermes 最终状态**:81 域 + 70+ 契约深化;gateway 剩余/transports/tests 契约面全部确认——程序化收敛最终达成
