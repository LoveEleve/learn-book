# SCC-3 服务发现抽象 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. DiscoveryClient 四方法面各服务谁? description 为什么"用于 HealthIndicator"?
2. Composite getInstances 短路与 getServices 合并的设计差异? 为什么不同策略?
3. SimpleDiscoveryClient 的 order 为什么可配? 组合场景怎么用?
4. @EnableDiscoveryClient 的 autoRegister 双分支各做什么?
5. probe 与 reactiveProbe 的演进原因?

## B. 源码实证 (5)

6. DEFAULT_ORDER 的值? 实现类怎么覆盖? (grep DiscoveryClient:37)
7. Composite 构造时用什么排序器? (grep L41)
8. ImportSelector 的 autoRegister=false 注入什么属性? (grep L56-61)
9. HeartbeatMonitor 怎么检测状态变更? (grep L29-35)
10. DiscoveryClientHealthIndicator 的 order 是什么? (grep L48)

## C. 推理深挖 (5)

11. 短路语义为什么"不检查健康"? 故障转移是负面的原因?
12. 为什么 getInstances 短路而 getServices 合并? 两个方法的语义差异?
13. autoRegister=false 时注入属性源 vs 直接不注册 — 为什么用属性源?
14. HeartbeatEvent 的 state "只要求变更时变化" — 为什么不用具体数据?
15. 为什么没有实例缓存? 实时查询的代价与收益?

## D. 跨域扩展 (5)

16. DiscoveryClient vs Nacos NamingService (5.8) 的接口设计对照?
17. Composite 短路 vs LoadBalancer 的实例选择 (SCC-6) 关系?
18. HeartbeatMonitor vs SCC-8 的 ready 守卫 (AtomicReference/AtomicBoolean)?
19. probe vs gRPC 健康检查协议?
20. 如果给 DiscoveryClient 加缓存, 应该在哪层? 为什么当前不做?
