# Kafka-5. Consumer 为什么不需要每次 fetch 都带所有分区——FetchSession、Incremental Fetch 与 Epoch 主链

> 场景：上一篇已经把 KafkaConsumer 的 `poll()` 主链讲清了：Fetcher 会按当前分区和 position 发送 FetchRequest，再把响应放进 FetchBuffer。走到这里，一个很现实的问题自然浮现：如果一个 Consumer 订阅了成百上千个分区，那它每次 fetch 是不是都要把全部分区的元数据都带上？
>
> 本篇只回答一个问题：**Consumer 为什么不需要每次 fetch 都带所有分区。** 本篇聚焦 Kafka 的 FetchSession 机制：`FetchSessionHandler`、Full/Incremental Fetch、`INITIAL_EPOCH / FINAL_EPOCH / INVALID_SESSION_ID`、变更集合和失败回退；不展开 KIP-848 或 ConsumerGroup 成员协调，也不把 server 端 `CachedPartition` 写成消息存储。

## 先把真正的困惑摆出来：为什么不能每次 fetch 都全量带所有分区

从最简单角度看，Consumer 向某个 broker 发 fetch 时，只要把它订阅的、由该 broker 负责的分区全部写进请求，似乎最直接：

```text
FetchRequest
  → 列出所有 topic-partition
    → 每个分区带上 fetch offset 等参数
      → 等 broker 返回
```

当分区数量很少时，这确实可行。一旦分区数量上来，问题就变了。

设想一个 Consumer 订阅了 1000 个分区，而每次 fetch 都要把这 1000 个分区的元数据完整列出。那么：

- 请求体在每次 poll 中都会线性膨胀；
- 大多数分区其实没有变化，只有 offset 前进一点；
- broker 收到的绝大多数请求字节都是重复的分区元数据；
- 网络、序列化、broker 解析成本都会被无意义放大。

这就是 FetchSession 要解决的第一个问题：**如何让客户端和 broker 对“当前会话包含哪些分区”形成一份缓存共识，而不是每次请求都重新携带全部。**

```text
首次 Full Fetch 注册全部分区
  → 之后 Incremental Fetch 只带变更
    → epoch 保证两端视图同步
      → 异常时回退 Full Fetch
```

*关键设计（斜体）：* *FetchSession 真正补的是“分区请求视图的增量同步”：客户端和 broker 先在 session 上对齐分区集合，之后每次请求只携带变更，用 epoch 维持视图版本，失败时回退 Full 重新对齐。*[模式: 会话分区缓存 + 增量变更 + 版本失配回退]

## 第一层：`FetchSessionHandler` 先维护一份“这个 broker 会话里有哪些分区”的本地视图

`FetchSessionHandler` 在客户端对应某个 broker 维护一个 fetch session。它最关键的状态有两块：

- `sessionPartitions`：当前 session 里包含哪些分区，以及各自最新的请求参数；
- `nextMetadata`：下一次请求要携带的 fetch metadata。

有了这份本地视图，客户端才能在每次 fetch 前判断：这次是要 Full 全量注册，还是只要增量变更。

`sessionPartitions` 不是消息缓存，也不是 offset 存储；它保存的是**分区请求参数的缓存视图**。真正要发的消息数据仍然由 broker 按 offset 从日志读取。

如果没有这份本地视图，主链会先在哪失败？客户端根本没法知道：

- 哪些分区已经在这个 broker 的 session 里；
- 哪些分区需要新增；
- 哪些分区的参数已经变化；
- 哪些分区不再需要监听，要 remove。

所以 `FetchSessionHandler` 是 FetchSession 的客户端状态核心：它让“增量发送”成为可能，因为它知道全量视图是什么。

## 第二层：Full Fetch 是 session 的建立，Incremental Fetch 是 session 的持续维护

`FetchSessionHandler.Builder.build()` 会根据 `nextMetadata.isFull()` 决定请求形态。

### Full Fetch

当 `isFull()` 为 true 时，客户端把 `sessionPartitions` 整体作为 `toSend`，不带 added/removed/replaced 差分。它表达的是：**请用这一整组分区作为当前 session 视图。**

这里要补一个容易忽略的边界：`isFull()` 并不只在建立 session 时为 true，`FINAL_EPOCH` 也会让 `isFull()` 返回 true。也就是说，full 分支既可能发生在“建立/重建 session”，也可能发生在“用 FINAL 关闭 session”。所以 full 请求不一定是建立，它还可能是收尾。

真正通常在 full 分支发生的建立场景包括：

- 会话刚开始；
- 之前失败导致需要重新对齐；
- 客户端检测到视图失配。

### Incremental Fetch

当不是 full 时，客户端会在 `build()` 里做差分：

```text
新增分区   → added
参数变化   → altered
不再监听   → removed
topic ID 变更 → replaced
```

然后只把这些变更放进请求，同时附带 session 对应的 metadata。

