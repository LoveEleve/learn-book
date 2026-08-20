# SW-4A OAL compiler/runtime — Pass 2 问题收敛

> 模块: `oap-server/oal-grammar` + `oap-server/oal-rt`
> 日期: 2026-08-18

## Q1: OAL parser 是否支持一般布尔表达式递归组合
不支持。

`OALParser.g4` 中的 `expression` 不是递归 AST，而是固定列举：
- `booleanMatch`
- `numberMatch`
- `stringMatch`
- `greater/less/...`
- `like/in/contain/not contain`

结论：
- OAL filter grammar 当前是“单条件匹配表达式”集合，不是一般逻辑表达式语言
- 这解释了为何很多复杂度转移到了 value 类型、map attribute 和 function arg 处理，而不是 parser 运算优先级

## Q2: parser / enricher / generator 的分层是否清晰
清晰。
- parser 用 `OALListenerV2` 生成不可变 `MetricDefinition`
- `MetricDefinitionEnricher` 通过反射补 source column / entrance method / persistent fields 等元数据
- `OALClassGeneratorV2` 基于 `CodeGenModel` 和模板生成类

结论：分析时不能把 parse tree 细节直接等同于生成逻辑，真正的“桥”在 enricher。

## Q3: 已知高风险边界是否已有测试覆盖
已确认覆盖较多，尤其：
- `>= <= like in contain not contain`
- map attribute
- nested boolean accessor
- array values for `in`
- decorator
- production scripts
- runtime generation

结论：本域当前最大的风险不是“无测试覆盖”，而是是否还有少数边角语义未进入真实脚本集。

## Q4: OAL compile pipeline 是否只生成 metrics 类
不是。会同时生成：
- metrics class
- metrics builder class
- dispatcher class

结论：任何 compile/runtime 问题都可能出现在三个生成物中的任一环节，不能只盯 metrics class。

## Q5: 当前是否发现直接源码缺陷
截至本轮，没有发现可复现的真实源码缺陷。

完整 `oal-rt` reactor 回归通过，且现有高风险点都有测试或真实脚本交叉。

## Q6: 剩余最值得继续质疑的方向
剩余风险主要是：
- 真实 production scripts 未覆盖到的极端 OAL 组合
- FreeMarker 模板与 metadata 之间的约束耦合
- 运行时 debug/source file/line table 等调试辅助契约是否足够稳定
