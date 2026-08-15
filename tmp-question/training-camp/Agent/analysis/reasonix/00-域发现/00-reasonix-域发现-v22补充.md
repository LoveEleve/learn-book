# Reasonix 域发现 v22 补充(续扫第八轮:billing 报价/shellsafe 效果分类)— 2026-08-14

> 承接:v21。本轮:internal/billing/quote.go(720)+ internal/shellsafe/effect.go(721)。
> 结论:**shellsafe 是"命令效果静态分类"(Certainty + WriteDomain)——命令安全的静态分析层**,深化 ②;无新域。

---

## 一、v22 深化确认

### shellsafe(命令效果静态分类)

| 设计 | 位置 | 要点 |
|------|------|------|
| **Certainty 二分** | effect.go:10-15 | EffectUnknown/EffectKnown——**无法静态证明 → fail closed**(变异/权限边界) |
| **WriteDomain 位掩码** | :18-25 | WriteWorkspaceContent/WriteRepositoryMetadata/WriteHostState/WriteExternalState——**4 类持久状态可写域** |
| **CommandEffect** | :27-39 | **参数无关的 CommandFamily/Reason**(安全可表面化——不带参数细节)/PermissionSafe/ExecutesCode/UsesNetwork |
| **AnyMutation** | :41 | Certainty!=Known OR Writes!=0——**变异判定** |

### billing(报价)

| 设计 | 要点 |
|------|------|
| **CostQuote 全模型** | DisplayRequest(货币规范化)/Valuation/RateSnapshot/RateCard/UsageTokens/QuoteInput/BuildQuote;PricingFingerprint(费率指纹) |

---

## 二、关键设计(通用价值)

1. **"静态证明不能 → fail closed"**:命令效果无法静态分类 → 按变异处理(安全默认)——**与 mutationBarrier/taskpolicy fail-closed 同族**
2. **"参数无关的分类"**:CommandFamily/Reason 不携带参数细节——**分类可安全表面化**(防泄露命令内容)
3. **"可写域位掩码"**:4 类持久状态的可写域——**效果分域的精确建模**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v21 | — | 102 | 102 |
| v22 | billing/shellsafe | +0(深化 2 设计) | **102**(深化) |

> 继续:next 轮 extension 剩余(protocol/validate 419/publish 417/runtimeplan 372)、pluginpkg(manifest 校验)、serve/auth。
