# Ch2 读与写 — Channel 的双向数据管道

> Cluster A: 5 KPs | 依赖 Ch1 ByteBuffer | Ch2 → §2.2

### 1. read(buf) — 返回值不是字节数, 是信号

场景: `FileInputStream.read()` 返回 `-1` 表示 EOF——`SocketChannel.read()` 返回值有三种可能: 正数(有数据)、0(暂时没有)、-1(对方关闭)。0 是整个非阻塞模型的基础。

源码路径: `SocketChannelImpl.java:336-367` — `read(ByteBuffer buf)` 先调 `beginRead()` → 阻塞模式下 `do-while (n==IOStatus.INTERRUPTED)` 循环调用 `IOUtil.read(fd, buf, -1, nd)` 直到不被中断 → 非阻塞模式下单次 `IOUtil.read()` 不循环 → `endRead()`(若有阻塞则 unpark 等待线程)。`SocketChannelImpl.java:300-319` — `beginRead()` 在阻塞模式下调用 `park()` 挂起当前线程, readerThread 记录当前线程供 close 协调。

关键设计: 阻塞和非阻塞用的是同一个 `read()` 方法签名——差异在 `beginRead()` 的内部行为。阻塞模式下 `park()` 是 JDK 内部的线程挂起机制(不是 `LockSupport.park()`, 是 `NativeThread.signal()` 级别的), 配合 `endRead()` 的 `unpark()` 实现阻塞等待。IOStatus 常量: `INTERRUPTED=-3`(被中断需重试)、`EOF=-1`(对端关闭)、`UNAVAILABLE=-2`(非阻塞下暂时不可用)。

数据流: `read(buf)` → 阻塞: `beginRead()->park()` 挂起线程 → `IOUtil.read(fd)` → `endRead()->unpark()` → 返回 >0。非阻塞: `IOUtil.read(fd)` → 单次调用, 无数据返回 0 → 调用方决定下一步(注册 Selector 或重试)。

### 2. write(buf) — 写满时返回 0, 不是死循环

场景: `channel.write(buf)` 在阻塞模式下会一直等到 TCP 发送缓冲区有空间——但非阻塞模式下, 如果发送缓冲区满了, 返回 0 而不是被阻塞。你要自己追踪写了多少、还剩多少。

源码路径: `SocketChannelImpl.java:448-476` — `write(ByteBuffer buf)` → `beginWrite()` 对称 `beginRead()` → `IOUtil.write(fd, buf, -1, nd)` 写入 → `endWrite()`。`SocketChannelImpl.java:410-431` — `beginWrite()` 在阻塞模式下同样 park。

关键设计: 非阻塞 write 返回 0 时, buffer 的 position 没有推进——buffer 中还有一个 `hasRemaining()` 返回 true。下一次 write 会自动从上次中断的位置继续写。这意味着非阻塞写入需要维护一个"待写缓冲区队列"——Netty 的 `ChannelOutboundBuffer` 的 flushed→unflushed→tail 三指针链表正是为此设计的。

数据流: `write(buf)` → `IOUtil.write(fd, buf)` → 返回 N 字节已写 → buffer.position += N → 若 `hasRemaining()` → 阻塞模式下循环写入, 非阻塞模式下记录剩余 → 等 select 发现 `OP_WRITE` 就绪再写。

### 3. readLock / writeLock — 读和写两把锁

场景: 多线程环境下——线程 A 正在 `read(buf)`, 线程 B 同时 `read(buf2)`——两个读操作可以并行吗？读和写可以并行吗？

源码路径: `SocketChannelImpl.java:76,79` — `readLock = new ReentrantLock()` 和 `writeLock = new ReentrantLock()` 是两把**独立的**锁。读者之间共享 readLock(Reentrant 可重入, 但同一线程才能重入, 不能多线程同时读), 写者共享 writeLock, 但读者和写者之间不互斥——你可以一个线程在 `read()`, 另一个线程同时 `write()`。`SocketChannelImpl.java:104-105` — `readerThread`/`writerThread` 是两个 `volatile` 字段——Channel 关闭时通过 `stateLock` 配合这两个标识等待正在读/写的线程完成再释放资源。

关键设计: readLock 和 writeLock 分离是 NIO 的关键设计选择——读写可以并发。但同一方向(读-读 或 写-写)是互斥的——因为系统调用的 `read(fd)`/`write(fd)` 本身不是可重入的, 两个读操作同时发生会导致数据交织。Netty 通过 EventLoop 的单线程模型直接避免了这个问题——每个 Channel 只在它的 EventLoop 线程上读写, 不需要锁。

数据流: Thread A: `readLock.lock()` → `read(0)` → `readLock.unlock()`。Thread B: `writeLock.lock()` → `write(0)` → `writeLock.unlock()`。A 和 B 不互斥。但两个 A 线程不能同时 read。

### 4. IOStatus — 系统调用返回值归一化

场景: OS 的 `read()` 系统调用在不同平台(Linux/macOS/Windows)返回不同的负数表示不同含义——JDK NIO 需要一套统一的常量表示。

源码路径: `IOUtil.java` — `IOStatus` 定义了三个常量: `INTERRUPTED=-3`(I/O 被信号中断, 需重新尝试)、`UNAVAILABLE=-2`(非阻塞模式下无数据/写满)、`EOF=-1`(对端关闭连接)。`IOStatus.normalize(int n)`: 负数→0——在非阻塞模式下, 所有"没有数据"的情况(包括 EOF)归一化为 0, 让调用方用统一的 `==0` 判断。

关键设计: EOF 在非阻塞模式下被 normalize 吞掉——`read()` 返回 0 可能是无数据也可能是 EOF。Netty 通过 `read()` 返回 -1 来传递 EOF, 不混淆"暂时没数据"和"连接关闭"。

数据流: `IOUtil.read(fd, buf)` → 内核返回 (平台相关负数或正数) → `IOStatus.normalize(n)` → IOStatus 常量 → 上层 `read()` 方法解释: ==0→pending, ==-1→EOF。

### 核心悬念

**"read() 返回 0 意味着'暂时没数据但连接还在'——这个 0 让非阻塞 I/O 成为可能。但 1000 个 Channel, 怎么知道哪个 Channel 有数据可读？一个一个轮询 read() 返回非零? 还是 Selector 帮忙? §2.2 的 connect/accept 是连接建立的必要过程——但 accept() 返回的 SocketChannel 是阻塞的, 为什么?"**

→ 引出 §2.2 连接与接受 — connect 的 TCP 三次握手不在一个 `connect()` 调用中完成, `finishConnect()` 是必不可少的第二拍。accept 返回的 SocketChannel 默认阻塞, 需要显式切换。
