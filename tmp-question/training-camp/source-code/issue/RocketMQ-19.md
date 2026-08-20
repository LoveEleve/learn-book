# RocketMQ-19. 事务消息为什么不能直接发正式消息 —— 半消息主链

> 场景：前面已经把 RocketMQ 的主链、可靠性边界和故障恢复讲清了。走到事务消息这里，读者最容易产生的直觉问题通常不是“二阶段怎么提交”，而是更早的一步：如果最终还是要给 Consumer 一条正常消息，为什么不一开始就把它按普通消息发出去？
>
> 本篇只回答一个问题：**事务消息为什么不能直接发正式消息，以及为什么 RocketMQ 必须先写 half message。** 本篇聚焦 prepare/half-message 阶段，不展开回查和二阶段 commit/rollback；那是下一篇和下下篇的主题。

## 先把真正的困惑摆出来：为什么不能直接发正式消息

从业务代码视角看，事务消息最容易被误解成这样一条朴素路径：本地事务开始，消息也先发出去；如果本地事务成功，那消息就算成功；如果本地事务失败，再想办法通知下游别处理，或者再补一条回滚消息。

这个直觉的问题在于，它把“消息对外可见”放在了“本地事务是否最终成立”之前。

而只要顺序这样排，主链就会立刻出现一个不可逆的问题：**下游可能已经看见并处理了正式消息，但本地事务后来却失败了。** 到那时，你已经不再是在“决定要不要把消息发出去”，而是在事后试图补救一个已经暴露给消费世界的事实。

可以把最直觉的错误路径先压成一张图：

```text
Producer 先发正式消息
  → Broker 按普通消息落到真实 Topic/Queue
    → Consumer 可能已经看见并处理
      → Producer 本地事务后来失败
        → 系统试图补偿，但正式消息已经先污染了消费世界
```

这条路径真正先坏掉的，不是 Broker 落盘，也不是 Consumer 能不能收到，而是：**消息一旦以正式消息身份进入真实 Topic/Queue，它就已经属于普通消费主链了。** 后面你再说“其实本地事务没成功”，这句话已经来晚了。

所以事务消息的第一性问题不是“如何把最终消息发给 Consumer”，而是：**在本地事务尚未决断之前，Broker 到底该保存一份什么形态的系统事实。**

RocketMQ 给出的答案不是“先发正式消息再补救”，而是“先发一条 half message，让它先变成可恢复但暂不可见的系统事实”。但这里还要把客户端时序说严：Producer 不是先执行本地事务、再随手写 half message；真实顺序是先给消息打上事务 prepared 属性并发送 prepare 请求，Broker 先把 half fact 落下来，客户端拿到 `SEND_OK` 以后才执行本地事务，随后再走 `endTransaction()`。也就是说，half message 不是本地事务之后的补记，而是本地事务执行前就已经建立好的 Broker 侧待决事实。

```text
Producer 发事务消息
  → Broker 不直接投到真实 Topic
    → 先写 half message
      → 本地事务仍在进行或结果未定
        → half message 可恢复、可回查
          → 不进入业务真实消费主链，但会进入事务检查链
            → 后续再决定 commit / rollback / check
```

*关键设计（斜体）：* *RocketMQ 事务消息先写 half message，不是为了多绕一层，而是为了在“本地事务尚未决断”这个空窗期里，先建立一份系统能恢复、但下游还看不见的事实。*[模式: 待决事实先落盘，再决定是否转正]

## 第一层：先发正式消息，会把“待决状态”错误暴露成“已成立事实”

为什么事务消息不能直接发正式消息？最根本的原因，是普通消息主链默认表达的是：这条消息已经可以进入真实 Topic、真实 Queue，并准备被 Consumer 看见。

一旦消息以这种身份进入 Broker，后面的主链就会自然推进：

- CommitLog 接纳它；
- ConsumeQueue 为它建立索引；
- Consumer 按正常主链拉取它；
- 下游业务把它当成已经成立的上游事实。

问题就在这里：对于事务消息，Producer 在这个时间点根本还没有资格对外宣布“这件事已经成立”。因为本地事务也许刚执行、也许尚未提交、也许马上就失败。

