# P02：消息队列核心 — 详细规划

> Phase: P02 | 周期: 3-4周 | 书: 4本 (3 RocketMQ + 1 Kafka)
> 依赖: P01 Netty（RocketMQ Remoting 模块基于 Netty）
> 学习模式: **Kafka vs RocketMQ 双轨对比**

---

## 一、为什么 Kafka 和 RocketMQ 一起学

两个消息队列代表两种设计范式：

| 维度 | Kafka | RocketMQ |
|------|-------|----------|
| **设计哲学** | 日志 = 存储（不可变追加写） | 消息 = 业务信号（可消费/重试/死信） |
| **存储模型** | Partition 分段日志（.log/.index/.timeindex） | CommitLog 顺序写 + ConsumeQueue 索引 |
| **消费者模型** | 消费者组 + 分区 Rebalance（Kafka Connect 协调） | 消费者组 + 队列负载均衡（Broker 端管理） |
| **可靠性** | ISR + 高水位机制 | 同步/异步刷盘 + 主从复制（DLedger for Raft） |
| **事务消息** | Exactly-Once（幂等生产者 + 事务） | 半消息 + 回查机制 |
| **适用场景** | 流处理、数据管道、日志收集 | 业务解耦、分布式事务、异步处理 |
| **Java 生态** | 非 Java 原生（Scala → Java 重写中） | Java 原生，国内首选 |

**为什么必须双轨？**

- Kafka 教你"消息就是日志"——这个思想颠覆传统消息模型，理解后才能做事件溯源 / CDC 数据管道
- RocketMQ 教你"消息就是业务"——事务消息、顺序消息、延迟消息，所有业务场景都需要
- 两个对比学才能看清每种设计决策的 tradeoff——例如为什么 RocketMQ 不用 Kafka 的 ISR 机制？

---

## 二、书单

### 2.1 《Kafka权威指南（第2版）》（Gwen Shapira 等）

| 维度 | 信息 |
|------|------|
| MinerU 路径 | `10-中间件-消息队列/Kafka权威指南.../full.md` |
| MinerU 质量 | ✅ 2942行，干净 Markdown |
| 原书特点 | Kafka 核心作者 Jay Kreps 作序，工业标准参考 |

| 章 | 核心知识点 | 学习深度 |
|----|-----------|---------|
| 第1章 初识Kafka | 发布/订阅模型、主题/分区、生产者/消费者、broker/集群 | 概念层 |
| 第2章 安装Kafka | 环境配置、ZK依赖、硬件选择 | 实操层 |
| 第3章 Kafka生产者 | 同步/异步发送、ack配置、序列化器、分区策略、**幂等生产者** | **精读** |
| 第4章 Kafka消费者 | 消费者群组、**分区再均衡（Rebalance）**、线程安全、poll循环 | **精读** |
| 第5章 管理Kafka | AdminClient API | 使用层 |
| 第6章 深入Kafka | **集群成员关系、控制器选举、复制机制、请求处理全链路、物理存储** | **深度精读** |
| 第7章 可靠的数据传递 | 可靠性保证、复制、broker配置 | **精读** |
| 第8章 精确一次性语义 | **幂等生产者 + 事务实现** | **深度精读** |
| 第9章 构建数据管道 | Kafka Connect | 了解 |
| 第10章 跨集群数据镜像 | MirrorMaker | 了解 |
| 第11章 保护Kafka | SSL/SASL/ACL | 使用层 |
| 第12章 管理Kafka | 运维管理 | 使用层 |
| 第13章 监控Kafka | 监控指标 | 使用层 |
| 第14章 流式处理 | Kafka Streams DSL | 了解 |

**精读章（4章）**：Ch3 生产者、Ch4 消费者、Ch6 深入Kafka、Ch7-8 可靠性+精确一次

### 2.2 RocketMQ 三本书合并

| 书 | 行数 | 质量 | 定位 |
|----|------|------|------|
| 《RocketMQ 消息中间件实战派 上册》 | 4956 | ⚠️ | 基础→进阶（Ch1-Ch7） |
| 《RocketMQ 消息中间件实战派 下册》 | 5407 | ⚠️ | 高级→高可用→新特性（Ch8-Ch14） |
| 《Apache RocketMQ 进阶之路 图解版》 | 1874 | ⚠️ | 原理深挖→架构师进阶 |

**综合章节目录（三本合并提炼）**：

