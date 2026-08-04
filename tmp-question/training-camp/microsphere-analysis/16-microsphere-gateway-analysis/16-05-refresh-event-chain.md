# 16-05：刷新机制与事件链 —— 缓存一致性设计

> **核心命题**：网关的缓存（路由定义 → 端点集合 → 过滤器数组）什么时候重建？microsphere 用"一个拦截器 + 一个监听器 + 一个 BeanDefinition 偷换"把 SCG 默认的刷新时机改写为受控的。本文给出：SCG 官方刷新基线的完整机制（5 类事件 + 去抖）、microsphere 三组件的精确拦截范围、**修正后的启动时序链**（启动链路是通的）、完整事件矩阵，以及"实例变更 → 路由刷新"的断链结论。

---

## 一、SCG 默认的刷新机制（基线）

### 1.1 RouteRefreshListener：5 类事件，两类去抖

`RouteRefreshListener`（SCG 4.1.2 源码）监听 **5 类事件**：

| # | 事件 | 动作 | 说明 |
|---|------|------|------|
| 1 | `ContextRefreshedEvent` | `reset()` → 发 `RefreshRoutesEvent` | **排除 management 命名空间**（`WebServerApplicationContext.hasServerNamespace`） |
| 2 | `RefreshScopeRefreshedEvent` | `reset()` | 配置刷新（`@RefreshScope` 全量失效后） |
| 3 | `InstanceRegisteredEvent` | `reset()` | 本进程实例注册完成 |
| 4 | `ParentHeartbeatEvent` | `resetIfNeeded(value)` | **`HeartbeatMonitor.update(value)` 去抖**——值变化才 reset |
| 5 | `HeartbeatEvent` | `resetIfNeeded(value)` | 同上 |

`reset()` → `publishEvent(new RefreshRoutesEvent(this))`。

### 1.2 CachingRouteLocator：只响应、不主动

`CachingRouteLocator`（SCG 源码）**不是 SmartLifecycle**——它只在收到 `RefreshRoutesEvent` 时刷新（`onApplicationEvent`），随后发布 `RefreshRoutesResultEvent`（成功时 source=this，失败时携带 Throwable）：

```java
private void publishRefreshEvent(List<Signal<Route>> signals) {
    cache.put(CACHE_KEY, signals);
    applicationEventPublisher.publishEvent(new RefreshRoutesResultEvent(this));   // 成功
}
private void handleRefreshError(Throwable throwable) {
    ...
    applicationEventPublisher.publishEvent(new RefreshRoutesResultEvent(this, throwable));  // 失败
}
```

**推论（决定了 microsphere 的全部设计）**：没有"启动即刷新"的机制——启动后的第一次 `RefreshRoutesEvent` 来自 `RouteRefreshListener` 的 `ContextRefreshedEvent` 分支。**谁拦截了这条链，谁就掐断了整个刷新体系**；反之，只要这条链活着，启动时序就成立。

**全链路一致的失败策略（设计原则）**：三个缓存组件在刷新失败时都保留旧缓存——`CachingRouteLocator`（`handleRefreshError` 只发失败事件、不碰 `cache`）、`CachingFilteringWebHandler`（`isSuccessRouteLocatorEvent` 失败不重建）、`WebEndpointMappingGlobalFilter`（同条件）。**"刷新失败 = 旧数据继续服务"是贯穿三组件的 fail-safe 一致性策略**，也是"只认成功"背后的设计原则——后面各节反复出现的 `isSuccessRouteLocatorEvent` 判断都是它的落点。

---

## 二、microsphere 三组件总览（webflux 模块）

```
GatewayAutoConfiguration（@ConditionalOnMicrosphereGatewayEnabled）
├── @EnableEventExtension                          // 03 模块：注册 InterceptingApplicationEventMulticaster
├── @Import(DisabledHeartbeatEventRouteRefreshListenerInterceptor.class)   // ① 拦截器：禁心跳
├── @Import(PropagatingRefreshRoutesEventApplicationListener.class)        // ② 监听器：环境变更→RefreshRoutesEvent
└── @Import(FilteringWebHandlerBeanDefinitionRegistryPostProcessor.class)  // ③ BDRPP：偷换 FilteringWebHandler
```

---

## 三、① 拦截器：拦截机制与精确范围

### 3.1 机制（03 模块源码验证）

