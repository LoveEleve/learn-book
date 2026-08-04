# 17-06 功能→代码映射手册

按功能点查找对应的核心类和调用链路。

---

## 功能 1：区域状态管理与配置

**功能**：持有当前区域和所有偏好配置，支持运行时动态修改。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `ZoneContext` | `commons/zone/ZoneContext.java` (235 行) | 全局单例，持有区域状态和配置 |
| `ZoneConstants` | `commons/zone/ZoneConstants.java` (241 行) | 所有配置属性名和默认值常量 |

### 调用链路

```
ZoneContext.get() → 获取单例
  → getZone() → 获取当前区域（从 volatile 字段）
  → setZone("beijing-1") → 设置区域
    → PropertyChangeSupport.firePropertyChange("zone", old, new)
    → ZoneContextChangedListener 收到变更事件
  → isEnabled() / setEnabled()
  → isPreferenceEnabled() / setPreferenceEnabled()
  → 其他 setter：preferenceFilterOrder、zoneReadyPercentage、sameZoneMinAvailable、disabledZone
```

### 关键代码

`ZoneContext.java` — 全部字段和 setter/getter

### 字段一览

| 字段 | 类型 | 默认值 | volatile |
|------|------|--------|----------|
| `enabled` | boolean | `true` | ✅ |
| `zone` | String | `"defaultZone"` | ✅ |
| `preferenceEnabled` | boolean | `false` | ✅ |
| `preferenceFilterOrder` | int | `10` | ✅ |
| `preferenceUpstreamZoneReadyPercentage` | int | `100` | ✅ |
| `preferenceUpstreamSameZoneMinAvailable` | int | `5` | ✅ |
| `preferenceUpstreamDisabledZone` | String | `null` | ✅ |
| `propertyChangeSupport` | PropertyChangeSupport | new instance | ❌ (final) |

所有配置字段都是 `volatile`，保证 `ZonePreferenceFilter.filter()` 无锁读取时看到最新值。

### 配置属性名（来自 ZoneConstants）

```java
// 区域基础属性
String ZONE_PROPERTY_NAME = "microsphere.availability.zone"
String CURRENT_ZONE_PROPERTY_NAME = "microsphere.current.availability.zone"  // System property
String DEFAULT_ZONE = "defaultZone"

// 启用开关
String ZONE_ENABLED_PROPERTY_NAME = ZONE_PROPERTY_NAME + ".enabled"  // 默认 true

// 偏好路由
String PREFERENCE_ENABLED_PROPERTY_NAME  // 默认 false
String PREFERENCE_FILTER_ORDER_PROPERTY_NAME  // 默认 10
String PREFERENCE_UPSTREAM_ZONE_READY_PERCENTAGE_PROPERTY_NAME  // 默认 100
String PREFERENCE_UPSTREAM_SAME_ZONE_MIN_AVAILABLE_PROPERTY_NAME  // 默认 5
String PREFERENCE_UPSTREAM_DISABLED_ZONE_PROPERTY_NAME  // 默认 null

// ZoneLocator
String LOCATOR_FAST_FAIL_PROPERTY_NAME  // 默认 false
String LOCATOR_TIMEOUT_PROPERTY_NAME  // 默认 3000
```

---

## 功能 2：区域定位（ZoneLocator）

**功能**：发现当前实例在哪个区域，支持 AWS（EC2/ECS）和配置文件等多种来源。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `ZoneLocator` | `spring/ZoneLocator.java` | SPI 接口（locate 方法） |
| `AbstractZoneLocator` | `spring/AbstractZoneLocator.java` | 抽象基类（超时 + 异常处理） |
| `CompositeZoneLocator` | `spring/CompositeZoneLocator.java` | 组合定位器，按优先级尝试 |
| `DefaultZoneLocator` | `spring/DefaultZoneLocator.java` | 从 System property 读取区域 |
| `Ec2AvailabilityZoneEndpointZoneLocator` | `aws/.../Ec2AvailabilityZoneEndpointZoneLocator.java` | AWS EC2 元数据端点 |
| `EcsContainerMetadataFileZoneLocator` | `aws/.../EcsContainerMetadataFileZoneLocator.java` | ECS 容器元数据文件 |
| `EcsTaskMetadataEndpointV4ZoneLocator` | `aws/.../EcsTaskMetadataEndpointV4ZoneLocator.java` | ECS Task 元数据端点 v4 |

