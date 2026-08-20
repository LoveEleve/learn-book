# 闭环笔记 q3: 双协议翻译 — 多协议协商 + remoting 接入

## 假设
单端口多协议 — 首包探测选择协议管线; remoting 请求在 proxy 内翻译成统一语义。

## 验证过程
- **ProtocolNegotiationHandler** (ByteToMessageDecoder): 首包 → 遍历 protocolHandlerList **match(in)** → 命中协议 config 装配管线; 无命中 → **fallbackProtocolHandler 兜底** (L46-58)
- **RemotingProtocolHandler** (61 行): **match 恒 true** (默认协议) → config: NettyEncoder + NettyDecoder + RemotingCodeDistributionHandler + NettyConnectManageHandler + NettyServerHandler (L52-59) — **复用 remoting 模块完整管线** (RM-1 已详)
- **RemotingProtocolServer** (382 行): `implements StartAndShutdown, RemotingProxyOutClient` — 注册处理器 (L191-217+):
  - SEND_MESSAGE/SEND_MESSAGE_V2/SEND_BATCH_MESSAGE/CONSUMER_SEND_MSG_BACK → sendMessageActivity (sendMessageExecutor)
  - END_TRANSACTION → transactionActivity
  - HEART_BEAT → clientManagerActivity (heartbeatExecutor); UNREGISTER_CLIENT/CHECK_CLIENT_CONFIG → defaultExecutor
  - PULL_MESSAGE/**LITE_PULL_MESSAGE**/**POP_MESSAGE** → pullMessageActivity (pullMessageExecutor)
  - UPDATE_CONSUMER_OFFSET/ACK_MESSAGE/CHANGE_MESSAGE_INVISIBLETIME/GET_* → consumerManagerActivity
  - LOCK_BATCH_MQ → consumerManagerActivity (默认线程池)
- **翻译面**: RemotingConverter (remoting/common): RemotingCommand ↔ gRPC 模型; http2proxy 目录 (gRPC↔remoting HTTP/2 辅助)

## 代码类型
Integration (多协议协商 + 翻译)

## 跨域关联
- RM-1 (协议): 请求码/管线复用 (RemotingProtocolHandler 直接复用 remoting handler)
- RM-8 (消费): LITE_PULL/POP 码在 proxy 收敛 (RM-8 POP 面交叉)

## 结论
单端口多协议: 首包 match 选择 (remoting 恒真兜底); remoting 请求经 activity 分发 + RemotingConverter 翻译 → 统一消息面; 非透明字节转发。
源码位置: ProtocolNegotiationHandler.java:46-58; RemotingProtocolHandler.java:47-59; RemotingProtocolServer.java:191-217+
