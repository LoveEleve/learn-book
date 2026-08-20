# R-6 skiplist+ZSet — Pass 1 探索笔记

> 域: R-6 skiplist+ZSet (有序集合) | 🔴 方案 A | 2026-08-13
> 源码: src/t_zset.c (4513) + server.h zskiplist 定义 (1341-1360) | Redis 7.4.2
> 已读测试: 无单元测试 (zset 靠集成测试: tests/integration/convert-ziplist-zset-on-load.tcl)

## 继承树/调用图

```
zskiplistNode (server.h:1341-1349):
  sds ele + double score + backward (后退指针)
  level[]: forward + span (跨越节点数)  ← 柔性数组

zskiplist (server.h:1351-1355): header + tail + length + level (当前最高层)

zset (server.h:1357-1360): dict* (member→score) + zsl* (有序)  ← 双结构

核心操作:
  zslCreateNode/zslCreate (L75-91): header 节点 ZSKIPLIST_MAXLEVEL 层
  zslRandomLevel (L126-132): threshold = P*RAND_MAX (0.25) — 25% 概率升层
  zslInsert (L137-192): update[]+rank[] 双数组 → 同分按 ele 字典序 → span 更新 (rank 差)
  zslDeleteNode (L196-214): span 调整 + level 收缩 (顶层空)
  zslDelete (L224+): 同分按 ele 定位
  zslGetRank (L508-528): span 累计 → O(log n) 排名
  zslGetElementByRank (L530+): 排名 → 元素
  zslIsInRange (L317+): 首尾检查 (tail.max ≥ min / first ≤ max) — 范围空判定
  zslNthInRange (L336+): 范围内第 N 个 (n=0 首 / -1 末 — **7.x 统一替代 FirstInRange/LastInRange**; ZRANGE BYSCORE offset L3345 / ZCOUNT L3443-3451) + ZSKIPLIST_MAX_SEARCH 小偏移逐节点优化 (L370)
  命令面:
  zsetAdd (L1425+): 双编码路径 (listpack: zzlFind/zzlInsert; skiplist: zslInsert+dictAdd)
    + 选项 NX/XX/GT/LT/INCR + NaN 校验
  zsetConvertAndExpand (L1270+): listpack→skiplist: dictExpand 预扩 → 遍历 zslInsert+dictAdd
  zsetDel (L1530+?): 双结构同步删除
  排名命令: ZRANK/ZREVRANK (zsetRank), ZRANGE (zslNthInRange), ZSCORE (dict 查)
```

## 基本元素分解

1. **双结构**: dict (成员→score, O(1) 存在性) + skiplist (score 有序, O(log n) 范围) — 互补
2. **层级概率**: P=0.25 几何分布 — 期望 1/(1-P)≈1.33 层
3. **span 跨度**: 每 level 存"跳过节点数" — 排名 O(log n)
4. **同分字典序**: (score, ele) 复合排序 — 确定性
5. **update/rank 双数组**: 插入路径记录 + span 维护
6. **范围查询**: IsInRange 首尾检查 + First/Last 边界定位
7. **双编码**: listpack (≤128/64B) ↔ skiplist+dict 转换
8. **命令面**: ZADD 选项族 (NX/XX/GT/LT/INCR)

## 标记问题 (9 个)

1. 为什么双结构 (dict+skiplist)? 单一结构不行吗? 一致性怎么维护?
2. P=0.25 层级概率 — 为什么 1/4? 层级分布期望? 内存/时间权衡?
3. span 跨度 + rank — 排名 O(log n) 的原理? 插入时 span 怎么维护?
4. 同分排序 (ele 字典序) — 为什么需要确定性? 对 ZRANGE 的影响?
5. zslInsert 的 update/rank 双数组算法 — span 更新公式 (rank[0]-rank[i]) 的含义?
6. 范围查询 (IsInRange/First/Last) — 为什么 O(log n)? 空范围判定?
7. zsetAdd 双编码路径 + NX/XX/GT/LT/INCR — 选项组合的语义面?
8. zsetConvert (listpack→skiplist) — dictExpand 预扩的意义? 转换触发?
9. 删除后的 level 收缩 — 为什么不立即收缩? (惰性)

## 时空溯源 (代码内痕迹)

- zskiplistNode 定义含 span (早期版本无 span — 排名要 O(n); span 引入后 ZRANK O(log n))
- ZSKIPLIST_P=0.25 / MAXLEVEL=32 (server.h:514-515): 32 层上限 = 2^64 元素的理论需要 (1/0.25^32)
- zset 双结构自初版 (2010s) 定型; 小规模编码从 ziplist → listpack (7.x)
- zslNthInRange/zslGetElementByRankFromNode: 排名优化 (7.x 重构, ZRANGE 提速)
