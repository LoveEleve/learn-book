# RocketMQ-29. Consumer 架构为什么不是“拉到就消费”——消费者总览

> 场景：RocketMQ 的消费者很容易被理解成一句话：Broker 把消息给我，我处理完，提交一下 offset，就结束了。但只要深入一点，就会发现这套理解几乎把关键结构全漏掉了：Push 其实不是 broker 真推，消费前有 Rebalance，消息到了客户端以后先进入 `ProcessQueue`，再由消费线程池异步处理，最后还要配合消费进度提交、顺序/并发两条分叉主链。本篇把 Consumer 体系重新拉成一张总图。

## 先把真正的困惑摆出来：Consumer 为什么不是“拿到消息然后执行回调”这么简单

如果只看业务代码，消费者好像就是：

- 注册一个 listener；
- 收到消息；
- 执行消费逻辑；
- 成功后提交进度。

但 RocketMQ 客户端内部真正运行的东西远比这复杂：

- Topic 下哪些队列归你，不是天然固定，而要经过 `Rebalance`；
- 拉到的消息不是立刻丢给 listener，而是先进 `ProcessQueue`；
- `PullMessageService`、消费线程池、offset 提交并不是一条线程顺手做完；
- 顺序消费和并发消费在运行时约束上也完全不同。

*关键设计（斜体）：* *RocketMQ Consumer 的本质不是“收到消息”，而是一套运行时架构：先通过 Rebalance 决定自己持有哪些 `MessageQueue`，再由拉取线程把消息从 Broker 拉回本地并放入 `ProcessQueue` 快照，随后由消费线程池按并发或顺序语义处理，最后再推动消费进度提交。Push、LitePull、顺序、并发，只是建立在这套骨架之上的不同消费表面。*[模式: Rebalance 领队列 + Pull 拉消息 + ProcessQueue 本地快照 + 消费线程调度 + Offset 提交]

## 第一层：Push 不是 broker 真推，底层仍然是 pull

这是 RocketMQ Consumer 最容易讲错的第一句。

`DefaultMQPushConsumer` 虽然对业务暴露的是“像推一样”的接口，但底层并不是 Broker 主动把消息推过来，而是客户端自己通过拉取链路不断从 Broker 取消息。

所以 Push 的真实语义更接近：

- **接口像 push**：业务代码被动接 listener 回调；
- **底层仍是 pull**：客户端内部持续拉消息。

这也是为什么 `PullMessageService` 是 Consumer 架构骨架之一，而不是一个边角类。

## 第二层：消费开始前，先要决定“哪些队列归我”

Consumer 不是一启动就随便从某个 Topic 拉消息，而是要先经过 `Rebalance`。

`RebalanceImpl` 的职责，不是简单“平均一下”，而是决定：

- 当前消费组下有哪些 `MessageQueue`；
- 其中哪些应该分配给当前 consumer 实例；
- 旧队列是否需要释放，新队列是否需要接管。

也就是说，消费主链真正的起点不是“收到消息”，而是：

```text
消费组成员变化 / 路由变化
  → Rebalance
    → 当前实例持有哪些 MessageQueue
      → 才有后续 pull 与消费
```

## 第三层：`ProcessQueue` 不是 Broker 队列，而是客户端本地快照

这也是最容易被名字误导的结构。

`ProcessQueue` 不是 Broker 侧那条真实的 topic queue，也不是 CommitLog/ConsumeQueue 那种服务端存储结构。它是**客户端本地对某个分配到手的 `MessageQueue` 的消费快照与运行时缓存**。

它承接的是：

- 从 Broker 拉回来的消息临时驻留；
- 当前消费进度附近的消息状态；
- 顺序/并发消费时的本地运行约束。

所以 Consumer 不是“从 Broker 拿一条，立刻回调一条”，而是：

- Broker 侧真实 `MessageQueue`
- 客户端侧 `ProcessQueue` 本地快照
- 再从快照进入消费线程池

这三层必须分开。

## 第四层：`PullMessageService` 负责拉，消费线程池负责算，它们不是一回事

当某个 `MessageQueue` 被当前 consumer 实例持有后，客户端会沿着完整的 pull 闭环继续拉消息；`PullMessageService` 是这个闭环里的调度骨架之一，但真正的拉取还会和 `DefaultMQPushConsumerImpl`、pull request 调度、网络请求封装等环节配合完成。

但拉回来以后，并不是由拉取线程直接顺手执行业务消费逻辑。RocketMQ 会把消息进一步交给消费服务：

- 并发消费路径：更偏向批量/并发投递给消费线程池；
- 顺序消费路径：则要在 queue 粒度上加更强的串行约束。

所以这里至少有两类不同职责：

- **拉取职责**：把消息从远端拿回来；
- **消费职责**：在本地线程池里执行 listener 并推进状态。

这也是为什么“Consumer 就是收到了就调回调”会错：中间还有完整的本地调度层。

## 第五层：并发消费和顺序消费不是“换个回调函数”，而是两套运行约束

顺序消费与并发消费的区别，不只是 listener 接口长得不一样。

更深层的差异在于：

