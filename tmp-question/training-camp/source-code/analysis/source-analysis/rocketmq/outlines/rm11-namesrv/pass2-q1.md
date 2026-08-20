# 闭环笔记 q1: 路由存储 — 六表 + 读写锁

## 假设
路由信息 = 多表关联的纯内存结构, 读写锁保护。

## 验证过程
- **六表** (RouteInfoManager:72-77):
  1. topicQueueTable: topic → Map<brokerName, QueueData> (初始容量 1024)
  2. brokerAddrTable: brokerName → BrokerData (clusterName + brokerAddrs Map<brokerId, addr>) (128)
  3. clusterAddrTable: clusterName → Set<brokerName> (32)
  4. brokerLiveTable: BrokerAddrInfo(clusterName, addr) → BrokerLiveInfo (lastUpdate + heartbeatTimeoutMillis + DataVersion + Channel + haServerAddr) (256)
  5. filterServerTable: BrokerAddrInfo → List<String> FilterServer (遗留, RM-6 废弃面) (256)
  6. topicQueueMappingInfoTable: topic → Map<brokerName, TopicQueueMappingInfo> (5.x 静态 topic 多级映射) (1024)
- **ReentrantReadWriteLock** (L71): 注册/注销/删 topic 写锁; 查询/列表读锁 — 读写分离
- **BrokerAddrInfo** (L1125+): (clusterName, brokerAddr) 二元组 — brokerLiveTable 主键; equals/hashCode 实现
- **BrokerLiveInfo** (L1187+): lastUpdateTimestamp/heartbeatTimeoutMillis/DataVersion/channel/haServerAddr — 心跳状态载体
- **无持久化**: 仅 KVConfigManager 持久化配置 (kvConfigPath, 非路由); 路由重启丢失 → broker 10s 初/10-60s 周期重注册重建 (RM-5 交叉)

## 代码类型
Data Structure (多表关联 + RWLock)

## 跨域关联
- RM-5 (Broker): 注册/心跳发起方
- RM-7 (发送): tryToFindTopicPublishInfo 消费端
- RM-3 (存储): QueueData 读写队列数来自 TopicConfig

## 结论
路由 = 六表关联网络 (topic→brokerName→addr→存活), RWLock 读写分离, 纯内存无持久化 — "无状态"是 namesrv 水平扩展前提。
源码位置: RouteInfoManager.java:71-77,85-90,1125-1195
