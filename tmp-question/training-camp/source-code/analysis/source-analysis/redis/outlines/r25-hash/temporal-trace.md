# R-25 t_hash — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2009 (antirez) | t_hash.c 初版 — HSET/HGET 家族 (版权 "2009-Present"); 小 hash 用 ziplist |
| 2.6 | HT 编码 + ziplist↔HT 阈值转换 (512/64) |
| 7.0 | **ziplist → listpack** (R-19); **storedKey API** (R-3) — hfield 直接作 dict 键 (免 sds 副本) |
| 7.4 (2024) | **HFE 字段级过期**: LISTPACK_EX 三元素组 (field/value/expire); mstrHashDictTypeWithHFE; 私有 hfe ebuckets; hexpire 命令族; GETF 惰性链; trash 状态机 |
| 演进 | HMSET 弃用 (回复差异保留 L2173 注释); HRANDFIELD 加权随机 (7.0+); HSET EX/PXAT 字段 TTL 选项 (7.4) |

## 痕迹证据

- L2173: "HMSET (deprecated) and HSET return value is different" — 历史兼容痕迹
- L1634: `dictExpireMeta->expireMeta.trash = 1` — 转换期状态机 (注释 L1634 前: "mark as trash (as long it wasn't ebAdd())")
- L941: dictUseStoredKeyApi — R-3 storedKey 应用点 (hfield 即键)
- L601-604: TryConversion 预扩注释 ("We guess that most of the values in the input are unique, so if there are enough arguments we create a pre-sized hash")
- L859-860: hashTypeSet 前置转换注释 ("needed for HINCRBY* case since in other commands this is handled early by hashTypeTryConversion")
- L1585/1628: dictExpand 预扩 — 转换免 rehash (与 R-6 zset 同技巧)
- L217: hfield 奇数地址 (mstr) — itemsAddrAreOdd=1 依据

## 推断标注

- "HSET 百万字段内存分布" — listpack 超 512 转 dict 是事实, 具体字节数对比是推断
- "HFE 是 7.4 引入" — LISTPACK_EX/hexpire 代码是事实, 精确版本号 (7.4) 从 ebuckets 版权 (2024) 推断
- "HT 编码性能优势" — 转换必然性 (listpack 线性查找 O(n)) 是推断, 代码只保证正确性
