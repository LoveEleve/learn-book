# RD-1 主类+连接管理 — 知识规划 (knowledge-planning)

> 项目: Redisson 4.6.2-SNAPSHOT | 🔴 A / 3 篇 (+harness) | Redisson(1532)+Config(1351)+ServiceManager(804)+connection/(39)
> 基线: REDISSON-PLAN RD-1 — 前置: **RD-3 Codec (并行)** — 展开 初始化链→模式工厂→懒连接→重试→集群探测→连接池→读写路由→DNS 切换→订阅
> 双链: 前置 [[rd0-intro]] | 复用 [[s75-boot-redis]] (承接: "连接深入在阶段3") | 对照 [[r20-server]] [[h02-concurrentbag]] [[r21-db]] | 引出 [[rd4-command]] [[rd2-rlock]]

---

## §0.8

- 🔴 A，3篇 — 构造五步(**Redisson.java:66-86: configCopy→ConnectionManager.create→createCommandExecutor→EvictionScheduler+WriteBehindService→register(LockRenewalScheduler)**) → 模式工厂(**ConfigSupport.getConfig 优先级 MasterSlave>Single>Sentinel>Cluster>Replicated + validate, ConnectionManager.create 5 分支, 3 子类只覆写 doConnect: 哨兵发现/槽位/主从同步**) → 懒连接(**lazyConnect 单飞: CAS latch+connectingThread 防自死锁+失败重试, MasterSlaveConnectionManager:190-227; lazy=false 默认→构造即连接**) → 重试循环(**connect: retryAttempts+1=5 次, 配置错/中断不重试, EqualJitter 退避; doConnect 全程有界等待防 latch 卡死**) → 集群探测(**detectCluster: EVAL 双 key→CROSSSLOT→setClusterDetected**) → 连接池(**MasterSlaveEntry 双池: masterPool/slavePool; ConnectionsHolder: free/all 双队列+AsyncSemaphore permit 精确性**) → 读写路由(**ReadMode SLAVE/MASTER/MASTER_SLAVE, connectionReadOp 决策, 写固定 master**) → DNS 切换(**DNSMonitor 自循环+多轮确认防抖+成功才更新, changeMaster 后旧 master 降级为 slave+失败回滚**) → 服务工厂(**ServiceManager: EventLoopGroup(TransportMode 分支)/HashedWheelTimer/订阅/空闲监控/NatMapper, register 注册表, shutdownFutures 统一完成**)
- 设计模式: [模式: 工厂分支+单飞锁+双队列信号量池+轮询切换]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| Redisson.java:66-86 | 初始化链 | 五步固定序; 附属服务依赖命令层产物 | High |
| Config.java:165; ConfigSupport.java:844-859 | 配置模型 | 默认 Kryo5; 5 模式优先级 | High |
| ConnectionManager.java:89-111 | 模式工厂 | 5 分支; 未定义报错; lazy 分支 | High |
| MasterSlaveConnectionManager.java:190-227 | 懒连接 | CAS 单飞+重入防护+失败重试 | High |
| MasterSlaveConnectionManager.java:229-331 | 重试/汇聚 | 5 次循环; 有界等待; internalShutdown 清理 | High |
| MasterSlaveConnectionManager.java:264-284 | 集群探测 | EVAL CROSSSLOT 识别 | High |
| ConnectionsHolder.java:44-59,141,224,263 | 池实体 | 双队列+AsyncSemaphore permit | High |
| MasterSlaveEntry.java:569-604; ReadMode.java:26-40 | 读写路由 | 三态; 写固定 master; SLAVE 无则 master | High |
| DNSMonitor.java:53-283; MasterSlaveEntry.java:500-545 | DNS 切换 | 自循环+多轮确认+成功才提交+降级回滚 | High |
| ServiceManager.java:122-156,297,404-416,766 | 服务工厂 | TransportMode 分支; register; shutdown | High |

---

## 02-04 聚合+分类+聚类 (3篇+harness)

**3篇理由**: ≥6 闭环 (8 个) — 按主题亲和性聚类: 篇1 初始化与配置 (q1/q3前半/q7/q6), 篇2 连接池与路由 (q5/q8/q3后半), 篇3 高可用: 懒连接+DNS+订阅 (q2/q4/q6 订阅面)。harness 验证懒连接单飞/重试/池 permit 精确性。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 初始化链五步 | 🔴 | **为什么🔴**: 全局唯一入口 |
| P1-2 | 模式工厂 5 分支 | 🔴 | **为什么🔴**: 拓扑选择 |
| P1-3 | lazyConnect 单飞 | 🔴 | **为什么🔴**: 并发正确性 |
| P1-4 | 连接池 permit | 🔴 | **为什么🔴**: 池容量根基 |
| P1-5 | 读写分离路由 | 🔴 | **为什么🔴**: 性能核心 |
| P2-1 | DNS 故障切换 | 🟡 | **为什么🟡**: 高可用面 |
| P2-2 | 重试循环+集群探测 | 🟡 | **为什么🟡**: 可靠性面 |
| P2-3 | ServiceManager 工厂 | 🟡 | **为什么🟡**: 支撑面 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **初始化与配置** (q1/q7/q6) | 🔴 | 入口 |
| B | **连接池与路由** (q5/q8/q3) | 🔴 | 数据面 |
| C | **高可用** (q2/q4) | 🟡 | 故障面 |

---

## 05 闭环结论摘要 (Pass 2 内化 — 假设→验证→结论)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 初始化链 | 五步固定序: configCopy→ConnectionManager.create→createCommandExecutor→EvictionScheduler+WriteBehindService→register(LockRenewalScheduler); 附属服务依赖命令层产物故不在 ServiceManager 内建 | Redisson.java:66-86; ServiceManager.java:766 |
| q2 | 懒连接 | 单飞锁: CAS latch 保证单线程连接, isCompletedExceptionally 检测失败可重试, connectingThread 身份检查防自死锁 | MasterSlaveConnectionManager.java:190-227 |
| q3 | 重试/探测 | connect=retryAttempts+1 次, 配置错/中断不重试, jitter 退避; doConnect 全程有界等待; EVAL CROSSSLOT 自动识别集群 | L229-331,264-284 |
| q4 | DNS 切换 | 自循环调度不重叠, dnsMonitoringTimes 多轮确认防抖, changeMaster 成功才更新, 旧 master 降级 slave, 失败回滚 | DNSMonitor.java:53-283; MasterSlaveEntry.java:500-545 |
| q5 | 读写路由 | ReadMode 三态在 connectionReadOp 决策; 写固定 master 池; SLAVE 模式无 slave 降级 master; rrCounter 轮询 | MasterSlaveEntry.java:569-604 |
| q6 | 服务工厂 | EventLoopGroup 按 TransportMode 分支 + HashedWheelTimer + 订阅/空闲监控/NatMapper; register 注册表; shutdownFutures 统一完成 | ServiceManager.java:122-156,766 |
| q7 | 模式工厂 | ConfigSupport 固定优先级 MasterSlave>Single>Sentinel>Cluster>Replicated + validate; 3 子类只覆写 doConnect | ConfigSupport.java:844-859 |
| q8 | 池 permit | AsyncSemaphore(poolMaxSize) 精确释放; init 失败必须归还; 4.6.1 tryRun 竞态教训 (over-increment 永久抬计数) | ConnectionsHolder.java:44-59; ConnectionsHolderTest:56-100 |

→ 引出 RD-4: 命令流水线在连接池上执行 — [[RD-4-command]] ; RD-2 锁消费订阅通道 — [[RD-2-rlock]]
