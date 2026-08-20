# Kafka 源码分析 — 超详细交接文档 V12 (阶段 4.2, 11/12 域产出)

> **⚠ 本文为 Kafka 分域唯一入口** — 新会话只需读本文 + 规划 (KAFKA-PLAN.md)
> **日期**: 2026-08-15 | Kafka 4.1.2 (gradle.properties:25 实证, KRaft 时代) | git 16070 commits + 全量 tags (时空溯源可行)
> **入口关系**: 阶段4.2 总入口 (HANDOFF-STAGE3.md §八) | 域规划 `KAFKA-PLAN.md` (12 域, 09 审计: 8/8 数字接受 + 1 淘汰表笔误修正 + §〇.5 00 域发现/三信号/依赖图) | 本文 V12 为唯一入口
> **给新 AI**: 读 §零 (11/12) → §一 (方法论铁律) → §二~§十二 (各域速查) → §十三 (K-9 详案) 开工; 每域完成: 协议块 → Pass 0-3 → 六层深审 → 回填 §零/§一 → HANDOFF-STAGE3

---

## §零 状态总览 (2026-08-15, 12/12 域产出 11 域交付)

| 域 | 目录 | 类型 | 大纲 | 闭环 | 问 | harness | 深审 | 时空溯源 | 状态 |
|:--:|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| K-3 Log 存储 | outlines/k3-log | 🔴 A | 3 篇 | 8 | 32 | 17/17 | 四轮 | ✅ | ✅ 交付 |
| K-4 Partition & ISR | outlines/k4-isr | 🔴 A | 4 篇 | 8 | 34 | 16/16 | 三轮 | ✅ | ✅ 交付 |
| K-12 FetchSession | outlines/k12-fetchsession | 🟡 B | 2 篇 | 6 | 18 | — | 三轮 | — | ✅ 交付 |
| K-7 Purgatory | outlines/k7-purgatory | 🟡 B | 2 篇 | 6 | 18 | — | 三轮 | — | ✅ 交付 |
| K-1 Producer | outlines/k1-producer | 🔴 A | 3 篇 | 8 | 30 | 13/13 | 四轮 | ✅ | ✅ 交付 |
| K-2 Consumer | outlines/k2-consumer | 🔴 A | 3 篇 | 8 | 30 | 14/14 | 三轮 | ✅ | ✅ 交付 |
| K-6 Consumer Group | outlines/k6-group | 🔴 A | 3 篇 | 8 | 30 | 17/17 | 三轮 | ✅ | ✅ 交付 |
| K-5 Controller | outlines/k5-controller | 🔴 A | 2 篇 | 6 | 30 | 13/13 | 三轮 | ✅ | ✅ 交付 |
| K-8 网络层 | outlines/k8-network | 🟡 B | 2 篇 | 6 | 18 | — | 三轮 | — | ✅ 交付 |
| K-11 事务与幂等 | outlines/k11-transaction | 🟡 B | 2 篇 | 6 | 18 | — | 三轮 | — | ✅ 交付 |
| K-10 Compaction | outlines/k10-compaction | 🟡 B | 2 篇 | 6 | 18 | — | 两轮+五维 | — | ✅ 交付 |
| K-9 KRaft | — | 🟡 B | ⏳ | — | — | — | — | — | ⏳ 待做 |

**执行序**: K-3 → K-4 → K-12 → K-7 → K-1 → K-2 → K-6 → K-5 → K-8 → K-11 → K-10 → K-9 (存储→API→协调→扩展)

**📊 累计**: 11 域完整交付 + K-9 待做 | 30 篇大纲 / 82 闭环 / 276 问 / 6 harness (90/90 PASS) / 6 份时空溯源

---

## §一 方法论铁律 (Kafka 阶段实战验证, 新 AI 必读)

