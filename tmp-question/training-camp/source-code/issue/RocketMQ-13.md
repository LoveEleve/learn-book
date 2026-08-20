# RocketMQ-13. 单机 CommitLog 为什么还不等于可靠消息 —— Master/Slave 复制主链

> 场景：前面已经把消息如何落到 CommitLog、如何由 Reput 分发到 ConsumeQueue，以及消息如何被 Consumer 拉走讲清了。走到这里，读者会自然地产生一个更底层的问题：既然 Master 已经把消息顺序追加进 CommitLog，为什么还要再复制给 Slave？Producer 什么时候才能把“发送成功”当成“消息更可靠”？
>
> 本篇只回答一个问题：**单机 CommitLog 为什么还不等于可靠消息，以及传统 Master/Slave HA 怎样把本地追加推进成副本确认。** 本篇聚焦 `DefaultHAService` 主链，不展开 DLedger、Controller 和自动选主；那些机制会在后文单独回答“副本怎样形成共识”和“Master 故障后谁来继续写”。

## 先把真正的困惑摆出来：Master 写成功，消息就安全吗

从 Producer 视角看，发送流程很容易被压缩成一句话：请求到了 Master，Master 把消息写进 CommitLog，然后返回成功。前文已经说明 CommitLog 是消息的物理真相层，这个判断没有错，但它只回答了“当前 Master 有没有留下这条消息”，还没有回答“当前 Master 出问题以后，系统是否还有另一份可以继续恢复的消息”。

单机 CommitLog 至少存在几种不同的时刻：

- 消息进入 Master 的内存映射文件；
- Master 的本地刷盘完成；
- 日志字节被复制到 Slave；
- Slave 回报已经追到目标物理 offset；
- Producer 收到最终发送结果。

这些时刻不是同一个时刻。尤其是“Master 本地追加成功”和“Slave 已经拥有这段日志”之间，隔着网络、连接、追赶、校验和确认等待。

如果 Master 只完成本地追加就返回成功，主链会先在哪失败？会先失败在：**返回成功的消息可能只存在于故障前的单个 Master 上，尚未形成可以接替恢复的副本。** 一旦 Master 在复制完成前损坏，Producer 看到的是成功，存储系统却可能只剩下不完整的本地事实。

所以传统 HA 解决的第一件事，不是“让消息再写一遍”，而是把“本地写入成功”与“副本确认成功”拆开，让系统明确知道消息已经推进到了哪一层。

可以先把这条链压成一张图：

```text
Producer
  → Master CommitLog 本地追加
    → Master 读取同一段物理日志
      → HA 连接传输 offset + 长度 + 日志字节
        → Slave 按物理 offset 追加
          → Slave 回报最大物理 offset
            → Master 按 ack 与超时决定 Producer 结果
```

*关键设计（斜体）：* *Master/Slave HA 不把“本地追加”伪装成“副本完成”，而是用物理 offset 把消息从单机事实推进到跨 Broker 的复制确认。*[模式: 本地事实与副本确认分层]

## 第一层：HA 复制的对象不是 ConsumeQueue，而是 CommitLog 的物理字节

前文已经讲过，RocketMQ 的 ConsumeQueue 是由 CommitLog 派生出来的消费索引桥。到了复制场景，这个关系尤其重要：Master 不需要先把每个 Topic、每个 Queue 的 ConsumeQueue 条目单独打包发送给 Slave，传统 HA 的主复制对象是 **CommitLog 的连续物理数据**。

Master 侧的 `DefaultHAConnection.WriteSocketService` 会从 `nextTransferFromWhere` 开始，通过 `getCommitLogData()` 取出 CommitLog 的一段数据，然后发送一个固定头部：

```text
8 bytes: physical offset
4 bytes: body size
N bytes: CommitLog body
```

这个头部不是业务消息头，而是复制协议用来定位和切分物理日志的传输头。它告诉 Slave：这批字节应该从哪个物理 offset 开始，以及本次要追加多少字节。

为什么复制对象要选 CommitLog，而不是直接复制 ConsumeQueue？因为 CommitLog 是所有逻辑队列共同依赖的顺序事实层。只要 Slave 在相同物理 offset 上拥有相同的日志字节，它就能让自己的 `ReputMessageService` 重新解析这些消息，再分别构建 ConsumeQueue、IndexFile 等派生结构。

