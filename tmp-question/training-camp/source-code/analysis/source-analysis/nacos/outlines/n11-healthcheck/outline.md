# N-11 健康检查 — 心跳任务链与四处理器

> 前置: [[N-10-客户端管理]] (客户端状态消费) + [[NC-6-服务端核心]] (HealthCheckReactor 面) | 对照: 执行计划 5.8 NC-5 (TcpSuperSenseProcessor 已不存在)
> 🔴 A | 方案 A (全深度) | 闭环: q1(任务链) q2(处理器族) q3(拦截器链) q4(心跳检查)

**读者处境**: 服务端怎么知道实例还活着? 心跳 (Beat) 与健康检查 (HealthCheck) 的关系? TCP/HTTP/MySQL/None 四种检查怎么选?

### 1. 任务模型 — HealthCheckTaskV2 与调度闭环

场景: 健康检查任务怎么调度?
源码路径:
- **HealthCheckTaskV2** (v2/HealthCheckTaskV2.java:43): extends AbstractExecuteTask + implements NacosHealthCheckTask — **doHealthCheck** (L110-124): 遍历客户端 → **HealthCheckProcessorV2Delegate.process** (L117) + 错误日志 (L124) + **重新调度** (L128: HealthCheckReactor.scheduleCheck(this) — 自续调度)
- **HealthCheckReactor.scheduleCheck** (NC-6 已见): HealthCheckTaskInterceptWrapper 包装 + GlobalExecutor.scheduleNamingHealth
- **NacosHealthCheckTask** (healthcheck/NacosHealthCheckTask.java:26): 任务接口 (Interceptable + Runnable)
关键设计 (q1): **"自续调度 = 任务不死"** — 每次 doHealthCheck 结束重新 scheduleCheck, 形成周期环; 错误不终止 (catch + 继续)。 [模式: 自续任务]

### 2. 处理器族 — HealthCheckProcessorV2 四实现 + Delegate

场景: 四种检查方式怎么组织?
源码路径:
- **HealthCheckProcessorV2** (v2/processor/HealthCheckProcessorV2.java:28): 接口 — getType/process
- **HealthCheckProcessorV2Delegate** (v2/processor/HealthCheckProcessorV2Delegate.java:38): **addProcessor 按 type 注册 Map** (L49-51) + **process 按类型分派** (L55)
- **TcpHealthCheckProcessor** (TcpHealthCheckProcessor.java:58): TCP 检查 — 最大
- **HttpHealthCheckProcessor** (HttpHealthCheckProcessor.java:52): HTTP 检查
- **MysqlHealthCheckProcessor** (MysqlHealthCheckProcessor.java:53): MySQL 检查
- **NoneHealthCheckProcessor**: 无检查
- **HealthCheckCommonV2** (HealthCheckCommonV2.java:42): 公共逻辑 (判定/上报)
- **HealthCheckExtendProvider** (extend/): SPI 扩展 (HealthCheckProcessorExtendV2)
关键设计 (q2): **"处理器族 = type 注册分派 + SPI 扩展"** — Delegate 按 getType 注册 Map, 运行时按实例配置分派; 用户可 SPI 扩展新检查方式。 [模式: 处理器注册表]

### 3. 拦截器链 — HealthCheckInterceptorChain 与四拦截器

场景: 检查前后怎么插桩?
源码路径:
- **HealthCheckInterceptorChain** (interceptor/HealthCheckInterceptorChain.java:27): 拦截器链
- **HealthCheckTaskInterceptWrapper** (interceptor/HealthCheckTaskInterceptWrapper.java:28): 包装任务 (NC-6 scheduleCheck 用)
- 四拦截器: **HealthCheckEnableInterceptor** (开关) / **HealthCheckResponsibleInterceptor** (责任节点判定 — 只有负责节点检查) / **AbstractHealthCheckInterceptor** (基类)
- **AbstractBeatCheckInterceptor** (heartbeat/AbstractBeatCheckInterceptor.java:26): 心跳拦截器基类
关键设计 (q3): **"链式插桩 = 开关/责任/扩展分层"** — 检查任务的横切逻辑走拦截器链 (是否启用/是否本节点负责); 与 NC-4 的扩展点哲学一致。 [模式: 拦截器链]

### 4. 心跳检查 — BeatCheckTask 与实例检查器

场景: 心跳维度怎么检查?
源码路径:
- **BeatCheckTask** (heartbeat/BeatCheckTask.java:24): 心跳检查任务 (NC-6 futureMap 5s 周期)
- **ClientBeatCheckTaskV2** (heartbeat/ClientBeatCheckTaskV2.java:36): v2 客户端心跳检查
- **InstanceBeatCheckTask** (heartbeat/InstanceBeatCheckTask.java:33): 实例心跳检查
- **ClientBeatProcessorV2** (heartbeat/ClientBeatProcessorV2.java:37): 心跳处理 (更新 lastBeatTime)
- **ClientBeatUpdateTask** (heartbeat/ClientBeatUpdateTask.java:29): 心跳时间更新
- 检查器族: **InstanceBeatChecker / UnhealthyInstanceChecker / ExpiredInstanceChecker** (heartbeat/): 健康/不健康/过期三维判定
- **InstanceBeatCheckTaskInterceptorChain** (heartbeat/InstanceBeatCheckTaskInterceptorChain.java:26): 心跳检查链
关键设计 (q4): **"心跳三维 = 正常/不健康/过期分治"** — 三个检查器各管一维; 心跳处理器更新时间戳, 检查任务判定状态 — 与 1.x 的 BeatReactor (已删除) 的架构差异。 [模式: 心跳三维分治]

### 5. 状态与同步 — HealthCheckStatus + HealthStatusSynchronizer

场景: 检查结果怎么落地?
源码路径:
- **HealthCheckStatus** (healthcheck/HealthCheckStatus.java:28): 状态记录
- **HealthStatusSynchronizer** (v2/HealthStatusSynchronizer.java:28) / **PersistentHealthStatusSynchronizer** (v2/PersistentHealthStatusSynchronizer.java:33): 状态同步 (临时/持久)
- **RsInfo** (healthcheck/RsInfo.java:28): 心跳信息载体 (1.x 遗留)
关键设计 (q5): **"状态同步 = 临时/持久分型"** — 检查结果按客户端类型同步 (与 N-10 三管理器/一致性双落点对齐)。 [模式: 状态分型同步]

### 6. 测试与行为锚

场景: 健康检查的边界?
源码路径:
- 测试: HealthCheckTaskV2Test / TcpHealthCheckProcessorTest (naming test)
- 日志锚: "[HEALTH-CHECK] schedule health check task" (HealthCheckTaskV2:119) / "[HEALTH-CHECK] error while process" (L124)
- 执行计划对照: TcpSuperSenseProcessor 不存在 — 3.x 用 HealthCheckTaskV2 + 处理器族
关键设计 (q1): **"日志锚 = 任务可观测"** — 调度/错误日志带 clientId。 [模式: 任务日志]
