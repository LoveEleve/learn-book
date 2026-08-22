# Kafka-6. Consumer 的 group 成员资格由谁说了算——GroupCoordinator、Classic 四步重平衡与 Modern 心跳重平衡

> 场景：前两篇已经把"Consumer 从哪个分区、以什么位置拉数据"讲清了（Kafka-4），也把"一个 broker 会话里有哪些分区"讲清了（Kafka-5）。但还欠着一个更根本的问题：一个消费者组里的每个成员，到底负责哪些分区？这个归属是谁拍板的？本篇正面回答这个问题：消费者组的成员资格与分区归属，是由一台协调器主持、全体成员共同维护的一份共识。

## 先把真正的困惑摆出来：两个 consumer 为什么不会重复消费同一个分区

假设两个 consumer 进程用同一个 groupId 订阅同一个 topic。每个进程里都有一个 `KafkaConsumer`，都会调用 `poll()`。按直觉，它们各自拉各自的数据，好像没什么问题。

可一旦这个 topic 有 100 个分区，两个进程都去拉，会发生什么？

- 两个进程都收到全部分区的数据 → 每一条消息被消费两次；
- 两个进程各拉一半分区 → 看似没问题，但谁来保证"各一半"？
- 第二个进程加入时第一个进程正在消费 → 归属切换时，谁负责哪些分区，边界在哪？

没有一个集中决策，这些问题都无法稳定回答。分区的归属必须有一个"权威"，而这个权威不能是某个 consumer 自己——它只知道自己，不知道别人。

```text
成员 C1 想：我要消费 0-49 分区
成员 C2 想：我要消费 50-99 分区
  → 两个成员各自为政
    → 无法收敛，重复消费或漏消费
```

这就是消费者组协调要解决的第一个问题：**"谁消费哪些分区"必须由全局视角决定，而不是由单个成员决定。**

*关键设计（斜体）：* *归属决策从成员本地升级到一台协调器：协调器按 groupId 把每个组绑定到 `__consumer_offsets` 的某个分区，维护成员列表与订阅，并用 Assignor 计算出"每个成员分到哪些分区"的目标分配，再通过协议把归属收敛到全体成员。*[模式: 协调器主持 + 成员回执 + 世代/epoch 防旧]

## 第一层：协调器不是一个独立服务，而是 `__consumer_offsets` 分区 leader 上的 shard

很多人会直觉地认为：既然叫协调器，那应该是一个独立运行的进程，像 ZooKeeper 那样专门管协调。

但 Kafka 的 GroupCoordinator 不是独立进程。它是 `GroupCoordinatorService` 按 `__consumer_offsets` 分区划分的 `GroupCoordinatorShard`：一个 group 被哈希到某个 `__consumer_offsets` 分区，这个分区在哪个 broker 上是 leader，那个 broker 就负责主持这个 group。

```text
groupId → hash → __consumer_offsets 分区 p
  → p 的 leader broker 承载 GroupCoordinatorShard
    → 该 shard 管理落在 p 上的所有 group
```

这带来两个重要推论。

第一，**协调器是分片化的**。不同的 group 由不同 broker 的不同 shard 主持，没有单点。第二，**协调器的状态是可恢复的**。协调器的元数据不是只在内存里，而是写成记录写进 `__consumer_offsets`，通过回放重建——`GroupCoordinatorShard` 本身就是"复制状态机 + 回放"的设计。

`CoordinatorRequestManager` 在客户端负责发现协调器：它向任意 broker 发送 FindCoordinator 请求，按 groupId 拿到对应的协调器节点，再与之建立连接。Kafka-4 里 Consumer 要提交 offset、要发心跳之前，第一步都是"先找到协调器"。

如果协调器不在"某个固定进程"里，主链会先在哪出问题？一个 group 的元数据必须持久化，否则 broker 重启后成员列表和归属全丢。所以协调器状态落在 `__consumer_offsets` 分区上，正是为了让"谁是协调者"和"协调者记得什么"都能随分区 leader 一起迁移和恢复。

