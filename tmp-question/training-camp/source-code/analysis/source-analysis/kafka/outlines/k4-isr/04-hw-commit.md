# K-4 Partition & ISR 篇 4/4 — 提交水位: HW 推进与跨域衔接

> 前置: [[K-4-isr-01]] [[K-4-isr-02]] [[K-4-isr-03]] | 复用: — | 对照: [[E-6-seqno]] (globalCheckpoint 对照) [[r9-replication]] (复制偏移对照) | 引出: [[K-12-fetchsession-01]] (消费读路径会话层) [[K-7-purgatory-01]] (延迟操作等待链) — (K-5 交付后补链)
> 🔴 A | 来源: Partition.scala:1152-1195 + docs/design/design.md §Availability + AbstractFetcherThread.scala:318
> 定位: K-4 卷收尾 — 回答"什么才算提交? HW 怎么推进? 与前后域怎么衔接?"

**读者处境**: 面试官问 "acks=all 怎么实现? 消费能看到未提交数据吗?" 你答 "ISR ack" — 但再问 "HW 具体怎么算? under-min-ISR 时怎么办? HW 和 ES 的 globalCheckpoint 什么区别?" 你答不上来。这篇是提交语义的完整答案, 收束 K-4 域。

### 1. 问题引入 — 提交水位

场景: producer 说 "已提交", 消费者才能看到 — "提交"在 broker 侧具体是什么?
- HW (high watermark) = 提交水位 (Partition.scala:1152)
- 本篇问题: HW 推进 (Q7) / 衔接 (Q8)

### 2. HW 推进 — 全 ISR 最小 LEO

场景: HW 怎么算?
- maybeIncrementLeaderHW (Partition.scala:1152-1195): **isUnderMinIsr → 不推进** (AbstractFetcherThread.scala:L1153-1156)
- newHW = leader LEO (AbstractFetcherThread.scala:L1160) → 遍历副本: 副本 LEO < newHW 且 (在 maximalIsr 或追赶中) → newHW = 副本 LEO (AbstractFetcherThread.scala:L1170-1174) — **HW = 全 ISR 最小 LEO**
- ISR 外追赶副本也等: shouldWaitForReplicaToJoinIsr (AbstractFetcherThread.scala:L1164-1167)
- 单调递增: leaderLog.maybeIncrementHighWatermark (AbstractFetcherThread.scala:L1177) — HW 只前进不后退

### 3. 提交语义 — 与配置的权衡

场景: 提交条件怎么配置?
- 设计文档: 提交 = ①复制到全 ISR ②ISR ≥ min.insync.replicas
- acks 选择权: acks=0/1/all — all 也只是"全 ISR"不是"全副本" (设计文档: "acknowledgement by all replicas does not guarantee the full set of assigned replicas")
- min.insync.replicas 权衡 (设计文档 §Availability): 高 = 更一致但可用性降 (ISR 不足拒写); 低 = 更可用但可能丢
- unclean.leader.election.enable (默认 false, 0.11.0.0 起) — 可用性 vs 一致性终极取舍
- 对照 E-6: ES globalCheckpoint = min(in-sync localCheckpoint) — 与 Kafka HW = min(ISR LEO) **同构**; 差异: ES 副本主动上报 (ReplicationTracker) vs Kafka follower 拉取后 leader 计算

### 4. 衔接 — 双链闭环

场景: K-4 怎么接 K-3/K-5?
- 写入链: processFetchRequest (AbstractFetcherThread.scala:318) → processPartitionData (AbstractFetcherThread.scala:L373) → appendAsFollower (K-3)
- 上报链: submitAlterPartition (Partition.scala:1034) → AlterPartitionManager → controller 持久化 (K-5)
- 消费链: fetchMessages → Partition.readRecords (K-12)
- 等待链: HW 推进 → tryCompleteDelayedRequests (Partition.scala:826) → DelayedProduce 完成 (K-7)

### 核心悬念
"HW 和 ES 的 globalCheckpoint 是不是一回事?" — 是: 都是"全同步副本集合的最小水位" (Kafka HW = min(ISR LEO), ES GCP = min(in-sync localCheckpoint)); 不是: 推进方向相反 (ES 副本独立推进 checkpoint 后 master 聚合; Kafka leader 在处理 follower 拉取请求时计算 HW (updateFollowerFetchState → maybeIncrementLeaderHW), 随 fetch 响应返回给 follower) — 面试对照题。

### 概念依赖链
Q7 HW → Q8 衔接 → (K-5/K-7/K-12 交付后回补双链)

### 源码锚点清单
- Partition.scala:1152-1195 (maybeIncrementLeaderHW) / 1153-1156 (under-min-ISR 冻结) / 1160 (初始 leader LEO) / 1164-1167 (shouldWaitForReplicaToJoinIsr) / 1170-1174 (全 ISR 最小 LEO) / 1177 (单调递增) / 826-827 (锁外延迟完成) / 1034 (submitAlterPartition)
- docs/design/design.md §Availability (min.insync.replicas/unclean election/acks 语义)
- AbstractFetcherThread.scala:318 (processFetchRequest) / 373 (processPartitionData)
