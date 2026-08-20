# 闭环笔记 q5: 扫描/随机/BIT — 游标编码与 Fenwick 定位

## 假设
游标 64 位复用: 高 48 位 dictScan + 低 num_dicts_bits 位 dict 索引; 选桶/跳桶靠 Fenwick 树 O(log n)。

## 验证过程
- 游标编码:
  - addDictIndexToCursor (kvstore.c:102-109): `cursor = (cursor << num_dicts_bits) | didx`
  - getAndClearDictIndexFromCursor (L111-117): `didx = cursor & (num_dicts-1); cursor >>= num_dicts_bits`
  - 注释 (L367-370): "48 upper bits for HT positioning, lower bits for dict index" — bits≤16 的依据
- kvstoreScan (L361-403): onlydidx 快进/超界 (L372-382) → dictScan (R-3 反转游标弱语义) → 扫完或 skip → 跳下一非空 dict → addDictIndexToCursor 重组
  - 注意: dictScan 回调可删除 (主动过期场景), 扫完 freeDictIfNeeded (L390)
- 非空 dict 定位 (L531-538): cumulativeKeyCountRead(didx)+1 → kvstoreFindDictIndexByKeyIndex
- Fenwick 树 (Fenwick/BIT):
  - cumulativeKeyCountAdd (L122-145): 更新 O(log n), 同时维护 non_empty_dicts 计数
  - kvstoreFindDictIndexByKeyIndex (L500-523): 从最高位 1<<num_dicts_bits 二分 — "target > node 值 → 减后下探"; 增 1 减 1 抵消 (BIT 1-based vs dict 0-based)
  - kvstoreGetFairRandomDictIndex (L431-434): randomULong % kvstoreSize + 1 → BIT 定位 — **概率 ∝ 元素数 (无偏)**
- 迭代器: kvstoreIterator (L555-617, 跨 dict + 释放时空 dict 回收) / kvstoreDictIterator (L683-722)
- 增量 rehash: kvstoreIncrementallyRehash (L642-661: rehashing list + 1000us 预算) / kvstoreTryResizeDicts (L621-633: resize_cursor 轮转 + 每轮 limit)
  - 消费: databasesCron (server.c:1054-1101) — CRON_DICTS_PER_DB=16 (server.h:105), INCREMENTAL_REHASHING_THRESHOLD_US=1000 (server.h:128); **hasActiveChildProcess 跳过** (防 CoW)
- cluster 扩容: dbExpandGeneric (db.c:2036-2050) — db_size/slots 近似 + dbExpandSkipSlot (L2022, 只扩本节点槽)

## 代码类型
Mechanism (游标复用 + 树索引)

## 跨域关联
- R-3 (dictScan 反转游标弱语义 + rehash 联动) / R-20 (databasesCron) / R-15 (slot 覆盖)

## 结论
64 位游标拆两段 = 每 dict 内仍是 dictScan 弱语义 (不丢旧键/重复允许/插入可漏), 跨 dict 由"非空跳转"串联 — 扫描语义不因分片改变。Fenwick 树把 RANDOMKEY 的选桶与扫描的跳桶降到 O(log n)。
源码位置: kvstore.c:102-117,361-403,431-434,500-538,621-661; server.c:1054-1101
