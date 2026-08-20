# 闭环笔记 q7: dictType 定制族 + 整数值联合存储

## 假设
dict 通过 dictType 函数指针族完全定制: 哈希/比较/复制/释放全可注入; dictEntry 的值是联合体, 整数类型直接存储免指针分配。

## 验证过程
- dictType (dict.h:32-92): hashFunction (必选) / keyCompare (NULL→指针相等) / keyDup/valDup (复制) / keyDestructor/valDestructor (释放) / expandAllowed (扩前回调)
- 7.x 扩展: no_value/keys_are_odd (q5) / storedHashFunction/storedKeyCompare (存储键 API: 结构化键以 C 字符串查, L60-90 长注释) / rehashingCompleted / onDictRelease / dictMetadataBytes (每 dict 元数据, 如 db id)
- **整数值联合** (dict.h, dictEntry.v): `val` (指针) / `s64` / `u64` / `double` — dictIncrSignedIntegerVal 等 (dict.c:850+) — **整数键值免分配** (哈希表直接存值)
- 消费实例 (server.c):
  - dbDictType: 键= sds (sdsHashFunction), 值= robj*, keys_are_odd=1 (L474-475)
  - keyptrDictType: 键直接是指针 (server.c:634, no_value)
  - 过期表/命令表/cluster 等各有定制
- 宏面: dictFreeVal/dictFreeKey/dictCompareKeys (dict.h:128-143) — 空指针安全分派

## 代码类型
Interface (策略注入) + Implementation (值存储)

## 跨域关联
- R-4 (sdsHashFunction) → 键哈希
- R-21 (dbDictType 键空间) → 最大实例
- R-33 (zmalloc) → 全部 entry/表分配

## 结论
dict = 通用哈希内核 + 策略注入: 哈希函数/比较/释放全由 dictType 定制, 键空间/过期表/命令表/set 内部用同一内核不同策略。联合值 (s64/u64/double) 让整数免分配 — 与 R-1 的共享整数呼应 (整数值连 entry 的分配都省)。
源码位置: dict.h:32-92; dict.c:850+; server.c:474-475
