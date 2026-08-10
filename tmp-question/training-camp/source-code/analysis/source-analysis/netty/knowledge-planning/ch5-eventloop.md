# Ch5 Netty EventLoop — 知识规划

> 来源: 25 源文件 | ~2100 行 | transport/src/main/java/io/netty/channel/
> 基线: Ch4 ByteBuf 回答了 "数据在哪" — Ch5 回答 "数据什么时候被读写"

---

## 01 提取 — 逐源映射

### 01.1 核心接口 (3 文件 — 12 KPs)

#### EventLoop.java + EventLoopGroup.java + SelectStrategy.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| EventLoop.java:27 | **EventLoop 同时继承 OrderedEventExecutor 和 EventLoopGroup** — 自包含设计，next() 返回自身 | High |
| EventLoop.java:21-25 | **1:N 关系**: 一个 EventLoop 处理多个 Channel | High |
| EventLoopGroup.java:25 | **三层继承**: ExecutorGroup→EventExecutorGroup→EventLoopGroup | High |
| EventLoopGroup.java:30,36 | **next()/register(Channel) 是核心差异化方法** — register 返回 ChannelFuture 异步 | High |
| SelectStrategy.java:31-39 | **三态常亮**: SELECT=-1(阻塞), CONTINUE=-2(跳过), BUSY_WAIT=-3(轮询) | High |
| SelectStrategy.java:51 | **calculateStrategy(selectSupplier, hasTasks)**: 有任务→selectNow+CONTINUE, 无任务→SELECT | High |
| SelectStrategy.java:46-49 | **返回值语义**: 负数控制行为, ≥0 表示就绪 Channel 数 | High |

### 01.2 抽象层 (2 文件 — 14 KPs)

#### SingleThreadEventLoop.java + AbstractEventLoop.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractEventLoop.java:24 | **extends AbstractEventExecutor implements EventLoop** | High |
| AbstractEventLoop.java:38-39 | **next() 返回自身** — 单实例即 Group | Medium |
| SingleThreadEventLoop.java:36-37 | **DEFAULT_MAX_PENDING_TASKS** 系统属性可配, 默认 Integer.MAX_VALUE, 最低 16 | High |
| SingleThreadEventLoop.java:39 | **tailTasks 独立队列** — 事件循环迭代结束时统一执行 | High |
| SingleThreadEventLoop.java:112-121 | **register() → channel.unsafe().register(this, promise)** — 真正执行者是 Channel.Unsafe | High |
| SingleThreadEventLoop.java:137-150 | **executeAfterEventLoopIteration(Runnable)**: 加入 tailTasks, 关闭时拒绝 | High |
| SingleThreadEventLoop.java:163-166 | **afterRunningAllTasks()**: 执行所有 tailTasks | High |
| SingleThreadEventLoop.java:168-176 | **hasTasks/pendingTasks 包含 tailTasks 计数** | Medium |
| SingleThreadEventLoop.java:200-244 | **ChannelsReadOnlyIterator** — 只读包装, 禁止 remove | Medium |

### 01.3 Io 抽象层 (3 文件 — 26 KPs)

#### IoEventLoop.java + IoEventLoopGroup.java + SingleThreadIoEventLoop.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| IoEventLoop.java:24 | **extends EventLoop + IoEventLoopGroup** — 同层自包含 | High |
| IoEventLoop.java:37,41,44 | **register(IoHandle) / isCompatible / isIoType** — Io 级 API | High |
| IoEventLoopGroup.java:52-65 | **新 IoHandle API**: register(IoHandle) → next().register(handle) | High |
| SingleThreadIoEventLoop.java:39-40 | **maxTaskProcessingQuantum**: 默认 1000ms, 系统属性可配, 最小 100ms | High |
| SingleThreadIoEventLoop.java:45-48 | **canBlock() = !hasTasks() && !hasScheduledTasks()** — 严格阻塞条件 | High |
| SingleThreadIoEventLoop.java:192-205 | **run() 主循环**: initialize → loop{ runIo() → runAllTasks(maxQuantumNs) } while(!confirmShutdown) | High |
| SingleThreadIoEventLoop.java:223-226 | **runIo() = ioHandler.run(context)** — 完全委托 | High |
| SingleThreadIoEventLoop.java:212-215 | **canSuspend()**: 父类条件 + numRegistrations.get()==0 | High |
| SingleThreadIoEventLoop.java:234-261 | **register(IoHandle) 异步**: inEventLoop→同步, 否则 execute 提交 + Promise | High |
| SingleThreadIoEventLoop.java:250-261 | **registerForIo0**: ioHandler.register→numRegistrations++→IoRegistrationWrapper→promise.setSuccess | High |
| SingleThreadIoEventLoop.java:295-324 | **IoRegistrationWrapper**: Decorator, cancel() 时 numRegistrations-- | High |
| SingleThreadIoEventLoop.java:264-266 | **wakeup() → ioHandler.wakeup()** | High |
| SingleThreadIoEventLoop.java:289-293 | **MPSC task queue**: PlatformDependent 多生产者单消费者, 注释 "never calls takeTask()" | High |

