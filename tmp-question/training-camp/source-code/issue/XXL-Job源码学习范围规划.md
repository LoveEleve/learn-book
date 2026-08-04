# XXL-Job 源码学习范围规划

> **版本**: v2.4.2
> **仓库**: `/data/workspace/source-code/code/spring/xxl-job/`
> **规模**: 113 个源文件（core 45 + admin 68），3 个子模块
> **日期**: 2026-08-03

---

## 一、仓库概况

XXL-Job 是轻量级分布式任务调度中间件，采用 **中心化调度（Admin）+ 分布式执行（Executor）** 架构。Admin 负责任务管理、触发、路由和日志收集；Executor 负责任务执行、心跳注册和结果回调。通信基于自研 HTTP RPC（`XxlRpcReferenceBean`/`XxlRpcProviderBean`）。

**子模块清单**（3 个，全部审计）：

| 子模块 | 文件数 | 职责 | 状态 |
|---|---|---|---|
| `xxl-job-core/` | 45 | 核心库：执行器、RPC 通讯、GLUE 编译、JobHandler 接口、线程模型 | ✅ 已探索 |
| `xxl-job-admin/` | 68 | 调度中心：任务管理、触发调度、路由策略、日志收集、Web UI | ✅ 已探索 |
| `xxl-job-executor-samples/` | 若干 | 示例执行器（Spring Boot 集成） | 淘汰 |

**core 包结构**：

| 包 | 职责 |
|---|---|
| `executor/` | `XxlJobExecutor` 主类——初始化 JobHandler 注册表 + RPC Provider + 心跳线程 + 任务回调线程 + 日志清理线程 |
| `handler/` | `IJobHandler` 接口——`execute()` 执行方法 + `@XxlJob` 注解支持；`GlueJobHandler` GLUE 模式执行器 |
| `thread/` | 后台线程：`JobThread`（每个任务独占线程执行）、`TriggerCallbackThread`（结果回调）、`ExecutorRegistryThread`（心跳注册） |
| `biz/` | RPC 通讯：`AdminBiz`（Admin 端 API）、`ExecutorBiz`（Executor 端 API）、`XxlJobRemotingUtil` HTTP 通讯 |
| `glue/` | GLUE 模式：动态编译 Java/Groovy/Shell/Python 源码为可执行 JobHandler |
| `log/` | `XxlJobFileAppender` 文件日志 |
| `server/` | `EmbedServer` Executor 内嵌 HTTP Server |

| admin 包 | 职责 |
|---|---|
| `core/scheduler/` | `XxlJobScheduler` 调度器入口——初始化 `XxlJobTrigger` 触发线程池 + 失败重试 + 失败告警 |
| `core/trigger/` | `XxlJobTrigger` 触发逻辑——解析路由策略→选择 Executor→发送 HTTP 触发请求 |
| `core/route/` | `ExecutorRouter` 路由策略接口 + 8 种实现（FIRST/LAST/ROUND/RANDOM/CONSISTENT_HASH/LEAST_FREQUENTLY_USED/LEAST_RECENTLY_USED/FAILOVER/BUSYOVER/SHARDING_BROADCAST） |
| `core/thread/` | `JobScheduleHelper` 时间轮调度——基于 `scheduleCron`/`scheduleFixRate` 的 `delayQueue` 调度 |
| `core/model/` | `XxlJobInfo` 任务元数据（Cron 表达式、路由策略、阻塞策略、GLUE 类型等） |

---

## 二、知识域规划

### 🔴 核心域（5 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| X-1 | **XXL-Job 总体架构** | XxlJobExecutor(271行), XxlJobScheduler, AdminBiz, ExecutorBiz, ExecutorRegistryThread, TriggerCallbackThread | **Admin 调度中心**：`XxlJobScheduler` 初始化时间轮+触发线程池+失败监控+告警；**Executor 执行器**：`XxlJobExecutor.start()` 五步初始化——① `initAdminBizList` HTTP 客户端连接 Admin ② `JobLogFileCleanThread` 日志清理 ③ `TriggerCallbackThread` 启动回调线程（`callBackQueue` LinkedBlockingQueue→`drainTo` 批量→HTTP 回调失败重试）④ `ExecutorRegistryThread` 启动心跳线程（每 30s 向所有 Admin 地址 HTTP POST 注册→任一成功即停止）⑤ `initEmbedServer` Netty 内嵌 HTTP Server 监听 Admin 触发请求；**通信**：`AdminBiz.registry/callback/beat` HTTP API + `ExecutorBiz.run/idleBeat/kill/beat/log` HTTP API |

