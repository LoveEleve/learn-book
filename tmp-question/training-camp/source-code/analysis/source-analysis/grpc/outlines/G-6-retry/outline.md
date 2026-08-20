# G-6 流控与重试 — 失败时的第二次机会: RetriableStream 的透明重试与对冲

> 前置: [[G-3-客户端]] (ClientCallImpl 内嵌) + [[G-2-服务端]] (UNAVAILABLE/停机触发) | 引出: [[G-4-负载均衡]] (失败换目标) + [[G-5-命名解析]] (重试解析) | 对照: 客户端重试库 (Resilience4j/Spring Retry)
> 🟡 B | 6 KP | [模式: 透明重放 + 并行抢先]
> Pass 2 闭环: q1(双模式) q2(缓冲) q3(决策) q4(对冲) q5(退避) q6(pushback)

**读者处境**: 一次 RPC 返回 UNAVAILABLE, 你没写任何重试代码, 但调用"自动"成功了——这是 service config 里 `retryPolicy` 一行配置的效果。但"自动重试"是最危险的功能: 重放的消息从哪来?重试风暴怎么防?服务端能不能说"别重试了"?

### 1. 双模式 — Retry 串行重放 vs Hedging 并行抢先

场景: `retryPolicy` 和 `hedgingPolicy` 配置能同时开吗?
源码路径:
- **互斥**: `"Should not provide both retryPolicy and hedgingPolicy"` (RetriableStream.java:146-147); `isHedging = hedgingPolicy != null` (L148)
- **commit 赢家协议** (L153-230): 锁内 `winningSubstream != null → return` (L158-160, 只定案一次) → state.committed (L166) → **CommitTask 取消所有非赢家流** `CANCELLED_BECAUSE_COMMITTED` (RetriableStream.java:191, 常量定义 L64) → 取消已调度 retry/hedging future (L173-188)
- postCommit 必须在 callExecutor 跑 (L227-229, "prevent deadlocks between multiple stream transports")
关键设计 (q1): **两种故障策略统一在一个包装器**: Retry = 失败后串行重放; Hedging = 定时并行开新流, 先完成者胜出。commit 是原子的"定案" — 恰好一个结果到达用户。**被放弃的方案: 两套独立实现** — 缓冲/取消/commit 逻辑高度重叠。 [并发: 锁+callExecutor 防死锁]

### 2. 缓冲 — 重试的代价是持有消息

场景: 重试要重放已发消息, 消息存哪?存多少?
源码路径:
- **BufferSizeTracer** (L1394-1410): outboundWireSize 记账 — 注释: "we have to hold the reference of the message longer for retry" (L1407-1410)
- **双层限额** (L1429-1441): `bufferNeeded > perRpcBufferLimit` (L1429, 单 RPC, **默认 1M** ManagedChannelImplBuilder.java:120) / `channelBufferUsed.addAndGet > channelBufferLimit` (L1437, 整通道共享, **默认 16M** L119, ChannelBufferMeter L1456+)
- **超限即 commit** (L1442-1446): `bufferLimitExceeded → commit(substream)` — 放弃重试换内存; commit 释放记账 (L168)
关键设计 (q2): **透明性有成本**: 缓冲 = 持有消息引用 (重放用); 双层上限 (单调用防爆炸/全通道防拖垮); 超限策略是"立即定案" — 优雅降级。**被放弃的方案: 无限缓冲 (OOM 风险); 逐消息丢弃 (重放缺消息产生错误语义)**。 [算法: 记账式内存上限]

### 3. 重试决策 — 状态码 ∩ 余量 ∩ 节流 ∩ pushback

场景: 什么失败值得重试?怎么防重试风暴?
源码路径:
- **makeRetryDecision** (L1065-1101): `isRetryableStatusCode = retryPolicy.retryableStatusCodes.contains(...)` (L1071) — **无默认集**: retryableStatusCodes 是 service config 必填 (ServiceConfigUtil.java:185, 且禁止含 OK L186)
- **throttle** (L1075-1079): `onQualifiedFailureThenCheckIsAboveThreshold` — 限流器消费重试机会 (防重试风暴)
- **attempt 余量** (L1080): `maxAttempts > previousAttemptCount + 1`
- **pushback 三态** (L1081-1091): null → 按状态码; `>= 0` → 用服务端延迟 (L1089-1091); `< 0` → 不重试
- **transparent retry** (L251, L940-958): **连接中途失败 (MISCARRIED) 透明重试不消耗 attempt** — 本地透明重试无限次但 **1000 次上限防护** ("Too many transparent retries. Might be a bug in gRPC", L941-944); **服务端拒绝 (REFUSED) 仅一次** (noMoreTransparentRetry CAS, L951-952)
- Hedging 对称: `isFatal = !nonFatalStatusCodes.contains` (L1101)
关键设计 (q3): 四因素决策: 状态码白名单 ∩ attempt 余量 ∩ 客户端节流 ∩ 服务端 pushback。**被放弃的方案: 无脑重试 N 次** — 重试风暴放大故障; throttle 是 gRPC 特有设计 (服务端过载时客户端自发退让)。 [跨域: G-2 UNAVAILABLE 触发]

