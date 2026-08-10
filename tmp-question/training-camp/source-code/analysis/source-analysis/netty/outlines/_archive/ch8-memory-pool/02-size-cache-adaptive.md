# SizeClasses 与 PoolThreadCache — 规范化与加速

## 概念依赖链

```
Q4 (SizeClasses 规范化) → Q5 (ThreadCache 三层) → Q6 (AdaptivePooling)

Q4 定义"要多少才算够" → Q5 回答"怎么不让分配出线程" → Q6 补充"大池小池怎么自适应"
```

## 叙事顺序

1. **问题引入** — 从篇一过渡
   - 篇一讲了 Chunk/Subpage/Arena 怎么分配内存——但 1024 字节和 1500 字节是不是都从同一种大小的 pool 分配？
   - 任何请求字节数都需要映射到确定的规范化大小——不然 Cache 无法匹配

2. **Q4：SizeClasses — 任意容量 → 规范大小**
   - `sizeClasses[sizeIdx][3]` = [log2Group, log2Delta, nDelta] 三列编码 (`SizeClasses.java`)
   - `size2SizeIdx(int)`: 二分查找 `sizeTable[]` — 请求 1500 → sizeIdx=X → 规范化大小 Y
   - tiny(1-512B) / small(512B-8KB) / normal(8KB-4MB) / huge(>4MB)
   - `pages2pageIdx(int pages)`: 页数 → pageIdx — run 的 size 指数

3. **Q5：PoolThreadCache — 分配不出线程**
   - `MemoryRegionCache<T>` (`PoolThreadCache.java:328-375`): 底层 is `Queue<Entry<T>>` (Fixed MPSC，`PoolThreadCache.java:336`)
   - tiny: `SubPageMemoryRegionCache[32]` (`PoolThreadCache.java:300`) — size=32 per sizeIdx
   - small: `NormalMemoryRegionCache[4]` — subpage Chunk 复用
   - normal: `NormalMemoryRegionCache[3]` — run Chunk 复用
   - `allocate()` (`PoolThreadCache.java:364-375`): `entry = queue.poll()` → `initBuf()` → `entry.unguardedRecycle()` → `++allocations`
   - `add()` (`PoolThreadCache.java:350-358`): `entry = newEntry(chunk, nioBuffer, handle)` → `queue.offer(entry)` — 满了 → `entry.unguardedRecycle()` 回 Arena
   - `trim()` (`PoolThreadCache.java:166`): 每 `freeSweepAllocationThreshold` (8192) 次分配 → `allocations=0` → 扫描清除 inactive cache
   - 分配路径: ThreadCache → Arena → Chunk/Subpage。Cache 拦截绝大部分，Arena 处理少数

4. **Q6：AdaptivePoolingAllocator — 动态池大小**
   - `MIN_CHUNK_SIZE = 128KB` (`AdaptivePoolingAllocator.java:103`) — 非固定 4MB
   - `MAX_CHUNK_SIZE = 8MB` (LOW_MEM: 2MB) (`AdaptivePoolingAllocator.java:115-117`)
   - `MAX_STRIPES = cores*2` (LOW_MEM: 1) (`AdaptivePoolingAllocator.java:107`)
   - `IS_LOW_MEM = maxMemory ≤ 512MB` (`AdaptivePoolingAllocator.java:86-87`) — 小堆自动裁剪
   - `BUFS_PER_CHUNK = 8` (`AdaptivePoolingAllocator.java:108`) — 大 buffer 时每 Chunk 目标 8 个 buffer
   - 与 PooledByteBufAllocator 对比: 固定 4MB vs 动态 128KB-8MB，SizeClasses vs 历史分配量

5. **收束**: SizeClasses 规范化 → 每请求对齐确定大小 → Cache 命中远超 miss。ThreadCache = EventLoop 线程的最后几纳秒。AdaptivePooling = 低内存场景的动态裁剪。现在内存准备好了、Pipeline 组装好了、EventLoop 驱动起来了——怎么把这些部件组装成一个能工作的 Netty 服务？引出 Ch9 Bootstrap。

## 核心悬念

**"一个 EventLoop 线程的所有 ByteBuf 都不出线程——分配从 ThreadCache 到 Arena 到 Chunk/subpage，释放原路返回。整个 I/O 路径上不跨线程申请内存。"**
