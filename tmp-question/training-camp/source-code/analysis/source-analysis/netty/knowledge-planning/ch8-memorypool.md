# Ch8 Netty MemoryPool — 知识规划

> 来源: 19 源文件 | ~9000 行 | buffer/src/main/java/io/netty/buffer/
> 基线: Ch7 Pipeline 的 write 产生 ByteBuf — Ch8 回答 "如何让 ByteBuf 复用"

---

## 01 提取 — 核心机制

### 01.1 PooledByteBufAllocator (857行)
- DEFAULT 静态初始化: 12 个 SystemProperty, pageSize=8192, maxOrder=9, chunkSize=4 MiB
- Arena 数量: `min(2*cores, maxMemory/chunkSize/2/3)` — #3888 减少 EventLoop 热点
- PoolThreadLocalCache: leastUsedArena 绑定 + useCacheForAllThreads 三级开关
- newHeapBuffer/newDirectBuffer: cache.get()→arena.allocate()→toLeakAwareBuffer

### 01.2 PoolArena (853行)
- 六级 ChunkList: qInit→q000→q025→q050→q075→q100(使用率分级)
- allocate() 三级路由: Small(Subpage)→Normal(ChunkList)→Huge(独立Unpooled)
- allocateNormal 五级遍历: q050→q025→q000→qInit→q075 短路尝试
- free() 三态: unpooled→cache.add→freeChunk
- 两级锁: Subpage 用 head.lock() 细粒度 + Normal 用 Arena ReentrantLock

### 01.3 PoolChunk (739行)
- handle 64 位编码 (15+15+1+1+32): pageRun+pageCount+isUsed+isSubpage+bitmapIdx
- Buddy 二叉树: depth/memoryMap, allocateRun(npages) 最小匹配
- allocateSubpage: isSubpage 标记 + PoolSubpage 位图分配
- free(handle): 位运算解码→collapseRuns 合并空 buddy→更新 memoryMap

### 01.4 PoolChunkList (262行)
- 六级链表: prevList/nextList 双向链 + free 时使用率变化触发跨 List 移动
- move0: 分配后使用率升高→移动到更高使用率 List
- free: 释放后使用率降低→移动到更低使用率 List
- q100: minUsage=100, 不参与分配, 仅 free 后离开

### 01.5 PoolSubpage (300行)
- 位图分配: bitmap[] long 数组, 每个 bit 代表一个 elem 是否已用
- numAvail 计数 + getNextAvail() 快速定位空闲位
- 哨兵链表: head.prev=head, head.next=head — 双向循环

### 01.6 PoolThreadCache (499行)
- MemoryRegionCache: tiny/small/normal 三队列 + MPSC 无锁队列
- allocate: cache.allocate(sizeIdx) → 命中直接返回
- free: cache.add(chunk, handle) → 满时 trim → Arena.freeChunk
- trim: cacheTrimInterval(8192) 定时释放不常用条目

### 01.7 SizeClasses (413行)
- 规格公式: sizeClasses 表 + size2SizeIdx 查表
- smallMaxSizeIdx 阈值: 小于此值的分配走 Subpage
- normalizeSize: 向上对齐到最近规格

### 01.8 PooledByteBuf 包装器
- PooledByteBuf.deallocate: arena.free(内存→池)→recyclerHandle.unguardedRecycle(对象→Recycler)
- AbstractPooledDerivedByteBuf.deallocate: 先 unguardedRecycle(this)→后 parent.release() 防 Recycler 立即 re-get
- PooledNonRetained: slice/duplicate 无 refCnt 开销
- SimpleLeakAwareByteBuf.unwrappedDerived: parent(this)+trackForcibly 桥接泄漏追踪

### 01.9 AdaptivePoolingAllocator (2153行)
- 两套分配器: AdaptivePooling(新,反代际) vs PooledByteBufAllocator(旧,jemalloc)
- Magazine 架构: SizeClassedChunk(小)+BuddyChunk(大)
- SizeClassedChunk 三态: AVAILABLE→localSize→DEALLOCATED
- Magazine 四级降级: alloc0→allocFromOverflow0→allocFromNew0→createNewChunk

---

## 02 深度分类

### 🔴 Deep
| KP | 为什么🔴 |
|----|---------|
| **handle 64 位编码** | 单 long 编码 5 字段(pageRun/pageCount/isUsed/isSubpage/bitmapIdx) — Chunk 分配/释放的灵魂 |
| **Buddy 二叉树 allocateRun** | 最小匹配算法 — memoryMap[depth] 向下搜索最小满足的兄弟节点 |
| **Arena 六级 ChunkList** | jemalloc 使用率分级 — 分配从 q050 开始(50% sweet spot), q100 不参与分配 |
| **PooledByteBuf 双归还** | arena.free(内存→池) + recyclerHandle.unguardedRecycle(对象→Recycler) — 内存和对象分离回收 |

### 🟡 Working
| KP | 说明 |
|----|------|
| **PoolThreadCache 三队列+MPSC** | tiny/small/normal 无锁队列, trim 定时释放 |
| **SizeClasses 规格表** | size2SizeIdx 查表, normalizeSize 对齐 |
| **PoolSubpage 位图分配** | bitmap long[] + getNextAvail 快速定位 |
| **AdaptivePooling Allocator 反代际** | Magazine + SizeClassedChunk 状态机 |

### 🟢 Surface
| KP | 放在哪 |
|----|-------|
| **PoolArenaMetric 指标** | 和 Arena 一起 |
| **HeapArena/DirectArena 内部类** | 和 Arena 一起 |
| **PooledDuplicated/Sliced 派生** | 和 PooledByteBuf 生命周期一起 |

---

## 03 聚类

### Cluster A: 分配器架构 (6 KPs) — 零前置
1. PooledByteBufAllocator DEFAULT 单例 + 12 属性
2. Arena 数量公式
3. heapArenas/directArenas 数组
4. PoolThreadLocalCache + leastUsedArena
5. newHeapBuffer/newDirectBuffer 分配流程
6. PooledByteBufAllocatorMetric

### Cluster B: Chunk + Buddy 二叉树 (7 KPs) — 依赖 A
1. handle 64 位编码 (15+15+1+1+32)
2. Buddy 二叉树 depth/memoryMap
3. allocateRun(npages) 最小匹配
4. allocateSubpage
5. free(handle)→collapseRuns 合并空 buddy
6. PoolChunkList 六级链表+阈值迁移
7. q100 不参与分配

### Cluster C: Subpage + ThreadCache (6 KPs) — 依赖 B
1. PoolSubpage 位图分配
2. getNextAvail 快速定位
3. 哨兵链表
4. PoolThreadCache MemoryRegionCache
5. tiny/small/normal 三队列+MPSC
6. trim 定时释放

### Cluster D: PooledByteBuf 生命周期 (6 KPs) — 依赖 A+B+C
1. PooledByteBuf.deallocate 双归还
2. AbstractPooledDerived.deallocate 归还顺序
3. PooledDirect/Heap 分配+释放
4. PooledNonRetained 无 refCnt
5. SimpleLeakAware 桥接泄漏追踪
6. AdaptivePoolingAllocator 反代际

### 教学顺序: A → B → C → D
