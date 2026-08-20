# MT-2 传播与上下文适配 — outline 收敛版

> 核心文件: `Propagator.java` / `BaggageToPropagate.java` / `ObservationAwareSpanThreadLocalAccessor.java` / `ObservationAwareBaggageThreadLocalAccessor.java` / `ReactorBaggage.java`
> harness: `MiniMT2` **17/17 PASS** | 日期: 2026-08-17

## 一、Propagator
- `fields()` 声明传播字段
- `inject(context, carrier, setter)` 写出站 carrier
- `extract(carrier, getter)` 返回 `Span.Builder`
- `Setter` / `Getter` 是无状态策略，可常量复用
- `Getter.getAll()` 默认退化为单次 `get()`；重复 header 必须由 bridge 覆盖
- `Propagator.NOOP`：fields 空、inject 不写、extract 返回 `Span.Builder.NOOP`

## 二、BaggageToPropagate
- map 构造会复制入参 map
- varargs 要求偶数个 key/value，否则 `IllegalArgumentException`
- 内容型 equals/hashCode
- 主要作为 context-propagation / Reactor carrier 中的 baggage 快照

## 三、ObservationAwareSpanThreadLocalAccessor
- 必须在 `ObservationThreadLocalAccessor` 之后注册
- 若 Observation 已通过 `TracingObservationHandler` 创建 span，accessor 退让返回 null
- 若用户又手动创建 child span 并入 scope，则返回用户当前 child span
- 无 current Observation 时直接读取 `tracer.currentSpan()`
- `setValue` 通过 `tracer.withSpan` 创建 per-thread `SpanAction`
- `restore` 关闭 action 并恢复 previous action；异常时检测 scope 污染

## 四、ObservationAwareBaggageThreadLocalAccessor
- 从 current Observation 的 `TracingContext` 或 tracer current span 选择 baggage 所属 span
- `getValue()` 从 `tracer.getAllBaggage(span.context())` 生成 `BaggageToPropagate`
- `setValue(value)`：
  - 无 current span → warn + no-op
  - 有 current span → 每个 entry 创建一个 `BaggageInScope`
  - 多个 baggage scope 组合成可逆关闭链
- `restore` / `restore(value)` 关闭当前链并恢复 previous chain

## 五、ReactorBaggage
- 使用 `ObservationAwareBaggageThreadLocalAccessor.KEY` 存取 Reactor Context
- `append(key,value)` / `append(map)` 都返回不可变 Context 转换函数
- 新 entries 后写，因此同名 key **新值覆盖旧值**

## 六、关键边界
- accessor 的 per-thread map 必须靠 restore/close 清理；未关闭 scope 会造成线程级状态污染
- `BaggageToPropagate.getBaggage()` 暴露内部 map，调用方应把它视为快照而非并发可变状态
- no current span 时 baggage accessor 的 warn/no-op 是设计语义，不是异常传播

## 七、验证
- 官方 `BaggageToPropagateTests`：奇数参数、map copy、varargs
- 官方 `ReactorBaggageTests`：单条、多条、同名覆盖、保留旧 key
- integration tests：Observation+manual span、纯 Observation、纯 span、线程池和 baggage 跨线程
- harness：**17/17 PASS**