如果复制层直接围绕 ConsumeQueue 展开，主链会先在哪变复杂？会变复杂在：每个 Topic/Queue 的派生索引都要单独协调，物理日志与逻辑索引之间还要额外处理一致性。复制 CommitLog 则把问题收敛成一个更基础的判断：**Slave 有没有连续拥有 Master 的下一段物理日志。**

所以本篇必须把边界钉死：

```text
Master CommitLog
  → 复制物理日志
    → Slave CommitLog
      → Slave Reput
        → Slave ConsumeQueue / IndexFile
```

HA 传输完成，不等于 Slave 的所有派生索引已经完成；但没有 CommitLog 这份物理事实，后面的派生结构也没有可靠来源。

## 第二层：Master 不是等 Producer 再来取日志，而是由 HA 连接主动推进复制

传统 HA 的连接方向也很容易被理解反。Master 负责监听，Slave 负责主动连接。

`DefaultHAService.init()` 在 Slave 角色下创建 `DefaultHAClient`；启动时，Master 的 `AcceptSocketService` 监听 HA 端口，Slave 的 `DefaultHAClient` 根据 `masterHaAddress` 连接 Master。连接建立后，Master 为这个 Socket 创建一个 `DefaultHAConnection`，并启动读写两个服务：

- `WriteSocketService`：从 Master CommitLog 取数据并发送；
- `ReadSocketService`：接收 Slave 回报的最大物理 offset。

这形成了一个很清晰的双向协议：

```text
Master → Slave：从某个 physical offset 开始的 CommitLog 字节
Slave  → Master：我当前已经追加到的最大 physical offset
```

为什么 Master 还要接收 Slave 的 offset，不能只看自己已经发出了多少？因为“Master 发出”只代表数据离开了 Master 的写路径，不代表 Slave 已经接收、校验、追加完成。只有 Slave 的回报，Master 才能把复制进度从“发送进度”推进为“副本已落地进度”。

这里已经出现了一个重要的状态分离：

- `nextTransferFromWhere` 是 Master 计划继续发送的位置；
- `slaveAckOffset` 是 Master 已知的 Slave 追加位置；
- Slave 的 `currentReportedOffset` 是 Slave 最近一次已经确认并回报的位置。

如果没有这条回报链，Master 最多只能知道“我试图发送了多少”，无法知道“副本真正拥有了多少”。

## 第三层：Slave 不是盲目追加，而是先用 offset 检查复制连续性

Slave 收到数据后，`DefaultHAClient.dispatchReadRequest()` 会先读取传输头中的 `masterPhyOffset` 和 `bodySize`，再把自己的当前最大物理 offset 与 Master 发来的起始 offset 对比。

核心约束是：如果 Slave 已经有数据，那么它当前的最大物理 offset 必须等于 Master 本次发送的起始 offset。随后，Slave 才会调用：

```text
DefaultMessageStore.appendToCommitLog(
    masterPhyOffset, bodyData, dataStart, bodySize
)
```

这不是一个可有可无的防御判断，而是复制正确性的地基。它保证 Slave 不是“收到什么就往文件尾巴写什么”，而是只接受能够接在自己现有物理日志之后的数据。

如果这个 offset 检查不存在，主链会先在哪失败？会失败在断线重连、重复发送或数据错位以后：Slave 可能把同一段数据重复追加，也可能把缺口之后的数据错误接上，最终形成一个看起来有文件、实际上物理日志已经无法与 Master 对齐的副本。

因此传统 HA 的追赶不是简单的字节搬运，而是一个带连续性约束的物理日志复制：

```text
Slave 当前 maxPhyOffset
  == Master 本次 transfer 的 startOffset
  → 允许 append
  → 更新本地最大物理 offset
  → 回报进度
```

这也解释了为什么 Slave 的复制进度以物理 offset 为核心，而不是以某个 Topic 的 queue offset 为核心。物理 offset 才能保证整份 CommitLog 的连续性。

## 第四层：Slave 追加 CommitLog 后，还要让自己的消费视图重新长出来

`DefaultMessageStore.appendToCommitLog()` 返回后，`DefaultMessageStore` 只会在追加结果为 `true` 时唤醒 `reputMessageService`；但需要特别注意，当前 `DefaultHAClient.dispatchReadRequest()` 没有检查这个布尔返回值，仍会继续推进 `dispatchPosition` 并调用 `reportSlaveMaxOffsetPlus()`。所以从源码事实看，这里不是一个可以直接写成“追加成功后再回报”的理想闭环，而是一个必须指出的实现边界：HA 客户端的回报路径没有把追加失败显式传回协议层。正常路径下，追加成功后 Reput 才会被唤醒；异常路径下，追加失败可能仍被继续处理，不能把回报 offset 自动等同于 Slave 已可靠落盘。

