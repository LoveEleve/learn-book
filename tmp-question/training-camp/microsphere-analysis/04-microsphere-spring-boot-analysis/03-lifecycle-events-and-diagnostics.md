# 04-03 生命周期事件与启动诊断

## 问题：Spring Boot 启动过程的三个痛点

Spring Boot 的启动由 `SpringApplication.run()` 驱动，经历 7 个生命周期阶段（`starting` -> `environmentPrepared` -> `contextPrepared` -> `contextLoaded` -> `started` -> `ready` -> `failed`），通过 `SpringApplicationRunListener` 和 `ApplicationEvent` 两个机制暴露给开发者。

三个痛点：

| 痛点 | Spring Boot 的现状 | 问题 |
|------|------------------|------|
| **父子容器重复触发** | `ApplicationPreparedEvent` 在父子容器中都会触发 | Listener 被调用两次，需要手动判断 context ID |
| **Jar 冲突检测** | 启动报 `NoSuchMethodError`/`ClassNotFoundException` 时只能靠经验排查 | 没有自动检测机制，错误信息不直观 |
| **条件评估结果不可见** | `ConditionEvaluationReport` 存在但不默认输出 | "为什么这个自动装配没生效"需要 debug 模式排查 |

microsphere-spring-boot 通过**一次性事件监听器 + Jar 冲突检测链 + 条件评估报告**三层设计解决这些痛点。

---

## 设计：三个独立子系统

### 整体架构

```
SpringApplication.run() 生命周期
    │
    │  ─── starting() ───
    │
    ├── BannedArtifactClassLoadingListener (SpringApplicationRunListener, HIGHEST_PRECEDENCE)
    │       └── 系统属性 microsphere.spring.boot.banned-artifacts.enabled=true 时
    │           调用 BannedArtifactClassLoadingExecutor 拦截冲突类
    │
    │  ─── contextInitialized() ───
    │
    ├── ArtifactsCollisionDiagnosisListener (ApplicationListener<ApplicationContextInitializedEvent>)
    │       └── 属性 microsphere.spring.boot.artifacts-collision.enabled=true 时
    │           调用 ArtifactDetector 检测 Maven artifact 版本冲突（同 groupId:artifactId 不同版本）
    │           └── 发现冲突 -> 抛出 ArtifactsCollisionException
    │                   └── ArtifactsCollisionFailureAnalyzer 生成 mvn dependency:tree 命令
    │
    │  ─── contextPrepared() -> ApplicationPreparedEvent ───
    │
    ├── OnceApplicationPreparedEventListener (防重复)
    │       └── OnceMainApplicationPreparedEventListener (防重复 + 过滤 bootstrap context)
    │               └── LoggingOnceMainApplicationPreparedEventListener (日志)
    │
    │  ─── ready() -> ApplicationReadyEvent ───
    │
    ├── ConditionEvaluationReportListener (ApplicationListener<ApplicationReadyEvent>)
    │       └── ConditionsReportMessageBuilder 输出条件评估报告
    │
    │  ─── failed() ───
    │
    └── ConditionEvaluationSpringBootExceptionReporter (SpringBootExceptionReporter)
            └── 启动失败时输出条件评估报告（error 级别）
```

---

### OnceApplicationPreparedEventListener：防止父子容器重复

#### 问题

Spring Cloud 的 `BootstrapApplicationListener` 会创建一个 bootstrap `ApplicationContext`（父容器），然后创建主 `ApplicationContext`（子容器）。`ApplicationPreparedEvent` 在两个容器中都会触发，导致 Listener 被调用两次。

#### 设计

```java
public abstract class OnceApplicationPreparedEventListener
        implements ApplicationListener<ApplicationPreparedEvent>, Ordered {

    // 每个 Listener 子类维护一个已处理 context ID 集合
    private static final Map<Class<? extends ApplicationListener>, Set<String>>
        listenerProcessedContextIds = newConcurrentHashMap();

    private final Set<String> processedContextIds;

    @Override
    public final void onApplicationEvent(ApplicationPreparedEvent event) {
        String contextId = event.getApplicationContext().getId();
        if (isProcessed(contextId)) return;     // 已处理，跳过
        if (isIgnored(springApplication, args, context)) {
            markProcessed(contextId);            // 标记为已处理（即使被忽略）
            return;
        }
        markProcessed(contextId);
        onApplicationEvent(springApplication, args, context);  // 委托子类
    }

    protected abstract void onApplicationEvent(
        SpringApplication springApplication, String[] args,
        ConfigurableApplicationContext context);

    protected boolean isIgnored(...) { return false; }  // 默认不忽略
}
```

