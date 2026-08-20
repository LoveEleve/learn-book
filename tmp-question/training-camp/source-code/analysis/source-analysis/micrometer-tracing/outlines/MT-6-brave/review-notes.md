# MT-6 Brave Bridge — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0 首轮深读)
- [x] 深读 12 个 Brave bridge 关键文件：tracer/span/builder/currentContext/baggage/context/finishedSpan/W3C/sampler
- [x] BraveTracer parent 语义已确认：`TraceContextOrSamplingFlags` 是 parent 入口
- [x] BraveSpan error 语义已确认：额外补 `error` tag，再记 throwable
- [x] BraveBaggageInScope 恢复语义已确认：close 恢复 previous baggage
- [x] BraveFinishedSpan links 语义已确认：通过 tags 编解码，不是原生 link 字段
- [x] W3CPropagation 已确认：`traceparent` + `tracestate` + 可选 baggage header

## 当前重点风险/待下轮实证
- [ ] `BraveSpanBuilder.setNoParent()` 当前实现看起来没有显式清空 `parentContext`，需 harness 实证
- [ ] `BraveBaggageInScope` 的 `tagFields` → span tags 同步，需要 harness/测试验证
- [ ] `W3CBaggagePropagator` 的 localFields 忽略 / malformed baggage header 忽略，需测试对照
- [ ] `ProbabilityBasedSampler` 的 100-window 近似采样与 `RateLimitingSampler` 需要边界验证

## 收敛判定
- 第一轮只完成了核心语义梳理，还未到“无问题”状态；下一轮必须补 harness 与官方测试交叉。

## 审查轮次: 第二轮 (2026-08-17, 官方测试交叉/风险收敛)
- [x] `BraveSpanBuilderTests` 对照确认：child parent 绑定、links tag 编码、多值 tags 全通过
- [x] `W3CBaggagePropagatorTest` 对照确认：空 header、metadata-only、duplicate keys、value with equals、invalid header、inject 行为都被覆盖
- [x] `ProbabilityBasedSamplerTests` 对照确认：probability=0/1 与 1000 次采样近似计数、null supplier 异常
- [x] `BraveTracingApiTests` 对照确认：legacy/new baggage API、withSpan/currentTraceContext、显式 parent、跨线程延续都被示例化验证
- [x] 风险更新：`setNoParent()` 当前未见专门测试，也未在实现中显式清 parent；保留为“设计边界”而非已证缺陷

## 收敛判定 (第二轮终)
MT-6 当前无新增缺陷。剩余边界主要是 `setNoParent()` 的语义保守解释，以及 Brave links 为 tags 编码兼容层这一实现现实。
