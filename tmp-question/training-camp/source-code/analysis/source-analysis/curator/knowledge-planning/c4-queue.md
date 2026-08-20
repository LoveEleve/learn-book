# C-4 队列与屏障 — 知识规划 (KP)

> 域级: 🟡 B | 模块: curator-recipes/.../recipes/queue/ (19 文件: DistributedQueue 656 / QueueBuilder 256 / SimpleDistributedQueue 243 / DistributedPriorityQueue 193 / DistributedIdQueue 230 / DistributedDelayQueue 229 / BlockingQueueConsumer 144 / ChildrenCache 146 / QueueSharder 257 / QueueSharderPolicies 136 / QueueBase 66 / ItemSerializer 99 / MultiItem 36 / ErrorMode 36 / QueueSafety 61 / QueueAllocator 26 / QueueConsumer 35 / QueuePutListener 40 / QueueSerializer 41) + recipes/barriers/ (2 文件: DistributedBarrier 126 / DistributedDoubleBarrier 293)
> 日期: 2026-08-15 | 版本: 5.8.0

## 一、机制提取 (逐源)

### M1 DistributedQueue: 顺序节点 FIFO 队列 (656)
- 模型: PERSISTENT_SEQUENTIAL 节点 (doPutInForeground L357-371: `client.create().withMode(PERSISTENT_SEQUENTIAL)` L359); 队列入队=建节点, 排序=children 名字典序
- **runLoop 消费循环** (L457-490): blockingNextGetData(currentVersion) 阻塞等版本变化 → sortChildren (L469) → getDelay(队首) (L472, 延迟队列用) → processChildren
- **processChildren** (L492-540): Semaphore(0) 统计在途; 过滤外来节点 (L502-506); minItemsBeforeRefresh/refreshOnWatch 提前终止 (L508-513); 每 item executor.execute 异步处理; processedLatch.acquire(size) 等全部完成 (L539)
- **processNormally 先删后消费** (L578-605): getData 拿 Stat → delete().withVersion(stat.getVersion()) (L587-589) → processMessageBytes 调 consumer (L591-593); NodeExists/NoNode/BadVersion = 被他人抢走 (L596-602)
- **processWithLockSafety 锁安全模式** (L607-651): 每 item 创建同名 EPHEMERAL 锁节点 (L612) → 谁建成功谁处理; requeue = inTransaction().delete+create (L623-632); finally delete().guaranteed() 释放锁节点 (L644-647)
- **blockIfMaxed 有界队列** (L445-455): blockingNextGetData(version, maxWait) — 版本没变则等待
- **flushPuts** (L240-257): putCount 计数 + wait/notify — 等待所有 put 提交
- 生命周期 LATENT→STARTED→STOPPED (L93-97); start() 建队列/锁路径 + 提交 runLoop 线程 (L158-183)
- 扩展点: sortChildren/getChildren/getDelay/makeRequeueItemPath protected (L424-434, L653-655)

### M2 ChildrenCache: 版本化 children 缓存 (146)
- 一次性 watcher 自动重挂: watcher 事件 → sync() 重新 getChildren+watch (L47-54, L134-136)
- Data{children, version} 不可变快照; setNewChildren version+1 + notifyAll (L138-145)
- **blockingNextGetData(startVersion, maxWait)** (L107-128): 版本未变 wait
- 连接恢复: CONNECTED/RECONNECTED → sync() 重建 (L65-73)

