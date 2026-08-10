# Ch4 Netty ByteBuf — 知识规划

> 来源: 38 源文件 | ~600KB 源码 | Netty buffer/src/main/java/io/netty/buffer/
> 基线: Ch3 NIO Selector → Ch4 Netty ByteBuf — 为什么重新发明缓冲区？

---

## 01 提取 — 逐源映射

### 01.1 核心接口 (3 文件 — 64 KPs)

#### ByteBuf.java (107KB — 核心接口, ~2000行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ByteBuf.java:67-73 | **三区段内存模型**: readerIndex/writerIndex 将字节序列划分为 discardable/readable/writable 三个区间 | High |
| ByteBuf.java:59-63 | **双指针设计**: readerIndex 推进读, writerIndex 推进写, read/write/skip 自动推进 | High |
| ByteBuf.java:120-145 | **discardReadBytes**: 丢弃已读字节, 前移可读数据到 offset=0, 释放可写空间 | High |
| ByteBuf.java:154-158 | **clear()** 语义不同于 NIO Buffer: 仅重置双指针为 0, 不清除数据 | High |
| ByteBuf.java:186-193 | **mark/reset 双标记**: 独立保存 readerIndex/writerIndex 快照, 无 readlimit 限制 | High |
| ByteBuf.java:195-213 | **衍生缓冲区三形态**: duplicate(共享容量)/slice(共享子区间)/copy(独立副本) | High |
| ByteBuf.java:215-221 | **retained vs non-retained**: retainedDuplicate/retainedSlice 自动 retain 减少 GC | High |
| ByteBuf.java:425-431 | **maxFastWritableBytes**: 无需重新分配可安全写入的最大字节数 | High |
| ByteBuf.java:253-269 | **capacity/maxCapacity 二级模型**: capacity 可动态调, maxCapacity 一次硬上限 | High |
| ByteBuf.java:248 | **实现 ReferenceCounted**: 继承引用计数接口, 对外暴露 refCnt/retain/release/touch | High |
| ByteBuf.java:274 | **alloc()**: 返回创建此 buffer 的 ByteBufAllocator | High |
| ByteBuf.java:2290-2365 | **nioBuffer/nioBuffers NIO 互操作**: 将可读字节暴露为 java.nio.ByteBuffer | High |
| ByteBuf.java:2372-2380 | **hasArray()/array()**: 仅堆 buffer 支持, Direct 抛 UnsupportedOperationException | High |
| ByteBuf.java:2395-2403 | **hasMemoryAddress()/memoryAddress()**: 仅 Direct buffer 有效, 返回 native 地址指针 | High |
| ByteBuf.java:2414 | **isContiguous()**: 底层是否为单连续内存区域; Composite 必须返回 false | High |
| ByteBuf.java:2478 | **compareTo**: 按字节字典序比较, 等价 memcmp | High |
| ByteBuf.java:2468-2469 | **equals**: 基于内容逐字节相等, 忽略 readerIndex/writerIndex | High |
| ByteBuf.java:2454 | **hashCode**: 基于可读字节内容计算 | High |
| ByteBuf.java:2080-2142 | **indexOf/bytesBefore**: 双向搜索 + 相对偏移量语义 | High |
| ByteBuf.java:2144-2177 | **forEachByte/forEachByteDesc**: ByteProcessor 回调遍历 | High |
| ByteBuf.java:530-555 | **ensureWritable**: 四级状态码 (0=够/1=不够/2=已扩/3=已扩至max) | High |
| ByteBuf.java:305 | **unwrap()**: 返回被包装的底层 buffer, 无包装返回 null | Medium |

#### AbstractByteBuf.java (42KB — 基类实现)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractByteBuf.java:71-80 | **状态字段直接存储**: readerIndex/writerIndex/markedIndex/maxCapacity 为 protected int | High |
| AbstractByteBuf.java:51-66 | **checkBounds/checkAccessible 系统属性开关**: 运行时可通过系统属性关闭性能检查 | High |
| AbstractByteBuf.java:68-69 | **资源泄漏检测器静态实例**: 每 ByteBuf 类级别共享 ResourceLeakDetector | High |
| AbstractByteBuf.java:110-116 | **checkIndexBounds**: 必须满足 0≤readerIndex≤writerIndex≤capacity | High |
| AbstractByteBuf.java:216-233 | **discardReadBytes compaction**: setBytes(0, this, readerIndex, ...) 前移数据 | High |
| AbstractByteBuf.java:235-255 | **discardSomeReadBytes**: 仅 readerIndex≥capacity/2 时压缩, 减少拷贝次数 | High |
| AbstractByteBuf.java:257-269 | **adjustMarkers**: 数据前移时标记跟随递减, 不能为负 | High |
| AbstractByteBuf.java:272-276 | **trimIndicesToCapacity**: 缩容时裁剪索引 | High |
| AbstractByteBuf.java:284-306 | **ensureWritable0**: 自动扩容核心, 热路径非短路 & 减少分支 | High |
| AbstractByteBuf.java:1385-1424 | **checkIndex 两层边界检查**: checkIndex→checkIndex0, 受 checkBounds 开关控制 | High |
| AbstractByteBuf.java:1478-1482 | **ensureAccessible**: 已释放守卫, refCnt==0→IllegalReferenceCountException | High |
| AbstractByteBuf.java:338-344 | **order 字节序切换**: 通过 SwappedByteBuf 装饰器实现, 当前序一致返回 this | High |
| AbstractByteBuf.java:668-698 | **setZero 高效零填充**: 8字节对齐写_long(0)+4字节对齐+逐字节尾部 | High |
| AbstractByteBuf.java:705-733 | **setCharSequence0**: UTF-8→writeUtf8, ASCII→writeAscii, 其他→byte[] | High |
| AbstractByteBuf.java:1205-1229 | **duplicate/slice 实现**: UnpooledDuplicatedByteBuf/UnpooledSlicedByteBuf 包装 | High |
| AbstractByteBuf.java:1257-1262 | **indexOf 双向委托 ByteBufUtil** | High |
| AbstractByteBuf.java:1306-1345 | **forEachByteAsc0/forEachByteDesc0** O(n) 逐字节回调 | High |
| AbstractByteBuf.java:1348-1360 | **hashCode/equals/compareTo 全委托 ByteBufUtil** | High |
| AbstractByteBuf.java:1363-1383 | **toString** 显示释放状态+key属性 | High |
| AbstractByteBuf.java:1484-1487 | **setIndex0** 无校验双指针设置, 内部使用 | High |
| AbstractByteBuf.java:308-335 | **ensureWritable(force) 四状态返回值** | High |

#### AbstractReferenceCountedByteBuf.java (3KB — 引用计数基类)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractReferenceCountedByteBuf.java:27 | **RefCnt 封装 volatile int**: 所有 CAS 操作由其静态方法封装 | High |
| AbstractReferenceCountedByteBuf.java:33-38 | **isAccessible** 使用 non-volatile 读取: best-effort guard | High |
| AbstractReferenceCountedByteBuf.java:40-43 | **refCnt() volatile 精确读取** | High |
| AbstractReferenceCountedByteBuf.java:60-68 | **retain()/retain(increment)**: CAS 递增, 返回 this 支持链式调用 | High |
| AbstractReferenceCountedByteBuf.java:82-89 | **release()/release(decrement)**: CAS 递减, 归零→deallocate() | High |
| AbstractReferenceCountedByteBuf.java:91-96 | **handleRelease**: 检查 release 结果, 归零时调用 deallocate() | High |
| AbstractReferenceCountedByteBuf.java:101-102 | **deallocate() 模板方法**: protected abstract, 子类实现实际释放 | High |
| AbstractReferenceCountedByteBuf.java:48-56 | **setRefCnt/resetRefCnt**: protected 仅子类使用 | Medium |
| AbstractReferenceCountedByteBuf.java:72-79 | **touch() 空实现**: 子类可重写用于泄漏追踪 | Medium |

