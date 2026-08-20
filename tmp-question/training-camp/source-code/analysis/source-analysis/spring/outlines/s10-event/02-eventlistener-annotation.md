# S2-3-2 @EventListener — 注解驱动事件的扫描→适配→执行

> 依赖 S2-3-1 | 🟡 Working | 3 KP | [模式: 适配器模式]

**读者处境**: 篇1 讲了编程式 `addApplicationListener` 的注册和广播 — 但现代 Spring Boot 中我们几乎不写 `implements ApplicationListener`——而是 `@EventListener`。这个注解怎么被发现、适配和执行的？

### 1. EventListenerMethodProcessor — SmartInitializingSingleton 扫描所有 @EventListener

场景: refresh() Step 11 `finishBeanFactoryInitialization` 中所有单例 Bean 创建完成 → Spring 调用所有 `SmartInitializingSingleton` 的 `afterSingletonsInstantiated()` → EventListenerMethodProcessor 遍历 BeanFactory 中每一个 Bean → 检查其方法是否有 `@EventListener` 注解 → 通过 EventListenerFactory 创建 ApplicationListener 适配器 → 自动注册到 multicaster。

源码路径:
- `EventListenerMethodProcessor.java:112-154` — **afterSingletonsInstantiated()**: ①L115 遍历 `beanFactory.getBeanNamesForType(Object.class)`(所有bean) ②L117 跳过 `ScopedProxyUtils.isScopedTarget` 命中的内部 bean, 逐个解析 targetClass(L120) ③对每个解析出 type 的 beanName 调用 `processBean(beanName, type)`(L145) — 但**为什么不是 BeanFactoryPostProcessor？** SmartInitializingSingleton 在所有 Bean 创建后才执行 — @EventListener 方法可能引用其他 Bean(通过参数注入) — 必须等这些 Bean 都就绪才能扫描
- `EventListenerMethodProcessor.java:156-206` — **processBean()**: ①`nonAnnotatedClasses` 缓存(ConcurrentHashMap.newKeySet(64), L84)→已被标记为"无@EventListener"的类直接跳过(L157) ②`MethodIntrospector.selectMethods(targetType, annotatedMethodFilter)`(L163)→找到所有@EventListener方法 ③双重循环: 外层遍历method(L186)、内层遍历factory(L187)→`factory.supportsMethod(method)`(L188)→`AopUtils.selectInvocableMethod`(L189)→`factory.createApplicationListener(beanName, targetType, methodToUse)`(L190-191)→`context.addApplicationListener(applicationListener)`(L195)→break(L196): 找到支持的factory后跳出内层循环, 处理下一个method
- `EventListenerMethodProcessor.java:postProcessBeanFactory()`(BFPP阶段): 提前获取 beanFactory 引用 + EventListenerFactory 列表(按 @Order 排序取最高优先级) — **但这不是在此时扫描@EventListener** — 只是提前拉取依赖

关键设计: **Why SmartInitializingSingleton 而非 BeanPostProcessor？** BPP 在每个 Bean 创建时执行 — 那时其他 Bean 可能还没创建。@EventListener 方法的参数可以注入任意事件类型的 Bean — 必须等容器初始化完成后才能安全扫描所有 Bean 的方法签名。SmartInitializingSingleton 是"所有单例都创建完毕"的 hook——确保扫描时的上下文完整。[模式: 适配器模式 — EventListenerFactory 将 @EventListener 方法适配为 ApplicationListener 接口]

数据流: refresh() Step 11 finishBeanFactoryInitialization→所有singleton创建完毕→afterSingletonsInstantiated()调用→L115 beanFactory.getBeanNamesForType(Object.class)→遍历beanName→L117 ScopedProxyUtils.isScopedTarget过滤→processBean(beanName, type)(L145)→L157 nonAnnotatedClasses.contains(type)→否→L163 MethodIntrospector.selectMethods→找到@EventListener方法→L186 外层遍历method、L187 内层遍历factory: L188 factory.supportsMethod(method)→true→L190-191 factory.createApplicationListener(beanName, type, method)→返回 ApplicationListenerMethodAdapter→L195 context.addApplicationListener(adapter)→break→下一method

### 2. ApplicationListenerMethodAdapter — @EventListener 方法的五阶段执行管线

场景: 上一步扫描到的 `@EventListener` 方法被包装为 `ApplicationListenerMethodAdapter` — 当事件触发时 — adapter.processEvent() 执行五阶段管线: 解析参数 → SpEL条件求值 → 反射调用 → 结果处理。这与直接 `onApplicationEvent(event)` 完全不同 — adapter 在调用方法前先检查条件。

