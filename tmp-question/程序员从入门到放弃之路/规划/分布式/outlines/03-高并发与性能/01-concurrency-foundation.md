# 并发基础 — 线程、线程池、连接池与锁究竟在哪里排队

> Cluster A: 10 KPs | 依赖: 分布式理论 01 CAP/一致性、系统编程线程/锁 | 读者基线: Java 多线程、线程池、数据库连接池
> 读者处境: 高并发与性能开篇；本篇回答线程池满、数据库连接耗尽、锁竞争和 CPU 加核无效之间的关系
> 打开新视角: 高并发不是“创建更多线程”，而是**在 CPU、任务队列、连接池、数据库和共享数据之间选择正确的排队位置**

---

### 概念依赖链

```
系统编程线程/锁 + 分布式 CAP → 本篇: 并发/线程池/连接池/锁
  ├─ §1 并发 vs 并行(Amdahl/上下文切换)
  ├─ §2 Java Thread/Virtual Thread(执行实体)
  ├─ §3 ThreadPoolExecutor(任务排队/拒绝)
  ├─ §4 连接池(数据库资源背压)
  └─ §5 锁/原子操作(共享状态安全)
先讲: 并发概念 → 线程实体 → 任务池 → 连接池 → 锁
后续依赖: 02-concurrent-data-structures(并发容器与低锁结构)
```

### 叙事顺序

1. 问题引入——线程池满、连接池满、CPU 加核后仍慢，真正排队在哪里？
2. 并发/并行与上下文切换
3. Java Thread/Virtual Thread
4. 线程池参数与队列
5. 连接池与数据库背压
6. 锁升级/CAS 与共享状态
7. 收束

### 1. 并发与并行 — 逻辑同时不等于物理同时

场景提示: 4 核 CPU 上运行 100 个线程，究竟有多少线程在同一时刻执行？加机器为什么可能不改善线程池超时？ [写作时展开]

关键设计: 并发是任务交错/同时存在，并行是多个执行核同时执行；Amdahl 和调度成本限制加核收益：

```[pseudocode]
concurrency:
  多个任务在时间窗口内推进
  → 单核也可通过时间片交错

parallelism:
  多核同时执行多个任务

Amdahl:
  speedup <= 1 / ((1-P) + P/N)
  → 串行部分限制总体上限

线程过多:
  runnable queue + context switch
  → 保存/恢复状态、缓存/TLB扰动
  → 不一定提高吞吐
```

Why: 为什么线程池满不一定是 CPU 核数不够？——**线程可能在等连接、锁、I/O 或下游，线程数增加只会增加排队和上下文切换**；并行化前要知道 workload 是 CPU bound 还是等待 bound。上下文切换延迟也不能用固定微秒数跨平台断言。 [系统性能: Amdahl/等待分析/USE 先定位瓶颈再加并发]

比喻锚点: 并发像多个订单同时在处理，只有并行才是多个工位同时加工；增加订单不等于增加工位。 [写作时展开]

### 2. Java Thread 与 Virtual Thread — 执行实体的不同成本

场景提示: `Thread.start()` 后 JVM 和操作系统分别创建/管理什么？ [写作时展开]

关键设计: 平台线程、用户态调度线程和虚拟线程的栈、调度、阻塞语义不同：

```[pseudocode]
platform thread:
  JVM thread ↔ OS schedulable thread
  → 独立栈/寄存器/调度实体
  → 可真正多核并行

Java thread states:
  NEW → RUNNABLE/BLOCKED/WAITING/TIMED_WAITING → TERMINATED
  RUNNABLE 不保证当前正在 CPU 上运行

virtual thread:
  用户态任务/continuation
  → 由少量 carrier/platform threads 承载
  → 阻塞可被 runtime 处理时让出 carrier
  → 仍受 pinning、CPU、内存和同步模型限制
```

Why: 为什么虚拟线程能创建很多，却不能把 CPU 密集任务无限并行？——**它减少了阻塞任务的线程栈/调度成本，但 CPU 核数、锁、数据库连接和下游容量仍是瓶颈**；“百万虚拟线程”是模型能力，不是业务吞吐承诺。 [JVM: 虚拟线程行为依赖 JDK 版本、调度器、pinning 和阻塞调用兼容性]

### 3. ThreadPoolExecutor — 队列策略决定系统如何过载

场景提示: `newCachedThreadPool` 运行一段时间后 OOM，`newFixedThreadPool` 又把任务无限堆在内存里，为什么？ [写作时展开]

