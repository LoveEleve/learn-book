# RocketMQ-20. Producer 本地事务执行完了，Broker 为什么还要回查

> 场景：上一篇已经把事务消息为什么必须先写 half message 讲清了。到这里，读者通常会紧接着冒出一个直觉问题：既然 Producer 本地事务已经执行过，而且客户端后面也会给 Broker 发 `endTransaction()`，为什么 RocketMQ 还要额外维护一整套回查链？
>
> 本篇只回答一个问题：**Producer 本地事务执行完了，Broker 为什么还要回查。** 本篇聚焦事务检查链：half queue 扫描、`CHECK_TRANSACTION_STATE` 请求、Producer 的 `checkLocalTransaction()` 回答以及 `UNKNOW`/超时/丢失边界；不展开最后 commit/rollback 怎样真正转正或删除 half message。

## 先把真正的困惑摆出来：本地事务都执行了，为什么 Broker 还不放心

从应用层视角看，事务消息的理想世界很简单：

1. Producer 先把 half message 发给 Broker；
2. 本地事务执行；
3. 客户端把 commit 或 rollback 告诉 Broker；
4. 事情结束。

如果这条链永远不丢包、不断线、不崩溃、不超时，那的确不需要回查。

问题在于，RocketMQ 从来不假设这个理想世界永远成立。

在“本地事务执行完成”和“Broker 最终可靠看到这次结论”之间，还可以插入很多故障：

- Producer 本地事务已经执行完，但进程在 `endTransaction()` 前崩溃；
- `endTransaction()` 已经发出，但网络丢失或 Broker 没收到；
- Producer 返回了 `UNKNOW`，说明当前仍然无法给出确定结论；
- Producer group 对应的连接暂时不可用，Broker 没法立刻确认；
- 客户端和 Broker 对同一条待决消息的认知已经不同步。

这就意味着：**“本地事务曾经执行过”并不等于“Broker 已经拿到了一个最终、可靠、可持久依赖的事务结论”。**

如果 Broker 只信一次客户端回执，主链会先在哪坏掉？会坏在“half message 已经存在，但 Broker 永远失去了确认它最终命运的机会”这里。到那时，系统既不敢随便 commit，也不敢随便 rollback，只能把待决事实永久悬挂。

这正是 RocketMQ 仍然要回查的根本原因：它不是不相信本地事务执行本身，而是不相信**单次回执链路一定可靠收束**。

```text
Producer 执行本地事务
  → 理想情况：endTransaction() 顺利到达 Broker
  → 现实情况：可能崩溃 / 丢包 / 超时 / 返回 UNKNOW
    → Broker 不能凭空猜最终结论
      → 必须围绕 half message 主动回查 Producer
```

*关键设计（斜体）：* *RocketMQ 回查不是在怀疑本地事务逻辑本身，而是在补“Broker 最终有没有拿到这次事务结论”这条不可靠链路。*[模式: 单次回执不可信，Broker 主动复核]

## 第一层：half message 解决的是“先有待决事实”，回查解决的是“待决事实最终怎样收敛”

上一篇已经立住了 half message：它让事务在尚未决断时，先拥有一份可恢复、可检查、但不进入业务正式消费主链的系统事实。

走到这里，最该先分清的就是：

- half message 解决的是：**事务未决时，系统有没有事实落点**；
- 回查解决的是：**这条待决事实后来到底该 commit、rollback，还是继续 unknown。**

如果没有 half message，Broker 连回查什么都不知道；如果有 half message 但没有回查，Broker 又可能永远不知道它最后该往哪个方向收束。

所以事务消息在 RocketMQ 里天然分成两层：

```text
第 1 层：half fact 已存在
第 2 层：half fact 最终怎样收敛
```

本篇要回答的，就是第二层为什么不能省略。

如果把 half message 写完就当事务链路完成，主链会先在哪失败？会失败在待决状态：Broker 知道这里有一条事务消息，但不知道它究竟应该进入正式世界还是彻底回滚，系统事实就会停在半空中。

