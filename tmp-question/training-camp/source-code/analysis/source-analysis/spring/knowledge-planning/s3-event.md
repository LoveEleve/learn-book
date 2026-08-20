# S2-3 事件机制 — ApplicationEventMulticaster + @EventListener

> 项目: Spring Framework 6.x | 🟡 Working / 2 篇 | 4文件/1782行
> 基线: S2-2 @Configuration — refresh() Step 8 初始化广播器 / Step 10 注册监听器

---

## §0.8

- 🟡 Working，2篇 — Multicaster 广播引擎 + @EventListener 注解驱动
- 设计模式: [模式: 观察者模式] — ApplicationEvent/ApplicationListener/Multicaster 是标准观察者; [模式: 适配器] GenericApplicationListenerAdapter 统一编程式+声明式 listener; [模式: 策略模式] sync/async通过TaskExecutor切换
- refresh()中两处调用: Step 8 `initApplicationEventMulticaster()`(创建广播器) + Step 10 `registerListeners()`(注册监听器) + Step 11 `finishRefresh()`(发布 ContextRefreshedEvent) + Step 12 `finishRefresh()`(启动 Lifecycle Bean)

---

## 01 提取

### AbstractApplicationEventMulticaster.java (532行, Agent提取49 KP)

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| AbstractApplicationEventMulticaster.java:69-71 | 字段 | **defaultRetriever**(LinkedHashSet) = 注册中心; **retrieverCache**(ConcurrentHashMap 64) = 按 (eventType,sourceType) 缓存的预筛选结果 | High |
| L192-218 | getApplicationListeners(event, eventType) | **缓存优先检索**: cacheKey=(eventType,sourceType)→retrieverCache.get→缓存命中直接返回→缓存不命中→retrieveApplicationListeners→putIfAbsent(无锁竞争) | High |
| L237-324 | retrieveApplicationListeners | **两阶段过滤+排序**: ①遍历applicationListeners+supportsEvent → ②遍历applicationListenerBeans→早期过滤(不实例化)→实例化+二次检查→AnnotationAwareOrderComparator排序→返回 | High |
| L110-113 | addApplicationListener | **Proxy去重**: AopProxyUtils.getSingletonTarget取真实target→若target已注册为先移除target再添加proxy→避免proxy和target双重调用 | High |
| L107-115 | add/removeApplicationListener | **任何注册变更→retrieverCache.clear()** — 所有缓存失效 | High |
| L265-296 | retrieveApplicationListeners beanName路径 | **早期过滤(不实例化)**: supportsEvent(beanFactory, beanName, eventType)查BeanDefinition泛型→通过后才getBean→singleton存filteredListeners / prototype存filteredListenerBeans(每次getBean) | High |
| L397-399 | supportsEvent(listener, eventType, sourceType) | **双维度检查**: GenericApplicationListener.supportsEventType + supportsSourceType → 两者都通过 | High |
| L345-358 | supportsEvent(beanFactory) 泛型匹配 | **三层泛型解析**: ①Generic/SmartApplicationListener放行(L345) ②Class级别ResolvableType检查 ③BeanDefinition级别泛型参数解析(如Listener\<ContextRefreshedEvent\>) | High |

### SimpleApplicationEventMulticaster.java (227行, Agent提取22 KP)

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| SimpleApplicationEventMulticaster.java:137-157 | multicastEvent | **核心广播**: ①resolveType→②获取TaskExecutor(循环外一次)→③遍历listener→④双条件异步(cutor非null && supportsAsyncExecution)→execute/fallback同步→⑤RejectedExecutionException降级同步 | High |
| L167-179 | invokeListener | **ErrorHandler分支**: errorHandler非null→try/doInvokeListener→catch→errorHandler.handleError(err)(消化)；errorHandler null→异常直接传播(后续listener中断) | High |
| L187-206 | doInvokeListener | **ClassCastException智能抑制**: lambda listener泛型擦除→Java8/9/11三种异常消息格式匹配→匹配成功 suppress to trace / 不匹配 rethrow | High |
| L54-58 | taskExecutor + errorHandler | **异步开关+容错策略**: taskExecutor=null→同步；errorHandler=null→无容错(异常传播) | High |

### EventListenerMethodProcessor.java (219行, Agent提取19 KP)

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| EventListenerMethodProcessor.java:afterSingletonsInstantiated() | 扫描入口 | **SmartInitializingSingleton**(非BFPP): 所有singleton创建后→遍历beanFactory→穿透AOP代理→processBean→扫描@EventListener方法 | High |
| processBean() | 扫描核心 | **nonAnnotatedClasses缓存**(ConcurrentHashMap 64)排除无注解类→MethodIntrospector扫描→双重循环(factory.supportsMethod→createApplicationListener→multicaster.addApplicationListener) | High |
| postProcessBeanFactory() | 早期获取 | 获取beanFactory→BeanFactoryResolver→EventListenerFactory排序(AnnotationAwareOrderComparator)→取最高优先级factory | High |

