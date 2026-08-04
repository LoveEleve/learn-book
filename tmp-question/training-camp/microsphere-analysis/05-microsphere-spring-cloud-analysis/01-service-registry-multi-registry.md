# 05-01 Service Registry 多注册中心

## 问题：Spring Cloud 只能用一个注册中心

Spring Cloud 的 `@EnableDiscoveryClient` + `ServiceRegistry` SPI 抽象了服务注册机制，但存在一个根本限制：**一个应用实例只能注册到一个注册中心**。

```java
// Spring Cloud 原生：只能有一个 ServiceRegistry Bean 被 @EnableDiscoveryClient 使用
// 要切换注册中心，需要修改依赖和配置
@EnableDiscoveryClient  // Eureka？Nacos？Consul？三选一
@SpringBootApplication
public class MyApplication { }
```

实际业务中，多注册中心的需求很常见：

| 场景 | 需求 | Spring Cloud 的困境 |
|------|------|-------------------|
| **注册中心迁移** | Eureka → Nacos：两个注册中心同时运行，逐步迁移 | 无法同时注册到两个，迁移需要停机切换 |
| **多区域部署** | 国内用 Nacos，海外用 Consul，所有实例同时注册 | 一个应用只能注册到一个，需额外部署 |
| **混合云** | 私有云用 Eureka，公有云用 Nacos，网关需要发现全部 | 服务名可以相同，但发现客户端互斥 |
| **注册中心做主备** | 主 Nacos 挂了自动切到备 Consul | 无主备机制，切换需要重启 |

Spring Cloud 原生只允许一个 `ServiceRegistry` Bean 生效。microsphere-spring-cloud 的 **MultipleServiceRegistry** 体系解决这个问题：让一个应用实例**同时注册到多个注册中心**，并且消费者可以从多个注册中心发现实例。

---

## 设计：多注册中心体系

### 整体架构

```
应用启动
    │
    ├── ServiceRegistryAutoConfiguration (自动装配)
    │       │
    │       ├── MultipleConfiguration (@ConditionalOnMultipleRegistrationEnabled)
    │       │       ├── MultipleRegistration(Collection<Registration>)   ← 聚合所有 Registration Bean
    │       │       ├── MultipleServiceRegistry(Map<beanName, ServiceRegistry>)  ← 聚合所有 Registry
    │       │       └── MultipleAutoServiceRegistration → 自动注册到所有 Registry
    │       │
    │       └── EventPublishingRegistrationAspect (AOP)
    │               ├── @Before register → RegistrationPreRegisteredEvent + RegistrationCustomizer
    │               └── @After register → RegistrationRegisteredEvent
    │               ├── @Before deregister → RegistrationPreDeregisteredEvent
    │               └── @After deregister → RegistrationDeregisteredEvent
    │
    ├── ServiceRegistrationEndpoint (Actuator)
    │       ├── @ReadOperation → 查询注册状态
    │       └── @WriteOperation → 手动注册/反注册
    │
    └── UnionDiscoveryClient (聚合发现)
            ├── SmartInitializingSingleton → 构建 DiscoveryClient 列表（排除 CompositeDiscoveryClient）
            └── getInstances() → 遍历所有 DiscoveryClient，聚合结果（不做去重，`getServices()` 去重）
```

---

### MultipleRegistration：多注册身份

```java
public class MultipleRegistration implements Registration {

    // key: Registration 类型 → Registration 实例
    // 例如: NacosRegistration.class → nacosRegistration
    //        EurekaRegistration.class → eurekaRegistration
    private Map<Class<? extends Registration>, Registration> registrationMap;
    private Registration defaultRegistration;         // 最后一个作为默认

    public MultipleRegistration(Collection<Registration> registrations) {
        for (Registration registration : registrations) {
            // 找到 Registration 实现类的所有子类型（排除 Registration 本身）
            List<Class<? extends Registration>> types = findAllClasses(
                ultimateTargetClass(registration),
                type -> isAssignableFrom(Registration.class, type) && !Registration.class.equals(type)
            );
            types.forEach(type -> registrationMap.put(type, registration));
            this.defaultRegistration = registration;  // 最后一个为默认
        }
    }

    @Override
    public String getServiceId() {
        return defaultRegistration.getServiceId();    // 委托给默认
    }

    public <T extends Registration> T special(Class<T> specialClass) {
        if (Registration.class.equals(specialClass))
            return (T) this;                          // 自身就是 MultipleRegistration
        return (T) this.registrationMap.getOrDefault(specialClass, null);
    }
}
```

