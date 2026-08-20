# K-4 Partition & ISR — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: K-3 ✅ (Log 存储) | 对照: [[E-6-seqno]] (ES 复制协议) [[r9-replication]] (Redis 主从) [[rd2-rlock]] (fencing)
> 源码: core/src/main/scala/kafka/cluster/Partition.scala (1987 行) + server/ReplicaManager.scala (2951 行) + server/AbstractFetcherThread.scala (954 行)
> [索引覆盖: scala 未在 codebase-memory 索引, 本域锚点全 grep+Read 实证]
> 测试地图: core/src/test/scala/unit/kafka/cluster/ (PartitionTest/PartitionLockTest) + server/ (AbstractFetcherThreadTest/AlterPartitionManagerTest)

## 继承树/调用图

```
ReplicaManager (ReplicaManager.scala) — 副本管理中枢
├── getOrCreatePartition → Partition (Partition.scala:308)
├── appendRecords (KafkaApis.handleProduceRequest → 主写路径, K-3 衔接)
├── fetchMessages (读路径, K-12 衔接)
├── AlterPartitionManager (ISR 变更 → 上报 controller, K-5 衔接)
└── ReplicaFetcherManager → AbstractFetcherThread (AbstractFetcherThread.scala:58)
      ├── doWork (AbstractFetcherThread.scala:L115): maybeTruncate (AbstractFetcherThread.scala:L174) → maybeFetch (AbstractFetcherThread.scala:L120)
      │     └── maybeTruncateToEpochEndOffsets (AbstractFetcherThread.scala:L262) — epoch 截断 4 规则
      └── processFetchRequest (AbstractFetcherThread.scala:L318) → processPartitionData → Log.appendAsFollower (K-3)

Partition (Partition.scala:308)
├── leaderIsrUpdateLock (Partition.scala:328, ReentrantReadWriteLock) — 读写锁
├── partitionState (CommittedPartitionState/OngoingReassignmentState — K-5 衔接)
├── makeLeader (Partition.scala:L733) / makeFollower (Partition.scala:L839) — 角色转换
├── maybeExpandIsr / maybeShrinkIsr — ISR 动态维护
├── updateFollowerFetchState (Partition.scala:L909) — 副本进度跟踪
└── HW 推进 (提交语义 = 全 ISR 复制, 设计文档 §Replication)
```

## 基本元素分解 (原则二)

1. **ISR 集合** — inSyncReplicaIds (Partition.scala:412): 与 leader 同步的副本集合, 提交语义的判定对象
2. **分区状态机** — makeLeader (Partition.scala:L733) / makeFollower (Partition.scala:L839): 角色转换 + 日志截断
3. **读写锁** — leaderIsrUpdateLock (Partition.scala:L328): 状态变更写锁/读取读锁
4. **两阶段拉取** — AbstractFetcherThread.doWork (AbstractFetcherThread.scala:L115): maybeTruncate → maybeFetch — 先保证截断正确再拉新数据
5. **epoch 截断** — maybeTruncateToEpochEndOffsets (AbstractFetcherThread.scala:L262): 按 leader epoch 回退到安全点
6. **HW 推进** — 全 ISR 复制后推进 (设计文档: committed = all ISR received)
7. **ISR 变更上报** — AlterPartitionManager: ISR 扩缩 → 持久化到元数据 (K-5)
8. **FENCED_LEADER_EPOCH** — epoch 过期 → 分区被 fence (fencing token 语义)

## 标记问题 (≥5)

1. **Q1: ISR 模型 vs 多数派** — 为什么 Kafka 不选 majority vote? (设计文档 §Replicated Logs: ISR 动态集合 + f+1 容错)
2. **Q2: maybeExpandIsr/maybeShrinkIsr 触发条件** — 副本滞后多久出 ISR? 回来怎么重进? (replica.lag.time.max.ms)
3. **Q3: makeLeader/makeFollower 状态机** — 角色转换做什么? 日志怎么截断? (Partition.scala:733,839)
4. **Q4: 两阶段拉取** — doWork 为什么先 truncate 再 fetch? (AbstractFetcherThread.scala:115)
5. **Q5: epoch 截断 4 规则** — 具体哪四条? 怎么保证回退安全? (maybeTruncateToEpochEndOffsets AbstractFetcherThread.scala:L262)
6. **Q6: FENCED_LEADER_EPOCH** — 什么时候返回? 语义? (对照 rd2-rlock fencing)
7. **Q7: HW 推进** — 什么条件下 HW 前进? 与 ISR 关系? (提交语义)
8. **Q8: 与 K-3/K-5 衔接** — appendAsFollower 写入链 + AlterPartitionManager 上报链

## 已读测试 (2 个)

- `PartitionTest`: 状态机/ISR 扩缩场景
- `AbstractFetcherThreadTest`: 两阶段/截断场景
- `PartitionLockTest`: 读写锁并发

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (8 元素, 对应源码位置)
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 3 个测试文件

## 跨域发现

- 来源: K-4 Pass 1 — 设计文档 (docs/design/design.md §Replication) 明示 ISR 模型学术对照是 PacificA (非 Raft/Zab)
- 发现: "不要求每写 fsync" (设计文档) 与 K-3 刷盘机制 (§2.5) 呼应 — 副本重进 ISR 必须全量重同步 (即使丢未刷盘数据)
- 已对照验证: K-3 UnifiedLog.appendAsFollower 链 (codebase-memory trace_path 实证) + 设计文档 §Replication
