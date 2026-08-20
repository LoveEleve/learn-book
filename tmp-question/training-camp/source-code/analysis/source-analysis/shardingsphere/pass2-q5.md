# Pass 2 闭环笔记 SS-5: 读写分离域不能只看单个 Router

## 初始假设
- `ReadwriteSplittingDataSourceRouter` 就是读写分离域的主战场，足以代表全部机制。

## 验证过程
- `features/readwrite-splitting/core` 下的实际主线至少包括：
  - `ReadwriteSplittingDataSourceRouter`（抽象门面）
  - `StandardReadwriteSplittingDataSourceRouter`
  - `QualifiedReadwriteSplittingDataSourceRouter`
  - `ReadDataSourcesFilter` / `DisabledReadDataSourcesFilter`
  - `ReadwriteSplittingSQLRouter`
- 这说明“读写分离”至少有两层：
  1. 数据源路由器（standard/qualified）
  2. 读节点筛选过滤器（可用性/禁用/事务等条件）
- 如果只写一个 Router，会看不到真实决策是在“路由器 + filter”组合中完成的。

## 结论

SS-5 的准确标题应理解为“读写分离路由与读节点筛选”。主战场不是单类，而是 standard/qualified 两类 router 加上 read data source filter 家族。