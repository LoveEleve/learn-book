# ALI-A10 RocketMQ Stream Binder — Spring Cloud Stream 的 RocketMQ 方言

> 前置: 无 (独立消息面) | 引出: [[RocketMQ]] (内核) | 对照: Spring Cloud Stream Binder SPI / RocketMQ 原生客户端
> 🟡 B | 方案 B (重要域) | 闭环: q1(binder 契约) q2(生产面) q3(消费面) q4(错误语义)

**读者处境**: `spring.cloud.stream.bindings.output.destination=topic-x` 配置后, 消息怎么发到 RocketMQ? push/pull 两种消费模式怎么选? 事务消息怎么接入? 消费失败的 ack 语义?

### 1. Binder 契约 — RocketMQMessageChannelBinder 的三方法实现

场景: Spring Cloud Stream 的 Binder SPI 怎么落地?
源码路径:
- **RocketMQMessageChannelBinder** (RocketMQMessageChannelBinder.java:59): extends AbstractMessageChannelBinder<Consumer/ProducerProperties, RocketMQTopicProvisioner> + implements ExtendedPropertiesBinder
- **createProducerMessageHandler** (L77-103): enabled 校验 (L81-84, 禁用 → 抛) → mergeRocketMQProperties 合并配置 (L85-87) → new RocketMQProducerMessageHandler + errorChannel (L91-93) + **PartitioningInterceptor 查找** (L94-99, 分区支持) + errorMessageStrategy (L101)
- **createConsumerEndpoint** (L113-146): **anonymous 群组判定** (L118) + **DLQ 必须配 group** (L123-126) + anonymousGroup 兜底 (L127) + RocketMQInboundChannelAdapter (L133) + **maxAttempts>1 → retryTemplate + recoveryCallback / 否则 errorChannel** (L137-144)
- **createPolledConsumerResources** (L148-159): RocketMQMessageSource (pull 模式) + PolledConsumerResources
关键设计 (q1): **"Binder = 消息方言适配器"** — 契约三方法分别对应 push 消费/生产/pull 消费; Stream 框架管通道绑定, Binder 只管 RocketMQ 方言。 [模式: 方言适配器]

### 2. 生产面 — RocketMQProducerMessageHandler 的事务与分区

场景: 消息发送怎么支持事务/分区?
源码路径:
- **onInit** (RocketMQProducerMessageHandler.java:91-107): enabled 守卫 (L93-95) + RocketMQProduceFactory.initRocketMQProducer (L97-98) + **isTrans 判定** (L99: TransactionMQProducer) + **MessageQueueSelector 三选** (L101-106): 用户 Bean / 分区默认 PartitionMessageQueueSelector / null
- **start** (L109-143): defaultMQProducer.start + **分区计数校正** (L117-132: 非事务 + partitioned → fetchPublishMessageQueues 与 partitionCount 比对, 不符则以真实队列数覆盖 + 同步 partitioningInterceptor) + 健康 instrumentation
- **handleMessageInternal** (L158+): RocketMQMessageConverterSupport.convertMessage2MQ (L161-162) + **TransactionMQProducer → TransactionListener Bean 查找** (L164-169, 缺 → MessagingException) / 普通 → 直接 send + 选择器
- 注释锚: "TransactionMQProducer does not currently support custom MessageQueueSelector" (L115-116) — 事务与分区互斥
关键设计 (q2): **"生产 = 转换 → 选择 → 发送 三段式"** — 消息转换 (MQ Message) → 队列选择 (分区/自定义) → 发送 (普通/事务); 事务消息走 TransactionListener, 分区选择器与事务互斥。 [模式: 三段式发送]
- **示例实证**: examples/rocketmq-tx-example TransactionListenerImpl (用户 Bean, 三态 COMMIT/ROLLBACK/UNKNOW) — 被 RocketMQBeanContainerCache.getBean(transactionListener) 查找; examples/rocketmq-orderly-consume-example OrderlyMessageQueueSelector (用户 Bean, id%tags%mqs) — 选择器三选中用户优先

