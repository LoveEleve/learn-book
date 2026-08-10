# Ch8 Chunk 与 Buddy 二叉树 — handle 的 64 位编码

> Cluster B: 7 KPs | 依赖 §8.1 Arena | §8.1 → §8.2

### 1. handle — 5 字段压缩为单 long

场景: Arena 给 ThreadCache 分配了一个块——ThreadCache 需要知道这个块在哪个 Chunk 的哪个位置(pages)、是 Subpage 还是连续 Page、Subpage 在位图的哪个 bit——这些元数据需要一个数据结构传回。分配一个新对象(如 `AllocResult{int pageRun, int pageCount, boolean isSubpage, long bitmapIdx}`)浪费 GC——用一个 long 数组。

源码路径: `PoolChunk.java:handle` — 64 位编码: `pageRun(15b)+pageCount(15b)+isUsed(1b)+isSubpage(1b)+bitmapIdx(32b)`。解码: `handle>>32 = bitmapIdx`, `(handle>>1)&1 = isSubpage`——单次移位。最大 pageRun = 2^14-1 = 16383 页 = 16383×8KB = 128MB——远超单个 Chunk 的 4MB——这 15 位是为嵌套 Chunk(多个 Chunk 组合)预留的。

关键设计: handle 不是对象——它是一个 long——传递通道是 Arena→ThreadCache→PooledByteBuf 内的字段。在 free 时, handle 又被传回——bit 操作解码 alloc 的元数据——不需要额外的内存分配。`isSubpage` 用一个 bit 区分, 因为 Subpage 的处理逻辑(PoolSubpage 位图)和普通 Page(Buddy 二叉树)完全不同。`bitmapIdx` 用了 32 位——因为一个 Subpage 最多有 `8192/minElemSize` = 8192/16 = 512 个 elem——32 位远超所需——留白位用于将来扩展。

数据流: `allocateRun(4 pages)` → 返回 handle = `(depthRun<<15) | (pageCount) | (isUsed=1)` → Arena 传给 ThreadCache → add 到 MemoryRegionCache → ThreadCache.allocate → PooledByteBuf.init(arena, chunk, handle) → `buf.release()` → `arena.free(chunk, handle)` → 解码 pageRun, pageCount, isUsed → `chunk.free(handle_val, ...)`。

### 2. Buddy 二叉树 — memoryMap[depth] 最小匹配

场景: Chunk 被切分为 4 页(int pool), 每页 8KB——怎么判断"有一个连续的 2 页区间可用"还是"只有散落的 2 个单页"？

源码路径: `PoolChunk.java:memoryMap[]`——数组 size = `maxOrder << 1` = 9 << 1 = 1024。`memoryMap[1]` 是根(代表整个 Chunk), `memoryMap[2]/[3]` 是左/右 half, 以此类推——完全二叉树。`allocateRun(int pages)`: 从根向下 search——每个节点 `memoryMap[node]` 存储该子树的最大可用 depth→`if(memoryMap[node] > d)` 表示子树全满, 不可分配→找兄弟→找到第一个 `memoryMap[node] == d` 的节点(恰好有对应自由 pages)。`(depth << 15) | pageCount` → handle。

关键设计: Buddy 二叉树的精妙之处——`memoryMap[node]` 存储的是 **depth 不是页数**。一个节点` memoryMap[node] == 5` 表示以该节点为根的子树中, 最大的自由块深度为 5(=叶子是 2^4=16 页)。如果因为分配 free 3 页后该节点仍有 5 页自由(不同分支), memoryMap 不变→下一次 5 页的 allocateRun 仍会匹配。

数据流: `allocateRun(4 pages)` → `log2(4)=2`→depth=2→从 `memoryMap[1]` 开始→递归向下→找到第一个 `memoryMap[node] == 2`→返回 depth→`(depth<<15) | pageCount`→`setMemoryMap(node, maxDepth+1)`(标记为 unavailable)→handle 返回给调用方。

