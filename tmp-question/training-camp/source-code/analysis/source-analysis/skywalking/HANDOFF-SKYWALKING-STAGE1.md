# HANDOFF-SKYWALKING-STAGE1

> 项目: `spring/skywalking`
> 仓库: `/data/workspace/source-code/code/spring/skywalking`
> 日期: 2026-08-18
> 状态: **阶段性交接文档（非全仓最终收官）**
> 方法论: `09 怀疑审计 / Pass0→Pass1→Pass2→Pass3 / harness / 多轮 review 一次性收敛`

---

## 0. 这份文档是什么，不是什么

这份文档是 **SkyWalking 当前阶段的大纲梳理交接**，不是全仓最终总交付。

已经完成并达到“可交接”状态的域：

1. `SW-1 Agent Command Model + Protocol Boundary Audit`
2. `SW-2 OAP Bootstrap / Module SPI`
3. `SW-3A Trace / Agent Analyzer`
4. `SW-3B Meter / MAL Analyzer`
5. `SW-3C Log / LAL Analyzer`
6. `SW-3D Event Analyzer`

尚未完成的域：

- `SW-3E Hierarchy compiler`
- `SW-3F GenAI analyzer`
- `SW-4 Query / MQE / OAL boundary`
- `SW-5 Storage / Persistence`
- `SW-6 Cluster / Configuration`
- `SW-7 Transport / Export / Observability`
- `SW-8 UI / Distribution / Integration boundary`

因此这份文档的正确用法是：
- 接力后续 SkyWalking 分析
- 快速理解已完成域的边界、结论、风险、构建命令
- 避免重复踩相同的 submodule / reactor / 大域吞并问题

而不应该把它误读成：
- “SkyWalking 全部核心机制已看完”
- “所有 proto / 所有插件 / 所有 OAP 子系统已收官”

---

## 1. 仓库级总览

### 1.1 根模块结构

当前已确认 SkyWalking 主结构为：

- `apm-protocol/`
- `oap-server/`
- `apm-webapp/`
- `apm-dist/`
- `skywalking-ui/`
- `test/`
- `docs/`
- `tools/`

本阶段主战场是：
- `apm-protocol/`
- `oap-server/`

`skywalking-ui / apm-webapp / apm-dist` 当前只做边界识别，尚未进入源码深审。

### 1.2 Git submodule 现实

本仓库中存在多个 submodule。当前最关键、已经实证到的事实：

- `apm-protocol/apm-network/src/main/proto` **初始是未初始化 submodule**
- 如果不初始化，`apm-network` 会因为缺少生成的 protobuf/grpc Java 依赖而编译失败

已执行的关键命令：

```bash
git submodule update --init apm-protocol/apm-network/src/main/proto
```

初始化后：
- `apm-network` 的 proto 文件可见
- protobuf/grpc generated Java 可正确生成
- `apm-network` compile 成功

这是一个必须在后续接手时优先检查的现实前提。

### 1.3 proto 分布不是单域

当前数字穷举结论：

- 全仓 proto：**53**
- `apm-protocol/apm-network`：**26**
- 其余仍大量分布在：
  - `oap-server/server-core`
  - `server-receiver-plugin/receiver-proto`
  - `server-query-plugin/traceql-plugin`
  - `exporter`
  - `configuration`
  - `test/e2e-v2`

方法论结论：

> SkyWalking 里的“协议”不能粗暴压成一个单域。
>
> `apm-protocol` 只是通用采集协议入口的一部分；
> OAP 内部专用 proto 必须按真实消费模块回归到各自域。

---

## 2. 当前拓扑与域拆分状态

### 2.1 初始 8 域框架

当前阶段沿用的总拓扑是：

1. `SW-1 Agent Command Model + Protocol Boundary`
2. `SW-2 OAP Bootstrap / Module SPI`
3. `SW-3 Analysis Pipeline`
4. `SW-4 Query / MQE / OAL boundary`
5. `SW-5 Storage / Persistence`
6. `SW-6 Cluster / Configuration`
7. `SW-7 Transport / Export / Observability`
8. `SW-8 UI / Distribution / Integration boundary`

### 2.2 SW-3 已拆成六个子域

在数字和包边界复核后，`SW-3` 已从单一分析域拆为：

