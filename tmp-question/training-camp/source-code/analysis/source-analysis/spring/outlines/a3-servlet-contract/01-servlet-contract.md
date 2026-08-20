# A-3 Servlet 规范与 Spring MVC — `HttpServlet.service()`、`Filter` 与 `DispatcherServlet` 的契约边界

> 依赖 W-1 DispatcherServlet | 🟡 Working | 2 KP | [模式: 适配器]

**读者处境**: `DispatcherServlet` 是一个 `HttpServlet`，但它内部的 `doDispatch` 完全是 Spring MVC 的逻辑——哪些是 Servlet 规范要求的，哪些是 Spring 自己的设计？

### 1. `HttpServlet.service()` — 规范要求的请求分发入口

场景: Tomcat 调用 `DispatcherServlet.service(request, response)` — Servlet 规范要求 `service()` 根据 HTTP 方法分发到 `doGet` / `doPost` 等。

源码路径:
- `FrameworkServlet.java` — `service()` 覆写，判断是否需要 `processRequest`
- `DispatcherServlet.java` — `doDispatch(...)` 是 Spring MVC 的调度核心

关键设计: **Why `DispatcherServlet` 覆写 `service()` 而不是 `doGet()`/`doPost()`？** Servlet 规范的 `service()` 是统一入口，子类覆写 `doGet`/`doPost` 是常见做法。但 Spring 选择在 `FrameworkServlet.service()` 层统一拦截，把所有 HTTP 方法都收进 `processRequest`，再由 `doDispatch` 统一调度。这样 MVC 不需要为每个 HTTP 方法写独立入口。

### 2. Servlet 规范的 `Filter` 与 Spring MVC 的 `HandlerInterceptor`

场景: `Filter` 在 Servlet 容器层执行，在 `DispatcherServlet` 之前。`HandlerInterceptor` 在 Spring MVC 层执行，在 `doDispatch` 内部。

关键差异:
- `Filter`：Servlet 规范定义，在请求进入 Servlet 之前执行，不知道 Spring MVC 的 handler 信息
- `HandlerInterceptor`：Spring MVC 定义，在 `doDispatch` 内部执行，可以拿到 `HandlerMethod`、`ModelAndView`
- `Filter` 的 `doFilter()` 是 Servlet 容器调用
- `HandlerInterceptor` 的 `preHandle/postHandle/afterCompletion` 是 `DispatcherServlet` 调用

关键设计: **Why 两层拦截？** `Filter` 更底层，适合编码、CORS、安全头等 Servlet 容器级逻辑。`HandlerInterceptor` 更上层，适合登录校验、审计、埋点等依赖 handler 语义的逻辑。两层不是替代关系，而是不同抽象层次的拦截面。

### 3. Servlet 规范的 `ServletContext` / `HttpSession` / `ServletRequest` 与 Spring 的桥接

场景: Spring 的 `RequestContextHolder`、`WebApplicationContext`、Web Scope（`request` / `session`）都建立在 Servlet 规范之上。

关键桥接:
- `RequestContextHolder` 通过 `ServletRequestAttributes` 桥接到 `HttpServletRequest`
- `RequestScope` / `SessionScope` 通过 `ServletRequestAttributes` 桥接到 `HttpServletRequest.getSession()`
- `GenericWebApplicationContext` 需要 `ServletContext` 才能注册 Web Scope

关键设计: **Why Spring 不自己实现 HTTP 语义？** Spring MVC 建立在 Servlet 规范之上，而不是重新实现 HTTP 层。它通过 `DispatcherServlet` 桥接到 Servlet API，再通过 `HandlerAdapter` / `HandlerMapping` 把请求推进到 Spring 的 handler 世界。这让 Spring MVC 可以运行在任何 Servlet 容器（Tomcat、Jetty、Undertow）上。

→ 引出 B-1: DispatcherServlet 如何接入 Tomcat（从 `ServletWebServerApplicationContext` 到 `TomcatWebServer`）。
