# Microsphere-Multiactive 深度分析

> **核心命题**：异地多活架构的核心是"同区域优先调用"--北京的服务优先调北京的服务，北京的 Redis 优先连北京的 Redis。microsphere-multiactive 用 31 个文件实现了区域发现、优先路由、动态切换、信息传播四个能力。
> **本文覆盖**：源码逐文件分析 + 工程权衡 + 分布式问题 + 架构对比 + 面试角度。

---

## 第一部分：问题背景--异地多活的核心挑战

### 什么是异地多活

异地多活（Multi-Active）是指多个数据中心同时提供服务，每个中心都能读写。和"两地三中心"（主备模式）不同，多活要求所有中心同时活跃。

国内典型场景：
- 阿里：杭州 + 张北 + 深圳，三地多活
- 美团：北京 + 上海，双活
- 字节：北京 + 新加坡，跨国多活

### 核心挑战：区域感知路由

```
北京机房的服务 A 调用服务 B
  -> 注册中心返回 [北京-B1, 北京-B2, 上海-B3, 上海-B4]
  -> 理想：只调北京-B1/B2（同区域，延迟 <1ms）
  -> 北京全挂：fallback 到上海-B3/B4（跨区域，延迟 ~30ms 但可用）
```

同区域调用的延迟优势：北京到北京 < 1ms，北京到上海 ~30ms。1000 QPS 的服务，30ms 的额外延迟意味着每个请求多等 30ms，P99 延迟从 5ms 变成 35ms。

### Spring Cloud 的空白

Spring Cloud 有 `ZonePreferenceServiceInstanceListSupplier`，但只有最基本的区域偏好。缺：
1. **区域发现**--怎么知道当前应用在哪个区域？
2. **安全阈值**--同区域只有 1 个实例时，全压给它会不会过载？
3. **动态切换**--配置中心改区域配置，运行时能不能立即生效？
4. **资源联动**--区域切换时 Redis/DataSource 怎么跟着切？
5. **多云支持**--AWS EC2/ECS、Eureka、K8s 的区域信息来源不同

microsphere-multiactive 补了这五个空白。

---

## 第二部分：ZoneContext--全局区域状态（单例）

### 源码

```java
public class ZoneContext {
    private static final ZoneContext instance = new ZoneContext();  // 单例

    private volatile boolean enabled = true;                    // 区域功能总开关
    private volatile String zone = "defaultZone";               // 当前区域
    private volatile boolean preferenceEnabled = false;          // 优先路由开关（默认关！）
    private volatile int preferenceFilterOrder = 10;             // 过滤器顺序
    private volatile int preferenceUpstreamZoneReadyPercentage = 100;  // 上游就绪率阈值
    private volatile int preferenceUpstreamSameZoneMinAvailable = 5;   // 同区域最小可用数
    private volatile String preferenceUpstreamDisabledZone = null;     // 禁用的区域

    private final PropertyChangeSupport propertyChangeSupport;

    // 属性变更时触发 PropertyChangeEvent
    private boolean isPropertyChanged(String name, Object oldVal, Object newVal) {
        boolean changed = !Objects.equals(oldVal, newVal);
        if (changed) {
            propertyChangeSupport.firePropertyChange(name, oldVal, newVal);
        }
        return changed;
    }

    public static ZoneContext get() { return instance; }
}
```

### 为什么用单例而不是 Spring Bean

ZoneContext 需要在非 Spring 代码中访问：
- Ribbon 的 `ServerListFilter` 是 Netflix 的接口，不一定在 Spring 上下文中
- Redis 的 `DynamicRedisConnectionFactory` 的 ThreadLocal 路由需要读当前区域
- 测试代码需要直接访问

如果是 Spring Bean，这些场景拿不到引用。单例牺牲了可测试性（无法 mock）换取了全局可达性。

### 为什么 preferenceEnabled 默认 false

区域偏好路由改变流量分布。开启后同区域流量集中，可能压垮少量同区域实例。默认关闭是安全第一：用户必须明确知道自己在做什么。

### PropertyChangeSupport--JavaBeans 事件机制

ZoneContext 不是 Spring Bean，不能用 Spring 的 `ApplicationEvent`。它用 Java 标准的 `PropertyChangeSupport` 触发属性变更事件。`ZoneContextChangedListener`（Spring 层）把这个 JavaBeans 事件桥接为 Spring 的 `ZoneContextChangedEvent`。

---

