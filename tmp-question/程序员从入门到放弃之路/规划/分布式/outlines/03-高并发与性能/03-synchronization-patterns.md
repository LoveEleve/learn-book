# 并发同步模式 — AQS、Latch、Barrier、Future 与分阶段协作如何避免永久等待

> Cluster A: 9 KPs | 依赖: 01-concurrency-foundation、02-concurrent-data-structures | 读者基线: CountDownLatch/CyclicBarrier、线程池、CAS
> 读者处境: 线程和并发容器已经建立；本篇回答线程怎样协调开始/结束/许可、异步任务怎样合并，以及等待超时/参与者失败时如何避免整个系统卡死
> 打开新视角: 同步器解决的不是同一个问题：**互斥保护共享状态，协调器表达阶段/许可/完成条件，异步组合表达依赖图**

---

### 概念依赖链

```
01 并发基础 + 02 并发容器 → 本篇: Java同步与异步编排
  ├─ §1 AQS(state/等待队列/独占共享)
  ├─ §2 CountDownLatch/CyclicBarrier(一次性/循环阶段)
  ├─ §3 Semaphore/ReadWriteLock(许可与共享访问)
  ├─ §4 CompletableFuture(依赖组合/异常)
  └─ §5 ForkJoinPool/Phaser(工作窃取/动态阶段)
先讲: AQS底层 → 固定协调器 → 许可 → 异步依赖 → 动态阶段
后续依赖: 04-traffic-management(限流/熔断/隔离)
```

### 叙事顺序

1. 问题引入——主线程等待子任务、多个阶段同步和服务并行调用，怎样避免一人失败全员永久等待？
2. AQS——同步器的状态与等待队列
3. Latch/Barrier——一次性与循环阶段
4. Semaphore/ReadWriteLock——许可和共享访问
5. CompletableFuture——异步依赖图
6. ForkJoinPool/Phaser——工作窃取和动态参与者
7. 收束

### 1. AQS — 状态变量与等待队列的同步框架

场景提示: CountDownLatch、Semaphore、ReentrantLock 看起来用途不同，为什么底层都能共享 AQS 思路？ [写作时展开]

关键设计: AQS 提供 state、队列和模板方法，子类定义 state 的业务语义：

```[pseudocode]
state:
  volatile/int-like synchronization state

acquire:
  tryAcquire/tryAcquireShared 成功?
    是 → 继续
    否 → Node 入等待队列
        → park()
        → 前驱释放/状态变化
        → unpark/重新尝试

release:
  tryRelease/tryReleaseShared
  → 状态改变
  → 唤醒后继

独占:
  一个持有者
共享:
  多个许可/读者/计数参与者
```

Why: 为什么等待线程不一直自旋？——**自旋低延迟但消耗 CPU，park 省 CPU 但唤醒有调度成本**；具体自旋/阻塞策略由实现、竞争时间和 JVM/OS 共同决定，不能固定写死“自旋多少次最优”。 [JVM/系统编程: AQS 是 Java 层框架，底层 park/unpark 与 OS 调度和 futex 路径相关]

比喻锚点: AQS 像排队系统模板：state 是窗口容量，队列是候场名单，具体同步器决定“一个人进、几张票或倒数到零”。 [写作时展开]

### 2. CountDownLatch 与 CyclicBarrier — 一次性完成条件与循环阶段

场景提示: 等 10 个子任务结束和让 10 个线程在每轮计算后同时出发，为什么不能互换这两个类？ [写作时展开]

关键设计: 两者等待对象和生命周期不同：

```[pseudocode]
CountDownLatch(count):
  worker countDown()
  coordinator await()
  count=0 → 永久打开
  → 一次性完成条件

CyclicBarrier(parties):
  每个参与者 await()
  全部到达 → barrier action/唤醒
  → phase 重置, 可重复使用
  → 参与者数量通常固定

异常:
  timeout/interrupt/参与者失败
  → barrier 可能 broken
  → 等待者收到异常, 必须处理恢复
```

Why: 为什么 Barrier 的 broken 状态很重要？——**如果一名参与者永远不来，其他线程不能无限假设阶段仍能完成**；超时/中断会让所有参与者知道本轮不能继续。Latch 的计数也不会自动因为线程异常而减少，任务必须在 finally/监督逻辑中报告完成或失败。 [Java: 同步器等待/中断/超时语义必须进入业务状态机]