```text
CoordinatorRequestManager 发现协调器
  → 按 groupId 哈希到 __consumer_offsets 分区
    → 连接该分区的 leader broker
      → 与 GroupCoordinatorShard 通信
```

## 第二层：Classic 协议用"加入→同步→心跳→离开"四步完成一次全组重平衡

Classic 协议是消费者组最早的协议，对应 `JoinGroup → SyncGroup → Heartbeat → LeaveGroup` 四个请求。它把"成员达成一致"的过程拆成四个显式阶段。

### 第一步 JoinGroup：收集成员与订阅

成员调用 `subscribe()` 后，客户端通过 `AbstractCoordinator.joinGroupIfNeeded()` 发起 JoinGroup。协调器收到后，把成员加入组，并根据现有成员判断是否进入重平衡。

组的生命周期用 `ClassicGroupState` 描述：

```text
EMPTY
  → PREPARING_REBALANCE
    → COMPLETING_REBALANCE
      → STABLE
        → DEAD（空组清理）
```

- `EMPTY`：没有成员，但还保留 offset；
- `PREPARING_REBALANCE`：正在收集成员的加入请求；
- `COMPLETING_REBALANCE`：成员已到齐，等待 leader 提交分配；
- `STABLE`：归属确定，正常消费，只用心跳维持。

成员加入后，协调器让组进入 `PREPARING_REBALANCE`，并等待：要么所有成员都加入，要么等到 rebalance 超时。

### 第二步 SyncGroup：leader 算出分配，全体拿到归属

等所有成员都加入后，组进入 `COMPLETING_REBALANCE`。协调器把**第一个加入的成员**设为 leader（leader 离开后由 `maybeElectNewJoinedLeader` 从已加入成员里重新指定），把全组成员列表交给它。leader 在**客户端**用 `AbstractPartitionAssignor`（Range/RoundRobin/Sticky 等）算出每个成员分到哪些分区，再把分配结果通过 SyncGroup 请求发回协调器。

协调器把这份分配持久化后，让组进入 `STABLE`，并把分配结果返回给所有成员。

注意这里的一个关键点：**Classic 的分配在客户端 leader 上完成，不在协调器上完成。** 协调器只负责收集成员、指定 leader、广播结果。

### 第三步 Heartbeat：稳定期的"我还在"

进入 `STABLE` 后，成员周期性地发送 Heartbeat。只要心跳正常，组就保持稳定，成员的分区归属不变。心跳的作用就是让协调器知道"我还活着"。

如果某个成员心跳超时，协调器认为它挂了，会把组从 `STABLE` 拉回 `PREPARING_REBALANCE`，重新走一遍四步。

### 第四步 LeaveGroup：主动退出

成员正常关闭时会发 LeaveGroup。协调器把成员移出组，重新触发重平衡，把该成员原来的分区重新分配给别人。

## 第三层：Classic 的代价——每次变化都是 stop-the-world

Classic 四步设计得很清晰，但代价也清晰：**每一次成员变化，都要全组走完四步。**

- 新成员加入 → 全组重平衡；
- 旧成员心跳超时 → 全组重平衡；
- 成员订阅变化 → 全组重平衡；
- 主动 leave → 全组重平衡。

在重平衡期间，所有成员都被要求放弃当前分配，等新的分配下来再重新开始消费。这个"全部停下、重新分配"的过程被称为 **stop-the-world**。

为什么 Classic 必须全停？因为它的协商是"先收集、再统一分配"：分配结果是基于"当前全组成员"算出来的，只要成员集合变了，旧分配对全体都不再适用。协调器必须等所有人对齐到同一个 generation，才敢说"归属收敛了"。

所以 Classic 的问题在成员多、变动频繁时会被放大：一个 100 成员的组，每加入一个成员，就要全组停一次。这就是 Kafka 后来引入 KIP-848 的原动力。

如果只改 Classic 的"哪一步"，能解决全停吗？

