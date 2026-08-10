# Ch4 ByteBuf 内存管理 — 从哪来到哪去

> 覆盖: Q5(Heap/Direct) / Q4(池化分配器) / Q7(泄漏检测)

---

### 1. Heap vs Direct — 字节的两种住法
  - Heap: byte[] — `hasArray()=true` (UnpooledHeapByteBuf.java:141) — TLAB 分配快
  - _getByte → HeapByteBufUtil.getByte(array, index) → `memory[index]` (HeapByteBufUtil.java:25-27)
  - Heap I/O 代价: JNI `GetByteArrayRegion` 拷贝到临时 Direct buffer — 堆可被 GC 移动
  - Direct: ByteBuffer.allocateDirect — `hasMemoryAddress()=true` (UnpooledDirectByteBuf.java:231) — 零拷贝 I/O
  - Direct 分配慢: `malloc()` 系统调用 — 但 SocketChannel.write 直接 DMA
  - 默认 preferDirect: `PooledByteBufAllocator.DEFAULT` (PooledByteBufAllocator.java:187-188) — `io.netty.noPreferDirect` 覆盖

### 2. 池化分配三层架构
  - Arena = `availableProcessors() * 2` (PooledByteBufAllocator.java:97-104) — 匹配 EventLoop 消除锁竞争
  - Arena 上限: `min(cores×2, maxMemory/chunkSize/2/3)` (PooledByteBufAllocator.java:106-117)
  - Arena 内部有 `ReentrantLock` (PoolArena.java:78) — 线程绑定减少但不消除同步 (跨线程 deallocation)
  - 每线程绑定: `leastUsedArena()` (PooledByteBufAllocator.java:558)
  - 大小分类: tiny(1-512B) / small(512B-8KB) / normal(8KB-4MB) — Buddy 分配 (chunk=8KB<<9=4MB)
  - 线程缓存: smallCacheSize=256, normalCacheSize=64 (PooledByteBufAllocator.java:120-121)
  - 分配路径: `threadCache.get()` → `directArena.allocate()` → `toLeakAwareBuffer()` (line 397-409)

### 3. 泄漏检测 — 四级
  - SIMPLE(默认, 1/128采样) / ADVANCED(1/128, 记录栈) / PARANOID(100%) / DISABLED
  - SAMPLING_INTERVAL=128 (ResourceLeakDetector.java:53)
  - PhantomReference 机制: GC 回收时检查 refCnt > 0 → 泄漏报告

### 4. 收束
  - Heap vs Direct → 分配从哪 → 池化 → 泄漏检测 → 回收

---

### 核心悬念
**"Heap 和 Direct 的物理差异怎样决定了 Netty 的默认选择？池化三层架构如何让绝大部分分配不离开线程本地缓存？"**
