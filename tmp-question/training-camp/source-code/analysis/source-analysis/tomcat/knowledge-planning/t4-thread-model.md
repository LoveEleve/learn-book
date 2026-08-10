# T-4 线程模型 — 知识规划

> 项目: Tomcat 10.1.x | 类型: **规范参考实现** (Servlet 6.0 / Jakarta EE)
> 核心文件: NioEndpoint(1787行)/AbstractEndpoint(1590行)/Acceptor(231行)/SocketWrapperBase(1521行) | 🟡B 域 / 预计 2-3 篇
> 基线: T-3 Pipeline — 请求处理逻辑在 Valve 中执行 — T-4 回答 "这些 invoke() 在哪个线程执行？"

---

## §0.8 域审核前置

- 核心类: 4(NioEndpoint/AbstractEndpoint/Acceptor/SocketWrapperBase+Poller内部类+SocketProcessor内部类)
- 5129 行 ≤ 5000? 轻微超标 — NioEndpoint 含 Poller+SocketProcessor 两个内部类 → 仍合理，不拆分
- 淘汰检查: 无 — NIO 线程模型是 Tomcat 的核心，Spring Boot 不做替代

---

## 01 提取 — 逐源映射

### Acceptor.java (231 行 — 接收连接线程)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Acceptor.java:68 | **run()**: while(!stopCalled) accept loop — 阻塞等待新连接 | High |
| Acceptor.java:114-116 | **countUpOrAwaitConnection()**: 连接数达到 maxConnections→Acceptor 阻塞等待，不 accept 新连接 — 反压机制 | High |
| Acceptor.java:75-107 | **pause 机制**: endp.isPaused()→自适应等待(<1ms tight/1-10ms sleep 1/ >10ms sleep 10) — 优雅暂停 | High |

### NioEndpoint.java (1787 行 — NIO I/O + Poller + SocketProcessor)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| NioEndpoint.java:595-598 | **Poller 内部类**: `Selector selector` + `SynchronizedQueue<PollerEvent> events` — Poller 持有 Selector 和事件队列 | High |
| NioEndpoint.java:741 | **Poller.run()**: `hasEvents = events()` 处理注册事件 → `selector.select(selectorTimeout)` 等待 I/O 事件 → 遍历 selectedKeys → processKey() | High |
| NioEndpoint.java:672 | **events()**: 处理 events 队列中的 PollerEvent — 将新连接注册到 Selector | High |
| NioEndpoint.java:628-632 | **addEvent()**: `events.offer(event)` + `wakeupCounter` CAS — 高效唤醒 Selector | High |
| NioEndpoint.java:801 | **processKey()**: 处理 SelectionKey — `OPEN_READ`→processSocket / `OPEN_WRITE`→processSocket | High |
| NioEndpoint.java:1683 | **SocketProcessor**: Runnable 包装 SocketWrapperBase+SocketEvent — 提交给 Executor 线程池 | High |
| NioEndpoint.java:559 | **PollerEvent**: cacheable 对象 — 避免 GC | High |

### AbstractEndpoint.java (1590 行 — 端点抽象基类)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| AbstractEndpoint.java:548 | **maxConnections=8192** — 最大连接数限制 | High |
| AbstractEndpoint.java:590 | **Executor executor** — 外部可注入线程池(Spring Boot 默认 `ThreadPoolExecutor(10,200,60s)`) | High |
| AbstractEndpoint.java:178 | **internalExecutor=true** — 外部未注入时自动创建线程池 | High |

### SocketWrapperBase.java (1521 行 — Socket 包装器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| SocketWrapperBase | **SocketWrapper \<SocketChannel\>** — 持有 SocketChannel + NioChannel，封装 I/O 读写操作 | High |

---

## 02 聚合

### P1
- **Acceptor→Poller→Worker 三线程模型**: Acceptor/NioEndpoint/AbstractEndpoint 全部涉及

### P2
- **PollerEvent 缓存池**: NioEndpoint 内部复用机制
- **maxConnections 反压**: AbstractEndpoint + Acceptor

---

## 03 深度分类

| KP | 级别 | 理由 |
|------|:--:|------|
| **Acceptor→Poller→Worker 三线程模型** | 🔴 Deep | Tomcat NIO 架构的核心 — 理解"为什么不是一请求一线程" |
| **Selector 事件循环(Poller.run)** | 🔴 Deep | NIO 的基础 — `select()→processKey→processSocket→executor` 全链路 |
| **maxConnections + pause 反压** | 🟡 Working | 生产关键但概念简单 |
| **PollerEvent/SocketProcessor 缓存池** | 🟡 Working | 性能优化 |

---

## 04 聚类

**Cluster A: 三线程模型 — Acceptor→Poller→Worker** (→ §1)
- Tomcat NIO 的三段式: TCP accept→Selector 事件→线程池执行
- 对比: Netty Reactor(Boss/Worker ThreadGroup) — 同为事件驱动，实现不同
- 配置: Spring Boot `server.tomcat.threads.max=200` / `server.tomcat.accept-count=100`
- 规范对应: `AsyncContext`(3.0) — Worker 线程可释放而请求不结束

**Cluster B: Poller 事件循环 + Selector wakeup** (→ §2)
- Poller.run(): events→select→processKey→processSocket→executor
- wakeupCounter 优化: CAS 判断是否需要 `selector.wakeup()`
- PollerEvent 缓存池: 避免频繁 new
- 规范对应: `ReadListener/WriteListener`(3.1) — 非阻塞 I/O 回调

> → 引出 T-5 Mapper 路由 — 请求已经在线程中执行 `pipeline.invoke()`，但 `invoke()` 内部第一步是 `engine.getPipeline()` → 查 Host → Host Pipeline → 查 Context → Context Pipeline → 查 Wrapper。这个"查"是怎么做的？Mapper 如何从 URL 找到 Host/Context/Wrapper 三者？
