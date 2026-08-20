# 闭环笔记 q3: AsyncWorker — 缓冲/分组/分片/requeue

## 假设
异步提交经缓冲队列消化; 批删失败无限重试 (requeue)。

## 验证过程
- **缓冲队列** (AsyncWorker:61-79): **LinkedBlockingQueue(ASYNC_COMMIT_BUFFER_LIMIT=10000)** (DefaultValues:48) + 2 线程 ScheduledExecutor (10ms 初始/1s 周期)
- **branchCommit** (L81-85): Phase2Context 入队 → **立即返回 PhaseTwo_Committed** — S-1 canBeCommittedAsync 消费面
- **满时背压** (L88-97): offer 失败 → **紧急 doBranchCommitSafely + thenRun 重入队** (注释 "doBranchCommit urgently so that the queue could be empty again")
- **消费流** (L113-139): drainTo 全量 → **按 resourceId 分组** (HashMap 16 初始)
- **分片批删** (L141-170): **Lists.partition(contexts, 1000)** → 逐片 batchDeleteUndoLog → **conn.commit()** (autoCommit false 时)
- **requeue 无限重试** (L149,165,189): dataSourceProxy null / SQLException → **addAllToCommitQueue** — 失败不丢 (S-7 无限重试面交叉)
- **资源缺失跳过** (L131-133): resourceId 空 → warn 跳过

## 代码类型
Architecture (异步批删)

## 跨域关联
- S-1: canBeCommittedAsync (异步提交面)
- S-2: batchDeleteUndoLog (IN 拼接, 1000 上限)
- S-7: requeue 无限重试

## 结论
AsyncWorker = 10000 缓冲 + 满时紧急 + 按资源分组 + 1000 分片 + requeue 无限重试。
源码位置: AsyncWorker.java:61-192; DefaultValues.java:46-48
