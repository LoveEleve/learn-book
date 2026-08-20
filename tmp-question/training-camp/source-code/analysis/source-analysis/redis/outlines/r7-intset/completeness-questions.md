# R-7 intset — completeness-questions

## 开发者视角

1. intset 的 encoding 字段存的是什么?为什么用字节宽直接当编码?
2. 什么值触发升级?升级时新值一定在哪个位置?
3. 升级为什么要从后往前搬元素?
4. SISMEMBER 查找是 O(log n) 吗?有什么快速路径?
5. 删除大量元素后, 编码会变小吗?为什么?
6. intset 存小端还是大端?为什么用 memcpy 而不是直接解引用?
7. set 什么时候用 intset?什么时候用 dict?
8. intsetValidateIntegrity 的 deep 模式查什么?

## 架构师视角

9. 升级的"头尾特权" (prepend) — 为什么新值必在极值?数学依据?
10. 从后往前搬 vs 从前往后 — 为什么前者安全?反例?
11. "升而不降"的取舍 — 降级的真实成本?什么场景会后悔?
12. 统一小端存储 — RDB 可移植性的设计?memcpy 免对齐?
13. 512 阈值 (set-max-intset-entries) 的依据?CONFIG SET 动态调整的影响?
14. intset vs listpack — 同为紧凑编码, 各自适用什么?为什么并存?
15. 单向转换哲学 (intset→dict/listpack→dict) — 与降级的对称性思考?

## 学生视角

16. SADD 1,2,3 → intset 的内存布局长什么样?
17. 升级 16→32: 具体搬移步骤?
18. 为什么全 int16 的 100 万整数集只要 2MB?
19. SREM 后内存为什么不小?怎么恢复?
20. intset 查找和 listpack 查找的复杂度差异?
