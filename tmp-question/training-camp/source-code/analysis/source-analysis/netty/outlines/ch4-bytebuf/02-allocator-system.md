# Ch4 分配器体系 — 模板方法 + 三级扩容 + 三路策略

> Cluster C: 12 KPs | 依赖 §4.1 双指针 | §4.1 → §4.2

### 1. ByteBufAllocator SPI — 按意图分配, 不承诺实现

场景: `ByteBuf buf = allocator.buffer(1024)` — 你拿到的是 HeapByteBuf 还是 DirectByteBuf? 答案不重要——你只需要一个"能读写的缓冲区"。类型的选择应该由分配器根据场景自动决定。

源码路径: `ByteBufAllocator.java:30-43` — `buffer()` 三重载, 语义是"由实现决定 heap/direct"。`ByteBufAllocator.java:48-58` — `ioBuffer()` I/O 场景语义, 优先 direct。`ByteBufAllocator.java:63-90` — `heapBuffer()/directBuffer()` 强制类型。`ByteBufAllocator.java:96-102` — `compositeBuffer()` 零拷贝聚合。`ByteBufAllocator.java:127` — `isDirectBufferPooled()` 查询 direct 是否池化, 影响 ioBuffer 的降级策略。`AbstractByteBufAllocator.java:219-224` — 模板方法: 子类只需实现 `newHeapBuffer(int, int)` 和 `newDirectBuffer(int, int)` 两个抽象方法, 就获得了全部 `buffer/ioBuffer/heapBuffer/directBuffer/compositeBuffer` 的行为。

关键设计: SPI 接口让调用方用"意图"(我需要缓冲区/I/O缓冲区/堆/直接/组合)而非"实现"(new HeapByteBuf)来表达需求。`buffer()` 不承诺类型——分配器可以根据配置在 heap 和 direct 之间切换——调用方不知道也不需要知道具体类型。`ioBuffer()` 是这个设计的核心: 它表达了"I/O 场景"的语义, 分配器据此选择 Direct(避免 JNI 拷贝)+ 池化(如果有)。`directByDefault = preferDirect && canReliablyFreeDirectBuffers()`——平台不能安全释放时强制回退到 heap(AbstractByteBufAllocator.java:64-81)。

数据流: 用户调 `alloc.buffer(256)` → `directByDefault? directBuffer(256) : heapBuffer(256)` → `newHeapBuffer/newDirectBuffer` 模板方法 → 子类创建具体实例 → `toLeakAwareBuffer(buf)` 包裹泄漏检测。

### 2. calculateNewCapacity — 三级扩容阈值

场景: `buf.writeBytes(data, 4096)` — writable space 只有 128 字节。写入前要扩容——扩到多少? 翻倍(512→1024→2048)在小容量下合理, 但 10MB 翻到 20MB 就浪费了。

源码路径: `AbstractByteBufAllocator.java:34,232-258` — `CALCULATE_THRESHOLD = 4 MiB`。三级策略: ≤4 MiB → 2 的幂翻倍(最小 64 字节); ==4 MiB → 精确 4 MiB(不翻倍到 8 MiB); >4 MiB → 按 4 MiB 步进(10 MiB→14 MiB)。翻倍策略类似 `ArrayList.grow()` 的 1.5x——但 ByteBuf 用 2x 是为减少重新分配频率(I/O 缓冲区的扩容频率远高于 ArrayList)。

关键设计: 4 MiB 是唯一精确匹配的边界值——因为 chunkSize 恰好是 DEFAULT_PAGE_SIZE << DEFAULT_MAX_ORDER = 8192 << 9 = 4 MiB (§8.1 MemoryPool)。扩容到这里时不再翻倍(避免一个 chunk 变成两个 chunk 的开销), 而是精确分配。>4 MiB 用步进而非翻倍——`(newCapacity / 4MiB + 1) * 4MiB`——防止大缓冲区翻倍爆炸。

