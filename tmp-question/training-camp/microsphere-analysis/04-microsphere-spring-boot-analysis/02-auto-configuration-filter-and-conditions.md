# 04-02 自动装配过滤与条件注解扩展

## 问题：Spring Boot 的自动装配控制粒度不够

Spring Boot 的自动装配通过 `@EnableAutoConfiguration` + `spring.factories`/`AutoConfiguration.imports` 实现。控制机制有三层：

1. **`spring.autoconfigure.exclude`**：排除指定自动装配类
2. **`@ConditionalOnClass`/`@ConditionalOnProperty` 等条件注解**：类级别条件化
3. **`@AutoConfigureBefore`/`@AutoConfigureAfter`/`@AutoConfigureOrder`**：自动装配顺序

但这些机制各有局限：

| 场景 | Spring Boot 能力 | 问题 |
|------|----------------|------|
| 按属性排除自动装配类 | `spring.autoconfigure.exclude=com.foo.BarAutoConfiguration` | 只支持完整类名，不支持通配符或前缀匹配 |
| 按属性前缀存在性条件化 | `@ConditionalOnProperty(name="app.feature.enabled")` | 需要知道确切的属性 key，不能按"前缀存在"条件化 |
| 检测 Web 框架+microsphere 同时可用 | `@ConditionalOnWebApplication` + `@ConditionalOnClass` | 需组合多个注解，重复且冗长 |
| 框架级条件复用 | 无 | 每个自动装配类需重复声明相同的条件组合 |

microsphere-spring-boot 通过**一个过滤器 + 四个组合条件注解**填补这些空白。

---

## 设计：导入过滤器 + 条件注解体系

### 整体架构

```
Spring Boot 自动装配流程
    │
    │  ─── 阶段 1：导入过滤 ───
    │
    ├── ConfigurableAutoConfigurationImportFilter (spring.factories 自动注册)
    │       └── 从 microsphere.autoconfigure.exclude 属性读取排除列表
    │       └── 在 @EnableAutoConfiguration 解析前过滤候选类
    │
    │  ─── 阶段 2：条件评估 ───
    │
    ├── @ConditionalOnPropertyPrefix
    │       └── OnPropertyPrefixCondition: 遍历所有属性名，检查是否有以指定前缀开头的
    │
    ├── @ConditionalOnWebMvcAvailable (组合条件)
    │       @ConditionalOnWebApplication(SERVLET)
    │       @ConditionalOnClass(Servlet + DispatcherServlet + HandlerMethod + microsphere Web API)
    │       @ConditionalOnProperty(microsphere.spring.boot.webmvc.enabled, matchIfMissing=true)
    │
    ├── @ConditionalOnWebFluxAvailable (组合条件)
    │       @ConditionalOnWebApplication(REACTIVE)
    │       @ConditionalOnClass(Flux + DispatcherHandler + HandlerMethod + microsphere WebFlux API)
    │       @ConditionalOnProperty(microsphere.spring.boot.webflux.enabled, matchIfMissing=true)
    │
    ├── @ConditionalOnActuatorEndpointPresent
    │       @ConditionalOnClass(Endpoint)
    │
    └── @ConditionalOnConfigurationProcessorPresent
            @ConditionalOnClass(ConfigurationMetadata)
```

---

### ConfigurableAutoConfigurationImportFilter：属性驱动的排除

#### Spring Boot 的 AutoConfigurationImportFilter 机制

Spring Boot 在 `@EnableAutoConfiguration` 解析阶段会加载 `AutoConfiguration.imports`（或旧版 `spring.factories`）中的所有候选类。在真正导入之前，会调用所有 `AutoConfigurationImportFilter` Bean 的 `match` 方法，对每个候选类返回 `true`（导入）或 `false`（排除）。

**关键时序**：`AutoConfigurationImportFilter` 在 `ConfigurationClassParser` 之前执行，被排除的类**不会**进入条件评估阶段。这比 `@Conditional` 更早、更彻底--被排除的类不会触发类加载、不会执行 `@Conditional` 逻辑、不会注册任何 Bean。

#### microsphere 的实现

