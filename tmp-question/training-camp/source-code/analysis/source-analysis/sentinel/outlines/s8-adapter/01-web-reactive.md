# 外围请求如何进入 Sentinel

> S-8 上篇。本文比较 WebMVC、WebFlux/Reactor 与其他适配器如何把外围请求接到 SphU，并保证 entry/exit 生命周期。

## 悬念

同样是一条 HTTP 请求，Servlet 是同步回调，WebFlux 是响应式信号，RPC 又是框架自己的 filter。Sentinel 怎么在不同生命周期里保持同一套 entry 语义？

## 一、WebMVC: request attribute + reference count

`AbstractSentinelInterceptor.preHandle` 的核心路径：

```text
提取 resourceName
  -> reference count +1
  -> 不是初始 request? 直接放行
  -> parseOrigin
  -> ContextUtil.enter(contextName, origin)
  -> SphU.entry(resourceName, COMMON_WEB, IN)
  -> Entry 放入 request attribute
```

reference count 用 request attribute 保存，处理 forward/include 等可能造成的重复 dispatch；只有计数从 0 变成 1 的初始 request 才进入 Sentinel (`AbstractSentinelInterceptor.java:75-111`)。

正常退出在 `afterCompletion`；如果 handler 启动异步处理，则 `afterConcurrentHandlingStarted` 先退出当前线程 context，避免 ThreadLocal 泄漏，最终请求完成再走 afterCompletion (`AbstractSentinelInterceptor.java:128-153`)。

Entry 放在 request attribute 里，而不是局部变量里，是因为 preHandle 和 afterCompletion 之间跨越了 Servlet 生命周期回调。

## 二、WebFlux: Filter 只组装配置

`SentinelWebFluxFilter` 不直接 entry/exit。它做三件事：

1. 从 exchange 取 path
2. 经过 UrlCleaner，空路径直接 `chain.filter(exchange)`
3. 构造 `EntryConfig`，对下游 Publisher 应用 `SentinelReactorTransformer` (`SentinelWebFluxFilter.java:35-57`)

```java
return chain.filter(exchange)
    .transform(buildSentinelTransformer(exchange, finalPath));
```

这里的 filter 只是把 Sentinel 变换附着到 reactive pipeline 上；真正的执行要等订阅发生。

## 三、Reactor: transformer → operator → subscriber

`SentinelReactorTransformer.apply` 根据 Publisher 类型包装：

```java
if (publisher instanceof Mono) {
    return new MonoSentinelOperator<>((Mono<T>) publisher, entryConfig);
}
if (publisher instanceof Flux) {
    return new FluxSentinelOperator<>((Flux<T>) publisher, entryConfig);
}
```

operator 的 `subscribe` 再创建 `SentinelReactorSubscriber`：

```java
source.subscribe(new SentinelReactorSubscriber<>(entryConfig, actual, true));
```

这层级是有原因的：entry 在 subscriber 的订阅阶段创建；exit 由终止信号驱动——Mono 等 unary 场景可能在 `onNext` 提前完成，其他情况由 complete/error/cancel 完成，并用 `AtomicBoolean` 防止重复 exit，而不是绑定到 filter 方法返回的瞬间。

所以 Reactor 适配器不是“一个异步版 Filter”，而是一层 publisher/operator/subscriber 包装。

## 四、WebMVC 与 WebFlux 的共同骨架

虽然生命周期完全不同，两者都遵循同一个抽象：

```text
外围框架生命周期
  -> 资源名 + origin + context
  -> SphU.entry / AsyncEntry
  -> 业务执行
  -> 对应生命周期的 exit
```

差别只是 Entry 的保存位置和 exit 触发点：

- WebMVC：request attribute，afterCompletion/async callback
- WebFlux：Reactor subscriber，terminal signal/cancel

## 五、RPC/HTTP 适配器的共通骨架

Dubbo、gRPC、Sofa RPC、Motan、OkHttp、Apache HttpClient 等适配器虽然 API 不同，但做的事情高度相似。例如 Dubbo provider filter 会对接口名和方法名各做一次 `SphU.entry`，业务异常时分别 `Tracer.traceEntry`，block 时转成 Dubbo 异常返回 (`SentinelDubboProviderFilter.java:68-84`)；OkHttp interceptor 会对请求资源名做一次 `SphU.entry`，block/业务异常分别走 fallback 与 `Tracer.traceEntry` (`SentinelOkHttpInterceptor.java:54-59`)。

抽象出来就是：

- 从框架请求对象提取资源名
- 提取 origin
- 调 `SphU.entry` 或异步 entry
- block 时转换成框架自己的异常/回调
- 正常结束时 exit
- 业务异常时调用 `Tracer`

因此适配器不是新的限流实现，而是外围生命周期到 S-2 core entry 模型的翻译层。

## 悬念回收

S-8 的关键不是“Sentinel 支持多少框架”，而是“每个框架如何把自己的生命周期翻译成同一种 entry/exit”：

- Servlet 用 request attribute 与回调
- WebFlux 用 transformer/operator/subscriber
- RPC/HTTP 用 filter/interceptor/template

外围形态不同，core 入口语义不变。

## 锚点

- `AbstractSentinelInterceptor.java:75-111`
- `AbstractSentinelInterceptor.java:128-153`
- `SentinelWebFluxFilter.java:35-57`
- `SentinelReactorTransformer.java:34-51`
- `MonoSentinelOperator.java:39-40`
- `SentinelDubboProviderFilter.java:68-84`
- `SentinelOkHttpInterceptor.java:54-59`
