# H-10 JMX — HikariPoolMXBean / HikariConfigMXBean / handleMBeans 注册

> 项目: HikariCP | 🟡 Working / 1 篇 | HikariPoolMXBean.java(95行)+HikariConfigMXBean.java(190行)+PoolBase.java(handleMBeans)
> 基线: HIKARICP-PLAN H-10 (第3层代理监控, 🟡) — MXBean 暴露; 前置: **H-1 池核心** — 展开 MXBean 接口+注册

---

## §0.8

- 🟡 Working，1篇 — 池 MXBean(HikariPoolMXBean[L26]: getIdleConnections[L38]/getActiveConnections[L50]/getTotalConnections[L58]/getThreadsAwaitingConnection[L66]/softEvictConnections[L72]/suspendPool[L81]/resumePool[L90]) → 配置 MXBean(HikariConfigMXBean[L26]: get/setConnectionTimeout[L35/44]/MinimumIdle[L123/132]/MaximumPoolSize[L140/152] — 运行期可改) → 注册机制(PoolBase.handleMBeans[L277]: isRegisterMbeans 开关[L278-280] + ManagementFactory.getPlatformMBeanServer[L286] + ObjectName[type=PoolConfig/Pool][L290-294] + registerMBean[L298-299])
- 设计模式: [模式: MXBean 暴露]—JMX 管理; [模式: 开关]—isRegisterMbeans 默认关

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HikariPoolMXBean.java:26,38,72,81 | 池MXBean | **interface(L26)**: 状态 getters(L38/50/58/66)+操作 softEvict(L72)/suspend(L81)/resume(L90) | High |
| HikariConfigMXBean.java:26,44,152 | 配置MXBean | **interface(L26)**: get/set ConnectionTimeout(L35/44)/MaximumPoolSize(L140/152) | High |
| PoolBase.java:277,278,286,298 | 注册 | **handleMBeans(L277)**: 开关(L278)→PlatformMBeanServer(L286)→registerMBean(L298-299) | High |
| HikariConfig.java:90,801 | 开关 | **isRegisterMbeans(L90)+setRegisterMbeans(L801)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: JMX 是一条线(接口→注册), 机制较薄 — 1篇 (~44行) 按"池MXBean → 配置MXBean → 注册机制"展开; H-1/H-9 复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | HikariPoolMXBean (状态+操作) | 🔴 | **为什么🔴**: 池怎么被 JMX 管理 |
| P1-2 | HikariConfigMXBean (运行期可改配置) | 🔴 | **为什么🔴**: 配置热改 |
| P1-3 | handleMBeans 注册机制 (开关+PlatformMBeanServer) | 🔴 | **为什么🔴**: MXBean 怎么注册 |
| P2-1 | ObjectName 命名 (type=Pool/PoolConfig) | 🟡 | **为什么🟡**: JMX 路径 |
| P2-2 | 与 H-1/H-9 关系 (状态来自池) | 🟡 | **为什么🟡**: 数据来源 |
| P3-1 | 边界 | 🟢 | **为什么🟢**: 复用 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **池MXBean** | 🔴 | 管理池 |
| B | **配置MXBean** | 🔴 | 热改配置 |
| C | **注册与边界** | 🟡 | 挂载 |

> **Cluster A (§1)**: HikariPoolMXBean(状态 getters + softEvict/suspend/resume)
> **Cluster B (§2)**: HikariConfigMXBean(get/set 配置)
> **Cluster C (§3)**: handleMBeans 注册 + ObjectName + 边界

→ 引出 H-11: SuspendResumeLock — JMX 之后: 池暂停/恢复的挂起锁(前置 H-1)