### 4. 对冲 — 定时开新流, 赢家通吃

场景: hedgingDelayNanos=500ms, 慢请求会怎样?
源码路径:
- **HedgingRunnable** (L463-520): `createSubstream(attempt, false, true)` (L477, 新对冲流) → addActiveHedge (L489) → **hasPotentialHedging** (L811-819: winning==null && attempts<max && !frozen) && throttle → 调度下个 (L512-517)
- 已 commit 的对冲流 → `cancel("Unneeded hedging")` (RetriableStream.java:509)
- **freezeHedging** (L823-835): 节流器冻结不再开
- drain(newSubstream) (L519): 新流立即重放缓冲消息
关键设计 (q4): 对冲赌**延迟**而非**失败**: 500ms 没响应就再发一个, 谁先完成谁赢。**被放弃的方案: 一次性全开 N 流** — 流量放大 N 倍; 定时错开让"慢"才有对冲价值。 [并发: 定时调度+锁]

### 5. 指数退避 — 1.6x + 20% 抖动

场景: 重试间隔怎么算?为什么加随机?
源码路径:
- **默认值** (ExponentialBackoffPolicy.java:38-43): initial=1s (L39) / max=2min (L40) / multiplier=1.6 (L42) / jitter=0.2 (L43)
- **next** (L48-53): `min(c*1.6, 2min) + uniformRandom(-0.2c, +0.2c)` (L50-52) — **细节: 封顶的是无抖动值, 抖动可小幅超出 2min** (返回 current + jitter, harness 实证)
- **调度** (BackoffPolicyRetryScheduler.java:63-64): `syncContext.schedule(retryOperation, delayNanos)` — G-3 串行模型定时
关键设计 (q5): 指数增长 (给恢复时间) + 封顶 (防无限) + **抖动** (防同步重试风暴 — 多客户端同时失败会同步退避, 抖动打散相位)。**被放弃的方案: 固定间隔** (过早/过晚); **纯指数无抖动** (thundering herd)。 [算法: 指数+抖动] [跨域: G-3 syncContext]

### 6. pushback — 服务端否决重试

场景: 服务端能说"别重试了"吗?
源码路径:
- **协议键**: `grpc-retry-pushback-ms` (RetriableStream.java:62, trailer 头)
- **三态** (L1072, L1081-1091): null → 按状态码; `>= 0` → 重试且用服务端延迟 (L1089-1091); `< 0` → 禁止重试
- **throttle 联动** (L1075-1077): 服务端拒绝计入限流失败
- hedging 版 (L434, L990): pushbackHedging 推迟对冲
关键设计 (q6): **服务端是重试节奏的最终权威**: 状态码无法表达"可重试但要等 X 毫秒"的精细控制, pushback 让服务端按自身负载指定。与客户端 throttle (自发限流) 双向防风暴。**被放弃的方案: 只靠状态码集合** — 粒度不足。 [协议: trailer 元数据通道]

### 核心悬念

"重试是客户端的事, 但服务端有否决权 — 那负载均衡器 (G-4) 怎么在重试之间换目标?解析器 (G-5) 失败也会退避重试?" 下一域 [[G-4-负载均衡]] (失败换 Subchannel) + [[G-5-命名解析]] (RetryingNameResolver 复用本域退避)。

### 负面空间 (不做)

1. 不写 service config JSON 解析细节 (JsonUtil, 支撑面)
2. 不写 throttle 算法全细节 (只讲语义)
3. 不写 ClientStreamTracer 缓冲的具体内存结构 (只讲记账语义)
4. 不写 RetryPolicy 配置校验规则穷举
5. 不写 HTTP/2 流取消细节 (G-2/G-3 已覆盖)
6. 不写客户端幂等语义讨论 (业务层话题)
