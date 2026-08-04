# 04-05 Actuator 端点、兼容层与测试框架

## 问题：Spring Boot Actuator 的可观测性和跨版本兼容

Spring Boot Actuator 提供了 `/actuator/*` 端点用于运行时监控。但 Spring Boot 原生端点聚焦于应用健康、指标和信息，不覆盖以下场景：

| 痛点 | Spring Boot 的现状 | 问题 |
|------|------------------|------|
| **ClassPath artifact 不可查** | 无 | 无法通过 HTTP 查询运行时加载了哪些 Jar |
| **配置元数据运行时不可查** | `spring-configuration-metadata.json` 只在 IDE 中使用 | 运行时无法查询"有哪些配置项、默认值是什么" |
| **所有端点的 Read Operation 不可聚合** | 每个端点独立访问 | 需要逐个调用 `/actuator/xxx`，无法一次获取所有端点结果 |
| **Actuator 线程池与业务线程池混用** | Actuator 使用 Spring Boot 默认 `TaskScheduler` | Actuator 任务可能干扰业务定时任务 |
| **Spring Boot 1.x/2.x API 不兼容** | `BootstrapContext` 等在 2.4+ 才引入 | 需要在旧版本中使用新 API |

microsphere-spring-boot 通过 **4 个自定义端点 + 监控线程池 + 兼容层 + 测试框架** 解决这些问题。

---

## 设计：四个维度

### 整体架构

```
microsphere-spring-boot-actuator (AutoConfiguration.imports 自动注册)
    │
    ├── ActuatorAutoConfiguration (@AutoConfigureOrder LOWEST_PRECEDENCE)
    │       └── MonitoredThreadPoolTaskScheduler (actuatorTaskScheduler)
    │               @ConditionalOnBean(MeterRegistry)
    │
    └── ActuatorEndpointsAutoConfiguration (@ConditionalOnActuatorEndpointPresent)
            ├── ArtifactsEndpoint         @Endpoint(id="artifacts")
            ├── WebEndpoints               @WebEndpoint(id="webEndpoints")
            └── ConfigurationProcessorConfiguration (@ConditionalOnConfigurationProcessorPresent)
                    ├── ConfigurationMetadataReader
                    ├── ConfigurationMetadataRepository
                    ├── ConfigurationMetadataEndpoint  @Endpoint(id="configMetadata")
                    └── ConfigurationPropertiesEndpoint @Endpoint(id="configProperties")

microsphere-spring-boot-compatible (无 AutoConfiguration，纯 API 兼容)
    └── org.springframework.boot.*
            ├── BootstrapContext / BootstrapRegistry / DefaultBootstrapContext
            ├── ServerProperties / JacksonProperties / MultipartProperties
            └── MultipartConfigFactory

microsphere-spring-boot-test (测试基础设施)
    └── AbstractAutoConfigurationTest<A, R>
            ├── AutoConfigurationTest<A> (非 Web)
            ├── WebAutoConfigurationTest (Servlet)
            └── ReactiveWebAutoConfigurationTest (Reactive)
```

---

### 自定义 Actuator 端点

#### ArtifactsEndpoint：ClassPath artifact 查询

```java
@Endpoint(id = "artifacts")
public class ArtifactsEndpoint {

    private final ArtifactDetector artifactDetector;

    @ReadOperation
    public List<Artifact> getArtifactMetaInfoList() {
        return artifactDetector.detect(false);
    }
}
```

访问 `GET /actuator/artifacts` 返回 ClassPath 中所有 Maven artifact 的元信息（groupId、artifactId、version、file path）。使用 `ArtifactDetector`（microsphere-java 第 2 篇）检测。

**使用场景**：在线诊断"运行时加载了哪些 Jar""版本是否正确"，无需登录服务器执行 `jar tf` 或 `mvn dependency:tree`。

#### ConfigurationMetadataEndpoint：配置元数据查询

```java
@Endpoint(id = "configMetadata")
public class ConfigurationMetadataEndpoint {

    @ReadOperation
    public ConfigurationMetadataDescriptor getConfigurationMetadata() {
        ConfigurationMetadataDescriptor descriptor = new ConfigurationMetadataDescriptor();
        descriptor.groups = repository.getGroups();
        descriptor.properties = repository.getProperties();
        return descriptor;
    }
}
```

访问 `GET /actuator/configMetadata` 返回所有 `spring-configuration-metadata.json` 中的配置组和属性。数据来源于 `ConfigurationMetadataRepository`（第 4 篇）。

**使用场景**：运行时查询"这个框架支持哪些配置项""默认值是什么""什么类型"，生成配置文档或校验配置拼写。

