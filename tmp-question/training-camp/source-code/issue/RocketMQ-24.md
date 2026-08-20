# RocketMQ-24. Producer 为什么不会每次都远程问 NameServer —— 路由缓存与刷新主链

> 场景：前面已经把 Producer 路由发现、发送决策和失败规避主链讲清了。到这里，读者很容易继续追问一个更细的问题：既然最终目的地信息来自 NameServer，那 Producer 为什么不会每次 send 都实时去问一遍？它手里的路由视图到底缓存在哪里，又是怎样刷新的？
>
> 本篇只回答一个问题：**Producer 为什么不会每次都远程问 NameServer，以及 RocketMQ 怎样靠本地路由缓存维持发送主链的本地决策能力。** 本篇聚焦 `TopicPublishInfo`、`MQClientInstance`、`MQClientAPIImpl` 和 Broker 侧 `TopicRouteInfoManager` 的缓存/刷新链；不回头重讲 `MQFaultStrategy` 的失败规避，也不展开 NameServer 内部元数据存储细节。

## 先把真正的困惑摆出来：为什么不每次发送都去问 NameServer

从最朴素的角度看，Producer 手里只有一个 topic，而 NameServer 才知道这个 topic 当前分布在哪些 Broker 上、有哪些可写队列。所以一种看似最稳妥的做法就是：

```text
每次 send
  → 先问 NameServer 当前路由
    → 拿到最新 TopicRouteData
      → 再决定发给谁
```

这个直觉之所以危险，不是因为它完全错，而是因为它把 NameServer 从“路由来源”误写成了“每次发送都必须实时参与的一跳”。

如果 Producer 真的每次都这么做，主链会先在哪坏掉？不会先坏在正确性，而会先坏在**发送主链失去本地决策能力**这里。因为：

- 每次发送都要额外多一轮 RPC；
- NameServer 的瞬时抖动会直接拖慢发送时延；
- 即使刚刚才发过同一个 topic，下一条消息也还是要重复问一次；
- Producer 端根本无法形成自己的 publish 视图，只能不断远程依赖。

RocketMQ 显然不想让发送主链变成“每次都现问现算”。它真正想要的是：

- **先在本地保留一份可发布视图**；
- **发送时优先消费这份本地视图**；
- **只有缓存缺失、不可用或后台周期到达时，才去拉新路由。**

所以这篇要先立住一个关键边界：

```text
NameServer
  → 路由来源

TopicPublishInfo / topicRouteTable / brokerAddrTable
  → 本地可复用的发送视图
```

一旦看清这个边界，后面“为什么 Producer 能本地选队列、为什么失败后能快速重试、为什么偶尔会拉路由”这些行为就都会重新对齐起来。

*关键设计（斜体）：* *RocketMQ 不是让 Producer 每次 send 都去远程拿一次路由，而是先把 NameServer 视图压成一份本地 publish cache；只有这样，发送主链才能在客户端本地完成多数决策。*[模式: 路由来源外置 + 发送视图本地化]

## 第一层：`TopicPublishInfo` 不是普通缓存对象，而是 Producer 当前能不能发、该往哪发的发布视图

`DefaultMQProducerImpl.tryToFindTopicPublishInfo()` 是理解这篇的最佳入口。

发送前，Producer 不是先发 RPC 到 NameServer，而是先查本地 `topicPublishInfoTable`：

- 如果本地已经有这个 topic 的 `TopicPublishInfo` 且它 `ok()`；
- 或者至少 `haveTopicRouterInfo` 已经成立；
- 就可以直接继续走后面的目标选择和发送主链。

只有本地没有、或者 publish view 还不够好时，才调用：

```text
mQClientFactory.updateTopicRouteInfoFromNameServer(topic)
```

这里最该强调的是：`TopicPublishInfo` 并不是“随便缓存一下 route data”的对象。它里面实际承载的是 Producer 这次发送真正要消费的发布视图：

- `messageQueueList`：当前 topic 的候选写队列；
- `sendWhichQueue`：本地选择队列时的游标；
- `haveTopicRouterInfo`：这份视图是不是已经建立过；
- `topicRouteData`：背后的原始路由数据。

所以 `TopicPublishInfo` 的地位更像：

```text
Producer 当前发送主链可直接消费的 publish view
```

而不是：

```text
把 NameServer 返回值临时存一下
```

