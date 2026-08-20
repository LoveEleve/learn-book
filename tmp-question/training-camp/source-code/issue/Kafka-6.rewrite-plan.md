# Kafka-6 重写规划

> 题目：Consumer 的 group 成员资格由谁说了算——GroupCoordinator、Classic 四步重平衡与 Modern 心跳重平衡
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka 消费者组"成员资格 + 分区归属"是如何被协调出来的：谁主持协调、成员如何加入/离开、分区如何被分配、Classic（四步协议）与 Modern（KIP-848 心跳协议）两条路径各自如何工作，以及为什么 Modern 能避免 Classic 的 stop-the-world。

## 1. 读者困惑

- 多个 consumer 用同一个 groupId 订阅，凭什么某个分区只被一个 consumer 消费？谁来做这个决定？
- 协调器（GroupCoordinator）到底在哪，为什么每个 group 只有一个"主持者"？
- Classic 协议的 JoinGroup → SyncGroup → Heartbeat → LeaveGroup 四步各在做什么？为什么叫 stop-the-world？
- KIP-848（Modern）只说"新协议是增量重平衡"，它的增量到底增量在哪？
- 成员加入/离开/订阅变化/心跳超时，分别怎么触发分区重新分配？
- `memberEpoch`、`generationId` 在两条协议里分别代表什么，为什么永远"只认最新的一代"？

## 2. 一句话顿悟

**消费者组把"谁消费哪些分区"从每个 consumer 的"猜"，变成由一台协调器主持、全体成员共同维护的一份"成员+归属"共识：Classic 用"加入→同步→心跳→离开"四步 + 世代号(generation)做全组 stop-the-world 重平衡；Modern(KIP-848) 用单一心跳 + 单调递增的 member epoch 做增量重平衡，只有受影响的成员才重新协调，其余成员继续消费。**

## 3. 五要素卡片

### 读者问题

两个 consumer 用一个 groupId 订阅同一个 topic，为什么不会重复消费同一个分区？协调器如何给每个分区找到"唯一主人"，并且在成员变化时保证归属重新收敛？

### 入口

- `GroupCoordinatorShard`：协调器的复制状态机，按 `__consumer_offsets` 分区 shard 承载一组 group
- `GroupMetadataManager`：请求处理与回放，同时处理 Classic 与 Modern 两种协议
- 客户端：`ConsumerMembershipManager` / `ConsumerHeartbeatRequestManager`（Modern）、`AbstractCoordinator` / `ConsumerCoordinator`（Classic）
- `MemberState`：客户端成员状态机（UNSUBSCRIBED/JOINING/RECONCILING/ACKNOWLEDGING/STABLE/FENCED/...）
- `ConsumerGroup`（Modern 服务端） / `ClassicGroup`（Classic 服务端）
- Assignor：服务端 `UniformAssignor`/`RangeAssignor`（Modern），客户端 `AbstractPartitionAssignor`（Classic）

### 状态核心

- Classic：`ClassicGroupState`（EMPTY→PREPARING_REBALANCE→COMPLETING_REBALANCE→STABLE→DEAD）+ `generationId` + leader/members/pendingJoin/pendingSync
- Modern：`memberEpoch`（0=加入，正数=已入组，-1/-2=离开）、`groupEpoch`、`targetAssignmentEpoch`、`assignedPartitions`/`partitionsPendingRevocation`
- 客户端 `MemberState`：JOINING → RECONCILING → ACKNOWLEDGING → STABLE，FENCED 后回 JOINING

### 失败路径

- 每个 consumer 自己决定消费哪些分区 → 重复消费/漏消费
- 没有协调器，成员加入/离开无感知 → 归属无法收敛
- Classic 每次成员变化都全组停止 → 大组 + 高频变动时反复全停
- Modern 若不做增量 → 心跳里每次携带全量分配，退化成 Classic
- 成员 epoch 失配（STALE/FENCED）后仍按旧 epoch 继续 → 数据重复或丢失

### 连接点

- 前文 `Kafka-4`：Consumer 的 fetch 主链，本篇补上"分区归属从哪来"
- 前文 `Kafka-5`：FetchSession 处理"一个 broker 会话有哪些分区"，本篇处理"一个 group 里每个成员分到哪些分区"
- 后文 `Kafka-7`/`Kafka-8`：Controller 与 KRaft 处理"分区本身在集群里由谁做 leader"，与本篇"组内成员的分区归属"是两个层次

