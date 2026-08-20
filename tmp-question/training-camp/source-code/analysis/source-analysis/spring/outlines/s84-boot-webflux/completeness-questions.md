# S-20 WebFlux+Netty 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 怎么配 webflux 的端口? | §2/§3 (server.port → getListenAddress) |
| 2 | 为什么用 Netty 不是 Tomcat? | §1 (@ConditionalOnWebApplication REACTIVE + classpath) |
| 3 | 端口被占会怎样? | §3 (PortInUseException → S-16) |
| 4 | 我的自定义处理器怎么接? | §2 (HttpHandler + ReactorHttpHandlerAdapter) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么 REACTIVE/SERVLET 条件互斥? | §1 (两套工厂不冲突) |
| 6 | 为什么适配器解耦? | §2 (HttpHandler vs Reactor Netty 生态) |
| 7 | 为什么 paused + start 分离? | §3 (refresh 后延迟绑定接流量) |
| 8 | 与 S-8 servlet 容器对照? | 边界 (§2/§3, REACTIVE vs SERVLET) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 9 | ReactiveWebServerFactory 契约是什么? | §1 (getWebServer(HttpHandler)→WebServer) |
| 10 | "Netty started on port X" 哪来的? | §3 (getStartedOnMessage) |
| 11 | HttpHandler 怎么到 Netty? | §2 (适配器链) |

## 覆盖: 11 问 / 3 身份 / 100%
