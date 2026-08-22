# RocketMQ-29 重写规划

> 题目：Consumer 架构为什么不是“拉到就消费”——消费者总览
> 状态：骨架补深篇。对应 `hz` 的 Consumer 架构设计主题，承接 RocketMQ-6/7/8/9/23，把 PullMessageService、Rebalance、ProcessQueue、消费线程池、顺序/并发消费几条线拉成消费者总图。

## 1. 读者困惑
- RocketMQ Consumer 为什么不是“Broker 推给我，我处理完就行”？
- Pull、Push、LitePull 到底是什么关系？
- Rebalance、ProcessQueue、消费线程池分别负责什么？
- 顺序消费和并发消费为什么不是简单换个回调函数？

## 2. 一句话顿悟
**RocketMQ Consumer 的本质不是“收消息”，而是一套由拉取、负载均衡、队列本地快照、消费线程调度、消费进度提交共同组成的运行时架构。Push/LitePull/顺序/并发只是建立在这套骨架之上的不同消费表面。**

## 3. 失败方案推演
- 把 Push 理解成 Broker 主动推送
- 把 Rebalance 看成一次性分配动作
- 把 ProcessQueue 误解成 Broker 侧队列
- 把顺序/并发消费误解成同一主链的轻微分支

## 4. 章节问题
- Consumer 为什么底层仍是 pull？
- Rebalance 为什么要和 ProcessQueue 配合？
- PullMessageService、ConsumeMessageService、offset 提交怎么接起来？
- 顺序消费和并发消费的架构分叉点在哪？

## 5. 至少要排除的误解
- Push = Broker push
- ProcessQueue = Broker queue
- Rebalance 完成后就不再变化
- 消费线程池只是“拿到消息后跑个 callback”

## 6. 关键证据清单
- `client/.../consumer/DefaultMQPushConsumer`
- `client/.../consumer/DefaultLitePullConsumer`
- `client/.../impl/consumer/DefaultMQPushConsumerImpl`
- `client/.../impl/consumer/PullMessageService`
- `client/.../impl/consumer/RebalanceImpl`
- `client/.../impl/consumer/ProcessQueue`

## 7. 版本与实现边界
- RocketMQ 5.x / 4.x 通用主链
- 本篇是消费者总览，不替代 RocketMQ-6/7/8/9/23 的细节篇

## 8. 字数预算
- 7000~10000 字