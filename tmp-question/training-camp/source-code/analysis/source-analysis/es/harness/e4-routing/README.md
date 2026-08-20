# E-4 Cluster Routing — harness 验证记录 (MiniRouting 13/13)

> 跑法: `javac MiniRouting.java MiniRoutingTest.java && java MiniRoutingTest` (JDK 21)
> 结果: **13/13 PASS** (首跑全绿)

## 验证矩阵

| # | 机制 | 验证点 | 源码对照 | 结果 |
|:--:|---|---|---|:--:|
| A1-A4 | 路由表版本化 | version 递增 + 不可变副本 | RoutingTable.java:59-60 (withIncrementedVersion) | PASS |
| B1-B3 | 决策器投票 | 任一 NO 拒绝 → 恢复后放别处 | AllocationDecider.java:32-91 + SameShard/Disk decider | PASS |
| C1-C4 | 读写路由 | 写固定主 / 读 preference (_primary/_only_nodes) | OperationRouting.java:63-77,206-240 | PASS |
| C5-C6 | 版本判新旧 | 旧版本丢弃 + 应用新版本 | MasterService.java:508 (发布前递增) | PASS |

## 验证意义

- 路由表版本化 (不可变 + 递增) / 决策器投票聚合 / 读写路由不对称 / 版本判新旧 — 4 大机制全部可复现
- **未验证面**: BalancedShardsAllocator 权重算法、19 决策器全量、自适应副本 (响应时间)、GroupShardsIterator 分组
- 结论: "路由表 + 分配投票 + 读写路由" 的分布式路由理解验证到位
