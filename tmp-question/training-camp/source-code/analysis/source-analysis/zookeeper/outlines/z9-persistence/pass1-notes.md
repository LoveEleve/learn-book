# Z-9 持久化 — Pass 1 探索笔记

> 域: Z-9 持久化 | 🔴 A 方案 (需 harness) | 2026-08-15
> 源码: server/persistence/ (FileTxnLog 860 + FileSnap 288 + FileTxnSnapLog 673 + SnapStream 329 + Util 272 + FilePadding 117 + TxnLog 162 + TxnLogToolkit 447 + SnapShot 72 + SnapshotInfo 35 = 3255) + server/PurgeTxnLog | ZooKeeper 3.9.5

## 调用图

```
写路径: SyncRequestProcessor (Z-4) → FileTxnSnapLog.append → FileTxnLog.append
          [FileHeader ZKLG|ver2|dbid] + [CRC(Adler32, payload) 8B][len 4B][payload][0x42] + 64MB 预分配
          → commit → flush + channel.force(false) (forceSync) → 关旧流留新 → 尺寸超限 rollLog

读路径: ZKDatabase.loadDataBase → FileTxnSnapLog.restore
          → FileSnap.deserialize (最多 100 快照回退 + CRC seal 三重校验)
          → fastForwardFromEdits (read(lastProcessedZxid+1) 重放事务 + compareDigest)
          → 无快照面: trustEmptySnapshot (升级逃生舱) / 空库 → save 空快照

学习者同步 (Z-2): syncFollower → TRUNC (maxCommittedLog) → learner 侧 truncateLog
          / DIFF (committedLog 窗口) / txnlog+committedLog (snapshotSizeFactor 限流) / SNAP

清理: PurgeTxnLog (num>=3) → 保留 N 快照 + getSnapshotLogs 可恢复性守卫
```

## 基本元素分解

1. **TxnLog 文件格式**: FileHeader + [CRC+len+payload+0x42] 记录链 + 预分配 ZeroPad (FileTxnLog:60-96)
2. **Snapshot 文件格式**: FileHeader + 树序列化 + seal (CRC + "/" 结束标记) ×3 段 (FileSnap:242-276, SnapStream:162-180)
3. **FileTxnSnapLog**: restore 双路径 (快照+事务重放) + save + truncateLog + 目录校验 (version-2)
4. **工具面**: PurgeTxnLog (清理) + TxnLogToolkit (repair) + FilePadding (预分配) + SnapStream (压缩模式)

## 标记问题 (20 问)

1. TxnLog 记录格式? (CRC 8B + len 4B + payload + 0x42)
2. CRC 覆盖范围? (Javadoc 声称含 len+0x42, 实现只覆盖 payload — 待实证)
3. 预分配? (64MB, position+4096 阈值)
4. commit 语义? (flush + force(false) + 关旧留新)
5. forceSync 默认? (yes, zookeeper.forceSync)
6. 尾部残缺怎么处理? (EOF 静默跳文件 — 预分配语义)
7. 中部损坏? (CRC_ERROR → IOException 致命)
8. 空尾文件? (EOF 分支自动删除恢复)
9. 快照 seal? (Adler32 + "/" 标记, 三重)
10. 快照回退? (findNValidSnapshots 最多 100)
11. restore 双路径? (快照 → fastForward 从 zxid+1)
12. 无快照有日志? (trustEmptySnapshot / throw "Something is broken!")
13. 空库? (trustEmptyDB → save 空快照 → 0)
14. truncate 边界? (exclusive: 移除 ≥zxid)
15. TRUNC 触发? (learner 领先 → maxCommittedLog)
16. 快照原子写? (AtomicFileOutputStream — fsync 才用)
17. PurgeTxnLog? (num>=3, getSnapshotLogs 守卫)
18. txnLogSizeLimit? (默认 -1 禁用, 3.9)
19. 目录校验? (dataDir/snapDir 交叉污染检查)
20. dbId 不对称? (log 动态 / snapshot -1)

## 时空溯源 (代码内注释锚)

- FileTxnLog:60-96 Javadoc 文件格式 (3.4 锚)
- FileTxnSnapLog:119 "ZOOKEEPER-1161" — datadir autocreate
- FileTxnSnapLog:163 "ZOOKEEPER-2967" — 目录交叉污染检查
- FileTxnSnapLog:285 "ZOOKEEPER-2325" — 空数据库初始化
- FileTxnSnapLog:287 "ZOOKEEPER-3056" — 3.4.x 升级逃生舱 (trustEmptySnapshot)
- FileTxnSnapLog:234 "ZOOKEEPER-3781" — 3.4→3.5 升级后强制首快照
- FileTxnSnapLog:445-453 fuzzy snapshot 容错注释 (快照中后期事务混入)
- SnapStream: ZOOKEEPER_SHAPSHOT_STREAM_MODE — 快照压缩 (3.6+ 特性面)

## 大域拆分判断

Z-9 = 双文件格式 + restore 双路径 + 清理工具; 单篇 🔴 A (8 闭环 q1-q4 + harness 4 面)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (执行计划/规划) | 验证 | 结论 |
|:--|:--|:--|
| "FileTxnLog (860) + FileSnap (288) + FileTxnSnapLog" | 行数全实证 (860/288/673); 另有 SnapStream 329 + Util 272 + FilePadding 117 + PurgeTxnLog (server/) | **接受+补充** ✅ |
| "checksum+length+bytes 记录" | Javadoc 格式 L74-95 实证 | **接受** ✅ |
| "snapCount 批量" | SyncRequestProcessor:146 (logCount > snapCount/2 + randRoll) — Z-4 交叉 | **接受** ✅ |
| "restore 双路径: 快照+事务重放" | FileTxnSnapLog.restore (L252-313) + fastForwardFromEdits (L326-377) | **接受** ✅ |
| 数字: 预分配 | preAllocSize 65536KB=64MB (FilePadding:30) | **补充** ✅ |
| 数字: 快照回退 | findNValidSnapshots(100) (FileSnap:77) | **补充** ✅ |
| 数字: PurgeTxnLog | num >= 3 (PurgeTxnLog:60-67) | **补充** ✅ |
