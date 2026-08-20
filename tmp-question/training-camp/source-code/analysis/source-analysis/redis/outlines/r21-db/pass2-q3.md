# 闭环笔记 q3: expires 表 — 零拷贝键 + 整数值

## 假设
expires 表是"键共享 + 值内嵌"的零额外分配表。

## 验证过程
- setExpire (db.c:1846-1863):
  - L1850-1853: **键复用主 dict 的 sds** — kvstoreDictAddRaw(db->expires, slot, dictGetKey(kde)) — 主 dict 的 dictEntry key 指针直接入 expires 表, 零拷贝 (注释 "Reuse the sds from the main dict in the expire dict")
  - L1855-1858: dictSetSignedIntegerVal (dict.c:849) — TTL 存 dictEntry 联合值, **无 val 分配**
  - L1860-1862: writable_slave → rememberSlaveKeyWithExpire (从库 TTL 记账)
- dbExpiresDictType (server.c:501-508): **key destructor = NULL** (键归主 dict 释放) + **val destructor = NULL** (值在联合体内) — 与 dbDictType (L490-499, sdsDestructor+ObjectDestructor) 对比
- getExpire (L1867-1874): dbFindExpires → dictGetSignedIntegerVal; 无条目返回 -1
- removeExpire (L1838-1840): kvstoreDictDelete(expires) == DICT_OK
- 内存账: 每 TTL = 1 dictEntry (键 8B 指针共享 + 值 8B 联合) — 无 sds 副本、无 robj

## 代码类型
Mechanism (结构共享优化)

## 跨域关联
- R-3 (联合值整数免分配) / R-1 (值对象 vs 键分离) / R-22 (过期读写面)

## 结论
expires 表通过"键指针共享 + 值内嵌"把每 TTL 的额外成本压到 ~1 dictEntry。删除顺序: 键共享保证了 dbGenericDelete 先删 expires 再删 keys 的安全 (expires 不触键释放)。
源码位置: db.c:1838-1874; server.c:490-508; dict.c:849
