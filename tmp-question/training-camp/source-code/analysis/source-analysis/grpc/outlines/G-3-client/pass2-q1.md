# 闭环笔记 Q1 — SynchronizationContext: 无锁单线程状态模型

假设: ManagedChannelImpl 所有共享状态用一个"借用当前线程"的串行化执行器保护, 免锁且无专用线程。

验证过程:
- grep `class SynchronizationContext` → `implements Executor` (SynchronizationContext.java:62); 结构: **ConcurrentLinkedQueue + AtomicReference<Thread> drainingThread** (L65-66)
- **drain** (L87-106): `drainingThread.compareAndSet(null, 当前线程)` CAS 抢"排空权" (L89) → 轮询队列执行 → finally 置 null (L102) → **do-while 再查队列** (L105, "must check queue again here to catch any added prior to clearing drainingThread") — 防释放瞬间竞态
- **execute** (L126-129): executeLater + drain — **可能内联执行** (调用线程直接跑任务); executeLater (L115-117) 纯入队 (锁内安全)
- **throwIfNotInThisSynchronizationContext** (L135-137): `checkState(Thread.currentThread() == drainingThread.get())` — **运行期断言**当前线程是排空线程
- ManagedChannelImpl 用法: 字段全注释 "Must be accessed from the syncContext" (L219-286 区); createSubchannel 等入口 `throwIfNotInThisSynchronizationContext` (L362,390,455); 异步事件 `syncContext.execute(new Shutdown())` (L722)
- 测试实证: createSubchannel_outsideSynchronizationContextShouldThrow (ManagedChannelImplTest.java:400)

代码类型: Algorithmic (并发协议)

结论: **无锁单线程模型** — 不是专用线程, 而是"任何线程都能借道执行, 但同一时刻只有一个人在执行" (CAS 抢权); 状态访问代价 = 一次 CAS + 队列轮询; 免去锁竞争与专用线程开销。**被放弃的方案: ① 每状态加锁 (synchronized)** — 大量读路径 (isReady/状态查询) 也要锁, 且易死锁; ② 专用状态线程 (事件循环)** — 多一次线程切换; 借用调用线程 (execute 内联) 让事件处理与触发线程合并。与 G-2 的 SerializingExecutor 同思想 (串行化) 但更轻 (无线程池依赖)。 [跨域: G-2 SerializingExecutor 同源思想] [并发: CAS + 队列] (SynchronizationContext.java:62-137; ManagedChannelImpl.java:180,362,722)
