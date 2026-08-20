# S-26 Actuator Health 聚合 — HealthIndicator → StatusAggregator → HealthEndpointGroups

> 依赖 S-21 Actuator + S-19 可用性 (复用) | 🔴 Deep | 6 KP | [模式: 策略 + 建造者 + 聚合]

**读者处境**: `/actuator/health` 返回 `{"status":"UP","components":{...}}` — 各组件(Db/Redis/DiskSpace)的状态怎么聚合成整体 status?为什么一个 DOWN 整个就 DOWN?liveness/readiness 探针端点哪来的?

### 1. 指示器抽象 — HealthIndicator + 注册

场景: 每个健康源(Db/Redis)是一个 HealthIndicator — 谁定义这个抽象?怎么注册?

源码路径:
- `HealthIndicator.java:28,45` — **抽象**: `interface HealthIndicator extends HealthContributor`(L28); `Health health()`(L45) — 每个健康源实现, 返回 Health
- `AbstractHealthIndicator.java:39,104` — **模板**: `abstract class`(L39); 模板方法 `doHealthCheck(Health.Builder builder)`(L104) — 子类填 builder, 父类统一 `new Health.Builder()`(L80) + 异常处理
- `HealthContributorRegistry.java` — **注册**: HealthIndicator 注册进 registry, 供 /actuator/health 枚举

关键设计: **Why 模板方法？** 各健康源只需 `doHealthCheck(builder)` 报告状态; 父类统一建 Builder、捕获异常(异常→DOWN)、记耗时 — 避免每个 indicator 重复样板; 也是 Composite(多个同类)与单例的通用基座。[模式: 模板方法 + 策略]

数据流: 实现 `class DbHealthIndicator extends AbstractHealthIndicator { doHealthCheck(builder){ builder.up()/down(ex) } }` → 注册进 HealthContributorRegistry → /actuator/health 枚举各 contributor → 各调 health() → 收集 Health。

### 2. 状态聚合 — StatusAggregator 谁说了算

场景: Db=UP、Redis=DOWN、Disk=UP — 整体 status 是什么?为什么一个 DOWN 就整体 DOWN?

源码路径:
- `HealthEndpointSupport.java:165` — **聚合入口**: `aggregateContributions(..., group.getStatusAggregator(), ...)`(L165) — 把所有组件贡献交给 StatusAggregator
- `StatusAggregator.java:33,58` — **策略接口**: `getAggregateStatus(Set<Status>)`(L58) — 组合多个状态
- `SimpleStatusAggregator.java:31,57` — **默认实现**: 基于有序状态列表(L31, 严重度顺序); DEFAULT_ORDER(L57); `AutoConfiguredHealthEndpointGroups.java:81` — `new SimpleStatusAggregator(properties.getStatus().getOrder())` — **order 来自 management.health.status.order**(默认 DOWN、OUT_OF_SERVICE、UP、UNKNOWN)
- `Health.java:277,330` — **状态**: Builder.up(L277)/down(L286)/status(L311)/build(L330); 状态码 UP/DOWN/OUT_OF_SERVICE/UNKNOWN

关键设计: **Why 有序聚合？** 整体状态 = 组件中**最严重**的那个 — 按 management.health.status.order 的严重度排序, 取最靠前的状态(有 DOWN 就整体 DOWN, 全 UP 才 UP); 用策略接口解耦聚合算法, 用户可自定义 StatusAggregator。[模式: 策略 + 严重度排序]

数据流: HealthEndpointSupport.aggregateContributions(L165) → 收集各组件 Status → group.getStatusAggregator().getAggregateStatus(statuses) → SimpleStatusAggregator 按 order[DOWN,OUT_OF_SERVICE,UP,UNKNOWN] 取最严重 → 整体 status=DOWN。

### 3. 分组与探针 — HealthEndpointGroups + Availability probes

场景: 为什么有 /actuator/health 之外还有 /livez /readyz?健康分组怎么组织?

源码路径:
- `AutoConfiguredHealthEndpointGroups.java:60,81` — **分组**: class(L60) 构建 primary 组 + 各组, 每组有自己的 StatusAggregator/HttpCodeStatusMapper/Show(如 L81 每组建聚合器)
- `IncludeExcludeGroupMemberPredicate.java` — **成员选择**: include/exclude(含 `*`/子路径)决定哪些 contributor 属于哪个组
- `AvailabilityProbesHealthEndpointGroups.java:52,54,67,68` — **探针**: LIVENESS="liveness"(L52, /livez, livenessState)+READINESS="readiness"(L54, /readyz, readinessState) — 由 S-19 的可用性状态驱动, 映射成健康组的探针端点

关键设计: **Why 分组？** 不同消费者要不同健康视图 — 主 health 全量, liveness/readiness 只看 S-19 的可用性状态(探针); 分组让每个组有自己的聚合器与包含成员。**Why probes 衔接 S-19？** K8s livenessProbe 打 /livez 对应 LivenessState, readinessProbe 打 /readyz 对应 ReadinessState — 把 S-19 的可用性状态机暴露成健康探针端点。[模式: 分组 + 探针映射]

数据流: /actuator/health → primary 组(全量聚合, §2) → 返回整体 status+components。K8s /livez → liveness 组 → 读 S-19 LivenessState → UP/BROKEN; /readyz → readiness 组 → 读 ReadinessState → ACCEPTING/REFUSING。

→ 引出 S-27: Metrics/Micrometer 编排 — 健康之后: MeterRegistryPostProcessor/CompositeMeterRegistry 的指标注册编排(前置 S-21)。
