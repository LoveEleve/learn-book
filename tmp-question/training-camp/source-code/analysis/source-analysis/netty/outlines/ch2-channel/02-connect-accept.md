# Ch2 连接与接受 — connect/finishConnect 的双拍与 accept 的陷阱

> Cluster B: 4 KPs | 依赖 §2.1 | §2.1 → §2.2

### 1. connect — 非阻塞的三次握手

场景: `java.net.Socket.connect()` 是阻塞的——调用线程停在 TCP 三次握手完成前。`SocketChannel.connect()` 在非阻塞模式下立即返回——不等待握手完成。

源码路径: `SocketChannelImpl.java:672-754` — `connect(SocketAddress sa)` 先检查 state 是否已连接/正在连接 → `configureBlocking(false)` 确保非阻塞(NIO SocketChannel 强制非阻塞连接) → `ND.connect(fd, remoteAddr)` 发起 TCP connect 系统调用 → 若连接立即成功(Local UNSPECIFIED 连接→本地), `state=ST_CONNECTED` → 否则 `state=ST_CONNECTIONPENDING`, 返回 false。`ND.connect()` 是非阻塞 connect——OS 返回 `EINPROGRESS` 时 Java 返回 false 而非抛异常。

关键设计: 非阻塞 connect 是两拍操作: `connect()` 发起 → 返回 false → `finishConnect()` 确认(TCP 握手完成)。如果跳过 finishConnect, write 会抛 `NotYetConnectedException`。Netty 的 `Bootstrap.connect()` 用 Selector 注册 `OP_CONNECT` 兴趣, 在 select 返回后调用 `finishConnect()`——用户不需要关心两拍细节。

数据流: `connect(remoteAddr)` → `ND.connect(fd, addr)`(EINPROGRESS) → state=ST_CONNECTIONPENDING → 返回 false → `finishConnect()`(后续调用) → `ND.finishConnect(fd)` → state=ST_CONNECTED。

### 2. finishConnect — 第二拍确认

场景: `connect()` 已经返回了——但你不知道 TCP 三次握手是否完成。需要通过 `finishConnect()` 确认。如果连接仍在进行, 返回 false 表示需要等待; 返回 true 表示握手成功。

源码路径: `SocketChannelImpl.java:757-777` — `finishConnect()`: 检查 state==ST_CONNECTIONPENDING → 调 `ND.finishConnect(fd)` → 底层 `getsockopt(fd, SOL_SOCKET, SO_ERROR)` 检查连接状态 → 若已完成(无错误), `state=ST_CONNECTED`, 返回 true → 否则返回 false。Linux 下等价用 `poll(fd, POLLOUT)`——因为连接完成的 socket 会变为可写。

关键设计: `finishConnect()` 可以重试——非阻塞 connect 未完成时, 你可以注册 Selector 的 `OP_CONNECT`, select 返回后再调用 `finishConnect()`, 避免忙等轮询。这正是 Ch3 Selector 与 Ch2 Channel 的交汇点。

数据流: `finishConnect()` → `ND.finishConnect(fd)`(getsockopt SO_ERROR) → 成功→state=ST_CONNECTED, 返回 true → 失败(仍在握手)→返回 false → 需要等待(Selector OP_CONNECT 或重试)。

### 3. bind — 固定本地地址

场景: Server 绑定监听地址(必须)。Client 通常让 OS 自动分配源地址和端口——但某些场景需要绑定特定 IP/端口: 多网卡机器指定出网卡、防火墙规则要求固定源端口。

源码路径: `SocketChannelImpl.java:572` — `bind(SocketAddress local)`: `Net.bind(fd, isa)` — Linux 上使用 `bind()` 系统调用。ServerSocketChannel 的 `bind` 同样走 `Net.bind`。`NetworkChannel.setOption/setOption` — 支持 `SO_RCVBUF/SO_SNDBUF/SO_REUSEADDR/SO_KEEPALIVE/TCP_NODELAY` 在 bind 前后设置。

关键设计: 客户端 bind 是可选的——不调用则 OS 自动选择源 IP(`connect()` 前不 bind)和临时端口。服务端 bind 是必须的——不 bind 客户端无法连接。Netty 的 `Bootstrap.option()` 和 `ServerBootstrap.option()` 在 `initAndRegister` 之前设置, 最终通过 `setChannelOptions` 调用到 `NetworkChannel.setOption`。

数据流: `bind(localAddr)` → `Net.bind(fd, addr)` → fd 绑定 → 服务端: `listen()` 进入监听状态 → 客户端: `connect()` 时源使用此绑定地址。

### 4. accept — 新连接默认是阻塞的

场景: `ServerSocketChannel.accept()` 返回了一个新的 `SocketChannel`——你希望它是非阻塞的, 但它默认是阻塞模式! 这个设计陷阱让很多 NIO 新手困惑。

源码路径: `ServerSocketChannelImpl.java:296` — `accept()` 内部: 调 `Net.accept(fd, ...)` 接收连接 → 返回 `SocketChannelImpl` → **`IOUtil.configureBlocking(newfd, true)`** ——新 SocketChannel 被显式设为**阻塞**模式。`ServerSocketChannel.accept()` 自身在非阻塞模式下返回 null(无新连接)或 SocketChannel。ServerSocketConfig 没有为 accept 返回的子 Channel 提供默认配置——需要业务代码手动 `configureBlocking(false)`。

关键设计: 这是 NIO 设计的历史包袱——传统的 `java.net.Socket` 是阻塞的, NIO 为了向后兼容, `accept()` 返回的 SocketChannel 默认阻塞。Netty 的 `ServerBootstrap.childOption()` 在 `ServerBootstrapAcceptor` 中自动为新 accept 的 Channel 调用 `configureBlocking(false)`, 设置 child options, 注入 childHandler——用户零感知。

数据流: `accept()` → `Net.accept(fd)→new SocketChannelImpl(newfd)` → `IOUtil.configureBlocking(newfd, true)`(阻塞!) → 返回阻塞 SocketChannel → 需要显式 `configureBlocking(false)` → 才能用 Selector 管理。

### 核心悬念

**"accept() 返回阻塞 Socket, connect() 是非阻塞的两拍操作——同一个 Channel 家族, 为什么行为完全不同？这是 NIO 设计的过渡折中——向后兼容旧 Socket 代码。Netty 的 ServerBootstrap 帮你抹平了这些差异, 但它的 childGroup/childHandler 机制内部具体做了几步初始化？§2.3 的阻塞 vs 非阻塞对比将展开 read/write 在两种模式下的完整行为差异——而 loopback 测试的回声循环, 是 Channel 与 ByteBuffer 协作的最小可运行验证。"**

→ 引出 §2.3 阻塞对比与收发循环 — configureBlocking 是连接建立后的第一步, SocketAdaptor 是旧代码的过渡桥。但 write 在阻塞/非阻塞下的差异——阻塞写满后挂起 vs 非阻塞写满返回 0——是 Netty ChannelOutboundBuffer 的设计动机来源。