**关键设计**：

- `onApplicationEvent` 标记为 `final`，子类只能覆盖 `onApplicationEvent(SpringApplication, ...)` 和 `isIgnored(...)`
- `processedContextIds` 是 `ConcurrentSkipListSet`，线程安全
- 按 **Listener 子类** 维护已处理集合（`Class<? extends ApplicationListener>` 为 key），不同 Listener 互不影响
- 被忽略的 context 也标记为已处理，防止后续重复判断

#### OnceMainApplicationPreparedEventListener：过滤 bootstrap context

```java
public abstract class OnceMainApplicationPreparedEventListener
        extends OnceApplicationPreparedEventListener {

    protected boolean isIgnored(ConfigurableApplicationContext context) {
        if (isBootstrapApplicationListenerPresent(context)) {
            return isBootstrapContext(context) || !isMainApplicationContext(context);
        }
        return false;  // 无 Spring Cloud 时，不过滤
    }
}
```

**判断逻辑**：
1. 检测 `BootstrapApplicationListener` 是否在 classpath（Spring Cloud 存在）
2. 如果存在：忽略 bootstrap context（ID 为 "bootstrap"）和非主 context
3. 如果不存在：不忽略任何 context（退化为 `OnceApplicationPreparedEventListener`）

**bootstrap context 识别**：通过 `spring.cloud.bootstrap.name` 属性或 context ID 是否为 `"bootstrap"` 判断。

#### Logging 实现

`LoggingOnceApplicationPreparedEventListener` 和 `LoggingOnceMainApplicationPreparedEventListener` 是日志实现：

- `isIgnored` 检查日志级别--如果为默认级别（禁用日志），返回 `true` 跳过
- `onApplicationEvent` 调用 `SpringApplicationUtils.log()` 输出启动信息

通过测试 `spring.factories` 注册：
```properties
org.springframework.context.ApplicationListener=\
io.microsphere.spring.boot.context.LoggingOnceApplicationPreparedEventListener,\
io.microsphere.spring.boot.context.LoggingOnceMainApplicationPreparedEventListener
```

---

### BannedArtifactClassLoadingListener：最早阶段的类加载拦截

#### 设计

`BannedArtifactClassLoadingListener` 继承 `SpringApplicationRunListenerAdapter`（已废弃），在 `starting()` 阶段--Spring Boot 生命周期的**第一个回调**--执行类加载拦截：

```java
public class BannedArtifactClassLoadingListener
        extends SpringApplicationRunListenerAdapter implements Ordered {

    // 系统属性（非应用属性），因为 starting() 阶段 Environment 尚未准备好
    public static final String BANNED_ARTIFACTS_ENABLED_PROPERTY_NAME =
        "microsphere.spring.boot.banned-artifacts.enabled";

    public BannedArtifactClassLoadingListener(SpringApplication springApplication, String... args) {
        super(springApplication, args);
        setOrder(HIGHEST_PRECEDENCE);
    }

    @Override
    public void starting() {
        if (isProcessed()) return;
        markProcessed();
        if (getBoolean(BANNED_ARTIFACTS_ENABLED_PROPERTY_NAME)) {
            BannedArtifactClassLoadingExecutor executor = new BannedArtifactClassLoadingExecutor();
            executor.execute(currentThread().getContextClassLoader());
        }
    }
}
```

**关键设计**：

- 使用**系统属性**（`System.getProperty`），不是应用属性（`environment.getProperty`）。因为 `starting()` 阶段 Environment 尚未准备好，只能读系统属性
- `HIGHEST_PRECEDENCE` 确保在所有 RunListener 中最先执行
- 使用 `ConcurrentMap<SpringApplication, Boolean>` 防止同一 SpringApplication 重复处理
- 实际拦截逻辑委托给 microsphere-java 的 `BannedArtifactClassLoadingExecutor`（02-microsphere-java 第 2 篇"禁止类加载"机制）

**与 `SpringApplicationRunListenerAdapter` 的关系**：`SpringApplicationRunListenerAdapter` 标注了 `@Deprecated(since = "0.2.9", forRemoval = true)`，但 `BannedArtifactClassLoadingListener` 继承它且自身**未废弃**。这是设计不一致--`BannedArtifactClassLoadingListener` 应该直接实现 `SpringApplicationRunListener`，而非继承废弃类。

**同类问题**：`FailureReportSpringApplicationRunListener` 同样继承 `SpringApplicationRunListenerAdapter` 且标注 `@Deprecated`，但**仍在主 `spring.factories` 中注册**。这意味着 Spring Boot 仍会实例化它，启动失败时仍会向 `System.err` 打印异常堆栈。用户无法通过属性禁用它，只能通过 `spring.factories` 覆盖或从 classpath 移除 microsphere-spring-boot。

