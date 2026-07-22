# Stage 3：并发编程

> **本 stage 定位**：C 语言学习路线的第三阶段，C 公认最具挑战性的领域之一——并发编程。对应 `C-C++技术专家学习路线.md` 的 **Layer 5**（pthread / 同步原语 / 原子操作 / 内存序 / 无锁数据结构）。
>
> **预计时间**：2 周（每日 2-3 小时投入，比其他 stage 多投入）
>
> **完成本 stage 后**：能用 pthread 写多线程程序、理解 5 大同步原语（mutex/spinlock/condvar/rwlock/semaphore）的适用场景、能讲清楚 C11 原子操作与 5 种内存序、能解释 x86-TSO vs ARM weak memory model 的差异、能写一个无锁 MPSC 队列。

---

## 1. 学习目标

完成本 stage 后，应能达成以下 6 个目标：

1. **能讲清楚线程 vs 进程的 trade-off**（地址空间 / 创建开销 / 通信方式 / 同步 / 崩溃影响）+ 何时选进程（Nginx 模型）何时选线程（Memcached 模型）
2. **能用 pthread API 写多线程程序**（create / join / detach / exit / cancel）
3. **能讲清楚 5 大同步原语的适用场景**（mutex / spinlock / condvar / rwlock / semaphore / barrier）+ condvar 的三个"为什么"（必须配合 mutex / 用 while 不用 if / signal 时机灵活）
4. **能用 C11 `<stdatomic.h>` 写原子操作**，理解 5 种内存序（relaxed / acquire / release / acq_rel / seq_cst）的含义与适用
5. **能解释 x86-TSO vs ARM weak memory model 的差异**，回答"为什么 x86 上不加内存屏障的多线程代码可能'碰巧'工作，移植到 ARM 后大量 bug 暴露"
6. **能写一个无锁 MPSC 队列**，理解 ABA 问题与 hazard pointer / epoch-based reclamation 内存回收方案

---

## 2. 对应 Layer

| Layer | 主题 | 本 stage 章节 |
|---|---|---|
| Layer 5 | 并发编程 | `01-线程与进程.md` / `02-pthread API与同步原语.md` / `03-原子操作与内存序.md` / `04-内存模型x86-TSO-vs-ARM.md` / `05-死锁检测与无锁数据结构.md` |

---

## 3. 学习内容（章节清单）

### 3.1 线程与进程

> **前置说明**：本节中的"进程"指操作系统进程的通用概念（Java 工程师可从 JVM 进程类比理解地址空间隔离、创建开销等），不依赖 fork/exec 等创建 API 的具体知识。Nginx worker 进程模型的分支策略等细节详见 **Stage 4**（OS 接口——进程与信号）。

- 线程 vs 进程的 5 维度对比（地址空间 / 创建开销 / 通信方式 / 同步 / 崩溃影响）
- 何时选进程（Nginx 模型：隔离性要求高 / 共享数据少 / 调试方便）
- 何时选线程（Memcached 模型：共享数据多 / 创建销毁频繁 / 性能要求高）

### 3.2 pthread API 与同步原语

- pthread API 全解（create / join / detach / exit / self / cancel）
- 5 大同步原语对比（mutex / spinlock / condvar / rwlock / semaphore / barrier）
- mutex 核心模式 + condvar 核心模式（生产者-消费者）+ 三个"为什么"
- 同步原语的性能特征（mutex 中等 / spinlock 高但忙等 / condvar 中等 / rwlock 读多写少）

### 3.3 原子操作与内存序

- C11 `<stdatomic.h>` 原子操作（`atomic_fetch_add` 等）
- 5 种内存序含义（relaxed / acquire / release / acq_rel / seq_cst）+ 适用场景
- 经典模式（自旋锁的 acquire/release 配合）
- 为什么 `acquire` 配合 `release`（happens-before 关系）
- 为什么 `memory_order_relaxed` 不够（只保证原子性不保证可见性）

### 3.4 内存模型：x86-TSO vs ARM weak

- x86-TSO（Total Store Order）— 主流 CPU 中**最强的**内存模型（store 不重排、load 不重排，但**允许 Store-Load 重排**，并非完全顺序一致）
- 在 x86 上，`acquire`/`release` 编译为普通 load/store（零额外成本）；`seq_cst` store 编译为 `xchg`/`mov+mfence`（有实质代价 ~20-40 cycles）
- ARM weak memory model — 大部分 load/store 可重排
- 内存屏障类型（mfence / sfence / lfence / dmb / dsb / isb）
- 实际影响：x86 上"碰巧"工作的代码移植到 ARM 后 bug 暴露（Apple Silicon 迁移时大量软件出 bug 的原因）