## 第三部分：ZoneLocator--区域发现（4 个定位器 + 组合模式）

### ZoneLocator SPI

```java
public interface ZoneLocator {
    boolean supports(Environment environment);  // 当前环境是否支持
    String locate(Environment environment);     // 返回区域名
}
```

### 4 个 ZoneLocator 及实际优先级

| 定位器 | order | 来源 | supports 条件 |
|--------|-------|------|--------------|
| `EcsContainerMetadataFileZoneLocator` | **5** | ECS 容器元数据文件 | 环境变量 `ECS_CONTAINER_METADATA_FILE` 存在 |
| `EcsTaskMetadataEndpointV4ZoneLocator` | **10** | ECS Task Metadata V4 端点 | 环境变量 `ECS_CONTAINER_METADATA_URI_V4` 存在 |
| `Ec2AvailabilityZoneEndpointZoneLocator` | **15** | EC2 元数据端点 HTTP 请求 | 永远 true（尝试请求，失败返回 null） |
| `DefaultZoneLocator` | **20** | Spring 属性 `microsphere.availability.zone` | 永远 true |

**优先级**：ECS 文件(5) > ECS V4(10) > EC2 端点(15) > Spring 属性(20)。ECS 优先于 EC2 是因为 ECS 容器可能跑在 EC2 上，EC2 元数据端点在 ECS 环境中也能访问但返回的是宿主机信息，不是容器信息。

注意：Netflix Eureka 的 `EurekaInstanceInfoZoneResolver` 是 `ZoneResolver`（从 InstanceInfo 解析区域），不是 `ZoneLocator`（定位当前应用的区域）。两者职责不同，不要混淆。

### CompositeZoneLocator--组合定位器

```java
public class CompositeZoneLocator implements ZoneLocator {
    private final List<ZoneLocator> zoneLocators;  // 按 @Order 排序
    private volatile String zone;                   // 缓存（只定位一次！）

    public String locate(Environment environment) {
        if (StringUtils.hasText(zone)) {
            return zone;  // 返回缓存
        }
        for (ZoneLocator zoneLocator : zoneLocators) {
            try {
                if (zoneLocator.supports(environment)) {
                    zone = zoneLocator.locate(environment);
                    if (zone != null) {
                        System.setProperty(CURRENT_ZONE_PROPERTY_NAME, zone);
                        break;  // 第一个非 null 结果生效
                    }
                }
            } catch (Throwable e) {
                if (fastFailEnabled) throw new IllegalStateException(...);
                // fast-fail=false：继续尝试下一个
            }
        }
        return zone;
    }
}
```

### 工程分析

**缓存设计**：`zone` 是 `volatile`，定位一次后缓存。区域在应用生命周期内通常不变。如果需要变，通过 `ZoneContext.setZone()` 更新。

**fast-fail**：默认 false。非 AWS 环境下 EC2 元数据端点不可达（`169.254.169.254` 是 link-local 地址），会超时。默认容忍超时，继续尝试下一个定位器。如果 true，超时就抛异常阻止启动。

**超时配置**：默认 3000ms。EC2 元数据端点在 AWS 环境内通常 < 100ms 响应，非 AWS 环境 3 秒后超时。这意味着非 AWS 环境启动会多等 3 秒（等 EC2 端点超时），然后 fallback 到 Spring 属性。

**HttpUtils 用 JDK HttpURLConnection**：没有连接池，每次请求新建连接。对于启动时的一次性请求可以接受，但如果区域定位被频繁调用（不会，因为有缓存）会有问题。

### ZoneAutoConfiguration--自动装配

```java
@Configuration
@ConditionalOnAvailabilityZoneAvailable
@Import(ZoneContextChangedListener.class)
public class ZoneAutoConfiguration {

    @Bean
    public ZoneContext zoneContext() {
        return ZoneContext.get();  // 返回单例
    }

    @Bean @Primary
    public CompositeZoneLocator zoneLocator(Collection<ZoneLocator> beans, ConfigurableApplicationContext context) {
        // 两个来源：Spring Bean + Spring Factories
        List<ZoneLocator> all = new ArrayList<>();
        all.addAll(beans);                              // Spring Bean
        all.addAll(loadFactories(context, ZoneLocator.class));  // spring.factories
        sort(all);  // 按 @Order 排序
        return new CompositeZoneLocator(all);
    }
}
```

