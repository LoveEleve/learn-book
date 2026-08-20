# H-1 核心架构 — HikariConfig → HikariDataSource → HikariPool → ConcurrentBag

> 项目: HikariCP (JDBC 连接池) | 🔴 Deep / 1 篇 | HikariDataSource(376行)+HikariConfig(1269行)+HikariPool(911行)+ConcurrentBag(414行)+PoolBase(793行)
> 基线: HIKARICP-PLAN H-1 (总览) — 池全链路导航; 前置: **S-10 Boot DataSource(引出: 本域是 S-10 池化的深入) + C-11 jdbc DataSource** — 展开池整体架构

---

## §0.8

- 🔴 Deep，1篇 — 入口门面(HikariDataSource extends HikariConfig implements DataSource,Closeable: 懒加载池——无参构造[L59]首次 getConnection 才建池[L111], 带参构造急建池[L80]) → 池核心(HikariPool extends PoolBase implements HikariPoolMXBean,IBagStateListener: 持有 ConcurrentBag<PoolEntry>[L75/92] + HouseKeeper 定时维护[L118]; getConnection[L140]→connectionBag.borrow[L160]) → 架构全链路(Config→DataSource→Pool→ConcurrentBag→PoolEntry→PoolBase)
- 设计模式: [模式: 门面]—HikariDataSource; [模式: 懒加载]—首次 getConnection 建池; [模式: 组合]—Pool 聚合 ConcurrentBag

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HikariDataSource.java:40,80,111 | 门面 | **extends HikariConfig implements DataSource,Closeable(L40)**: 带参构造 L80 new HikariPool 急建; 无参 L111 首次 getConnection 懒建 | High |
| HikariPool.java:52,75,92,118 | 池核心 | **extends PoolBase implements HikariPoolMXBean,IBagStateListener(L52)**: connectionBag(L75/92 new ConcurrentBag)+HouseKeeper 定时(L118) | High |
| HikariPool.java:140,160 | 获取入口 | **getConnection(L140)→connectionBag.borrow(timeout)(L160)** | High |
| PoolBase.java:54,210 | 基类 | **abstract(L54)**: new PoolEntry(newConnection)(L210) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 核心架构是总览导航 — 1篇 (~48行) 按"入口门面 → 池核心 → 全链路与衔接"展开; 只命名组件与角色+初始化, 深机制(borrow/requite)留给 H-2~H-4 (避免 forward reference)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 入口门面 HikariDataSource (懒加载池) | 🔴 | **为什么🔴**: 池怎么被启动 |
| P1-2 | 池核心 HikariPool (ConcurrentBag+HouseKeeper) | 🔴 | **为什么🔴**: 池的内部结构 |
| P1-3 | 架构全链路 (Config→DataSource→Pool→Bag→Entry→Base) | 🔴 | **为什么🔴**: 组件关系 |
| P2-1 | 懒 vs 急初始化 (两构造差异) | 🟡 | **为什么🟡**: 何时建池 |
| P2-2 | 首次 getConnection 建池 (double-checked) | 🟡 | **为什么🟡**: 懒加载实现 |
| P3-1 | 与 S-10 Boot DataSource 衔接 (知识网络) | 🟢 | **为什么🟢**: 池化内核消费方 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **入口门面** | 🔴 | 池启动 |
| B | **池核心** | 🔴 | 内部结构 |
| C | **全链路与衔接** | 🟡 | 架构图 |

> **Cluster A (§1)**: HikariDataSource(懒/急建池 + getConnection)
> **Cluster B (§2)**: HikariPool(ConcurrentBag + HouseKeeper + borrow)
> **Cluster C (§3)**: 全链路 + 与 S-10 衔接

→ 引出 H-13: DriverDataSource — 架构基石: JDBC 驱动的封装(getConnection 底层的连接来源, 池的叶子依赖)
