# S-14 TaskExecutor 自动配置 — applicationTaskExecutor (平台/虚拟线程选择)

> 项目: Spring Boot 3.x | 🟡 Working / 1 篇 | TaskExecutionAutoConfiguration+TaskExecutorConfigurations+ThreadPoolTaskExecutorBuilder+TaskExecutionProperties
> 基线: BOOT-PLAN-v2 S-14 — @Async 默认线程池; 前置: **C-7 TaskExecutor(机制复用) + S-5** — 展开默认执行器装配与线程选择

---

## §0.8

- 🟡 Working，1篇 — 条件(@ConditionalOnClass(ThreadPoolTaskExecutor)) → 装配(TaskExecutorConfigurations: applicationTaskExecutor @Bean(APPLICATION_TASK_EXECUTOR_BEAN_NAME) + @ConditionalOnThreading: VIRTUAL→SimpleAsyncTaskExecutor 虚拟线程 / PLATFORM→ThreadPoolTaskExecutorBuilder.build 平台池) → 配置(TaskExecutionProperties: spring.task.execution.* + builder @ConditionalOnMissingBean) → 与 @Async 衔接
- 设计模式: [模式: 条件装配]—线程类型选择; [模式: 构建器]—ThreadPoolTaskExecutorBuilder; [模式: 用户优先]—@ConditionalOnMissingBean

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| TaskExecutionAutoConfiguration.java:35,42,47 | 装配 | **@ConditionalOnClass(ThreadPoolTaskExecutor)(L35) + @Import(TaskExecutorConfigurations)(L38) + APPLICATION_TASK_EXECUTOR_BEAN_NAME(L47)** | High |
| TaskExecutorConfigurations.java:60,62 | 虚拟线程 | **applicationTaskExecutorVirtualThreads L62**: @ConditionalOnThreading(VIRTUAL) → SimpleAsyncTaskExecutor(builder) — 虚拟线程模式 | High |
| TaskExecutorConfigurations.java:66,69 | 平台池 | **applicationTaskExecutor L69**: @ConditionalOnThreading(PLATFORM) → threadPoolTaskExecutorBuilder.build() — C-7 池机制 | High |
| TaskExecutorConfigurations.java:76,80 | builder | **ThreadPoolTaskExecutorBuilderConfiguration L76: threadPoolTaskExecutorBuilder L80**(@ConditionalOnMissingBean + TaskExecutionProperties) | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 默认执行器装配约 300 行 — 知识主线: "条件 → 线程类型选择 → builder 构建". 1篇 (~44行) 按"装配→线程选择→配置→衔接"展开; C-7 池化机制复用(只讲装配)。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | applicationTaskExecutor (默认执行器 Bean) | 🔴 | **为什么🔴**: @Async 的默认执行器 — 名称约定 |
| P1-2 | @ConditionalOnThreading (虚拟线程 vs 平台池) | 🔴 | **为什么🔴**: Java 21 虚拟线程开关 — spring.threads.virtual.enabled |
| P1-3 | ThreadPoolTaskExecutorBuilder (builder + 属性) | 🔴 | **为什么🔴**: 执行器构建方式 — C-7 机制复用 |
| P2-1 | 与 @Async 衔接 (AsyncAnnotationBeanPostProcessor) | 🟡 | **为什么🟡**: 默认执行器怎么被 @Async 使用 |
| P2-2 | 与 C-7 边界 (机制 vs 装配) | 🟡 | **为什么🟡**: 池化机制在 C-7 |
| P3-1 | 用户自定义 TaskExecutor 覆盖 | 🟢 | **为什么🟢**: @ConditionalOnMissingBean |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **默认执行器装配** (Bean + 名称) | 🔴 | @Async 默认执行器 |
| B | **线程类型选择** (VIRTUAL/PLATFORM) | 🔴 | 虚拟线程开关 |
| C | **构建与边界** (builder + C-7) | 🟡 | 构建方式与分工 |

> **Cluster A (§1)**: TaskExecutionAutoConfiguration(条件 + @Import + bean 名)
> **Cluster B (§2)**: TaskExecutorConfigurations(applicationTaskExecutor: Threading 条件两分支)
> **Cluster C (§3)**: ThreadPoolTaskExecutorBuilder(属性) + @Async 衔接 + C-7 边界

→ 引出 S-15: AOT/Native Image — 异步层收束, AOT: SpringApplicationAotProcessor 与 RuntimeHints(s21 AOT 机制复用)

(End of file - total 61 lines)
