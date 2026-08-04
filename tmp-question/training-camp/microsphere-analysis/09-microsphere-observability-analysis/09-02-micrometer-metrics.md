# 09-02：Micrometer 指标采集——模板方法、系统指标与 Sentinel 分层实现

> **核心命题**：`AbstractMeterBinder` 的 `bindTo`（final 收敛 + supports 短路 + doBindTo 异常隔离）是同一作者在 13-mybatis/14-druid/09-observability 三个跨层级项目中第四次复用的模板方法模式——但每次复用时外层入口收敛方式和内层抽象粒度各不相同，需要分层对比才能看清"哪里同构、哪里不同"；Sentinel 的限流指标有两套可运行实现（`SentinelMetrics` MeterBinder 风格、`SentinelCollector` Prometheus Collector 风格，通过 `@ConditionalOnMissingBean` 互斥装配）加一个无实例的抽象基类（`AbstractSentinelMetrics`，留空作扩展接口），两者构成"有 Prometheus 专用路径时不走通用退化路径"的分层退路，而非简单的代码重复。

---

## 一、`AbstractMeterBinder`：与 13/14 同构的模板方法（第四次复现）

### 1.1 结构对比：不是一张扁平的 4 列表，而是两层结构

这套"模板方法 + 异常隔离"模式在 13-mybatis、14-druid、09-observability 中共出现 4 次（`InterceptorsExecutorFilterAdapter`、`AbstractStatementFilter.execute`、`AbstractFilter.filter`、`AbstractMeterBinder.bindTo`），不能靠一张扁平对比表描述——因为**外层入口收敛方式**和**内层抽象方法粒度**是两层不同的设计选择，每次复用时组合不同。

**外层：入口收敛方式**

| | 13-mybatis | 14-druid | 09-observabling-logging | 09-observability-micrometer |
|---|---|---|---|---|
| 入参粒度 | 10 个 `Executor` 方法（`update`/`query`/`commit` 等）各自调用同一套内部模板 | 13 个 `final` Statement 执行方法统一调用 `execute(statement, callback)` | 单方法 `filter(loggerName, level, message)` | 单方法 `bindTo(registry)` |
| 模式 | **多入口各自收敛**——不同操作路径走同一个责任链数组+游标 | **多入口统一收敛**——多个 JDBC 方法签名包成 lambda 进同一个模板 | **单入口** | **单入口** |
| 为什么选这种形式 | MyBatis `Executor` 接口有 15 个方法,10 个走链,必须各自作为入口 | Druid `Filter` 接口有 491 个方法,13 个 Statement 相关,必须各自声明 `final` 防止子类再覆写 | `Filter` 本身只做一件事,不需要多入口 | `MeterBinder` 接口本身只有一个 `bindTo` 方法 |

**内层：抽象方法粒度（子类需要实现的维度）**

| | 13-mybatis | 14-druid | 09-observability-logging | 09-observability-micrometer |
|---|---|---|---|---|
| 抽象方法数 | 0（`ExecutorFilter` 是接口,10 个 default 方法可选覆写） | 2（`beforeExecute`/`afterExecute`） | 1（`matches`）+ 2 个配置字段（`onMatch`/`onMismatch`） | 2（`supports`/`doBindTo`） |
| 扩展自由度 | 最高——子类只需关心 10 个方法中自己要拦截的那几个 | 最低——子类**只能**覆写两个回调，入口方法全部 `final` | 中等——匹配逻辑由 `matches` 控制，行为由配置字段决定 | 中等——判断是否挂载（`supports`）和具体如何挂载（`doBindTo`）独立 |
| 为什么选这个粒度 | 责任链里每个环节自己选拦截哪个操作 | 收敛口径唯一，避免子类绕过模板直接覆写语句执行 | 过滤逻辑简单（两段式:matches 判断 + onMatch/onMismatch 响应），一个抽象方法够用 | 环境适配（supports）和业务绑定（doBindTo）是两个独立关注点，分离更清晰 |

