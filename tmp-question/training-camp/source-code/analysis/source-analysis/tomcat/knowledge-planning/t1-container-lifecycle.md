# T-1 容器层次 + Lifecycle — 知识规划

> 项目: Tomcat 10.1.x | 类型: **规范参考实现** (Servlet 6.0 / Jakarta EE)
> 核心文件: 8 文件 / 11581 行 | 预计 3-4 篇 v5 大纲
> 基线: Netty Ch12 HTTP/2 编解码 — T-1 回答 "容器如何分层管理组件的生命周期"

---

## §0.8 域审核前置

### 1. 域过载检查
- 核心类: 8 个 (StandardServer/StandardService/StandardEngine/StandardHost/StandardContext/StandardWrapper/ContainerBase/Lifecycle)
- 8 ≤ 10 → **不触发拆分阈值** ✅
- 11581 行总量 → StandardContext 占 5858 行(50.6%)，提取时需聚焦容器层次接口，不展开 Web 应用细节

### 2. 淘汰清单
- T-5 Session → 已砍 ✅ (Spring Boot 99% 用 spring-session-data-redis 替代)
- T-1 所有组件在嵌入式 Tomcat(Spring Boot)和生产独立部署中仍活跃使用 → 无淘汰项 ✅

### 3. 规范缺口
T-1 需要映射的 Servlet 规范接口:
- `jakarta.servlet.ServletContext` → 实现于 `ApplicationContext` (StandardContext.getServletContext())
- `jakarta.servlet.ServletConfig` → 实现于 `StandardWrapperFacade`
- `jakarta.servlet.Servlet` → 由 `StandardWrapper.loadServlet()` 实例化
- `jakarta.servlet.ServletRegistration.Dynamic` → `ApplicationServletRegistration`

### 4. 禁止过度加域
- 5 域结构合理 — T-1 的 ContainerBase 已覆盖 Lifecycle 实现，T-3 的 Pipeline 已在 ContainerBase.pipeline 字段处预留 → 不需独立加域 ✅

### 项目类型判定
| 维度 | 值 |
|------|------|
| 类型 | 规范参考实现 |
| 规范 | Servlet 6.0 (Jakarta EE 10) |
| 额外维度 | 规范对应表 + GoF 设计模式 + 架构分层意图 |
| 控制流 | **反转** — 容器调用用户 Servlet(Tomcat → 用户)，区别于 Netty(用户 → Pipeline) |

---

## 01 提取 — 逐源映射

### Lifecycle.java (288 行 — 组件生命周期接口)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| Lifecycle.java:26-73 | **11 态状态机**: NEW→INITIALIZING→INITIALIZED→STARTING_PREP→STARTING→STARTED→STOPPING_PREP→STOPPING→STOPPED→DESTROYING→DESTROYED + 任意态 → FAILED。状态转换由 start()/stop()/destroy() 方法触发，auto 关键字表示容器框架自动触发级联转换 | High |
| Lifecycle.java:207 | **init()**: 组件初始化 — 完成对象创建后的配置加载和资源分配。触发 INIT_EVENT → LifecycleState.INITIALIZED | High |
| Lifecycle.java:227 | **start()**: 组件激活 — 从 NEW 调用会先调用 init(); 从 INITIALIZED 调用直接启动。触发 BEFORE_START→START→AFTER_START 三事件，状态直通 STARTING_PREP→STARTING→STARTED | High |
| Lifecycle.java:250 | **stop()**: 组件停用 — 触发 BEFORE_STOP→STOP→AFTER_STOP，状态走 STOPPING_PREP→STOPPING→STOPPED。FAILED 态走 stop() 时跳过 STOPPING_PREP 直接进入 STOPPING | High |
| Lifecycle.java:261 | **destroy()**: 组件销毁 — 释放所有资源。触发 DESTROY_EVENT → LifecycleState.DESTROYED。stop() 后必须调用 destroy() 否则资源泄漏 | High |
| Lifecycle.java:90-166 | **13 种 LifecycleEvent 常量**: BEFORE_INIT/AFTER_INIT/START/BEFORE_START/AFTER_START/STOP/BEFORE_STOP/AFTER_STOP/BEFORE_DESTROY/AFTER_DESTROY/PERIODIC/CONFIGURE_START/CONFIGURE_STOP。前 10 个对应 4 阶段生命周期，PERIODIC 用于定期任务，CONFIGURE_START/STOP 用于组件从 XML/server.xml 注入配置 | High |
| Lifecycle.java:172-194 | **观察者模式**: addLifecycleListener/findLifecycleListeners/removeLifecycleListener — 组件状态变化通知监听器。容器层次中父容器注册为子容器监听器以感知子容器状态 | High |
| Lifecycle.java:269 | **getState()**: 返回 LifecycleState 枚举 — 用于状态机判断 | High |
| Lifecycle.java:282-287 | **SingleUse 标记接口**: 实现此接口的组件在 stop() 后自动调 destroy() — 典型场景: 一次性执行后不再重启的组件 | Medium |

