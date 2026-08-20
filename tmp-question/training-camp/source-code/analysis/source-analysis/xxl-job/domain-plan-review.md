# XXL-Job 域规划深审记录

## 审查范围

- 执行计划 `issue/源码分析执行计划.md:610-620`
- 实际源码：`xxl-job-admin/src/main/java` 与 `xxl-job-core/src/main/java`
- 测试地图：admin/core test
- 本域 Pass 1：`pass1-notes.md`

## 发现与修正

### 1. 路由策略数字/名称不一致

- 计划写 `LFH`，源码不存在该策略。
- 源码实际存在 `ExecutorRouteLFU`，同时还有 `LRU`、`FAILOVER`，计划列表不完整。
- 规划修正：XJ-3 写作期重新按 `ExecutorRouteStrategyEnum` 穷举，不按计划旧列表照抄。

### 2. XJ-2 执行域边界过窄

- 仅写 `XxlJobSpringExecutor → JobThread` 会漏掉 `ExecutorBiz`/`ExecutorBizImpl` 的远程入口，以及 `TriggerCallbackThread` 的结果回传。
- 规划修正：XJ-2 吸收执行端完整闭环：注册/触发/日志/kill/idle-beat、JobThread、callback thread。

### 3. XJ-5 失败重试存在两套机制

- executor 内部 JobThread 的 handler 重试
- callback 失败后的 TriggerCallbackThread 队列、失败文件和重试线程
- 规划修正：XJ-5 明确区分两者，禁止写成一个“重试开关”。

### 4. XJ-7 日志存在两条边界

- executor 本地：`XxlJobFileAppender` + `JobLogFileCleanThread`
- admin 持久化/报表：`XxlJobLog` + `JobLogReportHelper` + DAO
- 规划修正：XJ-7 同时讲两条链，但不把 Admin 报表聚合误称为文件滚动。

### 5. XJ-4/XJ-6 依赖边界

- XJ-4 只讲分片参数如何构造和传播，广播触发回接 XJ-1/XJ-2。
- XJ-6 只讲 Glue 动态 handler 装配，普通 MethodJobHandler 归 XJ-2。

## 审查结论

- 7 域数量可保留，与执行计划一致。
- 原规划主题可保留，但 XJ-2、XJ-3、XJ-5、XJ-7 必须按本记录修正后再进入 Pass 2。
- 本轮未写正文、未批量生成域交付物；仅完成域规划审查和 Pass 1 修正。
