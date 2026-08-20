# Kafka — 知识网络化规划 (K-1~K-12, 09 域重审后 v1)

> **日期**: 2026-08-15 | 依据: issue/Kafka源码学习范围规划.md (12 域, R1-R6 六轮增强) + 09 对既有规划保持怀疑 域级重审
> **源码**: `/data/workspace/source-code/code/spring/kafka` (**v4.1.2**, gradle.properties:25 实证, KRaft 时代) | git: 16070 commits + 全量 tags (2026-08-15 fetch --unshallow)
> **定位**: 阶段4.2 — 消息与事务第一环. **流式平台内核 = 存储 (Log) + 复制 (ISR) + 客户端 (Producer/Consumer) + 协调 (Group/Controller)**
> **知识网络**: 与阶段3 ES (E-3 Translog 对照) + Redis (r8 AOF/r22 删除) + RocketMQ (阶段4.1, 消息中间件对照) 互联

## 〇、09 怀疑审计表 (Kafka, 2026-08-15) — 必读

| 既有规划断言 | 验证动作 | 证据 | 结论 |
|---|---|---|---|
| 模块文件数 8 项 (clients 1302/core 104/storage 119/group-coordinator 82/coordinator-common 27/metadata 175/raft 87/server-common 141) | find src/main 穷举 | 8/8 精确一致 | **接受** ✅ |
| 域清单 12 个 (7🔴+5🟡) | 顶层包扫描对照 | clients(producer 31/consumer 189/admin 213/common 825) + core(scala 8 包) + storage/log 3 层 + group-coordinator 全部有域覆盖 | **接受** ✅ |
| clients/admin 213 文件淘汰 | 设计决策测试 | AdminClient 运维 API (createTopic/describe) — 非消息引擎定义特征, 面试低频 | **接受** ✅ |
| clients/common 825 文件未独立成域 | 设计决策测试 | 工具类 (ByteUtils/Message/序列化) + 客户端网络 — 支撑, 并入 K-1/K-2/K-8 | **接受** ✅ |
| 淘汰表 "core/network SocketServer 淘汰" | 对照 K-8 域 | **自相矛盾**: K-8 网络层核心就是 SocketServer (1715 行) | **修正**: 移除该淘汰行, core/network 是 K-8 核心 |
| 时空溯源可行性 | git 检查 | 原 shallow 1 commit → **已 fetch --unshallow: 16070 commits + 全量 tags (0.8.0~4.1.2)** | **可行** ✅ |

**覆盖率报告**: 12/12, 无新增域 (规划已过 R1-R6 六轮增强, 数字断言错误率 0/8 — 与 ES 40% 对比, 该规划成熟度高)。

## 〇.5 00 域发现补充 (入口点展开 + 三信号 + 依赖图, 2026-08-15 补)

### 入口点 (00 §1, Kafka 官方示例)
`KafkaProducer.send()` / `KafkaConsumer.poll()` (客户端) + `KafkaApis.handleProduceRequest/handleFetchRequest` (服务端)

### Level-1/2 展开 (00 §2)
```
KafkaProducer.send → RecordAccumulator.append → BufferPool (K-1)
KafkaConsumer.poll → Fetcher → ConsumerCoordinator (K-2)
KafkaApis.handleProduceRequest → ReplicaManager.appendRecords → Partition.append
  → UnifiedLog.appendAsLeader → LocalLog.append → LogSegment.append (K-3 ✅ trace_path 实证)
KafkaApis.handleFetchRequest → ReplicaManager.fetchMessages → Partition.readRecords (K-4)
  → FetchSession 增量 (K-12 旁路)
KafkaApis.handleJoinGroup → GroupCoordinator (K-6 旁路)
PartitionStateMachine/ControllerChannelManager (K-5 旁路)
SocketServer (K-8 旁路: Acceptor/Processor 独立线程)
DelayedProduce/DelayedFetch → DelayedOperationPurgatory (K-7 旁路)
LogCleaner (K-10 旁路: 后台线程)
TransactionCoordinator/ProducerStateManager (K-11 旁路)
KRaft: QuorumController/raft/ (K-9 旁路)
```

