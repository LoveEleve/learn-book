# Kafka-8. 谁才是真正的 active Controller——KRaft 的选主、日志复制与 MetadataImage 主链

> 场景：上一章已经说明 Controller 如何决定分区 leader 与 ISR，但留下了一个更根本的问题：**Controller 自己凭什么成为唯一权威？** 如果 Controller-1 挂了，谁接班？新接班的节点如何让所有 broker 看到同一份元数据？本篇把边界收紧到 KRaft 的元数据共识：选主、提交、回放、snapshot 与 MetadataImage；分区 leader 的选举细节仍放在上一章那条主线里。

## 先把真正的困惑摆出来：Controller 挂了，谁接班

上一章的结论是：分区 leader 不能由 broker 之间各自商量，必须由唯一的 active Controller 决策。

但“唯一”马上带来第二个问题：

- active Controller 宕机了怎么办？
- 两个 controller 同时认为自己是 active 怎么办？
- 新 controller 接班时，怎么知道上一个 controller 已经写过哪些元数据？
- 普通 broker 怎么知道谁是新 controller、当前集群元数据是什么？

如果只是把“active controller”做成一个普通的单例进程，确实可以保证同一时刻只有一个，但进程一挂，整个集群就失去元数据决策者。如果让多个 controller 同时写内存，又会回到脑裂：节点 A 认为 topic 有 10 个分区，节点 B 认为只有 8 个分区，后续每次分区决策都可能不一致。

```text
Controller A：我才是 active，topic 已扩到 10 个分区
Controller B：我也能处理请求，topic 还是 8 个分区
  → 两套元数据同时生效 → 集群状态分裂
```

所以 KRaft 真正解决的不是“把 ZooKeeper 文件换个地方存”，而是把**Controller 选主、元数据写入、故障接班、状态恢复**放进同一条共识日志。

*关键设计（斜体）：* *voters 先通过 Raft 选出 metadata partition leader；只有 leader 所在节点上的 QuorumController 才能成为 active Controller。所有写操作先生成 metadata records 并复制到 quorum，提交后由 standby controller 通过 QuorumController 自己的 commit replay 追上内部状态，再由 broker / publisher 通过 MetadataLoader 消费 commit 或 snapshot，重建同一份 MetadataImage。*[模式: quorum 选主 + 日志提交 + image 回放]

## 第一层：KRaft 的 leader 是 metadata partition leader，不是业务分区 leader

先把两个“leader”拆开。

- **metadata partition leader**：KRaft 元数据 quorum 里的 Raft leader，负责接受元数据写入、推动复制和提交；
- **业务分区 leader**：Kafka-7 讲的 topic-partition leader，负责生产、消费与副本同步。

KRaft 只先解决第一个问题：谁有资格成为 active Controller。业务分区的 leader 仍然由 Controller 根据 ISR、broker 状态和选举策略决定。

KRaft 的参与者分成两类：

- **voter**：参加投票、参与 quorum，决定谁能成为 Raft leader；
- **observer**：不参加投票，只追踪元数据日志或 image。

`KafkaRaftClient` 的类注释把这件事说得很直接：这个协议区分 voters 与 observers，只有 voters 有资格处理协议请求、参与选举；Raft leader 通过 Fetch/BeginQuorumEpoch 等路径与其他节点同步。

```text
voters
  → 参与 VoteRequest / 过半判断
    → 选出 metadata partition leader
      → leader 所在节点的 QuorumController 才可能 active

observers / broker
  → 不参与选主
    → 追踪 metadata log / snapshot
      → 加载自己的 MetadataImage
```

如果把业务分区 leader 和 metadata partition leader 混成一个，主链会先在哪出问题？读者会以为每个 topic-partition 都要单独跑一轮 Raft 来选 Controller，或者误以为 broker 0 成为某个业务分区 leader后就有资格改全局元数据。实际上，KRaft 的 quorum 只有一条元数据主线；业务分区只是这条主线里被记录和传播的对象。

## 第二层：EpochElection 用过半票决定谁能接管

KRaft 的选主不是“谁先喊自己是 leader”，而是候选节点在 election epoch 内向 voters 请求投票。

`EpochElection` 维护每个 voter 的投票状态：`UNRECORDED`、`GRANTED`、`REJECTED`。它的关键判断是：获得的赞成票数是否达到 quorum majority。

```text
候选节点进入新 epoch
  → 向 voters 发送 VoteRequest
    → 记录每张票：GRANTED / REJECTED
      → GRANTED >= majoritySize
        → 选举成功，成为 metadata leader
      → 可能无法达到 majority
        → 选举失败，等待下一轮
```

