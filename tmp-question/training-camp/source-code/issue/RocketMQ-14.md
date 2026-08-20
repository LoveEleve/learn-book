# RocketMQ-14. DLedgerCommitLog 为什么把 RocketMQ 拉进了 Raft 世界

> 场景：上一篇已经把传统 Master/Slave 的复制主链讲清了：Master 追加 CommitLog，向 Slave 传输物理字节，Slave 回报 offset，Master 再按 ack 和超时决定发送结果。但这条链仍然有一个结构性问题：复制进度、提交边界和角色切换并没有被同一个共识日志统一承载。
>
> 本篇只回答一个问题：**RocketMQ 为什么需要 `DLedgerCommitLog`，以及它怎样把消息追加从“Master/Slave 复制”推进成“Leader/Follower 日志复制与 quorum 提交”。** 本篇聚焦 RocketMQ 存储层与 DLedger 的桥接，不展开 Controller 的独立选主实现；下一篇再回答 Broker 谁能继续写、角色怎样切换。

## 先把真正的困惑摆出来：换了 CommitLog，为什么就进入 Raft 世界

如果只是看类名，`DLedgerCommitLog extends CommitLog` 很容易给出一个过于轻松的判断：它大概只是把原来的 CommitLog 文件换成 DLedger 文件，其他消息流程应该不变。

这个判断只对了一半。

RocketMQ 确实仍然需要自己的消息格式、Topic/Queue offset、`AppendMessageResult`、Reput 和 ConsumeQueue；但消息真正写入的动作已经不再是 `MappedFile.appendMessage()` 那种单机追加。`DLedgerCommitLog.asyncPutMessage()` 会把 RocketMQ 消息编码成 DLedger entry，调用 `DLedgerServer.handleAppend()`，再等待 `AppendFuture` 对应的 DLedger 响应码。

这意味着消息写入至少出现了几种不能混为一谈的状态：

- RocketMQ 消息已经完成序列化；
- Leader 本地创建了一个 DLedger entry；
- entry 已经复制到足够的成员；
- DLedger 的 committed index 与 committed position 分别推进到各自边界；
- RocketMQ 的 Reput 已经把提交后的物理数据派生到 ConsumeQueue。

如果把 Leader 本地 entry 位置直接当作消息成功，主链会先在哪失败？会失败在“本地有日志、集群没有提交”的边界：Leader 可能在 quorum 完成前宕机，Producer 却已经收到成功，后续 Leader 选举就必须面对一条尚未稳定提交的 entry。

所以 `DLedgerCommitLog` 不是一个存储目录适配器，而是 RocketMQ 消息存储主链接入共识日志的桥。

```text
Producer
  → RocketMQ 消息编码
    → DLedger Leader append entry
      → Follower 复制
        → quorum 结果与 committed index 推进
          → committed position 对外暴露稳定读取边界
            → Future 响应
              → RocketMQ PUT_OK
                → Reput 派生 ConsumeQueue
```

*关键设计（斜体）：* *DLedgerCommitLog 把 RocketMQ 的“消息追加成功”从本地文件结果改造成 DLedger Future 的共识结果；RocketMQ 保留消息存储语义，但不再自己实现传统 HA 那套 offset 等待。*[模式: 消息存储适配层 + 共识日志提交]

## 第一层：DefaultMessageStore 先决定“这条消息由谁来落”

要理解 DLedger 主链，第一步不是直接看 `DLedgerCommitLog.asyncPutMessage()`，而是先看它为什么会被创建。

`DefaultMessageStore` 构造时根据 `MessageStoreConfig.isEnableDLegerCommitLog()` 选择 CommitLog 实现：

```text
if enableDLegerCommitLog
    → new DLedgerCommitLog(this)
else
    → new CommitLog(this)
```

这一步发生在消息进入 Broker 请求处理器之前，所以 DLedger 不是某次发送请求临时选择的策略，而是 Store 级别的结构选择。选择完成后，后面的 Broker 仍然通过 `MessageStore`/`CommitLog` 抽象写消息，但实际落点已经变成 DLedger。

`DLedgerCommitLog` 构造函数还会把 RocketMQ 配置映射进 `DLedgerConfig`：

- `selfId`：当前成员身份；
- `group`：DLedger 日志组；
- `peers`：成员地址集合；
- `storeBaseDir` 和 `dataStorePath`：日志存储位置；
- `mappedFileSizeForEntryData`：entry 数据文件大小；
- leader 偏好、批量推送和磁盘清理参数。

