# ByteBuf 核心抽象 — 为什么重新发明缓冲区

在 NIO 上卷结束后，读者已经理解了 ByteBuffer 的完整设计。但一个悬而未决的问题是：如果 Netty 只是封装了 NIO，为什么还要重新设计一个缓冲区？直接用 `java.nio.ByteBuffer` 不行吗？

这一篇回答三个东西：双指针读写模型、自动扩容、引用计数生命周期。三者串联成一条完整的论证链——ByteBuffer 不是功能不够，而是在 Pipeline 模型中根本设计错了方向。

---

## 一个问题：ByteBuffer 走进了死胡同

想象一个最简的 Netty Pipeline：三个 Handler，第一个读 HTTP 头，第二个做路由判断，第三个把请求体传进业务逻辑。

用 NIO ByteBuffer 做数据载体，每个 Handler 的操作流程是：

```
Handler 1: ByteBuffer buf = ...;
           buf.flip();      // 切换到读模式
           读出 header → 路由判断
           buf.compact();   // 切回写模式
           传给 Handler 2

Handler 2: buf.flip();      // 切回读模式
           路由处理
           buf.compact();   // 切回写模式
           传给 Handler 3

Handler 3: buf.flip();
           读取请求体 → 业务逻辑
           buf.compact();
```

三个 Handler，三次 flip/compact。这不是"写的代码多一点"的问题——**flip/compact 是全局状态，不是局部操作**。Handler 1 做完 flip 之后，如果 Handler 2 直接去读而不 flip，它读到的区域是错的。如果 Handler 2 flip 了但 Handler 1 还持有引用，Handler 1 以为还能写——它也错了。

Netty 的设计者看到了这个根本矛盾：**Pipeline 要求每个 Handler 独立操作同一份数据，ByteBuffer 却要求全局协调读/写模式切换**。两者天生打架。

解决方案看起来很简单——能不能让读写不使用同一个游标？

---

## 双指针模型：两个游标，零切换

打开 `ByteBuf.java`，第 248 行的类声明已经很直白：

```java
// ByteBuf.java:248
public abstract class ByteBuf implements ReferenceCounted, Comparable<ByteBuf>, ByteBufConvertible {
```

它同时实现了 `ReferenceCounted`——这个后面会详细展开。

ByteBuf 用两个独立的指针变量替代了 ByteBuffer 的单指针：

```
      +-------------------+------------------+------------------+
      | discardable bytes |  readable bytes  |  writable bytes  |
      |                   |     (CONTENT)    |                  |
      +-------------------+------------------+------------------+
      |                   |                  |                  |
      0      <=      readerIndex   <=   writerIndex    <=    capacity
```

这段图直接摘自 ByteBuf 的 Javadoc（`ByteBuf.java:67-74`）。`readerIndex` 指向下一个可读字节，`writerIndex` 指向下一个可写位置。它们的关系是不等式：

```
0 <= readerIndex <= writerIndex <= capacity
```

三个区域各司其职：

- **discardable bytes**（0 ~ readerIndex）：已经读过的字节，按需回收
- **readable bytes**（readerIndex ~ writerIndex）：待读取的数据，也就是"内容"
- **writable bytes**（writerIndex ~ capacity）：可以写入的空间

回到 Pipeline 场景——三个 Handler 传同一个 ByteBuf：

```
Handler 1: readerIndex=0, writerIndex=100
           读 40 字节 → readerIndex 推进到 40
           传给 Handler 2

Handler 2: readerIndex=40, writerIndex=100
           读 30 字节 → readerIndex 推进到 70
           传给 Handler 3

Handler 3: readerIndex=70, writerIndex=100
           读剩余 30 字节 → readerIndex=100
           done
```

**每个 Handler 只动 readerIndex，不动 writerIndex**。没有 flip、没有 compact、没有任何状态切换。读就是读，读完指针自动推进；写就是写，写完 writerIndex 自动推进。两个指针各管各的。

对应的 API 也很自然。读取的语义从 ByteBuf 的 Javadoc 直接翻译出来就是真理：`ByteBuf.java:76-103`——

```java
// ByteBuf.java:326
public abstract int readerIndex();

// ByteBuf.java:336
public abstract ByteBuf readerIndex(int readerIndex);

// ByteBuf.java:410
public abstract int readableBytes();

// ByteBuf.java:438
public abstract boolean isReadable();
```

