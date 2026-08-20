# R-8a RDB — fork COW 快照与二进制格式

> 前置: [[R-33-zmalloc]] (内存) + [[R-21-db]] (键空间遍历) + [[R-22-expire]] (过期持久化) + [[R-1-object]] (对象编码) + [[R-20-server]] (serverCron 调度) | 引出: [[R-9-replication]] (无盘复制 EOFMark) + [[R-8b-aof]] (混合持久化) | 对照: [[R-12-hll]] (magic 头辨识)
> 🔴 A (拆篇 1/2) | 6 KP | [模式: 紧凑二进制编码 + 流抽象 + fork 快照 + 原子替换 + 容错加载]
> Pass 2 闭环: q1(长度/整数编码) q2(rio 抽象) q3(对象序列化) q4(fork 快照) q5(文件格式) q6(加载与容错)

**读者处境**: dump.rdb 二进制里是什么? bgsave 怎么不阻塞主线程还保证一致? 为什么 RDB 文件小? 这篇拆 RDB: 长度编码体系、rio 流抽象、fork COW、原子 rename、加载容错。

### 1. 长度/整数编码 — 紧凑字节体系

场景: RDB 怎么把长度和整数压到最小?
源码路径:
- **长度四档** rdbSaveLen (rdb.c:151-184): <2^6 → 1B (L155-159) / <2^14 → 2B (L160-165) / ≤UINT32_MAX → 5B (L166-174) / 其余 → 9B (L175-183); 前 2 位 = 类型标记 (RDB_6BITLEN/14BITLEN/32BITLEN 0x80/64BITLEN 0x81, rdb.h:36-39)
- rdbLoadLenByRef (L194-238): 同构解码 + **RDB_ENCVAL 特殊值 → isencoded** (L205-208) — "长度"也可以是编码类型前缀
- **整数编码** rdbEncodeInteger (L258-280): INT8 (2B) / INT16 (3B) / INT32 (5B), 范围判定 (L260-278); 字符串先试整数编码 (rdbTryIntegerEncoding 语义)
- 时间戳: rdbSaveMillisecondTime (L119-123, LE 8B); **v9 字节序修复** (L125-146 注释: 5.0 前 BE 系统存错, 修复只对 ≥v9, 兼容老文件 L143)
关键设计 (q1): **2bit 类型 + 变长主体**: 常见小长度 1B, 大长度 9B — 压缩率与范围的平衡。[模式: 紧凑编码]
数据流: len → 档位判定 → [类型|值] 前缀 + 网络序主体。

### 2. rio 抽象 — 三实现流

场景: 同一套序列化代码怎么同时写文件/内存/socket?
源码路径:
- rio 函数表 (rio.c:89-100): read/write/tell/flush + checksum 回调 — **buffer/file/conn 三实现** (rioInitWithBuffer L102 / rioInitWithFile L199 / rioInitWithConn L303)
- rioBufferIO (L62-106): sds 累积, checksum=NULL
- rioFileIO (L111-200): fwrite + **autosync 增量写分批** (L114-131, "avoid a single write larger than the autosync threshold... kernel dirty pages"; REDIS_AUTOSYNC_BYTES=4MB, server.h:170)
- **checksum 钩子** rioGenericUpdateChecksum (L436): crc64 随读写累计
- rioSetAutoSync (L448) / rioSetReclaimCache (L458)
关键设计 (q2): **函数表注入**: rdb.c 只依赖 rio 接口, 目标设备切换零改动 (RDB→磁盘/内存/从库 socket)。[模式: 流抽象]
数据流: rdbSaveRio → rioWrite → 具体实现 (sds/文件/连接) + 校验和。

### 3. 对象序列化 — 26 类型分派

场景: 每种 Redis 对象在 RDB 里长什么样?
源码路径:
- **类型分派** rdbSaveObjectType (rdb.c:671-716): 按 (type, encoding) 双键映射 — string→RDB_TYPE_STRING / list→QUICKLIST_2 (L676-679) / set→INTSET|SET|SET_LISTPACK (L680-688) / zset→ZSET_2|ZSET_LISTPACK (L689-695) / **hash→HASH|HASH_LISTPACK|HASH_METADATA (HFE 面, L696-707)** / stream→STREAM_LISTPACKS_3
- **RDB_TYPE 宏族** (rdb.h:54-79): 类型号 0-7∪9-25 (**无 4/8 号**), rdbIsObjectType 校验 (rdb.h:80-83 "WHEN ADDING NEW RDB TYPE, UPDATE"); 含 7.x 演进痕迹 (HASH_METADATA_PRE_GA 22/23 与 24/25, 7.4 RC 兼容)
- rdbSaveObject (L829+): 按类型逐字段 — listpack/quicklist/intset **整包存储** (紧凑编码原样保留) vs dict/skiplist 逐项
- stream 深度序列化 (L734-825): PEL/消费者组 (nacks 参数 — 全局 PEL 带确认, 消费者 PEL 只存 ID 引用)
- 过期时间: rdbSaveKeyValuePair (L1190, EXPIRETIME_MS) + LRU/LFU 字段 (IDLE/FREQ opcode)
关键设计 (q3): **编码原样持久化**: listpack 等紧凑格式整包直存 (免重建); 类型号即版本号, 新增编码加新号 (向后兼容)。[模式: 类型分派]
数据流: robj → (type, encoding) → 类型号 + 字段序列化。

