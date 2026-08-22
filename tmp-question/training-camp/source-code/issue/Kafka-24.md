# Kafka-24. 分区在 Controller 眼里是什么状态——PartitionRegistration、BrokersToIsrs 与 Epoch 状态机主链

> 场景：Kafka-7 讲了 Controller 如何决定分区的 leader 与 ISR 归属，但那时我们更多把 `PartitionRegistration` 当成一个“结果”来看。本篇换一个角度：一个分区在 Controller 的内存和元数据日志里到底是什么状态，broker 被 fenced 时 Controller 如何快速找到受影响分区，`PartitionChangeRecord` 如何经过 merge 推进状态，以及 ELR / LastKnownELR 如何参与 leader 候选。

## 先把真正的困惑摆出来：一个分区在 Controller 里存了什么

假设 broker 1 被 fenced。Controller 需要知道：

- broker 1 是哪些分区的 leader？
- broker 1 在哪些分区的 ISR 里？
- 这些分区当前的 replicas、leaderEpoch、partitionEpoch 是什么？
- ISR 为空时，ELR 或 LastKnownELR 是否提供了候选？

如果 Controller 没有高效的数据结构，它只能遍历全部 topic、全部 partition，逐个判断 broker 1 是否参与。Kafka 的答案是两种互补结构：

```text
PartitionRegistration：按分区保存完整状态
BrokersToIsrs：按 broker 反向定位受影响分区
```

*关键设计（斜体）：* *Controller 用 `PartitionRegistration` 维护每个分区的 leader/ISR/replicas/epochs，用 `BrokersToIsrs` 反向追踪 broker 参与的 ISR/leader 分区；`PartitionChangeBuilder` 根据当前状态生成 `PartitionChangeRecord`，再由 `PartitionRegistration.merge()` 按记录推进状态。*[模式: 正排状态 + 反向索引 + record replay]

## 第一层：PartitionRegistration 是 Controller 的分区正排状态

`PartitionRegistration` 是 Controller 侧的元数据对象，不是 broker 本地的 `Partition` 运行时对象。它描述的是“Controller 认为这个分区应该是什么状态”。核心字段包括：

- **`replicas`**：这个分区的完整副本集合；
- **`isr`**：当前在同步集合中的副本；
- **`leader`**：当前分区 leader；
- **`leaderEpoch`**：leader 归属版本；
- **`partitionEpoch`**：分区注册状态版本；
- **`addingReplicas` / `removingReplicas`**：重分配过程中的增删副本；
- **`elr`**：Eligible Leader Replicas，ISR 为空时可参与晋升的候选；
- **`lastKnownElr`**：上一次已知的 ELR 候选，用于特定恢复路径的兜底。

它不是单纯的 `leader + isr` 二元组。Controller 需要同时保存“谁属于这个分区”“谁当前同步”“谁拥有领导权”“这份状态处在哪个版本”“是否正在重分配”这些信息，才能生成下一条安全的变更记录。

## 第二层：BrokersToIsrs 不是简单 Set，而是带 leader 标记的反向索引

`BrokersToIsrs` 的类注释明确说，它把 broker 与其参与的 ISR 分区关联起来，并用于“从所有 ISR 移除 broker”或“把 broker 的所有 leader 迁走”。真实结构不是简单的：

```text
brokerId → Set<TopicIdPartition>
```

而是：

```text
brokerId
  → topic UUID
    → int[] partitionIds
      → 最高位记录该 broker 是否是该 partition 的 leader
```

源码使用 partition id 的最高 bit 作为 `LEADER_FLAG`，低 31 位保存 partition id（`BrokersToIsrs.java:44-49`）。因此这个索引同时支持两种查询：

- `partitionsWithBrokerInIsr(brokerId)`：broker 参与 ISR 的所有分区；
- leader-only 查询：broker 当前领导的分区。

当 broker 被 fenced 时，Controller 可以直接通过反向索引拿到受影响分区，而不是扫描全集：

```text
handleBrokerFenced(1)
  → partitionsWithBrokerInIsr(1)
    → 找到 broker 1 所在 ISR 的分区
      → 对这些分区构造状态变更
```

当 broker unfenced 时，`partitionsWithNoLeader()` 返回的是当前无 leader 的分区集合，Controller 会以这个集合为输入重新尝试选举；它不意味着每个无 leader 分区最终一定由刚恢复的 broker 接任。

## 第三层：PartitionChangeBuilder 把“状态变化”变成 record

Controller 不直接把 `PartitionRegistration` 的字段改完就算结束。状态变化先经过 `PartitionChangeBuilder`。

以 broker 被 fenced 为例，`ReplicationControlManager.handleBrokerFenced()` 会：

