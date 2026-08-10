# T-1 §4 末级容器 — Engine/Host/Context/Wrapper 的差异化角色 + 规范映射

> 依赖 §3 | 🟡 Working | 4 KP | 规范映射表

**读者处境**: 已理解 5 层容器树的通用机制(Lifecycle+addChild+ReadWriteLock)和顶层编排(Server+Service)。但问题浮现: "既然 Engine 和 Host 都是 ContainerBase 的子类——代码一样——那四层容器到底有什么区别？" 区别在于: **每层的 addChild 类型约束、特有配置域、backgroundProcess 实现**——还有最关键——每层与 Servlet 规范的对应关系。

### 1. StandardEngine — defaultHost + Mapper 传播 + backgroundProcess=10

场景: 请求经过 Connector→CoyoteAdapter——Adapter 拿到 Request——需要知道"这个请求的 Host: header 对应哪个 Host 容器?" ——但如果 Host: 头为空(HTTP/1.0)——回退到 defaultHost。defaultHost 由 Engine 持有并直接写入 Mapper。

源码路径:
- `StandardEngine.java:107-127` — `setDefaultHost(host)`: `this.defaultHost = host` + `service.getMapper().setDefaultHostName(host)` — **唯一的直接调 Mapper 的容器**。其余容器通过 MapperListener 的 ContainerEvent 间接更新
- `StandardEngine.java:195-230` — `startInternal()`: log→getRealm()→super.startInternal()→检查 `findChild(getDefaultHost()) != null`——默认 Host 必须在 Engine 启动前加入
- `StandardEngine.java:64` — `backgroundProcessorDelay=10` — Engine 层每 10 秒触发 backgroundProcess

关键设计: **Why Engine 直接调 Mapper？** MapperListener 通过 ContainerEvent 异步更新路由表——存在窗口期: Host 已 `addChild` 但 `ADD_CHILD_EVENT` 还没被 MapperListener 处理——此时请求到达——Mapper 查不到新 Host——返回 404。但 `setDefaultHost` 是同步调用——setter 内直接 `mapper.setDefaultHostName()`——立即生效——消除了 defaultHost 的路由窗口期。这是**同步 vs 异步在关键路径上的取舍**: defaultHost 太关键(每个 HTTP/1.0 请求都用它)——必须同步——其余路由更新可承受窗口延迟。 [模式: Strategy — Mapper 更新策略: 关键(sync) vs 常规(async)]

→ 实现规范: `Engine` → Servlet 规范 2.3 §9.2.1 — Engine 代表整个 Catalina Servlet 引擎，处理所有请求的入口。一个 Service 只有一个 Engine。

数据流: `request.getHost(): "example.com"`→CoyoteAdapter→`connector.getService().getMapper().map(host, uri)`→在 Mapper 中查找→Host 未注册→回退到 `mapper.defaultHostName`→找到 "localhost"→Mapper 返回 Host+Context+Wrapper 映射→Adapter 调用 `host.getPipeline().getFirst().invoke(request, response)`。

### 2. StandardHost — appBase + autoDeploy + ErrorReportValve

场景: 开发阶段——程序员把 WAR 放到 webapps/——Tomcat 自动检测 WAR 变化——解压→部署→热重载。`autoDeploy=true` 是 Tomcat 的开发体验核心——生产环境通常关闭它在启动时一次性部署。

源码路径:
- `StandardHost.java:88` — `appBase = "webapps"` — 所有 Context 应用的根目录
- `StandardHost.java:112` — `autoDeploy = true` — 后台线程定期扫描 appBase 变化，检测到→部署/重部署
- `StandardHost.java:130` — `deployOnStartup = true` — 启动时扫描 appBase 目录，每个 WAR/目录创建一个 Context→`host.addChild(context)`
- `StandardHost.java:149` — `errorReportValveClass` = `ErrorReportValve` — 配置错误页面的 Valve 类名

关键设计: **Why autoDeploy 在 Host 层？** Context 代表一个 Web 应用——Host 代表一个虚拟主机——一个 Host 下有多个 Context。部署粒度是 Context——Host 是自然的管理者。如果 autoDeploy 在 Engine 层——Engine 需要遍历所有 Host→再扫描每个 Host 的 Context——两层嵌套。Host 层管理自己的 appBase——职责单一清晰。

→ 实现规范: `Host` → Servlet 规范 2.3 §9.2.2 — Host 代表一个虚拟主机(域名)。在 Tomcat 中对应 `server.xml` 的 `<Host name="localhost" appBase="webapps">` 配置。规范说 Host 可以有多个 Context——Tomcat 实现为 children Map 中的 Context 条目。

数据流: `deployOnStartup=true`→`Host.startInternal()`→扫描 `appBase` 目录→发现 `ROOT/`,`app.war`,`manager/`→为每个创建 `StandardContext` 实例→`context.setDocBase("webapps/ROOT")`→`host.addChild(context)`→addChild 触发 MapperListener→Mapper 建立路由映射→context.start()→加载 web.xml→Servlet 初始化→loadOnStartup Servlet。

### 3. StandardContext — Wrapper 子容器 + docBase + 规范核心映射点

场景: 一个 Web 应用有 20 个 Servlet——每个 Servlet 映射为一个 Wrapper——Context 是这些 Wrapper 的父容器。同时 Context 持有 `ServletContext` 实现(ApplicationContext)——Servlet 规范说 `ServletContext` 是"一个 Web 应用"的代表——这正好对应 Context 容器。

