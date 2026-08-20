# 闭环笔记 q3: dictFind 的 rehash 联动 — 缓存友好的桶迁移

## 假设
查找操作顺带推进 rehash: 若目标桶在游标后 (尚未迁移) 且非空, 直接迁移该桶 (缓存友好 — 正在访问的桶); 否则按游标步进 1 步。

## 验证过程
- dictFind (dict.c:736-770):
  - L743-744: 计算 h, idx (ht[0] 掩码)
  - L747-757: `if (dictIsRehashing(d))`: `idx >= rehashidx && ht_table[0][idx]` → `_dictBucketRehash(d, idx)` — **目标桶未迁移且非空: 直接迁移它**; 否则 `_dictRehashStep(d)` — 游标步进 1 桶
  - L758-761 双表查找: `table==0 && idx < rehashidx → continue` (已迁移的桶跳过 ht[0]) — 表 1 用新掩码
- _dictBucketRehash (L472-489): 单桶迁移 + 完成检测 (AVOID 阈值检查同 dictRehash)
- 注释 (L749-754): "**being more CPU cache friendly**" — 马上要读的桶先迁过来, 后续查找命中同一区域; 否则"not CPU cache friendly"的游标步进
- 语义: 查找本身完成迁移的一部分工作 — 高频访问的桶自动先迁移

## 代码类型
Algorithmic (访问模式感知的迁移调度)

## 跨域关联
- R-21 (键空间查找 lookupKey → dictFind) → 每次 GET 都可能推进 rehash
- q1 (三触发面) → 本机制是"操作联动"面的核心

## 结论
查找即迁移: 目标桶未迁 → 优先迁移它 (下一条命令大概率访问同一桶, 缓存命中); 否则游标步进。查找代价 O(1)+偶发迁移, 迁移工作被访问模式自适应调度 — 热门桶先迁, 冷桶由 serverCron 兜底。
源码位置: dict.c:736-770,472-489
