# 04-REQ：microsphere-spring-boot 完整需求规格

> 本文是 microsphere-spring-boot 的完整需求文档（v2，源码+Boot 3.5.16 验证）。
> 
> **⚠️ 重要修正（2026-08-03，Explore-11）**：
> - ~~REQ-调度器指标~~ 已被 Boot 2.6+ `TaskExecutorMetricsAutoConfiguration` 全覆盖——微球会导致**双重埋点**
> - `BindListener` 是 Boot `BindHandler` 的**阉割版**（void 返回 vs Boot 可改变结果），不是增强
> - `BannedArtifactClassLoadingListener` 运行期**死代码**——方法签名与 Boot 3.x 接口错位
> - 真正独特能力仅 6 项（condition/构件冲突/诊断/端点/配置事件/默认属性合并）
>
> 所有需求分三类：
> 1. **已实现需求**（REQ-001~011，11 项）——含 Boot 原生能力标注 + v2 修正说明
> 2. **待修复**（REQ-D01~D08，8 项）——含 2 个运行期高危
> 3. **全新发散**（REQ-N01~N09，9 项）——基于已有代码模式发散

---

### 与 Spring Boot 3.5.16 重叠按包分布（v2，源码已验证）

| 包 | 文件数 | Boot 重叠 | 判定 |
|---|:---:|:---:|------|
| core/classloading | 1 | ~0% | 独有（运行期死代码→见 D01） |
| core/condition | 2 | ~0% | `@ConditionalOnPropertyPrefix` 独有 |
| core/diagnostics | 3 | ~0% | 构件冲突 FailureAnalyzer 独有 |
| actuator/artifacts+webEndpoints | 3 | ~0% | 端点独有 |
| core/context/properties 事件机制 | 6 | ~40% | BindHandler 基础设施同，变更事件独有 |
| core/context/properties/bind 核心 | 2 | ~90% | BindListener=**阉割版** void BindHandler |
| actuator/scheduler | 2 | ~90% | Boot 2.6+ `TaskExecutorMetricsAutoConfiguration` 已全量监控 |
| compatible | 10 | ~100% | Boot 源码逐字复制 |
| webmvc/webflux | 6 | ~0% | 桥接 03 扩展，Boot 无对应 |

**结论：6 个真正独有能力，其余约 60% 是 Boot 已有能力的平行实现或薄封装。**

---

## 项目定位

**microsphere-spring-boot 是 03-microsphere-spring 的 Spring Boot 适配层**，解决的核心问题是：**03 的 `@Enable*` 注解需要手动声明——在 Spring Boot 项目中，开发者期望"加依赖即生效"。04 通过 Spring Boot 的 `AutoConfiguration.imports` / `spring.factories` / `SpringApplicationRunListener` / `FailureAnalyzer` 等专有机制，把 03 的全部能力零配置注入 Boot 应用。**

与前面项目的层次关系：
- **01**（最底层）—— JDK 内部能力
- **02**（中间层）—— 通用工具库
- **03**（应用层）—— Spring Framework 扩展
- **04**（适配层）—— **将 03 的 @Enable* 能力转为 Boot AutoConfiguration**

**源码信息**：
- 路径：`/data/workspace/java-training-camp/cloud-native-code/share/microsphere-spring-boot/`
- 版本：`0.2.32-SNAPSHOT`，Spring Boot 3.0~4.1 兼容（`main` 分支）/ 2.0~2.7（`1.x` 分支）
- 模块：`core`（47 文件）+ `actuator`（10）+ `compatible`（10）+ `test`（4）+ `webflux`（3）+ `webmvc`（3）
- 自动装配：5 个 `AutoConfiguration.imports` + `spring.factories`（7 类注册）

### 与 Spring Boot 原生功能的对比

> 经对 `/data/workspace/source-code/code/spring/spring-boot/`（v3.5.16）源码验证，标注每项 REQ 与 Boot 原生能力的关系。

