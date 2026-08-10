# T-1 §3 Server+Service — 顶层编排 + 启动/停止顺序反转

> 依赖 §2 | 🔴 Deep | 5 KP | [模式: Facade]

**读者处境**: 已理解容器树的双向引用和 Lifecycle 级联。现在站在 Tomcat 的入口: `Catalina.start()` 调了 `server.start()` — 这行代码怎么把 Server→Service→Engine→Host→Context→Wrapper 全部拉起来？停止时为什么顺序一定要反转？

### 1. StandardServer — services[] 数组 + await 等待

场景: Tomcat 部署在生产——启动脚本执行 `catalina.sh run`。`Catalina` 类调 `server.start()`→StandardServer 启动→全部 Service 启动→然后 `server.await()` 阻塞当前线程——等待 shutdown 命令或 SIGTERM。`await()` 是 Tomcat 进程不立即退出的原因。

源码路径:
- `StandardServer.java:140-144` — `Service[] services` + ReentrantReadWriteLock——数组非 HashMap(Service 数量极少, 无需按名查找)
- `StandardServer.java:459-483` — `addService()`: writeLock→`service.setServer(this)`→数组扩展→若 Server 已可用→`service.start()` 热启动
- `StandardServer.java:855-877` — `startInternal()`: fire CONFIGURE_START_EVENT→setState(STARTING)→utilityExecutor启动+JMX→`globalNamingResources.start()`→`services[].start()`→调度 periodicLifecycleEvent
- `StandardServer.java:508-635` — `await()` 三种模式: port=-2 嵌入模式直接返回 / port=-1 sleep循环+stopAwait flag / port>0 ServerSocket 接收 "SHUTDOWN" 命令

关键设计: **Facade 模式** — `StandardServer` 对外暴露单一 `Server` 接口，内部管理 Service 组数、utilityExecutor、Naming、JMX。外部(Catalina)只需 `server.start()` 和 `server.await()` 两个调用——不需知道内部有多少个 Service。**port=-1 模式**是容器管理(SIGTERM)的标准方式——不暴露 TCP shutdown 端口——外部通过 `kill <pid>` 发送 SIGTERM→JVM ShutdownHook→`server.stop()`→`stopAwait=true`→`await()` 返回。**防 DoS 随机种子** (L580-585): 攻击者可能精确填满 1024 字节缓冲区+下一个字节为 "SHUTDOWN"——随机化期望长度使攻击不可行。 [模式: Facade]

数据流: `Catalina.start()`→`server.start()`→`LifecycleBase.start()`→`startInternal()`→fire CONFIGURE_START_EVENT→外部 server.xml 解析器注入配置→setState(STARTING)→`globalNamingResources.start()`→for each service: `service.start()`→各 Service 内部走自己的 startInternal→返回→utilityExecutor.scheduleWithFixedDelay(periodicEvent)→setState(STARTED)→`server.await()` 阻塞。

### 2. StandardService — Engine+Connector+Mapper 三要素桥梁

场景: 一个 Tomcat 实例可能绑定两个 Connector——8080(HTTP)和 8443(HTTPS)——它们都属于同一个 Service、共享同一个 Engine。Service 就是 Connectors→Engine 的粘合层——Connector 收到请求后——通过 Service 找到 Engine——Engine 调用 pipeline→Host→Context→Wrapper。

源码路径:
- `StandardService.java:90` — `Engine engine` — Service 持有 Engine 直接引用
- `StandardService.java:97-103` — `Mapper mapper` + `MapperListener mapperListener` — 请求路由表在 Service 层
- `StandardService.java:504-527` — `initInternal()`: engine.init()→向 Executor 注入 JMX domain→mapperListener.init()→connectors[].init()
- `StandardService.java:406-431` — `startInternal()`: **engine.start() 在 connectors.start() 之前** — 关键顺序决策
- `StandardService.java:441-494` — `stopInternal()`: connectors 优雅关闭→connectors.pause()→engine.stop()→connectors.stop()→mapperListener.stop()

关键设计: **Why engine.start() before connectors.start()?** 考虑 Connector 在 engine 启动前就开始 accept()——此时 HTTP 请求到达——调用 `CoyoteAdapter.service(Request, Response)`——Adapter 找到 Engine——调 `engine.getPipeline().getFirst().invoke()`——但 Engine 还没 start——Pipeline 是 null——NPE。**容器的启动顺序不是可选的**——依赖方(Connector 依赖 Engine)必须在被依赖方启动之后启动。反过来停止: Connector 先 `closeServerSocketGraceful()`——停止接受新连接——让现有请求完成——再 `connectors.pause()`——再 engine.stop()——再 connectors.stop()——**完全反转**。这就是 **优雅关闭**: 不能先关 Engine(在处理的请求会断)——不能先关 Connector(新连接可以进但没有 Engine 处理)——必须先堵住新连接、排空现有连接、再关 Engine、最后关 Connector。

→ 实现规范: Service/Engine 关系 → Servlet 规范未定义，属于 Tomcat 架构层。

数据流: 启动: `service.start()`→`startInternal()`→`engine.start()`(Engine 内部走 Lifecycle→ContainerBase→start children→...→setState STARTED)→所有 Executor.start()→`mapperListener.start()`(监听 ContainerEvent 建路由表)→`connectors[].start()`(各 Connector 绑定端口 accept)→`service STARTED`。停止: `service.stop()`→for each connector: `closeServerSocketGraceful()`→for each connector: `pause()`→`engine.stop()`(反向停止 children)→for each connector: `stop()`→`mapperListener.stop()`→`executors.stop()`。

### 3. Server↔Service↔Engine 三级级联 — 启动一个触发全部

场景: 开发和测试环境——命令行启动 `Catalina.main()`→经过层层委托直到 `server.start()`——开发者完全没有意识到之下发生了 5 层容器的全部初始化——这就是 Composite+Template Method 的组合威力。

源码路径: StandardServer/StandardService/StandardEngine 三个类的 `initInternal()` 和 `startInternal()` 方法。完整的启动链: `Server.init()`→`services[].init()`→`engine.init()`→`hosts[].init()`→`contexts[].init()`→`wrappers[].init()`。停止完整反转。

关键设计: **级联不是递归**——init() 和 start() 内部的 `super.initInternal()`/`super.startInternal()` 不是递归调 children——ContainerBase 通过 `startStopExecutor` 线程池并行启停 children。5 层容器的每一层都是独立的 `LifecycleBase` 执行——状态机确保每层的 `initInternal()` 只执行一次——不会因为"父 start→子 start→父的父的 start"而产生重复初始化。

数据流: `catalina.start()`→...→`server.start()`→Server.startInternal→for each service: `service.start()`→Service.startInternal→`engine.start()`→Engine.startInternal→ContainerBase.startInternal→for each host child: `host.start()`→Host.startInternal→ContainerBase.startInternal→for each context child: `context.start()`→...→全部返回→5层全部 STARTED→`server.await()` 阻塞。

→ 引出 §4 末级容器 — 5 层结构已连通——但 Engine/Host/Context/Wrapper 除了管理子容器，各自还有独特的配置域和规范映射: Engine 的 defaultHost、Host 的 autoDeploy、Context 的 docBase、Wrapper 的 loadOnStartup——规范规定每层做什么——Tomcat 实现每层怎么管。
