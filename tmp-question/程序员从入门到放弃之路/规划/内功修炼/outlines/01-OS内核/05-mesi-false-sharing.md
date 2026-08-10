# MESI 缓存一致性 + 伪共享 + Store Buffer + 内存模型

> Cluster B: 5 KPs | 依赖: 01-物理内存管理 | 读者基线: 理解多核 CPU 共享主存的基础概念

---

### 1. MESI 四种状态 — 每个缓存行的身份
  - Modified(已修改, 独占): 本核修改过, 主存已过时, 其他核 Invalid → 只有唯一缓存副本 (arch/x86/include/asm/atomic.h:22)
  - Exclusive(独占, 干净): 本核独占, 与主存一致 → 可直接 Modify 无需通知总线 (arch/x86/include/asm/processor.h → X86_FEATURE constant area)
  - Shared(共享): 多核有副本, 与主存一致 → 需先 Invalidate 其他核才能 Modify (Documentation/memory-barriers.txt → CACHE COHERENCY)
  - Invalid(无效): 被其他核 Invalidate → 访问需重新从主存或其他核读取
  - 状态转换: Local Read(M→M/S→S/I→E via Read Miss) → Local Write(S→I→M or E→M) → Remote Read(M→S, 写回主存) → Remote Write(任何→I) — snooping 总线嗅探每个事务 (Documentation/memory-barriers.txt → CACHE COHERENCY)

### 2. Store Buffer — 写操作不必等待
  - Store Buffer 原理: CPU 写不等待 invalidate-ack → 先写 store buffer → 继续执行后续指令 → Store Forwarding(本核读先查 store buffer, 若命中直接返回) → StoreLoad 屏障清空 (Documentation/memory-barriers.txt:356)
  - TSO(Total Store Order): x86 的内存模型 → 写被 store buffer 缓冲 → 读可越过写 → 其他核看到"乱序写" → 需要 `mfence` 强制全局可见 (arch/x86/include/asm/barrier.h → __smp_mb/mb)
  - Store Buffer 容量: ~几十项 → 满了必须等待 → 性能悬崖

### 3. 伪共享 — 最隐蔽的性能杀手
  - 发生条件: 两个 CPU 写不同变量但位于同一缓存行(64 字节=`L1_CACHE_BYTES`) → 一个写导致另一 CPU 的缓存行 Invalid → MESI 往返 → 无法并行 → 性能暴跌 10-100x
  - 检测: `perf c2c`(cache-to-cache) → HITM 计数(Hit In Modified → 本核读时另一核是 Modified → 必然发生过伪共享) → 热点缓存行地址 (tools/perf/builtin-c2c.c → perf_c2c__hists_browser)
  - 修复: `____cacheline_aligned`(内核, `include/linux/cache.h → ____cacheline_aligned`) → padding 填充到 64 字节边界 → Java `@Contended` → `__attribute__((aligned(64)))` → 测试 `perf stat -e cache-misses`

### 4. 缓存类型与多级缓存架构
  - L1 缓存: 通常每核 32KB/32KB(I-cache/D-cache) → VIPT 映射(虚拟索引物理标记, Way 数限制) (arch/x86/kernel/cpu/common.c:1520)
  - L2 缓存: 每核 256KB-512KB → PIPT → L3(LLC) 共享 8-32MB → 通常 Inclusive(包含 L1/L2 内容) 或 NINE(非包含非排他) (arch/x86/kernel/cpu/cacheinfo.c → init_intel_cacheinfo)
  - 缓存行假共享: 加上 `__cacheline_aligned` 后测试 → `perf stat` 确认 cache-misses 下降

### 5. 收束
  - MESI 是多核数据一致性的基础协议，Store Buffer 是延迟写入的性能优化但引入了内存序问题
  - 伪共享是最常见的高性能退化根因 — 一个 `__cacheline_aligned` 能解决问题

---

### 核心悬念
**"Store Buffer 导致写入对其他核不立即可见 — 那 x86 的 LOCK 前缀和内存屏障到底做了什么？CAS 自旋锁怎么不安全了？"**

→ 引出 06-原子操作(LOCK 前缀/CAS) + 内存屏障(mfence/lfence/sfence)
