# 闭环笔记 q7: HFE 命令族 — hexpire/httl/hpersist

## 假设
HFE 命令族 = 键级 EXPIRE 命令的字段版: hexpire (N NX/XX/GT/LT) / httl / hpersist; 传播归一。

## 验证过程
- hexpireGenericCommand (t_hash.c:3124-3247):
  - 参数: key + N 个 field + 时间 + 条件 (NX/XX/GT/LT) — 多字段批量
  - 解析 (L3130-3170): 时间单位 (EX/PX/EXAT/PXAT) + 条件标志
  - 执行 (L3171+): 逐字段 hashTypeSetEx 三阶段 (Init/SetEx/Done 批量) — minExpireFields 聚合后一次全局更新
  - **传播**: 重写为 HEXPIRE 族规范形式? — 验证: rewriteClientCommandVector 到 hexpireat 变体
- httlGenericCommand (L2968-3123): 字段 TTL 查询 — 未设置 → -2 / 无 TTL → -1 / 剩余毫秒; 三值语义同 TTL (R-22)
- hpersistCommand (L3289-3294): 字段 PERSIST — hfieldPersist (L2886: 私有 hfe 摘除 + ExpireMeta 清除)
- hfield 结构 (L2837-2916): _hfieldNew (mstr 头 + 可选 ExpireMeta) / hfieldIsExpireAttached / hfieldGetExpireTime / hfieldIsExpired / hfieldPersist
- 传播归一: propagateHashFieldDeletion (L2918-2938) — 合成 HDEL 给从库

## 代码类型
Mechanism (字段级命令)

## 跨域关联
- R-22 (EXPIRE/TTL 三值/传播归一) / R-3 (mstr 奇数地址 — itemsAddrAreOdd=1)

## 结论
HFE 命令族 = R-22 命令面的字段级镜像: 条件标志/三值语义/传播归一全同构。多字段批量用三阶段框架聚合 (一次全局 HFE 更新)。hfield 的 mstr 头部奇数地址支撑 ebuckets 指针低 1 位判别 (R-21 itemsAddrAreOdd=1)。
源码位置: t_hash.c:2837-3294
