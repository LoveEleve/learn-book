# D-6 SQL Parser 体系 — 四层架构 + 多方言 SPI (概览)

> 前置: 无 (叶子域) | 复用: [[D-3-statfilter]] [[D-4-wallfilter]] (消费者) | 引出: [[D-3-statfilter]]
> 🟡 Working | 8 KP | [模式: 解释器 + 访问者 + 策略]
> Pass 2 闭环: q1(四层契约) q2(方言子类化) q3(内置解析器) q4(参数化流程) q5(双输出)

**读者处境**: Druid 的 SQL 监控能把 `WHERE id=123` 和 `WHERE id=456` 统计成同一条 — 它怎么"看懂"SQL 的?WallFilter 又怎么在 SQL 执行前发现注入?秘密在 Druid 自带的一个完整 SQL 解析器 (1238 文件, 比连接池本身还大)。这篇只讲架构地图 — 深入 lexer 状态机不在范围内。

### 1. 四层架构 — Lexer → Parser → AST → Visitor

场景: 一段 `SELECT * FROM user WHERE id=1` 从字符串变成可检查的树, 中间过几道工序?

源码路径:
- `sql/parser/Lexer.java:41` — **词法**: `public class Lexer`(L41) — 字符流逐个扫描成 `Token`(关键字表 `Keywords`/符号表 `SymbolTable`), 产出 token 流
- `sql/parser/SQLStatementParser.java:49,118,5261` — **语法**: `parseStatementList(List)`(L118)/`parseStatement()`(L5261) — 按 token 流递归下降构建 AST
- `sql/ast/`(422 文件) — **节点**: `SQLObject` 基类带 `setParent` 双向链 — 树由 `SQLStatement`(语句)/`SQLExpr`(表达式)/`SQLTableSource`(表源) 三大分支组成
- `sql/visitor/`(54 文件) — **遍历**: `SQLASTVisitor` 接口 → `SQLASTOutputVisitor`(AST 还原成 SQL 文本)/`SchemaStatVisitor`(表/列/条件统计)/`ParameterizedOutputVisitorUtils`(参数化合并)/`SchemaResolveVisitor`

关键设计: **Why 四层？** 词法(字符串→token)→语法(token→树)→遍历(树→输出/统计) — 每一层输入输出明确, 与编译器的 lexer/parser 同构; AST 一旦建成, 各种"消费者"只需要写新 Visitor, 不用碰解析。[模式: 解释器 + 访问者]

数据流: `SELECT * FROM user WHERE id=1` → Lexer 扫描 → token 流(1/2/3...) → SQLStatementParser.parseStatementList(118) → SQLSelectStatement 树 → 任意 Visitor 遍历: OutputVisitor 还原 SQL / SchemaStatVisitor 统计 / WallVisitor 检查(导航 D-4)。

### 2. 方言 SPI — 29 种数据库各自为政

场景: MySQL 的 `LIMIT` 和 Oracle 的 `ROWNUM` 语法完全不同, Druid 怎么处理?

源码路径:
- `sql/dialect/`(713 文件, 29 目录) — **方言族**: mysql/oracle/postgresql/sqlserver/hive/odps/db2/clickhouse/... 每方言一套 **Parser 子类**+**Visitor 子类**+**AST 子类** — 实证: `dialect/mysql/parser/MySqlStatementParser.java:50` `extends SQLStatementParser`、`dialect/mysql/visitor/MySqlOutputVisitor.java:40` `extends SQLASTOutputVisitor implements MySqlASTVisitor`
- `sql/repository/SchemaRepository.java:54` — **元数据**: 建表语句注册 → `Schema/SchemaObject` 模型; `SchemaResolveVisitorFactory` 产出解析器, 配合 `SchemaResolveVisitor` 回填列类型

关键设计: **Why 子类化而非配置？** 方言差异渗透在语法规则里(LIMIT/分页/函数), 只能按方言重写 Parser/Visitor — 用继承把"公共语法"和"方言语法"分离, 公共层收在 parser/ 根包。[模式: 策略 — 方言工厂]

数据流: 解析入口按 `DbType` 选方言 → createParser(如 MySqlStatementParser) → 公共 parseStatementList 骨架 + 方言子类覆写差异语法 → AST 子类节点 → 输出/统计同样走方言 Visitor。

### 3. 消费者 — StatFilter 参数化 与 WallFilter 检查

场景: 解析器在连接池里被谁用?为什么监控能合并 SQL?

源码路径:
- `D-3 消费`(导航): `StatFilter.mergeSql` → `ParameterizedOutputVisitorUtils` — **parameterize 流程(闭环 q4): 方言解析器(按 DbType)→parseStatementList→ParameterizedVisitor 遍历→字面量替换 `?`**(L160-185); outParameters 非空时同时收集参数值(q5); 空语句直接返回原 sql(L170-172)
- `D-4 消费`(导航): `WallProvider.checkInternal`(WallProvider L454): `createParser(sql)`(L481)+`parseStatementList`(L494) → WallVisitor 遍历规则检查
- `sql/SQLUtils.java` — **门面**: 静态 `parseStatements/toSQLString/parameterize` — 外部统一入口

关键设计: **Why 解析器内置而非用第三方？** 监控要"按参数化 SQL 聚合", 防火墙要"AST 级规则检查" — 两者都需要可控的完整 AST; 内置解析器让这两个消费点零外部依赖, 代价是 1238 文件的维护成本 (本域只学架构, 不深入算法)。**显式不做**: ①不集成第三方解析器(如 JSqlParser) — 定制深度(参数化/方言)无法满足 ②不深入 Lexer 状态机/AST 生成算法 — 概览边界。[模式: 门面]

数据流: 消费方 → SQLUtils 门面 / 直接方言工厂 → 四层管线 → 参数化输出(StatFilter 统计键)/规则检查(WallFilter 放行判定) → 详见 D-3/D-4。

→ 引出 D-3: StatFilter 监控 — mergeSql 参数化 (ParameterizedOutputVisitorUtils) 的具体调用点与 SQL 合并统计 (导航指针)。
