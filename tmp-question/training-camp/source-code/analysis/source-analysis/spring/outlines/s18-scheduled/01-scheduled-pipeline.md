# S2-11 @Scheduled — cron/fixedDelay/fixedRate 定时任务三策略

> 依赖 S2-10 @Async | 🟡 Working | 3 KP | [模式: 策略模式]

**读者处境**: @Async = 调用时异步 — @Scheduled = 定时自动触发。两者都是 Scheduling BPP 的产物，但调度策略完全不同。fixedRate vs fixedDelay 有什么区别？cron 表达式如何被解析和执行？

### 1. @EnableScheduling → ScheduledAnnotationBPP — 为什么是 SmartInitializingSingleton

场景: `@EnableScheduling` → @Import(SchedulingConfiguration) → @Bean 注册 ScheduledAnnotationBeanPostProcessor — 这个 BPP 实现了 SmartInitializingSingleton 接口 — 在 afterSingletonsInstantiated() 中注册所有 @Scheduled 任务 — 之所以不用 postProcessAfterInitialization 直接注册是因为 TaskScheduler 可能尚未就绪。

源码路径:
- `EnableScheduling.java:203` — **@Import(SchedulingConfiguration.class)**: SchedulingConfiguration @Bean 返回 ScheduledAnnotationBeanPostProcessor — BPP 实现 SmartInitializingSingleton + DestructionAwareBeanPostProcessor(容器销毁时取消所有 ScheduledTask)
- `ScheduledAnnotationBeanPostProcessor.java:239` — **afterSingletonsInstantiated()**: ①L241 清空 nonAnnotatedClasses 缓存 ②非 ApplicationContext 环境→提前 finishRegistration()。真正的注册在 finishRegistration()(L249): 已注入过 TaskScheduler→registrar.setScheduler；否则→new TaskSchedulerRouter(L254)→registrar.setTaskScheduler(L257)→收集所有 SchedulingConfigurer 并 configureTasks→L269 registrar.afterPropertiesSet()。缺省 SingleThreadScheduledExecutor 的创建在 ScheduledTaskRegistrar.scheduleTasks()(L429-432: Executors.newSingleThreadScheduledExecutor())，不在 BPP 内；registrar 由 BPP 构造器创建(L168)，不存在"未配置 Registrar→创建"
- `ScheduledAnnotationBeanPostProcessor.java:283` — **postProcessAfterInitialization()**: 跳过 AopInfrastructureBean/TaskScheduler/ScheduledExecutorService 自身(L285-287)→MethodIntrospector.selectMethods 扫描 @Scheduled 方法(L293)→无注解→加入 nonAnnotatedClasses 缓存(L300)；有→processScheduled(L336)→processScheduledTask(L405)→registrar.scheduleCronTask/scheduleFixedDelayTask/scheduleFixedRateTask。taskScheduler 有无的判断在 Registrar 内部(ScheduledTaskRegistrar.java:522/L551)，不在此方法——不区分"立即注册/等待"

关键设计: **Why afterSingletonsInstantiated 而非 postProcessAfterInitialization 注册任务？** TaskScheduler 由另一个 @Bean 提供(ThreadPoolTaskScheduler 或自定义) — 在 Bean B 的 postProcessAfterInitialization 时 TaskScheduler 可能还没创建 — 无法注册任务。SmartInitializingSingleton 在所有 singleton 创建后触发 — 此时 TaskScheduler 必然已就绪。**这与 S2-3 EventListenerMethodProcessor 用 SmartInitializingSingleton 同理 — 等待依赖 Bean 就绪。**

数据流: @EnableScheduling→@Import(SchedulingConfiguration)→@Bean ScheduledAnnotationBPP(构造器 L168 创建 registrar=new ScheduledTaskRegistrar())→refresh()Step6 registerBeanPostProcessors→BPP注册→Step11 每个Bean调用postProcessAfterInitialization→find @Scheduled methods→processScheduled→registrar.scheduleCronTask等→此刻 taskScheduler=null→任务被暂存(ScheduledTaskRegistrar.scheduleCronTask L524-527: addCronTask+unresolvedTasks.put)→所有singleton创建完毕→BPP.afterSingletonsInstantiated→finishRegistration(L249)→new TaskSchedulerRouter(L254)→registrar.setTaskScheduler→L269 registrar.afterPropertiesSet()→scheduleTasks()→遍历暂存的 tasks 重新 scheduleCronTask/scheduleFixedRateTask/scheduleFixedDelayTask；若仍未设置任何 scheduler→L430 创建 SingleThreadScheduledExecutor 兜底→返回

### 2. @Scheduled 三策略 — cron vs fixedDelay vs fixedRate

场景: `@Scheduled(cron="0 0 9 * * MON")` 每周一9:00执行 — `@Scheduled(fixedDelay=5000)` 上次方法完成后等5秒再执行 — `@Scheduled(fixedRate=5000)` 上次方法开始后5秒启动下次(如果上次执行超过5秒→等待完成→不并发)。三种策略适用于不同业务: cron=日历驱动(日报) — fixedDelay=防堆积(处理后等间隔) — fixedRate=固定节奏(心跳检测)。

