# 05-02 OpenFeign 热更新与装饰器体系

## 问题：Feign Client 配置只能在启动时设置一次

Spring Cloud OpenFeign 的 `@FeignClient` 在应用启动时通过 `FeignClientFactoryBean` 构建 `Feign.Builder`，然后创建代理对象。构建过程中使用的组件（`Contract`、`Decoder`、`Encoder`、`Retryer`、`ErrorDecoder` 等）一旦创建就无法改变。

```java
// Feign Client 配置启动后不可变
@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/users/{id}")
    User getUser(@PathVariable Long id);
}
```

这意味着：如果要修改 `connectTimeout`、`readTimeout`、替换 `Encoder`/`Decoder` 实现、修改 `Contract` 行为，**必须重启应用**。

这在以下场景中尤为不便：

| 场景 | 需求 | 重启的代价 |
|------|------|-----------|
| **配置中心推送** | 修改 `feign.client.config.userservice.connect-timeout` | 几十到上百个实例需要滚动重启 |
| **版本升级** | 更换 JSON 序列化器（Jackson → Fastjson） | 需要部署新版本 |
| **灰度测试** | 对某个服务单独调整超时或重试策略 | 无法实时调整 |
| **故障转移** | 某个服务响应变慢，需要实时调整超时 | 停机意味着服务降级 |

microsphere-spring-cloud 通过 **`FeignComponentRegistry` + `DecoratedFeignComponent` 体系** 实现 Feign Client 组件的运行时热更新。

---

## 设计：Capability 拦截 + 组件注册表 + 配置变更监听

### 整体架构

```
应用启动
    │
    ├── @EnableFeignAutoRefresh → Marker Bean
    │
    ├── FeignClientAutoRefreshAutoConfiguration
    │       ├── FeignComponentRegistry (注册表)
    │       │       └── Map<contextId, List<Refreshable>> 跟踪所有热更新组件
    │       └── FeignClientConfigurationChangedListener (监听配置变更)
    │               └── onApplicationEvent(EnvironmentChangeEvent)
    │                       └── registry.refresh(clientName, changedKeys)
    │
    ├── AutoRefreshCapabilityCustomizer
    │       └── 将 AutoRefreshCapability 注入 FeignClientSpecification
    │
    ├── FeignClient 构建时
    │       └── Feign.Builder 调用 Capability.enrich()
    │           └── AutoRefreshCapability.enrich():
    │                   ├── DecoratedContract(contextId, delegate)  → registry 注册
    │                   ├── DecoratedDecoder(contextId, delegate)   → registry 注册
    │                   ├── DecoratedEncoder(contextId, delegate)   → registry 注册
    │                   ├── DecoratedErrorDecoder(...)              → registry 注册
    │                   ├── DecoratedRetryer(...)                   → registry 注册
    │                   └── DecoratedQueryMapEncoder(...)           → registry 注册
    │
    └── 配置变更时
            └── EnvironmentChangeEvent (由 Spring Cloud Config / Nacos 配置推送触发)
                └── FeignClientConfigurationChangedListener
                    └── 根据 changedKeys 解析受影响的 clientName
                        └── FeignComponentRegistry.refresh(clientName, changedKeys)
                            └── List<Refreshable>.refresh()
                                └── DecoratedFeignComponent.refresh()
                                    ├── 从 NamedContextFactory 重新获取配置
                                    └── 创建新组件实例替换 delegate
```

---

### EnableFeignAutoRefresh：启用注解

```java
@Target(TYPE)
@Retention(RUNTIME)
@Import(EnableFeignAutoRefresh.Marker.class)
public @interface EnableFeignAutoRefresh {

    class Marker {
        // 仅作为 @ConditionalOnBean 的标记
    }
}
```

`@Import(Marker.class)` 注册了一个空 `Marker` Bean。`FeignClientAutoRefreshAutoConfiguration` 通过 `@ConditionalOnBean(Marker.class)` 条件化激活--只有标注了 `@EnableFeignAutoRefresh` 的应用才会加载 Feign 热更新配置。

