# MT-6 Brave Bridge — outline 收敛版

> 首批核心: `BraveTracer` / `BraveSpan` / `BraveSpanBuilder` / `BraveCurrentTraceContext` / `BraveBaggageManager` / `BraveBaggageInScope` / `BraveTraceContext` / `BraveTraceContextBuilder` / `BraveFinishedSpan` / `W3CPropagation` / `PropagationFactorySupplier` / `ProbabilityBasedSampler`
> 日期: 2026-08-17

## 一、域职责
- Brave bridge 不是简单适配壳，而是：
  - `TraceContext` / `Span` / `ScopedSpan` / `CurrentTraceContext` 映射
  - baggage current-span 联动
  - W3C propagation + baggage header 扩展
  - links 的“tag 编码兼容层”
  - sampling 策略

## 二、BraveTracer / BraveSpan / BraveCurrentTraceContext
- `BraveTracer.nextSpan(parent)`：用 `TraceContextOrSamplingFlags.create(braveContext)` 表达 parent
- `withSpan(span)`：包装 Brave `withSpanInScope`
- `startScopedSpan(name)`：直接委托 Brave scoped span
- `currentSpan()`：把 Brave current span 包成 `BraveSpan`
- `BraveSpan.error(...)` 除 `delegate.error(throwable)` 外，还会额外打 `error=<message|simpleName>` tag
- `BraveCurrentTraceContext.wrap(...)` 直接映射 Brave executor/executorService 包装

## 三、BraveSpanBuilder
- 先缓存 parent/name/events/tags/error/kind/remote/startTimestamp
- `start()` 时统一生成 Brave span 并再 start
- `setParent()` 通过 `TraceContextOrSamplingFlags`
- **`setNoParent()` 当前没有显式清空 `parentContext`**，只是 no-op 返回；当前从代码看，它更像“保留 builder 当前状态”，不是强语义的 parent reset
- `addLink(...)` 依赖 `LinkUtils` 把 link 编码到 tags

## 四、BraveBaggageManager / BraveBaggageInScope
- `baggageFields = tagFields ∪ remoteFields`
- `currentSpan()` 优先用回注 tracer；没有则 fallback 到 `Tracing.current()`
- `getAllBaggage(traceContext)` Brave 原生支持
- `createBaggageInScope(...)` 最终都落到 `BraveBaggageInScope`
- `BraveBaggageInScope` 关键语义：
  - 创建时记住 `previousBaggage`
  - `set/makeCurrent` 都更新 Brave baggage field
  - 若 baggage key 在 `tagFields` 白名单中，会同步写到当前 span tags (`Tags.BAGGAGE_FIELD.tag(...)`)
  - `close()` 时把值恢复成 `previousBaggage`
- 若调用时 traceContext 变化，会 debug 提示“unexpected”，但仍更新内部 traceContext 指针

## 五、BraveTraceContext / Builder
- `BraveTraceContext` 只是包装 Brave `TraceContext`
- `BraveTraceContextBuilder` 用 `EncodingUtils.fromString(...)` 解析 hex id：
  - traceId 支持 64-bit 或 128-bit
  - parentId/spanId 只取低位 long
- `sampled(@Nullable Boolean)` 保留 tri-state

## 六、BraveFinishedSpan
- 直接包裹 Brave `MutableSpan`
- `setTags/setEvents` 都是 clear 后重写
- 时间戳单位从 Brave 微秒转 `Instant`
- `links` 无原生字段，全部经 `LinkUtils` 在 tags 中编解码
- local/remote service/ip/port 都能原生落地
- `toBrave(FinishedSpan)` 只接受 `BraveFinishedSpan` 实例，说明它不是通用跨实现转换器

## 七、W3CPropagation
- 负责 Brave 侧 W3C `traceparent` / `tracestate` / `baggage`
- `keys()` 只返回 `traceparent,tracestate`；baggage 由内嵌 `W3CBaggagePropagator` 管
- inject:
  - 写 `traceparent`
  - 若 Brave baggage 中存在 `tracestate`，再写 header
  - 若配置 baggage support，则附带 `baggage` header
- extract:
  - 解析 `traceparent`
  - `tracestate` 解析失败时只 info log，回退为无 state 的上下文
  - 若开启 baggage support，再把 `baggage` header 转成 `BraveBaggageFields`
- `W3CBaggagePropagator`:
  - 注入时忽略 local fields 和 `tracestate`
  - 提取时解析 `key=value(;metadata)?`，坏条目 debug 忽略

## 八、采样与扩展点
- `PropagationFactorySupplier` 只是 `Propagation.Factory` 提供器抽象
- `ProbabilityBasedSampler`：
  - 基于 100 个请求窗口的 BitSet 近似采样
  - 不是 trace-id 一致采样，不适合 collector
  - `probability==0` 恒 false，`==1` 恒 true
- Brave bridge 里 `sampler` / `propagation` 子包应与 core bridge 同域审计，不能丢到“扩展层”不看

## 九、关键边界
- Brave links 是 tag 编码兼容层，不是原生模型
- `setNoParent()` 是否真能“清空 parent”是后续最需要实证的点
- baggage 同步到 span tags 只发生在 `tagFields` 白名单命中时
- `close()` 对 baggage 是恢复旧值，不是删除字段

## 十、第二轮补强
- 官方 `BraveSpanBuilderTests` 已直接证明：parent 绑定、links tag 编码、多值 tags 都按当前实现工作。
- 从源码与测试共同看，`setNoParent()` 在 Brave builder 中没有“清空已有 parentContext”的显式动作；它更像占位 API，实际 builder 一般在无 parent 初始状态下直接使用。
- `W3CBaggagePropagatorTest` 已覆盖 malformed baggage header、重复 key、metadata、value 中含 `=`、localFields 忽略与 inject 行为。
- `BraveTracingApiTests` 直接覆盖了 legacy baggage API 与新 `createBaggageInScope(...)` API 的 in-scope / out-of-scope 语义。
