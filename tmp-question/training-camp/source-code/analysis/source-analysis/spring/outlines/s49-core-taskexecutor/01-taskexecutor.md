# C-7 TaskExecutor — 任务执行器家族 (接口 → 简单实现 → 线程池 → 适配器)

> 依赖 C-6 Profile | 🟡 Working | 6 KP | [模式: 适配器 + 模板方法]

**读者处境**: @Async 的方法跑在哪个线程？@Scheduled 定时任务用什么执行？TaskExecutor 和 java.util.concurrent.Executor 什么关系？为什么测试环境和生产的执行器不同也能跑？

### 1. TaskExecutor 接口 + SyncTaskExecutor / SimpleAsyncTaskExecutor

场景: 异步事件、@Async 调用、定时任务 — 都只需"把 Runnable 丢进去执行" — 至于同步/新线程/线程池, 调用方不关心 — 这就是 TaskExecutor 抽象的意义。

源码路径:
- `TaskExecutor.java:39,50` — **接口**: extends java.util.concurrent.Executor — 单方法 execute(Runnable) — Spring 各模块(事件监听/调度/异步)统一依赖它
- `SyncTaskExecutor.java:38,46` — **同步版**: execute→task.run() — 调用线程直接执行 — 测试/必须串行的场景
- `SimpleAsyncTaskExecutor.java:64,87,249` — **每任务新线程**: execute 每次 new Thread — threadFactory(L87) / setVirtualThreads(Java 21 虚拟线程) / setConcurrencyLimit(L249 并发上限, 默认无界)

关键设计: **Why 抽象 TaskExecutor 而非直接用 ExecutorService？** Spring 需要"最小可执行契约" — 同步、异步、池化实现各异, 但调用方只依赖 execute(Runnable); 且 Spring 的 bean 生命周期/事件/异步都要注入它 — 单一抽象让实现可替换(测试用 Sync, 生产用 ThreadPool)。[模式: 抽象接口]

数据流: `applicationEventPublisher.publishEvent(x)` → 异步监听器 → listener.getExecutor().execute(runnable) → 若注入 SyncTaskExecutor → 监听线程内同步跑; 若 SimpleAsyncTaskExecutor → new Thread(runnable).start() — 每事件一个新线程。

### 2. ThreadPoolTaskExecutor — Bean 风格的线程池组装

场景: `@Bean ThreadPoolTaskExecutor` 配 corePoolSize=5 maxPoolSize=10 queueCapacity=100 — 这些属性怎么变成真正的线程池？与 JDK ThreadPoolExecutor 什么关系？

源码路径:
- `ThreadPoolTaskExecutor.java:84,278` — **initializeExecutor()**: L279 createQueue(queueCapacity) → L281 `new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds, TimeUnit.SECONDS, queue, threadFactory, rejectedExecutionHandler)` — **直接构造 JDK 线程池** — 覆写 execute(TaskDecorator 装饰)+beforeExecute/afterExecute 钩子(生命周期回调)
- `ThreadPoolTaskExecutor.java:119,142,191` — **Bean 参数**: setCorePoolSize/setMaxPoolSize/setQueueCapacity(默认 Integer.MAX_VALUE, L95 — 无界队列!)/setKeepAliveSeconds — 全部在 initializeExecutor 一次性组装
- `ThreadPoolTaskExecutor.java:385` — **execute()**: 首次调用触发 initializeExecutor(懒创建线程池)→executor.execute(task)

关键设计: **Why 不直接用 JDK ThreadPoolExecutor 而包一层？** ①Bean 化: 属性注入/生命周期(初始化销毁)与容器统一 ②装饰与钩子: TaskDecorator(上下文传递)、beforeExecute/afterExecute(监控/清理) ③懒初始化: 容器启动不建线程, 首次执行才建。[模式: 封装/门面]

数据流: @Async 方法调用 → AsyncAnnotationBeanPostProcessor → 默认 executor(容器找 TaskExecutor bean, Boot 的 applicationTaskExecutor)→ ThreadPoolTaskExecutor.execute(L385) → 首次: initializeExecutor → new ThreadPoolExecutor(5,10,60s,LinkedBlockingQueue(100)) → task 入队 → 5 核心线程消费 → 队列满→扩到 max 10 → 仍满→AbortPolicy(L386, 默认)→抛 RejectedExecutionException→Spring 包成 TaskRejectedException。

### 3. ConcurrentTaskExecutor 适配器 + 家族选型

场景: 项目已有 java.util.concurrent.Executor(如 ScheduledThreadPoolExecutor), 想复用 — 或测试环境想同步 — 家族怎么选？

源码路径:
- `ConcurrentTaskExecutor.java:66,126` — **适配器**: setConcurrentExecutor(任意 Executor) → execute 委托 adaptedExecutor(L156) — 把 JDK 线程池"翻译"成 Spring TaskExecutor
- `AsyncTaskExecutor.java`(接口) — **带超时扩展**: execute(task, startTimeout) — 提交等待语义
- `VirtualThreadTaskExecutor.java:32` — **虚拟线程**: Java 21+ 每任务一个虚拟线程(廉价) — SimpleAsyncTaskExecutor 也支持 setVirtualThreads 切换

关键设计: **Why 需要适配器？** 团队已有线程池/第三方框架暴露 Executor — 适配让"已有资产"无缝接入 Spring 体系而无需重写; 对称地, ThreadPoolTaskExecutor 也暴露 getThreadPoolExecutor() 反向取 JDK 对象。[模式: 适配器]

数据流: @Bean executor = new ConcurrentTaskExecutor(); executor.setConcurrentExecutor(new ScheduledThreadPoolExecutor(4)) → @Async 调用 → execute → adaptedExecutor.execute → JDK 池执行。选型: 测试→SyncTaskExecutor(可断言线程); 默认生产→ThreadPoolTaskExecutor(池化); 无界任务→SimpleAsyncTaskExecutor(每任务线程, 高并发慎用); 复用已有→ConcurrentTaskExecutor。

→ 引出 3-1: SpEL — @Scheduled(cron="#{...}")/@Cacheable(key="#{...}") 里的 SpEL 表达式: 解析→AST→求值 — 表达式引擎三阶段。
