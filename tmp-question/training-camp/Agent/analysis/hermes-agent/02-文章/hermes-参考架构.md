# Hermes 参考架构 — 源码学习 Agent 的设计蓝图

> 项目:NousResearch/hermes-agent(main)
> 生成:2026-08-15(v3,全域闭合)
> 输入:41 份闭环笔记(hq1-hq43,262 设计)+ 域发现 v12-v46(81 域 + 70+ 契约深化)
> 用途:从 Hermes 提取"源码学习 Agent"的四组件设计蓝图——抄什么/改什么/弃什么
> 前置:对照 Pi/Reasonix/OpenCode/dsh 参考架构(四项目已分析完)

---

## 一、Hermes 架构一句话

> **Hermes = 同步 while 主循环(预算/中断/工具批)驱动的、"缓存神圣"宪法(冻结快照注入)的、三层记忆 + 技能治理 + 大库会话存储的知识资产体系,带 verify 证据账本 + GoalGate + 守卫族 + goal-judge 独立审查器的验证门控,以及 hardline 安全层/回合互斥/凭证隔离的 Python Agent 引擎。**

六个定语对应六个核心域:主循环(agent-loop)、缓存宪法(prompt caching is sacred)、知识资产(memory/skills/sessiondb)、压缩(compaction)、验证(verify+GoalGate+守卫族)、委派(delegation)。

**与 4 个已分析项目的本质差异**:
- Pi/OpenCode:事件溯源(循环状态在 DB/内存)+ 事件发布
- Reasonix:计划契约 + checkpoint + 独立审查器
- dsh:一切皆插件 + 模型可见⟺已记录
- **Hermes:无事件溯源、无规格书数据模型——靠"冻结快照 + 写门控 + 确定性验证门"保证正确性**。正确性不是"重放",是"写入前验证 + 完成前验证"

**两大宪法性设计约束(AGENTS.md:19-24)**:
1. **Per-conversation prompt caching is sacred**——一切上下文注入设计围绕"缓存前缀稳定":技能进 user 消息、记忆用冻结快照、system prompt 永不重建
2. **核心是窄腰,能力在边缘**——新能力优先走扩展/技能/工具,不进核心

---

## 二、产品四组件 ← Hermes 设计映射总表

| 产品组件 | Hermes 抄什么 | Hermes 改什么 | 关键笔记 |
|---------|--------------|--------------|---------|
| ① 对齐模块 | 技能清单注入(进 user 消息非 system prompt)、steer/redirect 纠偏、busy_input_mode 三模式、coding_context 姿态单一判定、CLI mixin 编排 + 注册表分发 | 规格书 = 冻结快照 + 6 维盘问 | hq2/hq5/hq39 |
| ② 执行引擎 | 迭代预算+grace call、中断占位、工具批三模式、fallback 重装饰、23 类失败分类学、压缩五阶段+防 thrash+技能幽灵重注入+提交栅栏、委派摘要预算/心跳/生命周期、checkpoint git 快照、回合租约、审批 hardline+智能审批、秘密作用域、工具结果三层防溢出、文件协调、ESTOP、工具搜索桥渐进披露、缩放至零+唤醒、辅助客户端任务级路由、进程注册表、Cron 调度、自仓库保护 | 章节驱动 + 验收挂点 + 章节摘要模板 | hq4/hq5/hq6/hq8/hq11/hq13/hq14/hq15/hq16/hq17/hq19/hq20/hq21/hq23/hq25/hq29/hq32/hq37 |
| ③ 自动验收器 | **GoalGate(门先于判定)+ 证据账本 + 三守卫族 + readtool 真实评测 + goal-judge 独立审查器(四态/contract/wait 停泊)** | 章节 conformance + 独立审查器(Reasonix 合并) | hq7/hq18/hq36 |
| ④ 书级知识库 | **记忆三层模型 + 技能三态治理 + SessionDB 大库工程 + 交付账本 + 生命周期账本 + 背景审查 fork + 学习图谱 + 监控平面 + 洞察引擎** | 目录按"书籍/章节"组织 + 事件溯源(Pi/OpenCode) | hq1/hq2/hq3/hq9/hq10/hq12/hq24/hq26/hq27 |

---

## 三、组件① 对齐模块

### 抄自 Hermes