1. **09 怀疑审计每域必走**: 规划断言必须源码复核 — 累计抓出规划错误 5 处 (①淘汰表 core/network 笔误 ②auto.offset.reset 两态漏 none ③Classic/Async 双模型混淆 ④默认 assignor Range→Uniform ⑤K-10 LogCleaner L654/LogCleanerManager L798 文件尾错标→真实 L143,209/L240)
2. **裸行号三形态扫描**: 行级 `(Lxxx)` + 括号形 `(Lxxx` + 无括号形 ` Lxxx` 三种都要扫 — 写后立即 `grep -rnE '\(L[0-9]+' *.md | grep -vE 'scala:|java:'` + `grep -rnoE '(^|[^a-zA-Z:])L[0-9]+...'`
3. **awk 批量修复必须逐处人工复核归属**: 行号集合重叠导致系统性错标 (K-4 L98-103/K-7 L345/K-1 L345/K-2 L64-66 — 铁律第 5 次验证) — 修复后抽样交叉验证源码内容
4. **跨域对照必须核验对方域**: K-12 抓 E-4 编造对照 (全域零缓存概念) → 改为 E-10 diff 发布; K-8 抓 NioEventLoop 错引 ch2 → 实际 ch3-selector-01; 每个对照声明前 grep 对方域
5. **编造类数值错误 3 次**: K-4 索引 256KB→2MB (ENTRY_SIZE=8B); K-1 linger.ms 10ms→5ms (设计文档示例≠当前默认); K-10 "8B 哈希"→24B/条 (MD5 16B+8B offset) — 数值必须查当前源码默认
6. **MCP 语义工具强制**: 禁 grep 追踪调用链 (05) — K-1 doSend 链 trace_path 实证; Scala 端未索引需标注 `[索引覆盖: scala 未索引]`
7. **大纲先呈报确认再写正文** (01): 每域 Pass 3 大纲 → 用户确认 → 后续环节
8. **双链禁链未交付域** (06 §6): 引出只指向已存在域, 交付后回补 (K-3 03 篇/K-4 04 篇已回补)
9. **规划 K-8/K-11 断言 100% 验证零错误** — 说明规划质量随 R 轮迭代上升, 但前 8 域 4 处错误证明"标注已验证"也要复核
10. **三遍验证闭环**: 写时 grep → 自查 → REVIEW 逐锚点复核 (K-9 延续此纪律)

---

## §二 K-3 Log 存储 (✅ 🔴 A, 3 篇 + harness 17/17)

**目录**: outlines/k3-log/ | 核心文件: LogSegment.java:65,79-84,167-173,250-280,431-459,478-524,557,624-645 + LocalLog.java:68,80,101,251,526-529,581-646,654-686 + LogLoader.java:357-420,464-466 + ProducerStateManager.java:85-93,296,428-455 + OffsetIndex.java:97-107 + LazyIndex.java:28-46 + UnifiedLog.java:1826 + LogConfig.java:125,166,169,194,196 + ServerLogConfigs.java:81

| 机制 | 源码锚点 |
|:--|:--|
| 分段四件套 | LogSegment 4 字段 (L79-84): .log/.index/.timeindex/.txnindex, baseOffset 命名 (L83) |
| 写路径 | 数据先行 (L260) → 稀疏索引每 4096B 一条 (L270-274) → LEO 推进 (LocalLog.java:528) |
| 稀疏索引 | OffsetIndex 二分 (OffsetIndex.java:97-107) + LazyIndex 延迟 mmap (LazyIndex.java:28-46); 索引上限 10MB (ServerLogConfigs.java:77) → 1GB 段索引 ~2MB |
| roll 轮转 | 四条件 (L167-173): 1GB (LogConfig.java:125)/时间/索引满/溢出 |
| 崩溃恢复 | LogLoader 扫描 (L357-391) → 干净关停跳过 (L466) → 索引重建+坏数据截断 (LogSegment.java:478-524) |
| 刷盘机制 | 四文件全刷 (LogSegment.java:624-645) + recoveryPoint (LocalLog.java:101) + M/S 双配置 (LogConfig.java:166,169,194,196) |
| 截断 | 段级删除+段内截断+LEO 重置 (LocalLog.java:680-686); HW 保护 (UnifiedLog.java:1826) |
| ProducerState | 三容器 (ProducerStateManager.java:85-93) + snapshot 防重复 (L428-455) — K-11 底座 |
| 时空溯源 | 2011 初始 → 2012 KAFKA-506 分段 → 2017 KIP-98 事务 → 2019 LazyIndex → 2024 Java 化 |

**harness**: MiniKafkaLog 17/17 (append/稀疏索引二分/roll/truncate/恢复重建/minOneMessage — 抓 4 缺陷, 2 处验证真实源码语义: 段创建首条索引/定位含 startOffset 的批)
**REVIEW 教训**: 裸锚点 131 处根治; completeness 32 问 1 回补 (vs E-8 对照)

