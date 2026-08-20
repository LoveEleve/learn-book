# G-2 Pass 1 扫描笔记 — 服务端

> 日期: 2026-08-16 | 版本: 1.83.1 | 🔴 A | 模块: core/ServerImpl (980) + ServerCallImpl (399) + netty/NettyServer (494) + NettyServerHandler (1294) + ProtocolNegotiators (1249) + NettyServerBuilder (800) + api/ServerCall (269)

## 继承树/调用图

```
ServerBuilder.build() 抽象 (api, L436)
  → NettyServerBuilder (800, 类声明 L72) → buildTransportServers (L710)
  → NettyServer (494, implements InternalServer) .start(ServerListener) (L218)
  → NettyServerHandler (1294, extends AbstractNettyHandler) — HTTP/2 帧处理
      ├── onHeadersCreateStream → ServerTransportListener.streamCreated
      ├── KeepAlivePinger (L1030) / keepAliveManager (L431)
      └── GracefulShutdown (max_age L406-408 / max_idle L421-423 / GOAWAY L862-868)
  → ServerImpl (980, extends io.grpc.Server)
      ├── InternalHandlerRegistry — method → ServerCallHandler 查找
      ├── ServerStreamListenerImpl → ServerCallImpl (399)
      └── 生命周期: start/beginShutdown/shutdownNow/awaitTermination
  → ServerCallImpl (399, extends ServerCall) — sendHeaders/sendMessage/close 状态机 (L93-227)
  → ServerCalls 分派 (G-1 q6) / ServerInterceptor 链 / ServerServiceDefinition
ProtocolNegotiators (1249) — h2/h2c/TLS/ProxyNegotiator (L496)/ServerTlsHandler (L424)
```

## 基本元素分解

1. **ServerImpl**: 服务生命周期 (start/stop/shutdownNow) + 注册表 (InternalHandlerRegistry) + 连接跟踪 (transports)
2. **NettyServer/NettyServerHandler**: HTTP/2 服务端传输 — 帧接收/流创建/keepalive/优雅关闭
3. **ServerCallImpl**: 单个调用的服务端视图 — 响应状态机 (headers→messages→close)
4. **拦截器链**: ServerInterceptor 包装 ServerCallHandler (注册时包装, 请求时执行)
5. **ProtocolNegotiators**: 连接安全协商 (TLS/明文/h2c)

## 标记问题 (9)

1. **Q1 装配链**: NettyServerBuilder.build() → buildTransportServers → NettyServer 与 ServerImpl 谁先建?ServerListener 回调链 (start 通知谁)?
2. **Q2 注册表**: addService → InternalHandlerRegistry 怎么存?请求进来按什么 key 找 handler?找不到怎么办 (UNIMPLEMENTED)?
3. **Q3 请求接收链路**: HTTP/2 帧 → NettyServerHandler → ServerStream → ServerImpl 的 streamCreated → ServerCallImpl — 中间经过哪些包装 (拦截器在哪一层包)?
4. **Q4 拦截器链**: ServerInterceptors.interceptForward 的包装顺序?多拦截器嵌套顺序?ServerCallHandler 包装与 ServerCall 包装的区别?
5. **Q5 ServerCallImpl 状态机**: sendHeaders 只能一次?close 前必须 sendHeaders?非法顺序抛什么?
6. **Q6 keepalive/优雅关闭**: 服务端主动 ping 与客户端 ping 限制 (keepAliveEnforcer too_many_pings);GOAWAY 优雅关闭流程 (max_age/max_idle/graceful shutdown ping)
7. **Q7 协议协商**: ProtocolNegotiators 服务端如何选 h2/h2c/TLS?ServerTlsHandler 做什么?
8. **Q8 执行器**: ServerBuilder.executor → ServerImpl 怎么用?ServerCallExecutorSupplier 是什么?
9. **Q9 停机语义**: shutdown (优雅) vs shutdownNow 的区别?awaitTermination 与 transport 关闭顺序?

## 已读测试

- ServerImplTest (core): startUp (L263)/stopImmediate (L319)/shutdownNowAfterSlowShutdown (L419) — 生命周期 + 子传输关闭顺序
- NettyServerHandlerTest (netty): inboundDataWithEndStreamShouldForwardToStreamListener (L262)/clientHalfCloseShouldForwardToStreamListener (L296)/clientCancelShouldForwardToStreamListener (L314) — 帧→监听器转发
