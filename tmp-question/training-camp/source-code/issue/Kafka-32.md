# Kafka-32. 谁有资格当 leader、怎么投票、怎么提交——KRaft 的 Raft 状态机与 voter 集合主链

> 场景：Kafka-8 讲了 KRaft 的总体架构，Kafka-26 讲了 QuorumController 如何消费元数据日志。但 Raft 本身那一层——谁有资格投票、`EpochElection` 怎么过半、`VoterSetHistory` 怎么跟踪 voter 集合变化、`KafkaRaftClient` 怎么处理选主/提交——还没有正面展开。本篇把 KRaft 的 Raft 状态机讲透。这是 K-8 KRaft 域第 4 篇。

## 先把真正的困惑摆出来：一台机器怎么知道自己有没有资格当 leader

KRaft 的 metadata quorum 不是“所有 broker 都能投票”。Kafka 的 Raft 实现区分两种角色：

- **voter**：参与投票、参与 quorum，决定谁能成为 Raft leader；
- **observer**：不参加投票，只追踪日志和 snapshot。

`KafkaRaftClient` 的类注释说得很直白：这个协议区分 voters 与 observers，只有 voters 有资格处理“参与 quorum”的协议请求、参与选举。需要补一个边界：这里的“协议请求”特指投票、leader 相关这类影响 quorum 一致性的请求。observer 仍然可以 fetch 日志、读取数据——它只是不能投票、不能当 leader，不代表它完全读不到元数据。

`VoterSet` 定义了当前 quorum 的成员。当 leader 需要提交一条记录时，它必须等多数 voter 确认。`VoterSetHistory` 则跟踪 voter 集合在日志历史中的变化——因为 voter 集合本身也可以动态修改（通过 `VotersRecord` 写入日志）。

```text
voter set
  → 决定“谁有投票权”
    → 选举时过半选民同意
      → 成为 leader
        → 写日志
          → 等多数 voter 确认
            → commit
```

*关键设计（斜体）：* *KRaft 的 Raft 状态机用 `VoterSet` 定义 quorum 成员，用 `EpochElection` 追踪投票状态，用 `KafkaRaftClient` 处理请求/响应；`VoterSetHistory` 跟踪 voter 集合在日志中的历史变化，`KRaftControlRecordStateMachine` 则确保 voterSet 和 KRaft version 始终读到 LEO。*[模式: voter set 定义 + 过半选举 + 日志复制 + 状态机回放]

## 第一层：VoterSet 决定谁是 quorum 成员

`VoterSet` 是 `KafkaRaftClient` 的选民名册。它不只是静态配置，而是可以动态改变的——通过向日志写入 `VotersRecord` 来添加、移除、更新 voter。

`VoterSetHistory` 在内存中记录 voter 集合的历史变化，以便在生成 snapshot 时知道某个 offset 的 voter set 是什么。

```text
VoterSetHistory
  → 按 offset 记录 voter set 的每次变化
    → 生成 snapshot 时，voterSetAtOffset(offset) 返回该点 voter set
      → 用于判断“这台机器在当时的 quorum 里吗”
```

`VoterSet` 还有一个关键约束：**新旧 voter set 必须有重叠的多数派**（`hasOverlappingMajority`）。这样可以防止 voter 集合变更后，新旧两组选民之间无法形成多数。

## 第二层：EpochElection 追踪投票状态

当候选节点在 election timeout 到期后，进入 candidate 状态，向每个 voter 发送 `VoteRequest`。

`EpochElection` 维护每个 voter 的投票状态：

- `UNRECORDED`：还未收到回复
- `GRANTED`：同意
- `REJECTED`：拒绝

`isVoteGranted()` 检查 `numGranted >= majoritySize()`。注意这里的 majority 是“多数 voter”，不是“多数节点”。

值得强调的一点：候选节点进入候选状态时，会先把自己的 VoterState 初始化为 `GRANTED`——也就是**它先投自己一票**。这也是为什么 `isVoteGranted()` 从一开始就至少有一票：候选人的本地票已经算进 GRANTED 里。

`EpochElection.isVoteRejected()` 检查 `numGranted + numUnrecorded < majoritySize()`：如果即使所有未记录的 voter 都同意也不能达到多数，选举就提前结束。

## 第三层：KafkaRaftClient 用 Fetch 驱动复制，不是 leader push

Kafka 的 Raft 实现与传统 Raft 有一个关键差异：**复制主要由 follower 的 fetch 请求驱动，而不是 leader 主动 push。**

这就是为什么除了 `VoteRequest` / `BeginQuorumEpoch` / `EndQuorumEpoch` 这些标准 Raft 请求之外，Kafka Raft 还有一个 `FetchRequestData`——它和普通 partition 的 fetch 请求复用同一套 API 语义，但加上 metadata log 的 snapshot 和 epoch 校验。