---

### ArtifactsCollision 诊断链：Jar 冲突检测与修复建议

#### 三组件协作

```
ApplicationContextInitializedEvent
    │
    ├── ArtifactsCollisionDiagnosisListener (检测)
    │       ├── ArtifactDetector.detect() -> 检测 ClassPath 中的重复类
    │       └── 发现冲突 -> 抛出 ArtifactsCollisionException
    │               ├── 包含冲突 artifact 集合 Set<String>
    │               └── 消息列出所有冲突
    │
    └── ArtifactsCollisionFailureAnalyzer (分析)
            ├── 继承 AbstractFailureAnalyzer<ArtifactsCollisionException>
            ├── 生成 mvn dependency:tree -Dincludes=... 命令
            └── 建议在 pom.xml 中排除冲突 artifact
```

**触发时机**：`ApplicationContextInitializedEvent`--在 `ApplicationContext` 初始化后、Bean 定义注册前。此时 ClassPath 已完全加载，可以检测所有 Maven artifact。

**检测机制**：`ArtifactDetector.detect(false)` 遍历 ClassPath 中所有 Jar，解析 `META-INF/maven/groupId/artifactId/pom.properties` 获取 Maven 坐标，按 `groupId:artifactId` 分组。如果同一坐标出现多个版本，标记为冲突。冲突详情在 `logger.error()` 中输出，然后抛出 `ArtifactsCollisionException`。

注意：`ArtifactDetector` 检测的是 **Maven artifact 版本冲突**（同 `groupId:artifactId` 不同版本），不是类级别的重复（同 class name 不同 Jar）。前者通过 Maven 元数据检测，后者需要扫描 Jar 内的所有 `.class` 文件。microsphere-java 的 `BannedArtifactClassLoadingExecutor`（02-microsphere-java 第 2 篇）负责后者--在 ClassLoader 层面拦截冲突 Jar 的类加载。

**默认禁用**：`microsphere.spring.boot.artifacts-collision.enabled=false`。因为 `ArtifactDetector` 需要遍历 ClassPath 中所有 Jar 的所有类，启动时开销可能不可忽视（数百 Jar 时约 1-3 秒）。

**FailureAnalyzer 的修复建议**：

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Artifacts conflict. The list is as follows:[com.google.guava:guava:29.0-jre, com.google.guava:guava:30.1-jre]

Action:

Analyze conflict Artifacts by running the following Maven command in the root directory of the project source code:
mvn dependency:tree -Dincludes=com.google.guava:guava:29.0-jre,com.google.guava:guava:30.1-jre,
After analyzing the results, exclude them in the pom.xml file one by one!
```

`-Dincludes=` 参数直接列出冲突的 Maven 坐标，开发者复制粘贴即可运行。这是"可操作性错误信息"的设计--不仅告诉用户"出错了"，还告诉用户"怎么修"。

---

### ConditionEvaluationReport 系统：条件评估报告

#### 四组件协作

```
SpringApplication 启动流程
    │
    ├── ConditionEvaluationReportInitializer (ApplicationContextInitializer)
    │       └── 注册 BeanFactoryPostProcessor: ConditionEvaluationReportBuilder::build
    │           └── 在 BeanFactoryPostProcessor 阶段获取 ConditionEvaluationReport
    │               并缓存到 ConcurrentMap<BeanFactory, Report>
    │
    ├── ApplicationReadyEvent (启动成功)
    │       └── ConditionEvaluationReportListener
    │               └── ConditionsReportMessageBuilder.build()
    │                   └── 按 base-packages 过滤，输出匹配条件和不匹配条件
    │                   └── logger.info() 输出
    │
    └── failed() (启动失败)
            └── ConditionEvaluationSpringBootExceptionReporter
                    └── ConditionsReportMessageBuilder.build()
                    └── logger.error() 输出（error 级别）
```

#### ConditionEvaluationReportBuilder：缓存机制

```java
abstract class ConditionEvaluationReportBuilder {
    private static final Map<ConfigurableListableBeanFactory, ConditionEvaluationReport>
        reports = newConcurrentHashMap();

    static ConditionEvaluationReport build(ConfigurableListableBeanFactory beanFactory) {
        return reports.computeIfAbsent(beanFactory, ConditionEvaluationReport::get);
    }

