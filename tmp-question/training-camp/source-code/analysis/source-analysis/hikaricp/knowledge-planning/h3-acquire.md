# H-3 获取流程 — HikariPool.getConnection → borrow → createProxyConnection

> 项目: HikariCP | 🔴 Deep / 1 篇 | HikariPool.java(911行)+PoolEntryCreator.java
> 基线: HIKARICP-PLAN H-3 (第1层池核心, 🔴) — 获取编排; 前置: **H-2 ConcurrentBag(容器) + H-5 生命周期(建连) + H-13 DriverDataSource** — 展开借出链+动态扩池

---

## §0.8

- 🔴 Deep，1篇 — getConnection 主流程(HikariPool.getConnection[L152]: suspendResumeLock.acquire[L154]→connectionBag.borrow[L160]→evicted/dead 校验[L166]→createProxyConnection+leakTask[L179]→超时 createTimeoutException[L184]) → 动态扩池(addBagItem[L341]: waiting>队列大小→addConnectionExecutor.submit(poolEntryCreator)[L344] 异步建连) → createPoolEntry(L485)+addConnectionExecutor 线程池(L72/113, 边界)
- 设计模式: [模式: 门面编排]—getConnection; [模式: 异步补货]—poolEntryCreator; [模式: 边界回退]—超时异常

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HikariPool.java:152,154,160,166,179,184 | 主流程 | **getConnection(L152)**: acquire(L154)→borrow(L160)→evicted/dead 校验(L166)→createProxyConnection+leakTask(L179)→超时异常(L184) | High |
| HikariPool.java:341,343,344 | 扩池 | **addBagItem(L341)**: waiting>队列大小→submit(poolEntryCreator)(L344) | High |
| HikariPool.java:485 | 建连 | **createPoolEntry(L485)** — 新连接条目 | High |
| HikariPool.java:72,113 | 线程池 | **addConnectionExecutor(L72/113)** — 异步加连接线程池 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 获取是一条线(主流程→扩池→建连), 3 块耦合 — 1篇 (~48行) 按"主流程 → 动态扩池 → 建连与线程池"展开; H-2 borrow/H-5 建连复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | getConnection 主流程 (borrow + evicted/dead 校验) | 🔴 | **为什么🔴**: 获取连接的编排 |
| P1-2 | createProxyConnection 返回 (代理+leakTask) | 🔴 | **为什么🔴**: 用户拿到什么 |
| P1-3 | 动态扩池 addBagItem → poolEntryCreator (异步) | 🔴 | **为什么🔴**: 借不到怎么办 |
| P2-1 | addConnectionExecutor 线程池 (边界) | 🟡 | **为什么🟡**: 扩池并发控制 |
| P2-2 | createPoolEntry (新连接条目) | 🟡 | **为什么🟡**: 新连接创建 |
| P3-1 | 与 H-2/H-5/H-12 边界 | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **主流程** | 🔴 | 获取编排 |
| B | **扩池** | 🔴 | 借不到补货 |
| C | **建连与边界** | 🟡 | 新连接 |

> **Cluster A (§1)**: getConnection(acquire→borrow→校验→proxy)
> **Cluster B (§2)**: addBagItem→addConnectionExecutor.submit(poolEntryCreator)
> **Cluster C (§3)**: createPoolEntry + 线程池边界 + H-5/H-12 复用

→ 引出 H-5: 生命周期 — 获取之后: HikariConfig 校验/seal→PoolBase.newConnection 的连接创建(前置 C-11)
