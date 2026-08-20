# C-1 temporal-trace — 时空溯源

> 本地 git 为 release 单提交浅克隆 (cbafb53 2025-03-02), 无逐版本演进日志; 以下为代码内证据 + 版本线重建。

## 版本线 (来自 @since 与注释)

| 版本 | 事件 | 证据 |
|---|---|---|
| 2.x | namespace/fluent API 定型; PathChildrenCache StartMode 三值 (2.0.0 起) | @Deprecated 注释 + 网络考证 |
| 3.0.0 | connectionStateErrorPolicy 引入 (可插拔错误策略) | CuratorFrameworkFactory.java:427 "@since 3.0.0" |
| 3.2.0 | schemaSet 引入 | CuratorFrameworkFactory.java:499 |
| 4.0.2 | waitForShutdownTimeoutMs 引入 (close 等待) | CuratorFrameworkFactory.java:440 |
| 4.1.0 | runSafeService 可注入 | CuratorFrameworkFactory.java:515 |
| 4.2.0 | connectionStateListenerManagerFactory (circuitBreaking) | CuratorFrameworkFactory.java:527 |
| 4.2.0 | getListenable 返回类型 ListenerContainer→Listenable | ConnectionStateManager.java:148 |
| 5.0 | simulatedSessionExpirationPercent (会话过期模拟百分位) | CuratorFrameworkFactory.java:479 |
| 5.1.1 | zkClientConfig 支持 (ZK 3.6.1+) | CuratorFrameworkFactory.java:123 |
| 5.8.0 | 当前版本 (本次分析基准) | pom.xml:34 |

## 已知 issue 演进 (代码注释实证)

- **CURATOR-52**: 连接超时检测 — performBackgroundOperation 中未连接操作 sleep 1s 重排 (CuratorFrameworkImpl.java:820-823 注释)
- **CURATOR-405**: 会话过期注入 — SUSPENDED 超时主动 injectSessionExpiration (ConnectionStateManager.java:288 注释)
- **CURATOR-525**: LOST 但已连接 race — 强制 RECONNECTED (ConnectionStateManager.java:266-269 注释)
- **CURATOR-561**: 注入失效兜底 — instanceIndex 相同则 reset (ConnectionStateManager.java:294-296 注释)
- **CURATOR-724**: latchPath 消失重建 (LeaderLatch.java:610-617, C-2 域)

## 设计代际 (架构演进线)

```
ZK 原生客户端 (裸句柄/裸 watcher)
  → Curator 1.x (Netflix): fluent API + 基础配方; 连接管理雏形
  → Curator 2.x: 连接状态机成熟 (SUSPENDED/LOST 语义), PathChildrenCache 三模式定型
  → Curator 3.x: 错误策略可插拔; schema 校验
  → Curator 4.x: close 超时/runSafe 注入/监听器工厂; 并发与关闭竞态加固
  → Curator 5.x: 会话过期自证 (percent), ZK 版本兼容抽象 (zookeeperCompatibility), 
    新缓存 CuratorCache (ZK 3.6+ 持久 watcher) — 框架层趋稳, 重心移到配方代际更替
```

## 教训沉淀 (写作引用)

- RetryLoop 从类改接口会 IncompatibleClassChangeError — **二进制兼容优先于抽象纯粹** (RetryLoop.java:56-58)
- DelayQueue 关闭必须 clearSleep 先行 — **抽象泄漏: 队列实现细节影响关闭流程** (CuratorFrameworkImpl.java:384-390)
- 客户端无法精确复刻服务端会话时钟 → 百分比模拟 — **分布式不可精确 → 用可配置近似** (CuratorFrameworkFactory.java:470-474)