    static Map<String, ConditionEvaluationReport> getReportsMap() {
        // 将 BeanFactory 映射为 ID（serializationId 或 identityToString）
    }
}
```

以 `BeanFactory` 实例为 key 缓存 `ConditionEvaluationReport`，避免重复获取。`getReportsMap` 将 key 转换为字符串 ID（`DefaultListableBeanFactory` 的 `serializationId` 或 `identityToString`），供消息构建器使用。

#### ConditionsReportMessageBuilder：过滤与格式化

```java
public class ConditionsReportMessageBuilder {
    // 默认只报告 microsphere 包下的条件
    public static final String DEFAULT_BASE_PACKAGE = "io.microsphere";

    // 可通过属性配置
    public static final String BASE_PACKAGES_PROPERTY_NAME =
        "microsphere.spring.boot.conditions.report.base-packages";

    List<String> build() {
        Map<String, ConditionEvaluationReport> reportsMap = getReportsMap();
        // 对每个 report，按 base-packages 过滤条件
        // 只输出包名匹配的条件评估结果
    }
}
```

**过滤机制**：默认只输出 `io.microsphere` 包下的条件评估结果。这意味着报告中只显示 microsphere 自动装配类的条件匹配/不匹配信息，不显示 Spring Boot 原生自动装配的信息。用户可以通过 `microsphere.spring.boot.conditions.report.base-packages` 属性扩展过滤范围。

**输出格式**：每个 context 一条消息，包含匹配条件（matched conditions）和不匹配条件（unmatched conditions）。

---

## 永恒原理

### 1. 事件去重与 Context 身份识别

`OnceApplicationPreparedEventListener` 通过 context ID 去重，本质是"**基于身份的事件去重**"。在父子容器场景中，同一事件类型会被多个 context 触发，但每个 context 的处理逻辑应该只执行一次。

这与 03-spring 第 1 篇的 `OnceApplicationContextEventListener`（通过 `event.getSource()` 判断是否为原始 context）设计理念一致，但实现方式不同：
- 03-spring 的方式：检查事件源是否为当前 context（被动过滤）
- microsphere-boot 的方式：记录已处理的 context ID（主动去重）

两种方式各有优劣：被动过滤不需要维护状态，但如果多个 context 的事件源相同则无法区分；主动去重可以精确控制，但需要维护状态集合。

### 2. 系统属性 vs 应用属性的时序约束

`BannedArtifactClassLoadingListener` 使用系统属性（`System.getProperty`），而 `ArtifactsCollisionDiagnosisListener` 使用应用属性（`environment.getProperty`）。差异在于**触发时机**：

| 组件 | 触发阶段 | Environment 是否就绪 | 属性来源 |
|------|---------|-------------------|---------|
| BannedArtifactClassLoadingListener | `starting()` | ❌ 未就绪 | 系统属性（`-D` 参数） |
| ArtifactsCollisionDiagnosisListener | `contextInitialized()` | ✅ 已就绪 | 应用属性（`application.properties`） |

这是 Spring Boot 生命周期的重要约束：`starting()` 阶段只能用系统属性，`environmentPrepared()` 之后才能用应用属性。框架开发者必须根据触发时机选择正确的属性源。

### 3. FailureAnalyzer 的可操作性错误信息

Spring Boot 的 `FailureAnalyzer` 机制允许在启动失败时输出格式化的诊断信息。microsphere 的 `ArtifactsCollisionFailureAnalyzer` 不仅描述错误（"哪些 artifact 冲突"），还提供**可操作的修复步骤**（"运行 mvn dependency:tree -Dincludes=..."）。

这是"可操作性错误信息"的设计原则：错误信息应该包含三个要素--**What**（什么错了）、**Why**（为什么错）、**How**（怎么修）。大多数框架只做到了 What 和 Why，microsphere 补充了 How。

### 4. 条件评估报告的按需过滤

`ConditionsReportMessageBuilder` 默认只输出 `io.microsphere` 包的条件。这是"**信噪比优化**"的设计--Spring Boot 的 `ConditionEvaluationReport` 可能包含数百个条件，全量输出会淹没用户关心的信息。通过 base-packages 过滤，只显示 microsphere 相关的条件，提高可读性。

---

## 边界与反例

### 1. BannedArtifactClassLoadingListener 继承废弃类

`BannedArtifactClassLoadingListener` 继承 `SpringApplicationRunListenerAdapter`（`@Deprecated since 0.2.9`），但自身未废弃。如果未来版本移除 `SpringApplicationRunListenerAdapter`，`BannedArtifactClassLoadingListener` 会编译失败。

**缓解**：应重构为直接实现 `SpringApplicationRunListener`，不依赖废弃基类。

### 2. ArtifactsCollisionDiagnosisListener 的性能开销

`ArtifactDetector` 需要遍历 ClassPath 中所有 Jar 的所有类文件，检测重复类。在大型项目（500+ Jar）中，这可能耗时数秒。

**缓解**：默认禁用，仅在疑似 Jar 冲突时启用。启用后检测完毕即可关闭。

### 3. OnceApplicationPreparedEventListener 的内存泄漏

`listenerProcessedContextIds` 是 `static` 字段，以 `Class` 为 key 存储 `Set<String>`。在多次创建和销毁 ApplicationContext 的场景（如集成测试），已处理的 context ID 集合会持续增长，不会被清理。

**缓解**：`ConcurrentSkipListSet` 的内存开销较小（每个 context ID 约几十字节），在正常应用中影响可忽略。在长时间运行的测试套件中，可以在 `@AfterAll` 中手动清理。

### 4. ConditionEvaluationReport 的 BeanFactory 引用

`ConditionEvaluationReportBuilder` 的 `reports` Map 以 `BeanFactory` 实例为 key。如果 ApplicationContext 被关闭但 BeanFactory 未被 GC（如被其他对象引用），`reports` Map 会持有 BeanFactory 引用，阻止 GC。

**缓解**：`ConcurrentHashMap` 的 key 是弱引用？不，`ConcurrentHashMap` 使用强引用 key。这是一个潜在的内存泄漏点。可以考虑使用 `WeakHashMap` 或在 context 关闭时手动移除。

### 5. ConditionsReportMessageBuilder 的 base-packages 默认值

默认只输出 `io.microsphere` 包的条件。如果用户不知道这个默认值，可能看不到自己自定义自动装配类的条件评估结果。

**缓解**：设置 `microsphere.spring.boot.conditions.report.base-packages=com.example,io.microsphere` 扩展过滤范围。

---

## 现代 Spring Boot（3.x）是否已支持？

| microsphere 特性 | Spring Boot 3.x 是否有等价物 | 说明 |
|------------------|------------------------|------|
| `OnceApplicationPreparedEventListener` | 无 | Spring Boot 3.x 无事件去重机制 |
| `OnceMainApplicationPreparedEventListener` | 无 | Spring Boot 3.x 无 bootstrap context 过滤 |
| `BannedArtifactClassLoadingListener` | 无 | Spring Boot 3.x 无类加载拦截 |
| `ArtifactsCollisionDiagnosisListener` | 无 | Spring Boot 3.x 无 Jar 冲突自动检测 |
| `ArtifactsCollisionFailureAnalyzer` | 无 | Spring Boot 3.x 的 FailureAnalyzer 不提供 mvn 命令建议 |
| `ConditionEvaluationReportBuilder` 缓存 | 部分 | Spring Boot 3.x 有 `ConditionEvaluationReport`，但不缓存 |
| `ConditionEvaluationReportListener` 自动输出 | 无 | Spring Boot 3.x 默认不输出条件评估报告（需 `--debug`） |
| `ConditionEvaluationSpringBootExceptionReporter` | 无 | Spring Boot 3.x 启动失败时不输出条件报告 |
| `ConditionsReportMessageBuilder` 按包过滤 | 无 | Spring Boot 3.x 的 `--debug` 输出全量报告，无过滤 |

Spring Boot 3.x 的 `--debug` 参数会输出 `ConditionEvaluationReport`，但需要手动添加参数且输出全量报告（不过滤）。microsphere 的方案是自动输出（无需 `--debug`）、按包过滤（只看 microsphere 相关）、失败时也输出（error 级别），更适合生产环境调试。

---

## 小结

microsphere-spring-boot 的生命周期事件与启动诊断，通过三个独立子系统解决 Spring Boot 启动过程的痛点：

1. **一次性事件监听器**（`OnceApplicationPreparedEventListener` / `OnceMainApplicationPreparedEventListener`）：防止父子容器重复触发，过滤 Spring Cloud bootstrap context
2. **Jar 冲突检测链**（`BannedArtifactClassLoadingListener` -> `ArtifactsCollisionDiagnosisListener` -> `ArtifactsCollisionFailureAnalyzer`）：从类加载拦截到冲突检测到修复建议，形成完整的诊断闭环
3. **条件评估报告**（`ConditionEvaluationReportInitializer` -> `ConditionEvaluationReportBuilder` -> `ConditionEvaluationReportListener` / `ConditionEvaluationSpringBootExceptionReporter`）：自动缓存、按包过滤、启动成功和失败时都输出

三个子系统通过 `spring.factories` 自动注册，无需用户配置。Jar 冲突检测和条件报告默认禁用（通过属性启用），避免性能开销；一次性事件监听器和 BannedArtifact 默认启用。
