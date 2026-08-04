# 09 - microsphere-observability 深度分析

## 源码信息

- **本地路径**: `/data/workspace/java-training-camp/cloud-native-code/projects/microsphere-observability`
- **GitHub**: https://github.com/microsphere-projects/microsphere-observability
- **规模**: 39 个主 Java 文件 + 7 个测试，7 个 Maven 模块（logging / logging-spring-boot / micrometer / micrometer-spring-boot / observability-spring-boot / dependencies / parent）
- **依赖**: Log4j2、Micrometer、Prometheus simpleclient、Alibaba Sentinel、P6Spy
- **git**: 1 提交（shallow clone），2025-02-21 创建，版本停留在 `0.0.1-SNAPSHOT`（从未正式发布，与 14-druid `0.2.19`/13-mybatis `450 提交` 形成鲜明对比——是本系列迄今最不活跃的项目）

## 项目定位与需求出发点（从代码反推的真实需求）

**一句话**：这个项目是 microsphere 生态的"可观测性补全层"——不重造日志/指标框架本身，而是基于 Log4j2/Micrometer/Prometheus 现有的成熟框架，补它们生态里缺失或不便集成的能力。

**需求派生表（从"框架没提供什么"出发，而不是从"我们做了什么"出发）**：

| 痛点 | 框架现状 | microsphere-observability 的答案 |
|------|---------|-------------------------------|
| 启动期日志丢失 | Log4j2 的 Appender（尤其远程 Appender 如 Kafka）依赖网络连接，应用启动早期可能未就绪 | `InMemoryAppender` 缓冲 + `AddingInMemoryAppenderListener`/`RemovingInMemoryAppenderListener` 生命周期管理 + `DefaultKafkaLayout` 自动复用文件 Appender 格式 |
| Sentinel 限流指标接不进 Prometheus | Sentinel 官方 Dashboard 基于内部内存统计+落盘文件，没有原生 Micrometer/Prometheus 集成 | `SentinelMetrics`（MeterBinder 通用）+ `SentinelCollector`（Prometheus Collector 专用，有 Prometheus 时优先；无 Prometheus 时退化用前者） |
| 容器/物理机/网卡维度的指标采集缺失 | Spring Boot Actuator 内置 JVM 指标拿不到 cgroup 限制、物理机 swap、网卡流量 | `CGroupMemoryMetrics` + `SystemMemoryMetrics` + `NetworkStatisticsMetrics` 三个类补盲区 |
| JDBC 执行级别的监控指标缺失 | Spring Boot 内置 DataSource 指标只有连接池状态，没有每条 SQL 的计数/成功失败/慢 SQL | `MicrometerJdbcEventListener`（P6Spy 插件，拦截 JDBC 执行） |
| 跨项目 i18n 日志延伸 | `microsphere-i18n` 项目解决了业务消息的国际化，但日志消息仍走硬编码 | `I18nLogger`（SLF4J 装饰器）+ `I18nLog4j2Filter`（Log4j2 Filter） |

**但有一个核心反讽需要正视**：以上需求中，**启动期日志缓冲**因为 `@EventListener(ApplicationPreparedEvent)` 时序 bug 完全失效（Kafka Appender 从未被真正挂载），**Sentinel→Prometheus Push 模式**因为 `ConditionalOnBean(name=...)` 参数语义误用完全失效，**i18n 日志延伸**的 `I18nLogger` 只有 `trace()` 一个重载实现——这个项目目前的状态是**"需求清晰但实现半成品"**，与 git 历史（1 提交、`0.0.1-SNAPSHOT` 从未发布）吻合。写文章规划时要如实呈现"设计意图对、但关键实现点未完成/有 bug"的状态，不能把它写得像个成熟项目。

## 探索结论（全部源码验证，39 主文件逐一读过）

### 两大子系统

