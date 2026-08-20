# MI-3 DistributionSummary / Histogram 域 — Pass 0 发现

> 通读: DistributionSummary(421) / AbstractDistributionSummary(84) / DistributionStatisticConfig(498) / HistogramGauges(152) / CumulativeDistributionSummary(102) / StepDistributionSummary(111) / HistogramSnapshot(135)
> 日期: 2026-08-17

## 1. 入口与实现分层
- `DistributionSummary.Builder.register()` → `registry.summary(Id(DISTRIBUTION_SUMMARY), distributionConfigBuilder.build(), scale)`
- `MeterRegistry.newDistributionSummary(...)` 在 SimpleMeterRegistry 中按 `mode()` 分流:
  - `CUMULATIVE` → `CumulativeDistributionSummary`
  - `STEP` → `StepDistributionSummary`
- 两者都在创建后 `HistogramGauges.registerWithCommonFormat(summary, this)`

## 2. AbstractDistributionSummary 核心
- `record(double amount)` 只接受 `amount >= 0`；负值直接丢弃, **无日志**
- `scale` 在 record 入口统一生效: `scaledAmount = scale * amount`
- 先 `histogram.recordDouble(scaledAmount)`, 再 `recordNonNegative(scaledAmount)`
- `takeSnapshot()` 汇总 `count()/totalAmount()/max()` + 直方图/百分位

## 3. DistributionSummary 接口层
- 统计量: `count()/totalAmount()/mean()/max()`
- `measure()` 默认只暴露 `COUNT + TOTAL`；`MAX` 由具体实现重写后补上
- `mean()` 除零保护: `count == 0 ? 0 : total/count`
- deprecated 查询 API:
  - `histogramCountAtValue(long)`：从 `takeSnapshot().histogramCounts()` 遍历
  - `percentile(double)`：从 `takeSnapshot().percentileValues()` 遍历

## 4. Builder 能力
- tags/description/baseUnit
- 分布配置: `publishPercentiles` / `percentilePrecision` / `publishPercentileHistogram`
- `sla(long...)` / `sla(double...)` 已 deprecated → `serviceLevelObjectives(double...)`
- `minimumExpectedValue(Long/Double)` / `maximumExpectedValue(Long/Double)` 双重载
- `distributionStatisticExpiry` / `distributionStatisticBufferLength`
- **`scale(double)`**: 录入值缩放，是 Summary 相比 Timer 的关键差异
- `withRegistry()` 支持动态标签 (1.12.0)

## 5. DistributionStatisticConfig 核心
- `DEFAULT`:
  - `percentilesHistogram=false`
  - `percentilePrecision=1`
  - `minimumExpectedValue=1.0`
  - `maximumExpectedValue=+∞`
  - `expiry=2m`
  - `bufferLength=3`
- `merge(parent)`: **this 优先**，parent 兜底
- `isPublishingPercentiles()` = `percentiles != null && length > 0`
- `isPublishingHistogram()` = `percentileHistogram==true || slos.length>0`
- `getHistogramBuckets(supportsAggregablePercentiles)`:
  - percentileHistogram && supportsAggregablePercentiles → `PercentileHistogramBuckets.buckets(this)` + min + max
  - 始终并入 SLO buckets
- `Builder.validate()` 拒绝:
  - `bufferLength <= 0`
  - percentile 不在 `[0,1]`
  - min/max `<= 0`
  - `min > max`
  - SLO `<= 0`
  - 抛 `InvalidConfigurationException`

## 6. 直方图实现选择
- `AbstractDistributionSummary.defaultHistogram(...)`:
  - publishingPercentiles → `TimeWindowPercentileHistogram`
  - publishingHistogram → `TimeWindowFixedBoundaryHistogram`
  - else → `NoopHistogram.INSTANCE`
- 与 Timer 域完全同构，但 Summary 无 pauseDetector

## 7. Cumulative vs Step 实现差异
- `CumulativeDistributionSummary`
  - `LongAdder count` / `DoubleAdder total`
  - `TimeWindowMax max`
  - `count()/totalAmount()` 直接读累计值
  - `max()` → `TimeWindowMax.poll()`
- `StepDistributionSummary`
  - `LongAdder count` / `DoubleAdder total`
  - `StepTuple2<Long, Double> countTotal`
  - `count()/totalAmount()` 返回 **上一周期** 值 (`poll1/poll2`)
  - `_closingRollover()` → `countTotal._closingRollover()`

## 8. HistogramGauges
- 支持 Timer / LongTaskTimer / DistributionSummary 的 common format 导出
- Summary 导出:
  - percentiles → `${name}.percentile` + tag `phi=<p>`
  - buckets → `${name}.histogram` + tag `le=<bucket|+Inf>`
- 构造时先 `takeSnapshot()` 决定 `totalGauges`
- `snapshotIfNecessary()`：每个 publish cycle 第一个 gauge poll 时刷新 snapshot；其余 gauge 复用同一快照
- 所有衍生 gauge 都 `.synthetic(meter.getId())` — 与 MI-1 syntheticAssociation 唯一来源闭环

## 9. HistogramSnapshot
- 保存 `count/total/max + percentileValues + histogramCounts`
- `mean()` 除零保护
- `total(unit)/max(unit)/mean(unit)` 用于 time-based histogram 的单位换算；Summary 直接用无 unit 版本
- `empty(count,total,max)` 返回空桶/空百分位

## 10. 待 Pass 1 验证 (Q)
- Q1: Summary 负值 drop 是否真的**无日志**，与 Timer 域不同
- Q2: `scale` 对 `count/total/max/histogram bucket` 是否全链路生效
- Q3: `supportsAggregablePercentiles=false` 时, `percentileHistogram=true` 但 bucket 只剩 SLO/min/max 还是完全空？
- Q4: `HistogramGauges` 的 snapshot latch 语义：一个 publish cycle 只拍一次快照
- Q5: StepDistributionSummary 的 poll 语义是否与 StepCounter 完全同构 (同周期 0, 跨周期返回 previous)
- Q6: `measure()` 默认无 MAX，但具体实现重写后补 MAX —— 实际导出闭环