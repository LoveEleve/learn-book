# 闭环笔记 Q8 — Netty 客户端传输: 连接管理 + 双向保活

假设: NettyClientTransport 管连接生命周期 (lifecycleManager), KeepAliveManager 客户端侧主动 ping (可无调用保活), PingCountingFrameWriter 与服务端限频对称。

验证过程:
- **newStream** (NettyClientTransport.java:197-220): channel null → **FailingClientStream** (L202-204, 立即失败); 否则 NettyClientStream + TransportState (handler/eventLoop/maxMessageSize, L210-219)
- **start** (L222-260): **ClientTransportLifecycleManager** (L223-225, 连接状态: CONNECTING→READY→关闭, 通知上层) → `group.next()` 选 eventLoop (L227) → **KeepAliveManager** (L240-244, ClientKeepAlivePinger + **keepAliveWithoutCalls** 参数 — 无活动调用也保活) → NettyClientHandler.newHandler (L247-260)
- **NettyClientHandler** (1186): **PingCountingFrameWriter** (NettyClientHandler.java:239) — 客户端发 ping 计数; 服务端 too_many_pings 响应 (G-2 q6 对称); 流控窗口 (flowControlWindow L160-191)
- **双向保活**: 客户端主动 ping (保活) ↔ 服务端 keepAliveEnforcer (限频) — 同一协议两面
- 测试: NettyClientTransportTest/NettyClientHandlerTest 存在

代码类型: Implementation (传输生命周期)

结论: 客户端传输 = lifecycleManager (状态机) + KeepAliveManager (保活) + Handler (帧/流控); newStream 对未连接通道返回 FailingClientStream (快速失败) — 与 DelayedClientCall 的 NOOP_CALL 双失败路径。**被放弃的方案: 每调用建连接** — 连接复用 (HTTP/2 多路复用) 是 gRPC 性能基础; 保活 ping 保证空闲连接不被中间设备回收。 [跨域: G-2 服务端 keepalive 对称; G-4 InternalSubchannel 管理连接池] [HTTP/2: 连接复用/窗口] (NettyClientTransport.java:197-260; NettyClientHandler.java:239)
