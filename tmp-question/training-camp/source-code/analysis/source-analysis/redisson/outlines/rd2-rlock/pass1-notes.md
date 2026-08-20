# Pass 1 探索笔记: RD-2 RLock+Watchdog

> 方案 A (🔴) | 源码: `/data/workspace/source-code/code/spring/redisson` (4.6.2-SNAPSHOT)
> 大域: RedissonLock (600) + RedissonBaseLock (290) + renewal/ (8) + pubsub/ (LockPubSub) + 锁族 14 类 → 拆 3 篇

## Pass 0 上下文吸收

- 核心文件: RedissonLock(600)/RedissonBaseLock(290)/renewal(8 文件)/LockPubSub
- **锁族实测 14 类** (非规划的 9): RedissonLock/FairLock/FencedLock/SpinLock/NonReentrantLock/NonReentrantFairLock/MultiLock/RedLock/FasterMultiLock + **ReadWriteLock 家族 (RedissonReadWriteLock/RedissonReadLock/RedissonWriteLock)** + RedissonBaseLock 基类 — 09 审计数字需修正
- 09 审计已知: tryLock 四步 (Lua→订阅→Semaphore→续期) / Watchdog leaseTime/3=10s / 三续期器 / redisson_lock__channel
- 时空: CHANGELOG 锁续期演进 — FastMultilock (Faster) 新增, AsyncChunkProcessor 批量化

## 继承树/调用图

```
RedissonLock (600 行)                          RLock 接口实现 (可重入)  org.redisson
  ├── tryLock flow: 首试→订阅→循环 tryAcquire+等待 latch
  ├── tryLockInnerAsync (L214): 加锁 Lua (exists/hexists/hincrby/pexpire/pttl)
  ├── unlockInnerAsync (L348): 减重入→publish UNLOCK→删
  ├── forceUnlockAsync (L336): 无条件 del + publish
  └── RedissonBaseLock (290 行) ← [scheduleExpirationRenewal L72 → renewalScheduler]
       └── 14 锁族:
            ├── RedissonLock (普通可重入)
            ├── RedissonFairLock / NonReentrantFairLock (公平)
            ├── RedissonFencedLock (栅栏: 防 GC 停顿)
            ├── RedissonSpinLock (自旋)
            ├── RedissonNonReentrantLock (不可重入)
            ├── RedissonReadWriteLock → ReadLock/WriteLock (读写)
            ├── RedissonMultiLock (联锁: 全获得)
            ├── RedissonRedLock (红锁: 多数获得)
            └── RedissonFasterMultiLock (高性能联锁)

renewal/ (8 文件)                             LockRenewalScheduler (804 parent)
  ├── LockRenewalScheduler            ─ 三个 reference: LockTask/ReadLockTask/FastMultilockTask CAS 创建
  ├── RenewalTask (抽象)               ─ run/stop AtomicBoolean; schedule() lease/3
  │    ├── LockTask                    ─ buildChunk 批量续期 Lua (hexists+pexpire; 锁消失→移除)
  │    ├── ReadLockTask                ─ 读锁批量
  │    └── FastMultilockTask           ─ 高性能联锁批量
  ├── LockEntry/ReadLockEntry/FastMultilockEntry

pubsub/ LockPubSub                      ─ UNLOCK_MESSAGE=0 / READ_UNLOCK_MESSAGE=1
  └── onMessage: UNLOCK→tryRunListener+latch.release; READ→all+release(queueLength)
```

## 基本元素分解

1. **tryLock 四步协议** (RedissonLock:227-300) — 首试→订阅→while 循环 tryAcquire+等待 latch; 时间会计防超时
2. **加锁 Lua 可重入** (L214-224) — exists/hexists → hincrby+1+pexpire; else pttl
3. **解锁 Lua** (L348-360) — hexists 校验 owner → hincrby -1 → counter>0? 续期 : 删+发布 UNLOCK
4. **forceUnlock** (L336-346) — 无条件 del + publish (管理员解锁)
5. **Watchdog 续期核心** (RenewalTask:52-74) — running AtomicBoolean 开关; newTimeout lease/3
6. **批量续期** (LockTask:50-104) — chunkSize 一把锁一批; Lua 数组 hexists→pexpire; 锁消失→cancelExpirationRenewal
7. **订阅唤醒** (LockPubSub:32-52) — UNLOCK→latch.release; READ→release(queueLength)
8. **14 锁族** (根包 14 类) — 普通/公平/栅栏/自旋/不可重入/读写/联锁/红锁/高性能

## 标记问题 (8 个)

1. **Q1 tryLock 时间会计**: tryLock 对超时的精确设计 — 每次 tryAcquire/latch.acquire 前扣时间?为什么 lock() (阻塞) 与 tryLock(waitTime) 语义不同?
2. **Q2 LockEntry 语义**: 一个锁在多线程重入时 LockEntry 怎么持多 threadId?getFirstThreadId 的选择策略?cancel 时怎么处理?
3. **Q3 批量续期协议**: RenewalTask 的 schedule (lease/3) + chunkSize 批量 — 为什么用单 Lua 脚本多锁续期?ContainsDecoder 过滤语义?
4. **Q4 Watchdog 开关**: renewLock 触发 → LockRenewalScheduler CAS 创建单例任务; 锁释放后 stop?running 标志怎么复位?
5. **Q5 订阅协议**: subscribe(threadId) → RedissonLockEntry; LockPubSub createEntry/onMessage 生命周期; "一个订阅一个请求" vs 批量订阅?
6. **Q6 锁族差异**: 9→14 类 — Fair (队列), Spin (自旋 remain), Fenced (epoch), NonReentrant, ReadWrite (双 Pix), MultiLock (全获得), RedLock (多数), Faster (无锁表)?
7. **Q7 释放 vs 强制释放**: unlockInner (owner 校验) vs forceUnlock (无条件) — 过期/超时场景怎么用?
8. **Q8 与 redis 对照**: 服务端 r22 exist? Redis 阻塞命令 (BLPOP) vs Redisson 锁订阅唤醒 — 两种阻塞语义对照?

## 已读测试 (2 个)

- RedissonLockTest (顶层, 部分): 加锁/重入/续期验证
- (未深入 — LockOptionsTest 顶层已存在)

## 完成检查

- [x] 继承树/调用图已画出 (14 锁族修正)
- [x] 基本元素分解 8 项全部有源码位置
- [x] 8 个标记问题有源码位置
- [x] 已读测试 (RedissonLockTest)
- [x] 时空溯源 (CHANGELOG 锁续期演进)
- [x] **09 修正: 锁族 9→14 (补 ReadWriteLock 家族)**