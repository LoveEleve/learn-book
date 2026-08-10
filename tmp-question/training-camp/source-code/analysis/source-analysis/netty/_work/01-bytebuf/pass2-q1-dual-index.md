## Loop Note: Q1 — 双指针 vs ByteBuffer flip/compact

**Hypothesis**: ByteBuf 的双指针 (readerIndex/writerIndex) 替代了 Java NIO ByteBuffer 的 flip()/compact() 状态机，因为 flip() 必须在读/写模式间显式切换。

**Verification**:
- grep "flip\|compact" in ByteBuf.java → **NOT found** — 证实 ByteBuf 不需要这两个操作
- Read ByteBuf.java:60-177 → 双指针明确划分3区域: discardable(0~rI) / readable(rI~wI) / writable(wI~cap)
- Read ByteBuf.java:147-149 → discardReadBytes() 将可读字节移至开头，wI 减少而不是全部回缩
- 关键区别: ByteBuffer.clear() 置 limit=capacity position=0 → **语义等价于 ByteBuf.clear()**(rI=wI=0)，但 ByteBuf.clear() **不携带"必须 flip 才能读"的隐式状态**

**Code type**: Interface Design

**Conclusion**: 双指针消除了 Java NIO ByteBuffer 的模式切换开销。在 Netty Pipeline 中数据流经多个 Handler，ByteBuffer 需要在每个 handler 之间 flip/compact。ByteBuf 的双指针可以由 Handler 独立操作——每个 handler 只动 readerIndex，writerIndex 由写入者管理，无需显式切换。source: ByteBuf.java:60-177
