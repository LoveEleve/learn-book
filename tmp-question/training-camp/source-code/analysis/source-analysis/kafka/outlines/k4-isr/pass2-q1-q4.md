# K-4 闭环笔记 Q1-Q4: ISR 模型/扩缩/状态机/两阶段拉取

## Q1: ISR 模型 vs 多数派 — 为什么 Kafka 不用 majority vote?

假设: ISR 是动态追赶集合, 提交 = 全 ISR 收到, 而非多数派。

验证过程:
- 设计文档 (docs/design/design.md §Replicated Logs): "Instead of majority vote, Kafka dynamically maintains a set of in-sync replicas (ISR) that are caught-up to the leader. Only members of this set are eligible for election as leader. A write to a Kafka partition is not considered committed until all in-sync replicas have received the write"
- 多数派代价: "To tolerate one failure requires three copies of the data, and to tolerate two failures requires five copies" — 5x 磁盘/1/5 吞吐
- ISR 优势: f+1 副本容忍 f 失败 (与多数派 2f+1 相同容忍度, 但副本数更少); "the ability to commit without the slowest servers is an advantage of the majority vote" — 该优势被客户端 acks 选择权抵消
- 学术对照: "The most similar academic publication we are aware of to Kafka's actual implementation is PacificA from Microsoft"
- 提交条件 (设计文档): ①复制到全 ISR ②ISR ≥ min.insync.replicas — "committed message will not be lost, as long as there is at least one in sync replica alive"
- 每写不 fsync: "we do not want to require the use of fsync on every write for our consistency guarantees" — 与 K-3 刷盘 (§2.5) 呼应

代码类型: 设计权衡分析 (非实现)

结论: **Kafka 提交语义 = 全 ISR 复制 (非多数派): ISR 是动态追赶集合, f+1 副本容忍 f 失败; 学术模型是 PacificA; 每写不 fsync 是显式取舍 (重进 ISR 必须全量重同步)**。docs/design/design.md §Replication

## Q2: maybeExpandIsr/maybeShrinkIsr — ISR 动态维护

假设: 副本追上 HW 重进 ISR; 滞后超过 replica.lag.time.max.ms 被踢出; 变更上报 controller。

验证过程:
- maybeExpandIsr (Partition.scala:1018-1036): 读锁检查 needsExpandIsr (Partition.scala:L1019-1021) → 写锁 prepareIsrExpand (Partition.scala:L1023-1031) → **submitAlterPartition 锁外上报** (Partition.scala:L1034, "may increment the high watermark... complete delayed operations")
- 重进条件 isFollowerInSync (Partition.scala:L1049-1054): followerEndOffset >= leaderLog.highWatermark && >= leaderEpochStartOffset — 必须追到 HW 之上
- 资格 isReplicaIsrEligible (Partition.scala:L1056-1074): 非 fenced + 非关停 + broker epoch 匹配 (Partition.scala:L1071-1073)
- maybeShrinkIsr (Partition.scala:1231-1269): 读锁检查 (Partition.scala:L1233) → 写锁 getOutOfSyncReplicas + prepareIsrShrink (Partition.scala:L1239-1259) → 锁外上报 (Partition.scala:L1267)
- getOutOfSyncReplicas (Partition.scala:L1297-1306): **isInflight 时返回空** (Partition.scala:L1299) — 有 in-flight 更新不重复操作
- 两类落后 (注释 Partition.scala:L1284-1293): stuck (LEO 在 maxLagMs 内没更新) / slow (maxLagMs 内没追上 LEO) — 都看 lastCaughtUpTimeMs

代码类型: Implementation (ISR 动态维护)

结论: **expand = 追平 HW+epoch 起点 (Partition.scala:L1049-1054) 读锁检查写锁执行; shrink = stuck/slow 两类落后 (Partition.scala:L1284-1293) 超 replicaLagTimeMaxMs; 变更统一 submitAlterPartition 锁外上报 (Partition.scala:L1034,1267) — 上报在锁外防死锁 (HW 推进会完成延迟请求)**。Partition.scala:1018-1074,1231-1306

