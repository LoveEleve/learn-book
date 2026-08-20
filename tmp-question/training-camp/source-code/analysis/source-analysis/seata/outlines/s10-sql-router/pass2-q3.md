# 闭环笔记 q3: SQL 识别 — SQLVisitorFactory + SQLType 48 值

## 假设
SQL 类型识别经 SPI 工厂; SQLType 枚举覆盖 48 种。

## 验证过程
- **SQLVisitorFactory** (rm-datasource/sql): **SQLRecognizerFactory SPI 加载** (sqlParserType 配置, L37) → get(sql, dbType) 返回识别器列表
- **sqlparser 三模块**: seata-sqlparser-core (接口/识别器) + **seata-sqlparser-druid** (Druid 实现) + **seata-sqlparser-antlr** (ANTLR 实现) — 双解析器可切换
- **SQLType 48 值** (SQLType.java): 0-44 (SELECT/INSERT/UPDATE/DELETE/SELECT_FOR_UPDATE/REPLACE/TRUNCATE/CREATE/DROP/LOAD/MERGE/SHOW/ALTER/RENAME/DUMP/DEBUG/EXPLAIN/PROCEDURE/DESC/SELECT_LAST_INSERT_ID/SELECT_WITHOUT_TABLE/SEQUENCE 族/SAVE_POINT/SELECT_FROM_UPDATE/**MULTI_DELETE(35)/MULTI_UPDATE(36)**/CREATE_INDEX/DROP_INDEX/KILL/RELEASE_DBLOCK/LOCK_TABLES/UNLOCK_TABLES/CHECK_TABLE/SELECT_FOUND_ROWS) + **101/102/103** (INSERT_IGNORE/**INSERT_ON_DUPLICATE_UPDATE(102)/UPDATE_JOIN(103)**)
- **SQLRecognizer 接口**: getSQLType/getTableName/getOriginalSQL — 识别结果契约

## 代码类型
Implementation (SQL 识别)

## 跨域关联
- S-10: 路由输入 (本域是识别面)
- S-2: 镜像类型判定 (sqlRecognizer.getSQLType)

## 结论
识别 = SPI 工厂 (druid/antlr 双实现) + SQLType 48 值枚举; 路由消费 6+2 类型。
源码位置: SQLVisitorFactory.java:28-47; SQLType.java:23-218; sqlparser/ 三模块
