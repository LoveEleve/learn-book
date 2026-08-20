# C++ 原子与无锁编程 — 从缓存一致性到 memory order、CAS 与 SPSC 队列

> Cluster C: 5 KPs | 依赖: 03-06 进程/线程/信号/内存 | 读者基线: mutex、共享内存、C++ 基础
> 读者处境: 04 篇讲了 mutex 与线程安全，06 篇讲了地址空间和调度；本篇进一步下沉到多核共享内存：CPU 如何保持 cache line 一致，C++ atomic 如何表达顺序，以及无锁代码为什么容易错
> 打开新视角: 无锁不是“没有成本”，而是**把锁的互斥协议换成原子操作、内存序、重试、缓存一致性和内存回收协议**

---

### 概念依赖链

```
03-06 线程/信号/内存 → 本篇: C++ atomic 与无锁
  ├─ §1 cache coherence/MESI(硬件一致性)
  ├─ §2 硬件/编译器内存顺序(屏障)
  ├─ §3 C++ atomic/memory_order(语言内存模型)
  ├─ §4 CAS/ABA(原子更新与回收)
  ├─ §5 SPSC ring buffer(一个可验证的无锁结构)
  └─ §6 正确性/性能实验(不要把偶发结果当证明)
先讲: cache line → 屏障 → C++ 语义 → CAS/ABA → 队列 → 实验
后续依赖: MySQL 实战(从系统原理进入持久化应用)
```

### 叙事顺序

1. 问题引入——两个线程写共享变量，为什么“每次读写都是一条指令”仍可能得到错误结果？
2. MESI/缓存一致性——缓存行如何在核间转移
3. 屏障与 C++ memory order——硬件保证不等于语言保证
4. CAS 与 ABA——原子更新为什么仍需回收协议
5. SPSC ring buffer——在明确前提下写一个无锁结构
6. 实验——用 litmus/PMU/benchmark 验证而不是猜
7. 收束——无锁代码的审慎边界

### 1. MESI — 多核缓存如何保持同一 cache line 的一致

场景提示: 核 A 修改变量后，核 B 的缓存里还保留旧值；CPU 如何协调这条 cache line？ [写作时展开]

关键设计: MESI 描述缓存行在不同核缓存中的一致性状态；具体协议由 CPU 实现决定：

```[pseudocode]
Modified:
  当前缓存持有最新脏副本

Exclusive:
  只有当前缓存持有, 内容与内存一致

Shared:
  多个缓存可持有只读一致副本

Invalid:
  当前副本不可使用

核 A 写共享行:
  → 请求独占/所有权
  → 其他核副本失效或转移
  → A 获得可写状态并修改
```

Why: 为什么“缓存一致性”不等于“线程按源码顺序观察”？——**一致性协议解决同一 cache line 的值如何最终协调，内存模型还要规定不同地址的访问顺序和可见性**；store buffer、缓存层次、编译器重排和 CPU 弱内存模型仍可能影响观察顺序。MESI/MOESI 等名称也不能代表所有 CPU 完全相同。 [x86/ARM: 一致性协议、内存模型和屏障指令是不同层次的问题]

比喻锚点: 多核缓存像多人各持有同一份文件复印件；写入前要拿到编辑权，并让其他旧复印件失效。 [写作时展开]

### 2. 内存屏障 — 硬件/编译器如何约束顺序

场景提示: 一个线程先写数据再发布 flag，另一个线程看到 flag 后为何仍可能看不到数据？ [写作时展开]

关键设计: 屏障约束的是特定方向的内存访问顺序；C++ 原子语义最终映射到不同架构的指令/屏障：

```[pseudocode]
StoreStore:
  先前 store → 后续 store 的顺序

LoadLoad:
  先前 load → 后续 load 的顺序

LoadStore:
  先前 load → 后续 store 的顺序

StoreLoad:
  先前 store → 后续 load 的顺序, 通常最难/最贵

注意:
  编译器 barrier ≠ CPU hardware fence
  x86、ARM 等架构的默认排序不同
  语言层应优先用 C++ atomic/mutex 表达同步
```

Why: 为什么不能只凭 x86 的“强内存模型”写跨平台代码？——**C++ 程序必须满足语言内存模型，ARM 等弱内存序架构可能暴露 x86 上不明显的重排问题**；直接写 `mfence/dmb` 还会把代码绑定到架构。 [内核: Linux `smp_*mb` 与 C++ `std::atomic` 是不同抽象层，不能互换理解]

比喻锚点: 屏障像仓库闸门，要求前一批货先完成某种交接，再允许后一批货通过；不同仓库的默认调度规则不一样。 [写作时展开]

### 3. C++ atomic 与 memory_order — 让语言表达可见性协议

场景提示: 计数器只需要原子加一，发布一个已初始化对象却需要更强顺序；为何不能所有地方都用同一个 memory order？ [写作时展开]

关键设计: atomic 同时提供不可撕裂的原子操作和可选择的同步关系：

```[pseudocode]
relaxed:
  保证该原子对象的原子性/修改顺序
  不建立普通数据的跨线程同步
  → 独立统计计数等场景

release store + acquire load:
  发布线程先写普通数据
  → release 发布 flag/pointer
  → acquire 读取后看到此前写入
  → 发布/订阅协议

acq_rel:
  同一 RMW 既需要获取又需要释放语义

seq_cst:
  在满足条件的 seq_cst 操作间提供全局顺序
  → 简单但可能有额外约束/成本
```