| REQ | Boot 已有？ | 结论 |
|:---:|:---:|------|
| 001 BindListener | `BindHandler` 回调 **有**，属性变更事件 **无** | 回调等价，事件发布是 microsphere 独有 |
| 002 configProperties | `/actuator/configprops` **有** | **重复**——Boot 原生已提供配置属性端点 |
| 002 configMetadata / artifacts / webEndpoints | **无** | microsphere 独有 |
| 003 调度器指标 | `TaskExecutorMetricsAutoConfiguration`（2.6+）**有** | **重复**——Boot 自动给调度器绑 Micrometer |
| 004 合并式排除 | **无**（Boot 是**替换式**） | microsphere 独有语义 |
| 005 默认属性注入 | `EnvironmentPostProcessor` + `defaultProperties` **有** | 机制等价（putIfAbsent），封装便捷加载路径 |
| 006 构件冲突检测 | **无** | microsphere 独有 |
| 007 条件评估报告 | `ConditionEvaluationReport.get(bf)` + `/actuator/conditions` **有** | **重复**——Boot 已有程序化 API + 端点 |
| 008 版本兼容 shim | **无** | microsphere 独有（Boot 不需要自兼容） |
| 009 Web AutoConfiguration | **N/A** | 专门桥接 03 模块 |
| 010 前缀条件注解 | **无** | microsphere 独有 |
| 011 Boot Binder 适配 | **N/A** | 专门桥接 03→Boot Binder |

**结论**：11 项中，**3.5 项（002/003/005部分/007）Boot 原生已有等价实现**；**5.5 项（001事件/002部分/004/006/010）Boot 真的没有**，是 microsphere 独特价值。

---

## 一、配置属性绑定监听

### REQ-001：@ConfigurationProperties Bean 属性变更事件

**问题**：Spring Boot 的 `@ConfigurationProperties` 绑定是**静默的**——绑定完成时你完全不知道什么时候发生了什么值。生产环境需要：配置加载完成后的审计、运行时属性值变更的通知、属性值被覆盖时的告警。Spring Boot 没有这种通知机制。

**产出**：
- `BindListener` SPI：5 个生命周期回调——`onStart(name, target, context)` / `onSuccess(name, target, context, result)` / `onCreate(name, target, context, result)` / `onFailure(name, target, context, throwable)` / `onFinish(name, target, context, result)`
- `ListenableBindHandlerAdapter`：`AbstractBindHandler` 子类，将 Boot 的绑定事件回调转发给 `BindListener` 链
- `ConfigurationPropertiesBeanPropertyChangedEvent`：每次属性值变更时发布 Spring `ApplicationEvent`，可用 `@EventListener` 接收
- `ConfigurationPropertiesBeanContext`：为每个 `@ConfigurationProperties` bean 维护属性快照——`setProperty()` 时 `deepEquals` 比较新旧值，仅在确实变更时发布事件
- `@EnableConfigurationPropertiesExtension`：一行注解启用全部能力（含 `@OverrideAnnotationAttributes` 注解属性覆盖 + BeanSource 三源收集 BindListener）

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- `ConfigurationPropertiesBeanContext.cloneBean()` 变量名拼写 `clnoedBean`（应为 `clonedBean`）
- `deepEquals` 对不可 `Cloneable` 的 `List`/`Map` 字段引用共享——内部元素修改可能**漏报**变更
- `EventPublishingConfigurationPropertiesBeanPropertyChangedListener.onStart()` 有空 if-else 分支（代码味道）

**配置规格**：
```java
@Configuration
@EnableConfigurationPropertiesExtension(
    adviseBindListener = true,       // 启用 BindListener SPI
    publishEvents = true,            // 发布 PropertyChangedEvent
    sources = {BEAN_FACTORY, SPRING_FACTORIES, JAVA_SERVICE_PROVIDER}
)
```

---

## 二、自定义 Actuator 端点

### REQ-002：4 个生产诊断 Actuator 端点

**问题**：Spring Boot Actuator 的 20+ 内置端点覆盖了 health/metrics/env/loggers 等常用能力，但**缺少三个关键信息**：运行时 classpath 上有哪些 jar 及其版本、Web 端点完整注册表、所有 `@ConfigurationProperties` 的当前绑定值。运维排查问题每次都要手动拼这些信息。

