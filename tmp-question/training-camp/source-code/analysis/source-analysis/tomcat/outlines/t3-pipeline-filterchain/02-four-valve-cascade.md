# T-3 §2 4 Valve invoke 级联 — 从 Engine 到 Servlet

> 依赖 §1 | 🔴 Deep | 4 KP

**读者处境**: T-2 Adapter 调了 `engine.getPipeline().getFirst().invoke()` — 现在知道 getFirst() 返回 Pipeline 链的第一个 Valve — 对 Engine 就是 StandardEngineValve。但这个 Valve 的 invoke 做了什么？怎么挑选 Host？怎么一层层传到 Servlet？

### 1. StandardEngineValve — Host 选择器

场景: HTTP 请求的 Host 头是 "example.com" — Engine 下面有两个 Host 容器: "example.com" 和 "localhost"。StandardEngineValve 需要找到匹配的 Host — 把请求交给它的 Pipeline。

源码路径: `StandardEngineValve.java:56` — **invoke()**: 从 `request.getHost()` 获取 MappingData 中的 Host 引用 — 若 Host 为 null→尝试 `engine.findChild(engine.getDefaultHost())` →调用 `host.getPipeline().getFirst().invoke(request, response)` — **请求进入 Host Pipeline**。

关键设计: **Why Host 已经在 request.getHost() 中？** 因为在 T-2 的 `CoyoteAdapter.postParseRequest()` 中 — `mapper.map()` 已经把 Host: "example.com" → Host 容器引用设置了到 `request.getMappingData().host`。EngineValve 不需要重新查 — 直接从 request 取 — 这避免了 Engine 层的重复查找。T-2 做了路由 — T-3 只做分发。

数据流: `engine.getPipeline().getFirst().invoke()`(first=EngineValve)→`EngineValve.invoke()`→`request.getHost()`("example.com"的 Host 容器)→`host.getPipeline().getFirst().invoke()`→进入 Host Pipeline → StandardHostValve...

### 2. StandardHostValve — Context 选择器 + 错误页面

场景: URI 是 "/app/user" — Host 下面有两个 Context: "/app"(已部署)和 "/manager"(已部署)。HostValve 从 request 获取 Context 映射 — 未找到→生成 404 错误页面 — 找到→把请求交给 Context Pipeline。

源码路径: `StandardHostValve.java:80` — **invoke()**: 从 `request.getContext()` 获取 MappingData 中的 Context 引用 — 若 Context 为 null→`response.sendError(404)`→返回 — 否则 `context.getPipeline().getFirst().invoke(request, response)`。Context Pipeline 返回后——若 `response.isError()`→设置错误状态页→返回完整的 HTML 错误页面。

关键设计: **Why HostValve 做错误页面而不是 EngineValve？** 因为错误页面的路由与 Host 相关 — "example.com" 和 "localhost" 可能配置不同的 error-page。如果 EngineValve 做错误页面 — 它不知道哪个 Host 应该渲染错误 — 页面内容可能不对。**每一层 Valve 只处理自己容器的职责** — Engine 只管 Host 选择 — Host 管 Context 选择 + 错误页面 — Context 管 Wrapper 选择 — Wrapper 管 Servlet 执行。

数据流: `host.getPipeline().getFirst().invoke()`→HostValve.invoke()→`request.getContext()`("/app"的 Context 容器)→Context 非空→`context.getPipeline().getFirst().invoke()`→进入 Context Pipeline → StandardContextValve...

### 3. StandardContextValve → StandardWrapperValve — 最后两跳

场景: URI "/app/user" 中 — Context 是 "/app" — Wrapper 是 "user"(一个 Servlet)。ContextValve 从 request 获取 Wrapper — 检查是否有 WebSocket 冲突 — 把请求交给 Wrapper Pipeline — WrapperValve 创建 FilterChain→执行。

源码路径:
- `StandardContextValve.java:60` — **invoke()**: 从 `request.getWrapper()` 获取 Wrapper — 若 Wrapper 为 null→404 — 若 `context.getDispatcherType()==REQUEST` → 检查 WebSocket 路径冲突 → `wrapper.getPipeline().getFirst().invoke()`
- `StandardWrapperValve.java:86` — **invoke()**: wrapper.allocate() 获取 Servlet 实例 → `ApplicationFilterFactory.createFilterChain(request, wrapper, servlet)` → `filterChain.doFilter()`(下一章详讲) → wrapper.deallocate() 释放 Servlet 实例

关键设计: **Why StandardWrapperValve 是最后一个 Valve？** 因为 Servlet 是请求处理的终端 — Servlet 不需要再调 `getNext().invoke()` — 它直接生成 Response。WrapperValve 的职责就是创建 FilterChain 并执行 — FilterChain 内部可能不调 servlet.service()（如果 Filter 短路） — 但 WrapperValve 始终是 Pipeline 的 basic Valve(物理上的终端)。

数据流: `context.getPipeline().getFirst().invoke()`→ContextValve.invoke()→`request.getWrapper()`("user"的 Wrapper 容器)→wrapper 非空→`wrapper.getPipeline().getFirst().invoke()`→WrapperValve.invoke()→`wrapper.allocate()`(从实例池取 Servlet)→`ApplicationFilterFactory.createFilterChain(request, wrapper, servlet)`→`filterChain.doFilter()`→Filter1.doFilter()→Filter2.doFilter()→`servlet.service()`→生成 Response→返回→...→`wrapper.deallocate()`→返回→ContextValve 返回→HostValve 检查错误→返回→EngineValve 返回→Adapter finishResponse。

### 4. 级联本质 — 树 + 管道的双层递归

场景: 运维在 Engine/Host/Context 三层各加了 AccessLogValve — 现在一个请求会触发 3 个 AccessLogValve — 加起来 3 条日志？对的，这是 Tomcat 的设计: **每层容器独立 — 各自的 AccessLogValve 只记录该层的信息(**Engine 级别 — 全局请求量 / Host 级别 — 每个虚拟主机的请求 / Context 级别 — 每个 Web 应用的请求)。

关键设计: **树结构(Container)和链结构(Pipeline)是正交的** — 请求在 T-1 的 Container 树中垂直下降(Engine→Host→Context→Wrapper) — 每一层下降时经过 T-3 的 Pipeline 链水平展开(AccessLog→basic Valve→下一层 Pipeline)。Tree 决定路由路径 — Pipeline 决定处理栈 — 两者在 `container.getPipeline().getFirst().invoke()` 处交汇。

→ 引出 §3 Filter 双链 — WrapperValve 把请求交给了 `filterChain.doFilter()` — 这是另一个链: Filter 链。它和 Valve 链有什么不同? Filter 的 `chain.doFilter()` 和 Valve 的 `getNext().invoke()` 有什么区别?
