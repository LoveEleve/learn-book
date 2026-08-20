# B-1 DispatcherServlet 接入 Tomcat — 从 `ServletWebServerApplicationContext` 到嵌入式容器

> 依赖 A-3 Servlet 规范 + W-1 DispatcherServlet | 🟡 Working | 3 KP | [模式: 模板方法 + 工厂]

**读者处境**: Spring Boot 项目里没有 `web.xml`，`DispatcherServlet` 是怎么注册到 Tomcat 的？`TomcatServletWebServerFactory` 和 `TomcatWebServer` 又是怎样把嵌入式 Tomcat 启动起来的？

### 1. `ServletWebServerApplicationContext` — 在 refresh 中创建嵌入式 Web 服务器

场景: Spring Boot 的 `SpringApplication.run()` → 创建 `ServletWebServerApplicationContext` → `onRefresh()` 中创建 `WebServer`。

源码路径:
- `ServletWebServerApplicationContext.java` — `onRefresh()` 覆写父类钩子
- `createWebServer()` — 从 `ServletWebServerFactory` 获取 `WebServer`，触发 `initialize()` + `start()`
- `getSelfInitializer()` — 返回 `ServletContextInitializer`，在 `onStartup(servletContext)` 中注册 `DispatcherServlet`

关键设计: **Why 在 `onRefresh()` 创建 WebServer？** `onRefresh()` 是 `AbstractApplicationContext.refresh()` 的 Step 9 子类钩子——此时 BeanFactory 已就绪、BFPP/BPP 已注册，但单例 Bean 还未全部实例化。Spring Boot 选择在这个时点创建 WebServer，因为嵌入式容器（Tomcat/Jetty/Undertow）需要在单例创建之前就绑定端口和注册 Servlet，但又需要依赖已配置好的 `ServletWebServerFactory` Bean。

### 2. `TomcatServletWebServerFactory` — 把 Spring Boot 配置映射成 Tomcat 内部结构

场景: `server.port=8080`、`server.tomcat.max-threads=200` — 这些配置最终怎么变成 Tomcat 的 `Connector`、`ProtocolHandler`、`Context`？

源码路径:
- `TomcatServletWebServerFactory.java` — `getWebServer(...)` 创建 `Tomcat` 实例，配置 `Connector`、`Context`、`Wrapper`
- `TomcatWebServer.java` — 包装 `Tomcat` 实例，提供 `start()` / `stop()` 生命周期

关键设计: **Why Factory 和 WebServer 分开？** Factory 负责"创建和配置"，WebServer 负责"生命周期管理"。这让 `TomcatWebServer` 可以在 `start()` 时做 `removeServiceConnectors()` + `disableBindOnInit()` 等优化，把真正的端口绑定推迟到 Spring 容器完全就绪之后。

### 3. `DispatcherServlet` 注册到 Tomcat — 通过 `ServletContainerInitializer` 桥接

场景: `TomcatStarter` 实现 `ServletContainerInitializer`，在 Tomcat 启动时回调 `onStartup(Set<Class<?>>, ServletContext)`，把 `DispatcherServlet` 注册为 `Wrapper`。

源码路径:
- `TomcatStarter.java` — 实现 `ServletContainerInitializer`，持有 `ServletContextInitializer` 列表
- `onStartup(servletContext)` — 遍历 `ServletContextInitializer`，逐个调用 `onStartup`
- `DispatcherServletRegistrationBean` — 把 `DispatcherServlet` 注册到 Servlet 容器

关键设计: **Why 用 `ServletContainerInitializer` 而不是 `web.xml`？** Servlet 3.0+ 规范允许通过 `ServletContainerInitializer` + `@HandlesTypes` 编程式注册 Servlet，无需 `web.xml`。Spring Boot 利用这个规范入口，在嵌入式 Tomcat 启动时把 `DispatcherServlet` 编程式注册进去。这是 Servlet 规范为嵌入式容器预留的桥。

→ 引出 B-2: Spring Boot 如何装配 Spring Framework（`@SpringBootApplication` → `AutoConfigurationImportSelector` → `refresh()`）。