### 调用链路

```
ZoneAutoConfiguration（Spring Boot 启动时）
  → 收集所有 ZoneLocator Bean
  → 创建 CompositeZoneLocator(locators)
  → compositeZoneLocator.locate()
    → 按 order 排序的 ZoneLocator 列表
    → 遍历，调每个 ZoneLocator.locate()
      → AbstractZoneLocator.locate()（模板方法）
        → 子类实现 doLocate()
        → 超时处理（默认 3000ms）
        → 异常处理（fast-fail 决定是否抛出）
    → 第一个返回非空结果的即为当前区域
    → CompositeZoneLocator 写入 System property：microsphere.current.availability.zone
  → ZoneContext.get().setZone(result) 写入单例
```

### 关键代码

`AbstractZoneLocator.java` — 模板方法 + 超时 + 异常处理
`CompositeZoneLocator.java` — 组合 + 优先级

### ZoneLocator 优先级顺序

| 优先级 | 定位器 | 条件 |
|--------|--------|------|
| 最高 | `Ec2AvailabilityZoneEndpointZoneLocator` | EC2 元数据可用 |
| | `EcsContainerMetadataFileZoneLocator` | ECS 容器元数据文件存在 |
| | `EcsTaskMetadataEndpointV4ZoneLocator` | ECS Task 元数据端点可用 |
| 最低 | `DefaultZoneLocator` | 总是可用（读 System property） |

---

## 功能 3：区域优先路由（ZonePreferenceFilter）

**功能**：在负载均衡时，优先选择同区域的实例。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `ZonePreferenceFilter` | `commons/zone/ZonePreferenceFilter.java` (185 行) | 核心过滤器 |
| `ZoneContext` | `commons/zone/ZoneContext.java` | 提供配置 |
| `ZoneResolver` | `commons/zone/ZoneResolver.java` (24 行) | SPI：从实体解析区域 |

### 调用链路

```
filter(entities):
  │
  ├─ Step 1: entities.size ≤ 1 → 返回全部（不需要偏好）
  ├─ Step 2: !enabled → 返回全部（总开关关闭）
  ├─ Step 3: !preferenceEnabled → 返回全部（偏好关闭）
  ├─ Step 4: 统计 zoneReadyPercentage
  │   → 有区域信息的实体数 / 总数 < zoneReadyPercentage？
  │   → 是 → 返回全部
  ├─ Step 5: 按区域分组 → Map<zone, List<E>>
  ├─ Step 6: 获取当前区域（ZoneContext.getZone()）
  ├─ Step 7: 同区域实例数 < sameZoneMinAvailable？
  │    → 是 → 返回全部
  ├─ Step 8: 当前区域在 disabledZone 中？
  │    → 是 → 返回全部
  ├─ Step 9: 所有实例都是当前区域的？
  │    → 是 → 返回全部（不需要过滤）
  └─ Step 10: 返回同区域实例列表
```

### 关键代码

`ZonePreferenceFilter.java` — 构造函数 + `filter()` 10 步决策树

### 与负载均衡器的集成

| 负载均衡器 | 集成类 | 方式 |
|-----------|--------|------|
| Spring Cloud LoadBalancer | `ZonePreferenceServiceInstanceListSupplier` | 替换默认的 Supplier |
| Ribbon | `ZonePreferenceServerListFilter` | 在 ServerList 过滤链中插入 |

---

## 功能 4：区域信息传播（ZoneAttachment）

**功能**：在服务间调用时，通过 HTTP Header 或 RPC Attachment 传递区域信息。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `ZoneAttachmentHandler` | `commons/zone/ZoneAttachmentHandler.java` (43 行) | 区域信息的序列化/反序列化 |
| `ZoneAttachmentListener` | `spring-cloud/event/ZoneAttachmentListener.java` | 监听区域变更，更新 Attachment |
| `ZoneAttachmentPreRegistrationHandler` | `netflix/eureka/ZoneAttachmentPreRegistrationHandler.java` | Eureka 注册时附加区域信息 |
| `HttpUtils` | `commons/zone/HttpUtils.java` (52 行) | HTTP Header 工具 |

### 调用链路

