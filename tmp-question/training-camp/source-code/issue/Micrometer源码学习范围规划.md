# Micrometer 源码学习范围规划

> **版本**: 1.17.x (main分支)
> **仓库**: `/data/workspace/source-code/code/spring/micrometer/`
> **规模**: 789 个源文件，15+ 子模块，core/ 为核心
> **日期**: 2026-08-03

---

## 一、仓库概况

Micrometer 是 CNCF 旗下的 JVM 指标采集门面库，Spring Boot Actuator 默认使用它暴露 `/actuator/metrics` + `/actuator/prometheus` 等端点。核心是 **Meter → MeterRegistry → MeterFilter** 三层模型——业务代码通过 `Meter` 类型（Timer/Counter/Gauge/DistributionSummary/LongTaskTimer）记录指标，`MeterRegistry` 负责聚合存储，`MeterFilter` 负责过滤/转换/分发。

**核心模块**：

| 模块 | 职责 | 状态 |
|---|---|---|
| `micrometer-core/` | 核心引擎：Meter 体系、MeterRegistry、MeterFilter、@Timed 注解 | ✅ |
| `micrometer-commons/` | 公共工具 | 淘汰 |
| `micrometer-observation/` | Observation API（分布式追踪上下文） | 🟡 |
| `micrometer-test/` | 测试支持 | 淘汰 |
| `micrometer-java11/21/` | JDK 11/21 特定优化 | 淘汰 |
| `micrometer-jakarta9/` | Jakarta EE 9+ 适配 | 淘汰 |
| `micrometer-jetty11/12/` | Jetty 集成 | 淘汰 |
| `micrometer-bom/` | BOM | 淘汰 |

---

## 二、知识域规划

### 🔴 核心域（4 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| M-1 | **Meter 类型体系** | Meter, Timer, Counter, Gauge, DistributionSummary, LongTaskTimer, FunctionCounter, FunctionTimer, TimeGauge | **5 种核心 Meter**：`Timer`(耗时+吞吐—SLA `histogram`/`sla`/`publishPercentiles`)、`Counter`(单调递增计数)、`Gauge`(瞬时值—`WeakReference` 持有对象防 GC)、`DistributionSummary`(分布统计—同 Timer 但不计耗时)、`LongTaskTimer`(长任务—`start()`/`stop()` 记录正在执行的任务数+累计耗时)；**FunctionCounter/FunctionTimer**：不自行记录，通过 Supplier 函数读取外部值（如队列深度）；**@Timed/@Counted**：AOP 注解自动记录——`TimedAspect` 切面拦截 |
| M-2 | **MeterRegistry 注册与聚合** | MeterRegistry, SimpleMeterRegistry, CompositeMeterRegistry, StepMeterRegistry, PushMeterRegistry | **注册表体系**：`SimpleMeterRegistry`(内存 HashMap 存储—最新值)、`CompositeMeterRegistry`(组合多个 Registry—Fan-out 写入)、**`StepMeterRegistry`**(步长时间窗口聚合—`stepMillis` 周期轮转，Prometheus/Influx/Graphite 继承此类)、**`PushMeterRegistry`**(定时推送—`stepMillis` 间隔 `publish()` 到外部)；**Meter 注册**：`registry.timer(name, tags...)` → 同名同 Tag 返回同一 Meter 实例（`ConcurrentHashMap` 去重）；**`Meter.Id`**：name + Tags 构成唯一标识——`Convention` 接口定义命名规范 |
| M-3 | **MeterFilter 过滤与分发** | MeterFilter, MeterFilterReply, @AutoConfiguredTimed | **四层过滤链**：`MeterFilter` 提供 `map()`(ID 转换—重命名/添加标签)、`accept()`(DENY/NEUTRAL/ACCEPT—拒绝/通过/直接接受)、`configure()`(修改 DistributionStatisticConfig—SLA/百分位/histogram)；**常见 Filter**：`commonTags`(添加全局标签)、`deny()`(按 name pattern 拒绝)、`maximumAllowableTags`(限制标签基数防爆炸)、`maxAllowedMetrics`(限制指标总数)；**@AutoConfiguredTimed**：Spring Boot 自动为所有端点配置 `http.server.requests` 的百分位直方图 |
| M-4 | **DistributionStatisticConfig 直方图** | DistributionStatisticConfig, HistogramSnapshot, Clock | **直方图配置**：`percentilesHistogram`(生成 HDR Histogram—内存占用但精度高)、`sla`(Service Level Agreement—固定边界桶 `[10ms,100ms,1s,10s]`)、`minimumExpectedValue/maximumExpectedValue`(范围约束)；`DistributionStatisticConfig.merge()` 优先级合并（MeterFilter configure > Meter builder > Registry 默认） |

