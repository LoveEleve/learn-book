# 闭环笔记 q5: 节点分裂 — 复制 + lpDeleteRange 双侧裁剪

## 假设
中间插入 (或列表过长) 时节点分裂: 复制整个 listpack 到新节点, 然后用 lpDeleteRange 在两侧各删一半 — 复用 listpack 删除能力而非手动移动。

## 验证过程
- _quicklistSplitNode (quicklist.c:971-1004):
  - L975-978: 新节点 + `zmalloc(zl_sz)` + `memcpy` — **整包复制**
  - L981-983: 负 offset 转正 (从尾数)
  - L985-990: 裁剪范围计算: `orig_start = after ? offset+1 : 0; orig_extent = after ? -1 : offset` 等 — **-1 extent = 删到结尾**
  - L993-995: `node->entry = lpDeleteRange(node->entry, orig_start, orig_extent)` — 原节点删一半
  - L996-998: `new_node->entry = lpDeleteRange(new_node->entry, new_start, new_extent)` — 新节点删另一半
- 为什么复制+裁剪: listpack 的删除是"memmove 覆盖" (O(n)) — 复制 O(n) + 两次删除 O(n) = 3×O(n); 但实现极简 (复用 lpDeleteRange 的语义: 支持 -1 到结尾) — **代码量 vs 常数倍的取舍**
- 触发: _quicklistInsert 中间插入时节点已满 (fill 限制) → 分裂; LINSERT/中间 LPUSH

## 代码类型
Algorithmic (分裂策略) — 工程取舍

## 跨域关联
- R-19 (lpDeleteRange) → 分裂基础
- R-26 (LINSERT) → 触发面
- q2 (fill) → 分裂判定

## 结论
分裂 = 复制整包 + 双侧 lpDeleteRange: 常数 3×O(n) 换实现极简 (复用 listpack 的范围删除语义, 含 -1 到结尾)。offset 正负统一 (负 = 从尾数)。
源码位置: quicklist.c:971-1004
