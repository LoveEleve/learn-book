# 闭环笔记 q4: expireSlaveKeys — 可写从库记账

## 假设
可写从库自产的带 TTL 键, master 不知道 → 从库独立记账回收 (位图 dict)。

## 验证过程
- 背景注释 (expire.c:410-431): 正常从库等 master 的 DEL; 可写从库自产键 (交集临时键等) master 不知情 → 必须自己回收; 3.2 之前直接泄漏
- 结构 (L445): `dict *slaveKeysWithExpire` — 键 → uint64 dbid 位图 (key 是 sdsdup 副本 L532, 与主 DB 不同步)
- 记账时机 (rememberSlaveKeyWithExpire L511-539): setExpire 调 (db.c:1860-1862, `writable_slave = masterhost && !repl_slave_ro`) — 仅 masterhost 存在 + 可写
  - dbid > 63 不跟踪 (L524) — 位图上限
- 回收 (expireSlaveKeys L449-507): databasesCron 从库分支调 (server.c:1061)
  - 循环: dictGetRandomKey → 按位图逐 DB: dbFindExpires + activeExpireCycleTryExpire (L467-471)
  - 未过期 → 位图保留 (L483-486); 全过 → dictDelete (L498)
  - **停止三条件** (L500-505): 连续 3 个不可过期 (noexpire>3) / 每 64 循环检查 >1ms / 表空
- flushSlaveKeysWithExpireList (L555-560): FLUSHALL 时清记账表 (db.c:542,1800 调用) — 防误删新键
- getSlaveKeyWithExpireCount (L542): INFO 指标

## 代码类型
Mechanism (角色边界记账)

## 跨域关联
- R-21 (setExpire/dbFindExpires 连接) / R-20 (databasesCron 分支) / R-9 (复制角色)

## 结论
可写从库 = 角色特例的记账回收: 位图 dict 记录"从库自产 + 带 TTL"的键, cron 随机抽查, 过期即删 (用同一 activeExpireCycleTryExpire)。位图上限 63 DB 是显式设计折衷 (注释标注 "trivial fix")。
源码位置: expire.c:410-560; db.c:1860-1862