### M3 QueueBuilder 与四变体
- QueueBuilder: builder(client, consumer, serializer, queuePath) (L62-65); 默认 putInBackground=true (L45)/finalFlushMs=5000 (L46)/NOT_SET=MAX_VALUE (L50)
- **lockPath** (L201-204): 无锁=at-most-once (先删后消费丢消息), 有锁=消费后删+崩溃回队列 (文档 L189-200)
- maxItems → 强制 putInBackground=false (L216-220)
- **DistributedPriorityQueue** (193): refreshOnWatch=true 固定 (L64); priority 编码进节点名 (L111-116): 负数补码 0 前缀 + 8 位 hex, 正数 1 前缀 (L186-192) — 字典序=数值序
- **DistributedIdQueue** (230): 节点名 queue-|id| (L196-198); parseId/fixId (/ 与 | → _) (L213-229); remove(id) 遍历匹配 (L168-183)
- **DistributedDelayQueue** (229): 节点名 queue-|HEX_EPOCH| (L212-214); getDelay = epoch - now (L75-83); 队首延迟>0 → runLoop 阻塞到点 (L472-475)
- **SimpleDistributedQueue** (243): ZK 官方兼容 (qn- 前缀 L55); 同步阻塞 API — internalPoll 死循环 + 一次性 watcher + CountDownLatch (L171-208); offer PERSISTENT_SEQUENTIAL (L114-121)

### M4 QueueSharder: 分片 (257)
- 动机: ZK 传输层限制单队列 ~10K 节点 (类注释 L53-59)
- start: 发现已有队列或建 queue-UUID (L193-218) + **LeaderLatch 唯一扩容器** (L119) + 周期 checkThreshold (L121-136)
- checkThreshold (L220-256): children ≥ newQueueThreshold → 移出 preferred 标记扩容; ≤ 阈值一半 → 加回; **仅 leader** 建新分片 (L239-251)
- 默认值: newQueueThreshold=10000/thresholdCheckMs=30000/maxQueues=10 (QueueSharderPolicies L35-37)

### M5 BlockingQueueConsumer (144)
- 把 push 消费转 Java BlockingQueue 拉取 (L67-70); take/take(timeout)/drainTo (L96-138); 构造注入 ConnectionStateListener (L44-65)

### M6 DistributedBarrier: 单屏障 (126)
- **节点存在=屏障关闭, 不存在=打开** (checkExists==null 判定 L108)
- setBarrier/removeBarrier 幂等 (L63-82)
- waitOnBarrier: 循环 checkExists().usingWatcher → 存在则 wait() (L101-125); watcher → postSafeNotify 唤醒 (L42-47)

### M7 DistributedDoubleBarrier: 双屏障 (293)
- enter: 查 ready 节点 → 创建 EPHEMERAL + UUID 成员节点 (L115-132) → internalEnter: getChildren 计数 ≥ memberQty → 创建 ready 节点 (L260-292, 计数检查 L265, readyPath 创建 L267)
- leave: 删自己节点 + 反序等待 (L139-158): **最小序号 watch 最高序号, 其他人 watch 最小** (L216-227); connectionLost 显式抛 (L183-185)
- 成员节点 EPHEMERAL + UUID 随机命名 (L87-95); memberQty 是阈值非上限 (L57-61)
- watcher: connectionLost 标志 + hasBeenNotified (L62-73)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M1 runLoop + processChildren 消费模型 | P1 | 队列核心; 先删后消费语义 |
| M2 ChildrenCache 版本驱动 | P1 | 事件驱动消费的基础 |
| M3 变体族 (优先级/延迟/ID) | P2 | 名字编码技巧 |
| M1 锁安全模式 + requeue 事务 | P2 | at-least-once 语义 |
| M4 QueueSharder | P3 | 生产细节 |
| M6/M7 屏障 | P2 | 同步原语对照 |

## 三、负面空间

- **无 ack/确认语义**: 无锁模式先删后消费, 消费失败消息即丢 (at-most-once)
- **无持久化重放**: 消息是 PERSISTENT 节点但消费即删, 无重放
- **无多消费者负载均衡**: 多消费者抢同一节点, 无分区分配 (对比 Kafka)
- **无消息过期**: 数据面不检查 TTL (延迟队列仅控制消费时机)
- **无队列级事务**: requeue 事务只覆盖单节点操作
