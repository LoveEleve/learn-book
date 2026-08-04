# 第6章 Raft——共识算法的宠儿

> **技术专家重写**
>
> 原书第6章按"简介 → 算法描述 → 模拟 → 成员变更 → 日志压缩 → SOFA-JRaft 源码"组织，内容完整但源码部分占比过大。
> 本重写版聚焦一个核心问题：**Raft 如何用"可理解性"重新设计共识算法？三个子问题如何各自解决 Paxos 的什么缺陷？** 用具体编号推演每一步，深入讲解 Figure 8 问题。

---

## 6.1 Raft 的诞生——Paxos 太难理解了

### 6.1.1 Paxos 的"难理解"问题

第4章我们学了 Paxos。Lamport 在《Paxos Made Simple》开头写道："The Paxos algorithm, when presented in English, is very simple."（Paxos 用英语讲其实很简单。）

但事实是，Paxos 被公认为"分布式系统中最难理解的算法之一"。Diego Ongaro 在 Raft 论文中报告了一个实验：让学生分别学习 Paxos 和 Raft，结果 Raft 的学习效果显著好于 Paxos——**不是因为 Raft 更高效或更正确，而是因为 Raft 更易理解**。

Paxos 难理解的三个原因：

1. **角色拆分模糊**：Proposer/Acceptor/Learner 三角色，加上 Multi-Paxos 的 Leader，角色间交互复杂
2. **编号机制抽象**：提案编号 n 的全局唯一递增、Promise 的承诺逻辑，缺乏直觉
3. **Multi-Paxos 不完整**：Lamport 论文只给了 Basic Paxos 的完整描述，Multi-Paxos 只给了思路——工程细节留给读者

### 6.1.2 Raft 的设计哲学：可理解性优先

2014 年，Diego Ongaro 和 John Ousterhout 在 USENIX ATC 发表了 Raft 论文《In Search of an Understandable Consensus Algorithm》。Raft 的核心设计目标不是"更高效"或"更正确"，而是**更易理解**。

Raft 通过三个设计决策实现可理解性：

1. **问题分解**：把共识拆成三个独立子问题——领导人选举、日志复制、安全性。每个子问题独立理解、独立实现
2. **状态空间简化**：减少不确定的状态转换。例如 Raft 的节点只有三个状态（Follower/Candidate/Leader），而 Paxos 的角色更模糊
3. **强 Leader 模型**：所有写请求都经过 Leader，数据流是单向的（Leader → Follower）。Paxos 允许多 Proposer 竞争，数据流更复杂

**Raft 不是"更好的 Paxos"，而是"更易理解的 Paxos"**——两者的数学基础相同（多数派 + 日志复制），但 Raft 通过更清晰的结构大幅降低了实现难度。

> **跨书对照**：
> - **唐伟志《深入理解分布式系统》第4章**：从"Paxos + Raft 对比 + Go 实现"切入，提供代码级视角——差异在于"理论推导 vs 代码实现"
> - **江峰《分布式高可用算法》第8章**：从形式化角度证明 Raft 和 Paxos 在共识三性质上等价——差异在于"工程直觉 vs 数学严格"

---

## 6.2 三个子问题——把共识拆成可理解的模块

Paxos 是一个整体性协议——Prepare/Accept 两阶段交织在一起，初学者难以分清"哪个部分解决什么问题"。

Raft 把共识拆成三个独立子问题：

| 子问题 | 解决什么 | 对应 Paxos 的什么 |
|--------|---------|-----------------|
| **领导人选举** | 谁来当 Leader？Leader 崩溃如何切换？ | Multi-Paxos 的 Leader 选举（论文未定义） |
| **日志复制** | Leader 如何把操作复制到 Follower？ | Paxos 的 Accept 阶段 |
| **安全性** | 如何保证已提交的日志不被覆盖？ | Paxos 的 Prepare 阶段（探测已接受值） |

**关键洞察**：Paxos 的 Prepare 阶段同时解决了"探测已有值"和"获取承诺"两个问题——耦合在一起。Raft 把它们拆开：选举保证新 Leader 拥有所有已提交日志（安全性），日志复制负责同步（日志复制），不需要额外的 Prepare 阶段。

**Raft 的三个角色**：

| 角色 | 职责 | 类比 |
|------|------|------|
| **Follower（跟随者）** | 被动接收 Leader 的日志复制和心跳 | Paxos Acceptor |
| **Candidate（候选人）** | 发起选举，争取成为 Leader | Paxos Proposer（选举时） |
| **Leader（领导人）** | 处理所有客户端请求，复制日志到 Follower | Multi-Paxos Leader |

**状态转换**：

```
                ┌──────────────┐
                │   Follower   │◄──────────┐
                └──────┬───────┘           │
                       │ 选举超时           │ 收到 Leader 心跳
                       ▼                   │
                ┌──────────────┐           │
                │  Candidate   │           │
                └──────┬───────┘           │
                       │                   │
              ┌────────┼────────┐          │
              │ 获多数派│        │ 未获多数派│
              ▼        │        ▼          │
        ┌──────────┐   │  ┌──────────────┐│
        │  Leader  │   │  │  Follower    │├┘
        └──────────┘   │  └──────────────┘│
                       │     (退回)        │
                       └───────────────────┘
```