比喻锚点: Latch 是一次性开门闩，Barrier 是每轮都要等齐的集合点；有人掉队，集合点必须宣布本轮失效。 [写作时展开]

### 3. Semaphore 与 ReadWriteLock — 许可控制不等于互斥锁

场景提示: 数据库连接池有 20 个连接，为什么还可能用 Semaphore(10) 限制同时执行查询？ [写作时展开]

关键设计: Semaphore 控制并发许可，ReadWriteLock 则表达读共享/写独占：

```[pseudocode]
Semaphore(permits):
  acquire → permits--
  release → permits++
  → 可公平/非公平
  → 许可耗尽时等待/超时/失败

ReadWriteLock:
  read lock → 多个读者共享
  write lock → 独占
  → 适合明确读写比例与一致性协议

对比:
  thread pool 控制执行线程
  connection pool 控制连接资源
  semaphore 可进一步限制某段临界资源并发
```

Why: 为什么 Semaphore 不是“连接池的替代品”？——**它只控制许可，不提供连接创建、借还、验证和生命周期**；许可数还需和真实数据库容量、事务时长、连接池大小配合。公平性也可能降低吞吐，非公平可能造成等待者不均。 [系统性能: Semaphore 是局部背压器，不能替代完整资源池]

### 4. CompletableFuture — 用依赖图替代回调嵌套

场景提示: 订单请求要并行查库存、优惠券、地址，再合并结果；怎样避免同步串行和回调地狱？ [写作时展开]

关键设计: CompletableFuture 通过转换、扁平化、组合和异常阶段表达异步依赖：

```[pseudocode]
thenApply:
  value → transform value

thenCompose:
  value → async Future
  → flatMap, 避免 Future<Future<T>>

thenCombine:
  Future A + Future B → merge result

allOf/anyOf:
  等全部/任一完成

exceptionally/handle/whenComplete:
  分别表达恢复、同时访问结果/异常、最终观察
```

Why: 为什么 CompletableFuture 链容易“看起来异步，实际阻塞”？——**默认执行器、thenApply 是否运行在调用线程、阻塞 I/O 是否占用 common pool、异常是否被吞掉都会改变行为**；I/O 密集任务通常需要有界、可观测的自定义 executor，并设置 deadline/取消语义。 [Java: Future 完成图的线程执行器和异常传播必须按 API 方法区分]

### 5. ForkJoinPool 与 Phaser — 工作窃取和动态阶段

场景提示: 递归任务有时父任务一直等子任务，或者阶段参与者数量动态变化；哪些同步器适合？ [写作时展开]

关键设计: ForkJoinPool 用工作窃取提高分治任务利用率，Phaser 支持动态注册与多阶段推进：

```[pseudocode]
ForkJoin:
  worker local deque
  → owner 取本地任务
  → 空闲 worker 从其他队列窃取
  → fork/join 分治

任务粒度:
  太小 → 调度/窃取开销超过计算
  太大 → 并行度不足

Phaser:
  register parties
  arrive/await advance
  arriveAndDeregister
  → phase 递进/参与者动态变化
```

Why: 为什么把阻塞 I/O 直接塞进 common ForkJoinPool 很危险？——**工作窃取池假设任务适合计算并行，阻塞会占住 worker、降低其他任务进展**；分治任务也要有合理 cutoff，Phaser 必须处理参与者异常退出和 phase 永久等待。 [系统性能: 工作窃取优化的是可分割计算，不是所有阻塞任务]

### 6. 收束

同步/异步模式：

```[pseudocode]
AQS:
  提供状态/队列/阻塞模板

Latch/Barrier/Phaser:
  阶段与完成条件

Semaphore/Lock:
  许可/互斥/读写协议

CompletableFuture/ForkJoin:
  异步依赖与计算调度
```

**Aha Moment**: "同步器的本质不是“哪个类更高级”，而是**明确谁在等谁、等什么条件、等待多久、参与者失败怎么办，以及等待成本放在 CPU 还是队列**。"
**回答读者三问**: ①Latch/Barrier 差在哪=一次性完成与循环阶段；②Semaphore 是锁吗=它是许可控制，可允许多方进入；③Future 为什么会阻塞=执行器/阻塞任务/异常和取消语义可能把异步图重新变成等待。

---

### 核心悬念

**"限流算法如何在峰值保护系统，令牌桶、漏桶、滑动窗口和自适应限流又怎样与熔断、降级、隔离协作？"**

→ 引出 04-traffic-management — 限流算法、自适应保护、熔断降级与隔离。