## 第二层：只信一次 `endTransaction()` 回执，会把故障全留给 Broker 自己兜底

为什么单次回执不够？因为 `endTransaction()` 只是一次 RPC，而不是一条天然可靠的终局真相。

客户端真实时序是：

```text
prepare half message
  → Broker 返回 SEND_OK
    → Producer 执行本地事务
      → Producer 调用 endTransaction(commit / rollback / unknow)
```

这条链里最脆弱的一段恰恰在后半程。事务已经在 Producer 本地执行过，但 Broker 是否最终收到了一个确定结论，并不是自动成立的。

只靠这一次回执，主链会先在哪坏掉？至少有三种典型场景：

### 1. 本地事务做完后 Producer 崩溃

Producer 自己也许已经知道“该 commit 还是 rollback”，但它在把结果发回 Broker 前就挂了。Broker 侧只剩下一条 half message，看不见结论。

### 2. 回执发出了，但 Broker 没可靠收到

网络闪断、连接超时、Broker 短暂抖动，都可能让 `endTransaction()` 结果丢在半路。Producer 以为自己已经做完了，Broker 却仍然停留在待决状态。

### 3. 客户端自己返回 `UNKNOW`

RocketMQ 的事务接口明确允许 `LocalTransactionState.UNKNOW`。这说明系统从一开始就承认：本地事务的当前结果并不总能在第一次回答里被明确给出。

这三种情况有一个共同点：Broker 不能凭空猜。因为：

- 猜 commit，可能把本该回滚的事实错误转正；
- 猜 rollback，可能把本该成立的事实直接抹掉；
- 永远不处理，又会把 half message 无限悬挂。

所以 RocketMQ 的选择不是“更勇敢地猜一次”，而是：**当首次回执链路不可靠或不确定时，Broker 再主动问一遍 Producer。**

## 第三层：回查不是重新做本地事务，而是再次确认本地事务状态

很多人第一次听到事务回查，会产生一个危险误解：Broker 过一会儿再来问，是不是就等于让 Producer 再执行一次本地事务？

不是。

Producer 侧回查真正落到的是 `checkLocalTransaction()`，它的职责不是重做业务，而是**回答当前这条事务消息在本地系统里此刻能够确认出的状态是什么**。这里必须把话说严：它返回的未必总是最终结论，因为客户端完全可以回答 `UNKNOW`，表示“我现在仍然不能可靠判断”。

客户端收到 `CHECK_TRANSACTION_STATE` 请求后，会：

- 根据消息里的 producer group 找到对应的 Producer；
- 调用 `checkLocalTransactionState()` 或新的 `checkLocalTransaction()`；
- 把结果映射成 `COMMIT_MESSAGE` / `ROLLBACK_MESSAGE` / `UNKNOW`；
- 即便结果是 `UNKNOW`，也不是“什么都不回”，而是仍然回一条 `EndTransactionRequestHeader` 给 Broker，只是 `commitOrRollback=TRANSACTION_NOT_TYPE`，并标记 `fromTransactionCheck=true`。

也就是说，这条链的本质不是“重做业务动作”，而是“补做一次状态确认”。

如果把回查理解成“重试本地事务”，主链会先在哪理解错？会误以为 RocketMQ 把幂等和业务副作用问题重新压回给了应用。但源码真正做的不是这个，而是把“状态确认”单独抽出来，让 Producer 以一种可重复回答、但不必重做业务的方式，再次告诉 Broker结论。

所以回查应该被理解成：

```text
half fact 已存在
  → Broker 再次索要状态证明
    → Producer 回答当前最终状态
```

而不是：

```text
Broker 强迫 Producer 重做一次事务
```

## 第四层：Broker 为什么必须主动扫 half queue，而不是被动等消息自己收敛

既然回查是为了补单次回执链路的不可靠，那下一步自然就是：Broker 怎么知道该查谁？

答案就是 Broker 自己周期性扫描 half queue。

`TransactionalMessageCheckService` 是一个后台线程。它不会等某条消息自己“跳出来”，而是按 `transactionCheckInterval` 周期醒来，调用：

