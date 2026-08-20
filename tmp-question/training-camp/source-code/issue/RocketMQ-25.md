# RocketMQ-25. Broker 为什么不是“一堆 Processor 堆在一起”——请求码分发与宿主入口主链

> 场景：前面已经讲过 Broker 是宿主、发送怎么进来、拉取为什么会被挂起、事务消息又怎样在 Broker 世界里继续推进。走到这里，很容易产生一个新的直觉问题：Broker 收到这么多种请求，到底是谁决定交给哪个处理器？为什么 RocketMQ 要把 `SendMessageProcessor`、`PullMessageProcessor`、`ClientManageProcessor`、`EndTransactionProcessor`、`AdminBrokerProcessor` 等等拆得这么开？
>
> 本篇只回答一个问题：**Broker 为什么不是“一堆 Processor 堆在一起”，而是先在 `registerProcessor()` 里把不同 RequestCode 明确分发到不同入口和平面。** 本篇聚焦 `BrokerController.registerProcessor()`、RequestCode → Processor 映射和 executor 隔离；不重讲每个 Processor 的完整业务细节。

## 先把真正的困惑摆出来：Broker 为什么不能一个大 switch 就完了

如果只从“收到网络请求，然后做点事”这个抽象看，RocketMQ 的 Broker 完全可以想象成一个统一入口：

```text
收到 RemotingCommand
  → switch(requestCode)
    → 然后处理 send / pull / heartbeat / admin / transaction ...
```

这当然可以工作，但它会立刻带来一个结构性问题：**所有请求在运行时和理解路径上都会混成一锅。**

因为 RocketMQ 的请求并不是同一种东西。

- `SEND_MESSAGE` 走的是写入主链；
- `PULL_MESSAGE` 走的是消费与长轮询主链；
- `HEART_BEAT` 走的是客户端存活登记链；
- `END_TRANSACTION` 走的是半消息收束链；
- 各类 admin request 则更偏运维与查询世界。

如果这些请求都塞进一个统一 Processor 或统一线程池，主链会先在哪坏掉？

- 语义上，你会看不清“某个请求到底属于哪条系统主线”；
- 资源上，慢 admin 查询、consumer offset 操作、事务处理都可能挤占 send/pull 这种高频链路；
- 维护上，每增加一种 requestCode，统一入口都会继续膨胀。

所以 RocketMQ 真正的做法不是“一个总入口内部再慢慢分”，而是：**在 Broker 启动阶段，就把关键 RequestCode 显式绑定到不同 Processor 和不同 executor。**

可以先把总图压成：

```text
Remoting request(RequestCode)
  → BrokerController.registerProcessor()
    → SEND_*          → SendMessageProcessor + sendMessageExecutor
    → PULL_*          → PullMessageProcessor + pull/litePull executor
    → HEART_BEAT      → ClientManageProcessor + heartbeatExecutor
    → OFFSET/GROUP    → ConsumerManageProcessor + consumerManageExecutor
    → END_TRANSACTION → EndTransactionProcessor + endTransactionExecutor
    → 其他管理命令     → AdminBrokerProcessor(default) + adminBrokerExecutor
```

*关键设计（斜体）：* *RocketMQ 的 Broker 稳定性不只来自“功能很多”，更来自这些功能在启动时就被挂到不同入口和平面上：谁接请求、谁占哪类线程资源、谁属于哪条主链，全都先被钉死。*[模式: 请求码显式分发 + 入口边界先固定]

## 第一层：`registerProcessor()` 真正装配的不是类名，而是 Broker 的请求入口图

前文在 Broker 宿主篇里已经讲过，`initialize()` / `start()` 装的是宿主平面。但要真正看见“宿主怎样接住外部请求”，最关键的入口其实是 `BrokerController.registerProcessor()`。

这段代码的价值不在于“有很多 register 调用”，而在于它把 Broker 的请求入口图显式化了。也就是说，RocketMQ 在启动期就已经回答了：

