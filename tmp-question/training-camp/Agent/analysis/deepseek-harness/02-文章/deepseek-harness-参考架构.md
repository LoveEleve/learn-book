# DeepSeek Harness 参考架构 — 源码学习 Agent 的设计蓝图

> 项目:deepseek-ai/deepseek-harness(@deepseek-ai/dsh-root v0.1.0-rc.5,47f943859b)
> 生成:2026-08-15
> 输入:15 份闭环笔记(Q1-Q15,66 设计)+ 域发现 v4(51 域)
> 用途:从 dsh 提取"源码学习 Agent"的四组件设计蓝图——抄什么/改什么/弃什么
> 前置:对照 Pi/Reasonix/OpenCode 参考架构(三项目已分析完)

---

## 一、DeepSeek Harness 架构一句话

> **dsh = 基于 Cordis 的"一切皆插件"Agent Harness:插件贡献服务/类型化事件/可逆效果到共享上下文;无特权核心;每个能力是一个"能力缝"(Service Definition/Provider/Consumer 三角色);会话日志是上下文之源(模型可见 ⟺ 已记录)。**

**与 3 个已分析项目的本质差异**:
- Pi/OpenCode:单体执行引擎 + 事件发布
- Reasonix:长跑 + checkpoint
- **dsh:框架即产品**——核心是插件运行时,产品行为全部由可替换插件组成(profile/bundle 引导)

**对产品的独特价值**:产品 4 组件全部插件化的极端样本——"对齐/执行/验收/知识库"各自可换实现,这是其他 3 项目没有的架构自由度。

---

## 二、产品四组件 ← dsh 设计映射总表

| 产品组件 | dsh 抄什么 | dsh 改什么 | 关键笔记 |
|---------|-----------|-----------|---------|
| ① 对齐模块 | Profile/Bundle 组合(patch 整行替换)、Settings 命名空间、审批政策切换(approval/policy)、指令投影(agent-instructions) | 规格书 = profile 顶层 patch(最后写赢) | Q6/Q9/Q14/Q15 |
| ② 执行引擎 | 能力缝三角色、Turn/Step 双层、Tools 五事件管线、approval 瀑布、Sandbox 每调用政策、Guard 超时/重复提醒 | 章节执行 = 每调用政策切换 + repeat 收敛检测 | Q2/Q3/Q7/Q9/Q10/Q13 |
| ③ 自动验收器 | repeat-tool-reminder(收敛性检测)、工具结果修剪(tool-result-pruner)、llm/stream 瀑布(独立审查调用可拦截)、工具 UI 渲染意图 | 章节 conformance = post-execute 挂点 + 独立审查器(Reasonix 合并) | Q3/Q5/Q11/Q13 |
| ④ 书级知识库 | Session 日志(事件源 + Surface 投影 + 模型可见⟺已记录)、持久化双后端、版本机制(writer 决定)、Session-Query 检索 | 全书 = 一个 session 日志,章节 = 事件过滤 | Q1/Q8/Q12/Q15 |

---

## 三、组件① 对齐模块(6 维盘问)

### 抄自 dsh

| dsh 设计 | 位置 | 产品用法 |
|---------|------|---------|
| **Profile/Bundle 组合**(六层层序 + patch 整行替换) | boot/app-boot + bundle/(base/headless/web-app) | 规格书 = profile 顶层 patch——用户改目标 = 顶层覆盖(最后写赢,防漂移) |
| **Patch 失败策略**(文件缺失硬错/行缺失警告) | app-boot/index.ts:280-340 | 对齐文件缺失 = 硬错(用户点名);域缺失 = 警告(共享 overlay 宽容) |
| **Settings 命名空间 + 事件**(源可追溯) | settings/settings | 对齐设置(深度/画像)可审计(prev/next/source) |
| **审批政策切换**(approval/policy,durable 可重放,LAST = override) | interaction/user-approval:67-80 | 对齐权限预授权(学习模式/写书模式 = 政策切换) |
| **指令投影**(agent-instructions 两提交边界) | context/agent-instructions:80-130 | 领域骨架注入 = 投影(执行谱系 + 封闭步骤两边界后才改 inbox) |
| **Credential 空值即缺席** | credentials/credentials:31-62 | 对齐凭证(空值永不当配置的秘密) |

