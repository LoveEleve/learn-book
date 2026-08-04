# 03-07 Web 端点元数据统一

## 问题：MVC 和 WebFlux 的端点元数据不统一

Spring 有两套 Web 框架，各自有独立的端点元数据表示：

| 维度 | Spring WebMVC | Spring WebFlux |
|------|-------------|---------------|
| 端点映射 | `RequestMappingInfo` | `RequestMappingInfo`（不同包！） |
| Handler | `HandlerMethod` | `HandlerMethod`（不同包！） |
| HandlerMapping | `org.springframework.web.servlet.HandlerMapping` | `org.springframework.web.reactive.HandlerMapping` |
| 请求匹配 | `RequestCondition` 体系 | `RequestCondition` 体系（不同包！） |
| Servlet/Filter | `ServletRegistration`/`FilterRegistration` | 不适用 |

两套体系的类名相同但包不同，无法互操作。如果你要写一个"列出所有 HTTP 端点"的工具（如 API 文档生成、安全审计），需要分别处理 MVC 和 WebFlux，还要处理 Servlet/Filter 注册。

microsphere-spring 的 `web/metadata` 包通过 **`WebEndpointMapping` 统一元数据模型** 解决这个问题。

---

## 设计：统一元数据 + 工厂链 + 注册表

### 整体架构

```
@EnableWebExtension
    │
    ├── WebEndpointMappingRegistrar (SmartLifecycle)
    │       │
    │       ├── WebEndpointMappingResolver (MVC + WebFlux + Servlet)
    │       │       ├── 扫描 HandlerMapping 中的 RequestMappingInfo
    │       │       ├── 扫描 ServletRegistration
    │       │       └── 扫描 FilterRegistration
    │       │
    │       ├── SmartWebEndpointMappingFactory (复合工厂)
    │       │       ├── HandlerMappingWebEndpointMappingFactory (MVC)
    │       │       ├── HandlerMappingWebEndpointMappingFactory (WebFlux)
    │       │       ├── ServletRegistrationWebEndpointMappingFactory
    │       │       └── FilterRegistrationWebEndpointMappingFactory
    │       │
    │       └── WebEndpointMappingRegistry
    │               ├── SimpleWebEndpointMappingRegistry (Map by ID)
    │               └── CompositeWebEndpointMappingRegistry (委托多个 Registry)
    │
    └── WebEventPublisher (SmartLifecycle + HandlerMethodInterceptor)
            ├── 发布 HandlerMethodArgumentsResolvedEvent
            └── 发布 WebEndpointMappingsReadyEvent
```

---

### WebEndpointMapping：统一端点元数据

#### 数据模型

```java
public class WebEndpointMapping<E> {

    public enum Kind {
        SERVLET,      // jakarta.servlet.Servlet
        FILTER,       // jakarta.servlet.Filter
        WEB_MVC,      // Spring WebMVC DispatcherServlet
        WEB_FLUX,     // Spring WebFlux DispatcherHandler
        CUSTOMIZED    // 用户自定义
    }

    private final Kind kind;           // 端点类型
    private final E endpoint;           // 原始端点对象（HandlerMethod/ServletRegistration/...）
    private final int id;               // 唯一 ID（用于去重和 HTTP 头传递）
    private final String[] patterns;    // URL 路径模式
    private final String[] methods;     // HTTP 方法
    private final String[] params;      // 请求参数条件
    private final String[] headers;     // 请求头条件
    private final String[] consumes;    // Content-Type 条件
    private final String[] produces;    // Accept 条件
    private final boolean negated;      // 是否取反
    private final Object source;        // 来源（HandlerMapping 实例）
    private final Map<String, Object> attributes;  // 扩展属性
}
```

**设计要点**：

- **泛型 `E`**：`endpoint` 字段的类型是泛型，MVC 中是 `HandlerMethod`，WebFlux 中也是 `HandlerMethod`（不同包），Servlet 中是 `ServletRegistration`。泛型让类型安全，同时 `Kind` 枚举区分来源。
- **`id` 字段**：基于 `endpoint` 的 `hashCode` 计算的唯一标识。用于 Registry 去重和 HTTP 头传递（`microsphere_wem_id`）。
- **`source` 字段**：记录 `WebEndpointMapping` 的创建者（通常是 `HandlerMapping` 实例），用于追溯。
- **`transient` 修饰**：`endpoint`、`source`、`hashCode` 标记为 `transient`，序列化时不包含（因为 `HandlerMethod` 可能不可序列化）。
- **静态工厂方法**：`webmvc()`、`webflux()`、`servlet()`、`filter()`、`customized()` 创建对应 `Kind` 的 `Builder`。

