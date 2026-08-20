# 闭环笔记 q3: 半径搜索三步 — step 估计 + 包围盒 + 9 盒扫描

## 假设
半径→step 对折估计; 包围盒边界按纬度修正; 9 盒覆盖, 去重复/剔除无用邻格。

## 验证过程
- geohashEstimateStepsByRadius (geohash_helper.c:62-83):
  - 零半径 → 26 (L63); 循环 `range*2, step++` 直到 ≥ MERCATOR_MAX=20037726.37 (L65-68) — **对折法把半径撑到半周长量级**
  - `step -= 2` 冗余保证覆盖 (L69); 高纬 |lat|>66 再 -1, >80 再 -1 (L74-77); clamp 1..26 (L80-81)
  - 例: 10km → 10000×2^11=20480000 ≥ 20037726 → step=12-2=**10** (推断计算)
- geohashBoundingBox (L98-116): 半径=height=width; `lat_delta = rad_deg(height/R)` (L105); **long_delta_top/bottom 用 ±lat_delta 的 cos 修正** (L106-107, 纬线收缩); **南半球 min/max 反转** (L110-112, 南北纬线弯曲方向相反 — L88-96 图示)
- geohashCalculateAreasByShapeWGS84 (L121-211):
  - 矩形半径 = 对角线 `sqrt((w/2)²+(h/2)²)` (L142-143)
  - encode 中心 (step 估计) + neighbors + decode (L148-151)
  - **decrease_step 复核** (L158-182): 解码 4 邻核对包围盒是否被覆盖, 不足则 step-1 重算 — 边界安全网
  - **无用邻格剔除 GZERO** (L185-206, steps≥2): 邻格越界包围盒 → 置零跳过 (性能+正确性)
- membersOfAllNeighbors (geo.c:365-421): 9 盒 (自+8邻, L370-378); **大半径相邻格相同 → 跳过去重** (L408-415, "5000 km range or more" L404-407); 每个盒 scoresOfGeoHashBox → 前缀范围 [align(h), align(h+1)) (L328-352, 注释图示 L333-347)

## 代码类型
Mechanism (空间搜索)

## 跨域关联
- R-6: zslNthInRange 前缀查找 (skiplist 天然支持 score 范围)
- R-12: 对照 — HLL 无邻居概念, GEO 的空间局部性是搜索核心

## 结论
三步: 半径→step (对折+高纬修正+复核) → 包围盒 (cos 修正+南半球反转) → 9 盒扫描 (去重+剔除)。搜索面 = 以 9 个"粗格"覆盖半径圆, 精确过滤留给后续。
源码位置: geohash_helper.c:62-83,98-116,121-211; geo.c:328-421
