# H-6 HouseKeeper + ClockSource — 30s 后台维护 (idleTimeout 淘汰 / fillPool 补 minIdle)

> 依赖 H-3/H-4/H-5 (复用) | 🔴 Deep | 6 KP | [模式: 定时任务 + 淘汰策略 + 时钟抽象]

**读者处境**: 池里连接长时间不用会被回收; 池低于 minIdle 会补满 — 谁在背后干这些?多久一次?

### 1. 定时调度与时钟 — HouseKeeper + ClockSource

场景: 后台维护线程怎么启动?多久跑一次?用什么计时?

源码路径:
- `HikariPool.java:793` — **HouseKeeper**: `implements Runnable`(L793) — 后台维护任务
- `HikariPool.java:63,118` — **调度**: `housekeepingPeriodMs`(默认 `SECONDS.toMillis(30)`, L63) + `scheduleWithFixedDelay(new HouseKeeper(), 100L, housekeepingPeriodMs, MILLISECONDS)`(L118) — 100ms 初延 + 每 30s 固定延迟执行
- `ClockSource.java:45,84` — **时钟抽象**: `currentTime()`(L45) 返回不透明时间戳 + `elapsedMillis`(L84) — 统一毫秒/纳秒计时, 避免直接依赖 System.currentTimeMillis

关键设计: **Why scheduleWithFixedDelay？** 固定延迟(而非固定速率)保证上次跑完才开始下次, 避免维护任务堆积; 100ms 初延让池先建立。**Why 30s 默认？** 维护频率权衡: 太频繁耗 CPU, 太慢则空闲回收/补货不及时 — 30s 是平衡点(可配 com.zaxxer.hikari.housekeeping.periodMs)。**Why ClockSource 抽象？** 提供统一毫秒/纳秒计时, 跨平台时间戳一致; 时钟回拨检测由 HouseKeeper 基于它实现(§3)。[模式: 定时任务 + 时钟抽象]

数据流: 池构造 → scheduleWithFixedDelay(HouseKeeper, 100ms, 30s)(L118) → 每 30s HouseKeeper.run 执行维护(刷新配置/淘汰/补货)。

### 2. idleTimeout 淘汰 — 回收超时空闲连接

场景: 空闲连接超过 idleTimeout 怎么被回收?保底多少?

源码路径:
- `HikariPool.java:830` — **条件**: `if (idleTimeout > 0L && config.getMinimumIdle() < config.getMaximumPoolSize())`(L830) — 开了 idleTimeout 且 minIdle<max 才淘汰
- `HikariPool.java:835` — **逐条淘汰**: `elapsedMillis(entry.lastAccessed, now) > idleTimeout && connectionBag.reserve(entry)`(L835) — 空闲超时 + reserve 成功(避免正在用)
- `HikariPool.java:836` — **关闭**: `closeConnection(entry, "(connection has passed idleTimeout)")`(L836) — 真正关闭底层

关键设计: **Why reserve 再关？** reserve 把条目从可用集"预留"(状态 RESERVED), 确保不会被其他线程借走 — 关闭前先安全隔离, 防并发竞态。**Why 只收到 minIdle？** `maxToRemove = notInUse.size() - minIdle`(上限) — 即使全部超时也只回收"超出 minIdle 的部分", 保底 minIdle 个连接。[模式: 淘汰策略 + 安全预留]

数据流: HouseKeeper.run → L830 条件命中 → 取 not-in-use 列表 → 对 idle 超时者: reserve(L835)→closeConnection(L836) → 收到 maxToRemove 上限(保底 minIdle)。

### 3. fillPool 补 minIdle + 时钟回拨检测

场景: 淘汰后怎么补回 minIdle?系统时钟回拨怎么办?

源码路径:
- `HikariPool.java:845` — **补货**: `fillPool(true)`(L845) — 若总连接 < minIdle 则异步补建到 minIdle(H-3 的 poolEntryCreator)
- `HikariPool.java:816,820` — **时钟回拨**: `if (plusMillis(now, 128) < plusMillis(previous, housekeepingPeriodMs))`(L816, NTP 允许 +128ms) → `softEvictConnections()`(L820) — 检测到时钟回拨, 软淘汰所有连接防"时间倒流导致 idleTimeout/maxLifetime 误判"

关键设计: **Why fillPool？** 淘汰后连接数可能低于 minIdle — fillPool(true) 用 H-3 的异步建连补足, 维持最小连接保证可用性。**Why 时钟回拨 softEvict？** 系统时钟回拨会让"基于当前时间的 idleTimeout/maxLifetime"误判(看似超时) — 检测到(NTP 允许 128ms 容差)就软淘汰全部连接重来, 防错误淘汰。[模式: 补货 + 时钟异常防御]

数据流: 淘汰完 → fillPool(true)(L845) → 连接数<minIdle → 异步补建; 若 HouseKeeper 检测到时钟回拨(L816)→ softEvictConnections(L820) → 重来。

→ 引出 H-7: 连接验证 — 维护之后: connectionTestQuery/validationTimeout/isValid 的活连接校验(前置 H-5)。
