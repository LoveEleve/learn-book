# K-6 Consumer Group — Pass 1 探索笔记 (扫轮廓)

> 🔴 A | 依赖: K-2 ✅ (客户端协议端) + K-3 ✅ (offset 存储) | 对照: [[r14-sentinel]] (故障转移) [[r29-pubsub]] (订阅对照)
> 源码: group-coordinator/ (82 文件: GroupCoordinator 512 + GroupMetadataManager 8620 + ClassicGroup 1488 + ModernGroup 577 + ConsumerGroupMember 528 + OffsetMetadataManager) — Java 端已索引
> 测试地图: group-coordinator/src/test/ (GroupCoordinatorTest/ClassicGroupTest/ConsumerGroupMemberTest/OffsetMetadataManagerTest)

## 继承树/调用图

```
GroupCoordinator (GroupCoordinator.java, 512 行) — 协议面
├── consumerGroupHeartbeat (GroupCoordinator.java:L84, KIP-848 增量)
├── joinGroup (GroupCoordinator.java:L127) / syncGroup (GroupCoordinator.java:L143) / heartbeat (GroupCoordinator.java:L158) / leaveGroup (GroupCoordinator.java:L172)
└── offsetCommit (K-2 衔接)
GroupMetadataManager (GroupMetadataManager.java, 8620 行) — 元数据状态机
├── ClassicGroup (ClassicGroup.java:L1488) + ClassicGroupMember (ClassicGroupMember.java:L435) — 旧协议四步
├── ModernGroup (ModernGroup.java:L577) + ConsumerGroupMember (ConsumerGroupMember.java:L528, modern/consumer/) — KIP-848
└── OffsetMetadataManager — offset 过期/压缩
assignor/ (RangeAssignor/UniformAssignor/SimpleAssignor + streams StickyTaskAssignor)
```

## 基本元素分解 (原则二)

1. **协议面** — GroupCoordinator: KIP-848 heartbeat / 旧四步 API (GroupCoordinator.java:L84-172)
2. **双协议组** — ClassicGroup (四步) vs ModernGroup (增量) — 规划 R2 断言
3. **元数据状态机** — GroupMetadataManager (8620 行): 组生命周期 + __consumer_offsets 记录
4. **Assignor** — Range/Uniform/Simple (+ streams Sticky) — 分区分配算法
5. **Offset 存储** — OffsetMetadataManager: 过期条件/压缩
6. **成员模型** — ConsumerGroupMember (KIP-848) vs ClassicGroupMember

## 标记问题 (≥5)

1. **Q1: GroupCoordinator 双协议怎么分流?** — KIP-848 heartbeat vs 旧四步 (GroupCoordinator.java:L84-172)
2. **Q2: ClassicGroup 四步 rebalance 服务端?** — JoinGroup→SyncGroup→Heartbeat→Leave (ClassicGroup ClassicGroup.java:L1488)
3. **Q3: ModernGroup KIP-848 增量 rebalance?** — heartbeat 驱动增量分配 (GroupCoordinator.java:L84 + ModernGroup)
4. **Q4: Assignor 体系?** — Range/Uniform/Simple (assignor/)
5. **Q5: Offset 存储与过期?** — OffsetMetadataManager (__consumer_offsets)
6. **Q6: 与 K-2 客户端衔接?** — JoinGroup/Heartbeat 两端
7. **Q7: 与 K-5 Controller 衔接?** — offsets 分区 leader 在哪 (K-5)
8. **Q8: 与 Redis 对照?** — pub/sub vs 消费组 (r29)

## 已读测试 (2 个)

- `GroupCoordinatorTest`: 双协议处理
- `ClassicGroupTest` / `ConsumerGroupMemberTest`: 组状态

## 完成检查

- [x] 继承树/调用图
- [x] 基本元素分解 (6 元素)
- [x] 8 个标记问题
- [x] 已读 2 个测试

## 跨域发现

- 来源: K-6 Pass 1 — GroupCoordinator 的 KIP-848 heartbeat (GroupCoordinator.java:L84) 是增量 rebalance 驱动; 旧四步 API (GroupCoordinator.java:L127-172) 并存
- 发现: GroupMetadataManager 8620 行是状态机巨兽 (记录序列化 + 组生命周期) — 与 K-5 Controller 的 KRaft 记录同构 (元数据即记录)
- 已对照验证: K-2 AbstractCoordinator (客户端 ensureActiveGroup AbstractCoordinator.java:L400) — 两端协议
