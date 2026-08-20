# RD-2 篇2 — watchdog: 续期心跳与批量续期协议

> 前置: [[RD-2-篇1]] (获取成功触发续期) | 复用: [[rd1-connection]] (ServiceManager.newTimeout) | 对照: [[r22-expire]] (服务端惰性过期) | 引出: [[RD-2-篇3]] (锁族) + [[rd4-command]] (批量 Lua)
> 🔴 A | 3 KP | [模式: 单例心跳 + 固定周期 + 批量续期中 + 自动摘除]
> Pass 2 闭环: q2(LockEntry 记账) q3(批量续期 Lua) q4(Watchdog 开关)

**读者处境**: 你拿了一把锁设了 30s 过期 — 但业务跑了 2 分钟为什么不超时?谁在 30s 内"续命"?它一次续一把还是成百上千把?服务器重启脚本丢了怎么办?这篇拆 Watchdog 的完整生命周期: 获锁触发心跳 → 每 10s 批量续期 → 锁尽自动停止。以及它如何用"一个 Lua 脚本续 100 把锁"做到高效率。

### 概念依赖链
q4(Watchdog 开关) ← q3(批量续期) ← q2(LockEntry 记账) — 先讲心跳什么时候启停, 再讲每次心跳干什么, 最后讲"谁会出现在心跳里"。

### 核心悬念
"30 秒过期的锁, 业务跑 2 分钟却不超时 —— Watchdog 是谁、多久一次、一次管几把锁？"

### 叙事顺序
1. 问题引入: 默认 30s 过期, 长时间业务怎么不丢锁
2. 心跳开关 (q4) — 获锁触发 → add→tryRun/schedule → 锁尽 stop
3. 固定周期 (q4) — lease/3=10s, 失败也续排
4. 批量续期 (q3) — chunk=100 单 Lua 多键 hexists→pexpire
5. 失效摘除 (q3) — ContainsDecoder 过滤 result=0 → cancel
6. LockEntry 记账 (q2) — 多线程持有者, 代表者续期
7. 收束: "一个心跳进程, 100 锁一脚本, 锁尽自动停"

### 1. 心跳开关 — CAS 单例 + 启停边界

场景: 谁的锁带心跳?心跳几个?
源码路径:
- 启动: 获锁成功 + leaseTime<=0 → `scheduleExpirationRenewal` (RedissonBaseLock.java:72) → renewalScheduler.renewLock → LockRenewalScheduler (renewal/LockRenewalScheduler.java:50-53): **reference.compareAndSet(null, new LockTask)** — 进程级单任务
- add (RenewalTask.java:136-152): name2entry.compute → 新锁 → `if (tryRun()) schedule()` — 首次启动心跳
- **停止**: 锁释放 → cancelExpirationRenewal (RenewalTask.java:97-134): 移除 name → 空 → `stop()` (L128)
- running AtomicBoolean (RenewalTask.java:40-45): tryRun=CAS 防重入 — 多个锁只一个心跳
- 为什么 one-per-process: 批量续期后心跳对 1000 锁也只需一个 TimerTask
关键设计 (q4): 单例心跳: CAS 建任务 + AtomicBoolean 防重入 + 最后锁走 stop。[模式: 有界单例任务]
数据流: 获锁 → renewLock → LockTask.add → tryRun → schedule(10s)。

### 2. 固定周期 — 10 秒一跳

场景: 心跳多久?怎么续排?
源码路径:
- `schedule()` (RenewalTask.java:62-70): `newTimeout(this, internalLockLeaseTime/3)` — L68 **lease/3 = 30s/3 = 10s**
- `run(Timeout)` (RenewalTask.java:169-185): isShuttingDown 检查 → execute() 批量续期 → whenComplete → **schedule() 再排** — 成功续 (L183), 失败也续 (L179) **心跳不断**
- 为什么 /3: 足够频繁 (<超时/2) 防网卡抖动丢心跳; 又不至于太频繁
- internalLockLeaseTime = lockWatchdogTimeout (30s 默认)
关键设计 (q4): 固定频心跳 lease/3; 完成后再排 (串行不并发); 失败也续 (不让锁意外过期)。[模式: 自续排定时]
数据流: schedule → 10s → run → execute → 续排 → ...

### 3. 批量续期 — 一把脚本续 100 锁

场景: 心跳怎么不把网络打爆?
源码路径:
- RenewalTask.execute (RenewalTask.java:78-88): 非 cluster → renew(全部); cluster → renewSlots(按槽位)
- LockTask.renew (LockTask.java:37-43): `AsyncChunkProcessor.processAll(iter, chunkSize=100, buildChunk)` — 分片
- buildChunk (LockTask.java:50-83): 收集 ≤100 有效锁 (跳过空 entry) → keys + args
- **批量续期 Lua** (LockTask.java:84-96):
  ```
  for i=1,#KEYS: hexists(KEYS[i], ARGV[i+1])? → pexpire; result=1
              否则 → result=0
  ```
