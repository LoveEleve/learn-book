# S1-3 §2 BeanPostProcessor 三层干预 — 实例化前/初始化前后/销毁前

> 依赖 §1 | 🔴 Deep | 2 KP | [模式: Chain of Responsibility + Interceptor]

**读者处境**: §1 讲了 doCreateBean 13 步 — initializeBean 中 `applyBeanPostProcessorsBefore/AfterInitialization` 只是 BPP 体系的一个切面。实际上 Spring 有**三层 BPP**，分别在实例化前、初始化前后、销毁前三个时机切入——理解这六层是理解 Spring AOP/@Async/@Transactional 等所有"魔法"的基础。

### 1. InstantiationAwareBeanPostProcessor — 实例化前可返回代理

场景: `@Async public void sendEmail()` — Spring 不直接实例化 UserService — 而是返回一个 CGLIB 代理 — 代理的 `sendEmail()` 方法内部用线程池异步执行。这个代理不是在 initializeBean 之后生成的 — 它更早 — 在 `createBeanInstance` **之前**! `InstantiationAwareBeanPostProcessor.postProcessBeforeInstantiation()` 可以返回一个非 null 对象 — Spring 直接使用这个对象跳过后续所有实例化步骤。

源码路径:
- `AbstractAutowireCapableBeanFactory.java:1166` — **applyBeanPostProcessorsBeforeInstantiation()**: 遍历所有 BPP 调 `postProcessBeforeInstantiation(beanClass, beanName)` — 若返回非null→**跳过 doCreateBean**→直接走 `applyBeanPostProcessorsAfterInitialization`
- `AbstractAutoProxyCreator` — 实现 `InstantiationAwareBeanPostProcessor` — `postProcessBeforeInstantiation()` 检查是否需要提前生成代理 — `TargetSource` 需要自定义实例化逻辑时这里返回代理
- `InstantiationAwareBeanPostProcessor.postProcessAfterInstantiation()` — 在 `populateBean` 方法内部开头被调用(AbstractAutowireCapableBeanFactory.java:1433) — 返回 false→`populateBean` 直接 return(跳过属性填充)

关键设计: **Why 需要在实例化前拦截？** 某些场景下 — Bean 的实例化不应该走 Spring 的默认构造器注入 — 而应该走自定义逻辑(TargetSource 返回线程池中的共享实例)。BeforeInstantiation 返回非 null → 直接跳过 `doCreateBean` 全部 13 步 — 是最早的拦截点。**Chain of Responsibility**: InstantiationAwareBPP(Before)→Instance→populateBean(其内部开头有 InstantiationAwareBPP(After), :1433)→BPP(BeforeInit)→invokeInit→BPP(AfterInit)→AOP代理。[模式: Chain of Responsibility — BPP链顺序不可颠倒]

数据流: `getBean("scopedBean")`→`resolveBeforeInstantiation(beanName, mbd)`→遍历 BPP: `AutowiredAnnotationBeanPostProcessor.postProcessBeforeInstantiation()`→null(不拦截)→`AbstractAutoProxyCreator.postProcessBeforeInstantiation()`(AbstractAutoProxyCreator.java:273) 检查 TargetSource 是否需要提前代理→需要→返回代理实例→Spring 拿到非null→**跳过 doCreateBean**→直接 `applyBeanPostProcessorsAfterInitialization(proxy, beanName)`→返回代理。注: **作用域代理不在这里产生** — ScopedProxyCreator 没有 postProcessBeforeInstantiation 方法 — 它只有静态 `createScopedProxy()`(ScopedProxyCreator.java:37-40) 返回 BeanDefinitionHolder，在配置类后置处理阶段把原 BD 替换成 "scopedTarget.xxx" 目标 Bean + 代理 Bean 两套定义。

### 2. BeanPostProcessor + DestructionAwareBeanPostProcessor — 初始化+销毁双层

场景: `@PostConstruct public void init()` 和 `@PreDestroy public void cleanup()` — 前者在初始化前(§1 的 injectInitMethods)，后者在容器关闭时。两者都由 BPP 体系驱动: `InitDestroyAnnotationBeanPostProcessor` 同时处理 @PostConstruct 和 @PreDestroy — 但分别在两个不同的 BPP 接口中注册。

