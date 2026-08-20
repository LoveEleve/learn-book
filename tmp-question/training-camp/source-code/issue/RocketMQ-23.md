# RocketMQ-23. Consumer 没消息时为什么不立即返回——Pull 长轮询与挂起请求主链

> 场景：前面已经讲过 Consumer 怎样拉取消息、Broker 怎样按 ConsumeQueue 找到消费视图。到这里，读者会遇到一个非常现实的运行时问题：如果 Consumer 此刻拉不到消息，Broker 为什么不直接返回一个空结果？为什么 RocketMQ 还要把这个请求挂起来？
>
> 本篇只回答一个问题：**Consumer 暂时没有消息时，RocketMQ 怎样把 Pull 请求挂起，并在新消息到达或超时后重新驱动它。** 本篇聚焦经典 Pull 长轮询的 Broker 侧主链：`PullMessageProcessor`、`PullRequestHoldService`、`ManyPullRequest`、消息到达通知、过滤判断与超时唤醒；不展开 Pop long polling、LitePull 和消费组协调。

## 先把真正的困惑摆出来：没有消息，为什么不直接返回空

从同步 RPC 的角度看，Consumer 发起 Pull 请求，Broker 查不到消息，直接返回空结果似乎是最自然的做法：请求来了，马上给结果，客户端再过一会儿重新拉一次。

但消息系统的消费请求和普通查询有一个很重要的区别：Consumer 拉取的不是一个偶发查询，而是一条持续运行的消息流。

如果 Broker 每次查不到消息都立即返回，Consumer 就只能不断重复同一个动作：

```text
Pull
  → 没消息
    → 返回空
      → sleep 一小会儿
        → 再 Pull
          → 仍没消息
            → 继续循环
```

当 Consumer 数量、Topic 数量和队列数量上来以后，这条链会先在哪浪费？不会先坏在消息正确性，而会先坏在**大量没有结果的请求反复穿过网络、线程池和存储查询链**这里。

更糟的是，消息什么时候到达是不确定的。你把 sleep 设短了，空拉风暴更严重；设长了，消息到达以后又会出现额外延迟。

所以 RocketMQ 需要把问题从“客户端多久再问一次”改成“Broker 暂时没有结果时，能不能先保留这次请求，等结果可能出现时再继续处理”。

这就是长轮询的真正动机：它不是为了让某个 Broker 线程一直睡着，而是把一次暂时没有结果的 Pull 请求，转移成一个由消息到达事件或 timeout 驱动的挂起状态。

```text
Consumer Pull
  → Broker 当前没有可返回消息
    → 暂存 PullRequest
      → 新消息到达时尝试唤醒
        → 没匹配则继续挂起
          → 超时才重新执行并返回
```

*关键设计（斜体）：* *RocketMQ 长轮询把“暂时无结果”从一次立即失败的 RPC 结果，改造成 Broker 内部可保存、可被消息事件唤醒、也可被 timeout 正常收口的请求状态。*[模式: 挂起请求 + 事件唤醒 + 超时收口]

## 第一层：长轮询不是让工作线程阻塞，而是把请求移交给挂起服务

理解长轮询的第一步，是先否定一个非常常见的实现想象：Broker 收到请求以后，工作线程就一直 `sleep`，直到消息来了再返回。

如果每个 Pull 请求都占住一个工作线程等待消息，主链会先在哪耗尽？会耗尽在并发等待数量上。消息系统里的 Consumer 数量可以远大于 Broker 能长期持有的工作线程数量，线程阻塞等待无法成为稳定的挂起模型。

RocketMQ 的做法是把请求对象移交给 `PullRequestHoldService`。当 Pull 请求当前没有结果并满足挂起条件时，`suspendPullRequest(topic, queueId, pullRequest)` 会：

- 以 `topic@queueId` 生成分桶 key；
- 找到或创建对应的 `ManyPullRequest`；
- 把请求标记为 suspended；
- 放入这个分桶的挂起请求列表。

```text
PullMessageProcessor
  → suspendPullRequest(topic, queueId, pullRequest)
    → pullRequestTable[topic@queueId]
      → ManyPullRequest.addPullRequest()
```

这里的关键不是“多了一个 List”，而是请求的所有权发生了转移：

- 网络请求处理线程不再负责一直等；
- `PullRequestHoldService` 接管了“什么时候重新尝试这个请求”；
- 消息到达通知和 timeout 都可以成为后续驱动事件。

