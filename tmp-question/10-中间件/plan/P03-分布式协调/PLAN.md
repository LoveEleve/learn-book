# P03：分布式协调与共识 — 详细规划

> Phase: P03 | 周期: 3周 | 书: 1本(MinerU) + 1本(需补)
> 依赖: P02（Kafka 的 KRaft、RocketMQ DLedger 都基于 Raft 共识）
> 学习模式: **Raft(Etcd) vs ZAB(ZK) 双共识协议对比**

---

## 一、为什么这一站是"理论落地层"

P02 学了两个 MQ，看到了共识算法的应用（Kafka KRaft、RocketMQ DLedger），但始终是在用别人的封装。

P03 的目标：**从源码层面理解 Raft 和 ZAB 如何实现**——不只是"选举→日志复制→提交"的纸上流程图，而是 Java/Go 代码里 `nextIndex`、`matchIndex`、`commitIndex`、`lastApplied` 这些变量到底怎么变化。

**理解共识算法源码 = 中间件专家的分水岭。**

---

## 二、书单

### 2.1 《Etcd源码解析》（侯宜军）

| 维度 | 信息 |
|------|------|
| MinerU 路径 | `10-中间件-消息队列/Etcd源码解析.../full.md` |
| MinerU 质量 | ✅ 6003行，干净 Markdown |
| 语言 | Go |
| 定位 | Raft 共识算法的工业级实现 + MVCC 存储 |

| 章 | 核心知识点 | 学习深度 |
|----|-----------|---------|
| 第1章 Etcd简介 | Etcd vs ZK、使用场景、读写性能、概念词汇表 | 概念层 |
| 第2章 Raft协议 | **领导人选举/日志复制/安全性/集群成员变化/日志压缩/客户端交互** | **深度精读** |
| 第3章 操作实践 | 安装、Etcdctl、HTTP API、集群管理 | 实操层 |
| 第4章 源码分析准备 | Go 语言特性（goroutine/channel/接口）、环境搭建 | 准备层 |
| 第5章 客户端��码 | KV/集群配置/Watcher 的 HTTP 编程接口 | 使用层 |
| 第6章 Http服务端 | 服务端总架构、KV/Watcher 接口实现 | 使用层 |
| 第7章 Etcd服务端实现 | **Etcd启动、EtcdServer接口/实体/初始化/主循环、状态机(raftNode/node)** | **深度精读** |
| 第8章 集群间通信 | **Raft设计思路（任期/工作模式）、消息类型（MsgProp/MsgApp/MsgHeartbeat）、选举流程** | **深度精读** |
| 第9章 数据存储 | **内存数据(载入/保存/Get/Set)、事务和快照（文件命名/快照流程/事务日志）** | **深度精读** |

**精读章（4章）**：Ch2 协议、Ch7 服务端、Ch8 通信、Ch9 存储

### 2.2 《从 Paxos 到 ZooKeeper》（倪超）— 需补书

| 维度 | 信息 |
|------|------|
| 状态 | ❌ MinerU 无源，需自行获取 |
| 定位 | ZAB 共识协议的经典教材 |
| Tier | T1 源码+使用 |

**预期学习要点**：

| 主题 | 核心知识点 |
|------|-----------|
| Paxos 基础 | Basic Paxos → Multi-Paxos 推导（为什么需要 Multi-Paxos？） |
| ZAB 协议 | 崩溃恢复(Leader选举)、消息广播(2PC简化)、与 Raft 的差异 |
| ZK 数据模型 | ZNode 类型(PERSISTENT/EPHEMERAL/SEQUENTIAL)、Watch 机制 |
| ZK 会话管理 | Session 超时、心跳、分桶策略 |
| ZK 集群 | Leader/Follower/Observer 角色、ZAB 协议同步流程 |
| 典型应用 | 分布式锁、选主、配置中心、命名服务 |
| 源码分析 | `QuorumPeer.java`(启动)、`FastLeaderElection.java`(选举)、`Leader.java`(提议处理)、`FollowerZooKeeperServer.java`(日志同步) |

---

## 三、Raft vs ZAB 对比矩阵

| 维度 | Raft (Etcd) | ZAB (ZooKeeper) |
|------|------------|-----------------|
| **选举** | 随机超时 + RequestVote RPC（Candidate 发起） | Fast Leader Election（FLE），基于 epoch+zxid 比较 |
| **日志复制** | AppendEntries RPC（Leader → Follower），单向推 | 两阶段：Proposal → Ack → Commit，单向推 |
| **日志语义** | 每个 entry 包含 (term, index, command) | 每个 entry 包含 (epoch, zxid, data) |
| **读一致性** | ReadIndex（线性一��性读）+ LeaseRead | Sync 操作强制同步 Leader 最新状态 |
| **成员变更** | Joint Consensus（两步变更） | 通过 Leader 协调，逐步切换 |
| **写路径** | Leader 接收 → 写 WAL → 复制到多数派 → Apply → 返回 | Leader 接收 → 发起 Proposal → 等待多数 Ack → Commit → 返回 |
| **实现语言** | Go (etcd-io/raft library) | Java (Apache ZooKeeper) |
| **Watcher机制** | long-polling watch（一次触发后失效） | 同（单次触发，需重新注册） |
| **存储引擎** | BoltDB (bbolt) —— 基于 B+树 | 内存 DataTree + 事务日志 + 快照 |
| **与MQ的关系** | RocketMQ 5.x DLedger 基于 Raft | Kafka 旧版依赖 ZK（3.x 去 ZK 用 KRaft） |