1. 从 `BrokersToIsrs` 找到该 broker 参与的分区；
2. 为每个分区创建 `PartitionChangeBuilder`；
3. 构造 `isAcceptableLeader`，排除被移除 broker；
4. 从目标 ISR 中移除被 fenced broker；
5. 根据 PREFERRED / ONLINE / UNCLEAN 规则尝试选 leader；
6. `build()` 生成 `PartitionChangeRecord`，或返回空表示无需变化。

```text
broker 状态变化
  → 反向索引找 affected partitions
    → PartitionChangeBuilder
      → 调整 target ISR
        → 选择 acceptable leader
          → build()
            → Optional<PartitionChangeRecord>
```

Builder 的价值在于，它把“当前状态、broker 活跃性、选举策略、minISR、ELR”集中到一次纯决策中，生成可写入 metadata log 的 record。record 是共识事实，Builder 只是生成候选变化。

## 第四层：PartitionRegistration.merge 是 record replay 的状态推进器

`PartitionChangeRecord` 写入元数据日志后，Controller 的状态重放会调用 `PartitionRegistration.merge(record)`。它不是简单替换整个对象，而是按 record 中哪些字段出现来合并：

```text
merge(record)
  → record.replicas 非空？替换 replicas
  → record.isr 非空？替换 isr
  → record.leader != NO_LEADER_CHANGE？leader 改变且 leaderEpoch +1
  → record.adding/removing 非空？更新重分配状态
  → partitionEpoch +1
  → 返回新的 PartitionRegistration
```

源码 `PartitionRegistration.java:256-262` 明确区分了 leader 是否真的变化：如果 record 的 leader 是 `NO_LEADER_CHANGE`，`leaderEpoch` 保持不变；否则才加 1。另一方面，`partitionEpoch + 1` 在 merge 返回新对象时始终推进。

这两个 epoch 的职责不同：

- `leaderEpoch`：保护 leader 归属，供 broker/follower 判断旧 leader 状态；
- `partitionEpoch`：保护整个分区注册状态的更新顺序，leader、ISR、replicas 或重分配字段变化都可能推进。

所以“收到一条 record”不等于“leader 一定换了”，但通常意味着 Controller 的分区注册状态版本推进了。

## 第五层：ELR 不是 ISR 的别名，LastKnownELR 也不是普通备份

### ELR：ISR 为空时的候选晋升路径

`PartitionChangeBuilder.isValidNewLeader()` 的规则是：

```text
targetIsr 中的副本可以当 leader
或
当 targetIsr 为空时，targetElr 中的副本可以当 leader
```

当 ELR 中的副本被选为 leader，`tryElection()` 会把它从 ELR 移到新的 ISR（`PartitionChangeBuilder.java:328-356`）。所以 ELR 不是“已经同步的副本”，而是 ISR 为空时仍允许参与晋升的候选集合。

### LastKnownELR：特定恢复策略的最后已知候选

`lastKnownElr` 走的是更窄的路径。`canElectLastKnownLeader()` 要求：

- ELR 功能开启；
- 使用 balanced recovery 的 last-known-leader 策略；
- 当前 target ISR 和 target ELR 都为空；
- `lastKnownElr` 恰好只有一个成员；
- 这个成员当前仍然 acceptable。

满足这些条件时，Controller 才会尝试从 `lastKnownElr` 选一个 unclean leader。它不是“ELR 的普通备份”，而是一个有严格前置条件的最后已知 leader 恢复路径。

因此，三者不能混为一谈：

```text
ISR：当前已同步、正常参与复制的集合
ELR：ISR 为空时可被晋升的候选集合
LastKnownELR：特定 balanced recovery 下的最后已知 leader 候选
```

## 第六层：无 leader 分区如何重新尝试选举

`BrokersToIsrs.partitionsWithNoLeader()` 只回答“哪些分区目前没有 leader”，不直接决定新 leader。

当 broker unfenced 时，`handleBrokerUnfenced()` 把这个无 leader 分区迭代器交给 `generateLeaderAndIsrUpdates()`，同时把新 broker 作为 `brokerToAdd` 传入 acceptable leader 判断。之后每个分区仍然要经过 `PartitionChangeBuilder.build()`，可能选中新 broker，也可能选择其他 ISR/ELR 候选，甚至暂时保持 `NO_LEADER`。

```text
broker unfenced
  → 找到无 leader 分区
    → 将新 broker 纳入 acceptable leader
      → 每个分区独立构建选举 record
        → 能选出才写 PartitionChangeRecord
```

所以反向索引负责缩小搜索范围，Builder 才负责最终决策，两者职责不能混成“索引直接选 leader”。

## 第七层：副本重分配如何进入同一套状态模型

PartitionRegistration 还保存 `addingReplicas` 和 `removingReplicas`。这意味着副本重分配不是瞬间替换 `replicas` 数组，而是存在过渡状态：

```text
旧 replicas
  → addingReplicas 记录新副本
    → 新副本追赶并满足加入条件
      → removingReplicas 移除旧副本
        → 形成新的稳定 replicas/isr
```