`@EnableEventExtension` → `EventExtensionRegistrar` 注册 `InterceptingApplicationEventMulticaster` 替换默认 multicaster：

```java
// InterceptingApplicationEventMulticaster（03 模块）
@Override
protected final void invokeListener(ApplicationListener<?> listener, ApplicationEvent event) {
    DefaultApplicationListenerInterceptorChain chain = new DefaultApplicationListenerInterceptorChain(
            this.applicationListenerInterceptors, this::doInvokeListener);
    chain.intercept(listener, event);       // 每个 listener×event 组合先过拦截器链
}
```

拦截器从 bean 工厂收集（`getSortedBeans(ApplicationListenerInterceptor.class)`）——microsphere 的拦截器作为 `@Import` 的 bean 进入工厂，被自动装配进拦截链。

### 3.2 拦截范围（精确，勿扩大化）

```java
// DisabledHeartbeatEventRouteRefreshListenerInterceptor.java:52-55
if (INTERCEPTED_CLASS.equals(listenerClass) && matchesHeartbeatEvent(eventClass)) {
    return;    // RouteRefreshListener + (HeartbeatEvent|ParentHeartbeatEvent) → 短路
}
chain.intercept(applicationListener, event);
```

**只拦 2 个组合**：`RouteRefreshListener` × `{HeartbeatEvent, ParentHeartbeatEvent}`。**其余 3 类事件（ContextRefreshed/RefreshScopeRefreshed/InstanceRegistered）对 RouteRefreshListener 的处理完全不受影响**——这是整个刷新体系活着的前提（见第五节启动时序）。

**设计意图（刷新成本是拦截的真正动机）**：一次 `RefreshRoutesEvent` 触发的是**全量刷新**——`CachingRouteLocator` 从所有 `RouteDefinitionLocator`（配置文件 + discovery locator + 自定义仓库）重新拉取并排序；随后两个缓存组件各重建一次：`WebEndpointMappingGlobalFilter` 对**每个订阅服务执行一次阻塞 `choose()`** 并解析全部 `web.mappings` JSON（16-03 第五节），`CachingFilteringWebHandler` 拉全量路由合并排序。服务集越大、一次刷新越贵；而心跳（Nacos 等每 5-10s 一次）每次都触发——高频全量刷新不可持续。**拦截心跳是成本考量，不是洁癖**。

**为什么不用官方开关（设计权衡，已证）**：SCG 的 `RouteRefreshListener` bean 自带开关 `spring.cloud.gateway.route-refresh-listener.enabled`（默认 true，`GatewayAutoConfiguration.java:265-268`）——但它是**全局禁用**：连 `ContextRefreshedEvent`（启动链）、`RefreshScopeRefreshedEvent`（配置刷新链）、`InstanceRegisteredEvent` 一起断掉，刷新体系整体死亡。microsphere 需要保留这三类、只禁心跳——**拦截器比官方开关粒度细一档**，这正是它存在而非配个开关的理由。

**注意**：该拦截器**只在 webflux 模块**——MVC 模块没有它，也**不需要**：SCG Server MVC 的路由函数在 refresh scope（16-04 1.3），没有 RouteRefreshListener 参与，心跳刷新问题在 MVC 体系里不存在。

---

## 四、② 监听器：配置变更 → 补发 RefreshRoutesEvent

```java
// PropagatingRefreshRoutesEventApplicationListener.java（48 行）
public void onApplicationEvent(EnvironmentChangeEvent event) {
    if (containsGatewayPropertyName(event.getKeys())) {
        context.publishEvent(new RefreshRoutesEvent(this));   // 补发官方刷新事件
    }
}
private boolean isGatewayPropertyName(String key) {
    return startsWith(key, PREFIX);   // "spring.cloud.gateway"
}
```

**作用**：`EnvironmentChangeEvent`（spring-cloud-context 标准事件：配置中心 refresh 端点 / `ConfigurationPropertiesRebinder` 发布）发生时，**只要变更 key 以 `spring.cloud.gateway` 开头**就补发 `RefreshRoutesEvent` → 走官方刷新链。

