# RD-8 基础数据结构 — 知识规划 (knowledge-planning)

> 项目: Redisson 4.6.2-SNAPSHOT | 🟡 B / 2 篇 (无 harness) | RBucket/AtomicLong/Semaphore/CountDownLatch/BitSet
> 基线: REDISSON-PLAN RD-8 — 前置: **RD-4 (命令) + RD-1 (订阅) + RD-2 (等待模型)** — 展开 命令封装→统一等待→信号量→门闩→位操作
> 双链: 前置 [[rd4-command]] [[rd1-connection]] [[rd2-rlock]] | 复用 [[r24-string]] [[r11-bitmap]] | 对照 [[rd2-rlock]] (等待同构) | 引出 [[rd0-intro]] (生态全景)

---

## §0.8

- 🟡 B，2篇 — 命令封装模式(**每个方法→commandExecutor+RedisCommands 一一对应; RBucket GET/SET/GETSET/GETEX RedissonBucket:100-147**) → RAtomicLong 原子(**INCRBY/DECR 服务端原子; compareAndSet Lua 读-比-写 RedissonAtomicLong:85-134**) → 统一等待模型(**RLock/RSemaphore/RCountDownLatch 共享"先试→订阅→循环试-等→退订"; SemaphorePubSub 复用 RedissonLockEntry! RedissonSemaphore:66-98**) → RSemaphore(**release decrby+订阅唤醒 min(acquired,msg); 非公平**) → RCountDownLatch 门闩(**open/close 开关语义; ZERO/NEW_COUNT_MESSAGE CountDownLatchPubSub:40-56**) → RBitSet BITFIELD(**任意位宽 1-64bit 有/无符号+原子自增 RedissonBitSet:47-94**) → 过期语义(**extends RedissonExpirable**)
- 设计模式: [模式: 命令薄封装+统一订阅等待+门闩开关+位宽操作]

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| RedissonBucket.java:100-147 | 命令封装 | GET/SET/GETSET/GETEX | High |
| RedissonAtomicLong.java:85-134 | 原子计数器 | INCRBY/DECR/compareAndSet | High |
| RedissonSemaphore.java:66-98 | 统一等待 | 订阅+循环试-等 | High |
| SemaphorePubSub.java:27-42 | 信号量唤醒 | reuse RedissonLockEntry | High |
| CountDownLatchPubSub.java:40-56 | 门闩 | open/close 开关 | High |
| RedissonBitSet.java:47-94 | 位操作 | BITFIELD 任意位宽 | High |

---

## 02-04 聚合+分类+聚类 (2篇)

**2篇理由**: 6 闭环 → 篇1 命令封装 (q2/q3/q6), 篇2 统一等待 (q1/q4/q5)。无 harness (🟡 B)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 统一等待模型 | 🔴 | **为什么🔴**: 三结构共享 |
| P1-2 | 命令封装 | 🟡 | 基础面 |
| P1-3 | 信号量/门闩 | 🟡 | 同步面 |
| P1-4 | BITFIELD | 🟡 | 位操作 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **命令封装** (q2/q3/q6) | 🟡 | 基础 |
| B | **统一等待** (q1/q4/q5) | 🔴 | 核心 |

---

## 05 闭环结论摘要 (Pass 2 内化)

| # | 机制 | 结论 (一句话) | 源码位置 |
|:--:|---|---|---|
| q1 | 统一等待 | RLock/Semaphore/Latch 同构"先试→订阅→循环试-等"; PubSub 复用 LockEntry | RedissonSemaphore:66-98 |
| q2 | RBucket | GET/SET/GETSET/GETEX 命令封装 | RedissonBucket:100-147 |
| q3 | AtomicLong | INCRBY 服务端原子; CAS 用 Lua | RedissonAtomicLong:85-134 |
| q4 | RSemaphore | release decrby+订阅唤醒; 非公平 | RedissonSemaphore:267-269 |
| q5 | 门闩 | open/close 开关; 归零放行 | CountDownLatchPubSub:40-56 |
| q6 | BitSet | BITFIELD 任意位宽超集 | RedissonBitSet:47-94 |

→ 引出: Redisson 全景闭环 — 数据结构覆盖 Redis 全部类型 → [[rd0-intro]]