- **logging（14 文件）**：以 Log4j2 为主的日志增强——`Logging` 接口对标 JDK `LoggingMXBean`（目前只有 `StandardLogging` 一个 JDK 实现）；`AbstractStatementFilter` 式模板方法（`AbstractFilter.filter` final 收敛 matches/onMatch/onMismatch）；`InMemoryAppender`"启动期日志缓冲，待真正 Appender 就绪后转移"设计；`I18nLog4j2Filter`/`I18nLogger` 基于 `microsphere-i18n` 的日志国际化
- **micrometer（16 文件）+ spring-boot 自动配置（9 文件）**：`AbstractMeterBinder` 模板方法（同一作者跨 13/14/09 三个项目复用的"final bindTo + 异常隔离 + 子类实现 supports/doBindTo"模式）；System/CGroup/Network 三种系统级指标采集；JMX 通用绑定骨架（`MBeanMetrics`）；**三套并行的 Sentinel 指标实现**（`AbstractSentinelMetrics`/`SentinelMetrics`/`SentinelCollector`，后两者通过 `@ConditionalOnMissingBean` 互斥装配，非简单重复）；P6Spy JDBC 拦截指标

### 两个严重 bug（均可用源码/官方文档严格证明，且都导致下游功能连锁失效）

1. **`Log4j2AutoConfiguration.KafkaAppenderConfiguration.onApplicationPreparedEvent`（`@EventListener`）理论上永远不会被触发**——用 `spring-framework` v6.2.17 源码证明：`AbstractApplicationContext.refresh()` 里 `registerListeners()`（第 625 行，早期事件重放点）先于 `finishBeanFactoryInitialization()`（第 628 行，`EventListenerMethodProcessor.afterSingletonsInstantiated()` 真正扫描注册 `@EventListener` 方法的地方）。连锁后果：Kafka Appender 从未挂载→`InMemoryAppender` 缓存的日志被静默丢弃；`MicrometerAutoConfiguration.KafkaMetricsConfiguration.getKafkaProducer` 永远 `findAppender` 不到→**KafkaClientMetrics 也永不绑定**。同项目内 `ApplicationLoggingAutoConfiguration`/`WebMvcLoggingAutoConfiguration`/`WebServerLoggingAutoConfiguration` 三个类监听 Started/Failed/WebServerInitialized 等**晚期**事件，`@EventListener` 用法完全正确——是很好的正反对照素材。
2. **`ConditionalOnEnabledPrometheusMetricsExport` 的 `@ConditionalOnBean(name="io.micrometer.core.instrument.Clock")` 用类全限定名当 bean 名称，永远不匹配**——Spring Boot 官方 `MetricsAutoConfiguration.java:54` 确认 Clock bean 名是 `"micrometerClock"`，`ConditionalOnBean.name()` 官方 javadoc 明确是"bean 名称"语义。连锁后果：`PrometheusMetricsConfiguration` 永不激活→`prometheusPushGatewayManager` 永不创建→**Prometheus Push Gateway 推送完全失效**；`ConditionalOnEnabledPrometheusPushGateway` 元注解引用它，同样失效。

### 中等问题（7 项）

- `InMemoryAppender`+`LogEventComparator`：`ConcurrentSkipListSet` 用只比较时间戳的 Comparator，同毫秒内多条日志被当重复元素静默丢弃
- `CompositeFilter.filter()`：自带 `// TODO`，OR/XOR 完全未实现，AND 语义写错，DENY 被忽略（**作者已知未完成**，非隐藏 bug）
- `NetworkStatisticsMetrics.parseStats`：定时任务内解析逻辑无异常防护，撞上 `ScheduledThreadPoolExecutor` 静默终止周期任务的陷阱
- `AbstractSentinelMetrics`/`SentinelMetrics`：`resourceClusterNodes.get()→put()` 非原子的 check-then-act 竞态，高并发下可能重复注册 Gauge
- `SentinelMetrics.addMetricsAsync`：反射借用 Sentinel 官方私有单线程 `FlowRuleManager.SCHEDULER` 做高频 metrics 任务提交——与 13-mybatis`Executors.getDelegate`、本项目`getFieldValue(kafkaAppender,"manager")`是同一类"反射读私有字段"风险的第 3/4 个实例
- `MicrometerJdbcEventListener`：`private static MeterRegistry registry` 被实例构造函数赋值（多数据源场景互相覆盖）；`SLOW_SQL_TIME_METRIC_NAME` 把完整 SQL 字符串当 tag——**Micrometer 高基数反模式**，生产环境可致指标基数爆炸
- `CGroupMemoryMetrics`+`ConditionalOnCGroup`：只支持 cgroup v1 路径格式，v2 环境下条件注解能通过但 `supports()` 返回 false，静默无指标无明确提示

