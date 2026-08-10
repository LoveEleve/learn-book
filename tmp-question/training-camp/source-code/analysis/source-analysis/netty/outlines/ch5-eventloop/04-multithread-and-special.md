# Ch5 多线程模型与特殊 EventLoop

> Cluster E: 8 KPs | 依赖 §5.3 epoll | §5.3 → §5.4

### 1. MultithreadEventLoopGroup — CPU*2 个 EventLoop 编组

场景: 服务端 8 核 16 线程——创建多少个 EventLoop? 需要足够多来分散 Channel 的负载, 但又不能太多导致线程上下文切换成本超过 IO 等待成本。

源码路径: `MultithreadEventLoopGroup.java:37-46` — `DEFAULT_EVENT_LOOP_THREADS = Math.max(1, Runtime.getRuntime().availableProcessors() * 2)`——默认线程数=CPU 核数×2, 系统属性 `io.netty.eventLoopThreads` 可覆盖。nThreads=0→自动选择 CPU*2(MultithreadEventLoopGroup.java:51-53)。线程使用 `Thread.MAX_PRIORITY` 最高优先级(MultithreadEventLoopGroup.java:72-74)。`newChild(Executor, Object... args)` 抽象工厂——子类创建具体的 EventLoop(MultithreadEventLoopGroup.java:82)。

关键设计: CPU*2 不是"2倍 = 2线程处理 1核"——而是在"1线程可能在等待 IO(select 阻塞)→此时其他线程处理任务"的掩盖下, 通过 oversubscription 提升 CPU 利用率。每个 EventLoop 线程在 `select()` 阻塞时是不消耗 CPU 的——所以可以有超过 CPU 核数的线程数, 等待 select 时它们都在内核态睡眠。

数据流: `new MultithreadEventLoopGroup()` → `new Thread[nThreads=16]` → `for(i=0; i<16; i++)` → `children[i] = newChild(executor, args)` → 每个 child 是独立 EventLoop → started → 16 个 `run()` 并行循环。

### 2. PowerOfTwo Chooser — O(1) 轮询

场景: `group.next().register(channel)` ——哪个 EventLoop 分配这个新 Channel? 最简单的轮询: `children[idx++ % n]`。但 `%` 是除法指令——数学上的昂贵操作。当 children 数量是 2 的幂——可以用位运算。

源码路径: `MultithreadEventExecutorGroup` — 当 `children.length & (children.length - 1) == 0`(2 的幂) → `PowerOfTwoEventExecutorChooser`; 否则 → `GenericEventExecutorChooser`。`PowerOfTwoEventExecutorChooser.next()`: `children[idx.getAndIncrement() & (children.length - 1)]`——位与替代取模, 无除法指令。`GenericEventExecutorChooser.next()`: `children[Math.abs(idx.getAndIncrement() % children.length)]`——标准取模。

关键设计: 2 的幂优化背后的数据——CPU 对 `&` 操作是单周期指令, 对 `idiv` 是 30-90 周期。在高频 next()(每次新连接接受时调用)中, 这个优化累积了显著的 CPU 节省。为什么用 `idx.getAndIncrement()` 而非 `idx.incrementAndGet()`? 因为返回的是递增量**之前的**值——从 0 开始轮询, 而非从 1 开始。

数据流: `group.next()` → `chooser.next()` → `children[(idx++) & 15]` → EventLoop → `eventLoop.register(channel)` → Channel 绑定到该 EventLoop(永不切换)。每个 Channel 从生到死都在同一个 EventLoop 上——线程安全不需要任何锁。

### 3. MultiThreadIoEventLoopGroup — IoHandlerFactory 工厂注入

场景: MultithreadEventLoopGroup 只管"有几个线程"——但每个线程怎么执行 IO? NioEventLoop 需要 NioIoHandler(epoll), EpollEventLoop 需要 EpollIoHandler(kqueue)。这个差异通过 IoHandlerFactory 注入。

