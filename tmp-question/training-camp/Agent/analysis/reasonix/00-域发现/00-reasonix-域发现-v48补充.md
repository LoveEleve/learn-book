# Reasonix 域发现 v48 补充(续扫第三十四轮:recovery rules/provider_presets)— 2026-08-14

> 承接:v47。本轮:internal/recovery/rules.go(1,011)+ config/provider_presets(1,062)。
> 结论:**失败分类的排除清单与四分类确认**——自动守卫的失败语义完整。

---

## 一、v48 深化确认

### recovery rules(1,011)

| 设计 | 位置 | 要点 |
|------|------|------|
| **QualifyingFailure 排除清单** | rules.go:16-38 | success/blocked/userRejected/providerError/cancelled/emptySearch 排除;变异失败/验证非零退出/非只读工具失败 → 合格——**执行可靠性问题不当作权限/用户决策边界** |
| **ClassifyFailure 四分类** | :39-74 | transient(超时标记族:timed out/deadline exceeded/execution timeout)/verification/mutation/execution——**分类器刻意窄**(权限/沙箱/用户块已在 QualifyingFailure 过滤) |
| **风险边界** | :130-164 | 调用者高险无主机证明语义 → **不授可复用任务授权**;MCP 不重复提示(已有策略门);bash 确定性分类(test/build 命令受验证分类器约束,破坏性形式触发 highRisk) |
| **诊断/空搜索分类** | :164-211 | ClassifyEmptySearch/IsDiagnosticSuccess |

### provider_presets(1,062)

| 设计 | 要点 |
|------|------|
| **策展预设** | CuratedProviderPresets/CuratedProviderPreset;qwen/kimi/token 节奏**模型覆盖**(ProviderModelOverride)——**预设带模型特定覆盖** |

---

## 二、关键设计(通用价值)

1. **"执行可靠性 ≠ 权限边界"**:超时/失败不当作权限问题处理——**失败语义的精确分离**(防把瞬态问题当安全事件)
2. **"窄分类器"**:权限/沙箱/用户块先过滤,分类器只管剩余——**分类职责单一**
3. **"无主机证明的高险不授任务授权"**:调用者声明的高险没有语义证明 → 不授可复用授权——**授权的可证明性**

---

## 三、累计覆盖对账

| 轮次 | 层面 | 新增 | 累计 |
|:--:|------|:--:|:--:|
| v1-v47 | — | 102 | 102 |
| v48 | recovery rules/provider_presets | +0(深化 3 设计) | **102**(深化) |

> 继续:next 轮 bot/feishu(1,010)/responses provider(899)/anthropic provider(850)——同构适配器,快速核对。
