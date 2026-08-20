# RocketMQ-24 重写规划

> 题目：Producer 为什么不会每次都远程问 NameServer —— 路由缓存与刷新主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：补深 Producer / Broker 的路由缓存与刷新链，解释 `TopicPublishInfo`、`topicRouteTable`、`brokerAddrTable` 为什么存在，以及何时会触发 NameServer 刷新、何时只消费本地缓存。

## 1. 读者困惑

- Producer 明明只知道 topic，为什么却不会每次 send 都去远程问一次 NameServer？
- `TopicPublishInfo`、`topicRouteTable`、`brokerAddrTable` 这些缓存各自存什么、谁来刷新？
- 路由什么时候算旧了，RocketMQ 为什么不会每次都强制远程刷新？
- 客户端和 Broker 自己为什么都要维护一套路由缓存？
- 如果 NameServer 暂时不可达，缓存对发送主链到底救了什么？

## 2. 一句话顿悟

**RocketMQ 发送主链不是“每次 send 都现问 NameServer”，而是先消费本地路由缓存，再在缓存缺失、不可用或定时任务触发时刷新；`TopicPublishInfo` 负责 Producer 这次到底能发给谁，`topicRouteTable/brokerAddrTable` 负责把 NameServer 的集群视图留在本地持续复用。**

## 3. 五要素卡片

### 读者问题

为什么路由发现不能被理解成一次 RPC，而必须被理解成“本地缓存 + 按需刷新 + 定时拉新”的复合主链？

### 入口

- `DefaultMQProducerImpl.tryToFindTopicPublishInfo()`：Producer 发送前先查本地缓存
- `MQClientInstance.updateTopicRouteInfoFromNameServer()`：客户端批量定时刷新 topic 路由
- `MQClientAPIImpl.getTopicRouteInfoFromNameServer()`：真正发起 NameServer RPC 的入口
- `TopicPublishInfo`：Producer 发布视图
- `TopicRouteInfoManager`：Broker 侧 EscapeBridge / Pop 之类场景使用的本地路由缓存

### 状态核心

- `topicPublishInfoTable` / `topicRouteTable` / `brokerAddrTable`
- `TopicPublishInfo.ok()` / `haveTopicRouterInfo`
- `pollNameServerInterval` / `loadBalancePollNameServerInterval`
- `updateTopicRouteInfoFromNameServer(topic, ...)`
- `findBrokerAddressInPublish()` / `getTopicSubscribeInfo()`
- route changed 与 needUpdate 判定

### 失败路径

- 每次 send 都远程问路：发送链会被 NameServer RPC 拖慢并放大控制面压力
- 完全不刷新缓存：Broker 变化、queue 变化、主从切换后路由长期陈旧
- 只缓存 queue 列表，不缓存 brokerAddr：最后一步发不出去
- NameServer 暂时不可达：没有缓存会导致发送链直接失明
- 变更检测过粗：即使路由变化也不更新 publish/subscribe 视图

### 连接点

- 前文 `RocketMQ-3`：已讲路由发现入口，本篇补“为什么是本地视图 + 按需刷新”
- 前文 `RocketMQ-22`：发送失败规避建立在本地可用的 publish view 之上
- 前文 `RocketMQ-15`：Controller / NameServer 外部视图变化最终也要通过路由刷新进入 Producer 世界
- 后续机动篇可补 Broker Processor 分发或恢复与 DLedger/Controller 对照

## 4. 总图

```text
Producer send(topic)
  → tryToFindTopicPublishInfo() 先查本地 publish view
    → 缓存可用：直接走本地目标决策
    → 缓存缺失/不可用：向 NameServer 拉 TopicRouteData
      → 更新 TopicPublishInfo / brokerAddrTable
        → 后续发送继续消费本地缓存
  → 后台定时任务周期刷新路由，避免长期陈旧
```

## 5. 关键边界

- 本篇只讲路由缓存与刷新，不重讲 `selectOneMessageQueue()` / `MQFaultStrategy` 的运行时规避。
- 不把 `TopicPublishInfo` 写成普通缓存对象；它是 Producer 发布视图。
- 不把客户端缓存与 Broker 侧 `TopicRouteInfoManager` 混成一套；它们服务不同主链。
- 不把“本地缓存存在”误写成“永远不需要再问 NameServer”。

## 6. 失败方案推演

1. **每次发送都远程问 NameServer**：最直觉，但发送链完全受 NameServer RPC 时延牵制。
2. **一旦缓存建立就永不刷新**：Broker/queue 变化、主从切换后路由逐步陈旧。
3. **只有 topic→queue 缓存，没有 brokerAddr 缓存**：选出队列也未必能真正发出去。
4. **客户端和 Broker 共享一套缓存语义**：会混淆 publish、subscribe、escape bridge 等不同使用场景。

## 7. 误解清单

- Producer 发送不是每次都实时问 NameServer。
- `TopicPublishInfo` 不只是“缓存一下队列列表”，而是本次发送可消费的发布视图。
- 路由缓存存在不等于不会刷新；定时任务和按需拉新都在工作。
- 客户端和 Broker 都有路由缓存，但用途不同。
- 路由缓存不是为了省几次 RPC，而是为了让发送主链拥有本地决策能力。

## 8. 证据清单

- `client/src/main/java/org/apache/rocketmq/client/impl/producer/DefaultMQProducerImpl.java:883`
- `client/src/main/java/org/apache/rocketmq/client/impl/factory/MQClientInstance.java:332`
- `client/src/main/java/org/apache/rocketmq/client/impl/factory/MQClientInstance.java:381`
- `client/src/main/java/org/apache/rocketmq/client/impl/MQClientAPIImpl.java:2039`
- `client/src/main/java/org/apache/rocketmq/client/impl/producer/TopicPublishInfo.java:47`
- `broker/src/main/java/org/apache/rocketmq/broker/topic/TopicRouteInfoManager.java:68`
- `broker/src/main/java/org/apache/rocketmq/broker/topic/TopicRouteInfoManager.java:92`
- `broker/src/main/java/org/apache/rocketmq/broker/topic/TopicRouteInfoManager.java:196`
- `client/src/test/java/org/apache/rocketmq/client/producer/DefaultMQProducerTest.java:152`

## 9. 版本边界与字数预算

- 基线：RocketMQ `5.3.1`。
- 本篇聚焦客户端 publish 路由缓存，顺带点到 Broker 侧 `TopicRouteInfoManager`；不展开 NameServer 内部 `RouteInfoManager` 存储细节。
- 目标正文：7000~11000 字；核心拆解层覆盖本地 publish view、按需刷新、后台定时刷新、brokerAddr 映射与缓存失效风险。

## 10. 本轮重写主线

1. 从“为什么 Producer 不会每次都远程问 NameServer”开场。
2. 否定每次远程问、永不刷新、只缓存 queue 不缓存 brokerAddr 三种朴素方案。
3. 解释 `tryToFindTopicPublishInfo()` 怎样优先消费本地 publish view。
4. 解释 `MQClientInstance` 定时刷新如何维持 topic/broker 视图新鲜度。
5. 解释客户端缓存与 Broker 侧 `TopicRouteInfoManager` 的职责差别。
6. 收网：RocketMQ 的发送主链之所以能本地决策，是因为路由发现被改造成“缓存优先、按需拉新、后台续命”的结构。