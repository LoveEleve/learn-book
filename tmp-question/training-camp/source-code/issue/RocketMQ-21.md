# RocketMQ-21. Broker 二阶段 commit/rollback 怎样把事务消息真正落稳

> 场景：前两篇已经把事务消息的前两层立住了：先写 half message，让待决事务先有一份系统事实；再通过回查链，在单次回执不可靠时重新向 Producer 索要本地事务状态。走到这里，读者最自然的问题就是：Broker 现在既然已经拿到了 commit 或 rollback 结论，为什么还要再做一大段处理？
>
> 本篇只回答一个问题：**Broker 二阶段 commit/rollback 怎样把事务消息真正落稳。** 本篇聚焦 `EndTransactionProcessor`、prepare message 校验、正式消息重写与 half message 删除；不回头重讲为什么要 half message，也不重讲为什么要回查。

## 先把真正的困惑摆出来：拿到 commit/rollback 结论以后，为什么还没结束

从外部看，事务消息的二阶段似乎应该非常简单：Producer 告诉 Broker 这条消息应该 commit 还是 rollback，Broker 照办即可。

如果只是看这句话，好像 Broker 只需要做两件事里的一个：

- commit：把这条消息标成已提交；
- rollback：把这条消息标成已回滚。

但 RocketMQ 的源码并没有这么做。

因为到二阶段这里，Broker 真正面对的问题不是“改一个状态位”，而是：**如何把一条 half fact 收束成最终系统事实。**

这件事至少有三层复杂度：

- 这条请求真的是对应该 half message 吗？
- commit 以后，普通消费世界需要看到的是哪条最终消息？
- rollback 以后，系统怎样明确知道这条待决事实已经真正结束，而不是还悬在 half world 里？

如果 Broker 只在原 half message 上改标签，主链会先在哪坏掉？会坏在普通消费世界：真实 Topic/Queue、普通 ConsumeQueue、普通消费主链，仍然需要的是一条**正式消息事实**，而不是一条继续躺在 `RMQ_SYS_TRANS_HALF_TOPIC` 里的 half message。

所以二阶段真正要回答的，不是“状态改成什么”，而是：

```text
这条待决事实
  → 最终是要转成真实消费世界里的正式事实
  还是
  → 要在系统里被明确清理掉
```

也就是说，二阶段并不是事务消息的附属尾声，而是它真正收束成最终世界状态的那一步。

```text
half message 已存在
  → Broker 收到 commit / rollback 结论
    → 先校验 prepare message 身份与边界
      → commit: 重建真实消息并重新写入正式主链
      → rollback: 不写真实消息，但写入 remove/op 语义并推动 half fact 退出待决世界
        → 待决事实被收束成“正式存在”或“正式消失”
```

*关键设计（斜体）：* *事务消息真正落稳，不是在 half message 存在时，而是在二阶段把它明确收束成“最终可见的正式事实”或“最终被删除的回滚事实”。*[模式: 待决事实 → 最终事实/最终消失]

## 第一层：Broker 先做的不是 commit/rollback，而是校验“这条请求配不配操作这条 half message”

`EndTransactionProcessor.processRequest()` 并不是一上来就把 half message 转正或删除。它先做了一组很严格的入口校验。

### 1. slave 直接拒绝

如果当前 Broker 仍处于 `BrokerRole.SLAVE`，会直接返回 `SLAVE_NOT_AVAILABLE`。这已经说明：二阶段裁决必须由有资格处理事务主链的 Broker 承担，不能由从节点随便接管。

### 2. 区分普通客户端回执与回查回执

`fromTransactionCheck=true/false` 会影响对 `TRANSACTION_NOT_TYPE` 的处理方式。RocketMQ 明确知道：

- 普通发送回来的 pending status 是一回事；
- 回查链上的 pending/unknown 又是另一回事。

### 3. 先从 half world 找回 prepare message

无论 commit 还是 rollback，Broker 都会先通过 `TransactionalMessageService.commitMessage()` / `rollbackMessage()` 拿到 `OperationResult`。也就是说，真正的对象不是请求头本身，而是请求头对应的 prepare message。