| Hermes 设计 | 位置 | 产品用法 |
|------------|------|---------|
| **技能清单注入 user 消息**(非 system prompt) | agent/skill_commands.py:289-400(_build_skill_message) | 领域骨架清单进 user 消息——缓存前缀稳定(宪法#1) |
| **技能内容按需读取**(清单只列名,内容 skill_view) | tools/skills_tool.py:1057(skill_view) | 骨架清单 + 按需加载,不进 system prompt |
| **/steer 与 redirect**(运行中纠偏) | conversation_loop.py:1781-1830 / 242-320 | 对齐中途改主意:steer 注入(不破坏角色交替);redirect 竞争检测丢弃 stale 响应 |
| **busy_input_mode 三模式**(interrupt/queue/steer) | cli.py HermesCLI(4324) | 对齐期用户打断语义:打断/排队/纠偏 |
| **coding_context 姿态单一判定**(RuntimeMode 冻结 + ContextProfile 注册表) | 域发现 v27 | "是否在编码"永不重推导——快照一次性进稳定 system-prompt 层 |
| **记忆 Nudge + 技能 Nudge**(回合间主动提示) | run_agent.py + conversation_loop.py | 对齐期提示"是否值得记忆" |
| **/learn 标准引导**(一个提示词驱动创建) | agent/learn_prompt.py:1-237 | 对齐产物结构化的标准引导 + author 隐私(字面 "Hermes") |

### 产品设计(改)

```
★ 规格书 = 冻结快照(关键设计,Hermes 双态架构思想):
  - 对齐产物 = 6 维规格书(对象/目标/深度/画像/边界/产出)
  - 规格书构建时冻结为快照,注入 system prompt(加载时评估一次,缓存稳定)
  - session 内永不变异——用户改目标 = 新快照重建(不增量变异)
  ——与 OpenCode Epoch 基线互补:Hermes 是"不可变 + 重建",OpenCode 是"不可变 + 时间序更新"
  ——记忆快照注入的缓存利益(Hermes 宪法#1)证明:规格书必须以冻结形式进入上下文

★ 对齐中断处理:
  - 用户中途改目标 → steer 注入最后 tool 消息(非 user 消息——角色交替安全)
  - redirect 与响应竞争 → 检测后丢弃 stale 响应从修正重建
  - busy_input_mode = 用户打断三语义(interrupt/queue/steer)
```

---

## 四、组件② 执行引擎

### 抄自 Hermes(核心骨架)

| Hermes 设计 | 位置 | 产品用法 |
|------------|------|---------|
| **迭代预算 consume/refund + grace call** | agent/iteration_budget.py:32-58 | 章节分析预算:父 max_iterations(500)/子(50)独立;预算耗尽一次收尾(不给最后一轮会产出截断章节) |
| **中断处理每工具前检查 + 取消占位** | agent/tool_executor.py:784-818/1604-1640 | 被跳过的工具写取消结果("Tool execution cancelled")——模型必须知道哪些没跑 |
| **工具批三模式**(concurrent/sequential/segmented) | agent/tool_executor.py:759/1604/2391 | 并行按序回注 + 中断预检 + 参数错误不阻塞批 + Tool Search 桥接 unwrap(钩子看真实名) |
| **fallback 链重装饰**(reasoning 回声垫/缓存断点重渲染) | conversation_loop.py:2508-2760 | provider 降级不是换 URL:重装饰 + 载荷 sanitize + 响应形状验证在链内消化 |
| **失败分类学 23 类 → 恢复策略** | agent/error_classifier.py:24-77 | auth 永久→abort;context_overflow→压缩非 failover;ssl→fail fast 不烧重试——失败→策略确定性映射 |
| **steer/redirect 运行中纠偏** | conversation_loop.py:1781-1830/242-320 | 章节执行中纠偏(角色交替安全 + 响应竞争检测) |
| **回合收尾契约**(持久化→记忆→后台取消→压缩重置) | run_agent.py:4187+ | 每章收尾确定性顺序 |
| **压缩决策状态机**(阈值+冷却+防 thrash+恢复试探) | agent/context_compressor.py:2906-3098 | 防压缩死循环:ineffective≥2 阻塞 + 恢复窗口一次试探 + 重启不得解除 |
| **压缩五阶段**(裁剪/边界/摘要/知识保留/提交) | context_compressor.py:6423+ | 章头保护 + token 尾部 + 边界不拆 tool 组(证据链) |
| **技能幽灵重注入**(压缩不丢指令) | context_compressor.py:575-731 | **确定性重注入**——LLM 改写不可信,代码检查标准串缺失追加 |
| **提交栅栏 CompressionCommitFence** | agent/conversation_compression.py:445-600 | 取消 vs 后台提交确定性边界:取消要么提交前赢要么等完整,提交绝不中途放弃 |
| **摘要预算缩放**(content × 20%,cap=min(context×5%,10K))+ 迭代更新 | context_compressor.py:3508 | 章节摘要预算随内容缩放(_SUMMARY_RATIO=0.20;5% 是 cap 上限来源) |
| **micro-compaction 成本摊销**(显式取舍) | docs/micro-compaction.md | 批量 vs 每轮折一个交换(停顿 vs 缓存破坏 vs 知识保真) |
| **native_compaction**(服务端窄路由 + 本地 fallback 全副武装) | agent/native_compaction.py:1-345 | provider 原生能力窄路由 + 阈值钳制防双压 |
| **委派工具块清单**(五禁) | tools/delegate_tool.py:50-57 | 子代理不能递归/交互/共享写 |
| **委派角色树**(leaf/orchestrator + 深度上限) | delegate_tool.py:1061-1160 | 多章并行委派树防递归爆炸 |
| **委派摘要预算**(head+tail+溢出文件) | delegate_tool.py:2072-2260 | 批量结果回注上下文保护(父 headroom ÷ batch) |
| **心跳 + stale 分级检测** | delegate_tool.py:2295-2420 | 并行活性:in-tool 阈值高于 idle(慢工具不误杀) |
| **SubagentLifecycleService**(launch/status/wait/cancel/reconnect) | agent/subagent_lifecycle.py:187-487 | 子代理完整生命周期 API |
| **后台委派 + 异步完成队列** | tools/async_delegation.py:1603 | 批作为单异步单元,完成推单事件 |
| **checkpoint git 快照**(工具写前 ensure_checkpoint) | tools/checkpoint_manager.py:701 | 章节快照 + 容量上限(500MB/20 快照)防爆炸 |
| **工具结果持久化三层防溢出** | tools/tool_result_storage.py | per-tool cap/per-result 持久化/per-turn 聚合预算(200K chars) |
| **file_state 跨代理文件协调** | tools/file_state.py:59 | 并发子代理写同文件防损坏(stale 写拦截) |
| **ESTOP 可恢复暂停**(只停新工作) | agent/estop.py | 分析暂停语义:在途工作永不杀,损坏文件 fail-safe |
| **回合租约 turn_lease**(同 session 串行化) | gateway/turn_lease.py:97 | 多入口并发回合串行化(generation-scoped + identity-checked) |

### 产品设计(改)

```
★ 章节执行 = 双层循环 + Hermes 预算/中断/工具批:
  外层章节队列,内层章节步骤——Pi 双层循环 + Hermes 迭代预算(grace call)
  中断:每工具前检查 + 取消占位(章节产出"哪些步骤没跑"对验收器可见)

★ 章节验收挂点:
  每章完成 → 验收器(组件③)判据 = GoalGate 门 + 守卫族 + goal-judge
  ——Pi 用 shouldStopAfterTurn 钩子,Hermes 用"确定性门先于判定"(更强)

★ 失败循环(D17):
  工具失败 → 失败消息进上下文 → LLM 自纠 → 重验(23 类分类学决定重试/换模型/放弃)
  连续失败 → 尝试超限自动暂停(GoalGate 语义镜像 turn-budget)
  + 审批拒绝 → hardline 不可绕/智能审批熔断(hq15)
  + 回合级互斥 → turn_lease fail-closed(hq8)

★ 章节摘要(压缩五阶段 + 技能幽灵重注入):
  7 段模板(OpenCode)+ 确定性重注入(方法论/指令不因压缩丢失)
  + 提交栅栏(用户取消 vs 章节摘要提交的确定性边界)

★ 安全与隔离层(新笔记落地):
  - 命令安全:hardline 无条件地板 + 反混淆规整链 + 智能审批防注入 + 熔断(hq15)
  - 凭证隔离:contextvar 作用域 + 双模式 + 豁免名单(hq17)
  - 自仓库保护:变异分类 + 别名递归 + 非绕过(hq23)
  - 工具面:核心永不延迟 + 三档渐进披露(hq25)
  - 后台任务:进程注册表完整生命周期 + 崩溃恢复(hq32)
  - 长跑:空闲三条件 + ESTOP 手动暂停(自动 vs 手动互补,hq19/hq16)
  - 辅助任务:统一入口任务级路由(hq29)
```

---

## 五、组件③ 自动验收器(Hermes 最强贡献)

### 抄自 Hermes(完整组件清单)

| Hermes 设计 | 位置 | 产品用法 |
|------------|------|---------|
| **verify recipe 检测**(静态项目验证) | agent/verify/recipes.py:35+ | 章节"项目验证"——检测顺序(Node/Python/Go/Rust/Java/Makefile)+ 分层事实合并(manifest 是真相源) |
| **验证执行 + 就绪轮询** | agent/verify/runner.py:242 + :146 | 证明"真的能服务 HTTP";partial(--phase 子集)→ scope 降级 targeted |
| **证据账本**(record_verify_run ok/scope full\|targeted) | agent/verification_evidence.py:36/483 | **验收结果留痕** + 有界(2000 字符/30 天/100 事件)+ fail-silent(账本问题绝不改 CLI 退出码) |
| **GoalGate(门先于判定)** | hermes_cli/goals.py:427 | **验收器核心**:确定性 shell 门在 LLM 判定前;失败门短路判定(有界输出 → 续跑 prompt) |
| **未变工作区跳过**(指纹) | hermes_cli/goals.py:427+ | git status+HEAD sha256 指纹 → 重放失败不重跑(stuck agent 不能烧墙钟) |
| **尝试超限自动暂停** | hermes_cli/goals.py:427+ | 镜像 turn-budget 暂停语义 |
| **verification_stop(turn-end 编辑后新鲜验证)** | agent/verification_stop.py:1-316 | 纯策略:模型编辑后想结束而无新鲜证据 → 提示;非代码文件不提示 |
| **kanban_stop(叙述不是完成)** | agent/kanban_stop.py:25-108 | 任务必须以终端工具结束;模型叙述下一步就停 → 有界 nudge;max_attempts 2 防伪完成转死循环 |
| **verify_hooks**(验证钩子) | agent/verify_hooks.py:69 | 验收器扩展点 |
| **readtool 评测 harness(真实 agent A/B)** | evals/readtool/(runner/tasks/fixtures/report) | 验收器自身评测:真实 AIAgent 跑确定性 hostile 工作区(9 种 fixture);指标 accuracy/api_turns/tool_calls/total_tokens/wall_s(per-task 均值绝不求和) |
| **goal-judge 独立审查器**(四态 verdict) | hermes_cli/goals.py:1006 | **Hermes 侧独立审查器**(与 Reasonix Goaleval 同构):done/continue/wait/skipped 四态;goal_judge 可用性探测 + 异常放行双保险(fail-open 不卡死 worker) |
| **GoalContract 严格判定** | goals.py:332 | 结构化完成契约:Verification 判 DONE/Constraint 拒绝;draft_contract 失败回退裸 goal(弱 aux 不阻塞) |
| **wait 停泊**(等待外部进程) | goals.py:1808-1835 | "在等 CI poller/build"是合法验收状态:wait 屏障自动恢复(pid 退出/期限过) |
| **失败两轴**(parse vs transport) | goals.py:1787-1804 | 弱模型(parse 3 次暂停)vs 配置错(transport 5 次暂停)独立计数互不干扰——评估器失败分级 |

### 产品设计(改)

```
★ 章节 conformance = GoalGate + 证据账本 + 守卫族(架构定论):
  1. 门先于判定:确定性验证门(章节 KP 覆盖检查)在 LLM 完成声明前
  2. 失败 → 有界输出 → 续跑 prompt(agent 对照具体证据迭代)
  3. 未变工作区跳过(指纹)——连续 N 轮无新增的代码级实现
  4. 验收结果留痕(证据账本 ok/scope,partial 绝不当全绿)
  5. 完成 = 终端工具证据,不是叙述(kanban_stop/verification_stop 语义)

★ 独立审查器合并(Reasonix):
  Hermes GoalGate = 确定性 shell 门(验证层)
  + Hermes goal-judge = LLM 独立审查器(评估层,四态 + contract + wait 停泊)
  + Reasonix Goaleval = LLM 独立审查器(四隔离)
  ——三门串行:确定性门先过滤,LLM 审查再判定;Hermes 与 Reasonix 的
    审查器同构(独立 auxiliary 调用/温度 0/固定 prompt),Hermes 特有
    wait 停泊 + contract 严格判定 + 失败两轴

★ 验收器自证(元验收,D17 之五):
  readtool 真实 agent A/B 评测验收器本身(不信任何人的能力表)
  + 证据账本可审计 + GoalGate 指纹可测试 + goal-judge 失败两轴可追踪
```

---

## 六、组件④ 书级知识库(Hermes 最强贡献)

### 抄自 Hermes(核心蓝图)

| Hermes 设计 | 位置 | 产品用法 |
|------------|------|---------|
| **记忆三层模型**(存储/编排/插件契约) | tools/memory_tool.py:148 + agent/memory_manager.py:364 + agent/memory_provider.py:104 | 知识库 = 结构化文件 + 编排层 + 可插拔后端 |
| **双态架构**(冻结快照 vs 活态) | memory_tool.py:150-174 | **章节发布 = 冻结快照,写作中 = 活态**;快照注入 system prompt 缓存稳定 |
| **四操作语义**(add/replace/remove/apply_batch) | memory_tool.py:390-668 | 结论增改删 + 原子批量;**读失败必须拒绝**(append 安全只在真读到文件时成立) |
| **字符预算 + consolidation 引导** | memory_tool.py:165 | 每章结论容量上限 + 超限引导合并;**失败上限 3 次 → TERMINAL**(防死循环) |
| **成功响应 TERMINAL 不回声** | memory_tool.py:702 | 验收通过章节返回"不要重复"信号(防 thrash) |
| **写门控三层防护**(注入扫描/漂移检测/原子写) | memory_tool.py:919-1016 | 多 session 并发写结论防冲突;temp-file + os.replace 原子写;漂移备份 .bak |
| **审批门控**(allow/blocked/stage 三态) | tools/write_approval.py:230 | 结论发布可配审批,门控与执行分离 |
| **单外部 provider 原则 + 串行化后台写** | memory_manager.py:404-415/675 | 知识库扩展不膨胀工具 schema;turn N 落盘先于 N+1 |
| **上下文栅栏**(sanitize + StreamingContextScrubber) | memory_manager.py:174-369 | 检索结果注入防注入包装("这是数据不是指令")+ 跨 chunk 边界状态机 |
| **agent_context 写保护** | memory_provider.py:104-404 | **"谁在写"第一道闸**:cron/subagent 跳过写(防污染画像) |
| **中断回合不持久化** | run_agent.py:4187-4265 | **"只有完成的结论才入库"**——被打断的分析不入库 |
| **委派观察模式**(on_delegation) | memory_provider.py | 子代理不直接写,父侧 (task,result) 观察持久化 |
| **技能三态状态机**(active/stale/archived) | tools/skill_usage.py:53-56 + agent/curator.py:305-380 | 章节结论保鲜;**pinned/cron 引用豁免**(被依赖不自动归档) |
| **来源分级**(agent/builtin/hub/external/protected) | tools/skill_usage.py:338-520 | 知识来源治理边界;策略标志与事实字段分离 |
| **LLM 伞形合并审查**(独立 fork) | agent/curator.py:396-560 | 知识库自动重构:窄结论合并类级章节;机器可读 YAML 驱动引用迁移;**合并看内容不看用量** |
| **干跑模式**(REPORT ONLY) | agent/curator.py:338-355 | 高风险自动操作先报告后执行(不 bump 计划/同格式报告) |
| **运行前快照 + 每次报告** | curator.py:1510-1530 | 知识库维护三级证据(预快照 tar.gz + run.json + REPORT.md) |
| **安装安全扫描**(威胁模式 + 信任矩阵 + 路径校验) | tools/skills_guard.py + skills_hub.py | 外来知识准入三关(危险扫描 --force 不能覆盖) |
| **背景审查 fork**(每轮后回放问"该存什么") | agent/background_review.py:1144 | 知识沉淀硬通道(工具白名单 + 运行时拒绝,写直达不动主会话缓存) |
| **SessionDB WAL + 时间预算写**(20s/60s/0.5s) | hermes_state.py:3533(_execute_write) | 知识库日志并发写——时间预算分级 + jitter 破 convoy |
| **PASSIVE checkpoint 取代 TRUNCATE** | hermes_state.py:3533+ | 大库维护不独占锁(#45383:TRUNCATE 65K+ 页损坏 B-tree) |
| **有界读池 + permit 信号量** | hermes_state.py:3249(_read_ctx) | 检索并发防 fd 耗尽(EMFILE 教训:#69678) |
| **压缩锁 = 租约**(TTL + pid 死检) | hermes_state.py:5619-5750 | 章节压缩/索引重建跨进程互斥(正确性边界非忙信号) |
| **FTS5 降级链**(fts5→CJK→trigram→LIKE) | hermes_state_search.py:1704-1790 | 全书检索三级降级;**压缩归档行默认包含**(压缩≠删除) |
| **增量 FTS merge 取代 optimize** | hermes_state_search.py:2423-2492 | 索引维护毫秒级可交错(optimize 9-18s 写锁 = 故障源) |
| **自愈链 + fail-open 分级** | hermes_state.py:3660-3900 | 主数据优先派生索引(连接重开→FTS 重建→fail-open 分离) |
| **SCHEMA_SQL 单一真相源 + 启动 reconcile** | hermes_state_common.py:250-442 | 知识库 schema 增列自动生效(Beets/sqlite-utils 模式) |
| **锚定视图**(窗口+章首+章尾三切片) | hermes_state_search.py:975-1095 | 检索命中章节自动带"章首目标 + 章尾结论" |
| **AsyncSessionDB 门面** | hermes_state.py:11591 | 同步存储 + asyncio.to_thread 卸载,不冻结事件循环 |

### 产品设计(改)

```
★ 书级组织:
  全书一个知识库(记忆三层模型)+ 章节 = 条目过滤
  book/
    spec.md          ← 规格书冻结快照(组件①产出)
    MEMORY.md        ← 全书结论(双态:快照注入/活态写作)
    chapters/        ← 每章一个文件(createBranched 演化)
  条目:conclusion(结论)/evidence(证据)/spec/acceptance

★ 事件溯源合并(Pi/OpenCode):
  Hermes 无事件溯源——正确性靠"写入前验证"
  产品:Pi 事件溯源(可重放)+ Hermes 写入安全(原子写/漂移检测/写门控)
  重放 = 真相(Pi) + 写入 = 防丢(Hermes) 双支柱

★ 结论保鲜(技能治理语义):
  章节结论随源码演进 → 三态状态机(active/stale/archived)
  被引用结论(pinned/cron 语义)豁免归档
  结论合并 = LLM 伞形审查(独立 fork)+ absorbed_into 声明(防引用悬挂)

★ 全书检索(SessionDB 大库工程):
  FTS5 降级链 + 锚定视图(命中带章首目标+章尾结论)
  压缩≠删除(归档章节依然可搜)
  跨 session 交接 = 读快照 + 摘要(7 段模板注入)
```

---

## 七、D17 落地性检查(5 问)

### 1. 数据怎么流动(三合一原则)

```
规格书(1 份)三处消费(Hermes 版):
- 冻结快照注入 system prompt(Hermes 双态架构)——执行引擎的初始上下文
- 记忆活态 + 写门控(原子写/漂移检测)——知识库第一条日志
- 验收基准(GoalGate 门 + 证据账本)——验收判据
答案:Hermes 的"冻结快照"天然支撑"不可变基线"——规格书一次冻结三处消费
  补充:规格书变更 = 新快照重建(Hermes 无时间序更新——产品取 OpenCode 时间序更新合并)
```

### 2. 时机怎么定

```
- 验收时机:章节完成时(turn-end)+ 编辑后(verification_stop 触发)
- GoalGate 时机:完成声明前(确定性门先于 LLM 判定)
- 压缩时机:阈值触发 + cooldown + 防 thrash(无物可压显式记账)
- 记忆持久化:turn 结束同步(中断跳过)——"完成才入库"
- 技能治理:curator 间隔(LLM 合并审查独立 fork)
```

### 3. 失败怎么循环

```
- 工具失败 → 失败消息进上下文 → LLM 自纠 → 重验                    [Hermes 原样]
- GoalGate 失败 → 有界输出 → 续跑 prompt;未变工作区跳过(指纹)       [Hermes 原样]
- 压缩失败 → cooldown + ineffective 记账 + 恢复窗口试探(有界)       [Hermes 原样]
- 验收连续失败 → 尝试超限自动暂停(GoalGate 语义)                   [Hermes 原样]
- API 失败 → 23 类分类学 → 恢复策略(abort/fallback/压缩/retry)     [Hermes 原样]
- 中断 → 取消占位 + 中断不持久化(半截结果不入库)                   [Hermes 原样]
```

### 4. 大库怎么查

```
- 全书检索:FTS5 降级链(fts5→CJK→trigram→LIKE)+ 锚定视图
- 归档检索:压缩≠删除,归档行默认包含(#38763)
- 索引维护:增量 merge(毫秒级可交错,optimize 全量重写 = 故障源)
- 写入并发:WAL + 时间预算分级 + 有界读池 + 租约锁
- 慢查询日志:>1000ms 记录 routing path("下次回归是一个 grep")
```

### 5. 系统怎么自证

```
- 验收器自证:readtool 真实 agent A/B 评测(9 种 hostile fixture,不信能力表)
- 证据账本可审计:ok/scope 留痕 + fail-silent(观测不干预执行)
- 守卫可测:GoalGate 指纹逻辑(未变跳过语义)测试锁定
- 写入自证:漂移检测 round-trip 比对 + 原子写 + 备份(.bak)
- 存储自证:SCHEMA_SQL 单一真相源 + 启动 reconcile + 自愈链
```

---

## 八、产品决策清单(Hermes 独有贡献)

| # | 决策 | 来源 | 理由 |
|---|------|------|------|
| 1 | **缓存神圣 = 宪法级约束** | AGENTS.md:19 | 一切注入设计围绕缓存前缀稳定:技能进 user 消息、记忆冻结快照——产品必须继承(规格书冻结注入) |
| 2 | **门先于判定(GoalGate)** | hq7 | 确定性验证门在 LLM 完成声明前;未变工作区指纹跳过——"完成"的代码级定义 |
| 3 | **叙述不是完成(守卫族)** | hq7 | kanban_stop/verification_stop/delivery 三层共证——完成 = 终端工具证据 + 新鲜验证 |
| 4 | **失败分类学 23 类 → 恢复策略** | hq5 | 失败→策略确定性映射(auth 永久→abort;ssl→fail fast 不烧重试) |
| 5 | **中断不持久化 + 取消占位** | hq1/hq5 | "只有完成的结论才入库";被跳过的工具写取消结果(透明) |
| 6 | **提交栅栏** | hq4 | 取消 vs 后台提交的确定性边界——提交绝不中途放弃,取消要么先赢要么等完整 |
| 7 | **防 thrash 状态机** | hq4 | ineffective 记账 + 恢复窗口试探 + 重启不得解除(#54923)——收敛性代码级答案 |
| 8 | **技能幽灵重注入** | hq4 | 压缩不能丢可执行知识——LLM 改写不可信,确定性重注入是唯一可靠方案 |
| 9 | **委派摘要预算(head+tail+溢出文件)** | hq6 | 批量回注上下文保护(#9126 教训:N 个全量摘要炸父上下文) |
| 10 | **记忆写门控 + agent_context 写保护** | hq1 | "谁在写"第一道闸 + 写入前验证磁盘真相(读失败≠空存储) |
| 11 | **知识治理三件套**(三态状态机/来源分级/LLM 伞形合并) | hq2 | 防知识库膨胀成垃圾场——被依赖豁免 + 内容为准合并 + 干跑 |
| 12 | **大库工程**(WAL/租约/增量 merge/fail-open 分级) | hq3 | 10GB 级知识库的完整工程(TRUNCATE 损坏 B-tree 等真实教训) |
| 13 | **评测 = 真实 agent A/B** | hq7 | 不信任任何人的能力表(Command Code 评测表的 Hermes 列有错) |
| 14 | **hardline 无条件地板**(先于一切设置) | hq15 | rm -rf / 等灾难命令任何会话设置不能绕过;命令位置锚定防数据误报;反混淆规整链 + 智能审批防注入 + 连续拒绝熔断 |
| 15 | **回合级互斥 = 代际 + 身份校验** | hq8 | Fencing 家族第三变体:回合 [load→run→flush] 串行化,stale unwind 不能释放新回合;超时 fail-closed |
| 16 | **凭证隔离 = contextvar 作用域 + 双模式** | hq17 | fail-closed 只开在有隔离风险时(multiplex);豁免名单紧(部署配置豁免/凭证永不);round-trip 解析器 |
| 17 | **诚实 at-least-once(模糊性显式标记)** | hq9 | 崩溃未确认交付 → 带 ♻️ 标记重投,绝不静默重复;attempts 预算只花在真发送上 |
| 18 | **独立审查器四态 + wait 停泊** | hq18 | goal-judge:done/continue/wait/skipped;在等外部进程是合法状态;parse/transport 两轴自动暂停;可用性探测 + 异常放行双保险 |
| 19 | **工具面渐进披露(无状态目录)** | hq25 | 核心永不延迟;三档预算降级;无状态目录防静默脱落(OpenClaw #84141);桥接同路由(安全层不丢) |
| 20 | **辅助任务统一入口(任务级路由)** | hq29 | 每侧任务可 pin provider/模型;主→Portal→直连降级链;温度契约 + 任务级信号量——goal-judge/审查/摘要全经此 |

---

## 九、弃用清单(产品不抄——区分"部署面"与"机制")

> ★ review 调和(2026-08-15 深度 review):补充笔记(hq19-43)已将多数域写成产品②③④蓝本——
> 弃用清单随之精确化为**"部署面/平台面弃用,机制保留"**:

| Hermes 设计 | 弃用原因 | 保留的机制 |
|------------|---------|-----------|
| 20+ 平台适配器具体实现(telegram/discord/slack...) | 产品 CLI 优先 | **BasePlatformAdapter 抽象**(hq30:能力 ABC/声明式标志) |
| 40+ model-provider 插件 | 只留 2-3 个 provider | 插件系统机制(hq38:四源发现/行为契约) |
| Fly 云部署面(scale_to_zero 自挂起/wake 双策略/drain_control) | 产品单机,无 Fly | **空闲三条件谓词 + ESTOP 手动暂停语义**(hq19/hq16) |
| 消息平台边缘(rich_sent_store/channel_directory) | Telegram/渠道细节 | 回显索引/名称解析思想(hq41-43 参考) |
| cron/kanban 调度体系 | 产品章节驱动,不需要外部调度 | **CronScheduler ABC + 防自杀守卫**(hq37 机制)+ kanban_stop 叙述检测(hq7) |
| MoA 多层聚合 | 多模型聚合非源码学习核心 | 聚合无上限 + 隐私三档思想(hq28 参考) |
| ACP 适配器/VS Code 集成 | 外部编辑器集成,非产品核心 | — |
| 语音/TTS/图像/视频工具 | 产品无关边缘 | — |
| trajectory_compressor/batch_runner(训练数据管线) | 产品是分析 agent,不是训练管线 | **检查点续跑 + 内容匹配恢复**(hq34) |
| insights 会话洞察引擎 | 历史分析非 MVP | 精确计数前置(flush)思想(hq27) |
| 外部记忆 provider 具体实现(honcho/mem0/...) | 保留 MemoryProvider 契约 | 契约抽象(hq1) |
| MCP 双向桥具体实现 | MVP 不需要 | 双向桥模式(hq35 扩展点保留) |

---

## 十、与 Pi/Reasonix/OpenCode/dsh 的合并要点

| 维度 | Pi | Reasonix | OpenCode | dsh | Hermes | 产品取 |
|------|----|----------|----------|-----|--------|--------|
| 循环 | 内存双层 | 长跑+checkpoint | **DB 收件箱双层** | Turn/Step+双队列 | 同步 while+预算 | OpenCode + Hermes 预算(grace/中断占位) |
| 上下文 | 压缩 5 重保护 | 7 标题摘要 | **Epoch 基线+时间序更新** | profile patch | 冻结快照+防 thrash+提交栅栏 | OpenCode 基线 + Hermes 快照注入(缓存神圣)+技能幽灵重注入 |
| 验收 | 钩子+契约 | 独立审查器 | 事件管线+快照对 | repeat 收敛+post-execute | **GoalGate 门先于判定+守卫族+证据账本** | Hermes 门 + Reasonix 独立审查 + OpenCode 事件 |
| 知识库 | 事件溯源 | subject 冲突 | **双游标+投影器** | 日志不变量 | 三层记忆+技能治理+SessionDB | Pi 事件溯源 + Hermes 写入安全/治理/大库 |
| 规格书 | 提示词模板 | TaskSpec 三合一 | Epoch 基线 | profile patch | 冻结快照 | 合并(Pi 三合一 + OpenCode 基线 + Hermes 快照) |
| 正确性理论 | 事件溯源 | fail-closed | 投影同事务 | 模型可见⟺已记录 | **写入前验证+完成前验证** | 双支柱:重放(Pi)+写入安全(Hermes) |

> 关联缺口:合并表"验收"一行——Hermes 的 GoalGate 是**确定性门**(shell 命令),Reasonix 的 Goaleval 是**LLM 评估器**,两者串行互补,不是替代。

---

## 十一、覆盖对账表(41 笔记 262 设计 → 蓝图去向)

### 核心 7 份(2026-08-15 重写,两轮 review 通过)

| 笔记 | 设计数 | 蓝图去向 |
|------|:--:|---------|
| hq1 memory | 13 | ④核心(双态/四操作/写门控/栅栏/agent_context) |
| hq2 skills-lifecycle | 14 | ④核心(三态状态机/来源分级/LLM 合并/干跑/准入) |
| hq3 sessiondb-search | 14 | ④核心(写路径/读池/租约/降级链/自愈/锚定视图) |
| hq4 context-compression | 12 | ②核心(决策状态机/五阶段/幽灵重注入/提交栅栏/micro/native) |
| hq5 agent-loop | 12 | ②核心(预算/中断/工具批/fallback/分类学/收尾契约) |
| hq6 delegation | 11 | ②核心(块清单/角色树/并发/摘要预算/心跳/生命周期) |
| hq7 verification-quality | 11 | ③核心(recipe/证据账本/GoalGate/守卫族/评测) |

### 补充 11 份(2026-08-15,深度 review 通过)

| 笔记 | 设计数 | 蓝图去向 |
|------|:--:|---------|
| hq8 turn-lease | 8 | ②核心(回合租约:代际+身份/Fencing 同族) |
| hq9 delivery-ledger | 9 | ④核心(三检查点/死主认领/attempts 预算/诚实 at-least-once) |
| hq10 lifecycle-ledger | 7 | ④核心(哨兵状态机/心跳采样/OOM 启发/所有权守卫) |
| hq11 checkpoint-manager | 8 | ②核心(写前快照/共享 git 仓库/可撤销回滚/孤儿三步佐证) |
| hq12 background-review | 8 | ④核心(独立 fork 沉淀/白名单拒绝/持久化隔离/缓存温) |
| hq13 tool-result-storage | 6 | ②核心(三层防溢出/PINNED read_file/预算缩放) |
| hq14 file-state | 6 | ②核心(读戳/最后写者/三分类 stale/每路径锁) |
| hq15 approval | 6 | ②核心(hardline 地板/反混淆/智能审批+熔断/单核门) |
| hq16 estop | 6 | ②核心(哨兵暂停/双层方向/豁免族) |
| hq17 secret-scope | 5 | ②核心(contextvar 隔离/双模式/豁免名单/round-trip) |
| hq18 goal-judge | 5 | ③核心(四态 verdict/contract 严格判定/wait 停泊/fail-open 两轴) |

### 补充 25 份(2026-08-15,全域闭合)

| 笔记 | 设计数 | 蓝图去向 |
|------|:--:|---------|
| hq19 scale-to-zero | 4 | ②核心(自挂起/双策略唤醒/装配三合一) |
| hq20 session-stall | 4 | ②核心(停滞策略门/notify-once/闩锁) |
| hq21 stream-dispatch | 4 | ②核心(typed 事件路由/adapter 决定/可吃事件)⚠ 无生产接线 |
| hq22 tool-output-limits | 4 | ②支撑(三阈值集中/防御式读取) |
| hq23 self-repo-guard | 5 | ②核心(变异分类/反混淆/别名递归/非绕过) |
| hq24 learning-graph | 5 | ④支撑(学习信号过滤/词法关联/密度统计) |
| hq25 tool-search | 5 | ②核心(三桥渐进披露/无状态目录/预算降级) |
| hq26 monitoring | 5 | ②支撑(content-free 事件/无条件脱敏/匿名身份) |
| hq27 insights | 5 | ④支撑(报告结构/精确计数/三维分解) |
| hq28 moa | 5 | ②支撑(并行 fan-out/聚合无上限/隐私三档) |
| hq29 auxiliary-client | 5 | ②核心(任务级路由/自动链/温度契约/限流) |
| hq30 platform-adapter | 5 | ②支撑(能力 ABC/声明式标志/生命周期) |
| hq31 authz | 5 | ②支撑(授权集群/策略分层/配对存储) |
| hq32 process-registry | 5 | ②支撑(双 spawn/查询族/控制族/恢复) |
| hq33 browser-supervisor | 4 | ②支撑(CDP 监督/双通道接入/结构化状态) |
| hq34 batch-runner | 4 | ②支撑(并行批/检查点/内容恢复) |
| hq35 mcp-serve | 5 | ②支撑(双向桥/9 工具面/live 事件) |
| hq36 swe-runner | 4 | ③支撑(轨迹格式兼容/三环境/温度契约) |
| hq37 cron-scheduler | 5 | ②支撑(ABC/文件锁/防自杀/账本) |
| hq38 plugins | 5 | ②支撑(四源发现/钩子面/行为契约) |
| hq39 cli | 5 | ①核心(mixin 架构/注册表分发/三输入模式) |
| hq40 reasoning-summaries | 1 | ②支撑(流式边界修复) |
| hq41-43 gateway-runtime | 3 | ②支撑(排空 marker/回显索引/渠道目录) |

**统计**:②执行 20+ / ④知识库 8+ / ③验收 3 / ①对齐 1(交叉计)——核心产品组件全覆盖。
**结论**:41 笔记 262 设计全部分配;81 域全部有笔记覆盖或域发现深度标注(排除清单:平台具体实现/前端 TSX/语音图像为域发现明确排除)。

> ★ 覆盖对账(2026-08-15 深度 review 后更新):原 7 笔记 87 设计 → 41 笔记 262 设计;
> 高价值 8 域(缩放至零/MoA/辅助客户端/停滞/学习图谱/自仓库/监控/洞察)+ 中价值 10 域
> (平台抽象/授权/进程注册表/浏览器/批量/MCP/SWE/Cron/插件/CLI)+ 小件 4 域全闭合。

---

## 十二、落地缺口清单(产品需新增的设计)

| # | 缺口 | 证据 | 产品设计 |
|---|------|------|---------|
| A | **无规格书数据模型** | Hermes 对齐靠 prompt 模板/skills,无 TaskSpec/PlanContract 等价物 | 合并 Reasonix TaskSpec 三合一(goal/scope/non_goals/success_criteria+evidence_ids)——Hermes 提供冻结快照注入机制,Reasonix 提供 schema |
| B | **无章节概念**(会话级) | 记忆/技能/会话库全 session 级,无"章节"实体 | 章节 = 知识库条目过滤 + 7 段摘要交接(OpenCode 模板)+ 章节完成事件(OpenCode 缺口 A 同源) |
| C | **无事件溯源**(可重放性靠 SessionDB 而非事件日志) | 正确性 = 写入前验证(漂移检测/原子写),无重放语义 | 产品 = Pi/OpenCode 事件溯源(重放即真相)+ Hermes 写入安全(防丢防漂移)双支柱;Hermes 的租约锁/提交栅栏保留为写入层 |
| D | **无 LLM 独立审查器** | GoalGate 是确定性 shell 门,验证"命令通过与否",不评估"内容质量" | 合并 Reasonix Goaleval(独立审查器四隔离)——GoalGate 门 + Goaleval 审查串行 |
| E | **无章节摘要模板** | Hermes 压缩模板是 Goal/Progress/Decisions(执行摘要),非交接规格 | 引入 OpenCode 7 段模板(Objective/Work State/Next Move)作章节交接摘要 |

**说明**:A-E 是"Hermes 没做规格书/章节/事件溯源/LLM 审查"的产品新增,与其他四项目缺口互补(Hermes 提供写入安全 + 确定性门 + 治理,Pi/OpenCode 提供事件溯源 + 章节事件,Reasonix 提供规格书 + 独立审查)。

---

## 十三、Review 记录(收敛性)

| 轮次 | 发现 | 状态 |
|:--:|------|------|
| v1 | 初始架构(引用 7 笔记 87 设计 + 域发现关键域) | 完成 |
| v2 | 深度 review 修正:FailoverReason 24→23 类/深度上限 2→1/摘要预算 ×20%/headroom ×0.5(继承错误修正) | 完成 |
| v3 | 补充 34 份笔记(8-43)→ 41 笔记 262 设计;每份深度 review(测试数/行号/常量/语义精确化) | 完成 |

**覆盖检查**:41/41 笔记核对(262 设计全部分配);81 域全闭合(排除清单:平台具体实现/前端/语音图像为域发现明确排除)
**源码核对(v2 新增)**:turn_lease.py:97/delivery_ledger.py:188,236/lifecycle_ledger.py:181,224/checkpoint_manager.py:701/background_review.py:654/tool_result_storage.py:144/file_state.py:59/approval.py:543,3142,3982/estop.py:59/secret_scope.py:132/goals.py:427,1006(GoalGate+judge_goal 同文件)/scale_to_zero.py:118,134/wake.py:56/session_stall.py:27/stream_dispatch.py:40/tool_output_limits.py:59/self_repo_guard.py:698/learning_graph.py:254/tool_search.py:772/monitoring events.py:20/insights.py:140/moa_loop.py:1233/auxiliary_client.py:8963/base.py:2884/authz_mixin.py:386/process_registry.py:419/browser_supervisor.py:289/batch_runner.py:529/mcp_serve.py:63/mini_swe_runner.py:157/cron tick:5148/plugins.py:3388/cli.py:4324——全部核对通过
**每份笔记 review 记录**:hq8(3 处)/hq9(6 处)/hq10(4 处)/hq11(5 处)/hq12(3 处)/hq13(5 处)/hq14(4 处)/hq15(7 处)/hq16(5 处)/hq17(7 处)/hq18(2 处)/hq19(1 处)/hq20(2 处)/hq21(1 处)/hq22(0)/hq23(1 处)/hq24(0)/hq25(1 处)/hq26(1 处)/hq27(0)/hq28(1 处)/hq29(1 处)/hq30(0)/hq31(0)/hq32(0)/hq33(0)/hq34(0)/hq35(1 处)/hq36(0)/hq37(1 处)/hq38(0)/hq39(0)/hq40(1 处)——每份写完即深度 review(支柱 4)

## 十四、下一步

- [ ] 用 D17 落地性检查 review 本架构(5 问已在 §七,需独立核对)
- [ ] **跨项目沉淀**:5 项目参考架构合并 → 产品最终架构决策文档(HANDOVER 待办 #2)
- [ ] 方法论正式文件(methodology/zh/ 00-13)
- [ ] 产品 MVP(对齐模块 + 知识库日志原型)
