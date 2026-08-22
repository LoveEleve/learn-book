# Kafka-7 重写规划

> 题目：分区的 leader 是谁、ISR 怎么变——Controller 的分区归属决策与 Broker 心跳驱动
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka 集群层面"每个分区由哪个 broker 当 leader、哪些副本在 ISR"这个归属问题由谁决策：QuorumController 作为唯一 active controller 接收 broker 心跳、维护 partition registration，并通过 LeaderAndIsr 把归属推给 broker；broker 则通过 AlterPartition 把 ISR 变化回报给 controller。

## 1. 读者困惑

- 一个分区的 leader 到底是怎么选出来的？是 broker 之间商量吗？
- 为什么说 Kafka 里"分区 leader 选举"不是分布式选举，而是 Controller 单点决策？
- broker 加入/离开/心跳超时，Controller 如何感知并重新分配 leader？
- ISR 变化（follower 追不上/追上了）是怎么从 broker 流回 Controller 的？
- `leaderEpoch` 起什么作用，为什么 follower 用 epoch 而不是 offset 来判断是否回退？
- KRaft 下"谁是 controller"和"分区的 leader 是谁"是不是同一回事？

## 2. 一句话顿悟

**Kafka 的分区归属不是由 broker 之间选举产生的，而是由唯一的 active Controller 单点决策：Controller 通过 broker 心跳维护每个 broker 的注册状态（fenced/unfenced），一旦 broker 进出，就用 PartitionChangeBuilder 在现有 replica 与 ISR 集合内为每个受影响分区选一个新 leader，并把结果写成 metadata 记录；broker 通过 LeaderAndIsr 请求应用归属、通过 AlterPartition 请求把 ISR 变化回报给 Controller 闭环。**

## 3. 五要素卡片

### 读者问题

一个 topic 有 3 个副本，leader 挂了之后，谁来决定下一个 leader？follower 追不上 leader 时，ISR 缩小的决定又是谁做的、怎么通知所有人的？

### 入口

- `QuorumController`：唯一的 active controller，处理所有元数据变更请求（appendWriteEvent 单线程事件模型）
- `ReplicationControlManager`：controller 内负责 topic/partition 归属的子管理器
- `BrokerHeartbeatManager`：跟踪每个 broker 的心跳与注册状态
- `PartitionChangeBuilder`：具体执行 leader 选举决策（PREFERRED/ONLINE/UNCLEAN）
- `PartitionRegistration`：每个分区的 leader/ISR/replicas/leaderEpoch 状态
- broker 侧：`ReplicaManager.handleLeaderAndIsrRequest`、`AlterPartitionManager`
- `LeaderAndIsrRequest` / `AlterPartitionRequest`：归属下发与 ISR 回报的两个通道

### 状态核心

- Broker 状态机：FENCED / UNFENCED / CONTROLLED_SHUTDOWN / SHUTDOWN_NOW
- `PartitionRegistration`：leader、isr、replicas、leaderEpoch、partitionEpoch、elr
- `PartitionChangeBuilder.Election`：PREFERRED / ONLINE / UNCLEAN 三种选举语义
- `leaderEpoch`：每次 leader 变更 +1，follower 据此判断是否该回退

### 失败路径

- 没有集中决策 → 多个 broker 同时自认 leader，脑裂
- 心跳超时不处理 → 挂掉的 broker 继续被当作 leader/ISR 成员
- 不校验 leaderEpoch → 旧 leader 在切换后继续写，数据被覆盖
- broker 没追上 metadata 就 unfence → 带着旧视图服务，分区归属混乱
- 不回报 ISR 变化 → controller 不知道哪些 follower 是活的

### 连接点

- 前文 `Kafka-6`：ConsumerGroup 解决"组内成员分到哪些分区"，本篇解决"分区本身在集群里由谁当 leader"——两个层次的归属。
- 前文 `Kafka-4`：fetch 主链里 FENCED_LEADER_EPOCH 与 leaderEpoch 相关，本篇补上 epoch 从哪来。
- 后文 `Kafka-8`：KRaft 共识决定"谁是 active controller"，本篇假设"已有一个 controller"，不展开 Raft 细节。