**共性公式**：`final` 固定流程 → 检查前置条件 → 调用子类扩展点 → catch 兜底隔离——公式结构稳定，但每次复用的三个参数（入口数量×抽象方法数量×异常策略）都是根据具体 API 契约和关注点调整的，不是复制粘贴。

### 1.2 09 独有的「环境适配前置检查」设计

与 13/14 不同，`AbstractMeterBinder` 多了一个 `supports(MeterRegistry registry)` 抽象检查，子类在 `doBindTo` 之前就可以根据运行环境判断是否应该挂载指标。三个系统指标的实现是一个很好的例子：

| 类 | `supports()` 判断 | 为什么需要这个检查 |
|---|---|---|
| `SystemMemoryMetrics` | `com.sun.management.OperatingSystemMXBean` 是否存在 | 这个类是 HotSpot 私有扩展，在其他 JVM（如 IBM J9、Azul Zing）上不存在 |
| `CGroupMemoryMetrics` | `/sys/fs/cgroup/memory/` 目录是否存在 | cgroup v1 路径；v2 环境下目录结构不同，不检查就会读它不存在的数据，产生无用 Warn |
| `NetworkStatisticsMetrics` | `/proc/net/dev`（或自定义路径）是否存在 | `/proc/net/dev` 是 Linux 特有的，Windows 用不同的 API 暴露网络统计 |

`supports` 的意义在于——它的结果在容器启动阶段就已经确定了（不需要等 JVM 初始化完毕），一旦返回 `false`，`AbstractMeterBinder.bindTo` 直接短路返回，**连 `doBindTo` 方法的 try/catch 都不会进入**——避免了在每个子类里用 `try { doBindTo } catch (Exception)` 重新检查环境的前置条件，也避免了读不存在的文件、调不存在的 JVM 扩展 API 造成无关错误日志。

---

## 二、三种系统指标：物理机、容器、网卡

### 2.1 `SystemMemoryMetrics`：HotSpot JVM 物理机视角

```java
// SystemMemoryMetrics.java:67-104
@Override
protected void doBindTo(MeterRegistry registry) throws Throwable {
    OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
    Gauge.builder("memory.swap.space.total", osBean, OperatingSystemMXBean::getTotalSwapSpaceSize)
         .tags(tags).baseUnit(BaseUnits.BYTES).strongReference(true).register(registry);
    // getFreeSwapSpaceSize / getCommittedVirtualMemorySize / getTotalPhysicalMemorySize / getFreePhysicalMemorySize
    // ...共 5 个 Gauge
}
```

`OperatingSystemMXBean` 是 `com.sun.management` 包下的类——只在 HotSpot JVM 上存在（Azul Zing 虽兼容但用的是自己的实现）。`supports()` 用反射检测 `ClassLoader.loadClass("com.sun.management.OperatingSystemMXBean")`（第 42-51 行），如果类不存在则 `supports()` 返回 `false`——子类的 `doBindTo` 根本不会被调用。这里用的是**类级静态一次性判断**（第 40 行 `private static final boolean SUPPORTED = isSupported()`），不是每次 `bindTo` 都重新检查——这在 09-01 讨论的 `AbstractMeterBinder` 设计中是合理的（环境条件不会在运行时变化），但也意味着重新部署后需要重启才能重新检测。

### 2.2 `CGroupMemoryMetrics`：补 JVM 不感知容器限制的盲区

这是整个 micrometer 子系统里最有生产价值的部分（容器化场景下，`Runtime.maxMemory()` 在旧 JDK 里拿的是宿主机物理内存上限，不是 cgroup 限制给容器的配额）。

```java
// CGroupMemoryMetrics.java:35-126
private static final Path ROOT_DIRECTORY_PATH = 
    Paths.get(System.getProperty("cgroup.memory.dir", "/sys/fs/cgroup/memory/"));

@Override
protected boolean supports(MeterRegistry registry) {
    return Files.exists(ROOT_DIRECTORY_PATH);
}
```

两个关键观察：

