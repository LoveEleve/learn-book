# RocketMQ-9 重写规划

> 题目：顺序消费失败以后，为什么不能简单重试
> 状态：本轮按单篇闭环执行；保留该 plan 作为后续二轮 consistency pass 工件
> 目标：把这一篇写成“顺序消费的失败处理为什么直接威胁顺序契约，因此不能套用并发消费那种简单重试思路”的专题，而不是 `ConsumeOrderlyStatus` 枚举说明文

## 1. 读者困惑

- 为什么顺序消息一旦消费失败，不能像并发消费那样直接丢回重试队列或立刻并发重放？
- `ConsumeMessageOrderlyService` 在顺序语义里真正守住了哪条边界？
- `SUSPEND_CURRENT_QUEUE_A_MOMENT`、`tryLockLaterAndReconsume`、`lockOneMQ` 这些看起来像实现细节的动作，为什么实际上是在保护顺序契约？
- 顺序消费失败处理和普通重试的根本区别到底是什么？

## 2. 一句话顿悟

**顺序消费失败以后，RocketMQ 最先要保护的不是吞吐，而是“这条队列里的后继消息不能越过当前失败点继续前进”；因此顺序消费服务会优先暂停当前 queue、重试获取本地/分布式锁、稍后再消费，而不是像并发消费那样把失败消息简单扔回重试路径让后面的消息继续跑。**

## 3. 总图

```text
同一 queue 串行消费中
  → MessageListenerOrderly 返回失败/挂起语义
    → processConsumeResult()
      → 暂停当前 queue / 稍后重试 / 重新加锁
        → tryLockLaterAndReconsume / submitConsumeRequestLater
          → 继续沿同一 queue 顺序推进
```

## 4. 关键边界

- 本篇只讲顺序消费失败处理为什么不能套用普通重试思路，不展开并发消费的完整重试体系。
- `ConsumeOrderlyStatus` 在本篇不是枚举手册，而是顺序契约的控制面。
- `lockOneMQ` / `tryLockLaterAndReconsume` 要按“顺序保护动作”理解，而不是孤立小函数。
- 不把这篇写成死信队列或 Broker 重投专题。

## 5. 本轮重写主线

1. 用“为什么顺序消费失败不能像普通消费一样重试”开场。
2. 否定：失败消息丢回去重试就行、后继消息先过问题不大、锁和稍后重试只是实现细节。
3. 先讲顺序消费一旦失败，真正先会坏掉的是“后继消息不能越线”这条契约。
4. 再讲 `ConsumeMessageOrderlyService` 如何通过 suspend / later / relock 维持队列责任。
5. 最后讲为什么这里保护的是顺序契约，而不是简单提高成功率。
