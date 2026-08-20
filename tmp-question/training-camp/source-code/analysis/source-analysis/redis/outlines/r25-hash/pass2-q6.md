# 闭环笔记 q6: hashTypeSetEx — 条件 TTL 设置

## 假设
字段 TTL 设置 = 三阶段 (Init/SetEx/Done); SetExpiryHT 条件矩阵 (GT/LT/NX/XX); 已过期字段直接删。

## 验证过程
- 三阶段框架:
  - hashTypeSetExInit (L1114-1190): 上下文收集 (HashTypeSetEx: minExpireFields/fieldUpdated/fieldDeleted) + 编码升级准备
  - hashTypeSetEx (L1068-1102): 分发 (LISTPACK_EX → SetExpiryListpack; HT → SetExpiryHT)
  - hashTypeSetExDone (L1192-1238): 全局 HFE 更新 (ebRemove/ebAdd db->hexpires)
- hashTypeSetExpiryHT (L979-1060):
  - 字段无 TTL 元数据 (L994-1004): **XX|GT 条件不满足** (L997-1000, 无 TTL 视为无限); LT/NX 通过
  - 字段有 TTL (L1005-1037): **条件矩阵** (L1014-1017): GT 且 prev>=new / LT 且 prev<=new / NX → 拒; ebRemove 私有 hfe (L1021) + minExpireFields 追踪 (L1024-1025)
  - **已过期删字段** (L1044-1051): checkAlreadyExpired → propagateHashFieldDeletion + hashTypeDelete + HSETEX_DELETED (R-22 checkAlreadyExpired 同款)
  - 重注册 (L1056-1058): ebAdd 私有 hfe + fieldUpdated++
- 条件枚举: HFE_NX=1<<0 / XX=1<<1 / GT=1<<2 / LT=1<<3 (t_hash.c:197-200)
- SETEX 命令入口: HSET key field val EX 60 扩展 (hashTypeSetEx 链)

## 代码类型
Mechanism (条件 TTL)

## 跨域关联
- R-22 (checkAlreadyExpired/GT/LT 语义) / R-21 (hexpires)

## 结论
字段条件 TTL = 键级 EXPIRE 标志 (R-22 NX/XX/GT/LT) 的字段版 + 三阶段框架 (先收集后应用)。无 TTL 视为无限 (GT 失败/LT 通过) — 与 R-22 完全同语义。已过期字段在设置时即删 (不落库)。
源码位置: t_hash.c:979-1238
