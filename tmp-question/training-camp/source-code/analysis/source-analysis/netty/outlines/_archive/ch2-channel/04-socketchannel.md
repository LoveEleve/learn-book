# §2.1 SocketChannel 和 ServerSocketChannel — 文章大纲

## 读者基线

已从第1章掌握 ByteBuffer 的全部 API（四字段模型、Heap/Direct 分配、视图与陷阱）。尚不知道 NIO Channel 的概念——只有传统的 `java.net.Socket` 经验。

## Pass 0: 设计上下文

- `sun.nio.ch` 包: 43 个实现文件（`SocketChannelImpl`, `ServerSocketChannelImpl`, `IOUtil` 等）
- Channel API 演进: JDK 1.4 引入 NIO（SelectableChannel 基类），JDK 7 新增便捷方法 `SocketChannel.open(SocketAddress)` 一步完成 open+connect
- 设计意图: 用统一的 `Channel` 抽象替换 `InputStream/OutputStream` 的双流模型，为 `SelectableChannel.configureBlocking(false)` + Selector 的组合铺路

## Pass 1: 扫描结果

- 核心类: `SocketChannel` (abstract, wraps Socket)、`ServerSocketChannel` (abstract, wraps ServerSocket)、`SelectableChannel` (基类)
- 实现层: `sun.nio.ch.SocketChannelImpl` (1129 行)、`ServerSocketChannelImpl`
- 继承树: `AbstractSelectableChannel → SocketChannel / ServerSocketChannel`
- 标记问题 ≥5:
  1. `configureBlocking(false)` 后 read() 返回 0 是什么意思？和 blocking 模式下 `read()` 的 do-while 循环有什么区别？
  2. `readLock` 为什么是 per-channel 的？多个线程能同时读写同一个 SocketChannel 吗？
  3. ServerSocketChannel 的 `accept()` 在非阻塞模式下，有新连接时返回什么？没有新连接时返回什么？
  4. Channel 和传统的 `Socket.getInputStream()` 有什么区别？为什么要统一成 Channel 抽象？
  5. `beginRead/endRead` 的 interrupted 处理机制是什么？为什么 blocking 模式需要 `isOpen()` 检查的 do-while？

## 概念依赖链

```
传统 Socket 的双流模型 → Channel 单对象抽象 → blocking vs non-blocking → beginRead/endRead 线程安全 → ServerSocketChannel.accept() 的两面性
```

## 叙事顺序

**开篇场景：传统 Socket 的"流"困境**

每个写过 Java 网络程序的人都用过这段代码。`Socket.java` 的模型是双流：读走 `InputStream`、写走 `OutputStream`：

```java
Socket socket = new Socket("example.com", 80);
InputStream in = socket.getInputStream();
OutputStream out = socket.getOutputStream();
byte[] buf = new byte[1024];
int n = in.read(buf);  // 阻塞——当前线程卡住直到数据到达
```

这段代码没问题——但它把**连接**和**流**分成三个对象。如果你想"不阻塞地检查有没有数据", 做不到。`InputStream.read()` 只有一种模式：等。

**第一层：Channel — 一个对象替代三个**

NIO 的答案：`SocketChannel`。一个对象同时是**连接、读端、写端**：

```java
SocketChannel channel = SocketChannel.open(new InetSocketAddress("example.com", 80));
// 同一个对象既能读又能写
channel.read(buf);  // 同一个 channel 对象上的操作
channel.write(buf);
```

源码中 `SocketChannel.java:121` 是 abstract、`SocketChannelImpl.java:336` 是真实实现。`read()` 内部调用 `IOUtil.read(fd, buf, ...)`——`fd` 是原生 socket 文件描述符。而传统 `Socket.getInputStream()` 是 Java 层的封装——从这个点开始 NIO 就走了一条不同的路。

**第二层：blocking vs non-blocking — 一个开关改变一切**

这是 Channel 相比传统 Socket 的决定性差异。`configureBlocking()` 来自父类 `SelectableChannel`——这个继承关系本身就暗示了一个设计意图：**非阻塞模式是为 Selector 准备的**。只有 `SelectableChannel` 才能切换阻塞/非阻塞，只有非阻塞模式下的 Channel 才允许注册到 Selector。

`configureBlocking(false)` 改变的是**内核的行为**：

```java
// SocketChannelImpl.java:543
IOUtil.configureBlocking(fd, block);
```

