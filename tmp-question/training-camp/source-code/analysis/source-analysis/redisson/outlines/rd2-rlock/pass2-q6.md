# 闭环笔记 q6: 锁族差异 — 14 类锁的语义矩阵

## 假设
REDISSON-PLAN 锁族 9 类是低估 — 实测根包 14 类, 核心分三线: 等待策略 (队列/自旋/订阅) × 重入性 (可/不可) × 组合 (多锁/多数/读写) × 安全增强 (栅栏)。各改一个维度。

## 验证过程
- **11 大 SPLIT** (redisson root 包, 实测 14 类含基类):
  - `RedissonLock` (600) — 普通可重入, 订阅+Watchdog 标准型
  - `RedissonFairLock extends RedissonLock` (RedissonFairLock.java:42 注释 "guarantees an acquire order by threads") — **公平队列** (先进先出, 红黑树/zadd 排队)
  - `RedissonSpinLock extends RedissonBaseLock` (RedissonSpinLock.java:43) — **自旋**: `tryAcquire → Thread.sleep(backOff) → tryAcquire` (L85-96), **无订阅无信号, 忙等+退避**
  - `RedissonFencedLock` — **栅栏/epoch**: 防 GC 停顿后旧令牌续命 (fencing token)
  - `RedissonNonReentrantLock` / `RedissonNonReentrantFairLock` — 不可重入 (重入计数=1 简化)
  - `RedissonReadWriteLock → RedissonReadLock/RedissonWriteLock` — 读写: 双 key (读锁 key / 写锁 key), READ_UNLOCK 放行全部读者
  - `RedissonMultiLock implements RLock` (MultiLock.java:40) — **联锁**: allOf 全部子锁成功
  - `RedissonRedLock extends RedissonMultiLock` (RedissonRedLock.java:32) — **红锁**: 多数实例成功 (Redisson 实现 = MultiLock + majority?)
  - `RedissonFasterMultiLock extends RedissonBaseLock` (RedissonFasterMultiLock.java:74 注解 "only success when all values locked") — **高性能联锁** (group=smallest 双字段格式)
- 维度分解: 等待 (订阅/队列/自旋) × 重入 (可/不可) × 组合 (单/多/多数/读写) × 防御 (fencing)

## 代码类型
Interface (家族设计) — 同一接口 (RLock) 多实现变体

## 跨域关联
- Q1/Q5 (等待) → 订阅型 vs Q6 自旋型
- RD-0 (导论) → 锁族全景
- Redis 事务/锁面 → 对照

## 结论
14 锁族 = 语义矩阵: 等待机制 (订阅=标准/队列=公平/自旋=忙等) × 重入 (可/不可) × 组合 (单/联/多数/读写) × 防御 (fenced token)。面试必考 "Redisson 有哪些锁, 区别?" — 矩阵即是答案。
源码位置: RedissonLock.java:42, RedissonSpinLock.java:43,85-96, RedissonFairLock.java:42, RedissonRedLock.java:32, RedissonFasterMultiLock.java:74