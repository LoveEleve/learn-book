# HikariCP 源码学习范围规划

> **版本**: v7.0.2
> **仓库**: `/data/workspace/source-code/code/spring/hikaricp/`
> **规模**: 单模块，48 个源文件，约 9200 行
> **日期**: 2026-08-03

---

## 一、仓库概况

HikariCP 是 Java 生态中性能最高的 JDBC 连接池实现。单模块项目，代码极度精简，大量性能优化技巧（ThreadLocal 快速路径、Javassist 字节码注入、AtomicIntegerFieldUpdater 无锁 CAS）。

**包结构**（5 个包，全部审计）：

| 包 | 文件数 | 职责 | 状态 |
|---|---|---|---|
| `com.zaxxer.hikari` | 7 | 配置 + DataSource 入口 + MXBean 接口 | ✅ 已探索 |
| `com.zaxxer.hikari.pool` | 12 | 连接池核心：HikariPool + PoolBase + PoolEntry + 代理类 | ✅ 已探索 |
| `com.zaxxer.hikari.util` | 10 | 工具类：ConcurrentBag / DriverDataSource / SuspendResumeLock 等 | ✅ 已探索 |
| `com.zaxxer.hikari.metrics` | 17 | 指标监控：IMetricsTracker + Dropwizard(6)/Micrometer(2)/Prometheus(5) 三种实现 | ✅ 已探索 |
| `com.zaxxer.hikari.hibernate` | 2 | Hibernate 集成（HikariConnectionProvider） | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（6 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| H-1 | **连接池核心架构** | HikariConfig → HikariDataSource → HikariPool | 完整启动链路：配置验证→DataSource 初始化→HikariPool 构造→HouseKeeper 启动；`fastPathPool` 消除 volatile 读开销；**配置密封(sealing)机制**：池启动后所有 setter 调用 `checkIfSealed()` 抛异常，只有 MXBean 标记的 volatile 字段可运行时修改；**fail-fast**：`checkFailFast()` 按 `initializationFailTimeout` 三种策略（-1 跳过 / 0 验证不乱 / >0 阻塞至成功或超时）；`blockUntilFilled`（JVM 参数）并行预填连接 |
| H-2 | **ConcurrentBag 无锁并发设计** | ConcurrentBag, IConcurrentBagEntry, PoolEntry 状态机 | 三层加速：ThreadLocal 本地缓存（最多 16 个）→ CopyOnWriteArrayList 共享扫描 → SynchronousQueue 线程间传递；**PoolEntry 状态机**：`AtomicIntegerFieldUpdater` 管理 NOT_IN_USE→RESERVED（reserve）→IN_USE（borrow CAS）→NOT_IN_USE（requite）→REMOVED（remove）；WeakReference 防止 ClassLoader 泄漏；`values()` 返回 CopyOnWriteArrayList 快照，操作前必须 reserve() |
| H-3 | **连接获取完整流程** | HikariPool.getConnection() | suspendResumeLock.acquire → connectionBag.borrow(timeout) → 检查 evicted/dead（aliveBypassWindow 500ms 免检优化）→ beginRequest（可选）→ poolEntry.createProxyConnection → 返回 ProxyConnection |
| H-4 | **连接归还与连接驱逐** | ProxyConnection.close(), checkException(), PoolBase.resetConnectionState() | **归还流程**：close()→closeStatements()→未提交事务 rollback→dirtyBits 位掩码追踪 6 状态变更→resetConnectionState 恢复默认→poolEntry.recycle()→requite；**驱逐决策链**：checkException() 多层判断（SQLState "08" 前缀 / ERROR_STATES 集合 / ERROR_CODES 集合 / SQLExceptionOverride）→标记为 evicted → 下次 borrow 关闭 |
| H-5 | **连接生命周期管理** | HikariPool.createPoolEntry(), MaxLifetimeTask, KeepaliveTask | maxLifetime（默认 30min）+ 方差随机化（最多 25%，lifeTimeVarianceFactor=4）防止雪崩；keepaliveTime（默认 2min）+ 方差避免同时检测；endOfLife + keepalive ScheduledFuture 管理 |
| H-6 | **HouseKeeper 后台维护** | HikariPool.HouseKeeper, ClockSource | 30s 周期（housekeepingPeriodMs）：MBean 配置刷新→**ClockSource 平台自适应时钟**（NanosecondClockSource 默认/MillisecondClockSource Mac 兜底）→NTP 时钟回拨检测（128ms 容差，回拨触发全池软驱逐；前跳只 warn）→idleTimeout 空闲淘汰（`reserve()` CAS 锁定后关闭）→fillPool 补到 minIdle |

