# Multi-Paxos 与 Raft — 选举、日志、安全和成员变更如何工程化

> Cluster B: 16 KPs | 依赖: 04-paxos-core | 读者基线: Paxos Prepare/Accept、quorum、Leader、日志实例
> 读者处境: 04 篇解释了 Paxos 的安全核心，但 Basic Paxos 的自由度和重复 Prepare 难以直接工程化；本篇比较 Multi-Paxos 与 Raft 如何用稳定 Leader、日志约束和快照落地
> 打开新视角: Raft 的“易懂”来自更强的结构约束——**一个 Leader、严格日志前缀、任期、选举限制和提交限制**；这不是免费简化，而是用约束换可解释性

---

### 概念依赖链

```
04 Basic Paxos → 本篇: Multi-Paxos vs Raft
  ├─ §1 Raft Leader/term(选举)
  ├─ §2 AppendEntries(日志匹配/提交)
  ├─ §3 安全性(选举限制/提交限制)
  ├─ §4 Snapshot/成员变更(工程维护)
  └─ §5 选择边界(灵活性/可理解性)
先讲: 选举 → 日志 → 安全证明 → 快照/成员变更 → 对比
后续依赖: 06-zab-epaxos(ZAB/EPaxos 的不同取舍)
```

### 叙事顺序

1. 问题引入——Paxos 读懂了，但 Leader 挂掉、日志空洞和成员变更如何实现？
2. Raft 选举——term、Follower/Candidate/Leader
3. 日志复制——前缀匹配与提交
4. 安全性——最新日志与 Leader Completeness
5. Snapshot/成员变更——日志截断和联合共识
6. Multi-Paxos vs Raft——结构约束的取舍
7. 收束

### 1. Leader 选举 — term 与随机超时减少竞争

场景提示: Leader 失联后，多个节点可能同时开始选举；为什么随机超时能减少 split vote？ [写作时展开]

关键设计: Raft 节点在 Follower、Candidate、Leader 间转换，term 作为单调任期编号：

```[pseudocode]
Follower election timeout
  → Candidate
  → currentTerm++
  → 给自己投票
  → 向其他节点 RequestVote

收到多数票:
  → Leader

看到更高 term:
  → 降级/更新为 Follower

心跳/AppendEntries:
  → 重置选举计时器
```

Why: 为什么随机超时只是减少 split vote，不是数学上保证唯一 Leader？——**网络延迟、节点同时超时、消息丢失仍可能导致多轮选举**；term 和多数投票保证同一任期不会有两个合法 Leader，但活性还依赖最终通信和计时器条件。 [分布式理论: §02 的部分同步/超时模型是 Raft 活性的前提]

比喻锚点: 随机超时像让候选人随机先举手，减少同时抢话；term 像会议届次，旧届主席回来不能覆盖新届决议。 [写作时展开]

### 2. AppendEntries — 日志严格前缀匹配

场景提示: Follower 日志缺 entry、term 不同或中间有空洞时，Leader 如何修复？ [写作时展开]

关键设计: Raft 用 `prevLogIndex/prevLogTerm` 做前缀一致性检查，只允许 Leader 向后追加：

```[pseudocode]
Leader → Follower:
  AppendEntries(prevLogIndex, prevLogTerm, entries, leaderCommit)

Follower:
  prev index/term 匹配?
    是 → 删除冲突后缀并追加 entries
    否 → reject

Leader:
  reject → decrement/backtrack nextIndex
  → 重试直到找到共同前缀

commit:
  entry 被多数复制
  → Leader 推进 commitIndex
  → 后续消息告知 Followers
  → FSM/apply
```

Why: 为什么 Raft 强制日志前缀匹配，而 Multi-Paxos 可以更灵活地处理实例？——**Raft 用严格结构简化 Leader 选举和日志安全证明，代价是把更多修复责任集中在 Leader**；日志空洞、并发 proposer 的灵活性减少，但实现与调试更直观。 [分布式理论: Raft 的日志匹配性质是安全证明支柱，不只是编码风格]

比喻锚点: Raft 日志像多本账必须先有完全相同的页码和内容，后面才能继续写；发现中间页不同，先撕掉冲突后页再补齐。 [写作时展开]

### 3. 安全性 — 选举限制与提交规则如何保护已提交日志

场景提示: 一个日志落后的节点为什么不能凭更快超时当 Leader，然后覆盖已经提交的数据？ [写作时展开]

关键设计: Raft 用候选人日志新旧比较和当前任期提交限制保护 Leader Completeness：