`readableBytes()` 直接就是 `writerIndex - readerIndex`。不是 ByteBuffer 里那种搞不清楚的 `remaining()` 语义（flip 之后是 `limit - position`，compact 之前是另一回事）——ByteBuf 的可读字节数是一个恒定公式，和操作顺序无关。

同样，写的 API：

```java
// ByteBuf.java:341
public abstract int writerIndex();

// ByteBuf.java:351
public abstract ByteBuf writerIndex(int writerIndex);

// ByteBuf.java:416
public abstract int writableBytes();

// ByteBuf.java:450
public abstract boolean isWritable();
```

注意 `writableBytes()`——在 ByteBuffer 里要问"还能写多少"是绕弯子的（不 flip 时是 `limit - position`，写了之后又要区分模式——因为只能往 position 处写，不能往前面的 limit 处写）。在 ByteBuf 里它永远是 `capacity - writerIndex`。

还有一个重要的操作——`discardReadBytes()`（`ByteBuf.java:513`）。它的语义和 ByteBuffer 的 `compact()` 完全不同：

```
调用前:
   0         rI=30        wI=100    cap=256
   |--已读--|----可读----|----可写----|

调用 discardReadBytes() 后:
   0         rI=0         wI=70     cap=256
   |----可读----|--------可写--------|
```

它把可读字节移到开头，writerIndex 相应减少。但注意——**这不是 compact。compact 会把已读的扔掉、未读的移到开头，然后你又回到了写模式**。discardReadBytes 只是回收空间，readerIndex 归零，writerIndex 变成原来的 `writerIndex - oldReaderIndex`。

为什么不用 compact？因为在 Pipeline 里，下一个 Handler 需要知道"这里有一些数据要读"，而不是"可以继续往 buffer 里写了"。readerIndex 本身就是信号。

而 `clear()`（`ByteBuf.java:467`）更简单——`readerIndex = writerIndex = 0`。和 ByteBuffer.clear() 一样把两个指针归零，但**不附带"接下来必须 flip"的隐式契约**。ByteBuf.clear() 之后就可以直接读或写，行为由下一次操作自然决定。

除了 readerIndex 和 writerIndex 之外，ByteBuf 内部还藏着第三个隐式指针——`markedReaderIndex`（`AbstractByteBuf.java:73`）。它支撑了帧解析中最重要的"偷看一眼"模式：

```java
buf.markReaderIndex();                // 记录当前位置
if (buf.readableBytes() < 4) {
    buf.resetReaderIndex();           // 回退——数据不够，等下次
    return;
}
int frameLength = buf.readInt();     // 消费帧长度
```

`markReaderIndex()` 把当前 readerIndex 存到 markedReaderIndex（`AbstractByteBuf.java:192-193`），`resetReaderIndex()` 把 readerIndex 恢复到记录的位置（`AbstractByteBuf.java:198-199`）。这对方法让一个 Handler 可以试探性地读几个字节——如果发现数据不足，回退退出；如果够了，直接推进 readerIndex 消费数据。在基于帧的协议解析中（HTTP Chunk、WebSocket Frame、自定义 LengthFieldBasedFrame），mark/reset 是每次解码的标准前置动作。不需要调用方维护"我读到哪里了"的备份——ByteBuf 在内部替你记录了。

到这里，第一个设计问题已经有了答案：**双指针让 Pipeline 的每个 Handler 可以独立操作同一份数据，不需要在任何两个 Handler 之间做状态切换协议**。一个 Handler 触到 readerIndex 等于 writerIndex 时，`isReadable()` 返回 false，它就知道数据读完了——不需要任何一方告诉它"现在是读模式"。

但还有一个问题——

---

## 自动扩容：不再预设容量

ByteBuffer 的第二个致命缺陷是固定容量。`ByteBuffer.allocate(1024)` 之后，capacity 永远是 1024。如果 Handler 写入时发现可写空间不够——`remaining()` 返回 0——你只能在应用层手动分配新的、更大的 ByteBuffer，把数据拷过去，丢掉旧的。

ByteBuf 内置了这个机制。不需要思考"能不能装下"，只需要调用 `ensureWritable(minWritableBytes)`（`ByteBuf.java:535`）：

```java
buf.ensureWritable(256);   // 保证至少有 256 字节可写空间
// 如果当前 writableBytes() < 256，自动扩容
```

扩容的具体策略在 `AbstractByteBufAllocator.calculateNewCapacity()`（`AbstractByteBufAllocator.java:232-259`）。核心逻辑只有 20 行，分三条路径：

