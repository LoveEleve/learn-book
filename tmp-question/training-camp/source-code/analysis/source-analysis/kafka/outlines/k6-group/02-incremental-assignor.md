# K-6 Consumer Group 篇 2/3 — 分配的艺术: 增量 rebalance 与 Assignor

> 前置: [[K-6-group-01]] (双协议) | 复用: — | 对照: [[r14-sentinel]] (故障转移) | 引出: [[K-6-group-03]]
> 🔴 A | 来源: ModernGroup.java + ConsumerGroupMember.java:45,528 + assignor/ (7 文件) + GroupMetadataManager.java:4691-4724
> 定位: K-6 卷中篇 — 回答"KIP-848 怎么增量分配? 分配算法有哪些?"

**读者处境**: 面试官问 "KIP-848 增量 rebalance 原理? Range/Uniform/Sticky 区别?" 你答 "心跳驱动、算法" — 但再问 "成员怎么上报? 目标分配怎么算? 服务端还是客户端算?" 你答不上来。这篇是增量分配与算法的完整答案。

### 1. 问题引入 — 从全停到增量

场景: Classic 全停重分配 vs KIP-848 增量 — 具体怎么实现? **不做客户端算分配** (KIP-848 上移服务端)
- heartbeat 即状态上报 (GroupMetadataManager.java:4705-4721)
- 本篇问题: 增量机制 (Q3) / Assignor (Q4)

### 2. 增量机制 — 成员状态机

场景: KIP-848 成员怎么参与?
- ConsumerGroupMember (ConsumerGroupMember.java:45,528) — 成员状态
- heartbeat 请求携带: subscribedTopicNames/subscribedTopicRegex/serverAssignor/topicPartitions (GroupMetadataManager.java:4715-4720)
- 服务端: 按上报计算目标分配 → 返回 memberEpoch 推进
- epoch 语义: 递增版本, -1/-2 离组 (GroupMetadataManager.java:L4697-4701)

### 3. Assignor — 分配算法

场景: 目标分配谁算? 怎么算?
- assignor/ 包 (7 文件): RangeAssignor (按 topic 均分) / UniformAssignor (全局均分) / SimpleAssignor
- 默认: KIP-848 内置列表首位 = UniformAssignor (GroupCoordinatorConfig.java:187-193, "first one is the default") — 规划断言 "Range 默认" 是旧协议时代; KIP-848 服务端分配默认 Uniform
- streams 侧 (规划 R2 断言): StickyTaskAssignor (粘性保留) + CopartitionedTopicsEnforcer (co-partitioned topic 同分区数约束, CopartitionedTopicsEnforcer.java:39) — 消费组 assignor/ 无 Sticky (streams 专用)
- KIP-848: serverAssignor 参数 (GroupMetadataManager.java:L4719) — **服务端执行分配** (vs 旧协议客户端分配)
- 面试对比: Range 按 topic 分 (可能不均) vs Uniform 全局分 (更均)

### 4. 分配生命周期

场景: 一次增量 rebalance 完整流程?
- 成员 heartbeat 上报订阅 → 服务端检测变更 → 计算目标分配 → 受影响成员收到新 epoch → 调整分区
- 只调受影响成员: 其他成员 epoch 不变 (增量核心)
- K-2 客户端: ConsumerNetworkThread 持续 heartbeat

### 核心悬念
"KIP-848 为什么把分配从客户端移到服务端?" — 旧协议客户端分配需要 Join 阶段收集全部成员再算 (两轮 RTT + 全停); 服务端分配: 每次 heartbeat 增量上报 → 服务端始终掌握全貌 → 目标分配随时可算 — 分配权上移换取增量收敛。

### 概念依赖链
Q3 增量 → Q4 Assignor → (03 篇: Offset+衔接)

### 源码锚点清单
- GroupMetadataManager.java:4691-4724 (consumerGroupHeartbeat) / 4715-4720 (增量携带) / 4719 (serverAssignor)
- ConsumerGroupMember.java:45 (类) / 528 (行数)
- ModernGroup.java (577 行)
- assignor/ 包: RangeAssignor / UniformAssignor / SimpleAssignor (7 文件)
- streams/assignor/StickyTaskAssignor (对照)
