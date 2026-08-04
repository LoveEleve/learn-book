# 05-REQ：microsphere-spring-cloud 完整需求规格（v2，SC v4.3.2 源码验证）

> 本文是 microsphere-spring-cloud 的完整需求文档（v2，逐文件+逐方法+Spring Cloud v4.3.2 对照）。v1 SC 对比已验证准确——v2 新增 10 个 bug + 包级重叠表。
>
> 所有需求分三类：
> 1. **已实现需求**（REQ-001~007，7 项）——SC overlap% 标注
> 2. **待修复**（REQ-D01~D16，16 项）——含 6 个运行期崩溃
> 3. **全新发散**（REQ-N01~N05，5 项）
>
> **基准环境**：Java 17+，Spring Cloud v4.3.2，对照源码 `/data/workspace/source-code/code/spring/spring-cloud-commons/` + `spring-cloud-openfeign/`

---

### SC 重叠按包分布

| 包 | 文件数 | SC 重叠 | 判定 |
|---|:---:|:---:|------|
| service.registry Multiple/Registration/Event/Endpoint/Aspect | 26 | ~30% | 多注册/4事件/deregister/端点真增量 |
| discovery Union/Adapter | 8 | ~40% | Union合并 vs Composite取首个 |
| actuator features | 6 | ~10% | 配置属性声明 features 独有 |
| context.named Specification | 3 | ~0% | SC NamedContextFactory 无定制扩展 |
| fault.tolerance Tomcat/权重 | 5 | ~0% | SC 无 Tomcat 热更新 |
| openfeign autorefresh/components | 20 | ~0% | 组件级热刷新 SC 无（SC只刷新URL） |
| 条件/常量/自动配置 | 10 | ~50% | 条件注解自有 |

**整体独有率 ~35%（真增量，不是 04 类的薄封装）**

---

## 项目定位

**microsphere-spring-cloud 是 Spring Cloud 的生产增强插件**，解决的核心问题是：**Spring Cloud 的 Feign 配置不能热刷新（改 encoder/decoder/interceptor 必须重启）、一个服务只能注册到一个注册中心（多活架构需要同时注册到 Nacos+Eureka）、服务注册/注销没有生命周期事件（无法审计和告警）——microsphere 通过 Feign Capability 装饰器模式 + 多注册委托 + AOP 拦截填补了这三个缺口。**

与前面项目的层次关系：
- **01-04**（基础设施）——从 JDK 底层到 Spring Boot
- **05**（Spring Cloud 层）——分布式服务治理的运行时增强

**源码信息**：
- 路径：`/data/workspace/java-training-camp/cloud-native-code/stage-4/microsphere-spring-cloud/`
- 模块：`commons`（58 文件）+ `openfeign`（20 文件）= 78 文件
- 版本：`0.2.x-SNAPSHOT`，Spring Cloud 2022~2025.x 兼容

### 与 Spring Cloud 标准行为的对比

> 经对 `/data/workspace/source-code/code/spring/spring-cloud-commons/`（v4.3.2）和 `/data/workspace/source-code/code/spring/spring-cloud-openfeign/`（v4.3.2）源码直接验证。

| REQ | Spring Cloud 已有？ | 结论 |
|:---:|:---:|------|
| 001 多注册中心 | **无** | microsphere 独有 |
| 002 Feign 组件热刷新 | `RefreshableHardCodedTarget` 只刷新 URL——**不刷新 encoder/decoder/interceptor** | microsphere 独有（组件级 vs URL 级） |
| 003 注册生命周期事件 | `InstancePreRegisteredEvent` + `InstanceRegisteredEvent` **有**——**但无注销事件** | microsphere 独有注销端（Deregistered）+ 4 事件全覆盖 |
| 004 Union 发现 | `CompositeDiscoveryClient` **取第一个非空**（`if (!isEmpty()) return instances`） | 语义不同 |
| 005 Actuator 端点 | `ServiceRegistryEndpoint` `/actuator/serviceregistry` **有**——但只有 getStatus/setStatus | microsphere 独有：register/deregister 操作 |
| 006 Tomcat 动态配置 | **无** | microsphere 独有 |
| 007 Web 端点映射注册 | **无**（依赖 03） | microsphere 独有 |

