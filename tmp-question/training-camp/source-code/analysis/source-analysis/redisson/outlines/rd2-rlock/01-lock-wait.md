# RD-2 篇1 — lock-wait: tryLock 四步协议与订阅唤醒

> 前置: [[rd4-command]] (EVALSHA 执行 + 订阅命令) + [[rd1-connection]] (订阅通道) | 复用: [[r28-networking]] (PubSub 消息) | 对照: [[r26-list]] (BLPOP 服务端阻塞) | 引出: [[RD-2-篇2]] (Watchdog) + [[rd5-rmap]] (阻塞队列复用)
> 🔴 A | 3 KP | [模式: 首试-订阅-循环 + 信号量唤醒 + 时间会计]
> Pass 2 闭环: q1(tryLock 时间会计) q5(订阅唤醒) q7(释放双轨) q8(阻塞对照)

**读者处境**: 你调 `tryLock(5, TimeUnit.SECONDS)` — 这 5 秒内发生了什么?为什么它"精确等到即超时"?锁被释放时等待线程是怎么**立刻**醒来的, 而不是轮询?非持有者能解锁吗?这篇拆分布式锁最核心的等待协议: 首试 + 订阅 + 循环"试-等-试", 以及 Semaphore 唤醒与时间会计的精确配合。

### 概念依赖链
q1(tryLock 四步) ← q5(订阅唤醒) ← q7(释放双轨) — 先讲等待者的完整循环, 再讲谁唤醒它 (PublishSubscribe), 最后讲释放方的两种路径。

### 核心悬念
"tryLock 5 秒为什么准时超时？锁释放的那一瞬, 等待线程是怎么立刻被叫醒的？不敢轮询怕浪费, 靠什么？"

### 叙事顺序
1. 问题引入: 5 秒尝试, 内里是精密的时间预算
2. 四步协议 (q1) — 首试 → 订阅 → while(试-等-试) → 时间会计
3. 加锁 Lua (q1) — 可重入哈希锁 [exists/hexists→hincrby/pexpire]
4. 订阅唤醒 (q5) — Semaphore(0) + redisson_lock__channel + UNLOCK/READ 双消息
5. 释放双轨 (q7) — unlock owner 递减 vs forceUnlock 无条件
6. 收束: "等待=订阅不是轮询" 与 "释放=校验不是乱删"

### 1. tryLock 四步协议 — 精确的时间会计

场景: tryLock(5s) 为什么'刚好'超时?
源码路径:
- `tryLock(waitTime, leaseTime, unit)` (RedissonLock.java:227-300):
  - L228-230: `time=waitTime+...; threadId=当前线程`
  - **首试** (RedissonLock.java:231-235): tryAcquire → null=成功直接 true
  - L237-241: `time -= 已耗时; if(time<=0) false` — 首试时间也计入预算
  - **订阅** (RedissonLock.java:244): subscribe(threadId) + get(time) — 订阅也要成功且占预算
  - L265-270: 订阅后再扣时间
  - **while 循环** (RedissonLock.java:272-299): tryAcquire → 成功 true → 失败扣时 → `latch.tryAcquire(min(ttl, time))` 等信号 → 回循环
- 时间会计 (RedissonLock.java:287-291): 每次循环扣 `now-currentTime` — **总等待精确 ≤ waitTime**
- latch 等待票面 (RedissonLock.java:295-299): `ttl<time ? tryAcquire(ttl) : tryAcquire(time)` — 等到锁消失或预算耗尽
关键设计 (q1): 等待=有界循环, 用时间会计保证不超预算, 每次信号等 min(锁余命, 预算)。[模式: 首试-订阅-循环 + 会计]
数据流: tryLock → 首试 → 订阅 → 循环{抢锁/等信号} → 超时/成功。

### 2. 加锁 Lua — 可重入哈希锁

场景: 为什么锁是 hash 不是简单 key?
源码路径:
- `tryLockInnerAsync` (RedissonLock.java:214-224):
  ```
  if (exists==0 or hexists(KEYS[1], ARGV[2])==1) then   // 锁空 或 本线程持有
      hincrby(KEYS[1], ARGV[2], 1)                       // 计数+1 (重入)
      pexpire(KEYS[1], ARGV[1])                          // 续命 lease
      return nil                                         // 成功
  end
  return pttl(KEYS[1])                                   // 别人持有, 返回剩余 ttl
  ```
