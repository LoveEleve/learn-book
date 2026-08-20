# S-18 日志 — LoggingSystem 抽象 + LogbackLoggingSystem (日志系统选择与加载)

> 依赖 无 (独立域) | 🟡 Working | 6 KP | [模式: SPI/工厂 + 模板方法 + 监听器/事件]

**读者处境**: 启动时控制台第一行日志就打出来 — 谁选的 Logback?`logback-spring.xml` 和 `logback.xml` 区别?`<springProfile>` 为什么能按 profile 生效?为什么启动早期日志被"压住"直到配置加载完?

### 1. 抽象与选择 — LoggingSystem 是谁, 怎么选中 Logback

场景: 一个 Boot 应用可能用 Logback/Log4J2/JUL — 代码不硬编码, 启动时怎么自动挑一个?

源码路径:
- `LoggingSystem.java:40,159,167` — **抽象**: `public abstract class LoggingSystem`(L40) 定义 beforeInitialize/initialize/setLogLevel 等模板; 静态 `get(ClassLoader)`(L159): 先看系统属性 `-Dorg.springframework.boot.logging.LoggingSystem`(L160), 否则 L167 `SYSTEM_FACTORY.getLoggingSystem(classLoader)`
- `LoggingSystemFactory.java:28,42` — **SPI**: 接口 getLoggingSystem(classLoader)(L36); `fromSpringFactories()`(L42) 从 spring.factories 加载实现
- `spring.factories:2-5` + **@Order** — **注册与优先级**: 文件列出 Java/Log4J2/Logback; 但 SpringFactoriesLoader 加载后按 `@Order` 排序(值小者先): `LogbackLoggingSystem.java:495` `@Order(HIGHEST_PRECEDENCE+1024)` → 最先探测; `log4j2/Log4J2LoggingSystem.java:531` `@Order(0)` → 次之; `java/JavaLoggingSystem.java:179` `@Order(LOWEST_PRECEDENCE-1024)` → 最后兜底
- `DelegatingLoggingSystemFactory.java:40,44` — **选择**: getLoggingSystem(L40) 遍历 delegates(按 @Order 排序后为 [Logback, Log4J2, Java]), **首个非 null 返回**(L44); `LogbackLoggingSystem.java:496,498` — Logback 的 Factory(L496) 用 `ClassUtils.isPresent("ch.qos.logback.classic.LoggerContext")`(L498) 探测 — 有 Logback 类就返回

关键设计: **Why SPI + classpath 探测？** 日志实现不该硬编码 — 用 spring.factories 声明候选, 每个 Factory 探测自己库是否在 classpath(PRESENT), 返回非 null 即选中。**Why Logback 恒胜出(而非 Java)？** delegate 顺序由 `@Order` 排序(值小者先) — Logback @Order(HIGHEST+1024) 最优先探测, 默认 starter 带 Logback → 立即返回; Java @Order(LOWEST-1024) 是最后兜底(恒在, 因 LogManager 必存在); 换 Log4J2 就是排除 logback(其 Factory PRESENT=false 返回 null)→ 落到 @Order(0) 的 Log4J2。[模式: SPI/工厂 + @Order 排序 + classpath 探测]

数据流: LoggingSystem.get(classLoader)(L159) → 无系统属性 → SYSTEM_FACTORY.getLoggingSystem(L167) → DelegatingLoggingSystemFactory(L40) 遍历按 @Order 排序的 [Logback, Log4J2, Java] → Logback Factory(@Order HIGHEST+1024) 探测 ch.qos.logback 在 classpath → 非 null → new LogbackLoggingSystem(classLoader)。

### 2. 触发与生命周期 — 谁在启动时初始化日志

场景: 日志初始化在两个时间点发生 — 一个在应用最早期, 一个在环境就绪后 — 为什么分两步?

