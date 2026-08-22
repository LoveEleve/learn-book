# Kafka-37. Server 侧怎么记住你的 fetch session——FetchSession、CachedPartition 与 epoch 校验

> 场景：Kafka-5 讲了客户端 `FetchSessionHandler` 怎么维护 session 并发送增量 fetch。但 server 侧那一半一直没有讲透：server 按什么结构记住每个 session 的分区视图？增量请求里 `toForget` / `added` / `updated` 到底怎么落进缓存？epoch 校验在什么时机触发？本篇正面回答这些问题。这是 K-5 FetchSession 域第 2 篇，与 Kafka-5 客户端篇形成完整闭环。

## 先把真正的困惑摆出来：server 到底在"缓存"什么

客户端发送增量 fetch，声称"我只要这几个分区变化"。server 如果没有一份"这个客户端上一轮请求长什么样"的缓存，它根本无从判断哪些是新增、哪些是更新、哪些该忘记。

所以 server 必须有一个按 session 组织的**分区请求参数缓存**，而不是只把增量请求当普通 fetch 处理。

```text
客户端全量注册
  → server 缓存 CachedPartition 集合
    → 后续增量请求
      → server 对比缓存判断 added / updated / removed
```

*关键设计（斜体）：* *Server 用 `FetchSession` 保存一个 session 的分区缓存（`CachedPartition`），每个 CachedPartition 记录该分区上一次请求的关键参数；增量请求通过 `FetchSession.update(toForget)` 计算 added / updated / removed 并更新缓存；epoch 用来校验"这次请求的视图版本 == 上次的+1"，失配则要求重建 session。*[模式: 会话分区缓存 + 增量差分 + epoch 版本校验]

## 第一层：`CachedPartition` 缓存的是"分区的请求参数与响应边界"

`CachedPartition`（`FetchSession.scala`）不是把消息数据缓存下来，而是保存"这个分区在这个 session 里的上一次请求参数"：

- `topic` / `topicId` / `partition`
- `maxBytes`
- `fetchOffset`
- `highWatermark`（上次响应的）
- `leaderEpoch`
- `logStartOffset`
- `lastFetchedEpoch`

其中 `highWatermark` 的存在很关键：它让 server 可以在增量请求中**只返回高水位有变化的分区**，而不是把缓存里每个分区都塞进下一次响应。

## 第二层：`maybeUpdateResponseData` 决定"这次响应要不要带这个分区"

server 处理一个分区后，会调用 `CachedPartition.maybeUpdateResponseData` 判断是否需要在响应中携带它。源码列了几条"必须带上"的条件：

- 分区里有新数据（recordsSize > 0）；
- `highWatermark` 变了；
- `logStartOffset` 变了；
- 是 preferred replica 请求；
- 响应里有错误码；
- 出现了 `diverging epoch`（用于触发截断）。

这说明增量响应能省字节的另一个来源：**已缓存分区如果既没有新数据、高水位也没变、也没错误，server 就可以在响应里省略它，客户端直接沿用缓存里的参数。**

## 第三层：`FetchSession.update`——增量请求怎么落进缓存

当增量请求到达时，`FetchSession.update(fetchData, toForget)` 计算三类变更：

```text
fetchData.forEach
  → partitionMap.find(cachedPartitionKey)
    → 不在缓存 → 加入，记为 added
    → 在缓存   → 更新请求参数，记为 updated

toForget.forEach
  → partitionMap.remove(...)
    → 移除成功，记为 removed
```

返回 `(added, updated, removed)`。这就是增量 fetch 的完整落地：**新增进缓存、更新的覆盖参数、忘记的从缓存删除。**

## 第四层：epoch 校验——"视图版本"必须前后连续

`FetchSession.metadata` 返回 `(sessionId, epoch)`。在处理增量请求时，Kafka 会先校验：

```text
期望的 epoch = nextEpoch(请求携带的 epoch)
if (session.epoch != 期望的 epoch)
  → 说明两端视图版本不一致
    → 返回 INVALID_FETCH_SESSION_EPOCH
```

这在 `FetchSessionCache.handleIncrementalFetch` 里体现为预期的 `JFetchMetadata.nextEpoch(reqMetadata.epoch)`。如果 `session.epoch != expectedEpoch`，会打印 "Possible duplicate request"，并返回 `INVALID_FETCH_SESSION_EPOCH`。

所以 epoch 不是普通序号，而是**请求视图的版本**：增量请求必须建立在上一次已确认的视图之上。

## 第五层：session 不存在与 epoch 失配是两类错误

这里最容易混淆的点是：server 不会把所有 session 问题都压成一个错误码。

- 如果 `sessionId` 在缓存里根本找不到，返回的是 `FETCH_SESSION_ID_NOT_FOUND`；
- 如果 session 找到了，但请求携带的 epoch 与 server 期望的不连续，返回的是 `INVALID_FETCH_SESSION_EPOCH`；
- 如果请求最终走 sessionless/full 路径，响应里的 sessionId 仍可能是 `INVALID_SESSION_ID` 这个占位值。