如果仍然把它按普通消息发出去，主链会先在哪坏掉？会先坏在**可见性边界错位**：系统对下游宣布了一条已经成立的事实，但上游业务自己其实还在“待决”状态。

这也是为什么事务消息不能被理解成“普通消息 + 回滚通知”。因为那样做的前提是：你已经允许一条尚未最终成立的业务事实先污染正式消费世界，然后再指望补偿去擦干净它。RocketMQ 在这里选择的是更严格的路径：**在事务未决时，先别让它进入正式消费世界。**

所以 half message 真正先补的，不是事务功能花样，而是消息系统的一个基本纪律：

```text
本地事务未决
  → 不允许正式消息先进入真实 Topic/Queue
```

## 第二层：等本地事务做完再发，也不行，因为 Broker 会失去恢复锚点

看见上一层的问题以后，很多人会马上提出第二种朴素方案：那我别先发正式消息了，我等本地事务完全执行成功以后，再去发消息，不就既避免半消息，又避免下游提前消费了吗？

这个方案比“先发正式消息”更谨慎，但仍然不够。

因为它把另一个关键问题暴露出来了：**如果 Producer 在本地事务已经成功、但正式消息还没发出去之前崩溃，Broker 根本不知道有这样一条待发消息存在。**

换句话说，前一个错误方案是“过早暴露”；后一个错误方案则是“根本没留下系统事实”。

只靠 Producer 本地记住“我待会要发一条消息”，主链会先在哪坏掉？会坏在故障恢复时：Broker 既没有一条待决消息可查，Controller 也不知道这里曾有过事务消息的悬而未决状态，整个系统就无法对这条消息做任何后续回查或恢复判断。

所以 RocketMQ 需要的不是“晚点再发”，而是：**即使本地事务结果还没决出来，Broker 也必须先拥有一份能证明‘这里确实有一条待决事务消息存在’的系统事实。**

这就是 half message 存在的第二重理由：它不仅阻止正式消息过早可见，还给故障恢复留下锚点。

## 第三层：half message 真正建立的，是“可恢复但不可正式消费”的系统事实

走到这里，就可以更准确地给 half message 定位了。

half message 不是“慢一点投递的正式消息”，也不是“普通消息暂存在一个缓存区里”。它在 RocketMQ 里的真正位置是：

- **它已经是 Broker 存储系统承认的一条事实**；
- 但**它暂时还不是业务普通消费主链承认的正式消息事实**；
- 同时**它会进入 Broker 自己的事务检查世界，而不是对业务 Consumer 暴露成真实 Topic 消息**。

这两句话必须一起看。

如果它根本不是系统事实，故障恢复时就没有锚点；如果它已经是正式消费事实，下游又会过早看见它。事务消息需要的恰恰是中间层：**先成为系统内部可恢复、可检查的事实，但不直接进入真实消费世界。**

可以把这条半消息主链压成：

```text
事务消息发送
  → Broker 先写 half message
    → 系统内部承认“有一条待决事务消息存在”
      → 它不按真实 Topic/Queue 进入业务普通消费主链
        → 但事务服务仍会扫描 half queue 做检查/回查准备
          → 后续等本地事务 / 回查 / 二阶段再决定是否转正
```

如果不先建立这层中间事实，RocketMQ 在事务场景下就只剩下两个糟糕选项：

- 提前暴露正式消息；
- 或完全不留下任何 Broker 侧事实。

half message 本质上就是在两种坏方案之间建立第三条路。

## 第四层：为什么必须单独写进 `RMQ_SYS_TRANS_HALF_TOPIC`

既然 half message 的关键是“可恢复但暂不可见”，那下一步自然就是：它为什么不能还留在原 Topic，只是在消息属性里打一个“未提交”标记？

RocketMQ 没这么做，而是把它写进专门的 `RMQ_SYS_TRANS_HALF_TOPIC`。

这不是为了命名花样，而是为了**隔离正式消费主链和事务待决主链**。

`TransactionalMessageBridge.parseHalfMessageInner()` 做了几件关键改写：

