# G-6 流控与重试 — 知识规划 (KP)

> 域级: 🟡 B | 模块: core/RetriableStream (1618) + RetryPolicy (96) + HedgingPolicy (72) + ExponentialBackoffPolicy + BackoffPolicyRetryScheduler + JsonUtil
> 日期: 2026-08-16 | 版本: 1.83.1 | Pass 2 闭环: q1(双模式/commit) q2(缓冲记账) q3(重试决策) q4(对冲) q5(退避) q6(pushback)

## 一、机制提取 (逐源)

### M1 双模式与 commit (q1)
- RetriableStream (L55): retryPolicy XOR hedgingPolicy 互斥 (L146-147, "Should not provide both"); isHedging (L148)
- **commit** (L153-230): 锁内 winningSubstream 检查 (L158-160) → state.committed (L166) → 取消 retry/hedging future (L173-188) → CommitTask 取消非赢家流 (L199-202, CANCELLED_BECAUSE_COMMITTED) → postCommit (callExecutor 防死锁 L227-229)
- Substream (L1375): 每次尝试一个子流

### M2 缓冲记账 (q2)
- BufferSizeTracer (L1394-1410): outboundWireSize 记账 — 注释: "hold the reference of the message longer for retry"
- **双层限额** (L1429-1441): perRpcBufferLimit / channelBufferLimit (L92-93, ChannelBufferMeter L1456+ AtomicLong)
- **超限即 commit** (L1442-1446); commit 释放 (L168)

### M3 重试决策 (q3)
- makeRetryDecision (L1065-1101): retryPolicy null → 不重试; isRetryableStatusCode (L1071); **throttle** (L1075-1079, onQualifiedFailureThenCheckIsAboveThreshold); maxAttempts (L1080); **pushback 三态** (L1081-1091)
- 退避推进: intervalWithJitter + next*multiplier (L1084-1086)
- hedging 对称: isFatal = !nonFatalStatusCodes (L1101); transparent retry (L251, 连接建立前不消耗 attempt)

### M4 对冲 (q4)
- HedgingRunnable (L463-520): createSubstream(isHedged) (L477) → addActiveHedge (L489) → **hasPotentialHedging** (L811-819: winning==null && attempts<max && !frozen) → 调度下个 (L512-517, hedgingDelayNanos)
- 取消: "Unneeded hedging" (L499-501); freezeHedging (L823-835, throttle 冻结)
- drain(newSubstream) (L519)

### M5 指数退避 (q5)
- 默认: initial=1s (L39)/max=2min (L40)/multiplier=1.6 (L42)/jitter=0.2 (L43) (ExponentialBackoffPolicy.java)
- next = min(c*1.6, 2min) + uniform(±0.2c) (L48-53)
- BackoffPolicyRetryScheduler: syncContext.schedule (L63-64)

### M6 pushback (q6)
- grpc-retry-pushback-ms (RetriableStream.java:62); 三态 (L1081-1091); throttle 联动 (L1075-1077); hedging 版 pushbackHedging (L434,990)

## 二、聚合分级

| 级别 | 机制 |
|---|---|
| P1 | M1 commit 协议 / M2 缓冲记账 / M3 重试决策 (状态码+throttle+pushback) |
| P2 | M4 对冲定时 / M5 退避 |
| P3 | M6 pushback 协议细节 / transparent retry |

## 三、叙事线

场景: 调用失败 (UNAVAILABLE), 客户端自动重试 — 谁在管?读者疑问链: 重试和冲对是什么关系 (M1) → 重试要存什么 (M2) → 什么时候该重试 (M3) → 对冲怎么并行 (M4) → 等多久 (M5) → 服务端能否否决 (M6)。
