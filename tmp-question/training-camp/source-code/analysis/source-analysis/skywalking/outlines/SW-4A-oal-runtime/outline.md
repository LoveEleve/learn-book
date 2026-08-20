# SW-4A OAL compiler/runtime — Outline

> 模块: `oap-server/oal-grammar` + `oap-server/oal-rt`
> 日期: 2026-08-18

## 1. 域定位
`SW-4A` 是 SkyWalking 的 OAL 编译/runtime 内核：
- grammar 定义 OAL 语言
- parser 解析 `.oal`
- enricher 把 parser model 补齐为代码生成模型
- generator 产出 metrics / builder / dispatcher 动态类

它不是 query API，不是 storage，不是 MAL/LAL；是观测指标规则 DSL 的 runtime 编译链。

## 2. 主链拆解
### 2.1 Grammar / Parser
- `oal-grammar` 提供 `OALLexer.g4` / `OALParser.g4`
- `OALScriptParserV2` 创建 lexer/parser，挂上 `OALErrorListener`
- `ParseTreeWalker` 驱动 `OALListenerV2`
- 输出不可变 `MetricDefinition`

### 2.2 Enricher
`MetricDefinitionEnricher` 负责：
- 找 metrics function class
- 找 source columns
- 找 `@Entrance` method
- 拼 entrance method args expression
- 收集 persistent fields
- 生成 filter/template 所需 metadata

### 2.3 Generator
`OALClassGeneratorV2` 负责：
- metrics class
- metrics builder class
- dispatcher class
- 通过 FreeMarker template + Javassist 编译并加载

## 3. 语言能力边界
当前 OAL filter grammar 支持：
- `== != > < >= <=`
- `like`
- `in`
- `contain / not contain`
- number / string / boolean / enum / null / array
- map attribute：`tag["key"]`
- source attr cast / function arg cast
- decorator

但需要明确：
- 这不是一般递归布尔表达式语言
- `expression` 是固定匹配模式集合，而不是任意逻辑组合 AST

## 4. 当前测试现实
`oal-rt` 当前已有约 **98** 个官方测试通过，覆盖：
- parser happy path
- parsing error
- real OAL scripts
- generator integration
- production scripts
- runtime generation
- filter / function / model 语义

现有测试已经覆盖多个高风险边界：
- `>= <= like in contain not contain`
- map expression
- nested boolean accessor
- array `in`
- decorator
- source cast 与 numeric cast

## 5. 当前结论
本轮未发现直接源码 defect。

与 `SW-3E`、`SW-3F` 相比，`SW-4A` 的特点是：
- 机制更复杂
- 测试也更密
- 因此风险更可能在“少量边缘组合未被真实脚本覆盖”，而不是显式漏实现

## 6. 外域边界
- `SW-4A`：只关心 OAL compile/runtime
- `SW-4B`：MQE grammar/runtime 是另一条表达式链
- `SW-4C`：Query plugin/interface 是消费层，不并入本域
- `SW-5`：generated metrics 的落库语义不在本域深展开

## 7. 验证命令
已验证：
```bash
./mvnw -pl oap-server/oal-rt -am test
```

结果：`oal-grammar` + `oal-rt` 完整 reactor 回归通过，`oal-rt` 测试 **98/98 PASS**。

## 8. 收敛判断
当前 `SW-4A` 已达到“可交接的第一阶段收敛”：
- 主链清晰
- grammar / parser / enricher / generator 分层清楚
- 已知高风险点多数已被官方测试覆盖
- 暂无已确认源码缺陷

后续若继续深挖，最合理方向是挑选 production script 与模板交界的少数负面空间做 targeted probing，而不是盲目大改。 
