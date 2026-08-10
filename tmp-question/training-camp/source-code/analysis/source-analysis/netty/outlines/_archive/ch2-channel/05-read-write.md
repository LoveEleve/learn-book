# §2.2 Channel 的 read/write 语义 — 文章大纲

## 读者基线

已从 §2.1 理解 Channel 的阻塞/非阻塞双模式和 `configureBlocking()`。现在需要理解 `read()` 和 `write()` 的**返回值语义**——这是编写正确的 NIO 代码的基础。

## Pass 0: 设计上下文

- `IOStatus` 定义 6 个返回码常量：EOF(-1)、UNAVAILABLE(-2, 无数据)、INTERRUPTED(-3)、UNSUPPORTED(-4)、THROWN(-5)、UNSUPPORTED_CASE(-6)
- `IOStatus.normalize(n)` 将 `UNAVAILABLE(-2)` 翻转为 `0`——这是"非阻塞无数据返回 0"的根源
- `readLock` 和 `writeLock` 是**两把独立的锁**——读和写可以并发，但两个读不能并发

## Pass 1: 扫描结果

- 核心方法: `read(ByteBuffer)` / `read(ByteBuffer[])` (scatter read) / `write(ByteBuffer)` / `write(ByteBuffer[])` (gather write)
- 返回码层: `IOStatus.java:36-41` 六个常量 + `normalize()` 翻转语义
- 锁模型: `SocketChannelImpl.java:76 readLock` + `SocketChannelImpl.java:79 writeLock` — 读写分离
- 标记问题 ≥5:
  1. 非阻塞 `read()` 返回 0 时，到底是"没数据"还是"连接已关闭"？如何区分？
  2. `write()` 返回的字节数小于请求——谁负责写剩下的？用户代码还是 JDK？
  3. scatter read/gather write 的多 ByteBuffer 语义是什么？和单 Buffer 有什么区别？
  4. `readLock` 和 `writeLock` 是分开的——意味着什么？什么操作会同时拿两把锁？
  5. `IOStatus.normalize()` 只翻转 `UNAVAILABLE`→0，为什么不直接在内核返回 0？

## 概念依赖链

```
§2.1 的双模式 → read() 返回值的三种语义 → write() 部分写的处理 → scatter/gather → 读写锁分离 → ByteBuffer 的协作角色
```

## 叙事顺序

**开篇场景：非阻塞 read 返回 0 是什么意思？**

从 §2.1 最后一行接过来——"非阻塞模式 read 返回 0 时你得轮询"。`read()` 的返回值有三种——不是两种、不是"0 代表两种情况"：

```java
int n = channel.read(buf);

// n = 0  — 非阻塞模式，socket 接收缓冲区为空，正常。可以等会再试
// n = -1 — 对端关闭了连接（EOF），不会再收到数据
// n = N  — 读到了 N 个字节，position 推进 N 步
```

这三种返回值背后是 NIO 的返回码层和归一化层的协作——先理解这个，再看 `write()`。

**第一层：read() 的三种返回值**

`IOStatus.java:36-41` 定义了 NIO 的内核返回码层：

| 内核返回值 | IOStatus 常量 | normalize 后 | 含义 |
|------|:--:|:--:|------|
| `read(fd)` → 无数据 (errno=EAGAIN) | `UNAVAILABLE = -2` (C native 层映射) | **0** | 非阻塞无数据，正常 |
| `read(fd)` → EOF | `EOF = -1` (C native 层映射) | **-1** | 对端关闭连接 |
| `read(fd)` → N 字节 | N (正数) | **N** | 读到了 N 字节 |
| `read(fd)` → EINTR | `INTERRUPTED = -3` (C native 层映射) | — | 系统调用被中断（do-while 重试） |

`IOStatus.normalize(n)` 只做一件事（`IOStatus.java:59-62`）：**把 `UNAVAILABLE(-2)` 翻转为 `0`**。这就是为什么你在 Java 中看到 `read()` 返回 0——它对应的是内核的 EAGAIN/EWOULDBLOCK。所有其他负数（EOF/-1、错误/-5、不支持的 case/-6）原样传递。

