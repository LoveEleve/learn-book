# 闭环笔记 q6: 批量删除 — foreach 双形态 + 实体集合转换

## 假设
DeleteByIds 的 WHERE id IN (...) 支持两种参数形态: 主键集合 (SimpleType) 和实体集合 (取实体的主键属性); 实体集合删除是后加的转换能力。

## 验证过程
- DeleteByIds.logicDeleteScript L93-96: `convertForeach(convertChoose("!@org.apache.ibatis.type.SimpleTypeRegistry@isSimpleType(item.getClass())", "#{item}", "#{item." + keyProperty + "}"), COLL, null, "item", COMMA)` — **每个元素运行时判定**: 简单类型 (Long/String) → `#{item}` 直接用; 实体 → `#{item.id}` 取主键属性
- 命令仍是 UPDATE: `UPDATE %s %s WHERE %s IN (...) AND deleted=0` (SqlMethod.LOGIC_DELETE_BY_IDS, "3.5.7 从 deleteBatchIds 更名")
- 实体集合删除转换 (CHANGELOG 252: "逻辑删除 byId 支持转换为实体删除填充"; 4d5e4e45a: "存在逻辑删除且含有填充字段自动转换实体删除(主键类型必须与值类型完全匹配)"): 实体集合删除时 — 若有 withUpdateFill 字段, SET 里带实体字段 (ENTITY 前缀 + convertIf 实体非空), 且每实体单独填充 (MybatisParameterHandler 批量提取填充, MP-7 q4)
- LogicDelTest: deleteByIds(entityList) → 影响行数 2; deleteByIds(Arrays.asList(entity1.getId(), entity2.getId())) → 主键集合形态
- DeleteBatchByIds (deprecated 3.5.7 更名 deleteByIds)

## 代码类型
Algorithmic (运行时元素形态判定)

## 跨域关联
- MP-7 (批量提取填充 extractParameters, q4) → 实体集合删除的填充支撑
- M-2 (wrapCollection list/array 包装) → 集合参数约定
- M-3 (SimpleTypeRegistry) → 形态判定工具

## 结论
DELETE IN 批量删除的元素形态运行时二选一: SimpleType 直接用值, 实体取主键属性 — 一次注入同时服务 id 集合与实体集合; 实体集合 + withUpdateFill 时走"实体删除转换" (填充进 SET)。
源码位置: DeleteByIds.java:84-100; SqlMethod.java:58,62; LogicDelTest.java:50-57
