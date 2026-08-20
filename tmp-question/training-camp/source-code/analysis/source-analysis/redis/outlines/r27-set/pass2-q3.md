# 闭环笔记 q3: sinterGenericCommand — 空集短路与最小集遍历

## 假设
sinter = 空集短路 (交集必空) + 最小集遍历+成员检查; 结果全整数降回 intset。

## 验证过程
- sinterGenericCommand (t_set.c:1255-1417):
  - **空集短路** (L1275-1300): 任一 NULL/空 → 结果空 (删 dstkey 版本 / 空集回复); 提前返回
  - **最小集选择** (L1300+): 按基数排序找最小 set (setTypeSize 排序, qsortCompareSetsByCardinality L1229) — 遍历成本最小化
  - 遍历最小集 (L1320+): setTypeNext + 其他集 setTypeIsMember 全查 → 收集交集
  - **STORE 版本** (L1370-1400): 结果写入 dstset — **全整数 → maybeConvertToIntset 降级** (L1392)
  - cardinality_only (L1400+, SINTERCARD): 只计数 + LIMIT 提前退出
- sinterCommand (L1419) / sinterCardCommand (L1424) / sinterstoreCommand (L1456)
- sunionDiffGenericCommand (L1460-1630): 并集 (全部加入) / 差集 (首个集减其余) + STORE 变体

## 代码类型
Mechanism (集合运算)

## 跨域关联
- R-7 (intset 迭代) / R-3 (dict 成员检查)

## 结论
sinter = **空集短路 + 最小集策略** (计算复杂度 O(min×others)); 结果降级转 intset (双向转换闭环)。sunion/sdiff 迭代实现 + STORE 变体同构。
源码位置: t_set.c:1229-1460
