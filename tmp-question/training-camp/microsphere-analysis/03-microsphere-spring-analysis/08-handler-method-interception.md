# 03-08 HandlerMethod 细粒度拦截与 MVC/WebFlux 扩展

## 问题：Spring 的 HandlerInterceptor 粒度不够

Spring MVC 的 `HandlerInterceptor` 提供三个回调：`preHandle`、`postHandle`、`afterCompletion`。这些回调的粒度是"请求级"--在请求进入 Handler 前后触发，但无法观察：

| 场景 | HandlerInterceptor 能力 | 问题 |
|------|----------------------|------|
| 参数解析前后 | 无 | 无法在 `@RequestParam` 解析时插入逻辑 |
| 方法返回值处理 | 无 | 无法在返回值序列化前后插入逻辑 |
| HandlerMethod 执行前后 | `preHandle`/`postHandle` | 有，但参数未解析，拿不到方法参数值 |
| 异步请求 | 需 `AsyncHandlerInterceptor` | 额外接口，不统一 |

WebFlux 的情况更差：没有 `HandlerInterceptor` 等价物，只有 `WebFilter`（请求级），无法拦截 `HandlerMethod` 执行。

microsphere-spring 的 `web/method/support` 包通过 **三级拦截体系 + MVC/WebFlux 双适配** 填补这些空白。

---

## 设计：两级拦截 + 双框架适配

### 整体架构

```
@EnableWebExtension(interceptHandlerMethods = true)
    │
    ├── HandlerMethodAdvice (统一接口)
    │       ├── beforeResolveArgument / afterResolveArgument  ← 参数级
    │       ├── beforeExecuteMethod                            ← 方法级 before
    │       └── afterExecuteMethod(returnValue, error)         ← 方法级 after（含返回值和异常）
    │
    ├── DelegatingHandlerMethodAdvice (复合，ContextRefreshedEvent 时初始化)
    │       ├── HandlerMethodArgumentInterceptor 列表
    │       └── HandlerMethodInterceptor 列表
    │
    ├── MVC: InterceptingHandlerMethodProcessor
    │       implements HandlerMethodArgumentResolver    ← 拦截参数解析
    │       implements HandlerMethodReturnValueHandler  ← 拦截返回值
    │       implements HandlerInterceptor               ← 拦截方法执行
    │       implements WebMvcConfigurer                 ← 注册自身
    │
    └── WebFlux: InterceptingHandlerMethodProcessor
            implements WebFilter                        ← 拦截请求
            implements HandlerAdapter                   ← 拦截方法执行
            implements HandlerMethodArgumentResolver    ← 拦截参数解析
            implements HandlerResultHandler             ← 拦截返回值
            implements WebExceptionHandler              ← 异常处理
```

---

### 三级拦截接口

> 注：这里"三级"指参数级 before、参数级 after、方法级 before/after（含返回值），实际是两个维度的拦截。

#### 方法级：HandlerMethodInterceptor

```java
public interface HandlerMethodInterceptor {
    default void beforeExecute(HandlerMethod handlerMethod, Object[] args,
                                NativeWebRequest request) throws Exception { }
    default void afterExecute(HandlerMethod handlerMethod, Object[] args,
                               Object returnValue, Throwable error,
                               NativeWebRequest request) throws Exception { }
}
```

与 Spring 的 `HandlerInterceptor.preHandle(request, response, handler)` 相比，`HandlerMethodInterceptor` 直接接收 `HandlerMethod` 和**已解析的参数 `args`**。Spring 的 `preHandle` 只能拿到 `Object handler`（可能是 `HandlerMethod` 也可能是其他），且参数尚未解析。

#### 参数级：HandlerMethodArgumentInterceptor

```java
public interface HandlerMethodArgumentInterceptor {
    default void beforeResolveArgument(MethodParameter parameter,
                                        HandlerMethod handlerMethod,
                                        NativeWebRequest webRequest) { }
    default void afterResolveArgument(MethodParameter parameter,
                                       Object resolvedArgument,
                                       HandlerMethod handlerMethod,
                                       NativeWebRequest webRequest) { }
}
```

在 `HandlerMethodArgumentResolver.resolveArgument` 前后触发。`MethodParameter` 标识当前解析的参数（包含参数索引、类型、注解），`resolvedArgument` 是解析后的值。

