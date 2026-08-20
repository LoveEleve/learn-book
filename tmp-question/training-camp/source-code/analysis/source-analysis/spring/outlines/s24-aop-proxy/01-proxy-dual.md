# A-1 代理机制 — JDK Proxy vs CGLIB 的双轨实现

> 依赖 S2-10 @Async + S2-12 @Cacheable | 🔴 Deep | 2 KP | [模式: 代理模式 + 工厂模式]

**读者处境**: @Async、@Cacheable、@Transactional 都用 AOP 代理 — 但有些用的是 JDK Proxy(基于接口) — 有些是 CGLIB(基于子类)。DefaultAopProxyFactory 如何决定用哪种？JdkDynamicAopProxy.invoke 和 CglibAopProxy.DynamicAdvisedInterceptor 的调用链有什么不同？

### 1. DefaultAopProxyFactory.createAopProxy — JDK vs CGLIB 四条件决策

场景: ProxyFactory.getProxy() → DefaultAopProxyFactory.createAopProxy(AdvisedSupport config) → 检查 `config.isProxyTargetClass()`(强制CGLIB) → `config.isOptimize()`(优化模式) → target 有没有接口 → 无接口就 CGLIB → 有接口就 JDK Proxy。@Async(proxyTargetClass=true)走CGLIB — @Transactional(默认)如果Service有接口走JDK — 如果Service没有接口走CGLIB。

源码路径:
- `DefaultAopProxyFactory.java:60` — **createAopProxy()** — 决策树: ①L61 单一析取 `config.isOptimize() || config.isProxyTargetClass() || !config.hasUserSuppliedInterfaces()`(hasUserSuppliedInterfaces 声明于 AdvisedSupport.java:265)→true→进入 CGLIB 分支(如@Async的ProxyAsyncConfiguration设置proxyTargetClass=true) ②分支内 L67-68 `targetClass == null || targetClass.isInterface() || Proxy.isProxyClass(targetClass) || ClassUtils.isLambdaClass(targetClass)`→true→L69 JDK Proxy ③分支内其余情况→L71 CGLIB(ObjenesisCglibAopProxy) ④L61 为false(有用户接口且未优化/未强制)→L74 JDK Proxy — @Transactional(默认)如果Service有接口走JDK — 如果Service没有接口走CGLIB
- `JdkDynamicAopProxy.java:115-124` — **getProxy()**: `Proxy.newProxyInstance(determineClassLoader(classLoader), proxiedInterfaces, this)` — **this 是 InvocationHandler** — Proxy的每次方法调用 → invoke(this, proxy, method, args)
- `CglibAopProxy.java:162` — **getProxy()** → buildProxy(:176): ①L198 `createEnhancer()`(声明于 :265) ②L206 `enhancer.setSuperclass(proxySuperClass)`、L207 `enhancer.setInterfaces(...)` ③L215 `getCallbacks(rootClass)`→回调数组(L324 `new DynamicAdvisedInterceptor(this.advised)` 是数组元素之一)—L221 `new ProxyCallbackFilter`→L223 `setCallbackFilter` 按方法分发回调 ④L229 `createProxyClassAndInstance`→L258 `enhancer.create()` — DynamicAdvisedInterceptor实现CGLIB的MethodInterceptor — CGLIB生成子类重写所有非final方法

关键设计: **Why 有接口时默认用 JDK Proxy 而非 CGLIB？** JDK Proxy 是 Java 标准库代理 — 无第三方依赖 — 性能与 CGLIB 接近但更轻量。CGLIB 需要生成子类 — 字节码开销大 — 启动慢 — 且无法代理 final 方法。**JDK Proxy = 基于接口的代理(更轻量) — CGLIB = 基于类的代理(更强大但开销大) — Spring 选择"有接口→JDK / 无接口→CGLIB"的默认策略平衡两者。**

注意 `isOptimize()` 的语义: 它不表示"优化到 JDK" — 而是"允许 Spring 选择它认为最优的代理方式" — 实际效果是放开 JDK/CGLIB 决策进入 CGLIB 分支再按 targetClass 形态回退 JDK(L67-69)。`isProxyTargetClass()` 是**强制**CGLIB — 只有当 targetClass 本身是接口/已代理/lambda 时才被迫回到 JDK。两者都是"倾向 CGLIB"，差别在强制程度。

Spring Boot 默认 `spring.aop.proxy-target-class=true` — 即绝大多数 Spring Boot 应用的 Service 都走 CGLIB 路径。

