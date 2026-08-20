# RM-11 Namesrv 路由 — 无状态注册中心与路由发现

> 前置: [[RM-5-Broker]] (namesrv 周期注册 [10s,60s] 钳制) + [[RM-7-发送]] (tryToFindTopicPublishInfo) + [[RM-1-协议]] (REGISTER_BROKER=103) | 引出: [[RM-12-HA]] (Controller 内嵌/主从切换)
> 🟡 B | 6 KP | [模式: 注册中心 + 心跳保活 + 查询缓存]
> Pass 2 闭环: q1(六表结构) q2(注册) q3(注销) q4(路由查询) q5(心跳清理) q6(5.x 新面)

**读者处境**: 客户端怎么知道消息该发到哪台 broker? broker 挂了路由怎么失效? 这篇拆 namesrv: 纯内存注册中心 — 六表 + 注册/注销 + 心跳超时清理 + 路由查询。

### 1. 路由存储 — 六表 + 读写锁

场景: 路由信息存在哪? 什么结构?
源码路径:
- **RouteInfoManager** (1278 行, 唯一状态持有者): **六表** (L72-77): topicQueueTable (topic→brokerName→QueueData) / brokerAddrTable (brokerName→BrokerData) / clusterAddrTable (cluster→brokerName 集) / brokerLiveTable (addr→BrokerLiveInfo: 心跳+DataVersion+Channel) / filterServerTable (addr→FilterServer 列表, 遗留) / **topicQueueMappingInfoTable (5.x 静态 topic 多级映射)**
- **ReentrantReadWriteLock**: 读写分离 — 注册/注销写锁, 查询读锁; 初始容量 1024/128/32/256/256/1024 (L85-90); **锁使用不一致**: 读路径部分加锁 (queryBrokerTopicConfig 纯 CHM get 无锁 — 一致性靠原子性, 弱一致可接受)
- **BrokerData 结构**: clusterName + brokerName + brokerAddrs (Map<brokerId, addr>) — 主从同组; **BrokerAddrInfo (clusterName, addr) 为存活表主键** (L1125+)
- **无持久化**: 路由全内存; KVConfigManager 仅持久化配置 (kvConfigPath, 非路由)
关键设计 (q1): **路由 = 六表关联网络** (topic→broker→addr→存活); 读写锁保并发; **重启丢失靠 broker 重注册重建** (10s 初/10-60s 周期, RM-5)。[模式: 内存注册中心]

### 2. 注册 — 版本兼容 + 冲突仲裁

场景: broker 注册时 namesrv 做什么?
源码路径:
- **DefaultRequestProcessor.registerBroker** (L223-281): **crc32 body 校验** (L230-234) + **版本分支** (V3_0_11+ → RegisterBrokerBody 解压; 老版本仅 topic config) + zoneName 从 extFields
- **registerBroker 主逻辑** (RouteInfoManager:226-409): cluster 表补 brokerName → brokerAddrTable 建 BrokerData (registerFirst) → **同 IP:PORT 去重** (主从切换: 同地址只保留一条, L273) → **stateVersion 冲突仲裁** (旧记录 stateVersion > 新 → 拒绝注册, 防僵尸 broker 复活顶掉新主, L278-291) → topicQueueTable 更新 (registerFirst 或 DataVersion 变化才全量, L338-350) → **isPrimeSlave + enableActingMaster → 擦写权限** (L343-347) → brokerLiveTable 更新 (心跳超时=请求头 heartbeatTimeoutMillis, 默认 2min, L366-373) → filterServerTable → **最小 brokerId 变化通知** (notifyMinBrokerIdChanged, L398-401)
- **从库注册响应**: 返回 master 地址 (L386-396)
关键设计 (q2): **注册 = 幂等 upsert + 版本仲裁**; stateVersion 防脑裂 (Controller 时代); DataVersion 驱动 topic 表增量更新。**冲突静默面**: stateVersion 拒绝时返回空 result (非 null) → broker 收 SUCCESS 无感知 — 靠下轮注册自愈 (最终一致)。[模式: 注册 upsert]

### 3. 注销 — 异步批量 + 六表清理

场景: broker 下线路由怎么删?
源码路径:
- **三入口 → 统一通道**: 显式 UNREGISTER_BROKER=104 / 通道三事件 (BrokerHousekeepingService: close/exception/idle) / 心跳超时扫描 → 全部走 **submitUnRegisterBrokerRequest 批量队列**
- **onChannelDestroy 双形态** (L820-874): BrokerAddrInfo 直删 / Channel 反查 brokerLiveTable → setupUnRegisterRequest 反查 brokerName/brokerId (L876-899)
- **异步化 (5.x)**: onChannelDestroy → submitUnRegisterBrokerRequest → **BatchUnregistrationService** (82 行): LinkedBlockingQueue (**3000 容量**) + 单线程 take+drainTo 批量 → unRegisterBroker(Set) (L61-76) — **批量注销提速** (broker 批量下线风暴)
- **unRegisterBroker** (RouteInfoManager:571-650): brokerLiveTable/filterServerTable 删 → brokerAddrTable 删 addr (**空 → 删 brokerName**) → clusterAddrTable 删 name (**空 → 删 cluster**) → **cleanTopicByUnRegisterRequests**: 全量 topic 表清理 (removedBroker 删 QueueData, 空 topic 删; reducedBroker + acting master 无主 → 擦写权限, L652-685) → notifyMinBrokerIdChanged
- **setupUnRegisterRequest** (L876-899): 通道销毁时反查 brokerName/brokerId
关键设计 (q3): **注销收敛为统一批量通道**; 六表级联删除保一致; 通道三事件 (close/exception/idle) 都触发。[模式: 级联清理]

