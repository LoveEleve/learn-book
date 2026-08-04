# 第4章 分布式共识 · 02 — Raft

> **本篇定位**：第 4 章核心章节的第二篇，深入 Raft 算法的三子问题分解与工业实现。
>
> 第 4 章-01 详细推导了 Paxos——它是共识算法的理论基石，但**难理解、难实现**。Raft（Ongaro-Ousterhout 2014 USENIX ATC）正是为解决"可理解性"而生：**能力等价于 Paxos，但通过强 Leader + 三子问题分解，让算法易读、易实现、易维护**。
>
> Raft 今天是新系统主流共识算法之一——etcd / TiKV / CockroachDB / Nacos / SOFA-JRaft / Consul 在 KV / 注册中心 / 协调服务领域基于它（**注**：Spanner / Cassandra LWT / Kafka ISR / MySQL MGR / ZooKeeper 仍用 Paxos 路线）。**理解 Raft 是理解现代 KV / 注册中心 / 协调服务的钥匙**。
>
> 本篇不照搬书，而是从"为什么 Raft 比 Paxos 易理解"出发，按"设计哲学 → 三子问题 → 与 Paxos 对比 → 工业实现"的顺序展开。读完本篇，你将理解：
> 1. Raft 的"可理解性"不是抽象口号，而是具体的设计选择（强 Leader + 子问题分解 + 状态简化）
> 2. 三子问题（领导人选举 / 日志复制 / 安全性）各自解决什么，如何协作
> 3. Raft 与 Paxos 的能力等价性，以及 Raft 的工程优势
> 4. etcd / TiKV / Nacos / SOFA-JRaft 等工业实现的差异
>
> **跨书视角声明**：
> - **释慧利《深入理解分布式共识算法》第 6 章 Raft** 视角为主：算法推导，本篇主线
> - **唐伟志《深入理解分布式系统》第 4 章** 视角次之：理论 + Go 实现简化版
> - **江峰《分布式高可用算法》第 5 章 失败检测和选主 + 第 8 章 共识** 视角给形式化基础
> - **陈东明《分布式系统与一致性》第 9 章 CockroachDB** 视角给工业案例
>
> **写作顺序的依赖关系**：
> 1. 为什么需要 Raft（动机） → 2. 设计哲学 → 3. 领导人选举 → 4. 日志复制 → 5. 安全性 → 6. 与 Paxos 对比 → 7. 工业实现 → 8. 总结与 ZAB 演进动机
>
> **关键依赖约束**：三子问题必须按"选举 → 复制 → 安全性"顺序展开，因为日志复制依赖 Leader，安全性约束选举与复制。

---

## 1. 为什么需要 Raft？

### 1.1 Paxos 的困难

第 4 章-01 详细推导了 Paxos。Paxos 是共识算法的理论基石，但有三个显著困难：

**困难 1：理解困难**
- Lamport 1990 年的论文《The Part-Time Parliament》用希腊岛 Paxos 的寓言故事叙述，晦涩难懂
- 2001 年 Lamport 写了《Paxos Made Simple》简化版，但仍然被认为难懂
- 学生和工程师普遍反映"读完论文不知道怎么实现"

**困难 2：实现困难**
- Basic Paxos 给出单值共识，但工程需要 Multi-Paxos（日志复制）
- Multi-Paxos 的论文没完整给出——Leader 选举、日志压缩、成员变更、客户端去重等关键工程细节需要自己设计
- 不同团队的 Multi-Paxos 实现差异大（Chubby / Spanner / MGR 各自做了不同优化）

**困难 3：活锁问题**
- Basic Paxos 多 Proposer 竞争会产生活锁（第 4 章-01 第 5 节）
- 需要随机退避或 Leader 选举解决——但 Leader 选举本身是工程难题

### 1.2 Raft 的设计目标

**Raft**（Ongaro-Ousterhout 2014）的设计目标明确写在论文标题中：

> "In Search of an Understandable Consensus Algorithm"

**核心目标**：**可理解性（Understandability）**——让共识算法易理解、易实现、易维护。

**次要目标**：
- 能力等价于 Paxos（保证 Agreement + Validity + 最终 Termination）
- 工业可用（性能、可用性、可扩展性）

**关键认知**：Raft **不追求算法最小性**（Paxos 追求），**追求可理解性**。这意味着 Raft 可能在某些方面比 Paxos"冗余"，但这种冗余是为了易读。

### 1.3 Raft 的核心设计选择

为了可理解性，Raft 做了三个关键设计选择：

**选择 1：强 Leader 模型**
- Paxos 中 Leader 是优化（Multi-Paxos 可选），Raft 中 Leader 是必需
- 所有数据流经 Leader：客户端请求 → Leader → Follower
- 比 Paxos 的"多 Proposer"模型更清晰

**选择 2：三子问题分解**
- Raft 把共识分解为三个相对独立的子问题：
  1. **领导人选举**（Leader Election）：如何选 Leader、Leader 故障如何切换
  2. **日志复制**（Log Replication）：Leader 如何把日志复制到 Follower
  3. **安全性**（Safety）：如何保证日志一致性、不丢已提交条目
- 每个子问题独立可理解，降低复杂度

**选择 3：状态简化**
- 节点状态只有 3 种：Follower / Candidate / Leader（Paxos 没有显式状态机）
- 时间分 term（任期），term 单调递增——比 Paxos 的 Proposal ID 更易理解
- 日志双向一致性规则明确（第 5 节展开）

### 1.4 本篇的学习路径

```
第1节 为什么需要 Raft（本节）
   ↓
第2节 Raft 的设计哲学（强 Leader + 三子问题分解 + 状态简化）
   ↓
第3节 子问题 1：领导人选举（term + 随机超时 + RequestVote）
   ↓
第4节 子问题 2：日志复制（AppendEntries + committed index）
   ↓
第5节 子问题 3：安全性（选举限制 + 提交限制）
   ↓
第6节 Raft 与 Paxos 的对比
   ↓
第7节 Raft 的工业实现（etcd / TiKV / Nacos / SOFA-JRaft / CockroachDB）
   ↓
第8节 本篇总结与 ZAB 演进动机
```

带着"Raft 为可理解性而生"的认知进入第 2 节。

---

## 2. Raft 的设计哲学

### 2.1 强 Leader 模型

**Raft 的核心特征**：**所有数据流经 Leader**。

```
Client → Leader ←→ Follower
            ↑
         所有读写
```

**Leader 的职责**：
- 接受客户端请求
- 把请求追加到本地日志
- 复制日志到所有 Follower
- 决定日志何时提交（commit）
- 响应客户端

