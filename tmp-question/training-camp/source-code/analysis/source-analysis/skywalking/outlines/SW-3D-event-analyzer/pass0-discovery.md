# SW-3D Event Analyzer — Pass 0 发现

> 模块: `oap-server/analyzer/event-analyzer`
> 生产 Java: **8**
> 日期: 2026-08-18

## 1. 域职责
- 处理 `org.apache.skywalking.apm.network.event.v3.Event`
- 将事件数据转换成 `server-core` 的 `analysis.record.Event` 记录模型
- 当前是一个非常薄的 analyze→listener→record pipeline

## 2. 入口链
`EventAnalyzerServiceImpl.analyze(event)`
→ 若 start/end time 都无效则补当前时间
→ new `EventAnalyzer(moduleManager, this)`
→ `createListeners()`
→ `notifyListener(event)`
→ `notifyListenerToBuild()`

和 SW-3A 一样，每个输入 event 都会创建新的 analyzer + listener 实例，不跨 event 复用。

## 3. ModuleProvider
- module = `EventAnalyzerModule`
- provider name = `default`
- `prepare()`：创建 `EventAnalyzerServiceImpl` 并注册 `EventAnalyzerService`
- `start()`：只注册一个 listener factory：`EventRecordAnalyzerListener.Factory`
- requiredModules 仅依赖 `CoreModule`
- 结论：SW-3D 当前没有复杂 SPI fan-out，本质上是单 listener 输出链

## 4. EventRecordAnalyzerListener
- 内部持有一个 `server-core.analysis.record.Event` 聚合对象
- `parse(protoEvent)`：
  - layer：`Layer.nameOf(e.getLayer())`
  - uuid/name/type/message
  - source（若存在）→ service / serviceInstance / endpoint，经 `NamingControl` 规范化
  - parameters map 若非空 → Gson 序列化成 JSON 字符串
  - start/end time
  - timeBucket/timestamp 优先使用 startTime；无 startTime 时使用 endTime
- `build()`：`RecordStreamProcessor.getInstance().in(event)`

## 5. 当前边界
- 这不是 trace span 级分析，而是 event record 落库前整理
- 当前只有一个 listener；后续若增加其他 listener，则需要重新检查顺序语义
- RecordStreamProcessor 是消费侧，不在本域展开
- start/end 时间兜底逻辑在 serviceImpl 完成，不在 listener 完成

## 6. 待 Pass 1/2
- Q1: 两个时间都 <=0 时补当前时间，是否会产生 start=end 同毫秒的事件记录
- Q2: startTime>0 与 endTime>0 但 start>end 的异常输入是否允许直接透传
- Q3: EventRecordAnalyzerListener 对 source 缺失、parameters 空 map、message/type 空串的行为
- Q4: RecordStreamProcessor 单例输入是否有官方测试覆盖
- Q5: Event analyzer 是否还有隐藏 listener 工厂或未来扩展点