```text
transactionalMessageService.check(timeout, checkMax, listener)
```

而 `TransactionalMessageServiceImpl.check(...)` 并不是“扫到 half message 就直接问一次 Producer”，它中间还有一整层过滤与节流链：

- 遍历 `RMQ_SYS_TRANS_HALF_TOPIC` 的 half message queue；
- 对照 op queue 与 removeMap，跳过已经被 commit/rollback 处理过的项；
- 先经过 `needDiscard` / `needSkip`，把检查次数超限或文件保留期外的消息筛掉；
- 再结合 `PROPERTY_CHECK_IMMUNITY_TIME_IN_SECONDS`、当前 bornTime 与 op queue 状态判断现在是否值得打扰 Producer；
- 只有 `isNeedCheck` 成立时，才会真正 `putBackHalfMsgQueue(msgExt, i)` 并交给 `listener.resolveHalfMsg(msgExt)`。

也就是说，Broker 主动扫描并不等于 Broker 粗暴轮询。RocketMQ 在真正发起回查前，已经先做了一轮“这条消息现在该不该查、还能不能查、值不值得继续查”的过滤。

为什么必须 Broker 主动扫，而不是“等客户端最终总会回一条”？因为 half message 已经在 Broker 手里，Broker 才是那个知道“现在还有哪些待决事实悬着没收口”的一方。Producer 本地也许已经执行完，也许已经重启，也许当前根本没再主动发送任何请求。只有 Broker 主动扫 half queue，才有机会把这些悬挂事实一条条重新拉回收敛链。

如果 Broker 不主动扫，主链会先在哪坏掉？会坏在**静默悬挂**：既没有新的客户端请求触发，也没有外部事件提醒，half message 就可能永远停在待决状态，而系统却再也没有一次主动确认它命运的机会。

所以回查线程不是附属工具，而是事务世界的“主动收口器”。

## 第五层：回查问的不是“有没有这条 half message”，而是“你本地事务最终到底是什么状态”

`AbstractTransactionalMessageCheckListener.sendCheckMessage()` 很值得单独讲，因为它揭示了 RocketMQ 回查问题的真正对象。

这段逻辑会：

- 构造 `CheckTransactionStateRequestHeader`；
- 从 half message 中取出 `commitLogOffset`、`tranStateTableOffset`、`transactionId`；
- 把消息 topic 改回 `REAL_TOPIC`，queueId 改回 `REAL_QUEUE_ID`；
- 根据 `PROPERTY_PRODUCER_GROUP` 找到对应 producer channel；
- 调用 `broker2Client.checkProducerTransactionState(...)` 发起检查。

这个动作非常关键，因为它说明：Broker 并不是在问“half message 在不在”，也不是在问“现在能不能随便提交”。它在问的是：**对于这条原始业务消息，你本地事务最终状态到底是什么。**

之所以要把 half message 改回真实 Topic/Queue 语义再发给 Producer，就是为了让 Producer 能从自己的业务上下文里识别这条事务消息，给出本地事务状态回答。

如果不做这层“从 half 语义还原回真实业务语义”，主链会先在哪失真？Producer 看到的将只是一个系统 half topic 上的内部消息，很难用业务语境回答这条原始事务的真实状态。

所以回查的本质是：

```text
Broker 拿 half fact 做锚点
  → 还原出对应的原始业务语义
    → 向 Producer 索要最终状态回答
```

而不是直接围着 half topic 自己兜圈子。

## 第六层：`UNKNOW`、checkTimes、immunityTime 说明 RocketMQ 承认“结果不会第一次就明朗”

如果 RocketMQ 认为事务状态第一次一定能拿到明确答案，那它根本不需要这些额外边界：

- `LocalTransactionState.UNKNOW`
- `PROPERTY_TRANSACTION_CHECK_TIMES`
- `PROPERTY_CHECK_IMMUNITY_TIME_IN_SECONDS`
- `transactionCheckMax`
- `transactionTimeout`

