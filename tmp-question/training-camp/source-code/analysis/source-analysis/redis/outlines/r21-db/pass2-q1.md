# 闭环笔记 q1: lookupKey 副作用链 + 惰性过期三态

## 假设
lookupKey 不只是查字典: 过期判定、访问时间、统计、通知构成一条副作用链, 每项可单独关闭。

## 验证过程
- lookupKey (db.c:75-127):
  - L76: dbFind → kvstoreDictFind (L2064-2066, getKeySlot 分片寻址)
  - L88-97: 命中后 expireIfNeeded 拦截 — WRITE+非只读从库 → FORCE 删; NOEXPIRE → AVOID 只查
  - L104-113: LRU 更新 — CLIENT_NO_TOUCH 命令豁免 (touchCommand 例外); **hasActiveChildProcess 跳过** (防 RDB/AOF 子进程 CoW)
  - L115-123: stat_keyspace_hits/misses + notifyKeyspaceEvent "keymiss" (NONOTIFY 可关)
- 标志矩阵 (L59-68): NOTOUCH/NONOTIFY/NOSTATS/WRITE/NOEXPIRE 五标志 — 逐项独立
- 入口族 (L138-173): Read/Write/ReadOrReply/WriteOrReply — OrReply 失败即回错误对象
- slot 缓存 (L217-219): current_client->slot (CLIENT_EXECUTING_COMMAND 期间预计算, debugServerAssert 校验)

## 代码类型
Mechanism (查找路径 = 副作用链编排)

## 跨域关联
- R-3 (dict/kvstore) / R-1 (LRU 字段) / R-29 (keymiss 通知) / R-23 (maxmemory 淘汰的读)
- R-22 (主动过期) — 与惰性过期互补

## 结论
lookupKey = 查找 + 副作用链 (过期拦截 → 访问时间 → 统计 → 通知), 五标志位独立开关。CoW 保护 (有子进程不更新 LRU) 与 keymiss 通知是 7.x 的精细化。getKeySlot 的 current_client->slot 缓存避免每条命令重复 CRC16。
源码位置: db.c:75-127, 138-173, 205-222
