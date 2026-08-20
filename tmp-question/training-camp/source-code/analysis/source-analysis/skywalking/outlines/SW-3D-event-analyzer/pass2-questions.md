# SW-3D Event Analyzer — Pass 2 问题收敛

> 模块: `oap-server/analyzer/event-analyzer`
> 日期: 2026-08-18

## Q1: 两个时间都 <=0 时补当前时间，是否会产生 start=end 同毫秒的事件记录
是。`EventAnalyzerServiceImpl.analyze(...)` 在 `startTime<=0 && endTime<=0` 时分别调用两次 `System.currentTimeMillis()` 写回 start/end，但同一次 analyze 里大多数情况下会落在同毫秒；本次单测以“相等”为观察结果，当前实现可视为“用当前时间补成一个零跨度 event”。

结论：
- 这是当前实现语义，不是 listener 侧行为。
- 该行为没有额外归一化逻辑，不会强制拉开 start/end。

## Q2: startTime>0 与 endTime>0 但 start>end 的异常输入是否允许直接透传
允许直接透传。`EventAnalyzerServiceImpl` 只处理“双无效时间”场景，不校验 start/end 顺序；`EventRecordAnalyzerListener.parse(...)` 只按优先级选择 `startTime` 作为 `timeBucket/timestamp`，不修正跨度关系。

结论：
- 当前域不负责事件时序合法性矫正。
- 若后续要收紧契约，应在 receiver 或 analyzer service 层新增校验，而不是在 record listener 静默修正。

## Q3: source 缺失、parameters 空 map、message/type 空串的行为
已核实：
- `source` 缺失：`service/serviceInstance/endpoint` 保持未设置。
- `parameters` 空 map：`parameters` 字段不写入 JSON，保持未设置。
- `message` 未提供：proto 默认空串，record 写入空串。
- `type` 未提供：proto3 默认值为 `Normal`，record 写入 `Normal`。
- `layer` 为未知名字：`Layer.nameOf(...)` 返回 `UNDEFINED`，不会在 analyzer 内抛错；真正的必填/合法性约束在 receiver 层。

## Q4: RecordStreamProcessor 单例输入是否有官方测试覆盖
在 `event-analyzer` 域内未发现针对 `RecordStreamProcessor.getInstance().in(event)` 的专门单测。本次新增测试只验证 `parse(...)` 的 record 映射与 service 时间兜底，不直接 mock/接管 `RecordStreamProcessor` 单例。

结论：
- 当前更适合作为消费侧交叉引用，留待 SW-5 Storage / Persistence 域进一步展开。
- SW-3D 本域只需确认 build 阶段唯一出口就是 `RecordStreamProcessor`。

## Q5: Event analyzer 是否还有隐藏 listener 工厂或未来扩展点
当前源码实证没有隐藏 fan-out：
- `EventAnalyzerModuleProvider.start()` 只注册一个 `EventRecordAnalyzerListener.Factory`
- `EventAnalyzer.createListeners()` 只消费 `IEventAnalyzerListenerManager` 里已有 factory 列表
- 当前运行时 listener 数量由 provider start 阶段决定

结论：
- SW-3D 当前是单 listener 薄链路，不是类似 trace analyzer 的多 point fan-out 体系。
- 若未来新增 listener factory，需要重新审计 listener 顺序、副作用和 build 阶段幂等性。