**Follower 的职责**：
- 接受 Leader 的 AppendEntries
- 被动同步日志
- 不直接接受客户端请求

**Candidate 的职责**（临时状态）：
- Leader 故障时，Follower 转为 Candidate 发起选举
- 选举胜出则成为 Leader，失败则回到 Follower

**强 Leader 的优势**：
- 数据流清晰：单点写入，避免冲突
- 客户端路由简单：所有请求走 Leader
- 日志一致性容易保证：Leader 串行化

**强 Leader 的代价**：
- Leader 是单点——Leader 故障时服务短暂不可用（直到选举完成）
- 写入吞吐受 Leader 能力限制
- 跨地域写入延迟高（必须等多数派 ACK）

### 2.2 三子问题分解

Raft 把共识分解为三个相对独立的子问题：

| 子问题 | 解决什么 | 关键机制 |
|--------|---------|---------|
| **领导人选举** | 选出唯一 Leader，Leader 故障时切换 | term + 随机选举超时 + RequestVote RPC |
| **日志复制** | Leader 把日志复制到 Follower | AppendEntries RPC + committed index |
| **安全性** | 保证日志一致性、不丢已提交条目 | 选举限制 + 提交限制 |

**三个子问题的依赖关系**：
- 日志复制依赖领导人选举（先有 Leader 才能复制）
- 安全性约束选举和复制（选举限制保证新 Leader 有完整日志，提交限制保证不丢已提交条目）
- 因此本篇按"选举 → 复制 → 安全性"顺序展开

### 2.3 状态简化

**节点状态机**（3 状态）：

```
        超时，发起选举
  Follower ─────────────→ Candidate
     ↑                       │
     │                       │ 获得多数派选票
     │                       ↓
     │                    Leader
     │                       │
     │                       │ 发现更高 term 的 Leader
     └───────────────────────┘
```

**时间分 term**（任期）：
- term 是单调递增的整数
- 每个 term 最多一个 Leader（可能没有，如选举失败）
- 节点切换 term 时，旧 term 的消息被忽略

**term vs Paxos Proposal ID**：
- term 更简单：整数递增，无 node_id 区分
- term 有时间含义：每个 term 是一段"任期"
- Proposal ID 是"提议编号"，term 是"任期编号"——后者更易理解

### 2.4 Raft 的持久化状态

每个节点维护持久化状态（崩溃后从磁盘恢复）：

```
currentTerm: 当前 term（默认 0）
votedFor: 当前 term 投票给谁（默认 null）
log[]: 日志条目数组（每条含 term、index、command）
```

**为什么必须持久化？**
- `currentTerm`：防止崩溃后用旧 term 发起选举
- `votedFor`：防止崩溃后重复投票（一个 term 只能投一次）
- `log[]`：防止崩溃后丢失已接受的日志（破坏不变量）

**对比 Paxos**：Paxos 持久化 `promised_id` / `accepted_id` / `accepted_value`，Raft 持久化 `currentTerm` / `votedFor` / `log[]`——后者更结构化（日志数组 vs 单值）。

### 2.5 Raft 的易失状态

每个节点维护易失状态（内存，崩溃后丢失）：

```
commitIndex: 已提交的最大日志 index（默认 0）
lastApplied: 已应用到状态机的最大日志 index（默认 0）
```

**Leader 额外维护**：
```
nextIndex[]: 每个 Follower 的下一条要发送的日志 index（默认 Leader last log index + 1）
matchIndex[]: 每个 Follower 已确认复制的最大日志 index（默认 0）
```

**为什么这些是易失的？**
- `commitIndex` 和 `lastApplied` 可以从日志重建（重新计数）
- `nextIndex` 和 `matchIndex` 可以重新初始化（Leader 上任时设默认值）

### 2.6 本节小结

Raft 的设计哲学：
- **强 Leader**：所有数据流经 Leader，模型清晰
- **三子问题分解**：选举 / 复制 / 安全性，各自独立可理解
- **状态简化**：3 状态 + term + 结构化日志

带着这个设计哲学进入第 3 节——领导人选举。

---

## 3. 子问题 1：领导人选举

### 3.1 选举的核心问题

领导人选举回答一个问题：**如何选出唯一 Leader，且 Leader 故障时能切换？**

这个问题包含两个子问题：
1. **故障检测**：如何知道 Leader 故障了？
2. **选举过程**：如何选出唯一新 Leader，避免脑裂？

### 3.2 故障检测：心跳 + 选举超时

**机制**：
- Leader 定期发送心跳（AppendEntries RPC，空日志）给所有 Follower
- Follower 维护一个**选举超时**（election timeout）
- 如果 Follower 在选举超时内未收到 Leader 心跳，认为 Leader 故障，发起选举

**选举超时的选择**：
- 太短：误判 Leader 故障，频繁选举
- 太长：真故障时延迟高
- 工程经验：通常是心跳间隔的 10-20 倍
- etcd 默认：心跳 100ms，选举超时 1000ms
- 关键：**选举超时要远大于网络往返时间**，避免网络抖动触发误判

**为什么不用确定性超时？**
- 如果所有 Follower 同时超时，会同时发起选举，分裂投票
- Raft 用**随机化选举超时**解决：每个 Follower 的超时在 [T, 2T] 区间内随机
- 这样大概率只有一个 Follower 先超时，发起选举并胜出

### 3.3 选举过程：RequestVote RPC

**Follower 转为 Candidate 的流程**：

1. **增 term**：`currentTerm++`
2. **投票给自己**：`votedFor = self`（持久化）
3. **重置选举超时**：防止再次超时
4. **发送 RequestVote RPC** 给所有节点

**RequestVote RPC 参数**：
```
term: Candidate 的 currentTerm
candidateId: Candidate 自己的 ID
lastLogIndex: Candidate 最后一条日志的 index
lastLogTerm: Candidate 最后一条日志的 term
```

**RequestVote RPC 响应**：
```
term: 响应节点的 currentTerm（用于 Candidate 发现更高 term）
voteGranted: 是否投票
```

### 3.4 投票规则

**Acceptor（被请求投票的节点）的规则**：

```
收到 RequestVote(term, candidateId, lastLogIndex, lastLogTerm):
  if term < currentTerm:
    return voteGranted=false  # 拒绝旧 term

  if term > currentTerm:
    currentTerm = term  # 更新 term
    votedFor = null  # 重置投票

  # 关键：每个 term 只能投一次票
  if votedFor == null or votedFor == candidateId:
    # 检查 Candidate 的日志是否至少和自己一样新（选举限制，第 5 节详解）
    if candidate_log_is_up_to_date(lastLogIndex, lastLogTerm):
      votedFor = candidateId  # 持久化
      return voteGranted=true

  return voteGranted=false
```

