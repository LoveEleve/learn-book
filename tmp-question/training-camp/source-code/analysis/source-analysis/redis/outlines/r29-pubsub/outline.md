# R-29 Pub/Sub + Keyspace 通知 — 发布订阅与事件总线

> 前置: [[R-28-networking]] (客户端 IO/输出缓冲) + [[R-21-db]] (键空间) + [[R-3-dict]] (双镜像) + [[R-1-object]] (共享对象) | 引出: [[R-15-cluster]] (gossip 传播) + [[R-16-multi]] (命令上下文) | 对照: [[R-12-hll]] (MAY_REPLICATE 只读改值 vs publish 显式复制)
> 🟡 B | 6 KP | [模式: 双向注册 + 双镜像 + 分发两路 + 双协议格式 + 位掩码门 + 资源保护]
> Pass 2 闭环: q1(双订阅面) q2(订阅/退订) q3(分发) q4(消息格式) q5(keyspace 通知) q6(命令面)

**读者处境**: SUBSCRIBE 之后客户端还能发 GET 吗? PUBLISH 的消息会到从库吗? notify-keyspace-events 的字母串是什么意思? 订阅几百万频道会撑爆内存吗? 这篇拆发布订阅: 双向注册表、glob 模式匹配、RESP2/RESP3 双格式、从库复制语义、15 类位掩码通知。

### 1. 双订阅面 — 客户端 dict × 服务器镜像

场景: "我订了什么"和"谁订了这个频道"怎么都 O(1)?
源码路径:
- client 三 dict (server.h:1222-1224): pubsub_channels / pubsub_patterns / pubsubshard_channels — createClient 初始化 (networking.c:188-190)
- server 三镜像 (server.h:1989-1994): **kvstore** pubsub_channels (频道→客户端表) / **dict** pubsub_patterns / **kvstore** pubsubshard_channels (按 cluster slot)
- **pubsubtype 抽象** (pubsub.c:14-22): shard 标志 + 4 函数指针 (client dict/计数/镜像/消息类型) — 全局 (L54-62) 与分片 (L67-75) 两实例, 逻辑全参数化
- 计数: clientSubscriptionsCount = channels+patterns (L199-201); **CLIENT_PUBSUB 模式** (L222-234): 首订置位 + server.pubsub_clients++ (INFO 数据源, server.c:5649)
关键设计 (q1): **双层镜像**: 客户端记订阅、服务器记订阅者 — 双向 O(1) 互查; pubsubtype 让 7.0 的 shard 功能零重复代码。[模式: 双向注册]
数据流: SUBSCRIBE k → 客户端 dict + 服务器 kvstore 各记一笔。

### 2. 订阅/退订 — 双向注册与引用计数

场景: 订阅和退订的完整生命周期?
源码路径:
- pubsubSubscribeChannel (pubsub.c:238-271): **dictFindPositionForInsert 预定位** (L245, R-27 交叉) → 服务器 kvstoreDictAddRaw (L253, 新频道建 clients dict + incrRefCount L259-262) → 客户端 dictInsertAtPosition (L265) + 双引用 (L266)
- pubsubUnsubscribeChannel (L275-307): incrRefCount 保护 (L282-283) → 双向摘除 → **客户端表空则删频道** (L294-299, 注释 "abuse Redis PUBSUB creating millions of channels" 防滥用)
- 模式同构 (L340-389); 批量退订 (L393-411): **安全迭代器** (L396) + **零订阅仍回 null** (L407-408)
- cluster slot 迁移: pubsubShardUnsubscribeAllChannelsInSlot (L310-337); freeClient 断开自动退订 (networking.c:1548-1550)
关键设计 (q2): **双表注册 + 双引用**: 频道对象在两 dict 各持一引用; 空频道即删 = 频道滥用防护。[模式: 双向注册]
数据流: UNSUBSCRIBE → 客户端表摘除 → 服务器表摘除 → 空则删频道。

### 3. PUBLISH 分发 — 频道直发 + 模式 glob

场景: 一条消息怎么到所有订阅者? 到从库吗?
源码路径:
- pubsubPublishMessageInternal (pubsub.c:453-508): 频道 kvstore 查表直发 (L460-475, +updateClientMemUsageAndBucket L471) → **shard 类型跳过模式** (L477-480) → 模式全量扫 + **stringmatchlen glob** (L482-506, util.c:193, **nocase=0 区分大小写** L489)
- receivers = **消息投递数** (频道 L472 + 模式 L500 分计 — 同时订阅频道+匹配模式的一客户端计 2)
- publishCommand (L598-608): **sentinel 转发** (L599-601); `if (!server.cluster_enabled) forceCommandPropagation(PROPAGATE_REPL)` (L605-606) — **非 cluster 复制到从库** (从库二次分发); cluster 走 gossip (L590-595)
- 标志: CMD_PUBSUB|CMD_LOADING|CMD_STALE|CMD_FAST|CMD_MAY_REPLICATE|CMD_SENTINEL (2.0.0)
关键设计 (q3): **两路分发**: 频道 O(1) 精确 + 模式 O(模式数) glob; 传播双语义 — 单机/哨兵复制、cluster gossip。[模式: 分发两路]
数据流: PUBLISH ch msg → 频道表直发 → 模式表 glob → 从库复制/cluster gossip → receivers。