### 4. 再用 `checkPrepareMessage()` 校验身份

`checkPrepareMessage()` 至少会核对：

- producerGroup 是否匹配；
- `tranStateTableOffset` 是否匹配；
- `commitLogOffset` 是否匹配。

如果这些对不上，Broker 就不接受这次 commit/rollback。

这一步非常重要。因为事务消息到了二阶段，Broker 不能只因为“客户端说要 commit”就放行。它必须确认：**这条结论真的是对这条 half message 发出的。**

如果省掉这层校验，主链会先在哪坏掉？会坏在身份越权：错误的 producerGroup、错误的 queueOffset、错误的 commitLogOffset 都可能把不属于自己的待决事务错误转正或回滚。

所以二阶段的第一原则不是“快点收尾”，而是“先确认这次裁决确实打在正确的 prepare fact 上”。

## 第二层：commit 不是“把 half message 改成已提交”，而是重新构造正式消息

这是整篇最重要的结构点。

源码里的 commit 路径并没有在 half message 上改一个提交状态，然后就让 Consumer 去读它。相反，commit 的核心链是：

```text
prepare message
  → endMessageTransaction()
    → 恢复真实 Topic / Queue / body / properties
      → sendFinalMessage()
        → 重新写入普通消息主链
```

`endMessageTransaction()` 会把 half message 重新恢复成一条正式消息对象：

- topic 改回 `PROPERTY_REAL_TOPIC`；
- queueId 改回 `PROPERTY_REAL_QUEUE_ID`；
- body、bornHost、storeHost、reconsumeTimes 等字段继承回来；
- 清掉 `PROPERTY_REAL_TOPIC` / `PROPERTY_REAL_QUEUE_ID`；
- 清理 `PROPERTY_TRANSACTION_PREPARED`；
- 再根据二阶段结果把 sysFlag 的事务值改成 commit type。

这说明 commit 的真正含义不是“half message 继续活着，只是状态不同”，而是：**half message 提供恢复原始消息的素材，Broker 再把这条消息重新投回正式世界。**

为什么必须重写？因为普通消费主链最终承认的，必须是一条位于真实 Topic/Queue 下、符合正常消息语义的正式消息。half topic 本身只是待决世界，不是事务最终对外可见的世界。

如果 commit 只是改 half message 标签，主链会先在哪坏掉？

- 普通 Topic/Queue 世界里不会长出那条最终正式消息；
- Consumer 仍看不到它该看的真实业务消息；
- half topic 和正式消费主链的边界会被直接打穿。

所以 commit 真正做的是：**用 half fact 还原出 final fact，再把 final fact 重新写进正式主链。**

## 第三层：rollback 不是“什么都不做”，而是让待决事实明确消失

commit 要重写正式消息，rollback 看起来似乎简单很多：既然不用投递真实消息，那是不是直接返回成功就行？

也不行。

因为对事务消息来说，rollback 的关键不是“没有新消息出现”，而是：**系统必须明确知道这条待决事实已经结束，不该再继续留在 half world 里。**

所以 rollback 的主链是：

```text
拿到 prepare message
  → 校验它与请求确实对应
    → 不重写正式消息
      → deletePrepareMessage()
        → 写入 op/remove 语义
          → half fact 在后续 half/op 对照链里被判定为已结束
```

这里必须把“删除”两个字说严。`deletePrepareMessage()` 在实现上并不是立刻把 half message 物理擦掉，而是优先把 queueOffset 写进 op/remove 语义，由后续 half queue 与 op queue 对照时把这条待决消息视为已经被处理过。也就是说，rollback 的收束动作首先是**显式写出‘这条 half fact 已结束’的操作事实**，而不是同步物理抹除日志记录。

还要再补一层边界：这条 remove/op 语义也不是“写进去瞬间就对所有视图立刻生效”。它真正生效，依赖的是后续 half queue 与 op queue 的对照消费位点推进；只有当这套对照链把这条 half message 识别成已处理项后，它才会从待决世界里稳定退出。

