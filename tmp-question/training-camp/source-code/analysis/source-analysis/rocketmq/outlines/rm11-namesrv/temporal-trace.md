# RM-11 Namesrv 路由 — 时空溯源 (代码内痕迹)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.x (2015-) | 四表骨架 (topicQueueTable/brokerAddrTable/clusterAddrTable/brokerLiveTable) + REGISTER_BROKER 注册 + 心跳 2min 默认超时 + scanNotActiveBroker 5s 扫描; 版本兼容分支注释实证 (DefaultRequestProcessor:240 "V3_0_11+") |
| 4.x | KVConfigManager (顺序消息 topic 配置, NAMESPACE_ORDER_TOPIC_CONFIG) + filterServerTable (FilterServer 时代遗留) + GET_BROKER_MEMBER_GROUP=901 + V4_9_4 标准 JSON 编码 (ClientRequestProcessor:91) |
| 5.0 | topicQueueMappingInfoTable (静态 topic 多级映射) + Controller 内嵌 (enableControllerInNamesrv + CONTROLLER_REGISTER_BROKER=1003) + enableAllTopicList |
| 5.x | acting master 全链 (supportActingMaster/isPrimeSlave 擦权/notifyMinBrokerIdChanged/查询伪装) + ZoneRouteRPCHook (zone 过滤) + BatchUnregistrationService (3000 批量注销) + BROKER_HEARTBEAT=904 + deleteTopicWithBrokerRegistration + configBlackList |

## 痕迹证据

- DefaultRequestProcessor.java:239-247: 版本分支 (V3_0_11 注释锚)
- ClientRequestProcessor.java:91: V4_9_4 JSON 版本锚
- RequestCode.java:99-236,257: 请求码年代梯度 (100/103/104/105 老 → 205/206/216/224 中 → 318/319/322 新 → 901/904 5.x → 1003 Controller)
- RouteInfoManager.java:70: DEFAULT_BROKER_CHANNEL_EXPIRED_TIME=2min (3.x 常量)
- RouteInfoManager.java:77: topicQueueMappingInfoTable (5.0 静态 topic)
- NamesrvConfig.java:67-98: acting master/zone/批注销配置 (5.x 引入)
- ZoneRouteRPCHook.java: 96 行独立文件 (5.x 新类)

## 推断标注

- "3.x 四表" — 公知版本线 + 常量风格推断 (标注); 高置信 (3.x 文档同构)
- "4.x KV/FilterServer/901" — 特性年代推断 (标注); FilterServer 面已在 RM-6 证废弃
- "5.0/5.x 面" — 配置项 + 类文件新增推断 (标注); 未做 git 考古