### 产品设计(改)

```
★ 规格书 = 顶层 patch(关键设计):
  用户"帮我分析 JVM" → 系统组合 base 规格书(bundle 层)
  → 用户回答盘问 = 顶层 cordis.patch.yml(patch 按 id 替换规格书行)
  → --patch overlay = 会话级微调
  最后写赢 + 整行替换——与 OpenCode 的 Epoch 基线互补(基线 = bundle,变更 = patch)

★ 对齐状态可审计:
  settings/updated(prev/next/source)——每次盘问修改有源
  approval/policy 切换 durable 可重放——学习/写书模式切换有日志
```

---

## 四、组件② 执行引擎

### 抄自 dsh(核心骨架)

| dsh 设计 | 位置 | 产品用法 |
|---------|------|---------|
| **能力缝三角色**(Def/Provider/Consumer) | shell/fs/sandbox 各包 | 学习模式(只读 provider)vs 写书模式(写 provider)= provider 切换,Consumer 不动 |
| **Turn/Step 双层 + 0-step 闭合记录** | agent-loop/agent.ts:42-51 + architecture.md:65-90 | 章节执行主干;空步骤也记录(日志记录尝试) |
| **InboxTarget 双队列**(next-turn/next-step) | core/agent/types.ts:10 | 章节输入排队(steer = next-step,新任务 = next-turn) |
| **融合分派三模式**(emit/serial/waterfall) | core/agent/dispatch.ts:28-148 | 扩展点类型安全(subject 与 scope 不分歧) |
| **Tools 五事件管线**(pre/execute/post/code-dispatch-log/result) | core/tools/index.ts:152-207 | 章节工具执行的门/包装/修正/审计四层 |
| **approval 瀑布 + 政策**(ask\|never) | interaction/user-approval:30-100 | 章节写操作审批;never = CI 无头姿态 |
| **Sandbox 每调用政策**(read-only/workspace-write/danger-full-access) | sandbox/sandbox/index.ts:29-69 | 分析只读/写书工作区写——每调用切换 |
| **超时 = execute 瀑布包装**(TOOL_TIMEOUT 结构化结果) | guard/timeout-policy:25-85 | 章节工具超时归一化(模型看到结构化超时非 abort) |
| **Repeat 提醒**(3/5/8 阈值 advisory) | guard/repeat-tool-reminder | 章节循环检测(连续重复调用提示) |
| **Cordis 注册 = 可逆效果**(ctx.effect → disposer) | vendor/cordis + primer | 章节插件卸载自动回滚(HMR 安全) |

### 产品设计(改)

```
★ 章节执行 = 每调用政策切换:
  分析步骤(read/grep)→ read-only 政策
  写书步骤(write/edit)→ workspace-write 政策
  升级(如临时改配置文件)→ 严格更宽阶梯 + approval 通道(执行前 fail-closed)

★ 收敛性(#24 产品化):
  repeat-tool-reminder 模式 → 章节"连续 N 轮无新增"检测
  (advisory 提醒 → 结构化日志 → 验收器消费)
```

---

## 五、组件③ 自动验收器

### 抄自 dsh