- `SW-3A Trace / Agent Analyzer`
- `SW-3B Meter / MAL Analyzer`
- `SW-3C Log / LAL Analyzer`
- `SW-3D Event Analyzer`
- `SW-3E Hierarchy compiler`
- `SW-3F GenAI analyzer`

当前已完成的是 A/B/C/D。

### 2.3 数字依据

已穷举：

- `agent-analyzer`: **60** Java（当前主链生产 53 + test 7）
- `meter-analyzer`: **54** Java（生产 47 + test 7）
- `log-analyzer`: **52** Java（生产 42 + test 10）
- `event-analyzer`: **8** Java
- `hierarchy`: **7** Java
- `gen-ai-analyzer`: **10** Java

六者合计：**191**，与 `oap-server/analyzer` 全量数字一致。

### 2.4 `server-core/analysis/manual` 的边界

`server-core/analysis/manual` 等 analysis 子包不是 analyzer 子域的重复部分，而是：
- 结果模型
- dispatcher
- stream processor
- 聚合/存储消费侧

因此当前明确规则：

> `server-core/analysis/manual` 不重复并入 SW-3A~F。
>
> 它们在后续按“具体消费方向”与对应 analyzer 子域交叉引用。

否则会导致域重复归属和机制双重统计。

---

## 3. 已完成域详述

---

## 3.1 SW-1 Agent Command Model + Protocol Boundary Audit

### 3.1.1 这个域为什么不是“完整协议域”

初看 SkyWalking 很容易把 `apm-protocol` 直接当成完整协议域。

但当前真实情况是：
- `apm-protocol/apm-network` 当前可见 command model + 通用采集 proto
- 其余大量 proto 位于 OAP 自身 receiver/query/exporter/configuration 模块

所以 SW-1 的准确职责是：
- command envelope
- 当前 `apm-protocol` 的通用采集协议边界
- 为后续 proto 回归建立拆分依据

### 3.1.2 数字与构建

- `apm-protocol/apm-network` Java：**17**（main 16 + test 1）
- `apm-protocol/apm-network/src/main/proto`：**26** proto
- 初始化 submodule 后，`apm-network` 编译总 source file：**335**（含 generated protobuf/grpc Java）

已验证命令：

```bash
./mvnw -pl apm-protocol/apm-network -am -DskipTests compile
```

### 3.1.3 Command envelope 主链

核心类型：
- `BaseCommand`
- `Serializable`
- `Deserializable<T>`
- `CommandDeserializer`

`BaseCommand` 统一负责：
- `command`
- `serialNumber`
- `Command.Builder`
- 自动注入 `SerialNumber` 作为 argument

这意味着 command 有统一 envelope，而不是每个 command 各自手工拼 protobuf。

### 3.1.4 反序列化范围

`CommandDeserializer` 当前显式支持 4 个反序列化 command：

- `ProfileTaskCommand`
- `ConfigurationDiscoveryCommand`
- `AsyncProfilerTaskCommand`
- `PprofTaskCommand`

其他 command 目前只承担 OAP→agent 下发：

- `ContinuousProfilingPolicyCommand`
- `ContinuousProfilingReportCommand`
- `EBPFProfilingTaskCommand`
- `TraceIgnoreCommand`

方法论结论：

> 这不是遗漏，而是“反向解析职责边界”。
>
> 不能把“没出现在 CommandDeserializer”误判成漏分析。

### 3.1.5 消费方定位

已 grep 定位：

- `ProfileTaskCommand` → server-core CommandService + profile receiver
- `AsyncProfilerTaskCommand` → server-core CommandService + async profiler receiver
- `PprofTaskCommand` → server-core CommandService + pprof receiver
- `ConfigurationDiscoveryCommand` → configuration discovery receiver
- `ContinuousProfiling*` / `EBPF*` → 对应 profiling/ebpf receiver

所以 SW-1 不继续前吞这些 receiver 逻辑。

### 3.1.6 harness 与结论

已写 `MiniSW1`，验证：
- 4 个 round-trip command
- 4 个 serialize-only command
- unknown command reject

结果：**9/9 PASS**

当前结论：
- submodule 初始化问题已解决
- command envelope 边界清晰
- command model 与消费方边界已识别
- 无已知源码问题

---

## 3.2 SW-2 OAP Bootstrap / Module SPI

### 3.2.1 它是 OAP 的内核

