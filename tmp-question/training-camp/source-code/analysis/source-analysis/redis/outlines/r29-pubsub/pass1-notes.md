# R-29 pubsub+notify — Pass 1 探索笔记

> 域: R-29 Pub/Sub + Keyspace 通知 (pubsub.c + notify.c) | 🟡 B 方案 | 2026-08-13
> 源码: src/pubsub.c (748) + notify.c (124) | Redis 7.4.2

## 调用图

```
双订阅面数据结构:
pubsubtype 抽象 (pubsub.c:14-22): shard/client dict 获取/计数/kvstore 镜像/消息类型 — 全局 vs 分片两态
client 三 dict (server.h:1222-1224, networking.c:188-190 初始化): pubsub_channels/patterns/shard_channels
server 三镜像 (server.h:1989-1994): kvstore pubsub_channels (频道→客户端表) / dict pubsub_patterns / kvstore pubsubshard_channels (按 slot)
CLIENT_PUBSUB (1<<18) + server.pubsub_clients 计数 (pubsub.c:222-234)

订阅/退订:
pubsubSubscribeChannel (L238-271): 双向注册 (client dict + server kvstore) + dictFindPositionForInsert 预定位
pubsubUnsubscribeChannel (L275-307): 双向摘除 + 空频道删键 (防频道滥用)
pubsubSubscribePattern (L340-362) / Unsubscribe (L366-389): 同构双 dict
批量退订: pubsubUnsubscribeAllChannelsInternal (L393-411) / AllPatterns (L431-448) — 安全迭代器 + 零订阅仍回 null
cluster slot 迁移: pubsubShardUnsubscribeAllChannelsInSlot (L310-337)

分发:
pubsubPublishMessageInternal (L453-508): 频道直发 (kvstore 查表) → 模式 glob 分发 (stringmatchlen util.c:193)
publishCommand (L598-608): sentinel 转发 / 非 cluster → forceCommandPropagation PROPAGATE_REPL (从库复制)
pubsubPublishMessageAndPropagateToCluster (L590-595): cluster gossip (R-15 未来域)

消息格式:
addReplyPubsubMessage (L86-97): RESP2 mbulkhdr[3] / RESP3 push (CLIENT_PUSHING)
addReplyPubsubPatMessage (L102-114): 四元素含 pattern
共享消息类型: subscribebulk 等 (server.c:1932-1940)

Keyspace 通知 (notify.c):
NOTIFY_* 15 类位掩码 (server.h:641-656); notifyKeyspaceEvent (notify.c:83-124): module 旁路 → type 过滤 → __keyspace@<db>__:<key> 双频道格式
配置: notify-keyspace-events (server.c:2081 默认 0 全关)

命令面:
SUBSCRIBE 族 (2.0.0) / PUBSUB 子命令 5 个 (2.8.0, L611-668) / shard 族 (7.0.0)
RESP2 pubsub 白名单 (server.c:4112-4125): 仅 ping/subscribe 族/quit/reset
pingCommand pubsub 特殊回复 (server.c:4603-4612)
输出缓冲限制 pubsub 类 {32MB, 8MB, 60s} (config.c:152) — 慢消费者保护
pubsubMemOverhead (L734-742) / pubsubTotalSubscriptions (L744-748)
```

## 基本元素分解

1. 双订阅面: 客户端 dict (三) ↔ 服务器镜像 (kvstore/dict 三) 双向注册
2. pubsubtype 抽象: 全局/分片两态参数化
3. 分发两路: 频道精确 dict 查表 + 模式 glob 扫描 (stringmatchlen)
4. 消息格式: RESP2 数组 vs RESP3 push (CLIENT_PUSHING)
5. 传播语义: 非 cluster → 复制 PUBLISH 到从库; cluster → gossip; shard 按 slot
6. Keyspace 通知: 位掩码过滤 + 双频道格式 + 配置门

## 标记问题 (20 问)

1. pubsubtype 抽象解决什么问题?
2. 客户端三 dict 与服务器三镜像的关系?
3. 订阅为什么是双向注册? 引用计数?
4. dictFindPositionForInsert 预定位 (R-27 交叉)?
5. 空频道什么时候删除? 为什么?
6. 模式订阅的 glob 匹配算法?
7. RESP2/RESP3 的消息格式差异?
8. CLIENT_PUSHING 标志的作用?
9. PUBLISH 的从库复制语义?
10. cluster 的 PUBLISH 传播 (gossip)?
11. RESP2 pubsub 白名单 (为何)?
12. pingCommand 在 pubsub 模式下的特殊回复?
13. notify 的 15 类位掩码与字母映射?
14. __keyspace@ 与 __keyevent@ 双频道格式?
15. notify 为什么默认关闭?
16. module 旁路 (moduleNotifyKeyspaceEvent)?
17. 慢消费者的输出缓冲限制 (32/8/60)?
18. 批量退订零订阅时为什么回 null?
19. shard pubsub 与 cluster slot 的关系?
20. pubsubMemOverhead 的构成?

## 时空溯源 (代码内痕迹)

- 2.0.0: SUBSCRIBE/PSUBSCRIBE/UNSUBSCRIBE/PUNSUBSCRIBE/PUBLISH (commands.def 实证)
- 2.8.0: PUBSUB 命令; keyspace notifications (notify.c 版权 2013)
- 3.x: PUBLISH 传播语义演化 (非 cluster 复制到从库 — 当前代码 forceCommandPropagation)
- 6.0: RESP3 push 消息 (CLIENT_PUSHING/addReplyPushLen)
- 7.0: shard pubsub (SPUBLISH/SSUBSCRIBE/SUNSUBSCRIBE, commands.def 实证); kvstore 分片化 pubsub_channels

## 大域拆分判断

2 文件 872 行 — **不拆** (🟡 B, 6 闭环足够)
