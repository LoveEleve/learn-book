# XXL-Job 域规划 — 大纲

## XJ-1 Admin 调度
- `JobScheduleHelper`：预读窗口、misfire 三路径、时间轮
- `JobTriggerPoolHelper`：快/慢池、超时计数
- `XxlJobTrigger`：生成 TriggerParam、选路由、远程 trigger

## XJ-2 Executor 执行
- `XxlJobSpringExecutor` / `XxlJobExecutor`：注册、嵌入式 server、handler 仓库
- `ExecutorBizImpl`：run/log/kill/idle-beat 协议入口
- `JobThread`：队列、生命周期、handler 执行
- `TriggerCallbackThread`：异步回传与 callback 失败重试

## XJ-3 路由策略
- `ExecutorRouteStrategyEnum` 10 项穷举
- FIRST/LAST/ROUND/RANDOM/CONSISTENT_HASH
- LFU/LRU/FAILOVER/BUSYOVER/SHARDING_BROADCAST

## XJ-4 分片
- `ShardingUtil` 与 broadcast index/total
- 分片参数如何进入 handler 上下文

## XJ-5 失败重试与阻塞策略
- `ExecutorBlockStrategyEnum`
- `executorFailRetryCount`
- callbacklog 持久化重试

## XJ-6 GLUE 模式
- `GlueFactory` / `SpringGlueFactory`
- `GlueJobHandler` / `ScriptJobHandler`
- glue 更新时间与线程替换

## XJ-7 日志
- executor 文件日志 `XxlJobFileAppender`
- 本地清理 `JobLogFileCleanThread`
- callback 失败文件 `callbacklog`
- admin DB 日志/报表 `XxlJobLog*`
