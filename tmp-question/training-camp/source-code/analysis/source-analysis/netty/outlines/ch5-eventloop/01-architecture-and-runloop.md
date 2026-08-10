# Ch5 自包含架构与单线程执行 — EventLoop 的本质

> Cluster A+B: 16 KPs | 依赖 Ch4 ByteBuf | Ch5 → §5.2

### 1. 自包含设计 — 既是执行器也是自己的 Group

场景: `eventLoop.next()` 返回什么? 如果有一个单独的"选择器"对象——调用方需要维护 Group→EventLoop 的两层关系。`next()` 返回自身——一个 EventLoop 同时实现了 Group 的接口。

源码路径: `EventLoop.java:27` — `EventLoop extends OrderedEventExecutor, EventLoopGroup`——同时继承执行器和 Group。`next() = return this`——单实例即 Group(AbstractEventLoop.java:38-39)。`OrderedEventExecutor` 保证单线程串行顺序——所有提交的任务 happen-before(EventLoop.java:27)。`EventLoopGroup.java:25` — 三层继承: ExecutorGroup→EventExecutorGroup→EventLoopGroup, 逐层增加事件循环语义。`EventLoopGroup.java:30,36` — `next()` 返回 EventLoop(协变返回类型), `register(Channel)` 返回 `ChannelFuture`——注册是异步的。

关键设计: 自包含设计让 EventLoop 和 EventLoopGroup 不是"工厂-产品"的二元关系——而是"一个多线程 Group 包含多个单线程 EventLoop, 每个 EventLoop 自身也是一个单元素 Group"。调用方不需要区分"这是一个 Group 还是一个 EventLoop"——`eventLoop.register(channel)` 和 `group.register(channel)` 签名相同。Netty 为什么不独立这两个类型? 因为每个 EventLoop 天然支持 register()——它不需要 Group 的中间层来分配 Channel。Group 的 register 委托 next().register()——O(1) 级联。

数据流: `group.register(channel) → group.next() 选出某个 EventLoop → eventLoop.register(channel) → channel.unsafe().register(this, promise)` — Channel 绑定到 EventLoop, 后续所有 I/O 操作都在这个 EventLoop 线程上执行。

### 2. run() 主循环 — IO 与 Task 交替

场景: EventLoop 线程被启动后进入无限循环——每次迭代: 先处理 IO(select→process selected keys→read/write), 再处理提交的任务队列(runAllTasks)。用一段量子时间限制任务的执行时长——防止任务饥饿 IO。

源码路径: `SingleThreadIoEventLoop.java:192-205` — `run()`: `initialize()` → loop: `runIo()`(IO) → `runAllTasks(maxQuantumNs)`(tasks) → while(!`confirmShutdown()` && !`canSuspend()`)。`SingleThreadIoEventLoop.java:39-40` — `maxTaskProcessingQuantum` 默认 1000ms, 系统属性 `io.netty.eventLoop.maxTaskProcessingQuantumMs` 可配, 最小值 100ms。`SingleThreadIoEventLoop.java:223-226` — `runIo() = ioHandler.run(context)`——IO 完全委托 IoHandler。`SingleThreadIoEventLoop.java:45-48` — `canBlock() = !hasTasks() && !hasScheduledTasks()`——只有任务队列和定时任务都空了才允许 IO 阻塞(防止任务饥饿)。

关键设计: IO 和 Task 交替执行是 Netty 区别于传统 Reactor 模式的关键——传统 Reactor 要么优先 IO(select→process IO→然后才 task), 要么优先 task(先 task→没有才 select)。Netty 的交替模式 + maxTaskProcessingQuantum 让两者平衡——IO 不会被长 task 饿死(task 有量子限制), task 也不会被 IO 饿死(每轮 loop 都会 runAllTasks)。Ch3 的 Selector select() 和 Ch2 的 Channel read/write 在 runIo() 中被串联——`NioIoHandler.run(context)` 内部: calculateStrategy→select→processSelectedKeys(§5.2-§5.3)。

数据流: `run()` → `runIo()`(select+process read/write, Ch3 NIO) → `runAllTasks(1000ms)`(执行 submit 的任务, Ch4 ByteBuf 的 write/flush/retain/release) → `confirmShutdown()`(优雅停机检查) → 下一轮循环。

### 3. MPSC 任务队列 — 多生产者单消费者无锁

场景: 外部线程 A 调用 `eventLoop.execute(() -> writeAndFlush(data))`——把任务提交到 EventLoop 的任务队列。EventLoop 自己也是生产者(可以 execute 自己的延期任务)。Queue 需要线程安全但避免锁——MPSC。

