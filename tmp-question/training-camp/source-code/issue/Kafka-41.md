# Kafka-41. 路由怎么知道该找谁——KRaftMetadataCache、Publisher 与路由查询主链

> 场景：Producer 发消息要知道某个 partition 的 leader 在哪，事务 marker、controller 通道也要知道该连哪个 broker。但这些查询并不是每次都直接问 controller，而是落到 broker 本地的 `KRaftMetadataCache`。本篇补齐这条主线：MetadataImage 怎么进入 cache，cache 里到底放了什么，路由查询又怎么从 cache 里拿结果。

## 先把真正的困惑摆出来：MetadataCache 到底是谁更新的

如果只看名字，很容易误以为 `MetadataCache` 自己实现了 `onMetadataUpdate`，由 `MetadataLoader` 直接调用它更新缓存。

但真实链路不是这样。

```text
MetadataLoader
  → 回调 MetadataPublisher
    → publisher 把 newImage 发布进 KRaftMetadataCache
      → 各请求路径读取 cache 做路由判断
```

*关键设计（斜体）：* *`KRaftMetadataCache` 本身是一个读视图容器，不是直接接收 loader 回调的 publisher；真正承接 `onMetadataUpdate` 的是 `KRaftMetadataCachePublisher` / `BrokerMetadataPublisher` 这类 publisher，它们拿到新的 `MetadataImage` 后再调用 `metadataCache.setImage(newImage)`，把不可变 image 变成 broker 本地可查询的元数据视图。*[模式: 不可变 MetadataImage + Publisher 分发 + 本地路由读视图]

## 第一层：`KRaftMetadataCache` 缓存的是“路由视图”，不是所有运行态

`KRaftMetadataCache` 缓存的重点不是业务状态，而是 broker 做路由与 leader 判定时必须快速读取的元数据视图，包括：

- broker 注册信息（节点、listener、rack、epoch 等）；
- topic / partition 的元数据；
- `LeaderAndIsr`、leader endpoint 等路由判断需要的信息。

它的价值在于：broker 可以在本地直接回答“某个 topic-partition 当前 leader 是谁、这个 listener 下该连哪个 endpoint”。

## 第二层：真正接 `onMetadataUpdate` 的是 publisher，不是 cache 本身

`MetadataLoader` 在 apply delta 之后，会遍历 metadata publishers，调用它们的 `onMetadataUpdate(delta, image, manifest)`。

对 metadata cache 这条链来说，关键中间层是：

- `KRaftMetadataCachePublisher`
- `BrokerMetadataPublisher`

其中 `KRaftMetadataCachePublisher.onMetadataUpdate` 的逻辑非常直接：

```scala
override def onMetadataUpdate(...) = {
  metadataCache.setImage(newImage)
}
```

而 `BrokerMetadataPublisher` 则在 broker 启动阶段把多个 publisher 串起来，并明确写了“先 publish 到 metadata cache，再做其他后续动作”的顺序。

所以这里最重要的边界是：**loader 更新的是 publisher，publisher 再更新 cache。**

## 第三层：为什么是 `MetadataImage`，不是原地改 HashMap

KRaft 的元数据主线依赖不可变的 `MetadataImage`。publisher 收到的不是“某几个字段变了”的散乱事件，而是一个已经收敛好的新 image。

这样做的好处是：

- cache 切换的是一整份一致的元数据视图；
- 请求线程读取时不会看到“半更新状态”；
- broker 可以把“更新”和“查询”清晰分层：publisher 负责换 image，cache 负责回答查询。

## 第四层：路由查询到底查什么

`MetadataCache` 接口里最直接的两个查询是：

- `getLeaderAndIsr(topic, partitionId)`
- `getPartitionLeaderEndpoint(topic, partitionId, listenerName)`

前者回答控制面/副本层更关心的 leader 与 ISR 视图；后者回答网络路由层更关心的“这个 listener 下该连哪个 Node”。

这两个方法在 `KRaftMetadataCache` 里都有具体实现，所以很多请求路径并不是“查 cache 得到一坨 topic metadata 然后自己解析”，而是**直接调用有语义的方法拿结果**。

