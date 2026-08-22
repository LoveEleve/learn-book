# RocketMQ-34. 为什么 RocketMQ 性能不如 Kafka——存储、路由、消费模型对照桥接篇

> 场景：这是一个特别容易滑向口水战的问题。有人会说 RocketMQ 慢，是因为实现没 Kafka 极致；也有人会说 Kafka 快，只是因为它更简单。这两句话都只摸到了皮毛。真正要把这个问题讲透，必须把我们前面已经拆开的两套主链重新对照：Kafka 的高吞吐到底建立在哪些极短主链之上，RocketMQ 又在哪些地方承担了更多路由、索引、确认与恢复语义。只有这样，性能差异才不会被讲成一句“谁更强”。

## 先把真正的困惑摆出来：RocketMQ 的“慢”到底慢在哪

如果只看表面，RocketMQ 和 Kafka 都做消息收发、持久化、消费、主从/副本。

于是很多人会本能地问：

- 为什么 Kafka 常被视为更高吞吐？
- RocketMQ 到底是哪里更重？
- 这些差异是纯实现问题，还是架构取舍问题？

真正难的地方在于：**你不能脱离语义去比较主链长度。**

因为很多时候，RocketMQ 承担的不是“更低效的同一种工作”，而是“更丰富的一套工作”。

*关键设计（斜体）：* *Kafka 与 RocketMQ 的性能差异，不能简化成谁的代码更好，而更应看成两种架构取舍：Kafka 把系统极度压缩到顺序追加、批量聚合、较短消费确认主链上，因此天然更接近极简高吞吐；RocketMQ 则在 Topic/Queue 路由、ConsumeQueue/IndexFile、延迟/事务/Pop 确认模型、以及 5.x 的 Proxy 接入层上，承担了更多结构性成本。吞吐差异，本质上是“更短主链”与“更丰富语义”之间的代价交换。*[模式: Kafka 极短主链 + RocketMQ 更丰富语义 + 性能差异来自结构成本]

## 第一层：Kafka 的天然优势，是把写入主链压得非常短

Kafka 的核心写入心智可以压缩成：

- Producer 侧强 batch 聚合；
- Broker 侧顺序追加到日志段；
- 消费侧再通过 offset 顺序推进；
- 很多高吞吐场景下，主链围绕“追加日志 + 批量 fetch”展开。

也就是说，Kafka 特别像一个围绕**批量聚合 + 顺序日志**精细打磨的系统。

这使它的优势很自然：

- 单位消息协议摊销更低；
- 存储主链更纯；
- 消费确认主链通常更短；
- 很多语义不需要额外结构就能成立。

所以 Kafka 的快，不是偶然，是它在设计上就极度偏向“最短吞吐主链”。

## 第二层：RocketMQ 的写入不是只有 CommitLog，它从一开始就接受了更多派生结构

RocketMQ 虽然也有高性能顺序追加的 `CommitLog`，但它并没有停在“写入一条统一日志”这一步。

它还要继续构建：

- `ConsumeQueue`：消费索引桥；
- `IndexFile`：键查询入口；
- 延迟消息派生路径；
- 事务消息的半消息与转正路径。

这意味着 RocketMQ 即使主写入落点仍然很快，但它的整体存储系统天然就比 Kafka 承担了**更多派生结构维护成本**。

所以两者并不是：

- Kafka 有日志；
- RocketMQ 也有日志；
- 看谁日志写得快。

而是：**Kafka 更接近“日志本体就是系统核心”，RocketMQ 更接近“日志本体 + 多种消费/查询/语义派生结构一起工作”。**

## 第三层：路由模型也更重——RocketMQ 不是“Topic 到 Broker”这么简单

Kafka 的 Topic/Partition 路由当然也不轻，但 RocketMQ 的 Topic/Queue 模型带来了另一种结构成本：

- Topic 被拆成多个 `MessageQueue`；
- NameServer 返回 `TopicRouteData`，再展开 `QueueData` / `BrokerData`；
- Producer 还要在 `TopicPublishInfo` 上选择具体 queue；
- 顺序消息、故障规避、重试路径都会进一步影响 queue 选择。

这说明 RocketMQ 的发送路由天然更像“**先拿一张路由视图，再在多个 queue 分片里做更细粒度的选择**”。

这不是说它设计不好，而是它在发送前就承担了比“选个 partition 继续写”更多的结构性决策。

## 第四层：RocketMQ 的消费模型比 Kafka 更容易把确认语义拉长

Kafka 的消费主链虽然也绝不只是“简单三步”——它同样有 rebalance、fetch session、事务/`read_committed`、Purgatory 等复杂度——但在很多追求极致吞吐的主流场景下，它的消费确认主链往往更接近：

- 拉取；
- 处理；
- 推进 offset。

而 RocketMQ 尤其到了 5.x，消费模型会更重：

- 先有 Rebalance、ProcessQueue、本地调度；
- 再有顺序/并发两套运行约束；
- 再到 Pop 里，还会引入 ack、ck、invisibility、revive、死信。

这意味着 RocketMQ 的“消费成功”比 Kafka 更容易携带**服务端确认模型和恢复闭环成本**。

所以你不能一边要 Pop 这种更强消费语义，一边还要求它在每条主链长度上完全与 Kafka 持平。

## 第五层：5.x 的 Proxy 把接入复杂度显式化了，也会带来额外成本

这里也要注意比较范围：Proxy 是 RocketMQ 5.x 的显式接入层因素，不应被反推成“所有 RocketMQ 版本的一般性能结论”。

Kafka 的高吞吐比较里，常见默认前提是客户端直接与 Broker 交互。

而 RocketMQ 5.x 把 Proxy 推到前台以后，收益是：

