# 100个线程同时put一个HashMap — JDK怎么让你既读到正确数据又不阻塞?

> Cluster A: 8 KPs | 依赖: 01-concurrency-foundation | 读者基线: 了解Java HashMap基本使用, 知道基本线程安全问题

---

### 1. 为什么HashMap多线程put会死循环直到CPU 100%?
  线上突然CPU飚到100%, 线程dump显示所有线程卡在HashMap.transfer() — 为什么?
  - JDK7 HashMap死循环根因: resize→transfer→头插法逆序→并发时链表成环→get()时遍历成死循环 (B3 Ch4 §4.3)
  - JDK8修复: 尾插法(保留原顺序)+高位低位双链表(e.hash & oldCap)→避免成环但仍会数据覆盖(put时丢失) (B3 Ch4 §4.3, B4 Ch5 §5.4)
  - 碰撞处理: 链表(<=8)→红黑树(>=8且数组>=64) — 为什么要变树? 哈希冲突严重时链表O(n)退化为O(log n) (B3 Ch4 §4.3)
  - 关键设计: 为什么treeify阈值是8? — 泊松分布: 0.75负载因子下, 链表长度=8概率<千万分之一, 但恶意构造可触发DoS攻击

### 2. ConcurrentHashMap — 分段锁(CAS+synchronized)如何做到几乎无锁读?
  CHM的get()不需要加锁 — 但它怎么保证你读到的是完整的、不是正在被迁移中的"半截数据"?
  - JDK7→JDK8演进: Segment(分段锁, 默认16段, 并发度16最高)→CAS+synchronized(对数组节点加锁) + volatile保证数组引用可见性 (B3 Ch4 §4.3, B4 Ch5 §5.4)
  - 为什么get()不加锁能正确? — Node的val和next是volatile; 扩容时数组引用volatile; 数据迁移使用ForwardingNode标记(读到则去新数组找) [理论: happen-before保证 — volatile写(put/扩容) happen-before volatile读(get), 保证可见性]
  - size()的计数: JDK7(3次CAS+锁计算Segment.count), JDK8(CounterCell数组+CAS, 类似LongAdder) — 不需要全局锁 (B3 Ch4 §4.3)
  - 关键设计: CHM的锁粒度=Hash桶级别(一个桶一把锁), 不是整表一把锁 — 16个桶=16个线程可以同时写不同桶

### 3. CopyOnWriteArrayList — 读完全无锁, 但写要复制整个数组
  你看COW的源码, add()里Arrays.copyOf复制了整个数组 — 这不是很浪费吗?
  - COW机制: 写时复制新数组→修改→volatile赋值回引用 — 读线程看到的始终是老数组或新数组, 不会看到"写一半" (B4 Ch5 §5.4)
  - 适用场景: 读>>写(99%读, 1%写) — 白名单/黑名单/监听器列表/配置列表 (B4 Ch5 §5.4)
  - 代价: 每次写O(n)内存复制+GC(old数组) — 写频繁时CPU和内存双爆炸
  - 关键设计: COW vs CHM与读写锁 — COW(读完全无锁, 写代价高)适合配置类数据; CHM(读写都有轻微锁开销)适合缓存; ReadWriteLock(读共享写独占)适合读多写少但有即时性要求的场景

### 4. Disruptor — 无锁环形缓冲区如何做到600万QPS?
  你觉得ArrayBlockingQueue够快了(一把锁), Disruptor凭什么比它快一个数量级?
  - RingBuffer核心: 预分配数组(无GC)+Sequence(volatile long+CAS)+缓存行填充(避免伪共享) — 生产者通过CAS申请Sequence槽位, 消费者读Sequence (B1 Ch5 §5.3, B3 Ch4 §4.3)
  - 缓存行填充(伪共享问题): @Contended注解 → 64字节填充 → 避免两个Sequence变量在同一缓存行导致互相invalidates [工程: `long p1,p2,p3,p4,p5,p6,p7` — 7×8=56+实际字段=64字节, 保证独占一条缓存行]
  - 生产者类型: SingleProducer(单线程无锁)/MultiProducer(CAS竞争Sequence) — 为什么生产者类型影响性能? 单生产者省去CAS开销
  - 关键设计: Disruptor vs ArrayBlockingQueue — ABQ: 一把ReentrantLock(put+take)+两个Condition(notEmpty/notFull); Disruptor: 0锁+Sequence CAS+缓存行优化

### 5. 并发容器选型决策 — 什么场景用什么容器?
  面试官问: "你设计一个秒杀系统, 库存扣减用什么数据结构?"
  - 读多写少(配置/字典): COW → 读0锁开销, 写可接受延迟
  - 读写均衡(缓存/热点数据): CHM → 分段锁+volatile, 高并发读写
  - 生产者消费者(任务队列): BlockingQueue → 有界缓冲+等待通知机制 [案例: 秒杀扣库存用CHM(CAS扣减) + DB最终落盘, 不用数据库行锁做高并发扣减]
  - 高性能事件处理(日志/交易撮合): Disruptor → 0锁+批量消费, 6M TPS
  - 关键设计: 并发容器的选择本质是"在哪里排队" — CPU(Disruptor CAS自旋) vs 内存(BQ锁等待) vs 磁盘(持久化后消费)

### 6. 收束 — 无锁不是目标, 正确的并发语义才是
  - CHM用volatile+CAS+synchronized的组合拳证明了: 不是所有并发都需要锁, 但需要正确的内存可见性保证
  - COW证明了"空间换并发读性能" — 写复制的开销在可接受范围时是合理选择
  - Disruptor证明了缓存行友好+无锁设计可以比有锁快一个数量级
  - 但无锁并不总是更快 — CAS自旋在高竞争下消耗CPU比锁等待更严重

---

### 核心悬念
**"你写了一个CountDownLatch等所有子任务完成, 但如果有一个线程挂了就永远等不到 — 分布式环境下你怎么办?"**

→ 引出 同步模式: 从单机CountDownLatch到分布式协调, Semaphore/Barrier/CompletableFuture的实战陷阱 (03-synchronization-patterns)