### 4. fork COW 快照 — bgsave

场景: 保存期间主线程还在写, 怎么保证一致性?
源码路径:
- rdbSaveBackground (rdb.c:1636-1670): **redisFork 子进程** (L1645) → 子进程 rdbSave (L1651, 共享内存快照) → exitFromChild (L1655); 父进程记录 dirty_before_bgsave (L1642) + rdb_child_type=DISK (L1666)
- **COW 语义**: fork 后子进程看到的是 fork 时刻的内存快照 — 主线程继续写只影响自己的页 (标注: COW 页复制, R-18 碎片交叉)
- rdbSaveInternal (L1522-1574): fopen 临时文件 → rioInitWithFile + **autoSync 增量 fsync** (L1546-1549) → fflush+fsync (L1558-1559) → **reclaimFilePageCache** (L1560) → 失败 unlink (L1571)
- **原子替换** rdbSave (L1593-1634): temp-pid.rdb → **rename** (L1607, 原子) → **fsyncFileDir** (L1621, 目录项落盘) → dirty=0 (L1629)
- 完成回调: backgroundSaveDoneHandler (L3804, 子进程退出状态 → INFO)
关键设计 (q4): **fork = 零拷贝快照**: 一致性靠进程内存隔离而非锁; rename 保证文件要么旧要么新。[模式: fork 快照+原子替换]
数据流: BGSAVE → fork → 子进程写 temp → rename → fsync 目录。

### 5. 文件格式 — magic 到 CRC64

场景: dump.rdb 整体布局?
源码路径:
- rdbSaveRio (rdb.c:1452-1500): **magic "REDIS%04d"** (L1462-1463, **RDB_VERSION=12**, rdb.h:20) → AUX 字段 (rdbSaveInfoAuxFields L1258: redis-ver/ctime/used-mem/repl-id...) → FUNCTIONS → 逐 db (L1470-1473) → MODULE_AUX → **EOF opcode** (L1478) → **CRC64 8B** (L1482-1485, 可关闭 rdb_checksum)
- AUX 约定 (rdb.c:3430-3465): **%前缀 = INFO 日志字段** (L3443-3449); repl-* 仅供复制恢复
- 无盘复制: rdbSaveRioWithEOFMark (L1501-1520): **"$EOF:<40B 随机>\\r\\n" 前缀 + 后缀回文** (L1511-1517) — 从库可判定流边界 (R-9 交叉)
- SELECTDB (254) / RESIZEDB (251) / SLOT_INFO (244, cluster slot 预扩) / EXPIRETIME_MS (252) / IDLE (248) / FREQ (249) / FUNCTION2 (245)
关键设计 (q5): **opcode 流式布局**: 每 opcode 自带语义, 未知 opcode 跳过 (AUX 注释 L3431-3434 "required to skip") — 向前兼容。[模式: opcode 流]
数据流: REDIS0012 | AUX* | FUNCTIONS | [SELECTDB|RESIZEDB|SLOT_INFO|EXPIRETIME|key-val]* | EOF | CRC64。

### 6. 加载主循环与容错 — 双模式错误处理

场景: 损坏的 RDB 怎么处理? 老版本文件能读吗?
源码路径:
- rdbLoadRioWithLoadingCtx (rdb.c:3328-3712): magic 校验 (L3340-3350, **版本 1..RDB_VERSION 门** L3347) → 主循环 opcode 分派 (L3356+): EXPIRETIME/FREQ/IDLE 是**前置属性** (L3364-3389, 作用于下一个 key) / SELECTDB 切库 + **dbnum 越界 exit** (L3396-3402) / RESIZEDB 预扩 (L3405-3413) / SLOT_INFO cluster 专用 (L3414-3429) / AUX 跳过
- **惰性重建**: 加载即建对象 (listpack 整包/逐项重建), dictExpand 预扩防 rehash 抖动
- **v9 字节序修复** (L140-146): rdbver>=9 才 memrev64ifbe — 大端老文件兼容
- **rdbReportError 三分支** (L46-87): RESTORE 上下文 (L58-62, 客户端报错不退出) / rdbCheckMode (L63-65, redis-check-rdb) / 加载中 (L66-75, **自动拉起 rdb 检查器 + exit**); 短读 vs 损坏区分 (L76-84)
- rdbLoad (L3713): 磁盘加载入口
关键设计 (q6): **容错分级**: RESTORE 用户输入报错, 磁盘加载损坏即终; 版本门 + opcode 跳过 = 新旧文件互读。[模式: 容错加载]
数据流: magic → 版本门 → opcode 循环 → 属性/键值/切库 → EOF+CRC。

### 负面空间 — RDB 刻意不做的事

- **不做增量持久化**: 全量快照, 两次保存间数据丢失窗口 (RDB 语义)
- **不做并发快照**: fork COW 而非加锁 (内存页共享)
- **不做对象级压缩**: listpack 原样, LZF 仅字符串
- **不做在线校验**: 损坏在加载期才暴露
- **不做跨版本自动升级**: 老版本直读, 新版本字段 AUX 跳过
- **不做单文件多段**: 一次全量 (7.0 Multi-Part AOF 才有分段, 见 R-8b)

→ 引出: 增量持久化怎么做到毫秒级丢失? → [[R-8b-aof]]
