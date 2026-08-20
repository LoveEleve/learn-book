# 闭环笔记 q3: getLogicDeleteSql 的 isWhere 双语义 + 查询三路注入

## 假设
getLogicDeleteSql(startWithAnd, isWhere) 的 isWhere 参数控制取未删除值还是删除值 — isWhere=true 用于查询条件 (未删除值), false 用于删除 SET (删除值); 查询条件通过三条路径注入。

## 验证过程
- TableInfo.getLogicDeleteSql L437-448 → formatLogicDeleteSql(isWhere):
  - **isWhere=true → logicNotDeleteValue** (未删除值, 如 deleted=0 或 IS NULL) — 查询条件
  - **isWhere=false → logicDeleteValue** (删除值, 如 deleted=1) — 删除 SET
  - startWithAnd=true 时前缀 " AND "
  - 反直觉命名: "isWhere" 意为"是否作为查询条件", 查询条件取的是**未删除**值 (WHERE deleted=0)
- 查询三路注入:
  1. **主键查询尾缀**: SelectById.java:50 / SelectBatchByIds:51 — `getLogicDeleteSql(true, true)` 拼在 WHERE 尾部 → `WHERE id=#{id} AND deleted=0`
  2. **wrapper 路径**: AbstractMethod.sqlWhereEntityWrapper L243-246 — 逻辑删除分支: `convertWhere(table.getLogicDeleteSql(false, true) + NEWLINE + sqlScript)` — 未删除条件包在 <where> 最前 → SELECT_LIST/SELECT_COUNT/Update/Delete 共用 (SelectList.java:47)
  3. **普通字段排除**: TableInfo.getAllSqlWhere L395-402 — ignoreLogicDelFiled=true → 过滤掉 deleted 字段自身, 不让它生成普通 `<if>` 条件 (条件已由 getLogicDeleteSql 注入, 避免重复)
- CHANGELOG 77d8031b7: "优化逻辑删除查询字段紧随 where 条件查询" — convertWhere 位置演进

## 代码类型
Algorithmic (SQL 片段双语义) + Glue (三路拼装点)

## 跨域关联
- MP-1 (AbstractMethod.sqlWhereEntityWrapper L228-260) → wrapper 查询共用面
- MP-7 (逻辑删除 SQL 也是 UPDATE) → 同触发面
- M-6 (convertWhere/convertIf 脚本工具) → SqlScriptUtils

## 结论
isWhere=true 语义 = "查询条件" → 取未删除值; 查询三路: 主键尾缀 (SelectById) / convertWhere 包裹 (wrapper 路径) / 字段排除 (getAllSqlWhere)。deleted 字段自身从不生成普通 where 条件 — 逻辑删除条件由 getLogicDeleteSql 统一注入, 避免双写。
源码位置: TableInfo.java:437-470; AbstractMethod.java:243-246; SelectById.java:50
