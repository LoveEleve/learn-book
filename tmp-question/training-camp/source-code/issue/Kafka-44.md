# Kafka-44. 限流架构怎么串起来——QuotaFactory、ClientQuotaManager 与请求节流主链

> 场景：Kafka 的限流不是一个孤立的 if 判断，而是一整套从配额配置、请求统计、节流时间计算到响应延迟发送的链路。本篇补上这个主题，回答“Kafka 的限流架构到底在哪里，怎么把 throttleTimeMs 落到请求上的”。

## 先把真正的困惑摆出来：Kafka 限流到底限的是谁、在哪里生效

Kafka 既可能限制客户端请求速率，也可能限制副本复制速率，还可能限制 controller mutation 之类的控制面操作。真正的问题不是“有没有 throttle”，而是：**不同类型的配额由谁管理，什么时候记账，什么时候把请求挂起并延迟返回。**

*关键设计（斜体）：* *Kafka 用 `QuotaFactory` 在 broker 启动时一次性装配多类 quota manager；请求进入处理链后由 `RequestHandlerHelper` 调用对应 quota manager 先记账、再计算 `throttleTimeMs`，最后通过 `requestChannel` 延迟响应；复制流量则走 `ReplicationQuotaManager` 等专用限流器。*[模式: 多配额管理器装配 + 统一记账入口 + 延迟响应节流]

## 第一层：`QuotaFactory` 统一装配多类 quota manager

在 `BrokerServer.startup()` 里，broker 会调用：

```scala
quotaManagers = QuotaFactory.instantiate(config, metrics, time, ...)
```

这说明 Kafka 不是在各个 handler 里零散 new 限流器，而是由 `QuotaFactory` 统一创建一组 `QuotaManagers`，再分发给请求处理链、复制链、控制链使用。

从 `QuotaFactory.QuotaManagers` 的真实字段看，这组 manager 包括：

- `fetch`：`ClientQuotaManager`
- `produce`：`ClientQuotaManager`
- `request`：`ClientRequestQuotaManager`
- `controllerMutation`：`ControllerMutationQuotaManager`
- `leader` / `follower` / `alterLogDirs`：三类 `ReplicationQuotaManager`

所以限流架构首先是一个**集中装配**问题，而不是单点实现问题；而且它不是笼统的一种 replication quota，而是明确拆成 leader、follower、alter-log-dirs 三条复制/迁移链路。

## 第二层：请求路径的统一入口在 `RequestHandlerHelper`

普通客户端请求在处理完成准备返回时，会经过 `RequestHandlerHelper.sendResponseMaybeThrottle` / `maybeRecordAndGetThrottleTimeMs`。

关键链路是：

```text
处理请求
  → quotas.request.maybeRecordAndGetThrottleTimeMs(request, now)
    → 得到 throttleTimeMs
      → quotaManager.throttle(request, callback, throttleTimeMs)
        → 通过 requestChannel 延迟发送响应
```

也就是说，Kafka 的请求节流通常不是“拒绝请求”，而是**把响应延后发出去**，并且把 `throttleTimeMs` 写进响应体，让客户端知道自己被限了多久。

## 第三层：为什么是“先记账，再节流”

`maybeRecordAndGetThrottleTimeMs` 这个命名已经说明顺序：

1. 先把这次请求的流量/资源消耗计入统计；
2. 再根据配额窗口与当前使用量算出需要 throttle 多久。

这个顺序很重要。如果先判断再记账，会让当前请求的成本逃过统计，导致配额控制偏松。

## 第四层：复制限流不是走普通 request quota

副本复制是 broker 内部链路，不走普通客户端请求的 quota。`QuotaFactory` 明确把复制相关 quota 拆成 `leader`、`follower`、`alterLogDirs` 三个 `ReplicationQuotaManager`；`ReplicaManager` 再把这些 manager 接到 follower fetch、副本复制、目录迁移等路径上。

这也是为什么 Kafka 的“限流”不能只理解成客户端 API throttle：**副本同步与目录迁移有独立配额体系。**

