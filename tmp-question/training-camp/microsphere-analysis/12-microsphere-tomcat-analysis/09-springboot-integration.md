# 第 8 篇：Spring Boot 集成 + 生产定制

> 前 7 篇走完了 Tomcat 内核：容器树 → Valve 链 → Mapper → Servlet 生命周期 → Connector → NIO → 类加载。
> 本篇回答终极问题：**Spring Boot 如何把这些串起来？生产环境怎么定制？
> 面试官最关心的"配置项 → 源码"映射在哪里？**
>
> 这是全系列的**收官篇**：从 `SpringApplication.run()` 到端口监听，
> 再到 `server.tomcat.*` 每个配置项的底层实现。
>
> **源码范围**（全部在 Spring Boot 仓库）：
> - `ServletWebServerFactoryAutoConfiguration` / `ServletWebServerFactoryConfiguration`
> - `ServletWebServerApplicationContext` / `WebServerStartStopLifecycle`
> - `TomcatServletWebServerFactory` / `TomcatWebServer` / `TomcatStarter`
> - `WebServerFactoryCustomizerBeanPostProcessor` + 三个 Customizer
> - `ServerProperties`（`server.*` / `server.tomcat.*` 的完整清单）
> - `GracefulShutdown` / `TomcatVirtualThreadsWebServerFactoryCustomizer`
>
> **本篇定位**：Spring Boot 特有（嵌入式核心）。与第 0~7 篇的 Tomcat 内核知识在此汇合。

---

## 目录

