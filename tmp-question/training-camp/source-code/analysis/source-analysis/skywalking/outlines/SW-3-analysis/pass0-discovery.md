# SW-3 Analysis Pipeline — Pass 0 发现

> 首批深读: `AnalyzerModuleProvider.java` + analyzer 包/ server-core analysis 包数字盘点
> 日期: 2026-08-17

## 1. 首轮数字
- `oap-server/analyzer/` Java 文件：**191**
- `server-core/.../analysis/` Java 文件：**192**
- analyzer 不只是一个 trace parser，还包括：
  - agent trace analyzer
  - event analyzer
  - log/LAL analyzer
  - meter/MAL analyzer
  - hierarchy compiler
  - gen-AI analyzer

## 2. Analysis Pipeline 边界
SW-3 需要拆成几个机制层，而不是把 383+ 文件一次性吞并：

- Trace segment analysis / listener pipeline
- Meter/MAL analysis
- Log/LAL analysis
- Event analysis
- Hierarchy / relation / manual analysis
- Gen-AI analyzer

`server-core/analysis/manual` 是产出/聚合模型的消费侧，应与 analyzer listener 链交叉引用但不重复归属。

## 3. AnalyzerModuleProvider 主链
- module name = AnalyzerModule，provider name = default
- prepare:
  - 创建 DB/cache threshold watcher
  - 创建 uninstrumented gateway config
  - 创建 trace sampling watcher
  - 创建 `SegmentParserServiceImpl`
  - 注册 `ISegmentParserService`
  - 加载 meter config
  - 创建/注册 `IMeterProcessService`
- start:
  - 从 CoreModule 获取 `OALEngineLoaderService`，加载官方 Core OAL
  - 从 ConfigurationModule 获取 DynamicConfigurationService，注册多个 config watcher
  - 构建 listener manager 并注入 segment parser
  - 启动 meter process
- requiredModules:
  - Telemetry
  - Core
  - Configuration

## 4. Trace listener pipeline
- `listenerManager()` 根据 `traceAnalysis` 开关：
  - 可选 RPCAnalysisListener
  - 可选 cross-thread endpoint dependency listener
  - 可选 network alias listener
  - 始终 SegmentAnalysisListener
  - 始终 VirtualServiceAnalysisListener
- 结论：trace analysis 不是单个 parser，而是**配置开关控制的 listener factory pipeline**

## 5. 初步跨域链
`AnalyzerModuleProvider.start`
→ Core OAL engine load
→ DynamicConfiguration watcher registration
→ SegmentParser listener manager
→ MeterProcessService

因此 SW-3 同时依赖 SW-2 Module SPI、Core OAL、Configuration、Telemetry，不应孤立分析 listener。

## 6. 待 Pass 1/2
- Q1: SegmentParserServiceImpl 如何遍历 segment/span 并调用 listener
- Q2: listener 顺序是否影响 endpoint/relation/virtual service 结果
- Q3: traceAnalysis=false 时哪些 listener 被彻底排除，哪些仍始终运行
- Q4: MeterProcessService/MAL 与 OAL 的输入输出边界
- Q5: config watcher 变更是否影响已有 parser/listener，还是仅更新阈值状态
- Q6: analyzer 191 文件与 server-core analysis 192 文件如何避免重复归属
- Q7: LAL/MAL/OAL 三种 DSL compiler/runtime 应拆成 SW-4 还是 SW-3 扩展

## 子域拆分审计
- agent-analyzer 60 / meter-analyzer 54 / log-analyzer 52 / event-analyzer 8 / hierarchy 7 / gen-ai 10
- 六项合计 191，与 analyzer 全量 Java 数一致
- server-core analysis/manual 192 文件不重复并入，作为结果/dispatcher 消费侧
- 因此 SW-3 从单域修正为 SW-3A~SW-3F 六子域