- 把原始 topic 存进 `PROPERTY_REAL_TOPIC`；
- 把原始 queueId 存进 `PROPERTY_REAL_QUEUE_ID`；
- 保留 Producer 事先打上的 `PROPERTY_TRANSACTION_PREPARED` 等属性；
- 把消息 topic 改成 `RMQ_SYS_TRANS_HALF_TOPIC`；
- 把 queueId 统一改到 half queue 世界里；
- 重置 sysFlag 里的 transaction value，让 half message 当前不再以“正式事务消息已提交态”继续走普通消费主链。

这里必须再收紧一个很容易混淆的边界：Producer 在发送前已经打上了 `PROPERTY_TRANSACTION_PREPARED` 属性，但 `parseHalfMessageInner()` 同时又把 sysFlag 的 transaction value 重置为 `TRANSACTION_NOT_TYPE`。也就是说，half message 存储态不是简单依赖一个“prepared sysFlag”活着，而是通过**专用 half topic + 保留真实 topic/queue 属性 + 普通消费主链隔离**来表达“这是一条待决事务消息”。真正把消息重新构造成带 `TRANSACTION_PREPARED_TYPE` 语义的正式事务消息对象，是后续从 half message 还原真实消息时的另一层动作。

这组动作的意义非常清楚：Broker 不是在“原地保存一条稍后再说的普通消息”，而是在把它改写进一个独立的待决事务轨道。

为什么非得这样隔离？因为如果 half message 还留在真实 Topic/Queue，主链会先在哪坏掉？会坏在普通消费世界：

- ConsumeQueue 仍会在真实业务 Topic 之下为它建索引；
- 普通拉取逻辑可能会把它当成正式消息候选；
- 回查世界、正式消费世界、业务 Topic 世界三者边界会互相污染。

所以 `RMQ_SYS_TRANS_HALF_TOPIC` 的真正价值不是“专用 Topic”本身，而是它把事务待决消息明确隔离出普通消费主链。只有先隔离，后面才谈得上回查、commit 和 rollback 各自怎样收束。

## 第五层：真实 Topic/Queue 并没有丢，而是被保存成“未来可能转正”的身份信息

RocketMQ 把消息改写到 half topic，并不意味着真实业务 Topic/Queue 信息被抛弃了。恰恰相反，它们被小心地保存在属性里：

- `PROPERTY_REAL_TOPIC`
- `PROPERTY_REAL_QUEUE_ID`

这两个属性的意义可以直接压成一句话：**half message 当前不属于真实消费世界，但它未来可能需要被恢复成真实消费世界里的正式消息。**

所以 half message 既是隔离态，又是保留态：

```text
当前：不进入真实 Topic/Queue
未来：若 commit，则还原回真实 Topic/Queue
```

如果没有这层“真实身份备份”，主链会先在哪失败？等到后面 commit 阶段，Broker 根本不知道该把这条待决消息恢复成哪个真实 Topic、哪个真实 Queue，也就无法把事务消息真正转正。

这说明 half message 不是“只管当前看不见”，它同时还为后续二阶段提供了恢复坐标。也正因为这样，本篇必须把它写成一个独立主题，而不能把它压缩成“事务消息会先写个临时 topic”一句话带过。

## 第六层：`prepareMessage()` 真正做的，不是事务成功，而是建立 prepare fact

Broker 侧事务服务 `TransactionalMessageService` 给 prepare 阶段的命名就已经很直白了：

- `prepareMessage(...)`
- `asyncPrepareMessage(...)`

它们不是 commit，也不是 rollback，更不是“发送成功”。它们只是在处理：**把一条待决事务消息先安全写成 prepare/half 事实。**

客户端这边的真实顺序也必须钉死：

```text
Producer 给消息打上 TRAN_MSG / producerGroup 属性
  → 发送 prepare 请求
    → Broker 落 half message
      → 客户端拿到 SEND_OK
        → 执行本地事务
          → endTransaction(commit / rollback / unknow)
```

所以 prepare 成功不仅在语义上早于本地事务决断，而且在时序上就是本地事务执行的前置 RPC 结果。