---

## §三 K-4 Partition & ISR (✅ 🔴 A, 4 篇 + harness 16/16)

**目录**: outlines/k4-isr/ | 核心文件: Partition.scala:308,328,412,733-885,909-937,1018-1074,1152-1195,1231-1306,1331 + AbstractFetcherThread.scala:58,115-182,211-232,276-315,318-373,604-655 + ReplicaManager.scala + docs/design/design.md §Replication
| [索引覆盖: scala 未索引]

| 机制 | 源码锚点 |
|:--|:--|
| ISR vs 多数派 | 设计文档: f+1 容忍 f 失败, PacificA; 提交 = 全 ISR + min.insync |
| ISR 动态维护 | expand (L1018-1074: 追平 HW L1049-1054) / shrink (L1231-1306: stuck/slow L1284-1293) — 读锁检查写锁执行, 锁外上报 (L1034,1267) |
| 状态机 | makeLeader (L733-830: epoch 防旧 L743 + assignEpochStartOffset L793) / makeFollower (L839-885: ISR 清空 L861 + fetcher 重启 L883) — 不做本地选举 |
| 两阶段拉取 | doWork (L115-118): maybeTruncate (L174) → maybeFetch (L120) — 锁内截断+epoch 校验 (L215-225) |
| epoch 截断 4 规则 | getOffsetTruncationState (L604-655): ①undefined→HW ②valid+undefined epoch→min ③未知→迭代 ④正常→min 三者 |
| FENCED_LEADER_EPOCH | onPartitionFenced (L302-315): 同 epoch→等新 LeaderAndIsr (L308) — fencing token 语义 |
| HW 推进 | maybeIncrementLeaderHW (L1152-1195): HW=全 ISR 最小 LEO (L1170-1174) + under-min-ISR 冻结 (L1153) |
| 时空溯源 | 2011 初始 → 2012 KAFKA-307/339 → 2013 unclean election → **2017-04-06 KIP-101 epoch 截断** → 2018 KAFKA-6361 |

**harness**: MiniKafkaReplication 16/16 (ISR 扩缩/HW/两阶段/孤儿截断/状态机/fencing — 抓 1 断言缺陷)
**REVIEW 教训**: 裸锚点 51 处根治; awk 归属 2 处错标; 02 篇补负面 (不做本地选举); 内容深度轮抓索引数值 256KB→2MB

---

## §四 K-12 FetchSession (✅ 🟡 B, 2 篇)

**目录**: outlines/k12-fetchsession/ | 核心文件: FetchSession.scala:236,271-292,337-386,599-698 + FetchSessionHandler.java:60,76-77,542-598,604-608 + FetchMetadata.java:31-65 + FetchSession.scala:44-45 (Metric)

| 机制 | 源码锚点 |
|:--|:--|
| 会话模型 | FetchSession (L236: id/epoch/partitionMap) + update 增量三元组 (L271-292) |
| Epoch | INITIAL_EPOCH=0 → 递增 → FINAL_EPOCH=-1 (FetchMetadata.java:37,43,64-65) |
| Handler 双路径 | full→新会话/INVALID→INITIAL (L562-573); incremental→nextIncremental/关闭→INITIAL (L578-596) |
| 失效重建 | INVALID_SESSION_ID (FetchSession.scala:337) → 客户端回 INITIAL (L562,578) |
| 缓存淘汰 | CacheShard 五 map (L610-620) + maybeCreateSession (L672-698) |
| 指标 | NumIncrementalFetchSessions/PartitionsCached (L44-45) + evictionsMeter (L623) |

**REVIEW 教训**: 内容深度轮抓**编造对照** (E-4 路由缓存全域零概念) → 改 E-10 diff 发布对照; Metric 断言补覆盖; 规划断言"1ms/20ms/400ms"验证 (SystemTimer L40-42)

---

## §五 K-7 Purgatory (✅ 🟡 B, 2 篇)

**目录**: outlines/k7-purgatory/ | 核心文件: DelayedOperation.java:38-100 + DelayedOperationPurgatory.java:38-170 + TimingWheel.java:97-184 + SystemTimer.java:30-61 + DelayedProduce.scala:89-134