---

### FeignComponentRegistry：组件注册表

```java
public class FeignComponentRegistry {

    // 配置属性 key → Feign 组件类型的映射（9 个条目）
    private static final Map<String, Class<?>> configComponentMappings = ofMap(
            "retryer", Retryer.class,
            "error-decoder", ErrorDecoder.class,
            "request-interceptors", RequestInterceptor.class,
            "default-request-headers", RequestInterceptor.class,    // 默认请求头也是拦截器
            "default-query-parameters", RequestInterceptor.class,   // 默认查询参数也是拦截器
            "decoder", Decoder.class,
            "encoder", Encoder.class,
            "contract", Contract.class,
            "query-map-encoder", QueryMapEncoder.class
    );

    // contextId → 该 Feign Client 的所有 Refreshable 组件
    private final Map<String, List<Refreshable>> refreshableComponents = newConcurrentHashMap(32);

    // contextId → 聚合的请求拦截器
    private final Map<String, CompositedRequestInterceptor> interceptorsMap = newConcurrentHashMap(32);


    public void register(String contextId, Refreshable refreshable) {
        refreshableComponents.computeIfAbsent(contextId, k -> newLinkedList()).add(refreshable);
    }

    public void refresh(String contextId, Set<String> changedKeys) {
        List<Refreshable> refreshables = refreshableComponents.get(contextId);
        if (refreshables == null) return;

        // 根据 changedKeys 确定需要刷新的组件类型
        Set<Class<?>> affectedTypes = resolveAffectedComponentTypes(changedKeys);

        for (Refreshable refreshable : refreshables) {
            if (affectedTypes.contains(refreshable.componentType())) {
                refreshable.refresh();  // 只刷新受影响的组件
            }
        }
    }
}
```

**`configComponentMappings`** 将配置属性 key 映射到组件类型。例如：
- 变更 `feign.client.config.userservice.retryer` → `Retryer.class`
- 变更 `feign.client.config.userservice.contract` → `Contract.class`

**精炼刷新**：不是所有组件都刷新，只刷新受影响的。`resolveAffectedComponentTypes` 根据 `changedKeys`（`EnvironmentChangeEvent` 中的属性名列表）解析受影响的组件类型。

---

### AutoRefreshCapability：逐个组件 enrich

Feign 12+ 的 `Capability` 接口为每个 Feign 组件类型定义了独立的 `enrich(Component)` 方法。`AutoRefreshCapability` 实现这些方法，将每个组件包装为 `Decorated*` 并注册到 Registry：

```java
public class AutoRefreshCapability implements Capability, ApplicationContextAware {

    @Override
    public Contract enrich(Contract contract) {
        if (contract == null) return null;
        DecoratedContract decorated = instantiate(DecoratedContract.class, Contract.class,
            contextId, contextFactory, clientProperties, contract);
        this.componentRegistry.register(contextId, decorated);
        return decorated;
    }

    @Override
    public Decoder enrich(Decoder decoder) {
        if (decoder == null) return null;
        DecoratedDecoder decorated = instantiate(DecoratedDecoder.class, Decoder.class,
            contextId, contextFactory, clientProperties, decoder);
        this.componentRegistry.register(contextId, decorated);
        return decorated;
    }

    // Encoder, ErrorDecoder, Retryer, QueryMapEncoder, RequestInterceptor 类似...
    // 每个方法：null 检查 → 创建 Decorated* → 注册 → 返回包装对象
}
```

每个 `enrich` 方法的流程：
1. **null 检查**：如果组件为 null（未配置），返回 null（不包装）
2. **创建 `Decorated*`**：通过 `DecoratedFeignComponent.instantiate()` 创建包装器
3. **注册**：将包装器注册到 `FeignComponentRegistry`，以 `contextId` 为 key
4. **返回包装器**：返回 `Decorated*` 实例替换原始组件

