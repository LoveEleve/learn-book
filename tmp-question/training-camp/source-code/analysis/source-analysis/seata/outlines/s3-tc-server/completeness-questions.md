# S-3 TC Server — completeness-questions (全视角提问验证)

## 开发者视角

1. TC 有哪些后台线程池? (6 ScheduledThreadPool 各 1 线程)
2. 每个池干什么? (retryRollback/retryCommit/asyncCommit/timeoutCheck/undoDelete/syncProcessing)
3. 状态组有哪些? (5 组筛选数组 3/1/1/1/4)
4. 重试多久? (MAX_RETRY_TIMEOUT 默认 -1 → 永不超时, 靠 70s dead)
5. 超时怎么查? (timeoutCheck 扫描 Begin 态 → TimeoutRollbacking)
6. 请求怎么分发? (onRequest → LimitRequestDecorator → doXxx)
7. undo_log 怎么清? (undoLogDelete 广播 RM saveDays)
8. 动态延迟怎么算? (max(timeToDeadSession, period))

## 架构师视角

9. 为什么分区线程池? (各状态族独立调度 — 互不阻塞)
10. 为什么分布式锁? (多节点/RAFT 防重复执行 — 未获锁重排)
11. 为什么动态延迟? (最早到期会话驱动 — 无会话时 70s 低频)
12. 为什么 MAX=-1 默认? (永远重试直到 dead 阈值 — 最终一致优先)
13. 为什么轮询而非事件? (状态持久化在存储 — 轮询可重启恢复)
14. 为什么限流装饰器? (可插拔限流 — 默认关)
15. 对照 RocketMQ Broker? (延迟消息 vs 周期轮询)
16. RAFT 模式? (RaftCoordinator + distributedLock — 存储模式驱动)

## 学生视角

17. 什么是 TC? (事务协调者 — 全局事务的中枢)
18. 什么是状态组? (按 GlobalStatus 筛选会话的条件)
19. 什么是重试? (失败事务周期性再尝试)
20. 什么是 dead threshold? (重试终止阈值 70s)