**`candidate_log_is_up_to_date` 规则**（选举限制的直觉表述，第 5.2 节严格证明）：
- 比较 Candidate 的最后一条日志和自己的最后一条日志
- 先比 term：Candidate 的 lastLogTerm > 自己的 lastLogTerm → 更新
- term 相同：Candidate 的 lastLogIndex >= 自己的 lastLogIndex → 更新

### 3.5 Candidate 的三种结局

发起选举后，Candidate 可能出现三种结局：

**结局 1：胜出，成为 Leader**
- 收到多数派（≥ ⌈(N+1)/2⌉）选票
- 转为 Leader，立即发送心跳（宣布自己上任）

**结局 2：输给其他 Candidate**
- 收到其他 Leader 的 AppendEntries（term >= 自己的 currentTerm）
- 转回 Follower

**结局 3：分裂投票，无人胜出**
- 多个 Candidate 同时发起选举，没人拿到多数派
- 选举超时后，重新发起选举（随机超时避免再次分裂）

### 3.6 选举的安全性保证

**问题**：如何保证一个 term 最多一个 Leader？

**答案**：**每个节点在一个 term 内只能投一次票**（`votedFor` 持久化）。

**证明**：
- 假设 term T 有两个 Leader L1 和 L2
- L1 必须收到多数派 M1 的选票
- L2 必须收到多数派 M2 的选票
- M1 和 M2 必有交集（鸽笼原理）
- 交集中的节点既投给 L1 又投给 L2——但每个节点一个 term 只能投一次，矛盾
- 因此一个 term 最多一个 Leader

**这是 Raft 的核心 safety 保证**——通过"每 term 一票"+ 多数派，保证 Leader 唯一性。

### 3.7 选举的活锁问题

**问题**：如果分裂投票反复发生，永远选不出 Leader 怎么办？

**Raft 的解决**：**随机化选举超时**。

**为什么随机化有效？**
- 假设 N 个节点，选举超时在 [T, 2T] 内均匀随机
- 大概率只有一个节点先超时，发起选举并胜出
- 即使分裂，下一轮超时再次随机，最终会有人胜出
- 类似 CSMA/CD（以太网冲突检测）——随机退避是分布式避免冲突的经典方法

**与 FLP 的关系**：
- 随机化绕过了 FLP 的"确定性"前提（第 2 章 5.4 节）
- 在部分同步假设下（网络最终恢复），Raft 保证最终能选出 Leader
- 理论上可能"卡住"（持续分裂），但概率极低

### 3.8 选举的工程参数

| 参数 | etcd 默认 | ZooKeeper（ZAB）默认 | 说明 |
|------|----------|---------------------|------|
| 心跳间隔 | 100ms | tickTime=2000ms | Leader 发心跳的频率 |
| 选举超时 | 1000ms | initLimit=10×tickTime=20s | Follower 等待心跳的超时 |
| 随机区间 | [1000ms, 2000ms]（典型） | 类似 | 避免分裂投票 |

**跨地域集群**：超时必须放大。北京-上海 RTT 30ms，超时至少 100ms；中美 RTT 200ms，超时至少 500ms。**但实际跨地域生产集群通常设 5-10s**（远大于 RTT 下限），以容忍网络抖动和 GC 暂停。

### 3.9 跨书视角对照

| 作者 | 视角 | 核心论点 |
|------|------|---------|
| **释慧利** | 算法 | 第 6 章 Raft 选举的三子问题之一——term + 随机超时 + RequestVote RPC 是最小选举设计 |
| **江峰** | 形式化 | 第 5 章"失败检测和选主"——选举的正确性等价于"每 term 一票"+ 多数派公理 |
| **唐伟志** | 工程 | Go 实现中，选举超时随机化是关键——必须用 `rand.Intn(T)` 而非固定值 |
| **陈东明** | 案例 | etcd 用 Raft 选举，CockroachDB 用 Raft 选举（每个 Range 独立选 Leader） |

**4 视角差异与互补**：
- **释慧利 vs 江峰**：释慧利问"算法如何选 Leader"（RequestVote RPC），江峰问"选举为什么对"（多数派公理）
- **唐伟志 vs 陈东明**：唐伟志给"工程如何实现"（随机超时），陈东明给"工业如何用"（etcd/CockroachDB）
- **互补关系**：释慧利给"算法步骤"、江峰给"数学基础"、唐伟志给"工程细节"、陈东明给"工业案例"

### 3.10 本节小结

领导人选举是 Raft 的第一个子问题：
- **故障检测**：心跳 + 选举超时
- **选举过程**：Follower → Candidate → 发 RequestVote → 多数派胜出
- **安全性**：每 term 一票 + 多数派 → Leader 唯一
- **活锁解决**：随机化选举超时

带着这个选举机制进入第 4 节——日志复制。

---

## 4. 子问题 2：日志复制

### 4.1 日志复制的核心问题

日志复制回答一个问题：**Leader 如何把日志复制到所有 Follower，并保证所有副本日志一致？**

这个问题包含三个子问题：
1. **接收请求**：Leader 如何处理客户端请求
2. **复制日志**：Leader 如何把日志发给 Follower
3. **提交日志**：Leader 如何决定日志何时"安全提交"

### 4.2 日志结构

**日志条目（Log Entry）**：
```
{
  term: 写入时的 Leader term
  index: 日志位置（从 1 开始）
  command: 客户端命令
}
```

**日志示例**：
```
index:  1    2    3    4    5    6    7
term:   1    1    1    2    3    3    3
command: x=1 y=2 z=3 x=0 x=1 y=3 z=4
```

**关键不变量**（第 5.4 节严格证明，此处先给直觉）：
- 如果两条日志条目在相同 index 且 term 相同，则命令相同（Leader 一个 term 内一个 index 只写一条）
- 如果两条日志条目在相同 index 且 term 相同，则之前所有条目相同（AppendEntries 的 prevLogIndex 检查保证）

### 4.3 接收客户端请求

**Leader 的处理流程**：

1. 客户端发送命令 `cmd` 到 Leader
2. Leader 把 `cmd` 追加到本地日志：`log.append({term: currentTerm, index: lastLogIndex+1, command: cmd})`
3. Leader 持久化日志（WAL）
4. Leader 并行发送 AppendEntries RPC 给所有 Follower
5. Leader 等待多数派 ACK
6. Leader 提交日志（`commitIndex++`）
7. Leader 应用命令到状态机
8. Leader 响应客户端

