# 闭环笔记 q1: 双结构 — dict 管存在, skiplist 管有序

## 假设
zset 用 dict (member→score, O(1) 存在性/查分) + skiplist (score 有序, O(log n) 范围/排名) 双结构 — 单一结构无法同时满足两个操作面。

## 验证过程
- server.h:1357-1360: `typedef struct zset { dict *dict; zskiplist *zsl; } zset;`
- 分工:
  - dict: ZSCORE/ZREM 定位 (O(1) 成员查找); zsetAdd 的 `dictAdd(dict, ele, &score)` (L1500+)
  - skiplist: ZRANGE (有序遍历)/ZRANK (排名)/ZCOUNT (范围)
- 一致性维护: **所有写操作双写** — zsetAdd: zslInsert + dictAdd (L1520-1530 区域); zsetDel: zslDelete + dictDelete
- 为什么不用红黑树/平衡树: skiplist 实现简单 (无旋转), 并发友好 (Redis 单线程无碍), 且天然支持范围遍历 (forward 链)
- 为什么不用单一 dict: dict 无序 (ZRANGE 要排序 O(n log n)); 单一 skiplist: 成员存在性检查 O(log n) 但 ZSCORE 也是 O(log n) — 可接受但 dict 更快 + 双结构允许 zsl 只存 ele (无重复存储 score? 不 — zslNode 有 score 字段, 双份 score)
- 空间代价: 成员 + score 双份 (dict 一份, skiplist 一份) + 每节点 level 指针 — 空间换双操作面 O(log n)

## 代码类型
Algorithmic (双结构互补) — 架构决策

## 跨域关联
- R-3 (dict) → 成员表
- R-4 (sds ele) → 成员载体
- R-19 (listpack 小规模替代) → 双编码

## 结论
双结构 = 两个操作面的最优解: dict 管"有没有/分多少" (O(1)), skiplist 管"什么顺序/第几个" (O(log n)); 写路径双写保证一致 (单线程下无锁)。空间翻倍换时间, Redis 的经典"空间换时间"决策。
源码位置: server.h:1357-1360; t_zset.c:1425-1530,1589-1600
