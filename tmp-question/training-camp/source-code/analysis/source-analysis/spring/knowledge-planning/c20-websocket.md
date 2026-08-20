# C-20 WebSocket — 双向通信 (@EnableWebSocket → Handler → 握手)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | EnableWebSocket(约90行)+WebSocketConfigurer(约40行)+WebSocketHandlerRegistry(约110行)+WebSocketHandler(约90行)+HandshakeInterceptor(约70行)+WebSocketConfigurationSupport(约90行)+DefaultHandshakeHandler(约200行)
> 基线: C-19 结尾桥 — spring-test 收尾回 Web — @EnableWebSocket 注册实时双向通信; 原始执行计划 9-A

---

## §0.8

- 🟡 Working，1篇 — 注册链(@EnableWebSocket → @Import DelegatingWebSocketConfiguration → WebSocketConfigurer.addWebSocketHandlers → registry.addHandler(handler, path)) → 处理器(WebSocketHandler 生命周期: afterConnectionEstablished/handleMessage/handleTransportError/afterConnectionClosed + TextWebSocketHandler) → 握手(HandshakeInterceptor.beforeHandshake/afterHandshake + DefaultHandshakeHandler 升级 WebSocketSession) → 配置(WebSocketHandlerRegistration: addInterceptors/setAllowedOrigins)
- 设计模式: [模式: 前端控制器]—WebSocketHandlerMapping 注册路径; [模式: 模板方法]—WebSocketHandler 生命周期; [模式: 拦截器]—HandshakeInterceptor 握手钩子

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| EnableWebSocket.java:65 | 启动 | **@EnableWebSocket**: @Import(DelegatingWebSocketConfiguration) — 激活 WebSocket 配置 | High |
| WebSocketConfigurer.java:28 | 配置接口 | **addWebSocketHandlers**: 覆写注册; addHandler(WebSocketHandler, paths) 在 WebSocketHandlerRegistry L32 | High |
| WebSocketConfigurationSupport.java:43,63 | 装配 | **webSocketHandlerMapping L43**: 建 ServletWebSocketHandlerRegistry → getHandlerMapping(SimpleUrlHandlerMapping) → 调 registerWebSocketHandlers(L63) 让用户注册 | High |
| WebSocketHandlerRegistry.java:32 | 注册 | **addHandler**: (handler, 路径) → WebSocketHandlerRegistration(可链式配置拦截器/origins) | High |
| WebSocketHandler.java:35,43,50 | 生命周期 | **四个回调**: afterConnectionEstablished(L43, 连接建立)/handleMessage(L50, 收消息)/handleTransportError/afterConnectionClosed(L67) | High |
| HandshakeInterceptor.java:36,47 | 握手钩子 | **beforeHandshake(L47)/afterHandshake**: 握手前后拦截(鉴权/存 session 属性) | High |
| DefaultHandshakeHandler.java:33 | 握手 | **doHandshake**: 验证请求→升级 HTTP 为 WebSocketSession | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 注册+处理器+握手约 700 行 — 知识主线: "@EnableWebSocket 注册 → 请求命中 WebSocketHandler → 握手升级 → 生命周期回调". 1篇 (~45行) 按"注册→处理器→握手"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | @EnableWebSocket 注册链 (@Import→WebSocketConfigurer→addHandler→WebSocketHandlerMapping) | 🔴 | **为什么🔴**: 路径→处理器的注册机制 — 与 MVC @EnableWebMvc 同构 |
| P1-2 | WebSocketHandler 生命周期 (established/handleMessage/closed) | 🔴 | **为什么🔴**: 双向通信的核心 — 连接/消息/关闭回调 |
| P1-3 | HandshakeInterceptor (beforeHandshake/afterHandshake) | 🔴 | **为什么🔴**: 握手期鉴权/会话属性 — 连接建立的关卡 |
| P2-1 | 握手升级 (DefaultHandshakeHandler → WebSocketSession) | 🟡 | **为什么🟡**: HTTP→WebSocket 协议升级 |
| P2-2 | WebSocketHandlerRegistration 配置 (addInterceptors/setAllowedOrigins) | 🟡 | **为什么🟡**: 拦截器与跨域配置 |
| P3-1 | TextWebSocketHandler (文本消息便捷实现) | 🟢 | **为什么🟢**: 常用基类 — 只覆写 handleTextMessage |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **注册链** (@EnableWebSocket→HandlerMapping) | 🔴 | 路径→处理器注册 |
| B | **处理器生命周期** (WebSocketHandler 四回调) | 🔴 | 双向通信核心 |
| C | **握手与配置** (HandshakeInterceptor + 升级) | 🟡 | 连接建立与鉴权 |

> **Cluster A (§1)**: @EnableWebSocket + WebSocketConfigurer + WebSocketHandlerRegistry.addHandler + WebSocketHandlerMapping
> **Cluster B (§2)**: WebSocketHandler 生命周期 + TextWebSocketHandler
> **Cluster C (§3)**: HandshakeInterceptor + DefaultHandshakeHandler 升级 + allowedOrigins 配置

→ 引出 9-B: messaging — WebSocket 之上的一层抽象: @MessageMapping/@SendTo STOMP 消息路由(SimpMessagingTemplate/订阅)

(End of file - total 61 lines)