在这个阶段，`replicas`、`isr`、`addingReplicas`、`removingReplicas` 共同描述“当前状态 + 目标变化”。Controller 生成的 PartitionChangeRecord 只携带本次变化的字段，后续通过 merge 逐步推进。

这也是为什么 PartitionRegistration 不能被简化成一个“leader + ISR 数组”：重分配、ELR、lastKnownElr、leader recovery 和 epoch 都属于 Controller 必须持续维护的状态。

## 收网：PartitionRegistration 是状态，BrokersToIsrs 是索引，merge 是推进器

把整篇压成一句话：Controller 用 `PartitionRegistration` 存每个分区的正排状态，用真实的 `BrokersToIsrs` 反向索引快速找到 broker 参与的 ISR/leader 分区；broker 状态变化时，`PartitionChangeBuilder` 生成局部 `PartitionChangeRecord`，日志重放再由 `merge()` 按 leaderEpoch/partitionEpoch 推进；ELR、LastKnownELR 和 adding/removing replicas 则补齐无 leader与重分配边界。

```text
broker 变化
  → BrokersToIsrs 定位受影响分区
    → PartitionChangeBuilder 生成 record
      → 元数据日志
        → PartitionRegistration.merge()
          → leader/ISR/replicas/ELR/epoch 新状态
```

到这里，主线只发生了七件事。

第一，PartitionRegistration 是 Controller 的正排状态。

第二，BrokersToIsrs 是带 topic UUID、partition 数组和 leader flag 的反向索引。

第三，PartitionChangeBuilder 负责生成局部变更 record。

第四，merge 只在 leader 真变时推进 leaderEpoch，但每次合并都推进 partitionEpoch。

第五，ELR 是 ISR 为空时的候选晋升路径。

第六，LastKnownELR 是带严格条件的最后已知 leader 恢复路径。

第七，无 leader 和副本重分配都要经过 Builder 生成记录，不能由索引直接决定。

**本篇的一句话困惑**：一个分区在 Controller 里存了什么，怎么快速定位哪个 broker 影响了哪些分区？

**本篇的一句话顿悟**：Controller 用 PartitionRegistration 存正排状态，用 BrokersToIsrs 存带 leader 标记的反向索引；broker 变化时通过索引定位分区，用 PartitionChangeBuilder 生成 record，再由 merge 推进完整状态机。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“PartitionRegistration 就是 broker 侧的 Partition。”** Controller 侧是注册状态，broker 侧是运行时状态。
2. **“BrokersToIsrs 就是 brokerId 到 Set 的简单映射。”** 实际是 broker → topic UUID → partition 数组，并用最高位记录 leader flag。
3. **“ELR 和 LastKnownELR 都是 ISR 的备份。”** ELR 是 ISR 为空时的候选晋升路径，LastKnownELR 是特殊恢复策略下的最后已知 leader 候选。
4. **“merge 只在 leader 变更时调用。”** 每次收到 PartitionChangeRecord 都调用。
5. **“partitionsWithNoLeader 会直接指定新 leader。”** 它只提供无 leader 分区集合，最终仍由 PartitionChangeBuilder 重新选举。

### 关键证据清单

- `metadata/src/main/java/org/apache/kafka/metadata/PartitionRegistration.java:39`：PartitionRegistration 字段。
- `metadata/src/main/java/org/apache/kafka/metadata/PartitionRegistration.java:240`：merge()。
- `metadata/src/main/java/org/apache/kafka/controller/BrokersToIsrs.java:36`：反向索引职责。
- `metadata/src/main/java/org/apache/kafka/controller/BrokersToIsrs.java:44`：topic map、partition array、leader flag 编码。
- `metadata/src/main/java/org/apache/kafka/controller/BrokersToIsrs.java:281`：partitionsWithBrokerInIsr。
- `metadata/src/main/java/org/apache/kafka/controller/BrokersToIsrs.java:277`：partitionsWithNoLeader。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:322`：ISR/ELR acceptable leader。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:290`：LastKnownELR 选举前置条件。
- `metadata/src/main/java/org/apache/kafka/controller/PartitionChangeBuilder.java:328`：ELR 晋升为 leader 后加入 ISR。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1362`：handleBrokerFenced。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1407`：handleBrokerUnfenced。
- `metadata/src/main/java/org/apache/kafka/controller/ReplicationControlManager.java:1974`：generateLeaderAndIsrUpdates。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 Controller 侧分区状态模型，不展开 broker 侧 Partition 运行时状态机（Kafka-9）。
- 不展开完整副本重分配协议，只说明 adding/removing replicas 如何进入注册状态。
- 不把 ELR、LastKnownELR 与普通 ISR 混成同一层。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-7`（Controller 归属决策）、`Kafka-8`（KRaft 元数据日志）、`Kafka-9`（broker 侧 Partition 状态）。
- 后续桥接：下一篇可进入 K-7 Controller 域第 3 篇（副本状态机与 leader 选举/重分配），或切到其他域。