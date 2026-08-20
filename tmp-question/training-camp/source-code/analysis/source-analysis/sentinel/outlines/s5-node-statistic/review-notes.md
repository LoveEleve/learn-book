# S-5 审查记录

## Pass 1

- 识别出 S-5 主骨架：`NodeSelectorSlot` / `ClusterBuilderSlot` / `StatisticSlot` / `StatisticNode`
- 提前锁定 3 个高风险误区：
  1. 把 `EntryType` 误判成节点选择维度
  2. 把 `eagleeye` 误并入统计主链
  3. 把 `NodeBuilder` 误判成现役扩展点

## Pass 2 闭环收获

- `NodeSelectorSlot` 按 `context name` 分叉 `DefaultNode`，不是按 resource 分叉
- `ClusterBuilderSlot` 必须是原型，因为实例字段 `clusterNode` 承担“本链资源缓存”
- `StatisticSlot` 是统一记账口，entry 侧收口 pass/block/thread，exit 侧收口 rt/success/exception
- `DefaultNode` 通过覆写统计写方法，把局部统计自动双写到 `ClusterNode`
- `EntranceNode` / `DefaultNode` / `ClusterNode` 是三种统计坐标，不是重复建模
- `OccupiableBucketLeapArray` + `FutureBucketLeapArray` 是双数组借位模型，不是单数组 waiting 字段
- `StatisticSlotCallbackRegistry` 是正式织入点，S-6 热点参数统计依赖这里
- `NodeBuilder` 已退场，生产主链零消费者
- `eagleeye` 与节点/统计槽零直接依赖，属于并行日志面

## 时空溯源

- S-5 主骨架在 `0.1.0` 就已经完整存在
- 后续大改主要是：
  - `NodeBuilder` 退场，构建逻辑内联化
  - occupy 未来窗口机制引入
  - `LongAdder` 与滑窗索引算法修正

## 正文审查

### 上篇 01-node-tree

- 修正了 `NodeSelectorSlot` 与 `ClusterBuilderSlot` 的锚点行号
- 明确了“树内局部节点 vs 全局汇总节点”的双层模型
- 确认 `DefaultNode` 的双写代理作用不能漏写，否则中篇会误以为 `StatisticSlot` 直接写 `ClusterNode`

### 中篇 02-statistic-slot

- 修正了 `recordCompleteFor(...)` 和 exit callback 的行号
- 确认 `PriorityWaitException` 分支只加 thread、不加 pass 的解释必须和 future bucket 闭环
- 确认 `BlockException` 不进入完成记账，避免把 block 和 exception 混算

### 下篇 03-leap-array

- 校准了 `StatisticNode` 双窗口与 `OccupiableBucketLeapArray` / `FutureBucketLeapArray` 的锚点行号
- 强化了“future bucket 是未来借条簿，不是第二份普通滑窗”的表述
- 明确 `NodeBuilder` / `eagleeye` 只是旁枝，不进入主线

## 遗留

1. `LeapArray` 本体还没单独展开到算法级细节（索引/重置/过期判断），若后续正文需要更深算法篇，可追加 mini-harness
2. `eagleeye` 目前只做了边界确认，未做独立附篇
3. `StatisticNode.metrics()` 的分钟级拉取去重逻辑尚未单独展开