#### 统一接口：HandlerMethodAdvice

`HandlerMethodAdvice` 合并了方法级和参数级拦截：

```java
public interface HandlerMethodAdvice {
    default void beforeResolveArgument(...) { }
    default void afterResolveArgument(...) { }
    default void beforeExecuteMethod(HandlerMethod handlerMethod, Object[] args,
                                      NativeWebRequest request) { }
    default void afterExecuteMethod(HandlerMethod handlerMethod, Object[] args,
                                     @Nullable Object returnValue, @Nullable Throwable error,
                                     NativeWebRequest request) { }
}
```

用户可以实现 `HandlerMethodAdvice` 一次覆盖所有拦截点，也可以单独实现 `HandlerMethodInterceptor` 或 `HandlerMethodArgumentInterceptor`。`afterExecuteMethod` 同时接收返回值和异常，无需单独的"返回值级"回调。

#### DelegatingHandlerMethodAdvice：复合委托

`DelegatingHandlerMethodAdvice` 是 `HandlerMethodAdvice` 的复合实现，在 `ContextRefreshedEvent` 时从 BeanFactory 加载所有 `HandlerMethodInterceptor` 和 `HandlerMethodArgumentInterceptor` Bean：

```java
public class DelegatingHandlerMethodAdvice extends OnceApplicationContextEventListener<ContextRefreshedEvent>
        implements HandlerMethodAdvice {

    protected void onApplicationContextEvent(ContextRefreshedEvent event) {
        this.argumentInterceptors = getSortedBeans(context, HandlerMethodArgumentInterceptor.class);
        this.methodInterceptors = getSortedBeans(context, HandlerMethodInterceptor.class);
    }

    public void beforeExecuteMethod(HandlerMethod handlerMethod, Object[] args, NativeWebRequest request) {
        for (HandlerMethodInterceptor interceptor : methodInterceptors) {
            interceptor.beforeExecute(handlerMethod, args, request);
        }
    }
}
```

使用 `OnceApplicationContextEventListener` 确保只在当前 ApplicationContext 的事件中初始化（防止父子容器重复初始化）。Bean 列表通过 `getSortedBeans` 按 `Ordered` 排序。

---

### MVC 适配：InterceptingHandlerMethodProcessor

MVC 版本的 `InterceptingHandlerMethodProcessor` 通过实现 **四个接口** 将拦截器编织进 MVC 请求处理链：

```
HTTP 请求进入 DispatcherServlet
    │
    ├── HandlerInterceptor.preHandle (自身实现)
    │       └── 遍历 HandlerMethodAdvice.beforeExecuteMethod()
    │
    ├── HandlerMethodArgumentResolver.resolveArgument (自身实现)
    │       ├── 遍历 HandlerMethodAdvice.beforeResolveArgument()
    │       ├── 委托原始 ArgumentResolver.resolveArgument()
    │       └── 遍历 HandlerMethodAdvice.afterResolveArgument()
    │
    ├── HandlerMethod 执行 (由 HandlerAdapter 调用)
    │
    ├── HandlerMethodReturnValueHandler.handleReturnValue (自身实现)
    │       ├── 遍历 HandlerMethodAdvice.beforeHandleReturnValue()
    │       ├── 委托原始 ReturnValueHandler.handleReturnValue()
    │       └── 遍历 HandlerMethodAdvice.afterHandleReturnValue()
    │
    └── HandlerInterceptor.afterCompletion (自身实现)
            └── 遍历 HandlerMethodAdvice.afterExecuteMethod()
```

**关键设计：装饰原始 Resolver/Handler**

`InterceptingHandlerMethodProcessor` 在 `WebEndpointMappingsReadyEvent` 时（所有映射已注册）获取 `RequestMappingHandlerAdapter` 的 `HandlerMethodArgumentResolverComposite` 和 `HandlerMethodReturnValueHandlerComposite`，将自身**插入到 composite 的最前面**。自身在 `resolveArgument`/`handleReturnValue` 中先执行拦截器回调，再委托给原始 Resolver/Handler。

这样，所有 `@RequestMapping` 方法的参数解析和返回值处理都会经过拦截器，无需用户修改任何 Controller 代码。

