# K-6 闭环笔记 Q1-Q8: 双协议/Classic 四步/KIP-848 增量/Assignor/Offset/衔接/对照

## Q1: GroupCoordinator 双协议怎么分流?

假设: KIP-848 heartbeat API 与旧四步 API 并存, 委托 GroupMetadataManager。

验证过程:
- GroupCoordinator 接口 (GroupCoordinator.java:L84-172): consumerGroupHeartbeat (L84, KIP-848) / joinGroup (GroupCoordinator.java:L127) / syncGroup (GroupCoordinator.java:L143) / heartbeat (GroupCoordinator.java:L158) / leaveGroup (GroupCoordinator.java:L172)
- GroupCoordinatorShard 实现 (GroupCoordinatorShard.java:L153): consumerGroupHeartbeat (GroupCoordinatorShard.java:L457-461) / classicGroupJoin (GroupCoordinatorShard.java:L549) / classicGroupSync (GroupCoordinatorShard.java:L570) / classicGroupHeartbeat (GroupCoordinatorShard.java:L591) / classicGroupLeave (GroupCoordinatorShard.java:L970) — 全委托 GroupMetadataManager
- 双协议并存: 新协议 heartbeat + 旧协议四步 (K-2 客户端双模型对应)

代码类型: Interface (双协议)

结论: **双协议 = KIP-848 consumerGroupHeartbeat (GroupCoordinator.java:84, Shard GroupCoordinatorShard.java:L457) + 旧四步 classicGroupJoin/Sync/Heartbeat/Leave (Shard GroupCoordinatorShard.java:L549-970) — 全委托 GroupMetadataManager 状态机**。GroupCoordinator.java:84-172 + GroupCoordinatorShard.java:457-970

## Q2: ClassicGroup 四步 rebalance 服务端?

假设: 旧协议组状态机: JoinGroup→SyncGroup→Heartbeat→Leave。

验证过程:
- ClassicGroup (ClassicGroup.java, 1488 行) + ClassicGroupMember (ClassicGroupMember.java:L435) — 旧协议组
- 四步 API 全在 GroupCoordinatorShard (GroupCoordinatorShard.java:L549-970): classicGroupJoin (GroupCoordinatorShard.java:L549) → classicGroupSync (GroupCoordinatorShard.java:L570) → classicGroupHeartbeat (GroupCoordinatorShard.java:L591) → classicGroupLeave (GroupCoordinatorShard.java:L970)
- 服务端处理: GroupMetadataManager.classicGroupJoin/classicGroupSync/classicGroupHeartbeat (GroupMetadataManager.java:L554,575,595)
- K-2 客户端 AbstractCoordinator (ensureActiveGroup AbstractCoordinator.java:L400) 对应端

代码类型: Implementation (旧协议状态机)

结论: **Classic 四步服务端 = classicGroupJoin (Shard GroupCoordinatorShard.java:L549) → Sync (GroupCoordinatorShard.java:L570) → Heartbeat (GroupCoordinatorShard.java:L591) → Leave (GroupCoordinatorShard.java:L970), 状态机在 GroupMetadataManager (GroupCoordinatorShard.java:L554-595)**。ClassicGroup.java + GroupCoordinatorShard.java:549-970

## Q3: ModernGroup KIP-848 增量 rebalance?

假设: heartbeat 携带订阅/分配增量 → 服务端 reconcile。

验证过程:
- consumerGroupHeartbeat (GroupMetadataManager.java:4691-4724): memberEpoch=-1/-2 → leave (GroupMetadataManager.java:L4697-4701); 否则常规 heartbeat (GroupMetadataManager.java:L4705-4721)
- 请求携带增量: subscribedTopicNames/subscribedTopicRegex/serverAssignor/topicPartitions (GroupMetadataManager.java:L4715-4720) — 每次 heartbeat 上报状态
- ModernGroup (ModernGroup.java:L577) + ConsumerGroupMember (ConsumerGroupMember.java:L528) — 增量组模型
- 增量 rebalance: 服务端按上报计算目标分配, 只调受影响成员 (vs Classic 全停)

代码类型: Implementation (KIP-848 增量协议)

结论: **KIP-848 = heartbeat 即状态上报 (GroupMetadataManager.java:4705-4721 携带订阅/分配) + epoch 语义 (GroupMetadataManager.java:L4697: -1/-2 离组); ModernGroup (GroupMetadataManager.java:L577) 增量 reconcile — 与 Classic 全停式对比 (K-2 篇 2 衔接)**。GroupMetadataManager.java:4691-4724 + ModernGroup.java

