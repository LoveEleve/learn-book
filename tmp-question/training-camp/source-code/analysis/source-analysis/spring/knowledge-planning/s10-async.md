# S2-10 @Async — @EnableAsync→BPP→代理→TaskExecutor 异步调用链

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 6文件/~1156行
> 基线: S2-2 @Configuration — @EnableAsync 通过 @Import(AsyncConfigurationSelector) 注册 BPP → 与 Configuration 类似

---

## §0.8

- 🟡 Working，1篇 — @EnableAsync→AsyncAnnotationBPP→AOP代理(AsyncExecutionInterceptor)→TaskExecutor→返回值(Future/CompletableFuture)
- 设计模式: [模式: 代理模式]—AOP代理拦截@Async方法; [模式: 策略模式]—不同返回值类型走不同doSubmit分支
- 与 @Configuration CGLIB 代理的区别: @Async 是 AOP代理(方法级)，@Configuration 是 CGLIB 子类(类级)

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| EnableAsync.java:216行 | @EnableAsync | @Import(AsyncConfigurationSelector)→ProxyAsyncConfiguration→@Bean AsyncAnnotationBPP → AsyncConfigurer(自定义Executor+ExceptionHandler) | High |
| AsyncAnnotationBeanPostProcessor.java:156行 | postProcessAfterInitialization | **AOP代理创建**: ①buildAdvice→AsyncExecutionInterceptor ②createProxy→ProxyFactory→JdkDynamicAopProxy/CglibAopProxy ③AbstractAdvisingBeanPostProcessor模板: advisor.isEligible→ProxyFactory.getProxy | High |
| AsyncExecutionInterceptor.java:168行 | invoke() | **核心拦截**: ①L106 determineAsyncExecutor(方法@Async注解value)→TaskExecutor ②L136 Callable封装原调用 ③L128 doSubmit(task, executor, returnType)→CompletableFuture/Future/void | High |
| AsyncExecutionAspectSupport.java:334行 | determineAsyncExecutor() | **Executor选择**: ①@Async("executorName")→beanFactory.getBean(executorName) ②无qualifier→defaultExecutor ③都无→beanFactory.getBean(TaskExecutor.class) ④都无→beanFactory.getBean(DEFAULT_TASK_EXECUTOR_BEAN_NAME="taskExecutor") | High |
| AsyncExecutionInterceptor.java:doSubmit() | 返回值三路分发 | ①CompletableFuture→直接返回(调用方自行get) ②ListenableFuture(废弃)→executor.submitListenable ③其他(Future)/void→executor.submit→SimpleAsyncTaskExecutor.newThread | High |

---

## 02-04 聚合+分类+聚类

### 聚合

**P1 核心 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | AsyncExecutionInterceptor.invoke→determineAsyncExecutor→doSubmit 异步调用链 | 🔴 | **为什么🔴**: @Async的"发动机"——拦截方法→选择执行器→提交异步任务——三步骤缺一不可 |
| P1-2 | AsyncAnnotationBeanPostProcessor.postProcessAfterInitialization — AOP代理创建 | 🔴 | **为什么🔴**: @Async如何生效——BPP在@PostConstruct之后、@Autowired之后——为标有@Async的bean创建代理——此BPP是AbstractAdvisingBeanPostProcessor子类(与@Transactional的BPP是同类) |
| P1-3 | Executor选择链四层fallback | 🔴 | **为什么🔴**: @Async("executorName")→defaultExecutor→TaskExecutor.class bean→"taskExecutor" bean——四层兜底确保@Async总能找到Executor |

**P2 支持 (1):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | doSubmit 返回值三路分发(CompletableFuture/ListenableFuture/void) | 🟡 | **为什么🟡**: 不同返回值类型对应不同异步模式——影响调用方如何等待结果——但不改变核心异步调用链 |

### 聚类 (1篇)

**1篇理由**: ~1156行/6文件 — @EnableAsync注册→BPP→AOP代理→Interceptor→doSubmit — 五层完整调用链。1篇(~45行)覆盖接口→代理→执行→异常。

**单篇结构**: §1 @EnableAsync→BPP注册 + AOP代理创建 → §2 AsyncExecutionInterceptor三步骤 + Executor选择链 → §3 返回值类型 + 异常处理(AsyncUncaughtExceptionHandler)
