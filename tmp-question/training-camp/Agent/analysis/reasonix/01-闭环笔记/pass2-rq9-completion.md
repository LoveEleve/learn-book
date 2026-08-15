# rq9 Completion 验收模型(Claim + Report + GapKind)— 产品③验收器终极蓝本

> 项目:Reasonix(internal/completion/claim.go + report.go + taskcontract.go)
> 假设:completion 包是"主机证据 vs 模型声明"分离的最完整实现,GapKind 8 分类是验收器"拒绝呈现为已验证"的完整分类学。
> 结论:✅ 成立——这是产品③验收器的数据模型定论(比 rq3 goaleval 更细,含"过期证据""未检查变更"等 Pi/Hermes 都没有的维度)。

---

## 一、核心哲学:模型声明与主机证据分离

```
模型说"我完成了" → Claim(不可信,仅叙述)
主机证明"确实完成" → Ledger 收据(可信)
报告合成 → 模型声明永不清除主机发现的 gap
```

**Claim(claim.go:1-30)**:唯一模型-authored 的报告部分,只能增加主机已发现的:
- `Verified` — 对照账本检查
- `Unverified` / `Risks` — 主机无法验证但无理由压制的声明

**关键**:LatestCompleteClaim 只取**最新成功 complete 的账**;continue/blocked 报告和失败调用不claim 任何东西。

---

## 二、设计 1:Verdict 四态(Partial 是终端态)

**位置**:`report.go:1-33`

```
Unknown     — 未知
Incomplete  — 未完成
Partial     — 终端态:工作被证明符合标准,剩余缺口是声明的不是隐藏的
Done        — 完成
```

**Partial 的哲学**:承认"有缺口但缺口透明"优于"隐藏缺口宣称完成"——与 #24 收敛性(完成不可信)直接呼应:**"完成"报告必须列出所有 gap 才算数**。

## 设计 2:GapKind 8 分类(拒绝呈现为已验证)

**位置**:`report.go:47-80`

| # | Gap | 含义 |
|---|-----|------|
| 1 | **GapUnbackedClaim** | 回合断言了账本不支持的验证(**最重**——撒谎) |
| 2 | GapUnprovenCriterion | 标准无证据 |
| 3 | GapMissingCheck | 缺失检查 |
| 4 | GapFailedVerification | 验证失败 |
| 5 | **GapStaleVerification** | 证据过期(在最新变异前运行,证明不了当前树) |
| 6 | GapUnverifiedChange | 变更未验证 |
| 7 | **GapUnreviewedChange** | 变更后未被检查 |
| 8 | GapDeclaredUnverified | 声明未验证 |

**Stale 语义**(与 taskcontract.Status.Stale 呼应):"它曾经为真,必须对照当前代码重新证明"——**证据有保质期,树变了证据就失效**。

## 设计 3:Report 数据结构(验收器的完整输出)

**位置**:`report.go:80-120`

```
Report:
├── Verdict / Risk
├── Mutations(成功变异计数,含未命名路径)
├── Criteria[](ID/Text/Required/Status/Proofs)
├── Changes[](Path/Reviewed — 变更后是否被检查)
├── Verifications[](Command/Passed/Stale)
├── Gaps[](Kind/Detail)
├── Claimed(模型叙述)+ Risks(声明风险)—— 永不清除主机 gap
```

**Criterion.Required**:可选标准的存在——"必选 vs 可选验收标准"的区分。

---

## 三、与 TaskContract 的关系(单一汇聚点)

**taskcontract.go:1-40**:
```
TaskContract = taskintent 分类 + planner-gate 特征 + 计划验收标准 + 证据账本收据
- 构建零模型调用(纯信号汇聚)
- 所有终止仲裁者读同一记录(不各自维护)
- Status: Pending/Satisfied/Failed/Stale(证据早于最新变异)
```

**Completing 层 = TaskContract 的消费端**:report 读 contract 的 criteria/risk,合成 Gap。

---

## 四、跨项目印证(产品③架构定论)

| 维度 | Pi | Hermes | Reasonix |
|------|----|--------|----------|
| 损坏检测 | reducer 12 种 | — | — |
| 证据分类 | — | verification_evidence kind/scope | **GapKind 8 分类** |
| 完成判定 | — | run_verify ok/partial | **Verdict 四态(Partial 终端)** |
| 模型声明 | — | — | **Claim 分离(永不清除 gap)** |
| 过期证据 | — | stale-kill 超时 | **GapStaleVerification(树变异后失效)** |
| 单一汇聚 | — | verify_cmd→账本 | **TaskContract(零模型构建)** |

**结论**:产品③自动验收器 = 
```
验收器(独立审查器,Reasonix goaleval/Hermes verify/Pi conformance)
+ 账本收据(Hermes verification_evidence/Reasonix Ledger)
+ Gap 分类(Reasonix 8 类 — 拒绝呈现为已验证)
+ Verdict 终端态(Reasonix Partial — 不隐藏缺口)
+ 证据保鲜(Stale — 树变了证据失效)
+ 模型声明分离(Claim — 永不清除主机发现)
```

---

## 五、面试弹药

1. **"GapUnbackedClaim 是最重的 gap"**:回合断言了账本不支持的验证——验收器必须把"撒谎"列为最高级失败
2. **"Stale 验证证明不了当前树"**:证据有保质期,变更后旧验证失效——验收必须对照最新状态
3. **"Partial 是终端态"**:承认缺口且透明优于隐藏缺口宣称完成——与 #24"完成不可信"直接呼应
4. **"模型声明永不清除主机发现"**:Claim 只能增加,不能清除——声明与证据的权威分离
5. **"TaskContract 零模型构建"**:所有终止仲裁者读同一记录,不各自维护——单一汇聚点防漂移
6. **"引号内约束不能绑定 host"**:TaskPolicy 的防约束注入——规格书的引号/围栏内容不算数

---

## 六、产品映射汇总

| 设计 | 产品③(自动验收器)用法 |
|------|----------------------|
| Claim 分离 | 章节作者声明(只叙述,不验证) |
| GapKind 8 类 | 章节验收失败分类(撒谎/过期/未检查…) |
| Verdict 四态 | 章节验收结果(Partial 终端态不隐藏) |
| Stale 证据 | 源码版本变化后旧证据失效 |
| TaskContract 单一汇聚 | 规格书+证据+特征单一记录 |
| 证据账本 | 验收收据(失败保留但绝不匹配成功) |

> 覆盖设计数:6(设计 1-3 + 2 子设计)
