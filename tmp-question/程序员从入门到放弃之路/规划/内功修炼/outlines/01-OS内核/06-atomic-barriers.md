# 原子操作 — LOCK 前缀 + CAS + 内存屏障(mfence/lfence/sfence)

> Cluster B: 6 KPs | 依赖: 05-MESI 缓存一致性 | 读者基线: 理解 MESI 和 Store Buffer 概念
> 读者处境: 已读完 05 篇，知道多核缓存一致性与 store buffer 的乱序问题；本篇回答"如何在多核上实现'不可分割'的操作"
> 打开新视角: 原子性的硬件来源、CAS 自旋模式与 ABA 陷阱、屏障的两层体系（编译器+CPU）、x86 为何比 ARM 省屏障

---

### 概念依赖链

```
05-MESI(缓存一致性 + store buffer 乱序) → 本篇: 原子操作与屏障
  ├─ §1 LOCK 前缀(RMW 原子性的硬件来源 — 依赖 05 的缓存行机制)
  │    └─ §2 CAS 自旋(用 §1 构建的自旋模式 — 依赖 §1 cmpxchg)
  ├─ §3 编译器屏障(防止编译期重排 — 独立于硬件)
  │    └─ §4 CPU 屏障(修复 05 的 store buffer 乱序 — 依赖 05 §2)
  │         └─ §5 内存模型(不同 CPU 的屏障需求差异 — 依赖 §4)
先讲: 原子性(硬件) → 自旋(应用) → 编译期(软件) → 运行期(硬件) → 模型(差异)
后续依赖: 07-锁家族(自旋锁/mutex 构建于此)
```

### 叙事顺序

1. 问题引入——`i++` 在汇编里是"读-改-写"三步，多核下如何变成一步？（**Aha: 原子性是硬件指令给的，不是语言给的**）
   - 过渡: 一条指令怎么保证多核原子？——LOCK 前缀
2. LOCK 前缀——锁总线→锁缓存行；atomic_*/cmpxchg/cmpxchg16b
   - 过渡: 有了原子指令——怎么把它变成"自旋等待"的通用模式？
3. CAS 自旋——do-while 循环；ABA 问题与解决
   - 过渡: 原子操作解决了硬件层——编译期会不会乱序？
4. 编译器屏障——barrier()/READ_ONCE/WRITE_ONCE/volatile
   - 过渡: 编译期不乱序了——CPU 运行期呢（05 的 store buffer）？
5. CPU 屏障——StoreLoad/StoreStore/LoadLoad+LoadStore；Linux smp_mb 封装
   - 过渡: 不同 CPU 的屏障需求一样吗？——内存模型
6. 内存模型——x86 TSO vs ARM 弱模型；屏障数量差异
   - 过渡: 原子+屏障已齐——内核怎么在上面构建锁？07 篇
7. 收束——原子性(硬件)/自旋(模式)/两层屏障(编译+运行)/模型(差异)

### 1. LOCK 前缀 — 原子性从何而来

场景提示: 多核 `i++` 结果丢失——"读-改-写"三步被交错；atomic 系列为何无此问题。 [写作时展开]

关键设计: x86 用 **LOCK 前缀**把 RMW（Read-Modify-Write）变成原子操作：

```[pseudocode]
老 CPU:   LOCK 锁总线（所有内存访问被串行化）
现代 CPU: LOCK 锁缓存行（通过 MESI 独占该行, 其他核无法同时访问）
→ LOCK; addl $1, (%rax) 一条指令 = 多核原子
代价: ~数十 cycles（锁缓存行 vs 无锁 1 cycle）
```

关键指令 (arch/x86/include/asm):
- `atomic_inc/dec/add/sub`: `LOCK; addl $1, (%rax)`——一条指令不可中断
- `cmpxchg`: `LOCK; cmpxchg %rcx, (%rax)`——`*rax == old` 则写 new，返回旧值——CAS 的基础
- `cmpxchg16b`: 128 位 DWCAS——无锁数据结构（无锁队列）需要同时改两个指针时用

