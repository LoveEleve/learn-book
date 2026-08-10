# stage-1 · 第 13 节：Micrometer 基础 — 知识点提取

> 课程：stage-1 服务治理 第 13 节
> 来源 docs：`/data/workspace/java-training-camp/stage-1/docs/13. 第十三节：Micrometer 基础.md`
> 提取时间：2026-08-09 | 权重：核心（可观测性是服务治理核心）

---

## 一、本节概览

- **技术域**：Micrometer 指标（核心概念/核心 API/内建 Binder）
- **维度**：`[性能优化]`（监控指标）+ `[规范]`（指标模型）
- **核心命题**：理解指标基本类型(Timer/Counter/Gauge/DistributionSummary)、Tags、Micrometer 核心 API 与内建 Binder
- **知识点数**：7 个
- **前置**：监控指标概念、Micrometer 基本使用

## 前置条件清单
读者需先掌握：
1. **监控指标概念**（时序指标/标签）
2. **Micrometer 基本使用**（@Timed/Counter 等）
3. **Java 并发**（AtomicInteger 等，Counter 计数用）
未达前置者，先补：Micrometer 官方概念文档 + 监控指标入门

## 掌握度
目标读者：**本人（读源码多，Spring/监控熟悉）** — 已确认
讲解策略：Micrometer API + Binder 直接讲（你熟悉）；补指标模型(Timer/Counter/Gauge)全貌

---

## 二、知识点提取（三层次：需求 / 自主实现 / 参考实现）

### KP-01 指标核心概念（系统/业务指标 + Tags）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：无
- **需求**：理解要监控什么（系统 + 业务），以及如何区分维度
- **自主实现**：分系统指标(CPU/内存/RT)和业务指标(成交量/时间聚合)，用 Tags 区分维度
- **参考实现**：
  - 系统指标：CPU、内存、Response Time
  - 业务指标：自定义(成交量：数量/总量，时间单位聚合：秒/分/时)
  - **Tags 标签**：如应用(User Service)、实例(IP)——用标签区分同一指标的维度
- **对比取舍**：指标 + 标签 = 多维监控；标签是区分维度的关键
- **关联 microsphere**：已验证——microsphere-micrometer 有 binder（jmx/sentinel/system/jdbc）实现 MeterBinder

### KP-02 指标类型（Timer/Counter/Gauge/DistributionSummary）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-01
- **需求**：不同数据用不同类型指标表达
- **自主实现**：按数据特性选类型——计数(Counter)/耗时(Timer)/瞬时值(Gauge)/分布(DistributionSummary)
- **参考实现**：Micrometer 指标类型——Counter(计数)/Gauge(瞬时值)/Timer(耗时分布)/DistributionSummary(值分布)
- **对比取舍**：Timer 与 DistributionSummary 都含分布统计；Counter 单调递增
- **关联 microsphere**：已验证——microsphere-micrometer 实现 Micrometer 指标（见 KP-07）

### KP-03 指标聚合（平均值/最大/最小）
- **维度**：`[性能优化]` | **权重**：`[支撑]` | **深度**：🟡 | **优先级**：P2 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-02
- **需求**：把多个样本聚合成可读统计
- **自主实现**：提供平均值/最大值/最小值聚合
- **参考实现**：指标聚合——平均值、最大值、最小值（Timer 有 count/total/max 等）
- **对比取舍**：聚合是监控展示的基础（avg/max/min）
- **关联 microsphere**：无直接关联

### KP-04 Micrometer 核心 API（Meter/Counter Builder）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🔴 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Java、Micrometer
- **需求**：用 API 创建/使用指标
- **自主实现**：用 Builder + tags 创建指标，调用 increment/record 记录
- **参考实现**（已源码验证）：`Meter` 根接口；`Counter.builder(name).tags(...).register()` + `increment()`；Gauge/Timer/DistributionSummary 类似 Builder 模式
- **对比取舍**：Builder 模式 + tags 是 Micrometer API 核心
- **测试佐证**：`code/spring/micrometer/micrometer-core` 的 Counter.java（builder/tags/increment）