这说明 DLedger 的成员关系不是从 RocketMQ 的传统 `BrokerRole` 推导出来的，而是由 DLedger 的 group/peer 配置直接建立。RocketMQ 的 Broker 角色仍然存在，但它要通过 DLedger 角色状态和 Broker 角色回调连接起来。

如果仍然让传统 `HAService` 和 DLedger 同时承担复制，主链会先在哪混乱？会混乱在两套复制语义同时修改同一份 CommitLog：传统 HA 期待 `appendData()`，而 `DLedgerCommitLog.appendData()` 明确返回 `false`，防止旧 HA 路径继续写 DLedger 日志。

因此这里有一个重要边界：

```text
传统 CommitLog
  → DefaultHAService / appendData

DLedgerCommitLog
  → DLedgerServer.handleAppend
  → DLedger 自己管理复制与提交
```

这不是两套实现可以随意叠加，而是 Store 在初始化阶段已经选择了其中一条主路径。

## 第二层：DLedgerCommitLog 先把 RocketMQ 消息编码成 entry

传统 CommitLog 的追加动作可以直接接收 `MessageExtBrokerInner`，而 DLedger 的追加接口接收的是 entry body。因此 `DLedgerCommitLog` 必须补一层消息序列化。

`asyncPutMessage()` 的前半段仍然保留 RocketMQ 语义：

- 设置存储时间和 body CRC；
- 设置消息版本；
- 生成 Topic/Queue 相关的逻辑 offset；
- 按 `topic-queueId` 获取细粒度锁；
- 校验消息体和 properties 大小。

随后 `MessageSerializer.serialize()` 按 RocketMQ 消息布局生成一段 ByteBuffer，里面仍然包含 total size、magic code、body CRC、queue id、queue offset、physical offset、系统标记、时间、地址、body、topic 和 properties 等字段。

这里有一个看似矛盾的细节：序列化时消息里的 physical offset 先被写成占位值，真正写入 DLedger 后，`DLedgerFileStore` 的 append hook 再把 entry 位置加上 body offset 写回消息物理位置。

```text
RocketMQ MessageExtBrokerInner
  → MessageSerializer
    → DLedger entry body
      → DLedger append hook 写入物理位置
        → Reput 能按 RocketMQ 消息格式解析
```

这就是为什么 DLedgerCommitLog 不能直接把任意业务字节交给 DLedger：RocketMQ 后续的 Reput、ConsumeQueue 和消息查询仍然需要识别自己的消息格式。

如果只把消息 body 放入 DLedger，主链会先在哪失败？会失败在日志虽然能复制，但 RocketMQ 不知道 Topic、queueId、queueOffset 和 properties，Reput 无法构建消费索引，Consumer 也无法按原有协议取回消息。

所以 DLedger entry 是共识日志的载体，RocketMQ 消息格式仍是 entry body 的语义内容。

## 第三层：handleAppend() 把一次发送带进 Leader/quorum 链

消息编码完成后，`DLedgerCommitLog.asyncPutMessage()` 会构造 `AppendEntryRequest`：

```text
request.group = dLedgerConfig.group
request.remoteId = current selfId
request.body = serialized RocketMQ message
```

随后调用：

```text
dLedgerFuture = dLedgerServer.handleAppend(request)
```

这一步与上一篇传统 HA 的差别非常大。传统 HA 是 Master 取 CommitLog 数据，主动推给连接上的 Slave；这里 RocketMQ 把“追加并复制”的责任交给 DLedgerServer，DLedger 根据当前 `MemberState`、Leader 状态和 peer 配置决定这次 append 能否进入共识日志流程。

`handleAppend()` 返回的 Future 同时承担两个角色：

- 立即提供 entry position，让 RocketMQ 能构造 `AppendMessageResult` 和消息 ID；
- 后续完成时提供 `AppendEntryResponse`，让 RocketMQ 知道这条 entry 最终是成功、非 Leader、quorum 超时还是资源受限。

这两个时刻必须分开：

```text
Future position
  ≠
Future response code
```

如果只盯着 `dledgerFuture.getPos()`，主链会先在哪理解错？会把“日志位置已经分配”误写成“日志已经提交”。代码也明确把 `getPos() == -1` 视为失败，返回 `OS_PAGE_CACHE_BUSY`；但 position 正常并不跳过后续 Future 响应等待。

## 第四层：Producer 的 PUT_OK 由 DLedgerResponseCode 决定

`DLedgerCommitLog` 并不是在 `handleAppend()` 返回后立即构造最终发送结果，而是在 Future 完成后，根据 `AppendEntryResponse` 的 DLedger 响应码映射 RocketMQ 状态。这里要把话说严一点：Future 完成代表 DLedger 已经给了这次 append 一个结果，但正文不能把所有 `PUT_OK` 都直接写成“多副本 quorum 已被完整验证”。在单节点、手工固定 leader 或测试关闭选举的场景里，Future 同样会完成，但它证明的是当前配置下的 DLedger 提交结果，而不是必须存在一个完整生产级多副本故障场景。