#### Builder 模式

```java
WebEndpointMapping<?> mapping = WebEndpointMapping.webmvc()
        .endpoint(handlerMethod)
        .patterns("/api/users/{id}")
        .methods("GET", "POST")
        .consumes("application/json")
        .produces("application/json")
        .source(handlerMapping)
        .attribute("description", "User API")
        .build();
```

Builder 使用 `Set<String>` 收集值，`build()` 时转为 `String[]`。这确保了值的唯一性和不可变性。

---

### WebEndpointMappingFactory：工厂链

#### 工厂接口

```java
@FunctionalInterface
public interface WebEndpointMappingFactory<E> {
    default boolean supports(E endpoint) { return true; }
    Optional<WebEndpointMapping<E>> create(E endpoint);
    default Class<E> getSourceType() { /* 泛型解析 */ }
}
```

工厂负责将原始端点对象（`RequestMappingInfo`、`ServletRegistration` 等）转换为 `WebEndpointMapping`。`getSourceType()` 通过泛型解析自动确定工厂支持的端点类型。

#### AbstractWebEndpointMappingFactory

模板方法基类，`create` 方法包裹 try-catch，防止单个工厂异常影响整个链：

```java
public final Optional<WebEndpointMapping<E>> create(E endpoint) {
    try {
        return ofNullable(doCreate(endpoint));
    } catch (Throwable e) {
        logger.error("...", e);
        return empty();
    }
}
```

#### SmartWebEndpointMappingFactory：复合工厂

`SmartWebEndpointMappingFactory` 是工厂的工厂--它持有一组委托工厂，按端点类型路由：

```java
public class SmartWebEndpointMappingFactory extends AbstractWebEndpointMappingFactory<Object> {

    private final Map<Class<?>, List<WebEndpointMappingFactory>> delegates;

    protected WebEndpointMapping<?> doCreate(Object endpoint) {
        List<WebEndpointMappingFactory> factories = delegates.get(endpoint.getClass());
        for (WebEndpointMappingFactory factory : factories) {
            if (factory.supports(endpoint)) {
                Optional<WebEndpointMapping<Object>> result = factory.create(endpoint);
                if (result.isPresent()) return result.get();
            }
        }
        return null;
    }
}
```

**委托发现**：从 `SpringFactoriesLoader` 和 `BeanFactory.getBeansOfType` 两路加载工厂实现，按 `getSourceType()` 分组，同类型工厂按 `Ordered` 排序。

#### MVC 和 WebFlux 的平行工厂

MVC 和 WebFlux 各有 `HandlerMappingWebEndpointMappingFactory<H, M>`，结构完全平行：

```
MVC:  org.springframework.web.servlet.HandlerMapping
      -> HandlerMappingWebEndpointMappingFactory (web/mvc/metadata/)
      -> RequestMappingInfo -> WebEndpointMapping (Kind=WEB_MVC)

WebFlux: org.springframework.web.reactive.HandlerMapping
         -> HandlerMappingWebEndpointMappingFactory (web/webflux/metadata/)
         -> RequestMappingInfo -> WebEndpointMapping (Kind=WEB_FLUX)
```

两者的 `doCreate` 方法逻辑相同：从 `HandlerMetadata` 提取 handler 和 metadata，解析 patterns 和 methods，用 `webmvc()` 或 `webflux()` 工厂方法创建 Builder。差异仅在：
- `HandlerMapping` 的包不同（`servlet` vs `reactive`）
- `webmvc()` vs `webflux()` 工厂方法
- `contribute` 方法（子类覆盖，添加框架特定属性）

---

### WebEndpointMappingResolver：端点发现

`WebEndpointMappingResolver` 负责从 `ApplicationContext` 中发现所有端点：

```java
public interface WebEndpointMappingResolver {
    Collection<WebEndpointMapping> resolve(ApplicationContext context);
}
```

MVC 的 `HandlerMappingWebEndpointMappingResolver` 遍历所有 `HandlerMapping` Bean，从每个 `HandlerMapping` 的 `getHandler(request)` 结果中提取 `RequestMappingInfo`，通过 `SmartWebEndpointMappingFactory` 转换为 `WebEndpointMapping`。

WebFlux 的 `HandlerMappingWebEndpointMappingResolver` 逻辑类似，但适配 WebFlux 的响应式 `HandlerMapping`。

---

### WebEndpointMappingRegistry：注册表体系

