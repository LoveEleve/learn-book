# Reasonix 域发现 v31 补充(续扫第十七轮:agentpreset 预设矩阵)— 2026-08-14

> 承接:v30。本轮:internal/agentpreset/(policy.go/preset.go)——TaskPolicy 的基础定义,之前仅引用未打开。
> 结论:**PolicyOf 三预设完整矩阵确认**——"预设 → 策略"的确定性映射,产品①对齐模块"画像 → 契约"的直接参考。

---

## 一、v31 深化确认(agentpreset)

| 设计 | 位置 | 要点 |
|------|------|------|
| **PolicyOf 三预设矩阵** | policy.go:106-200 | **Light**(目标验证 VerifyTargeted+高险强制安全审查+仅代理+允许部分无检查)/**Delivery**(全验证 VerifyFull+原子契约 RequireAtomicContract+中险即强制审查+不允许部分无检查)/**Balanced**(中间档:中险条件审查+允许部分无检查) |
| **PresetPolicy 七件套** | :43-105 | PlannerPolicy(DirectOK/PreferLightPlan/FullPlanOnRisk/RequireAtomicContract/SemanticRouter)/CapabilityPolicy(DeterministicFirst/SemanticRouterAllowed/PreferProxy)/VerificationPolicy(Level/DiffReview/AllowPartialWithoutChecks)/ReviewPolicy(四风险档) |
| **ReviewForRisk** | :200+ | risk(0/1/2)→ review 等级;越界钳制到 high |
| **preset 兼容** | preset.go | Normalize(未知→Balanced)/LegacyTokenMode(旧模式↔预设往返)/PolicyVersion=1/**TestPolicyOfIsStablePerPreset(策略稳定性测试)** |

---

## 二、关键设计(通用价值)

1. **"预设 = 策略的组合模板"**:三预设编码了完整策略(规划/验证/审查/能力),不再散落配置——**画像化配置**(产品①对齐模块"学习者画像 → 深度分层"可直接映射)
2. **"确定性映射"**:PolicyOf 是纯函数(preset → 策略)——**配置的确定性推导**(TaskPolicy.Derive 的基础)
3. **"策略稳定性测试"**:preset 策略有稳定性测试(不随版本漂移)——**契约锁定**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v30 | — | 102 | 102 |
| v31 | agentpreset 预设矩阵 | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 event.go 全貌(Kind 全集)/tool.go 契约/permission(956)。
