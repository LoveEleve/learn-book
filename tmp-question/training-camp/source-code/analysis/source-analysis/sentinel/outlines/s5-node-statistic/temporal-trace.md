# S-5 节点与统计域 — 时空溯源

## 主线演进

- `0.1.0 (c92fea5d)`：`StatisticNode` / `DefaultNode` / `EntranceNode` / `ClusterNode` / `NodeSelectorSlot` / `ClusterBuilderSlot` / `StatisticSlot` / `LeapArray` / `ArrayMetric` 全部已具备，S-5 主骨架从第一版就基本成型。
- `468327bd`：移除全局 `NodeBuilder`，节点构建路径进一步内联进 `NodeSelectorSlot` / `ClusterBuilderSlot`。
- `be4d058b`：ProcessorSlot SPI 化，`NodeSelectorSlot` / `ClusterBuilderSlot` 从 legacy builder 体系切换到槽 SPI 体系。
- `044cdbb1 (#568)`：引入 occupy 机制，`OccupiableBucketLeapArray` / `FutureBucketLeapArray` 支持优先级借未来窗口。
- `3a9e2629 (#747)` + `fefd8c48`：`StatisticNode.curThreadNum` 从 AtomicInteger 系列演进为 JDK 原生 `LongAdder`。
- `0176f0ea (#723)`：`LeapArray` 取前窗逻辑改为 `calculateTimeIdx`，滑窗索引算法收敛。
- `74a40aa2 (#1115)`：Node/Metric 接口增强，支持条件查询分钟指标。
- `0e110c68 (#1196)`：修正 `StatisticNode.maxSuccessQps` 计算逻辑。
- `3755d534 (#2064)`：修正文档注释，不改语义。

## 关键判断

- S-5 不是“后期补出来的统计层”，而是 Sentinel 从 0.1.0 起就存在的核心地基。
- 后续演进主要集中在三类：
  1. 构建方式内联化(`NodeBuilder` 退场)
  2. 并发计数/滑窗算法修正(`LongAdder`、索引计算)
  3. 抢占式流控配套的未来窗口机制(`#568`)