### 01.4 多线程模型 (2 文件 — 18 KPs)

#### MultithreadEventLoopGroup.java + MultiThreadIoEventLoopGroup.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| MultithreadEventLoopGroup.java:37-46 | **DEFAULT_EVENT_LOOP_THREADS = CPU*2**: 最小 1, 系统属性可配 | High |
| MultithreadEventLoopGroup.java:51-53 | **nThreads=0 → 自动选择**: CPU*2 | High |
| MultithreadEventLoopGroup.java:72-74 | **Thread.MAX_PRIORITY**: 默认线程工厂最高优先级 | High |
| MultithreadEventLoopGroup.java:82 | **newChild(Executor, args) 抽象**: 子类实现 EventLoop 创建 | High |
| MultiThreadIoEventLoopGroup.java:35 | **extends MultithreadEventLoopGroup implements IoEventLoopGroup** | High |
| MultiThreadIoEventLoopGroup.java:189-213 | **newChild 从 args[0] 提取 IoHandlerFactory** → 创建 SingleThreadIoEventLoop | High |
| MultiThreadIoEventLoopGroup.java:220-227 | **combine() 参数组合**: 工厂前置插入 varargs | High |
| MultiThreadIoEventLoopGroup.java:41-184 | **10+ 构造重载** 覆盖 ThreadFactory/Executor/ChooserFactory 组合 | High |
| (继承) | **PowerOfTwoEventExecutorChooser**: idx&(count-1) 替代取模, O(1) | High |
| (继承) | **AutoScalingEventExecutorChooserFactory**: 支持自动扩缩容 | Medium |

### 01.5 Nio 实现层 (2 文件 — 19 KPs)

#### NioEventLoopGroup.java + NioEventLoop.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| NioEventLoopGroup.java:44 | **@Deprecated** — 重构为 MultiThreadIoEventLoopGroup + NioIoHandler | High |
| NioEventLoopGroup.java:96-98 | **构造注入 NioIoHandler.newFactory(selectorProvider, selectStrategyFactory)** | High |
| NioEventLoopGroup.java:165-167 | **setIoRatio() no-op** — IO ratio 控制权迁移到 IoHandlerContext | High |
| NioEventLoopGroup.java:173-177 | **rebuildSelectors()**: 遍历 children → NioEventLoop.rebuildSelector() — epoll bug 批量修复 | High |
| NioEventLoop.java:44-45 | **@Deprecated** — 核心逻辑全部移至 NioIoHandler | High |
| NioEventLoop.java:49-53 | **双任务队列**: front taskQueue + tail taskQueue, DEFAULT_MAX_PENDING_TASKS | High |
| NioEventLoop.java:76-110 | **register(SelectableChannel)**: 非 Netty Channel 注册, 非 eventLoop 线程→sync 阻塞等待 | High |
| NioEventLoop.java:112-138 | **register0()**: 创建 NioSelectableChannelIoHandle → IoRegistration.submit(NioIoOps) | High |
| NioEventLoop.java:161-172 | **rebuildSelector()**: 委托 NioIoHandler.rebuildSelector0() | High |
| NioEventLoop.java:175-177 | **registeredChannels() = selector.keys().size() - cancelledKeys** | High |

