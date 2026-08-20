# RM-5 Broker 启动 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | BrokerController 骨架定型: initialize 三阶段 + registerProcessor + initializeBrokerScheduledTasks (统计/持久化/主从) — 至今未变 |
| 4.x | **DLedger 集成** (DLedgerRoleChangeHandler) / 事务消息 (initialTransaction) / ACL (initialAcl) / fastRemotingServer (发送专用端口) |
| **5.0** | **ReplicasManager** (controller 模式自动故障转移, fenced 初始) / **RocksDBMessageStore** (+CQ 双写) / **存储插件** (MessageStoreFactory) / **BrokerIdentity** / 附件插件 / slave-act-master 隔离 |
| 5.x | **TimerWheel** (TimerMessageStore) / syncBrokerMemberGroup / refreshMetadata / topicQueueMappingClean / REPLAY 消息面 |

## 痕迹证据

- BrokerController.java:773-781: 7 个 configManager.load (initializeMetadata)
- BrokerController.java:797-804: DLedgerRoleChangeHandler (5.x 注释风格)
- BrokerController.java:478-486: fastRemotingServer listenPort-2
- BrokerController.java:855-858: ReplicasManager fenced=true
- BrokerController.java:608-770: 定时任务 (统计每日/持久化/积压/主从)
- BrokerController.java:1724-1746: namesrv 周期注册 (10-60s 钳制)
- BrokerController.java:180-230: BrokerIdentity (5.x)

## 推断标注

- "3.x 骨架定型" — RocketMQ 公知版本线 (标注)
- "4.x DLedger/事务/ACL/fast" — 特性年代推断 (标注)
- "5.0 ReplicasManager/RocksDB/插件" — 与 controller 模块同代推断 (标注)
- "5.x TimerWheel/REPLY" — 5.1+ 特性推断 (标注)
- 未做 git 考古, 版本线为代码结构推断, 已逐条标注
