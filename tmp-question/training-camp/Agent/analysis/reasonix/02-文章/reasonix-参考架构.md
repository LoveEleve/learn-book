# Reasonix 参考架构 — 源码学习 Agent 的设计蓝图(第二版)

> 项目:esengine/DeepSeek-Reasonix(main-v2,5,453 commits)
> 生成:2026-08-14
> 输入:7 份闭环笔记(RQ1-RQ7,107 设计)+ 域发现(41 域)
> 用途:从 Reasonix 提取产品四组件 + 编排 + 回滚的设计——补充 Pi 版没有的设计

---

## 一、Reasonix 架构一句话

> **Reasonix = 单控制器(control.Controller)驱动的、缓存优先(前缀字节稳定)的、双模型(planner+executor)协作的、带任务契约/检查点/记忆冲突模型的 Go Agent 引擎。**

四个定语对应四个核心设计:单控制器(control)、缓存优先(context_manager)、双模型(coordinator)、契约与回滚(taskcontract/checkpoint/memory)。

---

## 二、产品四组件 ← Reasonix 设计映射总表

| 产品组件 | Reasonix 抄什么 | Pi 版的补充 |
|---------|----------------|------------|
| ① 对齐模块 | TaskSpec + PlanContract(规格书数据模型)+ Coordinator 路由 | **规格书从"想法"变成"数据模型"** |
| ② 执行引擎 | ContextManager(缓存优先)+ 双层循环(Pi 已有)+ 写逃逸检测 | **缓存优先原则 + 写范围保护** |
| ③ 自动验收器 | Goaleval + BoundedLLM(独立审查器)+ autoresearch 验收算法 | **验收器从"检查单"变成"独立审查器"** |
| ④ 书级知识库 | Memory(subject 冲突/缓存优先)+ History(BM25)+ 7 标题摘要模板 | **结论防矛盾 + 记忆分级激活** |

---

## 三、组件① 对齐模块 — 规格书数据模型(最重要升级)

### 抄自 Reasonix(两个蓝本合并)

| Reasonix 设计 | 位置 | 产品用法 |
|--------------|------|---------|
| **TaskSpec 三合一**(goal/scope/non_goals/allowed_operations/success_criteria+evidence_ids) | autoresearch/task.go:33-40 | 规格书 schema |
| **验收算法**(证据计数 + 必选判据检查) | autoresearch/summary.go:31-54 | 验收器核心逻辑 |
| **nextRequiredAction 决策链**(blocked/stale≥4问人/stale≥2转向) | summary.go:74-82 | 分析卡死自动恢复 |
| **stale/pivot 计数** | task.go:42-50 | #24 死循环量化检测 |
| **PlanContract 数据模型**(Objective/Assumptions/Steps/Acceptance/Verification/Risks) | plancontract/plan.go | 规格书完整结构 |
| **host 分配 Identity**(ID/Revision json:"-") | plan.go:17-19 | 审批可 diff |
| **Assumption.Confirm**(假设+最便宜验证) | plan.go:29-32 | 假设可验证性 |
| **Verified vs Candidate**(证据不模糊) | plan.go:42-43 | 猜测不能当事实 |
| **Normalize/Validate 分离**(修复 vs 拒绝) | plan.go:69-240 | 规格书自动修复 |
| **NeedsApproval 扩张才审批**(收窄自动过) | diff.go:84-99 | 审批门黄金语义 |
| **确定性路由**(4 路由 + 3 深度) | planner_route.go | 何时需要规格书 |

### 产品设计(改)— 规格书 schema 定稿

```
产品规格书 = TaskSpec 骨架 + PlanContract 结构:
  goal / scope / non_goals
  allowed_operations(write/network/publish)
  success_criteria:[{id, description, required, evidence_ids}]
  assumptions:[{text, confirm}]   ← 对齐阶段的假设+验证方法
  steps:[{id, title, acceptance, verification, risks, verified_files, candidate_files}]
  learner_profile / output_form(产品新增维度)
ID/Revision 由产品分配(host-owned)
```

---

## 四、组件② 执行引擎 — 缓存优先 + 写保护

### 抄自 Reasonix

| Reasonix 设计 | 位置 | 产品用法 |
|--------------|------|---------|
| **缓存优先**(低于阈值零操作) | context_manager.go:107-121 | 章节未满不压缩 |
| **固定前缀**(system prompt + 首用户 turn 字节稳定) | compact.go:292-301 | 缓存命中区 |
| **多层阈值**(触发 0.85/接受 50%/硬上限/经济性) | compact.go:91-153 | 量化预算 |
| **7 标题摘要模板**(Standing facts/Goal/Decisions/Files/Commands/Errors/Pending) | compact.go:60-85 | 章节摘要模板 |
| **一代一票**(失败阻塞本代) | context_manager.go:123-182 | 摘要失败不重复付费 |
| **stale 保护 + stuck 标记** | context_manager.go:130-179 | 收敛性 |
| **CAS 安装检查点**(5 条件 + 持久化回滚) | compact_commit.go:22-56 | 摘要安装事务性 |
| **摘要接受判定**(严格小于源 + 低于触发线) | compact_projection.go:500-541 | 接受规则集 |
| **压缩作为工具**(模型主动调 + anchor 校验) | compact_projection.go:24-59 | 自主压缩 |
| **写逃逸检测**(写入限计划范围) | plan_contract.go:102-135 | 越界写保护 |
| **Runner 抽象**(单/双模型无感) | coordinator.go:15-19 | 模式无感接口 |
| **分场景降级**(普通降级 vs 边界 fail-closed) | coordinator.go:345-380 | 失败处理 |
| **交接防注入**(7 条 executor 指令) | coordinator.go:668-698 | 规格书不能操纵执行器 |
| **错误三层分层**(Pi 已有:overflow/可恢复/致命) | — | — |

