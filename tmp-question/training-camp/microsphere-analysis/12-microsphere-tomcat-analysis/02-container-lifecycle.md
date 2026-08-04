# 第 1 篇：架构总览 + 容器层次 + Lifecycle 状态机

> 上一篇（第 0 篇）建立了 Servlet 规范的契约全景。本篇开始进入 Tomcat 源码。
> 这是全系列的**架构地图**：理解 Tomcat 的分层设计、容器树结构，以及贯穿所有组件的生命周期机制。
> 后续所有篇章（Valve 链、Mapper、Connector、NIO、类加载）都建立在这套骨架之上。
>
> **源码范围**：`org.apache.catalina.*` 核心包
> - `Lifecycle.java` / `LifecycleState.java` / `LifecycleBase.java` / `LifecycleMBeanBase.java`
> - `Container.java` / `ContainerBase.java`
> - `StandardServer` / `StandardService` / `StandardEngine` / `StandardHost` / `StandardContext`
> - `org.apache.catalina.startup.Tomcat`（嵌入式入口）
>
> **本篇定位**：Tomcat 核心层（Standalone 与嵌入式共享的代码），Spring Boot 特有部分会在第 8 篇展开。

---

## 目录

1. [Tomcat 总体架构：两个世界](#1-tomcat-总体架构两个世界)
2. [容器树：Server → Service → Engine → Host → Context → Wrapper](#2-容器树server--service--engine--host--context--wrapper)
3. [嵌入式入口：Tomcat 类如何一键构建容器树](#3-嵌入式入口tomcat-类如何一键构建容器树)
4. [Lifecycle 生命周期机制：Tomcat 的灵魂](#4-lifecycle-生命周期机制tomcat-的灵魂)
5. [ContainerBase：容器树的基座](#5-containerbase容器树的基座)
6. [一次 start() 的完整级联：从 Tomcat.start() 到 Servlet](#6-一次-start-的完整级联从-tomcatstart-到-servlet)
7. [LifecycleMBeanBase：MBean 注册（嵌入式中被禁用）](#7-lifecyclembeanbasembean-注册嵌入式中被禁用)
8. [本篇小结与面试要点](#8-本篇小结与面试要点)

---

## 1. Tomcat 总体架构：两个世界

Tomcat 的代码分为两个大的世界，它们在 `CoyoteAdapter` 处交汇：

```
┌──────────────────────────────────────────────────────────────────────┐
│  Catalina 世界（容器层）—— 第 1~4 篇                                    │
│  org.apache.catalina.*                                                 │
│                                                                        │
│  Server → Service → Engine → Host → Context → Wrapper                  │
│  实现 Servlet 规范：管理 Servlet 生命周期、Filter 链、路由               │
└──────────────────────────────┬───────────────────────────────────────┘
                               │ CoyoteAdapter（桥梁）
┌──────────────────────────────┴───────────────────────────────────────┐
│  Coyote 世界（连接器层）—— 第 5~6 篇                                    │
│  org.apache.coyote.*  /  org.apache.tomcat.util.net.*                 │
│                                                                        │
│  Connector → ProtocolHandler → Endpoint                                 │
│  处理网络 IO：HTTP 解析、NIO、TLS、keep-alive                            │
└──────────────────────────────────────────────────────────────────────┘
```

**两个世界的分工**：

| | Catalina（容器层） | Coyote（连接器层） |
|---|---|---|
| 包前缀 | `org.apache.catalina` | `org.apache.coyote` / `org.apache.tomcat.util.net` |
| 职责 | 实现 Servlet 规范：容器、生命周期、请求路由 | 网络通信：socket、HTTP 协议解析、IO 模型 |
| 面向 | Servlet 规范 | 传输协议（HTTP/1.1、HTTP/2、AJP） |
| 代表类 | `StandardEngine`、`StandardContext`、`CoyoteAdapter` | `Connector`、`Http11NioProtocol`、`NioEndpoint` |
| 请求对象 | `org.apache.catalina.connector.Request` | `org.apache.coyote.Request` |

**一个请求跨越两个世界**（第 0 篇全景图的源码版）：

```
客户端 → NioEndpoint（Coyote：读 socket，解析 HTTP 报文）
       → Http11Processor（Coyote：生成 org.apache.coyote.Request）
       → CoyoteAdapter.service(coyoteRequest, coyoteResponse)   ← 桥梁
       → 转换为 catalina Request/Response
       → 容器层 Pipeline 处理（Engine → Host → Context → Wrapper）
       → Servlet.service()（业务代码）
```

> **面试点**：为什么 Tomcat 要拆成两个世界？——关注点分离。Coyote 层不关心 Servlet 规范（它可以只做协议处理），Catalina 层不关心网络细节（它可以只做容器管理）。更换协议（HTTP→AJP）只需更换 Coyote 实现；更换容器实现只需更换 Catalina 实现。`CoyoteAdapter` 是两者**最主要的粘合点**（`Connector` 也同时接触两边，第 5 篇详讲）。

---

## 2. 容器树：Server → Service → Engine → Host → Context → Wrapper

### 2.1 整体结构

Tomcat 的容器是一个**六层树**：

```
StandardServer（服务器：全局配置、生命周期总控）
  │
  └── StandardService（服务：持有 Engine + N 个 Connector + Executor + Mapper）
        │
        ├── Connector[]（第 5 篇：网络层）
        │
        └── StandardEngine（引擎：虚拟主机调度，defaultHost）
              │
              └── StandardHost（虚拟主机：按域名区分应用）
                    │
                    └── StandardContext（Web 应用：一个应用一个 Context）
                          │
                          └── StandardWrapper（Servlet 包装：一个 Servlet 一个 Wrapper）
```

### 2.2 每一层的职责

| 层 | 接口 | 实现类 | 职责 | 1 个对应多少个 |
|---|---|---|---|---|
| Server | `org.apache.catalina.Server` | `StandardServer` | 整个 Tomcat 实例：全局命名资源、服务集合、优雅关闭监听 | 1 : N Service |
| Service | `org.apache.catalina.Service` | `StandardService` | 一个"服务"：一个 Engine + 若干 Connector + 若干 Executor + 一个 Mapper | 1 : 1 Engine，1 : N Connector |
| Engine | `org.apache.catalina.Engine` | `StandardEngine` | 请求处理链的顶层容器：持有 Host 集合与默认 Host、全局 Realm、访问日志；**域名 → Host 的映射由 Mapper 完成**（第 3 篇） | 1 : N Host |
| Host | `org.apache.catalina.Host` | `StandardHost` | 虚拟主机：一个域名下的应用集合；配置 appBase、autoDeploy | 1 : N Context |
| Context | `org.apache.catalina.Context` | `StandardContext` | **Web 应用**：一个应用一个 Context；管理 Servlet/Filter/Listener/Session/类加载器 | 1 : N Wrapper |
| Wrapper | `org.apache.catalina.Wrapper` | `StandardWrapper` | **Servlet 包装**：一个 Servlet 一个 Wrapper；管理 Servlet 实例与生命周期 | 1 : 1 Servlet |

### 2.3 接口体系：契约与实现分离

每层都有**接口 + 标准实现**：

- **接口**（`org.apache.catalina` 根包）：`Server`、`Service`、`Engine`、`Host`、`Context`、`Wrapper`、`Container`——这些是**契约**，定义该层对外暴露的能力
- **实现**（`org.apache.catalina.core`）：`StandardServer`、`StandardService`、`StandardEngine`、`StandardHost`、`StandardContext`、`StandardWrapper`——标准实现

**继承体系**（以 Context 为例）：

```
LifecycleBase（生命周期骨架）
    ↑
LifecycleMBeanBase（+ MBean 注册）
    ↑
ContainerBase（+ 子容器管理、Pipeline、后台线程）  ← 容器树的公共基座
    ↑
StandardContext（+ Web 应用具体能力）
```

这就是第 0 篇对照表中"规范接口 → Tomcat 实现类"在源码侧的完整继承链：
- `StandardContext extends ContainerBase implements Context, NotificationEmitter`（`StandardContext.java:146`）——`Context` 接口本身 `extends Container, Lifecycle`，`ServletContext` 由内部 `ApplicationContext` 提供

> **面试点**：为什么 Server/Service 不继承 `ContainerBase`？——`ContainerBase` 是"容器树节点"的基类，提供 `addChild`/`findChildren` 等子容器管理。**Server 和 Service 虽然在概念上也是树的层级，但它们管理子级的方式不同**：Server 用 `Service[]` 数组管理服务，Service 用 `Connector[]` 数组 + 单个 `Engine` 引用（`setContainer`），都不是 `Container` 子容器模型。所以它们直接继承 `LifecycleMBeanBase`；而 Engine/Host/Context/Wrapper 是真正的容器节点，继承 `ContainerBase`。

---

## 3. 嵌入式入口：Tomcat 类如何一键构建容器树

### 3.1 手工构建 vs 嵌入式构建

- **Standalone 模式**（bin/startup.sh）：`Bootstrap` → `Catalina` 解析 `server.xml`，根据 XML 构建容器树
- **嵌入式模式**：`org.apache.catalina.startup.Tomcat` 类，**纯 Java 编程式构建**，不走 XML

### 3.2 Tomcat 类：懒构建容器树

**源码**：`org/apache/catalina/startup/Tomcat.java`（1267 行）

`Tomcat` 类采用了**懒加载 + 记忆化**构建：容器树不是构造时建好，而是**第一次访问时才创建**，且只创建一次。

**关键字段**（`Tomcat.java:148-152`）：

```java
protected Server server;                       // 记忆化：getServer() 只创建一次
protected int port = 8080;
protected String hostname = "localhost";
protected String basedir;
```

**getServer() —— 树的根**（`Tomcat.java:590-611`）：

```java
public Server getServer() {

    if (server != null) {          // 已创建，直接返回（记忆化）
        return server;
    }

    System.setProperty("catalina.useNaming", "false");   // 嵌入式默认禁用 JNDI

    server = new StandardServer();
    initBaseDir();                                       // 确定 base 目录
    ConfigFileLoader.setSource(new CatalinaBaseConfigurationSource(new File(basedir), null));
    server.setPort(-1);                                  // -1 = 不监听 shutdown 端口

    Service service = new StandardService();
    service.setName("Tomcat");
    server.addService(service);                          // Server 挂上 Service
    return server;
}
```

关键点：
- `catalina.useNaming=false`：**嵌入式默认禁用 JNDI**（Standalone 默认启用）
- `server.setPort(-1)`：**禁用 shutdown 端口**（Standalone 默认 8005）。注意 `await()` 中 -1 的语义是"不监听 socket，轮询 `stopAwait` 标志"（-2 才是不等待直接返回），嵌入式通过 `stopAwait()` 或 Spring 生命周期关闭
- 注意：此时 Service 中**还没有 Engine、没有 Connector**——都是懒创建

**getEngine() —— 树的第二层**（`Tomcat.java:572-583`）：

```java
public Engine getEngine() {
    Service service = getServer().findServices()[0];
    if (service.getContainer() != null) {   // 记忆化
        return service.getContainer();
    }
    Engine engine = new StandardEngine();
    engine.setName("Tomcat");
    engine.setDefaultHost(hostname);        // 默认主机名
    engine.setRealm(createDefaultRealm());  // 内存 Realm（嵌入式专用）
    service.setContainer(engine);
    return engine;
}
```

关键点：
- `setDefaultHost(hostname)`：默认虚拟主机名 = `localhost`
- `createDefaultRealm()`：返回 `SimpleRealm`（Tomcat 内部类）——**嵌入式默认用内存用户表**（`addUser`/`addRole` 往里面塞用户），Standalone 默认从 `conf/tomcat-users.xml` 读取

**getHost() —— 树的第三层**（`Tomcat.java:555-565`）：

```java
public Host getHost() {
    Engine engine = getEngine();
    if (engine.findChildren().length > 0) {
        return (Host) engine.findChildren()[0];   // 记忆化
    }
    Host host = new StandardHost();
    host.setName(hostname);
    getEngine().addChild(host);
    return host;
}
```

**getConnector() —— 网络层（第 5 篇详讲）**：

```java
public Connector getConnector() {
    Service service = getService();
    if (service.findConnectors().length > 0) {
        return service.findConnectors()[0];
    }
    Connector connector = new Connector("HTTP/1.1");  // 默认 NIO HTTP/1.1
    connector.setPort(port);
    service.addConnector(connector);
    return connector;
}
```

### 3.3 addContext / addServlet —— 应用与 Servlet 的注册

**addContext**（`Tomcat.java:636-650`）：编程式注册 Web 应用

```java
public Context addContext(Host host, String contextPath, String contextName, String dir) {
    silence(host, contextName);                        // 静默日志（嵌入式减少日志噪音）
    Context ctx = createContext(host, contextPath);    // new StandardContext()
    ctx.setName(contextName);
    ctx.setPath(contextPath);
    ctx.setDocBase(dir);
    ctx.addLifecycleListener(new FixContextListener());  // 关键：修正启动序列
    host.addChild(ctx);                                // 挂到 Host 下
    return ctx;
}
```

**FixContextListener**（`Tomcat.java:1063-1085`）——**嵌入式特有的关键 Listener**：

```java
public static class FixContextListener implements LifecycleListener {
    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        try {
            Context context = (Context) event.getLifecycle();
            if (event.getType().equals(Lifecycle.CONFIGURE_START_EVENT)) {
                context.setConfigured(true);   // ← 关键！跳过 ContextConfig 配置
                // 处理注解（WebAnnotationSet.loadApplicationAnnotations）
                // 设置默认 LoginConfig（"NONE"）+ NonLoginAuthenticator Valve
            }
        } catch (ClassCastException e) {
        }
    }
}
```

> **面试点（嵌入式核心）**：Standalone 模式下 Context 启动时靠 `ContextConfig`（解析 web.xml、扫描注解）来"配置"应用，配置完成才 `setConfigured(true)`。嵌入式**没有 web.xml**，`StandardContext.startInternal()` 会检查 `getConfigured()`，为 false 则启动失败——所以 `FixContextListener` 在 `CONFIGURE_START_EVENT` 时直接把 `configured` 置 true，**告诉容器"配置已完成"**。Spring Boot 也依赖这条链（第 8 篇详讲）。

**addServlet**（`Tomcat.java:324-349`）：

```java
public Wrapper addServlet(String contextPath, String servletName, String servletClass) {   // [324]
    Container ctx = getHost().findChild(contextPath);
    return addServlet((Context) ctx, servletName, servletClass);
}

public static Wrapper addServlet(Context ctx, String servletName, String servletClass) {   // [338]
    Wrapper sw = ctx.createWrapper();        // new StandardWrapper()
    sw.setServletClass(servletClass);
    sw.setName(servletName);
    ctx.addChild(sw);                        // 挂到 Context 下
    return sw;
}
```

### 3.4 嵌入式构建的完整调用链（Spring Boot 视角）

Spring Boot 的 `TomcatServletWebServerFactory.getWebServer()`（第 8 篇详讲）实际执行：

```
new Tomcat()                          // 空壳
tomcat.setBaseDir(baseDir)            // 设置 base 目录
new Connector("org.apache.coyote.http11.Http11NioProtocol")
tomcat.getService().addConnector(connector)   // → getServer()（懒创建 Server+Service）
tomcat.getHost().setAutoDeploy(false)         // → getEngine() → getHost()（懒创建 Engine+Host）
prepareContext(tomcat.getHost(), initializers) // → 创建 TomcatEmbeddedContext 挂到 Host
return new TomcatWebServer(tomcat)            // 包装（第 8 篇详讲）
```

**构建阶段到此为止——所有组件还处于 NEW 状态，没有启动。** 启动发生在 `TomcatWebServer.initialize()` 调 `tomcat.start()` 时（本篇第 6 节）。

---

## 4. Lifecycle 生命周期机制：Tomcat 的灵魂

### 4.1 为什么需要统一的生命周期？

容器树有 6 层，每层都有"创建 → 初始化 → 启动 → 停止 → 销毁"的需求。如果没有统一机制：

- 每个组件自己写 start/stop，风格各异
- 父子组件的启动顺序无法保证
- 无法在状态变化时通知外部（如 MBean、监听器）

Tomcat 用 **`Lifecycle` 接口 + `LifecycleBase` 抽象类**统一了这一切。

### 4.2 Lifecycle 接口与 11 个状态

**源码**：`org/apache/catalina/Lifecycle.java`（288 行）+ `LifecycleState.java`（63 行）

**四个核心方法**（`Lifecycle.java:207-261`）：

| 方法 | 语义 | 触发事件序列 |
|---|---|---|
| `init()` | 初始化：创建资源，不对外服务 | `BEFORE_INIT_EVENT` → `AFTER_INIT_EVENT` |
| `start()` | 启动：开始对外服务 | `BEFORE_START_EVENT` → `START_EVENT` → `AFTER_START_EVENT` |
| `stop()` | 停止：停止对外服务 | `BEFORE_STOP_EVENT` → `STOP_EVENT` → `AFTER_STOP_EVENT` |
| `destroy()` | 销毁：释放资源 | `BEFORE_DESTROY_EVENT` → `AFTER_DESTROY_EVENT` |

**11 个状态**（`LifecycleState.java:23-35`）：

```java
public enum LifecycleState {
    NEW(false, null),
    INITIALIZING(false, Lifecycle.BEFORE_INIT_EVENT),
    INITIALIZED(false, Lifecycle.AFTER_INIT_EVENT),
    STARTING_PREP(false, Lifecycle.BEFORE_START_EVENT),
    STARTING(true, Lifecycle.START_EVENT),        // ← available=true
    STARTED(true, Lifecycle.AFTER_START_EVENT),   // ← available=true
    STOPPING_PREP(true, Lifecycle.BEFORE_STOP_EVENT),
    STOPPING(false, Lifecycle.STOP_EVENT),
    STOPPED(false, Lifecycle.AFTER_STOP_EVENT),
    DESTROYING(false, Lifecycle.BEFORE_DESTROY_EVENT),
    DESTROYED(false, Lifecycle.AFTER_DESTROY_EVENT),
    FAILED(false, null);

    private final boolean available;      // 该状态下组件是否可用
    private final String lifecycleEvent;  // 进入该状态时触发的事件
}
```

**available 标志是关键设计**：只有 `STARTING`/`STARTED`/`STOPPING_PREP` 三个状态 `available=true`——即**组件只有在这三个状态下才允许被使用**。`isAvailable()` 被大量使用在 `addChild`（容器已启动则立即 start 新子容器）、`StandardHostValve`（Context 是否可用）等场景。

### 4.3 状态迁移图（官方源码注释版）

`Lifecycle.java:26-74` 的 javadoc 画了完整的状态机：

```
            start()
  -----------------------------
  |                           |
  | init()                    |
 NEW -»-- INITIALIZING        |
 | |           |              |     ------------------«-----------------------
 | |           |auto          |     |                                        |
 | |          \|/    start() \|/   \|/     auto          auto         stop() |
 | |      INITIALIZED --»-- STARTING_PREP --»- STARTING --»- STARTED --»---  |
 | |         |                                                            |  |
 | |destroy()|                                                            |  |
 | --»-----«--    ------------------------«--------------------------------  ^
 |     |          |                                                          |
 |     |         \|/          auto                 auto              start() |
 |     |     STOPPING_PREP ----»---- STOPPING ------»----- STOPPED -----»-----
 |    \|/                               ^                     |  ^
 |     |               stop()           |                     |  |
 |     |       --------------------------                     |  |
 |     |       |                                              |  |
 |     |       |    destroy()                       destroy() |  |
 |     |    FAILED ----»------ DESTROYING ---«-----------------  |
 |     |                        ^     |                          |
 |     |     destroy()          |     |auto                      |
 |     --------»-----------------    \|/                         |
 |                                 DESTROYED                     |
 |                                                               |
 |                            stop()                             |
 ----»-----------------------------»------------------------------

任何状态都可以迁移到 FAILED。
```

**关键迁移规则**：

1. **`start()` 在 NEW 状态自动补 `init()`**——`LifecycleBase.start()` 检测到 NEW 会先调 `init()`
2. **`start()` 幂等**：在 `STARTING_PREP`/`STARTING`/`STARTED` 时重复调用无效果
3. **`stop()` 幂等**：在 `STOPPING_PREP`/`STOPPING`/`STOPPED` 时重复调用无效果
4. **NEW 直接 `stop()` → STOPPED**：组件还没启动就被停（如启动失败时清理子组件）
5. **FAILED → stop() 清理**：启动失败后调用 stop 完成清理
6. **任何非法迁移抛 `LifecycleException`**

### 4.4 LifecycleBase：模板方法模式的教科书

**源码**：`org/apache/catalina/util/LifecycleBase.java`（413 行）

**核心设计：骨架方法 final，可变部分抽象**：

```java
public abstract class LifecycleBase implements Lifecycle {

    // ── 骨架方法：final，实现通用逻辑（状态检查、状态切换、事件触发）──

    public final synchronized void init() throws LifecycleException {
        if (!state.equals(LifecycleState.NEW)) {
            invalidTransition(BEFORE_INIT_EVENT);   // 只能从 NEW 进入
        }
        try {
            setStateInternal(LifecycleState.INITIALIZING, null, false);  // 状态→INITIALIZING
            initInternal();                                            // ← 调用子类实现
            setStateInternal(LifecycleState.INITIALIZED, null, false);  // 状态→INITIALIZED
        } catch (Throwable t) {
            handleSubClassException(t, "lifecycleBase.initFail", toString());
        }
    }

    public final synchronized void start() throws LifecycleException {
        // 幂等检查
        if (STARTING_PREP/STARTING/STARTED) return;
        // 状态补链
        if (state.equals(NEW)) init();            // NEW → 自动 init
        else if (state.equals(FAILED)) stop();    // FAILED → 先清理
        else if (!INITIALIZED && !STOPPED) invalidTransition(BEFORE_START_EVENT);  // 非法状态

        try {
            setStateInternal(STARTING_PREP, null, false);
            startInternal();                      // ← 调用子类实现
            // 子类必须在 startInternal() 内 setState(STARTING)
            if (state.equals(FAILED)) stop();     // 受控失败
            else if (!state.equals(STARTING)) invalidTransition(AFTER_START_EVENT);
            else setStateInternal(STARTED, null, false);
        } catch (Throwable t) {
            handleSubClassException(t, "lifecycleBase.startFail", toString());
        }
    }

    // ── 抽象方法：子类实现具体逻辑 ──

    protected abstract void initInternal() throws LifecycleException;
    protected abstract void startInternal() throws LifecycleException;
    protected abstract void stopInternal() throws LifecycleException;
    protected abstract void destroyInternal() throws LifecycleException;
}
```

**start() 的完整流程**（`LifecycleBase.java:138-181`）：

```
start()
  ├─ 幂等检查：STARTING_PREP/STARTING/STARTED → return
  ├─ 状态补链：NEW → init()；FAILED → stop()
  ├─ 非法状态检查
  ├─ setStateInternal(STARTING_PREP)   → 触发 BEFORE_START_EVENT
  ├─ startInternal()                   → 子类逻辑（此时一般 setState(STARTING)）
  │     └─ setState(STARTING)          → 触发 START_EVENT（此时组件 available=true）
  ├─ setStateInternal(STARTED)         → 触发 AFTER_START_EVENT
  └─ 异常 → handleSubClassException()  → setState(FAILED)
```

**setStateInternal 的状态校验**（`LifecycleBase.java:355-391`）：

```java
private synchronized void setStateInternal(LifecycleState state, Object data, boolean check) {
    if (check) {
        // 仅允许这些受控迁移：
        //   任意状态 → FAILED
        //   STARTING_PREP → STARTING（startInternal 内）
        //   STOPPING_PREP → STOPPING（stopInternal 内）
        //   FAILED → STOPPING（stop 清理）
        if (!(state == LifecycleState.FAILED ||
              (this.state == STARTING_PREP && state == STARTING) ||
              (this.state == STOPPING_PREP && state == STOPPING) ||
              (this.state == FAILED && state == STOPPING))) {
            invalidTransition(state.name());   // 抛 LifecycleException
        }
    }
    this.state = state;
    String lifecycleEvent = state.getLifecycleEvent();
    if (lifecycleEvent != null) {
        fireLifecycleEvent(lifecycleEvent, data);   // ← 触发事件
    }
}
```

**注意细节**：`setState(...)`（protected，带 check=true）是**子类调用的入口**，它内部委托给 `setStateInternal(state, null, true)` 做状态校验；而骨架方法（init/start 等）内部直接调 `setStateInternal(..., false)` 跳过二次校验——因为骨架方法自己已经完成了状态检查（如 `if (!state.equals(NEW))`），内部过渡（INITIALIZING→INITIALIZED 等）不需要再校验。子类 `startInternal()` 里调 `setState(STARTING)` 必须走校验，保证只有合法迁移。

### 4.5 事件机制：LifecycleListener

**源码**：`LifecycleEvent.java` + `LifecycleListener.java`

```java
public interface LifecycleListener {
    void lifecycleEvent(LifecycleEvent event);
}
```

- 每个组件持有 `CopyOnWriteArrayList<LifecycleListener>`（`LifecycleBase.java:46`）
- 当前状态：`private volatile LifecycleState state = LifecycleState.NEW`（`LifecycleBase.java:52`）
- `fireLifecycleEvent(type, data)` 遍历通知所有监听器
- 事件类型常量：`BEFORE_INIT_EVENT`/`AFTER_INIT_EVENT`/`BEFORE_START_EVENT`/`START_EVENT`/`AFTER_START_EVENT`/`BEFORE_STOP_EVENT`/`STOP_EVENT`/`AFTER_STOP_EVENT`/`BEFORE_DESTROY_EVENT`/`AFTER_DESTROY_EVENT`/`PERIODIC_EVENT`/`CONFIGURE_START_EVENT`/`CONFIGURE_STOP_EVENT`（`Lifecycle.java:90-166`）

**本篇已见到的监听器应用**：
- `FixContextListener`：`CONFIGURE_START_EVENT` 时置 `configured=true`
- `MapperListener`：监听容器事件（`ADD_CHILD_EVENT`）动态注册路由（第 3 篇详讲）
- `AprLifecycleListener`/`OpenSSLLifecycleListener`：APR/OpenSSL 初始化

> **面试点**：Tomcat 的监听器与 Servlet 的 Listener 有什么关系？——**无关**。`LifecycleListener` 是 Tomcat 内部的生命周期事件监听（组件级别）；`ServletContextListener` 等是 Servlet 规范的 Web 应用事件监听（应用级别）。前者在 `LifecycleBase.fireLifecycleEvent` 触发，后者在 `StandardContext.listenerStart()` 触发（第 0 篇对照表已区分）。

---

## 5. ContainerBase：容器树的基座

**源码**：`org/apache/catalina/core/ContainerBase.java`（1225 行）

`ContainerBase` 是 **Engine/Host/Context/Wrapper 四层共同的基类**，提供：

1. 子容器管理（children Map）
2. Pipeline 持有（第 2 篇详讲）
3. **启停子容器**（startStopExecutor：默认内联串行，可配并行）
4. 后台处理线程（backgroundProcessor）
5. 父子关系（parent/child）

### 5.1 子容器管理：children 映射

```java
protected final HashMap<String,Container> children = new HashMap<>();
private final ReadWriteLock childrenLock = new ReentrantReadWriteLock();
```

**addChild 流程**（`ContainerBase.java:566-604`）：

```java
public void addChild(Container child) {
    // 安全模式下用特权包装
    addChildInternal(child);
}

private void addChildInternal(Container child) {
    childrenLock.writeLock().lock();
    try {
        if (children.get(child.getName()) != null) {
            throw new IllegalArgumentException("名称重复");   // 同层名字唯一
        }
        child.setParent(this);      // ← 关键：设置父引用（双向关联）
        children.put(child.getName(), child);
    } finally {
        childrenLock.writeLock().unlock();
    }

    fireContainerEvent(ADD_CHILD_EVENT, child);   // ← 通知 MapperListener 等

    // 如果本容器已启动，立即启动新子容器
    // 注意：不能在锁内做（启动很慢）
    if ((getState().isAvailable() || STARTING_PREP) && startChildren) {
        child.start();
    }
}
```

**关键设计**：
- `setParent(this)`：**双向关联**——子容器记住父，父容器记住子
- `fireContainerEvent(ADD_CHILD_EVENT)`：**容器事件**（区别于 Lifecycle 事件）——`MapperListener` 正是监听这个事件来动态注册路由（第 3 篇）
- 已启动的容器 addChild 会**立即启动**新子容器（`Tomcat.addContext` 在 `tomcat.start()` 之前调用，所以不会触发——容器还没启动）

### 5.2 子容器启动：看似并行，默认串行

**启动子容器**（`ContainerBase.java:728-783`）：

```java
protected void startInternal() throws LifecycleException {

    reconfigureStartStopExecutor(getStartStopThreads());   // 按配置重建启停执行器

    // 启动从属组件（Cluster、Realm）
    Cluster cluster = getClusterInternal();
    if (cluster instanceof Lifecycle) ((Lifecycle) cluster).start();
    Realm realm = getRealmInternal();
    if (realm instanceof Lifecycle) ((Lifecycle) realm).start();

    // ── 启动所有子容器 ──
    Container[] children = findChildren();
    List<Future<Void>> results = new ArrayList<>(children.length);
    for (Container child : children) {
        results.add(startStopExecutor.submit(new StartChild(child)));  // 提交启停任务
    }
    // 等待全部完成，汇总异常
    MultiThrowable multiThrowable = null;
    for (Future<Void> result : results) {
        try {
            result.get();
        } catch (Throwable e) {
            if (multiThrowable == null) multiThrowable = new MultiThrowable();
            multiThrowable.add(e);
        }
    }
    if (multiThrowable != null) {
        throw new LifecycleException("threadedStartFailed", multiThrowable.getThrowable());
    }

    // 启动 Pipeline（Valve 链，第 2 篇详讲）
    if (pipeline instanceof Lifecycle) ((Lifecycle) pipeline).start();

    setState(LifecycleState.STARTING);   // 触发 START_EVENT

    // 启动后台处理线程（Session 过期扫描等）
    if (backgroundProcessorDelay > 0) {
        monitorFuture = ...scheduleWithFixedDelay(new ContainerBackgroundProcessorMonitor(), 0, 60, SECONDS);
    }
}
```

**StartChild**（`ContainerBase.java:1193-1206`）：

```java
private static class StartChild implements Callable<Void> {
    private final Container child;
    @Override
    public Void call() throws LifecycleException {
        child.start();    // 每个子容器在自己的任务中 start
        return null;
    }
}
```

**关键真相：默认是串行的**（`ContainerBase.java:706-718`）：

```java
private void reconfigureStartStopExecutor(int threads) {
    if (threads == 1) {
        // 1 个线程 → 用"内联执行器"：提交的任务直接在当前线程跑（就是串行）
        if (!(startStopExecutor instanceof InlineExecutorService)) {
            startStopExecutor = new InlineExecutorService();
        }
    } else {
        // > 1 个线程 → 委托给 Server 的 utilityExecutor（真正的并行）
        Server server = Container.getService(this).getServer();
        server.setUtilityThreads(threads);
        startStopExecutor = server.getUtilityExecutor();
    }
}
```

- `startStopThreads` 字段默认 **1**（`ContainerBase.java:260`）→ `InlineExecutorService` → **实际串行**启动子容器
- 只有配置 `setStartStopThreads(n)`（n>1）才切换到 Server 的 `utilityExecutor` 真正并行

> **面试点**：Tomcat 默认串行启动子容器，为什么代码要写成"提交 Future + 等待"的形式？——**为了支持可配置并行**。把启动逻辑统一为"提交任务"模型，默认单线程内联执行（串行），需要时配置 `startStopThreads` 即可无侵入升级为并行。这与请求处理线程池（第 6 篇）是两回事。

### 5.3 后台处理线程：backgroundProcessor

**作用**：周期性执行"后台任务"——最重要的就是 **Session 过期扫描**（`StandardContext.backgroundProcess()` → `Manager.backgroundProcess()` → `processExpires()`）。`StandardContext.backgroundProcess()`（`StandardContext.java:4782`）依次调用 Loader/Manager/WebResourceRoot 的 backgroundProcess。

**机制**（`ContainerBase.java:1090-1106` + 1150-1188）：

- `threadStart()`：用 `ContainerBackgroundProcessor`（Runnable）每 `backgroundProcessorDelay` 秒执行一次
- 递归逻辑（`processChildren`）：**父容器若 `backgroundProcessorDelay <= 0` 则不启动自己的线程，由父级递归调用**：

  ```java
  protected void processChildren(Container container) {
      // Context 先绑定 Webapp 类加载器再处理
      container.backgroundProcess();            // 自己处理一遍
      for (Container child : children) {
          if (child.getBackgroundProcessorDelay() <= 0) {
              processChildren(child);           // 子级未配置 → 递归处理
          }
      }
  }
  ```

- 默认 `backgroundProcessorDelay = -1`（不启动自己的线程）；**`StandardEngine` 构造时设为 10**（`StandardEngine.java:64`）——**只有 Engine 持有周期线程，向下递归处理 Host/Context/Wrapper**

### 5.4 停止与销毁

**stopInternal**（`ContainerBase.java:794-845`）：
1. 取消后台线程
2. 停 Pipeline（Valve）
3. **停止子容器**（`StopChild` 同样经 startStopExecutor 提交，默认内联串行；仅在 `isAvailable()` 时停）
4. 停 Realm/Cluster
5. 关闭 startStopExecutor

**destroyInternal**（`ContainerBase.java:848-875`）：
1. 销毁 Realm/Cluster/Pipeline
2. `removeChild` 所有子容器（递归销毁）
3. 从父容器移除自己

---

## 6. 一次 start() 的完整级联：从 Tomcat.start() 到 Servlet

这是本篇**最重要的实战走读**。嵌入式启动时（Spring Boot `TomcatWebServer.initialize()` → `tomcat.start()`）：

```java
// Tomcat.java:435-438
public void start() throws LifecycleException {
    getServer();      // 确保树已构建
    server.start();   // ← 级联起点
}
```

### 6.1 级联时序总图

```
Tomcat.start()
  └─ StandardServer.start()          [LifecycleBase.start]
      ├─ NEW → 自动 init()
      │    └─ StandardServer.initInternal()
      │         ├─ super.initInternal() → LifecycleMBeanBase → register MBean（嵌入式中被禁用）
      │         ├─ 注册 StringCache / MBeanFactory（MBean，嵌入式中被禁用）
      │         └─ service.init()  ← 每个 Service 初始化
      │              └─ StandardService.initInternal()
      │                   ├─ engine.init()           ← Engine 初始化
      │                   ├─ executor.init()         ← 线程池初始化
      │                   ├─ mapperListener.init()   ← Mapper 监听器初始化
      │                   └─ connector.init()        ← Connector 初始化（第 5 篇）
      │
      └─ StandardServer.startInternal()   [StandardServer.java:855-877]
           ├─ fireLifecycleEvent(CONFIGURE_START_EVENT)
           ├─ setState(STARTING)
           ├─ 初始化 utilityExecutor（调度线程池）
           ├─ globalNamingResources.start()
           └─ service.start()  ← 每个 Service 启动
                └─ StandardService.startInternal()   [StandardService.java:405-431]
                     ├─ setState(STARTING)
                     ├─ engine.start()        ← ★ 容器先启动
                     │    └─ StandardEngine（startInternal → super = ContainerBase 逻辑）
                     │         ├─ 启动子容器（Host，默认串行，可配并行）
                     │         │    └─ StandardHost（startInternal → super = ContainerBase 逻辑）
                     │         │         └─ 启动子容器（Context，默认串行，可配并行）
                     │         │              └─ StandardContext.startInternal（27 步，见 6.2）
                     │         ├─ pipeline.start()
                     │         └─ setState(STARTING)
                     ├─ executor.start()     ← 自定义 Executor 启动
                     ├─ mapperListener.start()  ← ★ 注册路由（第 3 篇）
                     └─ connector.start()    ← ★ 连接器最后启动（第 5 篇）
```

**顺序的关键**：**Engine 先于 Connector**。所以 Spring Boot 的 `removeServiceConnectors()` 技巧（第 8 篇）才能在 Connector 启动前把它从 Service 摘走。

**补充说明**：`StandardServer.initInternal()`（`StandardServer.java:937-959`）的顺序是 `super.initInternal()`（注册自身 MBean）→ 注册 `StringCache`/`MBeanFactory`（MBean，嵌入式中被禁用）→ `globalNamingResources.init()` → 各 `service.init()`。时序图中 `service.init()` 之前的两步 MBean 注册在嵌入式下均被 `Registry.disableRegistry()` 跳过。

### 6.2 StandardContext.startInternal：启动序列

**源码**：`StandardContext.java:4203-4519`（317 行，Tomcat 中最复杂的启动方法）

```
①  setConfigured(false)                        [4217]  ← 先重置配置标志
②  namingResources.start()                     [4223]  ← JNDI 资源（嵌入式中默认关闭）
③  postWorkDirectory()                         [4227]  ← 创建工作目录
④  getResources()==null → StandardRoot         [4230]  ← 资源根（WebResourceRoot）
⑤  resourcesStart()                            [4243]  ← 静态资源启动
⑥  getLoader()==null → WebappLoader            [4246]  ← 类加载器装配（第 7 篇）
⑦  cookieProcessor==null → Rfc6265             [4254]  ← 默认 Cookie 处理器
⑧  getCharsetMapper()                          [4258]  ← 字符集映射
⑨  bindThread()                                [4283]  ← 绑定线程上下文类加载器
⑩  Loader.start()                              [4289]  ← WebappClassLoader 创建（第 7 篇）
⑪  unbindThread→bindThread                     [4309]  ← 切换为 Webapp 类加载器
⑫  Realm 启动                                  [4319]
⑬  fireLifecycleEvent(CONFIGURE_START_EVENT)   [4342]  ← ★ FixContextListener → configured=true
⑭  启动子容器（Wrapper）                        [4345]  ← 串行 child.start()
⑮  pipeline.start()                            [4353]  ← Valve 链启动
⑯  Manager 创建（StandardManager）             [4373]  ← Session 管理器
⑰  if (!getConfigured()) → ok=false            [4392]  ← 配置检查（详见下）
⑱  ServletContext 属性（Resources/InstanceManager/JarScanner） [4399]
⑲  mergeParameters()                           [4421]  ← 合并 context-param
⑳  ★ ServletContainerInitializer.onStartup()   [4424]  ← 触发 TomcatStarter（第 8 篇）
㉑  listenerStart()                             [4436]  ← 触发 ServletContextListener 等
㉒  checkConstraintsForUncoveredMethods()       [4446]
㉓  Manager.start()                             [4452]  ← Session 管理器启动
㉔  filterStart()                               [4462]  ← Filter 实例化+init
㉕  loadOnStartup(findChildren())               [4470]  ← load-on-startup Servlet 加载
㉖  threadStart()                               [4477]  ← 后台线程（Session 过期扫描）
㉗  setState(STARTING)                          [4517]  ← 触发 START_EVENT
```

**与第 0 篇对照表的对应**：
- 第 ⑬ 步 → `LifecycleEvent` 机制（FixContextListener 响应 `CONFIGURE_START_EVENT`）
- 第 ⑳ 步 → `ServletContainerInitializer` 契约实现（触发 TomcatStarter）
- 第 ㉑ 步 → `ServletContextListener` 契约实现
- 第 ㉔ 步 → `Filter` 契约实现
- 第 ㉕ 步 → `Servlet` 生命周期（第 4 篇详讲）

> **面试点**：`setConfigured(false)` 与 FixContextListener 的配合——Standalone 由 `ContextConfig` 在 `CONFIGURE_START_EVENT` 时解析 web.xml 并 setConfigured(true)；嵌入式由 `FixContextListener` 直接置 true。若两者都没有设置（编程式 addContext 但没加 FixContextListener），第 ⑰ 步 `if (!getConfigured()) { ok = false; }` 直接判启动失败。

### 6.3 Wrapper 启动与 Servlet 的加载时机

`StandardWrapper.startInternal()`（`StandardWrapper.java:1172-1191`）**本身不加载 Servlet**，只做：

```java
protected void startInternal() throws LifecycleException {
    // 发送 j2ee.state.starting 通知（MBean 通知，嵌入式中被禁用）
    super.startInternal();     // ContainerBase：启动 Pipeline 等
    setAvailable(0L);          // available 是"可用时间戳"：0 = 立即可用（非 0 表示不可用到该时刻）
}
```

**Servlet 的加载统一由 `StandardContext.loadOnStartup()` 触发**（`StandardContext.java:4165-4199`）：

```java
public boolean loadOnStartup(Container children[]) {
    // 1. 收集 loadOnStartup >= 0 的 Wrapper，按优先级排序（TreeMap）
    TreeMap<Integer,ArrayList<Wrapper>> map = new TreeMap<>();
    for (Container child : children) {
        Wrapper wrapper = (Wrapper) child;
        int loadOnStartup = wrapper.getLoadOnStartup();
        if (loadOnStartup < 0) {
            continue;                       // < 0 跳过（懒加载）
        }
        map.computeIfAbsent(loadOnStartup, k -> new ArrayList<>()).add(wrapper);
    }
    // 2. 按优先级顺序调用 wrapper.load()（实例化 + init）
    for (ArrayList<Wrapper> list : map.values()) {
        for (Wrapper wrapper : list) {
            wrapper.load();   // StandardWrapper.java:697
        }
    }
    return true;
}
```

**两种初始化时机**：
1. **启动时**：`load-on-startup >= 0`（如 `DefaultServlet` 为 1，JSP Servlet 为 3——但嵌入式 Spring Boot 默认不注册它们）
2. **首次请求**：`load-on-startup = -1`（默认值，`StandardWrapper.java:135`）——**Spring Boot 的 DispatcherServlet 正是这个默认值**（`ServletRegistrationBean.java:62` 默认 -1），首次请求时由 `StandardWrapperValve.invoke()` → `allocate()` 懒加载（第 2、4 篇详讲）

> Spring Boot 的 `TomcatEmbeddedContext` 覆写了 `loadOnStartup()` 为 no-op（延迟到 `TomcatWebServer.start()` 才执行 `deferredLoadOnStartup()`），这是 Spring Boot 启动时序控制的核心（第 8 篇详讲）。

---

## 7. LifecycleMBeanBase：MBean 注册（嵌入式中被禁用）

**源码**：`org/apache/catalina/util/LifecycleMBeanBase.java`（197 行）

### 7.1 结构

```java
public abstract class LifecycleMBeanBase extends LifecycleBase implements JmxEnabled {

    @Override
    protected void initInternal() throws LifecycleException {
        // 如果 oname 为 null，注册 MBean
        if (oname == null) {
            oname = register(this, getObjectNameKeyProperties());  // 注册到 MBeanServer
        }
    }

    @Override
    protected void destroyInternal() throws LifecycleException {
        unregister(oname);   // 销毁时反注册
    }
}
```

- **所有 Standard 组件**（Server/Service/Engine/Host/Context/Wrapper/Connector）都继承它
- `init()` 时自动注册 MBean（`Catalina:type=Server` 等），`destroy()` 时反注册
- **ObjectName 的构建**：`getDomain()`（默认 "Catalina"）+ `getObjectNameKeyProperties()`（如 `type=Server`、`type=Service,name=Tomcat`）

### 7.2 嵌入式为什么被禁用？

**Spring Boot 的 `TomcatServletWebServerFactory.getWebServer()` 第一行**（第 8 篇详讲）：

```java
if (this.disableMBeanRegistry) {
    Registry.disableRegistry();   // ← 关闭整个 MBean Registry
}
```

**`Registry.disableRegistry()`**（`org.apache.tomcat.util.modeler.Registry:135-141`）：

```java
public static synchronized void disableRegistry() {
    if (registry == null) {
        // 替换为 NoDescriptorRegistry：registerComponent 全部 NO-OP
        registry = new NoDescriptorRegistry();
    } else if (!(registry instanceof NoDescriptorRegistry)) {
        log.warn(sm.getString("registry.noDisable"));   // 已初始化则无法禁用
    }
}
```

- 实现方式：不是"设标志"，而是**把单例 Registry 替换成 `NoDescriptorRegistry`**（`registerComponent`/`unregisterComponent` 均空实现）
- 注意时序：Spring Boot 在 `getWebServer()` 开头调用，**早于**任何组件 `init()`，所以能生效；如果 Registry 已被初始化（如先手动注册过 MBean），disableRegistry 只会告警
- 所有组件的 `initInternal()` 仍会执行，但 MBean 注册被跳过

**为什么 Spring Boot 要禁用它**：
1. 嵌入式单实例运行，不需要 JMX 远程管理（独立 Tomcat 用 JMX 管理多个应用）
2. 省去 MBean 注册开销
3. 避免多个嵌入式 Tomcat 实例的 MBean 名称冲突
4. 但注意：**`NoDescriptorRegistry` 只禁用 Tomcat 内部组件自动注册**，如果应用自己创建 `MBeanServer` 并注册，不受影响

> **面试点**：嵌入式的 JMX 是否完全不可用？——不是。`Registry.disableRegistry()` 禁的是 Tomcat 内部组件自动注册。Spring Boot Actuator 的 JMX endpoint（`management.endpoints.jmx.exposure.include=*`）走的是 **Spring 自己的 `MBeanExporter`**，与 Tomcat 的 Registry 无关，仍然可用。

---

## 8. 本篇小结与面试要点

### 8.1 本篇文章地图

```
第 0 篇（契约）          Servlet 规范接口
第 1 篇（本篇）          容器树 + Lifecycle 骨架    ← 全系列的地基
第 2 篇                  Pipeline-Valve 请求处理链
第 3 篇                  Mapper 路由机制
第 4 篇                  Servlet 生命周期管理
第 5 篇                  Connector 与 CoyoteAdapter
第 6 篇                  NIO 网络层与线程模型
第 7 篇                  类加载机制
第 8 篇                  Spring Boot 集成 + 生产定制
```

### 8.2 面试要点速查

1. **Tomcat 两层架构**：Catalina（容器层，实现 Servlet 规范）vs Coyote（连接器层，处理网络协议），`CoyoteAdapter` 是桥梁
2. **六层容器树**：Server → Service → Engine → Host → Context → Wrapper，每层接口 + Standard 实现
3. **接口体系**：`LifecycleBase` → `LifecycleMBeanBase` → `ContainerBase` → 各 Standard 组件；Server/Service 不继承 ContainerBase（不是树节点）
4. **Lifecycle 状态机**：11 状态、4 方法、模板方法模式（final 骨架 + 抽象内部方法）、幂等 start/stop、NEW 自动补 init、FAILED 统一清理
5. **available 标志**：仅 STARTING/STARTED/STOPPING_PREP 三个状态可用
6. **事件机制**：`setStateInternal` 进入状态时自动触发对应 `LifecycleEvent`，监听器用 `CopyOnWriteArrayList` 存储
7. **ContainerBase**：children HashMap（读写锁）、`setParent` 双向关联、`ADD_CHILD_EVENT` 容器事件、startStopExecutor 启停子容器（**默认内联串行**，配置 >1 才真正并行）
8. **后台线程**：`backgroundProcessor` 周期扫描（Session 过期），**只有 Engine 持有线程**（默认 10 秒），Host/Context/Wrapper 未配置（默认 -1）时由 Engine 递归调用其 `backgroundProcess()`
9. **StandardContext.startInternal 27 步**：`CONFIGURE_START_EVENT`（第 ⑬ 步）→ `FixContextListener` 置 configured=true → SCI → Listener → Filter → loadOnStartup
10. **MBean**：`LifecycleMBeanBase` 在 init/destroy 时注册/反注册，但 Spring Boot `Registry.disableRegistry()` 禁用了它；Actuator JMX 不受影响