**注意**：步骤 4-6 是异步的——Leader 不会等所有 Follower ACK，只等多数派。

### 4.4 AppendEntries RPC

**Leader 发送给 Follower 的 RPC**：

```
AppendEntries:
  term: Leader 的 currentTerm
  leaderId: Leader 的 ID
  prevLogIndex: 紧邻新日志之前的日志 index
  prevLogTerm: prevLogIndex 处日志的 term
  entries[]: 要追加的日志条目（心跳时为空）
  leaderCommit: Leader 的 commitIndex
```

**Follower 处理 AppendEntries**：

```
收到 AppendEntries(term, leaderId, prevLogIndex, prevLogTerm, entries, leaderCommit):
  if term < currentTerm:
    return success=false  # 拒绝旧 term 的 Leader

  if term > currentTerm:
    currentTerm = term  # 更新 term
    votedFor = null  # 重置投票

  # 日志一致性检查
  if prevLogIndex > 0:
    if log[prevLogIndex].term != prevLogTerm:
      return success=false  # 日志不一致，拒绝

  # 删除冲突日志，追加新日志
  for each entry in entries:
    if log[entry.index] exists and log[entry.index].term != entry.term:
      # 删除从 entry.index 开始的所有日志
      log = log[:entry.index - 1]  # 保留 index 1 到 entry.index-1（1-based index）
    log.append(entry)

  # 更新 commitIndex
  if leaderCommit > commitIndex:
    commitIndex = min(leaderCommit, lastNewEntryIndex)
    applyToStateMachine()

  return success=true
```

### 4.5 日志一致性检查

**关键机制**：`prevLogIndex` + `prevLogTerm` 的"前一条日志检查"。

**为什么需要这个检查？**
- Follower 的日志可能与 Leader 不一致（如 Follower 曾与旧 Leader 同步，错过了新 Leader 的部分日志）
- Leader 必须找到与 Follower 日志一致的"分叉点"，从那里开始覆盖

**检查逻辑**：
- Leader 在 AppendEntries 中带 `prevLogIndex` 和 `prevLogTerm`
- Follower 检查自己的 `log[prevLogIndex].term` 是否等于 `prevLogTerm`
- 如果不等：日志不一致，Follower 拒绝，Leader 重试时往前回退
- 如果等：日志一致，Follower 接受新日志

### 4.6 nextIndex 与日志回退

**Leader 维护每个 Follower 的 `nextIndex`**：下一条要发送的日志 index。

**初始化**：Leader 上任时，把所有 Follower 的 `nextIndex` 设为 `lastLogIndex + 1`（乐观假设 Follower 日志与 Leader 完全一致）。

**回退机制**：
- AppendEntries 失败（Follower 拒绝）→ Leader 把 `nextIndex--`，重发
- 重复直到找到一致点

**优化**（加速回退）：
- 标准回退是 `nextIndex--`（一次回退一条）
- 优化：Follower 在拒绝时返回"冲突的 term 起始 index"，Leader 直接跳到那里
- etcd / SOFA-JRaft 都实现了这个优化

### 4.7 日志提交

**提交规则**：
- Leader 收到多数派 ACK 后，可以提交该日志条目
- Leader 把 `commitIndex` 推进到该条目
- Leader 在后续 AppendEntries 中带 `leaderCommit`，通知 Follower 提交

**关键约束**（提交限制的直觉表述，第 5.3 节严格证明）：Leader 只能提交**当前 term** 的日志条目，不能提交旧 term 的条目（即使它们已被多数派复制）。这是为了防止"已提交的旧值被新 Leader 覆盖"——第 5.3 节给出反例与证明。

**为什么有这个约束？** 防止已提交的日志被覆盖——第 5.3 节详解。

### 4.8 日志应用

**应用规则**：
- `commitIndex` 推进后，节点把 `lastApplied` 推进到 `commitIndex`
- 期间把日志条目按序应用到状态机
- 应用是幂等的（同一命令多次应用结果相同）

**为什么应用是幂等的？**
- 客户端命令有唯一 ID，状态机去重
- 即使 Leader 重启后重新应用，结果相同

### 4.9 完整流程示例

考虑 5 节点集群（L, F1, F2, F3, F4），客户端发送 `x=1`：

```
t=1: Client → Leader L: cmd "x=1"
t=2: L 追加日志：log[5] = {term:3, index:5, command:"x=1"}
     L 持久化日志（WAL）
t=3: L 并行发送 AppendEntries(prevLogIndex=4, prevLogTerm=3, entries=[log[5]])
     给 F1, F2, F3, F4

t=4: F1, F2, F3 ACK: success=true（日志一致，已追加）
     F4 网络抖动，未及时响应

t=5: L 收到 3 个 ACK（含自己，多数派 3/5）
     L 推进 commitIndex=5
     L 应用 log[5] 到状态机：x=1
     L 响应 Client: 成功

t=6: L 在下一次心跳 AppendEntries 中带 leaderCommit=5
     F1, F2, F3 收到后推进 commitIndex=5，应用 log[5]
     F4 网络恢复后，L 重发 AppendEntries，F4 同步
```

**关键观察**：
- 写入延迟 = L 持久化时间 + 网络往返时间（多数派）
- F4 的延迟不影响写入（多数派即可）
- F4 最终会同步（最终一致性）

### 4.10 跨书视角对照

| 作者 | 视角 | 核心论点 |
|------|------|---------|
| **释慧利** | 算法 | 第 6 章 Raft 日志复制的"前一条日志检查"是关键——通过 prevLogIndex + prevLogTerm 保证日志一致性 |
| **江峰** | 形式化 | 日志复制的正确性等价于"日志不变量"——相同 index + term 的条目内容相同，且之前所有条目相同 |
| **唐伟志** | 工程 | Go 实现中，nextIndex 回退优化（Follower 返回冲突 term 起始 index）能显著加速日志同步 |
| **陈东明** | 案例 | etcd 每个 key 修改是一条日志；TiKV 每个 Region 是独立 Raft 组，日志复制粒度更细 |

**4 视角差异与互补**：
- **释慧利 vs 江峰**：释慧利问"算法如何保证一致"（prevLogIndex 检查），江峰问"一致性的数学基础"（日志不变量）
- **唐伟志 vs 陈东明**：唐伟志给"工程优化"（nextIndex 回退），陈东明给"工业粒度"（key vs Region）
- **互补关系**：释慧利给"算法机制"、江峰给"数学不变量"、唐伟志给"工程优化"、陈东明给"工业应用"

### 4.11 本节小结

