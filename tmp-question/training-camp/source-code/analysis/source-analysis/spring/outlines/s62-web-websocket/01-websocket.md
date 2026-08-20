# C-20 WebSocket — 双向通信 (注册链 → 处理器 → 握手)

> 依赖 C-15 WebFlux (网络层) | 🟡 Working | 6 KP | [模式: 前端控制器 + 模板方法 + 拦截器]

**读者处境**: 聊天/实时通知 — 浏览器和服务器建立 WebSocket 长连接双向推。Spring 里 @EnableWebSocket + WebSocketHandler 怎么工作？握手时怎么鉴权？

### 1. @EnableWebSocket 注册链 — 路径 → 处理器

场景: `@Configuration @EnableWebSocket class WebConfig implements WebSocketConfigurer { addWebSocketHandlers(registry) { registry.addHandler(chatHandler, "/chat"); } }` — 这个 /chat 路径怎么映射到 ChatHandler？

源码路径:
- `EnableWebSocket.java:65` — **@EnableWebSocket**: `@Import(DelegatingWebSocketConfiguration.class)` — 激活 WebSocket 配置
- `WebSocketConfigurationSupport.java:43,63` — **装配**: webSocketHandlerMapping L43: 建 ServletWebSocketHandlerRegistry → `getHandlerMapping()`(返回 SimpleUrlHandlerMapping) → 调 registerWebSocketHandlers(L63) 触发用户配置
- `WebSocketConfigurer.java:28` — **配置**: implements WebSocketConfigurer → addWebSocketHandlers(registry)
- `WebSocketHandlerRegistry.java:32` — **注册**: addHandler(handler, paths)(L32) → 返回 WebSocketHandlerRegistration(链式加拦截器/origins)

关键设计: **Why 复用 @Enable 注解 + HandlerMapping 模式？** 与 @EnableWebMvc 同构: 注解引入配置类, 配置类建"URL→处理器"映射(SimpleUrlHandlerMapping), 请求命中路径→调处理器 — WebSocket 也走 Spring MVC 的 HandlerMapping 机制, 只是处理器是 WebSocketHandler。[模式: 前端控制器 + 模块化配置]

数据流: @EnableWebSocket → DelegatingWebSocketConfiguration → WebSocketConfigurationSupport → 建 ServletWebSocketHandlerRegistry → 调 WebSocketConfigurer.addWebSocketHandlers → registry.addHandler(chatHandler, "/chat") → WebSocketHandlerMapping 注册 /chat→chatHandler → HTTP 请求 /chat 命中 → 进入握手。

### 2. WebSocketHandler — 连接生命周期回调

场景: 连接建立后, 收消息、处理错误、连接关闭 — 这些事件怎么通知应用逻辑？

源码路径:
- `WebSocketHandler.java:35,43,50,67` — **四回调**: afterConnectionEstablished(session)(L43, 连接建立, 可主动 sendMessage)/handleMessage(session, message)(L50, 收到消息)/handleTransportError(传输错误)/afterConnectionClosed(session, closeStatus)(L67, 连接关闭)
- `TextWebSocketHandler`(spring-websocket/socket/TextWebSocketHandler) — **便捷基类**: 只覆写 handleTextMessage(文本), 其余默认空实现
- session: WebSocketSession — sendMessage(WebSocketMessage) 主动推送, 保存属性和并发限制

关键设计: **Why 显式四回调而非"一个 handle"？** WebSocket 生命周期事件分明(连接/消息/错误/关闭) — 模板方法让应用只需覆写关心的事件; TextWebSocketHandler 进一步把文本消息提取出来, 免去类型判断。[模式: 模板方法 — 生命周期]

数据流: 客户端连上 → afterConnectionEstablished(session) → 服务器 session.sendMessage("welcome") 主动推 → 客户端发 "hi" → handleMessage(session, TextMessage("hi")) → 业务处理 → session.sendMessage("echo:hi") → 客户端断开 → afterConnectionClosed(session, NORMAL) → 清理。

### 3. HandshakeInterceptor + 握手升级

场景: WebSocket 从 HTTP 升级而来 — 握手阶段要鉴权(检查 token)、存用户信息到 session — HandshakeInterceptor 提供 before/after 钩子。

源码路径:
- `HandshakeInterceptor.java:36,47` — **钩子**: beforeHandshake(request, response, handler, attributes)(L47) — 返回 false 拒绝握手(可鉴权/校验); afterHandshake(握手完成) — attributes 里的值会复制进 WebSocketSession
- `DefaultHandshakeHandler.java:33` — **握手**: 类声明 extends AbstractHandshakeHandler — doHandshake 在父类 AbstractHandshakeHandler:209 — 验证请求/升级创建 WebSocketSession
- `WebSocketHandlerRegistration.java:48,71` — **配置**: addInterceptors(interceptors...)(L48)/setAllowedOrigins(跨域白名单 L71)/setHandshakeHandler

关键设计: **Why 单独握手拦截器而非在 afterConnectionEstablished 里鉴权？** 握手是 HTTP 阶段, 可读请求头/Cookie/拒绝(返回 4xx); 一旦升级为 WebSocket 再想拒绝已晚 — beforeHandshake 是"连接建立前最后防线", 且 attributes 能把认证信息传给 session。[模式: 拦截器 — 握手期关卡]

数据流: 客户端 ws://host/chat?token=x → HTTP 握手 → HandshakeInterceptor.beforeHandshake: 校验 token → 存 attributes[userId]=1 → true 通过 → DefaultHandshakeHandler.doHandshake 升级 → afterHandshake → WebSocketSession(含 userId 属性) → afterConnectionEstablished。token 无效 → beforeHandshake false → 拒绝握手(4xx)。

→ 引出 9-B: messaging — WebSocket 之上更高层抽象: @EnableWebSocketMessageBroker + @MessageMapping/@SendTo — STOMP 订阅/路由, 不用手写 WebSocketHandler。
