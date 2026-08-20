# N-12 推送面 — 变更通知的 executor 族与延迟任务链

> 前置: [[N-10-客户端管理]] (clientId 订阅) + [[NC-1-NamingService]] (客户端接收端) | 对照: UDP 遗留 vs gRPC 主线
> 🔴 A | 方案 A (全深度) | 闭环: q1(executor 族) q2(延迟任务链) q3(订阅服务) q4(模糊推送)

**读者处境**: 服务变更后服务端怎么把新实例列表推到订阅客户端? gRPC 与 UDP 双通道怎么共存? 推送失败怎么重试?

### 1. Executor 族 — PushExecutorDelegate 的双实现 + SPI

场景: 推送执行器怎么组织?
源码路径:
- **PushExecutor** (v2/executor/PushExecutor.java:30): 接口 — push
- **PushExecutorDelegate** (v2/executor/PushExecutorDelegate.java:36): **rpcPushExecuteService + udpPushExecuteService 双持有** (L38-40) + **push 时先查 SPI** (L66: SpiImplPushExecutorHolder) + **默认走 nacos 默认执行器** (L71)
- **PushExecutorRpcImpl** (executor/PushExecutorRpcImpl.java:37): gRPC 推送
- **PushExecutorUdpImpl** (executor/PushExecutorUdpImpl.java:37): UDP 推送
- **SpiPushExecutor** (executor/SpiPushExecutor.java:26) + **SpiImplPushExecutorHolder** (executor/SpiImplPushExecutorHolder.java:31): SPI 扩展面
关键设计 (q1): **"delegate 双执行器 + SPI 优先"** — 先查 SPI 自定义推送, 否则 rpc/udp 按客户端能力分派; 与 NC-5 组件注册/NC-11 处理器注册表同构。 [模式: 执行器注册表]

### 2. 延迟任务链 — PushDelayTask → PushExecuteTask

场景: 推送怎么批量/延迟执行?
源码路径:
- **PushDelayTaskExecuteEngine** (v2/task/PushDelayTaskExecuteEngine.java:37): 延迟引擎 (合并推送 — 短时间多次变更合并一次)
- **PushDelayTask** (v2/task/PushDelayTask.java:31): 延迟任务 (含推送数据)
- **PushExecuteTask** (v2/task/PushExecuteTask.java:42): extends AbstractExecuteTask — **run: 遍历 clientManager 客户端** (L57-60) + 执行推送
- **NamingPushCallback** (v2/task/NamingPushCallback.java:27): 推送回调 (失败重试)
- **PushConfig** (v2/PushConfig.java:28): 推送配置 (重试次数等)
关键设计 (q2): **"延迟合并 = 变更风暴削峰"** — 多次变更在延迟窗口内合并成一次推送 (PushDelayTask 携带最新数据); 执行时按 clientId 遍历。 [模式: 延迟合并推送]

### 3. 订阅服务 — NamingSubscriberService 三实现

场景: 订阅关系谁维护?
源码路径:
- **NamingSubscriberService** (push/NamingSubscriberService.java:29): 接口 — getSubscribers/订阅管理
- **NamingSubscriberServiceLocalImpl** (push/NamingSubscriberServiceLocalImpl.java:32): 本地实现
- **NamingSubscriberServiceAggregationImpl** (push/NamingSubscriberServiceAggregationImpl.java:50): 聚合实现 (集群订阅)
- **NamingSubscriberServiceV2Impl** (v2/NamingSubscriberServiceV2Impl.java:51): extends SmartSubscriber — **事件驱动** (服务变更事件 → 触发推送)
关键设计 (q3): **"订阅三实现 = 本地/聚合/事件"** — 本地管单节点, 聚合跨集群, V2 事件驱动推送触发。 [模式: 订阅分型]

### 4. 模糊推送 — FuzzyWatch 任务族

场景: 模糊订阅的变更怎么推?
源码路径:
- **NamingFuzzyWatchChangeNotifier** (push/NamingFuzzyWatchChangeNotifier.java:41) / **NamingFuzzyWatchSyncNotifier** (push/NamingFuzzyWatchSyncNotifier.java:51): 变更通知
- **FuzzyWatchChangeNotifyExecuteTask** (v2/task/FuzzyWatchChangeNotifyExecuteTask.java:33): 执行任务
- **FuzzyWatchSyncNotifyTask/Callback** (v2/task/): 同步通知与回调
- **FuzzyWatchPushDelayTaskEngine** (v2/task/FuzzyWatchPushDelayTaskEngine.java:36): 模糊推送延迟引擎
关键设计 (q4): **"模糊面 = 独立任务链"** — 模糊订阅有自己的 notifier/delay engine/回调族, 与精确订阅并行。 [模式: 模糊独立链]

### 5. 回调与钩子 — PushResultHook 族

场景: 推送结果怎么观测?
源码路径:
- **PushResultHook** (v2/hook/PushResultHook.java:24) + **PushResultHookHolder** (v2/hook/PushResultHookHolder.java:28) + **NacosMonitorPushResultHook** (v2/hook/NacosMonitorPushResultHook.java:28): 推送结果钩子 (监控)
- **PushResult** (v2/hook/PushResult.java:28): 结果载体
- **NoRequiredRetryException** (v2/NoRequiredRetryException.java:27): 无需重试异常 (客户端已断)
- **ClientInfo** (push/ClientInfo.java:29): 客户端信息 (协议判定)
关键设计 (q5): **"结果钩子 = 推送可观测"** — 监控钩子 (NacosMonitor) 记录推送结果; NoRequiredRetry 异常区分"无需重试"。 [模式: 结果钩子]

### 6. 测试与行为锚

场景: 推送的边界?
源码路径:
- 测试: PushExecutorTest / PushDelayTaskTest (naming test)
- 注释锚: "use nacos default push executor" (Delegate:71)
- 对照: UdpPushService (201 行) 1.x 遗留 vs v2/ 主线
关键设计 (q1): **"SPI 优先 + 默认兜底"** — 注释明言扩展面与默认面。 [模式: SPI 默认双面]
