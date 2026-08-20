# MI-3 DistributionSummary / Histogram — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2/3 + harness 收敛)
- [x] 通读 7 文件: DistributionSummary / AbstractDistributionSummary / DistributionStatisticConfig / HistogramGauges / CumulativeDistributionSummary / StepDistributionSummary / HistogramSnapshot
- [x] 注册闭环: Builder.register → MeterRegistry.summary → SimpleMeterRegistry.newDistributionSummary → Cumulative/Step 分流 + HistogramGauges.registerWithCommonFormat
- [x] `record(double)` 语义: Summary 负值 **silent drop**，与 Timer 的 warn+drop 不同
- [x] `scale` 全链路: 先缩放再 recordHistogram/recordNonNegative，影响 total/max/buckets，不影响 count 的“调用次数”语义
- [x] Step 语义: StepDistributionSummary `count()/totalAmount()` 返回 `StepTuple2.poll1/poll2`，即**上一周期**值
- [x] DSC 关键: DEFAULT(2m/3/+∞/1.0/precision=1) / merge(this 优先) / validate 5 类失败
- [x] HistogramGauges: synthetic derivative + 一次 publish cycle 一次 snapshot 刷新
- [x] harness `MiniMI3` **20/20 PASS**

## 关键打脸
- 打脸1: `HistogramGauges.polledGaugesLatch` 是 package-private，不能直接断言内部 latch；改成行为断言（下一 publish cycle 第一个 gauge poll 刷新 snapshot）
- 打脸2: percentile histogram 需要 `HdrHistogram` 运行时依赖；harness classpath 补 `HdrHistogram-2.2.2.jar` + `LatencyUtils-2.0.3.jar`

## 收敛判定
- MI-3 第一轮收敛，待第二轮做官方测试交叉 + 锚点/数字残留清零。

## 审查轮次: 第二轮 (2026-08-17, 官方测试交叉/残留与误解清零)
- [x] 官方测试交叉:
  - `DistributionSummaryTest` 3 个: cumulative/step 直方图 decay + double histogram accumulate failure log
  - `StepDistributionSummaryTest` 2 个: `mean()` 在未先调 total 的场景仍正确 + `_closingRollover()` 保留 partial step
  - `DistributionStatisticConfigTest` 5 个: merge + 4 类 validation
  - `HistogramGaugesTest` 3 个: every publish rollover / meter filter only once / `+Inf` bucket
- [x] 关键对齐:
  - `histogramCounts()` **即使 summary 本体是 cumulative**, bucket counts 仍按 step/expiry 衰减 (官方注释明确)
  - `HistogramGauges` 在 `SimpleMeterRegistry.newDistributionSummary()` 已**自动注册**, 手工再次 `registerWithCommonFormat` 只会触发重复 gauge warn；harness 已修正为只依赖自动注册路径
  - meter filter 对 percentile/histogram gauges 只应用一次，与 MI-7 的 `map()` 语义闭环
- [x] 残留扫描: MI-3 文档无旧数字/旧锚点残留
- [x] harness 仍 20/20 全绿，且去除重复注册 warn 依赖

## 收敛判定 (第二轮终)
MI-3 当前无已知问题。下一轮若继续深审，可转向 `AbstractTimeWindowHistogram / TimeWindowPercentileHistogram / FixedBoundaryHistogram` 的实现层复杂度与边界。

## 审查轮次: 第三轮 (2026-08-17, 结构完整性/交付物完整性/残留清零)
- [x] 发现并修复交付物缺口: `outline.md` 当时尚未落盘，已补齐正式收敛版 outline
- [x] 结构完整性: `pass0-discovery.md` / `outline.md` / `review-notes.md` / `harness/MI-3/MiniMI3.java` 现已齐全
- [x] 残留扫描: 无旧轮次数字残留；MI-3 关键数字一致 (`7` 文件 / harness `20/20` / 官方测试 `3+2+3+5`)
- [x] 行为复核: `HistogramGauges` 在 `SimpleMeterRegistry` 下自动注册，手工再次注册只会得到重复 gauge warn；文档和 harness 已同步修正
- [x] 交叉一致性: MI-1 syntheticAssociation 唯一来源说法保持成立，因为 Summary histogram gauges 仍经 `Gauge.Builder.synthetic(meter.getId())`

## 收敛判定 (第三轮终)
MI-3 交付物、语义、官方测试对照、harness、残留扫描均已闭环；当前无已知问题。