`contextId` 在 `setApplicationContext()` 中解析获得（即 `@FeignClient` 的名称）。

**`AutoRefreshCapabilityCustomizer`** 通过 `SpecificationCustomizer` SPI 将 `AutoRefreshCapability.class` 注入到每个 `FeignClientSpecification` 的配置类列表中。这样 `FeignClientFactoryBean` 在构建 Feign Builder 时会自动创建 `AutoRefreshCapability` 实例并调用其 `enrich` 方法。

---

### DecoratedFeignComponent：可刷新组件基类

```java
public abstract class DecoratedFeignComponent<T> implements Refreshable {

    protected volatile T delegate;  // volatile 保证多线程可见

    @Override
    public void refresh() {
        this.delegate = null;  // 标记失效，下次访问时懒加载
    }

    public T delegate() {
        T delegate = this.delegate;
        if (delegate == null) {
            delegate = loadInstance();  // 从 NamedContextFactory 重新获取配置并创建
            this.delegate = delegate;
        }
        return delegate;
    }

    protected abstract Class<? extends T> componentType();
}
```

**`volatile T delegate`**：保证 `refresh()` 中对 `delegate` 的 null 写入对后续调用可见。

**懒加载模式**：`refresh()` 将 delegate 置为 null，而非立即创建新组件。下次方法调用时（如 `DecoratedContract.parseAndValidateMetadata()`），通过 `delegate()` 检查到 delegate 为 null，调用 `loadInstance()` 创建新实例。

`loadInstance()` 内部通过 `loadInstanceFromContextFactory()` 调用 `contextFactory.getProvider(contextId, componentType).getIfAvailable(() -> instantiateClass(componentType))`--先从 `NamedContextFactory`（Feign 客户端的子 ApplicationContext）中获取 Bean 实例，如果不存在则通过无参构造器直接实例化。这是"先查 Bean，后回退实例化"的双重保障。

**与立即创建模式的区别**：
- 立即创建：`refresh()` 时完成所有创建，第一次调用无额外延迟 ✅ 预热
- 懒加载：`refresh()` 只设 null，下次调用时创建，第一次调用有延迟 ⚠️

microsphere 选择了懒加载模式，因为：
1. `loadInstance()` 会从子 ApplicationContext 中查找 Bean，在配置刚变更时这些 Bean 可能尚未创建完成
2. 懒加载将 Bean 创建延迟到实际使用时，避免不必要的创建开销

**具体实现**：

| 类 | 组件类型 | refresh 行为 |
|----|---------|-------------|
| `DecoratedContract` | `Contract` | 从 `FeignClientConfiguration.getContract()` 重新获取 |
| `DecoratedDecoder` | `Decoder` | 从 `FeignClientConfiguration.getDecoder()` 重新获取 |
| `DecoratedEncoder` | `Encoder` | 从 `FeignClientConfiguration.getEncoder()` 重新获取 |
| `DecoratedErrorDecoder` | `ErrorDecoder` | 从 `FeignClientConfiguration.getErrorDecoder()` 重新获取 |
| `DecoratedRetryer` | `Retryer` | 从 `FeignClientConfiguration.getRetryer()` 重新获取 |
| `DecoratedQueryMapEncoder` | `QueryMapEncoder` | 从 `FeignClientConfiguration.getQueryMapEncoder()` 重新获取 |

每个 `Decorated*` 类都：

1. 扩展 `DecoratedFeignComponent<T>`
2. 实现 `componentType()` 返回组件类型
3. 实现 `createDelegate(config)` 从新的配置创建组件实例
4. 实现原始组件接口（如 `Contract`、`Decoder`），所有方法委托给 `delegate`

例如 `DecoratedContract`：

