# Microsphere-Spring-Cloud 深度分析

> **核心命题**：Spring Cloud 的架构假设是"一个服务只注册到一个注册中心"。如果业务需要同时注册到 Nacos + Eureka + Consul，怎么办？
> **本文回答**：它设计了 3 个打破 Spring Cloud 默认假设的扩展机制。

---

## 知识点 1：`UnionDiscoveryClient` — 多注册中心的服务发现聚合

### Spring Cloud 的默认假设

```java
// Spring Cloud 的架构：每个 DiscoveryClient 对应一个注册中心
@Autowired
private DiscoveryClient discoveryClient;  // ← 只有一个！要么是 Nacos，要么是 Eureka

// 如果同时引入了 Nacos 和 Eureka 依赖
// Spring Cloud 通过 @Primary 或 CompositeDiscoveryClient 选择一个
```

但实际场景中，一个服务可能**同时**注册到了 Nacos（内网）和 Consul（跨区域）。服务发现时，需要**聚合两个注册中心的实例列表**。

### 小马哥的做法

```java
public class UnionDiscoveryClient implements DiscoveryClient {

    public List<ServiceInstance> getInstances(String serviceId) {
        List<ServiceInstance> all = newLinkedList();
        for (DiscoveryClient client : getDiscoveryClients()) {
            all.addAll(client.getInstances(serviceId));  // 合并所有注册中心的结果
        }
        return all;
    }

    public List<DiscoveryClient> getDiscoveryClients() {
        // 从 Spring 容器中发现所有 DiscoveryClient bean
        // 排除：1) CompositeDiscoveryClient（包装器，非实际注册中心）
        //       2) 自己（UnionDiscoveryClient）
        for (DiscoveryClient client : getSortedBeans(context, DiscoveryClient.class)) {
            if (excludeCompositeAndSelf(client)) continue;
            discoveryClients.add(client);
        }
        return discoveryClients;
    }
}
```

### 知识点

**为什么需要"合并"而不是"选择一个"？**

```
场景：服务 order-service 同时注册到
  Nacos:     order-service -> [10.0.1.1:8080, 10.0.1.2:8080]
  Consul:    order-service -> [10.1.1.1:8080, 10.1.1.2:8080]

传统方案（选一个）:
  discoveryClient.getInstances("order-service")
  → 只返回 Nacos 的结果，丢掉 Consul 的结果

UnionDiscoveryClient:
  → [10.0.1.1:8080, 10.0.1.2:8080, 10.1.1.1:8080, 10.1.1.2:8080]
```

**设计细节**：

1. **`getOrder() = HIGHEST_PRECEDENCE`**：确保 `UnionDiscoveryClient` 在所有其他 `DiscoveryClient` 之前被 Spring Cloud 发现和注入。如果 Spring Cloud 的负载均衡器拿到了 `UnionDiscoveryClient` 而不是单独的 `NacosDiscoveryClient`，它就能获得**聚合后的实例列表**。

2. **排除 `CompositeDiscoveryClient`**：Spring Cloud 自己也会创建一个 `CompositeDiscoveryClient` 包装所有 `DiscoveryClient`。如果不排除它，`UnionDiscoveryClient` 会把它当作一个数据源，导致重复聚合。

3. **懒初始化 + 缓存**：`getDiscoveryClients()` 第一次调用时从容器查找并缓存，后续调用直接用缓存。`afterSingletonsInstantiated()` 中提前初始化，避免第一次调用时的延迟。

> **可迁移知识**：多注册中心聚合的核心不是"怎么连接 Nacos + Consul"（各自有 adapter），而是**在 DiscoveryClient 这一层做接口聚合**。所有的 `DiscoveryClient` 都实现了同一个接口——利用这个多态特性，一个 `UnionDiscoveryClient` 就能透明地合并任意数量的注册中心。

---

## 知识点 2：`MultipleAutoServiceRegistration` — 一个服务向多个注册中心注册

### 问题

Spring Cloud 的 `AutoServiceRegistration` 假设：**一个服务只注册到一个注册中心**。如果你同时引入了 `spring-cloud-starter-alibaba-nacos-discovery` 和 `spring-cloud-starter-netflix-eureka`，只有一个能生效（谁先自动配置谁注册）。

### 小马哥的做法

```java
// MultipleRegistration = 多条 Registration 的组合
public class MultipleAutoServiceRegistration 
    extends AbstractAutoServiceRegistration<MultipleRegistration> {

    public MultipleAutoServiceRegistration(
        MultipleRegistration multipleRegistration,  // 包含 Nacos Registration + Eureka Registration
        ServiceRegistry<MultipleRegistration> serviceRegistry,
        AutoServiceRegistrationProperties properties) {
        super(serviceRegistry, properties);
    }
}
```

### 知识点

**Spring Cloud 的注册流程**：