```
服务 A 调用服务 B 之前：
  调用方
    → ZoneAttachmentHandler.attach(headers)
      → 从 ZoneContext 读取当前区域
      → 写入 HTTP Header（如 X-Zone: beijing-1）
  ↓ HTTP 请求
  服务 B 收到请求：
    → ZoneAttachmentHandler.resolve(headers)
      → 从 HTTP Header 中读取区域并返回 String
      → 是否使用调用方区域由业务决定（resolve 本身不设置 ZoneContext）

服务注册时：
  ZoneAttachmentPreRegistrationHandler（Eureka）
    → 读取当前区域
    → 写入 InstanceInfo 的 metadata
    → 其他服务通过 ZoneResolver 解析实例的区域
```

### 关键代码

`ZoneAttachmentHandler.java` — `attach()` 和 `resolve()`
`HttpUtils.java` — Header 读写

---

## 功能 5：区域切换与事件传播

**功能**：区域变更时，通过事件通知所有需要响应的组件。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `ZoneContextChangedListener` | `spring/event/ZoneContextChangedListener.java` (251 行) | 核心事件转发器 |
| `ZoneContextChangedEvent` | `spring/event/ZoneContextChangedEvent.java` | Spring 事件 |

### 调用链路

```
ZoneContext.setZone("new-zone")
  → PropertyChangeSupport.firePropertyChange("zone", "beijing-1", "new-zone")
  → ZoneContextChangedListener.propertyChange() 收到事件
    → 保存 originalZone
    → 更新 System property：microsphere.current.availability.zone
    → 发布 ZoneContextChangedEvent（Spring 事件）
      → 所有 ApplicationListener 收到通知
        → 18-dynamic 的 PropagatingDynamicJdbcConfigChangedEventListener
        → 其他自定义监听器

回退路径：
  → 外部系统检测到新 zone 不可用
  → ZoneContext.setZone(originalZone)
  → 同上事件链路
```

### 关键代码

`ZoneContextChangedListener.java` — `propertyChange()`（实现 PropertyChangeListener；不实现 ApplicationListener，只发布事件）
`ZoneContext.java` — `setZone()` 触发 `PropertyChangeSupport`

---

## 功能 6：自动装配

**功能**：Spring Boot 启动时自动配置区域功能。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `ZoneAutoConfiguration` | `spring-boot/autoconfigure/ZoneAutoConfiguration.java` | 核心自动装配 |
| `ZoneCloudAutoConfiguration` | `spring-cloud/autoconfigure/ZoneCloudAutoConfiguration.java` | Spring Cloud 集成装配 |
| `CustomizedLoadBalancerAutoConfiguration` | `spring-cloud/loadbalancer/CustomizedLoadBalancerAutoConfiguration.java` | LoadBalancer 替换 |
| `CustomizedLoadBalancerClientConfiguration` | `spring-cloud/loadbalancer/CustomizedLoadBalancerClientConfiguration.java` | LoadBalancer 客户端配置 |
| `@ConditionalOnAvailabilityZoneAvailable` | `spring-boot/condition/ConditionalOnAvailabilityZoneAvailable.java` | 必须有 zone 配置才加载 |
| `@ConditionalOnAvailabilityZoneEnabled` | `spring-boot/condition/ConditionalOnAvailabilityZoneEnabled.java` | zone 功能必须启用才加载 |

### 调用链路

```
Spring Boot 启动
  → ZoneAutoConfiguration
    → @ConditionalOnAvailabilityZoneAvailable（检查 zone 配置是否存在）
    → @ConditionalOnAvailabilityZoneEnabled（检查 enabled 是否为 true）
    → 创建 DefaultZoneLocator
    → 创建 CompositeZoneLocator（包括所有 ZoneLocator Bean）
    → compositeZoneLocator.locate() 定位区域
    → ZoneContext.get().setZone(result) 写入

  → ZoneCloudAutoConfiguration
    → 注册 CloudServerZoneResolver
    → 注册 ZoneAttachmentListener

  → CustomizedLoadBalancerAutoConfiguration / CustomizedLoadBalancerClientConfiguration
    → 替换默认的 ServiceInstanceListSupplier 为 ZonePreferenceServiceInstanceListSupplier
```

### 关键代码

