# S-11 undo_log 可靠性 — Pass 1 探索笔记

> 域: S-11 undo_log 可靠性 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: rm-datasource/AsyncWorker (229) + rm-datasource/undo/mysql/MySQLUndoLogManager (子表拆分 + 清理) + AbstractUndoLogManager (压缩 S-2 实证) + DefaultValues | Seata 2.5.0

## 调用图

```
写入面 (S-2 交叉):
  flushUndoLogs → needCompress (enable && >64k → zip) → insertUndoLogWithNormal
    → MySQL: maxAllowedPacket (context/1MB 默认) × 0.8 = limit
    → 超限 → 拆分: 首片主行 (branchId) + 子片子行 (UUID subId, SUB_SPLIT_KEY 拼接)
    → context 记录 SUB_ID_KEY

回滚面 (S-2 交叉):
  undo → getRollbackInfo → SUB_ID_KEY → getSubRollbackInfo (SELECT branch_id IN (...) AND xid)
    → 主+子字节拼接 → 解压 (compressorType from context)

清理面:
  AsyncWorker (S-1 canBeCommittedAsync 消费):
    branchCommit → Phase2Context 入队 (缓冲 10000) → 立即返回 Committed
    定时 1s: drainTo → 按 resourceId 分组 → Lists.partition(1000) → batchDeleteUndoLog
    失败/资源缺失 → requeue (无限重试)
  S-3 undoLogDelete 广播 → RM 侧 deleteUndoLogByLogCreated (log_created <= ? LIMIT ?)
  GlobalFinished 防护 (#489, S-2 实证)
```

## 基本元素分解

1. **压缩面**: needCompress (>64k) + CompressorFactory (zip) + context 记录 + 解压链
2. **子表面**: 80% 阈值拆分 + 主/子行 + SUB_ID_KEY + 读取拼接
3. **AsyncWorker**: 缓冲队列 (10000) + 满时紧急 + 按资源分组 + 1000 分片 + requeue
4. **清理面**: deleteUndoLogByLogCreated (LIMIT) + 广播触发 + GlobalFinished

## 标记问题 (20 问)

1. 压缩阈值? (>64k 严格, S-2 实证)
2. 压缩类型? (zip 默认)
3. 解压链? (compressorType from context)
4. 子表拆分阈值? (maxAllowedPacket × 0.8)
5. maxAllowedPacket? (context 记录 / 1MB 默认)
6. 首片在哪? (主行 branchId)
7. 子片 id? (UUID + SUB_SPLIT_KEY)
8. 读取拼接? (SELECT IN + 字节拼接)
9. AsyncWorker 队列? (10000)
10. 队列满? (紧急清 + 重入)
11. 分组? (按 resourceId)
12. 分片? (Lists.partition 1000)
13. requeue? (失败无限重试)
14. branchCommit 返回? (立即 Committed)
15. deleteUndoLogByLogCreated? (log_created <= ? LIMIT ?)
16. 触发? (S-3 undoLogDelete 广播)
17. GlobalFinished? (#489 防护)
18. 对照 ZK? (快照压缩 4.3)
19. 定时周期? (10ms 初始/1s)
20. saveDays? (清理保留天数)

## 时空溯源 (代码内注释锚)

- AsyncWorker:88-90 "if fail(which means the queue is full), then doBranchCommit urgently" — 背压注释
- AsyncWorker:149 "failed to find resource for {} and requeue" — 无限重试
- MySQLUndoLogManager:143 "1MB -> mysql5.6 default value" — maxAllowedPacket 默认
- MySQLUndoLogManager:61-62 DELETE ... LIMIT ? — 分页删除

## 大域拆分判断

S-11 = 可靠性汇总面 (压缩 + 子表 + 批删 + 清理); 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "压缩+子表+for(;;)无限重试+AsyncWorker/GlobalFinished/并发回滚保护" | 压缩 (S-2) + 子表拆分 (80% 阈值) + AsyncWorker requeue + GlobalFinished (S-2) | **接受** ✅ |
| 数字: 缓冲 | DEFAULT_CLIENT_ASYNC_COMMIT_BUFFER_LIMIT=10000 (DefaultValues:48) | **补充** ✅ |
| 数字: 拆分阈值 | maxAllowedPacket × 0.8; 默认 1MB → 0.8MB 片 (L136-143) | **补充** ✅ |
| 数字: 分片 | Lists.partition 1000 (AsyncWorker:160) | **补充** ✅ |
