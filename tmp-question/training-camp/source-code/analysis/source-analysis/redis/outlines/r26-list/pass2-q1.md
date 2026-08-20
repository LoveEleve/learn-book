# 闭环笔记 q1: list 双编码与转换

## 假设
list 双编码: listpack (小) / quicklist (大); 转换双向 (GROWING listpack→quicklist, SHRINKING quicklist 缩容)。

## 验证过程
- list 编码 (t_list.c:110-127): listTypeTryConversionRaw 分派 — QUICKLIST: GROWING 免 (L116-117) / SHRINKING → listTypeTryConvertQuicklist (L65); LISTPACK: SHRINKING 免 / GROWING → listTypeTryConvertListpack (L21)
- 阈值 (config.c:3152): list-max-listpack-size 默认 -2 (8KB quicklist 节点, R-5 已交付); 旧名 list-max-ziplist-size
- listTypeTryConvertListpack (L21-64): 新增字段/值超限 → quicklist 转换 (L50+)
- listTypeTryConvertQuicklist (L65-109): 缩容场景 (删除后节点变小 → 回 listpack?) — 验证: shrinking 参数 + beforeConvertCB (lazyfree 前转换)
- 创建: createListListpackObject (pushGenericCommand L477) — 从 listpack 起步
- 转换方向: listpack → quicklist 单向 (GROWING); quicklist 内部节点优化 (SHRINKING 是节点合并)

## 代码类型
Mechanism (编码选择)

## 跨域关联
- R-5 (quicklist 全部机制) / R-19 (listpack)

## 结论
list 编码 = listpack (小) → quicklist (大, 节点 8KB) 单向增长; quicklist 缩容有回调钩子 (beforeConvertCB, lazyfree 场景)。与 hash 的"单向升级"同哲学 (R-25)。
源码位置: t_list.c:21-127; config.c:3152