`KafkaRaftClient.MAX_FETCH_WAIT_MS = 500` 这个参数是 leader 在无新数据时限制 fetch 响应延迟的上界，用来控制把数据批量返回给 follower 的时间窗口。它并不是“等到所有 follower 都追上”的等待时长。

## 第四层：KRaftControlRecordStateMachine 保证 voterSet 和 kraftVersion 始终读到 LEO

它维护两个关键日志历史：

- `voterSetHistory`：voter 集合的历史
- `kraftVersionHistory`：KRaft 版本的历史

`updateState()` 在每次日志变化时被调用，确保状态机始终读到 LEO。`maybeLoadSnapshot()` 和 `maybeLoadLog()` 分别从 snapshot 和 log 重建状态。

```text
updateState()
  → maybeLoadSnapshot()
    → maybeLoadLog()
      → 逐条回放 log 中的 control record
        → 更新 voterSetHistory / kraftVersionHistory
```

控制记录（`VotersRecord`、`KRaftVersionRecord`）不是业务数据，而是 quorum 内部状态。`KRaftControlRecordStateMachine` 负责消费这些记录，让 Raft 层知道自己当前该用哪个 voter set 和哪个 KRaft 版本来决策。

## 第五层：选举与提交的安全边界

`KafkaRaftClient` 的注释明确了几个安全边界：

- 只有 voters 能参与选举；
- leader 在收到 quorum 确认后才推进 commit 位置；
- 每次 leader 变更后，follower 用 Kafka 的日志调协协议（epoch-based truncation）把日志截断到共同点，具体与 Kafka-28 讲的 follower epoch 截断是同一套机制；
- 动态 voter 变更时，新旧 voter set 必须重叠多数。

这些边界保证：即使 voter 集合在变化、leader 在切换，日志也不会出现两个不同 leader 写入不同版本的情况。

## 收网：VoterSet 定义 quorum，EpochElection 决定谁当选，CRSM 跟踪 voter/version 历史

把整篇压成一句话：KRaft 的 Raft 状态机用 `VoterSet` 定义 quorum 成员，`EpochElection` 过半判断决定谁当选 leader，`KafkaRaftClient` 处理选主/复制/提交请求；`KRaftControlRecordStateMachine` 通过 `VoterSetHistory` 和 `kraftVersionHistory` 确保 voter set 和 KRaft 版本始终读到 LEO，支持动态 quorum 变更。

```text
voter set
  → EpochElection（过半投票）
    → leader 选出
      → 日志复制（Fetch 驱动）
        → commit
          → KRaftControlRecordStateMachine 跟踪 voter/version 变化
```

到这里，主线只发生了五件事。

第一，VoterSet 定义谁是 quorum 成员。

第二，EpochElection 统计投票，多个分支判断选举是否成功/失败。

第三，KafkaRaftClient 用 Fetch 驱动复制，不是 leader push。

第四，KRaftControlRecordStateMachine 确保 voter/version 始终读到 LEO。

第五，新旧 voter set 必须有重叠多数。

**本篇的一句话困惑**：KRaft 的 Raft 选主到底怎么过半、怎么追踪 voter 变化？

**本篇的一句话顿悟**：VoterSet 定义选民，EpochElection 过半判定，VoterSetHistory 跟踪历史变化，CRSM 确保始终读到 LEO。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“所有 broker 都能投票。”** 只有 voters 有资格，observer 不能投票、不能当 leader。
2. **“voter 集合是静态的。”** 通过 `VotersRecord` 可以动态增删，但新旧集合必须重叠多数。
3. **“KRaft 复制由 leader push。”** 实际由 follower 的 Fetch 请求驱动。
4. **“KRaftControlRecordStateMachine 维护业务元数据。”** 它只跟踪 voter 集合与 KRaft version。
5. **“epoch 只用于选主。”** epoch 也用于标记每次 leader 任期，follower 用它判断 leader 是否合法。

### 关键证据清单

- `raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java:128`：Kafka Raft 的 voter/observer 边界与 API 注释。
- `raft/src/main/java/org/apache/kafka/raft/internals/EpochElection.java:30`：EpochElection 类。
- `raft/src/main/java/org/apache/kafka/raft/internals/EpochElection.java:91`：isVoteGranted 过半判断。
- `raft/src/main/java/org/apache/kafka/raft/internals/KRaftControlRecordStateMachine.java:42`：CRSM 职责。
- `raft/src/main/java/org/apache/kafka/raft/internals/KRaftControlRecordStateMachine.java:116`：updateState。
- `raft/src/main/java/org/apache/kafka/raft/internals/VoterSetHistory.java:28`：VoterSetHistory 作用。
- `raft/src/main/java/org/apache/kafka/raft/internals/VoterSetHistory.java:54`：addAt 与重叠多数校验。
- `raft/src/main/java/org/apache/kafka/raft/VoterSet.java:48`：voter set 定义。
- `raft/src/main/java/org/apache/kafka/raft/VoterSet.java:319`：hasOverlappingMajority。