# 17-02：异地多活架构对比与面试

> **核心命题**：microsphere-multiactive 不是唯一的异地多活方案。Spring Cloud 有内置 ZonePreference、阿里有单元化路由、K8s 有 topology zone。它们各自的取舍是什么？面试中被问"设计异地多活系统"怎么答？
> **本文覆盖**：3 套方案对比 + 10 道面试题深度解析。

---

## 对比一：Spring Cloud 内置 ZonePreference

### Spring Cloud 的做法

Spring Cloud LoadBalancer 有一个内置的 `ZonePreferenceServiceInstanceListSupplier`。它的核心逻辑：

```java
// Spring Cloud 内置（不是 microsphere 的版本）
public Flux<List<ServiceInstance>> get() {
    return getDelegate().get().map(instances -> {
        String zone = getZone();
        List<ServiceInstance> sameZoneInstances = instances.stream()
            .filter(i -> zone.equals(i.getZone()))
            .collect(Collectors.toList());
        if (!sameZoneInstances.isEmpty()) {
            return sameZoneInstances;
        }
        return instances;  // 没有同区域实例→返回全部
    });
}
```

### Spring Cloud 的局限

**问题一：区域来源单一**

Spring Cloud 的区域通过 `spring.cloud.loadbalancer.zone` 配置，或者通过 `DiscoveryClient` 的 `getLocalServiceInstance().getZone()` 获取。这要求用户在配置文件中写死区域，或者依赖注册中心返回的区域信息。

如果 100 个实例分布在 3 个区域，部署脚本必须为每个实例配置正确的区域。一个实例的配置错了，区域路由就不可靠。

**问题二：按区域过滤后不做安全校验**

Spring Cloud 的 ZonePreferenceServiceInstanceListSupplier 只是简单地"有同区域实例就用，没有就用全部"。没有就绪率检查、没有最小可用数检查。

假设同区域只有 1 个实例，100% 的流量全压给它。如果这个实例扛不住，服务降级——但这不是 Spring Cloud 能感知的。

**问题三：没有动态切换**

Spring Cloud 的区域在应用启动时确定，运行时不能改。如果区域故障，需要重启应用才能切换到另一个区域。

**问题四：没有区域信息传播**

Spring Cloud 只关注消费者端的区域偏好，不关注生产者端怎么把区域注册到注册中心。区域信息需要用户自己配置和管理。

### 对比总结

| 维度 | Spring Cloud 内置 | microsphere-multiactive |
|------|-----------------|----------------------|
| 区域来源 | 配置文件或 `ServiceInstance.getZone()` | ZoneLocator SPI（AWS 元数据、环境变量、配置文件） |
| 安全阈值 | 无 | zone-ready-percentage + same-zone-min-available |
| 动态切换 | 无 | ZoneContextChangedListener（EnvironmentChangeEvent） |
| 区域传播 | 无 | ZoneAttachmentHandler（注册时写入 metadata） |
| 降级策略 | 同区域为空时返回全部 | 三级降级：原始列表→过滤禁用区域→仅同区域 |
| 多云支持 | 无 | 6 个 ZoneLocator（AWS EC2/ECS + 本地 + K8s） |
| 故障回滚 | 无 | originalZone 回退机制 |

### Spring Cloud 内置方案适合的场景

- 单区域部署，不需要区域路由
- 多区域部署但区域不会变化，且每个区域有足够多的实例
- 区域信息通过部署工具自动配置且不会出错

---

## 对比二：阿里异地多活方案

### 阿里的方案层次

阿里的异地多活方案可以分为三个层次：

**第一层：中间件层——DTS（Data Transmission Service）**

DTS 是阿里云提供的数据同步服务。它支持：
- MySQL、Redis、MongoDB 之间的实时同步
- Binlog 监听 + 增量同步
- 冲突检测与解决机制

DTS 和 microsphere-multiactive 的关系：DTS 解决数据同步，microsphere-multiactive 解决路由切换。两者互不冲突——如果需要在不同区域间同步 Redis/MySQL 数据，DTS 是外部方案。

