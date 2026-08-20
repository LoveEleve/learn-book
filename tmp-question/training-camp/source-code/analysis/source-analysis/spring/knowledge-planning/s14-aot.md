# S2-14 AOT/Native Image — Ahead-of-Time 编译替代运行时逻辑

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 6文件/~935行
> 基线: S2-2 @Configuration — AOT 替代 CGLIB 代理 + @ComponentScan → GraalVM Native Image

---

## §0.8

- 🟡 Working，1篇 — AOT 编译阶段: BeanRegistrationAotContribution → BeanFactoryInitContribution → RuntimeHints → GraalVM 静态编译 → native binary
- 设计模式: [模式: 策略模式]—AOT Processor 接口允许不同模块自定义编译时代码生成
- AOT 替代了运行时: ①CGLIB代理(编译时生成子类) ②@ComponentScan(编译时扫描→生成注册代码) ③@PropertySource(编译时解析→生成配置加载代码)

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AotDetector.java:54行 | 检测模式 | `AotDetector.useGeneratedArtifacts()` — 系统属性 `spring.aot.enabled=true` → 运行时跳过@Configuration解析直接用编译时生成的初始化器 | High |
| BeanRegistrationAotProcessor.java | 接口 | **processAheadOfTime(RegisteredBean)** — 在编译时处理单个Bean注册 — ConfigurationClassProxyBeanRegistrationCodeFragments替代CGLIB→生成代理类源码 | High |
| BeanFactoryInitializationAotProcessor.java | 接口 | **processAheadOfTime(BeanFactory)** — 编译时生成BeanFactory初始化代码 — PropertySources→PropertySource处理器 / ImportRegistry→ImportAware处理器 | High |
| ApplicationContextAotGenerator.java:76行 | 生成入口 | `processAheadOfTime(beanFactory)` → 收集所有AOT Processor → 生成Java源码文件(JavaPoet) → `AotApplicationContextInitializer` | High |
| RuntimeHints.java:87行 | 反射提示 | **reflection() → type → hint → registerType(反射)/registerMethod(调用)/registerField(访问)** — 告诉GraalVM哪些类在native image中需要保留反射能力 | High |
| ReflectionHints.java:239行 | 注册实现 | `registerType(type)` → 注册类反射 + `registerTypeIfPresent(ClassLoader, className)` → 条件注册 → ResourceHints 注册 `classpath:` 资源 | High |

---

## 02-04 聚合+分类+聚类

### 聚合 — P1 核心 (2) + P2 支持 (1)

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | BeanRegistrationAotProcessor + BeanFactoryInitializationAotProcessor 双接口 — 编译时代替运行时 | 🔴 | **为什么🔴**: AOT核心——两个阶段: ①Bean注册阶段替换运行时CGLIB代理/@ComponentScan/@Bean方法为编译时生成的代码 ②BeanFactory初始化阶段替换@PropertySource/@ImportRegistry为编译时注册 |
| P1-2 | RuntimeHints — GraalVM 反射/资源/序列化提示 | 🔴 | **为什么🔴**: GraalVM native image 要求所有反射调用必须在编译时预先注册——RuntimeHints 是 Spring 与 GraalVM 的接口——@Configuration类需要反射加载、@Bean方法需要反射调用、@Autowired字段需要反射注入——全部通过RuntimeHints预先声明 |
| P2-1 | ApplicationContextAotGenerator 生成流程 + AotDetector 运行时检测 | 🟡 | **为什么🟡**: 编译时生成+运行时检测——是双接口的实现基础设施——AotDetector判断是否"AOT模式"→如果是则跳过@Configuration解析直接load编译时生成的初始化器 |

### 聚类 (1篇)

**1篇理由**: ~935行/6文件 — AOT概念虽广(覆盖全Spring模块)但核心就两个接口+RuntimeHints。1篇(~42行)覆盖双阶段编译+反射提示+运行时检测。

**单篇结构**: §1 双AOT接口(Registration + Init) 编译时代替运行时 → §2 RuntimeHints + ReflectionHints → GraalVM Native Image → §3 运行时: AotDetector.useGeneratedArtifacts 跳过常规解析