```java
public class ConfigurableAutoConfigurationImportFilter
        implements AutoConfigurationImportFilter, EnvironmentAware, Ordered {

    public static final String AUTO_CONFIGURE_EXCLUDE_PROPERTY_NAME = "microsphere.autoconfigure.exclude";

    private Set<String> excludedAutoConfigurationClasses;

    @Override
    public boolean[] match(String[] autoConfigurationClasses,
                           AutoConfigurationMetadata autoConfigurationMetadata) {
        boolean[] results = new boolean[autoConfigurationClasses.length];
        for (int i = 0; i < autoConfigurationClasses.length; i++) {
            results[i] = !isExcluded(autoConfigurationClasses[i]);
        }
        return results;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.excludedAutoConfigurationClasses = getExcludedAutoConfigurationClasses(environment);
    }
}
```

通过 `microsphere.autoconfigure.exclude` 属性指定要排除的类名列表：

```properties
# 排除单个
microsphere.autoconfigure.exclude=com.example.FooAutoConfiguration

# 排除多个
microsphere.autoconfigure.exclude=com.example.FooAutoConfiguration,com.example.BarAutoConfiguration
```

`getExcludedAutoConfigurationClasses` 从两个来源合并排除列表：
1. **`PropertySource` 直接读取**：`environment.getProperty(AUTO_CONFIGURE_EXCLUDE_PROPERTY_NAME)` 获取逗号分隔的类名
2. **`Binder` 绑定**：`Binder.get(environment).bind(AUTO_CONFIGURE_EXCLUDE_PROPERTY_NAME, String[].class)` 支持数组形式的属性

两者合并后去重，存储在 `Set<String>` 中。此外，每个排除类名都会经过 `environment.resolvePlaceholders()` 处理，支持 `${...}` 占位符--例如 `microsphere.autoconfigure.exclude=${my.excluded.classes}`。

#### 与 Spring Boot 原生排除的对比

| 维度 | `spring.autoconfigure.exclude` | `microsphere.autoconfigure.exclude` |
|------|------------------------------|-------------------------------------|
| 配置方式 | `application.properties` | `application.properties` |
| 格式 | 完整类名列表 | 完整类名列表 |
| 生效阶段 | `AutoConfigurationImportSelector` 内部排除 | `AutoConfigurationImportFilter`（在 Selector 内部、`@Conditional` 之前） |
| 与 `@Conditional` 的关系 | 排除的类不进入条件评估 | 排除的类不进入条件评估 |

两者效果类似，但 microsphere 的实现通过 `AutoConfigurationImportFilter` 接口工作，可以在 Spring Boot 原生排除逻辑之外**额外**排除。两者可以同时使用，互不冲突。

---

### @ConditionalOnPropertyPrefix：按属性前缀存在性条件化

#### 问题

Spring Boot 的 `@ConditionalOnProperty` 需要**确切的属性 key**：

```java
// 只检查 app.feature.enabled 是否存在且值为 true
@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")
```

但如果你的条件是"只要 `app.feature` 前缀下有**任意**属性就启用"，`@ConditionalOnProperty` 无法表达。你需要知道所有可能的属性 key，逐一声明 `@ConditionalOnProperty`，或者写自定义 `Condition`。

#### microsphere 的实现

```java
@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Conditional(OnPropertyPrefixCondition.class)
public @interface ConditionalOnPropertyPrefix {
    String[] value();  // 属性前缀，自动补点
}
```

```java
class OnPropertyPrefixCondition extends SpringBootCondition {

    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String[] prefixValues = attributes.getStringArray("value");
        boolean matched = !findPropertyNames(environment, propertyName -> {
            for (String prefix : prefixValues) {
                if (propertyName.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }).isEmpty();
        return matched ? match() : noMatch(...);
    }
}
```

使用示例：

```java
@Configuration
@ConditionalOnPropertyPrefix("app.datasource")  // 只要 app.datasource.* 有任意属性就启用
public class DataSourceAutoConfiguration { ... }
```

**实现要点**：

- 继承 `SpringBootCondition`（而非 Spring 的 `ConfigurationCondition`），获得 `ConditionOutcome` 日志能力
- 使用 `ResolvablePlaceholderAnnotationAttributes`（03-spring 第 6 篇）解析注解属性中的占位符，支持 `@ConditionalOnPropertyPrefix("${my.prefix}")`
- `findPropertyNames` 遍历 Environment 中**所有** PropertySource 的所有属性名，检查是否有以指定前缀开头的。这是一个 O(N×M) 操作（N = 属性总数，M = 前缀数量），但条件评估只在启动时执行一次