**结论**：7 项中 **2 项 Spring Cloud 有部分能力**（事件只有注册端、端点只有状态管理），5 项真正独有。源码验证完成。

---

## 一、多服务注册中心

### REQ-001：同时注册到多个注册中心

**问题**：Spring Cloud 标准机制只能注册到**一个**注册中心——`spring.cloud.nacos.discovery.enabled=true` 和 `spring.cloud.eureka.enabled=true` 互斥，不能同时生效。多活架构需要同一服务实例同时出现在多个注册中心——比如 Nacos（内部微服务）+ Eureka（遗留系统）+ Consul（跨机房服务网格）。

**产出**：
- `MultipleServiceRegistry`：`ServiceRegistry<MultipleRegistration>` 实现——内部维护 `Map<Class<?>, ServiceRegistry>` 注册表，`register()` 时遍历所有子注册中心，按 Registration 类型分发
- `MultipleRegistration`：聚合多个子 `Registration`——`special(type)` 按类型取对应的注册信息
- `MultipleAutoServiceRegistration`：替换标准 `AutoServiceRegistration`，保证 `@Primary` 时覆盖单注册机制
- `RegisterCustomizer` SPI：注册前钩子——可修改 registration 元数据
- 类型映射：通过 `spring.factories` 注册 `NacosServiceRegistry=NacosRegistration` 的映射关系——新增注册中心只需加一行 spring.factories

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- `MultipleRegistration` 同类型后注册会覆盖先注册——多实例注册中心同一类型只保留最后一个
- `special()` 返回 null 时静默跳过该注册中心——日志仅 trace
- `getStatus()` 只从最后一个注册中心取值——无法感知其他注册中心的健康状态
- 需显式开 `microsphere.spring.cloud.multiple-registration.enabled=true`

**配置规格**：
```properties
microsphere.spring.cloud.multiple-registration.enabled = true
```

---

## 二、OpenFeign 热刷新

### REQ-002：运行时替换 Feign 组件无需重启

**问题**：Spring Cloud OpenFeign 的 Encoder/Decoder/Contract/RequestInterceptor 等组件在 `Feign.Builder` 构建时注入——一旦 `FeignClient` 代理创建完毕，这些组件就固化了。线上要改超时、换序列化器、加请求头拦截器——必须重启。`@RefreshScope` 只刷新 `@Value` 属性，对 Feign 内部的组件无效。

**产出**（核心链路）：
- `AutoRefreshCapability`：实现 `feign.Capability` 接口——`enrich()` 时把 `Retryer/Contract/Decoder/Encoder/ErrorDecoder/QueryMapEncoder/RequestInterceptor` 包成 `Decorated*` 装饰器
- `DecoratedFeignComponent<T>`：所有装饰器的抽象基类——`volatile T delegate` + 懒加载 `delegate()`（从 `NamedContextFactory` 子上下文取） + `refresh()` 时将 delegate 置 null——下次调用自动从新配置重新加载
- `FeignComponentRegistry`：组件注册表——`configComponentMappings` 把配置 key（`retryer/decoder/encoder/...`）映射为组件类型；`refresh()` 时按配置 key 匹配组件类型，调 `Refreshable.refresh()`
- `FeignClientConfigurationChangedListener`：监听 `EnvironmentChangeEvent`——过滤 `spring.cloud.openfeign.client.config.*` 前缀的变更 key → 按 client 名分组 → 调 `FeignComponentRegistry.refresh()`
- `CompositedRequestInterceptor`：拦截器聚合——`refresh()` 时从 `FeignClientProperties` 重新读取 `request-interceptors` + `default-request-headers` + `default-query-parameters`

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- **❌ P1：FeignClientConfigurationChangedListener 无子属性 key 崩溃**——配置 key 恰好等于 `spring.cloud.openfeign.client.config.myservice`（无子属性如 `.retryer`）时 `substring(0, -1)` 抛 `StringIndexOutOfBoundsException`
- **❌ P1：CompositedRequestInterceptor.refresh() 配置缺失 NPE**——`defaultConfiguration/currentConfiguration` 为 null 时 `getter.apply()` 直接 NPE
- `AutoRefreshCapability` 的 `contextId` 依赖 `spring.cloud.openfeign.client.name`——缺失时 `register(null)` 抛异常
- 只对 `default.*` specification 注入——依赖 Spring Cloud 标准机制传递 Capability
- Listener 通过 `addApplicationListener()` 手动注册，顺序依赖 `ApplicationReadyEvent` 时机

