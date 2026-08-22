# Kafka-22. ClassicGroup 的完整一生——JoinGroup→SyncGroup→Heartbeat→LeaveGroup 四步协议主链

> 场景：Kafka-6 讲了 ConsumerGroup 双协议并存，Classic 和 Modern 都提了一笔。本篇把 ClassicGroup 这一条彻底讲透：一个 3 成员 group 从创建到稳定消费，再到成员退出，经过哪四个请求、经历了哪些状态、generation 怎么递增。这是 K-6 ConsumerGroup 域第 2 篇。

## 先把真正的困惑摆出来：一个 group 从无到有再到有人离开，经历了哪些事

假设 3 个消费进程要用同一个 groupId 订阅一个 topic。从它们第一次启动，到稳定消费，再到其中一个退出，中间到底发生了什么？

如果只用一句话回答，就是 ClassicGroup 的四步协议：

```text
JoinGroup  → 收集成员，选 leader，产生一代 generation
SyncGroup  → leader 算好分配，广播给所有人
Heartbeat  → 稳定期维持存活
LeaveGroup → 成员主动退出，触发重平衡
```

但这四步背后还有一整套状态机在驱动。这篇沿着状态机把每一步讲清楚：什么时候进 PREPARING_REBALANCE、什么时候 generation+1、什么时候进 STABLE。

*关键设计（斜体）：* *ClassicGroup 用四个显式请求协商"成员归属"：JoinGroup 收集成员、选出 leader、bump generation；SyncGroup 让 leader 在客户端算好分配并广播；Heartbeat 维持存活、超时即触发重平衡；LeaveGroup 移除成员并重平衡。整条链由 `ClassicGroupState`（EMPTY→PREPARING_REBALANCE→COMPLETING_REBALANCE→STABLE→DEAD）驱动。*[模式: 四步请求 + 状态机 + 代数递增]

## 第一层：ClassicGroupState 五状态是整条协议的骨架

`ClassicGroup` 的状态由 `ClassicGroupState` 枚举描述（`ClassicGroupState.java`）：

- **EMPTY**：没有成员，但还保留 offset；
- **PREPARING_REBALANCE**：正在收集加入请求，rebalance 进行中；
- **COMPLETING_REBALANCE**：成员已到齐，等待 leader 提交分配；
- **STABLE**：归属确定，正常消费，心跳维持；
- **DEAD**：空组清理。

它们的推进路径是：

```text
EMPTY
  → PREPARING_REBALANCE（有新成员加入）
    → COMPLETING_REBALANCE（所有成员都 join 了）
      → STABLE（leader 提交分配后）
        → PREPARING_REBALANCE（成员变化/超时，回到重平衡）
          → ...
            → EMPTY → DEAD
```

这个状态机是四步协议的"全局控制器"：每一步请求做什么、允许哪些后续请求，都由当前状态决定。比如 PREPARING_REBALANCE 时心跳会返回 `REBALANCE_IN_PROGRESS`，STABLE 时心跳才正常。

## 第二层：JoinGroup——收集成员，generate 一代

当客户端调用 `subscribe()` 并第一次 `poll()` 时，会发出 `JoinGroupRequest`。`GroupMetadataManager.classicGroupJoin()` 处理它（`GroupMetadataManager.java:6086`）。

### 新成员

如果是未知 memberId，走 `classicGroupJoinNewMember()`（`GroupMetadataManager.java:6237`）。coordinator 先返回 `MEMBER_ID_REQUIRED` 和一个新分配的 memberId，客户端带这个 memberId 重试后，才正式加入组并更新协议支持情况。

### 已有成员

如果是已存在的 memberId，走 `classicGroupJoinExistingMember()`（`GroupMetadataManager.java:6391`）。它会根据成员是否改变订阅、组处于什么状态来决定：直接返回当前 generation 信息，或触发 rebalance。

### 触发 PREPARING_REBALANCE

当新成员加入、或已有成员订阅变化时，`maybePrepareRebalanceOrCompleteJoin()` 决定是否让组进入 rebalance（`GroupMetadataManager.java:6863`）。

