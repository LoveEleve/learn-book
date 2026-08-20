# J-6 并发模型与定时器基础设施 — 引擎室: 时间轮、MPSC 与分段列表

> 前置: [[J-1-RAFT核心循环]] (RepeatedTimer 定时器使用) + [[J-2-日志复制]] (SegmentList 内存日志/MPSC 响应) + [[J-3-快照压缩]] (SnapshotMeta 表 SegmentList) + [[J-5-存储与RPC]] (per-peer 执行器/SPI) | 引出: 阶段5 Nacos (线程模型对照) | 对照: Netty HashedWheelTimer + JDK ScheduledThreadPoolExecutor
> 🔴 A (Hub) | 8 KP | [模式: 时间轮 + MPSC + 分段]
> Pass 2 闭环: q1(时间轮) q2(重排) q3(MPSC) q4(分段)

**读者处境**: 前 5 域里到处是"定时器""单线程执行器""内存日志"——它们的地基长什么样? 为什么选举超时能随机化? 为什么 appendEntries 能多线程发?

### 1. HashedWheelTimer — 时间轮的 1 秒精度

场景: 1000 个定时任务, 为什么不用 ScheduledThreadPoolExecutor 逐个排?
源码路径:
- **三态 workerState** (HashedWheelTimer.java:66-70): INIT/STARTED/SHUTDOWN, CAS 迁移 (L58-61)
- **wheel 桶数组 + mask** (L72-76): ticksPerWheel = 2 的幂, 定位 = tick & mask 取模; tickDuration 定刻度
- 引用论文 (L38-41): Varghese & Lauck "Hashed and Hierarchical Timing Wheels"
关键设计 (q1): **O(1) 插入/删除, 刻度内批量到期** — 时间轮把"每个任务一个线程/一个排序结构"变成"指针轮转 + 桶链表"; 精度 = tickDuration, 超时任务允许 tick 级误差。 [模式: 桶化调度]

### 2. RepeatedTimer — 触发即重排

场景: 选举定时器为什么能"每次超时都随机"? 心跳定时器为什么能"触发后自动续"?
源码路径:
- **run() 自动重排** (RepeatedTimer.java:83-107): onTrigger() → lock 内 timeout=null + schedule() — 触发完成才排下一次
- restart/reset/stop (L162-274); **adjustTimeout 每次调度前调用** (L187)
- J-1 的随机化实现点: adjustTimeout → randomTimeout [1000,2000) (NodeImpl.java:893-895)
关键设计 (q2): **"完成才重排"防重叠触发** — 触发中不再触发; 随机化窗口把投票风暴打散 (J-1 呼应)。 [模式: 自重启定时器]

### 3. MpscSingleThreadExecutor — 多写单读的秩序

场景: 20 个线程同时给一个 peer 发 AppendEntries 响应, 谁保证顺序?
源码路径:
- **三态 CAS** (MpscSingleThreadExecutor.java:46-48); execute = addTask + startWorker + wakeupForTask (L137-141)
- 底层 ThreadPerTaskExecutor (L76); shutdownHooks 经同一队列 (L149) — 防"关后任务丢失"
- 使用面: AppendEntriesRequestProcessor per-peer 执行器 (AppendEntriesRequestProcessor.java:235-262); Replicator 响应处理
关键设计 (q3): **MPSC = 多生产者的顺序保证** — 写方任意线程, 消费方单线程严格 FIFO (需队列保证); 三态 CAS 防"关后提交"。 [模式: 单消费者队列]

### 4. LongHeldDetectingReadWriteLock — 会告警的锁