### 01.6 NioIoHandler 核心 (1 文件 — 20 KPs)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| NioIoHandler.java:61 | **CLEANUP_INTERVAL=256**: cancelledKey 累计阈值, 触发 needsToSelectAgain | High |
| NioIoHandler.java:66-88 | **SELECTOR_AUTO_REBUILD_THRESHOLD=512**: epoll 空轮询检测阈值 | High |
| NioIoHandler.java:63-64 | **DISABLE_KEY_SET_OPTIMIZATION**: 禁用 SelectedSelectionKeySet, 回退 Plain | High |
| NioIoHandler.java:143-234 | **openSelector()**: provider.openSelector→反射→SelectorImpl 检查→SelectedSelectionKeySet 注入 | High |
| NioIoHandler.java:186-204 | **Java9+ Unsafe 注入**: objectFieldOffset+putObject 替换 selectedKeys | High |
| NioIoHandler.java:255-302 | **rebuildSelector0()**: 新 Selector→转移 keys→原子替换→关闭旧 Selector | High |
| NioIoHandler.java:318-390 | **DefaultNioRegistration**: SelectionKey attachment → IoRegistration 实现 | High |
| NioIoHandler.java:361-373 | **cancel()**: CAS canceled→key.cancel()→cancelledKeys++→≥256 时 needsToSelectAgain | High |
| NioIoHandler.java:393-416 | **register()**: CancelledKeyException 重试一次 selectNow() | High |
| NioIoHandler.java:419-496 | **run() 主循环**: calculateStrategy→select→processSelectedKeys | High |
| NioIoHandler.java:464-466 | **wakenUp race fix**: select 返回后 if(wakenUp.get())→selector.wakeup() | High |
| NioIoHandler.java:630-722 | **select() deadline-driven**: delayNanos→timeoutMillis→selector.select() | High |
| NioIoHandler.java:697-703 | **epoll bug 检测**: selectCnt≥512→rebuildSelector0()+selectNow()→cnt=1 重置 | High |
| NioIoHandler.java:563-583 | **processSelectedKeysOptimized()**: 直接数组遍历, needsToSelectAgain→reset+重来 | High |
| NioIoHandler.java:527-561 | **processSelectedKeysPlain()**: Iterator 遍历, needsToSelectAgain→selectAgain+重建迭代器 | High |
| NioIoHandler.java:586-597 | **processSelectedKey()**: attachment→DefaultNioRegistration→handle(readyOps) | High |
| NioIoHandler.java:615-619 | **wakeup() CAS 优化**: wakenUp.compareAndSet→selector.wakeup(), 减少昂贵 syscall | High |
| NioIoHandler.java:498-508 | **handleLoopException()**: IOException→sleep(1000) 防 CPU 空转 | Medium |

### 01.7 Selector 优化 (2 文件 — 12 KPs)

#### SelectedSelectionKeySet.java + SelectedSelectionKeySetSelector.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| SelectedSelectionKeySet.java:25-26 | **Array 替代 HashSe**: 消除 iterator 分配+contains/add 哈希开销 | High |
| SelectedSelectionKeySet.java:30-31 | **初始容量 1024**: keys=new SelectionKey[1024] | High |
| SelectedSelectionKeySet.java:35-46 | **add() O(1) 尾部追加**: 无去重, 依赖 Selector 保证不重复; 满时翻倍 | High |
| SelectedSelectionKeySet.java:48-51 | **remove() 永远 false**: selectedKeys 只需遍历+清空 | High |
| SelectedSelectionKeySet.java:95-101 | **reset(start)**: Arrays.fill(keys,start,size,null)+size=0 — 部分重置 | High |
| SelectedSelectionKeySet.java:104-108 | **increaseCapacity()**: 翻倍+arraycopy | High |
| SelectedSelectionKeySetSelector.java:24-30 | **Decorator 包装**: 不改变 Selector API, 仅 select*() 前置 reset | High |
| SelectedSelectionKeySetSelector.java:54-69 | **selectNow/select(timeout)/select()**: 每次 select* 前 reset | High |

### 01.8 特殊 EventLoop (3 文件 — 16 KPs)