---

## 6.3 子问题一：领导人选举

### 6.3.1 term——Raft 的"逻辑时钟"

Raft 使用 **term（任期号）** 作为逻辑时钟——每次选举开启一个新 term，term 单调递增。

```
term 1           term 2           term 3
┌─────────┐     ┌─────────┐     ┌─────────┐
│ Leader A│ 崩溃 │ Leader B│ 崩溃 │ Leader C│
└─────────┘     └─────────┘     └─────────┘
```

**term 的作用**：
1. **标识 Leader 的任期**——每个 term 最多一个 Leader
2. **检测过时信息**——如果节点收到 term < currentTerm 的消息，直接拒绝
3. **类比**：term 对应 Paxos 的提案编号 n、ZAB 的 epoch、VR 的 view-number——都源自同一个概念（第5章 5.5.4 节已述）

### 6.3.2 选举超时——为什么用随机？

**问题**：Leader 崩溃后，多个 Follower 同时发现"心跳超时"，同时变成 Candidate，同时发起选举——谁也拿不到多数派。

**Raft 的解决方案**：**随机选举超时**（randomized election timeout）。每个 Follower 的选举超时在一个随机范围内（Raft 论文示例 150-300ms；etcd 生产默认 [1000, 2000]ms），不同 Follower 的超时时刻不同——最先超时的 Follower 先发起选举，大概率获得多数派。

**为什么随机有效？**
- 3 节点集群中，3 个 Follower 的超时时间几乎不可能完全相同
- 最先超时的 Follower 先发 RequestVote——在其他 Follower 超时之前就能获得多数派
- 即使两个 Follower 几乎同时超时（split vote），下一次随机超时大概率能选出唯一 Leader

**对比 Paxos 的活锁**：Paxos 的两个 Proposer 互相抢占导致活锁（第4章 4.6 节）。Raft 用随机超时从概率上避免了活锁——这和 Paxos 的"随机退避"解决方案是同一思想，但 Raft 在协议设计层面就内置了随机性。

### 6.3.3 RequestVote RPC——完整的选举流程

**选举流程**：

```
T0: Follower-1 的选举超时触发
T1: Follower-1 状态变为 Candidate
    - currentTerm++（term 递增）
    - votedFor = 自己（投自己一票）
    - 重置选举超时
T2: Candidate-1 发送 RequestVote(term, candidateId, lastLogIndex, lastLogTerm) 给所有节点

T3: 其他节点收到 RequestVote:
    - 如果 term < currentTerm → 拒绝
    - 如果 votedFor == null 或 votedFor == candidateId：
      - 检查 Candidate 的日志是否至少和自己一样新（选举限制，6.5 节详述）
      - 如果日志够新 → 投赞成票，votedFor = candidateId，重置选举超时
      - 如果日志不够新 → 拒绝
    - 如果 votedFor != null 且 votedFor != candidateId → 拒绝（已投给别人）

T4: Candidate-1 收到多数派赞成票 → 状态变为 Leader
T5: Leader-1 立即发送心跳（空的 AppendEntries）给所有 Follower，确立权威
```

**RequestVote RPC 的参数**：

| 参数 | 含义 |
|------|------|
| `term` | Candidate 的当前 term |
| `candidateId` | Candidate 的节点 ID |
| `lastLogIndex` | Candidate 最后一条日志的 index |
| `lastLogTerm` | Candidate 最后一条日志的 term |

| 返回值 | 含义 |
|--------|------|
| `term` | Follower 的当前 term（用于 Candidate 检测自己是否过时） |
| `voteGranted` | 是否投票 |

**关键规则**：每个节点在一个 term 内**只能投一票**（votedFor 唯一）。这保证了每个 term 最多一个 Leader——因为要成为 Leader 需要多数派投票，而多数派中每个节点只能投一次。

### 6.3.4 选举限制——为什么选日志最新的节点？

RequestVote 中携带 `lastLogIndex` 和 `lastLogTerm`——Follower 检查 Candidate 的日志是否"至少和自己一样新"：

```
比较规则（先比 lastLogTerm，再比 lastLogIndex）：
1. 如果 Candidate 的 lastLogTerm > Follower 的 lastLogTerm → Candidate 更新
2. 如果 lastLogTerm 相同，Candidate 的 lastLogIndex >= Follower 的 lastLogIndex → Candidate 更新
3. 否则 → Candidate 不够新，拒绝投票
```

**为什么需要选举限制？** 如果选出一个没有所有已提交日志的节点当 Leader，已提交的日志可能丢失——违反 Safety。选举限制保证了新 Leader 必然拥有所有已提交的日志（第6.5 节安全性证明会回到这一点）。

---

## 6.4 子问题二：日志复制

### 6.4.1 AppendEntries RPC——Leader 的核心工具

Leader 通过 **AppendEntries RPC** 复制日志到 Follower。这个 RPC 同时用于：
1. **复制日志条目**（携带新的 log entries）
2. **心跳**（不携带 entries，仅维持 Leader 权威）

**AppendEntries RPC 的参数**：