**① 只支持 cgroup v1，v2 环境会过滤但信息不够明确**。`supports()` 检查的是 `/sys/fs/cgroup/memory/`（v1 路径），v2 的统一层级路径是 `/sys/fs/cgroup/memory.current` 这种格式——**顶层 `supports` 可以正确拒绝，但日志只说目录不存在，没明确告诉用户"你用的是 cgroup v2，当前版本不支持"**。外层条件注解 `@ConditionalOnResource(resources = "file:///sys/fs/cgroup/")`（v1/v2 都有这个目录本身）和内部 `supports()` 的判定粒度错位，进一步造成"条件注解认定 cgroup 子系统存在→进入装配→supports 返回 false→什么指标也没有"的路径缺陷——这是设计层面的信号错位，不是代码 bug（v1 环境下完全正确）。

**② 读文件没有异常防护链**。`readFileAsLong`（第 128-138 行）对 `Files.exists`/`Files.isReadable` 做了判空返回 `-1L`，但 `buildMemoryStatsGauge`（第 96-108 行）调用 `readAllLines(MEMORY_STAT_FILE_PATH)` 后直接 `split(" ")`——如果 `/sys/fs/cgroup/memory/memory.stat` 的格式不符合预期（比如内核版本不同导致文件格式变化），`NumberFormatException` 会从 `buildBytesGauge` 里 `Long.parseLong(value)` 一路冒上去。注意这里的调用栈不在 `AbstractMeterBinder.bindTo` 的 try/catch 范围内——`bindTo` 只保护 `doBindTo` 本身，而后续的 Gauge 取值（Micrometer 框架周期性触发 `Supplier.get()`）发生在 Micrometer 的采集线程里，不受 `bindTo` 的异常保护。

### 2.3 `NetworkStatisticsMetrics`：带定时刷新的网络接口指标

这是三个系统指标中唯一带**定时任务**的，因为网络接口的流量统计需要周期性刷新（不像内存/Swap 是一次性绑定静态 Gauge）。

```java
// NetworkStatisticsMetrics.java:77-182
@Override
protected void doBindTo(MeterRegistry registry) throws Throwable {
    this.registry = registry;
    bindStats();                     // 立即采集一次当前值
    bindStatsOnSchedule();           // 启动定时任务持续刷新
}
private void bindStatsOnSchedule() {
    scheduledExecutorService.scheduleAtFixedRate(this::bindStats, 0, interval, TimeUnit.MILLISECONDS);
}
```

定时任务里调用的 `bindStats()`（第 86-118 行）从 `/proc/net/dev` 读取行式统计数据，用手写字符扫描解析网卡接口名和收发字节/包/错误/丢弃计数，调用 `statsMap.computeIfAbsent(stats.name, name -> { bindStats(stats); return stats; })`——**新发现的网卡接口仅在第一次出现时注册 Gauge**，后续只更新 `Stats` 对象内的计数器值（通过 `Stats.update(stats)` 的 `synchronized` 保护）。

**一个生产隐患**：`parseStats` 方法（第 190-238 行）的手写字符串解析逻辑**完全没有异常防护**——`Long.parseLong(values.get(index++))` 的 `NumberFormatException` 或 `IndexOutOfBoundsException` 会从 `parseStats` 一路冒泡到 `bindStats`（定时任务入口），而 `bindStats` 方法内部**没有任何 try/catch**。根据 JDK `ScheduledThreadPoolExecutor` 的官方语义，如果 `scheduleAtFixedRate` 调度的任务抛出未捕获异常，**这个周期任务会永久静默终止**（后续周期不再执行）——网络指标采集会从此完全停止，且无任何日志或告警。

