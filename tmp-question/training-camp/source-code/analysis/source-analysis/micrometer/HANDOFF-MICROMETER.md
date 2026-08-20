# HANDOFF-MICROMETER

> 项目: `spring/micrometer`
> 阶段: 6.1 Micrometer
> 日期: 2026-08-17
> 方法论: 09 怀疑审计 / Pass0→Pass1→Pass2→Pass3 / harness 费曼验证 / 多轮 review 收敛

## 0. 执行结论

Micrometer 本阶段的 7 个域已经完成源码大纲梳理、关键语义验证、harness 费曼实证，以及多轮深审收敛：

1. `MI-1 MeterRegistry`
2. `MI-7 MeterFilter`
3. `MI-2 Counter/Gauge/Timer`
4. `MI-3 DistributionSummary/Histogram`
5. `MI-4 AOP/@Timed/@Counted`
6. `MI-5 MeterBinder/JVM binders`
7. `MI-6 Observation`

除 **MI-4 中“重复 `@Timed` 在 AOP 路径的支持不明确”** 这一明确记录的源码实现风险外，其余 6 个域已收敛为“无已知问题”。

---

## 0.1 边界说明

本交接文档里的“完成”指 **本阶段方法论约定的域级大纲梳理完成**，不是“仓内每个文件逐个逐行读完”。其中有两个域必须明确边界：

- `MI-5 MeterBinder`：已对 **JVM binder 主干** (`JvmGcMetrics / ExecutorServiceMetrics / JvmThreadMetrics / JvmMemoryMetrics / ClassLoaderMetrics / JvmCompilationMetrics / JvmHeapPressureMetrics`) 做深读、harness、review；其余 `binder/` 22+ 子包已完成归类，但未逐个做同等深度源码审计。
- `MI-6 Observation`：主战场在 `micrometer-observation` 独立模块。当前已对 **核心状态机与主链** (`Observation / ObservationRegistry / ObservationHandler / SimpleObservation / no-op/scope 相关`) 做深读、harness、review；`ObservedAspect / Propagator / ObservationThreadLocalAccessor / docs/transport` 等桥接扩展层已纳入拓扑与测试边界，但未逐个达到核心 8 文件同等深度。

因此，这份 handoff 是“**阶段级框架大纲 + 核心语义收敛**”，不是“全仓每个扩展点零边界遗漏”的逐文件审计报告。

---

## 1. 全局架构图（压缩版）

Micrometer 的运行主链可以压缩为四层：

### L1. 核心注册与过滤层
- `MeterRegistry`
- `MeterFilter`
- 负责：ID 映射、幂等注册、filter 链、distribution config merge、meter 生命周期

### L2. Meter 原语层
- `Counter / Gauge / TimeGauge / Timer / LongTaskTimer / Function* / DistributionSummary / MultiGauge`
- 负责：具体统计语义（累计/步长、直方图、百分位、synthetic gauge 等）

### L3. 注解与观察层
- `@Timed / @Counted` + `TimedAspect / CountedAspect`
- `Observation / ObservationRegistry / ObservationHandler`
- 负责：把业务方法调用和跨 handler 生命周期统一接到 meter/tracing/logging

### L4. Binder 消费层
- `MeterBinder`
- `JvmGcMetrics / ExecutorServiceMetrics / JvmThreadMetrics / JvmMemoryMetrics ...`
- 负责：把 JVM/JMX/线程池/外部组件状态转成 meter

拓扑关系是：
- `MI-1 → MI-7 → MI-2 → MI-3 → MI-4 → MI-5`
- `MI-6` 独立模块主战场，但通过 `DefaultMeterObservationHandler` 等桥接回到 `MeterRegistry`

---

## 2. 各域总览

### MI-1 `MeterRegistry`

核心职责：
- 所有 meter 的统一注册中心
- filter 链执行
- pre-filter ID / mapped ID 双映射
- remove / clear / close 生命周期控制

