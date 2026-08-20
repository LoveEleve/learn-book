# A-4 自动代理 — wrapIfNecessary 模板 + 三种匹配策略

> 依赖 A-1 代理 + A-3 @AspectJ | 🟡 Working | 2 KP | [模式: 模板方法]

**读者处境**: A-1 讲了怎么创建代理 — A-3 讲了 @AspectJ 怎么转为 Advisor — 但谁决定哪个 Bean 需要代理？答案是 AbstractAutoProxyCreator.wrapIfNecessary — 所有 BPP(@Async/@Cacheable/@AspectJ)都是它的子类。

### 1. AbstractAutoProxyCreator.wrapIfNecessary — 自动代理的模板方法

场景: Bean 经过 initializeBean 后 — postProcessAfterInitialization → AbstractAutoProxyCreator.wrapIfNecessary(bean, beanName) → 检查 whether infrastructure class→shouldSkip→getAdvicesAndAdvisorsForBean(子类实现)→empty? return bean / non-empty? createProxy→返回代理 @Service → UserService 被包装为 JDK/CGLIB 代理。

源码路径:
- `AbstractAutoProxyCreator.java:354` — **wrapIfNecessary()**: ①L361 `isInfrastructureClass(bean.getClass())`→true→return bean(不代理AOP基础设施) ②L361 `shouldSkip(bean.getClass(), beanName)`→true→跳过 ③L367 `getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null)`(子类覆写)→Object[] specificInterceptors ④L368 `specificInterceptors != DO_NOT_PROXY`→走创建代理分支; 否则 L376-377 标记后 return bean(不代理) ⑤L370 `createProxy(bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean))`(createProxy 方法声明于 L462)→返回代理
- `AbstractAutoProxyCreator.java:415` — **shouldSkip()**: 默认返回false—子类覆写(AspectJAwareAdvisorAutoProxyCreator覆写shouldSkip来标记@Aspect自身不用代理)
- 三个入口(实际声明行): `getEarlyBeanReference()` L265→wrapIfNecessary 调用在 L268; `postProcessAfterInitialization()` L315→wrapIfNecessary 调用在 L319; `postProcessBeforeInstantiation()` L273→自定义 TargetSource 时 createProxy(L295)(L248 是 determineProxyType 内的 getAdvicesAndAdvisorsForBean 调用)—对应Bean生命周期三个阶段

关键设计: **Why wrapIfNecessary 有三个调用入口？** Bean 生命周期三个阶段都需要自动代理: ①postProcessBeforeInstantiation(用途是自定义 TargetSource 的提前代理—L289 getCustomTargetSource 非空才 createProxy) ②postProcessAfterInitialization(正常初始化后→创建代理, 最常用) ③getEarlyBeanReference(解决循环依赖→提前暴露代理而非原始bean—L265)。三个入口确保 "无论Bean在哪个生命周期阶段被引用 — 都返回代理而非原始对象"。

数据流: refresh()Step11→getBean("userService")→createBean→doCreateBean→initializeBean→applyBeanPostProcessorsAfterInitialization→AnnotationAwareAspectJAutoProxyCreator.postProcessAfterInitialization(L315)→L319 wrapIfNecessary(userService, "userService")→L361 isInfrastructureClass(UserService.class)→false→L361 shouldSkip(UserService.class)→false→L367 getAdvicesAndAdvisorsForBean→findEligibleAdvisors→遍历所有Advisors→@Aspect LoggingAspect的Advisor→Pointcut.matches(getUser)→true→eligible→收集为eligibleAdvisors→advisors=[TransactionInterceptor, LoggingAdvisor]→L368 specificInterceptors != DO_NOT_PROXY→L370 createProxy→new ProxyFactory→setInterceptors→setTarget→getProxy→DefaultAopProxyFactory.createAopProxy→CGLIB(无接口)→Enhancer.create→UserService$$CGLIB代理→返回→替换BeanFactory中的userService→后续getBean返回代理

### 2. 三种匹配策略 — BeanName vs DefaultAdvisor vs AspectJAwareAdvisor

场景: 不同 BPP 选择不同的 AbstractAutoProxyCreator 实现: BeanNameAutoProxyCreator(旧式)<name="userService" advisors="txInterceptor">→只按名称匹配 Bean — 不用 Pointcut。DefaultAdvisorAutoProxyCreator→遍历所有 Advisor — Pointcut.matches()判断是否拦截。AspectJAwareAdvisorAutoProxyCreator→额外提供 @Aspect Advisor 排序(DeclarationOrder)。

源码路径:
- `BeanNameAutoProxyCreator.java:47` — **类声明**: extends AbstractAutoProxyCreator; isMatch(beanName, mappedName) 调用在 L122 — 通配符匹配, 不涉及 Pointcut
- `DefaultAdvisorAutoProxyCreator.java:39` — **继承链**: extends AbstractAdvisorAutoProxyCreator(与 AspectJAware 版为兄弟关系)
- `AspectJAwareAdvisorAutoProxyCreator.java:50` — **@Aspect 排序**: extends AbstractAdvisorAutoProxyCreator; shouldSkip 覆写(L103,@Aspect 自身不代理)

| 实现 | 匹配方式 | 使用场景 | 继承链 |
|:--|------|------|:--|
| BeanNameAutoProxyCreator | beanName 通配符 | 旧版/XML配置 | AbstractAutoProxyCreator 直接子类 |
| DefaultAdvisorAutoProxyCreator | Advisor.getPointcut().matches() | 所有 Advisor-base BPP(@Async/@Cacheable/编程式) | AbstractAdvisorAutoProxyCreator→AbstractAutoProxyCreator |
| AspectJAwareAdvisorAutoProxyCreator | @Aspect Advisor排序+eligibility | @AspectJ 注解切面 | AbstractAdvisorAutoProxyCreator→AbstractAutoProxyCreator(与Default版为兄弟, AspectJAwareAdvisorAutoProxyCreator.java:50 / DefaultAdvisorAutoProxyCreator.java:39) |

关键设计: **Why BeanName 匹配不需要 Pointcut？** 三种策略本质是"匹配规则"不同: BeanName 版按名字通配符匹配(最直接, 无 Pointcut 概念)— DefaultAdvisor 版按 Advisor.getPointcut().matches() 匹配(通用)— AspectJAware 版在 Default 基础上加 @Aspect 排序。三者都是 AbstractAutoProxyCreator 的"getAdvicesAndAdvisorsForBean 策略"差异 — 继承链共用 wrapIfNecessary 骨架。[模式: 模板方法 — 匹配策略是子类覆写点]

数据流: @Aspect @Component LoggingAspect→AspectJAwareAdvisorAutoProxyCreator.shouldSkip(LoggingAspect.class)→true(切面自身不代理)→跳过。UserService Bean进入wrapIfNecessary→shouldSkip=false→getAdvicesAndAdvisorsForBean→findEligibleAdvisors→遍历: TransactionInterceptor(来自@Transactional)→Pointcut.matches→true→eligible→LoggingAdvisor(来自@AspectJ)→Pointcut.matches→true→eligible→sorted(DeclarationOrder)→advisors=[TransactionInterceptor, LoggingAdvisor]→createProxy→ProxyFactory→CGLIB代理→UserService$$CGLIB包含两个Advisor→AOP调用链: TransactionInterceptor.invoke→proceed→LoggingAdvisor.before→proceed→getUser→返回

→ spring-aop 第四域完成。wrapIfNecessary 模板 + 三种匹配策略 — 自动代理的完整链路。引出 A-5: Pointcut 表达式 — execution/within/this/target/args/@annotation 的完整语法和 AspectJ PointcutParser。
