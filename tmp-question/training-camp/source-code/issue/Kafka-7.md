# Kafka-7. 分区的 leader 是谁、ISR 怎么变——Controller 的分区归属决策与 Broker 心跳驱动

> 场景：前一篇（Kafka-6）解决了"一个消费者组里每个成员分到哪些分区"，那是**组内成员**层次的归属。本篇要解决更高一层的归属：**一个分区本身在集群里由哪个 broker 当 leader、哪些副本在 ISR 里**。这也是 Kafka-4 里 `FENCED_LEADER_EPOCH` 背后那个 leaderEpoch 的源头。

## 先把真正的困惑摆出来：leader 挂了，谁来决定下一个 leader？

一个 topic 有 3 个副本，分布在 3 个 broker 上。正常时 broker 0 是 leader，broker 1、2 是 follower。现在 broker 0 突然宕机。接下来会发生什么？

直觉上最自然的方案是：剩下的两个 broker "商量"一下，谁当新 leader。

但这条路有一个致命问题：**网络分区时商量不出结果。** 假设 broker 0 没真挂，只是和 broker 1、2 之间网络断了。broker 1 认为"broker 0 挂了，我来当 leader"，broker 0 认为自己还活着、继续当 leader——两个 leader 同时写同一分区，数据就分裂了。这就是脑裂。

```text
broker 1 想：broker 0 挂了，我当 leader
broker 0 想：我还活着，我继续当 leader
  → 两个 leader 同时写 → 脑裂，数据不一致
```

所以"谁当 leader"不能由 broker 之间商量决定，必须有一个**唯一权威**来拍板。这个权威在 Kafka 里就是 **Controller**。

这就是本篇要解决的核心问题：**分区的 leader 与 ISR 归属，由唯一的 active Controller 单点决策，而不是由 broker 之间选举产生。**

*关键设计（斜体）：* *分区归属是"单点决策 + 广播执行"：active Controller 通过 broker 心跳感知 broker 存活，用 PartitionChangeBuilder 在现有 replica 与 ISR 集合内为受影响分区选新 leader，把结果写成 metadata 记录；broker 收到 LeaderAndIsr 后应用归属，再通过 AlterPartition 把 ISR 变化回报给 Controller，形成闭环。*[模式: 单点决策 + 状态机 + 下发/回报闭环]

## 第一层：Controller 是唯一的决策者，而且一次只处理一个事件

Kafka 里"Controller"不是一个抽象的"协调角色"，而是一个具体存在的进程实例：**active QuorumController**。在 KRaft 架构里，controller 是元数据分区的 Raft leader 所在的节点——也就是说，"谁是 controller"本身也是由 Raft 选出来的，但这个本篇不展开（留给 Kafka-8）。本篇假设：已经有一个 active controller。

Controller 最关键的运行特征，是**单线程事件模型**：所有会改变元数据的操作，都通过 `appendWriteEvent()` 排队成事件，一个一个执行（`QuorumController.java:931`）。

这个设计有什么好处？

- **没有并发决策**。两个 broker 同时提交的变更，在同一线程里串行处理，不会出现"controller 对同一分区给了两个 leader"的情况。
- **决策即日志**。每个事件处理完，产生的不只是内存更新，还有要写进 Raft log 的元数据记录（`ControllerResult`）。一旦提交，这个决策对所有节点可见。

如果 controller 不是单线程、而是多个线程并发处理元数据变更，主链会先在哪出问题？两个线程同时处理"broker A 心跳超时"和"broker B 请求成为 leader"时，就可能对同一个分区产生相互矛盾的两个决定。所以单线程不是性能偷懒，而是"唯一权威"能成立的前提。

```text
所有写请求
  → appendWriteEvent 排队
    → 单线程依次执行
      → 生成 ControllerResult（内存变更 + 待写记录）
        → 提交进 Raft log
```

## 第二层：Controller 怎么知道 broker 还活着——BrokerHeartbeatManager 与心跳状态机