如果 rollback 什么都不做，主链会先在哪坏掉？会坏在待决状态残留：

- Producer 明明已经说这条事务该回滚；
- Broker 却仍把 half message 留在待决世界里没有 remove 标记；
- 后续回查线程还可能继续反复扫到它；
- 整个系统无法知道这条待决事务其实已经有了最终结论。

所以 rollback 绝不是“没有投递正式消息就算完了”，它仍然要显式收束 half fact，只不过收束方式不是生成正式事实，而是**写出删除语义，让待决事实在后续对照链里退出系统世界**。

## 第四层：不能先删 half message 再写正式消息，否则 commit 失败会让系统失去两头事实

二阶段里还有一个非常容易被忽视的顺序问题：commit 时为什么要先写正式消息，成功以后再删除 prepare message？

源码的顺序很明确：

1. `endMessageTransaction()` 构造正式消息；
2. `sendFinalMessage()` 调用 `messageStore.putMessage(msgInner)`；
3. 只有当 `sendFinalMessage()` 返回 `ResponseCode.SUCCESS` 后，才 `deletePrepareMessage(result.getPrepareMessage())`。

这里还要把“成功”说严：`sendFinalMessage()` 不只在 `PUT_OK` 时返回 `ResponseCode.SUCCESS`，对 `FLUSH_DISK_TIMEOUT`、`FLUSH_SLAVE_TIMEOUT`、`SLAVE_NOT_AVAILABLE` 也会按成功语义返回。也就是说，Broker 在事务二阶段里把“最终正式消息已经可以被承认”定义得比单一 `PUT_OK` 稍宽，它沿用了普通消息发送结果里那条“成功但伴随某些边界告警”的语义。

这个顺序的意义巨大。

如果反过来，先删 half message 再尝试写正式消息，会发生什么？只要正式消息写入失败，系统就会同时失去：

- half world 里的待决事实；
- 正式世界里的最终事实。

也就是说，事务消息会从“待决”直接掉进“既没 final fact，也没 prepare fact”的黑洞。

RocketMQ 显然不允许这种情况发生。所以 commit 的顺序不是实现细节，而是事务系统最关键的保护之一：

```text
先建立最终事实
  → 再删除待决事实
```

这和数据库里“先确保新状态成立，再删旧锚点”的思路是一致的。

所以本篇必须把这个顺序钉死：**half message 是 Broker 在二阶段失败时仍可依赖的最后锚点，不能过早删除。**

## 第五层：`rejectCommitOrRollback()` 说明 Broker 不只看最终结论，还看“这结论来得是不是还合法” 

Broker 的二阶段处理并不只是“客户端说什么，我就照做什么”。`rejectCommitOrRollback()` 专门处理一种时序边界：

- 如果不是来自事务回查；
- 并且消息配置了自定义 `CHECK_IMMUNITY_TIME_IN_SECONDS`；
- 而当前 `currentTimeMillis - bornTime` 已经超过这个允许窗口；
- 那么 commit/rollback 会被拒绝，并返回 `ILLEGAL_OPERATION`。

这说明 Broker 不仅关心“结论是什么”，还关心：**你现在给出的这份结论，是否还处在被系统允许直接采纳的窗口里。**

为什么要这样做？因为 RocketMQ 希望把“客户端主动上报阶段”和“之后由回查链接管的阶段”分出边界。超过这个窗口后，系统更倾向于让这条待决事实重新回到检查链，而不是继续无条件接受一次迟到的普通回执。

如果没有这层时序拒绝，主链会先在哪变混乱？会坏在客户端和 Broker 的控制权边界：一条已经超出首轮免检/上报窗口的待决事务，仍可能被一份迟到的普通回执直接改写，和回查链本身产生交叉覆盖。

所以 `rejectCommitOrRollback()` 守的不是消息内容，而是**谁在什么时间窗口里还有资格主导这条待决事实的最终裁决**。

