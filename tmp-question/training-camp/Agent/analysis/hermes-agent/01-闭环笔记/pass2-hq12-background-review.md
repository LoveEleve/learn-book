# hq12 背景审查 fork(Background Review)— 产品④"知识沉淀硬通道"蓝本

> 项目:Hermes(agent/background_review.py 1,144 行 + run_agent.py 触发 + agent/turn_finalizer.py + agent/codex_runtime.py + tools/skill_manager_tool.py 写守卫)
> 假设:知识沉淀不能靠模型自觉(nudge 是软提示)——Hermes 每轮后 fork 一个独立 AIAgent 回放对话问"该存什么?",是"知识沉淀硬通道"的完整样本。
> 结论:✅ 成立——fork 隔离/工具白名单运行时拒绝/运行时继承(缓存温)/持久化隔离/写守卫触发/动作摘要全具备,产品④"自主知识维护"直接蓝本。

---

## 一、架构全景:为什么需要背景审查 fork

```
问题:记忆/技能沉淀靠模型自觉(nudge 提示)不可靠——软提示可跳过。
     Hermes 用独立 fork:每轮结束后,daemon 线程回放对话快照,问自己
     "该存什么记忆/技能?"——nudge 是提示,review 是强制。

┌────────────────────────────────────────────────────────────┐
│ 触发(turn_finalizer.py:753-766):                          │
│   final_response 存在 + 未中断 + 非 skip + (memory/skill nudge 计数到)│
│   → _spawn_background_review(回合收尾,响应已交付后——不与用户任务抢注意力)│
├────────────────────────────────────────────────────────────┤
│ fork(run_agent.py:1803):                                   │
│   daemon 线程 + propagate_context_to_thread(profile 隔离)  │
├────────────────────────────────────────────────────────────┤
│ 审查代理(background_review.py:654+):                       │
│   继承父运行时(provider/model/credential/缓存 system prompt)│
│   工具白名单:memory + skills 族,其余运行时拒绝             │
│   skip_memory=True(外部 provider 零副作用)+ _persist_disabled=True(会话零污染)│
├────────────────────────────────────────────────────────────┤
│ 输出:动作摘要(记忆/技能成功操作)→ 用户可见 + 回调          │
└────────────────────────────────────────────────────────────┘
```

**与 nudge 的关系**:nudge = 回合间计数驱动提示模型"是否值得记忆"(软);background_review = 每轮后独立 fork 回放"该存什么"(硬)。互补双通道。

---

## 二、设计 1:触发条件(回合收尾 + 响应后)

**位置**:`agent/turn_finalizer.py:753-766` + `agent/codex_runtime.py:913`

```
if (final_response and not interrupted
    and not skip_background_review
    and (_should_review_memory or _should_review_skills)):
    agent._spawn_background_review(messages_snapshot=list(messages), ...)

触发时机语义:
- 响应交付后触发——review 永不与用户任务竞争模型注意力
- 中断回合不触发(半截结果不入知识库——与 hq1"中断不持久化"同族)
- skip_background_review=True(cron)抑制——review fork 另 spawn AIAgent(~30K tokens/事件),
  cron 会话无人机交互收益

⚠ codex 路径差异(codex_runtime.py:913):触发条件无 skip_background_review 检查
  (注释自称"same cadence + signature as the default path"但该守卫缺失)
  ——产品实现须确认 codex 路径与默认路径守卫一致。

nudge 计数(_should_review_*):
- _skill_nudge_interval 到 + "skill_manage" 在有效工具集 → review skills
- memory 同理;触发后 _iters_since_skill 归零

测试(test_skip_background_review.py):
- cron 构造设置 skip_background_review ✓
- finalize_turn 跳过/触发按标志 ✓
```

**产品④映射**:知识沉淀触发 = 回合完成 + 未中断 + 非无人值守;成本有意识(skip 给 cron)。

## 设计 2:审查 fork 的运行时继承(缓存温 + 同凭证)

**位置**:`background_review.py:47-111`(_resolve_review_runtime)+ `736-811`(fork 构造)

