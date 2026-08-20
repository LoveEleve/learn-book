# R-13 GEO — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2013-2014 (ardb) | yinqiwen ardb 项目的 geohash 库初版 (geohash.c/geohash.h/geohash_helper.c 版权 2013-2014 yinqiwen + Matt Stancliff); geohash_helper.c 是 ardb C++ 的 C 转换 (L32-35 注释, 指向 github.com/yinqiwen/ardb 原始文件) |
| 2014 | Matt Stancliff 移植到 Redis (geo.c 版权 2014-Present); zset 承载 + 52bit score 方案定型 |
| 3.2.0 (2016) | GEOADD/GEORADIUS/GEORADIUSBYMEMBER/GEOPOS/GEODIST/GEOHASH 发布 (commands.def 实证) |
| 3.2.10 | GEORADIUS_RO/GEORADIUSBYMEMBER_RO 只读变体 (RADIUS_NOSTORE) |
| 6.2.0 | **GEOSEARCH/GEOSEARCHSTORE** (BYRADIUS/BYBOX 双形状, FROMMEMBER/FROMLONLAT); GEORADIUS 族标 CMD_DOC_DEPRECATED |
| 7.x | GEOSEARCH 为主命令; geohash_helper.c 保留 ardb 起源注释 |

## 痕迹证据

- geohash.h:48-52: EPSG:900913/3785/OSGEO:41001 坐标系注释 (墨卡托投影来源)
- geohash.c:47-50: 位交错算法来源注释 (Stanford bithacks 页面)
- geohash.c:79-81: deinterleave 来源 (stackoverflow 4909263)
- geohash_helper.c:42-47: WGS-84 椭球常量 (R_MAJOR 6378137 / R_MINOR 6356752.3142 / ECCENT)
- geohash_helper.c:32-35: ardb C++ 转换注释 (时空溯源关键证据)
- geo.c:36-39: t_zset.c 导出接口声明 (zzlFirstInRange/zslValueLteMax) — 与 zset 深度耦合的证据
- geo.c:894-898: 内部 -85/85 vs 标准 -90/90 差异注释 (GEOHASH 重编码原因)

## 推断标注

- "经度格 ≈0.6m / 纬度格 ≈0.3m" — 由 2^26 定点推算, 代码无注释
- "10km 半径 → step 10" — 对折算法手算, 非代码断言
- "墨卡托半周长近似" — MERCATOR_MAX=20037726.37 与真实 20037508 差异的解读
- "geohash_helper.c 的 C++ 转换时间" — 版本推断 (版权 2015-current Redis Ltd.)
