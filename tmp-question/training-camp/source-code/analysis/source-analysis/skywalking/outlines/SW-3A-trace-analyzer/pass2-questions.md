# SW-3A Trace / Agent Analyzer — Pass 2 问题

## P1: TraceAnalyzer listener 生命周期
- `SegmentParserServiceImpl.send()` 每个 segment new `TraceAnalyzer`
- `TraceAnalyzer.createSpanListeners()` 每次创建 listener 实例
- 结论：状态型 listener 不跨 segment 串线；listener manager 只保存 factory，不保存实例

## P2: 固定分析顺序
- Segment → First → Entry/Exit/Local → build
- `spanId==0` 才触发 First；span type 走三路 else-if
- 未知 span type 只 error log，不抛异常；但该 span 不进入任何专用 listener

## P3: 采样优先级
- 默认/服务采样策略均为：duration threshold 或 sample rate 命中即采样
- `forceSampleErrorSegment` 只在 sampler 未命中且 segment error 时补采样
- sample status 在 Segment 点确定，后续 First/Entry 不重新决定

## P4: SegmentAnalysisListener 输出
- Segment 点汇总时间、错误、searchable tags、duration
- duration 超 int max 钳制
- sample IGNORE 时 parseFirst/build 快速返回
- build 输出 Segment；随后为每个 searchable tag 输出 TagAutocomplete

## P5: point/cast 运行时契约
- `containsPoint` 返回 true 必须同时实现对应子接口
- factory/listener 配置错误会在 TraceAnalyzer 的强制 cast 处失败
- 当前没有发现通用类型检查防护，属于 SPI 使用约束

## P6: 发现的 sampling 边界风险
- `TraceSegmentSampler.shouldSample()` 使用 `Math.abs(segmentObject.getTraceId().hashCode()) % 10000`
- Java 中 `Math.abs(Integer.MIN_VALUE)` 仍为负数；极端 traceId hash 为 MIN_VALUE 时 sample 为负
- `withinRateRange(currentSample, policySample)` 只判断 `<`，负 sample 会异常命中采样范围
- 这是一个真实边界风险，需后续决定修复为 `Math.floorMod(hash, 10000)` 或增加回归测试

## 收敛状态
SW-3A 主 pipeline 已闭环，但由于 P6 sampling hash 边界风险，当前不能标记为“无问题”；下一步先对 sampling 风险做精确复现/修复评审，再做最终 review。