`IOUtil.java:415` 声明为 `public static native void configureBlocking(...)`——这是一个 native 方法。它的语义等价于 POSIX 系统调用 `fcntl(fd, F_SETFL, O_NONBLOCK)`：设置文件描述符 fd 的非阻塞标志。之后内核的 `read(fd)` 在没有数据时不阻塞当前线程，直接返回 EAGAIN。

JDK 层的两种 read 行为（`SocketChannelImpl.java:351-357`）：

```java
if (blocking) {
    do {
        n = IOUtil.read(fd, buf, -1, nd);     // 内核 read → 阻塞
    } while (n == IOStatus.INTERRUPTED && isOpen());  // 被中断后重试
} else {
    n = IOUtil.read(fd, buf, -1, nd);          // 内核 read → 非阻塞, 无数据返回 0
}
```

关键差异：阻塞模式用 `do-while` 循环处理中断重试，非阻塞模式一次调用就返回。`IOStatus.normalize(n)` 把内核的 `0`（无数据）、`-1`（错误）等翻译成 Java 语义。

**第三层：beginRead/endRead — 围绕一次 I/O 的线程状态管理**

`SocketChannelImpl.java:300-330` 的 `beginRead/endRead` 是 NIO 的线程协作机制，不直接参与数据读取：

- `beginRead(blocking)`：阻塞模式下记录当前线程为 `readerThread`（`NativeThread.current()`），line 308。这注册了线程以便 `close()` 知道现在谁在持有这个 channel
- `endRead(blocking, completed)`：清除 `readerThread` 记录。如果 channel 正在关闭（`state == ST_CLOSING`），通知关闭线程它的等待可以结束了（`stateLock.notifyAll()`），line 326-327
- 非阻塞模式下 `begin/endRead` 也被调用但 `if(blocking)` guard 使它们为空操作——因为 `read()` 立即返回，没有需要协调的阻塞点

这是 `Socket.close()` 和 `SocketChannel.close()` 的根本区别：Channel.close() 等待读线程完成而不是强制打断它——通过 `ST_CLOSING` 标记 + `readerThread` 追踪 + `stateLock` 协调。传统 Socket 的 close() 直接关闭 fd，不管谁在读。

**第四层：ServerSocketChannel — accept 的两种模式**

```java
ServerSocketChannel server = ServerSocketChannel.open();
server.bind(new InetSocketAddress(8080));

// 阻塞模式: accept() 卡住等新连接
SocketChannel client = server.accept();

// 非阻塞模式: accept() 立即返回
server.configureBlocking(false);
SocketChannel client = server.accept();  // 无连接 → 返回 null
```

`ServerSocketChannel.java:79` 是 abstract。实现 `ServerSocketChannelImpl:274-293` 中的 `accept()` 对阻塞/非阻塞两种模式有不同行为——和 `SocketChannel.read()` 的模式切换对称。

但有一个关键的细节（`ServerSocketChannelImpl.java:296`）：

```java
// newly accepted socket is initially in blocking mode
IOUtil.configureBlocking(newfd, true);
```

**不管 ServerSocketChannel 本身处于什么模式，`accept()` 返回的新 `SocketChannel` 永远是阻塞模式启动的。** 如果你需要非阻塞的客户端 Channel——而你几乎一定需要——你必须在 `accept()` 之后显式调用 `clientChannel.configureBlocking(false)`。这是 NIO 中最容易被忽略的设置，忘记它会导致代码在入队 Selector 时抛 `IllegalBlockingModeException`。

**结尾悬念**

现在有了 Channel，可以打开端口、接收连接、收发数据。但阻塞模式下，一个线程只能等一个 Channel——回到每个连接一个线程的老路。非阻塞模式 `read()` 返回 0 时你得轮询——无限循环检查 1000 个 Channel。有没有一种方式不轮询、不用多线程，就能知道**哪个 Channel 有数据到了**？这个问题的答案在第 3 章。但在那之前，需要先理解 `read()` 和 `write()` 的返回值到底意味着什么——下一节。

## 核心悬念

**读完能回答**：`SocketChannel` 用一个对象替代了传统 `Socket` + `InputStream` + `OutputStream` 的三对象模型——这个统一带来了什么新的可能？`configureBlocking(false)` 改变了什么？阻塞模式和非阻塞模式的 `read()` 在源码层面有什么区别？`beginRead/endRead` 为什么存在？

## 文章边界

- **在本篇内**: Socket vs SocketChannel 对比 → blocking/non-blocking → beginRead/endRead → ServerSocketChannel.accept()
- **不属于本篇**: Selector（§3.1）、Channel 与 ByteBuffer 的完整收发循环（§2.3）、read 返回 0 后的空转问题（§2.2）
