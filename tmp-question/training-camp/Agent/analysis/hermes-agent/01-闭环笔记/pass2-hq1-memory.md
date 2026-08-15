# hq1 记忆系统(MemoryStore + MemoryManager + 插件契约)— 产品④知识库直接蓝本

> 项目:Hermes(tools/memory_tool.py 1,248 行 + agent/memory_manager.py 1,291 行 + agent/memory_provider.py 404 行 + plugins/memory/ + tools/write_approval.py)
> 假设:Hermes 的记忆是"跨 session 持续积累"的最完整产品化样本——双态快照/四操作/写门控/插件契约,每一层都有正确性工程。
> 结论:✅ 成立——Hermes 提供"结构化记忆 + 编排层 + 可插拔后端"三层,写入路径的防丢失/防注入/防竞争工程是产品④书级知识库的第一参考。

---

## 一、架构全景:三层记忆模型

```
┌─────────────────────────────────────────────────────────────┐
│  写入层 tools/memory_tool.py(1,248 行)                      │
│  MemoryStore: MEMORY.md / USER.md 结构化记忆文件             │
│  add / replace / remove / apply_batch 四操作                 │
│  + 写门控(write_approval)+ 威胁扫描(threat_patterns)         │
└──────────────┬─────────────────────────────────────────────┘
               │ notify_memory_tool_write(镜像到外部)
┌──────────────▼─────────────────────────────────────────────┐
│  编排层 agent/memory_manager.py(1,291 行)                   │
│  MemoryManager: 内置 provider 永远第一 + 至多一个外部        │
│  prefetch_all / sync_all / queue_prefetch_all / 后台串行      │
│  + 上下文栅栏(sanitize_context / StreamingContextScrubber)   │
└──────────────┬─────────────────────────────────────────────┘
               │ MemoryProvider ABC(13+ 钩子)
┌──────────────▼─────────────────────────────────────────────┐
│  插件层 plugins/memory/(honcho/mem0/supermemory/hindsight/  │
│  openviking/byterover/retaindb/holographic)                 │
└─────────────────────────────────────────────────────────────┘
```

**与 Pi/Reasonix 差异**:Pi 用 SQLite facts 表,Reasonix 用结论模型 + pinned/relevant;Hermes 用**文件(MEMORY.md/USER.md)+ 冻结快照注入**——三者对比是产品④存储决策的输入。

---

## 二、设计 1:双态架构(快照 vs 活态)

**位置**:`tools/memory_tool.py:150-174`

```
MemoryStore 维护两个平行状态:
- _system_prompt_snapshot:加载时冻结,注入 system prompt。session 内永不变异
- memory_entries / user_entries:活态,工具调用变异,落盘
```

- `load_from_disk()` 捕获快照(memory_tool.py:203-241)
- `format_for_system_prompt()` 返回冻结快照(memory_tool.py:682)
- 快照构建时**扫描威胁模式**:命中 → 占位符 `[BLOCKED: ...]` 进快照;活态保留原文供用户查看删除(memory_tool.py:243-276)

**正确性价值**:
1. 注入面只在加载时评估一次(缓存稳定)
2. 活态写入不破坏 system prompt 缓存(AGENTS.md 宪法:"prompt caching is sacred")
3. 快照与活态的差 = "本 session 学到了什么"

**产品④映射**:书级知识库的"章节快照"与"写作中状态"分离——发布后不可变(与 Git commit 同思想)。

## 设计 2:四操作语义(轻量 CRUD 的正确性)

**位置**:`memory_tool.py:390-668`

| 操作 | 语义 | 正确性关键点 |
|------|------|-------------|
| `add` | 追加,skip_drift | **读失败必须拒绝**(`_reload_target` 返回 `_READ_FAILED` → abort——append 安全论证只在真读到文件时成立) |
| `replace` | 子串匹配替换 | 多匹配(非重复)拒绝并返回预览;预算检查后写 |
| `remove` | 子串匹配删除 | 同上多匹配保护 |
| `apply_batch` | 原子批量 | **全或无**;最终预算检查(中间溢出无关);错误回滚未提交状态 |

