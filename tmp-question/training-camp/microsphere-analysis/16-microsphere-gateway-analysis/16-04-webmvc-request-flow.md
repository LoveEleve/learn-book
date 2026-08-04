# 16-04：MVC 请求链路深度 —— HandlerSupplier + HandlerFilterFunction

> **核心命题**：Spring Cloud Gateway 4.x 的 Server MVC 没有 GlobalFilter 概念——路由是函数式 `RouterFunction`，自定义 scheme 靠 `HandlerSupplier` SPI 的静态方法发现。microsphere 用 3 个类（Supplier 60 行 + FilterFunction 310 行 + 自动配置 188 行）实现与 WebFlux 版等价的能力，但**实例选择、阻塞语义、NPE 防御、缓存生命周期**四个维度与 WebFlux 版有本质差异。本文先讲清官方 SPI 机制（SCG 源码验证），再逐段拆解 microsphere 实现，最后给出两版对比表与 excludes 语义差异。

---

## 一、SCG Server MVC 的扩展机制（先搞清官方 SPI）

### 1.1 HandlerSupplier：自定义 scheme 的入口

```java
// spring.factories（microsphere webmvc 模块）
org.springframework.cloud.gateway.server.mvc.handler.HandlerSupplier=\
io.microsphere.spring.cloud.gateway.server.webmvc.filter.WebEndpointMappingHandlerSupplier
```

SCG 通过 `HandlerDiscoverer` 发现自定义 scheme：

```java
// HandlerDiscoverer.java（SCG 4.1.2 源码）
public class HandlerDiscoverer extends AbstractGatewayDiscoverer {
    @Override
    public void discover() {
        doDiscover(HandlerSupplier.class, HandlerFunction.class);     // 返回 HandlerFunction 的方法
        doDiscover(HandlerSupplier.class, HandlerDiscoverer.Result.class);  // 返回 Result 的方法
    }
}
```

`AbstractGatewayDiscoverer.doDiscover` 的机制（源码逐行）：

```java
List<T> suppliers = loadSuppliers(supplierClass);   // SpringFactoriesLoader 加载 HandlerSupplier
methods.addAll(supplier.get());                     // 收集 supplier 暴露的全部方法
if (returnType.isAssignableFrom(method.getReturnType())) {
    addOperationMethod(method);                     // 以【方法名】注册 OperationMethod
}
```

**关键**：方法名 = scheme 名。这就是 `WebEndpointMappingHandlerSupplier` 必须把 `we(RouteProperties)` 写成静态方法、且 `get()` 返回 `getClass().getMethods()` 的原因——让反射器找到它，以 "we" 为 key 注册。

### 1.2 RouterFunctionHolderFactory：scheme → 处理器调用

每个 route 构建 RouterFunction 时（`getRouterFunction(RouteProperties, routeId)`，源码）：

```java
builder.filter((request, next) -> {                    // ① 先注册：URL 初始化过滤器
    MvcUtils.setRequestUrl(request, routeProperties.getUri());  // GATEWAY_REQUEST_URL_ATTR = route.uri（we://all）
    return next.handle(request);
});
builder.before(BeforeFilterFunctions.routeId(routeId)); // ② routeId 写入 attribute

String scheme = routeProperties.getUri().getScheme();   // "we"
Optional<NormalizedOperationMethod> op = findOperation(handlerOperations, scheme.toLowerCase(), handlerArgs);
// 先试无参；失败后补 routeProperties 参数（"routeProperties" key）再试
Object response = invokeOperation(normalizedOpMethod, ...);   // 反射调用 we(RouteProperties)
if (response instanceof HandlerDiscoverer.Result result) {
    handlerFunction = result.getHandlerFunction();       // http()
    result.getFilters().forEach(builder::filter);        // ③ 后注册：microsphere 的 we 过滤器
}
```

**执行顺序（反编译 `RouterFunctionBuilder.build()` 验证）**：`filterFunctions.stream().reduce(HandlerFilterFunction::andThen)`——`andThen` 语义是"先 this 后 after"，而注册顺序是 setRequestUrl → we 函数 → http() handler。所以**实际执行序**：

```
setRequestUrl（GATEWAY_REQUEST_URL_ATTR = we://all）
  → we 函数（WebEndpointMappingHandlerFilterFunction：改写 URL + 匹配 + 调 lb）
    → lb()（官方 LoadBalancerFilterFunctions：选实例 + 重建 URL）
      → http() handler（按 URL attr 转发）
```

### 1.3 路由重建：refresh scope