**配置规格**：
```java
@Configuration
@EnableFeignAutoRefresh
public class FeignConfig {}
```

---

## 三、服务注册生命周期事件

### REQ-003：注册/注销的事件通知

**问题**：Spring Cloud 的 `AutoServiceRegistration` 自动完成注册——整个过程**没有回调、没有事件、没有通知**。你无法知道"什么时候注册成功了？"被注销了"、"注册失败了？"、"注册信息是什么？"。运维需要在注册成功时触发健康检查、在注销时清理缓存。

**产出**：
- `EventPublishingRegistrationAspect`：`@Aspect` 拦截 `ServiceRegistry.register()`/`deregister()`——调用前发布 `RegistrationPreRegisteredEvent`/`RegistrationPreDeregisteredEvent`，调用后发布 `RegistrationRegisteredEvent`/`RegistrationDeregisteredEvent`
- `RegistrationCustomizer` SPI：`@Before` 拦截时在注册前调用——可修改注册元数据
- 4 个事件对象：`PRE_REGISTERED/REGISTERED/PRE_DEREGISTERED/DEREGISTERED`——每类携带 `Registration` + `ServiceRegistry` 引用

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- `RegistrationCustomizer` 未用 `@Order` 控制执行顺序——多个 Customizer 的执行顺序不确定
- `isIgnored()` 排除 `MultipleServiceRegistry`（防止多注册时重复事件）——硬编码排除逻辑
- 依赖 `@ConditionalOnClass("org.aspectj.lang.annotation.Aspect")`——需 AspectJ 在 classpath

**配置规格**：无配置项（存在 `EventPublishingRegistrationAspect` bean 即生效）。

---

## 四、Union 服务发现

### REQ-004：聚合多注册中心的服务实例

**问题**：Spring Cloud 的 `CompositeDiscoveryClient` 按 order 排序后**取第一个非空**结果——如果你的服务只注册到了 Nacos，Eureka `DiscoveryClient` 返回空，好——Nacos 的结果被返回了。但如果你的服务**同时注册到了 Nacos 和 Eureka**，Composite 仍然只取第一个（Nacos）的结果——Eureka 注册中心的冗余实例被忽略了。多活架构中需要两边都查。

**产出**：
- `UnionDiscoveryClient`：聚合所有 `DiscoveryClient`——`getInstances()` 遍历全部 client，`addAll()` 合并；`getServices()` 用 `LinkedHashSet` 去重

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- `getInstances()` **不去重**——同一服务在 Nacos+Eureka 都注册时，实例列表会重复出现同一 IP:Port
- 需显式设 `microsphere.spring.cloud.client.discovery.mode=union` 才启用
- 排除标准 `CompositeDiscoveryClient` + 自身——防止无限循环

**配置规格**：
```properties
microsphere.spring.cloud.client.discovery.mode = union
```

---

## 五、Actuator 端点

### REQ-005：手动注册/注销控制

**问题**：运维有时需要**临时摘除**一个实例——比如要上线打补丁、要做灰度切换、要隔离故障节点。Spring Cloud 没有给出手动注册/注销的接口——你只能 kill 进程触发 `@PreDestroy` 的 deregister。