这一步很容易被忽略，因为它不在 HA 包里，但它决定了复制后的消息什么时候能进入 Slave 的正常消费视图。

复制主链实际上分成两个阶段：

```text
网络复制阶段：Master CommitLog → Slave CommitLog
派生视图阶段：Slave CommitLog → Reput → ConsumeQueue / IndexFile
```

因此，Slave 已经在 CommitLog 中拥有某条消息，不代表它的 ConsumeQueue 已经马上可读。`HATest` 也专门保留了这个边界：消息可能已经复制到 Slave 的 CommitLog，但还没有及时分发到 ConsumeQueue，所以测试会先按物理 offset 直接读取，再等待正常消费视图可见。

这意味着“副本已复制”和“副本已可消费”是两个不同问题：

- HA ack 主要确认物理日志复制进度；
- Reput 决定派生消费索引的追赶进度；
- Consumer 能否拉取，还要看 Slave 的消费视图是否已经完成派生。

如果把这三个时刻混成一个时刻，主链会先在哪理解错？会把“Slave 已经收到字节”误写成“Slave 已经完成所有消费可见性工作”，从而在故障恢复和消费切换时误判消息状态。

## 第五层：Slave 的 offset 回报，才把复制进度送回 Master

在正常路径下，Slave 追加一段 CommitLog 后，会计算自己的 `currentPhyOffset`。只有当这个位置超过上一次回报的 `currentReportedOffset` 时，才通过 `reportSlaveMaxOffset()` 把新的最大物理 offset 写回 Master。但源码还有一个必须保留的失败边界：`dispatchReadRequest()` 没有根据 `appendToCommitLog()` 的布尔返回值中断当前数据帧处理，因此“发回了一个更大的 offset”只能说明 HAClient 认为本地物理位置已经推进，不能无条件证明这段数据已经成功追加。

Master 侧 `DefaultHAConnection.ReadSocketService` 收到这个 8 字节 offset 后，会更新 `slaveAckOffset`，并调用 `haService.notifyTransferSome()`。这个通知会唤醒等待复制确认的 `GroupTransferService`。

于是，复制进度形成了完整闭环：

```text
Master 读取 CommitLog
  → Master 发送数据
    → Slave 追加 CommitLog
      → Slave 回报 maxPhyOffset
        → Master 更新 slaveAckOffset
          → GroupTransferService 重新检查等待请求
```

这里的 `notifyTransferSome()` 很关键。它不是重新发送消息，而是把“副本进度发生变化”这个事实通知给等待中的确认请求。真正的确认判断仍然由 `GroupTransferService` 根据请求目标 offset 和 ack 条件完成。

如果只有数据发送、没有 offset 回报，主链会先在哪卡死？会卡在 Producer 的同步确认上：Master 永远无法证明 Slave 已经达到 `GroupCommitRequest.nextOffset`，只能等待超时。

## 第六层：Producer 的成功不是 Master 写完就返回，而是由 GroupCommitRequest 等待复制边界

消息在 CommitLog 追加完成后，`CommitLog.asyncPutMessage()` 会进入 `handleDiskFlushAndHA()`。本地刷盘和 HA 复制是两个并行的结果来源：

```text
本地追加
  ├─→ FlushManager：本地刷盘结果
  └─→ handleHA：副本 ack 结果
```

`handleHA()` 会把本次消息的结束物理位置计算成：

```text
nextOffset = wroteOffset + wroteBytes
```

随后创建 `GroupCommitRequest(nextOffset, slaveTimeout, needAckNums)`，放入 `GroupTransferService`。这个请求表达的不是“请再复制一次”，而是：**请等待足够的副本都达到这个物理位置，并在截止时间内给出结果。**

`GroupTransferService` 检查的核心条件是：

- 单副本确认模式下，`push2SlaveMaxOffset` 是否已经达到 `nextOffset`；
- 多副本确认模式下，包含 Master 在内，满足 offset 的连接数量是否达到 `ackNums`；
- Controller 模式的全量同步集合则是另一套语义，本篇不展开。

