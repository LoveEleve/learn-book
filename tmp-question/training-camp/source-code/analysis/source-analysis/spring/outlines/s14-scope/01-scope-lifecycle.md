# S2-7 Bean 作用域 — getBean 时的生命周期分流

> 依赖 S2-1 refresh() + S2-5 父子容器 | 🟡 Working | 3 KP | [模式: 策略模式 + 代理模式]

**读者处境**: 之前的域全部默认singleton — 但 @Scope("prototype") 什么时候创建新实例？@Scope("request") 在不同HTTP请求中怎么返回不同Bean？Scoped Proxy 是什么？

### 1. Scope 接口 + singleton/prototype 为何是 Special Case

场景: `getBean("myService")` — 进入 doGetBean → 第一步 getSingleton 检查 singletonObjects(L250) — 找到就返回已创建的 singleton。找不到 → 检查 scope → "singleton" 走 `getSingleton(beanName, ObjectFactory)`(L337) 存入 singletonObjects。如果 scope="prototype" → 跳过 singleton 路径 → beforePrototypeCreation(L356)→createBean→afterPrototypeCreation 且不缓存。如果 scope="request" → 走 scopes map 的 Scope.get(ObjectFactory) 回调 → 由 Scope 负责生命周期。

源码路径:
- `Scope.java:74` — **get(name, ObjectFactory)**: Scope 接口的核心 — 接收一个 ObjectFactory(createBean 的回调) → 返回作用域内的 bean — 是否缓存、何时销毁由 Scope 实现决定
- `AbstractBeanFactory.java:366-387` — **doGetBean scope 分支**: 获取 `mbd.getScope()`(L366) → 从 `this.scopes`(LinkedHashMap, L164) 查找 Scope(L370) → `scope.get(beanName, () -> {beforePrototypeCreation; try {createBean} finally {afterPrototypeCreation}})`(L375-381) → ObjectFactory 回调只在 Scope 决定"需要创建"时调用 → Scope 缓存则后续不调 createBean; Scope 不存在/未激活分别抛 IllegalStateException(L372)/ScopeNotActiveException(L387)
- `Scope.java:93` — **remove(name)**: 仅从作用域移除引用 — javadoc(L79-86)明确: 实现应同时移除已注册的销毁回调, 但**不执行**该回调 — 销毁由调用方(容器)负责。真正的销毁发生在作用域终止时: request/session 结束由 RequestAttributes 注册的 destruction callback 触发(RequestAttributes.java:119) — SimpleThreadScope 甚至不支持销毁回调(registerDestructionCallback 仅打 WARN)

关键设计: **Why singleton 不通过 Scope 接口？** Scope.get(ObjectFactory) 模式只能"创建"bean — 不能处理循环依赖。singleton 需要三级缓存(singletonObjects/earlySingletonObjects/singletonFactories) — 在 createBean 过程中提前暴露未完成的 bean 给其他 singleton(解决循环依赖)。如果把 singleton 也做成 Scope.get() → 循环依赖时 getBean→Scope.get→createBean→getBean→Scope.get→... → 死锁。**singleton 是 Scope 的 Special Case — 不是因为语义不同 — 是因为需要三级缓存解决循环依赖**。

数据流: getBean("userService")→scope=singleton→getSingleton("userService")→singletonObjects.get→存在→直接返回 / getBean("myPrototype")→scope=prototype→getSingleton→null→beforePrototypeCreation(L356)→createBean→不缓存→返回 / getBean("sessionData")→scope=session→getSingleton→null→scopes.get("session")→SessionScope→scope.get("sessionData", ObjectFactory)→RequestContextHolder.currentRequestAttributes()→getAttribute("sessionData",SCOPE_SESSION)→null(首次)→objectFactory.getObject()→createBean→setAttribute→返回sessionData→下次同一session内getBean→getAttribute→非null→直接返回(不调createBean)

### 2. Web Scopes — RequestContextHolder ThreadLocal 的魔法

场景: Controller(singleton)调用 `getBean("requestData")` — requestData 是 request scope — 在请求A中getBean返回dataA — 在请求B中getBean返回dataB — 但Controller和方法调用完全相同 — 区分在哪？RequestContextHolder 用 ThreadLocal 保存当前请求的 RequestAttributes — SessionScope/RequestScope 通过这个 ThreadLocal 区分不同请求/会话。

源码路径:
- `AbstractRequestAttributesScope.java:41-56` — **get(name, ObjectFactory)**: ①`RequestContextHolder.currentRequestAttributes()`(L42) → ThreadLocal 获取当前请求属性 ②`requestAttributes.getAttribute(name, getScope())`(L43) → 若已存在返回 / 若不存在调 objectFactory.getObject() 创建并 setAttribute(L45-46) ③销毁回调由 Scope.registerDestructionCallback(L73-75) 注册到 RequestAttributes — 请求/会话结束时由 RequestAttributes 实现(如 ServletRequestAttributes)触发执行
- `SimpleThreadScope.java:62-71` — **ThreadLocal 实现**: 内部 `ThreadLocal<Map<String, Object>> threadScope`(L58) — scope.get → threadScope.get()→get+null判断+put(L62-71, 注释 L64-65 明确禁止改成 computeIfAbsent) — scope.remove → 仅 `scope.remove(name)`(L76-78, **无 destroy 回调**) — 该类**不支持**销毁回调: registerDestructionCallback 仅打 WARN(L82-85)

