# ShardingSphere 域规划 — Pass 1 轮廓记录

> 日期: 2026-08-18 | 阶段 6.6 | 范围: `features/sharding` + `infra/rewrite` + `infra/merge` + `infra/algorithm/keygen` + `features/readwrite-splitting` + `features/encrypt`
> 冲突审计: analysis 目录此前不存在，未发现既有规划/HANDOFF/进行中产物

## 执行计划候选 6 域

- SS-1 分片引擎：`ShardingStandardRoutingEngine`
- SS-2 SQL 改写：`SQLRewriteContextDecorator` / `ShardingSQLRewriteContextDecorator`
- SS-3 归并引擎：`MergeEngine` / `GroupByStreamMergedResult`
- SS-4 分布式主键：`KeyGenerateAlgorithm` + Snowflake/UUID
- SS-5 读写分离：`ReadwriteSplittingDataSourceRouter`
- SS-6 数据脱敏/加密：`EncryptAlgorithm` + `EncryptSQLRewriteContextDecorator`

## Pass 1 观察

- ShardingSphere 不是单模块，而是 feature/infra/kernel/proxy 多层架构；本阶段计划只落到“JDBC/runtime 主链相关”的 6 域。
- SS-1/SS-2/SS-3 天然构成 SQL 生命周期主线：路由 → 改写 → 执行后归并。
- SS-4/SS-5/SS-6 更像功能插件域：主键生成、读写分离、数据加密，它们各自通过 SPI/Decorator 挂入主链。
- `ShardingRouter` 在执行计划中是旧名字，当前主战场应落到 `ShardingStandardRoutingEngine`；需要警惕计划术语与现源码漂移。
- `EncryptAlgorithm` 不是单纯 AES/MD5，而是 feature-encrypt 的 SPI 入口；“数据脱敏”在当前源码更准确地说应是加密/查询辅助加密能力。

## 标记问题

1. 执行计划中的 `ShardingRouter` 是否已被现版本类名替换？
2. 分片引擎的边界是在 feature/sharding/core 还是需要连到 infra/route？
3. SQL 改写是否应包含通用 `infra/rewrite` 抽象与 feature-specific decorator 两层？
4. 归并引擎除了 `MergeEngine` 和 `GroupByStreamMergedResult`，是否还需要排序/分页等 merged result 家族？
5. 分布式主键计划写“Snowflake/UUID/LEAF 三种”，但现源码里 LEAF 是否真的存在？
6. 读写分离当前核心是否就是 `ReadwriteSplittingDataSourceRouter`，还是还应吸收 load balancer 与 qualified/standard 路由器？
7. “数据脱敏”这个词是否会误导为 mask/encrypt 两个 feature，当前计划该聚焦哪一个？
8. 6 域是否漏掉 shadow/broadcast/mask 等功能域，还是它们应明确排除？

## 初步拓扑判断

建议拓扑：

```text
SS-1 分片路由主线 -> SS-2 SQL改写 -> SS-3 归并引擎
SS-4 主键生成 (独立算法插件)
SS-5 读写分离 (独立路由插件)
SS-6 加密/脱敏改写 (独立 decorator/algorithm 插件)
```

即：SQL 主线三域 + 三个并列插件域。下一步要用源码验证这个拓扑是否足够精确。

## 深度规划审查结论

- 执行计划中的 `ShardingRouter` 已不是现版本主战场，现行核心应落到 `ShardingStandardRoutingEngine` 与其 validator/engine 族。
- SS-2 不能只写单个 decorator，必须显式包含 `infra/rewrite` 通用抽象 + feature-specific `ShardingSQLRewriteContextDecorator`。
- SS-3 若只写 `MergeEngine + GroupByStreamMergedResult` 会偏窄；至少还要覆盖 order-by / pagination / memory-vs-stream merged result 家族，否则“归并引擎”只剩一条分支。
- SS-4 当前源码主线可证的是 `Snowflake` 与 `UUID`；执行计划里的 `LEAF` 在主战场目录未见实现，只见 mode tuple fixture 残留，不能写成现行核心算法。
- SS-5 不能只看单个 `ReadwriteSplittingDataSourceRouter`，还要显式纳入 qualified/standard 子路由与 `ReadDataSourcesFilter`。
- SS-6 计划名称应谨慎：当前源码里 encrypt 与 mask 是两个 feature。若维持 6 域上限，本阶段更适合聚焦 `encrypt` 主线，把 `mask` 标成旁枝边界，而不是笼统写“数据脱敏”。