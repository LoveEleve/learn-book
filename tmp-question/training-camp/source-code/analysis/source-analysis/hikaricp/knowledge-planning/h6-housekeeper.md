# H-6 HouseKeeper + ClockSource — 30s 后台维护 (idleTimeout 淘汰 / fillPool 补 minIdle)

> 项目: HikariCP | 🔴 Deep / 1 篇 | HikariPool.java(HouseKeeper L793)+ClockSource.java(160行)
> 基线: HIKARICP-PLAN H-6 (第2层后台维护, 🔴) — 定时维护; 前置: **H-3 获取/H-4 归还/H-5 连接创建** — 展开 HouseKeeper + ClockSource

---

## §0.8

- 🔴 Deep，1篇 — 定时调度(HouseKeeper implements Runnable[L793]: scheduleWithFixedDelay 100ms 初延 + housekeepingPeriodMs[默认30s][L63/118]) → idleTimeout 淘汰(idleTimeout>0 && minIdle<max[L830]: 对 not-in-use 中 idle 超时者 reserve[L835]+closeConnection"(connection has passed idleTimeout)"[L836]) → fillPool 补 minIdle(fillPool(true)[L845]) + 时钟回拨检测(plusMillis(now,128)<plusMillis(previous,period) → softEvictConnections[L816-820])
- 设计模式: [模式: 定时任务]—scheduleWithFixedDelay; [模式: 淘汰策略]—idleTimeout 超时淘汰; [模式: 时钟抽象]—ClockSource

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HikariPool.java:793,63,118 | 调度 | **HouseKeeper(L793)+period 默认30s(L63)+scheduleWithFixedDelay(L118)** | High |
| HikariPool.java:830,835,836 | 淘汰 | **idleTimeout>0&&minIdle<max(L830): reserve(L835)+close idle 超时(L836)** | High |
| HikariPool.java:845 | 补minIdle | **fillPool(true)(L845)** | High |
| HikariPool.java:816,820 | 时钟检测 | **retrograde clock(L816)→softEvictConnections(L820)** | High |
| ClockSource.java:45,84 | 时钟 | **currentTime(L45)+elapsedMillis(L84)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 后台维护是一条线(调度→淘汰→补货+时钟), 3 块耦合 — 1篇 (~48行) 按"调度与时钟 → idleTimeout 淘汰 → fillPool+时钟检测"展开; H-3/H-4/H-5 复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 定时调度 (HouseKeeper 30s 周期) | 🔴 | **为什么🔴**: 后台维护怎么触发 |
| P1-2 | idleTimeout 淘汰 (reserve + close 超时空闲) | 🔴 | **为什么🔴**: 空闲连接回收 |
| P1-3 | fillPool 补 minIdle | 🔴 | **为什么🔴**: 维持最小连接 |
| P2-1 | 时钟回拨/线程饥饿检测 (softEvict) | 🟡 | **为什么🟡**: 时钟异常处理 |
| P2-2 | ClockSource 时钟抽象 | 🟡 | **为什么🟡**: 纳秒/毫秒统一 |
| P3-1 | 与 H-3/H-4/H-5 边界 | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **调度与时钟** | 🔴 | 触发 |
| B | **淘汰与补货** | 🔴 | 维护动作 |
| C | **时钟异常与边界** | 🟡 | 异常处理 |

> **Cluster A (§1)**: HouseKeeper 调度 + ClockSource
> **Cluster B (§2)**: idleTimeout 淘汰(reserve+close) + fillPool 补 minIdle
> **Cluster C (§3)**: 时钟回拨 softEvict + 边界

→ 引出 H-7: 连接验证 — 维护之后: connectionTestQuery/validationTimeout/isValid 的活连接校验(前置 H-5)
