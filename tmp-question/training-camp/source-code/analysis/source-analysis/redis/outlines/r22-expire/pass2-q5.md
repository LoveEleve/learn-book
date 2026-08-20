# 闭环笔记 q5: EXPIRE 命令族 — 单位/标志/已过期重写

## 假设
EXPIRE 族统一走 expireGenericCommand: 单位换算 + 溢出守卫 + NX/XX/GT/LT + 已过期直接删并重写 DEL。

## 验证过程
- 四命令入口 (expire.c:753-770): EXPIRE (basetime=now, SECONDS) / EXPIREAT (0, SECONDS) / PEXPIRE (now, MS) / PEXPIREAT (0, MS)
- 溢出守卫 (L651-663): SECONDS → `when > LLONG_MAX/1000` 检查 ×1000; basetime 加法溢出检查
- NX/XX/GT/LT (L671-713):
  - NX: 已有 TTL → 0 (L675-680)
  - XX: 无 TTL → 0 (L683-689)
  - GT: when <= current 或无 TTL (视为无限) → 0 (L692-700)
  - LT: 无 TTL 视为无限但可设; 有 TTL 且 when >= current → 0 (L703-712)
  - 互斥校验 (parseExtendedExpireArgumentsOrReply L609-617): NX×XX/GT/LT 与 GT×LT 不兼容
- **checkAlreadyExpired** (L562-570): `when <= commandTimeSnapshot() && !loading && !masterhost` — 过期时间已到且非加载/非从库 → **直接删 + 重写**
- 已过期路径 (L715-728): dbGenericDelete (lazyfree_lazy_expire) → 重写 argv 为 DEL/UNLINK (L723-724) → notify "del" → 返回 1
- 正常路径 (L729-748): setExpire (R-21) → **统一重写为 PEXPIREAT <毫秒时间戳>** (L734-736, 已是最简格式则不动) + 时间参数归一 (L739-743) → notify "expire"
- 传播语义: 命令重写保证 AOF/从库只看到 PEXPIREAT/DEL — 主从时间无关

## 代码类型
Mechanism (命令规范化 + 传播一致)

## 跨域关联
- R-21 (setExpire/checkAlreadyExpired 的 loading 语义) / R-8 (AOF 重写) / R-9 (复制传播)

## 结论
EXPIRE 族 = 统一入参 (毫秒绝对时间戳) + 四标志条件设置 + "已过期当 DEL" 短路。传播统一到 PEXPIREAT/DEL 两种形式, 消除主从时钟差。loading/从库豁免保证 AOF 重放与复制命令不被误删。
源码位置: expire.c:562-570,584-620,635-750
