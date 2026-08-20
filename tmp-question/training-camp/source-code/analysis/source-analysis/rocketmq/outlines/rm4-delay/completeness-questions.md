# RM-4 延迟消息 — completeness-questions (全视角提问验证)

## 开发者视角

1. 18 级怎么配置? (messageDelayLevel 字符串, s/m/h/d 单位)
2. 延迟消息存哪? (SCHEDULE_TOPIC_XXXX + queueId=level-1)
3. 到期时间存哪? (CQ tagsCode 槽, storeTimestamp+delay)
4. 投递失败怎么办? (100ms 重排, offset 续扫)
5. 消息还原还原什么? (REAL_TOPIC/REAL_QUEUE_ID + 清延迟属性)
6. offset 怎么持久化? (JSON 10s 定时)
7. 异步投递的流控? (maxPendingLimit=2000)
8. 越界延迟等级? (钳制到 maxDelayLevel)

## 架构师视角

9. 等级即队列的设计? (每级独立进度, 简单可靠)
10. tagsCode 槽复用的取舍? (零额外存储, 语义由 topic 区分)
11. 三级延迟节奏 (1s/100ms/10s) 的精度开销平衡?
12. 为什么 waitStoreMsgOK=false? (投递异步容忍)
13. 同步 vs 异步投递? (等待确定性 vs 吞吐+流控)
14. 崩溃恢复怎么续投? (offset 持久化 + load)
15. 为什么不支持任意延迟? (等级粒度 vs Timer 消息)
16. 事务半消息为什么 discard? (延迟与事务状态冲突)

## 学生视角

17. 延迟消息的"延迟"存在哪? (属性 DELAY_TIME_LEVEL + tagsCode 到期时间)
18. 为什么用轮询不用精确调度? (消息队列吞吐场景, 误差可接受)
19. 消费者需要感知延迟吗? (不需要 — 到期还原透明)
20. SCHEDULE_TOPIC_XXXX 用户能发吗? (不能 — 系统 topic 保护)