| 参数 | 含义 |
|------|------|
| `term` | Leader 的当前 term |
| `leaderId` | Leader 的节点 ID |
| `prevLogIndex` | 紧接新条目之前的 log entry 的 index |
| `prevLogTerm` | prevLogIndex 对应的 entry 的 term |
| `entries[]` | 新的日志条目（心跳时为空） |
| `leaderCommit` | Leader 的 commitIndex |

| 返回值 | 含义 |
|--------|------|
| `term` | Follower 的当前 term（用于 Leader 检测自己是否过时） |
| `success` | Follower 是否包含匹配 prevLogIndex 和 prevLogTerm 的条目 |
| `conflictIndex` / `conflictTerm` | （优化）如果不匹配，告诉 Leader 从哪里开始不一致 |

### 6.4.2 日志匹配特性（Log Matching Property）

Raft 的日志复制有一个关键保证——**日志匹配特性**：

> 如果两条日志条目（在不同节点上）具有相同的 index 和 term，那么：
> 1. 它们的值相同
> 2. 它们之前的所有条目也完全相同

**这个特性如何保证？**

1. **相同 index + term → 值相同**：Leader 在一个 term 内，一个 index 只创建一个 entry——不会有两个不同的 entry 共享 (index, term)
2. **之前的条目也相同**：AppendEntries 携带 `prevLogIndex` 和 `prevLogTerm`——Follower 只有在本地 `log[prevLogIndex].term == prevLogTerm` 时才接受新条目。这构成了**归纳基础**——如果前一个条目匹配，那么再前一个也匹配（因为前一个的 AppendEntries 也做过同样的检查）

### 6.4.3 正常复制流程

```
T0: Client 发送写请求（如 PUT /key "value"）给 Leader
T1: Leader 将请求写入本地日志：log[index=6, term=3] = "PUT /key value"
T2: Leader 发送 AppendEntries 给所有 Follower:
    prevLogIndex=5, prevLogTerm=3, entries=[{index:6, term:3, cmd:"PUT /key value"}]
T3: Follower 检查本地 log[5].term == 3 → 匹配 → 追加 entry[6] → 回复 success=true
T4: Leader 收到多数派 success → 标记 entry[6] 为 committed（commitIndex=6）
T5: Leader 应用 entry[6] 到状态机（如 etcd 的 KV 存储）
T6: Leader 在下一次 AppendEntries 中携带 leaderCommit=6
T7: Follower 收到后，将本地 commitIndex 更新为 6，应用 entry[6] 到状态机
T8: Leader 返回 Client "操作成功"
```

### 6.4.4 日志不一致时的修复

Leader 崩溃重启后，新 Leader 的日志可能与某些 Follower 不一致——Follower 可能缺少条目，也可能有多余条目。

**Raft 的解决方案：Leader 强制覆盖 Follower**

Leader 为每个 Follower 维护两个索引：
- `nextIndex[followerId]`：Leader 下一次要发给该 Follower 的 entry 的 index
- `matchIndex[followerId]`：Leader 已知该 Follower 已复制的最高 index

**修复流程**：

```
T0: 新 Leader 上任，nextIndex[所有Follower] = Leader的最后index + 1
T1: Leader 发送 AppendEntries(prevLogIndex=lastIndex, prevLogTerm=lastTerm, entries=[])
T2: Follower 检查 log[lastIndex].term != lastTerm → 不匹配 → 返回 success=false
    （可选优化：返回 conflictIndex 和 conflictTerm）
T3: Leader 将 nextIndex[followerId]--（回退一个位置）
T4: Leader 重发 AppendEntries(prevLogIndex=lastIndex-1, ...)
T5: 重复 T2-T4，直到 Follower 返回 success=true
T6: 找到匹配点后，Leader 发送从匹配点之后的所有 entries
T7: Follower 追加新 entries，截断匹配点之后的旧 entries（如果有）
```

**关键点**：Follower 一旦发现 `prevLogIndex` 和 `prevLogTerm` 匹配，就会**截断**之后的所有不一致条目，用 Leader 的条目覆盖。这保证了 Leader 的日志是"权威的"——Follower 的日志最终会和 Leader 完全一致。

### 6.4.5 完整时间线推演

**场景**：3 节点集群，Leader-1 崩溃后 Leader-2 上任，Follower-3 的日志落后

```
Leader-2 日志:  [1] [2] [3] [4] [5]  (term=2, last index=5)
Follower-3 日志: [1] [2] [3]          (term=2, last index=3)

T0: Leader-2 上任，nextIndex[Follower-3] = 6
T1: Leader-2 发送 AppendEntries(prevLogIndex=5, prevLogTerm=2, entries=[])
T2: Follower-3 检查 log[5] → 不存在 → success=false
T3: Leader-2 nextIndex[Follower-3] = 5，重发 AppendEntries(prevLogIndex=4, ...)
T4: Follower-3 检查 log[4] → 不存在 → success=false
T5: Leader-2 nextIndex[Follower-3] = 4，重发 AppendEntries(prevLogIndex=3, prevLogTerm=2, entries=[4,5])
T6: Follower-3 检查 log[3].term == 2 → 匹配！→ 追加 entries[4,5] → success=true
T7: Leader-2 更新 matchIndex[Follower-3] = 5
T8: Follower-3 日志: [1] [2] [3] [4] [5]  ← 与 Leader 一致
```