**缓存优化**：`MethodParameterContext` 和 `ReturnTypeContext` 缓存了 `MethodParameter` 到原始 Resolver/Handler 的映射，避免每次请求都遍历 Composite 查找。

---

### WebFlux 适配：InterceptingHandlerMethodProcessor

WebFlux 版本实现 **五个接口**，适配响应式模型：

```
HTTP 请求进入 DispatcherHandler
    │
    ├── WebFilter.filter (自身实现)
    │       ├── RequestContextWebFilter: 绑定 RequestAttributes 到 ThreadLocal
    │       └── chain.filter(exchange) -> 继续
    │
    ├── HandlerAdapter.handle (自身实现)
    │       ├── 遍历 HandlerMethodAdvice.beforeExecuteMethod()
    │       ├── 委托原始 HandlerAdapter.handle()
    │       └── 遍历 HandlerMethodAdvice.afterExecuteMethod()
    │
    ├── HandlerMethodArgumentResolver.resolveArgument (自身实现)
    │       ├── 遍历 HandlerMethodAdvice.beforeResolveArgument()
    │       ├── 委托原始 ArgumentResolver.resolveArgument() (返回 Mono)
    │       └── Mono.doOnSuccess -> 遍历 HandlerMethodAdvice.afterResolveArgument()
    │
    ├── HandlerResultHandler.handleResult (自身实现)
    │       └── 拦截返回值处理
    │
    └── WebExceptionHandler.handle (自身实现)
            └── 异常处理
```

**WebFlux 的响应式适配**：`HandlerMethodArgumentResolver.resolveArgument` 返回 `Mono<Object>`，拦截器回调在 `Mono.doOnSuccess` 中执行。`HandlerAdapter.handle` 返回 `Mono<HandlerResult>`，`afterExecuteMethod` 在 `Mono.doOnSuccess`/`doOnError` 中执行。

**RequestContextWebFilter**：WebFlux 没有 `RequestContextHolder` 的等价物（因为响应式线程模型不保证线程绑定）。`RequestContextWebFilter` 通过 `setRequestAttributes` 将 `ServerWebRequest` 绑定到 ThreadLocal，使得 MVC 的 `RequestContextHolder` API 在 WebFlux 中也能使用。`doOnTerminate` 时清理 ThreadLocal，防止线程池复用导致的泄漏。

---

### MVC 扩展组件

#### AnnotatedMethodHandlerInterceptor：注解驱动拦截器

```java
public abstract class AnnotatedMethodHandlerInterceptor<A extends Annotation>
        extends MethodHandlerInterceptor {

    private final Class<A> annotationType;  // 泛型解析

    @Override
    protected final boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                                      HandlerMethod handlerMethod) {
        A annotation = getMethodAnnotation(request, handlerMethod);
        if (annotation != null) {
            return preHandle(request, response, handlerMethod, annotation);
        }
        return true;  // 无注解则放行
    }
}
```

只有标注了指定注解的 HandlerMethod 才触发拦截。子类只需指定注解类型（通过泛型参数）并覆盖 `preHandle(request, response, handlerMethod, annotation)`。这与第 4 篇的 `AnnotatedInjectionPointDependencyResolver<A>` 模式一致--泛型驱动的注解类型解析。

#### LazyCompositeHandlerInterceptor：延迟初始化

`LazyCompositeHandlerInterceptor` 在 `ContextRefreshedEvent` 时才从 BeanFactory 获取拦截器列表。这解决了"拦截器 Bean 尚未创建"的时序问题--`HandlerInterceptor` 在 `DispatcherServlet` 初始化时注册，但某些拦截器 Bean 可能在更晚的阶段创建。

#### ReversedProxyHandlerMapping：反向代理

`ReversedProxyHandlerMapping` 监听 `WebEndpointMappingsReadyEvent`，缓存所有 `WebEndpointMapping`（Kind=WEB_MVC）。当请求到达时，通过请求头 `microsphere_wem_id` 查找对应的 `WebEndpointMapping`，返回其 `endpoint`（`HandlerMethod`）。

这实现了**跨节点反向代理**：请求可以被路由到任意节点，节点通过 `WebEndpointMapping.id` 查找本地 Handler，无需重新解析 URL。

使用 `MethodHandle` 调用 `AbstractHandlerMapping#getHandlerExecutionChain`（protected 方法），避免反射开销。

