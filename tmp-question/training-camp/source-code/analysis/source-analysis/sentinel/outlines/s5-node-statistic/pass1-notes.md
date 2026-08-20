# S-5 节点与统计域 — Pass 1 轮廓记录

> 日期: 2026-08-17 | 范围: node/ + slots/nodeselector + slots/clusterbuilder + slots/statistic + eagleeye/

## 入口展开

- 核心四件套: `NodeSelectorSlot`(181) / `ClusterBuilderSlot`(165) / `StatisticSlot`(166) / `StatisticNode`(337)
- 节点层级: `EntranceNode`(127) → `DefaultNode`(170) → `ClusterNode`(126)；`Node` 接口 204 行
- 滑窗层: `LeapArray` / `WindowWrap` / `MetricBucket` / `ArrayMetric` / `BucketLeapArray` / `OccupiableBucketLeapArray` / `FutureBucketLeapArray`
- 扩展织入点: `StatisticSlotCallbackRegistry`
- 辅助日志面: `eagleeye/` 15 文件,与 flow/tokenbucket 同名不同包

## Pass 1 观察

- `ClusterBuilderSlot` 维护 `clusterNodeMap`，属于静态 volatile + COW + 双检锁风格(待 Pass 2 精确落锚)
- `NodeSelectorSlot` 负责按 context 名把资源节点挂到调用树上；S-2 已证 context 名是入口树 key，待本域补树结构细节
- `StatisticSlot` 是真正的统计入口: pass / block / rt / exception / threadNum 都在这里收口
- `StatisticSlotCallbackRegistry` 是 S-6 热点参数统计的织入点(执行计划已实证)
- `eagleeye/TokenBucket` 与 flow/tokenbucket 无关，需严格区分

## 测试地图

- `node/StatisticNodeTest.java`
- `node/ClusterNodeTest.java`
- `slots/clusterbuilder/ClusterNodeBuilderTest.java`
- `slots/nodeselector/NodeSelectorTest.java`
- `node/metric/MetricNodeTest.java`
- `eagleeye/EagleEyeCoreUtilsTest.java`

## 标记问题

1. `NodeSelectorSlot` 如何把同一 resource 在不同 context 下拆成不同 `DefaultNode`?
2. `ClusterBuilderSlot` 为什么必须是原型槽? `clusterNodeMap` 的共享与 `DefaultNode.clusterNode` 的绑定关系是什么?
3. `StatisticSlot` 的 pass/block/rt/exception/threadNum 具体各在哪几行更新?
4. `StatisticNode` 与 `ArrayMetric`/`LeapArray` 的分层关系是什么? 哪层负责窗口推进?
5. `EntranceNode`/`DefaultNode`/`ClusterNode` 三者分别代表什么统计视角?
6. `OccupiableBucketLeapArray` 与 `FutureBucketLeapArray` 如何支持抢占式流控?
7. `StatisticSlotCallbackRegistry` 的 entry/exit callback 时序是什么?
8. `eagleeye` 与 `StatisticSlot` 的关系是并行日志面,还是直接消费统计节点?
9. `NodeBuilder` 在哪里被消费? 与 `NodeSelectorSlot` 的边界怎么划?