---

## 6.5 子问题三：安全性

### 6.5.1 要保证什么？

Raft 的 Safety 核心是 **Leader 完整性特性（Leader Completeness Property）**：

> 如果一条日志条目在某个 term 被提交（committed），那么这条条目必然出现在所有更高 term 的 Leader 的日志中。

这保证了：已提交的日志不会丢失——因为每个新 Leader 都必然包含它。

### 6.5.2 选举限制——保证新 Leader 拥有已提交日志

6.3.4 节讲了选举限制：Follower 只投票给日志"至少和自己一样新"的 Candidate。

**为什么这能保证 Leader 完整性？**

假设 entry[i] 已被 term=T 的 Leader 提交——说明 entry[i] 已被多数派复制。新 Leader 需要 majority 的投票——这两个 majority 必有交集（鸽笼原理，第1章 1.2.6 节）。交集节点既有 entry[i]（因为它在已提交的 majority 中），又参与了新 Leader 的投票——它只会投给日志至少和自己一样新的 Candidate → 新 Leader 必然有 entry[i]。

### 6.5.3 提交限制——Figure 8 问题

这是 Raft 最微妙的点——**Leader 只能提交当前 term 的日志，不能提交之前 term 的日志**。

**为什么不能提交旧 term 的日志？**

考虑以下场景（Raft 论文 Figure 8）：

```
(a) S1 是 term=2 的 Leader，将 entry[2] 复制到 S2
(b) S1 崩溃，S5 通过 S3/S4/S5 的投票成为 term=3 的 Leader，写入 entry[2]（覆盖）
(c) S5 崩溃，S1 恢复成为 term=4 的 Leader，将 entry[2]（term=2）复制到 S2 和 S3
    此时 entry[2]（term=2）在 S1/S2/S3 上（多数派），但 S1 尚未提交
(d) S1 崩溃，S5 通过 S3/S4/S5 的投票（3 票 = majority）成为 term=5 的 Leader
    → S5 可以用 term=5 覆盖 entry[2]！
    → 如果 S1 在 (c) 中"提交"了 entry[2]（term=2），此时已提交的日志被覆盖了！
```

**问题根源**：一个 term=2 的日志条目，即使在多数派节点上，也可能被更高 term 的 Leader 覆盖——因为新 Leader 的选举不依赖这个旧 term 的条目。

**Raft 的解决方案**：Leader 只通过**复制当前 term 的条目**来间接提交旧 term 的条目。

```
(e) 正确流程：S1 在 term=4 下，先复制一个 term=4 的新 entry[3] 到多数派
    → entry[3] 被提交（当前 term）
    → entry[2]（旧 term）因为排在 entry[3] 前面，也被间接提交
    → 此时即使 S5 当选新 Leader，它也必须包含已提交的 entry[2]（选举限制）
```

**关键规则**：Leader 不会单独提交旧 term 的条目——它通过提交当前 term 的新条目来"顺带"提交之前的所有条目。这就是 `leaderCommit` 只更新到当前 term 已确认的 index 的原因。

**这个问题的工程影响**：如果实现 Raft 时犯了 Figure 8 的错误（直接提交旧 term 的日志），在极端故障序列下可能丢失已"提交"的数据。Raft 论文花了大量篇幅讨论这个问题——这是 Raft 实现中最容易出错的地方。

**Figure 8 完整五步推演**（5 节点 S1-S5，majority = 3）：

| 步骤 | S1 | S2 | S3 | S4 | S5 | Leader | 说明 |
|------|----|----|----|----|----|--------|------|
| (a) | [2] | [2] | | | | S1(term=2) | S1 将 entry[2](term=2) 复制到 S2 |
| (b) | [2] | [2] | | | [3] | S5(term=3) | S1 崩溃；S5 获 S3+S4+S5 投票，写入 entry[2](term=3)覆盖 |
| (c) | [2] | [2] | [2] | | [3] | S1(term=4) | S5 崩溃；S1 恢复，将 entry[2](term=2) 复制到 S2 和 S3。S1/S2/S3 有 entry[2](term=2)=majority，但**未提交** |
| (d) | [2] | [2] | [3] | [3] | [3] | S5(term=5) | S1 崩溃；S5 获 **S3+S4+S5**（3票），用 term=5 覆盖 entry[2]。**注意：S2 不会投给 S5**——S2 有 entry[2](term=2)，比 S5 的日志更新（选举限制） |
| (e) | [2][4] | [2][4] | [2][4] | | | S1(term=4) | **正确流程**：S1 在 term=4 先复制新 entry[3](term=4) 到 majority → entry[3] 提交 → entry[2](term=2) 被间接提交 → S5 无法覆盖（选举限制保证新 Leader 必有 entry[2]）|

**步骤 (d) 的关键**：S2 有 entry[2](term=2)，S5 的 lastLogTerm=3 > S2 的 lastLogTerm=2 → 按选举限制，S2 **应该**投给 S5（S5 的日志更新）。**等等——这里需要更精确的分析**：

