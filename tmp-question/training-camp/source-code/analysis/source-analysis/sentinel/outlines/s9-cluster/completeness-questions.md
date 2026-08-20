# S-9 集群限流域 — 全视角提问

## 功能

1. `ClusterStateManager` 如何管理 client/server 模式切换？
2. 集群 token 服务支持哪三类请求？
3. `TokenResultStatus` 各状态的含义？
4. server 侧如何做集群流控判定？
5. `TokenClientProvider` / `EmbeddedClusterTokenServerProvider` 如何 SPI 加载？
6. envoy-rls 如何复用集群判定？

## 性能

7. server 侧全局阈值计算的开销？
8. `GlobalRequestLimiter` 的作用？
9. Netty 传输的异步请求-响应如何关联？

## 并发

10. 模式切换与 token 请求是否互斥？
11. server 侧 metric 的并发更新如何保证？
12. 并发流控 `requestConcurrentToken` 的占用/释放如何配对？

## 扩展

13. 新增一种集群 token 服务实现需要动哪些点？
14. 集群阈值类型(GLOBAL/AVG_LOCAL)如何扩展？

## 边界

15. client 连不上 server 时如何回退？
16. 集群无规则/引用资源不可用时返回什么状态？
17. `fallbackToLocalWhenFail` 的回退策略？
18. 全局请求限流触发时返回什么状态？

## 演进

19. 集群限流从 1.4.0 引入，其与本地流控的关系？
20. `ClusterFlowChecker` 与本地 `FlowRuleChecker` 是否共用阈值语义？
21. envoy-rls 的 gRPC 集成与原生 Netty 集群的关系？
