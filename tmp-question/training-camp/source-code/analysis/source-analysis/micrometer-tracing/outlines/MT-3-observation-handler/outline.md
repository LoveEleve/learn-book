# MT-3 Observation→Tracing 处理链 — outline 收敛版

> 核心文件: `TracingObservationHandler.java` / `DefaultTracingObservationHandler.java` / `PropagatingSenderTracingObservationHandler.java` / `PropagatingReceiverTracingObservationHandler.java` / `RevertingScope.java` / `TracingAwareMeterObservationHandler.java`
> harness: `MiniMT3` **16/16 PASS** | 日期: 2026-08-17

## 一、域职责
- 把 `Observation` 生命周期映射到 tracing span 生命周期
- 负责 parent 选择、span 创建、scope 切换、error/event/tag、sender/receiver propagation、以及 meter handler 的 tracing 可见性

## 二、TracingObservationHandler 公共骨架
- `tagSpan(context, span)`：遍历 context key values；普通 key→tag，`ERROR` key→`span.error(new RuntimeException(value))`
- `getSpanName(context)`：`contextualName` 优先，fallback 到 technical `name`
- `getParentSpan(context)`：优先级是
  1. 当前 context 自己已有 `TracingContext`
  2. parent Observation 的 `TracingContext`
  3. 若存在手动 current span 且与 observation span 不同，则取手动 current span
- `onScopeOpened`：基于 span.context() 调 `currentTraceContext().maybeScope(...)`，再用 `RevertingScope` 管 previous scope 与 baggage scopes
- `onScopeClosed`：关闭当前 scope，但不清空 span 本体
- `onEvent`：`wallTime==0` 用无时间 event，否则毫秒 timestamp
- `onError`：context 有 error 才写入 span
- `endSpan`：先 `TracingContext.close()`，后 `span.end()`

## 三、TracingContext
- 存在于 `Observation.Context` 中
- 保存：
  - `Span span`
  - `Map<Thread, CurrentTraceContext.Scope> scopes`
  - `Observation.ContextView context`
- 同一 observation 可被多个线程持有不同 scope
- `setSpanAndScope(span, scope)` 是 span/scope 同步更新入口

## 四、DefaultTracingObservationHandler
- `onStart`：
  - 有 parent → `nextSpan(parent)`
  - 无 parent → `nextSpan()`
  - start 后放入 `TracingContext`
- `onStop`：
  - `getRequiredSpan()`
  - `span.name(getSpanName(...))`
  - `tagSpan(...)`
  - `endSpan(...)`
- 若未 start 就触发生命周期（例如 event）会抛 `IllegalStateException`

## 五、Sender / Receiver 处理器
### Sender
- 只支持 `SenderContext`
- `onStart`：
  - 创建 kind 对应的 sender span
  - 可设置 remote service name/address
  - 注入 carrier
  - 保存 span 到 `TracingContext`
- URI 解析失败只 warn，不中断业务
- `onStop`：tag → customize → name/contextualName → end

### Receiver
- 只支持 `ReceiverContext`
- `onStart`：
  - 从 carrier extract 出 `Span.Builder`
  - 适配 transport getter 的 `getAll`
  - 设置 kind / remote service / remote address
  - customize 后 start
- `onStop` 与 sender 对称，但不 inject

## 六、RevertingScope
- 关闭顺序：先关当前 trace scope，再恢复 previous scope
- `maybeWithBaggage(...)`：
  - 根据 `tracer.getBaggageFields()` 白名单，从 Observation key values 中筛 baggage
  - 为每个 baggage key/value 建立 `BaggageInScope`
  - 与 span scope 组合成逆序关闭链
- remote baggage 字段匹配是**大小写不敏感**（lower-case 比较）

## 七、TracingAwareMeterObservationHandler
- 除 `onStop` 外，其他生命周期直接委托给 delegate
- `onStop` 时如果 Observation 已持有 tracing span：
  - 先 `maybeScope(span.context())`
  - 在这个临时 scope 内执行 delegate.onStop
- 目的：让 meter handler（例如 exemplars）在 stop 时能读到 tracing context

## 八、验证要点
- 官方 `TracingObservationHandlerTests` 覆盖：
  - 未 start 即 event → `IllegalStateException`
  - stacked scopes close 顺序
  - null scope 支持
  - close scope 不覆盖 span
  - event timestamp 分支
- 官方 `TracingAwareMeterObservationHandlerTests` 覆盖：
  - 委托路径
  - onStop 临时 scope 可见性
- harness `MiniMT3` 覆盖：
  - default handler start/stop/error/tag/name
  - parent 继承
  - sender inject + receiver extract
  - meter handler 临时 scope

## 九、补强结论
- 当 parent Observation 已有 tracing span，但用户又手动把 child span 放进当前 scope 时，`getParentSpan(...)` 会优先选择**手动 current span**，而不是 parent Observation 的 span。
- `RevertingScope.maybeWithBaggage(...)` 只传播 `tracer.getBaggageFields()` 白名单内的 key，且大小写不敏感；不在白名单中的 Observation key values 不会转成 baggage。
