# MT-6 Brave Bridge — Pass 0 发现

> 首批深读: `BraveTracer` / `BraveSpan` / `BraveSpanBuilder` / `BraveCurrentTraceContext` / `BraveBaggageManager` / `BraveScopedSpan` / `BraveFinishedSpan`
> 日期: 2026-08-17

## 1. 域职责
- 把 Micrometer Tracing 抽象 API 精确映射到 Brave API
- 核心不是“简单转发”，而是：
  - `TraceContextOrSamplingFlags` 的 parent 表达
  - baggage 字段与当前 span/tag 联动
  - links 在 Brave 缺失原生模型时的编码补偿

## 2. BraveTracer
- 组合 3 个核心依赖：
  - `brave.Tracer`
  - `CurrentTraceContext`
  - `BaggageManager`
- `nextSpan(parent)`：
  - parent 为 null → `nextSpan()`
  - parent 有 Brave context → `brave.tracer.nextSpan(TraceContextOrSamplingFlags.create(context))`
- `withSpan(span)`：`tracer.withSpanInScope(delegate)`
- `startScopedSpan(name)`：直接 `brave.tracer.startScopedSpan(name)`
- `traceContextBuilder()` 返回 `BraveTraceContextBuilder`
- 若 baggageManager 是 `BraveBaggageManager`，构造时会回注 tracer，供 baggage 获取 currentSpan

## 3. BraveSpan
- `event(value)` → `annotate(value)`
- `event(value,time,unit)` → `annotate(micros, value)`
- `error(Throwable)`：
  - 额外写 `error=<message or exception simple name>` tag
  - 再 `delegate.error(throwable)`
- `end(time, unit)` → Brave 的微秒 finish
- `abandon()` 直接映射 Brave abandon

## 4. BraveSpanBuilder
- 先缓存 parent/name/events/tags/error/kind/remote/startTimestamp
- `start()` 时统一组装到 Brave span
- `setParent(context)` 通过 `TraceContextOrSamplingFlags.create(...)`
- **`setNoParent()` 当前只是 `return this`**，没有显式清空 parentContext；需后续验证 Brave 行为是否已足够表达“无 parent”
- `addLink(Link)`：
  - Brave 无原生 link 模型
  - 通过 `LinkUtils.traceIdKey/spanIdKey/tagKey` 编码进 tags

## 5. BraveCurrentTraceContext
- `context()` 读 Brave current context，并包装成 `BraveTraceContext`
- `newScope/maybeScope` 都是 Brave scope 包装
- `wrap(Executor)` / `wrap(ExecutorService)` 分别调用 Brave `executor()` / `executorService()`

## 6. BraveBaggageManager
- 三类字段:
  - `tagFields`
  - `remoteFields`
  - `baggageFields = tagFields ∪ remoteFields`
- `getAllBaggage()` / `getAllBaggage(traceContext)` 都直接委托 Brave `BaggageField.getAllValues...`
- `getBaggage(traceContext, name)`：
  - 若字段不存在返回 null
  - 若存在，用 current span + traceContext 创建 `BraveBaggageInScope`
- `createBaggageInScope(...)` 依赖 `BraveBaggageInScope.makeCurrent(...)`
- `currentSpan()` 优先用回注 tracer；若 tracer 为空，则 fallback 到 `Tracing.current()`
- `close()` 目前是 no-op（只保留历史注释）

## 7. BraveFinishedSpan
- 直接包 `MutableSpan`
- `setTags/setEvents` 都是“先 clear 再写入”策略
- 时间单位统一从 Brave 微秒转 `Instant`
- `getLocalServiceName/setLocalServiceName` Brave 原生支持
- **links 不原生存在**：
  - `getLinks()` 从 tags 反解码
  - `addLink(s)` 编码回 tags
- 说明：Brave bridge 在 links 上是“兼容模拟”而不是原生模型

## 8. 当前待验证问题
- Q1: `setNoParent()` 在 Brave builder 中是否真的表达“无 parent”，还是只是保持默认行为
- Q2: `BraveBaggageInScope` 如何把 baggage 字段同步成 span tags（`tagFields` 语义）
- Q3: `W3CPropagation` / `PropagationFactorySupplier` / sampler 子包是否应纳入同域核心，还是后续作为扩展边界
- Q4: `BraveFinishedSpan.toBrave(FinishedSpan)` 只接受 `BraveFinishedSpan`，是否意味着该 bridge 不支持跨实现 finished span 互转
- Q5: links 编码进 tags 后，对 exporter/filter 的可见性和冲突风险