### ApplicationListenerMethodAdapter.java (551行, Agent提取35 KP)

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| ApplicationListenerMethodAdapter.java:processEvent() | @EventListener执行 | **五阶段管线**: ①resolveArguments(参数解析+Payload解包)→②shouldHandle(SpEL condition求值)→③doInvoke(反射调用)→④handleResult(三通道:Reactive/CompletionStage/publishEvents) | High |
| resolveArguments() | 参数解析 | PayloadApplicationEvent解包(detectPayloadType)→0参数→BeansException->return null(事件监听不抛异常) | High |
| shouldHandle() | SpEL条件 | EventExpressionEvaluator.condition(event, condition, targetMethod, bean, args) | High |
| handleResult() | 三通道 | Reactive Streams(订阅EventPublicationSubscriber)→CompletionStage→publishEvents(数组/Collection/单对象) | High |
| resolveDeclaredEventTypes() | 事件类型推断 | @EventListener.classes显式声明优先→否则从方法参数类型推断→Kotlin协程参数排除 | High |

---

## 02-04 聚合+分类+聚类

### 聚合 (P1≥5 / P2 2-4 / P3 1)

**P1 核心机制 (5) — 每篇大纲至少1节的独立机制:**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | SimpleApplicationEventMulticaster.multicastEvent — sync/async双模式广播+TaskExecutor+ErrorHandler | 🔴 | **为什么🔴**: 事件机制的"发动机"——所有Java事件最终都经这里分发。sync/async/容错三个维度同时作用，缺它事件无法广播 |
| P1-2 | AbstractApplicationEventMulticaster.getApplicationListeners+retrieveApplicationListeners — 两阶段过滤+缓存+排序 | 🔴 | **为什么🔴**: 回答"怎么找到正确的listener"——泛型匹配+beanName延迟实例化+缓存策略是多播器的核心检索逻辑 |
| P1-3 | AbstractApplicationEventMulticaster.add/removeApplicationListener — Proxy去重+缓存失效 | 🔴 | **为什么🔴**: 回答"怎么注册listener"——Proxy/Target去重防止双重调用+缓存失效机制是正确性的保障 |
| P1-4 | EventListenerMethodProcessor.afterSingletonsInstantiated — @EventListener扫描+MethodIntrospector+EventListenerFactory | 🔴 | **为什么🔴**: 回答"@EventListener怎么被发现"——所有singleton创建后扫描注解→通过EventListenerFactory创建适配器→自动注册到multicaster |
| P1-5 | ApplicationListenerMethodAdapter.processEvent — @EventListener五阶段执行管线(SpEL→参数→调用→结果) | 🔴 | **为什么🔴**: 回答"@EventListener怎么执行"——5阶段管线从condition求值到结果处理是注解驱动事件的核心 |

**P2 支持机制 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | supportsEvent三路重载(BeanFactory/Class/instance) — 三层泛型解析 | 🟡 | **为什么🟡**: 类型匹配基础设施——P1-2检索listener时调用——逻辑自含但影响listener匹配精确度 |
| P2-2 | retrieverCache+CachedListenerRetriever+ListenerCacheKey — 缓存基础设施 | 🟡 | **为什么🟡**: 缓存优化——P1-2的辅助——它加速listener检索但检索逻辑(过滤/匹配)不依赖缓存 |
| P2-3 | ApplicationListenerMethodAdapter.handleResult三通道(Reactive/CompletionStage/publishEvents) | 🟡 | **为什么🟡**: 结果处理变体——3通道影响执行语义但不影响事件分发主线 |

**P3 辅助机制 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P3-1 | doInvokeListener ClassCastException lambda 擦除智能抑制(Java 8/9/11) | 🟢 | **为什么🟢**: 边缘兼容——lambda listener类型擦除导致的不匹配→三种Java版本的异常消息格式识别→仅trace不抛异常 |
| P3-2 | multicastEvent RejectedExecutionException 降级同步 | 🟢 | **为什么🟢**: 边缘恢复——线程池关闭时的fallback→保证事件不丢失 |
| P3-3 | Kotlin 协程参数处理 + NullBean 检测 | 🟢 | **为什么🟢**: 边缘适配——不影响Java主要场景 |

### 聚类决策 (2篇)

**篇1: Multicaster 广播引擎 — 注册→检索→分发全链路**

覆盖: P1-1(multicastEvent) + P1-2(getApplicationListeners) + P1-3(add/remove) + P2-1(supportsEvent) + P2-2(retrieverCache) + P3-1+P3-2

**为什么2篇不拆更多?** Multicaster 这3个P1紧密耦合——注册(P1-3)→检索(P1-2)→分发(P1-1)形成完整闭环。支持机制(P2-1/P2-2)和辅助(P3-1/P3-2)都在同一上下文中——拆开会打断数据流主线。2个P1核心+2个P2支持组成围绕Multicaster的完整故事。

**篇2: @EventListener 注解驱动 — 扫描→适配→执行**

覆盖: P1-4(EventListenerMethodProcessor扫描) + P1-5(Adapter五阶段执行) + P2-3(handleResult三通道) + P3-3(Kotlin)

**为什么单独成篇?** @EventListener 与编程式 ApplicationListener 是完全不同的注册路径——前者通过 SmartInitializingSingleton 后扫描+EventListenerFactory创建适配器,后者通过编程式 addApplicationListener。两篇从"广播怎么工作"到"注解怎么被发现"形成互补——读者先理解基础观察者模式再理解注解增强。
