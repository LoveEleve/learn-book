# 闭环笔记 q6: 5.x 新面 — Controller 内嵌 + acting master + zone

## 假设
5.x namesrv 从"纯路由"向"路由+管控"演进。

## 验证过程
- **Controller 内嵌**: NamesrvConfig.enableControllerInNamesrv=false 默认 (L79); NamesrvStartup: **createAndStartControllerManager** (L182-219) — namesrv 进程内起 ControllerManager (RM-12 交叉); CONTROLLER_REGISTER_BROKER=**1003** (RequestCode:257)
- **acting master 面** (5.x 从库代主路由): supportActingMaster=false 默认 (L67) / registerBroker 里 isPrimeSlave 擦写权限 (RouteInfoManager:343-347) / notifyMinBrokerIdChanged=false 默认 (L74) / 注销时无主擦写 (L676-681) / 查询伪装 MASTER_ID (L787-789) — **完整链**: 注册擦权 → 下线通知 → 查询伪装
- **deleteTopicWithBrokerRegistration=false 默认** (L92): 注册时同步删除 broker 侧消失的 topic (RouteInfoManager:320-336)
- **ZoneRouteRPCHook**: 96 行 zone 路由过滤 (q4 已详) — 机房维度隔离
- **BatchUnregistrationService**: 3000 容量批量注销 (q3 已详)
- **enableAllTopicList/enableTopicList** (L69-72): 全量 topic 列表开关 — 大数据量 namesrv 保护
- **configBlackList** (L98): 配置热更新黑名单 (UPDATE_NAMESRV_CONFIG=318 防护)

## 代码类型
Architecture (能力演进)

## 跨域关联
- RM-12 (HA): Controller 内嵌/acting master/主从切换 (强交叉)
- RM-5 (Broker): deleteTopicWithBrokerRegistration 协同
- RM-10 (事务): 从库代主 EscapeBridge 的路由依赖 (交叉)

## 结论
5.x namesrv = 3.x 路由核心 + Controller 内嵌 (HA 管控) + acting master 全链 (注册擦权/下线通知/查询伪装) + zone 过滤 + 批注销。
源码位置: NamesrvStartup.java:182-219; NamesrvConfig.java:67-98; RouteInfoManager.java:320-347,676-681,787-789