#### DefaultEventLoop.java + ThreadPerChannelEventLoop.java + ManualIoEventLoop.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| DefaultEventLoop.java:50-62 | **run()**: 纯任务循环 — takeTask→runTask→updateLastExecutionTime→confirmShutdown | High |
| DefaultEventLoop.java:42-43 | **addTaskWakesUp=true**: 任务入队自动唤醒 | High |
| ThreadPerChannelEventLoop.java:24-25 | **@Deprecated OIO**: 每 Channel 独占线程 | High |
| ThreadPerChannelEventLoop.java:36-47 | **register()**: 绑定 Channel, 失败 deregister | High |
| ThreadPerChannelEventLoop.java:65-91 | **run()**: shutdown→ch.unsafe().close(); ch 未注册→runAllTasks→deregister | High |
| ThreadPerChannelEventLoop.java:93-97 | **deregister()**: 归还到 idleChildren 池可复用 | High |
| ManualIoEventLoop.java:42-49 | **用户持有线程**: 不自行创建线程, runNow/run 手动驱动 | High |
| ManualIoEventLoop.java:54-57 | **四态 AtomicInteger**: STARTED→SHUTTING_DOWN→SHUTDOWN→TERMINATED | High |
| ManualIoEventLoop.java:215-242 | **run(IoHandlerContext, timeout)** 核心: lazy init→handler.run+runAllTasks→设置ThreadExecutorMap | High |
| ManualIoEventLoop.java:559-608 | **confirmShutdown**: quietPeriod 内无 task→确认关闭, 超时强制 | High |

### 01.9 辅助文件 (4 文件 — 14 KPs)

#### NioIoHandle.java + NioIoOps.java + NioSelectableChannelIoHandle.java + EventLoopTaskQueueFactory.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| NioIoHandle.java:25-32 | **IoHandle 的 NIO 子类型**: 暴露 SelectableChannel | High |
| NioIoOps.java:30-60 | **封装 JDK interestOps**: NONE(0)/READ(1)/WRITE(4)/CONNECT(8)/ACCEPT(16) | High |
| NioIoOps.java:63-78 | **EVENTS 预计算缓存**: valueOf() O(1) 查表 | High |
| NioIoOps.java:95-123 | **contains/with/without**: 位操作 API | High |
| NioSelectableChannelIoHandle.java:32-37 | **非 Netty Channel 适配器**: handle→deregister 抽象回调 | High |
| EventLoopTaskQueueFactory.java:37 | **newTaskQueue(maxCapacity)**: 工厂模式, MPSC+容量限制 | Medium |

---

## 01 聚合 — 跨文件汇总

N=25, P-level阈值: P1=≥5文件, P2=3-4文件, P3=1-2文件

### P1 — 全系统共识 (≥5 文件)

| Knowledge Point | 出现文件 |
|----------------|---------|
| **EventLoop 自包含设计 (extends Group + next()=this)** | EventLoop + IoEventLoop + AbstractEventLoop + SingleThreadEventLoop + NioEventLoop + DefaultEventLoop |
| **register(Channel/IoHandle) 异步注册** | EventLoopGroup + IoEventLoopGroup + SingleThreadEventLoop + SingleThreadIoEventLoop + NioEventLoopGroup + NioEventLoop + MultithreadEventLoopGroup + ManualIoEventLoop |
| **run() 主循环: tasks + IO 交替** | SingleThreadIoEventLoop + ManualIoEventLoop + DefaultEventLoop + NioIoHandler + ThreadPerChannelEventLoop |
| **MPSC task queue** | SingleThreadEventLoop + SingleThreadIoEventLoop + NioEventLoop + ManualIoEventLoop + DefaultEventLoop |
| **SelectStrategy 三态 (SELECT/CONTINUE/BUSY_WAIT)** | SelectStrategy + DefaultSelectStrategy + NioIoHandler + AbstractEventLoop |
| **nThreads = CPU*2 默认** | MultithreadEventLoopGroup + MultiThreadIoEventLoopGroup + NioEventLoopGroup + DefaultEventLoopGroup |
| **newChild(Executor, args) 工厂** | MultithreadEventLoopGroup + MultiThreadIoEventLoopGroup + NioEventLoopGroup + DefaultEventLoopGroup |
| **wakeup() 机制** | SingleThreadIoEventLoop + NioIoHandler + ManualIoEventLoop + DefaultEventLoop |
| **isCompatible/isIoType 类型检查** | IoEventLoop + IoEventLoopGroup + IoHandler + NioIoHandler |

### P2 — 局部重要 (3-4 文件)

