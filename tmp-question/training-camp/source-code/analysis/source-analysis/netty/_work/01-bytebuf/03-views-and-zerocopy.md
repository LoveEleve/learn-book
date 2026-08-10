# ByteBuf 的视图与零拷贝 — 操作已有数据的三种武器

前两篇讲了 ByteBuf 的创建和内存管理。但在网络编程中，最常见的操作不是在空 buffer 里写数据——而是从已有的 ByteBuf 里提取一段、把几段拼在一起、然后按正确的字节序写进网络。这篇讲三种零拷贝操作：`slice`/`duplicate` 提取视图、`CompositeByteBuf` 拼接数据、以及字节序在读写中的角色。

一句话概括：**最好的拷贝是不拷贝**。前两篇用池化省掉了分配开销，这一篇用视图省掉了数据搬运。

---

## 提取：slice、duplicate、copy 的三重身份

HTTP 请求的经典场景：客户端发过来一个 HTTP 请求，第一个 Handler 读 method（4 字节 `"GET "`），第二个 Handler 读 URI，第三个 Handler 读剩下的 header。三个 Handler 都操作同一个 ByteBuf 的不同片段，但每个 Handler 只应该看到它关心的那一段。

ByteBuf 提供了三种方式从已有 buffer 中提取数据。核心问题不在"怎么提取"——而在**提取之后底层数据还有没有共享**。共享意味着零拷贝但生命周期相互绑定，独立意味着拷贝开销但生命周期独立。

### slice：窗口截取

`slice()` 和 `slice(int index, int length)`（`ByteBuf.java:2207, 2233`）返回一个"窗口"——它和原 ByteBuf 共享同一块底层内存，但有自己的 `readerIndex` 和 `writerIndex`，上限固定为窗口大小。

```
原 buf:  [0----15----30-----------100-----------256]
           \      \    
            \      \--- slice(15, 15)
             \--- slice(0, 30)
```

窗口内的字节修改会反映在原 buf 中——因为它们指向同一块内存。窗口的 `readerIndex` 从 0 开始、`writerIndex` 从窗口大小开始——第一个 Handler 可以直接调用 `buf.readBytes()` 读取它关心的那段。

关键约束：**slice 不增加原 buf 的引用计数**。这意味着调用方必须保证原 buf 在窗口使用期间不被释放。在 Pipeline 中这不是问题——原 buf 的生命周期由 Head 和 Tail Context 管理，Handler 只是短暂借用。但如果窗口被传递到另一个 EventLoop 线程——引用计数的问题就出现了。

另外，`slice()` 有一个设计意图不同的变体——`readSlice(int length)`（`ByteBuf.java:1595`）。它创建视图的同时**推进 readerIndex**——相当于"把这 N 个字节切出来，然后消费掉"。帧解码的典型模式是：

```java
int frameLength = buf.readInt();          // 读帧长（消费 4 字节）
ByteBuf frame = buf.readSlice(frameLength);  // 取帧体（消费 frameLength 字节）
```

对比 `slice(frameLength)`——读者需要手动 `readerIndex += frameLength`。`readSlice` 把"取一段 + 消费一段"合并为一步，减少了两行容易出错的样板代码。对应的 retained 版本是 `readRetainedSlice(int)`（`ByteBuf.java:1613`），语义和 `retainedSlice()` 一致——用于跨线程传递时自动 retain。

### retainedSlice：安全的跨线程传递

`retainedSlice()`（`ByteBuf.java:2221`）对原 buf 调用了一次 `retain()`，然后创建和 `slice()` 行为完全相同的窗口。语义等价于 `slice().retain()`，但有一个优化的细节——这个返回可能不是"先 slice 再 retain"的两次原子操作，而是一个合并的内存操作。

在 Pipeline 中，如果窗口需要被传递给另一个异步处理线程（比如线程池处理一个大请求的 body），必须用 `retainedSlice`——原 Handler 的线程可能会在请求完成后 `release` 原 buf，窗口必须持有自己的引用计数保证不被过早回收。

### duplicate：分身阅读

`duplicate()`（`ByteBuf.java:2261`）和 slice 的核心区别——它返回**完整的 buf 视图**，不只是子区间。

```
slice(15, 30):    只看到 index 15-45 的数据
duplicate():      看到全部 0-256 的数据（但 readerIndex/writerIndex 独立）
```

场景不同——`slice` 是"我只想读这一段"，`duplicate` 是"我想从不同位置读同一个 buf"。比如两个并发线程各自维护自己的 `readerIndex` 阅读同一个 HTTP 消息。

`retainedDuplicate()`（`ByteBuf.java:2275`）的意义和 `retainedSlice` 对称。

### copy：切割不是万能的

