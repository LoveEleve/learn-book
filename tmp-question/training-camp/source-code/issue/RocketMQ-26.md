# RocketMQ-26. DLedger 与 Controller 为什么不是一回事——数据面共识与控制面裁决对照

> 场景：前面已经分别讲过 `DLedgerCommitLog` 为什么把 RocketMQ 拉进 Raft 世界，也讲过 Controller、选主和角色切换怎样决定 Broker 谁能继续写。写完这两篇之后，读者最容易出现的一个新困惑反而不是细节，而是关系：既然 DLedger 已经有 Leader/Follower，为什么 RocketMQ 还要再做一套 Controller？
>
> 本篇只回答一个问题：**DLedger 与 Controller 为什么不是一回事。** 本篇不是再重讲两边源码，而是把它们对照起来，解释 RocketMQ 5.x 为什么必须同时拥有“日志复制/提交的数据面”和“主角色裁决/注册/通知的控制面”，以及它们究竟在哪一层交汇。

## 先把真正的困惑摆出来：为什么有了 DLedger 还不够

从最直觉的角度看，RocketMQ 5.x 好像已经把一致性问题交给 DLedger 了：

- 它能选出 Leader；
- 能复制日志；
- 能等待提交；
- 还能在角色变化时给 Broker 回调。

于是很容易得出一个看似自然的结论：既然这样，为什么还要单独放一个 Controller 去选主、登记 brokerId、维护 syncStateSet、通知角色变化？难道不是重复建设吗？

这个直觉的问题在于，它把两类完全不同的问题混在了一起：

- **数据面问题**：这段日志有没有被复制并提交，系统现在还承认哪一段消息为真相？
- **控制面问题**：这组副本里现在谁该对外代表系统继续写，谁是 master，谁是 slave，外部路由视图该怎么看？

这两类问题相关，但不是同一问题。

如果只有 DLedger，没有 Controller，主链会先在哪断掉？并不会先坏在“消息能不能复制”，而会坏在“谁有资格把这组副本对外宣布成新的写主”这里。反过来，如果只有 Controller，没有 DLedger，又会先坏在“角色裁出来了，但消息日志本身有没有被安全复制和提交”这里。

所以 RocketMQ 5.x 真正的结构不是“再做一套重复系统”，而是把可靠性拆成两层：

```text
数据面：消息日志有没有被复制并提交
控制面：这组副本里谁还能继续代表系统对外写
```

这篇的关键就是把这两层并排摆清楚。

*关键设计（斜体）：* *RocketMQ 5.x 不是把所有一致性问题都塞给一个组件，而是明确拆成两种真相：DLedger 守“消息日志真相”，Controller 守“角色与路由真相”；只有两者同时成立，外界看到的系统才真正稳定。*[模式: 数据真相边界 + 角色真相边界]

## 第一层：DLedger 回答的是“这段日志能不能算系统真相”

先看 DLedger 这边到底在回答什么。

`DLedgerCommitLog` 真正接管的是消息写入与复制提交链：

```text
MessageSerializer
  → DLedgerServer.handleAppend()
    → Leader / Follower replication
      → committedIndex / committedPos
        → Future response
          → RocketMQ PutMessageStatus
```

这里它解决的核心问题不是“Broker 现在身份是什么”，而是：

- 这条消息有没有进入共识日志；
- 哪些 entry 只是写到了尾部；
- 哪些 entry 已经被提交；
- RocketMQ 现在还承认哪一段物理日志为稳定真相。

这就是为什么前面那篇要反复区分：

- `entry position`
- `ledger end index`
- `committedIndex`
- `committedPos`

因为 DLedger 的世界本质上是**日志真相世界**。它关心的是日志条目、提交边界、恢复边界，以及后续 Reput 还能从哪一段物理数据继续派生 ConsumeQueue。

如果把 DLedger 误写成“主从切换组件”，主链会先在哪失真？会看不见它真正先守住的是日志真相边界：哪些消息当前已经是系统正式承认的存储事实，哪些还只是尾部未完成状态。

所以 DLedger 的第一性问题不是“谁是主”，而是：**哪些日志现在算真。**

## 第二层：Controller 回答的是“这组副本里谁还能代表系统继续写”

再看 Controller。

`ControllerManager` / `DLedgerController` / `ReplicasManager` 这一套并不负责消息 body 的复制与提交。它们维护的是：

- brokerId 分配；
- masterBrokerId / masterEpoch；
- syncStateSet / syncStateSetEpoch；
- broker 心跳；
- `electMaster()` 结果；
- 角色变化通知；
- 最后重新注册 NameServer。

这套世界真正回答的是另一类问题：

```text
这组副本里
  → 谁现在还是活的
  → 谁数据更新鲜
  → 谁应被选成新的 master
  → 哪个 Broker 要切成 slave
  → 外界接下来该把谁当成写主
```

你会发现，这和“某条消息有没有 committed”不是一回事。

