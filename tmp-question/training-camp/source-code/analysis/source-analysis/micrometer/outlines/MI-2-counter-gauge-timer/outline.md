# MI-2 Counter/Gauge/Timer 家族 — outline 收敛版

> 通读: Counter(153)/Gauge(197)/TimeGauge(166)/MultiGauge(249)/StrongReferenceGaugeFunction(50)/FunctionCounter(130)/FunctionTimer(155)/Timer(478)/AbstractTimerBuilder(261)/AbstractTimer(307)/LongTaskTimer(518) — 11 文件 2644 行
> harness: MiniMI2 **20/20 PASS** (打脸2次: StepValue语义/Gauge检查条件) | 日期: 2026-08-17

## 一、接口族谱
| 类型 | 接口 | 测量 | 注册入口 |
|---|---|---|---|
| Counter | Counter/FunctionCounter | COUNT | registry.counter / more().counter |
| Gauge | Gauge/TimeGauge | VALUE | registry.gauge / more().timeGauge |
| Timer | Timer/FunctionTimer | COUNT+TOTAL_TIME+MAX | registry.timer / more().timer |
| LongTaskTimer | LongTaskTimer | ACTIVE_TASKS+DURATION+MAX | more().longTaskTimer |

## 二、默认边界 (关键差异)
- Timer: **min=1ms / max=30s** (AbstractTimerBuilder L35-37, 构造注入 L61-62)
- LongTaskTimer: **min=2min / max=2h** (L289-291)
- SimpleConfig 默认: **mode=CUMULATIVE / step=60s** (CUMULATIVE 时 StepCounter 不参与!)

## 三、Step 语义 (harness 实证, 打脸2次)
- **poll() 返回上一周期累积值** (previous), 非当前累积 (current)
- 同步长内 count()=0; 跨步长后 count()=上周期累积; 再跨步长=0
- SimpleConfig 默认 mode=CUMULATIVE → Counter 实际是 CumulativeCounter (不 step) → 需显式 STEP

## 四、直方图三级选择 (AbstractTimer L120-133)
- publishPercentiles → TimeWindowPercentileHistogram (HDR, 本地百分位, 不可聚合)
- publishPercentileHistogram → TimeWindowFixedBoundaryHistogram (fixed 桶, 可聚合)
- 否则 → NoopHistogram.INSTANCE (零成本)

## 五、Sample 延迟绑定
- Timer.Sample: startTime=clock.monotonicTime; stop(timer) 才绑定 timer (tags 最后时刻决定)
- LongTaskTimer.Sample: 抽象 (stop/duration); stop(long)/duration(long) deprecated → 恒 -1
- ResourceSample (Timer 1.6.0): AutoCloseable, close() 时 registry.timer()

## 六、弱引用 / 强引用
- DefaultGauge: WeakReference → obj null → NaN (value 函数抛异常 → 捕获 + NaN)
- StrongReferenceGaugeFunction: 持有 obj 强引用 → 对象可被弱引用化
- StepFunctionTimer: WeakReference<T> ref → obj GC 后 accumulateCountAndTotal 跳过 → 无新数据

## 七、负值 drop + 协调遗漏
- AbstractTimer.record: amount<0 → WarnThenDebugLogger + 丢弃 (L270-285)
- ClockDriftPauseDetector: LatencyUtils Class.forName → CNFE 降级禁用
- StepFunctionTimer: 函数值 Math.max(v,0) 负值钳制

## 八、MultiGauge (getMappedId 消费方)
- 每行 preFilteredId=commonId.withTags(unique); rowId=getMappedId
- overwrite && 已存在 → removeByPreFilterId; 消失行 → remove; 原子注册

## 九、measure() 统计量
| 类型 | 测量 |
|---|---|
| Counter/FunctionCounter | COUNT |
| Gauge/TimeGauge | VALUE |
| Timer | COUNT+TOTAL_TIME+MAX |
| FunctionTimer | COUNT+TOTAL_TIME |
| LongTaskTimer | ACTIVE_TASKS+DURATION+MAX |

## 十、Timed 注解映射
- Timer.builder(Timed, defaultName): longTask 时 value 必须非空 (IllegalArgument L94-100)
- LongTaskTimer.builder(Timed): 双重强制 longTask=true + value 非空