实际上 (d) 中 S5 的日志是 [3](term=3)，S2 的日志是 [2](term=2)。比较规则是先比 lastLogTerm：S5 的 3 > S2 的 2 → S5 更新 → S2 **会**投给 S5。所以 S5 获得 S2+S3+S4+S5 中至少 3 票（S3/S4/S5 或 S2/S3/S4 等）。

**这就是 Figure 8 问题的核心**：entry[2](term=2) 虽然在 (c) 中已复制到 majority（S1/S2/S3），但因为 S5 的 term=3 比 entry[2] 的 term=2 更高，S5 可以通过选举获得投票（它的 lastLogTerm 更高），然后用新 entry 覆盖 entry[2]。如果此时 entry[2] 已被"提交"，已提交日志就丢失了。

**步骤 (e) 的正确做法**：S1 不直接提交 entry[2](term=2)，而是先写入一个新 entry[3](term=4) 并复制到 majority → entry[3] 提交 → entry[2] 被**间接提交**（因为它排在 entry[3] 前面）。此时如果 S5 要当选，选举限制保证新 Leader 必须有 entry[3]（已提交），因此也有 entry[2]。

> **跨书对照**：唐伟志《深入理解分布式系统》第4章从 Go 实现角度展示了 Figure 8 问题的代码级规避。释慧利原书第6章 6.2 节也讨论了这个问题，但未给出完整推演。本节的推演遵循 Raft 论文 Figure 8 的原始路径。

### 6.5.4 持久化状态 vs 易失状态——什么必须落盘？

Raft 的安全性依赖节点的持久化状态。如果节点重启后"忘记"了关键信息，安全性会被破坏。

**必须持久化的状态（所有节点）**：

| 状态 | 含义 | 持久化要求 | 重启后丢失的后果 |
|------|------|-----------|----------------|
| `currentTerm` | 当前 term | 必须 fsync | 重启后可能给同一 term 投两次票 → 两个 Leader |
| `votedFor` | 本 term 投票给谁 | 必须 fsync | 同上 |
| `log[]` | 完整日志 | 必须 fsync | 已提交日志丢失 → 数据不一致 |

**易失状态（重启后重置）**：

| 状态 | 含义 | 重置值 |
|------|------|--------|
| `commitIndex` | 已提交的最高 index | 0（从快照+日志恢复） |
| `lastApplied` | 已应用到状态机的最高 index | 0 |
| `nextIndex[]`（Leader） | 每个 Follower 的下次复制位置 | 重新初始化 |
| `matchIndex[]`（Leader） | 每个 Follower 的已确认位置 | 重新初始化 |

**关键**：`currentTerm` 和 `votedFor` 必须在投票前 fsync——否则节点重启后可能"忘记"已投过票，给同一个 term 投两次票，导致两个 Leader。这与 Paxos 的 `promised_n` 持久化（第4章 4.8.1 节）和 ZAB 的 `acceptedEpoch` 持久化（第5章 5.5.5 节）是同一要求。

### 6.5.5 PreVote 优化——防止网络分区节点回归后频繁增加 term

**问题**：一个网络分区的节点（脱离 majority）会不断超时 → 不断增加 term → 网络恢复后，它的高 term 会让当前 Leader 退位（因为 Leader 收到更高 term 的消息会自动变为 Follower）。

**PreVote 优化**（etcd/SOFA-JRaft 等工业实现常用）：
- Candidate 在真正增加 term 之前，先发一轮 **PreVote**（不增加 term，不修改 votedFor）
- PreVote 询问："如果我发起选举，你会投给我吗？"
- 只有收到多数派 PreVote 同意，才真正增加 term 并发起正式 RequestVote

**效果**：网络分区的节点不会获得多数派 PreVote（因为 majority 在另一个分区），因此不会增加 term——网络恢复后不会干扰当前 Leader。

### 6.5.6 Safety 证明草图

综合三个子问题的保证：

1. **选举限制** → 新 Leader 必然拥有所有已提交的日志
2. **日志匹配特性** → 相同 (index, term) 的条目值相同且前序一致
3. **提交限制** → 只提交当前 term 的条目，间接提交旧 term 的条目

**推导**：如果 entry[i, term=T] 被提交 → 它在多数派 M₁ 上 → 新 Leader 需要 majority M₂ 的投票 → M₁ ∩ M₂ ≠ ∅ → 交集节点有 entry[i] 且投票给了新 Leader → 新 Leader 的日志至少和交集节点一样新 → 新 Leader 有 entry[i] ✓

**因此**：已提交的日志永远不会丢失——每个新 Leader 都必然包含它。这就是 Raft 的 Safety 保证。

---

## 6.6 成员变更——Joint Consensus

### 6.6.1 为什么不能直接切换配置？

集群从 3 节点扩容到 5 节点时，如果直接切换配置——可能出现"旧配置的 majority"和"新配置的 majority"不重叠：

```
旧配置 (3节点): {A, B, C}，majority = 2
新配置 (5节点): {A, B, C, D, E}，majority = 3

如果 A 和 B 还在旧配置，C 和 D 和 E 在新配置：
- A+B 构成旧 majority（2/3），可以选出 Leader-A
- C+D+E 构成新 majority（3/5），可以选出 Leader-C
→ 两个 Leader！脑裂！
```

