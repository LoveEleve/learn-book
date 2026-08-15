# Reasonix 域发现 v8 补充(第三轮深扫:验收报告/完成判定层)— 2026-08-14

> 承接:v7。v8 深挖 taskpolicy 的 Constraints/Review 细节 + completion(Claim/Report)完成判定层 + command/ablation。
> 结论:**completion 包是产品③验收器的"证据 vs 声明"终极模型**(GapKind 8 分类)——比 rq3 goaleval 更细。

---

## 一、v8 新增域

### 🔴 高价值新增(2 个 — 产品③验收器最细粒度蓝本)

| # | 域 | 文件 | 体量 | 设计要点 | 产品映射 |
|---|----|------|:--:|---------|---------|
| 68 | **完成报告 CompletionReport** | internal/completion/report.go(336) | 336 | **主机完成记录**:Verdict 四态(Unknown/Incomplete/**Partial 终端态**/Done);Criterion(ID/Text/Required/Status/Proofs);Change(Path/**Reviewed**——变更后是否被检查);Verification(Command/Passed/**Stale**——过期证明不了当前树);**GapKind 8 分类**(UnbackedClaim 最重/UnprovenCriterion/MissingCheck/FailedVerification/StaleVerification/UnverifiedChange/UnreviewedChange/DeclaredUnverified);Claimed/Risks 模型声明**永不清除主机发现的 gap** | ③验收器最细粒度 |
| 69 | **完成声明分离 Claim** | internal/completion/claim.go(150) | 150 | **模型自己的工作叙述**(Verified/Unverified/Risks)——update_goal 传入;**唯一模型-authored 的报告部分**;Verified 对照账本检查,Unverified/Risks 是主机无法验证但无理由压制的声明;LatestCompleteClaim 只取最新成功 complete 的账 | ③验收器(证据 vs 声明) |

### 🟡 中价值新增(4 个)

| # | 域 | 文件 | 体量 | 设计要点 |
|---|----|------|:--:|---------|
| 70 | **TaskPolicy 约束面** | internal/taskpolicy/policy.go:60-78 + Input | — | Constraints:ForbidMutation/ForbidTests/AllowedChecks/**ForbidExternal**/RequireFullVerification/PlanModeReadOnly;**Input 引号/围栏剥离**(引号内约束短语不能绑定 host——防约束注入);RaiseRisk 只升不降并重评 review/verification 下限;RequiresIndependentReview/SecurityReview;AllowsMutation/AllowsTests |
| 71 | **命令系统** | internal/command/(command 218/inspect 157/slashtool 133) | 508 | 命令解析/检查/斜杠工具桥 |
| 72 | **完成报告 vs 评审** | agentpreset ReviewLevel(ReviewNone/Conditional/Forced/ForcedSecurity) | — | 独立评审四档;ForbidMutation 时评审降 None(只读回合无需独立评审) |
| 73 | **消融基准** | internal/ablation/ablation.go(117)+ benchmarks | — | 子系统边际效用对比(类级:bugfix/codegen/exploration) |

---

## 二、产品③验收器的"证据 vs 声明"完整模型(三项目归并)

```
模型声明(不可信):
  Claim(Verified/Unverified/Risks)—— 主机只能对照账本验证 Verified

主机证据(可信):
  Ledger 收据(failed 保留但绝不匹配成功断言)
  ReceiptProgressSummary(失败与读不计进度)

报告合成(拒绝呈现为已验证):
  GapKind 8 分类:
    GapUnbackedClaim      — 回合断言了账本不支持的验证(最重)
    GapUnprovenCriterion  — 标准无证据
    GapMissingCheck       — 缺失检查
    GapFailedVerification — 验证失败
    GapStaleVerification  — 证据过期(在最新变异前,证明不了当前树)
    GapUnverifiedChange   — 变更未验证
    GapUnreviewedChange   — 变更后未检查
    GapDeclaredUnverified — 声明未验证
  Verdict:Partial = 终端态(工作被证明符合标准,剩余缺口是声明的不是隐藏的)
```

**跨项目印证**:
- Pi reducer:12 种损坏原因(重放损坏检测)
- Hermes verification_evidence:kind(lint/typecheck/build/test)+ scope(full/targeted)
- Reasonix completion:8 种 gap + Verdict 四态 + Claim 分离

**结论:产品③验收器 = 账本收据(可信)+ 模型声明(分离)+ Gap 分类(拒绝呈现)+ Verdict 终端态(不隐藏)**——这是三项目的共同答案。

---

## 三、TaskPolicy 的"执行契约"完整字段(产品②)

```
TaskPolicy(Derive 零模型调用,第一次模型请求前冻结):
├── Preset(frozen 角色)
├── Intent(任务意图分类,纯启发式)
├── Risk(只升不降;PersistentAction 抬≥Medium;SecurityClass 抬 High)
├── Route(Direct/LightPlan/FullPlan)
├── Constraints:
│   ├── ForbidMutation(禁写)
│   ├── ForbidTests(禁验证命令,gap 记 Partial)
│   ├── AllowedChecks(验证白名单)
│   ├── ForbidExternal(禁推送/发布)
│   ├── RequireFullVerification(强制全验证)
│   └── PlanModeReadOnly(计划模式只读边界)
├── Verification(Full/Targeted/None + 风险抬升自动升级)
├── Review(None/Conditional/Forced/ForcedSecurity;只读降 None)
└── RequiresIndependentReview()/AllowsMutation()/AllowsTests()
```

**与产品①规格书映射**:规格书(静态章节契约)→ TaskPolicy(回合运行时契约)。**"引号内约束不能绑定 host" = 防注入的执行契约原则**。

---

## 四、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v5 | 原始 | 41 | 41 |
| v6 | 体量排序复测 | +12 | 53 |
| v7 | 策略/契约/存储层 | +14 | 67 |
| v8 | 验收报告/完成判定层 | +6 | **73** |

> 剩余:boot/bot 细节、desktop 前端、workers——按产品价值到边际。可收敛。
