# Pass 1 探索笔记: RD-1 主类+连接管理 (Redisson/Config/ServiceManager/ConnectionManager)

> 方案 A (🔴) | 源码: `/data/workspace/source-code/code/spring/redisson` (4.6.2-SNAPSHOT)
> 大域: 预期 ≥6 闭环 → 拆 3 篇 (01-create / 02-connection / 03-pubsub-dns)
> 时空溯源适配: git 仅 1 commit 浅克隆无 tag → 改用 CHANGELOG 版本史作时空基线

## Pass 0 上下文吸收

- README: "Redisson: Valkey & Redis Java Client" — 4.x 双平台支持; 8 种部署模式声称 (含 Proxy/Multi-Cluster/Multi-Sentinel), 但 config/ 12 类仅 5 种服务器模式 + ConnectionManager.create 5 分支 — **README 超前于代码** (记录, 不追)
- CHANGELOG 时空线索: 4.x 引入 DelayStrategy (EqualJitterDelay 默认)/dnsMonitoringTimes/checkMasterLinkStatus (Cluster)/fallbackLoadingToMaster; 4.6.1 修 AsyncSemaphore.tryRun 竞态 (连接池 permit 泄漏)
- 测试地图: connection/ 3 测试 (ConnectionsHolderTest/MasterSlaveConnectionManagerTest/ReplicatedConnectionManagerTest) + 顶层 RedisClientTest + config/ 测试
- git log 仅 1 commit → 时空溯源不可行, 以 CHANGELOG 替代

## 继承树/调用图

```
Redisson.create(config)                          Redisson.java:119
  └─ new Redisson(config)                        Redisson.java:66-86
       ├─ Config configCopy = new Config(config)  (默认 Kryo5Codec L165)
       ├─ ConnectionManager.create(configCopy)   ConnectionManager.java:89-111
       │    ├─ SingleServerConfig     → SingleConnectionManager
       │    ├─ MasterSlaveServersConfig→ MasterSlaveConnectionManager
       │    ├─ SentinelServersConfig  → SentinelConnectionManager (extends MasterSlave)
       │    ├─ ClusterServersConfig   → ClusterConnectionManager  (extends MasterSlave)
       │    └─ ReplicatedServersConfig→ ReplicatedConnectionManager(extends MasterSlave)
       │    └─ !lazyInitialization → cm.connect()
       ├─ connectionManager.createCommandExecutor(...)   → CommandAsyncExecutor
       ├─ evictionScheduler = new EvictionScheduler(commandExecutor)
       ├─ writeBehindService = new WriteBehindService(commandExecutor)
       └─ serviceManager.register(new LockRenewalScheduler(commandExecutor))   ServiceManager:766

ConnectionManager (接口 89行)                      ConnectionManager.java
  ├─ MasterSlaveConnectionManager (~780行)
  │    ├─ fields: dnsMonitor/subscribeService/serviceManager/lazyConnectLatch(AtomicReference<CF>)/rrCounter
  │    ├─ lazyConnect() L190 / connect() L229 / doConnect() L286 / detectCluster() L264
  │    ├─ startDNSMonitoring() L333 / changeMaster() L649 / shutdown() L727
  │    ├─ create() L346 (构建 MasterSlaveEntry)
  │    └─ subclasses: Cluster/Sentinel/Replicated 各自 doConnect (槽位/哨兵/主从发现)
  ├─ SingleConnectionManager → SingleEntry
  ├─ ServiceManager (804行) — 中央服务: HashedWheelTimer/IdleConnectionWatcher/
  │    ConnectionEventsHub/ElementsSubscribeService/QueueTransferService/NatMapper/
  │    register() 服务注册表 (L766)
  ├─ MasterSlaveEntry (842行) — 主从拓扑容器: master+slaves ClientConnectionsEntry
  ├─ ClientConnectionsEntry (274行) — 单节点连接池 (ConnectionsHolder 包装)
  ├─ ConnectionsHolder — 按角色连接持有 (AsyncSemaphore permit 控制池容量)
  ├─ DNSMonitor (285行) — 域名监控 → changeMaster (L152)
  └─ IdleConnectionWatcher — 空闲连接回收

数据流: RedissonClient API → CommandAsyncExecutor → RedisExecutor → ConnectionManager.getEntry
        → MasterSlaveEntry 按 readMode 选 master/slave → ClientConnectionsEntry 借连接 → RESP
```

## 基本元素分解