`GatewayMvcPropertiesBeanDefinitionRegistrar`（SCG 源码）：

```java
// 注册 RouterFunctionHolder（不是 RouterFunction——避免 RouterFunctionMapping 拿到两个）
// 若存在 refreshScope bean → holder 设 scope = "refresh"
// 注册 DelegatingRouterFunction(RouterFunctionHolder) 给 RouterFunctionMapping
```

**路由函数在 refresh scope 里**：配置刷新（`RefreshScopeRefreshedEvent`）时 holder 重建 → 重新执行 `RouterFunctionHolderFactory` → 全部路由（含 we 路由）重建 → `we(RouteProperties)` 重新被反射调用。这是 MVC 版"路由定义变更"的官方刷新通道（16-05 详述事件矩阵）。

**关键推论：路由重建 ≠ 端点缓存重建（两条独立链路）**。`we()` 用 `computeIfAbsent(routeId, ...)`——refresh scope 重建路由时，**同一个 routeId 复用旧 filter function 实例**，其内部的 `requestMappingContexts`/`excludedRequestMappingInfoSet` **不随路由重建**；端点缓存只由 `HandlerConfig` 的 3 类事件驱动 `refresh()` 更新（16-04 第四节）。所以 MVC 版有两条互不影响的链路：路由函数（refresh scope，管"路由结构"）与端点缓存（事件驱动，管"端点内容"）——任何一条更新都不会自动带动另一条，理解这一点才能看懂 MVC 版的刷新时序。

---

## 二、WebEndpointMappingHandlerSupplier（60 行，三个关键点）

```java
public class WebEndpointMappingHandlerSupplier implements HandlerSupplier {

    private static final Map<String, WebEndpointMappingHandlerFilterFunction>
            handlerFilterFunctionsCache = new HashMap<>();          // ← 问题①：static

    @Override
    public Collection<Method> get() {
        return ofList(getClass().getMethods());                    // 暴露静态方法供 SPI 反射
    }

    public static Result we(RouteProperties routeProperties) {     // 方法名 we → 对应 we:// scheme
        String routeId = routeProperties.getId();
        WebEndpointMappingHandlerFilterFunction function =
                handlerFilterFunctionsCache.computeIfAbsent(routeId, WebEndpointMappingHandlerFilterFunction::new);
        return new Result(http(),          // 真正的转发 handler 仍是官方 http()
                ofList(function),          // 前置过滤器：we 逻辑
                emptyList());              // 后置过滤器：无
    }

    public static WebEndpointMappingHandlerFilterFunction getWebEndpointMappingHandlerFilterFunction(String routeId) {
        return handlerFilterFunctionsCache.get(routeId);           // 供 AutoConfiguration 刷新用
    }
}
```

**问题①：static Map 跨上下文共享**。所有 ApplicationContext 共用一份——18-dynamic 的多子上下文（各子上下文独立 refresh）或同 JVM 多网关实例时，相同 routeId 会命中**别的上下文创建的实例**。防御措施在 `refresh()` 的双守卫（见第三节），但 `we()` 创建阶段不做上下文隔离，`getWebEndpointMappingHandlerFilterFunction` 按 routeId 全局命中——隐患未被彻底消除。对比 17-multiactive 的 `ZoneContext` 单例：那是**有意**的全局（非 Spring 环境也要读），这里是**无意**的全局。

**与 WebFlux 的机制差异**：WebFlux 版过滤器是 **bean**（构造器注入 `DiscoveryClient`/`LoadBalancerClientFactory`/`GatewayProperties`），生命周期由容器管；MVC 版是 **static map 手工管理**（context 靠 `setApplicationContext` 事后补注入）——两套生命周期的根源差异。

---

## 三、WebEndpointMappingHandlerFilterFunction（310 行，逐段）

### 3.1 请求处理（filter()，与 WebFlux 10 步对照）

