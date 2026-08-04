# 16-03：WebFlux 请求链路深度 —— WebEndpointMappingGlobalFilter

> **核心命题**：一条 `GET /orders-service/api/orders` 到达网关后，如何被翻译成 `http://10.0.0.5:8080/api/orders`？本文逐段拆解 `WebEndpointMappingGlobalFilter`（487 行，本模块最核心的类）：过滤器链位置（10149）、10 步请求处理、双缓存刷新、六维匹配，并给出 5 个已验证的问题与 1 个测试佐证的"手动补链"事实。

---

## 一、在过滤器链中的位置（order = 10149）

```java
public int getOrder() {
    return LOAD_BALANCER_CLIENT_FILTER_ORDER - 1;   // WebEndpointMappingGlobalFilter.java:213-214
}
```

常量值经 javap 反编译 SCG 4.1.2 jar 验证：`RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER = 10000`、`ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER = 10150`。因此请求链为：

| order | 过滤器 | 对 `GATEWAY_REQUEST_URL_ATTR` 的动作 |
|-------|--------|--------------------------------------|
| 10000 | `RouteToRequestUrlFilter` | 写入 route.uri（`we://all`） |
| **10149** | **`WebEndpointMappingGlobalFilter`** | 若命中端点：**改写为 `http://host:port/path`** |
| 10150 | `ReactiveLoadBalancerClientFilter` | 只处理 `lb://` scheme，改写后的 `http://` 直接透传 |
| ... | NettyRoutingFilter | 按 attr 真实发请求 |

**设计巧思**：microsphere 过滤器挤在"URL 初始化"和"LB"之间。它把 URL 从 `we://` 改成 `http://host:port` 后，**LB 过滤器就不再干预**（只认 `lb://`）——所以实例选择必须由 microsphere 自己做，这直接导致了第六节的阻塞问题（对比 MVC 版委托 `lb()` 的路线，见 16-04）。

**两级匹配架构（理解 16 的关键）**：请求进入网关后经过**两级匹配**——① `RoutePredicateHandlerMapping` 按路由 predicate（`Path=/{application}/**`）匹配到 we 路由；② 本过滤器在路由内再按端点 `RequestMappingInfo` 匹配。第一级是"路由级"（SCG 标准机制），第二级是"端点级"（microsphere 特有）。两级匹配的代价：**所有 `/{application}/**` 路径都先进入本过滤器**（哪怕端点表里没有该服务），未命中端点时落入"静默 200"（见下）；收益：路由定义只写一次，端点级控制（excludes、六维条件）全部收敛在过滤器内。

---

## 二、类结构总览

```java
public class WebEndpointMappingGlobalFilter implements
        GlobalFilter, SmartApplicationListener, ApplicationContextAware,
        EnvironmentAware, DisposableBean, Ordered
```

**双缓存**（写路径 `synchronized(this)` 整体换引用，读路径无锁 volatile）：

```java
volatile Map<String, Collection<RequestMappingContext>> routedRequestMappingContextsCache = null;
// routeId → 该路由订阅服务的全部端点（已编译为 RequestMappingInfo + id）
volatile Map<String, Collection<RequestMappingInfo>> routedExcludedRequestMappingInfoCache = null;
// routeId → 该路由配置的 excludes（已编译为 RequestMappingInfo）
```

- 刷新期间请求看到的要么是旧完整缓存、要么是新完整缓存，**没有中间态**（先构建局部 map，最后 synchronized 交换引用）。
- 缓存的 key 是 **route id** 而非服务名——一条 `we://all` 路由对应一个缓存条目；多服务订阅被聚合进同一 routeId 的集合。
- 缓存值为 null（未初始化）时请求直接放行（不拦截），见第七节测试佐证。

---

## 三、请求处理路径（filter()，10 步）

