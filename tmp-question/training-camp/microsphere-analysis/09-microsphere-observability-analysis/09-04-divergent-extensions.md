# 09-04：发散扩展——基于现有框架还能补什么

> **核心命题**：09-observability 已经做了 5 个预置指标采集（容器内存、物理机 swap、网卡流量、Sentinel→Prometheus、JDBC SQL 计数）和一套模板方法框架（`AbstractMeterBinder`），不是终点——基于"桥接已有拦截点 + 转成 Micrometer 指标 + 自动配置可插拔"这套已经工作过的模式，还有哪些 Spring Boot Actuator 没覆盖的生产盲区可以用同样方式补上？

本文每一项发散都经过核实——标注哪些已有框架实现、哪些是框架有机制但没指标化、哪些是完全空白。

---

## 一、线程池饱和度预警

### 现状

Micrometer 官方 `ExecutorServiceMetrics` 已经能绑定线程池的当前 active 数、队列大小、已完成任务数——但这些都是**绝对值**。运维在 Grafana 上看到"active=3、queue=80"无法直接判断当前线程池是否接近饱和——需要他**心算** "queue=80，max=100，所以饱和度 80%"。

### 发散设计

基于 `JvmConfiguration.registerExecutorServiceMetrics`（`MicrometerAutoConfiguration.java:151-158`）已有的扫描 + 绑定逻辑，增加一个饱和度 Gauge：

```
对每个已绑定的 ExecutorService：
  poolSaturation = (queue.size() + activeCount) / maxPoolSize
  queueSaturation = queue.size() / queueCapacity
```

不需要手写新的指标采集逻辑——在现有 `registerExecutorServiceMetrics` 方法里加一行 Gauge 注册就行。这个 Gauge 的取值函数是动态的（`Supplier` 模式，每次 Micrometer 采集时重新计算），不占 CPU。

**已有框架实现？** 没有。`ExecutorServiceMetrics` 只给绝对值，不给饱和度百分比。

---

## 二、优雅关闭耗时与活跃连接数

### 现状

Spring Boot 有 `WebServerGracefulShutdownLifecycle`（Tomcat/Netty/Jetty 各自实现的 `GracefulShutdown`），能配置 `server.shutdown=graceful` + `spring.lifecycle.timeout-per-shutdown-phase`。但**没有把关闭过程中的关键数据指标化**——运维只能靠日志 grep "Graceful shutdown completed" 来推断耗时，无法从 Prometheus 上看到关闭期间还有多少活跃请求没有被处理完。

### 发散设计

监听 Spring Boot 关闭事件，在关闭前后各取一个快照，暴露两个 Gauge：

```
graceful.shutdown.active.connections  → 关闭开始时有多少活跃请求
graceful.shutdown.duration.seconds    → 关闭过程耗时（从 ContextClosedEvent 到容器彻底停止）
```

同时暴露一个 Counter：`graceful.shutdown.completed.total`——每次应用关闭成功时 +1，帮助运维统计"应用因为多少次正常关闭而退出"。

实现方式：`@EventListener(ContextClosedEvent.class)` 记录开始时间戳 + 活跃连接数（通过 Tomcat 的 `MBeanServer` 或 Reactor Netty 的 `HttpServer` API 获取），`@EventListener(ApplicationFailedEvent.class)` 记录结束时间戳并注册 Gauge。全部通过 `AbstractMeterBinder` 实现，不额外引入依赖。

**已有框架实现？** 没有。Spring Boot 有关闭流程但没有指标化。

---

## 三、配置刷新事件指标（Nacos / Spring Cloud Config）

### 现状

Spring Cloud 有 `RefreshScopeRefreshedEvent`（`spring-cloud-context/.../scope/refresh/RefreshScopeRefreshedEvent.java`），Nacos Config 有 `NacosConfigRefreshEventListener`。但这两个事件**没有任何对应指标**——运维无法从 Prometheus 上看到"配置刷新成功还是失败？刷新频率多高？最近一次刷新是什么时候？刷新耗时多长？"

### 发散设计

监听 `RefreshScopeRefreshedEvent` 和 `EnvironmentChangeEvent`，用 `AbstractMeterBinder` 实现：

```
config.refresh.success.total   → Counter：每次刷新成功 +1
config.refresh.failure.total   → Counter：每次刷新失败 +1（tag: reason）
config.refresh.duration.seconds → Timer：每次刷新的耗时
config.refresh.last.timestamp  → Gauge：最近一次刷新时间（Unix 毫秒戳）
```

