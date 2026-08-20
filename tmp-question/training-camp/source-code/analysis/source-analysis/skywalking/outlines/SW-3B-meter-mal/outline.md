# SW-3B Meter / MAL Analyzer — outline 收敛版

> 生产 Java 47 / 测试 Java 7 / 官方回归 79/79 PASS

## 主链
MAL 字符串 → ANTLR → AST model → metadata → Javassist class → MalExpression.run → Analyzer → MeterSystem

## 核心机制
- metadata 静态提取 sample names/scope/labels/downsampling/histogram/percentile
- Analyzer 只选择表达式引用的 sample families，再 filter，再执行生成 expression
- 输出：single / labeled / histogram / histogramPercentile
- closure 使用 companion class，不使用 Javassist 不支持的 lambda/anonymous class
- extension SPI 使用 namespace::method，编译期校验 namespace/method/参数
- v1 Groovy engine 是 expected data truth

## 边界
MAL 域不吞 OAL/LAL；server-core meter output 是消费侧。

## 验证
MALClassGeneratorClosure 14、Scope 9、Generator 20、Extension 9、Parser 22、DSL 5，合计 79/79 PASS。