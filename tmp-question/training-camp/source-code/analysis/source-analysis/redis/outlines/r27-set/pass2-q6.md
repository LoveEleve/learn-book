# 闭环笔记 q6: 编码演进 — 三编码与双向转换的取舍

## 假设
set 编码演进 = intset+HT 双编码 → +listpack 三编码; 双向转换 (HT→intset) 是 set 特有设计。

## 验证过程
- **演进对比**:
  - hash (R-25): listpack → HT 单向 (无降级)
  - list (R-26): listpack → quicklist 单向
  - **set: intset ↔ listpack ↔ HT 多向** — maybeConvertToIntset (L66-88) 明确支持 HT/listpack → intset
- 降级触发 (L1392): sinterstore 结果全整数且 ≤512 → 转回 intset — **节省内存** (intset 4/8B/元素 vs dict 指针)
- 非整数触发 (L181-194): intset → listpack (≤128/64B) 而非直接 HT — **中间态缓存** (listpack 比 HT 省)
- 规模触发: 超限 → HT (最终形态)
- 转换成本: 全量重建 (迭代+插入), 但仅在"结果集"场景 (sinterstore) 或"单元素越界"场景 — 低频
- intset 上限 1<<30 (L52): 防 intset 内部索引溢出

## 代码类型
Mechanism (编码取舍)

## 跨域关联
- R-7 (intset 升而不降 — 对照: set 反而支持降!) / R-25 (hash 单向) / R-26 (list 单向)

## 结论
set 是 Redis 中**唯一支持编码降级**的容器: 全整数结果可回 intset。动机 = intset 极致内存密度 (推断: 注释无明说, 但 maybeConvertToIntset 的存在即设计意图)。listpack 中间态 (intset 遇非整数先试 listpack) 也是 set 特有 — 渐进转换而非一步到位。
源码位置: t_set.c:66-88,169-202,1392