源码路径:
- `Scheduled.java:84-168` — **@Scheduled 注解**: cron(116)→CronExpression; fixedRate(135)→上次开始+间隔; fixedDelay(168)→上次完成+延迟; initialDelay→首次延迟; zone→cron时区; timeUnit→时间单位
- `ScheduledAnnotationBeanPostProcessor.java:405-436` — **processScheduledTask()**: ①验证仅一个属性被设置(cron/fixedDelay/fixedRate三者只有一个非默认值—否则throw IllegalArgumentException) ②cron: L432-436 embeddedValueResolver.resolveStringValue(cron) ③fixedRate: 创建IntervalTask→registrar.addFixedRateTask ④fixedDelay: 创建IntervalTask→registrar.addFixedDelayTask
- `ScheduledTaskRegistrar.java:514/539/573` — **scheduleCronTask/scheduleFixedRateTask/scheduleFixedDelayTask**: CronTask→L522 taskScheduler.schedule(runnable, cronTrigger)→返回 ScheduledTask(含 volatile ScheduledFuture 用于取消) / IntervalTask→L551/L555 taskScheduler.scheduleAtFixedRate(runnable, interval) 或 L585/L589 scheduleWithFixedDelay→返回 ScheduledTask。注意 scheduleAtFixedRate 是 TaskScheduler 接口方法，Registrar 侧方法名是 scheduleFixedRateTask

关键设计: **Why fixedRate 不并发执行？** fixedRate 的语义是"上次开始+间隔"—如果上次执行时间>间隔→executor.scheduleAtFixedRate 会检测到任务仍在运行→等待任务完成→立即执行下次(不堆积)。这是 Java ScheduledExecutorService 的内置行为 — Spring 只是将它映射到 @Scheduled 注解。**如果业务需要并行执行→@Async + fixedRate 组合。**

数据流: Bean @Scheduled(fixedRate=10000, initialDelay=5000) → BPP.postProcessAfterInitialization→find method(processData)→processScheduled→processScheduledSync→ScheduledMethodRunnable(bean, method)→processScheduledTask→cron="" fixedDelay=-1 fixedRate=10000→FixedRateTask(runnable, 10000, 5000)→registrar.scheduleFixedRateTask(task)→taskScheduler=null→暂存(addFixedRateTask+unresolvedTasks)→afterSingletonsInstantiated→finishRegistration→registrar.afterPropertiesSet()→scheduleTasks()→重新scheduleFixedRateTask→L551 taskScheduler.scheduleAtFixedRate(runnable, startTime=now+5000ms, interval=10000ms)→返回ScheduledFuture→L477 addScheduledTask存入scheduledTasks set→线程池开始执行: T=5s→首次执行→T=15s→第二次→T=25s→第三次

### 3. ScheduledTaskRegistrar — 任务生命周期 + 延迟注册

场景: @Scheduled 方法被注册为 ScheduledTask 后 — Spring 容器需要知道哪些任务正在运行 — 容器关闭时需要取消所有 ScheduledTask(通过 Future.cancel()) — 避免线程池残留线程阻止 JVM 正常退出。

源码路径:
- `ScheduledTaskRegistrar.java:420` — **afterPropertiesSet()**: 只调 scheduleTasks()(L421/L428)；若 taskScheduler==null→L430 创建 localExecutor=Executors.newSingleThreadScheduledExecutor()+ConcurrentTaskScheduler 兜底。真正的"暂存"在 scheduleCronTask/scheduleFixedRateTask/scheduleFixedDelayTask 的 else 分支(L524-527/L558-561/L592-595): taskScheduler==null→addXxxTask(存入 List)+unresolvedTasks.put(task, scheduledTask)。Registrar 实例由 BPP 构造器创建(L168)并在容器生命周期内始终复用——不存在"afterSingletonsInstantiated 时重新创建 Registrar"
- `ScheduledTaskRegistrar.java:640` — **destroy()**: DisposableBean 生命周期方法 → L641-643 遍历 scheduledTasks→task.cancel(false) → L645-646 关闭 localExecutor。Registrar 本身不是 Bean——由 BPP.destroy()(ScheduledAnnotationBeanPostProcessor.java:649) 在容器关闭销毁 BPP(DisposableBean) 时级联调用，非 @PreDestroy

关键设计: **Why ScheduledTask 作为返回值？** scheduleCronTask 返回 ScheduledTask 对象而非 void — 让容器可以跟踪所有已调度的任务。容器关闭时遍历 scheduledTasks → 逐个 cancel → 取消所有定时任务。如果没有 ScheduledTask 对象——定时任务在后台静默运行——关闭时没有引用来 cancel —— JVM 可能因非守护线程而无法退出。

数据流: afterSingletonsInstantiated→finishRegistration(L249)→SchedulingConfigurer.configureTasks→registrar.afterPropertiesSet()(L269)→scheduleTasks()(L428)→逐个 scheduleCronTask/fixedRateTask/fixedDelayTask→L514 new ScheduledTask(task)→L522 taskScheduler.schedule(runnable, cronTrigger)→返回 ScheduledFuture→L440/L477 addScheduledTask→存入 scheduledTasks set→容器关闭→BPP.destroy()(L649)→registrar.destroy()(L640)→L641-643 for ScheduledTask in scheduledTasks→task.cancel(false)→ScheduledFuture.cancel→localExecutor.shutdownNow()→JVM正常退出

→ spring-context 第十一域完成。@EnableScheduling→BPP→三策略→Registrar→TaskScheduler 五层。引出 S2-12: @Cacheable — @EnableCaching→CacheInterceptor→CacheManager→SpEL key→缓存抽象。