日志复制是 Raft 的第二个子问题：
- **接收请求**：Leader 追加日志 + 持久化 + 并行复制
- **复制日志**：AppendEntries RPC + 前一条日志检查
- **日志回退**：nextIndex 递减（或优化：冲突 term 跳跃）
- **提交日志**：多数派 ACK + 提交限制（仅当前 term）
- **应用日志**：commitIndex 推进 + 状态机应用

带着这个日志复制机制进入第 5 节——安全性。

---

## 5. 子问题 3：安全性

### 5.1 安全性的核心问题

安全性回答一个问题：**如何保证已提交的日志不丢失，且所有副本日志最终一致？**

**为什么需要安全性约束？** 仅靠选举 + 复制不足以保证 safety。考虑以下场景：

```
场景：已提交日志被覆盖
t=1: Leader L1（term=1）写入 log[2]={term:1, "x=1"}，复制到 F1, F2（多数派），提交
t=2: L1 崩溃，F3 当选 Leader（term=2），F3 的日志没有 log[2]
t=3: F3 写入 log[2]={term:2, "y=2"}（覆盖了 L1 的 log[2]）
t=4: F3 复制 log[2] 到其他节点

结果：已提交的 "x=1" 丢失——违反 safety
```

**这个场景说明**：必须限制选举——只有日志完整的节点才能当选 Leader。这就是**选举限制**。

### 5.2 选举限制

**规则**：投票时，Acceptor 检查 Candidate 的日志是否**至少和自己一样新**。

**`candidate_log_is_up_to_date(lastLogIndex, lastLogTerm)` 实现**：
```
if lastLogTerm != my_lastLogTerm:
  return lastLogTerm > my_lastLogTerm  # 比 term
return lastLogIndex >= my_lastLogIndex  # term 相同比 index
```

**为什么这样能保证已提交日志不丢？**
- 已提交 = 多数派复制
- 任何新 Leader 必须获得多数派选票
- 新 Leader 的日志与多数派有交集（鸽笼原理）
- 交集节点必然投票给"日志至少和自己一样新"的 Candidate
- 因此新 Leader 必然包含已提交的日志

**证明**：
- 假设日志条目 `e`（term=T, index=I）已被提交，意味着多数派 M 复制了 e
- 任何新 Leader 必须收到多数派 M' 的选票
- M 与 M' 必有交集（鸽笼原理）
- 交集中的节点 N 既在 M（复制了 e）又在 M'（投票给新 Leader）
- N 投票给新 Leader，意味着新 Leader 的日志至少和 N 一样新
- 由于 N 有 e（term=T, index=I），新 Leader 的日志必须包含 e 或更高 term 的同 index 条目
- 因此新 Leader 不会丢失 e

**这就是选举限制的核心价值**——通过"日志至少一样新"+ 多数派，保证已提交日志不丢。

### 5.3 提交限制

**规则**：Leader 只能提交**当前 term** 的日志条目。

**为什么有这个约束？** 考虑以下反例：

```
场景：提交旧 term 的日志导致丢数据
t=1: Leader L1（term=1）写入 log[2]={term:1, "x=1"}，复制到 F1（未达多数派，未提交）
t=2: L1 崩溃，L2（term=2）上任，L2 写入 log[2]={term:2, "y=2"}，复制到 F2，提交
t=3: L2 崩溃，L3（term=3）上任，L3 看到 log[2] 在 F1（term=1）和 F2（term=2）不一致

如果 L3 提交 log[2]={term:1, "x=1"}（基于 F1 的多数派复制），会覆盖已提交的 "y=2"——违反 safety
```

**Raft 的解决**：Leader 只能提交当前 term 的日志。当 Leader 提交当前 term 的日志时，所有之前的日志也间接被提交（因为日志不变量保证之前日志一致）。

**图示**：
```
Leader L3（term=3）的日志：
index:  1    2    3
term:   1    2    3
cmd:    x=1  y=2  z=3

L3 不能直接提交 log[1] 或 log[2]（旧 term）
L3 提交 log[3]（当前 term）时，log[1] 和 log[2] 自动提交
```

### 5.4 日志不变量

**Raft 的核心不变量**（保证 safety）：

> **不变量 1**：如果两条日志条目在相同 index 且 term 相同，则命令相同。
>
> **不变量 2**：如果两条日志条目在相同 index 且 term 相同，则之前所有条目相同。

**为什么成立？**
- 不变量 1：Leader 一个 term 内一个 index 只写一条日志（串行化）
- 不变量 2：AppendEntries 的 prevLogIndex + prevLogTerm 检查——只有前一条一致才接受新条目

**这两个不变量保证了**：
- 不同副本的日志不会"分叉"
- 已提交的日志不会被覆盖

### 5.5 状态机安全性质

**Raft 的最终 safety 保证**（State Machine Safety）：

> 如果某节点在 index i 应用了日志条目 e，则没有任何其他节点会在 index i 应用不同的日志条目。

**这个性质保证了**：所有副本的状态机最终一致。

**具体反例（帮助理解）**：假设节点 A 在 index=2 应用了 `x=1`（term=1），节点 B 能否在 index=2 应用 `y=2`（term=2）？

- 如果能，状态机不一致——违反 safety
- Raft 证明这不可能：
  - A 应用 `x=1`（term=1）→ `x=1` 已被提交 → 多数派 M 复制了 `x=1`
  - B 的 Leader 必须获得多数派 M' 选票
  - M 与 M' 必有交集（鸽笼原理）
  - 交集节点既复制了 `x=1` 又投票给 B 的 Leader
  - 选举限制要求 B 的 Leader 日志至少和交集节点一样新 → B 的 Leader 必含 `x=1`（term=1，index=2）
  - 由日志不变量，B 在 index=2 必须是 `x=1`，不可能是 `y=2`（term=2）

**一般化证明**（基于不变量 1+2 + 选举限制 + 提交限制）：
- 假设节点 A 在 index i 应用了 e（term=T）
- 任何其他节点 B 在 index i 应用 e'（term=T'）
- 如果 T = T'：由不变量 1，e = e' ✓
- 如果 T < T'：e 已被 A 应用 → e 已被提交 → e 已被多数派复制 → B 的 Leader 必然包含 e（选举限制）→ B 在 index i 必须有 e，不可能有 e' → 矛盾
- 如果 T > T'：同理矛盾

### 5.6 安全性 vs Termination

**Raft 保证**：
- **Safety**（Agreement + Validity）：始终成立
- **Termination**（liveness）：在部分同步假设下最终成立

**与 FLP 的关系**：
- Raft 通过随机化绕过 FLP 的"确定性"前提
- 通过部分同步假设保证最终能选出 Leader
- 理论上可能"卡住"（持续分裂），但概率极低

