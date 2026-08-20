# MT-7 OpenTelemetry Bridge — outline 收敛版

> 核心文件: `OtelTracer` / `OtelSpan` / `OtelSpanBuilder` / `OtelCurrentTraceContext` / `OtelBaggageManager` / `OtelFinishedSpan`
> 日期: 2026-08-17

## 一、OtelTracer
- `nextSpan(parent)` 会临时把 parent 的 OTel Context 设为 current，再用 `spanBuilder("").setParent(...)` 创建 child，finally 恢复旧 Context
- `withSpan(span)` 通过 `OtelCurrentTraceContext.maybeScope(...)`
- `withSpan(null)` 发布 `ScopeClosedEvent`，并切到 invalid span/context，达到清理 tracing/baggage 的效果
- `startScopedSpan(name)`：startSpan + makeCurrent，scope 与 span 由 `OtelScopedSpan` 统一管理
- `currentSpan()` 优先读取自定义 `OtelTraceContext`，否则 fallback 到 OTel `Span.current()`

## 二、OtelSpan
- builder 生成的 span 已经 started，因此 `start()` 返回 this
- typed tag 直接写 OTel AttributeKey
- error：`recordException` + ERROR status
- end：若 status UNSET，先写 OK，再 end
- `abandon()` 当前 no-op；OTel 的 span recording/export 生命周期不提供 Brave 式 abandon 对应操作
- remote service → `peer.service`
- remote IP/port → network peer attributes

## 三、OtelSpanBuilder
- 缓存 parent/noParent/name/attributes/error/kind/start timestamp/links
- `setNoParent()` 显式置 `noParent=true`
- `start()` 下发 parent/noParent、attributes、kind、timestamp、links；start 后再写 error/events
- parent 非空时返回 `SpanFromSpanContext` 包装，保存 parent tracing context
- OTel links 是原生 `LinkData`，不需要 Brave 那种 tag 编码

## 四、OtelCurrentTraceContext
- 以 OTel `Context` + `ContextKey<OtelTraceContext>` 保存 Micrometer trace context
- `newScope(context)` 同时比较目标/current span 与 baggage；两者一致才返回 noop scope
- 不一致时合并 baggage、创建新 Context、makeCurrent，并在 scope close 时恢复 old Context
- `maybeScope(null)` 使用 `Context.root().makeCurrent()` 清空当前 OTel Context
- wrap 族全部委托 `Context.current().wrap(...)`

## 五、OtelBaggageManager
- `remoteFields/tagFields/baggageFields` 三组字段
- `CompositeBaggage` 合并 current Context 与指定 traceContext Context；后层 child key 覆盖 parent
- baggage key 查找大小写不敏感
- remote field metadata 使用 `propagation=unlimited`
- `getAllBaggage(traceContext)` 支持显式 context 读取

## 六、OtelFinishedSpan
- typed tags 保留 OTel AttributeKey 类型
- links 原生映射为 LinkData
- error 通过 `exception` EventData 反推 `AssertingThrowable`
- local service name 读取 OTel Resource 的 `service.name`
- local IP 通过 NetworkInterface 懒加载缓存

## 七、深审发现
- OTel 与 Brave 的 `abandon()` 语义不对称：Brave 真实 abandon，OTel 只能 no-op；调用方不能假设跨 bridge 完全一致
- `OtelSpanBuilder.start()` 先应用 parent、再应用 noParent；同一 builder 同时调用两者时最终由 OTel noParent 设置覆盖 parent，属于“最后配置优先”语义
- **源码可疑点**：`OtelFinishedSpan.getAttributeKey(...)` 对 `List<Boolean>` 返回了 `AttributeKey.doubleArrayKey`，而不是 `booleanArrayKey`；这与 `OtelSpan.tagOfBooleans(...)` 的正确实现不一致，需要作为明确实现风险记录