**场景量化**：这个风险的触发概率很低——`/proc/net/dev` 的格式在 Linux 内核中极度稳定（固定列结构 25 年以上未变更），`Long.parseLong` 失败只在极其边缘的异常场景才可能发生（比如内核 bug 导致统计值输出为非数字、自定义驱动导出的虚拟网卡接口使用了非标准格式，或 `/proc/net/dev` 被意外损坏）。普通的生产环境中，更大的风险来源不是"格式变化"而是**文件读取过程中的时序问题**——比如定时任务在 `readAllLines` 正常返回后、但 `parseStats` 解析期间，文件刚巧被截断或替换为不完整内容（docker 容器重启、网络命名空间切换等极端运维操作），这时 `split(" ")` 返回的 `String[]` 长度不符合预期，`values.get(index++)` 越界触发 `IndexOutOfBoundsException`。触发场景仍然罕见，但一旦触发，后果是**整个网络指标采集永久静默停止**——这种"低概率 + 高影响"的组合才使得它值得在生产评估中列出。

---

## 三、MBean 通用绑定骨架：死代码的完整设计

这三个类是理解"作者搭了通用抽象但未落地具体实现"的典型例子：

```
MBeanAttributeMeterBinder（接口，1 个方法）
  ↑
MBeanMetrics extends AbstractMeterBinder（驱动类，81 行）
  ├── queryNames(objectNameToQuery, queryExp)   → 从 JMX MBeanServer 找到所有匹配 MBean
  ├── getMBeanAttributes(mBeanServer, objectName)→ 获取每个 MBean 的所有属性
  └── for each attribute → 遍历 attributeMeterBinders[] 逐一调用 bindTo(...)
```

设计本身是合理的：把"JMX MBean 属性→Micrometer Meter"的映射做成策略接口，驱动类只管遍历和分发。但**全项目搜索不到任何 `MBeanAttributeMeterBinder` 的实现类**——这套骨架完全是孤立的（零实现、零引用、零测试覆盖）。附加一个问题：`MBeanMetrics.doBindTo`（第 97-105 行）在每个 MBean 属性上**只读取一次 `attributeValue`**（第 100 行 `Object attributeValue = mBeanAttribute.getValue()`），把这个**一次性快照值**传给所有 binder——如果某个 binder 把这个快照值直接注册成 Gauge 的固定值（而不是传一个 `Supplier` 让 Micrometer 每次采集时重新查），那对应的指标值会永远冻结在首次绑定的那个时刻。不过因为没有实际实现，这个隐患无法验证——只能标注为"设计上潜在的误导风险，需要实现者注意"。

---

## 四、两套可运行的 Sentinel 指标实现与一层扩展接口

### 4.1 全景：两套可运行实现 + 一层抽象基类

```
                    AbstractSentinelMetrics<M>
                    ↑ extends (无具体子类实现)
                    
                    SentinelMetrics                SentinelCollector
                    ↑ implements                  ↑ extends
                    MeterBinder + Runnable         io.prometheus.client.Collector
                    + ProcessorSlotEntryCallback   + ProcessorSlotEntryCallback
                    
                    内存态，通过 ClusterNode      文件态，通过 MetricSearcher
                    实时读取 Sentinel 内部统计     读 Sentinel 落盘的 .log 指标文件
                    → 通用 Micrometer 后端          → Prometheus 专用（有原生、
                                                        → implicit model 之类的优势）
                    （Prometheus/Influx/...）

装配顺序（MicrometerAutoConfiguration）：
  @ConditionalOnBean(PrometheusMeterRegistry) → 优先 SentinelCollector
  @ConditionalOnMissingBean(SentinelCollector.class) → 退化 SentinelMetrics
```

### 4.2 为什么有两套而非一套

`AbstractSentinelMetrics` 是抽象基类，设计上留给子类实现 `getMetricsNamePrefix` 和 `addTags` 来定义指标名和标签——但目前全项目找不到任何子类，**这套抽象还没有落地的具体用例，属于预留扩展接口而非可运行实现**。

两套真正可运行的实现分工如下：

`SentinelMetrics` 是通用方案：它直接从内存里 `ClusterNode` 实时读取 Sentinel 内部统计（`totalRequest`/`successQps`/`blockQps` 等），用 Micrometer 通用 `MeterRegistry` 暴露——任何 Micrometer 支持的后端（Prometheus/Influx/Graphite/...）都能接收。

