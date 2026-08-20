# 闭环笔记 q1: 门面与三模式 — DefaultMQProducer.send 族

## 假设
门面 send 族按 CommunicationMode 分派; 配置面集中。

## 验证过程
- **门面** (DefaultMQProducer, 1442 行): send(Message) → defaultMQProducerImpl.sendDefaultImpl (SYNC) / send(msg, SendCallback, timeout) → ASYNC / sendOneway → ONEWAY
- **配置** (L127-139): retryTimesWhenSendFailed=2 / retryAnotherBrokerWhenNotStoreOK=false / sendLatencyFaultEnable (转 MQFaultStrategy) / **retryResponseCodes** (MQBrokerException 可重试码集合 — 发送失败/刷盘超时等)
- **启动** (L357): defaultMQProducerImpl.start (MQClientInstance 启动 — RM-8 交叉)
- **hook**: registerSendMessageHook (sendMessageHookList — 可观测扩展)

## 代码类型
Interface (门面)

## 跨域关联
- RM-1 (remoting): invoke 三模式
- RM-8 (消费): MQClientInstance 共享

## 结论
门面 = send 族三模式 + 重试配置 (2 次/换 broker/重试码集合) + hook 扩展; ASYNC/ONEWAY 立即返回。
源码位置: DefaultMQProducer.java:127-139,357-367; DefaultMQProducerImpl.java:534-560