ZoneLocator 的发现有两个来源：Spring Bean（用户自定义的）和 `spring.factories`（框架内置的 AWS/Eureka 定位器）。合并后按 `@Order` 排序。

---

## 第四部分：ZonePreferenceFilter--区域优先路由（核心）

### 完整逻辑

```java
public List<E> filter(List<E> entities) {
    // 1. 少于 2 个实体、功能关闭、偏好关闭 -> 返回原列表
    if (totalSize <= 1 || !zoneContext.isEnabled() || !zoneContext.isPreferenceEnabled())
        return entities;

    // 2. 当前区域未知或为 defaultZone -> 返回原列表
    if (isIgnored(zone)) return entities;

    // 3. 过滤掉禁用区域的实体
    if (disabledZone != null) {
        targetEntities = filterDisabledZone(entities, disabledZone, totalSize);
        if (targetEntities.size() <= 1) return entities;  // 过滤后太少，放弃
    }

    // 4. 找出同区域实体
    for (E entity : targetEntities) {
        String resolvedZone = zoneResolver.resolve(entity);
        if (resolvedZone != null) {
            zoneCount++;
            if (zone.equals(resolvedZone)) sameZoneEntities.add(entity);
        }
    }

    // 5. 安全阈值一：上游就绪率
    // zoneCount / totalSize * 100 < zoneReadyPercentage(默认100%)
    // 注意：totalSize 是禁用区域过滤后的数量，不是原始数量
    if (isUpstreamZoneNotReady(zoneCount, totalSize, upstreamReadyPercentage))
        return targetEntities;

    // 6. 安全阈值二：同区域最小可用数
    // sameZoneEntities.size() < sameZoneMinAvailable(默认5)
    // 意思是：同区域实例太少（< 5），不应用偏好（防止过载）
    if (sameZoneEntitiesSize < sameZoneMinAvailable)
        return targetEntities;

    // 7. 全部通过 -> 返回同区域实体
    return sameZoneEntities;
}
```

### 两个安全阈值的工程意义

**阈值一：zone-ready-percentage = 100%**

100 个实例中只有 99 个有区域信息 -> 就绪率 99% < 100% -> 不应用偏好。

这个默认值极其保守。生产环境中总有实例配置不一致（新部署的实例还没配区域、老实例配置错了）。100% 意味着**一个没配区域的实例就禁用偏好路由**。

建议生产环境调到 80-90%。但调低后有风险：10% 没配区域的实例不会被偏好路由选中，它们的流量可能全压到其他区域。

**阈值二：same-zone-min-available = 5**

同区域只有 3 个实例 -> 3 < 5 -> 不应用偏好。

防止"同区域实例太少，全部流量压过去导致过载"。5 是一个经验值，具体取决于实例的容量。如果每个实例能扛 1000 QPS，5 个实例能扛 5000 QPS，低于这个值就 fallback 到全部实例。

### 降级策略

任何一步检查不通过，都返回**所有实体**（不应用偏好），不是返回空或抛异常。这保证了：
- 偏好路由失败时流量仍然流通（只是不是区域最优）
- 不会因为区域配置错误导致服务不可用

### ZoneResolver--从实体解析区域

```java
// Spring Cloud ServiceInstance 的区域解析
public class CloudServerZoneResolver implements ZoneResolver<ServiceInstance> {
    public String resolve(ServiceInstance serviceInstance) {
        return serviceInstance.getMetadata().get("microsphere.availability.zone");
    }
}

// Ribbon Server 的区域解析
public class RibbonServerZoneResolver<T extends Server> implements ZoneResolver<T> {
    public String resolve(T server) {
        return server.getZone();  // Ribbon Server 自带 zone 字段
    }
}

// Eureka InstanceInfo 的区域解析
public class EurekaInstanceInfoZoneResolver implements ZoneResolver<InstanceInfo> {
    public String resolve(InstanceInfo instanceInfo) {
        return instanceInfo.getMetadata().get("microsphere.availability.zone");
    }
}
```

三个 ZoneResolver 适配三种不同的服务实例类型。核心逻辑一样：从 metadata 中读 `microsphere.availability.zone`。Ribbon 例外，直接用 `Server.getZone()`（Ribbon 内置的 zone 字段）。

---

## 第五部分：ZoneAttachmentHandler--区域信息传播

### 问题

服务注册时，注册中心怎么知道这个实例在哪个区域？

### 代码

