# 闭环笔记 q6: TTL/PERSIST/TOUCH — 查询面与时间一致

## 假设
TTL 族返回三值语义 (-2/-1/剩余), PERSIST 摘除 TTL, TOUCH 是纯访问。

## 验证过程
- ttlGenericCommand (expire.c:773-794):
  - 键不存在 → -2 (L777-779, lookupKeyReadWithFlags + NOTOUCH — 查询不改 LRU)
  - 键存在无 TTL → -1 (L784-790)
  - 有 TTL → `ttl = output_abs ? expire : expire - commandTimeSnapshot()` (L786) — 相对 = 到期时间戳减当前
  - 负值钳 0 (L787); **秒级四舍五入 (L792): `(ttl+500)/1000`** — 非截断
  - EXPIRETIME/PEXPIRETIME (L807-814): output_abs → 直接返回到期时间戳
- commandTimeSnapshot (server.c:221-232): cmd_time_snapshot — call() 内冻结, 脚本期间恒定 (注释: #1525, 防止命令内重查同一键过期状态翻转; 传播一致)
- persistCommand (L817-830): lookupKeyWrite (过期键会被惰性删 → 0) → removeExpire (R-21 db.c:1838) → notify "persist"
- touchCommand (L833-838): lookupKeyRead 计数 (LRU 更新副作用, 不返回值) — CLIENT_NO_TOUCH 豁免 (db.c:104-106 特判 touchCommand)

## 代码类型
Glue (查询面)

## 跨域关联
- R-21 (lookupKey 家族/removeExpire) / R-1 (LRU/TOUCH 特判)

## 结论
TTL 三值语义 (-2/-1/剩余) 是协议面标准; 时间一律走 commandTimeSnapshot 保证命令内一致。TOUCH 是"查询即更新 LRU"的显式命令 (DB 层面特判豁免 NO_TOUCH)。
源码位置: expire.c:562-570,773-838; server.c:221-232