---

### 01.2 分配器体系 (5 文件 — 62 KPs)

#### ByteBufAllocator.java (134行 — SPI接口)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ByteBufAllocator.java:22 | **线程安全接口**: 所有实现必须线程安全 | High |
| ByteBufAllocator.java:24 | **DEFAULT 晚绑定**: 委托 ByteBufUtil.DEFAULT_ALLOCATOR, 允许系统属性切换 | High |
| ByteBufAllocator.java:30-43 | **buffer() 三重载**: 由实现决定 heap/direct, 不承诺类型 | High |
| ByteBufAllocator.java:48-58 | **ioBuffer() 三重载**: I/O场景语义, 优先 direct | High |
| ByteBufAllocator.java:63-74 | **heapBuffer() 三重载**: 强制堆分配 | Medium |
| ByteBufAllocator.java:79-90 | **directBuffer() 三重载**: 强制直接内存 | Medium |
| ByteBufAllocator.java:96-102 | **compositeBuffer() 二重载**: 零拷贝聚合语义 | High |
| ByteBufAllocator.java:107-122 | **compositeHeapBuffer/compositeDirectBuffer**: 显式指定组合类型 | Medium |
| ByteBufAllocator.java:127 | **isDirectBufferPooled()**: 查询 direct 是否池化; 影响 ioBuffer() 降级策略 | High |
| ByteBufAllocator.java:133 | **calculateNewCapacity()**: 扩容算法在分配器级别定义 | High |

#### AbstractByteBufAllocator.java (260行 — 基类)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractByteBufAllocator.java:31-33 | **默认参数**: INITIAL_CAPACITY=256, MAX_CAPACITY=Integer.MAX_VALUE, MAX_COMPONENTS=16 | High |
| AbstractByteBufAllocator.java:34 | **CALCULATE_THRESHOLD = 4 MiB**: 扩容分水岭 | High |
| AbstractByteBufAllocator.java:36-38 | **排除 toLeakAwareBuffer 自身**: 防止 leak detector 递归 | High |
| AbstractByteBufAllocator.java:40-62 | **toLeakAwareBuffer**: 根据 isRecordEnabled() 选 Advanced/Simple 包装 | High |
| AbstractByteBufAllocator.java:64 | **directByDefault = preferDirect && canReliabilyFree**: 平台不能释放时强制 heap | High |
| AbstractByteBufAllocator.java:86-107 | **buffer() → directByDefault ? directBuffer : heapBuffer** | High |
| AbstractByteBufAllocator.java:110-131 | **ioBuffer()**: pool 场景即使平台不可靠也可用 direct | High |
| AbstractByteBufAllocator.java:145-150 | **heapBuffer(0,0) → emptyBuf** 零容量捷径 | High |
| AbstractByteBufAllocator.java:219-224 | **newHeapBuffer/newDirectBuffer 模板方法**: 子类实现此二抽象即获全部行为 | High |
| AbstractByteBufAllocator.java:232-258 | **calculateNewCapacity 三级扩容**: ≤4MiB 翻倍(≥64), ==4MiB 精确, >4MiB 步进 | High |

#### AdaptiveByteBufAllocator.java (117行 — 自适应分配器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AdaptiveByteBufAllocator.java:24-30 | **反代际假说**: 根据近期分配模式自动调优; 标注 experimental | High |
| AdaptiveByteBufAllocator.java:31-32 | **自身即 metric**: 实现 ByteBufAllocatorMetricProvider+Metric | High |
| AdaptiveByteBufAllocator.java:43-44 | **direct/heap 独立 AdaptivePoolingAllocator**: 各自维护状态 | High |
| AdaptiveByteBufAllocator.java:54-58 | **构造链**: super(preferDirect) + 创建 direct/heap AdaptivePoolingAllocator | High |
| AdaptiveByteBufAllocator.java:61-68 | **newHeapBuffer/newDirectBuffer → AdaptivePoolingAllocator.allocate()** + leak | High |
| AdaptiveByteBufAllocator.java:71-73 | **isDirectBufferPooled() return true** | High |
| AdaptiveByteBufAllocator.java:90-116 | **HeapChunkAllocator/DirectChunkAllocator**: HasUnsafe→最优实现, 无Unsafe→fallback | High |

#### UnpooledByteBufAllocator.java (323行 — 非池化分配器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| UnpooledByteBufAllocator.java:26 | **不池化核心语义**: 每次分配=new 新对象/新内存 | High |
| UnpooledByteBufAllocator.java:30-32 | **三轴配置**: metric(LongAdder), disableLeakDetector, noCleaner | High |
| UnpooledByteBufAllocator.java:37-38 | **DEFAULT 单例**: new UnpooledByteBufAllocator(directBufferPreferred()) | High |
| UnpooledByteBufAllocator.java:82-86 | **newHeapBuffer → HasUnsafe→InstrumentedUnsafeHeap, 否则→InstrumentedHeap** | High |
| UnpooledByteBufAllocator.java:88-98 | **newDirectBuffer 三路分支**: noCleaner→UnsafeNoCleaner, Unsafe→UnsafeDirect, 否则→Direct | High |
| UnpooledByteBufAllocator.java:113-115 | **isDirectBufferPooled() return false** | High |
| UnpooledByteBufAllocator.java:122-136 | **increment/decrement Heap/Direct**: 包级私有回调, Instrumented 子类调用 | High |
| UnpooledByteBufAllocator.java:138-322 | **Instrumented 子类体系**: 覆盖 allocateArray/freeArray + DecrementingCleanableDirectBuffer | High |

#### Unpooled.java (933行 — 便利工厂)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Unpooled.java:73 | **ALLOC = UnpooledByteBufAllocator.DEFAULT**: 所有静态方法委托同一实例 | High |
| Unpooled.java:88-95 | **EMPTY_BUFFER = ALLOC.buffer(0,0)**: EmptyByteBuf 单例 | High |
| Unpooled.java:101-149 | **buffer()/directBuffer() 五重载**: 纯委托 ALLOC | Medium |
| Unpooled.java:156-178 | **wrappedBuffer(byte[])**: 零拷贝包装, 共享原数组; 空数组→EMPTY | High |
| Unpooled.java:185-211 | **wrappedBuffer(ByteBuffer) 五路分支**: empty/heap/readOnly+direct/... | High |
| Unpooled.java:267-332 | **wrappedBuffer(ByteBuf... buffers)**: 0→EMPTY, 1→slice, N→CompositeByteBuf | High |
| Unpooled.java:362-367 | **copiedBuffer(byte[])**: array.clone() + wrappedBuffer, 深拷贝 | High |
| Unpooled.java:390-401 | **copiedBuffer(ByteBuffer)**: duplicate() 保护原 position, 修复 Netty#3896 | High |
| Unpooled.java:409-418 | **copiedBuffer(ByteBuf)**: 只拷贝可读区间, readerIndex/writerIndex 置零 | High |
| Unpooled.java:472-515 | **copiedBuffer(ByteBuf... buffers)**: 字节序一致性检查 | High |
| Unpooled.java:580-609 | **copiedBuffer(CharSequence, Charset)**: UTF-8→预计算+reserveAndWriteUtf8 | High |
| Unpooled.java:694-711 | **unmodifiableBuffer → asReadOnly()**: 已 deprecated | High |
| Unpooled.java:891-893 | **unreleasableBuffer**: UnreleasableByteBuf 防止误释放 | High |
| Unpooled.java:916-928 | **wrappedUnmodifiableBuffer**: 0→EMPTY, 1→asReadOnly, N→FixedComposite | High |

---

### 01.3 Heap/Direct 实现 (4 文件 — 58 KPs)