```java
public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
    String applicationName = request.pathVariables().get("application");   // ① {application} 变量
    request.attributes().put(GATEWAY_REQUEST_URL_ATTR, request.uri());     // ② 预置 URL attr（原始 URI）

    if (isBlank(applicationName)) return next.handle(request);             // ③ 无服务名 → 放行

    RequestMappingContext ctx = getMatchingRequestMappingContext(applicationName, request);  // ④ 匹配
    if (ctx == null) return next.handle(request);                          // ⑤ 无匹配 → 放行

    HandlerFilterFunction<ServerResponse, ServerResponse> lbHandlerFunctionDefinition = lb(applicationName);  // ⑥ 官方 LB 过滤器
    String rewritePath = (String) attributes.remove(WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME);   // ⑦ 改写路径
    int id = ctx.id;
    ServerRequest newRequest = from(request)
            .uri(create(rewritePath))                                      // ⑧ 新 URI（相对路径，仅 path）
            .header("microsphere_wem_id", valueOf(id))                     // ⑨ 端点 id 打标
            .build();
    attributes.put(GATEWAY_REQUEST_URL_ATTR, newRequest.uri());
    return lbHandlerFunctionDefinition.filter(newRequest, next);           // ⑩ 交给官方 lb()（选实例+重建URL）
}
```

**与 WebFlux 版的核心差异**：

| 步骤 | WebFlux（16-03） | MVC（本文） |
|------|------------------|-------------|
| ① 服务名 | `GATEWAY_REQUEST_URL_ATTR` 的 scheme 识别 + URI 模板变量 | `request.pathVariables()`（RouterFunction 匹配时提取） |
| ② 预置 URL | 无（RouteToRequestUrlFilter order 10000 已设 we://all） | **自己设** `GATEWAY_REQUEST_URL_ATTR = request.uri()`（MVC 无全局 URL 过滤器，匿名 setRequestUrl 过滤器先设了 we://all，这里覆盖为原始 URI） |
| ⑥ 实例选择 | **自选**（阻塞 `choose()`） | **委托**官方 `lb(applicationName)`（见 3.2） |
| ⑧ 改写 | `http://host:port` + rewritePath（绝对） | `create(rewritePath)`（**相对 URI**，scheme/host 交给 lb 的 reconstructURI 补全） |

### 3.2 实例选择：委托 lb()（与 WebFlux 的阻塞对比）

MVC 版**不做请求级实例选择**——`choose()` 只在**刷新缓存**时用：

```java
private ServiceInstance choose(String applicationName) {                 // WebEndpointMappingHandlerFilterFunction.java:192-196
    List<ServiceInstance> serviceInstances = this.discoveryClient.getInstances(applicationName);
    return serviceInstances.stream().findAny().orElse(null);   // 随机取一个，仅用于读元数据
}
```

真正的转发走官方 `lb(applicationName)`（`LoadBalancerFilterFunctions.lb`，SCG 源码）：

```java
LoadBalancerClient loadBalancerClient = clientFactory.getInstance(serviceId, LoadBalancerClient.class);
ServiceInstance instance = loadBalancerClient.choose(serviceId, lbRequest);   // 同步阻塞——但这是 Servlet 线程！
...
URI requestUrl = LoadBalancerUriTools.reconstructURI(serviceInstance, uri);   // 用相对 URI 重建 http://host:port/path
MvcUtils.setRequestUrl(request, requestUrl);                                  // 覆盖 URL attr
```

**相对 URI 的重建（已反编译 `LoadBalancerUriTools` 验证）**：`create(rewritePath)` 得到的 `/test/helloworld` 无 scheme/host——`reconstructURI` 的 `computeScheme` 对 null scheme 默认 `"http"`，host/port 用所选实例的，path 取原 URI 的，重建为 `http://host:port/test/helloworld`。集成测试通过（自环转发 200，RestTemplate 直连真实端口）证实了这条路径可行。

**阻塞语义对比（重要）**：`lb()` 的 `choose` 是同步阻塞，但 MVC 运行在 **Tomcat worker 线程池**（Servlet 同步模型），阻塞是常规操作；WebFlux 版的阻塞发生在 **Netty 事件循环**（16-03 P1）。**同样的"阻塞"在两个模型下危害完全不同**——这正是双实现各自合理性的关键。且 MVC 版走官方 LB 链，天然获得 17-multiactive 的 Zone ListSupplier 能力（16-08）。

### 3.3 匹配（getMatchingRequestMappingContext，副作用式改写）

```java
String path = request.path();
String rewritePath = substringAfter(path, "/" + applicationName);       // 剥前缀
request.attributes().put(WEB_ENDPOINT_REWRITE_PATH_ATTRIBUTE_NAME, rewritePath);

RequestPath rewriteRequestPath = parse(rewritePath, null);
setParsedRequestPath(rewriteRequestPath, servletRequest);              // ← 直接改原生 ServletRequest 的已解析路径

for (RequestMappingContext ctx : requestMappingContexts) {
    if (matches(ctx, servletRequest))                                   // getMatchingCondition(servletRequest)
        matches.add(ctx);
}
matches.sort((v1, v2) -> v1.compareTo(v2, servletRequest));            // 最具体优先
return first(matches);
```

