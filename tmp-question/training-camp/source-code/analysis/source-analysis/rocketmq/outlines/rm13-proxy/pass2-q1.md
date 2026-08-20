# 闭环笔记 q1: 拓扑双模式 — LOCAL 内嵌 / CLUSTER 独立

## 假设
proxy 可内嵌 broker 也可独立网关。

## 验证过程
- **ProxyMode** (enum: LOCAL/CLUSTER, ProxyStartup:180-213): proxyMode 参数驱动
- **CLUSTER**: DefaultMessagingProcessor.createForClusterMode → 独立 proxy + ClusterMessageService 远程转发; ProxyMetricsManager.initClusterMode
- **LOCAL**: **内嵌 BrokerController** (BrokerStartup.createBrokerController, L198-204) → createForLocalMode → LocalMessageService **直接调 broker processor** (同进程, 免网络); broker 启动失败 → 整个 proxy 失败 (同生命周期)
- **三端口**: grpcServerPort=**8081** / remotingListenPort=**8080** / metricsPromExporterPort=**5557** (ProxyConfig:89,226,240)
- **ProxyConfig 规模** (1528 行): grpc 线程池 **16+2×PROCESSOR_NUMBER** / 队列 **100000** / **grpcMaxInboundMessageSize=130MB** / maxMessageSize=**4MB** / rocketmqMQClientNum=**6** (CLUSTER 内嵌客户端数) / channelExpiredInSeconds=**60** / contextExpiredInSeconds=**30** / maxUserPropertySize=16KB / userPropertyMaxNum=128 / maxMessageGroupSize=64

## 代码类型
Architecture (双模式部署)

## 跨域关联
- RM-5 (Broker): LOCAL 模式复用 BrokerStartup 完整启动链
- RM-8 (消费): POP 面经 proxy (CLUSTER 语义统一)

## 结论
LOCAL = 部署便捷一体; CLUSTER = 独立网关水平扩展; 三端口 (8081 gRPC / 8080 remoting / 5557 metrics)。
源码位置: ProxyStartup.java:180-213; ProxyConfig.java:89,226,240; ProxyMode.java:20-48