## 第五层：动态配额配置怎么接到运行时

Kafka 的 client quota 不是写死在启动参数里。`ClientQuotaMetadataManager`、`DynamicTopicClusterQuotaPublisher` 等 metadata publisher 会把元数据层的配额变更推送到运行中的 `QuotaManagers`。

这说明配额并不只是静态配置：KRaft 元数据更新后，broker 可以动态刷新 quota 视图。

## 收网：限流架构 = 统一装配 + 统一记账 + 多路径生效

把整篇压成一句话：Kafka 在 broker 启动时用 `QuotaFactory` 装配 `fetch`、`produce`、`request`、`controllerMutation` 以及 `leader/follower/alterLogDirs` 这些 quota manager；普通请求通过 `RequestHandlerHelper` 统一记账并计算 `throttleTimeMs`，再以延迟响应的方式节流；副本复制与目录迁移走各自 replication quota manager；动态元数据发布再把运行时配额变更推送给这些 manager。

```text
BrokerServer.startup
  → QuotaFactory.instantiate
    → QuotaManagers(fetch / produce / request / controllerMutation / leader / follower / alterLogDirs)
      → 普通请求：RequestHandlerHelper 记账 + throttle response
      → 复制链路：leader / follower / alterLogDirs quota
      → 控制链路：controllerMutation quota
        → 动态元数据发布刷新 quota
```

**本篇的一句话困惑**：Kafka 的限流到底在哪里生效，是谁在算 throttleTimeMs？

**本篇的一句话顿悟**：Kafka 先由 QuotaFactory 装配多类 quota manager，再由 RequestHandlerHelper 或复制链路统一记账并计算 throttleTimeMs，最后通过延迟响应或专用复制限流来生效。**

## 本篇元数据：误解、证据与边界

### 至少要排除的误解

1. **“Kafka 只有一种 request quota。”** 实际有 `fetch`、`produce`、`request`、`controllerMutation`，以及 `leader/follower/alterLogDirs` 三类 replication quota。
2. **“被限流就是直接拒绝请求。”** 常见做法是延迟响应并写入 `throttleTimeMs`。
3. **“复制流量走普通客户端 quota。”** 副本同步有独立 `ReplicationQuotaManager`。
4. **“quota 只在启动时读一次。”** 动态元数据发布可刷新运行时 quota。
5. **“节流只和响应有关，不记账。”** Kafka 是先记账再算 throttle 时间。

### 关键证据清单

- `core/src/main/scala/kafka/server/BrokerServer.scala:200`：`QuotaFactory.instantiate(...)`。
- `core/src/main/java/kafka/server/QuotaFactory.java:54`：`QuotaManagers` 字段组成。
- `core/src/main/java/kafka/server/QuotaFactory.java:129`：各 quota manager 的真实实例化。
- `core/src/main/scala/kafka/server/RequestHandlerHelper.scala:34`：`throttle(...)`。
- `core/src/main/scala/kafka/server/RequestHandlerHelper.scala:111`：`maybeRecordAndGetThrottleTimeMs(...)`。
- `core/src/main/scala/kafka/server/ReplicaManager.scala:2629`：复制链路 quota manager 接入。
- `core/src/main/scala/kafka/server/metadata/ClientQuotaMetadataManager.scala:23`：client quota metadata 管理。
- `core/src/main/scala/kafka/server/metadata/DynamicTopicClusterQuotaPublisher.scala:17`：动态 quota 发布。

### 版本与实现边界

- 本文以 Kafka `v4.x` 为基线。
- 本篇聚焦配额装配与节流主链，不展开每个 quota 算法的采样窗口实现细节。
- 不把网络层 socket 背压与 quota throttle 混为一谈。

### 前置依赖与后续桥接

- 前置依赖：`Kafka-14`（RequestChannel）、`Kafka-39`（Broker 启动装配）。
- 后续桥接：可继续补 `消息堆积/容量评估` 这类偏运维专题。