### 三信号分类 (00 §3.5) — 与规划定级对照

| 域 | 定义特征测试 (00 §3.5) | 面试 | 生产 | Hub | 置信度 | 规划 |
|:--:|:--|:--:|:--:|:--:|:--:|:--:|
| K-3 Log | 无分段存储就不是 Kafka (append-only 内核) | 高频 | 主流 | ✅ (K-4/10/11/12 依赖) | 高 | 🔴 ✅ |
| K-4 ISR | 无 ISR 就不是分布式 Kafka | 高频 | 主流 | ✅ (K-5/6/12) | 高 | 🔴 ✅ |
| K-1 Producer | 客户端写入面定义特征 | 高频 | 主流 | ✅ (K-11) | 高 | 🔴 ✅ |
| K-2 Consumer | 客户端读取面定义特征 | 高频 | 主流 | ✅ (K-6/12) | 高 | 🔴 ✅ |
| K-6 Group | 消费组是 Kafka 定义特征 | 高频 | 主流 | 中 | 高 | 🔴 ✅ |
| K-5 Controller | KRaft 时代元数据中枢 | 高频 | 主流 | 中 | 高 | 🔴 ✅ |
| K-7 Purgatory | 延迟操作时间轮 (非定义特征但算法级) | 中频 | 主流 | 低 | 中 | 🟡 ✅ |
| K-8 网络 | SocketServer 三层 (Netty 对照面) | 中频 | 主流 | 低 | 中 | 🟡 ✅ |
| K-11 事务 | 幂等/两阶段 (定义特征边缘) | 高频 | 部分 | 低 | 中 | 🟡 ✅ |
| K-10 Compaction | 日志压缩 (Kafka 特有) | 中频 | 部分 | 低 | 中 | 🟡 ✅ |
| K-9 KRaft | 元数据共识 (Kafka 特有) | 中频 | 主流 | 低 | 中 | 🟡 ✅ |
| K-12 FetchSession | 增量拉取优化 (非定义特征) | 低频 | 主流 | 低 | 中 | 🟡 ✅ |

### 依赖图 (02 §1.2 强制, 补写)

```
K-3 Log (叶子) → K-4 ISR → K-12 FetchSession / K-7 Purgatory
K-3/K-4/K-7 → K-1 Producer (acks=all 等 Purgatory)
K-3/K-4/K-12 → K-2 Consumer
K-2/K-4 → K-6 Group
K-4/K-6 → K-5 Controller (KRaft)
K-3/K-6 → K-11 事务 (ProducerState)
K-3 → K-10 Compaction
K-5 → K-9 KRaft (元数据)
K-8 网络 (独立, 横切)
```

**循环依赖检测** (02 §1.4): Producer↔RecordAccumulator↔Sender (02 文档明示的 Kafka 例子) — K-1 域内循环, 三轮化解策略: 域内闭环处理; 域间无循环 ✅
**Hub 检查** (04 §Hub 升级): K-3 被 6 域依赖 (K-4/K-10/K-11/K-12/K-2), 未达 10 — 但保持 🔴 A 全深度 ✅

### 拓扑排序校验

执行序 **K-3 → K-4 → K-12 → K-7 → K-1 → K-2 → K-6 → K-5 → K-8 → K-11 → K-10 → K-9** — 每个域依赖都排在前面 ✅ (与 00 §4 拓扑一致; K-8 横切无依赖可插任意位, K-11 依赖 K-3/K-6 已满足)

## 一、域清单 (12 域: 7🔴 + 5🟡)

