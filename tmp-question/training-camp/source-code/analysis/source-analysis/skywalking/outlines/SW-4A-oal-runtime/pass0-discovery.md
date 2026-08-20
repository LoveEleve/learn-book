# SW-4A OAL compiler/runtime — Pass 0 发现

> 模块: `oap-server/oal-grammar` + `oap-server/oal-rt`
> 日期: 2026-08-18

## 1. 域职责
`SW-4A` 是 SkyWalking 的 OAL 编译/runtime 链：
- `oal-grammar` 提供 ANTLR grammar
- `oal-rt` 负责 OAL script 解析、元数据 enrich、Javassist 代码生成与 runtime 类加载

它不是 query 接口层，也不是存储层；更像一个“指标规则 DSL 编译器 + runtime 生成器”。

## 2. 主链
`.oal script`
→ `OALScriptParserV2`
→ `OALListenerV2`
→ `MetricDefinition`
→ `MetricDefinitionEnricher`
→ `CodeGenModel`
→ `OALClassGeneratorV2`
→ generated metrics / builder / dispatcher classes

## 3. 模块规模与测试密度
### 主源码
- `oal-grammar`: grammar 2 个（lexer/parser）
- `oal-rt`: 27 个 Java

### 测试
已见测试 **10** 组，约 **98** 用例通过：
- parser
- parsing error
- real scripts
- generator
- production scripts
- runtime generation
- model / filter / function 等

这是目前进入的 SkyWalking 子域里测试最密集的之一。

## 4. 当前边界特征
### 4.1 parser 不是通用表达式引擎
`OALParser.g4` 的 `expression` 是显式列举：
- `== != > < >= <=`
- `like`
- `in`
- `contain / not contain`

并不是可任意递归组合的布尔表达式语法树；复杂性主要在值类型与 source attribute/path 的组合，而不是一般语言级逻辑表达式。

### 4.2 parser / model / codegen 已分层
- parser 输出不可变 `MetricDefinition`
- generator 不直接依赖 parse tree，而依赖 enrich 后的 `CodeGenModel`
- codegen 通过 FreeMarker template + Javassist 完成

### 4.3 运行时 integration 依赖 generated classes
`OALClassGeneratorV2` 会同时生成：
- metrics class
- metrics builder class
- dispatcher class

因此本域不能只看 parser，也不能只看 generator；必须按整条 compile pipeline 审视。

## 5. 已经确认有覆盖的高风险点
现有测试已经覆盖：
- wildcard source
- number / boolean / string / enum / null filter
- `>= <= like in contain not contain`
- decorator
- map attribute（`tag["key"]`）
- nested boolean accessor（`protocol.success` -> `isSuccess()`）
- array value for `in`
- source attr cast / numeric cast
- production OAL scripts 与 runtime generation

## 6. 当前首轮结论
与 `SW-3E` 不同，`SW-4A` 暂未暴露出“grammar 有、visitor 没实现”这类直接缺口。

本域当前更可能存在的问题，不在大方向漏实现，而在：
- parser → enricher 的边缘值组合
- codegen 模板与 metadata 约束的错位
- 真实 production scripts 未覆盖到的极端输入

## 7. 下一步质疑主线
Pass 1/2 继续优先质疑：
1. parser 已声明、但官方测试是否遗漏的边界运算符组合
2. enrich 阶段对 function arg / filter value / source attr cast 的错配风险
3. codegen 生成类的命名、source file、line table、debug 输出等稳定性
4. real scripts 与 production scripts 之间是否还有负面空间
