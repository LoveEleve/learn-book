# 闭环笔记 q4: 路由查询 — 快照 + 就绪门禁 + 版本 JSON

## 假设
查询 = 读锁快照 + 防御性加工; 高频隔离。

## 验证过程
- **独立线程池**: GET_ROUTEINFO_BY_TOPIC=105 → ClientRequestProcessor (NamesrvController:211) 挂 clientRequestExecutor (**8 线程/50000 队列**); 其余请求 defaultExecutor (**16 线程/10000 队列**) — 查询风暴不拖垮注册 (L204-215)
- **就绪门禁** (ClientRequestProcessor:65-72): needWaitForService + **waitSecondsForService=45s** 内 → SYSTEM_ERROR "name server not ready" — 防启动风暴; **路由查询命中后 disable 门禁** (L78-80)
- **pickupTopicRouteData** (RouteInfoManager:700-801): 读锁 → QueueData 列表 → brokerName 集 → **BrokerData clone** (防外部篡改 L724) → filterServerTable 映射 (L728-735) → 静态映射附加 (L749)
- **acting master 伪装** (L751-797): supportActingMaster + 非系统 topic + 有 broker 无主 + enableActingMaster → **最小 brokerId 从库地址 remove 后以 MASTER_ID(0) put 回** (L787-789) — 客户端无感
- **zone 后置过滤** (ZoneRouteRPCHook:43-95): 仅 GET_ROUTEINFO_BY_TOPIC + SUCCESS + ZONE_MODE + ZONE_NAME → decode 响应 → 保留同 zone (或 master down 全留) → 级联清 queueData/filterServer
- **版本 JSON** (ClientRequestProcessor:90-97): V4_9_4+ / acceptStandardJsonOnly → BrowserCompatible + MapSortField; 否则默认 encode
- **orderMessageEnable**: NAMESPACE_ORDER_TOPIC_CONFIG KV 附加 (L82-87)
- **TOPIC_NOT_EXIST**: 错误码 + FAQ 链接 (L105-108)

## 代码类型
Implementation (快照 + 防御性加工)

## 跨域关联
- RM-7 (发送): tryToFindTopicPublishInfo → TopicPublishInfo 缓存 (RM-7 已讲)
- RM-9 (消费): broker 分配 queryAssignment 经 namesrv 路由 (交叉)
- RM-12 (HA): acting master 伪装的路由面 (交叉)

## 结论
查询 = 独立线程池 + 45s 门禁 + 快照 clone + 三后置加工 (acting master 伪装/zone 过滤/版本 JSON)。
源码位置: ClientRequestProcessor.java:65-108; RouteInfoManager.java:700-801; ZoneRouteRPCHook.java:43-95