```java
public class ZoneAttachmentHandler {
    public void attachZone(Map<String, String> metadata) {
        String zone = zoneContext.getZone();
        if (isNotBlank(zone)) {
            metadata.put("microsphere.availability.zone", zone);
        }
    }
}
```

### 两条注册路径

**Spring Cloud 通用路径**：
```java
public class ZoneAttachmentListener
        implements ApplicationListener<RegistrationPreRegisteredEvent> {
    public void onApplicationEvent(RegistrationPreRegisteredEvent event) {
        Registration registration = event.getRegistration();
        zoneAttachmentHandler.attachZone(registration.getMetadata());
    }
}
```

监听 `RegistrationPreRegisteredEvent`（来自 05-spring-cloud 的 `EventPublishingRegistrationAspect`），在注册前把区域写入 metadata。

**Netflix Eureka 专用路径**：
```java
public class ZoneAttachmentPreRegistrationHandler implements PreRegistrationHandler {
    public void beforeRegistration() {
        InstanceInfo instanceInfo = applicationInfoManager.getInfo();
        zoneAttachmentHandler.attachZone(instanceInfo.getMetadata());
    }
}
```

实现 Eureka 的 `PreRegistrationHandler` SPI，在 Eureka 注册前写入。

### 信息传播链

```
ZoneLocator 定位区域 -> ZoneContext 存储区域
  -> ZoneAttachmentHandler 注册时写入 metadata
    -> 注册中心存储
      -> 其他服务通过 DiscoveryClient 获取实例列表
        -> CloudServerZoneResolver 从 metadata 读取区域
          -> ZonePreferenceFilter 按区域过滤
```

---

## 第六部分：ZoneContextChangedListener--动态重配置

### 这是最重要的文件之一（251 行）

```java
public class ZoneContextChangedListener implements SmartApplicationListener, ApplicationContextAware, EnvironmentAware {

    // 监听事件（根据环境不同，监听 1-2 种）
    // Spring Cloud 应用：EnvironmentChangeEvent + ApplicationStartedEvent
    // Spring Boot 应用（无 Cloud）：ApplicationStartedEvent
    // 非 Boot Spring：ContextRefreshedEvent
    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        if (IS_SPRING_CLOUD_APPLICATION) {
            return ENVIRONMENT_CHANGE_EVENT_CLASS.isAssignableFrom(eventType)  // Spring Cloud 配置变更
                || APPLICATION_STARTED_EVENT_CLASS.isAssignableFrom(eventType); // Spring Boot 启动
        } else if (IS_SPRING_BOOT_APPLICATION) {
            return APPLICATION_STARTED_EVENT_CLASS.isAssignableFrom(eventType);
        } else {
            return CONTEXT_REFRESHED_EVENT_CLASS.isAssignableFrom(eventType);  // 非 Boot fallback
        }
    }

    public void onApplicationEvent(ApplicationEvent event) {
        tryChangeZoneContext();  // 重新读取所有区域配置
    }

    private void tryChangeZoneContext() {
        List<PropertyChangeEvent> events = new LinkedList<>();
        PropertyChangeListener listener = events::add;
        zoneContext.addPropertyChangeListener(listener);  // 临时监听

        // 逐个读取 Environment 中的区域属性，更新 ZoneContext
        for (String propertyName : ZONE_CONTEXT_PROPERTY_NAMES) {
            Consumer<String> handler = propertyChangedHandlers.get(propertyName);
            handler.accept(propertyName);  // 如果值变了，ZoneContext 触发 PropertyChangeEvent
        }

        zoneContext.removePropertyChangeListener(listener);

        if (!events.isEmpty()) {
            // 有属性变更 -> 发布 Spring ApplicationEvent
            context.publishEvent(new ZoneContextChangedEvent(context, zoneContext, events));
        }
    }
}
```

### 动态切换流程

```
运维在 Nacos 配置中心修改：microsphere.availability.zone=shanghai-1
  -> Spring Cloud 发布 EnvironmentChangeEvent
    -> ZoneContextChangedListener.onApplicationEvent()
      -> 读取 microsphere.availability.zone = "shanghai-1"
      -> ZoneContext.setZone("shanghai-1")
        -> PropertyChangeSupport 触发 "zone" 属性变更
      -> 收集 PropertyChangeEvent
      -> 发布 ZoneContextChangedEvent（Spring ApplicationEvent）
        -> 08-redis 监听 -> 切换 Redis 连接到 shanghai-1
        -> 18-dynamic 监听 -> 重建 DataSource 连接到 shanghai-1
        -> LoadBalancer 下次请求用新区域过滤
```

