# RD-1 篇1 — create: 一次 new Redisson() 背后发生了什么

> 前置: [[rd0-intro]] (全景) + [[r20-server]] (服务端骨架对照) | 复用: [[s75-boot-redis]] (承接 "连接深入在阶段3") | 对照: [[r21-db]] (服务端键空间 vs 客户端门面) | 引出: [[RD-1-篇2]] (连接池) + [[rd4-command]] (命令执行器) + [[rd2-rlock]] (续期器注册)
> 🔴 A | 3 KP | [模式: 五步装配 + 工厂分支 + 服务注册表]
> Pass 2 闭环: q1(初始化链) q7(模式工厂) q6(ServiceManager)

**读者处境**: 你在 Spring Boot 里配好 redisson-spring-boot-starter, 启动日志里一坨"Redisson 4.6.x server-times = ..." — 这 200ms 里发生了什么?为什么换一种 Redis 拓扑 (单机→哨兵) 只改一段配置、代码零改动?RedissonClient 是谁?ServiceManager 又是什么?这篇拆 new Redisson() 的五步装配: 配置复制、模式工厂、命令执行器、附属服务、续期器注册 — 以及藏在此刻的"服务注册表"设计。

### 概念依赖链
q1(初始化链) ← q7(模式工厂) ← q6(ServiceManager) — 先讲"谁创建谁"的装配序, 再挖模式如何选, 最后看中央服务容器。

### 核心悬念
"一次 new Redisson() 到底 new 了几个对象、按什么顺序、为什么换拓扑不用改代码？" — 读者读完能画出客户端启动的对象图。

### 叙事顺序
1. 问题引入: 一行 create() 到门面可用, 中间发生了什么
2. 五步装配 (q1) — 依赖序协议, 附属服务为何不在 ServiceManager
3. 模式工厂 (q7) — ConfigSupport 优先级 + 5 分支, 子类只变 doConnect
4. ServiceManager (q6) — 中央容器五件套 + register 注册表
5. 收束: 开篇"server-times"日志来源 (Version.logVersion) + 拓扑零改动之谜解锁

### 1. 五步装配 — 构造器就是初始化协议

场景: 一行 new Redisson(config) 到门面可用的完整链路?
源码路径:
- `Redisson.java:66-86` (构造器): 五步固定序 —
  1. `Config configCopy = new Config(config)` — **防御性复制** (copy ctor; 默认 Kryo5Codec 在此兜底, Config.java:165)
  2. `ConnectionManager.create(configCopy)` — 模式工厂 (§2)
  3. `connectionManager.createCommandExecutor(...)` — CommandAsyncExecutor (RD-4)
  4. `new EvictionScheduler(commandExecutor)` + `new WriteBehindService(commandExecutor)` — 附属服务 (RD-5 消费)
  5. `serviceManager.register(new LockRenewalScheduler(commandExecutor))` — RD-2 续期器注册 (§3)
- 为什么是五步可复现顺序: 附属服务 (Eviction/WriteBehind/LockRenewal) 全都依赖第二步产出的 commandExecutor — 所以不能在 ServiceManager 内部自建, 只能等命令执行器就绪后由构造器创建
- 门面分派: `Redisson` 实现 RedissonClient 接口 — getMap/getLock/getBucket 等 800+ 方法入口
关键设计 (q1): 初始化不是"new 一堆对象" — 是**依赖序协议**: 连接层 → 命令层 → 消费层, 每层产物喂下层。[模式: 五步装配]
数据流: create(config) → copy → Manager → Executor → 附属 → register。

### 2. 模式工厂 — 五种拓扑一个入口

