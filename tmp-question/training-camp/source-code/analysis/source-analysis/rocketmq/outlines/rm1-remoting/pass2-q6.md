# 闭环笔记 q6: 扩展面 — RPCHook + TLS + rpc/ (5.x proxy 对接)

## 假设
扩展面 = RPCHook (认证/统计) + TLS + 5.x rpc 层 (proxy 协议桥)。

## 验证过程
- **RPCHook** (RPCHook.java + NettyRemotingAbstract L191-205): doBeforeRequest (发送前 — 签名计算/鉴权) / doAfterResponse (响应后); **认证挂点** (5.x SignAuthentication 接入, RM-13 交叉); 客户端/服务端双侧
- **TLS 面** (TlsSystemConfig.java:24-30): tls.enable / tls.server.mode / tls.config.file / tls.test.mode.enable / tls.server.need.client.auth — 系统属性驱动 (Netty SslContext); TlsHelper (234 行) 构建/加载
- **rpc/ 层** (5.x, 12 文件): RpcClient/RpcClientImpl (gRPC 客户端封装) + RpcRequest/RpcResponse/RpcRequestHeader + RpcClientHook — **proxy 协议桥** (remoting 与 proxy gRPC 对接; RM-13 交叉); ClientMetadata (客户端元数据)
- **metrics** (RemotingMetricsManager): rpcLatency 记录 (5.x OpenTelemetry 面) — writeResponse 埋点
- **ProxyProtocolTest** (remoting 测试): HAProxy 协议编解码 (proxy 层转发场景)
- **ChannelEventListener**: 连接事件监听 (连接建立/关闭通知 — broker 侧清理面)
- **Configuration**: 配置持久化面

## 代码类型
Interface (扩展与集成面)

## 跨域关联
- RM-13 (Proxy): rpc/ 层 + 认证 hook + HAProxy
- RM-5 (Broker): ChannelEventListener 连接管理
- Netty (阶段1): SslContext/TLS

## 结论
扩展面 = RPCHook 认证挂点 + TLS 系统属性配置 + 5.x rpc/ (gRPC 桥) + metrics (OTel) + ChannelEventListener; remoting 是"协议内核 + 扩展插槽"结构。
源码位置: RPCHook.java; TlsSystemConfig.java:24-30; rpc/ 12 文件; ProxyProtocolTest.java