| 机制 | 源码锚点 |
|:--|:--|
| 状态机 | completed volatile + forceComplete 双检锁 (DelayedOperation.java:41,48-72) — 只完成一次 |
| 注册触发 | tryCompleteElseWatch (L122-170): 条件 watch + 超时定时双保险; SHARDS=512 (L41) |
| 死锁防护 | 注释 L135-154: checkAndComplete 无锁调用 |
| 分级时间轮 | TimingWheel (L97-184): tickMs=1ms/wheelSize=20 (SystemTimer.java:40-42) → 层级 1/20/400ms; overflowWheel (L131-141) |
| acks=all 触发 | DelayedProduce.tryComplete (L89-116): checkEnoughReplicasReachOffset (L101, K-4) → forceComplete (L112) |

**REVIEW 教训**: awk 行号集合重叠 5 处错标 (L98-103/L149-151); R4 线程面补 Reaper/单线程执行器; 规划断言 1ms/20ms/400ms 实证

---

## §六 K-1 Producer (✅ 🔴 A, 3 篇 + harness 13/13)

**目录**: outlines/k1-producer/ | 核心文件: KafkaProducer.java:940-1075 + RecordAccumulator.java:275-356 + BufferPool.java:39-75 + Sender.java:241-382 + BuiltInPartitioner.java:39-58,330 + ProducerConfig.java:381,382,393,397 + TransactionManager.java
| [MCP trace_path 实证 doSend 链]

| 机制 | 源码锚点 |
|:--|:--|
| doSend 六步 | 关闭检查 (L976) → waitOnMetadata (L990) → 序列化 (L1004-1016) → partition (L1021) → ensureValidRecordSize (L1031) → append (L1036) → wakeup (L1043) |
| 批聚合双路径 | tryAppend (RecordAccumulator.java:319) / appendNewBatch (L345, allocate L330) |
| BufferPool | free 回收 + waiters 阻塞 (BufferPool.java:49-75); buffer.memory=32MB (ProducerConfig.java:381) / batch.size=16KB (L393) |
| 粘性分区 | BuiltInPartitioner (L39): 无 key 固定分区攒大批; murmur2 (L330) |
| Sender 主循环 | run (L241-258) → sendProducerData (L379: accumulator.ready L382) → client.poll (L345) |
| 重试 | canRetry (Sender.java:691,875-881) + retries 默认 MAX_VALUE (ProducerConfig.java:382) |
| 时空溯源 | 2011 初始 → 2014 Java 异步化 → 2017 KAFKA-4818 幂等 → 2019 KIP-480 粘性分区 |

**harness**: MiniKafkaProducer 13/13 (流水线/批聚合/内存池/粘性/key 哈希/acks — 抓 2 缺陷: computeIfAbsent CME/测试预期)
**REVIEW 教训**: 编造类数值错误第 2 次 (linger.ms 10ms→5ms, ProducerConfig.java:397); R4 重试横切补全; awk 归属 1 处 (client.poll L345)

---

## §七 K-2 Consumer (✅ 🔴 A, 3 篇 + harness 14/14)

**目录**: outlines/k2-consumer/ | 核心文件: KafkaConsumer.java:532-536 + ConsumerDelegateCreator.java:57-70 + AsyncKafkaConsumer.java:172,299-305,385 + ClassicKafkaConsumer.java:116,690,1188 + AbstractCoordinator.java:368,400-401,463 + ConsumerCoordinator.java:103,375 + ConsumerConfig.java:175-179,631 + Fetcher.java:59 + FetchRequestManager.java
| [MCP trace_path 可验证]

| 机制 | 源码锚点 |
|:--|:--|
| 门面+双模型 | KafkaConsumer 门面 (L532, delegate L536) + Creator 分流 (L64: Async 默认 KIP-848 / L66: Classic) |
| FetchBuffer 跨线程 | AsyncKafkaConsumer (L304-305): 网络线程填 (L385) / 应用线程读 (L486-487) |
| rebalance 四步 | ensureActiveGroup (AbstractCoordinator.java:400) → joinGroupIfNeeded (L463) → pollHeartbeat (L368) |
| offset 管理 | position 单整数 (设计文档) + updateFetchPositions (L1188) + auto.offset.reset 三态 (ConsumerConfig.java:175-179) |
| 组活性 | max.poll.interval.ms=300000 (L631) + wakeup (KafkaConsumer.java:1850) |
| 时空溯源 | 2014 KAFKA-1328 → 2015 KAFKA-2464 → 2022-2024 KIP-848 (Async 2023-11-15/Classic 更名 2024-07-29) |