- 哪些 RequestCode 进入发送世界；
- 哪些进入消费/长轮询世界；
- 哪些进入客户端存活与 offset 管理世界；
- 哪些进入事务世界；
- 哪些最终落到默认管理处理器。

一旦你从“入口图”角度看它，`registerProcessor()` 就不再是样板初始化，而是整个 Broker 运行时的交通图。

如果这层不先讲，后面每一篇都容易写成“这个 Processor 是某个专题里的一个类”；但真实情况恰恰相反：**每个 Processor 先是 Broker 世界中的入口，再是各自专题的实现者。**

所以本篇的第一原则是：先把 Processor 当作“入口平面”，再把它们还原成后续各篇已经讲过或还没讲到的主链挂载点。

## 第二层：发送、拉取、事务、心跳并不是同一种请求，因此它们不该共享同一入口语义

RocketMQ 在 `registerProcessor()` 里最先注册的就是发送相关入口：

- `SEND_MESSAGE`
- `SEND_MESSAGE_V2`
- `SEND_BATCH_MESSAGE`
- `CONSUMER_SEND_MSG_BACK`

它们统一进入 `SendMessageProcessor`。

紧接着，拉取相关入口被单独挂给 `PullMessageProcessor`：

- `PULL_MESSAGE`
- `LITE_PULL_MESSAGE`

再往后，事务二阶段 `END_TRANSACTION` 有自己的 `EndTransactionProcessor`；客户端心跳、注销、配置检查进入 `ClientManageProcessor`；offset/group 查询和更新进入 `ConsumerManageProcessor`；默认管理命令再落入 `AdminBrokerProcessor`。

这里要特别补一处源码边界：并不是所有“消费相关”的 RequestCode 都统一挂在 `PullMessageProcessor` 上。`PEEK_MESSAGE`、`POP_MESSAGE`、`NOTIFICATION`、`POLLING_INFO`、`ACK_MESSAGE`、`CHANGE_MESSAGE_INVISIBLETIME` 等也有各自的 Processor 或 executor 绑定。也就是说，真正的分发原则不是“发送一类、消费一类”这么粗，而是**按请求的运行时职责继续细分入口与资源面**。

这个分发结构本身就很说明问题。RocketMQ 没有把它们写成“反正都能在 Broker 里处理”，而是从注册时就承认：

```text
请求类型不同
  → 宿主入口不同
    → 线程资源和后续主链也不同
```

为什么要这样分？因为这些请求关心的问题根本不同：

- 发送请求关心消息校验、Topic/Queue、Store 写入、返回码；
- 拉取请求关心订阅、过滤、是否挂起、是否超时；
- PEEK/POP 关心不同的消费请求模式与可见性；
- ACK / invisible time 关心消费确认与投递状态；
- 心跳请求关心 client 身份登记和 consumer/producers 存活视图；
- 事务二阶段关心 prepare message 身份、commit/rollback 收束；
- admin 请求关心各种运维与元数据操作。

如果把它们塞进一个统一入口，主链会先在哪混乱？你会在同一个 `switch` 里同时维护：

- 消息写入语义；
- 消费可见性语义；
- 客户端存活语义；
- 运维管理语义；
- 事务一致性语义。

这种混法不只是难读，而是会直接让不同请求的资源边界和异常路径互相缠绕。

## 第三层：不同 executor 不是线程池小优化，而是主链资源隔离

很多人第一次看 `registerProcessor()` 会注意到另一个现象：RocketMQ 不只是给不同 Processor 绑定入口，还给它们绑定了不同 executor。

这很容易被误解成“线程池配置细节”。但实际上，它补的是主链资源隔离。

看典型绑定关系：

- `SendMessageProcessor` → `sendMessageExecutor`
- `PullMessageProcessor` → `pullMessageExecutor` / `litePullMessageExecutor`
- `ClientManageProcessor` → `heartbeatExecutor` / `clientManageExecutor`
- `ConsumerManageProcessor` → `consumerManageExecutor`
- `EndTransactionProcessor` → `endTransactionExecutor`
- `AdminBrokerProcessor` → `adminBrokerExecutor`

