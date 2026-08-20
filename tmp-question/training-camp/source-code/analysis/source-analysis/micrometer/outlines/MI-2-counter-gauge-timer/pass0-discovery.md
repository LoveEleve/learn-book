# MI-2 Counter/Gauge/Timer 家族 — Pass 0 发现

> 通读: Counter(153)/Gauge(197)/TimeGauge(166)/MultiGauge(249)/StrongReferenceGaugeFunction(50)/FunctionCounter(130)/FunctionTimer(155)/Timer(478)/AbstractTimerBuilder(261)/AbstractTimer(307)/LongTaskTimer(518) — 11 文件 2644 行
> 日期: 2026-08-17

## 1. 注册入口三路径
| 路径 | 方法 | 类型 | 特点 |
|---|---|---|---|
| registry 直接 | counter/gauge/timer | Counter/Gauge/Timer | Counter/Gauge 保留 baseUnit; Timer baseUnit=null (实现决定 L471) |
| more() | counter/timeGauge/longTaskTimer/functionTimer | FunctionCounter/TimeGauge/LongTaskTimer/FunctionTimer | baseUnit 全 null; strongReference 由调用方包装 |
| Builder 直通 | 各 Builder.register | — | 构造 Id(name,tags,baseUnit,desc,type[,synthetic]) |

## 2. 默认直方图边界 (关键差异)
- Timer: **min=1ms / max=30s** (AbstractTimerBuilder L35-37, 构造时注入 L61-62)
- LongTaskTimer: **min=2min / max=2h** (L289-291, L308-309)
- DistributionSummary 域 (MI-3) 另述

## 3. AbstractTimer 直方图三级选择 (L120-133)
```
publishPercentiles → TimeWindowPercentileHistogram (HDR, 本地百分位)
publishHistogram   → TimeWindowFixedBoundaryHistogram (fixed 桶, 可聚合)
否则               → NoopHistogram.INSTANCE (零成本)
```

## 4. 负值 drop + 协调遗漏补偿
- record(long): amount<0 → WarnThenDebugLogger 警告 + 丢弃 (L270-285; Timer 注释 L115 "less than 0 dropped")
- ClockDriftPauseDetector: Class.forName("org.LatencyUtils.SimplePauseDetector") CNFE → 降级禁用 (L139-148)
- pause 期间: intervalEstimator (TimeCappedMovingAverage 128/10s) → recordValueWithExpectedInterval 补录缺失采样 (L175-183)

## 5. Sample 机制
- Timer.Sample: startTime=clock.monotonicTime; stop(timer) 才绑定 timer (tags 最后时刻决定, L310-332)
- LongTaskTimer.Sample: 抽象 (stop()/duration(unit)); stop(long)/duration(long) deprecated 1.5.0 → 恒 -1 (任务无 ID)
- ResourceSample (Timer, 1.6.0): AutoCloseable, close() 时 record (registry.timer)

## 6. measure() 统计量
| 类型 | 测量 |
|---|---|
| Counter/FunctionCounter | COUNT |
| Gauge/TimeGauge | VALUE |
| Timer | COUNT+TOTAL_TIME+MAX |
| FunctionTimer | COUNT+TOTAL_TIME (mean=total/count, count==0→0) |
| LongTaskTimer | ACTIVE_TASKS+DURATION+MAX |

## 7. MultiGauge (getMappedId 消费方)
- commonId 固定; 每行 preFilteredId=commonId.withTags(unique); rowId=**registry.getMappedId** (L94)
- overwrite && 已存在 → removeByPreFilterId (重建); overwrite||新 → registry.gauge(strongRef)
- 消失行 → registry.remove (L110-113); register 原子 (getAndUpdate)

## 8. strongReference 语义
- Gauge/TimeGauge Builder: strongReference(boolean) → StrongReferenceGaugeFunction 包装 (持有 obj 强引用, 实现可弱引用化)
- Supplier builder 版本: 默认 strongReference(true), null → NaN
- MultiGauge 行: 恒 StrongReferenceGaugeFunction (L102)

## 9. Timed 注解映射
- Timer.builder(Timed, defaultName): longTask 时 value 必须非空 (IllegalArgument L94-100); histogram→publishPercentileHistogram; percentiles; SLO (seconds→nanos 转换 L108)
- LongTaskTimer.builder(Timed): **双重强制** longTask=true + value 非空 (L47-56)

## 10. 待 Pass 1 验证 (Q)
- Q1: registry.more() 的 More 实现 (SimpleMeterRegistry) — 弱引用/强引用? functionTimer baseTimeUnit?
- Q2: registry.counter/gauge/timer 在 SimpleMeterRegistry 的实现 (Step 模式/Cumulative?)
- Q3: StepCounter/StepTimer/StepFunctionCounter (step/ 子包) — 与 MI-1 StepValue 联动
- Q4: SimpleGauge 弱引用行为 (WeakReference 丢值) + syntheticAssociation 使用
- Q5: MultiGauge removeByPreFilterId 与 MI-1 multiplePreFilterIdsMapToSameId 联动
- Q6: 负值 drop / mean 除零 / 空样本 行为 (harness 待验证)