**harness**: MiniKafkaConsumer 14/14 (门面分流/FetchBuffer/Classic 拉取/rebalance/三态 reset/commit — 一次通过)
**REVIEW 教训**: **规划断言被推翻** (Classic 无 ConsumerNetworkThread, 引用=0 — 规划第 3 处错误); 三态 reset 补 none; 02 篇锚点不足 5→8

---

## §八 K-6 Consumer Group (✅ 🔴 A, 3 篇 + harness 17/17)

**目录**: outlines/k6-group/ | 核心文件: GroupCoordinator.java:84-172 + GroupCoordinatorShard.java:153,457-970 + GroupMetadataManager.java:4691-4724 + ClassicGroup.java + ModernGroup.java + ConsumerGroupMember.java:45,528 + OffsetMetadataManager.java:82,583,600 + GroupCoordinatorConfig.java:187-193 + assignor/ (7 文件)

| 机制 | 源码锚点 |
|:--|:--|
| 双协议分流 | KIP-848 consumerGroupHeartbeat (L84, Shard L457) + 旧四步 classicGroupJoin/Sync/Heartbeat/Leave (Shard L549-970) |
| KIP-848 增量 | heartbeat 携带订阅/分配 (GroupMetadataManager.java:4715-4720) + epoch -1/-2 离组 (L4697-4701) |
| 分配上移 | serverAssignor (L4719) — 服务端执行分配 |
| Assignor | Uniform 默认 (GroupCoordinatorConfig.java:187-193, "first one is the default") + Range/Simple + streams Sticky/Copartitioned |
| Offset 存储 | commitOffset (Shard L852 → OffsetMetadataManager L600) + expireTimestampMs (L583) + OffsetExpirationCondition (L1032-1039) |
| 时空溯源 | 2015 KAFKA-2464 → 2017 事务 → 2022 KIP-848 模块 → 2023 OffsetMetadataManager/Classic 更名 → 2024 ModernGroup 抽象化 |

**harness**: MiniKafkaGroup 17/17 (heartbeat/epoch 离组/增量订阅/Classic 四步/Uniform/offset — 抓 1 缺陷: 分配需全组 reconcile)
**REVIEW 教训**: **规划第 4 处错误** (默认 assignor Range→Uniform); streams 面补覆盖; R3 重分配补全 (Controller 四职责)

---

## §九 K-5 Controller (✅ 🔴 A, 2 篇 + harness 13/13)

**目录**: outlines/k5-controller/ | 核心文件: QuorumController.java:174,729-831,931-985 + BrokerHeartbeatManager.java:45-68 + PartitionChangeBuilder.java:70-112 + MetadataImage.java:33-50 + PartitionReassignmentReplicas.java:32,98 + GroupCoordinatorConfig (对照)

| 机制 | 源码锚点 |
|:--|:--|
| 写事件队列 | appendWriteEvent (L931-954) → generateRecordsAndResult (L729-831) — 单一写路径 |
| Raft commit 分工 | active 推进 purgatory (L972-978) / standby 回放 (L979-985) → MetadataImage (L33-50) |
| 心跳活性 | BrokerHeartbeatManager (L58) + fenced (L66-68); 仅 active 持有 (L51-57) — KRaft 替代 ZK |
| Leader 三档选举 | preferred (L70) → ISR 内选 (L74) → 出 ISR 保在线 (L78, unclean); minISR (L88) |
| 重分配 | PartitionReassignmentReplicas (L32, 完成检查 L98) |
| 时空溯源 | 2012 KAFKA-499 ZK → 2021 KAFKA-12276 KRaft → 2021 KAFKA-13019 MetadataImage |

**harness**: MiniKafkaController 13/13 (写事件/回放/心跳 fenced/三档选举/快照 — 抓 2 缺陷: 选举漏 fenced 检查/心跳恢复未解除 fence)
**REVIEW 教训**: 规划断言修正 2 处 (状态机重构 PartitionStateMachine→PartitionChangeBuilder/选举三档); MetadataImage+重分配补覆盖

---

## §十 K-8 网络层 (✅ 🟡 B, 2 篇)

**目录**: outlines/k8-network/ | 核心文件: SocketServer.scala:72,474,591-728,816,846,906-918,950,1010-1012,1019 + RequestChannel.scala:344,351 + SocketServerConfigs.java:146,154 + ServerConfigs.java:46 + BrokerServer.scala:476
| [索引覆盖: scala 未索引]

