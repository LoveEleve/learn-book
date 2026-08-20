# 闭环笔记 q6: 填充策略族 (setFieldValByName/fillStrategy/strictFillStrategy) + 幂等性

## 假设
三个填充入口对 null 的容忍度不同: setFieldValByName 值非 null 才 set; fillStrategy 当前值 null 才填; strictFillStrategy 当前值 null 且新值非 null 才填。有值不覆盖 → 重复执行幂等。

## 验证过程
- MetaObjectHandler.java:101-106 setFieldValByName: `Objects.nonNull(fieldVal) && metaObject.hasSetter(fieldName)` → setValue — **提供值非 null 且实体有 setter 才 set** (直接覆盖已有值)
- L115-117 getFieldValByName: hasGetter ? getValue : null — 无 getter 返回 null (H2MetaObjectHandler 用此读 testType)
- L218-223 fillStrategy: `if (getFieldValByName(...) == null) { setFieldValByName(...) }` — **当前值 null 才填** (有值不覆盖, 历史 BUG 修复: CHANGELOG 1017 "修复BUG自动填充会覆盖之前的值")
- L234-242 strictFillStrategy: `if (metaObject.getValue(fieldName) == null) { Object obj = fieldVal.get(); if (Objects.nonNull(obj)) { setValue } }` — 当前值 null AND supplier 产出非 null 才 set (双保险)
- 幂等性: 每次 SQL 执行 new ParameterHandler (q1) → 同一实体第二次执行时字段已有值 → fillStrategy/strictFillStrategy 有值不覆盖 → 不重复填充

## 代码类型
Algorithmic (策略选择)

## 跨域关联
- MP-2 (fill 字段 SQL 直拼 q3) → 直拼列的值由本策略族保证
- strictFill (q2) 内部用 strictFillStrategy — 严格模式与普通 fillStrategy 的分工

## 结论
策略梯度: setFieldValByName (强制写, 值非 null) → fillStrategy (空才写) → strictFillStrategy (空+新值非 null 才写)。幂等由"有值不覆盖"保证 — 这是"每次执行都 new ParameterHandler"设计能成立的基石。
源码位置: MetaObjectHandler.java:101-106, 218-223, 234-242
