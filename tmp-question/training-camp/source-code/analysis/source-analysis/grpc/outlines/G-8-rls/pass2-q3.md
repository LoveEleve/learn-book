# 闭环笔记 Q3 — AdaptiveThrottler: 客户端比例节流 (2x 激进系数)

假设: RLS 客户端节流是"自适应比例" — 统计窗口内被节流/总请求比, 目标是把客户端请求率控制在服务端接受率的倍数内。

验证过程:
- **默认参数** (AdaptiveThrottler.java:45-47): `DEFAULT_HISTORY_SECONDS = 30` (L45, 统计窗口) / `DEFAULT_REQUEST_PADDING = 8` (L46, "A magic number to tune the aggressiveness... High numbers throttle less") / `DEFAULT_RATIO_FOR_ACCEPT = 2.0f` (L47, "The ratio by which the Adaptive Throttler will attempt to send requests above what the server is currently accepting")
- **双计数** (L65-71): requestStat (尝试数, 含客户端节流) + throttledStat (被节流数) — TimeBasedAccumulator 30s 滑动窗口
- **shouldThrottle** (L86): 随机采样 + 比例比较 — 节流判定带随机性 (非全有全无, 部分请求放行采样)
- **onRequest/onSuccess/onFailure**: 计数更新钩子 (请求发出/成功/失败分类)
- 测试: shouldThrottle (AdaptiveThrottlerTest.java:43)/negativeTickerValues (L118)

代码类型: Algorithmic (比例节流)

结论: 自适应节流 = **滑动窗口比例控制**: 30s 窗口统计"被节流的请求/总请求", 目标让客户端发送率 ≈ ratioForAccepts (2x) × 服务端接受率; requestsPadding (8) 是"激进系数" (越大越少节流); 判定带随机性防同步。**被放弃的方案: 固定限流 (如 10 req/s)** — 无法适应服务端容量变化; 比例式让节流随服务端健康状况自动缩放。 [跨域: G-6 RetriableStream 的 throttle 同思想 (onQualifiedFailureThenCheckIsAboveThreshold)] [算法: 滑动窗口/比例控制] (AdaptiveThrottler.java:43-90)
