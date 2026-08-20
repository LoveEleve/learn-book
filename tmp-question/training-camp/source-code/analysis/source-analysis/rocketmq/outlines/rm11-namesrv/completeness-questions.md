# RM-11 Namesrv 路由 — completeness-questions (全视角提问验证)

## 开发者视角

1. 路由信息存在哪? (RouteInfoManager 六表, 纯内存)
2. 注册和心跳是同一个请求吗? (不是 — REGISTER_BROKER=103 带配置; BROKER_HEARTBEAT=904 只更新时间戳)
3. 心跳超时多久? (默认 2min, broker 注册时可指定; scanNotActiveBroker 5s 扫一次)
4. 客户端查路由走哪个入口? (GET_ROUTEINFO_BY_TOPIC=105, 独立线程池)
5. broker 下线怎么删路由? (显式注销 / 通道事件 / 超时扫描 → 统一批量注销队列)
6. 主从在同一 brokerName 下怎么组织的? (brokerAddrTable: brokerName → brokerAddrs Map<brokerId, addr>)
7. 路由查询返回什么? (QueueData + BrokerData clone + filterServer + 静态 topic 映射)
8. 启动多久能服务? (needWaitForService 时 45s 就绪门禁)

## 架构师视角

9. 为什么路由不持久化? (无状态 → 多节点水平扩展, 重建成本低)
10. 多 namesrv 怎么保持一致? (不保证 — 客户端多地址容错, 对照 ZK/ETCD 共识)
11. 六表为什么要读写锁? (注册写多读更多; 读锁并发快照)
12. stateVersion 仲裁防什么? (僵尸 broker 复活顶掉新主 — Controller 时代脑裂防护)
13. 批注销队列的意义? (broker 批量下线风暴 → 单线程批量 drainTo 合并处理)
14. 查询为什么要 clone? (防外部引用篡改路由表)
15. 心跳为什么不带配置? (注册才全量; 心跳轻量 — 大数据包每 10s 打一次成本高)
16. DataVersion 的作用? (broker 侧轮询比对 → 只传输增量变化; 兼作心跳)
17. 对照 Kafka KRaft? (RocketMQ namesrv 无元数据持久化; Kafka 5.x KRaft 有元数据日志)

## 学生视角

18. namesrv 是干什么的? (路由注册中心 — 存 broker 地址和 topic 分布)
19. 什么是心跳? (broker 定期报活, 超时即被认为下线)
20. 客户端怎么拿路由? (启动时从 namesrv 拉全量, 之后定时刷新 + 异常重试)
