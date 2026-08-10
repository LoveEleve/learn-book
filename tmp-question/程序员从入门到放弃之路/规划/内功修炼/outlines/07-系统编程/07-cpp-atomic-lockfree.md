# C++ Atomic + 无锁编程 — MESI/内存屏障/CAS/无锁队列/CPU 黑盒测试

> Cluster C: 5 KPs | 依赖: B (03-06: 进程+线程+信号+内存) | 读者基线: 理解多线程和 mutex，知道 volatile 但不能解释为什么 volatile 不保证原子性

---

### 1. MESI 缓存一致性协议 — CPU 多核写一变量怎么同步
  - 四状态: Modified(脏, 唯我有, 写回前独有) / Exclusive(干净, 唯我有) / Shared(多核共享, 只读) / Invalid(缓存行失效) (B2 Ch1 §2-3)
  - 状态转换: 核心 A 写 M → 发 RFO(Request For Ownership) 到其他核 → 其他核 Invalid → A 拿到 Exclusive → 写入后变 Modified (B2 Ch1 §2-4)
  - 写缓冲区(Store Buffer): CPU 写不直接到 L1→先入 store buffer(异步)→避免停顿——但这打破了多核看的顺序(Store-Load 重排) (B2 Ch1 §3-4)
  - 无效队列(Invalidate Queue): 核心在忙时不处理 incoming invalid→先入队列→延迟失效——写者以为已 invalid 但读者没收到 (B2 Ch1 §3-5)
  - 屏障根源: Store Buffer+Invalidate Queue = 顺序破坏之源——硬件从不保证"一核写入另一核同时看到"; MOESI(AMD: 增加 O-Owned 状态)变体 (B2 Ch1 §2-5)

### 2. 内存屏障 — 四种类型的硬件保证
  - StoreLoad(最强): Store 前的所有 store 对其他核可见后, 才执行后续 load → `mfence` → x86 上 `lock; addl $0, 0(%rsp)` —— 最贵(100+ cycles) (B2 Ch1 §4-5)
  - StoreStore: 前面的 store 先于后面的 store → `wmb` → 写序保证 (B2 Ch1 §4)
  - LoadLoad: 前面的 load 先于后面的 load → `rmb` → 读序保证 (B2 Ch1 §4)
  - LoadStore: 前面的 load 先于后面的 store → x86 默认保证(唯一无需显式写 barrier) (B2 Ch1 §4)
  - x86-TSO(Total Store Order): x86 内存模型 = TSO — 所有 store 必序(store-store order guaranteed) + 写对所有核可见有全局序 — 比 ARM(弱一致模型)简单 (B2 Ch1 §5)
  - ARM 弱模型: store-load 重排 + store-store 不保证 → `dmb`(全屏障)/`dsb`(数据同步屏障)/`isb`(指令同步屏障) — `dmb sy` ≈ full fence

### 3. C++ atomic — 六种 memory_order 实战选择
  - `atomic<T>` 模板: `load/store/exchange/compare_exchange_strong/compare_exchange_weak/fetch_add` — 所有操作原子+可指定 memory_order (B2 Ch1 §7)
  - `memory_order_relaxed`: 只保证原子性, 不保证顺序 → 计数器(位置无关) → x86 无额外指令; `consume` 已废弃, 用 acquire 替代 (B2 Ch1 §8)
  - `memory_order_acquire`: load 后能看到前一个 release store 的所有写入 → spinlock lock (B2 Ch1 §8)
  - `memory_order_release`: store 前所有写入对后续 acquire load 可见 → spinlock unlock (B2 Ch1 §8)
  - `memory_order_acq_rel`: fetch_add 天然需要两边; `memory_order_seq_cst`: 全局全序默认 → x86 无额外成本(TSO≈seq_cst)→首选 (B2 Ch1 §8)

### 4. CAS — compare_exchange_strong/weak + ABA 问题
  - `bool compare_exchange_strong(T& expected, T desired)`: 原子比较-交换——`*this == expected ? (*this = desired, true) : (expected = *this, false)` (B2 Ch1 §7)
  - strong vs weak: strong 可能伪失败(即使相等也失败——硬件不保证——但上层保证重试) vs weak 可因伪条件失败 — weak 用于 loop, strong 用于单次判断 (B2 Ch1 §7)
  - ABA 问题: 线程1读A→暂停→线程2改A→B→B→A→线程1 CAS 成功(数据中间有修改)→解决: tagged pointer(64位指针+版本号)/RCU/hazard pointer (B2 Ch1 §9)

### 5. 无锁队列 — SPSC Ring Buffer
  - SPSC(单生产者单消费者): `T buf[N]` + `atomic<size_t> head`(写指针) + `atomic<size_t> tail`(读指针) — 头尾只在生产/消费各自写, 对方读 (B2 Ch1 §9)
  - push: `while (head - tail == N) spin` → `buf[head % N] = x` → `head.store(head+1, release)` — 数据可见在指针之后
  - pop: `while (head == tail) spin` → `T x = buf[tail % N]` → `tail.store(tail+1, acquire)` — 读指针后才可读数据
  - 为什么无锁: 两个 atomic 变量各被一人写——无共享写 → 无 CAS 竞争 → 仅 load/acquire + store/release 就够 — LMAX Disruptor 的简化原型 (B2 Ch1 §9)

### 6. CPU 黑盒测试 — StoreForwarding 与乱序实测
  - Store Forwarding: `mov [x], 1 ; mov r, [x]` (同核心写后读) — CPU 从 store buffer 转发值到 load → 不等待写入 L1 → ~4 周期(非 ~100) (B2 Ch1 §6)
  - 乱序实证: 线程1 `x.store(1);y.store(2)` vs 线程2 `r1=y;r2=x` → r1=2,r2=0 可能出现(x86 极少)——证明处理器可重排无依赖 store/load (B2 Ch1 §6)
  - 实验设计: 循环 10^9 次检测双线程"违反直觉"结果组合, 统计频次验证CPU重排几率; `rdtsc`(TSC~10ns精度但跨核心偏移)vs `clock_gettime`(~50ns绝对可靠)—benchmark用 MONOTONIC (B2 Ch1 §6-8)

### 7. 收束
  - MESI→内存屏障→C++ atomic 是一条垂直知识链：硬件从"写一个变量"开始需要的成本→屏障为什么有四种→C++ 怎么把屏障暴露为可组合的 memory_order
  - 写无锁代码的第一条准则：**不要写**。第二条准则：如果必须写，用 seq_cst 验证正确后再逐步放松; 用 `compare_exchange_weak` 而不是 `strong`
  - **内功修炼 7 卷 73 篇走到这里结束了**：从 OS 内核内存到 MESI 缓存一致性，从文件系统到 eBPF，从网络堆栈到系统性能，从系统调用到 C++ atomic——现在你有了"向下看到硬件、向上看到 API"的全链路视野

---

### 核心悬念
**"内功修炼的最后一篇到这里了——但编程不只是系统调用和无锁队列。真正的应用开发呢？数据库、缓存、消息队列——怎么从原理层进入实战层？"**

→ 引出 MySQL 实战 — 从系统原理到持久化存储的第一站
