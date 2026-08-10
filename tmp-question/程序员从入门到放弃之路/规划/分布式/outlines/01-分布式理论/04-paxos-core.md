# 三个角色提议"你是队长", 谁说了算? — Basic Paxos的推导与本质

> Cluster B: 15 KPs | 依赖: A(已知故障模型+一致性概念) | 读者基线: 知道分布式需要共识, 但不清楚具体协议

---

### 1. 一群人在吵谁当队长 — 共识问题是什么?
  三个节点各自收到了不同的提案: "node1当选"、"node2当选"、"node3当选" — 大家各说各话, 怎么能达成一致?
  - B2 Ch4 §1: Paxos角色 — Proposer(提议者)、Acceptor(投票者)、Learner(学习者, 记录结果)
  - B1 Ch4 §2: 共识的安全属性 — Agreement(两个正确节点不会决定不同值) + Validity(决定值必须是提议过的) + Termination(最终有决定)
  - 关键设计: Paxos只保证Safety(安全性, 不出现分歧), 不保证Liveness(活性, 可能活锁永远不决定) — FLP已经说了异步系统无法同时保证两者
  - 多数派(Quorum)直觉: 任何两个多数派必有交集 → 如果之前有决定, 后续提议者一定能"看到"该决定(从acceptor的promise中返回)

### 2. Prepare阶段 — "各位, 我要提一个编号为N的提案"
  为什么Paxos要有两阶段? 直接投不行吗?
  - B2 Ch4 §2: Prepare流程 — Proposer选唯一递增的Proposal Number N → 向多数派Acceptor发Prepare(N) → Acceptor承诺: 不再接受编号<N的提案, 并返回已接受的最高编号提案 [理论: Paxos的推导 — 从"单Acceptor"→"多数派"递增证明]
  - 关键设计: Proposal Number的作用是"全序" — 给每次提议一个唯一编号, 高编号淘汰低编号, 解决冲突
  - Promise = "拒绝低编号" + "告知已接受": Acceptor维护minProposal(不再接受小于此编号的Prepare)和acceptedProposal(已接受的最高编号提案值)
  - B1 Ch4 §2: 活锁问题 — 两个Proposer交替升编号, 永远无法进入Accept阶段 → 工程用随机退避+Leader选举解决

### 3. Accept阶段 — "既然没人反对, 这个值就是决定"
  Prepare过了半数, 能提交了吧 — 但提交什么值?
  - B2 Ch4 §2: Accept流程 — Proposer收到过半数Promise后 → 选值: 如果有acceptedValue则选它(继承之前提案), 否则用自己的value → 向Acceptor发Accept(N, V)
  - 关键设计: "继承之前的值"是Paxos的核心 — 如果之前的多数派已经有决议, 新的Proposer必须沿用该值 — 这就是为什么Paxos保证Safety
  - Acceptor接受Accept: 如果N≥minProposal则接受, 否则拒绝 → Learner收到多数派Accept后认为Chosen
  - 三节点多数派: Quorum=2, 能容忍1个故障(f=1, N=2f+1=3) → 这是Paxos的容错模型本质

### 4. 从两阶段到Multi-Paxos — 一阶段就够了
  每次写一个值都要两轮RPC(Promise→Accept), 太慢了 — Multi-Paxos怎么优化的?
  - B2 Ch4 §3: Multi-Paxos = Leader + 日志 + 连续实例 — 先选举一个Leader(稳定后), Leader跳过Prepare直接Accept
  - 关键设计: Leader稳定后的流程: Accept(instance, value) → 多数派确认 → Chosen — 从两阶段变成一阶段
  - 日志: 每个Paxos实例是一个log entry — Leader顺序填日志, Followers跟进 — 和Raft日志有本质区别(Raft强制匹配, Paxos允许空洞)
  - B2 Ch4 §4: PhxPaxos源码 — Ballot编号递增 + Master选举(租约) + Checkpoint定期快照压缩日志

### 5. 收束 — Paxos到底在解决什么问题
  - Paxos的本质是"多数派+两阶段"保证不同提议者不会决定冲突的值
  - Prepare = "声明编号+获取之前决定信息", Accept = "按规则提交值"
  - Multi-Paxos通过Leader将两阶段优化为一阶段 — 这是工程化的第一步

---

### 核心悬念
**"Paxos的Leader挂了怎么办? Raft说这个问题很简单 — 每个节点随机超时, 谁先醒来谁当Leader — 但Raft的Leader和Multi-Paxos的Leader有什么本质区别?"**

→ 引出 Multi-Paxos vs Raft: 选举/日志/安全性/成员变更全对比 (05-multi-paxos-raft)