---

## 四、源码阅读清单

### 4.1 Etcd 源码（Go）

| 源码模块 | 关键文件/函数 | 阅读目的 |
|---------|-------------|---------|
| Raft 状态机 | `raft/raft.go` → `Step()`, `tick()`, `becomeLeader()`, `becomeFollower()` | Raft 角色转换与状态推进 |
| 选举 | `raft/raft.go` → `campaign()`, `sendVoteRequest()` | RequestVote 完整流程 |
| 日志复制 | `raft/raft.go` → `bcastAppend()`, `handleAppendEntries()` | AppendEntries 的发送与处理 |
| 日志存储 | `raft/log.go` → `append()`, `commitTo()`, `unstable` | 日志存储结构（unstable + Storage 分层） |
| Progress追踪 | `raft/progress.go` → `nextIndex`, `matchIndex`, `StateProbe/Replicate/Snapshot` | Leader 如何追踪每个 Follower 的复制进度 |
| 节点接口 | `raft/node.go` → `node.run()`, `Ready` channel | 应用层如何消费 Raft Ready 事件 |
| EtcdServer | `etcdserver/server.go` → `run()` | etcd 如何对接 raft node |
| MVCC 存储 | `mvcc/kvstore.go`, `mvcc/watchable_store.go` | 多版本存储 + Watch 机制 |
| WAL | `wal/wal.go` | 预写日志实现 |
| 快照 | `etcdserver/api/snap/snapshotter.go` | 快照生成与应用 |

### 4.2 ZK 源码（Java，待补书后）

| 源码模块 | 关键类 | 阅读目的 |
|---------|--------|---------|
| 启动 | `QuorumPeerMain.java` → `QuorumPeer.java` | ZK 集群节点启动全流程 |
| 选举 | `FastLeaderElection.java` | FLE 算法实现 |
| Leader | `Leader.java` → `LeaderZooKeeperServer.java` | 提议处理与 COMMIT |
| Follower | `Follower.java` → `FollowerZooKeeperServer.java` | 日志同步与 Learner 角色 |
| ZAB 协议 | `Zab1_0Thread.java` | 消息广播与恢复 |
| 存储 | `FileTxnSnapLog.java`, `DataTree.java` | 事务日志 + 内存数据树 |
| 会话 | `SessionTrackerImpl.java` → 分桶策略 | 会话管理与超时检测 |
| Watch | `WatchManager.java` | 事件触发机制 |

---

## 五、关键设计哲学

### Raft 核心设计决策

| 设计决策 | 反事实假设 | 为什么选这个 |
|---------|-----------|-------------|
| **为什么 Raft 强调"领导权唯一"（Strong Leader）？** | 无主或多主设计 | 所有决策由 Leader 统一发起——日志流方向唯一（Leader→Follower），简化一致性推理 |
| **为什么日志 entry 只在 Leader 任期单调递增？** | 多任期混杂 | Log Matching Property（如果两个日志在相同 index 和 term 一致，则之前全部一致）——这是 Raft 正确性的核心 |
| **为什么成员变更用 Joint Consensus 两步？** | 原子一步切换 | 防止新旧配置并存时出现双 Leader——两步达成"从旧配置提交到新配置"的安全过渡 |
| **为什么用 ReadIndex 而不是直接读 Leader？** | Leader 直接返回本地数据 | 网络分区可能产生旧 Leader，ReadIndex 用心跳确认自己还是 Leader 后才返回——保证线性一致性读 |

### ZAB 核心设计决策

| 设计决策 | 反事实假设 | 为什么选这个 |
|---------|-----------|-------------|
| **为什么 ZAB 选举比 Raft 复杂（FLE 而非简单超时）？** | 简单超时选举 | ZK 追求选出一个"有最新事务日志"的 Leader——FLE 通过 epoch+zxid 比较，确保不丢已提交的事务 |
| **为什么 ZK 的写必须由 Leader 发起？** | Follower 也可接写 | 保证所有写操作的总顺序一致——这是 ZK 提供顺序一致性的基础 |
| **为什么 Watch 是一次性的？** | 持续的观察 | 简化服务端状态管理（不需要为每个 watch 维护长生命周期），同时避免 watch 风暴——客户端自己重注册 |

---

## 六、生产陷阱

