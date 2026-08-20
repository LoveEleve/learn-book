# S-19 篇② 启动运行时 — 虚拟线程开关 (spring.threads.virtual.enabled)

> 依赖 C-7 / S-14 (TaskExecutor) | 🟡 Working | 3 KP | [模式: 枚举开关 + 条件配置 + 策略选择]

**读者处境**: 配 `spring.threads.virtual.enabled=true` 后, @Async 的任务线程从哪来?虚拟线程和平台线程执行器差在哪?为什么默认关?— 开关到执行器选择的链路。

### 1. 开关 — Threading 枚举与属性

场景: 一个布尔属性 `spring.threads.virtual.enabled` 怎么变成一个"线程模型"决策?

源码路径:
- `Threading.java:28,48` — **枚举**: `enum Threading { PLATFORM, VIRTUAL }`(L28); `VIRTUAL.isActive(environment)`(L48): `spring.threads.virtual.enabled == true && Java >= 21` — 两个条件
- `Threading.java:36` — **PLATFORM.isActive**: `!VIRTUAL.isActive(environment)`(L36) — 默认平台线程
- 属性默认: `getProperty("spring.threads.virtual.enabled", boolean.class, false)`(L49) — 默认 false

关键设计: **Why 属性 + Java 版本双条件？** 虚拟线程是 Java 21 特性 — 仅属性为 true 还不够, 运行时必须 Java≥21, 否则虚拟线程 API 不可用; 双条件保证安全降级(旧 JDK 配了也不生效)。**Why 默认 false？** 虚拟线程非池化、模型差异大, 默认保守用平台线程。[模式: 枚举 + 条件判断]

数据流: 配置 spring.threads.virtual.enabled=true + 跑在 JDK21 → Threading.VIRTUAL.isActive(environment)(L48)→ true → 各处条件判断(§2)选择虚拟线程路径; 否则 PLATFORM.isActive→true。

### 2. 条件接线 — @ConditionalOnThreading 选执行器

场景: 开关怎么传导到"创建哪个执行器 Bean"?@Async 用的 applicationTaskExecutor 怎么变?

源码路径:
- `ConditionalOnThreading.java:37,44` — **注解**: `@Conditional(OnThreadingCondition.class)`(L37) + `Threading value()`(L44)
- `OnThreadingCondition.java:36,45` — **条件**: getMatchOutcome(L36) → `threading.isActive(environment)`(L45) — 命中与否由 §1 决定
- `TaskExecutorConfigurations.java:61,68` — **双分支**: `@ConditionalOnThreading(Threading.VIRTUAL)`(L61) → `applicationTaskExecutorVirtualThreads`(L62): `SimpleAsyncTaskExecutor`(builder.virtualThreads(true), L130); `@ConditionalOnThreading(Threading.PLATFORM)`(L68) → `applicationTaskExecutor`(L69): `ThreadPoolTaskExecutor`(池化, S-14 内核)

关键设计: **Why 条件注解而非 if？** 与 Boot 条件评估体系统一(S-3 @ConditionalOnXxx 同引擎); 两个 @Bean 互斥, 由条件决定哪个激活 — 虚拟开则建 SimpleAsyncTaskExecutor(virtualThreads), 否则建 ThreadPoolTaskExecutor。**Why 互斥双 Bean？** 容器里 applicationTaskExecutor 只有一个, 条件保证二选一, 无需运行时判断。[模式: 条件配置 + 互斥 Bean]

数据流: Threading.VIRTUAL.isActive→true → OnThreadingCondition.getMatchOutcome(环境)(L45) 命中 → TaskExecutorConfigurations 的 virtualThreads 分支(L61)激活 → builder.virtualThreads(true)(L130) → SimpleAsyncTaskExecutor(虚拟线程, 每任务一线程)。

### 3. 差异与边界 — 虚拟 vs 平台执行器

场景: 选完后, 虚拟线程的 SimpleAsyncTaskExecutor 和默认的 ThreadPoolTaskExecutor 行为差在哪?

源码路径:
- `TaskExecutorConfigurations.java:68,69` — **平台默认**: ThreadPoolTaskExecutor — 池化复用线程(S-14/C-7 已深析)
- `TaskExecutorConfigurations.java:61,62,129,130` — **虚拟**: SimpleAsyncTaskExecutor + `virtualThreads(true)`(L130) — 不池化, 每个任务 new 一个虚拟线程
- `Threading.java:36,48` — **互斥**: 二选一, 不会同时存在

关键设计: **Why 虚拟线程不池化？** 虚拟线程开销远低于平台线程(创建/切换成本低, 数量级远超平台线程), 池化反而多余 — SimpleAsyncTaskExecutor 每个任务直接建虚拟线程执行, 阻塞成本低; 平台线程则必须池化以复用有限线程。**Why 与 S-14 边界？** ThreadPoolTaskExecutor 的池化内核在 C-7/S-14 已讲, 本域只讲"开关怎么选到它/或选到虚拟版" — 复用内核, 展开切换。[模式: 策略选择 + 复用边界]

数据流: applicationTaskExecutor(虚拟版)=SimpleAsyncTaskExecutor(virtualThreads) → @Async 提交任务 → 每任务 newVirtualThread 执行 → 完成。平台版 = ThreadPoolTaskExecutor → 从池取线程复用(内核见 C-7/S-14)。

→ 引出 S-20: WebFlux+Netty — 运行时收束后: 响应式服务器工厂 ReactiveWebServerFactory 的自动装配(进入 Actuator/测试层)。
