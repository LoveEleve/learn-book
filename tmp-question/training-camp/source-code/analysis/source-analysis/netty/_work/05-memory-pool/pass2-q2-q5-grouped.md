## Loop Note: Q2 — PoolSubpage bitmap 分配

**Hypothesis**: PoolSubpage 用 long[] bitmap 位图管理固定大小的元素——nextAvail 快速路径和 doNotDestroy 缓存策略加速高频分配。

**Verification** (`PoolSubpage.java`):
- `maxNumElems = runSize / elemSize` (`line 74`) — 子页内元素总数
- `bitmapLength = maxNumElems >>> 6` (`line 75-77`) — 每 long 存 64 bit，ceil 除法
- `nextAvail` (`line 42`) — 快速分配: 记录下一个可用 bit 位，`allocate()` 先试 nextAvail
- `allocate()`: test nextAvail bit → 如果已用 → `findNextAvail()` (从 0 开始扫描 bitmap)
- `free(bitmapIdx)`: set bit 0 → 更新 nextAvail → `numAvail++`
- `doNotDestroy` (`line 41`) — true: 即使 numAvail==maxNumElems (完全空闲) 也不销毁——保留在 Arena 的 subpage 链表中供下次使用
- `head.lock()` 保护: Arena 的 `smallSubpagePools[sizeIdx]` 是双向链表头，synced on head

**Code type**: Algorithmic (bitmap allocation)

**设计权衡**: nextAvail = 一次分配成功——大多数时间 nextAvail 就是下一个空闲 bit (顺序分配)。O(1) 普通路径，O(N) 碎化后。doNotDestroy = 子页的缓存优于 Chunk 的立即归还——减少 Arena 级别的分配。

## Loop Note: Q3 — PoolArena 6 级 ChunkList

**Hypothesis**: PoolArena 6 级 PoolChunkList 按使用率分类 Chunk——Chunk 的 free() 后 usage 改变触发 move() 到对应队列。

**Verification** (`PoolArena.java`):
- q000(0-25%) → q025(25-50%) → q050(50-75%) → q075(75-99%) → q100(100%)
- `allocateNormal()`: 从 q050 开始找 (优先 50%+ 使用率的 chunk → 碎片少) → q025 → q000 → q075 → q100 → new chunk
- `free()` 释放后 ChunkList.add() 检查 usage → move() 到对应队列
- `PoolChunkList.free()`: 递减 freeBytes → 如果 usage 低于当前队列下限 → prevList.add(this) → 向下晋升到更低 (更空闲) 的队列

**Code type**: Algorithmic (chunk lifecycle management)

## Loop Note: Q4 — SizeClasses 规范化表

**Hypothesis**: SizeClasses 用二维表 + 二分查找实现容量规范化——将任意 byte 数映射到预定义 sizeIdx。

**Verification** (`SizeClasses.java`):
- `sizeClasses[ ][ ]`: [sizeIdx][3] = [log2Group, log2Delta, nDelta] — 三列编码
- `sizeIdx2sizeTab[ ]`: sizeIdx → actual byte capacity (linearly indexed)
- `size2SizeIdx(int)`: 二分查找 `sizeTable[ ]` → 返回 sizeIdx
- tiny(1-512), small(512-8KB), normal(8KB-4MB), huge(>4MB)
- `pages2pageIdx(int pages)`: 页数 → pageIdx (run 的 size 指数)

## Loop Note: Q5 — PoolThreadCache

**Hypothesis**: PoolThreadCache 用 MemoryRegionCache 三层缓存 tiny/small/normal——alloc 先查缓存，free 优先归还缓存。

**Verification** (`PoolThreadCache.java`):
- `MemoryRegionCache<PoolSubpage>` (tiny, 32 条目) / `MemoryRegionCache<PoolChunk>` (small, 4 条目) / `MemoryRegionCache<PoolChunk>` (normal, 3 条目)
- `allocateTiny/allocateSmall/allocateNormal`: 每个 sizeIdx 独立缓存 → 查 entry → 命中返回; 未命中 → Arena.allocate
- `cache`: `int[ ]` 快速索引表 → (sizeIdx → index in cache table)
- `allocations` 计数器: 每 8192 次分配 → `trim()` — 清理长时间未使用的缓存条目
- `free()`: 从 PoolChunk/PoolSubpage 释放 → 尝试加入对应 cache → 如果 cache 满 → 归还 Arena
