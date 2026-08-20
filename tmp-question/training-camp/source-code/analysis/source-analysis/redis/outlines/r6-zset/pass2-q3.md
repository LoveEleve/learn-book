# 闭环笔记 q3: span 跨度 — O(log n) 排名的数学

## 假设
每个 level 存 span (该层 forward 指针跨越的节点数) — 从 header 沿最高层走, 累加 span 即得排名 — 排名 O(log n) 而非 O(n)。

## 验证过程
- 结构 (server.h:1345-1348): `struct zskiplistLevel { forward; unsigned long span; }` — **span = 此层跳跃覆盖的节点数**
- zslGetRank (t_zset.c:508-528): 从 header 顶层走, `rank += x->level[i].span` — 累积跨越数 = 目标节点排名 (1-based)
- zslGetElementByRank (L530+): 反向 — `traversed + span <= rank` 前进, 命中 traversed==rank — 排名定位元素
- 插入维护 (zslInsert L171-183): 新节点 span = update[i] 原 span - (rank[0]-rank[i]); update[i] 新 span = (rank[0]-rank[i])+1; 高层 span++
- 删除维护 (zslDeleteNode L196-205): forward==x → span += x.span-1; 否则 span--
- 复杂度: 查找路径 O(log n) 层 × 每层常数 → **ZRANK/ZREVRANK O(log n)**; 无 span 时排名需逐节点数 (O(n))
- 历史: 早期 Redis skiplist 无 span (排名 O(n)), span 引入后 ZRANK 提速 — 时空溯源证据 (代码注释/演进)

## 代码类型
Algorithmic (跨度维护) — 排名优化的关键

## 跨域关联
- q5 (插入算法) → span 更新公式
- R-21 (ZRANK 命令消费) → 排名面
- R-27 (ZUNION/ZINTER 排名?) → 聚合面

## 结论
span = "跳表版的索引距离": 每层记录跳跃跨度, 排名 = 路径上 span 累加 — O(log n); 插入/删除用 rank 差 O(1) 维护 span。这是 skiplist 相对"链表排序"的杀手锏: 距离信息随跳随取。
源码位置: server.h:1345-1348; t_zset.c:171-183,508-545