关键结论：
- 存在 **双重 ID 映射**：`preFilterIdToMeterMap` + `meterMap`
- `mapId` 先于 `accept` 与 `configure`
- `accept` 语义：`DENY` 拒绝、`ACCEPT` 短路通过、`NEUTRAL` 继续
- `configure` 链式累积，最后再 merge registry default histogram config
- `MultiGauge` 是 `getMappedId(...)` 的唯一外部消费方
- `removeByPreFilterId(...)` 会在“多个 pre-filter ID 指向同一 mapped ID”场景下做反向清理
- `SimpleMeterRegistry` 默认 `mode=CUMULATIVE`, `step=1m`

harness：`MiniMI1` **25/25 PASS**

---

### MI-7 `MeterFilter`

核心职责：
- 对 meter 进行 map / accept / configure 三类变换
- 负责通用 tags、tag 改名、tag 值替换、计数上限、统计边界覆盖等

关键结论：
- `map` 先于 `accept/configure`
- `accept` 看的是 **map 后的 ID**
- `maxExpected/minExpected` 通过 `builder().build().merge(config)` 实现“filter 强制覆盖 meter config”
- `maximumAllowableTags` 是 **双通道委托**：超限时同时影响 `accept` 与 `configure`
- `forMeters(...)` 只对匹配谓词的 meter 委托，其余走 `MeterFilter.super`
- `NamingConvention` 的转换发生在 **导出侧**（`getConventionName/getConventionTags`），不是注册侧 `getName`
- `commonTags` 的“同 key 不覆盖”真实机制是 `Tags.merge` 的 **后集优先去重**

harness：`MiniMI7` **30/30 PASS**

---

### MI-2 `Counter/Gauge/Timer`

核心职责：
- 定义最常用 meter 原语及其 builder 语义
- 覆盖 step/cumulative、sample、长任务计时、function meter、MultiGauge 等

关键结论：
- `Counter/Gauge/Timer` 的 builder 都是 `Id(...)` 直通 `MeterRegistry`
- `Timer.Sample` 是 **延迟绑定**：`stop(timer)` 时才决定落到哪个 timer
- `AbstractTimerBuilder` 默认时间边界是 `1ms ~ 30s`
- `LongTaskTimer.Builder` 默认边界是 `2min ~ 2h`
- `DefaultGauge` 使用 `WeakReference`，对象丢失或 valueFunction 抛异常时返回 `NaN`
- `StrongReferenceGaugeFunction` 只是“强持有被观测对象”，不保证其内部值不为 null
- `StepValue.poll()` / `StepTuple2.poll*()` 返回的是 **上一周期值**，不是当前累积值
- `SimpleConfig` 默认 `mode=CUMULATIVE`，这点非常容易误判成 STEP
- `FunctionTimer` / `TimeGauge` / `FunctionCounter` 走 `registry.more()` 路径
- `MultiGauge.register(...)` 做动态 reconcile：新增则注册、缺失则移除、`overwrite=true` 时按 pre-filter ID 重建

harness：`MiniMI2` **20/20 PASS**

---

### MI-3 `DistributionSummary/Histogram`

核心职责：
- 处理非时间型分布统计
- 负责 scale、SLO、percentile histogram、snapshot、bucket 导出

关键结论：
- `AbstractDistributionSummary.record(amount)` 对负值是 **silent drop**（和 Timer 的 warn+drop 不同）
- `scale(double)` 先作用，再进入 histogram / total / max；但 `count()` 仍只按“调用次数”递增
- `DistributionStatisticConfig.merge(parent)` 是 **this 优先**
- `isPublishingHistogram()` 的条件是：`percentileHistogram=true` 或 `SLO 非空`
- `StepDistributionSummary` 的 `count()/totalAmount()` 同样返回 **上一周期** 值
- `HistogramGauges` 在 `SimpleMeterRegistry.newDistributionSummary(...)` 中会 **自动注册**
- `HistogramGauges` 每个 publish cycle 只拍一次 snapshot，其余 gauge 复用
- `meter filter` 不会对 histogram/percentile gauge 重复 map

harness：`MiniMI3` **20/20 PASS**

---

### MI-4 `AOP/@Timed/@Counted`

核心职责：
- 用切面把 `@Timed` / `@Counted` 映射到 `Timer/LongTaskTimer/Counter`
- 用 `@MeterTag` 扩展参数/返回值 tags

