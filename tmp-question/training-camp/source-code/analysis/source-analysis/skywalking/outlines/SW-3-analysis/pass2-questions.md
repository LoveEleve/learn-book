# SW-3 Analysis Pipeline — Pass 2 问题与收敛

## P1: listener 生命周期
- `SegmentParserServiceImpl.send()` 每个 Segment 创建新的 `TraceAnalyzer`
- `TraceAnalyzer.createSpanListeners()` 每次 analysis 初始化 listener 实例
- 因此 `analysisListeners` 虽是实例字段，但不会跨 segment 复用，避免状态串线；代价是每段重新创建 listener

## P2: 事件顺序
`TraceAnalyzer.doAnalysis` 固定顺序：
1. 空 segment 直接 return
2. 创建 listeners
3. notify Segment（全 segment 数据）
4. 按 span 顺序：spanId=0 先 First，再按 type 调 Exit/Entry/Local
5. 所有 listener build

结论：SegmentAnalysisListener 可先在 parseSegment 计算采样/时间/错误，再由 parseFirst/Entry 补 service/endpoint；build 最后输出。

## P3: sampling
- SegmentAnalysisListener 在 parseSegment 汇总最早 start、最晚 end、错误状态、searchable tags
- `duration` 超 int max 时钳制到 Integer.MAX_VALUE
- sample 状态 UNKNOWN/IGNORE 时根据 sampler 决定 SAMPLED/IGNORE
- `forceSampleErrorSegment` 可让错误 segment 强制采样
- IGNORE 状态在 parseFirst/build 阶段短路，减少后续输出

## P4: listener 点类型
- AnalysisListener.Point: First/Entry/Exit/Local/Segment
- listener 通过 containsPoint 声明兴趣点，再由 TraceAnalyzer 强制 cast 到对应子接口
- 这是一个运行时协议：containsPoint 返回错误会导致 ClassCastException；各 Factory 必须保证接口与 Point 一致

## P5: OAL/MAL/LAL 边界
- AnalyzerModuleProvider.start 负责加载 Core OAL、启动 MeterProcessService、配置 watcher
- OAL/MAL/LAL compiler/runtime 不应在 SW-3 trace listener 主域内混读
- 后续 SW-4 专门承接 DSL compiler/runtime/query engine

## P6: Pass 3 结论
- SW-3 当前主链已闭环，但 analyzer 383+ 文件不适合一次性写一个 outline
- 下一步应将 SW-3 拆成 Trace Pipeline、Meter/MAL、Log/LAL、Event、Hierarchy/Manual、GenAI 六个子域后分别 Pass0
- 当前不伪造单一 SW-3 harness；TraceAnalyzer 需要 protobuf SegmentObject + ModuleManager/Core service 测试夹具