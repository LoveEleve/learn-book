# 05-03 基础设施扩展（Features/LB/FaultTolerance/NamedContext）

## 问题：Spring Cloud 基础设施的六个长尾空白

前两篇覆盖了 Service Registry 和 OpenFeign 两大部分。本篇覆盖 commons 模块中的其余扩展，它们各自解决一个独立的痛点：

| 扩展 | Spring Cloud 的空白 | 解决方式 |
|------|-------------------|---------|
| **Features 特性管理** | `HasFeatures`/`FeaturesEndpoint` 不支持声明式条件化 | `@ConditionalOnFeaturesAvailable` + `FeaturesProperties` 属性驱动的特性开关 |
| **ReactiveDiscoveryClient 适配** | 响应式和非响应式 `DiscoveryClient` 不互通 | `ReactiveDiscoveryClientAdapter` 适配器 |
| **Specification 定制 SPI** | `NamedContextFactory.Specification` 初始化后不可变 | `SpecificationCustomizer` + `BeanPostProcessor` 拦截 |
| **Tomcat 动态配置** | `Connector` 参数（maxThreads、timeout、compression）重启才能改 | `TomcatDynamicConfigurationListener` 监听 `EnvironmentChangeEvent` |
| **Registration 元数据同步** | 多注册中心时元数据不同步 | `RegistrationMetaData` 同步 put/remove/clear 到所有 Registration |
| **加权轮询负载均衡** | Spring Cloud LoadBalancer 的轮询不支持权重 | `WeightedRoundRobin` + warmup 算法 |

---

## 设计：六个独立组件

### Features 特性管理

#### 问题

Spring Cloud 的 `HasFeatures` 和 `FeaturesEndpoint` 允许声明特性，但声明方式是编程式的（`@Bean` 返回 `HasFeatures` 实例），不支持条件化激活。微服务架构中，特性开关需要声明式配置（运维人员通过配置中心推送即可），而非代码修改。

#### 设计

**`FeaturesProperties`** 是一个 `@ConfigurationProperties(prefix = "microsphere.spring.cloud.features")`，绑定两个 Map：

- `abstract`：`Map<String, List<String>>` -- 模块名 → 特性名列表
- `named`：`Map<String, Map<String, String>>` -- 模块名 → {key → value} 映射

```properties
# 声明抽象特性
microsphere.spring.cloud.features.abstract.jdbc=my-jdbc-feature

# 声明命名特性
microsphere.spring.cloud.features.named.mymodule.key1=value1
```

**`ConfigurationPropertyHasFeaturesAutoConfiguration`**（249 行）实现 `BeanFactoryAware`、`BeanClassLoaderAware`、`InitializingBean`，通过 `@EnableConfigurationProperties(FeaturesProperties.class)` 绑定配置属性。在 `afterPropertiesSet()` 中：

1. 从 `FeaturesProperties` 读取 `abstract` 和 `named` 配置
2. 为每个 abstract 特性创建 `HasFeatures` Bean（通过 `beanFactory.registerSingleton()` 直接注册）
3. 为每个 named 特性创建 `NamedFeature` Bean（通过 `BeanDefinitionBuilder` + `BeanDefinitionRegistry`）
4. 按 `NamedFeatureComparator` 排序 named 特性

**`@ConditionalOnFeaturesAvailable`** 组合了两个条件：
- `@ConditionalOnFeaturesEnabled`：检查 `microsphere.spring.cloud.features.enabled`（默认 `true`）
- `@ConditionalOnAvailableEndpoint`：检查 `FeaturesEndpoint` 是否可用

**`NamedFeatureComparator`** 实现 `Comparator<NamedFeature>`，按名称比较，用于 `NamedFeature` 列表排序。

**`FeaturesUtils`** 提供工具方法生成配置属性名：
- `getAbstractFeaturePropertyName("jdbc")` → `microsphere.spring.cloud.features.abstract.jdbc`
- `getNamedFeaturePropertyName("mymodule", "key1")` → `microsphere.spring.cloud.features.named.mymodule.key1`

---

### ReactiveDiscoveryClientAdapter：响应式发现客户端适配

#### 设计

Spring Cloud 有两种发现客户端接口：

| 接口 | 返回类型 | 适用场景 |
|------|---------|---------|
| `DiscoveryClient` | `List<ServiceInstance>`（阻塞） | 传统 Servlet 应用 |
| `ReactiveDiscoveryClient` | `Flux<ServiceInstance>`（响应式） | WebFlux 应用 |