## Q3: makeLeader/makeFollower 状态机 — 角色转换

假设: 角色转换在写锁内: epoch 防旧请求, leader 记 epoch 起点供截断, follower 清 ISR 重启 fetcher。

验证过程:
- makeLeader (Partition.scala:733-830): partitionEpoch 检查防旧 (Partition.scala:L743-747) → updateAssignmentAndIsr (Partition.scala:L764-771) → **assignEpochStartOffset (Partition.scala:L793, 新 epoch 起点缓存 — follower 截断查询依据)** → resetReplicaState (Partition.scala:L797-804) → maybeIncrementLeaderHW (Partition.scala:L822, "ISR could be down to 1") → 锁外 tryCompleteDelayedRequests (Partition.scala:L826-827)
- makeFollower (Partition.scala:839-885): epoch 检查 (Partition.scala:L844-848) → leader 先更新再清 ISR (Partition.scala:L851-853, 防 under-min-isr 误报) → **isr = Set.empty** (Partition.scala:L861) → 返回 isNewLeaderEpoch 触发 fetcher 重启 (Partition.scala:L883)
- updateFollowerFetchState (Partition.scala:909-937): 读锁内更新 (Partition.scala:L923-931) — "avoid the race between ISR updates and the fetch requests from rebooted follower" (Partition.scala:L921-922)
- LeaderAndIsr 由 controller 下发 (K-5 衔接)

代码类型: Implementation (状态机)

结论: **makeLeader = epoch 防旧 (Partition.scala:L743) + epoch 起点缓存供截断 (Partition.scala:L793) + HW 重算 (Partition.scala:L822); makeFollower = 清空 ISR (Partition.scala:L861) + 重启 fetcher (Partition.scala:L883); 全程写锁 + 锁外完成延迟请求 (Partition.scala:L826) — 状态变更与请求完成解耦**。Partition.scala:733-885,909-937

## Q4: 两阶段拉取 — doWork 为什么先 truncate 再 fetch?

假设: 副本同步必须先保证日志回退正确 (截断到安全点) 才能拉新数据, 否则新数据叠在错位日志上。

验证过程:
- doWork (AbstractFetcherThread.scala:115-118): **maybeTruncate() → maybeFetch()** — 两阶段固定顺序
- maybeTruncate (AbstractFetcherThread.scala:L174-182): 有 epoch 的分区 → truncateToEpochEndOffsets (AbstractFetcherThread.scala:L177); 无 epoch → truncateToHighWatermark (AbstractFetcherThread.scala:L180) — 双路径
- maybeFetch (AbstractFetcherThread.scala:L120+): leader.buildFetch → 发送 → processFetchRequest (AbstractFetcherThread.scala:L318) → processPartitionData → Log.appendAsFollower (K-3 衔接, trace_path 实证)
- 截断锁保护: truncateToEpochEndOffsets 在 partitionMapLock 内 (AbstractFetcherThread.scala:L215) + 校验 epoch 未变 (AbstractFetcherThread.scala:L224-225) — "Ensure we hold a lock during truncation"
- 截断迭代: epoch 未知时 truncationCompleted=false → 再发 OffsetsForLeaderEpochRequest (AbstractFetcherThread.scala:L638)

代码类型: Implementation (拉取协议)

结论: **两阶段 = maybeTruncate (AbstractFetcherThread.scala:L174: epoch 路径 AbstractFetcherThread.scala:L177 / HW 路径 AbstractFetcherThread.scala:L180) → maybeFetch (AbstractFetcherThread.scala:L120); 截断在锁内+epoch 校验 (AbstractFetcherThread.scala:L215-225); 迭代截断直到 truncationCompleted (AbstractFetcherThread.scala:L638) — 先回退后前进, 防数据错位**。AbstractFetcherThread.scala:115-182,211-232

跨域关联: K-3 (appendAsFollower/read) / K-5 (AlterPartition 上报) / K-7 (HW 推进完成延迟请求)
