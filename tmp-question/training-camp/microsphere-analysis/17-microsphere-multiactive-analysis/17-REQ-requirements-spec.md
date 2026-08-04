# 17-REQ：Zone-Aware 多活部署需求规格

> 以 Spring Cloud LoadBalancer 为对比基准。SCL 已在 `spring-cloud-loadbalancer` 提供了 `ZonePreferenceServiceInstanceListSupplier`。
> 基准：Java 17+，Spring Cloud 2024.x+

---

## 项目定位

**microsphere-multiactive（31 文件）在 Spring Cloud LoadBalancer 已有的 Zone 优先路由基础上增加了 3 个增量：(1) 自动从云元数据发现 Zone——不需要手配 `spring.cloud.loadbalancer.zone`；(2) ZoneContext 属性变更事件通知；(3) 精细的 Zone 策略控制（同区最小可用数、上游就绪百分比、禁用区列表）。**

---

## 核心需求

### REQ-001：Zone 自动发现（ZoneLocator SPI）

**问题**：SCL 的 Zone Preference 需要你手动配 `spring.cloud.loadbalancer.zone=us-east-1a`。在 K8s/AWS 环境，Zone 是 Pod 部署时自动分配的——运维不应该手写。

**产出**：`ZoneLocator` SPI——`supports(Environment)` + `locate(Environment)`。3 个内置实现：AWS EC2 metadata、AWS ECS task metadata V4、自定义（从 Env 属性读取）。`CompositeZoneLocator` 组合多源，第一个 supports=true 的生效。

**状态**：[已验证实现]

---

### REQ-002：ZoneContext——非 Spring 的全局 Zone 配置单例

**问题**：Zone 信息需要被**非 Spring 代码**访问——Ribbon ServerListFilter、Redis 连接工厂、MyBatis 拦截器、静态工具类——这些地方拿不到 Spring BeanFactory。需要一个全局可达的 Zone 状态持有者。

**产出**：`ZoneContext` 单例——不是 Spring Bean，通过 `ZoneContext.get()` 全局获取。7 个 `volatile` 属性：

| 属性 | 默认值 | 设计意图 |
|------|--------|---------|
| `enabled` | `true` | 总开关 |
| `zone` | `"defaultZone"` | 当前区域（兼容 Eureka 默认区名） |
| `preferenceEnabled` | **`false`** | 安全第一——开启后流量分布改变，要求明确启用 |
| `preferenceFilterOrder` | `10` | 在 SCL Filter 链中的位置 |
| `upstreamZoneReadyPercentage` | **`100`** | 极其保守——上游 100 个实例中 1 个无 zone 信息 → 就绪率 99% < 100% → 不应用偏好 |
| `sameZoneMinAvailable` | `5` | 同区少于 5 个实例 → 回退全部实例 |
| `upstreamDisabledZone` | `null` | 逗号分隔的禁用区——机房故障时运维可动态屏蔽 |

每个 setter 通过 `PropertyChangeSupport.firePropertyChange()`（Java 标准，非 Spring ApplicationEvent）发布变更事件。只有值真正变化时才触发——避免重复设置产生无用事件。`getCurrentZone()` 静态方法不走单例字段，读 `System.getProperty()`——使 ZoneLocator 定位结果全局可见。

**状态**：[已验证实现]

---

### REQ-003：Zone Preference 路由（微球优化版 SCL）

**产出**：`ZonePreferenceServiceInstanceListSupplier`——同区实例优先，跨区回退。微球版 vs SCL 原生版差异：微球集成了 `ZoneContext` 的精细策略（`sameZoneMinAvailable`/`upstreamReadyPercentage`/`disabledZone`），SCL 原生仅按 zone metadata 过滤。

**状态**：[已验证实现]

---

### REQ-004：Zone 信息附加到服务注册元数据

**产出**：`ZoneAttachmentHandler`——发现 Zone 后写入 `Registration.metadata["zone"] = "us-east-1a"`，使下游能感知。AWS `ZoneAttachmentPreRegistrationHandler` 实现。

**状态**：[已验证实现]

---

## 与 Spring Cloud 原生对比

| | SCL 原生 | 微球 |
|---|---|---|
| Zone 发现 | 手动配 `spring.cloud.loadbalancer.zone` | 自动从 AWS EC2/ECS metadata 发现 |
| Zone 配置模型 | 一个属性 `spring.cloud.loadbalancer.zone` | `ZoneContext` **全局单例**（非 Spring Bean）——非 Spring 代码可用 |
| Zone 策略 | 同区优先，无同区 → 返回全部 | 同区优先 + **保守默认**（就绪率100%/同区最少5实例/默认关闭偏好） + 动态禁用区 |
| Zone 注册 | 无 | Zone 写入 Registration.metadata["zone"] |
| 事件机制 | 无 | `PropertyChangeSupport`（Java 标准，非 Spring） |

---

## 已知缺陷

| 缺陷 | 说明 |
|------|------|
| 🟡 微球版 SCL Supplier 命名为同名类 | `io.microsphere...ZonePreferenceServiceInstanceListSupplier` 与 `org.springframework.cloud.loadbalancer.core.ZonePreferenceServiceInstanceListSupplier` 同名——IDE 容易 import 错 |

---

## 发散需求

### REQ-N01：跨 Zone 延迟自动调节权重

当前 weight 是静态配置——根据 P99 延迟自动调整同 zone vs 跨 zone 流量比例。

### REQ-N02：Zone 级容灾演练

模拟某 Zone 全宕——流量自动切到其他 Zone——再模拟恢复。运维可控的 Zone failover drill。

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| — | 2026-08-03 | REQ 文档编写 |
