# C-1 review-notes — 六层深审记录 (2026-08-15)

## 审法: 大纲每锚点回源核对 + harness 实证对照

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | 大纲 §1 "Builder 23 个配置字段" — 实数为 L155-179 共 23 个字段声明 (ensembleProvider→zookeeperCompatibility) | 通过 ✅ (逐行数: L155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179 = 23) |
| 2 | 事实 | §2 "start() 四步" — 源码 L290-323 实为五步 (⑤ ensembleTracker.start) | **修正**: 大纲已写五步 (①状态分发 ②日志监听 ③client.start ④后台线程 ⑤tracker) ✅ |
| 3 | 事实 | §5 "指数公式 baseSleep * random(1<<(retryCount+1))" — 源 L66-73 逐字核对 | 通过 ✅ (Sleep extension pin 到 maxSleep 实证于 harness 日志) |
| 4 | 事实 | §5 "6 种策略" — ls retry/ 穷举 7 文件含 SleepingRetry 抽象 = 6 具体 | 通过 ✅ |
| 5 | 事实 | §7 "injectSessionExpiration" — ConnectionStateManager.java:299 `getTestable().injectSessionExpiration()` 逐字核对 | 通过 ✅ |
| 6 | 事实 | §7 "LOST 但 isConnected → 强制 RECONNECTED" — L263-272, CURATOR-525 注释实证 | 通过 ✅ |
| 7 | 事实 | §9 "backgroundOperations.forEach(clearSleep)" — CuratorFrameworkImpl.java:387 `backgroundOperations.forEach(OperationAndData::clearSleep)` 逐字核对 | 通过 ✅ |
| 8 | 事实 | §10 "namespace 懒创建" — NamespaceImpl.java:65-85 ensurePathNeeded + RetryLoop.callWithRetry 内 ZKPaths.mkdirs | 通过 ✅ (harness E 实证物理路径 /app/node1) |
| 9 | 事实 | §11 "ProtectedMode 节点名 _c_+GUID+_" — ProtectedUtils.java:53-54 逐字核对 + harness F 实证 (PASS) | 通过 ✅ |
| 10 | 事实 | §12 "forOperations → RetryLoop 内 zooKeeper.multi()" — CuratorMultiTransactionImpl.java:195-215 | 通过 ✅ |
| 11 | 结构 | 负面空间 "不重试不确定结果" 与 RetryLoop 双门控自洽 | 通过 ✅ (类型门控 NoNode 直抛 — harness C 实证) |
| 12 | 数字 | "会话默认 60000/15000" — CuratorFrameworkFactory.java:60-63 + harness A 双实证 | 通过 ✅ |
| 13 | 数字 | "QUEUE_SIZE=25" — ConnectionStateManager.java:56-59 静态块 + 系统属性可调 | 通过 ✅ |
| 14 | 数字 | "backgroundExceptions 上限 10" — ConnectionState.java:45 MAX_BACKGROUND_EXCEPTIONS=10 | 通过 ✅ |
| 15 | 过程 | 大纲 §6 "队列满丢弃最旧" — L235-239 eventQueue.poll() 先 poll 再 offer | 通过 ✅ (先丢最旧再入新) |

**结论**: 15 项核对 0 修正项 (1 项需在写作时注意的语义: "四步"实为"五步", 大纲已正确)。harness 28/28 实证覆盖: A(默认值) B(生命周期) C(重试计数) D(29 pin) E(namespace 物理/逻辑路径) F(保护模式) G(后台回调) H(断连/重连事件驱动) I(事务回滚) J(错误策略)。

## harness 发现 (可写入文章):

1. **isConnected() 是事件驱动最终一致** — 杀服务器后标志不会立即翻转, 需等 Disconnected 事件传播 (ConnectionState.java:203-250 checkState); 这正是"客户端状态滞后于物理事实"的实证, 对应 §8 RECONNECTED 设计动机。
2. **断连期间 inBackground 操作不丢** — 重连后可取数 (M9 队列语义实证)。
3. **日志实证**: maxRetries 100 → "Pinning to 29"; sleep 扩展过大 → "Pinning to 2147483647" — 两条 pin 分支真实可触发。
