# T-2 §3 CoyoteAdapter — 两次对象世界的转换器

> 依赖 §2 | 🔴 Deep | 4 KP | [模式: Adapter]

**读者处境**: Processor 调了 `adapter.service(coyoteRequest, coyoteResponse)` — 但 coyote 的 Request 和 T-1 讲的 catalina Request 是两个不同的类。为什么要有两个 Request？答案在 Adapter: coyote Request 是协议层(字节流→HTTP 头)，catalina Request 是 Servlet 层(addCookie/getSession/forward)。

### 1. service() — 请求从协议世界进入容器世界

场景: 一个 HTTP 请求到达 — `GET /app/user?id=1 HTTP/1.1\r\nHost: example.com\r\n` — Processor 已解析出 Method=GET、URI=/app/user、Host=example.com — 现在它需要把这三条信息传递给 Engine Pipeline。

源码路径:
- `CoyoteAdapter.java:303` — `service(coyote.Request req, coyote.Response res)` — Adapter 入口
- `CoyoteAdapter.java:305-306` — **Notes 缓存复用**: `req.getNote(ADAPTER_NOTES)` — 同一连接上的下一个请求不需要重新创建 catalina Request/Response — 从 Notes 取出上次的→`recycle()` 重置→复用
- `CoyoteAdapter.java:309-313` — **首次创建**: `connector.createRequest()` 创建 catalina Request→`request.setCoyoteRequest(req)` 注入→`response.setCoyoteResponse(res)` — 建立 coyote↔catalina 的双向映射
- `CoyoteAdapter.java:344` — **关键桥接线**: `connector.getService().getContainer().getPipeline().getFirst().invoke(request, response)` — T-2↔T-1 交汇

关键设计: **Notes 缓存 — 为什么不用 ThreadLocal？** 一个连接(非阻塞 I/O)可能在不同 Poller 线程间迁移 — ThreadLocal 失效。Notes 是 Coyote Request/Response 对象的 HashMap<String,Object> — 连接级别的缓存 — 不受线程切换影响。复用避免了 `new catalina.Request()` 的 GC 压力 — keep-alive 的 100 个请求只创建一次 catalina Request。 [模式: Adapter — 将 coyote.Request 适配为 catalina.Request，使协议层和容器层独立变化]

→ 实现规范: `ServletRequest`(1.0) → 通过 Connector.createRequest() 创建 → `RequestFacade` 包装 catalina.Request。

数据流: Processor 调用 `adapter.service(coyReq, coyRes)`→`catalinaReq = (Request) req.getNote(1)`→null(首次)→`catalinaReq = connector.createRequest()`→`catalinaReq.setCoyoteRequest(coyReq)`→`catalinaRes = connector.createResponse()`→`catalinaRes.setCoyoteResponse(coyRes)`→`req.setNote(1, catalinaReq)`→`res.setNote(1, catalinaRes)`→`postParseRequest(coyReq, catalinaReq, coyRes, catalinaRes)`→解析 URI+Host+Session→`request.setAsyncSupported(...)`→`connector.getService().getContainer().getPipeline().getFirst().invoke(catalinaReq, catalinaRes)`→返回→`request.finishRequest()`+`response.finishResponse()`→`request.recycle()`+`response.recycle()`→**不设 null — 保留在 Notes 中供下一请求复用**。

### 2. postParseRequest — URI→Mapper→Host/Context/Wrapper 的路径

场景: `GET /app/user?id=1 HTTP/1.1\r\nHost: example.com` — 需要知道 "/app" 对应哪个 Context、"example.com" 对应哪个 Host、"/user" 对应哪个 Wrapper。`postParseRequest()` 完成三步: URI 安全校验→调用 Mapper 映射→Session ID 解析。