```
ApplicationReadyEvent 触发
    ↓
AutoServiceRegistration.start()
    ↓
ServiceRegistry.register(Registration)  ← 只注册一次，只到一个注册中心
    ↓
ServiceRegistry.setStatus(Registration, "UP")
```

**Microsphere 的改造**：

```
ApplicationReadyEvent 触发
    ↓
MultipleAutoServiceRegistration.start()
    ↓
MultipleServiceRegistry.register(MultipleRegistration)
    ↓
遍历 MultipleRegistration.getRegistrations():
    └→ NacosServiceRegistry.register(nacosRegistration)
    └→ EurekaServiceRegistry.register(eurekaRegistration)
    ↓
所有注册中心都注册了同一个服务
```

**`RegistrationCustomizer` SPI**：注册前允许修改 Registration 的元数据：

```java
public interface RegistrationCustomizer {
    void customize(Registration registration);
}

// 实现示例：给 Nacos 注册中心加自定义元数据
public class NacosMetadataCustomizer implements RegistrationCustomizer {
    public void customize(Registration reg) {
        reg.getMetadata().put("startupTime", String.valueOf(System.currentTimeMillis()));
    }
}
```

> **可迁移知识**：Spring Cloud 的单注册中心假设不是 Bug，而是**简化设计**——Spring Cloud 认为多注册中心是"不应该出现"的场景。但当你的架构确实需要它时，解法不是"放弃 Spring Cloud"，而是**在它的接口层面做包装**——`MultipleRegistration` 包装多条 `Registration`，`MultipleServiceRegistry` 包装多个 `ServiceRegistry`。

---

## 知识点 3：`EventPublishingRegistrationAspect` — 注册生命周期的可观测性

### 问题

Spring Cloud 的服务注册过程是**静默的**——你调用 `serviceRegistry.register(reg)`，它注册成功或失败，没有任何事件广播。你无法：
- 知道注册是否成功（除非读日志）
- 在注册成功后做额外操作（如通知监控系统）
- 在注册失败后做降级处理

### 小马哥的做法

```java
// AOP 切面：拦截 ServiceRegistry 的 register/deregister/setStatus 方法
@Aspect
public class EventPublishingRegistrationAspect {

    @AfterReturning("execution(* ServiceRegistry.register(..))")
    public void afterRegister(JoinPoint jp) {
        Registration reg = (Registration) jp.getArgs()[0];
        context.publishEvent(new ServiceInstanceRegisteredEvent(reg));
    }
}
```

### 知识点

**为什么用 AOP 而不是包装类？**

Spring Cloud 的 `ServiceRegistry` 是**外部依赖**（`spring-cloud-commons` 提供的）。你不能修改它的实现代码。你有两个选择：

1. **装饰器模式**：写一个 `LoggingServiceRegistry` 包装原始的 `ServiceRegistry`
   - 问题：需要确保所有地方都用装饰器，而不是原始对象
2. **AOP 切面**：在 `ServiceRegistry` 的方法执行前后插入逻辑
   - 优势：透明，不改变调用方代码，**只要调用的是 Spring 管理的 bean 就能拦截**

**Microsphere 选择了 AOP**——注册 `EventPublishingRegistrationAspect` 作为 Spring AOP 切面，在 `ServiceRegistry.register()` / `deregister()` / `setStatus()` 执行后广播事件。

> **可迁移知识**：当你需要给**第三方接口**添加横切关注点（日志、事件、监控），装饰器模式和 AOP 都可以。选择的关键在于：装饰器需要控制对象的创建和注入（可能侵入性强），AOP 通过代理自动拦截（更透明但依赖 Spring AOP 基础设施）。**Microsphere 选择 AOP 因为它不修改 Spring Cloud 的自动配置**。

---

## 总结：Microsphere-Spring-Cloud 的 3 个知识点

| # | 知识点 | Spring Cloud 的默认假设 | Microsphere 的突破 | 适用场景 |
|---|---|---|---|---|
| 1 | 多注册中心发现 | 一个 DiscoveryClient 就够了 | UnionDiscoveryClient 聚合所有 | 跨区域、混合注册中心 |
| 2 | 多注册中心注册 | 一个服务只注册一次 | MultipleAutoServiceRegistration 一次注册到所有 | 灰度迁移、多活架构 |
| 3 | 注册生命周期事件 | 注册/注销没有事件广播 | AOP 切面发布事件 | 注册状态监控、自动化运维 |

**与前一层的继承关系**：

```
microsphere-java         →  定义了 Artifact 模型 + SPI 机制
microsphere-spring       →  用 BeanPostProcessor 把 SPI 集成到 Spring
microsphere-spring-boot  →  用 AutoConfiguration + Listener 把集成自动化
microsphere-spring-cloud →  把自动化的模式应用到 Spring Cloud 的注册/发现/负载均衡
```

每一层都在**上一层的抽象基础上做领域化适配**：java 层定义了可复用的工具，spring 层把它集成到 IoC 生命周期，spring-boot 层让它开箱即用，spring-cloud 层把它应用到微服务治理的具体场景。
