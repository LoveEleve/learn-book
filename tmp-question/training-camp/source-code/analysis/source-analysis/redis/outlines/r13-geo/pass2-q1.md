# 闭环笔记 q1: 52bit geohash 编码 — 范围归一 + 定点 + 位交错

## 假设
26 位经度 + 26 位纬度交错成 52bit; 范围 ±180/±85.05112878; 定点 ×2^26。

## 验证过程
- GEO_STEP_MAX=26 (geohash.h:46, "26*2 = 52 bits") ✅; 范围 GEO_LONG_MIN/MAX=±180, GEO_LAT_MIN/MAX=±85.05112878 (geohash.h:49-52, EPSG:900913 墨卡托投影纬度极限 — 注释 L48)
- geohashEncode (geohash.c:121-151): 范围归一 offset=(v-min)/(max-min) (L141-144) → 定点 `×2^step` (L147-148) → `interleave64(lat_offset, long_offset)` (L149)
- **interleave64 位布局 (L52-77): x=lat → 偶数位 (bit0,2,4...), y=lon → 奇数位** (L47-49 注释 "bits of x are in the even positions and bits from y in the odd") — **与标准 geohash 相反** (标准 lon 偶数位) — 内部自洽, 对外无影响
- 有效性检查: step>32/step==0/越界 → 0 (L125-131); 坐标系转换 (L153-162)
- 精度: 经度格 360/2^26 ≈ 5.4e-6° ≈ **0.6m** (赤道), 纬度格 170.1/2^26 ≈ 2.5e-6° ≈ **0.3m** (推断标注 — 代码无注释)
- geohashAlign52Bits (geohash_helper.c:213-217): `bits << (52 - step*2)` — 搜索时低 step 对齐到 52bit 顶, 保留前缀

## 代码类型
Mechanism (空间编码)

## 跨域关联
- R-12: **对照 — HLL 6bit 打包 (LSB-first) vs GEO 位交错**; 都是把信息压进数值, 但 HLL 是计数器, GEO 是空间前缀
- R-6: score 承载 (zset)

## 结论
52bit = 26 经 + 26 纬, 交错后作 zset score; 范围 -85.05~85.05 (墨卡托) 而非 ±90; 内部奇偶位与标准 geohash 相反, 自洽无碍。
源码位置: geohash.c:46,121-151; geohash_helper.c:213-217
