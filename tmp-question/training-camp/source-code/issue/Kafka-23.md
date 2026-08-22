# Kafka-23. ModernGroup 怎么靠一个心跳完成所有事——KIP-848 单心跳协议与增量重平衡主链

> 场景：Kafka-22 把 ClassicGroup 的四步协议讲透了。但 Kafka 还有另一套 ModernGroup（KIP-848），它把 JoinGroup→SyncGroup→Heartbeat→LeaveGroup 压成了**一个请求**：`ConsumerGroupHeartbeat`。本篇讲清楚这个请求怎么做到"一个当四个用"，以及增量重平衡为什么只影响部分成员。这是 K-6 ConsumerGroup 域第 3 篇。

## 先把真正的困惑摆出来：一个请求怎么替代四个

ClassicGroup 需要四个显式请求、一个完整的状态机、一个代数递增才能完成一次重平衡。ModernGroup 只用了一个心跳请求。

它怎么做到的？答案是：**这个心跳请求携带了足够的信息，让协调器在一次交互中知道"你是谁、你处于哪一代、你拥有哪些分区、你想订阅什么"。**

```text
ConsumerGroupHeartbeatRequest
  → groupId, memberId, memberEpoch（始终携带）
    → subscribedTopicNames / regex（加入时/变化时）
      → topicPartitions（回执：本成员已接受的 owned 分区）
        → serverAssignor（选择分配器）
```

协调器收到这个请求后，不需要四步，直接就能决定：该成员的归属是否需要调整。需要调整，就返回新 assignment；不需要，就只返回心跳间隔。

*关键设计（斜体）：* *ModernGroup（KIP-848）用单一 `ConsumerGroupHeartbeat` 请求完成加入、分配、回执、维持：协调器用 `memberEpoch` 判断状态（0=加入，>0=活跃，-1/-2=离开），用 `groupEpoch`/`assignmentEpoch` 跟踪全局版本，`TargetAssignmentBuilder` 只对受影响成员重新分配，`CurrentAssignmentBuilder` 逐成员推进状态机，实现增量重平衡。*[模式: 单心跳 + 三层 epoch + 增量分配]

## 第一层：三个 epoch 各管各的

ModernGroup 不是只有一个 generation，而是三个 epoch：

- **`groupEpoch`**：组的版本。每次成员订阅变化或正则变化时 +1。首次创建时从 0 bump 到 1。这是全局版本号。
- **`assignmentEpoch`**：目标分配版本。当 `groupEpoch > assignmentEpoch` 时，需要重新计算分配。`assignmentEpoch` 被推进到与 `groupEpoch` 一致（`ConsumerGroup.java:905`）。
- **`memberEpoch`**：单个成员的版本。协调器维护，代表该成员当前归属的"代次"。成员首次加入时 epoch=0，协调器返回 >0；成员一直递增直到关闭。

这三个 epoch 的关系是：`groupEpoch` 驱动全局版本，`assignmentEpoch` 驱动分配计算，`memberEpoch` 驱动每个成员的归属版本。

为什么需要三个而不只是一个？因为：

- `groupEpoch` 变化了，只说明"组的元数据可能变了"，但不一定需要重新分配（比如只改配置）；
- `assignmentEpoch` 只在分配实际需要变化时才推进；
- `memberEpoch` 逐成员跟踪，只有该成员归属变化时才递增。

## 第二层：consumerGroupHeartbeat 的处理流程

`GroupMetadataManager.consumerGroupHeartbeat()` 是 ModernGroup 的唯一入口（`GroupMetadataManager.java:2186`）。它处理心跳时，先检查 `memberEpoch`：

- **-1 或 -2**：走 leave 路径（`LEAVE_GROUP_MEMBER_EPOCH` / `LEAVE_GROUP_STATIC_MEMBER_EPOCH`）；
- **0**：首次加入，创建或找回成员，处理订阅；
- **>0**：正常心跳，校验 epoch 和 owned 分区。

对于正常心跳，流程是：

