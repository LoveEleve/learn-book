# Ch6 Channel 层 + Scheduled + Progressive — I/O Future 的专属扩展

> Cluster C: 10 KPs | 依赖 §6.2 | §6.2 → §6.3

### 1. ChannelFuture — I/O 操作的 Future 绑定

场景: `channel.write(msg)` 返回的 Future 是关于"这个写操作是否成功"的——和 `alloc.buffer()` 返回的 Future 不同——后者的结果是一个 ByteBuf。Channel I/O 操作不返回值(Void)——所以 ChannelFuture 是 `Future<Void>` 的子类型。

源码路径: `ChannelFuture.java:165` — `extends Future<Void>`。`channel()` 返回关联 Channel——"属于哪个 Channel 的 I/O Future" 绑定。三态机: `Uncompleted → Completed{success, failure, cancelled}`(ChannelFuture.java:42-59)。两个 timeout 混淆: `await(10, SECONDS)` 是等待时长(等待 Future 完成, 在调用方), 不同于 I/O 超时(connect timeout/read timeout, 在 ChannelOption 中配置)(ChannelFuture.java:122-164)。

关键设计: ChannelFuture 的 `channel()` 绑定把 Promise 和 Channel 的生命周期连接——当 Channel 关闭时, 所有未完成的 ChannelFuture 被 fail。`await()` 的超时不是 I/O 超时——这是 NIO 新手最常混淆的概念: `future.await(5000)` 是"等待至多 5 秒让操作完成", 不是"5 秒后操作超时"。I/O 超时通过 `ChannelOption.CONNECT_TIMEOUT_MILLIS` 在 Channel 层设置。

数据流: `channel.connect(addr)` → 返回 ChannelFuture → `future.addListener(f -> { if(f.isSuccess()) startRead(); })` → 连接成功 → EventLoop 调 `promise.setSuccess()` → listener 被通知 → `startRead()` 读取数据。

### 2. DefaultChannelPromise — FlushCheckpoint + executor 后退

场景: `channel.write(msg, promise)` — write 完成后需要通知 promise。但这个 promise 的 listener 应该在哪个线程上执行? 应该在 Channel 的 EventLoop 线程——因为 listener 可能操作同一个 Channel(如 `channel.close()`)。

源码路径: `DefaultChannelPromise.java:30` — `implements FlushCheckpoint`——参与 Channel flush 协调。`DefaultChannelPromise.java:56-64` — `executor()` 后退: `super.executor() == null` → `channel().eventLoop()`——通知在正确的 EventLoop 上执行。`DefaultChannelPromise.java:156-161` — `checkDeadLock` 条件化: `channel().isRegistered()` 才检查 await 死锁——未注册 Channel 的 await 安全。`DefaultChannelPromise.java:142-148` — `FlushCheckpoint` 接口: `flushCheckpoint()/flushCheckpoint(long)` getter/setter——`ChannelFlushPromiseNotifier` 用 checkpoint(monotonic counter) 确定哪些 flush 操作已完成。

关键设计: executor 后退机制让 Promise 知道"我的 listener 应该在哪个线程上运行"——`DefaultChannelPromise` 通过 `channel().eventLoop()` 回退, `DefaultPromise` 通过构造函数传入的 EventExecutor。`FlushCheckpoint` 的 monotonic counter 是一个简单但强大的设计——不同时创建 promise 的写操作(如 write(A) 5ms 后 write(B)), 它们的 checkpoint 分别是 counter+size(A) 和 counter+size(A)+size(B)——notifier 按 FIFO 顺序按数量通知。

数据流: `channel.write(msg, promise)` → DefaultChannelPromise(channel=this.channel, checkpoint=nextCheckpoint) → flush→`ChannelOutboundBuffer.addFlush()` → ChannelFlushPromiseNotifier.notifyPromises0() → checkpoint 匹配 → `promise.trySuccess()` → listener 在 `channel().eventLoop()` 上执行。

### 3. ScheduledFutureTask — fixed-rate vs fixed-delay 双调度

