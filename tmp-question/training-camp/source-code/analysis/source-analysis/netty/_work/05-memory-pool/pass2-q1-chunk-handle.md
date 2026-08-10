## Loop Note: Q1 — PoolChunk.handle 64bit 编码 + run 分配

**Hypothesis**: PoolChunk 用 64bit handle 编码所有分配信息，避免额外对象。run 分配使用 PriorityQueue 按 offset 排序——不是经典 Buddy 二叉树。

**Verification** (`PoolChunk.java`):
- Handle layout (`line 76-86`): `runOffset(15b) | size(15b) | isUsed(1b) | isSubpage(1b) | bitmapIdx(32b)`
- `allocateRun(runSize)` (`line 370-399`): `runsAvailLock.lock()` → `runFirstBestFit(pageIdx)` 找第一个足够大的队列 → `queue.poll()` 取 offset 最小的 run → `splitLargeRun()` 如果 run 太大 → `freeBytes -= pinnedSize`
- `splitLargeRun(handle, pages)` (`line 402-418`): if pages < runPages → split into used part + trailing free part → insertAvailRun(trailing) → return used handle
- `runsAvail`: `PriorityQueue<Integer>[]` (`line 163`) — 每个 queue 管理同一 page 大小的 runs，按 offset 排序
- `runsAvailMap`: `LongLongHashMap` (`line 164`) — runOffset → handle 的双向映射 (first/last page 各 entry)
- `isRun(handle)`: `(handle >> IS_SUBPAGE_SHIFT & 1) == 0`
- `usage()` (`line 298-323`): `100 - freeBytes*100/chunkSize`

**Code type**: Algorithmic (run-based allocation)

**设计权衡**: 不是经典 Buddy——PriorityQueue + firstBestFit。runsAvailMap 提供 O(1) runOffset → handle 查找，用于 free 时合并相邻 run。RunsAvailLock (ReentrantLock) 保护分配/释放竞态——不像 Arena 级别的全局锁，这是 Chunk 级别的细粒度锁。

**Conclusion**: PoolChunk = 64bit handle + PriorityQueue runs + splitLargeRun 分裂。不是二叉树 Buddy。source: PoolChunk.java:76-86,163-164,370-418
