# 14-REQ：Druid 连接池 Spring Boot 增强需求规格

> 以 Alibaba Druid（github.com/alibaba/druid，1682 文件）视角，描述生产环境中连接池 Spring Boot 集成的需求。microsphere-druid（12 文件）作为参考实现。
>
> 基准：Java 17+，Druid 1.2.x+，Spring Boot 3.x

---

## 项目定位

**Druid 是 Alibaba 开源的数据库连接池——自带 SQL 防火墙（WallFilter）、慢查询统计（StatFilter）、Web 监控页面。它的 `druid-spring-boot-starter` 已经提供了 Spring Boot 自动配置。microsphere-druid 在此基础上加了 3 个增量**：Statement 级的 Filter 抽象简化、Spring Cloud 配置传播、Actuator 连接池元数据。

**源码**：微球 12 文件，官方 Druid 1682 文件。

---

## 核心需求

### REQ-001：Statement 级 SQL 拦截 Filter

**问题**：Druid 的 `Filter` 机制功能强大但接口复杂（10+ 个方法）。大多数场景只需要"SQL 执行前/执行后"两个钩子。

**产出**：`AbstractStatementFilter` —— 把 Druid Filter 接口简化为 `beforeExecute/afterExecute/onError` 三个钩子。微球 07-sentinel 和 13-mybatis 都基于它做 SQL 拦截。

**状态**：[已实现]

### REQ-002：Spring Boot 自动配置 + 连接池指标

**问题**：Druid 官方 starter 提供自动配置，但连接池指标（maxActive/active/queue/waitCount）没有暴露为标准 Actuator 端点。

**产出**：`DruidDataSourcePoolMetadata` 实现 Spring Boot `DataSourcePoolMetadataProvider` SPI——暴露连接池的 `active/max/min/wait` 指标到 `/actuator/health` 和 JMX。

**状态**：[已实现]

### REQ-003：Spring Cloud 配置传播

微球 `AlibabaDruidCloudAutoConfiguration` 监听 `EnvironmentChangeEvent` 并传播 Druid 配置变更。

**状态**：[已实现]（官方 starter 已有类似能力，微球增加 HasFeatures actuator）

---

## 发散需求

### REQ-N01：Druid WallFilter 误杀白名单动态调整

**生产痛点**：SQL 防火墙误杀了正常 SQL——运维只能重启改配置。生产需要运行时加白名单。

**产出**：`WallConfigManager` —— `/actuator/druid/wall` 端点——查看当前拦截规则 + 动态添加白名单 `wall.addAllowSql("/admin/export")`。

**状态**：[待实现]

### REQ-N02：Druid 慢查询分级告警

**生产痛点**：`StatFilter` 只知道某条 SQL "慢"，但不知道是"突然变慢"还是"一直慢"。300ms 对 OLTP 是慢的但对报表查询可能正常。

**产出**：`GradedSlowSqlDetector` —— 按 SQL 模式分组建立历史基线 → 偏离基线 2σ 触发 WARN → 一级告警（500ms绝对阈值）+ 二级告警（偏离基线）。

**状态**：[待实现]

### REQ-N03：Druid 连接池水位自动调节

**生产痛点**：白天 QPS 高需要 50 个连接，凌晨低峰只需要 10 个——但连接池大小是死的。浪费数据库连接资源。

**产出**：`AdaptivePoolSizer` —— 根据 `waitCount/activePeak` 指标每小时评估 → 连续 3 个窗口 idle 连接过剩→缩减 `maxActive`；`waitCount > 0` 持续 5 分钟→扩容。

**状态**：[待实现]（Druid 已有 `isLowWaterLevel()` 探测，但无自动调节——`MaxActiveChangeTest` 是手动测的）

---

### REQ-N04：Druid StatFilter → Micrometer 指标桥接

**生产痛点**：Druid 有自己的 `StatFilter` 统计（SQL耗时/执行次数/并发）和 `MetricCollector`，但**不是 Micrometer 格式**——Prometheus/Grafana 没有标准的 `druid_sql_duration_ms` 指标。运维只能在 Druid 内置的 `/druid/index.html` Web 监控页面的表格看数据。

**产出**：`DruidMicrometerMetrics` —— 从 `DruidStatService` JSON API 或 `StatFilter` 的 `getDataSourceStatDataList()` 提取指标 → Micrometer Timer/Counter —— `druid_sql_duration_ms{sql,dataSource}` + `druid_sql_execute_count_total{sql,dataSource}` + `druid_pool_active_connections{dataSource}`。

**状态**：[待实现]

---

## 版本历史

| 版本 | 日期 | 变更 |
|------|------|------|
| — | 2026-08-03 | REQ 文档编写（Druid 视角） |
