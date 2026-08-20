# N-22 远程服务端面 — 完备性问题集 (20 问)


## A. 机制理解 (5)

1. SDK/集群双服务器的隔离理由?
2. RequestHandlerRegistry 的收集时机 (ContextRefreshedEvent)?
3. 连接注册与限制规则的语义?
4. 连接注册的并发控制?
5. 心跳事件的频率与驱动?

## B. 源码实证 (6)

6. 双服务器类名? (grep GrpcSdkServer/GrpcClusterServer)
7. Registry 的查询? (grep L57)
8. Acceptor 的分发? (grep L114-116)
9. 连接注册? (grep ConnectionManager:102)
10. 连接限制规则? (grep L80-81)
11. 事件族? (grep event/)

## C. 推理深挖 (5)

12. 未知请求类型 (handler null) 的兜底响应?
13. 双向流与单向流的差异?
14. 连接数超限的拒绝语义?
15. 心跳事件驱动的健康判定?
16. 集群服务器与 SDK 服务器的端口策略?

## D. 跨域扩展 (4)

17. 本域 vs NC-3 客户端内核: 两端闭合?
18. 注册表 vs N-12 推送 SPI: 注册模式对照?
19. 连接管理 vs openjdk 的 socket 层: 连接面规划对照?
20. 本域与 N-21 集群代理的关系?