**对比 WebFlux**：WebFlux 用 `exchange.mutate().request(...)` 生成**新 exchange** 匹配（不可变风格）；MVC 用 `setParsedRequestPath` **直接改写原生 request 的内部状态**（副作用式）——`getMatchingCondition` 内部基于 `RequestPath` 的模式匹配（含 `{id}` 模板变量提取）直接对改写后路径生效。两种风格都能工作，但 MVC 版"改了原对象"的副作用在过滤器链后续阶段可见，耦合更紧。

**excludes 语义差异（两版易踩的坑）**：`isExcludedRequest(servletRequest)` 用**原始未改写路径**匹配——所以：

| 版本 | excludes 的 patterns 写法 | 匹配对象 |
|------|---------------------------|----------|
| WebFlux | `/test-1/**`（含服务名前缀，不含网关前缀） | 原始路径 `/test-1/test/helloworld` |
| MVC | `/we/test-app/**`（含**网关前缀** /we + 服务名） | 原始路径 `/we/test-app/test/helloworld` |

同一份配置在两端不通用（WebFlux 测试 yaml 用 `/test-1/**`，MVC 测试 yaml 用 `/we/test-app/**`，均已验证）。

### 3.4 刷新（refresh()，双守卫）

```java
public void refresh(RouteProperties routeProperties, ApplicationContext context) {
    if (context != this.context) return;                    // 守卫①：上下文一致
    if (!Objects.equals(this.routeId, routeProperties.getId())) return;  // 守卫②：routeId 一致
    Collection<RequestMappingContext> ctxs = buildRequestMappingContexts(routeProperties);
    Set<RequestMappingInfo> excludes = buildExcludedRequestMappingInfoSet(routeProperties);
    synchronized (this) {                                   // 整体换引用
        this.requestMappingContexts = ctxs;
        this.excludedRequestMappingInfoSet = excludes;
    }
}
```

**双守卫正是 static 缓存（问题①）下的必要防御**。且 `buildRequestMappingContexts` 里有 WebFlux 版缺失的防御：

```java
ServiceInstance sampleServiceInstance = choose(subscribedService);
if (sampleServiceInstance != null) {                        // ← 167 行：WebFlux 版没有这个检查（16-03 P3）
    Collection<WebEndpointMapping> webEndpointMappings = getWebEndpointMappings(sampleServiceInstance);
    ...
}
```

---

## 四、自动装配：WebEndpointMappingGatewayServerMvcAutoConfiguration（188 行）

```java
@Configuration(proxyBeanMethods = false)
@ConditionalOnGatewayServerMvcAvailable
@ConditionalOnDiscoveryEnabled
@ConditionalOnBlockingDiscoveryEnabled          // 注意：要求【阻塞】DiscoveryClient（与 webflux 的 reactive 对应）
@ConditionalOnMicrosphereWebEndpointMappingEnabled
@ConditionalOnClass(name = {"...DiscoveryClient", "...LoadBalancerClientFactory"})
@AutoConfigureAfter(value = {WebMvcAutoConfiguration.class, DiscoveryClientAutoConfiguration.class}, name = {...})
@Import(WebEndpointMappingHandlerConfig.class)
```

内嵌的 `WebEndpointMappingHandlerConfig implements SmartApplicationListener`（`@ConditionalOnBean(GatewayMvcProperties, DiscoveryClient, LoadBalancerClientFactory)`）——**事件监听做在配置类里，执行体是 filter function**（WebFlux 版是 filter 自身实现 SmartApplicationListener，结构对比见 16-05）：

```java
// 支持 3 类事件
ContextRefreshedEvent     → refresh(routes, context, f -> f.setApplicationContext(context));  // 启动：补注入 context + DiscoveryClient
EnvironmentChangeEvent    → refresh(() -> findWebEndpointMappingRouteProperties(event.getKeys()));  // 只刷变更的 route
ServiceInstancesChangedEvent → refresh(this::getWebEndpointMappingRouteProperties);            // 全量（断链，16-05）

private List<RouteProperties> getWebEndpointMappingRouteProperties() {
    return this.gatewayMvcProperties.getRoutes().stream()
            .filter(r -> SCHEME.equals(r.getUri().getScheme()))      // 只处理 we://
            .collect(toUnmodifiableList());
}
```

