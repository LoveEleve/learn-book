# 闭环笔记 q6: 命令面与内存 — PUBSUB 子命令 + 缓冲限制

## 假设
PUBSUB 5 子命令 (CHANNELS/NUMSUB/NUMPAT/SHARDCHANNELS/SHARDNUMSUB); 输出缓冲 pubsub 类限制; 订阅计数统计。

## 验证过程
- 命令版本 (commands.def): subscribe/psubscribe/unsubscribe/punsubscribe/publish **2.0.0**; pubsub **2.8.0**; spublish/ssubscribe/sunsubscribe **7.0.0** — shard 族是 7.0 新增
- pubsubCommand (pubsub.c:611-668):
  - CHANNELS [pattern] (L628-633): channelList (L670-695) — kvstore 全 slot 遍历 + glob 过滤 + 延迟数组长度 (addReplyDeferredLen)
  - NUMSUB [ch...] (L634-644): kvstoreDictFetchValue(slot 0) → dictSize (订阅客户端数, **不含模式订阅** — help 注释 L619-620)
  - NUMPAT (L645-647): server.pubsub_patterns dictSize
  - SHARDCHANNELS (L648-653) / SHARDNUMSUB (L654-664): **按 slot 取** (calculateKeySlot L659) — cluster 分片语义
- spublish (L698-703) / ssubscribe (L706-718, 无 MULTI 特例 L707-712 与全局不同) / sunsubscribe (L721-732)
- **输出缓冲限制 pubsub 类 {32MB 硬, 8MB 软, 60s}** (config.c:152, client-output-buffer-limit 三元组) — 慢消费者保护 (R-28 输出缓冲超限断连)
- 内存面: pubsubMemOverhead (L734-742) = 三 dict 用量 — clientMemUsage 组成部分 (R-28)
- pubsubTotalSubscriptions (L744-748) = patterns + channels + shard channels
- INFO 统计: pubsub_clients (server.c:5649) — markClientAsPubSub 计数
- **RESP2 白名单回顾** (server.c:4112-4125): 订阅态只能执行订阅族/ping/quit/reset — 防协议死锁 (订阅客户端不能发普通命令的 RESP2 语义)

## 代码类型
Command (家族) + 资源管理

## 跨域关联
- R-28: 输出缓冲/慢消费者 (已讲), clientMemUsage
- R-21: kvstore 遍历
- R-15 (未来): slot 语义

## 结论
命令面 = 全局/分片双轨 + 5 子命令内省; 资源保护 = pubsub 类输出缓冲三元组 (32/8/60); 统计 = pubsub_clients + MemOverhead。
源码位置: pubsub.c:611-748; config.c:152; server.c:5649