### 6.6.2 Joint Consensus——两阶段过渡

Raft 使用 **Joint Consensus（联合一致）** 解决成员变更——这是 Raft 论文明确提出的概念（Multi-Paxos 论文未给出明确的成员变更算法）：

**阶段一：联合配置（Cold,new）**

Leader 提交一个特殊的配置变更条目——同时包含旧配置 Cold 和新配置 Cnew。此后的决策需要**旧配置的 majority 和新配置的 majority 都同意**：

```
决策条件 = Cold 的 majority AND Cnew 的 majority
```

这保证了过渡期间不会出现两个不重叠的 majority。

**阶段二：新配置（Cnew）**

联合配置提交后，Leader 提交另一个配置变更条目——只包含新配置 Cnew。此后只需要新配置的 majority：

```
决策条件 = Cnew 的 majority
```

**完整流程**：

```
T0: Leader 收到成员变更请求（3→5节点）
T1: Leader 追加 Cold,new 条目到日志，复制到 Cold ∪ Cnew 的 majority
T2: Cold,new 被提交 → 此后需要 Cold majority + Cnew majority
T3: Leader 追加 Cnew 条目到日志，复制到 Cnew 的 majority
T4: Cnew 被提交 → 成员变更完成，此后只需 Cnew majority
T5: 不在 Cnew 中的旧节点可以安全下线
```

### 6.6.3 单节点变更——更简单的替代方案

很多 Raft 实现（如 etcd）使用**单节点变更**——每次只增减一个节点。数学上可以证明：单节点变更不会导致两个不重叠的 majority（因为新旧配置的 majority 必有交集）。

**优点**：不需要 Joint Consensus 的两阶段过渡，实现更简单。
**限制**：一次只能增减一个节点——3→5 需要两次变更。

> **跨书对照**：Joint Consensus 是 Raft 论文的概念，Multi-Paxos 论文未给出明确的成员变更算法（Chubby 使用 α-reconfig）。释慧利原书第6章 6.4 节也讨论了成员变更。唐伟志《深入理解分布式系统》第4章从代码角度展示了 etcd 的单节点变更实现。

---

## 6.7 日志压缩与快照

### 6.7.1 日志无限增长的问题

Raft 的日志会无限增长——每个操作都被记录为一条 log entry。长期运行后，日志占用大量磁盘空间，且新 Leader 恢复时需要重放全部日志。

### 6.7.2 快照机制

**快照（Snapshot）**：状态机定期将当前状态序列化为快照文件，删除快照之前的所有 log entry。

```
快照前:
  log: [1] [2] [3] [4] [5] [6] [7] [8] [9] [10]
  状态机: 应用 1-10 后的状态

快照后:
  快照: {lastIncludedIndex=10, lastIncludedTerm=3, state=...}
  log: [11] [12] [13]  (只保留快照之后的条目)
```

**快照包含**：
- `lastIncludedIndex`：快照包含的最后一条 entry 的 index
- `lastIncludedTerm`：该 entry 的 term
- `state`：状态机的完整状态（如 etcd 的 KV 树）

### 6.7.3 InstallSnapshot RPC

当 Follower 落后太多（Leader 的 nextIndex 对应的 entry 已被快照删除），Leader 无法通过 AppendEntries 补齐——此时需要 **InstallSnapshot RPC** 发送完整快照：

```
Leader ──InstallSnapshot(term, lastIncludedIndex, lastIncludedTerm, data)──► Follower
Follower:
  1. 丢弃本地 lastIncludedIndex 之前的所有日志
  2. 将快照加载到状态机
  3. lastIncludedIndex 之后的日志保留（如果有且 term 匹配）
  4. 回复成功
```

**快照的触发时机**：
- 日志大小超过阈值（etcd 默认 100MB）
- 日志条目数超过阈值（Raft 论文建议 10000 条）
- 定时触发

---

## 6.8 Raft vs Paxos vs ZAB——三者对比

### 6.8.1 对比表

| 维度 | Paxos | ZAB | Raft |
|------|-------|-----|------|
| **协议类型** | 共识协议 | 原子广播协议 | 共识协议（复制状态机） |
| **设计目标** | 正确性 | primary order | 可理解性 |
| **角色** | Proposer/Acceptor/Learner | Leader/Follower/Observer | Leader/Candidate/Follower |
| **编号** | 提案编号 n | zxid = epoch + counter | term |
| **阶段** | 2（Prepare → Accept） | 3（Discovery → Sync → Broadcast） | 3 子问题（选举 + 复制 + 安全） |
| **Leader 恢复** | 对未确认 entry 逐一 Prepare | 选举 + 日志同步 + 丢弃幽灵 | 选举 + nextIndex 回退 + 强制覆盖 |
| **幽灵事务** | Prepare 继承 | 直接丢弃 | Leader 覆盖 Follower 日志 |
| **成员变更** | 论文未定义 | ZooKeeper 自实现 | Joint Consensus / 单节点 |
| **顺序保证** | 不保证 primary order | 保证 primary order | 保证（Leader 串行追加） |
| **典型实现** | Chubby/Spanner/Cassandra LWT | ZooKeeper | etcd/TiKV/Consul/Kafka KRaft |

