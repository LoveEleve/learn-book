# G-6 temporal-trace — 时空溯源 (🟡 域, 简版)

> 浅克隆无 git 历史 — 用代码内注释/常量痕迹。

## 演进痕迹

| 痕迹 | 证据 | 意义 |
|---|---|---|
| RetriableStream 统一实现 | retryPolicy + hedgingPolicy 同构 (RetriableStream.java:83-85) | **Retry (1.0 时代) 与 Hedging (后加) 合并演进** — hedging 是 retry 包装器的扩展模式 |
| TODO(b/145386688) | L163/485 "this.lock==ScheduledCancellor.lock so ok" | 锁合并的已知实现债 |
| pushback 协议 | grpc-retry-pushback-ms (L62) | 服务端控制演进 (gRFC A32 系列) |
| throttle | onQualifiedFailureThenCheckIsAboveThreshold | 客户端限流 (重试风暴防护演进) |
| ManagedChannelImplBuilder 缓冲配置 | retryBufferSize/perRpcBufferLimit (L551-560) | 缓冲内存治理演进 |

## 写书建议

呈现"失败处理的三层演进": ① Retry (串行重放) ② Hedging (并行抢先, 后加) ③ 治理面 (throttle/pushback/缓冲限额, 防重试风暴) — 体现"能力先加, 治理后补"的演化。
