# T-1 §1 Lifecycle — 11 态状态机 + 观察者事件

> 依赖: 无(第一篇) | 🔴 Deep | 4 KP | 跨出 Netty 进入规范参考实现

**读者处境**: 学完 Netty 13 章——习惯了"用户调 Pipeline→Handler 链处理"的控制流。Tomcat 完全不同: 容器调你的 Servlet——需要先理解"容器如何管理自己的启动/停止"，才能理解它为什么能调用你的代码。

### 1. 11 态状态机 — NEW→DESTROYED + 任意态→FAILED

场景: `Catalina.start()` 调用 `server.start()`——这行代码触发的不只是 Server 启动，而是一条自上而下的级联: Server→Service→Engine→Host→Context→Wrapper——全部 5 层容器依次经历同一个状态机。没有这个统一契约，5 层容器就是 5 个孤立的类。

源码路径: `Lifecycle.java:26-73` (Javadoc ASCII 图)。11 态: `NEW`→`INITIALIZING`→`INITIALIZED`→`STARTING_PREP`→`STARTING`→`STARTED`→`STOPPING_PREP`→`STOPPING`→`STOPPED`→`DESTROYING`→`DESTROYED` + 任意态→`FAILED`。"auto" 标记的转换由容器框架自动触发——开发者在子类中实现 `initInternal()`/`startInternal()`/`stopInternal()`/`destroyInternal()` 四个钩子方法，框架负责在正确的状态间转换。

关键设计: **Template Method 模式** — `LifecycleBase.init()` 调用 `setState(INITIALIZING)`→`initInternal()`→`setState(INITIALIZED)`。子类(StandardServer/StandardService/ContainerBase)只覆写 `initInternal()` 实现自身逻辑，不需要管理状态转换——框架保证"initInternal 执行前状态已是 INITIALIZING，执行后已是 INITIALIZED"。为什么用 Template Method? 因为 7 个容器类如果各自管理状态转换——只要有一个忘了 `setState()`——整个级联的时序就乱了。 [模式: Template Method]

→ 实现规范: 非 Servlet 规范——Tomcat 自身架构设计。GoF Template Method。

数据流: `server.init()`→`LifecycleBase.init()`→`setState(INITIALIZING)`→`initInternal()`(子类) →`setState(INITIALIZED)`。同理 `start()` 走 `STARTING_PREP→STARTING→STARTED` 三态。`stop()` 走 `STOPPING_PREP→STOPPING→STOPPED`。任意一步抛 `LifecycleException`→`setState(FAILED)`——状态机不忽略错误——所有容器都能感知 FAILED 并停止级联。

### 2. 13 种 LifecycleEvent — 观察者解码容器状态

场景: 运维想知道"Server 启动完成了吗？"——不是轮询 `server.getState()`。Tomcat 的答案是 `LifecycleListener`: 注册一个监听器——在 `AFTER_START_EVENT` 时收到回调——逻辑与状态机解耦。

源码路径: `Lifecycle.java:90-166`。13 种事件常量: `BEFORE_INIT`/`AFTER_INIT`/`START`/`BEFORE_START`/`AFTER_START`/`STOP`/`BEFORE_STOP`/`AFTER_STOP`/`BEFORE_DESTROY`/`AFTER_DESTROY`/`PERIODIC`/`CONFIGURE_START`/`CONFIGURE_STOP`。`Lifecycle.java:172-194` — `addLifecycleListener`/`removeLifecycleListener`/`findLifecycleListeners`。

关键设计: **观察者模式** — 容器状态变化时通知所有注册的 Listener，Listener 本身不干预状态转换(只读消费)。`CONFIGURE_START`/`CONFIGURE_STOP` 是为 `server.xml` 解析设计的——外部配置(XML 解析器)在 `STARTING_PREP` 阶段通过 `CONFIGURE_START_EVENT` 将配置注入到容器属性——容器不用依赖 XML 解析器。`PERIODIC` 用于定期后台任务——`StandardServer.startPeriodicLifecycleEvent()`(L880-894) 通过 utilityExecutor 调度，触发定期事件给监听器。 [模式: Observer]

数据流: 外部注册 `server.addLifecycleListener(myListener)`→`LifecycleBase.start()`→`setState(STARTING_PREP)`→`fireLifecycleEvent(BEFORE_START_EVENT, null)`→遍历 listeners→各 listener.lifecycleEvent(event)→...→`setState(STARTED)`→`fireLifecycleEvent(AFTER_START_EVENT, null)`→listener 收到通知。

### 3. initInternal/startInternal/stopInternal/destroyInternal — 4 个钩子 + 容器级联

场景: ContainerBase 有一个 `Realm` 属性——`Realm` 是子组件(不直接处理请求)——需要在容器启动时一起启动。`ContainerBase.startInternal()` 先调 `Realm.start()` 再调 `children[].start()`——凭什么 `Realm` 在 children 之前？因为 children(Wrappers)可能依赖 Realm 做认证。

源码路径: `ContainerBase.java:780-845`。`startInternal()`: `setState(STARTING)`→启动 Pipeline→**先用 startStopExecutor 并行启动所有 children**→再启动 Realm/Cluster 子组件。`stopInternal()`: 停 Pipeline→**先用 startStopExecutor 并行停止所有 children**→再停 Realm/Cluster。

关键设计: **子组件启动顺序由具体容器决定**——LifecycleBase 不规定顺序。ContainerBase 选择 children 先于 Realm——因为 children 是核心功能(请求处理)，Realm 是辅助功能(认证)。但 Engine 的 `initInternal()`(L186-190) 先 `getRealm()`(确保 NullRealm 存在)再 `super.initInternal()`——因为 Realm 为空时 Engine 无法初始化任何子容器。这就是 **Template Method 的灵活度**: 框架管状态转换，子类管执行顺序。

数据流: `engine.start()`→`LifecycleBase.start()`→`setState(STARTING_PREP)`→`startInternal()`(子类)→`super.startInternal()`(ContainerBase)→并行 `children[].start()`→各 child 重复此流程→全部 children 启动完成→返回→setState(STARTED)→fire AFTER_START_EVENT。

→ 引出 §2 Container 层次 — 理解了"所有容器共享同一个状态机"之后，下一个问题: "Server→Engine→Host→Context→Wrapper 这 5 层树是怎么建起来的？addChild 背后的双向引用和并发安全怎么保证？"
