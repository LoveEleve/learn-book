# 闭环笔记 q2: 层级概率 — P=0.25 与 MAXLEVEL 32

## 假设
zslRandomLevel 用 P=0.25 几何分布: 每层 25% 概率升层 — 层级期望 ≈ 1/(1-P) ≈ 1.33; MAXLEVEL 32 覆盖 2^64 元素的理论上限。

## 验证过程
- zslRandomLevel (t_zset.c:126-132): `threshold = ZSKIPLIST_P*RAND_MAX; while (random() < threshold) level += 1;` — **P=0.25 的几何分布** (每次 25% 升层)
- server.h:514-515: `ZSKIPLIST_MAXLEVEL 32` / `ZSKIPLIST_P 0.25`
- 分布性质: P(level=k) = (1-P)·P^(k-1); 期望层数 = 1/(1-P) = 1.33; 平均每节点 level[] 数组 ~1.33 项 (含 level 0)
- 层高与元素数: 2^64 元素时最高层 = log_{1/P}(2^64) = log_4(2^64) = 32 — **MAXLEVEL 32 = 理论上限** (注释 "Should be enough for 2^64 elements")
- 为什么 1/4 而非 1/2: P=1/2 → 每节点 ~2 层, 指针翻倍; P=1/4 → ~1.33 层 — **内存更省, 查找常数略大** (O(log n) 不变, 常数因子); 与经典跳表论文 (P=1/2) 的取舍: Redis 面向内存, 省指针优先
- 查找复杂度: O(log_{1/P} n) — P=1/4 时 log_4 n (常数略大但内存省一半指针)

## 代码类型
Algorithmic (概率结构) — 参数选择

## 跨域关联
- q5 (zslInsert 层级分配) → 随机层数的消费
- R-33 (zmalloc) → 每节点按 level 分配

## 结论
P=0.25 几何分布: 期望 1.33 层/节点 (省指针), MAXLEVEL=32 覆盖 2^64 理论元素。与经典 P=0.5 的取舍: Redis 内存敏感, 用查找常数换指针空间 — "省一半指针, 多走几步"。
源码位置: t_zset.c:126-132; server.h:514-515
