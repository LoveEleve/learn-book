# Ch6 组合器与不可变 Future — 多异步编排与已完成通知

> Cluster B: 10 KPs | 依赖 §6.1 | §6.1 → §6.2

### 1. CompleteFuture — 已完成 Future 的即时通知

场景: `alloc.buffer(0)` 返回 EmptyByteBuf——不需要等待, buffer 已经是"完成的"。类似地, `new SucceededFuture<Void>(null)`——一个永远成功的 Future。`addListener` 应该立即回调而不是等下一轮 EventLoop。

源码路径: `CompleteFuture.java` — `addListener(GenericFutureListener)` 在注册线程同步回调——不进 EventLoop 任务队列。`removeListener` NOOP(不需要维护 listener 列表)。`await()/sync()` 立即返回——Future 已完成。`SucceededFuture.java`: `isSuccess()`=true, `sync()` 返回 this, `getNow()` 返回 result。`FailedFuture.java`: `isSuccess()`=false, `cause()` 非 null, `sync()` 抛 cause, `getNow()`=null。两个都是构造时已确定终态——线程安全, 单例可共享。

关键设计: CompleteFuture 是对"操作立即完成"场景的优化——EventLoop 不需要 schedule 一个 task 来通知 listener——在当前线程直接调用 `listener.operationComplete(future)`。代价: 如果 listener 做了重操作(如写数据库), 它会阻塞当前线程(可能是 EventLoop 线程)——使用 CompleteFuture 的代码需注意 listener 的执行成本。

数据流: `new SucceededFuture<>(result)` → `future.addListener(l)` → `l.operationComplete(this)` (立即, 当前线程) → 调用方 `future.sync()` → 立即返回(await 不阻塞)。`new FailedFuture<>(cause)` → `future.addListener(l)` → `l.operationComplete(this)` → `future.sync()` → throw cause。

### 2. PromiseCombiner — add→finish→aggregate 三阶段

场景: 3 个异步写操作: `write(req1)`, `write(req2)`, `write(req3)`——三个都返回 ChannelFuture。你想在所有 3 个写完后再执行一个操作——需要等全部完成。

源码路径: `PromiseCombiner.java` — `add(Future)`: 累计 `expectedCount`, 注册内部 listener 到每个子 Future(PromiseCombiner.java:151-161)。`finish(Promise aggregate)`: 锁定 aggregatePromise, 禁止再 add(PromiseCombiner.java:173-177)。内部 listener: `operationComplete0()`——`doneCount++`, 只记第一个 cause(多失败下"哪个 cause 获胜"是 undefined)(PromiseCombiner.java:56-60)。`tryPromise()`: cause==null→`aggregate.trySuccess(null)`; 否则→`aggregate.tryFailure(cause)`(PromiseCombiner.java:169-171)——try 非 set 防 double-notify(多个子 Future 同时完成)。

关键设计: PromiseCombiner 的"只记第一个 cause"是一个设计选择——不是收集全部 causes。如果需要收集全部 causes(如 batch operation 的 error report), 需要用 PromiseAggregator(已 deprecated→推荐 PromiseCombiner)的 `failPending` 模式——它会遍历 pending Promises 用相同 cause 标记失败。`checkInEventLoop()` 保证所有操作在指定 EventLoop 线程执行(PromiseCombiner.java:67)——单线程+单 cause=不需要锁。

数据流: `combiner.add(f1).add(f2).add(f3)` → expectedCount=3 → `combiner.finish(aggregate)` → aggregatePromise 锁定 → f1 完成→doneCount=1 → f2 完成(with error)→cause=error, doneCount=2 → f3 完成→doneCount=3 → `tryPromise()`: cause!=null→`aggregate.tryFailure(error)`。

### 3. PromiseNotifier.cascade — 双向取消传播

场景: 事务操作——`write(txData)` 返回一个 Promise。如果在 write 完成前事务被取消——这个 Promise 也应该被取消。反过来, 如果 write 操作本身失败了——事务也应该知道这个 Future 失败了。

