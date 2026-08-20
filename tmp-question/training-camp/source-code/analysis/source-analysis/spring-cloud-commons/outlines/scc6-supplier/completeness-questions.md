# SCC-6 ServiceInstanceListSupplier 体系 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. Supplier\<Flux\<List\<ServiceInstance\>\>\> 的响应式惰性语义?
2. Delegating 基类为什么实现 SelectedInstanceCallback + InitializingBean + DisposableBean?
3. CacheFlux.lookup + onCacheMissResume 的缓存流程?
4. HealthCheckSupplier 的 refetchInstances 与 repeatHealthCheck 双周期差异?
5. Builder 的 with 方法怎么实现"逐层包装"?

## B. 源码实证 (5)

6. get(Request) default 与 get() 的关系? (grep Supplier:37-39)
7. selectedServiceInstance 怎么沿链传递? (grep Delegating:52-55)
8. SERVICE_DISCOVERY_TIMEOUT 默认值? (grep Discovery:51-55)
9. CacheFlux.lookup 的 cacheManager 从哪来? (grep Caching:55-57)
10. Builder 的 with(DelegateCreator) 自定义入口? (grep Builder:352)

## C. 推理深挖 (5)

11. 为什么 get() 返回 Flux 而非 List? 惰性获取的价值?
12. 缓存层包健康过滤 vs 健康过滤包缓存 — 顺序差异的语义?
13. timeout(30s) 在 Flux 链中的位置? 超时后发生什么?
14. selectedServiceInstance 回调的用途? (谁消费选中的实例?)
15. 为什么 CachingSupplier 用 Spring CacheManager 而非自建缓存?

## D. 跨域扩展 (5)

16. ServiceInstanceListSupplier vs Ribbon 的 ServerList 抽象?
17. CacheFlux vs SCC-2 的缓存语义 (响应式缓存 vs 同步缓存)?
18. 健康检查 Supplier vs SCC-8 的 HealthIndicator 健康面?
19. Builder 链 vs NamedContextFactory 的 registerBeans 装配 (SCC-13)?
20. 如果加"多注册中心聚合 Supplier", 应该怎么设计? (对照 SCC-3 Composite)
