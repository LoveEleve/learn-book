# E-1 闭环笔记 Q5-Q8: flush/失败边界/并发/Redis 对照

## Q5: flush vs refresh vs commit — 三层持久化语义

假设: refresh = 内存段可见; flush = refresh + IndexWriter.commit (落盘) + translog 轮转; commit 是 flush 的子步骤。

验证过程:
- Read flush (InternalEngine.java:2173-2260): flushLock.tryLock (InternalEngine.java:2184) 防并发 → hasUncommittedChanges 判定 (InternalEngine.java:2199) → `translog.rollGeneration()` (L2212, E-3 衔接) → IndexWriter commit (L2225-2230 区) → `translog.trimUnreferencedReaders()` (InternalEngine.java:2225) → waitForCommitDurability (InternalEngine.java:2255)
- shouldPeriodicallyFlush (InternalEngine.java:2129): 阈值 512MB / 1min (来自 E-3 的 IndexSettings)
- refresh (InternalEngine.java:2022): 只更新 ReaderManager, 不落盘 — 与 flush 的本质区别
- 测试: testSyncedFlushSurvivesEngineRestart (InternalEngine.java:1267)

代码类型: Implementation (持久化状态机)

结论: **refresh 打开新内存段 reader (搜索可见, 不落盘); flush = refresh + commit (IndexWriter 把段 fsync 落盘) + translog 轮转截断 (E-3 衔接, 旧代可删); commit 记录安全水位供 recovery — 三层语义: 可见性/持久化/恢复点**。InternalEngine.java:2173-2255 + L2022

## Q6: 文档失败 vs tragic — 主副分片不对称处理

假设: 主分片文档失败只失败该请求 (返回 IndexResult FAILURE), 副本/恢复失败视为 tragic (整体失效) — 因为副本无法单独重试。

验证过程:
- Read indexIntoLucene catch (InternalEngine.java:1404-1430): `indexWriter.getTragicException() == null && treatDocumentFailureAsTragicError(index) == false` → `return new IndexResult(ex, ...)` (L1428, 文档失败)
- Read treatDocumentFailureAsTragicError (InternalEngine.java:1435-1443): REPLICA / PEER_RECOVERY / LOCAL_RESET → true; PRIMARY → false
- 注释 (InternalEngine.java:1417-1426): "if it's a document failure then `indexWriter.getTragicException()` will be null otherwise we have to rethrow and treat it as fatal"
- 设计原因 (InternalEngine.java:1436-1439): "we prefer to fail a request individually (instead of a shard) if we hit a document failure on the primary" — 主分片可重试, 副本失败说明数据流损坏

代码类型: Implementation (故障分级)

结论: **同一异常两种处理: 主分片文档失败 = 单请求失败 (客户端可重试); 副本/恢复文档失败 = tragic (Lucene 状态可能损坏, 必须整体失效) — 不对称根因: 主分片失败可重放, 副本失败意味着主副数据流不一致**。InternalEngine.java:1404-1444

## Q7: 并发控制 — readLock + uid 锁 + throttle

假设: 三级并发控制: engine 级 readLock (并发写) / uid 级锁 (同文档串行) / throttle (写压控速)。

验证过程:
- Read index (InternalEngine.java:1134-1141): `readLock.acquire()` (InternalEngine.java:1134) + `versionMap.acquireLock(index.uid().bytes())` (InternalEngine.java:1139) + `throttle.acquireThrottle()` (InternalEngine.java:1140)
- 锁粒度: readLock 允许所有文档并发写; uid 锁只串行化同一文档 (防止同 id 并发版本竞争); throttle 控制整体写入速率 (IndexThrottle)
- 对照 E-3 Translog: translog 内部还有 syncLock/synchronized(this) — Engine 锁在文档层, translog 锁在缓冲层, 两层正交

代码类型: Implementation (锁粒度分层)

结论: **三级并发: readLock (全文档并发) → uid 锁 (同文档串行, versionMap.acquireLock) → throttle (全局限速) — 与 E-3 translog 的锁层次正交: Engine 管文档级, translog 管缓冲级**。InternalEngine.java:1134-1141

跨域关联: E-3 Translog (锁层次对照) / E-6 SeqNo (LocalCheckpointTracker 并发推进)

## Q8: 与 Redis 持久化对照 — 双写一致性

假设: Redis (RDB 快照 + AOF 日志) vs ES (Lucene commit + translog) — 同为"快照+日志"双轨, 但恢复粒度不同。

验证过程:
- Redis: rdbSaveType (rdb.c:95) 全量快照; AOF rewrite (aof.c, auto-aof-rewrite-percentage=100) 后台重写 (r8-persistence 已交付)
- ES: Lucene commit = 段快照 (IndexWriter.commit L2225); translog = 未提交增量日志 (E-3 交付)
- **关键差异**: Redis RDB/AOF 都是"全量数据"的两种表示; ES translog 只含"未 commit 操作" — commit 后 translog 可删 (flush L2212 轮转)
- 双写顺序: ES 先 translog 后 Lucene (E-3 Q1 已证); Redis 先 AOF 后内存改? (r8-persistence 对照)

代码类型: 对照分析

结论: **同为"快照+WAL"双轨, 但角色不同: Redis RDB 是完整快照 (AOF 是完整日志, 都代表全量), ES commit 是段快照 (translog 只是未提交增量, commit 后即弃) — ES 的日志更短, 因为磁盘索引本身承担了持久化**。对照锚点: InternalEngine.java:2212 vs rdb.c:95

跨域关联: [[r8-persistence]] (Redis 持久化) / [[E-3-translog-03]] (generation 生命周期)
