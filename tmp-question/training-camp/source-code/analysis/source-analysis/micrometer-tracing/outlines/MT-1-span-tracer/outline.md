# MT-1 Span / Tracer 核心抽象 — outline 收敛版

> 核心文件: `Span.java` / `Tracer.java` / `TraceContext.java` / `CurrentTraceContext.java` / `Baggage.java` / `BaggageManager.java` / `BaggageInScope.java`
> harness: `MiniMT1` **27/27 PASS** | 日期: 2026-08-17

## 一、域职责
- 这是 tracing 的最小统一抽象层
- Brave / OpenTelemetry bridge 都必须实现它
- 其关键不是 exporter，而是：`Span` 生命周期、scope/current context、baggage 作用域、no-op 贯通语义

## 二、Span
- `Span` 是“已存在的工作单元”，不是 builder 本身
- 生命周期：`start → name/tag/event/error → end|abandon`
- `end()`：结束并记录
- `abandon()`：结束但**不记录**
- `isNoop()`：即便 noop，也应允许出站注入传播
- `remoteServiceName` / `remoteIpAndPort`：允许 span 建模远端服务
- `Kind` 只有四类：`SERVER / CLIENT / PRODUCER / CONSUMER`

## 三、Span.Builder
- 面向“尚未 start、但还需要配置 parent/kind/link/startTimestamp”的场景
- 能力:
  - `setParent` / `setNoParent`
  - `name/tag/event/error`
  - `kind`
  - `remoteServiceName/remoteIpAndPort`
  - `startTimestamp`
  - `addLink(Link)`
- `start()` 负责 build + start

## 四、Tracer
- `Tracer extends BaggageManager`
- 主入口:
  - `nextSpan()`：基于 current span 生成 child；没有 current 就起 root
  - `nextSpan(parent)`：显式 parent
  - `withSpan(span)`：仅入 scope，不结束 span
  - `startScopedSpan(name)`：scope+span 打包
  - `spanBuilder()` / `traceContextBuilder()`
  - `currentTraceContext()` / `currentSpanCustomizer()` / `currentSpan()`
- 关键区分:
  - `ScopedSpan`：scope 和 span 生命周期绑在一起
  - `Span + SpanInScope`：scope 与 span 生命周期分离

## 五、TraceContext / CurrentTraceContext
- `TraceContext` 最小四元组：`traceId / parentId / spanId / sampled`
- `sampled()` 是 tri-state 语义：`true / false / null(defer)`
- `CurrentTraceContext` 负责“当前 trace context”抽象:
  - `newScope(context)`：强制切 current
  - `maybeScope(context)`：若相同 context 已在 scope → 返回 noop scope
  - `wrap(Callable/Runnable/Executor/ExecutorService)`：传播 current context
- `Scope.close()` 只清理作用域，不结束 span

## 六、Baggage / BaggageManager
- `Baggage` 不是普通 KV；它代表一个需要进入 scope 的 baggage entry
- 旧写法 `set(...)` 已 deprecated
- 新写法是 `makeCurrent(...)`
- `BaggageManager` 负责:
  - 查找 baggage
  - 创建 baggage
  - `createBaggageInScope(...)`
  - 获取 baggage fields
- 默认 `getAllBaggage(traceContext)`：
  - `traceContext == null` → 退回 current scope
  - 否则默认空 map
- 因此“按指定 traceContext 取 baggage”必须由具体实现重写

## 七、No-op 族
- `Span.NOOP`
- `Span.Builder.NOOP`
- `Tracer.NOOP`
- `TraceContext.NOOP`
- `CurrentTraceContext.NOOP`
- `Baggage.NOOP`
- `BaggageInScope.NOOP`
- 目标：禁用 tracing 时依然让调用代码顺畅贯通，不抛异常、不要求调用方分支判断

## 八、辅助类型
- `ScopedSpan`：直接面向“当前 span until end()”场景
- `SpanAndScope`：持有 `Span + SpanInScope`，`close()` 时先关 scope 再 `span.end()`
- `ThreadLocalSpan`：在当前线程维护 `ArrayDeque<SpanAndScope>` 栈；`set()` 会 `tracer.withSpan(span)` 入 scope，`remove()` 会 close 当前 scope 但不会 end span

## 九、harness 关键结论
- `Tracer.NOOP` 全表面 API 可安全贯通
- `nextSpan()` 在 simple tracer 中：root span `traceId == spanId`，child 继承 traceId 并写 parentId
- `withSpan()` 只改 current，不结束 span
- `SpanAndScope.close()` = `scope.close() + span.end()`
- `CurrentTraceContext.maybeScope()` 在相同 context 下保持 noop 行为
- `createBaggageInScope(...)` 依赖 current context；simple tracer 中若没有 current span，不适合直接调用无 context 版本
- `getAllBaggage(traceContext)` 必须由实现重写，simple tracer 已覆盖该语义

## 十、边界与已知限制
- `SimpleScopedSpan`（测试实现）并不把 current span 暴露成 `tracer.currentSpan()`，因此它更适合验证抽象 API 的“可用性”，不适合作为 bridge 级语义金标准
- `SimpleTraceContextBuilder` 在 sampled=`null` 的 tri-state 语义上不适合做强验证，因此 MT-1 对 tri-state 结论来自接口设计而非该 test double 的实现细节