场景: `eventLoop.scheduleAtFixedRate(() -> heartbeat(), 0, 30, SECONDS)` ——每 30 秒发送心跳: 是每 30 秒开始一次(不管上一次是否完成)还是上一次完成后隔 30 秒再开始? fixed-rate 是前者, fixed-delay 是后者。

源码路径: `ScheduledFutureTask.java` — 四重继承: `extends PromiseTask extends DefaultPromise implements ScheduledFuture, Comparable`——桥接 Netty+JDK+优先级队列。`period > 0` fixed-rate: `deadlineNanos += period`——不受执行时间影响, 追赶式(如果心跳花了 5 秒才完成, 下一次 deadline 仍是 30 秒后, 不会变成 35 秒)。`period < 0` fixed-delay: `deadlineNanos = nanoTime() - period`——从上次执行完成开始算(心跳完成后的 35 秒)。`compareTo` 优先比 deadlineNanos, 同期限用 id 打破平局——PriorityQueue 排序。

关键设计: fixed-rate 的"追赶"行为——如果一次执行花费了比 period 更长的时间(如 heartbeat 花了 40 秒而 period 是 30 秒), `deadlineNanos += period` 会让下一次立即执行(deadline 已过)。这避免了"慢任务导致定时器永久滞后"的问题(fixed-delay 会有这个陷阱)。`period != 0` 时的 reschedule——`run()` 完成后→`reschedule()`→submit 回到优先级队列——实现周期性执行。

数据流: `scheduleAtFixedRate(heartbeat, 0, 30, SECONDS)` → 初始化 `deadlineNanos = nanoTime() + 30s` → `run()`(heartbeat 执行 5s)→`deadlineNanos += 30s`(下次=35s后)→`reschedule()`→submit 回 PriorityQueue→下次 tick: `deadlineNanos <= now`→执行。

### 4. ProgressivePromise — 进度追踪

场景: 一个大文件上传(1GB)——你不需要等全部 upload 完成才给用户反馈。每上传 10% 通知一次进度——progressive listener 收到 operationProgressed。

源码路径: `ProgressivePromise.java:21` — `extends Promise<V>, ProgressiveFuture<V>`——可写的进度 Promise。`setProgress(long progress, long total)` — total=-1 未知总量, progress 是累计值非增量。`tryProgress` — done 后静默返回 false(vs setProgress 抛异常)(DefaultProgressivePromise.java:56-68)。`GenericProgressiveFutureListener.operationProgressed(F, progress, total)`——独立于完成回调, DefaultPromise 按 progressiveSize 分发(DefaultFutureListeners 维护独立的 progressive size 计数)。

关键设计: total=-1 是"未知总量"的 sentinel 值——此时的 validation 只检查 progress>=0(不检查 progress<=total, 因为 total 未知)。setProgress vs tryProgress 的语义差异: set 在 done 后抛异常(进度应该在完成前报告), try 静默失败(允许在 done 后的清理代码中调用 tryProgress 而不抛异常)。

数据流: `uploadFile(1GB)` → 每 100MB: `promise.setProgress(uploaded, 1024*1024*1024)` → listener: `operationProgressed(f, 104857600, 1073741824)` → 进度条更新(10%) → 全部写完: `promise.setSuccess()` → 完成。

### 核心悬念

**"Ch6 的 Promise/Future 让 EventLoop 的所有操作都是异步可等待的——register→ChannelFuture, bind→ChannelFuture, write→ChannelFuture。但 Promise 本身是 Future 的子类型——addListener 在同一个 Future 对象上既可以被 EventLoop 调用(写端, setSuccess), 也可以被业务代码调用(读端, sync/addListener)。Ch7 Pipeline 的 ChannelHandlerContext 让 Fire* 事件()和 write/flush 事件()在 Promise 上收敛——Handler 不需要知道上游 Handler 是同步完成还是异步完成, 都用 Future 来等待。"**

→ 引出 Ch7 Pipeline — Promise 让每步异步操作都有了可等待的结果。但 Netty 如何把这些异步操作串成一个处理链? ChannelPipeline 的责任链模式将入站/出站 Handler 串联成流⽔线——HeadContext/TailContext 的双向尾处理回答了"事件在管道中如何传播"。