### ContainerBase.java (1225 行 — 所有容器的抽象基类)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| ContainerBase.java:124 | **ContainerBase extends LifecycleMBeanBase implements Container** — 继承链: ContainerBase → LifecycleMBeanBase → LifecycleBase → Lifecycle。LifecycleMBeanBase 提供 JMX MBean 注册能力 | High |
| ContainerBase.java:75-77 | **Chain of Responsibility 模式**: 每个 Container 内建 Pipeline + Valve 链。Javadoc 明确标注 "Chain of Responsibility" — 这是 Tomcat 请求处理的核心架构模式 | High |
| ContainerBase.java:154-155 | **children HashMap + ReadWriteLock**: `HashMap<String,Container> children` + `ReentrantReadWriteLock childrenLock` — 容器层次数据结构的线程安全基础。写操作(添加/删除子容器)用 writeLock，查询(findChild/findChildren)用 readLock | High |
| ContainerBase.java:566-604 | **addChild / addChildInternal**: 1) writeLock 加锁 2) 重名校验 3) child.setParent(this) 建立双向引用 4) children.put() 5) fireContainerEvent(ADD_CHILD_EVENT) 6) 如果当前容器已运行且 startChildren=true → child.start() 自动启动子容器。步骤 6 在锁外执行(防止长时间锁定) | High |
| ContainerBase.java:620-630 | **findChild(name)**: 通过名称查子容器 — readLock 加锁 → children.get(name) — O(1) HashMap 查找 | High |
| ContainerBase.java:634-639 | **findChildren()**: 返回全部子容器数组 — readLock 加锁 → children.values().toArray() | High |
| ContainerBase.java:447-460 | **getParent()/setParent()**: 双向引用 — 父容器持有 children Map，子容器持有 parent 引用。setParent 时校验容器类型兼容性(Engine→Host→Context→Wrapper 层次不可颠倒) | High |
| ContainerBase.java:242-244 | **startChildren 配置**: 控制动态添加子容器时是否自动启动。Server.xml 静态配置的子容器在 startInternal 遍历启动，动态添加的依赖此标志 | Medium |
| ContainerBase.java:258-262 | **startStopExecutor**: 用于线程化启停子容器 — `InlineExecutorService` 默认单线程串行，可配线程数并行启停(大集群场景) | Medium |

