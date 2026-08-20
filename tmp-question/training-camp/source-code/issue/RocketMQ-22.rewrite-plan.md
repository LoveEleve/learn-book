# RocketMQ-22 重写规划

> 题目：发送失败以后，RocketMQ 为什么不是“重试一下就行”——MQFaultStrategy 与故障规避主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：补深 Producer 发送主链中的“失败以后怎样重选目标与规避故障节点”，把 `MQFaultStrategy`、`LatencyFaultTolerance`、`selectOneMessageQueue()` 与 `sendKernelImpl()` 的重试链收成一个独立闭环，而不是把发送失败看成简单 for-loop 重试。

## 1. 读者困惑

- RocketMQ 发送失败以后，为什么不能简单对同一个 Broker 再试一下？
- `MQFaultStrategy` 到底补的是哪层缺口：重试、选路，还是故障规避？
- Producer 怎么区分“节点暂时慢”“节点不可达”“只是这一轮超时”？
- `sendLatencyFaultEnable` 开关一开，发送目标为什么就不再是“从队列列表随便挑一个”？
- `sendKernelImpl()` 重试链与 `MQFaultStrategy.selectOneMessageQueue()` 的关系到底是什么？

## 2. 一句话顿悟

**RocketMQ 发送失败后的关键不在“再发一次”，而在“别再打到刚刚已经证明不值得碰的节点上”；`MQFaultStrategy` 先把 Broker 按可用/可达与隔离时间重新分层，再让 `sendKernelImpl()` 的重试真正变成一次新的目标决策，而不是对同一故障节点的盲撞。**

## 3. 五要素卡片

### 读者问题

为什么 Producer 发送失败后不能把“重试”理解成简单循环，而必须引入 `MQFaultStrategy` 和 `LatencyFaultTolerance`？

### 入口

- `DefaultMQProducerImpl.sendDefaultImpl()`：同步发送重试总入口
- `DefaultMQProducerImpl.selectOneMessageQueue()`：每次重试前重新选目标
- `MQFaultStrategy.selectOneMessageQueue()`：按可用/可达/上次 Broker 过滤选队列
- `MQFaultStrategy.updateFaultItem()` / `LatencyFaultToleranceImpl.updateFaultItem()`：记录 broker 故障与隔离期

### 状态核心

- `sendLatencyFaultEnable` / `startDetectorEnable`
- `latencyMax[]` / `notAvailableDuration[]`
- Broker 的 `isAvailable` / `isReachable`
- `lastBrokerName` 与 `BrokerFilter`
- `timesTotal` / `resetIndex` / `brokersSent[]`
- 不同异常类型对应的 faultItem 更新方式

### 失败路径

- 同一故障 Broker 上盲目重试：连续把请求打进已知坏点
- 把“队列存在”误当成“当前可用”：静态路由视图不能代表运行时健康度
- Remoting 异常与 Broker 响应异常不区分：会用错隔离/可达语义
- 所有失败都立刻换随机 Broker：丢掉 latency fault 的记忆与避障能力
- 不启用 latency fault：重试仍可能持续回到刚失败过的 Broker

### 连接点

- 前文 `RocketMQ-3`：已讲路由发现与基本发送主链，本篇补“失败以后怎样重新选目标”
- 前文 `RocketMQ-14/15`：Broker 不可用背后可能来自数据面或控制面异常，但本篇只看 Producer 侧如何规避
- 后续可与长轮询补深篇并列，形成 Broker 宿主/Producer 失败侧的运行时补层

## 4. 总图

```text
Producer send
  → 选一个 MessageQueue
    → sendKernelImpl()
      → 如果失败
        → updateFaultItem(记录当前 Broker 故障/隔离)
          → MQFaultStrategy 再选下一轮 queue
            → 优先可用 broker
              → 退化到可达 broker
                → 最后才回到普通选择
```

## 5. 关键边界

- 本篇只讲 Producer 侧发送失败后的目标规避与重试，不回头重讲 NameServer 路由发现。
- 不把 `MQFaultStrategy` 写成“简单负载均衡器”；它是运行时故障规避层。
- 不把 latency fault 理解成强一致健康检查；它是 Producer 侧经验性隔离与探测机制。
- 不展开 proxy 层的 MQFaultStrategy 适配，只聚焦 client 主链。

## 6. 失败方案推演

1. **失败后直接重试同一个 Broker**：最直觉，但会把已知故障点连续打爆。
2. **每次随机换 Broker**：能避开部分故障，但没有记忆，不知道哪些 Broker 正处于隔离期。
3. **只看静态队列列表，不看运行时可达性**：路由存在不等于当前能发。
4. **把所有异常都当成一种失败**：无法区分“暂时不可达”和“Broker 侧响应错误”，隔离策略会失真。

## 7. 误解清单

- 重试不是“同一请求再发一次”，而是一次新的目标决策。
- `MQFaultStrategy` 不只是性能优化，而是发送主链的一部分。
- 可用（available）和可达（reachable）不是同一回事。
- 关闭 latency fault 后，RocketMQ 并不会自动聪明避开刚失败的 Broker。
- `sendKernelImpl()` 不是独立决定重试策略，它消费的是前面重新选出来的目标。

## 8. 证据清单

- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:740`
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:760`
- `client/src/main/java/org/apache/rocketmq/client/latency/MQFaultStrategy.java:137`
- `client/src/main/java/org/apache/rocketmq/client/latency/MQFaultStrategy.java:164`
- `client/src/main/java/org/apache/rocketmq/client/latency/LatencyFaultToleranceImpl.java:103`
- `client/src/main/java/org/apache/rocketmq/client/latency/LatencyFaultToleranceImpl.java:126`
- `client/src/test/java/org/apache/rocketmq/client/producer/selector/SelectMessageQueueRetryTest.java:35`
- `client/src/test/java/org/apache/rocketmq/client/latency/LatencyFaultToleranceImplTest.java:39`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇聚焦客户端发送重试/规避；不展开 Broker 端具体失败原因与服务端重试语义。
- 目标正文：7000~11000 字；核心拆解层覆盖盲重试为什么不行、fault item 怎样形成、重试时怎样再选队列、异常类型如何影响规避。

## 10. 本轮重写主线

1. 从“失败后为什么不是直接再试一下”开场。
2. 否定同 Broker 重试、随机换 Broker、只看静态队列列表三种朴素方案。
3. 解释 `sendDefaultImpl()` 怎样在每轮失败后更新 fault item 并重新选队列。
4. 解释 `MQFaultStrategy` 的三层选择：available → reachable → 普通回退。
5. 解释 `LatencyFaultToleranceImpl` 怎样记录隔离窗口与可达性。
6. 收网：发送失败真正难的不是多发一次，而是下一次别再打错地方。