**设计要点**：

- `registrationMap` 以 Registration 的**子类型**为 key，而非 Bean 名称。例如 `NacosRegistration` 拿到 `NacosRegistration.class` 作为 key，`EurekaRegistration` 拿到 `EurekaRegistration.class` 作为 key
- 通过 `findAllClasses` 扫描子类型链，确保接口继承关系也正确匹配。例如 `NacosRegistration extends NacosRegistrationCustom` 会同时注册 `NacosRegistration.class` 和 `NacosRegistrationCustom.class`
- `defaultRegistration` 是最后一个 Registration，所有"通用"操作（`getServiceId`/`getHost`/`getPort`）委托给它

---

### MultipleServiceRegistry：多注册中心代理

```java
public class MultipleServiceRegistry implements ServiceRegistry<MultipleRegistration> {

    private final Map<String, ServiceRegistry> registriesMap;        // beanName → ServiceRegistry
    private final Map<String, Class<? extends Registration>> beanNameToRegistrationTypesMap;  // beanName → Registration 类型
    private ServiceRegistry defaultServiceRegistry;

    @Override
    public void register(MultipleRegistration registration) {
        iterate(registration, (reg, registry) -> registry.register(reg));
    }

    private void iterate(MultipleRegistration registration,
                         BiConsumer<Registration, ServiceRegistry> action) {
        registriesMap.forEach((beanName, registry) -> {
            Class<? extends Registration> registrationClass =
                beanNameToRegistrationTypesMap.get(beanName);
            Registration targetRegistration = registration.special(registrationClass);
            if (targetRegistration != null) {
                action.accept(targetRegistration, registry);
            }
        });
    }
}
```

**关键逻辑**：`register(MultipleRegistration)` 遍历所有注册中心的 `ServiceRegistry`，对每个 Registry 从 `MultipleRegistration` 中取出对应类型的 `Registration`，委托注册。这样，一个 `register` 调用同时注册到 Nacos、Eureka、Consul 等多个注册中心。

**注册类型匹配**：`MultipleServiceRegistry` 在构造时通过 `getRegistrationClass()` 解析每个 `ServiceRegistry` 的泛型参数 `T`（如 `ServiceRegistry<NacosRegistration>`）。例如 Nacos 的 `ServiceRegistry` 实现声明 `ServiceRegistry<NacosRegistration>`，解析出 `NacosRegistration.class`。然后 `MultipleRegistration.special(NacosRegistration.class)` 返回对应的 Registration。`special()` 方法在 `registrationMap` 中按子类型查找，如果请求的类型是 `Registration.class`（接口本身），返回 `MultipleRegistration` 自身。

---

### MultipleAutoServiceRegistration：自动注册

```java
public class MultipleAutoServiceRegistration
        extends AbstractAutoServiceRegistration<MultipleRegistration> {

    @Override
    protected MultipleRegistration getRegistration() {
        return multipleRegistration;
    }
}
```

继承 Spring Cloud 的 `AbstractAutoServiceRegistration`，将 `MultipleRegistration` 作为管理对象。Spring Cloud 的 `AutoServiceRegistration` 生命周期（`start()` → `register()` → `setStatus(UP)` → ... → `deregister()`）全部委托给 `MultipleServiceRegistry`，后者再委派给各个子 Registry。

**自动装配条件**：`@ConditionalOnMultipleRegistrationEnabled`（默认启用）+ `@ConditionalOnBean({Registration.class, ServiceRegistry.class})`。只有当容器中有至少一个 `Registration` 和一个 `ServiceRegistry` Bean 时才激活。

---

### EventPublishingRegistrationAspect：注册生命周期事件

