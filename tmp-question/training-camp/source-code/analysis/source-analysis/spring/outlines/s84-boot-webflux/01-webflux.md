# S-20 WebFlux+Netty — ReactiveWebServerFactory → NettyReactiveWebServerFactory → NettyWebServer

> 依赖 C-15 WebFlux (复用) + Netty + S-8 (servlet 对照) | 🔴 Deep | 6 KP | [模式: 工厂 + 适配器 + 条件配置]

**读者处境**: WebFlux 应用(spring-boot-starter-webflux)启动 — 谁创建了 Netty 服务器?`HttpHandler`(C-15 DispatcherHandler)怎么被 Netty 接收?为什么是 Netty 而不是 Tomcat?端口占用时报错哪来的?

### 1. 抽象与自动装配 — ReactiveWebServerFactory 的选中

场景: 用 webflux starter → 自动装配里怎么知道要用"响应式服务器", 并且默认选 Netty?

源码路径:
- `ReactiveWebServerFactory.java:30,31,42` — **接口**: `@FunctionalInterface`(L30) + `public interface ReactiveWebServerFactory extends WebServerFactory`(L31); `WebServer getWebServer(HttpHandler httpHandler)`(L42) — 工厂契约: 接收响应式 HttpHandler, 返回 WebServer
- `ReactiveWebServerFactoryAutoConfiguration.java:56,58,60` — **自动装配**: `@AutoConfiguration`(L56) + `@ConditionalOnClass(ReactiveHttpInputMessage)`(L57) + `@ConditionalOnWebApplication(type = Type.REACTIVE)`(L58) + `@Import(...EmbeddedNetty...)`(L60-64)
- `ReactiveWebServerFactoryConfiguration.java:57,60,62` — **EmbeddedNetty**: `@ConditionalOnMissingBean(ReactiveWebServerFactory)`(L57) + `@ConditionalOnClass(HttpServer)`(L58) → `class EmbeddedNetty`(L60): `@Bean NettyReactiveWebServerFactory nettyReactiveWebServerFactory(...)`(L62-63)

关键设计: **Why REACTIVE 条件？** 同一套自动装配同时有 servlet(Tomcat/Jetty, S-8)与 reactive(Netty)两套工厂 — `@ConditionalOnWebApplication(REACTIVE)` 保证只有响应式应用才装配响应式工厂, 与 Servlet 栈互斥不冲突。**Why @Import EmbeddedNetty？** 内嵌服务器工厂在独立配置类里按 @ConditionalOnClass(HttpServer, reactor-netty) 各自激活 — classpath 有 Reactor Netty 就注册 NettyReactiveWebServerFactory(默认 webflux starter 自带)。[模式: 工厂 + 条件配置]

数据流: 用 webflux starter → ReactiveWebServerFactoryAutoConfiguration 评估 → @ConditionalOnWebApplication(REACTIVE) 命中 → @Import EmbeddedNetty → @ConditionalOnClass(HttpServer) 命中 + @ConditionalOnMissingBean 满足 → @Bean NettyReactiveWebServerFactory 注册。

### 2. NettyReactiveWebServerFactory — getWebServer 构建

场景: 拿到工厂后, 一个 HttpHandler(DispatcherHandler 适配)怎么变成可启动的 Netty 服务器?

源码路径:
- `NettyReactiveWebServerFactory.java:72` — **getWebServer(HttpHandler)**: `createHttpServer()`(L73) 建 HttpServer → `new ReactorHttpHandlerAdapter(httpHandler)`(L74) → `createNettyWebServer(httpServer, handlerAdapter, lifecycleTimeout, getShutdown())`(L75) → `new NettyWebServer(httpServer, handlerAdapter, ...)`(L83)
- `NettyReactiveWebServerFactory.java:162,163` — **createHttpServer**: `HttpServer.create().bindAddress(this::getListenAddress)`(L163) — 绑定监听地址; 之后按需配 SSL/压缩/protocol/forwarded(L165-171)
- `NettyReactiveWebServerFactory.java:208,210` — **getListenAddress**: 有 getAddress() 用其主机+端口, 否则 `new InetSocketAddress(getPort())`(L210) — 端口来自 ServerProperties(server.port)
- 适配: `ReactorHttpHandlerAdapter` — 把 Spring 的 `HttpHandler`(Flux 风格)适配成 Reactor Netty 的 `HttpServerHandler`(BiFunction<Request,Response,Publisher>)

关键设计: **Why ReactorHttpHandlerAdapter？** Spring WebFlux 的 HttpHandler 处理逻辑(C-15 DispatcherHandler)与 Reactor Netty 服务器是两个生态 — 适配器把 `HttpHandler.handle(ServerHttpRequest,ServerHttpResponse)` 转成 Reactor Netty 期望的 `(HttpServerRequest,HttpServerResponse)→Publisher<Void>`, 解耦两者。[模式: 适配器]

数据流: getWebServer(httpHandler)(L72) → createHttpServer()(L162): HttpServer.create().bindAddress(getListenAddress)(L163, 端口=server.port) → new ReactorHttpHandlerAdapter(httpHandler)(L74) → new NettyWebServer(httpServer, handlerAdapter,...)(L83) → 返回(此时未 bind, paused)。

### 3. NettyWebServer 生命周期 — start + 端口冲突边界

场景: 返回的 WebServer 是"paused"的 — 谁在何时调用 start?端口被占会怎样?

源码路径:
- `NettyWebServer.java:117,120` — **start()**: 若未启动 → `startHttpServer()`(L120)
- `NettyWebServer.java:168,171,174,182` — **startHttpServer**: 无 routeProviders → `server.handle(this.handler)`(L171); 有 → `server.route(...)`(L174); 最后 `bindNow()`(L184, 或带 lifecycleTimeout L182) — 真正绑定端口开始监听
- `NettyWebServer.java:139,150` — **启动日志**: `logger.info(getStartedOnMessage(...))`(L139) → "Netty started on port X"(`port %s`)(L147-150)
- `NettyWebServer.java:123,132` — **边界**: 绑定失败(ChannelBindException 且 ERROR_ADDR_IN_USE)→ `throw new PortInUseException(localPort, ex)`(L132) — 走 S-16 诊断
- 触发: start 由 `WebServerStartStopLifecycle`(WebServerFactoryAutoConfiguration 的 lifecycle bean)在容器 refresh 后调用

关键设计: **Why paused + start 分离？** 工厂 getWebServer 只装配不监听 — 真正 bind 延迟到容器 refresh 后由 lifecycle 触发, 保证"全部 Bean 就绪再开始接流量"(与 Servlet 容器 S-8 同生命周期约定)。**Why PortInUseException？** Netty bind 抛 ChannelBindException(ADDR_IN_USE) 时转成 Boot 的 PortInUseException, 让 S-16 FailureAnalyzer 给出友好诊断 — 跨域衔接。[模式: 工厂 + 生命周期延迟绑定]

数据流: 容器 refresh → WebServerStartStopLifecycle.start() → NettyWebServer.start(L117) → startHttpServer(L168) → handle(handler)(L171) → bindNow(L184) 绑定 server.port → 成功: "Netty started on port 8080"(L150); 端口占用: ChannelBindException → PortInUseException(L132) → S-16 诊断。请求到达 → ReactorHttpHandlerAdapter → HttpHandler → C-15 DispatcherHandler 处理。

→ 引出 S-21: Actuator 端点 — WebFlux 服务器之后: Endpoint/WebEndpoint/ControllerEndpoint 的 Actuator 端点暴露(前置 C-12/C-13)。