源码路径:
- StandardContext extends ContainerBase — children 为 Wrapper 容器集合
- `StandardContext.getServletContext()` — 返回 `ApplicationContext` 实例 — **这是规范与实现的核心映射点**
- `docBase` — Web 应用根目录(如 "webapps/ROOT")
- `reloadable` — 监控 /WEB-INF/classes 和 /WEB-INF/lib 变化→classLoader 变更→自动重载

关键设计: **Context 是规范映射最密集的容器**: `getServletContext()`→实现规范: `ServletContext`(1.0)/`addServlet()`→实现规范: `ServletRegistration.Dynamic`(3.0)/`addFilter()`→实现规范: `FilterRegistration.Dynamic`(3.0)。Context 不仅管 Wrapper 层次——它是 Servlet 规范中整个 Web 应用的代表——`web.xml` 的 `<context-param>`、`<listener>`、`<error-page>` 都映射为 Context 的属性。**T-1 不讲 Filter 和 web.xml 解析**——那是 T-3 Pipeline 的职责——这里聚焦 Context 作为"5 层容器的第 4 层"的角色。

→ 实现规范: `Context` → `ServletContext` (1.0) / `ServletRegistration` (3.0) / `FilterRegistration` (3.0)。Servlet 规范的一个 Web 应用 = 一个 Context，Context 包含多个 Wrapper(每个 Servlet 一个)。

数据流: `host.addChild(context)`→context 被加入 Host 的 children Map→context.start()→`Context.startInternal()`→读取 web.xml→创建 Wrappers(每个 `<servlet>` 一个 StandardWrapper)→`context.addChild(wrapper)` 加入 children→按 loadOnStartup 排序启动 Servlets→`getServletContext()` 的 ApplicationContext 就绪→`context STARTED`。

### 4. StandardWrapper — 末级容器 + servletClass/loadOnStartup + loadServlet

场景: 容器层次的终点——Wrapper 下面没有子容器。它的职责是: 管理一个 Servlet 实例的完整生命周期——`loadServlet()`(init)→多线程 service()→`unloadServlet()`(destroy)。Wrapper 是 Tomcat 容器模型与 Servlet 生命周期的最终交汇点。

源码路径:
- StandardWrapper extends ContainerBase — **末级容器——不能覆写 addChild**(children=空)
- `servletClass`: Servlet 的全限定类名
- `loadOnStartup`: 控制启动时是否加载 Servlet——值越小越先加载，负数=延迟加载
- `loadServlet()`: `Class.forName(servletClass)`→`servlet.init(servletConfig)`→返回实例
- `unloadServlet()`: `servlet.destroy()`→释放引用→等待 GC
- `asyncSupported`: 标记支持异步处理 → 实现规范: Servlet 3.0+ AsyncContext

关键设计: **SingleInstance vs InstancePool** — 默认 Wrapper 创建一个 Servlet 实例，所有请求线程共享(servlet 必须是线程安全的)。如果 Servlet 实现 `SingleThreadModel`(已废弃)——Wrapper 创建实例池(默认 20 个)——每次请求取一个实例——用完放回(相当于对象池)。**但 SingleThreadModel 在 Servlet 2.4 已废弃**——因为多实例不能解决"静态变量线程安全"问题——且多实例破坏 Servlet 设计意图(单实例全局状态)。Tomcat 保留兼容但标注废弃。

→ 实现规范: `Wrapper` → `Servlet`(1.0) / `ServletConfig`(1.0) / `@WebServlet`(3.0) / `AsyncContext`(3.0) / `MultipartConfig`(3.0)。Wrapper 是规范中 "Servlet" 定义在容器模型中的对应容器。

数据流: 请求到达 Context→`Context.getPipeline().getFirst().invoke()`→Pipeline 遍历 Valve→StandardContextValve→根据 URL 匹配到 Wrapper→`wrapper.getPipeline().getFirst().invoke()`→StandardWrapperValve→`wrapper.allocate()`(获取 Servlet 实例)→`servlet.service(request, response)`→`wrapper.deallocate()`(归还实例)→响应返回。

### 规范映射总表

| 规范接口 (Jakarta Servlet 6.0) | Tomcat 实现类 | 引入版本 | 容器层 |
|------|------|:--:|:--:|
| `jakarta.servlet.Servlet` | `StandardWrapper.loadServlet()` 实例化 | 1.0 | Wrapper |
| `jakarta.servlet.ServletConfig` | `StandardWrapperFacade` | 1.0 | Wrapper |
| `jakarta.servlet.ServletContext` | `ApplicationContext` | 1.0 | Context |
| `jakarta.servlet.ServletRegistration.Dynamic` | `ApplicationServletRegistration` | 3.0 | Context |
| `jakarta.servlet.FilterRegistration.Dynamic` | `ApplicationFilterRegistration` | 3.0 | Context |
| `jakarta.servlet.AsyncContext` | `AsyncContextImpl` | 3.0 | Wrapper |
| `jakarta.servlet.http.HttpUpgradeHandler` | WebSocket 升级 | 3.1 | Wrapper |

→ 引出 T-2 Connector+Adapter — 容器层次的全部 5 层(Engine→Host→Context→Wrapper)已可路由请求——但现在的问题是: "请求的字节流从 TCP 端口进入后——谁把它变成 Request 对象——谁把它交给 Engine?" T-1 关注容器内部结构——T-2 关注容器的外部接口: Connector(接收 TCP 字节流)→CoyoteAdapter(字节→Request/Response 对象转换)→Engine.pipeline.invoke(交给容器层次处理)。