```java
@Aspect
public class EventPublishingRegistrationAspect {

    @Before("execution(* ServiceRegistry.register(*)) && target(registry) && args(registration)")
    public void beforeRegister(ServiceRegistry registry, Registration registration) {
        if (isIgnored(registry)) return;  // 排除 MultipleServiceRegistry（避免重复）
        context.publishEvent(new RegistrationPreRegisteredEvent(registry, registration));
        registrationCustomizers.forEach(customizer -> customizer.customize(registration));
    }

    @After("execution(* ServiceRegistry.deregister(*)) && target(registry) && args(registration)")
    public void afterDeregister(ServiceRegistry registry, Registration registration) {
        if (isIgnored(registry)) return;
        context.publishEvent(new RegistrationDeregisteredEvent(registry, registration));
    }
}
```

**事件体系**（4 个事件，继承 `RegistrationEvent`）：

```
ServiceRegistry.register() 调用前 → RegistrationPreRegisteredEvent
ServiceRegistry.register() 调用后 → RegistrationRegisteredEvent
ServiceRegistry.deregister() 调用前 → RegistrationPreDeregisteredEvent
ServiceRegistry.deregister() 调用后 → RegistrationDeregisteredEvent
```

**`RegistrationCustomizer` SPI**：在 `PreRegistered` 事件中、注册之前调用。允许用户修改 Registration 的元数据（如添加自定义标签、修改权重）：

```java
@Component
public class MyRegistrationCustomizer implements RegistrationCustomizer {
    @Override
    public void customize(Registration registration) {
        registration.getMetadata().put("custom-key", "custom-value");
    }
}
```

**`isIgnored` 过滤**：AOP 拦截所有 `ServiceRegistry.register()` 调用，但 `MultipleServiceRegistry.register()` 内部会遍历并调用子 Registry 的 `register()`，这会导致**重复拦截**。`isIgnored` 检查 registry 是否为 `MultipleServiceRegistry` 实例，如果是则跳过--因为 `MultipleServiceRegistry` 自身已经通过遍历调用了子 Registry，子 Registry 的 `register()` 会被正常拦截。

---

### UnionDiscoveryClient：聚合发现

`UnionDiscoveryClient` 实现了 `DiscoveryClient`，但它不是无条件注册的--通过 `DiscoveryClientAutoConfiguration` 的模式机制条件化激活：

```java
@Configuration
@ConditionalOnBlockingDiscoveryAvailable
@AutoConfigureBefore(CommonsClientAutoConfiguration.class)
public class DiscoveryClientAutoConfiguration {

    @ConditionalOnProperty(name = DISCOVERY_CLIENT_MODE_PROPERTY_NAME, havingValue = "union")
    static class UnionConfiguration {

        @Bean
        @Primary  // 替换 Spring Cloud 默认的 DiscoveryClient
        public UnionDiscoveryClient unionDiscoveryClient() {
            return new UnionDiscoveryClient();
        }
    }
}
```

只有当 `microsphere.spring.cloud.client.discovery.mode=union` 时（默认值），`UnionDiscoveryClient` 才作为 `@Primary` Bean 注册。这允许用户通过属性切换发现模式。

```java
public final class UnionDiscoveryClient implements DiscoveryClient,
        ApplicationContextAware, SmartInitializingSingleton {

    private List<DiscoveryClient> discoveryClients;

    @Override
    public void afterSingletonsInstantiated() {
        List<DiscoveryClient> sortedClients = getSortedBeans(context, DiscoveryClient.class);
        this.discoveryClients = newLinkedList();
        for (DiscoveryClient client : sortedClients) {
            if (client != this && !isCompositeDiscoveryClient(client)) {
                this.discoveryClients.add(client);  // 排除自身和 CompositeDiscoveryClient
            }
        }
    }

    @Override
    public List<ServiceInstance> getInstances(String serviceId) {
        List<ServiceInstance> serviceInstances = newLinkedList();
        List<DiscoveryClient> discoveryClients = getDiscoveryClients();
        for (DiscoveryClient discoveryClient : discoveryClients) {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
            if (isNotEmpty(instances)) {
                serviceInstances.addAll(instances);
            }
        }
        return serviceInstances;
    }
}
```

注意：`getInstances` 不做去重。如果同一服务实例注册到多个注册中心且各中心返回不同的 `instanceId`，消费者可能看到重复实例。`getServices()` 使用 `Set<String>` 去重。

**排除 `CompositeDiscoveryClient`**：Spring Cloud 默认有一个 `CompositeDiscoveryClient`，聚合所有 `DiscoveryClient`。`UnionDiscoveryClient` 排除它，因为 `CompositeDiscoveryClient` 会重复调用子 DiscoveryClient，导致 `UnionDiscoveryClient` 再次聚合时产生重复。