| 层次 | 主题 | 来源 | 学习深度 |
|------|------|------|---------|
| 基础 | RocketMQ 架构概述 | 上册Ch1 | 概念层 |
| 通信 | **Remoting 模块（基于 Netty）** | 上册Ch2 | **精读**（连接 P01） |
| 路由 | NameServer 路由发现 | 上册Ch3 | **精读** |
| 生产消费 | 生产者/消费者实现 | 上册Ch4 | **精读** |
| 存储 | **CommitLog + ConsumeQueue** | 上册Ch5 + 进阶之路Ch7 | **深度精读** |
| 治理 | 消息过滤/重试/死信 | 上册Ch6 | 使用层 |
| 事务 | **事务消息（半消息+回查）** | 上册Ch7 + 进阶之路Ch9 | **精读** |
| 轨迹 | 消息追�� | 下册Ch8 | 了解 |
| 稳定性 | 消息可靠性保障 | 下册Ch9 | **精读** |
| 三高 | 高并发/高可用/高性能 | 下册Ch10-12 | **精读** |
| 应用 | 分布式架构实战 | 下册Ch13 | 使用层 |
| 新特性 | **RocketMQ 5.x（Controller/Proxy/DLedger）** | 下册Ch14 | **精读** |
| 消费原理 | 消费进度/重平衡 | 进阶之路Ch5-6 | **精读** |
| 顺序消息 | 顺序消息实现 | 进阶之路Ch8 | 使用层 |
| 定时消息 | 延迟消息实现 | 进阶之路Ch10 | 使用层 |
| 幂等 | 消息幂等 | 进阶之路Ch11 | **精读** |

**精读主题（8个）**：Remoting 通信、NameServer 路由、CommitLog 存储、事务消息、消息可靠性、三高架构、5.x新特性、消费原理

---

## 三、双轨对比学习矩阵

以下 10 个主题必须两本书对照学习，不能单独读一本：

| # | 主题 | Kafka | RocketMQ | 核心对比点 |
|---|------|-------|----------|-----------|
| 1 | **存储模型** | Partition分段日志(.log/.index) | CommitLog顺序写+ConsumeQueue索引 | Kafka用文件系统能力，RocketMQ用内存映射+索引 |
| 2 | **零拷贝** | sendfile(FileChannel.transferTo) | mmap(MappedByteBuffer) | 一个走OS零拷贝，一个走堆外内存映射 |
| 3 | **复制机制** | ISR（同步副本集）+ Leader Epoch | 主从复制(DLedger Raft) 或 同步/异步刷盘 | ISR 允许滞后，Raft 要求多数派 |
| 4 | **消费者模型** | Coordinator协调Rebalance | Broker端管理消费进度 | Kafka 消费者主动分配，RocketMQ broker 分配 |
| 5 | **事务消息** | 幂等生产者 + 事务 Coordinator | 半消息 + 回查机制 | Kafka事务是通用分布式事务，RocketMQ事务是MQ特化 |
| 6 | **延迟消息** | 无原生支持（Kafka Streams 实现时间窗） | 原生18级延迟队列 | RocketMQ更贴近业务需求 |
| 7 | **顺序消息** | 单分区内有序 | 消息组 + 队列选择器 | 实现方式不同，原理相通 |
| 8 | **NameServer vs ZK** | 依赖 Zookeeper（Kafka 3.x 去ZK中） | 自研 NameServer（无状态） | RocketMQ 更轻量但功能少 |
| 9 | **消息过滤** | 无服务端过滤（拉取后客户端过滤） | Broker端Tag/SQL过滤 | RocketMQ减少网络传输 |
| 10 | **高水位 vs 消费进度** | HW(High Watermark)保证数据一致性 | ConsumeQueue offset独立管理 | 设计哲学差异 |

---

## 四、源码阅读清单

### 4.1 Kafka 源码（关键类）

| 源码模块 | 关键类 | 阅读目的 |
|---------|--------|---------|
| 日志存储 | `Log.scala`, `LogSegment.scala`, `LogManager.scala` | 分段日志、索引(.index/.timeindex)、日志清理 |
| 复制 | `Partition.scala`, `ReplicaManager.scala` | ISR管理、Leader选举、HW推进 |
| 生产者 | `KafkaProducer.java`, `Sender.java`, `RecordAccumulator.java` | 批量发送、压缩、分区路由、幂等性 |
| 消费者 | `KafkaConsumer.java`, `ConsumerCoordinator.java`, `AbstractCoordinator.java` | poll循环、Rebalance协议、位移提交 |
| 控制器 | `KafkaController.scala` | Controller选举、分区Leader分配 |
| 事务 | `TransactionCoordinator.scala` | 事务日志、Producer ID分配 |

### 4.2 RocketMQ 源码（关键类）

