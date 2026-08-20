# J-6 并发模型与定时器基础设施 — 知识规划 (KP)

> 域级: 🔴 A (Hub 升级: 被 J-1~J-5 全部核心路径依赖 + 算法决策密度) | 模块: util/concurrent (14 文件) + util/timer (7 文件) + util/RepeatedTimer (295) + util/SegmentList (459) + util/ThreadPoolUtil (283)
> 日期: 2026-08-15 | 版本: 1.4.1 | **REVIEW 扩域**: 初判误将 util/ 83 文件当"工具层"排除, 补跑 00 §3 设计决策测试后新增

## 一、机制提取 (逐源)

### M1 HashedWheelTimer 时间轮 (util/timer/HashedWheelTimer 762)
- **三态 workerState** (L66-70): INIT/STARTED/SHUTDOWN, AtomicIntegerFieldUpdater CAS (L58-61)
- **wheel 桶数组 + mask** (L72-76): ticksPerWheel 为 2 的幂 → 取模优化 (index = tick & mask); tickDuration 单位 tick
- 引用论文 (L38-41): Varghese & Lauck "Hashed and Hierarchical Timing Wheels"
- 时间轮语义: 每个 bucket 是链表, 相同剩余 tick 的任务挂同桶; worker 线程推进指针
- 与 Netty HashedWheelTimer 同源 (类注释)

### M2 RepeatedTimer 重排定时器 (util/RepeatedTimer 295)
- **触发后自动重排** (L83-107): run() → onTrigger() → lock 内 timeout=null + schedule()
- restart/reset/stop (L162-274): restart 立即重排; reset(ms) 改周期重排
- **adjustTimeout 每次调度前调用** (L187) — J-1 选举随机化 [1000,2000) 的实现点
- 基于 HashedWheelTimer (L57); destroy 后回收
- J-1 四个定时器 (vote/election/stepDown/snapshot) 全部是其子类实例

### M3 RaftTimerFactory/DefaultTimer (util/timer)
- RaftTimerFactory 抽象 (DefaultRaftTimerFactory): createTimer(dispatcherThreads) — 定时器工厂 SPI
- DefaultTimer: Timer 接口实现; timerPoolSize = cpus*3 上限 20 (NodeOptions.java:131); sharedTimerPool 可共享

### M4 MpscSingleThreadExecutor (util/concurrent 401)
- MPSC (多生产者单消费者) 语义: 多个线程 execute, 单工作线程串行消费
- **三态 CAS** (L46-48): STATE_UPDATER (INIT/STARTED/SHUTDOWN); execute = addTask + startWorker + wakeupForTask (L137-141)
- 底层 ThreadPerTaskExecutor (L76) + 任务队列
- shutdownHooks (L149): 关闭钩子经同一队列执行 — 防"关后任务丢失"
- 使用面: J-5 AppendEntriesRequestProcessor per-peer 执行器 (L235-262); J-2 Replicator 响应处理

### M5 LongHeldDetectingReadWriteLock (util/concurrent 153)
- ReentrantReadWriteLock 子类 RwLock (L69) + LongHeldDetectingLock 包装 (L91)
- **长持锁检测**: 持锁超过阈值打警告 — NodeImpl.writeLock/readLock 的底座 (NodeImpl.java:229-254 可告警)
- 诊断价值: 定位"哪个线程长时间持锁"的线上排查

### M6 FixedThreadsExecutorGroup (util/concurrent)
- 线程组 + **ExecutorChooser 轮询选择** (DefaultFixedThreadsExecutorGroup L34-44): 任务分发到组内线程
- ExecutorChooserFactory: DefaultExecutorChooserFactory (round-robin 或幂等选择)
- 使用面: 快照 writeExecutor (J-5 L300-304 默认 core=cpus/max=cpus*3)

### M7 SegmentList 分段列表 (util/SegmentList 459)
- **每段 128 元素** (SEGMENT_SHIFT=7, SEGMENT_SIZE=128, L52); ArrayDeque<Segment> (L54)
- **firstOffset 首段缓存偏移** (L59): 首段删除时的 O(1) 头指针移动, 不搬运数组
- **estimatedBytes 内存预算** (L63): EstimatedSize 接口估算, 软限制 (L247-249)
- Segment Recyclers 复用 (L85, 16382/128)
- removeFromFirstWhen/removeFromLastWhen (L187/L205)
- **使用面**: LogManagerImpl 内存日志 (J-2 L96) + BallotBox pendingMetaQueue (J-2 L55) + ClosureQueue

### M8 ThreadPoolUtil/ThreadPoolsFactory (util/ThreadPoolUtil 283)
- 线程池工厂: newThreadPool (core/max/queue 可配) + 命名; ThreadPoolsFactory 全局注册管理
- 使用面: 各域 runClosureInThread (NodeImpl/FSMCaller 错误回调)

## 二、聚合与分级

| 机制 | 级别 | 理由 |
|---|---|---|
| M1 时间轮 | P1 | 经典算法; 定时器底座 |
| M2 重排语义 | P1 | J-1 选举随机化实现点 |
| M4 MPSC | P1 | pipeline 并发核心 |
| M7 分段列表 | P1 | 内存日志数据结构 |
| M5 锁检测 | P2 | 诊断设计 |
| M6 线程组 | P2 | 分配策略 |
| M3/M8 工厂 | P2 | 装配面 |

## 三、负面空间

- **时间轮不做分层**: 单层时间轮, 无层级级联 (论文的 hierarchical 变体未实现)
- **不做工作窃取**: 线程组轮询分配, 无 steal
- **不做任务取消回调**: Timeout.cancel 后无监听
- **不做锁检测自动修复**: 只告警不干预
- **不做无锁化 SegmentList**: 头偏移优化但非无锁
