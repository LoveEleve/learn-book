# 消息驱动架构 — 秒杀洪峰如何削峰、解耦并最终收敛

> Cluster B: 12 KPs | 依赖: 04-caching-strategy | 读者基线: Producer/Consumer/Topic/Partition 基础
> 读者处境: 04 篇解决了读缓存和热点问题；本篇面对写洪峰：订单、库存、积分、通知如何从同步调用拆成消息事件，并让数据库不被瞬时流量击穿
> 打开新视角: MQ 不是“把请求藏起来”，而是**把瞬时入口速率和下游处理速率解耦**；代价是重复、顺序、回溯、积压和最终一致性

---

### 概念依赖链

```
04 caching/热点/最终一致 → 本篇: MQ削峰与事件驱动
  ├─ §1 同步洪峰(线程/DB饱和)
  ├─ §2 Kafka(分区日志/offset/复制)
  ├─ §3 Kafka/RocketMQ/RabbitMQ(设计取舍)
  ├─ §4 事件驱动(发布/订阅/顺序/幂等)
  └─ §5 秒杀容量与故障边界
先讲: 洪峰 → 日志/队列结构 → 引擎选择 → 事件架构 → 容量闭环
后续依赖: 06-storage-architecture(分库分表/读写分离)
```

### 叙事顺序

1. 问题引入——10 万请求/秒同时下单，同步链为什么会把线程池和数据库一起拖垮？
2. MQ 削峰——时间换稳定
3. Kafka——分区日志和消费者 offset
4. Kafka/RocketMQ/RabbitMQ——三种设计哲学
5. 事件驱动——服务如何通过事件解耦
6. 收束——消息系统的收益与新债务

### 1. 同步洪峰 — 下游处理速率决定整条调用链

场景提示: 下单流程同步调用库存、积分、支付、通知，任一环节变慢时为什么会出现线程池耗尽和级联雪崩？ [写作时展开]

关键设计: 同步调用把所有下游延迟和失败直接叠加到入口请求：

```[pseudocode]
用户请求
  → Order
  → Inventory
  → Point
  → Notify

同步:
  每一步等待下一步返回
  → 并发请求占用线程/连接
  → 下游变慢 → 上游排队 → 超时/重试放大

消息化:
  Order 写入事件/队列
  → Consumer 按自身并发和数据库能力处理
  → 入口与下游速率解耦
```

Why: 为什么 MQ 削峰不是“让订单更快完成”？——**它把瞬时压力变成队列积压和可控等待，目标是保护下游不崩，而不是消除处理时间**；用户体验需要状态查询、排队提示、超时和取消语义。容量规划必须用实际生产/消费速率，不能固定套“10 万进、1000 出、100 秒”数字。 [系统性能: MQ 引入队列后，等待从线程池转移到消息积压，需监控 lag/backlog]

比喻锚点: MQ 像水库：暴雨先蓄水，水电站按稳定流量放水；它避免洪峰冲垮下游，但水库水位会变高。 [写作时展开]

### 2. Kafka — 分区日志、offset 与复制

场景提示: Kafka 为什么更像“可回放的分布式日志”，而不是一个取出即消失的队列？ [写作时展开]

关键设计: Topic 分成 Partition，Producer 追加日志，Consumer 用 offset 管理读取进度：

```[pseudocode]
Topic
  → Partition
  → append-only log segments
      .log + index/time index

Producer:
  → 选择 partition(key/partitioner)
  → append event

Consumer group:
  → 每个 partition 同一时刻由一个 consumer 处理
  → 维护 offset
  → 可提交、重放、回退或从指定位置读取

可靠性:
  replication/acks/min.insync 等配置共同决定确认边界
```

Why: 为什么 Kafka 的“分区内有序”不等于 Topic 全局有序？——**每个 partition 是独立日志，跨 partition 没有天然全序**；partition key 选择同时影响顺序、热点与并行度。Kafka 的吞吐、保留、Page Cache、复制和消费延迟也不能用固定 MB/s 断言。 [消息系统: offset 是消费进度，不是业务处理成功的自动证明]