## 4. 总图

```text
active Controller (QuorumController)
  → appendWriteEvent 单线程处理所有写
    → ReplicationControlManager
      → PartitionChangeBuilder 选 leader（PREFERRED/ONLINE/UNCLEAN）
        → 写成 metadata 记录（PartitionChangeRecord）

Broker 侧
  → 心跳 → Controller 维护 fenced/unfenced/controlled shutdown
  → 收到 LeaderAndIsr → 应用 leader/ISR 归属
  → ISR 变化 → AlterPartition 回报 Controller
    → Controller 更新 PartitionRegistration 再广播
```

## 5. 关键边界

- 本篇只讲"分区 leader 与 ISR 归属"的决策与传播，不展开 KRaft Raft 共识本身（后文 Kafka-8）。
- 不把"谁是 controller"（KRaft 选举）和"分区 leader 是谁"（controller 决策）混成一个概念。
- 不把 ISR 说成 broker 之间自行协商：变化由 leader broker 通过 AlterPartition 回报，由 controller 最终确认。
- 不展开 Partition 状态机的全部细节（Kafka-9 会做），本篇聚焦 controller 决策路径。

## 6. 失败方案推演

1. **broker 之间互相选举分区 leader**：每次分区多、变化频繁时协商成本高，且网络分区时多个 broker 都自认 leader，脑裂。
2. **没有心跳、靠 broker 自报状态**：controller 无法感知 broker 实际存活，挂掉的 broker 继续被当 leader。
3. **ISR 变化只在 broker 本地生效**：controller 不知道真实 ISR，leader 变更时可能选一个其实没追上数据的 follower。
4. **不校验 leaderEpoch**：旧 leader 与心跳超时期间仍写分区，新 leader 切换后旧数据被覆盖且无人察觉。

## 7. 误解清单

- "分区 leader 是 broker 之间投票选的"：是 controller 单点决策，不是分布式选举。
- "ISR 是 broker 自己协商的"：由 leader broker 通过 AlterPartition 回报，controller 确认。
- "fenced broker 还能继续当 leader"：fenced 即被移出 ISR 并触发重新选举。
- "leaderEpoch 只是元数据编号"：follower 用它判断是否回退，是数据一致性关键。
- "KRaft 的 controller 选举 == 分区 leader 选举"：前者决定谁当 controller，后者由 controller 决策。

## 8. 证据清单

- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:931`：appendWriteEvent 单线程事件模型。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:145`：负责 ISR 与 leader 管理。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1554`：electLeader 决策入口。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:220`：electLeader 三种选举类型。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:231`：electPreferredLeader。
- `metadata/src/main/java/org/apache/kafka/controller/BrokerHeartbeatManager.java:399`：calculateNextBrokerState。
- `metadata/src/main/java/org/apache/kafka/metadata/PartitionRegistration.java:157`：isr / leaderEpoch 状态。
- `core/src/main/scala/kafka/server/ReplicaManager.scala:1993`：handleLeaderAndIsrRequest。
- `core/src/main/scala/kafka/server/AlterPartitionManager.scala:116`：submit 提交 ISR 变化回报。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`（无 ZK，controller 由 KRaft 决定）。
- 本篇聚焦 controller 决策与 broker 心跳驱动；不展开 KRaft Raft 实现、Partition 完整状态机、副本拉取细节。
- 目标正文：8000~12000 字；核心拆解层覆盖 broker 心跳状态机、leader 选举、LeaderAndIsr 下发、AlterPartition 回报、leaderEpoch。

## 10. 本轮重写主线

1. 从"leader 挂了谁来决定下一个"开场，引出 controller 单点决策。
2. 否定"broker 互选"与"无心跳自报"两种朴素方案。
3. 讲清 QuorumController 是唯一决策者 + 单线程事件模型。
4. broker 心跳 → fenced/unfenced 状态机 → 触发 leader 变更。
5. PartitionChangeBuilder 三种选举语义，leaderEpoch 递增。
6. LeaderAndIsr 下发 + AlterPartition 回报闭环。
7. 收网：分区归属 = controller 决策 + broker 心跳闭环，KRaft 只决定谁是 controller。