### 产品设计(改)

```
学习模式:只读工具集 + 固定前缀(规格书+首章指令)
写书模式:+写工具 + 写逃逸检测(只能写规格书声明文件)
章节完成:shouldStopAfterTurn 钩子(验收器判据)
压缩:章节摘要 = 7 标题模板 + 一代一票 + CAS 安装
```

---

## 五、组件③ 自动验收器 — 独立审查器

### 抄自 Reasonix

| Reasonix 设计 | 位置 | 产品用法 |
|--------------|------|---------|
| **独立审查器四隔离**(无工具/无历史/无压缩/缓存隔离) | goaleval/evaluator.go:1-9 | 验收器架构 |
| **兜底触发**(模型没报告才评估) | evaluator.go:3-5 | 验收时机 |
| **四种 outcome**(complete/continue/blocked/uncertain) | evaluator.go:46-57 | 章节验收语义 |
| **严格 complete 定义**(完成 + 验证尝试过) | evaluator.go:26-30 | 章节完成判定 |
| **证据不可信 + 防注入** | evaluator.go:41-43 | 验收安全 |
| **多层预算**(字段/请求/输出/时间) | evaluator.go:46-66 | 评估有界 |
| **fail-closed**(错误 = 暂停) | evaluator.go:76-81 | 验收失败不默认通过 |
| **双层输出防线**(MaxTokens + 流级中止) | bounded.go:133-140 | 输出防失控 |
| **温度 0 + 固定 prompt** | bounded.go:117-121 | 可复现 |
| **渐进收缩**(丢体积保身份) | recovery/reviewer.go:242-298 | 证据超预算兜底 |
| **多审查器共享 boundedllm** | boundedllm + 2 消费者 | 审查器族架构 |

### 产品设计(改)

```
验收器 = 独立审查器(goaleval 模式):
  章节完成时:规格书契约检查(evidence_ids 计数)
  四种 outcome + 严格 complete
  证据不可信 + fail-closed + 多层预算
  验收结果写入知识库日志(可重放)
收敛判定:连续 N 轮无新增(不是 agent 说完成)
```

---

## 六、组件④ 书级知识库 — 结论防矛盾

### 抄自 Reasonix

| Reasonix 设计 | 位置 | 产品用法 |
|--------------|------|---------|
| **记忆恰好一次进前缀**(缓存优先极致) | memory/doc.go:1-16 | 已验收结论进稳定前缀 |
| **变更走 tail 注入**(会话中不改前缀) | doc.go:9-13 | 新结论走尾部 |
| **Subject 冲突模型**(一问题一答案) | memory/subject.go | **结论防矛盾** |
| **冲突错误写给模型**(更新那个 id) | subject.go:63-70 | 反馈驱动 |
| **pinned vs relevant 激活** | activation.go | 结论分级 |
| **Freshness 老化** | freshness.go | 结论过期 |
| **低权威声明**(自动召回标注) | auto_recall.go:24 | 旧结论标注可过期 |
| **BM25 + 相对分数地板** | recall.go:110-174 | 结论检索 |
| **Path 故意缺失**(隐私) | recall.go:38-39 | 不暴露本地路径 |
| **ShadowHits 影子排名** | recall.go:57-59 | 算法升级影子测试 |
| **Memory 结构 = 结论数据模型**(ID/Revision/SubjectKey/ExpiresAt/Keywords) | store.go:78-95 | 结论字段 |
| **pinned 预算 1500 + 乐观并发 + create-only** | store_v2.go:128-217 | 写入安全 |
| **Archive 软删除** | store.go:196-257 | 废弃结论可追踪 |
| **Memory + History 双层** | memory/ + history/ | 结论库 + 会话检索 |
| **ResumeFromGoalText**(goal 文本 → 任务恢复) | autoresearch/store.go:107 | #21 交接自动化 |
| **epoch 保护迁移** | goal_legacy.go | 知识库版本升级 |

### 产品设计(改)

