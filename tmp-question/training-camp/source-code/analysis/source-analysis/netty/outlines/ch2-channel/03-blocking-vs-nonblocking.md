# Ch2 阻塞 vs 非阻塞 — configureBlocking 与收发循环

> Cluster C: 4 KPs | 依赖 §2.1+§2.2 | §2.2 → §2.3

### 1. configureBlocking — 两个单词切换两种语义

场景: 同一个 `read(buf)` 调用, 因为 `configureBlocking(true/false)` 的不同, 要么阻塞线程直到数据就绪, 要么立即返回 0 让调用方决定下一步。

源码路径: `SocketChannelImpl.java` — `configureBlocking(true)` → `IOUtil.configureBlocking(fd, true)` → **不做任何 fd 操作**(fd 本身已经是阻塞模式——标准 Unix 文件描述符的默认状态), 但设置 `nonBlocking=false` 让 `beginRead()/beginWrite()` 内部执行 `park()` 阻塞线程。`configureBlocking(false)` → `IOUtil.configureBlocking(fd, false)` → `fcntl(fd, F_SETFL, O_NONBLOCK)` ——这改变了内核行为: `read()` 系统调用在无数据时立即返回 `-1`+`errno=EAGAIN` 而非阻塞。`ServerSocketChannelImpl.accept()` 返回的 SocketChannel 默认 `configureBlocking(true)`(见 §2.2 accept)。

关键设计: NIO 的"阻塞"和"非阻塞"不是 Java 层面的包装差异——`configureBlocking(false)` 调用的是真正的 `fcntl O_NONBLOCK` 系统调用, 改变了内核文件描述符的行为。但"阻塞"模式并不把 fd 改回阻塞——它是在 `beginRead()/beginWrite()` 中通过 `park()` 在 Java 层模拟阻塞。这减少了内核态切换(不必每次切换都调 fcntl), 但意味着 NIO 的"阻塞模式"依赖于 `beginRead`/`endRead` 的配对——漏调 `endRead` 会导致线程永久挂起。

数据流: `configureBlocking(false)` → `fcntl(O_NONBLOCK)` → kernel 层: read/write 不再阻塞 → Java 层: `beginRead()` 不 park → `IOUtil.read()` 返回 0/-1 而非阻塞等待。

### 2. 阻塞 vs 非阻塞 — 完整行为对比

场景: 同一个 server, 你可以用两种完全不同的策略处理 1000 个连接: 1000 个线程各自阻塞等待(一个连接一个线程 → 线程上下文切换成本), 或者 1 个线程轮询 1000 个 Channel(非阻塞 → Selector)。选择取决于连接数和业务复杂度。

源码路径: `SocketChannelImpl.java:352-354` — 阻塞读: `do { n = IOUtil.read(fd, buf, -1, nd) } while (n == IOStatus.INTERRUPTED)` ——在 `beginRead()` park 之后的循环只处理被信号中断的情况。`SocketChannelImpl.java:356` — 非阻塞读: 单次 `n = IOUtil.read(...)`, 无循环, 无 park。阻塞写: `beginWrite()` park → `IOUtil.write()` → 不循环(写满一次就够了, 因为 write 会阻塞直到全部写完)。非阻塞写: 单次 `IOUtil.write()`, 写满 return 0。

关键设计: 阻塞模式下, `beginRead()` 的 `park()` 依赖底层 I/O 线程池的 `unpark()` 唤醒——当内核通知 fd 可读时。park 机制是 JDK NIO 自己的同步原语, 不是 `LockSupport`——`NativeThread` 级别的信号, 依赖文件描述符的内部事件通知。Netty 为什么不复用这个机制而是自己实现 EventLoop? 因为 JDK 的 park/unpark 绑定到文件描述符级别——每 Channel 有一个阻塞线程——Netty 的单 EventLoop 多 Channel 模型不能用 fd-park, 必须用 Selector。

数据流:
- 阻塞: `read()` → `beginRead()` park → 等待内核唤醒 → `IOUtil.read()` → `endRead()` unpark → 返回数据
- 非阻塞: `read()` → `IOUtil.read()` 单次 → 无数据返回 0 → 调用方决定(注册 Selector/重试/返回)