#### UnpooledHeapByteBuf.java (557行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| UnpooledHeapByteBuf.java:38 | **继承 AbstractReferenceCountedByteBuf**: 引用计数管理生命周期 | High |
| UnpooledHeapByteBuf.java:40-42 | **bytes[] + alloc + tmpNioBuf**: 堆数组存储 + 分配器引用 + 惰性 NIO 缓存 | High |
| UnpooledHeapByteBuf.java:50-82 | **构造器双形态**: new(initCap)→allocateArray + setIndex(0,0); wrap→setIndex(0,len) | High |
| UnpooledHeapByteBuf.java:84-86 | **allocateArray = new byte[capacity]**: GC 管理回收 | High |
| UnpooledHeapByteBuf.java:88-90 | **freeArray = NOOP**: 堆数组由 GC 自动回收 | High |
| UnpooledHeapByteBuf.java:104 | **ByteOrder.BIG_ENDIAN 固定** | High |
| UnpooledHeapByteBuf.java:118-138 | **capacity(newCapacity)**: System.arraycopy 复制 + allocateArray 替换 | High |
| UnpooledHeapByteBuf.java:167-256 | **getBytes/setBytes 三路分发**: memoryAddress→copyMemory, hasArray→arraycopy, 否则→回退 | High |
| UnpooledHeapByteBuf.java:303-318 | **nioBuffer vs internalNioBuffer**: 对外 slice(), 内部复用缓存定位 | High |
| UnpooledHeapByteBuf.java:333-334 | **_getByte/_setByte 委托 HeapByteBufUtil**: 集中管理字节序转换 | High |
| UnpooledHeapByteBuf.java:533-536 | **copy()**: 通过 allocator 创建新 buffer + writeBytes → 完全独立副本 | High |

#### UnpooledDirectByteBuf.java (803行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| UnpooledDirectByteBuf.java:43-47 | **四字段**: cleanable + buffer(ByteBuffer) + capacity + doNotFree | High |
| UnpooledDirectByteBuf.java:46 | **capacity 单独缓存**: ByteBuffer.remaining() 受 position/limit 影响 | High |
| UnpooledDirectByteBuf.java:47 | **doNotFree**: 包装外部 ByteBuffer 时防止释放非自有内存 | High |
| UnpooledDirectByteBuf.java:59-71 | **构造器 → allocateDirectBuffer**: PlatformDependent.allocateDirect | High |
| UnpooledDirectByteBuf.java:78-104 | **包装外部 ByteBuffer**: 必须 isDirect+!isReadOnly; doNotFree + slice 隔离 | High |
| UnpooledDirectByteBuf.java:124-130 | **allocateDirectBuffer 两重载**: CleanableDirectBuffer 封装 + Cleaner | High |
| UnpooledDirectByteBuf.java:132-153 | **setByteBuffer 替换+清理**: 先释放旧 buffer | High |
| UnpooledDirectByteBuf.java:198-203 | **capacity(newCapacity)**: 新分配→put() 复制→setByteBuffer 替换 | High |
| UnpooledDirectByteBuf.java:231-254 | **hasMemoryAddress/memoryAddress 双路**: Cleanable 路径+ByteBuffer 路径 | High |
| UnpooledDirectByteBuf.java:264-265 | **_getByte = buffer.get(index)**: JNI 底层读取 | High |
| UnpooledDirectByteBuf.java:282-314 | **_getShort/Int/Long**: VarHandle 条件加速, 否则回退 ByteBuffer+swap | High |
| UnpooledDirectByteBuf.java:303-314 | **_getUnsignedMedium**: 逐字节 &0xff+位移组装, 无 VarHandle 加速 | High |
| UnpooledDirectByteBuf.java:610-616 | **setBytes(ByteBuffer) 自引用防护**: src==tmpNioBuf 时 duplicate | High |
| UnpooledDirectByteBuf.java:693-707 | **setBytes(InputStream) ThreadLocal 中转**: 减少临时对象分配 | High |
| UnpooledDirectByteBuf.java:781-797 | **deallocate()**: buffer=null, cleanable.clean() 或 freeDirect | High |

#### UnpooledUnsafeDirectByteBuf.java (391行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| UnpooledUnsafeDirectByteBuf.java:34 | **USE_VAR_HANDLE 静态缓存**: 避免每次检查 | High |
| UnpooledUnsafeDirectByteBuf.java:36 | **memoryAddress 缓存**: 所有 Unsafe 操作的基地址 | High |
| UnpooledUnsafeDirectByteBuf.java:58-69 | **包装 Buffer 安全策略**: doFree=false + slice=true | Medium |
| UnpooledUnsafeDirectByteBuf.java:95-104 | **setByteBuffer 覆盖父类**: 同步缓存 memoryAddress | High |
| UnpooledUnsafeDirectByteBuf.java:107 | **hasMemoryAddress() 始终 true** | High |
| UnpooledUnsafeDirectByteBuf.java:124-140 | **_getByte/Short/Int/Long**: UnsafeByteBufUtil 直接内存读取 | High |
| UnpooledUnsafeDirectByteBuf.java:157-158 | **_getUnsignedMedium**: 直接 Unsafe 路径 | High |
| UnpooledUnsafeDirectByteBuf.java:212-330 | **getBytes/setBytes 批量 Unsafe**: copyMemory/setZero | High |
| UnpooledUnsafeDirectByteBuf.java:359-361 | **copy() Unsafe 复制**: 直接内存到内存 | High |
| UnpooledUnsafeDirectByteBuf.java:363-365 | **addr(index) = memoryAddress + index**: O(1) | High |
| UnpooledUnsafeDirectByteBuf.java:368-373 | **newSwappedByteBuf**: 非对齐访问→Unsafe 交换, 否则回退 | Medium |
| UnpooledUnsafeDirectByteBuf.java:377-390 | **setZero/writeZero Unsafe 批量置零**: UnsafeByteBufUtil.setZero | High |

#### UnpooledUnsafeHeapByteBuf.java (274行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| UnpooledUnsafeHeapByteBuf.java:25 | **继承 UnpooledHeapByteBuf**: 覆盖 get/set 为 Unsafe | High |
| UnpooledUnsafeHeapByteBuf.java:38-40 | **allocateUninitializedArray**: 跳过零填充开销 | High |
| UnpooledUnsafeHeapByteBuf.java:49-51 | **_getByte = UnsafeByteBufUtil.getByte(array, index)**: 绕过边界检查 | High |
| UnpooledUnsafeHeapByteBuf.java:60-127 | **_getShort/Int/Long 全部 Unsafe**: 单次操作 | High |
| UnpooledUnsafeHeapByteBuf.java:149-210 | **_setByte/Short/Int/Long 全部 Unsafe** | High |
| UnpooledUnsafeHeapByteBuf.java:250-263 | **setZero/writeZero Unsafe 批量**: Unsafe.setMemory 单次调用 | High |
| UnpooledUnsafeHeapByteBuf.java:267-273 | **newSwappedByteBuf 条件 Unsafe**: isUnaligned ? UnsafeHeapSwapped : Swapped | Medium |

---

### 01.4 视图与复合 (7 文件 — ~50 KPs)

