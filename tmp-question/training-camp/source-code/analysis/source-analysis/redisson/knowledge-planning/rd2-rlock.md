# RD-2 RLock+Watchdog — 知识规划 (knowledge-planning)

> 项目: Redisson 4.6.2-SNAPSHOT | 🔴 A / 3 篇 (+harness) | RedissonLock(600)+RedissonBaseLock(290)+renewal/(8)+LockPubSub+锁族 14 类
> 基线: REDISSON-PLAN RD-2 — 前置: **RD-4 (EVALSHA 执行) + RD-1 (订阅/续期调度)** — 展开 tryLock 四步→Watchdog→批量续期→订阅唤醒→锁族矩阵
> 双链: 前置 [[rd4-command]] [[rd1-connection]] | 复用 [[r28-networking]] (pubsub) | 对照 [[r26-list]] (BLPOP 阻塞) [[s75-boot-redis]] | 引出 [[rd5-rmap]] [[rd6-localcachedmap]] [[rd7-spring]]

---

## §0.8

- 🔴 A，3篇 — tryLock 四步(**首试→订阅→循环"试-等-试"→成功/超时; 时间会计扣减保证 ≤waitTime RedissonLock:227-300**) → 加锁 Lua(**exists/hexists→hincrby+1+pexpire; else pttl 可重入哈希锁 L214-224**) → 解锁 Lua(**hexists owner→hincrby-1→counter>0 续期 } {@ 删+发布 UNLOCK L348-360; forceUnlock 无条件 del+publish L336-346**) → Watchdog 开关(**CAS 单例 LockTask→tryRun/schedule→lease/3=10s 固定心跳→run→execute→续排→最后锁释放→stop; AtomicBoolean Running 防重入 RenewalTask:40-185**) → 批量续期(**AsyncChunkProcessor chunk=100 + 单 Lua 多键 hexists→pexpire + ContainsDecoder 过滤失效→cancel L37-104**) → LockEntry 记账(**锁多线程持有者表; getFirstThreadId 代表者续期; hasNoThreads→停**) → 订阅唤醒(**Semaphore(0) + channel 消息; UNLOCK→release(1); READ_UNLOCK→release(queueLength) LockPubSub:41-52**) → LockRenewalScheduler 三原子引用(**: LockTask/ReadLockTask/FastMultilockTask CAS 创建**) → 锁族 14 类(**等待 X 重入 X 组合 X 防御; Fair 队列/Spin 退避/Fenced token/Multi allOf/Red 多数/ReadWrite 双 key/Faster group**) → 阻塞对照(**客户端订阅 vs 服务端 BLPOP hold 连接**)
- 设计模式: [模式: 订阅唤醒+固定心跳+批量续期+重入记账+家族变体]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| RedissonLock.java:227-300 | tryLock | 四步+时间会计 | High |
| RedissonLock.java:214-224 | 加锁 Lua | 可重入哈希锁 | High |
| RedissonLock.java:336-360 | 解锁 | owner 校验 vs force | High |
| RenewalTask.java:40-185 | Watchdog | 开关/周期/续排/停止 | High |
| LockTask.java:37-104 | 批量续期 | chunk+Lua+过滤摘除 | High |
| LockEntry.java:29-61 | 记账 | 多线程持有表 | High |
| LockPubSub.java:41-52 | 唤醒 | Semaphore+双消息 | High |
| 锁族 14 类 | 变体 | 语义矩阵 | High |

---

## 02-04 聚合+分类+聚类 (3篇+harness)

**3篇理由**: 8 闭环 → 篇1 tryLock 协议+订阅 (q1/q5/q7/q8), 篇2 Watchdog 续期 (q2/q3/q4), 篇3 锁族矩阵 (q6)。harness 验证可重入 Lua 语义+心跳周期。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | tryLock 四步协议 | 🔴 | **为什么🔴**: 锁核心 |
| P1-2 | Watchdog 心跳 | 🔴 | **为什么🔴**: 续命关键 |
| P1-3 | 批量续期 Lua | 🔴 | **为什么🔴**: 效率引擎 |
| P1-4 | 订阅唤醒 | 🔴 | **为什么🔴**: 等待语义 |
| P2-1 | 锁族矩阵 | 🟡 | 变体 |
| P2-2 | 重入记账 | 🟡 | 支撑 |
| P2-3 | 释放协议 | 🟡 | 安全 |
| P2-4 | 阻塞对照 | 🟡 | 视角 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **加锁与等待** (q1/q5/q7) | 🔴 | 核心 |
| B | **Watchdog 续期** (q2/q3/q4) | 🔴 | 可靠 |
| C | **锁族** (q6/q8) | 🟡 | 变体 |

---

## 05 闭环结论摘要 (Pass 2 内化)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | tryLock | 首试+订阅+循环"试-等-试"; 时间会计保证 ≤waitTime | RedissonLock:227-300 |
| q2 | LockEntry | 锁的多线程持有者账本; hasNoThreads→停心跳 | LockEntry:29-61 |
| q3 | 批量续期 | chunk=100 单 Lua 多键续期 + ContainsDecoder 摘除失效 | LockTask:37-104 |
| q4 | Watchdog | CAS 单例+10s 固定心跳+失败也续排+锁尽则停 | RenewalTask:40-185 |
| q5 | 订阅唤醒 | Semaphore(0)+channel 消息; UNLOCK/READ 双消息 | LockPubSub:41-52 |
| q6 | 锁族 14 | 等待 X 重入 X 组合 X 防御 语义矩阵 | 根包 14 类 |
| q7 | 释放 | owner 校验递减 vs forceUnlock 无条件 del | RedissonLock:336-360 |
| q8 | 阻塞对照 | 客户端订阅 vs 服务端 BLPOP hold 连接 | RD-2 vs r26 |

→ 引出 RD-5: 阻塞队列 (RLMq) 复用锁订阅; RD-6 缓存失效订阅 — [[rd5-rmap]] [[rd6-localcachedmap]]