| 机制 | 源码锚点 |
|:--|:--|
| 三层模型 | Acceptor (L474, run L591 + round-robin L728) → Processor (L816, 3 线程) → Handler 池 (8 线程) |
| 六步循环 | configureNewConnections (L911) → processNewResponses (L913) → processCompletedReceives (L915) → processCompletedSends (L916) → processDisconnected (L917) → closeExcessConnections (L918) |
| 智能 poll | newConnections 空 300ms / 非空 0ms (L1010-1012) |
| 双队列 | requestQueue=ArrayBlockingQueue (RequestChannel.scala:351) / responseQueue=LinkedBlockingDeque (SocketServer.scala:846) + wakeup (L742) |
| 数值断言 | num.network.threads=3 (SocketServerConfigs.java:154) / num.io.threads=8 (ServerConfigs.java:46) / queued.max.requests=500 (L146) — 规划 R6 断言 100% 实证 |

**REVIEW 教训**: 内容深度轮抓**对照引用错误** (NioEventLoop 错引 ch2 → 实际 ch3-selector-01, 6 处修正; boss/worker → ch9-bootstrap-01); 规划 K-8 断言零错误 (首个全对域)

---

## §十一 K-11 事务与幂等 (✅ 🟡 B, 2 篇)

**目录**: outlines/k11-transaction/ | 核心文件: TransactionManager.java:95,252-255,329-372 + TransactionCoordinator.scala:113,505-593,704-732 + TransactionMarkerChannelManager.scala:30-31,167,176-206 + TransactionStateManager.scala:869 + ConsumerConfig.java:364-369 + UnifiedLog.java:1421 + GroupCoordinator.java:395
| [索引覆盖: Java 已索引, Scala 未索引]

| 机制 | 源码锚点 |
|:--|:--|
| 幂等三层 | TransactionManager (L95) + K-3 ProducerStateManager (底座) + producerId 分配 (L113) |
| 客户端五态 | init (L329) → begin (L332) → commit (L360) / abort (L372) + API 映射 (L252-255) |
| 两阶段 | handleEndTransaction (L505) → Prepare (L562-593) → Complete (V2 状态表 L704-732) |
| Marker 分发 | TransactionMarkerChannelManager (L176 按 broker 队列) + InterBrokerSendThread (L167 "TxnMarkerSenderThread") → 分区 leader 写 ControlBatch (UnifiedLog.java:1421) |
| read_committed | isolation.level (ConsumerConfig.java:364-369): 只读到 LSO (L368) |
| 状态机 | TransactionStateManager (L869) + __transaction_state — 与 K-6 组记录同构 |

**REVIEW 教训**: 规划 K-11 断言 100% 验证零错误 (连续第 2 域); read_committed/ControlBatch/InterBrokerSendThread 锚点补全; 代价/协调器故障回补

---

## §十二 K-10 Compaction (✅ 🟡 B, 2 篇)

**目录**: outlines/k10-compaction/ | 核心文件: Cleaner.java:147-180,492-529 + SkimpyOffsetMap.java:32-90 + LogCleaner.java:143,209 + LogCleanerManager.java:240,271-274 + LogConfig.java:129,132 + K-3 Log.replaceSegments (LocalLog.java:996)

| 机制 | 源码锚点 |
|:--|:--|
| 双哈希 | SkimpyOffsetMap (hash1/hash2 SkimpyOffsetMap.java:53-54, 构造 L73-93) — 16B MD5 哈希+8B offset=24B/条 (CleanerConfig.java:37 HASH_ALGORITHM=MD5, SkimpyOffsetMap.java:89), 只存哈希不存 key |
| 三阶段 | buildOffsetMap (Cleaner.java:156) → groupSegmentsBySize (L172) → cleanSegments (L180) |
| 原子 swap | Log.replaceSegments (LocalLog.java:996, .cleaned→正式段容器级替换) |
| 墓碑 | v2+ 由 batch.deleteHorizonMs 判定 (Cleaner.java:505-506, 写入时刻+delete.retention.ms 默认 24h LogConfig.java:129); <v2 由 legacyDeleteHorizonMs (L147, L503-504); cleanableHorizonMs (L163) |
| 调度 | LogCleaner 线程池默认 1 线程 (CleanerConfig.java:38, LogCleaner.java:143,209) + grabFilthiestCompactedLog (LogCleanerManager.java:240): 需 cleanableRatio>0.5 (LogConfig.java:132) 或 needCompactionNow → 取最高 (L271-289) |

