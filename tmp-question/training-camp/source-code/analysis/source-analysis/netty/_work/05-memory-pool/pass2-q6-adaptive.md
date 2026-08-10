## Loop Note: Q6 — AdaptivePoolingAllocator

**Hypothesis**: AdaptivePoolingAllocator 是 Netty 的自适应池化分配器——与 PooledByteBufAllocator 的固定 4MB Chunk 不同，它根据运行时分配历史动态调整池大小。小堆默认裁剪。

**Verification** (`AdaptivePoolingAllocator.java`):
- `MIN_CHUNK_SIZE = 128KB` (`line 103`) — 最小 Chunk 为 128KB (不是 PooledByteBufAllocator 的 4MB)
- `MAX_CHUNK_SIZE = 8MB` (low-mem: 2MB) (`line 115-117`) — 最大 Chunk 8MB
- `MAX_STRIPES = cores*2` (low-mem: 1) (`line 107`) — 条带数 = EventLoop 线程数
- `BUFS_PER_CHUNK = 8` (`line 108`) — 大 buffer 时每个 Chunk 目标存放 8 个 buffer
- `IS_LOW_MEM = true if maxMemory ≤ 512MB` (`line 86-87`) — 小堆模式
- `EXPANSION_ATTEMPTS = 3` (`line 104`) — Chunk 扩容尝试次数
- `reallocate()` 根据历史用量调整 Chunk 大小——不是固定大小
- `RETIRE_CAPACITY = 256` (`line 106`) — magazine 退役阈值
- ChunkController 根据分配历史调整 `computeBufferCapacity()`

**Code type**: Algorithmic (adaptive allocation)

**与 PooledByteBufAllocator 对比**:
| 维度 | PooledByteBufAllocator | AdaptivePoolingAllocator |
|------|------|------|
| Chunk 大小 | 固定 4MB | 动态 128KB-8MB |
| 策略 | 基于 SizeClasses | 基于历史分配量 |
| 低内存 | 无特殊处理 | IS_LOW_MEM ≤512MB |
| 条带数 | cores×2 | cores×2 (LOW_MEM=1) |

**结论**: AdaptivePoolingAllocator = 自适应 Chunk 大小——小分配用小 Chunk (减少浪费)，大分配用大 Chunk (减少碎片)。LOW_MEM 模式自动裁剪。不是替代 PooledByteBufAllocator——是与它共存的另一个分配器实现。source: AdaptivePoolingAllocator.java:85-125