**产出**（注册为 `/actuator/microsphere/*`）：

| 端点 | 路径 | 输出内容 |
|------|------|---------|
| `artifacts` | `/actuator/microsphere/artifacts` | `ArtifactDetector.detect()` 扫描的全部 classpath 构件 GAV 清单 |
| `webEndpoints` | `/actuator/microsphere/web/endpoints` | 所有注册的 MVC/WebFlux/Servlet 端点元数据（`WebEndpointMapping` 模型） |
| `configMetadata` | `/actuator/microsphere/config/metadata` | `spring-configuration-metadata.json` 结构化内容（groups+properties） |
| `configProperties` | `/actuator/microsphere/config/properties` | 当前全部 `@ConfigurationProperties` bean 的绑定值 |

还有 `endpoints.properties` 默认配置（只保留 health/info/env/loggers/metrics/mappings/prometheus 等常用端点 + TTL 缓存）。

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- `WebEndpoints` 聚合端点**同步逐个**调其他端点，无超时——某个端点慢/异常会整体拖垮
- `ConfigurationPropertiesEndpoint` 把 `sourceMethod` 方法名塞进 `field` 字段，语义错位（`metadata.setDeclaredField(sourceMethod)`）
- TTL 缓存默认值覆盖了 Boot 原生的缓存策略——加了这个依赖就改行为（设计决策，非 bug）

**配置规格**：通过 `endpoints.properties` 默认配置，Enabling 开关通过标准 `management.endpoint.<id>.enabled`。

---

### REQ-003：Micrometer 指标化的线程池调度器

**问题**：`ThreadPoolTaskScheduler` 执行定时任务，但任务执行次数和耗时没有指标。Prometheus/Grafana 上调度任务是一个盲区——任务超时了、堆积了、全部失败了你都不知道。

**产出**：
- `MonitoredThreadPoolTaskScheduler`：继承 `ThreadPoolTaskScheduler`，自动注册 Micrometer 指标——`ExecutorServiceMetrics.monitor(registry, executor, beanName)`，暴露 `executor.pool.size` / `executor.active` / `executor.queued` / `executor.completed` 等 Gauge

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- **P2：标准调度 API 的耗时漏统计**——父类 `ThreadPoolTaskScheduler.schedule()` 走原始 executor，只有通过 `getScheduledExecutor()` 返回的 `DelegatingScheduledExecutorService` 提交的任务才被 Micrometer 计时。`@Scheduled` 注解方法的标准调度路径**不走** delegating executor，指标缺失

**配置规格**：替换原生 `ThreadPoolTaskScheduler` 为 `MonitoredThreadPoolTaskScheduler`。

---

## 三、自动配置管理

### REQ-004：合并式 AutoConfiguration 排除过滤器

**问题**：Spring Boot 的 `spring.autoconfigure.exclude` 属性是**覆盖式**的——后一个 PropertySource 的值替换前一个。多团队项目中，基础工程排除了 `RabbitAutoConfiguration`，业务模块排除了 `MongoAutoConfiguration`——如果业务模块的配置文件也设了 `spring.autoconfigure.exclude`，基础工程的排除就被覆盖丢了。

**产出**：
- `ConfigurableAutoConfigurationImportFilter`：`microsphere.autoconfigure.exclude` 属性——**合并式**收集所有 PropertySource 中同名字段的值，确保多个配置文件的排除列表不互相覆盖
- 与 `spring.autoconfigure.exclude` 并行：用 Spring 标准属性做覆盖式排除，用 `microsphere.autoconfigure.exclude` 做合并式排除——两种策略共存，各取所需

**状态**：[已实现] 📝 **待编写原理文档**

**配置规格**：
```properties
# 合并式——多个文件间不会互相覆盖
microsphere.autoconfigure.exclude=\
  org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,\
  org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
```

---

### REQ-005：默认属性注入

