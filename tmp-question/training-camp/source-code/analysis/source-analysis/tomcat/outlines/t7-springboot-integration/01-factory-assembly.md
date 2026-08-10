# T-7 §1 TomcatServletWebServerFactory — Spring Boot 的总装车间

> 依赖 T-6 | 🟡 Working | 2 KP

**读者处境**: T-1~T-6 全部是 Tomcat 内核源码 — 但 Spring Boot 里嵌入式 Tomcat 是如何组装的？`SpringApplication.run()` 最终怎么调到了 `tomcat.start()`？

### 1. getWebServer() — 一行代码创建全部 5 层容器

场景: Spring Boot 应用启动 — `SpringApplication.run()` → `ServletWebServerApplicationContext.onRefresh()` → `createWebServer()` → `TomcatServletWebServerFactory.getWebServer()` — 这个工厂方法在 ~30 行内创建了 Tomcat → Connector → Engine → Host → Context → TomcatStarter — 对应 T-1 的全部 5 层容器。

源码路径:
- `TomcatServletWebServerFactory.java:196` — **getWebServer()**: `Tomcat tomcat = new Tomcat()`(L200) → `Connector connector = new Connector(this.protocol)`(L206) → `tomcat.getService().addConnector(connector)`(L208) → `tomcat.getHost().setAutoDeploy(false)`(L212) → `prepareContext(tomcat.getHost(), initializers)`(L218)
- `TomcatServletWebServerFactory.java:235` — **prepareContext()**: 创建 `StandardContext` → 设置 `classLoader`(T-6) / `docBase` / `sessionTimeout`
- `TomcatServletWebServerFactory.java:403` — **configureContext()**: `context.addLifecycleListener(new TomcatStarter(initializers))` — TomcatStarter 是 Spring↔Servlet 规范的桥

关键设计: **Why Tomcat 实例由 Factory 创建而非 Spring IoC?** Tomcat 不是 Spring Bean — 它有自己的生命周期(独立于 ApplicationContext)。`ServletWebServerApplicationContext` 在 `onRefresh()` 中创建 WebServer — 完成端口绑定 — 然后才继续 Spring 的 Bean 初始化(确保 WebServer 在 Bean 初始化前就接受请求 — 避免 Bean 初始化过程中端口不可用)。

数据流: `SpringApplication.run()`→`ServletWebServerApplicationContext.onRefresh()`→`createWebServer()`→`factory.getWebServer(initializers)`→`new Tomcat()`→`new Connector(protocol)`→`tomcat.getService().addConnector(connector)`→`tomcat.getHost().setAutoDeploy(false)`→`prepareContext(host, initializers)`→`new StandardContext()`→`context.addLifecycleListener(new TomcatStarter(initializers))`→`tomcat.start()`(T-1 Lifecycle cascade)→端口绑定→返回 `TomcatWebServer(tomcat, port)`。

### 2. TomcatStarter — Spring 如何与 Servlet 规范对接？

场景: Servlet 规范 3.0+ 定义了 `ServletContainerInitializer`(SCI) — 容器启动时调用 `onStartup(Set<Class<?>>, ServletContext)` — Servlet 的 `@HandlesTypes` 注解声明它感兴趣的类。Spring Boot 的 `TomcatStarter` 实现 SCI — 但 Spring 不需要扫描 `@HandlesTypes` — 它通过 `ServletContextInitializer` 列表直接注入。

源码路径:
- `TomcatServletWebServerFactory.java:403` — **configureContext()**: `context.addLifecycleListener(new TomcatStarter(initializers))` — initializers 是 Spring IoC 收集的所有 `ServletContextInitializer` Bean
- `TomcatStarter.onStartup()`: 遍历 initializers → 每个 initializer.onStartup(servletContext) → 注册 Filter/Servlet/Listener

关键设计: **Why LifecycleListener 而非 @HandlesTypes?** SCI 的 `@HandlesTypes` 需要容器扫描 classpath 找带注解的类 — 在嵌入式 Tomcat(JAR 启动)中扫描成本高。Spring Boot 绕过扫描 — 直接通过 IoC 容器注入已扫描好的 initializer 列表 — 省去了容器的 classpath 扫描。

→ 引出 §2 Customizer 三层 + 生产配置映射 — Factory 创建了 Tomcat — 但如何配置线程池大小/连接超时/SSL？`server.tomcat.threads.max=200` 怎么到达 `AbstractEndpoint.setMaxThreads()`？