`copy()`（`ByteBuf.java:2186`）和 `copy(int index, int length)`（`ByteBuf.java:2194`）唯一地创建了真正独立的副本——**分配新的内存、拷贝数据、返回一个全新的 ByteBuf**（`AbstractByteBuf.copy()` → `alloc().buffer(length).writeBytes(this, index, length)`）。它的引用计数从 1 开始，和原 buf 没有任何共享关系。

但问题是——你真的需要拷贝吗？在 Netty 里，大部分数据只被读取一次。除非你需要修改副本而保持原始数据不变（比如解密、签名验证），否则 slice + retain 是更好的选择——省掉了一次分配和一次内存拷贝。

### 一图区别

| 操作 | 数据共享？ | 引用计数 | 独立 index？ | 适用场景 |
|------|:--:|:--:|:--:|------|
| `slice()` | 是 | 不增 | 是 | 同线程临时截取 |
| `retainedSlice()` | 是 | +1 | 是 | 跨线程传递窗口 |
| `duplicate()` | 是 | 不增 | 是 | 并发读同一 buf 不同位置 |
| `copy()` | **否** | 新 (refCnt=1) | 独立 | 需要修改的独立副本 |

但 slice 只能截一块。如果要组合——比如 HTTP 响应的 header 在 `buf1`，body 在 `buf2`，想拼成一个连续字节流发给客户端——一个 slice 不够用了。

---

## 聚合：CompositeByteBuf 的组件拼图

把两个 ByteBuf 拼成一个——最天真的写法是：

```java
ByteBuf combined = alloc.buffer(header.readableBytes() + body.readableBytes());
combined.writeBytes(header);
combined.writeBytes(body);
// 现在 combined 是连续的——但 header 和 body 都被拷贝了一次
```

对于 128 字节的 header 和 64KB 的 body，拷贝 64KB 可能还可以。但如果 body 是 1MB 的文件传输——这次拷贝就是不必要的。

`CompositeByteBuf`（`CompositeByteBuf.java:49`）用一种 Component 模型来做到零拷贝：它内部持有一个 `Component[]` 数组，每个 Component 记录了原始 ByteBuf 的引用、offset、长度——像数据库的索引。当你请求索引 N 的字节时，它找到正确的 Component，然后在 Component 内部索引本地位置。

### 组件模型

```java
// CompositeByteBuf.java:1913-1969 (简化)
Component {
    ByteBuf srcBuf;      // 原始 buf 的 retained 引用
    ByteBuf buf;         // srcBuf 的 slice 视图
    int offset;          // 在 CompositeByteBuf 中的起始偏移
    int endOffset;       // 加入时: offset + length
    int srcIdx;          // 原 srcBuf 中的起始偏移 (被包装时)
    int length;
}
```

将 header 和 body 加入 CompositeByteBuf 的过程（`CompositeByteBuf.java:280`），`addComponent0()` 做了三件事：对 `srcBuf` 调用 `retain()`、创建一个 slice 视图作为 `buf`、计算并插入 offset/endOffset——然后 CompositeByteBuf 内的后续字节偏移因为这个插入而自动前移。

读一个字节时——`CompositeByteBuf.java:1617`，`findComponent(offset)` 定位正确的 Component。内部有一个 `lastAccessed` 弱缓存——因为读操作通常从低地址到高地址顺序推进，缓存的最近一次访问大概率就是下一次想找的 Component。缓存未命中时退到二分查找——在有 10 个 Component 的情况下，O(log 10) = 找到正确的 Component 后进行本地索引读取。

### 读的代价与写的陷阱

读操作从 O(1)（直接 buffer）退化到 O(log N)（N 是组件数）。在典型的 HTTP 场景中——header + body = 2 个 Component，log2(2) = 1 次比较，几乎无感。但 100 个 Component 时——比如流式文件传输被分成了 100 个分片——每次读都需要最多 7 次比较。

写操作才是真正的陷阱。向 CompositeByteBuf 写入字节时，如果写入位置跨越了两个 Component 的连接处——底层代码必须把这个位置切开、分裂成两个 Component，写入新增的字节，然后把两个 Component 重新接在一起。大范围写入时系统会触发 `consolidate0()`（`CompositeByteBuf.java:1774`）——把所有 Component 合并成一个连续 buffer——**此时拷贝就不可避免了**。

这就是 CompositeByteBuf 的设计权衡：读取快、聚合快、但随机和大范围写入会触发代价。它最适合的场景是"聚合后只读"——HTTP 响应、复用协议的头部与荷载、管道中固定格式的日志记录。

### 默认上限

