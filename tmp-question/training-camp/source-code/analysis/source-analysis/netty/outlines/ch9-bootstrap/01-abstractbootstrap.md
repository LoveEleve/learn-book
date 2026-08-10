# Ch9 AbstractBootstrap — bind() 的三步状态机与 ServerBootstrap

> Ch9 → §9.2 | 依赖 Ch8 MemoryPool

### 1. doBind — validate→initAndRegister→doBind0

场景: `serverBootstrap.bind(8080)` ——一个方法调用把 Ch4-Ch8 的全部基础设施(ByteBuf/EventLoop/Promise/Pipeline/MemoryPool)编织成一个可运行的服务端。

源码路径: `AbstractBootstrap.java:doBind()` — `validate()`: 检查 `group != null && channelFactory != null`。`initAndRegister()`: `channelFactory.newChannel()` 创建 Channel → `init(channel)` 抽象方法(Bootstrap.init 或 ServerBootstrap.init) → `group().register(channel)` 绑定到 EventLoop。`doBind0()`: `channel.bind(localAddress)`—EventLoop 异步执行。`PendingRegistrationPromise`: register 返回的 ChannelFuture 未完成→创建新 Promise→`promise.addListener(f -> doBind0())`—延迟 bind 到 register 完成后。`FailedChannel`: channelFactory.newChannel() 失败→promise.fail(AbstractBootstrap.java)。

关键设计: `doBind0` 必须是异步——不能在调用线程(main 线程)执行 `channel.bind()`—因为 Ch5 EventLoop 要求所有 I/O 操作在 EventLoop 线程执行。`PendingRegistrationPromise` 让 bind 延迟到 register 完成——Ch6 DefaultPromise 的 addListener 保证顺序: register 完成→listener 触发→doBind0→bind。`validate` 在前, `initAndRegister` 在中, `doBind0` 在后——三步串行但第三步异步——中间不能有阻塞点。

数据流: `bind(8080)`→`validate()`→`initAndRegister()`→`ReflectiveChannelFactory.newChannel()`→`init(channel)`→`group().register(channel)`→promise→`doBind0()`→`channel.bind(8080)`→EventLoop→`Unsafe.bind()`→OS bind→`promise.setSuccess()`→server started。

### 2. ServerBootstrap — 父-子 EventLoop 分离 + ServerBootstrapAcceptor

场景: Boss EventLoop 处理 accept 事件, Worker EventLoop 处理子 Channel 的 read/write——父子分离让 accept 与 I/O 不竞争同一线程。

源码路径: `ServerBootstrap.java:group(parentGroup, childGroup)` — 父子分离。`ServerBootstrap.init(channel)`: `pipeline.addLast(ChannelInitializer{ initChannel(ch) { ch.pipeline().addLast(ServerBootstrapAcceptor) } })`—delayed inject。`ServerBootstrapAcceptor extends ChannelInboundHandlerAdapter`: `channelRead(ctx, msg)`→accept 新 Channel→六步子 Channel 初始化: 1)`childGroup.register(child)`→2)`child.pipeline().addLast(childHandler)`→3)`setChannelOptions(child, childOptions)`→4)`setAttributes(child, childAttrs)`→5)`ChannelInitializerExtension.serverChildListener(channel)`(SPI)→6)`child.pipeline().fireChannelActive`。childGroup 回退: `childGroup==null → parentGroup`(worker 和 boss 共用)。exceptionCaught 限流恢复(#1328): accept 失败频繁→限流日志→防止刷屏。

关键设计: ServerBootstrapAcceptor 通过 ChannelInitializer 延迟注入——首次 accept 时才创建 acceptor——ChannelInitializer 自移除(§7.4 initChannel→finally pipeline.remove(this))让 acceptor 在 Server Channel 的 Pipeline 中只存在一次。`childGroup.register(child)` 是异步的——与 `doBind0` 同机制——PendingRegistrationPromise 保证 child channel 的后续初始化在 register 完成后执行。

数据流: `ServerBootstrap.bind(8080)`→`doBind`(同上)→`ServerSocketChannel` 就绪→client 连接→`ServerBootstrapAcceptor.channelRead(ctx, msg)`→`childGroup.register(child)`→`child.pipeline().addLast(childHandler)`→`child.group()=workerEventLoop`→`child.pipeline().fireChannelRegistered`→`child.pipeline().fireChannelActive`→子 Channel read/write 在 worker EventLoop 中。

### 3. Bootstrap.connect — DNS + 连接

场景: 客户端 `bootstrap.connect("example.com", 80)`——域名->IP 解析->TCP 连接。

源码路径: `Bootstrap.java:connect()` — DNS 三级跳: `disableResolver→return; isResolved→直连; else→resolver.resolve()`。惰性 DNS: `InetSocketAddress.createUnresolved(host, port)`—延迟解析 till connect。`ExternalAddressResolver` 反射加载—避免 NoClassDefFoundError(optional resolver 包)。`init(channel)`: 注入 Bootstrap 的 handler→`pipeline.addLast(handler())`+ClientInitializerExtension SPI→`doConnect(remoteAddr)`→`channel.connect(addr)`→EventLoop 投递。

关键设计: 客户端的 `init(channel)` 只注入 handler——不像 ServerBootstrap 注入 acceptor——client 没有 accept 逻辑。DNS 的惰性解析让 `Bootstrap.clone()` 不触发 DNS——只在 `connect()` 时解析。

数据流: `Bootstrap.connect("example.com", 80)`→`resolver.resolve("example.com")`→`InetAddress[93.184.216.34]`→`initAndRegister()`(同服务端)→`doConnect(addr)`→EventLoop: `channel.connect(addr)`→TCP 三次握手→`promise.setSuccess()`→channel active→read/write。

### 核心悬念

**"Bootstrap 的 Fluent API(group/channel/handler/option/attr)+doBind 三步状态机+ServerBootstrapAcceptor 的六步子 Channel 初始化——把 Ch4-Ch8 的全部基础设施编织成一个可运行的服务端。但 Pipeline 中流动的还是原始字节——Ch10 Codec 框架的 ByteToMessageDecoder——cumulation 积攒+callDecode 循环让这些字节变成业务消息。"**

→ 引出 Ch10 Codec — ByteToMessageDecoder.cumulation 双策略(MERGE/COMPOSITE) + callDecode 循环 + 三级状态机防重入——TCP 字节流→业务消息。