Controller 要决策分区归属，首先得知道哪些 broker 活着。这不是 broker 自己报一句"我活着"就行，因为那样无法分辨"真的活着"还是"网络断了但进程还在"。

所以 Kafka 用**心跳 + 超时**来判断：每个 broker 周期性地向 controller 发 BrokerHeartbeatRequest。如果 controller 在一定时间内没收到某个 broker 的心跳，就认为它失联了。

`BrokerHeartbeatManager.calculateNextBrokerState()`（`BrokerHeartbeatManager.java:399`）根据当前状态和这次心跳的内容，算出 broker 应该进入的下一个状态。Broker 状态机有四个状态：

- `FENCED`：已隔离，不能服务任何分区；
- `UNFENCED`：已解除隔离，可以服务分区；
- `CONTROLLED_SHUTDOWN`：优雅关机中，正在移交 leader 角色；
- `SHUTDOWN_NOW`：可以立即停机。

关键的转移逻辑：

- **FENCED → UNFENCED**：broker 发送心跳请求解除隔离，且**它的 metadata offset 已经追上了自己的注册记录**（`request.currentMetadataOffset() >= registerBrokerRecordOffset`，`BrokerHeartbeatManager.java:412`）。这一步很关键——broker 不能带着旧视图上岗，必须追上最新的集群元数据才能服务。
- **UNFENCED → FENCED**：broker 请求被隔离，或心跳超时后被 controller 通过 stale broker tracker 逐个捞出并主动隔离（`maybeFenceOneStaleBroker`）。
- **UNFENCED → CONTROLLED_SHUTDOWN**：broker 请求关机，且它当前还领导着分区。controller 先让它在受控状态下移交领导权，等所有分区都移走后再允许真正停机。
- **CONTROLLED_SHUTDOWN → SHUTDOWN_NOW**：所有分区 leader 都移走了，可以停了。

如果把"心跳超时"当成"broker 没说话"而非"broker 挂了"，主链会先在哪出问题？一个网络分区的 broker 会被当作还活着，继续被保留在 ISR 里，controller 就不会为它的分区选新 leader——整个分区停摆。所以"超时即 fenced"是必须的。实现上它也不是一口气批量处理全部超时 broker，而是通过 tracker **逐个**捞出 stale broker 串行处理，这正好和 controller 的单线程事件模型一致。

## 第三层：broker 被 fenced，会触发什么——从 ISR 移出并重新选 leader

Controller 一旦决定把某个 broker 标记为 FENCED，就会调用 `handleBrokerFenced()`（`ReplicationControlManager.java:1362`）。它做两件事：

1. 生成 LeaderAndIsr 更新，把该 broker 从它所在的所有分区的 ISR 里移出，并为这些分区重新选 leader；
2. 写一条 BrokerRegistrationChangeRecord，把"这个 broker 被 fenced"这个事实持久化。

`generateLeaderAndIsrUpdates` 遍历 `brokersToIsrs.partitionsWithBrokerInIsr(brokerId)`——也就是这个 broker 在 ISR 里的全部分区——对每个分区调用 PartitionChangeBuilder 重新计算 leader 和 ISR。

broker 被 fenced 时，它的所有领导权都失效。也就是说：**如果这个 broker 是某分区的 leader，fenced 之后这个分区必须立刻换 leader。**

这里要注意区分几种 broker 离开场景：

- **fenced（心跳超时）**：从 ISR 移出 + 重选 leader；
- **unregistered（主动注销）**：从 ISR 和 ELR 移出 + 重选 leader；
- **controlled shutdown（优雅关机）**：先把 leader 角色移交给别人，再允许停机。

它们的共同点是：**凡是要从集群里消失的 broker，controller 都要先把它的分区归属重新收敛。**

## 第四层：选 leader 不是掷骰子——PartitionChangeBuilder 的三种选举语义

具体"选谁当 leader"由 `PartitionChangeBuilder.electLeader()`（`PartitionChangeBuilder.java:220`）决定。它有三档语义，从保守到激进：

### PREFERRED（首选副本）

