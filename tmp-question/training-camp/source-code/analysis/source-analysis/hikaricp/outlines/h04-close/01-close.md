# H-4 归还流程 — ProxyConnection.close → HikariPool.recycle → ConcurrentBag.requite

> 依赖 H-2 ConcurrentBag + H-12 代理 (复用) | 🔴 Deep | 6 KP | [模式: 代理拦截 + 回收分派 + 状态复位]

**读者处境**: 应用调 `connection.close()` 其实没真关底层连接——是"归还池"。归还前做什么(关语句/回滚/复位)?失效连接怎么处理?

### 1. ProxyConnection.close — 代理关闭链

场景: 用户调 close(), 代理层做什么才把连接安全归还?

源码路径:
- `ProxyConnection.java:240` — **close()**: 归还入口
- `ProxyConnection.java:243` — **先关语句**: `closeStatements()`(L243) — 关掉该连接上打开的 Statement(可能触发连接失效)
- `ProxyConnection.java:246` — **取消泄漏任务**: `leakTask.cancel()`(L246, H-8)
- `ProxyConnection.java:250` — **脏事务回滚**: `if (isCommitStateDirty && !isAutoCommit) delegate.rollback()`(L249-251) — 未提交的脏事务回滚, 防数据残留
- `ProxyConnection.java:255,258` — **状态复位**: `dirtyBits != 0 → poolEntry.resetConnectionState(this, dirtyBits)`(L254-256) + `delegate.clearWarnings()`(L258)

关键设计: **Why 先 closeStatements？** 关闭语句可能抛异常触发连接失效(eviction) — 必须先处理, 否则会跳过后面的归还逻辑; 泄漏任务也先取消(连接即将回池, 不再追踪)。**Why 回滚脏事务？** 用户 close 时若 autoCommit=false 且事务被改过, 回滚防止"半提交状态"泄漏给下一个使用者; resetConnectionState 把 readOnly/autoCommit 等复位到配置值(H-5 的 setupConnection 逆操作)。[模式: 代理拦截 + 状态复位]

数据流: 用户 close()(L240) → closeStatements(L243) → leakTask.cancel(L246) → 脏事务?→ rollback(L250) → dirtyBits?→ resetConnectionState(L255) → clearWarnings(L258) → finally: delegate=CLOSED(L266) → `poolEntry.recycle()`(PoolEntry L77) → `hikariPool.recycle(this)`(L434, §2)。

### 2. HikariPool.recycle — 回收分派

场景: 关闭的代理连接怎么决定"回池"还是"真关"?

源码路径:
- `HikariPool.java:434` — **recycle(poolEntry)**: 回收编排(由 close→`poolEntry.recycle()`(L77)→`hikariPool.recycle(this)` 进入)
- `HikariPool.java:436` — **记录使用**: `metricsTracker.recordConnectionUsage`(L436, H-9)
- `HikariPool.java:437,438` — **失效分支**: `if (isMarkedEvicted()) → closeConnection(poolEntry, EVICTED_CONNECTION_MESSAGE)`(L437-438) — 已标记淘汰则真关底层
- `HikariPool.java:447` — **正常分支**: `connectionBag.requite(poolEntry)`(L447, H-2) — 回容器

关键设计: **Why evicted 判断？** 连接可能被 HouseKeeper/异常标记淘汰 — 归还时先查 isMarkedEvicted, 是则直接真关(不再回池), 否则 requite 回容器; 这保证"已淘汰的连接不再被复用"。**Why 复用 requite？** 回容器逻辑(H-2: handoff 给等待者/回 thread-local)已实现, recycle 只做分派。[模式: 回收分派 + 复用边界]

数据流: close 委托 → HikariPool.recycle(L434) → recordConnectionUsage(L436) → isMarkedEvicted?→ 是: closeConnection(L438) 真关; 否: connectionBag.requite(poolEntry)(L447)。

### 3. ConcurrentBag.requite — 回容器 (复用 H-2)

场景: requite 之后连接去了哪?

源码路径:
- `ConcurrentBag.requite`(H-2 已讲): `setState(NOT_IN_USE)` → 有等待者则 handoffQueue 直传, 否则回 thread-local 亲和表(<16)
- 衔接: 归还终点 = H-2 的无锁容器; 用户后续 getConnection 从同一容器借出(H-3)
- 边界: 代理创建/拦截(H-12)、容器(H-2)、获取(H-3)均已在各自域展开, 本域只讲"归还编排"

关键设计: **Why 归还终点在容器？** 归还的本质是把 PoolEntry 状态切回 NOT_IN_USE 并放回可用集合(thread-local 亲和或 handoff) — 由 H-2 的 requite 统一处理, 归还编排(recycle)只负责"分派 + 状态复位"。[模式: 复用边界]

数据流: recycle → requite(poolEntry)(L447) → ConcurrentBag 内 setState(NOT_IN_USE) → 等待者?handoffQueue 直传 : 回 thread-local(H-2)。下次 borrow 从容器取出复用。

→ 引出 H-6: HouseKeeper — 归还之后: 30s 定时维护(idleTimeout 淘汰/fillPool 补 minIdle/ClockSource)。
