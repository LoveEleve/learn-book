# Reasonix 域发现 v40 补充(续扫第二十六轮:goaleval/plancontract 源码验证)— 2026-08-14

> 承接:v39。本轮:internal/goaleval/evaluator.go + internal/plancontract/(plan/diff/order 族)——rq2/rq3 覆盖后的源码级确认。
> 结论:rq2/rq3 正确,无新设计遗漏。

---

## 一、v40 验证确认

### goaleval(rq3 验证)

| 设计 | 位置 | 验证 |
|------|------|------|
| Evaluator 接口/Verdict/GoalEvidence | evaluator.go:72-113 | ✅ |
| Evaluate(buildEvidence → LLM → parseVerdict) | :140-228 | ✅ |
| **fail-closed 测试** | evaluator_test.go:79 | ✅(TestEvaluateFailClosedOnBadResponses——坏响应 fail-closed) |

### plancontract(rq2 验证)

| 设计 | 位置 | 验证 |
|------|------|------|
| Plan/Assumption/Step/Criterion/Verification | plan.go:17-67 | ✅ |
| **Normalize 分配 ID** | :69-177 | ✅(assignStepIDs/assignCriterionIDs/repairParents/repairDependencies) |
| Validate | :208 | ✅ |
| **Diff.NeedsApproval 扩张才审批** | diff.go:88 | ✅(grew 检测:步骤数/标准数增长才需审批) |
| **Ordered 拓扑排序** | order.go:7-148 | ✅(siblingGroups/sortSiblings/cyclic 检测) |
| RenderDiff | diff.go:113 | ✅ |

---

## 二、关键设计(通用价值)

1. **"Normalize 修复父子/依赖"**:分配 ID + 修复父链 + 修复依赖——**计划数据的规范化**(数据不是 prose 的工程化)
2. **"NeedsApproval = 增长才审批"**:步骤/标准数量增长 → 审批;修改不审批——**审批的触发条件精确定义**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v39 | — | 102 | 102 |
| v40 | goaleval/plancontract 验证 | +0(rq2/rq3 源码级验证) | **102**(验证) |

> 继续:next 轮 cli 剩余(setup_manager/mcp/upgrade)、autoresearch/store——按需。
