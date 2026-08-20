# SOFAJRaft — 知识网络化规划 (J-1~J-5, 09 怀疑审计后 v1)

> **日期**: 2026-08-15 | **依据**: issue/源码分析执行计划.md 阶段4.6 (5 域) + 09 对既有规划保持怀疑 全量重审
> **源码**: `/data/workspace/source-code/code/spring/sofa-jraft` (**SOFAJRaft 1.4.1**, pom.xml:34 实证; jraft-core 279 主源文件 + jraft-extension 2 模块 + jraft-rheakv 366 文件)
> **定位**: 阶段 4.6 — 消息与事务第八环 **RAFT 一致性算法 Java 生产级实现 (移植自百度 braft C++)**
> **知识网络**: 与 ZooKeeper (4.3, ZAB vs Raft 对照) + Curator (4.5, 配方对照) + Kafka KRaft (4.2, 元数据协调对照) + Seata S-8 (SessionMode.RAFT 依赖 sofa-jraft!) 互联

---

## 〇、09 怀疑审计表 (SOFAJRaft, 2026-08-15) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| **域清单 5 个** (J-1~J-5) | 顶层包扫描 (jraft-core 279 文件 + extension + rheakv) | core/ (NodeImpl 3701/Replicator 1930/FSMCallerImpl 801/ReadOnlyServiceImpl 473) + storage/ (LogManagerImpl 1254/SnapshotExecutorImpl 780/RocksDBLogStorage 769) + rpc/ (52) + conf/ (3) — 5 面全覆盖; 未覆盖: **rheakv (366 文件, 嵌入式 KV)**、util/ (工具层) | **接受** ✅ rheakv 排除 (见下) |
| J-1 "NodeImpl—Follower/preVote/Candidate/requestVote/Leader→ReadIndex线性一致读" | core/NodeImpl.java | NodeImpl (3701) 实证; **preVote 实证** (L645 调用/L2787-2798 实现); **ReadIndex 在 ReadOnlyServiceImpl (473)** — 执行计划未指明实现类 | **接受+补充** ✅ |
| J-2 "Replicator/LogManager—AppendEntries(nextIndex/matchIndex)/BallotBox.commitAt" | core/ + storage/impl/ | Replicator (1930) + LogManagerImpl (1254) + **BallotBox.commitAt 实证** (BallotBox.java:99) | **接受** ✅ |
| J-3 "SnapshotExecutor.doSave→InstallSnapshot/LogManager.truncatePrefix" | storage/snapshot/ | 实现类为 **SnapshotExecutorImpl (780)** (接口 SnapshotExecutor); LocalSnapshot 族 5 文件 (~1300 行) | **接受+类名修正** ✅ |
| J-4 "Joint Consensus(C_old,new→C_new)—addPeer/removePeer/Learner" | conf/ + core/ | **ConfigurationEntry.isStable (L77) + oldConf/newConf 双配置结构** (entity/RaftOutter.java:350-385); Learner 支持实证 (newLearners/oldLearners); CliServiceImpl (672) | **接受** ✅ (joint consensus 语义=双配置提交, 非数学并集) |
| J-5 "RocksDBLogStorage/BoltRaftRpcFactory/GrpcRaftRpcFactory" | storage/impl/ + rpc/ + extension/ | RocksDBLogStorage (769) **确认为默认** (DefaultJRaftServiceFactory.java:49-51); BoltRaftRpcFactory ✓; **GrpcRaftRpcFactory 在 extension/rpc-grpc-impl** ✓; **新存储 RocksDBSegmentLogStorage (1214, RFC-0001) + SegmentFile (903) 未在规划内** | **接受+补充** ✅ (新存储并入 J-5) |
| 版本 | pom.xml | **1.4.1** | **补充** ✅ |
| 来源 | README_zh | **百度 braft (C++) 移植** | **补充** ✅ |
| rheakv (366 文件, 未规划) | 设计决策测试 (00 §3) | 嵌入式分布式 KV 存储 (RheaKV 应用层) — 承载设计决策但为**独立应用模块** (多 group 分片 KV), 非 RAFT 算法核心; 执行计划定位 "RAFT 实现" 域 | **排除** ✂️ (附理由: 应用层, 如需要可后续单独成项目) |
| util/concurrent + util/timer (83 文件, 未规划) | 设计决策测试 (00 §3 定量预检) | **初判错误**: 误当"工具层"整体排除 — 违反 00 §3 (≥50 文件必须读关键类) 与反模式 6 (HashedWheelTimer 5 文件但时间轮是设计决策的明示案例)。补读后实证: **HashedWheelTimer (762, 三态 workerState+wheel 数组+论文引用)** / **MpscSingleThreadExecutor (401, MPSC+三态 CAS)** / **LongHeldDetectingReadWriteLock (153, 长持锁检测 — NodeImpl.writeLock 底座)** / **SegmentList (459, 128/段+firstOffset 缓存+estimatedBytes 内存预算 — LogManager 内存日志/BallotBox 数据结构)** / **FixedThreadsExecutorGroup (ExecutorChooser 轮询)** / RepeatedTimer (295, J-1 定时器底座) — 全部承载算法级设计决策 | **修正** ⚠ 新增 **J-6 并发模型与定时器基础设施** (Hub: 被 J-1~J-5 全部核心路径依赖) |
| Seata 依赖关系 (STAGE3 隐含) | 源码扫描 | Seata S-8 SessionMode.RAFT → **io.seata:seata-server 依赖 sofa-jraft** — 反向依赖已满足 | **接受** ✅ (交叉引用点: Seata 用 jraft 做 session 复制) |