所以客户端回退 Full Fetch 这件事是对多种 session 异常的统一恢复动作，但**底层错误原因并不相同**。

## 第六层：`FetchSessionCache` 与分片淘汰

server 不会无限保存 session。`FetchSessionCacheShard` 管理：

- `evictableByAll` / `evictableByPrivileged` 两个按 `EvictableKey(privileged, cachedSize, id)` 排序的集合；
- 新 session 可能淘汰旧 session；
- follower 创建的 session 是 privileged：它在淘汰时可以比较更大的候选集合；非 privileged session 的淘汰候选范围更窄；
- `evictionMs` 控制"多久没用到才能被淘汰"。

这不是简单的“follower 完全受保护、consumer 完全不受保护”二元模型，而是**privileged session 在淘汰竞争里拥有更高优先级**。这样做是为了避免复制链路的 fetch session 被普通 consumer session 挤掉。

## 收网：Server 侧 FetchSession = 分区参数缓存 + 增量差分 + epoch 校验

把整篇压成一句话：Server 用 `FetchSession` 按 `sessionId` 缓存 `CachedPartition` 集合，每个 CachedPartition 记录分区上一次请求参数与高水位边界；增量请求通过 `FetchSession.update` 计算 added / updated / removed 并更新缓存；epoch 校验保证增量请求的视图连续性，session 不存在与 epoch 失配会返回不同错误（分别对应 `FETCH_SESSION_ID_NOT_FOUND` 与 `INVALID_FETCH_SESSION_EPOCH`），随后客户端统一回退 Full 注册；`FetchSessionCache` 按价值与年龄淘汰旧 session。

```text
全量请求
  → 建 CachedPartition，注册 session
    → 增量请求（epoch 连续）
      → update：added / updated / removed
        → maybeUpdateResponseData：能省则省
          → epoch 失配 / session 丢失
            → INVALID_FETCH_SESSION_EPOCH / FETCH_SESSION_ID_NOT_FOUND
              → 客户端回退 Full

缓存回收
  → FetchSessionCacheShard
    → evictableByAll / evictableByPrivileged
      → 按 value/age 淘汰
```

到这里，主线只发生了六件事。

第一，`CachedPartition` 缓存的是请求参数与响应边界，不是消息数据。

第二，`maybeUpdateResponseData` 决定哪些分区能省、哪些必须带上。

第三，`FetchSession.update` 把增量请求落进缓存，计算差分。

第四，epoch 校验保证视图版本连续。

第五，session 不存在与 epoch 失配会返回不同错误，但客户端都会回退 Full。

第六，`FetchSessionCache` 按价值与年龄淘汰旧 session，防止无界增长。

**本篇的一句话困惑**：server 侧是怎么记住每个客户端 fetch session 的，增量请求又怎么落进缓存？

**本篇的一句话顿悟**：server 用 FetchSession 缓存 CachedPartition 请求参数与高水位，用 FetchSession.update 计算增量差分，用 epoch 校验视图版本连续；session 不存在和 epoch 失配分别返回不同错误，但客户端最终都会回退 Full 重新注册。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **"CachedPartition 缓存消息数据。"** 缓存的是请求参数与响应边界（offset / maxBytes / HW / epoch）。
2. **"Full 和 Incremental 是两套协议。"** 是同一 session 的建立与维护两类请求形态。
3. **"epoch 失配和 session 不存在是同一个错误。"** 前者返回 `INVALID_FETCH_SESSION_EPOCH`，后者返回 `FETCH_SESSION_ID_NOT_FOUND`。
4. **"session 一定不会被清理。"** FetchSessionCache 会按价值和年龄淘汰。
5. **"follower 和 consumer 的 session 地位平等。"** follower 创建的是 privileged，在淘汰竞争里优先级更高。

### 关键证据清单

- `core/src/main/scala/kafka/server/FetchSession.scala:75`：CachedPartition 字段。
- `core/src/main/scala/kafka/server/FetchSession.scala:102`：从请求构造 CachedPartition。
- `core/src/main/scala/kafka/server/FetchSession.scala:138`：maybeUpdateResponseData。
- `core/src/main/scala/kafka/server/FetchSession.scala:236`：FetchSession。
- `core/src/main/scala/kafka/server/FetchSession.scala:272`：FetchSession.update。
- `core/src/main/scala/kafka/server/FetchSession.scala:525`：增量 epoch 校验。
- `core/src/main/scala/kafka/server/FetchSession.scala:599`：FetchSessionCacheShard。
- `core/src/main/scala/kafka/server/FetchSession.scala:714`：淘汰策略。

### 版本与实现边界

- 本文以 Kafka `v4.x` 为基线。
- 本篇聚焦 server 侧 FetchSession，不重复客户端 FetchSessionHandler（Kafka-5）。
- 不把 CachedPartition 与业务分区副本状态混同。
- SessionErrorContext / FullFetchContext / SessionlessFetchContext 作为 server 侧的三种请求上下文被引用来解释 Full/增量/session 缺失。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-5`（客户端 FetchSessionHandler）。
- 后续桥接：可回到 Kafka-9 的 follower fetch，或做全篇收束 review。