数据流: @Service public class UserService implements IUserService → ProxyFactory.setTarget(userService)→setInterfaces(IUserService.class)→getProxy()→DefaultAopProxyFactory.createAopProxy(config)→config.isProxyTargetClass()=false→!isOptimize()→hasUserSuppliedInterfaces=true→④L61析取为false→L74 JDK Proxy→JdkDynamicAopProxy.getProxy()→L124 Proxy.newProxyInstance(classLoader, [IUserService.class], this)→返回IUserService代理→调用userService.getUser(1)→Proxy→JdkDynamicAopProxy.invoke(L167)→getInterceptorsAndDynamicInterceptionAdvice→advice chain→AOP处理→method.invoke(target)→返回结果。若同场景配置 proxyTargetClass=true → L61 析取为 true → 进入 CGLIB 分支 → targetClass=UserService 非接口非代理非 lambda → L71 ObjenesisCglibAopProxy → 生成 UserService$$SpringCGLIB 子类 → 子类重写 getUser → 调用走 DynamicAdvisedInterceptor.intercept(L700)。

### 2. JdkDynamicAopProxy.invoke + CglibAopProxy 调用链对比

场景: Controller调用 userService.getUser(1) → 代理拦截 → JDK: JdkDynamicAopProxy.invoke(proxy, method, args) → CGLIB: DynamicAdvisedInterceptor.intercept(obj, method, args, proxy) → 两者的核心逻辑相同: 获取advice chain → new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain) → invocation.proceed() → 逐个advice调用 → 最终 joinpoint.proceed() → method.invoke(target, args)。

源码路径:
- `JdkDynamicAopProxy.java:167` — **invoke()**: ①L195 `this.advised.exposeProxy`→AopContext.setCurrentProxy(proxy)(ThreadLocal)—允许target内 this调用也走代理 ②L207 `getInterceptorsAndDynamicInterceptionAdvice(method, targetClass)`→Interceptors列表 ③L211 chain.isEmpty()→L216 `AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse)`直接调目标 ④非空→L221 new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain)→L223 proceed()→advice链
- `CglibAopProxy.java:690` — **DynamicAdvisedInterceptor.intercept()(:700)** — 同样的步骤: ①L714 获取advice chain ②L718 空链→L724 `AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse)` ③非空→L728 `new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain).proceed()`(本版本无 CglibMethodInvocation 子类)

关键设计: **两种代理的调用链核心一致 — 只是入口不同(JDK=invoke / CGLIB=intercept) — 但后续的 ReflectiveMethodInvocation.proceed + advice chain 完全共享。** 这意味着同一个 @Transactional advice 在 JDK 代理和 CGLIB 代理上行为完全一致 — 只是"如何进入调用链"不同 — "进入后怎么执行"完全相同。Spring AOP 通过统一的 ReflectiveMethodInvocation 实现了代理无关的 advice 执行。

**为什么 CGLIB 代理无法拦截 final/private 方法？** CGLIB 通过生成目标类的子类并覆写方法实现拦截 — 但 final 方法不可覆写、private 方法不可见、static 方法属于类本身 — 这三种方法无法被子类重写 → 调用它们时不会经过 DynamicAdvisedInterceptor。这就是 @Transactional 失效场景中"非 public 方法"的根源: Spring 6 默认用 CGLIB(proxyTargetClass 常被设为 true)，但 private/final 方法仍然直接调用目标类版本。

**invokeJoinpointUsingReflection 与 methodProxy.invoke 的区别**: CGLIB 的 methodProxy.invoke(target, args) 也会经过增强的代理类 — 但 Spring 在空链时选择 `AopUtils.invokeJoinpointUsingReflection(target, method, args)` 直接反射调用原始方法 — 绕过代理类字节码 — 避免空转的开销。非空链时 ReflectiveMethodInvocation 内部同样用反射调用 joinpoint — CGLIB 的 MethodProxy 仅用于 intercept 入口本身。

数据流: Controller调 userService.getUser(1)→JDK Proxy→JdkDynamicAopProxy.invoke(L167)→①L195 this.advised.exposeProxy=true→AopContext.setCurrentProxy(proxy)→ThreadLocal存储代理引用→②L207 getInterceptorsAndDynamicInterceptionAdvice(getUser, UserService)→[TransactionInterceptor, AsyncExecutionInterceptor]→③L211 chain非空→L221 new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain)→④L223 invocation.proceed()→currentInterceptorIndex=0→TransactionInterceptor.invoke(invocation)→beginTransaction→invocation.proceed()(递归)→index=1→AsyncExecutionInterceptor.invoke(invocation)→invocation.proceed()→index=2→chain结束→⑤ReflectiveMethodInvocation.invokeJoinpoint()(ReflectiveMethodInvocation.java:163)→method.invoke(target, args)→target.getUser(1)→查DB返回User→逐层回退→AsyncInterceptor返回结果→TransactionInterceptor.commitTrans→返回User→Controller收到

→ spring-aop 第一域完成。JDK Proxy ⇄ CGLIB 双轨 — DefaultAopProxyFactory 决策 — 统一的 ReflectiveMethodInvocation 调用链。引出 A-2: Advice 链 — Before/After/Throws/Around 五种 advice 的执行顺序和 ReflectiveMethodInvocation.proceed 的迭代逻辑。