所以 Full 和 Incremental 不是两种协议，而是同一个 fetch session 在“建立”和“持续维护”两个阶段的不同请求形态。

如果每次都用 Full Fetch，主链会先在哪退化？会话缓存和增量同步的收益就完全消失了，请求体又会回到全量携带所有分区的状态。

## 第三层：Incremental Fetch 靠“变更集合”而不是“全量副本”表达这次请求

增量请求真正聪明的地方，是不再重复整个分区集合，而是只表达相对 session 视图的差异。

`FetchRequestData` 携带几类分区：

- `toSend`：本轮实际要获取数据的分区；
- `toForget`：不再属于 session 的分区；
- `toReplace`：topic ID 变更、需要被替换的分区；
- `sessionPartitions`：客户端当前认为的完整 session 视图，用于让 broker 校验。

这里很容易出现一个误解：以为增量 fetch 就是把 `toSend` 发过去就完了。实际上，参数变化、分区移除、topic ID 替换都需要显式表达，否则 broker 会沿用旧视图里的参数。

如果只发 `toSend` 而不表达 altered/removed/replaced，主链会先在哪出错？

- 某个分区的 fetch offset 变了，但 broker 还在用旧参数；
- 某个分区不再被监听，但 session 里仍然留着它；
- topic ID 变化后，旧 ID 无法匹配新分区。

所以 Incremental Fetch 不是“少发一点参数”，而是用一套差分语义精确描述“session 视图应该怎么变”。

## 第四层：epoch 不是普通序号，而是 session 视图版本

`FetchMetadata` 定义了几个关键常量：

- `INVALID_SESSION_ID = 0`：没有 session 的客户端占位值，首次 Full Fetch 就带着它；
- `INITIAL_EPOCH = 0`：首次 Full Fetch 使用的初始 epoch；
- `FINAL_EPOCH = -1`：关闭现有 session 且不创建新 session 的结束标记；
- `nextEpoch(prevEpoch)`：把上一次成功 epoch 推进为下一次请求的 epoch。

这些常量最容易被误读成“一个不断+1的普通计数”。实际上，epoch 更接近**session 视图版本**。但这里要拆成两条线：

```text
正常推进线
  INITIAL_EPOCH
    → 第一次 Full Fetch
      → server 分配 sessionId 并缓存视图
        → 后续 Incremental Fetch 用 nextEpoch 逐次推进

关闭线
  FINAL_EPOCH
    → 关闭现有 session，不创建新 session
```

两条线都走同一个 `FetchMetadata`，但 FINAL 不属于正常推进链；它是收尾标记。为什么不能只看 sessionId？因为同一个 sessionId 可能对应不同版本的缓存视图。epoch 让 broker 能判断：这条请求是不是基于我当前缓存的同一版本视图发出来的。

如果请求的 epoch 与 broker 缓存版本不一致，broker 可以返回 `INVALID_FETCH_SESSION_EPOCH`，让客户端重新对齐。

所以 epoch 不是进度编号，而是 session 视图的版本标识。

## 第五层：session 丢失或 epoch 失配时，增量请求必须回退到 Full Fetch

增量 fetch 能省请求体，但它有一个前提：broker 端确实还认识这个 session。

当 broker 返回 `FETCH_SESSION_ID_NOT_FOUND` 或 `INVALID_FETCH_SESSION_EPOCH` 时，客户端都必须回到 Full Fetch。但这两个错误的原因并不相同：

- `FETCH_SESSION_ID_NOT_FOUND`：server 端根本没有这个 session，可能因为过期或被清理；
- `INVALID_FETCH_SESSION_EPOCH`：session 还在，但请求携带的视图版本与 server 缓存不一致。

两者都说明当前增量请求已经不能安全继续，区别在于一个是“session 不存在”，一个是“视图版本失配”。此时客户端不能继续尝试“再发一次增量”，而是要回到 Full Fetch，把全部分区重新注册成新 session。

```text
增量失败 / session 丢失 / epoch 失配
  → 回到 Full Fetch
    → 重新对齐 session 视图
      → 再进入增量
```

这个回退是 FetchSession 的兜底路径，也是它为什么仍然安全的原因。

如果把失配当成“网络抖动重发一次”，主链会先在哪失效？客户端会不断用错误 sessionId 或错误 epoch 重试，broker 每次都拒绝，双方永远无法重新建立一致视图。

所以增量 fetch 省字节的前提，是系统始终保留一条“回到 Full 重新对齐”的路径。

## 第六层：`FINAL_EPOCH` 表示关闭 session，不是普通 next epoch

很多人看到 `FINAL_EPOCH = -1` 会自然把它想成一个“很大的结束序号”。其实它是一个明确的关闭标记。

当客户端不再需要某个 broker 的 fetch session 时，会使用 FINAL_EPOCH 发送请求，告诉 broker 清理对应 session 缓存。

这和 `INITIAL_EPOCH` 一样，是 FetchMetadata 生命周期里的特殊状态：

