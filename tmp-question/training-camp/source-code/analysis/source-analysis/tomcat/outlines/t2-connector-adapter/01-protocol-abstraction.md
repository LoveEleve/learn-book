# T-2 §1 Protocol 抽象层 — 协议如何与 I/O 传输解耦

> 依赖 T-1 §4 | 🔴 Deep | 3 KP | 第一篇文章

**读者处境**: T-1 学完容器树——知道请求最终到达 `engine.getPipeline().getFirst().invoke()` — 但谁调了这行？Connector。Connector 怎么拿到请求的？从 TCP 端口。谁来监听 TCP 端口？在 Netty 里是 `ServerBootstrap.bind()` — 在 Tomcat 里是 Protocol + Endpoint 的组合。

### 1. Protocol 三层继承 — AbstractProtocol→AbstractHttp11→Http11NioProtocol

场景: `server.xml` 里配了 `<Connector port="8080" protocol="HTTP/1.1">` — 这个 `<Connector>` 标签如何映射到 Java 类？Tomcat 用 protocol 字符串查找 `Http11NioProtocol` — 这就是 "一个 Connector = 一个 Protocol + 一个 Endpoint" 的设计。

源码路径:
- `Http11NioProtocol.java:33-35` — 构造器: `new NioEndpoint()` 创建底层 NIO 传输 → `super(endpoint)` 传给父类 AbstractHttp11JsseProtocol
- `AbstractProtocol.java:80` — `AbstractEndpoint<S,?> endpoint` — Protocol 持有 Endpoint 引用，但不关心它是 NIO/NIO2/APR
- `AbstractProtocol.java:147` — `Adapter adapter` — ProtocolHandler 持有 Adapter，只有 adapter 知道如何交给 Container
- `AbstractProtocol.java:532` — **抽象工厂** `protected abstract Processor createProcessor()` — 子类 Http11Protocol 创建 `Http11Processor`

关键设计: **Strategy 模式 — Protocol 只定义流程，Endpoint 提供 I/O**。`AbstractProtocol.start()` 调 `endpoint.start()`(L637-648) — 但 Protocol 不知道 start() 内部是 `Selector.open()` 还是 `epoll_create()` — 这是 Endpoint 的职责。同样的 I/O 解耦在 Netty 里是 `EventLoopGroup` — 换 NioEventLoopGroup→EpollEventLoopGroup 只需改一行。 [模式: Strategy]

→ 实现规范: 非 Servlet 规范直接定义 — 但 `ProtocolHandler` 接口是 Tomcat 对"网络协议处理"的抽象。`Adapter` 接口是 Tomcat 对"协议层→容器层"的桥接抽象。

数据流: `server.xml 解析`→找到 protocol="HTTP/1.1"→反射 `Http11NioProtocol.class`→`new Http11NioProtocol()`→`new NioEndpoint()`→`super(endpoint)` 设置 endpoint 字段→`Connector.setProtocolHandler(protocol)`→`connector.init()`→`protocol.init()`→`endpoint.init()`→`endpoint.bind()`(监听 8080)→`protocol.start()`→`endpoint.start()`→Acceptor 线程开始 accept()。

### 2. Endpoint 解耦 — 为什么不用一个类搞定 Protocol + I/O？

场景: 如果只有 NIO — 把 `read()` 写在 `Http11NioProtocol` 里就行。但生产环境有人用 `Http11Nio2Protocol`（NIO2 异步）、有人用 `Http11AprProtocol`（APR 本地库的高性能 sendfile）。三个 Protocol 共享同样的 HTTP/1.1 解析逻辑（Http11Processor — 同一个），但 I/O 层完全不同。

源码路径: `AbstractProtocol.java:221-292` — 大量属性 getter/setter 直接透传: `getMaxThreads/setMaxThreads`→`endpoint.setMaxThreads()`, `getPort/setPort`→`endpoint.setPort()`。`AbstractProtocol` 是墙 — Protocol 层不管理连接数/端口 — 完全委托。

关键设计: **为什么不是 Interface + 3 Impl？** 因为 HTTP 协议处理逻辑（读 HTTP 头→解析→生成 Request）在 Http11Processor — 它被所有 NIO/NIO2/APR 协议共享。如果 Protocol 和 Endpoint 是同一个类 — 三种 I/O 模型需要三个版本的 Http11Processor。**Protocol = I/O 无关的协议骨架 + Endpoint = I/O 实现** — 这是 **Bridge 模式的变体**: 协议是抽象，I/O 是实现，两者独立变化。 [模式: Bridge — Protocol(抽象) + Endpoint(实现)]

数据流: `connector.getProtocolHandler().getEndpoint()` 获取当前 Endpoint→`endpoint.getPort()` 查端口→`endpoint.getLocalPort()` 动态端口。Protocol 层的 `setPort()` 调 `endpoint.setPort()` 透传 — Tomcat 嵌入式模式下 `Tomcat.setPort(8080)`→`connector.setPort()`→`protocol.setPort()`→`endpoint.setPort()` — 整个调用链全是委托。

### 3. Protocol Lifecycle — init→start→stop→destroy 对齐容器

场景: T-1 学了 Lifecycle 状态机 — 所有容器都有 init/start/stop/destroy。Protocol 也一样 — `AbstractProtocol` 没有 extend `LifecycleBase` — 但有完全对称的生命周期方法 — 因为 `ProtocolHandler` 接口要求。

源码路径:
- `AbstractProtocol.java:608-634` — `init()`: JMX ObjectName 注册→`endpoint.init()`。JMX 域名从 `adapter.getDomain()` 获取
- `AbstractProtocol.java:637-648` — `start()`: `endpoint.start()`→启动 `monitorFuture`(0/60s scheduleWithFixedDelay)→`startAsyncTimeout()`
- `AbstractProtocol.java:706-724` — `stop()`: 取消 monitor→`stopAsyncTimeout()`→对 waitingProcessors 调 `timeoutAsync(-1)` 强制超时→`endpoint.stop()`

关键设计: **Why Protocol 有自己的 start/stop 而不是跟 Container 走同一套 Lifecycle？** 因为 Protocol 不属于 Container 树 — `Connector` 持有 `ProtocolHandler` — 但 `Connector` 不是 `Container`。Connector 在 Service 层启动时调用 `connector.start()` — 内部调用 `protocol.start()` — Protocol 不管理子组件容器树 — 它只有一个 Endpoint — 用简单顺序调用而非 `startStopExecutor` 线程池。

数据流: `Service.startInternal()`(T-1 §3)→`connector.start()`→`protocol.start()`→`endpoint.start()`(Acceptor 线程开始)→monitor 开始调度 `startAsyncTimeout()`→TCP 端口就绪→`Service STARTED`。

→ 引出 §2 Processor — 请求来了 — Acceptor accept() 了连接 — 但谁把 TCP 字节流解析成 HTTP Request 对象？答案是 Processor — 每个连接分配一个 Http11Processor — 它知道 HTTP/1.1 的报文格式 — 逐行解析→生成 coyote.Request — 然后调用 `adapter.service(request, response)`。
