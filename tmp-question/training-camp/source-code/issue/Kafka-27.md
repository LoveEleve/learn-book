# Kafka-27. Broker 怎样看见同一份元数据——MetadataLoader、MetadataImage 与 Publisher 主链

> 场景：Kafka-8 讲了 KRaft 如何选主和复制日志，Kafka-26 又讲了 QuorumController 如何把写请求变成元数据日志。但对普通 broker 来说，它并不直接消费那一条条 records，而是需要一份**随 offset 演进、但对外稳定可读**的元数据视图。这就是 `MetadataLoader`、`MetadataDelta`、`MetadataImage` 和 `MetadataPublisher` 这一层要解决的问题。

## 先把真正的困惑摆出来：broker 怎么看见和 active controller 一样的元数据

假设 active controller 刚完成一个 `createTopic`：它已经把一条 `TopicRecord` 写进了 metadata log。现在还有两个问题：

- standby controller 怎么把这条记录变成它自己的内存状态？
- 普通 broker 怎么把这条记录变成自己可读的“topic 列表 + 分区分配 + broker 列表”等元数据视图？

一个最直觉的做法是：让每个使用者都自己去读 commit records，自行回放出状态。看起来省掉了一层抽象，但马上会出问题：

- controller 要写一套回放逻辑；
- broker 再写一套；
- metrics/publisher 再写一套；
- snapshot 和 commit 的路径还得重复实现两遍。

Kafka 的选择是：**把“records → 可读元数据视图”的工作抽成一层统一翻译器：MetadataLoader。**

*关键设计（斜体）：* *MetadataLoader 并不自己做业务决策，它只负责把 Raft commit batch / snapshot 先变成 `MetadataDelta`，再生成不可变的 `MetadataImage`，最后按顺序回调 `MetadataPublisher`。这样所有 broker / standby controller / publisher 都消费同一份 image，而不是各自重复读 records。*[模式: commit/snapshot → delta → image → publisher]

## 第一层：MetadataImage 是一份不可变的 broker 元数据快照

`MetadataImage` 的类注释第一句就说了：**The broker metadata image**。它不是日志，不是事件流，而是“某个 offset 对应的完整集群元数据快照”。

它内部包含：

- `features`：feature flags / metadataVersion
- `cluster`：broker、controller 注册信息
- `topics`：topic 与 partition 元数据
- `configs`
- `clientQuotas`
- `producerIds`
- ACL / SCRAM / delegationTokens

并且还带着 `MetadataProvenance`，记录：

- `lastContainedOffset`
- `lastContainedEpoch`

这让 Image 本身就有了“我代表哪一版元数据”的身份证。

为什么要不可变？因为 broker 上有很多读路径同时依赖元数据：路由、quota、controller 监听、metrics。与其让它们共享一份会被修改的对象，不如每次生成一份新的 image，让所有读路径都拿到一个稳定版本。

## 第二层：MetadataDelta 是 image 之间的差量

如果每次 commit 都直接生成一整份新的 MetadataImage，然后让所有组件整份替换，成本很高。Kafka 选择了中间层：`MetadataDelta`。

它的意义是：**从上一份 image 到下一份 image 的差量**。这样：

- 组件如果只关心变化，可以看 delta；
- 组件如果需要完整视图，可以拿 newImage；
- snapshot 与 commit 都能统一成“delta + image”的输出格式。

```text
oldImage + commitRecords
  → MetadataDelta
    → apply
      → newImage
```

这是 `MetadataLoader` 的核心价值：它把日志事件流转换成稳定的差量和快照。

## 第三层：MetadataLoader 的 catchingUp——没追上前，先别让 publisher 看见不完整状态

`MetadataLoader` 启动后，并不是立刻把第一条 commit 就推给 publisher。它有一个 `catchingUp` 阶段。

`stillNeedToCatchUp()` 会检查：

- 当前是否还在 catchingUp 模式；
- 高水位（high watermark）是否已知；
- 当前加载到的 offset 是否已经追上高水位；
- 有没有至少看见过一条 controller record。

