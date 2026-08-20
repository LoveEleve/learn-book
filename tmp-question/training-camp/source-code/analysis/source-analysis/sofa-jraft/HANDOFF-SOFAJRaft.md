# HANDOFF — SOFAJRaft 源码分析交接文档 (6/6 域收官, REVIEW 扩域)

> **日期**: 2026-08-15 | **版本**: SOFAJRaft 1.4.1 (pom.xml 实证; 移植自百度 braft C++)
> **给新 AI**: 本文是 SOFAJRaft 阶段的**唯一入口**。所有域已交付 (KP + 大纲 + 提问 + 深审 + 极简复现 harness)。
> **源码**: `/data/workspace/source-code/code/spring/sofa-jraft` | **规划**: SOFAJRaft-PLAN.md (09 审计 v1)

---

## §零 状态速查

| 域 | 级别 | 方案 | 大纲节数 | 提问 | harness | 状态 |
|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| J-1 RAFT 核心循环 | 🔴 | A | 11 | 33 | MiniJRaft **6/6** | ✅ |
| J-2 日志复制 | 🔴 | A | 13 | 33 | MiniReplicator **5/5** | ✅ |
| J-3 快照压缩 | 🔴 | A | 8 | 30 | MiniSnapshot **5/5** | ✅ |
| J-4 成员变更 | 🔴 | A | 7 | 30 | MiniMembership **5/5** | ✅ |
| J-5 存储与 RPC | 🟡 | B | 9 | 32 | (🟡 无) | ✅ |
| **J-6 并发与定时器基础设施** (REVIEW 扩域) | 🔴 | A | 8 | 29 | MiniTimeWheel **5/5** | ✅ |
| **合计** | 5🔴+1🟡 | | 56 节 | 187 问 | **26/26** | **6/6 收官** |

**环境**: Java 21; harness 全为纯逻辑极简复现 (01 §极简复现 要求), 无外部依赖可离线运行。

---

## §一 09 怀疑审计结论 (详细见 SOFAJRaft-PLAN.md)

- **域清单 5 个全接受** (J-1~J-5), rheakv (366 文件嵌入式 KV) **排除** (应用层, 非 RAFT 核心)
- **修正 2 处**: J-3 类名 SnapshotExecutorImpl (非 SnapshotExecutor); J-5 补充新存储 RocksDBSegmentLogStorage (1214, RFC-0001)
- **补充 4 处**: preVote 实证 / ReadIndex 实现类 ReadOnlyServiceImpl (473) / braft 来源 / Seata S-8 依赖 sofa-jraft
- **外部假设被源码推翻 3 处** (探索期发现, 已写入各域): ConfigurationChangeContext 不存在 (实为 ConfigurationCtx 内部类) / isConfChangeAllowed 不存在 / LogStorageFactory 不存在

---

## §二 拓扑与依赖

**J-1 → J-2 → J-3 → J-4 → J-5**

- J-2 依赖 J-1 (leader 语义/ballotBox.resetPendingIndex)
- J-3 依赖 J-2 (日志模型/truncatePrefix)
- J-4 依赖 J-1+J-2+J-3 (选举约束/waitCaughtUp/快照配置)
- J-5 支撑面 (LogStorage 接口被 J-2/J-3 消费)

## §三 关键机制速查

### J-1 核心循环
| 机制 | 锚点 |
|---|---|
| 8 态枚举 + isActive | State.java:25-38 |
| 随机选举超时 [1000,2000) | NodeImpl.java:893-895 |
| 心跳 = electionTimeout/10 = 100ms | NodeImpl.java:889-891 |
| 预投票不增任期 + lease 拒绝 | NodeImpl.java:2787-2847, 1802-1807 |
| 先持久化 votedFor 再拉票 + ABA 防御 | NodeImpl.java:1190-1208 |
| 投票三条件 (term/日志新旧/votedId) | NodeImpl.java:1875-1951 |
| 本任期提交能力 + 首条 conf 日志 | NodeImpl.java:1300-1307 |
| stepDown + TimeoutNow 传位 | NodeImpl.java:1312-1370, 3388-3431 |
| checkQuorum + lease 一体 | NodeImpl.java:2329-2439, 1847-1862 |
| ReadIndex 三模式 (Safe/Lease/单节点) | NodeImpl.java:1611-1733 |
| pendingNotifyStatus TreeMap | ReadOnlyServiceImpl.java:203-205 |
| FSM 单线程 + 攒批 32 | FSMCallerImpl.java:399-478 |

