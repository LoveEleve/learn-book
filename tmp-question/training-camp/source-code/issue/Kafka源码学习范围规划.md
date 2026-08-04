# Kafka 源码学习范围规划

> **版本**: 最新 (v4.x, KRaft)
> **仓库**: `/data/workspace/source-code/code/spring/kafka/`
> **规模**: 1302 Java(clients) + 104 Java+Scala(core) + 631(storage/metadata/raft/coordinator) ≈ 2000 源文件
> **日期**: 2026-08-04
> **R1**: 探索 clients/core/storage/group-coordinator — 7🔴+4🟡=11 域
> **R2**: 深度读 Partition(状态机+ISR)+Consumer(95文件)+GroupCoordinator(KIP-848)——K-2/K-4/K-6 增强
> **R3**: Segment布局+LazyIndex+Snapshot+FetchSession+TransactionMarker——K-3/K-11增强 + **K-12新域**
> **R4**: QuorumController+MetadataImage+ImageLoader+Raft state machine——**K-9 全面增强**
> **R5**: Cleaner三阶段算法(SkimpyOffsetMap双哈希+cleanSegments+groupBySize+原子swap)+KRaftControlRecordStateMachine——**K-10 全面增强**
> **R6**: SocketServer Acceptor→Processor→Handler 三层线程模型+AbstractFetcherThread epoch截断4规则——**K-8 全面增强 + K-4 增强**

---

## 一、仓库概况

Kafka 是 LinkedIn 开源的分布式流式平台，核心是 **append-only commit log**——所有消息按顺序追加到分区日志文件，消费者按 offset 顺序消费。

**源码模块**（按学习价值排序）：

