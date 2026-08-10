# Ch8 Subpage 与 ThreadCache — 位图分配与线程缓存

> Cluster C: 6 KPs | 依赖 §8.2 Buddy | §8.2 → §8.3

### 1. PoolSubpage — 页内位图分配

场景: 512B 请求——< pageSize(8KB)——不能分配整页——把一个 page 拆成 8192/512=16 个 elem, 用位图管理。

源码路径: `PoolSubpage.java` — `bitmap[] long` 数组——每 long 管理 64 个 elem(每 bit 一个)。`numAvail` 剩余可用 elem 计数。`getNextAvail()`: 位运算查找第一个 0 bit→`bitmap[wordIdx] |= (1L << bitIdx)` + `numAvail--`——O(1)~O(N) 取决于 bitmap 密度。`elemSize = pageSize / maxNumElems`——固定大小分片。

关键设计: 位图用 `long` 而非 `byte`——64 个 elem 共用一个 long——一次 `bitmap[wordIdx] & (1L << bitIdx)` 检查一个 bit——CPU 一次按 64 位块操作。`getNextAvail` 中的 `Long.numberOfTrailingZeros(~bitmap[wordIdx])` 一次找第一个 0 bit——JVM intrinsic(BSF 指令)。

数据流: `allocate(512B)`→`sizeIdx=5`→`subpage.allocate()`→`getNextAvail()`→bitmap[0]=`0b111...1110` (14 bits free)→`bit=0`→`bitmap[0] |= 1L`→`numAvail=15→14`→返回 `bitmapIdx=0`→PooledByteBuf 的 handle 编码该 bitmapIdx。

### 2. PoolThreadCache — FastThreadLocal 三层队列

场景: EventLoop-1 刚分配了一个 512B buffer——用完释放了。下一次 alloc(512) 大概率还是这个 size——如果直接还到 Arena 的 ChunkList, 下次要重新走 Arena → allocateNormal → ChunkList 遍历→锁竞争。放线程缓存——下次 alloc(512)→命中→零锁。

源码路径: `PoolThreadCache.java` — 三层 MemoryRegionCache: `tiny(smallSizeIdx≤38)`, `small(38<idx≤smallMax)`, `normal(idx>smallMax)`。每层内部: 每个 sizeIdx 一个 Queue。`allocate(sizeIdx)`: `cache[sizeIdx]` 队列 poll→命中→返回 PooledByteBuf 引用——绕过 Arena 锁。`cache.add(chunk, handle, sizeIdx)`: 队列 offer→满时 trim→`chunk.arena.free(chunk, handle)` 真正归还 Arena。

关键设计: ThreadCache 三层队列的关键——Tiny(<512B) 和 Small(512B~8KB) 是两个高频分配范围——放到不同层——避免相互争用。`cache.add` 的容量由 smallCache/normalCache(256/64) 限制——256 个 tiny elem 复用 = 128KB 线程缓存——平衡了 mem 占用和分配速度。

数据流: `alloc(512B)`→`threadCache.allocate(small, sizeIdx)`→sizeIdx=5→`cache[small][5]` 队列有3个→poll→PooledByteBuf 引用+重置 metadata(reader/writer Index)→返回给用户(零 Arena 锁)。`free()`→`threadCache.add(small, handle, sizeIdx)`→`cache[small][5]` 队列 +1→下次 alloc 命中。

### 3. trim — 8192 次分配后释放不常用条目

场景: ThreadCache 的 `cache[small][5]` 队列有 200 个 512B buffer——但最近 1000 次 alloc 都没分配 512B——这 200 个 buffer 白占了约 100KB 的内存, 需要归还 Arena。

源码路径: `PooledByteBufAllocator.java:cacheTrimInterval=8192`——每 8192 次 alloc 后自动触发 `threadCache.trim()`。`trim()`: 遍历所有三层(small/normal)的 sizeIdx→检查每个队列→释放最近不被 allocate 的队列——`arena.free(chunk, handle)`(PoolThreadCache.java)。

关键设计: trim 是"按 age 淘汰"——记录每个 sizeIdx 的最近一次 allocate 时间 + 当前 trim 时的 alloc count——差距大于 threshold 的队列→被释放。不是简单的 LRU per entry(成本高), 而是 per sizeIdx(per queue)的粗粒度淘汰——更高效。

数据流: alloc#8191→正常→alloc#8192→`trimCurrentThreadCache()`→`threadCache.trim()`→扫描→cache[512B] 上次 allocate 是 alloc#100, 当前是 alloc#8192, 差距 8092 > threshold→`arena.free(...)`→Arena 归还约 100KB→threadCache[512B] queue 清空。

### 4. PoolChunkList 六级链表 — 使用率分级迁移

场景: 一个 Chunk 刚分配了 2 页(使用率 1%) → 在 qInit(1-25%)。随着分配增多, 使用率到 40%→迁移到 q050(50-100%)。使用率超过 100%→q100(满 Chunk)不再参与 allocateNormal 遍历。

源码路径: `PoolChunkList.java` — prevList/nextList 双向链。分配时 `move0(chunk)`: 使用率升高→`remove(chunk)`+`nextList.add(chunk)` 迁移到更高使用率 List。free 时使用率降低→迁移到更低使用率 List。`qInit(1-25%)→q000(1-50%)→q025(25-75%)→q050(50-100%)→q075(75-100%)→q100(100%-∞)`。q100 不参与分配——`allocateNormal` 遍历从 q050 开始→q025→q000→qInit→q075——跳过 q100(empty)。

关键设计: 六级 ChunkList 的阈值不是精确的——chunk 在 move 时可能跨过中间 Level(如果使用率突变——如一个 alloc 提升了 30% 使用率)。`qInit` 自引用 `prevList(qInit)`——新 Chunk 加入 qInit→后续 allocate 时 move0 移入其他 Level→ChunkList 有序, 遍历从高使用率开始(大概率有 free page)。

数据流: Chunk 使用率=1%(刚创建)→`qInit.add(chunk)`→alloc 2 pages→使用率 40%→`qInit.remove(chunk)`+`q050.add(chunk)`→alloc 10 pages→使用率 90%→`q050.remove(chunk)`+`q075.add(chunk)`→free 8 pages→使用率 50%→`q075.remove(chunk)`+`q050.add(chunk)`→free 剩余 2 pages→使用率=0→`qInit.remove(chunk)`→ChunkList 全部 List 移除→Chunk destroy。

### 核心悬念

**"PoolSubpage 的位图分配+ThreadCache 的三层队列让 < pageSize 的分配效率接近零开销。但 PooledByteBuf 本身的分配和回收——`alloc.buffer()` 创建时从 Recycler 借对象, `buf.release()` 时归还到 Recycler——和 Arena.free(内存→ChunkList)是两个独立的回收系统——§8.4 的 PooledByteBuf 生命周期解释了两条回收路径如何协作——arena.free 归还内存, recyclerHandle.unguardedRecycle 归还对象。"**

→ 引出 §8.4 PooledByteBuf 生命周期 — dual return(arena.free + unguardedRecycle) + AbstractPooledDerived 归还顺序(先 recycle 自己后 release parent) + AdaptivePoolingAllocator 反代际分配器。