microsphere 的 `ReactiveDiscoveryClientAdapter` 将 `ReactiveDiscoveryClient` 适配为阻塞式 `DiscoveryClient`：

```java
public class ReactiveDiscoveryClientAdapter implements DiscoveryClient {

    private final ReactiveDiscoveryClient reactiveDiscoveryClient;

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        return execute(() -> reactiveDiscoveryClient.getInstances(serviceId)
                .collectList()
                .block());
    }
}
```

**`DiscoveryClientAutoConfiguration`**（92 行）注册 `UnionDiscoveryClient` 为 Primary `DiscoveryClient`：

```java
@Configuration
@ConditionalOnBlockingDiscoveryAvailable
@AutoConfigureBefore(CommonsClientAutoConfiguration.class)
public class DiscoveryClientAutoConfiguration {

    @ConditionalOnProperty(UNION_DISCOVERY_CLIENT_ENABLED_PROPERTY_NAME, matchIfMissing = true)
    @Bean
    @Primary  // 替换 Spring Cloud 默认的 DiscoveryClient
    public UnionDiscoveryClient unionDiscoveryClient() {
        return new UnionDiscoveryClient();
    }
}
```

**`ReactiveDiscoveryClientAutoConfiguration`**（76 行）注册 `ReactiveDiscoveryClientAdapter`：

```java
@Configuration
@ConditionalOnReactiveDiscoveryAvailable
@AutoConfigureAfter(SimpleReactiveDiscoveryClientAutoConfiguration.class)
public class ReactiveDiscoveryClientAutoConfiguration {

    @Bean
    @ConditionalOnBean(ReactiveDiscoveryClient.class)
    public ReactiveDiscoveryClientAdapter reactiveDiscoveryClientAdapter(
            ReactiveDiscoveryClient reactiveDiscoveryClient) {
        return new ReactiveDiscoveryClientAdapter(reactiveDiscoveryClient);
    }
}
```

**`AutoConfigureBefore(CommonsClientAutoConfiguration.class)`** 确保 `UnionDiscoveryClient` 在 Spring Cloud 默认的 `DiscoveryClient` 之前注册，`@Primary` 使其成为默认的 `DiscoveryClient` Bean。

---

### RegistrationMetaData：跨注册中心元数据同步

#### 问题

`MultipleRegistration` 持有多个 `Registration` 实例（Nacos、Eureka、Consul）。如果用户修改了 `Registration.getMetadata()`，只有被修改的 `Registration` 实例会更新，其他注册中心不会同步。

#### 设计

```java
public final class RegistrationMetaData implements Map<String, String> {

    private final Map<String, String> applicationMetaData;  // 应用级元数据
    private final Collection<Registration> registrations;   // 所有 Registration
    private final Object lock = new Object();

    @Override
    public String put(String key, String value) {
        synchronized (lock) {
            // 同步到所有注册中心
            this.registrations.forEach(registration -> {
                setMetadata(registration, key, value);
            });
        }
        return this.applicationMetaData.put(key, value);
    }

    // remove / clear / putAll 类似，都是 synchronized + 遍历所有 Registration
}
```

**关键设计点**：

- **`synchronized(lock)`**：所有写操作（`put`/`remove`/`clear`/`putAll`）加锁，保证多线程环境下元数据一致性
- **`applicationMetaData`**：本地存储元数据副本，`get` 操作直接返回本地值，无需访问 `Registration`
- **Zookeeper 特殊处理**（324 行中约 100 行）：`ZookeeperRegistration` 的 `getServiceInstance()` 方法不是直接可访问的，需要通过反射调用 `invokeMethod(registration, "getServiceInstance")` 获取 `ServiceInstance` 对象，再修改其 metadata

**`ServiceInstanceUtils`**（254 行）：提供 `ServiceInstance` 的 URI 构建、元数据设置/移除、JSON 序列化工具方法。`setMetadata` 和 `removeMetadata` 方法被 `RegistrationMetaData` 调用。

---

### SpecificationCustomizer：NamedContextFactory 规范定制

#### 问题

Spring Cloud OpenFeign 和 LoadBalancer 使用 `NamedContextFactory` 为每个客户端创建独立的子 ApplicationContext。每个子 Context 由 `Specification` 描述（包含配置类列表）。但 `Specification` 在创建后无法修改。

