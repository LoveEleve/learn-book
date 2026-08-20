# MT-8 Wavefront Reporter — outline 收敛版

> 核心文件: `WavefrontSpanHandler` / `WavefrontOtelSpanExporter` / `WavefrontBraveSpanHandler` / `SpanMetrics`
> 日期: 2026-08-17

## 一、生命周期
- 构造时启动：有界 span queue、daemon sender thread、heartbeat scheduler、WavefrontInternalReporter
- `end(...)`：收到→入队；stop/queue full→drop+计数；始终返回 true
- sender thread：queue take→send；单个 span/转换异常不会终止发送线程
- `close()`：CAS 防重、DeathPill、最多等待 5s、interrupt、shutdown heartbeat、flush sender、close sender

## 二、转换
- traceId 补零到 128-bit，再转 UUID
- spanId/parentId 转 UUID low bits
- null name → `defaultOperation`
- 时间戳转 millis；正 duration 最少 1ms；micros 用于 RED metrics
- events → Wavefront SpanLog
- TagList：default tags + span tags，过滤空值/默认 tag 冲突，规范化 error/debug/component/span.kind/local ip

## 三、适配器
- Brave handler：`MutableSpan → BraveFinishedSpan → WavefrontSpanHandler.end`
- OTel exporter：`SpanData → OtelFinishedSpan → WavefrontSpanHandler.end`
- OTel `flush()` 直接 success；`shutdown()` 关闭共享 handler

## 四、指标
- SpanMetrics：received / dropped / errors / queue size / remaining capacity
- WavefrontInternalReporter 额外生成 span-derived metrics 与 heartbeat

## 五、状态
- 整个模块已 deprecated（Wavefront EOL since 1.6.0）
- 官方 `WavefrontSpanHandlerTests` 已通过：发送、默认名/ID 转换、close flush/close
- 当前 Pass 0/1 已覆盖核心队列、close、转换与指标契约