# 04. 信号里不能 malloc,采样数据放哪? — 无锁存储与分配

> 🔴 Deep | 13 KP 中的 3 个(线性分配器/调用栈存储/overflow)
> 读者处境: 信号处理器三禁令之一"不能 malloc"——但每次采样都要存一个调用栈。答案: 预留的无锁内存。

### 1. "不 malloc 的分配器" — LinearAllocator

场景: 采样器预先把内存切成块,信号内从块里"拿"而非"申请"。

- `LinearAllocator`(linearAllocator.h:19): chunk 链——`allocateChunk`(linearAllocator.cpp:54)申请新块(只在块用完时)
- **并发竞争重试**(:80-81): "It's probably being allocated right now, so let's compete"——两个线程同时拿空块时,竞争分配,**原子交换保证只有一个赢家**
- 无锁: 分配 = 原子递增指针(在 chunk 内),不碰内核分配器
- [C++: 信号处理器只能调 async-signal-safe 函数——malloc 不在其列;线性分配器 = 预分配 + 原子指针推进,天然 async-signal-safe]

关键设计: **"预留 + 原子推进"替代"按需分配"**: 分配开销从"系统调用"降为"指针加法"——采样热路径的分配必须是 O(1) 且无锁。竞争重试是"两个信号线程同时拿尾块"的罕见但必须处理的竞态。

### 2. "调用栈的去重存储" — CallTraceStorage

场景: 100 万次采样,相同栈反复出现——全存爆内存。

- `CallTraceStorage`(callTraceStorage.cpp): 调用栈**去重存储**——相同栈只存一次,采样存"栈 ID"
- 原子计数 `__sync_add_and_fetch`(:67)——并发安全的记录数
- chunk 分配(`CALL_TRACE_CHUNK` :86)——与 LinearAllocator 配合
- `collectTraces/collectSamples`(:120/:143): 输出时收集(去重后的栈 + 采样计数)

关键设计: **"栈 ID"压缩存储**: 火焰图的"合并相同调用点"(AP-0 篇 3 的节点复用)在这里就有雏形——存储层去重,输出层聚合,两层都是"少存多算"。`_overflow_trace`(:84,`storage_overflow` 哨兵帧)标记存储耗尽——宁可标错不可丢统计。

### 3. "存储边界" — 内存上限与溢出

场景: 采样内存失控怎么办?

- `clear(size_t mem_limit)`(callTraceStorage.cpp:99): 按内存上限清理(旧采样丢弃)
- `capacity/usedMemory`(:110/:116): 监控
- `--memlimit`(AP-0 篇 2)落点: 采样器的自我保护

关键设计: **采样器的"内存预算"**: 采样数据是高频写入——上限+清理保证采样器自身不成为 OOM 源(与 Arthas 的 ThreadLocalWatch 防泄漏哲学一致,AR-2 篇 3;但实现层次完全不同: 这里是无锁预分配,Arthas 是 ring 栈)。

---

跨域桥: 存储消费方 = AP-5(flameGraph 读 collectSamples);信号三禁令 = AP-2 篇 1;内存预算哲学 = Arthas AR-2 篇 3(ThreadLocalWatch)。

**OpenJDK 关联**: [域 09 Memory 核心 — outlines/09-memory-core/] — 无锁分配与 JVM Arena 分配器的思想对照。
