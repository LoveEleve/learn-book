# 闭环笔记 q1: 双容器 — PLAIN 大元素与 PACKED 小元素

## 假设
quicklist 节点有两种容器: PACKED (listpack 打包多元素) / PLAIN (单大元素裸节点) — 大元素 (超 fill 限制) 单独成节点避免 listpack 膨胀。

## 验证过程
- quicklist.h:54: `container:2` — PLAIN==1 / PACKED==2
- isLargeElement (quicklist.c:508-519): `sz > quicklistNodeNegFillLimit(fill)` — 元素超过 fill 的字节限制 → 大元素
- __quicklistInsertPlainNode (L571+): 创建 PLAIN 节点直接存裸元素 (不走 listpack)
- PushHead (L583-603): `if (unlikely(isLargeElement(sz, fill))) → __quicklistInsertPlainNode` — 大元素不进 listpack
- 为什么: listpack 是"等宽小元素"优化 — 单个大元素 (如 1MB string) 放进 listpack 会让节点 sz 爆炸, 且后续插入要整体搬移; PLAIN 节点让大元素独立 (插入/删除 O(1) 节点级)
- packed_threshold (L51-58): 测试专用 (可调 PLAIN 阈值)

## 代码类型
Algorithmic (容器策略) — 双容器设计

## 跨域关联
- R-19 (listpack) → PACKED 容器
- R-26 (t_list) → 消费域
- R-33 (zmalloc) → 分配

## 结论
双容器 = 大小分流: 小元素打包 (listpack, 空间优), 大元素裸存 (PLAIN, 免 listpack 膨胀/搬移)。isLargeElement 判定 = 超 fill 字节限制。这是"快表"对极端负载的防护。
源码位置: quicklist.c:508-519,571-603; quicklist.h:54
