# Ch4 视图与零拷贝 — slice/duplicate 的共享语义与泄漏检测

> Cluster E: 10 KPs | 依赖 §4.3 Heap/Direct | §4.3 → §4.4

### 1. derive vs copy — Netty 零拷贝的设计哲学

场景: Ch1 的 ByteBuffer.slice() 共享底层数据, 但共享单 position/limit——NIO 没办法让两个 handler 同时读写同一个 buffer 的不同区段。Netty 的 slice/duplicate 共享底层数据 + 独立 readerIndex/writerIndex——每个 handler 看到的是"自己的 buffer", 但底层数据不拷贝。

源码路径: `ByteBuf.java:195-213` — `slice(int index, int length)` 返回共享底层数据 + 独立 readerIndex/writerIndex 的派生 buffer——仅有元数据代价(两个 int 字段)。`duplicate()` 返回全量共享视图。`copy()` 返回独立副本——`alloc.buffer(readableBytes)` + `writeBytes(original)`(ByteBuf.java)。`retainedSlice/retainedDuplicate` = slice/duplicate + retain()——slice 默认不增加 refCnt(派生视图不阻止 parent 被释放), retained 变体保证 parent 不会被提前释放(ByteBuf.java:215-221)。

关键设计: `slice()` 默认不 retain 是刻意为之——派生视图的生命周期应该由 parent 控制。如果 slice 自动 retain, 每次 slice 都会让 parent 的 refCnt+1——100 次 slice 后 release 100 次才能释放 parent。`retainedSlice()` 提供了"我需要这个视图阻止 parent 释放"的显式选择——用户决定引用计数的传播。

数据流: `buf.slice(4, 8)` → 新 UnpooledSlicedByteBuf(offset=4, len=8, parent=buf) → readerIndex=0, writerIndex=8 → `slice.getByte(0)` = `parent._getByte(4+0)` ——底层是同一个 `byte[]` 或 `memoryAddress`。`slice.release()` → 委托 `parent.release()`(AbstractDerived)。

### 2. AbstractDerivedByteBuf — 引用计数全委托

场景: slice/duplicate/readOnly 都是"派生派生缓冲区"——它们没有独立的底层存储, 所有数据操作(包括引用计数)都必须委托 parent。

源码路径: `AbstractDerivedByteBuf.java:44-108` — `refCnt()/retain()/release()/touch()` 全部委托 `unwrap()`。`refCnt0() = unwrap().refCnt()`——派生自身无独立引用计数(AbstractDerivedByteBuf.java:48-49)。`release0() = unwrap().release()`——不能独立于 parent 被释放(AbstractDerivedByteBuf.java:94-98)。`isAccessible()/isReadOnly()` 也委托(AbstractDerivedByteBuf.java:35-113)。

关键设计: 派生缓冲区的引用计数全委托使 slice/duplicate "免费"——不需要增加 parent refCnt, 不需要维护自己的 refCnt 字段。但代价是 parent 释放后, 所有派生视图都变为"悬空引用"——`isAccessible()` 返回 false。这是 §4.1 讨论的"release 后使用"问题的另一种形式: 不是 release 了同一个 buffer 继续用, 而是 release 了 parent 后, 派生视图还不知道 parent 已释放。

数据流: `parent.retain() → refCnt=1` → `duplicate()` → duplicate.refCnt=1(委托) → `parent.release() → refCnt=0 → deallocate()` → duplicate.refCnt=0 → `duplicate.ensureAccessible()` → `IllegalReferenceCountException`。

### 3. slice/duplicate — 子窗口共享 + adjustment

场景: 一个 HTTP 消息 buffer 的 [0,4) 是 magic bytes, [4,12) 是 header, [12,readerIndex) 是 body。Handler A 需要 body, Handler B 需要 header——两个 slice, 共享同一个底层 buffer, 各自独立指针。

源码路径: `AbstractUnpooledSlicedByteBuf.java:34` — `adjustment` 字段将切片本地索引翻译为原始 buffer 索引。`AbstractUnpooledSlicedByteBuf.java:473-475` — `idx(index) = index + adjustment`——所有 `_get/_set` 调用的索引转换公式。`AbstractUnpooledSlicedByteBuf.java:218-224` — `duplicate() = slice(0, capacity()) + 保留读写索引`——Duplicate 通过全量切片实现。`AbstractUnpooledSlicedByteBuf.java:36-49` — 多层嵌套解包: 识别嵌套 `AbstractUnpooledSlicedByteBuf` → 累加 adjustment(多层切片不嵌套, 直接作用原始 buffer)。`UnpooledSlicedByteBuf.java:38-125` — `_getXxx(idx)/_setXxx(idx)` 走 `unwrap()._getXxx(idx)`——旁路 public checkIndex, 直接调用内部方法。

关键设计: slice 的 capacity 固定为构造时的 length——不可扩缩容(`SlicedByteBuf.java:88-90`)。这是逻辑上的约束——切片只是 parent 的一个窗口, 不能超出这个窗口写数据。多层切片的 adjustment 累加避免了嵌套代理链——`slice.slice(2,4)` 的 adjustment 是 `originalAdjustment+2`。

