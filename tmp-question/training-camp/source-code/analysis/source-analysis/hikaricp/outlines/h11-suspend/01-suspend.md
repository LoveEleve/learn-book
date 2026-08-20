# H-11 SuspendResumeLock — 池暂停/恢复的挂起锁

> 依赖 H-1 池核心 + H-10 JMX (复用) | 🟡 Working | 6 KP | [模式: 信号量 + 空对象 + 开关]

**读者处境**: 运维想暂停连接池(如发版/维护)让请求排队 — 池怎么"挂起"?getConnection 会怎样?为什么默认没这功能?

### 1. 锁机制 — Semaphore 挂起锁

场景: 一个能让"获取连接"全部阻塞的锁怎么实现?

源码路径:
- `SuspendResumeLock.java:31` — **类**: `SuspendResumeLock`(L31)
- `SuspendResumeLock.java:47,48,60` — **信号量**: `MAX_PERMITS = 10000`(L47) + `acquisitionSemaphore = new Semaphore(MAX_PERMITS, true)`(L60, 公平)
- `SuspendResumeLock.java:63,75` — **获取/释放**: `acquire()`(L63)/`release()`(L75)

关键设计: **Why Semaphore 而非锁？** 信号量用"许可数"表达挂起状态 — 正常时 10000 个许可随便拿; 挂起时耗尽全部许可, 获取自然阻塞; 比 ReentrantLock 更贴合"暂停全部获取"的语义(锁只能持有一个)。[模式: 信号量]

数据流: 每次 getConnection → acquire()(L63) → semaphore.tryAcquire() 成功 → 继续借连接; 用完 → release()(L75) 归还许可。

### 2. suspend / resume — 耗尽与恢复许可

场景: 挂起怎么让获取全部阻塞?恢复怎么放行?

源码路径:
- `SuspendResumeLock.java:80,82` — **挂起**: `suspend()`(L80) → `acquisitionSemaphore.acquireUninterruptibly(MAX_PERMITS)`(L82) — **一次性耗尽全部 10000 个许可**
- `SuspendResumeLock.java:85,87` — **恢复**: `resume()`(L85) → `acquisitionSemaphore.release(MAX_PERMITS)`(L87) — 恢复全部许可
- `SuspendResumeLock.java:63,69` — **挂起时获取**: tryAcquire 失败 → 按 `com.zaxxer.hikari.throwIfSuspended` 抛 `SQLTransientException`(L69) 或 `acquireUninterruptibly()`(L72) 阻塞等待

关键设计: **Why 耗尽许可 = 挂起？** 挂起 = 拿走所有许可 → 后续 acquire 全部阻塞(或抛异常); resume = 归还 → 放行 — 用信号量计数天然表达"暂停/恢复", 无需锁状态机。**Why 两种挂起行为？** 默认阻塞等待(请求排队, resume 后继续); 配 throwIfSuspended=true 则立即抛异常(客户端快速失败)。[模式: 许可耗尽 + 双行为]

数据流: suspend()(L80) → drain 10000 许可(L82) → 后续 getConnection 的 acquire(L63) tryAcquire 失败 → 阻塞等待 或 抛 SQLTransientException(L69)。resume()(L85) → release 10000(L87) → 阻塞的获取放行。

### 3. 开关与用途 — FAUX_LOCK + allowPoolSuspension

场景: 为什么默认没挂起功能?和 JMX 什么关系?

源码路径:
- `SuspendResumeLock.java:33,41` — **FAUX_LOCK**: 空实现(acquire/release/suspend/resume 全 no-op, L33-45) — 不启用挂起时用, 零开销
- `HikariPool.java:93` — **开关**: `config.isAllowPoolSuspension() ? new SuspendResumeLock() : SuspendResumeLock.FAUX_LOCK`(L93)
- `HikariPool.java:154,191` — **挂钩**: getConnection 里 `suspendResumeLock.acquire()`(L154)/`release()`(L191)
- 衔接: JMX(H-10) 的 `HikariPoolMXBean.suspendPool()/resumePool()`(H-10 已讲) 调 suspend()/resume()

关键设计: **Why 默认 FAUX？** 挂起功能默认不需要(有信号量开销)— allowPoolSuspension=false 用 FAUX_LOCK 空实现, 零成本; 按需开启。**Why 与 JMX 衔接？** 挂起的操作入口在 JMX(H-10 的 suspendPool/resumePool), 机制在 SuspendResumeLock — 管理面与机制分离。[模式: 空对象 + 开关]

数据流: 配 allowPoolSuspension=true → HikariPool 建真锁(L93) → JMX(H-10) suspendPool → SuspendResumeLock.suspend()(L80) → getConnection acquire 阻塞/抛异常 → resumePool → resume()(L85) 放行。默认 false → FAUX_LOCK, 此时 suspendPool **抛 `IllegalStateException("is not suspendable")`**(HikariPool L390-392), 零开销但不可挂起。

→ 引出 HikariCP 收官: H-1~H-13 全部完成 — 连接池内核(无锁借用/生命周期/维护/代理/监控)全景。