### 3.5 死锁检测与无锁数据结构

- 死锁四条件（互斥 / 持有并等待 / 不可剥夺 / 循环等待）+ 避免方法
- 调试工具：helgrind / ThreadSanitizer (TSan)
- 无锁数据结构入门（MPSC 队列 — Vyukov 算法）
- ABA 问题 + hazard pointer / epoch-based reclamation 内存回收方案

---

## 4. 实战项目

1. **实现线程池**（fixed-size worker pool + task queue）
2. **实现无锁 MPSC 队列**（参考 Dmitry Vyukov 的实现）— **注意**：必须初始化哨兵节点，否则首次 enqueue 解引用 NULL；dequeue 端可能短暂读到 `t->next == NULL`，需返回 EAGAIN 或自旋等待
3. **用 `helgrind` / TSan 检测多线程程序的竞争**：写一个有数据竞争的程序，用工具定位
4. **写一个并发哈希表**（分桶锁 vs 全局锁，对比性能）
5. **测试 x86 vs ARM 内存模型差异**：写一个依赖内存序的程序，在 x86 上用 `seq_cst` 和 `relaxed` 跑对比，理解为什么 x86-TSO 提供了比 relaxed 更强的硬件保证（如 store-store 保序），但依赖这些保证不是可移植行为

---

## 5. 参考书章节

| 主题 | 极致C | Fluent C | Practical | Mastering | 高效调试 | Memory Thinking |
|---|---|---|---|---|---|---|
| 线程与进程 | Ch13, 17 | — | — | — | — | 部分 |
| pthread API 与同步原语 | Ch14-16, 18 | — | — | Ch7 | Ch7 | 部分 |
| 原子操作与内存序 | Ch14 | — | — | — | — | — |
| 内存模型 | — | — | — | — | — | — |
| 死锁与无锁数据结构 | — | — | — | — | Ch7 | — |

**重点参考**：
- 极致C Ch13-18（并发 / 同步 / 线程执行 / 线程同步 / 进程执行 / 进程同步 — 最系统的并发讲解）
- Mastering Ch7（同步技术 — 进阶视角）
- 高效调试 Ch7（竞争条件 + 死锁 — 调试视角看并发 bug）

**注**：内存模型（x86-TSO vs ARM weak）6 本书覆盖不足，需补充外部资料：
- 《C++ Concurrency in Action》Anthony Williams（C++ 但内存模型部分与 C11 共享）
- 《Is Parallel Programming Hard, And, If So, What Can You Do About It?》Paul E. McKenney（Linux 内核 RCU 作者，免费 PDF）

---

## 6. 完成标准

能回答以下 10 个问题即算完成：

1. 线程 vs 进程的 5 维度对比？Nginx 为什么选进程，Memcached 为什么选线程？
2. pthread API 的核心函数有哪些？`pthread_cond_wait` 为什么必须配合 mutex？
3. 5 大同步原语（mutex / spinlock / condvar / rwlock / semaphore）的适用场景？
4. condvar 的三个"为什么"（必须配合 mutex / 用 while 不用 if / signal 时机灵活）？
5. C11 `<stdatomic.h>` 提供哪些原子操作？5 种内存序的含义？
6. 为什么 `acquire` 配合 `release`？`memory_order_relaxed` 为什么不够？
7. x86-TSO vs ARM weak memory model 的差异？x86 上哪些 C11 内存序零额外成本、哪些有代价？
8. 内存屏障类型有哪些？x86 和 ARM 各有哪些指令？
9. 死锁四条件是什么？如何避免？
10. ABA 问题是什么？hazard pointer 和 epoch-based reclamation 如何解决？

---

## 7. 下一步

完成本 stage 后，进入 **`stage4-系统编程/`**（对应 Layer 4, 6, 7：OS 接口 + 网络编程 + 数据结构自己写）。

stage4 将深入系统编程核心，理解 syscall / 文件 I/O / 进程 / 信号 / IPC / mmap / epoll，写一个并发 HTTP 服务器，并自己实现 SDS / 哈希表 / 跳表 / 内存池等数据结构。