Why: 为什么"一条指令"就原子？——CPU 的中断/异常在**指令边界**处理，单条指令的执行本身不可分割。普通指令在单核上天然原子；多核的问题是"另一核同时改同一内存"。LOCK 前缀让 RMW 三步（读-改-写）对其他核不可见地完成——其他核要么看到旧值，要么看到新值，绝无中间态。 [x86: LOCK 锁缓存行代价 ~数十 cycles，无锁访问 1 cycle——原子性有价]

比喻锚点: LOCK 前缀=银行柜台加锁窗口——柜台（内存地址）挂锁期间只服务一个客户（核），办完（RMW 完成）才解锁；其他客户看到要么未办要么办完，看不到办到一半。 [写作时展开]

### 2. CAS 自旋 — 自旋等待的原子版本

场景提示: 计数器的原子递增——`atomic_inc` 是专用指令，但"基于当前值的一般更新"（入栈/替换指针）怎么办？ [写作时展开]

关键设计: CAS 自旋模式：

```[pseudocode]
do {
    old = atomic_read(v);   // 读当前值
    new = old + 1;          // 计算新值
} while (!cmpxchg(v, old, new));  // 尝试原子更新, 失败(值变了)则重试
```

- 非阻塞：失败立即重试，不睡眠
- `xchg`（原子交换）: 无条件写新值返回旧值——比 CAS 简单，spin lock 用 `xchg(&lock->val, 1)` 占锁
- ABA 问题: 期间值被 A→B→A，CAS 看不出变化——解决: tagged pointer（指针+版本号）/ 64 位拆位

比喻锚点: CAS 自旋=试衣间门锁——进门先试（读 old），出来锁门（CAS）：门锁显示"已占用"则重试另一间（循环）；若期间有人进过又出（ABA），只看"门锁状态"看不出来，需在钥匙上加"访问次数"（版本号）。 [写作时展开]

Why: 为什么不用 `atomic_inc` 而要手写 CAS 循环？——`atomic_inc` 本身**编译成单条 LOCK 指令**（`LOCK; addl $1`），只适用于递增；而 CAS 是**通用原子原语**：任何"基于当前值的更新"（入栈/出栈/替换指针）都能用"读旧值-算新值-CAS 写回"表达，编译器无法把 do-while 循环优化成单条指令。自旋锁、无锁栈、无锁队列全部构建于 CAS。

### 3. 编译器屏障 — 禁止重排序的第一步

场景提示: 两个线程的共享标志位——明明代码顺序是先写后读，实际却读到了旧值；问题可能在编译期。 [写作时展开]

关键设计: 编译器（GCC/Clang）会重排无依赖的语句以优化流水线——单线程无感知，多线程共享变量时破坏顺序：

| 工具 | 作用 | 局限 |
|------|------|------|
| `barrier()` | 编译器屏障：禁止跨屏障重排 | 不生成 CPU 指令，不能阻止 CPU 乱序 |
| `READ_ONCE/WRITE_ONCE` | 禁止撕裂读写（64bit 读不被拆成两次 32bit）→ 编译为单条 load/store | 不保证 CPU 级顺序 |
| `volatile` | 禁止优化（每次都读内存） | 过度同步——连无关优化也禁止 |

Why: 为什么要分"编译器屏障"和"CPU 屏障"两层？——乱序有两个来源：编译期（软件重排，GCC 优化）和运行期（硬件乱序，store buffer/流水线）。`barrier()` 治编译期，`mfence` 治运行期——**两层都必须防**。这正是"为什么 volatile 不够"的原因：volatile 只防编译器，防不了 CPU。

比喻锚点: 编译器屏障=交警贴"禁止变道"告示（软件规则，不设路障）；CPU 屏障=路口红绿灯（硬件强制排队）。两道关卡都要有，只贴告示（volatile/barrier）挡不住闯红灯的（CPU 乱序）。 [写作时展开]

### 4. CPU 内存屏障 — mfence/lfence/sfence

场景提示: 05 篇的 store buffer——写进了缓冲还没落缓存，其他核读旧值；屏障怎么强制顺序？ [写作时展开]

关键设计: 四种屏障按"修复哪种乱序"分类 (arch/x86/include/asm/barrier.h)：

