# Ch4 双指针与引用计数 — 为什么重新发明，为什么不靠 GC

> Cluster A+B: 18 KPs | 依赖 Ch3 Selector | Ch4 → §4.2

### 1. 双指针模型 — 为什么单 position 不够

场景: Ch1-Ch3 的 JDK NIO ByteBuffer 用单 position——同一个指针既要推进读又要推进写。Pipeline 中有三个 Handler: Handler A 写入了 100 字节, flip(), Handler B 读了前 40 字节——position 推进到 40。Handler C 需要写但 position 指向 40——要先 compact() 把未读数据前移, 再把 position 移到末尾才能写。每次 Handler 切换都在手动维护 flip/compact/clear 的状态——这就是 NIO 的"单 position 状态机"在多 Handler Pipeline 中的崩溃。

源码路径: `ByteBuf.java:59-63` — `readerIndex` 和 `writerIndex` 是两个独立的 int 字段。`readXXX()` 推进 readerIndex, `writeXXX()` 推进 writerIndex, 互不干扰。invariant: `0 ≤ readerIndex ≤ writerIndex ≤ capacity` (AbstractByteBuf.java:110-116)。`setIndex0(reader, writer)` 无校验直接设置——内部操作如 `clear()` 调用 setIndex0(0,0) 同时复位两个指针 (AbstractByteBuf.java:1484-1487)。`maxFastWritableBytes() = capacity - writerIndex`——不移动 readerIndex 就能获得的最大连续可写空间 (ByteBuf.java:425-431)。

关键设计: 双指针把 NIO 的"写模式→flip→读模式→compact→写模式"状态机消灭了——read 和 write 各自独立推进自己的指针, 不再需要模式切换。`readByte()` 只需要 readerIndex<writerIndex——如果有数据就能读。`writeByte()` 只需要 writerIndex<capacity——如果有空间就能写。这就是"read and write happen at the same time without flip"——Pipeline 的每个 Handler 都同时是 reader 和 writer, 不需要知道"上一个 Handler 是 write 模式还是 read 模式"。

数据流: 双指针三区段: `[0, readerIndex)` = discardable(已消费可回收), `[readerIndex, writerIndex)` = readable(当前可读), `[writerIndex, capacity)` = writable(可写空间)。三区段天然分界线——读只读 readable, 写只写 writable, 回收只回收 discardable, 三个操作在同一个 buffer 上互不冲突。

### 2. discardReadBytes — O(N) 压缩的两难

场景: readerIndex 推进到 4096——前面 4096 字节已经消费过, 不会再用。但可写空间只有 `capacity - writerIndex`——如果这个空间不够, 需要扩容。能不能在扩容之前先把前面 4096 字节"消除", 把数据前移到位置 0?

源码路径: `AbstractByteBuf.java:216-233` — `discardReadBytes()`: `setBytes(0, this, readerIndex, writerIndex - readerIndex)` 把可读区间 [readerIndex,writerIndex) 拷贝到位置 0, 然后 `writerIndex -= readerIndex, readerIndex = 0`。内部用 `System.arraycopy` 实现——O(N) 代价。`discardSomeReadBytes()` (AbstractByteBuf.java:235-255): 仅当 `readerIndex >= capacity()/2` 时执行——牺牲少量可写空间换取减少 O(N) 拷贝次数。`adjustMarkers()` (AbstractByteBuf.java:257-269): 数据前移后, markedReaderIndex/markedWriterIndex 相应递减(不能为负)。

关键设计: `discardReadBytes()` 是容量和性能之间的权衡——O(N) 压缩 vs 扩容分配新内存。Netty 选择 O(N) 但避免 GC 压力(扩容需要重新分配+释放旧内存)——相比之下, `discardSomeReadBytes()` 是一种"懒惰压缩": 只有过半的 buffer 是垃圾时才清理, 避免频繁的 O(N) 拷贝。CompositeByteBuf 的 `discardReadComponents()` 把 O(N) 变成了 O(1)——因为它直接释放整块已读的 Component, 不拷贝数据——这是从 Ch4 到 Ch7 的演进线。