```java
// AbstractByteBufAllocator.java:239
final int threshold = CALCULATE_THRESHOLD; // 4 MiB page

// 路径1: 恰好等于 4M
if (minNewCapacity == threshold) {
    return threshold;
}

// 路径2: 超过 4M — 线性增长，每次 +4MB
if (minNewCapacity > threshold) {
    int newCapacity = minNewCapacity / threshold * threshold;
    if (newCapacity > maxCapacity - threshold) {
        newCapacity = maxCapacity;
    } else {
        newCapacity += threshold;
    }
    return newCapacity;
}

// 路径3: 低于 4M — 指数翻倍，找最近的 2 的幂
final int newCapacity = MathUtil.findNextPositivePowerOfTwo(
    Math.max(minNewCapacity, 64)
);
return Math.min(newCapacity, maxCapacity);
```

`CALCULATE_THRESHOLD = 1048576 * 4`，也就是 4 MiB（`AbstractByteBufAllocator.java:34`）。这个数字不是拍脑袋的——PoolChunk 的默认大小就是 4MB。池化分配器按 4MB 的 chunk 向操作系统申请内存，扩容阈值和池化单元对齐，扩容出的 buffer 正好能被池化回收。

为什么分成两条策略？

**低于 4MB**（最常见的情况）——指数翻倍。比如一个 256 字节的 buffer 需要写 300 字节 → `findNextPositivePowerOfTwo(300)` = 512。下一次是 512 → 1024 → 2048 → 4096。每次翻倍，扩容次数是 O(log N)。最坏情况：从 64 扩容到 4MB，只需要 16 次扩容——每次都是 `System.arraycopy`，代价可预测。

**超过 4MB**（文件传输、大消息）——线性步进，每次 +4MB。想象一个 5MB 的 buffer 需要扩容到 100MB：如果用指数翻倍，5M→10M→20M→40M→80M→160M，最后一步从 80M 到 160M 会分配一个 80MB 的临时区域，内存压力巨大。线性增长的话：5M→8M→12M→16M→...→100M，每一步占用只比前一步多 4MB，全程内存足迹可控。

而且还有一个聪明的细节——**最小容量是 64 字节**。`Math.max(minNewCapacity, 64)` 保证了即使 `ensureWritable(1)` 也不会扩容到一个奇奇怪怪的 1 字节或 2 字节的 buffer。Netty 的设计哲学是：如果你需要用 buffer，最少也得是 64 字节——低于这个阈值不值得走一次扩容。

扩容触发后，内部发生的事情是可预测的：对于 Heap buffer，旧数据通过 `System.arraycopy` 拷贝到新的 `byte[]`，旧数组由 GC 回收；对于 Direct buffer，则是分配新的 Direct 缓冲区 → `ByteBuffer.put()` 将旧数据拷贝过来 → `setByteBuffer(newBuffer, true)` 释放旧的堆外内存（`UnpooledDirectByteBuf.java:197-201`）。那旧的 native memory 呢？

这正是第三个设计问题的起点。

---

## 引用计数：堆外内存的确定性回收

ByteBuffer 的堆外内存回收是 Java NIO 的一个历史包袱。`DirectByteBuffer.allocateDirect()` 分配时创建了一块堆外内存，同时注册了一个 `Cleaner` 虚引用。当 Java 对象被 GC 回收时，`Cleaner` 触发，释放堆外内存。

问题藏在"当 Java 对象被 GC 回收时"这半句。GC 的触发时机是不可预测的——可能下一秒，可能五分钟后。在这段时间里，一块已不用的堆外内存仍然占着物理内存。当框架以每秒数万次的速度分配和丢弃 Direct buffer 时，GC 的滞后足以把 JVM 的堆外内存撑爆。

更糟的是，这诱使开发者写出"防御性 alloc"的代码——每次都觉得"也许 copy 一份更安全"，结果是堆外内存的分配速度远远超过 GC 释放速度。

Netty 的解决方案是一个精确到指令级的替代品：`ReferenceCounted` 接口（`ReferenceCounted.java:32`）。

```java
// ReferenceCounted.java:32
public interface ReferenceCounted {
    int refCnt();                      // 当前引用计数
    ReferenceCounted retain();         // refCnt += 1
    ReferenceCounted retain(int increment);
    ReferenceCounted touch();          // 记录访问位置（泄漏追踪用）
    ReferenceCounted touch(Object hint);
    boolean release();                 // refCnt -= 1，到 0 时释放
    boolean release(int decrement);
}
```

