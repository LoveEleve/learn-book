# RD-2 篇3 — lock-family: 14 种锁族的语义矩阵

> 前置: [[RD-2-篇2]] (Watchdog 复用) | 复用: [[rd4-command]] (Lua 执行) | 对照: [[s29-tx-chain]] (Spring 锁) [[r26-list]] (阻塞) | 引出: [[rd5-rmap]] (本地缓存锁) + [[rd7-spring]] (Spring 集成)
> 🔴 A | 2 KP | [模式: 家族变体 — 等待X重入X组合X防御]
> Pass 2 闭环: q6(锁族 14) q8(阻塞对照 补充)

**读者处境**: 面试官问"Redisson 有多少种锁?" — 回答"很多"不够。为什么需要 FairLock?SpinLock 和普通锁差在哪?RedLock 和 MultiLock 谁是谁?读写锁怎么用?这篇拆 14 种锁的语义矩阵: 等待策略 (订阅/队列/自旋) × 重入性 × 组合方式 × 防御增强 — 一张矩阵讲透全部变体。

### 概念依赖链
q6(锁族矩阵) ← 篇2 (Watchdog) — 所有锁族共享 Watchdog 与订阅底座, 差异只在"等待/重入/组合"维度。

### 核心悬念
"14 种锁, 一种锁解决一个语义缺口 —— 每个缺口是什么？"

### 叙事顺序
1. 问题引入: 一把"普通锁"够用吗?为什么有 14 种
2. 语义矩阵 (q6) — 等待X重入X组合X防御
3. 等待维度 (q6) — 订阅 (标准) / 队列 (Fair) / 自旋 (Spin)
4. 重入维度 (q6) — Reentrant / NonReentrant
5. 组合维度 (q6) — MultiLock / RedLock / FasterMultiLock / ReadWriteLock
6. 防御维度 (q6) — FencedLock (fencing token)
7. 收束: "锁族 = 一张矩阵, 选型看维度"

### 1. 语义矩阵 — 四个维度 14 类

场景: 怎么给面试官讲清楚 14 种锁?
源码路径:
- **实测 14 类** (根包, 修正 REDISSON-PLAN 的 9): RedissonLock/FairLock/SpinLock/FencedLock/NonReentrantLock/NonReentrantFairLock/ReadWriteLock/ReadLock/WriteLock/MultiLock/RedLock/FasterMultiLock + 基类 RedissonBaseLock
- 矩阵: **等待 (订阅/队列/自旋) × 重入 (可/不可) × 组合 (单/联/多数/读写) × 防御 (fencing)**
- 全部实现 RLock 接口 (标准锁语义) — 换锁不换 API
关键设计 (q6): 家族 = 同一接口多实现, 每维度一个变体组合。[模式: 接口 + 变体矩阵]
数据流: getLock(kind) → 对应实现 → 统一 RLock 语义。

### 2. 等待维度 — 订阅 / 队列 / 自旋

场景: 拿不到锁时, 三种等法?
源码路径:
- **订阅型 (标准)**: RedissonLock — 等 Semaphore + 释放消息唤醒 (篇1) — 省连接, 公平性一般
- **队列型 (Fair)**: `RedissonFairLock extends RedissonLock` (RedissonFairLock.java:42 "guarantees an acquire order by threads") — **先来先得**, 内部 zset 排队 (RedissonFairLock.java:133,223: zrange/zadd timeout+threadId); 代价: 每次获取排队 O(log n)
- **自旋型 (Spin)**: `RedissonSpinLock extends RedissonBaseLock` (RedissonSpinLock.java:43): `tryAcquire → Thread.sleep(backOff) → tryAcquire` (RedissonSpinLock.java:85-96) — **无订阅无信号**, 忙等 + 指数退避; 适用短临界区, 抢不到不占连接
- 选型: 常规订阅; 需要严格顺序 Fair; 临界区极短 Spin
关键设计 (q6): 等待三策略: 订阅 (省连接) / 队列 (有序) / 自旋 (即时)。[模式: 等待策略变体]
数据流: 抢失败 → 订阅等消息 / 排队 / 退避重试。

### 3. 重入维度 — 可重入与不可重入

场景: 重入锁什么时候反而有害?
源码路径:
- `RedissonNonReentrantLock extends RedissonBaseLock` — 计数恒 1, 二次获取失败
- `RedissonNonReentrantFairLock` — 不可重入 + 公平
- 为什么有不可重入: 重入计数 = hash 字段内存; 某些协议 (如 Lua 递归锁) 不可重入更安全; 减少误用 (忘记解锁时重入掩盖问题)
关键设计 (q6): 重入 = 计数维度开关。[模式: 重入可配置]
数据流: 二次获取 → Reentrant 计数+1 / NonReentrant 拒绝。

### 4. 组合维度 — 联锁 / 红锁 / 读写 / 高性能

场景: 多个 Redis 节点, 怎么锁?
源码路径:
- **MultiLock** (RedissonMultiLock.java:40 implements RLock): `CompletableFuture.allOf` 所有子锁成功才成功 (RedissonMultiLock.java:144) — **联锁: 全部获得**
- **RedLock** (RedissonRedLock.java:32 extends MultiLock): 多数实例成功 (failLocks + majority) — **红锁: 防单点脑裂** (Redis 官方 Redlock 算法)
- **ReadWriteLock** (RedissonReadWriteLock → ReadLock/WriteLock): 双 key 双计数器; 写互斥, 读共享; READ_UNLOCK 放行全部读者
- **FasterMultiLock** (RedissonFasterMultiLock.java:74 "only success when all values locked"): 双字段 group=smallest — 高并发联锁优化
关键设计 (q6): 组合 = 单锁 → 多锁 (all/majority) → 读写 (双计) → 高性能联锁 (格式优化)。[模式: 组合锁]
数据流: MultiLock.lock → 全部子锁; RedLock → 多数; RW → 写独占读共享。

### 5. 防御维度 — FencedLock 栅栏

场景: GC 停顿后旧锁怎么防续命?
源码路径:
- `RedissonFencedLock` — **fencing token**: 每次获取锁得一个单调递增令牌 (epoch), 释放/续期都带令牌
- 解决: GC 停顿 → 旧线程继续执行但锁已过期 → 无 fencing 会双写; fencing 让旧令牌被拒绝
- 适用: 需要强一致的外部服务协作 (跨系统)
关键设计 (q6): fencing token = 单调令牌防旧执行残留。[模式: 防御增强]
数据流: 获锁 → 令牌 → 写操作带令牌 → 旧令牌被拒。

### 负面空间 — 锁族刻意不做的事

- **不统一锁接口差异**: 14 类各自语义, 换锁要明确选型
- **不做锁降级**: ReadLock→WriteLock 升级/降级需手动换 (无内部转换)
- **不自动选型**: 无"智能锁"自动挑等待策略
- **不保证 RedLock 严格正确**: RedLock 有理论争议 (Redis 社区), 复杂环境需自权衡
- **不做锁租约协商**: fencing 靠令牌, 无租约续签协议
- **对照 Spring 本地锁** ([[s29-tx-chain]]): Spring 事务锁是 JVM 内 (单进程), 无网络无续期; Redisson 分布式锁跨进程靠 Lua+订阅+Watchdog — 两者解决不同层级 (进程内并发 vs 进程间互斥), 选型按部署拓扑

→ 引出: RMap 怎么用锁做并发控制?本地缓存失效怎么订阅?→ [[rd5-rmap]] [[rd6-localcachedmap]]