# 闭环笔记 q5: 有序锁与 5.x broker 分配 — lockAll/unlockAll

## 假设
有序消费 = broker 侧队列锁 (LOCK_BATCH_MQ); 再平衡时锁随队列归属转移。

## 验证过程
- **lockAll** (L187-200): 有序消费的队列批量加锁 — lockBatchMQ → 成功 **setLocked(true)** (L171); 失败重试
- **unlockAll** (L98-130): 不再归属的队列释放锁 — unlockBatchMQ + **setLocked(false)** (L122)
- **锁语义**: broker 侧 ConsumerOffsetManager 持锁 (同一队列同一时刻仅一个消费者可有序消费 — 防并发顺序破坏)
- **ProcessQueue.isLockExpired** (RM-8): 锁过期 → 有序消费暂停 (锁定期内消费)
- **5.x broker 分配** (q2): 服务端分配时锁管理服务端化

## 代码类型
Implementation (分布式锁)

## 跨域关联
- RM-8 (有序消费): processQueue.lock
- RM-5 (Broker): LOCK_BATCH_MQ 请求码

## 结论
有序锁 = broker 侧队列级互斥 (批量加/解锁); 再平衡时锁随归属转移; 锁过期暂停消费。
源码位置: RebalanceImpl.java:98-200; ProcessQueue.isLockExpired
