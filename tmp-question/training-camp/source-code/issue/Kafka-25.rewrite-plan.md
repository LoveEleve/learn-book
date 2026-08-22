# Kafka-25 重写规划

> 题目：副本怎么迁、leader 怎么换——Controller 的 broker 状态机、leader 选举与 reassignment 主链
> 状态：K-7 Controller 域第 3 篇，按"副本状态机与 leader 选举/重分配"展开
> 目标：解释 Controller 如何根据 broker 状态变化（fenced/unfenced/controlled shutdown）、手工 `electLeaders` 请求、以及分区重分配请求来修改 leader/ISR/replicas。覆盖 `handleBrokerFenced` / `handleBrokerUnfenced` / `handleBrokerInControlledShutdown`、`electLeader` 的 PREFERRED/ONLINE/UNCLEAN 三档语义，以及 `changePartitionReassignment` 的新旧 assignment 状态推进。

## 1. 读者困惑

- broker 被 fenced、恢复、优雅关闭时，Controller 具体怎么修改分区 leader/ISR？
- PREFERRED / ONLINE / UNCLEAN 三种 leader 选举到底什么时候用？
- `partitionsWithNoLeader` 是怎么和 unfenced broker 关联起来的？
- 副本重分配（reassignment）是一次性替换 replicas，还是有过渡状态？
- 什么时候会触发 unclean leader election，为什么说它可能导致数据丢失？
- controlled shutdown 为什么要先迁走 leader 再允许停机？

## 2. 一句话顿悟

**Controller 的副本状态机是“broker 状态变化 → 影响分区集合 → 选举/重分配记录”的流水线：broker fenced 时，移出 ISR 并重选 leader；broker unfenced 时，拿无 leader 分区做重新选举；controlled shutdown 时，先迁走 leader；`electLeader` 根据 PREFERRED/ONLINE/UNCLEAN 三档语义选 leader；`changePartitionReassignment` 则把新的 replicas 集合变成一串 PartitionChangeRecord，渐进推进分区状态。**

## 3. 五要素卡片

### 读者问题

一个 broker 优雅关机时，为什么不能直接停机？Controller 如何把它领导的分区迁走，再让它安全退出？

### 入口

- `BrokerHeartbeatManager`：broker 状态机（FENCED/UNFENCED/CONTROLLED_SHUTDOWN/SHUTDOWN_NOW）
- `ReplicationControlManager.handleBrokerFenced` / `handleBrokerUnfenced` / `handleBrokerInControlledShutdown`
- `electLeaders` / `electLeader`
- `PartitionChangeBuilder.Election`：PREFERRED / ONLINE / UNCLEAN
- `changePartitionReassignment` / `PartitionReassignmentReplicas`

### 状态核心

- broker 状态：FENCED / UNFENCED / CONTROLLED_SHUTDOWN / SHUTDOWN_NOW
- 选举类型：PREFERRED / ONLINE / UNCLEAN
- reassignment：old replicas → adding/removing replicas → new replicas
- affected partitions：BrokersToIsrs / partitionsWithNoLeader

### 失败路径

- broker 直接停机不迁 leader → 分区短暂全挂
- 选举总是用 preferred → leader 不在 ISR 时选举失败
- ISR 全空时不允许 unclean election → 分区永久无 leader
- reassignment 一次性替换 → follower 还没追平就当正式副本

### 连接点

- 前文 `Kafka-7`：Controller 主脑负责归属决策。
- 前文 `Kafka-24`：PartitionRegistration / BrokersToIsrs 作为状态与索引。
- 前文 `Kafka-9`：broker 本地接收 LeaderAndIsrRequest 执行这些决策。

## 4. 总图

```text
broker 状态变化（fenced / unfenced / controlled shutdown）
  → 找受影响分区（BrokersToIsrs / partitionsWithNoLeader）
    → PartitionChangeBuilder 选举 leader / 更新 ISR
      → 生成 PartitionChangeRecord
        → 写入元数据日志
          → broker 收到 LeaderAndIsrRequest 执行

手工 electLeaders 请求
  → PREFERRED / ONLINE / UNCLEAN
    → electLeader(topic, partition)
      → 生成选举记录

reassignment
  → changePartitionReassignment(old, target)
    → addingReplicas / removingReplicas 过渡
      → 渐进推进到新副本集合
```

## 5. 关键边界

- 本篇聚焦 Controller 侧副本状态变化，不展开 broker 本地 Partition 执行细节（Kafka-9）。
- 不把 UNFENCED 与 partitionsWithNoLeader 混成“恢复 broker 就一定接 leader”。
- 不把 reassignment 写成“一次性替换 replicas”，它是过渡状态推进。
- 不把 ELR/LastKnownELR 细节重复展开（Kafka-24 已讲）。

## 6. 失败方案推演

1. **broker shutdown 直接停机**：leader 不迁走，分区瞬间失去 leader。
2. **所有选举都只走 preferred**：leader 不在 ISR 时无法恢复服务。
3. **所有选举都允许 unclean**：分区很快恢复，但有数据丢失风险。
4. **reassignment 直接替换 replicas 数组**：新副本未追平就当正式副本。

## 7. 误解清单

- “broker 恢复后一定会接管所有无 leader 分区。”：只是把它加入 acceptable leader 候选。
- “controlled shutdown 和 fencing 一样。”：controlled shutdown 是温和迁移，fencing 是立刻移出 ISR。
- “reassignment 一次就完成。”：要通过 adding/removing replicas 过渡。
- “unclean election 只是另一种选举风格。”：它可能导致数据丢失。
- “electLeaders 只给人工调用。”：内部也会有自动触发的选举路径。

## 8. 证据清单

- `metadata/src/main/java/org/apache/kafka/controller/BrokerHeartbeatManager.java:399`：broker 状态机。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1362`：handleBrokerFenced。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1407`：handleBrokerUnfenced。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1427`：handleBrokerInControlledShutdown。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1498`：electLeaders。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1554`：electLeader。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:231`：PREFERRED。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:260`：ONLINE / UNCLEAN。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:2058`：alterPartitionReassignments。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:2184`：changePartitionReassignment。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionReassignmentReplicas.java:32`：reassignment 过渡结构。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 broker 状态变化、选举、reassignment，不展开 PartitionRegistration 字段细节（Kafka-24）或 broker 执行（Kafka-9）。
- 目标正文：7000~11000 字。

## 10. 本轮重写主线

1. 从"broker 关机前 leader 怎么迁走"开场。
2. 否定直接停机、只用 preferred、全部 unclean、直接替换 replicas 四种方案。
3. 解释 broker 状态机与 handleBroker* 三路径。
4. 解释 electLeader 三档选举。
5. 解释 controlled shutdown 的温和迁移。
6. 解释 changePartitionReassignment 的过渡式推进。
7. 收网：Controller 根据 broker 状态变化生成记录，broker 再执行。