### J-2 日志复制
| 机制 | 锚点 |
|---|---|
| 5 态 + Probe 退化路径 | Replicator.java:161-167, 810 |
| nextIndex 两段式回退 | Replicator.java:1493-1509 |
| 流水线 256 在途 + seq 排序 + version 代际 | Replicator.java:597, 1309-1323 |
| 心跳 = 空 AppendEntries (超时 half) | Replicator.java:788-806 |
| 日志 RPC 超时 -1 / block 重试 | Replicator.java:1691, 1028-1053 |
| 压缩判定 prevLogTerm==0 | Replicator.java:1556 |
| 快照触发 3 点 + URI 传输 | Replicator.java:776-784, 1631-1652, 649 |
| learner 不参与计票 | Replicator.java:1532-1535 |
| BallotBox 连续提交 + 锁外回调 | BallotBox.java:99-143 |
| 双 quorum joint | Ballot.java:80,89,144-146 |
| 双写 (内存立即+磁盘异步) | LogManagerImpl.java:326-382 |
| AppendBatcher 256/256KB | LogManagerImpl.java:465-519 |
| 冲突截断 + appliedId 底线 | LogManagerImpl.java:1045-1105, 1025-1029 |
| 快照截断不推进 diskId | LogManagerImpl.java:652-659 (braft#224) |
| v2 三改进 (learners/checksum/protobuf) | log.proto:17-19 |
| 8B key + 双 CF | RocksDBLogStorage.java:474-478, 227-229 |

### J-3 快照
| 机制 | 锚点 |
|---|---|
| 快照任务与 apply 同队列串行 | FSMCallerImpl.java:201-210 |
| temp → 原子改名 + 引用计数 | LocalSnapshotStorage.java:247-261, 262-270 |
| remote:// URI + FileService | LocalSnapshotReader.java:144-151 |
| 硬链接复用 (checksum 相同) | LocalSnapshotCopier.java:254-328 |
| 断点续传 (offset/count + EAGAIN 不消耗) | CopySession.java:280-282, 233 |
| 同步阻塞式安装 (下载完才响应) | SnapshotExecutorImpl.java:526, 506 |
| 截断三分支 | LogManagerImpl.java:661-682 |
| 安装期间拒日志 EBUSY | NodeImpl.java:2074-2080 |

### J-4 成员变更
| 机制 | 锚点 |
|---|---|
| ConfigurationCtx 四阶段 | NodeImpl.java:332-538 |
| 追平期限 = 选举超时 | NodeImpl.java:408-411 |
| 两条日志 (JOINT+STABLE) | NodeImpl.java:509-518 |
| 并发 EBUSY + 幂等相等 | NodeImpl.java:2516-2527 |
| 回滚 = 不落盘 + flush 重写 | NodeImpl.java:448, 490-504 |
| 变更中禁转移 | NodeImpl.java:3323-3340 |
| learner 提升才追平 | CliServiceImpl.java:355-362 |
| isStable = oldConf 空 | ConfigurationEntry.java:77-79 |

### J-5 存储与 RPC
| 机制 | 锚点 |
|---|---|
| 4K 阈值双介质分流 | RocksDBSegmentLogStorage.java:157-161, 1077-1095 |
| 16B 位置元数据 | RocksDBSegmentLogStorage.java:1109-1116 |
| 预分配生产者-消费者 (≥2 段) | RocksDBSegmentLogStorage.java:399-416, 646-656 |
| 异步写 wrotePos/committedPos | SegmentFile.java:738-772, 838-865 |
| checkpoint+abort+脏尾截断 | RocksDBSegmentLogStorage.java:759-765, 471-475 |
| SPI priority Bolt0/gRPC1 | GrpcRaftRpcFactory.java:42-43 |
| 核心 7 + CLI 11 处理器 | RaftRpcServerFactory.java:122-147 |
| per-peer Mpsc + seq 排序投递 | AppendEntriesRequestProcessor.java:235-262, 311-348 |
| gRPC _call 单方法 | GrpcRaftRpcFactory.java:45, 138 |

## §四 harness 实证发现 (极简复现, 全部离线)

1. **投票规则**: 日志更新可投/相等可投/落后拒投 (MiniJRaft A) — LogId.compareTo 字典序实证
2. **一任期一票**: 同任期第二候选人拒投, 新任期再投 (MiniJRaft B)
3. **preVote lease**: lease 有效拒绝 / 过期放行 (MiniJRaft C)
4. **随机超时收敛**: 3 节点最终唯一 leader (MiniJRaft D)
5. **冲突回退两段式**: 落后跳至 lastLogIndex+1 / 冲突逐条减 (MiniReplicator A)
6. **乱序消费**: 先到 3 后到 1 → 已消费 1, 补齐 2 后消费 3; 旧 seq 拒绝 (MiniReplicator B)
7. **连续计票**: 缺中间票停住, 补票后推进 (MiniReplicator C)
8. **learner 不投票**: 确认不影响 quorum (MiniReplicator D)
9. **joint 双 quorum**: 新配置满足但旧配置不满足 → 不提交 (MiniReplicator E)
10. **截断三分支**: 全截/保守截到上次快照点/reset (MiniSnapshot C)
11. **ELEADERREMOVED**: 合并后 leader 被移出新配置 → 让位 (MiniMembership E)

## §五 遗留与待办

- [ ] 大纲→正式文章写作 (另行启动, 与 ZAB (Z-1/Z-2) 对照着写)
- [ ] 阶段5 Nacos NC-4 JRaftProtocol 对照面 (消费 J-1~J-5)
- [ ] Obsidian 知识图谱 (全局待办)
- [ ] Seata S-8 SessionMode.RAFT 交叉引用回填 (依赖 sofa-jraft)

## §六 文件路径

```
analysis/source-analysis/sofa-jraft/
├── SOFAJRaft-PLAN.md (09 审计表 + 拓扑)
├── knowledge-planning/ (j1~j5 共 5 个 KP)
├── outlines/
│   ├── j1-core-loop/ (outline 11 节 + 33 问 + review 21 项 + temporal-trace)
│   ├── j2-replication/ (13 节 + 33 问 + review 22 项)
│   ├── j3-snapshot/ (8 节 + 30 问 + review 19 项)
│   ├── j4-membership/ (7 节 + 30 问 + review 18 项)
│   ├── j5-storage-rpc/ (9 节 + 32 问 + review 17 项)
│   └── j6-concurrency/ (8 节 + 29 问 + review 17 项, REVIEW 扩域)
└── harness/
    ├── j1-miniraft/ (MiniJRaft.java 6/6)
    ├── j2-minireplicator/ (MiniReplicator.java 5/5)
    ├── j3-minisnapshot/ (MiniSnapshot.java 5/5)
    ├── j4-minimembership/ (MiniMembership.java 5/5)
    └── j6-minitimewheel/ (MiniTimeWheel.java 5/5)
```

**运行**: `javac Xxx.java && java Xxx` (纯 JDK, 无依赖)。

## §七 完成检查单

- [x] 顶层包扫描 ↔ 域清单覆盖矩阵 (5/5 + rheakv 排除理由)
- [x] 09 审计 2 修正 + 4 补充 + 3 外部假设推翻
- [x] **6/6 域交付 (REVIEW 扩域)**: KP + 大纲 (56 节) + 提问 (187 问, 每域 5 身份) + 六层深审 (114 项核对) + 时空溯源 (J-1)
- [x] **REVIEW 修复 2026-08-15**: ① 最大发现 — MiniJRaft.LogId.compareTo 与源码相反 (源码 term-first, LogId.java:94-103; harness 误写 index-first, 因用例 term 全同侥幸通过) → harness/outline/review-notes 4 处修正 ② J-4 锚点密度 3→30 ③ 标题 3 处 ④ 数字 44→48 节 ⑤ harness 21/21 重跑确认 (详见 REVIEW-2026-08-15.md)
- [x] 极简复现 harness 5 个 **26/26 全 PASS** (🔴 域强制, 符合 04 方案 A)
- [x] 每域负面空间 3-7 条
- [ ] 大纲文章写作 (下一阶段)