Why: 为什么 seq_cst 不是“永远最佳”，relaxed 也不是“越快越好”？——**memory_order 是正确性协议的一部分**：放松顺序若没有完整 happens-before 设计，会产生数据竞争或读到未初始化状态；过强顺序则可能限制优化和扩展。`memory_order_consume` 的实现/使用长期受限，实际代码通常采用 acquire。 [C++ memory model: atomic 只保护原子对象，普通共享数据仍需建立正确同步]

比喻锚点: memory_order 像交接单上的签字等级：relaxed 只确认“计数变了”，release/acquire 还确认“此前整批货已交接”，seq_cst 要求所有总仓按同一时间线盖章。 [写作时展开]

### 4. CAS 与 ABA — 原子比较不等于完整并发协议

场景提示: CAS 看见指针仍是 A 就成功，但它真的确认“中间什么都没发生”吗？ [写作时展开]

关键设计: compare_exchange 原子地比较并更新，但 ABA 会让值恢复原样却掩盖中间修改：

```[pseudocode]
expected = old
compare_exchange_weak/strong(expected, desired)
  成功: 当前值等于 expected → 写 desired
  失败: expected 被更新为当前值 → 重试/处理

ABA:
  T1 读 A
  T2 改 A→B→A
  T1 CAS(A→C) 成功
  → 只比较值, 不知道中间发生过 B

可能方案:
  tagged/versioned pointer
  hazard pointer
  epoch/RCU/reclamation protocol
  → 解决“对象仍存活且版本可信”问题
```

Why: 为什么 `compare_exchange_weak` 常用于循环而 strong 也不是“绝对更安全”？——**weak 允许无真实冲突的伪失败，循环可重试；strong 减少伪失败但仍需要处理真实竞争、ABA 和对象生命周期**。tagged pointer 还受指针位宽/对齐/平台地址规则约束，hazard pointer/RCU 则需要完整回收协议。 [C++ atomic: CAS 的 expected 更新语义与 memory_order 组合必须满足 API 约束]

比喻锚点: CAS 像核对门牌后换锁；如果门牌从 A 换成 B 又换回 A，单看门牌无法知道房屋中间是否已换过住户。 [写作时展开]

### 5. SPSC ring buffer — 在明确前提下实现无锁

场景提示: 单生产者、单消费者队列为何可以不用 CAS？换成多个生产者后哪里会失效？ [写作时展开]

关键设计: SPSC 让 head/tail 各自由一个角色写，另一角色只读；release/acquire 发布数据和空间：

```[pseudocode]
共享:
  T buffer[N]
  atomic<size_t> head // producer 写, consumer 读
  atomic<size_t> tail // consumer 写, producer 读

push(x):
  读 tail 判断满
  buffer[head % N] = x
  head.store(head + 1, release)

pop():
  读 head 判断空
  acquire 读取 head
  x = buffer[tail % N]
  tail.store(tail + 1, release)
  return x

前提:
  exactly one producer + one consumer
  环形索引溢出/容量策略正确
  生命周期和对象构造析构正确
```

Why: 为什么 SPSC 能不用 CAS，而 MPSC/MPMC 通常需要更复杂协议？——**SPSC 的每个索引只有一个写者，避免了多个生产者争抢同一 head**；一旦参与者数量改变，简单 load/store 不能保护 slot 分配、顺序和回收。无锁也不等于无等待，满/空时仍可能自旋或退避。 [内核: lock-free 队列的正确性取决于内存序、缓存一致性、对象生命周期和参与者模型]

比喻锚点: SPSC 像一条只有一个装货工和一个卸货工的传送带，各自只移动一端指针；增加第二个装货工后，两个人会抢同一个空位。 [写作时展开]

### 6. 实验与收束 — 不要用一次偶发结果证明内存模型

场景提示: litmus 测试几亿次没观察到重排，是否就证明重排不可能？ [写作时展开]

关键设计: 内存模型实验应验证语言正确性和硬件现象边界，不把“没观察到”当作证明：

```[pseudocode]
实验控制:
  明确 relaxed/acquire-release/seq_cst 对照
  固定线程亲和性/编译器/架构/优化级别
  记录 cycles/结果组合/失败次数

解释:
  观察到结果 → 说明执行允许该结果
  未观察到结果 → 可能是概率低/环境未触发
  C++ 未定义行为 → 不能靠跑实验证明安全

验证:
  ThreadSanitizer/静态分析/模型推理
  → benchmark 评估性能, 不是正确性证明
```

**Aha Moment**: "无锁代码的难点不是把 mutex 换成 CAS，而是同时满足**语言内存模型、硬件一致性、对象生命周期、ABA/回收和参与者前提**；任何一层缺失，‘看起来能跑’都不等于正确。"
**回答读者三问**: ①atomic 解决什么=原子操作和明确同步语义；②CAS 为什么仍会错=ABA、回收、内存序和竞争；③SPSC 为什么能无锁=单写者前提让索引更新不争抢。

---

### 核心悬念

**"系统编程阶段已经完成：从 fd、mmap、进程、线程、信号到 atomic；下一步进入应用实战，数据库、缓存和消息队列如何把这些底层原理组合成持久化系统？"**

→ 引出 MySQL 实战 — 从系统原理进入持久化存储与数据库。