最多能组合多少个 ByteBuf？默认上限由分配器的 `DEFAULT_MAX_COMPONENTS` 控制——目前是 16 个（`AbstractByteBufAllocator.java:33`）。超过这个上限时，新的 `addComponent` 调用 `consolidateIfNeeded()`——它检查 `componentCount > maxNumComponents`，触发 `consolidate0()` 强制将 17 个 Component 合并成一个连续的 buffer（`CompositeByteBuf.java:566-572`）——从零拷贝变成全量拷贝。16 的上限是一个工程折中——在大多数 HTTP 场景中（header + body 就 2 个 Component），16 已经富余很多。对于需要组合成百上千个分片的场景，框架设计者可以传入更大的 `maxNumComponents` 构造参数，但读性能也会随之劣化（log2(16)=4 次 vs log2(1000)≈10 次比较）。

---

## 格式：ByteOrder — 谁决定字节的阴阳

字节序（Endianness）是多字节整数的排列规则——BigEndian 高位字节在前（人类读数的习惯），LittleEndian 低位字节在前（x86 CPU 的原生格式）。网络协议（TCP/IP、HTTP）用 BigEndian。如果你在 C 里写 socket 程序，`htonl()` 和 `ntohl()` 是必须调用的——因为 x86 是 LittleEndian。

在 Java 里，JVM 在字节码层面用 BigEndian——`DataOutputStream.writeInt()` 写的是 BigEndian。但 `java.nio.ByteBuffer.order()` 可以设置端序——默认 BigEndian 但 `order(LITTLE_ENDIAN)` 会改变后续 `getInt()` 的行为。

Netty 的 ByteBuf 在一开始沿用了这条路——`order()` 返回当前端序，`order(ByteOrder)` 设置端序并返回 `SwappedByteBuf` 包装器进行端点翻转。但在 **Netty 4.2** 中，这两个方法都被标记为 `@Deprecated`（`ByteBuf.java:284, 298`）：

```java
// ByteBuf.java:280-284
/**
 * @deprecated use the Little Endian accessors, e.g. {@code getShortLE}, {@code getIntLE}
 * instead of creating a buffer with swapped {@code endianness}.
 */
@Deprecated
public abstract ByteOrder order();
```

Javadoc 的建议很清楚：**不要翻转 buf 的端序，而是用专门的 LE 方法**：

```java
// ByteBuf.java:611
public abstract short getShortLE(int index);

// ByteBuf.java:701
public abstract int   getIntLE(int index);

// ByteBuf.java:745
public abstract long  getLongLE(int index);
```

这个设计变更的背后逻辑很简单——`order(LITTLE_ENDIAN)` 会返回 `SwappedByteBuf` 包装器，在每次 `getInt()` 时执行 `Integer.reverseBytes(value)` 的数学翻转。如果你的代码在读取时做了 `buf.order(LITTLE_ENDIAN)`、读取完后又做了 `buf.order(BIG_ENDIAN)`——每一次 `order()` 调用都是一次包装层的创建。在高频读写中这就变成多余的对象分配。

LE 后缀的方法绕过了包装层——它们直接从底层内存读取、进行字节翻转、返回结果。不需要修改 buf 的状态，不需要包装器。对于 x86 平台，Netty 可以在 Unsafe 层做端点转换，性能远优于 SwappedByteBuf 的包装。

**SwappedByteBuf 为什么被抛弃？** 它是 `order(LITTLE_ENDIAN)` 的产物——一个实现了 `ByteBuf` 全部方法的包装器（`SwappedByteBuf.java:38`），在每次读多字节值时执行字节翻转：

```java
// SwappedByteBuf.java:283-284
public int getInt(int index) {
    return ByteBufUtil.swapInt(buf.getInt(index));
}
```

它读大端序的原始值 → 翻转字节 → 变成小端序。性能代价是每读一次多一次 `swapInt`（`Integer.reverseBytes`），每读一次多一次委托。而且 `order()` 切换状态是全局的——切换到 LITTLE_ENDIAN 后，所有后续读取都被翻转，切换回 BIG_ENDIAN 又在底层再造一个 SwappedByteBuf。Netty 4.2 的选择是：**砍掉状态机，端序变成每个读操作自己的参数**。

**什么时候用哪个方法？**

| 场景 | 用什么 | 为什么 |
|------|------|------|
| HTTP/gRPC/TCP/IP 标准协议 | `writeInt(val)` / `getInt()` | 默认 BigEndian → 网络字节序，零翻转 |
| 嵌入式设备 LE 二进制协议 | `writeIntLE(val)` / `getIntLE()` | 指定 LE，调用方清楚自己在翻转 |
| Protobuf 变长编码 | Protobuf 层处理 | Varint 编码与字节序无关，ByteBuf 提供原始字节流即可 |
| 和 C 程序通信 | 看目标平台 | x86=LE, ARM=可配——用 `/proc/cpuinfo` 查确认 |