### StandardServer.java (1031 行 — 顶级 Server 组件)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardServer.java:68 | **StandardServer extends LifecycleMBeanBase implements Server** — 继承 Lifecycle 状态机 + JMX MBean 注册能力 | High |
| StandardServer.java:120-149 | **shutdown 配置三要素**: port=8005 / address="localhost" / shutdown="SHUTDOWN" — socket 监听接收 shutdown 命令字符串 | High |
| StandardServer.java:122 | **portOffset**: 默认 0 — 多实例部署时所有端口统一偏移，`getPortWithOffset()` (L314-323) 仅对正端口叠加偏移 | High |
| StandardServer.java:140-144 | **services[] 数组 + ReentrantReadWriteLock** — Service 管理的数据结构，与 ContainerBase.children 同模式 | High |
| StandardServer.java:459-483 | **addService(service)**: writeLock → service.setServer(this) → 写入数组 → 若 Server 已可用 → service.start() 热启动 | High |
| StandardServer.java:639-668 | **findService / findServices**: readLock 保护 — findService 按名线性查找，findServices 返回 clone 快照 | High |
| StandardServer.java:937-959 | **initInternal()**: StringCache JMX 注册 → MBeanFactory 注册 → globalNamingResources.init() → services[].init() — 自顶向下 | High |
| StandardServer.java:855-877 | **startInternal()**: fire CONFIGURE_START_EVENT → setState(STARTING) → utilityExecutor启动+JMX → globalNamingResources.start() → services[].start() → 调度 periodicLifecycleEvent | High |
| StandardServer.java:898-929 | **stopInternal()**: setState(STOPPING) → cancel monitor → fire CONFIGURE_STOP_EVENT → services[].stop() → utilityExecutor关闭 → globalNamingResources.stop() → stopAwait() | High |
| StandardServer.java:508-635 | **await() 三种模式**: port=-2 嵌入模式直接返回 / port=-1 sleep循环+volatile flag / port>0 ServerSocket 接收 shutdown 命令。防 DoS: 期望长度=1024+randomSeed 防缓冲区预测攻击 | High |
| StandardServer.java:515-529 | **await() port=-1 轻量等待**: `while(!stopAwait) Thread.sleep(10000)` — 无 socket 开销，适用于容器管理(SIGTERM)的关闭方式 | High |
| StandardServer.java:580-585 | **防 DoS 设计**: `expected = 1024 + random.nextInt()` — 攻击者无法精确预测缓冲区大小，防止精确填满缓冲区绕过读取的风险 | High |
| StandardServer.java:880-894 | **startPeriodicLifecycleEvent()**: 通过 utilityExecutor 调度 PERIODIC_EVENT — 周期(秒)由 periodicEventDelay 配置，触发 fireLifecycleEvent(PERIODIC_EVENT) | High |

### StandardService.java (597 行 — Server↔Engine 桥梁)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardService.java:53 | **StandardService extends LifecycleMBeanBase implements Service** — 继承 Lifecycle 状态机 + JMX | High |
| StandardService.java:81-88 | **Connector[] + Executor[] 双数组各配 ReadWriteLock** — Service 层管理两个独立资源池，锁解耦 | High |
| StandardService.java:90 | **`Engine engine`** — Service 持有 Engine 的直接引用，是 Server→Connector 和 Container 层次的交汇点 | High |
| StandardService.java:97-103 | **Mapper + MapperListener**: `Mapper mapper = new Mapper()` + `MapperListener mapperListener = new MapperListener(this)` — 请求路由表在 Service 层构造，MapperListener 监听容器变化事件动态更新 | High |
| StandardService.java:134-173 | **setContainer(Engine)**: 旧Engine断开→新Engine建立双向关联→若Service已可用则start新Engine+重启MapperListener→stop旧Engine — 热替换语义 | High |
| StandardService.java:203-227 | **addConnector(connector)**: writeLock → connector.setService(this) → 写入数组 → 若 Service 已可用 → connector.start() 热注册 | High |
| StandardService.java:504-527 | **initInternal()**: engine.init() → Executor JMX domain同步+init → mapperListener.init() → connectors[].init() | High |
| StandardService.java:406-431 | **startInternal()**: setState(STARTING) → **engine.start() 先于 connectors.start()** — 确保请求到达时后端 Container 已就绪 | High |
| StandardService.java:441-494 | **stopInternal() 优雅关闭**: connectors 优雅关闭(closeServerSocketGraceful) → pause connectors → engine.stop() → connectors.stop() → mapperListener.stop() → executors.stop() — **顺序对称反转** | High |
| StandardService.java:447-449 | **closeServerSocketGraceful()**: 停止接受新连接但让现有请求完成 — 等待 `gracefulStopAwaitMillis` 毫秒后强制关闭 | High |
| StandardService.java:573-590 | **getDomainInternal()**: Engine名称优先→回退Service名称 — JMX域解析策略 | High |

