# 闭环笔记 q1: 动态二选一 — isWithLogicDelete 切换 UPDATE/DELETE

## 假设
同一注入方法类 (Delete/DeleteById/DeleteByMap/DeleteByIds) 根据表是否有逻辑删除字段, 动态生成 UPDATE (逻辑删除) 或 DELETE (物理删除) SQL, 命令类型同步切换。

## 验证过程
- Delete.java:47-62: `if (tableInfo.isWithLogicDelete())` → SqlMethod.LOGIC_DELETE ("UPDATE %s %s %s %s") + sqlLogicSet + sqlWhereEntityWrapper → **addUpdateMappedStatement (UPDATE 命令)**; else → SqlMethod.DELETE ("DELETE FROM %s %s %s") → **addDeleteMappedStatement (DELETE 命令)**
- DeleteById.java:57-79: 逻辑 → LOGIC_DELETE_BY_ID (UPDATE); 物理 → DELETE_BY_ID + addDeleteMappedStatement
- DeleteByMap.java:46-63: 同构 (LOGIC_DELETE_BY_MAP vs DELETE_BY_MAP)
- DeleteByIds.java:84-100: logicDeleteScript 恒逻辑 (SQL 模板本身 UPDATE); 物理分支同类
- addUpdateMappedStatement (AbstractMethod.java:382-395): SqlCommandType.UPDATE; addDeleteMappedStatement: DELETE — **命令类型影响 MappedStatement.sqlCommandType** → 触发 MybatisParameterHandler.updateFill (MP-7 q8 连接)

## 代码类型
Algorithmic (注入期动态分支) + Glue (命令类型路由)

## 跨域关联
- MP-1 (AbstractMethod 模板/DefaultSqlInjector) → 方法类的宿主
- MP-7 (updateFill 触发依赖 UPDATE 命令) → 逻辑删除分支用 addUpdateMappedStatement 是填充触发的根源
- M-2 (SqlCommandType 路由) → 命令类型语义

## 结论
逻辑删除的"删除"本质是 UPDATE: 同一方法类注入期动态二选一, 命令类型 (UPDATE/DELETE) 同步切换 — 物理/逻辑删除在 SQL 层共存, 切换点就是 isWithLogicDelete 这一个布尔值。
源码位置: Delete.java:47-62; DeleteById.java:57-79; DeleteByMap.java:46-63