源码基础——`SocketChannelImpl.java:336-366` 的 `read()` 全链路分 8 步：`readLock` 加锁 → `ensureOpenAndConnected` → `beginRead` 线程注册 → native `IOUtil.read` → 中断的 `do-while` 重试 → `endRead` 线程注销 → `IOStatus.EOF` 检查 → `normalize` 归一化。不展示完整方法体——在文章中用这 8 步逐段讲解。

**第二层：write() 的部分写**

和 `read()` 不同，`write()` 有一个 `read()` 没有的陷阱：**部分写**。

```java
ByteBuffer buf = ByteBuffer.allocate(8192);
// ... 填满 8192 字节 ...
int n = channel.write(buf);  // n 可能是 4096, 不是 8192
```

TCP 的 socket 发送缓冲区（`SO_SNDBUF`）有大小限制。如果一次 `write()` 请求超过缓冲区剩余空间，内核写满缓冲区就返回——已经在缓冲区外的数据**没被写进去**。

JDK 的 `SocketChannel.write()` **不替你处理部分写**（`SocketChannelImpl.java:448-472`）。它只调一次 `IOUtil.write(fd, buf, -1, nd)`，返回实际写入的字节数。和 Java `OutputStream.write()` 不同——后者的 `write(byte[])` 内部循环直到全部写完。

**第三层：scatter read / gather write**

当你有多个 ByteBuffer（比如 HTTP header 一个 buffer、body 另一个 buffer），JDK 提供了 scatter/gather 变体：

```java
// scatter read: 一个系统调用读进多个 buffer
ByteBuffer[] buffers = { headerBuf, bodyBuf };
long n = channel.read(buffers, 0, buffers.length);

// gather write: 一个系统调用从多个 buffer 写出
channel.write(buffers, 0, buffers.length);
```

native 层语义等价于 POSIX `readv(fd, iovec, ...)` 和 `writev(fd, iovec, ...)` 系统调用（`IOUtil.read/write` 的 ByteBuffer[] 重载 → native 实现）。关键语义：**按顺序填满**——先填 `headerBuf`，填满了再填 `bodyBuf`。如果 `headerBuf` 只填了一半就返回，`bodyBuf` 完全不会被碰。

**第四层：readLock ≠ writeLock — 为什么是两把锁**

`SocketChannelImpl.java:76-79` 声明了两把独立的 `ReentrantLock`：

```java
private final ReentrantLock readLock = new ReentrantLock();
private final ReentrantLock writeLock = new ReentrantLock();
```

这意味着什么？
- 两个线程**不能**同时 `read()`——第二个会被 `readLock` 阻塞
- 两个线程**不能**同时 `write()`——同理
- 但一个线程 `read()` **和**另一个线程 `write()` **可以并发进行**

这和 `java.net.Socket` 的全双工模型一致——TCP 本身就是全双工的。但 `close()` 需要**同时拿两把锁**（`readLock + writeLock`），因为关闭操作必须等所有 I/O 操作完成。`SocketChannelImpl.java:537-549` 中 `setOption` 等操作也需要同时持有 read+write lock。

**结尾悬念**

你知道了 `read()` 返回 0 的含义、`write()` 可能部分写、scatter/gather 的语义。现在只剩最后一步——把第 1 章学的 ByteBuffer 和这些 receive/send 操作**组装成一个完整的收发循环**。下一节。

## 核心悬念

**读完能回答**：`read()` 返回 0 和返回 -1 有什么区别——哪个是正常、哪个是异常？`write()` 为什么可能返回比请求少的字节，谁负责处理剩余数据？scatter read 的填满顺序是什么？为什么 `readLock` 和 `writeLock` 是两把独立的锁？

## 文章边界

- **在本篇内**: read 三种返回值 → write 部分写 → scatter/gather → 读写锁分离
- **不属于本篇**: Selector（§3.1）、ByteBuffer 与 Channel 的完整缓冲循环（§2.3）
