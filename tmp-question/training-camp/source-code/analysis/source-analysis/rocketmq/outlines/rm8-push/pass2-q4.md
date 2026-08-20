# 闭环笔记 q4: 消费服务 — Concurrently vs Orderly

## 假设
并发消费: 批量 + 线程池; 有序消费: 队列锁 + 单线程; 失败重试面。

## 验证过程
- **ConcurrentlyService** (469): consumeExecutor (20 线程, q1) + ConsumeRequest (批量: **consumeMessageBatchMaxSize=1 默认**) → listener.consumeMessage → 结果:
  - CONSUME_SUCCESS → processQueue.commit + offsetStore.updateOffset
  - RECONSUME_LATER → **sendMessageBack** (L289-322): delayLevel = context.getDelayLevelWhenNextConsume → **重试队列 (RETRY_TOPIC, RM-6 交叉)** — 重试延迟消费
- **OrderlyService** (573): **messageQueueLock (队列级锁, L63)** + processQueue.lock → 队列粒度串行; **tryLockLaterAndReconsume** (L226-235): 锁失败 → 10ms/3s 延迟重试; 有序 = 队列内严格顺序 (重试也入队尾)
- **超时面**: 消费超时配置 (consumeTimeout) + 批量面 (batchMaxSize)

## 代码类型
Algorithmic (消费调度)

## 跨域关联
- RM-6 (过滤): 重试消息过滤
- RM-4 (延迟): 重试延迟等级

## 结论
并发消费 = 20 线程池 + 批量 (默认 1) + 失败 sendMessageBack 延迟重试; 有序 = 队列锁串行 + 锁失败延迟重试 (10ms/3s)。
源码位置: ConsumeMessageConcurrentlyService.java:57-322; ConsumeMessageOrderlyService.java:63-328
