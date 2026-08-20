# H-13 DriverDataSource — JDBC 驱动封装 (池的连接来源)

> 项目: HikariCP | 🟡 Working / 1 篇 | DriverDataSource.java(188行)
> 基线: HIKARICP-PLAN H-13 (第4层支撑, 🟡) — JDBC 驱动桥; 前置: **C-11 jdbc DataSource** — 展开 driver→Connection 桥

---

## §0.8

- 🟡 Working，1篇 — Driver 解析策略(构造: 有 driverClassName → 先找 DriverManager 已注册 → 再类加载器直接实例化[TCCL/HikariConfig CL] → 无 className 则按 jdbcUrl 用 DriverManager.getDriver) → getConnection 委托(driver.connect(jdbcUrl, driverProperties)[L127] + 带参版 clone props 注入 USER/PASSWORD[L133-144]) → 与池衔接(PoolBase 用它提供原始连接)
- 设计模式: [模式: 门面/适配]—把 Driver 包成 DataSource; [模式: 多级解析]—driver 定位策略

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| DriverDataSource.java:40,50 | 桥 | **implements DataSource(L40)**: 构造(jdbcUrl, driverClassName, props, user, pass)(L50) | High |
| DriverDataSource.java:64,100,112 | 解析 | **driverClassName 分支(L64)**: DriverManager 已注册(L68)→classloader 直接 newInstance(L100); 无 className→DriverManager.getDriver(jdbcUrl)(L112) | High |
| DriverDataSource.java:127,133,144 | 委托 | **getConnection(L127)**: driver.connect(jdbcUrl, driverProperties); 带参 L133-144 clone props+USER/PASSWORD | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: DriverDataSource 是薄桥(~188行) — 1篇 (~44行) 按"driver 解析 → getConnection 委托 → 与池衔接"展开; 核心是 driver 定位的多级策略。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | Driver 解析策略 (className/classloader/jdbcUrl 多级) | 🔴 | **为什么🔴**: 怎么拿到 JDBC 驱动 |
| P1-2 | getConnection 委托 (driver.connect) | 🔴 | **为什么🔴**: 连接从哪来 |
| P1-3 | 与池衔接 (PoolBase 的连接来源) | 🔴 | **为什么🔴**: 池的叶子依赖 |
| P2-1 | 多 classloader 加载 (TCCL/HikariConfig CL) | 🟡 | **为什么🟡**: 驱动加载范围 |
| P2-2 | 连接属性 (username/password → driverProperties) | 🟡 | **为什么🟡**: 认证注入 |
| P3-1 | 与 C-11/S-10 边界 | 🟢 | **为什么🟢**: jdbc 内核复用 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **driver 解析** | 🔴 | 拿驱动 |
| B | **连接委托** | 🔴 | 连接来源 |
| C | **衔接与边界** | 🟡 | 池依赖 |

> **Cluster A (§1)**: DriverDataSource 构造(driverClassName 多级解析 + jdbcUrl 回退)
> **Cluster B (§2)**: getConnection(driver.connect + 带参 clone props)
> **Cluster C (§3)**: PoolBase 衔接 + C-11 边界

→ 引出 H-2: ConcurrentBag — driver 之后: 无锁并发容器(borrow/requite/PoolEntry 三态), 池"快"的秘密
