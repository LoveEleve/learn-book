# H-2 ConcurrentBag + PoolEntry + FastList — 无锁并发容器 (池"快"的秘密)

> 依赖 H-13 DriverDataSource (连接来源) | 🔴 Deep | 6 KP | [模式: 无锁并发 + 线程亲和 + 交接]

**读者处境**: Hikari 号称"快"——秘密就在 ConcurrentBag: 不用阻塞队列/锁, 而是无锁 CAS + 线程本地表 + 交接队列。它怎么组织?borrow/requite 到底怎么做到无锁又高效?

### 1. 结构与状态 — ConcurrentBag 的组成

场景: 连接到底存在哪些数据结构里?每个连接(条目)怎么标记状态?

源码路径:
- `ConcurrentBag.java:61,65,68` — **三层结构**: `sharedList = CopyOnWriteArrayList`(L65, 全局空闲) + `threadLocalList`(L68, 每线程快表) + `handoffQueue`(交接队列)
- `ConcurrentBag.java:83,84,85,86` — **四态**: `STATE_NOT_IN_USE=0`(L83)/`STATE_IN_USE=1`(L84)/`STATE_REMOVED=-1`(L85)/`STATE_RESERVED=-2`(L86) — 由 IConcurrentBagEntry 条目维护
- `PoolEntry.java:61,163` — **CAS 状态切换**: `AtomicIntegerFieldUpdater`(L61) + `compareAndSet`(L163) — 无锁改状态

关键设计: **Why 三层结构？** thread-local 让线程优先拿"自己上次归还的连接"(亲和, 零竞争); sharedList 是全局空闲池(COW 读多写少); handoffQueue 让等待线程直接拿到刚归还的连接 — 三层覆盖"快/兜底/直传"。**Why CAS 而非锁？** 状态切换用 AtomicIntegerFieldUpdater CAS, 无锁无阻塞 — 这是并发性能核心。[模式: 无锁并发 + 线程亲和]

数据流: 连接条目(PoolEntry)存 sharedList 或 threadLocalList → 状态用 CAS 在四态间切换 → borrow 从三层取, requite 回某层。

### 2. borrow — 无锁借用三级查找

场景: getConnection 要借连接 — 怎么做到"先快后慢"?

源码路径:
- `ConcurrentBag.java:130` — **borrow(timeout)**: 入口
- `ConcurrentBag.java:140` — **①thread-local**: 从线程本地表尾扫描, `compareAndSet(NOT_IN_USE→IN_USE)`(L140) 成功即返回 — 零竞争最快
- `ConcurrentBag.java:148` — **②sharedList**: 遍历全局空闲(L148), CAS 借出(L149); 若抢了别人的(waiting>1)→ `listener.addBagItem(waiting-1)`(L152) 补一个
- `ConcurrentBag.java:163` — **③handoffQueue**: `listener.addBagItem(waiting)`(L158) 请求扩池 + 轮询交接队列拿新归还的连接(L163), 超时返回 null

关键设计: **Why 三级查找？** 优先线程本地(亲和零竞争)→ 全局空闲(兜底)→ 交接队列(直传新归还)+ 请求创建 — 从"最快"到"兜底"逐级, 尽量无锁命中; 只有全空才可能阻塞等待新连接。**Why addBagItem 回调？** 借不到时通知池(IBagStateListener)动态加连接, 池异步创建(H-3)。[模式: 多级查找 + 回调解耦]

数据流: borrow(L130) → thread-local 扫到→CAS→返回(快); 无 → sharedList 扫→CAS→返回 + 可能补建; 仍无 → addBagItem(请求扩池) → handoffQueue 轮询等新归还/新创建 → 超时 null。

### 3. requite + 微优化 — 归还与 FastList

场景: 连接用完归还 — 给谁?怎么最大化复用?thread-local 表用什么数据结构?

源码路径:
- `ConcurrentBag.java:189` — **requite**: `bagEntry.setState(STATE_NOT_IN_USE)`(L189)
- `ConcurrentBag.java:192` — **先给等待者**: 有 waiter 时 spin `handoffQueue.offer`(L192) — 直接交接给等着的线程, 不落回
- `ConcurrentBag.java:200` — **否则回 thread-local**: 线程本地表 < 16 就 add(L200-203) — 亲和复用
- `FastList.java:40,115` — **微优化**: 自定义 List(L40), `removeLast()` O(1)(L115) — thread-local 表**非弱引用时**用它(弱引用时 ArrayList+WeakReference, L119/401-411)

关键设计: **Why 归还优先 handoff 给等待者？** 有线程在等连接时, 归还的连接直接交接给等待线程(handoffQueue), 避免"归还后等线程再来 sharedList 抢"的中间步骤 — 无缝直传。**Why 否则回 thread-local？** 无等待者时回当前线程本地表, 下次同线程 borrow 零竞争命中(亲和)。**Why FastList？** 非弱引用时(系统 classloader)thread-local 表用自定义数组表 removeLast O(1), 比 ArrayList 按索引删省移位 — 高频 borrow/requite 的微观提速; 弱引用则退 ArrayList+WeakReference 防泄漏。[模式: 交接 + 亲和 + 微优化]

数据流: 归还 requite(L189) → 有等待者?→ 是: handoffQueue.offer 直传(L192); 否: 回 thread-local 表(<16, L200)。borrow 时 thread-local 用 FastList.removeLast(L115) O(1) 取。

→ 引出 H-3: 获取流程 — 容器之后: HikariPool.getConnection→borrow→createEntry→addConnectionExecutor 的编排。
