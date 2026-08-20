# Pass 2 闭环笔记 SS-1: 分片路由主战场的真实位置

## 初始假设
- 执行计划里的 `ShardingRouter` 就是当前版本的核心入口。

## 验证过程
- 在主源码中未找到名为 `ShardingRouter` 的现行核心类；真正直接承担“标准分片路由”的类是 `features/sharding/core/.../ShardingStandardRoutingEngine.java`。
- 其周围还有一整组 route engine/validator 族：`route/engine/type/standard/*`、`route/engine/validator/*`，说明现行分片主线不是单类，而是“routing engine + validator”协作。
- 这意味着执行计划中的旧术语已经漂移，若继续沿用会让域名与源码主战场错位。

## 结论

SS-1 的标题应从“分片引擎”进一步精确为**分片路由引擎**，主战场以 `ShardingStandardRoutingEngine` 及其 validator/engine 家族为准，不能再沿用 `ShardingRouter` 这个过时术语。