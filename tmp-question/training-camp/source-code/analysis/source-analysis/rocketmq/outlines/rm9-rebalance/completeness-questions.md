# RM-9 Rebalance+offset+LitePull — completeness-questions (全视角提问验证)

## 开发者视角

1. 再平衡周期怎么自适应? (平衡 20s / 失衡 1s)
2. 客户端 vs broker 分配怎么切? (双表 + queryAssignment)
3. 队列不再归属怎么办? (drop + unlock + remove)
4. 新队列从哪读? (有进度续读 / 无进度尾部)
5. 6 个分配算法怎么选? (AVG 默认, 可插拔)
6. 有序消费怎么锁? (broker LOCK_BATCH_MQ)
7. LitePull 和 Push 差在哪? (手动 poll vs 自动)
8. subscribe/assign 能混用吗? (不能 — 互斥)

## 架构师视角

9. 20s/1s 自适应 vs 固定周期? (收敛速度与开销)
10. 为什么提供 broker 中心化分配? (服务端管理/Controller 协同)
11. AVG 余数处理? (前 mod 个多 1 — 负载近似均匀)
12. 起点默认尾部的取舍? (新消息优先, 旧消息靠重试)
13. 有序锁在 broker 的意义? (跨客户端互斥)
14. 再平衡对消费的影响? (drop 丢缓冲, at-least-once 重试兜底)
15. 对照 Kafka 的差异? (客户端默认 vs 中心化; 无粘性)
16. 广播模式怎么再平衡? (无 — 每客户端全量)

## 学生视角

17. 什么是再平衡? (消费者集合变化时队列重新分配)
18. AVG 算法怎么分? (按 cid 索引取队列段)
19. 消费起点为什么重要? (决定新消费者看到哪些消息)
20. 有序锁为什么需要? (顺序消费不能被并发破坏)