所以长轮询不是线程模型上的睡眠，而是请求生命周期上的暂存。

## 第二层：为什么按 `topic@queueId` 分桶，而不是把所有挂起请求放一个总队列

既然请求已经被挂起，下一步就要解决：新消息到达以后，Broker 该唤醒哪些请求？

RocketMQ 没有把所有挂起请求放在一个全局队列里，而是按 `topic@queueId` 分桶，维护：

```text
pullRequestTable
  → "TopicA@0" → ManyPullRequest
  → "TopicA@1" → ManyPullRequest
  → "TopicB@0" → ManyPullRequest
```

这个分桶直接对应消息到达事件的粒度。某个 Topic 的某个 queue 有新消息时，Broker 首先只需要处理对应桶里的挂起请求，不必扫描全世界所有 Consumer 的等待请求。

如果所有请求都放一个全局队列，主链会先在哪变差？会变差在通知放大：一条 `TopicA@0` 的新消息，可能要让 Broker 扫描大量与它毫无关系的 `TopicB@1`、`TopicC@3` 请求。

所以 `topic@queueId` 不是普通 Map key，而是把“消息到达的局部性”直接传递给挂起请求管理。

`ManyPullRequest` 自己也没有把消息缓存进来。它保存的是一批等待重新执行的 `PullRequest`，提供 `addPullRequest()` 与 `cloneListAndClear()`：

```text
ManyPullRequest
  → add: 收集等待请求
  → cloneListAndClear: 批量取出并清空当前列表
```

这一步的意义是让通知处理可以先拿走当前快照，再逐个判断和唤醒，避免一边遍历一边持续修改同一个列表。

## 第三层：没有消息到达事件时，PullRequestHoldService 还要靠周期检查兜底

长轮询不能只依赖“新消息写入时一定会发通知”。系统还需要一个兜底路径，处理：

- 通知丢失；
- 请求已经等待太久；
- Broker 状态变化导致需要重新执行；
- 某些消息到达路径没有携带足够的过滤信息。

`PullRequestHoldService.run()` 会周期运行：

- 开启长轮询时，默认按 5 秒周期检查；
- 未开启长轮询时，按 `shortPollingTimeMills` 周期检查。

检查动作会遍历当前挂起表，取每个 `topic@queueId` 的最新消费队列 offset，再调用 `notifyMessageArriving(topic, queueId, offset)` 进入同一套唤醒判断。

因此长轮询和短轮询的区别不是“一个有请求列表，一个没有”，而是：

```text
长轮询：请求可以挂起，周期检查只是兜底
短轮询：更频繁地检查并重新驱动挂起请求
```

如果没有周期检查，主链会先在哪留下死角？会留下“消息已经存在、请求也在等待，但通知链没有把它唤醒”的永久挂起请求。

所以 `PullRequestHoldService` 既有事件驱动路径，也有周期兜底路径。

## 第四层：新消息到达不等于所有挂起请求都应该立即返回

消息到达以后，最容易被写错的逻辑是：只要某个 queue 有新消息，就把这个 queue 下的所有 PullRequest 全部唤醒。

RocketMQ 的 `notifyMessageArriving()` 并没有这么粗暴。它会先把当前桶里的请求批量取出来，然后对每个请求分别判断：

1. 最新 queue offset 是否已经超过请求的起始 offset；
2. 新消息的 Tag/ConsumeQueue 信息是否匹配；
3. 如果需要，CommitLog properties 过滤是否也匹配；
4. 如果不匹配，请求是否已经到 timeout。

核心判断可以压成：

```text
newestOffset > pullFromThisOffset
  → 再判断 MessageFilter
    → 匹配：执行请求
    → 不匹配：继续挂起
```

如果新消息到了但 offset 没超过请求起点，主链会先在哪失败？会误唤醒一个其实没有新结果的 PullRequest，造成无效执行。

如果 offset 超过了但过滤条件不匹配，主链会先在哪失败？会把不属于这个 Consumer 订阅条件的消息误当成可返回结果，直接破坏前文讲过的 Broker 侧过滤语义。

所以消息到达事件只是“可能有结果”的信号，不是“当前请求一定有结果”的结论。

## 第五层：过滤不匹配时，请求不是失败，而是重新放回挂起队列