| dsh 设计 | 位置 | 产品用法 |
|---------|------|---------|
| **repeat-tool-reminder**(advisory 收敛检测) | guard/repeat-tool-reminder | 章节完成判定输入(连续重复 = 收敛信号) |
| **工具结果修剪**(tool-result-pruner) | compaction/tool-result-pruner | 验收证据大小控制(修剪过大的工具结果) |
| **llm/stream 瀑布**(独立调用可拦截) | llm/llm/src/index.ts:56-70 | 独立审查器调用挂 llm/stream(路由/录制/重试) |
| **工具 UI 渲染意图**(generic/terminal/diff + locations) | core/tools/presentation.ts | 验收输出呈现(章节产出 diff 视图) |
| **approval/request 瀑布**(四结局) | interaction/user-approval:30 | 验收"不过"的处置(拒绝/纠正进上下文) |
| **Tracing**(session-query/tracing + telemetry-otel) | session-query + session-telemetry-otel | 验收审计链路 |

### 产品设计(改)

```
★ 章节 conformance(合并 Reasonix 独立审查器):
  dsh 的 post-execute 挂点 + llm/stream 拦截 = 审查器通道
  + Reasonix 的独立审查器四隔离(无工具/无历史/无压缩/缓存隔离)
  ——dsh 提供"拦截面",Reasonix 提供"审查器形态"

★ 验收审计:
  approval/asked + decided(durable 非 surface)——谁批了啥有日志
  goal/change 事件——章节目标变更可重放
```

---

## 六、组件④ 书级知识库

### 抄自 dsh(理论基础)

| dsh 设计 | 位置 | 产品用法 |
|---------|------|---------|
| **Session 日志**(追加写事件源 + 模型可见⟺已记录) | core/session/index.ts:37-87 | 全书知识库 = 一个 session 日志(章节 = 事件过滤) |
| **Surface 投影**(3 事件 → LLM 消息 + append/replace 阴影) | core/session/surface.ts:15-114 | 模型视图 vs 人类转写分离(replace 阴影只影响模型) |
| **SessionEventMap 声明合并**(插件扩展事件) | core/session/types.ts:236-335 | 知识库事件 schema = 声明合并扩展(构建期强制) |
| **writer 决定 bump**(结构级 + ignorable 词汇级) | core/session/types.ts:51-91 | 知识库版本策略(比 OpenCode versionedType 更严格) |
| **WriteBehind 批量 + 耐久屏障** | session/persistence/write-behind | 知识库写性能(批量 200ms + flush 显式屏障) |
| **双后端**(JSONL+zstd vs SQLite) | session/persistence-* | 部署差异(本地/Web) |
| **Session-Query 检索**(corpus/cursor/filters) | session-query | 全书检索(章节/概念查询) |
| **Session-Title sole provider** | session/session-title* | 章节标题生成(LLM 变体) |
| **Typert 类型图生成器** | typert/generator(6245) | 知识库 schema 从源码生成(类型即真相) |

### 产品设计(改)

```
★ 章节 = 事件过滤(不是独立日志):
  全书一个 session 日志(turn/step 全序)
  章节 = session-query 过滤(turn 范围 + 目标事件)
  ——与 OpenCode 的"全书一个聚合"一致,但 dsh 有现成查询工具

★ 模型可见 ⟺ 已记录(运行时不变量):
  任何新模型可见输入(如章节结论)⟹ 新 session 事件
  ——产品④的最强正确性强制(比"应该记录"强一个量级)
```

---

## 七、D17 落地性检查(5 问)

### 1. 数据怎么流动(三合一原则)

```
规格书(1 份)三处消费(dsh 版):
- profile 顶层 patch(bundle 基线 + 用户覆盖)——执行引擎的初始上下文
- approval/policy 切换(durable)——权限预授权
- 章节验收基准(repeat 收敛 + post-execute 挂点)
答案:dsh 的 profile/bundle 层序天然支撑"基线 + 覆盖"——用户改规格书 = 顶层 patch,三处消费同源。
```

### 2. 时机怎么定

```
- 验收时机:step/end 后(工具结算完,post-execute 可修正)→ 章节完成判定
- 压缩时机:compaction 缝(agent 上下文 + tool-result-pruner)+ 手动命令(command-compact)
- 上下文更新:agent-inject() 在下一个被承认的请求落地(architecture.md:120)
- 审批:一切执行前(approveEscalation fail-closed)
```

