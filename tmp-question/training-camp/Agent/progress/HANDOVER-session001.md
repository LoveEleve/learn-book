# 交接文档 — Agent 项目 Session 详细交接(2026-08-14)

> 本文件记录 2026-08-14 session 的详细过程、方法、发现与教训。
> 承接:HANDOVER.md(权威进度)
> 本 session 完成:方法论决策(D1-D17)+ JD 分析 + 问题清单 + Pi 分析(87 设计)+ Reasonix 分析(100 设计)

---

## 一、本 session 时间线(按顺序)

### 阶段 1:项目启动(讨论与规划)

1. **定位讨论**:用户提供 4 个 agent 仓库链接(Pi/DeepSeek-Reasonix/Hermes/OpenCode),问是否认识 → 我逐一分析
2. **需求澄清**:用户要面 Kimi/DeepSeek 等,准备自研 agent
3. **建目录**:Agent/ 下建 jd/methodology/prompt/skills/harness/progress + analysis/{pi,reasonix,hermes-agent,opencode}
4. **JD 收集**:用户提供 10 份 JD(DS/Kimi/GLM/MiniMax/字节/腾讯×2/美团×3)→ 全部入库 + 交叉分析

### 阶段 2:方法论设计(讨论迭代)

5. **方法论 v1 讨论**:继承原方法论 + Agent 适配(双锚点/能力域/面试弹药)
6. **用户纠正**:入口点不能用户指定(初学者的困境)→ D13 入口自发现 + D12 三档提问 + D14 推荐带证据
7. **产品定位修正**:用户明确"产品是主线,面试是副产品" → 总纲重写为产品优先版
8. **产品定义澄清**:用户纠正"4 个项目也是举例的,产品是通用的" → 4 项目 = 自举样本,OpenJDK = 实战目标
9. **D15/D16 新增**:理论层检查 + 正确性问题检查(用户追问"有没有分布式/数学理论")
10. **D17 新增**:参考架构落地性检查(参考架构 v3 review 发现)

### 阶段 3:Pi 分析(完整流程)

11. **域发现 v1-v6**:59 域,6 轮 review(每轮都找到新东西)
    - v2:补 skills/session-manager/settings/resource-loader/composer
    - v3:补 cli 会话层/modes/llama 扩展
    - v4:补 images/tools-manager/fs-watch/git/transport
    - v5:补 prompt 模板/约束采样/facts 存储
    - v6:补 SessionMutation/上下文管线
12. **闭环笔记 7 份**:q1-q7(87 设计),每份多轮 review
    - q1 agent-session(3344 行):13 设计,2 轮 review
    - q2 session-manager(1714 行):17 设计,3 轮 review
    - q3 context-compaction(1360 行):16 设计,3 轮 review
    - q4 agent-loop(796 行 + types):14 设计,4 轮 review
    - q5 tools(5340 行):14 设计,3 轮 review
    - q6 skills(896 行):13 设计,3 轮 review
    - q7 distributed-theory:Fencing Token/Lease/Event Sourcing/2PC
13. **参考架构 v3**:四组件蓝图 + D17 补全

### 阶段 4:Reasonix 分析(完整流程)

14. **域发现 v1-v5**:41 域,5 轮 review
    - v2:补 workspacelease/repair/proc/shell 安全
    - v3:goaleval/plancontract 升核心域(产品①③蓝本)
    - v4:独立审查器模式(boundedllm 族)
    - v5:AutoResearch TaskSpec(产品①规格书三合一蓝本)
15. **闭环笔记 7 份**:rq1-rq7(100 设计),每份 2-4 轮 review
    - rq1 autoresearch(13):TaskSpec + 验收算法 + stale/pivot + ResumeFromGoalText
    - rq2 plancontract(12):数据不是 prose + host 分配 ID + NeedsApproval
    - rq3 goaleval+boundedllm(14):独立审查器 + fail-closed + 渐进收缩
    - rq4 context-manager(19):缓存优先 + 7 标题摘要 + CAS 安装
    - rq5 memory(17):subject 冲突 + pinned/relevant + 低权威声明
    - rq6 coordinator(11):双模型 + 分场景降级 + 交接防注入
    - rq7 checkpoint(14):意图先持久化 + InjectFail + 双阶段
