# C-21 messaging — STOMP 消息 (激活 → 路由 → 推送 → 代理)

> 依赖 C-20 WebSocket | 🟡 Working | 6 KP | [模式: 注解处理器 + 代理 + 管道]

**读者处境**: 聊天室 — 客户端发消息到 /app/chat, 服务器处理后广播给所有订阅 /topic/messages 的客户端 — 这套 STOMP 订阅/路由怎么做？和 C-20 的 WebSocketHandler 什么关系？

### 1. @EnableWebSocketMessageBroker — 激活消息管道

场景: `@Configuration @EnableWebSocketMessageBroker class WsConfig implements WebSocketMessageBrokerConfigurer` — 这个注解引入一整套消息基础设施: 入站/出站通道、SimpMessagingTemplate、消息代理。

源码路径:
- `EnableWebSocketMessageBroker.java:67` — **激活**: @Import(DelegatingWebSocketMessageBrokerConfiguration)
- `AbstractMessageBrokerConfiguration.java:159` — **装配**: clientInboundChannel L159(客户端消息入站)/clientOutboundChannel(出站)/SimpMessagingTemplate(应用推送入口)/SimpleBrokerMessageHandler(内存代理) — 消息在管道间流转
- 配置点: WebSocketMessageBrokerConfigurer.enableSimpleBroker("/topic","/queue") / configureMessageBroker(enableStompBrokerRelay 外接)

关键设计: **Why 引入"通道+代理"而不直接用 WebSocketHandler？** 消息层与传输层解耦: 无论 STOMP 走 WebSocket 还是 TCP, 应用只面向"目的地+消息"; 通道(Channel)解耦收发, 代理统一订阅/广播 — 高层抽象省去手写协议解析与订阅管理。[模式: 管道 + 代理]

数据流: @EnableWebSocketMessageBroker → 配置: registry.enableSimpleBroker("/topic") + registry.setApplicationDestinationPrefixes("/app") → 建 clientInboundChannel(收 STOMP)→SimpAnnotationMethodMessageHandler(路由 @MessageMapping)→clientOutboundChannel(推)→SimpleBrokerMessageHandler(广播)。

### 2. @MessageMapping 路由 + SimpMessagingTemplate 推送

场景: 客户端 `stompClient.send("/app/chat", {...})` → 服务器 @MessageMapping("/chat") 方法执行 → 返回值/主动推送回客户端。收和推各靠什么？

源码路径:
- `MessageMapping.java:111,125` — **收**: @MessageMapping(value) 标在方法 — 映射客户端目的地(/app/chat)
- `SimpAnnotationMethodMessageHandler.java:93,402` — **路由**: getMappingForMethod L402 读 @MessageMapping → 客户端消息按目的地匹配方法 → 调用(同 MVC HandlerMapping+HandlerAdapter 模式)
- `SendTo.java:45,50` — **回**: @SendTo(value) — 方法返回值发送到目的地(默认返回入站目的地)
- `SimpMessagingTemplate.java:47,139` — **主动推**: send L139→doSend L150→sendInternal L181: `messageChannel.send(message)` → 经 clientOutboundChannel → 代理广播

关键设计: **Why @MessageMapping 是"服务器收", SimpMessagingTemplate 是"服务器推"？** 两个方向分离: @MessageMapping 声明"我处理哪个目的地"(入站路由, 注解式); SimpMessagingTemplate 是"程序任意时刻主动发"(出站, 编程式 — 业务代码里 convertAndSend 群发)。@SendTo 是二者桥(方法返回值自动转推送)。[模式: 注解路由 + 模板推送]

数据流: 客户端 send("/app/chat", payload) → clientInboundChannel → SimpAnnotationMethodMessageHandler: 匹配 @MessageMapping("/chat") → 方法处理(payload) → 返回值: 有 @SendTo("/topic/messages") → SimpMessagingTemplate.convertAndSend("/topic/messages", result) → sendInternal → clientOutboundChannel → 代理。业务代码也可主动 template.convertAndSend("/topic/news", x) 群发。

### 3. SimpleBrokerMessageHandler — 订阅管理与广播

场景: 谁订阅了 /topic/messages？消息来了发给谁？SimpleBrokerMessageHandler 是内存代理, 维护订阅表并广播。

源码路径:
- `SimpleBrokerMessageHandler.java:52,302` — **代理**: handleMessageInternal L302: 按 SimpMessageType 分派 — SUBSCRIBE(L351 记录订阅: session→目的地)/UNSUBSCRIBE(L355 移除)/MESSAGE(L316 转发给该目的地订阅者)
- `SimpleBrokerMessageHandler.java:400` — **广播**: sendMessageToSubscribers(目的地, 消息) — 遍历该目的地的所有订阅 session, 逐个发送
- 对照: SimpleBroker(内存, 单机) vs `StompBrokerRelayMessageHandler`(外接 RabbitMQ/ActiveMQ, 集群共享订阅)

关键设计: **Why 用"内存代理"也能工作？** SimpleBroker 在应用内维护订阅映射并直接转发 — 单机/开发足够; 集群需外接代理(StompBrokerRelay)共享订阅状态 — 代理抽象让"单机到集群"只改配置。[模式: 代理 — 订阅/广播]

数据流: 客户端 A SUBSCRIBE /topic/messages → SimpleBroker.handleMessageInternal(SUBSCRIBE) → 记录 [sessionA → /topic/messages] → 服务器向 /topic/messages 推消息 → SimpleBroker(收到 MESSAGE) → sendMessageToSubscribers: sessionA 有订阅 → 经 sessionA 的 WebSocket 发送 → 客户端 A 收到。B 未订阅 → 不收。

→ 引出 Stage 7: Spring Boot 自动装配 (24域) — 至此 Spring Framework 计划 60 域 + 补充域全部覆盖完成。
