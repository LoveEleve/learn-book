# T-3 §4 Tomcat Pipeline vs Netty Pipeline — CoR 的两种实现哲学

> 依赖 §3 | 🟡 Working | Netty Ch7 对照 | 架构对比文章

**读者处境**: 这是 Stage 1(I/O 基础)的最后一个 Netty↔Tomcat 对照章节。学完 Netty Ch7 Pipeline 和 Tomcat T-3 Valve+Filter — 两者都是 Chain of Responsibility — 为什么设计差异如此之大？

### 1. ChannelPipeline vs Pipeline — 结构对比

场景: Netty 中 `pipeline.addLast("myHandler", new MyHandler())` — 往 Channel 的 Pipeline 加 Handler。Tomcat 中 `engine.getPipeline().addValve(new AccessLogValve())` — 往 Engine 的 Pipeline 加 Valve。操作看起来一样 — 但底层链结构完全不同。

源码路径:
- Netty: `DefaultChannelPipeline` — 双向链表(AbstractChannelHandlerContext.prev/next) — Head↔Tail Sentinel 节点包裹链的两端 — Head 处理 Outbound(写出)，Tail 处理 Inbound(读入)
- Tomcat: `StandardPipeline` — 单向链表(Valve.next) — basic Valve 始终在末尾 — 没有 Sentinel 节点 — getFirst() 返回链头(可能是 addValve 或 basic)

关键设计:

| 维度 | Netty ChannelPipeline | Tomcat Pipeline |
|------|------|------|
| 链结构 | 双向链表 + Head↔Tail Sentinel | 单向链表 + basic 终端 |
| 传播方向 | **双向**: Inbound(read)/Outbound(write) | **单向**: 只从 first→basic |
| 处理器接口 | ChannelInboundHandler + ChannelOutboundHandler | Valve.invoke() |
| 传递方式 | `ctx.fireChannelRead(msg)` — 事件穿透+查找下一 Inbound Handler | `getNext().invoke()` — 显式传递 |
| 短路机制 | Handler 不调 fireXxx() → 停止 | Valve 不调 getNext().invoke() → 停止 |
| 生命周期 | Handler 在 Channel 生命周期内持续存在 | Valve 在 Container 生命周期内持续存在 |

### 2. 双向 vs 单向 — 为什么 Tomcat 不需要 Outbound？

场景: Netty 的 Outbound 处理: `ctx.write(msg)` → Tail → 上一个 Outbound Handler → ... → Head → write 到 Socket。Tomcat 的 Response 写入直接通过 `response.getOutputStream().write()` → OutputBuffer → coyote.Response → SocketOutputStream — **不走 Pipeline**。

关键设计: **Tomcat Pipeline 只处理请求路由，不处理响应写入**。Response 在 Adapter→Engine Pipeline 之间已经创建完毕 — Valve 可以通过 `response.sendError()` 等修改 Response — 但 Response 的字节写入是 `servlet.service()` 完成后 — 由 Adapter.finishResponse() → OutputBuffer.close() → coyote.Response 完成的 — 不走 Valve 链。Netty 的 Handler 可以同时处理 Inbound(读到数据)和 Outbound(写入数据) — 因为它工作在更底层(字节流层)

架构意图: **职责分离** — Tomcat 把"请求路由"(Pipeline→Valve→FilterChain→Servlet)和"响应输出"(OutputBuffer→Coyote Response→Socket)分成两条独立的路径 — 不再像 Netty 那样在一个 Pipeline 中同时处理。这是历史产物: Tomcat 的 Pipeline 设计于 1999(Servlet 2.2) — Netty 的 Pipeline 设计于 2004 — 4 年后重新思考 CoR 在 I/O 框架中的最优实现。

### 3. Netty Handler vs Tomcat Valve — 两种处理器的接口差异

场景: Netty 用户写 `public class MyHandler extends ChannelInboundHandlerAdapter` — channelRead 方法签名 `void channelRead(ChannelHandlerContext ctx, Object msg)` — ctx 使得 Handler 能访问 Channel/Pipeline。Tomcat 用户写 `public class MyValve extends ValveBase` — invoke 方法签名 `void invoke(Request request, Response response)` — request/response 使得 Valve 能访问请求数据。

关键设计:

| 维度 | Netty Handler | Tomcat Valve |
|------|------|------|
| 上下文 | ChannelHandlerContext(封装了 Channel+Handler+绑定关系) | 无 ctx — 通过 request.getConnector() 间接访问 |
| 参数 | Object msg(泛型消息 — 不限定类型) | Request+Response(限定 HTTP 请求/响应) |
| 状态 | Handler 可用 `ctx.attr()` 存状态(AttributeKey) | Valve 无自带状态 — 通过 Pipeline 的外部容器管理 |
| 堆栈 | Inbound 堆栈(channelRead→channelReadComplete→...) | 单方法 invoke — 无事件分拆 |

**Why Tomcat Valve 没有 ctx？** 因为 Valve 在 Servlet 容器中 — 请求已经解析为 Request 对象 — Valve 不需要 ctx 来访问"当前连接" — 直接操作 Request/Response 即可。

### 4. 学了这两个 Pipeline 之后 — 再看 Dubbo/gRPC 的 Pipeline

场景: T-1/T-2/T-3 学完了 Tomcat 的请求处理全链路 — Netty Ch1-Ch14 学完了 Netty 的 I/O 基础。Stage 5 要做 gRPC 和 Dubbo — 它们的 Pipeline 是某种混合体: Dubbo 用 Netty ChannelPipeline 做传输 — 又在上面加了自己的 Filter 链(Dubbo Filter) — 这和 Tomcat 的 Valve+Filter 双链异曲同工。gRPC 用 Netty HTTP/2 Codec 做传输 — 又在上面加了 ClientInterceptor 链 — 又一个双链。

关键设计: **双链模式是 RPC 框架的共同选择** — Netty/Tomcat→传输层链 + Filter/Interceptor→业务层链。传输层关心: I/O 事件(read/write/connect)、心跳、序列化。业务层关心: 认证、限流、监控、日志。双链让两类关注点在代码中分离 — 不互相污染。

数据流: Dubbo `Consumer.invoke()` → Filter链(MonitorFilter/ExceptionFilter/...) → Netty ChannelPipeline(Encoder → WriteHandler → Socket) → 服务端 Netty ChannelPipeline(ReadHandler → Decoder) → Dubbo Filter链(TraceFilter/TimeoutFilter/...) → 业务实现。**一条请求在Netty Pipeline(2层)+Dubbo Filter(2层)之间穿行** — 这就是双链模式在 RPC 框架中的典型应用。

→ 引出 T-4 线程模型 — Pipeline 解决了"请求怎么处理" — 但所有这些 invoke() 在哪个线程执行? Acceptor→Poller→Worker 三线程模型如何与 Pipeline 执行链配合?
