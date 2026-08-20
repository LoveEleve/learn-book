# Pass 2 闭环笔记 Q2: Reactor 为什么用 Transformer/Operator

## 验证过程

- `SentinelWebFluxFilter` 只做 URL/origin/context 配置，然后对下游 Publisher 调 `.transform(new SentinelReactorTransformer(...))` (`SentinelWebFluxFilter.java:42-57`)。
- `SentinelReactorTransformer.apply` 根据 Publisher 类型包装成 `MonoSentinelOperator` 或 `FluxSentinelOperator`；不支持的 Publisher 类型直接抛 `IllegalStateException` (`SentinelReactorTransformer.java:34-51`)。
- 这比在 Filter 内直接 `SphU.entry` 更合适：Reactor 的真正执行发生在订阅、onNext、onComplete、onError/cancel 等信号阶段，operator 能把 Entry 与 reactive 生命周期绑定。
- `EntryConfig` 携带资源名、resourceType、EntryType、ContextConfig，使同一 operator 机制能复用于 WebFlux 和其他 Reactor 调用。

## 结论

Reactor 适配器不用一个普通 Filter 包住同步调用，而是通过 Transformer 把 Publisher 替换成带 Sentinel 生命周期的 Mono/Flux operator；这是响应式执行、异步线程切换和取消语义下保持 entry/exit 配对的必要边界。