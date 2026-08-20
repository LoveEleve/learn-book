# XXL-Job 域规划 — 全视角提问

## 功能

1. `JobScheduleHelper` 的 5 秒预读窗口为什么是 5000ms？
2. misfire 发生时为什么有直接触发与跳过两种处理？
3. `JobTriggerPoolHelper` 为什么要拆快/慢两个池？
4. `XxlJobTrigger` 如何从 jobInfo/group 生成 `TriggerParam`？
5. `ExecutorBizImpl` 如何决定复用旧 `JobThread` 还是新建？
6. 路由策略到底有几种，哪些是真正会选 executor 的？
7. `SHARDING_BROADCAST` 为什么不是普通路由？
8. 失败重试与 block strategy 的边界在哪里？
9. callback 失败为什么落盘到 `callbacklog` 而不是马上放弃？
10. `XxlJobFileAppender` 与 Admin `XxlJobLog` 的边界？
11. `GlueFactory`/`GlueJobHandler` 与普通 `MethodJobHandler` 的关系？
12. `ShardingUtil` 只负责参数注入还是也参与路由？

## 性能

13. 预读数 `preReadCount` 为什么按 fast+slow pool * 20 估算？
14. LFU/LRU 路由是否会有热点缓存偏斜？
15. callback 批量 drain queue 的作用是什么？

## 并发

16. `schedule_lock for update` 如何保证多 Admin 实例下只一方调度？
17. `ringData` 时间轮的并发安全性如何？
18. `JobThread.isRunningOrHasQueue()` 对 block strategy 的语义是否足够？
19. callback 重试与主执行线程是否隔离？

## 边界

20. GLUE 更新时旧线程如何淘汰？
21. 日志保留天数小于 3 为什么直接不启清理？
22. triggerParam 的 `executorParam` / `executorShardingParam` / `addressList` 谁覆盖谁？
