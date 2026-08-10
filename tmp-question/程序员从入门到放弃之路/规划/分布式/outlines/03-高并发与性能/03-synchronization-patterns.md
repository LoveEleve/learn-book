# 主线程在等所有子任务 — 但你确定能等到吗?

> Cluster A: 9 KPs | 依赖: 01-concurrency-foundation, 02-concurrent-data-structures | 读者基线: 用过CountDownLatch/CyclicBarrier但不清楚区别和进阶用法

---

### 1. AQS — 所有同步器的"操作系统"
  你翻看CountDownLatch、Semaphore、ReentrantLock的源码, 发现它们都继承AQS — 这套框架到底做了什么?
  - AQS本质: state(volatile int, 同步状态)+CLH队列(双向链表, 自旋等待)+模板方法(tryAcquire/tryRelease子类实现) (B1 Ch5 §5.3)
  - 独占vs共享: 独占(ReentrantLock: state=1有线程持有)→共享(Semaphore: state=N有N个许可, CountDownLatch: state=count倒数) [理论: AQS通过state语义区分独占和共享 — state的CAS更新定义了"获取"和"释放"的含义]
  - 入队出队: lock竞争失败→addWaiter(创建Node节点CAS入队尾)→acquireQueued(自旋检查是否前驱是head+tryAcquire)→park()阻塞→unparkSuccessor唤醒后继 (B1 Ch5 §5.3)
  - 关键设计: 为什么自旋一定次数后会park? — 自旋消耗CPU但低延迟, park不耗CPU但高延迟, 自旋次数=上下文切换时间/每次自旋时间 作为平衡点

### 2. CountDownLatch vs CyclicBarrier — 同样的"等待", 完全不同的语义
  你用CountDownLatch等10个线程完成, 换成CyclicBarrier代码编译通过但行为全变了 — 发生了什么?
  - CountDownLatch: 一次性, 一个/多个线程等待N个事件完成→countDown()减一→await()等state=0 — 门闩放下一去不回 (B1 Ch5 §5.3)
  - CyclicBarrier: 可复用, N个线程互相等待→await()等所有线程到达→barrier action(可选回调)→唤醒所有人→重置count — 栅栏(循环使用) (B1 Ch5 §5.3)
  - 典型场景: Latch = 主线程等5个子任务结果汇总(一次性); Barrier = 多线程分阶段计算, 每一阶段同步后进入下一阶段 (B1 Ch5 §5.3)
  - 关键设计: CyclicBarrier的broken状态 — 如果有线程await超时, barrier被标记为broken, 所有等待线程收到BrokenBarrierException — 防止部分线程完成部分线程卡住

### 3. Semaphore — 你用它做限流, 但它是共享的非独占锁
  你给一个服务加Semaphore(10)限流 — 第11个请求进来怎么处理?
  - Semaphore: state=permits — acquire()递减, release()递增 — 公平/非公平(默认)两种模式 (B1 Ch5 §5.3, B5 Ch6 §6.6)
  - Semaphore vs 固定线程池限流: Semaphore控制并发访问数(线程池线程可复用→Semaphore控制同时访问DB的数量), 线程池控制线程数 [工程: Semaphore控制数据库连接并发 — 连接池20个连接但Semaphore(10)限制同时执行查询的线程数, 减少DB压力]
  - Semaphore vs AtomicInteger计数: Semaphore自带阻塞等待(acquire), AtomicInteger需要自己写循环检查 — 前者是同步器, 后者是计数变量
  - ReadWriteLock本质: 读锁=共享Semaphore, 写锁=独占Semaphore — state高16位读计数+低16位写计数, 同一个AQS框架

### 4. CompletableFuture — 链式编排替代回调地狱
  你需要调服务A→用结果调B和C→用B和C的结果调D — 同步写法串行太慢, 回调写法嵌套太深。
  - 核心方法: thenApply(转换)→thenCompose(flatMap扁平化)→thenCombine(两个Future合并)→allOf(等所有完成)→anyOf(等任意完成)→exceptionally(异常恢复) (B1 Ch5 §5.1, B5 Ch8 §8.4)
  - 线程池选择: 默认ForkJoinPool.commonPool() — 但IO密集型任务应该用自定义线程池(避免阻塞commonPool影响其他CompletableFuture) (B4 Ch5 §5.3)
  - 异常处理模式: exceptionally(同步恢复)→handle(访问结果+异常两者)→whenComplete(最终执行, 不改变结果) [案例: 电商下单 — CompletableFuture.allOf(checkInventory(), checkCoupon(), checkAddress())→thenCombine(totalPrice)→thenCompose(createOrder)]
  - 关键设计: thenCompose vs thenCombine — thenCompose = flatMap(一个Future的输出是下个Future的输入), thenCombine = 两个独立Future然后合并

### 5. ForkJoinPool与Phaser — 工作窃取与多阶段同步
  你的递归任务有时子任务做完了但父任务在等, 有时子任务还没做父任务就提前结束了 — 谁偷了谁的任务?
  - ForkJoinPool: 工作窃取算法 — 线程完成自己的任务后从其他线程队列尾部"偷"任务, 双端队列(owner从头部取, 窃取者从尾部偷) (B4 Ch5 §5.3)
  - Phaser: 多阶段CountDownLatch+CyclicBarrier结合体 — phase(当前阶段)→arriveAndAwaitAdvance(到达并等下一阶段)→arriveAndDeregister(到达并注销) — 动态注册/注销参与者 (B1 Ch5 §5.3)
  - 关键设计: ForkJoin分治的临界点 — 任务太小: 调度开销>计算开销; 任务太大: 工作窃取无法展开。通过RecursiveTask的compute()中判断是否直接compute还是fork

### 6. 收束 — 同步模式的本质是"协调", 锁模式的本质是"互斥"
  - Latch/Barrier/Semaphore/CompletableFuture解决的是"谁等谁、等多久、等什么" — 协调型同步
  - Lock/synchronized解决的是"谁先来谁后到、一次一个" — 互斥型同步
  - 分布式场景下协调型同步的挑战远大于互斥 — 超时(这个线程可能没死只是慢)、网络分区(我看不到它但它做了)

---

### 核心悬念
**"限流说令牌桶好, 漏桶说稳定好, 但你的系统在双11峰值是平时的100倍 — 什么算法能让系统不崩也不浪费资源?"**

→ 引出 流量管理: 限流四算法+自适应+熔断降级+隔离 — 从理论到Sentinel再到双11实战 (04-traffic-management)
