# R-8 持久化 (RDB+AOF) — Pass 1 探索笔记 (大域拆 2 篇)

> 域: R-8 RDB+AOF (rdb.c + aof.c + rio.c + bio.c) | 🔴 A 方案 | 2026-08-13
> 源码: rdb.c (4070) + aof.c (2754) + rio.c (520) + bio.c (439) | Redis 7.4.2
> 拆篇: **R-8a RDB** (rdb.c+rio.c+bio.c) / **R-8b AOF** (aof.c)

## 调用图

```
rio 抽象 (rio.c):
rioBufferIO (L89-100) / rioFileIO / rioConnIO 三实现 — read/write/tell/flush/checksum 函数表
rioInitWithFile (L199) / rioInitWithBuffer (L102) / rioInitWithConn (L303)
rioSetAutoSync (L448): 增量写分批 (REDIS_AUTOSYNC_BYTES, 防内核脏页堆积)
rioGenericUpdateChecksum (L436): crc64

RDB 保存 (rdb.c):
长度编码 rdbSaveLen (L151-184): 6bit/14bit/32bit/64bit 四档
整数编码 rdbEncodeInteger (L258+): INT8/INT16/INT32 + LZF 字符串编码
对象类型 rdbSaveObjectType (L671-716): 26 种 RDB_TYPE_* (rdb.h:54-79)
rdbSaveKeyValuePair (L1190): key+val+expire+LRU/LFU 字段
rdbSaveRio (L1452-1500): magic "REDIS%04d" + AUX + 全 db + EOF + CRC64
rdbSaveInternal (L1522): 临时文件 + fflush+fsync+reclaimFilePageCache + 失败 unlink
rdbSave (L1593): **temp-pid.rdb → rename 原子替换** + fsyncFileDir
rdbSaveBackground (L1636): **redisFork → 子进程 rdbSave** (COW 快照) + dirty_before_bgsave
rdbSaveRioWithEOFMark (L1501): 无盘复制 $EOF:mark 包裹 (R-9 交叉)

RDB 加载 (rdb.c):
rdbLoadRioWithLoadingCtx (L3328-3712): magic 校验 → 主循环 opcode 分派
opcode 体系 (rdb.h:86-97): EXPIRETIME_MS(252)/SELECTDB(254)/RESIZEDB(251)/AUX(250)/
  IDLE(248)/FREQ(249)/SLOT_INFO(244)/FUNCTION2(245)/MODULE_AUX(247)/EOF(255)
rdbReportError (L46-87): RESTORE 上下文 vs 加载 vs rdbCheckMode 三分支
rdbLoad (L3713) / rdbSaveToSlavesSockets (L3851)

AOF (aof.c):
appendfsync 三策略 (config.c:77-79): everysec(默认)/always/no
feedAppendOnlyFile (L1308-1347): SELECT 注入 + 命令序列化 → aof_buf
flushAppendOnlyFile (L1045-1307): 空缓冲补偿 fsync + everysec 延期(≤2s) + 短写截断修复
aof_background_fsync (L905): bio 线程 fsync 卸载
AOF 重写: rewriteAppendOnlyFileRio (L2249-2348, 命令级重建: SET/RPUSH/SADD...) 
  → rewriteAppendOnlyFile (L2357, temp-rewriteaof + rename + **混合持久化 rdbSaveRio preamble**)
  → rewriteAppendOnlyFileBackground (L2437, fork + openNewIncrAofForAppend + **7.0 文件级分离: 无 diff 管道**)
  → backgroundRewriteDoneHandler (temp→新 BASE + 临时 INCR→新 INCR + 旧标 HISTORY + manifest 提正)
AOF 加载: loadAppendOnlyFiles (L1637) → loadSingleAppendOnlyFile (L1383, 假客户端重放)
aofManifest (L128-214): 7.0 多部分 AOF (base+incr 清单文件)

bio (bio.c): 3 worker (close_file/aof/lazy_free) + 完成回调管道 (bio_comp_list+job_comp_pipe)
```

## 基本元素分解

**R-8a RDB**:
1. 长度编码 (6/14/32/64bit) + 整数编码 (INT8/16/32) + LZF
2. 对象类型分派 (26 种 RDB_TYPE) + rio 抽象
3. fork COW 快照 (bgsave) + 临时文件 rename 原子替换
4. 文件格式: magic + AUX + SELECTDB + RESIZEDB + EOF + CRC64
5. 加载主循环: opcode 分派 + 惰性重建 (dictExpand 预扩)
6. 错误处理: rdbReportError 三分支 + RDB 版本兼容 (v9 BE 修复)

**R-8b AOF**:
1. 三策略 (always/everysec/no) + aof_buf + beforeSleep flush
2. flushAppendOnlyFile: 空缓冲补偿 + 延期 ≤2s + 短写 ftruncate 修复
3. feedAppendOnlyFile: SELECT 注入 + 序列化
4. 重写: 命令级重建 (变参批量) + 混合持久化 (RDB preamble)
5. 重写期增量: fork + INCR 文件切换 + diff 追加
6. 加载: 假客户端重放 + manifest 多部分 + 截断容忍 (AOF_TRUNCATED)

## 标记问题 (40 问: R-8a 20 + R-8b 20)

R-8a: 长度四档边界? / 整数编码选择? / LZF 何时压缩? / 对象类型分派? / fork COW 怎么保证一致? / rename 原子性? / fsyncFileDir 为什么? / EOF+CRC64 校验? / AUX 字段? / RESIZEDB 预扩? / SELECTDB 越界? / SLOT_INFO cluster? / v9 字节序修复? / rdbReportError 三分支? / RESTORE 上下文? / 无盘复制 EOFMark? / reclaimFilePageCache? / autoSync 分批? / checksum 关闭? / 加载进度回调?

R-8b: 三策略差异? / everysec 空缓冲补偿? / fsync 延期 2s? / 短写 ftruncate? / SELECT 注入? / 命令序列化? / 重写变参批量? / 混合持久化开关? / fork 后 INCR 切换? / diff 追加机制? / manifest 结构? / 加载截断容忍? / 假客户端? / AOF_WAIT_REWRITE 状态? / no-appendfsync-on-rewrite? / aof_selected_db? / bio fsync 卸载? / 时间戳注解? / WAITAOF 交互? / 重启加载顺序 (RDB vs AOF)?

## 时空溯源 (代码内痕迹)

- 2009 (初版): RDB 持久化 (rdb.c 版权 2009-Present; magic REDIS 格式)
- 2010 (1.2): AOF 引入 (appendfsync 三策略)
- 2.8: 混合? 不 — 2.8 无. 3.2: AOF 重写优化 (变参批量)
- 4.0: **混合持久化 aof-use-rdb-preamble** (RDB+AOF 头) + MODULE 类型
- 5.0: RDB v9 **字节序修复** (memrev64ifbe, rdb.c:125-146 注释权威证据) + STREAM
- 7.0: **Multi-Part AOF** (aofManifest, base+incr) + FUNCTION2 + SLOT_INFO
- 7.4: HASH_METADATA (HFE, rdb.h:76-79 PRE_GA 痕迹)

## 大域拆分判断

2 文件 6824 行 — **拆 2 篇** (R-8a RDB 6 KP / R-8b AOF 6 KP) + 双 harness