`notifyMessageArriving()` 对过滤请求的处理非常值得单独拎出来。

当新消息的 offset 已经超过请求起始位置，但 `MessageFilter` 判断不匹配时，代码不会直接把请求返回给 Consumer，也不会把它当错误请求丢掉，而是把它重新放回 `replayList`，最后再加入对应的 `ManyPullRequest`。

```text
消息到达
  → offset 已前进
    → filter 不匹配
      → replayList
        → ManyPullRequest.addPullRequest()
          → 等下一条消息或 timeout
```

这条路径说明了一个非常关键的语义：

**长轮询等待的是“下一条满足订阅条件的消息”，不是“下一条任意消息”。**

如果过滤不匹配就立即返回空结果，Consumer 会把一次本来应该继续等待的请求当成“当前没有结果”，随后重新发起新的 Pull，系统就会重新产生不必要的空拉。

所以过滤逻辑和长轮询并不是两个互不相干的功能：

- 过滤决定这次消息到达能不能唤醒请求；
- 长轮询决定不匹配时请求继续留在什么地方。

这也解释了为什么前文的消息过滤专题和本篇存在连接：Broker 侧过滤不仅影响“返回哪条消息”，还影响“挂起请求什么时候真正结束”。

## 第六层：timeout 不是异常，而是挂起请求的正常收口路径

长轮询不能无限等待。即使没有新消息，也即使一直没有匹配的过滤结果，PullRequest 也必须最终结束。

每个 `PullRequest` 都带有 suspend timestamp 与 timeout。`notifyMessageArriving()` 在处理请求时，如果：

- 当前没有匹配的新消息；
- 请求也已经超过 `suspendTimestamp + timeoutMillis`；

它会调用 `executeRequestWhenWakeup()` 重新执行这个请求，让 `PullMessageProcessor` 走正常响应路径，最终给 Consumer 一个空结果或当前状态。

这条路径很重要，因为它把“等待太久”从一个异常分支变成了协议正常语义：

```text
等待期间没有匹配消息
  +
超过 timeout
  → 重新执行 Pull
    → 返回当前没有结果
```

如果没有 timeout，主链会先在哪失效？会失效在资源生命周期：

- 客户端可能早已断开；
- 订阅可能已经变化；
- Broker 仍然保留旧请求；
- 挂起表会不断积累永远不会再有意义的等待对象。

所以 timeout 不是“长轮询失败了”，而是长轮询必须拥有的生命周期出口。

## 第七层：被唤醒以后不是直接拼响应，而是重新执行 PullMessageProcessor

长轮询还有一个很容易被低估的设计：消息到达时，Broker 不是直接把某条消息拼成响应发出去，而是调用：

```text
executeRequestWhenWakeup(channel, requestCommand)
```

也就是说，被唤醒的 PullRequest 会重新回到 `PullMessageProcessor` 的正常处理链。

为什么不在 `notifyMessageArriving()` 里直接组响应？因为 Pull 请求的完整语义远不止“有一条新消息”：

- offset 是否连续；
- 消息过滤是否匹配；
- 当前 Topic/Queue 是否还存在；
- 当前 Broker 是否仍允许读取；
- 静态 Topic 映射、冷数据流控、订阅版本等运行时条件是否变化。

如果唤醒路径自己重新复制一套 Pull 处理逻辑，主链会先在哪失控？会出现两套 Pull 语义：普通请求走一套，被唤醒请求走另一套，后续任何过滤、权限、映射和状态修复都可能出现分叉。

所以 RocketMQ 选择让唤醒请求重新进入同一个 `PullMessageProcessor`，把“等待结束”和“重新计算最终响应”分开：

```text
PullRequestHoldService
  → 只决定什么时候重试执行
PullMessageProcessor
  → 继续决定这次最终返回什么
```

这是一层非常重要的职责分离。

## 第八层：消息通知桥把存储写入和挂起请求唤醒接起来

长轮询要成立，必须有一条从“消息写入成功”到“等待请求被重新检查”的通知桥。

RocketMQ 的 `NotifyMessageArrivingListener` 就位于这个桥上。消息存储层产生新消息到达事件后，它会把通知转给相关的挂起服务，包括经典 Pull 长轮询路径。

