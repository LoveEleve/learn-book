# 闭环笔记 q3: PUBLISH 分发 — 频道直发 + 模式 glob + 传播语义

## 假设
频道精确查表直发; 模式 stringmatchlen glob 扫描; 非 cluster 复制到从库。

## 验证过程
- pubsubPublishMessageInternal (pubsub.c:453-508):
  - 频道分发 (L460-475): kvstoreDictFind 查频道 → 客户端表逐 client addReplyPubsubMessage (L470) + **updateClientMemUsageAndBucket** (L471, R-28 内存记账)
  - **shard 类型直接返回** (L477-480): 分片频道不做模式匹配 — 注释 "Shard pubsub ignores patterns"
  - 模式分发 (L482-506): 全量扫 server.pubsub_patterns; **stringmatchlen glob** (L489-492, util.c:193, 支持 */?/[...] — 标注: * 不匹配跨无字符? 非 glob 语义); getDecodedObject 解码 (L485); 每命中模式再遍历客户端表 addReplyPubsubPatMessage (L498)
  - 返回 receivers 计数 (收到消息的客户端数, 非消息条数)
- publishCommand (L598-608):
  - **sentinel 模式转发** sentinelPublishCommand (L599-601)
  - `if (!server.cluster_enabled) forceCommandPropagation(c,PROPAGATE_REPL)` (L605-606) — **非 cluster 时 PUBLISH 复制到从库** (从库收到后给它的订阅者分发); cluster 时不复制 (走 gossip)
  - 回复 receivers 数 (L607)
- pubsubPublishMessageAndPropagateToCluster (L590-595): cluster_enabled → clusterPropagatePublish (gossip, R-15 未来域)
- 命令标志 (commands.def): publish CMD_PUBSUB|CMD_LOADING|CMD_STALE|CMD_FAST|CMD_MAY_REPLICATE|CMD_SENTINEL (2.0.0) — MAY_REPLICATE 与 forceCommandPropagation 配合 (R-12 对照: pfcount 是唯一 READONLY+MAY_REPLICATE)

## 代码类型
Mechanism (分发)

## 跨域关联
- R-28: updateClientMemUsageAndBucket / 传播面
- R-12: MAY_REPLICATE 对照 (pfcount 只读改值 vs publish 显式复制)
- R-15 (未来): cluster gossip

## 结论
两路分发: 频道 O(1) 精确 + 模式 O(模式数) glob 扫描; 传播双语义 — 非 cluster 复制给从库 (从库二次分发), cluster 走 gossip。receivers = 客户端数。
源码位置: pubsub.c:453-508,590-608; commands.def publish