**第二层：路由层——单元化路由**

阿里的单元化（Unitization）路由不是按"区域偏好"路由，而是按"用户 ID 哈希"路由：

```
用户 ID = 123456789
  → 哈希取模 → 计算出应该在 "beijing-1" 单元
  → 该用户的所有请求强制路由到 beijing-1
  → 即使 beijing-1 延迟较高，也不跨单元
```

这和 microsphere 的区域偏好路由有本质区别：

| | 阿里单元化路由 | microsphere 区域偏好 |
|--|-------------|-------------------|
| 路由依据 | 用户 ID 哈希 | 调用方的区域 |
| 结果 | 同一用户所有请求到同一单元 | 同区域调用优先 |
| 跨区域时 | 强制路由到目标单元 | fallback 到其他区域 |
| 数据一致性 | 单元内强一致，跨单元最终一致 | 不保证（由同步层负责） |
| 复杂度 | 高（需要业务层支持单元化改造） | 低（只改路由配置） |

**第三层：切换层——箭头（MSHA）**

阿里云 MSHA（Multi-Site High Availability）是多活管控平台：
- 自动检测区域故障
- 自动切换流量到备用区域
- 切换前后数据一致性校验
- 切换过程支持灰度/蓝绿发布

### 阿里方案和 microsphere 方案的关系

```
阿里方案（生产级全栈）：
  MSHA（管控）→ 负责故障检测 + 切换决策
  DTS（数据） → 负责跨区域数据同步
  单元化路由（路由）→ 负责流量分发

microsphere-multiactive（轻量级）：
  人工判断 + 配置中心（管控）→ 手动切换
  08-redis Kafka 复制（数据）→ 可选的数据同步
  区域偏好路由（路由）→ 负责流量分发
```

microsphere-multiactive 的定位是"轻量级多活"：

**能做：**
- 区域自动发现和偏好路由
- 配置中心秒级切换
- 区域信息自动传播到 metadata
- 和 DataSource/Redis 联动的 ZoneContextChangedEvent

**不做：**
- 自动故障检测（需要外部健康检查）
- 数据一致性保证（需要外部数据同步方案）
- 单元化路由（需要业务层改造）
- 切换过程自动化（需要 MSHA 级别的平台）

### 适合和不适用的场景

**适合 microsphere 方案的场景：**
- 中等规模（单区域 5-50 个实例）
- 各区域服务独立部署，不共享数据库
- 可以接受手动切换（配置中心改一个属性）
- 数据一致性通过业务层或异步同步解决

**应该用阿里方案的场景：**
- 超大规模（单区域 100+ 实例）
- 单元化路由需求（按用户分片）
- 自动故障检测和切换
- 强数据一致性要求

---

## 对比三：Kubernetes 拓扑区域路由

### K8s 的做法

Kubernetes 1.17+ 引入 topology-aware routing。通过 TopologySpreadConstraints 和 service topologyKeys 控制 Pod 的分布和服务的流量路由。

```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      topologySpreadConstraints:
        - maxSkew: 1
          topologyKey: topology.kubernetes.io/zone
          whenUnsatisfiable: DoNotSchedule
---
apiVersion: v1
kind: Service
spec:
  topologyKeys: ["topology.kubernetes.io/zone", "*"]
```

topologyKeys 控制 Service 的流量分发：
- 优先将流量发送到同一 zone 的 Pod
- 如果同一 zone 没有可用 Pod，fallback 到其他 zone

### K8s 方案的局限

**局限一：只适用于 K8s 环境**

如果你的应用部分运行在 K8s、部分在物理机、部分在 AWS EC2，K8s 的 topology 不适用。microsphere-multiactive 的 ZoneLocator SPI 可以同时覆盖多种部署环境。

**局限二：没有安全阈值**

K8s 的 topologyKeys 没有同区域最小可用数的概念。如果 zone A 只有 2 个 Pod，所有流量依然优先分配给它们。microsphere 的 same-zone-min-available（默认 5）可以防止这个问题。

**局限三：没有动态切换**

