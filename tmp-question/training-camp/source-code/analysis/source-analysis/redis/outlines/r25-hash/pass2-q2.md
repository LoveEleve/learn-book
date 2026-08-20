# 闭环笔记 q2: hashTypeSet — 三编码分支

## 假设
hashTypeSet 是唯一写入入口: 三编码各自实现; KEEP_TTL/TAKE_VALUE 标志控制语义。

## 验证过程
- hashTypeSet (t_hash.c:855-977):
  - **前置转换** (L861-865): 字段/值超 64 → 转 HT (HINCRBY 场景, 其他命令已由 TryConversion 处理)
  - **LISTPACK 分支** (L867-894): lpFind (R-19) → 命中 lpReplace (L880) / 未命中 lpAppend ×2 (L887-888); **超 512 转 HT** (L893-894)
  - **LISTPACK_EX 分支** (L895-934): 三元素组 (field/value/expire); lpReplace 值 (L909) + KEEP_TTL 保留 (L918-919) / 否则 listpackExUpdateExpiry 清 TTL (L920-922); 新字段 listpackExAddNew (L928, HASH_LP_NO_TTL)
  - **HT 分支** (L935-967): hfieldNew (带 ExpireMeta=0) + **dictUseStoredKeyApi** (L941, R-3 storedKey) + dictAddRaw; 已存在: KEEP_TTL 保留旧 hfield (L947-949) / 否则 hfieldPersist (私有 hfe 摘除, L953) + dictSetKey 换新
  - TAKE_VALUE 标志 (L962-967): 直接接管 sds (免复制)
- 返回 update (0=新建/1=更新) — HSET 返回新字段数
- 调用点: hsetCommand (L2158) / hincrbyCommand (L2187, HASH_SET_TAKE_VALUE|KEEP_TTL) / hsetnxCommand

## 代码类型
Mechanism (编码内写入)

## 跨域关联
- R-19 (listpack API) / R-3 (storedKey) / R-21 (hexpires)

## 结论
hashTypeSet = 三编码统一入口 + 标志驱动语义 (KEEP_TTL 保留字段 TTL / TAKE_VALUE 零拷贝接管)。LISTPACK_EX 是 LISTPACK 的 TTL 扩展 (三元素组), 结构对称。
源码位置: t_hash.c:855-977
