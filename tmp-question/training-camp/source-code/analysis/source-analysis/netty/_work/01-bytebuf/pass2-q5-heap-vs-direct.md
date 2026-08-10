## Loop Note: Q5 — HeapByteBuf vs DirectByteBuf

**Hypothesis**: HeapByteBuf 用 byte[] 存储（hasArray=true），DirectByteBuf 用 java.nio.ByteBuffer.allocateDirect()（hasMemoryAddress=true），后者支持零拷贝 I/O 但需要手动引用计数而非依赖 GC。

**Verification** (grep against source):
- `UnpooledHeapByteBuf.java:38` — extends AbstractReferenceCountedByteBuf，内部用 `byte[]`
- `UnpooledHeapByteBuf.java:141` — `hasArray() { return true; }`
- `UnpooledHeapByteBuf.java:157` — `hasMemoryAddress() { return false; }`
- `UnpooledDirectByteBuf.java:39` — extends AbstractReferenceCountedByteBuf，内部用 `ByteBuffer`
- `UnpooledDirectByteBuf.java:216` — `hasArray() { return false; }`
- `UnpooledDirectByteBuf.java:231` — `hasMemoryAddress()` — 依赖 PlatformDependent
- Heap 的 getByte/setByte 用 `array[baseOffset + index]` (O(1))
- Direct 的 getByte/setByte 用 `Unsafe.getByte(memoryAddress + index)` (O(1), zero-copy to socket)

**Code type**: Implementation

**设计权衡**: 
- Heap: GC 托管 → 安全，分配释放快。但 NIO I/O 时 JNI 层需 `GetByteArrayRegion` 拷贝 heap 数据到临时 direct buffer——**有额外拷贝开销**
- Direct: 无 GC 开销 → `Unsafe.getByte(memoryAddress+index)` 直接操作。I/O 时 socket 层直接用内存指针→**零拷贝**。但分配/释放比 heap 慢，需显式引用计数管理
- Netty 默认策略: `PooledByteBufAllocator(preferDirect=true)` — 优先 direct（I/O 性能），用户可改系统属性 `-Dio.netty.noPreferDirect=true`
- 自适应池大小: `AdaptivePoolingAllocator`（2153行）根据历史分配量自适应调整 pool capacity——不是 heap/direct 选择，是池大小的自适应