```text
consumerGroupHeartbeat()
  → 校验 epoch+owned 分区（throwIfConsumerGroupMemberEpochIsInvalid）
    → 更新成员信息（订阅/正则/assignor）
      → 如果订阅变化，groupEpoch++
        → 如果 groupEpoch > assignmentEpoch
          → updateTargetAssignment（重新计算分配）
            → maybeReconcile（推进成员状态）
              → 返回响应
```

每一步都在同一个请求里完成，不需要多次往返。

## 第三层：TargetAssignmentBuilder 只对受影响成员生成记录

增量重平衡的核心在 `TargetAssignmentBuilder`。它的类注释说得很清楚：**"Records are only created for members which have a new target assignment. If their assignment did not change, no new record is needed."**（`TargetAssignmentBuilder.java:46`）。

也就是说，当协调器需要重新计算分配时，它基于当前所有成员和他们的订阅，用 assignor 算出一份新分配。但只有那些在新分配中**分区集合变化了**的成员，才会被写入新的目标分配记录。其他成员的分区归属不变，也就不需要写任何记录。

```text
当前 target assignment: C1→{0,1}, C2→{2,3}, C3→{4,5}
新成员 C4 加入
  → 重算：C1→{0}, C2→{2,3}, C3→{4,5}, C4→{1}
    → C1 和 C4 分配变了 → 写记录
      → C2、C3 不变 → 不写记录
        → 协调器只向 C1、C4 返回新 assignment
```

这就是"增量"的确切含义：**不是不重平衡，而是把重平衡的 IO 和状态变更范围缩小到"受影响成员"。**

## 第四层：hasAssignedPartitionsChanged 决定响应里是否带 assignment

协调器不是每次心跳都返回 assignment。`GroupMetadataManager.java:2334` 的触发条件：

```text
if (memberEpoch == 0 || isFullRequest || hasAssignedPartitionsChanged(member, updatedMember))
  → 响应里带 assignment
```

`hasAssignedPartitionsChanged`（`ModernGroupMember.java:183`）直接比较两个成员的 `assignedPartitions` 是否相等：

```java
public static boolean hasAssignedPartitionsChanged(
    ModernGroupMember member1, ModernGroupMember member2
) {
    return !member1.assignedPartitions().equals(member2.assignedPartitions());
}
```

所以只有**当前成员与更新后的成员分配不一样**时，响应里才带 assignment。这保证了稳定成员的每次心跳都不带无谓的分配信息。

## 第五层：CurrentAssignmentBuilder 推进成员状态机

成员不是"收到新分配就立刻生效"的。`CurrentAssignmentBuilder` 负责逐成员推进状态：

- **STABLE**：稳定状态。如果 `memberEpoch != targetAssignmentEpoch`，说明有更新分配，进入重算。
- **UNREVOKED_PARTITIONS**：等待成员撤销确认。成员在心跳里通过 `topicPartitions` 报回当前拥有的分区，协调器检查是否还有应撤销的分区未撤销。如果全部撤销了，进入下一步。
- **UNRELEASED_PARTITIONS**：成员当前拥有的一些分区尚不可用（target assignment 里这些分区还没分配给任何成员），协调器在推进到最新分配的同时，等待这些 unreleased 分区可用。
- **UNKNOWN**：未知状态，fence 该成员。

状态不是一条单链，而是由"要不要撤销、要不要新增、有没有 unreleased 分区"三条件决定的**多出口判断**：

```text
STABLE 且 epoch 落后
  → 有应撤销分区（新分配不再拥有）？
    → 是：请求撤销 → UNREVOKED_PARTITIONS
      → 成员确认撤销完 → 继续重算
  → 有新分区要接收？
    → 是：加入 assigned 分区
      → 有 unreleased 分区？→ UNRELEASED_PARTITIONS
        → 否则 → STABLE
  → 无撤销也无新增，但有 unreleased 分区？
    → UNRELEASED_PARTITIONS（等分区可用）
  → 否则 → STABLE（目标 epoch 生效）
```

这个多出口状态机保证了成员在归属切换过程中，不会出现"旧分区还没撤完就开始消费新分区"的中间态。

## 第六层：回执——owned partitions 的 echo

成员收到新分配后，不是立刻信任协调器说"你已经有了"，而是通过下次心跳里的 `topicPartitions` 字段回显它当前拥有的分区。协调器在 `maybeReconcile` 中比较这个回显与目标分配，决定成员是否已经真正接受了新归属。