#### 设计

```java
public interface SpecificationCustomizer {
    void customize(Specification specification, String beanName);
}
```

`SpecificationBeanPostProcessor` 继承 `GenericBeanPostProcessorAdapter<Specification>`（03-spring 第 4 篇），在 `processAfterInitialization` 中调用所有 `SpecificationCustomizer`：

```java
public class SpecificationBeanPostProcessor
        extends GenericBeanPostProcessorAdapter<Specification> implements BeanFactoryAware {

    private List<SpecificationCustomizer> customizers;

    @Override
    protected void processAfterInitialization(Specification bean, String beanName) {
        customizers.forEach(customizer -> customizer.customize(bean, beanName));
    }
}
```

**发现机制**：`setBeanFactory` 时通过 `getSortedBeans(beanFactory, SpecificationCustomizer.class)` 获取所有自定义器，按 `Ordered` 排序。

**`SpecificationAutoConfiguration`** 的自动装配：

```java
@Configuration
@ConditionalOnClass(name = "org.springframework.cloud.context.named.NamedContextFactory")
@AutoConfigureAfter(name = {
    "org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration",
    "org.springframework.cloud.openfeign.FeignAutoConfiguration"
})
public class SpecificationAutoConfiguration {

    @Bean
    @ConditionalOnBean(Specification.class)
    public SpecificationBeanPostProcessor specificationBeanPostProcessor() {
        return new SpecificationBeanPostProcessor();
    }
}
```

**`@AutoConfigureAfter` 在 LoadBalancer 和 Feign 之后**，确保 `Specification` Bean 已经被创建后再注册 `SpecificationBeanPostProcessor`。

**实际应用**：第 2 篇的 `AutoRefreshCapabilityCustomizer` 实现了 `SpecificationCustomizer`，在 `FeignClientSpecification` 初始化时将 `AutoRefreshCapability` 注入配置类列表。

---

### WeightedRoundRobin：加权轮询 + 预热权重

#### 数据结构

```java
public class WeightedRoundRobin {

    private final String id;              // 服务实例 ID
    private volatile int weight;          // 权重（volatile 允许动态调整）
    LongAdder current = new LongAdder();  // 当前计数器（LongAdder 比 AtomicLong 高并发友好）
    private volatile long lastUpdate;     // 最后更新时间
}
```

**`LongAdder` 而非 `AtomicLong`**：`LongAdder` 内部使用 Cell 数组减少 CAS 竞争，在大量并发更新时性能优于 `AtomicLong`。加权轮询场景中，每次请求都会增加 `current` 计数器，并发量可能很高。

#### 预热权重算法

`LoadBalancerUtils.calculateWarmupWeight` 来自 Dubbo 的预热算法：

```java
public static int calculateWarmupWeight(long uptime, long warmup, int weight) {
    int ww = (int) Math.round(Math.pow((uptime / (double) warmup), 2) * weight);
    return ww < 1 ? 1 : Math.min(ww, weight);
}
```

公式 `(uptime / warmup)² × weight` 产生抛物线增长：
- 预热开始时（`uptime=0`）：权重 = 1（最小）
- 预热 50% 时（`uptime=warmup/2`）：权重 = 0.25 × weight
- 预热结束时（`uptime=warmup`）：权重 = weight（满）

**为什么用抛物线而非线性**：新启动的应用需要时间完成 JIT 编译、缓存预热和连接池初始化。前期负载能力极弱，线性增长可能过早暴露。抛物线在前期几乎不增长，后期快速增长，更符合真实负载能力的变化。

---

### TomcatDynamicConfigurationListener：动态 Tomcat 配置

#### 问题

Tomcat 的 `Connector` 参数（`maxThreads`、`connectionTimeout`、`maxConnections`、`compression`、`maxHttpHeaderSize` 等）在 `ServerProperties` 中配置，应用启动后不可修改。流量突增时需要临时调整线程池大小。

#### 设计

`TomcatDynamicConfigurationListener` 是本文最复杂的组件（320 行），构造函数注入 `TomcatWebServer`、`ServerProperties` 和 `ConfigurableApplicationContext`：

