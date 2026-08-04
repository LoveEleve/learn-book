# 17-05 项目定位、使用与工程分析

## 目录

- [这是个什么项目](#这是个什么项目)
- [5 个子模块](#5-个子模块)
- [快速开始：从零集成](#快速开始从零集成)
- [完整配置参考手册](#完整配置参考手册)
- [核心概念](#核心概念)
- [ZoneLocator 体系：4 种定位器](#zonelocator-体系4-种定位器)
- [ZonePreferenceFilter：10 步决策树](#zonepreferencefilter10-步决策树)
- [动态切换机制](#动态切换机制)
- [与 18-dynamic 的集成链路](#与-18-dynamic-的集成链路)
- [工程问题分析](#工程问题分析)

---

## 这是个什么项目

`microsphere-multiactive` 是一个异地多活基础组件。

**一句话定位**：让微服务具备"同区域优先调用"能力——北京的服务优先调北京的实例、北京的 Redis 连北京的 Redis，区域切换时全局联动。

### 它解决的核心问题

在异地多活架构中，最基础的需求是"同区域优先"：

```
北京机房的服务 A 调用服务 B
  → 注册中心返回 [北京-B1, 北京-B2, 上海-B3, 上海-B4]
  → 理想：只调北京-B1/B2（同区域，延迟 <1ms）
  → 北京全挂：fallback 到上海-B3/B4（跨区域，延迟 ~30ms 但可用）
```

Spring Cloud 有 `ZonePreferenceServiceInstanceListSupplier`，但只解决了"选同区域实例"这一步。缺的东西很多：

| 缺失能力 | 说明 | microsphere-multiactive 的方案 |
|---------|------|----------------------------------|
| **区域发现** | 怎么知道当前实例在哪个区域？ | `ZoneLocator` SPI + 4 种实现（EC2 元数据 / ECS 容器文件 / ECS Task 端点 / 默认 System property） |
| **安全路由** | 同区域只有 1 个实例时全压给它会不会挂？ | `ZonePreferenceFilter` 的 10 步决策树 + `sameZoneMinAvailable` + `zoneReadyPercentage` |
| **动态切换** | 配置中心改区域配置，运行时立即生效？ | `ZoneContext` 的 `PropertyChangeSupport` → `ZoneContextChangedListener` |
| **资源联动** | 区域切换时 Redis/DataSource 跟着切？ | `ZoneContextChangedEvent` → 外部监听（18-dynamic 的 `PropagatingDynamicJdbcConfigChangedEventListener`） |
| **多云支持** | AWS EC2/ECS、Eureka、K8s 的区域信息来源不同 | 4 个 ZoneLocator 实现 + `CompositeZoneLocator` 组合模式 |

### 与其他方案的关系

| 方案 | 定位 | 与 17 的关系 |
|------|------|-------------|
| Spring Cloud `ZonePreferenceServiceInstanceListSupplier` | 最简单的区域偏好 | 17 的 `ZonePreferenceServiceInstanceListSupplier` 是对它的替代，加了安全阈值和 fallback |
| 阿里单元化路由 | 全量业务路由 + 数据分片 | 17 是轻量级方案，只做"区域感知负载均衡"，不做数据分片 |
| 18-dynamic | 数据库层的 Zone 感知 | 通过 `ZoneContextChangedEvent` 与 17 集成，Zone 切换时重建 DataSource |

---

## 6 个子模块

| 子模块 | 文件数 | 依赖 | 职责 |
|--------|-------|------|------|
| `microsphere-multiactive-commons` | 6 | 无（纯 Java） | `ZoneContext` 单例、`ZonePreferenceFilter` 决策树、`ZoneResolver` SPI、`ZoneAttachmentHandler` 区域信息传播 |
| `microsphere-multiactive-spring` | 7 | Spring Framework | `ZoneLocator` SPI、`CompositeZoneLocator`、`DefaultZoneLocator`、`ZoneContextChangedEvent`、`ZoneContextChangedListener` |
| `microsphere-multiactive-spring-boot` | 3 | Spring Boot | `ZoneAutoConfiguration`、条件注解 |
| `microsphere-multiactive-spring-cloud` | 6 | Spring Cloud | `ZonePreferenceServiceInstanceListSupplier`、`CloudServerZoneResolver`、`ZoneAttachmentListener` |
| `microsphere-multiactive-netflix` | 6 | Eureka + Ribbon | `EurekaInstanceInfoZoneResolver`、`RibbonServerZoneResolver`、`ZonePreferenceServerListFilter`、区域信息广播 |
| `microsphere-multiactive-aws` | 3 | AWS SDK | 3 个 AWS ZoneLocator 实现（EC2 元数据 / ECS 容器文件 / ECS Task 端点） |

### 模块依赖关系

```
commons（零依赖）
  └─ spring（依赖 Spring Framework）
       ├─ spring-boot（依赖 Spring Boot Auto-Configuration）
       ├─ spring-cloud（依赖 Spring Cloud LoadBalancer）
       ├─ netflix（依赖 Eureka Client + Ribbon）
       └─ aws（依赖 AWS SDK，与 netflix 平行）
```

---

## 快速开始：从零集成

### Maven 依赖

最简单的用法（只用 ZoneContext + 配置文件指定区域）：

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-multiactive-spring-boot</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

如果需要 Spring Cloud LoadBalancer 集成：

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-multiactive-spring-cloud</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

如果需要 Eureka/Ribbon 集成：

```xml
<dependency>
    <groupId>io.github.microsphere-projects</groupId>
    <artifactId>microsphere-multiactive-netflix</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 最简配置

```properties
# 指定当前实例所在区域
microsphere.availability.zone=beijing-1

# 开启区域优先路由（默认关闭，安全考虑）
microsphere.availability.zone.preference-enabled=true
```

Spring Boot 启动后：
1. `ZoneAutoConfiguration` 检测到区域相关配置（`@ConditionalOnAvailabilityZoneAvailable/Enabled`）
2. 收集所有 ZoneLocator Bean，创建 `CompositeZoneLocator`
3. `compositeZoneLocator.locate()` 定位当前区域（`DefaultZoneLocator` 从配置 `microsphere.availability.zone` 读取）
4. 结果写入 `ZoneContext` 单例（`ZoneContext.get().setZone(...)`）
5. `ZonePreferenceFilter` 在 LoadBalancer 中生效，同区域实例优先

### 在代码中使用

```java
// 获取当前区域
String currentZone = ZoneContext.get().getZone();

// 获取区域上下文（用于监听区域变更）
ZoneContext zoneContext = ZoneContext.get();
zoneContext.addPropertyChangeListener(evt -> {
    if ("zone".equals(evt.getPropertyName())) {
        String newZone = (String) evt.getNewValue();
        // 区域变更时的处理逻辑
    }
});
```

### 完整的配置示例

```properties
# 区域基础配置
microsphere.availability.zone=beijing-1
microsphere.availability.zone.enabled=true

# 区域偏好路由配置
microsphere.availability.zone.preference-enabled=true
microsphere.availability.zone.preference.filter-order=10
microsphere.availability.zone.preference.upstream.zone-ready-percentage=80
microsphere.availability.zone.preference.upstream.same-zone-min-available=3
microsphere.availability.zone.preference.upstream.disabled-zone=

# ZoneLocator 配置
microsphere.availability.zone.locator.fast-fail=false
microsphere.availability.zone.locator.timeout=3000
```

---

## 完整配置参考手册

### 区域基础配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `microsphere.availability.zone` | — | 当前区域（如 `beijing-1`、`shanghai-2`） |
| `microsphere.availability.zone.enabled` | `true` | 区域功能总开关 |

### 区域偏好路由配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `microsphere.availability.zone.preference-enabled` | `false` | 区域优先路由开关。**默认关闭**——开启后流量分布会变化，要求用户明确确认 |
| `microsphere.availability.zone.preference.filter-order` | `10` | Filter 在 LoadBalancer 链中的顺序 |
| `microsphere.availability.zone.preference.upstream.zone-ready-percentage` | `100` | 区域就绪百分比。上游实例中有区域信息的比例低于此值时，跳过偏好路由 |
| `microsphere.availability.zone.preference.upstream.same-zone-min-available` | `5` | 同区域最小可用实例数。低于此值时，跳过偏好路由 |
| `microsphere.availability.zone.preference.upstream.disabled-zone` | `null` | 禁用的上游区域（逗号分隔）。机房故障时运维设置 |

### ZoneLocator 配置

| 属性 | 默认值 | 说明 |
|------|--------|------|
| `microsphere.availability.zone.locator.fast-fail` | `false` | ZoneLocator 快速失败。true 表示定位器失败时直接抛异常 |
| `microsphere.availability.zone.locator.timeout` | `3000` | ZoneLocator 超时时间（ms） |

---

## 核心概念

### ZoneContext——全局区域状态

`ZoneContext` 是整个模块的核心。它是一个**单例**，持有当前区域和所有偏好配置，通过 `PropertyChangeSupport` 通知变更事件。

```java
public class ZoneContext {
    private static final ZoneContext instance = new ZoneContext();

    private volatile boolean enabled = true;           // 区域功能总开关
    private volatile String zone = "defaultZone";       // 当前区域
    private volatile boolean preferenceEnabled = false; // 偏好路由开关
    private volatile int preferenceFilterOrder = 10;
    private volatile int preferenceUpstreamZoneReadyPercentage = 100;
    private volatile int preferenceUpstreamSameZoneMinAvailable = 5;
    private volatile String preferenceUpstreamDisabledZone = null;

    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
}
```

关键设计点：

| 设计 | 原因 |
|------|------|
| **单例** | 需要在非 Spring 代码中使用（Ribbon Filter、Redis 工厂、MyBatis 拦截器），这些地方拿不到 Spring BeanFactory |
| **volatile 字段** | 所有配置字段都是 `volatile`，保证 `ZonePreferenceFilter.filter()` 无锁读取时看到最新值 |
| **PropertyChangeSupport** | 用 JavaBeans 标准事件机制，而不是 Spring 的 ApplicationEvent——因为 ZoneContext 不是 Spring Bean |
| **preferenceEnabled 默认 false** | 安全第一。开启偏好后流量分布变化，要求用户明确确认 |

### ZoneResolver——从任意实体解析区域

```java
@FunctionalInterface
public interface ZoneResolver<E> extends Function<E, String> {
    String resolve(E entity);
}
```

泛型接口，可以从不同类型的实体中解析区域：

| 实体类型 | 场景 | 实现 |
|---------|------|------|
| `ServiceInstance` | Spring Cloud LoadBalancer | `CloudServerZoneResolver` |
| `Server` | Ribbon 负载均衡 | `RibbonServerZoneResolver` |
| `InstanceInfo` | Eureka 实例 | `EurekaInstanceInfoZoneResolver` |

---

## ZoneLocator 体系：4 种定位器

ZoneLocator 负责"发现当前实例在哪个区域"。与 ZoneResolver 不同——ZoneLocator 定位自己，ZoneResolver 解析别人。

### ZoneLocator（发现自己的区域）

4 种实现：

| 定位器 | 数据来源 | 适用场景 |
|--------|---------|---------|
| `Ec2AvailabilityZoneEndpointZoneLocator` | AWS EC2 元数据端点 | AWS EC2 部署 |
| `EcsContainerMetadataFileZoneLocator` | ECS 容器元数据文件 | AWS ECS 部署 |
| `EcsTaskMetadataEndpointV4ZoneLocator` | ECS Task 元数据端点 v4 | AWS ECS/Fargate 部署 |
| `DefaultZoneLocator` | `ZoneContext.getCurrentZone()`（System property） | 非 AWS 部署（通过配置指定区域） |

### CompositeZoneLocator——组合定位

`CompositeZoneLocator` 持有多个 ZoneLocator，按优先级依次尝试，直到有一个成功：

```
CompositeZoneLocator.locate()
  → 遍历 ZoneLocator 列表（按优先级排序）
    → 调用 ZoneLocator.locate()
    → 成功？→ 返回区域 + 写入 System property
    → 失败？→ 下一个
  → 全部失败？→ 返回 null
```

写入 System property 后，`ZoneContext.getCurrentZone()` 静态方法可以读取到。

ZoneLocator 在 `ZoneAutoConfiguration` 中自动装配：

```
ZoneAutoConfiguration
  → 收集所有 ZoneLocator Bean
  → 创建 CompositeZoneLocator
  → 调用 compositeZoneLocator.locate() 定位区域
  → 写入 ZoneContext.setZone()
```

### 自动装配条件

```java
@Configuration
@ConditionalOnAvailabilityZoneAvailable  // 必须有区域可用
@ConditionalOnAvailabilityZoneEnabled    // 区域功能必须启用
public class ZoneAutoConfiguration {
    // ...
}
```

两个条件注解确保：没有区域配置时不启动区域功能，不启用时也不启动。

---

## ZonePreferenceFilter：10 步决策树

`ZonePreferenceFilter` 是负载均衡过滤器，决定"挑哪些实例给调用方"。它的决策逻辑是一个 10 步的二叉树——每一步要么返回"同区域实例"，要么"完全降级（返回全部实例）"。

### 决策流程

```
filter(entities):
  │
  ├─ Step 1: 实例数 ≤ 1 → 返回全部（不需要偏好）
  │
  ├─ Step 2: enabled == false → 返回全部（总开关关闭）
  │
  ├─ Step 3: preferenceEnabled == false → 返回全部（偏好开关关闭）
  │
  ├─ Step 4: 计算 zoneReadyPercentage
  │   → 遍历 entities，统计有区域信息的比例
  │   → 如果比例 < zoneReadyPercentage（默认 100%），返回全部
  │   → 安全阀：防止区域信息不全时做错误偏好
  │
  ├─ Step 5: 按区域分组 entities
  │   → Map<zone, List<E>>
  │
  ├─ Step 6: 获取当前区域（ZoneContext.getZone()）
  │
  ├─ Step 7: 检查当前区域的实例数
  │   → 如果 < sameZoneMinAvailable（默认 5），返回全部
  │   → 安全阀：防止同区域实例太少时全部压垮
  │
  ├─ Step 8: 检查禁用区域
  │   → 如果当前区域在 disabledZone 中，返回全部
  │
  ├─ Step 9: 检查 upstream 实例是否都是当前区域的
  │   → 全部都是 → 返回全部（不需要过滤）
  │
  └─ Step 10: 返回同区域实例列表
```

### 安全阈值的意义

| 参数 | 默认值 | 作用 | 生产建议 |
|------|--------|------|---------|
| `zoneReadyPercentage` | `100` | 区域信息不全时不做偏好。100% 表示只要有一个实例缺区域信息就降级 | 80-90%（部分老实例可能没打区域标签） |
| `sameZoneMinAvailable` | `5` | 同区域实例太少时全部压过去可能导致过载 | 根据实例的承载能力调整 |
| `disabledZone` | `null` | 机房故障时手动禁用该区域 | 运维紧急时使用 |

### 线程安全

`ZonePreferenceFilter` 不持有可变状态，所有配置读自 `ZoneContext` 的 `volatile` 字段。`filter()` 方法每次调用都是自包含的，没有副作用，可以在多线程环境下安全使用。

---

## 动态切换机制

### 事件传播

```
ZoneContext.setZone("beijing-2")  // 运维调用或配置中心触发
  → PropertyChangeSupport.firePropertyChange("zone", "beijing-1", "beijing-2")
    → ZoneContextChangedListener.propertyChange()
      → 更新 System property（microsphere.current.availability.zone）
      → 发布 ZoneContextChangedEvent（Spring 事件）
        → PropagatingDynamicJdbcConfigChangedEventListener（18-dynamic）
          → DynamicJdbcConfigChangedEvent
            → DynamicDataSource.initializeDataSource() 重建数据源
```

### 变更事件的两条路径

| 路径 | 机制 | 消费者 |
|------|------|--------|
| PropertyChangeSupport | JavaBeans 事件 | ZoneContext 的直接监听器（如 `ZoneContextChangedListener`） |
| ZoneContextChangedEvent | Spring ApplicationEvent | 所有 Spring 的 ApplicationListener（如 `PropagatingDynamicJdbcConfigChangedEventListener`） |

第一条路径用于非 Spring 组件（Ribbon Filter、纯 Java 代码）。第二条路径用于 Spring Bean。

### 重配置流程

`ZoneContextChangedListener`（251 行）监听 `ZoneContext` 的属性变更，将变更传播到 Spring 容器：

```
1. `ZoneContext.setZone("new-zone")` 被调用
2. `PropertyChangeSupport.firePropertyChange("zone", "beijing-1", "new-zone")`
3. `ZoneContextChangedListener.propertyChange()` 收到事件
4. 记录 originalZone（当前 zone 值，用于后续可能的回退）
5. 更新 System property：`microsphere.current.availability.zone` = "new-zone"
6. 发布 `ZoneContextChangedEvent`（Spring 事件）
    → 所有 ApplicationListener 收到通知
    → 包括 18-dynamic 的 PropagatingDynamicJdbcConfigChangedEventListener
```

### originalZone 回退机制

`ZoneContextChangedListener` 在切换前保存 `originalZone`（切换前的 zone 值）。这个 originalZone 本身不会触发自动回退——它只是一个"锚点"记录。当外部系统（健康检查、运维工具）检测到新 zone 不可用时，可以调用 `ZoneContext.setZone(originalZone)` 切回。originalZone 确保无论切了多少次，始终能回到最初的值。

---

## 与 18-dynamic 的集成链路

18-dynamic 通过 `PropagatingDynamicJdbcConfigChangedEventListener` 监听 17 的 `ZoneContextChangedEvent`，实现 Zone 切换时数据库层的自动重建。

### 完整链路

```
Zone 切换触发
  │
  ├─ 路径 A：ZoneContext.setZone() 直接调用（运维脚本/JMX）
  │
  ├─ 路径 B：配置中心修改 microsphere.availability.zone
  │   → Environment 更新
  │   → PropertySourcesChangedEvent（来自 microsphere-configuration）
  │   → ❌ 断点：17 没有监听此事件并调用 setZone() 的组件（见 17-08 分析）
  │   → 需要额外组件补全
  │
  └─ 路径 C：ZoneLocator 重新定位
      → ZoneContext.setZone()

所有有效路径汇集到：
  ZoneContextChangedEvent（17-multiactive 发布）
    │
    ├─ 17 内部的 ZoneContextChangedListener
    │   → 更新 System property
    │   → 更新负载均衡路由
    │
    └─ 18-dynamic 的 PropagatingDynamicJdbcConfigChangedEventListener
        → 检查 config.hasHighAvailabilityDataSource()
        → DynamicJdbcConfigChangedEvent
          → RefreshingDynamicDataSourceListener
            → initializeDataSource() 重建
```

### 动态DataSource 的 Zone 感知

18-dynamic 中 `DynamicJdbcConfig.getDataSourcePropertiesList()` 的 Zone 选择逻辑：

```java
public List<Map<String, String>> getDataSourcePropertiesList() {
    if (hasHighAvailabilityDataSource()) {
        ZoneContext zoneContext = getZoneContext();
        String zone = zoneContext.getZone();
        // 根据 17-multiactive 的 ZoneContext 当前区域选择配置
        List<Map<String, String>> props = highAvailabilityDataSourcePropertiesMap.get(zone);
        if (props == null || props.isEmpty()) {
            // 回退到 defaultZone
            return highAvailabilityDataSourcePropertiesMap.get(DEFAULT_ZONE);
        }
        return props;
    }
    return this.dataSourcePropertiesList;
}
```

### 集成契约

| 契约 | 17 提供 | 18 依赖 |
|------|--------|---------|
| 当前区域 | `ZoneContext.get().getZone()` | `DynamicJdbcConfig.getZoneContext().getZone()` |
| 区域变更通知 | `ZoneContextChangedEvent` | `PropagatingDynamicJdbcConfigChangedEventListener` |
| 区域回退 | `originalZone` 机制 | 18 只响应最终 zone，不处理回退逻辑 |

---

## 工程问题分析

### ZoneContext 为什么用单例

ZoneContext 如果做成 Spring Bean，在非 Spring 环境中就无法使用。Ribbon 的 `ServerListFilter`、Redis 连接工厂、MyBatis 拦截器——这些组件的初始化不在 Spring 容器管理范围内，但都需要读取当前区域。单例 + `ZoneContext.get()` 静态方法没有任何框架依赖。

代价是**不可 mock**。单元测试中不能替换 ZoneContext，只能通过 `System.setProperty` 间接设置区域。

### 为什么不直接用 Spring ApplicationEvent

`ZoneContext` 不是 Spring Bean，所以不能用 `ApplicationEventPublisher.publishEvent()`。选 `PropertyChangeSupport` 的理由：

| 方案 | 优点 | 缺点 |
|------|------|------|
| Spring ApplicationEvent | Spring 原生支持 | ZoneContext 必须变成 Spring Bean，非 Spring 代码拿不到 |
| PropertyChangeSupport | 无框架依赖，任何地方可用 | 事件只有属性名+新旧值，没有复杂的事件类型体系 |

17 的做法是双路径：`PropertyChangeSupport` 触发内部监听器，内部监听器再发布 `ZoneContextChangedEvent`（Spring 事件）。两条路径各管各的消费者。

### 安全默认值

`preferenceEnabled = false` 是安全第一的设计。开启区域偏好后，流量从"均匀分布"变成"同区域优先"，如果同区域实例少，流量全部压过去可能过载。默认关闭要求用户明确知道自己要做什么。

`zoneReadyPercentage = 100` 同样保守。100 个实例中 1 个没有区域信息，就绪率 99% < 100%，不应用偏好。生产环境建议降到 80-90%。

`sameZoneMinAvailable = 5` 防止同区域只有 1-2 个实例时全部压跨。

### ZoneLocator 的故障处理

4 个 ZoneLocator 通过 `CompositeZoneLocator` 组合，按优先级依次尝试。全部失败时返回 `null`，`ZoneAutoConfiguration` 根据 `fast-fail` 配置决定是否启动失败。默认 `fast-fail=false`，允许定位失败时应用继续启动（但不启用区域功能）。

### 与 18-dynamic 的内存开销对比

17-multiactive 本身非常轻量——核心类只有 31 个文件，没有子上下文，没有 Spring 容器重建。它的内存开销可以忽略不计（主要是 ZoneContext 单例 + 几个 ZoneLocator 实例）。

17 的 ZoneContext 变化触发 18 的 DataSource 重建——这是 17 和 18 集成的主要性能影响。Zone 切换本身是 µs 级的（只是属性变更 + 事件发布），但 18 响应事件需要秒级重建。
