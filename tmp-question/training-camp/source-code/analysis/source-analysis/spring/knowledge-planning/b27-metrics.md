# S-27 Metrics/Micrometer 编排 — MeterRegistryPostProcessor → CompositeMeterRegistry → PropertiesMeterFilter

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | MeterRegistryPostProcessor.java(170行)+CompositeMeterRegistryConfiguration.java(70行)+AutoConfiguredCompositeMeterRegistry.java(60行)+PropertiesMeterFilter.java(120行)+MetricsAutoConfiguration.java(120行)+JvmMetricsAutoConfiguration.java(40行)+MeterRegistryCustomizer.java(30行)
> 基线: BOOT-PLAN-v2 S-27 (深探新增) — 指标注册编排; 前置: **S-21 Actuator 端点** — 展开多 registry 组合/定制/过滤

---

## §0.8

- 🔴 Deep，1篇 — 注册编排(MeterRegistryPostProcessor.postProcessMeterRegistry[L93]: applyCustomizers[L96]→applyFilters[L97]→addToGlobalRegistryIfNecessary[L98]→applyBinders[L100] — **customizers 必须在 binders 前**) → 多 registry 组合(CompositeMeterRegistryConfiguration[L40-47]: @Conditional(MultipleNonPrimaryMeterRegistriesCondition)+@Primary AutoConfiguredCompositeMeterRegistry 聚合多 registry) → 配置→过滤(PropertiesMeterFilter[L46]: 把 management.metrics.*[common tags 等] 转成 MeterFilter) → 系统指标(MeterBinder: JVM/System 自动绑定)
- 设计模式: [模式: 后处理编排]—MeterRegistryPostProcessor 固定顺序; [模式: 组合]—CompositeMeterRegistry; [模式: 策略过滤]—MeterFilter

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| MeterRegistryPostProcessor.java:93,96,100 | 编排 | **postProcessMeterRegistry(L93)**: customizers(L96)→filters(L97)→global(L98)→binders(L100) — 顺序决策(customizers 先于 binders) | High |
| CompositeMeterRegistryConfiguration.java:40,44,45 | 组合 | **@Conditional(MultipleNonPrimaryMeterRegistries)(L40)+@Primary(L44)**: AutoConfiguredCompositeMeterRegistry(clock, registries)(L45-46) | High |
| PropertiesMeterFilter.java:46,52 | 过滤 | **implements MeterFilter(L46)**: 构造(MetricsProperties)(L52) — management.metrics.* → 过滤规则 | High |
| JvmMetricsAutoConfiguration.java | 系统指标 | **MeterBinder**: 注册 JVM/System 等指标(thin wiring, 辅助) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: Metrics 编排是一条线(顺序→组合→过滤), 3 块耦合 — 1篇 (~50行) 按"注册编排 → 多 registry 组合 → 配置过滤"展开; S-21 端点机制复用, MeterBinder 系统指标仅一笔带过(thin)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 注册编排 (MeterRegistryPostProcessor 固定顺序) | 🔴 | **为什么🔴**: customizers/filters/binders 应用顺序 |
| P1-2 | 多 registry 组合 (Composite + Primary 条件) | 🔴 | **为什么🔴**: 多个 registry 如何注入 |
| P1-3 | 配置→过滤 (PropertiesMeterFilter + MetricsProperties) | 🔴 | **为什么🔴**: management.metrics.* 生效 |
| P2-1 | MeterBinder 系统指标 (JVM/System) | 🟡 | **为什么🟡**: 系统指标自动绑定 |
| P2-2 | 生命周期 (MeterRegistryCloser ContextClosed) | 🟡 | **为什么🟡**: 关闭时清理 |
| P3-1 | 与 S-21 边界 (端点复用) | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **注册编排** | 🔴 | 应用顺序 |
| B | **组合与过滤** | 🔴 | 多 registry/配置 |
| C | **系统指标与边界** | 🟡 | 辅助 |

> **Cluster A (§1)**: MeterRegistryPostProcessor(customizers→filters→global→binders)
> **Cluster B (§2)**: CompositeMeterRegistryConfiguration(多 registry @Primary 组合) + PropertiesMeterFilter(配置→过滤)
> **Cluster C (§3)**: MeterBinder 系统指标 + MeterRegistryCloser + S-21 边界

→ 引出 S-28: Spring Data 仓库自动注册 — 指标之后: AbstractRepositoryConfigurationSourceSupport/RepositoriesRegistrar 的仓库扫描注册(前置 S-10/S-11)
