# R-9 复制 — Pass 1 探索笔记 (大域拆 2 篇)

> 域: R-9 主从复制 (replication.c) | 🔴 A 方案 | 2026-08-13
> 源码: src/replication.c (4231) | Redis 7.4.2
> 拆篇: **R-9a 全量同步** (握手/PSYNC/RDB 传输) / **R-9b 增量同步** (backlog/部分重同步/ACK)

## 调用图

```
全量同步 (R-9a):
从库侧: connectWithMaster (L2921) → syncWithMaster 状态机 (L2608):
  CONNECTING → RECEIVE_PING_REPLY (PING, L2629-2640) → SEND_HANDSHAKE (AUTH + REPLCONF listening-port/ip-address/capa eof psync2, L2672-2727)
  → RECEIVE_AUTH/PORT/IP/CAPA_REPLY → SEND_PSYNC (L2801-2809) → RECEIVE_PSYNC_REPLY
slaveTryPartialResynchronization (L2437): 写半 PSYNC replid offset (cached_master 才有部分机会, L2451-2458; 无缓存 → "?" + "-1" 强制全量)
  → 读半: +FULLRESYNC (L2497, 解析 replid+offset) / +CONTINUE (L2528) / -NOMASTERLINK|-LOADING → TRY_LATER (L2583) / 其他 → NOT_SUPPORTED (L2593)

主库侧: syncCommand (L915): PSYNC FAILOVER (L921-945) / 状态检查 / **masterTryPartialResynchronization** (L718):
  replid 双 ID 校验 (L730-753, replid2 仅到 second_replid_offset) → backlog 范围校验 (L756-767) → 部分成功: +CONTINUE + addReplyReplicationBacklog (L781-790)
  失败 → 全量: replicationSetupSlaveForFullResync (L689) → +FULLRESYNC replid offset (延迟到 RDB 生成时, L808-813)
RDB 传输: startBgsaveForReplication (L834, disk/socket 目标) → 磁盘: sendBulkToSlave (L1385, repldbfd 流式) / 无盘: rdbPipeReadHandler (L1487, 管道扇出) → updateSlavesWaitingBgsave (L1590)
从库接收: readSyncBulkPayload (L1853): 临时文件 temp-unixtime-pid.rdb → 加载 (diskless: 临时 db 空库) → replicationAttachToNewMaster (L1841)
上线: replicaPutOnline (L1275) + replicaStartCommandStream (L1308, 首 ACK 后开流 L1211-1212)

增量同步 (R-9b):
replBacklog (L102-413): repl_buffer_blocks 块链 + refcount 引用计数 + blocks_index rax (REPL_BACKLOG_INDEX_PER_BLOCKS=64 索引, server.h:486) + histlen/offset
feedReplicationBuffer (L315-413): 共享缓冲 — 尾部追加 + 新块 + 从库引用 + backlog 引用 + 增量裁剪 (REPL_BACKLOG_TRIM_BLOCKS_PER_CALL=64, server.h:482)
incrementalTrimReplicationBacklog (L242-295): 引用计数 1 才可裁 + 至少留 1 块 + histlen 超限
replicationFeedSlaves (L421): 命令 → 共享缓冲 (AOF 同源)
部分重同步: addReplyReplicationBacklog + masterTryPartialResynchronization 成功路径
ACK: replconfCommand (L1148): REPLCONF ACK offset [fack aof-offset] (L1184-1213) / GETACK (L1215-1218, WAIT 用) / RDB-ONLY (L1220) / RDB-FILTER-ONLY (L1229)
replicationSendAck (L3254): 从库每秒 ACK (cron)
PSYNC2: shiftReplicationId (L1698) / changeReplicationId (L1679) / replid2+second_replid_offset (从库切换主库时保留历史)
断线: replicationCacheMaster (L3292) / replicationDiscardCachedMaster (L3371) / replicationResurrectCachedMaster (L3386) / replicationHandleMasterDisconnection (L3106)
replicationCron (L3704): 握手超时 / ACK 检测 / 心跳

WAIT 族: waitCommand (L3521) / replicationCountAcksByOffset (L3487) / replicationCountAOFAcksByOffset (L3504)
```

## 基本元素分解

**R-9a 全量**:
1. 从库握手状态机 (PING/AUTH/REPLCONF/PSYNC 四段)
2. PSYNC 主库裁决 (replid 双 ID + backlog 范围)
3. FULLRESYNC 应答 (延迟到 RDB 就绪)
4. RDB 传输双模式 (磁盘文件流 / 无盘 socket 管道)
5. 从库接收与加载 (临时文件/空库交换)
6. 上线协议 (首 ACK 开流)

**R-9b 增量**:
1. repl_buffer_blocks 块链 + refcount + rax 索引
2. feedReplicationBuffer 共享写入 (主库/backlog/从库三方引用)
3. 部分重同步 (CONTINUE + backlog 数据)
4. ACK 心跳 (offset 追踪 + 断线检测 + GETACK)
5. PSYNC2 replid 演进 (replid2/second_replid_offset/移位)
6. 断线缓存恢复 (cached_master + resurrect)

## 标记问题 (40 问)

R-9a: 握手状态机几步? / PING 为何? / AUTH 失败? / capa eof psync2 含义? / PSYNC offset 语义 (offset+1)? / replid "?" 强制全量? / replid2 有效范围? / FULLRESYNC 何时回复? / 磁盘 vs 无盘传输? / EOFMark 无盘? / 临时文件命名? / diskless 空库交换? / 首 ACK 开流? / REPLCONF RDB-ONLY? / RDB-FILTER-ONLY functions? / 主库拒绝条件? / 级联复制? / 从库只读? / MASTER 断开重连? / 握手超时?

R-9b: backlog 块大小? / refcount 语义? / rax 索引 64 块? / histlen vs size? / 裁剪条件 (refcount==1)? / 至少留 1 块? / offset 语义 (首字节)? / 部分重同步数据范围? / ACK 频率? / GETACK 用途 (WAIT)? / fack AOF 偏移? / replid2 何时设? / shiftReplicationId 时机? / cached_master 缓存什么? / resurrect 恢复? / 断线后 backlog 保留? / WAIT 计数? / 多级级联 replid 传播? / 增量裁剪 max_blocks? / 重启后 backlog 重建 (rebase)?

## 时空溯源 (代码内痕迹)

- 2009: SYNC 全量复制 (主从雏形)
- 2.8: **PSYNC 部分重同步** (repl_backlog 引入) + REPLCONF
- 3.0: **无盘复制** (diskless, rdbPipeReadHandler)
- 4.0: **PSYNC2** (replid/replid2 双 ID 演进 — shiftReplicationId 注释 L1698+) + 混合持久化配合
- 6.2: WAIT 命令
- 7.0: 共享 repl_buffer_blocks (多从库零复制) + RDB-ONLY/RDB-FILTER-ONLY (REPLCONF 扩展)
- 7.4: fack AOF 偏移 (WAITAOF)

## 大域拆分判断

4231 行单文件 — **拆 2 篇** (R-9a 全量 6 KP / R-9b 增量 6 KP) + 双 harness