### StandardEngine.java (430 行 — Engine 容器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardEngine.java:51 | **StandardEngine extends ContainerBase** — 继承 children HashMap + ReadWriteLock，不需自建容器管理 | High |
| StandardEngine.java:64 | **backgroundProcessorDelay=10** — Engine 层的 background process 每 10 秒执行一次（比默认-1=继承父级更明确） | High |
| StandardEngine.java:186-190 | **initInternal()**: getRealm() 确保 NullRealm → super.initInternal() (ContainerBase) — 先确保 Realm 存在再初始化子容器 | High |
| StandardEngine.java:195-230 | **startInternal()**: log 服务信息 → getRealm() → super.startInternal() → 检查 defaultHost 对应的 Host 子容器存在 — 防止无默认Host启动 | High |
| StandardEngine.java:107-127 | **setDefaultHost(host)**: 更新字段 + `service.getMapper().setDefaultHostName(host)` — 直接传播到 Mapper 路由表，无需 MapperListener 重启 | High |
| StandardEngine.java:142-148 | **getService()/setService(service)** — Engine↔Service 双向引用，Engine 通过此引用访问 Mapper 和 Connector | High |

### StandardHost.java (812 行 — Host 容器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardHost.java:56 | **StandardHost extends ContainerBase** — 继承容器层次管理 | High |
| StandardHost.java:88 | **appBase = "webapps"** — Web 应用部署的基础目录 | High |
| StandardHost.java:112 | **autoDeploy = true** — 运行时自动检测 appBase 变化并部署/重新部署 | High |
| StandardHost.java:130 | **deployOnStartup = true** — 启动时扫描 appBase 并部署所有 Context | High |
| StandardHost.java:149 | **errorReportValveClass** — 默认 `ErrorReportValve`，用于生成 HTML 错误页面 | Medium |
| StandardHost.java:165-178 | **deployIgnore pattern** — 自动部署时跳过的文件/目录模式，防止临时文件触发热部署 | Medium |

### StandardContext.java (5858 行 — Context 容器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardContext.java | **StandardContext extends ContainerBase** — 继承容器层次管理，children 为 Wrapper | High |
| StandardContext.java | **docBase**: Web 应用根目录路径 — Context 容器特有配置 | High |
| StandardContext.java | **addChild() 添加 Wrapper**: 每个 Servlet 映射为一个 Wrapper 子容器 — 容器层次的最末级 | High |
| StandardContext.java | **reloadable**: 控制是否监控 /WEB-INF/classes 和 /WEB-INF/lib 变化自动重载 | Medium |
| StandardContext.java | **getServletContext()** → 实现规范: ServletContext (6.0) — Context 容器与 Servlet 规范的核心映射点 | High |

### StandardWrapper.java (1340 行 — Wrapper 容器)

| Source | Inferred Knowledge Point | Confidence |
|--------|------------------------|------------|
| StandardWrapper.java | **StandardWrapper extends ContainerBase** — 容器层次最末级，无子容器 | High |
| StandardWrapper.java | **loadOnStartup**: 控制是否在 Context 启动时加载 Servlet → 实现规范: Servlet 3.0+ loadOnStartup | High |
| StandardWrapper.java | **servletClass**: 需要通过此 Wrapper 调用的 Servlet 全限定类名 | High |
| StandardWrapper.java | **loadServlet()/unloadServlet()**: Servlet 实例的生命周期管理 — init()→service()→destroy() 映射到 Servlet 规范生命周期 | High |
| StandardWrapper.java | **SingleThreadModel 检测**: 若 Servlet 实现 SingleThreadModel → 创建实例池 — 已废弃(Servlet 2.4)但 Tomcat 仍兼容 | Low |
| StandardWrapper.java | **asyncSupported**: 标记此 Servlet 是否支持异步处理 → 实现规范: Servlet 3.0+ AsyncContext | Medium |

---

## 02 聚合 — P1/P2/P3 分级

### P1 — 全系统共识（≥5 处引用）

