# E-1 Index Engine 篇 2/3 — 可见性与持久化: refresh/flush/NRT

> 前置: [[E-1-engine-01]] (写入路径) [[E-3-translog-03]] (generation 生命周期) | 复用: — | 对照: [[r8-persistence]] (Redis 快照) | 引出: [[E-1-engine-03]] [[E-2-search]] (searcher 消费) [[E-5-shard]]
> 🔴 A | 来源: InternalEngine.java:2022,2129,2173 + LiveVersionMap.java + IndexSettings.java:279
> 定位: Engine 卷中篇 — 回答"写入后多久能搜到? refresh/flush/commit 差在哪?"

**读者处境**: 写入返回 201, 但立刻搜索查不到 — 为什么? 面试官问 "ES 的 NRT (近实时) 是什么? refresh 和 flush 区别?" 你答 "refresh 1s 一次, flush 落盘" — 但为什么 realtime get 能立即看到? 这篇是可见性三层语义的完整答案。

### 1. 问题引入 — 三级可见性

场景: `PUT /idx/_doc/1` 成功后, 三个动作的可见性完全不同:
- realtime get: 立即可见 (LiveVersionMap + translog)
- search: 1s 后可见 (refresh)
- 崩溃恢复: flush 后才有 commit 点
- 本篇问题: 三层各自的机制与成本

### 2. realtime get — 不查 Lucene 的读

场景: 为什么 get 比 search 快看到?
- get (InternalEngine.java:827-845): realtime → realtimeGetUnderLock (InternalEngine.java:865-905)
- realtimeGetUnderLock (InternalEngine.java:865-924): versionMap.acquireLock(uid) (L869-871, "to do this truly in RT") → getVersionFromMap → 非删除 → 版本检查 → 从 translog 读 (InternalEngine.java:916) → 兜底 searcher (InternalEngine.java:923-924)
- 测试实证: testSimpleOperations (InternalEngine.java:828-1042): index → realtime 可见 (InternalEngine.java:871-876) → 非 realtime 不可见 (InternalEngine.java:877-882) → refresh 后可见 (InternalEngine.java:884-900)
- 关键设计: realtime get 走内存版本表 + WAL, **不打开新 reader** — 单文档查询的廉价可见性

### 3. refresh — NRT 的开关

场景: 搜索什么时候能看到新文档?
- refresh (InternalEngine.java:2022-2060): ReferenceManager.maybeRefresh (InternalEngine.java:2044) / maybeRefreshBlocking (InternalEngine.java:2041)
- **双 ReaderManager** (InternalEngine.java:140-141): internal (引擎内部) + external (搜索用) — "even though we maintain 2 managers we really do the heavy-lifting only once" (InternalEngine.java:2035)
- 默认 1s (IndexSettings.java:279 DEFAULT_REFRESH_INTERVAL) — 由 IndexShard 定时触发 (E-5 衔接)
- 内存代价: 新段 reader 持有已删文档 (旧段标记删除, 直到 merge 才物理清理) — NRT 与段合并的权衡 (E-8 Merge 衔接)

### 4. flush — 持久化的分水岭

场景: refresh 和 flush 到底差在哪?
- flush (InternalEngine.java:2173-2260): flushLock.tryLock (InternalEngine.java:2184) → hasUncommittedChanges (InternalEngine.java:2199) → `translog.rollGeneration()` (InternalEngine.java:2212) → IndexWriter commit → `translog.trimUnreferencedReaders()` (InternalEngine.java:2225) → waitForCommitDurability (InternalEngine.java:2255)
- shouldPeriodicallyFlush (InternalEngine.java:2129): 阈值 512MB / 1min; 大 merge 后强制 (InternalEngine.java:2834)
- **三层语义**: refresh (内存段可见) < flush (段落盘 + translog 轮转) < commit (安全水位, recovery 起点)
- 对照 Redis: RDB 全量快照 (rdb.c:95) vs Lucene commit 增量段快照 — 快照粒度差异

### 5. 时空溯源 — SearcherManager → 双 ReaderManager

场景: 可见性机制怎么演化的?
- v0.90: RobinEngine 单 SearcherManager (InternalEngine.java:115) + flushNeeded 标志 (InternalEngine.java:127)
- v8.12: internal/external 双 ReaderManager (InternalEngine.java:140-141) + LiveVersionMap 作为 refresh listener (InternalEngine.java:291)
- 变迁: 单 reader → 双 reader (内部分离 warming/缓存) → LiveVersionMap 挂在 refresh 事件上 (双 map 切换, 篇 3 展开)

### 6. 收束 — NRT 的成本与权衡

- realtime get: 版本表 + translog 读 — 单文档廉价
- refresh 1s: 搜索可见性延迟 — 可配 refresh_interval 调优
- flush: 落盘成本 — 由阈值驱动而非定时
- 终极结论: NRT = "内存段可见 + 版本表即时读 + WAL 兜底" 的三层配合
- 引出: 篇 3 (LiveVersionMap 双 map 与失败分级)

### 核心悬念
"写入了为什么搜不到, 但 get 能到?" — 因为 realtime get 走内存版本表+WAL, search 要等 refresh 打开新段 reader — 两级可见性是成本与延迟的折中。

### 概念依赖链
Q4 realtime get → Q5 refresh/flush/commit 三层 → 时空溯源 → (篇 3 LiveVersionMap)

### 源码锚点清单
- InternalEngine.java:827-845 (get) / 865-924 (realtimeGetUnderLock) / 140-141 (双 ReaderManager) / 2022-2060 (refresh) / 2041-2051 (双 manager 一次重活) / 2129 (shouldPeriodicallyFlush) / 2173-2260 (flush) / 2212 (translog.rollGeneration) / 2225 (trimUnreferencedReaders) / 2255 (waitForCommitDurability) / 2834 (大 merge 后)
- IndexSettings.java:279 (DEFAULT_REFRESH_INTERVAL 1s)
- rdb.c:95 (Redis RDB 快照对照)
