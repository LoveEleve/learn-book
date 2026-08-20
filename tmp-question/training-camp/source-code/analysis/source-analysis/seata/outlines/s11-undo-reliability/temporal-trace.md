# S-11 undo_log 可靠性 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 0.9~1.x | 骨架: 压缩 (64k 阈值) + undo_log 单行; GlobalFinished (#489 防 Phase1 提交) |
| 1.x | **子表拆分**: maxAllowedPacket × 0.8 + UUID 子片 + SUB_ID_KEY (MySQL 先行) |
| 2.x | **AsyncWorker 异步批删**: 缓冲队列 + 分组 + 1000 分片 + requeue; batchDeleteUndoLog IN 拼接; deleteUndoLogByLogCreated 方言实现 |
| 2.5.0 | 缓冲配置 (CLIENT_ASYNC_COMMIT_BUFFER_LIMIT=10000); 子表/批删稳定 |

## 痕迹证据

- AsyncWorker.java:88-90: "if fail(which means the queue is full), then doBranchCommit urgently" (2.x 锚)
- AsyncWorker.java:149: "failed to find resource for {} and requeue" (2.x 锚)
- MySQLUndoLogManager.java:143: "1MB -> mysql5.6 default value" (1.x 锚)
- MySQLUndoLogManager.java:61-62: DELETE ... LIMIT ? (2.x 锚)
- AbstractUndoLogManager.java:394-397: issue #489 (1.x 锚)

## 推断标注

- "0.9~1.x 骨架" — Fescar 起 (公知版本线) (标注)
- "2.x AsyncWorker" — 缓冲配置存在性推断 (标注)
- "2.5.0 10000" — DefaultValues 实证 (实证)
- git 多 commit 可考古 — 本域以注释锚 + 配置键为主

## 对照线 (阶段 4.3 已交付)

- ZK 快照压缩: GZIP/SNAPPY/CHECKED (SnapStream) vs Seata zip — 同思路
- ZK 快照回退: 100 代 vs Seata undo_log 定时清理 — 保留策略对照
