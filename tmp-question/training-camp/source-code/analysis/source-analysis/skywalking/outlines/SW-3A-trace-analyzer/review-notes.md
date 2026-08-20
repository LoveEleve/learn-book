# SW-3A Trace / Agent Analyzer — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2 + sampling 回归)
- [x] 53 个 production Java / 7 个 test Java 数字复核
- [x] `TraceAnalyzer` 顺序：Segment→First→Entry/Exit/Local→build
- [x] listener 每 segment 新建，状态不跨 segment
- [x] 官方 `TraceSamplingPolicyWatcherTest` 8 tests 全通过（完整 reactor `-am`）
- [x] 新增极端 hash 回归：`"polygenelubricants".hashCode()==Integer.MIN_VALUE` 时不再产生负 sample
- [x] `TraceSegmentSampler` 已修复：`Math.abs(hash)%10000` → `Math.floorMod(hash,10000)`
- [x] analyzer agent module 编译通过

## 审查轮次: 第二轮 (2026-08-18, 测试命令/边界/残留复核)
- [x] 首次错误命令 `-Dtest` 作用于 reactor 前置模块导致“无匹配测试”失败；已改用 `-Dsurefire.failIfNoSpecifiedTests=false`，完整 reactor 测试通过
- [x] 24-module reactor 构建链成功：apm-network codegen → server-core → analyzer dependencies → agent-analyzer
- [x] sampling 极端值、duration threshold、dynamic policy fallback、invalid YAML retain-last-good 均由官方测试覆盖
- [x] 未将控制台 YAML parse error 日志误判成测试失败；测试结果为 8/8 PASS
- [x] 无新的 listener 顺序、state leak、unknown SpanType、point/cast 问题

## 收敛判定
SW-3A 当前无已知源码问题。sampling 极端 hash 风险已修复并完成完整 reactor 回归。