源码路径:
- `BeanPostProcessor.java:82` — **postProcessBeforeInitialization**: 返回原 Bean 或包装 Bean(不创建代理)
- `BeanPostProcessor.java:108` — **postProcessAfterInitialization**: 返回原 Bean 或代理 Bean(**AOP 代理在此**)
- `DestructionAwareBeanPostProcessor.postProcessBeforeDestruction()` — 容器 close() 时调用 — `InitDestroyAnnotationBeanPostProcessor` 在此执行 @PreDestroy

关键设计: **Why BeforeInit 和 AfterInit 分离？** BeforeInit 的 BPP 处理"准备"工作(如读取 @Value 注解 → 设置字段)。AfterInit 的 BPP 处理"包装"工作(如生成代理 → 替换原始 Bean)。如果两者合并 — BeforeInit 已经调了 `@PostConstruct` — 然后 AfterInit 生成代理 — 代理可能没有记录 "init 已执行" — 重复调用 init。分离保证了: 原始 Bean 的 init → 包装为代理 → 代理不再调 init。[模式: Interceptor — BPP 拦截 Bean 创建流程，在特定时机插入逻辑]

数据流: `initializeBean("userService", bean, mbd)`→`invokeAwareMethods`→`applyBeanPostProcessorsBeforeInitialization(bean, "userService")`→BPP1: `CommonAnnotationBeanPostProcessor.postProcessBeforeInitialization()` 执行 @PostConstruct → BPP2: `AutowiredAnnotationBeanPostProcessor.postProcessBeforeInitialization()` → null(无操作)→`invokeInitMethods` 执行 afterPropertiesSet+initMethod→`applyBeanPostProcessorsAfterInitialization(bean, "userService")`→BPP1: `CommonAnnotationBeanPostProcessor.postProcessAfterInitialization()`→null→BPP2: `AbstractAutoProxyCreator.postProcessAfterInitialization()`→需要 AOP→`wrapIfNecessary(bean, beanName)`→`createProxy()`→返回 CGLIB 代理→`registerDisposableBean(beanName, new DisposableBeanAdapter(bean, mbd, acc))`— Adapter 持有 @PreDestroy 回调列表。

### 3. DestructionAwareBeanPostProcessor — @PreDestroy 的销毁回调链

场景: `ctx.close()` → 所有单例 Bean 需要调用 `@PreDestroy` 和 `DisposableBean.destroy()`。BPP 第三层 `DestructionAwareBeanPostProcessor.postProcessBeforeDestruction()` 在 Bean 销毁前执行回调。

源码路径: `AbstractApplicationContext.doClose()` → `DefaultSingletonBeanRegistry.destroySingletons()` → 遍历 `disposableBeans` Map → `DisposableBeanAdapter.destroy()` → `InitDestroyAnnotationBeanPostProcessor.postProcessBeforeDestruction(bean, beanName)` — 反射调用 @PreDestroy

关键设计: **Why @PreDestroy 不在 initializeBean 中直接执行？** 初始化时 Bean 刚开始使用——不知道何时销毁。销毁回调必须注册为延迟执行——容器 close() 时才遍历 disposableBeans 逐个销毁——顺序与初始化相反(后创建先销毁，防止依赖 Bean 先被销毁导致 NPE)。

数据流: `ctx.close()`→`AbstractApplicationContext.doClose()`→`beanFactory.destroySingletons()`→`DefaultSingletonBeanRegistry.destroySingletons()`→遍历 `disposableBeans`(按创建逆序)→每个调 `bean.destroy()`→`DisposableBeanAdapter.destroy()`→遍历内部已注册的销毁回调: ①`@PreDestroy` → `InitDestroyAnnotationBeanPostProcessor.postProcessBeforeDestruction(bean, beanName)` 反射调用 cleanup() ②`DisposableBean.destroy()` ③XML/注解自定义 destroy-method → 全部执行完毕→单例 Bean 销毁完成。

→ 引出 S1-4 循环依赖 — A↔B 循环引用时 BeanPostProcessor 做什么？`AbstractAutoProxyCreator.getEarlyBeanReference()`(AbstractAutoProxyCreator.java:265-268) 在三级缓存的 lambda 中执行 `wrapIfNecessary(bean)` — 需要 AOP 时 B 拿到的直接就是**早生成的代理** — 不是原始 Bean。之后 AfterInitialization 检查 `earlyBeanReferences`(:315-319) — 已创建 early 代理(remove 返回==bean)→**不再二次包装**；未提前暴露才走 `wrapIfNecessary` 生成最终代理。这要求三级缓存 singletonFactories 中存的是 lambda 而非最终 Bean — 因为代理可以在填充完成后才生成(S1-2 §2 的三级缓存时序有答案)。
