# 第 0 篇：Servlet 规范全景（前置知识篇）

> 本系列第 1~8 篇将深入分析嵌入式 Tomcat 的源码实现。
> 但 Tomcat 本质是 **Servlet 容器的参考实现**——它的代码就是 Servlet 规范的落地。
> 不理解契约（规范），就看不懂实现（Tomcat）。
>
> 本篇只讲 **Jakarta Servlet 6.0 规范**本身（Tomcat 10.1 实现的版本），
> 所有内容以 Tomcat 仓库中 `java/jakarta/servlet/**` 的规范 API 接口源码为依据，
> **不涉及任何 Tomcat 实现类**。看完本篇，你会带着一份"契约清单"进入源码分析。

---

## 目录

1. [Servlet 规范版本演进](#1-servlet-规范版本演进)
2. [Servlet 是什么：一次请求的生命周期全景](#2-servlet-是什么一次请求的生命周期全景)
3. [Servlet 生命周期契约](#3-servlet-生命周期契约)
4. [GenericServlet 与 HttpServlet](#4-genericservlet-与-httpservlet)
5. [ServletConfig 与 ServletContext](#5-servletconfig-与-servletcontext)
6. [Filter 责任链契约](#6-filter-责任链契约)
7. [Listener 监听器体系](#7-listener-监听器体系)
8. [可插拔机制：ServletContainerInitializer 与注解](#8-可插拔机制servletcontainerinitializer-与注解)
9. [请求分发：DispatcherType 与 RequestDispatcher](#9-请求分发dispatchertype-与-requestdispatcher)
10. [异步处理契约：AsyncContext](#10-异步处理契约asynccontext)
11. [Session 契约](#11-session-契约)
12. [规范接口 → Tomcat 实现类对照表](#12-规范接口--tomcat-实现类对照表)

---

## 1. Servlet 规范版本演进

Servlet 规范由 JCP（Java Community Process）制定，后移交 Eclipse Foundation（Jakarta EE）。版本演进：

| 规范版本 | Java EE 版本 | 发布年 | 关键特性 |
|---|---|---|---|
| Servlet 2.3 | J2EE 1.3 | 2001 | 引入 **Filter** 机制 |
| Servlet 2.4 | J2EE 1.4 | 2003 | `web.xml` 由 DTD 升级为 **XML Schema（XSD）** |
| Servlet 2.5 | Java EE 5 | 2006 | 注解起步（`@WebServlet` 尚未出现） |
| Servlet 3.0 | Java EE 6 | 2009 | **注解、可插拔性（SCI）、异步处理** 三大变革 |
| Servlet 3.1 | Java EE 7 | 2013 | **非阻塞 IO**（ReadListener/WriteListener）、协议升级 |
| Servlet 4.0 | Java EE 8 | 2017 | HTTP/2 支持 |
| Servlet 5.0 | Jakarta EE 9 | 2020 | `javax.servlet` → `jakarta.servlet` 包迁移 |
| Servlet 6.0 | Jakarta EE 10 | 2022 | `ServletConnection` API（`getRequestId()`/`getProtocolRequestId()`）、`HttpServlet.doHead` 默认行为变更（`LEGACY_DO_HEAD` 弃用开关）、`ServletRequest` 运行时标识方法 |

**关键转折点（面试常问）**：

- **Servlet 3.0（2009）**：在此之前，Web 应用必须写 `web.xml` 才能注册 Servlet/Filter/Listener。3.0 引入 `@WebServlet` 等注解和 `ServletContainerInitializer` 可插拔机制，**Spring Boot 的嵌入式 Tomcat 正是靠这套机制实现编程式注册**——`TomcatStarter` 就是一个 `ServletContainerInitializer`。
- **Servlet 3.1（2013）**：引入非阻塞 IO。Servlet 可以在不占用线程的情况下读取请求体/写入响应体（`ReadListener`/`WriteListener`）。
- **Servlet 5.0（2020）**：Oracle 将 Java EE 捐给 Eclipse 后，包名从 `javax.*` 强制迁移到 `jakarta.*`。**Tomcat 9 → 10 的唯一分界线**：Tomcat 9 支持 javax，Tomcat 10 支持 jakarta。这也是为什么 Spring Boot 3.x 强制要求 Tomcat 10。

> **面试点**：为什么 Spring Boot 3 不能用 Tomcat 9？因为 Tomcat 9 实现的是 `javax.servlet` 规范，Spring Boot 3 全面切换到 `jakarta.servlet`，两者包名不兼容。

---

## 2. Servlet 是什么：一次请求的生命周期全景

先建立宏观画面。一个 HTTP 请求进入 Web 服务器后，在 Servlet 容器视角下经历：

```
客户端 HTTP 请求
   │
   ▼
┌─────────────────────────────────────────────────────────────┐
│  Web 服务器（Tomcat Connector 层，第 5、6 篇详讲）              │
│  解析 TCP → HTTP 报文 → org.apache.coyote.Request            │
└─────────────────────────────────────────────────────────────┘
   │
   ▼
┌─────────────────────────────────────────────────────────────┐
│  容器层（Tomcat Catalina 层，第 1、3 篇详讲）                   │
│  Host 选择 → Context 选择 → Wrapper 选择                      │
│  根据 URL 找到目标 Web 应用（Context）和 Servlet（Wrapper）     │
└─────────────────────────────────────────────────────────────┘
   │
   ▼
┌─────────────────────────────────────────────────────────────┐
│  请求处理链（第 2 篇详讲）                                      │
│  Filter1.doFilter() → Filter2.doFilter() → ... → Servlet      │
└─────────────────────────────────────────────────────────────┘
   │
   ▼
  Servlet.service(req, res)  ← 业务代码在这里执行（Spring MVC DispatcherServlet）
   │
   ▼
  响应沿原路返回客户端
```

**Servlet 规范定义了这条链中每一个环节的"契约"**：
- `Filter` 定义拦截链如何工作
- `Servlet` 定义业务处理器如何被调用
- `ServletContext` 定义应用级资源如何共享
- `ServletRequest/ServletResponse` 定义请求/响应数据如何读写

Tomcat 的全部源码，就是对这些接口的实现。

---

## 3. Servlet 生命周期契约

**源码**：`jakarta/servlet/Servlet.java`（124 行，规范核心接口）

`Servlet` 是**所有 Servlet 必须实现的接口**，定义了完整的生命周期：

```java
public interface Servlet {

    // 1. 初始化：容器调用且【仅调用一次】
    //    必须成功返回后，Servlet 才能接收请求
    void init(ServletConfig config) throws ServletException;

    // 2. 配置：返回 init() 传入的 ServletConfig
    ServletConfig getServletConfig();

    // 3. 服务：处理请求，可被多线程并发调用
    //    【Servlet 是单实例多线程模型】——共享实例变量必须同步
    void service(ServletRequest req, ServletResponse res) throws ServletException, IOException;

    // 4. 信息：返回作者/版本等描述信息
    String getServletInfo();

    // 5. 销毁：容器在卸载 Servlet 前调用【仅一次】
    //    所有 service() 线程退出后（或超时后）才会调用
    void destroy();
}
```

**生命周期时序（规范强制）**：

```
构造（容器反射 newInstance）
   │
   ▼
init(config)   ← 仅一次，成功后才能服务请求
   │
   ▼
service(req, res)  ← 可多次，可并发（多线程同时调用）
   │
   ▼
destroy()  ← 仅一次，所有请求处理完（或超时）后调用
   │
   ▼
垃圾回收
```

三个"仅一次"和"并发"是理解 Tomcat `StandardWrapper` 源码的关键：

> **面试点**：Servlet 是单例还是多例？——**单例**。容器只创建一个实例，多个线程并发调用 `service()`。因此 Servlet 的实例字段不是线程安全的。`StandardWrapper` 中只有一个 `instance` 字段，配合 `countAllocated` 原子计数器管理并发访问（第 4 篇详讲）。

---

## 4. GenericServlet 与 HttpServlet

### 4.1 GenericServlet —— 协议无关的骨架

**源码**：`jakarta/servlet/GenericServlet.java`

```java
public abstract class GenericServlet implements Servlet, ServletConfig, java.io.Serializable { ... }
```

注意 `GenericServlet` **同时实现了 `Servlet` 和 `ServletConfig`**——它自己保存并返回 `ServletConfig`（`getServletConfig()` 返回的正是 `init()` 传入的那个对象）。提供与协议无关的基础能力：
- 存储 `ServletConfig`，提供 `getInitParameter()` / `getServletContext()` 便捷方法
- 默认空实现 `destroy()`、`getServletInfo()`
- 保留 `abstract void service()` 供子类实现
- 提供无参 `init()` 便捷方法（覆盖时不必处理 config）

### 4.2 HttpServlet —— HTTP 方法分发器

**源码**：`jakarta/servlet/http/HttpServlet.java`（1072 行）

`HttpServlet extends GenericServlet`，面向 HTTP 协议，核心是**方法分发**：

```
service(req, res)
   │  判断 HTTP method
   ├── GET     → doGet(req, res)
   ├── POST    → doPost(req, res)
   ├── PUT     → doPut(req, res)
   ├── DELETE  → doDelete(req, res)
   ├── HEAD    → doHead(req, res)  （默认走 doGet，但不写响应体）
   ├── OPTIONS → doOptions(req, res)（自动生成 Allow 头）
   └── TRACE   → doTrace(req, res)
```

几个规范细节：

- 默认实现**并非全部返回 405**，而是分三类：
  - `doGet`/`doPost`/`doPut`/`doDelete` → 返回 **405（Method Not Allowed）**。子类只需覆盖自己支持的方法
  - `doOptions` → 自动生成 `Allow` 响应头。实现细节：通过**反射扫描当前类实际覆写了哪些 `doXxx` 方法**来拼装 Allow 值并缓存（子类新增自定义方法也会被识别）
  - `doTrace` → **回显请求头**（跳过 `authorization`/`cookie`/`x-forwarded` 等敏感头）——TRACE 的反射型 XSS 风险（XST 攻击）正源于此，生产环境通常用安全配置禁用
- **HTTP/1.0 的特例**：`sendMethodNotAllowed()` 对 HTTP/0.9 和 HTTP/1.0 返回 **400**（而不是 405），因为 1.0 之前的协议没有 405 状态码
- **HEAD 的处理（Servlet 6.0 变更）**：`doHead()` 默认直接调用 `doGet()`，响应体由**容器负责丢弃**，只把头部发给客户端。这是当前唯一的默认行为——旧版（Servlet 6.0 之前）通过 `NoBodyResponse` 包装器计算 Content-Length 的做法已弃用（`LEGACY_DO_HEAD` 开关），**不必深入**。
- 覆盖 doGet 时建议同时覆盖 `getLastModified()`，支持条件请求（If-Modified-Since）优化——`service()` 中会先判断 `If-Modified-Since`，资源未变更时直接返回 304。

**Spring MVC 中的 DispatcherServlet 就是继承 HttpServlet 的**。所以一次 Spring Boot 请求最终调用的是 `DispatcherServlet.service()` → 内部按 HttpServlet 的分发机制进入 doGet/doPost。

> **面试点**：自定义 Servlet 时，为什么只重写 `doGet()`/`doPost()` 而不重写 `service()`？——`HttpServlet.service()` 负责 HTTP 方法分发，直接重写会丢失分发逻辑。

---

## 5. ServletConfig 与 ServletContext

### 5.1 ServletConfig —— 单个 Servlet 的配置

**源码**：`jakarta/servlet/ServletConfig.java`

```java
public interface ServletConfig {
    String getServletName();              // Servlet 名称（web.xml 中配置的 servlet-name）
    ServletContext getServletContext();   // 所属 Web 应用的上下文
    String getInitParameter(String name); // 该 Servlet 的初始化参数
    Enumeration<String> getInitParameterNames();
}
```

作用域：**每个 Servlet 一份**。初始化参数在注册时指定，`init()` 时注入。

### 5.2 ServletContext —— Web 应用全局上下文

**源码**：`jakarta/servlet/ServletContext.java`（938 行，规范中最庞大的接口）

一个 Web 应用（Context）对应**一个** `ServletContext` 实例，是 Web 应用的"全局户口本"：

**核心能力分组**：

| 能力 | 方法 | 用途 |
|---|---|---|
| 注册 Servlet | `addServlet(String, String/Class/Servlet)` | 编程式注册（Spring Boot 的核心入口） |
| 注册 Filter | `addFilter(...)` | 编程式注册 Filter |
| 注册 Listener | `addListener(...)` | 编程式注册监听器 |
| 映射 | `ServletRegistration.Dynamic.addMapping(...)`（在 addServlet 返回的 registration 上调用） | Servlet 路径映射 |
| 全局属性 | `setAttribute`/`getAttribute` | 应用级共享数据 |
| 资源访问 | `getResource`/`getResourceAsStream`/`getRealPath` | 读取 Web 应用内文件 |
| 请求分发 | `getRequestDispatcher(path)` | 获得分发器（forward/include） |
| 配置 | `getInitParameter`/`setInitParameter` | 应用级初始化参数 |
| 会话 | `getSessionTimeout`/`setSessionTimeout` | Session 配置 |
| 信息 | `getServerInfo()` | 容器信息（如 "Apache Tomcat/10.1.34"） |
| 异步开关 | `Registration.Dynamic.setAsyncSupported(...)`（定义于 `Registration.Dynamic`，`ServletRegistration.Dynamic` / `FilterRegistration.Dynamic` 均继承） | 是否支持异步 |

**Facade（门面）模式的重要应用**：

Servlet 容器为了避免应用代码直接操作内部实现，通常将 `ServletContext` 包装一层门面。Tomcat 中：
- 内部实现：`org.apache.catalina.core.ApplicationContext`
- 门面：`org.apache.catalina.core.ApplicationContextFacade`

`getServerInfo()` 返回的字符串来自 `org.apache.catalina.util.ServerInfo.properties`（`server.info=Apache Tomcat/@VERSION@`）。

> **注意区分**：`getServerInfo()` 与 HTTP 响应头 `Server:` 是**两个独立机制**：
> - `ServletContext.getServerInfo()` → `ServerInfo.properties` 中的 `server.info`，应用可读但**不可定制**
> - 响应头 `Server: Apache Tomcat/10.1.34` → 由 `server.tomcat.server-header` 配置，Spring Boot 通过 `connector.setProperty("server", ...)` 定制（见第 8 篇）

> **面试点**：`ServletContext` 与 Spring `ApplicationContext` 是什么关系？——两个完全不同的东西。前者是 Servlet 规范定义的 Web 应用容器上下文（Tomcat 维护）；后者是 Spring 的 IoC 容器（Spring 维护）。Spring Boot 通过 `WebApplicationContextUtils` 把两者桥接（第 8 篇详讲）。

---

## 6. Filter 责任链契约

**源码**：`jakarta/servlet/Filter.java`（105 行）+ `jakarta/servlet/FilterChain.java`

### 6.1 Filter 接口

```java
public interface Filter {

    // 初始化：容器调用【仅一次】
    default void init(FilterConfig filterConfig) throws ServletException {}

    // 过滤：每次请求经过时调用
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException;

    // 销毁：仅一次
    default void destroy() {}
}
```

### 6.2 FilterChain —— 链式调用的核心

```java
public interface FilterChain {
    // 调用链中的下一个 Filter；如果是最后一个，则调用目标 Servlet
    void doFilter(ServletRequest request, ServletResponse response)
            throws IOException, ServletException;
}
```

**责任链执行模型**：

```
                    ┌──────────────┐
   请求 ───────────►│ Filter1      │
                    │  doFilter()  │
                    │  前置逻辑     │
                    │  chain.dof() │──► 调用 Filter2
                    │  后置逻辑     │      │
                    │              │      ▼
                    │              │   ┌──────────────┐
                    │              │   │ Filter2      │
                    │              │   │  chain.dof() │──► Servlet
                    │              │   │  后置逻辑     │      │
                    │              │   └──────────────┘      ▼
                    │              │                     业务代码
                    └──────────────┘

响应沿调用栈反向返回（后置逻辑逆序执行）
```

**三个关键语义**：

1. **拦截**：Filter 可以**不调用** `chain.doFilter()`，请求被拦截，Servlet 不会执行。
2. **包装**：Filter 可以在调用链之前用 `ServletRequestWrapper`/`ServletResponseWrapper` 包装 request/response，实现请求改写、响应缓存等（装饰器模式）。
3. **顺序**：Filter 执行顺序按注册顺序决定（web.xml 中 `filter-mapping` 声明顺序，或编程式 `addFilter`/`addMappingForUrlPatterns` 的调用顺序）。Spring 生态中 `@Order` 注解/`FilterRegistrationBean.setOrder()` 是**框架层对注册顺序的包装**，规范本身不关心。

**Filter vs Servlet 生命周期对比**：

| | Filter | Servlet |
|---|---|---|
| 实例数 | 单例 | 单例 |
| 初始化 | `init()` 一次 | `init()` 一次 |
| 处理 | `doFilter()` | `service()` |
| 用途 | 横切逻辑（鉴权、日志、压缩） | 业务处理 |

> **面试点**：Spring 的 `CharacterEncodingFilter`、Spring Security 的 `FilterChainProxy`、Spring Boot Actuator 的过滤器，全部是 `Filter` 实现。`FilterChainProxy` 内部还嵌套了一层 Spring Security 自己的过滤器链——这就是"过滤器套过滤器"（规范只定义单层链，Spring 用"链中链"扩展）。

---

## 7. Listener 监听器体系

**源码**：`jakarta/servlet/*Listener.java` + `jakarta/servlet/http/*Listener.java`

监听器是 Servlet 规范的**事件驱动机制**。共 **9 个监听器接口**（8 个容器管理 + 1 个对象自身感知，见下）：

| 监听器 | 监听对象 | 触发时机 |
|---|---|---|
| `ServletContextListener` | 应用上下文 | `contextInitialized()` / `contextDestroyed()`（应用启动/关闭） |
| `ServletContextAttributeListener` | 应用属性 | 属性添加/删除/替换 |
| `ServletRequestListener` | 请求 | `requestInitialized()` / `requestDestroyed()`（每次请求开始/结束） |
| `ServletRequestAttributeListener` | 请求属性 | 属性添加/删除/替换 |
| `HttpSessionListener` | 会话 | `sessionCreated()` / `sessionDestroyed()` |
| `HttpSessionAttributeListener` | 会话属性 | 属性添加/删除/替换 |
| `HttpSessionIdListener` | 会话 ID | `sessionIdChanged()` |
| `HttpSessionActivationListener` | 会话序列化 | `sessionWillPassivate()` / `sessionDidActivate()` |

**`HttpSessionBindingListener`（第 9 个）比较特殊**——它不是容器管理的监听器（不需要注册），而是由**被绑定到 Session 的对象自身**实现，Session 写入/移除该对象时容器回调 `valueBound()`/`valueUnbound()`。它用于对象感知自己的绑定状态，比如记录"我当前在哪个 Session 中"。

**两个关键触发点（后续源码分析会用到）**：

- `ServletRequestListener.requestInitialized()`：在请求进入容器时触发——Tomcat 的 `StandardHostValve` 调用 `fireRequestInitEvent()`（第 2 篇）
- `ServletContextListener.contextInitialized()`：Web 应用启动时触发——由 `StandardContext.listenerStart()` 统一回调（第 1 篇）

> **面试点（易混淆）**：`WebApplicationInitializer` 与 `ServletContextInitializer` 是**两个不同的接口**：
> - `org.springframework.web.WebApplicationInitializer`（spring-web）：被 `SpringServletContainerInitializer`（SPI 机制）发现，**WAR 外置部署**用，`SpringBootServletInitializer implements WebApplicationInitializer`
> - `org.springframework.boot.web.servlet.ServletContextInitializer`（spring-boot）：被 `TomcatStarter` 调用，**嵌入式**用，`ServletRegistrationBean` 等都实现它
>
> 两者方法签名一样（都是 `onStartup(ServletContext)`），但属于不同模块、不同触发路径，面试时容易说混。

---

## 8. 可插拔机制：ServletContainerInitializer 与注解

**这是理解 Spring Boot 嵌入式 Tomcat 最关键的一节。**

### 8.1 ServletContainerInitializer（SCI）

**源码**：`jakarta/servlet/ServletContainerInitializer.java`（47 行）

```java
public interface ServletContainerInitializer {

    // 容器启动 Web 应用时，回调所有 SCI
    void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException;
}
```

**注册方式（SPI）**：SCI 实现类必须写在 jar 包的
`META-INF/services/jakarta.servlet.ServletContainerInitializer` 文件中（一行一个类名）。
容器启动时通过 `ServiceLoader` 机制自动发现并实例化。

**执行时机**：Web 应用启动过程中，容器在 web.xml/注解处理完成前后调用所有 SCI 的 `onStartup()`，传入 `ServletContext`，SCI 可以**编程式注册 Servlet/Filter/Listener**。

### 8.2 @HandlesTypes —— 类扫描的声明

```java
@HandlesTypes({WebApplicationInitializer.class})
public class SpringServletContainerInitializer implements ServletContainerInitializer { ... }
```

SCI 用 `@HandlesTypes` 声明"我感兴趣哪些类型"。容器会在启动时扫描 Web 应用的所有 classpath，
收集所有**实现了这些类型的类**，打包成 `Set<Class<?>>` 传给 `onStartup()`。

### 8.3 注解体系

Servlet 3.0 起支持注解注册，无需 web.xml：

| 注解 | 作用 |
|---|---|
| `@WebServlet(name, urlPatterns, initParams...)` | 注册 Servlet |
| `@WebFilter(urlPatterns, servletNames...)` | 注册 Filter |
| `@WebListener` | 注册 Listener |
| `@MultipartConfig` | 声明 Servlet 支持 multipart/form-data 上传 |
| `@WebInitParam` | Servlet/Filter 的初始化参数 |
| `@ServletSecurity` + `@HttpConstraint` | 声明安全约束 |

### 8.4 Spring Boot 是如何利用这套机制的？

这是**全系列最重要的一条线索**，先在这里埋下：

```
spring-web 模块（Spring Framework 的一部分，非 spring-boot 包）：
  META-INF/services/jakarta.servlet.ServletContainerInitializer
      └── org.springframework.web.SpringServletContainerInitializer
            │  @HandlesTypes(WebApplicationInitializer.class)
            │
            ▼
  SpringServletContainerInitializer.onStartup()
      └── 实例化所有 WebApplicationInitializer
            └── SpringBootServletInitializer（implements WebApplicationInitializer，onStartup → createRootApplicationContext）
```

注意 `SpringServletContainerInitializer` 的**准确归属**：
- 类全名：`org.springframework.web.SpringServletContainerInitializer`
- 所在模块：**spring-web**（Spring Framework），不在 spring-boot 仓库中
- spring-boot 侧只提供 `SpringBootServletInitializer`（实现 **`WebApplicationInitializer`**，用于 **WAR 外置部署**场景）

但**嵌入式模式不走这条链**！嵌入式模式下 Spring Boot 用 `TomcatStarter`（自己实现的 `ServletContainerInitializer`）替代了 SPI 发现机制，通过 `context.addServletContainerInitializer(starter, NO_CLASSES)` 编程式注册（此处的 `context` 是 **Tomcat 内部接口 `org.apache.catalina.Context`**，不是规范中的 `ServletContext`——这是 Tomcat 对规范 SPI 机制的扩展，第 8 篇详讲）。

> **面试点**：嵌入式 vs 外置（WAR 部署）的区别是什么？——嵌入式：Spring Boot 用 `TomcatStarter` 编程式注入 SCI，不走 `META-INF/services` 发现；外置：Spring Boot 依赖 `META-INF/services` SPI 机制由外部 Tomcat 发现 `SpringServletContainerInitializer`。

---

## 9. 请求分发：DispatcherType 与 RequestDispatcher

### 9.1 DispatcherType 枚举

**源码**：`jakarta/servlet/DispatcherType.java`

```java
public enum DispatcherType {
    FORWARD,   // RequestDispatcher.forward() 转发
    INCLUDE,   // RequestDispatcher.include() 包含
    REQUEST,   // 客户端发起的原始请求（normal, non-dispatched）
    ASYNC,     // AsyncContext.dispatch() 异步分发
    ERROR      // 错误处理机制（如定义的错误页）
}
```

**为什么 Filter 要区分 DispatcherType？**
Filter 的 `urlPatterns` 默认只拦截 `REQUEST` 类型。但如果一个请求被 `forward()` 到另一个 Servlet，
那个 Servlet 的执行也经过了 Filter 链——是否拦截取决于 Filter 注册时的 `dispatcherTypes` 配置。

**Spring Boot 中的典型例子**（第 8 篇会看到）：
`ForwardedHeaderFilter` 注册时显式指定了三种类型：
```java
registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ASYNC, DispatcherType.ERROR);
```

### 9.2 RequestDispatcher

**源码**：`jakarta/servlet/RequestDispatcher.java`

```java
public interface RequestDispatcher {
    // 转发：目标 Servlet 全权处理并输出响应
    void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException;
    // 包含：目标 Servlet 输出后，源 Servlet 继续
    void include(ServletRequest request, ServletResponse response) throws ServletException, IOException;
}
```

**forward vs include vs redirect 区别**（面试高频）：

| | forward | include | redirect |
|---|---|---|---|
| 方向 | 服务器内部 | 服务器内部 | 客户端重新发起 |
| URL 变化 | 不变 | 不变 | 变化 |
| 响应 | 由目标 Servlet 输出 | 目标输出后源 Servlet 继续 | 302 响应，浏览器重新请求 |
| 是否经过 Filter | 是（FORWARD 类型） | 是（INCLUDE 类型） | 新请求（REQUEST 类型） |

---

## 10. 异步处理契约：AsyncContext

**源码**：`jakarta/servlet/AsyncContext.java`（161 行）+ `ReadListener`/`WriteListener`

### 10.1 为什么需要异步？

Servlet 3.0 之前，一个请求**从头到尾占用一个容器线程**。如果业务需要等待外部资源
（数据库、远程调用、长轮询），线程一直被占用——容器线程池很快被耗尽。

异步处理允许：Servlet **先释放容器线程**，等外部资源就绪后**再完成响应**。

### 10.2 核心 API

```java
// Servlet 内开启异步（必须在容器线程内调用；startAsync 定义在 ServletRequest 接口上）
AsyncContext asyncContext = request.startAsync();

// 容器线程立即返回，业务线程可以任意提交任务
asyncContext.start(new Runnable() { ... });

// 业务完成后，异步提交响应
asyncContext.complete();          // 立即完成
asyncContext.dispatch("/target"); // 分发给另一个 Servlet 完成
```

### 10.3 非阻塞 IO（Servlet 3.1）

异步 + 非阻塞 IO 的组合是"全异步"：
- `ReadListener.onDataAvailable()`：请求体数据可读时回调
- `WriteListener.onWritePossible()`：响应通道可写时回调

这样**读请求体也不占用线程**（区别于 `getInputStream().read()` 阻塞读）。

**注意边界**：真正"全异步"的 **Spring WebFlux 默认基于 Reactor Netty，不依赖 Servlet 规范**（Servlet 兼容模式可跑在 Tomcat 上但非主流）。Servlet 异步机制主要服务：
- Spring MVC 的 `DeferredResult` / `ResponseBodyEmitter`（长轮询、SSE）
- 传统 Servlet 应用自己用 `AsyncContext` 做异步化

> **面试点**：Tomcat 如何处理异步？——`CoyoteAdapter.service()` 中检测到 `request.isAsync()` 后，**不回收 Request/Response 对象**（`if (!async) request.recycle()`），等待异步线程完成后通过 `AsyncContextImpl` 派发完成事件。第 2 篇会看到这段代码。

---

## 11. Session 契约

**源码**：`jakarta/servlet/http/HttpSession.java` + `SessionTrackingMode.java`

### 11.1 HttpSession 接口

```java
public interface HttpSession {
    Object getAttribute(String name);
    void setAttribute(String name, Object value);
    void removeAttribute(String name);
    void invalidate();           // 使会话失效
    boolean isNew();             // 是否新建（未与客户端确认）
    long getCreationTime();
    long getLastAccessedTime();
    int getMaxInactiveInterval(); // 最大不活动时间（秒）
    void setMaxInactiveInterval(int interval);
    // ...
}
```

### 11.2 Session 跟踪方式（SessionTrackingMode）

**源码**：`jakarta/servlet/SessionTrackingMode.java`

```java
public enum SessionTrackingMode {
    COOKIE,   // 通过 JSESSIONID Cookie（默认，也是现代唯一主流方式）
    URL,      // URL 重写（;jsessionid=xxx）——已过时，仅在 Cookie 被禁用时兜底
    SSL       // SSL 会话 ID —— 已过时，几乎不再使用
}
```

**会话 ID 传递**：容器把 Session ID 放到 `JSESSIONID` Cookie 中；客户端后续请求带上 Cookie，容器据此找回 Session。

> **面试点**：现代生产环境为什么只用 COOKIE？——URL 重写会把 Session ID 暴露在日志/浏览器历史中（安全风险），且破坏缓存与 SEO；SSL 跟踪依赖 TLS 会话复用，与 HTTP/2、连接池等现代特性冲突。两个都已过时，了解存在即可。

### 11.3 关键概念

- **过期**：`maxInactiveInterval` 秒内无访问，容器自动销毁 Session（Tomcat 中由 `StandardManager.backgroundProcess()` 周期扫描，第 8 篇涉及）
- **钝化/激活**：`HttpSessionActivationListener` 提供会话序列化前后的回调——这是**分布式容器（VM 间迁移会话）**的场景，嵌入式单机部署用不到，了解即可
- **并发**：Session 不是线程安全的，规范建议应用自行同步

---

## 12. 规范接口 → Tomcat 实现类对照表

**这是全系列的阅读地图**。读 Tomcat 源码时，先查这张表定位"这个类实现了哪个契约"。

| 规范接口 | Tomcat 实现类 | 所在篇 |
|---|---|---|
| `Servlet` | `StandardWrapper`（管理）；`DefaultServlet`（实现参考，**嵌入式 Spring Boot 默认不注册**） | 第 4 篇 |
| `ServletContext` | `ApplicationContext` + 门面 `ApplicationContextFacade` | 第 1/8 篇 |
| `ServletConfig` | `StandardWrapper`（自身实现）+ 门面 `StandardWrapperFacade` | 第 4 篇 |
| `ServletRequest` | `RequestFacade`（门面）→ `org.apache.catalina.connector.Request` | 第 2 篇 |
| `ServletResponse` | `ResponseFacade`（门面）→ `org.apache.catalina.connector.Response` | 第 2 篇 |
| `HttpServletRequest` | `Request`（实现）+ `RequestFacade`（门面） | 第 2 篇 |
| `Filter` | `ApplicationFilterConfig`（包装）+ `ApplicationFilterChain`（链） | 第 2 篇 |
| `FilterChain` | `ApplicationFilterChain` | 第 2 篇 |
| `ServletContextListener` | 由 `StandardContext.listenerStart()` 触发回调（注册经 `TomcatStarter`/注解扫描） | 第 1/8 篇 |
| `ServletRequestListener` | `StandardHostValve.fireRequestInitEvent()` 触发 | 第 2 篇 |
| `ServletContainerInitializer` | `StandardContext` 收集调用（第 1 篇）；Spring 侧 `TomcatStarter`（第 8 篇） | 第 1/8 篇 |
| `AsyncContext` | `AsyncContextImpl` + `CoyoteAdapter.asyncDispatch()` | 第 2 篇 |
| `HttpSession` | `StandardSession` + 门面 `StandardSessionFacade` | 第 8 篇 |
| `SessionCookieConfig` | `ApplicationSessionCookieConfig` | 第 8 篇 |
| `RequestDispatcher` | `ApplicationDispatcher` | 第 2 篇 |
| `ServletRegistration` | `ApplicationServletRegistration` | 第 1/8 篇 |
| `FilterRegistration` | `ApplicationFilterRegistration` | 第 1/8 篇 |

---

## 本篇小结

读完本篇，你应该能回答：

1. Servlet 的生命周期是什么？为什么它是单实例多线程模型？
2. `HttpServlet.service()` 如何分发 HTTP 方法？
3. Filter 链如何工作？`chain.doFilter()` 不调用会发生什么？
4. SCI + `@HandlesTypes` + `META-INF/services` 如何实现可插拔？
5. Spring Boot 嵌入式与外置部署在 SCI 发现机制上的区别？
6. `DispatcherType` 五种类型的区别？Filter 为什么关心它？
7. 异步处理为什么能释放容器线程？
8. `ServletContext` 与 Spring `ApplicationContext` 的区别？

带着这份契约清单，下一篇开始分析 Tomcat 如何用源码实现这些规范。
