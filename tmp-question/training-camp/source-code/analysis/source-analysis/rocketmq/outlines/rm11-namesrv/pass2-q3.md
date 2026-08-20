# 闭环笔记 q3: 注销 — 异步批量 + 六表清理

## 假设
注销 = 异步批量处理 + 六表级联清理。

## 验证过程
- **三入口 → 统一通道**:
  1. 显式 UNREGISTER_BROKER=104 → DefaultRequestProcessor.unregisterBroker → **submitUnRegisterBrokerRequest** (队列, L368-382)
  2. 通道销毁: BrokerHousekeepingService.onChannelClose/Exception/Idle → onChannelDestroy (L36-48)
  3. 心跳超时: scanNotActiveBroker → closeChannel + onChannelDestroy (RouteInfoManager:803-818)
- **onChannelDestroy 双形态** (L820-874): BrokerAddrInfo 直删 / Channel 反查 brokerLiveTable → setupUnRegisterRequest 反查 brokerName/brokerId (L876-899)
- **BatchUnregistrationService** (82 行): LinkedBlockingQueue (**unRegisterBrokerQueueCapacity=3000**) + 单线程 take + **drainTo 批量** + HashSet 去重 → unRegisterBroker(Set) (L61-76); 队列满 → submit 失败 → 显式注销返回 SYSTEM_ERROR (DefaultRequestProcessor:373-378)
- **unRegisterBroker 六表级联** (L571-650):
  1. brokerLiveTable.remove (L585)
  2. filterServerTable.remove (L591)
  3. brokerAddrTable: 删 addr → **空则删 brokerName** (L601-612)
  4. clusterAddrTable: 删 name → **空则删 cluster** (L619-633)
  5. **cleanTopicByUnRegisterRequests** (L652-685): 全量扫 topicQueueTable — removedBroker 删 QueueData (空 topic 整删) / reducedBroker + acting master 无主 → **擦写权限** (isNoMasterExists L687-698)
  6. notifyMinBrokerIdChanged (L642-644)

## 代码类型
Implementation (异步批量 + 级联清理)

## 跨域关联
- RM-5 (Broker): unregisterBrokerAll 优雅关闭
- RM-12 (HA): 注销触发的 min brokerId 变化 → acting master 通知链 (交叉)
- RM-1 (协议): 104 请求码

## 结论
注销 = 三入口统一进批量队列 (单线程 drainTo 合并) → 六表级联清理 (删 addr → 空删 name → 空删 cluster → topic 表全量扫) → 写权限擦除链。
源码位置: BatchUnregistrationService.java:61-76; RouteInfoManager.java:571-698; BrokerHousekeepingService.java:36-48
