# 闭环笔记 q4: 精确过滤与排序 — haversine + 双编码遍历 + pqsort

## 假设
9 盒粗筛后逐点精确过滤; 距离 haversine; 排序全量/部分两路; COUNT/ANY 提前退出。

## 验证过程
- geoGetPointsInRange (geo.c:261-323): 范围 `[min, max)` (L264, minex=0/maxex=1)
  - **LISTPACK 编码**: zzlFirstInRange (L274, 导出自 t_zset.c:920) + lpNext 迭代 (L279-297)
  - **SKIPLIST 编码**: zslNthInRange (L303, R-6 已讲) + level[0].forward 迭代 (L308-320)
  - 每点 geoWithinShape 过滤 (L290/L314); member 拷贝 sdsdup/sdsnewlen (L292,L316); limit 提前 break (L295,L318)
- geoWithinShape (L232-247): CIRCULAR → geohashGetDistanceIfInRadiusWGS84 / RECTANGLE → GetDistanceIfInRectangle (L236-245); 距离与形状半径比较
- 距离计算 (geohash_helper.c):
  - **haversine** geohashGetDistance (L229-242): `a = u² + cos(lat1)cos(lat2)v²`, `2R·asin(√a)`; **v==0 (同经度) 捷径** → geohashGetLatDistance = R·|Δlat| (L235-236, L224-226)
  - **矩形过滤顺序** (L266-280): 先纬度 (便宜) L270-273, 后经度 (在 y2 纬度算, L274), 最后才算真实距离 (L278) — 失败早退
  - EARTH_RADIUS_IN_METERS=6372797.560856 (L52, WGS-84 二次平均半径)
- 排序 (geo.c:423-438, 740-754): asc/desc 比较器; **全量 qsort vs 部分 pqsort** (L748-753, 只排 returned_items 个 — pqsort.h 引入); COUNT 无排序强制 ASC (L718, 注释 "COUNT without ordering does not make much sense" L714-717); ANY 不排序
- COUNT/ANY 语义: ANY 需 COUNT (L692-695); ANY 时 limit=count 传入扫描提前退出 (L725)

## 代码类型
Mechanism (空间过滤) + 性能优化

## 跨域关联
- R-6: zslNthInRange 复用
- R-26: 无 (非阻塞)
- R-27: qsort 最小集 (sinter) — 同 pqsort 家族? 否, R-27 用 qsort 全量, GEO 引入 pqsort 部分 — 对照记录

## 结论
粗筛(9盒) + 精筛(几何) 两段式; 距离便宜优先 (同经度捷径/矩形先纬后经); 排序按需选择全量/部分。COUNT 隐含 ASC 排序保证最近 N。
源码位置: geo.c:232-323,423-438,718,740-754; geohash_helper.c:224-280
