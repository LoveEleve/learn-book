# RD-1 篇2 — connection: 连接池的双池模型与路由

> 前置: [[RD-1-篇1]] (五步装配产出 Manager) | 复用: [[h13-datasource]] (Hikari 池生命周期 — 同一"连接池"概念的服务端对照) | 对照: [[h02-concurrentbag]] (无锁容器 vs 信号量池 — 80/20 差异展开) | 引出: [[rd4-command]] (RedisExecutor 借还连接) + [[rd5-rmap]] (r21-db 键空间视图在此承接)
> 🔴 A | 3 KP | [模式: 双池分离 + 三态路由 + 信号量容量]
> Pass 2 闭环: q3(重试/汇聚/集群探测) q5(读写路由) q8(池 permit)

**读者处境**: 你的写请求和读请求走同一个连接池吗?master 挂了谁发现谁切换?5 次重试的边界在哪?为什么说连接池容量"一个 permit 都不能多"?这篇拆 MasterSlaveEntry 的双池模型 (masterPool/slavePool), 读写三态路由 (ReadMode), 以及 AsyncSemaphore 怎么把池容量焊死成协议。

### 概念依赖链
q5(双池路由) ← q3(重试汇聚) ← q8(permit) — 先看池怎么组织/路由, 再看池怎么建 (connect 重试), 最后看池容量协议。

### 核心悬念
"读写为什么分池？一次失败的 connect 会试几次、什么情况直接放弃？池容量为什么不会漂移？"

### 叙事顺序
1. 问题引入: 一次读一次写, 连接从哪里借
2. 双池模型 (q5) — 读写隔离 + ReadMode 三态 + 降级兜底
3. 重试循环 (q3) — 5 次边界 + 有界等待 + detectCluster
4. permit 协议 (q8) — AsyncSemaphore 焊死容量 + 4.6.1 竞态史
5. 收束: 三机制串成"连接池生命周期"一条线, 引出单命令如何借池执行

### 1. 双池模型 — 读写分池, 各管各的

场景: 一次读一次写, 连接从哪借?
源码路径:
- `MasterSlaveEntry` (842 行): 一个主从拓扑 = masterEntry (主连接池) + slaveConnectionPool (从连接池) + 元数据
- 写路径: `connectionWriteOp` (MasterSlaveEntry.java:569) → `masterConnectionPool.get(command, false)` — **写固定走 master 池**
- 读路径: `connectionReadOp` (MasterSlaveEntry.java:585-604) → **ReadMode 三态** (ReadMode.java:26-40): `MASTER` → 走 write 池 / `SLAVE` → slaveConnectionPool / `MASTER_SLAVE` → slave 优先
- SLAVE 模式降级: "Uses MASTER if no SLAVES available" — 从池空时自动回退 master (ConnectionPool 内部逻辑)
- Cluster 多 entry: `rrCounter` (MasterSlaveConnectionManager.java:74) → getEntry floorMod 轮询 (L186, MasterSlaveConnectionManager.java)
- slave 间均衡: balancer/ 包 (RoundRobinBalancer)
- **对照 Hikari** ([[h13-datasource]]): HikariCP 的连接池也是"核心池 + 溢出池"双池思想 — 但 Hikari 借出有 30s maxLifetime/泄漏检测 (h13), Redisson 借出无池层超时 (超时在命令层 RD-4), 生命周期管理两者的差异是"flush 即断" vs "permit 即控"
关键设计 (q5): 双池分离 = **读写路径隔离** (master 池不受读放大影响) + ReadMode 三态把路由决策权交给配置。[模式: 双池 + 路由三态]
数据流: readAsync → entry.connectionReadOp → mode==MASTER? master池 : slave池 → borrow。

### 2. 重试循环 — 5 次尝试的边界

场景: 连不上 master 时, connect() 会试几次?什么情况不试?
源码路径:
- `connect()` (MasterSlaveConnectionManager.java:229-262): `attempt = retryAttempts+1` (=5, BaseConfig:62) — for 循环:
  - L241-243 (MasterSlaveConnectionManager.java): **IllegalArgumentException → shutdown + 直接抛** (配置错误无重试价值)
  - L250-252: **InterruptedException → 直接抛** (中断不吞)
  - L234-236: lastAttempt 标记 (最后一次失败才抛)
  - L253-259: 其他异常 → `retryDelay.calcDelay(attempt)` + sleep (EqualJitterDelay 1-2s, BaseConfig.java:67)
