# K-4 Partition & ISR 篇 1/4 — 谁算活着: ISR 模型与动态维护

> 前置: [[K-3-log-01]] (分段+写路径) [[K-3-log-02]] (读路径) | 复用: — | 对照: [[r9-replication]] (Redis 主从) [[E-6-seqno]] (ES 复制协议) | 引出: [[K-4-isr-02]]
> 🔴 A | 来源: docs/design/design.md §Replication + Partition.scala:1018-1074,1231-1306
> 定位: K-4 卷开篇 — 回答"Kafka 的副本一致性模型是什么? ISR 怎么动态维护?"

**读者处境**: 面试官问 "Kafka 怎么保证不丢消息? ISR 是什么?" 你答 "副本同步" — 但再问 "为什么不用多数派? 副本落后多久出 ISR? 回来怎么重进?" 你答不上来。这篇是 ISR 模型的完整答案。

### 1. 问题引入 — 复制不是多数派

场景: 3 副本写 1 条消息, 等几个 ack 才算提交? Raft/Zab 说多数 (2/3), Kafka 说"全 ISR" — 为什么?
- 设计文档 §Replicated Logs: ISR 动态集合 vs majority vote 的完整权衡
- 本篇问题: 模型 (Q1) / 动态维护 (Q2)

### 2. ISR vs 多数派 — 设计权衡

场景: 为什么 Kafka 不选 2f+1 多数派?
- 多数派代价: 容忍 1 失败要 3 副本, 2 失败要 5 副本 (设计文档 §Replicated Logs) — 5x 磁盘/1/5 吞吐
- ISR 方案: f+1 副本容忍 f 失败 — 同容忍度更少副本
- 学术模型: PacificA (设计文档明示)
- 提交条件: ①复制到全 ISR ②ISR ≥ min.insync.replicas
- 权衡代价: 多数派"不等慢节点"优势被客户端 acks 选择权抵消 (acks=0/1/all)

### 3. 动态维护 — expand 与 shrink

场景: 副本掉队怎么处理? 回来怎么重进?
- ISR 集合读取: inSyncReplicaIds (Partition.scala:412) — 当前 ISR 是分区状态的直接视图
- shrink: getOutOfSyncReplicas (Partition.scala:1297-1306) — 两类落后: stuck (LEO 超时没动) / slow (没追上) (注释 Partition.scala:L1284-1293)
- expand: 重进条件 isFollowerInSync (Partition.scala:1049-1054) — LEO ≥ HW 且 ≥ leader epoch 起点
- 资格: isReplicaIsrEligible (Partition.scala:L1056-1074) — 非 fenced/非关停/broker epoch 匹配
- 两阶段锁: 读锁检查 (Partition.scala:L1019) → 写锁执行 (Partition.scala:L1023) → **锁外上报** (Partition.scala:L1034)

### 4. 上报链 — ISR 变更持久化

场景: ISR 变了, 谁记住?
- submitAlterPartition 锁外上报 (Partition.scala:1034,1267) — "may increment the high watermark (and consequently complete delayed operations)" — 锁外防死锁
- AlterPartitionManager → controller 持久化 (K-5 衔接) — "ISR set is persisted in the cluster metadata whenever it changes" (设计文档)

### 核心悬念
"Kafka 为什么不用 Raft 的多数派做数据复制?" — 多数派 2f+1 才能容忍 f 失败 (5 副本才能容忍 2), 数据复制成本太高; ISR 用 f+1 达到同容忍度, 代价是提交要等全 ISR (慢节点拖累) — 用客户端 acks 选择权换吞吐, 用 f+1 副本换磁盘/吞吐。学术上是 PacificA 而非 Raft。

### 概念依赖链
Q1 模型 → Q2 动态维护 → Q3 上报 → (02 篇: 状态机) → (K-5 衔接)

### 源码锚点清单
- docs/design/design.md §Replication (ISR vs 多数派全文)
- Partition.scala:1018-1036 (maybeExpandIsr) / 1034 (锁外上报) / 1049-1054 (isFollowerInSync) / 1056-1074 (isReplicaIsrEligible) / 1231-1269 (maybeShrinkIsr) / 1284-1293 (stuck/slow 注释) / 1297-1306 (getOutOfSyncReplicas)