`SentinelCollector` 是 Prometheus 专用路径：它继承 `io.prometheus.client.Collector`（Prometheus Java 客户端原生抽象），从 Sentinel 落盘的 `.log` 指标文件（`MetricWriter.METRIC_BASE_DIR`）通过 `MetricSearcher.findByTimeAndResource` 读取历史窗口数据——利用了 Prometheus 原生的 Label 机制和 `CollectorRegistry` 管理，对于已有 Prometheus 基础设施的团队，这是最高效的路径。

两者通过 `@ConditionalOnBean` + `@ConditionalOnMissingBean` 实现互斥装配：有 Prometheus 就走专用路径，没有就退化走通用方案。

### 4.3 两个实现共有的两个隐患

**① check-then-act 竞态条件**。`SentinelMetrics.addMetrics(contextName, resourceName, node)`（第 166-176 行）和 `AbstractSentinelMetrics.addMetrics`（第 110-121 行）都是先 `get()` 再 `put()` 的去重模式：

```java
ClusterNode processedClusterNode = processedResourceClusterNodes.get(resourceName);
if (!Objects.equals(processedClusterNode, clusterNode)) {
    addMetrics(contextName, resourceName, clusterNode, registry);
    processedResourceClusterNodes.put(resourceName, clusterNode);
}
```

这不是原子操作。在多线程并发（`SentinelMetrics` 的异步回调 + 每分钟定时扫描 + Sentinel 自己的 Slot Chain 线程）下，多个线程可能同时读到 `processedClusterNode == null`，都判断"未处理"，都去注册 Gauge。这不是一个会导致功能故障的 bug（Micrometer 对重复注册同名 Meter 有幂等保护），但会产生**不必要的重复注册尝试**和 WARN 日志。

**② 借用 Sentinel 官方内部私有线程池**。`SentinelMetrics.addMetricsAsync` 把每次 `onPass`/`onBlocked` 回调通过 `scheduler.execute(task)` 异步提交——但这个 `scheduler` 来自 `getSentinelMetricsTaskExecutor()`，其实现是用反射读取 Sentinel 官方的私有静态字段 `FlowRuleManager.SCHEDULER`（`SentinelUtils.java:151-153`）：

```java
public static ScheduledExecutorService findSentinelMetricsTaskExecutor() {
    return getStaticFieldValue(FlowRuleManager.class, "SCHEDULER");
}
```

`FlowRuleManager.SCHEDULER` 是 Sentinel 官方内部用来周期性清理/刷新流控统计窗口的**单线程**调度器——microsphere 把每次请求的 Metrics 任务提交塞进这个单线程池，从理论上会和 Sentinel 自己的统计刷新任务共享执行时间。而且**私有静态字段的命名/类型是 Sentinel 内部未公开的实现细节**，Sentinel 升级后改名或删掉这个字段会导致 `getSentinelMetricsTaskExecutor()` 反射失败、退化到创建一个新的单线程池（`SentinelUtils.java:184-188`）——行为会悄悄改变但没有编译期或运行期错误提示。这是同一作者"反射读私有字段"惯用模式的第四次出现（前三次：13-mybatis `Executors.getDelegate`、13-mybatis `SqlSessionFactoryBean.plugins`、09-observability `getKafkaProducer`）。

---

## 五、P6Spy JDBC 拦截指标

### 5.1 `MicrometerJdbcEventListener`：实现与两个问题

```java
// MicrometerJdbcEventListener.java:41-148
public class MicrometerJdbcEventListener extends LoggingEventListener {
    private static MeterRegistry registry;       // ← static 字段

    public MicrometerJdbcEventListener(MeterRegistry registry) {
        this.registry = registry;                // ← 实例构造器写 static 字段
    }
}
```

**问题一：static 字段污染**。P6Spy 的 `JdbcEventListener` 典型生命周期是每个数据库连接一个实例——如果有多个 `MeterRegistry`（比如多数据源各分配不同的 registry），后创建的实例会**静默覆盖**先创建的实例的 registry，导致所有 Listener 实例都共享同一个（最后设置的）registry，多数据源场景下指标归属混乱。

