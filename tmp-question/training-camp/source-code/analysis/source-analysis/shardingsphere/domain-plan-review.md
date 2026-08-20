# ShardingSphere 域规划深审记录

## 审查范围
- 执行计划 `issue/源码分析执行计划.md:633-642`
- 实际源码：`features/sharding` / `infra/rewrite` / `infra/merge` / `infra/algorithm/keygen` / `features/readwrite-splitting` / `features/encrypt` / `features/mask`
- 当前阶段：只做域规划审查，不写正文

## 发现与修正

### 1. SS-1 术语漂移
- 执行计划写 `ShardingRouter`
- 现源码主战场是 `ShardingStandardRoutingEngine`
- 还伴随 validator/route engine 家族
- 结论：SS-1 应命名为“分片路由引擎”，不能沿用旧类名

### 2. SS-2 需要双层边界
- 仅写 `ShardingSQLRewriteContextDecorator` 不够
- 必须同时纳入 `infra/rewrite` 的 `SQLRewriteContextDecorator` 抽象
- 结论：SS-2 是“通用 rewrite 框架 + sharding feature decorator”两层

### 3. SS-3 计划过窄
- 归并不只 `MergeEngine + GroupByStreamMergedResult`
- 现源码还有 `GroupByMemoryMergedResult`、`OrderByStreamMergedResult`、分页 decorator merged result 族
- 结论：SS-3 应按“流式/内存/分页/SHOW 结果”家族写，不能只盯 GroupByStream

### 4. SS-4 计划中的 LEAF 不成立
- 主战场目录实测只有 `SnowflakeKeyGenerateAlgorithm` 与 `UUIDKeyGenerateAlgorithm`
- `LEAF` 仅出现在 `mode/api` 测试 fixture 命名中，不是当前主战场算法实现
- 结论：SS-4 先收敛为 `Snowflake + UUID + SPI`，不把 LEAF 写成现行核心

### 5. SS-5 需要展开子路由
- `ReadwriteSplittingDataSourceRouter` 只是接口/门面
- 现源码还包括：`StandardReadwriteSplittingDataSourceRouter`、`QualifiedReadwriteSplittingDataSourceRouter`、`ReadDataSourcesFilter`
- 结论：SS-5 应写成“读写分离路由与读节点筛选”，不能只写一个 Router 类名

### 6. SS-6 名称需谨慎
- 当前源码里 `encrypt` 与 `mask` 是两个 feature
- 若受 6 域上限限制，应优先聚焦 `encrypt` 主线（SPI + rewrite + merge）
- `mask` 作为旁枝/边界补充，而不是把两个 feature 简化成一句“数据脱敏”

## 审查结论
- 6 域数量可暂时保留
- 但 SS-1/2/3/4/5/6 的标题与边界都必须按本记录修订后再进入 Pass 2
- 当前已达到“可继续做闭环，不可直接写正文”的状态
