# Ch4 Heap vs Direct — 存在哪里, 为什么重要

> Cluster D: 12 KPs | 依赖 §4.2 分配器 | §4.2 → §4.3

### 1. Heap 实现 — `byte[]` + GC 托管

场景: 业务逻辑处理的 90% 的 ByteBuf 不需要直接参与 Socket I/O——`byte[]` 在 JVM 堆里, 分配极快, GC 自动回收。Heap 是默认的最优选择。

源码路径: `UnpooledHeapByteBuf.java:84-86` — `allocateArray(initialCapacity) = new byte[initialCapacity]`——JVM TLAB(Thread-Local Allocation Buffer)中分配, 比 Direct 的 `UNSAFE.allocateMemory` 快两个数量级。`UnpooledHeapByteBuf.java:50-82` — 构造器双形态: `new array` 时 `setIndex(0,0)`(空的, 准备写); `wrap existing array` 时 `setIndex(0, array.length)`(包裹, 标记整个数组已可读)。`UnpooledHeapByteBuf.java:40,42` — `alloc` 字段保留分配器引用(用于 copy 时创建同类 buffer), `tmpNioBuf` 是惰性的 `ByteBuffer.wrap(hb)` 缓存——首次 `nioBuffer()` 调用时创建并缓存, 避免重复 wrap(减少 GC 压力)。

关键设计: `_getByte(i) = hb[ix(i)]` (UnpooledHeapByteBuf.java:333-334)——纯数组索引。`ix()` 计算 `index + offset`——offset 是历史遗留的分段字段。HotSpot 在热路径上内联 `_getByte` → 代码直接编为 `mov al, [rbx + rcx * 1 + offset]`——零方法调用开销。`freeArray() = NOOP`——`byte[]` 由 GC 自动回收, 不需要显式释放。

数据流: `alloc.heapBuffer(1024)` → `allocateArray(1024)` → `hb = new byte[1024]`(TLAB) → `readerIndex=0, writerIndex=0` → 用户 `writeBytes(data)` → `hb[writerIndex++] = data[i]` → 释放时 `release()` → `deallocate()` → `freeArray()`(NOOP)。

### 2. Direct 实现 — 堆外内存 + Cleaner 释放

场景: Socket I/O 使用 HeapByteBuf 时, JNI 需要把 `byte[]` 拷贝到堆外临时缓冲区(`GetByteArrayRegion`)再传给 `write(fd, buf, len)`——每次 I/O 都拷贝。Direct buffer 的地址直接传给 OS——零拷贝。

源码路径: `UnpooledDirectByteBuf.java:59-71` — `allocateDirectBuffer` → `PlatformDependent.allocateDirect(capacity)` → 返回 `CleanableDirectBuffer`(封装 `java.nio.ByteBuffer` + `Runnable cleanable`)。`UnpooledDirectByteBuf.java:46` — `capacity` 单独缓存——因为 `ByteBuffer.remaining()` 受 position/limit 影响, 不能用作稳定的容量查询。`UnpooledDirectByteBuf.java:47` — `doNotFree` 标志——包装外部 ByteBuffer(用户传入)时设为 true, 防止 deallocate 时释放不属于本对象的堆外内存。`UnpooledDirectByteBuf.java:132-153` — `setByteBuffer(newBuffer, tryFree)`: `tryFree=true` 时先释放旧 buffer(优先 `cleanable.clean()`, 其次 `PlatformDependent.freeDirectBuffer(oldBuffer)`, `doNotFree` 例外跳过)。

关键设计: `_getByte(i) = buffer.get(i)` ——走 JDK NIO ByteBuffer 的 JNI 调用, 而非 Unsafe 直接访问。DirectByteBuf 的 `_getInt`/`_getLong` 有 VarHandle 条件加速: `PlatformDependent.hasVarHandle()` 时用 `VarHandleByteBufferAccess.getIntBE(buffer, index)` 绕过 ByteBuffer 的方法调用开销。`allocateDirect` 分配慢但 I/O 零拷贝——这个"分配慢、使用快"的权衡在 Pooled(§8)中得到弥补: 只分配一次, 反复复用。

数据流: `alloc.directBuffer(1024)` → `Bits.reserveMemory(1024)` 检查总池 → `PlatformDependent.allocateDirect(1024)` → `UNSAFE.allocateMemory(1024)` → `new DirectByteBuffer(address, cap)` → `channel.write(buf)` → JNI `GetDirectBufferAddress(buf)` → `write(fd, address, len)` → `release()` → `deallocate()` → `cleanable.clean()` → `UNSAFE.freeMemory(address)` → `address=0`。

### 3. Unsafe 路径 — 绕 JNI/边界检查的加速

场景: `DirectByteBuffer._getInt(i)` 要走 `buffer.getInt(index)`——这是一个 JNI 调用, 有同步开销。`UnpooledUnsafeDirectByteBuf` 缓存了堆外内存的起始地址, 用 `Unsafe.getInt(address + i)` 直接从物理地址读取——绕过了 JNI。