| 模块 | 文件数 | 语言 | 职责 | 纳入 |
|:---:|:---:|:---:|---|---|
| `clients/` | 1302 | Java | Producer/Consumer/Admin 客户端 API + 网络层 + 序列化 | ✅ 核心 |
| `storage/` | 119 | Java | Log/LogSegment 存储引擎、日志清理(compaction) | ✅ 核心 |
| `core/` | 104 | Scala+Java | Broker Server、ReplicaManager、KafkaApis | ✅ 核心 |
| `group-coordinator/` | 82 | Java | 消费者组协调器、Rebalance 协议 | ✅ 核心 |
| `coordinator-common/` | 27 | Java | Coordinator 共享工具 | 🟡 |
| `metadata/` | 175 | Java | KRaft 元数据管理(取代 ZK) | 🟡 |
| `raft/` | 87 | Java | KRaft 共识协议——Raft 实现 | 🟡 |
| `server-common/` | 141 | Java | 服务端共享组件、Purgatory(延迟操作) | 融入 |
| `tools/` `trogdor/` `connect/` | — | — | 工具/压测/Connect——淘汰 | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（7 个——Producer/Consumer/Broker 三端）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| K-1 | **Producer—发送流程** | KafkaProducer, RecordAccumulator, Sender, BufferPool | **send() 异步流水线**：`KafkaProducer.send(ProducerRecord)` → ①序列化(Serializer) → ②分区(Partitioner，默认 murmur2 hash) → ③`RecordAccumulator.append()` 按 tp(topic-partition) 聚合成 `ProducerBatch` → ④`BufferPool` 管理堆外内存(默认 32MB，`batch.size`=16KB) → ⑤`Sender` 后台线程 drain 满的 batch → **NetworkClient** 发送到 leader broker。**应答机制**：`acks=0`(不等)/`acks=1`(leader 写入即返回)/`acks=all`(所有 ISR 确认)。**幂等性**：`enable.idempotence=true` → Producer 分配 `producerId+epoch`，Broker 端对 `<producerId, seqNo>` 去重(单分区有序)。**事务**：`TransactionalId` → `initTransactions()` → `beginTransaction()` → `commitTransaction()` — 跨分区原子写入(两阶段) |
| K-2 | **Consumer—拉取流程** | KafkaConsumer(Classic), AsyncKafkaConsumer, Fetcher, ConsumerCoordinator, FetchBuffer | **poll() 双模型(R2 源码验证)**：①**ClassicKafkaConsumer**(旧) — `poll(timeout)` → `ConsumerCoordinator.ensureActiveGroup()` → `Fetcher.sendFetches()` 异步拉取 → `ConsumerNetworkThread` 后台收数据 ②**AsyncKafkaConsumer**(新，95 文件) — 基于 `CompletableFuture` 的异步 API，`ConsumerNetworkThread` 事件循环驱动，独立的 `FetchRequestManager`/`FetchBuffer`/`FetchCollector` 体系。**Offset 管理**：`auto.offset.reset`(earliest/latest) → `position()` 当前消费位置 → `commitSync/commitAsync` 提交到 `__consumer_offsets` compact topic(key=groupId+topic+partition)。**Rebalance 协议**：`ConsumerCoordinator.pollHeartbeat()` 心跳 → Coordinator 超时触发 rebalance → `AbstractPartitionAssignor`(Range/RoundRobin/Sticky/CooperativeSticky/Uniform) 分配 → `onPartitionsRevoked/Assigned` 回调 |
| K-3 | **Log 存储引擎** | LocalLog, LogSegment, OffsetIndex, LazyIndex, ProducerStateManager | **分段存储(R2 源码验证)**：每个分区目录下多个 `LogSegment`。**每个 Segment 4 个文件**：①`.log`(FileRecords 数据) ②`.index`(`LazyIndex<OffsetIndex>` — offset→position 稀疏索引，mmap 按需加载) ③`.timeindex`(`LazyIndex<TimeIndex>` — timestamp→offset) ④`.txnindex`(`TransactionIndex` — 事务标记索引)。**LazyIndex 延迟加载(R3 关键)**：`ReentrantLock` 保护，首次 `get()` 时才 mmap 文件——broker 启动时数千 Segment 无需全部 mmap。**写入流程**：`activeSegment.append(lastOffset, records)` → `updateLogEndOffset()`。**ProducerStateManager(R3 710行)**：`ProducerStateEntry`(producerId+epoch+lastSeq+lastDataOffset+offsetDelta+coordinatorEpoch+currentTxnFirstOffset) → `snapshot` 文件持久化(CRC+version验证)→崩溃恢复时 `loadFromSnapshot()` + 从 `lastSnapOffset` 回放 Segment batch 重建状态。**Producer 过期**：`removeExpiredProducers()` → idle > `producerIdExpirationMs`(默认 24h) 清理。**Transaction 追踪**：`ongoingTxns`(TreeMap 按 firstOffset) + `unreplicatedTxns`(已完成但高于 HW 的事务)。**日志清理**：①`delete` — 按 retention 时间/大小删除旧 Segment(默认 7 天) ②`compact` — key 去重保留最新 value
| K-4 | **Partition & ISR—副本同步** | Partition, ReplicaManager, AbstractFetcherThread | **Partition 状态机**：`CommittedPartitionState` + `OngoingReassignmentState` + `leaderIsrUpdateLock` 读写锁 + `maybeExpandIsr/maybeShrinkIsr`。**Follower 同步(R6 AbstractFetcherThread 源码验证)**：`doWork()` 两阶段——①`maybeTruncate()`(epoch-based 截断→HW 截断, 4 rules 截断策略) ②`maybeFetch()`(`leader.buildFetch(partitionStateMap)`→发送 fetch→`processFetchRequest()`→append 到本地 Log)。**Epoch 截断 4 规则**：①undefined epoch→用 HW ②valid offset+undefined epoch→truncate 到 leader offset(低于 LEO 时) ③epoch 未知→truncate 到最大已知小 epoch 的 endOffset ④正常→min(leaderOffset, followerEpochEndOffset, LEO)。**FENCED_LEADER_EPOCH**→分区被 fence
| K-5 | **Controller—控制器** | KafkaController(旧ZK), QuorumController(KRaft) | **职责**：①分片状态机(PartitionStateMachine) — Online/Offline/New 状态转换 ②副本状态机(ReplicaStateMachine) — NewReplica/OnlineReplica/OfflineReplica ③分区 Leader 选举——ISR 中第一个活着的副本成为新 Leader ④分区重分配——Rebalance。**KRaft 替代 ZK**：Kafka 3.3+ 用 KRaft(Raft共识) 管理元数据，消除 ZK 依赖——`QuorumController` 作为 Active Controller 通过 Raft log 复制元数据变更。**Controller 选举**：KRaft 模式下由 Raft 选举自动决定 Controller(leader of metadata partition) |
| K-6 | **Consumer Group—消费者组** | GroupCoordinator, ConsumerGroup, ClassicGroup, ModernGroup | **GroupCoordinator (82 文件 R2 发现)**：运行在 `__consumer_offsets` 分区 Leader 所在 Broker。**双协议并存**：①**ClassicGroup**(ZK 时代，ClassicGroupMember) — JoinGroup→SyncGroup→Heartbeat→LeaveGroup 的四步 rebalance + Eager 协议(stop-the-world 全停再分配) ②**ModernGroup**(KIP-848，ConsumerGroupMember) — **增量 Incremental Rebalance**——Consumer 不会全停，只在分区变更时调整受影响的分区。**Assignor 体系**：`RangeAssignor`(默认，按 topic 均分) / `UniformAssignor`(新，均分到所有 consumer) / `StickyTaskAssignor`(粘性，尽量保留已有分配) / `SimpleAssignor`。**Topology 感知**：`ConfiguredTopology` + `CopartitionedTopicsEnforcer`——Kafka Streams 要求 co-partitioned topic 有相同分区数。**Offset 存储**：`OffsetMetadataManager` 管理 offset 过期和压缩——`OffsetExpirationCondition` 判断 offset 是否过期(基于消费者组最后活跃时间) |
| K-7 | **Purgatory—延迟操作** | DelayedOperation, DelayedProduce, DelayedFetch, DelayedOperationPurgatory | **需求场景**：①`acks=all` — Producer 需等所有 ISR 同步完成 ②`fetch.min.bytes=1` — Consumer fetch 需等足够数据。**Purgatory 设计**：`DelayedOperation` 注册到 `DelayedOperationPurgatory` 的 TimerWheel(时间轮) → 超时或条件满足时 `tryComplete()` → 完成回调 `onComplete()`。**TimerWheel**：分级时间轮(Hierarchical Timing Wheel) — 每级精度递增(1ms/20ms/400ms/...)，避免 `ScheduledThreadPoolExecutor` 的 O(n) 遍历 |

