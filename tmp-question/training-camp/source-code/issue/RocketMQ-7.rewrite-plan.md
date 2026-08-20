# RocketMQ-7 重写规划

> 题目：Push、LitePull、Rebalance 为什么不是三套平行模型
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成“消费主链的不同入口姿势与责任分配模型”专题，而不是 Push/Pull API 对照表

## 1. 读者困惑

- Push、LitePull、Pull 这些 consumer 形态到底是不是三套完全不同的消费世界？
- Rebalance 为什么不是附加协议细节，而是消费主链能成立的前提？
- 为什么 `DefaultMQPushConsumerImpl` 和 `DefaultLitePullConsumerImpl` 看起来入口不同，背后却仍共享同一条“队列责任 -> 拉取 -> 本地承接 -> 位点推进”主链？
- `AllocateMessageQueueAveragely` 这类分配策略为什么会直接影响消费主链，而不是单纯负载均衡选项？

## 2. 一句话顿悟

**Push、LitePull、Pull 的差别，不在于它们各自拥有独立消费内核，而在于“谁驱动拉取、谁触发消费、责任队列怎样被分配给当前消费者”；`RebalanceImpl` 先决定队列责任，Push/LitePull 再在这条统一消费推进主链上选择不同入口姿势。**

## 3. 总图

```text
ConsumerGroup 视角
  → RebalanceImpl 决定当前消费者负责哪些 MessageQueue
    → Push 模型：框架持续拉取并回调业务监听器
    → LitePull 模型：应用 poll/拉取，自己驱动消费节奏
      → 但两边都共享：队列责任 -> 拉取 -> ProcessQueue -> 位点推进
```

## 4. 关键边界

- 本篇只讲消费模型分化与 Rebalance 主线，不展开 ConsumerGroup 协调协议完整细节。
- 不把 Push / LitePull 写成 API 手册或配置项大全。
- `Rebalance` 在本篇是消费主链前提层，不是旁支协议补充。
- 分配策略只讲它如何影响责任队列划分，不展开所有策略实现细节。

## 5. 本轮重写主线

1. 用“为什么看起来像三套消费者，其实不是三套消费内核”开场。
2. 否定：Push=自动消费所以无需拉取主链、LitePull=完全另一套实现、Rebalance=外围细节。
3. 先讲 Rebalance：先分队列责任，后面一切才有意义。
4. 再讲 Push：框架帮你驱动拉取和消费。
5. 再讲 LitePull：应用自己驱动节奏，但仍踩同一主链。
6. 收网时明确：消费模型分化的是入口姿势，不是主链本体。
