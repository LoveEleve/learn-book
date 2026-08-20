# C-15 WebFlux — 响应式 Web (DispatcherHandler → RouterFunction → WebClient)

> 依赖 C-14 @InitBinder | 🟡 Working | 6 KP | [模式: 函数式路由 + 响应式流 + 前端控制器]

**读者处境**: WebFlux 和 Spring MVC 什么关系？RouterFunction 和 @RequestMapping 区别？WebClient 和 RestTemplate 区别？为什么说 WebFlux 是非阻塞的？

### 1. DispatcherHandler — 响应式前端控制器的 Flux 分派链

场景: 请求进来 → 响应式框架怎么找到 handler 并执行？和 MVC 的 DispatcherServlet 一样有"映射→适配→结果"三步, 但全程是 Mono/Flux 声明式组合, 不阻塞线程。

源码路径:
- `DispatcherHandler.java:72,142` — **handle(exchange)**: L142 `Flux.fromIterable(handlerMappings)` → `.concatMap(mapping -> mapping.getHandler(exchange))`(逐个映射尝试, 响应式) → `.next()`(取第一个 handler) → `.switchIfEmpty(createNotFoundError())`(无匹配→404) → `.onErrorResume(handleResultMono)`(异常→结果处理器) → `.flatMap(handler -> handleRequestWith(...))`(执行 handler)
- `DispatcherHandler.java:75,81` — **组件**: handlerMappings(映射)/handlerAdapters(适配)/resultHandlers(结果处理) — 与 MVC 同构
- 对照: DispatcherServlet(阻塞, 同步方法) vs DispatcherHandler(非阻塞, 返回 Mono<Void>)

关键设计: **Why 用 Flux 声明式组合而非命令式遍历？** 非阻塞的关键: 每一步返回 Mono/Flux(可能未完成), 组合算子(concatMap/next/switchIfEmpty)在事件就绪时才推进, **不占用线程等待** — 高并发下少量线程即可; 命令式循环会阻塞线程池。[模式: 响应式流组合]

数据流: 请求 → DispatcherHandler.handle(exchange) → Flux: 遍历 handlerMappings → RouterFunctionMapping.getHandler 匹配 → 命中 RouterFunction → next() 取到 → handleRequestWith → HandlerAdapter.invokeHandler → HandlerFunction.handle → Mono<ServerResponse> → 结果处理器(ResponseBodyResultHandler)写响应 → Mono<Void> 完成。

### 2. RouterFunction + HandlerFunction — 函数式路由

场景: `RouterFunctions.route(RequestPredicates.GET("/user"), req -> ServerResponse.ok().body(user))` — 路由即函数组合, 不用 @Controller/@RequestMapping。

源码路径:
- `RouterFunctions.java:65,96,115` — **路由工厂**: L115 `route(predicate, handlerFunction)` → DefaultRouterFunction; `route()`(L96) 返回 Builder — `.GET("/x", handler).POST(...).andRoute(...)` 链式组合
- `RouterFunction.java`(162行) — **接口**: 组合路由 — 嵌套(nest 加前缀)/谓词匹配
- `HandlerFunction.java:30,37` — **处理函数**: `handle(ServerRequest request) → Mono<ServerResponse>` — 纯函数, 无类无注解

关键设计: **Why 函数式而非注解？** 路由与处理分离成数据(函数组合), 可动态构建/测试(不启动服务器即可断言路由); 注解是"类级+反射匹配", 函数式是"代码级组合" — 适合动态/非侵入场景, 但注解仍是 WebFlux 默认主流。[模式: 函数式路由组合]

数据流: GET /user → RouterFunctionMapping.getHandler → 遍历 routerFunction 链: 谓词 matches(GET, /user)→true → 返回 HandlerFunction(用户方法) → HandlerFunction.handle(ServerRequest) → ServerResponse.ok().body(Mono<User>) → 响应。

### 3. WebClient — 响应式 HTTP 客户端

场景: 服务间调用、消费外部 API — 非阻塞: `webClient.get().uri(...).retrieve().bodyToMono(User.class)` — 不阻塞线程等响应。

源码路径:
- `WebClient.java:80,86,98,145` — **接口**: get() L86/post() L98 链式 URI 构建; `create()` L145(DefaultWebClientBuilder)
- 响应消费: `retrieve()`(简化, 自动解码) / `exchangeToMono(Function)`(完整控制状态码/头) → `bodyToMono`/`bodyToFlux`(解码为响应式类型)
- 对照: RestTemplate(阻塞, 每个请求占线程) vs WebClient(非阻塞, 基于 Reactor Netty)

关键设计: **Why WebClient 取代 RestTemplate？** 阻塞客户端在高并发下线程耗尽(每请求一线程); WebClient 基于 Reactor Netty 事件驱动 — 一个连接/线程服务多个并发请求, 且返回 Mono/Flux 支持背压与响应式组合。[模式: 响应式客户端]

数据流: webClient.get().uri("/user/1").retrieve().bodyToMono(User.class) → 构建请求 → Reactor Netty 异步发送 → 不阻塞, 返回 Mono<User> → 订阅/下游组合 → 响应到达→反序列化→Mono 发射 User。exchangeToMono 版可先查 status→按需处理。

→ 引出 Stage 7: Spring Boot 自动装配 (24域) — 至此 15 个计划内缺域(C-1~C-15)全部完成: spring-core 7 + SpEL + context 2 + jdbc 1 + web 4。