选举目标是分区的**第一个副本**（`targetReplicas.get(0)`），即创建分区时的"首选 leader"。逻辑是：如果首选副本在 ISR 里且健康，就选它；否则退而求其次，选当前 leader（如果还健康），再否则从其余副本里挑一个在线的。这通常用于 `electLeaders` API 的 preferred 类型，目的是把 leader 拉回预分配的 broker，均衡负载。

### ONLINE（在线即可）

当前 leader 若健康则保留，否则从 replicas 里找第一个健康（在 ISR、未 fenced）的副本当 leader；如果普通在线副本都不满足，还会尝试 `lastKnownElr` 这个兜底分支。这是日常 broker 故障时最常用的路径——`generateLeaderAndIsrUpdates` 内部默认走的选举。

### UNCLEAN（不干净选举）

当 ISR 里没有任何副本存活时，ONLINE 选不出来。UNCLEAN 会从**不在 ISR 但仍在 replicas 里**的副本中挑一个当 leader。这会丢失未同步的数据，但换来"分区保持可用"。默认配置下这是被禁止的（`unclean.leader.election.enable=false`），除非运维明确开启。

三档语义的差别，本质是对"可用性 vs 一致性"的取舍：

- PREFERRED 优先负载均衡；
- ONLINE 优先不丢数据（只在 ISR 里选）；
- UNCLEAN 优先可用性（可能丢数据）。

```text
PREFERRED：首选副本健康？→ 是就选它，否就看当前 leader / 其余在线副本
ONLINE  ：ISR 里找健康副本，找不到就 NO_LEADER
UNCLEAN ：ISR 全挂时，从 replicas 里（不在 ISR）挑一个，可能丢数据
```

如果只有一种"随便选一个健康的"语义，主链会先在哪失败？预分配的 preferred replica 永远得不到重用，负载均衡失效；同时 ISR 全挂时分区直接没有 leader，无法自愈。所以三种语义分别对应不同诉求。

## 第五层：决策结果怎么落成记录，又怎么被传播出去

Controller 的每个决策都要变成**元数据记录**，而不是只改内存。以 leader 变更为例，`PartitionChangeBuilder` 生成一条 `PartitionChangeRecord`，记录新的 leader、ISR 等；`PartitionRegistration.merge()` 用它更新分区的内存状态，同时该记录被写进 Raft log（`PartitionRegistration.java:248`）。

`PartitionRegistration` 是分区归属的核心状态，它保存：

- `leader`：当前 leader；
- `isr`：在同步集合；
- `replicas`：全部分区副本；
- `leaderEpoch`：leader 变更次数，每次 leader 变更 +1（`PartitionRegistration.java:261`）；
- `partitionEpoch`：分区配置变更次数。

这些记录随 metadata 一起被所有节点消费。broker 侧通过 `ReplicaManager.handleLeaderAndIsrRequest()`（`ReplicaManager.scala:1993`）接收 LeaderAndIsr 请求，把新 leader/ISR 应用到本地 Partition。

```text
Controller 决策
  → PartitionChangeRecord 写 Raft log
    → 各 broker 消费 metadata 更新
      → leader/ISR 变化的 broker 收到 LeaderAndIsr
        → 应用本地 Partition 状态
```

如果决策只改 controller 内存、不写记录，主链会先在哪出问题？controller 一旦切换或重启，所有分区归属全部丢失，且其他 broker 根本不知道有变更。所以"决策必须成为日志"是 KRaft 元数据模型的地基。

## 第六层：闭环的下半截——ISR 变化怎么从 broker 回报给 controller

选举和下发只是闭环的上半截。下半截是：**follower 追不上 leader、或追上了，这些 ISR 变化由谁上报？**

答案在 `AlterPartitionManager`（`AlterPartitionManager.scala`）。当 leader broker 发现自己 ISR 里的某个 follower 落后过多（超过 `replica.lag.time.max.ms`），它会先构造一个**候选的** LeaderAndIsr 变更，通过 `AlterPartitionRequest` 把这个变化提交给 controller（`AlterPartitionManager.submit`，`AlterPartitionManager.scala:116`）。也就是说，broker 可以提出"我建议把谁移出 ISR"，但**真正让 ISR 生效的权威仍然是 controller**。

