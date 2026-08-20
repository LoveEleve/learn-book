# S-14 TaskExecutor 自动配置 — applicationTaskExecutor

> 依赖 C-7 (复用) + S-5 | 🟡 Working | 6 KP | [模式: 条件装配 + 构建器]

**读者处境**: @Async 的方法跑在哪个线程池?虚拟线程怎么开?默认执行器叫什么?

### 1. 默认执行器装配

场景: @Async 方法默认用 applicationTaskExecutor — 谁建的?为什么叫这个名?

源码路径:
- `TaskExecutionAutoConfiguration.java:35,42,47` — **装配**: @ConditionalOnClass(ThreadPoolTaskExecutor)(L35 — 线程池类在 classpath) + @Import(TaskExecutorConfigurations)(L38) + `APPLICATION_TASK_EXECUTOR_BEAN_NAME = "applicationTaskExecutor"`(L47 — 约定名)
- `TaskExecutorConfigurations.java:53` — **配置集合**: 内部多配置类, 按线程类型选择

关键设计: **Why 约定 bean 名？** @Async 的执行器通过"查找名字"确定(AsyncAnnotationPostProcessor 找 applicationTaskExecutor 或 TaskExecutor 类型)— 固定名让自动装配与使用方解耦: 用户换执行器只需同名覆盖或自定义 bean。[模式: 名称约定]

数据流: 有 spring-context 的 ThreadPoolTaskExecutor(C-7) → TaskExecutionAutoConfiguration 激活 → @Import(TaskExecutorConfigurations) → 按线程类型(§2)建 applicationTaskExecutor → @Async 方法(AsyncAnnotationPostProcessor)找到它 → 异步执行。

### 2. 线程类型选择 — 平台池 vs 虚拟线程

场景: spring.threads.virtual.enabled 开关 — 虚拟线程模式用哪个执行器?

源码路径:
- `TaskExecutorConfigurations.java:60,62` — **虚拟线程**: applicationTaskExecutorVirtualThreads L62: @ConditionalOnThreading(Threading.VIRTUAL) → SimpleAsyncTaskExecutor(builder 构建) — 每任务虚拟线程(C-7 的 SimpleAsync 语义)
- `TaskExecutorConfigurations.java:66,69` — **平台池**: applicationTaskExecutor L69: @ConditionalOnThreading(Threading.PLATFORM) → threadPoolTaskExecutorBuilder.build() — C-7 的 ThreadPoolTaskExecutor 池
- 开关: spring.threads.virtual.enabled=true(JDK 21+) → VIRTUAL 分支

关键设计: **Why @ConditionalOnThreading？** 同一 bean 名两套实现, 由虚拟线程开关选择 — Boot 3.2+ 的 Threading 条件(S-3 家族); 平台模式=传统线程池, 虚拟模式=轻量每任务线程。[模式: 条件装配 + 双实现]

数据流: spring.threads.virtual.enabled=false(默认) → Threading.PLATFORM → applicationTaskExecutor(L69): ThreadPoolTaskExecutorBuilder.build() → 传统池(C-7 机制)。=true(JDK 21) → VIRTUAL → SimpleAsyncTaskExecutor(builder) → 每任务虚拟线程。

### 3. Builder 构建 + @Async 衔接

场景: 执行器怎么用属性构建?@Async 怎么找到它?

源码路径:
- `TaskExecutorConfigurations.java:76,80` — **builder**: ThreadPoolTaskExecutorBuilderConfiguration L76 → threadPoolTaskExecutorBuilder L80(@ConditionalOnMissingBean + TaskExecutionProperties — spring.task.execution.*: pool-size/queue-capacity 等) — builder 模式组装 C-7 线程池
- 衔接: AsyncAnnotationPostProcessor(s17 已讲)找 TaskExecutor → applicationTaskExecutor 命中 → @Async 使用
- 覆盖: 用户 @Bean TaskExecutor/Executor → @ConditionalOnMissingBean 让位

关键设计: **Why Builder？** 与 C-7 直接 new 不同, Boot 用 Builder 封装"属性→线程池"映射(线程数/队列/存活时间), 语义更清晰; 用户也可用同一 Builder 定制。**Why 与 C-7 边界？** 池化机制(C-7)引用, 装配(本域)展开 — 06 §2.5。[模式: 构建器 + 边界声明]

数据流: spring.task.execution.pool.core-size=8 → TaskExecutionProperties(S-5) → ThreadPoolTaskExecutorBuilder.build() → ThreadPoolTaskExecutor(8 线程, C-7 机制) → applicationTaskExecutor → @Async 方法异步执行。用户 @Bean TaskExecutor(自定义) → 跳过自动装配, @Async 用用户的。

→ 引出 S-15: AOT/Native Image — 异步层收束: SpringApplicationAotProcessor 与 RuntimeHints — Boot 的 AOT 处理管线(s21 AOT 机制复用)。
