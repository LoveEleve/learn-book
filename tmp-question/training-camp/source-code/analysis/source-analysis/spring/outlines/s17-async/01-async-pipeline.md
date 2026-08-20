# S2-10 @Async — AOP 代理拦截 + TaskExecutor 异步调用链

> 依赖 S2-2 @Configuration + S2-6 BPP全景 | 🟡 Working | 3 KP | [模式: 代理模式]

**读者处境**: 前面学了 BeanPostProcessor 全景 — @Async 是 BPP 的典型应用。@EnableAsync 注册一个 BPP → BPP 在 initializeBean 后扫描 @Async 方法 → 创建 AOP 代理 → 代理拦截方法调用 → 提交到 TaskExecutor 异步执行。

### 1. @EnableAsync → AsyncAnnotationBPP — BPP 注册 + AOP 代理创建

场景: `@EnableAsync` 在 @Configuration 类上 → @Import(AsyncConfigurationSelector) → PROXY 模式固定返回 ProxyAsyncConfiguration(JDK/CGLIB 由运行时 ProxyFactory 决定, 选择器不做分支) → @Bean 注册 AsyncAnnotationBeanPostProcessor → BPP 在 Step 6 registerBeanPostProcessors 被注册 → Step 11 每个 bean 创建后调 postProcessAfterInitialization → 找到 @Async 方法 → 创建 AOP 代理。

源码路径:
- `EnableAsync.java:167-168` — **@EnableAsync**: L167 @Import(AsyncConfigurationSelector) → mode()=PROXY(默认)/ASPECTJ → PROXY 模式固定选 ProxyAsyncConfiguration(AsyncConfigurationSelector.java:47-50, 无 JDK/CGLIB 分支; proxyTargetClass 默认 false, EnableAsync.java:193) → @Bean AsyncAnnotationBPP → AsyncConfigurer(自定义Executor+ExceptionHandler)
- `AsyncAnnotationBeanPostProcessor.java:148` — **代理创建入口**: L148 `new AsyncAnnotationAdvisor(this.executor, this.exceptionHandler)` — 实际代理创建在父类 AbstractAdvisingBeanPostProcessor.postProcessAfterInitialization(L88-110): ①`isEligible(beanClass)` 判断需代理(L95)②`ProxyFactory proxyFactory = prepareProxyFactory(bean, beanName)`(L114 调用, 声明在 L188)③`proxyFactory.addAdvisor(this.advisor)`(L72)④`proxyFactory.getProxy(classLoader)`(L128); interceptor 由 advisor 内部 buildAdvice(AsyncAnnotationAdvisor.java:159-164) 组装: new AnnotationAsyncExecutionInterceptor(L162)+configure(L163)
- `AbstractAdvisingBeanPostProcessor.java:88` — **模板**: postProcessAfterInitialization(L88) → isEligible(L95) → prepareProxyFactory(L114 调用/L188 声明) → ProxyFactory.getProxy → 返回代理

关键设计: **Why @Async 用 AOP 代理而非 CGLIB 子类增强(像@Configuration)？** @Configuration 需要拦截类内方法间调用(dataSource()在transactionManager()内被调)→需要 CGLIB 子类拦截 this.xxx()。@Async 只需要拦截外部对 @Async 方法的调用(Controller 调 Service[@Async])→AOP 代理(AOP Alliance MethodInterceptor)即可——不需要子类拦截 this 调用。**CGLIB 子类=类内调用拦截——AOP 代理=外部调用拦截——@Async属于后者。**

数据流: @EnableAsync→@Import(AsyncConfigurationSelector)→PROXY→ProxyAsyncConfiguration→@Bean AsyncAnnotationBeanPostProcessor(executor, exceptionHandler)→refresh()Step 6 registerBeanPostProcessors→BPP注册→Step 11 finishBeanFactoryInitialization→getBean("emailService")→createBean→initializeBean→applyBeanPostProcessorsAfterInitialization→AsyncAnnotationBPP.postProcessAfterInitialization(L88)→new AsyncAnnotationAdvisor(L148)→find @Async methods(sendEmail)→isEligible(beanClass)(L95)→true→prepareProxyFactory(L114)→addAdvisor(L72)→getProxy(L128)→返回EmailService$$SpringCGLIB代理→Registration→完成→Controller.emailService.sendEmail()→CGLIB代理拦截→AsyncExecutionInterceptor.invoke→异步执行

### 2. AsyncExecutionInterceptor — determineAsyncExecutor + doSubmit 三步骤

场景: Controller 调 `emailService.sendEmail()`(标有 @Async) → CGLIB 代理拦截 → AsyncExecutionInterceptor.invoke() → ①确定用哪个 TaskExecutor → ②封装原调用为 Callable → ③提交到 Executor 异步执行 → Controller 立即返回(不等待 sendEmail 完成)。

