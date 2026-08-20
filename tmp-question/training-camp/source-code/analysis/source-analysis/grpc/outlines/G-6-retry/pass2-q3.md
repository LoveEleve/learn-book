# 闭环笔记 Q3 — 重试决策: 状态码 + throttle + pushback 三因素

假设: 是否重试由三因素决定: 状态码可重试性、客户端 throttle 限流、服务端 pushback 信号。

验证过程:
- **makeRetryDecision** (RetriableStream.java:1065-1090): retryPolicy null → 不重试 (L1066-1068); `isRetryableStatusCode = retryPolicy.retryableStatusCodes.contains(status.getCode())` (L1071)
- **throttle 限流** (L1075-1079): `isThrottled = !throttle.onQualifiedFailureThenCheckIsAboveThreshold()` — 请求超限时**消费重试机会** (服务端在被压垮时客户端主动降低重试频率, 防止重试风暴)
- **attempt 上限** (L1080): `retryPolicy.maxAttempts > substream.previousAttemptCount + 1` — 已用次数 + 1 次 (本次)
- **pushback 三态** (L1082-1090): 无 pushback → 按状态码; `pushbackMillis >= 0` → 重试但用服务端指定延迟; `< 0` → 不重试 (服务端拒绝)
- **退避**: `intervalWithJitter(nextBackoffIntervalNanos)` + `nextBackoffIntervalNanos = min(next * backoffMultiplier, maxBackoffNanos)` (L1084-1086)
- **Hedging 判定对称**: `isFatal = !hedgingPolicy.nonFatalStatusCodes.contains(...)` (L1101) — hedging 用"非致命码"集合 (重试用"可重试码"集合)
- **transparent retry**: createSubstream(isTransparentRetry) (L251) — 连接建立前失败的透明重试 (不消耗 attempt, 测试 L621)

代码类型: Implementation (决策逻辑)

结论: 重试决策 = 状态码 ∩ attempt 余量 ∩ 节流器 ∩ pushback; 三处防护: 状态码白名单 (默认仅部分码)、客户端节流 (重试风暴防护)、服务端 pushback (服务端主动拒绝)。**被放弃的方案: 无脑重试 N 次** — 会放大故障 (重试风暴); throttle 是 gRPC 特有设计 (服务端过载时客户端自发退让)。 [跨域: G-2 UNAVAILABLE 触发 (停机语义); G-5 解析失败重试] [算法: 指数退避+抖动] (RetriableStream.java:1065-1101, L251)
