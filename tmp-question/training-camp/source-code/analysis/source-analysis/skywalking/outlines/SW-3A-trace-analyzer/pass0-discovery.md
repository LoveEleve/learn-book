# SW-3A Trace / Agent Analyzer — Pass 0 发现

> 模块: `oap-server/analyzer/agent-analyzer`
> 生产 Java: **53**，测试 Java: 7
> 日期: 2026-08-17

## 1. 域职责
- 接收 `SegmentObject`，按 span 类型和分析点遍历
- 通过 listener factory pipeline 生成 segment/entry/exit/local/first 分析结果
- 负责 trace sampling、segment status、searchable tag、service/endpoint ID 与 SourceReceiver 输出

## 2. 入口链
`SegmentParserServiceImpl.send(segment)`
→ 新建 `TraceAnalyzer`
→ `createSpanListeners()`
→ `notifySegmentListener`
→ 按 span 顺序 dispatch First + Entry/Exit/Local
→ `notifyListenerToBuild()`

每个 segment 新建 TraceAnalyzer/listener 实例，避免 listener 状态跨 segment 串线。

## 3. Listener SPI
- `AnalysisListener` 定义：`containsPoint(Point)` + `build()`
- 点类型：`First / Entry / Exit / Local / Segment`
- 专用子接口：`FirstAnalysisListener` / `EntryAnalysisListener` / `ExitAnalysisListener` / `LocalAnalysisListener` / `SegmentListener`
- `SegmentParserListenerManager` 维护有序 `LinkedList<AnalysisListenerFactory>`
- TraceAnalyzer 依据 `containsPoint` 判断后强制 cast 到专用子接口
- 因此 factory 的 point 声明与实现接口必须严格一致

## 4. TraceAnalyzer 顺序契约
1. 空 spans segment 直接 return
2. 创建 listener 实例
3. Segment 点先处理整段数据
4. 遍历 span：
   - spanId=0 → First
   - Exit → Exit
   - Entry → Entry
   - Local → Local
   - 其他类型 → error log
5. 所有 listener build

## 5. SegmentAnalysisListener 已见语义
- Segment 点汇总最早 start、最晚 end、错误状态、searchable tags
- duration 超 `Integer.MAX_VALUE` 会钳制
- 采样状态：UNKNOWN → sampler.shouldSample；错误且 forceSampleErrorSegment → 强制 SAMPLED；否则 IGNORE
- First/Entry 点构造 serviceId/endpointId
- build 时 IGNORE 直接返回，否则 SourceReceiver.receive(segment)
- searchable tag 同 key 去重；tag/value 超长度丢弃
- build 后额外发 `TagAutocomplete`

## 6. AnalyzerModuleProvider 交叉依赖
- Analyzer provider prepare 阶段注册 `ISegmentParserService`
- start 阶段构造 listener manager：
  - traceAnalysis 开启才加入 RPC/cross-thread/network-alias listener
  - SegmentAnalysisListener 与 VirtualServiceAnalysisListener 始终加入
- listener factories 依赖 CoreModule 的 SourceReceiver/NamingControl/ConfigService 等服务

## 7. 当前边界
- SW-3A 只深读 trace/agent parser 与 listener 主链
- meter/MAL、log/LAL、event、hierarchy、genAI 分别归 SW-3B~F
- server-core analysis/manual 是输出/dispatcher 消费侧，不重复归入 SW-3A

## 8. 待 Pass 1/2
- Q1：listener 有序列表中不同 listener 的 build/parse 顺序是否产生数据依赖
- Q2：spanId=0 规则是否始终等同 first span
- Q3：错误 segment force sampling 与 sampler 的优先级
- Q4：SegmentAnalysisListener 的 state 在一个 segment 内如何变化
- Q5：listener point 错配是否有测试防护
- Q6：空 segment/未知 SpanType 的数据丢弃与告警边界