# RM-10 顺序+事务消息 — completeness-questions (全视角提问验证)

## 开发者视角

1. 顺序消息怎么保证同一 key 进同一队列? (SelectMessageQueueByHash: hashCode % size)
2. 顺序发送失败会重试吗? (SYNC 不重试 — 失败即抛; ASYNC 重试同 broker)
3. 半消息和普通消息存储上差在哪? (topic 改 HALF + REAL_TOPIC/QID 备份 + sysFlag 清空)
4. 本地事务返回值怎么传回 broker? (endTransactionOneway, END_TRANSACTION=37)
5. 回查到了客户端走哪个方法? (checkLocalTransaction → endTransactionOneway fromTransactionCheck=true)
6. 回查多久一次? (30s 周期; 免疫期 6s 内不查)
7. 回查超过次数会怎样? (15 次 → TRANS_CHECK_MAX_TIME_TOPIC 丢弃)
8. 事务消息能配延迟吗? (不能 — sendMessageInTransaction 清掉 DelayTimeLevel)

## 架构师视角

9. 顺序消息为什么是"队列内序"而非全局序? (单队列串行吞吐; 多队列按业务键分片)
10. 选择器发送为什么不做 SYNC 重试? (换队列=乱序; 保序优先于可用性)
11. 半消息+OP 消息双流设计的意义? (对账: OP 记录已决断, 回查只扫未决断)
12. 免疫时间为什么取 max(自定义, 6s)? (防刚发的消息立即被回查 — 竞态窗口)
13. 为什么 END 是 oneway? (回传失败靠回查兜底, 客户端不阻塞)
14. 三校验 (group/queueOffset/commitLogOffset) 防什么? (防伪造/串消息 — 幂等屏障)
15. 回查为什么 at-least-once? (客户端无 ACK; 15 次弃置是终止条件)
16. 5.x OP 批量对账的意义? (减少 OP 写入放大 — 4096B 攒批)
17. 对照 Kafka 事务/Kafka 幂等? (RocketMQ 无 idempotent producer; 靠半消息对账)

## 学生视角

18. 什么是半消息? (先落盘但不投递的消息 — 事务状态未决前消费者看不到)
19. 什么是回查? (broker 定时问生产者"本地事务到底提交了没")
20. 顺序消息和事务消息有关系吗? (独立特性; 可组合 — 顺序事务)