源码路径: `PromiseNotifier.java:75-110` — `cascade(Promise<?> promise, Future<?> future)`: 双向取消传播——promise 上注册 listener: promise 被取消→`future.cancel(false)`; future 上注册 PromiseNotifier: future 完成→通知 promise(trySuccess/tryCancel/tryFailure)。PromiseNotifier 构造时 `promises.clone()` 防御性复制——防止外部修改(PromiseNotifier.java:55-61)。取消死循环防护: cascade 中匿名子类检测双取消(isCancelled && f.isCancelled)→短路(PromiseNotifier.java:94-99)。

关键设计: 双向取消的难点是死循环——promise 取消→future 取消→future 完成→通知 promise 取消→promise 取消→...。Netty 用 `isCancelled() && f.isCancelled()` 双真检测作为终止条件——两个都已是取消状态时, 跳过级联通知。这个防护不是完美的(理论上可能漏通知), 但避免了死循环——在实践中足够。

数据流: `cascade(promise, future)` → promise 被 cancel→`future.cancel(false)` → future 完成→PromiseNotifier.operationComplete(future)→`promise.trySuccess(future.getNow())` or `promise.tryFailure(future.cause())`。双向: promise 取消→future 取消, future 完成→promise 通知。

### 4. PromiseTask — Runnable + Promise 桥接

场景: 你想把一个 Runnable 提交到 Executor 执行, 然后用 Promise 的方式拿到结果——不需要 `java.util.concurrent.Future.get()` 的阻塞等待。

源码路径: `PromiseTask.java:21` — `extends DefaultPromise<V> implements RunnableFuture<V>`——同时是 RunnableFuture(可提交 Executor)和 DefaultPromise(可 addListener/sync)。哨兵模式: `COMPLETED/CANCELLED/FAILED` 静态 Runnable 替换原始 task——防 ScheduledFutureTask 周期性重调度时重复执行(PromiseTask.java:44-46)。`run()` 模板: `setUncancellableInternal()` → `runTask()` → `setSuccessInternal(result)` / on error `setFailureInternal(cause)`(PromiseTask.java:102-111)。公开写禁用: `setSuccess/trySuccess/setFailure/tryFailure` 全部 throw IllegalStateException——只有内部 runTask 可写(PromiseTask.java:126-128)。

关键设计: PromiseTask 是接口桥接——让 Netty 的 Promise 体系复用 JDK 的 Executor 体系。哨兵 Runnable 是关键——ScheduledFutureTask 的 period 非零时, run() 后会 `reschedule()` 重新 submit 到优先级队列——哨兵替换原始 task 防止再次执行时重复 runTask。

数据流: `new PromiseTask<>(() -> doWork())` → `executor.execute(task)` → Executor 线程: `task.run()` → `setUncancellableInternal()` → `doWork()` → `setSuccessInternal(result)` → 调用方: `task.sync()` 返回结果。

### 核心悬念

**"PromiseCombiner 用 add→finish→aggregate 三阶段编排 100 个异步操作。但每个 Future 内部——sync/await 为什么要在 EventLoop 上死锁检测? ChannelFuture 和 ChannelPromise 的 channel() 绑定怎么让异步操作与 Channel 生命周期关联? §6.3 的 ChannelFuture/ChannelPromise/DefaultChannelPromise——FlushCheckpoint 集成和 executor 后退——是 Promise 与 Channel/EventLoop 两个体系交汇的关键接口。"**

→ 引出 §6.3 Channel 层 + Scheduled + Progressive — DefaultChannelPromise 的 executor 后退机制(channel().eventLoop()) 和 checkDeadLock 条件化(channel().isRegistered()才检查)、ScheduledFutureTask 的 fixed-rate vs fixed-delay 双调度模式、ProgressivePromise 的进度追踪——这些是 Promise 体系中最接近业务层的三个扩展。
