# 闭环笔记 Q6 — 连接生命周期: 双 GOAWAY 优雅关闭 + ping 双向管理

假设: 服务端连接生命周期有两条 ping 线 (服务端主动保活 / 客户端 ping 频率强制) 和一个双 GOAWAY 优雅关闭协议。

验证过程:
- **双 GOAWAY 协议** (GracefulShutdown, NettyServerHandler.java:1071-1145): start (L1097): ① 第一个 **GOAWAY lastStreamId=MAX_VALUE** (拒绝新流, NO_ERROR) ② 发 GRACEFUL_SHUTDOWN_PING ③ 定时 GRACEFUL_SHUTDOWN_PING_TIMEOUT_NANOS (L1109-1115, ping 超时兜底) → **PING ack 或超时 → secondGoAwayAndClose** (L1118): 第二个 GOAWAY `lastStreamCreated()` (L1127-1131, 等现有流全部完成) + 覆盖 gracefulShutdownTimeoutMillis 后 close (L1133-1141) — **两段式: 先拒新流, 再等旧流**
- **触发源**: maxConnectionAge 定时器 (L401-408 "max_age") / maxConnectionIdleManager (L421-423 "max_idle") / 客户端 GoAway 响应 (L862-868) / 服务端 shutdown (ServerImpl)
- **服务端主动保活**: keepAliveManager (L430-434, KeepAlivePinger L1030, keepAliveDuringTransportIdle=true) — 服务端在空闲时也发 ping
- **客户端 ping 强制**: KeepAliveEnforcer (L261-273, WriteMonitoringFrameWriter 包 L273) — `pingAcceptable()` (L995) 拒绝过度 ping → GOAWAY ENHANCE_YOUR_CALM (L997) + `Status.RESOURCE_EXHAUSTED "Too many pings from client"` (L999) — 防 ping 洪水

代码类型: Implementation (连接状态机)

结论: 优雅关闭是 **协议级两阶段**: GOAWAY(MAX) 拒新 → PING 确认 → GOAWAY(last) 收尾 → close; 服务端双 ping 管理: 自己发 (保活) 和管别人 (限频); **被放弃的方案: 直接 close 连接** — 会丢在途请求; 双 GOAWAY 让客户端知道何时建新连接、何时等完旧流。ping 限频 (ENHANCE_YOUR_CALM + RESOURCE_EXHAUSTED) 是 HTTP/2 服务端的 DoS 防线。 [跨域: G-3 客户端消费 GOAWAY 决定重建连接] (NettyServerHandler.java:401-434,995-999,1071-1145)
