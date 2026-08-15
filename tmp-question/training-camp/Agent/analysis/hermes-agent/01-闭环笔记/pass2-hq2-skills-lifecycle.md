# hq2 技能生命周期(Skills + Curator + Hub)— 产品④"知识资产治理"蓝本

> 项目:Hermes(tools/skills_tool.py 2,093 + skill_manager_tool.py 1,810 + skills_hub.py 4,620 + skills_guard.py 1,174 + skill_usage.py 1,340 + agent/curator.py 2,019 + agent/learn_prompt.py 237 + agent/skill_commands.py 840)
> 假设:技能(可复用知识单元)有完整生命周期治理(创建→追踪→衰变→归档→恢复),是产品④"防知识库膨胀成垃圾场"的治理样本。
> 结论:✅ 成立——来源分级/用量追踪/三态状态机/LLM 合并审查/安全准入/干跑模式全部具备。

---

## 一、架构全景:技能 = 知识单元的产品化

```
┌────────────────────────────────────────────────────────────┐
│ 创建层:/learn(learn_prompt.py)+ skill_manage(create)        │
│   agent 自主把"刚做过的事"写成 SKILL.md(标准引导)           │
└──────────────┬─────────────────────────────────────────────┘
               │ provenance 标记(created_by: agent)
┌──────────────▼─────────────────────────────────────────────┐
│ 使用层:skill_commands.py(注入 user 消息非 system prompt!)   │
│   + 模板变量/内联 shell/支撑文件发现                        │
└──────────────┬─────────────────────────────────────────────┘
               │ .usage.json 用量追踪(use/view/patch 三计数)
┌──────────────▼─────────────────────────────────────────────┐
│ 治理层:curator.py(自动状态机 + LLM 合并审查 + 报告)         │
└──────────────┬─────────────────────────────────────────────┘
               │ 安装准入:skills_guard 扫描 + hub 校验
┌──────────────▼─────────────────────────────────────────────┐
│ 分发层:skills_hub.py(GitHub 源/SSRF 防护/锁文件/审计)      │
└────────────────────────────────────────────────────────────┘
```

---

## 二、设计 1:三态状态机(active/stale/archived)

**位置**:`tools/skill_usage.py:53-56` + `agent/curator.py:305-380`

```
STATE_ACTIVE → STATE_STALE(超 stale_after_days 未活动)
STATE_STALE → STATE_ARCHIVED(超 archive_after_days)
STATE_STALE → STATE_ACTIVE(再次使用 = reactivated)
STATE_ARCHIVED → (手动 restore,可逆)
```

**状态机正确性细节**:
1. **pinned 豁免**:pinned 技能永不自动迁移(curator.py:331)
2. **cron 引用豁免**:被任何 cron job 引用的技能永不迁移("调度器只在 job 触发时 bump 用量,低频 job 的技能会被错误老化")(curator.py:296-309)
3. **种子锚定**:built-in 首次纳入治理时钟从现在起锚(不 epoch)
4. **零使用宽限**:use_count=0 不立即归档,除非 ≥ stale_after_days 老("use=0 是证据缺失,不是过时证据")

**产品④映射**:**知识保鲜**——章节结论随源码演进过时需三态治理;被依赖结论(pinned/cron 引用)豁免。

## 设计 2:来源分级(Provenance = 治理边界)

**位置**:`tools/skill_usage.py:338-520`

```
治理权限矩阵:
- agent 自创(created_by: "agent")  → 完全可治理
- bundled built-in                 → 默认不可;prune_builtins 开启才可归档
- hub-installed                    → 永不治理(外部拥有者)
- external_dirs 挂载               → 只读
- protected builtins("plan")       → 永不治理(承载斜杠命令 UX)
```