**不需要重启应用。** 配置中心改一下，区域就切换了。

### "originalZone" 回退机制

```java
private void changeZone(String propertyName) {
    String zone = getProperty(propertyName, ORIGINAL_ZONE);
    if (ORIGINAL_ZONE.equalsIgnoreCase(zone)) {
        zone = revertOriginalZone();  // 重新调用 ZoneLocator 定位
    }
    zoneContext.setZone(zone);
}

private String revertOriginalZone() {
    return zoneLocator.locate(environment);  // 重新自动定位
}
```

如果把 `microsphere.availability.zone` 设为 `originalZone`，会重新调用 ZoneLocator 自动定位。这用于"手动切换到上海后想切回自动检测"的场景。

---

## 第七部分：负载均衡集成

### Spring Cloud LoadBalancer（Reactive）

```java
public class ZonePreferenceServiceInstanceListSupplier
        extends DelegatingServiceInstanceListSupplier {

    public Flux<List<ServiceInstance>> get() {
        return getDelegate().get().map(this::filteredByZone);
    }

    private List<ServiceInstance> filteredByZone(List<ServiceInstance> serviceInstances) {
        return zonePreferenceFilter.filter(serviceInstances);
    }
}
```

**注意：不是自动启用的。** `CustomizedLoadBalancerClientConfiguration` 有条件：

```java
static class OptimizedZoneConfigurationCondition implements Condition {
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return LoadBalancerEnvironmentPropertyUtils.equalToForClientOrDefault(
            context.getEnvironment(), "configurations", "optimized-zone-preference");
    }
}
```

需要配置 `spring.cloud.loadbalancer.configurations=optimized-zone-preference` 才启用。这是 opt-in 设计，避免意外改变路由行为。

### Netflix Ribbon（Blocking）

```java
public class ZonePreferenceServerListFilter<T extends Server> implements ServerListFilter<T> {
    public List<T> getFilteredListOfServers(List<T> servers) {
        return filter.filter(servers);
    }
}
```

Ribbon 的 `ServerListFilter` 适配器。同时有 `DiscoveryClientServer` 和 `DiscoveryClientServerList` 把 Spring Cloud 的 `DiscoveryClient` 适配为 Ribbon 的 `Server` 和 `ServerList`。

`DiscoveryClientServer` 在构造时调用 `CloudServerZoneResolver.resolve()` 设置 `Server.setZone()`：

```java
public DiscoveryClientServer(ServiceInstance serviceInstance) {
    super(serviceInstance.getScheme(), serviceInstance.getHost(), serviceInstance.getPort());
    this.setZone(resolveZone(serviceInstance));  // 从 metadata 读取区域
}
```

---

## 第八部分：分布式问题分析

### 问题一：切换窗口

区域从 beijing-1 切换到 shanghai-1 时：

| 组件 | 切换时机 | 切换耗时 |
|------|---------|---------|
| LoadBalancer | 下一个请求立即生效 | 0ms |
| Redis | 监听 ZoneContextChangedEvent -> 重建连接池 | 秒级（关闭旧池 + 创建新池） |
| DataSource | 监听 ZoneContextChangedEvent -> 重建子上下文 | 数秒到数十秒 |

**切换窗口内**：LoadBalancer 已经路由到 shanghai-1 的服务实例，但 Redis/DataSource 还连着 beijing-1。请求到 shanghai-1 的服务，数据却读写 beijing-1 的 Redis/DB。

这不是 microsphere 的 bug，是所有多活系统的共性问题。标准解法是"先切数据，后切路由"：
1. 先在 shanghai-1 建立 Redis/DB 连接
2. 等数据同步完成
3. 再切换 LoadBalancer 路由

但 microsphere 的切换是同时触发的（一个事件），没有顺序保证。

### 问题二：区域故障检测

microsphere-multiactive **没有区域故障检测机制**。它只提供"手动切换"（通过配置中心改 zone 属性）和"禁用区域"（通过 `preference.upstream.disabled-zone`）。

如果 beijing-1 整个机房挂了：
- microsphere 不会自动检测到
- 需要运维手动在配置中心改 `microsphere.availability.zone=shanghai-1`
- 或者改 `microsphere.availability.zone.preference.upstream.disabled-zone=beijing-1` 禁用北京

