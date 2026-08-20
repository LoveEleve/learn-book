# C-4 队列与屏障 — 顺序节点上的 FIFO 与"发令枪"

> 前置: [[C-1-CuratorFramework]] (children watcher/后台操作) + [[Z-6-Watcher]] (一次性 watcher 重挂) | 引出: [[C-8-服务发现]] (QueueSharder 用 LeaderLatch) | 对照: ZK 官方 DistributedQueue + Kafka (消息语义)
> 🟡 B | 7 KP | [模式: 顺序节点 + 版本缓存 + watcher 重挂]
> Pass 2 闭环: q1(消费循环) q2(名字编码) q3(锁安全) q4(屏障)

**读者处境**: 用 ZK 做任务队列, 消费者怎么知道"有新任务了"? 消费到一半进程崩了, 任务算不算丢? 优先级/延迟队列怎么在"顺序节点只有序号"的限制下实现?

### 1. 入队 — PERSISTENT_SEQUENTIAL 就是排队

场景: put() 一条消息, ZK 里发生了什么
源码路径:
- doPutInForeground (DistributedQueue.java:357-371): `client.create().withMode(PERSISTENT_SEQUENTIAL).forPath(path, bytes)` (DistributedQueue.java:359)
- 后台版 doPutInBackground → internalCreateNode (DistributedQueue.java:373-407); putCount 统计 + putListener 通知 (DistributedQueue.java:360-363)
- start(): 建队列路径 + 提交 runLoop 线程 (DistributedQueue.java:158-183)
关键设计 (q1): **ZK 服务端串行化创建 = 全局排队登记**; 与锁不同的只是节点持久性 (消息不能被会话带走) 与 maxLeases 语义。 [模式: 顺序节点]

### 2. 消费循环 — 版本号驱动的阻塞等待

场景: 消费者线程在干嘛? 轮询 getChildren 吗?
源码路径:
- **runLoop** (DistributedQueue.java:457-490): `childrenCache.blockingNextGetData(currentVersion)` (DistributedQueue.java:463-465) — 版本不变则线程 wait, 变化才醒 → sortChildren (L469) → processChildren (L492-540)
- **ChildrenCache** (ChildrenCache.java:146): 一次性 watcher 收到事件 → **sync() 重新 getChildren+重挂 watcher** (ChildrenCache.java:47-54, 134-136) → version+1 + notifyAll (ChildrenCache.java:138-145); blockingNextGetData 版本等待 (ChildrenCache.java:107-128)
- processChildren (L492-540): Semaphore(0) 统计在途 → 每 item 异步处理 (L520-536) → acquire(size) 等全部完成
关键设计 (q1): **watcher 重挂是硬性要求** (ZK 一次性语义, Z-6 交叉); 版本号是"数据变了"的单调信号, 比比较对象引用可靠 (防 ABA); 消费循环与处理线程分离 — 事件不阻塞处理。 [模式: 版本缓存]

### 3. 消费 — 先删后消费 vs 锁安全模式

场景: 消息什么时候算"被消费了"?
源码路径:
- **processNormally 先删后消费** (DistributedQueue.java:578-605): getData 拿 Stat → **delete().withVersion(stat.getVersion())** (DistributedQueue.java:587-589) → consumer.processMessage (L591-593); NodeExists/NoNode/BadVersion = 他人已抢 → 跳过 (L596-602)
- **processWithLockSafety** (DistributedQueue.java:607-651): 每 item 创建同名 **EPHEMERAL** 锁节点 (L612) → 谁建成功谁处理; requeue = **事务**: delete + create(PERSISTENT_SEQUENTIAL) 原子重排 (L623-632); finally delete().guaranteed() (L644-647)
- 语义: 无锁 = at-most-once (类注释 L64-66 明示); 有锁 = 消费后删, 崩溃时锁自动释放消息回队列
关键设计 (q3): **"删"是消费语义的锚点** — 版本号删除防误删他人新写; 锁安全模式用"同名节点互斥"把消费权变成独占, 用事务保证 requeue 不丢消息。 [模式: 乐观删除/独占消费]

### 4. 变体族 — 把优先级/延迟/ID 编进节点名

