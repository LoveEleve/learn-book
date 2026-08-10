# T-7 §2 Customizer 三层 + 配置映射 + GracefulShutdown

> 依赖 §1 | 🟡 Working

**读者处境**: Factory 创建了 Tomcat 实例 — 但 `server.tomcat.threads.max=200` 怎么作用到 `NioEndpoint` 的线程池？`WebServerFactoryCustomizer` 是什么？优雅关闭怎么实现的？

### 1. Customizer 三层体系

场景: 需要给 Engine Pipeline 加一个 `AccessLogValve` — 在 Spring Boot 中通过 `@Bean WebServerFactoryCustomizer<TomcatServletWebServerFactory>` — 获取 `factory.addEngineValves(new AccessLogValve())` — 不需要写 XML。这是三层的中间层。

源码路径:
- **Layer 1 — 配置属性**: `ServerProperties(server.tomcat.*)` — Spring Boot 自动映射 properties→Factory setter(`setMaxThreads`→connector/endpoint)
- **Layer 2 — Customizer**: `WebServerFactoryCustomizer<T>` — Bean 后处理器，在 Factory 创建 WebServer 前调用 customize()
- **Layer 3 — 直接定制**: `TomcatServletWebServerFactory` 子类覆写 `postProcessContext()` — 最灵活但最重

关键设计: **Why 三层而非一层？** Layer 1 覆盖 90% 的场景(`server.tomcat.*` 属性)。Layer 2 覆盖程序化定制(加 Valve、自定义 Connector)。Layer 3 覆盖极端场景(子类化 Factory)。三层递进避免了"所有定制都要子类化 Factory"的笨重方案。

数据流: `application.properties server.tomcat.threads.max=200`→`ServerProperties.setMaxThreads(200)`→`TomcatWebServerFactoryCustomizer`(Spring Boot 内置)→`factory.setMaxThreads(200)`→`factory.getWebServer()`→`Connector connector = new Connector()`→`AbstractEndpoint endpoint = protocolHandler.getEndpoint()`→`endpoint.setMaxThreads(200)`→Worker 线程池大小=200。

### 2. 配置映射 — server.tomcat.* 到源码的精确路径

场景: `server.tomcat.max-connections=10000` — 这个值最终写入 `AbstractEndpoint.setMaxConnections(10000)`。Spring Boot 的 `ServerProperties.Tomcat` 内部类映射了所有 `server.tomcat.*` 属性 → Factory 的 setter → 最终到 Tomcat 源码字段。

关键映射:

| 配置项 | Factory setter | 最终目标 |
|------|------|------|
| `server.tomcat.threads.max` | `setMaxThreads` | `AbstractEndpoint.setMaxThreads()` |
| `server.tomcat.max-connections` | `setMaxConnections` | `AbstractEndpoint.setMaxConnections()` |
| `server.tomcat.accept-count` | `setAcceptCount` | `AbstractEndpoint.setAcceptCount()` |
| `server.tomcat.connection-timeout` | `setConnectionTimeout` | `AbstractEndpoint.setConnectionTimeout()` |

### 3. GracefulShutdown — Spring Boot 3.x 的优雅关闭

场景: `kill <pid>` 发送 SIGTERM → Spring Boot 关闭 → Tomcat 需要先停止接受新连接 → 等待现有请求完成 → 再关闭线程池。`GracefulShutdown` 是 Spring Boot 3.x 新增的内置功能 — 注册 `GracefulShutdownTomcatConnectorCustomizer` 到 Connector。

源码路径: Spring Boot `GracefulShutdown` 类 — 实现 `TomcatConnectorCustomizer` → `connector.stop()` 前调用 `connector.getProtocolHandler().closeServerSocketGraceful()`(T-2 §3 的 `closeServerSocketGraceful`) → 等待活动连接排空 → 超时强制关闭。

→ Tomcat 源码分析全部 7 域完成。Spring 生态 Stage 1(I/O 基础): Netty 13 章 + Tomcat 7 域。Stage 2: Spring Framework 核心容器。