### 5.7 跨书视角对照

| 作者 | 视角 | 核心论点 |
|------|------|---------|
| **释慧利** | 算法 | 第 6 章 Raft 安全性的两个限制（选举限制 + 提交限制）是 Raft 区别于 Basic Paxos 的关键 |
| **江峰** | 形式化 | 安全性的形式化等价于"状态机安全性质"——所有副本最终应用相同的日志序列 |
| **唐伟志** | 工程 | Go 实现中，提交限制是最容易出 bug 的地方——必须严格只提交当前 term 的日志 |
| **陈东明** | 案例 | etcd / TiKV / CockroachDB 都依赖 Raft 的状态机安全性质，保证数据一致 |

**4 视角差异与互补**：
- **释慧利 vs 江峰**：释慧利问"算法如何保证 safety"（两个限制），江峰问"safety 的形式化表述"（状态机安全性质）
- **唐伟志 vs 陈东明**：唐伟志给"工程实现难点"（提交限制易出 bug），陈东明给"工业依赖"（数据一致基础）
- **互补关系**：释慧利给"算法机制"、江峰给"形式化性质"、唐伟志给"实现难点"、陈东明给"工业依赖"

### 5.8 本节小结

安全性是 Raft 的第三个子问题：
- **选举限制**：投票时要求 Candidate 日志至少和自己一样新——保证已提交日志不丢
- **提交限制**：Leader 只能提交当前 term 的日志——防止旧 term 日志覆盖
- **日志不变量**：相同 index + term 的条目内容相同，且之前所有条目相同
- **状态机安全性质**：所有副本最终应用相同日志序列

**这三个机制共同保证 Raft 的 safety**——已提交日志永不丢失，所有副本最终一致。

**至此，Raft 的三子问题（选举 / 复制 / 安全性）已全部展开**。下一节把 Raft 与 Paxos 做系统对比，理解两者的能力等价性与设计哲学差异。

---

## 6. Raft 与 Paxos 的对比

### 6.1 能力等价性

**Raft 与 Multi-Paxos 的能力等价**：
- 都在部分同步 + Crash-recovery + 2f+1 节点模型下工作
- 都保证 Agreement + Validity + 最终 Termination
- 都基于多数派 + Leader + 日志复制
- 都支持日志复制（SMR）

**差异在于设计哲学**：
- Paxos 追求"算法最小性"
- Raft 追求"可理解性"

### 6.2 核心机制对比

| 维度 | Multi-Paxos | Raft |
|------|-------------|------|
| Leader 角色 | 可选优化 | 必需设计 |
| 算法结构 | Prepare + Accept 两阶段 | 三子问题分解（选举/复制/安全性） |
| 提案编号 | Proposal ID（round + node_id） | term（整数递增） |
| 日志一致性 | 隐式（Prepare 阶段保证） | 显式（prevLogIndex + prevLogTerm 检查） |
| 提交规则 | 论文未明确约束（工程实现需自行处理图 8 类问题） | 明确约束：只能提交当前 term 的条目 |
| 选举 | 未明确给出（工程自己实现） | 内置（RequestVote RPC + 随机超时） |
| 成员变更 | 论文未明确（工程各自实现，如 Chubby α-reconfig） | 论文给出联合共识 + 单节点变更两种方案 |
| 日志压缩 | 未明确给出（工程自己实现） | Snapshot（内置） |

### 6.3 易理解性对比

**Paxos 难理解的原因**：
1. 角色抽象（Proposer/Acceptor/Learner）与实际部署不直观
2. Proposal ID 与日志 index 混用
3. Prepare 阶段的"承诺"语义不直观
4. Multi-Paxos 的工程细节未在论文中明确

**Raft 易理解的原因**：
1. 强 Leader 模型，数据流清晰
2. 三子问题分解，每个子问题独立可理解
3. term 概念直观（任期编号）
4. 日志一致性规则明确（prevLogIndex 检查）
5. 选举、日志压缩、成员变更都内置在论文中

### 6.4 工程实现对比

**Paxos 实现的难点**：
1. Multi-Paxos 论文不完整，工程细节需自己设计
2. 不同团队的实现差异大（Chubby / Spanner / MGR 各不同）
3. 选举 + 日志压缩 + 成员变更都要自己实现

**Raft 实现的优势**：
1. 论文给出完整算法（选举/复制/安全性/压缩/成员变更）
2. 不同团队的实现相对一致
3. 有大量开源参考实现（etcd / hashicorp/raft / SOFA-JRaft）

### 6.5 工业采用对比

| 系统 | 算法 | 采用原因 |
|------|------|---------|
| Chubby | Multi-Paxos | 历史原因（Raft 未出生） |
| Spanner | Multi-Paxos | 历史原因 + 全球规模优化 |
| MySQL MGR | Paxos 变种（XCom） | 历史原因 |
| etcd | Raft | 易实现 + 易维护 |
| TiKV | Raft | 易实现 + 多 Raft 组 |
| CockroachDB | Raft | 易实现 + 跨地域 |
| Nacos | Raft（持久实例） | 易实现 + 与 Spring Cloud 集成 |
| Consul | Raft | 易实现 + 健康检查集成 |
| SOFA-JRaft | Raft | 蚂蚁开源，Java 实现 |

**趋势**：现代系统倾向 Raft（易实现易维护），但 Paxos 仍在历史系统和大规模场景中使用。

### 6.6 跨书视角对照

| 作者 | 视角 | 核心论点 |
|------|------|---------|
| **释慧利** | 算法 | Raft 与 Paxos 能力等价，差异在设计哲学（可理解性 vs 最小性） |
| **江峰** | 形式化 | 两者都满足共识三性质，数学上等价 |
| **唐伟志** | 工程 | 工程上 Raft 更易实现，论文完整度是关键差异 |
| **陈东明** | 案例 | 现代系统倾向 Raft，但 Paxos 仍在历史系统中使用 |

**4 视角差异与互补**：
- **释慧利 vs 江峰**：释慧利问"算法差异在哪"（设计哲学），江峰问"数学上是否等价"（共识三性质）
- **唐伟志 vs 陈东明**：唐伟志给"工程实现差异"（论文完整度），陈东明给"工业采用趋势"（Raft 主流）
- **互补关系**：释慧利给"设计哲学"、江峰给"数学等价性"、唐伟志给"工程实现"、陈东明给"工业趋势"

### 6.7 本节小结

Raft 与 Paxos 能力等价，差异在设计哲学：
- **Paxos**：算法最小性，难理解难实现
- **Raft**：可理解性，易实现易维护

