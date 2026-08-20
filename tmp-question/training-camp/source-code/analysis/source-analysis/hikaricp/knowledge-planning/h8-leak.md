# H-8 泄漏检测 — ProxyLeakTask / leakDetectionThreshold

> 项目: HikariCP | 🟡 Working / 1 篇 | ProxyLeakTask.java(95行)+ProxyLeakTaskFactory.java(55行)+HikariConfig.java
> 基线: HIKARICP-PLAN H-8 (第3层代理监控, 🟡) — 借用超时泄漏跟踪; 前置: **H-12 代理(ProxyConnection) + H-3/H-4 生命周期** — 展开泄漏检测

---

## §0.8

- 🟡 Working，1篇 — 泄漏检测入口(ProxyLeakTaskFactory.schedule[L38]: 阈值0→NO_LEAK 空实现[L40] 否则 scheduleNewTask; borrow 时 schedule, close 时 cancel[H-3 L179/H-4 L246]) → 泄漏报告(ProxyLeakTask.run[L75]: 借用超时未还 → isLeaked + LOGGER.warn 栈追踪[L80-84]) → 配置(leakDetectionThreshold[L66, 默认0=不检测]/getter L221/setter L228)
- 设计模式: [模式: 延迟任务]—schedule 定时报告; [模式: 空对象]—NO_LEAK 零开销; [模式: 生命周期钩子]—borrow schedule/close cancel

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ProxyLeakTaskFactory.java:38,40 | 入口 | **schedule(L38)**: 阈值0→NO_LEAK(L40) | High |
| ProxyLeakTask.java:32,68,70 | 调度 | **class(L32)+schedule(L68)→executor.schedule(this, 阈值)(L70)** | High |
| ProxyLeakTask.java:75,80 | 报告 | **run(L75)**: isLeaked+WARN 栈追踪(L80-84) | High |
| HikariConfig.java:66,221,228 | 配置 | **leakDetectionThreshold(L66, 默认0)+getter/setter** | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 泄漏检测是一条线(入口→报告→配置), 3 块耦合但机制较薄 — 1篇 (~44行) 按"入口与生命周期 → 泄漏报告 → 配置与边界"展开; H-12/H-3/H-4 复用。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | 泄漏检测入口 (schedule/cancel + NO_LEAK) | 🔴 | **为什么🔴**: 何时启动检测 |
| P1-2 | run() 泄漏报告 (栈追踪 + WARN) | 🔴 | **为什么🔴**: 泄漏怎么暴露 |
| P1-3 | leakDetectionThreshold 配置 (默认0) | 🔴 | **为什么🔴**: 检测开关 |
| P2-1 | 与 H-3/H-4 生命周期衔接 | 🟡 | **为什么🟡**: borrow schedule/close cancel |
| P2-2 | NO_LEAK 空实现 (零开销) | 🟡 | **为什么🟡**: 未开启零成本 |
| P3-1 | 与 H-12 边界 | 🟢 | **为什么🟢**: 代理衔接 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **入口与生命周期** | 🔴 | 何时检测 |
| B | **泄漏报告** | 🔴 | 怎么暴露 |
| C | **配置与边界** | 🟡 | 开关 |

> **Cluster A (§1)**: ProxyLeakTaskFactory.schedule(NO_LEAK) + borrow/close 衔接
> **Cluster B (§2)**: ProxyLeakTask.run(isLeaked + WARN 栈追踪)
> **Cluster C (§3)**: leakDetectionThreshold 配置 + H-12 边界

→ 引出 H-9: 指标监控 — 泄漏之后: MetricsTracker/PoolStats/Micrometer 的池指标(前置 H-1)