**覆盖率报告**: 既有规划 5 域 → 重审后 **5 域** (100%, 无增删), 修正 **2 处** (J-3 类名 SnapshotExecutorImpl/J-5 补充新存储), 补充 **4 处** (preVote/ReadOnlyServiceImpl 实现类/rheakv 排除理由/来源 braft)。

---

## 一、入口点与主线

`RaftGroupService.start (jraft-example Counter 例) → NodeImpl (3701) → Node 状态机 (State 枚举/选举超时 TimerManager) → 成为 Leader → ReplicatorGroup (Replicator 1930) 日志复制 → LogManagerImpl (1254) 落盘 (RocksDBLogStorage/SegmentLogStorage) → BallotBox (294) 计票 → commitIndex → FSMCallerImpl (801) 应用状态机 → ReadOnlyServiceImpl (473) ReadIndex 线性一致读` — 快照面: `SnapshotExecutorImpl (780) → LocalSnapshotStorage → InstallSnapshot` — 成员面: `CliServiceImpl (672) → ConfigurationEntry 双配置`。

## 二、域清单 (5 域: 4🔴 + 1🟡)

| # | 域 | 模块 (文件行) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| J-1 | **RAFT 核心循环** | core/NodeImpl (3701) + State + TimerManager + ReadOnlyServiceImpl (473) + FSMCallerImpl (801) | 角色状态机/任期/选举超时/preVote/requestVote/投票规则/ReadIndex+LeaseRead 线性一致读/apply 队列 | 🔴 A |
| J-2 | **日志复制** | core/Replicator (1930) + ReplicatorGroupImpl (315) + BallotBox (294) + storage/impl/LogManagerImpl (1254) + entity/codec (v1/v2) | AppendEntries(nextIndex/matchIndex)/流水线复制/心跳/计票 commitAt/日志压缩 (truncatePrefix) | 🔴 A |
| J-3 | **快照压缩** | storage/snapshot/SnapshotExecutorImpl (780) + LocalSnapshot 族 (~1300) + InstallSnapshot RPC | doSave 时机/本地快照读写/远程安装/快照与日志截断联动/限流 | 🔴 A |
| J-4 | **成员变更** | core/CliServiceImpl (672) + conf/ (Configuration 3 文件) + NodeImpl 变更路径 | addPeer/removePeer/transferLeader/Learner/joint 双配置提交/变更中选举限制 | 🔴 A |
| J-5 | **存储与 RPC** | storage/impl/RocksDBLogStorage (769) + storage/log/RocksDBSegmentLogStorage (1214) + SegmentFile (903) + rpc/ (52 文件: Bolt 工厂/处理器) + extension/rpc-grpc-impl | 两种日志存储架构/RocksDB WAL/分片文件读写/RPC 请求处理器族 | 🟡 B |
| J-6 | **并发模型与定时器基础设施** (新增, Hub 升级) | util/concurrent (14 文件: MpscSingleThreadExecutor 401/LongHeldDetectingReadWriteLock 153/FixedThreadsExecutorGroup) + util/timer (7: HashedWheelTimer 762) + util/RepeatedTimer (295) + util/SegmentList (459) + ThreadPoolUtil (283) | 时间轮调度/MPSC 执行器/长持锁检测/分段列表数据结构/线程组轮询/定时器工厂 SPI | 🔴 A |

