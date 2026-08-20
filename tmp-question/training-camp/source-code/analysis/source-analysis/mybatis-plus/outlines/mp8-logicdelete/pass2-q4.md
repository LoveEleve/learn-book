# 闭环笔记 q4: 更新面 — updateById 带 AND deleted=0, SET 恒排除 deleted

## 假设
更新操作对逻辑删除有两层防护: WHERE 注入未删除条件 (更新已删记录不生效), SET 排除 deleted 字段自身 (防用户意外改删除标记)。

## 验证过程
- UpdateById.java:48-51: `additional = optlockVersion(tableInfo) + tableInfo.getLogicDeleteSql(true, true)` → UPDATE_BY_ID 模板尾缀 → **WHERE id=#{id} AND deleted=0**; sqlSet 传 `tableInfo.isWithLogicDelete()` (logic=true)
- sqlSet (AbstractMethod.java:120-130) → getAllSqlSet(logic=true, prefix) (TableInfo.java:416-430): `ignoreLogicDelFiled=true → filter(!(isWithLogicDelete() && i.isLogicDelete()))` — **SET 排除 deleted 字段**
- Update.java:45: sqlSet(true, true, ...) — **恒排除 deleted** (不依赖表配置); sqlWhereEntityWrapper(true) 含未删除条件
- 推论: updateById(已逻辑删除记录) → WHERE id=? AND deleted=0 不匹配 → 影响行数 0 (更新不生效, 与 MP-6 乐观锁冲突语义同源: 行数 0 = 失败)
- 对比: updateById 的 SET 若不带 deleted 排除 → 用户实体 deleted 字段有值会覆盖删除标记 (危险) — 所以恒排除

## 代码类型
Algorithmic (SQL 防护双路)

## 跨域关联
- MP-6 (行数 0 = 冲突语义) → 更新已删记录的行数 0 语义
- MP-1 (sqlSet/logic 参数路由) → 注入面
- MP-7 (updateFill 也会改 SET? — 不, deleted 字段本身不标 fill; withUpdateFill 字段可进 SET)

## 结论
更新面双防护: WHERE AND deleted=0 (更新已删不生效) + SET 恒排除 deleted (防覆盖删除标记)。与查询注入 (q3) 的"排除自身"是同一设计哲学: 逻辑删除字段由框架专属管理, 用户 SQL 面不可见。
源码位置: UpdateById.java:48-51; AbstractMethod.java:120-130; TableInfo.java:416-430
