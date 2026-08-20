# 闭环笔记 q3: 类型分派 — defragKey 与跨结构引用同步

## 假设
defragKey 是"一个键的所有指针"的搬移入口: 键名 → robj → 值, 每一步搬移都要同步相关表的引用。

## 验证过程
- **defragKey** (defrag.c:729-822):
  - **键名搬移** (L737-752): activeDefragSds → kvstoreDictSetKey (keys 表) + **expires 表同步** (L740-747): 搬移后不能用 sds 比较查找 (旧指针已释放), 改用 `kvstoreGetHash(newsds)` + `kvstoreDictFindEntryByPtrAndHash(keysds, hash)` — 哈希+旧指针定位条目再更新键; HASH 类型额外 `hashTypeUpdateKeyRef` (L750-751, HFE/listpackEx 的键引用)
  - **robj 搬移** (L754-766): **HFE 特例** (L755-758): hashTypeGetMinExpire != INVALID → `ebDefragItem(&db->hexpires, ...)` — 时间桶持有 robj 引用, 搬移须同步 (R-21 ebuckets); 否则 activeDefragStringOb
  - **值分派** (L768-821): type×encoding 矩阵 —
    - STRING: 已由 robj helper 处理 (L768-769)
    - LIST: quicklist → defragQuicklist (L771-772, 大键延后判定 L492-493) / listpack → 整块搬 ob->ptr (L773-775)
    - SET: HT → defragSet / intset|listpack → 整块搬 (L780-788)
    - ZSET: listpack 整块搬 / skiplist → defragZsetSkiplist (L792-796)
    - HASH: listpack 整块 / **listpackEX 双搬** (L804-809: lpt 结构 + lpt->lp 两层) / HT → defragHash
    - STREAM: defragStream (L815-816, 三层: s 结构/rax 树/entry data)
    - MODULE: defragModule (L817-818, moduleDefragValue)
    - 未知 → serverPanic
- **zset 双结构一致** (defragZsetSkiplist L498-525): zs 结构 → zsl → header → 元素逐个 activeDefragZsetEntry (dict 键 sds + zsl 节点同步, L246-256: dictSetKey + zslDefrag 返回 score 引用) → dictDefragTables
- **pubsub 共享频道名** (L870-902): **refcount 断言** `channel->refcount == dictSize(clients)+1` (L878) — 服务器表 + 每客户端一份; 搬移后**客户端侧 dict 也要更新** (L886-894: 遍历客户端表 dictFind(newchannel) 替换)
- **defragOtherGlobals** (L906-915): eval scripts (LUA_SCRIPT 值) / moduleDefragGlobals / pubsub 两表 LUT — 注释 L907-910: "one small allocation can hold a full allocator run, so although small, it is still important"

## 代码类型
Implementation (全谱类型分派 + 跨引用同步)

## 跨域关联
- R-21 (db): expires 共享 sds 键 (零拷贝) — 搬移必须双表同步
- R-25 (hash): HFE / listpackEx / hashTypeUpdateKeyRef
- R-6 (zset): dict+skiplist 双结构
- R-29 (pubsub): 频道共享引用

## 结论
defragKey = 单键全指针搬移入口; 每次搬移都要同步引用: 键名 (keys+expires+hash 元数据), robj (HFE 时间桶), 值 (zset 双结构/pubsub 双端/stream 三层); 全 type×encoding 矩阵覆盖 + serverPanic 兜底。
源码位置: defrag.c:498-553,729-822,870-915
