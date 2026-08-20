# MT-2 传播与上下文适配 — Pass 0 发现

> 通读: `Propagator.java` / `BaggageToPropagate.java` / `ObservationAwareSpanThreadLocalAccessor.java` / `ObservationAwareBaggageThreadLocalAccessor.java` / `ReactorBaggage.java`
> 日期: 2026-08-17

## 1. 域职责
- 负责 tracing/baggage 在不同 carrier、Observation scope、ThreadLocal、Reactor Context 之间传播
- 不创建 span 本体；创建/管理仍由 Tracer/Observation handler/bridge 完成

## 2. Propagator 契约
- `fields()`：声明传播字段
- `inject(context, carrier, setter)`：向下游 carrier 写字段
- `extract(carrier, getter)`：从上游 carrier 返回 `Span.Builder`
- `Setter` / `Getter` 都是无状态策略接口，可作为常量保存
- `Getter.getAll(...)` 默认退化为一次 `get(...)`，实现应在重复 header 场景自行覆盖
- `Propagator.NOOP`：空 fields、inject 不操作、extract 返回 `Span.Builder.NOOP`

## 3. BaggageToPropagate
- 对 baggage map 做副本包装，避免直接持有调用方 map
- varargs 构造要求 key/value 偶数个，否则抛 `IllegalArgumentException`
- 当前 `getBaggage()` 返回内部 map 本身，是否可变需要列为实现边界验证
- equals/hashCode 基于 map 内容

## 4. ObservationAwareSpanThreadLocalAccessor
- 必须在 `ObservationThreadLocalAccessor` **之后注册**
- 设计是避免同一个 Observation 产生两个 span:
  - OTLA 已由 `TracingObservationHandler` 创建 span → tracing accessor 退让，返回 null
  - 用户手动创建 child span 并入 scope → 返回用户当前 span
  - 无 current Observation → 返回 tracer.currentSpan()
- `setValue(span)`：`tracer.withSpan(value)` + per-thread `SpanAction` 栈
- `restore(...)`：关闭当前 action 并校验恢复后的 current span；不一致时 warn + assertion
- `spanActions` 使用 `Map<Thread, SpanAction>`，必须依赖 restore/close 清理，否则可能保留线程 key

## 5. ObservationAwareBaggageThreadLocalAccessor
- 从 current Observation 的 `TracingContext` 或 tracer.currentSpan() 选择 baggage 所属 span
- `getValue()`：读取 `tracer.getAllBaggage(span.context())`，空则返回 null
- `setValue(BaggageToPropagate)`：
  - 要求当前有 span；没有 span 只 warn 并不设置
  - 每个 baggage entry 建立一个 `BaggageInScope`
  - 组合成可逆 `BaggageAndScope` 链
- `restore/restore(value)`：关闭当前 baggage scope，恢复之前的 scope
- `baggageInScope` 是 per-thread map，生命周期依赖 restore/close

## 6. ReactorBaggage
- 只操作 Reactor `Context` 中的 key：`ObservationAwareBaggageThreadLocalAccessor.KEY`
- `append(key,value)` 与 `append(map)` 都是不可变 context 转换函数
- 合并语义：旧 baggage 先放入，新 entries 后 `putAll`，**新值覆盖同名旧值**
- 最终重新包装成 `BaggageToPropagate`

## 7. 待 Pass 1 验证
- Q1: `Getter.getAll()` 默认一次读取是否被 Brave/Otel propagator 正确覆盖
- Q2: `BaggageToPropagate.getBaggage()` 暴露内部可变 map 是否为有意设计
- Q3: span accessor 在 Observation 已创建 span、用户又创建 child span 两种路径的精确退让语义
- Q4: accessor 的 per-thread map 是否在 restore 后严格清理，异常路径是否可能泄漏
- Q5: baggage accessor 无 current span 时的 warn/no-op 语义与官方测试是否一致
- Q6: ReactorBaggage 同名 key 覆盖、空 map、原 Context 不变性是否完整覆盖