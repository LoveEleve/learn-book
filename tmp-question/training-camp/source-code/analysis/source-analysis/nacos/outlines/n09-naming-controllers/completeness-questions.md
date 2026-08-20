# N-09 命名控制器面 — 完备性问题集 (20 问)

## A. 机制理解 (5)
1. 控制器为什么零业务逻辑? Operator 抽象的边界?
2. v2/v3 分代共存的兼容策略?
3. trace 事件为什么在入口发布?
4. 六控制器各自的职责边界?
5. 控制器与 Operator 抽象的接口面?

## B. 源码实证 (6)
6. InstanceController 的 registerInstance 转发? (grep L127)
7. 六控制器的清单? (grep controllers/)
8. v2/v3 文件数? (grep 目录)
9. ServiceController 的服务创建语义?
10. CatalogController 的目录面?
11. ClusterController 与集群的关系?

## C. 推理深挖 (5)
12. HealthController 与健康检查的衔接?
13. OperatorController 的运维面?
14. batchUpdateMetadata 的批量语义?
15. v2/v3 同功能怎么路由 (URL 前缀)?
16. HealthController 与 N-11 健康检查的关系?

## D. 跨域扩展 (4)
17. v2/v3 请求的 URL 前缀路由?
18. 批量元数据操作与实例操作的事务性?
19. trace 事件的异步发布与丢失风险?
20. 控制器层是否做参数校验?