---

### @ConditionalOnWebMvcAvailable / @ConditionalOnWebFluxAvailable：组合条件注解

#### 设计

这两个注解是**组合条件**--将多个 `@Conditional` 元注解合为一个：

```java
@ConditionalOnWebApplication(type = SERVLET)
@ConditionalOnClass(name = {
    "jakarta.servlet.Servlet",
    "org.springframework.context.ApplicationContext",
    "org.springframework.web.method.HandlerMethod",
    "org.springframework.web.servlet.DispatcherServlet",
    "io.microsphere.spring.web.method.support.HandlerMethodInterceptor",     // microsphere Web API
    "io.microsphere.spring.webmvc.annotation.EnableWebMvcExtension"          // microsphere WebMVC API
})
@ConditionalOnProperty(name = "microsphere.spring.boot.webmvc.enabled", matchIfMissing = true)
public @interface ConditionalOnWebMvcAvailable {
}
```

`@ConditionalOnWebFluxAvailable` 结构平行，检测 `REACTIVE` 类型 + `Flux` + `DispatcherHandler` + microsphere WebFlux API。

**组合的价值**：

1. **减少重复**：`WebMvcAutoConfiguration` 和 `WebFluxAutoConfiguration` 只需一个注解，而非三行
2. **语义清晰**：`@ConditionalOnWebMvcAvailable` 比 `@ConditionalOnWebApplication(SERVLET) + @ConditionalOnClass(...) + @ConditionalOnProperty(...)` 更易读
3. **统一开关**：`microsphere.spring.boot.webmvc.enabled=false` 可以一键禁用所有 microsphere WebMVC 扩展

#### WebMvcAutoConfiguration：自动装配的实战

```java
@ConditionalOnWebMvcAvailable
@AutoConfigureAfter(name = {
    "org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration",
    "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration"  // Spring Boot 4.0+
})
@EnableWebMvcExtension(registerHandlerInterceptors = true, reversedProxyHandlerMapping = true)
@Import(LoggingConfiguration.class)
public class WebMvcAutoConfiguration {

    @ConditionalOnProperty(name = "...filter.enabled", matchIfMissing = true)
    @Bean
    public ContentCachingFilter contentCachingFilter() { ... }

    @ConditionalOnProperty(name = "...content-negotiation.enabled", matchIfMissing = true)
    @Bean
    public ConfigurableContentNegotiationManagerWebMvcConfigurer ...() { ... }

    @ConditionalOnProperty(name = "...logging.enabled", matchIfMissing = true)
    static class LoggingConfiguration {
        // LoggingMethodHandlerInterceptor, LoggingPageRenderContextHandlerInterceptor, ...
    }
}
```

**设计要点**：
- `@AutoConfigureAfter` 同时列出了 Spring Boot 2.x 和 4.0 的类名，兼容不同版本
- `@EnableWebMvcExtension`（03-spring 第 7/8 篇）激活 microsphere 的 Web 扩展
- 每个 Bean 独立条件化（`@ConditionalOnProperty`），用户可按需禁用

---

### @ConditionalOnActuatorEndpointPresent / @ConditionalOnConfigurationProcessorPresent

这两个注解是简单的 `@ConditionalOnClass` 封装：

```java
@ConditionalOnClass(name = "org.springframework.boot.actuate.endpoint.annotation.Endpoint")
public @interface ConditionalOnActuatorEndpointPresent { }

@ConditionalOnClass(name = "org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata")
public @interface ConditionalOnConfigurationProcessorPresent { }
```

**为什么不用 `@ConditionalOnClass` 直接标注**：组合注解提供了**语义命名**。`@ConditionalOnActuatorEndpointPresent` 比 `@ConditionalOnClass(name="org.springframework.boot.actuate.endpoint.annotation.Endpoint")` 更易读，且如果将来 Actuator API 的类名变化，只需修改注解定义一处。

---

### MonitoredThreadPoolTaskScheduler：Actuator 专用线程池

`ActuatorAutoConfiguration` 注册了一个 `MonitoredThreadPoolTaskScheduler` Bean：

