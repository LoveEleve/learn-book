# S-8 适配器与扩展域 — Pass 1 轮廓记录

> 日期: 2026-08-17 | 范围: sentinel-adapter 适配器模块 + datasource/annotation/metric/quarkus 扩展

## 模块分组

- Web/API: Spring WebMVC、WebFlux、Spring Cloud Gateway、Zuul
- RPC: Dubbo/Dubbo3、gRPC、Sofa RPC、Motan、JAX-RS
- HTTP: OkHttp、Apache HttpClient
- Reactive: Reactor（`SentinelReactorTransformer` 族，不是 Filter）
- Annotation: `SentinelResourceAspect`
- DataSource: `AbstractDataSource` + Nacos/Apollo/Consul/Etcd/Eureka/Redis/ZK/Spring Cloud Config
- Metrics: `JMXMetricExporter`、Prometheus `PromExporterInit`
- Quarkus: deployment processor + native image recorder

## Pass 1 观察

- 适配器主线大多是“外围生命周期 → SphU/SphO/AsyncEntry → Block handler/fallback → exit”。
- 不同框架差异主要在：资源名提取、origin 提取、上下文传播、异常/响应转换。
- datasource 主线不是限流执行，而是 `ReadableDataSource → Converter → SentinelProperty` 的规则推送。
- annotation-aspectj 主线是方法切面包装资源、处理 blockHandler/fallback。
- metric exporter 主线是从 MetricReader/MetricNode 拉取统计，转换成 JMX/Prometheus exposition。

## 标记问题

1. WebMVC/WebFlux/SCG 适配器的入口与 exit 时机分别是什么？
2. Reactor 为什么用 Transformer/Operator 而不是 Filter？上下文如何跨线程传播？
3. RPC provider/consumer 的资源名和 origin 如何提取？
4. fallback 与 blockHandler 的优先级/异常传播是什么？
5. DataSource 如何把外部配置推送到 RuleManager？
6. `AbstractDataSource` 的线程模型与资源关闭如何处理？
7. `SentinelResourceAspect` 如何解析 annotation 属性并执行 fallback？
8. JMX/Prometheus exporter 如何读取 MetricNode？
9. Quarkus deployment/runtime 两阶段分别做什么？
10. 适配器是否全部依赖 core，哪些模块只提供转换而不直接 entry？