```java
public class DecoratedContract extends DecoratedFeignComponent<Contract> implements Contract {

    public DecoratedContract(String contextId, ..., Contract delegate) {
        super(contextId, contextFactory, clientProperties, delegate);
    }

    @Override
    protected Class<? extends Contract> componentType() {
        Class<Contract> contractClass = get(FeignClientConfiguration::getContract);
        return contractClass == null ? Contract.class : contractClass;
    }

    @Override
    public List<MethodMetadata> parseAndValidateMetadata(Class<?> targetType) {
        return delegate.parseAndValidateMetadata(targetType);  // 委托给当前 delegate
    }
}
```

---

### FeignClientConfigurationChangedListener：配置变更事件监听

```java
public class FeignClientConfigurationChangedListener
        implements ApplicationListener<EnvironmentChangeEvent> {

    private final FeignComponentRegistry registry;
    private final String PREFIX = "spring.cloud.openfeign.client.config.";

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        Map<String, Set<String>> effectiveClients = resolveChangedClient(event);
        if (!effectiveClients.isEmpty()) {
            effectiveClients.forEach(registry::refresh);
        }
    }
}
```

**`resolveChangedClient`** 从 `EnvironmentChangeEvent` 的 `changedKeys` 中提取受影响的 Feign Client 名称：

```
changedKeys 示例：
  spring.cloud.openfeign.client.config.userservice.connect-timeout
  spring.cloud.openfeign.client.config.userservice.read-timeout

→ resolved: { "userservice" → {"connect-timeout", "read-timeout"} }
```

**注册时机**：通过 `@EventListener(ApplicationReadyEvent.class)` 在 `ApplicationReadyEvent` 时注册。比 `ConfigurationPropertiesRebinder` 晚，确保 `@RefreshScope` 的 Bean 先刷新完成后，Feign 组件再刷新。

---

### AutoRefreshCapabilityCustomizer：自动注入

```java
public class AutoRefreshCapabilityCustomizer implements SpecificationCustomizer {

    @Override
    public void customize(Specification specification, String beanName) {
        if (specification instanceof FeignClientSpecification && beanName.startsWith("default.")) {
            injectAutoRefreshCapability((FeignClientSpecification) specification);
        }
    }

    void injectAutoRefreshCapability(FeignClientSpecification specification) {
        Class<?>[] configClasses = specification.getConfiguration();
        Class<?>[] newConfigClasses = combine(AutoRefreshCapability.class, configClasses);
        specification.setConfiguration(newConfigClasses);
    }
}
```

**为什么需要 Customizer**：`FeignClientSpecification` 在 `NamedContextFactory` 初始化时创建。microsphere 通过 `SpecificationCustomizer` SPI（第 3 篇介绍）拦截这个初始化过程，将 `AutoRefreshCapability.class` 插入到配置类列表中。这样 `AutoRefreshCapability` 成为所有 Feign Client 的默认配置。

---

### 完整流程

```java
@EnableFeignAutoRefresh
@EnableFeignClients
@SpringBootApplication
public class Application { }
```

1. 启动时 `AutoRefreshCapabilityCustomizer` 将 `AutoRefreshCapability` 注入所有 Feign Client 配置
2. 每个 `@FeignClient` 构建时，`AutoRefreshCapability.enrich()` 将 Contract/Decoder/Encoder 等包装为 `Decorated*` 并注册到 `FeignComponentRegistry`
3. 运行时 Nacos/Spring Cloud Config 推送配置变更，发布 `EnvironmentChangeEvent`
4. `FeignClientConfigurationChangedListener` 解析受影响的 clientName，调用 `registry.refresh("userservice", {"connect-timeout"})`
5. `FeignComponentRegistry` 找到 `userservice` 下所有 `Refreshable`，只刷新类型匹配的组件
6. `DecoratedFeignComponent.refresh()` 从新配置创建新组件实例，原子替换 `volatile delegate`

---

## 永恒原理

### 1. Feign Capability API 作为组件级拦截入口

