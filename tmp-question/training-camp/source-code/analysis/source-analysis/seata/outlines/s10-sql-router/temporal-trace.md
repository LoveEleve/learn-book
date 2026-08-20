# S-10 SQL 路由 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: ExecuteTemplate 路由 + SQLType 基础值 (SELECT/INSERT/UPDATE/DELETE/SELECT_FOR_UPDATE) + druid 解析 |
| 1.x | **INSERT_ON_DUPLICATE_UPDATE (102)/UPDATE_JOIN (103)** 方言扩展 (mysql 先行); SelectForUpdate 行锁镜像 |
| 2.x | **sqlparser 模块化**: core/druid/antlr 三模块; MultiExecutor (MULTI_UPDATE/DELETE 35/36); SQLVisitorFactory SPI |
| 2.5.0 | SQLType 48 值 (SEQUENCE 族/LOCK_TABLES/CHECK_TABLE 扩展); 10 方言目录 |

## 痕迹证据

- ExecuteTemplate.java:69-73: 守卫注释 (AT/GlobalLock)
- SQLVisitorFactory.java:37: SQLRecognizerFactory SPI 加载
- SQLType.java:23-218: 48 值枚举 (0-44 + 101-103)
- MultiExecutor.java:81-88: SqlServerMulti 变体分支
- sqlparser/ 三模块: 模块结构实证 (2.x)

## 推断标注

- "0.9~1.x 骨架" — Fescar 起 (公知版本线) (标注)
- "2.x 三模块" — 目录结构实证 (实证)
- "2.5.0 48 值" — 枚举实证 (实证)
- git 多 commit 可考古 — 本域以枚举/模块结构为主

## 对照线 (阶段 3/4 已交付)

- MyBatis (3.4): 插件链拦截 vs Seata 路由表 — 拦截点对比
- Druid: SQL 解析器 (druid 模块复用) vs antlr 自研