**问题二：高基数 Tag**。`SLOW_SQL_TIME_METRIC_NAME` 对应的 Gauge 把**完整 SQL 字符串**作为 tag 值（第 142-146 行 `tag("sql", sql)`）——这是 Micrometer 官方明确警告的**高基数 Tag 反模式**。Tag 的取值空间应该是有限的枚举集合（SQL 类型 `SELECT/INSERT/UPDATE/DELETE` 只有几种），但把完整 SQL 当 tag 意味着每条不同的 SQL 语句（即使只是参数值不同）都会产生一个新的时间序列。在 Prometheus 后端，这会导致**指标基数爆炸**（数千甚至数万条不同 SQL = 数千甚至数万个独立 time series）——轻则拖慢 Prometheus 查询性能，重则因基数超限被 Prometheus 服务器拒绝抓取。

`MicrometerJdbcEventListenerTest.testAddMetrics`（第 59-63 行）调用 `addMetrics` 但**没有任何断言**，传入的 `timeElapsedNanos=1000000000` 恰好等于默认慢 SQL 阈值（`DEFAULT_SLOW_SQL_TIME_THRESHOLD = TimeUnit.SECONDS.toNanos(1)`），而 `addMetrics` 使用的是 `>` 严格大于判断，所以等于阈值不会触发慢 SQL Gauge 注册——**高基数 tag 那行代码从未被测试覆盖到**。

---

## 六、自动配置装配全景

`MicrometerAutoConfiguration`（`MicrometerAutoConfiguration.java:91-294`）用 6 个内部 `@Configuration` 类管理所有指标组件的装配：

| 内部配置类 | 条件 | 注册的组件 |
|-----------|------|----------|
| `SystemConfiguration` | `microsphere.micrometer.system.enabled` (默认 true) | `NetworkStatisticsMetrics` + `SystemMemoryMetrics` |
| `CGGroupConfiguration` | `microsphere.micrometer.cgroup.enabled` (默认 true) + `@ConditionalOnCGroup` | `CGroupMemoryMetrics` |
| `JvmConfiguration` | `microsphere.micrometer.jvm.enabled` (默认 true) | Micrometer 官方 `JvmCompilationMetrics`/`JvmInfoMetrics` + 手动扫描 `ExecutorService` Bean 绑定线程池指标 |
| `SentinelMetricsConfiguration` | `microsphere.micrometer.sentinel.enabled` (默认 true) + `@ConditionalOnSentinelEnabled` | `SentinelCollector`（优先）/ `SentinelMetrics`（退化） |
| `KafkaMetricsConfiguration` | `microsphere.micrometer.kafka.enabled` (默认 true) + `@ConditionalOnClass(KafkaClient)` | `KafkaClientMetrics`（通过反射拿 KafkaAppender 内部的 `Producer`） |
| `PrometheusMetricsConfiguration` | `microsphere.micrometer.prometheus.enabled` (默认 true) + `@ConditionalOnEnabledPrometheusMetricsExport` | `PrometheusPushGatewayManager` |

每个组件都有一个 `microsphere.micrometer.<子系统>.enabled` 的属性开关（与前几个项目一致的 `matchIfMissing=true` 默认启用模式），其中 `PrometheusMetricsConfiguration` 依赖的 `@ConditionalOnEnabledPrometheusMetricsExport` 由于 09-03 将要展开的 bug（`@ConditionalOnBean(name=全限定类名)`）而完全失效——这里先不展开。

---

## 七、问题清单（已证）