Feign 12+ 的 `Capability` 接口为每个组件类型定义了独立的 `enrich(Component)` 方法。`Feign.Builder` 在构建时会依次调用每个 `Capability` 的这些方法，允许外部代码包装或替换组件：

```java
// Feign 12+ 的 Capability 接口
public interface Capability {
    default Contract enrich(Contract contract) { return contract; }
    default Encoder enrich(Encoder encoder) { return encoder; }
    default Decoder enrich(Decoder decoder) { return decoder; }
    default Retryer enrich(Retryer retryer) { return retryer; }
    default ErrorDecoder enrich(ErrorDecoder decoder) { return decoder; }
    default RequestInterceptor enrich(RequestInterceptor interceptor) { return interceptor; }
    default QueryMapEncoder enrich(QueryMapEncoder encoder) { return encoder; }
}
```

`AutoRefreshCapability` 分别实现每个 `enrich` 方法，创建 `Decorated*` 包装器并注册。这种方式相比于一次性包装整个 `Feign.Builder` 更灵活--每个组件可以被单独替换或忽略。

### 2. volatile + 懒加载 vs 同步锁

`DecoratedFeignComponent.delegate` 使用 `volatile` 保证可见性，`refresh()` 时设为 null，下次访问时通过 `delegate()` 懒加载。这避免了对每个委托方法加 `synchronized` 的性能开销：

```java
// 无锁委托（利用 volatile 的 happens-before 语义）
@Override
public List<MethodMetadata> parseAndValidateMetadata(Class<?> targetType) {
    return delegate().parseAndValidateMetadata(targetType);  // delegate() 内部做懒加载
}
```

`volatile` 配合懒加载的设计权衡：
- 优点：`refresh()` 开销极低（只是一个 null 赋值），Feign 方法调用不需要同步
- 代价：刷新后的第一次调用会触发 `loadInstance()` 的查找和创建开销
- 适用场景：Feign 组件通常是长时间稳定使用的，配置变更远少于方法调用，符合"读远远多过写"的特征

### 3. 精炼刷新 vs 全量刷新

`FeignComponentRegistry.refresh()` 根据 `changedKeys` 解析受影响的组件类型，只刷新需要更新的组件。这种"精炼刷新"比全量刷新更高效：

- 变更 `connect-timeout`：只刷新 `Retryer`
- 变更 `contract`：只刷新 `Contract`
- 变更 `decoder`：只刷新 `Decoder`

`configComponentMappings` 将属性 key 映射到组件类型，实现"精确制导"。

### 4. NamedContextFactory 与 per-client 配置隔离

Spring Cloud OpenFeign 使用 `NamedContextFactory` 为每个 `@FeignClient` 创建独立的子 ApplicationContext。每个客户端有自己的配置。microsphere 的 `DecoratedFeignComponent` 利用这个机制：

- `contextId`：Feign 客户端名称（`"userservice"`）
- `contextFactory.getContext(contextId)`：获取该客户端对应的子 ApplicationContext
- `FeignClientConfiguration`：从子 ApplicationContext 的 Environment 中读取配置

这使得刷新只影响指定客户端，不同客户端之间配置隔离。

---

## 边界与反例

### 1. Capability API 的版本依赖

`Capability` 是 Feign 12+ 的 API。如果项目使用 Feign 11 或更早版本，`AutoRefreshCapability` 无法工作。Spring Cloud 2023.0.x 已使用 Feign 12+，但自定义项目可能依赖旧版本。

**缓解**：通过 `@ConditionalOnClass(name = "feign.Capability")` 自动检测。

### 2. FeignComponentRegistry 的线程安全

`refreshableComponents` 使用 `ConcurrentHashMap`，但 `refresh()` 的"查找 + 迭代"操作不是原子操作。如果在迭代期间有新的 `@FeignClient` 初始化并注册到 Registry，新的组件不会被本次刷新覆盖。

