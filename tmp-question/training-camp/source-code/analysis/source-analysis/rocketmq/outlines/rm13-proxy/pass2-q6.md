# 闭环笔记 q6: 客户端接入面 — 5.x gRPC 客户端

## 假设
5.x 客户端 = gRPC 直连 proxy; 重试/退避/长轮询参数化。

## 验证过程
- **5.x 客户端 (grpc 面)**: 直连 :8081; 协议 = MessagingService v2 (apache/rocketmq/v2 协议, proxy/grpc 对应服务实现)
- **重试参数** (ProxyConfig:127-136): grpcClientProducerMaxAttempts=**3** / grpcClientProducerBackoffMultiplier=**2** (指数退避 2^n) / grpcClientConsumerLongPollingBatchSize=**32** / channelExpiredInSeconds=**60** / contextExpiredInSeconds=**30**
- **POP 语义统一**: LITE_PULL_MESSAGE/POP_MESSAGE 码在 proxy 收敛 (q3) — 老 remoting 客户端的 POP 消费也可经 proxy (RM-8 交叉)
- **无状态网关**: proxy 不存消费状态 (offset/ProcessQueue 在 broker, RM-8/9); 客户端会话注册 (ProducerManager/ConsumerManager) 在 broker (RM-5) — proxy 可水平扩展, 客户端重试换 proxy
- **rmq 客户端桥**: CLUSTER 模式内嵌 6 个 RocketMQ 客户端连 broker (rocketmqMQClientNum=6)

## 代码类型
Architecture (接入面)

## 跨域关联
- RM-7/8/9: 客户端语义 (send/pull/pop/rebalance) 在 proxy 的代选/转发面
- RM-1: remoting 客户端 (老) 与 gRPC 客户端 (新) 双轨

## 结论
5.x 客户端 = gRPC 直连 8081 + 重试 3/退避 2/长轮询 32; proxy 无状态 (状态在 broker) → 水平扩展前提; 老 remoting 客户端走 8080 翻译接入。
源码位置: ProxyConfig.java:127-139; RemotingProtocolServer.java:191-217