### 3. SocketAdaptor — NIO→BIO 的过渡桥

场景: 你有一份旧的 `java.net.Socket` 代码——用 `InputStream.read()` 读, `OutputStream.write()` 写。现在想改成 NIO 的 `SocketChannel`, 但不想改所有代码。`SocketAdaptor` 把 SocketChannel 包装成传统 Socket。

源码路径: `SocketAdaptor.java`(439 行) — 实现 `java.net.Socket`。`getInputStream()` 返回内部类 `ChannelInputStream`——它的 `read()` 实现: 非阻塞调用 channel.read(buf) → 返回 0 → **自旋重试**(`Thread.onSpinWait()`), 直到有数据——模拟阻塞 `InputStream.read()` 的行为。`connect()`: 先用 `channel.connect(addr)` 非阻塞发起 → 然后在 `channel.finishConnect()` 未完成时 park 等待——模拟阻塞 `Socket.connect()`。

关键设计: SocketAdaptor 的作用是过渡期工具——让 JDK 从 BIO 平滑过渡到 NIO。它不是为生产设计的——自旋重试会空耗 CPU。Netty 不提供类似的适配层——它直接要求用户接受 NIO 非阻塞模型。

数据流: `SocketAdaptor.getInputStream().read()` → `channel.read(buf)`(非阻塞) → n==0 → 自旋重试 → n>0 → 返回 → n<0 → 返回 -1 EOF。

### 4. 完整收发循环 — Channel + ByteBuffer 四步组合

场景: 把 §1.1-§1.3 的 ByteBuffer 和 §2.1-§2.2 的 Channel 合并——一次完整的网络收发需要 flip/compact/clear 配合 read/write。

源码路径: 读循环: `while ((n = channel.read(buf)) > 0) { buf.flip(); // 写模式→读模式 while (buf.hasRemaining()) process(buf.get()); buf.compact(); // 保留未读, 准备下一轮 read }`。`n == 0` 时退出(非阻塞, 等 Selector), `n == -1` 时 EOF 关闭。写循环: `while (buf.hasRemaining()) { channel.write(buf); }` ——非阻塞模式下, 写满返回 0 时 break, 记录剩余数据, 等 OP_WRITE 事件。

关键设计: 四步组合是 NIO 的最经典模式——`read→flip→process→compact`。如果 `process` 处理了全部数据, compact 后 `position == 0, limit == capacity`, 等价于 `clear()`。如果只处理了一半, compact 把剩下的一半前移, position 指向剩余数据末尾, limit=capacity→下一轮 read 追加在末尾。这正是 §4.2 ChannelOutboundBuffer 的非阻塞写循环的来源——Netty 把这个循环封装成 `addMessage→addFlush→nioBuffers→doWrite`。

数据流: `read(buf)` 推进 position → `flip()` 设 limit=pos, pos=0 → `process(buf.get())` 消费数据 → `compact()` 前移未读, 设 position=剩余长度 → 下一轮 `read(buf)` 追加在 position 之后。

### 核心悬念

**"非阻塞模式下, read 返回 0 让你可以注册 Selector 等下一轮——write 返回 0 让你知道 TCP 发送缓冲区满了需要记录待写数据。Ch3 Selector 就是用轮询代替忙等: 一个线程注册 1000 个 Channel, select() 返回后遍历就绪 Channel, 逐个 read——但 epoll 100% CPU bug(select 返回 0 但实际无就绪 Channel)是 Selector 的致命缺陷。Netty 的 EventLoop 怎么检测和修复这个 bug?"**

→ 引出 Ch3 NIO Selector — Ch1 的 ByteBuffer 让缓冲区有了状态机, Ch2 的 Channel 让数据有了出入管道。但 1000 个 Channel 不能用 1000 个轮询——Selector 让一个线程管理全部 I/O。它的 register/select/selectedKeys 三步曲和 epoll 的 bug 检测阈值是 Netty EventLoop 设计的关键前提。