| Knowledge Point | 出现文件 |
|----------------|---------|
| **tailTasks + executeAfterEventLoopIteration** | SingleThreadEventLoop + SingleThreadIoEventLoop |
| **canBlock/hasTasks/hasScheduledTasks** | SingleThreadIoEventLoop + ManualIoEventLoop + IoHandlerContext |
| **numRegistrations 计数器** | SingleThreadIoEventLoop + ManualIoEventLoop + NioEventLoop |
| **IoRegistrationWrapper (cancel→decrement)** | SingleThreadIoEventLoop |
| **confirmShutdown (quietPeriod+timeout)** | ManualIoEventLoop + SingleThreadEventLoop + DefaultEventLoop |
| **SelectedSelectionKeySet 数组替代 HashSet** | SelectedSelectionKeySet + SelectedSelectionKeySetSelector + NioIoHandler |
| **rebuildSelector/rebuildSelector0** | NioEventLoop + NioIoHandler + NioEventLoopGroup |
| **processSelectedKeysOptimized/Plain** | NioIoHandler + SelectedSelectionKeySet |
| **IoHandlerContext (canBlock/delayNanos)** | SingleThreadIoEventLoop + ManualIoEventLoop |
| **Thread.MAX_PRIORITY 线程工厂** | MultithreadEventLoopGroup + MultiThreadIoEventLoopGroup |

### P3 — 独立 (1-2 文件)

| Knowledge Point | 出现文件 |
|----------------|---------|
| **IO ratio 已废弃** | NioEventLoop + NioEventLoopGroup |
| **ChannelsReadOnlyIterator** | SingleThreadEventLoop |
| **ManualIoEventLoop 四态 AtomicInteger** | ManualIoEventLoop |
| **ThreadPerChannel 双池 (active/idle)** | ThreadPerChannelEventLoop |
| **NioSelectableChannelIoHandle 适配器** | NioSelectableChannelIoHandle |
| **AutoScalingEventExecutorChooserFactory** | MultiThreadIoEventLoopGroup |
| **CLEANUP_INTERVAL=256** | NioIoHandler |
| **DISABLE_KEY_SET_OPTIMIZATION** | NioIoHandler |
| **CancelledKeyException 重试** | NioIoHandler |
| **handleLoopException→sleep(1000)** | NioIoHandler |

---

## 02 深度分类

### 🔴 Deep (承载核心设计决策)

| KP | 为什么🔴 |
|----|---------|
| **EventLoop 自包含设计 (extends Group, next()=this)** | Netty 最优雅的设计 — 一个 EventLoop 既是执行器又是自己的 Group, 避免 Group/EventLoop 的二元复杂性 |
| **SelectStrategy 三态 (SELECT/CONTINUE/BUSY_WAIT)** | 任务感知的选择器调度 — 有任务时跳过阻塞 select 避免饥饿 |
| **run() 主循环: runIo→runAllTasks** | IO 与任务交替执行, maxTaskProcessingQuantum 防止 task 吃光 CPU |
| **SELECTOR_AUTO_REBUILD_THRESHOLD+rebuildSelector0** | epoll 100% CPU bug 的唯一工程解 — 检测空轮询→重建 Selector→迁移 keys |
| **SelectedSelectionKeySet 数组替换 HashSet** | 消除 GC 压力 — add O(1) 无哈希/contains 开销, 遍历无 iterator 分配 |
| **wakeup CAS race fix** | select() 返回后 if(wakenUp.get())→selector.wakeup() — 这是 Netty 最隐蔽的 bug 修复之一 |
| **IoHandlerContext + shouldReportActiveIoTime** | IO ratio 从百分比→实际时间上报, 让上层基于真实 IO 时间自适应 |
| **canBlock = !hasTasks() && !hasScheduledTasks()** | 严格的阻塞条件保证任务不因 IO 等待饥饿 |

### 🟡 Working (有设计决策, 非核心)

| KP | 说明 |
|----|------|
| **nThreads = CPU*2 默认** | 合理的默认值 — 但可被调优 |
| **PowerOfTwoEventExecutorChooser** | idx&(count-1) 替代取模 — 位运算优化 |
| **newChild 工厂 + IoHandlerFactory 注入** | 构造链分层 — 支持依赖注入和测试 |
| **tailTasks + executeAfterEventLoopIteration** | 每轮 IO/tasks 处理完后的收尾机制 |
| **DefaultNioRegistration + CancelledKeyException 重试** | JDK bug 绕过 — register 时可能遇到未 flush 的 cancelled key |
| **NioEventLoop 空心化 → NioIoHandler** | 架构解耦 — EventLoop=调度, IoHandler=IO 操作 |

### 🟢 Surface (机制性了解即可)