K8s 的 topologyKeys 在 Service 定义中固定，运行时不能动态改。microsphere 的配置中心机制可以在不改动 K8s 资源的情况下切换区域。

**局限四：没有应用层区域感知**

K8s 的 topology 只在流量分发层工作。应用代码（如 DynamicDataSource）不知道区域切换，无法自动切换数据库连接。

### K8s 方案 + microsphere 方案的配合

最理想的方案是两者结合：

```
应用部署在 K8s
  └─ K8s 负责基础设施层的区域拓扑（Pod 分布、Service 路由）
  └─ microsphere 负责应用层的区域感知（ZoneContext、DataSource 切换）

应用代码中：
  ZoneLocator 从 K8s Downward API 注入的环境变量中读取 zone
  （K8s 的 topology.kubernetes.io/zone 通过 Downward API 注入）
  
  ZonePreferenceFilter 在应用层做区域偏好路由
  （K8s 的 Service 路由无法区分同一 zone 内的不同实例优先级）
```

K8s 的 topologyKeys: `["topology.kubernetes.io/zone", "*"]`

spring-cloud-kubernetes 可以从 K8s 的 Pod 标签中读取 zone，提供给 ZoneLocator。

---

## 面试题

### 面试题 1：设计一个异地多活系统

面试官问："假设公司需要在北京和上海两个机房部署一个微服务系统，要求两个机房同时提供服务，一个机房故障时另一个机房能接管。你怎么设计？"

**回答框架（从宏观到微观）：**

首先，异地多活和主备（两地三中心）的区别：两地三中心是"一个写、多个读"，异地多活是"都写都读"。选异地多活意味着要处理数据冲突。

第一层——路由：
每个机房部署的服务实例通过 ZoneLocator 知道自己所在机房。服务调用时通过 ZonePreferenceFilter 优先选择同机房实例，减少跨机房延迟。两个安全阈值（zone-ready-percentage、same-zone-min-available）防止同机房实例过载。

第二层——数据：
异地多活的数据冲突是核心难题。按照一致性要求分类：
- 强一致数据（如库存、余额）：只在一个机房写入（单元化），另一个机房通过数据同步只读
- 最终一致数据（如用户资料）：双写 + 冲突解决
- 本地无关数据（如日志）：各写各的，不需要同步

第三层——切换：
通过配置中心（Nacos/Apollo）控制区域切换。正常情况下每个服务自己知道区域，不需要人工干预。故障时运维在配置中心改一个属性，所有服务秒级切换。

第四层——安全：
切换前检查同机房实例的健康状态、就绪率、最小可用数。任何检查不通过都保持当前路由，不自动切换。切换后原机房作为禁用区域，避免流量回切。

面试官追问：同机房所有实例都挂了怎么办？

回答：ZonePreferenceFilter 的最后一级降级是返回全部实例。如果同机房实例数为 0，直接返回其他机房的实例。路由层不会因为机房故障而阻断流量——流量会绕过故障机房，路由到其他机房。

面试官追问：切换窗口期数据不一致怎么处理？

回答：路由切换是秒级的（配置中心 -> EnvironmentChangeEvent -> ZoneContext.setZone），但数据源切换需要时间（关闭连接池、建新连接池）。窗口期内请求路由到新机房，数据源还连旧机房。解法是"先切数据后切路由"：先在目标机房建立数据源连接，等同步完成后，再切路由。microsphere 没有实现这个顺序控制——这是一个平台层面的能力，需要在编排层（如 MSHA）解决。

---

### 面试题 2：多活 vs 主备，选哪个？

面试官问："我们现在的架构是两地三中心（一个中心写，两个中心读），想改成异地多活（三个中心都写）。有什么风险和代价？"

回答：

两地三中心（Active-Standby）：
- 所有写操作在主中心
- 备中心只提供读服务
- 主中心故障时切换备中心为主（手动或自动）
- 数据一致性没问题（单点写入）

异地多活（Multi-Active）：
- 所有中心都提供读写
- 需要处理数据冲突
- 复杂度比主备高一个量级

什么情况下不值得改：

