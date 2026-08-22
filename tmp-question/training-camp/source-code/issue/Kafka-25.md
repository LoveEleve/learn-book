# Kafka-25. 副本怎么迁、leader 怎么换——Controller 的 broker 状态机、leader 选举与 reassignment 主链

> 场景：Kafka-24 已经把 Controller 眼中的分区状态结构讲清了：`PartitionRegistration`、`BrokersToIsrs`、`PartitionChangeBuilder`。本篇继续往前走一步：**这些结构在 broker 状态变化时是怎么用起来的？** 一个 broker 被 fenced、恢复、优雅关机时，Controller 怎么迁 leader、改 ISR？这就是 K-7 Controller 域第 3 篇。

## 先把真正的困惑摆出来：broker 要下线，leader 怎么迁走？

想象一个 broker 正在领导若干分区。现在它要下线了：

- 如果直接停机，所有这些分区都会短暂失去 leader；
- 如果等它自己慢慢退出，leader 迁移又可能太慢；
- 如果随便选一个副本顶上，可能选到不在 ISR 的副本，造成数据丢失。

所以 Controller 不能简单做“broker 死了就换 leader”，而是要根据 broker 当前状态（fenced、unfenced、controlled shutdown）决定用什么策略：

```text
broker 变化
  → 找受影响分区
    → 为这些分区生成新的 leader / ISR 记录
      → 写入 metadata log
        → broker 收到 LeaderAndIsrRequest 执行
```

*关键设计（斜体）：* *Controller 把 broker 侧的变化（fenced/unfenced/shutdown）转成“需要重算的分区集合”，再用 `PartitionChangeBuilder` 为每个受影响分区生成 leader/ISR 变更记录；如果是手工 electLeaders 则按 PREFERRED/ONLINE/UNCLEAN 三种语义选 leader；如果是 reassignment 则通过 addingReplicas/removingReplicas 过渡到新副本集合。*[模式: broker 状态机 + 局部重算 + 渐进迁移]

## 第一层：BrokerHeartbeatManager 先定义 broker 的状态

Controller 视角下，一个 broker 只有几种关键状态：

- **FENCED**：被隔离，不可参与服务；
- **UNFENCED**：恢复可用；
- **CONTROLLED_SHUTDOWN**：正在温和下线，需要先迁走 leader；
- **SHUTDOWN_NOW**：可以直接关机。

`BrokerHeartbeatManager.calculateNextBrokerState()` 根据心跳与元数据进度，算出 broker 的下一个状态（`BrokerHeartbeatManager.java:399`）。Controller 接着在 `ReplicationControlManager` 里走三条主要路径：

- `handleBrokerFenced`
- `handleBrokerUnfenced`
- `handleBrokerInControlledShutdown`

这三条路径不是形式上的“同名方法”，而是三类完全不同的迁移策略。

## 第二层：handleBrokerFenced——立刻移出 ISR，必要时重选 leader

当 broker 被 fenced 时，Controller 的逻辑最简单也最强硬：它已经不可信了，必须立刻从 ISR 里踢出去。

`ReplicationControlManager.handleBrokerFenced()`（`ReplicationControlManager.java:1362`）会：

1. 从 `BrokersToIsrs` 反向索引中找到所有包含该 broker 的分区；
2. 对每个分区调用 `generateLeaderAndIsrUpdates()`；
3. 在 `PartitionChangeBuilder` 里把该 broker 从 target ISR 中排除；
4. 如果它还是当前 leader，就用当前 ISR/ELR/LastKnownELR 规则重新选 leader。

```text
handleBrokerFenced(brokerId)
  → partitionsWithBrokerInIsr(brokerId)
    → PartitionChangeBuilder(targetIsr 去掉该 broker)
      → 如果 leader 被移走 → 重新选 leader
        → 写 PartitionChangeRecord
```

fenced 的语义是“快速断舍离”：不等这个 broker 优雅退出，也不给它缓冲，先从 ISR 和 leader 视角中清掉。

## 第三层：handleBrokerUnfenced——恢复只是加入候选，不保证你一定接 leader

反过来，broker 从 FENCED 恢复到 UNFENCED 时，逻辑没那么直接。它恢复可用，只意味着它**可以参与选举了**，不意味着它一定要接管 leader。

`handleBrokerUnfenced()`（`ReplicationControlManager.java:1407`）的关键点是：

- 它只对 `partitionsWithNoLeader()` 返回的分区做处理；
- 在 `generateLeaderAndIsrUpdates()` 里，把这个刚恢复的 broker 作为 acceptable leader 候选；
- 最终每个分区还是要经过 `PartitionChangeBuilder.build()` 判断，能不能真的成为 leader。

所以 `partitionsWithNoLeader()` 只是“当前无 leader 的分区集合”，不是“这个 broker 要接管的清单”。它只是缩小搜索范围，真正的 leader 仍由选举逻辑决定。

## 第四层：handleBrokerInControlledShutdown——先迁 leader，再允许停机

`handleBrokerInControlledShutdown()`（`ReplicationControlManager.java:1427`）对应的是温和下线。

与 fenced 最大的区别在于：

- fenced 是“立刻不可信”，直接移出 ISR；
- controlled shutdown 是“我准备下线，但希望尽量平滑”，所以 Controller 要先把该 broker 当前领导的分区迁走，再允许它停机。

`BrokerHeartbeatManager` 会把 broker 状态推进到 `CONTROLLED_SHUTDOWN`，而 `handleBrokerInControlledShutdown()` 负责调用 `generateLeaderAndIsrUpdates()`，把这个 broker 持有的 leader 角色尽量转移出去。

只有等所有领导权都迁走，broker 才会被允许进入 `SHUTDOWN_NOW`。

## 第五层：三档选举——PREFERRED / ONLINE / UNCLEAN

