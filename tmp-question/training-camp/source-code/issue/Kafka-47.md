# Kafka-47. 容量该怎么算——从 batch、segment、复制放大、fetch 到 quota 的估算路径

> 场景：很多人做 Kafka 容量评估时，公式只有一句：TPS × 单条大小 × 保留时间。这个起点不能说错，但远远不够。Kafka 的真实容量边界，既受 producer 批量聚合影响，也受 broker 的 segment / index / 活跃分区数量影响，还会被 follower 复制流量、consumer fetch 行为和 quota 回压共同拉扯。本篇不做拍脑袋公式，而是把“容量”沿源码主链拆成几层可估算的边界。

## 先把真正的困惑摆出来：容量为什么不能只按“每秒多少消息、每条多大”来算

因为 Kafka 吃掉的不是只有消息 payload。

同样 1 MB/s 的业务数据流：

- 如果 producer 以大 batch 聚合发送，协议、对象、索引、系统调用摊薄得多；
- 如果拆成大量小消息、小批次，broker 的请求处理、索引增长、上下文切换、page cache 压力都会更高；
- 如果副本因子从 1 变 3，磁盘写入与复制带宽会被放大；
- 如果分区数暴涨，活跃 segment、索引文件、fetch 扇出和元数据开销都会放大。

*关键设计（斜体）：* *Kafka 的容量必须沿五层一起估：producer 的批量聚合决定请求与对象摊销；broker 的 append / segment / index 决定单分区存储与活跃文件开销；副本复制决定写入放大；consumer fetch 决定读取扇出与延迟回压；quota 决定系统在超载时如何收缩流量。单看 payload bytes，只能得到一个粗到危险的下界。*[模式: 批量摊销 + 分区/segment 开销 + 复制放大 + 读取回压 + quota 保护]

## 第一层：先看 producer——batch 决定单位消息的摊销成本

容量评估第一步不该从 broker 磁盘开始，而该先问 producer：**你是怎么把消息攒成 batch 的？**

`RecordAccumulator` / `ProducerBatch` 的存在说明 Kafka 不是逐条把消息发给 broker，而是尽量把多条消息聚成批次再发送。

这件事直接影响容量：

- batch 越大，单位消息摊到的请求头、对象管理、网络 syscall 成本越低；
- batch 越小，虽然单条延迟可能更低，但 broker 要处理更多请求、更碎的写入与更多小块索引增长。

所以“同样 bytes/s”并不等于“同样 broker 压力”：**批量聚合度不同，CPU 与 IO 开销可能差很多。**

## 第二层：再看 broker——分区数和活跃 segment 决定常驻开销

broker 并不是只写一个大文件。每个 partition 都会维护：

- `LogSegment`
- `OffsetIndex`
- `TimeIndex`
- 活跃 segment 的内存/文件句柄/页缓存占用

因此容量第二步要问：**有多少分区、每个分区有多少活跃 segment、segment 切换多频繁？**

分区数越多：

- 索引文件越多；
- 活跃文件句柄越多；
- page cache 更碎；
- 请求扇出与调度成本更高。

所以“分区越多越并发”只说对了一面；另一面是它会把 broker 的常驻资源压力持续拉高。

## 第三层：磁盘容量不是只看 payload，要乘复制放大与保留时间

真正落到磁盘容量时，常见的第一版草算式才出现：

```text
业务写入速率 × 保留时间 × 副本因子
```

但必须立刻强调：**这只是一个非常粗的下界草算，不是可靠容量底板。** 它还没有把压缩率、协议/索引开销、控制批次、事务元数据、清理滞后等因素算进去。

另外，副本复制不只放大磁盘，还放大网络与 leader/follower 的追加链路。这里要把两类东西分开：

- **写放大**：leader append + follower 拉取并追加；
- **等待机制**：`DelayedProduce` / `DelayedFetch` 这类是请求等待与回压语义，不等于磁盘写放大本身。

所以副本因子从 1 到 3，不是“多两份盘”这么简单，而是：

- leader 写一次；
- follower 还要拉取并追加；
- 整体网络与磁盘 IO 都被放大。

## 第四层：读容量也要算——fetch 扇出与 minBytes/maxBytes 会影响回压形态

很多容量估算只看写入，但消费侧一样会把 broker 拉满。

`ReplicaManager.readFromLog(...)` 和 `DelayedFetch` 说明 fetch 不是“来一个请求就立刻给一点数据”，而是受以下参数影响：

- `minBytes`
- `maxBytes`
- `maxWaitMs`
- 分区数带来的一次 fetch 扇出规模

消费者很多、每次 fetch 很碎、分区又很多时，broker 的读路径、响应拼装、网络发送都会被拖高。

所以容量评估至少要分成两半：

- **写容量**：producer batch + append + replication
- **读容量**：fetch fanout + bytes returned + response churn

## 第五层：flush / segment / retention 决定的是“写入后怎么存、多久清”

再往后要看日志生命周期：

- `segment.bytes` / roll 频率决定 segment 轮转速度；
- retention 决定旧 segment 保留多久；
- flush 与 recovery point 决定 crash recovery 边界与刷盘行为。

这些参数并不直接创造业务吞吐，但会决定：

