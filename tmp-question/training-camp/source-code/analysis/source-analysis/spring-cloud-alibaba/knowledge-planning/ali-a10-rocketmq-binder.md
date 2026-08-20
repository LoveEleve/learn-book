# ALI-A10 RocketMQ Stream Binder — 知识规划 (KP)

> 🟡 B | 模块: spring-cloud-starter-stream-rocketmq (33) | 版本: 2025.0.0.0

## 01 核心机制提取

| # | 机制 | 核心类:行号 | 一句话 |
|:--:|:--|:--|:--|
| 1 | Binder 契约 | RocketMQMessageChannelBinder:59-62 | AbstractMessageChannelBinder 三方法 |
| 2 | 生产校验 | Binder:81-84 | enabled 禁用 → 抛 |
| 3 | 配置合并 | Binder:85-87 | mergeRocketMQProperties |
| 4 | 分区拦截 | Binder:94-99 | PartitioningInterceptor 查找 |
| 5 | push 消费 | RocketMQInboundChannelAdapter | DefaultMQPushConsumer 驱动 |
| 6 | pull 消费 | RocketMQMessageSource | 轮询消息源 |
| 7 | DLQ 约束 | Binder:123-126 | anonymous + DLQ → 抛 |
| 8 | 重试分流 | Binder:137-144 | maxAttempts>1 重试 / =1 错误通道 |
| 9 | 错误 ack | Binder:161-179 | ErrorAcknowledgeHandler 三选 |
| 10 | 事务生产 | ProducerMessageHandler:164-169 | TransactionMQProducer + TransactionListener |

## 02 高频坑

1. DLQ topic 必须配 group (anonymous 抛)
2. 事务消息不支持自定义 MessageQueueSelector
3. maxAttempts 决定重试 vs errorChannel
4. 分区计数不符会覆盖 partitionCount
5. 转换双层: Stream 面 + MQ 面
6. errAcknowledge 可配置自定义处理器

## 03 深度分类

| 类别 | 条目 (锚点) |
|:--|:--|
| 契约 | AbstractMessageChannelBinder / ExtendedPropertiesBinder / PolledConsumerResources |
| 生产 | 三段式 (转换/选择/发送) / 事务 / 分区校正 / 选择器三选 |
| 消费 | push 适配器 / pull 消息源 / anonymous 群组 |
| 错误 | ErrorAcknowledgeHandler / DLQ / retryTemplate / recoverer |
| 转换 | RocketMQMessageConverter / RocketMQHeaderMapper / MessageQueueSelector |
| 可观测 | Instrumentation / RocketMQBinderHealthIndicator |

## 04 跨域桥接

- → RocketMQ 内核: DefaultMQProducer/PushConsumer/PullConsumer
- → Spring Cloud Stream: Binder SPI 契约
- ↔ ALI-A9: 事务消息 vs 分布式事务 (边界对照)
- → 面试: "RocketMQ Stream Binder 怎么工作" — 方言适配 + push/pull + 事务