#### ExclusiveViewResolverApplicationListener：排他 ViewResolver

当配置了 `microsphere.spring.webmvc.view-resolver.exclusive-bean-name=myViewResolver` 时，在 `ContextRefreshedEvent` 时移除所有其他 `ViewResolver` Bean，只保留指定的。这解决了多 ViewResolver 冲突问题（如 Thymeleaf 和 JSP 同时存在时的优先级冲突）。

#### ConfigurableContentNegotiationManagerWebMvcConfigurer：可配置内容协商

通过 `microsphere.spring.webmvc.content-negotiation.*` 属性配置 `ContentNegotiationManager`：

```properties
microsphere.spring.webmvc.content-negotiation.favor-parameter=true
microsphere.spring.webmvc.content-negotiation.parameter-name=format
microsphere.spring.webmvc.content-negotiation.media-types.json=application/json
```

实现方式：反射修改 `ContentNegotiationConfigurer` 内部的 `ContentNegotiationManagerFactoryBean`，用 `DataBinder` 绑定属性。这是第 5 篇 `DefaultConfigurationBeanBinder` 的同类技术。

#### ContentCachingFilter：内容缓存

`ContentCachingFilter` 继承 `OncePerRequestFilter`，使用 `ContentCachingResponseWrapper` 包装响应，将输出内容缓存到请求属性中。这使得后续的 `@EventListener` 或 `HandlerInterceptor` 可以读取响应体（Spring 默认不支持读取已写入的响应体）。

#### RequestBodyAdviceAdapter / ResponseBodyAdviceAdapter

Spring MVC 的 `RequestBodyAdvice` 和 `ResponseBodyAdvice` 接口有四个方法，但大多数实现只需要覆盖一个。Adapter 提供默认空实现，子类只需覆盖关心的方法。这是适配器模式的标准应用。

---

### WebFlux 扩展组件

#### CompositeWebFilter

`CompositeWebFilter` 持有 `List<WebFilter>`，在 `filter` 方法中构建链式调用。与 Spring 的 `DefaultWebFilterChain` 不同，microsphere 的 `CompositeWebFilter` 支持动态添加过滤器（`addFilter`），且防止自引用和重复添加。

#### DelegatingWebFilter

`DelegatingWebFilter` 在 `ContextRefreshedEvent` 时自动发现所有 `WebFilter` Bean（排除自身），委托给内部的 `CompositeWebFilter`。这解决了 WebFlux 中 `WebFilter` 注册顺序不可控的问题--所有过滤器被统一管理，按 `Ordered` 排序。

#### RequestContextWebFilter

如前所述，`RequestContextWebFilter` 将 `ServerWebExchange` 适配为 `NativeWebRequest`，绑定到 `RequestContextHolder`。这使得 MVC 的 `RequestContextHolder.getRequestAttributes()` API 在 WebFlux 中也可用，方便共享 MVC/WebFlux 代码。

`threadContextInheritable` 属性控制是否使用 `InheritableThreadLocal`（默认 false，与 Spring 的 `RequestContextFilter` 一致）。

---

### RequestContextStrategy

```java
public enum RequestContextStrategy {
    DEFAULT,                    // RequestContextHolder 默认行为
    THREAD_LOCAL,               // 显式 ThreadLocal
    INHERITABLE_THREAD_LOCAL    // InheritableThreadLocal（子线程可继承）
}
```

`@EnableWebExtension(requestContextStrategy = INHERITABLE_THREAD_LOCAL)` 控制请求属性的 ThreadLocal 类型。`INHERITABLE_THREAD_LOCAL` 在异步场景中有用（子线程可以访问父线程的请求属性），但有线程池泄漏风险（第 2 篇的 `ThreadLocal` 内存泄漏问题）。

---

## 永恒原理

### 1. 拦截粒度的层次化

microsphere 的两级拦截（参数级 + 方法级）对应了 `HandlerMethod` 的生命周期阶段：

```
请求到达 -> 参数解析 -> 方法执行 -> 请求结束
              │            │
              │            ├── beforeExecuteMethod (方法级 before)
              │            └── afterExecuteMethod (方法级 after，含 returnValue + error)
              ├── beforeResolveArgument (参数级 before)
              └── afterResolveArgument (参数级 after)
```

