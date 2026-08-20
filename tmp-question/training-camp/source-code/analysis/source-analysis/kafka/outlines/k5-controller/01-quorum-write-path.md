# K-5 Controller 篇 1/2 — 单一写者: QuorumController 与 Raft 复制

> 前置: [[K-6-group-03]] (组元数据) | 复用: — | 对照: [[E-10-clusterstate]] (ES 单写者协调) | 引出: [[K-5-controller-02]]
> 🔴 A | 来源: QuorumController.java:174,729-831,931-985
> 定位: K-5 卷开篇 — 回答"元数据变更怎么被唯一权威处理?"

**读者处境**: 面试官问 "KRaft 控制器干什么? 元数据怎么变更?" 你答 "写日志" — 但再问 "写事件队列怎么工作? commit 后谁处理? active/standby 分工?" 你答不上来。这篇是 Controller 写路径的完整答案。

### 1. 问题引入 — 元数据的唯一写者

场景: createTopic/registerBroker/Leader 变更 — 谁拍板? 怎么同步到全集群? **不做多写者** (元数据一致性根基)
- QuorumController (QuorumController.java:174) — KRaft 时代唯一写者
- 本篇问题: 写事件 (Q1) / commit 分工 (Q2)

### 2. 写事件队列 — 单一写路径

场景: 元数据操作怎么处理?
- appendWriteEvent (QuorumController.java:L931-954): 操作转 ControllerWriteEvent 入队 (QuorumController.java:L944-950) → future 返回 (QuorumController.java:L953)
- generateRecordsAndResult (QuorumController.java:L729-831): 执行 → ControllerResult (记录+响应)
- 设计: 所有变更 (createTopic/registerBroker/选举) 统一走队列 → Raft log — 无并发写竞争

### 3. Raft commit — active/standby 分工

场景: 记录提交后谁处理?
- QuorumMetaLogListener.handleCommit (QuorumController.java:L956-985): Raft commit 回调
- active controller (QuorumController.java:L965): 推进水位 + 完成 purgatory (QuorumController.java:L972-978) — 记录已 replay 无需重复 (QuorumController.java:L970-971 注释)
- standby controller: 回放记录重建状态 (QuorumController.java:L979-985) → 产出 MetadataImage (MetadataImage.java:33: ClusterImage L50/TopicsImage L38/ConfigurationsImage L39) — 全节点共享的元数据快照 (规划 R4 断言)
- 单写多读: active 写, standby 随时接管

### 核心悬念
"为什么元数据要单一写者?" — 元数据是全局一致性的根基: 多写者需要复杂冲突解决 (ZK 时代也是单 leader); KRaft 用 Raft 选主 (K-9) → 单写者 + 记录 log → 所有节点状态一致可验证 — "用日志存状态"的极致 (对照 E-10 单写者广播)。

### 概念依赖链
Q1 写事件 → Q2 commit 分工 → (02 篇: 活性+状态机) → (K-9 Raft 交付后回补)

### 源码锚点清单
- QuorumController.java:174 (类) / 729-831 (generateRecordsAndResult) / 931-954 (appendWriteEvent) / 944 (ControllerWriteEvent) / 946-950 (入队) / 953 (future) / 956-985 (handleCommit) / 965 (isActiveController) / 972-978 (active 推进) / 979-985 (standby 回放)