### 4. 消息格式 — RESP2 数组 vs RESP3 push

场景: 订阅消息在协议层长什么样? 订阅后能执行其他命令吗?
源码路径:
- addReplyPubsubMessage (pubsub.c:86-97): resp==2 → **mbulkhdr[3]** (L90) / RESP3 → **addReplyPushLen(3)** (L92); 模式消息 4 元素 (L102-114)
- 订阅/退订确认含**实时计数** (L126, L146); 批量零订阅 channel=NULL → null (L142-145)
- **CLIENT_PUSHING** (1ULL<<46, server.h:385): 置位→回复→复位, old_flags 防嵌套 (L87-88,96)
- **共享协议串** (server.c:1932-1940): messagebulk/subscribebulk/psubscribebulk 等 9 个预生成
- **RESP2 订阅白名单** (server.c:4112-4125): 仅 ping/subscribe 族/quit/reset — 注释 "With RESP3 there are no limits"
- pingCommand 订阅态特殊回复 ["pong",msg] (server.c:4596, pubsub 分支 L4603-4612); SUBSCRIBE 族拒 CLIENT_DENY_BLOCKING (pubsub.c:522, MULTI 豁免 — L707 shard 无豁免)
关键设计 (q4): **双轨协议**: RESP2 数组兼容老客户端 + RESP3 push 原生; 白名单防 RESP2 协议死锁。[模式: 双协议格式]
数据流: PUBLISH → 每客户端 addReplyPush/mbulkhdr → 输出缓冲 → 网络。

### 5. Keyspace 通知 — 位掩码门 + 双频道

场景: 怎么在键变化时收到通知? notify-keyspace-events 怎么配?
源码路径:
- **15 类位掩码** (server.h:641-656): K/E/g/$/l/s/h/z/x/e/t/m/d/n + LOADED; **NOTIFY_ALL = 10 类组合, 不含 K/E/m/n/LOADED** (L656, 注释 L652 "excluded on purpose")
- 配置字母映射: keyspaceEventsStringToFlags (notify.c:19-44) — **15 字符** (含 A), **LOADED 无字符映射仅 module 内部用**; "A" 判定 = (flags & NOTIFY_ALL)==NOTIFY_ALL (L54)
- notifyKeyspaceEvent (L83-124): **module 旁路优先** (L93, 绕过配置) → **type 位过滤** (L96) → **__keyspace@<db>__:<key>** (L101-110, 消息=事件名) / **__keyevent@<db>__:<event>** (L113-122, 消息=key)
- 默认 `notify_keyspace_events = 0` 全关 (server.c:2081); 消费端 15 文件 (db.c 9 处/t_*.c 60+ 处/expire/evict)
关键设计 (q5): **通知 = 位掩码门 + PUBLISH 转发**: 键空间操作打点, 通知面统一; K/E 双频道视角互换 (key→event / event→key)。[模式: 位掩码门]
数据流: SET k v → notifyKeyspaceEvent(NOTIFY_STRING,"set",k) → 过滤 → 双频道 PUBLISH。

### 6. 命令面与资源保护 — 子命令 + 缓冲限制

场景: 怎么内省订阅状态? 慢消费者怎么保护?
源码路径:
- 版本: 订阅族 2.0.0 / PUBSUB 2.8.0 / **shard 族 7.0.0** (commands.def)
- PUBSUB 5 子命令 (pubsub.c:611-668): CHANNELS (kvstore 全 slot 遍历+glob, L670-695) / NUMSUB (**kvstore bits=0 单 dict, 固定 slot 0**, L634-644, server.c:2691) / NUMPAT (L645-647) / SHARDCHANNELS / **SHARDNUMSUB 按 slot** (L654-664, calculateKeySlot db.c:205); slot 三函数等价性: getKeySlot (client 缓存优先 db.c:210) / keyHashSlot (cluster.h:43) / calculateKeySlot — cluster 下同为 CRC16, 非 cluster 恒 0
- **输出缓冲 pubsub 类 {32MB 硬, 8MB 软, 60s}** (config.c:152) — 慢消费者断连保护 (R-28)
- 内存: pubsubMemOverhead (L734-742) = 三 dict 用量 → clientMemUsage (R-28)
- pubsubTotalSubscriptions (L744-748); INFO pubsub_clients (server.c:5649)
关键设计 (q6): **内省 + 配额**: 子命令全读镜像表; 32/8/60 三元组防订阅洪水拖垮输出缓冲。[模式: 资源保护]
数据流: PUBSUB NUMSUB k → kvstore 查表 → dictSize。

### 负面空间 — Pub/Sub 刻意不做的事

- **不做消息持久化/积压**: 离线订阅者丢消息 (对照 Stream R-10 有 PEL/积压)
- **不做确认/重投**: fire-and-forget 语义
- **不做订阅读者组**: 无竞争消费 (对照 Stream 消费者组)
- **不做模式订阅计数**: NUMSUB 不含模式订阅者 (help 注释实证)
- **不做订阅持久化**: 重启后订阅全丢
- **不做单客户端多 DB 频道**: 频道全局唯一, 无 db 命名空间 (通知频道例外 @db)

→ 引出: 消息要持久化/积压/确认怎么办? → [[R-10-stream]]
