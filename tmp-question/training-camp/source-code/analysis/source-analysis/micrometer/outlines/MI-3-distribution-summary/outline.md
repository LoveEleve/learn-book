# MI-3 DistributionSummary / Histogram — outline 收敛版

> 核心文件: `DistributionSummary.java` / `AbstractDistributionSummary.java` / `DistributionStatisticConfig.java` / `HistogramGauges.java` / `CumulativeDistributionSummary.java` / `StepDistributionSummary.java` / `HistogramSnapshot.java`
> harness: `MiniMI3` **20/20 PASS** | 日期: 2026-08-17

## 一、注册与实现分流
- `DistributionSummary.Builder.register()` → `MeterRegistry.summary(...)` → `SimpleMeterRegistry.newDistributionSummary(...)`
- `SimpleConfig.mode()`:
  - `CUMULATIVE` → `CumulativeDistributionSummary`
  - `STEP` → `StepDistributionSummary`
- 两条路径创建后都会调用 `HistogramGauges.registerWithCommonFormat(summary, registry)`，因此直方图/百分位 gauge 在 `SimpleMeterRegistry` 下是**自动注册**的

## 二、接口语义
- `record(double amount)`：负值 **silent drop**，与 Timer 的 warn+drop 不同
- `count()`：记录调用次数
- `totalAmount()`：累计总值
- `mean()`：`count == 0 ? 0 : total/count`
- `max()`：最大值
- `measure()` 默认只暴露 `COUNT + TOTAL`；具体实现重写后补 `MAX`
- deprecated 查询接口：`histogramCountAtValue()` / `percentile()` 都只是 `takeSnapshot()` 的遍历包装

## 三、scale 语义
- `Builder.scale(double)` 是 Summary 相比 Timer 的关键差异
- `AbstractDistributionSummary.record()` 先做 `scaledAmount = scale * amount`
- 随后：
  - `histogram.recordDouble(scaledAmount)`
  - `recordNonNegative(scaledAmount)`
- 结果：`scale` 影响 `totalAmount/max/histogram bucket`，**不影响 `count()` 的“调用次数”语义**

## 四、直方图实现选择
- `AbstractDistributionSummary.defaultHistogram(...)`
  - `isPublishingPercentiles()` → `TimeWindowPercentileHistogram`
  - `isPublishingHistogram()` → `TimeWindowFixedBoundaryHistogram`
  - else → `NoopHistogram.INSTANCE`
- Summary 与 Timer 的直方图选择同构，但 Summary 无 pauseDetector

## 五、Cumulative vs Step 差异
- `CumulativeDistributionSummary`
  - `LongAdder count` / `DoubleAdder total`
  - `TimeWindowMax max`
  - `count()/totalAmount()` 直接返回累计值
- `StepDistributionSummary`
  - `LongAdder count` / `DoubleAdder total`
  - `StepTuple2<Long, Double> countTotal`
  - `count()/totalAmount()` 返回 **上一周期** 值 (`poll1/poll2`)
  - `_closingRollover()` 强制把 partial step 刷成 previous（官方测试覆盖）
- 关键细节：即使 Summary 本体是 cumulative，`histogramCounts()` 仍会按 step/expiry 衰减

## 六、DistributionStatisticConfig
- `DEFAULT`:
  - `percentilesHistogram=false`
  - `percentilePrecision=1`
  - `minimumExpectedValue=1.0`
  - `maximumExpectedValue=+∞`
  - `expiry=2m`
  - `bufferLength=3`
- `merge(parent)`：**this 优先**，parent 兜底
- `isPublishingPercentiles()`：配置了 percentile 数组才算 true
- `isPublishingHistogram()`：`percentileHistogram=true` 或 `SLO 非空`
- `getHistogramBuckets(supportsAggregablePercentiles)`：
  - 支持 aggregable + percentileHistogram → 百分位桶 + min + max
  - 始终并入 SLO buckets
- `Builder.validate()` 拒绝：
  - `bufferLength <= 0`
  - percentile 不在 `[0,1]`
  - `minimumExpectedValue <= 0`
  - `maximumExpectedValue <= 0`
  - `minimumExpectedValue > maximumExpectedValue`
  - `SLO <= 0`

## 七、HistogramGauges
- Summary 导出格式：
  - percentile gauge: `${name}.percentile` + `phi=<p>`
  - bucket gauge: `${name}.histogram` + `le=<bucket|+Inf>`
- 所有衍生 gauge 都 `.synthetic(meter.getId())`，与 MI-1 syntheticAssociation 闭环
- `snapshotIfNecessary()` 语义：一个 publish cycle 的**第一个** gauge poll 刷新一次 snapshot，其余 gauge 复用同一快照
- meter filter 只应用一次；不会对 percentile/histogram gauge 再重复 map（官方测试覆盖）

## 八、HistogramSnapshot
- 保存 `count/total/max + percentileValues + histogramCounts`
- `mean()` 内部同样有除零保护
- `empty(count,total,max)` 返回空桶/空百分位快照
- `total(unit)/max(unit)/mean(unit)` 是 time-based histogram 复用能力；Summary 场景通常直接用无 unit 版本

## 九、官方测试交叉
- `DistributionSummaryTest`: cumulative/step 下 histogram decay 一致；double histogram accumulate failure 不抛出、只记日志
- `StepDistributionSummaryTest`: `mean()` 在未先调 total 时仍正确；`_closingRollover()` 保留 partial step
- `HistogramGaugesTest`: 每次 publish rollover / meter filter only once / `+Inf` bucket
- `DistributionStatisticConfigTest`: merge + 4 类 validation