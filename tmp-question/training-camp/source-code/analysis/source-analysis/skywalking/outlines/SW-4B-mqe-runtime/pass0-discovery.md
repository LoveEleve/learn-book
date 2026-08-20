# SW-4B MQE grammar/runtime — Pass 0 发现

> 模块: `oap-server/mqe-grammar` + `oap-server/mqe-rt`
> 日期: 2026-08-18

## 1. 域职责
`SW-4B` 是 SkyWalking 的 MQE（Metrics Query Engine）表达式 grammar/runtime 链：
- `mqe-grammar` 提供 ANTLR grammar
- `mqe-rt` 提供 visitor 和各类 operation 执行器

它不是代码生成器链，而更像 **表达式解释执行器**。

## 2. 主链
`MQEParser.g4`
→ parse tree
→ `MQEVisitorBase`
→ operation 层：
- `BinaryOp`
- `CompareOp`
- `BoolOp`
- `AggregationOp`
- `LogicalFunctionOp`
- `TrendOp`
- `TopNOfOp`
- `SortValuesOp`
- `SortLabelValuesOp`
- `AggregateLabelsOp`

## 3. 当前结构特征
### 3.1 grammar 允许递归表达式
`MQEParser.g4` 中 `expression` 是递归定义，支持：
- arithmetic
- compare
- bool operator
- aggregation
- trend
- logical operator
- topN / topNOf
- relabels
- aggregate labels
- sort values / sort label values
- baseline

### 3.2 runtime 核心在 operation 组合
和 OAL 不同，MQE 的关键风险不在代码生成模板，而在：
- `ExpressionResultType` 组合
- labeled / unlabeled / single / series / sorted-list / record-list 的交叉
- `emptyValue` 与 `boolResult` 标志传播

## 4. 当前测试现实
已见测试 **12** 组，完整回归后约 **28** 用例通过（本轮新增 2 个）。

已覆盖：
- aggregation
- binary
- compare
- bool
- logical function
- trend
- topNOf
- sort values
- sort label values
- aggregate labels

## 5. 首轮质疑点
- Q1: many↔one / one↔many 时 single-side `emptyValue` 是否正确传播
- Q2: `boolResult` 约束是否只在 bool op 生效，其他 op 是否会错误消费它
- Q3: labeled 与 unlabeled 结果 join/match 的语义是否对称
- Q4: baseline / relabels / aggregateLabels 是否有隐藏空值问题

## 6. 已发现的真实问题
本轮已证实：
- `LROp.many2OneBinaryOp(...)` 与 `one2ManyBinaryOp(...)` 初版只检查 many-side `emptyValue`
- 若 single result 本身是 empty，仍继续读取其 doubleValue 参与运算
- 导致结果被错误算成非空值，而不是传播 empty

这是 MQE 运行时的真实语义缺陷，已在本轮修复。
