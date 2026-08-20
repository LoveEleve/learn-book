# K-4 闭环笔记 Q5-Q8: epoch 截断/ fencing/HW 推进/衔接

## Q5: epoch 截断 4 规则

假设: follower 向 leader 问 epoch 终点, 按返回情况分 4 种截断策略。

验证过程:
- getOffsetTruncationState (AbstractFetcherThread.scala:604-655) — 源码注释 AbstractFetcherThread.scala:L585-599 即 4 规则:
  - **规则 1 (AbstractFetcherThread.scala:L606-613)**: leader 返回 undefined epoch offset → 用 HW (初始 fetch offset): leader 旧格式 (< IBP_0_11_0) 或请求 epoch < leader 首知 epoch
  - **规则 2 (AbstractFetcherThread.scala:L614-619)**: valid offset + undefined epoch → min(leader offset, LEO): IBP < 2.0 场景 (OffsetForLeaderEpoch v0)
  - **规则 3 (AbstractFetcherThread.scala:L626-638)**: leader epoch 本地未知 → truncate 到最大已知小 epoch 的 endOffset + **truncationCompleted=false 再发请求** (AbstractFetcherThread.scala:L638) — 迭代逼近
  - **规则 3 变体 (AbstractFetcherThread.scala:L643-653)**: endOffsetForEpoch 无结果 (新 broker 无 epoch 追踪) → min(leader offset, LEO), 一次完成
  - **规则 4 (AbstractFetcherThread.scala:L639-642)**: 正常 → min(leader offset, follower epoch endOffset, LEO) (AbstractFetcherThread.scala:L640-641)
- 触发链: maybeTruncate (AbstractFetcherThread.scala:L174-182) → truncateToEpochEndOffsets (AbstractFetcherThread.scala:L211-232: 锁内 + epoch 校验 AbstractFetcherThread.scala:L224-225)

代码类型: Algorithmic (一致性截断)

结论: **4 规则 (源码注释 AbstractFetcherThread.scala:L585-599): ①undefined offset→HW (AbstractFetcherThread.scala:L606) ②valid offset+undefined epoch→min(leader,L, LEO) (AbstractFetcherThread.scala:L614) ③epoch 未知→最大已知小 epoch endOffset+迭代 (AbstractFetcherThread.scala:L626-638) ④正常→min(leader, follower endOffset, LEO) (AbstractFetcherThread.scala:L639); 截断锁内+epoch 校验 (AbstractFetcherThread.scala:L215-225)**。AbstractFetcherThread.scala:604-655

## Q6: FENCED_LEADER_EPOCH — fencing 语义

假设: follower 的 epoch 比 leader 旧 → 被 fence, 等新 LeaderAndIsr; 对照 Redis fencing token。

验证过程:
- 处理点 (AbstractFetcherThread.scala:276-280): FENCED_LEADER_EPOCH → onPartitionFenced (Partition.scala:L279)
- onPartitionFenced (AbstractFetcherThread.scala:L302-315): 请求 epoch == 当前 epoch → markPartitionFailed (AbstractFetcherThread.scala:L308, "Will await the new LeaderAndIsr state before resuming fetching"); 请求 epoch < 当前 → 重试 (AbstractFetcherThread.scala:L311-312)
- 语义: leader 已换 epoch, 旧 epoch 的 follower 写入无效 — 与 rd2-rlock (fencing token) 同源: 旧 token/epoch 的请求必须被拒
- makeLeader epoch 起点缓存 (Partition.scala:793) 保证新 leader 能回答 follower 的 epoch 查询

代码类型: Interface (fencing 契约)

结论: **FENCED_LEADER_EPOCH → onPartitionFenced (AbstractFetcherThread.scala:L302): 同 epoch 等新 LeaderAndIsr (AbstractFetcherThread.scala:L308), 旧 epoch 重试 (Partition.scala:L312) — epoch 即 fencing token (rd2-rlock 同源语义), leader epoch 起点缓存 (Partition.scala:793) 支撑查询**。AbstractFetcherThread.scala:276-315

## Q7: HW 推进 — 提交语义的实现

假设: HW = 全 ISR 的最小 LEO; under-min-ISR 时 HW 不动。

验证过程:
- maybeIncrementLeaderHW (Partition.scala:1152-1195): **isUnderMinIsr → 不推进** (Partition.scala:L1153-1156) → newHighWatermark = leader LEO (Partition.scala:L1160) → 遍历 remoteReplicas: 副本 LEO < newHW 且 (在 maximalIsr 或 shouldWaitForReplicaToJoinIsr) → newHW = 副本 LEO (Partition.scala:L1170-1174) — **HW = 全 ISR 最小 LEO**
- shouldWaitForReplicaToJoinIsr (Partition.scala:L1164-1167): isCaughtUp(LEO, maxLagMs) + isReplicaIsrEligible — ISR 外的追赶副本也等
- leaderLog.maybeIncrementHighWatermark (Partition.scala:L1177) — 单调递增 (K-3 UnifiedLog)
- 提交语义: 设计文档 "committed = all in-sync replicas received" — HW 即提交水位
- 触发: makeLeader (Partition.scala:L822) / updateFollowerFetchState 后 (K-4 消费链)

代码类型: Implementation (水位推进)

结论: **HW 推进 = 全 ISR 最小 LEO (Partition.scala:L1160-1174) + under-min-ISR 冻结 (Partition.scala:L1153) + ISR 外追赶副本也等 (Partition.scala:L1164-1167); HW 单调递增 (Partition.scala:L1177) — 提交水位即 HW, 消费只读 ≤ HW (设计文档)**。Partition.scala:1152-1195

## Q8: 与 K-3/K-5 衔接 — 写入链与上报链

假设: appendAsFollower 是副本写入入口 (K-3); AlterPartitionManager 把 ISR 变更上报 controller (K-5)。

验证过程:
- 写入链: AbstractFetcherThread.processFetchRequest (AbstractFetcherThread.scala:L318) → processPartitionData (AbstractFetcherThread.scala:L373) → Log.appendAsFollower (K-3, codebase-memory trace_path 实证: appendAsFollower → UnifiedLog.append → LocalLog.append → LogSegment.append)
- appendRecordsToFollowerOrFutureReplica (Partition.scala:1331) — follower 侧写入
- 上报链: submitAlterPartition (Partition.scala:1034,1267) → AlterPartitionManager → controller (K-5) — ISR 变更持久化到元数据 (设计文档: "This ISR set is persisted in the cluster metadata whenever it changes")
- 读链: fetchMessages (ReplicaManager) → Partition.readRecords (K-12 FetchSession 衔接)

代码类型: 衔接分析

结论: **双链闭环: 写入 (fetcher AbstractFetcherThread.scala:L318 → appendAsFollower → K-3) + 上报 (submitAlterPartition Partition.scala:L1034 → AlterPartitionManager → K-5 持久化); ISR 变更即元数据变更, controller 广播新 LeaderAndIsr 驱动 makeLeader/makeFollower (K-5)**。Partition.scala:1034,1331 + AbstractFetcherThread.scala:318

跨域关联: K-3 (appendAsFollower) / K-5 (AlterPartition) / K-12 (fetch 消费) / E-6 SeqNo (term 对照) / rd2-rlock (fencing)
