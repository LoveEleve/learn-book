# Kafka-5 重写规划

> 题目：Consumer 为什么不需要每次 fetch 都带所有分区——FetchSession、Incremental Fetch 与 Epoch 主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：解释 Kafka 的 FetchSession 机制：客户端为什么能从 Full Fetch 进入 Incremental Fetch，server 为什么能靠 session 缓存分区并只处理变更，以及 `INITIAL_EPOCH → next epoch → FINAL_EPOCH / INVALID_SESSION_ID` 这套生命周期如何让双方保持同步。

## 1. 读者困惑

- Consumer 订阅了很多分区时，为什么 fetch 请求不能每次都把全部分区元数据带上？
- `FetchSessionHandler` 怎样区分 Full Fetch 和 Incremental Fetch？
- 增量请求里的 added / altered / removed / replaced 分别表达什么？
- `INITIAL_EPOCH`、`FINAL_EPOCH`、`INVALID_SESSION_ID`、`nextEpoch()` 各自代表什么？
- 为什么增量 fetch 能省数据，但失败/超时时还要回退到 Full Fetch？

## 2. 一句话顿悟

**FetchSession 让客户端和 broker 对“当前会话包含哪些分区、每个分区参数是什么”达成一份缓存共识：首次用 Full Fetch 注册全部分区，之后每次请求只带变更（新增、修改、删除、替换），并在 epoch 失效、session 丢失或参数不一致时回退到 Full Fetch 重新对齐。**

## 3. 五要素卡片

### 读者问题

为什么 Consumer 在订阅分区很多时，fetch 请求不能简单地把所有分区元数据每次都发一遍，而必须先建立 FetchSession？

### 入口

- `FetchSessionHandler`：客户端维护 session partitions 与 next metadata
- `FetchSessionHandler.Builder.build()`：决定本次是 Full 还是 Incremental
- `FetchMetadata`：`INVALID_SESSION_ID` / `INITIAL_EPOCH` / `FINAL_EPOCH` / `nextEpoch()`
- `FetchResponse`：通过 sessionId / epoch 回应客户端
- server 侧 `FetchSession` / `CachedPartition`：缓存分区视图

### 状态核心

- `sessionPartitions`：客户端 session 里有哪些分区
- `nextMetadata`：下一次请求要用的 fetch metadata
- Full vs Incremental 判断
- added / altered / removed / replaced
- epoch：INITIAL → next → FINAL / INVALID_SESSION_ID
- `FETCH_SESSION_ID_NOT_FOUND` / `INVALID_FETCH_SESSION_EPOCH` 的失败恢复

### 失败路径

- 每次全量带所有分区：订阅分区多时请求体线性膨胀
- 增量请求但 session 丢失：server 不认识 sessionId，必须回退 Full
- epoch 不匹配：server 无法确认请求对应哪个 session 版本，需要重新对齐
- 分区参数变化但只发 toSend：缺少 altered/replaced 语义会错用旧参数
- 一直不关闭 session：长期占用 server 缓存

### 连接点

- 前文 `Kafka-4`：Fetcher 构造 FetchRequest 时会使用 FetchSessionHandler
- 后文 `Kafka-6`：ConsumerGroup 成员/分区变化会影响 session 需要更新哪些分区
- 后文 `Kafka-9`：副本侧 ReplicaFetcher 也会使用 fetch session 语义

## 4. 总图

```text
Consumer 首次 fetch
  → FetchSessionHandler 以 INITIAL 发送 Full Fetch
    → 注册所有分区到 session
      → server 返回 sessionId + 分区缓存视图
        → 后续使用 Incremental Fetch
          → 只带 added/altered/removed/replaced
            → epoch 逐次递增
              → 异常/超时/丢失则回退 Full Fetch
```

## 5. 关键边界

- 本篇只讲 FetchSession 机制，不展开 KIP-848 或 ConsumerGroup 成员协调。
- 不把 Full/Incremental 写成两种独立协议；它们是同一 session 的不同请求形态。
- 不把 epoch 写成一个普通序号；它是对应 session 视图版本，失配时必须回退。
- 不把 server 端 `CachedPartition` 写成消息存储；它缓存的是分区请求参数视图。

## 6. 失败方案推演

1. **每次 fetch 都全量带所有分区元数据**：分区多时请求/响应体持续膨胀。
2. **有 session 后永不校验 epoch**：server 与 client 视图失配后会静默出错。
3. **参数变化时只发 toSend**：会沿用旧参数，无法表达 altered/replaced。
4. **session 失效后继续增量**：server 找不到 session，需要回退 Full。

## 7. 误解清单

- Full Fetch 和 Incremental Fetch 是同一 session 的两种请求形态。
- `FINAL_EPOCH` 表示要关闭 session，不是普通 next epoch。
- `INVALID_SESSION_ID` 是常量，不代表 session 一定是非法，首次请求本来就用它。
- 增量 fetch 省的是分区元数据，不是消息数据本身。
- epoch 失配或 session 丢失不是单纯重发，而是要回到 Full 重新对齐。

## 8. 证据清单

- `clients/src/main/java/org/apache/kafka/clients/FetchSessionHandler.java:60`
- `clients/src/main/java/org/apache/kafka/clients/FetchSessionHandler.java:95`
- `clients/src/main/java/org/apache/kafka/clients/FetchSessionHandler.java:275`
- `clients/src/main/java/org/apache/kafka/common/requests/FetchMetadata.java:31`
- `clients/src/main/java/org/apache/kafka/common/requests/FetchMetadata.java:48`
- `clients/src/main/java/org/apache/kafka/common/requests/FetchMetadata.java:91`
- `clients/src/test/java/org/apache/kafka/clients/FetchSessionHandlerTest.java:215`
- `clients/src/test/java/org/apache/kafka/clients/consumer/internals/FetcherTest.java:335`

## 9. 版本边界与字数预算

- 基线：Kafka `v4.x, KRaft`。
- 本篇聚焦客户端 FetchSessionHandler 与 FetchMetadata，server 侧 `FetchSession`/`CachedPartition` 只作为边界提示。
- 目标正文：7000~11000 字；核心拆解层覆盖 Full/Incremental 区分、变更集合、epoch 生命周期、失败回退。

## 10. 本轮重写主线

1. 从“为什么不能每次全量带所有分区”开场。
2. 否定全量携带、永不校验 epoch、只发 toSend 三种直觉方案。
3. 解释 FetchSessionHandler 怎样维护 sessionPartitions 与 nextMetadata。
4. 解释 Full/Incremental 的 build 差异与变更集合。
5. 解释 epoch 生命周期与失败回退。
6. 收网：FetchSession 是把分区请求视图在 client/broker 两端缓存成共识，用增量变更和 epoch 维持同步。