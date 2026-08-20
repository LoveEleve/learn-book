# B-1-2 TomcatStarter + DispatcherServlet 注册 — 从 Spring Bean 到 Tomcat Wrapper

> 依赖 B-1-1 | 🟡 Working | 2 KP | [模式: 适配器 + 生命周期桥接]

**读者处境**: `ServletWebServerApplicationContext` 创建好了嵌入式容器，但 `DispatcherServlet` 具体是怎么被变成 Tomcat 里的 `Wrapper` 并映射到 `/` 的？`TomcatStarter` 和 `ServletContainerInitializer` 在这里各自做什么？

### 1. `TomcatStarter`：`ServletContextInitializer` 的批量桥接

场景: Spring Bean 中存在 `ServletRegistrationBean` / `DispatcherServletRegistrationBean` / `FilterRegistrationBean` 等组件，Tomcat 启动时需要把它们统一注册到 `ServletContext`。

源码路径:
- `TomcatStarter.java` — 实现 `ServletContainerInitializer`
- `onStartup(Set<Class<?>>, ServletContext)` → 遍历 `ServletContextInitializer` 列表 → 逐个调用 `initializer.onStartup(servletContext)`
- `ServletContextInitializerBeans` — 从 BeanFactory 收集所有 `ServletContextInitializer` Bean

关键设计: **Why 需要一个 TomcatStarter 桥？** Spring 容器只知道有一组 `ServletContextInitializer` Bean，Tomcat 只认 `ServletContainerInitializer`。`TomcatStarter` 正是这两者之间的适配器：它把 Spring Bean 列表翻译成一次 Tomcat 启动时的 `onStartup()` 批量注册动作。

### 2. `DispatcherServletRegistrationBean`：把 `DispatcherServlet` 变成 Tomcat 里的 Servlet 注册项

场景: Spring 中有一个 `DispatcherServlet` Bean，默认 URL mapping 是 `/`。Tomcat 需要拿到：
- servletName
- loadOnStartup
- URL patterns
- asyncSupported
- init parameters

源码路径:
- `DispatcherServletRegistrationBean`（spring-boot）— 默认注册 `DispatcherServlet`
- `ServletRegistrationBean` 父类：`onStartup(servletContext)` → `servletContext.addServlet(servletName, servlet)` → `registration.addMapping(urlMappings)`
- `TomcatServletWebServerFactory` 中通过 `TomcatStarter` 批量触发这些注册

关键设计: **Why `DispatcherServlet` 不能直接 new Wrapper 注入 Tomcat？** Spring Boot 不只管理 `DispatcherServlet`，还管理 Filter、Listener、ErrorPage 等注册项。用 `ServletRegistrationBean` 体系先统一描述“注册什么 + 映射到哪”，再由 `TomcatStarter` 批量翻译给 Tomcat，才能保持一种一致的注册模型。

→ 引出 B-2-1: `@SpringBootApplication` → `SpringApplication.run()` 的装配总入口。
