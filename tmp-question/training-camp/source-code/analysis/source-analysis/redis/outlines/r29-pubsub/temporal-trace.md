# R-29 pubsub+notify — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (初版) | pubsub.c 版权 "2009-Present" — 与 Redis 同年; 2.0.0 发布 SUBSCRIBE/PSUBSCRIBE/UNSUBSCRIBE/PUNSUBSCRIBE/PUBLISH (commands.def 实证) |
| 2013 (2.8) | **keyspace notifications** 引入 (notify.c 版权 2013-Present; 2.8.0 PUBSUB 命令); 双频道 __keyspace@/__keyevent@ 格式定型 |
| 3.x | PUBLISH 传播语义: 非 cluster 模式复制到从库 (publishCommand forceCommandPropagation PROPAGATE_REPL — 当前代码形态); cluster 走 gossip |
| 6.0 | **RESP3 push 消息** — CLIENT_PUSHING (1ULL<<46)/addReplyPushLen; RESP2 订阅白名单与 RESP3 自由双轨 |
| 7.0 | **shard pubsub** (SPUBLISH/SSUBSCRIBE/SUNSUBSCRIBE); server.pubsub_channels 从 dict 改为 kvstore (cluster slot 分片, pubsubshard_channels 按 slot); acl-pubsub-default 配置 |
| 7.4 | 现状: pubsubtype 抽象参数化全局/分片; NOTIFY_NEW (n) 事件; updateClientMemUsageAndBucket 记账 |

## 痕迹证据

- pubsub.c:14-22: pubsubtype 结构注释 ("Currently used for pubsub and pubsubshard feature") — 抽象演进证据
- pubsub.c:294-298: 空频道删除注释 ("it will be possible to abuse Redis PUBSUB creating millions of channels") — 滥用防护动机
- pubsub.c:477-480: "Shard pubsub ignores patterns" — 分片语义简化
- notify.c:11-12: keyspace notifications 文档引用 (redis.io/topics/notifications)
- server.h:652: KEY_MISS "excluded from NOTIFY_ALL on purpose" — 设计决策痕迹
- server.c:4113-4125: RESP2 白名单注释 ("With RESP3 there are no limits")
- server.c:2081: notify_keyspace_events = 0 默认关闭

## 推断标注

- "3.x 传播语义定型" — 版本推断 (当前代码形态, 仓库浅克隆无法 git 验证精确版本)
- "RESP3 push 6.0 引入" — 版本推断 (RESP3 6.0 发布, CLIENT_PUSHING 随之)
- "stringmatchlen 非跨行 glob" — util.c:57-200 实现含 * ? [...] (标准 glob), 精确语义见 util.c (不逐字符展开)
