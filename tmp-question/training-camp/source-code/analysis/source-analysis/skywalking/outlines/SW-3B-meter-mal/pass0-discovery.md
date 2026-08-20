# SW-3B Meter / MAL Analyzer — Pass 0 发现

> 模块: `oap-server/analyzer/meter-analyzer`
> 生产 Java: **47**，测试 Java: 7
> 日期: 2026-08-18

## 1. 域职责
- 将 MAL（Meter Analysis Language）表达式编译成运行时 `MalExpression`
- 解析输入 `SampleFamily`，执行 filter/聚合/运算/downsampling/histogram/percentile
- 将结果送入 `MeterSystem` 生成 OAP meter metrics

## 2. 编译链
`MAL expression string`
→ ANTLR lexer/parser (`MALScriptParser`)
→ immutable `MALExpressionModel`
→ `MALMetadataExtractor`
→ `MALClassGenerator`
→ Javassist 生成 main/closure class
→ `MalExpression.run(Map<String,SampleFamily>)`

运行时生成代码使用变量级表达式链，不通过反射执行核心表达式。

## 3. Analyzer 主链
- `Analyzer.build(...)`:
  - filter string → `FilterExpression`
  - `DSL.parse(...)`
  - expression.parse() → `ExpressionMetadata`
  - new Analyzer + init/register meter
- `analyse(sampleFamilies)`:
  - 只从全量 scrape map 中选表达式引用的 sample names
  - 先应用 filter
  - expression.run(input)
  - 成功结果按 metric type 写入 MeterSystem

## 4. 输出类型
- single → `AcceptableValue<Long>`
- labeled → `DataTable`
- histogram → `BucketedValues`
- histogramPercentile → `PercentileArgument`
- 另有 service/relation/traffic 等自动生成 traffic 输出

## 5. DSL 能力边界
- `SampleFamily` method chain：sum/avg/rate/downsampling/service 等
- arithmetic binary expression
- enum ref（Layer）
- safe navigation filter
- closure（tag/forEach/serviceRelation 等）
- extension SPI：`namespace::method()`
- 运行时通过 companion classes 承载 closure，避免 Javassist 不支持 lambda/anonymous class

## 6. Metadata 与生成类
- metadata 在 AST 静态提取，不执行 dry-run
- metadata 包含 sample names、scope type、aggregation labels、downsampling、histogram/percentile 信息
- class name 来自 yaml source + line + rule name；无 hint 时 fallback 全局计数
- debug 开关可把生成 class 输出到 `mal-rt/`

## 7. 测试边界
- MALClassGeneratorTest 覆盖：
  - simple/method-chain/arithmetic/enum/downsampling/closure
  - source generation
  - filter safe navigation
  - malformed/empty expression
- analyzer test 还覆盖 parser/generator/extension/DSL 编译与运行
- 输入/expected data 以 v1 Groovy engine 结果为 truth，不能把空输出当通过

## 8. 当前待 Pass 1/2
- Q1：metadata 提取与生成 run 方法对 sample 缺失/空 SampleFamily 的一致性
- Q2：Analyzer 的 input selection 是否确实避免扫描全量样本 map
- Q3：histogram bucket `le` 排序/Long.MIN_VALUE/percentile 转换
- Q4：closure companion class 生命周期与 classloader 泄漏
- Q5：extension SPI 静态方法/首参 SampleFamily/参数类型校验
- Q6：MAL 编译错误与运行时 Result failure 的边界
- Q7：Analyzer 输出 single/labeled/histogram 的 scope/entity/traffic 归属

## 9. 初步边界结论
SW-3B 不是单纯“解析器”：它是 **DSL compiler + generated runtime + metadata-driven analyzer + MeterSystem output** 四段链。OAL/LAL 不归入本域，避免 DSL 混域。