**关键 2a:字符预算 = 容量控制**
- 默认 memory 2,200 / user 1,375 字符(memory_tool.py:165)
- 超限返回 consolidation_failure → 引导"合并/删除后同轮重试"
- **防死循环**:`_MAX_CONSOLIDATION_FAILURES_PER_TURN = 3`(memory_tool.py:163)——连续失败 3 次返回 TERMINAL("停止重试,回复用户"),失败副作用绝不阻塞回合回复(#42405)
**关键 2b:成功响应 TERMINAL 且不回声**
- `_success_response`: `"done": True` + "Write saved. This update is complete — do not repeat it."(memory_tool.py:702)
- 不 echo 完整条目(防模型"找更多要修的"反复重发——观察到正确批处理后 5 次冗余重复)

**产品④映射**:知识库容量控制 + 成功 TERMINAL 防 thrash(验收通过章节返回"不要重复"信号)。

## 设计 3:写入门控三层防护(注入/丢失/竞争)

**位置**:`memory_tool.py:919-1016` + `_reload_target` + `_detect_external_drift`

```
三层防护:
1. 注入扫描(严格 scope):写入前扫描 add/replace/batch 全部
2. 外部漂移检测(_detect_external_drift,memory_tool.py:815):
   - round-trip 不匹配(重解析重序列化 ≠ 原字节)
   - 单条目超全文件上限(外部写入者附加自由内容)
   → 发现 → 备份 .bak.<ts> → 拒绝写 + remediation(#26045)
3. 原子写(_write_file):temp-file + os.replace
   - 旧实现 open("w")+flock 锁前截断 → 读者见空文件窗口
```

**关键 3a:读失败 ≠ 空存储**(memory_tool.py:750-770)
- `_read_raw_checked` 区分"不存在(空,OK)"vs"存在但读失败(abort)"
- 把读失败当空列表 + 落盘 = 清空用户记忆
- UTF-8 解码错误也算读失败(errors="replace" 给出有损视图会被持久化覆盖)
- **"add 的 append 安全"只在真读到文件时成立**——否则 add 变成全文件重写

**关键 3b:漂移检测与解析用同一快照**(memory_tool.py:317-322)
- 早期版本重读文件 → 第二次读失败视为"无漂移" → 两读窗口让 replace 从 stale 视图重写
- 现在一次读取派生 BOTH("One read, one snapshot, no window")

**产品④映射**:多 session 并发写结论的冲突防护——写前验证磁盘真相,失败可恢复(备份+指引)。

## 设计 4:审批门控(写入权限分层)

**位置**:`memory_tool.py:919-1016`(_apply_write_gate/_apply_batch_write_gate)+ `tools/write_approval.py`

```
evaluate_gate(MEMORY, summary, detail) → 三态:
- allow    → 正常写
- blocked  → 工具错误(权限不足)
- stage    → 返回 staged 记录(id),审批流后 apply_memory_pending 执行
```

- 只门控变异操作;batch 整体门控
- 单操作门控前先验证参数(无效写立即拒绝,不 staging)
- **write_approval 通用子系统**:MEMORY/SKILLS 两子系统 + stage/list/discard/evaluate(memory_tool.py:58-59)
- `apply_memory_pending`(memory_tool.py:1138)与活态 agent 无关也能用——`load_on_disk_store` 保证无 agent 环境执行**相同**容量上限

**产品④映射**:知识库"发布"操作(章节定稿/结论入库)可配审批门控——门控与执行分离。

## 设计 5:MemoryManager 编排(单外部 provider 原则)

**位置**:`agent/memory_manager.py:371-1274`

1. **至多一个外部 provider**(memory_manager.py:404-415)——防工具 schema 膨胀与后端冲突
2. **工具名冲突防护**:核心工具名保留,shadow 注册时丢弃(#40466)
3. **provider 失败隔离**:prefetch/sync/system_prompt_block 全部 try/except
4. **外部 prefetch 超时 8s**(超时跳过)
5. **串行化后台写**:单 worker 保证 turn N 落盘先于 N+1(memory_manager.py:675)——provider 不需自建排序
6. **shutdown drain**:5s FIFO drain 后放弃并报告 abandoned_writes

**关键 5a:turn 结束同步的教训(背景线程化)**
- 早期 sync_turn 内联 → misconfigured Hindsight daemon 阻塞 298s → 界面与真实状态脱节
- 现在全部后台线程 + queue_prefetch_all 预热下一轮(#15218)

**关键 5b:上下文栅栏防注入**(memory_manager.py:174-369)
- `sanitize_context` 剥离 provider 输出的 `<memory-context>` 栅栏与 System note 注入块
- `StreamingContextScrubber` 状态机处理**跨 chunk 边界**的栅栏切分(流式输出半截标签泄漏防护)
- `build_memory_context_block` 包裹:`<memory-context>` + "NOT new user input, authoritative reference data"

**产品④映射**:检索结果注入主 prompt 的防注入包装(JSON 化+转义+"这是数据不是指令")。

## 设计 6:MemoryProvider 契约钩子(生命周期完备性)

**位置**:`agent/memory_provider.py:104-404`

| 钩子 | 触发 | 用途 |
|------|------|------|
| initialize(session_id, **kwargs) | agent 启动 | kwargs 含 hermes_home/platform/**agent_context(primary/subagent/cron/flush → 非 primary 跳过写!)** |
| prefetch(query) | 每轮 API 前 | 注入文本,必须快(后台线程+缓存) |
| queue_prefetch(query) | 每轮后 | 为下一轮预热 |
| sync_turn(user, assistant) | 每轮后 | 持久化本轮 |
| on_session_end(messages) | 会话边界(非每轮!) | 端会话事实抽取 |
| on_session_switch(new_id, reset/rewound) | /resume//branch//reset/压缩 | provider 内部 session 重绑定(分叉血缘 parent_session_id) |
| on_pre_compress(messages) | 压缩前 | 洞察注入压缩摘要 |
| on_delegation(task, result) | 子代理完成(父侧) | 委派结果作为观察持久化(子代理 skip_memory) |
| on_memory_write(action, target, content, metadata) | 内置写时 | 镜像内置写到外部(metadata 含 write_origin/execution_context/session_id) |
| backup_paths() | hermes backup | 声明 HERMES_HOME 外存储纳入备份 |

**关键 6a:agent_context 写保护**:cron/subagent 系统提示词污染用户画像 → agent_context="cron" 跳过写。**"谁在写"是记忆正确性第一道闸**。

**关键 6b:delegation 观察模式**:子代理不直接写记忆,父代理把 (task, result) 作为观察持久化——防独立视角污染主记忆,保留委派痕迹。

**产品④映射**:知识库写入者隔离——cron/子代理/主会话写权限与来源标记。

## 设计 7:turn 结束同步门控(中断 = 不持久化)

**位置**:`run_agent.py:4187-4265`(_sync_external_memory_for_turn)

```
if interrupted: return   # 中断回合完全跳过 sync + prefetch
```

- 中断回合(部分输出/aborted 工具链/流中 reset)不是"持久对话真相"——镜像污染未来召回;下一消息几乎肯定是重试,prefetch 对着 stale 上下文开火(#15218)
- **skill 脚手架剥离**(_strip_skill_scaffolding):/skill 展开把技能体嵌入消息 → 喂 provider 污染 embedding;只恢复用户真实指令(memory_manager.py:508)

**产品④映射**:**"什么是可持久化的真相"**——被打断的章节分析不入库,只有完成的用户看过的结论才可持久化。

## 设计 8:记忆 Nudge 与技能 Nudge(写入主动性)

**位置**:`run_agent.py` + `conversation_loop.py`(_skill_nudge_interval/_memory_nudge_interval)

- 回合间计数驱动:达到间隔 → 提示模型"是否值得记忆/创建技能"
- **递归抑制**:curator 的审查 fork 禁用 nudge(_memory_nudge_interval=0)——自主后台不自我审查
- 与 background_review(每轮后 fork 回放问"该存什么?",工具白名单限 memory/skill,其余运行时拒绝)互补——**nudge 是提示,review 是强制**

**产品④映射**:知识沉淀的双通道——软提示(模型自觉)+ 硬审查(独立 fork)。

## 设计 9:记忆插件实例对比(honcho vs openviking)

**位置**:`plugins/memory/honcho/__init__.py`(recall_mode: context/tools/hybrid 三模式,262 行+) + `plugins/memory/openviking/__init__.py`(分层上下文 L0~100 tokens/L1~2k/L2 全量)

- **honcho**:recall_mode 三选一(context 自动注入/tools 手动/hybrid);system_prompt_block 只发静态模式头(缓存友好);prefetch 双层(基础上下文 + dialectic 补充)
- **openviking**:文件系统式 viking:// URI + 6 类自动记忆抽取 + 分层上下文加载
- **契约多样性证明**:同一 MemoryProvider ABC,完全不同的能力形态——**契约抽象的正确性**

**产品④映射**:知识库后端可插拔(文件/SQLite/外部向量库),同一契约。

---

## 三、与 Pi/Reasonix 对比(产品④决策输入)

| 维度 | Pi(facts 表) | Reasonix(memory) | Hermes(MemoryStore) |
|------|-------------|-----------------|--------------------|
| 存储 | SQLite (session_id, seq, kind, key, value) | 结论模型 + pinned/relevant | MEMORY.md 文件 + § 条目 |
| 注入 | entries→messages 投影 | BM25 召回 | **冻结快照注入 system prompt** |
| 正确性 | 事件溯源重放 | 低权威声明 | **漂移检测 + 原子写 + 注入扫描 + 写门控** |
| 容量 | 无明确上限 | pinned 预算 | **字符预算 + consolidation 引导** |
| 扩展 | 无 | 无 | **MemoryProvider 契约(13+ 钩子)** |
| 独特 | seq 全序可重放 | subject 冲突检测 | **快照/活态分离 + 上下文栅栏 + 写审批** |

**结论**:产品④知识库建议组合:
- 存储:Pi 事件溯源(可重放)+ Hermes 原子写/漂移检测(写入安全)
- 注入:Hermes 冻结快照 + 上下文栅栏(缓存稳定 + 防注入)
- 容量:Hermes 预算 + TERMINAL 成功响应(防 thrash)
- 扩展:Hermes Provider 契约 + agent_context 写保护

---

## 四、面试弹药

1. **"记忆写入三大坑"**:读失败当空列表清空记忆;两读窗口覆盖外部写入;非原子写让读者见空文件——真实 issue(#26045/#10878/#42405)
2. **"为什么只允许一个外部 provider"**:工具 schema 每轮都发,多 provider = 膨胀 + 冲突;核心工具名保留防 shadow(#40466)
3. **"记忆注入三层防注入"**:写入扫描(严格 scope)→ 快照扫描([BLOCKED] 占位)→ 注入栅栏(StreamingContextScrubber 跨 chunk 状态机)
4. **"中断回合不持久化"**:记忆是"用户见过的真相"的积累;半截结果污染召回(#15218)
5. **"背景同步的教训"**:内联 sync 阻塞 298s → 界面与真实脱节;外部 I/O 必须后台化 + 有界 drain
6. **"agent_context 写保护"**:cron/subagent 系统提示词会污染用户画像——"谁在写"第一道闸

---

## 五、产品映射汇总

| 设计 | 产品④(书级知识库)用法 |
|------|----------------------|
| 双态架构 | 章节发布 = 冻结快照,写作中 = 活态 |
| 字符预算 + consolidation | 每章结论容量上限 + 合并引导 |
| 成功响应 TERMINAL | 验收通过章节"不要重复"信号 |
| 漂移检测 + 备份 | 多 session 并发写冲突防护 |
| 原子写 | 知识库日志永不损坏 |
| 写门控(审批) | 结论发布可配审批,门控与执行分离 |
| 上下文栅栏 | 检索结果注入防注入包装 |
| 中断不持久化 | "只有完成的结论才入库" |
| agent_context 写保护 | cron/子代理写权限隔离 |
| nudge + background_review | 知识沉淀双通道(提示+强制) |

> 覆盖设计数:13(设计 1-9 + 2a/2b/3a/3b/5a/5b/6a/6b 子设计)