| X-2 | **Admin 调度引擎** | JobScheduleHelper(380行), XxlJobTrigger(226行), JobTriggerPoolHelper | **分布式 DB 锁**：`SELECT * FROM xxl_job_lock WHERE lock_name='schedule_lock' FOR UPDATE` + `setAutoCommit(false)` 事务——MySQL 行级锁实现多 Admin 实例互斥调度；**双线程时间轮**：① `scheduleThread`（持续循环）：扫描 `trigger_next_time < now+5s` 的待执行任务→计算下次触发时间（Cron 或固定速率）→`pushTimeRing(ringSecond, jobId)` 入环形队列→更新 DB 下次触发时间→commit 事务；② `ringThread`（每秒 1 次）：**处理 2 个槽位**（当前秒 + 前 1 秒防漏）→ `ringData.remove(slot)` 原子取出任务列表→`JobTriggerPoolHelper.trigger()`；**预读量**：`preReadCount = (fastMax + slowMax) * 20`（满足触发线程池 QPS）；**Misfire 策略**：`DO_NOTHING`（跳过）或 `FIRE_ONCE_NOW`（立即补偿触发）；**触发线程池**：快池（核心 10 最大 20）+ 慢池（核心 10 最大 100——1 分钟内超时 500ms×10 次降级） |
| X-3 | **Executor 执行引擎** | JobThread(252行), XxlJobExecutor(271行), XxlJobContext, TriggerCallbackThread | **单任务单线程**：每个 jobId 一个 `JobThread`——`LinkedBlockingQueue<TriggerParam>` 阻塞队列；**超时隔离**：有 `executorTimeout>0` 时，`handler.execute()` 包装在 `FutureTask` 的独立线程中执行→`futureTask.get(executorTimeout, SECONDS)` 超时→`futureThread.interrupt()` 中断执行线程→`XxlJobHelper.handleTimeout()`；**空闲销毁**：`triggerQueue.poll(3s)` 无任务→`idleTimes++`→累计 >30 次（~90s）→`XxlJobExecutor.removeJobThread()` 销毁此 JobThread；**去重**：`triggerLogIdSet`（HashSet）防止同一 logId 重复触发；**结果回调**：每次执行完毕→`TriggerCallbackThread.pushCallBack(HandleCallbackParam)`→回调线程批量 drain 后 HTTP 发送 Admin——失败重试（retry thread） |
| X-4 | **路由策略** | ExecutorRouter + 9 种实现 + ExecutorRouteStrategyEnum | **SHARDING_BROADCAST 不在路由层**：它在 `XxlJobTrigger.trigger()` 中直接遍历所有 Executor 并发调用 `processTrigger()`——不经过 Router；**9 种 Router**：FIRST（固定第一个）/ LAST（固定最后一个）/ ROUND（`count.getAndIncrement() % list.size()` AtomicLong 轮询）/ RANDOM（`Random().nextInt()`）/ CONSISTENT_HASH（`jobId+1` 一致性哈希）/ **LFU**（`ConcurrentHashMap<jobId, HashMap<address, count>>` 最少使用）/ **LRU**（`LinkedHashMap<jobId, List<address>>` 最近最少使用——accessOrder=true）/ FAILOVER（心跳检测后随机取存活实例）/ BUSYOVER（`idleBeat()` HTTP 探测 Executor 空闲状态——超时跳过） |
| X-5 | **GLUE 动态执行** | GlueFactory(90行), SpringGlueFactory, GlueTypeEnum | **MD5 缓存编译**：`GlueFactory.loadNewInstance()` → `getCodeSourceClass()` 用 MD5 hash 做 key 查 `ConcurrentHashMap` 缓存，命中则复用 Class，未命中则 `GroovyClassLoader.parseClass(codeSource)` 编译→缓存→`clazz.newInstance()`→`injectService(instance)` Spring 自动装配→返回 `IJobHandler`；**Spring 集成**：`SpringGlueFactory` 继承 GlueFactory，重写 `injectService()` 调用 `applicationContext.getAutowireCapableBeanFactory().autowireBean(instance)`；**refreshInstance**：`type=0`（普通）或 `type=1`（SpringGlueFactory），切换时重建 ClassLoader 和缓存；**GLUE 版本轮询**：Executor 定时检测 `glueUpdatetime` 变更→重新调用 `loadNewInstance()` 加载最新源码

### 🟡 扩展域（2 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| X-6 | **任务失败重试与告警** | JobAlarmer, XxlJobFailMonitorHelper | **失败重试**：`executorFailRetryCount`（重试次数）→`XxlJobTrigger.processTrigger()` 递归重试；**失败监控**：`XxlJobFailMonitorHelper` 独立线程扫描失败日志→`JobAlarmer` 发送告警邮件（`EmailJobAlarm` 实现 `JobAlarm` 接口）；**超时告警**：`jobTimeoutSecondAlarm` 配置超时时间→超时任务单独告警 |
| X-7 | **日志与运维** | XxlJobFileAppender, JobLogReportHelper, JobRegistryMonitorHelper | **日志系统**：`XxlJobFileAppender` 按日期滚动（`yyyy-MM-dd` 目录）→`XxlJobLogGlue.log()` 记录 GLUE 源码；**日志清理**：Executor `JobLogFileCleanThread` 定期清理过期日志文件（`-XXL:ExecutorLogRetentionDays`）；**统计报告**：`JobLogReportHelper` 每日统计成功/失败/运行中的任务数→`XxlJobLogReport` 表；**注册监控**：`JobRegistryMonitorHelper` 清理心跳超时的 Executor 注册记录 |

---

## 三、淘汰清单

| 子模块/功能 | 理由 |
|---|---|
| `xxl-job-executor-samples/` | 示例项目 |
| Admin 前端 UI（`templates/`）| Freemarker 页面，非调度核心 |
| `XxlJobRemotingUtil` HTTP 通讯细节 | HTTP POST + 超时 + 重试，非架构核心 |
| `XxlJobThreadPool` 线程池封装 | 简单 ExecutorService 包装 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 5 |
| 🟡 扩展域 | 2 |
| **总域** | **7** |
| 淘汰模块 | 1 + 3 功能 |

---

## 五、学习顺序建议

```
X-1 总体架构（理解 Admin↔Executor 通信模型）
  → X-2 Admin 调度（时间轮 + 触发线程池）
    → X-3 Executor 执行（JobThread 单线程 + 阻塞策略）
      → X-4 路由策略（10 种分布式路由）
        → X-5 GLUE 动态执行（Groovy 运行时编译）
          → X-6/X-7 重试告警/日志运维（按需）
```

以上规划完成，共 **5🔴+2🟡=7 域**。XXL-Job 代码量小（113 文件），核心聚焦调度引擎和 GLUE 动态编译两大设计亮点。