对比：Spring Cloud 的 `HealthCheckHandler` 可以自动摘除不健康实例。但 microsphere-multiactive 没有集成健康检查。

### 问题三：数据一致性

异地多活的核心难题是数据一致性。两个区域同时写同一个 key，怎么处理冲突？

microsphere-multiactive **不处理数据一致性**。它只负责路由和切换，不负责数据同步。数据同步由：
- 08-redis 的 Kafka 复制（最终一致，有延迟窗口）
- 数据库的 Binlog 同步（如 Canal + Kafka）
- 应用层的数据同步逻辑

这是一个合理的关注点分离：路由层不管数据，数据层不管路由。但用户需要自己解决数据一致性问题。

### 问题四：和容错引擎的配合

区域偏好路由和限流/熔断的关系：

```
请求进来 -> LoadBalancer 区域偏好过滤 -> 选出同区域实例
  -> Ribbon/LoadBalancer 负载均衡 -> 选一个实例
    -> Sentinel/Resilience4j 限流熔断 -> 判断是否允许调用
      -> 实际调用
```

区域偏好先于限流熔断。如果同区域实例全部被熔断（CircuitBreaker OPEN），LoadBalancer 不会自动 fallback 到其他区域--它只看到同区域实例列表（已经被 ZonePreferenceFilter 过滤了）。

解法：把 `same-zone-min-available` 设高一点，确保有足够的同区域实例。或者配合 `disabled-zone` 手动禁用故障区域。

---

## 第九部分：架构对比

### 和 Spring Cloud 内置 ZonePreference 的对比

| | Spring Cloud 内置 | microsphere-multiactive |
|--|-----------------|----------------------|
| 区域发现 | 无（需要用户手动配置 `spring.cloud.loadbalancer.zone`） | 4 个 ZoneLocator 自动发现（AWS EC2/ECS + property） |
| 安全阈值 | 无 | 两个阈值（就绪率 + 最小可用数） |
| 动态切换 | 无 | EnvironmentChangeEvent 触发 |
| 资源联动 | 无 | ZoneContextChangedEvent 传播到 Redis/DataSource |
| 降级策略 | 简单 fallback | 多级 fallback（禁用区域 -> 就绪率检查 -> 最小可用数检查） |

### 和阿里异地多活方案的对比

| | 阿里异地多活 | microsphere-multiactive |
|--|-----------|----------------------|
| 数据同步 | 自研 DTS（Data Transmission Service） | 不处理（交给 08-redis Kafka 复制或外部方案） |
| 路由控制 | 自研单元化路由（按用户 ID 路由到对应单元） | 按区域偏好路由（不区分用户） |
| 故障切换 | 自动检测 + 自动切换 | 手动切换（通过配置中心） |
| 一致性保证 | 强一致（单元内强一致，跨单元最终一致） | 不保证（依赖应用层） |
| 复杂度 | 极高（需要改造应用支持单元化） | 低（只需要配置区域属性） |

microsphere-multiactive 是"轻量级多活"--不处理数据同步和单元化，只解决路由和切换。适合中小规模的多活场景，不适合阿里级别的超大规模。

---

## 附：整合方案记录（待实现）

### 目标

将 microsphere-multiactive 的完整能力整合到 my-xhs 项目中，使简历描述的"异地多活架构"全部落地：统一区域定位抽象、同区域优先路由、安全保护策略、配置中心秒级切换与回滚。

### 现状（my-xhs 已有）

```
my-xhs-common/src/main/java/com/myxhs/common/zone/
├── ZoneContext.java                 # 单例区域状态（已实现，无需改）
├── ZoneContextAutoConfiguration.java # 自动配置（已实现，需扩展）
├── ZoneConstants.java              # 常量（已实现，需扩展）
├── ZoneProperties.java             # 配置属性（已实现，需扩展）
├── ZoneResolver.java               # 区域解析接口（已实现，无需改）
├── ZonePreferenceFilter.java       # 区域优先过滤器（已实现，无需改）
├── loadbalancer/
│   ├── ServiceInstanceZoneResolver.java         # ServiceInstance 解析器（已实现，无需改）
│   ├── ZonePreferenceServiceInstanceListSupplier.java  # LoadBalancer 集成（已实现，无需改）
│   └── ZoneLoadBalancerConfiguration.java       # LoadBalancer 配置（已实现，无需改）
└── datasource/
    └── DynamicDataSource.java      # 动态数据源（已实现，切换等待比 microsphere 更完善）
```