**产出**：
- `/actuator/serviceRegistration`（`@ReadOperation` 查看当前注册状态，`@WriteOperation` 手动注册）
- `/actuator/serviceDeregistration`（`@WriteOperation` 手动注销）

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- `running` 是 **static** 字段——两个端点实例共享状态，多实例时互相覆盖
- `afterSingletonsInstantiated` 用 `getIfAvailable` 可能拿到 null——`metadata()` 里直接 `serviceRegistry.getStatus()` NPE

**配置规格**：通过 `@ServiceRegistrationEndpointAutoConfiguration` 自动注册。

---

## 六、Tomcat 动态配置

### REQ-006：Tomcat 容器运行时参数热更新

**问题**：Spring Boot 的 `server.tomcat.threads.max` 修改后需要重启——`@ConfigurationProperties` 的 `ServerProperties` 虽然支持 `@RefreshScope`，但 Tomcat 的 `Connector` 和 `Executor` 不会自动应用新值。需要在容器层面做运行时替换。

**产出**：
- `TomcatDynamicConfigurationListener`：监听 `EnvironmentChangeEvent`——过滤 `server.*` key → 链式比较新旧值 → 有差异时通过反射设置 Tomcat Connector/Executor 属性
- 支持的热更新属性：`threads.minSpare/max`、`acceptCount`、`connectionTimeout`、`maxConnections`、`maxHttpHeaderSize`、`maxSwallowSize`、`maxPostSize`
- 有 `ConfigurationPropertiesRebinder` 时直接复用 rebind 后的 `ServerProperties`，否则自行 bind

**状态**：[已验证实现] 📝 **待编写原理文档**

**配置规格**：`@ConditionalOnClass(Tomcat)` + `@ConditionalOnWebApplication(SERVLET)` 自动启用。

---

## 七、Web 端点映射注册

### REQ-007：Web 端点元数据写入注册信息

**问题**：服务注册时只注册 IP:Port——"我的服务提供了哪些 HTTP 端点？"这个信息不在注册中心里。网关/前端无法从注册中心获取完整的 API 路由信息。

**产出**：
- `WebServiceRegistryAutoConfiguration`：监听 `WebEndpointMappingsReadyEvent`（来自 03-microsphere-spring）→ 将全部端点映射序列化为 JSON → URL encode → 写入 `Registration.metadata["web.mappings"]`
- 同时写入：`web.context-path`、`management-port`、`start-time`
- MVC 版：自动排除 Spring 内置 Filter 和 DispatcherServlet 映射

**状态**：[已验证实现] 📝 **待编写原理文档**

**已知问题**：
- 依赖 03 的 `WebEndpointMapping` 体系——不引入 03 则无数据源
- WebFlux 版 `isExcludedMapping` 恒 false（未过滤内置端点）

**配置规格**：无配置项（`@ConditionalOnBean(Registration)` + WebEnvironment 自动启用）。

---

### ⚠️ 其他已实现但未独立成 REQ 的能力

- **HasFeatures 机制**（`client/actuator/`，5 文件）：通过 `features.yaml` 声明各模块的能力类 + classpath 探测 + `ConfigurationPropertyHasFeaturesAutoConfiguration` 注册为 Spring Boot `HasFeatures` bean——用于 `/actuator/features` 端点。目前未在任一 REQ 中独立列出。
- **WeightedRoundRobin**（`fault/tolerance/loadbalancer/`）：只有数据结构（`increaseCurrent()` + `sel(total)`），**无选择逻辑、无实例列表管理、无权重动态更新**——是一个**不完整的半成品**。见 REQ-D07。

---

## 八、待实现需求（bug 修复）

### REQ-D01：ServiceInstanceUtils.setProperties source/target 混淆修复

**方案**：`target.getMetadata().putAll(source.getMetadata())`——当前错误地操作了 source 自身元数据。

**状态**：[待修复] — 当前 source 自身 metadata 被清空再复制回自己，target 完全未设置。影响 discovery 互转链路。