## 4. 总图

```text
成员 C1、C2 用同一 groupId
  → CoordinatorRequestManager 发现协调器（按 groupId 哈希到 __consumer_offsets 分区）
    → 协调器主持 group 的成员/归属共识

Classic（四步）
  JoinGroup → 收集成员+订阅
    → PREPARING_REBALANCE
      → SyncGroup（leader 用客户端 assignor 算分配）
        → COMPLETING_REBALANCE
          → STABLE：心跳维持，成员变化再触发四步

Modern（KIP-848，单心跳）
  首次 heartbeat(epoch=0) 加入
    → 协调器算 target assignment，返回 assignment + memberEpoch=1
      → 后续 heartbeat 带本成员已接受的 assignment 回执
        → 协调器只对受影响成员下发新 assignment，其余成员不动
          → 成员 epoch 单调递增，失配即 FENCED 重新加入
```

## 5. 关键边界

- 本篇只讲"组成员资格 + 分区归属的协调"，不展开 KIP-848 细节错误码全集，也不展开 StreamsGroup/ShareGroup。
- 不把 Classic 的 `generationId` 与 Modern 的 `memberEpoch` 混成一个概念：前者随整组重平衡递增，后者随单个成员协调递增。
- 不把"协调器在哪"写成"某个固定 broker 常驻服务"：它是 `__consumer_offsets` 对应分区的 leader 所承载的 shard。
- 不把 Assignor 写成只有服务端或只有客户端：Modern 在服务端，Classic 在客户端 leader 执行。

## 6. 失败方案推演

1. **每个 consumer 自己决定消费哪些分区**：没有全局视图，两个成员会重复消费同一分区，或分区无人消费。
2. **没有协调器、靠成员间互相通信**：成员加入/离开无法及时感知，归属无法收敛。
3. **Classic 每次变化都全组四步重平衡**：大组 + 高频变动时，每次都 stop-the-world，吞吐抖动大。
4. **Modern 不携带回执、不校验 epoch**：协调器无法知道成员是否真正接受了新 assignment，epoch 失配后旧成员继续消费会重复/丢数据。

## 7. 误解清单

- "协调器是独立服务"：它是 `__consumer_offsets` 分区 leader 上的 shard。
- "Classic 和 Modern 是两套完全独立的系统"：同一个 GroupCoordinatorShard/GroupMetadataManager 同时处理两条协议。
- "generationId == memberEpoch"：Classic 世代号 vs Modern 成员 epoch 语义不同，递增时机也不同。
- "Modern 完全不需要重平衡"：它仍有重平衡，只是增量调整受影响成员。
- "partition assignment 一定在客户端"：Modern 在服务端 assignor 计算，Classic 在客户端 leader 计算。

## 8. 证据清单

- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerMembershipManager.java`：Modern 成员生命周期。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/MemberState.java`：客户端成员状态机。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerHeartbeatRequestManager.java:229`：HeartbeatState.buildRequestData。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java:463`：Classic joinGroupIfNeeded。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:651`：Classic leader 执行 onLeaderElected 分配。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:4691`：consumerGroupHeartbeat 入口。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6086`：classicGroupJoin 入口。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroupState.java`：Classic 组状态机。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroup.java:133`：generationId。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroup.java:830`：validateMemberEpoch（StaleMemberEpoch）。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/assignor/UniformAssignor.java:53`：Modern 服务端 assignor。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CoordinatorRequestManager.java`：协调器发现。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦组成员协调与分区归属；不展开协议错误码全集、ShareGroup/StreamsGroup、offset 存储细节（后续篇）。
- 目标正文：8000~12000 字；核心拆解层覆盖 Classic 四步、Modern 单心跳、epoch/generation 语义、失败恢复。

## 10. 本轮重写主线

1. 从"两个 consumer 为什么不会重复消费"开场，引出"协调器主持归属共识"。
2. 否定"各自决定"与"成员间互信"两种朴素方案。
3. 讲清协调器是谁、在哪：GroupCoordinatorShard + __consumer_offsets 分区。
4. Classic 四步：JoinGroup → SyncGroup → Heartbeat → LeaveGroup，generation 与 stop-the-world。
5. Modern 单心跳：member epoch、target assignment、回执、增量重平衡。
6. 收网：Classic/Modern 共享"协调器主持归属"这一本质，差异只在重平衡的粒度。