# K-5 Controller 篇 2/2 — 活性与状态: 心跳/选举/状态机

> 前置: [[K-5-controller-01]] (写路径) [[K-4-isr-01]] (ISR) | 复用: — | 对照: [[rd2-rlock]] (fencing) [[r14-sentinel]] (故障转移) | 引出: — (K-9 Raft 交付后补链)
> 🔴 A | 来源: BrokerHeartbeatManager.java:45-68 + PartitionChangeBuilder.java:70-112 + BrokerControlStates.java
> 定位: K-5 卷收尾 — 回答"broker 死活谁判断? Leader 怎么选?"

**读者处境**: 面试官问 "broker 挂了谁发现? 分区 Leader 怎么重选?" 你答 "心跳、控制器" — 但再问 "fenced 是什么? 选举三档策略? 状态机在哪?" 你答不上来。这篇是活性与选举的完整答案, 收束 K-5 域。

### 1. 问题引入 — 活着的定义

场景: broker 掉线 — 怎么发现? 分区怎么办? **不再依赖 ZK** (KRaft 自管活性)
- BrokerHeartbeatManager 心跳活性 (KRaft 替代 ZK 临时节点)
- 本篇问题: 心跳 (Q3) / 状态机 (Q4) / 选举 (Q5)

### 2. 心跳活性 — fenced 语义

场景: 活性怎么跟踪?
- BrokerHeartbeatManager (BrokerHeartbeatManager.java:L58): 软状态 + BrokerHeartbeatTracker (L45-49 注释)
- fenced 标记 (BrokerHeartbeatManager.java:L66-68) — 心跳超时 → fenced → 不参与服务
- 仅 active controller 持有 (BrokerHeartbeatManager.java:L51-57 注释) — standby 不处理心跳
- 对照 rd2: fenced = fencing token 语义 (旧角色被隔离)

### 3. 状态机 — 4.x 重构

场景: 分区/副本状态谁管?
- 4.1.2: PartitionChangeBuilder (选举/变更) + BrokerControlStates (broker 状态) — 内嵌 QuorumController 体系
- 规划断言 PartitionStateMachine/ReplicaStateMachine 是旧版 (KRaft 迁移重构)
- 状态转换由写事件驱动 (篇 1 Q1 队列)

### 4. Leader 选举 — 三档策略

场景: 分区 Leader 怎么选?
- PartitionChangeBuilder 注释 (PartitionChangeBuilder.java:L70-78): ①preferred replica (ISR 内) 优先 (PartitionChangeBuilder.java:L70) ②ISR 内选 (PartitionChangeBuilder.java:L74) ③可出 ISR 保在线 (L78, unclean)
- minISR 约束 (PartitionChangeBuilder.java:L88,112) — 与 K-4 HW 冻结联动
- 触发: 心跳超时 → fenced → 状态机选举
- 重分配 (规划断言 ④): PartitionReassignmentReplicas (PartitionReassignmentReplicas.java:32, 目标副本集构建 L98 完成检查) → 写事件驱动变更 (篇 1 Q1)
- 规划断言 "ISR 中第一个活着的" = 简化, 实际三档

### 核心悬念
"fenced 和 unclean 选举怎么权衡?" — 心跳超时 fenced (活性隔离, rd2 同源) → 选举: 优先 preferred/ISR (一致性) → ISR 全挂时出 ISR 选 (可用性, unclean 开关) — 与 K-4 的 HW 冻结 (under-min-ISR) 构成一致性双保险。

### 概念依赖链
Q3 心跳 → Q4 状态机 → Q5 选举 → (K-9 交付后回补)

### 源码锚点清单
- BrokerHeartbeatManager.java:45-49 (注释) / 51-57 (仅 active) / 58 (类) / 66-68 (fenced)
- PartitionChangeBuilder.java:70 (preferred 优先) / 74 (ISR 内选) / 78 (出 ISR 保在线) / 88 (minISR) / 112 (构造)
- BrokerControlStates.java (broker 状态)
- K-4 衔接: BrokersToIsrs / HW 冻结