### 6.8.2 为什么 Raft 更受欢迎？

1. **可理解性 = 可维护性 = 工程可靠性**——Raft 的代码更容易写对、更容易 review、更容易 debug
2. **论文本身就是工程指南**——Raft 论文包含了完整的算法描述、边界条件、实现建议。Paxos 论文只给了思路
3. **强 Leader 模型简化了客户端交互**——客户端只需要找 Leader 发请求，不需要处理多 Proposer 竞争
4. **生态效应**——etcd（Kubernetes 底座）的成功带动了 Raft 的广泛采用

### 6.8.3 VR 传承回顾

第5章 5.5.4 节已述：VR（1988）是共同祖先，Paxos（1990）、ZAB（2008）、Raft（2014）是三条平行路径。Raft 论文明确引用了 VR 作为 term 的灵感来源——**不是 ZAB 影响了 Raft，而是两者都受 VR 启发**。

---

## 6.9 生产实践

### 6.9.1 etcd——Raft 的旗舰实现

etcd 是 Kubernetes 的元数据存储，使用 Raft 保证多副本一致性。

**存储引擎**：bbolt（BoltDB 的 CoreOS fork，磁盘嵌入式的 COW B+树存储引擎；COW = Copy-On-Write 写时复制——写入时不动原页，复制新页修改，旧页供读路径使用）。内存中维护 key→revision 的 B 树索引用于高效查找，数据本身持久化在 bbolt 磁盘文件中。

**MVCC（Multi-Version Concurrency Control，多版本并发控制）**：etcd 的每个 key 保存完整修改历史（revision = {main, sub}），支持历史版本查询（`etcdctl get --rev=N`）。Compact 操作清理旧版本。

**读优化**：
- **ReadIndex Read**（3.4+ 默认）：Follower 向 Leader 请求当前 committed index，等待本地 applied index 追上后返回——保证线性一致性，Follower 也能处理读
- **Lease Read（租约读）**：Leader 在 lease 期内直接读，不走 Raft——最快但依赖时钟准确性
- **Serializable Read**：直接从本地状态机读，不走共识——最快但可能读到旧数据

**写优化**：
- **Pipeline（流水线）**：Leader 连续 Propose 不等前一个 committed——吞吐提升显著
- **Batch（批量）**：多个请求合并为一个 log entry——减少 fsync 次数
- **Group Commit（组提交）**：多个 Propose 的日志合并一次 fsync

**关键参数**：
| 参数 | 默认值 | 说明 |
|------|--------|------|
| `heartbeat-interval` | 100ms | Leader 心跳间隔 |
| `election-timeout` | 1000ms | 选举超时（随机范围 [1000, 2000]ms） |
| `snapshot-count` | 10000 | 快照触发的日志条目数 |
| `quota-backend-bytes` | 2GB | 后端数据库大小告警阈值 |

### 6.9.2 TiKV——Multi-Raft 架构

TiKV 将数据划分为 Region（默认 96MB），每个 Region 由独立的 Raft Group 管理。一个集群中同时有数十万个 Raft Group 在运行。

**Multi-Raft 的工程挑战与解决方案**：

1. **批量心跳**：如果每个 Region 独立发心跳，10 万个 Region × 3 节点 = 30 万条心跳消息——网络风暴。TiKV 把同一对节点之间的多个 Region 心跳**合并为一个批量消息**——大幅减少网络开销

2. **Region 分裂与合并**：
   - 分裂：Region 超过阈值（默认 96MB）时自动分裂为两个——保证数据均匀分布
   - 合并：相邻 Region 都很小时自动合并——减少 Raft Group 数量

3. **Region 迁移（由 PD 调度）**：PD（Placement Driver，TiDB 的调度组件）检测到节点负载不均时，迁移 Region：
   - 先增加副本（Add Learner → Add Voter）
   - 等新副本追上日志
   - 再减少旧副本（Remove Voter）

4. **Raft Engine**：TiKV 3.x+ 使用独立的 Raft Engine 孓存储 Raft 日志（与 KV 数据分离）——减少写放大

### 6.9.3 CockroachDB——leaseholder 与 Raft Leader 分离

CockroachDB 是开源的分布式 SQL 数据库，使用 Raft + HLC（Hybrid Logical Clock，混合逻辑时钟，第2章 2.4.3 节已述）实现跨地域的 Serializable 隔离。

**核心设计——leaseholder 与 Raft Leader 分离**：
- **Raft Leader**：负责日志复制和提交
- **leaseholder（租约持有者）**：负责处理读写请求——不一定是 Raft Leader

**为什么要分离？** 如果 Raft Leader 和 leaseholder 是同一节点，跨地域部署时 Leader 可能在另一个地域——每次写都要跨地域 RTT。分离后，leaseholder 可以是本地地域的节点（通过 lease 机制），Raft Leader 可以在另一个地域——读请求不需要跨地域。

**对比 TiKV**：TiKV 的 Leader 既是 Raft Leader 也是读写入口——简单但跨地域写延迟高。CockroachDB 的分离设计更适合跨地域部署。

