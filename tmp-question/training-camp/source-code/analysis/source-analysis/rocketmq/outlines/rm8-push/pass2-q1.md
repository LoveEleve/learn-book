# 闭环笔记 q1: 拉取调度 — PullMessageService + 延迟分级

## 假设
拉取请求队列 (阻塞队列); 立即/延迟两种入队; 延迟按场景分级。

## 验证过程
- **PullMessageService** (ServiceThread): messageRequestQueue = LinkedBlockingQueue<MessageRequest> (L33); run 循环 take → executePullRequestImmediately (转 PushConsumerImpl)
- **入队双路** (L56-81): executePullRequestImmediately (即时) / executePullRequestLater (延迟 — 提交到 scheduledExecutorService)
- **延迟分级** (DefaultMQPushConsumerImpl L101-113):
  - pullTimeDelayMillsWhenException=**3000ms** (异常)
  - PULL_TIME_DELAY_MILLS_WHEN_CACHE_FLOW_CONTROL=**50ms** (本地缓存流控)
  - PULL_TIME_DELAY_MILLS_WHEN_BROKER_FLOW_CONTROL=**20ms** (broker 流控)
  - PULL_TIME_DELAY_MILLS_WHEN_SUSPEND=**1000ms** (挂起)
- **消费线程池** (ConcurrentlyService L73): consumeThreadMin/Max=**20/20** (DefaultMQPushConsumer:161-166) — 固定 20 线程

## 代码类型
Implementation (调度队列)

## 跨域关联
- RM-9 (Rebalance): 拉取请求由 rebalance 分配队列驱动

## 结论
调度 = 阻塞队列 + 延迟分级 (异常 3s/缓存 50ms/broker 20ms/挂起 1s); 消费线程池固定 20。
源码位置: PullMessageService.java:31-81; DefaultMQPushConsumerImpl.java:101-113; DefaultMQPushConsumer.java:161-166
