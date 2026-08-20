# R-6 skiplist+ZSet — completeness-questions

## 开发者视角

1. zset 为什么是 dict + skiplist 两个结构?各管什么?
2. 跳表的高度怎么决定?为什么 32 层封顶?
3. span 是什么?ZRANK 怎么用它?
4. 两个 member 同分, 谁排在前面?
5. ZADD INCR 怎么处理?NaN 会发生什么?
6. zset 什么时候从 listpack 变成 skiplist?
7. ZRANGEBYSCORE 怎么跳过范围外的元素?
8. 删除元素后跳表层数会立即变少吗?

## 架构师视角

9. 双结构的一致性怎么保证?单线程的优势?
10. P=0.25 vs P=0.5 — 为什么 Redis 选 1/4?期望层数 1.33 怎么算?
11. span 距离索引 — 排名 O(log n) 的数学?插入怎么 O(1)/层维护?
12. (score, ele) 复合全序 — 为什么需要?对命令确定性的意义?
13. update/rank 双数组 — 一次遍历拿到两份信息的设计?
14. zslNthInRange 统一首/末/偏移 — 7.x 重构的动机?
15. 为什么用 skiplist 不用红黑树?工程权衡?
16. dictExpand 预扩在转换中的作用 — 为什么转换要零 rehash?

## 学生视角

17. ZADD member score 后, 数据怎么进入两个结构?
18. ZRANK 的过程: 从 header 怎么走到目标?
19. 同分排序: score 和 ele 怎么组合比较?
20. 跳表和普通链表的区别?为什么叫"跳"表?
