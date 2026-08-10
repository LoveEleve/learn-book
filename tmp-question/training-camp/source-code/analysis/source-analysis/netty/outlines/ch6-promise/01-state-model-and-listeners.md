# Ch6 Promise/Future 状态模型与 Listener — DefaultPromise 的 5 态编码

> Cluster A: 14 KPs | 依赖 Ch5 EventLoop | Ch6 → §6.2

### 1. Future/Promise 读写分离 — isSuccess / cause / getNow

场景: `ChannelFuture future = channel.connect(addr)` ——连接是否完成? 成功还是失败? 返回值是什么? Future 只提供查询接口——isDone/isSuccess/cause/getNow。Promise 是 Future 的可写扩展——setSuccess/setFailure/trySuccess/tryFailure。

源码路径: `Future.java` — `isSuccess()` = isDone() && cause()==null, `isDone()` = success|cancelled|failed 都算 done。`cause()` 三态映射: 失败→catch 的 throwable, cancel→懒创建 CancellationException, 否则→null。`getNow()` = done? 返回值 : null——非阻塞读取, 不保证 happens-before。`Promise.java` — `setSuccess(V)/setFailure(Throwable)`: 强制写入, 已 done 抛 IllegalStateException。`trySuccess/tryFailure`: CAS 尝试写入, 已 done 返回 false——适合多生产者竞争。

关键设计: Future/Promise 的读写分离是 Netty 异步模型的基础——EventLoop 持有 Promise(可写, 操作完成时 setSuccess), 外部调用方持有 Future(只读, 通过 addListener 或 sync 等待结果)。JDK 的 `java.util.concurrent.Future` 只有 `get()→阻塞` 和 `isDone()→轮询`, 没有异步回调——Netty 的 `addListener(GenericFutureListener)` 让回调注册成为一等公民。

数据流: `channel.connect(addr)` → 返回 ChannelFuture(Promise 的子类型) → EventLoop 完成连接 → `promise.setSuccess()` → 所有 listener 被通知 → 调用方 `future.isSuccess()` → true。

### 2. DefaultPromise.result 单字段 5 态编码

场景: 一个 Future 有 5 种状态: 1)未完成 2)成功(有返回值) 3)成功(无返回值, Void) 4)失败(有 throwable) 5)被标记为不可取消。用 5 个 boolean 字段管理是不可维护的——一个 volatile 字段编码全部。

源码路径: `DefaultPromise.java:result` — 单 volatile Object 字段编码 5 态: `null`=未完成(初始态); `SUCCESS`=成功但无返回值(Void/Future 的 sentinel 值, `result==SUCCESS` 时 isSuccess()=true); `UNCANCELLABLE`=标记不可取消(sentinel, 非 done); `CauseHolder`=失败(包装 throwable, `cause()` 返回 `((CauseHolder)result).cause`); 实际值 V = 成功返回值(`result instanceof V && result != SUCCESS`)。`RESULT_UPDATER.compareAndSet(this, null, val)`——CAS 原子状态转换, 只第一个成功写入的获胜。

关键设计: 单字段 5 态的设计让 `isSuccess()` 和 `isDone()` 成为单次 volatile 读——不需要多字段比较。`SUCCESS` sentinel 值解决了 `Promise<Void>` 的问题——无返回值时, `result=null` 和未完成状态冲突——用 sentinel 值区分。`CauseHolder` 避免用 `instanceof Throwable` 判断(返回值 V 可能恰好是 Throwable)。设计灵感: 类似 `CompletableFuture` 的 `result` 字段(也是单字段, 但 Netty 的实现更早)。

数据流: `setSuccess(null)` → `RESULT_UPDATER.CAS(null, SUCCESS)` → `notifyListeners()` → `isSuccess()` = `result == SUCCESS` → true。`setFailure(e)` → `CAS(null, new CauseHolder(e))` → `notifyListeners()` → `cause()` = `((CauseHolder)result).cause`。

### 3. addListener 渐进升级 — null→1→2→array

场景: 99% 的情况下一个 Future 只有 0-2 个 listener——为这 2 个 listener 预分配一个数组是浪费。但偶尔会有 10+ 个 listener(多个 handler 等待同一个异步操作)——需要扩容。

源码路径: `DefaultPromise.java:addListener0` — 渐进升级: 0 个 listener→存单个 listener 引用(`GenericFutureListener`); 1 个→升级为 `GenericFutureListener[]` 数组[2]; 2+ 个→`DefaultFutureListeners` 包装, 内部数组翻倍扩容。`DefaultFutureListeners.java` — `listeners` 数组存储, `add()` O(1) 尾部追加, `remove()` O(n) 遍历+arraycopy。