#### CompositeByteBuf.java (81KB — ~2000行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| CompositeByteBuf.java:44-48 | **虚拟缓冲区**: 将多个 ByteBuf 作为单一合并视图, 零拷贝拼接 | High |
| CompositeByteBuf.java:49,1913-1985 | **Component 双指针**: srcBuf(原始) + buf(解包后), 双重引用计数 | High |
| CompositeByteBuf.java:58-75 | **components[] 动态数组 + 累加偏移**: capacity 通过 endOffset 计算 | High |
| CompositeByteBuf.java:566-573 | **maxNumComponents + consolidateIfNeeded**: 超限自动合并 | High |
| CompositeByteBuf.java:320-348 | **newComponent 逐层解包**: WrappedByteBuf→SwappedByteBuf→SlicedByteBuf→真实底层 | High |
| CompositeByteBuf.java:332-340 | **识别 PooledSlicedByteBuf/DuplicatedByteBuf**: 提取 adjustment 偏移 | High |
| CompositeByteBuf.java:1936-1954 | **Component 双向索引映射+偏移重算** | High |
| CompositeByteBuf.java:270-275 | **checkForOverflow**: Integer.MAX_VALUE 防溢出 | High |
| CompositeByteBuf.java:280-310 | **addComponent0 带 finally 回滚**: 失败时 release | High |
| CompositeByteBuf.java:371-420 | **addComponents0 批量添加**: 预计算+移位+finally 回滚 | High |
| CompositeByteBuf.java:470-529 | **addFlattenedComponents 浅拷贝**: 零拷贝复用源组件 | High |
| CompositeByteBuf.java:1774-1793 | **consolidate0**: allocBuffer→transferTo→替换组件 | High |
| CompositeByteBuf.java:1957-1960 | **Component.transferTo**: writeBytes+free 一体 | High |
| CompositeByteBuf.java:920-945 | **toComponentIndex0 二分查找**: O(log n) | High |
| CompositeByteBuf.java:1615-1654 | **findComponent 带 lastAccessed 弱缓存**: 分摊 O(1) | High |
| CompositeByteBuf.java:1013-1022 | **_getInt 跨组件多字节读取**: 同组件内→委托, 否则逐字节拼接 | High |
| CompositeByteBuf.java:722-759 | **decompose**: 拆分各组件 slice, 保证引用计数正确 | High |
| CompositeByteBuf.java:1798-1843 | **discardReadComponents**: 释放已读完组件 | High |
| CompositeByteBuf.java:1846-1900 | **discardReadBytes**: 释放已读+首组件重新切片 | High |
| CompositeByteBuf.java:844-886 | **capacity(newCapacity)**: 扩容→padding组件, 缩容→removeCompRange | High |
| CompositeByteBuf.java:1962-1968 | **Component.slice() 惰性缓存**: 首次创建后复用 | High |

#### SlicedByteBuf.java + DuplicatedByteBuf.java + ReadOnlyByteBuf.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| SlicedByteBuf.java:18-20 | **切片语义**: 暴露父缓冲区子区域, 共享底层数据, 独立读写索引 | High |
| SlicedByteBuf.java:46-48 | **capacity() 固定=length**: 切片不可扩缩容 | High |
| DuplicatedByteBuf.java:37-51 | **全量代理**: 转发所有数据访问; 解包嵌套 DuplicatedByteBuf + PooledDerived | High |
| DuplicatedByteBuf.java:88-91 | **capacity 委托 parent**: 视图容量与 parent 同步 | High |
| ReadOnlyByteBuf.java:53-55 | **isReadOnly() 始终 true** | High |
| ReadOnlyByteBuf.java:59-66 | **所有写入抛 ReadOnlyBufferException**: 全量写拦截 | High |
| ReadOnlyByteBuf.java:287-299 | **duplicate/slice 保持只读**: 派生视图也注入 ReadOnlyByteBuf | High |
| ReadOnlyByteBuf.java:398-411 | **nioBuffer → asReadOnlyBuffer()**: NIO 层面只读保护 | High |
| ReadOnlyByteBuf.java:100-102 | **hasArray() 返回 false**: 阻止数组绕路 | High |

#### EmptyByteBuf.java + SwappedByteBuf.java + UnreleasableByteBuf.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| EmptyByteBuf.java:40,74-76 | **Null Object 模式**: capacity=0, 所有读写抛 IndexOutOfBoundsException | High |
| EmptyByteBuf.java:865-896 | **slice/duplicate/copy 返回 this**: 避免创建新对象 | High |
| EmptyByteBuf.java:1012-1044 | **refCnt=1 固定, retain/release no-op** | High |
| SwappedByteBuf.java:43-49 | **自动计算补序**: BIG→LITTLE, LITTLE→BIG | High |
| SwappedByteBuf.java:58-63 | **order() 智能解包**: 一致时剥离 Swapped 层 | High |
| SwappedByteBuf.java:243-309 | **读后交换/写前交换**: getShort→swapShort, setShort→swap再写 | High |
| UnreleasableByteBuf.java:22-25 | **阻断引用计数**: retain/release/touch 全部 no-op | High |
| UnreleasableByteBuf.java:53-102 | **派生视图传播保护**: slice/duplicate 返回 Unreleasable 包装 | High |

#### AbstractDerivedByteBuf.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractDerivedByteBuf.java:35-113 | **全量委托 unwrap()**: isAccessible/refCnt/retain/release/touch | High |
| AbstractDerivedByteBuf.java:28-32 | **extends AbstractByteBuf abstract**: 派生缓冲区基类 | High |
| AbstractDerivedByteBuf.java:48-49 | **refCnt0 = unwrap().refCnt()**: 无独立引用计数 | High |
| AbstractDerivedByteBuf.java:94-98 | **release0 = unwrap().release()**: 不能独立释放 | High |

---

### 01.5 工具类与适配器 (7 文件 — 48 KPs)

#### ByteBufUtil.java (82KB)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ByteBufUtil.java:79-98 | **DEFAULT_ALLOCATOR 系统属性驱动**: pooled/unpooled/adaptive | High |
| ByteBufUtil.java:112-120 | **threadLocalTempArray**: FastThreadLocalThread 下启用, 虚拟线程友好 | High |
| ByteBufUtil.java:133-138 | **ensureAccessible**: 引用计数保护门 | High |
| ByteBufUtil.java:200-202 | **ensureWritableSuccess**: 统一扩容意图判定 | High |
| ByteBufUtil.java:208-236 | **hashCode 4字节uint批量**: endian 自适应, 0→1(对齐JDK String) | High |
| ByteBufUtil.java:243-326 | **indexOf Two-Way 算法**: 单字节→ByteBuf.indexOf 更快 | High |
| ByteBufUtil.java:367-409 | **equals 8字节long两阶段**: 跨endian时swap | High |
| ByteBufUtil.java:431-471 | **compare 4字节uint批量**: 4种endian组合独立路径 | High |
| ByteBufUtil.java:573-611 | **firstIndexOf SWAR**: SIMD Within A Register, unaligned 不支持时退化 | High |
| ByteBufUtil.java:724-753 | **lastIndexOf SWAR**: 反向端序转换 | High |
| ByteBufUtil.java:633-660 | **swapShort/Medium/Int/Long**: 24位medium需符号扩展处理 | High |
| ByteBufUtil.java:829-934 | **writeUtf8 5条快速路径**: AsciiString→Unsafe→Array→Direct→通用 | High |
| ByteBufUtil.java:1145-1191 | **utf8MaxBytes/utf8Bytes**: 最坏+精确两套容量计算 | High |
| ByteBufUtil.java:1351-1371 | **decodeString**: US-ASCII 快速路径 new String(array) | High |
| ByteBufUtil.java:1378-1388 | **threadLocalDirectBuffer**: 系统属性控制线程局部直连缓冲区 | High |
| ByteBufUtil.java:1703-1768 | **ThreadLocal(Unsafe)DirectByteBuf Recycler**: 超限→释放, 否则→recycle | High |
| ByteBufUtil.java:1514-1700 | **HexUtil**: 惰性查找表 + 64KiB 行偏移预计算 | High |
| ByteBufUtil.java:1889-1965 | **isUtf8**: 严格 RFC 3629, overlong 拒绝 | High |

#### HeapByteBufUtil.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| HeapByteBufUtil.java:29-92 | **VarHandle 双路径**: hasVarHandle→VarHandleByteBufferAccess, 否则手动位移 | High |
| HeapByteBufUtil.java:36-44 | **getShort0 BE/LE**: 默认BE; LE 保持对称结构 | High |
| HeapByteBufUtil.java:47-56 | **getUnsignedMedium**: 无 VarHandle 加速(Java 无3字节原生类型) | High |

