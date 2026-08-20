# 闭环笔记 q2: 三阶段初始化 — Metadata/MessageStore/Recover

## 假设
元数据 7 管理器加载; 存储双实现选择; 恢复+服务注册。

## 验证过程
- **initializeMetadata** (L773-781): **7 个 configManager.load** — topicConfigManager / topicQueueMappingManager / consumerOffsetManager / subscriptionGroupManager / consumerFilterManager / consumerOrderInfoManager (+ topicConfig 重试?) — 内存态恢复
- **initializeMessageStore** (L783-822):
  - **双实现**: enableRocksDBStore → RocksDBMessageStore / 默认 DefaultMessageStore (5.x rocksdbCQDoubleWrite 双写面)
  - **DLedger 集成** (L797-804): DLedgerRoleChangeHandler 注册 (RM-12)
  - BrokerStats + **MessageStoreFactory.build 插件** (MessageStorePluginContext — 存储插件链)
  - **分发前置**: CommitLogDispatcherCalcBitMap (过滤位图分发, RM-6 交叉)
  - **TimerWheel** (L813-819): TimerCheckpoint/TimerMessageStore 装配 (5.x Timer 消息)
- **recoverAndInitService** (L849-900):
  - **ReplicasManager** (5.x controller 模式, fenced=true 初始, L855-858)
  - 恢复序: messageStore.load → timerWheel.load → **scheduleMessageService.load (RM-4)** → 插件 load
  - 服务注册: initializeRemotingServer → initializeResources → registerProcessor → initializeScheduledTasks → initialTransaction (RM-10) → initialAcl (RM-13) → initialRpcHooks → initialRequestPipeline → TLS FileWatchService

## 代码类型
Implementation (装配)

## 跨域关联
- RM-3/4/6/10/12/13: 全消费面

## 结论
三阶段 = 元数据恢复 → 存储装配 (双实现+DLedger+插件+TimerWheel) → 服务注册链 (处理器/定时/事务/ACL/TLS); 恢复序严格 (store→schedule→插件)。
源码位置: BrokerController.java:773-900
