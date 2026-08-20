# K-12 FetchSession 篇 2/2 — 生命与衔接: 失效重建/缓存淘汰/读路径

> 前置: [[K-12-fetchsession-01]] (会话+增量) | 复用: — | 对照: [[r28-networking]] (协议对照) [[E-10-clusterstate]] (diff 发布对照) | 引出: — (K-2 Consumer 交付后补链)
> 🟡 B | 来源: FetchSession.scala:599-698,337-386 + FetchSessionHandler.java:604-608 + K-4 衔接
> 定位: K-12 卷收尾 — 回答"会话失效/缓存满了怎么办? 与读路径怎么衔接?"

**读者处境**: 面试官问 "fetch 会话会失效吗? 缓存会不会爆?" 你答 "会重建" — 但再问 "什么时候失效? 重建代价? 缓存怎么淘汰? 谁在用这个会话?" 你答不上来。这篇是会话生命周期的完整答案, 收束 K-12 域。

### 1. 问题引入 — 会话不是永久的

场景: 会话缓存是服务端有界资源 — 满了怎么办? 会话过期了怎么办?
- 失效重建 (Q4) / 缓存淘汰 (Q5) / 读路径衔接 (Q6)
- 本篇问题: 生命周期三件事

### 2. 失效重建 — 回到全量

场景: 会话无效怎么恢复?
- 服务端: 会话不存在/创建失败 → INVALID_SESSION_ID 响应 (FetchSession.scala:337,356,386)
- 客户端: 收到 INVALID → nextMetadata 回 INITIAL (FetchSessionHandler.java:562,578) — 下次请求恢复全量
- 主动关闭: notifyClose → nextCloseExisting (FetchSessionHandler.java:604-608)
- 重建代价: 一次全量 fetch (所有分区元数据) → 服务端重建会话

### 3. 缓存淘汰 — 有界资源

场景: 缓存满了怎么处理?
- FetchSessionCacheShard (FetchSession.scala:599): maxEntries 上限 + evictionMs 时间
- 五 map 结构: sessions + lastUsed + evictableByAll/ByPrivileged (FetchSession.scala:L610-620) — 双淘汰队列
- maybeCreateSession (FetchSession.scala:L672-698): 满时 tryEvict 成功才创建, 否则 INVALID_SESSION_ID (FetchSession.scala:L692-696)
- 特权会话优先保留 (FetchSession.scala:L700+ 淘汰规则)
- 指标: 核心 Metric NumIncrementalFetchSessions / NumIncrementalFetchPartitionsCached (FetchSession.scala:44-45) + evictionsMeter (FetchSession.scala:623) 监控淘汰速率 — SRE 排查缓存压力的入口

### 4. 读路径衔接 — 消费请求的会话层

场景: 会话服务谁?
- 消费读路径 (K-4 篇 3/4): KafkaApis.handleFetchRequest → ReplicaManager → FetchSession 缓存 → 增量响应 (FetchSession.scala:337-386)
- 副本拉取是独立通道: AbstractFetcherThread.processFetchRequest (K-4 篇 3) — 会话机制主要服务消费者
- 对照 E-10: ES 集群状态 diff 发布 (新节点 full/已知节点 diff) vs Kafka 会话增量 — 都是"只传变更"的增量协议 (差异: 节点级 TransportVersion vs 会话级 epoch)

### 核心悬念
"fetch 会话缓存和 ES 的什么对应?" — ES 集群状态发布用 full/diff 双模式 (E-10 篇 2: 新节点 full/已知节点 diff); Kafka 用 FetchSession 缓存分区参数 — 同一思想: 分布式读路径的"只传变更"增量协议; 差异: ES diff 按 TransportVersion 序列化 (节点级), Kafka 会话按 epoch 判新旧 (会话级)。

### 概念依赖链
Q4 失效 → Q5 淘汰 → Q6 衔接 → (K-2 Consumer 交付后回补)

### 源码锚点清单
- FetchSession.scala:337 (INVALID 响应) / 356 (错误响应) / 386 (增量响应) / 599 (CacheShard) / 610-620 (五 map) / 655-660 (newSessionId) / 672-698 (maybeCreateSession) / 685 (创建时 epoch) / 692-696 (满时拒绝) / 700+ (淘汰规则)
- FetchSessionHandler.java:562 (full INVALID→INITIAL) / 578 (incremental 关闭→INITIAL) / 604-608 (notifyClose)
- K-4: AbstractFetcherThread.scala:318 (processFetchRequest, 副本通道)
