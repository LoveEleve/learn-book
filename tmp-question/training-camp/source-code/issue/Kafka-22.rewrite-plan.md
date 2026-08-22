# Kafka-22 重写规划

> 题目：ClassicGroup 的完整一生——JoinGroup→SyncGroup→Heartbeat→LeaveGroup 四步协议主链
> 状态：K-6 ConsumerGroup 域第 2 篇，按"ClassicGroup 协议"展开
> 目标：正面讲透 ClassicGroup 的四步协议：JoinGroup（收集成员、选举 leader、产生 generation）、SyncGroup（leader 分配、广播）、Heartbeat（维持存活）、LeaveGroup（主动退出）；以及 rebalance 的触发（成员加入/离开/超时）、generation 的递增、`ClassicGroupState` 状态机如何驱动整条重平衡。

## 1. 读者困惑

- ClassicGroup 完整重平衡的四步，每一步到底在做什么、谁发请求、谁响应什么？
- JoinGroup 为什么 leader 能拿到所有成员的订阅信息？
- generationId 是什么时候递增的，为什么它代表了"一代 rebalance"？
- SyncGroup 阶段 leader 的分配结果怎么到达其他成员？
- Heartbeat 在稳定期维持什么，超时了会怎样？
- rebalance 有哪些触发源，它们怎么让组回到 PREPARING_REBALANCE？

## 2. 一句话顿悟

**ClassicGroup 用四个显式请求把"成员归属"协商出来：JoinGroup 收集成员并用 generation 打上"这一代"标记，选出 leader；SyncGroup 让 leader 在客户端算好分配并广播给所有成员；Heartbeat 维持存活、超时则重平衡；LeaveGroup 主动退出并重平衡。整条链由 `ClassicGroupState`（EMPTY→PREPARING_REBALANCE→COMPLETING_REBALANCE→STABLE→DEAD）驱动。**

## 3. 五要素卡片

### 读者问题

一个 3 成员 group，从创建到稳定消费，再到一个成员退出，每一步发生了哪些请求？generation 为什么从 0 变到 1 再变到 2？

### 入口

- `ClassicGroup`：组成员与状态
- `ClassicGroupState`：EMPTY/PREPARING_REBALANCE/COMPLETING_REBALANCE/STABLE/DEAD
- `GroupMetadataManager.classicGroupJoin`：JoinGroup 处理
- `GroupMetadataManager.classicGroupSync`：SyncGroup 处理
- `GroupMetadataManager.classicGroupHeartbeat`：Heartbeat 处理
- `GroupMetadataManager.classicGroupLeave`：LeaveGroup 处理
- `ClassicGroup.generationId`：代数

### 状态核心

- `ClassicGroupState`（EMPTY→PREPARING_REBALANCE→COMPLETING_REBALANCE→STABLE→DEAD）
- `generationId`：每代 +1
- `leaderId`：第一个加入的成员
- `members` / `pendingJoinMembers` / `pendingSyncMembers`
- `ClassicGroupMember`：memberId / topics / protocol

### 失败路径

- JoinGroup 后成员不来 Sync → 超时重新 rebalance
- 只有部分成员加入 → 等待 rebalance timeout
- Heartbeat 超时 → 成员被移除 → rebalance
- 静态成员 id 被其他进程占用 → fencing

### 连接点

- 前文 `Kafka-6`：ConsumerGroup 双协议总览，本篇深挖 Classic 一条。
- 前文 `Kafka-4/21`：Consumer poll 与 offset 提交依赖稳定的 group 成员资格。
- 后文：K-6 ConsumerGroup 域第 3 篇（ModernGroup/KIP-848 协议）。

## 4. 总图

```text
成员加入
  → classicGroupJoin（JoinGroup）
    → 组进入 PREPARING_REBALANCE
      → 所有成员加入 / rebalance timeout
        → 进入 COMPLETING_REBALANCE（generation++）
          → leader 拿全成员列表，客户端算分配
            → classicGroupSync（SyncGroup）
              → 广播分配 → STABLE
                → Heartbeat 维持
                  → 成员进出 / 超时 → 回 PREPARING_REBALANCE
```

## 5. 关键边界

- 本篇只深挖 ClassicGroup 协议，不展开 ModernGroup（K-6 第 3 篇）。
- 不把 ClassicGroup 与 ConsumerGroup（Modern）混成一个实现：前者四步请求，后者单心跳。
- 不把 generation 与 Kafka-7 的 leaderEpoch 混成同一个概念。
- 不展开所有 Assignor 细节，只讲 leader 在客户端算分配这一事实。

## 6. 失败方案推演

1. **成员间直接协商，无 coordinator**：脑裂、无法收敛。
2. **一次请求完成所有协商**：遇到跨成员分配时无法保证一致性。
3. **不用 generation 标记**：成员无法确认自己属于哪一代，旧代请求与新代混淆。
4. **心跳不稳定**：正常成员被误判离线，频繁重平衡。

## 7. 误解清单

- “ClassicGroup 只需要一次 JoinGroup 就完成”：还要 SyncGroup 分发分配。
- “leader 是协调器指定的『协调者』”：它是第一个加入的成员，在客户端算分配。
- “generation 只在 join 时递增”：它在 rebalance 完成时递增，代表一代。
- “memberId 每次加入都变”：Classic 的 memberId 由 broker 分配，但静态成员用 group.instance.id。
- “Heartbeat 只防超时”：它还用于检测成员异常退出并触发 rebalance。

## 8. 证据清单

- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroupState.java:46`：状态机。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroup.java:133`：generationId。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroup.java:1291`：generation++。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6086`：classicGroupJoin。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6237`：classicGroupJoinNewMember。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6391`：classicGroupJoinExistingMember。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:7319`：classicGroupSync。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:7598`：classicGroupHeartbeat。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6863`：maybePrepareRebalanceOrCompleteJoin。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6563`：completeClassicGroupJoin。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`，group-coordinator 模块。
- 本篇聚焦 ClassicGroup 四步协议与状态机，不展开 Assignor 算法、rebalance 超时配置的全部参数。
- 目标正文：7000~11000 字。

## 10. 本轮重写主线

1. 从"3 成员 group 从创建到稳定，发生了哪些请求"开场。
2. 否定"成员直接协商"和"一次请求完成"两种方案。
3. 解释 `ClassicGroupState` 五状态。
4. 解释 JoinGroup 阶段（新成员/旧成员、leader 选举、generation++）。
5. 解释 SyncGroup 阶段（leader 分配、广播、进 STABLE）。
6. 解释 Heartbeat 阶段（维持存活、超时重平衡）。
7. 解释 LeaveGroup 阶段（移除成员、触发重平衡）。
8. 收网：四步协议 + 状态机 + generation 共同构成 Classic 重平衡。