#### ByteBufInputStream.java + ByteBufOutputStream.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ByteBufInputStream.java:47-57 | **构造时快照 readerIndex**: writerIndex 后续变化不影响 available | High |
| ByteBufInputStream.java:51-57 | **releaseOnClose + close()**: 可选所有权转移, 关闭时释放 buffer | High |
| ByteBufInputStream.java:109-121 | **构造器预释放**: 参数校验失败时若 releaseOnClose=true 立即 release | High |
| ByteBufOutputStream.java:44 | **构造时快照 writerIndex**: writtenBytes() 统计写入量 | High |
| ByteBufOutputStream.java:171-187 | **close() 三步**: super.close→关闭 utf8out→条件释放; idempotent | High |

#### ByteBufHolder.java + DefaultByteBufHolder.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ByteBufHolder.java:23 | **extends ReferenceCounted**: Holder 本身引用计数 | High |
| ByteBufHolder.java:33-45 | **copy/duplicate/retainedDuplicate 三层语义** | High |
| ByteBufHolder.java:50 | **replace(ByteBuf) 不可变替换**: 返回新 Holder | High |
| DefaultByteBufHolder.java:34-36 | **content() + ensureAccessible**: 引用计数门控 | High |
| DefaultByteBufHolder.java:43-66 | **模板方法: 所有复制→replace()**: 子类只需重写 replace | High |
| DefaultByteBufHolder.java:80-117 | **引用计数全委托 data 字段** | High |
| DefaultByteBufHolder.java:119-125 | **contentToString() 绕过 ensureAccessible**: 调试输出不抛异常 | Medium |
| DefaultByteBufHolder.java:133-158 | **equals 限制同Class**: getClass()==o.getClass() 而非 instanceof | High |

---

### 01.6 泄漏检测与派生缓冲区 (12 文件 — ~65 KPs)

#### AdvancedLeakAwareByteBuf.java + SimpleLeakAwareByteBuf.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AdvancedLeakAwareByteBuf.java:36 | **继承 SimpleLeakAwareByteBuf**: 双层架构 | High |
| AdvancedLeakAwareByteBuf.java:39-45 | **PROP_ACQUIRE_AND_RELEASE_ONLY**: 控制堆栈记录范围 | High |
| AdvancedLeakAwareByteBuf.java:51-53 | **排除自身**: addExclusions 防止递归记录 | High |
| AdvancedLeakAwareByteBuf.java:69-931 | **~80+方法 recordLeakNonRefCountingOperation**: 全量操作追踪 | High |
| AdvancedLeakAwareByteBuf.java:933-960 | **retain/release 直接 leak.record()** | High |
| SimpleLeakAwareByteBuf.java:27 | **继承 WrappedByteBuf**: Decorator/Proxy 模式 | High |
| SimpleLeakAwareByteBuf.java:34-35 | **trackedByteBuf + leak(ResourceLeakTracker)**: 泄漏追踪核心 | High |
| SimpleLeakAwareByteBuf.java:48-110 | **slice/duplicate/readSlice/asReadOnly 共享 leak**: 派生缓冲区同追踪 | High |
| SimpleLeakAwareByteBuf.java:52-90 | **retainedSlice: unwrappedDerived 预处理**: 解包 Swapped 层 | High |
| SimpleLeakAwareByteBuf.java:186-199 | **unwrappedDerived 核心**: 池化→重设parent, 非池化→共享包装 | High |
| SimpleLeakAwareByteBuf.java:143-174 | **release→closeLeak**: 引用计数归零时关闭追踪器 | High |
| SimpleLeakAwareByteBuf.java:126-129 | **异常附加关闭堆栈**: ThrowableUtil.addSuppressed 诊断 use-after-free | High |

#### AdvancedLeakAwareCompositeByteBuf.java + SimpleLeakAwareCompositeByteBuf.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AdvancedLeakAwareCompositeByteBuf.java:36 | **继承 SimpleLeakAwareCompositeByteBuf**: Composite 双层架构 | High |
| AdvancedLeakAwareCompositeByteBuf.java:42-1018 | **所有非引用计数+Composite特有方法 全量记录** | High |
| SimpleLeakAwareCompositeByteBuf.java:24 | **继承 WrappedCompositeByteBuf**: Composite 专用 Decorator | High |
| SimpleLeakAwareCompositeByteBuf.java:34-62 | **release 先 unwrap() 再 super.release()**: 防止内部改变 | High |

#### AbstractUnpooledSlicedByteBuf.java + UnpooledSlicedByteBuf.java + UnpooledDuplicatedByteBuf.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractUnpooledSlicedByteBuf.java:32-34 | **buffer + adjustment**: 原始引用+累积偏移 | High |
| AbstractUnpooledSlicedByteBuf.java:36-49 | **嵌套解包**: 多层切片累加 adjustment; Duplicated→unwrap | High |
| AbstractUnpooledSlicedByteBuf.java:88-90 | **capacity(newCapacity) 抛异常**: 切片容量不可变 | High |
| AbstractUnpooledSlicedByteBuf.java:218-235 | **duplicate = slice(0, capacity())**: 全量切片实现 | High |
| AbstractUnpooledSlicedByteBuf.java:473-475 | **idx(index) = index + adjustment**: 索引转换公式 | High |
| UnpooledSlicedByteBuf.java:38-125 | **_getXxx/_setXxx 走 unwrap()._getXxx(idx)**: 旁路 checkIndex | High |
| UnpooledDuplicatedByteBuf.java:33-120 | **_getXxx/_setXxx 直接传 index**: Duplicate 不偏移 | High |

#### FixedCompositeByteBuf.java (689行)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| FixedCompositeByteBuf.java:33-36 | **只读复合缓冲区**: 固定 ByteBuf[] 数组虚拟视图, extends ARCB | High |
| FixedCompositeByteBuf.java:45-77 | **构造器**: 总和 readableBytes=capacity, ByteOrder 必须一致 | High |
| FixedCompositeByteBuf.java:63 | **混合字节序抛异常** | High |
| FixedCompositeByteBuf.java:80-208 | **所有 set/write 抛 ReadOnlyBufferException** | High |
| FixedCompositeByteBuf.java:229-250 | **findComponent 线性扫描 + 惰性 Component 缓存**: 替换 buffers[] 引用 | High |
| FixedCompositeByteBuf.java:271-365 | **多字节跨组件读取**: 同组件→直接读, 跨组件→逐字节/分段 | High |
| FixedCompositeByteBuf.java:534-600 | **nioBuffer/nioBuffers**: 单组件→直接委托, 多组件→合并或零拷贝提取 | High |
| FixedCompositeByteBuf.java:663-667 | **deallocate 级联释放**: 遍历所有子 buffer 调用 release | High |

#### ReadOnlyByteBufferBuf.java + 派生

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ReadOnlyByteBufferBuf.java:34-36 | **包装 JDK ByteBuffer**: extends ARCB, 持有 ByteBuffer | High |
| ReadOnlyByteBufferBuf.java:42-47 | **只读验证+统一BIG_ENDIAN**: 构造器 slice+order | High |
| ReadOnlyByteBufferBuf.java:52 | **deallocate 空**: ByteBuffer 由 GC 管理 | Medium |
| ReadOnlyByteBufferBuf.java:103-118 | **_getShortLE 通过 swapShort**: BE读后交换 | High |
| ReadOnlyByteBufferBuf.java:566-615 | **duplicate/slice 保持只读**: ReadOnlyDuplicatedByteBuf/ReadOnlySlicedByteBuf | High |
| ReadOnlyAbstractByteBuf.java:22-77 | **对 AbstractByteBuf 的特化只读**: _getXxx 旁路 | High |
| ReadOnlyUnsafeDirectByteBuf.java:28 | **叠加 Unsafe 加速**: 在 ReadOnlyByteBufferBuf 上加速 | High |
| ReadOnlyUnsafeDirectByteBuf.java:39-61 | **_getXxx 通过 UnsafeByteBufUtil**: 绕 ByteBuffer 开销 | High |

