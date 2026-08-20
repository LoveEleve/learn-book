# J-5 review-notes — 六层深审记录 (2026-08-15)

## 审法: 探索代理锚点 + 本审抽查 (🟡 B 方案, 无时空溯源/极简复现)

| # | 层 | 发现 | 处置 |
|---|---|---|---|
| 1 | 事实 | §1 "RFC 动机: 版本绑定+包体积" — 0001-new-log-storage.md:24-28 实证 (**非**大日志放大 — 修正外部假设) | 通过 ✅ |
| 2 | 事实 | §1 "落地 = extends RocksDBLogStorage" — RocksDBSegmentLogStorage.java:65 | 通过 ✅ |
| 3 | 事实 | §2 "4K 阈值" — L157-161 (系统属性可配) | 通过 ✅ |
| 4 | 事实 | §2 "16B 元数据 = magic+reserved+firstLogIndex 8B+wrotePos 4B" — encodeLocationMetadata L1109-1116, LOCATION_METADATA_SIZE=16 L148 | 通过 ✅ |
| 5 | 事实 | §2 "写屏障 onDataAppend→put→joinAll→doSync" — RocksDBLogStorage.java:517-523 | 通过 ✅ |
| 6 | 事实 | §3 "预分配 ≥2 段 + 生产者-消费者" — L67 (PRE_ALLOCATE_SEGMENT_COUNT=2), L399-416, L646-656 | 通过 ✅ |
| 7 | 事实 | §3 "新文件 1G 映射" — maxSegmentFileSize L153-155 + loadNewFile L474-507 | 通过 ✅ |
| 8 | 事实 | §4 "异步 memcpy + wrotePos/committedPos" — SegmentFile.java:738-772, L838-865; writeExecutor L300-304 | 通过 ✅ |
| 9 | 事实 | §5 "checkpoint 5s + abort + 脏尾截断" — L759-765, L471-475, SegmentFile.java:634-711 | 通过 ✅ |
| 10 | 事实 | §5 "corrupted 仅最后文件 + 改名保留" — L623-641 | 通过 ✅ |
| 11 | 事实 | §6 "段内截断查 RocksDB 元数据" — L979-1042 + clear 64B 洞 SegmentFile.java:445-462 | 通过 ✅ |
| 12 | 事实 | §7 "SPI priority: Bolt 0 / gRPC 1" — GrpcRaftRpcFactory.java:42-43; META-INF/services 两文件实证 | 通过 ✅ |
| 13 | 事实 | §7 "核心 7 + CLI 11 处理器" — RaftRpcServerFactory.java:122-147 穷举 (核心: AppendEntries/GetFile/InstallSnapshot/RequestVote/Ping/TimeoutNow/ReadIndex; CLI: AddPeer/RemovePeer/ResetPeer/ChangePeers/GetLeader/Snapshot/TransferLeader/GetPeers/AddLearners/RemoveLearners/ResetLearners) | 通过 ✅ |
| 14 | 事实 | §8 "MpscSingleThreadExecutor per peer + seq 排序投递" — AppendEntriesRequestProcessor.java:235-262, L311-348 | 通过 ✅ |
| 15 | 事实 | §9 "gRPC _call 单方法" — GrpcRaftRpcFactory.java:45, L138; MarshallerRegistry L58-72 | 通过 ✅ |
| 16 | **事实修正** | 任务提示 "LogStorageFactory" **不存在** — LogStorage 创建由 JRaftServiceFactory.createLogStorage 承担 (DefaultJRaftServiceFactory.java:48-52) | ✅ KP 已按实际写 (无 LogStorageFactory 引用) |
| 17 | 结构 | 负面空间 "不做完全 Java 化" 与 RFC 蓝图自洽 (蓝图未落地是事实) | 通过 ✅ |

**结论**: 17 项核对 0 修正 (1 项外部假设被源码推翻: LogStorageFactory 不存在)。🟡 B 方案完成 (无时空溯源/极简复现, 符合 04 决策树)。
