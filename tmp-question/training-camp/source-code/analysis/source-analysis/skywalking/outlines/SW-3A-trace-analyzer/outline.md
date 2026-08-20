# SW-3A Trace / Agent Analyzer — outline 收敛版

> 核心: `SegmentParserServiceImpl` / `TraceAnalyzer` / listener SPI / `SegmentAnalysisListener` / `TraceSegmentSampler`
> harness: 未单独构造 protobuf listener harness；官方 reactor sampling 回归 8/8 PASS

## 主链
`SegmentObject` → new TraceAnalyzer/listeners → Segment → First → Entry/Exit/Local → build → SourceReceiver

## 关键语义
- 每个 segment 新建 listener，避免状态跨 segment
- spanId=0 触发 First
- 采样在 Segment 点决定
- duration 超 int max 钳制
- error + forceSampleErrorSegment 可强制采样
- IGNORE 在 First/build 快速短路
- searchable tags 去重/超长丢弃，build 后输出 TagAutocomplete

## 修复
- `TraceSegmentSampler` 使用 `Math.floorMod(traceId.hashCode(), 10000)`，修复 `Math.abs(Integer.MIN_VALUE)` 产生负 sample 的边界缺陷

## 验证
- `TraceSamplingPolicyWatcherTest`：8/8 PASS
- 完整 reactor `./mvnw -pl oap-server/analyzer/agent-analyzer -am -Dtest=TraceSamplingPolicyWatcherTest -Dsurefire.failIfNoSpecifiedTests=false test`：PASS
- analyzer agent 53 个 production Java 编译通过