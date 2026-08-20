# MT-7 OpenTelemetry Bridge — Pass 0 发现

> 首批深读: `OtelTracer` / `OtelSpan` / `OtelSpanBuilder` / `OtelCurrentTraceContext` / `OtelBaggageManager` / `OtelFinishedSpan`
> 日期: 2026-08-17

## 1. 域职责
- 将 Micrometer Tracing 抽象映射到 OpenTelemetry API/SDK
- 与 Brave bridge 的关键差异在于：OTel Context/Baggage/SpanData/SpanProcessor/Exporter 都有原生模型，不需要大量 tag 编码兼容

## 2. OtelTracer
- 组合 OTel API tracer、`OtelCurrentTraceContext`、event publisher、baggage manager
- `nextSpan(parent)`：
  - parent context 不是当前 OTel Context 时，临时 `makeCurrent()`
  - 用 `spanBuilder("").setParent(...)` 创建 child
  - finally 恢复旧 Context
- `withSpan(span)`：通过 `OtelCurrentTraceContext.maybeScope(...)`
- `withSpan(null)`：发布 `ScopeClosedEvent`，并切到 invalid span/context，清理当前状态
- `startScopedSpan(name)`：`startSpan()` + `span.makeCurrent()`，返回 `OtelScopedSpan`
- `currentSpan()`：优先读取自定义 `OtelTraceContext`，否则读取 OTel `Span.current()`

## 3. OtelSpan
- OTel span builder 已经 start 的 span，故 `start()` 是 no-op 返回自身
- name → `updateName`
- typed tags 直接写 OTel AttributeKey（string/long/double/boolean/array）
- error → `recordException` + `StatusCode.ERROR`
- end → 若 status unset，先设置 `StatusCode.OK`，再 end
- abandon() 当前 **no-op**（与 Brave abandon 不同）
- remote service → `peer.service` attribute
- remote IP/port → network peer attributes

## 4. OtelSpanBuilder
- 缓存 parent/noParent/name/attributes/error/kind/start timestamp/links
- `setNoParent()` 显式设置 `noParent=true`
- start 时：
  - parent 与 noParent 依次应用到 OTel SpanBuilder
  - typed attributes、kind、timestamp、links 全部下发
  - error 在 start 后 recordException + ERROR status
  - annotations 在 start 后 addEvent
- parent 非空时返回 `SpanFromSpanContext` 包装，以保留 parent tracing context

## 5. OtelCurrentTraceContext
- 使用 OTel `Context` + 自定义 `ContextKey<OtelTraceContext>`
- `newScope(context)` 同时比较：
  - current OTel span
  - context 中 span
  - baggage 是否相同
- 若 span+baggage 都相同，返回 noop scope
- 否则合并 current baggage 与目标 context baggage，创建新 OTel Context 并 makeCurrent
- `maybeScope(null)` 使用 `Context.root().makeCurrent()` 清理当前上下文
- wrap Callable/Runnable/Executor/ExecutorService 全部委托 `Context.current().wrap(...)`

## 6. OtelBaggageManager
- `remoteFields` / `tagFields` / union baggageFields
- `CompositeBaggage` 将当前 Context 与指定 traceContext Context 合并；child key 覆盖 parent key
- `getAllBaggage(traceContext)` 支持按显式 OTel context 查找
- remote field 创建时加 metadata `propagation=unlimited`
- `getBaggage` 对 key 匹配大小写不敏感
- `getBaggageFields()` 当前返回 remoteFields

## 7. OtelFinishedSpan 关联
- typed tags 真正保留 OTel AttributeKey 类型
- links 使用 OTel `LinkData`
- error 从名为 `exception` 的 EventData 反推 Throwable 外壳
- local service name 从 OTel Resource 的 `service.name` 读取
- local IP 通过 NetworkInterface 懒加载并缓存

## 8. Pass 1 待验证
- Q1: `OtelSpan.abandon()` no-op 是否符合 OTel Span 生命周期设计
- Q2: `setNoParent()` 与 parent 同时设置时实际优先级
- Q3: `OtelCurrentTraceContext.newScope()` span+baggage 相等判断是否真能返回 noop scope
- Q4: `withSpan(null)` 发布 ScopeClosedEvent 后 baggage/span 是否都清理
- Q5: `CompositeBaggage` child 覆盖 parent 的顺序与大小写行为
- Q6: OTel typed tag list 的 Boolean list AttributeKey 选择是否存在实现错误（源码可疑）