数据流: `readBytes(N)` 推进 readerIndex 到 N → writable space = capacity - writerIndex → 空间不够 → `discardReadBytes()` → readerIndex=0, writerIndex=writerIndex-N → writable space +N。

### 3. 容量与扩容 — capacity/maxCapacity 二级模型

场景: ByteBuf 创建时设了 initialCapacity=256, 但消息体可能是 4KB——你需要在写入时自动扩容, 但又要防止无限增长吃掉内存。

源码路径: `ByteBuf.java:253-269` — `capacity(int newCapacity)` 可动态调整, `maxCapacity()` 是构造时设定的一次性硬上限。`ensureWritable(int minWritableBytes)` 四级状态: 0=空间足够, 1=不足但未扩(force=false), 2=已扩容, 3=已扩至 maxCapacity 仍不足(ByteBuf.java:530-555)。`ensureWritable0(int minWritableBytes, boolean force)` — 热路径用非短路 & 减少分支, JIT 友好(AbstractByteBuf.java:284-306)。`trimIndicesToCapacity(int newCapacity)` — 缩容时若 writerIndex>newCapacity 则截断到 newCapacity (AbstractByteBuf.java:272-276)。

关键设计: capacity 可动态调, maxCapacity 不可调——二级模型防止业务逻辑通过反复扩容把内存吃光。`ensureWritable` 的四级状态让调用方精确判断: 0 继续写/1 写不下了(上层决定)/2 扩容了继续写/3 到顶了不能再写。这种精确的返回值语义是 Netty 异步模型的基础——调用方不需要 catch 异常来判断容量不足。

数据流: `writeBytes(data, 4096)` → `ensureWritable(4096)` → 检查 writable space → 不足: `calculateNewCapacity()` 计算新容量(§4.2 分配器) → `capacity(newCapacity)` 扩容 → 写 data → 超过 maxCapacity: ensureWritable 返回 3 → 上层记录异常。

### 4. 引用计数 — volatile + CAS 替代 GC

场景: Ch1 的 DirectByteBuffer 用 Cleaner(PhantomReference) 被动回收堆外内存——GC 不触发, Cleaner 不执行, 堆外内存泄漏。Netty 需要确定性释放: 每次 release 自己检查 refCnt==0, 立即释放。

源码路径: `AbstractReferenceCountedByteBuf.java:27` — `RefCnt` 封装 `volatile int` 引用计数。`retain(int increment)` — CAS 递增, 返回 this 链式调用(AbstractReferenceCountedByteBuf.java:60-68)。`release(int decrement)` — CAS 递减, 归零→`handleRelease()`→`deallocate()`(AbstractReferenceCountedByteBuf.java:82-96)。`isAccessible()` — 使用 non-volatile 读取 `refCnt()>0`? 判断, best-effort guard, race condition 是允许的(AbstractReferenceCountedByteBuf.java:33-38)——因为 `release()` 的 CAS 最终会失败并正确归零。

关键设计: CAS 让 retain/release 无锁并发——多线程共享同一 ByteBuf 不需要 synchronized。`isAccessible()` 用 non-volatile 读取是刻意为之——这是一个"最优猜测"防护: 如果读到了 refCnt>0, 大概率对象是存活的; 如果读到了 refCnt=0, 则抛异常拒绝访问。这个 best-effort 设计接受假阴性(刚释放就检查)和假阳性(CAS 还没写入), 但避免了每次内容访问都 volatile 读。

数据流: `create() → refCnt=1` → HandlerA: `retain() → refCnt=2` → HandlerA 完成: `release() → refCnt=1` → HandlerB 完成: `release() → refCnt=0 → handleRelease() → deallocate()` → Heap: NOOP / Direct: cleanable.clean() / Pooled: 归还 PoolArena(Ch8)。

### 5. deallocate 模板方法 — 多态释放

场景: HeapByteBuf 的底层是 `byte[]`(GC 回收), DirectByteBuf 的底层是 `UNSAFE.allocateMemory`(需要调用 `UNSAFE.freeMemory`), PooledByteBuf 需要归还到 PoolArena——三种释放方式完全不同。但调用方只需要知道 `release()`。