这说明 RocketMQ 从一开始就不愿意让所有请求共享同一个执行池。

为什么？因为只要共享，主链会先在资源竞争上变形：

- admin 查询慢了，send/pull 也会被拖慢；
- 心跳风暴来了，事务收束可能被挤压；
- 拉取长轮询与发送写入会互相争抢同一执行入口。

所以不同 executor 的意义不是“更好调优”，而是：

```text
不同请求世界
  → 先隔离线程资源
    → 再各自继续自己的主链
```

如果你把这一层忽略掉，后面长轮询篇、事务篇、发送篇都会变成“看起来只是逻辑不同”，但读者会看不见 RocketMQ 在运行时其实早已通过 executor 把这些世界隔开了。

## 第四层：default processor 不是主入口，而是显式入口之外的兜底管理平面

`registerProcessor()` 的最后，还有一个特别重要的动作：

```text
registerDefaultProcessor(adminProcessor, adminBrokerExecutor)
```

这说明 `AdminBrokerProcessor` 并不是 Broker 的“总入口”，而是：**在那些没有被关键主链显式注册的 RequestCode 之外，承担默认管理请求的兜底入口。**

这个边界特别值得讲清楚，因为很多人会把 default processor 误想成“Broker 一切请求最后都靠它”。但 RocketMQ 的结构刚好相反：

- 关键消息主链请求是显式注册的；
- default processor 处理的是那些更泛的管理/运维命令。

如果把 default processor 写成“真正的大脑”，主链会先在哪失真？会把发送、拉取、事务这些显式主链入口的结构价值直接抹平，好像所有请求最终都只是 admin 分支下的一个 case。

而 RocketMQ 这里的真正设计是：**先把关键入口独立出来，剩余才由 default 承接。**

所以 default processor 的价值并不是“什么都处理”，而是“让非关键主链命令有一个稳定兜底面，同时不污染关键主链的入口边界”。

## 第五层：长轮询和事务二阶段为什么都必须回到显式 Processor，而不是自己在旁路完成

前面两篇机动补深里，已经分别看到了：

- 长轮询最终要 `executeRequestWhenWakeup()` 回到 `PullMessageProcessor`；
- 事务回查和二阶段最终要回到 `EndTransactionProcessor`。

这件事很重要，因为它说明 Broker 的入口边界不是一次性的“请求进来时用一下”，而是后续所有异步/延后路径都必须再回到这张入口图里来。

### 长轮询

新消息到达以后，`PullRequestHoldService` 并不自己构造最终响应，而是重新交给 `PullMessageProcessor`。这意味着“唤醒 Pull 请求”和“真正判断这次能返回什么”是分开的。

### 事务

Producer 回查返回状态以后，最终仍通过 `END_TRANSACTION` 请求回到 `EndTransactionProcessor`，再决定 commit/rollback 怎样收束。

这两条链共同说明一个更普遍的规律：

```text
异步延后路径
  ≠
脱离原始 Processor 的旁路逻辑
```

如果长轮询自己拼响应、事务回查自己落 commit，主链会先在哪裂开？同一类请求会出现两套入口语义：

- 正常请求走显式 Processor；
- 异步唤醒/回查结果走另一套旁路逻辑。

那样 RocketMQ 的“入口边界先钉死”这层结构就会被自己打穿。

所以本篇必须把这个点收得很死：**Processor 分发图不只是网络请求的入口图，也是异步/延后路径最终必须回归的宿主入口图。**

## 第六层：Broker 处理器分发为什么值得单独成篇——因为它决定后面所有专题从哪一扇门进入

走到这里，就能看出为什么这篇不是“Broker 启动篇的附录”。

前面的篇章已经分别讲了：

- Producer 怎么发；
- Consumer 怎么拉；
- 长轮询怎么挂起再唤醒；
- 事务消息怎么 half、怎么 check、怎么 commit/rollback；

但如果没有一篇专门说明 **这些链在 Broker 世界里分别从哪一扇门进入**，读者就很容易形成一个错觉：这些专题只是分散在不同包里的实现，而不是被同一份宿主入口图组织起来的。