`findWebEndpointMappingRouteProperties`：遍历 `spring.cloud.gateway.mvc.routes[N].id` 形式的 key，按 route id 值匹配——与 WebFlux 版 `matchesEvent(EnvironmentChangeEvent)` 同构。

---

## 五、两版实现对比表（八维）

| 维度 | WebFlux（16-03） | MVC（本文） |
|------|------------------|-------------|
| 扩展点 | SCG `GlobalFilter` bean（order 10149） | SCG `HandlerSupplier` SPI + `HandlerFilterFunction` |
| 实例选择 | 自选：`LoadBalancerClientFactory.choose()` **阻塞卡事件循环** | 委托官方 `lb()`（阻塞在 Servlet 线程，合理）+ `findAny()` 随机仅读元数据 |
| URL 改写 | `http://host:port + rewritePath`（绝对） | `create(rewritePath)`（相对，lb 的 reconstructURI 补全） |
| 匹配风格 | `exchange.mutate()` 新对象（不可变） | `setParsedRequestPath` 改原生 request（副作用） |
| excludes patterns | `/test-1/**`（含服务名，不含 /we） | `/we/test-app/**`（含 /we 网关前缀） |
| refresh NPE 防御 | **无**（16-03 P3） | **有**（167 行 null 检查） |
| 缓存生命周期 | 实例字段 + 4 类事件 | **static map** 跨上下文共享 + 双守卫 |
| 事件监听位置 | filter 自身 implements SmartApplicationListener | 配置类内嵌 HandlerConfig |
| 路由重建通道 | RefreshRoutesEvent → CachingRouteLocator → RefreshRoutesResultEvent | refresh scope（RefreshScopeRefreshedEvent）重建 RouterFunction + EnvironmentChangeEvent 刷端点缓存 |

---

## 六、测试行为佐证（MVC 集成测试揭示了什么）

`WebEndpointMappingHandlerFilterFunctionTest`（MockMvc + RANDOM_PORT，`simple-service-registry,gateway` profile）：

1. **/we 网关前缀**：请求 `GET /we/test-app/test/helloworld` → 200 + body——路由 predicate 是 `Path=/we/{application}/**`，`application=test-app`。
2. **excludes 命中即失败形态**：同路径加 `Content-Type: application/json` + `Accept: plain/text`（命中 `/we/test-app/**` + consumes + produces）→ `assertThrows(Exception)`——排除后放行 → `http()` handler 按当前 URL attr（we 函数第②步已覆盖为**原始请求 URI**，非 setRequestUrl 的 `we://all`）尝试转发 → MockMvc 无真实下游连接 → 异常。**MVC 版 excludes 命中是"显式异常"，WebFlux 版是"静默 200"**（16-03 无匹配分支）——失败形态完全不同。
3. **手动补链**：`webApplicationContext.publishEvent(new ServiceInstancesChangedEvent(...))`（测试 :174）——与 WebFlux 测试同款"手动发布"，断链佐证 +1。
4. **deregister 后请求失败**：`testConfig.deregister(registration)` + 手动事件 → 同请求 `assertThrows`——MVC 版刷新链路在手动事件下**确实工作**（lb() 找不到实例抛 503），反证生产缺的是发布者而非执行逻辑。
5. **不存在服务**：`/we/test-app2/test/helloworld` 抛异常——路径剥前缀后仍匹配端点集合 → `lb("test-app2")` 无实例抛 `HttpServerErrorException(503)`（与 WebFlux 版"静默 200"再次形成对比）。
6. **EnvironmentChangeEvent 精确刷新**：`GATEWAY_ROUTES_PROPERTY_NAME_PREFIX + "[0].id"` → 刷新后请求仍 200。

---

## 七、小结

MVC 版一句话：**HandlerSupplier SPI 的 `we(RouteProperties)` 静态工厂 → `Result(http(), [filterFn], [])` → 路由函数 refresh scope 重建 → 请求时 filterFn 剥前缀、六维匹配、委托官方 `lb()` 选实例转发**。

三个关键结论（引用时直接使用）：

- **MVC 版的"阻塞"不是问题**（Servlet 线程），WebFlux 版的"阻塞"是事故源（事件循环）——双实现各自合理，对比必须区分线程模型。
- **MVC 版无静态缺陷类问题**（NPE 防御、null 检查都有），但有 **static map 跨上下文共享**隐患；WebFlux 版反之——两版互为镜像。
- **excludes 与失败形态两版不通用**：excludes patterns 前缀不同（`/we/`）、未命中端点时 MVC 显式 503 / WebFlux 静默 200。
