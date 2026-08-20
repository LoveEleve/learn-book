# 闭环笔记 q2: 增删改三件套 — 三表一致性

## 假设
keys/expires/hexpires 三张表的一致由"删除顺序约定 + 键共享"保证。

## 验证过程
- dbAddInternal (db.c:180-195): kvstoreDictAddRaw(keys, slot) → 键 sdsdup (L189 — 主 dict 拥有键副本) → initObjectLRUOrLFU (L190) → SetVal → signalKeyAsReady (L192, 阻塞键: BLPOP 就绪) → NOTIFY_NEW
- dbSetValue (L256-289): 覆盖 — **旧值 lru 继承给新值** (L262) → overwrite 分支: incrRefCount 保护 (RM_StringDMA 可能释放) + moduleNotifyKeyUnlink + signalDeletedKeyAsReady (XREADGROUP 阻塞) → **hash 旧值摘除 hexpires** (L281-282) → 释放旧值: lazyfree_lazy_server_del ? freeObjAsync : decrRefCount
- dbGenericDelete (L372-408): TwoPhaseUnlinkFind (R-3: 定位→暂停 rehash→删→恢复) → 通知 (module/lazyfree 守护) → **kvstoreDictDelete(db->expires)** (L401, 键共享 sds 安全) → UnlinkFree → async ? freeObjAsync (L396: 先置 NULL 防双删)
- 三入口 (L411-425): dbSyncDelete / dbAsyncDelete / dbDelete (server.lazyfree_lazy_server_del 配置)
- setKey (L310-330): 四态路由 (ADD_OR_UPDATE/ALREADY_EXIST/DOESNT_EXIST/默认查找) + KEEPTTL (L328: removeExpire 跳过) + NO_SIGNAL
- dbAddRDBLoad (L235-242): RDB 加载专用 — 键不 dup (调用方转移所有权), 无通知

## 代码类型
Mechanism (多表一致性约定)

## 跨域关联
- R-3 (TwoPhaseUnlink 两阶段删除) / R-1 (引用计数守护) / R-26 (阻塞键信号) / R-8 (dbAddRDBLoad)

## 结论
一致性 = 顺序约定: 删时**先 expires 后 keys** (键共享 sds 所以 expires 删除不触键释放); hexpires 是嵌入式的 (hash 对象自带 ExpireMeta), 只做注册/摘除。所有写命令过 signalModifiedKey (WATCH/TRACKING) — 单出口。
源码位置: db.c:180-330, 372-425