关键设计: 渐进升级是"空间换时间"的典型——用三种存储模式(单引用/数组[2]/DefaultFutureListeners)覆盖 0-100+ listener 的全范围, 保证低 listener 数时零数组开销。`addListener` 的早监听: addListener 时如果 Future 已完成(done), 立即在当前线程回调 listener——不进 EventLoop 任务队列——`notifyListener0(future, listener)`(DefaultPromise)。

数据流: `future.addListener(l1)` → listeners=null→listener=l1 → `future.addListener(l2)` → `listeners = new GenericFutureListener[]{l1, l2}` → `future.addListener(l3)` → `new DefaultFutureListeners(l1, l2)`→`add(l3)` → 无限扩容。

### 4. sync/await — 阻塞等待 + 死锁检测

场景: 你需要在当前线程等待一个异步操作完成——但当前线程可能就是 EventLoop 线程——如果 await() 阻塞了 EventLoop, 整个 Channel 的 IO 被挂起——死锁。

源码路径: `DefaultPromise.java:checkNotifyWaiters` — `sync()` = `await()` + 如果 cause!=null 则 throw cause(把异步异常同步抛出)。`await()` = `Object.wait()` 线程阻塞——当 `notifyListeners()` 中 `checkNotifyWaiters()` 调用 `notifyAll()` 时唤醒。`DefaultPromise.java:checkDeadLock` — `inEventLoop() && channel().isRegistered()` → `BlockingOperationException`——EventLoop 线程上 await 会导致整个 Channel 的 IO 停止。`DefaultChannelPromise.java:156-161` — `checkDeadLock` 条件化: `channel().isRegistered()` 才检查——未注册 Channel 的 await 安全(Channel 还没绑定到 EventLoop)。

关键设计: checkDeadLock 是 EventLoop 单线程模型的自我防护——代码在 EventLoop 线程上 await→EventLoop 永远 self-block→永远不会 `notifyAll`→死锁。BlockingOperationException 的错误信息会提示用 `addListener` 替代 `await`——`ChannelFutureListener.CLOSE` 等预定义 listener 提供常用的异步回调。

数据流: `future.sync()` → `await()`—`Object.wait()` → EventLoop 完成操作 → `promise.setSuccess()`→`notifyListeners()`→`checkNotifyWaiters()`→`notifyAll()` → 调用方被唤醒 → `cause!=null? throw cause : return future`。

### 5. cause() 懒创建 + isVoid 短路

场景: `future.cancel()` 被调用了——调用方不需要立即看到 CancellationException。只有当调用方真正调 `cause()` 时才创建异常——但创建异常时 fillInStackTrace() 是昂贵的 native 调用。

源码路径: `DefaultPromise.java:cause()` — cancel 时不创建 Exception, 只在 `cause()` 首次调用时懒创建。共享预填堆栈帧: `CANCELLATION_CAUSE_HOLDER` 缓存——`fillInStackTrace` 零调用。`isVoid()`: ChannelFuture.isVoid()=true 的 Future 禁用 addListener/await/sync——VoidChannelPromise 用于"不需要结果"的场景——零分配短路。

关键设计: 懒创建 CancellationException 是内存优化——大多数取消的 Future 没人查 cause(), 预先创建 Exception(含堆栈 fill)是浪费。`CANCELLATION_CAUSE_HOLDER` 共享预填堆栈帧——多个 Future 共享同一个 Exception 对象 + 同一个堆栈(因为取消发生的位置通常是同一个代码路径)。

数据流: `future.cancel()` → result=CauseHolder(CANCELLED) → 调用方 `future.cause()` → result==CANCELLED → 首次调用→new CancellationException("...")→缓存到 CANCELLATION_CAUSE_HOLDER → 后续调用→返回缓存的对象。

### 核心悬念

**"引用计数(Ch4)让内存有了确定性释放, Promise(Ch6)让异步操作有了确定性通知。但 100 个异步操作——你要等全部完成才继续——单独 addListener 不够用。§6.2 的 PromiseCombiner(add→finish→aggregate) 和 PromiseNotifier.cascade(双向取消传播) 是 Netty 异步编排的两块基石——相似但不相同: Combiner 是在所有子 Future 完成后通知一个聚合 Promise, Notifier 是一个 Future 完成时通知多个 Promise。"**

→ 引出 §6.2 组合器与不可变 Future — CompleteFuture(addListener 立即通知) + PromiseCombiner 三阶段 + PromiseNotifier 双向取消 + PromiseTask 哨兵防重复。当 100 个异步操作有 10 个失败——PromiseCombiner 只记第一个 cause, PromiseAggregator 级联失败全部——两种策略各有适用的场景。
