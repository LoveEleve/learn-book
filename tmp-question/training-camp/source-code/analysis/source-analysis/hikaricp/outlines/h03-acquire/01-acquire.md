# H-3 获取流程 — HikariPool.getConnection → borrow → createProxyConnection

> 依赖 H-2 ConcurrentBag + H-5 生命周期 + H-13 (复用) | 🔴 Deep | 6 KP | [模式: 门面编排 + 异步补货 + 边界回退]

**读者处境**: 应用调 `dataSource.getConnection()` 拿到的到底是什么?连接被借出前经过哪些校验?池里没空闲连接时怎么异步补货?

### 1. getConnection 主流程 — 借出编排

场景: getConnection 拿到一个可用连接 — 中间经历什么?

源码路径:
- `HikariPool.java:152` — **入口**: `getConnection(hardTimeout)`(L152)
- `HikariPool.java:154` — **挂起锁**: `suspendResumeLock.acquire()`(L154, H-11) — 池暂停时阻塞
- `HikariPool.java:160` — **borrow**: `connectionBag.borrow(timeout, MILLISECONDS)`(L160, H-2) — 无锁借用
- `HikariPool.java:166` — **校验**: `isMarkedEvicted() || (距上次访问 > aliveBypassWindowMs[默认500ms] && isConnectionDead)`(L166) → 是则 `closeConnection`(L167) 重试(H-5/H-7) — **alive bypass 优化**: 最近用过的连接跳过昂贵的死检
- `HikariPool.java:179` — **返回代理**: `poolEntry.createProxyConnection(leakTaskFactory.schedule(poolEntry))`(L179) — 包成代理连接(H-12) + 排泄漏任务(H-8)
- `HikariPool.java:184,191` — **边界**: 超时 `createTimeoutException`(L184) / finally `release`(L191)

关键设计: **Why 循环重试？** borrow 可能拿到"已淘汰/已死"的连接 — 关闭后重借, 直到超时; 保证返回的都是可用连接。**Why createProxyConnection？** 用户拿到的是代理(带状态追踪/close 拦截), 底层连接归池管理 — 归还/泄漏都在代理层(H-12/H-4)。[模式: 门面编排 + 循环重试]

数据流: getConnection(L152) → acquire(L154) → loop: borrow(L160) → 若 evicted/dead(L166)→close 重试; 否则 recordBorrowStats(L171) + beginRequest + createProxyConnection(leakTask)(L179) → 返回代理连接。全空→超时→createTimeoutException(L184)。

### 2. 动态扩池 — addBagItem → poolEntryCreator

场景: 池里没空闲连接且等待者多 — 谁来补货?怎么补?

源码路径:
- `HikariPool.java:341` — **回调**: `addBagItem(waiting)`(L341) — 由 borrow 借不到时触发(H-2 的回调)
- `HikariPool.java:343,344` — **异步提交**: `if (waiting > addConnectionExecutor.getQueue().size())`(L343) → `addConnectionExecutor.submit(poolEntryCreator)`(L344) — 只有"请求数 > 排队数"才提交, 避免过量
- `poolEntryCreator`(L70) — PoolEntryCreator Runnable: 执行 createPoolEntry 建新连接并 add 入 bag

关键设计: **Why 异步而非同步建连？** 建新连接是慢操作(网络/驱动)— 若在 borrow 线程同步做会阻塞所有等待者; 交给 addConnectionExecutor 异步建, 借出线程只等 handoffQueue(H-2) 拿结果。**Why 限流？** `waiting > 队列大小` 防止重复提交过多建连任务(已有在建就跳过)。[模式: 异步补货 + 限流]

数据流: borrow 借不到 → listener.addBagItem(waiting)(H-2) → L343 判断 → submit(poolEntryCreator)(L344) → 异步 createPoolEntry → add 入 bag → 借出线程从 handoffQueue 拿到。

### 3. createPoolEntry + 线程池边界

场景: 新连接条目怎么创建?异步补货的并发边界?

源码路径:
- `HikariPool.java:485` — **createPoolEntry**: 调 `newPoolEntry(...)`(L489, PoolBase H-5) → `new PoolEntry(newConnection(isEmptyPool), ...)`(PoolBase L210) — 建新连接条目; newConnection 在 H-5
- `HikariPool.java:72,113` — **addConnectionExecutor**: `ThreadPoolExecutor`(L72); `createThreadPoolExecutor(maxPoolSize, ...)`(L113) — 异步建连专用线程池
- `HikariPool.java:121,122` — **边界**: core/max = `min(16, availableProcessors)`(L121-122) — 受限避免爆发建连

关键设计: **Why 独立建连线程池？** 建连慢且会触发驱动/网络 — 隔离到专用线程池, 不占业务线程; core/max 取 min(16, CPU核数) 限并发, 防瞬间建连风暴。**Why 边界 H-5？** 连接真正建立(newConnection: 驱动连接+超时+验证)在 H-5 生命周期展开, 本域只讲"获取编排怎么触发建连"。[模式: 独立线程池 + 并发限流]

数据流: poolEntryCreator 执行 → createPoolEntry(L485) → `newPoolEntry()`(PoolBase L208, H-5) → `new PoolEntry(newConnection(isEmptyPool))`(L210) → add 入 bag → 等待线程拿。addConnectionExecutor 限制并发(min(16,CPU), L121-122)。

→ 引出 H-5: 生命周期 — 获取之后: HikariConfig 校验/seal → PoolBase.newConnection 的连接创建(前置 C-11)。