SkyWalking OAP 的真正骨架不是 Spring Boot 自动装配，而是自定义模块系统：
- `ModuleDefine`
- `ModuleProvider`
- `ModuleManager`
- `BootstrapFlow`

几乎所有 receiver/analyzer/storage/query/plugin 都建立在它之上。

### 3.2.2 `ModuleManager.init` 主链

真实流程：

1. 读取 `ApplicationConfiguration.moduleList()`
2. `ServiceLoader<ModuleDefine>` 发现所有模块定义
3. 只处理配置启用的模块
4. 对每个命中模块执行 `ModuleDefine.prepare(...)`
5. 准备阶段结束后，把 `isInPrepareStage` 置 false
6. 配置里还有未匹配模块名 → `ModuleNotFoundException`
7. 建立 `BootstrapFlow`
8. 依赖拓扑排序后执行 provider `requiredCheck()` + `start()`
9. 全部完成后统一 `notifyAfterCompleted()`

### 3.2.3 `ModuleDefine.prepare`

职责：
- 发现 provider
- 保证一个 module 最终只绑定一个 provider
- 通过 `ConfigCreator` 反射创建 config bean
- `copyProperties` 从 module/provider 配置写入 bean
- 调用 provider.prepare()

关键异常边界：
- `ProviderNotFoundException`
- `DuplicateProviderException`
- `ModuleConfigException`

### 3.2.4 `ModuleProvider.requiredCheck`

这是一个容易忽略的双向检查：
- required service 缺任何一个都不行
- 实现数量也不能多于 `ModuleDefine.services()` 声明数量

也就是说：

> 不是“只要把需要的 service 补齐就行”，
> 多实现同样会报错。

### 3.2.5 `BootstrapFlow` 依赖排序

- 先检查 requiredModules 是否都已加载
- 再做迭代式拓扑排序
- 一轮没有 provider 被移除 → `CycleDependencyException`

额外边界：
- `loadedModules` 来自 `HashMap`
- 所以**同层 provider 的相对顺序不是稳定契约**
- 但依赖约束仍然可靠

### 3.2.6 SPI 现实规模

已穷举：
- ModuleDefine 服务文件：**38**
- ModuleProvider 服务文件：**59**

因此后续所有域都必须同时看：
- 配置启用模块
- ServiceLoader 可见 define/provider
- provider name 与 module() 匹配关系

### 3.2.7 构建教训与修正

一开始如果直接测 `library-module`，会因为缺少 `library-util` 而阻塞。

正确命令：

```bash
./mvnw -pl oap-server/server-library/library-module -am test
```

结果：
- `library-util`: **50 tests PASS**
- `library-module`: **5 tests PASS**

覆盖了：
- 正常 init
- config transport
- module missing
- cycle dependency

当前结论：
- SW-2 已收敛
- 无已知源码问题

---

## 3.3 SW-3A Trace / Agent Analyzer

### 3.3.1 入口链

主链：

`SegmentObject`
→ `SegmentParserServiceImpl.send()`
→ new `TraceAnalyzer`
→ create listeners
→ Segment
→ First
→ Entry/Exit/Local
→ build
→ `SourceReceiver`

listener 是**每个 segment 新建**，不跨 segment 复用。

### 3.3.2 listener SPI

- `AnalysisListener.containsPoint(Point)`
- `build()`
- 点类型：`First / Entry / Exit / Local / Segment`
- TraceAnalyzer 根据 point 决定 cast 到具体子接口

这意味着：

> `containsPoint` 与子接口实现必须严格匹配，
> 否则会在 runtime cast 处失败。

### 3.3.3 `SegmentAnalysisListener` 核心职责

负责：
- 起止时间汇总
- duration 计算
- error 聚合
- searchable trace tags
- serviceId / endpointId
- `Segment` 记录输出
- `TagAutocomplete` 输出

关键行为：
- duration 超 int → 钳制到 `Integer.MAX_VALUE`
- `forceSampleErrorSegment` 可覆盖 sampler
- searchable tag 去重、超长丢弃
- sample IGNORE 后，后续阶段快速返回

### 3.3.4 已发现并修复的真实缺陷

原实现：

```java
Math.abs(traceId.hashCode()) % 10000
```

