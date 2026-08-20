# K-12 FetchSession — 知识规划 (00 §10: 逐源提取→聚合→分类→聚类)

> 2026-08-15 | 源码: core/src/main/scala/kafka/server/FetchSession.scala (908 行) + clients/src/main/java/org/apache/kafka/clients/FetchSessionHandler.java (628 行) + FetchMetadata (常量)
> [索引覆盖: scala 未在 codebase-memory 索引, 锚点 grep+Read 实证; Java 端已索引]

## 01 逐源提取

| 源文件 | 机制点 |
|---|---|
| FetchSession.scala | ①object 常量 (FetchMetadata.java:L37) ②class FetchSession (FetchSession.scala:L236: id/epoch/partitionMap) ③update 增量三元组 (FetchSession.scala:L271-292: added/updated/removed) ④CacheShard/Cache 淘汰 (FetchSession.scala:L599,805) ⑤newSessionId (FetchSession.scala:L655) ⑥INVALID_SESSION_ID 响应 (FetchSession.scala:L337,356,386) |
| FetchSessionHandler.java | ①class (FetchSessionHandler.java:L60) ②sessionId/nextMetadata (FetchSessionHandler.java:L76-77) ③handleResponse 会话失效处理 (FetchSessionHandler.java:L559,578) ④新会话/增量切换 (FetchSessionHandler.java:L569,604) |
| FetchMetadata.java | 常量: INVALID_SESSION_ID=0 (FetchMetadata.java:L31) / INITIAL_EPOCH=0 (FetchMetadata.java:L37) / FINAL_EPOCH=-1 (FetchMetadata.java:L43) / nextEpoch (FetchMetadata.java:L64-65) |

## 02 聚合 (P1/P2/P3)

| 聚合机制 | 来源 | 分级 |
|---|---|---|
| 会话模型 (id+epoch+缓存分区) | FetchSession + Handler | P1 |
| 增量更新三元组 (added/updated/removed) | FetchSession.update | P1 |
| Epoch 生命周期 (INITIAL→递增→FINAL/INVALID) | FetchMetadata + Handler | P1 |
| 会话失效处理 (INVALID_SESSION_ID → 重建全量) | Handler.handleResponse | P1 |
| 服务端缓存淘汰 (CacheShard LRU 类) | FetchSessionCache | P2 |
| 与 K-4 读路径衔接 (processFetchRequest) | ReplicaManager | P2 |

## 03 深度分类

- 🔴: 会话增量协议 (定义 FetchSession 机制) — 但域整体 🟡 (优化机制非定义特征)
- 🟡: Epoch 生命周期 / 缓存淘汰
- 🟢: 常量定义 (FetchMetadata)

## 04 聚类 (教学顺序)

```
问题: 1000 分区每次 fetch 全量元数据 → 会话缓存方案
客户端侧: FetchSessionHandler (session 管理/epoch 推进) 
  → 服务端: FetchSession (缓存分区/增量 diff)
  → Epoch 机制 (判新旧/失效重建)
  → 缓存淘汰 (FetchSessionCache)
  → 与 K-4 读路径衔接
```

**拆篇建议**: 2 篇 (🟡 B)
- 01: 会话模型与增量协议 (id/epoch/added-updated-removed)
- 02: 生命周期与衔接 (失效重建/缓存淘汰/K-4 衔接)
