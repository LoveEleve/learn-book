# K-6 Consumer Group — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: group-coordinator/ (82 文件: GroupCoordinator 512 + GroupMetadataManager 8620 + ClassicGroup 1488 + ModernGroup 577 + assignor/) — Java 端已索引

## 01 逐源提取

| 源文件 | 机制点 |
|---|---|
| GroupCoordinator.java | ①consumerGroupHeartbeat (L84, KIP-848) ②joinGroup (GroupCoordinator.java:L127) ③syncGroup (GroupCoordinator.java:L143) ④heartbeat (GroupCoordinator.java:L158) ⑤leaveGroup (GroupCoordinator.java:L172) ⑥offsetCommit (K-2 衔接) |
| GroupMetadataManager.java | ①元数据状态机 (8620 行) ②组生命周期 ③记录序列化 (__consumer_offsets) |
| ClassicGroup.java | ①旧协议组状态 (四步 rebalance 服务端) ②成员管理 (ClassicGroupMember 435) |
| ModernGroup.java | ①KIP-848 增量组 ②ConsumerGroupMember (modern/consumer/) |
| assignor/ | RangeAssignor / UniformAssignor / SimpleAssignor (+ StickyTaskAssignor?) |
| offsets/ | OffsetMetadataManager (offset 过期/压缩, 规划 R2) |

## 02 聚合 (P1/P2/P3)

| 聚合机制 | 来源 | 分级 |
|---|---|---|
| 双协议 (ClassicGroup vs ModernGroup KIP-848) | ClassicGroup + ModernGroup | P1 |
| KIP-848 增量 rebalance (heartbeat 驱动) | GroupCoordinator L84 + ModernGroup | P1 |
| 四步 rebalance (服务端) | ClassicGroup + GroupCoordinator GroupCoordinator.java:L127-172 | P1 |
| Assignor 体系 | assignor/ | P1 |
| Offset 存储 (__consumer_offsets + 过期) | OffsetMetadataManager | P1 |
| 元数据状态机 | GroupMetadataManager | P2 |

## 03 深度分类

- 🔴: 双协议 + 增量 rebalance + Assignor + Offset 存储 (Consumer Group 定义特征)
- 🟡: 元数据状态机 / 迁移策略 (ConsumerGroupMigrationPolicy)
- 🟢: 配置 (session.timeout 等)

## 04 聚类 (教学顺序)

```
服务端组协调 (GroupCoordinator 协议面)
  → 双协议: ClassicGroup (四步) vs ModernGroup (KIP-848 增量)
  → 元数据状态机 (GroupMetadataManager, __consumer_offsets 记录)
  → Assignor (Range/Uniform/Sticky)
  → Offset 存储 (OffsetMetadataManager)
  → K-2 客户端衔接 (客户端 JoinGroup/Heartbeat → 服务端处理)
```

**拆篇建议**: 3 篇 (🔴 A, 8 闭环)
- 01: GroupCoordinator 与双协议 (Classic 四步 vs Modern KIP-848)
- 02: 增量 rebalance 与 Assignor (KIP-848 heartbeat 驱动 + 分配算法)
- 03: Offset 存储与衔接 (__consumer_offsets + K-2/K-5 衔接 + 对照)