### 差距分析

| 简历要求 | 缺失机制 | 缺失文件数 | 参考来源 |
|---------|---------|-----------|---------|
| 统一区域定位（AWS EC2/ECS/本地） | ZoneLocator SPI + 多个实现 | 6 个 | microsphere 的 ZoneLocator、CompositeZoneLocator、AbstractZoneLocator、Ec2AvailabilityZoneEndpointZoneLocator、EcsContainerMetadataFileZoneLocator、EcsTaskMetadataEndpointV4ZoneLocator |
| 配置中心秒级切换 | ZoneContextChangedListener（EnvironmentChangeEvent 监听） | 2 个 | microsphere 的 ZoneContextChangedListener、ZoneContextChangedEvent |
| 区域回滚到自动检测 | originalZone 回退机制 | 嵌入在 ZoneContextChangedListener 中 | microsphere 的 changeZone() + revertOriginalZone() |
| 区域信息注册到 Nacos | ZoneAttachmentHandler（注册前写入 metadata） | 1 个 | microsphere 的 ZoneAttachmentHandler + ZoneAttachmentListener + ZoneCloudAutoConfiguration |

### 完整文件方案

#### Phase 1：核心能力（必须）

| 文件 | 行数 | 说明 |
|------|------|------|
| `zone/locator/ZoneLocator.java` | 20 | SPI 接口：supports(Environment) + locate(Environment) |
| `zone/locator/AbstractZoneLocator.java` | 30 | 抽象基类：实现 Order 和 BeanNameAware |
| `zone/locator/CompositeZoneLocator.java` | 100 | 组合定位器：按 Order 排序，逐个尝试，缓存结果 |
| `zone/locator/EnvironmentZoneLocator.java` | 30 | 从环境变量 MYXHS_ZONE 读取区域 |
| `zone/locator/PropertyZoneLocator.java` | 30 | 从 application.yml 的 myxhs.availability.zone 读取 |
| `zone/listener/ZoneContextChangedEvent.java` | 40 | 区域变更 Spring ApplicationEvent |
| `zone/listener/ZoneContextChangedListener.java` | 150 | 监听 EnvironmentChangeEvent，动态重配置 |

#### Phase 2：AWS 支持（可选，按需启用）

| 文件 | 行数 | 说明 |
|------|------|------|
| `zone/locator/aws/Ec2MetadataEndpointZoneLocator.java` | 60 | EC2 元数据端点 HTTP 请求 |
| `zone/locator/aws/EcsContainerMetadataFileZoneLocator.java` | 70 | ECS 容器元数据文件 |
| `zone/locator/aws/EcsTaskMetadataV4ZoneLocator.java` | 70 | ECS Task Metadata V4 端点 |

#### Phase 3：区域信息传播（可选）

| 文件 | 行数 | 说明 |
|------|------|------|
| `zone/ZoneAttachmentHandler.java` | 40 | 区域信息写入 metadata |
| `zone/listener/ZoneAttachmentListener.java` | 30 | 监听注册事件，写入 zone |
| `zone/config/ZoneCloudAutoConfiguration.java` | 40 | 区域传播的自动配置 |

### 定位器优先级设计

```
Order  定位器                      场景
───── ────────────────────────── ────────────────────────
5     EcsContainerMetadataFile    AWS ECS（容器元数据文件）
10    EcsTaskMetadataV4           AWS ECS（新版端点）
15    Ec2MetadataEndpoint         AWS EC2
30    EnvironmentZoneLocator      K8s/Docker/物理机（环境变量 MYXHS_ZONE）
40    PropertyZoneLocator         本地开发（application.yml）

优先级说明：环境特定 > 通用配置。AWS 环境中 EC2/ECS 定位器优先，
非 AWS 环境它们超时后自动 fallback 到环境变量/配置文件。
```

### ZoneContextAutoConfiguration 扩展方案

```java
@AutoConfiguration
@ConditionalOnProperty(prefix = "myxhs.availability.zone", name = "enabled", matchIfMissing = true)
public class ZoneContextAutoConfiguration {

    @Bean
    public ZoneContext zoneContext(ZoneProperties zoneProperties, Environment environment) {
        ZoneContext zoneContext = ZoneContext.get();
        // ... 设置 enabled、preference、thresholds ...

        // 区域来源：ZoneLocator 优先，兜底到 Nacos metadata
        ZoneLocator locator = compositeZoneLocator(environment);
        String zone = locator.locate(environment);
        if (zone == null) {
            zone = environment.getProperty("spring.cloud.nacos.discovery.metadata.zone", "defaultZone");
        }
        zoneContext.setZone(zone);
        return zoneContext;
    }
}
```