**粒度问题（已证）**：`startsWith("spring.cloud.gateway")` 是粗粒度——`spring.cloud.gateway.globalcors.*`、`spring.cloud.gateway.httpclient.*` 等**与路由定义无关**的变更也会触发全量刷新。`WebEndpointMappingGlobalFilter` 自己的二级缓存有精确到 route id 的过滤（16-03 第四节），但 `CachingRouteLocator` 的全量刷新躲不过。

**与 15-configuration 的关系**：15 的 `@PropertySourceExtension`/`PropertySourcesChangedEvent` 注解模型**不在**这条链上——16 走的是 spring-cloud-context 标准 `EnvironmentChangeEvent`（Nacos/Apollo 等经 Spring Cloud refresh 通道发布）。两套配置体系独立（16-06 也印证）。

**与官方通道的叠加**：`RouteRefreshListener` 的 `RefreshScopeRefreshedEvent` 分支未被拦截——配置刷新时（RefreshScope.refreshAll）**官方通道也在**。所以配置变更实际有**双通道**（EnvironmentChangeEvent→microsphere 补发 + RefreshScopeRefreshedEvent→官方 reset），都通向 RefreshRoutesEvent。

---

## 五、③ BDRPP + CachingFilteringWebHandler：每路由过滤器缓存

### 5.1 偷换（FilteringWebHandlerBeanDefinitionRegistryPostProcessor，57 行）

```java
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    String[] beanNames = beanFactory.getBeanNamesForType(FilteringWebHandler.class, false, false);
    for (String beanName : beanNames) {
        registry.removeBeanDefinition(beanName);
        registerBeanDefinition(registry, beanName, CachingFilteringWebHandler.class);   // 同 bean 名换类
    }
}
```

在 BeanDefinition 已注册、bean 未实例化阶段，把 SCG 自动装配的 `FilteringWebHandler` **按原名替换**为 `CachingFilteringWebHandler`。SCG 4.1.2 全库注入 `FilteringWebHandler` 的只有 `RoutePredicateHandlerMapping`（`GatewayAutoConfiguration.java:295` 构造器注入）——替换后它无感拿到缓存版。

### 5.2 缓存逻辑（CachingFilteringWebHandler，124 行）

```java
@Override
public Mono<Void> handle(ServerWebExchange exchange) {
    Route route = exchange.getRequiredAttribute(GATEWAY_ROUTE_ATTR);
    GatewayFilter[] routedGatewayFilters = getRoutedGatewayFilters(route);   // 按 routeId 查数组
    return new DefaultGatewayFilterChain(routedGatewayFilters).filter(exchange);
}

@EventListener(RefreshRoutesResultEvent.class)
public void onRefreshRoutesResultEvent(RefreshRoutesResultEvent event) {
    if (matchesEvent(event)) {          // isSuccessRouteLocatorEvent：只认成功 + RouteLocator 来源
        this.routedGatewayFiltersCache = buildRoutedGatewayFiltersCache((RouteLocator) event.getSource());
    }
}

private Map<String, GatewayFilter[]> buildRoutedGatewayFiltersCache(RouteLocator routeLocator) {
    Map<String, GatewayFilter[]> cache = new HashMap<>();
    routeLocator.getRoutes().subscribe(route -> {           // 拉全量路由
        cache.put(route.getId(), combineGatewayFilters(route));  // globalFilters + route filters 合并排序
    }).dispose();
    return cache;
}

private List<GatewayFilter> globalFilters() {
    return getFieldValue(this, "globalFilters");            // ← 反射读父类私有字段
}
```

**与官方原版的对比**：

| | SCG `FilteringWebHandler` | `CachingFilteringWebHandler` |
|---|---|---|
| 每请求 | 合并 globalFilters + 路由 filters + 排序（每次分配） | O(1) 查数组 |
| 缓存失效 | 无 | `RefreshRoutesResultEvent` 成功时整体重建 |
| 代价 | 每请求一次 `sort` | 每次刷新一次 `sort` |

**三个风险（已证）**：