1. [从 SpringApplication.run() 到端口监听：全链路总图](#1-从-springapplicationrun-到端口监听全链路总图)
2. [自动装配：三个配置类的接力](#2-自动装配三个配置类的接力)
3. [TomcatServletWebServerFactory.getWebServer()：总装车间](#3-tomcatservletwebserverfactorygetwebserver总装车间)
4. [prepareContext / configureContext：应用注册](#4-preparecontext--configurecontext应用注册)
5. [TomcatStarter：Spring 与 Servlet 规范的桥](#5-tomcatstarterspring-与-servlet-规范的桥)
6. [TomcatWebServer：两阶段启动的指挥者](#6-tomcatwebserver两阶段启动的指挥者)
7. [定制器体系：三层 Customizer 的接力](#7-定制器体系三层-customizer-的接力)
8. [生产定制：server.tomcat.* 配置 → 源码映射](#8-生产定制servertomcat-配置--源码映射)
9. [优雅关闭与虚拟线程](#9-优雅关闭与虚拟线程)
10. [本篇小结与面试要点](#10-本篇小结与面试要点)

---

## 1. 从 SpringApplication.run() 到端口监听：全链路总图

**这是全系列的终极地图**——把前 7 篇的知识点全部串起来：

```
SpringApplication.run()
  │
  ▼
AbstractApplicationContext.refresh()            [Spring 容器刷新]
  ├─ invokeBeanFactoryPostProcessors()          [自动装配：第 2 节]
  │    └─ ServletWebServerFactoryAutoConfiguration
  │         └─ 注册 TomcatServletWebServerFactory Bean（@ConditionalOnClass Tomcat）
  │
  ├─ registerBeanPostProcessors()               [定制器挂载：第 7 节]
  │    └─ WebServerFactoryCustomizerBeanPostProcessor（@Import 注册，最高优先级）
  │
  ├─ onRefresh()                                [★ 关键钩子：第 6 节]
  │    └─ ServletWebServerApplicationContext.onRefresh()   [ServletWebServerApplicationContext.java:164]
  │         └─ createWebServer()                [186]
  │              ├─ getWebServerFactory()       [找 TomcatServletWebServerFactory Bean]
  │              └─ factory.getWebServer(initializers)   [★ 第 3 节]
  │                   ├─ new Tomcat()           [Tomcat 空壳]
  │                   ├─ new Connector("...Http11NioProtocol")   [第 5 篇]
  │                   ├─ prepareContext()       [第 4 节]
  │                   ├─ configureContext()     [第 4 节]
  │                   └─ return new TomcatWebServer(tomcat)
  │                        └─ initialize()      [TomcatWebServer.java:110]
  │                             ├─ disableBindOnInit()          [第 5 篇]
  │                             ├─ tomcat.start()               [容器启动，第 1 篇]
  │                             │    └─ Context START_EVENT → removeServiceConnectors()  [第 5 篇]
  │                             └─ startNonDaemonAwaitThread()
  │
  ├─ finishRefresh()                           [★ 关键钩子：第 6 节]
  │    └─ lifecycleProcessor.onRefresh()       [SmartLifecycle 启动]
  │         └─ WebServerStartStopLifecycle.start()   [第 6 节]
  │              └─ webServer.start()          [TomcatWebServer.java:229]
  │                   ├─ addPreviouslyRemovedConnectors()   [★ 加回 Connector → bind 端口]
  │                   ├─ performDeferredLoadOnStartup()     [★ 延迟的 loadOnStartup]
  │                   └─ checkThatConnectorsHaveStarted()
  │
  └─ 发布 ServletWebServerInitializedEvent    ["Tomcat started on port 8080"]
```

**两个关键钩子**（面试必背）：

| 钩子 | 触发时机 | 干什么 |
|---|---|---|
| `onRefresh()` | 容器刷新早期 | **创建** WebServer（`getWebServer`）——但不启动端口 |
| `finishRefresh()` | 容器刷新末尾 | **启动** WebServer（`SmartLifecycle.start`）——绑定端口 |

> **面试点**：Spring Boot 为什么要把"创建 WebServer"和"启动 WebServer"分开？
> ——`onRefresh` 时所有 Bean 已注册但**未初始化完**；`finishRefresh` 时容器完全就绪。
> 分两步保证：**端口监听时应用上下文已完全初始化**（所有 @PostConstruct、Bean 都已就绪），
> 启动失败时可以在绑端口前抛出（不会留下"端口被占"的僵尸进程）。

---

## 2. 自动装配：三个配置类的接力

### 2.1 ServletWebServerFactoryAutoConfiguration

**源码**：`ServletWebServerFactoryAutoConfiguration.java`（169 行）

```java
@AutoConfiguration(after = SslAutoConfiguration.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ServletRequest.class)              // 有 Servlet API
@ConditionalOnWebApplication(type = Type.SERVLET)      // 是 Servlet 应用（非 reactive）
@EnableConfigurationProperties(ServerProperties.class) // ★ server.* 配置绑定
@Import({ ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
        ServletWebServerFactoryConfiguration.EmbeddedTomcat.class,
        ServletWebServerFactoryConfiguration.EmbeddedJetty.class,
        ServletWebServerFactoryConfiguration.EmbeddedUndertow.class })
public class ServletWebServerFactoryAutoConfiguration {

    @Bean
    public ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(...) {
        return new ServletWebServerFactoryCustomizer(serverProperties, ...);
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
    static class TomcatConfiguration {

        @Bean
        TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
                ServerProperties serverProperties) {
            return new TomcatServletWebServerFactoryCustomizer(serverProperties);
        }
    }
    ...
}
```

**BeanPostProcessorsRegistrar**（135-165 行）——**最高优先级注册定制器处理器**：

```java
public static class BeanPostProcessorsRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,
            BeanDefinitionRegistry registry) {
        // ★ 通过 @Import 在自动装配的最早期注册（其他 BeanPostProcessor 之前）
        registerSyntheticBeanIfMissing(registry, "webServerFactoryCustomizerBeanPostProcessor",
                WebServerFactoryCustomizerBeanPostProcessor.class);
        registerSyntheticBeanIfMissing(registry, "errorPageRegistrarBeanPostProcessor",
                ErrorPageRegistrarBeanPostProcessor.class);
    }
}
```

### 2.2 ServletWebServerFactoryConfiguration.EmbeddedTomcat

**源码**：`ServletWebServerFactoryConfiguration.java:63-80`

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Servlet.class, Tomcat.class, UpgradeProtocol.class })
@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
static class EmbeddedTomcat {

    @Bean
    TomcatServletWebServerFactory tomcatServletWebServerFactory(
            ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,
            ObjectProvider<TomcatContextCustomizer> contextCustomizers,
            ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        // ★ 收集用户自定义的三种 Customizer，注入 Factory
        factory.getTomcatConnectorCustomizers().addAll(connectorCustomizers.orderedStream().toList());
        factory.getTomcatContextCustomizers().addAll(contextCustomizers.orderedStream().toList());
        factory.getTomcatProtocolHandlerCustomizers().addAll(protocolHandlerCustomizers.orderedStream().toList());
        return factory;
    }
}
```

**条件链**（谁生效取决于 classpath）：

| 配置类 | 条件 | 生效场景 |
|---|---|---|
| `EmbeddedTomcat` | classpath 有 `Tomcat` | `spring-boot-starter-web`（默认） |
| `EmbeddedJetty` | classpath 有 `Jetty` | 排除 Tomcat 引入 Jetty |
| `EmbeddedUndertow` | classpath 有 `Undertow` | 排除 Tomcat 引入 Undertow |

### 2.3 三个 Customizer Bean 的注册

| Bean | 类 | 作用 |
|---|---|---|
| `servletWebServerFactoryCustomizer` | `ServletWebServerFactoryCustomizer` | 通用 `server.*`（port/context-path/SSL/session/compression...） |
| `tomcatServletWebServerFactoryCustomizer` | `TomcatServletWebServerFactoryCustomizer` | `server.tomcat.*` 生命周期类（redirect/mbean/apr/tld） |
| `tomcatWebServerFactoryCustomizer` | `TomcatWebServerFactoryCustomizer` | `server.tomcat.*` 协议/性能类（threads/connections/timeouts/accesslog） |

> **面试点**：三个 Customizer 的区别？——**职责分层**：
> `ServletWebServerFactoryCustomizer` 面向**所有** Servlet 容器（Tomcat/Jetty/Undertow 通用）；
> `TomcatServletWebServerFactoryCustomizer` 面向 **Tomcat 特有的生命周期配置**；
> `TomcatWebServerFactoryCustomizer` 面向 **Tomcat 的协议/网络配置**（在 `EmbeddedWebServerFactoryCustomizerAutoConfiguration` 注册，`@ConditionalOnClass(Tomcat.class)`）。

---

## 3. TomcatServletWebServerFactory.getWebServer()：总装车间

**源码**：`TomcatServletWebServerFactory.java:195-220`

```java
@Override
public WebServer getWebServer(ServletContextInitializer... initializers) {

    // ★ 1. 禁用 MBean Registry（第 1 篇 7.2）
    if (this.disableMBeanRegistry) {
        Registry.disableRegistry();
    }

    // ★ 2. 创建 Tomcat 空壳 + 设置 base 目录
    Tomcat tomcat = new Tomcat();
    File baseDir = (this.baseDirectory != null) ? this.baseDirectory : createTempDir("tomcat");
    tomcat.setBaseDir(baseDir.getAbsolutePath());

    // ★ 3. APR 生命周期监听器（可选，useApr 配置）
    for (LifecycleListener listener : getDefaultServerLifecycleListeners()) {
        tomcat.getServer().addLifecycleListener(listener);
    }

    // ★ 4. 创建 Connector（第 5 篇）：默认 Http11NioProtocol
    Connector connector = new Connector(this.protocol);   // DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol"
    connector.setThrowOnFailure(true);
    tomcat.getService().addConnector(connector);
    customizeConnector(connector);                        // ★ 端口/压缩/SSL/HTTP2 等
    tomcat.setConnector(connector);
    registerConnectorExecutor(tomcat, connector);         // 自定义 Executor 注册到 Service

    // ★ 5. 禁用自动部署（嵌入式不需要 Host 扫描）
    tomcat.getHost().setAutoDeploy(false);
    configureEngine(tomcat.getEngine());                  // backgroundProcessorDelay + engineValves

    // 附加 Connector（HTTPS 等）
    for (Connector additionalConnector : this.additionalTomcatConnectors) {
        tomcat.getService().addConnector(additionalConnector);
        registerConnectorExecutor(tomcat, additionalConnector);
    }

    // ★ 6. 准备 Context（第 4 节）
    prepareContext(tomcat.getHost(), initializers);

    // ★ 7. 包装成 TomcatWebServer（第 6 节）
    return getTomcatWebServer(tomcat);
}

protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
    return new TomcatWebServer(tomcat, getPort() >= 0, getShutdown());
}
```

**与第 1 篇 `Tomcat.getServer()` 懒构建的衔接**：
- `tomcat.getService()` → 触发 `getServer()`（创建 StandardServer + StandardService）
- `tomcat.getHost()` → 触发 `getEngine()` + `getHost()`
- `prepareContext()` → `host.addChild(context)`
- **此时所有组件是 NEW 状态**——真正启动在 `TomcatWebServer.initialize()` 的 `tomcat.start()`（第 1 篇 6）

---

## 4. prepareContext / configureContext：应用注册

### 4.1 prepareContext（235-271 行）

```java
protected void prepareContext(Host host, ServletContextInitializer[] initializers) {
    File documentRoot = getValidDocumentRoot();
    // ★ Spring Boot 自己的 Context 子类：延迟 loadOnStartup + 类加载器
    TomcatEmbeddedContext context = new TomcatEmbeddedContext();

    // 静态资源根（嵌入式：classpath:/META-INF/resources 等）
    WebResourceRoot resourceRoot = (documentRoot != null) ? new LoaderHidingResourceRoot(context)
            : new StandardRoot(context);
    context.setResources(resourceRoot);
    context.setName(getContextPath());
    context.setPath(getContextPath());
    context.setDocBase((documentRoot != null) ? documentRoot : createTempDir("tomcat-docbase").getAbsolutePath());

    // ★ FixContextListener（第 1 篇 3.3）：configured=true
    context.addLifecycleListener(new FixContextListener());

    // ★ 父类加载器 = Spring Boot 的 LaunchedURLClassLoader（第 7 篇）
    ClassLoader parentClassLoader = (this.resourceLoader != null) ? this.resourceLoader.getClassLoader()
            : ClassUtils.getDefaultClassLoader();
    context.setParentClassLoader(parentClassLoader);

    // ★ 自定义类加载器 + delegate=true（第 7 篇 6）
    WebappLoader loader = new WebappLoader();
    loader.setLoaderInstance(new TomcatEmbeddedWebappClassLoader(parentClassLoader));
    loader.setDelegate(true);
    context.setLoader(loader);

    // 默认 Servlet（registerDefaultServlet 配置）
    if (isRegisterDefaultServlet()) {
        addDefaultServlet(context);
    }
    // JSP Servlet（classpath 有 Jasper 才注册）
    if (shouldRegisterJspServlet()) {
        addJspServlet(context);
        addJasperInitializer(context);
    }

    context.addLifecycleListener(new StaticResourceConfigurer(context));
    ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
    host.addChild(context);                    // ★ 挂到 Host（此时不启动）
    configureContext(context, initializersToUse);
    postProcessContext(context);
}
```

**mergeInitializers 的三个内置 initializer**（`AbstractServletWebServerFactory.java:276-283`）：

```java
protected final ServletContextInitializer[] mergeInitializers(ServletContextInitializer... initializers) {
    List<ServletContextInitializer> mergedInitializers = new ArrayList<>();
    // ★ ① context-param：把 server.servlet.context-parameters 设置进 ServletContext
    mergedInitializers.add((servletContext) -> this.initParameters.forEach(servletContext::setInitParameter));
    // ★ ② Session 配置：超时/Cookie（server.servlet.session.*）
    mergedInitializers.add(new SessionConfiguringInitializer(this.session));
    mergedInitializers.addAll(Arrays.asList(initializers));   // ★ ③ selfInitialize（第 5 节）
    mergedInitializers.addAll(this.initializers);            // ★ ④ 用户 addInitializers 的
    return mergedInitializers.toArray(new ServletContextInitializer[0]);
}
```

所以 TomcatStarter 收到的数组 = **context-param + Session 配置 + selfInitialize + 用户自定义** 的合并体。

### 4.2 configureContext（403-433 行）

```java
protected void configureContext(Context context, ServletContextInitializer[] initializers) {
    // ★ TomcatStarter：ServletContainerInitializer 桥（第 5 节）
    TomcatStarter starter = new TomcatStarter(initializers);
    if (context instanceof TomcatEmbeddedContext embeddedContext) {
        embeddedContext.setStarter(starter);
        embeddedContext.setFailCtxIfServletStartFails(true);   // ★ Servlet 启动失败 → 整个应用失败
    }
    // ★ 编程式注册 SCI（不走 META-INF/services）
    context.addServletContainerInitializer(starter, NO_CLASSES);

    // 生命周期监听器（用户自定义）
    for (LifecycleListener lifecycleListener : this.contextLifecycleListeners) {
        context.addLifecycleListener(lifecycleListener);
    }
    // Context 级 Valve（用户自定义）
    for (Valve valve : this.contextValves) {
        context.getPipeline().addValve(valve);
    }
    // 错误页
    for (ErrorPage errorPage : getErrorPages()) {
        context.addErrorPage(tomcatErrorPage);
    }
    setMimeMappings(context);
    configureSession(context);              // Session 超时/Cookie（第 0 篇契约）
    configureCookieProcessor(context);      // SameSite 等
    new DisableReferenceClearingContextCustomizer().customize(context);
    // @WebListener 注解扫描的类注册
    for (String webListenerClassName : getWebListenerClassNames()) {
        context.addApplicationListener(webListenerClassName);
    }
    // 用户自定义 TomcatContextCustomizer
    for (TomcatContextCustomizer customizer : this.tomcatContextCustomizers) {
        customizer.customize(context);
    }
}
```

---

## 5. TomcatStarter：Spring 与 Servlet 规范的桥

**源码**：`TomcatStarter.java`（70 行，完整）

```java
/**
 * {@link ServletContainerInitializer} used to trigger {@link ServletContextInitializer
 * ServletContextInitializers} and track startup errors.
 */
class TomcatStarter implements ServletContainerInitializer {

    private final ServletContextInitializer[] initializers;

    private volatile Exception startUpException;

    TomcatStarter(ServletContextInitializer[] initializers) {
        this.initializers = initializers;
    }

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {
        try {
            // ★ 逐个调用 Spring 的 ServletContextInitializer
            for (ServletContextInitializer initializer : this.initializers) {
                initializer.onStartup(servletContext);
            }
        }
        catch (Exception ex) {
            // ★ 关键：异常不抛，存起来（主线程重新抛出）
            this.startUpException = ex;
            logger.error("Error starting Tomcat context. Exception: " + ex.getClass().getName() + ...);
        }
    }

    Exception getStartUpException() {
        return this.startUpException;
    }
}
```

**调用时机**：`StandardContext.startInternal()` 第 ⑳ 步
`ServletContainerInitializer.onStartup()`（第 1 篇 6.2）——`TomcatStarter.onStartup()` 被触发。

**传入的 initializers 是什么**（`ServletWebServerApplicationContext.getSelfInitializer`）：

```java
// ServletWebServerApplicationContext.java:237-248
private org.springframework.boot.web.servlet.ServletContextInitializer getSelfInitializer() {
    return this::selfInitialize;      // 方法引用
}

private void selfInitialize(ServletContext servletContext) throws ServletException {
    prepareWebApplicationContext(servletContext);      // 设置 ServletContext 属性
    registerApplicationScope(servletContext);          // 注册 application scope
    WebApplicationContextUtils.registerEnvironmentBeans(getBeanFactory(), servletContext);
    // ★ 所有 ServletContextInitializer Bean（ServletRegistrationBean/FilterRegistrationBean 等）
    for (ServletContextInitializer initializerBean : getServletContextInitializerBeans()) {
        initializerBean.onStartup(servletContext);
    }
}
```

**完整桥接链**：

```
StandardContext.startInternal() 第 ⑳ 步
  └─ TomcatStarter.onStartup()            [TomcatStarter（SCI 实现）]
       └─ selfInitialize()                [ServletWebServerApplicationContext（ServletContextInitializer）]
            ├─ prepareWebApplicationContext()
            ├─ registerApplicationScope()
            ├─ registerEnvironmentBeans()
            └─ 遍历 ServletContextInitializerBeans
                 ├─ ServletRegistrationBean → servletContext.addServlet()  ★ DispatcherServlet
                 ├─ FilterRegistrationBean  → servletContext.addFilter()   ★ CharacterEncodingFilter 等
                 └─ ServletListenerRegistrationBean → servletContext.addListener()
```

**异常桥接**（`TomcatWebServer.rethrowDeferredStartupExceptions`，196-212 行）：

```java
private void rethrowDeferredStartupExceptions() throws Exception {
    Container[] children = this.tomcat.getHost().findChildren();
    for (Container container : children) {
        if (container instanceof TomcatEmbeddedContext embeddedContext) {
            TomcatStarter tomcatStarter = embeddedContext.getStarter();
            if (tomcatStarter != null) {
                Exception exception = tomcatStarter.getStartUpException();
                if (exception != null) {
                    throw exception;        // ★ 主线程重新抛出（启动失败可见）
                }
            }
        }
        if (!LifecycleState.STARTED.equals(container.getState())) {
            throw new IllegalStateException(container + " failed to start");
        }
    }
}
```

> **面试点**：为什么 TomcatStarter 不直接抛异常？
> ——`onStartup()` 在 `StandardContext.startInternal()` 中调用，异常会导致 Context 启动失败，
> 但错误信息会被 Tomcat 的日志吞掉、且启动流程继续（`ok=false` 而已）。
> TomcatStarter **捕获并存储**异常，由 `TomcatWebServer.initialize()` 在 `tomcat.start()` 返回后
> **主线程重新抛出**——启动失败能立刻以异常形式暴露给 `SpringApplication.run()`。

---

## 6. TomcatWebServer：两阶段启动的指挥者

### 6.1 initialize()（第 5 篇 7 节已详讲，此处收尾）

`TomcatWebServer.java:110-150`：

```
initialize()
  ├─ addInstanceIdToEngineName()        [多实例时 Engine 名加后缀]
  ├─ 注册 Context START_EVENT 监听器     [触发时 removeServiceConnectors]
  ├─ disableBindOnInit()                [bindOnInit=false]
  ├─ tomcat.start()                     [★ 容器启动（端口不绑）]
  ├─ rethrowDeferredStartupExceptions() [★ TomcatStarter 异常重抛]
  ├─ ContextBindings.bindClassLoader()  [绑定类加载器]
  └─ startNonDaemonAwaitThread()        [★ 非守护线程保 JVM]
```

### 6.2 start()（228-258 行）——finishRefresh 触发

```java
@Override
public void start() throws WebServerException {
    synchronized (this.monitor) {
        if (this.started) {
            return;
        }
        try {
            addPreviouslyRemovedConnectors();      // ★ 加回 Connector → bind 端口（第 5 篇）
            Connector connector = this.tomcat.getConnector();
            if (connector != null && this.autoStart) {
                performDeferredLoadOnStartup();    // ★ 延迟的 loadOnStartup Servlet
            }
            checkThatConnectorsHaveStarted();      // Connector 状态检查
            this.started = true;
            logger.info(getStartedLogMessage());  // "Tomcat started on port 8080"
        }
        catch (ConnectorStartFailedException ex) {
            stopSilently();
            throw ex;
        }
        catch (Exception ex) {
            PortInUseException.throwIfPortBindingException(ex, ...);   // ★ 端口占用友好报错
            throw new WebServerException("Unable to start embedded Tomcat server", ex);
        }
    }
}
```

### 6.3 触发者：WebServerStartStopLifecycle（SmartLifecycle）

**源码**：`WebServerStartStopLifecycle.java`（servlet 版，约 60 行）

```java
class WebServerStartStopLifecycle implements SmartLifecycle {

    private final ServletWebServerApplicationContext applicationContext;
    private final WebServer webServer;
    private volatile boolean running;

    @Override
    public void start() {
        this.webServer.start();    // ★ TomcatWebServer.start()
        this.running = true;
        this.applicationContext.publishEvent(new ServletWebServerInitializedEvent(...));  // 发布启动事件
    }

    @Override
    public int getPhase() {
        // ★ 在默认 SmartLifecycle 之前启动（-1024 偏移）
        return WebServerGracefulShutdownLifecycle.SMART_LIFECYCLE_PHASE - 1024;
    }
}
```

**注册时机**（`createWebServer()`，`ServletWebServerApplicationContext.java:193-198`）：

```java
this.webServer = factory.getWebServer(getSelfInitializer());
...
getBeanFactory().registerSingleton("webServerGracefulShutdown",
        new WebServerGracefulShutdownLifecycle(this.webServer));
getBeanFactory().registerSingleton("webServerStartStop",
        new WebServerStartStopLifecycle(this, this.webServer));
```

> **面试点**：`SmartLifecycle` 在 Spring 刷新的哪个阶段触发？
> ——`finishRefresh()` → `lifecycleProcessor.onRefresh()` →
> 按 `getPhase()` 排序调用所有 `SmartLifecycle.start()`。
> `WebServerStartStopLifecycle.getPhase()` 用 `DEFAULT_PHASE - 1024` 保证**先于**其他
> SmartLifecycle（如消息监听器）启动 WebServer。

---

## 7. 定制器体系：三层 Customizer 的接力

### 7.1 WebServerFactoryCustomizerBeanPostProcessor

**源码**：`WebServerFactoryCustomizerBeanPostProcessor.java`（约 90 行）

```java
public class WebServerFactoryCustomizerBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof WebServerFactory webServerFactory) {
            // ★ 所有 WebServerFactory Bean 初始化前，应用所有 Customizer
            LambdaSafe.callbacks(WebServerFactoryCustomizer.class, getCustomizers(), webServerFactory)
                .invoke((customizer) -> customizer.customize(webServerFactory));
        }
        return bean;
    }

    private Collection<WebServerFactoryCustomizer<?>> getCustomizers() {
        // ★ 按 @Order 排序（AnnotationAwareOrderComparator）
        this.customizers.sort(AnnotationAwareOrderComparator.INSTANCE);
        ...
    }
}
```

**关键**：
- 它是 **BeanPostProcessor**——对每个 `WebServerFactory` Bean 在**初始化前**执行定制
- 定制器按 `@Order` 排序执行（`Ordered` 接口）
- 执行时机：`TomcatServletWebServerFactory` Bean 创建时（`postProcessBeforeInitialization`）

**定制器执行时序**：

```
TomcatServletWebServerFactory Bean 创建
  └─ postProcessBeforeInitialization
       ├─ ServletWebServerFactoryCustomizer.customize()    [order=0，通用 server.*]
       ├─ TomcatServletWebServerFactoryCustomizer.customize() [order=0，生命周期类]
       ├─ TomcatWebServerFactoryCustomizer.customize()       [order=0，协议/性能类]
       ├─ TomcatVirtualThreadsWebServerFactoryCustomizer     [order=1]
       └─ 用户自定义 WebServerFactoryCustomizer（@Order 控制）
            │
            ▼
getWebServer() ← 定制完成后的 Factory 被调用
```

### 7.2 定制器 → Factory 属性的传递

**ServletWebServerFactoryCustomizer**（`ServletWebServerFactoryCustomizer.java:78-100`）——PropertyMapper 映射：

```java
@Override
public void customize(ConfigurableServletWebServerFactory factory) {
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(this.serverProperties::getPort).to(factory::setPort);
    map.from(this.serverProperties::getAddress).to(factory::setAddress);
    map.from(this.serverProperties.getServlet()::getContextPath).to(factory::setContextPath);
    map.from(this.serverProperties.getServlet()::isRegisterDefaultServlet).to(factory::setRegisterDefaultServlet);
    map.from(this.serverProperties.getServlet()::getSession).to(factory::setSession);
    map.from(this.serverProperties::getSsl).to(factory::setSsl);
    map.from(this.serverProperties.getServlet()::getJsp).to(factory::setJsp);
    map.from(this.serverProperties::getCompression).to(factory::setCompression);
    map.from(this.serverProperties::getHttp2).to(factory::setHttp2);
    map.from(this.serverProperties::getServerHeader).to(factory::setServerHeader);
    map.from(this.serverProperties.getShutdown()).to(factory::setShutdown);
    ...
}
```

**TomcatWebServerFactoryCustomizer**（`TomcatWebServerFactoryCustomizer.java:88-156`）——协议类配置：

```java
@Override
public void customize(ConfigurableTomcatWebServerFactory factory) {
    ServerProperties.Tomcat properties = this.serverProperties.getTomcat();
    PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
    map.from(properties::getBasedir).to(factory::setBaseDirectory);
    map.from(properties::getBackgroundProcessorDelay).as(Duration::getSeconds).as(Long::intValue)
        .to(factory::setBackgroundProcessorDelay);
    customizeRemoteIpValve(factory);                       // ★ RemoteIpValve
    map.from(threadProperties::getMax).when(this::isPositive)
        .to((maxThreads) -> customizeMaxThreads(factory, maxThreads));        // AbstractProtocol.setMaxThreads
    map.from(threadProperties::getMinSpare).when(this::isPositive)
        .to((minSpareThreads) -> customizeMinThreads(factory, minSpareThreads));
    map.from(properties::getConnectionTimeout)
        .to((connectionTimeout) -> customizeConnectionTimeout(factory, connectionTimeout));
    map.from(properties::getMaxConnections).when(this::isPositive)
        .to((maxConnections) -> customizeMaxConnections(factory, maxConnections));   // AbstractProtocol.setMaxConnections
    map.from(properties::getAcceptCount).when(this::isPositive)
        .to((acceptCount) -> customizeAcceptCount(factory, acceptCount));
    map.from(properties::getKeepAliveTimeout)
        .to((keepAliveTimeout) -> customizeKeepAliveTimeout(factory, keepAliveTimeout));
    map.from(properties::getMaxKeepAliveRequests)
        .to((maxKeepAliveRequests) -> customizeMaxKeepAliveRequests(factory, maxKeepAliveRequests));
    map.from(properties::getAccesslog).when(Accesslog::isEnabled)
        .to((enabled) -> customizeAccessLog(factory));     // ★ AccessLogValve
    ...
    customizeStaticResources(factory);                     // ★ 静态资源缓存
    customizeErrorReportValve(this.serverProperties.getError(), factory);  // ★ ErrorReportValve
}
```

**通用委托方法**（`customizeHandler`，287-295 行）——**第 5 篇 3.3 属性委托链的应用**：

```java
private <T extends ProtocolHandler> void customizeHandler(ConfigurableTomcatWebServerFactory factory, int value,
        Class<T> type, ObjIntConsumer<T> consumer) {
    factory.addConnectorCustomizers((connector) -> {
        ProtocolHandler handler = connector.getProtocolHandler();
        if (type.isAssignableFrom(handler.getClass())) {
            consumer.accept(type.cast(handler), value);   // ★ 直接调 ProtocolHandler 的 setter
        }
    });
}
```

> **面试点**：`server.tomcat.threads.max` 如何到达底层？
> ——`ServerProperties`（配置绑定）→ `TomcatWebServerFactoryCustomizer.customizeMaxThreads`
> → `factory.addConnectorCustomizers(connector -> protocol.setMaxThreads(...))`
> → `AbstractProtocol.setMaxThreads`（`AbstractProtocol.java:247`）→ `Endpoint.setMaxThreads`
> → `ThreadPoolExecutor` 的 maximumPoolSize。**每个配置项都是一条完整的委托链**（第 5 篇 3.3）。

---

## 8. 生产定制：server.tomcat.* 配置 → 源码映射

### 8.1 完整映射表（面试核心）

| 配置项 | 默认值 | 定制器方法 | 底层目标（源码位置） |
|---|---|---|---|
| `server.port` | 8080 | `ServletWebServerFactoryCustomizer` | `Connector.setPort`（第 5 篇） |
| `server.address` | - | 同上 | `AbstractProtocol.setAddress` |
| `server.servlet.context-path` | "" | 同上 | `Context.setPath` |
| `server.tomcat.threads.max` | 200 | `customizeMaxThreads` | `AbstractProtocol.setMaxThreads` → `ThreadPoolExecutor`（第 6 篇） |
| `server.tomcat.threads.min-spare` | 10 | `customizeMinThreads` | `AbstractProtocol.setMinSpareThreads` → corePoolSize |
| `server.tomcat.threads.max-queue-capacity` | MAX | `customizeMaxQueueCapacity` | `AbstractProtocol.setMaxQueueSize` → `TaskQueue`（第 6 篇） |
| `server.tomcat.max-connections` | 8192 | `customizeMaxConnections` | `AbstractProtocol.setMaxConnections` → `LimitLatch`（第 6 篇） |
| `server.tomcat.accept-count` | 100 | `customizeAcceptCount` | `AbstractProtocol.setAcceptCount` → `ServerSocket.bind(backlog)`（第 5 篇） |
| `server.tomcat.connection-timeout` | 20000（20s，`SocketProperties.soTimeout`） | `customizeConnectionTimeout` | `AbstractProtocol.setConnectionTimeout` → `socketProperties.setSoTimeout` |
| `server.tomcat.keep-alive-timeout` | - | `customizeKeepAliveTimeout` | `AbstractProtocol.setKeepAliveTimeout` |
| `server.tomcat.max-keep-alive-requests` | 100 | `customizeMaxKeepAliveRequests` | `AbstractHttp11Protocol.setMaxKeepAliveRequests` |
| `server.tomcat.processor-cache` | 200 | `customizeProcessorCache` | `AbstractProtocol.setProcessorCache` → `SynchronizedStack`（第 6 篇） |
| `server.tomcat.max-http-form-post-size` | 2MB | `customizeMaxHttpFormPostSize` | `Connector.setMaxPostSize` |
| `server.tomcat.max-http-response-header-size` | 8KB | `customizeMaxHttpResponseHeaderSize` | `AbstractHttp11Protocol.setMaxHttpResponseHeaderSize` |
| `server.tomcat.max-swallow-size` | 2MB | `customizeMaxSwallowSize` | `AbstractHttp11Protocol.setMaxSwallowSize` |
| `server.tomcat.max-parameter-count` | 10000 | `customizeMaxParameterCount` | `Connector.setMaxParameterCount` |
| `server.tomcat.accesslog.enabled` | false | `customizeAccessLog` | **AccessLogValve**（Engine 阀门） |
| `server.tomcat.accesslog.pattern` | common | 同上 | `AccessLogValve.setPattern` |
| `server.tomcat.remoteip.*` | - | `customizeRemoteIpValve` | **RemoteIpValve**（Engine 阀门） |
| `server.tomcat.resource.cache-ttl` | - | `customizeStaticResources` | `StandardRoot.setCacheTtl`（Context 监听器） |
| `server.tomcat.basedir` | 临时目录 | `map.from(basedir)` | `Tomcat.setBaseDir` |
| `server.tomcat.background-processor-delay` | 10s | `map.from(...)` | `Engine.setBackgroundProcessorDelay`（第 1 篇后台线程） |
| `server.tomcat.uri-encoding` | UTF-8 | `map.from(uriEncoding)` | `Connector.setURIEncoding` |
| `server.tomcat.mbeanregistry.enabled` | false | `TomcatServletWebServerFactoryCustomizer` | `Registry.disableRegistry`（第 1 篇） |
| `server.tomcat.use-apr` | NEVER | `getUseApr` | `AprLifecycleListener` |
| `server.error.include-stacktrace` | never | `customizeErrorReportValve` | **ErrorReportValve**（`setShowServerInfo(false)`） |
| `server.shutdown` | **graceful**（3.5 默认） | `setShutdown` | `GracefulShutdown`（第 9 节） |
| `spring.threads.virtual.enabled` | false | `TomcatVirtualThreadsWebServerFactoryCustomizer` | `VirtualThreadExecutor`（第 9 节） |

### 8.2 三个典型定制的源码走读

**① 访问日志**（`customizeAccessLog`，327-349 行）：

```java
private void customizeAccessLog(ConfigurableTomcatWebServerFactory factory) {
    ServerProperties.Tomcat tomcatProperties = this.serverProperties.getTomcat();
    AccessLogValve valve = new AccessLogValve();     // ★ 创建 Valve
    PropertyMapper map = PropertyMapper.get();
    Accesslog accessLogConfig = tomcatProperties.getAccesslog();
    map.from(accessLogConfig.getConditionIf()).to(valve::setConditionIf);
    map.from(accessLogConfig.getPattern()).to(valve::setPattern);
    map.from(accessLogConfig.getDirectory()).to(valve::setDirectory);
    map.from(accessLogConfig.getPrefix()).to(valve::setPrefix);
    map.from(accessLogConfig.getSuffix()).to(valve::setSuffix);
    map.from(accessLogConfig.isRotate()).to(valve::setRotatable);
    map.from(accessLogConfig.getMaxDays()).to(valve::setMaxDays);
    ...
    factory.addEngineValves(valve);                  // ★ 加到 Engine 阀门链（第 2 篇）
}
```

**② RemoteIpValve**（`customizeRemoteIpValve`，230-257 行）：

```java
private void customizeRemoteIpValve(ConfigurableTomcatWebServerFactory factory) {
    Remoteip remoteIpProperties = this.serverProperties.getTomcat().getRemoteip();
    String protocolHeader = remoteIpProperties.getProtocolHeader();
    String remoteIpHeader = remoteIpProperties.getRemoteIpHeader();
    // ★ 配置了协议头/远程 IP 头，或在云平台（K8s/CF 自动探测）→ 启用
    if (StringUtils.hasText(protocolHeader) || StringUtils.hasText(remoteIpHeader)
            || getOrDeduceUseForwardHeaders()) {
        RemoteIpValve valve = new RemoteIpValve();
        valve.setProtocolHeader(StringUtils.hasLength(protocolHeader) ? protocolHeader : "X-Forwarded-Proto");
        if (StringUtils.hasLength(remoteIpHeader)) {
            valve.setRemoteIpHeader(remoteIpHeader);
        }
        valve.setTrustedProxies(remoteIpProperties.getTrustedProxies());
        valve.setInternalProxies(remoteIpProperties.getInternalProxies());
        ...
        factory.addEngineValves(valve);              // ★ Engine 阀门
    }
}
```

**③ 静态资源缓存**（`customizeStaticResources`，351-362 行）：

```java
private void customizeStaticResources(ConfigurableTomcatWebServerFactory factory) {
    ServerProperties.Tomcat.Resource resource = this.serverProperties.getTomcat().getResource();
    factory.addContextCustomizers((context) -> context.addLifecycleListener((event) -> {
        if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
            // ★ Context 配置阶段设置缓存（第 1 篇 6.2 的 ⑬ 步）
            context.getResources().setCachingAllowed(resource.isAllowCaching());
            if (resource.getCacheTtl() != null) {
                context.getResources().setCacheTtl(resource.getCacheTtl().toMillis());
            }
        }
    }));
}
```

> **面试点**：访问日志/RemoteIp 为什么用 Valve 实现？
> ——因为它们是**请求级横切**（每请求执行），且需要访问容器内部对象——
> 正是 Valve 的定位（第 2 篇）。`AccessLogValve` 加到 **Engine** 阀门链：
> 所有请求（无论哪个 Host/Context）都会经过。

---

## 9. 优雅关闭与虚拟线程

### 9.1 GracefulShutdown（`server.shutdown=graceful`，**3.5 默认即为 graceful**）

**源码**：`GracefulShutdown.java`（134 行，完整）

> **注意**：`ServerProperties.shutdown` 默认值是 `Shutdown.GRACEFUL`（`ServerProperties.java:116`）
> ——Spring Boot 3.x 已把优雅关闭设为默认！`TomcatWebServer` 构造时
> `shutdown == Shutdown.GRACEFUL` 才创建 `GracefulShutdown`（`TomcatWebServer.java:106`）。

```java
final class GracefulShutdown {

    void shutDownGracefully(GracefulShutdownCallback callback) {
        logger.info("Commencing graceful shutdown. Waiting for active requests to complete");
        CountDownLatch shutdownUnderway = new CountDownLatch(1);
        new Thread(() -> doShutdown(callback, shutdownUnderway), "tomcat-shutdown").start();
        try {
            shutdownUnderway.await();
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void doShutdown(GracefulShutdownCallback callback, CountDownLatch shutdownUnderway) {
        try {
            List<Connector> connectors = getConnectors();
            connectors.forEach(this::close);              // ★ 暂停 + 关 socket
            shutdownUnderway.countDown();
            awaitInactiveOrAborted();                     // ★ 等待活跃请求
            if (this.aborted) {
                callback.shutdownComplete(GracefulShutdownResult.REQUESTS_ACTIVE);
            }
            else {
                callback.shutdownComplete(GracefulShutdownResult.IDLE);
            }
        }
        finally {
            shutdownUnderway.countDown();
        }
    }

    private void close(Connector connector) {
        connector.pause();                                // ★ 暂停接受新请求
        connector.getProtocolHandler().closeServerSocketGraceful();  // ★ 优雅关闭 socket（第 5 篇 bindState）
    }

    private void awaitInactiveOrAborted() {
        try {
            for (Container host : this.tomcat.getEngine().findChildren()) {
                for (Container context : host.findChildren()) {
                    while (!this.aborted && isActive(context)) {
                        Thread.sleep(50);                 // ★ 50ms 轮询
                    }
                }
            }
        }
        catch (InterruptedException ex) { ... }
    }

    private boolean isActive(Container context) {
        // ★ 活跃判断：异步计数 > 0 或 已分配 Servlet 计数 > 0（第 4 篇 countAllocated！）
        if (((StandardContext) context).getInProgressAsyncCount() > 0) {
            return true;
        }
        for (Container wrapper : context.findChildren()) {
            if (((StandardWrapper) wrapper).getCountAllocated() > 0) {
                return true;
            }
        }
        return false;
    }
}
```

**触发链**：

```
server.shutdown=graceful
  → TomcatServletWebServerFactory.setShutdown(GRACEFUL)
  → new TomcatWebServer(tomcat, autoStart, GRACEFUL)
       → this.gracefulShutdown = new GracefulShutdown(tomcat)   [TomcatWebServer.java:106]
  → 关闭时：WebServerGracefulShutdownLifecycle（SmartLifecycle 负 phase）
       → gracefulShutdown.shutDownGracefully(callback)
```

**与第 4 篇的呼应**：`isActive()` 用 `StandardWrapper.getCountAllocated() > 0`
判断活跃请求——正是第 4 篇 `allocate()`/`deallocate()` 维护的计数！

### 9.2 虚拟线程（`spring.threads.virtual.enabled=true`）

**源码**：`TomcatVirtualThreadsWebServerFactoryCustomizer.java`（47 行，完整）

```java
/**
 * Activates {@link VirtualThreadExecutor} on {@link ProtocolHandler Tomcat's protocol
 * handler}.
 */
public class TomcatVirtualThreadsWebServerFactoryCustomizer
        implements WebServerFactoryCustomizer<ConfigurableTomcatWebServerFactory>, Ordered {

    @Override
    public void customize(ConfigurableTomcatWebServerFactory factory) {
        // ★ 给 ProtocolHandler 设置虚拟线程执行器（第 6 篇 createExecutor 的分支）
        factory.addProtocolHandlerCustomizers(
                (protocolHandler) -> protocolHandler.setExecutor(new VirtualThreadExecutor("tomcat-handler-")));
    }

    @Override
    public int getOrder() {
        return TomcatWebServerFactoryCustomizer.ORDER + 1;   // ★ 在性能定制器之后
    }
}
```

**注册条件**（`EmbeddedWebServerFactoryCustomizerAutoConfiguration.java:71-75`）：

```java
@Bean
@ConditionalOnThreading(Threading.VIRTUAL)   // spring.threads.virtual.enabled=true
TomcatVirtualThreadsWebServerFactoryCustomizer tomcatVirtualThreadsProtocolHandlerCustomizer() {
    return new TomcatVirtualThreadsWebServerFactoryCustomizer();
}
```

**影响**（对照第 6 篇）：

| | 平台线程（默认） | 虚拟线程 |
|---|---|---|
| 执行器 | `ThreadPoolExecutor`（10~200） | `VirtualThreadExecutor`（每任务一线程） |
| 线程数限制 | maxThreads=200 | **无限制**（虚拟线程廉价） |
| TaskQueue | 生效（扩线程策略） | 不涉及 |
| 适用 | 传统阻塞 IO 场景 | 高并发 IO 密集场景 |

> **面试点**：虚拟线程开启后 `server.tomcat.threads.max` 还有效吗？
> ——**不再限制**。`createExecutor()` 中 `getUseVirtualThreads()` 为 true 时直接用
> `VirtualThreadExecutor`（第 6 篇 3.3），不走 ThreadPoolExecutor + TaskQueue。
> `spring.threads.virtual.enabled=true` 时 Spring Boot 自动注册
> `TomcatVirtualThreadsWebServerFactoryCustomizer`（`@ConditionalOnThreading`）。

---

## 10. 本篇小结与面试要点

### 10.1 全系列地图（收官）

```
第 0 篇：Servlet 规范（契约）
第 1 篇：容器树 + Lifecycle（骨架）
第 2 篇：Pipeline-Valve（请求处理链）
第 3 篇：Mapper（路由）
第 4 篇：StandardWrapper（Servlet 生命周期）
第 5 篇：Connector + CoyoteAdapter（两层桥接）
第 6 篇：NIO 线程模型（网络心脏）
第 7 篇：类加载机制（隔离与覆盖）
第 8 篇（本篇）：Spring Boot 集成 + 生产定制（汇合）
```

### 10.2 面试要点速查

1. **两个钩子**：`onRefresh()` 创建 WebServer（不绑端口）、`finishRefresh()` 启动 WebServer（绑端口）
2. **自动装配三件套**：`ServletWebServerFactoryAutoConfiguration`（总开关）→ `EmbeddedTomcat`（条件创建 Factory）→ 三个 Customizer（配置注入）
3. **getWebServer 七步**：disableRegistry → new Tomcat → Connector → disableAutoDeploy → prepareContext → configureContext → TomcatWebServer
4. **TomcatStarter 桥**：SCI 实现，异常捕获存储 → 主线程重抛（`rethrowDeferredStartupExceptions`）
5. **selfInitialize**：prepareWebApplicationContext → registerApplicationScope → 遍历 ServletContextInitializer Beans（DispatcherServlet 注册）
6. **两阶段启动**：`removeServiceConnectors`（START_EVENT 摘走）+ `disableBindOnInit` → 容器先启动、端口后绑（第 5 篇）
7. **定制器执行**：BeanPostProcessor 在 Factory 初始化前按 @Order 应用；三层分工（通用/生命周期/协议）
8. **配置 → 源码映射**：每个 `server.tomcat.*` 都是一条委托链（第 8 节映射表）
9. **AccessLog/RemoteIp 是 Valve**：加在 Engine 阀门链（第 2 篇知识）
10. **优雅关闭**：pause → closeServerSocketGraceful → 轮询 `countAllocated`/`getInProgressAsyncCount`（第 4 篇计数！）
11. **虚拟线程**：`VirtualThreadExecutor` 替代 ThreadPoolExecutor，maxThreads 失效
12. **TomcatEmbeddedContext**：`loadOnStartup` 延迟 + `LazySessionIdGenerator` + starter 持有
    - `LazySessionIdGenerator`（`TomcatEmbeddedContext.java:58-62`）：覆写 `startInternal()` 为空——
      父类 `SessionIdGeneratorBase.startInternal()` 会**立即生成一个 Session ID**（触发 `SecureRandom` 初始化，
      熵收集很慢且阻塞）；Spring Boot 延迟到**第一次真正创建 Session 时**才初始化（启动性能优化）
