# K-6 Consumer Group 篇 1/3 — 协调中枢: GroupCoordinator 与双协议

> 前置: [[K-2-consumer-01]] (客户端双模型) [[K-2-consumer-02]] (rebalance) | 复用: — | 对照: [[r29-pubsub]] (订阅对照) | 引出: [[K-6-group-02]]
> 🔴 A | 来源: GroupCoordinator.java:84-172 + GroupCoordinatorShard.java:153,457-970 + ClassicGroup.java
> 定位: K-6 卷开篇 — 回答"服务端怎么协调消费组? 双协议怎么分流?"

**读者处境**: 面试官问 "消费组协调器干什么? KIP-848 是什么?" 你答 "组管理" — 但再问 "双协议怎么并存? 四步协议服务端怎么处理? 新协议 heartbeat 怎么驱动?" 你答不上来。这篇是组协调的完整答案。

### 1. 问题引入 — 客户端协议的对面

场景: K-2 讲客户端 JoinGroup/Heartbeat — 服务端谁处理? (K-2 篇 2 衔接) **不做无协调广播** (组内分区独占, 非 pub/sub)
- GroupCoordinator (GroupCoordinator.java:84-172) 协议面
- 本篇问题: 双协议分流 (Q1) / Classic 四步 (Q2) / KIP-848 (Q3)

### 2. 协议面 — 双协议并存

场景: 新旧协议怎么共处?
- GroupCoordinator 接口: consumerGroupHeartbeat (L84, KIP-848) + joinGroup (GroupCoordinator.java:L127) / syncGroup (GroupCoordinator.java:L143) / heartbeat (GroupCoordinator.java:L158) / leaveGroup (GroupCoordinator.java:L172)
- GroupCoordinatorShard 实现 (GroupCoordinatorShard.java:L153): 全委托 GroupMetadataManager (GroupCoordinatorShard.java:L457-970)
- 双协议并存 = 向后兼容 (K-2 客户端双模型对应: Async→KIP-848, Classic→四步)

### 3. Classic 四步 — 服务端状态机

场景: 旧协议四步怎么处理?
- classicGroupJoin (GroupCoordinatorShard.java:L549) → classicGroupSync (GroupCoordinatorShard.java:L570) → classicGroupHeartbeat (GroupCoordinatorShard.java:L591) → classicGroupLeave (GroupCoordinatorShard.java:L970)
- ClassicGroup (ClassicGroup.java:L1488) + ClassicGroupMember (ClassicGroupMember.java:L435): 组状态机
- 全停式 rebalance: Join 期间所有成员停消费 (K-2 篇 2 对照)

### 4. KIP-848 — heartbeat 即状态

场景: 新协议为什么增量?
- consumerGroupHeartbeat (GroupMetadataManager.java:4691-4724): epoch=-1/-2 → leave (GroupMetadataManager.java:L4697-4701)
- heartbeat 携带增量: 订阅/分配/serverAssignor (GroupMetadataManager.java:L4715-4720) — 每次上报状态
- ModernGroup (ModernGroup.java:L577) 增量 reconcile — 只调受影响成员

### 核心悬念
"KIP-848 增量 rebalance 为什么快?" — Classic 四步: Join 等全部成员 → 全停 → 重分配; KIP-848: heartbeat 即状态上报 → 服务端算目标分配 → 只调受影响成员 — 从 stop-the-world 到增量收敛, 大组 (1000+ 成员) 收益显著。

### 概念依赖链
Q1 双协议 → Q2 Classic → Q3 KIP-848 → (02 篇: 增量+Assignor)

### 源码锚点清单
- GroupCoordinator.java:84 (consumerGroupHeartbeat) / 127 (joinGroup) / 143 (syncGroup) / 158 (heartbeat) / 172 (leaveGroup)
- GroupCoordinatorShard.java:153 (类) / 457-461 (consumerGroupHeartbeat) / 549 (classicGroupJoin) / 570 (classicGroupSync) / 591 (classicGroupHeartbeat) / 970 (classicGroupLeave)
- GroupMetadataManager.java:4691-4724 (consumerGroupHeartbeat) / 4697-4701 (leave) / 4715-4720 (增量携带)
- ClassicGroup.java (1488 行) / ClassicGroupMember.java (435 行) / ModernGroup.java (577 行)
