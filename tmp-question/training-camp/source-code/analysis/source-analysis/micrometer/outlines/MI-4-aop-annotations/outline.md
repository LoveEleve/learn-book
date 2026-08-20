# MI-4 AOP / 注解域 — outline 收敛版

> 核心文件: `TimedAspect.java` / `CountedAspect.java` / `Timed.java` / `Counted.java` / `MeterTag.java` / `MeterTagSupport.java` / `MeterTagAnnotationHandler.java` / `CountedMeterTagAnnotationHandler.java` / `MeterTags.java` / `TimedSet.java`
> harness: `io.micrometer.core.aop.MiniMI4` **22/22 PASS** | 日期: 2026-08-17

## 一、域职责
- 该域负责把注解语义桥接为 `Timer / LongTaskTimer / Counter` 的 builder 调用
- **不实现底层计量**；底层记录仍由 MI-2/MI-3 处理
- 主轴：
  - `@Timed` → `TimedAspect`
  - `@Counted` → `CountedAspect`
  - `@MeterTag` → `MeterTagSupport` + 两个 AnnotationHandler

## 二、TimedAspect
- 默认 metric 名：`method.timed`
- 默认 exception tag 值：`none`
- 两个切点：
  - `timedClass(...)`：类上 `@Timed` 且方法上无 `@Timed`
  - `timedMethod(...)`：方法上 `@Timed`
- `perform(...)` 分流：
  - `longTask=false` → `processWithTimer(...)`
  - `longTask=true` → `processWithLongTaskTimer(...)`
- `CompletionStage` 场景：不是方法返回就 stop，而是 `whenComplete(...)` 才记录/停止
- `recordBuilder(...)` 负责把注解翻译成 `Timer.Builder`：
  - `description`
  - `extraTags`
  - `exception`
  - join point tags (`class`, `method` 默认)
  - `histogram` / `percentiles` / `serviceLevelObjectives(seconds→Duration→nanos)`
  - `@MeterTag` 参数/返回值 tags（若配置 handler）
- `getExceptionTag(...)`：异步异常优先取 `throwable.getCause()` 的简单类名

## 三、TimedAspect 边界行为
- `makeSafe(tagsBasedOnJoinPoint)`：provider 抛异常时只记 warn/debug，回退 `Tags.empty()`
- `buildLongTaskTimer(...)`：创建失败返回 `Optional.empty()`，不打断业务流
- `stopTimer(...)`：吞掉 `sample.stop()` 异常
- `record(...)`：`sample.stop(builder.register(registry))` 失败只打日志 `Failed to record.`

## 四、CountedAspect
- 默认 metric 名：`method.counted`（来自注解默认值）
- 固定 tag：
  - `result=success|failure`
  - `exception=<simpleName|none>`
- `recordFailuresOnly=true`：成功路径完全不记
- `CompletionStage` 场景：完成后再根据结果计 success/failure
- 异步失败：若 throwable 有 cause，优先用 cause 的简单类名
- `record(...)`：`Counter.builder(...).register(registry).increment()`

## 五、@Timed / @Counted / @MeterTag 语义
### `@Timed`
- `value()` 为空时，Aspect 用 `method.timed`
- `longTask()` 切换到 `LongTaskTimer`
- `serviceLevelObjectives()` 单位是 **seconds**
- `@Repeatable(TimedSet.class)`

### `@Counted`
- `value()` 默认 `method.counted`
- `recordFailuresOnly()` 默认 false
- 支持 `extraTags()` / `description()`

### `@MeterTag`
- value 解析优先级：`resolver()` > `expression()` > `argument.toString()`
- value 最终为 null 时回退空串 `""`
- key 优先级：`value()` 非空优先，否则 `key()`
- `@Repeatable(MeterTags.class)`
- 只能作用于 `PARAMETER` / `METHOD`
- 文档明确要求 **low-cardinality** 值

## 六、MeterTagSupport / AnnotationHandler
- `MeterTagSupport.resolveTagKey(...)`：`value()` 优先于 `key()`
- `MeterTagSupport.resolveTagValue(...)`：
  - resolver 非 `NoOpValueResolver` → 用 resolver
  - 否则 expression 非空 → 用 `ValueExpressionResolver`
  - 否则 argument 非 null → `toString()`
  - 否则空串
- `MeterTagAnnotationHandler` 绑定 `Timer.Builder`
- `CountedMeterTagAnnotationHandler` 绑定 `Counter.Builder`
- 两者逻辑同构，仅 builder 类型不同

## 七、测试与验证边界
- 仓内官方测试已迁移：
  - `TimedAspectTest` / `CountedAspectTest` / `MeterTagSupportTests` 在当前模块仅保留“已迁移到 samples module”的壳文件
  - 本地仍保留 `NullMeterTagSupportTests`，验证空串回退
- 因此 MI-4 需要依赖自建 harness 做主体验证
- harness 已覆盖：
  - sync success/failure
  - async completion success/failure
  - skip predicate
  - `recordFailuresOnly`
  - `@MeterTag` resolver / expression / toString / null
  - longTask path

## 八、关键收敛结论
- `TimedAspect` 与 `CountedAspect` 都把“异步完成”当成真正记录时点
- 同步异常与异步异常都以“cause 优先”策略构造 exception tag（Counted async/Timed async）
- `@MeterTag` 逻辑无状态，核心是解析优先级与空值回退
- 该域的最大风险点不是计量实现，而是：切点去重、异步完成时机、builder tag 组合顺序、以及 low-cardinality 约束

## 九、额外深审发现
- `@Timed` 虽声明为 `@Repeatable(TimedSet.class)`，但 `TimedAspect.timedMethod()`/`timedClass()` 读取注解时使用的是 `getAnnotation(Timed.class)`，而 Java 反射在“重复注解”场景下返回的是 `TimedSet` 容器，`getAnnotation(Timed.class)` 为 `null`。
- 这意味着 **TimedAspect 对重复 `@Timed` 的支持至少不是显式实现的**；与 Jersey 侧 `TimedFinder` 会同时检查 `Timed` 与 `TimedSet` 形成对照。
- 当前结论：`@Repeatable` 元数据存在，但 AOP 路径对重复 `@Timed` 是否真正生效存在实现风险，需要在 samples module 或运行时织入环境中单独确认。