---

### REQ-D02：FeignClientConfigurationChangedListener 无子属性 key 崩溃修复

**方案**：`indexOf(".") == -1` 时 `return key`（不截断），而非 `substring(0, -1)`。

**状态**：[待修复] — `config.default` 或 `config.myservice` 这类恰好等于配置前缀的 key 触发即崩溃。

---

### REQ-D03：CompositedRequestInterceptor.refresh() NPE 修复

**方案**：`defaultConfiguration == null` 时 `new FeignClientConfiguration()` 兜底初始化，同理 `currentConfiguration`。

**状态**：[待修复] — 配置 map 无对应 client 名键时直接 NPE。

---

### REQ-D04：UnionDiscoveryClient.getInstances 实例去重

**方案**：合并前用 `Set<ServiceInstance>` （按 instanceId 或 host+port 去重）替代 `List.addAll()`。

**状态**：[待修复] — 同一服务多注册中心同时注册时实例重复。

---

### REQ-D05：AbstractServiceRegistrationEndpoint static running 字段修复

**方案**：`static boolean running` → `instance boolean running`——每个端点实例独立状态。

**状态**：[待修复] — 两个端点共享状态，多实例互相覆盖。

---

### REQ-D06：MultipleRegistration 同类型覆盖行为

**方案**：同类型 Registration 改为 `Map<Class, List<Registration>>` 或 `CompositeRegistration`——追加而非覆盖。

**状态**：[待改进] — 当前后注册覆盖先注册，无法表现多实例注册。

---

### REQ-D07：WeightedRoundRobin 完整实现

**方案**：当前只有数据结构（`increaseCurrent()` + `sel(total)`），需补全：(1) `select(List<ServiceInstance>)` 选择逻辑；(2) `setWeight(instance, weight)` 动态更新权重；(3) `add/remove` 实例管理。`FaultTolerancePropertyConstants` 中 `warmup-time`/`weight` 常量已定义但无端到端读取点。

**状态**：[待补全] — 当前只有算法数据结构，无法独立使用。

---

### REQ-D08：DiscoveryUtils.setProperties 参数颠倒

**方案**：`setProperties(targetLocal, local)` → `setProperties(local, targetLocal)`——参数次序反了导致 local 的 metadata 被清空（配合 D01 的 source/target 混淆，共同破坏 source metadata）。

### REQ-D09：MultipleServiceRegistry.getRegistrationClass fallback 缺陷

fallback 用 `loadFactoryNames(serviceRegistryClass, ...)` 映射错误——实现类当 factoryType，仅 spring.factories 有映射时有效。映射失败返回 Registration.class → registry.register(MultipleRegistration) → Nacos 收到错误类型 → ClassCastException。

### REQ-D10：Feign 组件热刷新对默认组件退化

`DecoratedFeignComponent.refresh` 后 `delegate()` 从 NamedContextFactory 子上下文拉取——SC FeignClientsConfiguration 的 decoder/encoder/contract/retryer 都是 singleton bean，配置变化后返回旧实例；未配置组件类时 componentType()=接口.class → getProvider(接口.class) 拉到默认单例 → 刷完组件退化为默认值。

### REQ-D11：DecoratedErrorDecoder fallback 到抽象类

`componentType()` 未配置 errorDecoder 且子上下文无 bean → `instantiateClass(ErrorDecoder.Default.class)`——Default 是抽象类 → InstantiationException。

### REQ-D12：EventPublishingRegistrationAspect @After 语义错误

AspectJ `@After` 是 finally 语义——register() 抛异常后仍执行 → 注册失败却发布 RegistrationRegisteredEvent。

### REQ-D13：ReactiveDiscoveryClientAdapter 阻塞

`toList` 检测到非阻塞线程时用 `toFuture().get()`——阻塞事件循环。

### REQ-D14：TomcatDynamicConfigurationListener source 依赖

`isSourceFrom` 用 `context.equals(event.getSource())`——仅 `/actuator/refresh` 生效；EnvironmentManager 发布时 source 不匹配 → 动态配置失效。

