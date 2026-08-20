# K-12 闭环笔记 Q1-Q6: 会话模型/Epoch/Handler/失效重建/淘汰/衔接

## Q1: 服务端会话怎么缓存分区?

假设: FetchSession 持有分区参数缓存, update 产生增量三元组。

验证过程:
- class FetchSession (FetchSession.scala:236): id/privileged/partitionMap/usesTopicIds/creationMs/lastUsedMs/epoch (FetchSession.scala:L237-242)
- update (FetchSession.scala:271-292): fetchData 遍历 → 新增 (mustAdd FetchSession.scala:L279, added) / 已有 (updateRequestParams FetchSession.scala:L284, updated) / toForget 删除 (FetchSession.scala:L289-291, removed) — **增量三元组 (added, updated, removed)**
- getFetchOffset (FetchSession.scala:L268-270): 缓存查找

代码类型: Implementation (会话缓存)

结论: **服务端会话 = FetchSession (id+epoch+partitionMap, FetchSession.scala:L236); update 产生 added/updated/removed 三元组 (FetchSession.scala:L271-292) — 增量 fetch 只需传变更分区**。FetchSession.scala:236,271-292

## Q2: Epoch 怎么推进?

假设: 会话 epoch 从 INITIAL 递增, FINAL 关闭, INVALID 重建。

验证过程:
- 常量 (FetchMetadata.java:31-43): INVALID_SESSION_ID=0 (FetchMetadata.java:L31) / INITIAL_EPOCH=0 (FetchMetadata.java:L37) / FINAL_EPOCH=-1 (FetchMetadata.java:L43)
- nextEpoch (FetchMetadata.java:64-65): FINAL_EPOCH 之后恒为 FINAL_EPOCH — 关闭态不可逆
- 初始: maybeCreateSession 用 nextEpoch(INITIAL_EPOCH) (FetchSession.scala:685) — 新会话 epoch=1
- 推进: 客户端 nextIncremental() (FetchSessionHandler.java:596) 每次响应后 epoch+1

代码类型: Interface (会话版本契约)

结论: **Epoch = 会话版本号: INITIAL_EPOCH=0 起递增 (FetchMetadata.java:37), FINAL_EPOCH=-1 终态不可逆 (FetchMetadata.java:L43,64-65); 服务端创建时 nextEpoch(INITIAL) (FetchSession.scala:685), 客户端每响应推进 (FetchSessionHandler.java:596)**。FetchMetadata.java:31-65

## Q3: 客户端 Handler 怎么维护会话?

假设: FetchSessionHandler 持有 nextMetadata, 按响应类型切换 full/incremental。

验证过程:
- 状态: sessionId/nextMetadata (FetchSessionHandler.java:76-77)
- handleResponse (FetchSessionHandler.java:L559-598) 双路径:
  - full fetch: INVALID_SESSION_ID → INITIAL 重建 (FetchSessionHandler.java:L562-565); 新会话 → newIncremental(sessionId) (FetchSessionHandler.java:L569-573)
  - incremental: INVALID_SESSION_ID (服务端关闭) → INITIAL (FetchSessionHandler.java:L578-584); 继续 → nextIncremental() (FetchSessionHandler.java:L590-596)
- KIP-219 节流: 空 full 响应 + throttleTime → INITIAL (FetchSessionHandler.java:L542-553)

代码类型: Implementation (客户端状态机)

结论: **Handler 按响应双路径切换: full→新会话/INVALID→INITIAL (FetchSessionHandler.java:L562-573); incremental→继续 nextIncremental/关闭→INITIAL (FetchSessionHandler.java:L578-596) — 增量会话持续则只发变更分区**。FetchSessionHandler.java:76-77,559-598

## Q4: 失效重建流程?

假设: 会话无效 (超时/错误) → 客户端重建全量会话。

验证过程:
- 服务端侧: INVALID_SESSION_ID 响应 (FetchSession.scala:337,356,386) — 会话不存在/创建失败时
- 客户端侧: response.sessionId() == INVALID_SESSION_ID → FetchMetadata.INITIAL (FetchSessionHandler.java:562,578) — 下次请求回到全量
- 主动关闭: notifyClose → nextCloseExisting (FetchSessionHandler.java:604-608)
- 重建代价: 全量 fetch 一次 (所有分区元数据) → 服务端再建会话

代码类型: Glue (失效恢复)

结论: **失效重建 = 服务端 INVALID_SESSION_ID (FetchSession.scala:337) → 客户端回 INITIAL 全量 (FetchSessionHandler.java:562,578); 主动关闭 notifyClose (FetchSessionHandler.java:L604) — 重建代价是恢复一次全量 fetch**。FetchSession.scala:337 + FetchSessionHandler.java:562-608

## Q5: 缓存淘汰策略?

假设: 容量上限 + 时间淘汰, 特权会话优先保留。

验证过程:
- FetchSessionCacheShard (FetchSession.scala:599): maxEntries + evictionMs + 5 个 map (sessions/lastUsed/evictableByAll/evictableByPrivileged, FetchSession.scala:L610-620)
- maybeCreateSession (FetchSession.scala:L672-698): 有空间或 tryEvict 成功 → 创建; 否则 INVALID_SESSION_ID (FetchSession.scala:L692-696)
- newSessionId (FetchSession.scala:L655-660): 随机正数避开 INVALID
- 淘汰规则 (FetchSession.scala:L700+): 特权可逐出非特权; 按 lastUsed 时间

代码类型: Implementation (缓存管理)

结论: **缓存 = 分片 + maxEntries 上限 + 双淘汰队列 (evictableByAll/ByPrivileged FetchSession.scala:L618-620); 满时 tryEvict 失败则拒绝建会话 (FetchSession.scala:L692-696) — 会话缓存是有界资源**。FetchSession.scala:599-698

## Q6: 与 K-4 读路径衔接?

假设: ReplicaManager.processFetchRequest 先查会话缓存, 增量则只处理变更。

验证过程:
- K-4 篇 3: AbstractFetcherThread.processFetchRequest (AbstractFetcherThread.scala:318) — 副本拉取
- 消费读路径: ReplicaManager 处理 KafkaApis.handleFetchRequest → FetchSession 查缓存 → 增量响应 (FetchSession.scala:337-386 的响应生成)
- 会话 epoch vs 分区 epoch: 两套独立递增体系 (K-12 Q2 vs K-4 Q5) — 面试易混点

代码类型: 衔接分析

结论: **衔接 = 消费读路径 (K-4 篇 3/4) 上游会话层: ReplicaManager → FetchSession 缓存 → 增量响应 (FetchSession.scala:337-386); 会话 epoch 与分区 leader epoch 是两套体系 (易混点)**。FetchSession.scala:337-386

跨域关联: K-4 (读路径) / K-3 (LocalLog.read) / r28-networking (增量协议对照)
