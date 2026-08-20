# S-16 诊断 FailureAnalyzer — 启动失败分析机制

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | FailureAnalyzer(36行)+AbstractFailureAnalyzer(66行)+FailureAnalysis(70行)+FailureAnalyzers(110行)+LoggingFailureAnalysisReporter(58行)+analyzer 包(20+ 实现, PortInUse/NoUniqueBeanDefinition 等)+SpringApplication(handleRunFailure)+SpringFactoriesLoader(spring-core)
> 基线: BOOT-PLAN-v2 S-16 — 启动失败友好诊断; 前置: **C-13 异常(请求期解析链 — 本域为启动期, 概念平行, 机制独立)** — 展开启动失败分析机制

---

## §0.8

- 🟡 Working，1篇 — 触发(SpringApplication.handleRunFailure: run 启动失败时 → reportFailure → getExceptionReporters 加载 SpringBootExceptionReporter) → 装配(FailureAnalyzers implements SpringBootExceptionReporter: SpringFactoriesLoader 加载 FailureAnalyzer 实现, ArgumentResolver 注入 BeanFactory/Environment) → 结果模型(FailureAnalyzer 接口 analyze→FailureAnalysis(description/action/cause) + AbstractFailureAnalyzer 泛型 findCause 沿 cause 链) → 具体实现(简单型 PortInUse / 注入型 NoUniqueBeanDefinition extends AbstractInjectionFailureAnalyzer) → 报告(LoggingFailureAnalysisReporter 输出 "APPLICATION FAILED TO START" banner)
- 设计模式: [模式: SPI/工厂]—SpringFactoriesLoader 加载; [模式: 模板方法]—AbstractFailureAnalyzer.findCause; [模式: 策略]—每个 analyzer 管一类异常; [模式: 链式尝试]—首个非 null 分析接管

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SpringApplication.java:799,812,826,836 | 触发 | **handleRunFailure L799**: run 启动失败(创建/refresh 阶段抛异常)→ L812 reportFailure → L826 getExceptionReporters: SpringFactoriesLoader 加载 SpringBootExceptionReporter | High |
| spring.factories:34 | 注册 | **SpringBootExceptionReporter=FailureAnalyzers**(spring.factories 的 SPI 注册) | High |
| FailureAnalyzers.java:46,54,80 | 装配 | **FailureAnalyzers implements SpringBootExceptionReporter**: 构造 L54 → L64 loadFailureAnalyzers: SpringFactoriesLoader.load(FailureAnalyzer.class, ArgumentResolver, FailureHandler.logging); L74 ArgumentResolver 注入 BeanFactory+Environment; L80 reportException → analyze L85 逐个尝试首个非 null | High |
| FailureAnalyzer.java:35 | 接口 | **接口**: analyze(Throwable)→FailureAnalysis(null=不处理) — @FunctionalInterface | High |
| AbstractFailureAnalyzer.java:32,52,57 | 模板 | **findCause L57**: 沿 failure.getCause() 链找匹配类型; getCauseType L52: ResolvableType 解析泛型 T; analyze L32 模板 | High |
| FailureAnalysis.java:41 | 结果模型 | **FailureAnalysis(description/action/cause)**: 诊断描述+建议动作+根因 | High |
| analyzer/PortInUseFailureAnalyzer.java:32 | 简单实现 | **简单型**: 直接 new FailureAnalysis("Port 占用描述","动作建议",cause) | High |
| analyzer/NoUniqueBeanDefinitionFailureAnalyzer.java:35,45 | 注入型实现 | **注入型**: extends AbstractInjectionFailureAnalyzer(构造注入 beanFactory) → 用 getMergedBeanDefinition 枚举候选 Bean 生成详细消息 | High |
| LoggingFailureAnalysisReporter.java:35,44 | 报告 | **banner**: L44 buildMessage → "APPLICATION FAILED TO START" 横幅 + Description/Action 段 | High |
| SpringFactoriesLoader.java:193,223,521 | 加载内核 | **内核**: load(spring-core) 读 META-INF/spring.factories → instantiateFactory → ArgumentResolver.and 复合构造参数 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: FailureAnalyzer 机制体量 ~200 行核心 + 20+ 具体实现(同类) — 知识主线: "启动失败时, SPI 加载 analyzer → 逐类尝试分析 → 输出友好诊断". 1篇 (~44行) 按"触发装配 → 结果模型 → 具体实现与报告"展开; SpringFactoriesLoader 内核(spring-core)引用不重复。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 触发与装配入口 (SpringApplication.handleRunFailure→FailureAnalyzers) | 🔴 | **为什么🔴**: 启动失败诊断怎么被触发/谁加载 |
| P1-2 | 结果模型 (FailureAnalyzer 接口 + AbstractFailureAnalyzer 泛型 findCause + FailureAnalysis) | 🔴 | **为什么🔴**: 分析如何组织/如何沿 cause 链匹配 |
| P1-3 | SpringFactoriesLoader 加载 + ArgumentResolver 构造注入 (BeanFactory/Environment) | 🔴 | **为什么🔴**: analyzer 如何获得上下文 — 与 S-2 .imports 对照 |
| P2-1 | 具体实现: 简单型(PortInUse) vs 注入型(NoUniqueBeanDefinition) | 🟡 | **为什么🟡**: 两种 analyzer 写法 + 各自适用场景 |
| P2-2 | 报告输出 (LoggingFailureAnalysisReporter banner) | 🟡 | **为什么🟡**: 诊断怎么展示给用户 |
| P3-1 | 与 C-13 边界 (启动期 vs 请求期) | 🟢 | **为什么🟢**: 同一"异常处理"主题的两个阶段, 机制独立 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **触发与装配** (handleRunFailure + FailureAnalyzers + SpringFactoriesLoader) | 🔴 | 启动失败时谁/怎么加载 |
| B | **结果模型** (FailureAnalyzer/AbstractFailureAnalyzer/FailureAnalysis) | 🔴 | 分析怎么组织/匹配 |
| C | **实现与报告** (简单/注入型 + banner) | 🟡 | 具体 analyzer 与输出 |

> **Cluster A (§1)**: SpringApplication.handleRunFailure → FailureAnalyzers(SpringFactoriesLoader + ArgumentResolver 构造注入)
> **Cluster B (§2)**: FailureAnalyzer 接口 + AbstractFailureAnalyzer(findCause/getCauseType) + FailureAnalysis
> **Cluster C (§3)**: PortInUseFailureAnalyzer / NoUniqueBeanDefinitionFailureAnalyzer(AbstractInjectionFailureAnalyzer) + LoggingFailureAnalysisReporter banner

→ 引出 S-17: 外部化配置深化 — 启动期另一核心: ConfigData 17 级优先级/application.yml 加载(进入启动运行时层)
