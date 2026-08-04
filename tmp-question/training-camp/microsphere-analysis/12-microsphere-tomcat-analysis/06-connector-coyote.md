# 第 5 篇：Connector 与 CoyoteAdapter 桥接

> 前几篇讲完了容器层（Catalina 世界）。本篇进入**连接器层（Coyote 世界）**。
> 回答核心问题：**Tomcat 如何把"端口监听"变成"Servlet 调用"？Connector 的两阶段启动是怎么设计的？**
>
> 这是全系列的分水岭：第 1~4 篇是容器层（请求进来之后），第 5~6 篇是网络层（请求怎么进来）。
>
> **源码范围**：
> - `org.apache.catalina.connector.Connector`（1154 行，连接器本体）
> - `org.apache.coyote.ProtocolHandler` / `AbstractProtocol`（协议处理器）
> - `org.apache.coyote.http11.Http11NioProtocol`（HTTP/1.1 NIO 实现）
> - `org.apache.tomcat.util.net.AbstractEndpoint` / `NioEndpoint`（网络端点）
> - `org.apache.catalina.connector.CoyoteAdapter`（两层桥接）
> - Spring Boot 侧：`TomcatWebServer`（两阶段启动控制，预告）
>
> **本篇定位**：Tomcat 核心层为主，末尾结合 Spring Boot 的两阶段启动技巧（第 8 篇详讲）。

---

## 目录