`ZoneAutoConfiguration.java` — 核心装配逻辑
`CustomizedLoadBalancerClientConfiguration.java` — LoadBalancer 替换

---

## 功能 7：ZoneResolver 体系

**功能**：从不同类型的上游实例中解析出所属区域。

### 关键类

| 类 | 位置 | 职责 |
|---|------|------|
| `ZoneResolver` | `commons/zone/ZoneResolver.java` (24 行) | SPI 接口 |
| `CloudServerZoneResolver` | `spring-cloud/CloudServerZoneResolver.java` | 从 `ServiceInstance` metadata 解析 |
| `RibbonServerZoneResolver` | `netflix/ribbon/RibbonServerZoneResolver.java` | 从 Ribbon `Server` 解析 |
| `EurekaInstanceInfoZoneResolver` | `netflix/eureka/EurekaInstanceInfoZoneResolver.java` | 从 Eureka `InstanceInfo` metadata 解析 |

### 调用链路

```
ZonePreferenceFilter.filter(entities)
  → 遍历每个 entity
    → zoneResolver.resolve(entity)
      → CloudServerZoneResolver: entity.getMetadata().get("zone")（具体 key 以实现为准）
      → RibbonServerZoneResolver: entity 的元数据
      → EurekaInstanceInfoZoneResolver: instanceInfo.getMetadata().get("zone")
    → 收集所有实体的区域信息
  → 计算 zoneReadyPercentage
  → 按区域分组
```

### ZoneResolver 接口

```java
@FunctionalInterface
public interface ZoneResolver<E> extends Function<E, String> {
    String resolve(E entity);
}
```

---

## 功能索引：按类名查找

| 类名 | 负责的功能 |
|------|-----------|
| `ZoneContext` | 区域状态管理、配置持有、事件触发 |
| `ZoneConstants` | 配置属性名常量 |
| `ZonePreferenceFilter` | 区域优先路由 10 步决策树 |
| `ZoneResolver` | 区域解析 SPI |
| `ZoneAttachmentHandler` | 区域信息序列化/反序列化 |
| `HttpUtils` | HTTP Header 工具 |
| `ZoneLocator` | 区域定位 SPI |
| `AbstractZoneLocator` | 定位器模板（超时 + 异常处理） |
| `CompositeZoneLocator` | 组合定位器（按优先级依次尝试） |
| `DefaultZoneLocator` | 默认定位器（读 System property） |
| `Ec2AvailabilityZoneEndpointZoneLocator` | AWS EC2 元数据定位 |
| `EcsContainerMetadataFileZoneLocator` | AWS ECS 容器文件定位 |
| `EcsTaskMetadataEndpointV4ZoneLocator` | AWS ECS Task 端点定位 |
| `ZoneContextChangedListener` | 区域变更事件转发（251 行核心） |
| `ZoneContextChangedEvent` | 区域变更 Spring 事件 |
| `ZoneAutoConfiguration` | 自动装配入口 |
| `ZoneCloudAutoConfiguration` | Spring Cloud 自动装配 |
| `CustomizedLoadBalancerAutoConfiguration` | LoadBalancer 替换配置 |
| `CustomizedLoadBalancerClientConfiguration` | LoadBalancer 客户端配置 |
| `ZonePreferenceServiceInstanceListSupplier` | LoadBalancer 区域优先 Supplier |
| `CloudServerZoneResolver` | Spring Cloud 实例区域解析 |
| `RibbonServerZoneResolver` | Ribbon 实例区域解析 |
| `EurekaInstanceInfoZoneResolver` | Eureka 实例区域解析 |
| `ZonePreferenceServerListFilter` | Ribbon 区域优先 ServerList 过滤 |
| `ZoneAttachmentListener` | 区域变更后的 Attachment 更新 |
| `ZoneAttachmentPreRegistrationHandler` | Eureka 注册时附加区域信息 |
| `DiscoveryClientServer` | Eureka 实例到 Ribbon Server 适配 |
| `DiscoveryClientServerList` | Eureka 实例列表到 Ribbon ServerList 适配 |
| `ConditionalOnAvailabilityZoneAvailable` | 条件注解：zone 可用 |
| `ConditionalOnAvailabilityZoneEnabled` | 条件注解：zone 启用 |
| `ZoneUtils` | 工具方法 |
