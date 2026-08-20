# H-4 归还流程 — ProxyConnection.close → HikariPool.recycle → ConcurrentBag.requite

> 项目: HikariCP | 🔴 Deep / 1 篇 | ProxyConnection.java(340行)+HikariPool.java(911行)+ConcurrentBag.java(414行)
> 基线: HIKARICP-PLAN H-4 (第1层池核心, 🔴) — 归还编排; 前置: **H-2 ConcurrentBag(requite) + H-12 代理(ProxyConnection)** — 展开归还链+状态复位

---

## §0.8

- 🔴 Deep，1篇 — ProxyConnection.close(代理关闭链[L240]: closeStatements[L243]→leakTask.cancel[L246]→脏事务 rollback[L250]→dirtyBits resetConnectionState[L255]→clearWarnings[L258]) → HikariPool.recycle(回收编排[L434]: recordConnectionUsage[L436]→isMarkedEvicted? closeConnection[L438] : requite[L447]) → ConcurrentBag.requite(回容器, H-2: handoff 给等待者或回 thread-local)
- 设计模式: [模式: 代理拦截]—ProxyConnection.close; [模式: 回收分派]—recycle evicted vs requite; [模式: 状态复位]—resetConnectionState

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ProxyConnection.java:240,243,246 | close | **close(L240)**: closeStatements(L243)→leakTask.cancel(L246) | High |
| ProxyConnection.java:250,255,258 | 复位 | **脏事务 rollback(L250)→dirtyBits resetConnectionState(L255)→clearWarnings(L258)** | High |
| HikariPool.java:434,436,437 | recycle | **recycle(L434)**: recordConnectionUsage(L436)→isMarkedEvicted? closeConnection(L438): requite(L447) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 归还是一条线(代理关闭→回收→回容器), 3 块耦合 — 1篇 (~48行) 按"代理关闭 → 回收分派 → 回容器"展开; H-2 requite/H-12 代理复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | ProxyConnection.close (代理关闭链) | 🔴 | **为什么🔴**: 归还入口 |
| P1-2 | HikariPool.recycle (回收分派: evicted vs requite) | 🔴 | **为什么🔴**: 归还怎么走 |
| P1-3 | ConcurrentBag.requite (回容器复用 H-2) | 🔴 | **为什么🔴**: 连接归哪 |
| P2-1 | 脏状态处理 (rollback + resetConnectionState) | 🟡 | **为什么🟡**: 状态复位 |
| P2-2 | closeStatements + leakTask.cancel | 🟡 | **为什么🟡**: 收尾 |
| P3-1 | 与 H-2/H-12 边界 | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **代理关闭** | 🔴 | 归还入口 |
| B | **回收分派** | 🔴 | 归还没失效 |
| C | **状态与边界** | 🟡 | 复位 |

> **Cluster A (§1)**: ProxyConnection.close(closeStatements + 脏处理 + 复位)
> **Cluster B (§2)**: HikariPool.recycle(evicted→close, 否则 requite)
> **Cluster C (§3)**: ConcurrentBag.requite(复用 H-2) + 边界

→ 引出 H-6: HouseKeeper — 归还之后: 30s 定时维护(idleTimeout 淘汰/fillPool 补 minIdle/ClockSource)