```
WebEndpointMappingRegistry (接口)
    │
    ├── FilteringWebEndpointMappingRegistry (抽象，应用 WebEndpointMappingFilter)
    │       │
    │       ├── SimpleWebEndpointMappingRegistry
    │       │       Map<Integer, WebEndpointMapping> by ID
    │       │
    │       └── CompositeWebEndpointMappingRegistry
    │               委托多个 Registry（SmartInitializingSingleton 自动发现）
```

**FilteringWebEndpointMappingRegistry**：在 `register` 前应用 `WebEndpointMappingFilter`（函数式接口，继承 `Filter<WebEndpointMapping>`）。过滤器可以按 `Kind`、`source`、`patterns` 等条件筛选。多个过滤器通过 `FilterOperator`（默认 `OR`）组合--任一过滤器通过即接受注册。`register` 方法标记为 `final`，确保子类无法绕过过滤。

**SimpleWebEndpointMappingRegistry**：用 `Map<Integer, WebEndpointMapping>` 存储，以 `id` 为 key 实现去重。

**CompositeWebEndpointMappingRegistry**：在 `afterSingletonsInstantiated` 时从 BeanFactory 发现所有 `WebEndpointMappingRegistry` Bean（排除自身），委托注册和查询。这是组合模式的经典应用--多个 Registry 的注册结果可以被统一查询。

---

### WebEndpointMappingRegistrar：生命周期触发

`WebEndpointMappingRegistrar` 是 `SmartLifecycle`，在 `doStart` 中：

1. 获取 `WebEndpointMappingRegistry` Bean
2. 获取所有 `WebEndpointMappingResolver` Bean（从 BeanFactory + SpringFactoriesLoader）
3. 对每个 Resolver 调用 `resolve(context)` 获取映射
4. 将映射注册到 Registry
5. 记录注册数量

**Phase 设置**：`setPhase(WebEventPublisher.DEFAULT_PHASE - 10)`，确保 Registrar 在 `WebEventPublisher` 之前启动。这样 `WebEventPublisher` 启动时所有映射已注册完毕，可以发布 `WebEndpointMappingsReadyEvent`。

---

### WebRequestRule：请求匹配规则

`WebRequestRule` 是独立于 MVC/WebFlux 的请求匹配抽象：

```java
public interface WebRequestRule {
    boolean matches(NativeWebRequest request);
}
```

`NativeWebRequest` 是 Spring 的通用请求抽象，MVC 的 `HttpServletRequest` 和 WebFlux 的 `ServerWebExchange` 都可以适配。

**规则体系**：

```
WebRequestRule (接口)
    │
    ├── AbstractWebRequestRule<T> (抽象基类)
    │       ├── getContent() -> Collection<T>    // 离散项
    │       └── getToStringInfix() -> String      // 拼接符
    │
    ├── CompositeWebRequestRule (AND 组合)
    │       所有规则都匹配才返回 true
    │
    ├── WebRequestMethodsRule (HTTP 方法匹配)
    ├── WebRequestPattensRule (URL 路径匹配)
    ├── WebRequestParamsRule (参数匹配)
    ├── WebRequestHeadersRule (请求头匹配)
    ├── WebRequestConsumesRule (Content-Type 匹配)
    └── WebRequestProducesRule (Accept 匹配)
```

**表达式体系**：

- `MediaTypeExpression` / `ConsumeMediaTypeExpression` / `ProduceMediaTypeExpression` - 媒体类型匹配
- `NameValueExpression` / `WebRequestParamExpression` / `WebRequestHeaderExpression` - 名称值匹配

这套规则体系与 Spring MVC 的 `RequestCondition` 平行，但不依赖 MVC API。它可以用于：
- 运行时请求匹配（不依赖框架特定的 `HandlerMapping`）
- 端点过滤（根据规则筛选 `WebEndpointMapping`）
- 测试辅助（构造请求验证端点匹配）

---

### WebEventPublisher：事件发布

`WebEventPublisher` 是 `SmartLifecycle` + `HandlerMethodInterceptor`：

**作为 HandlerMethodInterceptor**：
- `beforeExecute` 方法在 HandlerMethod 执行前发布 `HandlerMethodArgumentsResolvedEvent`，包含请求、HandlerMethod、参数

**作为 SmartLifecycle**：
- `doStart` 时从 Registry 获取所有 `WebEndpointMapping`，发布 `WebEndpointMappingsReadyEvent`
- `doStop` 时不做任何事

`WebEndpointMappingsReadyEvent` 携带所有已注册的 `WebEndpointMapping` 集合（不可变），用户可以通过 `@EventListener` 接收：

