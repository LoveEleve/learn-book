# Z-4 Processor 链 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 3.4.x | 四处理器骨架 (Prep/Sync/Commit/Final) + outstandingChanges + snapCount — 管线定型 |
| 3.5.x | **multi 原子性强化**: getPendingChanges/rollbackPendingChanges (ZOOKEEPER-1624 注释 L228-235); **tree digest 进事务** (precalculateDigest/setTxnDigest L561-563) |
| 3.6.x | **CommitProcessor 读写分离增强**: maxReadBatchSize/maxCommitBatchSize + per-session 队列细化 (L246-279 注释); **closeSession txn 化** (CloseSessionTxn 携带路径列表 L609-611) |
| 3.9.x | **throttling**: decInProcess + THROTTLEDOP (Final L170,207-209); maxWriteQueuePollTime (Sync 批控) |

## 痕迹证据

- PrepRequestProcessor.java:228-235: ZOOKEEPER-1624 注释 (multi 父记录)
- PrepRequestProcessor.java:581-583: closeSession 竞态注释
- CommitProcessor.java:261-270: 读-提交切换注释 (maxReadBatchSize)
- SyncRequestProcessor.java:144-151: snapCount 随机抖动 (3.5+ 注释风格)
- FinalRequestProcessor.java:207-209: THROTTLEDOP (3.9 面)

## 推断标注

- "3.4.x 骨架" — 公知版本线 (ZK 3.4 处理器链定型) (标注)
- "3.5.x digest/multi" — digest 3.5 引入 (与 Z-3 一致) + multi 回滚强化推断 (标注)
- "3.6.x 读写分离增强" — maxBatchSize 配置年代推断 (标注)
- "3.9.x throttling" — 节流特性年代推断 (标注)
- git shallow (1 commit) — 无考古, 全注释锚
