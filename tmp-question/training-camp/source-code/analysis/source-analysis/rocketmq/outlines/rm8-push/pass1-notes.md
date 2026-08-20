# RM-8 消费-Push — Pass 1 探索笔记

> 域: RM-8 消费-Push (PushConsumer 内部机制) | 🔴 A 方案 | 2026-08-14
> 源码: client/consumer + impl/consumer (12595 行: DefaultMQPushConsumerImpl 1592 + DefaultMQPushConsumer 1005 + PullMessageService + ProcessQueue 464 + ConsumeMessage*Service) | RocketMQ 5.3.1

## 调用图

```
门面 (DefaultMQPushConsumer, 1005):
subscribe (tag/SQL92) → DefaultMQPushConsumerImpl (1592)

运行时 (MQClientInstance 1391 — RM-9 部分? 实为共享骨架):
producer+consumer 共享; consumer 侧: rebalanceService + pullMessageService + 消费服务

拉取链 (DefaultMQPushConsumerImpl.pullMessage L300+):
PullMessageService (ServiceThread): pullRequestQueue (LinkedBlockingQueue<MessageRequest>) 消费
  → executePullRequestImmediately (入队) / executePullRequestLater (延迟 50-3000ms)
pullMessage: offsetStore.readOffset (集群模式) → sysFlag (commitOffset/suspend/subscription/classFilter)
  → pullAPIWrapper.pullKernelImpl (ASYNC, suspend 15s 长轮询, timeout 30s)
  → pullCallback: 成功 → ProcessQueue.putMessage → submitConsumeRequest / 空 → suspend 重拉

消费服务 (ConsumeMessage*Service):
ConcurrentlyService (469): consumeExecutor (线程池) → ConsumeRequest (批量 batchMaxSize) → 消息监听器
  → 成功后 ProcessQueue.commit + offset 推进; 失败 → 重试 (sendMessageBack RM-6 交叉)
OrderlyService (573): 消息队列锁 (MessageQueueLock) + 单线程 per 队列
PopConcurrentlyService (5.x POP 模式, 484)

缓存 (ProcessQueue 464):
msgTreeMap (TreeMap<offset, MessageExt>) — 拉取与消费之间的缓冲; 消费进度推进
  流控: 缓存大小/条数 (maxSpan 等 → executePullRequestLater 50ms)
```

## 基本元素分解

1. **门面**: DefaultMQPushConsumer (subscribe/registerMessageListener/start)
2. **拉取调度**: PullMessageService (队列 + 立即/延迟)
3. **长轮询**: pullKernelImpl (ASYNC + broker 挂起 15s)
4. **缓存**: ProcessQueue (TreeMap 消息缓冲 + 进度)
5. **消费服务**: Concurrently/Orderly (线程池 + 锁)
6. **进度管理**: offsetStore (集群: broker 存储; 广播: 本地文件)

## 标记问题 (20 问)

1. PullMessageService 的队列模型? (LinkedBlockingQueue)
2. executePullRequestLater 的延迟分级? (50/20/1000/3000ms)
3. 长轮询参数? (suspend 15s / timeout 30s)
4. sysFlag 四 bit? (commitOffset/suspend/subscription/classFilter)
5. ProcessQueue 缓存结构? (TreeMap)
6. 拉取-消费之间的流控? (缓存上限 → 延迟拉取)
7. 消费线程池? (consumeThreadMin/Max)
8. 批量消费? (consumeMessageBatchMaxSize)
9. 并发 vs 有序消费? (锁粒度)
10. 消费成功怎么推进? (commit + offset)
11. 消费失败重试? (sendMessageBack + 重试队列)
12. offsetStore 两模式? (集群/广播)
13. 订阅更新? (subscribe 动态)
14. 5.x POP 模式? (PopConcurrentlyService)
15. 消费进度提交时机? (定时/批量)
16. 广播消费的 offset? (本地文件)
17. 消息锁? (MessageQueueLock — 有序)
18. 拉取超时处理? (CONSUMER_TIMEOUT)
19. 与 RM-7 共享 MQClientInstance? (骨架复用)
20. 测试面? (client consumer 测试)

## 时空溯源 (代码内痕迹)

- 3.x: PushConsumer 骨架 (PullMessageService + ProcessQueue + 长轮询 15s)
- 4.x: ConsumeMessageConcurrently/Orderly 服务化; offsetStore 抽象; 流控 (缓存上限)
- 5.x: **POP 消费模式** (PopConcurrentlyService/PopProcessQueue — 5.0 新消费协议); MessageRequest 队列重构 (5.x)

## 大域拆分判断

12595 行消费面 — RM-8 聚焦 Push 链; RM-9 聚焦 Rebalance+offset+LitePull; 🔴 A 单篇 (6 闭环)

## 域级怀疑审计 (自建域断言 复查)

| 断言 (PLAN v3) | 验证 | 结论 |
|:--|:--|:--|
| "MQClientInstance/PullMessageService (长轮询)/ProcessQueue/DefaultMQPushConsumerImpl" | 面全存在 (1391/长轮询 15s/464/1592) | **接受** ✅ |
| 数字: 长轮询 | BROKER_SUSPEND_MAX_TIME_MILLIS=15s / CONSUMER_TIMEOUT=30s (L114-115) | **补充** ✅ |
| 数字: 延迟分级 | 异常 3000 / 缓存流控 50 / broker 流控 20 / suspend 1000 (L101-113) | **补充** ✅ |
| "PullMessageService 属 Push 内部" (v2 修正) | PullMessageService 在 impl/consumer — Push 拉取调度器 ✓ | **接受** ✅ |
