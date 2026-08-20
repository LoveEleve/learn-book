# H-11 SuspendResumeLock — 池暂停/恢复的挂起锁

> 项目: HikariCP | 🟡 Working / 1 篇 | SuspendResumeLock.java(89行)+HikariPool.java(挂钩)+HikariConfig.java(开关)
> 基线: HIKARICP-PLAN H-11 (第3层代理监控, 🟡) — 挂起语义; 前置: **H-1 池核心** — 展开暂停/恢复

---

## §0.8

- 🟡 Working，1篇 — 锁机制(SuspendResumeLock[L31]: Semaphore[MAX_PERMITS=10000, L47/60] + acquire[L63: tryAcquire→失败按 throwIfSuspended 抛异常或阻塞]/release[L75]) → suspend/resume(suspend[L80]: acquireUninterruptibly(MAX_PERMITS) 耗尽全部许可 → 后续 acquire 阻塞; resume[L85]: release(MAX_PERMITS) 恢复) → 开关与用途(HikariPool L93: allowPoolSuspension ? new SuspendResumeLock : FAUX_LOCK[L33 空实现]; getConnection acquire[L154]/release[L191])
- 设计模式: [模式: 信号量]—Semaphore 许可耗尽=挂起; [模式: 空对象]—FAUX_LOCK; [模式: 开关]—allowPoolSuspension

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SuspendResumeLock.java:31,47,60 | 锁 | **class(L31)+MAX_PERMITS=10000(L47)+Semaphore(L60)** | High |
| SuspendResumeLock.java:63,69 | acquire | **tryAcquire(L65)→throwIfSuspended 抛异常(L69)或阻塞** | High |
| SuspendResumeLock.java:80,82,85 | suspend/resume | **suspend(L80): acquireUninterruptibly(MAX_PERMITS)(L82); resume(L85): release(MAX_PERMITS)** | High |
| HikariPool.java:93,154 | 挂钩 | **allowPoolSuspension? 真锁: FAUX(L93); getConnection acquire(L154)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 挂起锁是一条线(锁机制→suspend/resume→开关), 机制较薄 — 1篇 (~44行) 按"锁机制 → suspend/resume → 开关与用途"展开; H-1/H-10 衔接。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | Semaphore 锁机制 (MAX_PERMITS + acquire/release) | 🔴 | **为什么🔴**: 挂起锁怎么实现 |
| P1-2 | suspend/resume (耗尽/释放全部许可) | 🔴 | **为什么🔴**: 挂起/恢复语义 |
| P1-3 | FAUX_LOCK + allowPoolSuspension 开关 | 🔴 | **为什么🔴**: 默认零开销 |
| P2-1 | throwIfSuspended 异常模式 | 🟡 | **为什么🟡**: 挂起时获取行为 |
| P2-2 | 与 getConnection/H-10 衔接 | 🟡 | **为什么🟡**: 挂起用途 |
| P3-1 | 与 H-1/H-10 边界 | 🟢 | **为什么🟢**: 复用 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **锁机制** | 🔴 | 实现 |
| B | **suspend/resume** | 🔴 | 语义 |
| C | **开关与边界** | 🟡 | 用途 |

> **Cluster A (§1)**: Semaphore(MAX_PERMITS) + acquire/release
> **Cluster B (§2)**: suspend(drain)/resume(restore) + throwIfSuspended
> **Cluster C (§3)**: FAUX_LOCK + allowPoolSuspension + getConnection 衔接

→ 引出 HikariCP 收官: H-1~H-13 全部完成 — 连接池内核(无锁借用/生命周期/维护/代理/监控)全景
