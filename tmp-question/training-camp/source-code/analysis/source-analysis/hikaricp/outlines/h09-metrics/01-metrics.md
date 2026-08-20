# H-9 指标监控 — IMetricsTracker / PoolStats / MetricsTrackerFactory

> 依赖 H-1 池核心 (复用) | 🟡 Working | 6 KP | [模式: 策略 + 空对象 + 门面]

**读者处境**: 配了 metrics 后能看池的连接/等待统计 — 指标从哪来?怎么接入 Prometheus/Micrometer?为什么默认没指标?

### 1. 指标抽象 — IMetricsTracker + Factory

场景: 池的生命周期事件(建连/借出/归还/超时)怎么变成指标?

源码路径:
- `IMetricsTracker.java:22` — **抽象**: `interface IMetricsTracker extends AutoCloseable`(L22) — 指标追踪契约
- `IMetricsTracker.java:24,26,28,30` — **事件钩子**: `recordConnectionCreatedMillis`(L24)/`recordConnectionAcquiredNanos`(L26)/`recordConnectionUsageMillis`(L28)/`recordConnectionTimeout`(L30) — **默认都是 no-op 空方法**
- `MetricsTrackerFactory.java:19,28` — **工厂**: `create(poolName, poolStats) → IMetricsTracker`(L28) — 按需创建实际实现
- `MetricsTracker.java:25` — **默认空实现**: `class MetricsTracker implements IMetricsTracker`(L25)

关键设计: **Why 默认 no-op？** 指标是可选功能 — IMetricsTracker 方法默认空实现 + 未配置 factory 时用 MetricsTracker 空类, 池零指标开销; 用户配了 Micrometer/Dropwizard 才真正记录。**Why Factory 模式？** 池生命周期事件通过 IMetricsTracker 统一上报, 具体统计实现(计数/计时)由 Factory 决定 — 解耦池逻辑与指标后端。[模式: 策略 + 空对象]

数据流: 池构造时用 MetricsTrackerFactory.create(poolName, poolStats)(L28) 建 IMetricsTracker → 生命周期事件调 recordConnection*(L24-30) → 未配置则空实现无操作。

### 2. 池状态 — PoolStats

场景: /metrics 里的 total/active/idle/pending 连接数哪来的?

源码路径:
- `PoolStats.java:28,33` — **抽象**: `abstract class PoolStats`(L28) + volatile 采样字段 totalConnections 等(L33-38)
- `PoolStats.java:46,98` — **统计**: `getTotalConnections`(L46)/`getIdleConnections`(L55)/`getActiveConnections`(L64)/`getPendingThreads`(L73) — 每个 getter `if (shouldLoad()) update()`(L48-50) — **1 秒懒刷新窗口**缓存采样(volatile); `update()`(L98, 抽象) 由 HikariPool L675-681 实现: 从 ConcurrentBag 各状态计数采样(H-2: total=size, idle=NOT_IN_USE, active=IN_USE, pending=等待线程)

关键设计: **Why PoolStats 抽象 + 懒刷新？** 池状态统计(总/活/闲/待)是监控通用需求, 由 ConcurrentBag 状态计数推导(H-2); 但 getter 不实时算 — volatile 字段缓存 + **1 秒懒刷新窗口**(shouldLoad/update), 避免每次读都遍历 bag(高频监控场景降开销); 抽象类给各种指标后端(metrics/MXBean)统一数据视图, 各实现只需读 getters。[模式: 快照视图 + 懒加载]

数据流: 指标后端读 PoolStats → getter(L46) 触发 shouldLoad?→ update()(L98, HikariPool 实现) 从 bag 采样 → 返回 volatile 缓存值(1 秒窗口内不重算) → 总/闲/活/待。

### 3. 挂钩与集成 — PoolBase delegate + Micrometer

场景: 池在哪些点上报指标?怎么接入实际后端?

源码路径:
- `PoolBase.java:59` — **挂钩点**: `IMetricsTrackerDelegate metricsTracker`(L59) — 池持有指标委托
- `PoolBase.java:405` — **上报**: `metricsTracker.recordConnectionCreated(elapsedMillis(start))`(L405) — 建连时上报耗时
- `PoolBase.java:720,741` — **delegate**: 内部 IMetricsTrackerDelegate(默认 no-op, L720) + MetricsTrackerDelegate(L741) 包装外部 IMetricsTracker
- `HikariConfig.java:97` — **配置**: `metricsTrackerFactory`(L97) — 配了才有实际实现
- 集成: `metrics/micrometer/MicrometerMetricsTracker`、dropwizard、prometheus — 各实现 IMetricsTracker

关键设计: **Why 内部 delegate + 外部 tracker 两层？** PoolBase 内部用 IMetricsTrackerDelegate(默认 no-op), 用户配置的 IMetricsTracker 经 MetricsTrackerDelegate 包装接入 — 池逻辑不依赖具体指标库, 外部实现可插拔。**Why 各钩子点分散？** 建连/借出/归还/超时各处调 recordConnection* — 覆盖完整生命周期, 各后端统一消费。[模式: 委托 + 可插拔]

数据流: 配 metricsTrackerFactory=micrometer → PoolBase 建 delegate(L59) 包装 MicrometerMetricsTracker → 建连时 recordConnectionCreated(L405)、借出 recordConnectionAcquiredNanos、归还 recordConnectionUsageMillis、超时 recordConnectionTimeout → Micrometer 记录到 MeterRegistry(Prometheus 导出)。

→ 引出 H-10: JMX — 指标之后: HikariPoolMXBean/PoolConfigurationMXBean 的池状态暴露(前置 H-1)。
