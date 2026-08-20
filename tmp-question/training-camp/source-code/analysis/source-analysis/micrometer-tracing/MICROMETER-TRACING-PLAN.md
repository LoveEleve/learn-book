# Micrometer Tracing 源码分析计划 (阶段 6.2)

> 仓库: `/data/workspace/source-code/code/spring/micrometer-tracing`
> 日期: 2026-08-17
> 方法论: 09 怀疑审计 / Pass0→Pass1→Pass2→Pass3 / harness 费曼验证 / 多轮 review 收敛

## 0. 仓库边界审计结论

本仓库不是单一 core module，而是：

- `micrometer-tracing/`：抽象 API、Observation handler、传播/线程上下文、注解切面、exporter 抽象
- `micrometer-tracing-bridges/micrometer-tracing-bridge-brave/`：Brave 适配
- `micrometer-tracing-bridges/micrometer-tracing-bridge-otel/`：OpenTelemetry 适配
- `micrometer-tracing-reporters/micrometer-tracing-reporter-wavefront/`：Wavefront 出口
- `micrometer-tracing-tests/`：simple test double、assertion、integration setup

初版“6 域”过粗：把 Brave/Otel 合并、把 reporter 隐含进 exporter，无法保证两个实现桥的差异和 reporter 的重试/批处理语义被独立审计。已修正为 **8 个域**。

## 1. 修正后的完整拓扑（8 域）

| 域 | 名称 | 主战场 | 边界 |
|---|---|---|---|
| MT-1 | Span/Tracer 核心抽象 | `Span` / `Tracer` / `TraceContext` / `CurrentTraceContext` / `Baggage*` / `ScopedSpan` / `SpanAndScope` / `ThreadLocalSpan` / `Link` | tracing 基础契约与 scope 语义 |
| MT-2 | 传播与上下文适配 | `propagation/Propagator` / `contextpropagation/*` / reactor baggage | header inject/extract、Observation/ThreadLocal/ Reactor 传播 |
| MT-3 | Observation→Tracing 处理链 | `TracingObservationHandler` / `DefaultTracingObservationHandler` / sender/receiver handler / `RevertingScope` / tracing-aware meter handler | 消费 Micrometer Observation 生命周期并创建/管理 span |
| MT-4 | 注解切面 | `annotation/SpanAspect` / `NewSpan` / `ContinueSpan` / `SpanTag*` / invocation processors | 注解到 span builder、参数 tag、异常/返回值语义 |
| MT-5 | Exporter 抽象与过滤 | `exporter/FinishedSpan` / `SpanReporter` / `SpanFilter` / `SpanExportingPredicate` / `SpanIgnoringSpanExportingPredicate` / `TestSpanReporter` | finished span 模型、过滤、导出 predicate、reporter 契约 |
| MT-6 | Brave Bridge | `bridge-brave/` 全部生产代码：Tracer/Span/Builder/Scope/Baggage/Propagator/handlers/sampler | Brave API 到 Micrometer Tracing 抽象的落地 |
| MT-7 | OpenTelemetry Bridge | `bridge-otel/` 全部生产代码：Tracer/Span/Builder/Scope/Baggage/Propagator/Exporter/Processor/listener | OTel API/SDK 到 Micrometer Tracing 抽象的落地 |
| MT-8 | Wavefront Reporter | `reporter-wavefront/`：`WavefrontSpanHandler` / `WavefrontOtelSpanExporter` / `WavefrontBraveSpanHandler` / `SpanMetrics` | Wavefront 格式、span handler/exporter、metrics 聚合 |

### 生产文件覆盖复核
- MT-1 覆盖 root API：`Span/Tracer/TraceContext/CurrentTraceContext/Baggage*` + `ScopedSpan/SpanAndScope/ThreadLocalSpan/Link/SpanCustomizer/SpanName/SpanNamer`
- MT-2 覆盖 `propagation/` 全部 + `contextpropagation/` 全部（含 reactor 子包）
- MT-3 覆盖 `handler/` 全部 7 个生产文件
- MT-4 覆盖 `annotation/` 全部 14 个生产文件，不只 `SpanAspect`
- MT-5 覆盖 `exporter/` 全部 7 个生产文件
- MT-6 覆盖 Brave bridge 的 `bridge/`、`propagation/`、`sampler/` 全部生产文件
- MT-7 覆盖 OTel bridge 的 `bridge/`、`propagation/` 全部生产文件
- MT-8 覆盖 Wavefront reporter 全部 4 个生产文件
- production Java 文件总量（core + 两 bridge + reporter）已穷举为 **115**，上述 8 域有归属，无生产包遗漏

