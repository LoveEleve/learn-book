# Kafka-27 重写规划

> 题目：Broker 怎样看见同一份元数据——MetadataLoader、MetadataImage 与 Publisher 主链
> 状态：K-8 KRaft 域第 3 篇，按“MetadataLoader / MetadataImage / Publisher”展开
> 目标：解释 KRaft 下 broker 和 standby controller 如何从元数据日志 / snapshot 中生成同一份 `MetadataImage`：`MetadataLoader` 如何在事件队列中处理 commit、加载 snapshot、构造 `MetadataDelta` / `MetadataImage`，以及 `MetadataPublisher` 如何接收这份 image 的更新。这是 K-8 域第 3 篇。

## 1. 读者困惑

- standby controller / broker 是怎么追上 active controller 的元数据状态的？
- 为什么需要 `MetadataDelta` 和 `MetadataImage` 两套对象，而不是一份可变状态？
- 新节点刚起来时，怎么获得一份完整的元数据视图？
- commit 批次到了之后，怎么从 records 变成 Image？
- snapshot 加载和 commit 回放有什么区别？
- `MetadataPublisher` 在这里扮演什么角色？

## 2. 一句话顿悟

**`MetadataLoader` 是 KRaft 元数据的“翻译器”：它把 Raft 的 commit batch / snapshot 先变成 `MetadataDelta`，再生成不可变的 `MetadataImage`，最后按顺序回调所有 `MetadataPublisher`。standby controller 与 broker 不直接读 records 做业务，而是通过 Loader 看见同一份 image。**

## 3. 五要素卡片

### 读者问题

Raft 已经保证了元数据日志是一致的，为什么还要再做一层 `MetadataImage`？

### 入口

- `MetadataLoader`：commit/snapshot → delta/image → publishers
- `MetadataBatchLoader`：把批次积累成 delta
- `MetadataImage`：不可变元数据快照
- `MetadataDelta`：上一份 image 到下一份 image 的差量
- `MetadataPublisher`：broker / 组件消费 image 更新的接口
- `LeaderAndEpoch`：当前 active controller 信息

### 状态核心

- `image`：当前最新的 MetadataImage
- `catchingUp`：初始追赶阶段
- `highWaterMarkAccessor`：判断追赶是否完成
- `uninitializedPublishers` / `publishers`：待初始化/已初始化的 publisher 列表
- `LoaderManifest`：描述本次更新来自 commit 还是 snapshot

### 失败路径

- 直接让 broker 读 records 做业务 → 所有组件都重复实现重放逻辑
- 不用不可变 image → 读写共享状态，线程安全混乱
- 没有 catch-up 阶段 → publisher 在状态不完整时就开始消费
- snapshot 加载后不生成 delta → publisher 无法统一处理 snapshot 与 commit

### 连接点

- 前文 `Kafka-8`：KRaft 选主与元数据共识总览。
- 前文 `Kafka-26`：QuorumController 如何把请求变成 metadata log。
- 后文：K-9 ISR/Controller 运行时专题要消费这些 image。 

## 4. 总图

```text
Raft commit batch / snapshot
  → MetadataLoader.handleCommit / handleLoadSnapshot
    → MetadataBatchLoader 累积记录
      → MetadataDelta
        → MetadataImage
          → maybePublishMetadata
            → MetadataPublisher.onMetadataUpdate(delta, image, manifest)
```

## 5. 关键边界

- 本篇不重复 KRaft 选主（K-8 第 1 篇）或 QuorumController 请求执行链（K-8 第 2 篇）。
- 不把 MetadataLoader 与 QuorumController 混成同一层：前者是“日志到 image 的翻译器”，后者是“产生日志的业务执行器”。
- 不把 MetadataImage 写成可变对象：它是 immutable snapshot。
- 不展开所有 publisher 具体实现，只讲统一接口。

## 6. 失败方案推演

1. **所有 broker 自己读 records 自己解释**：每个模块各写一套重放器，一致性难保证。
2. **只维护一份可变全局状态**：线程安全和回放时序难以保证。
3. **publisher 一启动就接收 commit**：还没 catch up 完，状态不完整。
4. **snapshot 单独走完全不同路径**：publisher 需要写两套消费逻辑。

## 7. 误解清单

- “MetadataImage 就是 commit log” ：Image 是快照，log 是事件流。
- “Delta 和 Image 二选一即可” ：Delta 适合增量传播，Image 适合提供稳定视图。
- “publisher 直接消费 commit records” ：它消费的是 Loader 生成的 delta/image。
- “snapshot 加载时不会触发 publisher 更新” ：snapshot 也会转成 delta/image 再统一发布。
- “catchingUp 只是个状态位” ：它控制 publisher 何时开始看到元数据。

## 8. 证据清单

- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:56`：MetadataLoader 类注释。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:226`：stillNeedToCatchUp。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:260`：scheduleInitializeNewPublishers。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:272`：initializeNewPublishers。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:325`：maybePublishMetadata。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:355`：handleCommit。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:375`：handleLoadSnapshot。
- `metadata/src/main/java/org/apache/kafka/image/MetadataImage.java:28`：MetadataImage。
- `metadata/src/main/java/org/apache/kafka/image/publisher/MetadataPublisher.java:26`：MetadataPublisher 接口。

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦 MetadataLoader / MetadataImage / Publisher，不展开所有 Image 结构体细节。
- 目标正文：6000~10000 字。

## 10. 本轮重写主线

1. 从"broker 怎么看见和 active controller 一样的元数据"开场。
2. 否定“直接读 records 做业务”和“用可变全局状态”两种方案。
3. 解释 MetadataDelta / MetadataImage 两层。
4. 解释 MetadataLoader 的 catchingUp、commit、snapshot 路径。
5. 解释 initializeNewPublishers / maybePublishMetadata。
6. 解释 MetadataPublisher 的统一接口。
7. 收网：MetadataLoader 是日志到 image 的翻译器。