```
继承父活运行时(默认 auto/未设/与父同模型):
  provider/model/api_key/base_url/api_mode/credential_pool/request_overrides
  codex_app_server → codex_responses 降级(API 模式不可继承原样)

路由(auxiliary.background_review.{provider,model}):
  设了不同模型 → resolve_runtime_provider 解析, routed=True
  → 不同模型冷缓存(fork 重建 system prompt)→ 重放压缩 DIGEST(见设计 3)

同模型路径缓存温(关键,~26% 端到端成本降低,PR #17276):
- review_agent._cached_system_prompt = agent._cached_system_prompt(继承父缓存 system prompt)
- session_id/session_start 钉到父的(任何重渲染路径字节一致)
- enabled_toolsets/disabled_toolsets 匹配父 → tools[] 字节一致(Anthropic 缓存键含 tools)
- reasoning_config 匹配父(缓存键按 thinking 存在性命名空间)
- ephemeral_system_prompt 继承(网关会话上下文在 API 调用时附加——保持完整前缀一致)
- prefill_messages 深拷贝继承(unicode 修复路径原地变异——共享 dict 会让 fork 改写父的 prefill)
- OpenRouter provider 路由 pin 全继承(缓存按 UPSTREAM provider——缺 pin 可能路由到不同上游错失温缓存)
- _skip_mcp_refresh = True(turn 间 MCP 刷新会加晚连 MCP 工具破坏 tools 字节一致)

凭证继承:无此则 AIAgent.__init__ 从 env 重跑自动解析——OAuth-only provider/
  session-scoped creds/credential-pool 无法重建 → 回合尾幽灵 "No LLM provider configured"
```

**产品④映射**:审查/辅助任务必须继承主运行时——凭证不可重建 + 缓存温是两个真实事故类。

## 设计 3:路由摘要(digest vs 全量重放)

**位置**:`background_review.py:123-164`(_digest_history)+ `967-974`(选择)

```
策略(整段注释是政策本身):
  同模型 → 全量重放(缓存温,便宜缓存读)
  不同模型 → 压缩 DIGEST(缓存反正冷,减少冷写 token 是纯赚)

_digest_history(tail=24):
  最近 24 条逐字保留(头若 tool 角色则扩,保角色交替)
  旧轮次压缩为单条合成 user-role digest:
    USER: text[:300] / ASSISTANT[tools: 名列表] / ASSISTANT: text[:200]
  前缀声明"Earlier conversation digest — older turns summarised to bound
  the review's cold-write cost..."

测试(test_refine_focus.py):
- 无 focus → prompt 字节一致(自动 post-turn 审查与之前完全一致)
- focus(/refine 用户引导)→ 追加到 prompt(见设计 6)
```

**产品④映射**:审查输入的上下文策略——温缓存全量,冷缓存压缩;"字节一致"是缓存命中的纪律。

## 设计 4:工具白名单运行时拒绝(权限收敛)

**位置**:`background_review.py:933-959` + `943-952`(白名单构造)

```
review_toolsets = ["skills"] + (["memory"] 若 memory_enabled/user_profile_enabled)
  —— 不硬编码 ["memory","skills"]:profile 设 memory_enabled:false 时
     仍给 memory 工具 = 污染禁用记忆的 profile(#54937 layer 2)

review_whitelist = 白名单工具名集合
set_thread_tool_whitelist(whitelist, deny_msg_fmt="Background review denied
  non-whitelisted tool: {tool_name}. Only memory/skill tools are allowed.")
→ 白名单外工具运行时拒绝(不是 schema 隐藏——LLM 可见但调用被拒)

线程作用域:set_thread_tool_whitelist 只影响本线程;finally clear

审批回调:_bg_review_auto_deny(无用户在场 → 危险命令一律 deny,
  防 input() 从 worker 线程与父 TUI 死锁 #15216——与 delegate_tool 同模式)
```

**正确性价值**:
1. 白名单 = 运行时拒绝,不是 schema 裁剪(模型可见但被拒——诚实)
2. memory 工具按 profile 标志条件授予(不污染禁用记忆的 profile)
3. 线程作用域隔离(不清空其他线程白名单)

**产品④映射**:自主维护的权限收敛——"能看见但不能调"比"看不见"更诚实;按 profile 条件授予。

## 设计 5:持久化隔离(_persist_disabled——curator 接管根因修复)

**位置**:`background_review.py:843-856`(PERSISTENCE ISOLATION 注释)+ `897-907`(压缩禁用)

