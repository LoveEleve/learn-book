# S2-3-1 Multicaster 广播引擎 — 注册→检索→分发全链路

> 依赖 S2-2 | 🟡 Working | 3 KP | [模式: 观察者模式]

**读者处境**: S2-2 理解了 @Configuration 如何被解析成 BeanDefinition — 但容器启动的最后几步: refresh() Step 8/10/11 的 Multicaster 和 Listener 是 Spring 的"神经系统"。publishEvent() 从发布者到 listener 的完整路径是怎样的？

### 1. SimpleApplicationEventMulticaster.multicastEvent — sync/async 双模式的广播开关

场景: `context.publishEvent(new MyEvent(this))` → AbstractApplicationContext 委托给 multicaster → `multicastEvent(event)` — 从这一刻起，事件进入广播管线。如果 multicaster 配置了 TaskExecutor，listener 在独立线程执行——但事务上下文、类加载器上下文不传播。

源码路径:
- `SimpleApplicationEventMulticaster.java:142-157` — **multicastEvent(event, eventType)**: ①L143 resolveType(若eventType=null→ResolvableType.forInstance) ②L144 executor=getTaskExecutor()(循环外一次) ③L145 getApplicationListeners(委托父类) ④L146-153 遍历listener: 异步双条件 `executor!=null && listener.supportsAsyncExecution()` → executor.execute(λ) / RejectedExecutionException→fallback同步；同步路径 → invokeListener
- `SimpleApplicationEventMulticaster.java:167-179` — **invokeListener**: ErrorHandler分支: errorHandler非null→try{doInvokeListener}catch→errorHandler.handleError(err)→异常消化→后续listener继续；errorHandler=null→异常传播→后续listener中断
- `AbstractApplicationEventMulticaster.java:55,58` — **taskExecutor + errorHandler**: 字段定义在父类(SimpleApplicationEventMulticaster 继承)— taskExecutor=null→同步(默认)；errorHandler=null→无容错

关键设计: **Why 异步双条件而非单条件？** `executor!=null` 是全局开关，`supportsAsyncExecution()` 是**listener级别的选择退出**。有些 listener(如 @TransactionalEventListener)需要在原事务上下文中执行——不能异步——调用方必须能声明"我不要异步"。如果只有全局开关，所有 listener 都是同一个策略——失去灵活性。[模式: 策略模式 — TaskExecutor实现异步策略切换]

数据流: publishEvent(MyEvent)→AbstractApplicationContext.publishEvent(L421)→L443-448 先解析 eventType(ResolvableType.forInstance)→L455 以**非 null** eventType 调用 multicastEvent(applicationEvent, eventType)(SimpleApplicationEventMulticaster:142 的 L143 null 回退分支在标准路径不触发)→L144 executor=getTaskExecutor()(Spring 默认 null)→L145 getApplicationListeners(event, type)→返回排序后的listeners→遍历: listener.supportsAsyncExecution()→false(默认)→invokeListener→errorHandler=null→doInvokeListener→listener.onApplicationEvent(event)→业务 listener

### 2. AbstractApplicationEventMulticaster.getApplicationListeners — 两阶段过滤+缓存

场景: multicastEvent 调用 `getApplicationListeners(event, type)` — 容器中有 50 个 listener，但只有 3 个监听 ContextRefreshedEvent — 如何快速筛选？两阶段过滤: 先查缓存→缓存不命中→遍历所有 listener+泛型匹配+排序→缓存结果。

源码路径:
- `AbstractApplicationEventMulticaster.java:192-218` — **getApplicationListeners()**: ①ListenerCacheKey=(eventType,sourceType)(L194)→②retrieverCache.get(cacheKey)快速路径(L200)→③缓存不命中→**先** putIfAbsent(L207) 创建空条目占位(无锁竞争, 并发时后到者放弃填充)→④retrieveApplicationListeners(L223) 填充条目 — 缓存安全条件: ClassUtils.isCacheSafe(防止不同ClassLoader冲突)(L203-205)
- `AbstractApplicationEventMulticaster.java:234-324` — **retrieveApplicationListeners()**: ①synchronized(defaultRetriever)快照复制→释放锁②**阶段1**: 遍历applicationListeners→supportsEvent(listener, eventType, sourceType)→筛选③**阶段2**: 遍历applicationListenerBeans→supportsEvent(beanFactory, beanName, eventType)早期过滤(不实例化)→getBean→二次supportsEvent→singleton存filteredListeners / prototype存filteredListenerBeans④L316 AnnotationAwareOrderComparator.sort(allListeners)→返回
- `AbstractApplicationEventMulticaster.java:69-71` — **defaultRetriever + retrieverCache**: defaultRetriever(LinkedHashSet) = 注册中心; retrieverCache(ConcurrentHashMap 64) = 按类型缓存

