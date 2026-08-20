# S-27 Metrics/Micrometer 编排 — MeterRegistryPostProcessor → CompositeMeterRegistry → PropertiesMeterFilter

> 依赖 S-21 Actuator (复用) | 🔴 Deep | 6 KP | [模式: 后处理编排 + 组合 + 策略过滤]

**读者处境**: 加 actuator 后 /actuator/prometheus 有指标 — 谁把 customizer/binder 应用到所有 MeterRegistry?多个导出(如 Prometheus+JMX)怎么注入一个 registry?management.metrics.tags 怎么生效?

### 1. 注册编排 — MeterRegistryPostProcessor 固定顺序

场景: 容器里每个 MeterRegistry 都要被定制/过滤/绑定 — 顺序怎么定?为什么?

源码路径:
- `MeterRegistryPostProcessor.java:93` — **编排**: `postProcessMeterRegistry(meterRegistry)`(L93)
- `MeterRegistryPostProcessor.java:96,97,98,100` — **顺序**: `applyCustomizers`(L96)→`applyFilters`(L97)→`addToGlobalRegistryIfNecessary`(L98)→`applyBinders`(L100)
- `MeterRegistryPostProcessor.java:94,95` — **注释决策**: "Customizers must be applied before binders, as they may add custom tags or alter timer/summary configuration"(L94-95)

关键设计: **Why customizers 先于 binders？** customizer 可能加公共 tag/改 timer 配置 — binder 绑定指标时要用到这些配置; 若 binder 先跑, 绑定的指标就漏了 customizer 加的 tag/配置。这是明确的顺序决策(源码注释点明)。[模式: 后处理编排]

数据流: 容器创建 MeterRegistry → BeanPostProcessor(MeterRegistryPostProcessor) → postProcessMeterRegistry(L93) → customizers(L96, 加 tag/配置) → filters(L97, 过滤) → global registry(L98) → binders(L100, 绑定系统指标)。

### 2. 多 registry 组合 — CompositeMeterRegistry + Primary

场景: 配了 Prometheus + JMX 两个导出 → 容器有多个 MeterRegistry — 用户注入哪个?

源码路径:
- `CompositeMeterRegistryConfiguration.java:40,41` — **条件**: `@Conditional(MultipleNonPrimaryMeterRegistriesCondition.class)`(L40) — 仅当"多候选且无 primary"时
- `CompositeMeterRegistryConfiguration.java:43,44,45,46` — **组合**: `@Bean @Primary AutoConfiguredCompositeMeterRegistry compositeMeterRegistry(clock, registries)`(L43-46) — 把所有 registry 包进一个 @Primary 组合
- `AutoConfiguredCompositeMeterRegistry` — 继承 Micrometer CompositeMeterRegistry, 写指标广播到所有子 registry

关键设计: **Why Composite + @Primary？** 多个导出 registry 时, 让所有 MeterBinder/用户都注入同一个 CompositeMeterRegistry, 指标自动广播到 Prometheus/JMX 等 — 单一注入点 + 多后端; 条件"多且无 primary"避免单 registry 时多此一举。[模式: 组合 + Primary]

数据流: Prometheus+JMX 两个 MeterRegistry → MultipleNonPrimaryMeterRegistriesCondition 命中 → @Primary AutoConfiguredCompositeMeterRegistry(L45) 包住两者 → 用户/ binder 注入组合 registry → 指标广播到两个后端。

### 3. 配置→过滤 — PropertiesMeterFilter + 系统指标

场景: management.metrics.tags=env=prod 怎么变成每个指标的 tag?JVM 内存/线程指标谁绑的?

源码路径:
- `PropertiesMeterFilter.java:46,52` — **配置→filter**: `implements MeterFilter`(L46), 构造 `PropertiesMeterFilter(MetricsProperties)`(L52) — 把 management.metrics.*(tags/过滤等) 转成 MeterFilter 应用到所有指标
- `MetricsAutoConfiguration.java:69,70` — **注册**: `@Bean PropertiesMeterFilter propertiesMeterFilter(MetricsProperties)`(L69-70)
- `JvmMetricsAutoConfiguration.java` / `SystemMetricsAutoConfiguration.java` — **系统指标**: 把 Micrometer 的 JVM/系统 `MeterBinder` 注册成 Bean, 由 §1 的 applyBinders 绑定

关键设计: **Why 配置转 MeterFilter？** 用户配 management.metrics.tags 是声明式 — 转成 MeterFilter 后, 每个注册的指标都被统一注入公共 tag/过滤规则; 这样配置与指标注册解耦, 一处配置全局生效。**Why MeterBinder 单独 Bean？** 系统指标(JVM 内存/线程/GC)是薄绑定, 由 binders 阶段统一绑定 — 属 thin wiring, 一笔带过。[模式: 策略过滤 + 声明式配置]

数据流: management.metrics.tags → MetricsProperties → PropertiesMeterFilter(L52) → 由 §1 的 applyFilters 应用到每个 registry → 每指标带公共 tag。JVM 指标: JvmMetricsAutoConfiguration 注册 MeterBinder → §1 applyBinders 绑定 → 各 registry 有 JVM 指标。

→ 引出 S-28: Spring Data 仓库自动注册 — 指标之后: AbstractRepositoryConfigurationSourceSupport/RepositoriesRegistrar 的仓库扫描注册(前置 S-10/S-11)。