关键结论：
- `TimedAspect` 与 `CountedAspect` 都支持：
  - sync success/failure
  - `CompletionStage` 完成后再记录
  - 自定义 `tagsBasedOnJoinPoint`
  - skip predicate
- `TimedAspect`：
  - `longTask=false` → Timer 路径
  - `longTask=true` → LongTaskTimer 路径
  - 异步异常 tag 取 `throwable.getCause()` 优先
- `CountedAspect`：
  - 固定 tags：`result=success|failure`，`exception=<simpleName|none>`
  - `recordFailuresOnly=true` 时成功路径完全不记
- `@MeterTag` 三层优先级：`resolver > expression > toString`
- `class-level` 与 `method-level` 注解不会双记：pointcut 有 `!@annotation(...)` 去重

**明确记录的源码风险**：
- `@Timed` 虽标记为 `@Repeatable(TimedSet.class)`
- 但 `TimedAspect` 读取方法注解时用的是 `getAnnotation(Timed.class)`
- 反射实证：重复 `@Timed("a") @Timed("b")` 时，`getAnnotation(Timed.class)` 返回 `null`，只有 `TimedSet` 可见
- 同仓 `TimedFinder`（Jersey binder）会同时处理 `Timed` 和 `TimedSet`，而 AOP 路径没有同样逻辑
- 当前结论：**AOP 路径对重复 `@Timed` 的支持不明确，存在实现风险**

harness：`MiniMI4` **22/22 PASS**

---

### MI-5 `MeterBinder/JVM binders`

核心职责：
- 从 JVM/JMX/线程池等状态源注册 meter
- 是典型的“消费层”而非新原语实现层

关键结论：
- `JvmGcMetrics`：
  - 监听 GC notification
  - 输出 `pause/concurrent.phase/allocated/promoted/live/max/cpu.time`
  - `close()` 必须清理 listener
- `JvmHeapPressureMetrics`：
  - 构造时就挂 listener，`bindTo()` 只负责暴露 gauges
  - 输出 `jvm.memory.usage.after.gc` 与 `jvm.gc.overhead`
- `ExecutorServiceMetrics`：
  - 可监控线程池状态，也可包装 executor 记录任务耗时
  - direct `Executor` 的 timer 名是 `executor.execution` / `executor.idle`
  - `ExecutorService/ScheduledExecutorService` 还会暴露 `executor.queued` 等状态 gauge
  - `metricPrefix` 通过 `sanitizePrefix()` 统一成带 `.` 或空串
  - wrapper 持有 `registeredMeterIds`，shutdown 时负责清理 meter
- `JvmThreadMetrics`：
  - state gauges 依赖 `getAllThreadIds()`
  - 对不支持的 VM 捕获 `Error` 而不是 `Exception`
- `ClassLoaderMetrics/JvmMemoryMetrics/JvmCompilationMetrics` 都是标准 pull 型 binder

harness：`MiniMI5` **16/16 PASS**

---

### MI-6 `Observation`

核心职责：
- 统一抽象“开始/错误/事件/scope/结束”等生命周期
- 让 metrics / tracing / logging 等多个 handler 复用同一埋点

关键结论：
- no-op 分支返回的是 **`NoopButScopeHandlingObservation`**，不是纯 `Observation.NOOP`
- 即便 observation 被 predicate 禁用，也要保留 scope / current observation 传播语义
- `SimpleObservation` 在创建时就：
  - 把当前 scope observation 自动设为 parent
  - 按 `supportsContext(context)` 过滤 handlers 并固化成 deque
- 顺序语义：
  - `onStart/onError/onEvent/onScopeOpened` 正序
  - `onScopeClosed/onStop` 倒序
- `ObservationFilter` **只在 stop 前生效**，不影响 earlier callbacks
- convention 优先级：`custom > global > default`
- `FirstMatchingCompositeObservationHandler` 与 `AllMatchingCompositeObservationHandler` 语义清晰且官方测试覆盖
- `SimpleObservationRegistry.isNoop()`：没有 handlers 也视为 no-op

harness：`MiniMI6` **20/20 PASS**

---

## 3. 跨域共性结论

### 3.1 “上一周期值”是 Step 家族的核心直觉
这一点在多个域里反复打脸后已实证闭环：
- `StepCounter`
- `StepFunctionTimer`
- `StepDistributionSummary`

