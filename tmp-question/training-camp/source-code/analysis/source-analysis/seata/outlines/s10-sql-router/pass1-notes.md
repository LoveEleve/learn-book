# S-10 SQL 路由 — Pass 1 探索笔记

> 域: S-10 SQL 路由 | 🟡 B 方案 (无 harness) | 2026-08-15
> 源码: rm-datasource/exec/ (ExecuteTemplate 186 + 基类族 + 10 方言目录) + sqlparser/ (seata-sqlparser-core/druid/antlr 三模块 + SQLType 48 值) | Seata 2.5.0

## 调用图

```
StatementProxy.execute → ExecuteTemplate.execute (L56-177):
  守卫: !requireGlobalLock && AT != branchType → 直通原 statement (L69-73)
  SQL 识别: sqlRecognizers 空 → SQLVisitorFactory.get(sql, dbType) (SPI)
  路由 (单识别器):
    INSERT → EnhancedServiceLoader.load(InsertExecutor, dbType) (方言 SPI!)
    UPDATE → SqlServerUpdateExecutor / UpdateExecutor
    DELETE → SqlServerDeleteExecutor / DeleteExecutor
    SELECT_FOR_UPDATE → SqlServerSelectForUpdateExecutor / SelectForUpdateExecutor
    INSERT_ON_DUPLICATE_UPDATE → MySQL/Mariadb/PolarDBX 专用 (其他 NotSupportYet)
    UPDATE_JOIN → MySQL/Mariadb/PolarDBX 专用
    default → PlainExecutor
  多识别器 → MultiExecutor (MULTI_UPDATE/MULTI_DELETE 分支)
  异常包装: 非 SQLException → SQLException
```

## 基本元素分解

1. **路由核心**: ExecuteTemplate (守卫 + 识别 + 6 分支 + Multi + Plain)
2. **executor 族**: AbstractDMLBaseExecutor (S-4 实证) → Insert/Update/Delete/SelectForUpdate 子类 + 方言覆盖
3. **SQL 识别**: SQLVisitorFactory → SQLRecognizerFactory SPI (druid/antlr 双实现)
4. **SQLType**: 48 值 (0-44 + 101/102/103)
5. **方言面**: exec/ 10 方言目录 (mysql/oracle/pg/sqlserver/dm/kingbase/mariadb/oceanbase/oscar/polardbx)

## 标记问题 (20 问)

1. 路由守卫? (非 AT 直通)
2. SQL 识别? (SQLVisitorFactory SPI)
3. 路由分支? (6 类型 + Multi + Plain)
4. INSERT 路由? (InsertExecutor SPI 方言)
5. UPDATE/DELETE? (SqlServer 专用 vs 通用)
6. INSERT_ON_DUPLICATE? (3 方言支持)
7. UPDATE_JOIN? (3 方言支持)
8. MultiExecutor? (MULTI_UPDATE/DELETE)
9. SQLType 48 值? (0-44 + 101-103)
10. 异常包装? (非 SQLException → SQLException)
11. sqlparser 三模块? (core/druid/antlr)
12. SQLRecognizer 接口? (getSQLType/getTableName)
13. PlainExecutor? (兜底)
14. SelectForUpdate? (FOR UPDATE 面, S-2)
15. 方言 SPI InsertExecutor? (EnhancedServiceLoader)
16. 守卫 requireGlobalLock? (GlobalLock 场景也走)
17. MultiDelete/Update? (sqlserver 变体)
18. 对照 MyBatis? (路由 vs 插件)
19. 基类链? (AbstractDMLBaseExecutor)
20. 镜像采集? (executor 内, S-2)

## 时空溯源 (代码内注释锚)

- ExecuteTemplate:69-73 守卫注释 (AT/GlobalLock)
- SQLVisitorFactory:37 SQLRecognizerFactory SPI 加载
- SQLType:23+ 枚举 (0-44 + 101-103)
- MultiExecutor:81-88 SqlServer 变体分支

## 大域拆分判断

S-10 = SQL 路由面 (识别 + 路由 + executor 族 + 方言); 单篇 🟡 B (8 闭环 q1-q4 + 无 harness)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "ExecuteTemplate—7种SQL类型+12+数据库方言+SPI InsertExecutor" | 路由 6 类型 + Multi + Plain (ExecuteTemplate:100-168); exec/ 10 方言目录; InsertExecutor SPI | **接受+修正** ✅ (7 类型 → 6+Multi+Plain 更精确) |
| "SQL 识别器" | SQLVisitorFactory → SQLRecognizerFactory (druid/antlr 双实现) | **接受** ✅ |
| SQLType 数 | **48 值** (0-44 + 101/102/103) | **补充** ✅ |