核心映射可以压成这样：

```text
SUCCESS
  → PUT_OK

INCONSISTENT_LEADER / NOT_LEADER / LEADER_NOT_READY / DISK_FULL
  → SERVICE_NOT_AVAILABLE

WAIT_QUORUM_ACK_TIMEOUT
  → IN_SYNC_REPLICAS_NOT_ENOUGH

LEADER_PENDING_FULL
  → OS_PAGE_CACHE_BUSY
```

这里最关键的是 `SUCCESS` 的位置：它不是“Leader 本地写入成功”的同义词，而是 DLedger Future 已经给出了成功响应。`WAIT_QUORUM_ACK_TIMEOUT` 也没有直接沿用传统 HA 的 `FLUSH_SLAVE_TIMEOUT`，而是映射为 `IN_SYNC_REPLICAS_NOT_ENOUGH`，因为 DLedger 这条路径的失败语义是 quorum 未达标，而不是传统 Master/Slave 的 Slave flush 等待超时。

这条映射还说明一个重要事实：`DLedgerCommitLog` 是跨语义翻译器。它把 DLedger 的状态码翻译成 RocketMQ Producer 能理解的 `PutMessageStatus`，但不会把所有底层状态伪装成成功。

如果所有 DLedger 异常都统一返回“发送失败”，主链会先在哪损失信息？Producer 无法区分应该重试路由、等待 Leader ready、处理 quorum 不足，还是降低写入压力。至少在源码当前映射里，Leader 不可用、quorum 不足和 pending full 已经被分成不同 RocketMQ 状态。

## 第五层：DLedger 的多个位置，不能被压成一个 offset

上一篇传统 HA 主要围绕物理 offset 展开，而 DLedger 会同时出现多种位置概念。它们都和“进度”有关，但回答的不是同一个问题。

### 1. entry position

`dledgerFuture.getPos()` 是 DLedger entry 的位置。RocketMQ 在构造 `AppendMessageResult` 时，会把它加上 `DLedgerEntry.BODY_OFFSET`，得到消息数据在 DLedger 文件布局中的物理位置，并据此生成消息 ID。

它回答的是：**这条 entry 在日志文件中的位置在哪里。**

### 2. ledger end index

`ledgerEndIndex` 更接近当前日志尾部的索引边界，表示日志里已经存在或正在追赶到哪一条 entry。它回答的是：**日志尾巴推进到了哪里。**

### 3. committed index / committed position

这两个量必须拆开看，不能写成一个复合名词。

- `committedIndex` 回答的是：**日志序号层面已经提交到了哪一条 entry。** `DLedgerRoleChangeHandler` 在角色切换时等待的是 `ledgerEndIndex == committedIndex`，也就是当前日志尾部已经全部提交，不再有“尾部 entry 已存在但还没提交”的缺口。
- `committedPos` 回答的是：**字节位置层面已经可以作为稳定读取边界的数据到哪里。** `DLedgerCommitLog.getMaxOffset()` 优先返回 DLedger store 的 `committedPos`；`getConfirmOffset()` 也直接返回 `getMaxOffset()`。

这说明 RocketMQ 在 DLedger 模式下，把“服务切换前是否还有未提交 entry”主要交给 `committedIndex` 判断，把“读取、恢复和对外稳定可见的数据边界”主要交给 `committedPos` 判断，而不是继续使用传统 HA 的 confirm offset 机制。

### 4. queue offset

queue offset 仍然属于 RocketMQ 消费语义，由 Topic/Queue 逻辑推进并写入消息格式。它回答的是：**某条消息在某个逻辑队列里的消费序号是多少。**

四者关系可以先写成：

```text
entry position       → 日志文件物理位置
ledger end index     → 日志尾部索引
committed position   → DLedger 稳定提交边界
queue offset         → RocketMQ 消费队列位置
```

如果把它们全部叫“消息 offset”，主链会先在哪坏掉？会把“日志已经写到尾部”“日志已经 quorum 提交”“Consumer 已经能按 queue offset 读取”误判成同一件事。

## 第六层：DLedger 提交以后，RocketMQ 仍然需要 Reput

DLedger 负责把 entry 复制、提交并持久化，但 RocketMQ 的消费视图并不会因为 DLedger committed position 推进就自动完成。

