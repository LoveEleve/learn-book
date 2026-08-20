# Pass 2 闭环笔记 Q5: 指标导出与 Reactor operator

## 验证过程

- `JMXMetricExporter` 不是直接暴露某个 Node，而是定时执行 `MetricCollector.collectMetric()`，再由 `MetricBeanWriter` 写入 JMX bean；调度周期 1 秒 (`JMXMetricExporter.java:31-63`)。
- `PromExporterInit` 是 `InitFunc`：注册 `SentinelCollector` 后启动 Prometheus `HTTPServer`，默认暴露 `/metrics`，并在 JVM shutdown hook 中停止 (`PromExporterInit.java:24-46`)。
- Reactor adapter 的 `MonoSentinelOperator`/`FluxSentinelOperator` 只是壳，真正的 entry/exit 绑定在 `SentinelReactorSubscriber` 上；`MonoSentinelOperator.subscribe` 直接 new `SentinelReactorSubscriber(entryConfig, actual, true)` (`MonoSentinelOperator.java:39-40`)。
- 这说明 metrics exporter 主线是“定时收集并暴露指标”，reactor 主线是“operator/subscriber 绑定 entry 生命周期”，两者都只是 core 统计结果的消费/包装层。

## 结论

指标导出不参与规则判定，它只是周期性读取 `MetricCollector` 结果并转成 JMX/Prometheus 暴露；Reactor 适配则把 entry 生命周期挂到 subscriber 信号流上。两者都属于“消费 core 能力”的外围模块，而不是 core 本身。