### 3. 失败怎么循环

```
- 工具失败 → tool/result(冻结 JSON)进日志 → LLM 自纠(工具结果在历史)
- 审批拒绝 → approval/decided(rejected)→ 拒绝结果进上下文
- 超时 → TOOL_TIMEOUT 结构化结果(非 abort)→ 模型可重试
- 沙箱拒绝 → denial 方言签名(EROFS/EACCES/EPERM 按后端)→ 模型看到准确拒绝
- 无后端 → SANDBOX_UNAVAILABLE fail-closed(缺隔离 ≠ 命令失败)
- 写失败 → WriteBehind 后台报告(不阻塞 agent)
- 连续重复 → repeat 提醒(advisory)→ 模型自纠正
```

### 4. 大库怎么查

```
- 会话检索:session-query(corpus/cursor/filters/sources)——语料级查询
- 事件回放:surface 折叠(deriveEventMessage 同函数重放 log 前缀)
- 版本兼容:writer 决定 bump + ignorable 词汇级——旧日志读取明确
- 存储:JSONL 行式(可 tail)vs SQLite 表(可 SQL)
```

### 5. 系统怎么自证

```
- 运行时不变量:模型可见 ⟺ 已记录(每包 ./invariant + verify-package-invariants 门禁)
- 契约门禁:verify-export-jsdoc/verify-cordis-config/verify-md-links/verify-doc-budgets
- 测试:test:coverage(per-file 100%)+ test:snapshot(无 key 回放)+ test:e2e(真 API)
- 类型自证:Typert 从源码生成类型目录(类型即真相)
- 文档自证:doc-sync 门禁(文档与代码同步)
```

---

## 八、产品决策清单(dsh 独有贡献)

| # | 决策 | 来源 | 理由 |
|---|------|------|------|
| 1 | **一切皆插件(框架即产品)** | q4 | 可替换性即架构——产品 4 组件都可换实现 |
| 2 | **能力缝三角色** | q7 | 换 provider 换产品(学习/写书 = provider 切换) |
| 3 | **模型可见 ⟺ 已记录(不变量)** | q1 | 正确性最强强制(比"应该记录"强一个量级) |
| 4 | **每调用沙箱政策** | q10 | 分析只读/写书工作区写——政策跟调用走 |
| 5 | **审批政策 ask\|never + fail-closed** | q9 | 无头 CI 姿态可预知(不问即知结局) |
| 6 | **writer 决定版本 bump** | q1 | 结构级 bump + 词汇级 ignorable——版本策略精确 |
| 7 | **waterfall 短路即设计** | q4 | 单决策事件策略监听器不调 next 即拥有决策 |
| 8 | **超时 = 结构化结果(TOOL_TIMEOUT)** | q13 | 模型看到结构化超时非 abort——失败归一化 |
| 9 | **重复提醒 advisory(3/5/8)** | q13 | 收敛性检测不 veto(提醒而非强制) |
| 10 | **Surface 双视图(append/replace 阴影)** | q1 | 人类转写不被替换抹掉 |
| 11 | **融合分派(subject 与 scope 不分歧)** | q2 | 扩展点类型安全(类型级强制) |
| 12 | **空值即缺席(凭证)** | q14 | 空白永不当配置的秘密 |

---

## 九、弃用清单(产品不抄)

| dsh 设计 | 弃用原因 |
|---------|---------|
| Cordis vendored 框架整体 | 产品不需要自己托管插件框架(抄模式不抄框架) |
| Profile/Bundle 文件格式 | 产品规格书是结构化 schema,不是 YAML patch |
| Typert 类型图生成器 | 产品文档不需要从源码生成目录 |
| Python SDK + 捆绑运行时 | 产品 MVP 不需要 |
| 双后端持久化(JSONL/SQLite) | 产品 MVP 单后端(先 JSONL) |
| Landlock/Windows ACL 原生 | 产品 MVP 无沙箱(授权层先行) |
| Claude/Codex hook 桥 | 外部工具集成,非产品核心 |
| Web 客户端(~30 UI 包) | 前端视觉(排除原则) |