### 🟡 扩展域（3 个）

| 编号 | 域 | 核心类 | 说明 |
|:---:|---|---|---|
| M-5 | **Observation API（分布式追踪）** | Observation, ObservationRegistry, ObservationHandler | **Micrometer 1.10+ 新增**：`Observation` 封装 Span 生命周期→`start()`/`stop()`/`error()`/`event()`/`scope()`；`ObservationHandler` SPI——`TimerObservationHandler`(Metrics)、`TracingObservationHandler`(Traces)、`LoggingObservationHandler`(Logs)；Spring Boot 3 整合——`ObservationAutoConfiguration` 自动装配 |
| M-6 | **PrometheusMeterRegistry** | PrometheusMeterRegistry, PrometheusNamingConvention, TextFormat | **Prometheus 输出**：`PrometheusMeterRegistry` 扩展 `StepMeterRegistry`→`scrape()` 生成 Prometheus TextFormat(`# HELP/# TYPE + 指标名{labels} 值 时间戳`)；`_total` 前缀转换、`_bucket{le=}` 直方图桶标记 |
| M-7 | **MeterBinder 基础设施指标** | JvmGcMetrics, JvmMemoryMetrics, JvmThreadMetrics, ProcessorMetrics, ExecutorServiceMetrics, TomcatMetrics, LogbackMetrics, DataSourcePoolMetrics, HikariCPMetrics | **binder/ 包 175 文件**——Spring Boot Actuator 自动采集的全部基础设施指标：`MeterBinder.bindTo(registry)` SPI——JVM GC/内存/线程/CPU、Tomcat 连接池、HikariCP 连接池、Logback 日志、Kafka/Jetty 等——**这是 Micrometer 对 Spring Boot 用户的最大价值**，之前完全遗漏 |

---

## 三、淘汰清单

| 模块/功能 | 理由 |
|---|---|
| `micrometer-commons/` | 公共工具 |
| `micrometer-test/` | 测试支持 |
| `micrometer-java11/21/` | JDK 版本特定优化 |
| `micrometer-jakarta9/` | EE 9 适配 |
| `micrometer-jetty11/12/` | Jetty 集成 |
| `micrometer-observation-test/` | 测试 |
| `micrometer-osgi-test/` | OSGi |
| 其他 MeterRegistry 实现（Graphite/Influx/StatsD/...）| 用 Prometheus 即可 |
| `micrometer-bom/` `benchmarks/` `concurrency-tests/` | 构建/测试 |

---

## 四、统计

| 类别 | 数量 |
|---|---|
| 🔴 核心域 | 4 |
| 🟡 扩展域 | 3 |
| **总域** | **7** |

---

## 五、学习顺序建议

```
M-1 Meter 类型体系（理解 Timer/Counter/Gauge/DistributionSummary 五种指标）
  → M-2 MeterRegistry 注册与聚合（理解 Step/Composite 聚合模式）
    → M-3 MeterFilter 过滤链（理解 accept/map/configure 三层）
      → M-4 DistributionStatisticConfig（理解百分位直方图）
        → M-5/M-6 按需深入
```

以上规划完成，共 **4🔴+2🟡=6 域**。Micrometer 是 CNCF 项目，非 Spring 组件——聚焦核心 Meter→Registry→Filter 三层。