**关键 2a:命名 vs 语义的坑(#67140)**
`created_by` 字段名像"谁写的"(provenance 事实),实际消费为"是否接受自主治理"(策略标志)。文档明确:"created_by: 'agent' 意味着 curator-managed,不是证明 agent 写的"——字段已在磁盘上,改名孤儿化所有记录,保留名字但语义化调用 `is_curator_managed()`。

**关键 2b:记录缺失与显式 null 必须同判(race with bookkeeping)**
后台审查守卫中 key 于 `isinstance(usage_rec, dict)` 的判定导致:本地技能无记录时首写通过,`bump_patch` 创建 `created_by: null` 记录,此后同一写被拒绝——**"恰好允许一次"不是策略,是与自己记账的竞态**。两种形状 fail closed(skill_manager_tool.py:424-450)。

**产品④映射**:知识来源分级(用户/agent/引用)治理边界差异;策略标志与事实字段分离。

## 设计 3:后台审查写守卫(自主执行的权限收敛)

**位置**:`tools/skill_manager_tool.py:301-424`

```
后台审查(自主,无人在场)→ 更严:
- pinned:任何写拒绝(不只删)(#25839)
- external/bundled/hub/protected:全部拒绝
- 非 curator-managed:拒绝(需 hermes curator adopt 主动加入)
- 所有权无法验证:拒绝

前台(用户在场)→ 宽松:
- 可编辑 external/bundled/hub(用户明确指示)
- pinned 只挡删除
```

**关键 3a:读-写前置守卫(Read-Before-Write)**:后台审查**必须先 skill_view 加载目标才能写**(skill_manager_tool.py:424-452)——防 LLM 基于猜测改写技能文件。

**关键 3b:合并删除必须声明去向(fail closed)**:curator 的 LLM 合并 pass 中 delete 唯一合法形式 = 验证过的合并(`absorbed_into` 必须声明且目标存在)(skill_manager_tool.py:463-526);裸 prune 是 fail-open 缺陷(#29912)被拒绝;删除路由到**可恢复归档**而非 rmtree。

**产品④映射**:自主知识维护权限收敛 + 读-写前置 + 破坏性操作可恢复。

## 设计 4:用量追踪(telemetry 驱动治理)

**位置**:`tools/skill_usage.py:852-924`

```
每技能:use_count/view_count/patch_count/last_used_at/last_viewed_at/
       last_patched_at/state/pinned/created_by
latest_activity_at:三时间戳取最新(创建排除——区分从未活跃)
```

- `.usage.json` 跨进程文件锁(skill_usage.py:90-124)
- bump_use 在**工具实际执行时**记录(非注入时)
- **LLM 合并审查明确禁止用用量做合并依据**:"Judge overlap on CONTENT, not on use_count"(curator.py:397)

**产品④映射**:知识热度驱动保鲜优先级,但治理判断以内容为准。

## 设计 5:LLM 合并审查(Umbrella-Building)

**位置**:`agent/curator.py:396-560`(CURATOR_REVIEW_PROMPT)

```
目标 = 类级指令库 + 经验知识;窄技能堆 = 库的失败
判断标准:"人类维护者会写成 N 个独立技能还是一个 N 标签子节的技能?"
三方式:MERGE INTO EXISTING UMBRELLA / CREATE NEW UMBRELLA /
        DEMOTE TO REFERENCES/TEMPLATES/SCRIPTS
强制输出:机器可读 YAML(consolidations: [{from,into,reason}] + prunings)
        ——每个归档技能必须出现在恰好一个列表,驱动 cron 引用迁移
```

**自主运行约束**:
- 独立 fork AIAgent(enabled_toolsets=["skills","terminal"],skip_memory=True)
- 禁用递归 nudge("curator 绝不能 spawn 自己的 review")
- 标记 `_memory_write_origin="background_review"` → 触发写守卫
- stdout/stderr → /dev/null

**产品④映射**:知识库自动重构(窄结论合并为类级章节、细节降级支撑文件、机器可读合并清单驱动引用迁移)。

## 设计 6:干跑模式(Dry-Run = 高风险动作的安全演练)

**位置**:`agent/curator.py:338-355` + `run_curator_review(dry_run=True)`

```
DRY-RUN — REPORT ONLY. DO NOT MUTATE THE SKILL LIBRARY.
- 禁止变异动作;读取随便
- 输出 = 交付物(与 live run 同格式报告)
- 意外变异显式声明供审查者回滚
```

- 干跑**不 bump** last_run_at/run_count(预览不推迟真实运行)
- 仍写 REPORT.md + 记录 last_report_path

**产品④映射**:高风险自动操作先报告后执行,报告与真实同格式。

## 设计 7:运行前快照 + 每次运行报告(可审计性)

**位置**:`agent/curator.py:1510-1530` + `curator_backup.py` + `_write_run_report`

```
- 每次真实运行前:tar.gz 预快照(curator_backup)
- LLM pass 前记录 before_report(报告可 diff)
- 报告:logs/curator/{YYYYMMDD-HHMMSS}/run.json + REPORT.md
- 状态先持久化再跑 LLM pass(崩溃中途仍记录运行,不立即重触发)
- 重命名映射追加到用户可见摘要
```

**产品④映射**:知识库自动维护的三级证据(预快照+报告+diff)。

## 设计 8:安装安全扫描(外来知识的准入检查)

**位置**:`tools/skills_guard.py` + `tools/skills_hub.py`

```
双入口:
1. 威胁模式扫描(THREAT_PATTERNS 正则族):
   - 六类:exfiltration/injection/destructive/persistence/network/obfuscation
   - 分级:critical/high/medium/low + verdict(safe/caution/dangerous)
   - 样例:curl 插值 ${API_KEY}、读 ~/.ssh/~/.aws/.env
2. 信任矩阵决定安装(INSTALL_POLICY):
   - trust_level:builtin/trusted/community × verdict → allow/ask/block
   - dangerous + community/trusted:--force 也不能覆盖
```

**路径安全**:
- `_normalize_bundle_path`:拒绝绝对路径、`..` 穿越、**冒号**(Windows 盘符/NTFS ADS——`file.py:payload` 写入扫描不可见字节)
- 锁文件 install_path 校验:必须以 skill_name 结尾(防 rmtree 擦整个 skills/ 树)
- SSRF:连接时校验 + 无自动重定向 + 重定向目标逐一校验 + 跳转上限
- 支持目录白名单 + 相对引用遍历检测

**产品④映射**:外部导入知识(参考文献/他人结论)的威胁扫描 + 信任分级 + 路径安全三关。

## 设计 9:技能注入(user 消息而非 system prompt)

**位置**:`agent/skill_commands.py:289-400`(_build_skill_message)

```
- 技能内容注入为 user 消息(保 prompt 缓存——skill 清单不是 system prompt)
- 注入元素:激活说明 + 内容 + [Skill directory] + 配置值 + setup note
  + 支撑文件清单(→ skill_view 加载)
- 模板变量替换 + 内联 shell 展开(opt-in)
- stable_prefix 边界声明(Anthropic 缓存断点可放)
- 脚手架标记(_SKILL_INVOCATION_PREFIX 等)让 memory 提取用户真实指令
```

**产品④映射**:知识注入的缓存友好协议——内容进 user 消息,缓存前缀不变。

## 设计 10:/learn 技能创建(标准引导)

**位置**:`agent/learn_prompt.py:1-237`

- **一个提示词驱动**(无独立蒸馏引擎):引导 agent 用现有工具收集来源(代码目录/API 文档/对话)并按 HARDLINE 标准写 SKILL.md
- 标准内嵌:description ≤60 字符/现代章节顺序/Hermes 工具框架化/不发明命令
- 大资料源 → knowledge-base 布局(SKILL.md 索引 + 每章 references/ 按需加载)
- **author 隐私**:永远字面 "Hermes"(防环境派生用户名 = 隐私泄漏)

**产品④映射**:知识创建的标准引导 + 大资料的分层布局(索引+按需章节)。

---

## 三、与 Pi/Reasonix 对比(知识资产治理)

| 维度 | Pi | Reasonix | Hermes |
|------|----|----------|--------|
| 知识单元 | facts(label) | 结论模型 | SKILL.md(带 frontmatter/支撑文件) |
| 生命周期 | 无 | 无 | **三态状态机 + 用量追踪 + 归档恢复** |
| 自动维护 | 无 | 无 | **LLM 伞形合并审查(独立 fork)** |
| 安全准入 | 无 | 无 | **威胁扫描 + 信任矩阵 + 路径校验** |
| 可审计 | 事件日志 | checkpoint | **预快照 + 报告 + diff** |
| 自主权限 | 无 | fail-closed 审查器 | **后台/前台双权限 + 读-写前置** |

**结论**:产品④知识库治理决策输入——知识单元生命周期状态机 + 来源分级 + 用量追踪 + 独立审查器合并 + 干跑 + 审计轨迹 + 外来准入扫描。

---

## 四、面试弹药

1. **"技能库治理三个教训"**:created_by 字段名与语义错位(#67140);记录缺失与 null 不同判导致"恰好允许一次"竞态;裸 prune 的 fail-open(#29912)
2. **"为什么 curator 用独立 fork"**:自主后台维护与用户在场交互权限面必须不同;递归 nudge 会无限自我审查
3. **"use=0 不是过时证据"**:用量是证据缺失;合并看内容不看计数;零使用有宽限
4. **"--force 不能覆盖 dangerous verdict"**:安全分级——高信任用户也不能覆盖危险扫描结论
5. **"干跑三重正确性"**:不 bump 计划/报告同格式/异常显式声明

---

## 五、产品映射汇总

| 设计 | 产品④(书级知识库)用法 |
|------|----------------------|
| 三态状态机 + 引用豁免 | 章节保鲜(被依赖不自动归档) |
| 来源分级 | 用户/agent/引用知识治理边界 |
| 后台审查写守卫 | 自主维护权限收敛 + 读-写前置 |
| absorbed_into 声明 | 章节合并声明去向防引用悬挂 |
| 用量追踪 | 热度驱动优先级(内容为准) |
| LLM 伞形合并 | 知识库自动重构 |
| 干跑模式 | 高风险操作先报告后执行 |
| 预快照+报告+diff | 维护可审计性 |
| 威胁扫描+信任矩阵 | 外来知识准入三关 |
| 注入 user 消息 | 知识注入缓存友好 |
| /learn 标准引导 | 知识创建标准 + 分层布局 |

> 覆盖设计数:14(设计 1-10 + 2a/2b/3a/3b 子设计)