业务上有强一致性要求时（如支付、库存），异地多活意味着要处理"同一个用户同时在两个中心下单"的冲突。如果是单元化路由（按用户分片），那本质上是多主片——每个用户只在一个中心写，不是真正的多活。真正的异地多活要求任意中心都能处理任意用户的请求，这在一致性上极其困难。

阶段性地推进：

```
阶段 1：同区域主备 -> 同区域多活
  在同一个机房内部署多个可读写实例，通过 ZonePreferenceFilter 路由
  不涉及跨区域数据同步

阶段 2：跨区域主备
  主区域读写，备区域只读
  数据通过 DTS/Canal 同步
  主区域故障时切备为主

阶段 3：跨区域多活（按用户分片）
  每个区域负责一部分用户（用户 ID 哈希）
  区域内读写，跨区域通过 DTS 同步
  区域故障时用户切换到另一个区域

阶段 4：跨区域多活（全量）
  所有区域都可以处理所有用户的读写
  依赖 CRDT 或者业务层的冲突解决
```

大多数公司不需要到阶段 4。阶段 3（单元化路由）已经能解决大部分问题。

---

### 面试题 3：区域偏好路由为什么可能导致过载？

面试官问："开启区域偏好路由后，同区域的流量集中到一个区域内的实例上。什么情况下这会导致过载？怎么防止？"

回答：

过载场景：

正常情况：北京 20 个实例，上海 20 个实例，流量均匀分布在 40 个实例上。开启区域偏好后：北京的 20 个实例只服务北京流量（假设 50%），上海的 20 个实例只服务上海流量（50%）。如果北京的流量突然增长到 80%，北京的 20 个实例要承担 80% 的流量，而上海的 20 个实例只承担 20%。北京的实例过载。

另一个常见场景：滚动更新时，同区域实例逐台重启。重启期间同区域实例减少，剩余的实例承担超出预期的流量。

microsphere 的防护机制：

same-zone-min-available（默认 5）：如果同区域实例数低于 5，不应用偏好路由，返回全部实例。这样更新的过程中流量会被自动分散到其他区域。

如何设置正确的阈值：

same-zone-min-available 的设置取决于单实例的容量和总流量：
- 单实例容量 = 200 QPS
- 同区域流量 = 1000 QPS
- min-available = 1000/200 = 5

如果单实例容量是 500 QPS，min-available 可以设到 2-3。但低于 2 时单个实例故障会导致 50% 的流量损失——这时即使区域偏好路由保护了实例不过载，整体服务的可用性也会下降。

---

### 面试题 4：ZoneContext 为什么设计成单例？

面试官问："ZoneContext 是全局单例。在 Spring Boot 中为什么不做成 Spring Bean？单例有什么问题？"

回答：

为什么不是 Spring Bean：

ZoneContext 需要在三类代码中访问：
非 Spring 代码：Ribbon 的 ServerListFilter（Netflix 库，不在 Spring 管理范围内）
静态工具类：自定义的 ZoneUtils 可能会从静态上下文中调用
框架代码：DynamicDataSource 的 afterPropertiesSet 中 addPropertyChangeListener

如果是 Spring Bean，这些代码需要通过 ApplicationContext.getBean() 获取，或者通过构造函数注入。前者需要 ApplicationContext 的引用（不是总能拿到），后者需要所有调用方都是 Spring Bean。

单例的问题：

无法替换：单元测试中不能 mock ZoneContext。不能在测试用例之间隔离区域设置。测试代码只能通过 System.setProperty 间接改变区域。
隐式依赖：类内部调用 ZoneContext.get()，没有在构造函数或方法签名中声明依赖。代码阅读者不知道这个类依赖于 ZoneContext。
全局状态：ZoneContext 是一个全局可变对象。一个线程改了 region，另一个线程立即看到。在多线程测试中，不同测试用例之间的区域设置会相互影响。

缓解方案：

虽然 ZoneContext 是单例，但 ZonePreferenceFilter 不是——ZonePreferenceFilter 通过构造函数接收 ZoneContext 引用。如果你需要在测试中使用不同的区域设置，可以：

