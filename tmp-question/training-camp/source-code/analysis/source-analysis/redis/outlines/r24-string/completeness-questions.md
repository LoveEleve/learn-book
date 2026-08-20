# R-24 字符串命令 — completeness-questions

## 开发者视角

1. SET 的 9 个标志位怎么互斥?
2. SET key v GET 的旧值怎么返回?传播时怎么处理?
3. INCR 原地更新的四个条件是什么?
4. INCRBYFLOAT 为什么重写为 SET KEEPTTL?
5. SETRANGE 越界怎么写?用什么函数?
6. GETRANGE 的负索引怎么换算?
7. GETEX 的 TTL 三路径?
8. LCS 的三层内存防护?

## 架构师视角

9. 解析期互斥 vs 执行期检查 — 为什么选前者?
10. SET 传播归一 (PXAT) 与 R-22 PEXPIREAT 归一的关系?
11. INCR 原地更新为什么要求非共享范围 (0-9999 之外)?
12. INCRBYFLOAT 重写 SET 解决了什么 (浮点精度传播)?
13. checkStringLength 的加法溢出检测 (uint64) 防什么?
14. GETEX "先校验 expire 再回复" 的顺序为什么重要?
15. LCS 的 ztrymalloc 降级 vs 崩溃 — 取舍?

## 学生视角

16. SET key 100 与 SET key "abc" 内存差异 (编码)?
17. INCR key 的完整执行路径 (从解析到传播)?
18. SETRANGE k 5 "abc" 的结果字符串什么样?
19. GETEX k PX 100 传播成什么?
20. GETRANGE k -3 -1 的索引计算过程?