### 6.9.4 SOFA-JRaft——蚂蚁金服的 Java Raft

SOFA-JRaft 是蚂蚁金服开源的 Raft Java 实现，用于 Nacos（1.4+ 的 CP 模式）、SOFAMosn、蚂蚁内部配置中心等。

**关键工程特性**：
- **SPI 扩展点**：存储（LogStorage）、日志（ LogManager）、状态机（StateMachine）、RPC 均可通过 SPI 定制——适应不同场景
- **Replicator（复制器）**：每个 Follower 对应一个 Replicator，负责管理 nextIndex/matchIndex 和 AppendEntries 的发送
- **NodeImpl（节点实现）**：Raft 节点的核心实现，管理状态转换（Follower → Candidate → Leader）
- **RheaKV**：基于 JRaft 的嵌入式 KV 存储——提供 KV API + Raft 共识的一体化方案

### 6.9.5 Kafka KRaft——Raft 替代 ZooKeeper

Kafka 2.8 引入 KRaft 模式，3.3 默认可用，4.0（2024）完全移除 ZooKeeper 依赖。

**KRaft 与标准 Raft 的差异**：
- **日志载体不同**：标准 Raft 用独立日志文件，KRaft 复用 Kafka 的 topic partition 作为日志——元数据本身存储在 `__cluster_metadata` topic 中
- **Controller Quorum**：只有 Controller 节点参与 KRaft 选举（通常 3 或 5 个），不是所有 Broker 都参与
- **Snapshot**：KRaft 的快照是 Kafka 的 segment 文件，复用 Kafka 的存储引擎

**为什么 Kafka 选 Raft 而非 ZAB？**
1. ZooKeeper 的 JVM GC 停顿影响 Kafka 稳定性（第5章 5.10.7 节已述）
2. ZooKeeper 的 znode 内存模型限制元数据规模（大集群 >10,000 分区时 ZK 成瓶颈）
3. KRaft 复用 Kafka 的存储引擎，运维更简单（一套系统而非两套）
4. Kafka 4.0 完全移除 ZK 后，部署和运维成本显著降低

### 6.9.6 Consul——multi-datacenter 支持

HashiCorp Consul 使用 Raft 做服务发现和 KV 存储的共识层。

**Multi-datacenter 设计**：每个 datacenter 有独立的 Raft 集群——不同 datacenter 的 Raft 不互相通信。跨 datacenter 的服务发现通过 WAN Gossip 协议同步——不依赖 Raft。

**对比 etcd**：etcd 不原生支持 multi-datacenter（跨地域部署需要业务层处理）。Consul 的 multi-datacenter 设计更适合全球分布式场景。

---

## 6.10 本章总结

### 10 个核心要点

1. **Raft 的核心设计目标是可理解性**——不是更高效或更正确，而是更易理解和实现。通过问题分解、状态空间简化、强 Leader 模型实现。

2. **三个子问题独立解决**：领导人选举（谁来当 Leader）、日志复制（如何同步）、安全性（如何保证不丢）。Paxos 把这些问题耦合在 Prepare/Accept 两阶段中——Raft 拆开后更清晰。

3. **term 是 Raft 的逻辑时钟**——单调递增，标识 Leader 任期。对应 Paxos 的提案编号 n、ZAB 的 epoch、VR 的 view-number——term/epoch 概念源自 VR（1988）的 view-number；Paxos 的提案编号 n 是 Lamport 独立设计的平行方案。

4. **随机选举超时从概率上避免活锁**——每个 Follower 的超时时间随机（如 150-300ms），最先超时的先发起选举。这比 Paxos 的"两个 Proposer 互相抢占"更优雅。

5. **RequestVote RPC 携带 lastLogIndex 和 lastLogTerm**——Follower 只投给日志"至少和自己一样新"的 Candidate。这是选举限制，保证新 Leader 拥有所有已提交日志。

6. **日志匹配特性**：相同 (index, term) 的条目值相同且前序一致。由 AppendEntries 的 prevLogIndex/prevLogTerm 检查 + 归纳法保证。

7. **日志不一致时 Leader 强制覆盖 Follower**——通过 nextIndex 回退找到匹配点，然后 Leader 的条目覆盖 Follower 的不一致条目。比 Paxos 的 Prepare 探测更直接。

8. **Figure 8 问题——Leader 只能提交当前 term 的日志**。直接提交旧 term 的日志可能导致已"提交"的日志被覆盖。Leader 通过提交当前 term 的新条目来间接提交旧 term 的条目。

9. **Joint Consensus 解决成员变更**——两阶段过渡（Cold,new → Cnew），保证新旧配置的 majority 都有交集。单节点变更是简化方案——每次只增减一个节点。

10. **Raft 比 Paxos 更受欢迎是因为可理解性**——不是数学上更优，而是更容易写对、更容易维护。etcd/TiKV/Consul/Kafka KRaft 等新系统都选 Raft。

### 第7章预告

第7章将进入 Paxos 变种算法的世界——Fast Paxos 和 EPaxos。这些算法试图突破 Paxos/Raft 的"单 Leader"瓶颈，允许多个节点并发提案。我们将理解它们的优化思路和为什么工业落地有限。

---