```java
@EventListener
public void onMappingsReady(WebEndpointMappingsReadyEvent event) {
    event.getMappings().forEach(mapping -> {
        System.out.printf("[%s] %s %s%n",
            mapping.getKind(),
            Arrays.toString(mapping.getMethods()),
            Arrays.toString(mapping.getPatterns()));
    });
}
```

---

### @EnableWebExtension：启用注解

```java
@EnableWebExtension(
    registerWebEndpointMappings = true,   // 注册端点映射（默认 true）
    interceptHandlerMethods = true,        // 拦截 HandlerMethod（默认 true）
    publishEvents = true,                  // 发布 Web 事件（默认 true）
    sources = {BEAN_FACTORY, SPRING_FACTORIES, JAVA_SERVICE_PROVIDER},  // 发现来源
    requestContextStrategy = DEFAULT       // RequestContextStrategy（默认 DEFAULT）
)
@Configuration
public class WebConfig { }
```

`WebExtensionBeanDefinitionRegistrar` 根据属性注册：
- `registerWebEndpointMappings=true`：注册 `WebEndpointMappingResolver`、`WebEndpointMappingRegistry`、`SmartWebEndpointMappingFactory`、`WebEndpointMappingRegistrar`
- `interceptHandlerMethods=true`：注册 `HandlerMethodInterceptor`、`HandlerMethodArgumentInterceptor`、`HandlerMethodAdvice`（第 8 篇主题）
- `publishEvents=true`：注册 `WebEventPublisher`，发布 `WebEndpointMappingsReadyEvent` 和 `HandlerMethodArgumentsResolvedEvent`
- `sources`：控制 `WebEndpointMappingFactory` 等组件的发现来源（与第 6 篇 `BeanSource` 一致）
- `requestContextStrategy`：控制 `RequestContextStrategy` 策略（`DEFAULT`/`INHERITABLE_THREAD_LOCAL`），影响 `RequestContextHolder` 的 ThreadLocal 类型

---

## 永恒原理

### 1. 统一元数据模型与抽象-具体分离

`WebEndpointMapping` 是"抽象"层，统一了 MVC/WebFlux/Servlet/Filter 四种端点的元数据表示。`HandlerMappingWebEndpointMappingFactory`（MVC/WebFlux 各一个）是"具体"层，将框架特定的端点对象转换为统一的 `WebEndpointMapping`。

这种分离的好处是：**消费者只需要理解 `WebEndpointMapping`，不需要知道 MVC 和 WebFlux 的差异**。一个 API 文档生成器可以遍历 `WebEndpointMappingRegistry`，对每个映射生成文档，无需区分它来自 MVC 还是 WebFlux。

### 2. 工厂链与路由

`SmartWebEndpointMappingFactory` 是工厂链的核心。它不是简单的工厂列表遍历，而是**按端点类型路由**：

```
RequestMappingInfo (MVC)  -> MVC Factory  -> WebEndpointMapping(WEB_MVC)
RequestMappingInfo (Flux) -> Flux Factory -> WebEndpointMapping(WEB_FLUX)
ServletRegistration       -> Servlet Factory -> WebEndpointMapping(SERVLET)
FilterRegistration        -> Filter Factory  -> WebEndpointMapping(FILTER)
```

路由通过 `Map<Class<?>, List<WebEndpointMappingFactory>>` 实现，以端点对象的 `Class` 为 key。这比线性遍历所有工厂更高效，且支持同类型多个工厂（按 Ordered 排序，第一个成功的返回）。

### 3. Composite Registry 与 SmartInitializingSingleton

`CompositeWebEndpointMappingRegistry` 在 `afterSingletonsInstantiated` 时自动发现所有 `WebEndpointMappingRegistry` Bean。这是 `SmartInitializingSingleton` 的典型用法--在所有单例 Bean 创建完成后执行，确保所有 Registry 都已就绪。

组合模式让多个 Registry 的数据可以被统一查询，同时保持各自的独立性。例如，MVC Registry 和 WebFlux Registry 可以分别管理自己的映射，Composite Registry 将两者合并供全局查询。

### 4. SmartLifecycle 的 Phase 顺序

`WebEndpointMappingRegistrar` 的 phase 设为 `WebEventPublisher.DEFAULT_PHASE - 10`，确保 Registrar 先于 Publisher 启动。这是 `SmartLifecycle` 的 phase 机制的正确用法--通过 phase 值控制启动顺序，而非依赖 `@DependsOn` 或 Bean 初始化顺序。

---

## 边界与反例