```java
public class TomcatDynamicConfigurationListener
        implements ApplicationListener<EnvironmentChangeEvent> {

    private final TomcatWebServer tomcatWebServer;
    private final ServerProperties serverProperties;
    private volatile ServerProperties currentServerProperties;  // 快照，用于对比
    private final boolean configurationPropertiesRebinderPresent;  // @RefreshScope 是否存在

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        if (!isSourceFrom(event)) return;  // 只处理当前 ApplicationContext 的事件
        Set<String> serverPropertyNames = filterServerPropertyNames(event);
        if (serverPropertyNames.isEmpty()) return;
        configureTomcatIfChanged(serverPropertyNames);
    }
}
```

**`isSourceFrom(event)`**：通过 `context.equals(event.getSource())` 判断事件来源。与 03-spring 第 1 篇的 `OnceApplicationContextEventListener` 和 04-spring-boot 第 3 篇的 `OnceApplicationPreparedEventListener` 设计一致，防止父子容器重复处理。

**获取刷新配置**：

```java
private ServerProperties getRefreshableServerProperties(Set<String> serverPropertyNames) {
    if (configurationPropertiesRebinderPresent) {
        // @RefreshScope 存在时：ServerProperties 已被自动刷新
        return serverProperties;
    } else {
        // @RefreshScope 不存在时：手动绑定变更的属性
        Map<String, String> serverProperties = getProperties(environment, serverPropertyNames);
        return bind(serverProperties, "server", ServerProperties.class);
    }
}
```

**双模式适配**：
- **`@RefreshScope` 模式**：`ConfigurationPropertiesRebinder` 已自动刷新 `ServerProperties` Bean，直接使用
- **手动绑定模式**：从 Environment 提取变更的属性，用 `BindUtils.bind()` 绑定到 `ServerProperties` 对象

**配置变更检测与增量更新**：

```java
private void configureProtocol(ServerProperties refreshable, ProtocolHandler handler) {
    if (handler instanceof AbstractProtocol protocol) {
        ServerProperties.Tomcat refreshableTomcat = refreshable.getTomcat();
        ServerProperties.Tomcat currentTomcat = currentServerProperties.getTomcat();

        // 线程池核心线程数
        configure("Tomcat thread pool's core size")
            .value(refreshableTomcatProperties.getThreads()::getMinSpare)
            .on(this::isPositive)
            .compare(currentTomcatProperties.getThreads()::getMinSpare)
            .apply(protocol::setMinSpareThreads);

        // 线程池最大线程数
        configure("Tomcat thread pool's max size")
            .value(refreshableTomcatProperties.getThreads()::getMax)
            .on(this::isPositive)
            .compare(currentTomcatProperties.getThreads()::getMax)
            .apply(protocol::setMaxThreads);

        // 连接超时
        configure("Tomcat connection timeout(ms)")
            .value(refreshableTomcat::getConnectionTimeout)
            .compare(currentTomcat::getConnectionTimeout)
            .as(this::toIntMillis)
            .on(this::isPositive)
            .apply(protocol::setConnectionTimeout);

        // 最大连接数
        configure("Tomcat max connections")
            .value(refreshableTomcat::getMaxConnections)
            .compare(currentTomcat::getMaxConnections)
            .on(this::isPositive)
            .apply(protocol::setMaxConnections);
    }
}
```

**`configure()` 流畅 API 的链式调用**：

```
configure("name")
  .value(newSupplier)    // 提供新值
  .on(condition)         // 前置条件检查（如 isPositive 确保值合法）
  .compare(oldSupplier)  // 提供旧值引用
  .as(converter)         // 类型转换（如 Duration → int）
  .apply(setter)          // 设置到 Tomcat 实例
```

**`configureHttp11Protocol`** 处理 HTTP/1.1 特有参数：
- `protocol.setMaxHttpHeaderSize`：HTTP 请求头最大大小
- `protocol.setMaxSwallowSize`：HTTP 请求体最大大小
- `connector.setMaxPostSize`：POST 请求最大大小

**快照机制**：`currentServerProperties` 保存上次配置值，每次配置变更后调用 `initCurrentServerProperties()` 更新快照。`configure()` 的 `.compare()` 方法对比新旧值，仅当值变化时才调用 `.apply()` 设置到 Tomcat--这避免了不必要的 Connector 重配置。

---

### ServiceRegistrationEndpoint / ServiceDeregistrationEndpoint：Actuator 注册控制

#### AbstractServiceRegistrationEndpoint

