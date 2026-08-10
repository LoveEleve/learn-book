# T-4 §1 Acceptor→Poller→Worker — Tomcat NIO 的三线程模型

> 依赖 T-3 §2 | 🔴 Deep | 3 KP | Reactor 变体

**读者处境**: 在 Netty Ch3 学了 Selector 模型 — 知道 `Selector.select()` 可以同时监控 N 个连接。Tomcat 的 NIO 也是基于 Selector — 但 Acceptor/Poller/Worker 的三线程分工与 Netty 的 Boss/Worker 有什么不同？

### 1. Acceptor — 接收连接 + 反压

场景: Spring Boot `server.port=8080` 启动后 — 一个单独的 Acceptor 线程在 `while(true)` 循环中阻塞 `accept()` — 新连接到达→计数检查→交给 Poller 注册。这是 Tomcat 的"前台接待"。

源码路径:
- `Acceptor.java:68` — **run()**: `while(!stopCalled)` — 死循环等待连接
- `Acceptor.java:114-116` — **countUpOrAwaitConnection()**: 先 `countUp()` — 若已达 `maxConnections`(默认8192)→`awaitConnection()` 阻塞 — 反压: Acceptor 不接收新连接直到有空位
- `Acceptor.java:75-107` — **pause 自适应等待**: `<1ms` 紧循环 / `1-10ms` sleep 1ms / `>10ms` sleep 10ms — 优雅暂停

关键设计: **Why 8192 连接上限？** 不是 Tomcat 只能处理 8192 连接 — 是**限制连接并发**而非请求并发。8192 个 TCP 连接 = ~8192 个 SocketWrapper + ~8192 个 Selector 注册 — 内存约 100MB。超过 8192 — 反压到更上层(负载均衡器/操作系统 TCP backlog)。[模式: Bounded Buffer — maxConnections 是缓冲区上限]

数据流: `ServerSocketChannel.accept()`→新 SocketChannel→`countUpOrAwaitConnection()`→连接数+1→`endpoint.setSocketOptions(socketChannel)`→设置 non-blocking/linger/buffer→`NioEndpoint.setSocketOptions()`→创建 `NioChannel` → `NioSocketWrapper(nioChannel)` → `poller.register(socketWrapper)`→Poller 将 SocketChannel 注册到 Selector→`poller.addEvent(PollerEvent(wrapper, OP_REGISTER))`→唤醒 Poller。

### 2. Poller — Selector 事件循环

场景: 1000 个连接已建立 — 10 个有数据可读 — 其它 990 个空闲。Poller 线程在 `selector.select()` 阻塞 — 只返回有事件的 10 个连接 — 不浪费 CPU 轮询空闲连接。这是 NIO 相比 BIO(一连接一线程) 的核心优势。

源码路径:
- `NioEndpoint.java:595-598` — **Poller 内部类**: `Selector selector` + `SynchronizedQueue<PollerEvent> events` — 事件队列连接 Acceptor 和 Poller
- `NioEndpoint.java:741` — **Poller.run()**: `events()` 处理注册事件→`selector.select(selectorTimeout)` 等待 I/O 事件→`selectedKeys` 遍历→`processKey()`
- `NioEndpoint.java:672` — **events()**: 从队列取出 PollerEvent — 将新 SocketChannel 注册到 Selector 的 `OP_READ` 事件
- `NioEndpoint.java:628-632` — **addEvent()**: `events.offer(event)` + `wakeupCounter.incrementAndGet()` CAS — 若 `wakeupCounter==0`→`selector.wakeup()` 唤醒

关键设计: **Why wakeupCounter CAS 而不是每次都 selector.wakeup()？** `selector.wakeup()` 是昂贵的系统调用(Linux `pipe write`/`eventfd`)。如果有 100 个连接同时到达 — 不需要 100 次 wakeup — 第一次 wakeup 后 Poller 已经在处理 events 队列 — 后续 `addEvent` 的 event 会被已经在运行的 `events()` 取出。CAS 保证了"只在 Poller 确实在 select() 中阻塞时"才唤醒。[模式: Double-Checked Wakeup]

数据流: Acceptor 调用 `poller.register(wrapper)`→`poller.addEvent(new PollerEvent(wrapper, OP_REGISTER))`→`events.offer(event)`→`wakeupCounter.getAndIncrement()`→wakeupCounter==0→`selector.wakeup()`→Poller 从 select() 返回→`events()` 取出 PollerEvent→`wrapper.getSocketChannel().register(selector, OP_READ, wrapper)`→Selector 开始监听该连接的读事件→下一轮 select()→有数据可读→selectedKeys 返回→`processKey()`→`processSocket(wrapper, OPEN_READ)`。

### 3. Worker — Executor 线程池执行

场景: Poller 发现有数据可读 — 创建一个 `SocketProcessor`(Runnable 包装) — 提交给线程池执行。SocketProcessor 内部: 读取 HTTP 报文→Http11Processor 解析→CoyoteAdapter.service()→Pipeline.invoke()→servlet.service()→整条 T-2→T-3 的调用链都在 Worker 线程上执行。请求处理完→线程归还线程池→下一个 SocketProcessor 复用。

源码路径:
- `NioEndpoint.java:1681` — **SocketProcessor**: `extends SocketProcessorBase<NioChannel>` — `implements Runnable` — 包装 `SocketWrapperBase + SocketEvent`
- `AbstractEndpoint.java:590` — **Executor executor** — Spring Boot 默认 `ThreadPoolExecutor(10 核心, 200 最大, 60s keepAlive)`
- `AbstractEndpoint.java:178` — **internalExecutor=true** — 外部未注入时自动创建

关键设计: **Why 200 最大线程？** T-3 的 Pipeline 是同步阻塞的 — `servlet.service()` 可能执行 JDBC→Redis→RPC — 整个线程阻塞等待。如果只有 10 个线程 — 10 个慢请求就会阻塞全部请求处理。200 = 200 个并发慢请求可同时被处理 — 超过 200 → Tomcat 拒绝(Poller 不调 processSocket) → 反压到客户端(TCP 连接不关闭 — 等待 Poller 处理)。

数据流: `Poller.processKey()`→`processSocket(wrapper, OPEN_READ)`→`SocketProcessor processor = new SocketProcessor(wrapper, event)`→`executor.execute(processor)`→Worker 线程 `processor.run()`→`wrapper.getSocket()`→`InputBuffer inputBuffer = new InternalInputBuffer()`→Http11Processor 读取 HTTP 报文→解析 Method/URI/Headers→`adapter.service(coyReq, coyRes)`→Pipeline→EngineValve→HostValve→ContextValve→WrapperValve→FilterChain→`servlet.service()`→返回→`processor` 结束→线程归还线程池→Poller 一轮循环结束。

→ 引出 §2 Poller 内部机制 — 三线程模型已理解 — 但 Poller 内部的 `selector.select()`、`wakeup()`、PollerEvent 缓存池、keep-alive 连接复用是更深层的 I/O 优化。Poller 是 Tomcat 高性能的核心 — 值得单独深入。