每一级解决不同的横切关注点：
- **参数级**：参数审计（记录每个参数的值）、参数修改（修改解析后的值）、参数验证（自定义校验逻辑）
- **方法级**：方法耗时统计、权限检查、日志记录。`afterExecuteMethod` 同时携带返回值和异常，可以用于返回值审计（如加密敏感字段）、异常监控（如记录未捕获异常）

MVC 的 `InterceptingHandlerMethodProcessor` 额外实现了 `HandlerMethodReturnValueHandler`，在返回值序列化阶段插入拦截，但这一层级通过方法级的 `afterExecuteMethod` 暴露给用户，不单独定义"返回值级"回调。

Spring 的 `HandlerInterceptor` 只有方法级（且参数未解析），无法满足参数级的需求。

### 2. 装饰原始组件 vs 替换原始组件

`InterceptingHandlerMethodProcessor` 的 MVC 版本采用**装饰策略**：不替换 `HandlerMethodArgumentResolverComposite`，而是将自身插入到 Composite 的最前面。当自身被调用时，先执行拦截器回调，再委托给原始 Resolver。

这种策略的好处是**不破坏 Spring 的默认行为**：如果不启用 `@EnableWebExtension`，Spring 的原始 Resolver 链不受影响。启用后，拦截器"附加"在原始链上，而非替换。

WebFlux 版本也采用类似策略：`HandlerAdapter` 和 `HandlerMethodArgumentResolver` 都是在 `WebEndpointMappingsReadyEvent` 时装饰原始组件。

### 3. 响应式拦截的 Mono 回调适配

WebFlux 的 `HandlerMethodArgumentResolver.resolveArgument` 返回 `Mono<Object>`，不能像 MVC 那样在方法返回后立即执行 `afterResolveArgument`。microsphere 使用 `Mono.doOnSuccess` 在 Mono 完成后触发回调：

```java
public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
                                     ServerWebExchange exchange) {
    advice.beforeResolveArgument(parameter, handlerMethod, webRequest);
    return delegate.resolveArgument(parameter, bindingContext, exchange)
            .doOnSuccess(arg -> advice.afterResolveArgument(parameter, arg, handlerMethod, webRequest));
}
```

`doOnSuccess` 在 Mono 成功完成时执行，不影响 Mono 的值传递。如果 Mono 报错，`doOnSuccess` 不执行--`afterResolveArgument` 只在成功解析时触发。

### 4. WebEndpointMappingsReadyEvent 作为初始化时机

MVC 和 WebFlux 的 `InterceptingHandlerMethodProcessor` 都监听 `WebEndpointMappingsReadyEvent`（第 7 篇定义），在这个事件中获取 `RequestMappingHandlerAdapter` 并装饰其 Resolver/Handler。

选择这个时机的原因：`WebEndpointMappingsReadyEvent` 在 `SmartLifecycle.doStart` 中发布，此时所有 Bean 已创建完毕，`RequestMappingHandlerAdapter` 已完全初始化，可以安全地修改其内部结构。

---

## 边界与反例

### 1. InterceptingHandlerMethodProcessor 的初始化时序

`InterceptingHandlerMethodProcessor` 监听 `WebEndpointMappingsReadyEvent`，该事件在 `WebEventPublisher` 的 `SmartLifecycle.doStart` 中发布。如果 `WebEventPublisher` 未启动（`publishEvents=false`），`InterceptingHandlerMethodProcessor` 不会初始化，拦截器不会生效。

**缓解**：`@EnableWebExtension(interceptHandlerMethods = true)` 默认启用 `publishEvents`，两者一起生效。但如果用户显式设置 `publishEvents=false`，拦截器会静默失效。

### 2. WebFlux RequestContextWebFilter 的 ThreadLocal 泄漏

`RequestContextWebFilter` 在 `filter` 中设置 ThreadLocal，在 `doOnTerminate` 中清理。如果 `Mono` 链被取消（`cancel`）且 `doOnTerminate` 未触发（在某些 Reactor 版本中可能发生），ThreadLocal 不会被清理。

**缓解**：使用 `doFinally` 而非 `doOnTerminate` 可以覆盖更多终止场景（包括 cancel）。但 microsphere 使用 `doOnTerminate`，可能在极端场景下泄漏。