```java
public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    URI url = exchange.getAttribute(GATEWAY_REQUEST_URL_ATTR);
    if (isInvalidScheme(url)) {                 // ① scheme ≠ we → 放行（普通路由不受影响）
        return chain.filter(exchange);
    }
    String applicationName = uriTemplateVariables.get("application");  // ② 从 {application} 取服务名
    if (isBlank(applicationName)) {             // ③ 取不到 → 放行
        return chain.filter(exchange);
    }
    RequestMappingContext ctx = getMatchingRequestMappingContext(applicationName, exchange);  // ④ 匹配
    if (ctx != null) {                          // ⑤ 命中
        ServiceInstance instance = choose(applicationName);   // ⑥ 选实例（阻塞！见第六节）
        if (instance != null) {
            String uri = getUriString(instance);              // ⑦ http://host:port
            String rewritePath = (String) attributes.remove(WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME);  // ⑧ 取第④步存的改写路径
            URI targetURI = create(uri + rewritePath);        // ⑨ 拼接（context-path 缺失！见第六节）
            request = request.mutate().header("microsphere_wem_id", valueOf(ctx.id)).build();  // ⑩ 端点 id 打标
            attributes.put(GATEWAY_REQUEST_URL_ATTR, targetURI);
            return chain.filter(exchange.mutate().request(request).build());
        }
    }
    return chain.filter(exchange);              // 无匹配/无实例 → 原样放行
}
```

各步细节：

**② 服务名来源**：URI 模板变量 `{application}`——要求路由 predicate 必须是 `Path=/{application}/**`（`CommonConstants.APPLICATION_NAME_URI_TEMPLATE_VARIABLE_NAME = "application"`）。与 MVC 版的 `/we/{application}/**` 前缀约定不同（16-04 对比）。**注意**：这里**不校验服务是否存在**——`test-0` 这种不存在的服务只要路径能匹配到端点集合，就会走到选实例（选不到 → 放行 → 过滤器链空转，最终 200 空响应，见下"无匹配分支"）。

**④ 匹配（getMatchingRequestMappingContext，三步）**：

```java
if (isExcludedRequest(routeId, exchange)) return null;        // (a) 先查 excludes 缓存
...
String rewritePath = substringAfter(path, "/" + applicationName);  // (b) 剥 /{application} 前缀
exchange.getAttributes().put(WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME, rewritePath);
ServerWebExchange newExchange = exchange.mutate()
        .request(request.mutate().path(rewritePath).build()).build();
// (c) 用改写后的路径匹配 RequestMappingInfo
```

- (b) 是**纯字符串** `substringAfter`，不是 URL 语义处理——`/orders-service/api/orders` → `/api/orders`。改写的中间结果暂存在 exchange 属性 `msgw-we-rewrite-path`（`RouteConstants.WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME`），第⑧步取出使用。
- (c) 用 **mutate 后的新 exchange** 做匹配（原始 exchange 不动），因为端点 patterns 是 context-path 无关的绝对路径（provider 发布语义，见 16-07）。
- (a) 的 excludes 匹配同样用 `RequestMappingInfo.getMatchingCondition(exchange)`——**excludes 和端点共用同一套匹配机制**（16-06 详解配置来源）。

**⑤→⑨ 转发目标构建**：`getUriString(instance)`（`http://host:port`，secure 时 `https://`，port≤0 时按 80/443）`+ rewritePath`。

**⑩ `microsphere_wem_id` 头**：`WebEndpointMapping.ID_HEADER_NAME = "microsphere_wem_id"`，把命中的端点映射 id（`endpoint.hashCode()`，16-07 说明其语义）传给下游。**消费方已定位**：03 模块的 `ReversedProxyHandlerMapping`（webmvc/webflux 各一个）——接收端按此头**反查 `WebEndpointMappingRegistry` 直接命中对应 handler**（"反向代理"模式）。即：**正向（网关转发打标）↔ 反向（接收端还原）是一对配套设计**——没有 03 的 ReversedProxyHandlerMapping，这个头就是纯观测数据；配合使用则网关与接收端可跳过路径匹配直接按端点 id 对接。

**无匹配分支的行为（已源码验证，比想象更隐蔽）**：`chain.filter(exchange)` 放行后，`GATEWAY_REQUEST_URL_ATTR` 仍是 `we://all`。关键在 SCG 4.1.2 的 `NettyRoutingFilter`（order 2147483647，链尾）：

```java
String scheme = requestUrl.getScheme();
if (isAlreadyRouted(exchange) || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
    return chain.filter(exchange);   // ← 非 http/https 直接放行！
}
```