### 3. 消费面 — push 适配器与 pull 消息源

场景: push/pull 两种消费模式的差异?
源码路径:
- **RocketMQInboundChannelAdapter** (integration/inbound/RocketMQInboundChannelAdapter.java:53, 230 行): AbstractMessageSource/适配器 — push 模式 (MQ 推送 + 内部消费)
- **RocketMQMessageSource** (integration/inbound/pull/RocketMQMessageSource.java:52, 177 行): pollable 消息源 — pull 模式 (Stream 轮询驱动)
- **RocketMQConsumerFactory** (integration/inbound/RocketMQConsumerFactory.java:44, 185 行): DefaultMQPushConsumer/DefaultMQPullConsumer 工厂 + 订阅关系
- **分派**: createConsumerEndpoint (push) vs createPolledConsumerResources (pull) — 由 Stream 的 binding 模式决定
关键设计 (q3): **"push = 事件驱动, pull = 轮询驱动"** — 两种模式复用同一属性合并/错误基础设施, 差异只在消费触发方式。 [模式: 双消费模式]

### 4. 错误语义 — ErrorAcknowledgeHandler 与 DLQ 约束

场景: 消费失败怎么 ack? DLQ 怎么用?
源码路径:
- **getPolledConsumerErrorMessageHandler** (Binder L161-179): MessagingException 载荷 → **AcknowledgmentCallback 提取** (L167-170) → **ErrorAcknowledgeHandler 三选** (L171-174: 用户配置 errAcknowledge / DefaultErrorAcknowledgeHandler) → ack.acknowledge(handler.handler(...))
- **DLQ 约束** (L123-126): anonymous group + DLQ topic → 抛 "group must be configured for DLQ" — DLQ 命名需要 group
- ErrorAcknowledgeHandler 接口 + DefaultErrorAcknowledgeHandler (extend/inbound/pull/)
- maxAttempts 语义: >1 → 重试 + recoverer, =1 → 直接 errorChannel
关键设计 (q4): **"ack 语义可插拔 + DLQ 强约束"** — 错误处理策略由 errAcknowledge 配置驱动 (可自定义), DLQ 是安全网但要求 group (命名规范); 重试与错误通道按 maxAttempts 分流。 [模式: 可插拔 ack]

### 5. 辅助面 — 转换/头映射/健康/装配

场景: 消息头怎么映射? Binder 怎么装配?
源码路径:
- **RocketMQMessageConverter** (convert/, 132 行) + RocketMQMessageConverterSupport: Message ↔ MQ Message 双向
- **RocketMQHeaderMapper** (support/): AbstractRocketMQHeaderMapper + JacksonRocketMQHeaderMapper — 头序列化 (自定义头保留)
- **RocketMQBinderHealthIndicator** (actuator/) + InstrumentationManager: 生产/消费健康上报
- **RocketMQBinderAutoConfiguration** (autoconfigurate/, 75 行): @ConditionalOnClass + 注册 Binder Bean (MessageChannelBinder)
- **RocketMQTopicProvisioner** (provisioning/, 98 行): topic 生命周期管理
关键设计 (q5): **"辅助面 = 转换/映射/健康三层"** — 转换管载荷, 映射管头, 健康管观测 — 各司其职, 不污染核心 Binder。 [模式: 关注点分离]

### 6. 测试与行为锚

场景: Binder 的行为锚?
源码路径:
- 测试: RocketMQBinderTests / RocketMQMessageChannelBinderTests (test 目录)
- 注释锚: "group must be configured for DLQ" (L124) / "TransactionMQProducer does not currently support custom MessageQueueSelector" (L115-116) — 约束语义官方声明
- 引用锚: Spring Cloud Stream docs 3.2.1 (L121) — DLQ 设计依据
关键设计 (q1): **"约束写注释 + 依据引文档"** — 边界行为 (DLQ/事务分区) 以注释固化, 设计依据链接官方文档。 [模式: 约束固化]