Controller 的裁决对象不是单条消息，而是**broker-set 的对外角色与元数据真相**。

如果把 Controller 误写成“消息复制组件”，主链会先在哪失焦？会完全看不见它最重要的职责：让外部世界最终只看到一个清晰的新主视图，而不是每个 Broker 各自根据本地状态自证“我可能是主”。

所以 Controller 的第一性问题也不是“日志真相”，而是：**谁现在有资格代表这组副本继续对外写。**

## 第三层：三种“主”不是同一个主

理解这篇最重要的纪律，仍然是把三种“主”拆开。

### 1. DLedger leader

它回答的是：在某个共识日志组里，谁负责接受 append、推进日志复制和提交边界。

### 2. Controller leader

它回答的是：在控制器集群里，谁有资格裁决 broker-set 元数据、选主和角色通知。

### 3. Broker master

它回答的是：在业务世界里，哪个 Broker 现在应该被 Producer/Consumer 经由 NameServer 路由视图视为主要写入者。

只要把这三者混成一个词，主链就会马上塌：

- 你会以为 DLedger leader 自动等于业务 master；
- 你会以为 Controller 只是把 DLedger 结果翻译一下；
- 你会看不见 Broker 还要落角色、切服务、再注册 NameServer。

所以 RocketMQ 5.x 的结构不该被讲成“一个主节点选出来就全世界完事”，而应该被讲成：

```text
DLedger leader
  → 解决日志写入与提交

Controller leader
  → 解决控制面裁决

Broker master
  → 解决外部可写身份
```

它们之间有依赖，但不是自动同构的三个名字。

## 第四层：为什么只有 DLedger 不够——日志真相成立，不代表外部世界知道该连谁

可以先把“只有 DLedger”的失败方案单独推一遍。

假设系统只有 DLedger 日志复制与提交，没有 Controller 控制面，那会怎样？

- 消息日志也许已经能复制、能提交；
- 某个 Broker 也许在本地数据面上表现出 leader 身份；
- 但新副本 brokerId 谁来发？
- master 死了以后，谁统一裁决新的 masterBrokerId？
- syncStateSet 谁来维护？
- 哪个 Broker 负责通知所有其他 Broker 切角色？
- NameServer 路由最后到底该看到谁？

也就是说，**日志真相成立，并不自动推出外部角色真相已经成立。**

如果没有 Controller，RocketMQ 就会缺一层统一裁决者：每个 Broker 也许都能看见一些本地事实，但没人能把这些事实收束成“现在全系统应该以谁为主”这份对外一致视图。

所以只有 DLedger，主链会先断在：

```text
日志能写对
  ≠
外部世界知道该连谁
```

这就是为什么前面那篇 Controller 一直强调“NameServer 路由再暴露”必须单独成立——它不是日志复制顺便附带的副产品。

## 第五层：为什么只有 Controller 也不够——角色裁出来了，不代表消息真相已经安全

反过来，再推一遍“只有 Controller”的失败方案。

假设系统只有 brokerId、心跳、选主、角色通知，但没有 DLedger 这层统一日志提交边界，会怎样？

- 你可以选出一个新的 master；
- 可以通知大家角色变了；
- 可以重新注册 NameServer；
- 但这台 Broker 到底是不是拥有最完整、最安全、已提交的消息日志？

如果没有 DLedger 的日志复制/提交真相支撑，Controller 就只能基于：

- 心跳里的 epoch / maxOffset / priority

做裁决。但这些信息最终之所以有意义，仍然建立在底层日志复制机制已经把消息写对、复制对、提交边界定对的前提上。

也就是说：**角色真相如果脱离日志真相，就可能变成“选出了一个主，但这台主手里的消息世界并不可靠”。**

所以只有 Controller，主链会先断在：

```text
外部世界知道该连谁
  ≠
那台 Broker 手里真的有可靠消息真相
```

这也是 RocketMQ 5.x 不把“选主”和“日志提交”压成同一套结构的原因：一套负责选人，一套负责确保证据。

## 第六层：两层真正的交汇点，不在概念上，而在 Broker 落角色与 NameServer 再暴露上

前面讲的是分工，下一步必须讲交汇。

DLedger 和 Controller 不是平行永不相交的两层。它们真正交汇在两个非常具体的地方：

### 1. Broker 本地角色切换

`ReplicasManager.changeToMaster()` / `changeToSlave()` 会在 Controller 裁出结果后，推动：

- `BrokerRole` 切换；
- `AutoSwitchHAService` 切换；
- `syncStateSet` 更新；
- 特定服务开关变化。

这一步的意义是：控制面的“裁决结果”必须被 Broker 本地真正消费，才能变成运行中的角色现实。

### 2. NameServer 路由再暴露

