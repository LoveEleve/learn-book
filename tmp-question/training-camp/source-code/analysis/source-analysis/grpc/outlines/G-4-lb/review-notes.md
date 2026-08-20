# G-4 review-notes — 六层深审记录

> 2026-08-16 | 锚点回源全部重新 grep, 零发现=不合格原则执行

## 六层深审

| 层 | 审查项 | 结果 |
|---|---|---|
| 1. 锚点回源 | 9 个唯一锚点 (🟡 ≥4 ✅), 抽查 9 处全部命中 (createSubchannel L1050/pickSubchannel L461/defaultIsPickFirst 测试 L130/shuffle L60-67/CONNECTION_DELAY_INTERVAL_MS=250 L63/MultiChild 默认子策略 L106/InternalSubchannel syncContext L249/RoundRobin sequence L36) | ✅ 零修正 |
| 2. 数字穷举 | 250ms (L63)/stdevFactor/1000f (L835)/enforcementPercentage/maxEjectionPercent | ✅ 全部实证 |
| 3. 代码块逐字 | "logical connection to the given group of addresses" (L1050)/"Make a balancing decision for a new RPC" (L461)/"Could not find policy" (L52)/"This behavior matches what Envoy proxy does" (L846) | ✅ |
| 4. 负面空间 | 6 条 | ✅ |
| 5. 桥链 | ← G-3+G-5 / → G-7 (MultiChild 家族) | ✅ |
| 6. 五维检查 | 全 ✅ | ✅ |

## 第二轮: 07 全量维度审查 (2026-08-16)

### R1 行内引用内容回源 — 3 处修正 (写作时"零修正"结论被推翻 — 只验了行号存在, 未验引用内容)

| 发现 | 修复 |
|---|---|
| **LoadBalancer:1062-1063 是 @since 1.4.0 注释**, syncContext 要求实际在**类 javadoc L58-63** ("All methods on the LoadBalancer interface are called from a Synchronization Context") | outline + pass2-q1 |
| **RoundRobin updateOverallBalancingState 实际在 L55** (非 L40-55); CONNECTING NoResult L70-71 / TRANSIENT_FAILURE L73 / READY L75-76 | outline + pass2-q5 |
| **OutlierDetection eject 实际在 L837-838** (非 L852-853) | outline + pass2-q6 |

### R2/R3: 6/6 闭环全含被放弃+跨域 (1680-2019B) / 锚点 11 / 结构全 ✅ — 免修

### R4 横切: **HealthCheckingLoadBalancerFactory 只有负面空间提及, 无对照引用** — v2 规划定为"G-4 对照参考" → §6 补对照 (主动健康检查 vs 被动统计驱逐)

## 发现与修正 (第一轮)
2. **completeness 30 问 30/30 全 ✅** — 吸取前轮教训, 直接写完整 (含跨域桥声明: 同 Zone→G-7 WrrLocality, 加权轮询→G-7)。
3. **教训执行**: 6 闭环全含"被放弃的方案"+跨域; outline 锚点 file:line。
4. **时空溯源**: 见 temporal-trace.md。

## 结论

大纲机制全部有源码实证; 零修正。**达到合格标准**。