这些设计共同说明一件事：**RocketMQ 从一开始就承认，事务状态的收敛可能是渐进的，而不是一次完成的。**

### `UNKNOW`

它表示当前 Producer 仍然不能给出最终 commit/rollback 结论。Broker 不能把它解释成失败，也不能把它解释成成功，只能继续把 half fact 留在待决世界里，等待下一轮检查或后续结果。还要特别注意：`UNKNOW` 不是“这次就不回任何结果”，而是客户端会显式回一条事务检查响应，只不过里面的 `commitOrRollback` 被设置成 `TRANSACTION_NOT_TYPE`，告诉 Broker“我现在仍然无法裁决”。换句话说，`UNKNOW` 不是静默，而是一条显式的 unknown 回执。

### `PROPERTY_CHECK_IMMUNITY_TIME_IN_SECONDS`

它表示：这条事务消息在一定时间内不应该过早被 Broker 打扰。因为本地事务可能还在执行，或者业务自己希望有一段免检查窗口。

### `PROPERTY_TRANSACTION_CHECK_TIMES` / `transactionCheckMax`

它们表示：RocketMQ 也不能无限期地问下去。某条消息如果反复回查仍然得不到明确结论，最终需要进入 discard/兜底路径，而不是永远挂起。

如果没有这些边界，回查链会先在哪坏掉？

- 没有 immunity time：Broker 可能过早打扰尚未执行完的本地事务；
- 没有 check times：Broker 可能无限回查一条永远 unknown 的消息；
- 没有 `UNKNOW`：系统只能强迫客户端在尚未准备好时瞎给一个 commit/rollback。

所以这些字段并不是“参数细节”，而是事务回查链承认现实复杂性的设计证据。

## 第七层：没有可用 Producer channel，Broker 也不能自己拍板

`sendCheckMessage()` 还有一个很容易被忽略的失败边界：Broker 需要根据 producer group 找到可用 channel。

如果 channel 不存在，它只会记日志：

```text
Check transaction failed, channel is null
```

这个细节特别重要，因为它再次说明：Broker 在事务世界里并没有“没有回答也能自己拍板”的特权。哪怕它此刻已经非常想知道结果，它仍然只能承认：当前没有可用回查通道，这条待决事实还不能被可靠收束。

如果在 channel 不可用时 Broker 自己猜，主链会先在哪坏掉？会再次回到最危险的问题：系统会在没有本地事务证据的前提下，擅自把 half fact 转成 final fact。

所以 RocketMQ 宁可暂时留待后续重试或兜底处理，也不让 Broker 在没有证据时越权决定。

## 第八层：回查不是事务世界的终点，而是为了把 half fact 推到二阶段决定点

本篇到这里必须收住一个边界：回查回答的是“现在能不能拿到更可靠的本地事务状态”，而不是“最终消息怎样真正落稳”。

也就是说：

- half message 提供待决事实；
- 回查提供状态再次确认；
- 二阶段 commit/rollback 才负责最终把这条事实转正、丢弃或继续处理。

这也是为什么客户端回查回答后，`DefaultMQProducerImpl.checkTransactionState()` 会再发一条 `EndTransactionRequestHeader` 回到 Broker，但真正如何 commit/rollback 这条 prepare fact，要留给下一篇展开。

如果本篇把回查和二阶段混写，主链会先在哪失焦？会把最关键的问题——“为什么单次回执不够、为什么 Broker 还要主动问一次”——重新淹没在大量 commit/rollback 存储细节里。

所以本篇最正确的收束方式，不是把事务消息全讲完，而是明确：**回查的价值在于把待决 half fact 从‘不知道’推进到‘有资格进入最终裁决’。**

## 收网：Broker 之所以还要回查，是因为它不能把事务正确性押在一次性回执上

如果把整篇压成一句话，RocketMQ 之所以在 Producer 本地事务执行完以后还要回查，不是因为它不相信业务代码会执行，而是因为它不能把事务正确性押在一次 `endTransaction()` 回执上；只要这条回执链路可能被崩溃、超时、丢包或 `UNKNOW` 打断，Broker 就必须围绕 half message 保留主动检查链，再次向 Producer 索要本地事务最终状态。