```java
// 测试代码中
ZoneContext context = new ZoneContext(); // 假设构造函数是 public 的
context.setZone("test-zone");
ZonePreferenceFilter<ServiceInstance> filter =
    new ZonePreferenceFilter<>(context, testResolver);
```

但 ZoneContext 的构造函数是 private（单例模式）。需要在 ZoneContext 中加一个包级可见的构造函数用于测试。

---

### 面试题 5：ZonePreferenceFilter 的降级策略

面试官问："如果区域偏好路由的所有条件都不满足，最终返回什么？会阻断业务吗？"

回答：

不会阻断业务。ZonePreferenceFilter 的所有降级路径都返回非空列表：

三级降级：

第一级（第 1-4 步）：返回原始 entities 列表。触发条件：功能关闭、偏好关闭、区域无效、禁用区域过滤后实体太少。这一级返回的列表包含所有实例（包括禁用区域的实例）。

第二级（第 5-6 步）：返回 targetEntities 列表。触发条件：就绪率不达标、同区域实例太少。targetEntities 已经排除了禁用区域的实例。

第三级（第 7 步）：返回 sameZoneEntities 列表。触发条件：所有条件满足。这是正常路径，只返回同区域实例。

降级原则是 Fail-open：

如果同区域没有任何实例，ZonePreferenceFilter 返回 targetEntities（所有非禁用实例）。流量正常流动，只是没有区域偏好。

如果某个安全阈值检查不通过（如就绪率 80% < 100%），同样返回 targetEntities。流量继续，只是区域路由偏好暂缓。

ZonePreferenceFilter 不抛异常，不返回空列表（除非原始 entities 本身就是空的）。

什么时候会空？

原始 entities 为空时返回空。禁用区域过滤后 entities 为空时，返原始 entities（已经是空列表）而不是 targetEntities。代码逻辑是 `if (currentSize <= 1) { return entities; }`——当所有实例都属于禁用区域时，entities 全部被过滤，currentSize 可能为 0，此时返回原始的空列表 entities。

这是合理的行为——如果所有实例都在被禁用的区域，业务不该继续往这些实例发请求。但这个问题也可以通过"不要禁用所有区域"来避免。

---

### 面试题 6：ZoneContextChangedEvent 的发布流程

面试官问："当我在 Nacos 配置中心把 zone 从 beijing-1 改成 shanghai-1 后，事件是怎么传播的？哪些组件会收到通知？"

回答：

配置变更事件的传播路径：

Nacos 客户端检测到配置变更
  -> NacosContextRefresher 收到变更通知
    -> 调用 ContextRefresher.refresh() 重建 Spring Environment
      -> 重建完毕后 Environment 中 microsphere.availability.zone 的值已更新
      -> 发布 EnvironmentChangeEvent
        -> ZoneContextChangedListener.supportsEventType() 判断为 Spring Cloud 应用
        -> ZoneContextChangedListener.onApplicationEvent()
            -> tryChangeZoneContext()
              -> 从 Environment 读取 microsphere.availability.zone = "shanghai-1"
              -> zoneContext.setZone("shanghai-1")
                -> PropertyChangeSupport.firePropertyChange("zone", "beijing-1", "shanghai-1")
                -> PropertyChangeEvent 被收集

      -> 收集到的 PropertyChangeEvent 不为空
        -> context.publishEvent(new ZoneContextChangedEvent(context, zoneContext, events))

ZoneContextChangedEvent 的监听者：

@EventListener 监听 ZoneContextChangedEvent 的组件：

DynamicDataSource（my-xhs）：onZoneChanged(PropertyChangeEvent event) -> switchDataSource("shanghai-1") -> 关闭旧连接池，创建新连接池

08-redis 的 ListenableConfigurationPropertiesBindHandlerAdvisor：重新绑定 Redis 配置

18-dynamic 的 PropagatingDynamicJdbcConfigChangedEventListener：重建 DynamicJdbcConfig

下一个请求到达时的变化：