#### AbstractUnsafeSwappedByteBuf.java

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractUnsafeSwappedByteBuf.java:33 | **断言 isUnaligned**: 要求硬件支持非对齐访问 | High |
| AbstractUnsafeSwappedByteBuf.java:35 | **nativeByteOrder 布尔等价**: BIG_ENDIAN_NATIVE==(order==BIG_ENDIAN) | High |
| AbstractUnsafeSwappedByteBuf.java:38-43 | **条件 reverseBytes**: nativeByteOrder→直传, 否则 Long.reverseBytes | High |
| AbstractUnsafeSwappedByteBuf.java:124-145 | **writeXxx 直接操作 AbstractByteBuf 字段**: 绕公开 API | High |
| AbstractUnsafeSwappedByteBuf.java:165-170 | **_getShort/_getInt/_getLong 抽象方法**: 子类按 BE/LE 实现 | High |

---

## 01 聚合 — 跨文件汇总

N=38, P-level阈值: P1=≥8文件, P2=3-7文件, P3=1-2文件

### P1 — 全系统共识 (≥8 文件, ~28 KPs)

| Knowledge Point | 出现文件 |
|----------------|---------|
| **双指针模型(readerIndex/writerIndex)** | ByteBuf + AbstractByteBuf + 全子类 |
| **三区段(discardable/readable/writable)** | ByteBuf + AbstractByteBuf + Composite + views |
| **discardReadBytes/discardSomeReadBytes** | ByteBuf + AbstractByteBuf + Composite + FixedComposite |
| **capacity/maxCapacity 二级容量** | ByteBuf + AbstractByteBuf + 全子类 |
| **ensureWritable 四级状态码** | ByteBuf + AbstractByteBuf + Heap + Direct + Unsafe variants |
| **markReaderIndex/markWriterIndex + reset** | ByteBuf + AbstractByteBuf + Duplicated + Sliced |
| **slice()/duplicate() 共享底层 + 独立索引** | ByteBuf + DuplicatedByteBuf + SlicedByteBuf + Pooled variants + AbstractDerived |
| **copy() 独立副本** | ByteBuf + AbstractByteBuf + Heap + Direct + Composite + Unpooled |
| **ensureAccessible/checkAccessible 释放守卫** | AbstractByteBuf + ByteBufUtil + DefaultByteBufHolder + LeakAware |
| **retain()/release() CAS 引用计数** | AbstractReferenceCountedByteBuf + AbstractDerived + Composite + FixedComposite + Empty + LeakAware + 全部子类 |
| **deallocate() 模板方法** | AbstractReferenceCountedByteBuf + Heap + Direct + Composite + FixedComposite |
| **refCnt volatile + isAccessible** | AbstractReferenceCountedByteBuf + 全子类 |
| **ByteBufAllocator.buffer/heap/direct/composite** | ByteBufAllocator + AbstractByteBufAllocator + Adaptive + Unpooled + Unpooled.java |
| **toLeakAwareBuffer 泄漏检测包装** | AbstractByteBufAllocator + AdaptiveByteBufAllocator + UnpooledByteBufAllocator + Composite |
| **calculateNewCapacity 三级扩容策略** | AbstractByteBufAllocator + 全子类 Allocators |
| **Unpooled.wrappedBuffer 多路分发** | Unpooled + 全子类 Heap + Direct |
| **Unpooled.copiedBuffer 深拷贝工厂** | Unpooled + 全子类 Heap + Direct + Composite |
| **nioBuffer/nioBuffers NIO 互操作** | ByteBuf + AbstractByteBuf + Heap + Direct + Composite + ReadOnly + FixedComposite |
| **hasArray/array() 数组暴露** | ByteBuf + Heap(true) + Direct(false) + ReadOnly(false) + Empty(true) |
| **hasMemoryAddress/memoryAddress() 地址暴露** | ByteBuf + Direct(true) + Unsafe(true) + Empty(depends) |
| **order()/order(byteOrder) 字节序** | ByteBuf + AbstractByteBuf + Heap(BIG_ENDIAN) + SwappedByteBuf + Composite + ReadOnly |
| **_getByte/_setByte 基础访问** | Heap + Direct + Unsafe variants + UnpooledSliced + UnpooledDuplicated + ReadOnly + HeapByteBufUtil |
| **ByteBufHolder content持有+引用计数传播** | ByteBufHolder + DefaultByteBufHolder |
| **Composite 零拷贝组件拼接** | CompositeByteBuf + WrappedCompositeByteBuf + LeakAwareComposite |
| **LeakAware Simple/Advanced 双层架构** | AdvancedLeakAwareByteBuf + SimpleLeakAwareByteBuf + Composite variants |
| **ResourceLeakDetector track/close 生命周期** | AbstractByteBufAllocator + SimpleLeakAwareByteBuf + AdvancedLeakAwareByteBuf |
| **indexOf/forEachByte 搜索遍历** | ByteBuf + AbstractByteBuf + ByteBufUtil + Composite |

### P2 — 局部重要 (3-7 文件, ~35 KPs)

| Knowledge Point | 出现文件 |
|----------------|---------|
| **directByDefault 决定 buffer() 路由** | AbstractByteBufAllocator + Adaptive + Unpooled + ByteBufUtil |
| **newHeapBuffer/newDirectBuffer 模板方法** | AbstractByteBufAllocator + Adaptive + UnpooledByteBufAllocator |
| **ioBuffer() I/O语义优先 direct** | ByteBufAllocator + AbstractByteBufAllocator |
| **allocateArray = new byte[N] (Heap)** | UnpooledHeapByteBuf + UnpooledUnsafeHeapByteBuf |
| **allocateUninitializedArray 跳过零填充** | UnpooledUnsafeHeapByteBuf |
| **CleanableDirectBuffer + Cleaner** | UnpooledDirectByteBuf + UnpooledUnsafeDirectByteBuf + UnpooledByteBufAllocator |
| **doNotFree 防释放非自有内存** | UnpooledDirectByteBuf + UnpooledUnsafeDirectByteBuf |
| **Unsafe access (getByte/setByte/putByte)** | UnpooledUnsafeHeapByteBuf + UnpooledUnsafeDirectByteBuf + ReadOnlyUnsafeDirect + UnsafeByteBufUtil |
| **VarHandle 条件加速多字节访问** | UnpooledDirectByteBuf + UnpooledUnsafeDirectByteBuf + HeapByteBufUtil |
| **Composite Component.findComponent 组件定位** | CompositeByteBuf + FixedCompositeByteBuf |
| **Composite consolidate 组件合并** | CompositeByteBuf |
| **Composite addComponent/addFlattened 组件管理** | CompositeByteBuf |
| **Composite discardReadComponents/discardReadBytes** | CompositeByteBuf |
| **Component slice 惰性缓存** | CompositeByteBuf + FixedCompositeByteBuf |
| **Slice adjustment 索引偏移** | AbstractUnpooledSlicedByteBuf + UnpooledSlicedByteBuf + CompositeByteBuf 解包 |
| **ReadOnly write 拦截门面** | ReadOnlyByteBuf + FixedCompositeByteBuf + ReadOnlyByteBufferBuf |
| **Empty Null Object 模式** | EmptyByteBuf + Unpooled.EMPTY_BUFFER + AbstractByteBufAllocator.emptyBuf |
| **Swapped 字节序装饰器** | SwappedByteBuf + AbstractByteBuf + AbstractUnsafeSwappedByteBuf |
| **Unreleasable 阻断释放** | UnreleasableByteBuf + Unpooled.unreleasableBuffer |
| **派生派生缓冲区 unwrap() 委托链** | AbstractDerivedByteBuf + DuplicatedByteBuf + UnpooledSlicedByteBuf + LeakAware |
| **releaseOnClose 所有权转移** | ByteBufInputStream + ByteBufOutputStream |
| **swapShort/swapInt/swapLong 工具** | ByteBufUtil + SwappedByteBuf + UnpooledDirectByteBuf |
| **SWAR firstIndexOf/lastIndexOf** | ByteBufUtil |
| **writeUtf8 5路径优化** | ByteBufUtil |
| **isUtf8 严格验证** | ByteBufUtil |
| **utf8Bytes 精确容量计算** | ByteBufUtil + Unpooled.copiedBufferUtf8 |
| **copy/duplicate/retainedDuplicate Holder 三层** | ByteBufHolder + DefaultByteBufHolder |
| **AdaptivePoolingAllocator 独立 direct/heap** | AdaptiveByteBufAllocator |
| **Instrumented 子类度量体系** | UnpooledByteBufAllocator |
| **unwrappedDerived 预处理解包** | SimpleLeakAwareByteBuf |
| **FixedComposite 固定只读复合** | FixedCompositeByteBuf |
| **ReadOnlyByteBufferBuf 包装 JDK ByteBuffer** | ReadOnlyByteBufferBuf + ReadOnlyUnsafeDirectByteBuf |
| **AbstractUnsafeSwappedByteBuf 硬件加速** | AbstractUnsafeSwappedByteBuf + UnsafeDirectSwapped + UnsafeHeapSwapped |
| **ThreadLocal Direct ByteBuf Recycler** | ByteBufUtil |