语义非常直白：每当你把 ByteBuf 传递给下一个持有者，调用 `retain()`；每个持有者用完调用 `release()`。谁把计数降到 0，谁就负责释放。

实现位于 `AbstractReferenceCountedByteBuf`（`AbstractReferenceCountedByteBuf.java:24-101`），这是一个不到 80 行的文件，核心逻辑极其精简：

```java
// AbstractReferenceCountedByteBuf.java:27
private final RefCnt refCnt = new RefCnt();

// AbstractReferenceCountedByteBuf.java:60-63
@Override
public ByteBuf retain() {
    RefCnt.retain(refCnt);
    return this;
}

// AbstractReferenceCountedByteBuf.java:82-84
@Override
public boolean release() {
    return handleRelease(RefCnt.release(refCnt));
}

// AbstractReferenceCountedByteBuf.java:91-96
private boolean handleRelease(boolean result) {
    if (result) {
        deallocate();
    }
    return result;
}
```

`RefCnt` 根据 JVM 能力自适应选择最有效的原子更新方式：优先 Unsafe，其次 VarHandle，最后降级到 `AtomicIntegerFieldUpdater`（`RefCnt.java:34-46`）。内部用一个 `volatile int` 存储引用计数——偶数值表示真实引用数（`value >>> 1`），奇数值表示已释放（`RefCnt.java:52-58`）。`release()` 返回 true 意味着计数降到了 0——此时 `handleRelease` 调用 `deallocate()`。这是一个 abstract 方法，由子类实现：

- `UnpooledHeapByteBuf.deallocate()` — 把内部 `byte[]` 引用置 null，依赖 GC
- `UnpooledDirectByteBuf.deallocate()` — 直接调用 `PlatformDependent.freeDirectBuffer()` 释放 native memory
- `PooledByteBuf.deallocate()` — 把 buffer 归还到池中复用

关键的设计决策是 `isAccessible()`（`AbstractReferenceCountedByteBuf.java:34-37`）：

```java
@Override
boolean isAccessible() {
    // Try to do non-volatile read for performance as the ensureAccessible()
    // is racy anyway and only provide a best-effort guard.
    return RefCnt.isLiveNonVolatile(refCnt);
}
```

注释中的 "best-effort guard" 暴露了一个深思熟虑的权衡：用非 volatile 读代替 volatile 读，换取吞吐量。如果引用计数已经降到 0，标准路径上 `ensureAccessible()` 会抛出异常——但这发生在每次读写操作之前，频率极高。用非 volatile 读牺牲了一点理论正确性（在多线程竞争 refCnt 降到 0 的极端边界情况下），换来了每次读写操作节省一次内存屏障。在 Netty 的 Pipeline 中——ByteBuf 由一个 EventLoop 的单线程处理，不存在这种竞态——这个权衡几乎总是赢的。

`retain()/release()` 听起来是给开发者增加负担——得记得成对调用，忘一次就是泄漏。但 Netty 的 Pipeline 已经做了自动处理。Inbound 消息从 Head 向 Tail 流动——如果你的 Handler 没有主动释放消息，消息最终到达 **TailContext**。这里的 `onUnhandledInboundMessage()` 会自动调用 `ReferenceCountUtil.release(msg)` 兜底（`DefaultChannelPipeline.java:1207`）。对于 Outbound 方向，写操作从 Tail 向 Head 反向传播——到达 **HeadContext** 后，`unsafe.write(msg, promise)` 负责写出，release 时机由 promise 的生命周期管理（`DefaultChannelPipeline.java:1385-1386`）。对大部分 Pipeline Handler 来说，**你不需要手动调用 release——默认情况下框架帮你兜底。**只有在需要把 ByteBuf 传递给跨越 EventLoop 的异步场景（比如把数据发到远端，引用需要跨线程持有），才需要显式 `retain()` 和 `release()`。

---

## 三者合一

现在可以完整回答篇头的核心问题：**为什么 Netty 重新发明了缓冲区？**

ByteBuffer 的三个致命设计约束：

1. **flip/compact 状态机** —— 要求全局协调读/写模式切换。Pipeline 模型中的每个 Handler 应该独立操作数据，两者根本矛盾。
2. **固定容量** —— 每次 "写不下了" 都要在应用层手工扩容、拷贝、替换引用。Netty 作为框架不能要求用户做这些。
3. **GC 驱动的 Direct 内存回收** —— 不确定的释放时机意味着堆外内存消耗不可预测。一个每秒处理数万请求的框架用 GC 回收 Direct 内存，就像靠楼管阿姨每个月来收一次垃圾。

