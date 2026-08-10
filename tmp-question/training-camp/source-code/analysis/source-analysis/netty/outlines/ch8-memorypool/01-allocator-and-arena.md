# Ch8 分配器架构 — PooledByteBufAllocator 与 PoolArena

> Cluster A: 6 KPs | 依赖 Ch7 Pipeline | Ch8 → §8.2

### 1. PooledByteBufAllocator DEFAULT — 12 属性对标 jemalloc

场景: `alloc.buffer(1024)` — 100 万次调用 = 100 万次 `new byte[]` 或 `UNSAFE.allocateMemory`。池化分配器预先分配大块内存, 按需切分, 复用释放的块。

源码路径: `PooledByteBufAllocator.java:68-185` — DEFAULT 静态初始化: `pageSize=8192, maxOrder=9, smallCache=256, normalCache=64, maxCachedBufferCapacity=32KB, cacheTrimInterval=8192, useCacheForAllThreads=false`——12 个参数全可通过系统属性覆盖。`PooledByteBufAllocator.java:187-188` — `DEFAULT = new PooledByteBufAllocator(!isExplicitNoPreferDirect())`——单一静态实例。`PooledByteBufAllocator.java:344-360` — `validateAndCalculatePageShifts`: pageSize≥4096+2 的幂, 返回 `log2(pageSize)`。`chunkSize = 8192 << 9 = 4 MiB`(PooledByteBufAllocator.java:362-377)。

关键设计: 12 个默认参数对标 jemalloc——`pageSize=8KB`, `maxOrder=9`→一个 Chunk 恰好 `8KB << 9 = 4MB`——jemalloc 的默认分配粒度。`smallCache=256`, `normalCache=64`——不同数不是随意取的——benchmark 和数据调优的结果。4MiB chunk 是后续所有机制(Buddy 分配/ChunkList 迁移)的硬件基础。

数据流: `alloc.buffer(4096)` → `PooledByteBufAllocator.DEFAULT` → `directByDefault? newDirectBuffer : newHeapBuffer` → `threadCache.get()` → `arena.allocate(cache, 4096)` → Normal 分配→走 ChunkList(§8.2)→返回 PooledDirectByteBuf。

### 2. Arena 数组 — 减少 EventLoop 锁竞争

场景: 16 个 EventLoop 线程——全部共享一个 PoolArena → 每次分配争 Arena 锁——16 个线程排队等待 = CPU 利用率极低。每个 Arena 独立——EventLoop 绑定到专属 Arena——分配无锁。

