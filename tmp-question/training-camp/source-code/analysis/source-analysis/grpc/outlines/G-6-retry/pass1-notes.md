# G-6 Pass 1 扫描笔记 — 流控与重试

> 日期: 2026-08-16 | 版本: 1.83.1 | 🟡 B | 模块: core/RetriableStream (1618) + RetryPolicy (96) + HedgingPolicy (72) + BackoffPolicyRetryScheduler + ExponentialBackoffPolicy (31) + JsonParser/JsonUtil + TimeProvider

## 继承树/调用图

```
ClientCallImpl.sendMessage → RetriableStream (55, abstract, implements ClientStream)
  ├── retryPolicy (83) XOR hedgingPolicy (85) — 互斥 (L146-147)
  ├── Substream (L1375) — 每次尝试一个子流
  ├── BufferManager — 已发消息缓冲 (重放用)
  ├── isRetryableStatusCode (L1071) — 状态码判定
  ├── HedgingRunnable (L463) — 对冲定时器
  ├── scheduledHedging (L122) — 对冲调度
  └── pushback: grpc-retry-pushback-ms (L62) — 服务端拒绝重试信号
RetryPolicy (96): maxAttempts/initialBackoffNanos/maxBackoffNanos/backoffMultiplier/retryableStatusCodes (L33-39)
HedgingPolicy (72): maxAttempts/hedgingDelayNanos/nonFatalStatusCodes (L31-33)
ExponentialBackoffPolicy (31): multiplier=1.6 (L42) + 随机化 (L58)
BackoffPolicyRetryScheduler — 调度重试
JsonUtil — service config JSON 解析 (retryPolicy 来源)
```

## 基本元素分解

1. **RetriableStream**: 透明重试的统一实现 (Retry + Hedging 双模式)
2. **策略对象**: RetryPolicy/HedgingPolicy — 不可变配置 (service config 解析)
3. **退避**: ExponentialBackoffPolicy (1.6x 指数 + 随机抖动)
4. **缓冲**: BufferManager (消息重放) + 调度 (BackoffPolicyRetryScheduler)

## 标记问题 (6)

1. **Q1 双模式互斥**: retryPolicy 和 hedgingPolicy 为什么不能同时给?透明重试的两种语义?
2. **Q2 缓冲与重放**: BufferManager 缓冲到什么程度?哪些状态 (headersRead) 后不再重试?transparent retry 是什么 (测试 L621)?
3. **Q3 状态码判定**: isRetryableStatusCode (L1071) — retryableStatusCodes 默认值?与 nonFatalStatusCodes (hedging) 区别?
4. **Q4 对冲机制**: HedgingRunnable — hedgingDelayNanos 后启动第二个 substream?先完成者胜出 (commit)?
5. **Q5 退避调度**: ExponentialBackoffPolicy 1.6x + 随机;BackoffPolicyRetryScheduler 怎么调度?与 TimeProvider 关系?
6. **Q6 pushback**: grpc-retry-pushback-ms — 服务端拒绝重试的协议信号?

## 已读测试

- RetriableStreamTest (core): retry_everythingDrained (L239); transparentRetry_cancel_race (L621); retry_headersRead_cancel (L455) — headersRead 后不重试
- HedgingPolicyTest (L38): getHedgingPolicies; getRetryPolicies_hedgingDisabled (L100)
- BackoffPolicyRetrySchedulerTest/ExponentialBackoffPolicyTest/RetryPolicyTest 存在
