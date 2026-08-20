# R-9b 增量同步 — replBacklog 共享缓冲与部分重同步

> 前置: [[R-9a-replication]] (全量同步) + [[R-28-networking]] (输出缓冲) + [[R-20-server]] (cron) | 引出: [[R-15-cluster]] (gossip) + [[R-14-sentinel]] | 对照: [[R-8b-aof]] (AOF 同源传播)
> 🔴 A (拆篇 2/2) | 6 KP | [模式: 共享块链 + 引用计数 + 环形裁剪 + 心跳确认 + ID 演进]
> Pass 2 闭环: q1(backlog 块链) q2(feed 共享写) q3(部分重同步) q4(ACK 心跳) q5(PSYNC2) q6(断线恢复)

**读者处境**: 从库断线 5 秒重连为什么不用全量? repl_backlog 环形缓冲怎么转? ACK 心跳怎么用? 这篇拆增量同步: 块链共享缓冲、引用计数裁剪、CONTINUE 续传、ACK/WAIT、replid 演进。

### 1. replBacklog — 共享块链与引用计数

场景: 主库最近 N 字节写命令怎么留存?
源码路径:
- 结构 (server.h:482-486): repl_buffer_blocks 块链 + **REPL_BACKLOG_TRIM_BLOCKS_PER_CALL=64** (L482) + **REPL_BACKLOG_INDEX_PER_BLOCKS=64** (L486, rax 索引)
- createReplicationBacklog (replication.c:102-113): **offset = master_repl_offset+1** (L112, 虚设"下一条"语义 L109-111)
- **块大小自适应** (L346-353): max(repl_backlog_size/16, PROTO_REPLY_CHUNK_BYTES=16KB) 上限 — 可裁与可填平衡
- **blocks_index rax** (L152-162): 每 64 块一个索引 (offset→块), 部分重同步 O(log n) 定位
- 裁剪: incrementalTrimReplicationBacklog (L242-295): **refcount==1 才可裁** (L261, 只有 backlog 引用) + 至少留 1 块 (L250) + "裁剪后不超限则停" (L265-266) + 新 head 引用转移 (L274-278)
关键设计 (q1): **块链 + 引用计数 = 主库/backlog/各从库共享同一批内存**: 零复制扇出; 裁剪只动 backlog 引用。[模式: 共享块链]
数据流: 写命令 → 块链尾部 → backlog/各从库按引用读取。

### 2. feedReplicationBuffer — 三方共享写

场景: 一条命令怎么同时给 backlog 和所有从库?
源码路径:
- feedReplicationBuffer (L315-413): 尾部追加 (L327-341) → 满则新块 (L342-372) → **每从库引用首块** (L382-387, 首次引用 refcount++) → **backlog 引用** (L393-397) → 新块时**增量裁剪** (L403-411, 每次最多 64 块)
- 引用位置: 每从库 ref_repl_buf_node + ref_block_pos (L382-384) — 从库各自推进读取位置
- 输出缓冲限制: 新块时 closeClientOnOutputBufferLimitReached (L390, 慢从库保护)
- replicationFeedSlaves (L421): 命令序列化 → feedReplicationBuffer (**AOF 同源** — R-8b 交叉)
- canFeedReplicaReplBuffer (L187-198): WAIT_BGSAVE_START/RDBONLY/CLOSE_ASAP 不喂
关键设计 (q2): **单次写入多方共享**: 内存零拷贝; 从库各自游标推进, 慢从库引用旧块天然保护 backlog 不被裁穿。[模式: 共享写]
数据流: 命令 → 块链 → 引用传播 → 各从库读自己的游标位置。

### 3. 部分重同步 — CONTINUE 续传

场景: 断线重连时 backlog 怎么续传?
源码路径:
- 主库侧: masterTryPartialResynchronization 成功 → **+CONTINUE** (L781-785, PSYNC2 带 replid) → addReplyReplicationBacklog (L790)
- **addReplyReplicationBacklog 定位** (L598-663): skip=offset-首字节 (L616) → **rax 近似定位块** (L619-644, > 游标找 ≤offset 索引) → 精确线性扫描 (L647-651) → **引用设置** (L654-660: 块 refcount++ + ref_repl_buf_node + ref_block_pos=offset-块起始) → 返回续传字节数 histlen-skip
- **范围半开区间** (L756-758): 允许 `[backlog.offset, backlog.offset+histlen]` = `[首字节, master_repl_offset+1]` — **从库已完全追上 (offset+histlen) 也可 CONTINUE (续 0 字节)**; harness 实证
- 从库侧: slaveTryPartialResynchronization 读半 (L2528-2574): **+CONTINUE 解析新 replid** (L2538-2562, 变化 → replid2=旧 ID + second_replid_offset=当前+1 L2550-2553 + **disconnectSlaves** L2561 通知下级) → **replicationResurrectCachedMaster** (L2567) → backlog 未建则建 (L2572)
- 请求侧: PSYNC replid **cached_master->reploff+1** (L2451-2453)
- 失败回退: 无缓存 → "?" + "-1" 强制全量 (L2456-2458); -NOMASTERLINK/-LOADING → TRY_LATER (L2583-2591)
关键设计 (q3): **offset 续传协议**: 从库报自己的处理偏移, 主库从 backlog 该点续发 — 断线期间的命令都在 backlog 则无需全量。[模式: 续传]
数据流: 从库断线 → 重连 PSYNC(offset+1) → +CONTINUE → backlog 剩余数据。