```java
public abstract class AbstractServiceRegistrationEndpoint
        implements SmartInitializingSingleton, ApplicationListener<WebServerInitializedEvent> {

    protected String applicationName;
    protected Registration registration;
    protected int port;
    protected ServiceRegistry<Registration> serviceRegistry;
    protected AbstractAutoServiceRegistration serviceRegistration;

    @Override
    public void afterSingletonsInstantiated() {
        this.registration = serviceRegistration.getRegistration();
        this.serviceRegistry = serviceRegistration.getServiceRegistry();
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        this.port = event.getWebServer().getPort();
    }
}
```

**`SmartInitializingSingleton`**：在所有单例 Bean 创建完成后获取 `registration` 和 `serviceRegistry`。此时 `AbstractAutoServiceRegistration` 已完全初始化，可以安全获取其依赖。

**`ApplicationListener<WebServerInitializedEvent>`**：获取 Web 服务器端口，用于 Actuator 端点返回。

#### 两个子类

| 端点 | ID | 操作 | 作用 |
|------|-----|------|------|
| `ServiceRegistrationEndpoint` | `serviceRegistration` | `@ReadOperation` 查询状态 + `@WriteOperation` 注册 | 手动注册 |
| `ServiceDeregistrationEndpoint` | `serviceDeregistration` | `@WriteOperation` 反注册 | 手动反注册 |

`ServiceRegistrationEndpoint` 的 `@ReadOperation` 返回：
- `application-name`：应用名
- `registration`：注册详情
- `port`：端口
- `status`：注册状态
- `running`：是否运行中
- `enabled`：是否启用
- `phase`/`order`：SmartLifecycle 参数
- `config`：配置对象

---

## 永恒原理

### 1. 特性开关的声明式 vs 编程式

Spring Cloud 的 `HasFeatures` 是编程式声明（需手动 `@Bean`），microsphere 的 `FeaturesProperties` 是声明式（属性文件即可）。`ConfigurationPropertyHasFeaturesAutoConfiguration` 将声明式配置转换为运行时 Bean 注册--通过 `@EnableConfigurationProperties(FeaturesProperties.class)` 绑定属性，在 `afterPropertiesSet()` 中通过 `beanFactory.registerSingleton()` 注册 `HasFeatures` Bean。

### 2. 适配器模式在响应式/阻塞式之间的桥接

`ReactiveDiscoveryClientAdapter` 将 `Flux<ServiceInstance>` 适配为 `List<ServiceInstance>`。`collectList().block()` 的语义等价性：只有在所有元素都收集完成后才返回，与 `DiscoveryClient.getInstances()` 的语义一致。

### 3. 元数据同步的 synchronized 与本地缓存

`RegistrationMetaData` 的双层设计：
- 写操作：`synchronized(lock)` + 遍历所有 Registration，同步到每个注册中心
- 读操作：直接返回 `applicationMetaData` 本地副本，无锁访问

这是读写分离的简化版：写操作代价高（需要跨注册中心同步），读操作代价低（本地缓存）。`synchronized` 锁保证写操作原子性，防止多线程并发修改导致元数据不一致。

### 4. configure() 流畅 API 的链式调用

`TomcatDynamicConfigurationListener` 的 `configure()` API 使用流畅构建器模式，每个方法返回 `Configurer` 实例，支持链式调用：

```
configure("Tomcat connection timeout")
    .value(() -> newTimeout)           // 新值供应器
    .on(value -> value > 0)            // 前置条件
    .compare(() -> oldTimeout)         // 旧值引用
    .as(duration -> (int)duration.toMillis())  // 类型转换
    .apply(timeout -> protocol.setConnectionTimeout(timeout))  // 最终设置
```

每次配置变更后，`currentServerProperties` 快照被更新（`initCurrentServerProperties()`），下次配置变更事件时，`.compare()` 引用的旧值是新快照的值。这确保了配置变更的增量生效。

### 5. SmartInitializingSingleton 作为安全初始化时机

`AbstractServiceRegistrationEndpoint` 使用 `SmartInitializingSingleton.afterSingletonsInstantiated()` 获取 `registration` 和 `serviceRegistry`。这与 03-spring 第 1 篇的 `afterSingletonsInstantiated` 触发时机一致：所有单例 Bean 创建完成后，`AbstractAutoServiceRegistration` 已完全初始化。

### 6. 抛物线预热权重