- **风险 A（吞请求）**：`getRoutedGatewayFilters` 在缓存为 null 时返回 `EMPTY_FILTER_ARRAY` → `DefaultGatewayFilterChain` 直接 `return empty()`——请求被"成功结束"但无任何过滤器执行、无响应。**触发窗口**：启动后缓存未建立前（RefreshRoutesResultEvent 未到）。由于启动链路（第五节）会立即建缓存，窗口极小；但若启动刷新失败或极端时序下出现，后果是静默吞请求。
- **风险 B（反射脆弱）**：`getFieldValue(this, "globalFilters")` 反射读父类私有字段——SCG 升级改字段名/类型即静默失败（null → NPE）。
- **风险 C（阻塞消费）**：`routeLocator.getRoutes().subscribe(...).dispose()` 是"同步消费 Flux"的手工等价物——若 RouteLocator 内部依赖异步源（如 Redis 路由仓库），`subscribe` 后立即 `dispose` 可能只消费到部分路由。

**风险 D（双缓存不一致窗口，本模块特有）**：`CachingFilteringWebHandler`（过滤器数组缓存）与 `WebEndpointMappingGlobalFilter`（端点双缓存）都监听同一个 `RefreshRoutesResultEvent` 并各自重建——Spring 对同一事件的多个监听器**调用顺序不保证**（默认按注册序）。若端点缓存先重建、过滤器数组后重建（或反之），中间窗口期内请求可能命中"新端点表 + 旧过滤器数组"或"旧端点表 + 新过滤器数组"。实际影响有限：两个缓存的数据源不同（实例元数据 vs 路由定义），一次刷新中先后到达即产生毫秒级窗口；且刷新是低频事件、失败时两边都 fail-safe（见 1.2 设计原则）——**风险可控，但设计上未做原子化**（如"两个缓存由同一组件顺序重建"）。

---

## 六、启动时序链（修正版——启动链路是通的）

**关键修正**：拦截器只拦 2 类心跳事件，`ContextRefreshedEvent` 分支未被拦截——所以启动时刷新链完整：

```
SpringApplication.run
 → ContextRefreshedEvent（主上下文）
   → RouteRefreshListener.onApplicationEvent → reset()          【未被拦截】
     → RefreshRoutesEvent
       → CachingRouteLocator.onApplicationEvent → fetch → publishRefreshEvent
         → RefreshRoutesResultEvent(this)【success，source=RouteLocator】
           → CachingFilteringWebHandler.onRefreshRoutesResultEvent    → 建立过滤器数组缓存
           → WebEndpointMappingGlobalFilter.onApplicationEvent        → 建立端点双缓存
```

**两处并行触发**：`WebEndpointMappingGlobalFilter` 还**直接**监听 `ContextRefreshedEvent`（只认自己上下文）——它有两个启动触发点（直接 + 间接），`CachingFilteringWebHandler` 只有间接一个（靠 RouteRefreshListener 保证）。

**gateway 也是服务**：`InstanceRegisteredEvent`（本进程实例注册完成，spring-cloud-commons 官方注册流程发布）也会触发一次 refresh。

---

## 七、完整事件矩阵（触发源 → 事件 → 动作）

> 快速参照表；各行的展开见对应章节（启动链见六、断链见八）。

| 触发源 | 事件 | RouteRefreshListener（官方） | microsphere 监听器 | CachingFilteringWebHandler | WebEndpointMappingGlobalFilter |
|--------|------|------------------------------|--------------------|---------------------------|-------------------------------|
| 启动 | `ContextRefreshedEvent` | reset → RefreshRoutesEvent | —（自身直接 refresh） | — | refresh（自身上下文） |
| 心跳（值变） | `HeartbeatEvent`/`ParentHeartbeatEvent` | **被拦截** | — | — | — |
| 配置刷新 | `RefreshScopeRefreshedEvent` | reset → RefreshRoutesEvent | — | — | — |
| 配置变更 | `EnvironmentChangeEvent` | — | **补发** RefreshRoutesEvent（`spring.cloud.gateway` 前缀） | — | refresh（按 route id 匹配） |
| 路由刷新结果 | `RefreshRoutesResultEvent` | — | — | **成功才重建** | **成功才重建** |
| 本实例注册 | `InstanceRegisteredEvent` | reset → RefreshRoutesEvent | — | — | — |
| 服务实例变更 | `ServiceInstancesChangedEvent` | — | — | — | refresh（**断链，见下节**） |

**MVC 模块的事件矩阵不同**（16-04 已述）：路由重建走 refresh scope（RefreshScopeRefreshedEvent）；端点缓存由 `HandlerConfig` 监听 ContextRefreshed/EnvironmentChange/ServiceInstancesChanged。

---

## 八、断链结论：实例变更 → 路由刷新，链路是断的

