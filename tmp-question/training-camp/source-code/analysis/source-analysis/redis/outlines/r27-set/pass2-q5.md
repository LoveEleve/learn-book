# 闭环笔记 q5: SADD/SREM 命令面与空集

## 假设
SADD = 创建 (按 hint) → 预转换 → setTypeAdd 批量; SREM 空集删键; SMOVE 双向。

## 验证过程
- saddCommand (t_set.c:583-607): lookupKeyWrite → 不存在 setTypeCreate (size_hint = 新增数, L592-595) + setTypeMaybeConvert (L596) → 批量 setTypeAdd (L597-600) → 返回新增数 + notify "sadd"
- sremCommand (L608-635): 批量 setTypeRemove → **空集 dbDelete + notify "del"** (L620-624) → 返回删除数
- smoveCommand (L636-690): src==dst 短路 (L654-658) → setTypeRemove → **空 src 删键** (L665-669) → dst 创建 setTypeCreate (L672-674) → setTypeAdd + 双信号
- sismember/smismember (L691-721): setTypeIsMember 三编码分派
- scard (L722): setTypeSize
- sscanCommand (L1650): scanGenericCommand (R-21)

## 代码类型
Glue (命令面)

## 跨域关联
- R-21 (dbAdd/dbDelete) / R-26 (空集删键不变量同款)

## 结论
命令面 = lookup → 创建 (hint 预判) → 批量操作 → 空集删键 (维持"空集不存在")。SADD 的 size_hint 是创建时一次性预判 (避免增长后逐次转换)。
源码位置: t_set.c:583-738,1650
