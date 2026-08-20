# S-20 WebFlux+Netty — ReactiveWebServerFactory → NettyReactiveWebServerFactory → NettyWebServer

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | ReactiveWebServerFactory(45行)+ReactiveWebServerFactoryAutoConfiguration(150行)+ReactiveWebServerFactoryConfiguration(180行)+NettyReactiveWebServerFactory(230行)+NettyWebServer(270行)+AbstractReactiveWebServerFactory(90行)
> 基线: BOOT-PLAN-v2 S-20 — 响应式服务器工厂自动装配; 前置: **C-15 WebFlux(请求处理内核 DispatcherHandler/RouterFunction — 复用) + Netty N-1~N-12(网络内核) + S-8(servlet 容器对照)** — 展开响应式服务器工厂

---

## §0.8

- 🔴 Deep，1篇 — 抽象与自动装配(ReactiveWebServerFactory 接口: getWebServer(HttpHandler)→WebServer + ReactiveWebServerFactoryAutoConfiguration: @AutoConfiguration + @ConditionalOnWebApplication(REACTIVE) + @Import EmbeddedNetty) → NettyReactiveWebServerFactory(getWebServer: createHttpServer[HttpServer.create().bindAddress(getListenAddress)] → ReactorHttpHandlerAdapter → new NettyWebServer) → 生命周期(NettyWebServer.start: handle/route → bindNow → "Netty started on port X"; 端口冲突→PortInUseException)
- 设计模式: [模式: 工厂]—ReactiveWebServerFactory; [模式: 适配器]—ReactorHttpHandlerAdapter 把 HttpHandler 适配到 Reactor Netty; [模式: 条件配置]—@ConditionalOnWebApplication(REACTIVE)+@ConditionalOnClass

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ReactiveWebServerFactory.java:30,31,42 | 接口 | **@FunctionalInterface(L30)**: getWebServer(HttpHandler)→WebServer(L42) — 响应式 WebServer 工厂契约 | High |
| ReactiveWebServerFactoryAutoConfiguration.java:56,58,60 | 自动装配 | **@AutoConfiguration(L56)+@ConditionalOnWebApplication(REACTIVE)(L58)+@Import(L60)** 含 EmbeddedNetty | High |
| ReactiveWebServerFactoryConfiguration.java:57,60,62 | EmbeddedNetty | **@ConditionalOnMissingBean(L57)+@ConditionalOnClass(HttpServer)(L58)→class EmbeddedNetty(L60)**: @Bean NettyReactiveWebServerFactory(L62-63) | High |
| NettyReactiveWebServerFactory.java:72,73,74,75,83 | 工厂实现 | **getWebServer(L72)**: createHttpServer(L73)→ReactorHttpHandlerAdapter(L74)→createNettyWebServer(L75)→new NettyWebServer(L83) | High |
| NettyReactiveWebServerFactory.java:162,163,208 | 配置 | **createHttpServer(L162)**: HttpServer.create().bindAddress(getListenAddress)(L163); getListenAddress(L208): 用 getPort()(L210) | High |
| NettyWebServer.java:117,168,182 | 生命周期 | **start(L117)**: startHttpServer(L168)→handle/route(L171/174)→bindNow(L182-184)→"Netty started on port"(L150); 冲突→PortInUseException(L123-135) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 响应式服务器工厂是"工厂→实现→启动"一条线, 3 块耦合紧密 — 1篇 (~52行) 按"抽象与自动装配 → Netty 工厂 → 生命周期"展开; C-15 的请求处理内核(HttpHandler/DispatcherHandler)复用, 本域只讲服务器工厂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 抽象与自动装配 (ReactiveWebServerFactory + 条件 REACTIVE + EmbeddedNetty) | 🔴 | **为什么🔴**: 响应式服务器怎么被选中装配 |
| P1-2 | NettyReactiveWebServerFactory.getWebServer (HttpServer 构建 + 适配器) | 🔴 | **为什么🔴**: HttpHandler 怎么接入 Reactor Netty |
| P1-3 | NettyWebServer 生命周期 (bindNow + 启动 + PortInUse 边界) | 🔴 | **为什么🔴**: 服务器如何启动/端口冲突 |
| P2-1 | 配置 (ServerProperties port/host → getListenAddress) | 🟡 | **为什么🟡**: 端口/地址映射 |
| P2-2 | 与 S-8 servlet 对照 (REACTIVE vs SERVLET 条件) | 🟡 | **为什么🟡**: 两种 Web 栈工厂差异 |
| P3-1 | 与 C-15/Netty 边界 (请求处理内核 vs 工厂) | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **抽象与自动装配** | 🔴 | 服务器工厂怎么选/装 |
| B | **Netty 工厂实现** | 🔴 | HttpHandler 怎么适配 |
| C | **生命周期与边界** | 🔴 | 启动/端口冲突 |

> **Cluster A (§1)**: ReactiveWebServerFactory 接口 + ReactiveWebServerFactoryAutoConfiguration(@ConditionalOnWebApplication REACTIVE + @Import EmbeddedNetty)
> **Cluster B (§2)**: NettyReactiveWebServerFactory.getWebServer(createHttpServer → ReactorHttpHandlerAdapter → NettyWebServer)
> **Cluster C (§3)**: NettyWebServer.start(bindNow + 日志 + PortInUse) + 配置 + 与 S-8 对照

→ 引出 S-21: Actuator 端点 — WebFlux 服务器之后: Endpoint/WebEndpoint/ControllerEndpoint 的 Actuator 端点(前置 C-12/C-13)