三个事实（均已源码验证）：

1. **心跳刷新被禁用**（webflux 模块）：`DisabledHeartbeatEventRouteRefreshListenerInterceptor` 掐掉 `RouteRefreshListener` 对 `HeartbeatEvent`/`ParentHeartbeatEvent` 的响应。
2. **`ServiceInstancesChangedEvent` 无发布者**：事件定义于 05 模块（`microsphere-spring-cloud-commons`），但**全工作区 main 代码中没有任何地方 `new ServiceInstancesChangedEvent(...)`**——注册中心只发 `Registration*` 事件（`EventPublishingRegistrationAspect` AOP，05 模块验证）。
3. **两个网关实现都依赖它**：WebFlux 版 `WebEndpointMappingGlobalFilter` 监听（`onServiceInstancesChangedEvent()` → refresh）；MVC 版 `HandlerConfig` 监听。测试都需**手动 publish**（WebFlux 测试 :159、MVC 测试 :174）——测试作者都绕不开的缺口。

**结论**：默认配置下，**服务实例上下线不会触发网关路由/端点缓存刷新**。场景推演：
- 新服务实例上线（含滚动升级改了 API 路径）：网关端点缓存**永不更新**，直到配置变更或重启
- 实例下线：已缓存端点仍尝试转发 → 选实例时 LB 返回失败（WebFlux 版 choose 阻塞返回空 → 放行 → 静默 200；MVC 版 lb() 抛 503）

**与 17-multiactive 的同构**（HANDOVER 已记录）：17 的"配置中心改 zone → setZone()"链路断（无监听 PropertySourcesChangedEvent 的组件）；16 的"实例变更 → 刷新"链路断（无发布者）。**microsphere 各项目共同的模式：事件接口已预留、发布者缺失**。补全方式二选一：在注册中心监听侧（Nacos 订阅回调等）发布 `ServiceInstancesChangedEvent`；或调整拦截器放行心跳事件（恢复官方心跳刷新，回到"每心跳全量刷新"的噪声模式）。

**与 18-dynamic 的对比**：18 的 `DynamicDataSource` 热替换有完整事件发布链（动态数据源模块自行发布变更事件）；16 的发布链断在起点——同是"热替换"主题，18 是闭环、16 是半成品。

---

## 九、事件作用域（与 18-04 结论一致）

- `WebEndpointMappingGlobalFilter` 的 `ContextRefreshedEvent` 匹配条件 `this.context == event.getApplicationContext()`（16-03）；MVC 版 `refresh()` 的 `context != this.context` 守卫（16-04）——都隐含"子上下文发布的事件，父上下文监听器收不到"。
- 若网关跑在 18-dynamic 的多子上下文结构里，各上下文需各自持有过滤器/监听器实例——**MVC 版的 static 缓存（16-04 问题①）在此场景直接串上下文**。

---

## 十、小结（引用要点）

- **刷新链的命门是 `RouteRefreshListener` 的 ContextRefreshedEvent 分支**——拦截器只拦 2 类心跳事件，启动链路因此是通的（修正了"窗口期吞请求"的旧表述）。
- **拦截心跳是成本考量**：一次刷新 = 全量拉路由 + 每订阅服务一次阻塞 choose + 双缓存重建，高频心跳刷新不可持续；**拦截器比官方开关（`spring.cloud.gateway.route-refresh-listener.enabled`，全局禁用）粒度细一档**——这是它存在而非配开关的理由。
- **配置变更双通道**：EnvironmentChangeEvent（microsphere 补发，粗粒度）+ RefreshScopeRefreshedEvent（官方 reset）——都通。
- **fail-safe 原则**："刷新失败 = 旧数据继续服务"是 `CachingRouteLocator`/`CachingFilteringWebHandler`/`WebEndpointMappingGlobalFilter` 三组件一致的安全策略。
- **断链一处**：`ServiceInstancesChangedEvent` 无发布者 + 心跳被禁 → 实例上下线网关无感知；测试手动 publish 是直接佐证；与 17 的断链同构。
- **CachingFilteringWebHandler 四风险**：空数组吞请求（窗口极小）、反射读私有字段（升级脆弱）、subscribe-dispose 阻塞消费（异步源不完整）、**双缓存不一致窗口**（两组件独立重建，毫秒级、未原子化）。
