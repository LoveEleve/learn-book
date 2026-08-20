# S-26 Actuator Health 聚合 — HealthIndicator → StatusAggregator → HealthEndpointGroups

> 项目: Spring Boot 3.x | 🔴 Deep / 1 篇 | HealthIndicator(50行)+AbstractHealthIndicator(110行)+Health.java(340行)+HealthContributorRegistry(50行)+HealthEndpointSupport(180行)+StatusAggregator(60行)+SimpleStatusAggregator(80行)+AutoConfiguredHealthEndpointGroups(130行)+IncludeExcludeGroupMemberPredicate(80行)+AvailabilityProbesHealthEndpointGroups(140行)
> 基线: BOOT-PLAN-v2 S-26 (深探新增) — 健康状态聚合; 前置: **S-21 Actuator 端点 + S-19 可用性(Liveness/Readiness)** — 展开聚合/分组/探针

---

## §0.8

- 🔴 Deep，1篇 — 指示器抽象(HealthIndicator.health() + AbstractHealthIndicator.doHealthCheck(builder) + HealthContributorRegistry 注册) → 状态聚合(HealthEndpointSupport.aggregateContributions[L165] → StatusAggregator.getAggregateStatus + SimpleStatusAggregator[order 来自 management.health.status.order]) → 状态模型(Health: UP/DOWN/OUT_OF_SERVICE/UNKNOWN + Builder.up/down/status/build) → 分组编排(AutoConfiguredHealthEndpointGroups + IncludeExcludeGroupMemberPredicate + AvailabilityProbesHealthEndpointGroups[liveness /livez /readiness /readyz])
- 设计模式: [模式: 策略]—StatusAggregator; [模式: 建造者]—Health.Builder; [模式: 聚合]—组件健康→整体

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| HealthIndicator.java:28,45 | 指示器 | **interface(L28)**: health()(L45); AbstractHealthIndicator(L39): doHealthCheck(builder)(L104) | High |
| HealthEndpointSupport.java:165 | 聚合 | **aggregateContributions(apiVersion, contributions, group.getStatusAggregator(), ...)(L165)** — 多组件→整体 | High |
| StatusAggregator.java:33,58 | 聚合接口 | **interface(L33)**: getAggregateStatus(statuses)(L58) — 组合多个组件状态 | High |
| SimpleStatusAggregator.java:31,57 | 默认聚合 | **order 有序列表(L31)**: DEFAULT_ORDER(L57) — 严重度高者胜出; AutoConfiguredHealthEndpointGroups:81 SimpleStatusAggregator(properties.getStatus().getOrder()) | High |
| Health.java:277,330 | 状态模型 | **Builder.up(L277)/down(L286)/status(L311)/build(L330)** — UP/DOWN/OUT_OF_SERVICE/UNKNOWN | High |
| AvailabilityProbesHealthEndpointGroups.java:52,67 | 探针 | **LIVENESS="liveness"(L52)/livez/livenessState; READINESS="readiness"(L54)/readyz/readinessState(L67-68)** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: Health 聚合是一条线(指示器→聚合→分组), 3 块耦合 — 1篇 (~52行) 按"指示器抽象 → 状态聚合 → 分组与探针"展开; S-21 端点机制复用, S-19 可用性衔接。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | HealthIndicator 抽象 (HealthIndicator/AbstractHealthIndicator/Registry) | 🔴 | **为什么🔴**: 组件健康怎么报告 |
| P1-2 | 状态聚合 (StatusAggregator + SimpleStatusAggregator + order) | 🔴 | **为什么🔴**: 多组件→整体谁说了算 |
| P1-3 | Health 状态模型 (UP/DOWN/... + Builder) | 🔴 | **为什么🔴**: 健康状态表示 |
| P2-1 | 分组编排 (AutoConfiguredHealthEndpointGroups + IncludeExclude) | 🟡 | **为什么🟡**: 多组健康端点 |
| P2-2 | Availability probes (liveness/readiness 组) | 🟡 | **为什么🟡**: K8s 探针端点 |
| P3-1 | 与 S-21/S-19 边界 | 🟢 | **为什么🟢**: 复用边界 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **指示器抽象** | 🔴 | 组件怎么报健康 |
| B | **聚合与状态** | 🔴 | 多→整体 |
| C | **分组与探针** | 🟡 | 端点组织 |

> **Cluster A (§1)**: HealthIndicator/AbstractHealthIndicator + HealthContributorRegistry
> **Cluster B (§2)**: HealthEndpointSupport.aggregate + StatusAggregator(Simple: order) + Health.Builder
> **Cluster C (§3)**: AutoConfiguredHealthEndpointGroups + IncludeExclude + AvailabilityProbes(liveness/readiness)

→ 引出 S-27: Metrics/Micrometer 编排 — 健康之后: MeterRegistryPostProcessor/CompositeMeterRegistry 的指标注册编排(前置 S-21)