### 🟡 扩展域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| K-8 | **网络层—三层线程模型** | SocketServer(1715行), Acceptor, Processor, RequestChannel | **Acceptor→Processor→Handler 三层线程架构(R6 源码验证 SocketServer.scala)**：①**Acceptor**(1线程/listener) — Java NIO `ServerSocketChannel.accept()` → round-robin `assignNewConnection()` → `processor.newConnections.offer()`(ArrayBlockingQueue) → 所有 Processor 队列满则阻塞。②**Processor**(`num.network.threads`=3) — 主循环：`configureNewConnections()`(NIO selector注册,限流connectionQueueSize) → `processNewResponses()` → `selector.poll(300ms/0ms)` → `processCompletedReceives()`(解析请求→RequestChannel) → `processCompletedSends()`(写响应) → `processDisconnected()` → `closeExcessConnections()`。**智能 poll 超时**：有新连接时 timeout=0(立即处理)，队列空时 timeout=300ms(省 CPU)。③**KafkaRequestHandlerPool**(`num.io.threads`=8) — 从 `RequestChannel.requestQueue`(ArrayBlockingQueue, `queued.max.requests`=500) 取请求 → `KafkaApis.handle()` → 路由到具体 handler(handleProduceRequest/handleFetchRequest/...) → 响应入 Processor 的 `responseQueue`(LinkedBlockingDeque) + `wakeup()` |
| K-9 | **KRaft—元数据共识** | QuorumController(2172行), MetadataImage, MetadataLoader, KRaftControlRecordStateMachine | **KRaft vs ZK 对比(R4 源码验证)**：Kafka 3.3+ GA，4.0 废弃 ZK。**QuorumController**(metadata/ 175 文件) — 所有元数据操作(createTopic/createPartitions/registerBroker)通过 `appendWriteEvent()` 转为 `ControllerResult<T>`(元数据记录 + 响应) → 写入 Raft log → Leader 复制到 Follower Controllers。**MetadataImage** — 集群元数据的完整快照(ClusterImage + TopicsImage + ConfigurationsImage + ...)，每个 broker 从 Raft log 加载自己的 image。**Image 加载流程**(image/loader/)：`SnapshotManifest` → `MetadataLoader` → 从 snapshot 恢复基础状态 → `LogDeltaManifest` 回放增量 → 更新 `MetadataImage`。**MetadataPublisher**(image/publisher/) — Active Controller 通过 `SnapshotEmitter` 定期生成 snapshot → `SnapshotGenerator` 压缩 Raft log → 非 Controller broker 从 Raft log 拉取更新。**Raft 实现**(raft/ 87 文件)：`KRaftControlRecordStateMachine`(Raft 状态机) + `BatchAccumulator` + `EpochElection`(term-based 选举) + `VoterSetHistory`(quorum 管理) + `FuturePurgatory`(等待 committed offset)。**Broker 注册**：`BrokerHeartbeatManager` 跟踪心跳(KRaft 替代 ZK 临时节点)——`BrokersToIsrs` 维护 ISR 映射
| K-10 | **Log Compaction—日志压缩** | Cleaner(766行), LogCleaner(654行), SkimpyOffsetMap, LogCleanerManager | **Key 去重(R5 源码验证 Cleaner.java 完整算法)**：Compaction 保留每个 key 的最新 value。**三阶段流程**：①**buildOffsetMap** — `SkimpyOffsetMap`(hash-based, hash1+hash2 双哈希探测) 遍历 dirty segment → `buildOffsetMapForSegment()` 读取 MemoryRecords batch → 提取 key → 存入 map(key→latestOffset) → `maxDesiredMapSize = slots × dupBufferLoadFactor` 容量上限。②**cleanSegments** — 创建新 segment(`.cleaned` 后缀) → `groupSegmentsBySize()` 将 segment 分成 ~segmentSize 组(每组生成一个 cleaned segment) → 遍历每条消息：map 中 key 的最新 offset == 当前 offset → 保留；否则 → 丢弃(旧值)。墓碑消息(value=null) 保留但 `legacyDeleteHorizonMs` 后删除。③**原子交换** — 清理完成后 `Log.replaceSegments()` 原子替换旧 segment(s) 为新的 cleaned segment。**Cleaner 线程池**：`LogCleaner` 管理 `Cleaner` 线程池(`num.io.threads`) → `LogCleanerManager.grabFilthiestCompactedLog()` 选脏比最高 log → `Cleaner.clean()` 执行。**事务感知**：`CleanedTransactionMetadata` 跨组传递事务状态——清理期间不打断事务边界
| K-11 | **事务 & 幂等** | TransactionManager, TransactionCoordinator, TransactionMarkerChannelManager, ProducerStateManager | **幂等保证**(单分区单 Producer)：`ProducerStateManager` 跟踪每个 producerId 的 `lastSeq`——Broker 拒绝 seqNo 跳跃/重复的消息 + `ProducerStateEntry.producerEpoch` 防止 epoch 回退。**事务保证**(跨分区)：`TransactionCoordinator` + `__transaction_state` topic → 两阶段提交(Prepare → Commit/Abort) → **TransactionMarkerChannelManager(R3 发现)** 通过 `InterBrokerSendThread` 向所有涉及分区 Leader 发送 `WriteTxnMarkersRequest`(含 `TxnMarkerEntry` + `TransactionResult: COMMIT/ABORT`) → Leader 写入 `ControlBatch` 标记 → Consumer `read_committed` 隔离级别过滤未提交数据。**Epoch 保护**：Producer epoch 递增防止僵尸 Producer 写入(网络分区恢复后旧 Producer 消息被 Broker 拒绝) |
| K-12 | **FetchSession—增量拉取** | FetchSession(server), FetchSessionHandler(client), CachedPartition | **76 文件 `storage/internals/log/` + 源码验证(R3 全新发现)**。**问题**：传统 fetch 请求每次都要发送**所有**订阅分区的元数据(topic+partition+fetchOffset)——消费者有 1000 个分区时，每个 poll 请求都带 1000 条元数据。**增量 Fetch 方案**：①`FetchSessionHandler`(客户端) 管理 session——首次 `Full Fetch` 注册所有分区 ②后续 `Incremental Fetch` **只发变更**(新增/移除分区) ③Server 端 `FetchSession` 持有 `CachedPartition` 集合——无变更的分区直接用缓存 ④`Epoch` 机制：`INITIAL_EPOCH`(建会话) → 每次成功递增 → `FINAL_EPOCH`(关闭) → `INVALID_SESSION_ID`(超时)。**Metric**：NumIncrementalFetchSessions、NumIncrementalFetchPartitionsCached——减少量级：1000 分区 metadata → 仅变更分区 delta