如果把它写成普通缓存对象，主链会先在哪失真？会看不见：

- `selectOneMessageQueue()` 消费的不是抽象缓存，而是它；
- `MQFaultStrategy` 规避的不是抽象路由，而是它里的候选队列；
- 发送失败后能立即重选目标，也建立在这份本地 publish view 已经手握在客户端这一前提上。

## 第二层：发送主链真正先做的是“本地有无视图”，而不是“每次都去远程刷新”

`tryToFindTopicPublishInfo()` 的逻辑其实非常能说明 RocketMQ 的哲学。

它不是：

```text
send
  → updateTopicRouteInfoFromNameServer()
  → 再发
```

而是：

```text
send
  → 先看本地 TopicPublishInfo
    → 不够再 updateTopicRouteInfoFromNameServer()
      → 然后继续发送
```

这意味着 NameServer 在发送主链里的位置，是**按需介入**，而不是**每次必经**。

为什么这个顺序这么重要？因为它决定了发送主链的重心到底在哪里：

- 如果每次都先远程刷新，重心在控制面；
- 如果先消费本地视图，重心仍在客户端本地。

RocketMQ 之所以能在前面两篇里把“选队列”“故障规避”“失败后重选”都写成本地动作，前提就在这里：Producer 手里真的握着一份本地 publish view，而不是每次都去远程申请一个答案。

如果这一点没先讲清楚，后面所有“为什么 RocketMQ 发送得快”“为什么失败后还能迅速换目标”都会显得像魔法。

## 第三层：`MQClientInstance` 后台定时刷新，让缓存不是一次性快照

只讲“本地缓存”还不够，因为读者接下来一定会问：既然 Producer 不每次都去问，那缓存会不会越来越旧？

答案是：会旧，所以 RocketMQ 还有后台周期刷新链。

`MQClientInstance.startScheduledTask()` 会定时做几类事情，其中和路由最相关的是：

```text
scheduled updateTopicRouteInfoFromNameServer()
```

它会：

- 收集当前所有 Consumer 订阅的 topic；
- 收集当前所有 Producer 发布过的 topic；
- 合并成 `topicList`；
- 对这些 topic 逐个执行 `updateTopicRouteInfoFromNameServer(topic)`。

这条链说明缓存并不是“一旦建立就永不更新”。RocketMQ 的策略是：

- **发送现场按需补一次**；
- **后台任务周期性续命一次**。

二者配合起来，才能形成一套真正稳定的路由缓存机制。

如果只有按需刷新，没有后台续命，主链会先在哪老化？那些长时间持续发送、但中间没有触发本地缺失条件的 topic，可能一直沿用一份已经陈旧的路由视图。

如果只有后台刷新，没有发送现场按需补一次，主链又会先在哪变钝？一个新 topic 第一次发送时，Producer 可能还没等到调度周期，就根本拿不到可用的发布视图。

所以 RocketMQ 的路由缓存刷新不是单线程思维，而是：

```text
前台按需补
+
后台周期续命
```

## 第四层：真正的远程入口是 `getTopicRouteInfoFromNameServer()`，它只提供原始路由事实，不直接替你完成发送决策

到了这里，就可以回头看 `MQClientAPIImpl.getTopicRouteInfoFromNameServer()` 了。

这条 RPC 做的事情很朴素：

- 组装 `GET_ROUTEINFO_BY_TOPIC` 请求；
- 发给 NameServer；
- 成功时反序列化成 `TopicRouteData`；
- 失败时抛出相应异常。

注意，它只解决了一件事：**把 NameServer 当前持有的原始路由事实拉回来。**

它并没有直接替 Producer 决定：

- 这次选哪个 MessageQueue；
- 这个 Broker 此刻值不值得碰；
- 失败后要不要重选别的 Broker。

这些问题都留在后面的本地视图和本地决策链上处理。

所以 NameServer RPC 的正确位置是：

```text
Route source
  → TopicRouteData 原始事实
    → 本地 publish / subscribe 视图转换
      → 本地目标决策
```

如果把 `getTopicRouteInfoFromNameServer()` 直接等同于“发送决策完成”，主链会先在哪塌掉？会把路由来源、缓存转换、本地队列选择、故障规避几层完全压成一次 RPC 的附属结果。

## 第五层：为什么不仅要缓存队列列表，还要缓存 `brokerAddrTable`

