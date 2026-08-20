# R-13 GEO — completeness-questions

## 开发者视角

1. 52bit 编码 = 26 经 + 26 纬怎么交错?
2. GEOADD 为什么是 ZADD 包装? score 是什么?
3. 半径→step 的估计算法?
4. 9 盒扫描怎么覆盖半径圆?
5. geoGetPointsInRange 的双编码遍历?
6. haversine 距离公式与同经度捷径?
7. GEOHASH 命令为什么重编码 -90/90?
8. GEODIST 的单位换算?

## 架构师视角

9. 为什么范围是 -85.05~85.05 (墨卡托)?
10. 内部奇偶位与标准 geohash 相反的影响?
11. bounding box 南北半球差异的设计?
12. qsort vs pqsort 的取舍 (COUNT 场景)?
13. GEOADD 委托 ZADD 的架构收益?
14. GEOSEARCH 替代 GEORADIUS 的动机?
15. STORE 手工建 zset 的路径?
16. 粗筛+精筛两段式的复杂度?

## 学生视角

17. 10km 半径搜索 step 是多少? 为什么?
18. 11 字符 geohash 第 11 位为什么恒 '0'?
19. COUNT 为什么强制 ASC 排序?
20. 50km 半径 vs 5km 半径的邻居去重差异?
