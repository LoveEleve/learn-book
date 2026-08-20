# 闭环笔记 q4: 统一消息面 — MessagingProcessor + 双 MessageService

## 假设
双协议收敛到统一语义面; 本地直写或远程转发。

## 验证过程
- **MessagingProcessor** (337) / **DefaultMessagingProcessor** (364): 统一方法集 (L158-230): sendMessage (queueSelector 参数) / popMessage / ackMessage / batchAckMessage / changeInvisibleTime / pullMessage / updateConsumerOffset(Async) / endTransaction / queryMessage / getTopicRouteDataForProxy / forwardMessageToDeadLetterQueue
- **createForLocalMode** (L110-114): LOCAL — 内嵌 broker
- **createForClusterMode** (L118-136): CLUSTER — **authConfig.getInnerClientAuthenticationCredentials() 解析 SessionCredentials** (内嵌客户端认证) — 远程转发带凭据
- **LocalMessageService** (484): brokerController.getSendMessageProcessor().processRequest(ctx, request) (L115) — **直接调 broker 处理器** (同进程); EndTransactionProcessor (L186) / PopMessageProcessor (L206) 同构; 包一层 RemotingCommand + SimpleChannelHandlerContext 模拟
- **ClusterMessageService**: 独立模式 — 内嵌 RocketMQ 客户端 (rocketmqMQClientNum=6) 远程发 broker
- **MessageQueueSelector** (proxy/service/route, 314): proxy 侧队列选择 — **gRPC 客户端无路由概念, proxy 代选队列**

## 代码类型
Architecture (语义统一层)

## 跨域关联
- RM-1 (协议): 双协议入口 (gRPC v2 + remoting)
- RM-7 (发送): sendMessage 语义 (queueSelector)
- RM-8 (消费): pop/ack/pull 语义 (RM-8 POP 面)

## 结论
统一消息面 = 双协议收敛点; LOCAL 直调 broker processor (零网络), CLUSTER 内嵌客户端远程转发 (带认证凭据); proxy 代选队列 (gRPC 客户端无路由)。
源码位置: DefaultMessagingProcessor.java:110-136,158-230; LocalMessageService.java:80-115,186,206
