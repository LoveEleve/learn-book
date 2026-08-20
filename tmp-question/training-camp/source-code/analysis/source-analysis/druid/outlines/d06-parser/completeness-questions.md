# D-6 SQL Parser 体系 全视角验证

## 开发者视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 1 | 想给解析器加一种新数据库支持要动哪些层? | §2 (Parser+Visitor+AST 三个子类) |
| 2 | 怎么把 AST 还原成 SQL 文本? | §1 (SQLASTOutputVisitor) |
| 3 | 外部怎么调用解析器? | §3 (SQLUtils 门面 parseStatements/parameterize) |
| 4 | 解析结果存在什么结构里? | §1 (SQLStatement/SQLExpr/SQLTableSource 树, setParent 双向链) |

## 架构师视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 5 | 为什么四层分开? | §1 (每层输入输出明确, 新消费者只写 Visitor) |
| 6 | 为什么方言用子类化? | §2 (方言差异渗透语法, 继承分离公共/方言) |
| 7 | 为什么内置解析器不用第三方? | §3 (监控聚合+防火墙检查都要可控 AST, 零外部依赖) |
| 8 | SchemaRepository 解决什么问题? | §2 (列类型解析, SchemaResolveVisitor 回填) |
| 9 | 本域为什么不深入算法? | §1/§3 (概览定位, 1238 文件只学架构地图) |

## 学生视角
| # | 问题 | 答案位置 |
|:--:|------|------|
| 10 | SQL 从字符串到树经过哪几步? | §1 (Lexer→Parser→AST→Visitor 四层) |
| 11 | Druid 支持多少种方言? | §2 (29 目录 713 文件) |
| 12 | 参数化是什么意思? | §3 (WHERE id=123 → id=?, 合并统计) |
| 13 | Visitor 模式在这里怎么用? | §1 (输出/统计/检查都是不同 Visitor) |
| 14 | 参数化完整流程几步? | §3 (方言解析器→parseStatementList→ParameterizedVisitor 遍历→字面量替换 ?) |
| 15 | 参数化能同时输出参数值吗? | §3 (outParameters 收集, visitor.setOutputParameters) |

## 覆盖: 15 问 / 3 身份 / 100%
