# 闭环笔记 q5: zslInsert 算法 — update/rank 双数组与 span 维护

## 假设
插入先自顶向下找位置 (记录每层 update 前驱 + rank 累计排名), 再自底向上链接 + 用 rank 差更新 span — O(log n) 查找 + O(level) 链接。

## 验证过程
- zslInsert (t_zset.c:137-192):
  - L144-156 查找: 自顶向下, `rank[i] = i==level-1 ? 0 : rank[i+1]` (高层累计继承) + while 前进时 `rank[i] += span` — **每层记录"到达插入位跨越的节点数"**
  - L161-169 层级: 随机新层 > 当前 → 初始化 update[i]=header, span=length (新层跨整个表)
  - L170-178 链接: 每层 `x->forward = update[i]->forward; update[i]->forward = x`; **span 更新: x->span = update 原 span - (rank[0]-rank[i]); update->span = (rank[0]-rank[i])+1**
  - L181-183: 未涉及层 span++
  - L185-190: backward 链 + tail 更新
- span 公式含义: rank[0] = 插入位总排名 (0-based); rank[i] = 在 i 层前驱的排名 — **rank[0]-rank[i] = 前驱在第 i 层跨越到插入位的距离**; 新节点在第 i 层跨 (原 span - 这段) 个节点; 前驱跨 (这段+1) (含新节点)
- 复杂度: 查找 O(log n) (P=0.25 下 log_4 n), 链接 O(level) ≈ O(log n) 期望

## 代码类型
Algorithmic (概率结构插入) — 核心算法

## 跨域关联
- q2 (层级概率) → 新层随机
- q3 (span) → 维护公式
- R-19 (listpack 编码的 zzlInsert) → 小规模替代

## 结论
zslInsert = 查找 (rank 累计) + 链接 (span 差更新): rank 数组让 span 维护 O(1)/层 — 不重扫; 新层初始化 span=length (整表跨)。这是 skiplist 插入的教科书实现 + span 增强。
源码位置: t_zset.c:137-192
