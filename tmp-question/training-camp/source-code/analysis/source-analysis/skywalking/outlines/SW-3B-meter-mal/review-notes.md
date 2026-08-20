# SW-3B Meter / MAL Analyzer — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-18, Pass0/1 + 完整 reactor 回归)
- [x] 47 个 production Java / 7 个 test Java 数字确认
- [x] MAL compiler、AST/model、metadata、Javassist generated class、runtime Analyzer 四段链闭环
- [x] 官方测试全量回归：
  - MALClassGeneratorClosureTest 14
  - MALClassGeneratorScopeTest 9
  - MALClassGeneratorTest 20
  - MALExtensionFunctionTest 9
  - MALScriptParserTest 22
  - DSLV2Test 5
  - 合计 **79 tests，0 failure**
- [x] closure companion、safe navigation、method chain、arithmetic、enum、downsampling、extension SPI 均有测试覆盖
- [x] v1 expected data / input data 的 truth 边界已纳入 Pass0

## 关键结论
- Analyzer 只从全量 scrape map 选择 AST metadata 中引用的 sample names，再 filter，再执行 expression
- output 分成 single/labeled/histogram/histogramPercentile 四类
- generated code 不依赖运行时反射执行核心 expression；closure 通过 companion class
- OAL/LAL 不混入 MAL 域

## Review 轮次: 第二轮 (2026-08-18, 数字/边界/生成产物审计)
- [x] 生产/test 文件数字与 Pass0 一致
- [x] 79 个官方测试全部通过，包含 malformed expression / invalid filter / extension wrong namespace/method/arg count
- [x] `Analyzer` 的 sample selection 是按 metadata key lookup，不是扫描输入 map 全量执行
- [x] histogram `le`/percentile 输出与 `BucketedValues`/`PercentileArgument` 分流已核对
- [x] closure class 生成与 class output 测试已核对
- [x] 无新未归属机制；server-core meter output 作为消费侧保持边界

## 收敛判定
SW-3B 当前无已知问题；下一步转 SW-3C Log/LAL analyzer。