缺陷：
- `Math.abs(Integer.MIN_VALUE)` 仍为负数
- 极端 traceId hash 会生成负 sample
- 进而错误命中采样条件

已修复为：

```java
Math.floorMod(traceId.hashCode(), 10000)
```

并补测试：
- 使用 `"polygenelubricants"`（其 hash 正好是 `Integer.MIN_VALUE`）
- 验证 rate=0 时不会被错误采样

### 3.3.5 回归

命令：

```bash
./mvnw -pl oap-server/analyzer/agent-analyzer -am -Dtest=TraceSamplingPolicyWatcherTest -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：
- `TraceSamplingPolicyWatcherTest`: **8/8 PASS**
- 完整 reactor 构建成功

当前结论：
- SW-3A 已收敛
- 该 sampling 极端值缺陷已修复

---

## 3.4 SW-3B Meter / MAL Analyzer

### 3.4.1 它不是简单 parser，而是 4 段链

MAL 主链：

`MAL string`
→ `ANTLR`
→ `MALExpressionModel`
→ `MALMetadataExtractor`
→ `MALClassGenerator`
→ generated `MalExpression`
→ `Analyzer`
→ `MeterSystem`

所以它同时是：
- compiler
- metadata extractor
- generated runtime
- analyzer output pipeline

### 3.4.2 `Analyzer` 的关键运行方式

- 不扫描全量 sample family 做表达式解释执行
- 而是只选 metadata 中声明引用的 sample names
- 再应用 filter
- 再执行生成的 expression
- 再按 metric type 输出：
  - single
  - labeled
  - histogram
  - histogramPercentile

### 3.4.3 DSL 能力

已实证覆盖：
- arithmetic
- method chain
- enum ref
- downsampling
- closure
- safe navigation
- extension SPI `namespace::method()`
- histogram / percentile

### 3.4.4 官方测试回归

完整 reactor 回归：
- `MALClassGeneratorClosureTest`: 14
- `MALClassGeneratorScopeTest`: 9
- `MALClassGeneratorTest`: 20
- `MALExtensionFunctionTest`: 9
- `MALScriptParserTest`: 22
- `DSLV2Test`: 5

合计：**79/79 PASS**

当前结论：
- SW-3B 已收敛
- 无已知源码问题

---

## 3.5 SW-3C Log / LAL Analyzer

### 3.5.1 链路

LAL 主链：

`LAL script`
→ `ANTLR`
→ `LALScriptModel`
→ `LALClassGenerator`
→ generated `LalExpression`
→ `ExecutionContext`
→ sink / listener

### 3.5.2 与 MAL 的关键区别

- MAL 输入是 meter sample family
- LAL 输入是 log/parse context
- LAL 有 parser spec：json / text / yaml
- LAL 还支持 output field assignment、sink sampler、rate limit、interpolated ID

### 3.5.3 官方测试回归

- `LALClassGeneratorBasicTest`: 10
- `ConditionTest`: 17
- `DefTest`: 13
- `ExtractorTest`: 10
- `SinkTest`: 5
- `ExpressionExecutionTest`: 37
- `ScriptParserTest`: 25
- `DSLV2Test`: 3

合计：**120/120 PASS**

当前结论：
- SW-3C 已收敛
- 无已知源码问题

---

## 3.6 SW-3D Event Analyzer

### 3.6.1 它是很薄的 record 输出链

主链：

`EventAnalyzerServiceImpl.analyze(event)`
→ 时间兜底
→ new `EventAnalyzer`
→ create listeners
→ parse
→ build
→ `RecordStreamProcessor`

### 3.6.2 当前 listener 数量

当前 provider start 阶段只注册：
- `EventRecordAnalyzerListener.Factory`

所以本域目前不是复杂 fan-out，而是单 listener 输出链。

### 3.6.3 `EventRecordAnalyzerListener` 做什么

- 复制 proto Event 到 `server-core.analysis.record.Event`
- 通过 `NamingControl` 规范化 service/serviceInstance/endpoint
- 非空 parameters map 用 Gson 转 JSON
- `timeBucket/timestamp` 优先用 startTime，否则用 endTime
- build 阶段调用 `RecordStreamProcessor.getInstance().in(event)`

当前结论：
- SW-3D 已完成 Pass 0
- 尚未做进一步深审与测试交叉
- 当前只可视为“边界梳理完成”，未达到 A/B/C 同级收敛度

---

## 4. 当前统计

### 4.1 已完成域

| 域 | 结果 |
|---|---|
| SW-1 | 9/9 harness PASS |
| SW-2 | 50 + 5 官方测试 PASS |
| SW-3A | 8/8 官方测试 PASS + sampling 修复 |
| SW-3B | 79/79 官方测试 PASS |
| SW-3C | 120/120 官方测试 PASS |
| SW-3D | Pass 0 完成，待进一步深审 |

### 4.2 已确认并修复的问题

| 位置 | 问题 | 状态 |
|---|---|---|
| `TraceSegmentSampler` | `Math.abs(Integer.MIN_VALUE)` 负值采样边界 | 已修复为 `Math.floorMod(...)` |
| `apm-network` | proto submodule 未初始化导致编译失败 | 已通过 `git submodule update --init ...` 修复 |
| `library-module` | 单模块测试缺 `library-util` 依赖 | 已通过 `-am` 修正构建方式 |

### 4.3 当前仍未完成的事情

- SW-3D 的 Pass 1/2/3 + review 收敛
- SW-3E / SW-3F
- SW-4 / SW-5 / SW-6 / SW-7 / SW-8
- SkyWalking 总交接文档 `HANDOFF-SKYWALKING.md`

---

## 5. 关键方法论经验（这次最容易重踩的坑）

### 5.1 不能只看目录名
`apm-protocol` 看起来像协议总域，但实际上：
- proto 还分布在 OAP receiver/query/exporter/configuration
- 必须做全仓 proto 穷举后再归域

### 5.2 submodule 没初始化时，目录事实会误导你
如果不先初始化：
- `apm-network` 看起来像“只有 16 个 Java command model”
- 实际这是**不完整 checkout**，不是协议本身真的这么小

### 5.3 reactor Maven 命令必须带 `-am`
单独测试子模块时，很容易误判“源码问题”，但其实是前置依赖模块没一起构建。

### 5.4 不要把 analysis/manual 和 analyzer 重复归域
`server-core/analysis/manual` 是消费侧，不是 analyzer 本体。

### 5.5 看到“日志 ERROR”不要立即判定测试失败
像 `TraceSamplingPolicyWatcherTest` 会在非法 YAML 场景里打印 error log，但断言仍可能是 PASS。

---

## 6. 交付物路径

当前已生成：

- `skywalking/SKYWALKING-PLAN.md`
- `skywalking/outlines/SW-1-agent-command/`
  - `pass0-discovery.md`
  - `pass2-questions.md`
  - `outline.md`
  - `review-notes.md`
- `skywalking/harness/SW-1/MiniSW1.java`
- `skywalking/outlines/SW-2-bootstrap/`
  - `pass0-discovery.md`
  - `outline.md`
  - `review-notes.md`
- `skywalking/outlines/SW-3-analysis/pass0-discovery.md`
- `skywalking/outlines/SW-3A-trace-analyzer/`
  - `pass0-discovery.md`
  - `pass2-questions.md`
  - `outline.md`
  - `review-notes.md`
- `skywalking/outlines/SW-3B-meter-mal/`
  - `pass0-discovery.md`
  - `outline.md`
  - `review-notes.md`
- `skywalking/outlines/SW-3C-log-lal/`
  - `pass0-discovery.md`
  - `outline.md`
  - `review-notes.md`
- `skywalking/outlines/SW-3D-event-analyzer/pass0-discovery.md`

---

## 7. 下一步建议（唯一主线）

从当前进度继续，唯一推荐主线是：

1. 先把 `SW-3D Event Analyzer` 做完 Pass 1/2/3 并收敛
2. 再继续 `SW-3E Hierarchy compiler`
3. 然后 `SW-3F GenAI analyzer`
4. 分析管线 SW-3 全部收口后，再进入 `SW-4 Query / MQE / OAL boundary`

原因：
- 现在最自然的依赖主线仍然在 analyzer 体系内部
- 先收完整个 SW-3，再切到 query/storage/cluster，拓扑最清晰

如果后续有人接手，务必先读：
- `SKYWALKING-PLAN.md`
- `SW-1 review-notes`
- `SW-2 review-notes`
- `SW-3A/B/C review-notes`

它们已经记录了最关键的边界和已踩过的构建坑。