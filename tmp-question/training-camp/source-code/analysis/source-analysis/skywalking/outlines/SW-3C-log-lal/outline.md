# SW-3C Log / LAL Analyzer — outline 收敛版

> 生产 Java 42 / 测试 Java 10 / grammar 2 / 官方回归 120/120 PASS

## 主链
LAL script → ANTLR → `LALScriptModel` → `LALClassGenerator` → `LalExpression` → `ExecutionContext` → listener/sink

## 核心机制
- parser spec：json/text/yaml
- filter condition：tag function / safe navigation / if/elseif / eq/neq / null checks
- def/value extraction：toJson / toJsonArray / cast / nested access / variable reuse
- extractor：registry/process/metrics/output field assignment
- sink：sampler / rate limit / interpolated ID / condition
- runtime：编译后表达式在 `ExecutionContext` 中执行

## 验证
- Basic 10
- Condition 17
- Def 13
- Extractor 10
- Sink 5
- ExpressionExecution 37
- ScriptParser 25
- DSLV2 3
- 合计 **120/120 PASS**

## 边界
- 输入是 log 事件/解析上下文，不是 meter SampleFamily
- receiver-proto 只提供协议输入，不属于本域
- 不与 MAL/OAL、event、genAI 混域