组进入 `PREPARING_REBALANCE` 后，coordinator 等待：要么所有成员都加入，要么等到 rebalance timeout。这里还有个细节：**第一次的 rebalance 有额外的 initial rebalance delay**，给新组成员更多时间加入。

### leader 的产生

当组的所有成员都 join 后，coordinator 进入 `completeClassicGroupJoin()`（`GroupMetadataManager.java:6563`），调用 `initNextGeneration()` 使 `generationId++`（初值为 0，`ClassicGroup.java:133`、`1291`），并选出一个 leader——通常是第一个加入的成员。然后组进入 `COMPLETING_REBALANCE`。

为什么 generation 此时递增？因为从这一刻起，组进入了一个新的一代：membership 集合和可能的分配都基于这次 join 结果。generation 是"这一代归属"的版本号。

## 第三层：JoinGroup 的响应——为什么 leader 能看到所有成员

JoinGroup 的响应里，coordinator 会返回：

- `generationId`：当前代数；
- `leader`：leader 是谁；
- `members`（仅 leader）：所有成员的订阅信息。

选举 leader 的逻辑很朴素：`ClassicGroup` 里第一个加入的成员成为 leader（`ClassicGroup.java:457`）。leader 离开后，会从已加入成员中重新指定。

关键点：**只有 leader 的 JoinGroup 响应里带全体成员列表。** 普通成员的响应只带 generation，不带 members。这样保证分配计算只发生在 leader 侧，避免所有成员各自算一套分配。

## 第四层：SyncGroup——leader 在客户端算好分配，广播给所有人

组进入 `COMPLETING_REBALANCE` 后，`classicGroupSync()` 处理 `SyncGroupRequest`（`GroupMetadataManager.java:7319`）。

- **leader** 的 SyncGroup 请求里携带它给每个成员算好的分配（`assignments`）；
- **普通成员** 的 SyncGroup 请求只带自己信息，等 coordinator 把分配结果返回。

coordinator 收到 leader 的分配后：

1. 校验 generation 与状态；
2. 把分配结果填充给所有成员的响应；
3. 组进入 `STABLE`。

这里有个非常重要的语义：**分配是在 leader 的客户端上用 `AbstractPartitionAssignor` 算的，coordinator 只负责收集成员信息、转发分配、广播结果。** 分配算法不是 SyncGroup 的核心，但"谁算分配"决定了 SyncGroup 的职责边界。

如果 leader 一直不来 SyncGroup，coordinator 会等 sync timeout；超时则重新触发 rebalance。

## 第五层：Heartbeat——稳定期的"我还在"

组进入 `STABLE` 后，`classicGroupHeartbeat()` 处理 `HeartbeatRequest`（`GroupMetadataManager.java:7598`）。

- 正常心跳：coordinator 重置该成员的过期定时器，返回成功；
- 组处于 PREPARING_REBALANCE：返回 `REBALANCE_IN_PROGRESS`；
- 成员已不在组里：返回 `UNKNOWN_MEMBER_ID`；
- generation 不匹配：返回 `ILLEGAL_GENERATION`。

如果某个成员在超时时间内没心跳，coordinator 认为它挂了，把它移除并触发 rebalance（回到 PREPARING_REBALANCE）。

心跳是 Classic 重平衡的"探针"：它既维持稳定期，也检测异常离线。

## 第六层：LeaveGroup——主动退出

`classicGroupLeave()` 处理 `LeaveGroupRequest`。成员正常关闭时调用 `close()` 会发送 LeaveGroup。

coordinator 从组里移除该成员：

- 如果它之前持有分区，这些分区需要重新分配；
- 把组带回 PREPARING_REBALANCE，触发新一轮重平衡。

对静态成员，LeaveGroup 的语义更复杂：它用 `group.instance.id` 标识身份，别的进程不能占用同一个 instance id 抢走静态成员资格。

## 第七层：rebalance 的触发源汇总

把上面几层汇总，ClassicGroup 触发 rebalance 的源头有：

