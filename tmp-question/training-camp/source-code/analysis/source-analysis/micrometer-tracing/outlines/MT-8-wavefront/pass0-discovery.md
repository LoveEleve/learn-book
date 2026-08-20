# MT-8 Wavefront Reporter — Pass 0 发现

> 通读: `WavefrontSpanHandler`(515) / `WavefrontOtelSpanExporter`(87) / `WavefrontBraveSpanHandler`(61) / `SpanMetrics`(82)
> 日期: 2026-08-17

## 1. 域职责
- 将 `FinishedSpan` 转成 Wavefront span 协议并异步发送
- 同时产出 Wavefront span-derived metrics / heartbeat metrics
- 当前模块整体标记 `@Deprecated`（Wavefront EOL since 1.6.0）

## 2. WavefrontSpanHandler 生命周期
- 构造时：
  - 建立有界 `LinkedBlockingQueue`
  - 启动 daemon sending thread
  - 启动 heartbeat scheduled executor（每 60s）
  - 启动 WavefrontInternalReporter
  - 注册 queue size / remaining capacity metrics
- `end(context, span)`：
  - `reportReceived()`
  - stop 后到达 → drop + 计数 + warn
  - queue full → drop + 计数 + warn
  - 正常入队
  - 始终返回 true，保证其他 span handlers 继续执行
- `run()`：
  - take queue
  - DeathPill → 退出
  - `send(...)` 失败不杀死发送线程，捕获 Throwable 并继续
- `close()`：
  - CAS 防重复 close
  - 插入 DeathPill
  - 最多等待 5s
  - interrupt sending thread
  - shutdown heartbeat scheduler
  - flush + close WavefrontSender

## 3. Wavefront 转换核心
- traceId 左侧补零到 128-bit，再拆成 UUID 高低部分
- spanId 转为 UUID low bits
- parentId 非空且非零才写 parent UUID
- name null → `defaultOperation`
- 时间:
  - start/end 转 millis
  - 正 duration 至少按 1ms 发送
  - 同时计算 micros 用于 RED/derived metrics
- events → Wavefront `SpanLog(annotation=event value)`
- TagList:
  - 合并 default tags + span tags
  - `error` tag 被规范化为 `error=true`
  - 空 tag value 丢弃
  - default tag key 不允许 span 覆盖
  - `debug/component/span.kind/local ip` 做特殊处理

## 4. Brave / OTel 出口适配
- `WavefrontBraveSpanHandler.end(...)`：`MutableSpan → BraveFinishedSpan → WavefrontSpanHandler.end`
- `WavefrontOtelSpanExporter.export(...)`：每个 `SpanData → OtelFinishedSpan` 后入队
- OTel exporter `flush()` 当前直接 success；`shutdown()` 关闭共用 Wavefront handler

## 5. SpanMetrics
- `reportDropped`
- `reportReceived`
- `reportErrors`
- 注册 queue size / remaining capacity
- 提供 NOOP 实现

## 6. 待 Pass 1 验证
- Q1: queue full / close 后 end 的 drop 计数与 return true 语义
- Q2: DeathPill 与 close 的剩余队列 flush 顺序
- Q3: traceId/spanId 长度、补零和非法 hex 输入边界
- Q4: TagList default tag 覆盖、空值、error/debug/component/span.kind 的优先级
- Q5: OTel exporter flush/shutdown 的 CompletableResultCode 语义
- Q6: heartbeat/RED metrics 与 span 发送失败的错误计数是否分离