源码路径: `SingleThreadIoEventLoop.java:289-293` — `newTaskQueue0(maxPendingTasks)` 创建 `PlatformDependent.newMpscQueue()`——MPSC(多生产者单消费者)无锁队列。注释明确"never calls takeTask()"——纯 poll 模式, 不阻塞。`SingleThreadEventLoop.java:36-37` — `DEFAULT_MAX_PENDING_TASKS` 系统属性可配, 默认 Integer.MAX_VALUE, 最低 16。`SingleThreadEventLoop.java:39` — `tailTasks` 独立队列——事件循环迭代结束时统一执行。`SingleThreadEventLoop.java:137-150` — `executeAfterEventLoopIteration(task)` 把 task 加入 tailTasks——关闭时拒绝。

关键设计: MPSC 队列的"纯 poll"设计是关键——EventLoop 不阻塞等待任务(queue.take()), 而是 runAllTasks 中非阻塞 poll + maxQuantum。这意味着任务入队不保证立即执行——需要 wakeup() 通知 EventLoop 有新任务(§5.3)。tailTasks 的"事件循环迭代结束时统一执行"语义——用在 `Channel.flush()` 中: flush 不只是立即写, 而是在本轮 IO+task 全部处理完后, 统一执行所有尾任务(批量发送 pending writes)。

数据流: Thread A: `eventLoop.execute(task)` → MPSC queue.offer(task) → `wakeup()`(通知 EventLoop) → EventLoop: `runAllTasks()` → `queue.poll()` 获取 task → `task.run()` → 完成后 `afterRunningAllTasks()` 执行 tailTasks。

### 4. registerForIo0 — 异步注册到 IoHandler

场景: `channel.register(selector, OP_READ)` ——这个注册必须在 EventLoop 线程执行(Ch3 的教训: Selector 的 register 会阻塞, 必须 asynchronize 到 EventLoop)。外部线程调用 register——需要把注册操作提交到 EventLoop 并返回一个 Promise 等待结果。

源码路径: `SingleThreadIoEventLoop.java:234-261` — `register(IoHandle handle)`: `inEventLoop? registerForIo0(handle, newPromise()) : execute(() -> registerForIo0(handle, promise))`——线程安全注册。`registerForIo0()`: `ioHandler.register(handle)` → `numRegistrations.incrementAndGet()` → 封装为 `IoRegistrationWrapper` → `promise.setSuccess(reg)`(SingleThreadIoEventLoop.java:250-261)。`IoRegistrationWrapper`: Decorator 包装 IoRegistration, `cancel()` 时 `numRegistrations.decrementAndGet()`(SingleThreadIoEventLoop.java:295-324)。

关键设计: register 的异步化保证线程安全——Ch2 的块模式下 Selector.register 会取内部锁(regLock), 多线程同时调 register 可能 blocking each other。Netty 通过 register 始终在 EventLoop 线程执行来避免这种锁争(`inEventLoop()? 直接调 : executor.execute())`。`numRegistrations` 计数器用于 `canSuspend()` 判断——注册数 = 0 才允许挂起(SingleThreadIoEventLoop.java:212-215)。

数据流: External Thread: `eventLoop.register(handle)` → Promise → `execute(() -> registerForIo0())` → EventLoop: `ioHandler.register(handle)` → SelectionKey → `numRegistrations.increment` → `promise.setSuccess(reg)` → External Thread: `promise.sync()` 等待。

### 核心悬念

**"EventLoop 的单线程模型保证了 register→select→read/write→runAllTasks 不需要任何锁——但代价是所有操作必须 EventLoop 线程执行。Ch5 §5.2 的 SelectStrategy 三态机(SELECT/CONTINUE/BUSY_WAIT)决定了 runIo() 在什么时候阻塞等待 IO 事件——有任务时跳过 select 直接 runAllTasks。但 Selector 本身的性能瓶颈——selectedKeys HashSet 的遍历和 Iterator 分配——在 §5.2 的 SelectedSelectionKeySet 中用数组替换 HashSet 解决。"**

→ 引出 §5.2 SelectStrategy 与 Selector 优化 — runIo() 的核心: calculateStrategy 根据 hasTasks 决定 select 还是跳过。SELECT(-1)阻塞, CONTINUE(-2)跳过, BUSY_WAIT(-3)轮询。三态状态机 + SelectedSelectionKeySet 数组替换是 Ch3 NIO 的 selectedKeys 陷阱的 Netty 解法。
