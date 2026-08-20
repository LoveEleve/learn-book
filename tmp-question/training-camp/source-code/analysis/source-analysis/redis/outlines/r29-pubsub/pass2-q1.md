# 闭环笔记 q1: 双订阅面 — 客户端 dict × 服务器镜像

## 假设
客户端侧 3 dict (channel/pattern/shard) + 服务器侧镜像 (kvstore/dict), 双向注册, pubsubtype 参数化。

## 验证过程
- client 三 dict (server.h:1222-1224): `pubsub_channels` (SUBSCRIBE) / `pubsub_patterns` (PSUBSCRIBE) / `pubsubshard_channels` (SSUBSCRIBE) — createClient 初始化 (networking.c:188-190, objectKeyPointerValueDictType)
- server 三镜像 (server.h:1989-1994): `kvstore pubsub_channels` (频道→客户端表, 键=robj 频道) / `dict pubsub_patterns` / `kvstore pubsubshard_channels` (按 cluster slot)
- **pubsubtype 抽象** (pubsub.c:14-22): shard 标志 + 4 函数指针 (client dict 获取/订阅计数/服务器镜像/3 消息类型对象) — 全局 (L54-62) 与分片 (L67-75) 两实例, 订阅/退订/分发函数全参数化
- 计数: clientSubscriptionsCount = channels+patterns (L199-201) / Shard 仅 shard (L204-206) / Total = 两者和 (L218-220); serverPubsubSubscriptionCount (L189-191)
- **CLIENT_PUBSUB 模式** (L222-234): 首次订阅置位 + server.pubsub_clients++ — INFO pubsub_clients 数据源 (server.c:5649)

## 代码类型
Structure (双镜像注册表)

## 跨域关联
- R-3 (已交付): dict 复用 (client 侧 objectKeyPointerValueDictType)
- R-21 (已交付): kvstore 分片 (R-21 已讲 14bit=16384 槽) — pubsubshard 用同机制
- R-27 (已交付): dictFindPositionForInsert 预定位 (setTypeAdd 同 API)

## 结论
双层镜像: 客户端记"我订了什么", 服务器记"谁订了这个频道" — 双向 O(1) 互查; pubsubtype 让全局/分片共享同一套逻辑。
源码位置: server.h:1222-1224,1989-1994; pubsub.c:14-75,199-234