1. **Redisson 门面** — 800+ API 方法分派到各结构实现; 持有 connectionManager/commandExecutor/evictionScheduler/writeBehindService/config — Redisson.java:65-86
2. **Config 配置模型** — 空构造 + copy 构造; 5 模式配置类 (Single/MasterSlave/Sentinel/Cluster/Replicated); 全局默认 (watchdog 30s/batch 100/retry 4/jitter 1-2s/threads 16/nettyThreads 32/timeout 3000/lazy=false) — Config.java:63-153, BaseConfig.java:58-67
3. **ConnectionManager.create 工厂** — 5 分支 if-else → 5 实现; null → IllegalArgumentException — ConnectionManager.java:89-111
4. **ServiceManager 中央服务** — EventLoopGroup/HashedWheelTimer/IdleConnectionWatcher/ConnectionEventsHub/ElementsSubscribeService/QueueTransferService/NatMapper/responses 表 + register() — ServiceManager.java:122-156,766
5. **MasterSlaveConnectionManager 拓扑管理** — lazyConnect (CAS 单飞)/connect/doConnect (解析地址+建 Entry)/DNSMonitor/changeMaster/shutdown 三态 (quietPeriod) — L190-331,649,727
6. **MasterSlaveEntry 节点容器** — 主从 Entry 集合; changeMaster 切换; 读写路由 — 842 行
7. **ClientConnectionsEntry + ConnectionsHolder** — 单节点池: 连接创建/借还/健康检查; AsyncSemaphore permit (池容量) — 274 行
8. **DNSMonitor** — 周期 DNS 解析 (dnsMonitoringInterval) → 地址变化 → changeMaster/changeSlave — 285 行
9. **PublishSubscribeService/LockPubSub** — 订阅通道管理 (RLock 释放通知载体) — pubsub/ 6 文件
10. **IdleConnectionWatcher** — 空闲连接扫描回收 (Entry: min/max 配置)

## 标记问题 (8 个)

1. **Q1 初始化链顺序**: Redisson 构造器为何在 ConnectionManager 之后才创建 EvictionScheduler/WriteBehindService/LockRenewalScheduler (而非 ServiceManager 内)? register() 注册表的机制是什么? — Redisson.java:66-86 + ServiceManager:766
2. **Q2 lazyConnect 防重入**: AtomicReference<CompletableFuture<Void>> lazyConnectLatch 如何保证多线程只触发一次连接 + 失败重试? 死锁防护原理? — MasterSlaveConnectionManager:190-227 + MasterSlaveConnectionManagerTest:179 (testLazyConnectReentryFromConnectingThreadDoesNotDeadlock)
3. **Q3 eager vs lazy 汇聚**: connect() 与 lazyConnect() 两条路径如何汇聚到 doConnect? lazyInitialization 默认 false 的语义? — L229-262/286 + Config:111
4. **Q4 DNS 故障切换协议**: DNSMonitor 周期? 什么触发 changeMaster? 新地址验证 (ping) 避免脑裂? — DNSMonitor:285 + MasterSlaveConnectionManager:649
5. **Q5 读写分离路由**: readMode (SLAVE/MASTER/MASTERSLAVE) 在哪决策? rrCounter 轮询? executeAllAsync 全 slave? — MasterSlaveEntry:842 + rrCounter L74
6. **Q6 ServiceManager 生命周期**: 各组件何时创建/关闭? shutdown 顺序? responses 表/adders 表的角色? — ServiceManager:804
7. **Q7 Config→Manager 映射**: 5 模式配置如何被 create 识别 (ConfigSupport.getConfig)? 未定义地址的报错路径? — ConnectionManager:89-111 + ConfigSupport
8. **Q8 AsyncSemaphore permit 机制**: ConnectionsHolder 池容量控制 (initConnections 精确释放 — 4.6.1 竞态修复) — ConnectionsHolderTest:56-100

## 已读测试 (2 个)

- ConnectionsHolderTest: testFailedInitConnectionReleasesPermitExactlyOnce (池容量 permit 精确性), testSuccessfulInitReleasesEachPermitExactlyOnce — harness 素材: AsyncSemaphore 计数验证
- MasterSlaveConnectionManagerTest: testLazyConnectRetriesAfterFailedInitialization, testLazyConnectReentryFromConnectingThreadDoesNotDeadlock, testDoConnectBoundsWaitWhenMinimumIdleSizeIsZero — harness 素材: lazyConnect 单飞语义

## 完成检查

- [x] 继承树/调用图已画出
- [x] 基本元素分解 10 项, 全部有源码位置
- [x] 8 个标记问题, 每个有源码位置
- [x] 已读 2 个测试文件 (ConnectionsHolderTest/MasterSlaveConnectionManagerTest)
- [x] 时空溯源适配记录 (CHANGELOG 替代 git tag)