`DefaultMessageStore` 的启动流程仍然会启动 Reput，并在恢复/启动阶段检查 CommitLog 与 ConsumeQueue 的 dispatch 进度。DLedgerCommitLog 的 `checkMessageAndReturnSize()` 还需要适配 DLedger entry 的 body offset：它跳过 DLedger entry 元数据，再调用 RocketMQ 原有消息解析逻辑。

因此，消费可见性仍然是另一条派生链：

```text
DLedger committed data
  → DLedgerCommitLog.getData()
    → checkMessageAndReturnSize()
      → Reput
        → ConsumeQueue / IndexFile
          → Consumer pull
```

为什么不能让 DLedger 直接“顺便建好 ConsumeQueue”？因为 DLedger 只知道共识日志 entry，不应该承担 RocketMQ Topic/Queue 派生索引的全部业务语义。Store 层保留 Reput，使“日志提交”和“消费视图构建”仍然分层。

这也解释了一个边界：Producer 收到 `PUT_OK` 代表 DLedger Future 返回成功，但不自动代表 Consumer 在同一时刻已经能读到消息。测试中也会等待 `dispatchBehindBytes() == 0` 和消费队列最大 offset 推进后，再验证拉取结果。

## 第七层：恢复时，DLedger committed position 会反过来约束消费索引

DLedger 模式下的恢复不是简单地“重新扫描最后一个 CommitLog 文件”。`DLedgerCommitLog` 会加载并恢复 DLedger 文件；如果已有 DLedger 文件，还会记录新旧 CommitLog 的分界，并根据恢复出来的**有效物理处理位置**检查 ConsumeQueue 是否存在超前的冗余数据。

这里要避免一个偷换：恢复逻辑并不总是直接拿 `committedPos` 当唯一边界。正常恢复里会用 DLedger 文件写入/恢复得到的最大物理位置，异常恢复里会边扫描边推进 `processOffset`。共同点不是“总有一个 committedPos 被直接拿来裁决”，而是：**RocketMQ 会用 DLedger 恢复出来的有效物理日志边界，反过来约束 ConsumeQueue 是否超前。**

正常恢复和异常恢复都包含一个共同判断：如果 ConsumeQueue 的最大物理位置已经大于等于 DLedger 恢复出的有效处理位置，就要截断冗余逻辑文件，避免消费索引指向一段并未处于当前有效日志边界内的数据。

```text
DLedger 文件恢复
  → 得到有效 processOffset / maxPhyOffset 边界
    → 对比 ConsumeQueue 最大物理位置
      → CQ 超前则 truncateDirtyLogicFiles()
      → Reput 从正确边界继续推进
```

这里体现了 DLedger 对 RocketMQ 存储恢复的真正影响：恢复不再只看文件尾部有没有字节，而要看哪些字节仍然处于恢复后可继续承认的有效日志范围。

如果恢复只按 ConsumeQueue 最大 offset 继续，主链会先在哪失败？会把之前派生出来、但后来不在有效 DLedger 提交边界内的索引继续暴露给 Consumer，造成“索引看起来有消息，底层稳定日志却没有对应事实”的错位。

## 第八层：Leader/Follower 角色变化会反向影响 Broker 服务状态

DLedger 并不只返回 append 成功或失败，还会产生 Leader、Follower、Candidate 等角色变化。RocketMQ 在 `BrokerController.initializeMessageStore()` 中为 DLedger LeaderElector 注册 `DLedgerRoleChangeHandler`，把 DLedger 的角色事件接回 Broker。

角色处理不是简单地修改一个 `brokerRole` 字段：

- Candidate/ follower 路径会切成 Slave，并关闭部分 Master 特有服务；
- Leader 路径会先等待 DLedger 日志追平、`committedIndex` 与 `ledgerEndIndex` 对齐，并等待 `dispatchBehindBytes() == 0`；
- 确认追平后，才把 Broker 切成 `SYNC_MASTER` 并重新注册。

这说明 DLedger 的共识角色和 RocketMQ 的对外 Broker 角色之间存在一个桥接层：

```text
DLedger MemberState role
  → DLedgerRoleChangeHandler
    → Broker 服务状态
      → Namesrv 注册视图与对外写入能力
```

但本篇到这里必须收住。这里展示的是“日志角色怎样影响 Broker 服务状态”的边界，不展开 Controller 如何做集群级选主，也不把 DLedger 自身的 LeaderElector 讲成 RocketMQ Controller。

## 收网：DLedgerCommitLog 改变的是可靠性主链的确认方式

如果把整篇压成一句话，`DLedgerCommitLog` 的价值不是把 CommitLog 文件换到另一个目录，而是把 RocketMQ 消息追加接入 DLedger 的 Leader/Follower 日志复制、quorum 提交和 Future 响应链。

