# R-21 db 键空间 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | db.c 初版 — lookupKey/dbAdd/setExpire/expire 家族 (版权 "2009-Present") |
| 2011 | **kvstore 引入** (版权 "2011-Present", 原 dictarray) — 注释明确 "used when Redis is running in cluster mode... separate dict per hash-slot" — 初期只服务 cluster |
| 5.x/6.x | 惰性删除制度化: lazyfree.c 独立文件 (freeObjAsync/emptyDbAsync 换表法); UNLINK 命令 |
| 7.0 | **kvstore 全面接管 redisDb.keys** — 单机也是 kvstore (bits=0 退化为 1 dict, API 统一); expires 表同构; 两阶段删除 TwoPhaseUnlink (dict 层) |
| 7.x | **key-spec 声明式 getKeys** (commands.def 生成, 替代 firstkey/lastkey/step + getkeys_proc 混用); SCAN TYPE 过滤 |
| 7.x (2024) | **ebuckets 时间桶** (版权 "2024 Redis Ltd.") — HFE 字段级过期: db->hexpires 全局表 + hash 内嵌 hfe; EB_BUCKET_KEY_PRECISION=0 显式 TBD |
| 演进 | expires_cursor 保留 (R-22 主动过期游标); avg_ttl 统计; dict_size_index Fenwick 树 (随机/跳桶 O(log n)) |

## 痕迹证据

- kvstore.c:12 注释: "when Redis is running in cluster mode, we use kvstore to save all keys that map to the same hash-slot in a separate dict" — 设计原点 = cluster 槽
- server.h:968-982 redisDb: keys/expires (kvstore) + hexpires (ebuckets) 三表并列 — 7.x 分层: dict → kvstore → ebuckets
- ebuckets.h:142: `#define EB_BUCKET_KEY_PRECISION 0 /* TBD: modify to 10 */` — 新 DS 的未决设计点
- db.c:180 dbAddInternal 的 signalKeyAsReady (阻塞键) 与 NOTIFY_NEW — 早期就内置的事件面
- lazyfree.c 版权无年份注释 (server.h 包含) — 5.x 起的持续演进

## 推断标注

- kvstore 单机化的动机 (bits=0 也走 kvstore) 是"统一 API + 为 cluster 双形态" — **推断** (代码无注释明说, 但 kvstoreCreate 统一接管 redisDb.keys 是事实)
- Fenwick 树选桶 O(log n) vs 线性扫描 — BIT 是 7.x 对"随机键/跳桶"的复杂度优化 — **推断** (引入动机未注释, 但注释给出 BIT 复杂度)