- 并发消费下，同一 `MessageQueue` 拉回来的消息也可以按批次更积极地并发推进；
- 顺序消费则要求围绕同一 `MessageQueue` 的推进保持更强的串行性与锁语义；
- 失败后的重试、挂起、重新投递策略也不同。

所以它们虽然都建立在“Rebalance → Pull → ProcessQueue → 消费服务”这套总骨架上，但到了消费执行层，实际上是**两种不同的运行时约束模型**。

## 第六层：Offset 提交不是最后顺手写一下，而是消费主链闭环的一部分

消费者真正完成，不是 listener 返回就结束，还要看消费进度如何推进。

因为消费进度（offset）决定的是：

- 下次从哪里继续消费；
- Rebalance 后其他实例接手时从哪里开始；
- 失败重试与消费一致性边界如何落定。

所以 offset 提交不是附属动作，而是 Consumer 架构闭环的一部分：**没有进度推进，前面的 pull、缓存、消费线程调度都无法收敛成稳定行为。**

## 第七层：把 Consumer 总图压成一句话，就是“先领队列，再拉，再本地消费，再提交进度”

如果把整个架构压缩，RocketMQ Consumer 主链可以概括成：

1. `Rebalance` 先决定当前实例持有哪些 `MessageQueue`；
2. `PullMessageService` 持续从这些 queue 对应的 Broker 拉消息；
3. 消息先进入客户端本地 `ProcessQueue`；
4. 再由消费服务按并发或顺序语义调度到业务 listener；
5. 消费结果再推动 offset 进度前移。

这里也不要把它理解成“所有模型都是 listener 一返回就立刻统一提交”。更准确地说：offset 的推进时机与方式，会随着并发/顺序模型以及成功、失败、挂起等结果分支而变化。

这几步缺任何一步，Consumer 都讲不完整。

## 收网：RocketMQ Consumer 是一套运行时架构，不是一个回调接口

把整篇压成一句话：RocketMQ Consumer 不是“Broker 把消息给我，我处理完就行”，而是一套由 `Rebalance`、`PullMessageService`、`ProcessQueue`、消费线程池与 offset 提交共同组成的运行时架构；Push 只是对业务呈现为 push 风格，底层仍然是 pull，而顺序与并发消费则是在这套骨架上叠加不同运行约束的两条分支。

```text
消费组变化 / 路由变化
  → Rebalance 领到 MessageQueue
    → PullMessageService 拉消息
      → ProcessQueue 本地快照
        → ConsumeMessageService / listener
          → offset 提交
            → 下一轮继续消费
```

**本篇的一句话困惑**：RocketMQ Consumer 为什么不是“拉到消息就执行回调”这么简单？

**本篇的一句话顿悟**：RocketMQ Consumer 的本质是一套运行时架构：先领队列，再拉消息，再进入本地 `ProcessQueue`，再按并发/顺序语义交给消费线程池，最后以 offset 提交完成闭环；Push 只是表面接口，底层仍是 pull。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Push = Broker 主动推消息。”** Push 的表面接口是被动回调，底层仍是 pull。
2. **“ProcessQueue = Broker 上的真实队列。”** 它是客户端本地快照与运行时缓存。
3. **“Rebalance 分配一次就结束。”** 消费组成员和路由变化时会持续发生。
4. **“消费线程池只是 listener 的包装。”** 它承接并发/顺序不同运行约束。
5. **“offset 提交只是最后顺手记一下。”** 它是消费主链闭环的重要一环。

### 关键证据清单

- `client/src/main/java/org/apache/rocketmq/client/consumer/DefaultMQPushConsumer.java`：Push 消费者外观。
- `client/src/main/java/org/apache/rocketmq/client/consumer/DefaultLitePullConsumer.java`：LitePull 外观。
- `client/src/main/java/org/apache/rocketmq/client/impl/consumer/DefaultMQPushConsumerImpl.java`：Push 实际实现宿主。
- `client/src/main/java/org/apache/rocketmq/client/impl/consumer/PullMessageService.java`：拉取线程服务。
- `client/src/main/java/org/apache/rocketmq/client/impl/consumer/RebalanceImpl.java`：Rebalance 主链。
- `client/src/main/java/org/apache/rocketmq/client/impl/consumer/ProcessQueue.java`：本地消费快照。
- `client/src/main/java/org/apache/rocketmq/client/impl/consumer/ConsumeMessageConcurrentlyService.java`：并发消费服务分支。
- `client/src/main/java/org/apache/rocketmq/client/impl/consumer/ConsumeMessageOrderlyService.java`：顺序消费服务分支。

### 版本与实现边界

- 本文以 RocketMQ `5.x` / `4.x` 通用主链为基线。
- 本篇是消费者总览，不替代 `RocketMQ-6/7/8/9/23` 的细节篇。
- 不把 Push/LitePull/顺序/并发说成彼此孤立的四套平行系统。

### 前置依赖与后续桥接

- 前置依赖：`RocketMQ-6`（拉取/ProcessQueue/消费推进）、`RocketMQ-7`（Push/LitePull/Rebalance）、`RocketMQ-8/9`（顺序语义）、`RocketMQ-23`（长轮询）。
- 后续桥接：可继续补 `Proxy/Pop` 体系，或补 RocketMQ 4.x vs 5.x Consumer 架构对照。