源码路径:
- `CoyoteAdapter.java:622-667` — **URI 处理管线**: 检查 `suspiciousURI`(编码点段攻击 %2e)→`decodedURI` 复制→`parsePathParameters` 剥离 `;JSESSIONID=xxx`→`URLDecoder` %xx 解码→`normalize()` 归一化(/./、/../、//)→`convertURI()` 字符编码转换(UTF-8→目标编码)
- `CoyoteAdapter.java:697` — **Mapper 映射**: `connector.getService().getMapper().map(serverName, decodedURI, version, request.getMappingData())` — 一次调用填充 mappingData.host + mappingData.context + mappingData.wrapper + redirectPath + welcomeFiles
- `CoyoteAdapter.java:716-738` — **Session ID 三层回退**: URL 路径参数(;JSESSIONID=)→Cookie(JSESSIONID)→SSL Session ID

关键设计: **Why Mapper.map() 返回 void 而非返回 MappingResult？** 因为 Mapper 不知道返回什么结构 — 它填充 `request.getMappingData()` 的 5 个字段。这种 "传入可变对象→填充" 的模式避免了额外的对象创建 — MappingData 被缓存在 Request 中 — `recycle()` 时清零。这是性能敏感的零分配设计。

架构意图: **分层安全** — URI 安全校验在 Adapter 层做 — 不依赖容器层。即使 Engine/Host/Context 错误的接受了恶意 URI — Adapter 已经拦截了。这是 Tomcat 安全架构的 defense-in-depth 原则。

数据流: `postParseRequest()`→URI decode+normalize→`version.parseRequestURI(decodedURI)` 提取 HTTP 版本→`serverName = req.serverName().toString()`(从 Host header)→`connector.getService().getMapper().map(serverName, decodedURI, version, mappingData)`→mappingData.host="example.com" Host 容器引用→mappingData.context="/app" Context 容器引用→mappingData.wrapper="user" Wrapper 容器引用→返回 true→`request.setAsyncSupported(...)`→进入 Pipeline。

### 3. 代理、安全、特殊请求 — postParseRequest 的周边防线

场景: 生产环境 — Nginx 反向代理在 Tomcat 前面 — Host header 被 Nginx 改为代理地址 — Tomcat 收到的 Host 不是客户端的原始 Host。或者攻击者发送 `%2e%2e/`(编码后的 "../") 试图穿越目录。

源码路径:
- `CoyoteAdapter.java:578-592` — **proxyName/proxyPort**: 若设置了 `proxyName`(如 "example.com")→覆盖 Host header — 反向代理场景
- `CoyoteAdapter.java:622-626` — **suspiciousURI 检测**: `connector.getRejectSuspiciousURIs()` 控制是否拒绝含 `%2e`(编码的 `.` )的 URI
- `CoyoteAdapter.java:597-612` — **OPTIONS * 短路**: `*` 请求→返回 `Allow: GET, POST, ...` → 直接返回，不走 Pipeline
- `CoyoteAdapter.java:617-618` — **CONNECT 拒绝**: HTTP CONNECT 返回 501 — Tomcat 原生不支持代理模式
- `CoyoteAdapter.java:815-840` — **TRACE 过滤器**: `!connector.getAllowTrace()` → 返回 405 → Wrapper 支持的 methods 列表

关键设计: **Why 特殊请求短路不走 Pipeline？** OPTIONS * 和 CONNECT 不是普通的 Web 应用请求 — 路由到 Context/Wrapper 没有意义。短路处理避免了: 1) 创建 mappingData(空映射) 2) 进入 Engine Pipeline 再被 StandardEngineValve 拒绝 3) 浪费一条完整的 Pipeline 调用链。

→ 实现规范: `proxyName/proxyPort` — Servlet 规范未定义，属于 Tomcat 部署特性。`rejectSuspiciousURIs` — Tomcat 安全增强。

数据流: 反向代理: `request.getServerName()` 返回 `connector.getProxyName()`(若设置) 否则 `req.serverName().toString()` — Servlet 的 `request.getServerName()` 看到的是代理配置的域名非物理主机名。

→ 引出 §4 Request/Response — coyote↔catalina 两层委托的具体机制: 怎么实现 `catalinaRequest.getParameter("id")` 最终调到 `coyoteRequest`？`catalinaResponse.getOutputStream().write()` 怎么写到 Socket？
