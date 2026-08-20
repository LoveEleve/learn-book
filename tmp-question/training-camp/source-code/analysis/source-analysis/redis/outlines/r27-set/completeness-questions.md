# R-27 集合命令 — completeness-questions

## 开发者视角

1. set 的三种编码什么时候用?
2. intset 加非整数元素会发生什么?
3. 什么场景会转回 intset (降级)?
4. SINTER 空集怎么短路?
5. SPOP COUNT 正负语义?
6. SMOVE src==dst 返回什么?
7. SINTERCARD 和 SINTER 差在哪?
8. set-max-listpack-entries 为什么是 128?

## 架构师视角

9. 三编码渐进转换链 (intset→listpack→HT) 的设计动机?
10. 为什么 set 是唯一支持降级的容器 (对比 hash/list)?
11. dictFindPositionForInsert 预定位插入的优化?
12. sinter 最小集策略的复杂度分析 (O(min×others))?
13. SPOP 大集合的采样/全排双策略 (count vs size/10)?
14. intset 1<<30 上限的依据?
15. "空集不存在"不变量与 R-26 的同源性?
16. sinterstore 转 intset 的触发条件 (L1392)?

## 学生视角

17. SADD 1 2 3 的内存分布 (intset 4B/元素)?
18. SADD "a" 到 intset 的转换路径?
19. SINTERSTORE 结果的编码选择过程?
20. SPOP key 1000000 和 SPOP key 10 的实现差异?
21. SMOVE 的完整执行链?