### 3. allocateSubpage — 页内切分 bits

场景: 分配 128B——小于 pageSize(8KB)——不能在页级切割——需要用位图把一个 Page 切成 8192/128=64 个 elem。

源码路径: `PoolChunk.java:allocateSubpage(int sizeIdx)` — `allocateRun(1)`(分配 1 page)→`new PoolSubpage(chunk, page_offset, elemSize, sizeIdx)`→`subpage.allocate()` 找个空 bit→`bitmapIdx`(本次分配在页内的位图偏移)→返回 handle = `(depth<<15) | (pageCount<<1) | (1<<1) | (bitmapIdx)`——`isSubpage=1`。

关键设计: Subpage 的分配分两步——先 `allocateRun(1)` 分配整页(页级分配, Buddy 二叉树), 再 `subpage.allocate()` 分配页内 elem(位图分配, PoolSubpage 管理)。两层管理——页级(Buddy)管理"整页是否 free", 子页级(bitmap)管理"页内每个 elem 是否 free"。

数据流: `allocate(128B)`→sizeIdx 对应 Subpage→`allocateSubpage(sizeIdx)`→`allocateRun(1)`→1 page→`new PoolSubpage(elemSize=128B, maxNumElems=64)`→`getNextAvail()` 找第一个空 bit(§8.3)→bitmapIdx=5→handle = `(depth<<15) | (1<<1) | 5`→PooledByteBuf 记住 handle。

### 4. free(handle) → collapseRuns 合并空兄弟

场景: 分配了 2 页, 现在 free 了这 2 页——如果这 2 页的 buddy(相邻的同级块)也是 free——应该合并成 4 页的连续区间——递归向上。

源码路径: `PoolChunk.java:free(handle, ...)` — 位运算解码→`isSubpage? freeSubpage→回退到 page free : direct page free`。`collapseRuns(int node)`: `while(depth >= 0):`→`setMemoryMap(node, depth)`→检查 buddy(兄弟节点)是否 free(`memoryMap[buddy] == depth`)→是→`memoryMap[parent] = depth-1`(合并)→递归向上→否→break。`memoryMap[1] == maxOrder` → Chunk 完全空闲→`ChunkList.remove(chunk)`。

关键设计: collapseRuns 的"递归向上"让 Buddy 分配保持整洁——free 一个 2 页块, 如果它的兄弟也是 free→自动合并为 4 页→如果 4 页的兄弟也 free→合并为 8 页→...→根自由。这是分配时的大块优先的自然消退——使用后释放, Chunk 自然恢复到大块可用状态。

数据流: `free(handle=3<<15 | 2<<1)`→`depth=3, pageCount=2`→`node=idx(depth, runOffset)`→`setMemoryMap(node, depth)`→检查 buddy(同一个 parent 的兄弟)→`memoryMap[buddy]==depth`(free)→`setMemoryMap(parent, depth-1)`→合并→继续向上→`memoryMap[grandparent]==depth-2`(...)→...→`memoryMap[1]` 更新。

### 核心悬念

**"Buddy 二叉树解决了 Chunk 内部的页级分配——但< pageSize 的分配(128B/512B)需要更细的粒度。§8.3 的 PoolSubpage 位图分配——bitmap long[] 每 bit 代表一个 elem——64 个 elem 共用一个 long——`getNextAvail()` 用位运算一次找一批空位。PoolThreadCache 的三队列(tiny/small/normal)+MPSC 无锁——让 90% 的分配绕过 Arena 锁。"**

→ 引出 §8.3 Subpage 与 ThreadCache — PoolSubpage 的 bitmap long[] 位图分配 + PoolThreadCache 的 MemoryRegionCache MPSC 队列——三层缓存(Tiny/Small/Normal)→从 Arena 的 ChunkList 到 ThreadLocal 的缓存。
