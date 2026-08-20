# SW-4 Query / MQE / OAL Boundary — Pass 0 发现

> 仓库: `spring/skywalking`
> 日期: 2026-08-18
> 状态: **SW-4 仅完成域内再拆分 Pass 0，尚未进入某个子域的 Pass 1/2/3**

## 1. 为什么 SW-4 不能继续保持单域
原始计划把 SW-4 叫做 `Query / MQE / OAL boundary`，但首轮盘点已经证明这仍然过大：

至少存在三条明显不同的机制链：
1. **OAL 编译/runtime 链**：`oal-grammar/` + `oal-rt/`
2. **MQE 表达式/runtime 链**：`mqe-grammar/` + `mqe-rt/`
3. **Query 接口/适配层**：`server-query-plugin/`（GraphQL、Zipkin query 等）

三者的输入、输出、执行时机都不同：
- OAL：更偏规则定义 -> 代码生成 -> 运行时聚合
- MQE：更偏查询表达式求值
- Query plugin：更偏接口适配、查询聚合、外部 API

因此若继续把 SW-4 视为一个单域，会重复出现前面已规避过的“大域吞并多个机制”问题。

## 2. 当前建议的 SW-4 域内再拆分
### SW-4A OAL compiler/runtime
主战场：
- `oap-server/oal-grammar/`
- `oap-server/oal-rt/`

### SW-4B MQE grammar/runtime
主战场：
- `oap-server/mqe-grammar/`
- `oap-server/mqe-rt/`

### SW-4C Query interface / query plugin boundary
主战场：
- `oap-server/server-query-plugin/`

当前只完成了这一步拆域判断，还未开始任一子域的深审。

## 3. 已见到的结构证据
### 3.1 OAL 侧
已盘点到：
- grammar：`OALParser.g4` / `OALLexer.g4`
- runtime：`OALEngineV2`、`OALScriptParserV2`、`OALClassGeneratorV2`
- 测试：parser / generator / production scripts / runtime scripts 等多组

说明 OAL 已具备完整“语法 -> AST/模型 -> 代码生成/运行时”链路。

### 3.2 MQE 侧
已盘点到：
- grammar：`MQEParser.g4` / `MQELexer.g4`
- runtime：`MQEVisitorBase` + operation 体系（`CompareOp`、`BinaryOp`、`AggregationOp` 等）
- 测试：`AggregationOpTest`、`CompareOPTest`、`LogicalFunctionOpTest` 等

说明 MQE 更偏解释/操作符求值链。

### 3.3 Query plugin 侧
已盘点到：
- `server-query-plugin/zipkin-query-plugin/` 明确存在
- 结合 graph search 可见 `HierarchyQueryService` 等 query consumer/use-site 与 `server-core` 紧耦合
- `server-query-plugin/` 规模明显大于单个 grammar/runtime 模块

说明 query 侧不是简单“语法执行器”，而是接口层/聚合层边界。

## 4. 当前测试密度印象
- `oal-rt/`：测试较多，覆盖 parser/generator/真实脚本
- `mqe-rt/`：测试也较密集，偏 operation 级验证
- `server-query-plugin/`：尚未进入细盘点，但接口/集成成分更重，预期构建与测试策略不同

## 5. 方法论结论
此时正确动作不是直接开始写 `SW-4` 总纲，而是：
1. 接受 SW-4 需要域内再拆分
2. 先从 **SW-4A OAL compiler/runtime** 开始
3. 再到 **SW-4B MQE grammar/runtime**
4. 最后进入 **SW-4C Query interface/query plugin boundary**

## 6. 下一步唯一推荐主线
从当前点继续，唯一推荐是：
1. 先完成 `SW-4A OAL compiler/runtime`
2. 再完成 `SW-4B MQE grammar/runtime`
3. 最后再做 `SW-4C Query interface/query plugin boundary`

原因：
- OAL/MQE 都是表达式/运行时内核，更适合作为 query 侧前置基础
- Query plugin 的边界和风险依赖前两者的契约更清楚后再看，最不容易混乱