### KP-05 Counter 计数机制（字段/Atomic 计数）
- **维度**：`[性能优化]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Java 并发
- **需求**：统计事件发生次数
- **自主实现**：用 int/long 字段或 AtomicInteger/AtomicLong 计数
- **参考实现**：Counter 常见计数——对象 int/long 字段、AtomicInteger/AtomicLong
- **对比取舍**：Atomic 保证并发安全计数；Counter 单调递增
- **关联 microsphere**：已验证——microsphere-micrometer 用 Counter/指标采集（见 KP-07 binder）

### KP-06 Micrometer 内建 Binder
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：Micrometer
- **需求**：自动采集 JVM/Kafka/系统/Tomcat 等指标，无需手写
- **自主实现**：用内建 Binder 自动绑定常见组件指标
- **参考实现**：Micrometer 内建 Binder——JVM、Kafka、Logging、系统、Tomcat 等；Spring Boot Actuator 支持的指标列表
- **对比取舍**：内建 Binder = 开箱即用的自动指标采集
- **测试佐证**：`code/spring/micrometer` 有 jvm/system 等 Binder

### KP-07 MeterBinder / MeterRegistry（注册机制）
- **维度**：`[工程问题]` | **权重**：`[核心]` | **深度**：🟡 | **优先级**：P1 | **过时**：`[时间无关模式]` | **置信度**：High
- **前置**：KP-06
- **需求**：把指标注册到 MeterRegistry，Binder 绑定指标
- **自主实现**：MeterBinder 实现类绑定指标 → 注册到 MeterRegistry（bindTo(registry)）
- **参考实现**（已源码验证）：`MeterRegistry`（指标注册表）+ `MeterBinder`（接口，`bindTo(MeterRegistry)` 绑定指标）；microsphere-micrometer `AbstractMeterBinder implements MeterBinder`，具体实现 `MBeanAttributeMeterBinder`(jmx)/`SentinelMetrics`(sentinel)/`SystemMemoryMetrics`(system)/`CGroupMemoryMetrics`(cgroup)
- **对比取舍**：MeterBinder + MeterRegistry 是 Micrometer 扩展/注册的核心机制；扩展新指标=实现 MeterBinder
- **测试佐证**：microsphere-micrometer `AbstractMeterBinder.java implements MeterBinder` + 各 binder 实现 + micrometer-core MeterRegistry

---

## 三、聚合与深度汇总

| 知识点 | 维度 | 权重 | 优先级 | 深度 | 置信度 |
|--------|------|:---:|:---:|:---:|:---:|
| 指标核心概念+Tags | 性能 | 核心 | P1 | 🔴 | High |
| 指标类型(Timer/Counter...) | 性能 | 核心 | P1 | 🔴 | High |
| 指标聚合 | 性能 | 支撑 | P2 | 🟡 | High |
| Micrometer 核心 API | 工程 | 核心 | P1 | 🔴 | High |
| Counter 计数机制 | 性能 | 核心 | P1 | 🟡 | High |
| 内建 Binder | 工程 | 核心 | P1 | 🟡 | High |
| MeterBinder/Registry | 工程 | 核心 | P1 | 🟡 | High |

---

## 四、与 microsphere 的关联（参考实现已验证）

- **Micrometer 源码**：`code/spring/micrometer/micrometer-core`（Meter/Counter/Gauge/Timer/MeterRegistry）
- **microsphere-micrometer**：microsphere-observability 的 `instrument/binder/`（sentinel/jmx/system/jdbc）+ prometheus 集成
- `[待验证]` microsphere-micrometer 的完整封装

---

## 五、本节小结（三层次视角）

**需求**：理解 Micrometer 指标模型（类型/Tags）、核心 API 与内建 Binder——这是可观测性的基础。

**自主实现核心**：若我设计——
1. 指标类型选型：Counter(计数)/Timer(耗时)/Gauge(瞬时)/DistributionSummary(分布)
2. Tags 区分维度（应用/实例）
3. Builder 模式创建指标 + MeterRegistry 注册
4. MeterBinder 自动绑定 JVM/系统/Tomcat 指标

**参考实现**：Micrometer（code/spring/micrometer 有源码）+ microsphere-micrometer（binder sentinel/jmx/system）。Counter.builder(name).tags(...) + increment() 已源码验证。

**对比取舍**：知识本体是"Micrometer 指标模型与 API"。可观测性(第 13-16 篇)的基础。核心洞察：**指标类型 + Tags + Builder/MeterRegistry/Binder 是 Micrometer 三大机制**。

**待验证汇总**：
- Timer/DistributionSummary 分布统计细节
- microsphere-micrometer 完整封装
