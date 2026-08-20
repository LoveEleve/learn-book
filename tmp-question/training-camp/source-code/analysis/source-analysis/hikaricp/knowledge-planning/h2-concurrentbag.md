# H-2 ConcurrentBag + PoolEntry + FastList — 无锁并发容器 (池"快"的秘密)

> 项目: HikariCP | 🔴 Deep / 1 篇 | ConcurrentBag.java(414行)+PoolEntry.java(209行)+FastList.java(370行)
> 基线: HIKARICP-PLAN H-2 (第1层池核心, 🔴) — 无锁并发容器; 前置: **H-13 DriverDataSource(连接来源)** — 展开 borrow/requite/PoolEntry 三态/FastList

---

## §0.8

- 🔴 Deep，1篇 — 结构与状态(ConcurrentBag: sharedList=CopyOnWriteArrayList[L65] + threadLocalList 每线程快表[L68] + handoffQueue 交接队列 + IConcurrentBagEntry 四态[NOT_IN_USE/IN_USE/REMOVED/RESERVED, L83-86] + PoolEntry 用 AtomicIntegerFieldUpdater CAS[L61/163]) → borrow 无锁借用(先 thread-local[CAS NOT_IN_USE→IN_USE, L140]→sharedList 扫描[L147]→handoffQueue 轮询[L162]; addBagItem 扩池回调) → requite 归还(先 handoffQueue 给等待者[L192], 否则回 thread-local 亲和[L200])
- 设计模式: [模式: 无锁并发]—CAS 状态切换; [模式: 线程亲和]—thread-local 快表; [模式: 交接]—handoffQueue 直传

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ConcurrentBag.java:61,65,68 | 结构 | **class(L61)**: sharedList=CopyOnWriteArrayList(L65)+threadLocalList(L68)+handoffQueue | High |
| ConcurrentBag.java:83,84,85,86 | 状态 | **IConcurrentBagEntry**: STATE_NOT_IN_USE=0/IN_USE=1/REMOVED=-1/RESERVED=-2(L83-86) | High |
| ConcurrentBag.java:130,140,148,163 | borrow | **borrow(L130)**: thread-local(L140)→sharedList(L148)→handoffQueue(L163); addBagItem 扩池(L152) | High |
| ConcurrentBag.java:189,192,200 | requite | **setState(NOT_IN_USE)(L189)**→handoffQueue 给等待者(L192)→thread-local 亲和(L200) | High |
| PoolEntry.java:61,163 | 条目CAS | **AtomicIntegerFieldUpdater(L61)+compareAndSet(L163)** | High |
| FastList.java:40,115 | 微优化 | **class(L40)+removeLast O(1)(L115)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: ConcurrentBag 是无锁容器的核心, 3 块耦合(结构/borrow/requite) — 1篇 (~52行) 按"结构与状态 → borrow → requite+微优化"展开; 是 H-3/H-4 借还流程的容器内核。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 结构与状态 (shared/threadlocal/handoff + 四态 + PoolEntry CAS) | 🔴 | **为什么🔴**: 无锁容器怎么组织 |
| P1-2 | borrow 无锁借用 (三级查找 + CAS) | 🔴 | **为什么🔴**: 借出为什么快 |
| P1-3 | requite 归还 (handoff + threadLocal 亲和) | 🔴 | **为什么🔴**: 归还怎么最大化复用 |
| P2-1 | FastList (thread-local 微优化) | 🟡 | **为什么🟡**: 微优化在哪 |
| P2-2 | add/remove + addBagItem 扩池回调 | 🟡 | **为什么🟡**: 新增/淘汰 |
| P3-1 | 与 H-3/H-4 边界 (流程编排) | 🟢 | **为什么🟢**: 容器 vs 流程 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **结构状态** | 🔴 | 容器组织 |
| B | **borrow/requite** | 🔴 | 借还核心 |
| C | **微优化与边界** | 🟡 | 增强 |

> **Cluster A (§1)**: ConcurrentBag 结构(shared+threadlocal+handoff) + IConcurrentBagEntry 四态 + PoolEntry(CAS)
> **Cluster B (§2)**: borrow(三级查找无锁) + requite(handoff+亲和)
> **Cluster C (§3)**: FastList + add/remove + addBagItem + H-3/H-4 边界

→ 引出 H-3: 获取流程 — 容器之后: HikariPool.getConnection→borrow→createEntry→addConnectionExecutor 的编排
