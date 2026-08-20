# D-6 SQL Parser 体系 — 四层架构 + 多方言 SPI (概览)

> 项目: Druid (JDBC 连接池) | 🟡 Working / 1 篇 | sql/ 1238 文件: parser 23 + ast 422 + visitor 54 + dialect 713 + repository 11
> 基线: DRUID-PLAN D-6 (Parser 概览) — 前置: **无(叶子)** — 概览级架构, 不深入 Lexer/AST 生成算法; 被 D-3(参数化) + D-4(防火墙) 消费

---

## §0.8

- 🟡 Working，1篇 — 四层架构(**Lexer 词法[L41: Token/CharTypes/Keywords/SymbolTable, 23 文件]** → **Parser 语法[SQLStatementParser.parseStatementList L118/parseStatement L5261, SQLExprParser/SQLSelectParser]** → **AST 节点[422 文件: SQLStatement/SQLExpr/SQLTableSource 层级, SQLObject.setParent 双向链]** → **Visitor 遍历[54 文件: SQLASTOutputVisitor 输出/SchemaStatVisitor 统计/ParameterizedOutputVisitorUtils 参数化/SchemaResolveVisitor 元数据解析]**) → 方言层(**dialect/ 713 文件 29 方言目录: mysql/oracle/postgresql/sqlserver/hive/odps/...** 每方言自己的 Parser+Visitor, 如 MySqlStatementParser/MySqlOutputVisitor) → 元数据(**repository/SchemaRepository[L54] 建表语句→Schema 模型, SchemaResolveVisitor 回填类型**) → 消费者(**D-3: ParameterizedOutputVisitorUtils.parameterize WHERE id=123→id=? 合并统计; D-4: WallProvider.createParser+parseStatementList+WallVisitor 检查**)
- 设计模式: [模式: 解释器]—AST 遍历; [模式: 访问者]—Visitor 族; [模式: 策略]—方言工厂

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| sql/parser/ (23 文件) | 词法+语法 | **Lexer(L41): 字符流→Token 流(关键字表 Keywords/符号 SymbolTable); SQLStatementParser(L49): parseStatementList(L118)/parseStatement(L5261); 子类按方言** | High |
| sql/ast/ (422 文件) | 节点 | **SQLObject 基类(setParent L~): 双向父子链; SQLStatement/SQLExpr/SQLTableSource 三大分支; 每方言子类(MySqlSelectStatement 等)** | High |
| sql/visitor/ (54 文件) | 遍历 | **SQLASTVisitor 接口→SQLASTOutputVisitor(输出 SQL 文本)/SchemaStatVisitor(表/列/条件统计, getTables/getColumns/getConditions)/ParameterizedOutputVisitorUtils(参数化)/SchemaResolveVisitor** | High |
| sql/dialect/ (713 文件 28 目录) | 方言 | **每方言独立 Parser+Visitor+AST 子类: mysql/oracle/postgresql/sqlserver/hive/odps/db2/...** — 方言差异封装在子类 | High |
| sql/repository/ (11 文件) | 元数据 | **SchemaRepository(L54): 建表语句注册→Schema/SchemaObject 模型; SchemaResolveVisitorFactory 生成解析器** | High |
| sql/SQLUtils.java | 门面 | **静态工具入口: parseStatements/toSQLString/parameterize** — 消费方统一入口 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 概览域 — 1篇 (~42行) 按"四层架构→方言→消费者"展开; 只讲架构与接口职责, 不深入 lexer 状态机/AST 生成算法 (基线明确"不深入 1238 文件实现")。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | Lexer 词法层 (Token 流) | 🟡 | **为什么🟡**: 解析起点 |
| P1-2 | Parser 语法层 (parseStatementList) | 🟡 | **为什么🟡**: 语法树构建 |
| P1-3 | AST 节点层 (SQLObject 双向链) | 🟡 | **为什么🟡**: 树结构 |
| P1-4 | Visitor 遍历层 (输出/统计/参数化) | 🟡 | **为什么🟡**: 树消费方式 |
| P1-5 | 方言 SPI (28 方言) | 🟡 | **为什么🟡**: 多库适配 |
| P2-1 | SchemaRepository 元数据 | 🟢 | **为什么🟢**: 类型解析 |
| P2-2 | 消费者 (D-3 参数化/D-4 防火墙) | 🟢 | **为什么🟢**: 使用方 |
| P3-1 | 概览边界声明 (不深入算法) | 🟢 | **为什么🟢**: 范围控制 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **四层架构** | 🟡 | 主链 |
| B | **方言 SPI** | 🟡 | 扩展 |
| C | **消费者** | 🟢 | 使用 |

> **Cluster A (§1)**: Lexer→Parser→AST→Visitor 逐层职责+源码锚点
> **Cluster B (§2)**: dialect 29 目录+每方言 Parser/Visitor 子类+repository 元数据
> **Cluster C (§3)**: D-3 参数化 / D-4 防火墙 两个消费场景 (导航)

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 四层契约 | 层间唯一接口+SQLObject 双向链: 词法/语法生产树, 访问层消费树 — 新消费场景只需新 Visitor 不动解析; @@session 快速路径是 MySQL 兼容优化 | SQLParser.java:28, SQLStatementParser.java:118-150, visitor/SQLASTOutputVisitor.java:67 |
| q2 | 方言子类化 | 每库独立 Parser+Visitor+AST 子类(dialect/ 29 目录) — 方言差异渗透语法只能继承分离, 公共骨架在根包 | dialect/mysql/parser/MySqlStatementParser.java:50, dialect/mysql/visitor/MySqlOutputVisitor.java:40 |
| q3 | 内置解析器 | 监控参数化+防火墙 AST 检查都需要可控完整 AST — 内置零外部依赖, 代价 1238 文件维护成本 | D-3 mergeSql / D-4 checkInternal 消费 (导航) |
| q4 | 参数化流程 | **parameterize = SQLParserUtils.createSQLStatementParser(按 DbType 选方言)→parseStatementList→createParameterizedOutputVisitor→遍历把字面量替换 ?**; 空语句直接返回原 sql | ParameterizedOutputVisitorUtils.java:83-185 |
| q5 | 双输出 | outParameters 非空时同时收集参数值列表(visitor.setOutputParameters) — 参数化 SQL 与参数值分离, 供聚合/复用 | ParameterizedOutputVisitorUtils.java:174-178 |

→ 引出 D-3: StatFilter 监控 — mergeSql 参数化 (ParameterizedOutputVisitorUtils) 的具体调用点与 SQL 合并统计 (导航指针)