| # | 域 | 模块 (文件) | 核心主题 | 方案 |
|:--:|---|---|---|---|
| K-3 | **Log 存储** | storage/internals/log (76) | 分段 4 文件/稀疏索引/roll/恢复/ProducerState — **存储底座** | 🔴 A |
| K-4 | **Partition & ISR** | core kafka/cluster + server (ReplicaManager) | 副本状态机/ISR 扩缩/AbstractFetcherThread epoch 截断 4 规则 | 🔴 A |
| K-12 | **FetchSession** | core kafka/server + clients | 增量拉取会话/Epoch 机制 (R3 新增) | 🟡 B |
| K-7 | **Purgatory** | server-common (DelayedOperation) | 时间轮延迟操作/acks=all 等待 | 🟡 B |
| K-1 | **Producer** | clients producer (31) | send 流水线/RecordAccumulator/BufferPool/幂等 | 🔴 A |
| K-2 | **Consumer** | clients consumer (189) | poll 双模型 (Classic/Async)/Fetcher/Rebalance | 🔴 A |
| K-6 | **Consumer Group** | group-coordinator (82) | 双协议 (Classic/Modern KIP-848)/Assignor 体系 | 🔴 A |
| K-5 | **Controller** | core kafka/controller + metadata (175) | 状态机/Leader 选举/QuorumController (KRaft) | 🔴 A |
| K-8 | **网络层** | core kafka/network (SocketServer 1715) | Acceptor→Processor→Handler 三层 | 🟡 B |
| K-11 | **事务 & 幂等** | clients + group-coordinator + storage | 两阶段提交/TransactionMarker/ProducerState 衔接 | 🟡 B |
| K-10 | **Compaction** | storage (Cleaner 766) | SkimpyOffsetMap 双哈希/三阶段/原子 swap | 🟡 B |
| K-9 | **KRaft** | metadata (175) + raft (87) | QuorumController/MetadataImage/Raft 状态机 | 🟡 B |

## 二、执行顺序 (规划 §六, 存储→API→协调→扩展)

**K-3 → K-4 → K-12 → K-7 → K-1 → K-2 → K-6 → K-5 → K-8 → K-11 → K-10 → K-9**

> 拓扑理由: **K-3 Log 叶子** (存储底座, 无内部依赖, 先讲数据落盘 — 与 ES E-3 Translog 同构); **K-4 ISR** (消费 Log, 复制面); **K-12 FetchSession** (消费读路径); **K-7 Purgatory** (acks=all/fetch.min 的等待机制); **K-1/K-2** (客户端双端, 消费 broker 能力); **K-6 Group** (消费复制/消费路径); **K-5 Controller** (协调面); **K-8 网络** (传输面); **K-11 事务** (消费 ProducerState); **K-10 Compaction** (消费 Log 清理); **K-9 KRaft** (元数据共识收束)。

## 三、知识网络图 (双链)

| 来源 (已存在) | Kafka 侧 | 关系 |
|---|---|---|
| redis/r8-persistence | K-3/K-10 | 对照: AOF rewrite vs 段轮转/compaction |
| redis/r22-expire, r23-evict | K-3/K-10 | 对照: 惰性+主动过期 vs retention 删除 |
| es/e3-translog | K-3 | 对照: ES generation 轮转 vs Kafka segment roll; checkpoint vs recoveryPoint |
| es/e8-merge | K-3/K-10 | 对照: TieredMerge 合并 vs Kafka 无合并 (只清理) |
| es/e6-seqno | K-4/K-11 | 对照: seqNo+term vs offset+epoch |
| rocketmq (阶段4.1) | K-1/K-3/K-6 | 对照: RM CommitLog+ConsumeQueue vs Kafka 分段; RM 事务消息 vs K-11 |
| es/e4-routing | K-4 | 对照: 路由 vs 副本分配 |
| redisson/rd2-rlock | K-9 | 对照: fencing token vs KRaft epoch |

## 四、完成检查单

- [x] 顶层包扫描 ↔ 域清单覆盖矩阵 (12/12)
- [x] 数字断言穷举 (8/8 接受, 0 修正)
- [x] 淘汰表笔误修正 (core/network)
- [x] 时空溯源可行性 (fetch --unshallow 完成)
- [x] 交叉引用核验 (redis/es/rocketmq 目录 [ -d ] 待逐域核验)
- [ ] K-3~K-12 逐域交付 (K-3 ✅ 2026-08-15)
