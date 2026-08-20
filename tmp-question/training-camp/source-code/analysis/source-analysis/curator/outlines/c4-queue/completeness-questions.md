# C-4 队列与屏障 — completeness-questions (全视角提问验证)

## 开发者视角

1. DistributedQueue.put() 前台和后台差在哪? putListener 什么时候触发?
2. 队列怎么知道有新消息? 消费者需要轮询吗?
3. 消费到一半进程崩溃, 消息去哪了 (无锁模式)?
4. lockPath 怎么启用? 锁安全模式消费流程?
5. flushPuts 是干嘛的? close 前要调吗?
6. PriorityQueue 的优先级怎么编码? 负数怎么处理?
7. DelayQueue 的延迟写在节点名还是数据里? 为什么?
8. SimpleDistributedQueue 和 DistributedQueue 有什么区别?
9. DistributedBarrier.setBarrier() 幂等吗? waitOnBarrier 超时?
10. DistributedDoubleBarrier 的 memberQty 是上限吗?

## 架构师视角

11. 为什么用版本号而不是对象引用来等待数据变化?
12. 一次性 watcher 重挂的竞态: 事件和重挂之间丢了变更怎么办?
13. 先删后消费 vs 锁安全模式的语义权衡 (at-most-once vs 至少一次)?
14. requeue 为什么必须用事务? 不用会怎样?
15. 名字编码优先级的边界: 排序串的字典序等价性怎么保证?
16. QueueSharder 为什么用 LeaderLatch? 重复扩容竞态?
17. 屏障为什么成员节点用 EPHEMERAL? 持久会怎样?
18. 双屏障 leave 为什么反序 watch? 全员 watch 会怎样?
19. 对比 Kafka: ZK 队列缺什么 (ack/分区/重放)?
20. connectionLost 显式抛错的设计意图?

## 学生视角

21. 什么是 FIFO? 顺序节点怎么保证?
22. 什么是 watcher? 为什么是一次性的?
23. 什么是信号量? processChildren 里的 Semaphore 干嘛?
24. 什么是屏障? 单屏障和双屏障区别?
25. 优先级队列为什么能靠名字排序?
