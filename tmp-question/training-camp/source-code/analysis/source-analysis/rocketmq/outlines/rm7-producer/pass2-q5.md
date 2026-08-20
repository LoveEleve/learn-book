# 闭环笔记 q5: sendKernelImpl + 5.x ProduceAccumulator

## 假设
kernel = 请求构造 + hook + invoke 分派; 5.x 批量累积发送。

## 验证过程
- **sendKernelImpl** (L900+): SendMessageRequestHeader 构造 (group/topic/queueId/bornHost/uniqId) → **sendMessageHook 前置** (sendMessageBefore) → **invokeSync/Async/Oneway** (RM-1 三模式, timeout 剩余) → processSendResponse (响应解析) → hook 后置
- **失败面**: kernel 异常 → sendDefaultImpl 捕获分类 (q2)
- **ProduceAccumulator** (5.x, 510 行): **批量累积发送** — 按 aggregateKey 聚合 (topic+queue?) → 条件触发: **batchMaxDelayMs (1ms-30s)** / **batchMaxBytes (1B-2MB)** (L168-181) → 批量发送; 5.x 吞吐优化面
- **测试** (client): DefaultMQProducerTest / DefaultMQProducerImplTest / trace 变体 — 门面+主链覆盖

## 代码类型
Implementation (发送执行 + 批量面)

## 跨域关联
- RM-1 (remoting): invoke 三模式
- RM-3 (存储): 批量消息 (SEND_BATCH)

## 结论
kernel = 头构造 + hook 链 + 三模式 invoke; 5.x ProduceAccumulator 按延迟/字节双条件批量累积 (发送吞吐优化)。
源码位置: DefaultMQProducerImpl.java:900-1130; ProduceAccumulator.java:45-181; client/src/test