数据流: `ensureWritable(4096)` 发现空间不足 → `allocator.calculateNewCapacity(writerIndex+4096, maxCapacity)` → ≤4MiB: `Math.max(64, 2*current)` → ==4MiB: `4MiB` → >4MiB: `((n/4MiB)+1)*4MiB` → `buf.capacity(newCapacity)` 扩容。

### 3. toLeakAwareBuffer — 自动泄漏检测包装

场景: 分配器创建了一个 ByteBuf——在它被分配给用户之前, 需要包裹一层泄漏检测。但这层包装对用户完全透明——用户只看到 `ByteBuf` 接口。

源码路径: `AbstractByteBufAllocator.java:40-62` — 每次 `newHeapBuffer/newDirectBuffer` 返回后, 立即调 `toLeakAwareBuffer(buf)` 包裹。`leakDetector.isRecordEnabled()` 决定包装类型: 记录模式→`AdvancedLeakAwareByteBuf`(记录完整堆栈, ~80+ 方法全量追踪); 仅计数模式→`SimpleLeakAwareByteBuf`(只计时)。Composite 有独立包装: `toLeakAwareBuffer(CompositeByteBuf)`→`AdvancedLeakAwareCompositeByteBuf`(AbstractByteBufAllocator.java:52-62)。`addExclusions(AbstractByteBufAllocator, "toLeakAwareBuffer")` 防止泄漏检测器自身的调用被追踪——递归(AbstractByteBufAllocator.java:36-38)。

关键设计: 泄漏检测包装器是 Decorator 模式——它包裹原始的 ByteBuf, 拦截所有内容访问方法记录调用栈。`AdvancedLeakAwareByteBuf` 是高级版——每个非引用计数方法(如 readByte/writeByte/slice)都调 `recordLeakNonRefCountingOperation()`, 记录操作发生时的堆栈。但引用计数操作(如 retain/release)始终记录堆栈。生产环境通过 `ResourceLeakDetector.setLevel(DISABLED)` 完全关闭, 零性能开销。

数据流: `newDirectBuffer(256)` → 创建 UnpooledDirectByteBuf → `toLeakAwareBuffer(buf)` → isRecordEnabled? → `new AdvancedLeakAwareByteBuf(buf, leak)` → 返回给调用方。调用方 `buf.writeByte(1)` → AdvancedLeakAware 先 `recordLeakNonRefCountingOperation()` 再 `delegate.writeByte(1)` — 堆栈记录+业务操作。

### 4. Unpooled — 工厂的五路分发与深拷贝

场景: 你已经有一个 `byte[]` 或 `ByteBuffer`——只需要一个 ByteBuf "视图", 不想拷贝数据。`Unpooled.wrappedBuffer()` 零拷贝包裹, `Unpooled.copiedBuffer()` 深拷贝——两个方法覆盖了大部分非池化场景。