1. [Connector 的职责：连接器层的"总装车间"](#1-connector-的职责连接器层的总装车间)
2. [ProtocolHandler 工厂：从字符串到实例](#2-protocolhandler-工厂从字符串到实例)
3. [四层继承体系：Http11NioProtocol 的身世](#3-四层继承体系http11nioprotocol-的身世)
4. [两阶段启动：init() 不绑端口，start() 才绑端口](#4-两阶段启动init-不绑端口start-才绑端口)
5. [bind 的真相：bindOnInit 开关](#5-bind-的真相bindoninit-开关)
6. [CoyoteAdapter：两层世界的粘合剂](#6-coyoteadapter两层世界的粘合剂)
7. [Spring Boot 的两阶段启动技巧（预告）](#7-spring-boot-的两阶段启动技巧预告)
8. [本篇小结与面试要点](#8-本篇小结与面试要点)

---

## 1. Connector 的职责：连接器层的"总装车间"

### 1.1 定位

**Connector**（`org.apache.catalina.connector.Connector`，1154 行）是**连接器层的总装车间**：

- 它**不是**网络组件本身，而是"组织者"——持有 `ProtocolHandler`（协议处理）和 `CoyoteAdapter`（容器桥接）
- 它在 `catalina.connector` 包（Catalina 侧），但管理的是 `coyote` 包的对象——**跨两层**
- 一个 Service 可以有多个 Connector（如 HTTP 8080 + HTTPS 8443），第 1 篇已见

```java
public class Connector extends LifecycleMBeanBase {
    // 协议处理器（网络层的总负责人）
    protected ProtocolHandler protocolHandler = null;
    // 适配器（连接器层 → 容器层的桥梁）
    protected Adapter adapter = null;
    // 所属 Service
    protected Service service = null;
}
```

### 1.2 构造：Connector("HTTP/1.1") 发生了什么？

**源码**：`Connector.java:78-95`

```java
public Connector(String protocol) {
    configuredProtocol = protocol;
    ProtocolHandler p = null;
    try {
        p = ProtocolHandler.create(protocol);   // ★ 工厂方法创建协议处理器
    } catch (Exception e) {
        log.error(sm.getString("coyoteConnector.protocolHandlerInstantiationFailed"), e);
    }
    if (p != null) {
        protocolHandler = p;
        protocolHandlerClassName = protocolHandler.getClass().getName();
    } else {
        protocolHandler = null;
        protocolHandlerClassName = protocol;
    }
    setThrowOnFailure(Boolean.getBoolean("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE"));
}
```

**关键**：构造时只创建 `ProtocolHandler`，**网络资源（端口）此时还没碰**——这就是两阶段启动的基础。

---

## 2. ProtocolHandler 工厂：从字符串到实例

**源码**：`org/apache/coyote/ProtocolHandler.java:240-254`

```java
static ProtocolHandler create(String protocol) throws ... {
    if (protocol == null || "HTTP/1.1".equals(protocol) ||
            org.apache.coyote.http11.Http11NioProtocol.class.getName().equals(protocol)) {
        return new org.apache.coyote.http11.Http11NioProtocol();   // ★ 默认 NIO HTTP/1.1
    } else if ("AJP/1.3".equals(protocol) ||
            org.apache.coyote.ajp.AjpNioProtocol.class.getName().equals(protocol)) {
        return new org.apache.coyote.ajp.AjpNioProtocol();          // AJP 协议
    } else {
        // 其他：反射实例化（如 HTTP/2、自定义协议）
        Class<?> clazz = Class.forName(protocol);
        return (ProtocolHandler) clazz.getConstructor().newInstance();
    }
}
```

**协议名 → 实现类**：

| 配置值 | 实现类 | 用途 |
|---|---|---|
| `"HTTP/1.1"`（默认） | `Http11NioProtocol` | 标准 HTTP/1.1（NIO） |
| `"AJP/1.3"` | `AjpNioProtocol` | Apache 前端集成协议 |
| 完整类名 | 反射创建 | 自定义/HTTP/2 等 |

> Spring Boot 的 `TomcatServletWebServerFactory` 默认用**完整类名**：
> `new Connector("org.apache.coyote.http11.Http11NioProtocol")`——效果等同 `"HTTP/1.1"`。

---

## 3. 四层继承体系：Http11NioProtocol 的身世

### 3.1 继承链

```
ProtocolHandler（接口：init/start/stop/pause/resume...）
    ↑
AbstractProtocol<S>（抽象基类：持 Endpoint、ConnectionHandler、属性委托）
    ↑
AbstractHttp11Protocol<S>（HTTP/1.1 特性：keep-alive、swallow、maxKeepAliveRequests...）
    ↑
AbstractHttp11JsseProtocol<S>（TLS/SSL 特性：SSLHostConfig、证书管理...）
    ↑
Http11NioProtocol（NIO 实现：new NioEndpoint()）
```

**关键源码行**：

```java
// Http11NioProtocol.java:28
public class Http11NioProtocol extends AbstractHttp11JsseProtocol<NioChannel> {

    public Http11NioProtocol() {
        this(new NioEndpoint());          // ★ 构造时创建 NIO 端点
    }
}

// AbstractProtocol.java:55
public abstract class AbstractProtocol<S> implements ProtocolHandler, MBeanRegistration {

    private final AbstractEndpoint<S,?> endpoint;   // 低层网络端点

    public AbstractProtocol(AbstractEndpoint<S,?> endpoint) {
        this.endpoint = endpoint;
        ConnectionHandler<S> cHandler = new ConnectionHandler<>(this);
        getEndpoint().setHandler(cHandler);   // ★ 端点 ←→ 协议处理器 双向绑定
        setHandler(cHandler);
    }
}
```

### 3.2 每层职责

| 类 | 职责 |
|---|---|
| `ProtocolHandler` | 协议生命周期契约（init/start/stop/pause/resume）+ 工厂方法 |
| `AbstractProtocol` | **持有 Endpoint**、ConnectionHandler 连接管理、属性委托到 Endpoint |
| `AbstractHttp11Protocol` | HTTP/1.1 语义：keep-alive 超时、maxKeepAliveRequests、maxSwallowSize、maxHttpRequestHeaderSize |
| `AbstractHttp11JsseProtocol` | TLS：SSLHostConfig、证书、SNI |
| `Http11NioProtocol` | 指定 NIO 实现（`NioEndpoint`），几乎无代码 |

### 3.3 属性委托链（第 8 篇生产配置的基础）

`AbstractProtocol` 的 `setProperty`/`getProperty` **直接委托给 Endpoint**：

```java
// AbstractProtocol.java:116-129
public boolean setProperty(String name, String value) {
    return endpoint.setProperty(name, value);
}
public String getProperty(String name) {
    return endpoint.getProperty(name);
}
```

`AbstractEndpoint.setProperty` 用**反射**设置（`AbstractEndpoint.java:1005-1012`）：

```java
public boolean setProperty(String name, String value) {
    setAttribute(name, value);          // ← 先放进 attributes Map（getProperty 会读到）
    final String socketName = "socket.";
    try {
        if (name.startsWith(socketName)) {
            return IntrospectionUtils.setProperty(socketProperties, name.substring(socketName.length()), value);
        } else {
            // ★ 第四参 false：找到 setFoo 就调用；找不到则【不】fallback 到 setProperty 方法
            //（防止 Endpoint.setProperty 内递归调用自己）
            return IntrospectionUtils.setProperty(this, name, value, false);
        }
    } catch (Exception x) {
        getLog().error(...);
        return false;
    }
}
```

**注意 `setAttribute(name, value)` 的副作用**：`setProperty` 会把属性**同时**写进 attributes Map
（`AbstractEndpoint.java:969/981`）——所以 get/set 之后 `getProperty` 再查就能读到（第 7 节时序依赖此行为）。

**这就是 Spring Boot 配置能生效的链路**：

```
server.tomcat.max-connections=20000
  → TomcatWebServerFactoryCustomizer.customizeMaxConnections()
  → factory.addConnectorCustomizers(connector -> protocol.setMaxConnections(20000))
  → AbstractProtocol.setMaxConnections() → endpoint.setMaxConnections()（直接调 setter，不走反射）
```

而 `connector.setProperty("bindOnInit", "false")` 走的是**反射委托链**：

```
Connector.setProperty("bindOnInit", "false")        [Connector.java:314]
  → IntrospectionUtils.setProperty(protocolHandler, ...)  反射查找
  → AbstractProtocol 无 setBindOnInit → fallback 到 setProperty(String,String)
  → AbstractProtocol.setProperty → Endpoint.setProperty   [AbstractProtocol.java:116]
  → IntrospectionUtils.setProperty(this, "bindOnInit", ...)  反射调用
  → AbstractEndpoint.setBindOnInit(false)                     [AbstractEndpoint.java:707]
```

**关键不对称（get 与 set 路径不同）**：

Spring Boot 的 `disableBindOnInit()` 先执行 `connector.getProperty("bindOnInit")` 判断是否为 null——
这里 `get` 与 `set` 走的是**不同的路径**：

- `get`：`Connector.getProperty` → IntrospectionUtils 在 **AbstractProtocol 上找 `getBindOnInit()`**——不存在！
  → fallback 到 `protocolHandler.getProperty(String)` → `endpoint.getProperty(String)` → **查 attributes Map**（`AbstractEndpoint.java:995-1001`）
  → `bindOnInit` 是字段而非 attribute → 返回 **null** → Spring Boot 的 `if (bindOnInit == null)` 成立
- `set`：IntrospectionUtils 在 AbstractProtocol 上找不到 `setBindOnInit` → fallback 到
  `protocolHandler.setProperty(String,String)` → **委托到 Endpoint** → 反射调用 `setBindOnInit(false)` → 设置成功

> **面试点**：为什么 `connector.getProperty("bindOnInit")` 返回 null（明明 Endpoint 有 `getBindOnInit()`）？
> ——`getProperty` 的查找目标是 **AbstractProtocol 层**（Connector 直接持有的对象），而 `bindOnInit` 属性
> 在 **AbstractEndpoint 层**。get 路径 fallback 后只查 attributes Map（返回 null），set 路径却会一路
> 委托到 Endpoint 反射设置——**get/set 不对称**。Spring Boot 正是利用这一点：get 到 null 才触发 set。

> **面试点**：`server.tomcat.*` 配置如何到达网络层？——两条路：
> ① 专用 setter（`AbstractProtocol.setMaxConnections` 等）直接设置；
> ② 通用 `setProperty` 反射委托（`bindOnInit` 等）。最终都落在 `AbstractEndpoint` 上（第 8 篇展开）。

---

## 4. 两阶段启动：init() 不绑端口，start() 才绑端口

### 4.1 完整启动链路（Connector 视角）

```
StandardService.initInternal()                    [StandardService.java:504-527]
  └─ connector.init()                             [LifecycleBase.init → initInternal]
       └─ Connector.initInternal()                [Connector.java:989-1032]
            ├─ super.initInternal()               [LifecycleMBeanBase：注册 MBean，嵌入式中被禁用]
            ├─ adapter = new CoyoteAdapter(this)  ← ★ 创建桥接
            ├─ protocolHandler.setAdapter(adapter) ← 协议处理器持有桥接
            ├─ TLS 实现选择（JSSE/OpenSSL）
            └─ protocolHandler.init()             [AbstractProtocol.init]
                 └─ endpoint.init()               [AbstractEndpoint.init]
                      └─ if (bindOnInit) bind()   ← ★ 默认在这里绑端口！

StandardService.startInternal()                  [StandardService.java:405-431]
  └─ connector.start()                            [LifecycleBase.start → startInternal]
       └─ Connector.startInternal()               [Connector.java:1040-1063]
            ├─ setState(STARTING)
            ├─ protocolHandler.setUtilityExecutor(...)
            └─ protocolHandler.start()            [AbstractProtocol.start]
                 └─ endpoint.start()              [AbstractEndpoint.start]
                      ├─ if (bindState == UNBOUND) bind()   ← bindOnInit=false 时在这里绑
                      └─ startInternal()          [NioEndpoint.startInternal]
                           ├─ createExecutor()    ← 线程池
                           ├─ initializeConnectionLatch() ← LimitLatch（第 6 篇）
                           ├─ new Poller() + start ← Poller 线程
                           └─ startAcceptorThread() ← Acceptor 线程
```

### 4.2 init 阶段：资源准备

**Connector.initInternal()**（`Connector.java:989-1032`）：

```java
@Override
protected void initInternal() throws LifecycleException {

    super.initInternal();

    if (protocolHandler == null) {
        throw new LifecycleException(sm.getString("coyoteConnector.protocolHandlerInstantiationFailed"));
    }

    // ★ 创建桥接并绑定：CoyoteAdapter 是两层唯一粘合点
    adapter = new CoyoteAdapter(this);
    protocolHandler.setAdapter(adapter);

    // TLS 实现选择（Java 22+ OpenSSL FFM → APR OpenSSL → 默认 JSSE）
    if (JreCompat.isJre22Available() && OpenSSLStatus.getUseOpenSSL() && ...) {
        jsseProtocolHandler.setSslImplementationName("...panama.OpenSSLImplementation");
    } else if (AprStatus.isAprAvailable() && ...) {
        jsseProtocolHandler.setSslImplementationName(OpenSSLImplementation.class.getName());
    }

    try {
        protocolHandler.init();       // ← 协议处理器初始化（Endpoint.init）
    } catch (Exception e) {
        throw new LifecycleException(...);
    }
}
```

**AbstractProtocol.init()**（`AbstractProtocol.java:609-634`）：

```java
public void init() throws Exception {
    ...
    endpoint.init();    // 委托给 Endpoint
}
```

**AbstractEndpoint.init()**（`AbstractEndpoint.java:1315-1334`）：

```java
public final void init() throws Exception {
    if (bindOnInit) {              // ← ★ 关键开关（默认 true）
        bindWithCleanup();         //   绑端口！
        bindState = BindState.BOUND_ON_INIT;
    }
    // MBean 注册（嵌入式中被禁用）
    ...
}
```

### 4.3 start 阶段：开始服务

**Connector.startInternal()**（`Connector.java:1040-1063`）：

```java
@Override
protected void startInternal() throws LifecycleException {

    // 端口校验
    String id = (protocolHandler != null) ? protocolHandler.getId() : null;
    if (id == null && getPortWithOffset() < 0) {
        throw new LifecycleException(sm.getString("coyoteConnector.invalidPort", ...));
    }

    setState(LifecycleState.STARTING);

    // 配置工具线程池（后台任务用）
    if (protocolHandler != null && service != null) {
        protocolHandler.setUtilityExecutor(service.getServer().getUtilityExecutor());
    }

    try {
        protocolHandler.start();    // ← Endpoint.start
    } catch (Exception e) {
        throw new LifecycleException(...);
    }
}
```

**AbstractEndpoint.start()**（`AbstractEndpoint.java:1388-1394`）：

```java
public final void start() throws Exception {
    if (bindState == BindState.UNBOUND) {   // ← init 时没绑（bindOnInit=false）
        bindWithCleanup();                  //   现在绑！
        bindState = BindState.BOUND_ON_START;
    }
    startInternal();                        // ← 启动线程
}
```

**NioEndpoint.startInternal()**（`NioEndpoint.java:248-286`，第 6 篇详讲）：

```java
@Override
public void startInternal() throws Exception {
    if (!running) {
        running = true;
        paused = false;

        // 对象池：processorCache / eventCache / nioChannels（SynchronizedStack 复用）
        ...

        // ★ 创建请求处理线程池（默认无自定义 Executor 时）
        if (getExecutor() == null) {
            createExecutor();
        }

        // ★ 连接数限制器（LimitLatch，maxConnections）
        initializeConnectionLatch();

        // ★ 启动 Poller 线程（Selector 事件循环）
        poller = new Poller();
        Thread pollerThread = new Thread(poller, getName() + "-Poller");
        pollerThread.setDaemon(true);
        pollerThread.start();

        // ★ 启动 Acceptor 线程（accept 新连接）
        startAcceptorThread();
    }
}
```

---

## 5. bind 的真相：bindOnInit 开关

### 5.1 什么是 bind？

**bind** = 创建 `ServerSocketChannel` 并绑定端口（`NioEndpoint.bind()`，193-242 行）：

```java
@Override
public void bind() throws Exception {
    initServerSocket();          // ← ServerSocketChannel.open() + bind(addr, acceptCount)
    setStopLatch(new CountDownLatch(1));
    initialiseSsl();             // TLS 初始化
}
```

```java
protected void initServerSocket() throws Exception {
    if (getUseInheritedChannel()) {
        // 继承 OS 提供的 channel（容器编排场景）
        serverSock = (ServerSocketChannel) System.inheritedChannel();
    } else if (getUnixDomainSocketPath() != null) {
        // Unix Domain Socket
        ...
    } else {
        // ★ 常规 TCP
        serverSock = ServerSocketChannel.open();
        socketProperties.setProperties(serverSock.socket());
        InetSocketAddress addr = new InetSocketAddress(getAddress(), getPortWithOffset());
        serverSock.bind(addr, getAcceptCount());   // ← backlog = acceptCount！
    }
    serverSock.configureBlocking(true);   // Acceptor 用阻塞 accept
}
```

### 5.2 bindOnInit 默认值

`AbstractEndpoint.java:705`：`private boolean bindOnInit = true;`

**默认行为**：`init()` 阶段就绑端口（Standalone 模式：`connector.init()` 完成时端口已监听）。

**bindState 四态**（`AbstractEndpoint.java:133-142`）：

```java
protected enum BindState {
    UNBOUND(false, false),            // 未绑定
    BOUND_ON_INIT(true, true),        // init 时绑定
    BOUND_ON_START(true, true),       // start 时绑定
    SOCKET_CLOSED_ON_STOP(false, true)  // 停止时优雅关闭了 socket（曾绑定过）
}
```

**状态迁移**：

```
init():  bindOnInit=true  → bind() → BOUND_ON_INIT
start(): bindState==UNBOUND → bind() → BOUND_ON_START
stop():  BOUND_ON_START/SOCKET_CLOSED_ON_STOP → unbind() → UNBOUND
destroy(): BOUND_ON_INIT → unbind() → UNBOUND
closeServerSocketGraceful()（优雅关闭，第 8 篇）:
         BOUND_ON_START → 停 Acceptor → 关 socket → SOCKET_CLOSED_ON_STOP
```

### 5.3 为什么需要 bindOnInit=false？

**场景**：低权限用户启动 Tomcat 需要绑定 80 端口——Unix 系统允许"先以 root 启动绑定端口，
再降权运行"。`bindOnInit=false` + 生命周期钩子实现"先绑后降权"。

**更重要**：**Spring Boot 用它实现两阶段启动**（第 7 节）。

> **面试点**：`acceptCount` 对应什么？
> ——`ServerSocket.bind(addr, getAcceptCount())` 的 **backlog 参数**（等待队列长度）。
> Spring Boot 的 `server.tomcat.accept-count` 最终就是这里（第 8 篇）。

---

## 6. CoyoteAdapter：两层世界的粘合剂

### 6.1 定位回顾

`CoyoteAdapter` 是**连接器层与容器层的唯一桥梁**（第 1 篇 1.1）：

- 持有者：`Connector.initInternal()` 创建并绑定到 `ProtocolHandler`（`Connector.java:999-1000`）
- 调用者：Coyote 层（`Http11Processor`）每解析完一个 HTTP 请求，调 `adapter.service()`
- 实现者：`org.apache.catalina.connector.CoyoteAdapter`（Catalina 侧，1315 行）

### 6.2 双向数据流

```
Coyote 层（网络）                          Catalina 层（容器）
─────────────────                        ──────────────────
Http11Processor                            StandardEngineValve...
    │                                          │
    │  org.apache.coyote.Request               │  org.apache.catalina.connector.Request
    │  org.apache.coyote.Response              │  org.apache.catalina.connector.Response
    │                                          │
    └──→ adapter.service(coyoteReq, coyoteRes) ──→  pipeline.getFirst().invoke(catReq, catRes)
              │                                    │
              │  setCoyoteRequest / note            │  request.getRequest()（门面 RequestFacade）
              │  setCoyoteResponse                  │
              └── 数据转换（coyote ↔ catalina）──────┘
```

### 6.3 与第 2 篇的衔接

第 2 篇的 `CoyoteAdapter.service()` 七步流程（302-426 行）正是这个桥梁的实现：
1. 对象转换（note 槽位复用）
2. X-Powered-By
3. `postParseRequest()`（Mapper 路由，第 3 篇）
4. `pipeline.getFirst().invoke()`（进入容器，第 2 篇）
5. 异步处理
6. 访问日志
7. 对象回收

> **面试点**：为什么叫 "Coyote"？——Tomcat 把网络协议处理层命名为 Coyote（丛林狼），
> 与 Catalina（容器层，源自 Catalina 岛）组成 Tomcat 的两大子系统。历史命名，无特殊含义。

---

## 7. Spring Boot 的两阶段启动技巧（预告）

Spring Boot 用 **bindOnInit=false + removeServiceConnectors** 实现"先启动容器、后绑端口"，
保证 `TomcatWebServer.initialize()` 时容器全部就绪，端口最后才监听。

**源码**：`TomcatWebServer.java:110-150`（`initialize()`）+ 169-187（两个技巧）

```java
private void initialize() throws WebServerException {
    synchronized (this.monitor) {
        try {
            addInstanceIdToEngineName();

            Context context = findContext();
            // ★ 技巧一：监听 Context 的 START_EVENT，把 Connector 从 Service 摘走
            context.addLifecycleListener((event) -> {
                if (context.equals(event.getSource()) && Lifecycle.START_EVENT.equals(event.getType())) {
                    // Remove service connectors so that protocol binding doesn't
                    // happen when the service is started.
                    removeServiceConnectors();
                }
            });

            // ★ 技巧二：禁用 init 时绑端口
            disableBindOnInit();

            // ★ tomcat.start()：容器启动，但 Connector 不在 Service 里 + bindOnInit=false
            this.tomcat.start();

            rethrowDeferredStartupExceptions();
            ...
            startNonDaemonAwaitThread();
        } catch (Exception ex) {
            stopSilently();
            destroySilently();
            throw new WebServerException("Unable to start embedded Tomcat", ex);
        }
    }
}

private void disableBindOnInit() {
    doWithConnectors((service, connectors) -> {
        for (Connector connector : connectors) {
            Object bindOnInit = connector.getProperty("bindOnInit");
            if (bindOnInit == null) {
                connector.setProperty("bindOnInit", "false");   // ← 反射委托链（3.3 节）
            }
        }
    });
}

private void removeServiceConnectors() {
    doWithConnectors((service, connectors) -> {
        this.serviceConnectors.put(service, connectors);   // 记住摘走的
        for (Connector connector : connectors) {
            service.removeConnector(connector);            // 从 Service 摘走
        }
    });
}
```

**时序推演**：

```
TomcatWebServer.initialize()
  1. 注册 Context START_EVENT 监听器
  2. disableBindOnInit()：connector.setProperty("bindOnInit", "false")
  3. tomcat.start()
     → StandardServer.start → StandardService.startInternal()
       → engine.start()（容器先启动！）
       → mapperListener.start()
       → connector.start()  ← 但 Connector 已被摘走？！
```

**等等——Connector 还没被摘走，怎么保证不启动？**

关键在时序：`StandardService.startInternal()` 的启动顺序是 **engine → executor → mapperListener → connectors**
（第 1 篇 6.1）。`tomcat.start()` 中：

```
StandardService.startInternal()
  ├─ engine.start()          ← Context 启动 → 触发 START_EVENT
  │    └─ ... → Context.startInternal() → setState(STARTING) → START_EVENT
  │         └─ 监听器触发 → removeServiceConnectors() ← ★ 此刻才摘走！
  ├─ executor.start()
  ├─ mapperListener.start()
  └─ connector.start()       ← 遍历 findConnectors()——已空！什么都不做
```

**为什么监听 Context 的 START_EVENT 而不是直接摘？**——因为 `tomcat.start()` 之前
`getWebServer()` 阶段 Connector 必须在 Service 里（`registerConnectorExecutor` 等配置依赖它），
而且摘早了 `StandardService.startInternal()` 遍历时 Connector 不在，就不会启动它。
**摘走 = 从 `findConnectors()` 的视野中消失 = 跳过启动**。

**补充细节**：摘走时 Connector 的状态是 **INITIALIZED**（`tomcat.start()` 内部先走 `init()` 链，
`connector.init()` 已完成但未 `start()`）——`StandardService.removeConnector()` 里
`if (connector.getState().isAvailable())` 不成立（INITIALIZED 不是 available），
所以摘走**不会触发 stop**，只是从数组移除。这正是安全摘走的保证。

```
TomcatWebServer.start()（Spring 容器 finishRefresh 后）
  ├─ addPreviouslyRemovedConnectors()   ← ★ 把 Connector 加回来
  │    └─ service.addConnector(connector)
  │         └─ StandardService.addConnector() 中：若 Service 已启动 → connector.start()
  │              └─ Connector.startInternal() → protocolHandler.start() → endpoint.start()
  │                   └─ bindState == UNBOUND → bind() ← ★ 真正绑端口！
  │    （注：autoStart=false 时（测试场景）加回后立即 stopProtocolHandler 停掉）
  ├─ performDeferredLoadOnStartup()     ← 延迟的 loadOnStartup Servlet
  └─ checkThatConnectorsHaveStarted()
```

**最终效果**：容器（Engine→Host→Context→Wrapper）全部启动完成后，端口才监听——
避免了"端口已监听但应用未就绪"的窗口。

> **面试点**：Spring Boot 为什么这么折腾？——两阶段启动的三个收益：
> ① 端口监听前应用已完整初始化（`TomcatStarter` 的 SCI 回调已执行）；
> ② 启动失败时不会出现"端口被占但进程已死"的残留；
> ③ 可在绑端口前完成 `rethrowDeferredStartupExceptions()`（TomcatStarter 异常提前暴露，第 8 篇详讲）。

---

## 8. 本篇小结与面试要点

### 8.1 本篇地图

```
第 1 篇：容器树（Service 持有 Connector 数组）
第 2 篇：CoyoteAdapter.service()（桥接的调用入口）
第 3 篇：postParseRequest（桥接内部的路由）
第 5 篇（本篇）：Connector 两阶段启动 + 桥接组装
第 6 篇：NIO 网络层（Endpoint 内部：Acceptor/Poller/Worker）
第 8 篇：Spring Boot 两阶段启动的完整时序
```

### 8.2 面试要点速查

1. **Connector 是总装车间**：持有 ProtocolHandler + CoyoteAdapter，本身不处理网络
2. **协议工厂**：`ProtocolHandler.create("HTTP/1.1")` → `Http11NioProtocol`；`"AJP/1.3"` → `AjpNioProtocol`；完整类名 → 反射
3. **四层继承**：`ProtocolHandler` → `AbstractProtocol`（持 Endpoint）→ `AbstractHttp11Protocol`（HTTP 语义）→ `AbstractHttp11JsseProtocol`（TLS）→ `Http11NioProtocol`
4. **两阶段启动**：`init()` 准备资源（bindOnInit=true 时绑端口）→ `start()` 开始服务（**保证**绑端口：`bindState == UNBOUND` 才绑，已绑不重复）
5. **bindOnInit**：默认 true（init 时绑）；false 则推迟到 start（`bindState` 四态跟踪）
6. **bind 的本质**：`ServerSocketChannel.open() + bind(addr, acceptCount)`——`accept-count` = backlog
7. **CoyoteAdapter**：`Connector.initInternal()` 创建并绑定到 ProtocolHandler；`service()` 是请求入口
8. **属性委托链**：`connector.setProperty` → `protocolHandler.setProperty` → `endpoint.setProperty` → 反射 setter
9. **Spring Boot 两阶段**：`removeServiceConnectors`（START_EVENT 时摘走）+ `disableBindOnInit` → 容器先启动、端口后绑
10. **端点线程**：`NioEndpoint.startInternal` 创建 Executor + LimitLatch + Poller + Acceptor（第 6 篇展开）
