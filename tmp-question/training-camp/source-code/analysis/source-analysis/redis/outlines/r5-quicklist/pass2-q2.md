# 闭环笔记 q2: fill 双语义 — 元素数 vs 字节数

## 假设
fill (list-max-listpack-size) 双语义: 正数 = 节点元素数上限; 负数 = 字节上限 (2^k 映射到 optimization_level 表) — 默认 -2 = 8KB。

## 验证过程
- quicklistNodeLimit (quicklist.c:472-482): `fill >= 0 → count = fill (fill==0 → 1)`; `fill < 0 → size = quicklistNodeNegFillLimit(fill)`
- quicklistNodeNegFillLimit (L462-468): `offset = (-fill)-1; return optimization_level[offset]` — 负 fill 映射表
- optimization_level (L49): {4096, 8192, 16384, 32768, 65536} — **fill=-1→4KB, -2→8KB (默认), -3→16KB, -4→32KB, -5→64KB**
- 配置 (config.c:3152): `list-max-listpack-size` 默认 **-2** (旧名 list-max-ziplist-size)
- 为什么双语义: 元素数限制 (fill=128: 固定元素数节点) vs 字节限制 (fill=-2: 固定内存节点) — **按元素数: 节点分裂均匀但大元素节点内存不均; 按字节: 内存均匀但小元素节点元素多** — 负值 (默认) 是内存敏感选择
- 边界: fill==0 → count=1 (每节点 1 元素 — 退化为普通链表); 负 fill 超表 → 取最大 (65536)

## 代码类型
Interface (配置语义) + Algorithmic (映射)

## 跨域关联
- R-26 (t_list 配置) → 消费
- R-19 (listpack 容量) → 节点上限
- 配置面: MODIFIABLE_CONFIG (CONFIG SET)

## 结论
fill 双语义 = 节点上限的两种度量: 正数按元素 (均匀分裂), 负数按字节 (内存均匀, 默认 -2=8KB)。负 fill 是 Redis 内存敏感的默认选择 — "每个节点 ≤8KB, 无论元素多少"。
源码位置: quicklist.c:49,462-482; config.c:3152
