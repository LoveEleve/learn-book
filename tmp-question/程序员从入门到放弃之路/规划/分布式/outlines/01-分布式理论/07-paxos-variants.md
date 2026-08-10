# Paxos写了一堆变种论文, 到底在用哪个? — Paxos家族7种变种全拆解

> Cluster B: 12 KPs | 依赖: 04-06(已知Paxos/Raft/ZAB/EPaxos全族) | 读者基线: 理解共识算法后想了解扩展方向

---

### 1. Fast Paxos — 能不能一阶段就达成共识?
  Basic Paxos的两阶段(Prepare→Accept)太慢了 — Lamport自己提的Fast Paxos能减到一阶段吗?
  - B2 Ch8 §1: Fast Paxos — 跳过Prepare, 直接Accept → 但Quorum不再是简单多数派(要N-⌊N/3⌋个Accept接受) → 如果冲突→回退到Classic Round(两阶段)
  - 关键设计: Classic Round vs Fast Round — Fast Round要求更大的Quorum(因为缺少Prepare的"发现已有值"功能) → 最佳情况(N=5, Quorum=5-⌊5/3⌋=4)→4/5的Acceptor必须接受
  - Classic Quorum: 任意多数派(⌊N/2⌋+1)即可 — Fast Quorum: 更大(因为有并发proposal冲突风险)
  - 工程结论: Fast Paxos适合"低冲突+需要低延迟"的场景 — 冲突一旦发生→回退两阶段→延迟反而更高

### 2. Disk Paxos — 消息改成磁盘写
  如果节点之间消息通信不可靠但共享磁盘(如SAN)可用 — Disk Paxos怎么把Prepare/Accept变成磁盘写?
  - B2 Ch7 §1: Disk Paxos — Acceptor状态(acceptedProposal, acceptedValue)写共享磁盘而非网络发消息 → Proposer读磁盘判断状态
  - 关键设计: 磁盘块 + 原子写入 — Quorum读磁盘块→判断是否有之前决定 — 消除了"消息超时=未知"的模糊性
  - 适用场景: 共享存储环境(数据中心SAN/分布式块存储) — 比网络消息更简单(磁盘写成功就是成功) — 代价: 磁盘IO延迟

### 3. 其他变种速览 — 各解决什么问题?
  - Cheap Paxos(B2 Ch7 §2): 主处理单元(执行Paxos) + 辅助处理器(只记录不投票) — 降低对处理器能力的要求 — 类似"读写分离"的硬件层
  - Generalized Paxos(B2 Ch7 §3): 可交换操作(如set x=3, set y=5)可乱序 → 不可交换操作(set x=3, set x=5→需定序) — 减少有依赖时的共识延迟
  - Flexible Paxos: 放宽Quorum限制 — 两个独立的Quorum: Q1(Prepare)+Q2(Accept)不必都是多数派 → 但Q1+Q2必须>N → 允许"更少的节点参与一个阶段"
  - 关键设计: 选型速查 — Fast Paxos(低延迟, Client直接写>/2 Acceptors)、Disk Paxos(磁盘容灾, Acceptor写盘替代多数派)、EPaxos(去中心化, 无Leader瓶颈, 但复杂度高3x)
  - B2 Ch7 §4 Stoppable Paxos: 优雅"暂停"共识(停止accept新提案) → 在集群维护/升级期间保持"无新决定"的安全态

### 4. 变种选择矩阵 — 什么场景用什么?
  | 场景 | 推荐变种 | 核心理由 |
  |------|---------|---------|
  | 通用分布式一致性 | Multi-Paxos/Raft | 标准/生态/调试工具 |
  | 低冲突+超低延迟 | Fast Paxos | 一阶段Fast Round(需N≥5才有效) |
  | 共享存储环境 | Disk Paxos | 磁盘代替网络, 消除网络不确定性 |
  | 可交换操作多 | Generalized Paxos | 乱序执行减少等待 |
  | 混合硬件环境 | Cheap Paxos | 辅助处理器降成本 |
  | 全球多数据中心 | EPaxos | 去中心化/本地Commit |

### 5. 收束 — Paxos不是一种算法, 是一个算法家族
  - Basic Paxos → Multi-Paxos → Raft → ZAB → EPaxos → 变种 — 核心不变(多数派+两阶段承诺), 适配不同场景
  - 选择标准: 简单性(Raft) > 理论最优(Fast) > 场景特化(Disk/Generalized)
  - Lamport在2001年说"Paxos很简单" — 真的简单, 但变种矩阵证明了"没有一种共识适配所有场景"

---

### 核心悬念
**"共识算法解决了'大家达成一致'的问题, 但事务还要保证'要么全成功要么全失败' — 2PC怎么用共识解决原子提交? 为什么2PC会阻塞而3PC不会?"**

→ 引出 分布式事务协议: 2PC/3PC/XA/TCC (08-2pc-3pc-tcc)