| 源码模块 | 关键类 | 阅读目的 |
|---------|--------|---------|
| Remoting | `NettyRemotingClient.java`, `NettyRemotingServer.java`, `RemotingCommand.java` | 基于 Netty 的自定义协议（连接 P01） |
| NameServer | `NamesrvController.java`, `RouteInfoManager.java` | 路由注册与发现 |
| 存储 | `DefaultMessageStore.java`, `CommitLog.java`, `ConsumeQueue.java`, `MappedFile.java` | CommitLog/ConsumeQueue 双文件存储 |
| 生产者 | `DefaultMQProducer.java`, `MQClientInstance.java`, `CommunicationMode` | 发送流程（同步/异步/单向） |
| 消费者 | `DefaultMQPushConsumer.java`, `PullMessageService.java`, `RebalanceImpl.java` | Push/Pull模式、负载均衡 |
| 事务 | `TransactionMQProducer.java`, `TransactionListener.java` | 半消息+回查 |
| 5.x新 | `DLedgerCommitLog.java`, `ControllerManager.java`, `BrokerContainer.java` | Raft存储、Proxy模式 |

---

## 五、关键设计哲学（"为什么这样设计"）

### Kafka 核心设计决策

| 设计决策 | 反事实假设 | 为什么选这个 |
|---------|-----------|-------------|
| **为什么用日志模型而不是消息队列模型？** | 消息消费后删除 | 日志不可变的特性使 replay/回溯/多消费者独立消费成为可能——这是流处理的基石 |
| **为什么 ISR 而不是多数派（Raft）？** | 每条消息都需要多数节点确认 | ISR 让"跟得上的副本"参与确认，兼顾性能和数据可靠性 |
| **为什么需要 Coordinator？** | Broker 直接管理消费者 | Coordinator 将消费者组协议与 Broker 核心逻辑解耦 |
| **为什么 Kafka 3.x 去 ZK？** | 继续依赖 ZK | ZK 成为瓶颈（元数据变更需要全量同步），KRaft 将元数据管理内嵌到 Kafka 自身 |

### RocketMQ 核心设计决策

| 设计决策 | 反事实假设 | 为什么选这个 |
|---------|-----------|-------------|
| **为什么用 CommitLog 而不是 Partition 日志？** | 每个Topic一个日志文件 | 所有 Topic 共用一个 CommitLog，顺序写混存 → 磁盘 IO 最大化 |
| **为什么 NameServer 无状态？** | 像 ZK/Nacos 那样有状态 | 无状态 = 不需要选举 = 任意启动/宕机不丢数据（数据来自 Broker 心跳上报） |
| **为什么延迟消息用 18 级而不是任意时间？** | 用户可定任意延迟时间 | TimerWheel 固定级别的内存开销可预测，任意时间需要持久化定时器（太重） |
| **为什么事务消息用"半消息+回查"而不是 2PC？** | 先 prepare 再 commit | 避免 prepare 消息长时间占用存储——半消息对消费者不可见，回查机制保证最终一致性 |

---

## 六、生产陷阱

### Kafka 陷阱

| # | 陷阱 | 场景 | 根因与修复 |
|---|------|------|-----------|
| 1 | **Rebalance 风暴** | 消费者偶尔 GC 停顿 → Coordinator 认为心跳超时 → 触发 Rebalance → 所有分区暂停消费 | `session.timeout.ms` 和 `max.poll.interval.ms` 配置不当；延长超时 + 分离消费线程和心跳线程 |
| 2 | **消息丢失（acks=0/1）** | `acks=1` 或 `acks=0`，Leader 宕机后消息未同步到 Follower | 设置为 `acks=all` + `min.insync.replicas=2` |
| 3 | **消息重复（at-least-once）** | 消费者处理完消息但未提交 offset 就宕机，重启后重新消费 | 业务幂等设计 + Kafka 事务（read_committed） |
| 4 | **__consumer_offsets 无限增长** | 大量消费者组 + 频繁位移提交 → compact 跟不上 | 清理无用消费者组 + 调大 `offsets.retention.minutes` |
| 5 | **磁盘写满** | 日志保留策略（`retention.bytes` / `retention.ms`）配置过宽 | 设置合理的日志保留 + 监控磁盘使用率 + 告警 |

### RocketMQ 陷阱

| # | 陷阱 | 场景 | 根因与修复 |
|---|------|------|-----------|
| 1 | **消息丢失（异步刷盘）** | `flushDiskType=ASYNC_FLUSH`，Broker 宕机丢失 500ms 内的消息 | 核心业务改为 `SYNC_FLUSH` 或使用 DLedger 集群 |
| 2 | **消费失败无限重试** | 消息处理抛出异常 → 进入重试队列 → 一直重试 → 消费积压 | 设置 `maxReconsumeTimes` + 处理不了的直接进死信 |
| 3 | **NameServer 路由延迟** | Broker 刚注册到 NameServer，Producer 还用的是旧路由 | NameServer 不主动推送，依赖定时拉取（默认30s）——对路由敏感场景需要缩短间隔 |
| 4 | **顺序消息消费单线程瓶颈** | MessageListenerOrderly 单线程消费 → 一个消息慢卡全队列 | 拆细队列 + 异步化非顺序依赖操作 |
| 5 | **事务消息超时回查导致性能退化** | 大量半消息在回查 → 占用系统资源 + 发送延迟 | 控制事务消息比例 + 优化回查逻辑（缓存状态） |