### 4. 路由查询 — 快照 + 就绪门禁 + 版本 JSON

场景: 客户端查询 topic 路由拿什么?
源码路径:
- **ClientRequestProcessor** (115 行, 独立线程池 clientRequestExecutor 8 线程/50000 队列 — 高频查询隔离, NamesrvController:211): **waitSecondsForService=45s 就绪门禁** (needWaitForService 默认 false — 仅在开启时生效; **门禁失效双路径**: 45s 时间条件自然短路 + 路由命中 disable 提前放行, 无永久拒绝路径, L65-80)
- **pickupTopicRouteData** (RouteInfoManager:700-801): 读锁 → QueueData 列表 + **BrokerData clone** (防外部篡改) + filterServer 映射 → **topicQueueMappingInfoTable 附加** (L749) → **5.x acting master 伪装**: supportActingMaster + 无主且 enableActingMaster → **最小 brokerId 从库地址伪装成 MASTER_ID (0) 返回** (L763-797)
- **版本 JSON**: V4_9_4+ → BrowserCompatible 标准编码; 老版本默认编码 (L90-97); TOPIC_NOT_EXIST 时返回错误码+FAQ 链接
- **ZoneRouteRPCHook** (96 行): ZONE_MODE+ZONE_NAME 请求 → **响应后置过滤**: 保留同 zone brokerData + 级联 queueData/filterServer 清理 (L62-95)
关键设计 (q4): **查询 = 只读快照 + 后置加工** (clone 防篡改/acting master 伪装/zone 过滤); 独立线程池防查询拖垮注册。[模式: 快照查询]

### 5. 心跳与清理 — 超时判定 + 双通道

场景: broker 失联怎么发现?
源码路径:
- **心跳**: BROKER_HEARTBEAT (**904**) → updateBrokerInfoUpdateTimestamp (DefaultRequestProcessor:384-395, 仅更新时间戳); **QUERY_DATA_VERSION (322) 也更新时间戳** (L355) — broker 的 DataVersion 轮询兼作心跳; **注册本身也 upsert 心跳时间戳** — 心跳三来源
- **超时判定**: scanNotActiveBroker (NamesrvController:117, **5s 周期** 初始 5ms): `lastUpdate + heartbeatTimeoutMillis < now` → closeChannel + onChannelDestroy (L803-818); **heartbeatTimeoutMillis 默认 2min** (DEFAULT_BROKER_CHANNEL_EXPIRED_TIME, L70), 由 broker 注册请求头指定; **余量**: broker 10-60s 心跳周期 vs 2min 超时 — 容 2-12 次心跳丢失不误踢; 扫描为无锁遍历 (ConcurrentHashMap 弱一致, 漏扫下轮补)
- **通道事件双通道**: BrokerHousekeepingService (三事件) + 定时扫描 — 双保险; 事件注销失败仅 log → **5s 扫描兜底 (终态不丢)**; **可见性窗口**: lastUpdateTimestamp 非 volatile 无锁更新 (L469-475) — 扫描可能短暂看旧值, 2min 超时 >> 窗口 → 误踢概率极低 ⚠
关键设计 (q5): **心跳 = 轻量时间戳更新** (不带 topic 配置, 注册才带); 超时 = 主动踢 + 被动事件双通道。[模式: 心跳保活]

### 6. 5.x 新面 — Controller 内嵌 + acting master + zone

- **Controller 内嵌**: enableControllerInNamesrv → NamesrvStartup 同进程起 ControllerManager (L182-219); CONTROLLER_REGISTER_BROKER=1003
- **acting master 全链四步**: 注册 isPrimeSlave 擦权 (L343-347) / 注销无主擦权 (L676-681) / 查询最小 brokerId 伪装 MASTER_ID (L787-789) / **minId 变化 invokeOneway 通知 (300ms, 在写锁临界区内发起 — 双配置开启时注册写路径可被拖慢** ⚠, L398-401,934)
- **写权限管理面**: WIPE_WRITE_PERM=205 / ADD_WRITE_PERM=206 (operateWritePermOfBroker 全表扫; **ADD 强制 READ|WRITE**, L532-555) — 与 acting master 共用 QueueData.perm; **WIPE/ADD 一次性语义**: DataVersion 变化时 QueueData 重建还原 (topicConfig.perm 为准), prime slave 擦权每注册重擦 (幂等不受影响) ⚠
- **zone 路由**: 请求级 zone 过滤 (master down 时保留该 broker 全部从库 — 可用性优先, RM-12 机房维度交叉)
- **批注销**: BatchUnregistrationService (3000 容量; 满 → 显式注销报错, 通道事件静默 → 5s 扫描兜底)
- **顺序消息 KV**: orderMessageEnable → NAMESPACE_ORDER_TOPIC_CONFIG 附加到路由响应 (ClientRequestProcessor:82-87)
- **deleteTopicWithBrokerRegistration** 默认 false: 注册时同步删 broker 侧消失的 topic (L320-336)

### 负面空间 — namesrv 刻意不做的事

- **不做路由持久化**: 全内存, 重启靠 broker 重注册重建 (namesrv 无状态是特性)
- **不做多节点共识**: 多 namesrv 各自独立, 一致性靠客户端多地址容错 (对照 ZooKeeper/ETCD)
- **不主动推送变更**: 客户端轮询 + 心跳变更感知 (RM-9 交叉)
- **不做 broker 健康探测**: 仅心跳超时被动判定 (无主动探活)
- **不存消息/进度/消费状态**: 纯路由; 消费进度在 broker (RM-8)
- **zone 过滤是响应后置**: 非路由存储层隔离
- **acting master 默认关闭**: 从库代主路由需显式开启

→ 引出: 主从怎么同步数据? 谁来做故障转移? → [[RM-12-HA]]