比喻锚点: Kafka 像多个并行账本，每本账本内部有序、可翻页重读；不同账本之间没有一个免费全局页码。 [写作时展开]

### 3. Kafka、RocketMQ、RabbitMQ — 不是简单性能排名

场景提示: 团队从 RabbitMQ 换 Kafka 后发现路由、顺序和消费语义都不同，为什么？ [写作时展开]

关键设计: 三类消息系统的核心抽象不同：

```[pseudocode]
Kafka:
  partition log + consumer pull + offset replay
  → 高吞吐/流处理/事件日志

RabbitMQ:
  exchange + queue + binding + push/ack
  → 灵活路由/短消息/队列语义

RocketMQ:
  commit log + consume queue
  → 事务消息/延迟消息/顺序与生态能力

选择:
  业务路由、重放、吞吐、延迟、事务消息、运维生态
  → 逐项比较, 不用单一“快”排序
```

Why: 为什么 Push vs Pull 不是“谁更先进”的问题？——**Push 让 broker 控制发送并可快速交付，Pull 让 consumer 控制读取速率和回溯；两者把背压、批量和消费节奏放在不同位置**。事务消息、顺序、重试和死信也必须按具体产品验证。 [分布式理论: 消息系统选择属于一致性、背压、回放和运维能力的联合取舍]

比喻锚点: RabbitMQ 像快递员把包裹主动送到门口，Kafka 像收件人按账本页码自己来取，RocketMQ 则在账本上额外提供预约、延迟和事务凭证。 [写作时展开]

### 4. 事件驱动 — 服务从“调用谁”变成“谁关心这个事件”

场景提示: Order 服务创建订单后，库存、积分、通知服务怎样独立演进，又怎样保证重复消息不造成重复扣减？ [写作时展开]

关键设计: 发布/订阅通过事件 Schema 解耦生产者和消费者，但把一致性和版本责任转移到事件层：

```[pseudocode]
OrderCreated(order_id, version)
  → Inventory consumer
  → Point consumer
  → Notify consumer

每个消费者:
  读取 event
  → 幂等键/消费记录
  → 本地事务更新
  → commit offset at correct boundary

顺序:
  同一业务 key → 同一 partition
  → 分区内保持该 key 的事件顺序
```

Why: 为什么消息队列不能自动保证事件驱动最终一致？——**消息至少一次意味着消费者必须幂等，offset 提交和业务本地事务之间仍有失败窗口**；事件 Schema 也会演进，顺序只在定义的 key/partition 范围内成立。事件驱动把耦合从同步调用转移到数据契约、重试和补偿。 [分布式事务: 本地消息表/事务消息与消费者幂等是事件驱动可靠性的基础]

### 5. 收束

秒杀写洪峰链路：

```[pseudocode]
入口限流/校验
  → Order event append
  → consumer group 按可承载速率消费
  → DB 批量/本地事务处理
  → offset 在业务成功边界提交
  → 重试/死信/对账处理异常
```

**Aha Moment**: "MQ 的价值不是让 10 万请求瞬间完成，而是**把入口洪峰、下游处理、重试和最终一致性拆成可观测的阶段**；你用稳定性换取了队列积压、重复消费和回放管理的新责任。"
**回答读者三问**: ①MQ 为什么能削峰=解耦入口和消费速率；②Kafka 为什么能回放=日志保留和 offset 独立；③事件驱动怎么保证不重复=消费者幂等、事务边界和 offset 提交策略。

---

### 核心悬念

**"消息把写入压力排队了，但单库 10TB、查询和扩容仍是瓶颈；如何分库分表、读写分离和迁移而不把事务与查询打碎？"**

→ 引出 06-storage-architecture — 分库分表、读写分离与分片扩容。