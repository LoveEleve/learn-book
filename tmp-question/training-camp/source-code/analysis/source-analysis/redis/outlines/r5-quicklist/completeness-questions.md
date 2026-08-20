# R-5 quicklist — completeness-questions

## 开发者视角

1. quicklist 和普通链表有什么区别?为什么分页?
2. list-max-listpack-size=-2 是什么意思?正数和负数有什么区别?
3. 1MB 的大字符串进 list 会发生什么?PLAIN 节点是什么?
4. list-compress-depth=1 会压缩哪些节点?为什么头尾不压?
5. 访问压缩中的元素会反复解压吗?recompress 是什么?
6. 中间插入时节点满了怎么办?
7. LINDEX 怎么穿过压缩节点?
8. 节点的 count 字段为什么是 16 位?

## 架构师视角

9. 双容器 (PLAIN/PACKED) 的大小分流设计 — 大元素为什么不进 listpack?
10. fill 双语义 (元素数 vs 字节数) — 各自的均匀性/内存特性?
11. 压缩三条件 (深度外/≥48B/收益≥8B) — 为什么需要收益阈值?
12. recompress 延迟重压 — 内存峰值 vs 压解频率的取舍?
13. 分裂 = 复制+双侧裁剪 (3×O(n)) — 为什么不用移动?代码复杂度 vs 常数?
14. 三路路由 + 相邻合并 — 稀疏节点的处理哲学?
15. 迭代器自动解压 — 压缩透明性的设计?resetIterator 的意义?
16. quicklist 演进 (linkedlist+ziplist → quicklist → listpack) — 每步解决什么?

## 学生视角

17. LPUSH 10 个元素, 内存布局长什么样 (节点/容器/元素)?
18. 8KB 节点满了, LPUSH 下一个元素会发生什么?
19. 压缩节点被访问的完整流程 (解压→读取→重压)?
20. LINDEX 从头部数到第 500 个元素, 会经过哪些节点?
