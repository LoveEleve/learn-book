# SW-3C Log / LAL Analyzer — Pass 0 发现

> 模块: `oap-server/analyzer/log-analyzer`
> 生产 Java: **42**，测试 Java: **10**，ANTLR grammar: **2**
> 日期: 2026-08-18

## 1. 域职责
- 这是 Log Analysis Language (LAL) 的 compiler/runtime/analyzer 域
- 作用是把日志输入（text/json/yaml 或直接 inputType）通过规则脚本转换成：
  - traffic / record / metrics sink
  - 过滤/采样/字段提取
- 与 SW-3B MAL 相似，但输入是 log 事件，不是 meter sample family

## 2. 结构分层
- compiler:
  - `LALScriptParser`
  - `LALScriptModel`
  - `LALClassGenerator`
  - `LALBlockCodegen` / `LALDefCodegen` / `LALValueCodegen`
- runtime:
  - `dsl/ExecutionContext`
  - `LalExpression`
  - `LalRuntimeHelper`
- spec:
  - parser spec（Json/Text/Yaml）
  - filter spec
  - sink spec
  - sampler spec
  - extractor spec
- provider/log:
  - `LogAnalyzerModuleProvider`
  - `LogAnalyzerFactory`
  - `LogAnalyzerServiceImpl`
  - analysis/sink listener factory

## 3. 编译链
LAL script
→ ANTLR4 (`LALLexer.g4` / `LALParser.g4`)
→ `LALScriptModel`
→ `LALClassGenerator`
→ Javassist 生成表达式类
→ `LalExpression` 运行于 `ExecutionContext`
→ listener/sink 输出

## 4. 与 MAL 的差异
- MAL 输入是 `SampleFamily` map；LAL 输入是日志对象/解析树/执行上下文
- LAL 拥有 parser spec（json/text/yaml）与 output field assignment
- LAL 的 sink 带采样与 rate limit 语义
- LAL 不处理 meter scope/entity 聚合，而处理日志字段与下游 sink 绑定

## 5. 官方测试覆盖
- `LALClassGeneratorBasicTest` 10
- `LALClassGeneratorConditionTest` 17
- `LALClassGeneratorDefTest` 13
- `LALClassGeneratorExtractorTest` 10
- `LALClassGeneratorSinkTest` 5
- `LALExpressionExecutionTest` 37
- `LALScriptParserTest` 25
- `DSLV2Test` 3
- 合计 **120 tests**

## 6. 初步边界
- `receiver-proto` 是 LAL 可消费的输入协议来源之一，但不归本域
- log analyzer 的 provider/runtime 不与 meter analyzer 的 MAL runtime 混域
- hierarchy/event/genAI 继续保留后续独立子域
