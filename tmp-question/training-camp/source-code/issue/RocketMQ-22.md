# RocketMQ-22. 发送失败以后，RocketMQ 为什么不是“重试一下就行”——MQFaultStrategy 与故障规避主链

> 场景：前面已经把 Producer 路由发现与发送主链讲清了：Producer 先拿到 `TopicPublishInfo`，再做队列选择，最后由 `sendKernelImpl()` / `MQClientAPIImpl` 真正把请求发给 Broker。但写到这里，读者还会自然冒出一个运行时问题：如果这次发送失败了，RocketMQ 接下来到底在做什么？
>
> 本篇只回答一个问题：**发送失败以后，RocketMQ 为什么不是“重试一下就行”。** 本篇聚焦 Producer 侧的失败规避与重新选目标：`MQFaultStrategy`、`LatencyFaultTolerance`、`selectOneMessageQueue()` 和 `sendDefaultImpl()` 的重试主链；不回头重讲 NameServer 路由发现，也不展开服务端为什么会失败。

## 先把真正的困惑摆出来：为什么失败后不能直接再发一次

从最直觉的角度看，发送失败这件事似乎非常简单：

- 第一次没发成功；
- 那就再发一次；
- 如果还不行，再多试几次。

很多系统里，所谓“重试机制”确实就是这么朴素。

但 RocketMQ 这里的问题在于：**失败不是一个抽象布尔值，它通常已经携带了“这个目标此刻不值得再碰”的运行时信息。**

如果 Producer 还把“重试”理解成“对同一个目标再打一遍”，主链会先在哪坏掉？会坏在它把已经暴露出来的故障信息完全浪费掉了。因为：

- 如果 Broker 网络暂时不可达，你继续打它，只是在放大同一类失败；
- 如果 Broker 刚刚响应超时或抛异常，静态队列视图虽然还在，但运行时可用性已经变了；
- 如果同一 Broker 上有多个 queue，问题可能根本不在“这条 queue 存不存在”，而在“这个 broker 当前是不是就不该再碰”。

所以发送失败后真正应该先问的，不是“再不要试一次”，而是：**下一次还应不应该继续打刚刚那个 Broker。**

RocketMQ 的回答不是“统一重试 N 次”，而是把“重试”拆成两层：

1. 记录这次失败对当前 Broker 的运行时判断；
2. 下一轮重新选目标时，尽量避开已经被判定为不值得碰的 Broker。

可以先把这条链压成一张图：

```text
Producer 选中一个 MessageQueue
  → sendKernelImpl()
    → 发送失败 / 超时 / Broker 异常
      → updateFaultItem(记录当前 Broker 故障语义)
        → MQFaultStrategy 再选下一轮目标
          → 优先可用 broker
            → 不行再退化到可达 broker
              → 最后才回到普通选择
```

*关键设计（斜体）：* *RocketMQ 发送失败后的关键不是“多发一次”，而是“下一次别再打错地方”；`MQFaultStrategy` 真正补的是运行时故障规避层，而不是简单轮询的附加优化。*[模式: 失败记忆 + 目标重选 + 规避故障点]

## 第一层：重试不是再打一遍，而是一次新的目标决策

这一点必须先钉死。RocketMQ 里的重试，不是把第一次请求原样对同一个 Broker 再打一遍。

`DefaultMQProducerImpl.sendDefaultImpl()` 的发送循环每次失败后，真正做的是：

- 记录刚才打过哪个 Broker；
- 更新这个 Broker 的 fault item；
- 再次调用 `selectOneMessageQueue(...)` 选新目标；
- 再用新的 queue 走下一轮 `sendKernelImpl()`。

这就意味着，重试的单位不是“再次执行同一网络动作”，而是：

```text
重新做一次 queue / broker 目标选择
  → 再把请求发出去
```

为什么要把这件事提得这么高？因为只要你把重试理解成“同一个请求再跑一遍”，就会天然低估 `MQFaultStrategy` 的地位，以为它只是锦上添花的负载均衡优化。

实际不是。RocketMQ 把它放在发送主链正中央，原因恰恰是：失败以后，如果目标不变，重试几乎就失去了最重要的意义。

如果下一轮还继续打同一个 Broker，主链会先在哪失败？会先失败在 Broker 级别的故障隔离上：你已经知道这台 Broker 刚刚不对劲，却继续把后续请求丢过去，重试就退化成了机械重复。

所以 RocketMQ 的“重试”正确理解应该是：**失败暴露运行时信息 → 目标重新决策 → 才是下一次发送。**

