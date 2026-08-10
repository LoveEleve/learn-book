# stage-1 · 第 11 节：基于监控指标的负载均衡实现 — 知识点提取

> 课程：stage-1 服务治理 第 11 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/11. 第十一节：基于监控指标的负载均衡实现.md`
> 提取时间：2026-08-09 | 权重：核心（负载均衡是服务治理核心）

---

## 一、本节概览

- **技术域**：监控指标体系 + 基于监控指标的负载均衡（JMX 指标 + LoadBalancer）
- **维度**：`[性能优化]`（监控指标）+ `[分布式问题]`（负载均衡）+ `[分布式问题]`（注册中心）
- **核心命题**：如何获取监控指标（CPU/Load/线程/RT/QPS），并用它们做智能负载均衡
- **知识点数**：8 个
- **前置**：JMX、负载均衡概念、Spring Cloud LoadBalancer、注册中心

## 前置条件清单
读者需先掌握：
1. **JMX**（Java Management Extension，MXBean）
2. **负载均衡概念**（轮询/权重/一致性哈希）
3. **Spring Cloud LoadBalancer**（ReactorServiceInstanceLoadBalancer）
4. **注册中心**（Nacos/Eureka 服务实例元信息）
未达前置者，先补：JMX 入门 + Spring Cloud LoadBalancer + 注册中心概念

## 掌握度
目标读者：**本人（读源码多，Spring 熟悉）** — 已确认
讲解策略：JMX 指标 + LoadBalancer 直接讲（你熟悉）；补监控指标体系全貌

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 核心监控指标体系
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **需求**：掌握判断服务健康/性能的核心指标
- **自主实现**：监控 CPU 使用率、系统负载(Load)、线程状态、RT(响应时间)、QPS/TPS
- **参考实现**：docs 列出核心指标——CPU 使用率、系统负载、线程状态、响应时间(RT)、QPS、TPS
- **对比取舍**：这些指标是服务治理/负载均衡/容错的基础数据来源
- **关联 microsphere**：`[待验证]`

### KP-02 JMX 标准接口（MXBean）
- **维度**：`[规范]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：JMX
- **需求**：通过 JMX 获取 JVM 监控指标
- **自主实现**：用 `ManagementFactory` 获取各 MXBean 读取指标
- **参考实现**：
  - 工厂：`java.lang.management.ManagementFactory`
  - 标准接口：OperatingSystemMXBean / MemoryPoolMXBean / MemoryManagerMXBean / GarbageCollectorMXBean / ClassLoadingMXBean / MemoryMXBean / ThreadMXBean / RuntimeMXBean / CompilationMXBean
  - HotSpot 扩展：com.sun.management.OperatingSystemMXBean / ThreadMXBean / GarbageCollectorMXBean（含 getProcessCpuLoad）
- **对比取舍**：标准接口跨 JVM 实现一致，com.sun 扩展含更细指标（进程 CPU）
- **测试佐证**：JDK 源码 java.lang.management 包

### KP-03 JMX 获取 CPU 利用率（getProcessCpuLoad）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **需求**：获取 JVM 进程 CPU 利用率，用于负载均衡/监控
- **自主实现**：调 `OperatingSystemMXBean#getProcessCpuLoad()`（返回 [0.0,1.0]）
- **参考实现**：`com.sun.management.OperatingSystemMXBean#getProcessCpuLoad` 返回 0.0~1.0（0=无 CPU，1=满 CPU），不可用返回负值
- **对比取舍**：进程级 CPU 是负载均衡的关键输入
- **待验证**：用 JDK17 源码验证 getProcessCpuLoad 实现

### KP-04 监控体系全貌（来源/收集/存储）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：监控概念
- **需求**：理解监控体系（来源→收集→存储）
- **自主实现**：设计指标来源、采集、存储分层
- **参考实现**：
  - 来源：JMX(JVM Memory/CPU/Load/Threading) + JNI + FileSystem(CPU/网络/物理内存/CGroup)
  - 收集：Jolokia(JMX HTTP 桥)/Netflix Servo/Micrometer/Spring Metrics/Prometheus/Pinpoint/Skywalking/Zipkin
  - 存储：ES/Prometheus Server/OpenTSDB/InfluxDB/Redis/HBase/MySQL
- **对比取舍**：采集(JVM 内)与存储(外部)分离；Micrometer 是 Java 指标门面
- **关联 microsphere**：`[待验证]` microsphere-observability

### KP-05 基于监控指标的负载均衡（核心）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：LoadBalancer、KP-03
- **需求**：不用轮询，而根据服务实例的实时指标（CPU/负载）选实例
- **自主实现**：实现 `ReactorServiceInstanceLoadBalancer.choose()`，读实例上报的指标(如 cpu-usage)，选负载最低的
- **参考实现**（已源码验证，但**实现不完整**）：biz-client `CpuUsageLoadBalancer implements ReactorServiceInstanceLoadBalancer`——choose() 里 `ServiceInstanceListSupplier.get()` 获取实例，读到每个实例 metadata 的 `cpu-usage` 指标，但 **`// TODO 完成 CPU 利用率的算法实现` 后直接 `return serviceInstances.get(0)`（无脑返回第一个）——算法未实现**
- **对比取舍**：基于指标的负载均衡比轮询更智能，但需实例上报指标 + **真实算法**；biz-client 只做了"读指标"这步，选实例算法是 TODO
- **权重/置信度说明**：**参考实现不完整（TODO），故置信度降为 Medium**——这是 docs 想讲的需求方向，但参考实现未完成；知识本体(基于指标选实例)仍成立，但参考实现不能当作已验证的完整实现
- **测试佐证**：biz-client `CpuUsageLoadBalancer.java`（choose() 读 metadata.cpu-usage，但 TODO + 返回 get(0)，算法未实现）