**新系统在 KV / 注册中心 / 协调服务领域倾向 Raft**——etcd / TiKV / CockroachDB / Nacos / Consul / SOFA-JRaft 都基于它（**注**：Spanner / Cassandra LWT / Kafka ISR / MySQL MGR / ZooKeeper 仍用 Paxos 路线）。

**下一节看 Raft 在工业界的具体实现**——etcd / TiKV / CockroachDB / Nacos / SOFA-JRaft / Consul 各自做了哪些工程优化。

---

## 7. Raft 的工业实现

### 7.1 etcd

**etcd**：CoreOS 开源的分布式 KV 存储，Kubernetes 的元数据底座。

**核心设计**：
- 单 Raft 组（整个集群一个 Raft 实例）
- 所有 key 修改是一条 Raft 日志
- 强一致读写（默认 ReadIndex Read，3.4+ Follower 也可服务线性一致读；可选 Lease Read 优化）

**关键参数**：
- 心跳间隔：`--heartbeat-interval=100`（ms）
- 选举超时：`--election-timeout=1000`（ms）
- 数据目录：`/var/lib/etcd/`（含 `member/wal/` + `member/snap/`）
- 默认集群大小：3 / 5 / 7 节点

**etcd 的工程优化**：
- **WAL + Snapshot**：WAL 持久化日志，定期 Snapshot 压缩
- **MVCC**：key 的多版本（用于 watch）
- **Lease**：TTL 机制（用于临时节点）
- **Compact**：定期清理旧版本

### 7.2 TiKV

**TiKV**：PingCAP 开源的分布式 KV 存储，TiDB 的存储底座。

**核心设计**：
- **多 Raft 组**（每个 Region 一个独立 Raft 实例）
- Region 是数据分片单元（默认 96MB，5.x+ 推荐 256MB）
- 不同 Region 的 Leader 可分布在不同节点（负载均衡）

**多 Raft 组的工程挑战**：
- **批量心跳**：单节点可能同时是数千个 Region 的 Leader，心跳合并发送
- **批量日志复制**：多个 Region 的日志合并发送
- **PD（Placement Driver）**：调度 Region 分布、负载均衡

**TiKV 的优化**：
- **Raft Engine**：专用 Raft 日志引擎（替代 RocksDB）
- **Region Split / Merge**：自动分裂 / 合并
- **Region Replica Migration**：在线迁移副本

### 7.3 CockroachDB

**CockroachDB**：开源的全球分布式 SQL 数据库，受 Spanner 启发。

**核心设计**：
- 多 Raft 组（每个 Range 一个 Raft 实例，类似 TiKV）
- HLC（混合逻辑时钟）替代 TrueTime
- 跨地域多副本

**CockroachDB 的特色**：
- **Range**：数据分片单元（默认 64MB；21.x+ 支持自动调整，**待 MCP 核实当前默认值**）
- **Leaseholder**：每个 Range 有一个 Leaseholder（不一定是 Raft Leader），处理读请求
- **HLC**：跨 Range 事务排序

**与 TiKV 的差异**：
- TiKV 用单时间戳（TSO，TiDB 集中分配）
- CockroachDB 用 HLC（去中心化）

### 7.4 Nacos

**Nacos**：阿里开源的注册中心 + 配置中心。

**核心设计**：
- **双协议**：Raft（CP，持久实例）+ Distro（AP，临时实例）
- 持久实例走 Raft（强一致）
- 临时实例走 Distro（最终一致）

**Nacos Raft 的实现演进**（版本差异需注意）：
- **1.3 前**：自研简化版 Raft（`CPProtocol` + `PersistentServiceProcessor`），非基于 SOFA-JRaft
- **1.4+**：引入 SOFA-JRaft 用于持久实例协议（`PersistentClientOperationServiceImpl`），与早期自研实现共存或替换
- 持久实例存储在 Raft 日志
- 选举超时：`raft.electionTimeout=5000`（ms，默认，**待 MCP 核实 2.x 是否调整**）
- 心跳间隔：`raft.heartbeatPeriodMs=500`（ms，默认，**待 MCP 核实**）

> **待 MCP 核实声明**：Nacos 2.x 引入 gRPC 长连接后，Distro/Raft 协议路由实现有调整，但持久/临时实例的协议边界保留。具体路由代码在 `naming/consistency/persistent` 与 `naming/consistency/ephemeral` 包。需 grep `PersistentClientOperationServiceImpl` 确认 2.x 的 Raft 实现来源。

### 7.5 SOFA-JRaft

**SOFA-JRaft**：蚂蚁开源的 Java Raft 实现。

**核心设计**：
- 纯 Java 实现（基于 Raft 论文）
- 工业级生产可用

**特点**：
- **Raft Log**：基于 RocksDB 的日志存储
- **Snapshot**：定期快照压缩
- **状态机**：用户实现 `StateMachine` 接口
- **ReadIndex + Lease Read**：线性一致读优化

**应用场景**：
- 蚂蚁内部多个系统（如分布式锁、配置中心）
- Nacos 持久实例的 Raft 实现（1.4+ 基于 SOFA-JRaft，1.3 前自研简化版）
- RheaDB（基于 SOFA-JRaft 的分布式 KV）

### 7.6 Consul

**Consul**：HashiCorp 开源的服务发现 + 配置中心。

**核心设计**：
- 单 Raft 组（类似 etcd）
- 强一致读写

**与 etcd 的差异**：
- Consul 强调服务发现 + 健康检查（etcd 是纯 KV）
- Consul 用 Go 写，etcd 也用 Go 写
- Consul 有原生 multi-datacenter 支持（etcd 需要额外工具）

### 7.7 工业实现对比

| 系统 | Raft 组 | 分区 | Leader 读 | 工程优化 |
|------|---------|------|----------|---------|
| etcd | 单 Raft 组 | 单分片 | Leader Read / Lease Read | MVCC + Watch |
| TiKV | 多 Raft 组 | Range（96MB/256MB） | Leader Read | 批量心跳 + Raft Engine |
| CockroachDB | 多 Raft 组 | Range（64MB） | Leaseholder Read | HLC + 跨地域 |
| Nacos | 单 Raft 组 | 单分片 | Leader Read | Distro 双协议 |
| SOFA-JRaft | 用户定义 | 用户定义 | ReadIndex + Lease Read | 通用框架 |
| Consul | 单 Raft 组 | 单分片 | Leader Read | multi-datacenter |

### 7.8 选型建议