关键设计: **Why RequestContextHolder 用 ThreadLocal？** 每个HTTP请求由不同线程处理 — ThreadLocal 天然隔离不同请求/不同用户的属性。用户A的请求在 Thread-5 → RequestContextHolder 返回 Thread-5 绑定的 RequestAttributes(SessionA的数据) — 用户B的请求在 Thread-7 → 返回 Thread-7 的 RequestAttributes(SessionB的数据)。同一个 getBean("sessionData") 在不同线程返回不同 Session 的 bean — Controller 不需要知道"当前是哪个用户"。

数据流: HTTP请求到达→Tomcat线程池分配线程T5→Spring FrameworkFilter→RequestContextFilter.initContextHolders(L112-118)→RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request))(L113)→T5进入Controller→getBean("requestScopedBean")→scope=request→RequestScope.get("requestScopedBean", ObjectFactory)→RequestContextHolder.currentRequestAttributes()→返回T5绑定的ServletRequestAttributes→getAttribute("requestScopedBean",SCOPE_REQUEST)→null→objectFactory.getObject()→createBean→setAttribute→返回→请求结束→RequestContextFilter.resetContextHolders(L120-124)→RequestContextHolder.resetRequestAttributes()(L121)→清除T5的ThreadLocal→T5回到线程池处理下一个请求→绑定新request

### 3. Scoped Proxy — 短作用域 Bean 注入长作用域 Bean

场景: @Controller(singleton) 中 @Autowired 了一个 @Scope("request") 的 Bean — singleton 在容器启动时创建 — 此时 request 还不存在 — @Autowired 注入什么？答: 注入的是 CGLIB 代理(ScopedProxy) — 代理把所有方法调用转发给 `ScopedObject.getTargetObject()`(ScopedObject.java:42, 实现 DefaultScopedObject.java:59-60) → 从当前 request scope 获取真实 bean → 如果当前没有 request(如启动时) → 抛 ScopeNotActiveException。

源码路径:
- `ScopedProxyFactoryBean.java:87-115` — **setBeanFactory()**: 创建代理: ①`ProxyFactory pf = new ProxyFactory()`(L93) + `pf.setTargetSource(this.scopedTargetSource)`(L95, SimpleBeanTargetSource) ②`new DefaultScopedObject(cbf, targetBeanName)`(L108) → `pf.addAdvice(new DelegatingIntroductionInterceptor(scopedObject))`(L109) ③`pf.getProxy(classLoader)`(L115) — CGLIB/JDK 由 ProxyFactory 按 proxyTargetClass/接口决定 — 注意方向: **ScopedProxyCreator.createScopedProxy(ScopedProxyCreator.java:37-40) 是在注册期把 BeanDefinition 替换为 ScopedProxyFactoryBean 类型**, setBeanFactory 是随后的代理创建
- `ScopedProxyFactoryBean.java:121-125` — **getObject()**: 仅返回 `this.proxy`(L125) — 真实 bean 的获取发生在每次代理方法调用时: MethodInterceptor → DefaultScopedObject.getTargetObject()(DefaultScopedObject.java:59-60) → beanFactory.getBean(目标beanName) → Scope.get → 返回当前 request/session 中的真实 bean → 调用真实方法

关键设计: **Why Scoped Proxy 而非 keepalive 机制？** 早期 Spring(1.x) 使用"request-scoped bean 注入 singleton 时把 singleton 也变成 request scope" — 这导致整个依赖链都变成短作用域 — 不可控。Scoped Proxy 的解决: singleton 持有的是 Proxy — Proxy 延迟到每次方法调用时才从 Scope 获取真实 bean — singleton 自身仍是 singleton — 只有被注入的 bean 的作用域变短。

数据流: @Controller(singleton) 中有 @Autowired RequestData requestData(@Scope("request")) → 注册期: ScopedProxyCreator.createScopedProxy(ScopedProxyCreator.java:37-40) 把 BeanDefinition 替换为 ScopedProxyFactoryBean 类型 → 实例化时 setBeanFactory(L87-116): ProxyFactory(L93)+SimpleBeanTargetSource(L95)+DefaultScopedObject 经 DelegatingIntroductionInterceptor 注入(L108-109) → proxyTargetClass=true → CGLIB Enhancer 创建 RequestData$$EnhancerBySpringCGLIB 代理子类 → @Controller.requestData = CGLIB代理实例 → HTTP请求到达 → @Controller.method() → requestData.getData() → CGLIB MethodInterceptor → DefaultScopedObject.getTargetObject()(DefaultScopedObject.java:59-60) → beanFactory.getBean("requestData") → RequestScope.get → RequestContextHolder.ThreadLocal → getAttribute → 获取当前 request 的真实 RequestData → 调用真实方法 → 返回结果

→ spring-context 第七域完成。scope 接口→doGetBean 分支→Web Scope→Scoped Proxy — 四个层次解释了 singleton/prototype/request/session 的生命周期差异。引出 S2-8: ApplicationContext 三大实现 — AnnotationConfig/ClassPathXml/GenericWeb — refresh() 之前发生了什么？