---

## 十、与 Pi/Reasonix/OpenCode 的合并要点

| 维度 | OpenCode | dsh | 产品取 |
|------|----------|----|--------|
| 循环 | DB 收件箱双层 | Turn/Step + InboxTarget 双队列 | OpenCode(可重放) + dsh(双队列语义) |
| 上下文 | Epoch 基线+时间序更新 | profile 层序 + patch 覆盖 | OpenCode 基线 + dsh 覆盖模型 |
| 验收 | 事件管线+快照对 | repeat 收敛 + post-execute 挂点 | OpenCode 事件 + dsh 收敛检测 + Reasonix 独立审查 |
| 知识库 | 双游标+投影器+版本化 | 日志不变量 + surface 双视图 | dsh 不变量最强 |
| 规格书 | Epoch 基线+批量提问 | profile 顶层 patch | OpenCode 基线 + dsh patch 覆盖 |
| 扩展 | 插件窄能力 | 能力缝三角色 | dsh 能力缝(产品组件插件化) |

---

## 十一、覆盖对账表(15 笔记 → 蓝图去向)

| 笔记 | 设计数 | 蓝图去向 |
|------|:--:|---------|
| q1 session-log | 6 | ④核心(日志不变量/surface/版本) |
| q2 agent-loop | 5 | ②核心(融合分派/Turn-Step) |
| q3 tools | 5 | ②核心(五事件管线)+③(挂点) |
| q4 cordis | 5 | ②核心(框架机制,抄模式弃框架) |
| q5 llm | 5 | ②核心(llm/stream 瀑布)+③(审查通道) |
| q6 profile-bundle | 4 | ①核心(规格书=顶层 patch) |
| q7 capability-seams | 4 | ②核心(能力缝三角色) |
| q8 persistence | 5 | ④支撑(WriteBehind/双后端) |
| q9 interaction | 5 | ②核心(approval)+①(政策切换) |
| q10 sandbox | 5 | ②核心(每调用政策) |
| q11 compaction-skill-subagent | 4 | ②支撑(三缝样本) |
| q12 sdk-api-typert | 5 | ④支撑(检索/生成器) |
| q13 guard-goal-jobs | 4 | ③核心(收敛检测/超时) |
| q14 settings-credentials | 4 | ①支撑(设置/凭证) |
| q15 support-domains | 6 | ④支撑(指令投影/存储/检索) |

**统计**:②执行 11 份 / ④知识库 6 份 / ①对齐 3 份 / ③验收 2 份(交叉计)
**结论**:核心产品组件(①②④)覆盖 20 份;弃用 7 项(V1/外部/框架)——与弃用清单一致。

---

## 十二、落地缺口清单(产品需新增的设计)

| # | 缺口 | 证据 | 产品设计 |
|---|------|------|---------|
| A | **无章节概念**(日志是会话级) | SessionEventMap 全 session 级(turn/step 编号) | 章节 = turn 范围 + session-query 过滤(不需新事件) |
| B | **规格书不是 profile**(dsh 的 profile 是 YAML 文件) | bundle/base/cordis.patch.yml 行级 | 产品规格书 = 结构化 schema,用"配置对象"而非 YAML patch |
| C | **验收器无独立形态**(dsh 的审查挂 post-execute) | tools 管线无独立审查器 | 合并 Reasonix 独立审查器四隔离(挂 llm/stream) |
| D | **无章节摘要模板**(dsh compaction 是 agent 上下文) | compaction 缝无 7 段模板 | 引入 OpenCode 7 段摘要模板(章节交接) |

**说明**:A-D 是"dsh 没做章节/验收/摘要"的产品新增,与 OpenCode 缺口互补(dsh 提供机制面,OpenCode 提供内容面)。