```text
half message 已存在
  → Broker 周期扫描 half queue
    → 发现仍未明确收敛的待决事实
      → 构造 CHECK_TRANSACTION_STATE
        → Producer 执行 checkLocalTransaction()
          → 返回 COMMIT / ROLLBACK / UNKNOW
            → Broker 才有资格进入下一步 endTransaction 处理
```

到这里，主线只发生了五件事。

第一，half message 只解决“待决事实先存在”，没有解决“最终结论一定送达 Broker”。

第二，单次 `endTransaction()` 回执链路并不可靠，所以 Broker 不能永远被动等它。

第三，Broker 通过扫描 half queue 主动把悬挂事务重新拉回收敛链。

第四，Producer 侧回查回答的是本地事务状态，不是重做业务事务。

第五，回查的终点不是事务结束，而是为下一步 commit/rollback 真正落稳创造条件。

**本篇的一句话困惑**：Producer 本地事务都执行完了，Broker 为什么还要回查？

**本篇的一句话顿悟**：因为本地事务执行过，不等于 Broker 已可靠拿到最终结论；只要一次性回执链路仍可能丢失、超时或返回 `UNKNOW`，Broker 就必须围绕 half message 再次主动确认，才能让待决事实最终收敛。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“回查是在怀疑本地事务有没有执行。”** 回查怀疑的是 Broker 是否可靠拿到了最终结论。
2. **“`UNKNOW` 就等于失败。”** 它表示当前还不能下最终结论。
3. **“回查等于重做一次本地事务。”** Producer 侧做的是状态确认，不是重复业务动作。
4. **“Broker 可以在没有回查结果时自己猜 commit/rollback。”** RocketMQ 明确避免这种越权判断。
5. **“本篇已经把事务消息全部讲完了。”** 本篇只讲回查必要性和检查链，二阶段落稳留给下一篇。

### 关键证据清单

- `broker/src/main/java/org/apache/rocketmq/broker/transaction/TransactionalMessageCheckService.java:43`：Broker 周期性触发事务检查线程。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/TransactionalMessageService.java:68`：事务服务的 `check(...)` 契约，明确遍历未提交/未回滚 half message 做检查。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:162`：Broker 扫描 half queue 并决定是否进入检查链。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:108`：`needDiscard` 通过检查次数控制是否放弃继续回查。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:123`：`needSkip` 通过文件保留期与时间边界决定是否直接跳过。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/AbstractTransactionalMessageCheckListener.java:51`：构造 `CHECK_TRANSACTION_STATE` 请求并还原真实 Topic/Queue 语义。
- `client/src/main/java/org/apache/rocketmq/client/impl/ClientRemotingProcessor.java:100`：Producer 客户端接收 `CHECK_TRANSACTION_STATE` 请求。
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:360`：Producer 侧执行 `checkLocalTransaction()` 并回送 `EndTransactionRequestHeader`。
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:425`：`UNKNOW` 也会显式回送 `TRANSACTION_NOT_TYPE`。
- `broker/src/test/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImplTest.java:131`：事务检查服务进入 `resolveHalfMsg` 的测试证据。
- `broker/src/test/java/org/apache/rocketmq/broker/transaction/queue/DefaultTransactionalMessageCheckListenerTest.java:70`：check listener 发送检查消息的测试证据。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇聚焦事务状态回查链，不展开最终 commit/rollback 怎样把 half message 转正或删除。
- 本文把 `UNKNOW`、免检时间、检查次数都看作回查收敛边界的一部分，而不是附属配置。
- 本文不把“回查成功得到状态”和“Broker 最终完成二阶段落稳”混成同一步。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-19` 的 half fact、`RocketMQ-18` 的故障恢复真相边界。
- 后续桥接：下一篇 `RocketMQ-21` 继续回答 Broker 二阶段 commit/rollback 怎样把 half fact 真正落成最终系统事实。