- 磁盘空间增长速度；
- 活跃/历史 segment 数量；
- 恢复成本；
- cleaner / retention 删除的背景开销。

因此容量如果只看“每天写多少 G”，却不看 segment 与 retention，最后常常会低估后台存储维护成本。

## 第六层：quota 是容量保护阀，不是“性能不够才开”

当流量逼近极限时，Kafka 不是只有“扛住”或“挂掉”两种状态。`QuotaFactory`、`RequestHandlerHelper`、`ReplicationQuotaManager` 说明它至少准备了多类配额与节流入口：

- `QuotaFactory` 会装配 `fetch` / `produce` / `request` / `controllerMutation` 以及 `leader` / `follower` / `alterLogDirs` 这几类 quota manager；
- `RequestHandlerHelper` 明确承接了 request quota 的记账与 `throttleTimeMs` 计算；
- 复制链路则由专门的 replication quota manager 控制。

所以这层更稳妥的结论不是“所有 quota 都以同一种方式生效”，而是：**Kafka 预先布置了多类节流入口，用来把系统从失控边缘拉回来。**

所以 quota 在容量评估里的角色不是可有可无，而是：**它定义了超载时系统怎么退化。** 没有这层，容量上限只是“理想跑分”；有这层，才是可运营的上限。

## 第七层：把容量估算拆成四个问题，就不会只剩一个大公式

如果按照源码边界拆，容量评估至少应该回答四个问题：

### 1) 每秒进来多少“批”，不是只有多少“条”
- 看 batch 大小、批次数、请求数；
- 决定 producer 与 broker 每秒处理多少 request / append。

### 2) 每个分区维持多少活跃存储结构
- 看 partition 数、segment 数、索引数；
- 决定常驻文件、页缓存、调度开销。

### 3) 每条写入被放大几次
- 看副本因子、acks、follower fetch；
- 决定网络和磁盘写放大。

### 4) 超载时系统怎么收缩
- 看 fetch 行为与 quota；
- 决定延迟变高、吞吐被压，还是直接雪崩。

只要这四问没回答，所谓“容量评估”往往只是一张磁盘采购表。

## 收网：Kafka 容量是多层放大与摊销的平衡，不是单公式

把整篇压成一句话：Kafka 容量不能只按 payload bytes 估，而要同时看 producer 侧 batch 摊销、broker 侧 partition/segment/index 常驻开销、副本复制导致的网络与磁盘放大、consumer fetch 带来的读路径压力，以及多类 quota 在超载时提供的保护与退化方式；就连最常见的“写入速率 × 保留时间 × 副本因子”也只能算一个很粗的下界起点，只有把这些层一起算，容量结果才接近真实可运营边界。

```text
容量评估
  → producer：RecordAccumulator / ProducerBatch
    → broker：LogSegment / OffsetIndex / TimeIndex
      → replication：ReplicaManager / follower fetch
        → consumer：readFromLog / DelayedFetch
          → overload protection：QuotaFactory / RequestHandlerHelper
```

**本篇的一句话困惑**：Kafka 容量到底该怎么算，为什么“TPS × 单条大小 × 保留时间”总让人算着算着就失真？

**本篇的一句话顿悟**：Kafka 容量是“批量摊销、分区常驻开销、复制放大、读取扇出、quota 退化策略”这几层共同作用的结果；只看 payload bytes，你算出来的只是一个危险的下界。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“容量评估只看磁盘总量。”** CPU、请求数、分区常驻结构、复制与读路径同样关键。
2. **“同样 bytes/s，broker 压力就差不多。”** batch 聚合度不同，单位消息开销可能差很多。
3. **“分区越多越好。”** 它会持续放大 segment、索引、page cache 与调度成本。
4. **“副本因子只增加可靠性，不增加容量消耗。”** 它同时放大磁盘、网络与复制 append 成本。
5. **“quota 只是限客户端，不属于容量评估。”** quota 定义了系统超载时如何退化。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/producer/internals/RecordAccumulator.java`：producer 批量聚合入口。
- `clients/src/main/java/org/apache/kafka/clients/producer/internals/ProducerBatch.java`：批次对象。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/LogSegment.java`：segment 存储单元。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/OffsetIndex.java`：offset 索引。
- `storage/src/main/java/org/apache/kafka/storage/internals/log/TimeIndex.java`：时间索引。
- `core/src/main/scala/kafka/server/ReplicaManager.scala:674`：append 主入口。
- `core/src/main/scala/kafka/server/ReplicaManager.scala:1726`：`readFromLog(...)`。
- `core/src/main/scala/kafka/server/DelayedFetch.scala:77`：fetch 等待条件。
- `core/src/main/java/kafka/server/QuotaFactory.java:54`：quota manager 组成。
- `core/src/main/scala/kafka/server/RequestHandlerHelper.scala:111`：请求节流时间计算。

### 版本与实现边界

- 本文以 Kafka `v4.x` 为基线。
- 本篇是源码 + 运维桥接篇：讲的是估算路径，不替代真实压测与容量基线。
- 不把经验参数写成绝对结论。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-15/16`（producer 批量）、`Kafka-17/18`（segment/index）、`Kafka-44`（quota）。
- 后续桥接：可继续补“限流生效后的性能退化路径”或“启动恢复时的容量冲击”。