### KP-06 Spring Cloud LoadBalancer 扩展点（ServiceInstanceListSupplier）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Spring Cloud LoadBalancer
- **需求**：理解 LoadBalancer 的扩展点，自定义负载策略
- **自主实现**：实现 `ReactorServiceInstanceLoadBalancer` + `ServiceInstanceListSupplier` 定制实例列表
- **参考实现**：`ReactorServiceInstanceLoadBalancer.choose()` + `ServiceInstanceListSupplier.get()`；biz-client 有 `UserServiceServiceInstanceListSupplier`
- **对比取舍**：LoadBalancer 通过 ServiceInstanceListSupplier(选实例来源) + ReactorServiceInstanceLoadBalancer(选择策略) 双扩展点
- **测试佐证**：`code/spring/spring-cloud-loadbalancer` + biz-client 实现

### KP-07 注册中心（服务实例元信息）
- **维度**：`[分布式问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[过时→Nacos]` | **置信度**：High
- **前置**：注册中心概念
- **需求**：服务注册发现，实例元信息（含指标）供负载均衡用
- **自主实现**：实例注册到注册中心，携带 metadata（含 cpu-usage 等指标）
- **参考实现**：docs 用 Netflix Eureka——Eureka Server REST URI /eureka/apps/，实例含 metadata/status/leaseInfo 等；Eureka Client 全量订阅 + 轮询(registryFetchIntervalSeconds 默认30s)
- **对比取舍**：**Eureka 已过时(Netflix OSS)**，国内主流是 **Nacos**（有源码）——实例 metadata 承载 cpu-usage 等指标供 CpuUsageLoadBalancer 用
- **测试佐证**：`code/spring/nacos` 源码；docs 的 Eureka 实例 XML 含 metadata/cpu-usage 思路
- **关联 microsphere**：microsphere-nacos

### KP-08 注册中心对比（Eureka/Nacos）
- **维度**：`[分布式问题]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：Medium
- **前置**：KP-07
- **需求**：选注册中心
- **自主实现**：对比 Eureka(AP)/Nacos(CP+AP)/Consul
- **参考实现**：Eureka=AP、Nacos=双模式、Consul=AP/CP（第2节分布式理论关联）；国内主流 Nacos
- **对比取舍**：Eureka 已过时，Nacos 是国内主流（注册+配置一体）
- **待验证**：Nacos 一致性模式（CP/AP 切换）

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| 核心监控指标 | 性能 | 核心 | P1 | 🔴 | High |
| JMX 标准接口 | 规范 | 核心 | P1 | 🟡 | High |
| JMX CPU 利用率 | 性能 | 核心 | P1 | 🟡 | High |
| 监控体系全貌 | 性能 | 支撑 | P2 | 🟡 | High |
| 基于指标的负载均衡 | 分布式 | 核心 | P1 | 🔴 | Medium（参考实现TODO未完成） |
| LoadBalancer 扩展点 | 工程 | 核心 | P1 | 🟡 | High |
| 注册中心 | 分布式 | 核心 | P1 | 🟡 | High（过时→Nacos） |
| 注册中心对比 | 分布式 | 支撑 | P2 | 🟡 | Medium |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **基于指标负载均衡**：biz-client `CpuUsageLoadBalancer`——choose() 读到 metadata.cpu-usage 指标，但**算法是 TODO 未实现**（只读指标 + 返回第一个）
- **LoadBalancer 扩展**：biz-client `UserServiceServiceInstanceListSupplier` + `code/spring/spring-cloud-loadbalancer`
- **注册中心**：`code/spring/nacos`（主流，替换 Eureka）
- `[待验证]` microsphere 是否有 LoadBalancer/监控封装

---

## 五、本节小结（三层次视角）

**需求**：获取监控指标（CPU/Load/线程/RT/QPS），用它们做智能负载均衡。

**自主实现核心**：若我设计——
1. 用 JMX(getProcessCpuLoad) 获取进程 CPU 指标
2. 服务实例上报指标到注册中心 metadata（如 cpu-usage）
3. 实现 ReactorServiceInstanceLoadBalancer.choose()，读实例指标选负载最低者
4. LoadBalancer 双扩展点：ServiceInstanceListSupplier(实例来源) + LoadBalancer(选择策略)

**参考实现**：biz-client `CpuUsageLoadBalancer`——**注意它只"读指标"未"实现算法"（choose() 里 TODO + return get(0)），参考实现不完整**。完整实现需自定义选实例算法；spring-cloud-loadbalancer 提供扩展点；Nacos（主流注册中心，替换过时 Eureka）。

**对比取舍**：知识本体是"监控指标 + 基于指标的负载均衡"。核心洞察：**实例 metadata 上报指标(cpu-usage) → 负载均衡读指标选实例**。但 biz-client 的 CpuUsageLoadBalancer **只读到指标、算法是 TODO 未实现**——不能当作完整实现，置信度 Medium。这提醒：代码"看起来在做"≠"真的实现"。

**待验证汇总**：
- getProcessCpuLoad 实现（JDK17）
- Nacos 一致性模式
- microsphere 是否有 LoadBalancer 封装
- CpuUsageLoadBalancer 的完整选实例算法（TODO 待实现）
