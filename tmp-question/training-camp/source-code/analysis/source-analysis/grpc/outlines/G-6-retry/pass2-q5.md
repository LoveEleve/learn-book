# 闭环笔记 Q5 — 指数退避: 1.6x + 20% 抖动, 1s→2min 收敛

假设: 退避是"指数增长 + 均匀抖动" — 默认 1s 起步, 1.6x 增长, 2min 封顶, ±20% 抖动防同步重试。

验证过程:
- **参数默认值** (ExponentialBackoffPolicy.java:38-43): `initialBackoffNanos = 1s` (L39) / `maxBackoffNanos = 2min` (L40) / `multiplier = 1.6` (L42) / `jitter = .2` (L43)
- **nextBackoffNanos** (L48-53): `next = min(current * 1.6, 2min)` (L50) → 返回 `current + uniformRandom(-0.2*current, +0.2*current)` (L51-52) — **先指数后加抖动**
- **调度** (BackoffPolicyRetryScheduler.java:63-64): `delayNanos = policy.nextBackoffNanos()` → `syncContext.schedule(retryOperation, delayNanos, ...)` — 重试任务进 syncContext 定时调度 (G-3 q1 串行模型)
- 测试: ExponentialBackoffPolicyTest/BackoffPolicyRetrySchedulerTest 存在
- RetriableStream 消费: `intervalWithJitter(nextBackoffIntervalNanos)` (RetriableStream.java:1084) + `next *= backoffMultiplier` (L1085-1086, 策略值来自 RetryPolicy)

代码类型: Algorithmic (退避算法)

结论: 退避三要素: **指数增长** (放大间隔) + **封顶** (2min 防无限) + **抖动** (±20% 防同步重试风暴 — 多个客户端同时失败会同步退避, 抖动打散相位)。**被放弃的方案: 固定间隔重试** — 服务器恢复需要时间, 固定间隔过早或过晚; 纯指数无抖动 — 客户端同步化 (thundering herd)。 [跨域: G-3 syncContext 调度; TimeProvider 时钟抽象供测试] [算法: 指数+抖动] (ExponentialBackoffPolicy.java:38-53; BackoffPolicyRetryScheduler.java:63-64)