| KP | 放在哪 |
|----|-------|
| **DefaultEventLoop** 纯任务执行 | 和 DefaultEventLoopGroup 一起 |
| **ThreadPerChannelEventLoop** OIO 遗留 | 和 DefaultEventLoop 对比 |
| **ManualIoEventLoop** 用户持有线程 | 特殊场景 — 和 DefaultEventLoop 对比 |
| **NioIoOps 位掩码封装** | 和 NioIoHandler select 一起 |
| **NioSelectableChannelIoHandle** 适配器 | 和 NioIoHandler register 一起 |
| **ChannelsReadOnlyIterator** | 和 registeredChannels 一起 |
| **IoRegistrationWrapper** Decorator | 和 registerForIo0 一起 |
| **cleanup=destroy** | 和 run 生命周期一起 |

---

## 03 聚类

### Cluster A: 自包含架构 (6 KPs) — 零前置, 回答 "EventLoop 是什么"

1. EventLoop extends OrderedEventExecutor + EventLoopGroup — 同时是执行器和自己的 Group
2. next() 返回 this — 单实例即 Group
3. EventLoopGroup 三层继承体系
4. register(Channel/IoHandle) 异步语义 + ChannelFuture
5. isCompatible/isIoType 类型检查体系
6. AbstractEventLoop → AbstractEventExecutor 继承链

### Cluster B: 单线程执行模型 (10 KPs) — 依赖 A, 回答 "怎么跑"

1. run() 主循环: initialize → runIo → runAllTasks → confirmShutdown
2. MPSC task queue + newTaskQueue0 纯 poll 模式
3. tailTasks + executeAfterEventLoopIteration
4. afterRunningAllTasks() 尾任务执行点
5. maxTaskProcessingQuantum (默认 1000ms)
6. canBlock = !hasTasks() && !hasScheduledTasks()
7. canSuspend: 父类条件 + numRegistrations==0
8. register(IoHandle) 异步: execute 提交+Promise
9. registerForIo0: ioHandler.register→numRegistrations++
10. IoRegistrationWrapper: cancel→numRegistrations--

### Cluster C: SelectStrategy 与 Selector 优化 (8 KPs) — 依赖 B, 回答 "什么时候等 IO"

1. SelectStrategy 三态: SELECT/CONTINUE/BUSY_WAIT
2. calculateStrategy(selectSupplier, hasTasks): 有任务→selectNow+CONTINUE
3. DefaultSelectStrategy: hasTasks?selectSupplier.get():SELECT
4. Selector 注入: JDK SelectorImpl → Unsafe/反射替换 selectedKeys
5. SelectedSelectionKeySet: Array 替代 HashSet, add O(1)
6. SelectedSelectionKeySetSelector: Decorator, select* 前 reset
7. processSelectedKeysOptimized: 数组直接遍历
8. processSelectedKeysPlain: Iterator 降级路径

### Cluster D: epoll bug 与重建 (10 KPs) — 依赖 C, 回答 "什么时候崩了怎么办"

1. SELECTOR_AUTO_REBUILD_THRESHOLD=512
2. selectCnt 累积检测: selectNow() 返回 0 → selectCnt++
3. selectCnt≥512 → rebuildSelector0()
4. rebuildSelector0: openSelector→转移 keys→原子替换→关闭旧
5. wakeup CAS race fix: 返回后 if(wakenUp.get())→wakeup
6. wakenUp CAS: compareAndSet→selector.wakeup() 减少 syscall
7. deadline-driven select: delayNanos→timeoutMillis
8. CancelledKeyException 重试: register→失败→selectNow flush→重试
9. CLEANUP_INTERVAL=256 + needsToSelectAgain
10. handleLoopException→sleep(1000) 防 CPU 空转

### Cluster E: 多线程与特殊模型 (8 KPs) — 依赖 A, 回答 "怎么编排多个"

1. MultithreadEventLoopGroup: nThreads=CPU*2, Thread.MAX_PRIORITY
2. newChild 工厂: 从 args 提取 IoHandlerFactory
3. PowerOfTwoEventExecutorChooser: idx&(count-1)
4. MultiThreadIoEventLoopGroup: combine() 参数组合
5. NioEventLoopGroup.rebuildSelectors() 批量重建
6. DefaultEventLoop: 纯任务执行, 无 IO
7. ManualIoEventLoop: 用户持有线程, 四态生命周期
8. ThreadPerChannelEventLoop: OIO 遗留, 双池复用

### 教学顺序: A → B → C → D → E
A(架构) → B(单线程) → C(策略+Selector优化) → D(epoll bug+故障恢复) → E(多线程+特殊)