源码路径:
- `LoggingApplicationListener.java:236,238` — **早期**: 监听 `ApplicationStartingEvent`(L236) → `loggingSystem.beforeInitialize()`(L238) — 启动最早, 先让日志"能打但不输出"
- `LoggingApplicationListener.java:241,324,325` — **就绪后**: 监听 `ApplicationEnvironmentPreparedEvent`(L241) → `initializeSystem`(L324): 读 `logging.config` 属性(L325) → 传给 system.initialize
- `LogbackLoggingSystem.java:128,188` — **实现**: beforeInitialize(L128): 装 JUL→SLF4J bridge + 加 `SUPPRESS_ALL_FILTER`(静默); initialize(L188): 加载配置后移除静默过滤器

关键设计: **Why 分两阶段？** 启动最早期(ApplicationStartingEvent)环境/配置文件还没就绪 — 此时调 beforeInitialize 让日志"预热"(装 bridge), 并加 SUPPRESS_ALL_FILTER 压住输出; 等 Environment 就绪(ApplicationEnvironmentPreparedEvent)再真正 loadConfiguration, 完成后移除过滤器 — 避免"配置加载前就刷屏"。**Why 监听器？** 与 S-17 的 EnvironmentPostProcessor 同类 — 用 SpringApplication 事件在正确时机介入。[模式: 监听器/事件 + 两阶段初始化]

数据流: ApplicationStartingEvent(L236) → beforeInitialize(L238, Logback: 装 JUL bridge + SUPPRESS_ALL_FILTER) → ... → ApplicationEnvironmentPreparedEvent(L241) → initializeSystem(L324) → 读 logging.config(L325) → LogbackLoggingSystem.initialize(L188) → 加载配置 → 移除 SUPPRESS_ALL_FILTER → 日志正常输出。

### 3. 配置加载 — logback-spring.xml 约定 + Boot 扩展

场景: 没写 `logging.config` 时, Boot 从哪找配置文件?`logback-spring.xml` 比 `logback.xml` 强在哪?`<springProfile>` 怎么工作?

源码路径:
- `AbstractLoggingSystem.java:56,61,129` — **约定**: initialize(L56): 有 configLocation→加载指定; 否则 `initializeWithConventions`(L61): 找自初始化配置(logback.xml 等)→ 没有再找 `getSpringConfigLocations`(L129, 把 logback.xml 变成 logback-spring.xml)
- `LogbackLoggingSystem.java:123` — **标准位置**: `getStandardConfigLocations`: logback-test.groovy/xml, logback.groovy, logback.xml
- `LogbackLoggingSystem.java:217` — **加载器**: `SpringBootJoranConfigurator` — 解析 logback 配置, 支持 Boot 扩展 `<springProfile>`(按激活 profile 生效)/`<springProperty>`(读 Environment 属性到 logback 变量)
- `LoggingApplicationListener.java:325` — **显式**: `logging.config` 属性可指定任意配置文件(覆盖约定)

关键设计: **Why -spring.xml 变体？** logback.xml 会被 logback 自己加载(早于 Boot), 无法用 `<springProfile>`/`<springProperty>`; 命名成 logback-spring.xml 后由 Boot 的 SpringBootJoranConfigurator 加载, 才能解释这些 Spring 感知的标签 — 这是"让配置文件感知 Spring 环境"的关键。**Why 约定优先于默认？** conventions 找到文件就用, 找不到 loadDefaults(内建默认) — 覆盖"无配置也能跑"。[模式: 模板方法 + 命名约定 + 定制解析器]

数据流: initialize(L56) → logging.config 为空 → initializeWithConventions(L61) → getSelfInitializationConfig(logback.xml 存在?)→ 没有 → getSpringInitializationConfig(logback-spring.xml 存在)→ 找到 → LogbackLoggingSystem.loadConfiguration → SpringBootJoranConfigurator(L217) 解析(处理 <springProfile>/<springProperty>)→ 应用。

→ 引出 S-19: 启动运行时 — 日志之外: 可用性(ApplicationAvailability/LivenessState/ReadinessState)+ ApplicationReadyEvent 拆 2 篇(进入启动运行时层收束)。