---

### ServiceRegistrationEndpoint：手动注册控制

```java
@Endpoint(id = "serviceRegistration")
public class ServiceRegistrationEndpoint extends AbstractServiceRegistrationEndpoint {

    @ReadOperation
    public Map<String, Object> metadata() {
        // 返回注册状态：应用名、Registration 详情、端口、状态、是否运行中、是否启用、phase、order
    }

    @WriteOperation
    public boolean start() {
        if (!isRunning()) {
            serviceRegistry.register(registration);  // 手动触发注册
            setRunning(true);
        }
        return isRunning;
    }
}
```

通过 `GET /actuator/serviceRegistration` 查询注册状态，`POST /actuator/serviceRegistration` 手动触发注册。这在调试和运维场景中有用（如注册中心重启后手动重新注册）。

---

### 一个完整示例

```yaml
# application.yml：同时注册到 Nacos 和 Eureka
spring:
  application:
    name: my-service
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
    eureka:
      client:
        service-url:
          defaultZone: http://127.0.0.1:8761/eureka/
```

只需同时依赖 `nacos-discovery-starter` 和 `eureka-starter`，microsphere 自动创建：

1. `NacosRegistration` + `EurekaRegistration` → 聚合为 `MultipleRegistration`
2. `NacosServiceRegistry` + `EurekaServiceRegistry` → 聚合为 `MultipleServiceRegistry`
3. `MultipleAutoServiceRegistration` → 启动时调用 `MultipleServiceRegistry.register(multipleRegistration)` → 依次注册到 Nacos 和 Eureka
4. `UnionDiscoveryClient` → 消费者通过 `DiscoveryClient.getInstances("my-service")` 获取两个注册中心的实例

---

## 永恒原理

### 1. 组合模式 + 代理模式

`MultipleServiceRegistry` 同时使用了组合模式和代理模式：

- **组合**：持有多个 `ServiceRegistry` 实例，将 `register`/`deregister` 操作广播到所有子 Registry
- **代理**：与 Spring Cloud 的 `ServiceRegistry` 接口相同，对外表现为一个单一的 `ServiceRegistry`，内部委托给具体的子 Registry

`UnionDiscoveryClient` 采用相同思路：对外表现为一个 `DiscoveryClient`，内部遍历所有子 DiscoveryClient 并聚合结果。

这种"统一接口 + 内部委派"的设计模式让 Spring Cloud 的上层组件（`AutoServiceRegistration`、`DiscoveryClient` 消费者）不需要感知多注册中心的存在。

### 2. AOP 与事件发布的时序控制

`EventPublishingRegistrationAspect` 通过 AOP 拦截 `ServiceRegistry.register()`，在注册前后发布事件。关键设计是 `isIgnored` 排除 `MultipleServiceRegistry`：

```
调用链路：
  MultipleAutoServiceRegistration.start()
    → MultipleServiceRegistry.register(multipleRegistration)
        → NacosServiceRegistry.register(nacosRegistration)        ← 被 AOP 拦截 ✅
        → EurekaServiceRegistry.register(eurekaRegistration)      ← 被 AOP 拦截 ✅
    → (如果不排除 MultipleServiceRegistry) ← 也会被 AOP 拦截，但这是"包装器调用"，不应触发事件
```

`isIgnored` 确保事件只对"真实的"子注册中心的 `register` 调用触发，而非对 `MultipleServiceRegistry` 这个"包装器"的调用。

### 3. 聚合发现而非去重

`UnionDiscoveryClient.getInstances()` 简单地将所有子 DiscoveryClient 的结果合并（`addAll`），不做去重。`getServices()` 使用 `Set<String>` 去重。这种设计的隐含假设是：**同一服务实例在不同注册中心中应使用相同的 `instanceId`**。如果不同注册中心为同一实例生成了不同的 `instanceId`（如 Nacos 的 instanceId 与 Eureka 的不同），消费者会看到重复的实例。

这要求用户主动统一 `instanceId` 的生成规则（如配置 `spring.cloud.nacos.discovery.instance-id` 和 `eureka.instance.instanceId` 为相同值）。

---

## 边界与反例

### 1. Registration 类型匹配的准确性

