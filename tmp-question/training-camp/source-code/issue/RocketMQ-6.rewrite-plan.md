# RocketMQ-6 重写规划

> 题目：Consumer 为什么不是“读到消息就算完”——拉取、ProcessQueue 与消费推进主链
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成“ConsumeQueue 之后，Consumer 为什么还需要拉取、队列分配、本地承接和进度推进”专题，而不是 Push/Pull API 比较文

## 1. 读者困惑

- ConsumeQueue 已经建立了消费索引，为什么 Consumer 还不能简单地“按 offset 直接读完就结束”？
- Pull、Push、LitePull、Rebalance、ProcessQueue 分别在补哪一层缺口？
- 为什么 Consumer 真正面对的不是“读日志”，而是“先拿队列，再拉消息，再本地承接，再推进位点”？
- `ProcessQueue` 为什么不是普通缓存容器，而是消费推进主链的一部分？

## 2. 一句话顿悟

**RocketMQ 的消费主链真正先解决的不是“有没有消息可读”，而是“当前消费者该负责哪些队列、怎样把 Broker 拉回来的消息放进本地承接平面、以及消费成功/失败后怎样继续推进位点”；`PullMessageService`、`RebalanceImpl`、`ProcessQueue` 和消费服务一起构成了这条推进主链。**

## 3. 总图

```text
ConsumeQueue 已有按队列的消费索引
  → Rebalance 决定当前消费者负责哪些队列
    → PullMessageService / pullMessage 拉回消息
      → ProcessQueue 本地承接与状态缓存
        → ConsumeMessageService 交给业务消费
          → 成功/失败后推进或重试位点
```

## 4. 关键边界

- 本篇只讲消费推进主链，不展开 ConsumerGroup 协调细节和重平衡协议全景，那是后续独立专题。
- 不把 Push / LitePull 写成 API 使用大全，而是只拿它们解释“消费推进模型为何分层”。
- `ProcessQueue` 在本篇视为本地承接平面，而不是普通缓存实现细节。
- 重试在本篇只讲“它为什么属于推进主链的一部分”，不展开所有死信队列策略。

## 5. 本轮重写主线

1. 用“有了 ConsumeQueue，为什么 Consumer 还没法直接把消息读完”开场。
2. 否定：ConsumeQueue 已足够、Push=自动消费、ProcessQueue=普通本地缓存。
3. 先讲 Rebalance：不先确定负责哪些队列，消费主链根本起不来。
4. 再讲 PullMessageService / pullMessage：把 Broker 侧可读事实拉回本地。
5. 再讲 ProcessQueue：为什么消费前还要有本地承接平面。
6. 最后讲 ConsumeMessageService 和位点推进/失败重试如何让主链闭环。