`ReactiveLoadBalancerClientFilter` 只认 `lb://`（放行）、`NettyRoutingFilter` 不认 `we://`（放行）→ **过滤器链以 `Mono.empty()` 结束，响应未提交，最终以 200 空响应返回**——"we 路由未命中端点"时请求被**静默吞掉**，既无错误日志也无 5xx（测试佐证：`/test-0/test/helloworld` 断言 isOk 通过，见第七节）。与"空过滤器数组吞请求"（16-05 的 CachingFilteringWebHandler 风险）同款失败模式。

**运维含义（两级匹配的代价落地）**：生产上"接口返回空"且网关无错误日志时，排障第一嫌疑就是 we 路由未命中端点（服务名写错、端点缓存未刷新、excludes 误配）。**无法从网关日志区分"正常空响应"与"被吞请求"**——这是 16 生产可观测性的真实缺口（与 17-03 的跨区域重试、11 的熔断配合等部署讨论对应的网关侧问题）。

---

## 四、缓存构建路径（refresh()，4 类事件触发）

```java
public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
    return ContextRefreshedEvent.class.equals(eventType)
            || RefreshRoutesResultEvent.class.equals(eventType)
            || EnvironmentChangeEvent.class.equals(eventType)
            || ServiceInstancesChangedEvent.class.equals(eventType);
}
```

| 事件 | 触发条件 | 语义 |
|------|----------|------|
| `ContextRefreshedEvent` | `this.context == event.getApplicationContext()` | 只认**自己上下文**（多上下文场景防误刷） |
| `RefreshRoutesResultEvent` | `isSuccessRouteLocatorEvent`：`success && source instanceof RouteLocator` | **只认成功**的路由刷新——失败不刷缓存，旧路由保持可用 |
| `EnvironmentChangeEvent` | key 形如 `spring.cloud.gateway.routes[N].id` 且值等于某 route id（`matchesEvent`） | 只关心**路由定义**变化，其他配置变更不触发 |
| `ServiceInstancesChangedEvent` | 无匹配条件 | **断链**：全工作区 main 代码无发布者（测试需手动 publish，见第七节）；16-05 详述 |

`refresh()` 本体：

```java
List<RouteDefinition> webEndpointRoutes = getWebEndpointRoutes();   // 只取 uri.scheme == "we"
for (RouteDefinition webEndpointRoute : webEndpointRoutes) {
    routedRequestMappingContextsMap.put(routeId, buildRequestMappingContexts(routeUri));
    routedExcludedRequestMappingInfoMap.put(routeId, buildExcludedRequestMappingInfoSet(routes, routeId));
}
synchronized (this) {   // 整体换引用
    this.routedRequestMappingContextsCache = ...;
    this.routedExcludedRequestMappingInfoCache = ...;
}
```

**路由来源是 `GatewayProperties.getRoutes()`（静态配置）**——与 2024 原始版的 `RouteLocator` 动态来源不同（16-02 演进史），这意味着 discovery.locator 生成的路由**不参与**端点映射缓存。

**服务订阅解析**：

```java
Collection<String> getSubscribedServices(URI routeUri) {
    String host = routeUri.getHost();
    if (ALL_SERVICES.equals(host)) {          // we://all → discoveryClient.getServices() 全量
        return discoveryClient.getServices();
    }
    return commaDelimitedListToSet(host);     // we://a,b → 显式订阅
}
```

---

## 五、缓存内容构建（buildRequestMappingContexts）

```java
private Collection<RequestMappingContext> buildRequestMappingContexts(URI routeUri) {
    Collection<String> subscribedServices = getSubscribedServices(routeUri);
    Collection<RequestMappingContext> requestMappingContexts = new LinkedList<>();
    // TODO support ZonePreferenceFilter        ← WebEndpointMappingGlobalFilter.java:275
    for (String subscribedService : subscribedServices) {
        ServiceInstance sampleServiceInstance = choose(subscribedService);   // 问题①
        Collection<WebEndpointMapping> webEndpointMappings = getWebEndpointMappings(sampleServiceInstance);  // 问题②
        for (WebEndpointMapping webEndpointMapping : webEndpointMappings) {
            requestMappingContexts.add(new RequestMappingContext(webEndpointMapping));
        }
    }
    return requestMappingContexts;
}
```

`RequestMappingContext` 内部把 `WebEndpointMapping` 编译为完整 `RequestMappingInfo`：