如果截止时间内没有满足条件，请求会被唤醒为 `FLUSH_SLAVE_TIMEOUT`。这时要特别注意：**超时是 Producer 的确认结果，不等价于消息一定没有写入，也不等价于消息一定不会最终到达 Slave。** 它只说明在规定等待窗口内，系统没有获得足够的复制确认。

这正是消息系统最容易被简化错的边界：

```text
PUT_OK
  ≠ 只代表 Master 本地追加

FLUSH_SLAVE_TIMEOUT
  ≠ 代表这条消息已经从所有副本消失

它们分别代表：
  Producer 是否在当前确认策略和时间窗口内拿到目标结果
```

## 第七层：SYNC_MASTER 和 ASYNC_MASTER 的差别，是确认边界不同

传统 HA 的角色配置决定了复制确认是否进入发送结果主链。`CommitLog.needHandleHA()` 只对满足条件的 `SYNC_MASTER` 进入 HA 处理：消息允许等待存储、不是复制模式，并且当前 Broker 是同步 Master。但这还不等于每条消息都会等待 Slave，因为 `handleHA()` 在 `needAckNums <= 1` 时会直接返回 `PUT_OK`；而 `MessageStoreConfig.inSyncReplicas` 的默认值就是 `1`。只有配置要求的 ack 数量大于 1 时，`GroupCommitRequest` 才会真正等待额外副本。

因此，不能把两种模式简单压成“同步一定等 Slave、异步一定不复制”，更准确的对比是：

- `ASYNC_MASTER`：复制可以在后台继续进行，但 Slave ack 不进入当前发送请求的必经确认；Producer 不必为副本追赶等待。
- `SYNC_MASTER`：允许 HA 确认进入发送结果主链，但是否真正等待额外副本，还取决于 `inSyncReplicas`、自动同步副本和相关 ack 配置；当有效 `needAckNums <= 1` 时，`handleHA()` 直接返回 `PUT_OK`。

这不是“同步模式会复制、异步模式不复制”的区别，而是：**复制是否以及在什么配置下成为当前发送请求成功语义的一部分。**

如果把两者说成“一个有副本，一个没副本”，主链会先在哪理解错？会误以为 `ASYNC_MASTER` 没有 HA，或者误以为 `SYNC_MASTER` 默认就能保证等到 Slave。实际上，前者强调低延迟，后者只是打开了同步确认的可能性；两者都还需要继续讨论故障恢复、角色切换和副本是否真的可接管。

## 第八层：传统 Master/Slave 能复制，但不能自己决定谁接管继续写

到这里，传统 HA 的主链已经闭环了：

```text
Master 本地追加
  → 复制 CommitLog 字节
    → Slave 按 offset 追加
      → Slave 回报进度
        → Master 按 ack / timeout 返回结果
```

但这条链仍然没有回答另一个更大的问题：如果 Master 突然宕机，Slave 怎么自动成为新的写入者？

`DefaultHAService` 主要负责的是：

- 建立和维护主从连接；
- 推送 CommitLog 数据；
- 接收 Slave 物理 offset；
- 统计副本是否追上；
- 为发送请求提供复制确认结果。

它并不在这条传统复制链里完成 Raft 意义上的任期、日志多数派提交、自动选主和角色切换。即使 Slave 已经拥有完整日志，也不等于它已经获得“下一任 Master”的系统身份。

如果把“数据复制完成”直接等同于“故障自动恢复完成”，主链会先在哪失败？会失败在角色层：副本可能有数据，却没有统一裁决谁能写、谁能对外提供新的主视图、旧 Master 恢复后如何避免双主。

这就是为什么后面的 DLedger 和 Controller 必须单独成篇：它们不是把本篇的几个类换个名字，而是把问题从“怎样复制”推进到“怎样形成一致的副本状态和角色决策”。

## 收网：可靠消息不是一个落盘动作，而是一条确认边界链

如果把整篇压成一句话，单机 CommitLog 只能证明 Master 曾经留下了消息；传统 Master/Slave HA 则把这条消息继续推进到另一个 Broker 的 CommitLog，并通过 Slave 的物理 offset 回报，让 Master 知道副本已经追到哪里，再依据 ack 数量和超时决定 Producer 是否拿到成功结果。

```text
Producer
  → Master CommitLog 追加
    → 本地刷盘路径
    → HA 复制路径
      → Slave 连续 offset 校验
        → Slave CommitLog 追加
          → Slave 回报最大物理 offset
            → Master GroupTransferService 等待确认
              → PUT_OK / FLUSH_SLAVE_TIMEOUT
```

