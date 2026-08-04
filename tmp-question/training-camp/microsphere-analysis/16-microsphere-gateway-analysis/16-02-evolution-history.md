# 16-02：演进史——从"自研负载均衡"到"拥抱 SCG 生态"

> **核心命题**：16 的当前代码形态不是一次设计出来的——352 个提交揭示了两代架构：2024-03 的原始设计是"网关自管实例列表 + 可插拔实例谓词 + 自研轮询负载均衡"的独立路线；2025-10-31 一次重构（f227106）转向"SCG LoadBalancer 生态"，**带走了 6 项能力**（context-path 处理、实例聚合、谓词插件点、无阻塞自选实例、按服务排除、每路由自定义 LB）。当前代码里的全部"怪相"——死代码 `buildPath`、阻塞 `choose()`、`TODO support ZonePreferenceFilter`、样本实例——都是这次重构的遗产。理解重构，才能解释"为什么是今天这个样子"。

---

## 一、时间线总览（三阶段）

```
2024-02-26  Initial commit
2024-03-26~28  原型期：pom + 双模块（gateway / gateway-mvc 存根）+ 首次代码提交（2a40e27）
2024-04~08    基建期：Maven wrapper、CI workflow、profile 化依赖——此后仅 pom 微调
──────────────────────── 沉寂 14 个月 ────────────────────────
2025-10-27    恢复活跃：Refactor Maven workflows and update dependencies
2025-10-28~29 测试补全潮：事件/拦截器/条件注解测试 + filter 渐进重构（c4c649a、e0b5251）
2025-10-31    ★ 核心重构 f227106："Refactor WebEndpointMappingGlobalFilter for reactive discovery"
2025-11-01    删除 LoadBalancer 接口（694b080）、ServiceInstancePredicate（e4cf1d1）
2025-11-08    MVC 模块真身（55eae9d："Add WebEndpointMapping support for Spring Cloud Gateway MVC"）
2025-11-11~12 模块拆分 commons/webflux/webmvc（58f5f4b）+ excludes/事件/常量重构
──────────────────────── 又一轮沉寂（2025-12 ~ 2026-06） ────────────────────────
2026-06-20    MVC 条件注解细化（Require Web MVC and order after WebMvcAutoConfiguration）
2026-07-01    features.yaml 注册（"Register gateway features metadata"）、BindListener 重命名
2026-07-09~11 开关语义放宽、WebFlux 条件守卫，最后一笔提交 2026-07-11（CI 合并）
```

**项目状态画像**：2024 年原型后**沉睡 14 个月**，2025-10 底到 2025-11 中旬 3 周内完成全部核心工作，之后又是半年沉寂——典型"作者集中爆发式开发"，README 的 CI 矩阵（4 个 Spring Cloud train × 3 个 JDK）是自动化兜底，不是持续投入。

---

## 二、2024-03 原始设计解剖（2a40e27，filter 461 行）

原始模块名就叫 `microsphere-spring-cloud-gateway`（现 webflux）和 `microsphere-spring-cloud-gateway-mvc`——后者**当时只有 pom + 复制来的 spring.factories + 测试存根，零 main 代码**（2024 年 MVC 版只是占位，真身 2025-11-08 才落地）。

**一个重要事实：461 行的原始 filter 零测试**。2a40e27 的测试文件只有 `CachingFilteringWebHandlerTest`（46 行）和两个 yaml——**核心的 `WebEndpointMappingGlobalFilter`（自研 LB + 谓词过滤 + context-path 逻辑）从未被任何测试保护过**。这解释了 2025-10-31 重构为何能毫无负担地删掉整套自研 LB：没有测试 = 没有回归成本，也意味着**原始设计的"正确性"从未被验证过**（它可能根本没在真实流量下跑过）。

原始 `WebEndpointMappingGlobalFilter`（461 行）与现在的五点本质差异：

### 1. 实例聚合 + 请求级轮询（自研 LB）

```java
static class RequestMappingContext {
    private List<ServiceInstance> serviceInstances = new LinkedList<>();  // 聚合该端点全部实例
    private final AtomicInteger position = new AtomicInteger(0);

    void addServiceInstance(ServiceInstance serviceInstance) { ... }

    ServiceInstance choose(ServerWebExchange exchange) {
        List<ServiceInstance> serviceInstances = this.serviceInstances.stream()
                .filter(si -> serviceInstancePredicate(exchange, si))   // 谓词过滤
                .collect(Collectors.toList());
        ...
        int offset = size == 1 ? 0 : this.position.incrementAndGet() % size;  // 轮询
        return serviceInstances.get(offset);
    }
}
```

