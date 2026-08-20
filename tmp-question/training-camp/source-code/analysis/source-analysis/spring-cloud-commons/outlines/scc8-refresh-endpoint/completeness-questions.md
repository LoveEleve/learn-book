# SCC-8 RefreshEndpoint + 事件 — 完备性问题集 (20 问)

> 每问 4 态: 理解 (自答) / 验证 (grep 实证) / 深挖 (源码推理) / 扩展 (跨域对照)

## A. 机制理解 (5)

1. RefreshEndpoint 为什么是"薄门面"? 决策在哪一层?
2. RefreshEventListener 的 ready 标志防什么? 什么时候会发"未就绪"的 RefreshEvent?
3. RefreshEvent 和 RefreshEventListener 的关系? 谁 publish 谁 handle?
4. EnvironmentChangeEvent 的"同源不同发布者"是什么意思? 两个发布者各在哪条路径?
5. RefreshScopeHealthIndicator 聚合哪两处错误? 为什么用聚合而非单一?

## B. 源码实证 (5)

6. RefreshEndpoint 的 @Endpoint id 和 @WriteOperation 方法签名? (grep RefreshEndpoint:33-44)
7. supportsEventType 监听哪两个事件类型? (grep RefreshEventListener:50-53)
8. EnvironmentManager 的 MANAGER_PROPERTY_SOURCE 是什么? (grep L45)
9. HealthIndicator 的装配条件是什么? (grep RefreshEndpointAutoConfiguration:61-62)
10. RefreshEndpointAutoConfiguration 的 Endpoint 四条件? (grep L71-73)

## C. 推理深挖 (5)

11. 为什么 RefreshEvent 在应用就绪前会被忽略? 设计动机?
12. EnvironmentManager 的 reset() 发布 EnvironmentChangeEvent 与 ContextRefresher 发布的有何差异?
13. RefreshScopeHealthIndicator down 时 withDetail 给谁看? 运维怎么用?
14. 多实例部署时 /actuator/refresh 只刷当前实例 — 怎么广播? (负面空间对照 Spring Cloud Bus)
15. WritableEnvironmentEndpoint 写属性后走 EnvironmentChangeEvent 吗? 验证 publish 链?

## D. 跨域扩展 (5)

16. RefreshEndpoint vs Spring Cloud Bus 的 refresh 广播机制对照?
17. RefreshEventListener 的 ready 守卫 vs SCC-1 的 bootstrap 就绪时序?
18. HealthIndicator 聚合模式 vs Spring Boot 其他健康指示器?
19. RefreshEvent 程序化触发 vs Nacos 配置中心的监听回调?
20. 如果给 RefreshEndpoint 加鉴权, 应该在哪层? (Actuator 安全 vs 端点内)