## 第六层：`fromTransactionCheck=true` 不是普通客户端回执，它代表“这是回查之后的二阶段裁决”

上一层继续引出一个重要边界：Broker 对 `fromTransactionCheck=true` 和普通客户端主动回执并不是完全同一视角。

回查触发的结果来自：

```text
Broker 主动问
  → Producer 回答当前状态
    → Producer 再回一条 EndTransactionRequestHeader
      → fromTransactionCheck=true
```

这条链的本质是：Broker 已经怀疑单次回执不够，主动介入确认过一次本地事务状态，接下来收到的是**检查链上的状态回答**。但这里必须再补一条关键边界：如果回查结果仍然是 `TRANSACTION_NOT_TYPE`，`EndTransactionProcessor` 会直接 `return null`，也就是这次检查链回答不会进入后续 commit/rollback 收束，而是显式维持待决状态。

因此在 `EndTransactionProcessor` 里，来自检查链的 pending/commit/rollback 日志语义会和普通客户端上报路径区分开看。它们不是完全两个世界，但确实代表两种不同的上下文：

- 普通客户端路径：这是发送后的首次二阶段上报；
- 回查路径：这是 Broker 主动介入后的再次状态确认。

如果不区分这两种上下文，主链会先在哪失真？会把“客户端主动及时提交”和“Broker 在不确定后主动问回来的结论”混成一条路径，读者就看不见 RocketMQ 为什么要同时保留普通二阶段路径和回查二阶段路径。

所以 `fromTransactionCheck=true` 不是字段装饰，而是事务世界里“这份结论是主动上报来的，还是 Broker 复核后拿到的”这一层语境标记。并且它还决定了另一件事：检查链上的 `TRANSACTION_NOT_TYPE` 会被短路掉，继续维持待决，而不是被当成普通二阶段分支继续向下执行。

## 第七层：二阶段 commit/rollback 最终要收束的是“待决事实的命运”

走到这里，就可以把 commit 和 rollback 放在同一张抽象图里看了。

### commit

```text
half fact
  → 身份校验通过
    → 还原真实消息
      → 正式消息写入成功
        → 删除 half fact
          → 最终结果：正式事实存在
```

### rollback

```text
half fact
  → 身份校验通过
    → 不写正式消息
      → 删除 half fact
        → 最终结果：正式事实不存在，待决事实也消失
```

这两条链虽然动作不同，但目标一致：**让 half fact 结束“待决”状态。**

所以本篇真正的中心句应该是：事务消息的二阶段不在于“客户端回了什么枚举值”，而在于 Broker 最终怎样把这条待决事实收束成清晰的系统状态：

- 该存在，就以正式消息身份存在；
- 该消失，就连待决痕迹也一起消失。

这才叫“落稳”。

## 第八层：如果 commit/rollback 没有真正落稳，回查链和恢复链都会被污染

为什么本篇必须单独成篇？因为二阶段落稳不是事务消息世界的一个小尾巴，它会直接影响前面所有主链：

- 如果 commit 没真正生成正式消息，Consumer 世界就永远缺了一条本该成立的事实；
- 如果 rollback 没删掉 half message，回查线程会继续把它当成未决事实反复扫描；
- 如果 prepare message 身份校验做错，错误的事务结论会污染别的消息；
- 如果正式消息写入失败后仍删除了 half message，故障恢复时连最后锚点都找不到。

所以二阶段其实是事务消息世界里最像“落地结算”的那一步。没有它，前面的 half message 与回查都只能停留在“系统知道这里有问题”，但无法把问题真正关闭。

## 收网：事务消息真正落稳，不在回查本身，而在 Broker 把待决事实收束成最终事实

如果把整篇压成一句话，RocketMQ 事务消息的二阶段 commit/rollback，真正做的不是改 half message 状态，而是先确认这次裁决确实针对正确的 prepare message，然后：

- commit 时，恢复真实 Topic/Queue，重新写入正式消息，成功后删除 half fact；
- rollback 时，不生成正式消息，但同样删除 half fact；