#### ConfigurationPropertiesEndpoint：已解析配置属性查询

```java
@Endpoint(id = "configProperties")
public class ConfigurationPropertiesEndpoint {

    @ReadOperation
    public ConfigurationPropertiesDescriptor getConfigurationProperties() {
        List<ConfigurationProperty> fromServiceLoaders = loadAll();           // Java SPI
        List<ConfigurationProperty> fromMetadata = adaptFromRepository();     // 配置元数据
        return new ConfigurationPropertiesDescriptor()
            .addConfigurationProperties(fromServiceLoaders)
            .addConfigurationProperties(fromMetadata);
    }
}
```

合并两个来源的配置属性：
- **Java ServiceLoader**：`ConfigurationPropertyLoader.loadAll()` 加载 `META-INF/services/` 中注册的 `ConfigurationProperty`
- **配置元数据仓库**：从 `ConfigurationMetadataRepository` 的 `ItemMetadata` 转换为 `ConfigurationProperty`

**与 `ConfigurationMetadataEndpoint` 的区别**：后者返回的是"声明的元数据"（编译时生成），前者返回的是"运行时解析的属性"（包含实际值和来源）。

#### WebEndpoints：端点聚合

```java
@WebEndpoint(id = "webEndpoints")
public class WebEndpoints {

    @ReadOperation
    public Map<String, Object> invokeReadOperations() {
        Map<String, Object> results = newHashMap();
        for (ExposableWebEndpoint webEndpoint : webEndpointsSupplier.getEndpoints()) {
            if (endpointBean == this) continue;  // 排除自身
            for (WebOperation operation : webEndpoint.getOperations()) {
                if (isReadOperation(operation)) {
                    results.put(operation.getId(), operation.invoke(context));
                }
            }
        }
        return results;
    }
}
```

访问 `GET /actuator/webEndpoints` 一次调用所有 Web 端点的 Read Operation，返回 `{endpointId: result}` 映射。**排除自身**避免递归调用。

**使用场景**：一次性获取所有端点状态，减少 HTTP 请求次数。适合监控系统定期采集。

---

### MonitoredThreadPoolTaskScheduler：Actuator 专用监控线程池

#### 设计

```java
public class MonitoredThreadPoolTaskScheduler extends ThreadPoolTaskScheduler
        implements ApplicationContextAware, SmartInitializingSingleton {

    private DelegatingScheduledExecutorService delegate;

    @Override
    protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory,
                                                       RejectedExecutionHandler handler) {
        ScheduledExecutorService executor = super.createExecutor(poolSize, threadFactory, handler);
        this.delegate = new DelegatingScheduledExecutorService(executor);  // 包装但不监控
        return executor;
    }

    @Override
    public void afterSingletonsInstantiated() {
        MeterRegistry registry = context.getBean(MeterRegistry.class);
        ScheduledExecutorService scheduledExecutor = super.getScheduledExecutor();
        // 用 Micrometer 包装原始 executor，然后设为 delegate 的内部委托
        this.delegate.setDelegate(monitor(registry, scheduledExecutor, beanName));
    }
}
```

**两阶段设计**：
1. `createExecutor`：创建线程池后用 `DelegatingScheduledExecutorService` 包装，保存引用。此时 `delegate` 的内部委托是原始 executor，**尚未被 Micrometer 监控**
2. `afterSingletonsInstantiated`：所有 Bean 创建完成后，获取 `MeterRegistry` 和原始 executor，用 `ExecutorServiceMetrics.monitor()` 包装原始 executor 为被监控的 executor，然后通过 `delegate.setDelegate()` 替换内部委托

最终结构：`DelegatingScheduledExecutorService -> MonitoredExecutorService -> OriginalExecutor`

**为什么要延迟到 `afterSingletonsInstantiated`**：`MeterRegistry` Bean 可能在 `MonitoredThreadPoolTaskScheduler` 之后创建。`SmartInitializingSingleton` 确保所有单例 Bean 都就绪后再获取 `MeterRegistry`。

**为什么用 `beanName` 而非硬编码字符串**：`monitor(registry, scheduledExecutor, beanName)` 使用 Bean 名称作为 Micrometer 指标的 tag。这使得多个 `MonitoredThreadPoolTaskScheduler` 实例（如果有）可以通过不同 Bean 名称区分指标。

**监控指标**：注册后 Micrometer 自动采集线程池的 `active`/`completed`/`queued`/`pool.size`/`pool.core`/`pool.max` 等指标，通过 `/actuator/metrics` 端点暴露。

---

### 兼容层：Spring Boot API 跨版本支持

