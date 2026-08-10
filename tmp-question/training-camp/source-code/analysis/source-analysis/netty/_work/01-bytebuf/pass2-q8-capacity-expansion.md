## Loop Note: Q8 — 容量扩展策略

**Hypothesis**: ByteBuf 的容量扩展不是简单的翻倍——小 buffer 用指数翻倍（幂2），大 buffer 用线性步进（4MB 阶梯），64 byte 是最小分配单位。

**Verification** (AbstractByteBufAllocator.java:232-259):
- `calculateNewCapacity(minNewCapacity, maxCapacity)` (line 232) — 核心算法
- `CALCULATE_THRESHOLD = 4MB` (line 239) — 阈值分界线
- 超过 4MB: `newCapacity = minNewCapacity / threshold * threshold + threshold` — **线性增 4MB** (line 246-253)
- 低于 4MB: `findNextPositivePowerOfTwo(Math.max(minNewCapacity, 64))` — **指数翻倍** (最小 64) (line 257)
- 上限: `Math.min(newCapacity, maxCapacity)` — 永不超 maxCapacity (line 258)

**Code type**: Algorithmic

**设计权衡**:
```
capacity=256 → write需要512 → ensureWritable(256)
→ 256 < 4MB → 指数: findNextPowerOfTwo(512) = 512 → 新 capacity=512

capacity=5MB → write需要7MB → ensureWritable(2MB)  
→ 7MB > 4MB → 线性: 7/4*4+4 = 8MB → 新 capacity=8MB
```

**为什么分界点在 4MB?** 4MB = chunkSize (PoolChunk 的默认大小)。低于 4MB 的小分配是框架中最常见的（HTTP header ≈ 几百字节），指数翻倍快速找到合适大小。超过 4MB 的（文件传输 buffer），线性增长避免过度分配。

**结论**: 容量扩展 = 小 buffer 二进制翻倍（每次 ×2）、大 buffer 线性步进（每次 +4MB）、最小 64 bytes。这是 Java ArrayList 翻倍策略的网络 I/O 专用变体。source: AbstractByteBufAllocator.java:232-259