`TransactionalMessageServiceImpl.prepareMessage()` 直接委托 `TransactionalMessageBridge.putHalfMessage()`；后者再把消息改写后送进 Store。接口注释还明确说明异步 prepare 的 Future 会在 put success、flush 和 replica done 之后完成，这意味着这里建立的 half fact 不是一句空泛的“Broker 记住了”，而是仍然受 Store 写入、刷盘和当前副本确认策略约束的系统事实。于是 prepare 阶段真正成立的是：

```text
半消息已经被 Broker 接纳并落成一条待决系统事实
```

而不是：

```text
业务事务已经成功
或
下游已经可以消费它
```

如果把 `prepareMessage()` 理解成事务成功，主链会先在哪理解错？会混淆 prepare 与 commit：你会误以为只要 half message 落盘，Broker 就已经可以像普通消息一样对外宣布“这条业务事实成立”。而 RocketMQ 的设计显然不是这样。

所以本篇必须把 prepare 的话讲得非常克制：prepare 建立的是 **prepare fact**，不是 **final fact**。

## 第七层：half message 为什么不是延迟消息、重试消息那种“暂时先放着”

表面上看，half message 也像一种“先放着、以后再处理”的消息，于是很容易被误写成：这不就是另一种延迟轨道或重试轨道吗？

但事务消息的 half message 和延迟/重试有一个根本区别：

- 延迟消息是**最终一定准备按普通消息身份重新投递，只是时机还没到**；
- 重试消息是**曾经进入过消费世界，但处理失败，需要再次尝试**；
- half message 则是**当前连“能不能进入正式消费世界”都还没有定论**。

这说明 half message 的暂存不是时间问题，也不是失败重试问题，而是**事实状态仍未决**的问题。

如果把 half message 误写成“事务版延迟消息”，主链会先在哪歪掉？会错过最核心的认知：RocketMQ 先写 half message 的原因不是为了等一个时钟或重试窗口，而是为了在本地事务悬而未决时，先避免正式事实过早暴露，同时留下恢复锚点。

所以 half message 应被理解成“待决事实轨道”，而不是“稍后再投递轨道”。

## 第八层：没有 half message，后面的回查和二阶段就没有落脚点

本篇虽然不展开回查与二阶段，但必须先把它们和 half message 的关系钉死。

为什么后面 Broker 还能回查？为什么还能 commit/rollback？为什么还能删除 prepare 消息？因为 half message 已经先把“这里有一条待决事务消息”这个事实稳稳落进 Broker 世界里了。

也就是说：

- `RocketMQ-20` 里的回查，不是在空中追问，而是在追问一条已经存在的 half message；
- `RocketMQ-21` 里的 commit/rollback，不是在空中生成最终结果，而是在处理一条已经存在的 prepare fact；
- 整条事务恢复主链，都是建立在 half message 已经先存在的前提上。

如果没有本篇这层 half fact，后文主题会先在哪悬空？会悬空在：Broker 根本不知道该对哪条待决消息做回查，也没有一条可被转正或删除的 prepare 消息可供处理。

所以本篇不能只被写成“事务消息的第一步”。它实际上是在为后两篇建立整个事务世界的系统锚点。

## 收网：事务消息先写 half message，本质上是在守“未决事实不能先污染正式世界”

如果把整篇压成一句话，RocketMQ 事务消息之所以不能直接发正式消息，是因为本地事务结果尚未决断时，Broker 既不能让下游先看到一条可能无效的正式事实，也不能什么都不记；它只能先把消息改写成 `RMQ_SYS_TRANS_HALF_TOPIC` 上的一条 half message，让它先成为系统内部可恢复、可回查、但暂不进入真实消费主链的事实。

```text
事务消息发送
  → 真实 Topic/Queue 信息被备份
    → 消息改写进 RMQ_SYS_TRANS_HALF_TOPIC
      → prepare fact 建立
        → 但不进入正式消费主链
          → 后续再决定 commit / rollback / check
```

到这里，主线只发生了五件事。

第一，直接发正式消息会让待决事务过早污染正式消费世界。

第二，等本地事务完全做完再发又会让 Broker 失去故障恢复锚点。

