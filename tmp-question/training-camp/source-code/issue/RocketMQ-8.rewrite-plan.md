# RocketMQ-8 重写规划

> 题目：顺序消息为什么只能保证“分区内有序”
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成“RocketMQ 顺序消息是如何建立在前面 Producer 队列选择 + Consumer 顺序推进主链上的局部保证”专题，而不是 MessageQueueSelector / MessageListenerOrderly API 说明文

## 1. 读者困惑

- RocketMQ 为什么不能天然保证全局消息顺序，而只能保证“分区内有序”？
- Producer 端的 `MessageQueueSelector` 和 Consumer 端的 `MessageListenerOrderly` 到底各自补哪一层缺口？
- 为什么“同一 key 进同一队列”还不够，消费侧还要再维持顺序推进？
- 顺序消息失败重试时，为什么不能像并发消费那样随便重放？

## 2. 一句话顿悟

**RocketMQ 的顺序消息不是在 Broker 全局层强行维持一条总顺序，而是在发送侧用 `MessageQueueSelector` 把“同一业务线索”稳定压进同一队列，再在消费侧用 `MessageListenerOrderly` / 顺序消费服务让这个队列按本地责任顺序推进；它本质上是一种队列内局部有序，而不是系统级全局有序。**

## 3. 总图

```text
Producer 发送主链
  → MessageQueueSelector 按业务键稳定选同一队列
    → Broker 仍按普通消息主链写入 CommitLog / ConsumeQueue
      → Consumer 侧拿到这个队列责任
        → MessageListenerOrderly / ConsumeMessageOrderlyService 串行推进
          → 失败时暂停/稍后再试，而不是随意并发重放
```

## 4. 关键边界

- 本篇只讲“顺序保证为什么是队列内局部保证”，不展开事务消息、延迟消息或 ConsumerGroup 协调协议细节。
- Producer 端不讨论所有分配策略，只聚焦 `MessageQueueSelector` 如何把业务键映射到稳定队列。
- Consumer 端不展开全部消费实现，只聚焦顺序消费与并发消费的根本边界差异。
- 不把顺序消息误写成“Broker 全局排序服务”。

## 5. 本轮重写主线

1. 用“为什么顺序消息只能保证分区内有序”开场。
2. 否定：Broker 全局排序、只要发送进同一队列就自动端到端顺序、失败重试还能随便并发。
3. 先讲 Producer 侧的队列绑定：业务键 -> 固定队列。
4. 再讲 Consumer 侧的顺序推进：同一队列本地责任串行化。
5. 最后讲失败与重试为什么会直接威胁顺序语义。
6. 收网时明确：顺序消息是建立在主链之上的“局部保证”，不是另起一套消息系统。
