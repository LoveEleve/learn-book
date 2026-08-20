# R-8b AOF — 命令日志、三策略与重写

> 前置: [[R-8a-rdb]] (混合持久化 preamble) + [[R-20-server]] (beforeSleep 调度) + [[R-28-networking]] (传播面) + [[R-2-events]] (bio 线程/事件循环) | 引出: [[R-9-replication]] (同步流) + [[R-14-sentinel]] | 对照: [[R-8a-rdb]] (全量 vs 增量)
> 🔴 A (拆篇 2/2) | 6 KP | [模式: 写前日志 + 策略分级 + 延期合并 + 后台重写 + 容错重放]
> Pass 2 闭环: q1(三策略) q2(flush 细节) q3(feed 序列化) q4(重写) q5(重写生命周期) q6(加载)

**读者处境**: appendfsync everysec 到底丢多少? 重写时新写入的命令怎么不丢? 重启加载 AOF 比 RDB 慢为什么还用? 这篇拆 AOF: 三策略、写前缓冲、fsync 调度、fork 重写、manifest 多部分。

### 1. 三策略 — always/everysec/no

场景: 每次写命令 fsync 吗? 丢数据的权衡?
源码路径:
- **appendfsync 三态** (config.c:77-79, 3134): everysec (默认) / always / no; aof_state 三态 AOF_ON/OFF/WAIT_REWRITE (server.h)
- **写前缓冲**: feedAppendOnlyFile 把序列化命令追加到 **aof_buf** (aof.c:1340-1344) — 命令回复前不落盘 (L1337-1339 注释 "flushed on disk just before of re-entering the event loop")
- 落盘时机: **beforeSleep → flushAppendOnlyFile** (server.c, 事件循环每次迭代) — 同步路径始终在回复前
- **always**: 每条命令 flush 后 fsync (数据零丢失, 吞吐受限); **everysec**: 1 秒合并 fsync (aof_fsync 后台线程); **no**: 完全交给 OS (page cache)
- 丢失窗口 (标注推断): always=0 / everysec≤1s / no=OS 决定
关键设计 (q1): **写前日志 + 策略分级**: 数据安全由 fsync 频率决定, 缓冲与落盘解耦。[模式: 写前日志]
数据流: 命令 → aof_buf → (策略) → write → fsync。

### 2. flushAppendOnlyFile — 延期与短写修复

场景: fsync 慢怎么办? 写一半崩了怎么办?
源码路径:
- **空缓冲补偿** (aof.c:1050-1079): 缓冲空但仍有未 fsync 数据 → everysec 超 1s 且无 fsync 进行中 → 补 fsync (L1056-1061, 注释 L1051-1055 "data in page cache cannot be flushed in time")
- **everysec 延期 ≤2s** (L1085-1105): fsync 进行中 → 推迟写 (aof_flush_postponed_start) → 超 2s 强制写 + **aof_delayed_fsync 计数** (L1102)
- **短写修复** (L1136-1183): nwritten < 缓冲长 → **ftruncate 截断回旧长度** (L1162, 消除部分写) → 记录 aof_last_write_errno
- 错误语义: nwritten==-1 直接记录 errno (L1147-1152); 截断成功视为 -1 (L1170-1173)
- 延迟监控: latencyAddSampleIfNeeded aof-write-pending-fsync/active-child/alone (L1124-1131)
关键设计 (q2): **可延迟 + 可截断**: everysec 用 2s 预算换吞吐; 短写 ftruncate 保证文件只含完整命令。[模式: 延期合并+截断修复]
数据流: aof_buf → (fsync 忙? 延期≤2s) → write → 短写? ftruncate。

### 3. feedAppendOnlyFile — SELECT 注入与序列化

场景: AOF 里怎么知道命令属于哪个 db?
源码路径:
- **SELECT 注入** (aof.c:1324-1331): dictid != aof_selected_db → 先写 SELECT (L1328-1329); aof_selected_db 缓存避免重复 (L1330)
- **命令序列化** (L1335): catAppendOnlyGenericCommand — RESP 格式重写 (数组头+bulk 体, 二进制安全)
- 时间戳注解 (L1314-1320, aof_timestamp_enabled)
- 缓冲条件 (L1340-1344): AOF_ON 或 (WAIT_REWRITE 且 AOF 子进程活着 — 重写期间继续追加)
- **传播统一**: 注释 L1333-1334 "All commands should be propagated the same way in AOF as in replication" — AOF 与从库同源 (R-9 交叉)
关键设计 (q3): **命令级日志**: 存命令而非数据 — 重放即可重建; SELECT 显式注入 = 多 db 正确性。[模式: 命令日志]
数据流: 命令 → SELECT(必要时) → RESP 序列化 → aof_buf。

### 4. AOF 重写 — 命令级重建 + 混合持久化