第三，half message 真正建立的是“可恢复但不可正式消费”的系统事实。

第四，`RMQ_SYS_TRANS_HALF_TOPIC` 和 `PROPERTY_REAL_TOPIC` / `PROPERTY_REAL_QUEUE_ID` 把“当前隔离”与“未来可能转正”同时保留下来。

第五，后面的回查和二阶段并不是独立魔法，它们都建立在 half message 已经先存在的前提上。

**本篇的一句话困惑**：事务消息为什么不能直接发正式消息，而要先写 half message？

**本篇的一句话顿悟**：因为在本地事务尚未决断前，RocketMQ 既不能让正式消息先进入真实消费世界，也不能让 Broker 什么事实都没有；所以它只能先把消息落成一条 half message，让待决事务先拥有可恢复、可回查、但暂不可见的系统事实。

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“half message 就是正式消息晚点发。”** half message 首先解决的是待决事实隔离，不是单纯延迟投递。
2. **“prepare 成功就等于事务成功。”** prepare 只代表 half fact 已建立。
3. **“Broker 可以等本地事务结束后再知道这条消息。”** 那样 Producer 故障时会失去恢复锚点。
4. **“直接在原 Topic 打个未提交标记也一样。”** 这会污染普通消费主链和真实 Topic/Queue 的可见性边界。
5. **“本篇已经把事务消息讲完了。”** 本篇只立住 half message，回查和二阶段在后续篇目。

### 关键证据清单

- `client/src/main/java/org/apache/rocketmq/client/producer/TransactionMQProducer.java:82`：事务发送入口 `sendMessageInTransaction()`。
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:1433`：Producer 发送前打上事务 prepared 属性并发送 prepare 请求。
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:1442`：拿到 `SEND_OK` 后才执行 `executeLocalTransaction()`，随后再走 `endTransaction()`。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/TransactionalMessageService.java:28`：prepare/asyncPrepare 的事务服务抽象。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/TransactionalMessageService.java:37`：异步 prepare Future 的完成边界包括 put success、flush 和 replica done。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:99`：异步 prepare 直接写 half message。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:104`：同步 prepare 直接写 half message。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:165`：事务检查服务扫描 `RMQ_SYS_TRANS_HALF_TOPIC`，说明 half message 不进业务消费主链但会进事务检查链。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageBridge.java:211`：`putHalfMessage()` 把消息送入 half-message 存储链。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageBridge.java:219`：保存 `REAL_TOPIC/REAL_QID` 并改写到 half topic，同时重置 transaction value。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageUtil.java:42`：half topic 常量。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageUtil.java:65`：从 half message 还原正式事务消息时重新设置 prepared 属性与 sysFlag。
- `common/src/main/java/org/apache/rocketmq/common/topic/TopicValidator.java:53`：`RMQ_SYS_TRANS_HALF_TOPIC` 属于系统 Topic。
- `common/src/main/java/org/apache/rocketmq/common/topic/TopicValidator.java:62`：`RMQ_SYS_TRANS_HALF_TOPIC` 不允许作为普通发送 Topic 使用。
- `broker/src/test/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImplTest.java:87`：prepareMessage 落 half message 的测试证据。
- `broker/src/test/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageUtilTest.java:47`：事务 prepared 属性与 realTopic/realQueue 的测试证据。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇只讲事务消息的 prepare/half-message 阶段，不展开回查协议与二阶段 commit/rollback 落地。
- 本文把 half message 定位为“系统内部待决事实”，不把它与延迟消息、重试消息或正式消费主链混写。
- 本文不把 DLedger/HA/Controller 的一般可靠性细节重讲一遍，只在必要处把事务 half fact 与恢复主线连接起来。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-4` 的 CommitLog 真相层、`RocketMQ-5/6` 的可见性与消费世界、`RocketMQ-18` 的故障恢复真相边界。
- 后续桥接：下一篇 `RocketMQ-20` 继续回答“Producer 本地事务执行完了，Broker 为什么还要回查”；再下一篇 `RocketMQ-21` 回答二阶段 commit/rollback 怎样把 half fact 真正转成最终事实。