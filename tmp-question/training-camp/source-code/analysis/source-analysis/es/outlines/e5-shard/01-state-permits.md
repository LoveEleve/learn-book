# E-5 Shard 篇 1/3 — 状态机与操作许可: 分片怎么活着

> 前置: [[E-1-engine-03]] (引擎生命周期) [[E-6-seqno-01]] | 复用: — | 对照: [[r20-server]] (服务端骨架) | 引出: [[E-5-shard-02]] [[E-5-shard-03]] [[E-10-clusterstate]]
> 🔴 A | 来源: IndexShardState.java:10-17 + IndexShard.java:493,885 + IndexShardOperationPermits.java:49-50,82-153
> 定位: Shard 卷开篇 — 回答"分片 5 态怎么迁? permits 怎么两模式?"

**读者处境**: 一个分片有"状态"和"操作许可"两套机制 — 面试官问 "ES 分片有几种状态? 怎么迁移?" "close 时正在进行的写操作怎么办?" 你答 "5 态 + 信号量" — 这篇是分片生命周期的骨架答案。

### 1. 问题引入 — 分片的两套机制

场景: 一个分片同时被读写、被恢复、被关闭 — 怎么保证状态一致?
- 状态机: 5 态 (IndexShardState.java:10-17) — 生命周期
- 操作许可: Semaphore (IndexShardOperationPermits.java:49-50) — 并发控制
- 本篇问题: 状态怎么迁 (Q1) + 操作怎么控 (Q2) + 关闭怎么收尾 (Q6)

### 2. 5 态状态机 — 双轨驱动

场景: 状态什么时候变?
- 5 态: CREATED(0)/RECOVERING(1)/POST_RECOVERY(2)/STARTED(3)/CLOSED(5) — RELOCATED 已移除 (兼容 IDS[4]=STARTED, L25-26)
- 双轨驱动:
  - 集群驱动: updateShardState (IndexShard.java:493) — POST_RECOVERY→STARTED (IndexShard.java:529-536, "global state is...")
  - 本地驱动: recoverFromStore→RECOVERING (IndexShard.java:744) / postRecovery→POST_RECOVERY (IndexShard.java:1723)
- 迁移规则: 主→副本非法 (IndexShard.java:517-524 IllegalArgumentException); changeState 统一 mutex (IndexShard.java:885-892)
- 时空溯源: v0.90 5 态含 RELOCATED → v5.0 +POST_RECOVERY (6 态) → v8.12 -RELOCATED (5 态, 848c7e4917c 2018-03-28)

### 3. 操作许可 — 双模式信号量

场景: MAX_VALUE 的信号量怎么"正常并发 + 阻塞全占"?
- 结构 (IndexShardOperationPermits.java:49-50): `TOTAL_PERMITS = Integer.MAX_VALUE` + `Semaphore(MAX_VALUE, true)` (fair 防饿死)
- 正常模式: 单 permit (IndexShardOperationPermits.java:259-260) — 写操作几乎不互斥
- 阻塞模式: blockOperations (IndexShardOperationPermits.java:82-116): delayOperations 调用 (IndexShardOperationPermits.java:88 排队新操作) → waitUntilBlocked 调用 (IndexShardOperationPermits.java:89 异步) → acquireAll (IndexShardOperationPermits.java:110)
- acquireAll (IndexShardOperationPermits.java:138-153): `semaphore.tryAcquire(TOTAL_PERMITS, timeout)` (IndexShardOperationPermits.java:145) — **全占 = 等所有在途完成**; 释放 release(TOTAL_PERMITS) (IndexShardOperationPermits.java:149)
- **超时语义**: 超时抛 ElasticsearchTimeoutException (IndexShardOperationPermits.java:152-153); timeout 由调用方传 (close/promotion/recovery 各自定)
- 目的: close/promotion/recovery 时需要"无新操作插入"的稳定窗口

### 4. 关闭 — 先拒后排

场景: close 时正在进行的写怎么办?
- close (IndexShard.java:1672-1690): changeState(CLOSED) (IndexShard.java:1676) → engine.flushAndClose (IndexShard.java:1683, 可配) → IOUtils.close(engine+listeners) (IndexShard.java:1688) → permits.close() (IndexShard.java:1689)
- 拒绝: delayOperations 检查 closed (IndexShardOperationPermits.java:128-136, closed 检查 L130) — 关闭后拒绝排队
- 测试: testClosesPreventsNewOperations (IndexShardTests.java:335-364): 所有 acquire 抛 IndexShardClosedException
- 不可逆: CLOSED 是唯一终态

### 5. 收束 — 生命周期的骨架

- 状态机 = 生命周期 (谁来驱动), permits = 并发控制 (操作怎么排队)
- 引出: 篇 2 (主升/复制组) — 篇 3 (恢复/对照)

### 核心悬念
"MAX_VALUE 的信号量怎么做到'阻塞'?" — 全占 = 取走全部 permit, 等价于等待所有在途操作完成 — "先排队新操作, 再等旧操作排空" 的两步窗口。

### 概念依赖链
Q1 状态机 → Q2 permits 双模式 → Q6 关闭 → (时空溯源)

### 源码锚点清单
- IndexShardState.java:10-17 (5 态) / 25-26 (RELOCATED 兼容)
- IndexShard.java:493 (updateShardState) / 517-524 (主→副本非法) / 529-536 (POST_RECOVERY→STARTED) / 744 (RECOVERING) / 885-892 (changeState) / 1672-1690 (close) / 1704 (postRecovery) / 1723 (POST_RECOVERY) / 2370 (recoverFromStore)
- IndexShardOperationPermits.java:49-50 (Semaphore) / 82-116 (blockOperations) / 136-140 (closed 拒绝) / 138-153 (acquireAll) / 145 (tryAcquire 全占) / 259-260 (单 permit)
- IndexShardTests.java:335-364 (testClosesPreventsNewOperations)
