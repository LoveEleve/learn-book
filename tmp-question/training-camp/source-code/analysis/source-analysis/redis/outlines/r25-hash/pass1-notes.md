# R-25 t_hash — Pass 1 探索笔记

> 域: R-25 t_hash (t_hash.c) | 🔴 A 方案 | 2026-08-13
> 源码: src/t_hash.c (3418) | Redis 7.4.2

## 调用图

```
写路径:
hsetCommand (L2158) → hashTypeLookupWriteOrCreate (L1541: createHashObject + dbAdd)
  → hashTypeTryConversion (L594: 阈值预判转换) → hashTypeSet (L855)
    → LISTPACK: lpFind/lpReplace/lpAppend → 超 512 转 HT (L893-894)
    → LISTPACK_EX: 三元素组 (field/value/expire) → listpackExAddNew (L928)
    → HT: hfieldNew + dictAddRaw + dictSetVal (L936-967)
  → notify "hset" + dirty

读路径:
hgetCommand (L2312) → hashTypeGetValue (L711): 惰性过期链 (L737-778)
  → GETF_OK/EXPIRED/EXPIRED_HASH (空 hash 删键 L770-775)
hmgetCommand (L2321) → 批量
genericHgetallCommand (L2445) → 迭代器 (skipExpiredFields 优化 L2462-2464)

转换:
hashTypeTryConversion (L594): 字段数 > hash_max_listpack_entries → 预转换+dictExpand (L605-609)
  / 单值 > hash_max_listpack_value → 转换 (L615-617) / lpSafeToAdd 总量 (L621)
hashTypeConvert (L1669) → Listpack→HT (L1553: dictExpand 预扩 + 迭代 + dictAdd)
  → ListpackEX→HT (L1611: ebRemove 全局 + dictExpireMetadata 迁移 + ebAdd 全局 L1661-1662)

HFE:
hashTypeSetEx (L1068) → SetExpiryListpack / SetExpiryHT (L979: GT/LT/NX/XX 条件 + ebRemove/ebAdd 私有 hfe)
hashTypeGetValue 惰性 (L737-778): 过期判定 → 从库只报 → 主库删字段+propagate+hexpired 通知+空 hash 删键
hashTypeDbActiveExpire (L2073) ← R-22 activeExpireHashFieldCycle (ebExpire 全局表)
hashTypeAddToExpires (L2040) / hashTypeRemoveFromExpires (L1996) — 全局注册 (R-21 消费)

命令面:
hrandfield (L2547): 加权随机 + withvalues
hscanCommand (L2509): SCAN 泛化
hexpire 族 (L3124-3289): hexpire/httl/hpersist — 字段级 TTL 命令
```

## 基本元素分解

1. **编码三态**: LISTPACK (field/value 两元素组) / LISTPACK_EX (三元素组 + TTL) / HT (hfield 键)
2. **hfield 结构**: mstr (嵌入式元数据) + ExpireMeta (可选)
3. **hashTypeSet**: 三编码分支 + KEEP_TTL/TAKE_VALUE 标志
4. **转换**: 触发 (阈值/lpSafeToAdd) + 实现 (迭代迁移 + dictExpand 预扩) + HFE 元数据迁移
5. **惰性过期**: GETF 三态 (OK/EXPIRED/EXPIRED_HASH) + 传播 + 通知
6. **条件 TTL**: hashTypeSetExpiryHT (GT/LT/NX/XX + checkAlreadyExpired 删字段)
7. **全局注册**: AddToExpires/RemoveFromExpires/trash 标记 (转换期)
8. **命令面**: HSET/HINCRBY/HGETALL/HRANDFIELD/HSCAN

## 标记问题 (9 个)

1. 编码三态怎么选 (创建/阈值)?
2. hashTypeSet 三分支差异 (lpReplace vs hfieldNew)?
3. TryConversion 的三个触发条件?
4. 转换怎么保持 HFE 元数据 (trash/迁移)?
5. GETF 三态惰性链 (从库/主库/空 hash)?
6. SetExpiryHT 的条件矩阵 (GT/LT/NX/XX + 无 TTL 语义)?
7. hexpire 命令族与键级 EXPIRE 的差异?
8. HRANDFIELD 的加权随机?
9. HGETALL 的 skipExpiredFields 优化?

## 时空溯源 (代码内痕迹)

- 2009 (antirez): t_hash.c 初版 — HSET/HGET 家族 (版权 2009-Present)
- 2.6: HT 编码 + listpack (ziplist) 阈值转换
- 7.0: **ziplist → listpack** (R-19); storedKey API (R-3) — hfield 直接作 dict 键
- 7.4 (2024): **HFE 字段级过期** — LISTPACK_EX 三元素组 + mstrHashDictTypeWithHFE + ebuckets 私有 hfe + hexpire 命令族; hashTypeGetValue 惰性链 (GETF_EXPIRED_HASH)

## 大域拆分规划 (01 §大域)

8 闭环 → **2 篇**:
- 篇 1 (命令面与编码): q1 (编码三态) + q2 (hashTypeSet) + q3 (转换) + q4 (命令面)
- 篇 2 (HFE 字段过期): q5 (惰性链) + q6 (条件 TTL) + q7 (命令族) + q8 (全局注册与迁移)
