# 09-REQ：microsphere-observability 完整需求规格

> 本文是 09-observability 的完整需求文档，覆盖两部分：(1) 已有代码实现的需求（标注完成度和已知 bug）；(2) 规划中待实现的需求（发散）。每项需求独立，不强制全部启用。

---

## 一、运行时资源可见性

### REQ-001：容器内存指标

Actuator 的 JVM 内存指标拿 `Runtime.maxMemory()`——宿主机物理上限，不是 cgroup 限制值。容器被 K8s OOMKilled 时 Prometheus 上 JVM 堆曲线一切正常——运维看到的是假数据。这个模块读 `/sys/fs/cgroup/` 下的 cgroup 内存文件，暴露容器真实限额和实际用量，支持 v1 和 v2 两个版本。

**产出指标**：Gauge `cgroup.memory.usage_in_bytes` / `cgroup.memory.limit_in_bytes` / `cgroup.memory.stat.rss` / `cgroup.memory.stat.cache` / `cgroup.memory.stat.swap` 等 16 个

**状态**：[已实现，不完整] 只支持 cgroup v1 路径格式。v2 的 `/sys/fs/cgroup/memory.current` 等新路径未支持，v2 环境下静默无指标。

---

### REQ-002：物理机内存结构与 swap 指标

Actuator 不暴露 swap 维度——运维看到"内存使用率 85%"无法区分"正常压力"和"已经在 swap 了，磁盘 I/O 暴涨"。这个模块从 HotSpot 私有 `OperatingSystemMXBean` 拿 swap 总量/余量、虚拟内存、物理内存总量/余量——5 个 Gauge。

**产出指标**：`memory.swap.space.total` / `memory.swap.space.free` / `memory.committed.virtual` / `memory.physical.total` / `memory.physical.free`（均为 Bytes 单位）

**状态**：[已实现] 依赖 `com.sun.management.OperatingSystemMXBean`（HotSpot 私有），非 HotSpot JVM 上 `supports()` 返回 false，优雅降级。

---

### REQ-003：网卡流量指标

Actuator 没有网络 I/O。运维排查"服务为什么对外请求变慢"时需要看各网卡接口的收发字节、包量、错误、丢弃数——不是全局汇总，是**每个独立网卡一条时间序列**。这个模块解析 `/proc/net/dev`，每个网卡接口独立注册 8 个 Gauge，定时刷新。

**产出指标**：`network.receive.bytes|packets|errors|drop` / `network.transmit.bytes|packets|errors|drop`（每个网卡一组，tag `interface=<网卡名>`）

**状态**：[已实现，有隐患] `parseStats` 手写字符串解析无异常防护，`NumberFormatException` 或 `IndexOutOfBoundsException` 会撞上 `ScheduledThreadPoolExecutor` 静默终止周期任务的陷阱。触发概率极低（`/proc/net/dev` 格式 25 年未变），但一旦触发网络指标采集永久停止。

---

### REQ-004：磁盘空间监控

很多应用往本地磁盘写日志或审计文件，磁盘满了是经典静默杀手——Actuator 默认不监控磁盘。这个模块配置监控路径列表（默认 `logging.file.path`），暴露每个路径的 `disk.free`/`disk.used`/`disk.total` Gauge。

**产出指标**：`disk.free` / `disk.used` / `disk.total`（每个监控路径一组，tag `path=<路径>`）

**状态**：[待实现] Spring Boot Actuator 已有 `DiskSpaceMetricsBinder`（`SystemMetricsAutoConfiguration.java:70`），但它是内置健康检查用的（`/actuator/health` 里的 `diskSpace` 组件），不是独立的可插拔指标模块——无属性开关、无自定义路径列表。这个需求基于已有的 `DiskSpaceMetrics` 实现，包装成 09-observability 的可插拔模块格式。

---

## 二、中间件治理数据接入统一监控栈

### REQ-005：Sentinel 限流/熔断数据→Micrometer 桥接

Sentinel 的限流 Dashboard 和团队的标准 Prometheus/Grafana 监控栈是两套独立体系——运维需要两个面板才能看全链路。这个模块把 Sentinel 内部 `ClusterNode` 实时统计数据（totalRequest / successQps / blockQps / passQps / exceptionQps / avgRt）转成 Micrometer Gauge，直接进入 Prometheus。

**产出指标**：`sentinel.<resourceName>.total` / `sentinel.<resourceName>.success` / `sentinel.<resourceName>.pass` / `sentinel.<resourceName>.block` / `sentinel.<resourceName>.exception` / `sentinel.<resourceName>.total-qps` / `sentinel.<resourceName>.success-qps` / `sentinel.<resourceName>.block-qps` / `sentinel.<resourceName>.exception-qps` / `sentinel.<resourceName>.pass-qps` / `sentinel.<resourceName>.max-success-qps` / `sentinel.<resourceName>.rt`（TimeGauge，毫秒）——每个 resourceName 一组