```java
@ConditionalOnBean(type = "io.micrometer.core.instrument.MeterRegistry")
@Bean(name = "actuatorTaskScheduler", destroyMethod = "shutdown")
public ThreadPoolTaskScheduler actuatorTaskScheduler(
        @Value("${...pool-size:1}") int poolSize,
        @Value("${...thread-name-prefix:microsphere-spring-boot-actuator-task-}") String threadNamePrefix) {
    ...
}
```

**设计意图**：Actuator 的端点操作（如 health check、metrics 采集）默认使用 Spring Boot 的 `TaskScheduler`。microsphere 提供了独立的 `actuatorTaskScheduler`，配置为单线程（`poolSize=1`）+ daemon 线程（`setDaemon(true)`，JVM 退出时不阻塞），避免 Actuator 任务干扰业务线程池。仅当 Micrometer `MeterRegistry` 存在时才注册，因为 `MonitoredThreadPoolTaskScheduler` 依赖 Micrometer 做指标监控。

---

## 永恒原理

### 1. 导入过滤 vs 条件评估：两个排除阶段

Spring Boot 的自动装配排除有两个阶段，效果不同：

| 阶段 | 机制 | 效果 |
|------|------|------|
| **导入过滤**（ImportFilter） | `AutoConfigurationImportFilter.match()` | 被排除的类**不进入** `ConfigurationClassParser`，不触发类加载、不执行 `@Conditional` |
| **条件评估**（Condition） | `@Conditional` / `SpringBootCondition` | 被排除的类**已进入** `ConfigurationClassParser`，类已加载，`@Conditional` 已执行，但不注册 Bean |

microsphere 的 `ConfigurableAutoConfigurationImportFilter` 在第一阶段工作，比 `@Conditional` 更早、更彻底。这是为什么它适合排除"完全不需要的框架"--不仅跳过 Bean 注册，还跳过类加载，减少启动时间和内存占用。

### 2. 组合条件注解与语义命名

`@ConditionalOnWebMvcAvailable` 是"组合条件注解"的典型应用--将多个 `@Conditional` 元注解合为一个具有语义名称的注解。这是 Spring 的"元注解"机制的自然延伸：

```
@ConditionalOnWebApplication + @ConditionalOnClass + @ConditionalOnProperty
    └── @ConditionalOnWebMvcAvailable (组合 + 语义命名)
            └── WebMvcAutoConfiguration (使用方)
```

Spring Boot 自身也大量使用这种模式（如 `@ConditionalOnWebApplication` 组合了 `@ConditionalOnClass`）。microsphere 的贡献是将 microsphere 自身的 API 检测纳入组合条件，确保 microsphere Web 扩展只在 microsphere Web API 存在时激活。

### 3. 属性前缀匹配 vs 精确 key 匹配

`@ConditionalOnPropertyPrefix` 与 `@ConditionalOnProperty` 的本质区别是匹配粒度：

- `@ConditionalOnProperty`：**精确 key + 值匹配**（`name="app.feature.enabled", havingValue="true"`）
- `@ConditionalOnPropertyPrefix`：**前缀存在性匹配**（`"app.feature"` -> 任意 `app.feature.*` 属性存在即匹配）

前缀匹配在"配置驱动装配"场景中更有用：当配置属性的前缀存在时，意味着用户已经声明了相关配置，自动装配应该激活。例如 `app.datasource.primary.url` 存在意味着用户配置了主数据源，可以自动装配数据源 Bean。

---

## 边界与反例

### 1. ConfigurableAutoConfigurationImportFilter 的属性读取时机

`setEnvironment` 在 `AutoConfigurationImportFilter` 初始化时调用，此时 Environment 可能尚未加载所有 PropertySource（如 `application-{profile}.yml` 可能晚于 filter 初始化加载）。如果排除列表定义在晚加载的 PropertySource 中，可能被遗漏。

**缓解**：microsphere 的 `getExcludedAutoConfigurationClasses` 同时从 `PropertySource` 和 `Binder` 两个途径读取，`Binder` 会遍历所有已加载的 PropertySource。但无法保证 100% 覆盖。

### 2. OnPropertyPrefixCondition 的性能开销

