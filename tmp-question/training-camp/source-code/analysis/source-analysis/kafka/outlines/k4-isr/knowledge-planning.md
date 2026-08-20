# K-4 Partition & ISR — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: core/src/main/scala/kafka/ (Partition 1987 行 + ReplicaManager 2951 行 + AbstractFetcherThread 954 行)
> [索引覆盖: scala 未在 codebase-memory 索引, 本域全部锚点 grep+Read 实证]

## 01 逐源提取 (每个源文件的机制)

| 源文件 | 机制点 |
|---|---|
| Partition.scala | ①leaderIsrUpdateLock 读写锁 (Partition.scala:L328) ②分区状态机 (partitionState) ③inSyncReplicaIds (Partition.scala:L412) ④makeLeader (Partition.scala:L733)/makeFollower (Partition.scala:L839) ⑤maybeExpandIsr/maybeShrinkIsr ⑥updateFollowerFetchState (Partition.scala:L909) ⑦HW 推进 ⑧LEO 跟踪 |
| ReplicaManager.scala | ①appendRecords 主路径 ②fetchMessages ③AlterPartitionManager (ISR 变更上报) ④getOrCreatePartition ⑤副本状态管理 (online/offline) |
| AbstractFetcherThread.scala | ①doWork 两阶段 (AbstractFetcherThread.scala:L115) ②maybeFetch (AbstractFetcherThread.scala:L120) ③maybeTruncate (AbstractFetcherThread.scala:L174) ④epoch 截断 4 规则 (maybeTruncateToEpochEndOffsets (AbstractFetcherThread.scala:L262)) ⑤processFetchRequest (AbstractFetcherThread.scala:L318) ⑥fenced leader epoch |

## 02 聚合 (跨文件去重, P1/P2/P3 分级)

| 聚合机制 | 来源文件 | 分级 | 说明 |
|---|---|---|---|
| ISR 动态维护 (expand/shrink) | Partition + ReplicaManager | P1 | 定义特征: ISR 模型核心 |
| 状态机 (makeLeader/makeFollower) | Partition | P1 | 副本角色转换 |
| 两阶段拉取 (truncate→fetch) | AbstractFetcherThread | P1 | 副本同步核心 |
| epoch 截断 4 规则 | AbstractFetcherThread | P1 | 一致性关键 |
| HW 推进 | Partition | P1 | 提交语义 |
| 读写锁 (leaderIsrUpdateLock) | Partition | P2 | 并发策略 |
| AlterPartitionManager 上报 | ReplicaManager | P2 | ISR 变更传播 (K-5 衔接) |
| replica.lag.time.max.ms 配置 | ReplicaManager/Partition | P2 | ISR 移除阈值 |
| FENCED_LEADER_EPOCH | AbstractFetcherThread | P2 | fencing 语义 |
| 延迟请求衔接 (DelayedProduce/DelayedFetch) | ReplicaManager | P3 | K-7 衔接 |

## 03 深度分类 (🔴/🟡/🟢)

- 🔴: ISR 模型 / 状态机 / 两阶段拉取 / epoch 截断 — 定义特征 (无 ISR 就不是分布式 Kafka)
- 🟡: HW 推进 / 读写锁 / AlterPartition 上报
- 🟢: 配置读取 (replica.lag.time.max.ms) — 提及即可

## 04 聚类 (机制边界 + 依赖图 + 教学顺序)

```
K-4 内部概念依赖:
ISR 集合定义 (inSyncReplicaIds) → maybeExpandIsr/maybeShrinkIsr (动态维护)
  → HW 推进 (提交语义 = 全 ISR 复制)
状态机 (makeLeader/makeFollower) → leaderIsrUpdateLock 保护
AbstractFetcherThread (副本拉取) → epoch 截断 4 规则 (回退安全) → processFetchRequest (K-3 衔接)
AlterPartitionManager → 上报 controller (K-5 衔接)
```

**依赖图**: K-3 (Log, appendAsFollower/read) → K-4 → K-12 (FetchSession) / K-7 (DelayedProduce 等 ISR ack) / K-5 (ISR 上报) / K-6 (消费 HW)
**教学顺序**: ①ISR 模型 (概念+设计权衡 vs 多数派) → ②Partition 状态机 (makeLeader/makeFollower) → ③副本拉取 (两阶段+epoch 截断) → ④HW 推进与提交语义 → ⑤配置与对照 (replica.lag.time.max.ms + 与 E-6/Raft 对照)

**拆篇建议**: 4 篇 (🔴 A 域, 8 闭环)
- 01: ISR 模型与设计权衡 (vs 多数派/PacificA) — Q1/Q2
- 02: Partition 状态机 + 读写锁 — Q3
- 03: 副本拉取: 两阶段 + epoch 截断 4 规则 — Q4/Q5/Q6
- 04: HW 推进/提交语义 + 配置与跨域对照 — Q7/Q8