- **doConnect 有界等待** (MasterSlaveConnectionManager.java:286-331): `masterFuture.get(connectTimeout * max(1, masterConnectionMinimumIdleSize))` (L299) + slave 同样有界 (L311) — 注释: "bound the wait even when minimumIdleSize == 0; an unbounded join() never completes the lazyConnect latch if ... stalls, parking all callers" — 防懒连接 latch 永久挂起
- 失败清理: catch → `internalShutdown()` (L322, MasterSlaveConnectionManager.java) — 已建连接全回收再抛
- **detectCluster** (MasterSlaveConnectionManager.java:264-284): EVAL 双 key 脚本 ("test1"/"test2") → `CROSSSLOT` 错误 → `serviceManager.setClusterDetected(true)` — master-slave 模式下自动识别集群节点
关键设计 (q3): 重试协议 = **配置错误/中断不重试 + 有界等待防卡死 + 失败清理避免泄漏**; detectCluster 是 4.x 的拓扑自适应彩蛋。[模式: 定向重试 + 有界 JSON]
数据流: connect → doConnect → setupMaster 有界等 → detectCluster → 就绪。
> ⚠️ 两级重试预告: 本章是 **connect 级重试** (建链); 单条命令的执行重试 (attempts=4 + jitter, timeout=3000ms) 在 RD-4 命令层 — 两层重试共用 BaseConfig.retryAttempts, 但触发点与语义不同 (建链 vs 命令), 这是绝佳面试区分点。

### 3. AsyncSemaphore — 焊死池容量的协议

场景: 池最大 8, 为什么不会变成 9?
源码路径:
- `ConnectionsHolder` (ConnectionsHolder.java:44-59): `allConnections` (ConcurrentLinkedQueue) + `freeConnections` (ConcurrentLinkedDeque) + `freeConnectionsCounter = new AsyncSemaphore(poolMaxSize, eventLoopGroup)` — **permit 数 = 池容量**
- 初始化: `initConnections(minimumIdleSize)` (ConnectionsHolder.java:141) — 每建一连接消费一 permit, **失败必须归还**
- 借用: `acquireConnection` (ConnectionsHolder.java:224) — freeConnections poll 到就用 (零 permit 操作), 空则等 permit + 回调新建; **返回 CompletableFuture<T>, 池层无超时** (超时在命令层 timeout=3000ms, RD-4)
- 归还: `releaseConnection` (ConnectionsHolder.java:263) — 回队 + release permit
- **4.6.1 竞态史** (CHANGELOG): "Fixed - AsyncSemaphore.tryRun() over-increment on cancelled waiters" — 取消的等待者多 release 一次 → 计数 > poolMaxSize → 空闲驱逐失效、池永不排空
- 协议不变式: `counter.getCounter() == poolMaxSize` (ConnectionsHolderTest:56-100 两个测试都断言它)
关键设计 (q8): 信号量 permit = **池容量协议**: 借出不碰信号量 (池满才等), 归还精确配对; 竞态教训 = 取消路径的释放必须恰好一次。[模式: 信号量容量 + 精确释放]
数据流: acquire → poll free 缓冲池 / 满则等 permit → 新建 → 用毕 → 归还回队+permit。

### 负面空间 — 连接池刻意不做的事

- **不做无锁容器** (对照 h02-concurrentbag): 这里用双队列 + 信号量, Hikari 用 ThreadLocal + 交接队列 — 异步模型下信号量的协调语义更配 EventLoop
- **不做连接健康检查探活**: 空闲连接由 IdleConnectionWatcher 定期回收 (Replicated 模式), 无 Hikari 的治愈式 validation
- **不做超时熔断语义**: timeout=3000ms 覆盖的是命令响应 (RD-4), 池本身无熔断状态机
- **不做写放大保护**: master 池满就直接等, 没有"写失败快速失败"的流控开关
- **不做 readMode 动态切换**: 三态是静态配置, 运行中不变 (路由决策无热更)

→ 引出: 单条命令在这池上怎么执行?重试/超时/连接选择的闭环 → [[rd4-command]]
→ 对比: Hikari 为什么能无锁?两种连接池架构的分歧点 → [[h02-concurrentbag]]