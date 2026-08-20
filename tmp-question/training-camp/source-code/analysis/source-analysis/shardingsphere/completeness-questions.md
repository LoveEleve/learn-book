# ShardingSphere 域规划 — 全视角提问

## 功能

1. 现版本分片主战场到底是不是 `ShardingStandardRoutingEngine`？
2. 分片路由需要覆盖哪些 route engine type 与 validator？
3. SQL 改写为什么必须分成 `infra/rewrite` 抽象层与 feature decorator？
4. 归并引擎除了 GroupByStream 还要包含哪些家族？
5. 分布式主键的现行内置实现到底有几个？
6. 读写分离主线是在 router、filter，还是 SQLRouter？
7. encrypt 与 mask 哪个应该进入主线，哪个只是边界补充？

## 性能

8. merge 的 stream 与 memory 路径分别适合什么场景？
9. 分页 merged result builder 为什么按数据库方言拆？
10. key generator SPI 为什么要独立于 sharding feature？

## 并发

11. 读写分离下 transaction/qualified router 如何保证一致性？
12. merge 阶段的 stream result 是否允许边读边合？
13. key generator 的雪花算法是否有时钟回拨风险处理？

## 扩展

14. 新增一个加密算法/主键算法需要经过哪些 SPI 路径？
15. 新增一个 rewrite decorator 或 merge result builder 怎么挂入框架？
16. readwrite-splitting 的 load balancer/filter 如何扩展？

## 边界

17. 执行计划里的 LEAF 是否只是历史/测试残留？
18. mask 是否应该单独成域，而不是混入 encrypt？
19. `ShardingRouter` 旧术语是否会误导后续写作？
20. SS-1/SS-2/SS-3 是否存在重复或漏项？