### REQ-D15：endpoints.properties 入侵性默认关闭 SC 端点

jar 内属性文件 `management.endpoint.serviceregistry.enabled=false` 侵入性关闭 SC 原生端点。

### REQ-D16：双重注册风险

存在任何 Registration+ServiceRegistry 时即创建 @Primary MultipleAutoServiceRegistration，与 Nacos 原生 AutoServiceRegistration 并存→向同一注册中心注册两次。

---

## 九、发散需求（生产环境需要的全新能力）

### REQ-N01：Feign 热刷新的健康检查与回滚

**生产痛点**：配置中心推了新配置，Feign 热刷新生效了——但新配置可能导致 Feign 调用大面积失败（换了一个有 bug 的 Decoder）。没有"刷之前先验证"和"刷失败了回滚"的机制。

**产出**：`FeignHealthChecker` —— refresh 前先对目标 client 做一次 `health()` 调用（发一个 `/health` 的 Feign 请求），成功才刷；`FeignRollback` —— 保存上一次有效的 delegate 引用，health check 失败时回退。

**状态**：[待实现]（基于 `DecoratedFeignComponent.delegate` 的 volatile 替换机制，回滚只需保存旧引用）

---

### REQ-N02：多注册中心故障隔离

**生产痛点**：`MultipleServiceRegistry` 注册时遍历所有注册中心——如果一个注册中心挂了（如 Eureka Server 宕机），`register()` 抛异常会中断所有后续注册。生产需要"一个注册中心失败了，其他继续注册"的故障隔离。

**产出**：`FaultTolerantServiceRegistry` —— 每个子注册中心独立 try-catch，"Nacos 注册成功但 Eureka 注册失败"时发布 `RegistrationFailedEvent`（含失败原因）并继续下一个。`getStatus()` 返回 `Map<Class, String>` 而非单一 String。

**状态**：[待实现]

---

### REQ-N03：Feign 热刷新的作用域控制

**生产痛点**：`EnvironmentChangeEvent` 触发后，所有 Feign client 都被刷新——哪怕只改了一个 client 的配置。在大量 Feign client 的场景下（几十个 client），全量刷新耗时且无必要。

**产出**：`ScopeAwareRefreshStrategy` —— 根据变更 key 的 `groupingBy` 只刷新受影响的 client；`default` 配置变动仍刷新所有（保持现有行为）。

**状态**：[待实现]

---

### REQ-N04：注册状态 Prometheus 指标

**生产痛点**：`EventPublishingRegistrationAspect` 发布了事件，但没有数量化指标——过去一小时注册了多少次？注销了多少次？哪些服务频繁重注册（可能是网络抖动）？

**产出**：`RegistrationMetrics` —— 监听 `RegistrationRegisteredEvent`/`RegistrationDeregisteredEvent` → Micrometer Counter + Gauge，`registration_count_total{service, registry}` 和 `current_instances{service}`。

**状态**：[待实现]

---

### REQ-N05：HasFeatures 运行时模块发现端点

**生产痛点**：生产环境中你引用了哪些 microsphere 模块？各有什么能力？运维需要一个"告诉我这个应用启用了哪些 microsphere 能力"的入口。`ConfigurationPropertyHasFeaturesAutoConfiguration` 已经通过 `features.yaml` 注册了能力类探测体系，但没有暴露为可查询的端点。

**产出**：`/actuator/microsphere/features` —— 基于已有的 `HasFeatures` bean + `FeaturesProperties`，输出当前应用中启用的模块列表及其能力类清单（包含 loaded/unloaded 状态标注）。

**状态**：[待实现]（基于已有的 HasFeatures 探测体系，只缺端点暴露层）

---

## 十、版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| 0.2.x-SNAPSHOT | 2024~2025 | 持续开发，Spring Cloud 2022~2025.x 兼容 |
| — | 2026-08-02 | REQ 文档编写（第一版） |