到这里，主线只发生了四件事。

第一，Master 本地写成功只是单机事实，不等于已经形成副本。

第二，传统 HA 复制的是 CommitLog 物理字节，Slave 再通过 Reput 构建自己的 ConsumeQueue 等派生视图。

第三，正常路径下 Slave 的 offset 回报和 `GroupCommitRequest` 把复制进度接回 Producer 的发送结果；但当前 HAClient 没有检查 `appendToCommitLog()` 的返回值，所以回报 offset 不能被无条件解释成“副本已经成功落盘”。超时表示确认窗口内未达标，也不应被简化为“消息一定丢失”。

第四，`PUT_OK` 也不是脱离配置的绝对可靠承诺，它还受到本地刷盘结果、`waitStoreMsgOK`、`inSyncReplicas` 和当前 HA 模式影响；传统 Master/Slave 解决的是副本复制与确认边界，还没有解决自动选主和故障后的角色接管。

**本篇的一句话困惑**：Master 已经把消息写进 CommitLog，为什么还不能直接把它当成可靠消息？

**本篇的一句话顿悟**：因为本地追加只建立了 Master 的单机事实；可靠性还需要同一段 CommitLog 按物理 offset 复制到 Slave、由 Slave 在正常路径下回报已追赶位置，并由 Master 按确认策略决定发送结果，而自动选主则是下一层一致性问题。

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Master 本地 CommitLog 写成功，就等于消息已经有副本。”** 本地追加、刷盘和 Slave 复制是不同阶段。
2. **“HA 复制的是 ConsumeQueue。”** 传统 HA 传输的是 CommitLog 物理字节，ConsumeQueue 由 Slave 的 Reput 重新派生。
3. **“SYNC_MASTER 每条消息默认都会等待 Slave。”** `inSyncReplicas` 默认是 `1`，`handleHA()` 在有效 ack 数量不超过 `1` 时直接返回 `PUT_OK`。
4. **“Slave 回报 offset 就一定说明追加成功。”** 当前 HAClient 没有检查 `appendToCommitLog()` 的布尔返回值，这是源码中的失败边界。
5. **“FLUSH_SLAVE_TIMEOUT 就等于消息已经丢失。”** 它表示规定确认窗口内没有达到目标，不能直接推出最终副本状态。
6. **“复制完成就自动完成故障切主。”** 传统 DefaultHAService 不负责 Raft 任期、自动选主和双主隔离。

### 关键证据清单

- `store/src/main/java/org/apache/rocketmq/store/ha/DefaultHAService.java:125`：启动监听、GroupTransferService 与 Slave HAClient。
- `store/src/main/java/org/apache/rocketmq/store/ha/DefaultHAConnection.java:333`：Master 从 CommitLog 读取并按物理 offset/长度传输。
- `store/src/main/java/org/apache/rocketmq/store/ha/DefaultHAClient.java:189`：Slave 校验物理 offset 连续性。
- `store/src/main/java/org/apache/rocketmq/store/ha/DefaultHAClient.java:203`：Slave 追加数据但未检查 `appendToCommitLog()` 返回值。
- `store/src/main/java/org/apache/rocketmq/store/DefaultMessageStore.java:1248`：追加成功时唤醒 Reput，失败时记录错误。
- `store/src/main/java/org/apache/rocketmq/store/ha/GroupTransferService.java:79`：按目标 offset、ack 数量和 deadline 等待复制确认。
- `store/src/main/java/org/apache/rocketmq/store/CommitLog.java:1297`：`needAckNums <= 1` 时直接返回 `PUT_OK`。
- `store/src/main/java/org/apache/rocketmq/store/config/MessageStoreConfig.java:322`：`inSyncReplicas` 默认值为 `1`。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 源码为基线。
- 主线对象是传统 `DefaultHAService`、`DefaultHAConnection`、`DefaultHAClient`；`AutoSwitchHAService`、DLedger、Controller、JRaft 只作为边界提示。
- 本文讨论的是 CommitLog 物理复制与发送确认，不把 Slave 的 Reput 完成、ConsumeQueue 可读、Consumer 消费完成误写成同一时刻。
- 复制确认不等于自动故障恢复；角色切换、共识提交和恢复边界留给后续篇目。
