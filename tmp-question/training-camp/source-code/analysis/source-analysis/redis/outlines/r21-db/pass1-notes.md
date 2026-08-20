# R-21 db 键空间 — Pass 1 探索笔记

> 域: R-21 db 键空间 (巨型域) | 🔴 方案 A | 2026-08-13
> 源码: src/db.c (2818) + kvstore.c (1060) + ebuckets.c (2440) + lazyfree.c (274) | Redis 7.4.2

## 继承树/调用图

```
lookupKey (db.c:75): dbFind (L2064: kvstoreDictFind, getKeySlot) → val
  → expireIfNeeded (L1974): keyIsExpired (L1928: commandTimeSnapshot) 
      → KEY_VALID / KEY_EXPIRED (复制不删) / KEY_DELETED (deleteExpiredKeyAndPropagate L1877)
  → 命中: LFU/LRU 更新 (LOOKUP_NOTOUCH 跳过, hasActiveChildProcess 跳过)
  → miss: keymiss 通知 + stat_keyspace_misses

dbAddInternal (L180): getKeySlot → kvstoreDictAddRaw(db->keys) 
  → kvstoreDictSetKey (sdsdup) → initObjectLRUOrLFU → kvstoreDictSetVal
  → signalKeyAsReady (阻塞键) + NOTIFY_NEW
dbSetValue (L256): 覆盖旧值 — overwrite 标志 (module unlink 通知/signalDeletedKeyAsReady)
  → 旧值释放: lazyfree_lazy_server_del ? freeObjAsync : decrRefCount
  → HFE: 旧值 OBJ_HASH → hashTypeRemoveFromExpires (db->hexpires)
dbGenericDelete (L372): kvstoreDictTwoPhaseUnlinkFind → module 通知 → 删除 expires 表
  → kvstoreDictTwoPhaseUnlinkFree → async ? freeObjAsync : 同步释放

setExpire (L1846): 键复用主 dict sds (dictGetKey(kde) 零拷贝!) → kvstoreDictAddRaw(db->expires)
  → dictSetSignedIntegerVal (整数联合值, 免分配)
getExpire (L1867) / removeExpire (L1838)

kvstoreCreate (kvstore.c:230): num_dicts_bits → 1<<bits 个 dict 指针数组
  → 三创建路径: initServer (server.c:2674-2675) / initTempDb (db.c:569-570) / emptyDbAsync (lazyfree.c:210-211)
getKeySlot (db.c:210): cluster_enabled ? keyHashSlot (crc16&0x3FFF, cluster.h:43-56) : 0
  → 优化: current_client->slot 缓存 (CLIENT_EXECUTING_COMMAND)

kvstoreScan (kvstore.c:361): cursor 高 48 位 dictScan + 低 num_dicts_bits 位 didx
  → 非空 dict 跳转: kvstoreGetNextNonEmptyDictIndex → BIT (Fenwick 树) 二分
kvstoreGetFairRandomDictIndex (L431): randomULong % size → BIT 定位
kvstoreTryResizeDicts (L621) / kvstoreIncrementallyRehash (L642): rehashing list
  → 消费: databasesCron (server.c:1054-1101, CRON_DICTS_PER_DB=16, 1000us)

ebuckets (ebuckets.c): 时间桶 DS — list (≤16 项) → rax (bucketKey 6B) → segment (≤16 项)
  → ebAdd (L1424) / ebExpire (L1464) / ebExpireDryRun (L1561) / ebGetNextTimeToExpire (L1663)
  → HFE: hashExpireBucketsType (t_hash.c:115) + hashFieldExpireBucketsType (t_hash.c:124)

lazyfree (lazyfree.c): freeObjAsync (L184, LAZYFREE_THRESHOLD=64) / emptyDbAsync (L201)
  → bioCreateLazyFreeJob → lazyfreeFreeObject/LazyfreeFreeDatabase (L13/L23)
```

## 基本元素分解

1. **查找路径**: lookupKey 家族 — 过期检查 + LRU/LFU + 统计 + 通知
2. **惰性过期**: expireIfNeeded 三态 (VALID/EXPIRED/DELETED) — 主从差异/强制删除/静态键
3. **增删改**: dbAddInternal/dbSetValue/dbGenericDelete — 三表 (keys/expires/hexpires) 一致性
4. **expires 表**: 键共享 sds + 整数联合值 — 零额外分配
5. **kvstore 分片**: num_dicts_bits → dict 数组 — cluster 16384 槽 14bit
6. **kvstore 扫描/随机**: 48+14bit 游标编码 / BIT Fenwick 树 / FAIR 随机
7. **kvstore 重hash**: rehashing list + 增量 (databasesCron 消费)
8. **ebuckets**: 时间桶 — list→rax 升级 / segment / 主动过期
9. **HFE**: hash 字段级过期 — db->hexpires 全局注册 (早到字段)
10. **惰性删除**: freeObjAsync 阈值 64 / emptyDbAsync 换表 / bio 线程
11. **命令面**: DEL/EXISTS/KEYS/SCAN/RANDOMKEY/SWAPDB/FLUSHDB
12. **getKeys**: key-spec 声明式键提取 (7.x 新面)

## 标记问题 (12 个)

1. lookupKey 的完整副作用链 (过期/LRU/统计/通知)?
2. expireIfNeeded 三态什么语义?复制下为什么只报过期不删除?
3. dbAdd/dbSetValue/dbDelete 三表一致性怎么保证 (keys/expires/hexpires)?
4. expires 键为什么能复用主 dict 的 sds (零拷贝)?
5. kvstore 分片怎么寻址?cluster 14bit 怎么映射?
6. kvstoreScan 游标怎么编码 48+14 位?跨 dict 怎么跳?
7. FAIR 随机/BIT 树怎么做到 O(log n) 选桶?
8. ebuckets 为什么 list→rax?segment 怎么分裂?
9. ebExpire 主动过期怎么批量删?为什么有 dry-run?
10. HFE (hash 字段过期) 怎么全局注册?为什么单独 ebuckets?
11. lazyfree 阈值 64 依据?emptyDbAsync 换表法怎么保证一致性?
12. getKeys 声明式 key-spec vs 回调式 getkeys_proc 两代?

## 时空溯源 (代码内痕迹)

- 2009 (antirez): db.c 初版 — lookupKey/dbAdd/expire 家族 (版权 2009-Present)
- 2011: kvstore (原 dictarray) 引入 — "Index-based KV store" (版权 2011-Present), 初期只为 cluster 槽分组
- 7.0: kvstore 全面接管 redisDb.keys (非 cluster 也是 1-dict kvstore, 统一 API)
- 7.x: ebuckets (2024 版权, Redis Ltd.) — HFE 字段过期 (EB_BUCKET_KEY_PRECISION=0 TBD)
- 7.x: 两阶段删除 (TwoPhaseUnlink) / no-value dict (R-3 联动) / expires_cursor 保留
- 7.x: getKeys 从 firstkey/lastkey/step 演进到 key-spec 声明式 (commands.def 生成)

## 大域拆分规划 (01 §大域)

8 闭环 → **2 篇**:
- 篇 1 (键空间操作): q1 (lookupKey+惰性过期) + q2 (增删改三件套) + q3 (expires 表) + q8 (命令面+getKeys)
- 篇 2 (kvstore 分片实现): q4 (分片寻址) + q5 (扫描/随机/BIT) + q6 (ebuckets 时间桶+HFE) + q7 (惰性删除)
