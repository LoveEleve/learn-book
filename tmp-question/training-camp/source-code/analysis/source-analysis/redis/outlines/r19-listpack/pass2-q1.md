# 闭环笔记 q1: backlen 自描述 — "无级联更新"的核心设计

## 假设
每个 entry 尾部存"自身长度" (backlen, 1-5B 固定空间) — 插入/替换变长只影响自身 backlen 与头部, 后驱不受影响 — **listpack 无级联更新** (对比 ziplist 的 prevlen 级联传播)。

## 验证过程
- entry 布局 (listpack.c:869-872): `backlen_size = lpEncodeBacklen(backlen, enclen)` — **backlen = 自身长度编码**; LP_MAX_BACKLEN_SIZE=5 (listpack.c:29) — 任何长度 (≤2^32) 的编码空间固定
- lpSkip (L452-457): `entrylen += lpEncodeBacklen(NULL,entrylen); p += entrylen` — 跳过 = 自身长度 + 自身 backlen
- lpPrev (L473-482): `p--` (前驱 backlen 末尾) → 解码前驱长度 → `p -= prevlen+backlen(prevlen)-1` 跳回前驱起点 — O(1)
- **插入分析** (lpInsert L821-968): 新 entry 写自身 backlen; 后驱的 backlen 字段存"后驱自身长度" — **不引用前驱, 插入零传播**; 只更新头部 (total_bytes/num_elements, L937-946)
- **ziplist 对照** (ziplist.c:55-69): prevlen **存前驱长度**且 1B (0-253)/5B (0xFE) 可变 — 前驱变长可能 1B→5B → 自身长度变 → 再影响下一个 → **级联传播** (ziplist.c 有 __ziplistCascadeUpdate)
- REDIS-PLAN "级联更新"表述错误 — listpack 的设计卖点就是消除级联 (antirez 2017)

## 代码类型
Algorithmic (自描述布局) — 核心设计决策

## 跨域关联
- R-25 (t_hash) / R-27→无 / R-10 (t_stream) → 消费方
- ziplist (已退役) → 对照路线 (级联 vs 无级联)
- R-3 (dict) → "紧凑数组" vs "哈希链" 两条小规模存储路线

## 结论
backlen 存**自身**长度 + 固定 5B 编码空间 = 插入/替换零级联 (只改头部计数); ziplist 的 prevlen 存**前驱**长度 + 1B/5B 可变 = 前驱膨胀级联传播。**"把长度放自己身上, 不给邻居留负担"** — listpack 对 ziplist 的根本改进。
源码位置: listpack.c:29,452-482,869-872; ziplist.c:55-69
