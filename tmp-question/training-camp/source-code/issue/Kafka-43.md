# Kafka-43. 一条请求的完整旅程——从 SocketServer 到 ReplicaManager，再到 responseQueue

> 本篇是 Kafka 全系列的收束篇，不做新专题，而是把前面分散的子系统按“一条请求从入站到出站”的路径重新串起来：网络层怎么接住请求，请求怎么被 `KafkaApis` 路由，produce/fetch 在 `ReplicaManager` 里怎么分叉，响应又怎么回到 Processor。

## 先把真正的困惑摆出来：Kafka 真的是“收到请求 → 处理 → 直接写回”这么简单吗

不是。哪怕只看最常见的 produce / fetch，两条路径也已经不同：

- produce 不同 `acks` 走法不同；
- fetch 可能直接返回，也可能进入 `DelayedFetch` 等 `minBytes`；
- 响应也不是业务线程直接写 socket，而是回到 `RequestChannel` / responseQueue，再由 Processor 的 selector 写回。

*关键设计（斜体）：* *请求统一从 `SocketServer` 进入 `RequestChannel`，再由 handler 线程交给 `KafkaApis`；`KafkaApis` 根据 API key 分流到 `handleProduceRequest` / `handleFetchRequest` 等入口；produce 在 `ReplicaManager.appendRecords` 中依据 `acks` 决定是直接返回还是挂入 `DelayedProduce`，fetch 在 `ReplicaManager.readFromLog` 后依据结果与 `minBytes` 决定是直接返回还是挂入 `DelayedFetch`；最终响应统一回到 `RequestChannel.sendResponse`，由 Processor 写回客户端。*[模式: 统一入站 + API 分流 + produce/fetch 分叉 + 统一出站]

## 第一层：网络入口不是“业务线程直接收包”

请求先进入 `SocketServer`：

```text
Acceptor
  → Processor
    → RequestChannel
```

Processor 负责网络收发、协议解析、基础校验，然后把请求封装成 `RequestChannel.Request` 放入请求队列。真正的业务逻辑并不在 Processor 里执行。

## 第二层：`KafkaApis` 是 API 分发器，不是所有逻辑都写在一个大方法里

handler 线程从 `RequestChannel` 取出请求后，交给 `KafkaApis.handle(...)`。`KafkaApis` 再按 `ApiKeys` 分发：

- `PRODUCE` → `handleProduceRequest`
- `FETCH` → `handleFetchRequest`
- 其他 API 各自走对应 handler

所以 `KafkaApis` 的核心职责不是完成所有业务，而是把网络层拿到的协议请求路由到正确的业务入口。

## 第三层：produce 主链不是只有一种走法

produce 请求进入 `KafkaApis.handleProduceRequest` 后，最终会调用 `ReplicaManager.appendRecords(...)`。

但这里不能把后续链路写成“总是 DelayedProduce 等 ISR”：

- `acks=0`：客户端不等 broker 正式响应确认；
- `acks=1`：leader 本地 append 完就能返回；
- `acks=-1` / `all`：只有需要等待 ISR 条件时，才会进入 `maybeAddDelayedProduce(...)`，挂入 `DelayedProduce`。

所以更准确的主线是：**appendRecords 是统一入口，DelayedProduce 只是其中一条需要等待条件满足时的分支。**

## 第四层：fetch 主链是“先读，再决定要不要等”

fetch 请求进入 `KafkaApis.handleFetchRequest` 后，会走到 `ReplicaManager.readFromLog(...)`。

这里同样不是“总是 DelayedFetch”：

- 如果本次读取结果已经满足返回条件，可以直接回包；
- 如果还不满足 `minBytes` 或其他完成条件，才会构造 `DelayedFetch` 并放入 fetch purgatory 等待。

所以 fetch 的真正节奏是：

```text
handleFetchRequest
  → readFromLog(...)
    → 已满足条件 → 直接响应
    → 未满足条件 → DelayedFetch → 完成后再响应
```

## 第五层：响应统一回到 `RequestChannel.sendResponse`

不管 produce 还是 fetch，最终响应都不是业务代码直接把字节写回 socket，而是通过 `RequestChannel.sendResponse(...)` 回到对应 Processor 的 responseQueue，再由 Processor 的 selector 异步写回客户端。

这一步把**业务处理**和**网络回写**清楚分层：handler 线程负责算结果，Processor 线程负责网络发包。

