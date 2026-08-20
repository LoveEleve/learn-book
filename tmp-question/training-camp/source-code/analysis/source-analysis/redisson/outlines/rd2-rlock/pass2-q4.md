# 闭环笔记 q4: Watchdog 开关 — 心跳的启动/持续/停止闭环

## 假设
Watchdog 一个进程只跑一个心跳任务 (LockRenewalScheduler CAS 单例), 加锁时事触发 → add → tryRun + schedule (10s); 锁释放完 → cancelExpirationRenewal → stop。AtomicBoolean running 防重入。

## 验证过程
- 启动: RedissonBaseLock.scheduleExpirationRenewal (RedissonBaseLock.java:72) → renewalScheduler.renewLock → LockRenewalScheduler (renewal/LockRenewalScheduler:50-53): `reference.compareAndSet(null, new LockTask(...))` → task.add
- add (RenewalTask:136-152): name2entry.compute → 新 entry → `if (tryRun()) schedule()`
- schedule (RenewalTask:62-70): `newTimeout(this, internalLockLeaseTime/3) = 10s` (30s/3)
- run (RenewalTask:169-185): isShuttingDown 检查 → execute() 批量续期 → whenComplete → **schedule() 续排** (成功 L183 / 失败 L179 都续 — 心跳不断)
- 停止: cancelExpirationRenewal (RenewalTask:97-134) → name 移除 → `if (name2entry.isEmpty()) stop()` (L128); stop 置 running=false (RenewalTask:58-60)
- **并发保护**: tryRun = running.CAS (L40-45); add 时 tryRun 失败说明已在跑, 直接返回 (不重复 schedule)
- 续期周期修正: internalLockLeaseTime/3 是固定, 与锁剩余时间无关 — **固定频率心跳** (10s)

## 代码类型
Implementation (调度状态机) — running 标志 + CAS 单例 + 固定频心跳

## 跨域关联
- Q3 (批量续期) → run 每轮回调 execute
- RD-1 (ServiceManager.newTimeout) → 心跳宿主
- 面试点: "Watchdog 多久续一次?为什么?锁没了心跳停不停?"

## 结论
Watchdog = 进程级单心跳: CAS 创建 LockTask → add 触发 tryRun/schedule (10s 固定周期) → run→execute→schedule 续排 → 最后锁释放→cancel→stop。失败也续排 (不让锁意外过期), AtomicBoolean 防多路心跳。这是"客户端续命"的完整闭环。
源码位置: RenewalTask.java:40-70,97-185; LockRenewalScheduler.java:50-53; RedissonBaseLock.java:72