### 🟡 扩展域（7 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| H-7 | **连接验证机制** | PoolBase.isConnectionDead() | JDBC4 `Connection.isValid(seconds)` 优先（isUseJdbc4Validation）；无 testQuery 配时才用 connectionTestQuery；aliveBypassWindow（500ms）避免频繁验证最近归还的连接；setNetworkTimeout 验证超时控制 |
| H-8 | **连接泄漏检测** | ProxyLeakTask, ProxyLeakTaskFactory | leakDetectionThreshold 配置后，borrow 时 schedule 定时任务；close() 时 cancel；超时触发 WARN 日志 + 完整调用栈；归还后打印 "unleaked" 日志 |
| H-9 | **指标监控体系** | IMetricsTracker, MetricsTrackerFactory, PoolStats | 5 个指标点：连接创建耗时 / 获取等待时间 / 使用时长 / 超时次数 / 关闭；3 种实现：Dropwizard（3.x + 5.x）/ Micrometer / Prometheus；PoolStats 1 秒轮询分辨率 |
| H-10 | **JMX 运行时管理** | PoolBase.handleMBeans(), HikariPoolMXBean, HikariConfigMXBean | 注册 com.zaxxer.hikari:type=Pool + PoolConfig 两个 MBean；mxBean 属性运行时可变（maxPoolSize/idleTimeout 等）；sealed 后只能通过 MXBean 修改 |
| H-11 | **连接池挂起/恢复** | SuspendResumeLock | Semaphore(10000 permits) 实现优雅停服；suspend() 一次性 acquire 全部 permits 阻塞所有 getConnection()；resume() 释放全部 + fillPool；默认禁用（FAUX_LOCK 假锁 JIT 优化掉） |
| H-12 | **代理对象生成** | ProxyFactory, JavassistProxyFactory | ProxyConnection/Statement/PreparedStatement/CallableStatement/ResultSet/DatabaseMetaData 六种代理；默认 Javassist 字节码注入替代 java.lang.reflect.Proxy 反射；方法体编译时替换（源码中故意抛异常） |
| H-13 | **DriverDataSource** | DriverDataSource | 实现 DataSource 接口，包装 JDBC DriverManager；支持 driverClassName 直接指定或 jdbcUrl 自动推断；MySQL 特殊处理（SynchronousExecutor 绕过 Bug 75615）；密码脱敏日志 |

---

## 三、淘汰清单

| 子模块/类 | 理由 | 类型 |
|---|---|---|
| `hikari.hibernate.*` (HikariConnectionProvider / HikariConfigurationUtil) | 用户用 MyBatis-Plus，不用 Hibernate | Hibernate 特有集成 |
| `HikariJNDIFactory` | JNDI 数据源查找，Spring Boot 用 `spring.datasource.url` 直连 | 现代项目不用的旧模式 |
| `metrics.dropwizard.CodahaleMetricsTracker` | Dropwizard Metrics 3.x，已过时；Spring Boot 默认 Micrometer | 过时框架 |
| `metrics.dropwizard.CodahaleHealthChecker` | Dropwizard HealthCheck，Spring Boot Actuator HealthIndicator 替代 | Actuator 替代 |
| `metrics.dropwizard.Dropwizard5MetricsTracker` | 用 Micrometer 即可覆盖 | 多实现取其核心 |
| `metrics.prometheus.*`（4 个文件） | Micrometer 桥接 Prometheus 更通用 | 多实现取其核心 |
| OSGi 相关 | 项目不用 OSGi | 打包方式 |
| `SQLExceptionOverride` | 面试低频，生产一般用默认策略即可 | 边缘扩展点 |
| `HikariCredentialsProvider` | 运行时动态凭据，面试低频 | 边缘扩展点 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 6 |
| 🟡 扩展域 | 7 |
| **总域** | **13** |
| 淘汰子模块 | 5 个包/类 |
| 子模块总数 | 5 个包（全部审计） |

---

## 五、与 Framework/Boot 规划的交叉覆盖

| 交叉点 | HikariCP 域 | Boot 域 | 关系 |
|---|---|---|---|
| DataSource 自动装配 | H-1 核心架构 | B-9 DataSource/HikariCP | Boot 讲自动装配机制，HikariCP 讲池内部实现 |
| 连接验证 | H-7 连接验证 | B-9 | Boot 配 `spring.datasource.hikari.validation-timeout` |
| 连接泄漏 | H-8 泄漏检测 | B-9 | Boot 配 `spring.datasource.hikari.leak-detection-threshold` |
| 指标监控 | H-9 指标监控 | B-21 Actuator | Actuator 通过 Micrometer 暴露 hikaricp_connections_* 指标 |

**原则**：Boot 域讲"怎么配、自动装配做了什么"，HikariCP 域讲"池内部怎么实现"。不重复。

---

## 六、学习顺序建议

```
H-1 核心架构（了解整体） 
  → H-3 获取流程 + H-4 归还流程（理解借还）
    → H-2 ConcurrentBag（理解存储层）
      → H-5 生命周期 + H-6 HouseKeeper（理解维护层）
        → H-7~H-13 按需深入
```

以上规划完成，共 **6🔴+7🟡=13 域**，等你确认。