- 让成员不放弃旧分配就加入？协调器无法保证新分配与旧分配兼容；
- 让协调器在服务端分配？可以，但这只是把计算挪了个地方，全停的逻辑还在；
- 让成员分阶段协商？那本质就是 Modern 的增量思路了。

所以问题的根子不在"谁算分配"，而在"重平衡的粒度"。

## 第四层：Modern 协议（KIP-848）用单一心跳完成加入、分配、回执、维持

KIP-848 推出的 Modern 协议，把 Classic 的四步压成**一个请求**：`ConsumerGroupHeartbeat`。成员从加入、拿分配、确认分配、维持心跳，全都通过这个单一请求完成。

客户端用 `ConsumerHeartbeatRequestManager.HeartbeatState.buildRequestData()` 构建心跳，它总是携带三样东西：

- `groupId`；
- `memberId`（成员启动时生成）；
- `memberEpoch`（当前成员协调到的版本号）。

其他信息（订阅、正则、server assignor、本成员已接受的 assignment）只在加入或发生变化时才发送。这保证了请求体在稳定期很小，同时协调器随时能拿到"这个成员现在认为自己拥有哪些分区"。

`memberEpoch` 是 Modern 协议的核心：协调器每次给成员下发的分配都对应一个新的 epoch，成员收到后把 epoch 记下来，下次心跳带回。但要注意，**回执本身不是 epoch**——成员真正向协调器确认"我已接受这个归属"的，是心跳里回显的 `topicPartitions`（本成员当前拥有的分区集合）。epoch 只是始终携带的版本凭据，参与 fencing 校验；`topicPartitions` 才是接受归属的回执。也要注意，epoch 只在分配确实变化的成员上推进，稳定成员的心跳不 bump（见第五层）。

```text
首次心跳：memberEpoch=0（表示要加入）
  → 协调器创建成员，计算 target assignment
    → 返回 assignment + memberEpoch=1
      → 成员应用新 assignment，把 owned 分区回显进下次心跳的 topicPartitions
        → 协调器据此确认"它已接受当前归属"（epoch 只是版本凭据）
```

服务端 `GroupMetadataManager.consumerGroupHeartbeat()` 是入口：它首先检查 `memberEpoch` 是否为 `-1`（动态成员离开）或 `-2`（静态成员离开），是则走 leave；否则走正常心跳处理。

## 第五层：Modern 的"增量"体现在只调整受影响成员

Modern 能避免 stop-the-world，靠的不是"不重平衡"，而是"重平衡的粒度变小"。

协调器为整个组维护一个 `groupEpoch` 和一份 **target assignment**（每个成员应该拥有哪些分区）。当组里发生变化（新成员加入、成员离开、订阅变化）时，协调器重新计算 target assignment，但只把**变化了的成员**拎出来单独通知，其余成员的归属不动。

```text
C1、C2、C3 都稳定在 epoch N
  → 新成员 C4 加入
    → 协调器重算 target assignment
      → 只影响 C1、C4（C1 让出几个分区，C4 接手）
        → 只向 C1、C4 下发新 assignment + 新 epoch
          → C2、C3 完全不动，继续消费
```

这才是"增量重平衡"的确切含义：**不是不重平衡，而是把重平衡的范围从"全组"缩小到"受影响成员"。**

那么协调器怎么知道哪些成员受影响？它并不"对比新旧"挑出变化——`TargetAssignmentBuilder` 是基于最新成员与订阅**重新计算**一份 target assignment，并且只为"分配确实变化了"的成员生成记录。成员重算出的新归属与它当前拥有的分区一比较，谁受影响就清楚了。

如果 Modern 每次变化都把所有成员的 assignment 全量重发，主链会先在哪退化？它就成了"换皮的四步"，全组的请求体都会膨胀，增量优势消失。所以"只改受影响成员"不是实现细节，而是 Modern 的定义特征。

## 第六层：epoch 失配是协调的关键安全阀