## 第五层：哪些路径会读它

`KafkaApis` 里处理 produce / fetch / 元数据相关请求时，会直接查 `metadataCache.getLeaderAndIsr(...)` 做 leader 判定或兜底。

另一个很典型的例子是事务 marker 通道：`TransactionMarkerChannelManager` 会调用 `metadataCache.getPartitionLeaderEndpoint(...)`，决定把 marker 发给哪个 broker。

这说明 `KRaftMetadataCache` 不只是“客户端 metadata 请求才会用到”，而是 broker 内部多条链路共享的本地路由底座。

## 收网：MetadataCache 是本地路由读视图，Publisher 才是更新入口

把整篇压成一句话：`MetadataLoader` 不会直接回调 `MetadataCache`，而是先把新的 `MetadataImage` 发给 `KRaftMetadataCachePublisher` / `BrokerMetadataPublisher` 这类 publisher；publisher 再通过 `metadataCache.setImage(newImage)` 更新 broker 本地的 `KRaftMetadataCache`；produce/fetch/事务 marker 等请求路径随后通过 `getLeaderAndIsr`、`getPartitionLeaderEndpoint` 等接口直接读取这份本地路由视图。

```text
MetadataLoader
  → onMetadataUpdate(delta, newImage, manifest)
    → KRaftMetadataCachePublisher / BrokerMetadataPublisher
      → metadataCache.setImage(newImage)
        → KRaftMetadataCache
          → getLeaderAndIsr / getPartitionLeaderEndpoint
            → produce / fetch / marker 路由
```

**本篇的一句话困惑**：Kafka 的元数据缓存到底是谁更新的，路由查询又是怎么落到这份缓存上的？

**本篇的一句话顿悟**：KRaftMetadataCache 自己不是 metadata publisher；真正接收 MetadataLoader 回调的是 publisher，它们把新的 MetadataImage 发布进 cache，随后各条请求路径直接从本地 cache 读取 leader 与 endpoint 路由信息。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“MetadataCache 自己实现了 onMetadataUpdate。”** 真正接 loader 回调的是 publisher。
2. **“每次请求都问 controller。”** broker 大量路由查询都先读本地 cache。
3. **“MetadataCache 和 MetadataImage 是一回事。”** image 是不可变快照，cache 是围绕 image 的本地查询视图。
4. **“MetadataCache 只给客户端 metadata 请求用。”** produce/fetch/marker 等内部路径也直接读它。
5. **“更新 cache 就是原地改几个 map。”** KRaft 主线更接近“发布整份新 image，再切换本地视图”。

### 关键证据清单

- `metadata/src/main/java/org/apache/kafka/image/loader/MetadataLoader.java:308`：调用 publisher `onMetadataUpdate(...)`。
- `core/src/main/scala/kafka/server/metadata/KRaftMetadataCachePublisher.scala:30`：接收回调并 `setImage(newImage)`。
- `core/src/main/scala/kafka/server/metadata/BrokerMetadataPublisher.scala:130`：先 publish 到 metadata cache。
- `core/src/main/scala/kafka/server/metadata/KRaftMetadataCache.scala:362`：`getLeaderAndIsr(...)`。
- `core/src/main/scala/kafka/server/metadata/KRaftMetadataCache.scala:381`：`getPartitionLeaderEndpoint(...)`。
- `core/src/main/scala/kafka/server/KafkaApis.scala:325`：请求路径读取 `metadataCache.getLeaderAndIsr(...)`。
- `core/src/main/scala/kafka/coordinator/transaction/TransactionMarkerChannelManager.scala:388`：marker 路由读取 `getPartitionLeaderEndpoint(...)`。

### 版本与实现边界

- 本文以 Kafka `v4.x`、KRaft 为基线。
- 本篇聚焦 broker 本地 metadata cache 的更新与查询，不展开 controller 侧 image 生成细节。
- 不把 `MetadataImage`、publisher、cache 三层压成一层。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-27`（MetadataLoader / Image / Publisher）、`Kafka-39`（启动装配）。
- 后续桥接：可继续补 `Kafka-45`（Broker 集群感知）与控制器路由链。