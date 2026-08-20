# MT-3 Observation→Tracing 处理链 — Pass 0 发现

> 通读: `TracingObservationHandler`(339) / `DefaultTracingObservationHandler`(63) / `PropagatingSenderTracingObservationHandler`(130) / `PropagatingReceiverTracingObservationHandler`(137) / `RevertingScope`(127) / `TracingAwareMeterObservationHandler`(95)
> 日期: 2026-08-17

## 1. 域职责
- 把 Micrometer Observation 生命周期映射到 Tracing span 生命周期
- 负责：创建/复用 parent span、scope 切换、error/event/tag、baggage scope、sender/receiver propagation、meter exemplar tracing access

## 2. TracingObservationHandler 公共骨架
- `tagSpan(context, span)`：遍历 context key values；普通 key → span tag；key=`ERROR` → span.error(RuntimeException(value))
- `getSpanName(context)`：contextualName 非空优先，否则 technical name
- `getParentSpan(context)`：兼容三种 parent 来源：
  - 当前 Observation 自己已有 TracingContext
  - parent Observation 的 TracingContext
  - 用户手动创建并 scope 的 current span
- `onScopeOpened`：从 TracingContext 取 span，通过 `maybeScope` 入 current trace context，并叠加 baggage scopes
- `onScopeClosed`：关闭当前 scope
- `onEvent`：wallTime=0 时用无时间 event，否则带毫秒 timestamp
- `onError`：context 有 error 才转发到 span
- `getRequiredSpan`：未 start 就使用 handler 回调会抛 IllegalStateException
- `endSpan`：先 closing tracing context，再 `span.end()`

## 3. TracingContext
- 放在 Observation.Context 中
- 保存：span、per-thread scope map、Observation ContextView
- 同一个 Observation 可在多个线程持有不同 scope
- `setSpanAndScope` 同时更新 span 与 scope

## 4. DefaultTracingObservationHandler
- `onStart`：从 `getParentSpan` 得到 parent；`nextSpan(parent)`/`nextSpan()` 创建 child/root，start 后保存到 TracingContext
- `onStop`：取 required span → 设置 span name → tag context → end + close

## 5. Sender handler
- 只支持 `SenderContext`
- `onStart`：
  - 创建 `Span.Kind` 与 transport kind 对应的 sender span
  - parent 来自 `getParentSpan`
  - remote service name/address 写入 builder
  - `propagator.inject(child.context(), carrier, setter)`
  - 将 child 保存到 TracingContext
- remote address 通过 URI 解析 host/port；解析失败只 warn，不打断业务
- `onStop`：tag → customize → contextualName/name → end

## 6. Receiver handler
- 只支持 `ReceiverContext`
- `onStart`：把 transport getter 适配成 tracing `Propagator.Getter`，包括 `getAll`
- `propagator.extract(...)` 得到 builder，再设置 receiver kind/remote service/address，customize 后 start
- `onStop` 与 sender 对称，但不做 inject

## 7. RevertingScope
- 关闭顺序：先关闭 current trace scope，再把 TracingContext scope 恢复为 previous
- `maybeWithBaggage(...)`：从 Observation low/high key values 中筛 remote baggage fields，创建 baggage scopes，并把它们和 span scope 组成逆序关闭链
- remote field 匹配使用小写比较

## 8. TracingAwareMeterObservationHandler
- 除 onStop 外，其余生命周期直接 delegate
- onStop 时若有 tracing span，先 `maybeScope(span.context())`，保证 delegate（例如 meter exemplar handler）能读到当前 tracing context，再调用 delegate.onStop

## 9. 待 Pass 1 验证
- Q1: parent 选择顺序：manual current span vs parent Observation tracing span
- Q2: sender inject 是否发生在 span 保存到 Observation Context 之前，receiver extract 是否正确支持多值 header
- Q3: `ERROR` key 被转换成 RuntimeException 是否会丢失原始 Throwable
- Q4: `RevertingScope` baggage scopes 的关闭顺序和 previous scope 恢复是否严格逆序
- Q5: sender/receiver remote URI 非法时是否只 warn 不创建失败
- Q6: `TracingAwareMeterObservationHandler` onStop 的 temporary scope 是否一定关闭