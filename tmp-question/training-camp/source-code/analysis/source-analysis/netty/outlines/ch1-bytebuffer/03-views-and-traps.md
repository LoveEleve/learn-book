# Ch1 视图与陷阱 — slice/duplicate 的共享语义与 API 暗坑

> Cluster C+D: 10 KPs | 依赖 §1.2 | §1.2 → §1.3

### 1. slice / duplicate / asReadOnlyBuffer — 共享底层数据的三分身

场景: 一条 TCP 消息的第 1-4 字节是消息长度、第 5-12 字节是消息 ID——你需要把这一个 buffer 切成三个独立的子缓冲区交给三个不同的 Handler 处理, 但不能拷贝数据。

源码路径: `Heap-X-Buffer.java.template:104` — `slice()` 构造 `new HeapByteBuffer(hb, -1, 0, remaining(), remaining(), position() + offset)` → offset 指向原始数组中的子区域起点, position=0/limit=remaining→独立的读写指针但共享 `hb` 引用。`Heap-X-Buffer.java.template:130` — `duplicate()` 构造全量 new Buffer, offset 从 0 开始但拷贝当前 position/limit/mark →独立索引+全量数据共享。`X-Buffer.java.template:601` — `asReadOnlyBuffer()` 返回一个 `HeapByteBufferR` 包装器——所有 `put` 方法抛 `ReadOnlyBufferException`。

关键设计: `slice()` 和 `duplicate()` 都在构造函数里传 offset——子 buffer 的 `_get(i)` 最终变成 `hb[offset + i]`。这是零拷贝的基础: 只需要 4 个 int 字段(position/limit/capacity/mark)的状态复制, 不需要 `System.arraycopy`。但代价也是巨大的——释放了原始 buffer 后, slice 仍持有 `hb` 引用(G1 GC 不会回收——没有引用计数), 导致缓冲区的"有效生命周期"被延长到所有派生视图都被 GC 回收。

数据流: `buffer.slice()` → 新 HeapByteBuffer(offset=pos_old, pos=0, lim=rem) → `slice.getInt(0)` = `hb[offset+0]`(位运算转换) → 原始 buffer 的 `hb[88]` 也被读到(同一个数组)。`duplicate()` 同理但 offset=0 → 共享全部底层数据。

### 2. hasArray / array — API 里埋的雷

场景: 你写了一个方法接受 `ByteBuffer` 参数, 内部用 `buf.array()` 拿到底层数组做高效的批量操作。测试用 HeapByteBuffer 全部通过——生产环境配置改为 Direct 后全线崩溃。

源码路径: `X-Buffer.java.template:1024` — `hasArray()` 在 HeapByteBuffer 中返回 true, DirectByteBuffer 返回 false(堆外内存没有 `byte[]` 数组)。`X-Buffer.java.template:1047` — `array()` 在 Heap 中返回 `hb` 引用, 在 Direct 中抛 `UnsupportedOperationException`。

关键设计: `hasArray()/array()` 的存在本身就是 NIO 设计的一个折中——为了让 Legacy 代码(大量使用 `byte[]` 的代码)能渐进迁移到 NIO, 暴露了堆缓冲区的内部数组。但它造成了"写一次, 生产环境 crash"的不可移植代码——因为 `allocateDirect` 和 `allocate` 在 API 层面看起来完全一样(`ByteBuffer buf = ByteBuffer.allocate(1024)` vs `ByteBuffer.allocateDirect(1024)`——返回类型都是 `ByteBuffer`)。

数据流: `buf.hasArray() → true → buf.array() 返回 hb → 直接操作 byte[]` ✅ 或 `buf.hasArray() → false → buf.array() → UnsupportedOperationException ❌`。

### 3. equals / compareTo — 剩余区间才是全部

场景: 你用 `buffer.putInt(42).putLong(100L)` 写入 12 字节后 `flip()`, 期望 `equals()` 比较这 12 字节——但 `remaining()` 在 flip 后是 12。如果 flip 后你又读了前 4 个字节(被消费了), remaining 变成 8, equals 只比较后 8 个字节——但你的测试代码中 position 刚好是 0, 所以测试通过。

源码路径: `X-Buffer.java.template:1307-1323` — `equals(Object ob)`: 先比较 `remaining()` →相等则逐字节比较各自 `remaining()` 范围内的内容。两个 buffer 的 capacity 可以不同、limit 可以不同、position 可以不同——只要 remaining 相同且剩余区间逐字节相等就返回 true。`X-Buffer.java.template:1347-1364` — `compareTo(ByteBuffer that)`: 在各自 `remaining()` 范围内字典序比较——先耗尽的那一方算"更小"。

关键设计: `equals()` 的语义不是"两个 buffer 的全部内容是否相同"——而是"两个 buffer 的**当前可读区间**是否相同"。这个语义设计是合理的(符合 Buffer 的"游标移动"哲学), 但对没有习惯 NIO 模型的新人来说, flip 前后 equals 结果不同是一个高频 bug。