场景: AOF 无限增长怎么办? 重写后为什么变小?
源码路径:
- rewriteAppendOnlyFileRio (aof.c:2249-2348): 遍历全键空间 → **按类型变参命令重建**: STRING→SET (L2291-2295) / LIST→RPUSH 批量 (rewriteListObject, **AOF_REWRITE_ITEMS_PER_CMD=64** 每批, server.h:112) / SET→SADD / ZSET→ZADD / HASH→HSET / STREAM→XADD 流式 / MODULE→rewriteModuleObject
- **过期时间单独 PEXPIREAT** (L2319-2324, 绝对值 — R-22 语义)
- **混合持久化** rewriteAppendOnlyFile (L2357-2418): `aof_use_rdb_preamble` → **rdbSaveRio 写 RDB 头** (L2380-2385) — RDB 二进制 + 后续命令的 AOF 复合文件 (4.0 特性)
- 临时文件 temp-rewriteaof-pid.aof → fflush+fsync → rename (L2402)
- 增量 fsync + dismissObject COW 提示 (L2315-2316, R-18 交叉)
关键设计 (q4): **重写 = 数据集转命令流**: 变参批量 (64/批) 压命令数; 混合格式 RDB 压缩率高 + 加载快。[模式: 后台重建]
数据流: 全键 → 类型重写 → 变参命令 → (RDB preamble?) → temp → rename。

### 5. 重写生命周期 — fork + INCR 分离 + manifest 提正

场景: 重写期间的写入怎么不丢?
源码路径:
- rewriteAppendOnlyFileBackground (aof.c:2437+): **fork** → 子进程写 temp-rewriteaof-bg-pid.aof (fork 时刻快照); **父进程 openNewIncrAofForAppend** (L2453) — 重写开始即切新 INCR, 新命令全进 INCR
- **⚠ 7.0 架构修正: 无 diff 管道合并** — ≤6.x 的 aofReadDiffFromParent/aof_rewrite_buf 已移除 (grep 实证); 完整性靠 **BASE+INCR 双文件组合**
- **manifest 提正** backgroundRewriteDoneHandler (L2565+): temp BASE → **rename 新 BASE** + 临时 INCR → rename 新 INCR → 旧 BASE/INCR 标 **HISTORY** → **aofManifestDup 临时 manifest 原子替换** (aof.c:365-371 "atomically make the server.aof_manifest point to this temporary") → bio 删历史 (重写注释 L2424-2436)
- aof_state 三态流转: AOF_ON → WAIT_REWRITE (重写中) → 完成后回 ON
- **aofManifest 结构** (server.h:1520-1522, 1807): base_aof_info + incr_aof_list + history_aof_list + dirty 标志
关键设计 (q5): **BASE/INCR 文件级分离**: 子进程写快照 BASE, 父进程写增量 INCR, manifest 串起顺序 — 重写不阻塞写入且免管道同步。[模式: fork 重写+文件级分离]
数据流: BGREWRITEAOF → fork → 子进程 temp BASE + 父进程切新 INCR → 完成: 双 rename + manifest 提正 + 旧标 HISTORY。

### 6. AOF 加载 — 假客户端重放 + 截断容忍

场景: 重启怎么恢复? 文件损坏/截断怎么办?
源码路径:
- **loadSingleAppendOnlyFile** (aof.c:1383-1636): **假客户端 createAOFClient** (L1355-1375, CLIENT_ID_AOF + DENY_BLOCKING + SLAVE_STATE 防回复) → 逐命令解析重放 (fakeClient 驱动 processCommand 链)
- **RDB preamble 检测** (L1423-1437): 前 5B "REDIS" → 先 rdbLoadRio 再续 AOF (混合格式)
- **截断容忍** (L1440+): 尾部损坏 → **AOF_TRUNCATED** (**仅最后一个文件容忍**, last_file 判定 L1694; 非最后文件截断 → 加载失败)
- 致命错误分级: AOF_OPEN_ERR/NOT_EXIST/EMPTY/FAILED (L1377-1382)
- loadAppendOnlyFiles (L1637): **manifest 驱动多文件加载** (BASE + INCR 按序)
- 期间 aof_state=AOF_OFF (L1416) 防 EXEC 回喂 (L1414-1415)
关键设计 (q6): **重放 = 重执行命令**: 借 processCommand 全链 (ACL/传播/通知全生效); 截断只丢尾部不崩。[模式: 容错重放]
数据流: manifest → BASE+INCR → 假客户端 → processCommand 重放 → 截断容忍。

### 负面空间 — AOF 刻意不做的事

- **不做命令压缩存储**: 文本协议体积大 (重写才瘦身)
- **不做对象级增量**: 命令级日志, 单键热更新日志膨胀
- **不做 fsync 保证**: everysec/no 有丢失窗口 (always 才零丢失)
- **不做加载期校验和**: 截断靠格式检测非 crc
- **不做在线压缩**: 重写是 fork 后台, 主线程不做
- **不做跨进程恢复**: 无 multi-tenant 隔离 (单库语义)

→ 引出: RDB 怎么直接推给从库? → [[R-9-replication]]
