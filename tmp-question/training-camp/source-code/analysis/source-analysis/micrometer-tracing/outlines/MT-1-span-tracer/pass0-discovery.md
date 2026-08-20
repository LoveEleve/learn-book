# MT-1 Span / Tracer 核心抽象 — Pass 0 发现

> 通读: `Span`(537) / `Tracer`(249) / `TraceContext`(150) / `CurrentTraceContext`(151) / `Baggage`(124) / `BaggageManager`(157) / `BaggageInScope`(64)
> 日期: 2026-08-17

## 1. 域职责
- 这是 Micrometer Tracing 的 **核心抽象层**，定义 tracing 统一 API
- bridge-brave / bridge-otel 都要实现这一层，而非绕开它
- 重点不在具体实现，而在：
  - span 生命周期
  - scope 与 current trace context
  - baggage 的作用域语义
  - no-op 的传播语义

## 2. Span 抽象
- `Span` 不是 builder-only API，而是一个“已存在的单位工作”对象
- 生命周期主轴:
  - `start()`
  - `name(...)`
  - `event(...)`
  - `tag(...)`
  - `error(...)`
  - `end()` / `end(time, unit)`
  - `abandon()` (**结束但不导出**) 
- `isNoop()` 的语义非常重要：
  - 不记录、不上报
  - **但仍应可被注入到出站请求**
- `remoteServiceName(...)` / `remoteIpAndPort(...)` 说明 Span 同时承担 client/server 远端关系建模
- `Span.Kind` 只有四种：`SERVER / CLIENT / PRODUCER / CONSUMER`

## 3. Span.Builder
- 用于“未开始但高度可配置”的 span 构造
- 能力:
  - `setParent(context)`
  - `setNoParent()`
  - `name/tag/event/error`
  - `kind`
  - `remoteServiceName/remoteIpAndPort`
  - `startTimestamp(...)`
  - `addLink(Link)`（1.1.0）
- `start()` 负责 **build and start**
- `Builder.NOOP.start()` 返回 `Span.NOOP`

## 4. Tracer 抽象
- `Tracer extends BaggageManager`
- tracing 主入口:
  - `nextSpan()`：基于 current span 生成 child；若无 current 则起新 trace
  - `nextSpan(parent)`：显式 parent；若 parent 为 null，则退化为 `nextSpan()`
  - `withSpan(span)`：只入 scope，不结束 span
  - `startScopedSpan(name)`：直接创建 + 入 scope，返回 `ScopedSpan`
  - `spanBuilder()` / `traceContextBuilder()`
  - `currentTraceContext()` / `currentSpanCustomizer()` / `currentSpan()`
- `SpanInScope` 只是 scope handle，**close 不会结束 span**
- 文档明确区分:
  - `ScopedSpan`：scope 与 span 生命周期绑定在一起
  - `Span + SpanInScope`：scope 与 span 生命周期分离

## 5. TraceContext 抽象
- 核心字段:
  - `traceId()`
  - `parentId()`
  - `spanId()`
  - `sampled()` (`true / false / null=defer`)
- `TraceContext.NOOP` 返回空 trace/span id、null sampled
- `TraceContext.Builder` 只是最小四元 builder，不带 baggage/links/extra fields

## 6. CurrentTraceContext 抽象
- tracing 里的“当前上下文载体”抽象
- 主能力:
  - `context()`：当前 TraceContext
  - `newScope(context)`：强制切当前 context
  - `maybeScope(context)`：若已是当前 context，则返回 noop scope
  - `wrap(Callable/Runnable/Executor/ExecutorService)`：把任务/执行器包进 trace 传播语义
- `Scope.close()` 只关闭作用域，不结束 span/context 本体
- `NOOP` 实现对 wrap 直接返回原对象

## 7. Baggage 语义
- `Baggage` 代表单个 baggage entry
- 文档明确:
  - 某些 tracer（如 OTel）中的 baggage 可能是 immutable
  - 更新 baggage 可能需要创建新的 scope
- API 分层:
  - 读: `get()` / `get(traceContext)`
  - 旧写法（deprecated）: `set(...)`
  - 新写法: `makeCurrent()` / `makeCurrent(value)` / `makeCurrent(traceContext, value)`
- 也就是说，**baggage 设置的核心语义是“入 scope”**，而不是纯 KV 更新

## 8. BaggageManager
- 负责 baggage entry 的查找、创建、入 scope
- 默认方法关键点:
  - `getAllBaggage(traceContext)`：若 traceContext 为 null → 退回 `getAllBaggage()`；否则默认空 map
  - `createBaggageInScope(name, value)` → `createBaggage(name).makeCurrent(value)`
  - `createBaggageInScope(traceContext, name, value)` → `createBaggage(name).makeCurrent(traceContext, value)`
- 注意：这里 **没有要求 createBaggage(traceContext, name)**；上下文绑定是在 `makeCurrent(...)` 上完成的
- `getBaggageFields()` 1.3.0 新增，默认空 list

## 9. No-op 族
- `Span.NOOP`
- `Span.Builder.NOOP`
- `Tracer.NOOP`
- `TraceContext.NOOP`
- `CurrentTraceContext.NOOP`
- `Baggage.NOOP`
- `BaggageInScope.NOOP`
- 共同点：
  - API 可无异常贯通
  - scope/wrap 语义保留最小空实现
  - 用于在无 tracer/禁用 tracing 场景下保持调用方代码简单

## 10. 待 Pass 1 验证 (Q)
- Q1: `ScopedSpan` / `SpanAndScope` / `ThreadLocalSpan` 各自怎么分工，和 `Tracer.startScopedSpan()` 的关系是什么
- Q2: `abandon()` 与 `end()` 在 bridge-brave/otel 中的实际语义差异
- Q3: `CurrentTraceContext.maybeScope()` 的“已在 scope 则 noop”是否有官方测试覆盖
- Q4: `BaggageManager.getAllBaggage(traceContext)` 默认返回空 map，这意味着“按 traceContext 读 baggage”必须由 bridge 重写，需验证
- Q5: `Span.Builder.addLink(...)` 在 bridge 中是否都落地，还是 default no-op
- Q6: `currentSpanCustomizer()` 为 null 的场景和 `currentSpan()==null` 是否同步出现