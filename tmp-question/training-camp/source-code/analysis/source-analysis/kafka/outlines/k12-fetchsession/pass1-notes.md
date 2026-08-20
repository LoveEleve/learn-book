# K-12 FetchSession — Pass 1 探索笔记 (扫轮廓)

> 🟡 B | 依赖: K-4 ✅ (读路径) + K-3 ✅ (LocalLog.read) | 对照: [[r28-networking]] (增量协议对照)
> 源码: core/src/main/scala/kafka/server/FetchSession.scala (908 行) + clients/src/main/java/org/apache/kafka/clients/FetchSessionHandler.java (628 行)
> [索引覆盖: scala 未索引, Java 端 (FetchSessionHandler) 已索引 — codebase-memory 验证]
> 测试地图: FetchSessionTest.scala + FetchSessionHandlerTest.java + BaseFetchRequestTest.scala

## 继承树/调用图

```
客户端: FetchSessionHandler (FetchSessionHandler.java:60)
├── sessionId (FetchSessionHandler.java:L76) / nextMetadata (FetchSessionHandler.java:L77)
├── handleResponse (FetchSessionHandler.java:L559: INVALID_SESSION_ID → 重建; FetchSessionHandler.java:L569: 增量推进)
└── FetchMetadata (FetchMetadata.java: 常量 L31/37/43)

服务端: ReplicaManager.processFetchRequest (K-4 衔接)
└── FetchSessionCache (FetchSession.scala:805)
      └── FetchSessionCacheShard (FetchSession.scala:L599) — 分片缓存 + 淘汰
            └── FetchSession (FetchSession.scala:L236: id/epoch/partitionMap)
                  └── update (FetchSession.scala:L271-292: added/updated/removed 增量三元组)
```

## 基本元素分解 (原则二)

1. **会话标识** — FetchSession.id + epoch (FetchSession.scala:L236-239): 客户端与服务端共享的会话状态
2. **缓存分区表** — partitionMap (CachedPartition): 服务端记住各分区 fetch 参数
3. **增量更新** — update 三元组 (FetchSession.scala:L271-292): added/updated/removed — 只传变更
4. **Epoch 机制** — INITIAL_EPOCH=0 → 递增 → FINAL_EPOCH=-1 (FetchMetadata.java:37,43): 判会话新旧
5. **失效重建** — INVALID_SESSION_ID=0 (FetchMetadata.java:L31) → 客户端重建全量会话
6. **缓存淘汰** — FetchSessionCacheShard (FetchSession.scala:L599): 容量上限 + 淘汰策略

## 标记问题 (≥5)

1. **Q1: 服务端会话怎么缓存分区?** — partitionMap + update 增量 (FetchSession.scala:236,271-292)
2. **Q2: Epoch 怎么推进?** — INITIAL→递增→FINAL (FetchMetadata.java:37,43 + nextEpoch L64-65)
3. **Q3: 客户端 Handler 怎么维护会话?** — sessionId/nextMetadata/handleResponse (FetchSessionHandler.java:76-77,559,569)
4. **Q4: 失效重建流程?** — INVALID_SESSION_ID → 全量 (FetchSessionHandler.java:L559,578)
5. **Q5: 缓存淘汰策略?** — CacheShard (FetchSession.scala:599,655,659)
6. **Q6: 与 K-4 读路径衔接?** — ReplicaManager.processFetchRequest → FetchSession (K-4 篇 3/4)

## 已读测试 (2 个)

- `FetchSessionTest`: 会话生命周期/增量更新
- `FetchSessionHandlerTest`: 客户端会话管理/epoch 推进

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 (6 元素, 对应源码位置)
- [x] 6 个标记问题, 每个有源码位置
- [x] 已读 2 个测试文件

## 跨域发现

- 来源: K-12 Pass 1 — FetchSession 是 ReplicaManager 读路径的会话层 (K-4 篇 3 processFetchRequest AbstractFetcherThread.scala:L318 上游)
- 发现: Epoch 机制与 K-4 leader epoch 是两套独立递增体系 (会话 epoch vs 分区 epoch) — 面试易混点
- 已对照验证: K-4 AbstractFetcherThread.scala:318 processFetchRequest (K-4 pass2 实证)
