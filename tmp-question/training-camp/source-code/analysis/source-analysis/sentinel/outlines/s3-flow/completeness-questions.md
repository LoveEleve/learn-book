# S-3 Flow 流控域 — 全视角提问

## 功能

1. `FlowSlot` 是入口壳还是算法实现？
2. 多条 `FlowRule` 是选一条、合并，还是全量遍历？
3. `limitApp`、`strategy`、`refResource` 如何共同决定判定节点？
4. `grade` 与 `controlBehavior` 如何组合？
5. Default/WarmUp/Throttling/WarmUpRateLimiter 四种 controller 的行为差异？
6. `PriorityWaitException` 为什么不属于 BlockException？

## 性能

7. `LeapArray` 如何避免每次请求都重建窗口？
8. `calculateTimeIdx`、CAS 建桶、短锁重置分别承担什么？
9. `values()` 如何过滤过期 bucket？
10. `FlowRuleManager` 的正则规则缓存如何降低查询成本？

## 并发

11. 多线程同时创建窗口时如何保证只发布一个 bucket？
12. ThrottlingController 的 `latestPassedTime` CAS 竞争失败如何处理？
13. 令牌桶 `TokenUpdateStatus` 如何保证参数值级别的原子更新？
14. occupy future bucket 如何避免 waiting/pass 双记？

## 扩展

15. `TrafficShapingController` 如何支持新增流控行为？
16. `FlowRuleManager.register2Property` 如何接入外部数据源？
17. cluster mode 如何通过 checker 分支接入而不新增 ClusterFlowSlot？

## 边界

18. count 为 0、acquireCount 非法、参数时间线超过 queue timeout 时如何处理？
19. 集群 token 服务失败时 fallbackToLocalWhenFail 如何生效？
20. `tokenbucket/` 旁枝是否真的在当前执行路径？

## 演进

21. occupy future bucket 何时引入？
22. WarmUpRateLimiterController 何时加入？
23. NodeBuilder/tokenbucket 等遗留代码为什么不能混入现行主线？
