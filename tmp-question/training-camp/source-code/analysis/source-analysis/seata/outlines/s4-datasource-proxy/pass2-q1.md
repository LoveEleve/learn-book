# 闭环笔记 q1: 代理链 — DataSourceProxy 构造与注册

## 假设
DataSourceProxy 包一层代理; 构造期完成方言识别/资源注册/undo 表检查。

## 验证过程
- **嵌套解包** (DataSourceProxy:95-100): targetDataSource instanceof SeataDataSourceProxy → getTargetDataSource — **多层代理剥洋葱**
- **init** (L105-130): 借连接 → jdbcUrl → **dbType (JdbcUtils.getDbType)** → Oracle: userName; MySQL: validMySQLVersion + **checkDerivativeProduct (polardb-x 检测 — productVersion 关键字, L136-158)** → **checkUndoLogTableExist (fast fail)** (L167-183: 无 undo_log 表 → IllegalStateException "in AT mode, %s table not exist")
- **initResourceId** (L236-252): **7 方言分支** (PG/Oracle/MySQL+polardb-x/SQLServer/DM/Oscar/default) — resourceId = 方言化 JDBC URL (集群内唯一标识)
- **注册面** (L126-129): **DefaultResourceManager.get().registerResource(this)** + TableMetaCacheFactory.registerTableMeta + **RootContext.setDefaultBranchType(AT)** (线程默认分支类型)
- **getConnection** (L212-221): 包装 new ConnectionProxy(this, target); **getPlainConnection** (L198-200): 绕过代理直连 (undo/内部操作用)
- **SQLServer 警示** (L121-124): AT 模式实验性功能 log

## 代码类型
Architecture (代理链)

## 跨域关联
- S-2: undo_log 表 fast-fail (本域是检查面)
- S-10: exec/ ExecuteTemplate (被代理链调用)
- S-12: registerResource → RM 资源管理 (锁查询面)

## 结论
代理链 = DataSourceProxy (构造期方言识别+资源注册+undo 表 fast-fail) → ConnectionProxy (上下文) → Statement 代理; 嵌套代理解包。
源码位置: DataSourceProxy.java:95-252; DefaultValues.java:38-60
