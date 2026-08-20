# 闭环笔记 Q6 — pushback: 服务端否决重试的协议信号

假设: gRPC 有显式协议让服务端控制客户端重试: grpc-retry-pushback-ms 头 — 正值 = 指定延迟, 负值 = 禁止重试。

验证过程:
- **协议键** (RetriableStream.java:62): `Metadata.Key.of("grpc-retry-pushback-ms", ...)` — trailer 头
- **三态决策** (L1072, L1081-1091): `getPushbackMills(trailer)` (L1072): null → 按状态码判定; `>= 0` → **重试且用服务端延迟** `backoffNanos = TimeUnit.MILLISECONDS.toNanos(pushbackMillis)` (L1089-1091); `< 0` → 不重试 (服务端拒绝)
- **throttle 联动** (L1075-1077): pushback < 0 也计入 `onQualifiedFailure` (限流器把服务端拒绝视为一次失败)
- **hedging 版** (L434, L990): pushbackHedging — 对冲也有 pushback 控制 (推迟下次对冲)

代码类型: Protocol (服务端控制信号)

结论: pushback 是**服务端主动的流量控制**: 比状态码更精确 — 服务端知道自己的负载, 指定"多久后再试"或"别试了"; 与客户端 throttle (客户端自发限流) 形成双向防重试风暴。**被放弃的方案: 只靠状态码集合** — 无法表达"可重试但要等 X 毫秒"的精细控制; pushback 让服务端成为重试节奏的最终权威。 [跨域: G-2 服务端发送侧 (trailer 元数据)] [协议: 元数据通道] (RetriableStream.java:62,434,990,1072-1091)