从而让这条事务消息最终离开“待决世界”，成为“正式存在”或“正式消失”的系统事实。

```text
endTransaction(commit / rollback)
  → 找回并校验 prepare message
    → commit: 重写正式消息 → 成功后删 half
    → rollback: 不写正式消息 → 直接删 half
      → half fact 最终被收束成 final fact 或 final deletion
```

到这里，主线只发生了五件事。

第一，二阶段处理的对象不是客户端枚举值本身，而是与之对应的 prepare fact。

第二，commit 不是改标签，而是重写正式消息。

第三，rollback 不是忽略不管，而是删除待决事实。

第四，先写正式事实、后删 half fact 的顺序，是 Broker 防止双失真的关键保护；并且这里的“成功”不只包括 `PUT_OK`，还包括 `FLUSH_DISK_TIMEOUT` / `FLUSH_SLAVE_TIMEOUT` / `SLAVE_NOT_AVAILABLE` 这些仍被 `sendFinalMessage()` 映射成 `ResponseCode.SUCCESS` 的结果。

第五，事务消息真正落稳，意味着待决事实不再悬空，而是被明确收束进最终世界状态。

**本篇的一句话困惑**：Broker 拿到 commit/rollback 结论以后，为什么还不能一句话结束？

**本篇的一句话顿悟**：因为事务消息真正需要落稳的不是“客户端说了什么”，而是 Broker 怎样把 half fact 收束成最终系统事实；commit 要重写正式消息，rollback 要清理待决事实，只有这一步完成，事务世界才算真正闭环。

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“commit 就是在 half message 上改个状态。”** 实际上要重写正式消息。
2. **“rollback 就是什么都不做。”** rollback 也要显式写出 remove/op 删除语义，让 prepare message 退出待决世界。
3. **“客户端说 commit/rollback，Broker 就直接照做。”** Broker 还要校验 producerGroup、queueOffset、commitLogOffset 和时序边界。
4. **“可以先删 half message，再慢慢写正式消息。”** 这样正式消息写失败时会同时丢失待决事实和最终事实。
5. **“本篇已经在讲事务回查。”** 本篇只讲二阶段收束，不重讲回查必要性。

### 关键证据清单

- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:57`：二阶段总入口与 slave 拒绝。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:129`：commit/rollback 主干分支。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:199`：`rejectCommitOrRollback()` 守住时序合法性。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:220`：`checkPrepareMessage()` 校验 prepare message 身份。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:250`：`endMessageTransaction()` 还原真实消息。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:275`：`sendFinalMessage()` 把正式消息重新写入普通主链。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:597`：`deletePrepareMessage()` 通过写 op/remove 语义推进待决事实退出，而不是同步物理擦除。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:634`：commitMessage 找回待决消息。
- `broker/src/main/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImpl.java:639`：rollbackMessage 找回待决消息。
- `broker/src/test/java/org/apache/rocketmq/broker/processor/EndTransactionProcessorTest.java:97`：commit、rollback、reject 等二阶段处理测试证据；当前更偏响应码与入口分支验证，而不是对“先写正式消息后删 half”顺序的直接断言。
- `broker/src/test/java/org/apache/rocketmq/broker/transaction/queue/TransactionalMessageServiceImplTest.java:156`：`deletePrepareMessage()` 的 remove/op 写入测试证据。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇聚焦 Broker 二阶段 commit/rollback 收束，不重讲 half message 的建立与回查必要性。
- 本文讨论的是普通事务消息的二阶段落稳，不展开更广义的业务补偿设计。
- 本文把“正式消息写入成功”与“half message 删除成功”都纳入落稳定义，避免只看其中一步。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-19` 的 half fact、`RocketMQ-20` 的回查链、`RocketMQ-4/5` 的正式消息/ConsumeQueue 主链。
- 后续桥接：RocketMQ 事务三篇到这里完成第一轮闭环；后续若做二轮 consistency review，可再统一检查 prepare → check → commit/rollback 三篇之间的术语与失败边界一致性。