ByteBuf 的三个回答：

1. **双指针**（`ByteBuf.java:62-74`）—— `readerIndex` 和 `writerIndex` 各自独立推进。读的人只管向前读，写的人只管向前写。没有模式、没有 flip、没有 compact。readerIndex 等于 writerIndex 时 `isReadable()` 返回 false，自然终结。不需要任何人声明"现在是读模式"。
2. **自动扩容**（`AbstractByteBufAllocator.java:232-259`）—— `<4MB` 指数翻倍（16 次从 64 到 4MB），`>=4MB` 线性步进（每次 +4MB），最小 64 字节。不是简单的"每次翻倍"——两条策略在避免过度分配和节省拷贝次数之间找到了分界点。
3. **引用计数**（`ReferenceCounted.java:32`，`AbstractReferenceCountedByteBuf.java:60-96`）—— `retain()/release()` 提供指令级的生命周期控制。最后一个 `release()` 触发 `deallocate()` ，堆外内存在不需要的瞬间立即 free。不确定的 GC 时机变成了确定的计数为 0 时刻。

这三个不是独立的设计。它们是一个闭环：**双指针保证了 Pipeline 的零切换吞吐 → 扩容保证了 pipeline 不会因为空间不够而中断 → 引用计数保证了扩出来的 Direct 内存能被准确回收**。少一个，整个模型就不自洽。

读到这里，后续的问题自然产生：

- "引用计数和 GC 到底各自管什么？Direct 内存真的完全绕过 GC 了吗？" — 这是 Heap 和 Direct 的内存布局差异，下一篇回答。
- "每次 ensureWritable 都触发 System.arraycopy——怎么避免？" — 这是池化分配器（PooledByteBufAllocator）的核心价值，下下一篇回答。

**下一步**：ByteBuf 的内存管理 — Heap vs Direct 的物理差异，以及 Netty 如何用池化分配代替频繁的 malloc/free。

---

## 源码路径记录

| 概念 | 源码位置 | 行号 |
|------|---------|:--:|
| ByteBuf 双指针模型 (Javadoc) | `ByteBuf.java` | 62-74 |
| ByteBuf 类声明 (implements ReferenceCounted) | `ByteBuf.java` | 248 |
| readerIndex() / readerIndex(int) | `ByteBuf.java` | 326, 336 |
| writerIndex() / writerIndex(int) | `ByteBuf.java` | 341, 351 |
| readableBytes() / writableBytes() | `ByteBuf.java` | 410, 416 |
| isReadable() / isWritable() | `ByteBuf.java` | 438, 450 |
| clear() (readerIndex=writerIndex=0) | `ByteBuf.java` | 467 |
| discardReadBytes() | `ByteBuf.java` | 513 |\n| markedReaderIndex field | `AbstractByteBuf.java` | 73 |\n| markReaderIndex() / resetReaderIndex() | `AbstractByteBuf.java` | 192-199 |
| ensureWritable(int) | `ByteBuf.java` | 535 |
| CALCULATE_THRESHOLD = 4 MiB | `AbstractByteBufAllocator.java` | 34 |
| calculateNewCapacity() | `AbstractByteBufAllocator.java` | 232-259 |
| ReferenceCounted API | `ReferenceCounted.java` | 32 |
| refCnt field (Unsafe/VarHandle/AtomicUpdater 自适应) | `AbstractReferenceCountedByteBuf.java` | 27 |
| RefCnt 三级降级策略 | `RefCnt.java` | 34-46 |
| RefCnt 偶数/奇数编码 (value>>>1) | `RefCnt.java` | 52-58 |
| isAccessible() (non-volatile read) | `AbstractReferenceCountedByteBuf.java` | 34-37 |
| retain() / release() | `AbstractReferenceCountedByteBuf.java` | 60-63, 82-84 |
| handleRelease() → deallocate() | `AbstractReferenceCountedByteBuf.java` | 91-96 |
| deallocate() abstract | `AbstractReferenceCountedByteBuf.java` | 101 |
| TailContext 兜底释放 (Inbound) | `DefaultChannelPipeline.java` | 1207 |
| HeadContext.write (Outbound → unsafe) | `DefaultChannelPipeline.java` | 1385-1386 |
| UnpooledDirectByteBuf 扩容 (alloc→put→free) | `UnpooledDirectByteBuf.java` | 197-201 |