关键设计: **Why 两阶段遍历而非单集合？** applicationListeners(直接引用)和applicationListenerBeans(beanName)生命周期不同——前者是程序注册的(注册时已实例化)，后者是延迟实例化(广播时才getBean)。beanName路径可以做**早期泛型过滤不实例化**(查BeanDefinition)→避免实例化不需要的listener。这是性能优化——一个大容器可能注册了100个beanName listener但某一事件只需2个。

数据流: multicastEvent→getApplicationListeners(event, ResolvableType)→L194 ListenerCacheKey(eventType, sourceType)→L200 retrieverCache.get(cacheKey)→null(首次)→L207 putIfAbsent先占位(空 CachedListenerRetriever)→L223 retrieveApplicationListeners→L243 synchronized(defaultRetriever): 快照复制listeners+listenerBeans(L244-245)→L250-257 阶段1: for listener in applicationListeners: supportsEvent(listener, eventType, sourceType)→匹配→allListeners.add→L261-314 阶段2: for beanName in applicationListenerBeans: supportsEvent(beanFactory, beanName, eventType)查泛型(:265)→通过→getBean→二次supportsEvent→L286 !allListeners.contains去重(:288 是 isSingleton 判断)→L316 sort→填充retriever缓存(:317-325)→返回listeners

### 3. addApplicationListener — Proxy去重+缓存失效的注册管线

场景: `context.addApplicationListener(myListener)` — 如果 myListener 是一个 AOP Proxy(被 @Transactional 增强过)，Spring 怎么防止 "proxy 和 target 都触发"？为什么注册变更后缓存要全部清空？

源码路径:
- `AbstractApplicationEventMulticaster.java:106-115` — **addApplicationListener()**: ①synchronized(defaultRetriever)锁注册中心(非锁this)②L110 AopProxyUtils.getSingletonTarget(listener)→取真实target③若target已在defaultRetriever中存在→remove(target)(先移除)④add(proxy)(后添加proxy)→⑤retrieverCache.clear()(所有缓存失效)
- `AbstractApplicationEventMulticaster.java:120-124` — **addApplicationListenerBean()**: 按beanName注册→同样触发retrieverCache.clear()
- `AbstractApplicationEventMulticaster.java:128-140` — **remove/removeBean**: 同样同步+清除缓存

关键设计: **Why Proxy去重而非简单判断？** AOP Proxy 和其 target 可能**同时**注册为 ApplicationListener — 如果 proxy 和 target 在容器启动时都被 ApplicationListenerDetector 注册到 defaultRetriever 中 — 同一个逻辑事件会执行两次(一次 proxy，一次 target)。getSingletonTarget 取真实target→先移除再添加proxy→确保不管谁先注册谁后注册，最终只有proxy留在注册表中。**而缓存必须清空的原因是 listener集合变了—之前(pre-proxy)缓存的检索结果不再有效。**

数据流: context.addApplicationListener(proxyListener)→SimpleApplicationEventMulticaster.addApplicationListener(proxyListener)→L107 synchronized(defaultRetriever)→L110 AopProxyUtils.getSingletonTarget(proxy)→返回真实target→L111 singletonTarget instanceof ApplicationListener→L112 remove(target)→L114 add(proxy)→L115 retrieverCache.clear()→所有按类型的缓存失效→下一次multicastEvent时retrieveApplicationListeners重新过滤

→ 引出篇2: @EventListener 注解怎么被自动扫描并注册为 ApplicationListener — 不需要手动 addApplicationListener。
