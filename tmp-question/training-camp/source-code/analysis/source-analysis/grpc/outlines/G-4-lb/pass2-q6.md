# 闭环笔记 Q6 — OutlierDetection: 成功率统计驱逐 (Envoy 风格)

假设: 离群检测用统计方法 (mean/stdev 阈值 + 概率执行 + 上限百分比) 驱逐异常地址 — 实现源自 Envoy 规范。

验证过程:
- **类结构** (util/OutlierDetectionLoadBalancer.java:74): 定时检测 (L209, ejectOutliers) + 周期 uneject (L212)
- **SuccessRateEjection** (L806-860): 请求量过滤 (requestVolume/minimumHosts, L810-815) → 计算所有地址成功率 → `requiredSuccessRate = mean - stdev * (stdevFactor / 1000f)` (L835) → `tracker.successRate() < requiredSuccessRate → eject` (L837-838)
- **概率执行** (L856-858): `new Random().nextInt(100) < enforcementPercentage` — 不全部执行 (留观察样本)
- **上限** (L842-848): `trackerMap.ejectionPercentage() >= maxEjectionPercent → return` — 注释: "This behavior matches what Envoy proxy does"
- **eject 效果** (L373-378): `subchannel.eject()` — "The subchannel has been ejected by outlier detection" (subchannel 从可选项中移除)
- 第二算法: FailurePercentageEjection (L557, 失败百分比阈值)

代码类型: Algorithmic (统计驱逐)

结论: 离群检测 = **统计哨兵**: 定期算成功率分布, 低于 mean-stdev 阈值的地址被 eject (临时移出轮询); 概率执行 (enforcement) 与上限百分比防误伤/全灭。**被放弃的方案: 固定阈值 (如成功率 < 90% 驱逐)** — 无法适应集群整体健康度波动; 统计阈值自适应。源自 Envoy outlier detection 规范 (gRPC 移植)。 [跨域: G-7 xds 生态同源 (Envoy 规范)] [算法: 均值-标准差] (OutlierDetectionLoadBalancer.java:806-860,373-378)
