# 闭环笔记 q5: 解码面 — GEOPOS/GEODIST/GEOHASH 重编码

## 假设
GEOPOS 解码 52bit; GEODIST haversine/单位; GEOHASH 输出标准 11 字符需重编码。

## 验证过程
- decodeGeohash (geo.c:92-95): `step=GEO_STEP_MAX` 固定 26 → geohashDecodeToLongLatWGS84 (取格中心, geohash.c:206-215 midpoint)
- GEOPOS (geo.c:940-966): zsetScore (L952) + decode (L957) → [lon, lat] 两元素数组; 缺成员 NULL (L953/L958)
- GEODIST (geo.c:973-1005): argc==5 有单位 (L977-979) / >5 syntaxerr (L980-983); 两 score 解码 (L1000) → haversine/单位 (L1003-1004); 缺成员 NULL
- **GEOHASH 命令 (geo.c:878-934) — 重编码而非直接输出**:
  - 内部范围 -85/85 ≠ 标准 -90/90 (L894-898 注释) → **decode 再 encode 标准范围** (L901-914)
  - base32 字母表 `"0123456789bcdefghjkmnpqrstuvwxyz"` (L879) — 标准 geohash 表 (去 a/i/l/o)
  - **11 字符 = 55bit > 52bit → 第 11 位 idx=0 → 恒 '0'** (L918-929, "assume zero" L923)
  - 字符取位: `bits >> (52-(i+1)*5) & 0x1f` (L926) — 高 5 位组优先
- 单位换算 extractUnitOrReply (L134-150): m=1 / km=1000 / ft=0.3048 / mi=1609.34
- addReplyDoubleDistance (L212-216): fixedpoint 4 位小数 (距离回复精度)

## 代码类型
Command + Mechanism (解码)

## 跨域关联
- R-6: zsetScore (双编码查找)
- R-12: 对照 — HLL magic 头校验 vs GEO 无校验 (score 本身自描述)

## 结论
三个读命令直接解码; GEOHASH 因内部-85/85 与标准 -90/90 差异必须 decode→re-encode; 11 字符是 55bit 兼容格式, 第 11 位恒 '0'。
源码位置: geo.c:92-95,134-150,212-216,878-1005
