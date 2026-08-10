# Ch14 HashedWheelTimer — 512 Bucket 时间轮

> 依赖 Ch11 HTTP | 最后收尾

### 1. 时间轮结构 — O(1) 替代 PriorityQueue

场景: 10000 个连接——每个连接 30 秒超时——需要 10000 个定时器。`java.util.concurrent.ScheduledThreadPoolExecutor` 内部用 `DelayedWorkQueue`(PriorityQueue)——insert O(log N)=~13 层比较——10000 个定时器≈130000 次比较。时间轮: `tick & mask` 位运算→O(1)。

源码路径: `HashedWheelTimer.java:111-112` — `wheel`(L111) + `mask`(L112) 字段声明: `HashedWheelBucket[] wheel; final int mask;`。构造器初始化在 `HashedWheelTimer.java:338-339` — `wheel = new HashedWheelBucket[ticksPerWheel]`、L290 `mask = wheel.length - 1`(2 的幂, 为位运算设计)。`tick & mask` 定位 bucket——单次 AND, 无循环/比较。默认 `tickDuration=100ms`——最大时间跨度 = 512×100ms=51.2 秒——IO 超时通常 30 秒, 在范围内。`newTimeout(task, delay, unit)`: `deadline = nanoTime() + delay`→`pendingTimeouts.increment()`→`timeouts`(MPSC 队列)offer→`start()` CAS `INIT→STARTED`→`workerThread.start()`。`INSTANCE_COUNT_LIMIT=64`(L92)——超过 64 个实例打印 WARN——提醒单例共享。

关键设计: 时间轮的"不精确"特性——tickDuration=100ms——IO 超时不需要毫秒级精度(tcp keepalive 30s, 100ms 精度完全够)。`pendingTimeouts` 限制(默认无限)可配 `maxPendingTimeouts`——防止外部线程疯狂提交 timer 导致 OOM。`start()` CAS 保证只有一个 worker 线程启动——外部线程可以并发调用 `start()` 但只有一个获胜。

数据流: `timer.newTimeout(() -> closeConnection(), 30, SECONDS)`→`deadline = nanoTime() + 30s`→`timeouts.offer(timeout)`→Worker 线程: `waitForNextTick()`(sleep until next 100ms boundary)→`transferTimeoutsToBuckets()`(100K/tick 上限)→`bucket.expireTimeouts(deadline)`→`task.run(timeout)`→close 连接。

### 2. HashedWheelBucket — 双向链表 + 到期执行

场景: wheel[42] 上有 3 个 timeout——两个 `remainingRounds==0`(本 tick 执行), 一个 `remainingRounds==5`(还要 5 ticks)。

源码路径: `HashedWheelBucket.java:762-777` — head/tail 双向链表——`HashedWheelTimeout` 自身作为节点(prev/next 字段)——零额外 `Node` 对象(`LinkedList.Node` 是额外 GC)。`addTimeout(timeout)`: tail 追加, 维护 prev/next 双链。`expireTimeouts(long deadline)`: 遍历链表→`remainingRounds <= 0`→`timeout.deadline <= deadline`→`timeout.expire()`(CAS INIT→EXPIRED→task.run(timeout))→`remainingRounds > 0 && !isCancelled()`→`remainingRounds--`。`remove(timeout)`: prev/next 指针重连, O(1) 中段移除。

关键设计: Timeout 自身作链表节点→避免 GC。`expireTimeouts` 只在一个 tick 内遍历→链表长度通常很短(每 tick 只有几个 timeout)——遍历 O(N)成本很低。`remainingRounds` 的作用: deadline 超出当前轮次(spans multiple wheel rotations)→每轮减 1→到 0 执行→避免了"deadline 超过 51.2 秒的 timer 被分配到错误 bucket"的问题。

数据流: `wheel[42].expireTimeouts(deadline_tick42)`→遍历链表: timeout1`remainingRounds=0, deadline<=current`→`task.run(timeout1)`→timeout2`remainingRounds=5`→`remainingRounds=4`→tick43→...→tick47→`remainingRounds=0, deadline<=current`→`task.run(timeout2)`→timeout3`remainingRounds=3`→`remainingRounds=2`(未到期)。

### 3. Worker 三态 + 精确等待

场景: 100ms tick——worker 线程需要在下一个 100ms 边界醒来——不是简单的 `Thread.sleep(100)`——因为 transfer/expire 本身花了一些时间。

源码路径: `HashedWheelTimer.java:570-605` — `waitForNextTick()`: `deadline = tickDuration * (tick + 1)`——计算下一个 tick boundary(从 startTime 开始的绝对 deadline)。`sleepTimeMs = (deadline - currentTime + 999999) / 1000000`——需要 sleep 的毫秒数。`sleepTimeMs <= 0`→立即返回。Windows 特殊处理(#356): `sleepTimeMs = sleepTimeMs / 10 * 10`→`sleepTimeMs == 0 → sleepTimeMs = 1`——Windows JVM bug 绕过。`Thread.sleep(sleepTimeMs)`→被 interrupt→检查 `workerState == SHUTDOWN`→返回 `Long.MIN_VALUE`(停止信号)。

关键设计: `waitForNextTick()` 用绝对时间而非相对时间——`tick * tickDuration` 计算绝对 deadline——不是 `sleep(100ms - 上次transfer耗时)`——不会累积误差——每次 tick 都从 startTime 重新计算精确的绝对时间。Windows bug #356——JVM 在 Windows 上调用 sleep(1) 实际 sleep 0ms→循环死转 CPU→`sleepTimeMs/10*10` 把 sleep time 对齐到 10ms 粒度→`sleepTimeMs==0→=1` 保证至少 sleep 1ms。

数据流: tick=0→`waitForNextTick()`→`deadline = 100ms`→`sleep(deadline - now)`→醒→`transferTimeoutsToBuckets()`→`expireTimeouts(deadline)`→tick++→tick=1→`waitForNextTick()`→`deadline = 200ms`→sleep(200-currentTime=200-101.5=98.5ms→98ms)→醒→transfer→expire→tick++→...

### 4. cancel + stop — 清理

场景: `timeout.cancel()`——取消一个已注册的 timer——需要从 bucket 的链表中移除。

源码路径: `HashedWheelTimeout.cancel()`: CAS `INIT→CANCELLED`→`timer.cancelledTimeouts.offer(this)`(MPSC 队列)→Worker 下次 tick 开头 `processCancelledTasks()` 遍历 `cancelledTimeouts`→`timeout.removeAfterCancellation()`→从 bucket 链表中移除。`stop()`: CAS `STARTED→SHUTDOWN`→循环收集所有未完成的 timeout→`workerThread.interrupt()`→返回 `unprocessedTimeouts`。

关键设计: cancel 异步——外部线程不直接修改 bucket 链表(Woker 线程独占)——通过 `cancelledTimeouts` 队列委托 Worker 线程处理——线程安全。`removeAfterCancellation` 只在 Worker 线程上执行——链表 prev/next 操作非线程安全。

数据流: `timeout.cancel()`→CAS→`cancelledTimeouts.offer(timeout)`→Worker tick: `processCancelledTasks()`→`timeout.removeAfterCancellation()`→bucket 链表: `timeout.prev.next = timeout.next`→`timeout.next.prev = timeout.prev`→移出。

→ Netty 源码分析核心卷至此全部完成。从 NIO ByteBuffer 的四字段状态机(Ch1), 到时间轮的 O(1) 定时调度(Ch14)——全书 12 章形成一个完整闭环。