**问题**：每个 Spring Boot 项目都要在 `application.properties` 里写同样的默认值——`server.shutdown=graceful`、`spring.lifecycle.timeout-per-shutdown-phase=60s`、关闭 FormContent/HiddenHttpMethod Filter——这些不是"业务配置"，是"基础工程标准"。但 Spring Boot 没有"库自带默认值"的机制——每个库只能靠 AutoConfiguration 注册 bean，无法注入属性。

**产出**：
- `DefaultPropertiesPostProcessor` SPI + `SpringApplicationDefaultPropertiesPostProcessor`：从 `classpath*:/META-INF/config/default/*.properties` 加载默认属性——`core.properties`（优雅关闭 + 禁用冗余 Spring MVC Filter）、`endpoints.properties`（Actuator 端点默认策略）
- 合并语义：`putIfAbsent`——默认属性只在用户没有显式设置时才生效

**状态**：[已实现] 📝 **待编写原理文档**

**配置规格**：通过 `config/default/your-module.properties` 添加模块级默认属性。

---

## 四、启动诊断

### REQ-006：Classpath 构件冲突检测

**问题**：Maven 依赖冲突（两个版本的同名 jar 同时在 classpath 上）在编译时查不出来——报 `NoSuchMethodError` 或 `ClassNotFoundException` 时往往已经是生产事故。Spring Boot 默认没有 classpath 上的 jar 冲突检查。

**产出**：
- `ArtifactsCollisionDiagnosisListener`：`ApplicationReadyEvent` 时扫描 classpath，检查同一 `groupId:artifactId` 是否出现多个不同版本
- `ArtifactsCollisionFailureAnalyzer`：当冲突导致启动失败时，向用户输出"在 classpath 上发现了这些冲突 jar"的 FailureAnalyzer 消息（含 `mvn dependency:tree` 排查提示）
- `BannedArtifactClassLoadingListener`：启动时检查 classpath 上是否包含被禁用的 jar（如 `log4j-core:2.14.0`），命中时 fail-fast

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- `ArtifactsCollisionDiagnosisListener` 默认 **disabled**（`microsphere.diagnostics.artifacts-collision.enabled=false`）
- `BannedArtifactClassLoadingListener` 继承已标记 `@Deprecated(forRemoval=true)` 的 `SpringApplicationRunListenerAdapter`——弃用与注册并存
- `classLoader != contextClassLoader` 时 `BannedArtifactClassLoadingListener` 静默跳过（日志仅 trace）

**配置规格**：
```
microsphere.diagnostics.artifacts-collision.enabled = false  # 默认关闭
```

---

### REQ-007：条件评估报告

**问题**：Spring Boot 启动时 `ConditionEvaluationReport` 记录了哪些 AutoConfiguration 被加载、哪些被跳过、原因是 `@ConditionalOnClass` 不满足还是 `@ConditionalOnMissingBean` 已存在——但这份报告只在 `--debug` 模式下输出到控制台，无法程序化读取、无法导出、无法对比不同环境的差异。

**产出**：
- `ConditionEvaluationReportInitializer`：在 `ApplicationContextInitializer` 阶段通过 `BeanFactoryPostProcessor` 收集条件评估报告
- `ConditionEvaluationReportListener`：`ApplicationReadyEvent` 时输出按 `base-packages`（默认 `io.microsphere`）过滤后的条件评估报告
- `ConditionEvaluationSpringBootExceptionReporter`：启动失败时输出失败时的条件评估快照
- 输出格式：分 AutoConfiguration Class × Condition 两维表格 + 计数摘要

**状态**：[已实现] 📝 **待编写原理文档**

**配置规格**：
```
microsphere.spring.boot.condition-evaluation-report.base-packages = io.microsphere
```

---

## 五、版本兼容

### REQ-008：Spring Boot 多版本兼容 shim

**问题**：Spring Boot 3.x→4.x 的包重构导致某些类的位置/API 发生变化。一个库要同时兼容 3.0~4.1 需要维护两套代码。常见的做法是维护两个分支——microsphere 用了兼容 shim 模块替代。

