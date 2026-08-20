# Pass 2 闭环笔记 Q4: Heartbeat 与 MetricController

## 验证过程

- `HeartbeatSender` 只定义两个方法：`sendHeartbeat()` 与 `intervalMs()`，core 的 `HeartbeatSenderInitFunc` 负责按周期调它 (`HeartbeatSender.java:17-36`)。
- 以 `HttpHeartbeatSender` 为例，它实现 `sendHeartbeat()`，把本机信息按 HTTP 心跳协议发往 Dashboard；sender 本身不调度，只负责单次发送。
- `MetricController` 是 Dashboard 读取/聚合指标的入口，背后依赖 `MetricsRepository`（实际默认实现是 `InMemoryMetricsRepository`），负责查询某 app/resource/时间区间的 metric 数据。
- 因此 transport-common 与 dashboard 的配合是：
  - HeartbeatSender：client → dashboard 机器存活/注册
  - CommandCenter + handler：dashboard → client 查询/修改
  - MetricController/Repository：dashboard 本地聚合展示指标

## 结论

心跳、命令、指标是三条不同的数据流：心跳负责机器发现，命令负责规则和状态交互，MetricController 负责 dashboard 侧的指标查询与展示。它们共同构成“client ↔ dashboard”的控制面。