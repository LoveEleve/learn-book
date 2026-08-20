# MI-4 AOP / 注解域 — Pass 0 发现

> 通读: `TimedAspect`(378) / `CountedAspect`(312) / `Timed`(94) / `Counted`(69) / `MeterTag`(72) / `MeterTagSupport`(64) / `MeterTagAnnotationHandler`(55) / `CountedMeterTagAnnotationHandler`(57) / `MeterTags`(46) / `TimedSet`(34)
> 日期: 2026-08-17

## 1. 域边界
- 这是 **注解→AOP→Meter Builder** 的桥接层，不直接实现计量逻辑；真正计量仍落到 MI-2/MI-3 的 `Timer/Counter/LongTaskTimer`。
- 主入口:
  - `TimedAspect`：处理 `@Timed`
  - `CountedAspect`：处理 `@Counted`
  - `MeterTagAnnotationHandler` / `CountedMeterTagAnnotationHandler`：把 `@MeterTag` 解析成 builder tags

## 2. TimedAspect 主流程
- 默认 metric 名: `method.timed`
- 默认异常 tag 值: `none` (`KeyValue.NONE_VALUE`)
- 可注入三类策略:
  - `MeterRegistry`
  - `tagsBasedOnJoinPoint`
  - `shouldSkip`
- `makeSafe(...)`：包装 tags provider，若 provider 抛异常 → warn/debug log + `Tags.empty()`
- 两个切点:
  - `timedClass(...)`：类上 `@Timed`，且方法上没有 `@Timed`
  - `timedMethod(...)`：方法上 `@Timed`
- `perform(...)`：
  - `timed.longTask()==false` → `processWithTimer(...)`
  - `true` → `processWithLongTaskTimer(...)`
- `CompletionStage` 特判：不是方法返回即 stop，而是 `whenComplete(...)` 时再记录/停止

## 3. TimedAspect / Timer 路径
- `Timer.Sample sample = Timer.start(registry)`
- 非异步路径：`finally` 中 `record(...)`
- 异步路径：
  - `pjp.proceed()` 成功拿到 `CompletionStage`
  - 在 `whenComplete(...)` 里 `record(...)`
- `record(...)`：`sample.stop(recordBuilder(...).register(registry))`
- `recordBuilder(...)` 构造 tag:
  - `timed.extraTags()`
  - `exception=<异常简单类名|none>`
  - join point tags (`class`, `method` 默认)
  - histogram / percentiles / SLO(seconds→nanos via `TimeUtils.secondsToUnit`)
  - 若配置了 `meterTagAnnotationHandler`，再追加参数/返回值注解 tags
- `getExceptionTag(...)`：若 throwable 有 cause，优先取 `cause.getClass().getSimpleName()`

## 4. TimedAspect / LongTaskTimer 路径
- `buildLongTaskTimer(...)` 安全创建：builder/register 异常 → `Optional.empty()`，不打断业务
- `sample = LongTaskTimer.start()`
- 同步路径：`finally sample.stop()`
- 异步路径：`CompletionStage.whenComplete(...)` 里 `sample.stop()`
- `stopTimer(...)` 吞掉 stop 异常（故意忽略）

## 5. CountedAspect 主流程
- 默认 metric 名来自 `@Counted.value()`，默认值在注解里是 `method.counted`
- 结果 tag:
  - `result=success`
  - `result=failure`
- 异常 tag:
  - `exception=<异常简单类名|none>`
- `recordFailuresOnly=true` 时：只记失败，不记成功
- 同样支持:
  - `tagsBasedOnJoinPoint`
  - `shouldSkip`
  - `CountedMeterTagAnnotationHandler`
- `CompletionStage` 特判：完成后才记 success/failure
- `record(...)`：构造 `Counter.Builder`，`register(registry).increment()`

## 6. 注解语义
### `@Timed`
- `value()` 可空；空则 `TimedAspect` 用 `method.timed`
- `longTask()` 切换到 `LongTaskTimer`
- `percentiles()` / `histogram()` / `serviceLevelObjectives()`
- SLO 单位是 **seconds**（Aspect 里转换成 Timer.Builder 的 `Duration`）
- `@Repeatable(TimedSet.class)`

### `@Counted`
- 默认 metric 名: `method.counted`
- `recordFailuresOnly()` 默认 false
- 支持 `extraTags()` / `description()`

### `@MeterTag`
- 三层优先级:
  1. `resolver()`
  2. `expression()`
  3. `argument.toString()`
- 若最终 value 为 null → `""`
- key 优先级: `value()` 非空优先，否则 `key()`
- `@Repeatable(MeterTags.class)`
- 可打在 `PARAMETER` 和 `METHOD`

## 7. AnnotationHandler 适配层
- `MeterTagAnnotationHandler` 绑定 `Timer.Builder`
- `CountedMeterTagAnnotationHandler` 绑定 `Counter.Builder`
- 两者核心逻辑相同：`KeyValue.of(resolveTagKey(...), resolveTagValue(...))`

## 8. 待 Pass 1 验证 (Q)
- Q1: `TimedAspect` 的 `@Repeatable @Timed` 是否真的都生效，还是 Aspect 只吃到一个注解？
- Q2: `timedClass` 与 `timedMethod` 的优先级/去重：类注解 + 方法注解同时存在时是否双记？
- Q3: `CountedAspect.recordFailuresOnly` 在 `CompletionStage` 场景是否严格只记 failure
- Q4: `buildLongTaskTimer()` 失败吞异常 vs `record()` 失败打日志，边界差异是否合理且已被测试覆盖
- Q5: `MeterTagSupport.resolveTagValue()` 中 resolver / expression / toString 三层优先级的测试覆盖情况
- Q6: `getExceptionTag()` / `recordCompletionResult()` 是否都取 cause 优先，和同步异常路径是否一致