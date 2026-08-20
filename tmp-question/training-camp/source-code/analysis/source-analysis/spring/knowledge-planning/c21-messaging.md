# C-21 messaging — STOMP 消息 (MessageMapping → SimpMessagingTemplate → 代理)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | EnableWebSocketMessageBroker(约100行)+AbstractMessageBrokerConfiguration(约600行)+SimpAnnotationMethodMessageHandler(约470行)+SimpMessagingTemplate(约200行)+SimpleBrokerMessageHandler(约430行)+MessageMapping(约130行)+SendTo(约90行)
> 基线: C-20 WebSocket — 之上的一层抽象: @MessageMapping/@SendTo STOMP 订阅路由, 不用手写 WebSocketHandler; 原始执行计划 9-B

---

## §0.8

- 🟡 Working，1篇 — 激活(@EnableWebSocketMessageBroker → AbstractMessageBrokerConfiguration 建通道/模板/代理) → 路由(SimpAnnotationMethodMessageHandler: @MessageMapping 匹配客户端消息→方法) → 推送(SimpMessagingTemplate.send: 应用内→clientOutboundChannel→代理→订阅者) → 代理(SimpleBrokerMessageHandler: SUBSCRIBE 记订阅/MESSAGE 广播; StompBrokerRelay 外接)
- 设计模式: [模式: 注解处理器]—@MessageMapping 路由; [模式: 代理]—消息代理分发; [模式: 管道]—inbound/outbound 通道解耦

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| EnableWebSocketMessageBroker.java:67 | 激活 | **@EnableWebSocketMessageBroker**: @Import(DelegatingWebSocketMessageBrokerConfiguration) | High |
| AbstractMessageBrokerConfiguration.java:159 | 装配 | **clientInboundChannel L159**(入站)/clientOutboundChannel/SimpMessagingTemplate/SimpleBrokerMessageHandler — 消息管道与代理 | High |
| MessageMapping.java:111,125 | 注解 | **@MessageMapping(value)**: 方法映射到目的地(/app/xxx) | High |
| SendTo.java:45,50 | 注解 | **@SendTo(value)**: 方法返回值发送到指定目的地(默认=入站目的地) | High |
| SimpAnnotationMethodMessageHandler.java:93,402 | 路由 | **处理器**: getMappingForMethod L402 读 @MessageMapping → 客户端消息按目的地匹配→调用方法(同 MVC 的 HandlerMapping+HandlerAdapter) | High |
| SimpMessagingTemplate.java:47,139 | 推送 | **send L139**: doSend L150→sendInternal L181: messageChannel.send(message) → 经 clientOutboundChannel→代理 | High |
| SimpleBrokerMessageHandler.java:52,302 | 代理 | **handleMessageInternal L302**: MESSAGE→sendMessageToSubscribers(L316/L400 广播); SUBSCRIBE L351 记订阅; UNSUBSCRIBE L355 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 激活+路由+推送+代理约 1900 行 — 知识主线: "@MessageMapping 收 → 方法处理 → SimpMessagingTemplate 推 → 代理广播". 1篇 (~46行) 按"激活→路由→推送→代理"展开。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | @MessageMapping 路由 (SimpAnnotationMethodMessageHandler: 目的地→方法) | 🔴 | **为什么🔴**: 收消息的处理核心 — 与 MVC @RequestMapping 同构 |
| P1-2 | SimpMessagingTemplate 推送 (send→channel→代理) | 🔴 | **为什么🔴**: 应用主动推送(群发/点对点)的标准通道 |
| P1-3 | SimpleBrokerMessageHandler 代理 (SUBSCRIBE 订阅 + MESSAGE 广播) | 🔴 | **为什么🔴**: 订阅/广播机制 — STOMP 消息分发的核心 |
| P2-1 | @EnableWebSocketMessageBroker 装配 (inbound/outbound 通道+代理) | 🟡 | **为什么🟡**: 激活与消息管道 |
| P2-2 | @SendTo/返回值 (方法结果→目的地) | 🟡 | **为什么🟡**: 处理结果如何回推 |
| P3-1 | SimpleBroker vs StompBrokerRelay (内存 vs 外接代理) | 🟢 | **为什么🟢**: 单机 vs 集群消息代理选型 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **激活与管道** (@EnableWebSocketMessageBroker + 通道) | 🟡 | 消息基础架构 |
| B | **收/路由** (@MessageMapping + 处理器) | 🔴 | 入站处理 |
| C | **推/广播** (SimpMessagingTemplate + 代理) | 🔴 | 出站分发 |

> **Cluster A (§1)**: @EnableWebSocketMessageBroker + AbstractMessageBrokerConfiguration(通道/模板/代理)
> **Cluster B (§2)**: @MessageMapping 路由 + @SendTo 返回值 → SimpMessagingTemplate
> **Cluster C (§3)**: SimpleBrokerMessageHandler(订阅/广播) + 两种代理选型

→ 至此 60 域 Spring Framework 计划 + 补充域全部覆盖完成 — 下一阶段 Stage 7: Spring Boot 自动装配 (24域)

(End of file - total 61 lines)
