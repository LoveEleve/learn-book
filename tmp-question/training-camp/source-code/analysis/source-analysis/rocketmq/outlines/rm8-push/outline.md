# RM-8 消费-Push — 拉取调度 + 缓存 + 消费服务

> 前置: [[RM-1-remoting]] (ASYNC) + [[RM-3-commitlog]] (存储) + [[RM-6-过滤]] | 引出: [[RM-9-再平衡]] | 对照: Kafka Consumer (poll 循环)
> 🔴 A | 6 KP | [模式: 长轮询拉取 + 消息缓存 + 消费线程池 + 进度持久化]
> Pass 2 闭环: q1(拉取调度) q2(长轮询) q3(ProcessQueue) q4(消费服务) q5(offset) q6(5.x POP)

**读者处境**: Push 消费者怎么"自动"收到消息? 拉取-消费之间缓冲在哪? 消费失败去哪? 这篇拆 Push: 拉取调度、长轮询、缓存与流控、并发/有序消费、进度管理。

### 1. 拉取调度 — PullMessageService

场景: 谁驱动拉取?
源码路径:
- **PullMessageService** (ServiceThread): messageRequestQueue (LinkedBlockingQueue) — 拉取请求队列
- **双入队**: executePullRequestImmediately (即时) / executePullRequestLater (延迟)
- **延迟分级**: 异常 3s / 缓存流控 50ms / broker 流控 20ms / 挂起 1s
关键设计 (q1): **拉取请求队列化** = 调度解耦; 延迟分级按场景 (流控快重试, 异常慢重试)。[模式: 调度队列]

### 2. 长轮询拉取 — pullKernelImpl

场景: 没消息时客户端干什么?
源码路径:
- **pullMessage**: offsetStore.readOffset → **sysFlag 四 bit** → pullKernelImpl (**pullBatchSize=32 默认**, consumeTimeout=15 分钟)
- **长轮询参数**: **broker 挂起 15s** + **客户端超时 30s**
- **回调**: 成功 → ProcessQueue; 空 → 1s 重拉; 流控 → 50ms
关键设计 (q2): **长轮询 = 服务端挂起 + 到达唤醒** (PullRequestHoldService, RM-5) — 省空轮询。[模式: 长轮询]

### 3. ProcessQueue — 拉取-消费缓冲

场景: 拉得快消费慢怎么办?
源码路径:
- **TreeMap 双缓冲**: msgTreeMap (按 offset 有序) + consumingMsgOrderlyTreeMap (有序消费)
- **流控三阈值**: consumeConcurrentlyMaxSpan=2000 (跨度) / pullThresholdForQueue=1000 条 / pullThresholdSizeForQueue=100MiB (三独立面)
- **推进**: commit/removeMessage → offset 前进
关键设计 (q3): **有序缓冲 + 双阈值流控** — 防止拉爆内存; 跨度/条数/字节三面控制。[模式: 消息缓冲]

### 4. 消费服务 — Concurrently vs Orderly

场景: 消费并发度怎么控制?
源码路径:
- **ConcurrentlyService**: 线程池 (min/max=20 默认可调, 60s 空闲回收) + ConsumeRequest 批量 (batchMaxSize=1 默认) → listener → CONSUME_SUCCESS → commit; **RECONSUME_LATER → sendMessageBack (延迟重试, delayLevel 业务可调; 超 maxReconsumeTimes=16 → DLQ 死信)**; cleanExpireMsgExecutors 过期清理; **消费 hooks (filterMessage/Before/After — 可观测扩展)**
- **OrderlyService**: **MessageQueueLock 队列级锁** + processQueue.lock → 队列内串行; 锁失败 10ms/3s 延迟重试
关键设计 (q4): **并发 = 线程池无序**; **有序 = 队列锁串行** (业务顺序保证)。[模式: 消费调度]

### 5. 消费进度 — OffsetStore 双实现

场景: 进度存哪?
源码路径:
- **RemoteBrokerOffsetStore** (集群): 内存 + persist → **broker (ConsumerOffsetManager, RM-5)** — 重平衡共享
- **LocalFileOffsetStore** (广播): 本地文件 (rocketmq.client.localOffsetStoreDir)
- **提交链**: 消费成功 → commit → updateOffset → persist (拉取后 + 定时 + 关闭)
关键设计 (q5): **集群进度在 broker = 跨客户端共享** (重平衡后从 broker 续读)。[模式: 进度持久化]

### 6. 5.x POP 模式 + 测试

- **POP** (5.x): **PopProcessQueue.ack()** — pop+ack 消费协议 (可见性窗口, 对照 Kafka); broker PopLongPollingService (RM-5)
- **测试** (5345 行): PullConsumerTest / **rebalance 6 算法测试** (RM-9 素材) / ControllableOffset

### 负面空间 — Push 消费刻意不做的事

- **不做服务端推送**: "Push" 是客户端长轮询模拟 (真实 pull + broker 挂起)
- **不做自动负载均衡参与**: 队列分配归 RebalanceService (RM-9)
- **不做消费幂等**: 消费重复由业务处理 (at-least-once)
- **不做消息本地持久化**: 拉取即内存 (崩溃丢未消费缓冲)
- **不做 backpressure 深度**: 流控是阈值暂停, 无动态反馈 (对照 Kafka max.poll)

→ 引出: 队列怎么分给消费者? → [[RM-9-再平衡]]