**产出**（`microsphere-spring-boot-compatible`，10 文件）：
- `BootstrapContext` / `BootstrapRegistry` / `ConfigurableBootstrapContext` 等——从 Boot 3.5.x fork 的类，在 Boot 4.x 中被移除后由 shim 提供
- Maven profile 控制：`spring-boot-3.5`（默认）、`3.0~3.4`、`4.0`、`4.1` 多 profile，pom 中按激活 profile 决定是否引入 compatible 模块

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- **P2：默认 profile（spring-boot-3.5）引用 Boot 3.5 才有的新类**——切换到 `3.0~3.4` profile 编译会失败
- `package-info.java` 注释说"用于兼容低版本"，但 pom 只在 4.x profile 引入——方向性矛盾

**配置规格**：Maven `-P` 激活对应 Spring Boot 版本的 profile。

---

## 六、Web 自动配置

### REQ-009：WebMVC/WebFlux 自动装配桥接

**问题**：03-microsphere-spring 提供了 `@EnableWebMvcExtension` 和 `@EnableWebFluxExtension`——但在 Spring Boot 项目中用户不想手动加这些注解。需要 Boot 标准的 `@AutoConfiguration` 机制自动激活。

**产出**：
- `WebMvcAutoConfiguration`：`@ConditionalOnWebMvcAvailable` → 自动 `@EnableWebMvcExtension(registerHandlerInterceptors=true, reversedProxyHandlerMapping=true)` + 条件注册 `ContentCachingFilter` / `ConfigurableContentNegotiationManagerWebMvcConfigurer` / `ExclusiveViewResolverApplicationListener`
- `WebFluxAutoConfiguration`：`@ConditionalOnWebFluxAvailable` → 自动 `@EnableWebFluxExtension(reversedProxyHandlerMapping=true)`
- 条件注解：`@ConditionalOnWebMvcAvailable` / `@ConditionalOnWebFluxAvailable`——`@ConditionalOnWebApplication(type=SERVLET/REACTIVE)` + `@ConditionalOnClass(03 模块类)` + `@ConditionalOnProperty(matchIfMissing=true)`

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- `PropertyConstants` 常量名拼写 `ENALBED`（应为 `ENABLED`）——webflux 和 webmvc 同构双模块含相同拼写错误

**配置规格**：无配置项（加依赖即生效，可通过 `@ConditionalOnProperty` 的 `haveValue=false` 禁用）。

---

## 七、关键注解

### REQ-010：属性前缀存在性条件注解

**问题**：Spring Boot 的 `@ConditionalOnProperty` 只能检查特定键值——如果一个配置前缀下"有任何属性"就可以，不需要知道具体键名。比如"只要 `app.datasource` 前缀下有任何配置项就启用 DataSource 自动配置"。

**产出**：
- `@ConditionalOnPropertyPrefix("spring.datasource")`：检查目标前缀是否存在任意属性
- `OnPropertyPrefixCondition`：`findPropertyNames()` 扫描全部 `EnumerablePropertySource` + 非可枚举源遍历——如果找到任何以 prefix 开头的属性名，则条件满足

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **P1：前缀边界匹配不精确**——javadoc 声称 prefix 自动补点，但实现只做 `startsWith(prefix)`——`foo.bar` 会误匹配 `foobar` 开头的属性
- **P1：`findPropertyNames` 全量扫描所有 PropertySource**——Environment 中属性量大时性能显著退化

**配置规格**：
```java
@ConditionalOnPropertyPrefix("spring.datasource")
```

---

### REQ-011：Spring Boot AutoConfiguration 条件化属性绑定

**问题**：03 的 `ConfigurationBeanBinder` 需要一个 Spring Boot 兼容的实现——03 工作在纯 Spring Framework 层（用 `DataBinder` + `PropertySources`），04 需要利用 Boot 的 `Binder` API（更强大的类型转换和 Relaxed Binding）。