#### 设计

`microsphere-spring-boot-compatible` 模块的类放在 `org.springframework.boot` 包下--直接复用 Spring Boot 的包名，提供旧版本中不存在的新 API：

| 类 | Spring Boot 版本 | 作用 |
|---|----------------|------|
| `BootstrapContext` | 2.4+ | 启动上下文接口 |
| `BootstrapRegistry` | 2.4+ | 启动注册器接口 |
| `ConfigurableBootstrapContext` | 2.4+ | 可配置启动上下文 |
| `DefaultBootstrapContext` | 2.4+ | 默认实现 |
| `BootstrapContextClosedEvent` | 2.4+ | 关闭事件 |
| `ServerProperties` | 1.x/2.x | `server.*` 属性（旧包名） |
| `JacksonProperties` | 1.x/2.x | `spring.jackson.*` 属性（旧包名） |
| `MultipartProperties` | 1.x/2.x | `spring.servlet.multipart.*` 属性（旧包名） |
| `MultipartConfigFactory` | 1.x | Multipart 配置工厂（1.x API） |

**兼容策略**：这些类在 Spring Boot 2.4+ 中已存在（`BootstrapContext` 等）或在旧版本中存在（`ServerProperties` 旧包名）。compatible 模块在 classpath 中提供这些类，使得 microsphere 代码可以编译和运行在不同 Spring Boot 版本上。

**风险**：如果 Spring Boot 升级后这些类的签名变化，compatible 模块的 shim 可能与真实 API 不一致。但 compatible 模块不注册 `AutoConfiguration`，不会自动激活--只有在 classpath 中没有 Spring Boot 原生类时才生效（Java ClassLoader 按 classpath 顺序加载，先找到的优先）。

---

### 测试框架：自动装配测试基类

#### 设计

```java
public abstract class AbstractAutoConfigurationTest<A, R extends AbstractApplicationContextRunner> {

    protected final Class<A> autoConfigurationClass;  // 泛型解析
    protected final R runner;                          // ApplicationContextRunner

    protected AbstractAutoConfigurationTest() {
        // 解析泛型参数 A 和 R
        this.autoConfigurationClass = resolveGeneric(0);
        this.runnerClass = resolveGeneric(1);
        this.runner = newRunner();
    }
}
```

三个子类：
- `AutoConfigurationTest<A>`：非 Web 测试，使用 `ApplicationContextRunner`
- `WebAutoConfigurationTest`：Servlet Web 测试，使用 `WebApplicationContextRunner`
- `ReactiveWebAutoConfigurationTest`：Reactive Web 测试，使用 `ReactiveWebApplicationContextRunner`

**设计意图**：封装 Spring Boot `ApplicationContextRunner` 的通用测试模式--创建 runner、注册自动装配类、断言条件。子类只需指定自动装配类类型，无需重复编写 runner 创建逻辑。

---

## 永恒原理

### 1. Actuator 端点作为可观测性入口

Spring Boot Actuator 的 `@Endpoint` 机制提供了一种标准化的"HTTP 暴露"方式：标注 `@ReadOperation` 的方法自动映射为 `GET /actuator/{id}`。microsphere 利用这个机制暴露了 Spring Boot 不提供的运维信息（artifact 列表、配置元数据、端点聚合）。

这体现了"**可观测性的三个支柱**"（Metrics、Logging、Tracing）之外的第四个支柱--**Introspection**（自省）：应用能够通过标准接口暴露自身的内部状态（加载了什么、配置了什么、提供了什么端点）。

### 2. 线程池隔离与监控

`MonitoredThreadPoolTaskScheduler` 将 Actuator 的定时任务与业务定时任务隔离到不同线程池，并用 Micrometer 监控。这是"**资源隔离**"原则的体现：

- **隔离**：Actuator 任务（health check、metrics 采集）不影响业务任务
- **监控**：通过 Micrometer 暴露线程池指标，可以观察 Actuator 线程池的健康状况
- **配置**：单线程默认值（`poolSize=1`）适合轻量级 Actuator 任务，可按需调整

### 3. API 兼容层的包名复用策略

compatible 模块将类放在 `org.springframework.boot` 包下，这是"**包名复用**"策略--通过使用与 Spring Boot 相同的包名，让 microsphere 代码可以无感知地使用这些 API，无论它们来自 Spring Boot 原生还是 compatible 模块。

这种策略的风险是"**类冲突**"--如果 classpath 中同时有 Spring Boot 原生类和 compatible 模块的 shim 类，ClassLoader 只会加载先找到的那个。compatible 模块需要确保其 shim 类与 Spring Boot 原生类的行为一致，否则可能导致运行时异常。