源码路径: `PooledByteBufAllocator.java:97-117` — Arena 数量 = `Math.min(2*cores, maxMemory / chunkSize / 2 / 3)`——最少 1, 最多 CPU×2。Lower bound(2×cores): 减少 EventLoop 热点竞争(issue #3888)。Upper bound: 每个 Arena 至少 3 个 chunks≤50% 总内存——否则 Arena 太少没 chunk 可用。`PooledByteBufAllocator.java:306-335` — `heapArenas[]` 和 `directArenas[]` 独立创建——每个 Arena 持有独立 SizeClasses。

关键设计: Arena 数量公式的精妙之处——upper bound 保证每个 Arena 有至少 3 个完整 Chunk(=12MB)的自由池——太少 chunk→allocateNormal 频繁触发 `newChunk`——失去池化的意义。`min(2*cores, ...)` 保证 Arena 数不超过 CPU 并行能力——多余的 Arena 只浪费内存不提升性能(线程数<cpu×2)。

数据流: `new PooledByteBufAllocator(nHeapArena=4, nDirectArena=4)` → 4 个 HeapArena + 4 个 DirectArena → 每个 Arena: SizeClasses + 6-ChunkList chain + smallSubpagePools → Thread1→Arena[0], Thread2→Arena[1]...→分配无锁。

### 3. PoolThreadLocalCache + leastUsedArena — 线程绑定到负荷最小 Arena

场景: 新的 EventLoop 线程——Arena-0 有 3 个线程, Arena-1 有 2 个——绑定到 Arena-1(最少线程) → 负载均衡。

源码路径: `PooledByteBufAllocator.java:516-578` — `PoolThreadLocalCache extends FastThreadLocal<PoolThreadCache>`——`initialValue()` 调用 `leastUsedArena` 选择 Arena。`leastUsedArena`: 遍历 Arena 数组, 比较 `numThreadCaches.get()` 取最小值(PooledByteBufAllocator.java:558-577)。`useCacheForAllThreads` 三级开关: true/FastThreadLocalThread/EventExecutor→带缓存; 否则 cacheSize=0(无缓存)(PooledByteBufAllocator.java:531-548)。`trimCurrentThreadCache`: `scheduleAtFixedRate` 定时释放不常用缓存(PooledByteBufAllocator.java:763-770)。

关键设计: `FastThreadLocal` 保证线程绑定 Arena 不改变——一个 EventLoop 生到死都在同一个 Arena 上分配和释放。`leastUsedArena` 按线程数负载均衡——不是按分配量(不同线程分配量差异大)——锁竞争取决于线程数。

数据流: EventLoop-1 首次 `alloc.buffer()` → `PoolThreadLocalCache.initialValue()` → `leastUsedArena(arenas)` → 扫描→Arena[2] 仅 2 线程→绑定 Arena[2] → `new PoolThreadCache(arena[2], ...)` → 后续所有 alloc 走 Arena[2]。

### 4. allocate() 三级路由 — Small/Subpage, Normal/ChunkList, Huge/malloc

场景: `alloc.buffer(128)` vs `alloc.buffer(4096)` vs `alloc.buffer(10MB)`——三种大小→三种完全不同的分配路径。

源码路径: `PoolArena.java:129-148` — `allocate()`: `sizeIdx ≤ smallMaxSizeIdx → tcacheAllocateSmall`(Subpage 位图管理, 每个 elem < pageSize) ; `sizeIdx < nSizes → tcacheAllocateNormal`(ChunkList 6 级链表, ≥pageSize 整页分配) ; else → `allocateHuge`(直接 `newUnpooledChunk(reqCapacity)`, 不加入 ChunkList, 用完直接 free)(PoolArena.java:229-235)。`tcacheAllocateNormal` 五级遍历: q050→q025→q000→qInit→q075→全部失败→`newChunk()→qInit.add(c)` (PoolArena.java:206-223)。

关键设计: 三级路由的边界——`smallMaxSizeIdx` 分 Subpage 和 Normal——Ch4 ByteBuf 的 `allocate(128)`(一个小 header)→subpage 按 128B 为单位管理一个 page(8192/128=64 个 elem)。Normal 的 5 级遍历从 q050 开始——因为 50% 使用率的 ChunkList 是"sweet spot"——既有自由空间又尚未碎片化。

数据流: `alloc.buffer(128)` → sizeIdx 对应 tiny → `tcacheAllocateSmall(cache, sizeIdx)` → cache.allocateSmall(sizeIdx)→命中→return 缓存 PooledByteBuf; cache miss→`smallSubpagePools[sizeIdx]`→head.next→`subpage.allocate()`(位图 0→1)→`initBufWithSubpage(buf, handle, reqCapacity)` → PooledByteBuf 返回。

### 5. free() 三态 + 两级锁

场景: `buf.release()` — PooledByteBuf.refCnt=0 → `deallocate()`——归还到 Arena。但线程缓存可能会复用这个 buf——如果缓存还空, 先放缓存; 缓存满了才归还到 Arena 的 ChunkList。

源码路径: `PoolArena.java:237-253` — `free(chunk, handle, sizeIdx, cache)`: 1)unpooled chunk→`destroyChunk`(Huge 分配); 2)`cache.add(this, handle, sizeIdx)`成功→不释放(线程缓存复用); 3)cache 失败→`freeChunk(chunk, handle, sizeIdx)`归还 ChunkList。两级锁: Subpage 分配用 `head.lock()` 细粒度(精确到 sizeIdx 的头节点); Normal 分配/Chunk 级操作用 `Arena` 级 `ReentrantLock`(PoolArena.java:164-177)。

关键设计: ThreadCache 的"释放优先放缓存"减少 Arena 级锁的操作次数——90% 的 free→cache.add 成功——因为这 90% 的分配是同一个 Thread 连续调用——两次 alloc 中间的 buffer 大概率符合 cache size。只有 cache 满(或 8192 次 trim)或跨线程释放时才会真正归还 Arena——锁的真正使用频率远低于调用频率。

数据流: `buf.release()`→refCnt=0→`deallocate()`→`arena.free(chunk, handle, sizeIdx, threadCache)`→`threadCache.add(handle, sizeIdx)`→命中→`threadCache[normal][sizeIdx]` 队列增加→future alloc 从这个缓存取→alloc 零 Arena 锁。缓存满→`arena.freeChunk(chunk, handle)`→`lock()`→`chunk.parent.free(handle)`→ChunkList 迁移使用率→`unlock()`。

### 核心悬念

**"PooledByteBufAllocator 的 Arena 架构解决了多线程分配竞争。但每个 Chunk 内部——4MiB 的连续内存如何高速分配和回收？§8.2 的 PoolChunk 用 handle 64 位编码(5 字段压缩单 long) + Buddy 二叉树(memoryMap depth 匹配) + collapseRuns 合并空闲兄弟——让 1 万个 128B 到 1 个 4MB 的分配都在 O(log N) 内完成。"**

→ 引出 §8.2 Chunk 与 Buddy 二叉树 — Chunk 是内存池的分配单元, handle 是 Chunk 的通用语言。`allocateRun(npages)` 怎么在二叉树上找到恰好匹配的兄弟节点？`free(handle)` 怎么把相邻空块合并回去？
