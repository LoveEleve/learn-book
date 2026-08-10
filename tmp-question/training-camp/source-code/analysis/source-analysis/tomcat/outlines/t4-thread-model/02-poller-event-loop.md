# T-4 §2 Poller 内部机制 — Selector 事件循环 + keep-alive + 缓存池

> 依赖 §1 | 🔴 Deep | 3 KP | [模式: Object Pool + Double-Checked Wakeup]

**读者处境**: 理解了三线程分工 — 但 Poller 内部 `selector.select(selectorTimeout)`、`selectedKeys` 迭代、PollerEvent 缓存、keep-alive 复用 — 这些是决定 Tomcat 在 1000 并发下能否低延迟响应的关键。

### 1. Poller.run() 完整事件循环 — 三步循环

场景: Poller 启动后进入无限循环 — 三步: 1)处理注册事件(新连接注册到 Selector) 2)select() 等待 I/O 事件 3)遍历 selectedKeys 处理可读/可写事件。如果没有任何事件 — `selector.select(1000)` 等待 1000ms 后自动返回(处理超时)。

源码路径:
- `NioEndpoint.java:741` — **Poller.run()**: 完整的循环体
- `NioEndpoint.java:749` — `hasEvents = events()` — 处理注册事件队列
- `NioEndpoint.java:753-755` — `keyCount` 选择: `hasEvents>0`→`selectNow()`(非阻塞) / `hasEvents==0`→`select(selectorTimeout)`(阻塞等待)
- `NioEndpoint.java:780-790` — `selectedKeys` 迭代 + `processKey()`

关键设计: **Why hasEvents 影响 select 模式？** 如果有注册事件 — 说明 Acceptor 正在提交新连接 — 这些连接需要尽快注册到 Selector — `selectNow()` 非阻塞快进到 processKey。如果没有注册事件 — `select(1000)` 等待 I/O 事件 — 同时作为 Poller 的心跳(处理 keep-alive 超时)。这避免了"注册事件排在新连接后面等 1000ms"的延迟。

数据流: Poller.run() 循环→`hasEvents = events()`→处理 events 队列(新连接注册 OP_READ)→`keyCount = hasEvents ? selectNow() : select(1000)`→`selectedKeys = selector.selectedKeys().iterator()`→for each key: `sk.readyOps()`→`OP_READ`→`processSocket(wrapper, OPEN_READ)`→`OP_WRITE`→`processSocket(wrapper, OPEN_WRITE)`→sk.cancel()→继续下一个 key。

### 2. PollerEvent 缓存池 — 避免 GC 抖动

场景: Tomcat 每秒 10000 个请求 — keep-alive 连接每次有新请求→Poller 需要创建 PollerEvent 重新注册 OP_READ — 每秒 10000 次 `new PollerEvent()` → GC 压力。Tomcat 的方案: PollerEvent 对象缓存池 — 用 `SynchronizedStack` 存储已回收的 PollerEvent — 新连接先尝试从池取 — 池空才 new。

源码路径:
- `NioEndpoint.java:559` — **PollerEvent 类**: `cacheable` 注释 — NioSocketWrapper + OP_READ/OP_WRITE 封装
- `NioEndpoint.java:635-644` — **createPollerEvent()**: `eventCache.pop()`→null→`new PollerEvent()` / 非null→`event.reset(wrapper, interestOps)` 复用
- `NioEndpoint.java:97` — **eventCache** 字段: `SynchronizedStack<PollerEvent>` 类型

关键设计: **Why 缓存 PollerEvent 而不是 SocketProcessor？** PollerEvent 是轻量对象(只有 NioSocketWrapper + int interestOps — 约 24 字节)。但创建频率极高(每个读/写事件一个) — 缓存避免了 TLAB（Thread Local Allocation Buffer）的频繁分配。SocketProcessor 也是缓存的 — 但它在 Executor 层。两处缓存都是为高并发设计的。[模式: Object Pool]

数据流: Poller 需要注册新 OP_READ→`PollerEvent pe = createPollerEvent(wrapper, OP_READ)`→`eventCache.pop()`→有→`pe.reset(wrapper, OP_READ)` 重置字段→`addEvent(pe)`→PollerEvent 使用完毕→回收: 放入 `eventCache.push(pe)`→下次复用。无缓存→`new PollerEvent(wrapper, OP_READ)`→用完→GC 回收。

### 3. keep-alive 连接复用 — 从 CLOSE 到 OPEN 的轮回

场景: T-2 学过 — Processor 处理完请求返回 `SocketState.OPEN` — ConnectionHandler 调用 `release(processor)` → `wrapper.registerReadInterest()` → Poller 重新注册 OP_READ — 同一个连接上的下一个 HTTP 请求可以被 Poller 再次检测。这是 keep-alive 的基础 — 一次连接 N 次请求。

源码路径: 
- `AbstractProtocol.java:974-979` — ConnectionHandler 处理 OPEN 状态 — 调用 `wrapper.registerReadInterest()` — Poller 注册 OP_READ
- `Poller.addEvent(new PollerEvent(wrapper, OP_REGISTER))` — 重新注册

关键设计: **Why Poller 不直接监听从 `CLOSED` 到 `OPEN` 的转换？** TCP 连接的状态机在操作系统内核中 — Java NIO 的 Selector 不能监听"连接关闭"事件(Java 9+ 的 JPMS 对 sun.nio.ch 的限制更严格)。Tomcat 退而求其次: SocketWrapper 记录状态 — Poller 处理 OPEN 时重新注册 — 保持"请求→处理→keep-alive→再请求"的循环。

→ 引出 T-5 Mapper 路由 — 三线程模型和 Poller 内部机制解释了"请求怎么被接收和调度" — 线程池中的 Worker 线程执行 T-3 的 Pipeline.invoke() — invoke() 内部第一步需要从 URL 找到 Host/Context/Wrapper 三者 — 这是 Mapper 的职责。