| KP | 涉及文件 | 证据 |
|------|:--:|------|
| **Lifecycle 11 态状态机** (NEW→...→DESTROYED) | 8 | Lifecycle 接口定义，全部 7 个 StandardXxx 类通过 LifecycleBase→LifecycleMBeanBase→ContainerBase 链继承 |
| **Container 层次 (parent/children)** | 5 | ContainerBase — StandardEngine/Host/Context/Wrapper 均继承 children HashMap + ReadWriteLock |
| **ReadWriteLock 并发模式** | 5 | ContainerBase.childrenLock + StandardServer.servicesLock + StandardService.connectorsLock+executorsLock — 同级并发控制模式 |
| **initInternal→startInternal 级联** | 8 | 全部 7 个 StandardXxx + ContainerBase 实现 — 每层先处理自身再 super.xxx() 驱动子容器 |
| **stopInternal 反向级联** | 8 | 全部容器 — 启动顺序(自顶向下)与停止顺序(自底向上)完全对称反转 |

### P2 — 局部重要（2-4 处引用）

| KP | 涉及文件 | 证据 |
|------|:--:|------|
| **Mapper/MapperListener 请求路由** | 2 | StandardService(Mapper 构造+Listener) + StandardEngine(setDefaultHost 传播到 Mapper) |
| **addChild 自动启动** | 4 | ContainerBase.addChild → StandardHost(添加 Context)→StandardContext(添加 Wrapper)→StandardEngine(添加 Host) |
| **JMX MBean 注册** | 3 | LifecycleMBeanBase + StandardServer(register UtilityExecutor/StringCache/MBeanFactory) + StandardService(register Executor) |
| **优雅关闭 (graceful stop)** | 2 | StandardService(closeServerSocketGraceful+gracefulStopAwaitMillis) + StandardServer(await 停止) |
| **Realm 认证集成** | 3 | StandardEngine(getRealm 确保 NullRealm) + ContainerBase(getRealmInternal) + StandardContext(Realm 配置) |

### P3 — 独立知识点（1 处独有）

| KP | 文件 | 原因 |
|------|------|------|
| **await/shutdown 三种模式** | StandardServer | 顶级 Server 独有 — embed/sleep/socket 模式 |
| **portOffset 多实例部署** | StandardServer | 全局端口偏移 — 所有 Connector 端口统一加偏移 |
| **防 DoS 随机种子** | StandardServer | 攻击者无法预测 shutdown socket 缓冲区大小 |
| **autoDeploy + deployOnStartup** | StandardHost | Host 级 Web 应用自动部署 — 唯一的自动部署容器 |
| **servletClass + loadOnStartup** | StandardWrapper | Wrapper 级 Servlet 生命周期 — 容器层次最末级独有 |
| **Engine 与 Mapper 的直接耦合** | StandardEngine | setDefaultHost 直接调 getService().getMapper() — 唯一直接调 Mapper 的容器 |

---

## 03 深度分类 — 🔴🟡🟢 per KP

| KP 群 | 级别 | 判定理由 |
|------|:--:|------|
| **Lifecycle 11 态状态机** (NEW→INITIALIZED→STARTED→STOPPED→DESTROYED) | 🔴 Deep | 整个 Tomcat 的生命周期管理都建立在这个状态机之上 — 不理解它就无法理解任何组件的启动流程 |
| **Container 层次 (5 级树 + ReadWriteLock)** (Server→Service→Engine→Host→Context→Wrapper) | 🔴 Deep | Tomcat 的架构骨架 — 所有请求从 Connector 进入后按此层次路由到 Wrapper(Servlet)。Ch12 全维度深审已证: 框架结构缺则文章断裂 |
| **startInternal/stopInternal 级联 + 顺序反转** (自顶向下启动/自底向上停止) | 🔴 Deep | 承载 Tomcat "整体启动"的设计决策 — 为什么 Engine.start 在 Connector.start 之前? 为什么停止顺序反转? 是理解容器生命周期依赖的关键 |
| **Mapper/MapperListener 动态路由** | 🟡 Working | 请求路由在 Service 层 — MapperListener 订阅 ContainerEvent 动态更新路由表。重要但不影响容器层次基础理解 |
| **JMX MBean 注册/注销** | 🟡 Working | 运维管理接口 — LifecycleMBeanBase 自动注册/注销，StandardServer 额外注册 UtilityExecutor/StringCache。内嵌 Spring Boot 时不使用 JMX |
| **await/shutdown 三模式** | 🟡 Working | 生产部署需要但概念上不复杂 — embed/sleep/socket 三种等待模式的选择 |
| **autoDeploy/deployOnStartup** | 🟡 Working | Host 级别的热部署能力 — 生产环境常关闭(autoDeploy=false)，开发环境核心功能 |
| **servletClass/loadOnStartup/SingleThreadModel** | 🟡 Working | Wrapper 级 Servlet 生命周期 — 与 Servlet 规范直接对应，但属于细节配置非架构 |