这就是 `ConsumerHeartbeatRequestManager.HeartbeatState.buildRequestData()` 里 `topicPartitions` 字段的语义：**不在任何时候都发，只在加入或本地 assignment 变化后发**，作为对协调器的回执。

如果成员的 `topicPartitions` 回执与目标分配不一致，协调器会继续推进成员状态（UNREVOKED_PARTITIONS 等），直到双方一致。

## 收网：单心跳 + 三层 epoch + 增量重平衡

把整篇压成一句话：ModernGroup（KIP-848）用单一 `ConsumerGroupHeartbeat` 请求完成加入、分配、回执、维持；`groupEpoch`/`assignmentEpoch`/`memberEpoch` 三层 epoch 分别跟踪全局版本、分配版本、成员版本；`TargetAssignmentBuilder` 只对受影响成员生成新记录，`hasAssignedPartitionsChanged` 决定响应里是否带 assignment；`CurrentAssignmentBuilder` 通过 STABLE→UNREVOKED_PARTITIONS→UNRELEASED_PARTITIONS 状态机保证归属切换安全。

```text
ConsumerGroupHeartbeat
  → memberEpoch==0？加入 + 首次分配
    → memberEpoch>0？正常心跳
      → groupEpoch > assignmentEpoch？
        → 重算分配（TargetAssignmentBuilder）
          → 只改受影响成员
            → 返回 assignment（仅对变化成员）
              → 成员回执 topicPartitions（下次心跳）
```

到这里，主线只发生了六件事。

第一，三个 epoch 分别跟踪组、分配、成员版本。

第二，consumerGroupHeartbeat 一个请求完成所有交互。

第三，TargetAssignmentBuilder 只对受影响成员生成记录。

第四，hasAssignedPartitionsChanged 决定响应是否带 assignment。

第五，CurrentAssignmentBuilder 状态机保证切换安全。

第六，topicPartitions 回执为协调器提供成员已接受归属的确认。

**本篇的一句话困惑**：一个请求怎么替代四个，增量到底增在哪？

**本篇的一句话顿悟**：ModernGroup 用单一心跳携带 memberEpoch 和 owned 分区回执，协调器用三层 epoch 和 TargetAssignmentBuilder 只对受影响成员调整分配，用 CurrentAssignmentBuilder 状态机保证切换安全——其余成员完全不受影响。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“groupEpoch 就是 memberEpoch。”** 前者全局，后者逐成员。
2. **“ModernGroup 也有四步。”** 只有一个请求，但内部有状态机。
3. **“增量 = 不重平衡。”** 仍然重平衡，但只影响部分成员。
4. **“heartbeat 只是心跳。”** 它还携带 owned partitions 作为回执。
5. **“assignment 每次心跳都返回。”** 只有首次加入、全量请求、或分配变化时才返回。

### 关键证据清单

- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroup.java:83`：ConsumerGroup 类。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:2186`：consumerGroupHeartbeat 入口。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:2292`：updateTargetAssignment。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:2334`：返回 assignment 的触发条件。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroupMember.java:58`：MemberState 初始值。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/CurrentAssignmentBuilder.java`：成员分配推进。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/CurrentAssignmentBuilder.java:276`：computeNextAssignment 三条件多出口。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/TargetAssignmentBuilder.java:46`：只记录分配变化的成员。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/ModernGroupMember.java:183`：hasAssignedPartitionsChanged。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroup.java:905`：groupEpoch > targetAssignmentEpoch 判断。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线，group-coordinator 模块。
- 本篇聚焦 ModernGroup 单心跳协议与增量重平衡，不展开 UniformAssignor 算法细节。
- 不把 groupEpoch 与 memberEpoch 混成同一概念。
- 不把 ModernGroup 的 CurrentAssignmentBuilder 状态机与 ClassicGroup 的 ClassicGroupState 混成同一层。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-6`（ConsumerGroup 双协议总览）、`Kafka-22`（ClassicGroup 四步对比）。
- 后续桥接：K-6 ConsumerGroup 域 3 篇收官。下一篇可进入 K-7 Controller 域第 2 篇（分区状态机），或切到其他域。