源码路径: `Unpooled.java:73` — `ALLOC = UnpooledByteBufAllocator.DEFAULT` 单例, 所有静态方法委托这个实例。`Unpooled.java:156-178` — `wrappedBuffer(byte[])`: 空数组→`EMPTY_BUFFER`, 否则 `new UnpooledHeapByteBuf(ALLOC, array, length)`——零拷贝共享原数组。`Unpooled.java:185-211` — `wrappedBuffer(ByteBuffer)`: 五路分支(empty/heap+array/readOnly+direct/readOnly+nonDirect/normal), 根据 ByteBuffer 的 isDirect/isReadOnly/hasArray 属性选择最优实现。`Unpooled.java:267-332` — `wrappedBuffer(ByteBuf...)`: 0→EMPTY, 1→slice, N→CompositeByteBuf。`Unpooled.java:362-515` — `copiedBuffer` 深拷贝: `array.clone()` 保护原始数组, ByteBuffer 先 `duplicate()` 保护原 position(修复 Netty#3896), ByteBuf 只拷贝可读区间。`UnpooledByteBufAllocator.java:30-32` — 三轴配置: metric(LongAdder)、disableLeakDetector、noCleaner。

关键设计: wrappedBuffer 的五路分支体现了 Netty 的"积极匹配"哲学——根据底层数据的特性(hasArray/memoryAddress/isDirect/isReadOnly)选择最优路径, 避免不必要的拷贝。`wrappedBuffer(ByteBuf)` 的 `0→EMPTY, 1→slice, N→CompositeByteBuf`——CompositeByteBuf 是多个 buffer 的零拷贝聚合视图, Ch4 先讲 Composite 的创建(§4.5), Ch7 Pipeline 讲它如何与 handler 协作。

数据流: `wrappedBuffer(byte[]{1,2,3})` → `new UnpooledHeapByteBuf(ALLOC, [1,2,3], 3)` → hb 引用原始数组 → `buf.setByte(0, 5)` → 原始数组[0] 也被修改。`copiedBuffer(byte[]{1,2,3})` → `array.clone()` → `wrappedBuffer(cloned)` → 修改 buf 不影响原数组。

### 5. AdaptiveByteBufAllocator — 反代际假说

场景: 一个应用开始时频繁分配 256B 的 ByteBuf(HTTP header), 后来频繁分配 16KB(Http body)。静态的 PooledByteBufAllocator 默认 4MiB chunk(§8)——256B 和 16KB 的分配路径完全不同。AdaptiveByteBufAllocator 根据近期分配模式自动调整大小类——反代际假说: 最近频繁使用的 size 应该获得更多预分配资源。

源码路径: `AdaptiveByteBufAllocator.java:24-30` — 标注 experimental, 反代际假说驱动。`AdaptiveByteBufAllocator.java:43-44` — `direct` 和 `heap` 是两个独立的 `AdaptivePoolingAllocator` 实例——各自维护自适应的池状态。`AdaptiveByteBufAllocator.java:61-68` — `newHeapBuffer/newDirectBuffer → AdaptivePoolingAllocator.allocate()` + `toLeakAwareBuffer` 包裹。`isDirectBufferPooled() = true`(AdaptiveByteBufAllocator.java:71-73)。系统属性 `io.netty.allocator.type = adaptive` 切换到此分配器(ByteBufUtil.java:79-98)。

关键设计: Adaptive 和 Pooled 并存——两个分配器解决不同的问题: Pooled 基于 jemalloc 的固定大小类(§8.1 SizeClasses), Adaptive 基于运行时统计的动态调整。反代际假说的核心: 不是"最老的 chunk 先回收", 而是"最近频繁使用的 size → 预分配更多资源"。HeapChunkAllocator/DirectChunkAllocator — HasUnsafe→选择最优实现(Unsafe 直接内存或标准 ByteBuffer)(AdaptiveByteBufAllocator.java:90-116)。

数据流: `allocType=adaptive` → `DEFAULT_ALLOCATOR` 晚绑定到 AdaptiveByteBufAllocator 实例 → `alloc.buffer(256)` → 统计 256 sizeClass 的分配频率 → `AdaptivePoolingAllocator.allocate(direct, 256)` → Magazine 分配(SizeClassedChunk→BuddyChunk 按需晋升) → `toLeakAwareBuffer` → 返回。

### 核心悬念

**"calculateNewCapacity 的 4MiB 分水岭不是随意选择的——DEFAULT_PAGE_SIZE(8192) << DEFAULT_MAX_ORDER(9) = 4MiB = 一个 PoolChunk 的大小。§4.3 的 Heap vs Direct 深入内存存储层面: UnpooledHeapByteBuf(byte[] GC回收) 和 UnpooledDirectByteBuf(Cleaner+Deallocator) 的核心差异——这些差异在 §8.1 PoolArena 的 allocateNormal 被放大: Normal 分配走 ChunkList(6级使用率分级), Small 走 Subpage(位图管理), Huge 直接 malloc。"**

→ 引出 §4.3 Heap vs Direct — 分配器选择了 "创建哪个实现", 但两种内存存储方式在分配/访问/释放三个维度的差异决定了它们的适用场景。allocateArray → new byte[N] (TLAB极速) vs allocateDirect → UNSAFE.allocateMemory (malloc 慢百倍)——为什么 I/O 场景还要用 Direct?