## 第二层：静态路由视图并不等于当前可用性，`MQFaultStrategy` 补的正是这层缺口

前文已经讲过，`TopicPublishInfo` 提供的是一张本地路由视图：哪些 Broker、哪些 Queue 当前逻辑上存在、可以成为候选目标。

但只要进入运行时失败场景，这张图就不够用了。

因为“候选目标存在”回答的是静态结构问题；而“当前值不值得碰”回答的是运行时健康度问题。RocketMQ 必须再补一层，把这两个问题区分开。

`MQFaultStrategy` 就是这一层。

它并不负责从 NameServer 拉路由，也不直接负责网络发送；它负责的是：**在已有候选队列世界里，再根据最近失败历史和当前可达性，把一些目标临时踢出去。**

所以这篇里最该先打掉的误解是：

```text
有路由视图
  ≠
 这些 Broker 当前都值得发
```

如果没有这层运行时筛选，主链会先在哪坏掉？只要一个 Broker 还留在静态路由里，Producer 就可能继续不断命中它；失败记忆完全不会影响后续决策，重试就只是在静态世界里盲撞。

因此 `MQFaultStrategy` 不是“再搞个更复杂的负载均衡”，而是把发送主链从“结构可发”推进到“运行时尽量别打错地方”。

## 第三层：`MQFaultStrategy.selectOneMessageQueue()` 真正补的是三层回退策略

理解 `MQFaultStrategy` 最好的方法，不是先看参数，而是先看它的选择顺序。

当 `sendLatencyFaultEnable` 打开时，它不会简单地从 `TopicPublishInfo` 里随便挑一个 queue。它大致会按三层策略走：

```text
1. 先选 available + 非上次 broker
2. 选不到，再选 reachable + 非上次 broker
3. 还选不到，最后才退回普通 selectOneMessageQueue()
```

这三层顺序非常有解释力。

### 第一层：available

`available` 表示这个 Broker 当前不在“隔离期”里。只要它还处在 faultItem 的不可用窗口内，就尽量别碰。

### 第二层：reachable

如果所有 available 候选都没有，就退化到“至少现在还能连得上”的 Broker。也就是说，即便已经不完全理想，也尽量避免打向一个明确不可达的 Broker。

### 第三层：普通回退

如果连 reachable 层也选不出来，RocketMQ 才退回普通队列选择。到这一步说明运行时过滤已经没法继续收紧，系统宁可回到基础选择，也不让发送彻底卡死。

这说明 `MQFaultStrategy` 的主线并不是“一票否决某些 Broker”，而是做一层渐进回退：

```text
尽量选最好
  → 不行退而求其次
    → 实在不行才回普通选择
```

如果没有这种分层回退，主链会先在哪变僵？

- 只看 available：一旦全都在隔离期，发送可能完全没得选；
- 只看 reachable：会忽略刚刚已经证明很慢或很差的 Broker；
- 直接普通回退：故障记忆几乎等于没用。

所以这三层顺序不是实现细节，而是 RocketMQ 在“尽量规避已知坏点”和“别把发送完全锁死”之间做的运行时折中。

## 第四层：`updateFaultItem()` 让失败真正变成“下一轮不要再打这里”的记忆

上一层讲的是如何选；这一层必须回答：RocketMQ 怎么知道某个 Broker 该暂时避开？

答案就是 `updateFaultItem()`。

每一轮发送失败或异常返回后，`sendDefaultImpl()` 会根据异常类型和耗时调用：

```text
updateFaultItem(brokerName, currentLatency, isolation, reachable)
```

这个调用不是记日志用的，而是实打实在更新 Producer 侧的运行时记忆。里面至少有三类信息：

- 当前这次耗时是多少；
- 是否需要“隔离”这个 Broker；
- 这个 Broker 从 Producer 视角看是否仍可达。

随后 `MQFaultStrategy` 会把 `currentLatency` 映射到 `notAvailableDuration`，例如耗时越长，隔离时间可能越长。`LatencyFaultToleranceImpl` 再把这个 Broker 写进 `faultItemTable`，记录：

- 当前延迟；
- 当前不可用到什么时候；
- 是否可达。

这就形成了一条很清晰的失败记忆链：

```text
发送失败 / 超时 / Broker 异常
  → 记录 brokerName + 延迟 + 可达性 + 隔离时长
    → 后续 selectOneMessageQueue() 尽量避开它
```

