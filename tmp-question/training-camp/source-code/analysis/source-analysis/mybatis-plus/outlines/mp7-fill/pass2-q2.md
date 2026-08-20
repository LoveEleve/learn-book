# 闭环笔记 q2: strictFill 三条件匹配 + 泛型守卫

## 假设
strictInsertFill 只填充"表里确实声明了 fill 的字段", 且字段类型必须与声明一致 — 防止把不属于表模型的字段/类型不匹配的值塞进实体。

## 验证过程
- MetaObjectHandler.java:195-207 strictFill 核心:
  1. 外层守门: `(insertFill && tableInfo.isWithInsertFill()) || (!insertFill && tableInfo.isWithUpdateFill())` — 表级无 fill 字段直接跳过
  2. 内层过滤: fieldList.stream() 三重匹配 — `j.getProperty().equals(fieldName)` (字段名) **AND** `fieldType.equals(j.getPropertyType())` (字段类型精确) **AND** `(insertFill && j.isWithInsertFill()) || (!insertFill && j.isWithUpdateFill())` (fill 标记)
  3. `.findFirst()` — 匹配到才 fill
- 泛型 <T, E extends T>: StrictFill.of(fieldName, Class<T> fieldType, E fieldVal) — E 是 T 的子类, 如 OjbkXx extends Ojbk (MetaObjectHandlerTest:77-79, 84-85)
- 测试 H2MetaObjectHandler: "测试实体没有的字段 (testType1), 不应该 set 到实体" — strictInsertFill("testType1"...) 匹配不到 fieldList → 跳过
- 类型不匹配: strictInsertFill(metaObject, "age", Integer.class, 222) — 若表字段是 Long 则 fieldType 不匹配 → 不填充 (防御: commit 5412d3e52 "增强参数填充处理器,防止因类型不匹配导致转换错误")

## 代码类型
Algorithmic (流式过滤三条件) + Interface (泛型契约)

## 跨域关联
- MP-2 (TableFieldInfo.withInsertFill/withUpdateFill L221-222, propertyType) → 匹配的数据源
- MP-6 (getVersionFieldInfo 同类查 TableInfo) → 对照: 乐观锁单条件, strictFill 三条件

## 结论
strictFill = "表模型白名单": 字段名+字段类型+fill 标记三重精确匹配, 任何一个不满足就不填充。类型用 Class.equals 精确比较 (基本类型与包装类不混用), 值可以是子类实例 (泛型 E extends T)。
源码位置: MetaObjectHandler.java:195-207; StrictFill.java:31-51