```text
stillNeedToCatchUp(where, offset)
  → highWaterMark 已知？
  → 当前 offset >= HWM-1？
  → 已看见 controller record？
    → 全满足：catchingUp=false
      → 才允许 publisher 初始化/看到更新
```

注意最后一个条件：即使 offset 已经追上了高水位，但只要这套 batch loader **还没见过任何一条 controller record**，`stillNeedToCatchUp()` 仍然返回 true。这是因为"追到 offset"只说明位置到了，而不代表这里真的有一份可发布的 controller 状态。

这一步非常关键：如果 broker 还没追上高水位，就让 publisher 开始读 metadata，那它看到的是一份不完整的集群状态，可能连 controller 信息都还没齐。

所以 `catchingUp` 不是一个普通的布尔开关，而是一个安全边界：**没追到高水位之前，不对外发布元数据。**

## 第四层：handleCommit——commit batch 先交给 MetadataBatchLoader，再决定是否 publish

当 Raft 层通知一批 records 已经 commit 时，`MetadataLoader.handleCommit()` 做的不是自己逐条做业务逻辑，而是把 batch 交给 `MetadataBatchLoader.loadBatch()`，让它累积并生成 delta。

```text
handleCommit(reader)
  → eventQueue.append(...)
    → while (reader.hasNext())
      → batchLoader.loadBatch(batch, currentLeaderAndEpoch)
    → batchLoader.maybeFlushBatches(currentLeaderAndEpoch, true)
```

也就是说：

- `MetadataLoader` 自己不是那个“逐条理解 record 含义”的对象；
- 它更像一个控制器：收 commit → 交给 batchLoader → 统一 flush → 进入 publish。

最终 `batchLoader` 会回调 `maybePublishMetadata(delta, image, manifest)`，真正对外发布。

## 第五层：handleLoadSnapshot——snapshot 也走同一条 publish 主线

snapshot 不是另一套完全独立的体系。`handleLoadSnapshot()` 也会：

1. 读取 snapshot batches；
2. 生成 `MetadataDelta` / `MetadataImage`；
3. 统一走 `maybePublishMetadata()`。

所以对 publisher 来说，snapshot 与 commit 的差别只是 `LoaderManifest` 的类型不同：

- `SnapshotManifest`
- `LogDeltaManifest`

真正的 `onMetadataUpdate(delta, image, manifest)` 接口是一致的。

这正是 `MetadataPublisher` 接口存在的意义：上层组件不必关心“这份状态是从 commit 来的还是 snapshot 来的”，它只关心“我拿到了一份 delta 和一份新的 image”。

## 第六层：新 publisher 怎么补课——initializeNewPublishers

MetadataLoader 还解决了一个很实际的问题：如果某个 `MetadataPublisher` 在系统运行一段时间后才注册，它不能从 offset 0 把整个日志回放一遍。

Kafka 的做法是：给新 publisher 一份基于当前 image 的**补课快照**。

`initializeNewPublishers()` 做的事情是：

```text
uninitializedPublishers
  → stillNeedToCatchUp ? 还没追平就延后
  → 构造一个基于 MetadataImage.EMPTY 的空 MetadataDelta
    → image.write(writer, ...) 把当前 image 重写进这个 delta
      → SnapshotManifest
        → publisher.onMetadataUpdate(delta, image, manifest)
        → publisher.onControllerChange(currentLeaderAndEpoch)
```

注意这里**不是把 image 指针直接交给 publisher**，而是先基于 `MetadataImage.EMPTY` 建一个空 delta，再把当前 image 重写进去，生成一份“从 EMPTY 一直补到当前 image”的 catch-up delta。这样 publisher 看到的接口形式，和它后续正常收到 commit delta 时完全一致。

这一步让新 publisher 不需要自己理解“我漏了哪些 commit”，而是直接拿到一份当前完整 image（以 delta 形式表述）。

## 第七层：MetadataPublisher——所有使用者都通过同一个接口拿元数据