`MultipleServiceRegistry` 通过 `getRegistrationClass()` 解析 `ServiceRegistry` 的泛型参数 `T`。这依赖 Java 的 `ParameterizedType` 信息，如果 `ServiceRegistry` 实现是匿名类或泛型签名不完整，解析可能失败。

**缓解**：Spring Cloud 的注册中心实现（Nacos/Eureka/Consul）都有完整的泛型签名，实际场景中解析失败的概率较低。

### 2. isIgnored 的 AOP 漏洞

`EventPublishingRegistrationAspect` 的 `isIgnored` 检查 registry 是否为 `MultipleServiceRegistry` 实例。但如果用户自定义 `ServiceRegistry` 包装器（继承 `MultipleServiceRegistry`），且未正确处理事件传播，可能导致事件丢失或重复。

### 3. UnionDiscoveryClient.getInstances 不做去重

`UnionDiscoveryClient.getInstances()` 直接合并所有子 DiscoveryClient 的返回结果，不做任何去重。如果同一服务实例注册到多个注册中心，且各注册中心返回的 `ServiceInstance` 的 `instanceId` 不同（这是常见情况），消费者会收到重复实例。这可能导致负载均衡不均匀或请求被路由到同一实例多次。

**缓解**：统一配置 `instanceId` 格式，使不同注册中心返回相同的 `instanceId`。或者在使用 `UnionDiscoveryClient` 的消费端自行去重（如 `Map<String, ServiceInstance>` 按 `instanceId` 去重）。

### 4. MultipleAutoServiceRegistration 的启动顺序

`MultipleAutoServiceRegistration` 依赖 `AutoServiceRegistrationProperties` 的 `isEnabled()`。如果某个子 Registry 的 `SmartLifecycle.phase` 不同，`AbstractAutoServiceRegistration` 的启动顺序可能早于或晚于其他 Registry 的初始化。

### 5. Actuator 端点的线程安全

`ServiceRegistrationEndpoint.start()` 没有 `synchronized`。如果多个线程同时调用，可能导致多次注册。在正常运维场景中通常不会，但在自动化脚本中可能发生。

---

## 现代 Spring Cloud（2023/2024）是否已支持？

| microsphere 特性 | Spring Cloud 是否有等价物 | 说明 |
|------------------|------------------------|------|
| `MultipleServiceRegistry`（多注册中心注册） | 无 | Spring Cloud 只允许一个 `ServiceRegistry` Bean |
| `MultipleAutoServiceRegistration`（自动注册到多个） | 无 | Spring Cloud 的 `AutoServiceRegistration` 只绑一个 Registry |
| `UnionDiscoveryClient`（聚合发现） | 无 | Spring Cloud 的 `CompositeDiscoveryClient` 存在，但不聚合多注册中心结果 |
| `EventPublishingRegistrationAspect`（注册事件） | 无 | Spring Cloud 无注册生命周期事件 |
| `ServiceRegistrationEndpoint`（手动注册控制） | 无 | Spring Cloud 无注册控制 Actuator 端点 |

Spring Cloud 2023.0.x 的 `ServiceRegistry` 接口没有变化。`CompositeDiscoveryClient` 虽然是复合发现客户端，但它简单地遍历所有 `DiscoveryClient` 并调用 `getInstances()`，与微服务的多注册中心场景无关。

---

## 小结

microsphere-spring-cloud 的多注册中心体系，解决了一个 Spring Cloud 长期未解决的痛点：**一个应用实例只能注册到一个注册中心**。

核心组件：
- **`MultipleRegistration`**：多注册身份聚合，按子类型路由
- **`MultipleServiceRegistry`**：多注册中心代理，将 `register`/`deregister` 广播到所有子 Registry
- **`MultipleAutoServiceRegistration`**：继承 `AbstractAutoServiceRegistration`，使自动注册流程支持多注册中心
- **`EventPublishingRegistrationAspect`**：AOP 注册生命周期事件 + `RegistrationCustomizer` SPI
- **`UnionDiscoveryClient`**：聚合发现
- **`ServiceRegistrationEndpoint`**：手动注册控制 Actuator 端点

整个体系通过 `@ConditionalOnMultipleRegistrationEnabled` 自动装配，对用户透明。只需要同时引入多个注册中心的 starter，无需修改代码。