### 4. ACK 心跳 — REPLCONF ACK/GETACK

场景: 主库怎么知道从库活着且追上了?
源码路径:
- **从库每秒 ACK** (replicationCron L3739-3744 → replicationSendAck L3254): REPLCONF ACK <offset>; **CLIENT_PRE_PSYNC 老协议从库不 ACK** (L3741-3742)
- 主库处理 replconfCommand (L1184-1213): ACK offset → repl_ack_off (L1192-1193) + **fack aof-offset** (L1194-1198, WAITAOF) + repl_ack_time 刷新 (L1200); 无回复 (L1213)
- **GETACK** (L1215-1218): 请求立即 ACK (WAIT 命令触发)
- **断线检测** (cron L3720-3732): TRANSFER 超时 (L3720-3726) / **CONNECTED 超时 = master->lastinteraction 超 repl_timeout → freeClient(master)** (L3727-3732) / PING 从库 (L3747+)
- **WAIT** (L3521): 主库专用 (L3525-3527) → 先非阻塞尝试 (L3544-3548) → 不满足则 **blockForReplication 阻塞客户端** (L3551) + **replicationRequestAckFromSlaves 主动拉 ACK** (L3552-3553, 触发 GETACK) — 强一致读
关键设计 (q4): **ACK = 处理进度 + 存活证明**: offset 单调推进, 主库据此判断断线与同步状态; GETACK 主动拉取 (WAIT 场景)。[模式: 心跳确认]
数据流: 每秒 REPLCONF ACK offset → 主库更新 ack_off/ack_time → WAIT/断线判定。

### 5. PSYNC2 — replid 演进

场景: 主库切换后旧从库怎么还能部分重同步?
源码路径:
- **replid/replid2 双 ID** (L1679-1715): changeReplicationId (L1679) / clearReplicationId2 (L1687) / **shiftReplicationId** (L1698-1715): 新主库生成时旧 replid 移入 replid2 + **second_replid_offset = master_repl_offset+1** (L1704-1706, **权威注释: 从库请求的是第一个未收到的字节 = offset+1**)
- 从库侧 ID 传播 (L2546-2562): +CONTINUE 新 replid → 本地 replid 更新 + 旧 ID 存 replid2
- 主库裁决 (L730-753): 匹配 replid1 或 (replid2 且 offset ≤ second_replid_offset) — 级联从库可用旧 ID 续传
- **RDB 持久化 replid** (AUX 字段, R-8a 交叉): 重启后保留 replid2 续传能力
关键设计 (q5): **ID 代际演进**: 主库换代保留一代历史 (replid2), 级联拓扑断线后可跨代续传。[模式: ID 演进]
数据流: 主库切换 → 新 replid + replid2=旧 → 从库按 replid2 续传。

### 6. 断线恢复 — cached_master 与复活

场景: 断线后从库怎么快速恢复?
源码路径:
- **replicationCacheMaster** (L3292): 断线时把 master 客户端转缓存 (保留 replid/reploff) — **backlog 数据仍引用** (L3292+, 从库侧 backlog)
- replicationDiscardCachedMaster (L3371): 缓存过期/换代时丢弃
- **replicationResurrectCachedMaster** (L3386): +CONTINUE 后把缓存恢复为活跃 master 客户端
- 断线期间: 从库继续服务读 (replica-serve-stale-data) / replicationHandleMasterDisconnection (L3106)
- replicationCron 重连 (L3704+): 超时后重新 connectWithMaster (L2921)
关键设计 (q6): **缓存客户端 = 断线续传的会话保持**: replid/offset 在手, 重连即 PSYNC; 缓存丢弃 = 强制全量。[模式: 会话缓存]
数据流: 断线 → 缓存 master 状态 → 重连 PSYNC → +CONTINUE 复活。

### 负面空间 — 增量同步刻意不做的事

- **不做无限保留**: backlog 有界 (默认 1MB, repl-backlog-size), 超出即全量
- **不做逐命令确认**: ACK 是偏移级非命令级 (批量)
- **不做多代 replid**: 只保留一代 (replid2), 更久历史需全量
- **不做从库间直接同步**: 一律经主库 (级联也是树形)
- **不做增量压缩**: 命令原样传输
- **不做乱序交付**: 单连接顺序流, 断线重连全量或续传

→ 引出: 高可用怎么自动选主? → [[R-14-sentinel]]