**产出**：
- `BindableConfigurationBeanBinder`：实现 03 的 `ConfigurationBeanBinder`，内部用 Spring Boot `Binder` + `MapPropertySource` 绑定 Map 到 Bean
- `ListenableConfigurationPropertiesBindHandlerAdvisor`：实现 Boot 的 `ConfigurationPropertiesBindHandlerAdvisor`，`apply()` 时收集 `BindListener` bean 并包装进 `ListenableBindHandlerAdapter`
- `ConfigurationBeanBindingPostProcessor`（来自 03）：在 04 环境下自动生效——加依赖即有

**状态**：[已实现] 📝 **待编写原理文档**

**已知问题**：
- `ConfigurationPropertyUtils` 通过反射访问 Boot 内部类（`JavaBeanBinder$BeanProperty`、`ConfigurationPropertyName.elements` 私有字段）——Boot 4.0 包重构后极易失效。与 01/02 的 `sun.reflect` 反射模式同构，目标换成 Boot 内部

**配置规格**：通过 `@EnableConfigurationPropertiesExtension` 启用（已在 `ConfigurationPropertiesAutoConfiguration` 中默认激活）。

---

## 八、待实现需求（bug 修复）

### REQ-D01：OnPropertyPrefixCondition 前缀边界修复

**方案**：`startsWith(prefix)` → `startsWith(prefix + ".") || equals(prefix)`——确保 `foo.bar` 不会误匹配 `foobar` 开头的属性。

**状态**：[待修复] — 当前边界不精确导致误匹配。

---

### REQ-D02：MonitoredThreadPoolTaskScheduler 标准调度路径耗时指标修复

**方案**：在 `afterSingletonsInstantiated` 中覆盖父类的 `scheduledExecutor` 字段（反射）或提供一个 `MonitoredScheduledThreadPoolExecutor` 替代默认的 `ScheduledThreadPoolExecutor`，使 `@Scheduled` 注解方法的执行路径也经过 Micrometer 计时。

**状态**：[待修复] — 当前只有显式 `getScheduledExecutor()` 路径被计时，`@Scheduled` 路径指标缺失。

---

### REQ-D03：SpringBootVersion 低版本 profile 编译修复

**方案**：`SpringBootVersion.CURRENT` 的解析逻辑改为不依赖 Boot 3.5 才引入的类——或在 3.0~3.4 profile 下提供替代实现。

**状态**：[待修复] — 切换到 `3.0~3.4` profile 时编译失败。

---

### REQ-D04：WebEndpoints 聚合端点健壮性修复

**方案**：改为异步并行调用各子端点 + 每个端点单独超时 + 异常端点返回错误信息而非中断全部。

**状态**：[待修复] — 当前同步顺序调用，一端卡住全端受影响。

---

### REQ-D05：SpringApplicationRunListenerAdapter 弃用与注册矛盾清理

**方案**：移除 `BannedArtifactClassLoadingListener` 对已弃用 `SpringApplicationRunListenerAdapter` 的继承——改为直接实现 `SpringApplicationRunListener` 接口；同时更新 `spring.factories` 中注册的 `FailureReportSpringApplicationRunListener`（亦标注 forRemoval）。

**状态**：[待修复] — 弃用类仍在 spring.factories 中被注册，依赖关系未解耦。

---

### REQ-D06：ConfigurationPropertiesBeanContext deepEquals 深度修复

**方案**：`cloneBean()` 中对不可 `Cloneable` 的集合字段执行深层递归复制（或改用序列化/反射深度拷贝），`deepEquals` 确保能检测到 `List`/`Map` 子元素的变更。

**状态**：[待修复] — 当前引用共享导致内部元素变更可能漏报。

---

## 九、发散需求（生产环境需要的全新能力）

### REQ-N01：条件评估报告 Actuator 端点

**生产痛点**：线上排查"某个 AutoConfiguration 为什么没加载"需要开 `--debug` 重启——生产环境不可接受。运维需要一个 `/actuator/microsphere/conditions` 端点即时返回当前的条件评估报告。

**产出**：`ConditionsEndpoint` —— 基于现有的 `ConditionEvaluationReportInitializer` + `ConditionEvaluationReportListener` 收集的数据，暴露 HTTP/JMX 端点，按 AutoConfiguration class 展示每项条件的匹配/不匹配原因。无需 debug 模式。