场景: 单机/主从/哨兵/集群/复制, 一行配置怎么切?
源码路径:
- `ConnectionManager.create` (ConnectionManager.java:89-111): `ConfigSupport.getConfig(configCopy)` 拿到具体模式配置 → **5 分支 if-else** (MasterSlave/Single/Sentinel/Cluster/Replicated) → 5 个 Manager 实现; 全 null → `IllegalArgumentException("server(s) address(es) not defined!")` (L104-106)
- `ConfigSupport.getConfig` (ConfigSupport.java:844-859): 模式识别有**固定优先级** MasterSlave→Single→Sentinel→Cluster→Replicated, 命中一个先 `validate()` 再返回
- `!lazyInitialization → cm.connect()` (ConnectionManager.java:107-108): 默认 lazy=false → 构造期立即连接 (篇3 详述懒路径)
- 三种子类边界: Sentinel/Cluster/Replicated 都 extends MasterSlaveConnectionManager — **只覆写 doConnect** (哨兵发现/槽位映射/主从同步), 连接池全部复用 (RD-1 篇2)
关键设计 (q7): 工厂分支把"拓扑差异"收敛到**连接发现阶段** (doConnect), 之上所有逻辑 (池/路由/命令) 拓扑无关。[模式: 分支工厂 + 子类只变发现]
数据流: useMasterSlaveServers() → config 持有模式对象 → getConfig 识别 → 分支建 Manager。

### 3. ServiceManager — 你离不开的服务注册表

场景: EventLoopGroup 从哪来?定时任务谁调度?NatMapper 干吗的?
源码路径:
- `ServiceManager` 全字段 (ServiceManager.java:122-156): ConnectionEventsHub (连接事件广播)/`EventLoopGroup group` (L126, Netty 线程组)/`HashedWheelTimer timer` (L138, 所有定时任务: 续期+DNS+清理)/`IdleConnectionWatcher` (L140, 空闲回收)/`ElementsSubscribeService` (L144, 订阅, 篇3)/`NatMapper` (L146, NAT 地址映射)/QueueTransferService (L154) + `LockRenewalScheduler` (L156)
- TransportMode 分支 (构造器): EPOLL → EpollEventLoopGroup ("redisson-netty" DefaultThreadFactory), KQUEUE/NIO 类似; **UDS 仅 single 模式 + EPOLL/KQUEUE** (构造器校验, UDS is supported only...)
- `newTimeout` (ServiceManager.java:297): 委托 HashedWheelTimer — DNSMonitor/续期/清理全走它
- `register(LockRenewalScheduler)` (ServiceManager.java:766): 服务注册表模式 — 字段初值 null, 构造器注入; ServiceManager 只持有引用, 生命周期归 Redisson
- shutdown 面: shutdownFutures (ServiceManager.java:404)/shutdownFuturesAsync (ServiceManager.java:416) + `responses`/`lastFutures` 表 (L152/L391, ServiceManager.java) — 统一完成所有在飞请求
关键设计 (q6): ServiceManager = **单例中央服务容器**: 传输/定时/订阅/空闲/NAT 五大件 + 服务注册表扩展点; shutdown 由 shutdownLatch 广播。[模式: 中央服务容器 + register()]
数据流: 任意组件需要定时 → newTimeout(timer); 需要线程 → group; 需要收尾 → shutdownFutures。

### 负面空间 — 初始化刻意不做的事

- **不做热重载**: Config 复制后不可变, 改配置需重建 RedissonClient
- **不推迟连接 (默认)**: lazyInitialization=false 构造即 connect (懒路径篇3)
- **不做多 RedissonClient 共享**: 每个实例独立 Manager/Executor (referenceEnabled 可共享对象但是可选项)
- **不在此解析 RESP**: 协议解析在命令层 (RD-4), 构造器只建通道
- **不做拓扑热切换**: 模式在构造期定死, 运行中不变更 (DNS 切换是地址级非模式级)

→ 引出: 连接池怎么组织?读写怎么路由?池容量怎么锁死?→ [[RD-1-篇2]]
→ 承接验证: s75-boot-redis 的"连接深入在阶段3"在此开始回答 — [[s75-boot-redis]]