`registerBrokerWhenRoleChange()` 会在角色切换后重新 `registerBrokerAll(...)`。这一步的意义是：外部 Producer/Consumer 看见的新主视图，并不是 Controller 内存里的布尔值，而是通过 NameServer 再暴露出去的路由事实。

可以把交汇压成：

```text
DLedger
  → 给出日志可承认边界
Controller
  → 给出角色裁决结果
ReplicasManager
  → 把裁决落成本地 Broker 角色
NameServer
  → 把新角色再暴露给外部世界
```

这说明 RocketMQ 5.x 的稳定闭环不只是“两层都存在”，而是“两层都必须最终交汇到 Broker 角色与外部路由视图上”。

## 第七层：为什么这篇值得单独做对照——它补的是“分工图”，不是新实现

如果不单独做这篇对照，读者很容易在 `RocketMQ-14` 和 `RocketMQ-15` 之间留下一个理解空洞：

- 知道 DLedger 在复制和提交日志；
- 知道 Controller 在选主和通知角色；
- 但不知道两者为什么都必须存在，彼此又怎么分工。

这个空洞会直接导致后续两个误解：

1. 把 Controller 看成 DLedger 的重复建设；
2. 把 DLedger 看成“既能复制日志、又天然解决所有外部角色问题”的全能组件。

所以这篇机动补深的价值，不是新增一个机制，而是把前两篇已经建立的机制关系图重新画清楚。它补的不是功能实现，而是**分工图**。

## 收网：RocketMQ 5.x 的可靠性闭环，不是一个组件包打天下，而是两种真相分层成立

如果把整篇压成一句话，RocketMQ 5.x 里 DLedger 和 Controller 之所以都必须存在，是因为它们根本不在回答同一个问题：DLedger 负责把消息日志复制并提交成可承认的数据真相，Controller 负责把副本集合裁决成对外可写的角色真相；只有当数据真相和角色真相同时成立，并在 Broker 落角色与 NameServer 再暴露上汇合，系统外部看到的世界才真正稳定。

```text
DLedger
  → 哪些日志现在算真

Controller
  → 这组副本里谁现在算主

ReplicasManager + NameServer
  → 把“日志真相 + 角色真相”一起落成外部可见世界
```

到这里，主线只发生了五件事。

第一，DLedger 守的是消息日志真相，不是最终外部角色身份。

第二，Controller 守的是 broker-set 角色真相，不是单条消息提交边界。

第三，DLedger leader、Controller leader、Broker master 是三种不同层级的“主”。

第四，只有 DLedger 没法给外部世界统一主视图，只有 Controller 也没法保证消息日志真相可靠。

第五，真正的稳定闭环来自两层分工之后在 Broker 落角色与 NameServer 路由再暴露上的交汇。

**本篇的一句话困惑**：为什么 RocketMQ 有了 DLedger 还要再做 Controller？

**本篇的一句话顿悟**：因为 DLedger 只回答“哪些日志已经能算数”，Controller 才回答“这组副本里谁现在有资格继续对外写”；RocketMQ 5.x 的可靠性不是一个组件包打天下，而是数据真相边界和角色真相边界两层同时成立。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“DLedger 已经有 Leader，所以 Controller 是重复建设。”** 两者解决的问题不同。
2. **“Controller 选完主，消息真相自然就对了。”** 角色真相不自动等于日志真相。
3. **“DLedger leader 就等于 Broker master。”** 数据面 leader 不自动等于外部写主身份。
4. **“NameServer 是选主裁决者。”** 它最终只是再暴露路由视图。
5. **“这篇是在重讲 DLedger 或 Controller 实现细节。”** 本篇讲的是两层分工与交汇，不是逐行源码复述。

### 关键证据清单

- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:548`：消息进入 DLedger 数据面追加与提交链。
- `store/src/main/java/org/apache/rocketmq/store/dledger/DLedgerCommitLog.java:620`：数据面提交结果映射为 RocketMQ 发送结果。
- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:102`：控制面初始化 `DLedgerController`。
- `controller/src/main/java/org/apache/rocketmq/controller/ControllerManager.java:146`：控制面检测 broker 失活并触发选主。
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:225`：Broker 消费控制面裁决并落角色。
- `broker/src/main/java/org/apache/rocketmq/broker/controller/ReplicasManager.java:321`：Broker 角色变化后重新注册 NameServer。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇是对照与收束篇，不重复展开 DLedger/Controller 各自内部算法细节。
- 本文不展开 JRaftController、proxy 或 NameServer 内部 RouteInfoManager，只聚焦数据面/控制面对照。
- 本文把 NameServer 视为外部路由暴露层，不把它误写成控制面裁决者。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-14` 的 DLedger 数据面、`RocketMQ-15` 的 Controller 控制面。
- 后续桥接：RocketMQ 机动补深篇到这里已经把发送、长轮询、路由缓存、处理器分发和数据面/控制面对照补齐；下一步更适合转入 RocketMQ 全卷二轮 consistency review，或切到 Kafka 开篇。