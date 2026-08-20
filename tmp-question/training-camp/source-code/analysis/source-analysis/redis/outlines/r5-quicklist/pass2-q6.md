# 闭环笔记 q6: PushHead 三路 + 插入路径

## 假设
PushHead/PushTail 三路: 大元素 → PLAIN 节点; 节点允许 → lpPrepend/lpAppend; 不允许 → 新节点 (原节点可能先合并相邻小节点)。

## 验证过程
- quicklistPushHead (quicklist.c:583-603):
  - L586-588: `isLargeElement → __quicklistInsertPlainNode` (路 1)
  - L590-596: `_quicklistNodeAllowInsert → lpPrepend(head->entry)` + UpdateSz (路 2)
  - L597-603: 不允许 → quicklistCreateNode + lpPrepend(lpNew(0)) + _quicklistInsertNodeBefore (路 3)
- _quicklistNodeAllowInsert (L521-534): 检查节点限制 (fill) + `sizeMeetsSafetyLimit` (SIZE_SAFETY_LIMIT 防单节点过大)
- _quicklistInsertNodeBefore (L541+): 插入前可能 _quicklistMergeNodes (相邻节点合并 — 删除后的稀疏节点)
- 中间插入 _quicklistInsert (L1010+): 定位迭代器 → 节点满 → 分裂 (q5) → 插入
- count 维护: quicklist->count++ + node->count++

## 代码类型
Glue (插入路由) + Algorithmic (边界处理)

## 跨域关联
- R-26 (LPUSH/LINSERT) → 消费
- q1/q2 (容器/fill) → 路由条件
- q5 (分裂) → 中间插入

## 结论
三路路由: 大元素独立 (PLAIN) / 就地追加 (允许) / 新节点 (满则建, 附带相邻合并防稀疏)。节点安全上限 (SIZE_SAFETY_LIMIT) 双保险。插入复杂度: 头尾 O(1), 中间 O(n) 定位。
源码位置: quicklist.c:521-534,541-603,1010+