---

## 三、淘汰清单

| 模块/包 | 理由 |
|:---:|---|
| `connect/` | Kafka Connect——数据集成框架，非消息引擎核心 |
| `tools/` `trogdor/` | 工具/压测——非核心学习内容 |
| `docker/` `examples/` `generator/` | 部署/示例/代码生成 |
| `clients/admin/` | AdminClient(创建 topic/分区管理)——运维 API，面试低频 |
| `config/` | 配置定义 |
| `core/server/` 中 broker 启动/配置类 | `KafkaServer/KafkaConfig/DynamicBrokerConfig`——基础设施 |
| `core/network/` | SocketServer/NIO 细节——中间件 |
| `core/metrics/` | 指标采集 |
| `core/coordinator/transaction/`(旧) | 旧事务协调器——新版在 `group-coordinator/` |

---

## 四、统计

| 类别 | 数量 |
|:---:|:---:|
| 🔴 核心域 | 7 | — |
| 🟡 扩展域 | **5** | 原 4 → **R3 +1**(FetchSession) |
| **总域** | **12** | R1 11 → R3 12 |

## 五、关键架构关系

```
Producer 端: KafkaProducer.send() → RecordAccumulator(batch per partition)
             → Sender thread(drain) → NetworkClient(NIO) → Broker Leader

Broker 端: SocketServer → KafkaApis.handleProduceRequest()
           → ReplicaManager.appendRecords() → Partition.append()
           → Log.appendAsLeader() → LogSegment.append()
           → HW 推进(等 ISR ack) → DelayedProduce 完成

Consumer 端: poll() → ConsumerCoordinator(join group + assign partitions)
             → Fetcher.fetch() → Broker 返回 FetchResponse
             → offset commit → __consumer_offsets compacted topic

Controller: PartitionStateMachine → Leader 选举 → 广播 LeaderAndIsr 到各 Broker
            QuorumController(KRaft) → Raft log 管理元数据
```

## 六、阅读顺序

```
K-3 Log存储 → K-4 ISR副本 → K-12 FetchSession → K-7 Purgatory → K-1 Producer → K-2 Consumer
→ K-6 ConsumerGroup → K-5 Controller → K-8 Network → K-11 事务幂等 → K-10 Compaction → K-9 KRaft
```

存储(Log/ISR) → 核心API(Producer/Consumer) → 协调(Group/Controller) → 扩展(Network/Compact/Txn/KRaft)