```[pseudocode]
RequestVote:
  candidate lastLogTerm/index
  → Follower 只投给日志至少同样新的候选者

Leader Completeness:
  已提交 entry 必须出现在后续合法 Leader 日志

提交限制:
  Leader 不能只凭多数副本计数直接提交旧 term entry
  → 先提交当前 term 的 entry
  → 由日志顺序间接确认前任期 entry
```

Why: 为什么 Raft 不能简单“看到某个旧 entry 在多数节点就提交它”？——**旧 term entry 可能在某些节点出现但未形成稳定提交，直接依赖计数会与后续 Leader 规则产生安全问题**；当前 term entry 的提交再带动前缀，是 Raft 安全证明中的关键细节。 [分布式理论: Leader Completeness/Log Matching/State Machine Safety 需要一起理解]

比喻锚点: 新任主席不能只看旧议案“曾经被多数人拿到过”，要先让本届正式议案通过，再确认前面的议案链也被承接。 [写作时展开]

### 4. Snapshot 与成员变更 — 日志总会变长，集群也会变化

场景提示: 日志积累百万条后，新节点加入或落后节点追赶，为什么不能一直重放全部日志？ [写作时展开]

关键设计: 快照压缩状态，成员变更用联合共识避免新旧配置同时产生冲突多数派：

```[pseudocode]
Snapshot:
  状态机状态 + lastIncludedIndex/Term
  → 截断更早日志
  → 落后节点 InstallSnapshot
  → 从快照后继续 AppendEntries

Membership:
  C_old → joint configuration(C_old,new)
  → 新旧配置规则同时生效
  → 提交后切换 C_new
  → 避免配置切换时出现两个不相交多数派
```

Why: 为什么成员变更不能直接把一个节点加入/删除后立即使用新 quorum？——**切换瞬间新旧配置可能各自形成多数，导致两个领导/决定路径无法安全交集**；Joint Consensus 用过渡配置保证变更期间仍有交集，代价是临时协调复杂度和更高 quorum 要求。 [分布式理论: snapshot 是状态压缩，membership 是安全边界变更，两者不能混成简单“清日志”]

比喻锚点: 快照像把旧账本装订成总账，成员变更像旧董事会和新董事会共同签署交接文件，不能直接换门牌。 [写作时展开]

### 5. Multi-Paxos 与 Raft — 不是简单“谁更强”

场景提示: 新项目为什么常选择 Raft，而某些系统仍使用 Multi-Paxos 族？ [写作时展开]

关键设计: 两者都可通过稳定 Leader 优化连续日志，但约束和表达方式不同：

```[pseudocode]
Multi-Paxos:
  Paxos 安全核心 + 稳定 Leader
  → 可复用 Prepare
  → 实现空间更灵活
  → 日志/恢复/成员变更需更多工程约束

Raft:
  单 Leader
  → term + 强日志前缀
  → 选举限制 + 提交限制
  → 更容易解释/实现/教学

共同点:
  quorum / Leader / log replication / snapshot
  → 都不能在少数派不可达时无条件提交
```

Why: 为什么“Raft 更易懂”不等于“所有场景都应换 Raft”？——**现有生态、性能、成员变更、日志模型、实现成熟度和系统兼容性都要考虑**；协议选择必须围绕安全性证明、故障模型和工程维护，而不是只看动画是否直观。 [分布式理论: 协议比较应同时看 Safety、Liveness、实现复杂度与部署生态]

### 6. 收束

Raft 心智模型：

```[pseudocode]
term/election
  → 选稳定 Leader
AppendEntries
  → 强制日志前缀一致
quorum/commit rule
  → 保护已提交状态
snapshot/membership
  → 控制日志和集群演进
```

**Aha Moment**: "Raft 的易懂不是少了共识难题，而是**把 Leader、日志前缀、任期和提交限制组织成更强的结构约束**；Multi-Paxos 更灵活，但需要读者和实现者承担更多隐含协议细节。"
**回答读者三问**: ①随机选举解决什么=减少同时竞选，不保证永不分裂；②Raft 为什么安全=最新日志投票限制+日志匹配+提交规则；③成员变更为什么难=必须避免新旧多数派不相交。

---

### 核心悬念

**"Raft 和 Multi-Paxos 都强调 Leader；ZooKeeper 的 ZAB 为什么采用另一套广播/恢复协议，EPaxos 又如何减少中心 Leader 依赖？"**

→ 引出 06-zab-epaxos — ZAB、EPaxos 与不同共识路线。