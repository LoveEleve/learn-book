# S-8 适配器与扩展域 — 全视角提问

## 功能

1. WebMVC/WebFlux/SCG 的 entry/exit 生命周期分别在哪里？
2. Reactor 为什么必须用 operator/subscriber，而不是普通 filter？
3. AspectJ 的 blockHandler/fallback/Tracer 处理顺序是什么？
4. DataSource 如何把外部配置推送到 RuleManager？
5. JMX/Prometheus exporter 从哪里取指标？
6. Quarkus deployment/runtime 两阶段分别做什么？

## 性能

7. WebMVC reference count 为何能避免重复 dispatch 统计？
8. DataSource 的 refresh 频率和 converter 开销会在哪里体现？
9. JMX 每秒导出是否会影响主线程？

## 并发

10. Reactor subscriber 如何保证 entry/exit 配对？
11. WebMVC 异步请求如何清理 ThreadLocal context？
12. DataSource property 更新和 RuleManager 的 listener 是否线程安全？

## 扩展

13. 自定义 RequestOriginParser/UrlCleaner/BlockExceptionHandler 如何接入？
14. 自定义 DataSource 或 exporter 需要实现哪些接口？
15. `SentinelResourceAspect` 如何缓存 fallback/blockHandler 方法解析结果？

## 边界

16. WebFlux 空路径或 UrlCleaner 清空路径时怎么办？
17. BlockException 与业务异常在切面里如何分流？
18. Prometheus exporter 初始化失败时对主流程有影响吗？
19. No data source / no rule 时外围模块是否退化为空包装？

## 演进

20. `SentinelWebInterceptor` 在 `spring/webmvc/` 而不是 servlet 包，说明了什么边界？
21. Reactor adapter 从一开始就不是 Filter，这个设计是否与响应式模型天然匹配？
