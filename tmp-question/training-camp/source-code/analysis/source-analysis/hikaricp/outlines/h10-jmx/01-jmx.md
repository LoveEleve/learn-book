# H-10 JMX — HikariPoolMXBean / HikariConfigMXBean / handleMBeans 注册

> 依赖 H-1 池核心 (复用) | 🟡 Working | 6 KP | [模式: MXBean 暴露 + 开关]

**读者处境**: 用 JConsole 连上后能看到 `com.zaxxer.hikari:type=Pool` — 那是谁注册的?能做什么?为什么默认看不到?

### 1. 池 MXBean — HikariPoolMXBean

场景: JMX 里怎么查看/操作池?能做什么操作?

源码路径:
- `HikariPoolMXBean.java:26` — **接口**: `interface HikariPoolMXBean`(L26) — 池的管理契约
- `HikariPoolMXBean.java:38,50,58,66` — **状态**: `getIdleConnections`(L38)/`getActiveConnections`(L50)/`getTotalConnections`(L58)/`getThreadsAwaitingConnection`(L66) — 池状态视图(数据来自 H-1 池)
- `HikariPoolMXBean.java:72,81,90` — **操作**: `softEvictConnections`(L72)/`suspendPool`(L81)/`resumePool`(L90) — JMX 控制台可触发

关键设计: **Why MXBean 接口？** JMX 规定"接口名以 MXBean 结尾 + 实现类"即可自动暴露 getters/操作 — HikariPool 实现它, JConsole 直接看到状态与可调操作; 无需额外适配层。[模式: MXBean 约定]

数据流: JConsole 查 com.zaxxer.hikari:type=Pool → 调 getIdleConnections(L38) 等 → 返回池状态; 点 softEvictConnections(L72) → 软淘汰连接(H-6 同机制)。

### 2. 配置 MXBean — HikariConfigMXBean

场景: JMX 里怎么热改配置?改了什么生效?

源码路径:
- `HikariConfigMXBean.java:26` — **接口**: `interface HikariConfigMXBean`(L26)
- `HikariConfigMXBean.java:35,44` — **超时**: `getConnectionTimeout`(L35)/`setConnectionTimeout`(L44)
- `HikariConfigMXBean.java:123,132,140,152` — **池大小**: `getMinimumIdle`(L123)/`setMinimumIdle`(L132)/`getMaximumPoolSize`(L140)/`setMaximumPoolSize`(L152)

关键设计: **Why 配置 MXBean 可 set？** H-5 的 seal 守卫"多数" setter(防并发不一致)— 但**无 checkIfSealed 守卫的 setter 共 14 个**(setCatalog/ConnectionTimeout/IdleTimeout/LeakDetectionThreshold/MaxLifetime/MaximumPoolSize/MinimumIdle/Password/Username/Credentials/ValidationTimeout/MetricsTrackerFactory/MetricRegistry/KeepaliveTime), 其中 **11 个经 MXBean 暴露**(JMX 热改, 如动态调最大池大小), 另有 metricsTrackerFactory/metricRegistry/keepaliveTime 也免守卫(非 MXBean); 这是 seal 之外的热改窗口。**Why 分开两个 MXBean？** 池状态/操作(HikariPoolMXBean)与配置(HikariConfigMXBean)分开 — 职责清晰, JMX 树里也分开。[模式: 受控热改]

数据流: JConsole setMaximumPoolSize(200)(L152) → 改配置 → 池后续扩容按新上限(H-3 建连/H-6 fillPool 用新值)。

### 3. 注册机制 — handleMBeans

场景: MXBean 什么时候/怎么注册到 MBeanServer?为什么默认没有?

源码路径:
- `PoolBase.java:277` — **注册入口**: `handleMBeans(hikariPool, register)`(L277)
- `PoolBase.java:278,280` — **开关**: `if (!config.isRegisterMbeans()) return;`(L278-280) — 默认 false 不注册
- `PoolBase.java:286` — **服务器**: `ManagementFactory.getPlatformMBeanServer()`(L286) — 平台 MBeanServer
- `PoolBase.java:290,294` — **命名**: `new ObjectName("com.zaxxer.hikari:type=PoolConfig/Pool (poolName)")`(L290-294)
- `PoolBase.java:298,299` — **注册**: `mBeanServer.registerMBean(config, beanConfigName)` + `registerMBean(hikariPool, beanPoolName)`(L298-299)
- `HikariConfig.java:90,801` — **开关配置**: `isRegisterMbeans`(L90, 默认 false)/`setRegisterMbeans`(L801)

关键设计: **Why 默认关？** JMX 注册有开销且暴露管理接口(可改配置/挂起池) — 默认关闭, 用户显式配 `setRegisterMbeans(true)` 才启用; 这是"默认安全, 按需管理"的权衡。**Why PlatformMBeanServer？** 用平台服务器而非新建 — JConsole/JVisualVM 直接可见, 无需额外连接。[模式: 开关 + 平台服务器]

数据流: 配 isRegisterMbeans=true → 池构造 → handleMBeans(pool, true)(L277) → 开关放行 → PlatformMBeanServer(L286) → registerMBean(config, PoolConfig)(L298) + registerMBean(pool, Pool)(L299) → JConsole 可见。

→ 引出 H-11: SuspendResumeLock — JMX 之后: 池暂停/恢复的挂起锁(前置 H-1)。
