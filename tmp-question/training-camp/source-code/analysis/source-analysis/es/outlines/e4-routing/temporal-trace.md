# E-4 Cluster Routing — 时空溯源 (v0.90 → v8.12.2)

> 方法: git show 早期 tag + git log commit 日期实证
> 断代锚点: v0.90.0 / v5.0.0-alpha1 / v8.12.2

## 演进主线 (3 代)

| 代 | 版本 | 结构 | 关键决策 |
|:--:|---|---|---|
| 1 | v0.90 | RoutingTable 427 行 + **MutableShardRouting (143) / ImmutableShardRouting (339) 分离** + allocation/ 模块 | **可变/不可变分离**: 分配期间可变, 发布后不可变 — 防御误改 |
| 2 | v5.0 | ShardRouting 合并 (c57951780e0, **2015-06-24** "Simplify ShardRouting abstraction") + OperationRouting/Murmur3HashFunction | **单类 ShardRouting**: 状态转换方法内聚 (moveToStarted/initialize); Murmur3 哈希路由 (id → 分片) |
| 3 | v8.12 | ShardRouting 992 行 + BalancedShardsAllocator 1452 行 + 19 决策器 + 自适应副本 | **成熟期**: 状态机方法全内聚 (ShardRouting.java:447-600) + 权重平衡 + 自适应副本选择 (L39) |

## 三个核心设计变迁

### 1. Mutable/Immutable 分离 → 单类

```
v0.90: MutableShardRouting (143 行) + ImmutableShardRouting (339 行) — 两类型防误改
v5.0:  单 ShardRouting (2015-06-24) — 状态转换方法内聚 (moveToStarted/initialize/reinitialize)
```
- 设计原因: 分离增加了复制成本 (转换方法), 单类 + 不可变字段语义 (final) 足够

### 2. 手写分配 → BalancedShardsAllocator (2013)

```
v0.90: 简单分配 (节点循环)
v5.0:  BalancedShardsAllocator (2eb09e6b1ab, 2013-01-17) — 权重函数平衡
```
- 设计原因: 多节点多索引需要"方差最小化"而非简单轮询 — shard/index 权重

### 3. 随机副本 → 自适应副本 (v5+)

```
早期: 读副本随机/轮询
v8.12: use_adaptive_replica_selection (OperationRouting.java:39) — 按响应时间/队列选副本
```
- 设计原因: 慢节点随机命中影响延迟; 自适应让读走快副本

## 对照 Redis Cluster

- Redis: CRC16(slot) 确定性哈希 (16384), 无路由表
- ES: 显式 RoutingTable + 动态分配
- 结论: 哈希定位 (Redis) vs 表定位 (ES) — 两种分布式坐标, 各有取舍

## REVIEW 修正记录 (2026-08-14)

- ✅ BalancedShardsAllocator 2013-01-17 / ShardRouting 合并 2015-06-24 日期实证
- ✅ v0.90 Mutable/Immutable 分离实证

## 完成检查

- [x] v0.90 Mutable/Immutable 分离已读
- [x] 合并/引入 commit 日期实证
- [x] 三核心变迁逐代对照
- [x] Redis Cluster 对照
