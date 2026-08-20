# 闭环笔记 q6: 范围查询 — IsInRange 首尾检查 + zslNthInRange

## 假设
范围查询两步: zslIsInRange 用首尾元素快速判空 (O(1)); zslNthInRange 从 header 顶层跳过范围外区域定位边界 (O(log n))。7.x 用 zslNthInRange 统一替代 FirstInRange/LastInRange。

## 验证过程
- zslIsInRange (t_zset.c:317-334): 空范围判定 (`min > max` 或 `min==max && 开区间`) → tail (最大) 必须 ≥ min → header 第一节点必须 ≤ max — **O(1) 判空**
- zslNthInRange (L336-410): `!zslIsInRange → NULL`; 从 header 顶层 while 跳过 `!zslValueGteMin(forward->score)` 的区域 (记录 edge_rank); 若 n 小 (ZSKIPLIST_MAX_SEARCH) → 逐节点; 否则 **zslGetElementByRankFromNode(last_highest_level_node, ...)** — 记录"最高层最后一个范围外节点" + 排名差定位
- 消费者: ZRANGE BYSCORE offset (L3345-3347: n=offset/-offset-1), ZCOUNT (L3443/3451: n=0/-1 取首末)
- 复杂度: 跳过范围外 O(log n) + 定位偏移 O(log n) — 全程不扫描范围内全部元素 (ZCOUNT 除外 — 它要数)
- ZSKIPLIST_MAX_SEARCH 优化 (L370): 偏移小时逐节点跳比跳表路径更省 (跳表每层开销 > 1 次比较)

## 代码类型
Algorithmic (范围定位) — skiplist 招牌卖点

## 跨域关联
- R-21 (ZRANGE/ZCOUNT/ZREVRANGE 命令) → 消费者
- q3 (rank 工具) → 定位原理
- 对照 R-3 (dict 无范围概念) → 双结构分工

## 结论
范围查询 = 判空 (首尾 O(1)) + 定位 (跳表 O(log n)): 从最高层跳过范围外区域, 记录"最后一个出界节点"再用排名差跳进范围 — 7.x 用 zslNthInRange 统一首/末/偏移三态 (n=0/-1/offset)。这是 skiplist 相对平衡树的最大工程优势: 范围遍历不需要中序遍历。
源码位置: t_zset.c:317-410,3345-3451