增量重平衡带来了一个新问题：协调器给 C1 下了"让出分区 X"的新 assignment，但如果 C1 因为网络延迟还在按旧 assignment 消费，会怎样？

协调器对 epoch 的校验是分路径的：

- **心跳路径**：`GroupMetadataManager.throwIfConsumerGroupMemberEpochIsInvalid()` 校验请求里的 memberEpoch。若大于当前 epoch 或小于且非 previous epoch，抛 **`FencedMemberEpochException`**（对应 `FENCED_MEMBER_EPOCH`），客户端据此 `transitionToFenced` 后放弃分区重新加入。
- **offset 提交/拉取路径**：`ConsumerGroup.validateMemberEpoch()` 校验，失配抛 **`StaleMemberEpochException`**。

两条路径抛的异常不同，但共同点是：不认"旧代次的成员继续用旧归属操作"。注意一个宽容分支：心跳路径若成员带的是 **previous epoch**，且其 owned 分区仍是当前 assignment 的子集，协调器会**放行**（`GroupMetadataManager.java:1535`）——因为"响应丢失、成员还没拿到新 epoch"是合理的，不应误判为失配。

客户端 `MemberState` 状态机也同步了这个语义：

```text
JOINING（要加入）
  → RECONCILING（收到新 assignment，正在应用）
    → ACKNOWLEDGING（应用完，下次心跳回执）
      → STABLE（稳定期）
        → FENCED（epoch 失配/被踢）
          → 重新 JOINING
```

成员一旦被 fenced，会放弃自己的分区（调用 onPartitionsLost），然后以 epoch=0 重新加入。注意这里用的是**同一个 memberId**：memberId 是客户端进程启动时生成的，进程生命周期内固定不变，fenced 后重加入不换 id，只是把 memberEpoch 归零重新同步——重点是不允许"旧 epoch 的成员继续消费已不属于它的分区"。

这就是"只认最新一代"的机制含义：`generationId`（Classic）和 `memberEpoch`（Modern）不是普通计数，而是**归属版本的校验凭据**。请求里带的代次对不上，协调器就拒绝，从而杜绝旧成员在归属切换后继续消费导致重复或丢失。

如果把 epoch 校验去掉，主链会先在哪失败？C1 让出分区 X 给 C4 后，C1 还在按旧位置拉 X 的数据，与 C4 同时消费 X——重复消费无法被察觉。所以 epoch 校验是增量重平衡能安全工作的前提。

## 收网：归属共识由协调器主持，Classic 用全组四步对齐，Modern 用单心跳增量收敛

把整篇压成一句话：消费者组的分区归属，不是成员各自决定的，而是由一台协调器主持的共识；Classic 用"加入→同步→心跳→离开"四步、以世代号为对齐凭据，每次变化都全组 stop-the-world；Modern(KIP-848) 用单一心跳携带 member epoch 与回执，把重平衡范围缩小到受影响成员，并用 epoch 失配校验防止旧成员继续消费已不属于它的分区。

```text
协调器 = __consumer_offsets 分区 leader 上的 shard
  → 维护成员列表 + target assignment

Classic：JoinGroup → SyncGroup → Heartbeat → LeaveGroup（generation 对齐，全组停）
Modern ：ConsumerGroupHeartbeat 单请求（member epoch，增量调整受影响成员）
  → 失配 → FENCED → 重新 JOINING
```

到这里，主线只发生了六件事。

第一，协调器是分片化的 shard，不是独立服务，状态落在 `__consumer_offsets`。

第二，Classic 用四步把成员对齐到同一个 generation，分配在客户端 leader 完成。

第三，Classic 每次变化都全组 stop-the-world，问题出在重平衡粒度。

第四，Modern 用单一心跳承载加入、分配、回执、维持，回执是成员回显的 owned 分区（topicPartitions），epoch 只是版本凭据。

第五，Modern 只调整受影响成员，其余成员不动，这才是"增量"的确切含义。

第六，epoch 失配时协调器拒绝并让成员重新加入，杜绝旧归属继续消费。

