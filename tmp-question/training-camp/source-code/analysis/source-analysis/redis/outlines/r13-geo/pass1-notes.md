# R-13 GEO — Pass 1 探索笔记

> 域: R-13 GEO (geo.c + geohash.c + geohash_helper.c) | 🟡 B 方案 | 2026-08-13
> 源码: src/geo.c (1005) + geohash.c (299) + geohash_helper.c (280) + geohash.h (135) | Redis 7.4.2

## 调用图

```
编码层 (geohash.c):
interleave64 (L52-77): 位交错 — x=lat 偶数位, y=lon 奇数位 (与标准 geohash 相反!)
geohashEncode (L121-151): 范围归一 → 定点 (×2^step) → interleave
geohashDecode (L164-194): deinterleave → 格边界 (min/max 含 +1 格)
geohashNeighbors (L266-299): 8 邻格 (geohash_move_x/y 加减掩码)

搜索层 (geohash_helper.c):
geohashEstimateStepsByRadius (L62-83): 半径 → step 估计 (MERCATOR_MAX 对折法 + 高纬修正)
geohashBoundingBox (L98-116): 形状 → 经纬度边界 (南半球反转)
geohashCalculateAreasByShapeWGS84 (L121-211): 盒 → step → 中心+8 邻 → 去无用邻格
geohashGetDistance (L229-242): haversine (v==0 同经度捷径)
geohashAlign52Bits (L213-217): bits << (52-step*2)

命令层 (geo.c):
GEOADD (L445-503): **ZADD 包装** — encode(STEP_MAX=26) → score → replaceClientCommandVector
georadiusGeneric (L523-844): 5 flags 合一 (COORDS/MEMBER/NOSTORE/GEOSEARCH/GEOSEARCHSTORE)
  → 参数矩阵 → geohashCalculateAreasByShapeWGS84 → membersOfAllNeighbors (L365-421, 9 盒)
  → geoGetPointsInRange (L261-323, 双编码 zslNthInRange/zzlFirstInRange)
  → geoWithinShape 精确过滤 (L232-247) → qsort/pqsort → 回复/STORE
GEOHASH (L878-934): 重编码标准 -90/90 → 11 字符 base32
GEOPOS (L940-966) / GEODIST (L973-1005)

外部接入 (grep 实证): commands.def L11019-11028 (10 命令) — geoadd/georadius 族 3.2.0, GEOSEARCH 族 6.2.0; zzlFirstInRange/zslValueLteMax 导出自 t_zset.c (L920/L312, geo.c:36-39 声明)
```

## 基本元素分解

1. 52bit 编码: 范围归一 + 定点 2^26 + 位交错 (lat 偶数位/lon 奇数位)
2. GEOADD = ZADD 包装: score 即 geohash, 零新类型
3. 半径→step 估计: MERCATOR_MAX 对折 + 高纬修正 + decrease_step 复核
4. 9 盒邻居扫描: 中心 + 8 邻 + 大半径去重 + 无用邻格剔除
5. 精确过滤: haversine/矩形双模式 + qsort/pqsort + COUNT/ANY
6. 解码面: GEOPOS/GEODIST/重编码标准 geohash 11 字符

## 标记问题 (20 问)

1. 52bit 怎么来的 (26×2)?
2. interleave64 的奇偶位布局与标准 geohash 差异?
3. 定点编码 (×2^step) 的精度?
4. GEOADD 为什么是 ZADD 包装?
5. 半径搜索的 step 怎么定 (EstimateStepsByRadius)?
6. bounding box 的南北半球差异?
7. 9 盒扫描的去重与剔除?
8. scoresOfGeoHashBox 的前缀范围?
9. geoGetPointsInRange 的双编码遍历?
10. haversine 公式与同经度捷径?
11. 矩形过滤 (GetDistanceIfInRectangle) 的顺序?
12. qsort vs pqsort 的取舍?
13. COUNT/ANY/强制 ASC 的语义?
14. STORE 路径的手工建 zset?
15. GEOHASH 命令为什么重编码 -90/90?
16. 11 字符 base32 为什么第 11 位恒 '0'?
17. GEODIST 的单位换算?
18. 缺键时 STORE/非 STORE 的分支?
19. GEOSEARCH 的 BYRADIUS/BYBOX 参数矩阵?
20. 命令族演化 (3.2 → 6.2 deprecated)?

## 时空溯源 (代码内痕迹)

- 2013-2014 (yinqiwen ardb 项目): geohash 库初版 (geohash.c 版权 2013-2014 yinqiwen + Matt Stancliff)
- 2014: Matt Stancliff 移植至 Redis (geo.c 版权 2014-Present); geohash_helper.c 是 ardb C++ 的 C 转换 (L32-35 注释)
- 3.2.0 (2016): GEOADD/GEORADIUS/GEORADIUSBYMEMBER/GEOPOS/GEODIST/GEOHASH 发布
- 3.2.10: GEORADIUS_RO/GEORADIUSBYMEMBER_RO 只读变体
- 6.2.0: GEOSEARCH/GEOSEARCHSTORE 发布 (BYRADIUS/BYBOX 双形状), GEORADIUS 族标 deprecated (commands.def CMD_DOC_DEPRECATED)
- 7.x: GEOSEARCH 成为主命令

## 大域拆分判断

3 文件 1584 行 — **不拆** (🟡 B, 6 闭环足够)
