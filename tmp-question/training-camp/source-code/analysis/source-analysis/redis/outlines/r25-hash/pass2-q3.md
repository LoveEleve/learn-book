# 闭环笔记 q3: 编码转换 — 触发与迁移

## 假设
转换三触发 (字段数/单值/总量); 实现 = 迭代迁移 + dictExpand 预扩; HFE 元数据随迁。

## 验证过程
- hashTypeTryConversion (t_hash.c:594-623):
  - **三触发**:
    1. 批量字段数: `new_fields > hash_max_listpack_entries` (L605-609) → 转换 + **dictExpand 预扩** (L607, 免 rehash — R-6 zset 同技巧)
    2. 单值超长: `len > hash_max_listpack_value` (L615-617) → 转换
    3. **总量**: `!lpSafeToAdd(lp, sum)` (L621-622, R-19 1GB 安全线) → 转换
  - 仅 listpack/listpackEX 编码检查 (L598-599)
- hashTypeConvertListpack (L1553-1609): LISTPACK→HT: dictCreate(mstrHashDictType) + **dictExpand 预扩** (L1585) + 迭代 hfieldNew/dictAdd (L1587-1601); 重复字段 → panic (L1594-1600, 腐坏防御)
  - LISTPACK→LISTPACK_EX (L1559-1575): 每 field/value 后 **lpInsertInteger HASH_LP_NO_TTL** (L1568) — 两元素组扩为三元素组
- hashTypeConvertListpackEx (L1611-1666): LISTPACK_EX→HT:
  - **全局摘除** (L1624-1625): `ebRemove(hexpires, hashExpireBucketsType, o)` — 转换期从 db->hexpires 摘
  - dictCreate(mstrHashDictTypeWithHFE) + dictExpireMetadata 填充 (L1627-1634: key 引用 + hfe=ebCreate + **trash=1 标记**)
  - 迭代迁移 (L1638-1654): hfieldNew + 有 TTL → **ebAdd 私有 hfe** (L1652-1653)
  - **全局重注册** (L1661-1662): minExpire 有效 → ebAdd(hexpires)
- hashTypeConvert (L1669-1679): 分发 + HT→X panic (L1675, 无降级)

## 代码类型
Mechanism (编码迁移)

## 跨域关联
- R-19 (lpSafeToAdd) / R-6 (dictExpand 预扩同技巧) / R-21 (hexpires) / R-3 (storedKey)

## 结论
转换 = 单向升级 + 预扩免 rehash + HFE 元数据三阶段迁移 (全局摘 → 私有建 → 全局重注)。trash 标记处理转换期"未注册"状态 (R-21 的 ebuckets trash 位)。
源码位置: t_hash.c:594-623,1553-1679
