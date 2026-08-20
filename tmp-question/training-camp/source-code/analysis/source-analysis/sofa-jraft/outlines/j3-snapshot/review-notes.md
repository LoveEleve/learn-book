# J-3 review-notes — 六层深审记录 (2026-08-15)

## 审法: 探索代理锚点 + 本审抽查 + 纯逻辑 harness

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "snapshotTimer = 3600s + 首触发随机化" — NodeImpl.java:966, L977-990 | 通过 ✅ |
| 2 | 事实 | §1 "snapshotLogIndexMargin 默认 0" — NodeOptions.java:77 | 通过 ✅ |
| 3 | 事实 | §1 "单快照 EBUSY" — SnapshotExecutorImpl.java:330-340 | 通过 ✅ |
| 4 | 事实 | §2 "SNAPSHOT 任务与 apply 同队列" — FSMCallerImpl.java:201-210, L407-459 | 通过 ✅ |
| 5 | 事实 | §2 "SnapshotMeta = lastAppliedIndex/Term" — L624-627; 配置 L628-648 | 通过 ✅ |
| 6 | 事实 | §3 "temp → 原子 rename" — LocalSnapshotStorage.java:247-261; EEXISTS L240-245; 引用计数 L262-270 | 通过 ✅ |
| 7 | 事实 | §3 "meta 文件 __raft_snapshot_meta" — Snapshot.java:36 | 通过 ✅ |
| 8 | 事实 | §4 "remote:// URI + FileService" — LocalSnapshotReader.java:144-151; Replicator.java:649 | 通过 ✅ |
| 9 | 事实 | §4 "硬链接复用" — LocalSnapshotCopier.java:254-328 (Files.createLink L305) | 通过 ✅ |
| 10 | 事实 | §4 "分块 offset/count 断点续传" — CopySession.java:280-282, L249; EAGAIN 不消耗重试 L233 | 通过 ✅ |
| 11 | 事实 | §5 "两端共用 throttle" — SnapshotExecutorImpl.java:241-243; ThroughputSnapshotThrottle.java:55 | 通过 ✅ |
| 12 | 事实 | §6 "RPC 下载完才响应" — SnapshotExecutorImpl.java:526, L506 | 通过 ✅ |
| 13 | 事实 | §6 "安装期间 AppendEntries EBUSY" — NodeImpl.java:2074-2080; preVote 跳过 L2791-2796 | 通过 ✅ |
| 14 | 事实 | §7 "不推进 diskId + braft#224" — LogManagerImpl.java:652-659 | 通过 ✅ |
| 15 | 事实 | §7 "截断三分支" — L661-682 (全截 L661-666/上次快照点 L667-677/reset L678-682) | 通过 ✅ |
| 16 | 事实 | §8 "快照配置 → updateConfigurationAfterInstallingSnapshot" — NodeImpl.java:3502-3504 | 通过 ✅ |
| 17 | 事实 | §6 "ESTALE 拒绝旧快照" — FSMCallerImpl.java:716-722 | 通过 ✅ |
| 18 | 数字 | 关键常量: 3600s/默认 margin 0/快照目录前缀 snapshot_ | 通过 ✅ |
| 19 | 结构 | 负面空间 "不做增量快照" 与硬链接复用自洽 (文件级复用≠数据级增量) | 通过 ✅ |

**结论**: 19 项核对 0 修正。harness 验证冻结/改名/截断/复用语义。

## harness 设计 (MiniSnapshot — 纯逻辑)

- A. 冻结语义: 快照任务在 apply 之后执行, 快照点 = 当时 lastApplied
- B. temp 原子改名: 崩溃残留 temp 被清理, 正式目录只有完整快照
- C. 截断三分支: term==0 全截 / term 匹配只截到上次快照点 / term 不匹配 reset
- D. 引用计数: 传输中的快照不删, 归零才删
- E. 硬链接复用: 相同 checksum 文件不重复下载