为什么一定要过半？因为任意两个多数派必然相交。只要一个 epoch 的 leader 得到多数票，另一个节点就不能在同一组 voter 视图下得到另一套互斥多数派，从而避免两个 active controller 同时获得合法身份。

`KafkaRaftClient` 的注释还点出 Kafka Raft 与教科书 Raft 的一个实现差异：Kafka 的复制主要由 follower fetch 驱动，所以 leader 需要 `BeginQuorumEpoch` 告诉 voters“新 epoch 的 leader 是谁”，而不是只靠普通数据 push 让 follower 偶然发现。

这一步只解决“谁当 leader”，还没有解决“leader 写的元数据是否已经被大多数节点接受”。选主和提交是两件事，不能混在一起。

## 第三层：QuorumController 只有在 Raft leader 身份下才是 active

上一章把 `QuorumController` 当作分区归属的唯一决策者。本章补上它的资格来源：它必须先观察到自己是 metadata quorum 的 leader。

`QuorumController` 通过 `QuorumMetaLogListener.handleLeaderChange()` 接收 Raft 的领导变化：

- 如果当前节点成为新 epoch 的 leader，它更新自己的 `curClaimEpoch`，获得 active controller 资格；
- 如果当前节点不再是 leader，它放弃 active 身份，成为 standby；
- standby 仍然继续消费并回放已提交的 metadata records，但不能对外做 active controller 的写决策。

```text
Raft leader change
  → 当前节点是 metadata leader？
    → 是：更新 claim epoch，QuorumController active
    → 否：放弃 active，作为 standby 回放日志
```

这解释了 Kafka-7 的“唯一 Controller”为什么不等于“只有一个 Controller 进程”：集群可以有多个候选 controller 节点，但同一 metadata epoch 内只有 Raft leader 所在节点拥有 active 身份。其他 **voter** 节点会提前把状态追到最新，故障时才有资格快速接班；observer/broker 即使追到了最新 MetadataImage，也没有投票资格，不能直接成为 active controller。

## 第四层：Controller 写元数据的第一步不是改内存，而是排队生成记录

当客户端请求创建 topic、扩分区、修改配置，active Controller 不应该直接把某个 Java 对象改掉，然后再想办法通知其他节点。

`QuorumController.appendWriteEvent()` 做的第一件事，是把操作包装成 `ControllerWriteEvent` 放进事件队列，等 controller 线程执行；执行结果是 `ControllerResult`，其中包含业务返回值和待写入的 metadata records。

```text
客户端请求
  → appendWriteEvent
    → ControllerWriteEvent 入队
      → active controller 执行
        → ControllerResult（响应 + metadata records）
```

这样做的关键不是“代码更整齐”，而是把**决策**和**日志记录**绑定在一起。Controller 产生的每个分区、broker、配置变化，都要能被序列化成元数据记录，进入 Raft log，之后由其他节点按同样顺序重放。

如果先改内存、之后再异步写日志，主链会先在哪失败？active controller 在内存里已经把 broker 0 设为 leader，但它在写日志前崩溃；新 controller 从旧日志恢复，就会认为 broker 0 仍然是旧状态。于是 active 节点短暂做过的决策没有共识依据，整个集群会出现“请求曾经成功、重启后却不存在”的幽灵状态。

## 第五层：BatchAccumulator 与 KafkaRaftClient 把记录复制到 quorum

ControllerResult 生成记录后，还要经过 Raft 的追加与复制。这里要把“controller 主链”和“Raft 内部实现链”分开看：从 controller 视角，只需要知道 records 会交给 `KafkaRaftClient` 进入复制；而在 **Raft leader 的内部实现** 里，`BatchAccumulator` 才负责把待追加的 records 组成 batch，附带当前 epoch 与 offset，再交给本地 replicated log，并通过 Raft 网络让 follower fetch。

```text
ControllerResult.records
  → BatchAccumulator 聚成 batch
    → leader append 到 replicated log
      → followers Fetch 复制
        → 多数 voter 追上并确认
          → metadata offset committed
```

这里有一个必须区分的边界：**append 到 leader 本地日志，不等于 committed。** 只有当 quorum 达成提交条件，元数据变更才成为所有后续 controller 都必须承认的事实。

这一层最好再把三个相近概念拆开：

