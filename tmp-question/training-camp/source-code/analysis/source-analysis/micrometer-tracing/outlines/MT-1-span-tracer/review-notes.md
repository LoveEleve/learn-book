# MT-1 Span / Tracer 核心抽象 — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2 + harness 收敛)
- [x] 通读 7 个核心抽象文件 + 3 个 simple test-double 实现 (`SimpleTracer` / `SimpleCurrentTraceContext` / `SimpleSpanBuilder`)
- [x] `Span` / `Tracer` / `CurrentTraceContext` / `BaggageManager` 的主轴语义已闭环
- [x] `withSpan` vs `startScopedSpan` vs `SpanAndScope` vs `ThreadLocalSpan` 的职责边界已明确
- [x] no-op 族语义已通过 `NoopTracerTests` 与 harness 双重确认
- [x] `getAllBaggage(traceContext)` 默认空 map → 需要实现层重写，这一点已通过 simple tracer 验证
- [x] harness `MiniMT1` **24/24 PASS**

## 关键打脸 / 易错点
- 打脸1：`SimpleScopedSpan` 测试实现并不会把 current span 暴露成 `tracer.currentSpan()`，所以不能把它当成 bridge 级 current-scope 金标准
- 打脸2：`SimpleTraceContextBuilder` 对 sampled=`null` 的 tri-state 不适合做强断言，MT-1 这点应回到接口契约层理解
- 打脸3：`createBaggageInScope(name, value)` 在 simple tracer 中依赖 current context，没有 current span 时会 NPE；这是 test double 限制，不应外推为抽象 API 保证

## 收敛判定
- 第一轮收敛；下一轮应做官方测试交叉 + 抽象/实现分层风险审查，避免把 simple test 实现误当成正式语义。

## 审查轮次: 第二轮 (2026-08-17, 官方测试/辅助类型/生命周期补强)
- [x] `NoopTracerTests` 对照：Span/Tracer/Builder/CurrentTraceContext 全表面 noop 调用均不抛异常
- [x] `SimpleSpanBuilderTests` 对照：parent、links、tag/event/error、start timestamp 路径一致
- [x] `SimpleCurrentTraceContext` 对照：`maybeScope` 相同 context 返回 `Scope.NOOP`，不同 context 才切 scope
- [x] `SpanAndScope.close()` 实读验证：先 scope.close，再 span.end
- [x] `ThreadLocalSpan` 实读+实证：栈顶 get，remove 只 close scope、不结束 span
- [x] `abandon()` 实读+实证：simple span 标记 abandoned，与正常 end 分离
- [x] harness 从 24/24 补强至 **27/27 PASS**

## 收敛判定 (第二轮终)
MT-1 核心抽象、辅助类型、no-op、scope、parent、baggage、link、abandon 已完成闭环。剩余 bridge 差异留给 MT-6/MT-7，不在 MT-1 抢先下结论。