16. **参考架构 v2**:100 设计收敛 + D17 4 缺口补全

### 阶段 5:交接

17. **HANDOVER.md**(权威进度)+ **本文件**(详细交接)

---

## 二、方法论运用记录(本 session 怎么执行的)

### 域发现流程(验证有效)

```
Pass 0:读 AGENTS.md/REASONIX.md/SPEC.md(设计文档优先)
→ Pass 1:目录结构扫描 + 包定位
→ 域清单(🔴核心/🟡支撑/🟢扫描)+ 排除清单
→ 多轮 review(每轮打开不同层面)
→ 完备性检查:JD 关键词覆盖测试 + 94 包全核对
```

### 闭环笔记流程(验证有效)

```
假设 → 验证(grep/读源码)→ 结论 + file:line + 产品映射 + 面试弹药
每份多轮 review:验证行号引用/补遗漏/修正错误
```

### 关键方法经验

1. **"深挖被多个域依赖的共享包"**:boundedllm(goaleval+guardian 共同依赖)→ 发现独立审查器模式
2. **"测试即行为契约"**:测试定义的边界(幂等/一致性/容错)比实现更权威
3. **"域的价值随深挖显现"**:autoresearch 初看是历史读取器,深入后发现 TaskSpec 是规格书蓝本
4. **"review 的含金量取决于层面"**:浅层文件(utils)低价值,深层接口(harness/session/types)高价值

---

## 三、关键发现汇总(产品蓝图核心)

### 产品①规格书(两个蓝本)

```
TaskSpec(autoresearch):goal/scope/non_goals/allowed_operations/success_criteria+evidence_ids
PlanContract(plancontract):Objective/Assumptions(Confirm)/Steps(Acceptance/Verification/Risks)
  + host 分配 ID + Verified/Candidate 证据分离 + NeedsApproval 扩张才审批
```

### 产品②执行引擎(缓存优先)

```
ContextManager:唯一阈值 0.85 + 低于阈值零操作 + 固定前缀 + 一代一票
  + 7 标题摘要模板 + CAS 安装 + 压缩即工具 + 写逃逸检测
双层循环(Pi):内层工具/外层队列 + 钩子契约
```

### 产品③验收器(独立审查器)

```
Goaleval+BoundedLLM:四隔离 + 四 outcome + 严格 complete + 证据不可信
  + fail-closed + 多层预算 + 温度 0
验收算法(autoresearch):evidence_ids 计数 + 必选判据检查
```

### 产品④知识库(结论防矛盾)

```
Memory:subject 冲突 + pinned/relevant + Freshness + 低权威声明
  + BM25 + 影子排名 + pinned 预算 + 软删除
Memory+History 双层 + ResumeFromGoalText(交接自动化)
```

### 安全与回滚

```
Fencing Token(Pi)+ MutationBarrier 代号(Reasonix)
Checkpoint:意图先持久化 + InjectFail + Prepare/Commit 双阶段
```

## 三.5 每份笔记的 review 轮次明细

### Pi(7 份,87 设计)

| 笔记 | 行数 | 设计数 | Review 轮次 | 关键发现 |
|------|:--:|:--:|:--:|---------|
| q1 agent-session | 3344 | 13 | 2 | steer/followUp 双队列/自主续跑/压缩 5 重保护/handleRunFailure 兜底 |
| q2 session-manager | 1714 | 17 | 3 | CustomEntry/压缩三段式/分支三操作/延迟落盘/facts 模型 |
| q3 context-compaction | 1360 | 16 | 3 | 7 标题摘要模板的 Pi 版/工具组不可分/usage 双轨 |
| q4 agent-loop | 796+443 | 14 | 4 | 双层循环/钩子契约/截断全失败/事件归约 |
| q5 tools | 5340 | 14 | 3 | AgentTool 契约/Operations 可插拔/OutputAccumulator/工具预设分级 |
| q6 skills | 896 | 13 | 3 | SKILL.md 格式/清单注入/双调用路径/条件注入 |
| q7 distributed | — | 0(6 理论) | 1 | Fencing Token/Lease/Event Sourcing/2PC 恢复 |

### Reasonix(7 份,100 设计)