```
问题(curator-takeover 根因):fork 共享父 session_id(缓存温所需),
  否则它把审查 harness 回合("Review the conversation above and update
  the skill library…")+ 自身响应直接写进用户真实会话 state.db。
  用户下一活回合重读被注入的 user 消息 = 常设指令 → agent"变成"curator,
  拒绝真实任务。

隔离:
- _persist_disabled = True——硬停每条 DB 写/懒开路径
  (_flush_messages_to_session_db/_ensure_db_session/_get_session_db_for_recall)
- _session_db = None + _session_json_enabled = False
- 审查只通过工具写记忆/技能存储(它需要的一切)
- skip_memory=True——fork 不触碰外部 memory 插件(honcho/mem0…):
  否则 __init__ 重建 _memory_manager 以父 session_id 作用域,
  run_conversation 经三处注入点(on_turn_start/prefetch_all/sync_all)
  把 harness prompt 泄漏进用户真实记忆命名空间;
  内置 MEMORY.md/USER.md 状态从父重绑——memory(action=add) 仍落盘,
  外部 provider 零副作用
- _end_session_on_close = False——fork 共享父活 session_id,
  不经 opt-out 则 close() 会在会话中途 finalize 父的 session 行
  (review 每 ~10 回合触发)
- compression_enabled = False——fork 共享父 session_id,
  若赢压缩竞态会把父旋转进新 child 而 gateway 永不采纳(fork 单生命周期
  即死)→ 父下次从 stale 起再压 → 同父双 sibling 子(#38727);
  且 review 需全上下文产好摘要

测试(test_background_review.py):
- test_background_review_fork_opts_out_of_session_finalization ✓
- test_background_review_shuts_down_memory_provider_before_close ✓
```

**正确性价值**:**自主维护的隔离是最深的一层**——不隔离则审查自己的动作变成用户会话的指令(fork 写污染主会话 = curator 接管事故类)。每个共享父状态的属性(fork 都共享 session_id)都要显式 opt-out。

**产品④映射**:知识库自主维护必须双隔离——外部 provider 零副作用(skip_memory)+ 主会话零污染(_persist_disabled);共享状态必须显式 opt-out。

## 设计 6:动作摘要 + 用户引导(/refine focus)

**位置**:`background_review.py:410-624`(summarize_background_review_actions)+ `1093-1134`(spawn/focus)

```
摘要(人类可见):
- notification_mode: off(无动作)/ on(通用"Memory updated")/ verbose(内容预览)
- 跳过 prior_snapshot 已存在的工具消息(#14944:继承历史会把陈旧 created/updated
  当新鲜)——existing_tool_call_ids/existing_tool_contents 双键去重
- notify_tools = {"memory", "skill_manage"}——辅助工具成功不冒充记忆工作
- 工具结果 JSON 防畸形(#59437):_change 可能是 list/int/scalar,
  统一 dict 归一化 + 异常 → 空 actions(部分有效动作仍返回,不炸整个 review)
- 摘要:dict.fromkeys 去重 + " · " 连接 → _safe_print + background_review_callback

focus(/refine [instructions] 用户引导):
- spawn_background_review_thread(focus=...)→ 追加到选中 prompt
  ("The user explicitly requested this review with the following focus —
  prioritize it over the general instructions above")
- 自动 post-turn 审查 focus=None → prompt 字节一致(测试锁定)

测试(test_background_review.py):
- test_memory_notifications_off_returns_nothing ✓
- test_skill_patch_off_silent_verbose_shows_diff ✓
- test_refine_focus.py:无 focus 字节一致 / focus 追加 / memory-only 兼容 ✓
```

**产品④映射**:自主维护的用户可见性——动作摘要(off/on/verbose 三档)+ 用户可引导(/refine 等价物)。

## 设计 7:跨回合取消(新活回合取消仍在跑的 review)

**位置**:`background_review.py:909-931`(注册)+ `688-711`(_unregister)

```
注册:
- agent._background_review_agent = review_agent(锁保护)——下一活回合
  用直接指针主动取消仍在跑的 review
- agent._active_children.append(review_agent)——与子代理委派同一列表,
  interrupt() 扇出覆盖

原因:review 仍在流式时新回合启动 = 同 session_id/凭证竞速 → 双 prompt-token 记账
  + Ctrl+C 无效锁死

_unregister(幂等):从两个槽清除;run_conversation finally + 外层安全网 finally
  双调用点(异常路径覆盖 setup 与 run 之间注册后未达 finally 的缺口)

测试(test_background_review.py):
- test_background_review_registers_on_active_children_for_interrupt ✓
- test_new_live_turn_cancels_still_running_background_review ✓
```

**产品④映射**:后台任务与主回合的互斥——"新回合开始必须能取消仍在跑的后台审查"(防双 agent 并发出站)。

## 设计 8:线程级静音(不干扰其他线程)

**位置**:`background_review.py:713-722`(thread_scoped_silence)+ `1042-1054`(摘要输出)

