# 闭环笔记 Q3 — PickFirst 两代: 单 Subchannel → 每地址一 Subchannel + Happy Eyeballs

假设: 1.83 的 PickFirst 演进: 旧版整列表一个 Subchannel, 新版每地址独立 Subchannel + RFC 6555 Happy Eyeballs 并行连接。

验证过程:
- **旧版 PickFirstLoadBalancer** (PickFirstLoadBalancer.java:40): 单 `Subchannel subchannel` (L43) — 整个地址列表一个等效组; **shuffle 配置** (L60-67, "shuffle the address list. This can help better distribute the load", weightedShuffling 标志); 空地址 → UNAVAILABLE (L54-58); refreshNameResolution (L113)
- **新版 PickFirstLeafLoadBalancer** (PickFirstLeafLoadBalancer.java:60): **每地址一 Subchannel** — `Map<SocketAddress, SubchannelData> subchannels` (L69) + **地址 Index** (L70)
- **Happy Eyeballs** (L64-65, 112-118): `enableHappyEyeballs = !isSerializingRetries() && PickFirstLoadBalancerProvider.isEnabledHappyEyeballs()` — RFC 6555 风格: 当前地址尝试失败后 **250ms 间隔** (CONNECTION_DELAY_INTERVAL_MS, L63) 启动下一地址连接
- **requestConnection 逐地址** (L508-520): `addressIndex.getCurrentAddress()` → 无 Subchannel 则建 → 按状态走 (READY/TRANSIENT_FAILURE 分支)
- **退避重连** (L78-79): ExponentialBackoffPolicy.Provider (G-6 复用)
- 测试: pickAfterResolved_shuffle (PickFirstLoadBalancerTest.java:163)

代码类型: Implementation (选址策略)

结论: 两代差异 = **连接粒度**: 旧版 "一个 Subchannel 连地址列表" (连接失败整个列表失效); 新版 "每地址独立 Subchannel + Index 顺序尝试 + 250ms 并行启动" (Happy Eyeballs — 一个地址失败不阻塞下一个)。**被放弃的方案 (旧版): 单连接全列表** — 首个地址不可达时重连代价高; Happy Eyeballs 把"串行等待"变"并行试探", 与 G-6 的 hedging 思想同源。 [跨域: G-6 退避复用/hedging 同思想; G-3 syncContext 创建约束] [网络: RFC 6555] (PickFirstLoadBalancer.java:40-67; PickFirstLeafLoadBalancer.java:63-70,508-520)