### 明确的未完成/死代码（4 项）

- `I18nLogger`：`TODO : Finish`，仅 `trace(String,Object...)` 一个重载真正实现，debug/info/warn/error 全部级别全部空方法体（约 240 行空壳）
- `LoggingConfiguration`：死代码，无主代码引用，`@since TODO`
- `MBeanMetrics`/`MBeanAttributeMeterBinder`：完整的 JMX 通用绑定骨架，零实现类、零引用、零测试
- `Log4j2KafkaAppenderProperties.isEnabled()`：死代码，真正启用判断走的是条件注解

### 测试基建的系统性缺陷（5 项，可作独立小节）

- micrometer 侧测试（`CGroupMemoryMetricsTest`/`SystemMemoryMetricsTest` 等）走真断言（检查 Meter 是否存在），spring-boot 侧测试（`ApplicationLoggingAutoConfigurationTest`/`WebMvcLoggingAutoConfigurationTest`/`WebServerLoggingAutoConfigurationTest`）全部是"装配起来即通过"的浅层测试，没有一个验证事件监听器业务逻辑真的执行
- `MicrometerJdbcEventListenerTest.testAddMetrics` 无任何断言，且传入值恰好等于阈值（`1000000000ns`），完全没覆盖到高基数 tag 那行代码
- `NetworkStatisticsMetricsTest`：开了一个 `WatchService` 监听线程但**从未调用 `.start()`**，`thread.join(5000)` 对未启动线程立即返回——整段"动态发现新接口"验证代码是摆设

### 架构观察（跨项目视角，14-02 归纳过的"作者惯用模式"再添新证据）

- **`AbstractMeterBinder`**（final `bindTo` + 异常隔离 + 子类实现两个抽象方法）与 13-mybatis 的 `InterceptorsExecutorFilterAdapter`、14-druid 的 `AbstractStatementFilter.execute` 是同一套"模板方法+异常隔离"设计在第三个项目的复现
- **反射读私有字段模式第 3/4 次出现**：`SentinelUtils.findSentinelMetricsTaskExecutor`（反射读 `FlowRuleManager.SCHEDULER`）+ `MicrometerAutoConfiguration.getKafkaProducer`（反射读 `KafkaAppender.manager`→`KafkaManager.producer`）
- **至少 3 种不同的线程池管理策略并存**：`NetworkStatisticsMetrics` 自建独立线程池 / `SentinelMetrics` 借用 Sentinel 官方线程池 / `MicrometerUtils` 全局共享单例线程池——缺乏统一约定
- **`observability-spring-boot` 是纯依赖聚合模块**（两个自动配置文件均为空），只是把 logging-spring-boot + micrometer-spring-boot 打包成一个 starter，与 14-druid 的 spring-cloud 空壳（有 features.yaml 内容）不同——这是真正的"零内容"空壳

## 文章规划（3 篇）

### 09-01 Logging 增强：日志缓冲、过滤器桥接与国际化
> 核心命题：`InMemoryAppender` 的"启动期缓冲、待 Kafka Appender 就绪后转移"设计为什么理论上永远不会真正转移；microsphere 自定义 `Filter` 抽象如何桥接到 Log4j2 官方 `AbstractFilter`
- `Logging` 接口对标 `LoggingMXBean`（目前只有 JDK 实现）
- `AbstractFilter`/`CompositeFilter`/`LoggingNameFilter`：microsphere 框架无关 Filter 抽象 + `Log4j2FilterAdapter` 桥接；`CompositeFilter` 的已知未完成（OR/XOR/DENY）
- `InMemoryAppender`+`LogEventComparator`：设计意图（`transfer()` 到 Kafka Appender）+ 同毫秒去重丢失问题
- **核心 bug 完整证据链**：`ApplicationPreparedEvent` 时序（复用 HANDOVER.md 已确认结论）→`registerListeners()` vs `EventListenerMethodProcessor.afterSingletonsInstantiated()` 先后顺序→`@EventListener` 监听 `ApplicationPreparedEvent` 永不触发→Kafka Appender 从未挂载→日志静默丢弃
- 正反对照：`ApplicationLoggingAutoConfiguration` 等三个类的正确 `@EventListener` 用法
- `I18nLog4j2Filter`/`I18nLogger`：日志国际化设计 + `I18nLogger` 的"仅一个重载实现"未完成状态
- `LoggingConfiguration` 死代码 + `DefaultKafkaLayout`（精巧但因上游 bug 从未被执行）

