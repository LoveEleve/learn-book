# N-24 事件中心 — NotifyCenter 的发布器工厂与订阅分发

> 前置: [[NC-1-Naming]] + [[NC-2-Config]] + [[NC-5-一致性]] (全模块消费) | 对照: 事件驱动的公共底座
> 🔴 A | 方案 A (全深度) | 闭环: q1(注册面) q2(发布器) q3(订阅分发)

**读者处境**: 全模块的 NotifyCenter.publishEvent/registerSubscriber — 这个事件中心怎么实现? 慢事件 (SlowEvent) 与普通事件的区别? 发布器是线程吗?

### 1. 注册面 — NotifyCenter 的注册 API

场景: 事件/订阅怎么注册?
源码路径:
- **NotifyCenter** (notify/NotifyCenter.java:45): 单例 — **publisherMap** (L67: ConcurrentHashMap<EventType, EventPublisher>) + **SPI 加载 EventPublisher** (L79-80) + 默认工厂 (L55)
- **registerToPublisher(eventType, queueMaxSize)** (L329-330): 独立发布器
- **registerToSharePublisher(SlowEvent)** (L319): 共享发布器 (慢事件)
- **registerSubscriber(consumer)** (L164-165) / **deregisterSubscriber** (L228)
- **publishEvent(event)** (L280-295): 按类型路由到发布器
关键设计 (q1): **"类型→发布器 Map + SPI 工厂"** — 每事件类型独立发布器 (可配队列), 慢事件共享; SPI 可扩展发布器实现。 [模式: 事件注册表]

### 2. 发布器 — DefaultPublisher 的队列线程

场景: 发布器怎么异步?
源码路径:
- **DefaultPublisher** (notify/DefaultPublisher.java:41): **extends Thread implements EventPublisher** — **ArrayBlockingQueue 队列** (L55/71) + 队列上限 (L53: queueMaxSize/ringBufferSize)
- **run 消费循环** (L94-111): **queue.take()** (L111) → receiveEvent
- **receiveEvent** (L169-192): 遍历订阅者 → notifySubscriber (L192)
- **publish** (L135): offer 入队
- **DefaultSharePublisher** (DefaultSharePublisher.java:33): 共享版 (慢事件)
- **ShardedEventPublisher** (ShardedEventPublisher.java:28): 分片版
- 命名: "nacos.publisher-{type}" (L65)
关键设计 (q2): **"发布器即线程 = 异步解耦"** — 每发布器一个消费者线程 + 有界队列; 生产-消费模型 (NC-1 NotifyCenter 注册的发布器全走此面)。 [模式: 队列线程]

### 3. 订阅分发 — Subscriber 族

场景: 订阅者怎么组织?
源码路径:
- **Subscriber** (listener/Subscriber.java:30): 接口 — onEvent + subscribeType + ignoreExpireEvent
- **SmartSubscriber** (listener/SmartSubscriber.java:30): 多类型订阅者 (subscribeTypes)
- **SlowEvent** (SlowEvent.java:26): 慢事件标记 (共享发布器)
- **Event** (Event.java:24): 事件基类 (sequence 号)
- **EventPublisherFactory** (EventPublisherFactory.java:26): 工厂
关键设计 (q3): **"订阅者 = 单/多类型 + 慢事件分型"** — SmartSubscriber 一次订阅多类型; SlowEvent 走共享发布器 (防每类型建线程)。 [模式: 订阅分型]

### 4. 测试与行为锚

场景: 事件中心边界?
源码路径:
- 测试: NotifyCenterTest / DefaultPublisherTest (common test)
- 锚: publisher 线程名 "nacos.publisher-{type}"
- 慢事件共享语义
关键设计 (q1): **"慢事件共享 = 资源节约"** — 低频慢事件不独占线程。 [模式: 共享发布器]
