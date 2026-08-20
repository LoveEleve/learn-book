# S-8 适配器与扩展域 — 大纲

## 上篇: 外围请求如何进入 Sentinel — 01-web-reactive.md

1. WebMVC interceptor 的 request/reference count 生命周期
2. WebFlux filter 与 Reactor transformer
3. Mono/Flux subscriber 如何绑定 entry/exit
4. RPC/HTTP 适配器的共通骨架

## 中篇: 注解、异常与配置推送 — 02-aspect-datasource.md

1. `SentinelResourceAspect` annotation → SphU
2. blockHandler/fallback/exception trace 顺序
3. `AbstractDataSource` converter/property
4. 配置中心 → RuleManager 的推送链

## 下篇: 指标导出与扩展边界 — 03-exporter-boundary.md

1. JMXMetricExporter 定时收集
2. Prometheus InitFunc + HTTPServer
3. Quarkus deployment/runtime 两阶段
4. 适配器都是外围包装，不改变 core 语义