Controller 收到 AlterPartition 后，校验（比如"ISR 必须包含当前 leader"），如果合法就更新 `PartitionRegistration`，并把更新后的记录写进日志、广播给所有 broker。

所以 ISR 的收缩/扩张是这样一个闭环：

```text
leader broker 检测到 follower 落后
  → 构造新的 ISR 候选（不是最终生效）
    → AlterPartition 提交给 controller
      → controller 校验并确认
        → 写记录 + 广播
          → 所有 broker 用新 ISR
```

如果 follower 落后只是 leader 本地知道、不回报 controller，主链会先在哪出问题？controller 对真实 ISR 一无所知，等这个 leader 也挂了、需要从 ISR 选新 leader 时，可能会选一个其实已经落后很多的 follower——丢数据。所以"ISR 变化必须回报并最终由 controller 确认"，是选举正确性的保障。

## 第七层：leaderEpoch——follower 判断"我该不该回退"的凭据

最后一个关键机制是 `leaderEpoch`。它的作用在 follower 侧最明显。

假设 broker 0 是 leader，leaderEpoch = 5。后来 broker 0 与集群断开，controller 把 leader 换成 broker 1，leaderEpoch 变成 6。再过一会，broker 0 恢复网络，但它不知道自己已经不是 leader 了，还想继续写。

controller 或新 leader 如何让 broker 0 收敛？答案是：**分区状态变更与数据面请求都会携带 leaderEpoch，但它们的校验落点并不完全相同。** controller 侧在处理分区状态变更时，会像 `ReplicationControlManager` 那样对 leaderEpoch 做大小比较；broker 侧在真正服务数据面请求时，旧 epoch 又会表现成 `FENCED_LEADER_EPOCH` 之类的错误。总之，broker 0 带着旧 epoch=5 来写，发现当前分区 leaderEpoch 已经是 6，就知道自己过时了，立即放弃领导权、变成 follower 并从新 leader 回退数据。

`PartitionRegistration.merge()` 里 `newLeaderEpoch = leaderEpoch + 1`（`PartitionRegistration.java:261`），保证每次 leader 变更 epoch 严格递增。它会在 controller/broker 的分区状态校验里被直接比较（比如 `ReplicationControlManager.java:1248` / `1260`），而在数据面请求上又进一步体现为 Kafka-4 里看到的 `FENCED_LEADER_EPOCH` 一类错误。

```text
leader 变更 → leaderEpoch + 1
  → 旧 leader 用旧 epoch 请求
    → 新 leader / controller 发现 epoch 小
      → 拒绝 / FENCED_LEADER_EPOCH
        → 旧 leader 收敛为 follower
```

如果不校验 leaderEpoch，旧 leader 在切换后继续写，主链会先在哪失败？新旧 leader 同时写同一分区，数据被覆盖且无人察觉。所以 leaderEpoch 不是"元数据编号"，而是**分区归属版本的校验凭据**——和 Kafka-6 里 generation/memberEpoch 的思路完全一致。

## 收网：分区归属 = Controller 单点决策 + Broker 心跳闭环

把整篇压成一句话：Kafka 分区的 leader 与 ISR 归属，不是 broker 之间选举产生的，而是由唯一的 active Controller 单点决策：Controller 通过 broker 心跳维护 broker 的 fenced/unfenced 状态，broker 进出时用 PartitionChangeBuilder 在 replica/ISR 集合内为受影响分区选新 leader，把结果写成元数据记录；broker 通过 LeaderAndIsr 应用归属，通过 AlterPartition 把 ISR 变化回报给 Controller 闭环；leaderEpoch 保证旧 leader 在切换后无法继续写。

