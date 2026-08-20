# 闭环笔记 q8: 键空间命令面 — 遍历/删除/互换 + getKeys

## 假设
命令面 = 统一副作用出口 (signalModifiedKey) + SCAN 分片游标 + SWAPDB 指针互换 + key-spec 声明式键提取。

## 验证过程
- SCAN 四步 (scanGenericCommand db.c:1049-1319):
  - Step1 选项 (L1069-1114): COUNT (默认 10) / MATCH / TYPE (仅 db 扫描, 过滤实现在 Step3 复查) / NOVALUES (仅 HSCAN)
  - Step2 迭代 (L1152-1195): ht 路径 — maxiterations = count×10 (L1157) + data.sampled < count 双限; db → kvstoreScan; 集合 → dictScan; 小编码 (SET listpack/intset) → 一次性返回游标归 0
  - Step3 过滤 (L1279-1301): expireIfNeeded 删过期 + typename 复查 (TODO 注释 8.0 移除)
  - Step4 回复 (L1303-1318): [游标, [键...]]
- KEYS (L864-905): pattern 匹配; cluster + 非全键 → patternHashSlot 预判单槽 (pslot 优化 L876-885)
- RANDOMKEY (dbRandomKey L336-369): kvstoreGetFairRandomDictIndex (q5) → expireIfNeeded 重试; **maxtries=100 + allvolatile 兜底** (L351-360: 全到期从库防死循环, 返回可能已过期键)
- DEL/UNLINK (delGenericCommand L796-813): 先 expireIfNeeded (KEY_DELETED 跳过) → dbAsyncDelete (UNLINK/配置) / dbSyncDelete → signalModifiedKey + notify "del"
- SWAPDB (dbSwapDatabases L1712-1755): **只换 keys/expires/hexpires/avg_ttl/expires_cursor 指针** (L1731-1741); blocking_keys/ready_keys/watched_keys **不换** (客户端留原 DB); 换前 touchAllWatchedKeysInDb (事务失效) + scanDatabaseForDeletedKeys (XREADGROUP 解阻) + 换后 scanDatabaseForReadyKeys (BLPOP 就绪)
- FLUSHDB/ALL (flushCommandCommon L730-779): SYNC/ASYNC/默认三态; **blocking_async** (L736-760: 条件满足时 SYNC 也转后台, bioCreateCompRq + BLOCKED_LAZYFREE, 完成回 flushallSyncBgDone L700); jemalloc purge (L771-777); flushAllDataAndResetRDB (L676-697)
- emptyDbStructure (L471-501): async ? emptyDbAsync : (ebDestroy HFE 先毁 → kvstoreEmpty ×2); avg_ttl/expires_cursor 重置
- signalModifiedKey (L621-624): touchWatchedKey (R-16 WATCH) + trackingInvalidateKey (R-17)
- **getKeys 三代** (L2133-2442):
  1. getKeysFromCommandWithSpecs (L2260) — key-spec 声明式 (commands.def 生成): INDEX/KEYWORD 起 + RANGE/KEYNUM 找; 失败回退
  2. getkeys_proc 回调 (L2434-2442 优先分支): 复杂命令手写 (sortGetKeys L2570 / migrateGetKeys L2615 / xreadGetKeys L2713 / setGetKeys L2761 / bitfieldGetKeys L2786)
  3. legacy range (L2379-2421): firstkey/lastkey/step 兜底
  - doesCommandHaveKeys (L2287-2291) — 键存在性快速判定

## 代码类型
Glue (命令面装配)

## 跨域关联
- R-3 (dictScan/pslot) / R-16 (WATCH) / R-17 (TRACKING) / R-26 (阻塞键信号) / R-22 (过期过滤)

## 结论
命令面统一在信号出口 (signalModifiedKey) 上做 WATCH/TRACKING; SWAPDB 指针互换 O(1) 但 watch/blocked 语义显式保留; SCAN 的 TYPE 过滤与过期过滤合在 Step3 (一次遍历双过滤); getKeys 声明式 key-spec 是 7.x 换代 (cluster 槽检查/multi-key 判定依赖它)。
源码位置: db.c:621-645,730-813,864-905,1049-1319,1712-1755,2133-2442
