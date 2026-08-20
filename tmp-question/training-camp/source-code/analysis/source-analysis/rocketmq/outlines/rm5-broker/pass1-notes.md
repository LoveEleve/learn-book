# RM-5 Broker 启动 — Pass 1 探索笔记

> 域: RM-5 Broker 启动 (枢纽域) | 🔴 A 方案 | 2026-08-14
> 源码: BrokerController.java (2526) + BrokerStartup.java (316) | RocketMQ 5.3.1

## 调用图

```
启动面 (BrokerStartup):
main → createBrokerController (L245: buildBrokerController + initialize) → start (L55: controller.start)
buildBrokerController (L84): 参数/配置加载 (命令行 -c/-n + 环境) → new BrokerController

装配面 (BrokerController.initialize L824):
三阶段: initializeMetadata (L773: 7 个 configManager.load) → initializeMessageStore (L783: 
  DefaultMessageStore/RocksDBMessageStore 选择 + DLedger 角色变更注册 + BrokerStats + MessageStoreFactory 插件 + 
  5.x RocksDB CQ 双写 + TimerWheel) → recoverAndInitService (L849):
  ReplicasManager (5.x controller 模式 fenced) → messageStore.load → timerWheel.load → scheduleMessageService.load
  → 插件 load → initializeRemotingServer (双服务: remoting + fastRemoting) → initializeResources →
  registerProcessor (L1070, 48 处!) → initializeScheduledTasks (18 定时) → initialTransaction →
  initialAcl → initialRpcHooks → initialRequestPipeline → TLS FileWatchService (证书热加载)

定时任务 (18 个, initializeBrokerScheduledTasks + 其他):
brokerStats.record (每日) / configManager.persist×N (10s) / protectBroker / printWaterMark /
  dispatchBehindBytes (积压监控) / syncAll (主从) / printMasterAndSlaveDiff / updateNamesrvAddr /
  fetchNameServerAddr (5s?) / syncBrokerMemberGroup (5.x) / refreshMetadata (5.x) / topicQueueMappingClean

5.x 新面:
ReplicasManager (controller 模式自动故障转移, fenced 初始) / RocksDBMessageStore / rocksdbCQDoubleWrite /
  DLedgerRoleChangeHandler / TimerWheel (TimerMessageStore) / brokerAttachedPlugins (插件)
```

## 基本元素分解

1. **启动链**: main → createBrokerController → initialize → start
2. **initialize 三阶段**: 元数据加载 → 存储装配 → 服务恢复+注册
3. **48 处理器注册**: 命令面 → remoting processorTable
4. **18 定时任务**: 持久化/主从/namesrv/积压/保护
5. **5.x 新面**: ReplicasManager/RocksDB/TimerWheel/插件
6. **关闭面**: shutdown 有序 (定时器/线程池/存储)
7. **配置面**: BrokerConfig/MessageStoreConfig/NettyServerConfig 三配置

## 标记问题 (20 问)

1. createBrokerController 的配置加载链? (命令行/环境/文件)
2. initialize 三阶段的失败处理? (逐步短路)
3. 7 个 configManager 是什么? (topic/offset/group/filter 等)
4. Default vs RocksDB MessageStore 选择? (enableRocksDBStore)
5. DLedger 角色变更注册? (RM-12 交叉)
6. 双 remoting 服务? (remotingServer + fastRemotingServer 差异)
7. 48 处理器覆盖哪些命令面?
8. 18 定时任务清单?
9. syncAll 干什么? (主从全量同步)
10. protectBroker? (资源保护?)
11. 积压监控 dispatchBehindBytes?
12. ReplicasManager 的 fenced? (5.x controller)
13. TimerWheel 装配? (5.x Timer 消息)
14. 插件加载? (brokerAttachedPlugins)
15. TLS 证书热加载? (FileWatchService)
16. 事务初始化? (initialTransaction — RM-10 交叉)
17. ACL 初始化? (initialAcl — RM-13 交叉)
18. shutdown 顺序? (逆序)
19. 启动失败处理? (exit)
20. BrokerIdentity? (5.x 身份)

## 时空溯源 (代码内痕迹)

- 3.x: BrokerController 骨架定型 (initialize 三阶段 + registerProcessor + 定时任务)
- 4.x: DLedger 集成 (DLedgerRoleChangeHandler) / 事务消息 (initialTransaction) / ACL (initialAcl)
- 5.0: ReplicasManager (controller 模式) / RocksDBMessageStore / 双 remoting (fast) / BrokerIdentity / 插件体系
- 5.x: TimerWheel (Timer 消息) / syncBrokerMemberGroup / refreshMetadata / topicQueueMappingClean

## 大域拆分判断

BrokerController 2526 行 + Startup 316 — 枢纽装配域; 🔴 A 单篇 (6 闭环)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "BrokerController 装配/模块注册/定时任务" | initialize 三阶段 + 48 处理器 + 18 定时 | **接受** ✅ |
| 数字: registerProcessor | 48 处 (grep -c) | **补充** ✅ |
| 数字: 定时任务 | 18 个调用点 (python 提取去重) | **补充** ✅ |
| 数字: configManager | 7 个 (initializeMetadata) | **补充** ✅ |