### 3. ReversedProxyHandlerMapping 的 MethodHandle 兼容性

`ReversedProxyHandlerMapping` 使用 `MethodHandle` 调用 `AbstractHandlerMapping#getHandlerExecutionChain`（protected 方法）。如果 Spring 重构此类或方法签名，`MethodHandle` 查找会失败，`getHandlerExecutionChainMethodHandle` 为 null，反向代理功能失效。

### 4. ConfigurableContentNegotiationManagerWebMvcConfigurer 的反射修改

通过反射修改 `ContentNegotiationConfigurer` 内部的 `ContentNegotiationManagerFactoryBean`。如果 Spring 修改了 `ContentNegotiationConfigurer` 的字段名或类型，反射会失败。

### 5. AnnotatedMethodHandlerInterceptor 的泛型解析

与第 4 篇 `GenericBeanPostProcessorAdapter` 和第 6 篇 `AnnotatedBeanCapableImportCandidate` 一样，`AnnotatedMethodHandlerInterceptor` 通过 `ResolvableType` 解析泛型参数 `A`。匿名子类可能导致解析失败。

---

## 现代 Spring（6.x）是否已支持？

| microsphere 特性 | Spring 6.x 是否有等价物 | 说明 |
|------------------|----------------------|------|
| `HandlerMethodInterceptor`（方法级+参数） | 无 | Spring 6.x 的 `HandlerInterceptor` 仍只有请求级 |
| `HandlerMethodArgumentInterceptor` | 无 | Spring 6.x 的参数解析不可拦截 |
| `HandlerMethodAdvice` 统一接口 | 无 | Spring 6.x 没有统一拦截接口 |
| WebFlux HandlerMethod 拦截 | 无 | Spring 6.x 的 WebFlux 仍无 HandlerInterceptor 等价物 |
| `ReversedProxyHandlerMapping` | 无 | Spring 6.x 无反向代理 HandlerMapping |
| `ExclusiveViewResolverApplicationListener` | 无 | Spring 6.x 无排他 ViewResolver 机制 |
| `ConfigurableContentNegotiationManagerWebMvcConfigurer` | 部分 | Spring Boot 的 `WebMvcProperties` 可配置部分内容协商参数 |
| `ContentCachingFilter` | 部分 | Spring 的 `ContentCachingResponseWrapper` 存在，但无现成 Filter |
| `CompositeWebFilter` (WebFlux) | 无 | Spring 6.x 的 WebFlux 无复合 WebFilter |
| `RequestContextWebFilter` (WebFlux) | 无 | Spring 6.x 的 WebFlux 无 RequestContextHolder 适配 |
| `RequestContextStrategy` | 无 | Spring 6.x 无可插拔请求上下文策略 |

Spring 6.0 的 `HandlerInterceptor` 没有新增参数级或返回值级回调。Spring Boot 3.0 的 `@ControllerAdvice` + `ResponseBodyAdvice` 可以拦截返回值，但不拦截参数解析。

---

## 小结

microsphere-spring 的 HandlerMethod 细粒度拦截，通过 **三级拦截（参数级/方法级/返回值级）+ MVC/WebFlux 双适配** 将 Spring 的请求级拦截扩展到方法内部。

核心设计是 `InterceptingHandlerMethodProcessor`：它实现多个 Spring 接口（MVC 4 个、WebFlux 5 个），将自身编织进 MVC/WebFlux 的请求处理链，在参数解析、方法执行、返回值处理三个阶段触发拦截器回调。拦截器通过 `DelegatingHandlerMethodAdvice` 统一管理，在 `ContextRefreshedEvent` 时自动发现并排序。

MVC 扩展组件（`AnnotatedMethodHandlerInterceptor`、`ReversedProxyHandlerMapping`、`ExclusiveViewResolverApplicationListener`、`ConfigurableContentNegotiationManagerWebMvcConfigurer`、`ContentCachingFilter`）和 WebFlux 扩展组件（`CompositeWebFilter`、`DelegatingWebFilter`、`RequestContextWebFilter`）进一步填补了两个框架的实用功能空白。

整个体系通过 `@EnableWebExtension(interceptHandlerMethods = true)` 一键启用，对 Controller 代码完全透明。
