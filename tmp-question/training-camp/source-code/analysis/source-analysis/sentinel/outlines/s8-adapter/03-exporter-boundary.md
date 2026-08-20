# 指标导出与扩展边界

> S-8 下篇。本文讲指标 exporter、Quarkus 两阶段扩展，以及适配器与 core 的边界。

## 悬念

适配器很多，但它们是否改变 Sentinel 的核心规则？指标 exporter 是否参与统计？Quarkus 的 deployment/runtime 又分别承担什么？

答案是：大多数 S-8 模块都是外围包装和结果消费，不重新实现 core 规则。

## 一、JMXMetricExporter:定时收集

`JMXMetricExporter` 实现 `MetricExporter`，内部持有：

- `MetricCollector`
- `MetricBeanWriter`
- 单线程 `ScheduledExecutorService`

`start()` 每秒调度 `JMXExportTask`，`export()` 执行：

```java
metricBeanWriter.write(metricCollector.collectMetric());
```

它不直接操作 FlowRule/AuthorityRule，也不参与 entry 判定；它定时收集已有统计，再写入 JMX bean。

## 二、Prometheus:InitFunc + HTTPServer

`PromExporterInit` 实现 `InitFunc`。初始化时：

1. new `SentinelCollector` 并 register
2. 读取 Prometheus port
3. 启动 `HTTPServer`
4. 注册 JVM shutdown hook 停止 server

```java
new SentinelCollector().register();
int promPort = PrometheusGlobalConfig.getPromFetchPort();
server = new HTTPServer(promPort);
```

Prometheus 来拉取 `/metrics` 时，collector 再读取 Sentinel 指标。初始化异常只记录 warning，不把 exporter 故障传播到 core entry 主链。

## 三、Reactor 与 WebFlux 的边界

WebFlux filter 和 Reactor transformer 不重新实现限流算法：

- filter 提取 path/origin/context
- transformer 包装 Publisher
- subscriber 负责 entry/exit
- 规则判定仍进入 SphU/CtSph/ProcessorSlotChain

这也是适配器边界的典型：外围负责“何时进入、资源叫什么、如何退出”，core 负责“是否放行、如何统计”。

## 四、Quarkus:deployment/runtime 两阶段

Quarkus adapter 把工作拆成两个阶段：

- deployment processor：构建期通过 `@BuildStep` 产出 `FeatureBuildItem`、`AdditionalBeanBuildItem`，例如 annotation 模块把 `SentinelResourceInterceptor` 注册为附加 bean (`SentinelAnnotationQuarkusAdapterProcessor.java:34-42`)
- native image deployment：通过 `@BuildStep(onlyIf = NativeBuild.class)` 产出 `RuntimeInitializedClassBuildItem`，把 `Env`、`InitExecutor`、`ClusterStateManager`、`FlowRuleManager` 等类标记为运行时初始化 (`SentinelNativeImageProcessor.java:43-59`)

这样做不是 Sentinel 自己的规则设计，而是适应 Quarkus build-time augmentation 和 native image 的生命周期约束。

## 五、适配器边界的统一模型

可以把所有适配器归纳为四类职责：

1. Resource extractor：从请求/方法/RPC invocation 提取资源名
2. Origin parser：提取调用方 origin
3. Lifecycle bridge：调用 SphU/AsyncEntry 并在正确时机 exit
4. Error bridge：把 BlockException 转成框架响应，把业务异常交给 Tracer/fallback

DataSource 与 exporter 是另外两类：

- DataSource：外部配置 → property
- Exporter：core metrics → JMX/Prometheus

它们都不应该把外围协议细节泄漏进 core 规则实现。

## 悬念回收

S-8 的边界可以压缩成一句话：

```text
外围框架/配置/监控协议
  -> 适配器翻译层
  -> Sentinel core entry/rule/statistic
  -> 适配器再翻译回响应/配置/指标
```

适配器数量很多，但核心语义只有一套。真正需要深挖的不是每个类，而是每个模块如何完成这四个桥接职责。

## 锚点

- `JMXMetricExporter.java:31-63`
- `PromExporterInit.java:24-46`
- `SentinelWebFluxFilter.java:42-57`
- `SentinelReactorTransformer.java:34-51`
- `SentinelResourceAspect.java:50-73`
- `SentinelAnnotationQuarkusAdapterProcessor.java:34-42`
- `SentinelNativeImageProcessor.java:43-59`
