# Kafka-23 重写规划

> 题目：ModernGroup 怎么靠一个心跳完成所有事——KIP-848 单心跳协议与增量重平衡主链
> 状态：K-6 ConsumerGroup 域第 3 篇，按"ModernGroup/KIP-848"展开
> 目标：解释 KIP-848 的 ModernGroup 协议如何用单一 `ConsumerGroupHeartbeat` 请求替代 Classic 的四步：成员加入、分配接收、心跳维持、离开都在一个请求内完成。核心覆盖 `ConsumerGroupHeartbeatRequest` 的字段语义、`groupEpoch`/`assignmentEpoch`/`memberEpoch` 三层 epoch 的意义、`TargetAssignmentBuilder` 只对受影响成员重新分配、`CurrentAssignmentBuilder` 的成员状态机（STABLE→UNREVOKED_PARTITIONS→UNRELEASED_PARTITIONS→STABLE）以及增量重平衡如何避免 stop-the-world。

## 1. 读者困惑

- KIP-848 说"一个请求替代四步"，怎么做到的？
- 为什么 ModernGroup 需要三个 epoch：`groupEpoch`、`assignmentEpoch`、`memberEpoch`？
- 增量重平衡的"增量"到底增量在哪，协调器怎么知道哪些成员需要调整？
- 成员在撤销分区时如果还没撤完，怎么处理？
- `ConsumerGroupHeartbeatRequest` 里的 `topicPartitions` 字段是做什么的？
- `TargetAssignmentBuilder` 和 `CurrentAssignmentBuilder` 各负责什么？

## 2. 一句话顿悟

**ModernGroup（KIP-848）用单一 `ConsumerGroupHeartbeat` 请求完成加入、分配、回执、维持：协调器根据 `memberEpoch` 判断状态（0=加入，>0=活跃，-1/-2=离开），用 `groupEpoch`/`assignmentEpoch` 跟踪全局版本，`TargetAssignmentBuilder` 只对受影响成员重新分配，`CurrentAssignmentBuilder` 逐成员推进状态机（STABLE→UNREVOKED_PARTITIONS→UNRELEASED_PARTITIONS→STABLE），实现增量重平衡——只有需要撤销或新增分区的成员才参与，其余成员不受影响。**

## 3. 五要素卡片

### 读者问题

一个 5 成员组，新成员加入后，为什么只有 2 个成员需要调整分区，其余 3 个完全不动？

### 入口

- `ConsumerGroupHeartbeatRequest`：单心跳请求
- `GroupMetadataManager.consumerGroupHeartbeat`：心跳处理入口
- `ConsumerGroup`：Modern 组（server）
- `ConsumerGroupMember`：成员状态
- `TargetAssignmentBuilder`：全局目标分配计算
- `CurrentAssignmentBuilder`：逐成员分配推进
- `MemberState`：`STABLE` / `UNREVOKED_PARTITIONS` / `UNRELEASED_PARTITIONS` / `UNKNOWN`

### 状态核心

- `groupEpoch`：组版本（订阅变化时 +1）
- `assignmentEpoch`：目标分配版本（分配变化时 = groupEpoch）
- `memberEpoch`：成员版本（协调器维护，单调递增）
- `hasAssignedPartitionsChanged`：比较新旧分配决定是否受影响的判断
- 返回 assignment 的时机：`memberEpoch == 0 || isFullRequest || hasAssignedPartitionsChanged`

### 失败路径

- 只发一次心跳但没收到 assignment → 下次心跳重试，不用重新加入
- 成员撤销分区超时 → 协调器超时后 fence 该成员
- 成员 epoch 与协调器不一致 → FENCED_MEMBER_EPOCH，重新加入
- 增量计算不正确 → 分区被遗漏或重复分配

### 连接点

- 前文 `Kafka-6`：ConsumerGroup 双协议总览。
- 前文 `Kafka-22`：ClassicGroup 四步协议，本篇是 Modern 对比。
- 前文 `Kafka-9`：ISR 扩缩（类似增量调整），与增量重平衡的增量概念呼应。

## 4. 总图

```text
ConsumerGroupHeartbeat（memberEpoch=0，首次加入）
  → 协调器创建成员，计算 target assignment
    → 返回 assignment + memberEpoch=1
      → 后续心跳带 memberEpoch 和 owned partitions（回执）

新成员 C4 加入
  → 协调器重算 target assignment
    → 只 C1（让出分区）、C4（接手）受影响
      → 只向 C1、C4 回复新 assignment
        → C2、C3 继续消费，不动
```

## 5. 关键边界

- 本篇只讲 ModernGroup 协议，不重复 ClassicGroup 的对比细节（Kafka-6/22）。
- 不把 groupEpoch 与 memberEpoch 混成同一概念：前者全局，后者逐成员。
- 不展开 UniformAssignor 等分配算法细节，只讲 TargetAssignmentBuilder 的职责。
- 不展开 ConsumerGroupHeartbeatRequest 全部字段，只讲核心语义。

## 6. 失败方案推演

1. **每次心跳全量分配所有成员**：退回 Classic 效果，增量优势消失。
2. **没有 epoch 校验**：旧成员在分配变化后仍按旧归属消费，重复。
3. **撤销分区无限等待**：超时后 fence 该成员，否则阻塞。
4. **只靠 groupEpoch 不靠 memberEpoch**：无法逐成员跟踪归属版本。

## 7. 误解清单

- "groupEpoch 就是 memberEpoch"：前者全局，后者逐成员。
- "ModernGroup 也有四步"：只有一个请求，但内部有状态机。
- "增量是说"不重平衡"：仍然重平衡，但只影响部分成员。
- "heartbeat 只是心跳"：它还携带 owned partitions 作为回执。
- "assignment 只返回一次"：每次有变化或首次加入才返回。

## 8. 证据清单

- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroup.java:83`：ConsumerGroup 类。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:2186`：consumerGroupHeartbeat 入口。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:2292`：updateTargetAssignment。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:2309`：maybeReconcile。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroupMember.java:58`：MemberState 初始值。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/CurrentAssignmentBuilder.java`：成员分配推进。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroup.java:905`：groupEpoch > targetAssignmentEpoch 判断。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/TargetAssignmentBuilder.java:46`：只记录分配变化的成员。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/ModernGroupMember.java:183`：hasAssignedPartitionsChanged。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:2334`：返回 assignment 的触发条件。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`，group-coordinator 模块。
- 本篇聚焦 ModernGroup 单心跳协议与增量重平衡，不展开 UniformAssignor 算法细节。
- 目标正文：7000~11000 字。

## 10. 本轮重写主线

1. 从"一个请求替四步"开场。
2. 否定"全量分配"和"没有 epoch"两种方案。
3. 解释三个 epoch 的语义。
4. 解释 consumerGroupHeartbeat 处理流程。
5. 解释 TargetAssignmentBuilder 只改受影响成员。
6. 解释 CurrentAssignmentBuilder 成员状态机。
7. 解释 返回 assignment 的触发条件与回执。
8. 收网：单心跳 + 三层 epoch + 增量重平衡。