```text
DefaultMessageStore
  → DLedgerCommitLog
    → MessageSerializer
      → DLedgerServer.handleAppend()
        → Leader / Follower replication
          → quorum response
            → DLedgerResponseCode
              → RocketMQ PutMessageStatus
                → committed data
                  → Reput / ConsumeQueue
```

到这里，主线只发生了五件事。

第一，Store 在初始化阶段选择 `DLedgerCommitLog`，所以 DLedger 是存储主路径，不是一次发送请求的临时插件。

第二，RocketMQ 消息仍然按自己的格式编码进 DLedger entry，后续 Reput、ConsumeQueue 和查询语义没有被 DLedger 取代。

第三，entry position、ledger end index、committed position 和 queue offset 各自回答不同问题，不能被压成一个“消息 offset”。

第四，Producer 的 `PUT_OK` 来自 DLedger Future 的 `SUCCESS` 响应；quorum timeout、非 Leader 和资源压力会被翻译成不同的 RocketMQ 失败状态。

第五，DLedger 提供的是共识日志和角色事件，Broker 的服务切换与 Controller 的控制面仍然是下一层问题。

**本篇的一句话困惑**：为什么 RocketMQ 不继续用传统 Master/Slave，而要把 CommitLog 接到 DLedger？

**本篇的一句话顿悟**：因为传统 HA 只围绕复制 offset 等待，DLedgerCommitLog 则把消息追加、Leader/Follower 复制、quorum 提交和 Producer 结果收进同一条 Future 链；RocketMQ 保留自己的消息与消费索引语义，但可靠性的稳定边界交给 DLedger 的 committed position。

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“DLedgerCommitLog 只是换了一个 CommitLog 文件目录。”** 它改变了写入、复制、提交和失败返回主链。
2. **“`getPos()` 正常就等于消息发送成功。”** entry position 和 Future response 是两个时刻，最终状态由响应码映射决定。
3. **“DLedger 的所有 offset 都是同一种 offset。”** entry position、ledger end index、committed position 和 queue offset 职责不同。
4. **“DLedger committed 就等于 ConsumeQueue 已经可读。”** Reput 仍要把提交后的物理消息派生为消费索引。
5. **“DLedger Leader 就等于 RocketMQ Broker 已经完成主切换。”** `DLedgerRoleChangeHandler` 还要等待追平并切换 Broker 服务状态。
6. **“单节点测试的 PUT_OK 就证明了多副本 quorum。”** 单节点只能验证本地 Leader/Future/存储桥接，不等于多节点复制故障场景。

### 关键证据清单

- `store/src/main/java/org/apache/rocketmq/store/DefaultMessageStore.java:227`：根据配置选择 `DLedgerCommitLog`。
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:88`：构造 DLedger 配置与文件存储。
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:137`：启动 DLedgerServer。
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:548`：消息序列化、构造 AppendEntryRequest 并调用 `handleAppend()`。
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:620`：Future 响应码映射为 RocketMQ PutMessageStatus。
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:821`：禁用传统 HA 的 `appendData()` 路径。
- `broker/src/main/java/org/apache/rocketmq/broker/BrokerController.java:783`：注册 DLedger 角色变化处理器。
- `broker/src/main/java/org/apache/rocketmq/broker/dledger/DLedgerRoleChangeHandler.java:56`：角色变化、日志追平与 Broker 角色切换。
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:293`：恢复 DLedger 文件并处理 ConsumeQueue 超前边界。
- `store/src/test/java/org/apache/rocketmq/store/dledger/DLedgerCommitlogTest.java:120`：正常/异常恢复与消费视图验证。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 源码和仓库内 DLedger 依赖 API 为基线。
- 本篇聚焦 `DLedgerCommitLog`、`DLedgerServer`、Future 响应和 RocketMQ Store/Reput 桥接；不展开 Controller、JRaftController、DLedgerController 的控制面实现。
- 本文使用单节点测试作为本地存储与恢复证据，不把它当作多节点 quorum 故障测试。
- 本文区分 `committedIndex`、`committedPos`、RocketMQ Reput 和 Consumer 可见性；角色切换与故障恢复总串联留给后续篇目。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-4` 的 CommitLog 真相层、`RocketMQ-5/6` 的 Reput/ConsumeQueue 派生链、`RocketMQ-13` 的传统 HA 确认边界。
- 后续桥接：下一篇 `RocketMQ-15` 继续回答 Controller、选主和 Broker 谁能继续写；后文 `RocketMQ-18` 再统一收束故障恢复、可见性与重复投递边界。