如果没有这层记忆，主链会先在哪失忆？第一次失败和第五次失败之间对 Producer 来说完全没区别。那 RocketMQ 就不可能做到“刚刚失败的点先别碰”，只能继续靠静态轮询赌运气。

所以 `updateFaultItem()` 的角色应该被理解成：它把一次失败，从“已经发生过的异常”升级成“下一轮决策需要考虑的历史”。

## 第五层：RocketMQ 并不是把所有失败都当成一种故障在处理

如果 RocketMQ 只是简单重试，它也就不必区分这么多异常分支。

但 `sendDefaultImpl()` 里，至少会把几类异常分开处理：

- `MQClientException`
- `RemotingException`
- `MQBrokerException`
- `InterruptedException`
- 以及同步返回但 `SEND_OK` 之外的状态

它们的共同点是都会进入重试/异常链；不同点在于，它们更新 fault item 的方式并不一样。

例如：

- `RemotingException` 在探测器开启时，会把 broker 标成 `reachable=false`；
- `MQBrokerException` 虽然也是失败，但更像 Broker 端已经明确回了错误，因此可达性和隔离语义不同；
- `MQClientException` 则更接近客户端侧的问题，但同样会触发 fault 记忆更新；
- `InterruptedException` 不继续吞掉，而是直接抛出。

这说明 RocketMQ 并没有把“失败”压扁成一个统一布尔值，而是在尽量保留：

- 这次失败是路不通；
- 还是节点能通但返回错；
- 还是调用链自身被打断。

如果把所有异常都统一看成“发失败了，换个 Broker 再试”，主链会先在哪损失信息？会损失在规避策略上：Producer 将不知道某台 Broker 是“暂时隔离”还是“明确不可达”，运行时避障就会越来越粗糙。

所以 RocketMQ 这里的关键不是重试次数，而是：**失败类型也在参与后续目标决策。**

## 第六层：`LatencyFaultToleranceImpl` 不是健康检查中心，而是 Producer 侧经验性隔离表

到了这里，很容易再掉进另一个误区：看到 `LatencyFaultToleranceImpl` 有探测线程、有 `isReachable()`、有 `detectByOneRound()`，就把它想成一个强一致的健康检查中心。

这其实会高估它。

它在 RocketMQ 里的真实位置，更像一个 Producer 侧的经验性隔离表：

- 某个 Broker 最近是不是慢过、错过；
- 现在是不是还处在隔离期；
- 在探测器开启时，它现在是不是重新 reachable 了。

它并不是 NameServer，也不是 Controller，更不是 Broker 自己对外宣告的权威健康状态。它只是 Producer 为了少打错地方，在本地维护的一份运行时判断表。

这一点非常重要，因为它决定了我们如何理解它的能力边界：

- 它能帮你避开刚刚证明不值得碰的 Broker；
- 但它不能替代真正的集群控制面；
- 它更像一种“经验性自我保护”，而不是“全局真相”。

如果把它误写成强一致健康中心，主链会先在哪理解错？会错把 Producer 的本地规避决策当成集群级裁决，从而夸大 `MQFaultStrategy` 的能力边界。

所以这里要记住：

```text
Controller / NameServer
  → 集群视图

LatencyFaultToleranceImpl
  → Producer 本地运行时规避视图
```

二者可以互补，但不是一回事。

## 第七层：`lastBrokerName` 与 `resetIndex` 说明 RocketMQ 不只是“避坏点”，还在避免连续命中同一 Broker

`sendDefaultImpl()` 在每一轮重试时，都会记录 `lastBrokerName`，并在第二轮及以后把 `resetIndex=true`。这说明 RocketMQ 的目标不仅是“别碰已知坏点”，还包含另一层更细的意图：**尽量别让下一轮又落回刚刚打过的 Broker。**

为什么这层细节重要？因为有时候失败并不是因为这个 Broker 永远不可用，而是它此刻刚刚表现不好。即使它还没进入长隔离，也不意味着下一轮立即再打它就是好主意。

所以发送失败后的目标决策实际上又多了一层：

```text
别打当前已知坏点
  +
尽量别连续命中上一次刚打过的 Broker
```

这使 RocketMQ 的发送规避更像一种“带记忆的目标重排”，而不是简单随机换个 queue。

如果没有 `lastBrokerName` 这层过滤，主链会先在哪退化？即使开启了故障规避，你也仍可能在短时间内不断回到同一个刚失败过的 Broker，只是换了同 Broker 下的另一个 queue 而已。

所以这层小细节，实际上是在补“Broker 级别的重试分散度”。

