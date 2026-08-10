# T-2 §2 Processor — 请求处理单元 + 池化复用

> 依赖 §1 | 🔴 Deep | 3 KP | [模式: Object Pool]

**读者处境**: 知道 Protocol+Endpoint 监听端口、accept 连接。但 HTTP 是请求-响应协议 — 一个 TCP 连接上可能有 N 个请求(keep-alive) — 每个请求需要一个"处理器"。在 Netty 里是 `ChannelHandler` — 在 Tomcat 里是 `Processor`。

### 1. Processor 接口 — 5 个核心契约

场景: Endpoint 的 Poller 线程发现 Socket 有可读事件 — 它需要把连接交给 Processor 处理 — 但 Poller 不知道这是 HTTP/1.1 还是 HTTP/2。Processor 接口让 Poller 只需调 `processor.process(wrapper, event)` — 具体协议解析由实现类决定。

源码路径:
- `Processor.java:43` — `SocketState process(SocketWrapperBase<?> socketWrapper, SocketEvent status)` — 返回 SocketState 决定连接后续调度
- `Processor.java:84` — `recycle()` — 回收 Processor，重置状态为下一请求准备
- `Processor.java:62` — `isAsync()` — 是否处于异步处理状态(Servlet 3.0)
- `Processor.java:74` — `timeoutAsync(long now)` — 异步超时检查，now<0 时强制触发
- `Processor.java:57` — `isUpgrade()` — 是否正在处理协议升级(HTTP→WebSocket/h2c)

关键设计: **SocketState 决定了连接的命运** — `OPEN`(keep-alive 继续读)、`LONG`(长轮询注册读兴趣)、`UPGRADING`(协议升级)、`CLOSED`(关闭连接)。Poller 线程根据 SocketState 决定: 重新注册 Selector 等待下一请求、关闭连接、或等待异步事件。这是 Tomcat 的 "非阻塞 I/O 状态机" — Netty 也有同样的概念(`channelReadComplete`→决定是否继续读)。 [模式: State — SocketState 枚举驱动连接状态机]

数据流: Poller 检测到 `SocketEvent.OPEN_READ`→`ConnectionHandler.process(wrapper, OPEN_READ)`→从 recycledProcessors 获取 Processor→`processor.process(wrapper, OPEN_READ)`→Http11Processor 读取 HTTP 报文→解析 Method/URI/Headers→创建 coyote.Request→调用 `adapter.service(request, response)`(CoyoteAdapter)→Adapter 内部设置 catalina Request→Mapper 路由→Engine Pipeline invoke→servlet.service()→响应写入→Processor 返回 `SocketState.OPEN`→ConnectionHandler 回收 Processor→registerReadInterest()→Poller 继续监听该连接。

### 2. ConnectionHandler — Processor 的获取↔回收中枢

场景: Tomcat 每秒处理 10000 个请求 — 每个请求都 new Http11Processor→用完就丢弃？不 — Http11Processor 创建时需要注册 JMX MBean、分配输入/输出缓冲区 — new 的成本 ~5ms — 回收只需 `recycle()` 重置字段 ~0.1ms。Processor 池化是 Tomcat 高性能的隐藏基石。

源码路径:
- `AbstractProtocol.java:887-899` — **三阶段获取**: 1) `recycledProcessors.pop()` 从池取 → 2) 池空→`createProcessor()` 新建→`register(processor)` JMX 注册 → 3) `processor.setSslSupport()` 设置 SSL 信息
- `AbstractProtocol.java:974-979` — **OPEN 状态回收**: `release(processor)`→`wrapper.registerReadInterest()` 注册读兴趣 — keep-alive 循环
- `AbstractProtocol.java:1092-1116` — **release() 逻辑**: `processor.recycle()`→upgrade 类型从 waitingProcessors 移除→非 upgrade push 到 recycledProcessors 池
- `AbstractProtocol.java:166` — `processorCache=200` — 池最大容量，超过则 unregister+丢弃

关键设计: **Why 池上限 200？** 200 个 Processor 同时活跃意味着 200 个并发 HTTP 请求在解析。超过 200 — 说明请求积压 — 再缓存更多 Processor 也没用 — 瓶颈在 Endpoint 的线程池。Pool 上限是反压信号 — 不是硬件限制。 [模式: Object Pool]

数据流: 请求到达 `ConnectionHandler.process(wrapper, event)`→`processor = recycledProcessors.pop()`→pop 返回 null(池空)→`processor = createProcessor()`(new Http11Processor)→`register(processor)`(JMX)→`processor.setSslSupport(wrapper.getSslSupport())`→`state = processor.process(wrapper, event)`→返回 OPEN→`release(processor)`→`processor.recycle()`→`recycledProcessors.push(processor)`→`wrapper.registerReadInterest()`→下一请求→pop 返回缓存的 processor→...。

### 3. 升级与异步 — Processor 的特殊状态路径

场景: HTTP 连接升级到 WebSocket — Processor 不能再处理普通的 HTTP 请求 — 它需要创建 `UpgradeProcessor`(WebSocket 协议专用)。或者异步请求 — 请求交给业务线程 — Processor 不阻塞等待 — 通过 `waitingProcessors` 集合在超时时回调。

源码路径:
- `AbstractProtocol.java:907-962` — **UPGRADING 状态**: 获取 UpgradeToken→释放 Http11Processor→创建 upgrade processor→初始化 HttpUpgradeHandler→设置 ASYNC_IO 状态→do-while 循环升级可能多步
- `AbstractProtocol.java:86` — `ConcurrentHashMap.newKeySet() waitingProcessors` — 异步 Processor 集合
- `AbstractProtocol.java:655-672` — `startAsyncTimeout()` — monitorFuture 每秒调用，遍历 waitingProcessors 调用 `processor.timeoutAsync(now)`

关键设计: **Why do-while for UPGRADING?** WebSocket 升级可能需要多次握手(HTTP Upgrade→WebSocket 帧协议)。do-while 保证`一次升级不成功→继续尝试→直到 CLOSED/UPGRADED`。

数据流: `processor.process()` 返回 `SocketState.UPGRADING`→`upgradeToken = processor.getUpgradeToken()`→`release(processor)`(释放旧的 Http11Processor)→`newUpgradeProcessor = createUpgradeProcessor(wrapper, upgradeToken)`→`newUpgradeProcessor.process(wrapper, status)`→返回 `SocketState.UPGRADED`→退出 do-while→`wrapper.setCurrentProcessor(upgradeProcessor)`→之后该连接的所有事件都由 upgradeProcessor 处理。

→ 引出 §3 CoyoteAdapter — Processor 把字节流变成了 coyote.Request — 调了 `adapter.service(request, response)` — CoyoteAdapter 拿到请求后做什么？怎么把 coyote 的 Request 变成 Engine 能理解的 catalina Request？
