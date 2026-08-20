# S2-11 @Scheduled — cron/fixedDelay/fixedRate 定时任务调度

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 6文件/~2082行
> 基线: S2-10 @Async — @Scheduled 与 @Async 都是 Scheduling BPP 的注解驱动——不同调度策略(定时 vs 异步)

---

## §0.8

- 🟡 Working，1篇 — @EnableScheduling→ScheduledAnnotationBPP→TaskScheduler→cron/fixedDelay/fixedRate→ScheduledTaskRegistrar
- 设计模式: [模式: 策略模式]—三种调度策略(cron/fixedDelay/fixedRate)对应不同Task; [模式: 观察者模式]—ScheduledTaskRegistrar管理所有ScheduledTask生命周期
- vs @Async: @Scheduled = 定时触发的同步执行 + @Async = 调用时异步

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| EnableScheduling.java:203 | @Import | @Import(SchedulingConfiguration)→@Bean ScheduledAnnotationBPP→BPP是SmartInitializingSingleton | High |
| ScheduledAnnotationBeanPostProcessor.java:113 | BPP | 实现DestructionAwareBeanPostProcessor+SmartInitializingSingleton→afterSingletonsInstantiated(L239)注册任务/postProcessAfterInitialization(L283)延迟注册 | High |
| ScheduledAnnotationBeanPostProcessor.java:336-405 | processScheduled→processScheduledTask | **注解解析**: ①processScheduled→sync/async分支 ②processScheduledTask→验证仅一个属性(cron/fixedDelay/fixedRate)③读取@Scheduled属性→ScheduledTaskRegistrar.scheduleCronTask/fixedDelay/fixedRate | High |
| Scheduled.java:84-168 | @Scheduled注解 | cron(116)→CronExpression; fixedRate(135)→上次开始+间隔; fixedDelay(168)→上次完成+延迟; initialDelay→首次执行延迟; zone→cron时区 | High |
| ScheduledTaskRegistrar.java:514-555 | scheduleCronTask/scheduleAtFixedRate | scheduledTask→TaskScheduler.scheduleAtFixedRate(runnable,interval)→返回ScheduledTask→addScheduledTask跟踪生命周期 | High |
| ScheduledTaskRegistrar.java:afterPropertiesSet | 延迟注册 | 若TaskScheduler尚未设置→暂存task列表→afterSingletonsInstantiated→resolveScheduler→注册所有pending tasks | High |

---

## 02-04 聚合+分类+聚类

### 聚合

**P1 核心 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | ScheduledAnnotationBPP.processScheduled→processScheduledTask 三策略路由 | 🔴 | **为什么🔴**: @Scheduled的"发动机"——从@Scheduled注解提取cron/fixedDelay/fixedRate→创建对应TaskObject→ScheduledTaskRegistrar调度——三步缺一不可 |
| P1-2 | cron vs fixedDelay vs fixedRate 三策略语义区分 | 🔴 | **为什么🔴**: 最容易混淆的三个定时策略——cron=绝对时间(每周一9:00)→fixedDelay=上次完成+间隔(不管上次执行多久)→fixedRate=上次开始+间隔(如果上次未完成→不并发执行) |
| P1-3 | ScheduledTaskRegistrar 延迟注册: afterSingletonsInstantiated→resolveScheduler→注册所有pending tasks | 🔴 | **为什么🔴**: 为什么任务注册要延迟到afterSingletonsInstantiated？TaskScheduler可能由@Bean定义→在容器启动时才可用→BPP.postProcessAfterInitialization时TaskScheduler可能还不存在→暂存task→等所有singleton就绪后统一注册 |

**P2 支持 (1):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | TaskScheduler接口: schedule/scheduleAtFixedRate/scheduleWithFixedDelay | 🟡 | **为什么🟡**: 调度器抽象——ThreadPoolTaskScheduler(Spring Boot默认)/ConcurrentTaskScheduler/自定义——不影响@Scheduled注解语义但影响并发数和精度 |

### 聚类 (1篇)

**1篇理由**: ~2082行/6文件 — BPP(679行)是核心，注解(251行)+注册器(649行)+调度器接口(248行)+配置类(48行+207行)—1篇(~45行)覆盖From @EnableScheduling→BPP注册→注解解析→三种策略→TaskScheduler调度。

**单篇结构**: §1 @EnableScheduling→ScheduledAnnotationBPP(为何是SmartInitializingSingleton) → §2 @Scheduled三策略(cron/fixedDelay/fixedRate)+BPP解析流程 → §3 ScheduledTaskRegistrar延迟注册+TaskScheduler调度
