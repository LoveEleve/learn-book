# E-1 Index Engine 篇 3/3 — 版本管理: LiveVersionMap 与失败分级

> 前置: [[E-1-engine-01]] [[E-1-engine-02]] | 复用: — | 对照: [[rd2-rlock]] (看门狗续期对照) [[r21-db]] (版本语义) | 引出: [[E-5-shard]] [[E-6-seqno]]
> 🔴 A | 来源: LiveVersionMap.java:25,120-175,211,216 + InternalEngine.java:1404-1444,182,291
> 定位: Engine 卷收尾 — 回答"内存版本表怎么无锁并发 + 失败怎么分级"

**读者处境**: Engine 里有个 LiveVersionMap — 面试官问 "ES 的版本控制在哪? 怎么做到高并发?" 你答 "LiveVersionMap" — 但再问 "refresh 时它怎么切换? 为什么不用锁?" 你卡住了。这篇是版本管理 + 失败分级的完整答案, 收束 Engine 域。

### 1. 问题引入 — 版本表为什么存在

场景: 同一文档 id 被写 100 次, 每次都要知道"当前版本是多少"才能决定冲突与否 — 但查 Lucene 太慢 (要打开 segment reader)。
- 答案: LiveVersionMap — 内存中的 id → (version, seqNo, term, translogLocation) 映射
- 全貌 (LiveVersionMap.java:25): implements ReferenceManager.RefreshListener — **版本表生命周期挂在 refresh 事件上**

### 2. 双 map 结构 — 无锁并发读

场景: 为什么刷新时不需要锁?
- Maps(current, old) (InternalEngine.java:125-169): current 新写, old 待清
- buildTransitionMap (InternalEngine.java:146-152): beforeRefresh 时建新 current, 旧 current 变 old — "Builds a new map for the refresh transition"
- invalidateOldMap (InternalEngine.java:165-169): afterRefresh 时 old 清空归档 (archive.afterRefresh(old))
- **并发安全** (InternalEngine.java:120-122): "we don't need to maintain a happens before relationship across doc IDs... volatile read of the Maps reference"
- tombstones (InternalEngine.java:211): 删除标记独立 map; unsafeKeysMap (InternalEngine.java:216): 断言用
- 关键设计: 读路径无锁 (volatile 引用发布), 写路径靠 Engine 的 uid 锁 (篇 1) 串行化同文档

### 3. 版本检查 — plan 阶段的消费

场景: 版本表怎么被计划阶段消费?
- planIndexingAsPrimary (InternalEngine.java:1314-1383): `versionMap.enforceSafeAccess()` (InternalEngine.java:1333) → resolveDocVersion (InternalEngine.java:1335) → 冲突三分支 (InternalEngine.java:1349,1360,1367)
- index 执行后: versionMap.maybePutIndexUnderLock (InternalEngine.java:1238) 更新版本
- 时空溯源: v0.90 单 ConcurrentMap (RobinEngine.java:182) → v8.12 双 map — "单 map 无法安全清理, 双 map 让已 refresh 键进 old"

### 4. 失败分级 — 文档失败 vs tragic

场景: 写入失败, 什么时候只失败请求, 什么时候整个引擎挂掉?
- indexIntoLucene catch (InternalEngine.java:1404-1430): `getTragicException() == null && treatDocumentFailureAsTragicError(index) == false` → IndexResult(FAILURE) (InternalEngine.java:1428)
- treatDocumentFailureAsTragicError (InternalEngine.java:1435-1443): REPLICA / PEER_RECOVERY / LOCAL_RESET → tragic; PRIMARY → 单请求失败
- **不对称根因** (InternalEngine.java:1436-1439): "we prefer to fail a request individually (instead of a shard) if we hit a document failure on the primary" — 主分片失败可重试, 副本失败 = 数据流损坏
- **运维视角**: tragic 后引擎整体失效 → 上层 IndexShard 标记 failed (E-5 展开) — 数据可从副本恢复, 单副本需 snapshot 兜底
- 与 E-3 translog tragedy 对照: 同一"状态不可信 → 整体失效"模式

### 5. soft deletes — 删除历史的保留

场景: 删除的文档去哪了?
- softDeletesField (InternalEngine.java:182): 每个文档写入软删除标记 (L1474 附近 add)
- SoftDeletesPolicy: 保留删除历史 (供 peer recovery 从 seqNo 恢复, E-6/E-5 衔接)
- 代价: 磁盘保留已删文档直到 merge 清理 (E-8 Merge 衔接)

### 6. 收束 — 与 Redis 版本语义对照

- Redis: 无版本概念, 覆盖写 (WATCH/MULTI 才有 CAS, r21-db 对照)
- ES: 版本号 + seqNo + primaryTerm 三维版本 (E-6 展开) — 分布式复制需要逻辑位点
- 终极结论: LiveVersionMap = "写后即时可见 + 无锁并发 + refresh 事件驱动" 三合一; 失败分级 = "可重试的请求失败 vs 不可信的引擎失效"
- 引出: E-5 Shard (引擎生命周期) — E-6 SeqNo (版本的三维语义)

### 核心悬念
"版本表在 refresh 时为什么不需要锁?" — 双 map 切换: 新写进 current, 旧键在 old 里继续可读, afterRefresh 后 old 清空归档 — 无锁读靠 volatile 引用发布。

### 概念依赖链
Q3 双 map → Q6 失败分级 → Q8 Redis 对照 → (E-6 seqNo 衔接)

### 源码锚点清单
- LiveVersionMap.java:25 (RefreshListener) / 120-122 (无锁注释) / 125-169 (Maps 双 map) / 146-152 (buildTransitionMap) / 165-169 (invalidateOldMap) / 211 (tombstones) / 216 (unsafeKeysMap)
- InternalEngine.java:182 (softDeletesField) / 291 (addListener versionMap) / 1238 (maybePutIndexUnderLock) / 1314-1383 (plan 消费) / 1333 (enforceSafeAccess) / 1404-1430 (catch 分级) / 1435-1443 (treatDocumentFailureAsTragicError)
- RobinEngine.java:182 (v0.90 单 map, git show 实证)