- KEYS[1]=锁名; ARGV[2]=`getLockName(threadId)` (uuid:threadId); ARGV[1]=lease
- **hash 结构**: {field=holder, value=重入计数} — 一个锁多持者互不干扰
- 原子性: 整个 Lua 单次执行, 无竞态 (服务端原子)
关键设计 (q1): 可重入 = hash + hincrby 计数; 锁命名 = uuid:threadId (多持者)。[模式: 哈希重入锁]
数据流: tryAcquire → EVALSHA → exists/hexists → 成功!(计数+1) 或 失败(等 ttl)。

### 3. 订阅唤醒 — Semaphore 与频道消息

场景: 等待线程怎么被立刻叫醒?
源码路径:
- 频道: `getChannelName()` (RedissonLock.java:71) = `prefixName("redisson_lock__channel", name)`
- `subscribe(threadId)` (L244) → RedissonLockEntry: **latch = new Semaphore(0)** (RedissonLockEntry.java:31,37)
- LockPubSub (pubsub/LockPubSub.java:41-52):
  - UNLOCK_MESSAGE=0 → **tryRunListener + getLatch().release()** (唤醒等待者1)
  - READ_UNLOCK_MESSAGE=1 → **tryRunAllListeners + release(queueLength)** (读锁: 全放行)
- onMessage 由发布订阅连接推送 (RD-1 订阅体系)
- 等待侧循环: `latch.tryAcquire(ttl/time)` → 被 release 唤醒 → 抢锁
- 相比轮询: 释放即时唤醒, 零轮询请求; 掉线靠 latch 超时兜底
关键设计 (q5): 等待 = Semaphore(0) 阻塞 + PubSub 消息唤醒; 双消息 (UNLOCK 单个/READ 全部)。[模式: 信号量 + 发布订阅]
数据流: 等锁 → latch.acquire 挂起 → 释放者 PUBLISH → onMessage → latch.release → 醒来再抢。

### 4. 释放双轨 — owner 校验与强制回收

场景: 解锁必须是自己吗?死锁怎么回收?
源码路径:
- `unlockInnerAsync` (RedissonLock.java:348-360):
  - `hexists(KEYS[1], ARGV[3]) == 0 → nil` (非持有者, 拒绝)
  - `hincrby -1` → counter>0 → pexpire 续期 (重入未完, 锁不删)
  - counter==0 → 删锁 + 发布 UNLOCK
- `forceUnlockAsync` (RedissonLock.java:336-346): `del KEYS[1] → publish UNLOCK → 1/0` — **无条件删 + 通知等待者**
- 用途: unlock (正常释放) / forceUnlock (超时清理/运维干预/死锁恢复)
关键设计 (q7): 双轨释放: 持有者有序递减 (安全), 任何人无条件删 (可运维)。[模式: 校验释放 + 强制回收]
数据流: unlock → owner 校验 → 递减 → 归零删+publish; forceUnlock → del+publish。

### 5. 阻塞对照 — 客户端订阅 vs 服务端 BLPOP

场景: 和 Redis 阻塞队列 (BLPOP) 差在哪?
源码路径 (概念对照, r26 详述):
- **Redisson 锁**: 阻塞在**客户端** (Semaphore), 唤醒靠 PubSub; 订阅连接复用, 不占命令连接
- **BLPOP** (r26-list): 阻塞在**服务端** (连接 hold), 唤醒靠数据就绪 push; 占一条连接
- 差异: 挂载点/唤醒源/连接占用/掉线兜底 (锁=超时, BLPOP=重发)
关键设计 (q8): 两种分布式阻塞哲学 — 订阅式 (客户端等待, 连接省) vs 连接式 (服务端等待, 简单)。[模式: 客户端订阅 vs 服务端 hold]
数据流: 锁等待 = 订阅连接等消息; BLPOP = 一条连接干等数据。

### 负面空间 — 加锁与等待刻意不做的事

- **不轮询**: 不用定时抢锁, 用订阅即时唤醒
- **不无限等**: tryLock 有严格时间预算; lock() 才是无限
- **不允非持者解锁**: owner 校验; 强解只有 forceUnlock
- **不超时自动重试**: 抢锁失败按调用方语义返回 false (不自动转 lock)
- **不保证唤醒顺序**: UNLOCK 唤醒 1 个但非严格 FIFO (公平锁在 FairLock)

→ 引出: 抢到锁后谁给它续命?锁没了心跳怎么停?→ [[RD-2-篇2]]