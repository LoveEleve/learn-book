# Ch1 分配与内存 — Heap vs Direct 的分叉点

> Cluster B: 5 KPs | 依赖 §1.1 | §1.1 → §1.2

### 1. allocate — JVM 堆内的极速分配

场景: 大多数业务代码不需要关心内存在堆内还是堆外——你只想创建一个能读写的缓冲区。`ByteBuffer.allocate(1024)` 是最简单的入口。

源码路径: `X-Buffer.java.template:345` — `allocate(int)` 调用 `new HeapByteBuffer(capacity, capacity)` 创建堆缓冲区。构造链: `HeapByteBuffer(int cap, int lim)` → `super(-1, 0, lim, cap, new byte[cap], 0)` → `Buffer(int mark, int pos, int lim, int cap, Object segment)`。底层 `byte[]` 在 JVM TLAB(Thread-Local Allocation Buffer) 中分配——和普通 `new byte[N]` 一样快。`Heap-X-Buffer.java.template:281-287` 的 `_get(i)` 和 `_put(i,b)` 就是 `hb[i]` 和 `hb[i]=b`——纯数组索引。

关键设计: HeapByteBuffer 的 `_get`/`_put` 是 package-private 方法——HotSpot 在热路径上可以内联到调用处, 消除方法调用开销。`ix()` 方法计算 `index + offset`——offset 是分段(segment)的基地址偏移, 用于安全沙箱(现已废弃但代码保留)。

数据流: `ByteBuffer.allocate(N)` → `new HeapByteBuffer(N)` → `hb = new byte[N]`(TLAB) → position=0, limit=capacity → 调用方 `buf.put(data)` 写入 hb 数组。

### 2. allocateDirect — 堆外内存 + JNI 通道

场景: Socket I/O 使用 HeapByteBuffer 时, JVM 需要把数据从堆数组拷贝到临时直接缓冲区再传给操作系统(`JNI GetByteArrayRegion`)。如果缓冲区本身就住在堆外, JNI 直接用指针传递——零拷贝。

源码路径: `X-Buffer.java.template:316` — `allocateDirect(int)` → `Bits.reserveMemory(size, cap)` 先检查总池 → `new DirectByteBuffer(capacity)`。`Direct-X-Buffer.java.template:9-23` — `_get(i)` = `UNSAFE.getByte(address + i)`, `_put(i,b)` = `UNSAFE.putByte(address + i, b)` ——不再经过 JNI 方法调用, 直接内存地址访问。

关键设计: DirectByteBuffer 内部持有一个 `long address` 字段指向堆外内存的起始地址。`_get`/`_put` 用 `address + i` 计算物理地址再 Unsafe 读写——这条路径完全绕开了 Java 数组的边界检查(`hb[ix(index)]` 内部有隐式检查)。代价: `UNSAFE.allocateMemory` 是 native 方法→系统调用 `malloc`, 比 TLAB 慢数百倍。分配慢但 I/O 零拷贝——这是"分配-使用"的频率权衡。

数据流: `allocateDirect(N)` → `Bits.reserveMemory(N)` 检查总池 → `UNSAFE.allocateMemory(N)` 分配堆外→ `new DirectByteBuffer(address, cap)` → I/O 线程 `channel.write(buf)` → JNI 通过 `GetDirectBufferAddress(buf)` 拿到 `address` → 直接调用 `write(fd, address, len)`。

### 3. Deallocator + Cleaner — 堆外内存不能靠 GC

场景: JVM 的 GC 管理堆内存——对象不再被引用时自动回收。但 `UNSAFE.allocateMemory` 分配的堆外内存 GC 完全不知道——`DirectByteBuffer` 对象被回收时, JVM 不会自动调用 `UNSAFE.freeMemory(address)`。

源码路径: `Direct-X-Buffer.java.template:69-90` — `Deallocator` 内部类封装 `address + size + capacity`。`Deallocator.run()` → `UNSAFE.freeMemory(address)` + `address = 0`(防 double-free)。`Direct-X-Buffer.java.template:96` — `Cleaner` 是 `PhantomReference` 的子类——`DirectByteBuffer` 被 GC 回收后, PhantomReference 进入 ReferenceQueue → 后台线程调用 `Deallocator.run()` 释放堆外内存。

