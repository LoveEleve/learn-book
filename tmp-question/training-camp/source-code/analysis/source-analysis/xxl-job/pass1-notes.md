# XXL-Job 调度与执行域 — Pass 1 轮廓记录

> 日期: 2026-08-17 | 阶段 6.4 | 范围: xxl-job-admin + xxl-job-core
> 冲突审计: 未发现既有 analysis 目录、HANDOFF 或其他 AI 进行中产物

## 计划域

- XJ-1 Admin 调度：`JobScheduleHelper` 时间窗口/Cron、`JobTriggerPoolHelper` 快慢触发池
- XJ-2 Executor 执行：`XxlJobSpringExecutor`、`JobThread`、`@XxlJob` 生命周期
- XJ-3 路由策略：`ExecutorRouter` 的 FIRST/LAST/ROUND/LFU/RANDOM/CONSISTENT_HASH/BUSYOVER/SHARDING（执行计划中的 `LFH` 为笔误，源码实际是 LFU）
- XJ-4 分片：`ShardingUtil` 与广播执行参数
- XJ-5 失败重试：RetryCount + ExecutorBlockStrategy
- XJ-6 GLUE：`GlueFactory`、`SpringGlueFactory`、Groovy/Java 动态代码
- XJ-7 日志：`XxlJobFileAppender`、`JobLog`、滚动与清理

## Pass 1 观察

- XXL-Job 明确分成 Admin 控制面和 Executor 执行面：Admin 决定何时触发/选择谁，Executor 负责本地 JobThread 执行。
- 调度主线可能是“时间窗口扫描 → 触发池异步执行 → 路由到 executor → HTTP 调用 → 回调更新日志”。
- Executor 侧的 `JobThread` 是 handler 运行时容器，失败/重试/阻塞策略可能都在这里收口。
- GLUE 与日志属于扩展/运维面，但会穿透执行生命周期。

## 标记问题

1. `JobScheduleHelper` 如何处理过去/未来 5 秒窗口与 Cron 下一次时间？
2. `JobTriggerPoolHelper` 为什么分 fast/slow 两个池？
3. Admin 如何选择 executor 并发起 HTTP trigger？
4. `JobThread` 的状态、队列和 stop/destroy 生命周期？
5. `ExecutorRouter` 多策略如何组合与短路？
6. 分片参数如何从 Admin 传到 executor handler？
7. 失败重试与阻塞策略的执行顺序？
8. GLUE 代码如何加载、编译和缓存？
9. `XxlJobFileAppender` 如何生成 job 日志并滚动清理？
10. Admin/Executor 的职责边界与回调协议是什么？
11. `TriggerCallbackThread` / `JobCompleteHelper` 如何把 executor 结果异步回传 Admin？
12. `ExecutorBiz` 的 trigger/log/kill/idle-beat 接口如何组成执行协议？

## 规划修正结论

- XJ-1 扩展为“调度扫描 + 触发池 + trigger 发起”，但不下沉 executor 回调实现。
- XJ-2 扩展为“Spring executor + JobThread + ExecutorBiz + callback thread”，覆盖执行端完整生命周期。
- XJ-3 保持路由策略本体；实际源码策略包含 `LFU`，执行计划中的 `LFH` 是笔误；XJ-5 只讲阻塞/失败/重试，不吸收回调协议。
- XJ-7 负责 executor 本地文件日志与 Admin 日志模型，避免把 `JobLogReportHelper` 的日报聚合误写成文件滚动。
- XJ-4 只讲分片参数的构造与传播，广播触发仍回接 XJ-1/XJ-2，不单独复制调度流程。
- XJ-6 只讲 GlueFactory/GlueJobHandler 的动态 handler 装配，不把普通 MethodJobHandler 重复纳入。

## 深度规划审查结论

- 域数量 7 与执行计划一致，但 XJ-2 必须吸收 `ExecutorBiz`/`ExecutorBizImpl`/`TriggerCallbackThread`，否则执行端从 trigger 到 callback 的主闭环会断裂。
- XJ-5 的“失败重试”需要明确分成两类：executor 内部 JobThread 重试，以及 callback 失败后的 `TriggerCallbackThread` 持久化/重试；二者不是同一机制。
- XJ-7 需要区分 executor 本地 `XxlJobFileAppender`/`JobLogFileCleanThread` 与 Admin `XxlJobLog`/`JobLogReportHelper`，前者是文件写入，后者是数据库日志/报表聚合。
- XJ-3 的路由策略应以实际枚举和实现类为准：FIRST、LAST、ROUND、LFU、LRU、RANDOM、CONSISTENT_HASH、BUSYOVER、FAILOVER、SHARDING；执行计划中的列表不完整且 `LFH` 拼写错误，写作前必须重新穷举。