## 第八层：发送失败真正难的不是网络重发，而是“下一次别再打错地方”

走到这里，可以把 RocketMQ 发送失败后的主链压成一句真正有解释力的话了。

它不是：

```text
send fail → retry
```

而是：

```text
send fail
  → 记录这次失败对当前 Broker 的运行时判断
    → 下一轮重新选目标
      → 优先避开不可用和不可达 Broker
        → 再把新的目标真正发出去
```

这就是为什么 `MQFaultStrategy` 必须被视为发送主链的一部分，而不是旁枝末节。

如果没有它，RocketMQ 发送失败后的“重试”只剩机械重复；有了它，重试才开始真正具备运行时故障规避能力。

## 收网：RocketMQ 发送失败后的关键不是“多试一次”，而是“别再打错地方”

如果把整篇压成一句话，RocketMQ 发送失败后的真正关键不是“对同一请求多发一次”，而是 Producer 根据 `MQFaultStrategy` 和 `LatencyFaultTolerance` 先把这次失败变成一份本地故障记忆，再在下一轮重试前重选目标，尽量避开刚刚证明不值得碰的 Broker。

```text
Producer 发送
  → sendKernelImpl()
    → 失败 / 超时 / Broker 异常
      → updateFaultItem()
        → available / reachable / 普通回退 三层重选
          → 新一轮 sendKernelImpl()
```

到这里，主线只发生了五件事。

第一，重试不是“同一请求原样再来”，而是一次新的目标决策。

第二，静态路由视图只能告诉 Producer“哪里存在”，`MQFaultStrategy` 才告诉它“此刻哪里值得碰”。

第三，`updateFaultItem()` 把一次失败变成后续决策可消费的历史记忆。

第四，RocketMQ 会区分不同失败类型，不把所有异常压成同一种故障语义。

第五，真正的发送故障规避能力，不在网络重发本身，而在“下一次别再打错地方”。

**本篇的一句话困惑**：发送失败以后，RocketMQ 为什么不是简单重试一下就行？

**本篇的一句话顿悟**：因为失败最大的价值不是“告诉你这次没发成”，而是“告诉你这个 Broker 此刻不值得再碰”；RocketMQ 先把这次失败记进 `MQFaultStrategy` / `LatencyFaultTolerance`，再重选目标，重试才真正有意义。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“重试就是对同一个 Broker 再发一遍。”** RocketMQ 的重试核心是重选目标。
2. **“队列存在就说明当前可用。”** 静态路由存在不等于运行时健康。
3. **“MQFaultStrategy 只是性能优化。”** 它是发送主链里的运行时故障规避层。
4. **“available 和 reachable 没区别。”** 一个看隔离窗口，一个看探测可达性。
5. **“所有异常都只是 send fail。”** RocketMQ 会根据异常类型更新不同的故障语义。

### 关键证据清单

- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:740`：发送失败后的重试循环入口。
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:760`：每轮失败后重新 `selectOneMessageQueue()`。
- `client/src/main/java/org/apache/rocketmq/client/latency/MQFaultStrategy.java:137`：available → reachable → 普通回退 的重选策略。
- `client/src/main/java/org/apache/rocketmq/client/latency/MQFaultStrategy.java:164`：把发送失败更新成 broker fault item。
- `client/src/main/java/org/apache/rocketmq/client/latency/LatencyFaultToleranceImpl.java:103`：记录当前 latency、隔离时长和 reachable 标记。
- `client/src/main/java/org/apache/rocketmq/client/latency/LatencyFaultToleranceImpl.java:126`：available / reachable 判定边界。
- `client/src/test/java/org/apache/rocketmq/client/producer/selector/SelectMessageQueueRetryTest.java:35`：重试时不会机械回到同一 broker 选择路径。
- `client/src/test/java/org/apache/rocketmq/client/latency/LatencyFaultToleranceImplTest.java:39`：fault item 隔离与可达性测试证据。

### 版本与实现边界

- 本文以 RocketMQ `5.3.1` 为基线。
- 本篇聚焦 Producer 侧发送失败后的目标规避，不展开 Broker 端具体失败来源与服务端重试。
- 本文把 `MQFaultStrategy` 定位为客户端运行时规避层，不把它当作集群控制面的健康真相源。
- 本文不展开 proxy 层的消息队列选择实现，先守住原生 client 主链。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-3` 的路由发现与发送主链。
- 后续桥接：下一篇建议补 `RQ-23：长轮询`，把 Broker 端“为什么请求不会立刻返回”这条运行时链补齐。