```
- thread_scoped_silence 只静音本 worker 线程(stdout/stderr→devnull)
  ——进程级 redirect_stdout 会把网关事件循环线程(Telegram 长轮询)也静音
    数十秒,吞掉他们的控制台输出(#55769/#55925)
- 审查 fork suppress_status_output=True——预算耗尽/限流重试/压缩警告
  等生命周期消息经 _emit_status → _vprint 泄漏(不走 sys.stdout,
  重定向挡不住)→ 用户只看到成功动作摘要
- 收尾(shutdown_memory_provider/close)也在静音内——Honcho flush/
  Hindsight sync 线程收尾静默
```

**产品④映射**:后台任务线程隔离——静音只作用于自己线程,永不干扰主线程/网关线程。

---

## 三、与四项目对比(自主知识维护)

| 维度 | Pi | Reasonix | Hermes | 产品取 |
|------|----|----------|--------|--------|
| 沉淀触发 | — | — | **nudge(软)+ background_review fork(硬)** | 双通道 |
| 审查形态 | — | 独立审查器(goaleval) | **独立 fork AIAgent 回放对话** | fork 形态 |
| 工具权限 | — | 无工具 | **白名单运行时拒绝 + auto-deny 审批** | Hermes |
| 隔离 | — | 无历史/无压缩 | **skip_memory + _persist_disabled + 压缩禁用** | Hermes(最强) |
| 缓存 | — | — | **继承父缓存 system prompt(温)+ 路由摘要(冷)** | Hermes |
| 输出 | — | Verdict 四态 | **动作摘要 off/on/verbose + 用户引导** | Hermes |
| 取消 | — | — | **active_children 注册 + 新回合取消** | Hermes |

**结论**:产品"知识沉淀硬通道"参考 = Hermes background_review 全案。与 Reasonix goaleval 的关系:**goaleval 是"验收内容质量"的独立审查器(无工具);background_review 是"沉淀知识"的独立 fork(限 memory/skill 工具)**——两个独立审查器家族,一个验收一个沉淀。

---

## 四、面试弹药

1. **"nudge 是提示,review 是强制"**:软提示可跳过;独立 fork 回放对话是硬沉淀通道
2. **"curator 接管根因"**:fork 共享父 session_id(缓存温),不隔离则审查 harness 回合写进用户真实会话 → 用户下回合把它当常设指令,agent"变成"curator——_persist_disabled 硬停每条 DB 写
3. **"缓存温省 26%"**(PR #17276):继承父缓存 system prompt + tools 字节一致 + session_id/session_start 钉住——不同模型则冷缓存 + 压缩 digest 纯赚
4. **"白名单是运行时拒绝不是 schema 裁剪"**:模型可见工具但调用被拒——诚实;memory 工具按 profile 标志条件授予(#54937)
5. **"共享状态必须显式 opt-out"**:fork 共享父 session_id 的每个后果(会话 finalize/压缩竞态/MCP 刷新)都有对应 opt-out(_end_session_on_close/compression_enabled/_skip_mcp_refresh)
6. **"新回合取消仍在跑的 review"**:active_children 注册(与委派同列表)+ 直接指针——防同 session 双 agent 并发出站

---

## 五、产品映射汇总

| 设计 | 产品④用法 |
|------|---------|
| 回合收尾触发 | 知识沉淀硬通道(响应后 + 未中断 + 非 cron) |
| 运行时继承 | 审查/辅助任务继承主凭证 + 缓存温 |
| 路由摘要 | 温缓存全量/冷缓存 digest("字节一致"纪律) |
| 白名单运行时拒绝 | 自主维护权限收敛 + profile 条件授予 |
| 持久化隔离 | fork 写主会话 = 污染事故类(skip_memory + _persist_disabled) |
| 动作摘要 + focus | 自主维护可见性(三档)+ 用户引导 |
| 跨回合取消 | 后台任务与主回合互斥 |
| 线程级静音 | 后台线程不干扰主线程/网关线程 |

> 覆盖设计数:8(设计 1-8)
> 测试契约:test_background_review.py(6:内存 provider 关停顺序/fork 不 finalize 会话/active_children 注册/新回合取消/通知 off/技能 patch 摘要)+ test_skip_background_review.py(5:cron skip 语义)+ test_refine_focus.py(3:字节一致/focus 追加/memory-only)
> 触发点:turn_finalizer.py:753-766(默认路径)/codex_runtime.py:913(codex 路径)/run_agent.py:1803(spawn 包装)