| 笔记 | 行数 | 设计数 | Review 轮次 | 关键发现 |
|------|:--:|:--:|:--:|---------|
| rq1 autoresearch | 1360 | 13 | 4 | TaskSpec 规格书/验收算法/nextRequiredAction/stale-pivot/ResumeFromGoalText |
| rq2 plancontract | 1434 | 12 | 4 | 数据不是 prose/host ID/NeedsApproval/Render-ProjectTodos 一致性 |
| rq3 goaleval+boundedllm | 586 | 14 | 3 | 独立审查器/四 outcome/fail-closed/渐进收缩/多审查器族 |
| rq4 context-manager | 1559 | 19 | 5 | 缓存优先/7 标题摘要/CAS 安装/压缩即工具/自适应校准 |
| rq5 memory | 7235 | 17 | 4 | subject 冲突/pinned-relevant/低权威声明/影子排名/结论数据模型 |
| rq6 coordinator | 1060 | 11 | 3 | 双模型/分场景降级/交接防注入/写逃逸检测 |
| rq7 checkpoint | 5459 | 14 | 4 | 意图先持久化/InjectFail/双阶段/代号屏障/BlobStore |

### 参考架构 review 轮次

- Pi 参考架构:v1 → v2(源码遗漏 30% 补全)→ v3(D17 5 问,产品设计层 5 缺口)
- Reasonix 参考架构:v1 → v2(D17 4 缺口补全)

---

## 四、教训清单(跨 session 沉淀)

### 用户纠正记录(重要!)

1. **入口点不能用户指定**(初学者)→ D13
2. **问必带推荐**(平台要推荐 Linux)→ D14
3. **产品是主线,面试是副产品** → 总纲重写
4. **4 个项目也是举例,产品是通用的** → 定位修正
5. **不能预设"只挖 16 格"**(会漏)→ 全面挖掘后收敛
6. **前端/桌面/非 Linux 排除,但核心要全面** → 排除三原则
7. **"session 是进程吗"** → session 是数据不是进程,上下文满 ≠ 内存满
8. **"会涉及 Linux 系统编程"** → D10 Linux 基准
9. **"收敛性"** → agent 说完成不可信,review 循环才是可信的(#24)

### 分析教训

1. **参考架构 v1 遗漏率 30%**(Pi)→ 需要"设计数对账"
2. **支撑域和核心域边界动态变化**(goaleval/plancontract 升级)→ 随深挖升级
3. **跨域模式比单域重要**(独立审查器模式)→ 找共享包
4. **D17 落地性检查**:蓝图必须回答数据流/时机/失败循环/大库/自证

---

## 五、待办与下一步

### 立即待办

- [ ] 方法论正式文件(methodology/zh/ 00-13)——D1-D17 落成可执行规范
- [ ] prompt/zh/self-constraint-prompt.md(Agent 版自约束契约)
- [ ] skills/zh/01-快速参考.md

### 分析待办

- [ ] Hermes 分析(记忆/技能/网关域——产品④直接相关)
- [ ] OpenCode 分析(权限/可观测/事件域)
- [ ] 产品总设计蓝图(Pi + Reasonix 合并,含 D17 验证)

### 产品 MVP 待办

- [ ] 对齐模块(6 维盘问 + 三档提问 + 规格书 schema)
- [ ] 书级知识库日志(subject 冲突 + 事件溯源)
- [ ] 验收器(独立审查器 + 验收算法)
- [ ] OpenJDK 验证闭环

---

## 六、未决问题(用户决策)

1. **下一步优先**:Hermes 分析 / OpenCode 分析 / 方法论正式文件 / 产品 MVP?
2. **方法论正式文件**:先写(产品前置规范)还是先分析完 4 个项目?
3. **产品 MVP 起点**:对齐模块 + 知识库日志(推荐)?

---

## 七、接手须知(下个 AI 第一件事)

1. **必读**:HANDOVER.md(权威)→ 本文 → methodology-v2-decisions.md(决策总纲)
2. **流程**:按 HANDOVER.md §五"下一步决策"与用户确认方向
3. **工具**:4 个项目 MCP 索引已建(data-workspace-agents-*);分析新项目前确认索引新鲜
4. **参考**:分析 Hermes/OpenCode 时,对照 Pi/Reasonix 的闭环笔记格式与 review 轮次标准
5. **交接更新**:本 session 结束时,把"当前状态"同步到 HANDOVER.md §四,并写新的 session 文档
