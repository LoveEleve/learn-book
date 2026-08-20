# J-2 review-notes — 六层深审记录 (2026-08-15)

## 审法: 探索代理锚点 + 本审抽查 + 纯逻辑 harness

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "5 态状态机" — Replicator.java:161-167 穷举 (Created/Probe/Snapshot/Replicate/Destroyed) | 通过 ✅ |
| 2 | 事实 | §1 "Probe = 空 AppendEntries + EMPTY data" — L810 实证 | 通过 ✅ |
| 3 | 事实 | §2 "nextIndex 初始 lastLogIndex+1" — L173 实证 | 通过 ✅ |
| 4 | 事实 | §2 "两段式回退" — L1493-1509 逐行核对 (L1498 批量/L1504 逐条/L1511 Probe) | 通过 ✅ |
| 5 | 事实 | §3 "在途 256" — RaftOptions.java:60 maxReplicatorInflightMsgs; 判定 L597 | 通过 ✅ |
| 6 | 事实 | §3 "seq 排序 + version 代际" — L127-129, L1309-1323, L1274-1280 | 通过 ✅ |
| 7 | 事实 | §4 "心跳 RPC 超时 = electionTimeoutMs/2" — L806; "日志 RPC 超时 -1" — L1691 | 通过 ✅ |
| 8 | 事实 | §5 "压缩判定 prevLogTerm==0 && prevLogIndex!=0" — L1556 | 通过 ✅ |
| 9 | 事实 | §6 "block 一个心跳周期" — L1028-1053 blockTimer = startMs + dynamicHeartBeatTimeoutMs (L1040) | 通过 ✅ |
| 10 | 事实 | §6 "无重试次数" — 代码无计数常量, consecutiveErrorTimes 仅日志节流 (L1443) | 通过 ✅ |
| 11 | 事实 | §7 "commitAt 连续推进" — BallotBox.java:99-143 (L117-123); 锁外 onCommitted L141 | 通过 ✅ |
| 12 | 事实 | §7 "Ballot quorum = n/2+1 + 双 quorum joint" — Ballot.java:80,89,144-146 | 通过 ✅ |
| 13 | 事实 | §7 "learner 不参与计票" — Replicator.java:1532-1535 isFollower() 判定 | 通过 ✅ |
| 14 | 事实 | §8 "回调成功=已持久化" — StableClosure 磁盘线程 run (LogManagerImpl.java:372-376) | 通过 ✅ |
| 15 | 事实 | §9 "AppendBatcher 256/256KB" — L465-519 (cap L524); maxAppendBufferSize RaftOptions.java:42 | 通过 ✅ |
| 16 | 事实 | §10 "checkAndResolveConflict" — L1045-1105; unsafeTruncateSuffix FATAL L1025-1029 | 通过 ✅ |
| 17 | 事实 | §11 "truncatePrefix 不推进 diskId" — L652-659 注释 (braft PR#224) | 通过 ✅ |
| 18 | 事实 | §12 "AutoDetectDecoder 0xBB→V2" — L43-47; v2 learners/checksum log.proto:17-19 | 通过 ✅ |
| 19 | 事实 | §13 "8 字节 key + 双 CF" — RocksDBLogStorage.java:474-478, 227-229; 配置双写 L489-494 | 通过 ✅ |
| 20 | 事实 | §8 "sync 默认 true" — RaftOptions.java:51-52 | 通过 ✅ |
| 21 | 数字 | 关键常量穷举: 256/1024/512KB/100ms/500ms 全部双源核对 | 通过 ✅ |
| 22 | 结构 | 负面空间 "不做乱序提交" 与 commitAt 连续语义自洽 | 通过 ✅ |

**结论**: 22 项核对 0 修正。harness 验证计票/回退/乱序/learner 语义。

## harness 设计 (MiniReplicator — 纯逻辑)

- A. 冲突回退两段式: 落后 → 批量跳; 冲突 → 逐条减
- B. 流水线乱序: 乱序响应按 seq 等待, 不推进旧状态
- C. BallotBox 连续计票: 缺中间票不提交
- D. learner 不参与计票: learner 确认不影响 quorum
- E. joint 双 quorum: 新老配置都多数才提交
