# 闭环笔记 q4: 清理与运维面 — PurgeTxnLog + 尺寸面 + 压缩

## 假设
日志无限增长 → 快照保留 N 个 + 可恢复性守卫; 尺寸/压缩可配。

## 验证过程
- **PurgeTxnLog.purge** (PurgeTxnLog:60-73): **num >= 3 硬约束** (COUNT_ERR_MSG L39) — 至少保留 3 个快照; findNValidSnapshots(num) → 取最旧的保留
- **可恢复性守卫** (PurgeTxnLog:79-166): leastZxidToBeRetain = 最旧保留快照 zxid → 删除 zxid < 该值的 log+snapshot, **但 getSnapshotLogs(leastZxidToBeRetain) 保留** — 注释 (L95-122): **log.(X-a) 可能包含 >X 的事务** (学习者状态机无 log 滚动快照) → 删之则快照 X 不可恢复
- **自动清理**: DatadirCleanupManager (autopurge.snapRetainCount/purgeInterval) 定时调 purge (server/DatadirCleanupManager:31-32)
- **尺寸面**: snapCount 默认 100000 (ZooKeeperServer:224, Z-4 交叉) → SyncRequestProcessor:146 logCount > snapCount/2 + randRoll 触发快照; txnLogSizeLimitInKb **默认 -1 禁用** (FileTxnLog:122-147, 3.9) — 超限 commit 时 rollLog (L434-442); calculateTxnLogSizeLimit = **snapshotSizeFactor × 最新快照大小** (ZKDatabase:365-376) — learner txnlog 同步限流 (Z-2)
- **压缩面**: SnapStream StreamMode **GZIP/SNAPPY/CHECKED 默认 CHECKED** (L64-93, zookeeper.snapshot.compression.method); 扩展名驱动读侧 (L227-237), 写侧全局模式
- **运维工具**: TxnLogToolkit (447) — 转储/修复日志 CLI; fsync 阈值告警 (FileTxnLog:108-137, 默认 1000ms)

## 代码类型
Operation (清理/运维)

## 跨域关联
- Z-2: learner txnlog 同步限流 (snapshotSizeFactor)
- Z-4: snapCount 触发 + Sync 批量
- Z-6: 无 (纯存储面)

## 结论
清理 = 保留 N≥3 快照 + getSnapshotLogs 守卫 (跨界日志); 尺寸双面 (snapCount 随机触发 / txnLogSizeLimit 3.9 可选); 压缩三模式默认不压。
源码位置: PurgeTxnLog.java:39-166; FileTxnLog.java:122-147,434-442; SnapStream.java:64-93; ZKDatabase.java:365-376; DatadirCleanupManager.java:31-32