**状态**：[已实现，有隐患] (1) 指标注册的 `get()`→`put()` 非原子操作，多线程并发时可导致不必要的重复注册尝试；(2) 异步提交借用 Sentinel 官方私有静态字段 `FlowRuleManager.SCHEDULER`（反射读取），Sentinel 升级改字段名即静默失效。

---

### REQ-006：JDBC SQL 执行指标

Actuator 的 DataSource 指标只管连接池状态（active/idle/max），不管每条 SQL。DBA 问"哪个 SQL 把库拖慢了"无法从 Prometheus 回答。这个模块桥接 P6Spy，拦截每条 JDBC 执行，暴露 SQL 类型维度的执行计数和慢查询耗时。

**产出指标**：`microsphere.jdbc.sql.success.count`（Counter，tag `type=SELECT|INSERT|UPDATE|DELETE`） / `microsphere.jdbc.sql.failure.count`（Counter，同 tag） / `microsphere.jdbc.slow-sql.time`（Gauge，tag `type` + `batches`，阈值默认 1s）

**状态**：[已实现，有隐患] (1) `private static MeterRegistry registry` 是静态字段却被实例构造函数赋值——多数据源场景下后创建的实例会静默覆盖之前的 registry；(2) `SLOW_SQL_TIME_METRIC_NAME` 把完整 SQL 字符串当 tag——高基数反模式，生产环境可致 Prometheus 指标基数爆炸。

**修正方向**：慢查询 Gauge 改为 Timer（自动输出 p50/p95/p99），tag 只保留 `type` 和 `batches`——完整 SQL 不放进 tag，改为 DEBUG 日志输出。

---

### REQ-007：Feign Client 调用指标（发散，待实现）

Spring Cloud OpenFeign 已内置 `MicrometerCapability`（`FeignClientsConfiguration.java:254`），但需要 `feign.metrics.enabled=true` 显式开启，且默认只提供基础计数器。这个模块基于已有的 Feign invocation handler 机制，补充更细维度的拆分。

**待确认（发散的边界）**：需要先验证 `MicrometerCapability` 具体暴露了什么指标、可以用什么样的扩展方式来增加产出指标的维度（例如按 `clientName` tag 区分不同的 Feign 接口等），之后才能给出完整需求规格——这项在最终实施阶段会做详细规划。

---

## 三、日志可靠性

### REQ-008：启动期日志缓冲

Log4j2 的远程 Appender（Kafka）启动比业务逻辑慢。应用启动早期产生的一批日志——包括可能导致 crash 的关键异常——在 Kafka 连上之前就已经消失了，运维在日志中心找不到任何线索。这个模块在应用启动时挂一个内存缓冲 Appender，等真正目标 Appender 就绪后转移过去；如果目标 Appender 永远无法就绪，降级到本地 RollingFile，不丢弃日志。

**产出**：无独立指标。实现方式是 Log4j2 的 `Appender` + Spring Boot 生命周期事件驱动的插入/转移/移除流水线。

**状态**：[已实现，❌ P1 bug 完全失效] `@EventListener(ApplicationPreparedEvent)` 时序错误——`registerListeners()` 早期事件重放早于 `EventListenerMethodProcessor.afterSingletonsInstantiated()` 注册 `@EventListener` 监听器，`KafkaAppenderConfiguration.onApplicationPreparedEvent` 永不触发。Kafka Appender 从未挂载、缓存日志被 `RemovingInMemoryAppenderListener` 清空丢弃。

**修正方向**：改用 `spring.factories` 注册 `ApplicationListener<ApplicationPreparedEvent>`（与 `AddingInMemoryAppenderListener` 同样的原生注册方式），或改为监听 `ApplicationStartedEvent` 晚期事件。增加 Kafka 永不可达时的本地文件降级。

---

### REQ-009：应用生命周期事件自动日志打点

运维排查问题时需要知道"应用什么时候启动的？启动参数是什么？有没有启动失败？WebServer 跑在哪个端口上？"——这些信息全部可以由 Spring Boot 生命周期事件自动触发日志记录，不需要开发者手写。这个模块监听 `ApplicationStartedEvent`（记录启动成功和参数）、`ApplicationFailedEvent`（记录启动失败和异常）、`ServletRequestHandledEvent`（记录每个 HTTP 请求的基本信息）、`WebServerInitializedEvent`（记录 WebServer 端口）——四个事件，三种配置类，全自动。