LoadBalancer 侧：ServiceInstanceListSupplier.get() -> ZonePreferenceFilter.filter() -> zoneContext.getZone() 已返回 "shanghai-1" -> 偏好 shanghai-1 的实例

application.yml 中的对应配置：

nacos:
    config:
        shared-configs:
            - data-id: myxhs-zone.yml
              refresh: true

myxhs-zone.yml 内容：
myxhs:
  availability:
    zone: shanghai-1

只修改 myxhs.availability.zone 一个属性，不用动其他配置，不用重启应用。

---

### 面试题 7：分区容错——一个区域完全不可用

面试官问："假设北京机房网络中断，完全不可达。上海机房 20 个实例正常运行。你的系统会发生什么？"

回答：

第一阶段：服务调用方感知

北京的服务 A 调用上海的服务 B 时，Ribbon/LoadBalancer 从 DiscoveryClient 获取服务 B 的实例列表。Nacos 的健康检查机制（客户端心跳）检测到北京的服务 B 实例不可达后，将其标记为不健康并从可用实例列表中移除。

第二阶段：区域偏好路由失效

如果北京的服务 A 开启了区域偏好路由（myxhs.availability.zone.preference.enabled=true）：
ZonePreferenceFilter 获取当前区域 = "beijing-1"
从服务 B 的可用实例列表中解析区域信息
此时服务 B 的可用实例全部在上海（zone = "shanghai-1"）
sameZoneEntities.size() = 0
sameZoneEntitiesSize > 0 不满足，直接返回 targetEntities（全部上海实例）

不需要等待超时或配置变更，Nacos 摘除不健康实例立即生效，ZonePreferenceFilter 在下一个请求中自动 fallback。

第三阶段：运维手动切换

上海的服务能正常处理所有请求。但如果北京的服务也对数据源做了同区域路由，数据源还连北京的数据库——北京的数据库不可达，查询失败。

运维在配置中心修改：
myxhs:
  availability:
    zone: shanghai-1
  preference:
    upstream:
      disabled-zone: beijing-1

ZoneContextChangedListener 监听 EnvironmentChangeEvent，更新 zone 为 "shanghai-1"
DynamicDataSource.onZoneChanged() -> switchDataSource("shanghai-1") -> 连接上海的数据库

第四阶段：恢复

北京机房网络恢复后：
运维删除 disabled-zone 配置，把 zone 设为 "originalZone"
ZoneContextChangedListener 重新调用 ZoneLocator.locate()，检测到北京可用，zone 还原为 "beijing-1"
北京的服务重新注册到 Nacos
流量逐渐恢复同区域路由

整个过程中：
请求不会失败（上海实例接管）
数据不会丢失（DataSource 切换到上海数据库）
恢复不需要重启任何服务

但有三件事需要手动做：
检测故障（需要外部监控系统，如 Prometheus + AlertManager）
决策是否切换（人工判断）
在配置中心改值（手工操作）

---

### 面试题 8：ZonePreferenceFilter 和熔断的关系

面试官问："如果同区域的服务实例被 CircuitBreaker 熔断了，ZonePreferenceFilter 会怎么做？"

回答：

ZonePreferenceFilter 和 CircuitBreaker 是两个独立层，顺序是：

ZonePreferenceFilter 过滤（决定调哪些实例）
  → LoadBalancer 选择一个实例
    → 发起调用
      → CircuitBreaker 判断是否允许调用
        → 实际执行

ZonePreferenceFilter 不感知熔断状态

ZonePreferenceFilter 在 LoadBalancer 的 ServiceInstanceListSupplier 阶段执行。这个阶段只查看实例的元数据（区域、权重），不查看实例的健康状态（是否被熔断）。

一个典型的故障场景：

服务 A 的 zone = "beijing-1"
服务 B 有 10 个实例：6 个在 beijing-1、4 个在 shanghai-1
服务 A 调用服务 B
ZonePreferenceFilter 返回全部 6 个 beijing-1 实例

问题：其中 3 个实例因为异常返回率过高被 CircuitBreaker 熔断了

