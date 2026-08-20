# N-11 健康检查 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. HealthCheckTaskV2 的"自续调度"闭环怎么工作? 为什么不自毁?
2. 四处理器 (Tcp/Http/Mysql/None) 的选择时机与依据?
3. 拦截器链的 Enable/Responsible 拦截器各管什么?
4. 心跳三维检查器 (正常/不健康/过期) 的分工?
5. 检查结果怎么同步到集群? (临时/持久分型)

## B. 源码实证 (6)

6. doHealthCheck 的完整逻辑? (grep HealthCheckTaskV2:110-128)
7. Delegate 的 type 注册 Map? (grep HealthCheckProcessorV2Delegate:49-55)
8. TcpHealthCheckProcessor 的规模? (grep 421 行)
9. 四拦截器的类名? (grep interceptor/)
10. ClientBeatProcessorV2 的职责? (grep heartbeat/)
11. HealthStatusSynchronizer 的双实现? (grep v2/)

## C. 推理深挖 (5)

12. 心跳超时多久判定不健康? 判定链路的字段 (lastBeatTime)?
13. Responsible 拦截器在集群多节点的判定逻辑 (谁负责检查)?
14. NoneHealthCheckProcessor 在什么配置下启用?
15. 过期实例清理 (ExpiredInstanceChecker) 与 IP 删除超时的关系?
16. 检查失败重试的退避策略?

## D. 跨域扩展 (4)

17. 本域 vs 执行计划 5.8 NC-5 (TcpSuperSenseProcessor): 3.x 重构对照?
18. 心跳任务 vs ALI-A5 NacosDiscoveryHeartBeatPublisher: 两端心跳语义?
19. 处理器族 vs NC-5 Distro 组件注册: 两种注册表模式?
20. 本域 vs openjdk 的 safepoint/signal: 健康检查的规划粒度对照?
