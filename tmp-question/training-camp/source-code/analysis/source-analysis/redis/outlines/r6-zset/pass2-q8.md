# 闭环笔记 q8: zsetConvert — listpack→skiplist 转换与预扩

## 假设
转换从 listpack 逐元素读 (ele, score) 双写进 dict + skiplist; **dictExpand 预扩**避免转换过程触发 rehash; 转换单向。

## 验证过程
- zsetConvertAndExpand (t_zset.c:1270-1300+):
  - L1277-1281: 创建 zs (dict + zslCreate)
  - L1292: `dictExpand(zs->dict, cap)` — **预扩** (cap=现有元素数): 转换中逐 dictAdd 不触发渐进 rehash (一次扩容到位, 转换 O(n) 无迁移开销)
  - L1287-1290: `eptr = lpSeek(zl, 0)` 定位首元素 → sptr = lpNext (ele/score 成对)
  - while 循环: 读 ele (lpGetValue) + score (lpGetIntegerValue/strtod) → zslInsert + dictAdd (双写)
- 触发面: zsetAdd 超阈值 (q7 L1480) / 显式转换 (ZADD 前检查); 反向 (skiplist→listpack) 不存在 — 单向 (与 intset/listpack 同哲学)
- 转换后: zobj->encoding = SKIPLIST, 旧 listpack 释放
- zsetConvert 入口 (L1265): 简单包装 (cap=zsetLength)

## 代码类型
Glue (编码转换) — 数据迁移

## 跨域关联
- R-19 (listpack 读取) / R-3 (dict) → 源/目标
- q7 (触发面) → 阈值转换
- R-33 (zmalloc) → 分配

## 结论
转换 = 预扩 dict + 逐元素双写: dictExpand(cap) 让转换过程零 rehash; 单向 (与全部编码转换哲学一致)。转换后 listpack 释放, zset 进入"大集合模式"。
源码位置: t_zset.c:1265-1300
