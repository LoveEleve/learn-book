# R-27 t_set — Pass 1 探索笔记

> 域: R-27 t_set (t_set.c) | 🟡 B 方案 | 2026-08-13
> 源码: src/t_set.c (1658) | Redis 7.4.2

## 调用图

```
写入:
saddCommand (L583) → lookupKeyWrite → setTypeCreate (L25: 三编码选择)
  → setTypeMaybeConvert (L40: size_hint 预判) → setTypeAdd (L94) → setTypeAddAux (L104)
    → INTSET: intsetAdd + maybeConvertIntset (L110)
    → HT: dictFindPositionForInsert + dictInsertAtPosition (L124-128, 预定位免双查)
    → LISTPACK: lpFind/lpAppend + 超限转 HT (L155)
    → **intset 非整数元素 → listpack 或 HT** (L169-202)
sremCommand (L608) → setTypeRemove (L212)

集合运算:
sinterGenericCommand (L1255): 空集短路 (empty>0) → 最小集遍历+成员检查
  → **结果全整数 → maybeConvertToIntset 转回** (L1392) ← 双向转换实证!
sunionDiffGenericCommand (L1460): 并集/差集 — 迭代加入 + STORE 版本
sinterCardCommand (L1424): 只算基数

随机:
spopWithCountCommand (L739): COUNT 正负 + 大 set 采样 (dictGetFairRandomKey)
srandmemberWithCountCommand (L998): 同上
setTypeRandomElement (L407) / setTypePopRandom (L430)

命令面:
smoveCommand (L636) / sismember (L691) / smismember (L703) / scard (L722)
sscanCommand (L1650): scanGenericCommand 委托
```

## 基本元素分解

1. **三编码**: INTSET (整数+≤512) / LISTPACK (≤128/64B) / HT (大/混合) — set-max-listpack-entries=128 与 hash 512 不同!
2. **双向转换**: intset→listpack/HT (非整数/超限) + **HT→intset (sinterstore 全整数, L1392)** — 与 hash 单向不同!
3. **setTypeAddAux**: 三编码分支 + dict 预定位插入 (FindPositionForInsert)
4. **集合运算**: sinter (空集短路+最小集遍历) / sunion/sdiff (迭代) + STORE 变体
5. **随机**: SPOP/SRANDMEMBER COUNT 语义 + FAIR 随机
6. **intsetMaxEntries**: 1<<30 上限

## 标记问题 (8 个)

1. set 三编码怎么选 (创建/写入)?
2. intset→listpack→HT 的转换链?
3. HT→intset 降级的时机 (L1392)?
4. set-max-listpack-entries=128 为什么与 hash 512 不同?
5. dict 预定位插入优化 (FindPositionForInsert)?
6. sinter 空集短路 + 最小集遍历策略?
7. SPOP COUNT 正负语义?
8. intset 1<<30 上限的依据?

## 时空溯源 (代码内痕迹)

- 2009 (antirez): t_set.c 初版 — SADD/SREM 家族 (版权 2009-Present); intset+HT 双编码
- 2.8+: SPOP/SRANDMEMBER COUNT; 集合运算优化 (sinter 最小集)
- 7.x: **listpack 编码加入** (set-max-listpack-entries=128); **HT→intset 降级** (maybeConvertToIntset, sinterstore 全整数场景); dict 预定位插入
- 演进: 从双编码 → 三编码; 从单向升级 → 双向转换 (set 特有!)

## 大域拆分判断

1658 行单文件 — **不拆** (🟡 B, 6 闭环足够)。