**关键**：刷新时 `discoveryClient.getInstances(service).stream().flatMap(...)` 把**该端点的所有实例**聚合进 `RequestMappingContext`（`mappedContexts.computeIfAbsent(webEndpointMapping, ...)` 按端点去重），请求时在**端点到实例**粒度做轮询——这是"端点级负载均衡"，比现在的"服务级样本实例"细一个粒度。配套还有 `LoadBalancer<T>` 接口（`choose(elements)` + `getName()`）和 `Config.loadBalancer`（String，每路由可配自定义 LB bean 名）——**路由可配置负载均衡器**的能力。

### 2. ServiceInstancePredicate 插件点（Zone 的原定挂载点）

```java
public interface ServiceInstancePredicate {
    boolean test(ServerWebExchange exchange, ServiceInstance serviceInstance);
}
```

filter 提供 `setWebEndpointServiceInstanceChooseHandler(ServiceInstancePredicate)` setter，`choose()` 前先用谓词过滤实例。这个插件点的语义就是"按请求上下文挑选可用实例"——**ZonePreferenceFilter 如果当时接入，就是在这里**。注意当前代码里 `// TODO support ZonePreferenceFilter` 恰好写在 `buildRequestMappingContexts`（样本实例选择处）——TODO 是重构后重新找的挂载点，而原始挂载点（谓词插件）在重构中被删除了。

### 3. 路径处理：context-path 是有实现的功能

```java
private String buildPath(ServiceInstance serviceInstance, URI url) {
    String servicePath = "/" + serviceInstance.getServiceId().toLowerCase();
    if (path.startsWith(servicePath)) {
        return path.replaceFirst(servicePath, contextPath);   // metadata 里的 web.context-path
    }
}
```

`buildBasePath` + `buildPath` 组合：`http://host:port` + 用 `web.context-path` **替换** `/{serviceId}` 前缀——部署在 context-path 下的服务是能正确转发的。2025-10-29 还专门修过一次（e0b5251，"Fix context path replacement"，改用 `buildURI(contextPath, path.substring(...))` 并加 `index != 0` 防御）——**说明这条路径直到重构前夜都是活代码**。

### 4. 配置模型：Config 比现在宽

```java
static class Config {
    Exclude exclude = new Exclude();      // exclude.services（Set<String>，整服务排除！）+ patterns/methods
    String loadBalancer;                  // 每路由自定义 LB bean 名
}
```

`createConfig(route)` 从 `route.getMetadata()["web-endpoint"]` 取扁平属性 → `BindableConfigurationBeanBinder.bind(...)` → `Config.init()`。**原始配置支持按服务排除（`exclude.services`）**，现在的 `WebEndpointConfig` 只剩 excludes 六维条件，`exclude.services` 和 `loadBalancer` 都被删了。

### 5. 事件与路由来源

```java
public class WebEndpointMappingGlobalFilter implements
        GlobalFilter, ApplicationListener<RefreshRoutesResultEvent>, DisposableBean, Ordered {
    public void onApplicationEvent(RefreshRoutesResultEvent event) {
        RouteLocator routeLocator = (RouteLocator) event.getSource();
        routeLocator.getRoutes().filter(this::isWebEndpointRoute).subscribe(route -> {...}).dispose();
    }
}
```

- **只监听一个事件**（RefreshRoutesResultEvent），且路由从 `RouteLocator`（动态组合，含 discovery locator）拉取——**比现在从 `GatewayProperties`（静态配置）取路由更接近 SCG 生态**（能感知 discovery.locator 生成的路由）。
- 注册方式还是 Boot 2 风格：`spring.factories` 的 `EnableAutoConfiguration` 键；`GatewayAutoConfiguration` 用 `@ConditionalOnProperty("spring.cloud.gateway.enabled")` + `@EnableEventExtension(intercepted = true)`。

---

## 三、2025-10-31 核心重构（f227106）

commit message 原文：

> "Updated WebEndpointMappingGlobalFilter and its auto-configuration to use ReactiveDiscoveryClient and LoadBalancerClientFactory, removing legacy ServiceInstancePredicate logic. Adjusted filter logic to extract application name from URI template variables and use reactive load balancing for service instance selection. Updated tests and configuration to match the new approach and removed unused or obsolete code."

