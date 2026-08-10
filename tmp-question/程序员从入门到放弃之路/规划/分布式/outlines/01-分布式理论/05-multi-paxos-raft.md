# Paxos论文读了3遍没看懂, Raft的动画10分钟就理解了 — 为什么Raft比Paxos更简单?

> Cluster B: 16 KPs | 依赖: 04-Basic Paxos(已知Paxos两阶段+多数派) | 读者基线: 理解Paxos基本概念

---

### 1. Leader选举 — 随机超时, 先醒来者当选
  Leader挂了, 三个节点都可能自己发起选举 — 如何不产生多个Leader?
  - B2 Ch6 §1: Raft节点三态 — Follower→Candidate→Leader→Follower — 只有Leader能发起日志复制
  - 选举流程: Follower超时(election timeout 150-300ms随机) → 变Candidate → term++ → 给自己投票 → 向其他节点发RequestVote → 过半数→当选Leader; 被拒绝→回Follower
  - 关键设计: 随机超时防分裂 — 每次超时时间随机(不是固定), 减少两人同时竞选的概率 — Raft的"简洁性"核心体现之一
  - 任期term: 全局逻辑时钟 — 每个节点维护currentTerm, 看到更高term→降级Follower — term单调递增→解决老Leader回归的冲突

### 2. 日志复制 — Leader的日志必须覆盖Follower
  Paxos允许日志空洞, Raft不允许 — 为什么?
  - B2 Ch6 §2: 日志结构 — [term, index, command] — Leader追加log entry → 向Follower发AppendEntries(prevLogIndex, prevLogTerm, entries) → Follower一致性检查
  - 关键设计: 一致性检查 — prevLogIndex和prevLogTerm必须匹配Follower的本地日志 → 不匹配→Leader递减nextIndex重试 → 最终Follower日志被Leader覆盖
  - B1 Ch4 §4: 日志提交 — 当entry被复制到多数派→Leader commit→通过后续AppendEntries告知Follower→Follower apply
  - 关键区别: Multi-Paxos日志可乱序(允许空洞, 允许任意Proposer写不同slot), Raft日志严格有序(只从Leader写, 强一致性检查) — Raft牺牲了一点并发换取巨大的可理解性

### 3. 安全性 — 为什么选出的Leader一定拥有最新日志?
  网速慢的节点(日志落后很多)也可能当选Leader — 怎么防止它覆盖已经提交的数据?
  - B2 Ch6 §3: 选举限制 — Candidate的log必须至少和大多数节点的log一样新(谁的最后一个entry的term大或index大谁更新) → 请求投票时RequestVote携带候选人最后日志信息, Follower判断 [理论: Raft安全性 — 选举限制(term+N) + 提交限制(延迟提交前term)两个约束保证安全]
  - 关键设计: Raft安全性两个支柱 — (1)选举限制: 包含全部已提交日志的节点才能当选Leader (2)提交限制: 只有当前term的日志才能通过计数提交，前term日志靠新term日志"带过去"
  - 提交限制: Leader不能提交以前term的entry — 必须等当前term的entry提交后才"间接"提交之前term的entry → 防止Leader当选后"旧的entry被覆盖"的幻象问题
  - Leader Completeness Property: 任何已提交的entry必然出现在所有后续Leader的日志中 — 这是Raft安全性的核心保证
  - B2 Ch6 §7: Paxos vs ZAB vs Raft对比矩阵 — Leader方向(Paxos双向/Raft单向/ZAB单向), 选举策略(最高ballot/最新日志/最新zxid), 实现复杂度(极高/中/中低)

### 4. 快照 + 成员变更 — 工程化必然面对的问题
  日志长了怎么办? 集群加机器怎么办?
  - B2 Ch6 §5: Snapshot快照 — 当日志过长, 截断前N条, 生成状态快照 → 新节点加入时Leader发InstallSnapshot(快照+最后包含的index/term)
  - B2 Ch6 §4: 成员变更 — 单节点变更(一次加/删1个, 保证新旧配置无重叠多数派问题) vs 联合共识(C_old,new→C_new, 两次提交)
  - SOFAJRaft源码(B2 Ch6 §6): BallotBox(投票箱, 判断过半), FSMCaller(调用业务状态机的Apply), Replicator(每个Follower的复制线程)
  - 典型配置: 3节点→多数派=2, f=1; 5节点→多数派=3, f=2 — 节点数增加容错性但延迟上升
  - 关键设计: InstallSnapshot — 不需要重放百万条日志，直接把状态快照发给落后节点; 成员变更用Joint Consensus避免双Leader

### 5. 收束 — Raft vs Paxos: 选哪个?
  - Raft = 选举限制 + 日志强匹配 + 提交限制 → 安全性保证 → 比Paxos好懂 → etcd/TiKV/Kafka(3.3+)/Nacos选择它
  - Multi-Paxos = 允许空洞 + 任意Proposer → 更灵活但更复杂 → Google Chubby/Percolator选择它
  - 选择建议: 新项目优先Raft(生态好/文档多/调试工具成熟), 特殊场景(需要多个任意Leader的并发写)可以考虑Multi-Paxos

---

### 核心悬念
**"ZooKeeper的核心ZAB协议, 既不是Paxos也不是Raft — 它为什么单独设计一个共识协议? ZK的线性写是怎么做到的?"**

→ 引出 ZAB与EPaxos: ZK为什么用ZAB + 去中心化的EPaxos (06-zab-epaxos)