| 业务需求 | 推荐系统 | 理由 |
|---------|---------|------|
| K8s 元数据 | etcd | K8s 原生支持 |
| 大规模 KV（PB 级） | TiKV | 多 Raft 组 + 自动分片 |
| 全球 SQL | CockroachDB | HLC + 跨地域 |
| 微服务注册 + 配置 | Nacos | 双协议 + Spring Cloud 集成 |
| 通用 Raft 框架 | SOFA-JRaft | Java 可嵌入 |
| 服务发现 + 健康检查 | Consul | 原生支持 |

### 7.9 跨书视角对照

| 作者 | 视角 | 核心论点 |
|------|------|---------|
| **释慧利** | 算法 | 工业实现的差异在多 Raft 组 + 分区策略，核心算法都是标准 Raft |
| **江峰** | 形式化 | 工业实现必须保证"Leader 唯一性"+"日志不变量"两个 safety 性质 |
| **唐伟志** | 工程 | Go 实现中，WAL + Snapshot + ReadIndex 是关键工程组件 |
| **陈东明** | 案例 | etcd 是 Raft 工业化标杆；CockroachDB 把 Raft 扩展到全球规模 |

### 7.10 本节小结

Raft 的工业实现（etcd / TiKV / CockroachDB / Nacos / SOFA-JRaft / Consul）都遵循"标准 Raft + 工程优化"的模式。差异在于：
- **单 Raft 组 vs 多 Raft 组**：etcd 单组，TiKV/CockroachDB 多组
- **分区策略**：单分片 / Range / 用户定义
- **读优化**：Leader Read / Lease Read / ReadIndex

**理解标准 Raft 是理解这些工业实现的前提**——所有优化都建立在 Raft 的算法基础之上。

**下一节总结本篇核心认知，并引出下一篇 ZAB**——ZooKeeper 的原子广播协议，与 Paxos/Raft 形成共识算法三足鼎立。

---

## 8. 本篇总结与 ZAB 演进动机

### 8.1 Raft 的核心成就

Raft 完成了共识算法工程化的关键突破：
1. **第一个为可理解性设计的共识算法**：三子问题分解 + 强 Leader + 状态简化
2. **论文完整**：选举/复制/安全性/压缩/成员变更都明确给出
3. **工业主流**：etcd / TiKV / CockroachDB / Nacos / Consul / SOFA-JRaft 全部基于它

### 8.2 Raft 的三子问题

| 子问题 | 解决什么 | 关键机制 |
|--------|---------|---------|
| 领导人选举 | 选出唯一 Leader | term + 随机超时 + RequestVote |
| 日志复制 | Leader 把日志复制到 Follower | AppendEntries + prevLogIndex 检查 |
| 安全性 | 保证已提交日志不丢 | 选举限制 + 提交限制 |

### 8.3 Raft 与 Paxos 的关系

**关系**：
- **算法本质相同**：都基于多数派 + Leader + 日志复制
- **设计哲学不同**：Paxos 追求最小性，Raft 追求可理解性
- **能力等价**：都保证 Agreement + Validity + 最终 Termination
- **工程选择**：现代系统倾向 Raft（易实现易维护）

### 8.4 ZAB 的演进动机

**ZAB**（ZooKeeper Atomic Broadcast，2008）是 ZooKeeper 的共识协议，比 Raft 早 6 年。

**ZAB 的设计动机**：
- ZooKeeper 需要一个"原子广播"协议（所有副本按相同顺序接收事务）
- ZAB 不是从"共识问题"出发，而是从"主从原子广播"出发
- 与 Raft 的强 Leader 模型类似，但设计更早

**ZAB 与 Raft 的关系**：
- 都是强 Leader 模型
- 都基于多数派
- ZAB 的 epoch 类似 Raft 的 term
- ZAB 的核心是"原子广播"，Raft 的核心是"共识"——数学上等价，工程上有差异

**下一篇将详解 ZAB**，对比它与 Paxos/Raft 的异同。

### 8.5 后续篇章路径

```
第4章-01 Paxos（已完成）
   ↓
第4章-02 Raft（本篇）
   ↓
第4章-03 ZAB（下一篇）
   - 原子广播协议
   - 与 Paxos/Raft 的差异
   - ZooKeeper 的工程实现
   ↓
第4章-04 对比与工业实现
   - 三大算法对比矩阵
   - etcd / ZooKeeper / TiKV / Spanner 工业实现
   - 选型建议
```

### 8.6 关键认知

带着以下 3 个认知进入下一篇（ZAB）：

**认知 1：Raft 是为可理解性而生的共识算法**
- 强 Leader + 三子问题分解 + 状态简化
- 不是新算法，而是 Paxos 的工程化重新设计

**认知 2：三子问题是 Raft 的核心结构**
- 领导人选举（term + 随机超时 + RequestVote）
- 日志复制（AppendEntries + prevLogIndex 检查）
- 安全性（选举限制 + 提交限制）
- 三个子问题协作保证 safety + liveness

**认知 3：Raft 是新系统主流共识算法之一**
- etcd / TiKV / CockroachDB / Nacos / Consul / SOFA-JRaft 在 KV / 注册中心 / 协调服务领域基于它
- **注**：Spanner / Cassandra LWT / Kafka ISR / MySQL MGR / ZooKeeper 仍用 Paxos 路线
- Paxos 仍在历史系统和大规模数据库场景中使用，新系统在 KV/协调服务领域倾向 Raft

---

## 结语：从理论到工业的完整链条

回顾全篇，我们完成了 Raft 的完整推导：

- **第 1 节**：从 Paxos 困难推导 Raft 的可理解性目标
- **第 2 节**：Raft 的设计哲学（强 Leader + 三子问题分解 + 状态简化）
- **第 3 节**：领导人选举（term + 随机超时 + RequestVote）
- **第 4 节**：日志复制（AppendEntries + prevLogIndex 检查 + 提交规则）
- **第 5 节**：安全性（选举限制 + 提交限制 + 日志不变量）
- **第 6 节**：与 Paxos 对比（能力等价，设计哲学不同）
- **第 7 节**：工业实现（etcd / TiKV / CockroachDB / Nacos / SOFA-JRaft / Consul）
- **第 8 节**：总结与 ZAB 演进动机

**本篇的核心论断**：Raft 不是新算法，而是 Paxos 的工程化重新设计——通过强 Leader + 三子问题分解 + 状态简化，让共识算法易理解、易实现、易维护。理解 Raft 是理解现代 KV / 注册中心 / 协调服务（etcd / TiKV / CockroachDB / Nacos）的钥匙。

**带着这一框架进入下一篇**——ZAB，我们将看到 ZooKeeper 如何从"原子广播"角度设计共识协议，与 Paxos/Raft 形成三足鼎立。