关键设计: 线程池是“线程数 + 工作队列 + 拒绝策略”的联合状态机：

```[pseudocode]
submit(task)
  → worker < corePoolSize?
      创建 worker
  → 否则 queue 未满?
      入队
  → 否则 worker < maximumPoolSize?
      创建额外 worker
  → 否则 rejection handler

队列:
  SynchronousQueue → 无容量, 直接交给 worker
  bounded queue → 有限背压
  unbounded queue → 可能隐藏内存/延迟无限增长

拒绝:
  Abort / CallerRuns / Discard / DiscardOldest
  → 不同的数据丢失/背压语义
```

Why: 为什么“最大线程数”在使用无界队列时可能几乎不起作用？——**任务先进入队列，池不会轻易扩展到 maximum**；队列策略实际上决定请求在何处排队。线程数公式只能作为初始估计，最终要按 CPU/等待比例、任务时长、下游容量和尾延迟压测。 [JVM: ThreadPoolExecutor 参数组合决定拒绝/背压，不存在所有业务通用最佳值]

比喻锚点: 线程池像餐厅：先开固定桌位，再排队；队列无限长看起来不拒客，却会让客人和食材一起过期。 [写作时展开]

### 4. 连接池 — 数据库连接是稀缺资源和背压阀

场景提示: 线程池有 500 个线程，数据库连接池只有 20 个，为什么系统表现为线程大量等待而不是吞吐提升？ [写作时展开]

关键设计: 连接池复用连接并限制进入数据库的并发：

```[pseudocode]
request
  → borrow connection
  → 无空闲连接?
      等待/超时
  → 执行 SQL/事务
  → reset state
  → return connection

池太小:
  应用线程在池前排队

池太大:
  数据库锁/CPU/I/O/上下文竞争增加
  → QPS 未升, P99 变差
```

Why: 为什么连接池大小不等于 QPS 上限？——**QPS 还受事务时间、数据库并发、锁、I/O 和 SQL 成本影响**；连接数调大可能只是把应用等待搬进数据库。HikariCP 等实现的内部优化不能替代容量模型，leak detection 也需结合正常长事务/流式读取判断。 [分布式架构: 16 连接池篇与本篇共同说明“有限资源池就是流量控制器”]

### 5. 锁升级与 CAS — 共享状态安全也有竞争成本

场景提示: `synchronized` 为什么不必然比 `ReentrantLock` 慢，CAS 又为什么不是无成本无锁？ [写作时展开]

关键设计: JVM 锁实现会根据竞争形态优化，但具体升级路径随 JDK 版本变化；CAS 则通过原子比较交换处理短冲突：

```[pseudocode]
synchronized/monitor:
  进入/退出对象监视器
  → 低竞争与高竞争可能走不同快速/阻塞路径

CAS:
  compare(expected, desired)
  → 成功更新
  → 失败重试/退避

高竞争:
  CAS 重试/缓存一致性流量上升
  锁膨胀/线程阻塞/调度成本上升
```

Why: 为什么不能背“偏向锁→轻量锁→重量锁”当作所有现代 JDK 的固定实现？——**锁实现、偏向锁策略和 JVM 版本持续变化，性能取决于竞争、临界区、线程调度和数据布局**；CAS 也会遭遇 ABA、活锁、饥饿和缓存线 bouncing。优化前需用锁/CPU/等待工具证明热点。 [系统编程: C++ atomic/MESI 与 JVM 锁共同体现硬件一致性和语言同步语义]

### 6. 收束

并发铁三角：

```[pseudocode]
线程池:
  控制执行任务数量/任务队列

连接池:
  控制进入数据库/下游的并发

锁/原子:
  保护共享状态

池/锁/线程任一过载:
  → 其他层可能出现假满/排队/超时
  → 需要统一观测与容量建模
```

**Aha Moment**: "高并发优化的本质不是把线程数调大，而是**决定在哪里排队、在哪里背压、哪些资源必须串行，以及如何让队列不会无限增长**。"
**回答读者三问**: ①并发和并行差在哪=逻辑同时与物理同时；②线程池为什么会 OOM=队列/线程上限和任务速率不匹配；③连接池为什么是流控=它限制下游资源并把压力转成可见等待。

---

### 核心悬念

**"线程、线程池、连接池和锁已经建立；ConcurrentHashMap、CopyOnWrite、Disruptor 和 BlockingQueue 如何在共享数据上实现低锁并发？"**

→ 引出 02-concurrent-data-structures — 并发数据结构与无锁/低锁实现。