| # | 陷阱 | 中间件 | 场景 | 修复 |
|---|------|--------|------|------|
| 1 | **ZK Session 超时 → 全部临时节点被删** | ZK | 网络闪断导致 Session 超时 → 分布式锁全部释放 → 多个服务同时获取锁 → 数据竞争 | 合理设置 sessionTimeout + 业务层加"续约锁" |
| 2 | **ZK 脑裂** | ZK | 网络分区 → 两边各自选出 Leader → 数据不一致 | ZK 3.5+ 默认支持动态重配置 |
| 3 | **etcd 集群过半节点宕机 → 不可用** | Etcd | 3 节点集群宕 2 台 → 不能达成多数派 → 写入阻塞 | 规划 5 节点集群，允许宕 2 台 |
| 4 | **etcd mvcc 空间爆炸** | Etcd | 频繁写入不 compact → mvcc 保留所有历史版本 → 磁盘耗尽 | 开启自动 compact（`--auto-compaction-mode=periodic --auto-compaction-retention=1h`） |
| 5 | **ZK watch 遗漏** | ZK | watch 触发后忘记重新注册 → 后续变更感知不到 | 使用 Curator 的 NodeCache/PathChildrenCache 自动重注册 |
| 6 | **Commit 索引不推进（etcd apply 慢）** | Etcd | 应用层处理 Ready 慢 → committed 的 entry 一直不 apply → 后续写入阻塞 | 监控 `etcd_disk_backend_commit_duration_seconds` |

---

## 七、面试 Q&A

| 问题 | 面试官意图 | 回答主线 |
|------|----------|---------|
| "Raft 和 Paxos 有什么区别？" | 考察是否只背了概念，有没有深入理解 | Raft 强调可理解性——Strong Leader + 日志匹配 + 成员变更；Paxos 更泛化但 Multi-Paxos 等价于 Raft |
| "ZAB 和 Raft 的核心差异？" | 考察是否只知 Raft 不知 ZAB | ZAB 选举优先选数据最新的，Raft 选举优先选 term 最新的——这是设计哲学差异 |
| "etcd 的线性一致性读是如何实现的？" | 考察是否理解"读"的复杂性 | ReadIndex——与多数节点心跳确认自己还是 Leader，等待 commitIndex ≥ readIndex，然后返回 |
| "ZK 为什么不适合作为服务注册中心？" | 考察对 CP 系统局限性的理解 | CP 不能牺牲可用性——网络分区时 ZK 拒绝写入，注册中心(AP)需要允许写入 |
| "Raft 日志的 committed vs applied 有什么区别？" | 考察状态机概念 | committed = 多数节点已持久化（安全不丢），applied = 应用层状态机已执行（对客户端可见） |

---

## 八、章节依赖

```
上游：
  P02 消息队列：Kafka KRaft / RocketMQ DLedger 的 Raft 实现 → 作为 P03 的动机

下游：
  P04 分布式存储：2PC/Paxos → 在 P03 Raft/ZAB 基础上理解共识算法全景
  P06 ShardingSphere：ZooKeeper 用于 ShardingSphere 的注册中心模式
```

---

## 九、产出物（Phase C）

```
md/P03-Etcd/
├── D-01-Raft选举与日志复制.md             # campaign → RequestVote → bcastAppend → commitTo
├── D-02-EtcdServer主循环与状态机.md       # node.run() → Ready channel → apply 链路
├── D-03-MVCC存储与Watch机制.md             # kvstore → watchableStore → BoltDB 事务
├── D-04-快照与日志压缩.md                  # snapshotter → compact → 从快照恢复

md/P03-ZooKeeper/（待补书后产出）
├── D-01-ZAB崩溃恢复与消息广播.md          # FLE选举 → Proposal → Ack → Commit
├── D-02-ZK会话管理与Watch机制.md           # SessionTracker → 分桶 → 超时检测
├── D-03-ZK典型应用源码分析.md              # 分布式锁/选主/配置中心的 ZK 实现

md/P03-对比/
├── D-01-Raft-vs-ZAB-协议对比.md            # 6 维度对比矩阵 + 各自适用场景
└── D-02-共识算法面试Q&A.md                 # 5道题 + 面试官意图分析
```

## 十、学习检查清单

- [ ] 能手写 Raft `RequestVote` 和 `AppendEntries` 的伪代码（包含 term/index/preLogIndex/preLogTerm 校验）
- [ ] 能解释 `nextIndex` 和 `matchIndex` 在日志复制过程中如何变化（画一个 3 节点集群的渐进图）
- [ ] 能写出 Raft Joint Consensus 两步成员变更的状态迁移
- [ ] 能对比 ZAB FLE 选举和 Raft 选举的差异，并解释为什么 ZAB 要优先选事务最新的节点
- [ ] 能画出 Etcd `Ready` channel 到 BoltDB 的完整数据流
- [ ] 能写出 etcd mvcc 中 `revision`、`create_revision`、`mod_revision` 三个版本号的含义和更新规则
