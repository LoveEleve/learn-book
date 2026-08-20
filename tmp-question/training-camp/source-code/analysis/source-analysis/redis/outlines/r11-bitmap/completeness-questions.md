# R-11 位操作 — completeness-questions

## 开发者视角

1. redisPopcount 的 SWAR 三阶段是什么?
2. redisBitpos 怎么加速稀疏位图?
3. BITFIELD 的 WRAP/SAT/FAIL 差在哪?
4. SETBIT 第 0 位是字节的哪一位?
5. BITOP 键长度不一致怎么处理?
6. BITCOUNT 的 BIT/BYTE 单位?
7. BITFIELD 的 64 位特判?
8. SETBIT 的 dirty 三条件?

## 架构师视角

9. 查表 + SWAR 的组合为什么高效?
10. bitpos 字对齐跳过的复杂度 (O(段数))?
11. 溢出三模式的设计意图 (WRAP 截断/SAT 钳制)?
12. MSB 位序统一约定的意义 (多命令一致)?
13. BITOP maxlen 零填充语义?
14. BITCOUNT 掩码方案 vs 子串拷贝?
15. 为什么 BITFIELD 上限 64 位?
16. SETBIT 返回旧值的读改写合一?

## 学生视角

17. BITCOUNT 一个字节 0b10110010 有多少个 1?
18. BITPOS k 1 找第一个 1 的过程 (0xFF00FF)?
19. BITFIELD SET u5 7 23 的位分布?
20. SETBIT k 1000000 1 会发生什么 (扩容)?
21. BITOP 的字宽批量运算 (L672+)?