## 第六层：幂等与事务是在主链上“叠加约束”，不是另起一条链

如果 producer 开启幂等或事务，主链并不会换成另一条“神秘路径”，而是在 append 主线上叠加：

- `ProducerAppendInfo` / `ProducerStateManager` 校验 epoch / sequence；
- 事务状态与 marker 决定消费者 `read_committed` 的可见性。

所以事务/幂等更像 produce 主链上的额外约束层，而不是脱离 `ReplicaManager.appendRecords` 的独立世界。

## 收网：Kafka 请求主链 = 统一入站、分叉处理、统一出站

把整篇压成一句话：Kafka 请求先由 `SocketServer` 的 Acceptor / Processor 接住并送入 `RequestChannel`，再由 handler 线程交给 `KafkaApis` 按 API 分流；produce 统一走 `ReplicaManager.appendRecords`，但只有需要等待副本条件时才进入 `DelayedProduce`，fetch 统一走 `ReplicaManager.readFromLog`，但只有未满足返回条件时才进入 `DelayedFetch`；最后响应统一通过 `RequestChannel.sendResponse` 回到 Processor 写回客户端，幂等/事务则作为 append 主链上的额外约束叠加其上。

```text
客户端请求
  → SocketServer（Acceptor → Processor → RequestChannel）
    → Handler → KafkaApis.handle(...)
      → PRODUCE → handleProduceRequest → ReplicaManager.appendRecords
      → FETCH   → handleFetchRequest   → ReplicaManager.readFromLog
        → 条件未满足 → DelayedProduce / DelayedFetch
          → RequestChannel.sendResponse
            → Processor responseQueue
              → selector 写回客户端
```

**本篇的一句话困惑**：一条 Kafka 请求从进 broker 到回给客户端，中间到底经过了哪些层，哪些地方会分叉等待？

**本篇的一句话顿悟**：Kafka 用 SocketServer + RequestChannel 统一接入请求，用 KafkaApis 按 API 分流，在 ReplicaManager 中把 produce/fetch 分别落到 append/read 主链上，只有在需要等待 ISR 或 `minBytes` 等条件时才进入 delayed 操作，最后统一经 responseQueue 回写。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“业务线程直接读 socket 再直接写回 socket。”** 网络收发在 Processor，业务处理在 handler。
2. **“produce 一定会进入 DelayedProduce。”** 只有需要等待副本条件时才会进入 delayed 分支。
3. **“fetch 一定会进入 DelayedFetch。”** 先读 `readFromLog`，只有条件不满足才等待。
4. **“KafkaApis 自己做完全部业务。”** 它更重要的职责是 API 分发与主链组织。
5. **“幂等/事务是另一条完全独立的请求链。”** 它们是 append 主链上的额外约束层。

### 关键证据清单

- `core/src/main/scala/kafka/server/KafkaApis.scala:169`：API key 分发到 `handleProduceRequest` / `handleFetchRequest`。
- `core/src/main/scala/kafka/server/KafkaApis.scala:388`：`handleProduceRequest`。
- `core/src/main/scala/kafka/server/KafkaApis.scala:555`：`handleFetchRequest`。
- `core/src/main/scala/kafka/server/ReplicaManager.scala:674`：`appendRecords(...)`。
- `core/src/main/scala/kafka/server/ReplicaManager.scala:704`：`maybeAddDelayedProduce(...)`。
- `core/src/main/scala/kafka/server/ReplicaManager.scala:1726`：`readFromLog(...)`。
- `core/src/main/scala/kafka/server/ReplicaManager.scala:1704`：构造 `DelayedFetch`。
- `core/src/main/scala/kafka/server/DelayedProduce.scala:57`：`DelayedProduce`。
- `core/src/main/scala/kafka/server/DelayedFetch.scala:50`：`DelayedFetch`。

### 版本与实现边界

- 本文以 Kafka `v4.x` 为基线。
- 本篇是总串联篇，聚焦请求主链，不展开每个子系统内部细节。
- 不把 `acks=0/1/all`、立即返回与 delayed 等待压成一条单一路径。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-13/14`（网络层）、`Kafka-10/36`（Purgatory）、`Kafka-33/35`（幂等/事务）。
- 后续桥接：可继续补“消息丢失排查”或“容量评估”这类源码+运维桥接篇。