场景: 只有序号节点的 ZK, 怎么表达优先级?
源码路径:
- **DistributedPriorityQueue** (DistributedPriorityQueue.java:193): refreshOnWatch=true (L64); 节点名前缀编码: 负数补码 0 前缀+8 位 hex, 正数 1 前缀 (DistributedPriorityQueue.java:186-192) — **字典序 = 数值序**
- **DistributedDelayQueue** (DistributedDelayQueue.java:229): 节点名 queue-|HEX_EPOCH| (L212-214); getDelay = epoch - now (L75-83); 队首未到期 → runLoop 阻塞到点 (DistributedQueue.java:472-475)
- **DistributedIdQueue** (230): queue-|id| (L196-198); remove(id) 遍历匹配 (L168-183); fixId 替换 / 与 | (L213-216)
- **SimpleDistributedQueue** (SimpleDistributedQueue.java:243): ZK 官方兼容 (qn- 前缀 L55); take 用一次性 watcher + CountDownLatch (SimpleDistributedQueue.java:171-208)
关键设计 (q2): **排序约束逼出名字编码** — 顺序节点只有创建序, 变体把业务序编进名字参与字典序; 这是"用数据面补协议面"的经典手法。 [模式: 名字编码]

### 5. 有界队列与 flushPuts — 近似上限与异步确认

场景: 队列上限 1000, 怎么近似保证?
源码路径:
- blockIfMaxed (DistributedQueue.java:445-455): blockingNextGetData(data.version, maxWait) — 版本没变则等待
- maxItems 设置 → QueueBuilder 强制 putInBackground=false (QueueBuilder.java:216-220); 构造警告有界+后台不一致 (DistributedQueue.java:141-144)
- flushPuts (DistributedQueue.java:240-257): putCount AtomicInteger + wait/notify — 等所有异步 put 提交
关键设计 (q1): **近似上界** — 计数基于缓存快照, 多进程并发 put 无法精确; 前台 put 才能保证 put 完成时序。 [模式: 近似约束]

### 6. 屏障 — 单屏障发令枪 vs 双屏障集合点

场景: 100 台机器等齐了再开跑, 用 ZK 怎么表达?
源码路径:
- **DistributedBarrier** (DistributedBarrier.java:126): 节点存在=关闭 (waitOnBarrier checkExists==null 判定 DistributedBarrier.java:108); setBarrier/removeBarrier 幂等 (L63-82); watcher → postSafeNotify (L42-47)
- **DistributedDoubleBarrier** (DistributedDoubleBarrier.java:293): enter — 创建 EPHEMERAL+UUID 成员节点 (L115-132) → getChildren 计数 ≥ memberQty → 创建 ready 节点 (L260-292); leave — 删自己 + **反序等待: 最小 watch 最大, 其他 watch 最小** (L216-227); connectionLost 显式抛 ConnectionLossException (L183-185)
- memberQty 是**阈值非上限** (DistributedDoubleBarrier.java:57-61 注释: "more than memberQty can enter"); ready 节点创建后先到者不再阻塞后到者 (L120, L126)
关键设计 (q4): **成员节点 EPHEMERAL 保崩溃退场** — 成员死了 barrier 不死锁; 双屏障 leave 的"两端收缩" watch 链让事件传播最短; 显式 connectionLost 让调用方感知临时节点已丢。 [模式: 存在性+计数]

### 7. QueueSharder — 10K 节点上限的扩容

场景: 单队列过万节点会怎样?
源码路径:
- 动机: ZK 传输层限制单队列 ~10K 节点 (QueueSharder.java:53-59 类注释)
- **LeaderLatch 保证唯一扩容器** (QueueSharder.java:76,104); checkThreshold 周期检查 (QueueSharder.java:220-256): ≥ 阈值 → 移出 preferred; ≤ 一半 → 加回; 仅 leader 建新分片 (L239-251); getInitialQueues (L193-203)
- 默认值: 10000/30000ms/10 (QueueSharderPolicies.java:35-37)
关键设计 (q2): **扩容是 leader 的专属职责** — 避免多实例同时看到超阈值重复建分片; 这是 C-2 选举的配方级应用。 [模式: 分片+选举]

## 代码类型
Architecture (分布式原语)

## 负面空间 — Curator 队列刻意不做的事

- **不做 ack/确认**: 无锁模式先删后消费 = at-most-once (Kafka 对照: ack 后才提交 offset)
- **不做消息持久化重放**: 消费即删, 无 replay
- **不做多消费者负载均衡/分区**: 全部消费者竞争同一批节点 (无消费组概念)
- **不做积压监控/告警**: 队列深度要自己 getChildren 数
- **不做跨队列事务消费**: requeue 事务仅单节点
- **不做 TTL 数据面过期**: 延迟队列只延迟消费时机, 节点一直存在

→ 引出: 缓存配方怎么把"watch 流"变"一致性缓存"? → C-5 缓存与监听