场景: 线上 NodeImpl 卡住了, 怎么知道谁拿着锁?
源码路径:
- ReentrantReadWriteLock 子类 (LongHeldDetectingReadWriteLock.java:69) + 检测包装 (L91)
- **NodeImpl.writeLock/readLock 的底座** (NodeImpl.java:229-254): NodeReadWriteLock 继承之; 阈值 = 系统属性 jraft.node.detecting.lock.max_blocking_ms_to_report (**默认 -1 = 关闭**, L232); 触发时 warn + 记录 metrics "node-lock-blocked" (L242-250)
关键设计 (q4): **锁长持 = 死锁/饥饿的前兆** — 检测器在持有超阈值时打警告定位线程栈; 诊断而非干预。 [模式: 可观测锁]

### 5. FixedThreadsExecutorGroup — 轮询分发

场景: 快照写线程池的线程怎么分配任务?
源码路径:
- 线程组 + ExecutorChooser (DefaultFixedThreadsExecutorGroup.java:34-44); ExecutorChooserFactory 轮询/幂等
- 使用面: 快照 writeExecutor (RocksDBSegmentLogStorage.java:300-304)
关键设计 (q3): **固定组 + 轮询 = 确定性分发** — 任务均匀散到 N 线程; chooser 抽象允许替换策略。 [模式: 线程组]

### 6. SegmentList — 128 个一格的"分页数组"

场景: 内存日志 100 万条, 头删尾加, 用什么结构?
源码路径:
- **每段 128 元素** (SegmentList.java:52); ArrayDeque<Segment> (L54)
- **firstOffset 首段缓存偏移** (L59): 首段内删除 O(1) 移动头指针, 不搬数组
- **estimatedBytes 内存预算** (L63): EstimatedSize 接口 → LogManager 软限制背压 (L247-249)
- Segment Recyclers 复用 (L85); removeFromFirstWhen/LastWhen (L187/L205)
- 使用面: LogManagerImpl 内存日志 (LogManagerImpl.java:96) + BallotBox pendingMetaQueue (BallotBox.java:55)
关键设计 (q4): **分段 = 删除不搬移** — 普通 ArrayList 头删 O(n); 分段后头删仅换段/移偏移 O(1); 预算接口让 LogManager 知道"内存日志占了多少"以做背压。 [模式: 分段数组]

### 7. 定时器工厂 — 一套 Timer 抽象

场景: 所有定时器怎么统一创建/共享?
源码路径:
- RaftTimerFactory (DefaultRaftTimerFactory): createTimer(dispatcherThreads); DefaultTimer 实现 Timer 接口
- timerPoolSize = cpus*3 上限 20 (NodeOptions.java:131); sharedTimerPool 可共享
关键设计 (q2): **工厂 + 共享池** — 全局一套定时器线程池, 节点间共享 (多 group 部署省线程); 可替换实现。 [模式: 工厂抽象]

### 8. 全景 — 地基如何撑起整座楼

场景: 前 5 域的哪些机制用到了这些地基?
源码路径:
- J-1: RepeatedTimer × 4 (选举随机化/心跳/快照) + NodeImpl 可告警锁
- J-2: SegmentList (内存日志/票箱) + MpscSingleThreadExecutor (流水线响应)
- J-3: SegmentList (SnapshotMeta 表) + FixedThreadsExecutorGroup (writeExecutor)
- J-5: Mpsc per-peer 执行器 + ThreadPoolUtil + 时间轮 (连接超时)
关键设计 (q1): **并发地基是全项目的隐藏依赖** — 每个域的"单线程串行""随机超时""内存日志"最终都落在这 5 个结构上; 这就是 Hub 域的地位。 [模式: 基础设施层]

## 代码类型
Architecture (基础设施层)

## 负面空间 — JRaft 并发基础设施刻意不做的事

- **不做分层时间轮**: 单层桶, 无 hierarchical 级联 (超长任务会绕轮多圈)
- **不做工作窃取**: 线程组轮询, 无 steal (对照 ForkJoinPool)
- **不做无锁分段列表**: 头偏移优化但整体加锁
- **不做锁检测干预**: 只告警不自动降级
- **不做任务优先级**: 时间轮桶内 FIFO

→ 引出: 阶段5 Nacos 的 JRaftProtocol 怎么复用这套地基?