```java
static RequestMappingInfo buildRequestMappingInfo(WebEndpointMapping webEndpointMapping) {
    return paths(webEndpointMapping.getPatterns())
            .methods(buildRequestMethods(webEndpointMapping))
            .params(webEndpointMapping.getParams())
            .headers(webEndpointMapping.getHeaders())
            .consumes(webEndpointMapping.getConsumes())
            .produces(webEndpointMapping.getProduces())
            .build();
}
```

**六维条件**（path/method/param/header/consume/produce）与 Spring MVC 的 handler 映射语义完全一致——这是本设计的核心复用点：**把 Spring 的 `RequestMappingInfo` 当"路由匹配器"用**，`compareTo` 直接获得"最具体优先"排序（`/api/orders` 优先于 `/api/**`）。

---

## 六、问题清单（全部已源码验证）

### 问题①：阻塞选实例（性能隐患）

```java
private ServiceInstance choose(String applicationName) {      // WebEndpointMappingGlobalFilter.java:293-298
    ReactorLoadBalancer<ServiceInstance> loadBalancer =
            this.clientFactory.getInstance(applicationName, ReactorServiceInstanceLoadBalancer.class);
    Mono<Response<ServiceInstance>> mono = loadBalancer.choose();
    Response<ServiceInstance> response = getValue(mono);      // ← 阻塞
    return response.getServer();
}
```

`MonoUtils.getValue`（03 模块 `microsphere-spring-webflux`）：

```java
public static <T> T getValue(Mono<T> mono) {                  // MonoUtils.java:44-47
    if (isInNonBlockingThread()) {
        return execute(() -> mono.toFuture().get());   // 非阻塞线程上阻塞 → 卡 Netty 事件循环
    } else {
        return mono.block();
    }
}
```

WebFlux 网关的请求线程就是 Netty event loop（`isInNonBlockingThread()` 为 true）→ **每次 we 路由请求都 `toFuture().get()` 阻塞事件循环**直到 LB 返回。对纯内存策略（RoundRobin + 缓存 ListSupplier）阻塞窗口极小；但实例列表来自注册中心（Nacos 等，走远程订阅/查询）时，阻塞窗口随网络与注册中心延迟放大，事件循环线程被占满的风险陡增。对比：官方 `ReactiveLoadBalancerClientFilter` 是纯异步 `flatMap` 链；MVC 版没有此问题（16-04）。

### 问题②：context-path 丢失（功能退化）

第⑨步 `create(uri + rewritePath)` 拼出的 URL **不含服务的 context-path**。类里明明有处理函数：

```java
static String buildPath(ServiceInstance serviceInstance, URI url) {   // WebEndpointMappingGlobalFilter.java:466
    String servicePath = SLASH_CHAR + serviceInstance.getServiceId() + SLASH_CHAR;
    int index = path.indexOf(servicePath, 0);
    if (index != 0) {
        return path;                                    // 非 /{serviceId}/ 前缀 → 原样
    }
    String contextPath = metadata.get(WEB_CONTEXT_PATH_METADATA_NAME);
    return buildURI(contextPath, path.substring(servicePath.length()));
}
```

**已验证 `buildPath` 在主流程无人调用**（仅 `WebEndpointMappingGlobalFilterStaticTest` 引用）——2024 原始版在主流程使用（16-02 的 L1），2025-10-31 重构后成为死代码。后果：部署在 context-path 下的服务（如 `server.servlet.context-path=/shop`）网关转发 URL 缺 context-path，404。

### 问题③：refresh 路径 NPE（静默失效）

`buildRequestMappingContexts` 直接 `getWebEndpointMappings(sampleServiceInstance)`——若 `choose()` 返回 null（订阅的服务**瞬时无实例**）：

```java
// ServiceInstanceUtils.getWebEndpointMappings
String encodedJSON = getMetadata(serviceInstance, WEB_MAPPINGS_METADATA_NAME);  // ← serviceInstance.getMetadata() NPE
```

- NPE 发生在 `synchronized` 交换**之前** → 旧缓存原样保留（**不是崩溃，是静默失效**）。
- 刷新在事件监听器里触发；`RefreshRoutesResultEvent` 由 `CachingRouteLocator` 的 reactor `subscribe` 回调异步发布，NPE 抛在响应式链上——**无日志、不崩溃、缓存保持旧值，且服务恢复后不会自动补救**（需下一次成功事件或重启）。
- 对照：**MVC 版同一位置有 `if (sampleServiceInstance != null)` 防御**（`WebEndpointMappingHandlerFilterFunction.java:167`）——同一份逻辑，两实现健壮性不一致（16-04）。

