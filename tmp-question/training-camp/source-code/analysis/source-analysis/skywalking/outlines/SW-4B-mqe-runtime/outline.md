# SW-4B MQE grammar/runtime — Outline

> 模块: `oap-server/mqe-grammar` + `oap-server/mqe-rt`
> 日期: 2026-08-18

## 1. 域定位
`SW-4B` 是 MQE 表达式解释执行链：
- grammar 解析查询表达式
- visitor 分发到 operation
- operation 基于 `ExpressionResult` 做值运算、比较、聚合和结果整形

与 `SW-4A OAL` 相比，它不是代码生成器，而是更偏“执行期 evaluator”。

## 2. 机制主链
- grammar：`MQEParser.g4` / `MQELexer.g4`
- visitor：`MQEVisitorBase`
- operation：`BinaryOp`、`CompareOp`、`BoolOp`、`AggregationOp`、`LogicalFunctionOp`、`TrendOp`、`TopNOfOp`、`SortValuesOp`、`SortLabelValuesOp`、`AggregateLabelsOp`
- 底层共性：`LROp`

`LROp` 是多个左右值操作的共享骨架，是本域的真正关键点之一。

## 3. 关键语义
### 3.1 ExpressionResult 形态
MQE 运行时必须处理：
- single value
- time series values
- sorted list
- record list
- labeled / unlabeled
- empty value
- bool result

### 3.2 grammar 允许递归表达式
`MQEParser.g4` 的 `expression` 是递归的，因此与 OAL 不同，它天然支持 operator 组合，而不只是固定匹配模式。

### 3.3 operation 的主要风险
主要在这些维度：
- many↔one / one↔many 的值传播
- labeled 集合按 label 对齐
- compare 结果如何映射为 0/1
- bool op 对 `boolResult` 的依赖
- empty value 是否应短路传播

## 4. 本轮确认并修复的真实缺陷
`LROp` 在以下路径中原本遗漏 single-side empty 传播：
- `many2OneBinaryOp(...)`
- `one2ManyBinaryOp(...)`

初版行为：
- 只检查 many-side `emptyValue`
- single result 为 empty 时仍继续计算

修复后：
- many-side 或 single-side 任一为空都传播 empty
- 语义与 single↔single、series↔series 路径更一致

## 5. 测试覆盖
原有测试覆盖：
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

本轮新增：
- `LROpEdgeCaseTest`

新增覆盖：
- empty scalar + many result 的 binary 路径
- empty scalar + many result 的 compare 路径

## 6. 当前结论
`SW-4B` 当前已达到可交接收敛状态：
- grammar / visitor / operation 边界清晰
- 已确认并修复一个真实运行时缺陷
- 完整回归通过

## 7. 外域边界
- `SW-4A`：OAL compiler/runtime 是另一条链
- `SW-4B`：MQE expression runtime
- `SW-4C`：Query interface/plugin 才是 MQE 的消费层

## 8. 验证命令
```bash
./mvnw -pl oap-server/mqe-rt -am -Dtest=LROpEdgeCaseTest -Dsurefire.failIfNoSpecifiedTests=false test
./mvnw -pl oap-server/mqe-rt -am test
```

## 9. 剩余风险
- baseline / relabels / aggregateLabels 仍可继续做 targeted probing
- labeled 结果的极端对齐输入仍值得后续继续质疑