- ARGV[1]=lease; ARGV[i+1]=holder 字段
- **1 RTT 续 100 锁** — 无批量时 100 RTT
关键设计 (q3): 分片批量: chunk=100 锁一个 Lua 续期, Cluster 按槽位分组避免跨槽位脚本错。[模式: 分片批量脚本]
数据流: 心跳 → 非 cluster? 全量 : 按槽 → chunk 分片 → Lua 批量续。

### 4. 失效摘除 — 锁没了自动退出心跳

场景: 锁被 unlock 后, 心跳还会一直续吗?
源码路径:
- ContainsDecoder 消费 result (LockTask.java:101-104): **result=0 (锁不存在/非持有) → keys.removeAll(existingNames) → cancelExpirationRenewal** — 已释放锁自动摘除
- cancelExpirationRenewal (RenewalTask.java:97-121): 移除 threadId → hasNoThreads? → 从 slot2names 移 → name2entry 返回 null
- LockEntry 记账 (LockEntry.java:29-61): addThreadId/removeThreadId/getFirstThreadId — 多线程持有者表
- 为什么多持者: 同一把锁 JVM 内多线程各自持 (同 name), 全部退才算释放
关键设计 (q3/q2): 续期自带"检测 + 摘除": 锁消失这轮就退出, 心跳列表自动瘦身; LockEntry 账本决定退出时机。[模式: 续期即检测]
数据流: 续期 result → 0 号锁 → cancel → 该锁移出心跳 → 全空 → stop。

### 5. 三续期器 — 普通 / 读锁 / 多锁组

场景: 只有一把普通锁需要续期吗?
源码路径:
- LockRenewalScheduler 三个 CAS 引用 (renewal/LockRenewalScheduler.java:30-33): `reference` (LockTask) / `readLockReference` (ReadLockTask) / `multilockReference` (FastMultilockTask) — **每类一个单例心跳**
- `ReadLockTask extends LockTask` (ReadLockTask.java:33): renew 时用 `keyPrefix` (L66-73) — **读锁的 key 带前缀** (`lock:xxx` 下的 readlock), 续期 Lua 按前缀定位; `add(name, lockName, threadId, keyPrefix)` (L134)
- `FastMultilockTask extends LockTask` (FastMultilockTask.java:32): renew 用 `EVAL_BOOLEAN` (L78, 不同脚本 — 布尔续期) ; `add(..., Collection<String> fields)` (L113) — **多锁组 (group) 的多个字段一起续**
- 分派: RedissonReadLock → renewReadLock; FasterMultiLock → renewFastMultiLock; 普通 → renewLock
关键设计 (q4 补充): 三续期器 = 心跳按锁类型分型: 普通 (单字段)/读锁 (前缀)/多锁组 (fields 集) — 各自 Lua 适配。[模式: 分型续期器]
数据流: 读锁获锁 → renewReadLock → ReadLockTask → 前缀续期; Faster → FastMultilockTask → 字段集续期。

### 负面空间 — Watchdog 刻意不做的事

- **不每锁一请求**: 批量 (100/脚本) — 为性能牺牲单锁粒度
- **不精确续期**: 固定 lease/3 心跳, 不做"还剩Xms续一下"的精确计算
- **不感知业务超时**: 心跳只问"锁还在不在", 不管业务是否该结束 (调用方负责)
- **不跨进程协调**: Watchdog 是进程内单例, 多实例各自心跳
- **不做锁人工续期 API**: 续期只能靠默认 Watchdog, 无显式 renew API (减少误用)
- **不暴露心跳指标**: 无专有心跳计数器/日志, 运维靠 Redisson 日志 + APM 采集 (completeness Q31)
- **30s 默认理由**: 常见业务长任务 (秒级~分钟级) 默认 30s 够用, 配合 Watchdog 续命而非拉长超时 (避免崩溃后锁被长期占用, completeness Q36)
- **对照服务端过期** ([[r22-expire]]): 服务端 Redis 惰性过期 (expireIfNeeded "读到才删") 是锁 TTL 的最终兜底 — 客户端 Watchdog 续期失效时, 锁靠服务端 PEXPIRE 过期自动释放 (双保险: 客户端续命 + 服务端兜底)

→ 引出: 14 种锁族怎么在同一 Watchdog 下共存?→ [[RD-2-篇3]]