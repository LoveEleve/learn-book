# MI-2 Counter/Gauge/Timer 家族 — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0 全量通读 + Pass1 细节验证 + Pass2 问题深挖 + Pass3 harness 收敛)

### ① 机制: 锚点有效性/因果链/闭环
- [x] 全文件通读: 11 文件 2644 行 (Counter/Gauge/TimeGauge/MultiGauge/StrongReferenceGaugeFunction/FunctionCounter/FunctionTimer/Timer/AbstractTimerBuilder/AbstractTimer/LongTaskTimer)
- [x] 消费闭环: Builder → register → registry.counter/gauge/timer → newCounter/newGauge/newTimer 抽象
- [x] SimpleConfig 默认: mode=CUMULATIVE → CumulativeCounter/CumulativeTimer; step=60s → StepCounter 默认步长
- [x] StepValue.poll 语义实证: 返回 previous (上周期累积), 非 current

### ② 语义: 术语精确
- [x] "StepValue 返回上周期累积值" (非当前) — harness 实证 + 反射验证
- [x] "WeakReference → NaN" (非0) — harness 实证
- [x] "negative drop → WarnThenDebugLogger + IllegalArgumentException" — harness 日志实证
- [x] "SimpleConfig 默认 mode=CUMULATIVE" — 源码实读 L54-55

### ③ 架构/拓扑: 前向引用
- [x] MI-2 拓扑第 3 位: 依赖 MI-1 (MeterRegistry 注册流程) + MI-7 (MeterFilter accept) — 已分析, 合法
- [x] DistributionStatisticConfig → MI-3 (后向引用, 已实证)
- [x] HistogramGauges → syntheticAssociation → MI-1 (已实证)

### ④ 数字穷举
- [x] 11 文件穷举 (grep instrument/*.java)
- [x] 6 Builder 类 (Counter/Gauge/TimeGauge/Timer/LongTaskTimer/MultiGauge)
- [x] 3 more() 路径 (counter/timeGauge/longTaskTimer)
- [x] 20 断言 harness (20/20 全绿)

### ⑤ harness 打脸记录
- 打脸1: SimpleConfig 默认 mode=CUMULATIVE → Counter 非 StepCounter → 需显式 STEP
- 打脸2: Gauge strongReference 检查条件错误 (NaN 是正确结果)
- 总计: 2 次打脸全修正