LoadBalancer 仍然可能选中这 3 个被熔断的实例
调用会立即被 CircuitBreaker 拒绝（CallNotPermittedException）
LoadBalancer 可能会重试其他 beijing-1 实例
如果 6 个实例全部被熔断，6 次调用全部被拒绝

ZonePreferenceFilter 不会自动 fallback 到 shanghai-1 实例，因为它认为 beijing-1 的 6 个实例都是候选。

解法一：配合 CircuitBreaker 的重试

在 LoadBalancer 级别配置重试。如果选中的实例被熔断，重试下一个实例。当同区域所有实例都被熔断后（6 次重试后），请求失败。此时业务方可以选择降级或报错。

解法二：调整 same-zone-min-available

如果 6 个 beijing-1 实例中有 3 个被熔断，实际上只剩下 3 个可用实例。如果 same-zone-min-available 设为 5，ZonePreferenceFilter 会返回全部 10 个实例（包括 4 个 shanghai-1 实例），因为 beijing-1 的可用实例 3 < 5。

但 ZonePreferenceFilter 无法感知熔断——它不检查实例是否被 CircuitBreaker 放开。代码中 ZonePreferenceFilter 只检查 region 信息，不检查 CircuitBreaker 的状态。

解法三：Spring Cloud LoadBalancer 的 HealthCheck

Spring Cloud LoadBalancer 有 HealthCheck 机制（定期检查实例的 /actuator/health 端点）。如果实例返回 5xx，从候选列表中移除。但 CircuitBreaker 的熔断状态不一定会反映到 /actuator/health 端点——这取决于应用的实现。

最佳实践：

设置 same-zone-min-available 略高于单区域实例数
配合 LoadBalancer 重试
监控同区域实例的 CircuitBreaker 状态，异常时手动通过 disabled-zone 或重新设置 same-zone-min-available 进行干预

---

### 面试题 9：跨区域数据一致性

面试官问："区域切换时，请求路由到了新区域的实例，但数据还没同步过来。这个窗口期怎么处理？"

回答：

窗口期的定义：

配置中心改 zone = shanghai-1
  → 下一瞬间，LoadBalancer 开始路由到 shanghai-1 实例
  → 但 DynamicDataSource 正在关闭 beijing-1 的连接池、创建 shanghai-1 的连接池（需要几秒到几十秒）
  → 窗口期内：请求的代码在 shanghai-1 跑，数据库连的是 beijing-1

这不是 microsphere 特有的问题，任何涉及到跨区域资源切换的系统都有这个问题。

my-xhs 的 DynamicDataSource 已经做了一层防护：waitForActiveConnections() 机制。

关闭旧连接池之前等待所有活跃连接完成（最长 30 秒）
如果有 TCC 事务在执行，等事务完成后再切换
超时后强制切换（TCC 有 Cancel 兜底机制）

但这只保护了"进行中的事务"，不保护"切换窗口内新进来的请求"。

三个常见解法：

解法一：读多写少的场景。窗口期内新请求进入新区域的实例，数据库还是旧区域的。读请求读到旧数据（不太一致，但系统可用）。写请求写到旧数据库（通过 DynamicDataSource 的 delegate 指向旧数据源）。等到数据同步完成后再正式切到新数据库。

解法二：双写 + 版本号。写入时两个数据库都写，读时优先读新区域的数据库。如果新数据版本低于旧数据，回退到旧库。这一般需要业务层的支持。

解法三：写操作只在主区域进行（单元化）。读操作可以跨区域。区域切换只影响读路由，不影响写路由。写路由始终指向主区域。

microsphere 不实现这些机制——它只负责让路由切换和资源切换联动（通过 ZoneContextChangedEvent）。强一致性需要数据层（如 08-redis 的 Kafka 复制）和业务层（如幂等、版本号）来保证。

---

### 面试题 10：ZoneLocator 的 SPI 设计

面试官问："ZoneLocator 为什么设计成 SPI（接口 + 多实现 + 组合模式）？直接配置一个属性不就行了吗？"

回答：

直接配置一个属性确实够用：