| # | 问题 | 证据 |
|---|------|------|
| P1 | `NetworkStatisticsMetrics.parseStats` 无异常防护 → `ScheduledThreadPoolExecutor` 静默终止周期任务，网络指标采集永久停止 | `NetworkStatisticsMetrics.java:190-238`（`Long.parseLong(values.get(index++))` 无 try/catch）+ `ScheduledThreadPoolExecutor` 官方语义 |
| P2 | `SentinelMetrics`/`AbstractSentinelMetrics`：`get→put` 非原子 check-then-act 竞态 | `SentinelMetrics.java:171-174` |
| P3 | `SentinelMetrics`：反射借用 Sentinel 官方私有字段 `FlowRuleManager.SCHEDULER`——升级改字段名即静默失效 | `SentinelUtils.java:151-153` |
| P4 | `MicrometerJdbcEventListener`：static `registry` 字段被实例构造函数覆盖 | `MicrometerJdbcEventListener.java:58-62` |
| P5 | `MicrometerJdbcEventListener`：完整 SQL 作 tag → Micrometer 高基数反模式，生产环境指标基数爆炸 | `MicrometerJdbcEventListener.java:142-146` |
| P6 | `CGroupMemoryMetrics` + `ConditionalOnCGroup`：`supports()` 与条件注解判定粒度不一致（v1/v2），v2 环境下静默无指标无明确提示 | `CGroupMemoryMetrics.java:48-54` + `ConditionalOnCGroup.java:22` |
| P7 | `MBeanMetrics`：完整设计但零实现、零引用、零测试（死代码骨架） | 全文 grep 无 `implements MBeanAttributeMeterBinder` |
| P8 | `MicrometerJdbcEventListenerTest.testAddMetrics`：无断言 + 传入值恰好等于慢 SQL 阈值，高基数 tag 代码行从未被测试覆盖 | `MicrometerJdbcEventListenerTest.java:59-63` |
| P9 | `MicrometerUtils` 暴露全局共享 `ScheduledExecutorService` 但 `NetworkStatisticsMetrics` 自己又新建一个（没有复用）——至少 3 种不同的线程池管理策略并存，缺乏统一约定 | `MicrometerUtils.java:39` vs `NetworkStatisticsMetrics.java:80-81` |

---

## 八、测试佐证

- `CGroupMemoryMetricsTest`：用 `System.setProperty("cgroup.memory.dir", testDir)` 重定向到测试固件（`META-INF/test-data/` 下模拟 v1 格式文件），断言 16 个具体 metric 名存在——**测试覆盖充分，是项目里少数有实质性断言的测试**
- `SystemMemoryMetricsTest`：只验证 `assertFalse(registry.getMeters().isEmpty())`——环境依赖 `com.sun.management.OperatingSystemMXBean`，在非 HotSpot JVM 上可能失败
- `NetworkStatisticsMetricsTest`：用 `System.setProperty` 重定向到测试文件，**但 WatchService 监听线程从未 `.start()`**（`thread.join(5000)` 对未启动线程立即返回）——整段"动态发现新接口"验证代码是摆设
- `AbstractSentinelMetrics`/`SentinelMetrics`/`SentinelCollector`/`MBeanMetrics`：**完全没有测试**

---

## 九、小结（引用要点）

- **模板方法模式的第四次复现**：`AbstractMeterBinder.bindTo`（final 收敛 + supports 短路 + doBindTo 异常隔离）是同一作者在 13-mybatis/14-druid/09-observability 中的第四次使用——收敛模板稳定，具体形态因 API 契约不同而各异（入口方法名、异常策略、子类扩展点数量）
- **`supports()` 短路检查是 09 独有的设计**：三个系统指标子类用它在 `doBindTo` 之前就根据运行环境（JVM 类型、cgroup 版本、操作系统）判断是否继续，避免了在每个子类里重复检查环境的 bad smell
- **Sentinel 有两套可运行指标实现，构成条件装配的分层退路**：`SentinelCollector`（有 Prometheus 时的专用路径）→ `SentinelMetrics`（退而求其次的通用方案）——此外 `AbstractSentinelMetrics` 是零实例的抽象基类，留空作扩展接口预留，不能混称"三套运行中实现"
- **P1（`ScheduledThreadPoolExecutor` 静默终止）和 P5（SQL 高基数 Tag）是两个在生产环境中最可能造成实际危害的隐患**——前者导致网络指标完全停止、后者导致 Prometheus 基数爆炸
