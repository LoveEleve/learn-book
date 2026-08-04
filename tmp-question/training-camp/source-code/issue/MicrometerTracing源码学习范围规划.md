# Micrometer Tracing 源码学习范围规划

> **版本**: main 分支
> **仓库**: `/data/workspace/source-code/code/spring/micrometer-tracing/`
> **规模**: 149 个源文件，桥接 Micrometer ↔ Brave/OpenTelemetry
> **日期**: 2026-08-03

---

## 一、仓库概况

Micrometer Tracing 是 Spring Cloud Sleuth 的继任者，提供 **Tracer → Span → Baggage → Propagation** 四层追踪抽象 + Brave(默认)/OpenTelemetry 桥接实现。Spring Boot 3.x 集成后，`@NewSpan/@ContinueSpan` 自动创建 Span，`BaggageField` 跨线程传播。

**核心包**：

| 包 | 职责 | 关键类 |
|---|---|---|
| `tracing/` root | Tracer/Span/SpanInScope/CurrentTraceContext | Tracer, Span, BaggageInScope, ThreadLocalCurrentTraceContext |
| `handler/` | 事件处理器——DefaultTracingObservationHandler | TracingObservationHandler |
| `propagation/` | 上下文传播——W3C TraceContext/B3 | Propagation, TraceContext |
| `exporter/` | 导出——SpanFilter/Reporter | SpanHandler, SpanFilter |
| `annotation/` | @NewSpan/@ContinueSpan 注解 | NewSpan, ContinueSpan |
| `contextpropagation/` | Context Propagation API 适配 | ContextSnapshot |

---

## 二、知识域规划

### 🔴 核心域（2 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| T-1 | **Tracer + Span 模型** | Tracer, Span, SpanInScope, CurrentTraceContext | **Span 生命周期**：`tracer.nextSpan()` → `span.start()` → `spanInScope = tracer.withSpan(span)`(MDC/ThreadLocal) → `span.event("event")` → `span.tag("key","val")` → `span.error(e)` → `span.end()`；**Span 类型**：`Span` 接口——`BraveSpan`(Brave RealSpan 包装)、`OtelSpan`(OpenTelemetry)、`NoopSpan`(测试)；**CurrentTraceContext**：`ThreadLocalCurrentTraceContext` 线程绑定——`try(Scope scope = context.newScope(currentSpan))` 自动传播 |
| T-2 | **Propagation 上下文传播** | Propagation, TraceContext, HttpClientHandler, HttpServerHandler | **注入/提取**：`HttpClientHandler.handleSend(request)` 注入 traceId/spanId→HTTP Headers（`X-B3-TraceId/X-B3-SpanId` B3 格式或 `traceparent` W3C 格式）→ `HttpServerHandler.handleReceive(request)` 提取→恢复 Span；**Baggage**：`BaggageField.create("userId")` → `baggage.set("123")` → Propagation 自动编码→跨服务传播→`baggage.get()` 读取 |

### 🟡 扩展域（1 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| T-3 | **Observation API 集成** | DefaultTracingObservationHandler, TracingObservationConvention | **Micrometer Observation ↔ Tracing**：`DefaultTracingObservationHandler` 监听 `Observation.start()/stop()/error()` → 自动创建/结束 Span——Spring Boot 3 的 `ObservationAutoConfiguration` 统一了 Metrics+Tracing |

---

## 三、淘汰

| 模块 | 理由 |
|---|---|
| `internal/` `docs/` | 内部实现/文档 |
| `brave/` `otel/` 桥接具体实现 | 理解接口即可 |
| `micrometer-tracing-bridge-*` `micrometer-tracing-reporter-*` | 桥接/上报模块 |
| `micrometer-tracing-integration-test/` `benchmarks/` | 测试/基准 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 2 |
| 🟡 扩展域 | 1 |
| **总域** | **3** |

以上规划完成，共 **2🔴+1🟡=3 域**。149 文件——聚焦 Tracer→Span→Propagation 链。
