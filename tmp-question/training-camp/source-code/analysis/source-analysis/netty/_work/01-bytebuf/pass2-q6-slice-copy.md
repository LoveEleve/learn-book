## Loop Note: Q6 — slice/duplicate/copy 语义区别

**Hypothesis**: slice 和 duplicate 都共享底层数据（视图），区别是 slice 截取子区间而 duplicate 是全量。copy 是深拷贝，完全独立。retained* 变体自动增加引用计数。

**Verification** (ByteBuf.java:2186-2275 + AbstractByteBuf 实现):
- `slice()` (line 2207) / `slice(index, length)` (line 2233) — 创建视图，readerIndex/writerIndex 独立，capacity 固定为 length
- `duplicate()` (line 2261) — 创建全量视图，所有 index 独立，共享数据
- `copy()` (line 2186) / `copy(index, length)` (line 2194) — 深拷贝，新分配独立 buffer
- `retainedSlice()` (line 2221) — slice + retain(1)，方便链式传递
- `retainedDuplicate()` (line 2275) — duplicate + retain(1)
- AbstractByteBuf 中: slice → new UnpooledSlicedByteBuf(this, index, length) — DerivedByteBuf，共享原 buf 的引用计数
- AbstractByteBuf 中: copy → alloc().buffer(length).writeBytes(this, index, length) — 真实的内存拷贝

**Code type**: Interface Design

**设计权衡**:
| 操作 | 数据共享 | 引用计数 | 使用场景 |
|------|:--:|:--:|------|
| slice | 是 | 不增(refCnt) | 临时视图，调用方持有原 buf 生命周期 |
| retainedSlice | 是 | 增1(retain) | 跨 handler 传递，调用方释放原 buf |
| duplicate | 是 | 不增 | 并发读取同一 buf 不同位置 |
| copy | 否 | 新(refCnt=1) | 需要修改的独立副本 |
| **clear()** 陷阱: ByteBuf.clear() 只重置 index 不清数据 → 和 ByteBuffer.clear() 语义不同（Javadoc 明确标注） |

**Conclusion**: `slice` = 零拷贝视图(窗口截取)、`duplicate` = 零拷贝视图(全量)、`copy` = 真拷贝(独立)。retained* 变体是做 pipeline 链式传递的关键。source: ByteBuf.java:2186-2275
