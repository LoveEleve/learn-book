# S-18 日志 — LoggingSystem 抽象 + LogbackLoggingSystem (日志系统选择与加载)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | LoggingSystem(211行)+LoggingSystemFactory(45行)+DelegatingLoggingSystemFactory(60行)+AbstractLoggingSystem(244行)+LogbackLoggingSystem(520行)+SpringBootJoranConfigurator(280行)+LoggingApplicationListener(440行, context/logging)
> 基线: BOOT-PLAN-v2 S-18 — 日志系统抽象; 前置: **无(独立域)** — 展开日志系统选择与加载

---

## §0.8

- 🟡 Working，1篇 — 抽象与选择(LoggingSystem 抽象: beforeInitialize/initialize/setLogLevel 模板 + LoggingSystemFactory SPI: DelegatingLoggingSystemFactory 逐个探测 classpath, Logback 默认胜出) → 触发与生命周期(LoggingApplicationListener: ApplicationStartingEvent→beforeInitialize(静默+JUL bridge), ApplicationEnvironmentPreparedEvent→initialize) → 配置加载(AbstractLoggingSystem 约定: logging.config 或 conventions 找 logback-spring.xml; LogbackLoggingSystem→SpringBootJoranConfigurator 支持 <springProfile>/<springProperty>)
- 设计模式: [模式: SPI/工厂]—LoggingSystemFactory; [模式: 模板方法]—AbstractLoggingSystem; [模式: 监听器+事件]—触发时序

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| LoggingSystem.java:40,159,167 | 抽象+入口 | **get(classLoader) L159**: 优先系统属性, 否则 SYSTEM_FACTORY.getLoggingSystem(L167); NONE=L51 | High |
| LoggingSystemFactory.java:28,36,42 | SPI | **接口 L28**: getLoggingSystem(classLoader); fromSpringFactories L42 | High |
| spring.factories:2-5 | 注册 | **LoggingSystemFactory=Java/Log4J2/Logback** 三个 Factory(文件顺序); **@Order 定优先级**: Logback HIGHEST+1024 / Log4J2 0 / Java LOWEST-1024 | High |
| DelegatingLoggingSystemFactory.java:40,44 | 选择 | **getLoggingSystem L40**: 遍历按 @Order 排序的 delegates, 首个非 null 返回(L44) — Logback 最先 | High |
| LogbackLoggingSystem.java:496,498 | Logback 探测 | **Factory L496**: PRESENT=ClassUtils.isPresent("ch.qos.logback.classic.LoggerContext")(L498) → 在则返回 LogbackLoggingSystem | High |
| LoggingApplicationListener.java:236,238,241,324 | 触发 | **onApplicationStartingEvent(L236)→beforeInitialize(L238)**; onApplicationEnvironmentPreparedEvent(L241)→initializeSystem(L324): logging.config(L325) 或约定 | High |
| AbstractLoggingSystem.java:56,61,129 | 配置约定 | **initialize(L56)**: 有 configLocation→具体, 否则 conventions(L61)→getSpringConfigLocations(L129) 加 -spring 变体 | High |
| LogbackLoggingSystem.java:123,128,188,217 | Logback 实现 | **getStandardConfigLocations(L123)**: logback-test.groovy/xml, logback.groovy, logback.xml; beforeInitialize(L128); initialize(L188)→SpringBootJoranConfigurator(L217) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 日志系统是独立主题, 核心 3 块(抽象选择/触发生命周期/配置加载) — 1篇 (~44行) 按"抽象与选择 → 触发与生命周期 → 配置加载与 logback 扩展"展开; 无下层可复用(独立域)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 抽象与选择 (LoggingSystem + LoggingSystemFactory SPI + **@Order 排序** + classpath 探测 → Logback) | 🔴 | **为什么🔴**: 日志系统统一抽象与自动选择机制(delegate 顺序靠 @Order 而非文件顺序) |
| P1-2 | 触发与生命周期 (LoggingApplicationListener 事件时序) | 🔴 | **为什么🔴**: beforeInitialize/initialize 的启动时机 |
| P1-3 | 配置加载约定 (logback-spring.xml + conventions) | 🔴 | **为什么🔴**: 配置文件怎么被找到/加载 |
| P2-1 | beforeInitialize 静默 + JUL bridge | 🟡 | **为什么🟡**: 初始化前为何静默/桥接 JDK 日志 |
| P2-2 | logback 扩展 (<springProfile>/<springProperty>) | 🟡 | **为什么🟡**: Boot 对 logback 的增强 |
| P3-1 | 与 S-17 边界 (日志独立于 configdata, 启动更早) | 🟢 | **为什么🟢**: 启动阶段顺序 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **抽象与选择** (LoggingSystem + Factory SPI) | 🔴 | 日志系统怎么抽象/选哪个 |
| B | **触发与生命周期** (LoggingApplicationListener) | 🔴 | 何时初始化 |
| C | **配置加载** (conventions + logback 扩展) | 🟡 | 配置怎么来 |

> **Cluster A (§1)**: LoggingSystem 抽象 + LoggingSystemFactory(Delegating+classpath 探测)+ LogbackLoggingSystem.Factory
> **Cluster B (§2)**: LoggingApplicationListener 事件时序(beforeInitialize/initialize)
> **Cluster C (§3)**: AbstractLoggingSystem conventions + logback-spring.xml + SpringBootJoranConfigurator 扩展

→ 引出 S-19: 启动运行时 — 日志之外: 可用性(ApplicationAvailability/Liveness/Readiness)+ApplicationReadyEvent 拆2篇(进入启动运行时层收束)