关键设计: Cleaner 是"被动回收"——GC 什么时候回收 `DirectByteBuffer` 对象是不确定的。如果 JVM 堆压力不大但堆外内存已用满(`-XX:MaxDirectMemorySize`), 堆不触发 GC → Cleaner 不执行 → 堆外内存泄漏。这就是 Netty 为什么要用引用计数替代 Cleaner——引用计数归零时立即 `UNSAFE.freeMemory`, 不依赖 GC 的调度(§4.1 ByteBuf 引用计数)。

数据流: DirectByteBuffer 创建 → Cleaner 注册 PhantomReference → GC 回收 DirectByteBuffer → PhantomReference 入队 → Cleaner 线程调用 `Deallocator.run()` → `UNSAFE.freeMemory(address)` → address 置 0 防重复。

### 4. Bits.reserveMemory — 堆外内存的总池限流

场景: JVM 堆可以用 `-Xmx` 限制, 堆外内存也需要一个上限——否则 100 个连接各创建一个 1MB 的 DirectByteBuffer 就能撑爆物理内存。

源码路径: `Bits.java:1-175` — `reserveMemory(long size, int cap)`: 原子地累加 `totalCapacity` → 检查 `totalCapacity + size > MaxDirectMemorySize` → 超限时: 先 `System.gc()` 触发 GC(尝试让 Cleaner 释放已死对象的堆外内存) → 指数退避 `Thread.sleep(1,2,4,8...256ms)` 等待 GC 完成 → 最多 9 次重试。仍失败 → `OutOfMemoryError("Direct buffer memory")`。`MaxDirectMemorySize` 默认等于 `-Xmx` 值。

关键设计: `System.gc()` 不能保证 GC 立即完成——所以用指数退避等待 Cleaner 释放内存。这个机制在 GC 压力大时效率很低(G1 的 Mixed GC 可能跳过 Full GC)。Netty 的 `PooledByteBufAllocator` 通过复用已分配的 Chunk 绕过重复分配→不频繁触发 Bits 检查。

数据流: `allocateDirect(N)` → `tryReserveMemory(N)` → totalCapacity 超限 → `System.gc()` → sleep(1,2,4...) 重试 → 重试成功返回 → 失败 9 次抛 OOM。

### 5. wrap — 零拷贝包裹现有数组

场景: 你已经有一个 `byte[]`——不想再分配一个缓冲区, 只想要一个"视图"来读写这个数组的某个区段。

源码路径: `X-Buffer.java.template:389,421` — `wrap(byte[] array, int offset, int length)` 直接 `new HeapByteBuffer(array, offset, length)` → position=offset, limit=offset+length, capacity=array.length。不分配新数组, buffer 的 hb 字段引用原始数组。

关键设计: wrap 的数组和 Buffer 共享底层内存——`buf.put(0, (byte)1)` 会修改原始数组的第 0 个字节。这不是 bug——是"零拷贝"的设计代价。`wrap(array, 0, array.length)` 等价于 `wrap(array)`——全数组包裹。对比 `ByteBuf` 的 `wrappedBuffer`, Netty 版本支持更复杂的路由(heap/direct/composite)。

数据流: 原始 `byte[] data` → `wrap(data, 4, 8)` → 新 HeapByteBuffer, hb=data, pos=4, lim=12 → `buf.get()` 读取 data[4] → 修改 buf → 原始数组 data 同步被修改。

### 核心悬念

**"DirectByteBuffer 的 Cleaner 被动回收不可预测——堆外内存用满了但 GC 不触发怎么办？Netty Ch4 ByteBuf 用 CAS 引用计数(retain/release)替代 Cleaner——每次 release 立即检查 refCnt==0 时调用 deallocate(). 但引用计数又引入了新的问题: 谁来保证每一次 retain 都有对应的 release？"**

→ 引出 §1.3 视图与陷阱 — HeapByteBuffer(allocator+wrap) 和 DirectByteBuffer(Cleaner+Bits) 各有利弊。当 buffer 被 slice/duplicate/asReadOnlyBuffer 切分后, 多个视图共享同一块内存——修改一个视图会影响所有其他视图。API 陷阱(equals/compareTo 只在 remaining 范围比较)让 bug 更难追踪。
