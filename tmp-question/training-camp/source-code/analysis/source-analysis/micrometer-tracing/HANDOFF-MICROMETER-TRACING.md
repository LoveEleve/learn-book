# HANDOFF-MICROMETER-TRACING

> 仓库: `/data/workspace/source-code/code/spring/micrometer-tracing`
> 阶段: 6.2 Micrometer Tracing
> 日期: 2026-08-17
> 方法论: Pass0→Pass1→Pass2→Pass3 / harness / 多轮 review

## 0. 总结

仓库生产代码已按包边界拆为 8 个域：

1. MT-1 Span/Tracer 核心抽象
2. MT-2 传播与上下文适配
3. MT-3 Observation→Tracing 处理链
4. MT-4 注解切面
5. MT-5 Exporter 抽象与过滤
6. MT-6 Brave Bridge
7. MT-7 OpenTelemetry Bridge
8. MT-8 Wavefront Reporter

生产 Java 文件已穷举：core 59、Brave 24、OTel 27、Wavefront 5，共 115 个。tests/simple、integration-test、docs/internal 作为支撑边界，不冒充生产域。

## 1. 域状态

| 域 | 结果 | 状态 |
|---|---:|---|
| MT-1 Span/Tracer | 27/27 harness | 2 轮 review，收敛 |
| MT-2 Propagation | 17/17 harness | 2 轮 review，收敛 |
| MT-3 Observation Handler | 16/16 harness | 2 轮 review，收敛 |
| MT-4 Annotation | 14/14 harness | 2 轮 review，收敛 |
| MT-5 Exporter | 12/12 harness | 2 轮 review，收敛 |
| MT-6 Brave | 官方测试交叉 | setNoParent 已修复并回归 |
| MT-7 OTel | targeted 测试通过 | Boolean list 已修复；local IP 测试已兼容环境 |
| MT-8 Wavefront | targeted test 通过 | deprecated/EOL |

独立 harness 合计：**86/86 PASS**。

## 2. MT-1 Span/Tracer

- `Span` 生命周期：start/name/tag/event/error/end/abandon
- `Tracer`：nextSpan/withSpan/startScopedSpan/spanBuilder/current context/baggage
- `CurrentTraceContext`：newScope/maybeScope/wrap
- `Baggage` 以 scope 为核心，不是普通 KV
- `SpanAndScope.close()` 先关 scope，再 end span
- `ThreadLocalSpan.remove()` 只关闭 scope，不结束 span
- no-op 族保证禁用 tracing 时 API 仍可贯通

## 3. MT-2 Propagation

- `Propagator`：fields/inject/extract
- OTel bridge 覆盖 Getter.getAll 支持多值 header
- span accessor 必须在 Observation accessor 后注册，避免重复创建 span
- baggage accessor 无 current span 时 warn/no-op
- ReactorBaggage 同名 key 后写覆盖前写
- 未关闭 accessor scope 会产生线程状态污染

## 4. MT-3 Observation Handler

- Default handler：parent 选择→创建 span→start→stop 时 name/tag/end
- sender：创建 span→inject carrier→stop
- receiver：extract builder→start→stop
- contextualName 优先于 technical name
- `TracingContext` 维护 span 与 per-thread scope
- `RevertingScope` 恢复 span scope 与 baggage scope
- meter handler 的 onStop 会临时把 span 放回 current scope
- 手动 current child span 优先于 parent Observation span
- baggage 只传播 `tracer.getBaggageFields()` 白名单

## 5. MT-4 Annotation

- `@NewSpan` 一定新建 span
- `@ContinueSpan` 有 current span 时复用；无 current 时退化新建
- 默认方法名转 lower-hyphen
- `SpanTag` 优先级：resolver > expression > toString
- `ContinueSpan.log()` 非空才写 before/after/afterFailure
- 同类自调用受 AOP 代理限制
- processor 捕获 Exception，Error 不走 onFailure 标记路径

## 6. MT-5 Exporter

- `FinishedSpan` 统一承载 name/time/tags/events/IDs/error/kind/links
- `SpanFilter.map` 是 mutation hook
- `SpanExportingPredicate` 决定导出
- 约定顺序：filter map→predicate→reporter
- 名称过滤使用 regex `matches()` 全匹配
- Brave links 通过 tags 编码；OTel links 使用原生 LinkData

## 7. MT-6 Brave Bridge

- parent 通过 `TraceContextOrSamplingFlags`
- BraveSpan.error 额外写 error tag
- baggage close 恢复 previous baggage
- W3C propagation 处理 traceparent/tracestate/baggage
- ProbabilityBasedSampler 是 100 样本窗口近似，不适合 collector 的 trace-id 一致采样
- `setNoParent()` 已显式设置 EMPTY，并补测试验证清除 parent

## 8. MT-7 OTel Bridge

- OTel Context 是 current trace/baggage 载体
- newScope 同时比较 span 与 baggage，相同才返回 noop scope
- SpanBuilder 原生支持 parent/noParent、typed attributes、links、timestamp
- OTel end 在 status unset 时写 OK
- OTel abandon 是 no-op，与 Brave 不对称
- FinishedSpan 使用原生 AttributeKey、LinkData、Resource service.name

**已修复：**
- `OtelFinishedSpan.getAttributeKey(List<Boolean>)` 已修正为 `BOOLEAN_ARRAY`
- 已补 Boolean list 回归测试
- OTel targeted 测试通过

**环境兼容：**
- local IP 测试已允许无 site-local 网卡环境返回 null，同时保留显式 `setLocalIp()` 的非空回归断言

## 9. MT-8 Wavefront

- 有界 queue、daemon sender、heartbeat、derived metrics
- queue full/close 后到达 span：drop+计数
- end 始终返回 true，保证其他 handlers 继续
- close：DeathPill、join(5s)、interrupt、flush、close
- Brave/OTel 分别通过 FinishedSpan 接入
- 模块 deprecated，原因是 Wavefront EOL

## 10. 风险表

| 风险 | 类型 | 状态 |
|---|---|---|
| OTel Boolean list 使用 DOUBLE_ARRAY | 源码类型错配 | 已修复为 BOOLEAN_ARRAY，并补测试回归通过 |
| OTel local IP 测试 | 环境兼容性 | 已兼容无 site-local 网卡环境，并回归通过 |
| Brave setNoParent 未显式清 parent | 源码缺陷 | 已显式设置 EMPTY，并补测试回归通过 |
| Wavefront deprecated/EOL | 产品生命周期 | 已记录 |
| accessor/scope 未关闭 | 使用约束 | 已记录 |

## 11. 后续动作

1. 如需继续费曼验证，为 MT-6~MT-8 增补独立 harness
2. 在后续具备 site-local 网卡的容器环境中做一次可选的 OTel local IP 加强回归
3. Micrometer Tracing 阶段结束后进入下一个框架
4. 保持本交接文档与后续修复状态同步
