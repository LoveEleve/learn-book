# Ch9 ChannelInitializer + BootstrapConfig + Clone

> §9.1 → §9.2 | 依赖 §9.1

### 1. ChannelInitializer — initChannel 自移除防重入

场景: `childHandler(new ChannelInitializer(){ initChannel(ch) { ch.pipeline().addLast(codec, handler); } })` —新 Channel 被 accept 后, ChannelInitializer 把 codec 注入 Pipeline, 然后从 Pipeline 移除自己——不需要永久占据 Pipeline 位置。

源码路径: `ChannelInitializer.java:125-141` — `initChannel(ctx)`:`initMap.add(ctx)` 防重入→`initChannel(C)` 回调→`finally { pipeline.remove(this) }`。`ChannelInitializer.java:53-59` — `@Sharable` + `ConcurrentHashMap.newKeySet()`—多 Channel 共享同一实例, 每个 Channel 独立防重入。`handlerAdded`: Channel.isRegistered()?→`initChannel`; else→等 `channelRegistered`(ChannelInitializer.java:105-117)。`ChannelInitializerExtensions.java` — SPI 三级开关: none/serviceload/log→三级回调: clientListener(ClientBootstrap)/serverListener(ServerBootstrap)/serverChild(ServerBootstrapAcceptor)。`WeakReference<ClassLoader>` 缓存 + DCL 单例。

关键设计: ChannelInitializer 的 `pipeline.remove(this)` 保证"用完即毁"——initChannel 回调中的代码可以任意添加 Handler——不需要担心多次添加(initMap 防重入)。`extensions` SPI 让生产环境的初始化可配置: 如在 clientListener 中统一设置 TCP_NODELAY=true 通过 SPI 而非每个 Bootstrap.option 手动设置。

数据流: `channelRegistered`→`handlerAdded`→`initChannel(channel)`→`channel.pipeline().addLast(codec, handler)`→`finally { pipeline.remove(this) }`→ChannelInitializer 离开→SPI: `clientListener(channel)`→`TCP_NODELAY=true`→`channel.pipeline().fireChannelRegistered`。

### 2. BootstrapConfig — 不可变配置快照 + clone

场景: 一个 Bootstrap 配置了 5 个 options + 3 个 attributes——需要传递给另一个组件但不想让它修改原 Bootstrap。

源码路径: `AbstractBootstrapConfig.java` — `group()/channelFactory()/options()/attrs()` 只读视图。`Bootstrap.clone()`: `super.clone()` 浅拷贝→`options(LinkedHashMap.clone())` 深拷贝→`attrs(ConcurrentHashMap.clone())` 深拷贝——修改 clone 不影响原 Bootstrap(AbstractBootstrap.java)。`BootstrapConfig` 和 `ServerBootstrapConfig` 分别提供 `remoteAddress()` 和 `childGroup()/childHandler()`。

关键设计: clone 的深拷贝只针对 options/attrs(配置和属性)——handler/group/childHandler 共享引用(不深拷贝)——因为 Handler 和 Group 通常是不变的——不需要拷贝。options 用 LinkedHashMap(保持添加顺序)+attrs 用 ConcurrentHashMap(线程安全)。

数据流: `Bootstrap original = ... ; Bootstrap clone = original.clone()`→clone.options 是 original.options 的深拷贝→`clone.option(TCP_NODELAY, true)`→不影响 original→`original.connect()`→使用原始 TCP_NODELAY=false。

### 3. ChannelFactory — 反射创建 Channel

场景: `bootstrap.channel(NioServerSocketChannel.class)` — Bootstrap 需要创建 Channel 的工厂——用反射 `clazz.getConstructor().newInstance()`。

源码路径: `ReflectiveChannelFactory.java` — `newChannel()`:`clazz.getDeclaredConstructor().newInstance()`。`bootstrap/ChannelFactory.java` — `void newChannel(ChannelPromise)` 回调版本(使用传 promise)。

关键设计: `ReflectiveChannelFactory` 是单例——同一个 Bootstrap 实例的所有 Channel 都用同一个工厂——`getDeclaredConstructor()` 只在 `newChannel()` 首次调用时执行——后续缓存 Constructor 对象。

数据流: `bootstrap.channel(NioServerSocketChannel.class)`→`new ReflectiveChannelFactory(NioServerSocketChannel.class)`→`doBind()`→`initAndRegister()`→`channelFactory.newChannel()`→`constructor.newInstance()`→`new NioServerSocketChannel()`。

### 核心悬念

**"Bootstrap 的 bind()/connect() 把 Ch4-Ch8 的全部基础设施编织为可运行的 Netty 应用。但 Pipeline 中流动的还是原始字节——Ch10 Codec 框架的 ByteToMessageDecoder——cumulation 积攒+callDecode 循环让字节变成消息。四类拆包器(FixedLength/DelimiterBased/LengthFieldBased/LineBased)覆盖了 TCP 流的所有边界问题。"**

→ 引出 Ch10 Codec — ByteToMessageDecoder 的 cumulation 积攒 + callDecode 循环 + 三级状态机防重入——半包处理 → 完整消息。