---

## 边界与反例

### 1. ArtifactsEndpoint 的性能开销

`ArtifactDetector.detect(false)` 遍历 ClassPath 中所有 Jar，解析 Maven 元数据。在大型项目（500+ Jar）中，单次调用可能耗时数秒。由于 `@ReadOperation` 是同步的，HTTP 请求会阻塞直到检测完成。

**缓解**：Actuator 端点通常由监控系统定时调用，不是高频请求。如果性能敏感，可以考虑缓存检测结果。

### 2. WebEndpoints 的递归风险

`WebEndpoints.invokeReadOperations()` 遍历所有 Web 端点并调用其 Read Operation。虽然代码排除了自身（`if (endpointBean == this) continue`），但如果其他端点的 Read Operation 间接触发了 `WebEndpoints`，仍可能导致递归。

**缓解**：当前实现通过 `isReadWebOperationCandidate` 过滤，只调用"无参数的 Read Operation"，降低了递归风险。但如果某个端点的 Read Operation 内部发起了对 `/actuator/webEndpoints` 的 HTTP 请求，仍会递归。

### 3. MonitoredThreadPoolTaskScheduler 的两阶段时序窗口

`createExecutor` 在 `initialize()` 阶段调用，`afterSingletonsInstantiated` 在所有单例 Bean 创建后调用。两个阶段之间存在时间窗口--在此期间 `delegate` 的内部委托是原始 executor（未被 Micrometer 监控）。如果在此窗口内有定时任务执行，其指标不会被采集。

**缓解**：窗口期通常很短（从 `ThreadPoolTaskScheduler.initialize()` 到 `SmartInitializingSingleton` 回调之间），且 Actuator 的定时任务通常在应用就绪后才开始调度。实际影响可忽略。

### 4. 兼容层的版本检测缺失

compatible 模块没有运行时版本检测机制--它不检查 classpath 中是否已有 Spring Boot 原生类。如果 classpath 顺序导致 compatible 模块的 shim 类被先加载，而 Spring Boot 原生类行为不同，可能导致隐蔽的 bug。

### 5. 测试框架的泛型解析

`AbstractAutoConfigurationTest` 通过 `ResolvableType` 解析泛型参数。如果子类是匿名类或泛型签名不完整，解析可能失败（与第 4 篇的 `GenericBeanPostProcessorAdapter` 相同的问题）。

---

## 现代 Spring Boot（3.x）是否已支持？

| microsphere 特性 | Spring Boot 3.x 是否有等价物 | 说明 |
|------------------|------------------------|------|
| `ArtifactsEndpoint` | 无 | Spring Boot 3.x 无 artifact 列表端点 |
| `ConfigurationMetadataEndpoint` | 无 | Spring Boot 3.x 无运行时配置元数据查询 |
| `ConfigurationPropertiesEndpoint` | 部分 | Spring Boot 3.x 有 `/actuator/configprops`，但不合并元数据 |
| `WebEndpoints` 聚合 | 无 | Spring Boot 3.x 无端点聚合 |
| `MonitoredThreadPoolTaskScheduler` | 无 | Spring Boot 3.x 无 Actuator 专用监控线程池 |
| 兼容层 | 不适用 | Spring Boot 3.x 已移除 1.x API，compatible 模块主要面向 2.x |
| 测试框架 | 部分 | Spring Boot 3.x 有 `ApplicationContextRunner`，但无泛型基类封装 |

Spring Boot 3.x 的 `/actuator/configprops` 端点暴露 `@ConfigurationProperties` Bean 的属性值，但不暴露配置元数据（类型、默认值、描述）。microsphere 的 `ConfigurationMetadataEndpoint` 补充了这个空白。

---

## 小结

microsphere-spring-boot 的 Actuator 端点、兼容层与测试框架，通过四个维度扩展了 Spring Boot 的可观测性和开发体验：

1. **4 个自定义端点**：`artifacts`（ClassPath artifact 查询）、`configMetadata`（配置元数据查询）、`configProperties`（已解析配置属性查询）、`webEndpoints`（端点聚合）
2. **`MonitoredThreadPoolTaskScheduler`**：Actuator 专用线程池，隔离 + Micrometer 监控，两阶段设计（`createExecutor` 包装 + `afterSingletonsInstantiated` 注册）
3. **兼容层**：Spring Boot API 跨版本 shim，包名复用策略
4. **测试框架**：泛型驱动的自动装配测试基类，封装 `ApplicationContextRunner` 通用模式

至此，microsphere-spring-boot 的 5 篇深度分析全部完成。
