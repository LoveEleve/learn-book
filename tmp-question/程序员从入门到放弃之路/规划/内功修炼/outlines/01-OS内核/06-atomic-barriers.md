# 原子操作 — LOCK 前缀 + CAS + 内存屏障(mfence/lfence/sfence)

> Cluster B: 6 KPs | 依赖: 05-MESI 缓存一致性 | 读者基线: 理解 MESI 和 Store Buffer 概念

---

### 1. LOCK 前缀 — 原子性从何而来
  - LOCK 前缀机制: 锁总线(老 CPU)或锁缓存行(现代 CPU 通过 MESI 保证) → 确保 RMW(Read-Modify-Write)原子可见 → 代价 ~数十 cycles(锁缓存行) (arch/x86/include/asm/atomic.h:22)
  - `atomic_add/atomic_sub/atomic_inc/atomic_dec`: `LOCK; addl $1, (%rax)` → 一条指令不可中断 → 多核原子 (arch/x86/include/asm/atomic.h:50)
  - `cmpxchg`: `LOCK; cmpxchg %rcx, (%rax)` → 如果 `*rax == old` 则 `*rax = new` → 返回旧值 → CAS 自旋的基础 (arch/x86/include/asm/cmpxchg.h:127)
  - `cmpxchg16b`: 16 字节 CAS(128 位) → DWCAS → 用于无锁数据结构(无锁队列) → `CMPXCHG16B` 指令 (arch/x86/include/asm/cmpxchg_64.h:24)

### 2. CAS 自旋 — 自旋等待的原子版本
  - CAS 循环: `do { old = atomic_read(v); new = old+1; } while (!cmpxchg(v, old, new));` → 非阻塞 → ABA 问题(old 被改过又改回, CAS 看不出) → 解决: tagged pointer / 版本号 (include/linux/atomic/atomic-instrumented.h)
  - `xchg` 原子交换: 无条件写入新值返回旧值 → 比 CAS 简单 → 用于 spin lock 的 `xchg(&lock->val, 1)` 占锁
  - CAS 扩展: `atomic_cmpxchg` 出错版本 → Linux 内核统一用 `cmpxchg` + LOCK 前缀

### 3. 编译器屏障 — 禁止重排序的第一步
  - `barrier()`: 编译器优化屏障 → 不生成 CPU 指令 → 禁止编译器跨屏障重排序(代码顺序保证) → 不能阻止 CPU 乱序 (include/linux/compiler.h:300)
  - `READ_ONCE/WRITE_ONCE`: 禁止撕裂读/写(一次读 64bit 被编译器拆成 2 次 32bit) → 编译为单条 load/store → 不保证 CPU 级顺序 (include/linux/compiler.h:235)
  - `volatile` vs READ_ONCE: volatile 过度同步(禁止优化) → READ_ONCE 更精准(只禁止撕裂+重排跨此变量)

### 4. 四种 CPU 内存屏障 — mfence/lfence/sfence
  - StoreLoad(最贵): `mfence` → Store Buffer 刷新(等待所有 pending store 完成) → 后继 load 对所有核可见前 store → ~100+ cycles (arch/x86/include/asm/barrier.h:38)
  - StoreStore: `sfence` → 前 store 可见后 → 后继 store 才能执行 → Write-Combining Buffer 刷新 → 轻量
  - LoadLoad + LoadStore: `lfence` → 前 load 完成 → 后继 load/store 才能开始 → 轻量 (arch/x86/include/asm/barrier.h:30)
  - Linux 封装: `smp_mb()/smp_rmb()/smp_wmb()/smp_read_barrier_depends()` → SMP 编译为真实屏障 / UP 编译为 `barrier()` → 内核通用 API (include/asm-generic/barrier.h:68)

### 5. 内存模型 — x86 TSO vs ARM 弱模型
  - x86 TSO: store-store 有序(cache line 写顺序), load-load 有序 → 只需要 `mfence`(StoreLoad) 和 `sfence`(写合并) → 比其他平台少用
  - ARM/Power: 弱模型 → store-store 无序 / load-load 可能乱 → 每个内核原子操作后需要 `dmb` 屏障
  - Linux 屏障原则: 用 `smp_mb()` 而非 x86 特定屏障 → 代码跨平台 → 屏障应与数据依赖匹配

### 6. 收束
  - LOCK 前缀 = 硬件原子操作的实现，CAS = LOCK + cmpxchg 的自旋模式
  - 编译器屏障防止重排，CPU 屏障防止乱序 → 两层分层设计
  - x86 TSO 模型使屏障需求小(只需 StoreLoad 和 StoreStore)但 ARM 弱模型需要更多屏障

---

### 核心悬念
**"CAS 自旋有 ABA 问题，MESI 有缓存行乒乓 — 那内核怎么在这些底层原子操作上构建生产级的锁？从 spinlock 到 mutex 到 RCU 都是什么？"**

→ 引出 07-互斥锁 + 自旋锁/qspinlock + 读写锁 + 信号量 + futex + seqlock