改动：**5 个文件共 +114/-160**，其中 filter 单文件 187 行变动（+/- 混合）：

| 维度 | 重构前（2024 原始 + 10 月渐进） | 重构后（当前形态） |
|------|-------------------------------|-------------------|
| 实例来源 | 聚合端点全部实例（`getInstances` 全量） | **每服务一个样本实例**（`choose()` 挑一个读元数据） |
| 实例选择 | 自研轮询（`position % size`）+ 谓词过滤 | **阻塞委托** SCG LoadBalancer（`LoadBalancerClientFactory.getInstance(...).choose()` + `MonoUtils.getValue`） |
| 插件点 | `ServiceInstancePredicate` setter | **删除** |
| 服务名提取 | `pathWithinApplication`/`modifyContextPath` 上下文路径推导 | **URI 模板变量** `{application}`（要求 predicate 用 `Path=/{application}/**`） |
| context-path | `buildPath` 主流程调用（元数据替换） | `buildPath` 保留但**主流程不再调用**（`getUriString + rewritePath` 拼接） |
| 事件 | 仅 RefreshRoutesResultEvent | 4 类事件（ContextRefreshed/RefreshRoutesResult/EnvironmentChange/ServiceInstancesChanged） |
| 配置注入 | 构造器 `DiscoveryClient` + setter | 构造器 `DiscoveryClient + LoadBalancerClientFactory + GatewayProperties` |
| 路由来源 | `RouteLocator` 动态 | `GatewayProperties.getRoutes()` 静态 |

**当晚的摇摆**：240ca41（10-31 19:36，f227106 之后 2 小时）"Replaces ReactiveDiscoveryClient and its adapter with DiscoveryClient"——作者先按 reactive 重构，当晚又改回 `DiscoveryClient`（最终形态：bean 方法注入 `ReactiveDiscoveryClientAdapter`，它实现 `DiscoveryClient` 接口——**用适配器把响应式发现桥接给阻塞签名**，但 `choose()` 里 `MonoUtils.getValue` 的阻塞问题反而被保留下来）。这一天的提交序列（f227106 → 240ca41）暴露了作者在 reactive/blocking 之间的犹豫，最后停在一个两不像的位置。

**重构动机的诚实标注**：commit message 只说了"怎么做"（改用 LoadBalancerClientFactory），没说"为什么"。从时间线看（2025-10-27 起 microsphere-spring-cloud 版本连续 bump、2025-11-08 MVC 版落地），合理推测是配合 Spring Cloud 2025 train / LoadBalancer 生态的兼容性工作，但**源码与提交均无直接证据**——本文不替作者补动机，只记录"转向 SCG 生态"这一客观方向。

**11-01 补刀**：删除 `LoadBalancer` 接口（694b080）和 `ServiceInstancePredicate`（e4cf1d1），commit message 明说 "unused or redundant"——**插件点正式死亡**。

---

## 四、重构损失清单（解释当前代码的"怪相"）

| # | 能力 | 重构前 | 重构后 | 对应怪相 |
|---|------|--------|--------|----------|
| L1 | context-path 转发 | 有（`buildPath` 元数据替换） | **丢**（方法留、调用删） | `buildPath` 死代码 + 上下文路径服务 404 隐患（16-03） |
| L2 | 实例聚合 | 端点→全部实例 | 每服务 1 样本实例 | 金丝雀/实例间端点不一致时路由表不完整（16-03） |
| L3 | 谓词插件点 | `ServiceInstancePredicate` | 删 | `TODO support ZonePreferenceFilter` 无挂载点（16-08） |
| L4 | 无阻塞选实例 | 自研轮询（纯内存） | **阻塞** `toFuture().get()` | Netty 事件循环被堵（16-03） |
| L5 | 按服务排除 | `Config.exclude.services` | 删 | 只能按端点条件排除（16-06） |
| L6 | 每路由自定义 LB | `Config.loadBalancer` | 删 | 固定走 SCG LoadBalancer |

**重构的得**（不能只列损失）：

