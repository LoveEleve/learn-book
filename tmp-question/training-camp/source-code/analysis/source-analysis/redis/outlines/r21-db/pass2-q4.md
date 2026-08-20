# 闭环笔记 q4: kvstore 分片寻址 — 一个 dict 数组

## 假设
分片 = 按 cluster 槽把键空间拆成 dict 数组; 单机退化为 1 dict; 寻址 = crc16 & 0x3FFF。

## 验证过程
- kvstoreCreate (kvstore.c:230-268):
  - num_dicts_bits → num_dicts = 1<<bits (L251); **assert bits ≤ 16** (L233: 游标要省 48 位给 dictScan)
  - flags: KVSTORE_ALLOCATE_DICTS_ON_DEMAND (1<<0) / KVSTORE_FREE_EMPTY_DICTS (1<<1) (kvstore.h:14-15)
  - 回调注入 (L241-248): userdata/dictMetadataBytes/rehashingStarted/Completed — kvstore 强制接管 (R-3 dictType 扩展点)
  - Fenwick 树 dict_size_index (L262): 仅 num_dicts > 1 时分配
- 三创建路径 (同一逻辑, cluster 时 14bit+FREE_EMPTY, 否则 0bit+ON_DEMAND):
  - initServer (server.c:2667-2675) — 主键空间
  - initTempDb (db.c:559-575) — 无盘复制临时库
  - emptyDbAsync (lazyfree.c:201-215) — FLUSHDB ASYNC 换表
  - 另有 pubsubshard_channels (server.c:2693) 同用 14bit
- 寻址链: getKeySlot (db.c:210) → calculateKeySlot (L205, 无缓存版) → keyHashSlot (cluster.h:43-60): 无 `{` → `crc16(key,keylen) & 0x3FFF`; 有 `{...}` hash tag → 只哈希中间 (e==keylen || e==s+1 时退全键)
- 常量: CLUSTER_SLOT_MASK_BITS=14 (cluster.h:8), CLUSTER_SLOTS=16384 (L9)
- 空 dict 回收: freeDictIfNeeded (L164-173) — FREE_EMPTY 时删除空 dict; **rehash 暂停期不回收** (安全迭代器场景, L167-168)

## 代码类型
Mechanism (分片寻址)

## 跨域关联
- R-20 (initServer 创建点) / R-15 (cluster 槽/CRC16) / R-3 (dict 单元) / R-29 (pubsub 分片)

## 结论
kvstore = dict 数组 + 按需分配 + 空桶回收 + 树索引。cluster 14bit = 16384 槽一一对应; 单机 0bit = 1 dict 完全退化但 API 统一。hash tag 是槽定位的用户级控制面 (同槽键可一起迁移)。
源码位置: kvstore.c:230-268, 164-173; server.c:2667-2675; cluster.h:8-9,43-60