`findPropertyNames` 遍历所有 PropertySource 的所有属性名，对每个属性名检查是否以指定前缀开头。如果有 500 个属性和 3 个前缀，需要 1500 次 `String.startsWith` 调用。这在启动时通常可接受（< 1ms），但如果 Environment 中有大量 PropertySource（如配置中心推送了数千个属性），开销会增加。

**缓解**：条件评估只在启动时执行一次。如果启动性能敏感，避免在大量自动装配类上使用 `@ConditionalOnPropertyPrefix`。

### 3. @ConditionalOnWebMvcAvailable 的版本兼容

注解中列出了 6 个类名，包括 `jakarta.servlet.Servlet`（Spring Boot 3.x）。如果应用使用 Spring Boot 2.x（`javax.servlet.Servlet`），此条件不匹配，WebMVC 自动装配不激活。

**缓解**：microsphere-spring-boot 设计时已考虑版本兼容，`compatible` 模块提供了 Spring Boot 1.x/2.x 的 API 兼容。但 `@ConditionalOnWebMvcAvailable` 中的 `jakarta.servlet.Servlet` 硬编码了 Spring Boot 3.x 的包名，在 2.x 环境中需要使用旧版本注解或修改类名。

### 4. MonitoredThreadPoolTaskScheduler 的单线程限制

`actuatorTaskScheduler` 默认 `poolSize=1`，意味着 Actuator 的所有定时任务串行执行。如果 Actuator 有多个耗时端点（如 `heapdump`），可能互相阻塞。

**缓解**：通过 `microsphere.spring.boot.actuator.task-scheduler.pool-size` 属性调大线程数。但 Actuator 任务通常不应该耗时，如果端点慢应该优化端点实现而非增加线程。

---

## 现代 Spring Boot（3.x）是否已支持？

| microsphere 特性 | Spring Boot 3.x 是否有等价物 | 说明 |
|------------------|------------------------|------|
| `ConfigurableAutoConfigurationImportFilter` | 部分 | Spring Boot 有 `spring.autoconfigure.exclude`，但 microsphere 提供了额外的排除通道 |
| `@ConditionalOnPropertyPrefix` | 无 | Spring Boot 3.x 无前缀存在性条件注解 |
| `@ConditionalOnWebMvcAvailable` | 部分 | Spring Boot 有 `@ConditionalOnWebApplication(SERVLET)`，但不检测 microsphere API |
| `@ConditionalOnWebFluxAvailable` | 部分 | 同上 |
| `@ConditionalOnActuatorEndpointPresent` | 无 | Spring Boot 无此语义命名（需手写 `@ConditionalOnClass`） |
| `@ConditionalOnConfigurationProcessorPresent` | 无 | 同上 |
| `MonitoredThreadPoolTaskScheduler` | 无 | Spring Boot 3.x 无 Actuator 专用线程池 |

Spring Boot 3.x 的 `@ConditionalOnProperty` 新增了 `prefix` 属性，但语义是"在 prefix 下查找 name 属性"，仍然是精确 key 匹配，不是"prefix 下任意属性存在"的语义。microsphere 的 `@ConditionalOnPropertyPrefix` 填补了这个空白。

---

## 小结

microsphere-spring-boot 的自动装配控制与条件注解扩展，通过**一个导入过滤器 + 四个组合条件注解** 将 Spring Boot 的自动装配控制从"类级别"细化到"属性级别"：

- **`ConfigurableAutoConfigurationImportFilter`**：通过 `microsphere.autoconfigure.exclude` 属性在导入阶段排除自动装配类，比 `@Conditional` 更早、更彻底
- **`@ConditionalOnPropertyPrefix`**：按属性前缀存在性条件化，比 `@ConditionalOnProperty` 的精确 key 匹配更灵活
- **`@ConditionalOnWebMvcAvailable` / `@ConditionalOnWebFluxAvailable`**：组合条件注解，将"Web 应用类型 + 框架 API 存在 + 属性开关"三重条件合为一个语义注解
- **`@ConditionalOnActuatorEndpointPresent` / `@ConditionalOnConfigurationProcessorPresent`**：语义命名的 `@ConditionalOnClass` 封装

这些扩展的共同设计理念是**让自动装配的控制更声明式、更可配置**。用户通过属性就能控制哪些自动装配激活，无需修改代码或添加 `@Conditional` 注解。
