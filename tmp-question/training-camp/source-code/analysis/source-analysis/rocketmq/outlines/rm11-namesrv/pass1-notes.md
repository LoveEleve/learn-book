# RM-11 Namesrv 路由 — Pass 1 探索笔记

> 域: RM-11 Namesrv 路由 | 🟡 B 方案 | 2026-08-14
> 源码: RouteInfoManager (1278) + DefaultRequestProcessor (684) + NamesrvController (285) + NamesrvStartup (243) + KVConfigManager (194) + ClientRequestProcessor (115) + ZoneRouteRPCHook (96) + ClusterTestRequestProcessor (86) + BatchUnregistrationService (82) + BrokerHousekeepingService (54) + KVConfigSerializeWrapper (32) = 11 文件/3149 行 (模块行数穷举实证) | RocketMQ 5.3.1

## 调用图

```
注册面 (broker → namesrv):
BrokerController.registerBrokerAll (RM-5, 10s 初/[10s,60s]) → BrokerOuterAPI.registerBroker
  → REGISTER_BROKER=103 → DefaultRequestProcessor.registerBroker (crc32+版本分支+zoneName)
    → RouteInfoManager.registerBroker: 六表 upsert + stateVersion 仲裁 + prime slave 擦写 + 心跳表更新
心跳面: BROKER_HEARTBEAT=904 → updateBrokerInfoUpdateTimestamp (轻量)
        QUERY_DATA_VERSION=322 → isBrokerTopicConfigChanged + 更新心跳时间戳 (兼心跳)
下线面: 显式 UNREGISTER_BROKER=104 / BrokerHousekeepingService 三事件 / scanNotActiveBroker 5s 超时
  → onChannelDestroy → submitUnRegisterBrokerRequest → BatchUnregistrationService (3000 队列)
  → unRegisterBroker(Set) 六表级联清理 + 擦写权限

查询面 (客户端 → namesrv):
GET_ROUTEINFO_BY_TOPIC=105 (独立线程池 8/50000) → ClientRequestProcessor
  → 45s 就绪门禁 → pickupTopicRouteData (快照+clone+filterServer+静态映射+acting master 伪装)
  → ZoneRouteRPCHook 后置 zone 过滤 → V4_9_4 标准 JSON
```

## 基本元素分解

1. **六表存储**: topicQueue/brokerAddr/clusterAddr/brokerLive/filterServer/topicQueueMappingInfo
2. **注册**: upsert + crc32 + 版本兼容 + stateVersion 仲裁 + prime slave
3. **注销**: 批量异步 + 六表级联 + 擦写权限
4. **查询**: 快照 clone + 就绪门禁 + 版本 JSON + zone 过滤
5. **心跳清理**: 904 轻量 + 322 兼心跳 + 5s 扫描 + 双通道
6. **5.x 新面**: Controller 内嵌 / acting master / zone / 批注销

## 标记问题 (20 问)

1. 六表各自的 key 结构? (topic/brokerName/cluster/addr...)
2. 读写锁粒度? (全表 RWLock)
3. 注册时 crc32 校验? (bodyCrc32)
4. 老版本 broker 怎么兼容? (V3_0_11 分支)
5. stateVersion 冲突怎么处理? (拒绝注册)
6. 主从同 brokerName 怎么存? (brokerAddrs Map<id, addr>)
7. 主从切换地址去重? (同 IP:PORT 只留一条)
8. prime slave 擦写权限? (acting master 面)
9. 注销为什么异步? (BatchUnregistrationService)
10. 通道销毁怎么反查 broker? (setupUnRegisterRequest)
11. 注销时 topic 表怎么清理? (cleanTopicByUnRegisterRequests)
12. 查询为什么 clone? (防篡改)
13. 45s 门禁干什么? (启动风暴防护)
14. V4_9_4 JSON 差异? (BrowserCompatible)
15. acting master 伪装逻辑? (最小 brokerId → MASTER_ID)
16. zone 过滤在哪层? (RPCHook 后置)
17. 心跳超时默认值? (2min + 5s 扫描)
18. 谁更新心跳时间戳? (904/322/注册)
19. Controller 内嵌怎么启动? (enableControllerInNamesrv)
20. 路由不持久化怎么恢复? (broker 重注册)

## 时空溯源 (代码内痕迹)

- 3.x: 四表骨架 (topicQueue/brokerAddr/clusterAddr/brokerLive) + REGISTER_BROKER + 2min 超时 + 5s 扫描
- 4.x: KVConfigManager (顺序消息配置) + filterServerTable + GET_BROKER_MEMBER_GROUP=901
- 5.0: topicQueueMappingInfoTable (静态 topic) + Controller 内嵌 + CONTROLLER_REGISTER_BROKER=1003 + enableAllTopicList
- 5.x: acting master 面 (supportActingMaster/isPrimeSlave/notifyMinBrokerIdChanged) + ZoneRouteRPCHook + BatchUnregistrationService + BROKER_HEARTBEAT=904 + deleteTopicWithBrokerRegistration

## 大域拆分判断

RM-11 = namesrv 全模块 11 文件/3149 行 (最小模块); 单篇 🟡 B, 6 闭环; 路由协议数据结构 (TopicRouteData/QueueData/BrokerData) 在 common 模块 (交叉引用)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "RouteInfoManager/BrokerData/QueueData/TopicRouteData 三表" | 实际**六表** (含 filterServerTable + topicQueueMappingInfoTable) | **修正: 三表 → 六表** ⚠ |
| "心跳超时清理" | scanNotActiveBroker 5s + 2min 默认 + 双通道 | **接受** ✅ |
| "5.x 弱化 (client 可经 proxy; namesrv→controller)" | proxy 面 RM-13 交叉; enableControllerInNamesrv 内嵌实证 | **接受** ✅ |
| "namesrv 无状态, 多节点" | 全内存 + 无复制; KV 仅配置持久化 | **接受** ✅ |
| 数字: 请求码 | REGISTER_BROKER=103 / UNREGISTER=104 / GET_ROUTE=105 / WIPE=205 / GET_TOPICS=224 / UPDATE_NS=318 / GET_NS=319 / QUERY_DV=322 / GET_MEMBER=901 / HEARTBEAT=904 / CONTROLLER=1003 | **补充** ✅ |
| 数字: 线程池 | default 16/10000 + client 8/50000 + 批注销 3000 | **补充** ✅ |
| 数字: 时间 | scanNotActiveBrokerInterval=5s / 默认超时 2min / waitSecondsForService=45s | **补充** ✅ |
