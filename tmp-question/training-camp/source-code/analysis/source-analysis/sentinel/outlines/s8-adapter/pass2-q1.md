# Pass 2 闭环笔记 Q1: WebMVC/WebFlux 的入口与 exit 时机

## 验证过程

- WebMVC `AbstractSentinelInterceptor.preHandle`：提取资源名 → 用 request attribute reference count 只处理初始 request → `ContextUtil.enter(contextName, origin)` → `SphU.entry(..., EntryType.IN)` → 把 Entry 放入 request attribute (`AbstractSentinelInterceptor.java:75-111`)。
- 被 block 时调用 block handler，随后 `ContextUtil.exit()`，不进入 controller (`AbstractSentinelInterceptor.java:112-124`)。
- 正常请求：
  - 普通同步请求在 `afterCompletion` exit
  - 异步请求在 `afterConcurrentHandlingStarted` exit 当前线程 context，异步完成后再由 afterCompletion 处理 (`AbstractSentinelInterceptor.java:128-153`)
- WebFlux 不在 filter 方法里直接 entry/exit，而是：
  - 取 URL 并经过 UrlCleaner
  - 构造 `EntryConfig`（资源名、COMMON_WEB、IN、context/origin）
  - 对 `chain.filter(exchange)` 应用 `SentinelReactorTransformer` (`SentinelWebFluxFilter.java:35-57`)
- Reactor operator 在订阅/终止信号时负责 Entry 生命周期，因此适配了异步/背压模型。

## 结论

WebMVC 依赖 Servlet interceptor 生命周期与 request attribute 保存 Entry；WebFlux 把 Entry 生命周期交给 Reactor operator，避免把异步请求硬绑在 filter 调用线程。两者最终都进入 SphU，但退出时机由外围框架生命周期决定。