```text
CommitLog / ConsumeQueue 新消息到达
  → NotifyMessageArrivingListener
    → PullRequestHoldService.notifyMessageArriving()
      → 按 topic@queueId 找等待请求
        → 唤醒 / 重挂起 / timeout 收口
```

如果存储层只负责把消息写进去、不通知挂起请求，主链会先在哪失效？Consumer 虽然最终能通过下一次主动 Pull 找到消息，但长轮询请求本身不会及时结束，消息到达与请求响应之间会出现额外延迟。

所以长轮询不是单独挂在 Broker 外面的一个等待服务，而是存储消息到达事件与网络请求生命周期之间的一座桥。

## 收网：长轮询本质是把“暂时无结果”变成可重入的请求状态

如果把整篇压成一句话，RocketMQ 长轮询并不是让 Broker 线程睡着等消息，而是把没有结果的 PullRequest 按 `topic@queueId` 暂存在 `ManyPullRequest` 中；新消息到达时，Broker 再按 offset 和过滤条件决定唤醒还是继续挂起，超时以后重新执行请求并正常返回。

```text
Pull 无结果
  → suspendPullRequest()
    → ManyPullRequest 分桶保存
      → 消息到达通知
        → offset / filter 判断
          → 匹配：executeRequestWhenWakeup()
          → 不匹配：重新挂起
      → timeout：重新执行并返回
```

到这里，主线只发生了五件事。

第一，长轮询不是工作线程阻塞等待，而是挂起请求所有权转移给 `PullRequestHoldService`。

第二，`topic@queueId` 分桶让消息到达通知只影响相关等待请求。

第三，新消息到达只是可能有结果，真正唤醒还要经过 offset 和过滤判断。

第四，过滤不匹配时请求继续挂起，timeout 才是正常收口路径。

第五，唤醒以后重新进入 `PullMessageProcessor`，避免普通 Pull 与唤醒 Pull 形成两套语义。

**本篇的一句话困惑**：Consumer 没消息时，Broker 为什么不立即返回空结果？

**本篇的一句话顿悟**：因为 RocketMQ 把“暂时无结果”变成了 Broker 内部可保存、可按消息到达事件重试、可按过滤条件继续挂起、也可由 timeout 收口的请求状态，而不是让 Consumer 不断空拉。

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“长轮询就是 Broker 工作线程 sleep。”** 实际是把 PullRequest 转移到挂起服务。
2. **“新消息到达就唤醒所有请求。”** 只处理对应 `topic@queueId`，还要经过 offset/filter 判断。
3. **“过滤不匹配就是本次 Pull 失败。”** 请求会继续放回挂起队列等待后续匹配消息。
4. **“timeout 是异常。”** timeout 是挂起请求正常结束的生命周期出口。
5. **“唤醒时可以直接拼响应。”** RocketMQ 会重新进入 `PullMessageProcessor`，复用完整 Pull 语义。

### 关键证据清单

- `broker/src/main/java/org/apache/rocketmq/broker/processor/PullMessageProcessor.java:287`：Pull 请求入口。
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:44`：挂起请求并按 `topic@queueId` 分桶。
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:68`：长轮询/短轮询周期检查。
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:100`：周期检查各桶的最新 queue offset。
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldService.java:118`：新消息到达后的 offset/filter/timeout 判断。
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/ManyPullRequest.java:25`：批量保存挂起请求。
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/ManyPullRequest.java:33`：clone-and-clear 批量取出请求。
- `broker/src/main/java/org/apache/rocketmq/broker/longpolling/NotifyMessageArrivingListener.java:40`：存储新消息到达通知桥。
- `broker/src/test/java/org/apache/rocketmq/broker/longpolling/PullRequestHoldServiceTest.java:90`：挂起、通知、唤醒测试证据。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇聚焦经典 Pull 长轮询；PopLongPollingService、ColdData、LMQ 子类只作为边界提示。
- 本文不展开 LitePull、Push、Rebalance 和消费组协调，只把它们作为前置消费主链背景。
- 本文把 timeout、过滤重挂起和唤醒重入都纳入长轮询闭环，不把它简化成一个等待参数。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-2` 的 Broker 宿主能力、`RocketMQ-5/6/7` 的 ConsumeQueue/Pull/过滤主链。
- 后续桥接：后续机动篇可继续补 Namesrv 路由缓存、Broker Processor 分发或 DLedger/Controller 对照；本篇先收住经典 Pull 的挂起与唤醒边界。