它们的 `poll()` / `poll1()` / `poll2()` 返回的是 **previous**，不是 current。

### 3.2 no-op 不等于“完全不做事”
两个地方特别容易误判：
- `Observation` 被禁用时，仍保留 scope 传播
- `MeterRegistry` / `Gauge` 里，某些 no-op/weak-reference 行为仍参与线程本地或生命周期恢复

### 3.3 自动注册的 synthetic / derivative metric 很多
- `HistogramGauges.registerWithCommonFormat(...)`
- `Gauge.Builder.synthetic(meter.getId())`
- MI-1 中 `syntheticAssociation` 的唯一来源闭环已经成立

### 3.4 配置 merge 几乎都是 “this 优先”
典型如：
- `DistributionStatisticConfig.merge(parent)`
- `MeterFilter.maxExpected/minExpected` 内部通过 builder+merge 强制覆盖

---

## 4. harness 汇总

| 域 | harness | 结果 |
|---|---|---|
| MI-1 | `MiniMI1` | 25/25 |
| MI-7 | `MiniMI7` | 30/30 |
| MI-2 | `MiniMI2` | 20/20 |
| MI-3 | `MiniMI3` | 20/20 |
| MI-4 | `MiniMI4` | 22/22 |
| MI-5 | `MiniMI5` | 16/16 |
| MI-6 | `MiniMI6` | 20/20 |

累计：**153/153 PASS**

### Review 轮次统计

| 域 | review-notes 轮次 | 说明 |
|---|---:|---|
| MI-1 | 6 | 含多轮锚点、复杂度、因果链与 harness 复核 |
| MI-7 | 7 | 含官方测试逐条对照与注册路径终走查 |
| MI-2 | 1 | 含 Pass0/1/2/3 + 官方 StepCounter/MultiGauge/Timer 交叉；轮次记录较简略 |
| MI-3 | 3 | 含交付物完整性、自动注册、残留清零 |
| MI-4 | 3 | 含 samples 模块交叉与重复 `@Timed` 风险追踪 |
| MI-5 | 2 | 含 JVM binder 官方测试路径盘点 |
| MI-6 | 2 | 含 Observation/no-op/current/composite 官方测试交叉 |

已记录的域级 review 轮次合计：**24 轮**。MI-2 的 review-notes 记录较简略，但其 Pass0→Pass3 与官方测试交叉结论已写入对应 outline/review 文档；这里保留事实，不把“记录轮次”夸大为更多轮。

---

## 5. 当前唯一明确风险

### 重复 `@Timed` 的 AOP 支持不明确
证据链：
1. `@Timed` 声明了 `@Repeatable(TimedSet.class)`
2. `TimedAspect` 使用 `method.getAnnotation(Timed.class)` 读取注解
3. 反射实证：重复 `@Timed` 时 `getAnnotation(Timed.class)==null`，只有 `TimedSet` 存在
4. 同仓 `TimedFinder` 会显式处理 `TimedSet`
5. samples 模块也没有覆盖“重复 `@Timed`”的 AOP 测试

结论：
- 这不是分析残缺，而是源码中真实存在的 **实现/覆盖风险点**

---

## 6. 交付物路径

- `outlines/MI-1-meter-registry/`
- `outlines/MI-7-meter-filter/`
- `outlines/MI-2-counter-gauge-timer/`
- `outlines/MI-3-distribution-summary/`
- `outlines/MI-4-aop-annotations/`
- `outlines/MI-5-meter-binder/`
- `outlines/MI-6-observation/`
- `harness/MI-1/`
- `harness/MI-7/`
- `harness/MI-2/`
- `harness/MI-3/`
- `harness/MI-4/`
- `harness/MI-5/`
- `harness/MI-6/`

---

## 7. 建议的下一动作

如果继续本阶段工作，建议优先级如下：

1. 先对 `HANDOFF-MICROMETER` 本文做多轮深审（锚点/数字/措辞/风险表述）
2. 若需要落到改码层，优先验证/修复 **重复 `@Timed`** 风险
3. 若需要跨框架对照，再把 `Observation` 与 Spring Cloud Commons / Gateway 的 Observation 链做映射
