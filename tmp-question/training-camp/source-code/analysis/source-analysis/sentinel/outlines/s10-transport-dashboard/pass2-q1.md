# Pass 2 闭环笔记 Q1: CommandCenter/HeartbeatSender 的 SPI 加载与启动

## 验证过程

- `CommandCenterProvider` 在静态块中用 `SpiLoader.of(CommandCenter.class).loadHighestPriorityInstance()` 解析最高优先级 CommandCenter (`CommandCenterProvider.java:23-39`)。
- `CommandCenterInitFunc` 在 `@InitOrder(-1)` 阶段运行：拿到 provider 返回的实例后先 `beforeStart()`，再 `start()` (`CommandCenterInitFunc.java:18-33`)。
- `HeartbeatSenderProvider` 同样用 `loadHighestPriorityInstance()` 解析 sender (`HeartbeatSenderProvider.java:22-38`)。
- `HeartbeatSenderInitFunc` 也在 `@InitOrder(-1)` 阶段运行：
  1. 取 sender
  2. 初始化 `ScheduledThreadPoolExecutor`
  3. 解析心跳间隔（优先读 `TransportConfig.HEARTBEAT_INTERVAL_MS`，否则用 sender 默认值）
  4. `scheduleAtFixedRate` 周期调用 `sender.sendHeartbeat()` (`HeartbeatSenderInitFunc.java:31-88`)

## 结论

CommandCenter 和 HeartbeatSender 都不是硬编码实现，而是通过 SPI 选出最高优先级实例，再由全局 InitFunc 启动。Transport 模块的“默认实现”来自 SPI 解析结果，而不是 core 手写选择。