- **leader 本地 append**：只是记录先写进了 leader 自己的 log；
- **high watermark / committed offset**：表示多数 voter 已经接受，这条记录成了 quorum 事实；
- **last stable offset**：在 `QuorumController.handleCommit()` 这条主线上，active controller 会推进 committed/stable offset，并据此完成等待中的事件。

所以对外可见的安全边界不是“leader 写进去了”，而是“这条 metadata record 已经跨过 high watermark / committed 边界，并进入可稳定消费的 offset 区间”。

`KRaftControlRecordStateMachine` 负责跟踪控制类 record，例如 KRaft version 与 voter set，并且要求状态机始终读到 log end offset。这个状态机不是业务 `MetadataImage` 本身，而是 Raft quorum 自己用来知道“当前 voter 集合和协议版本”的内部状态。

如果 leader 写入本地日志后就立刻把响应返回给客户端，主链会先在哪失败？leader 还没复制到多数节点就宕机，新的 leader 可能没有这条记录；客户端已经认为操作成功，但新 controller 根本看不到它。于是 KRaft 必须把“对外完成”绑定到 committed metadata offset。

## 第六层：standby controller 如何追上——controller 自己 replay commit

Raft 日志提交以后，active controller 和 standby controller 的处理方式不同。这里要先把一条容易混淆的边界讲清楚：**standby controller 追状态，主要靠的是 `QuorumController` 自己的 commit replay；普通 broker / publisher 追状态，才主要靠 `MetadataLoader` 把日志与 snapshot 变成 MetadataImage。**

### Active controller：记录已经在决策时应用

`QuorumMetaLogListener.handleCommit()` 发现当前节点是 active 时，不再重复 replay 这些 records，因为 active 在生成决策时已经更新了自己的状态。它主要推进 committed/stable offset，并完成等待这些 offset 的 purgatory 项。

### Standby controller：按提交顺序 replay

如果当前节点是 standby，`handleCommit()` 会逐条读取 committed batch，调用 `replay()` 把记录应用到本地 controller 状态。它不能自行决定新状态，只能按照 active controller 已经提交的记录重建状态。

```text
同一批 committed metadata records
  → active：状态已在写入流程应用，只推进 committed offset
  → standby：逐条 replay，重建 controller 状态
```

这保证了未来接班的 controller 不需要“猜”旧 controller 做过什么，而是从共识日志得到完整因果顺序。

## 第七层：broker / publisher 如何追上——MetadataLoader 把日志与 snapshot 变成 MetadataImage

controller standby 通过上一层那条 replay 主线追状态；而普通 broker 不需要参与 controller 的每个内部决策对象，它更关心的是拿到一份一致的集群元数据视图。Kafka 在这条 broker-side 主线里，用 `MetadataLoader` 把 Raft 提供的 batch/snapshot 转成两种中间产物：

- `MetadataDelta`：从上一份 image 到当前变更的差量；
- `MetadataImage`：某个 metadata offset 上完整、不可变的元数据视图。

`MetadataLoader` 自己维护一个线程，按顺序调用各个 `MetadataPublisher`。新 publisher 加入时，不是从空白开始猜，而是收到一份基于当前 image 的 catch-up delta。

```text
Raft commit batch / snapshot
  → MetadataLoader
    → MetadataDelta
      → apply
        → MetadataImage
          → MetadataPublisher.onMetadataUpdate
```

`MetadataImage` 不是某一个 topic 的状态，而是集群元数据的组合快照：features、cluster、topics、configs、ACL、quota、producer IDs 等都在同一份 provenance 下组织起来。它带有 last contained offset/epoch，使用者可以知道自己看到的元数据版本。

如果每个 broker 只收到零散的“某分区变了”事件、不维护 image，主链会先在哪失败？新组件启动时无法知道此前所有变更的完整结果，只能依赖事件是否从未丢失；而 image 给了它一个明确的版本化状态基线。

## 第八层：snapshot 不是备份，而是恢复路径的截断点

元数据日志会持续增长。如果每次 controller 或 broker 重启都从 offset 0 回放，恢复时间会随集群历史增长。

Raft/log 侧会在合适的 offset 生成 snapshot。snapshot 包含某个 offset 之前的完整状态；`MetadataLoader` 在这里扮演的是**消费 snapshot** 的角色，而不是生成 snapshot。后续恢复只需：

```text
snapshot 基线
  → 回放 snapshot 之后的增量 log
    → 得到最新 MetadataImage
```

`MetadataLoader.handleLoadSnapshot()` 会读取 snapshot 中的 batches，重放生成 delta/image；`KRaftControlRecordStateMachine` 也会在 snapshot 与 log 变化时更新自己的 voter/KRaft version 历史。