### P3 — 独立 (1-2 文件, ~38 KPs)

| Knowledge Point | 出现文件 |
|----------------|---------|
| **maxNumComponents + consolidateIfNeeded** | CompositeByteBuf |
| **newComponent 逐层解包链** | CompositeByteBuf |
| **decompose 拆分组件** | CompositeByteBuf |
| **checkForOverflow 整数溢出** | CompositeByteBuf |
| **componentCount 计数器** | CompositeByteBuf |
| **trackedByteBuf vs buf 双引用** | SimpleLeakAwareByteBuf |
| **PROP_ACQUIRE_AND_RELEASE_ONLY 开关** | AdvancedLeakAwareByteBuf |
| **recordLeakNonRefCountingOperation** | AdvancedLeakAwareByteBuf |
| **Composite.acquireAndReleaseOnly** | AdvancedLeakAwareCompositeByteBuf |
| **release 先 unwrap()** | SimpleLeakAwareCompositeByteBuf |
| **multi-layer slicing adjustment 累加** | AbstractUnpooledSlicedByteBuf |
| **duplicate=full slice + preserve idx** | AbstractUnpooledSlicedByteBuf |
| **findComponent 惰性 Component 缓存** | FixedCompositeByteBuf |
| **nioBuffer 多组件合并** | FixedCompositeByteBuf + CompositeByteBuf |
| **cross-component multi-byte read** | FixedCompositeByteBuf + CompositeByteBuf |
| **ReadOnly nioBuffer .asReadOnlyBuffer()** | ReadOnlyByteBuf + ReadOnlyByteBufferBuf |
| **ReadOnlyDuplicatedByteBuf/ReadOnlySlicedByteBuf** | ReadOnlyByteBufferBuf |
| **ReadOnlyUnsfe Unsafe 加速** | ReadOnlyUnsafeDirectByteBuf |
| **nativeByteOrder 条件 reverseBytes** | AbstractUnsafeSwappedByteBuf |
| **addFlattenedComponents 浅拷贝** | CompositeByteBuf |
| **releaseOnClose 构造器预释放** | ByteBufInputStream |
| **threadLocalTempArray** | ByteBufUtil + UnpooledDirectByteBuf |
| **FastThreadLocalThread 优化** | ByteBufUtil |
| **DEFAULT_ALLOCATOR 系统属性切换** | ByteBufUtil |
| **calculateNewCapacity 精确边界值行为** | AbstractByteBufAllocator |
| **copyBytes(ByteBuffer) 修复 Netty#3896** | Unpooled |
| **copiedBuffer 字节序一致性检查** | Unpooled |
| **EQUALS 限制同 Class** | DefaultByteBufHolder |
| **ByteBufConvertible asByteBuf idempotent** | ByteBufConvertible |
| **isContiguous 判断机制** | ByteBuf + Composite(false) + views |
| **checkIndexBounds 三参数约束** | AbstractByteBuf |
| **setZero 8字节对齐零填充** | AbstractByteBuf + Unsafe variants |
| **setCharSequence0 编码分派** | AbstractByteBuf |
| **touch/touch(hint) 空实现** | AbstractReferenceCountedByteBuf |
| **PlatformDependent canReliabilyFree** | AbstractByteBufAllocator + UnpooledDirectByteBuf |
| **DecrementingCleanableDirectBuffer** | UnpooledByteBufAllocator |
| **copy 目标 direct 优先** | ReadOnlyUnsafeDirectByteBuf |

---

## 02 深度分类

### 🔴 Deep (承载核心设计决策 — Netty 区别于 JDK NIO 的根本)

| KP | 为什么🔴 |
|----|---------|
| **双指针模型(readerIndex/writerIndex)** | Netty ByteBuf 最核心的设计 — JDK Buffer 单 position 无法区分读/写, 导致 flip() 后必须 clear() 才能写 |
| **三区段(discardable/readable/writable)** | 废弃+可读+可写三区让 pipeline 中多个 handler 无需 copy 即可访问不同片段 |
| **discardReadBytes 内存压缩** | 关键性能权衡 — O(N) arraycopy vs 零写空间浪费; Composite 版本不同(释放组件) |
| **CAS 引用计数(retain/release)** | 替代 GC 的确定性内存管理 — Netty 为什么不用 GC: Direct 内存不受 GC 完全控制 |
| **deallocate() 模板方法** | Heap=NOOP, Direct=freeMemory, Pooled=归还池 — 多态释放 |
| **derive vs copy 语义** | slice/duplicate 零拷贝共享 + 独立索引 — 设计哲学: 默认零拷贝, read/write offset 是元数据 |
| **ByteBufAllocator SPI** | 分配器抽象让 Pooled/Unpooled/Adaptive 可无缝切换 — 策略模式在框架级的应用 |
| **calculateNewCapacity 三级扩容** | ≤4MiB翻倍, ==4MiB精确, >4MiB步进 — 避免大容量爆炸 |

### 🟡 Working (有设计决策, 非核心)

| KP | 说明 |
|----|------|
| **CompositeByteBuf 零拷贝拼接** | 组件模型 + Component 解包链 + consolidate — 非所有 buffer 需要复合, 但为 HTTP 消息聚合提供关键优化 |
| **AbstractDerived 引用计数全委托** | slice/duplicate 不增加 refCnt → 需要 Unreleasable 或 retained* 保护 |
| **LeakAware Simple/Advanced 双层** | 泄漏检测 — 高级记录堆栈, 简单仅计数; 生产可关闭 |
| **ensureWritable 四状态协议** | 精确定义扩容决策, 调用者可根据返回值做不同处理 |
| **heap vs direct 内存选择** | allocateArray=GC 管理 vs Cleaner+Deallocator — 确定性释放 |
| **Unsafe 加速路径** | memoryAddress + Unsafe.getByte — 绕 JNI/边界检查, 15-30% 性能提升 |
| **VarHandle 条件优化** | Java 9+ VarHandle 提供对齐操作, LE 有原生优势 |
| **directByDefault 策略** | canReliabilyFree 必须满足 → 池化兜底 |
| **Swapped 字节序装饰器** | HE 和 LE 间透明转换 — 包装而非拷贝 |
| **ReadOnly 写拦截门面** | 防止 pipeline 中误写 — 编译期不可见 |
| **FixedComposite 固定只读复合** | 多 buffer 聚合一次性视图 — wrappedUnmodifiableBuffer 使用 |
| **ByteBufHolder replace 模式** | 不可变替换 — 支持 copy/duplicate/retainedDuplicate 的模板方法 |