对于大多数网络协议（HTTP/gRPC/自定义二进制协议），默认的 BigEndian 写操作是正确的——`writeInt(contentLength)` 直接写网络字节序的整数。只有在对接 LittleEndian 协议（比如一些嵌入式设备的二进制格式或 Protobuf 的底层变长编码）时才需要 `getIntLE` / `writeIntLE`。

---

## 三者合一

现在可以完整描述一个典型的 I/O 数据处理路径：

```
网络层读取 → Direct ByteBuf (pooled, from thread cache)
  → HTTP 解析:
    → slice() 截取 header              ← 零拷贝
    → retainedSlice() 跨线程传 body   ← 安全的异步传递
    → CompositeByteBuf 拼 header+body  ← 零拷贝聚合
      → write请求:
        → 默认 BigEndian (网络字节序)  ← 协议标准，直接用 `writeInt`/`writeIntLE`
        → 发给 socket → [done]
```

这条路径上——从网络读到写到网络——全程没有一次额外的内存拷贝。数据只从网卡 DMA 到 Direct buffer，然后通过视图切片、Composite 聚合、最终 DMA 到网卡。中间的所有操作都在操作指针和引用计数，而不是移动数据。

这就是 Netty 放弃 ByteBuffer 的第四条也是最后一条原因：**ByteBuffer 的视图操作有隐式的共享陷阱**。`ByteBuffer.slice()` 和 `ByteBuffer.duplicate()` 共享底层数组，但它们共享的是**默认的 ByteOrder、mark 状态、和底层 bigEndian/littleEndian 属性**——你对 slice 的 `order()` 调用会影响原 buffer 的读取。这不是 bug——ByteBuffer 的设计就是 `order()` 修改底层共享状态。但在 Pipeline 里，一个 Handler 修改了端序，下游的所有 Handler 都会被感染——这正是 flip/compact 同构的问题：全局状态在一个局部操作中被污染。

而 ByteBuf 的 `slice()` 和 `duplicate()` 有独立的 `readerIndex`/`writerIndex`，`order()` 在 4.2 中被废弃——端序现在由每个读操作本身指定（`getInt()` vs `getIntLE()`），不再是一个可被修改的 buf 属性。**Netty 把 ByteBuffer 中所有"全局状态"都局部化了**：读位置（双指针替代单指针）、容量（自动扩容）、内存回收（引用计数替代 GC）、数据共享（视图操作独立 index）、端序（*LE 方法）。ByteBuf 不是 ByteBuffer 的"增强版"——它是对 ByteBuffer 中每一个全局状态变量进行的系统化局部化。

读完这篇，ByteBuf 的分析就结束了——从创建、扩容、引用计数、内存池化、视图操作到字节序。下一篇进入 EventLoop——这些 ByteBuf 是怎么在被读写的？谁来驱动读写循环？Netty 4.2 的 IoHandler 和 EventLoop 分离是什么架构？

---

## 源码路径记录

| 概念 | 源码位置 | 行号 |
|------|---------|:--:|
| copy() / copy(int, int) | `ByteBuf.java` | 2186, 2194 |
| slice() / slice(int, int) | `ByteBuf.java` | 2207, 2233 |
| readSlice(int) / readRetainedSlice(int) | `ByteBuf.java` | 1595, 1613 |
| retainedSlice() / retainedSlice(int, int) | `ByteBuf.java` | 2221, 2246 |
| duplicate() | `ByteBuf.java` | 2261 |
| retainedDuplicate() | `ByteBuf.java` | 2275 |
| CompositeByteBuf class | `CompositeByteBuf.java` | 49 |
| Component 内部类 (srcBuf/offset/endOffset) | `CompositeByteBuf.java` | 1913-1969 |
| addComponent0() — addComp + shiftComps | `CompositeByteBuf.java` | 280, 2358 |
| removeComponent() — comp.free() | `CompositeByteBuf.java` | 613 |
| findComponent(offset) — lastAccessed + 二分 | `CompositeByteBuf.java` | 1617 |
| maxNumComponents (allocator default = 16) | `AbstractByteBufAllocator.java` | 33 |
| consolidateIfNeeded() — size > maxNumComponents | `CompositeByteBuf.java` | 566-572 |
| consolidate0() — 超限合并 | `CompositeByteBuf.java` | 1774 |
| order() — @Deprecated (use LE methods) | `ByteBuf.java` | 284 |
| order(ByteOrder) — @Deprecated | `ByteBuf.java` | 298 |
| getShortLE(int index) | `ByteBuf.java` | 611 |
| getIntLE(int index) | `ByteBuf.java` | 701 |
| getLongLE(int index) | `ByteBuf.java` | 745 |
| SwappedByteBuf.getInt (ByteBufUtil.swapInt) | `SwappedByteBuf.java` | 283-284 |