snapshot 的核心价值不是“多一份备份”，而是把无限增长的历史压缩成一个可直接加载的状态基线，同时保留 snapshot 之后的日志用于精确追赶。

## 收网：KRaft 先决定谁能当 Controller，再让所有节点看到同一份元数据

把整篇压成一句话：voters 通过 Raft 选出 metadata partition leader，leader 所在节点的 `QuorumController` 才是 active Controller；active Controller 把每次元数据变更生成 records，复制到 quorum 并等待 commit；**standby controller** 通过 `QuorumController` 自己的 commit replay 追上内部状态，**broker / publisher** 再通过 `MetadataLoader` 消费 commit 或 snapshot，形成带有 offset/epoch provenance 的同一份 MetadataImage。

```text
voters 过半选主
  → metadata leader
    → active QuorumController
      → appendWriteEvent 生成 records
        → BatchAccumulator + KafkaRaftClient 复制
          → quorum commit
            → standby replay / snapshot
              → MetadataDelta → MetadataImage
```

到这里，主线只发生了八件事。

第一，KRaft 的 leader 是 metadata partition leader，不是业务分区 leader。

第二，只有 voters 参与投票，过半票决定 leader，observer 只追状态。

第三，QuorumController 通过 Raft leader change 获得或放弃 active 身份。

第四，元数据写入先变成 records，再进入 Raft log，不允许只改内存。

第五，leader 本地 append 不等于提交，只有 quorum commit 才是共识事实。

第六，standby controller 按提交顺序 replay，不能自行决策。

第七，MetadataLoader 把 batch/snapshot 变成 MetadataDelta 与 MetadataImage。

第八，snapshot 是恢复截断点，避免从 offset 0 回放无限历史。

**本篇的一句话困惑**：Controller 挂了以后，谁接班，怎么保证新旧 Controller 看到的是同一份元数据？

**本篇的一句话顿悟**：KRaft 把 Controller 身份和元数据状态都放进 Raft 共识：voters 选出 metadata leader，leader 所在节点才 active；所有变更先 quorum commit，再由其他节点 replay 或从 snapshot 恢复成同一份 MetadataImage。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“KRaft 只是在 Kafka topic 里保存 ZooKeeper 数据。”** 它同时负责 controller 选主、日志提交与故障恢复。
2. **“Raft leader 就是业务分区 leader。”** KRaft leader 是 metadata partition leader，业务分区 leader 仍由 Controller 决策。
3. **“Controller 先改内存、再补日志也可以。”** 元数据决策必须进入共识日志，commit 后才是集群事实。
4. **“snapshot 只是备份。”** snapshot 是恢复时的状态基线，后面再接增量 log。
5. **“所有 broker 都能给 controller 投票。”** 只有 voters 参与投票，observer/broker 只追踪状态。

### 关键证据清单

- `raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java:128`：Kafka Raft 的选主、voter/observer 与复制边界。
- `raft/src/main/java/org/apache/kafka/raft/internals/EpochElection.java:91`：过半投票判断。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:931`：appendWriteEvent。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:954`：commit listener，区分 active 与 standby。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1056`：Raft leader change。
- `raft/src/main/java/org/apache/kafka/raft/internals/BatchAccumulator.java:118`：leader 端 append/batch。
- `raft/src/main/java/org/apache/kafka/raft/internals/KRaftControlRecordStateMachine.java:42`：KRaft 控制记录状态机。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:56`：日志/snapshot 到 delta/image 的职责。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:355`：handleCommit。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:375`：handleLoadSnapshot。
- `metadata/src/main/java/org/apache/kafka/image/MetadataImage.java:28`：broker 元数据 image。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 路线为基线。
- 本篇聚焦 metadata quorum、controller 选主、日志提交、snapshot/image 回放；Kafka-7 的分区 leader 选举与 Kafka-9 的 Partition/ISR 运行时细节留到各自专题处理。
- 不把 `KRaftControlRecordStateMachine` 与业务 `MetadataImage` 混成同一个状态机：前者跟踪 voter/KRaft 控制记录，后者是 broker 可消费的集群元数据视图。
- Vote/Fetch RPC 的全部字段级细节不作为本篇主线，正文只保留选主、提交、回放、snapshot 这条理解路径。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-7`（Controller 如何做分区归属决策）。
- 后续桥接：下一篇可进入 `Kafka-9`，回到 Partition/ISR 运行时状态机，解释 leader、follower、epoch 截断与副本追赶如何落到 broker 本地。