- 新成员加入（JoinGroup 未知 memberId）；
- 已有成员订阅变化（JoinGroup 携带新 topics）；
- 成员心跳超时（Heartbeat 未按时）；
- 成员心跳返回错误后重新 join；
- 成员主动 LeaveGroup；
- 静态成员被新进程替换。

任何一个触发都会把组状态拉回 `PREPARING_REBALANCE`，然后重新走 `JoinGroup → SyncGroup → 新 generation → STABLE`。

这就是为什么 Kafka-6 里说 Classic 是 stop-the-world：**每当成员集合变化，所有成员的新一代归属都要被重新协商一次。**

## 收网：四步请求 + 状态机 + generation 共同构成 Classic 重平衡

把整篇压成一句话：ClassicGroup 用 JoinGroup、SyncGroup、Heartbeat、LeaveGroup 四个显式请求完成"谁在组里、谁分到哪个分区"的协商；`ClassicGroupState` 状态机（EMPTY→PREPARING_REBALANCE→COMPLETING_REBALANCE→STABLE→DEAD）驱动每一步，`generationId` 在重平衡完成时递增标记新一代归属；任何成员集合变化都会让组回到 PREPARING_REBALANCE 重新协商。

```text
成员加入
  → JoinGroup → PREPARING_REBALANCE
    → 全员 join / timeout → COMPLETING_REBALANCE（generation++）
      → SyncGroup（leader 算分配 + 广播）→ STABLE
        → Heartbeat 维持
          → 成员进出/超时/leave → 回 PREPARING_REBALANCE
```

到这里，主线只发生了七件事。

第一，ClassicGroupState 五状态驱动整条协议。

第二，JoinGroup 收集成员、选出 leader、产生 generation。

第三，只有 leader 的 JoinGroup 响应带全体成员列表。

第四，SyncGroup 让 leader 在客户端算好分配并广播。

第五，Heartbeat 维持存活，超时触发 rebalance。

第六，LeaveGroup 移除成员并触发 rebalance。

第七，任何成员集合变化都会回到 PREPARING_REBALANCE。

**本篇的一句话困惑**：一个 group 从无到稳定再到成员离开，经历了哪些事？

**本篇的一句话顿悟**：ClassicGroup 用 JoinGroup→SyncGroup→Heartbeat→LeaveGroup 四步完成归属协商，由 ClassicGroupState 状态机驱动，generation 标记每一代归属；任何成员变化都会让组回到 PREPARING_REBALANCE 重新走一遍。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“ClassicGroup 只要一次 JoinGroup 就完成。”** 还要 SyncGroup 分发分配。
2. **“leader 是 coordinator 指定的协调者。”** 它是第一个加入的成员，在客户端算分配。
3. **“generation 只在 join 时递增。”** 它在 rebalance 完成（进入 COMPLETING_REBALANCE）时递增。
4. **“memberId 每次 join 都变。”** 首次 join 时 broker 分配 memberId，后续重试使用同一 id；static member 用 group.instance.id 标识。
5. **“Heartbeat 只防超时。”** 它还用于检测离线并触发 rebalance。

### 关键证据清单

- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroupState.java:46`：状态机。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroup.java:133`：generationId。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroup.java:1291`：generation++。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6086`：classicGroupJoin。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6237`：classicGroupJoinNewMember。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6391`：classicGroupJoinExistingMember。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:7319`：classicGroupSync。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:7598`：classicGroupHeartbeat。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6863`：maybePrepareRebalanceOrCompleteJoin。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:7785`：classicGroupLeave。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线，group-coordinator 模块。
- 本篇聚焦 ClassicGroup 四步协议与状态机，不展开 Assignor 算法、rebalance 超时配置的全部参数。
- 不把 ClassicGroup 与 ModernGroup（K-6 第 3 篇）混成一个实现。
- 不把 generation 与 Kafka-7 的 leaderEpoch 混成同一概念。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-6`（ConsumerGroup 双协议总览）、`Kafka-4/21`（Consumer 依赖组资格）。
- 后续桥接：下一篇进入 K-6 ConsumerGroup 域第 3 篇（ModernGroup / KIP-848 单心跳协议与增量重平衡）。