application.yml：
myxhs:
  availability:
    zone: beijing-1

问题是 zone 的来源有多个：

AWS EC2 上：从 169.254.169.254 元数据端点获取
AWS ECS 上：从容器元数据文件获取
K8s 上：从 Downward API 注入的环境变量获取
物理机上：从部署脚本的环境变量获取
本地开发：从 application.yml 获取

SPI 设计的价值在于：

ZoneLocator 的按需发现机制：ZoneAutoConfiguration 在启动时通过 Spring FactoriesLoader 和 Bean 发现所有 ZoneLocator 实现。用户不需要手动选择和指定使用哪个定位器。如果在 AWS EC2 上部署，EC2 定位器自动生效；如果在本地开发，DefaultZoneLocator 生效。

优先级控制：每个定位器有 order 值。ECS 文件定位器(order=5) > ECS V4 端点定位器(order=10) > EC2 元数据端点定位器(order=15) > Spring 属性定位器(order=20)。优先级是"越环境特定越优先"——AWS ECS 中自动用 ECS 文件定位器，EC2 中自动用 EC2 元数据端点定位器，本地机器上自动用 application.yml 配置。

可扩展性：如果用户需要从自定义的云平台元数据服务中获取区域，只需要实现 ZoneLocator 接口。不需要改框架代码。

为什么不用 @Profile 或 @Conditional：

@Profile 需要用户在启动时指定 profile（-Dspring.profiles.active=aws-ec2）。这回到了"用户手动指定"的老路。ZoneLocator 的 supports() 方法可以自动检测当前环境——用户在 application.yml 中不需要任何环境相关的配置。

@ConditionalOnProperty 可以在配置了某个属性时才加载 Bean。但 ZoneLocator 的价值是"不需要配置任何东西就能自动检测"。

一个实际例子：

同一个应用部署包，开发者在本地运行时 zone 自动 = "local"。部署到 AWS ECS 时 zone 自动 = "us-east-1a"。部署到 K8s 时 zone 自动 = "cn-beijing-b"。0 配置改动。

如果用一个属性解决：开发者本地需要配一个值、AWS ECS 部署脚本需要配一个值、K8s 部署脚本需要配一个值。微服务有 20 个服务，每个服务都要配。SPI 让这个配置过程完全自动化。

优缺点：

SPI 增加了启动时间（CompositeZoneLocator 依次尝试多个定位器，EC2 定位器在非 AWS 环境中超时 3 秒）。但对于启动时的一次性定位，这是可以接受的。ECS 文件定位器(order=5)和 ECS V4 端点定位器(order=10)有 support() 检查环境变量是否存在，不存在的环境不参与尝试，不会超时。只有 EC2 定位器（order=15，supports() 永远返回 true）在非 AWS 环境中会超时。

---

## 总结：各方案的适用场景

### 选择指南

| 场景 | 推荐方案 |
|------|---------|
| 单区域部署 | 不需要区域路由，Spring Cloud 默认即可 |
| 多区域部署，区域固定，实例充足 | Spring Cloud ZonePreference + 同区域主备 |
| 多区域部署，区域固定，实例数波动 | microsphere-multiactive（安全阈值防止过载） |
| 多区域部署，需要动态切换 | microsphere-multiactive（ZoneContextChangedListener） |
| 多区域部署，需要单元化路由 | 阿里 MSHA + 单元化改造 |
| 多区域部署，部署在 K8s | microsphere-multiactive（ZoneLocator 从 K8s Downward API 读取）+ K8s topologyKeys 基础路由 |
| 多区域部署，需要自动故障切换 | 外部健康检查 + 编排平台（MSHA 或自研） |

### 面试回答框架（3 句话）

"异地多活的核心是区域感知路由和故障切换的统一平台。microsphere-multiactive 提供了一套轻量级方案：通过 ZoneLocator SPI 自动发现区域，通过 ZonePreferenceFilter 实现带安全阈值的偏好路由，通过 ZoneContextChangedListener 实现配置中心驱动的秒级切换。对于大型场景需要补充自动故障检测和单元化路由能力。""