---

## 04 聚类 — 教学顺序

**Cluster A: Lifecycle 契约 — 所有容器共享的启动语言** (→ 对应 §1)
- Lifecycle 11 态状态机: NEW→INITIALIZED→STARTED→STOPPED→DESTROYED
- LifecycleEvent 观察者模式: 13 种事件常量 → LifecycleListener
- LifecycleBase 模板方法: initInternal/startInternal/stopInternal/destroyInternal
- 规范对应: `LifecycleBase.init()/start()/stop()/destroy()` 的模板方法模式 → [模式: Template Method]

**Cluster B: Container 层次 — 5 级容器树的结构基础** (→ 对应 §2)
- ContainerBase: children HashMap + ReadWriteLock → parent/children 双向引用
- addChild 流程: writeLock→重名校验→setParent→put→fireEvent→autoStart
- 容器类型约束: Engine→Host→Context→Wrapper 层次不可颠倒
- Container 接口中的 Pipeline + Valve 预留 (连接 T-3)
- 规范对应: `Engine`/`Host`/`Context`/`Wrapper` → Servlet 规范 2.3 容器模型
- 架构意图: **组合模式** — 容器自身是 Container 且持有子 Container，形成递归树 [模式: Composite]
- 控制流: **反转** — 父容器控制子容器生命周期，区别于 Netty 的"用户调 Pipeline"

**Cluster C: Server + Service — 顶层编排器** (→ 对应 §3)
- StandardServer: services[] 管理 + await 关闭等待
- StandardService: Engine→Connector→Mapper 三要素的桥梁
- 启动级联: Server.init()→Service.init()→Engine.init()→Host→Context→Wrapper
- 停止级联: Connector.gracefulStop→Engine.stop()→Connector.stop()→MapperListener.stop() — 顺序反转
- 规范对应: `Service.addConnector()` — Connector 注册到容器层次 → `getMapper().setDefaultHostName()` 直接调用
- 架构意图: **Facade 模式** — StandardServer 对外暴露单一 Server 接口，内部管理 Server/Service 两层 [模式: Facade]

**Cluster D: 末级容器 — Engine/Host/Context/Wrapper 的差异化角色** (→ 对应 §4)
- StandardEngine: defaultHost 管理 → getService().getMapper() 传播
- StandardHost: appBase + autoDeploy/deployOnStartup
- StandardContext: docBase + ServletContext 映射 + Wrapper 子容器管理
- StandardWrapper: servletClass + loadOnStartup + loadServlet
- 规范映射表: `ServletContext`(Context) / `ServletConfig`(Wrapper) / `Servlet`(Wrapper.loadServlet) / `FilterChain`(T-3)
- 架构意图: **分层职责** — 每层容器各有特定配置域，不跨层混淆

> → 引出 T-2 Connector+Adapter — Container 层次解决了"请求到达 Engine 后如何路由到 Wrapper"，但"请求如何从 TCP 端口到达 Engine"是 Connector 的职责。T-1 容器层次建立了 Engine→Host→Context→Wrapper 路由目标，T-2 回答 "Http11NioProtocol→CoyoteAdapter 如何把 TCP 字节流转换为 Request/Response 对象交给 Engine"。
