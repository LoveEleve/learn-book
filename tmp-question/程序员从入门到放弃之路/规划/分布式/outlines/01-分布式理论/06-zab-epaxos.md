# ZooKeeper为什么不用Raft, 也不直接用Paxos? — ZAB与EPaxos的两种进化

> Cluster B: 14 KPs | 依赖: 04-Paxos + 05-Raft(已知两种共识核心) | 读者基线: 理解Paxos/Raft基本差异

---

### 1. ZAB — ZooKeeper专用共识协议
  ZK用了10年都没出过数据丢失事故 — ZAB到底特殊在哪?
  - B2 Ch5 §1: ZAB四个阶段 — Leader Election(选主) → Discovery(发现, 同步历史) → Synchronization(同步, 确保Follower追上) → Broadcast(广播, 正常处理请求)
  - 关键设计: ZAB的"原子广播" — 给每个事务分配全局递增的zxid(epoch+64位counter) — 所有Follower按zxid顺序apply — 保证"所有事务以相同顺序被处理"
  - B1 Ch7 §2: ZK的关键设计 — 顺序一致性(客户端请求按发送顺序执行) + 原子性(事务要么全节点执行要么全不执行) + 单一系统映像(客户端无论连哪个ZK看到的视图相同)
  - B2 Ch5 §2: 与Paxos/Raft的区别 — ZAB不保证"最新日志才能当选"(Paxos-style), 而是保证"多数派同步后才开放服务"(类似ZAB独有的epoch边界)

### 2. ZK的请求处理器链 — 一个写请求的源码路径
  客户端连ZK, 执行create /path data — 从收到请求到persist到磁盘, 经历了什么?
  - B2 Ch5 §3: FastLeaderElection — 节点投zxid最大者(自己的log如果最新就投自己) → 过半→Leader
  - RequestProcessor链: PrepRequestProcessor(生成txn) → SyncRequestProcessor(写事务日志+Snapshot) → ProposalRequestProcessor(发起Proposal) → CommitProcessor(等过半Follower的ACK后Commit) → FinalRequestProcessor(Apply到内存DataTree)
  - 关键设计: CommitProcessor的双队列 — committedRequests(已过半可apply) + queuedRequests(等待commit) — 保证"先提交的事务先apply"
  - ZK的watch机制: 客户端注册watch到ZNode → 该ZNode变化→ZK主动通知 → 单次触发(需要重新注册) — 一个"分布式广播通知"的原语

### 3. EPaxos — 不需要Leader的共识
  Raft需要选主, ZAB也需要选主 — 有没有不需要Leader的共识?
  - B2 Ch9 §1: EPaxos设计 — 每个命令独立执行共识(不是按日志顺序) → 只对冲突的命令建立依赖关系 → 非冲突命令可以乱序提交
  - 关键设计: PreAccept — Proposer向多数派Replica发PreAccept → Replica记录依赖图(哪些命令和这个命令冲突) → 返回Reply → Proposer选最终依赖→Commit
  - 强连接vs弱连接: 如果所有Replica的依赖图一致(强连接) → 命令可直接Commit; 如果不一致(弱连接) → 需要额外Accept轮
  - B2 Ch9 §2: EPaxos去中心化优势 — 高吞吐(冲突少时乱序执行) + 跨数据中心(每个DC本地Replica可执行本地命令) — 代价: 依赖图维护复杂

### 4. 三种共识协议何时选哪个?
  - Raft: 标准场景(etcd/consul/TiKV) — 好理解/好调试/好维护
  - ZAB: 需要ZK生态时的唯一选择(协调服务/分布式锁/命名服务) — 与ZK深度绑定
  - EPaxos: 高并发写入+冲突率低+需要跨DC性能(新一代分布式数据库如CockroachDB/Spanner-like系统) — 复杂度高

### 5. 收束 — 共识算法没有银弹
  - ZAB = Paxos + zxid全序 + ZK紧密集成 → 为协调服务优化
  - EPaxos = 去掉Leader + 依赖图 + 冲突检测 → 为高吞吐并发写优化
  - 共识协议的选择取决于: 是否需要强Leader语义(主站式/ZK)/是否需要跨数据中心(EPaxos)/团队能否承受复杂度

---

### 核心悬念
**"Basic Paxos太慢, Multi-Paxos/Raft/ZAB都需要Leader — 有没有人想给Paxos'加速'或'变形'? 7种Paxos变种各自卖的是什么药?"**

→ 引出 Paxos家族全览: Fast/Cheap/Disk/Generalized/Flexible Paxos (07-paxos-variants)
