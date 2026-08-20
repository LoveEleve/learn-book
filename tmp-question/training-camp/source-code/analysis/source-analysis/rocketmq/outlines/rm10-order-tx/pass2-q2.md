# 闭环笔记 q2: 顺序消费协同 — 队列锁与有序消费

## 假设
顺序消费 = 同一队列同一时刻只被一个消费者串行消费; 锁在 broker 侧跨客户端。

## 验证过程
- **进程内锁**: ConsumeMessageOrderlyService (RM-8 已讲) — MessageQueueLock (ConcurrentHashMap<MessageQueue, Object>) 锁住整队列; 锁失败 **10ms 后重提** (L233/448/461)
- **broker 分布式锁**: LOCK_BATCH_MQ 批量锁 (RM-9 已讲) — 锁随再平衡归属转移; ProcessQueue.isLockExpired → 暂停消费
- **再平衡立即锁**: RebalanceImpl:521 — `if (isOrder && !this.lock(mq))` → 有序 topic 新归属队列立即 lock
- **起点无特殊分支**: computePullFromWhereWithException (RebalancePushImpl:166-230) 全 switch 无 isOrder 判断 — **5.3.1 顺序 topic 与普通 topic 同起点逻辑** (默认 maxOffset 尾部); 3.x 文档"顺序消息从 0 起读"对 5.3.1 不成立
- **有序消费组**: MessageListenerOrderly + setConsumeFromWhere(CONSUME_FROM_FIRST_OFFSET) 是用户显式选择 — 非系统强制

## 代码类型
Implementation (分布式锁 + 有序消费)

## 跨域关联
- RM-8 (消费): ConsumeMessageOrderlyService/MessageQueueLock/10ms 重试
- RM-9 (再平衡): LOCK_BATCH_MQ/锁随归属/isOrder 传参

## 结论
顺序保证 = 发送端同队列 (q1) + 消费端单消费者串行 (本 q)。5.3.1 无顺序特殊起点逻辑。
源码位置: RebalanceImpl.java:521; RebalancePushImpl.java:166-230; ConsumeMessageOrderlyService.java:233,448-461