`calculateWarmupWeight` 的公式 `(uptime / warmup)² × weight` 产生抛物线增长。这是 Dubbo 的标准预热算法：新启动的服务实例在预热期内权重逐渐增加，避免刚启动时收到大量请求。

---

## 边界与反例

### 1. ConfigurationPropertyHasFeaturesAutoConfiguration 的 BeanDefinition 注册时序

它通过 `BeanDefinitionRegistry` 注册 `HasFeatures` Bean，时机在 `afterPropertiesSet()` 中。如果 `afterPropertiesSet()` 在 `BeanFactoryPostProcessor` 之后调用，`BeanDefinition` 的注册可能被跳过（因为 `freezeConfiguration()` 已冻结）。

### 2. RegistrationMetaData 的 Zookeeper 反射

对 Zookeeper `Registration` 的元数据修改需要通过反射获取 `ServiceInstance` 对象。如果 Zookeeper 注册中心的实现类名或方法名变化，反射调用会失败，元数据同步会静默失效。

### 3. TomcatDynamicConfigurationListener 的 configure() 原子性

`configure()` 是按参数逐个调用的，不是原子操作。如果在 `configure()` 调用过程中发生新的配置变更事件，`currentServerProperties` 快照已更新，新旧值对比可能失效。

### 4. ServiceRegistrationEndpoint 的线程安全

`start()` 方法没有 `synchronized`。如果多个线程同时调用，可能导致多次注册。`setRunning(true)` 和 `serviceRegistry.register(registration)` 不是原子操作。

### 5. ReactiveDiscoveryClientAdapter 的 block() 限制

`block()` 在 Reactor 调度线程中调用会抛出 `IllegalStateException`。适配器应在阻塞线程（如 Servlet 请求处理线程）中使用。

---

## 现代 Spring Cloud 是否已支持？

| microsphere 特性 | Spring Cloud 是否有等价物 | 说明 |
|------------------|------------------------|------|
| `FeaturesProperties` 声明式特性 | 无 | Spring Cloud 无声明式特性绑定 |
| `ConfigurationPropertyHasFeaturesAutoConfiguration` | 无 | Spring Cloud 无自动注册特性 Bean |
| `ReactiveDiscoveryClientAdapter` | 无 | Spring Cloud 的两种 DiscoveryClient 不互通 |
| `RegistrationMetaData` 同步 | 无 | Spring Cloud 无跨注册中心元数据同步 |
| `SpecificationCustomizer` SPI | 无 | Spring Cloud 无 Specification 定制 |
| `WeightedRoundRobin` + warmup | 无 | Spring Cloud LoadBalancer 无加权轮询 |
| `TomcatDynamicConfigurationListener` | 无 | Spring Boot 不支持运行时 Tomcat 参数调整 |
| `ServiceRegistrationEndpoint` / `ServiceDeregistrationEndpoint` | 无 | Spring Cloud 无注册控制 Actuator 端点 |

---

## 小结

microsphere-spring-cloud 的基础设施扩展，覆盖了六个独立组件：

1. **Features**（`FeaturesProperties` + `ConfigurationPropertyHasFeaturesAutoConfiguration` + `@ConditionalOnFeaturesAvailable`）：声明式特性开关，通过 `AutoRegistrationBean` 自动注册 `HasFeatures` Bean
2. **ReactiveDiscoveryClientAdapter**（`ReactiveDiscoveryClientAdapter` + `DiscoveryClientAutoConfiguration` + `ReactiveDiscoveryClientAutoConfiguration`）：适配器 + `@Primary` 替换默认 DiscoveryClient
3. **RegistrationMetaData**（`RegistrationMetaData` + `ServiceInstanceUtils`）：`synchronized` 元数据同步，Zookeeper 反射适配
4. **SpecificationCustomizer**（`SpecificationCustomizer` + `SpecificationBeanPostProcessor` + `SpecificationAutoConfiguration`）：SPI 拦截 `NamedContextFactory` 初始化
5. **WeightedRoundRobin**（`WeightedRoundRobin` + `LoadBalancerUtils.calculateWarmupWeight`）：`LongAdder` 计数器 + Dubbo 抛物线预热
6. **TomcatDynamicConfigurationListener**（通过 `configure()` 流畅 API 检测变更、增量更新、快照对比）：运行时动态调整 Tomcat Connector 参数

至此，microsphere-spring-cloud 的 3 篇深度分析全部完成。