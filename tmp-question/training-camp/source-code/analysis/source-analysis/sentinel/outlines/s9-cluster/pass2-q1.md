# Pass 2 闭环笔记 Q1: ClusterStateManager 的模式切换与 client/server 启停

## 验证过程

- `ClusterStateManager` 定义三种模式：`CLUSTER_CLIENT=0`、`CLUSTER_SERVER=1`、`CLUSTER_NOT_STARTED=-1`，用 `volatile int mode` 保存 (`ClusterStateManager.java:27-30`)。
- 通过 `SentinelProperty<Integer>` 监听模式切换，静态块里 `InitExecutor.doInit()` + `stateProperty.addListener(PROPERTY_LISTENER)` (`ClusterStateManager.java:36-38`)。
- `setToClient()` / `setToServer()`：
  - 已是目标模式 → 直接返回
  - 否则设置 mode、`sleepIfNeeded()`（限制两次切换间隔不小于 `MIN_INTERVAL`）、记 `lastModified`，然后启动对应组件
- `startClient()`：先停掉 server（若有），再 `TokenClientProvider.getClient().start()` (`ClusterStateManager.java:56-75`)。
- `startServer()`：先停掉 client（若有），再 `EmbeddedClusterTokenServerProvider.getServer().start()` (`ClusterStateManager.java:103-123`)。
- client/server 都通过 SPI provider 解析，provider 在静态块用 `SpiLoader.loadFirstInstance()` 加载 (`TokenClientProvider.java:28-45`, `EmbeddedClusterTokenServerProvider.java:27-44`)。

## 结论

集群模式由 `ClusterStateManager` 统一管理：模式切换是互斥的（client 和 server 不同时启动），通过 property 监听响应配置变化，切换时先停另一模式再启动目标模式。client/server 实例来自 SPI provider，provider 用 `SpiLoader.loadFirstInstance()` 惰性加载。