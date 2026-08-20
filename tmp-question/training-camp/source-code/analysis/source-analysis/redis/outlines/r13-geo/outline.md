# R-13 GEO — 空间索引 (52bit 编码 + ZSet 载体)

> 前置: [[R-6-zset]] (ZSet 双结构) + [[R-12-hll]] (编码对照) + [[R-21-db]] (键空间) + [[R-1-object]] (字符串) | 引出: [[R-15-cluster]] (槽位散列对照) | 对照: [[R-12-hll]] (概率 vs 空间编码) + [[R-11-bitmap]] (位级操作)
> 🟡 B | 6 KP | [模式: 空间编码 + 包装委托 + 网格扫描 + 两段式过滤 + 解码重编码]
> Pass 2 闭环: q1(52bit 编码) q2(GEOADD=ZADD) q3(半径三步) q4(过滤排序) q5(解码面) q6(命令族)

**读者处境**: 附近的人怎么找? 经纬度怎么塞进 zset 的 score? 为什么 GEOADD 的 key 用 ZRANGE 也能查? 精度 0.6m 怎么保证? 这篇拆空间索引: 52bit 位交错、ZADD 包装、半径→step 估计、9 盒扫描、haversine 精筛、GEOSEARCH 双形状。

### 1. 52bit 编码 — 范围归一 + 位交错

场景: 经纬度怎么变成一个可排序的数值?
源码路径:
- GEO_STEP_MAX=26 (geohash.h:46, "26*2 = 52 bits"); 范围 **±180 / ±85.05112878** (geohash.h:49-52, EPSG:900913 墨卡托投影纬度极限)
- geohashEncode (geohash.c:121-151): 归一 offset=(v-min)/(max-min) (L141-144) → 定点 ×2^26 (L147-148) → **interleave64(lat_offset, long_offset)** (L149)
- **奇偶位布局** (geohash.c:52-77): x=lat → 偶数位, y=lon → 奇数位 (L47-49 注释) — **与标准 geohash 相反** (标准 lon 偶数位), 内部自洽
- geohashAlign52Bits (geohash_helper.c:213-217): `bits << (52 - step*2)` — 搜索时对齐 52bit 顶保前缀
- 精度: 经度格 360/2^26 ≈ 0.6m / 纬度格 170.1/2^26 ≈ 0.3m (推断标注)
关键设计 (q1): **位交错 = 空间局部性**: 前缀越长, 格子越细; score 大小序 = 空间 Morton 序。[模式: 空间编码]
数据流: (lon,lat) → 归一 → 定点 26bit×2 → 交错 52bit → zset score。

### 2. GEOADD — ZADD 包装

场景: GEOADD 为什么不自己建数据结构?
源码路径:
- geoaddCommand (geo.c:445-503): 解析 NX/XX/CH (L451-458, CH 透传 "Handle in zaddCommand" L455) → 参数数 %3 校验 (L460-464)
- **构造 zadd 向量**: argv[0]="zadd" (L470); 每元素 encode(STEP_MAX=26) + Align52 → score (L491-493)
- **replaceClientCommandVector + zaddCommand** (L501-502) — 命令重写, 复用 R-6 全部语义
关键设计 (q2): **零新类型**: GEO 的正确性 = zset + 编码函数; NX/XX/CH/传播全继承 ZADD。[模式: 包装委托]
数据流: GEOADD k lon lat name → 编码 score → ZADD k score name。

### 3. 半径搜索三步 — step 估计 + 包围盒 + 9 盒

场景: 半径查询怎么把"圆"转成 zset 的范围查询?
源码路径:
- geohashEstimateStepsByRadius (geohash_helper.c:62-83): 半径×2 对折到 MERCATOR_MAX=20037726.37 (L65-68) → step-2 冗余 (L69) → 高纬 |lat|>66/-1, >80/-1 (L74-77) → clamp 1..26 (L80-81)
- geohashBoundingBox (L98-116): lat_delta=rad_deg(h/R) (L105); **long_delta_top/bottom 用 cos(±lat_delta) 修正** (L106-107 纬线收缩); **南半球 min/max 反转** (L110-112)
- geohashCalculateAreasByShapeWGS84 (L121-211): 矩形半径=对角线 (L142-143) → encode 中心+8 邻 (L148-151, geohash_move_x/y 位运算 geohash.c:228-264) → **decrease_step 复核** (L158-182 边界安全网) → **GZERO 剔除无用邻格** (L185-206, 需 steps≥2, geohash_helper.h:37)
- membersOfAllNeighbors (geo.c:365-421): 9 盒循环; **大半径相邻同格去重** (L408-415); scoresOfGeoHashBox 前缀范围 [align(h), align(h+1)) (L328-352)
关键设计 (q3): **网格覆盖圆**: 用 9 个粗格覆盖搜索圆, 粗筛后精筛; step 自适应半径与纬度。[模式: 网格扫描]
数据流: 半径/形状 → step → 中心 9 盒 → 每盒前缀范围 → 粗筛候选。

### 4. 精确过滤与排序 — haversine + 两段式