源码路径:
- `ApplicationListenerMethodAdapter.java:252-263` — **processEvent()**: ①L253 args=resolveArguments(event)→②L254 shouldHandle(event, args) SpEL条件→false?return→③L255 doInvoke(args)反射调用→④L259-261 result为null→`logger.trace("No result object given - no result to handle")`→⑤L257 handleResult(result)结果三通道
- `ApplicationListenerMethodAdapter.java:295-312` — **resolveArguments()**: ①L296-299 声明的事件类型解析失败→return null(本次不处理) ②L300-302 0参数→return new Object[0] ③L304-310 非ApplicationEvent声明 + 事件是PayloadApplicationEvent→L306 `getPayload()`→payload类型匹配→return new Object[] {payload} ④L311 直接 return new Object[] {event}(其余参数走 autowire, 无异常捕获逻辑)
- `ApplicationListenerMethodAdapter.java:275-286` — **shouldHandle()**: L279-280 若@EventListener声明condition→L282-283 `EventExpressionEvaluator.condition(condition, event, targetMethod, methodKey, args)`→SpEL求值
- `ApplicationListenerMethodAdapter.java:315-337` — **handleResult()**: 三通道: ①Reactive Streams(Publisher)→subscribe(ReactiveResultHandler)响应式发布(L316-320) ②CompletionStage→`whenComplete`(L322-329, 不是 thenAccept)→publishEvents ③ListenableFuture→addCallback(L331-333) ④同步→publishEvents(L334-336: 数组/Collection/单对象)→applicationContext.publishEvent()
- `ApplicationListenerMethodAdapter.java:371-401` — **doInvoke()**: ①NullBean检测→返回null ②method.setAccessible→③Kotlin协程检测→④method.invoke(target, args)反射调用→⑤三层异常转换: InvocationTargetException→unwrap; IllegalAccessException→diagnose; 其他→throw

关键设计: **Why handleResult 三通道而非统一？** @EventListener 方法的返回值有不同语义: ①普通对象→发布为新事件(publishEvent) ②Publisher(Mono/Flux)→响应式订阅(需要Reactor依赖) ③CompletionStage→异步完成回调。如果不分通道——普通返回值和响应式返回值无法通过同一接口处理——Publisher.subscribe和publishEvent是两种完全不同的执行模式。

数据流: ApplicationEventMulticaster.multicastEvent→invokeListener→doInvokeListener→adapter.onApplicationEvent(event)→adapter.processEvent(event)→L253 args=resolveArguments(event): 声明类型非ApplicationEvent且事件是PayloadApplicationEvent?→L306 getPayload→匹配则return {payload}→else→L311 args={event}→L254 shouldHandle: condition非null?→L282-283 EventExpressionEvaluator.condition(condition, event, targetMethod, methodKey, args)→SpEL: "#event.type == 'INFO'"→false→return→(下一个listener)

### 3. @EventListener vs ApplicationListener — 两种注册路径最终汇入同一个 Multicaster

场景: 容器中同时有编程式 listener(implements ApplicationListener) 和注解式 listener(@EventListener) — 两者都触发同一个事件 — 它们的注册路径不同但**最终作为 ApplicationListener 在同一个 Multicaster 中运行**。

源码路径:
- **编程式路径**: context.addApplicationListener(myListener) → AbstractApplicationEventMulticaster.addApplicationListener(L106) → 存入 defaultRetriever.applicationListeners → multicastEvent(L142)遍历 → listener.onApplicationEvent(event) 直接回调
- **@EventListener 路径**: EventListenerMethodProcessor.afterSingletonsInstantiated(L112) → processBean(L156) → MethodIntrospector扫描 → DefaultEventListenerFactory.createApplicationListener → **返回 ApplicationListenerMethodAdapter**(实现ApplicationListener) → multicaster.addApplicationListener(adapter) → 注册到**同一个 defaultRetriever** → multicastEvent 遍历 → adapter.onApplicationEvent → processEvent 五阶段管线

关键设计: **Why 不创建第二个广播器？** @EventListener 的语义仍是"监听事件" — 只是注册方式从手动 addApplicationListener 变成了自动扫描。统一 Multicaster 带来的好处: ①Order 排序统一(编程式和注解式 listener 在同一个 list 中按 @Order 排序) ②sync/async 策略统一(同一个 taskExecutor) ③ErrorHandler 容错统一。创建第二个广播器会让两种 listener 的排序和执行策略不一致 — 违反使用者直觉。

数据流: context 注册 MyListener(implements ApplicationListener) → multicaster.addApplicationListener(MyListener) → 存入 defaultRetriever → 容器扫描 AppService.@EventListener handle(MyEvent) → afterSingletonsInstantiated → processBean(AppService) → MethodIntrospector 找到 handle 方法 → DefaultEventListenerFactory.createApplicationListener → new ApplicationListenerMethodAdapter(bean, method) → adapter 实现 ApplicationListener → multicaster.addApplicationListener(adapter) → defaultRetriever 中现有 [MyListener, adapter] → publishEvent(MyEvent) → multicastEvent → getApplicationListeners → retrieveApplicationListeners → 两个都通过 supportsEvent → 按 @Order 排序 → 遍历: MyListener.onApplicationEvent → adapter.onApplicationEvent → processEvent 五阶段

→ spring-context 第三域完成。引出 S2-4: 国际化 MessageSource + ResourceBundle — refresh() Step 7 initMessageSource()。