### 🟢 Surface (机制性了解即可)

| KP | 放在哪 |
|----|-------|
| **Empty Null Object 模式** | 分配器零容量优化 — 和 slice/duplicate 一起 |
| **Unreleasable 阻断释放** | 和 Unpooled 工厂一起 |
| **InputStream/OutputStream 适配** | 和 NIO 互操作一起 |
| **ByteBufUtil hashCode/equals/compare** | 和 ByteBuf API 一起 |
| **SWAR search** | 工具类辅助 |
| **writeUtf8 多路径** | 工具类辅助 |
| **HexUtil 调试** | 工具类辅助 |
| **setZero 零填充优化** | 和 capacity 扩容一起 |
| **touch 泄漏追踪** | 和泄漏检测一起 |
| **alloc 追踪调用链** | 和分配器一起 |
| **DEFAULT_ALLOCATOR 切换** | 和分配器一起 |
| **checkBounds/checkAccessible 开关** | 和 AbstractByteBuf 一起 |
| **mark/reset 标记** | 和双指针一起 |

---

## 03 聚类

### Cluster A: 双指针与内存模型 (10 KPs) — 零前置依赖, 直接回答"为什么重新发明"

机制边界: ByteBuf 与 JDK NIO Buffer 的本质差异 — 双指针 → 三区段 → 读写分离

1. readerIndex/writerIndex 双指针
2. 三区段: discardable/readable/writable
3. discardReadBytes/discardSomeReadBytes 压缩策略
4. capacity/maxCapacity 二级容量
5. ensureWritable 四级状态 + ensureWritable0 自动扩容
6. clear() vs mark/reset vs rewind
7. maxFastWritableBytes 度量
8. equals/compareTo/hashCode 内容比较
9. hasArray/hasMemoryAddress/hasDirect 类型检测
10. isContiguous 连续性判断

### Cluster B: 引用计数与生命周期 (8 KPs) — 依赖 A, 回答"为什么不靠 GC"

机制边界: AbstractReferenceCountedByteBuf 的 CAS 实现 → deallocate → 释放 → 泄漏检测

1. volatile RefCnt + CAS retain/release
2. deallocate() 模板方法 (Heap=NOOP, Direct=free, Pooled=归还)
3. ensureAccessible/checkAccessible 已释放守卫
4. touch/track 调用链追踪
5. ResourceLeakDetector factory 创建
6. SimpleLeakAwareByteBuf: Decorator + ResourceLeakTracker
7. AdvancedLeakAwareByteBuf: 全量操作堆栈记录
8. Composite 泄漏感知 (Simple/Advanced)

### Cluster C: 分配器体系 (12 KPs) — 依赖 A+B, 回答"怎么创建"

机制边界: ByteBufAllocator SPI → Abstract模板方法 → Pooled/Unpooled/Adaptive 三种策略

1. ByteBufAllocator SPI: buffer/ioBuffer/heapBuffer/directBuffer/compositeBuffer
2. AbstractByteBufAllocator: newHeapBuffer/newDirectBuffer 模板方法
3. directByDefault 路由策略 + canReliabilyFree 检查
4. calculateNewCapacity 三级扩容 (≤4MiB翻倍, =4MiB精确, >4MiB步进)
5. toLeakAwareBuffer 自动包装
6. ioBuffer() I/O 场景语义
7. UnpooledByteBufAllocator: 三轴配置 (metric/leakDetector/noCleaner)
8. AdaptiveByteBufAllocator: 反代际假说 + AdaptivePoolingAllocator
9. DEFAULT_ALLOCATOR 系统属性切换 (pooled/unpooled/adaptive)
10. Unpooled.java 工厂: wrappedBuffer 多路分发
11. Unpooled.copiedBuffer 深拷贝: 字节序一致性检查 + 编码优化
12. Instrumented 度量体系 (LongAdder)

### Cluster D: Heap vs Direct 内存实现 (12 KPs) — 依赖 A+C, 回答"存在哪里"

机制边界: byte[] vs off-heap memory — 分配/访问/释放三阶段差异

1. **Heap**: allocateArray = new byte[N], freeArray = NOOP (GC)
2. **Heap**: _getByte = hb[ix(index)], System.arraycopy compact/capacity
3. **Heap**: ByteOrder.BIG_ENDIAN 固定
4. **Unsafe Heap**: allocateUninitializedArray 跳过零填充
5. **Unsafe Heap**: UnsafeByteBufUtil.getByte(array, BYTE_ARRAY_BASE_OFFSET + index)
6. **Direct**: CleanableDirectBuffer + Cleaner + Deallocator
7. **Direct**: capacity 单独缓存 (ByteBuffer.remaining 不可靠)
8. **Direct**: doNotFree 防释放非自有内存
9. **Direct**: _getByte = buffer.get(index) JNI 底层
10. **Unsafe Direct**: memoryAddress 缓存 + addr(index) = memoryAddress + index
11. **Unsafe Direct**: UnsafeByteBufUtil direct memory 读写
12. **VarHandle 加速**: getShort/Int/Long 条件优化

### Cluster E: 视图与零拷贝 (10 KPs) — 依赖 A+B+D, 回答"怎么避免拷贝"

机制边界: slice/duplicate → 共享数据+独立索引 → 视图链 → ReadOnly/Empty/Unreleasable

1. slice(): 子区域共享 + adjustment 偏移
2. duplicate(): 全量共享 + 同坐标空间
3. AbstractDerived: 引用计数全委托 unwrap()
4. retainedSlice/retainedDuplicate: slice+retain
5. ReadOnly: 写拦截门面 + nioBuffer asReadOnlyBuffer
6. Empty: Null Object 模式
7. Unreleasable: 阻断释放保护
8. Swapped: 字节序装饰器 (读后/写前交换)
9. AbstractUnsafeSwapped: 硬件非对齐加速
10. multi-layer slicing: adjustment 累加 + 嵌套解包

### Cluster F: Composite 复合缓冲区 (10 KPs) — 依赖 A+B+E, 回答"怎么拼接零拷贝"

机制边界: 多 ByteBuf 虚拟聚合 → Component 组件体系 → consolidate 合并

1. Composite: 虚拟拼接 + components[] 动态数组
2. Component: 双指针(srcBuf+buf) + 双向索引映射
3. addComponent/addFlattened: 组件管理 + finally 回滚
4. findComponent: 二分 + lastAccessed 弱缓存
5. toComponentIndex: 偏移到组件映射, O(log n)
6. consolidate: allocBuffer → transferTo → 替换
7. discardReadComponents vs discardReadBytes
8. cross-component multi-byte read: 分段策略
9. FixedComposite: 固定只读 + ByteOrder 一致性检查
10. nioBuffer 多组件合并/零拷贝提取

### Cluster G: 工具与适配 (6 KPs) — 穿插在 A~F 中

1. ByteBufUtil.hashCode/equals/compare: 4 种 endian 组合
2. writeUtf8 5 路径优化
3. SWAR firstIndexOf/lastIndexOf
4. swapShort/swapInt/swapLong 原语
5. ByteBufHolder: content + replace 模板方法
6. InputStream/OutputStream 适配: releaseOnClose + startIndex

### 教学顺序: A → B → C → D → E → F → G
A(双指针) → B(引用计数) → C(分配器) → D(内存实现) → E(视图) → F(复合) → G(工具穿插)
