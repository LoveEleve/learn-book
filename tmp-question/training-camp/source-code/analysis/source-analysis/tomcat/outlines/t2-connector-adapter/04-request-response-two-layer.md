# T-2 §4 Request/Response — coyote↔catalina 两层委托 + 对象复用

> 依赖 §3 | 🟡 Working | 4 KP | 规范映射表

**读者处境**: CoyoteAdapter 把 request/response 交给了 Engine Pipeline — 但 Servlet 代码里写的是 `request.getParameter("id")` — 这个 `request` 是什么类型？它怎么拿到 "id=1" 的？答案在两层委托: catalina.connector.Request(上层) → coyote.Request(下层) → Socket 字节流。

### 1. catalina.connector.Request — 上层语义 + Connector 通道

场景: `request.getServerName()` — Servlet 规范要求返回 Host header 的值。Catalina Request 自己不知道 Host — 它从 coyote.Request 获取 — coyote.Request 从 HTTP 报文解析 Host header。

源码路径:
- `Request.java:153` — `protected coyote.Request coyoteRequest` — 所有数据来自协议层
- `Request.java:539` — `final Connector connector` — Request 可通过 Connector 访问全局配置(端口/编码/安全策略)
- `Request.java:608-609` — `MappingData mappingData` — 缓存 Mapper 映射结果，T-1 容器引用: host/context/wrapper
- `Request.java:160-163` — `setCoyoteRequest()` — 建立委托关系并通知 InputBuffer
- `Request.java:431-504` — `recycle()` — 43 行状态重置，涵盖全部字段(authType/session/mappingData/asyncContext 等)

关键设计: **Why catalina.Request 不直接持有一个 Connector 字段就够了？** 因为 Servlet 的 `request.getAttribute("javax.servlet.request.ssl_session")` 需要访问 SSL 握手信息 — 这在 Coyote 层的 `Request.getAttribute()` 中。委托链: `catalina.request.getAttribute(key)`→无→`coyoteRequest.getAttribute(key)` — 两层查找确保上层语义不重写底层协议信息。

→ 实现规范: `HttpServletRequest`(1.0) — `RequestFacade` 包装 catalina.Request。Facade 模式防止应用代码绕过安全限制直接调 `request.recycle()` — 只暴露 Servlet 规范方法。

数据流: `request.getServerName()`→判断 connector.getProxyName() 是否设置→有→返回 proxyName→无→`coyoteRequest.serverName().toString()`→返回 Host header→`request.getContextPath()`→`mappingData.contextSlashCount` 从 URI 推导→验证 canonicalContextPath→返回 "/app"。

### 2. catalina.connector.Response — OutputBuffer + OutputStream/Writer 互斥

场景: Servlet 代码 `response.getWriter().write("hello")` — 字符 "hello" 需要变成 HTTP 响应报文 `HTTP/1.1 200 OK\r\nContent-Length: 5\r\n\r\nhello`。这之间经过: Writer→OutputBuffer→coyote.Response→SocketOutputStream。

源码路径:
- `Response.java:100` — `protected coyote.Response coyoteResponse` — 所有写操作最终到协议层
- `Response.java:131` — `OutputBuffer outputBuffer` — 响应数据的主缓冲区
- `Response.java:167,173` — `usingOutputStream` / `usingWriter` 互斥标志 — 同一响应不可同时用
- `Response.java:501-512` — `getOutputStream()`: 检查 usingWriter→false→set usingOutputStream=true→懒初始化 CoyoteOutputStream
- `Response.java:421-424` — `finishResponse()` → `outputBuffer.close()` — 刷出缓冲
- `Response.java:202-230` — `recycle()` — cookies/outputBuffer/标志/stream/writer 全量重置

关键设计: **Why OutputStream vs Writer 互斥？** 同时使用会破坏 HTTP 响应 — `getOutputStream()` 写的是字节(二进制) — `getWriter()` 写的是字符(文本) — 如果混用 — OutputStream.write() 之后 Writer 再 write() — 字符编码和字节边界不一致 — 响应体损坏。Tomcat 在 `getOutputStream()` 中设置 `usingOutputStream=true` — `getWriter()` 检查此标志 — 已设→抛 `IllegalStateException`。 [模式: Mutual Exclusion Guard]

