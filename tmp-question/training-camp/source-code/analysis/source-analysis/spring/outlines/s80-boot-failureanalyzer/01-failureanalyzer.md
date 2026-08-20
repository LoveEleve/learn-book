# S-16 诊断 FailureAnalyzer — 启动失败分析机制

> 依赖 C-13 (概念平行, 机制独立) | 🟡 Working | 6 KP | [模式: SPI/工厂 + 模板方法 + 策略 + 链式尝试]

**读者处境**: 启动 Spring Boot 报错 — 控制台打出一大段 "APPLICATION FAILED TO START" 横幅, 告诉你"端口被占、Bean 重复、配置绑不上" — 这份友好诊断是哪来的?裸异常为什么能被翻译成人话?是谁在启动失败时拦截了异常?

### 1. 触发与装配 — 启动失败时谁加载分析器

场景: 端口被占 → SpringApplication.run 抛 PortInUseException → 启动中断前, 先走异常报告流程 — FailureAnalyzers 被加载并逐个调用。

源码路径:
- `SpringApplication.java:799,812,826,836` — **入口**: run 启动失败 → `handleRunFailure`(L799) → L812 `reportFailure(getExceptionReporters(context), exception)` → L826 `getExceptionReporters`: 通过 SpringFactoriesLoader 加载 SpringBootExceptionReporter → L836 reportFailure 遍历 reporter, reportException 返回 true 则记录已报异常并返回
- `spring.factories:34` — **注册**: `SpringBootExceptionReporter=org.springframework.boot.diagnostics.FailureAnalyzers`(SPI 注册表)
- `FailureAnalyzers.java:46,54,64` — **装配**: implements SpringBootExceptionReporter(L46); 构造 L54 → L64 `loadFailureAnalyzers`: `SpringFactoriesLoader.load(FailureAnalyzer.class, ArgumentResolver, FailureHandler.logging)` — 读 spring.factories 全部实现并实例化; L70-76 `getArgumentResolver`: `ArgumentResolver.of(BeanFactory...)` + `.and(Environment...)` — 把上下文注入需要它的 analyzer 构造函数
- `FailureAnalyzers.java:80,85` — **调用**: reportException L80 → analyze L85: 遍历 analyzers, 首个 `analyze(failure)` 非 null 即返回(链式尝试); 单个 analyzer 抛异常被 catch(trace 日志)不影响其他

关键设计: **Why SPI 而非硬编码？** 具体分析器是开放的扩展点(第三方可加)— SpringFactoriesLoader 从 spring.factories 读实现类名并实例化, 不编译期依赖。**Why 首个非 null 接管？** 每个 analyzer 只认自己那类异常(如 PortInUseFailureAnalyzer 只处理 PortInUseException), 返回 null=不归我管, 链继续 — 直到有分析器认领。[模式: SPI/工厂 + 链式尝试]

数据流: run 中端口绑定异常 → handleRunFailure(L799) → L812 reportFailure → getExceptionReporters(L826) 加载 FailureAnalyzers(spring.factories:34) → FailureAnalyzers 构造(L54) 用 SpringFactoriesLoader.load 加载 20+ FailureAnalyzer(带 ArgumentResolver 注入 BeanFactory/Environment) → reportException(L80) → analyze(L85): PortInUseFailureAnalyzer.analyze 命中 → 非 null → 返回分析结果。

### 2. 结果模型 — 分析怎么写, 异常怎么匹配

场景: 一个 analyzer 要"分析某类异常" — 接口怎么定义?怎么沿异常 cause 链找到真正要分析的那个异常?

源码路径:
- `FailureAnalyzer.java:35` — **接口**: `@FunctionalInterface`; `analyze(Throwable failure) → FailureAnalysis`(返回 null = 无法分析)
- `AbstractFailureAnalyzer.java:32,44,52,57` — **模板**: 泛型 `AbstractFailureAnalyzer<T extends Throwable>`; analyze(L32) = `findCause(failure, getCauseType())` → 非 null 才调抽象 `analyze(rootFailure, cause)`(L44); `getCauseType`(L52) 用 `ResolvableType.forClass(AbstractFailureAnalyzer.class, getClass()).resolveGeneric()` 反射读泛型 T; `findCause`(L57) 从 failure 沿 `getCause()` 链逐层深入找 `type.isInstance` 的异常
- `FailureAnalysis.java:41` — **结果**: `new FailureAnalysis(description, action, cause)` — 三段: 描述(发生什么) + 动作(怎么修) + 根因异常