源码路径: `AbstractReferenceCountedByteBuf.java:101-102` — `deallocate()` 是 `protected abstract` 方法——每个子类实现自己的释放逻辑。`handleRelease(boolean result)` → 若 result=true(refCnt 已归零) → 调用 `deallocate()`。Heap 实现: `freeArray() = NOOP`——`byte[]` 由 GC 回收, 不需要显式释放。Direct 实现: `cleanable.clean()`(Cleaner 路径) 或 `PlatformDependent.freeDirectBuffer(buffer)`(直接释放)。Pooled 实现: `arena.free(chunk, handle)`——归还内存到 ChunkList, `recyclerHandle.unguardedRecycle(this)`——对象归还到 Recycler(Ch8)。

关键设计: 模板方法模式让释放逻辑完全由子类多态决定——调用方不关心底层是堆、堆外、还是池化。这和 Ch1 的 Cleaner 被动回收形成对比: 引用计数归零是确定的、即时的、调用方触发的——不依赖 GC 的调度。但代价也是明显的——每次 retain 都需要匹配一次 release, 遗漏 release 导致内存泄漏, 遗漏 retain 导致 use-after-free。

数据流: `release() → handleRelease() → deallocate()` → Heap: return (no-op) / Direct: `cleanable.clean()` → `UNSAFE.freeMemory` / Pooled: `arena.free(chunk, handle)` → arena ChunkList 回收 + `recyclerHandle.recycle(this)` → Recycler 对象池。

### 6. ensureAccessible + checkBounds — 双重守卫

场景: `buf.writeByte(1)` —— `buf` 已经被 release 了(refCnt==0)。调用方没有感知, 直接写了——数据写进了已经被释放的内存(use-after-free)。需要一层入口守卫: 任何内容访问之前检查 buffer 是否还存活。

源码路径: `AbstractByteBuf.java:1478-1482` — `ensureAccessible()`: 每个内容访问方法(map/write/set)入口调用, 若 buffer 已被释放(`refCnt==0`)则抛 `IllegalReferenceCountException`。`AbstractByteBuf.java:1385-1424` — `checkIndex(index, length)`: 先调 `ensureAccessible()` 再调 `checkIndex0()`, 双重防御。系统属性开关: `io.netty.buffer.checkBounds` 关闭边界检查, `io.netty.buffer.checkAccessible` 关闭可达性检查(AbstractByteBuf.java:51-66)——两个开关可独立控制, 生产环境可关闭以获取性能提升。

关键设计: ensureAccessible 是非保护性的——在 refCnt race condition 下可能漏检(因为 isAccessible 是 non-volatile 读)。但这不是问题——release 的 CAS 最终会使 refCnt 归零, 下一次内容访问会正确抛异常。这种 optimistic check 在大多数情况下有效, 只在高并发 release 时可能漏检——但总比每次 volatile 读的性能代价小。

数据流: `writeByte(1)` → `ensureAccessible()`: refCnt==0 → `IllegalReferenceCountException("freed")` → 调用方获知已释放。`refCnt>0` → `checkIndex(writerIndex)`: 边界检查 → `_setByte(writerIndex, 1)` → `writerIndex++`。

### 核心悬念

**"引用计数让 Netty 不再依赖 GC 的被动回收——但谁来保证每一次 retain 都有对应的 release? 多线程共享一个 ByteBuf 时, A 线程 retain, B 线程 release, C 线程又 retain——引用计数不会乱吗? 泄漏检测(SimpleLeakAwareByteBuf/AdvancedLeakAwareByteBuf)怎么在运行时发现 'retain 了但忘了 release' 的 bug? §4.2 分配器体系回答了 '怎么创建'——calculateNewCapacity 的三级扩容策略(≤4MiB翻倍, =4MiB精确, >4MiB步进)是 ByteBufAllocator 的核心设计。"**

→ 引出 §4.2 分配器体系 — 双指针和引用计数定义了 ByteBuf 是什么, 但怎么创建它? ByteBufAllocator 的 buffer/ioBuffer/heapBuffer/directBuffer SPI 接口如何让 Pooled/Unpooled/Adaptive 三种策略无缝切换?
