# E-5 Shard 生命周期 — 时空溯源 (v0.90 → v8.12.2)

> 方法: git show 早期 tag + git log commit 日期实证
> 断代锚点: v0.90.0 / v5.0.0-alpha1 / v8.12.2

## 演进主线 (3 代)

| 代 | 版本 | 结构 | 关键决策 |
|:--:|---|---|---|
| 1 | v0.90 | `shard/service/InternalIndexShard` (876 行) + IndexShard 接口 (133 行) — 状态 5 态: CREATED/RECOVERING/STARTED/RELOCATED/CLOSED (IndexShardState.java:28-32) | **接口/实现分离** (IndexShard + InternalIndexShard); 主降副本已有警告 (L225) — 状态机雏形 |
| 2 | v5.0 | IndexShard 合并单类 (1562 行), + POST_RECOVERY 态 (IndexShardState.java:29) | **6 态** (CREATED/RECOVERING/POST_RECOVERY/STARTED/RELOCATED/CLOSED); 恢复阶段细分 (POST_RECOVERY 隔离"恢复完成但未激活") |
| 3 | v8.12 | IndexShard 4242 行 — 5 态 (RELOCATED 移除) + ReplicationGroup + IndexShardOperationPermits + GlobalCheckpointSyncer | **RELOCATED 移除 (848c7e4917c, 2018-03-28, #29246)** — 合并进 STARTED (兼容 IDS[4]=STARTED); 组件化膨胀: Engine/ReplicationTracker/permits 全部内聚 |

## 三个核心设计变迁

### 1. 接口/实现分离 → 单类膨胀

```
v0.90: IndexShard (接口 133 行) + InternalIndexShard (实现 876 行)
v8.12: IndexShard 单类 4242 行 (接口方法全内联)
```
- 设计原因: 早期模块化 (Guice 注入), 后期组件 (Engine/ReplicationTracker/Store) 独立成类, 门面收敛回单类

### 2. 5 态 → 6 态 → 5 态

```
v0.90: CREATED/RECOVERING/STARTED/RELOCATED/CLOSED (5 态)
v5.0:  + POST_RECOVERY (6 态) — 恢复完成后等待激活的中间态
v8.12: - RELOCATED (5 态) — 2018-03-28 移除, 合并进 STARTED
```
- 设计原因: POST_RECOVERY 隔离"本地恢复完成"与"集群确认 active"; RELOCATED 是路由层概念 (ShardRouting 状态), 不该在分片状态重复

### 3. 状态驱动源 — 本地 vs 集群

```
v0.90: recovering()/started() 等方法直接迁移 (内部驱动)
v8.12: updateShardState (IndexShard.java:493) 集群状态驱动 + recoverFromStore/postRecovery 本地驱动双轨
```
- 设计原因: 分布式分片状态必须与 master 的 ShardRouting 对齐 — 集群状态是权威, 本地恢复只是中间态

## 对照 Redis 哨兵

- Redis: 哨兵独立进程检测 + 选举 — 状态机在哨兵侧 (r14-sentinel)
- ES: 状态机在分片自身 (IndexShard) + master 驱动 — 无独立监督进程
- 结论: ES 的"自我状态 + 集群权威"比 Redis 哨兵"外部监督"更内聚, 但复杂度更高

## REVIEW 修正记录 (2026-08-14)

- ✅ RELOCATED 移除 2018-03-28 (848c7e4917c) 日期实证
- ✅ v0.90 InternalIndexShard 876 行 + 接口分离实证
- ✅ v5.0 POST_RECOVERY 引入实证

## 完成检查

- [x] v0.90 InternalIndexShard 结构已读
- [x] v5.0 6 态实证 / v8.12 RELOCATED 移除日期实证
- [x] 三核心变迁逐代对照
- [x] Redis 哨兵对照