数据流: `buf.slice(4, 8)` → adjustment=4, readerIndex=0, writerIndex=8 → `slice.getByte(0)` → `idx(0) = 0+4 = 4` → `parent._getByte(4)` → parent 的 `hb[4+offset]`(或 `memoryAddress+4`)。`slice.writeByte(5)` → `ensureWritable(1)` + `_setByte(writerIndex(), 5)` → writerIndex++。

### 4. ReadOnly / Empty / Unreleasable — 三种特殊包装

场景: 配置数据(如白名单)放在 buffer 里——Handler 只读, 但 pipeline 中其他 handler 可能误调用 `writeByte()` 修改它。ReadOnly 是写拦截门面。

源码路径: `ReadOnlyByteBuf.java:53-66` — `isReadOnly()/isWritable()` 始终返回 true/false, 所有 `setXxx()/writeXxx()/discardReadBytes()/capacity()` 抛 `ReadOnlyBufferException`(ReadOnlyByteBuf.java:74-247)。`EmptyByteBuf.java:40,74-76` — Null Object 模式, capacity/maxCapacity=0, refCnt=1 固定, `slice/duplicate/copy` 返回 this(EmptyByteBuf.java:865-896)。`UnreleasableByteBuf.java:22-25,104-131` — 包装 buffer 阻止 release 操作——retain/release/touch 全部 no-op, `retainedSlice()` 委托 `slice()` 而非 `retained*`(避免不可释放造成引用计数泄漏)(UnreleasableByteBuf.java:71-76)。

关键设计: ReadOnly 不仅拦截 write 操作——`hasArray()` 也返回 false(ReadOnlyByteBuf.java:100-102), `nioBuffer()` 返回 `.asReadOnlyBuffer()`(ReadOnlyByteBuf.java:398-411)——从 Java 层到 NIO 层的全路径只读保护。Empty 的 `slice/duplicate/copy` 全部返回 this——不创建新对象, 永远不产生垃圾。

数据流: `buffer.asReadOnly()` → 新 ReadOnlyByteBuf(parent=buffer) → `readOnly.writeByte(1)` → `ReadOnlyBufferException` → 调用方不可写。`buffer.unreleasable()` → 新 UnreleasableByteBuf(delegate=buffer) → `unrel.release()` → 返回 false, 实际 refCnt 不变。

### 5. Swapped — 字节序装饰器

场景: 数据在网络上传输用 BigEndian(TCP/IP 标准)——但你的本地 CPU 是 LittleEndian(x86)。读取网络 buffer 时需要 swap 每个 int/short/long 的字节序——但不能修改原始 buffer。

源码路径: `SwappedByteBuf.java:43-49` — 构造时自动计算补序: `BIG_ENDIAN→LITTLE_ENDIAN, LITTLE_ENDIAN→BIG_ENDIAN`。`getShort(i)` → `buf.getShort(i)` → `ByteBufUtil.swapShort(result)`——读后交换。`setShort(i, v)` → `ByteBufUtil.swapShort(v)` → `buf.setShort(i, swapped)`——写前交换(SwappedByteBuf.java:243-398)。`order(ByteOrder)` 智能解包: 请求与原 buf 字节序一致→返回原 buf(剥离 SwappedByteBuf 层)——与当前序一致时返回 this(SwappedByteBuf.java:58-63)。

关键设计: SwappedByteBuf 也是一个 Decorator——和 ReadOnly/Unreleasable 一样, 它在原始 buffer 外面套了一层。套多层的顺序很重要——`new SwappedByteBuf(new ReadOnlyByteBuf(buf))` 和 `new ReadOnlyByteBuf(new SwappedByteBuf(buf))` 行为不同(前者先限制写再交换, 后者先交换再限制写——但对只读效果一样)。Netty 的解包逻辑(如 `LeakAware.wipe(SwappedByteBuf)`)会剥离这些包装层。

数据流: `buf.order(LITTLE_ENDIAN)` → 当前 `buf.order()==BIG_ENDIAN` → `new SwappedByteBuf(buf, LITTLE_ENDIAN)` → `swapped.getInt(0)` → `buf.getInt(0)`(BE 读数 0x01020304) → `swapInt(0x01020304)` → 0x04030201(LE)。

### 核心悬念

**"派生视图(slice/duplicate/readOnly)的引用计数用了'全委托'模式——派生自身没有 refCnt, 一切委托 parent。当 parent 被 release 后, 派生视图变为悬空引用。但 CompositeByteBuf 的 components 持有多个子 buffer 的引用——一个组件被 release, 整个 Composite 的 visible range 就变了——这比单 parent 的派生复杂得多。§4.5 的 Component 双引用(srcBuf+buf)和 consolidate 合并策略是 Composite 与派生视图的根本区别。"**

→ 引出 §4.5 CompositeByteBuf — 多个 ByteBuf 如何零拷贝拼成一个大缓冲区? Component 的 srcBuf/buf 双引用、addComponent 的 finally 回滚、consolidate 的合并策略——Composite 是 Netty 零拷贝机制的最后一块拼图。
