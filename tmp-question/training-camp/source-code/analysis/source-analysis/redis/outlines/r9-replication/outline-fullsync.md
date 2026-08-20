# R-9a 全量同步 — 握手状态机与 RDB 传输

> 前置: [[R-8a-rdb]] (RDB 载体/EOFMark) + [[R-28-networking]] (连接抽象) + [[R-2-events]] (事件驱动) + [[R-16-multi]] (传播面) | 引出: [[R-9b-replication]] (增量同步) + [[R-14-sentinel]] | 对照: [[R-8-rdb-aof]] (持久化 vs 复制)
> 🔴 A (拆篇 1/2) | 6 KP | [模式: 状态机握手 + 裁决协议 + 双模式传输 + 延迟应答]
> Pass 2 闭环: q1(握手状态机) q2(PSYNC 裁决) q3(FULLRESYNC) q4(RDB 传输) q5(从库接收) q6(上线)

**读者处境**: 从库连上主库后发生了什么? PSYNC 怎么知道要不要全量? 全量传输为什么分磁盘/无盘? 这篇拆全量同步: 9 步握手状态机、PSYNC 裁决、FULLRESYNC 延迟应答、RDB 双模式传输。

### 1. 握手状态机 — PING→AUTH→REPLCONF→PSYNC

场景: 从库连接主库的完整协议流程?
源码路径:
- **REPL_STATE 13 状态全集** (server.h:430-444): NONE/CONNECT/CONNECTING/RECEIVE_PING/SEND_HANDSHAKE/RECEIVE_AUTH/RECEIVE_PORT/RECEIVE_IP/RECEIVE_CAPA/SEND_PSYNC/RECEIVE_PSYNC/**TRANSFER (收 RDB)**/**CONNECTED** — 握手子集 9 个
- syncWithMaster (replication.c:2608) 状态机:
  - **CONNECTING → RECEIVE_PING_REPLY** (L2629-2640): 非阻塞连接成功后发 **PING** (L2638) — 探测主库存活
  - PONG 校验 (L2644-2668): 接受 +PONG / -NOAUTH / -NOPERM (兼容老版本 L2652-2654)
  - **SEND_HANDSHAKE** (L2672-2727): **AUTH** (L2674-2688) + **REPLCONF listening-port** (L2692-2705) + **REPLCONF ip-address** (L2710-2714) + **REPLCONF capa eof psync2** (L2722-2724, 能力声明)
  - 依次收 AUTH/PORT/IP/CAPA 回复 (L2730-2794, **无 masterauth 跳 AUTH** L2730-2731, 无 announce-ip 跳 IP L2763-2764; 老版本不认识选项的错误可忽略 L2752-2757)
  - **SEND_PSYNC** (L2801-2809) → RECEIVE_PSYNC_REPLY
- 超时: replicationCron (L3704+): 握手超时取消 (L3712-3718) / TRANSFER 超时 (L3720-3726) / CONNECTED 超时 (L3727-3732)
关键设计 (q1): **逐段推进的事件驱动状态机**: 每步发一个命令等一个回复, 可超时可重试 — 老主库不认识的 REPLCONF 选项静默忽略 (兼容)。[模式: 状态机握手]
数据流: PING → PONG → AUTH → REPLCONF×3 → PSYNC。

### 2. PSYNC 裁决 — 主库决定部分还是全量

场景: PSYNC replid offset 主库怎么裁决?
源码路径:
- masterTryPartialResynchronization (L718-814):
  - **replid 双 ID 校验** (L730-753): 匹配 replid1 或 (replid2 且 offset ≤ second_replid_offset); "?" 强制全量 (L734-735)
  - **backlog 范围校验** (L756-767): `offset ≤ psync_offset ≤ offset+histlen` — 太旧(被裁)/未来(超主库) → 全量
  - 成功: **+CONTINUE [replid]** (L781-785, PSYNC2 带 replid) + **addReplyReplicationBacklog 发送 backlog** (L790) + 立即 ONLINE (L774)
  - 失败 → need_full_resync (L808-813)
- 部分重同步统计: stat_sync_partial_ok/err (L998-1007)
关键设计 (q2): **双 ID + 范围双检**: replid2 是主库换代的历史 ID (PSYNC2), backlog 范围决定数据可用性。[模式: 裁决协议]
数据流: PSYNC replid offset → 双 ID 校验 → 范围校验 → +CONTINUE+backlog / 全量。

### 3. FULLRESYNC — 延迟应答

