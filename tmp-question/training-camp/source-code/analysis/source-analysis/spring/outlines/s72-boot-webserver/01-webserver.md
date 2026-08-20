# S-8 嵌入式容器 — ServletWebServerFactory (选择与衔接, 组装复用 t7)

> 依赖 Tomcat t7 (复用) | 🔴 Deep | 6 KP | [模式: 工厂 + 条件装配 + 定制器]

**读者处境**: 嵌入式 Tomcat 谁创建的?换 Jetty/Undertow 怎么换?容器在启动的哪个点创建?—— Tomcat 的**组装细节** t7 已讲透, 本域讲**选择/装配/衔接** (06 §2.5 复用≠省略)。

### 1. 工厂抽象 + 三容器条件选择

场景: `TomcatServletWebServerFactory`/`JettyServletWebServerFactory`/`UndertowServletWebServerFactory` 三个工厂 — 公共抽象让容器可替换; classpath 决定用哪个。

源码路径:
- `ServletWebServerFactory.java:31,43` — **接口**: extends WebServerFactory — getWebServer(ServletContextInitializer...)(L43) → 返回 WebServer(TomcatWebServer/JettyWebServer/UndertowWebServer)
- `ServletWebServerFactoryConfiguration.java:61,64,69` — **三选一**: Tomcat(@ConditionalOnClass({Servlet, Tomcat, UpgradeProtocol}) L64 → @Bean TomcatServletWebServerFactory L69) / Jetty(@ConditionalOnClass(Server/Loader/WebAppContext) L86) / Undertow — **classpath 有谁的类就用谁**
- 工厂子类: TomcatServletWebServerFactory.getWebServer 内部 `new Tomcat()`+组装(t7 §1 已展开 — 本域不重复)

关键设计: **Why 工厂抽象 + classpath 选择？** 容器可替换性: 应用代码只依赖 ServletWebServerFactory/WebServer 抽象, 换容器=换 starter 依赖(引 undertow 弃 tomcat)→ 条件装配自动换工厂 — 零代码改动。**Why 组装细节在 t7？** t7 已深度覆盖 Tomcat 组装(getWebServer 全链), 本域只讲"工厂怎么被选中/激活" — 06 §2.5: 机制内核引用, 装配视角展开。[模式: 工厂 + 条件装配]

数据流: 项目依赖 starter-tomcat → classpath 有 Tomcat/UpgradeProtocol 类 → ServletWebServerFactoryConfiguration.Tomcat 内部类条件匹配 → @Bean TomcatServletWebServerFactory 注册。换 starter-undertow → Tomcat 条件不满足, Undertow 条件满足 → UndertowServletWebServerFactory 注册。

### 2. 自动装配与生命周期衔接

场景: 工厂 Bean 有了, 什么时候调 getWebServer?答案: refresh 的 onRefresh — 容器创建与 Spring 生命周期衔接。

源码路径:
- `ServletWebServerFactoryAutoConfiguration.java:74,77` — **自动装配**: @AutoConfiguration(after=SslAutoConfiguration)(L65)+@ConditionalOnClass(ServletRequest)(L67) + @Import(BeanPostProcessorsRegistrar + ServletWebServerFactoryConfiguration)(L70) — servletWebServerFactoryCustomizer L77(ServerProperties → 工厂定制)
- `ServletWebServerApplicationContext.java:164,167` — **生命周期**: onRefresh L164 → createWebServer L167 → 找 ServletWebServerFactory → factory.getWebServer(initializers)(组装细节 → t7)
- `TomcatServletWebServerFactoryCustomizer.java:38,53` — **Tomcat 定制**: customize L53 应用 ServerProperties 到工厂 — server.tomcat.* 属性(优雅关闭等, t7 02 配置映射衔接)

关键设计: **Why 在 onRefresh 创建 WebServer？** 容器必须"Bean 全部就绪前"就绑定端口(供请求到达) — onRefresh 是 refresh 早期阶段(S-4 时序); createWebServer 找工厂→getWebServer→start — 之后 DispatcherServlet 等再注册进 ServletContext。**Why Customizer？** ServerProperties(S-5 绑定)通过 Customizer 应用到工厂, 不改工厂代码。[模式: 生命周期钩子 + 定制器]

数据流: refresh(S-4) → ServletWebServerApplicationContext.onRefresh(L164) → createWebServer(L167) → 注入的 TomcatServletWebServerFactory → getWebServer(initializers)(t7 §1: new Tomcat→Connector→prepareContext→TomcatStarter) → TomcatWebServer.start(端口绑定) → refresh 继续(DispatcherServlet 注册) → 应用就绪。

### 3. 三容器对照 — 本域增量

场景: 换容器要了解什么差异?三工厂的结构对照 — t7 只讲了 Tomcat, Jetty/Undertow 是本域增量。

源码路径:
- `JettyServletWebServerFactory`(web/embedded/jetty) — **Jetty**: getWebServer → new Server + WebAppContext 组装 — 与 Tomcat 的 Connector/Engine 模型不同(Jetty 用 Handler 链)
- `UndertowServletWebServerFactory`(web/embedded/undertow) — **Undertow**: getWebServer → Undertow builder(Undertow.Builder) — 轻量/非阻塞风格
- 切换: 改依赖(排除 starter-tomcat, 加 starter-jetty/undertow) → §1 条件选择自动切换 — 配置文件(server.port 等)通用, 专属配置(server.tomcat.*/server.jetty.*/server.undertow.*)不同

关键设计: **Why 配置面部分通用？** server.port/address 等由 ServletWebServerFactoryCustomizer 统一应用(所有容器共用); server.tomcat.*/jetty.*/undertow.* 由各容器专属 Customizer 应用 — 通用配置跨容器迁移无缝, 专属配置才需改。**三容器定位**: Tomcat(默认/最全特性) vs Jetty(轻量/嵌入式友好) vs Undertow(高并发 IO)。[模式: 抽象 + 专属定制分离]

数据流: 换 Undertow: 改依赖 → ServletWebServerFactoryConfiguration 选 Undertow 工厂 → getWebServer: Undertow.Builder 组装 → UndertowWebServer.start → 同一 onRefresh 链路。server.port 照用(通用 Customizer), server.undertow.* 专属配置生效。

→ 引出 S-9: HTTP客户端+消息转换 — Web 服务端就绪, 补客户端侧: RestClient/HttpMessageConverters/JacksonAutoConfiguration。