## Q4: Assignor 体系?

假设: Range/Uniform/Simple 多分配算法。

验证过程:
- assignor/ 包: RangeAssignor / UniformAssignor / SimpleAssignor (7 文件)
- streams 侧: StickyTaskAssignor (streams/assignor/)
- 分配算法: Range (按 topic 均分) / Uniform (全局均分) / Simple (基础)
- 客户端 vs 服务端: KIP-848 服务端分配 (serverAssignor 参数 GroupMetadataManager.java:L4719)

代码类型: Algorithmic (分配算法)

结论: **Assignor = RangeAssignor/UniformAssignor/SimpleAssignor (assignor/ 7 文件) + streams StickyTaskAssignor; KIP-848 由服务端执行 (serverAssignor 参数 GroupMetadataManager.java:4719)**。assignor/ 包

## Q5: Offset 存储与过期?

假设: __consumer_offsets 记录 + OffsetMetadataManager 管理过期。

验证过程:
- OffsetMetadataManager (group/OffsetMetadataManager.java): offset 记录管理
- __consumer_offsets compact topic (K-3 存储面) — key=groupId+topic+partition
- 过期: OffsetExpirationCondition (规划 R2 断言) — 组最后活跃时间
- K-2 commitSync/commitAsync (客户端) → offsetCommit (服务端 GroupCoordinator)

代码类型: Implementation (offset 存储)

结论: **Offset 存储 = OffsetMetadataManager (group/OffsetMetadataManager.java) + __consumer_offsets compact topic (K-3) + 过期条件 (组最后活跃) — K-2 commit 的服务端落点**。OffsetMetadataManager.java

## Q6: 与 K-2 客户端衔接?

假设: JoinGroup/Heartbeat 两端协议。

验证过程:
- K-2 客户端: AbstractCoordinator.ensureActiveGroup (AbstractCoordinator.java:L400-401) + joinGroupIfNeeded (AbstractCoordinator.java:L463) + pollHeartbeat (AbstractCoordinator.java:L368)
- K-6 服务端: GroupCoordinatorShard classicGroupJoin (GroupCoordinatorShard.java:L549) / classicGroupHeartbeat (GroupCoordinatorShard.java:L591) — 同协议两端
- KIP-848: 客户端 ConsumerNetworkThread 发 heartbeat → 服务端 consumerGroupHeartbeat (GroupCoordinatorShard.java:L457)
- 闭环: 客户端 poll → heartbeat → 服务端组状态 → 分配回客户端

代码类型: 衔接分析

结论: **K-2↔K-6 两端协议: 客户端 ensureActiveGroup (K-2 AbstractCoordinator.java:400) ↔ 服务端 classicGroupJoin (Shard GroupCoordinatorShard.java:L549) / KIP-848 heartbeat (GroupCoordinatorShard.java:L457) — 同协议两端已闭环 (K-2 交付时已铺垫)**。GroupCoordinatorShard.java:457-591

## Q7: 与 K-5 Controller 衔接?

假设: __consumer_offsets 分区 leader 决定协调器位置。

验证过程:
- 组协调器 = __consumer_offsets 分区 leader 所在 broker (规划断言)
- K-5 Controller 管理分区 leader (交付后回补)
- 客户端按 groupId 哈希定位协调器 (K-2 metadata 层)
- 面试点: "协调器在哪 = groupId 哈希 → offsets 分区 → leader broker"

代码类型: 衔接分析

结论: **协调器定位 = groupId 哈希 → __consumer_offsets 分区 → leader broker (K-5 Controller 管理分区 leader, 交付后回补细节)**。K-5 衔接 (待回补)

## Q8: 与 Redis 对照?

假设: 消费组 vs pub/sub vs 队列。

验证过程:
- Redis: PUBLISH/SUBSCRIBE (r29) 无状态广播; BRPOP 队列无组概念
- Kafka: 消费组 = 分区在组内互斥分配 + 位点 + 重平衡 — 队列+订阅的融合
- 对照维度: 消息分发 (pub/sub 广播 vs 组内分区独占) / 位点 (无 vs offset) / 弹性 (无 vs rebalance)
- 面试记忆点: "Redis pub/sub 是广播, Kafka 组是分区独占的广播变体"

代码类型: 对照分析

结论: **Kafka 消费组 (分区独占+位点+rebalance) vs Redis pub/sub (广播无状态, r29) — 消息分发两范式**。r29-pubsub 对照

跨域关联: K-2 (客户端协议) / K-3 (offset 存储) / K-5 (协调器定位) / r29 (对照)