→ 实现规范: `HttpServletResponse`(1.0) — `ResponseFacade` 包装 catalina.Response。`getOutputStream()`/`getWriter()` 互斥 → Servlet 规范 §5.1。

数据流: Servlet 调 `response.getWriter()`→检查 `usingOutputStream`=false→`usingWriter=true`→创建 `CoyoteWriter(outputBuffer)`→`writer.write("hello")`→`outputBuffer.write("hello".getBytes(charset))`→写入缓冲区→`response.flushBuffer()`→`outputBuffer.flush()`→`outputBuffer.realWriteBytes()`→`coyoteResponse.doWrite(byteBuffer)`→写入 SocketOutputStream→TCP 发送。

### 3. recycle — 对象池的清洁工

场景: keep-alive 连接 — 第一个请求 GET /user→200 OK→下一个请求 GET /app→404。第二个请求如果复用了第一个的 catalina.Request — mappingData 里还残留着 /user 的 Context 引用 — 返回的 contextPath 是 "/user" 而非 "/app" — 功能错误。`recycle()` 必须在 Adapter.service() 返回前被调用。

源码路径:
- `CoyoteAdapter.java:420-424` — 同步请求结束时 recycle Request + Response
- `Request.java:431-504` — Request.recycle(): 重置 dispatcherType/authType/inputBuffer/streams/reader/userPrincipal/parts/locales/remote-addr/sessionInfo/cookieInfo/mappingData/parameters/asyncContext/facade — **43 行重置代码**
- `Response.java:202-230` — Response.recycle(): 清理 cookies/outputBuffer.recycle()/using*标志/appCommitted/writer/outputStream

关键设计: **Why recycle 代码 43 行 — 不直接 new 一个？** 因为 `new catalina.Request(connector)` → 创建 InputBuffer → 关联 coyoteRequest → 设置 cookie 处理器 — 一套完整的初始化链路 — ~1ms。`recycle()` 遍历所有字段置为 null/0/false — ~0.05ms。池化复用省了 20 倍时间 — keep-alive 性能的基础。

数据流: `CoyoteAdapter.service()`→`request.recycle()`→dispatcherType=null/authType=null→inputBuffer.recycle()→recycleSessionInfo()→recycleCookieInfo(false)→mappingData.recycle()(host/context/wrapper=null)→parameters.recycle()→asyncContext=null→facade=null→response.recycle()→outputBuffer.recycle()→cookies.clear()→usingOutputStream=false/usingWriter=false→appCommitted=false→**Notes 中保留**→下次 `req.getNote(ADAPTER_NOTES)` 返回已回收的 catalina.Request→`setCoyoteRequest(newCoyoteReq)` 建立新的委托→复用完成。

### 4. 规范映射总表

| 规范接口 (Jakarta Servlet 6.0) | Tomcat 实现类 | 引入版本 | 层 |
|------|------|:--:|:--:|
| `jakarta.servlet.http.HttpServletRequest` | `RequestFacade` (包装 catalina.connector.Request) | 1.0 | catalina |
| `jakarta.servlet.http.HttpServletResponse` | `ResponseFacade` (包装 catalina.connector.Response) | 1.0 | catalina |
| `jakarta.servlet.ServletInputStream` | `CoyoteInputStream` (包装 InputBuffer) | 1.0 | catalina |
| `jakarta.servlet.ServletOutputStream` | `CoyoteOutputStream` (包装 OutputBuffer) | 1.0 | catalina |
| `jakarta.servlet.ServletConnection` | Connector 提供连接信息 | 6.0 | Connector |
| `jakarta.servlet.AsyncContext` | `AsyncContextImpl` (CoyoteAdapter.asyncDispatch) | 3.0 | Adapter |

→ 引出 T-3 Pipeline+双链 — Request/Response 通过 Adapter 进入了 Engine Pipeline。`getPipeline().getFirst().invoke(request, response)` 的第一个 Valve 是 StandardEngineValve。Valve 链包含: StandardEngineValve→StandardHostValve→StandardContextValve→StandardWrapperValve — 每个 Valve 做什么？ApplicationFilterChain(Filter 链) 在 StandardWrapperValve 中怎么被调用？T-1 预留的 Pipeline 字段 — T-3 展开它的完整执行流程。