- 客户端接入更统一；
- Pop/消费语义更容易服务端化；
- 协议适配与接入状态更清晰。

但代价也是显式的：

- 更多接入层处理链；
- 额外的协议/状态宿主；
- 更多服务端编排成本。

所以 5.x 的一些能力，不是“白拿的功能增强”，而是**用额外结构成本换来更清楚的接入边界与语义能力。**

## 第六层：为什么不能脱离语义只看裸吞吐 benchmark

这是最关键的一层。

如果只拿“每秒写多少条消息”比较，很容易得出简单结论：Kafka 更快，RocketMQ 更慢。

但这类 benchmark 往往默认忽略了：

- 是否需要键查询；
- 是否需要多种消费模型；
- 是否需要延迟/事务路径；
- 是否需要更重的服务端确认模型；
- 是否要让接入层和路由层承担更多职责。

所以真正严谨的对比，不是“谁吞吐高就谁更先进”，而是：

> **在比较目标语义相近的前提下，谁把系统压缩得更接近极简主链，谁就更容易跑出极致吞吐；谁承担更多附加语义，谁就天然会付出结构成本。**

## 第七层：这不是在替 RocketMQ 找借口，而是在还原它的设计目标

说 RocketMQ 性能通常不如 Kafka，不意味着 RocketMQ “做坏了”；
说 Kafka 高吞吐，也不意味着它在所有语义维度都天然占优。

更准确的理解应该是：

- Kafka 特别擅长把“批量追加 + 批量消费”这条链做到极短；
- RocketMQ 更愿意在系统内部显式承载业务中间件世界常见的丰富语义：
  - 队列分片
  - 消费模型差异
  - 延迟 / 事务
  - 服务端确认闭环
  - 接入层收口

所以这里不是“快/慢”这么简单，而是：**设计目标从一开始就不完全一样。**

## 收网：RocketMQ 性能通常不如 Kafka，本质上是更丰富语义换来的结构成本

把整篇压成一句话：Kafka 常被认为性能更强，核心不只是实现细节，而是它把系统长期压在“批量聚合 + 顺序追加 + 较短确认主链”这条极简吞吐路径上；RocketMQ 虽然也有高性能 CommitLog，但同时承担了 Topic/Queue 路由、ConsumeQueue/IndexFile、延迟/事务、Pop 服务端确认模型以及 5.x Proxy 接入层等更多结构性语义，因此整体更容易付出主链更长、状态更多、维护面更广的成本。RocketMQ 的“慢”，很多时候不是低效，而是**它愿意用额外复杂度换取更多显式能力。**

```text
Kafka
  → batch 聚合
  → 顺序追加日志
  → 较短消费确认主链
  → 更容易逼近极限吞吐

RocketMQ
  → CommitLog + ConsumeQueue + IndexFile
  → Topic/Queue 路由选择
  → 延迟/事务/Pop 确认闭环
  → Proxy 接入层
  → 付出更多结构成本
```

**本篇的一句话困惑**：为什么 RocketMQ 常被认为性能不如 Kafka，这到底是实现问题还是架构问题？

**本篇的一句话顿悟**：更接近真相的答案是：Kafka 把系统压得更短，RocketMQ 承担的语义更多；两者性能差异，本质上是“极简吞吐主链”与“更丰富中间件语义”之间的架构取舍。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“RocketMQ 慢只是实现差。”** 更重要的是它承担了更多结构性语义成本。
2. **“Kafka 快只是因为代码写得更好。”** 它的架构主链本身就更偏极简吞吐。
3. **“功能更丰富但性能还应该无损持平。”** 更强语义通常对应更长主链和更多状态。
4. **“只看写入 benchmark 就能下结论。”** 还要看消费、确认、查询、延迟、事务等维度。
5. **“RocketMQ 与 Kafka 只是同类产品的直线优劣比较。”** 它们的设计目标并不完全重合。

### 关键证据清单

- `book/成长之路/tmp-question/training-camp/source-code/issue/Kafka-15.md:1`：Kafka Producer 批量聚合。
- `book/成长之路/tmp-question/training-camp/source-code/issue/Kafka-17.md:1`：Kafka Log/Segment。
- `book/成长之路/tmp-question/training-camp/source-code/issue/Kafka-36.md:1`：Kafka Purgatory / 等待机制。
- `book/成长之路/tmp-question/training-camp/source-code/issue/Kafka-47.md:1`：Kafka 容量与结构成本。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java`：Kafka 批量聚合入口。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogSegment.java`：Kafka 顺序日志段。
- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-28.md:1`：RocketMQ 存储总览。
- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-29.md:1`：RocketMQ Consumer 总览。
- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-31.md:1`：RocketMQ Pop 总览。
- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-32.md:1`：RocketMQ Pop 确认与恢复闭环。
- `book/成长之路/tmp-question/training-camp/source-code/issue/RocketMQ-33.md:1`：RocketMQ 4.x vs 5.x 架构对照。
- `store/src/main/java/org/apache/rocketmq/store/CommitLog.java`：RocketMQ 主写入链。
- `store/src/main/java/org/apache/rocketmq/store/ConsumeQueue.java`：RocketMQ 消费索引桥。

### 版本与实现边界

- Kafka 以 `v4.x` KRaft 主链为主。
- RocketMQ 以 `4.x / 5.x` 主链为主。
- 本篇是桥接对照，不是严格压测复现实验报告。
- 本篇重点是结构成本对照，不做“谁绝对更好”的价值判断。

### 前置依赖与后续桥接

- 前置依赖：建议先读 `Kafka-15/17/36/47` 与 `RocketMQ-28/29/31/32/33`。
- 后续桥接：如果继续，可补 RocketMQ Metrics、TieredStore、BrokerContainer、Slave Acting Master 等 5.x 能力篇。