---

## 七、面试 Q&A（含面试官意图）

| 问题 | 面试官意图 | 回答主线 |
|------|----------|---------|
| "Kafka 如何保证消息不丢失？" | 考察端到端可靠性理解 | 3段式: Producer(acks=all+retries) → Broker(ISR≥2+unclean.leader.election.enable=false+min.insync.replicas≥2) → Consumer(先处理后提交+enable.auto.commit=false) |
| "RocketMQ 和 Kafka 的区别？" | 考察是否只是用过API，有没有对比思维 | 从存储模型切入——Kafka日志模型 vs RocketMQ CommitLog → 引出复制机制、消费者模型、事务实现等差异 |
| "Kafka 为什么快？" | 考察是否理解 IO 原理 | 顺序写磁盘 + sendfile零拷贝 + PageCache + 批量压缩 + 分区并行 |
| "RocketMQ 的 CommitLog 为什么所有 Topic 共用？" | 考察存储设计理解 | 顺序写 = 最快写路径，所有Topic消息混在一起顺序刷盘。读时靠ConsumeQueue索引定位 |
| "为什么用 ISR 而不是 Raft？" | 考察共识算法+工程tradeoff | ISR粒度更细（每条消息单独确认 vs Raft每个entry确认），Kafka追求极致吞吐 |
| "如何保证消息顺序？" | 考察顺序消息理解 | Kafka: 同一Key→同一分区+单分区有序。RocketMQ: MessageQueueSelector+MessageListenerOrderly |

---

## 八、章节依赖

```
上游：
  P01 Netty：RocketMQ Remoting 模块基于 Netty → P01 Ch3(编解码) / Ch5(Reactor模式)

下游：
  P03 Etcd/ZK：Kafka走KRaft(Raft共识)，RocketMQ走DLedger(Raft共识) → P03
  P04 分布式存储：CommitLog/PartitionLog 存储设计 → P04 Ch2(B+树/LSM)
  P06 Dubbo：RPC调用可以异步投递到MQ解耦 → P06 Ch4(服务通讯)
```

---

## 九、产出物（Phase C）

```
md/P02-Kafka/
├── D-01-生产者全链路.md           # acks配置 → RecordAccumulator → Sender线程 → 幂等性
├── D-02-消费者群组与Rebalance.md  # Coordinator协议 → Rebalance流程 → 位移提交
├── D-03-日志存储与ISR复制.md      # Segment文件 → 索引 → HW推进
├── D-04-事务与Exactly-Once.md     # 事务Coordinator → LSO vs HW → 幂等保证

md/P02-RocketMQ/
├── D-01-CommitLog存储设计.md      # MappedFile → CommitLog → ConsumeQueue → 刷盘策略
├── D-02-NameServer路由发现.md     # Broker注册 → 心跳 → 路由拉取 → 延迟问题
├── D-03-事务消息半消息回查.md     # half message → check → commit/rollback
├── D-04-RocketMQ 5.x新特性.md     # DLedger → Controller → Proxy → 对等部署

md/P02-对比/
├── D-01-Kafka-vs-RocketMQ-存储模型对比.md     # Partition分段 vs CommitLog共用
├── D-02-Kafka-vs-RocketMQ-消息可靠性对比.md   # ISR vs 主从/DLedger
├── D-03-Kafka-vs-RocketMQ-事务消息对比.md     # 幂等+事务 vs 半消息+回查
└── D-04-生产陷阱与面试Q&A.md                  # 10个陷阱 + 6个面试题
```

## 十、学习检查清单

- [ ] 能手画 Kafka 一条消息从 Produce 到 Consume 的完整生命周期（包含所有涉及的类和磁盘文件）
- [ ] 能手画 RocketMQ CommitLog → ConsumeQueue 的双文件读写流程
- [ ] 能解释 ISR 机制的 HW/LEO 推进过程，并写出异常场景下的数据丢失/数据不一致案例
- [ ] 能写出 Kafka Rebalance 的完整协议交互序列（JoinGroup → SyncGroup → Heartbeat → LeaveGroup）
- [ ] 能对比 Kafka Exactly-Once 和 RocketMQ 事务消息的实现差异，并说出各自适用的业务场景
- [ ] 能解释 `acks=all` + `min.insync.replicas=2` 为什么还不够（还需要 `unclean.leader.election.enable=false`）
