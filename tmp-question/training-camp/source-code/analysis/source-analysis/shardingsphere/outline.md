# ShardingSphere 域规划 — 大纲

## SS-1 分片路由引擎
- `ShardingStandardRoutingEngine`
- 其他 route engine type：broadcast/complex/ignore/unicast
- validator 家族与 SQLRoute/SubqueryRoute 测试面

## SS-2 SQL 改写
- `infra/rewrite` 的 `SQLRewriteContextDecorator`
- `ShardingSQLRewriteContextDecorator`
- rewrite token / parameter / condition 的 feature-specific 扩展点

## SS-3 归并引擎
- `infra/merge`：`MergeEngine` / `MergedResult`
- sharding feature：group-by stream/memory、order-by、pagination、show/ddl merged result
- memory vs stream vs decorator 三种结果形态

## SS-4 分布式主键
- `KeyGenerateAlgorithm` SPI
- `SnowflakeKeyGenerateAlgorithm`
- `UUIDKeyGenerateAlgorithm`
- 计划中的 LEAF 作为审计纠正，不进入主战场

## SS-5 读写分离
- `ReadwriteSplittingDataSourceRouter` 抽象
- standard / qualified 路由器
- `ReadDataSourcesFilter` / `DisabledReadDataSourcesFilter`
- `ReadwriteSplittingSQLRouter`

## SS-6 加密与查询辅助改写
- `EncryptAlgorithm` SPI
- `EncryptSQLRewriteContextDecorator`
- encrypt merge/result decorator / metadata reviser / rule builder
- `mask` 作为旁枝边界，不并入主线