另一个很容易被低估的细节是：光有 topic 对应的 queue 列表还不够。

Producer 最终要真正发消息，还得知道：

- 这个 queue 对应哪个 brokerName；
- 这个 brokerName 现在的 publish 地址是什么。

这也是为什么客户端和 Broker 侧路由管理里，都不只有 `topicRouteTable`，还有 `brokerAddrTable`。

路由缓存如果只有：

```text
topic → messageQueueList
```

却没有：

```text
brokerName → brokerAddr
```

主链会先在哪卡住？会卡在最后一跳：你已经知道“这次该发给 brokerA 的 queue-1”，但真正下网络请求时却不知道 brokerA 当前地址是什么。

所以 RocketMQ 的本地路由缓存至少包含两类信息：

1. **发布视图**：有哪些候选 queue；
2. **地址映射**：这些 queue 背后对应哪个可联系的 broker 地址。

这也解释了为什么 `TopicRouteInfoManager.findBrokerAddressInPublish()`、`MQClientInstance.findBrokerAddressInPublish()` 这种动作必须存在。发送主链最终不是把 queue 名字发出去，而是要找到一个真实可连的 broker 地址。

## 第六层：路由缓存存在，不等于缓存永远可信；RocketMQ 还会检查“是不是需要更新”

即使后台任务在刷，RocketMQ 也没有盲信“旧缓存只要存在就一定能继续用”。

在 Broker 侧的 `TopicRouteInfoManager.updateTopicRouteTable()` 里就能看见这种思路：

- 先比较新的 `TopicRouteData` 和旧数据是不是有变化；
- 即使没变化，如果 `TopicPublishInfo` 还不 `ok()`，也可能仍需要更新；
- subscribe / publish 视图会分别更新。

客户端侧虽然代码组织不同，但本质逻辑一致：缓存是否可用，不只是看“Map 里有这个 key”，还要看这份视图是不是足够支撑当前主链继续往下走。

如果缓存存在就绝不更新，主链会先在哪掉坑？

- route 数据也许已经变化；
- 某些 queue 已经不再可写；
- broker 地址也许已经切换；
- 但客户端还抱着一份“逻辑上有这个 topic”却实际不可发的旧视图。

所以 RocketMQ 的缓存哲学不是“有就一直用”，而是：

```text
先尽量复用
  → 但持续准备在必要时更新
```

## 第七层：为什么 Broker 自己也要有一套路由缓存——它服务的是另一条主链

到这里还要再补一个特别容易混淆的点：不仅客户端有路由缓存，Broker 侧也有 `TopicRouteInfoManager`。这会让人下意识地问：是不是两边其实是一回事？

不是。

客户端的 `TopicPublishInfo` / `topicRouteTable` 主要服务 Producer/Consumer 本地决策主链；Broker 侧的 `TopicRouteInfoManager` 则更偏向：

- EscapeBridge 这类跨 Broker 发送；
- Pop / QueryAssignment 这类依赖 subscribe info 的场景；
- Broker 内部自己需要一个可复用的路由/订阅视图。

所以两边都缓存路由，不代表它们的职责相同。

如果把客户端缓存和 Broker 缓存写成同一件事，主链会先在哪失焦？会把“Producer 为什么能本地选目标”和“Broker 内部为什么也要知道别的 Broker 地址”混成一锅，最后谁都讲不深。

这也是机动补深篇应该单独存在的原因：前文为了主链收束，只需要知道 Producer 有本地视图即可；到了补深篇，才有空间把“同样叫路由缓存，但客户端和 Broker 侧究竟服务哪条链”讲清楚。

## 第八层：缓存优先不是为了省几次 RPC，而是为了让发送主链真正留在本地

写到这里，最该收紧的一句话是：RocketMQ 之所以不每次都远程问 NameServer，并不是因为“RPC 很贵”这么简单。

更根本的原因是：**只要发送主链每次都必须远程问路，它就不再是客户端本地决策链，而会退化成一个被控制面实时牵着走的同步流程。**

而 RocketMQ 前面整个 Producer 世界——

- 本地 publish view；
- 本地 queue 选择；
- 本地 `MQFaultStrategy` 规避；
- 失败后快速换目标；

都建立在这样一个前提上：客户端手里先有一份能消费的本地视图。

所以缓存优先真正守住的是：