- 用 INITIAL 开始；
- 用 nextEpoch 推进；
- 用 FINAL 关闭。

如果不把 FINAL_EPOCH 单独识别为关闭标记，主链会先在哪出问题？客户端关闭时 broker 可能一直保留旧 session 缓存，长期占用内存；下一次客户端重连时又带着旧 sessionId 出现，造成状态混乱。

所以 `FINAL_EPOCH` 不是“下一个很大的 epoch”，而是 session 生命周期的收尾语义。

## 收网：FetchSession 是把分区请求视图在两端的缓存共识，用增量变更和 epoch 维持同步

如果把整篇压成一句话，Kafka 的 FetchSession 不是“少发一点参数”的技巧，而是让客户端和 broker 对“这个 broker 会话里有哪些分区、每个分区参数是什么”达成一份缓存共识：首次用 Full Fetch 注册全部分区，之后用 Incremental Fetch 只带变更，并用 `INITIAL_EPOCH → nextEpoch` 维持视图版本推进，session 丢失或 epoch 失配时回退 Full 重新对齐，最后用 `FINAL_EPOCH` 关闭 session。

```text
Consumer 首次 fetch
  → INITIAL / Full Fetch 注册所有分区
    → server 分配 sessionId
      → Incremental Fetch 只带 added/altered/removed/replaced
        → epoch 逐次推进
          → 失配 / 丢失 → 回退 Full Fetch
            → FINAL_EPOCH 关闭 session
```

到这里，主线只发生了五件事。

第一，`FetchSessionHandler` 先维护一份分区请求视图，才能做增量。

第二，Full Fetch 建立 session，Incremental Fetch 维护 session。

第三，增量请求用变更集合表达差异，而不是用全量副本。

第四，epoch 是 session 视图版本，不是普通序号。

第五，session 丢失或 epoch 失配必须回退 Full 重新对齐，`FINAL_EPOCH` 负责关闭 session。

**本篇的一句话困惑**：Consumer 为什么不需要每次 fetch 都带所有分区？

**本篇的一句话顿悟**：因为 Kafka 用 FetchSession 把“这个 broker 会话包含哪些分区、参数是什么”在客户端和 broker 两端缓存成一份共识；之后每次 fetch 只带变更，并用 epoch 维持版本，失配时回退 Full 重新对齐。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Full 和 Incremental 是两种不同协议。”** 它们是同一 session 的建立与维护形态。
2. **“增量 fetch 只发 toSend 就够了。”** 还要表达 altered / removed / replaced。
3. **“epoch 就是一个不断加 1 的计数。”** 它是 session 视图版本标识。
4. **“`FINAL_EPOCH` 是最后一个 next epoch。”** 它是关闭 session 的标记。
5. **“session 丢失后重发一次增量就行。”** 需要回退 Full Fetch 重新对齐。

### 关键证据清单

- `clients/src/main/java/org/apache/kafka/clients/FetchSessionHandler.java:60`：客户端为每个 broker 维护 fetch session。
- `clients/src/main/java/org/apache/kafka/clients/FetchSessionHandler.java:95`：`FetchRequestData` 的 toSend/toForget/toReplace/sessionPartitions 结构。
- `clients/src/main/java/org/apache/kafka/clients/FetchSessionHandler.java:275`：`build()` 按 Full/Incremental 构造请求。
- `clients/src/main/java/org/apache/kafka/common/requests/FetchMetadata.java:31`：`INVALID_SESSION_ID = 0`。
- `clients/src/main/java/org/apache/kafka/common/requests/FetchMetadata.java:37`：`INITIAL_EPOCH = 0`。
- `clients/src/main/java/org/apache/kafka/common/requests/FetchMetadata.java:43`：`FINAL_EPOCH = -1`。
- `clients/src/main/java/org/apache/kafka/common/requests/FetchMetadata.java:91`：`isFull()` 判断 Full/Incremental。
- `clients/src/test/java/org/apache/kafka/clients/FetchSessionHandlerTest.java:215`：Full Fetch 使用 INITIAL_EPOCH 的测试证据。
- `clients/src/test/java/org/apache/kafka/clients/consumer/internals/FetcherTest.java:335`：`FINAL_EPOCH` 表示关闭 session 的测试证据。

### 版本与实现边界

- 本文以 Kafka `v4.x, KRaft` 路线为基线。
- 本篇聚焦客户端 `FetchSessionHandler` 与 `FetchMetadata`；server 侧 `FetchSession` / `CachedPartition` 只作为边界提示。
- 本文不展开 ConsumerGroup / KIP-848 协调，也不把 server 分区缓存写成消息存储。
- 本文把 Full/Incremental、变更集合、epoch 生命周期都视为 FetchSession 主链，而非参数细节。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-4` 的 Consumer/Fetcher 拉取主链。
- 后续桥接：下一篇可进入 `Kafka-6`，把 ConsumerGroup 的成员协调与分区分配作为控制面专题继续展开。