**REVIEW 教训**: 深审抓**编造数值** (8B→24B/条 MD5+8B) + **墓碑机制错述** (legacyDeleteHorizonMs 仅 <v2) + **对照错引** (惰性+主动过期是 r22-expire 非 r23-evict) + 负面空间补 3 条; 锚点错标 2 处修复 (L654/L798 文件尾→真实 L143,209/L240)

---

## §十三 K-9 KRaft 详案 (最后 1 域, 🟡 B)

### 1. 范围
- **文件**: raft/src/main/java/org/apache/kafka/raft/ (87 文件) + metadata/ (175 文件: MetadataImage/KRaftControlRecordStateMachine) + server-common
- **类型**: 🟡 B — Pass 0-2, Pass 3 可选 (6 闭环 → 2 篇大纲); 无 harness/无时空溯源
- **[索引覆盖: Java 已索引]**

### 2. 规划要点 (KAFKA-PLAN §一 + issue 规划 K-9 R4/R5)
- KRaft vs ZK 对比 (Kafka 3.3+ GA, 4.0 废弃 ZK)
- QuorumController (K-5 已交付) 的复制底座: Raft log 复制元数据变更
- MetadataImage (K-5 已交付 L33-50) 加载链: SnapshotManifest → MetadataLoader → LogDeltaManifest
- KRaftControlRecordStateMachine + BatchAccumulator + EpochElection (term 选举) + VoterSetHistory + FuturePurgatory

### 3. 候选闭环问题 (6 个)
1. Raft 核心协议: 选举/复制/commit (K-5 已铺垫 handleCommit)?
2. EpochElection (term 选举) vs K-4 leader epoch 差异?
3. BatchAccumulator 批积累?
4. MetadataLoader 加载链 (snapshot → delta)?
5. VoterSetHistory (quorum 管理)?
6. 与 K-5 QuorumController 衔接 (K-5 篇 1 已铺垫) + 与 E-10/SofaJRaft 对照?

### 4. 衔接点 (前面 11 域已铺垫)
- K-5: QuorumController 写事件 (L931-954) + handleCommit (L956-985) — Raft log 是复制底座
- K-6: 组元数据记录同源 (元数据即记录模式)
- E-10: ES 集群状态发布 (对照, 已交付)
- SofaJRaft (阶段4.6, 未交付 — 仅规划提及不链接)

---

## §十四 REVIEW 教训汇总 (Kafka 阶段最痛的点)

### 1. 规划断言怀疑 (09 铁律) — 累计 4 处错误
| # | 规划断言 | 实际 | 验证 |
|:--:|:--|:--|:--|
| 1 | core/network SocketServer 淘汰 | K-8 核心 (1715 行) | 09 审计抓出 |
| 2 | auto.offset.reset 两态 | 三态 (earliest/latest/none, ConsumerConfig.java:175-179) | K-2 REVIEW-1 |
| 3 | Classic 用 ConsumerNetworkThread | Classic 引用=0, Async 才有 (L385) | K-2 REVIEW-3 |
| 4 | RangeAssignor 默认 | KIP-848 默认 Uniform (GroupCoordinatorConfig.java:187-193) | K-6 REVIEW-3 |

### 2. 编造类错误 3 次 (内容深度轮抓)
- K-4: 索引 "256KB" → 实测 ~2MB (ENTRY_SIZE=8B, 1GB/4096×8)
- K-1: linger.ms "设计文档 10ms" → 实际默认 5ms (ProducerConfig.java:397) — 历史示例≠当前默认
- K-10: "只存 8 字节哈希" → 实际 24B/条 (MD5=16B + 8B offset, SkimpyOffsetMap.java:89 + CleanerConfig.java:37)

### 3. 跨域对照编造/错引 3 次
- K-12: "E-4 路由缓存" — E-4 全域零缓存概念 → 改 E-10 diff 发布
- K-8: "Netty NioEventLoop (ch2)" — ch2 全域 0 处 → 实际 ch3-selector-01 (L31) + boss/worker → ch9-bootstrap-01
- K-10: "惰性+主动过期 (r23-evict)" — r23 是内存淘汰 → 实际 r22-expire (过期机制) 才对位