```text
Controller（单线程，唯一权威）
  → broker 心跳 → fenced/unfenced
    → PartitionChangeBuilder 选 leader（PREFERRED/ONLINE/UNCLEAN）
      → PartitionChangeRecord 写 Raft log
        → LeaderAndIsr 下发到 broker
          → broker 应用归属
            → AlterPartition 回报 ISR 变化 → Controller 确认 → 广播
```

到这里，主线只发生了七件事。

第一，Controller 是唯一决策者，单线程事件模型保证决策不互相矛盾。

第二，Controller 用心跳 + 超时判断 broker 死活，超时即 fenced。

第三，broker 被 fenced 就从 ISR 移出并重选 leader。

第四，选 leader 有 PREFERRED/ONLINE/UNCLEAN 三档语义，对应负载均衡/不丢数据/可用性优先。

第五，每个决策都写成元数据记录并广播，不是只改内存。

第六，ISR 变化由 leader broker 通过 AlterPartition 回报，controller 最终确认。

第七，leaderEpoch 递增，让旧 leader 无法在切换后继续写。

**本篇的一句话困惑**：一个分区的 leader 挂了，谁来决定下一个 leader，怎么保证不会出现两个 leader？

**本篇的一句话顿悟**：分区归属由唯一的 active Controller 单点决策，不是 broker 之间选举；Controller 用心跳感知 broker 存活，用 PartitionChangeBuilder 选 leader，用 LeaderAndIsr/AlterPartition 下发与回报闭环，用 leaderEpoch 防止旧 leader 继续写。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **"分区 leader 是 broker 之间投票选的。"** 是 Controller 单点决策，不是分布式选举。
2. **"ISR 是 broker 自己协商确定的。"** 由 leader broker 通过 AlterPartition 回报，Controller 最终确认并广播。
3. **"fenced 的 broker 还能继续当 leader。"** Fenced 即被移出 ISR 并触发重选。
4. **"leaderEpoch 只是个元数据编号。"** 它是分区归属版本凭据，follower 据此判断是否回退，防止旧 leader 继续写。
5. **"KRaft 选 controller 和选分区 leader 是一回事。"** 前者决定谁当 Controller，后者由 Controller 决策。

### 关键证据清单

- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:931`：appendWriteEvent 单线程事件模型。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:145`：负责 ISR 与 leader 管理。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1554`：electLeader 决策入口。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:220`：electLeader 三种选举类型。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:231`：electPreferredLeader。
- `metadata/src/main/java/org/apache/kafka/controller/BrokerHeartbeatManager.java:399`：calculateNextBrokerState 状态机。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1670`：maybeFenceOneStaleBroker 逐个处理过期 broker。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1362`：handleBrokerFenced 触发 ISR 移出与重选。
- `metadata/src/main/java/org/apache/kafka/metadata/PartitionRegistration.java:157`：leader/isr/leaderEpoch 状态。
- `metadata/src/main/java/org/apache/kafka/metadata/PartitionRegistration.java:261`：newLeaderEpoch = leaderEpoch + 1。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1248`：controller 侧直接比较 leaderEpoch 大小。
- `core/src/main/scala/kafka/server/ReplicaManager.scala:1993`：handleLeaderAndIsrRequest 应用归属。
- `core/src/main/scala/kafka/server/AlterPartitionManager.scala:116`：submit 提交 ISR 变化回报。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 路线为基线（无 ZK，controller 由 KRaft 决定）。
- 本篇聚焦 Controller 决策与 broker 心跳驱动；不展开 KRaft Raft 实现（Kafka-8）、Partition 完整状态机（Kafka-9）、副本拉取细节。
- 本篇把 PREFERRED/ONLINE/UNCLEAN 视为选举语义，不展开全部 API 错误码。
- 不把"谁是 Controller"与"分区 leader 是谁"混成一个概念。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-4`（fetch 主链里的 FENCED_LEADER_EPOCH）、`Kafka-6`（ConsumerGroup 归属 = 组内层次，本篇 = 集群层次）。
- 后续桥接：下一篇可进入 `Kafka-8`，正面回答"谁是 active Controller"——KRaft 的 Raft 共识与元数据复制。