真正决定“换给谁”的是 `electLeader()`（`ReplicationControlManager.java:1554`）和 `PartitionChangeBuilder` 的 `Election` 类型。

### PREFERRED

优先选择预分配的 preferred replica（通常是 replicas 列表的第一个）。如果它在 ISR 中且可用，就选它；否则退化到在线的副本。

### ONLINE

只在 ISR（或 ISR 为空时 ELR）里选 leader，不会越过同步边界。它强调不丢数据。

### UNCLEAN

当 ISR 为空、ELR/LastKnownELR 都不足以恢复服务时，才会允许从 non-ISR 的 acceptable replica 中选 leader。这可能导致数据丢失，所以默认要显式开启 `unclean.leader.election.enable`。

所以 Controller 的 leader 选举不是只有一种模式，而是根据场景在 PREFERRED / ONLINE / UNCLEAN 之间选择最稳妥的一档。

## 第六层：reassignment——不是一次性替换 replicas，而是过渡状态推进

分区重分配（reassignment）不是“把旧 replicas 列表换成新 replicas 列表”这么简单。如果这样做，新副本还没追上日志就被当成正式副本，ISR 和 HW 会立刻失真。

`changePartitionReassignment()`（`ReplicationControlManager.java:2184`）通过 `PartitionReassignmentReplicas` 把新旧副本集合拆成三份：

- **目标 replicas**：最终想要的副本集合；
- **addingReplicas**：新加入但尚未追平的副本；
- **removingReplicas**：待移除但还未完全安全退出的副本。

Controller 生成的记录不会一口气把 replicas 换完，而是让 partition 先进入"adding/removing"过渡态，等新副本追平、旧副本退出条件满足后，再推进到新的稳定状态。

```text
old replicas
  → addingReplicas（新副本开始追）
    → 新副本追平并进入 ISR
      → removingReplicas（旧副本安全退出）
        → new stable replicas
```

这也是为什么 Kafka-24 里 `PartitionRegistration` 要同时保存 `replicas`、`addingReplicas`、`removingReplicas`：Controller 眼中的分区并不是“只会瞬时切换”，而是一个渐进迁移的状态机。

## 第七层：为何 PREFERRED、ONLINE、UNCLEAN 与 reassignment 必须放在一起看

leader 选举与 reassignment 看起来像两件事，实际耦合很深：

- reassignment 过程中，leader 可能需要重新选择；
- broker fenced/unfenced/shutdown 时，reassignment 也可能被打断；
- `PartitionChangeBuilder` 既要考虑当前 ISR，又要考虑 adding/removing replicas；
- 如果开启 unclean election，它甚至可能在 reassignment 未完成时选到非 ISR 的副本。

所以在 Controller 视角下，leader 选举和 reassignment 不是分开的两个模块，而是同一条“根据 broker 状态变化生成 PartitionChangeRecord”的流水线里的两种决策情景。

## 收网：broker 状态变化 → 分区集合 → 变更记录 → broker 执行

把整篇压成一句话：Controller 先根据 broker 状态机（fenced/unfenced/controlled shutdown）决定哪些分区受影响，再通过 `PartitionChangeBuilder` 生成新的 leader/ISR/replicas 变更记录；PREFERRED/ONLINE/UNCLEAN 三种选举策略决定 leader 该怎么换，`changePartitionReassignment()` 决定副本该怎么渐进迁移，最后一切都落成 `PartitionChangeRecord` 写入日志，由 broker 侧执行。

```text
broker 状态变化
  → 找受影响分区
    → 选举 / reassignment 计算
      → PartitionChangeRecord
        → metadata log
          → broker 收到 LeaderAndIsr / state update 执行
```

到这里，主线只发生了六件事。

第一，BrokerHeartbeatManager 定义了 broker 的状态机。

第二，handleBrokerFenced 立刻移出 ISR，必要时重选 leader。

第三，handleBrokerUnfenced 只是把 broker 纳入候选，不保证一定接 leader。

第四，controlled shutdown 先迁 leader，再允许停机。

第五，PREFERRED / ONLINE / UNCLEAN 三档选举决定 leader 如何换。

第六，reassignment 通过 adding/removing replicas 渐进推进，而不是一次性替换。

**本篇的一句话困惑**：broker 变化时，Controller 到底怎么决定 leader 怎么换、副本怎么迁？

**本篇的一句话顿悟**：Controller 先把 broker 状态变化映射成受影响分区集合，再用 PartitionChangeBuilder 和 reassignment 逻辑生成变更记录；选举策略决定 leader 怎么换，adding/removing replicas 决定副本怎么渐进迁。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“broker 恢复后一定会接管所有无 leader 分区。”** 恢复只是把它加入 acceptable leader 候选。
2. **“controlled shutdown 和 fencing 一样。”** 前者是温和迁移，后者是立刻移出 ISR。
3. **“reassignment 一次就完成。”** 要通过 adding/removing replicas 过渡。
4. **“unclean election 只是另一种选举风格。”** 它可能导致数据丢失。
5. **“electLeaders 只给人工调用。”** 内部也有自动触发的选举路径。

### 关键证据清单

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

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 broker 状态变化、选举、reassignment，不展开 broker 侧 Partition 执行细节（Kafka-9）。
- 不把 PREFERRED / ONLINE / UNCLEAN 混成一种选举语义。
- 不展开 ELR/LastKnownELR 的字段细节（Kafka-24 已讲）。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-7`（Controller 归属决策）、`Kafka-24`（分区状态数据结构）、`Kafka-9`（broker 侧 Partition 运行时状态）。
- 后续桥接：K-7 Controller 域 3 篇收官。下一步可进入 K-8 KRaft 域第 2 篇（QuorumController 与元数据日志），或切到其他域。