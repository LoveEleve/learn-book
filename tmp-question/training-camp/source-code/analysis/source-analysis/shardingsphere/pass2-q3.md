# Pass 2 闭环笔记 SS-3: 归并引擎不是单一 GroupByStream

## 初始假设
- 归并引擎主要就是 `MergeEngine` + `GroupByStreamMergedResult`。

## 验证过程
- `infra/merge` 提供通用 `MergeEngine` 与 `MergedResult` 抽象，以及 `stream/memory/local/transparent/decorator` 等多种结果形态。
- `features/sharding/core` 在其上实现了多类归并：
  - `GroupByStreamMergedResult`
  - `GroupByMemoryMergedResult`
  - `OrderByStreamMergedResult`
  - 多种 `PaginationDecoratorMergedResultBuilder` 与分页 decorator merged result
  - SHOW/DDL 结果合并类
- 说明“归并”至少包含三类轴线：
  1. stream vs memory
  2. group-by / order-by / pagination
  3. sharding-specific show/ddl merged result

## 结论

SS-3 如果只写 `GroupByStreamMergedResult` 会把整个归并域错误压扁成单一路径。正确边界应是：`infra/merge` 抽象 + sharding feature 的 group/order/pagination/show 归并家族。