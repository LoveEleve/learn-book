# H-9 指标监控 — IMetricsTracker / PoolStats / MetricsTrackerFactory

> 项目: HikariCP | 🟡 Working / 1 篇 | IMetricsTracker.java(40行)+MetricsTracker.java(30行)+PoolStats.java(100行)+MetricsTrackerFactory.java(30行)+Micrometer/Dropwizard/Prometheus 实现
> 基线: HIKARICP-PLAN H-9 (第3层代理监控, 🟡) — 池指标桥; 前置: **H-1 池核心** — 展开指标抽象+挂钩+集成

---

## §0.8

- 🟡 Working，1篇 — 指标抽象(IMetricsTracker[L22]: recordConnectionCreatedMillis/AcquiredNanos/UsageMillis/Timeout[L24-30] 默认 no-op; MetricsTracker 默认空实现[L25]; MetricsTrackerFactory.create[L19/28]) → 池状态(PoolStats[L28]: getTotal/Idle/Active/Pending 从 ConcurrentBag 算[L46-73]) → 挂钩与集成(PoolBase.metricsTracker delegate[L59] + recordConnectionCreated[L405] 等钩子; 配置 setMetricsTrackerFactory 才有实际实现, 默认 no-op 零开销)
- 设计模式: [模式: 策略]—IMetricsTracker 可替换; [模式: 空对象]—默认 no-op; [模式: 门面]—MetricsTrackerFactory

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| IMetricsTracker.java:22,24,26,28,30 | 抽象 | **interface(L22)+默认 no-op 方法(L24-30)** | High |
| MetricsTrackerFactory.java:19,28 | 工厂 | **create(poolName, poolStats)→IMetricsTracker(L28)** | High |
| PoolStats.java:28,46,73 | 池状态 | **abstract(L28)+getTotalConnections(L46)/getIdle(L55)/getActive(L64)/getPending(L73)** | High |
| PoolBase.java:59,405 | 挂钩 | **metricsTracker delegate(L59)+recordConnectionCreated(L405)** | High |
| HikariConfig.java:97 | 配置 | **metricsTrackerFactory(L97, 默认为 null=no-op)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 指标是一条线(抽象→状态→挂钩), 3 块耦合但机制较薄 — 1篇 (~46行) 按"指标抽象 → 池状态 → 挂钩集成"展开; H-1 复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 指标抽象 (IMetricsTracker + 默认 no-op) | 🔴 | **为什么🔴**: 指标怎么定义 |
| P1-2 | PoolStats 池状态 (从 ConcurrentBag 算) | 🔴 | **为什么🔴**: 池统计怎么来 |
| P1-3 | 挂钩与默认零开销 | 🔴 | **为什么🔴**: 未配置不耗资源 |
| P2-1 | 集成实现 (Micrometer/Dropwizard/Prometheus) | 🟡 | **为什么🟡**: 实际指标后端 |
| P2-2 | 配置 (setMetricsTrackerFactory) | 🟡 | **为什么🟡**: 怎么开启 |
| P3-1 | 与 H-1/H-3/H-4 边界 | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **指标抽象** | 🔴 | 定义 |
| B | **池状态** | 🔴 | 统计 |
| C | **挂钩集成** | 🟡 | 接入 |

> **Cluster A (§1)**: IMetricsTracker + MetricsTrackerFactory + 默认 no-op
> **Cluster B (§2)**: PoolStats(从 ConcurrentBag 算总/活/闲/待)
> **Cluster C (§3)**: PoolBase delegate 挂钩 + Micrometer 集成 + 配置

→ 引出 H-10: JMX — 指标之后: HikariPoolMXBean/PoolConfigurationMXBean 的池状态暴露(前置 H-1)
