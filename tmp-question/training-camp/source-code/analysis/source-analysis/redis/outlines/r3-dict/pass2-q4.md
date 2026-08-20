# 闭环笔记 q4: 2 幂表掩码 — 缩容为什么不用重算哈希

## 假设
表大小恒为 2 的幂, 桶索引 = hash & (size-1)。扩容时低位掩码变大 (重算); **缩容时新表掩码是旧掩码的子集, 桶内元素在新表中的索引 = idx & 新掩码 — 无需重算哈希**。

## 验证过程
- rehashEntriesInBucketAtIndex (dict.c:320-327): `if (ht_size_exp[1] > ht_size_exp[0])` → 扩容: `h = dictHashKey(d, key, 1) & DICTHT_SIZE_MASK(ht_size_exp[1])` (重算); else → **缩容: `h = idx & DICTHT_SIZE_MASK(ht_size_exp[1])`** — 注释 L322-325: "We're shrinking the table. The tables sizes are powers of two, so we simply mask the bucket index in the larger table to get the bucket index in the smaller table"
- 数学依据: 表 0 大小 2^k, 表 1 大小 2^j (j<k)。原索引 idx = h & (2^k-1) (低 k 位)。新索引 = h & (2^j-1) = idx & (2^j-1) — **低 j 位相同, 直接掩码**
- 收益: 缩容迁移 O(1)/元素 (纯位运算) 且**不用调哈希函数** (siphash 计算省掉); 扩容仍需重算 (h & 大掩码)
- 联动: 这也解释为什么表大小恒为 2 的幂 — 掩码运算 (与) 替代取模 (%), 且使缩容掩码复用成立

## 代码类型
Algorithmic (2 幂掩码优化) — 数学性质利用

## 跨域关联
- R-4 (sds 键哈希) → 哈希函数调用面
- q1 (迁移) → 迁移成本的核心差异

## 结论
2 的幂表 + 掩码寻址: 扩容重算哈希 (新掩码), **缩容直接 idx & 新掩码** (低 j 位不变) — 迁移 O(1)/元素且免哈希计算。这是"表大小恒为 2 幂"设计的数学回报, 也解释了掩码寻址替代取模。
源码位置: dict.c:320-327