源码路径: `MultiThreadIoEventLoopGroup.java:35` — `extends MultithreadEventLoopGroup implements IoEventLoopGroup`。`newChild(executor, args)` — 从 `args[0]` 提取 `IoHandlerFactory` → 创建 `new SingleThreadIoEventLoop(this, executor, ioHandlerFactory, maxPendingTasks, rejectedHandler, maxTaskProcessingQuantumMs)`(MultiThreadIoEventLoopGroup.java:189-213)。`combine(IoHandlerFactory, Object... args)` — 将工厂前置插入 varargs——传给父类构造函数(MultiThreadIoEventLoopGroup.java:220-227)。

关键设计: IoHandlerFactory 的注入让 EventLoop 的线程模型和 IO 模型完全解耦——NioIoHandler(Java NIO epoll)、EpollIoHandler(JNI epoll)、KQueueIoHandler(JNI kqueue)、IoUringIoHandler(Linux IoUring)——都共享同一个 SingleThreadIoEventLoop 的 run()→runIo()→runAllTasks() 框架。NioEventLoop 是旧版, 在新的架构中变成了 @Deprecated 薄壳——实际的 IO 逻辑全部移到 NioIoHandler(§5.1)。

数据流: `new NioEventLoopGroup(nThreads)` → `new MultiThreadIoEventLoopGroup(nThreads, NioIoHandler.newFactory(...))` → `combine(factory, args)` → `MultithreadEventLoopGroup` 构造 → children[i] = `new SingleThreadIoEventLoop(this, executor, factory)`。

### 4. DefaultEventLoop / ManualIoEventLoop — 两种特殊模型

场景: 有的任务不需要 IO——纯 CPU 计算(加密/解密/JSON parse)。DefaultEventLoop 不需要 Selector——`run()` 就是 `takeTask()→runTask()→updateLastExecutionTime()→confirmShutdown()`, 无 IO。ManualIoEventLoop 用于测试——测试线程手动调用 `runNow()` 驱动 IO。

源码路径: `DefaultEventLoop.java:50-62` — `run()` 纯任务循环: 没有 selector/select/processSelectedKeys。`DefaultEventLoop.java:42-43` — `addTaskWakesUp=true`——任务入队时自动唤醒。`ManualIoEventLoop.java:42-49` — 不自行创建线程——用户通过 `run()/runNow()` 手动驱动。四态 AtomicInteger: `STARTED(0)→SHUTTING_DOWN(1)→SHUTDOWN(2)→TERMINATED(3)` 不可逆(ManualIoEventLoop.java:54-57)。`confirmShutdown()` — quietPeriod 内无 task 提交→确认关闭; 超时强制(ManualIoEventLoop.java:559-608)。

关键设计: ManualIoEventLoop 的"用户持有线程"模型是为测试设计的——让 EventLoop 的单线程行为可以被外部线程同步控制: `manualLoop.run()` 执行一轮 IO+task, `manualLoop.runNow()` 不阻塞。DefaultEventLoop 的纯 task 模式是用在不需要 Selector 的场景——例如 `LocalChannel`(进程内通信, 不需要网络 IO)中的 EventLoop。

数据流: `DefaultEventLoop.run()` → takeTask(非阻塞 poll) → runTask → hasTasks?→下一轮 → confirmShutdown → stop。`ManualIoEventLoop.runNow()` → lazy init IoHandler → `handler.run(nonBlockingContext)` + `runAllTasks(timeoutNanos)` → 返回。

### 核心悬念

**"Ch5 的 EventLoop 架构——自包含设计(§5.1)、SelectStrategy 三态(§5.2)、epoll 三层防护(§5.3)、多线程编组(§5.4)——回答了 '数据在什么时候被读写'。但每个 Channel 是异步的——register/bind/connect/write/flush 全部返回 ChannelFuture。Ch6 Promise/Future 用 GenericFutureListener 的回调和 cause 传播机制连接了这些异步操作——让 '先 A 后 B' 的串行逻辑在异步模型下工作。"**

→ 引出 Ch6 Promise/Future — EventLoop 的所有操作都是异步的——register/bind/connect/write 返回 ChannelFuture。Ch6 的 DefaultPromise.result 单字段 5 态编码、addListener0 渐进升级、sync/await 的死锁检测——是异步操作链的粘合剂。