RocketMQ 这里真正成熟的地方就在于，它没有把“模块很多”误写成“入口杂糅”，而是在启动阶段先把：

- RequestCode 边界；
- Processor 边界；
- executor 资源边界；
- default 兜底边界；

全部钉死。后面每篇专题，其实都只是这张入口图上某一扇门背后的运行时展开。

所以这篇应该带给读者的不是“记住有多少 Processor”，而是：**Broker 真正稳定的前提，是先把不同请求的入口边界和资源边界画清楚。**

## 收网：Broker 不是“大 switch”，而是先把请求入口边界钉死的宿主

如果把整篇压成一句话，RocketMQ 的 Broker 之所以不是“一堆 Processor 堆在一起”，是因为它在 `registerProcessor()` 里先按 RequestCode 把发送、拉取、心跳、事务和管理命令明确分配到不同 Processor 与不同 executor；这张入口图既决定了请求从哪里进入 Broker 世界，也决定了异步唤醒和事务回查最终要回到哪条主链里收口。

```text
RequestCode
  → 显式 Processor 入口
    → 对应 executor 资源隔离
      → 进入各自消息主链
        → 异步/延后路径最终再回到同一入口
```

到这里，主线只发生了五件事。

第一，Broker 不是一个大 switch，而是一张显式的请求入口图。

第二，发送、拉取、心跳、事务、管理命令各自拥有不同 Processor 入口，不只是因为代码分包，而是因为主链职责不同。

第三，不同 executor 先把不同请求世界的资源边界隔开，避免它们互相拖垮。

第四，default processor 是显式主链入口之外的兜底面，不是 Broker 的总大脑。

第五，长轮询唤醒和事务二阶段这类异步路径，最终也必须回到对应 Processor，才能保持主链语义一致。

**本篇的一句话困惑**：Broker 为什么不能是“一个大 switch 处理所有请求”？

**本篇的一句话顿悟**：因为 RocketMQ 真正稳定的 Broker，不是功能很多，而是先把不同 RequestCode 的入口边界和线程资源边界钉死；每条消息主链都先从自己那扇门进去，后面的异步路径也再回到同一扇门收口。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Processor 分层只是代码组织习惯。”** 它首先是运行时入口边界。
2. **“default processor 才是真正主入口。”** 关键消息主链入口是显式注册的。
3. **“不同 executor 只是线程池优化。”** 它们先隔离了不同主链的资源世界。
4. **“长轮询/事务回查可以直接旁路完成。”** 最终仍要回到对应 Processor 收口。
5. **“Broker 只是模块多，看起来复杂。”** 真正的复杂度在入口边界和资源边界先被结构化了。

### 关键证据清单

- `broker/src/main/java/org/apache/rocketmq/broker/BrokerController.java:1070`：RequestCode → Processor 总装配入口。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/SendMessageProcessor.java:86`：发送主链入口。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/PullMessageProcessor.java:287`：拉取主链入口。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/ClientManageProcessor.java:57`：心跳/客户端管理入口。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/ConsumerManageProcessor.java:58`：offset/group 管理入口。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/EndTransactionProcessor.java:57`：事务二阶段入口。
- `broker/src/main/java/org/apache/rocketmq/broker/processor/AdminBrokerProcessor.java:248`：默认管理入口。
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:147`：长轮询唤醒后重新回到 `PullMessageProcessor`。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇聚焦 RequestCode 分发与宿主入口，不深入 Netty remoting 层和每个 Processor 的全部算法细节。
- 本文把 Processor 看成“主链入口”，不把它写成各专题的重复说明书。
- 本文不展开 Pop / Notification / Ack 等全部分支内部实现，只把它们作为入口图中的邻近节点点到为止。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-2` 的 Broker 宿主能力、`RocketMQ-23` 的长轮询唤醒重入、`RocketMQ-21` 的事务二阶段入口。
- 后续桥接：如果继续补机动篇，下一篇建议再做 `Controller/DLedger 对照`，把数据面与控制面的一致性结构再收一次。