**产出**：DEBUG 级别日志（启动状态/失败异常/请求信息/WebServer 端口），无独立指标

**状态**：[已实现] `ApplicationLoggingAutoConfiguration` / `WebMvcLoggingAutoConfiguration` / `WebServerLoggingAutoConfiguration` 三个配置类，`@EventListener` 用法正确（监听的是 `Started`/`Failed`/`Handled`/`Initialized` 等晚期事件，不存在 09-01 P1 的时序问题）。`ApplicationLoggingAutoConfiguration` 额外注册了 `LoggingUncaughtExceptionHandler` 兜底未捕获异常。

---

### REQ-010：日志洪流检测指标（发散，待实现）

Log4j2 的 `BurstFilter` 能压但不能告诉你"哪个 Error 正在爆发"——它默默丢弃重复消息，Prometheus 上没这条指标。不用内存缓冲区存日志，而是基于 Log4j2 Filter 链做只检测不对日志内容做修改的监听——对每条日志做轻量 hash，维护滑动窗口计数器，转成 Prometheus Gauge（tag `pattern=<hash前缀>`），窗口超时后归零。

**产出指标**：`log.burst.matched`（Counter，tag `pattern=<hash 64 位截断前 8 位>`——不存完整文本，只做区分度）：每匹配到一个模板计数一次，向 Prometheus 报告最近一个收集窗口（~10 秒）内的匹配数

**状态**：[待实现] 已排除 HTTP 请求、磁盘空间、Feign 三个已有框架内置的能力——这三个已验证，不需要作为发散纳入本需求文档。

---

## 四、应用生命周期关键事件指标化

### REQ-011：线程池饱和度预警

Spring Boot 内置 `TaskExecutorMetricsAutoConfiguration` 只给 active/queue/pool 绝对值，且只覆盖 `ThreadPoolTaskExecutor` 和 `TaskScheduler`——不覆盖通用 `ExecutorService` 和 `ForkJoinPool.commonPool()`。这个模块扫描所有线程池 Bean（覆盖 Spring Boot 漏掉的两种类型），绑定标准 `ExecutorServiceMetrics`，额外暴露饱和度 Gauge。

**产出指标**：除以 Micrometer 官方 `ExecutorServiceMetrics` 自动输出的 pooled/pool/queued/active 四个维度的绝对值之外，额外增加 `executor.pool.saturation`（Gauge，`(active + queue) / max`）和 `executor.queue.saturation`（Gauge，`queue.size() / queue.remainingCapacity()`）

**状态**：[待实现] `JvmConfiguration.registerExecutorServiceMetrics` 已扫描 `ExecutorService` 和 `ForkJoinPool.commonPool()`（覆盖了 Spring Boot 内置的盲区）——在已有扫描逻辑上来补饱和度 Gauge，不重复造轮子。

---

### REQ-012：优雅关闭耗时与活跃连接数（发散，待实现）

Spring Boot 有 `GracefulShutdown` 但没有指标化。运维只能靠 grep "Graceful shutdown completed" 推断耗时。这个模块监听 `ContextClosedEvent`，记录关闭开始时的活跃连接数和最终耗时。

**产出指标**：`graceful.shutdown.active.connections`（Gauge，关闭开始时） / `graceful.shutdown.duration.seconds`（Gauge，关闭耗时） / `graceful.shutdown.completed.total`（Counter，每次关闭成功 +1）

**状态**：[待实现] Spring Boot 有关闭流程没有指标——在 GracefulShutdown 生命周期上挂 Gauge + Counter。

---

### REQ-013：配置刷新事件指标（发散，待实现）

Spring Cloud Config / Nacos 配置刷新是运行时关键事件——刷新成功、失败、频率、耗时——Actuator 完全没有对应指标。这个模块监听 `RefreshScopeRefreshedEvent`，在刷新成功或失败时触发 Counter + Timer。

**产出指标**：`config.refresh.success.total`（Counter） / `config.refresh.failure.total`（Counter，tag `reason`） / `config.refresh.duration.seconds`（Timer） / `config.refresh.last.timestamp`（Gauge，Unix 毫秒戳）

**状态**：[待实现] ⚠️ 这是一个需要跨模块验证的需求——目前的观察已经确认 Spring Cloud 存在对应事件且没有指标化；但完整验证需要在分析 07-sentinel / 06-nacos 模块时使用 Spring Cloud 源码交叉确认所有涉及的刷新机制与事件细节，届时再补全剩余规格。

---

## 五、框架层

### REQ-014：自定义指标框架（`AbstractMeterBinder`）

