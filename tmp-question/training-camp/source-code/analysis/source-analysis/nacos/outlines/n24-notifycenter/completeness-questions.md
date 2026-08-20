# N-24 事件中心 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. 类型→发布器 Map 与共享发布器的分工?
2. 发布器线程的消费模型?
3. SmartSubscriber 与单类型订阅差异?
4. 发布器关闭时队列中的事件?
5. 订阅者异常对事件循环的影响?

## B. 源码实证 (6)

6. publisherMap 结构? (grep NotifyCenter:67)
7. SPI 加载? (grep L79-80)
8. registerToPublisher 签名? (grep L329-330)
9. DefaultPublisher 的队列? (grep L55/71)
10. receiveEvent 的分发? (grep L169-192)
11. SlowEvent 的共享? (grep DefaultSharePublisher)

## C. 推理深挖 (5)

12. 队列满时 publish 的语义 (丢弃/阻塞)?
13. 订阅者异常时事件处理?
14. 慢事件阈值与共享队列容量?
15. 发布器线程的关闭流程?
16. 分片发布器 (ShardedEventPublisher) 的适用场景?

## D. 跨域扩展 (4)

17. 本域 vs NC-1/NC-5 消费方: 事件驱动闭环?
18. 队列线程 vs openjdk 的 JFR 事件: 事件系统设计对照?
19. 发布器 vs N-12 推送延迟引擎: 异步队列对照?
20. 本域与 N-22 连接事件的关系?
