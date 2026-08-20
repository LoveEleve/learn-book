# E-6 SeqNo 复制协议 — 时空溯源 (v2.0 → v8.12.2)

> 方法: git show 早期 tag + git log commit 日期实证
> 断代锚点: v2.0.0 / v5.0.0-alpha1 / v6.0.0-alpha1 / v7.0.0-alpha1 / v8.12.2

## 演进主线 (4 代)

| 代 | 版本 | 结构 | 关键决策 |
|:--:|---|---|---|
| 1 | v2.0 | 无 seqNo 概念 (Engine.java grep seqNo = 0) | **版本号 version 单维** — 复制靠 version 冲突检测, 无逻辑位点 |
| 2 | v5.0 | 引入期: **Primary Terms (b364cf54bfd, 2015-10-21)** → **Sequence Numbers (5fb0f9a88ff, 2015-11-19)** → **Local checkpoints (3106948ae1d, 2015-12-15)** | **三个 commit 三个月内连发** — seqNo+primaryTerm+localCheckpoint 三位一体设计定型 |
| 3 | v6.0 | seqno/ 包出现: SequenceNumbersService + GlobalCheckpointTracker (6 文件) | **globalCheckpoint 独立跟踪器** — 跨副本水位聚合 |
| 4 | v7.0 | ReplicationTracker (取代 GlobalCheckpointTracker) + CountedBitSet (取代简单位图) | **合并进 ReplicationTracker + 租约引入 (501999c09d6 #37167)** — 保留历史供恢复 |

## 三个核心设计变迁

### 1. version → seqNo + primaryTerm (2015)

```
v2.0: 每文档 version 号 (乐观锁), 复制靠版本冲突 — 无法区分"操作顺序"与"操作新旧"
v5.0: seqNo (分片内单调位点) + primaryTerm (主世代号) — 三维版本: version 留给客户端, seqNo/term 留给复制
```
- 设计原因: 版本号冲突检测无法支撑**乱序复制/精确恢复** — 需要逻辑位点

### 2. Local checkpoint → GlobalCheckpointTracker → ReplicationTracker (2015-2017)

```
v5.0: LocalCheckpointTracker (本分片处理水位)
v6.0: + GlobalCheckpointTracker (跨副本聚合)
v7.0: 合并进 ReplicationTracker (ReplicationTracker.java:147 globalCheckpoint) + CheckpointState 逐副本状态
```
- 设计原因: 主分片需要同时跟踪"所有副本的 checkpoint"才能推进 globalCheckpoint

### 3. 无保留 → RetentionLease (2017)

```
v6.0: 无历史保留 — merge 直接清理 soft deletes
v7.0: RetentionLease (501999c09d6 #37167) — "谁需要保留 seqNo ≥ N 的历史"
```
- 设计原因: peer recovery/CCR 需要读取已 soft-delete 的历史 — 无租约时被 merge 吞掉

## 对照 Redis 复制演进

- Redis: 字节偏移位点 (replication.c:28 replicationSendAck) — 从未改变, 因为命令流复制天然适合
- ES: 逻辑位点 (seqNo) — 支持乱序/部分/精确恢复, 复杂度更高
- 结论: 位点设计 = 复制粒度 (命令流 vs 操作集) 的直接结果

## REVIEW 修正记录 (2026-08-14)

- ✅ 三个引入 commit 日期全部实证 (2015-10/11/12)
- ✅ v6.0 无 ReplicationTracker (只有 GlobalCheckpointTracker) — v7.0 才合并
- ✅ v2.0 无 seqNo (grep 实证)

## 完成检查

- [x] v2.0 无 seqNo 实证
- [x] 三个引入 commit 日期实证
- [x] v6.0→v7.0 GlobalCheckpointTracker→ReplicationTracker 演变
- [x] Redis 对照锚点 (replication.c:28)