以上 1-13 都是预置实现。团队自己的特殊指标需求，如果不提供统一的模板方法，就会被分散在代码的各个角落里。这个框架让你只需定义两个方法——`supports`（当前环境是否满足条件）和 `doBindTo`（具体如何注册 Meter），入口 `bindTo` 是 `final`，做四件事：(1) 前缀短路——supports 不满足则直接返回，不进入 doBindTo 的 try/catch；(2) 异常完全隔离——catch(Throwable) 只打 error 日志，不往调用栈上抛；(3) 生命周期——框架自动处理 MeterRegistry 绑定和指标注册；(4) 自动配置——每个子模块一个 `@Configuration` + `microsphere.observability.<子系统>.enabled` 属性开关。

**产出**：抽象类 `AbstractMeterBinder` + 每个子模块的 `XxxConfiguration` + 对应的条件注解和属性定义。

**状态**：[已实现] 已有 6 个内部 `@Configuration` 类（`SystemConfiguration` / `CGGroupConfiguration` / `JvmConfiguration` / `SentinelMetricsConfiguration` / `KafkaMetricsConfiguration` / `PrometheusMetricsConfiguration`，其中最后一个因 `ConditionalOnBean(name=FQCN)` bug 完全失效）。

---

### REQ-014：跨日志框架 Filter 抽象与桥接

Log4j2/Logback/Log4j1 各自的 Filter 接口互不兼容——团队写了一份日志过滤逻辑（比如"只记录 com.example.payment 包下 ERROR 级别的日志"），如果哪天从 Log4j2 切到 Logback，这份逻辑就要用另一套 API 重写一遍。这个框架定义一套框架无关的 Filter 抽象（`io.microsphere.logging.filter.Filter`），每个日志框架通过一个桥接适配器（`XxxFilterAdapter`）把自身的事件对象（`LogEvent`/`ILoggingEvent`）转换成通用的 `(loggerName, level, message)` 三参数后，交给统一的 Filter 判断——过滤逻辑写一遍，适配器各写一个。

**产出**：`Filter` 接口（`filter(loggerName, level, message)` → `ACCEPT/NEUTRAL/DENY`） + `AbstractFilter` 模板方法基类 + `CompositeFilter` 组合器 + `Log4j2FilterAdapter` 桥接（后续可扩展 `LogbackFilterAdapter` 等）+ `LoggingNameFilter` 预置实现

**状态**：[已实现，部分未完成] 只有 Log4j2 桥接，无 Logback/Log4j1 版；`CompositeFilter` OR/XOR 未实现（`// TODO`），AND 语义写错，DENY 被忽略。

---

## 六、需求汇总表

| # | 需求 | 状态 |
|---|------|------|
| REQ-001 | 容器内存指标（cgroup） | 已实现，不完整（只 v1） |
| REQ-002 | 物理机 swap/虚拟/物理内存 | 已实现 |
| REQ-003 | 网卡流量指标 | 已实现，有隐患（parseStats） |
| REQ-004 | 磁盘空间监控 | 待实现（新增） |
| REQ-005 | Sentinel→Micrometer 桥接 | 已实现，有隐患（竞态 + 反射） |
| REQ-006 | JDBC SQL 执行指标（P6Spy） | 已实现，有隐患（高基数 tag + static 污染） |
| REQ-007 | Feign Client 调用指标 | 待实现（发散，需验证 OpenFeign 现有能力后再规格化） |
| REQ-008 | 启动期日志缓冲 | 已实现，❌ P1 bug 完全失效 |
| REQ-009 | 应用生命周期事件自动日志打点 | 已实现 |
| REQ-010 | 日志洪流检测指标 | 待实现（发散） |
| REQ-011 | 线程池饱和度预警 | 待实现（发散） |
| REQ-012 | 优雅关闭耗时 | 待实现（发散） |
| REQ-013 | 配置刷新事件指标 | 待实现（发散，需跨模块验证后再补完整规格） |
| REQ-014 | 自定义指标框架（`AbstractMeterBinder`） | 已实现 |
| REQ-015 | 跨日志框架 Filter 抽象与桥接 | 已实现，部分未完成 |

**总计**：15 项需求。已实现 10 项（其中 1 项不完整、3 项有隐患、1 项完全失效、1 项部分未完成）。待实现 5 项（其中 2 项标注为发散待验证，需要后续跨模块确认后再给完整规格）。

---

## 七、已排除的能力（不属于本需求文档）

- HTTP 请求级别延迟：Spring Boot Observation 框架默认自动采集 `http.server.requests`——不需要重复实现
- 磁盘空间健康检查：`DiskSpaceMetricsBinder` 在 `SystemMetricsAutoConfiguration` 中自动配置——不需要重复实现
- Feign 调用基础计数：OpenFeign `MicrometerCapability` 在 `FeignClientsConfiguration` 中自动注册——不需要重复实现