**缓解**：`ConcurrentHashMap` 的 `computeIfAbsent` 和 `get` 都是原子的。注册和刷新是"写后读"关系--新注册的组件有最新的配置，不需要被本次刷新覆盖。

### 3. DecoratedContract 的 Feign 版本兼容性

`DecoratedContract` 直接实现 `feign.Contract` 接口。如果 Feign 版本变化导致 `Contract` 接口新增方法，`DecoratedContract` 需要在编译时同步更新。否则实现 `Contract` 的类会因缺少方法而编译失败。

**缓解**：Feign 的 `Contract` 接口相对稳定（`parseAndValidateMetadata` 是唯一需要实现的方法），版本变化的概率较低。

### 4. EnvironmentChangeEvent 的触发范围

`FeignClientConfigurationChangedListener` 监听 `EnvironmentChangeEvent`，但此事件由 Spring Cloud Config 或 Nacos 配置推送触发时才发布。如果配置是通过 `application.properties` 文件修改后重启应用生效的（非配置中心模式），不会触发 `EnvironmentChangeEvent`，Feign 组件不会刷新。

**缓解**：手动调用 `context.publishEvent(new EnvironmentChangeEvent(changedKeys))` 可以模拟配置变更事件。

### 5. AutoRefreshCapabilityCustomizer 仅在 default. 前缀下生效

`AutoRefreshCapabilityCustomizer.customize()` 仅当 `beanName.startsWith("default.")` 时注入 `AutoRefreshCapability`。这意味着只有默认 Feign 配置会被注入，非默认的客户端配置不会被自动注入。如果用户通过 `@FeignClient(configuration = MyConfig.class)` 指定了自定义配置，需要手动添加 `AutoRefreshCapability`。

---

## 现代 Spring Cloud 是否已支持？

| microsphere 特性 | Spring Cloud 是否有等价物 | 说明 |
|------------------|------------------------|------|
| `AutoRefreshCapability`（Feign 组件热更新） | 无 | Spring Cloud 无此功能 |
| `DecoratedFeignComponent` 体系 | 无 | Spring Cloud 无装饰器组件 |
| `FeignComponentRegistry`（组件注册表） | 无 | Spring Cloud 无组件注册表 |
| `FeignClientConfigurationChangedListener` | 无 | Spring Cloud 无 Feign 配置变更监听 |

Spring Cloud 2023.0.x 的 `RefreshAutoConfiguration` + `@RefreshScope` 支持 `@ConfigurationProperties` 的刷新，但 `@FeignClient` 的配置不是 `@ConfigurationProperties`，不受 `@RefreshScope` 保护。Feign 组件的热更新需要额外的定制开发。

---

## 小结

microsphere-spring-cloud 的 OpenFeign 热更新体系，通过 **Feign Capability 拦截 + 装饰器组件 + 组件注册表 + 配置变更监听** 四层设计，实现了 Feign Client 组件的运行时热更新。

核心组件：
- **`AutoRefreshCapability`**：利用 Feign 12+ `Capability` API，在 Feign Builder 构建时拦截并包装所有组件
- **`DecoratedFeignComponent<T>`**：抽象基类，`volatile delegate` + `refresh()` 原子替换
- **6 个 `Decorated*` 实现**：`Contract`/`Decoder`/`Encoder`/`ErrorDecoder`/`Retryer`/`QueryMapEncoder`
- **`FeignComponentRegistry`**：跟踪所有 `Refreshable` 组件，按 `changedKeys` 精炼刷新
- **`FeignClientConfigurationChangedListener`**：监听 `EnvironmentChangeEvent`，解析受影响的 Feign Client
- **`AutoRefreshCapabilityCustomizer`**：通过 `SpecificationCustomizer` SPI 自动注入 Capability

精炼刷新机制确保只有受影响的组件被刷新，而非全量重建。`volatile` 原子替换保证刷新线程安全，不阻塞 Feign 方法调用。