## 三、执行顺序 (拓扑: 状态机 → 复制 → 压缩 → 成员 → 支撑)

**J-1 → J-2 → J-3 → J-4 → J-5 → J-6**

> 拓扑理由: 核心状态机 (J-1) → 日志复制 (J-2) → 快照 (J-3) → 成员变更 (J-4) → 存储与 RPC (J-5) → 并发与定时器基础设施 (J-6, 收束: 被前 5 域全部核心路径依赖的 Hub, 时间轮/MPSC/分段列表为 J-1 定时器、J-2 流水线、J-5 处理器提供底座; 教学上置于最后做"地基回望", 依赖上全量被依赖无前向问题)。

## 四、知识网络图

```
← 复用: ZooKeeper ZAB (4.3 对照 Raft) + Kafka KRaft (4.2 元数据协调) + Curator (4.5 配方)
→ 引出: Seata S-8 SessionMode.RAFT (会话存储依赖) + Nacos CP 一致性 (JRaftProtocol 对照, 阶段5)
```

## 五、完成检查单

- [x] 顶层包扫描 ↔ 域清单覆盖矩阵 (5/5)
- [x] 09 审计: 修正 2 处 (J-3 类名/J-5 新存储), 排除 1 处 (rheakv), 补充 4 处 (preVote/ReadOnlyServiceImpl/braft 来源/Seata 依赖)
- [x] 数字断言穷举 (NodeImpl 3701/Replicator 1930/BallotBox 294/LogManagerImpl 1254/SnapshotExecutorImpl 780 实证)
- [x] **J-1 ✅ 2026-08-15** (harness MiniJRaft 6/6: 投票规则/一任期一票/preVote lease/随机超时收敛/本任期提交/多数派提交; 深审 21 项; 33 问/5 身份)
- [x] **J-1 ✅ 2026-08-15** (harness MiniJRaft 6/6)
- [x] **J-2 ✅ 2026-08-15** (harness MiniReplicator 5/5: 冲突回退两段式/乱序消费/连续计票/learner 不投票/joint 双 quorum; 深审 22 项; 33 问)
- [x] **J-3 ✅ 2026-08-15** (harness MiniSnapshot 5/5: 冻结/原子改名/截断三分支/引用计数/硬链接复用; 深审 19 项; 30 问)
- [x] **J-4 ✅ 2026-08-15** (harness MiniMembership 5/5: isStable/四阶段/双quorum/追平回滚/ELEADERREMOVED; 深审 18 项; 30 问; 修正 2 外部假设: ConfigurationChangeContext 与 isConfChangeAllowed 均不存在)
- [x] **J-5 ✅ 2026-08-15** (🟡 B 方案: 深审 17 项; 32 问; 修正 1 外部假设: LogStorageFactory 不存在)
- [x] **J-6 ✅ 2026-08-15** (REVIEW 扩域, Hub 🔴A: harness MiniTimeWheel 5/5: 桶定位/到期精度/重复调度/随机化/绕轮; 深审 17 项; 29 问)
- [x] **阶段 4.6 全量收官 6/6** (REVIEW 扩域后): 56 节大纲 + 187 问 + 深审 114 项 + harness 5 个 **26/26 断言全 PASS** (J-1 6/6 + J-2 5/5 + J-3 5/5 + J-4 5/5 + J-6 5/5)
