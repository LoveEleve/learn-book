# 闭环笔记 q6: GEOSEARCH 双形状 + 命令族演化

## 假设
georadiusGeneric 五 flags 合一; GEOSEARCH 增加 BYBOX; STORE 路径手工建 zset; 命令族 3.2→6.2 演化。

## 验证过程
- 命令注册 (commands.def L11019-11028): geoadd/georadius/georadiusbymember/geopos/geodist/geohash **3.2.0**; georadius_ro/bymember_ro **3.2.10**; geosearch/geosearchstore **6.2.0**; **GEORADIUS 族 deprecated (6.2.0)** — GEOSEARCH 上位
- georadiusGeneric (geo.c:523-844): flags 5 位 (L509-513: COORDS/MEMBER/NOSTORE/GEOSEARCH/GEOSEARCHSTORE)
  - 形状与中心: COORDS → 坐标+半径 (L534-539) / MEMBER → 成员定位 (L544-553, longLatFromMember L120-126) / GEOSEARCH → FROMMEMBER|FROMLONLAT+BYRADIUS|BYBOX 矩阵 (L554-560, 参数解析 L617-662)
  - 互斥校验: WITHDIST 等与 STORE 不兼容 (L671-676); FROMMEMBER/FROMLONLAT 二选一 (L678-683); BYRADIUS/BYBOX 二选一 (L685-690); ANY 需 COUNT (L692-695)
  - **缺键分支** (L698-712): 有 STORE → 删目标键+回复 0; 无 → 空数组
  - COUNT 无排序 → 强制 ASC (L718); ANY → limit=count 提前退出 (L725)
  - STORE 路径 (L803-842): **手工建 zset** (createZsetObject + zslInsert + dictAdd, L811-827) → zsetConvertToListpackIfNeeded (L830) → setKey (L831) → notify geosearchstore/georadiusstore (L833); STOREDIST 时 score=距离 (L819); 空结果删目标键 (L836-840)
  - 回复: 数组/嵌套选项 (L774-802); 距离除以 conversion 按单位 (L780)
- 只读变体: RADIUS_NOSTORE (L511, L858/L863) — GEORADIUS_RO 拒 STORE

## 代码类型
Command (家族) + Mechanism (STORE)

## 跨域关联
- R-6: zslInsert/dictAdd/zsetConvertToListpackIfNeeded — STORE 直接复用内部 API
- R-21: lookupKeyRead/checkType/dbDelete/setKey/notify
- R-23: CMD_DENYOOM (写命令)

## 结论
五命令合一框架; GEOSEARCH 6.2 用 FROMMEMBER/FROMLONLAT + BYRADIUS/BYBOX 替换 GEORADIUS 参数面; STORE 走手工 zset 构建; 只读变体禁 STORE。
源码位置: geo.c:509-513,523-712,803-872; commands.def:11019-11028