### 1. WebEndpointMapping 的 transient 字段与序列化

`endpoint`、`source`、`hashCode` 标记为 `transient`，意味着 `WebEndpointMapping` 的序列化会丢失这些字段。如果通过 RMI 或分布式缓存传递 `WebEndpointMapping`，接收方无法获取原始 `HandlerMethod` 对象。

**缓解**：`id` 字段（基于 `hashCode` 计算）不是 `transient`，可以用于跨节点标识。接收方可以通过 `id` 在本地 Registry 中查找完整的 `WebEndpointMapping`。

### 2. SmartWebEndpointMappingFactory 的泛型解析

`getSourceType()` 通过 `ResolvableType` 解析泛型参数。如果工厂子类是匿名类或泛型签名不完整，解析可能失败，返回 `Object.class`。这会导致该工厂出现在所有端点类型的候选列表中，降低路由效率。

### 3. WebRequestRule 的 NativeWebRequest 适配

`WebRequestRule.matches(NativeWebRequest)` 依赖 `NativeWebRequest`。MVC 中通过 `ServletWebRequest` 适配 `HttpServletRequest`，WebFlux 中需要自定义适配器。如果适配器不完整（如缺少某些 Header 的映射），匹配结果可能不正确。

### 4. CompositeWebEndpointMappingRegistry 的循环注册

`CompositeWebEndpointMappingRegistry` 在 `doRegister` 中将映射委托给所有子 Registry。如果某个子 Registry 又是 Composite（嵌套组合），可能导致同一映射被注册多次。`SimpleWebEndpointMappingRegistry` 通过 `id` 去重可以缓解，但其他 Registry 实现不一定去重。

### 5. WebEndpointMappingRegistrar 的触发时机

`WebEndpointMappingRegistrar` 是 `SmartLifecycle`，在 `ContextRefreshedEvent` 后启动。这意味着在 `ContextRefreshedEvent` 时 Registry 是空的--端点映射尚未注册。如果其他组件在 `ContextRefreshedEvent` 时尝试查询映射，会得到空结果。

**缓解**：监听 `WebEndpointMappingsReadyEvent` 而非 `ContextRefreshedEvent`，确保映射已注册。

---

## 现代 Spring（6.x）是否已支持？

| microsphere 特性 | Spring 6.x 是否有等价物 | 说明 |
|------------------|----------------------|------|
| `WebEndpointMapping` 统一元数据 | 无 | Spring 6.x 的 MVC 和 WebFlux 端点元数据仍不统一 |
| `WebEndpointMappingFactory` 工厂链 | 无 | Spring 6.x 没有端点转换工厂体系 |
| `WebEndpointMappingRegistry` | 无 | Spring 6.x 没有统一的端点注册表 |
| `WebRequestRule` 通用匹配 | 部分 | Spring 6.x 的 `RequestCondition` 体系类似，但分别绑定 MVC/WebFlux |
| `WebEndpointMappingsReadyEvent` | 无 | Spring 6.x 没有端点就绪事件 |
| `@EnableWebExtension` | 无 | Spring 6.x 没有统一 Web 扩展注解 |

Spring Boot 的 `WebMvcEndpointHandlerMapping` 和 `WebFluxEndpointHandlerMapping` 通过 Actuator 暴露端点信息，但这是管理端点（`/actuator/`），不是业务端点的统一元数据。

Spring 6.0 的 `RequestMappingHandlerMapping` 仍然分别处理 MVC 和 WebFlux，没有统一抽象。`HandlerMethod` 虽然类名相同，但分属不同包（`org.springframework.web.method` vs `org.springframework.web.reactive.method`），无法互操作。

---

## 小结

microsphere-spring 的 Web 端点元数据统一，通过 **`WebEndpointMapping` 统一模型 + 工厂链转换 + 注册表管理 + 事件通知** 四层设计，将 MVC、WebFlux、Servlet、Filter 四种端点的元数据统一为一种表示。

核心价值是**解耦端点的"定义"与"消费"**：框架（MVC/WebFlux）负责定义端点，`WebEndpointMappingFactory` 负责转换，`WebEndpointMappingRegistry` 负责存储，消费者（文档生成器、安全审计、监控）只依赖统一的 `WebEndpointMapping`，不需要知道端点来自哪个框架。

`WebRequestRule` 体系提供了独立于框架的请求匹配能力，`WebEventPublisher` 在端点就绪和方法执行时发布事件，进一步增强了 Web 层的可观察性。整个体系通过 `@EnableWebExtension` 一键启用，对应用透明。