关键设计: **Why 沿 cause 链找？** 真实异常常被包装多层的(如 PortInUseException 被包在 IllegalStateException/BeanCreationException 里) — findCause 一层层剥到目标类型, 才拿得到真正的根因。**Why ResolvableType 读泛型？** 子类只写 `extends AbstractFailureAnalyzer<PortInUseException>` 无需自己声明处理类型 — 泛型参数即契约。[模式: 模板方法 + 反射泛型]

数据流: 抛出的异常被包成 UnsatisfiedDependencyException→...→PortInUseException 链 → PortInUseFailureAnalyzer.analyze(L32) → findCause 沿 getCause() 链匹配到 PortInUseException(命中) → 非 null → 调子类 analyze(rootFailure, cause) → new FailureAnalysis(描述, 动作, cause)。

### 3. 具体实现与报告 — 简单型 vs 注入型 + banner 输出

场景: 有的诊断一句话(端口占用), 有的要翻容器找候选 Bean(NoUniqueBeanDefinition) — 两种 analyzer 怎么写?分析结果怎么展示?

源码路径:
- `analyzer/PortInUseFailureAnalyzer.java:32` — **简单型**: extends AbstractFailureAnalyzer<PortInUseException> → analyze 直接 `new FailureAnalysis("Web server failed to start. Port ... was already in use.", "Identify and stop the process...", cause)` — 纯字符串, 无需上下文
- `analyzer/NoUniqueBeanDefinitionFailureAnalyzer.java:35,39,45` — **注入型**: extends `AbstractInjectionFailureAnalyzer`(L36); 构造参数声明 `BeanFactory beanFactory`(L39) — 由 §1 ArgumentResolver 按**精确类型** BeanFactory.class 命中注入 → 构造体内 `Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory)`(L40) 校验并收窄成 ConfigurableBeanFactory; analyze(L45) 用 `beanFactory.getMergedBeanDefinition(beanName)`(L67) 枚举每个候选 Bean 的来源("defined by method ... in ...")拼成详细列表, 再附 MissingParameterNamesFailureAnalyzer.ACTION 建议
- `LoggingFailureAnalysisReporter.java:35,44` — **报告**: report(L35) → buildMessage(L44): `String.format` 拼 `***************************\nAPPLICATION FAILED TO START\n***************************`(L46-49) + `Description:`(L50, description) + 有 action 才加 `Action:`(L52-54) — 日志 error 级输出

关键设计: **Why 注入型要 BeanFactory？** 简单型只描述现象; 注入型要"列候选 Bean 及定义来源"帮用户定位(哪个配置类注册的、工厂方法名)— 需访问容器, 故通过构造函数注入(§1 ArgumentResolver 提供)。**Why 参数声明 BeanFactory 而非 ConfigurableBeanFactory？** ArgumentResolver 用**精确类型匹配**(`candidateType.equals(type)`), 只认构造声明的确切类型 — 故构造参数声明为 BeanFactory.class 才能被命中; 需要更窄的 ConfigurableBeanFactory 就在构造体内 Assert+cast 收窄。**Why Action 可有可无？** 报告器用 `StringUtils.hasText` 判断 action — 某分析没给 action(为 null/空)时 banner 只打 Description; PortInUse 这种给了 action("Identify and stop the process...")的才追加 Action 段。[模式: 策略分层(简单/注入) + 格式化输出]

数据流: NoUniqueBeanDefinitionException → NoUniqueBeanDefinitionFailureAnalyzer(beanFactory 由 ArgumentResolver 注入) → analyze → extractBeanNames + getMergedBeanDefinition 逐个列候选 → 拼消息 → FailureAnalysis → FailureAnalyzers.report(L100) 调 LoggingFailureAnalysisReporter.report → buildMessage → error 日志: APPLICATION FAILED TO START 横幅 + Description + Action。

→ 引出 S-17: 外部化配置深化 — 启动期的另一核心: ConfigDataEnvironmentPostProcessor 的 17 级优先级/application.yml 加载(进入启动运行时层)。