| # | 获得 | 说明 |
|---|------|------|
| G1 | SCG LoadBalancer 生态 | Zone ListSupplier、权重、缓存等定制能力天然可用（17 的全局配置也因此对网关生效，见 16-08） |
| G2 | 事件体系完整化 | 从单事件到 4 类事件 + 条件匹配（16-05），刷新时机可控 |
| G3 | 结构更清晰 | filter 行数 461 → 487（微增），但内部类 `Config` 外移为 commons 的 `WebEndpointConfig`、常量外抽 `RouteConstants`，被双实现共享 |
| G4 | 双实现对齐 | MVC 版（2025-11-08）直接复用同一套语义（we://、excludes、`{application}` 模板），没有历史包袱 |

**复盘判断**：重构的动机是"向 SCG 标准看齐"（L1/L5/L6 是特性裁剪，G1 是战略收益），但执行粗糙——阻塞调用（L4）是重构**引入**的回归而非历史遗留，且 `MonoUtils.getValue` 的阻塞语义在响应式链路上从未被质疑过。**对训练营的启示**：一次"现代化重构"可能同时带来收益和隐性退化，评估重构要看"删掉的能力是否有人依赖"，而不是只看新代码是否更标准。

---

## 五、重构之后的演进（2025-11 ~ 2026-07）

**2025-11-08 MVC 真身（55eae9d，+1066 行）**：`WebEndpointMappingHandlerFilterFunction`（380 行）+ `WebEndpointMappingHandlerSupplier` + `WebEndpointMappingGatewayServerMvcAutoConfiguration`（169 行）。当前版本该 filter 为 310 行（后续迭代中调整）。同日配套条件注解（`ConditionalOnGatewayServerMvcAvailable/Enabled`）、集成测试、`application.yaml`（含 `/we/{application}/**` 的 predicate 约定）。

**2025-11-11~12 模块拆分与重构**：
- 58f5f4b + b4710cb：拆出 `commons`（`WebEndpointConfig`、条件注解、`CommonsPropertyConstants`），并新增 `ConfigUtils`（"Add ConfigUtils for binding WebEndpointConfig"）；webflux/webmvc 各自独立模块
- 7b1cfb7 + 11842e3：excludes 逻辑迁移到 `WebEndpointConfig` 模型；事件处理增强（`c0c768a`，EnvironmentChangeEvent 按 route id 精确匹配）
- 6c47d6d + a71f3ad：创建 `RouteConstants`（"Add RouteConstants and unit test"），随后从 filter 删除冗余常量定义改用之；`WebEndpointConfig` 检索改用 `ConfigUtils`（c8b36f2）
- 2ff79f5/90d43ea：缓存变量改名、`RequestMappingInfo` 构建内聚为内部类——**这是"代码质量收尾"，功能不变**

**2026-06-20 MVC 细化**："Require Web MVC and order after WebMvcAutoConfiguration"、"Add WebFlux availability condition to auto-config"——MVC 的条件体系向 webflux 对齐。

**2026-07-01 features.yaml**："Register gateway features metadata"——把四个核心类登记为 actuator `/features` 报告（16-01 第四节）；"Rename web endpoint bind advisor to listener"——`BindHandlerAdvisor` → `BindListener` 改名（语义从"顾问"收敛为"监听器"）。

**2026-07-09~11 收尾**："Relax gateway enabled property checks"（开关语义放宽）、"Guard WebFlux autoconfig by gateway presence"（webflux 自动配置加网关存在性守卫）——**最后一次有内容的提交后，只剩 CI 合并**。

---

## 六、小结（引用要点）

- **三代形态**：2024 原型（自研 LB 独立路线）→ 2025-10-31 重构（SCG 生态转向）→ 2025-11 拆分定稿；MVC 真身 2025-11-08 才落地（2024 的 mvc 模块是零代码存根）。
- **6 项能力在重构中消失**：context-path 转发（L1，变成死代码 `buildPath`）、端点级实例聚合（L2，变成样本实例）、谓词插件点（L3，`ServiceInstancePredicate` 被删，`TODO ZonePreferenceFilter` 失去原挂载点）、无阻塞选实例（L4，**重构引入阻塞回归**）、按服务排除（L5）、每路由自定义 LB（L6）。
- **重构的得**：SCG LoadBalancer 生态接入（G1，17 的 Zone 配置因此对网关生效）、事件体系完整（G2）、双实现语义统一（G4）。
- **项目画像**：2024 原型 → 沉寂 14 个月 → 3 周爆发完成全部核心 → 半年沉寂；单人维护、学习型项目。