### 09-02 Micrometer 指标采集：模板方法、系统指标与三套 Sentinel 实现
> 核心命题：`AbstractMeterBinder` 模板方法与 13/14 的同构关系；为什么 Sentinel 指标被实现了三次
- `AbstractMeterBinder`：与 13-mybatis/14-druid 模板方法模式的第三次复现（14-02 已归纳的作者惯用模式再添一证）
- System/CGroup/Network 三种系统指标：`com.sun.management.OperatingSystemMXBean` 反射检测、cgroup v1 路径与 v2 环境的适配缺口、`/proc/net/dev` 手写解析 + 定时任务无异常防护陷阱
- MBean 通用绑定骨架（`MBeanMetrics`）：死代码分析——完整设计但零实现零引用
- **三套 Sentinel 指标实现对比**：`AbstractSentinelMetrics`（无实现类）/`SentinelMetrics`（内存态 MeterBinder 风格,借用官方私有线程池）/`SentinelCollector`（Prometheus Collector 风格,读官方落盘指标文件）——通过 `@ConditionalOnMissingBean` 互斥装配的真实设计意图，而非简单重复；两者共有的 check-then-act 竞态问题
- P6Spy JDBC 拦截指标：`MicrometerJdbcEventListener` 的 static 字段污染 bug + 高基数 tag 反模式（完整 SQL 字符串当 tag）

### 09-03 自动配置错误案例与测试基建缺陷
> 核心命题：`ConditionalOnBean(name=...)` 参数语义混淆如何让 Prometheus Push Gateway 整条链路静默失效；两个严重 bug 折射出的"事件时序"与"条件注解参数语义"两类系统性风险
- **`ConditionalOnEnabledPrometheusMetricsExport` bug 完整证据链**：`ConditionalOnBean.name()` 官方 javadoc 语义→Spring Boot 官方 `MetricsAutoConfiguration.micrometerClock()` bean 名核实→条件永不匹配→Push Gateway 完全失效
- 两个严重 bug 的共性总结：都是"看起来正确、实际因框架 API 细节误用而完全失效"的隐蔽错误，且都没有任何运行时错误提示（条件不满足/事件未触发都是静默行为）
- `MicrometerAutoConfiguration` 全景：6 个内部 `@Configuration` 类的装配关系、`registerExecutorServiceMetrics` 手动扫描线程池 Bean 的设计
- 测试基建系统性缺陷：spring-boot 侧 3 个"装配起来即通过"浅层测试 vs micrometer 侧真断言测试的对比；`MicrometerJdbcEventListenerTest` 无断言；`NetworkStatisticsMetricsTest` 未启动线程的摆设代码
- 生产评估：适用场景、风险清单汇总、项目活性（`0.0.1-SNAPSHOT` 从未发布，是本系列最不活跃项目）

## 与其他模块的引用约定

- **反射读私有字段模式**：本模块新增第 3/4 个实例（`SentinelUtils.findSentinelMetricsTaskExecutor`/`getKafkaProducer`），与 13-mybatis（`Executors.getDelegate`/`SqlSessionFactoryBean.plugins`）、14-druid 无此模式（14-druid 是"手写 switch 分支替代现成多态方法"的另一类风险）汇总，未来 07-sentinel 分析时可继续验证是否有第 5 个实例
- **模板方法+异常隔离模式**：13-mybatis `InterceptorsExecutorFilterAdapter` → 14-druid `AbstractStatementFilter.execute` → 09-observability `AbstractMeterBinder.bindTo`，三个项目的第三次复现，09-02 展开对照
- **`ApplicationPreparedEvent` 时序结论复用**：HANDOVER.md 已有三重验证（refresh 之前发布），本模块 09-01 是这个结论的第四次引用来源，且首次用它反证了一个具体的 `@EventListener` 误用 bug——比之前单纯的时序陈述更有实证价值，建议 HANDOVER.md 补充这个案例引用
