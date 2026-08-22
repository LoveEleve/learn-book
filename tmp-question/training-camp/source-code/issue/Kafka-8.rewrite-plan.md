# Kafka-8 重写规划

> 题目：谁才是真正的 active Controller——KRaft 的选主、日志复制与 MetadataImage 主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka 在去掉 ZooKeeper 后，为什么还能保证集群始终只有一个 active Controller：Raft 如何选主、controller 的写入为什么必须先进元数据日志、standby controller / broker 又如何通过 MetadataLoader 从日志与 snapshot 重建 MetadataImage。

## 1. 读者困惑

- Kafka 去掉 ZooKeeper 以后，谁来决定“哪个 controller 说了算”？
- 为什么上一章说 controller 是唯一权威，但它自己又不算单点？
- controller 改元数据时，为什么不能直接改内存，非要先写 Raft log？
- standby controller 与普通 broker 怎么追上元数据，为什么大家看到的是同一份 MetadataImage？
- snapshot 在 KRaft 里是做什么的，为什么不是每次都从头回放全量日志？
- voter / observer、leader / follower、controller / broker 这几组角色到底怎么区分？

## 2. 一句话顿悟

**KRaft 把“谁是 controller”这个问题也放进一条共识日志里解决：Raft 先从 voters 中选出 metadata partition leader，它所在节点上的 QuorumController 才能成为 active Controller；所有元数据修改都先经 `appendWriteEvent` 生成记录并追加到元数据日志，提交后再由 standby controller 与 broker 通过 MetadataLoader 回放日志或加载 snapshot，重建出同一份 MetadataImage。**

## 3. 五要素卡片

### 读者问题

如果 controller 也是一个进程，那它挂了之后谁接班、又凭什么让全体 broker 都承认“新 controller 才是唯一权威”？

### 入口

- `KafkaRaftClient`：KRaft 共识实现，负责选主、复制、snapshot/fetch
- `EpochElection`：统计投票，判断是否过半
- `QuorumController`：Raft leader 所在节点上的 active controller
- `KRaftControlRecordStateMachine`：跟踪 voter set / kraft.version 等控制记录
- `MetadataLoader`：把日志 / snapshot 变成 `MetadataDelta` 与 `MetadataImage`
- `MetadataImage`：broker 侧可消费的元数据快照
- `BatchAccumulator`：leader 端把待提交元数据聚成批次

### 状态核心

- Raft 角色：leader / follower / candidate（voters 参与投票，observers 不投票）
- controller 角色：active / standby（只有 metadata partition leader 才能 active）
- 元数据形态：log → delta → image
- snapshot：替代“从 offset 0 回放到今天”
- `LeaderAndEpoch`：当前元数据日志 leader 与 epoch

### 失败路径

- 没有共识选主 → 两个 controller 同时活跃，脑裂
- controller 直接改内存不写日志 → 故障切换后所有元数据丢失
- standby 不回放日志 / 不加载 snapshot → 各节点看到的 metadata image 不一致
- follower 没追上日志就当 controller → 用旧视图决策，分区/配置全部错乱
- 不区分 voter 和 observer → 非投票节点也能影响主选举，破坏 quorum 安全性

### 连接点

- 前文 `Kafka-7`：Controller 负责“分区 leader 与 ISR 归属”；本篇补上“谁能成为这个唯一 controller”。
- 后文 `Kafka-9`：在有了 KRaft 元数据共识后，再回到 Partition/ISR 运行时状态机本身。
- 与前文 `Kafka-6` 呼应：ConsumerGroup 用 coordinator 决定组内归属；KRaft 用 Raft 决定 controller 归属。

## 4. 总图

```text
voters 组成 metadata quorum
  → 选出 metadata partition leader
    → 该节点上的 QuorumController 成为 active controller
      → appendWriteEvent 生成 metadata records
        → BatchAccumulator 聚批 → KafkaRaftClient 复制到 quorum
          → 过半提交
            → active controller 完成写请求
            → standby controller / broker 通过 MetadataLoader
              回放 commit 或加载 snapshot
                → 更新 MetadataDelta → 形成 MetadataImage
```

## 5. 关键边界

- 本篇只讲 KRaft 元数据共识，不展开 Controller 决策细节（上一章 Kafka-7 已讲）。
- 不把“Raft leader”与“分区 leader”混成一个概念：本篇的 leader 是 metadata partition leader。
- 不把 broker 都当 voter：只有 voters 参与投票，observer/broker 只消费日志。
- 不展开全部 Raft RPC 细节与动态 quorum 变更细节，只抓选主、提交、回放、snapshot 主链。

## 6. 失败方案推演

1. **controller 挂了后靠 broker 自己猜谁接班**：两个节点都自认 controller，脑裂。
2. **controller 直接改内存再异步广播**：崩溃后没有提交日志，standby 永远追不上真实状态。
3. **每次重启都从 offset 0 回放全量 metadata log**：恢复时间随运行时间线性增长，元数据规模一大就卡死。
4. **observer 也参与投票**：投票集合不稳定，quorum 安全性被破坏。

## 7. 误解清单

- "KRaft 只是把 ZooKeeper 的数据搬进 Kafka topic"：它还把 controller 选主与提交语义一起搬进 Raft。
- "Raft leader 就是任意业务分区 leader"：这里是 metadata partition 的 leader。
- "controller 改完内存再补日志也行"：提交日志先于对外生效，这是安全性的根。
- "snapshot 只是备份文件"：它是缩短恢复路径、替代全量回放的正式状态来源。
- "所有 broker 都参与 controller 投票"：只有 voters 投票，observer/broker 只追日志。

## 8. 证据清单

- `raft/src/main/java/org/apache/kafka/raft/KafkaRaftClient.java:128`：KRaft/Raft 主链与 RPC 注释。
- `raft/src/main/java/org/apache/kafka/raft/internals/EpochElection.java:91`：过半投票判断。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:931`：appendWriteEvent。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:954`：QuorumMetaLogListener 处理 commit。
- `metadata/src/main/java/org/apache/kafka/controller/QuorumController.java:1056`：handleLeaderChange。
- `raft/src/main/java/org/apache/kafka/raft/internals/BatchAccumulator.java:118`：leader 端聚批 append。
- `raft/src/main/java/org/apache/kafka/raft/internals/KRaftControlRecordStateMachine.java:42`：控制记录状态机。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:56`：日志/snapshot → delta/image。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:355`：handleCommit。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:375`：handleLoadSnapshot。
- `metadata/src/main/java/org/apache/kafka/image/MetadataImage.java:28`：broker metadata image。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦元数据 quorum、controller 选主、日志提交、snapshot/image 回放；不展开分区数据副本复制、ISR 运行时细节。
- 目标正文：8000~12000 字；核心拆解层覆盖选主、提交、回放、snapshot、image 一致性。

## 10. 本轮重写主线

1. 从“controller 挂了谁接班”开场，引出 KRaft 解决的是 controller 归属。
2. 否定“大家自己猜”与“先改内存后补日志”两种直觉方案。
3. 解释 voters / observers 与 metadata partition leader。
4. 解释 QuorumController 只有在 Raft leader 身份下才 active。
5. 解释 appendWriteEvent → BatchAccumulator → KafkaRaftClient → quorum commit 主链。
6. 解释 standby controller / broker 用 MetadataLoader 回放 commit 或 snapshot，生成同一份 MetadataImage。
7. 收网：KRaft 先决定“谁能做 controller”，Kafka-7 再决定“controller 如何改分区归属”。