**状态**：[待实现]

---

### REQ-N02：BindListener 属性变更审计日志

**生产痛点**：配置中心推送了新配置——谁触发的？哪些 `@ConfigurationProperties` bean 的属性变了？变更前后的值是什么？生产环境需要配置变更的审计追踪。

**产出**：`BindChangeAuditor` —— 基于已有的 `BindListener.onSuccess()` 回调 + `ConfigurationPropertiesBeanPropertyChangedEvent`，在属性变更时记录 old→new 值到日志/事件，可按 bean 或属性名过滤。

**状态**：[待实现]

---

### REQ-N03：配置变更 → 动态刷新联动

**生产痛点**：`/actuator/refresh` 只能刷新 `@RefreshScope` bean。如果你的 bean 是 `@ConfigurationProperties` 但没加 `@RefreshScope`，配置中心改了属性值后不会生效。Spring Cloud `@RefreshScope` 需要 Cloud Context 依赖——纯 Boot 项目没有这个能力。

**产出**：`ConfigurationPropertiesRefresher` —— 基于 03 的 `PropertySourcesChangedEvent` + 04 的 `BindListener` + `ConfigurationPropertiesBeanContext`，在属性源变更时自动重新绑定已变更的 `@ConfigurationProperties` bean，无需 `@RefreshScope` 注解。

**状态**：[待实现]（与 03-REQ-N08 配套，03 做框架层、04 做 Boot 集成层）

---

### REQ-N04：MonitoredScheduledExecutorService 通用化

**生产痛点**：`MonitoredThreadPoolTaskScheduler` 只覆盖了调度器——其他自定义 `ThreadPoolExecutor`、`ScheduledExecutorService` 仍然没有 Micrometer 指标。生产环境任何线程池都应被监控。

**产出**：`ExecutorServiceMetricsBinder` —— 通用的 `ExecutorService` → Micrometer 包装器。复用 `MonitoredThreadPoolTaskScheduler` 中的 `ExecutorServiceMetrics.monitor()` 逻辑，但抽取为独立的 `BeanPostProcessor`，自动检测容器中的 `ExecutorService` / `ScheduledExecutorService` bean 并注册指标（支持排除列表）。

**状态**：[待实现]

---

### REQ-N05：BindListener Micrometer 指标

**生产痛点**：`@ConfigurationProperties` 绑定是启动时的关键路径——绑定耗时长会拖慢整个启动。但没有任何指标告诉你"这次启动绑定了多少个属性、耗时多少"。`BindListener.onSuccess/onFailure` 已有回调，只缺一个指标采集器。

**产出**：`BindListenerMetrics` —— 实现 `BindListener`，在 `onSuccess`/`onFailure` 中记录 Micrometer Counter（绑定成功数、失败数）+ Timer（单个属性绑定耗时），tag 含 `bean`/`property`/`source`。自动注册为 bean 后 `@EnableConfigurationPropertiesExtension` 的三源收集会自动纳入。

**状态**：[待实现]（基于已有 BindListener SPI + REQ-003 的 Micrometer 集成模式）

---

### REQ-N06：Classpath 健康 HealthIndicator

**生产痛点**：`ArtifactsCollisionDiagnosisListener` 检测到冲突但以异常形式报告——运维无法通过 `/actuator/health` 持续监控。Prometheus 告警规则需要的是"当前 health=UP 还是 DOWN"而非日志中的异常。

**产出**：`ArtifactsCollisionHealthIndicator` —— 实现 Boot `HealthIndicator`，在 `health()` 中调用 `ArtifactDetector` 检测冲突——无冲突返回 `UP`，有冲突返回 `DOWN` 并列出冲突的 artifact 详情。运维可以通过 Prometheus Alertmanager 在冲突发生时立即收到告警。

**状态**：[待实现]（基于已有 ArtifactDetector + 04 的 Boot HealthIndicator 标准扩展）

---

### REQ-N07：AutoConfiguration 排除冲突可视化