### 4. awk 批量归属错误 5 次 (铁律: 修复后必须人工复核)
- K-4: L98-103/L149-151 错标 (TimingWheel 字段/分支 → DelayedOperation/Purgatory)
- K-7: 同上行号集合重叠 5 处
- K-1: client.poll L345 错标 (Sender → RecordAccumulator)
- K-2: L64-66 错标 (ConsumerDelegateCreator → ClassicKafkaConsumer)
- K-10: LogCleaner.java:654 (文件尾 mb())/LogCleanerManager.java:798 (文件尾 OffsetsToClean) 错标 → 真实 L143,209/L240

### 5. 裸行号三形态 (每域都有)
- 扫描命令: `grep -rnE '\(L[0-9]+' *.md | grep -vE 'scala:|java:'` (行级) + `grep -rnoE '(^|[^a-zA-Z:])L[0-9]+...'` (无括号形)
- 根治后必须 review-notes 也同步 (第 5 次验证: 验证清单形裸锚点)

### 6. 规划质量观察
- K-8/K-11 断言 100% 实证零错误 (R6/R3 轮最精确)
- K-10 R5 断言全部实证 — 规划随 R 轮迭代质量上升, 但"标注已验证"也要复核

---

## §十五 知识网络图 (全域双链)

```
K-3 Log (叶子: 分段存储) ──前置──→ K-4 (ISR) → K-12 (会话) / K-7 (等待)
K-3/K-4/K-7 ──→ K-1 (Producer) ──→ K-2 (Consumer) ──→ K-6 (Group)
K-6/K-5 ──→ K-9 (KRaft 收束)
K-4/K-6 ──→ K-5 (Controller)
K-5 ──→ K-9 (Raft log 底座)
K-3 ──→ K-10 (Compaction) / K-11 (ProducerState 底座)
K-1/K-11 ──→ K-3 (事务存储)
K-8 (网络, 横切) ──→ K-6 (KafkaApis 请求入口)

跨仓库:
redis/r8-persistence ↔ K-3 (WAL 对照) / K-10 (AOF rewrite)
redis/r9-replication ↔ K-4 (位点对照)
redis/r14-sentinel ↔ K-4/K-6 (故障转移)
redis/r16-multi ↔ K-11 (事务对照)
redis/r22-expire/r23-evict ↔ K-3/K-10 (删除语义)
redis/r24-string ↔ K-1 (写入面)
redis/r28-networking ↔ K-8/K-12 (协议)
redis/r29-pubsub ↔ K-6 (订阅对照)
redisson/rd2-rlock ↔ K-4/K-5/K-11 (fencing)
redisson/rd4-command ↔ K-1 (命令批)
es/e3-translog ↔ K-3 (WAL 对照)
es/e6-seqno ↔ K-4 (term 对照)
es/e8-merge ↔ K-3/K-10 (段合并)
es/e10-clusterstate ↔ K-5/K-9 (单写者协调)
netty/ch3-selector ↔ K-8 (NioEventLoop)
netty/ch9-bootstrap ↔ K-8 (boss/worker)
netty/ch14-timer ↔ K-7 (时间轮)
rocketmq/rm7-producer ↔ K-1 (Producer 对照)
rocketmq/rm8-push ↔ K-2 (push/pull)
```

---

## §十六 完成检查单

- [x] 09 审计 (数字 8/8 接受, 淘汰表 1 笔误修正, 00 域发现/三信号/依赖图补写)
- [x] 11 域完整交付 (7🔴 + 4🟡) + K-9 待做
- [x] 30 篇大纲 + 82 闭环 + 276 问 + 6 harness (90/90) + 6 时空溯源
- [x] 每域两轮+深审 (07 五维度收官) + REVIEW 教训汇总 (§十四)
- [x] 规划断言怀疑审计累计 5 处修正 + 编造类 3 处 + 对照错引 3 处
- [x] 全域裸行号三形态零残留 + 跨域对照核验纪律
- [x] K-10 大纲呈报确认 + completeness 18 问 + 深审回填 (2026-08-15)
- [ ] **K-9 KRaft (最后 1 域)** — 按 §十三 详案开工
- [ ] 阶段4 收官: Kafka 12/12 + HANDOFF-STAGE3 同步