### 问题④：样本实例（设计取舍）

`sampleServiceInstance` 每个服务只挑**一个**实例读元数据。实例间端点集不一致时（金丝雀、灰度、新旧版本并存），网关只认识被挑中那台的端点。原始版按端点聚合全部实例（16-02 L2），重构后收敛为样本——**简化了缓存模型，牺牲了完整性**。

### 问题⑤：GatewayUtils 死代码

`GatewayUtils.getGatewayProperties/getRouteProperties`（webflux 模块）主流程无人调用，仅 `GatewayUtilsTest` 引用——与 `buildPath` 同属"重构遗留"。

---

## 七、测试行为佐证（e2e 测试揭示了什么）

`WebEndpointMappingGlobalFilterTest`（@SpringBootTest + RANDOM_PORT，`simple-service-registry,gateway` profile）值得注意的几点：

1. **手动补链**：测试里 `this.context.publishEvent(new ServiceInstancesChangedEvent(...))` **手动发布**实例变更事件（`WebEndpointMappingGlobalFilterTest.java:159`）——**测试作者都需要手动发，生产无发布者（16-05 断链结论的直接佐证）**。
2. **3 实例自注册**：`TestConfig` 监听 `RegistrationPreRegisteredEvent`，把**本进程**注册 3 次（`instances.add(registration)` ×3，`:107-109`）——`we://test-app` 请求实际上"网关转发给自己"（自环），验证全链路（路由匹配 → 选实例 → 改写 URL → 转发 → TestController 响应）。
3. **未初始化缓存放行 + 静默 200**：`testFilterOnNoRoutedRequestMappingContext` 先 `filter.clear(); filter.destroy();` 再请求 `/test-app/test/helloworld` 仍 `isOk`——缓存为 null 时请求直接放行，we 路由未命中端点时以 **200 空响应**结束（NettyRoutingFilter 放行非 http/https scheme，见第六节"无匹配分支"）。同理 `testFilterOnUnregisteredApplication` 的 `/test-0/test/helloworld`（服务不存在但路径匹配）也是 200——**未命中端点 ≠ 报错，是静默成功**。
4. **本地接口优先于网关路由（order 0 vs order 1）**：`/test/helloworld`（无服务名前缀的普通路径）返回 `helloWorld()` 正文——这不是"predicate 不命中"，而是 **WebFlux 的 `RequestMappingHandlerMapping`（order 0）优先于 `RoutePredicateHandlerMapping`（order 1，可配 `spring.cloud.gateway.handler-mapping.order`，SCG 源码 60 行）**：请求先被 TestController 的 `@GetMapping` 接住，网关过滤器链根本不参与。这是 SCG WebFlux 的标准行为——**网关路由与应用本地接口按 order 共存**，也是 we 路由没有"非 we 放行"需求的原因之一。

---

## 八、小结

WebFlux 版核心链路一句话：**we:// scheme 拦截 → 剥 `/{application}` 前缀 → excludes 先行 → `RequestMappingInfo` 六维匹配 + 最具体优先 → 阻塞选实例 → 拼 URL（缺 context-path）加 `microsphere_wem_id` 头 → 交给 LB 过滤器之后的链**。

四个已验证问题（引用时直接使用）：

| # | 问题 | 证据 |
|---|------|------|
| P1 | 阻塞选实例：`choose()` 在 Netty 事件循环上 `toFuture().get()` | `WebEndpointMappingGlobalFilter.java:293-298` + `MonoUtils.java:44-47` |
| P2 | context-path 丢失：`buildPath` 死代码（仅测试引用） | `WebEndpointMappingGlobalFilter.java:466-480` |
| P3 | refresh 路径 NPE：无实例服务 → `getWebEndpointMappings(null)` NPE，刷新静默失败且不自动补救 | `WebEndpointMappingGlobalFilter.java:277-278` + `ServiceInstanceUtils.java:110-111` |
| P4 | 样本实例：端点集只取一个实例的 | `WebEndpointMappingGlobalFilter.java:277` |

外加：P5 `GatewayUtils` 死代码（仅测试引用）；以及"实例变更事件需测试手动发布"这一断链佐证（16-05 展开）。
