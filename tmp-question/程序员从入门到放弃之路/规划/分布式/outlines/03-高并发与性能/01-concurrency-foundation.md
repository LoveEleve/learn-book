# 单线程写代码是多线程在跑 — 你真的知道自己的程序在并发什么吗?

> Cluster A: 10 KPs | 依赖: 域1-01 (CAP理论) | 读者基线: 写过Java多线程代码, 用过线程池但不清楚内部细节

---

### 1. 并发 vs 并行 — 你的4核CPU在"同时"跑100个线程吗?
  线上服务挂了, 日志显示线程池满了 — 你以为是CPU不够, 加机器后依然满。
  - 并发≠并行: 并发(concurrency)是逻辑同时(时间片轮转), 并行(parallelism)是物理同时 — 单核也能并发, 多核才能并行 (B5 Ch2 §2.3, B6 Ch2 §2.1)
  - Amdahl定律: speedup = 1/((1-P) + P/N) — 20%串行部分=最多5x加速, 无论加多少核 [理论: Amdahl定律揭示了并行化的根本限制 — 串行部分是不可逾越的天花板]
  - 上下文切换开销: 线程数>CPU核数 → 每个切换保存/恢复寄存器+刷新TLB+Cache污染 → 8-15μs/次 (B3 Ch4 §4.2)
  - 关键设计: 为什么异步模型线程数少? — 同步: 线程=请求数(1000请求=1000线程+1000次switches), 异步: 线程=CPU核数(4核=4线程, 事件循环不阻塞)

### 2. Java线程模型的真相 — Thread.start()到底触发了什么?
  你new Thread().start(), 底层是怎么映射到CPU的?
  - 线程模型演变: 1.1 用户态线程(绿色线程, 1:1→1:N映射)→1.3 Native Thread(内核级线程, 1:1映射) — 现在的Thread=内核线程的wrapper (B1 Ch5 §5.2)
  - Thread生命周期: NEW→RUNNABLE(syscall通知内核注册)→BLOCKED(竞争锁)→WAITING(wait/join/park)→TIMED_WAITING→TERMINATED — 注意RUNNABLE≠Running, 可能在内核就绪队列等待 (B1 Ch5 §5.2)
  - Java线程内存开销: 每个Thread ≈ 1MB栈内存(默认) + tcb + tid — 1000线程=1GB虚拟内存, 实际物理内存按需分配但栈深度过大会OOM (B5 Ch8 §8.5, B4 Ch5 §5.3)
  - 协程/虚拟线程: Project Loom的VirtualThread — 用户态调度+栈按需分配(几百字节)→可以创建百万虚拟线程而物理线程只有CPU核数个 (B1 Ch5 §5.2)

### 3. 线程池 — 为什么newCachedThreadPool是OOM的祸首?
  线上堆栈: "java.lang.OutOfMemoryError: unable to create new native thread" — 谁在无限制创建线程?
  - ThreadPoolExecutor核心参数: corePoolSize(常驻)→maxPoolSize(上限)→keepAliveTime(空闲回收)→workQueue(任务缓冲)→handler(拒绝策略: Abort/CallerRuns/Discard/DiscardOldest) (B1 Ch5 §5.2)
  - 任务调度流程: 提交任务→当前线程数<corePoolSize(创建新线程)→workQueue未满(入队)→线程数<maxPoolSize(创建新线程)→执行拒绝策略 [工程: 线程池参数调优公式 — 最佳线程数 = CPU核数 * 目标CPU利用率 * (1 + 等待时间/计算时间)]
  - 四种内置池的坑: newFixedThreadPool(Integer.MAX_VALUE)的LinkedBlockingQueue→堆OOM; newCachedThreadPool(Integer.MAX_VALUE)的SynchronousQueue→线程OOM (B1 Ch5 §5.2)
  - 关键设计: 队列选型决定行为 — SynchronousQueue(直接提交, 0缓冲)→LinkedBlockingQueue(无限缓冲, 易内存溢出)→ArrayBlockingQueue(有限缓冲, 需预估容量)

### 4. 连接池 — 为什么你的数据库连接数总打到上限?
  并发上来后数据库报"too many connections" — 连接池没生效还是配置太小?
  - 连接池本质: 复用长连接(免去TCP握手+TLS+MySQL认证三轮RTT)→连接池=有限资源池, 本质是流量控制器 (B1 Ch5 §5.2, B5 Ch8 §8.5)
  - HikariCP为什么快: 字节码级精简(≤JIT内联阈值35字节)+ConcurrentBag(无锁设计)+FastList替代ArrayList(remove不扫描) (B1 Ch5 §5.2, B6 Ch5 §5.3)
  - 连接数公式: connections = ((core_count * 2) + effective_spindle_count) — 但实际需要压测确定, 连接池大小≠QPS上限 [案例: 某系统连接池从20调到200, QPS没涨, 锁竞争从0.2%涨到15%－连接池太大反而慢]
  - 关键设计: 连接泄漏检测 — HikariCP的leakDetectionThreshold(超时未归还打印堆栈), Druid的removeAbandoned

### 5. 锁升级 — synchronized为什么现在不比ReentrantLock慢?
  你面试时背过"synchronized重量级很慢" — 但JDK15之后这句话不准了。
  - 锁升级路径: 无锁→偏向锁(同一个线程反复获取, CAS设置ThreadID)→轻量级锁(多线程交替执行, 自旋CAS)→重量级锁(自旋失败, 膨胀→OS mutex→线程阻塞) (B4 Ch5 §5.4)
  - 偏向锁的撤销代价: 到达safepoint→暂停线程→检查是否存活→撤销偏向→STW — 这就是JDK15默认关闭偏向锁的原因(高并发下撤销>收益) (B4 Ch5 §5.4)
  - CAS无锁: AtomicInteger的compareAndSet → CPU的CMPXCHG指令 → Unsafe.compareAndSwapInt (B4 Ch5 §5.4, B3 Ch4 §4.3)
  - 关键设计: 为什么有锁升级机制? — 大部分锁是线程不竞争的(偏向锁), 偶尔竞争的(轻量级锁), 竞争激烈的(重量级锁), 自适应调整 > 一刀切

### 6. 收束 — 从线程到锁到池, 三者构成并发铁三角
  - 线程池控制"多少人干活"(资源管理), 连接池控制"多少人访问数据库"(资源瓶颈), 锁控制"多少人能同时动同一份数据"(安全保证)
  - 三者互相关联: 线程池开太大→竞争加剧→锁升级→性能下降; 连接池开太小→线程等连接→线程池假满
  - 并发性能的本质是"在哪里排队"的选择 — 让CPU排队(线程池队列)还是DB排队(连接池)还是内存排队(锁)?

---

### 核心悬念
**"ConcurrentHashMap说有读不用加锁, 那它怎么保证你读的时候不读到写到一半的数据?"**

→ 引出 并发数据结构: ConcurrentHashMap/CopyOnWrite/Disruptor/BlockingQueue — 这些并发容器到底是如何实现无锁或低锁的 (02-concurrent-data-structures)