数据流: buffer1(100 字节, pos=0, lim=100) vs buffer2(100 字节, flip 后 pos=0, lim=50) → equals 只比较 buffer1 的前 50 字节和 buffer2 的全部 50 字节 → 即使两 buffer 底层数据完全相同, equals 也返回 false。

### 4. order — 字节序的二进制分叉

场景: 网络协议(TCP/IP)规定多字节整数用 BigEndian(高字节在前)传输——`buf.putInt(0x01020304)` 在 BE 下写入 `01 02 03 04`, 在 LE 下写入 `04 03 02 01`。如果编解码两端的字节序不一致, 得到的值完全错乱。

源码路径: `X-Buffer.java.template:1624` — `order()` 返回当前 ByteOrder, Heap 默认 `ByteOrder.BIG_ENDIAN`。`X-Buffer.java.template:1651` — `order(ByteOrder bo)` 设置后影响所有多字节操作: `getInt/getLong/getDouble/putInt/putLong/putDouble` 全部依赖当前 order。内部用 `Bits.putIntB/putIntL` 两套方法实现——BE 调用 `putIntB`, LE 调用 `putIntL`。

关键设计: ByteOrder 是静态属性不是动态参数——设置一次后所有后续多字节读写都受影响。如果 Pipeline 中有两个 Handler 使用不同的字节序, 前一个 Handler 改了 order 后后一个 Handler 读到的就是乱序的——这是 NIO 设计的全局状态污染。Netty ByteBuf 在多字节 `getInt/getIntLE` 提供 LE 变体方法, 不依赖全局 order 状态。

数据流: `buf.order(LITTLE_ENDIAN)` → 全局属性切换 → `buf.putInt(v)` → 内部调用 `putIntL(v)`(低字节在前) → `buf.order(BIG_ENDIAN)` → `buf.getInt()` → 内部调用 `getIntB()` → 读到的值和写入值不同(除非对称)。

### 5. bulk get/put — 批量传输的效率跳板

场景: 单字节循环 `for(int i=0; i<4096; i++) buf.put(data[i])` 每次调用 JNI(JIT 也难全展开) ——用 `buf.put(data, 0, 4096)` 一次操作完成 4KB 传输。

源码路径: `X-Buffer.java.template:740` — `get(byte[] dst, int offset, int length)` 先 `checkBounds(offset, length, dst.length)` 边界验证 → 循环 `dst[offset++] = _get(i++)`。`X-Buffer.java.template:881` — `put(byte[] src, int offset, int length)` 对称。`put(ByteBuffer src)` 在 Heap 间用 `System.arraycopy(source.hb, ..., this.hb, ...)` 优化——不是逐字节拷贝。

关键设计: bulk get/put 仍然是相对操作(推进 position)——与 `get()/put()` 一致性。检查边界在循环外一次性完成, 减少了单字节操作的边界检查开销。

数据流: `buf.put(data, 4, 4096)` → checkBounds(4, 4096, data.length) → 循环 4096 次: `hb[ix(pos++)] = data[4+n]` → position += 4096。

### 6. mark / reset — 临时位置快照

场景: 读到某个位置不确定是否是这个消息, 想"偷看"后面的字节, 看完不对还能退回到原位置。

源码路径: `Buffer.java:380` — `mark()` 把当前 position 记到 mark。`Buffer.java:396` — `reset()` 把 mark 恢复到 position, 若 mark=-1 抛 `InvalidMarkException`。mark 初始值=-1, flip/clear 都会重置 mark=-1——不能跨模式复用。

关键设计: mark/reset 的 peek 模式是 NIO 唯一的"临时退后"机制——没有像栈一样的多层 undo, 只能保存一个标记。Netty ByteBuf 的 `markReaderIndex/markWriterIndex` 提供了独立标记, 但同样限制为单层。

数据流: `buf.mark()` → mark=pos → `buf.getInt()/buf.getLong()` 读取后 pos 推进 → `buf.reset()` → pos=mark(`reset()` 内部校验 mark≥0)。

### 核心悬念

**"ByteBuffer 的 slice/duplicate 共享数据但没有引用计数——派生视图的生命周期完全依赖 GC。hasArray/array 让同一份代码在 Heap/Direct 之间不兼容。equals 对 remaining 敏感导致 flip 后比较结果变化。Netty Ch4 ByteBuf 用 readerIndex/writerIndex 双指针 + CAS 引用计数 + retainedSlice 一次性解决了这三个问题——但付出的代价是什么？"**

→ 引出 Ch2 NIO Channel — 缓冲区是数据的容器, Channel 是数据的管道。`SocketChannel.read(ByteBuffer)` 和 `SocketChannel.write(ByteBuffer)` 让缓冲区的四字段状态机与 TCP 的流式传输对接——阻塞模式和非阻塞模式下同样的 `read()` 方法有完全不同的语义。