源码路径: `UnpooledUnsafeDirectByteBuf.java:36` — `long memoryAddress` 缓存堆外内存起始地址——`PlatformDependent.directBufferAddress(buffer)` 一次性获取。`UnpooledUnsafeDirectByteBuf.java:363-365` — `addr(int index) = memoryAddress + index`——单次加法, O(1)。`_getByte(i) = UnsafeByteBufUtil.getByte(addr(i))`——`Unsafe.getByte(address)` 内联为 `mov al, [rax]`——和堆数组一样的零方法调用开销。`UnpooledUnsafeHeapByteBuf.java:38-40,49-51` — Unsafe Heap 的对称: `allocateUninitializedArray`(跳过 `new byte[N]` 的零填充), `_getByte = UnsafeByteBufUtil.getByte(array, BYTE_ARRAY_BASE_OFFSET + index)`——绕过 JVM 数组边界检查。

关键设计: Unsafe 路径的设计意图是"让堆外内存访问和堆内存访问一样快"。`memoryAddress` 的一次性缓存避免了每次访问都调 `PlatformDependent.directBufferAddress()`。但 Unsafe 绕过了 JVM 的安全检查——`addr(index)` 没有 `index >= 0 && index < capacity` 检查——如果一个 bug 导致 index 越界, 你会直接读/写任意物理内存(Core Dump)。Netty 在前置的 `ensureAccessible()+checkIndex()` 中做了边界防护, Unsafe 只用于已确认安全的路径。

数据流: Direct: `buf.getInt(4)` → `buffer.getInt(4)`(JNI) → ~50ns。Unsafe Direct: `buf.getInt(4)` → `UnsafeByteBufUtil.getInt(addr(4))` → `Unsafe.getInt(addr)`(内联单条指令) → ~10ns。

### 4. Heap vs Direct 三路跨 Buffer 拷贝

场景: `srcBuf.getBytes(0, dstBuf, 0, 4096)` ——要把一个 buffer 的数据复制到另一个。如果两个都是 Heap(buffer 底层都是 `byte[]`), 用 `System.arraycopy`——JVM intrinsic, 高效。如果一个 Heap 一个 Direct——需要跨内存类型拷贝。

源码路径: `UnpooledHeapByteBuf.java:167-177,246-256` — `getBytes(src→dst)` 和 `setBytes(dst←src)` 都是三路分发: 1) `dst.hasMemoryAddress() && PlatformDependent.hasUnsafe()` → `copyMemory(srcAddr, dstAddr, length)`——Unsafe 直接内存拷贝, 最快; 2) `dst.hasArray()` → `System.arraycopy(src.hb, srcIdx, dst.hb, dstIdx, length)`——堆数组拷贝; 3) 回退: `dst.setBytes(index, src, srcIndex, length)`——逐字节循环。

关键设计: 三路分发的"积极匹配"策略——检查最优路径(memoryAddress→Unsafe copyMemory)、次优路径(hasArray→System.arraycopy)、兜底路径(逐字节回退)。`PlatformDependent.hasUnsafe()` 的检查在类加载时已完成——不是每次 getBytes 都检查。Netty 的 CompositeByteBuf 的 `_getInt` 也有类似的多路分发: 同组件内直接委托, 跨组件逐字节拼接(§4.5)。

数据流: Heap→Direct: `getBytes(0, directBuf, 0, N)` → `directBuf.hasMemoryAddress() && hasUnsafe()` → `copyMemory(srcAddr, dstAddr, N)` → Direct→Heap: 对称。Heap→Heap: `src.hasArray() && dst.hasArray()` → `System.arraycopy(src.hb, srcIdx, dst.hb, dstIdx, N)`。

### 核心悬念

**"Unsafe 绕过了 JVM 的边界检查——如果 index 越界, 直接写物理内存。Netty 在 ensureAccessible()+checkIndex() 中做了前置防护——但这些检查在热路径上也是成本。Ch4 §4.4 的视图操作(slice/duplicate/readOnly)在 Heap 和 Direct 上的行为完全一致——因为 _get/_put 的 derivation 机制被 AbstractDerivedByteBuf 统一了。但当派生 buffer 被释放时, parent buffer 的数据还在——Ch4 的引用计数和 Ch8 的内存池化(§8.1 PoolArena.allocateNormal)怎么保证 '一个派生视图的 release 不会提前释放还在被另一个视图使用的 parent 内存'？"**

→ 引出 §4.4 视图与零拷贝 — UnpooledHeapByteBuf 和 UnpooledDirectByteBuf 的差异在 slice()/duplicate()/asReadOnlyBuffer() 中消失了——三个视图操作在不同的底层存储上表现完全一致。泄漏检测(SimpleLeakAwareByteBuf/AdvancedLeakAwareByteBuf)怎么追踪派生视图的 use-after-free?