### ZoneContextChangedListener 方案

```java
@Component
public class ZoneContextChangedListener
        implements ApplicationListener<EnvironmentChangeEvent>, ApplicationContextAware, EnvironmentAware {

    // 当 Nacos 配置中心的 myxhs.availability.zone 变更时触发
    // 1. 读取新值
    // 2. ZoneContext.setZone(newZone)
    // 3. PropertyChangeSupport fires "zone" changed
    // 4. DynamicDataSource.onZoneChanged() -> switchDataSource(newZone)
    //   （已有，含活跃连接等待逻辑）
    // 5. 如果新值是 "" 或 null -> fallback 到默认行为
    // 6. 如果新值是 "originalZone" -> 重新调用 ZoneLocator.locate() 自动检测
}
```

### 和已有 DynamicDataSource 的衔接

my-xhs 的 DynamicDataSource 已实现 `PropertyChangeListener` 监听 `zone` 属性变更：

```java
private final PropertyChangeListener zoneChangeListener = this::onZoneChanged;
// ZoneContext.get().addPropertyChangeListener(zoneChangeListener);  // afterPropertiesSet
// ZoneContext.get().removePropertyChangeListener(zoneChangeListener); // destroy
```

新增 ZoneContextChangedListener 触发 ZoneContext.setZone() -> PropertyChangeSupport fires -> DynamicDataSource.onZoneChanged() 自动响应。不需要改 DynamicDataSource 的任何代码。

### 配置项

```yaml
myxhs:
  availability:
    zone:
      enabled: true                   # 区域功能总开关
      locator:
        fast-fail: false              # 定位失败是否阻止启动
        timeout: 3000                 # 元数据端点超时
    preference:
      enabled: false                  # 区域优先路由开关（默认关闭）
      filter:
        order: 10                     # 过滤器顺序
      upstream:
        zone-ready-percentage: 100    # 上游区域就绪百分比（保守：100%）
        same-zone-min-available: 5    # 同区域最小可用实例数
        disabled-zone:                # 禁用的上游区域（逗号分隔）
```

### 和面试简历的逐条对应

| 简历 | 实现方式 | 状态 |
|------|---------|------|
| "构建在 Spring Boot 框架，实现统一 Availability Zone 定位抽象" | ZoneLocator SPI + 6 个实现，覆盖 EC2/ECS/K8s/物理机/本地 | ⏳ 待实现 |
| "无论 Java 应用部署在 AWS EC2/ECS 容器环境，还是本地机器" | ECS locator -> EC2 locator -> 环境变量 locator -> property locator | ⏳ 待实现 |
| "服务调用、数据存储同区域访问" | ZonePreferenceFilter（服务调用）+ DynamicDataSource（数据存储） | ✅ 已有 |
| "内建保护策略，防止单区域负载过高" | same-zone-min-available 阈值（默认 5） | ✅ 已有 |
| "配置中心调整策略，秒级切换到非故障区域" | ZoneContextChangedListener 监听 Nacos EnvironmentChangeEvent | ⏳ 待实现 |
| "通过回滚策略配置，秒级恢复到同区域优先" | originalZone 回退机制触发 ZoneLocator 重新定位 | ⏳ 待实现 |
| "节点分布在北美、中国大陆、欧洲" | 各区域部署的实例通过 ZoneLocator + Nacos metadata 自动识别区域 | ⏳ 待实现后可达 |
| "通过响应时间监控对比前后，整体减少 10-30%" | 同区域路由减少跨区域延迟，这是结果指标 | ⏳ 待验证 |

### 代码行数估算

| 阶段 | 文件数 | 行数 | 依赖 |
|------|--------|------|------|
| Phase 1（核心） | 6 | ~370 | 无 |
| Phase 2（AWS） | 3 | ~200 | AWS SDK（可选） |
| Phase 3（传播） | 3 | ~110 | microsphere-spring-cloud（可选） |
| **总计** | **12** | **~680** | |

当前 my-xhs zone 包已有 15 个文件 ~1200 行，加上这些后共 ~27 个文件 ~1900 行，覆盖完整的异地多活能力。
