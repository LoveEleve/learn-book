# S-4 SpringApplication.run — 启动全流程 (环境 → 容器 → refresh → 回调)

> 依赖 s8 refresh + C-9 + C-3 | 🔴 Deep | 6 KP | [模式: 模板方法 + 工厂 + 事件广播]

**读者处境**: `SpringApplication.run(MyApp.class, args)` 一行启动 — 内部六步做什么?环境怎么先于容器就绪?Servlet/Reactive 容器怎么选?

### 1. run() 六步总流程 — Boot 启动地图

场景: run() 是 Boot 的 main 入口 — 六步: 事件广播开始 → 环境准备 → 容器创建 → 容器准备 → refresh(完整启动) → 回调(复用 C-9)。

源码路径:
- `SpringApplication.java:301` — **run()**: L306 createBootstrapContext → L310 listeners.starting(启动事件) → L313 prepareEnvironment(环境, §2) → L315 createApplicationContext(容器, §3) → L317 prepareContext(装配) → L318 refreshContext(s8 refresh 全流程) → L322 StartupInfoLogger(启动耗时) → L325 callRunners(C-9: Runner 执行) → L332 listeners.ready(ApplicationReadyEvent, C-9 已述在 runners 后)
- `SpringApplication.java:328,336` — **异常兜底**: handleRunFailure — 启动失败走失败分析器(FailureAnalyzer 机制, 后续域展开)

关键设计: **Why 环境在容器创建前？** 容器创建需要环境(如 web 类型/属性源决定配置类); 环境先就绪, refresh 时直接可用。**Why 复用 s8/C-9？** refresh 12 步(s8)与 callRunners/ready(C-9)已在 Spring 侧讲透 — 此处只标注衔接点, 展开 run 特有的环境/容器部分(06 §2.5)。[模式: 模板方法 — 固定六步]

数据流: run(args) → starting(广播) → prepareEnvironment(§2) → createApplicationContext(§3) → prepareContext: 注册主源(MyApp.class)+应用初始化器 → refreshContext: s8 的 12 步(配置解析/Bean 创建/自动装配 S-2/S-3 在此) → StartupInfoLogger → callRunners(C-9) → ready(事件) → 返回容器。

### 2. prepareEnvironment — 环境先于容器就绪

场景: 环境准备做了什么?怎么推断"这是个 Web 应用"?

源码路径:
- `SpringApplication.java:350` — **prepareEnvironment**: L350 `getOrCreateEnvironment()` → configureEnvironment(L492: 加默认属性源/命令行属性) → L353 `listeners.environmentPrepared`(广播 — EnvironmentPostProcessor 在此处理, 后续域展开) → L358 `bindToSpringApplication`(把 spring.main.* 绑定到 SpringApplication 属性)
- `SpringApplication.java:275,473` — **类型推断**: WebApplicationType.deduceFromClasspath — 从 classpath 是否有 Servlet/WebFlux 类推断 SERVLET/REACTIVE/NONE → 决定 Environment 类型(StandardServletEnvironment 等)与容器类型
- 与 C-3 衔接: 创建的 Environment 就是 C-3 的 ConfigurableEnvironment — 属性源体系复用

关键设计: **Why 推断 Web 类型？** 同一套 run() 要服务 Servlet(Spring MVC)、Reactive(WebFlux)、纯应用三类 — 从 classpath 特征(类存在性)自动推断, 零配置; 决定后续 Environment 与容器选择。**Why bindToSpringApplication？** 让 spring.main.web-application-type 等能配置化覆盖推断结果。[模式: 环境准备 + 类型推断]

数据流: run → deduceFromClasspath: classpath 有 jakarta.servlet + Spring MVC → SERVLET → getOrCreateEnvironment: StandardServletEnvironment(在 C-3 基础上加 servlet 属性源) → configureEnvironment(命令行属性) → environmentPrepared 事件(ConfigData 加载 application.yml 在此) → bindToSpringApplication: spring.main.* → 属性。

### 3. createApplicationContext — 三种容器选型

场景: Web 类型决定了容器: Servlet 应用用 AnnotationConfigServletWebServerApplicationContext(能启动嵌入式容器), 响应式用 Reactive 版, 纯应用用普通 AnnotationConfig。

源码路径:
- `SpringApplication.java:573` — **createApplicationContext**: `applicationContextFactory.create(webApplicationType)` — 工厂按类型创建
- 三选一: **AnnotationConfigServletWebServerApplicationContext**(Servlet: 含嵌入式服务器能力, 启动逻辑见 Tomcat t7) / **AnnotationConfigReactiveWebServerApplicationContext**(Reactive) / **AnnotationConfigApplicationContext**(纯应用)
- `SpringApplication.java:317` — **prepareContext**: 注册主配置源(MyApp.class)、应用 ApplicationContextInitializer、环境注入容器

关键设计: **Why 用 ApplicationContextFactory？** 类型选择集中: 工厂按 WebApplicationType 产对应容器, 且可自定义(setApplicationContextFactory)— 三类应用的差异(是否带 WebServer)封装在容器子类里, run() 不感知差异。[模式: 工厂 — 按类型产容器]

数据流: createApplicationContext(SERVLET) → ApplicationContextFactory.create → AnnotationConfigServletWebServerApplicationContext(实例) → prepareContext: register(MyApp.class 主源) + initializers(SpringBootApplication 上的 ApplicationContextInitializer) → refreshContext: s8 refresh → onRefresh 时创建嵌入式服务器(t7: getWebServer) → 启动完成。

→ 引出 S-5: @ConfigurationProperties — bindToSpringApplication 用到的 Binder/松弛绑定, 就是属性绑定核心 — @ConfigurationProperties 的底层机制。
