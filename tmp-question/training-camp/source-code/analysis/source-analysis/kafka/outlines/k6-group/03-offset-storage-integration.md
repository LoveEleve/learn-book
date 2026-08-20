# K-6 Consumer Group 篇 3/3 — 位点的家: Offset 存储与跨域衔接

> 前置: [[K-6-group-02]] (增量+Assignor) [[K-2-consumer-02]] (offset 客户端) | 复用: — | 对照: [[r29-pubsub]] (对照收束) | 引出: — (K-5 Controller 交付后补链)
> 🔴 A | 来源: OffsetMetadataManager.java + GroupCoordinatorShard.java:457-970 + ClassicKafkaConsumer.java:1188
> 定位: K-6 卷收尾 — 回答"offset 存哪? 怎么过期? 与 K-2/K-5 怎么衔接?"

**读者处境**: 面试官问 "commit 的 offset 存哪? 协调器在哪?" 你答 "主题、leader" — 但再问 "__consumer_offsets 怎么组织? 过期怎么处理? 协调器怎么定位?" 你答不上来。这篇是位点存储的完整答案, 收束 K-6 域。

### 1. 问题引入 — commit 去了哪

场景: 客户端 commitSync 提交 — 数据到哪了? (K-2 篇 2 衔接) **不做 broker 逐消息记录消费** (单整数检查点)
- __consumer_offsets compact topic (K-3 存储面)
- 本篇问题: Offset 存储 (Q5) / 衔接 (Q6/Q7) / 对照 (Q8)

### 2. Offset 存储 — 记录与过期

场景: offset 记录怎么管?
- OffsetMetadataManager (OffsetMetadataManager.java:82, 1294 行): offset 记录管理
- __consumer_offsets: key=groupId+topic+partition, compact 保留最新 (K-3); 服务端入口 commitOffset (GroupCoordinatorShard.java:852 → OffsetMetadataManager.java:600)
- 过期: expireTimestampMs 计算 (OffsetMetadataManager.java:583: now+retention) + 组最后活跃条件 (OffsetExpirationCondition)
- 客户端 commitSync/commitAsync (K-2, ClassicKafkaConsumer.java:1188) → 服务端 commitOffset (GroupCoordinatorShard.java:852)

### 3. 协调器定位 — 哈希到 leader

场景: 协调器在哪? 怎么找?
- groupId 哈希 → __consumer_offsets 分区 → leader broker (K-5 Controller 管理分区 leader)
- 客户端 metadata 层: 按 groupId 定位协调器地址 (K-2 客户端 AbstractCoordinator.java:400 ensureActiveGroup 已铺垫)
- 面试点: "协调器 = offsets 分区 leader" — 与 K-5 的 leader 管理衔接 (交付后回补)

### 4. 衔接与对照 — 收束

场景: K-6 在整个 Kafka 的位置?
- K-2 客户端 (JoinGroup/Heartbeat/commit) ↔ K-6 服务端 (协议面+状态机) — 两端闭环
- K-3 存储面 (__consumer_offsets compact) ↔ K-6 记录管理
- K-5 Controller (offsets 分区 leader) ↔ 协调器定位
- 对照 r29: Redis pub/sub 无状态广播 vs 消费组分区独占+位点 — 分发范式差异收束

### 核心悬念
"为什么 offset 存 topic 而不是别的?" — compact topic (K-3): 每 key 保留最新值 (groupId+topic+partition → offset), 天然契合"位点=最新检查点"; 且复用复制/存储基础设施 (K-4 ISR/K-3 存储) — 用日志存状态 (与 KRaft 元数据同构, K-9)。

### 概念依赖链
Q5 Offset 存储 → Q6/Q7 衔接 → Q8 对照 → (K-5 交付后回补)

### 源码锚点清单
- OffsetMetadataManager.java (group/ 包: offset 记录+过期)
- GroupCoordinatorShard.java:457-970 (协议处理链)
- ClassicKafkaConsumer.java:1188 (updateFetchPositions, K-2)
- K-3: __consumer_offsets compact topic / K-5: 分区 leader (待回补)