场景: 粗筛出的候选怎么变成精确答案?
源码路径:
- geoGetPointsInRange (geo.c:261-323): 范围 [min,max) (L264); **LISTPACK: zzlFirstInRange** (L274, 导出 t_zset.c:920) / **SKIPLIST: zslNthInRange** (L303, R-6) 双编码遍历
- geoWithinShape (L232-247): CIRCULAR → 半径距离比较 / RECTANGLE → 矩形判定
- **haversine** (geohash_helper.c:229-242): 2R·asin(√(u²+cos(lat1)cos(lat2)v²)); **v==0 同经度捷径** → R·|Δlat| (L235-236); R=EARTH_RADIUS_IN_METERS=6372797.560856 (L52)
- **矩形过滤顺序** (L266-280): 先纬度 (便宜) → 后经度 → 最后真实距离 — 失败早退
- 排序 (geo.c:740-754): **全量 qsort vs 部分 pqsort** (L748-753, 只排 returned_items); **COUNT 无排序且非 ANY → 强制 ASC** (L718, 注释 L717 "not needed for ANY option"); ANY → limit 提前退出 (L725)
关键设计 (q4): **粗筛(9盒)+精筛(几何) 两段式**; 距离计算按成本排序 (捷径优先); 部分排序省 COUNT 场景。[模式: 两段式过滤]
数据流: 候选点 → geoWithinShape 精确距离 → 排序/截断 → 回复。

### 5. 解码面 — GEOPOS/GEODIST/GEOHASH 重编码

场景: 存储的 score 怎么还原成经纬度给人看?
源码路径:
- decodeGeohash (geo.c:92-95): 固定 step=26 → geohashDecodeToLongLatWGS84 (取格中心, geohash.c:206-215)
- GEOPOS (geo.c:940-966): zsetScore + decode → [lon,lat]; 缺成员 NULL
- GEODIST (geo.c:973-1005): 两 score 解码 → haversine/单位; 单位 m=1/km=1000/ft=0.3048/mi=1609.34 (L134-150)
- **GEOHASH 重编码** (geo.c:878-934): 内部 -85/85 ≠ 标准 -90/90 (L894-898 注释) → **decode→re-encode 标准范围** (L901-914); base32 字母表 "0123456789bcdefghjkmnpqrstuvwxyz" (L879, 去 a/i/l/o — 标准 geohash 约定, 推断标注); **11 字符=55bit>52bit → 第 11 位恒 '0'** (L920-924)
关键设计 (q5): 读命令零计算直达; GEOHASH 因内部坐标系差异必须重编码 (对外兼容标准)。[模式: 解码重编码]
数据流: score → 固定 step 解码 → 格中心坐标 → 单位换算/标准重编码。

### 6. 命令族 — GEOSEARCH 双形状与演化

场景: 五个命令怎么共存? STORE 怎么落盘?
源码路径:
- 注册 (commands.def:11019-11028): GEOADD/GEORADIUS 族 **3.2.0** / _RO 变体 **3.2.10** / GEOSEARCH 族 **6.2.0**; **GEORADIUS 族 deprecated (6.2.0)**
- georadiusGeneric (geo.c:523-844) 五 flags (L509-513): COORDS/MEMBER/NOSTORE/GEOSEARCH/GEOSEARCHSTORE
- GEOSEARCH 参数矩阵 (L554-560,617-662): FROMMEMBER\|FROMLONLAT 二选一 (L678-683) + BYRADIUS\|BYBOX 二选一 (L685-690); 互斥校验 (L671-695)
- 缺键分支 (L698-712): 有 STORE → 删目标键+回 0; 无 → 空数组
- **STORE 手工建 zset** (L803-842): zslInsert+dictAdd (L815-827) → zsetConvertToListpackIfNeeded (L830) → setKey (L831); STOREDIST score=距离 (L819); 空结果删键 (L836-840)
关键设计 (q6): **五命令合一框架**; GEOSEARCH 6.2 参数面统一替换 GEORADIUS; STORE 直用内部 API。[模式: 命令家族]
数据流: GEOSEARCH k FROMLONLAT x y BYRADIUS 10 km → 形状 → 9 盒 → 精筛 → 回复/STORE。

### 负面空间 — GEO 刻意不做的事

- **不做经纬度范围校验外的投影转换**: 直接用墨卡托范围, 无球面投影 (高纬精度损失由 cos 修正缓解)
- **不做地理哈希动态降精度**: score 固定 52bit, step 只在搜索期变化 (存储恒满精度)
- **不做跨 key 空间索引**: 单 zset 内搜索, 无全局空间树
- **不做距离排序缓存**: 每次查询重算 haversine
- **不做删除地理单元**: 成员级删除走 ZREM (R-6)
- **不做圆内格完全覆盖保证**: 9 盒 + decrease_step 只是近似覆盖搜索圆, **精筛只过滤不补漏** (边界格成员可能漏检 — decrease_step 已尽量缓解)

→ 引出: 16384 槽位怎么按 key 散列? → [[R-15-cluster]]