**生产痛点**：多团队多模块排除 AutoConfiguration 时，经常不知道"最终生效的是什么"——A 团队排除了 RabbitMQ，B 团队无意中又引入了。`microsphere.autoconfigure.exclude` 合并式排除了，但没有告诉你合并后的最终结果。

**产出**：`ExcludeReportEndpoint` —— `/actuator/microsphere/exclude-report` 端点。列出：每个 PropertySource 中声明的排除项 → 合并后的最终排除列表 → 被排除的 AutoConfiguration 的详细原因（来自 ConditionsReportEndpoint 的 cross-reference）。

**状态**：[待实现]（基于 REQ-004 的合并式排除 + Boot 已有的 ConditionsReportEndpoint）

---

### REQ-N08：WebEndpointMapping → OpenAPI/Swagger 导出

**生产痛点**：04 的 `webEndpoints` 端点已收集了所有 MVC/WebFlux/Servlet 端点的完整元数据（路径/方法/参数/Headers/Content-Type）——但只能看 JSON，无法直接生成 OpenAPI 文档给前端/Swagger UI 消费。

**产出**：`OpenApiExporter` —— 读取 `WebEndpointMappingRegistry` 中的全部端点元数据 → 构造 OpenAPI 3.0 `Paths` + `Components` → 从 ConfigurationMetadata 补充参数描述 → 输出 `openapi.json`。

**状态**：[待实现]（基于 REQ-002 的 WebEndpointMapping 元数据，复用 03-web 的规则框架）

---

### REQ-N09：条件评估链路追踪

**生产痛点**：一个 `@ConditionalOnPropertyPrefix("spring.datasource")` 条件不满足导致某个 AutoConfiguration 没加载——但你想知道"究竟是哪个属性源没有这个前缀？所有 PropertySource 中哪些有？"目前只能看 Boolean 结果。

**产出**：`ConditionTraceListener` —— 在 `OnPropertyPrefixCondition` 的 `getMatchOutcome()` 中记录每个 PropertySource 的前缀匹配状态。暴露端点 `/actuator/microsphere/condition-trace/{className}` —— 输出该条件在每个 PropertySource 中的检查明细。

**状态**：[待实现]（基于 REQ-010 的 OnPropertyPrefixCondition + 04 的 ConditionEvaluationReport 基础设施）

---

> **⚠️ 跨文档标注**：本 REQ 文档中的 6 项 Boot 已有等价项（001回调/002configProps/003调度器/005默认属性/007条件报告/011 Binder）将在 **Spring Boot 官方源码 REQ 文档**（待产出）中作为"已实现需求"出现。两个文档互为对照——Boot REQ 描述 Spring 原生实现，04 REQ 描述 microsphere 的封装层与独特能力。请勿在两个文档中重复标记同一项为"独有"。

---

## 十、跨项目一致性校验

> 以下模式在 01/02/03/04 中持续出现——确认是作者共性代码模式。

| 模式 | 01 表现 | 02 表现 | 03 表现 | 04 表现 |
|------|---------|---------|---------|---------|
| 反射读内部私有字段 | ClassLoader.classes | URLClassPath.ucp | DefaultListableBeanFactory.resolvableDependencies | JavaBeanBinder$BeanProperty / ConfigurationPropertyName.elements |
| WebFlux/MVC 同构 + 相同 bug | — | — | ConsumingWebEndpointMapping ×2 | PropertyConstants ENALBED ×2 + ConditionalOnWebXxx ×2 |
| 弃用与注册并存 | — | — | — | SpringApplicationRunListenerAdapter @Deprecated 但 spring.factories 仍注册 |
| 测试固化风险 | — | FilterOperatorTest | ProducesRuleTest | 67 个测试文件，需确认 OnPropertyPrefix 边界/ENALBED 行为是否被固化 |

---

## 十一、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 0.2.32-SNAPSHOT | 2024~2025 | 持续开发（Maven Central 已发布），Spring Boot 3.0~4.1 兼容 |
| — | 2026-08-02 | REQ 文档编写（第一版） |
