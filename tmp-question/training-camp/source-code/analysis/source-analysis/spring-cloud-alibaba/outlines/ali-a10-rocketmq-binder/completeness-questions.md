# ALI-A10 RocketMQ Stream Binder — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. Binder 的三种创建方法分别对应什么消息场景?
2. 事务消息与分区选择器为什么互斥?
3. push/pull 两种消费模式谁驱动谁?
4. DLQ 为什么必须配 group?
5. maxAttempts 的两种语义分支?

## B. 源码实证 (6)

6. createProducerMessageHandler 的 enabled 校验行为? (grep L81-84)
7. PartitioningInterceptor 怎么找? (grep L94-99)
8. anonymous group 怎么判定? (grep L118-127)
9. 分区计数校正的条件? (grep L117-132)
10. TransactionListener 怎么获取? (grep L164-169)
11. 错误消息处理的三选逻辑? (grep L171-174)

## C. 推理深挖 (5)

12. 分区计数不符时覆盖 partitionCount 的意义? "may be npe" 注释指什么风险?
13. 事务消息发送失败 (事务回滚) 后 handleMessageInternal 走什么路径?
14. pull 模式下轮询频率谁控制? RocketMQMessageSource 与 Stream poller 的关系?
15. ErrorAcknowledgeHandler 自定义的 Bean 容器是哪个? (grep RocketMQBeanContainerCache)
16. 健康 Instrumentation 的 markStartedSuccessfully 失败路径?

## D. 跨域扩展 (4)

17. Stream Binder SPI vs SCC-1 的 PropertySourceLocator SPI: 两种 SPI 插槽对照?
18. 事务消息 vs Seata (A9) 的分布式事务: 消息事务与数据库事务的边界?
19. 本 Binder vs RocketMQ 原生客户端: Stream 抽象带来的能力/代价?
20. DLQ + 重试 vs A6 的限流降级: 失败处理的两种哲学?