```
书级知识库 = Reasonix Memory 模型:
  目录:book/{book-id}/(全书日志)+ chapters/(每章)
  条目:conclusion(结论:subject_key + evidence_ids)/ spec / acceptance
  subject 冲突:同知识点新结论更新旧结论(版本++)
  pinned(已验收常驻,预算内)/ relevant(检索)
  Freshness:源码版本更新后结论老化
  交接:ResumeFromGoalText——"继续分析 JVM"自动恢复
```

---

## 七、回滚与恢复 — Checkpoint

### 抄自 Reasonix

| Reasonix 设计 | 位置 | 产品用法 |
|--------------|------|---------|
| **每轮快照**(编辑前状态 + MsgIndex 边界) | checkpoint.go:65-84 | 章节快照 + 分析回滚 |
| **意图先持久化**(发布前写意图) | transaction.go:695-708 | 崩溃可补偿 |
| **逐文件发布 + 每步持久化** | transaction.go:678-743 | 每步可恢复 |
| **InjectFail 故障注入** | transaction.go:685, 718 | 事务正确性被测试 |
| **Prepare/Commit 双阶段**(预检 + 重验证 + 代) | transaction.go:45-239 | 防过期提交 |
| **MutationBarrier 代号**(不靠墙钟) | barrier.go | 并发变更检测 |
| **事务状态机 + 双态目标**(Restore/Forward) | types.go:191-240 | 回滚/补偿分离 |
| **BlobStore 内容寻址**(SHA-256 + GC) | blob.go | 快照内容小 |
| **Undo 撤销** | transaction.go:242+ | 回滚可撤销 |

### 产品设计(改)

```
产品"章节回滚":
  每章快照(分析前状态)+ 会话回滚边界
  回滚事务:意图先持久化 + 逐文件发布 + InjectFail 测试
  Prepare 预检 + Commit 重验证(代验证防过期)
  大内容 blob 内容寻址
```

---

## 八、Reasonix 独有设计的价值总结

| 设计 | Pi 版没有的 | 产品价值 |
|------|------------|---------|
| **规格书数据模型**(TaskSpec+PlanContract) | ✅ | 规格书从"想法"变成"可 diff 的数据" |
| **独立审查器**(Goaleval+BoundedLLM) | ✅ | 验收器从"检查单"变成"隔离审查" |
| **Subject 冲突模型** | ✅ | 结论防矛盾(同知识点不并存) |
| **缓存优先**(前缀字节稳定) | 部分 | 上下文管理最高原则 |
| **写逃逸检测** | ✅ | 写书模式只写声明文件 |
| **交接防注入**(7 条指令) | ✅ | 规格书不能操纵执行器 |
| **回滚事务**(意图持久化+故障注入) | 部分 | 章节回滚可证明正确 |
| **记忆分级**(pinned/relevant + 预算) | ✅ | 结论常驻 vs 检索 |

## 九、Review 记录

| 轮次 | 覆盖 |
|:--:|------|
| v1 | 7 份闭环笔记全部纳入,107 设计映射 |
| v2 | D17 落地性检查:发现 4 个设计缺口(规格书三合一/验收时机/验收失败循环/跨章节引用)→ 补入 §九.5 |

## 九.5 D17 落地性检查(2026-08-14)— 4 个设计缺口补全

### 缺口 1:规格书三合一(数据流)

**规格书同时是**:
- 执行引擎的初始上下文(执行契约:goal/scope/non_goals/allowed_operations)
- 验收器的契约来源(success_criteria + evidence_ids 是验收基准)
- 知识库的第一条日志(spec 条目)

**一份 spec 三处消费,避免三份漂移**——规格书一改,执行/验收/日志同步。

### 缺口 2:验收时机(时机)

**"何时算章节完成"的明确决策**:
```
触发点:章节摘要已安装(compaction 完成)且规格书成功标准全部满足
  → shouldStopAfterTurn 返回 true
具体判定:open_criteria 为空(所有必选判据有证据)
  或 nextRequiredAction 是 "continue with the next evidence-producing step"
    且已无更多证据可产(探索路径枯竭)
验收频率:每章一次,不是每轮(每轮验收成本太高)
```

### 缺口 3:验收失败循环(失败循环)

**验收 fail-closed 后怎么回到执行**:
```
验收 outcome = blocked 或 uncertain → 失败消息作为 steer 注入执行循环
  → LLM 下一轮修正(带验收理由:"c2 判据缺证据,请补充 file:line")
  → 重新验收
  → 连续 N 轮无改善 → 升级问用户(stale 语义)
```

### 缺口 4:跨章节引用查询路径(大库)

**"第 20 章引用第 3 章结论"的具体路径**:
```
1. 结论库检索(subject_key + BM25)——找到第 3 章结论
2. 结论条目带 evidence_ids → 读对应证据
3. 结论带 acceptance 状态 → 确认已验收
4. 历史会话检索(Around)按需回溯原始分析
查询结果链式返回:结论 → 证据 → 验收状态
```

---

## 十、下一步

- [ ] 用 D17 落地性检查 review 本架构
- [ ] 进入 OpenCode 分析(第三个项目)
- [ ] 或先汇总"产品总设计蓝图"(Pi + Reasonix 合并)