`MetadataPublisher` 非常简单：

- `onControllerChange(LeaderAndEpoch)`
- `onMetadataUpdate(MetadataDelta, MetadataImage, LoaderManifest)`

它把使用者屏蔽在 records 和 snapshot 细节之外：

- controller 注册信息变化 → `onControllerChange`
- 任何元数据变化（commit 或 snapshot）→ `onMetadataUpdate`

`onControllerChange` 并不是只在正常 leader 切换时才触发：新 publisher 在 `initializeNewPublishers()` 里也会被补发一次 controller change 回调，确保它的 controller 视图与 `currentLeaderAndEpoch` 对齐。

所以 broker 内部真正依赖元数据的组件都不需要自己去理解 metadata log，它们只要实现 `MetadataPublisher` 接口即可。

## 收网：MetadataLoader 是 commit/snapshot 到 image 的翻译器

把整篇压成一句话：KRaft 下 broker 与 standby controller 不是直接消费 metadata records 做业务，而是通过 `MetadataLoader` 把 commit/snapshot 统一翻译成 `MetadataDelta` 和不可变的 `MetadataImage`，再按顺序回调所有 `MetadataPublisher`。`catchingUp` 保证没追平高水位前不对外发布，`initializeNewPublishers` 让新组件能直接补课到当前 image。

```text
Raft commit / snapshot
  → MetadataLoader
    → MetadataBatchLoader
      → MetadataDelta
        → MetadataImage
          → maybePublishMetadata
            → MetadataPublisher.onMetadataUpdate
```

到这里，主线只发生了六件事。

第一，MetadataImage 是 broker 看到的不可变元数据快照。

第二，MetadataDelta 是 image 之间的差量。

第三，MetadataLoader 的 catchingUp 保证追平高水位前不对外发布。

第四，commit batch 先交给 batchLoader，再统一 flush / publish。

第五，snapshot 也通过 delta/image 主线统一发布。

第六，MetadataPublisher 让所有使用者通过统一接口消费元数据。

**本篇的一句话困惑**：broker 怎么看见和 active controller 一样的元数据？

**本篇的一句话顿悟**：broker 不直接消费元数据记录，而是通过 MetadataLoader 把 commit/snapshot 统一翻译成 MetadataDelta 与 MetadataImage，再由 MetadataPublisher 顺序消费同一份不可变视图。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“MetadataImage 就是 metadata log。”** Image 是快照，log 是事件流。
2. **“Delta 和 Image 二选一即可。”** Delta 适合增量传播，Image 适合稳定视图。
3. **“publisher 直接消费 commit records。”** 它消费的是 Loader 生成的 delta/image。
4. **“snapshot 加载不影响 publisher。”** snapshot 也会触发统一的 onMetadataUpdate。
5. **“catchingUp 只是个状态位。”** 它决定 publisher 何时能安全看到元数据。

### 关键证据清单

- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:56`：MetadataLoader 类注释。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:226`：stillNeedToCatchUp。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:260`：scheduleInitializeNewPublishers。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:272`：initializeNewPublishers。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:325`：maybePublishMetadata。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:355`：handleCommit。
- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:375`：handleLoadSnapshot。
- `metadata/src/main/java/org/apache/kafka/image/MetadataImage.java:28`：MetadataImage。
- `metadata/src/main/java/org/apache/kafka/image/publisher/MetadataPublisher.java:26`：MetadataPublisher 接口。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 为基线。
- 本篇聚焦 MetadataLoader / MetadataImage / Publisher，不展开所有 Image 结构体细节。
- 不把 MetadataLoader 与 QuorumController 混成同一层：前者翻译日志，后者产生日志。
- 不展开所有 publisher 具体实现，只讲统一接口。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-8`（KRaft 总览）、`Kafka-26`（QuorumController 与元数据日志主链）。
- 后续桥接：K-8 KRaft 域 3 篇收官。下一篇可切回 K-9 ISR 子专题，或继续全篇一致性收束。