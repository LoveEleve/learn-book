# C-7 TaskExecutor — 任务执行器家族 (接口 → 简单实现 → 线程池 → 适配器)

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | TaskExecutor(52行)+SyncTaskExecutor(51行)+SimpleAsyncTaskExecutor(525行)+VirtualThreadTaskExecutor(约120行)+ThreadPoolTaskExecutor(460行)+ConcurrentTaskExecutor(268行)
> 基线: C-6 Profile 结尾桥 — 条件装配定了"哪些 Bean 在", 异步执行定"谁在什么线程跑" — @Async/@Scheduled 的底层执行器; 原始执行计划 0-7

---

## §0.8

- 🟡 Working，1篇 — 接口(TaskExecutor extends Executor: 单方法 execute) → 简单实现(SyncTaskExecutor: 同线程 task.run(); SimpleAsyncTaskExecutor: 每任务新线程+并发上限) → 线程池(ThreadPoolTaskExecutor: core/max/queue/keepAlive 参数→JDK ThreadPoolExecutor + TaskDecorator/钩子) → 适配器(ConcurrentTaskExecutor: 包装任意 Executor) → 选型(同步/异步/池化/虚拟线程)
- 设计模式: [模式: 适配器]—ConcurrentTaskExecutor 包装 JDK Executor; [模式: 模板方法]—ExecutorConfigurationSupport 固定初始化骨架; [模式: 工厂]—ThreadPoolTaskExecutor 组装 JDK 线程池

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| TaskExecutor.java:39,50 | 接口 | **统一接口**: extends java.util.concurrent.Executor — execute(Runnable) — 让 Spring 各处(事件/异步/调度)只依赖这一个抽象 | High |
| SyncTaskExecutor.java:38,46 | 同步实现 | **同步执行**: execute→task.run() 调用线程直接跑 — 最简, 用于测试/需串行的场景 | High |
| SimpleAsyncTaskExecutor.java:64,87,249 | 每任务新线程 | **无池化**: execute 每次 new Thread — threadFactory(L87)/setVirtualThreads(虚拟线程支持)/setConcurrencyLimit(L249 并发上限, 默认无界) — 高频短任务不可用(线程爆炸) | High |
| ThreadPoolTaskExecutor.java:84,278 | 线程池实现 | **池化核心**: initializeExecutor L278: createQueue(queueCapacity)→`new ThreadPoolExecutor(core, max, keepAliveSeconds, SECONDS, queue, threadFactory, rejectedHandler)` — 覆写 execute(TaskDecorator)+beforeExecute/afterExecute 钩子 | High |
| ThreadPoolTaskExecutor.java:119,142,191 | 参数映射 | **Bean 风格配置**: setCorePoolSize/setMaxPoolSize/setQueueCapacity(默认 Integer.MAX_VALUE, L95)/setKeepAliveSeconds — 属性在 initializeExecutor 一次性组装成 JDK 线程池 | High |
| ThreadPoolTaskExecutor.java:385 | execute() | **执行链**: execute→executor.execute(task) — 空线程池懒创建(initializeExecutor 首次执行才调) | High |
| ConcurrentTaskExecutor.java:66,126 | 适配器 | **JDK 适配**: 包装任意 java.util.concurrent.Executor(ScheduledThreadPoolExecutor 等) — setConcurrentExecutor(L126) | High |
| AsyncTaskExecutor.java | 扩展接口 | **带超时**: execute(task, startTimeout) — 提交任务的等待超时语义 | High |

---

## 02-04 聚合+分类+聚类 (1篇)

**1篇理由**: 接口+5个实现约 1400 行 — 骨架单线: "接口 → 简单→池化→适配". 1篇 (~45行) 按"接口→简单→池化→适配→选型"展开; 若分 2 篇则"参数映射"与"执行链"割裂。

### P1P2P3 分级

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | ThreadPoolTaskExecutor (参数→JDK 线程池组装 + 懒初始化 + TaskDecorator/钩子) | 🔴 | **为什么🔴**: 生产默认执行器 — @Async 默认用它 — "Bean 配置→线程池"的组装方式是框架实践范本 |
| P1-2 | SimpleAsyncTaskExecutor 语义 (每任务新线程 + 并发上限 + 虚拟线程) | 🔴 | **为什么🔴**: 无池化方案的边界 — 高并发下线程爆炸的经典教训 — Boot 旧版默认的坑 |
| P1-3 | TaskExecutor 统一接口 (execute 单方法 + 家族关系) | 🔴 | **为什么🔴**: 抽象的意义 — 调用方只依赖接口, 实现可换(测试用 Sync/生产用 ThreadPool) |
| P2-1 | SyncTaskExecutor vs ConcurrentTaskExecutor (同步/适配) | 🟡 | **为什么🟡**: 另两个形态 — 同步执行与"复用已有线程池" |
| P2-2 | 参数语义 (core/max/queue 与 JDK 线程池策略) | 🟡 | **为什么🟡**: 线程池调优的基础 — 拒绝策略/队列满时的行为 |
| P3-1 | VirtualThreadTaskExecutor (Java 21 虚拟线程) | 🟢 | **为什么🟢**: 现代演进 — 每任务一线程的新经济模型 |

### 深度分类

| Cluster | KP | 级别 | 为什么 |
|:--:|------|:--:|------|
| A | **接口与家族** (TaskExecutor + Sync/SimpleAsync) | 🔴 | 抽象 + 两个简单实现的语义 |
| B | **池化实现** (ThreadPoolTaskExecutor 组装+执行) | 🔴 | 生产核心 — 参数映射到 JDK 线程池 |
| C | **适配与现代** (ConcurrentTaskExecutor + 虚拟线程) | 🟡 | 复用与演进 |

> **Cluster A (§1)**: TaskExecutor 接口 + SyncTaskExecutor(同线程) + SimpleAsyncTaskExecutor(每任务新线程)
> **Cluster B (§2)**: ThreadPoolTaskExecutor — 参数/组装/懒初始化/执行链 — 与 @Async 的衔接
> **Cluster C (§3)**: ConcurrentTaskExecutor 适配器 + 家族选型表 + VirtualThreadTaskExecutor

→ 引出 3-1: SpEL — @Async/@Scheduled 的表达式(如 cron)里 `#{...}` 就是 SpEL — 表达式解析引擎(ExpressionParser→AST→求值)

(End of file - total 61 lines)