```text
发送主链的本地性
```

NameServer 仍然重要，但它更像是定期供给事实、按需修正事实，而不是每次发送都远程裁决一次目的地。

## 收网：RocketMQ 的路由发现不是“一次 RPC”，而是“本地视图优先 + 按需拉新 + 后台续命”

如果把整篇压成一句话，RocketMQ Producer 之所以不会每次都远程问 NameServer，是因为路由发现真正被做成了一条“本地视图优先、按需拉新、后台定时续命”的复合主链：Producer 先消费 `TopicPublishInfo` 这样的本地 publish view，只有缓存缺失或不可用时才远程拉 `TopicRouteData`，随后再更新 queue 列表和 broker 地址映射，继续把发送决策留在客户端本地完成。

```text
send(topic)
  → tryToFindTopicPublishInfo()
    → 本地缓存可用：直接进入目标决策
    → 本地缓存缺失/不可用：getTopicRouteInfoFromNameServer()
      → 更新 publish view + brokerAddrTable
        → 后续发送继续消费本地视图
  → 后台周期任务持续刷新 topic 路由
```

到这里，主线只发生了五件事。

第一，NameServer 是路由来源，但不是每次发送的实时决策者。

第二，`TopicPublishInfo` 不是普通缓存对象，而是 Producer 当前可消费的发布视图。

第三，路由刷新不是一次性动作，而是“发送现场按需补 + 后台任务周期续命”的组合。

第四，仅缓存 queue 列表还不够，最终还要缓存 broker 地址映射，发送主链才能真正落到网络目标。

第五，缓存优先真正守住的不是几次 RPC，而是 Producer 发送主链的本地决策能力。

**本篇的一句话困惑**：Producer 为什么不会每次都远程问一次 NameServer？

**本篇的一句话顿悟**：因为 RocketMQ 把路由发现做成了“本地 publish view 优先、缺了再远程补、后台再持续刷新”的结构；只有这样，发送、重选和故障规避这些动作才能真正留在客户端本地完成。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“每次 send 都会去 NameServer 问一次路由。”** 发送前先看本地 `TopicPublishInfo`。
2. **“TopicPublishInfo 只是简单缓存对象。”** 它是 Producer 当前的 publish view。
3. **“缓存一旦建立就永不更新。”** 发送现场和后台任务都会触发刷新。
4. **“只要有 queue 列表就够了。”** 还需要 broker 地址映射才能真正发出去。
5. **“客户端缓存和 Broker 路由缓存是一回事。”** 两边服务不同主链。

### 关键证据清单

- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:883`：发送前先查本地 `TopicPublishInfo`。
- `client/src/main/java/org/apache/rocketmq/client/impl/factory/MQClientInstance.java:332`：后台周期任务拉新 NameServer 地址与 topic route。
- `client/src/main/java/org/apache/rocketmq/client/impl/factory/MQClientInstance.java:381`：遍历 Producer/Consumer topic 做定时路由刷新。
- `client/src/main/java/org/apache/rocketmq/client/impl/MQClientAPIImpl.java:2039`：真正的 NameServer 路由 RPC 入口。
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/TopicPublishInfo.java:47`：publish view 是否可用的 `ok()` 判断。
- `broker/src/main/java/org/apache/rocketmq/broker/topic/TopicRouteInfoManager.java:68`：Broker 侧也有周期路由刷新。
- `broker/src/main/java/org/apache/rocketmq/broker/topic/TopicRouteInfoManager.java:92`：Broker 侧按需拉 topic route。
- `broker/src/main/java/org/apache/rocketmq/broker/topic/TopicRouteInfoManager.java:196`：Broker 侧 `tryToFindTopicPublishInfo()` 的缓存优先逻辑。
- `client/src/test/java/org/apache/rocketmq/client/producer/DefaultMQProducerTest.java:152`：无路由时发送失败与拉路由测试证据。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇聚焦客户端 publish 路由缓存，并顺带点到 Broker 侧 `TopicRouteInfoManager`；不展开 NameServer 服务端 `RouteInfoManager` 的内部存储细节。
- 本文不重讲 `MQFaultStrategy` 的故障规避策略，只把它作为“为什么本地视图必须存在”的后续消费方。
- 本文不展开静态 Topic / QueueMapping 的深层变换逻辑，先守住经典 topic route cache 主链。