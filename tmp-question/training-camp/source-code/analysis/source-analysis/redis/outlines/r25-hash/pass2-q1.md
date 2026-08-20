# 闭环笔记 q1: 编码三态 — 创建与选择

## 假设
hash 三编码: LISTPACK (两元素组) / LISTPACK_EX (三元素组+TTL) / HT (hfield 键); 创建从 listpack 起步。

## 验证过程
- createHashObject (object.c:249): 空 listpack (OBJ_ENCODING_LISTPACK)
- 编码常量: OBJ_ENCODING_LISTPACK / LISTPACK_EX / HT — hash 专用 LISTPACK_EX (7.4 HFE)
- 阈值 (config.c:3215,3221): hash-max-listpack-entries 默认 512 / hash-max-listpack-value 默认 64 (旧名 ziplist 兼容别名)
- hashTypeLookupWriteOrCreate (t_hash.c:1541-1550): lookupKeyWrite + checkType + 不存在 → createHashObject + dbAdd
- 编码升级单向: listpack → HT / listpack → LISTPACK_EX (字段带 TTL 时) — 无降级
- mstrHashDictType (L74-84): hfield 作键 (dictMstrHash/dictHfieldKeyCompare/dictHfieldDestructor)
- mstrHashDictTypeWithHFE (L86-113): + dictMetadataBytes (hashDictWithExpireMetadataBytes) + onRelease — HT 带 HFE 元数据

## 代码类型
Mechanism (编码选择)

## 跨域关联
- R-19 (listpack 编码) / R-1 (编码枚举) / R-21 (dbAdd)

## 结论
编码三态 = 规模分级: 小 (listpack 零指针) / 中带 TTL (listpack EX) / 大 (dict)。升级单向 (无降级, 与 R-7 intset 同哲学)。HT 有 HFE 变体 (dictMetadata 扩展)。
源码位置: t_hash.c:74-130,1541-1550; config.c:3215,3221