源码路径:
- `AsyncExecutionInterceptor.java:106-128` — **invoke()**: ①L106 `determineAsyncExecutor(userMethod)`→选择Executor(为null则抛 IllegalStateException, L107-109) ②L112-126 `Callable<Object> task = () -> {...invocation.proceed()...}` 封装调用 ③L128 `doSubmit(task, executor, userMethod.getReturnType())`→提交到线程池
- `AsyncExecutionAspectSupport.java:171-190` — **determineAsyncExecutor()**: ①@Async("executorName")→`findQualifiedExecutor`(L180)→`BeanFactoryAnnotationUtils.qualifiedBeanOfType(beanFactory, Executor.class, qualifier)`(L222) ②无qualifier→`this.defaultExecutor.get()`(L183, SingletonSupplier 单例缓存) — getDefaultExecutor 找不到时**返回 null**(L237-238), invoke 中 executor==null 直接抛 IllegalStateException(L107-109) — **"四层兜底确保总能找到"不成立**
- `AsyncExecutionAspectSupport.java:286-302` — **doSubmit()**(定义于父类, 非 AsyncExecutionInterceptor) — **四路分发**: ①CompletableFuture→`executor.submitCompletable(task)`(L287-288, 任务照常提交, 不是"直接返回") ②ListenableFuture→`submitListenable`(L290-291) ③Future→`executor.submit(task)`(L293-294) ④void/kotlin.Unit→`executor.submit(task)`(L296-297) — 其它返回类型抛 IllegalArgumentException(L300-302)

关键设计: **Why Executor 选择是"两级 + 框架兜底"而非四层？** ①指定名称: `@Async("emailExecutor")`→qualifiedBeanOfType 按类型(Executor.class)+限定符查找 ②未指定名称→defaultExecutor(SingletonSupplier 缓存): getDefaultExecutor 先按类型找唯一 TaskExecutor(L244)、再按名称 "taskExecutor"(L250/264)、都没有→返回 null — 此时 AsyncExecutionInterceptor 再兜底 new SimpleAsyncTaskExecutor()(L158-160, **框架级兜底, 非 Spring Boot 默认** — Boot 的默认异步执行器是 auto-configuration 创建的 ThreadPoolTaskExecutor)。两级保证: 指定名称时用指定 Executor — 未指定时优先容器默认; 兜底仍找不到则抛 IllegalStateException, 不会静默改用无界线程池。

数据流: Controller.emailService.sendEmail(user)→AOP代理→AsyncExecutionInterceptor.invoke(MethodInvocation)(L104-106)→L106 determineAsyncExecutor→@Async value=""→无qualifier→L183 defaultExecutor.get()→getDefaultExecutor(L238-264): beanFactory.getBean(TaskExecutor.class)(L244)→无→getBean("taskExecutor")(L250)→无→返回null→L158-160 兜底 new SimpleAsyncTaskExecutor(**框架级兜底, 非 Boot 默认**)→L112-126 Callable = () -> {invocation.proceed()...}→L128 doSubmit(task, executor, void.class)→void分支 L296-297→executor.submit(task)→SimpleAsyncTaskExecutor→new Thread(task)→异步执行 sendEmail→Controller立即继续→不等待返回

### 3. 异常处理 + 返回值类型选择

场景: @Async 方法抛出 RuntimeException → Controller 不会收到异常(调用方已经返回了) → 异常被 AsyncUncaughtExceptionHandler 处理 → 默认 SimpleAsyncUncaughtExceptionHandler 打 error 日志 → 自定义 AsyncConfigurer.getAsyncUncaughtExceptionHandler() 可改为发送告警/记录DB/重试。

源码路径:
- `AsyncExecutionAspectSupport.java:318-325` — **handleError()** — **异常处理**: catch Throwable → ①方法返回类型是 Future→`ReflectionUtils.rethrowException(ex)`(L319-320) — **无 future.completeExceptionally 调用** ②否则→`this.exceptionHandler.obtain().handleUncaughtException(ex, method, params)`(L325)→AsyncUncaughtExceptionHandler——默认 SimpleAsyncUncaughtExceptionHandler 打 error 日志(L38-39) / 自定义 handler
- `AsyncExecutionAspectSupport.java:286-302` — **doSubmit() 返回值决策**: CompletableFuture→submitCompletable(L287-288) / ListenableFuture→submitListenable(L290-291) / Future→submit(L293-294) / void→submit+null(L296-297) / 其它→IllegalArgumentException(L300-302)

关键设计: **Why @Async void 方法的异常需要 UncaughtExceptionHandler？** 调用方调 @Async void 方法后立即返回—不持有 Future—无法 get() 获取异常。如果没有 ExceptionHandler→异常被线程池吞掉→Silent Failure(静默失败)→调用方永远不知道异步任务失败了。**AsyncUncaughtExceptionHandler 是 void 方法的"唯一异常出口"。**

数据流: emailService.sendEmail(user)→AOP代理→AsyncExecutionInterceptor.invoke→executor.submit→线程池执行sendEmail→sendEmail内部throw RuntimeException→Callable 内 catch(L120-123)→AsyncExecutionAspectSupport.handleError(ex, method, args)(L318)→方法返回类型 void(非Future)→L325 exceptionHandler.obtain().handleUncaughtException(ex, sendEmail, [user])→AsyncUncaughtExceptionHandler→SimpleAsyncUncaughtExceptionHandler(默认)→log.error("Unexpected exception occurred invoking async method: sendEmail", ex)→线程终止→Controller 已返回(不知道异常)

→ spring-context 第十域完成。@Async 从 @EnableAsync→BPP→AOP代理→Interceptor→Executor→异常处理 — 六层。引出 S2-11: @Scheduled — @EnableScheduling→ScheduledAnnotationBPP→TaskScheduler→cron/fixedDelay/fixedRate。