### 支撑包的明确挂靠关系
- `internal/` 4 文件：`EncodingUtils` → MT-1（trace/span id 编码）；`SpanNameUtil/DefaultSpanNamer` → MT-4（注解/方法命名）；其余内部工具随实际消费域复核，不独立成运行时域
- `docs/` 3 文件：`SpanDocumentation/AnnotationSpanDocumentation/EventValue` → MT-4（注解/文档化 span 语义）与 MT-1（span event 契约）交叉引用
- `tests/simple`：只作为 harness/test double，不冒充生产实现；其行为若与抽象契约冲突，必须记录为 test-double 限制（MT-1 已出现）
- `integration-test`：用于跨 bridge/reporter 回归，不独立成域

## 2. 执行顺序（修正后）

1. MT-1 Span/Tracer 核心抽象
2. MT-2 传播与上下文适配
3. MT-3 Observation→Tracing 处理链
4. MT-4 注解切面
5. MT-5 Exporter 抽象与过滤
6. MT-6 Brave Bridge
7. MT-7 OpenTelemetry Bridge
8. MT-8 Wavefront Reporter

理由：
- MT-6/7 必须在抽象、传播、handler、exporter 契约明确后分别审计
- Brave 与 OTel 的同名职责不能合并，否则会丢失 SDK 生命周期、采样、baggage、exporter 差异
- Wavefront 消费 exporter/bridge 产物，最后处理

## 3. 当前状态

- [x] 域审计完成：初版 6 域 → 修正为 8 域
- [x] MT-1 Pass 0：核心 7 文件 + 3 个 simple test-double 深读
- [x] MT-1 Pass 0/1/2/3 + harness 27/27 + review 2 轮
- [x] MT-2 Pass 0/1/2/3 + harness 17/17 + review 2 轮
- [x] MT-3 Pass 0/1/2/3 + harness 16/16 + review 2 轮
- [x] MT-4 Pass 0/1/2/3 + harness 14/14 + review 2 轮
- [x] MT-5 Pass 0/1/2/3 + harness 12/12 + review 2 轮
- [x] MT-6 Pass 0 + review 2 轮（Brave bridge风险边界已记录）
- [x] MT-7 Pass 0/1/2 + targeted Gradle回归 + review 5 轮（Boolean list类型风险已确认）
- [x] MT-8 Pass 0/1 + targeted test + outline/review
- [ ] HANDOFF-MICROMETER-TRACING 总交接 review
- [ ] `HANDOFF-MICROMETER-TRACING.md`

## 4. 域规划审查结论

- [x] 初版 6 域发现过粗：Brave/Otel 合并会掩盖实现差异；已拆成 MT-6/MT-7
- [x] reporter 不再隐含进 exporter；Wavefront 独立为 MT-8
- [x] `annotation/`、`handler/`、`contextpropagation/` 全包归属已补齐
- [x] production 115 个 Java 文件已按包边界穷举，无重复归属、无未归属生产包
- [x] tests/simple、integration-test、docs/internal 作为支撑/边界，不冒充生产域

## 5. MT-1 首批待验证问题
- Q1: `ScopedSpan` / `SpanAndScope` / `ThreadLocalSpan` 的职责边界
- Q2: `abandon()` 与 `end()` 在 bridge 实现中的差异
- Q3: `CurrentTraceContext.maybeScope()` 的实现和测试语义
- Q4: `BaggageManager.getAllBaggage(traceContext)` 的 bridge 重写契约
- Q5: `Span.Builder.addLink(...)` 是否被 Brave/Otel 都落地
- Q6: `currentSpanCustomizer()` 为 null 与 `currentSpan()==null` 的关系

## 6. 成果要求
- 每域: `pass0-discovery.md` / `outline.md` / `review-notes.md` / `Mini* harness`
- 每域至少 2 轮深审；发现新问题就继续加轮次
- 全阶段: `HANDOFF-MICROMETER-TRACING.md`
