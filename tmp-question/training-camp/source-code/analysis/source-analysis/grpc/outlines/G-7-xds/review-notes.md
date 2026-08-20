# G-7 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep (含引用内容), 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 10 处引用**内容**逐一验证 | **10/10 命中**: watchXdsResource L251-252/typeUrl L70/全量-增量注释 L76-77/version Map L74-77/Ketama 注释 L60-64 逐字/虚拟节点 L339-341/WRR 配置 L70-75/累积扫描注释 L121-124 逐字 ("Not using Arrays.binarySearch for better readability")/CdsLoadBalancer2 L77-81/WrrLocality 属性 L77-78。**1 处遗漏补强**: WRR 配置面缺 `blackoutPeriod: 10s` (L71) — 已补 |
| 2. 数字穷举 | blackoutPeriod 10s/oobReportingPeriod 10s/weightExpirationPeriod 180s/weightUpdatePeriod 1s/errorUtilizationPenalty 1.0/scale 虚拟节点/1/N 影响 | ✅ 全部实证 |
| 3. 代码块逐字 | Ketama 注释 (L60-64)/"Not using Arrays.binarySearch" (L123)/"One instance per top-level cluster" (L77) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← G-3+G-4+G-5 / → G-8 (数据面对照) | ✅ |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16)

### R1 行内引用内容回源 — 2 处修正 + 1 处重大遗漏

| 发现 | 修复 |
|---|---|
| **ControlPlaneClient:89-90 是类注释/构造器**, 退避字段实际在 **L84** (retryBackoffPolicy) + 使用处 L480-483 | outline + pass2-q1 |
| **RingHash:337 是 while 循环**, "Per GRFC A61 use the first address" 实际在 **L331** | outline + pass2-q3 |
| **⚠️ XdsNameResolver (1206 行) 大纲零覆盖** — Pass 2 时 Q2 被跳过 (7 问只闭环 6 个) | **补 pass2-q2 闭环 + outline §1 入口段** (控制面解析器/ConfigSelector/RPC_HASH_KEY/RLS 插件挂载) |

其余 18 处引用 (WrrLocality:68-80/ClusterManager:126-127/ClusterImpl:99,144/Cds:120-125/RingHash:425-435,443-447/WRR:381/WeightedRandomPicker:116-119/XdsClientImpl:98) 全部命中。

### R2/R3: 闭环 6 个 (补 q2 后) 全含被放弃+跨域 / 桥 ←G-3+G-4+G-5 →G-8 — 免修

### R4 横切: 退避/重连/syncContext/MultiChild 8 处覆盖 ✅ — 免修

## 发现与修正 (第一轮)

1. **blackoutPeriod 遗漏**: WRR 配置面 (L70-75) 共 6 个参数, 大纲原写 5 个 — 补 `blackoutPeriod: 10s` (L71, 启动后黑窗期不采负载)。
2. **completeness 32 问**: 3 处 ⚠️ (Ketama vs Maglev/Dubbo 容错/k8s topologyKeys) 声明为写作时展开的对照讨论 — 无需补大纲。
3. **教训执行**: 闭环 6 个全含被放弃方案+跨域; 锚点 file:line; 引用内容写作时逐字验证 (blackoutPeriod 是唯一遗漏)。
4. **时空溯源**: 见 temporal-trace.md。

## 结论

大纲机制全部有源码实证; 1 处配置参数补强; 无机制性错误。**达到合格标准**。