| 屏障 | 修复的顺序 | 指令 | 代价 |
|------|-----------|------|------|
| StoreLoad | 写→读（最贵） | `mfence` | ~100+ cycles |
| StoreStore | 写→写 | `sfence` | 轻量 |
| LoadLoad + LoadStore | 读→读 / 读→写 | `lfence` | 轻量 |

- StoreLoad 最贵：要等 store buffer 排空（所有 pending store 全局可见）后，后续 load 才能执行——这正是 05 篇 store buffer 引入的乱序
- Linux 封装: `smp_mb()/smp_rmb()/smp_wmb()/smp_read_barrier_depends()`——SMP 编译为真实屏障，UP 编译为 `barrier()`

Why: 为什么 StoreLoad 最贵而 StoreStore 便宜？——StoreStore 只需等写合并缓冲排空（本核内顺序）；StoreLoad 要跨核（等我的写对别人可见，再看别人的写）——涉及 store buffer 与 invalidate-ack 的完整往返，所以 ~100+ cycles。 [x86: mfence ~100+ cycles，sfence/lfence 数十 cycles——屏障代价随"跨核程度"递增]

比喻锚点: CPU 屏障=仓库出库顺序管控——StoreStore 只是"本仓出库排队"（sfence 轻量）；StoreLoad 是"我出库后要确认别人收到再收货"（mfence 贵）——跨库确认必然慢。 [写作时展开]

### 5. 内存模型 — x86 TSO vs ARM 弱模型

场景提示: 同一段无锁代码在 x86 上正确、在 ARM 上错——屏障需求因 CPU 而异。 [写作时展开]

关键设计:

| 模型 | 保证 | 屏障需求 |
|------|------|---------|
| x86 TSO | store-store 有序、load-load 有序 | 只需 mfence（StoreLoad）+ sfence（写合并） |
| ARM/Power 弱模型 | 一切可能乱序 | 每次原子操作后 dmb；读写都要屏障 |

Linux 原则: 用 `smp_mb()` 而非 x86 特定屏障——内核代码跨平台，屏障与数据依赖匹配（需要什么顺序就放什么屏障），由架构层映射到实际指令。

Why: 为什么 x86 能保证 store-store 有序而 ARM 不能？——x86 的 store 经 store buffer 且 FIFO 提交（硬件保证写顺序）；ARM 为降低硬件复杂度/功耗允许无保证。这是**硬件设计哲学**差异：x86 把顺序义务放硬件（软件省心但硬件贵），ARM 把义务放软件（硬件简单但软件难写）。 [x86: store 经 store buffer FIFO 提交保序; ARM: 无保证需 dmb——同一段无锁代码跨架构正确性依赖屏障]

比喻锚点: 内存模型=交规——x86 是"司机优先"（交规严格，司机（软件）省心但道路（硬件）造价高）；ARM 是"行人优先"（交规宽松，司机（软件）自己小心）。 [写作时展开]

### 6. 收束

回到 `i++` 多核场景：
- LOCK 前缀 = 硬件原子性（锁缓存行）
- CAS 自旋 = 原子原语的应用模式（ABA 陷阱）
- 编译器屏障 = 防编译期重排（barrier/READ_ONCE）
- CPU 屏障 = 防运行期乱序（mfence/sfence/lfence）
- 内存模型 = 屏障需求的架构差异（TSO vs 弱模型）

**Aha Moment**: "原子性不是语言特性，是硬件指令（LOCK 前缀）给的；而'顺序'需要两层防线——编译器重排（barrier）和 CPU 乱序（mfence）。x86 把顺序保证放在硬件里，ARM 让软件负责——所以同样的无锁代码，换个架构可能就错了。"
**回答读者三问**: ①i++ 丢更新=LOCK 前缀缺失；②自旋锁为何用 xchg=原子交换占锁；③x86 无锁代码 ARM 出错=内存模型差异。

---

### 核心悬念

**"CAS 自旋有 ABA 问题，MESI 有缓存行乒乓 — 那内核怎么在这些底层原子操作上构建生产级的锁？从 spinlock 到 mutex 到 RCU 都是什么？"**

→ 引出 07-锁家族——原子操作是砖，锁是墙；从自旋到睡眠的完整锁体系。
