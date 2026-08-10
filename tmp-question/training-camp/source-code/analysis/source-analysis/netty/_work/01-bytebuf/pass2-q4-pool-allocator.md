## Loop Note: Q4 — 池化分配器三层架构

**Hypothesis**: PooledByteBufAllocator 用 PoolArena(全局) × PoolThreadCache(线程) × SizeClasses(大小) 三层架构实现内存池化，避免 jemalloc 式全局锁竞争。

**Verification** (PooledByteBufAllocator.java:38-794):
- `PoolArena<byte[]>[] heapArenas` (line 190) — heap arena 数组
- `PoolArena<ByteBuffer>[] directArenas` (line 191) — direct arena 数组
- Arena 数量公式 (line 104-117):
  - `defaultMinNumArena = availableProcessors() * 2` — 匹配 EventLoop 数量减少 Arena 锁竞争 (issue #3888)
  - `DEFAULT_NUM_HEAP_ARENA = min(cores*2, maxMemory/chunkSize/2/3)` — 内存驱动上限
  - `DEFAULT_NUM_DIRECT_ARENA = min(cores*2, maxDirectMemory/chunkSize/2/3)`
- `PoolThreadLocalCache extends FastThreadLocal<PoolThreadCache>` (line 516) — 每线程缓存线程专属 arena
- `leastUsedArena(arenas)` (line 558) — 分配线程到 arena：选活跃线程最少的 arena
- pageSize=8192, maxOrder=9 → chunkSize = 8KB << 9 = 4MB (line 71,83,289)
- smallCacheSize=256, normalCacheSize=64 (line 120-121)
- allocate 流程: `threadCache.get() → directArena.allocate(cache, cap, maxCap)` (line 398-403)
- `SizeClasses(pageSize, pageShifts, chunkSize, 0)` (line 309) — 按大小分类(tiny/small/normal)

**Code type**: Algorithmic (memory management)

**Conclusion**: PooledByteBufAllocator 设计理念：多 Arena 减少锁竞争（Arena 数量 = 2×核心数，匹配 EventLoop 数量）+ 每线程绑定一个 Arena + SizeClasses 按大小分类。小分配从线程缓存拿，大分配从 PoolChunk Buddy 算法分配。source: PooledByteBufAllocator.java:104-403
