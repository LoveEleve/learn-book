# 闭环笔记 q2: gRPC 协议层 — 服务面 + 管线

## 假设
gRPC 入口 = v2 服务实现 + 函数式管线 + 线程池分流。

## 验证过程
- **GrpcMessagingApplication** (464 行): `extends MessagingServiceGrpc.MessagingServiceImplBase` — v2 协议全部服务方法 (sendMessage/receiveMessage/ackMessage/changeInvisibleTime/popMessage/queryMessage/endTransaction)
- **RequestPipeline 构造** (create L145-158): 
  ```java
  pipeline = pipeline.pipe(new AuthorizationPipeline(authConfig, ...))
                     .pipe(new AuthenticationPipeline(authConfig, ...));
  pipeline = pipeline.pipe(new ContextInitPipeline());
  ```
- **pipe 语义反转** (RequestPipeline:28-33): `source.execute()` 先执行再自身 → **执行序 = ContextInit → Authentication → Authorization** (最后 pipe 的先生执行)
- **authConfig 驱动**: `if (authConfig != null)` 才挂认证/授权 — **无认证配置零开销**
- **addExecutor 分流** (L166+): request instanceof GeneratedMessageV3 → 管线执行 + validateContext; 按 producer/consumer/clientManager/transaction 四线程池 + **拒绝 → flowLimitStatus (TOO_MANY_REQUESTS)** (L161)
- **GrpcServer** (27 行): server.start(); GrpcServerBuilder + ProxyAndTlsProtocolNegotiator (TLS 协商, tlsTestModeEnable 默认 true)

## 代码类型
Implementation (gRPC 服务 + 管线)

## 跨域关联
- RM-1 (协议): gRPC 与 remoting 是平级协议面 (proxy 内共存)
- RM-5 (Broker): 认证管线同构 (broker 侧同款)

## 结论
gRPC 层 = MessagingServiceImplBase (v2) + pipe 反转管线 (ContextInit→Authn→Authz) + 四线程池分流 + 满则流控; 认证配置驱动。
源码位置: GrpcMessagingApplication.java:145-175; RequestPipeline.java:28-33; GrpcServer.java:42-44