**本篇的一句话困惑**：两个 consumer 用同一个 groupId，凭什么不会重复消费同一个分区？

**本篇的一句话顿悟**：因为归属不是成员各自决定的，而是由协调器主持的共识；Classic 用全组四步 + 世代号对齐，Modern 用单心跳 + member epoch 增量收敛，两条路共同保证"每个分区在任一时刻只有一个主人"。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **"协调器是独立进程服务。"** 它是 `__consumer_offsets` 分区 leader 上的 shard，状态随分区 leader 迁移。
2. **"Classic 和 Modern 是完全独立的两套系统。"** 同一个 `GroupCoordinatorShard`/`GroupMetadataManager` 同时处理两条协议。
3. **"generationId 就是 memberEpoch。"** Classic 世代号随整组重平衡递增，Modern 成员版本号随单成员协调递增，语义与时机都不同。
4. **"fenced 后重加入会拿到新的 member id。"** memberId 是客户端进程启动时生成的，进程生命周期内不变；fenced 重加入只是 epoch 归 0。
5. **"Modern 不需要重平衡。"** 它仍有重平衡，只是增量调整受影响成员。
6. **"分区分配一定在客户端完成。"** Modern 在服务端 assignor 计算，Classic 在客户端 leader 计算。
7. **"memberEpoch 就是接受归属的回执。"** 回执是心跳里回显的 owned 分区（topicPartitions）；epoch 只是始终携带的版本凭据，参与 fencing 校验。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/CoordinatorRequestManager.java`：协调器发现（FindCoordinator 按 groupId）。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerMembershipManager.java`：Modern 成员生命周期。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/MemberState.java:27`：客户端成员状态机（JOINING/RECONCILING/ACKNOWLEDGING/STABLE/FENCED）。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerHeartbeatRequestManager.java:229`：HeartbeatState.buildRequestData 构建单心跳请求。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractCoordinator.java:463`：Classic joinGroupIfNeeded 主循环。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/ConsumerCoordinator.java:651`：Classic leader 的 onLeaderElected 执行客户端分配。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupCoordinatorShard.java:153`：复制状态机承载协调器。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:4691`：consumerGroupHeartbeat 入口，-1/-2 为离开。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:6086`：classicGroupJoin 入口。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroupState.java:46`：Classic 组状态机。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/classic/ClassicGroup.java:133`：generationId。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/ConsumerGroup.java:830`：validateMemberEpoch（offset 路径抛 StaleMemberEpoch）。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/GroupMetadataManager.java:1523`：throwIfConsumerGroupMemberEpochIsInvalid（心跳路径抛 FencedMemberEpoch，previous epoch+owned 子集放行）。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/TargetAssignmentBuilder.java:53`：只对分配变化的成员生成记录。
- `clients/src/main/java/org/apache/kafka/clients/consumer/internals/AbstractMembershipManager.java:79`：memberId 客户端启动生成，进程生命周期内不变。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/modern/consumer/CurrentAssignmentBuilder.java:136`：STABLE 且 epoch != target 才重算；UNREVOKED 撤销完才 epoch+1。
- `group-coordinator/src/main/java/org/apache/kafka/coordinator/group/assignor/UniformAssignor.java:53`：Modern 服务端 assignor。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 路线为基线。
- 本篇聚焦组成员协调与分区归属；不展开协议错误码全集、ShareGroup/StreamsGroup、offset 存储细节。
- 本篇把 Classic 四步与 Modern 单心跳都视为"归属共识"的两条实现路径，而非两套独立中间件。
- 不把 KIP-848 的 member epoch 写成普通序号：它是归属版本凭据，失配即拒绝。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-4`（Consumer/Fetcher 拉取主链）、`Kafka-5`（FetchSession 会话视图）。
- 后续桥接：下一篇可进入 `Kafka-7`，把分区分区 leader 与副本的归属（Controller/KRaft 管理）作为"集群层次"的另一个归属问题继续展开。