实现方式：通过 `@EventListener` 监听刷新事件（确认事件类型为晚期事件——`RefreshScopeRefreshedEvent` 在 `ContextRefreshedEvent` 之后才发布，`@EventListener` 可用，与 09-01 P1 不同），在事件回调中更新 Counter/Timer。自动配置里加一个 `microsphere.observability.config-refresh.enabled` 开关。

**已有框架实现？** 没有。Spring Cloud 有事件机制但没有对应的指标。

---

## 四、日志洪流检测指标

### 现状

Log4j2 的 `BurstFilter`（`org.apache.logging.log4j.core.filter.BurstFilter`）能控制日志输出的最大速率——当某条日志的速率超过阈值时，丢弃后续重复消息。这是**压**，不是**检测**。运维看不到"目前哪个 Error 正在爆发"——`BurstFilter` 默默地丢弃，Prometheus 上没有这条指标。

### 发散设计

做成一个**只计数、不过滤**的检测器：基于 `InMemoryAppender` 或 `Log4j2FilterAdapter` 桥接已有的 Log4j2 Filter 链（`AbstractLifeCycle implements Appender` 或 extends `AbstractFilter`），对每条日志做轻量级 hash（不存完整文本），维护一个滑动窗口计数器（`ConcurrentHashMap<Long, LongAdder>`），每隔 X 秒（可配置，默认 10s）把计数器高的那些 hash slot 转成 Prometheus Gauge：

```
log.burst.detected  → Gauge, tag: hash=0xABCD, label="手动配置的人类可读规则名"
log.burst.detected.count → 窗口内的爆发次数
```

其中 `hash` 只作为关联标识，不直接替换为日志原文（避免 cardinality 爆炸）——需要用户在配置中手工将关心的 hash → 人类可读的日志名做一个映射。滑动窗口 Clear 后计数器归零。

**已有框架实现？** Log4j2 `BurstFilter` 能压但不能指标化。Micrometer 没有这个维度的指标。

---

## 与已有框架的关系（诚实标注）

| 发散方向 | 任何已有框架实现了吗 | 关系 |
|---------|-------------------|------|
| 线程池饱和度预警 | ⚠️ Spring Boot 内置 `TaskExecutorMetricsAutoConfiguration` 只覆盖 `ThreadPoolTaskExecutor`/`TaskScheduler`——**不覆盖** `ExecutorService` 通用类型和 `ForkJoinPool.commonPool()`（09-observability 的 `JvmConfiguration` 恰好扫描了这两个 Spring Boot 漏掉的类型）。饱和度 Gauge 是目前两者都缺的 | 09-observability 的扫描范围已宽于 Spring Boot 内置——在这个基础之上，补饱和度 Gauge 水到渠成 |
| 优雅关闭耗时 | ❌ Spring Boot 有机制没指标化 | 在 `GracefulShutdown` 生命周期上挂两个 Gauge + 一个 Counter |
| 配置刷新指标 | ❌ Spring Cloud 有事件没 Counter/Timer | 在已有事件机制上挂 Counter + Timer |
| 日志洪流检测 | ⚠️ Log4j2 `BurstFilter` 能压但不能指标化 | 不同的设计目标——只检测不拦截，结果变成 Prometheus 指标 |
| HTTP 请求级别延迟 | ~~已有~~ Spring Boot Observation 默认自动采集 `http.server.requests` | 不需要做，已经在框架层 | 
| 磁盘空间 | ~~已有~~ `DiskSpaceMetricsBinder` 在 `SystemMetricsAutoConfiguration` 中自动配置，默认启用 | 不需要做，已经在了 | 
| Feign 调用指标 | ~~已有~~ OpenFeign `FeignClientsConfiguration` 中 `MicrometerCapability` 自动配置，默认启用 | 不需要做，已经在了 |

**四个发散方向全部遵循 09-observability 已有的架构模式**：借已有拦截点（`ExecutorServiceMetrics`/`GracefulShutdown`/`RefreshScopeRefreshedEvent`/Log4j2 Filter 链）→ 做轻量适配（不重复造轮子，只在现有对象上挂 Gauge/Counter/Timer）→ 自动配置加开关（`microsphere.observability.<子系统>.enabled`，`matchIfMissing=true`）→ 所有实现继承 `AbstractMeterBinder`，`supports()` 检查环境（优雅关闭指标只在 Web 应用场景下启用，配置刷新指标只在 classpath 存在 `RefreshScopeRefreshedEvent` 类时启用）。不是推倒重来的新架构，是在现有框架基础上多补几个"常用实现"。