场景: 为什么 +FULLRESYNC 不立即回复?
源码路径:
- 全量路径设置: syncCommand (L1016-1026): SLAVE_STATE_WAIT_BGSAVE_START + CLIENT_SLAVE + 加入 server.slaves
- **backlog 首次创建** (L1029-1039): 第一个从库 → changeReplicationId + createReplicationBacklog
- **四情形复用** (L1041+): BGSAVE 进行中(disk/socket) / 无 → startBgsaveForReplication (L834)
- **+FULLRESYNC replid offset 延迟到 RDB 就绪** (L689 replicationSetupSlaveForFullResync + L808-813 注释 "must include the master offset at the time the RDB file we transfer is generated") — offset 是 RDB 生成时刻的偏移, 从库以此续传
- startBgsaveForReplication (L834-913): disk/socket 目标选择 (mincapa 协商) + 脚本缓存清空
关键设计 (q3): **应答携带 RDB 时刻偏移**: 从库拿到 RDB 后知道从哪个 offset 继续 — 全量+增量无缝衔接。[模式: 延迟应答]
数据流: PSYNC → 全量 → bgsave 启动 → RDB 就绪 → +FULLRESYNC replid offset。

### 4. RDB 传输 — 磁盘文件 vs 无盘管道

场景: RDB 怎么推给从库? 两种模式?
源码路径:
- **磁盘模式**: sendBulkToSlave (L1385): 主库 open repldbfd → **先发 "$<len>\r\n" RESP 批量前导** (L1392-1409) → 流式读文件写入从库 socket
- **无盘模式** (diskless-sync): **rdbPipeReadHandler** (L1487): 子进程写管道 → 主库读管道 → 扇出多从库 (L1462-1487) — 内存零磁盘
- **多从库共享一次 bgsave** (syncCommand CASE 1 L1041-1078): 磁盘模式靠 **能力/需求匹配** (slave_capa 子集 + slave_req 相等, L1065-1066) + **copyReplicaOutputBuffer 复制差异缓冲** (L1070-1072) + 复用 psync_initial_offset — 新从库挂到进行中的 bgsave
- 过滤 RDB (REPLCONF RDB-FILTER-ONLY, L1229-1255): functions 过滤器 (SLAVE_REQ_RDB_EXCLUDE_*)
- updateSlavesWaitingBgsave (L1590): bgsave 完成后依次发 RDB
- EOFMark 包裹 (R-8a 交叉): 无盘模式 $EOF:mark 判定边界 (L1620-1634 "wait for REPLCONF ACK... to enable streaming")
关键设计 (q4): **传输双模式**: 磁盘模式简单可靠, 无盘模式免磁盘 IO (首从库延迟低); 多从库共享一次 bgsave 管道扇出。[模式: 双模式传输]
数据流: bgsave → 磁盘文件/管道 → 逐从库推流。

### 5. 从库接收 — 临时文件与空库交换

场景: 从库收到 RDB 后怎么处理?
源码路径:
- 接收准备 (L2863-2877): **临时文件 temp-<unixtime>-<pid>.rdb** (L2866-2867, O_EXCL 防冲突) 或 diskless 内存
- readSyncBulkPayload (L1853): 逐块读入 → 写临时文件 (L1853+) → 读完后:
  - **磁盘加载**: 清空旧数据 → rdbLoad 临时文件 → rename 到 dump.rdb
  - **无盘加载** (useDisklessLoad L1802): **临时空 db 交换** (replicationEmptyDbCallback L1741 + replicationAttachToNewMaster L1841) — 加载完成后原子切换
- 加载期间阻塞 (loading 状态)
- AOF 交互: restartAOFAfterSYNC (L1785)
关键设计 (q5): **全量替换语义**: 从库数据整体被主库 RDB 覆盖; 空库交换保证加载期间对外不可见旧数据。[模式: 快照替换]
数据流: RDB 流 → 临时文件 → 加载 → 原子切换。

### 6. 上线 — 首 ACK 开流

场景: 从库什么时候开始接收增量命令?
源码路径:
- replicaPutOnline (L1275): SLAVE_STATE_ONLINE (L1275+)
- **replicaStartCommandStream** (L1308): 通知主库开始推命令流 — **延迟到首 REPLCONF ACK** (L1211-1212: `repl_start_cmd_stream_on_ack && ONLINE → replicaStartCommandStream`) — 无盘模式必须等 ACK 确认 RDB 收完 (L1201-1208 注释)
- 从库侧确认: REPLCONF ACK 首帧 (diskless 上线信号)
- 上线后: 从库只读 (replica-read-only) + 过期不删 (R-21 交叉)
关键设计 (q6): **ACK 门控**: RDB 边界靠 EOFMark 判定, 增量边界靠 ACK 确认 — 双协议握手完成。[模式: 确认门控]
数据流: RDB 完成 → 从库 ACK → 主库开命令流 → 增量阶段。

### 负面空间 — 全量同步刻意不做的事

- **不做增量 RDB**: 全量快照必然全传 (小数据集优化靠磁盘共享)
- **不做断点续传**: RDB 传一半断了重来 (无 chunk 校验)
- **不做多从库独立 bgsave**: 共享一次快照 (一致性优先)
- **不做加载期服务**: 从库加载时阻塞 (diskless 空库交换减轻但仍有窗口)
- **不做 RDB 校验和逐块验证**: 全文件 CRC (加载期)

→ 引出: 全量之后增量怎么保证不丢? → [[R-9b-replication]]
