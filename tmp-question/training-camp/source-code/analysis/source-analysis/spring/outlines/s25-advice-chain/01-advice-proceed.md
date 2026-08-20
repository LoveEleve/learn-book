# A-2 Advice 链 — proceed 递归迭代 + 五种Advice执行顺序

> 依赖 A-1 代理 | 🟡 Working | 1 KP | [模式: 责任链模式]

**读者处境**: A-1 中 JdkDynamicAopProxy.invoke 的最后调了 `invocation.proceed()` — 这个 proceed 是什么？它如何逐个调用 advice 链？五种 Advice(Before/Around/AfterReturning/Throws)的执行顺序是什么？

### 1. ReflectiveMethodInvocation.proceed — 责任链的递归迭代

场景: JdkDynamicAopProxy.invoke → `new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain)` → `invocation.proceed()` → proceed 内部有 `currentInterceptorIndex` 指针从 0 开始 → 每次调用链上一个 interceptor.invoke(this) → interceptor 内部调 `invocation.proceed()` → 递归→ index+1 → 直到所有 interceptor 调用完毕 → invokeJoinpoint → method.invoke(target)。

源码路径:
- `ReflectiveMethodInvocation.java:160` — **proceed()**: ①L162 `currentInterceptorIndex == interceptorsAndDynamicMethodMatchers.size()-1`→链已到末尾→L163 直接 `return invokeJoinpoint()` ②L166-167 `interceptorOrInterceptionAdvice = interceptorsAndDynamicMethodMatchers.get(++currentInterceptorIndex)`→取下一个 ③L184 `interceptor.invoke(this)`→**advice调this=ReflectiveMethodInvocation实例**→advice内部必须调 `invocation.proceed()` 才能继续链 ④如果advice不调proceed→链停在此处(如@Cacheable命中→直接返回缓存→不调proceed) ⑤L175-178 动态匹配失败分支→`return proceed()` 递归跳过该拦截器(仅动态匹配分支, 非通用递归路径) ⑥所有advice执行完→L195 `invokeJoinpoint()`→`AopUtils.invokeJoinpointUsingReflection(this.target, this.method, this.arguments)`→真正执行目标方法
- `ReflectiveMethodInvocation.java:195` — **invokeJoinpoint()**: `method.invoke(this.target, this.arguments)` — 目标方法的最终调用 — proceed链的终点

关键设计: **Why proceed 是递归而非循环？** 循环(for/while)只能顺序执行 — 没有"advice之间传递状态"的能力。递归让每个advice可以: ①在proceed前执行(Before/around-before) ②调proceed继续 ③在proceed后执行(AfterReturning/around-after) ④在异常时拦截(Throws)。**递归 = advice 在调用栈上下层之间有"之前"和"之后"两个阶段 — 循环只能有"之前"一个阶段。**

数据流: JdkDynamicAopProxy.invoke→chain=[TransactionInterceptor, LogBeforeAdvice, TimerAroundAdvice]→new ReflectiveMethodInvocation→proceed()→L160 index=-1 < size-1(2)→++index=0→get(0)=TransactionInterceptor→L184 TransactionInterceptor.invoke(this)→advice代码: beginTx→invocation.proceed()→递归proceed→L160 index=0 < 2→++index=1→get(1)=LogBeforeAdvice→LogBeforeAdvice.before(method, args)→log→invocation.proceed()→递归proceed→L160 index=1 < 2→++index=2→get(2)=TimerAroundAdvice→TimerAroundAdvice.invoke(this)→start=System.nanoTime→invocation.proceed()→递归proceed→L160 index=2 NOT < 2→end→invokeJoinpoint→method.invoke(target, args)→返回result→TimerAroundAdvice: duration=System.nanoTime-start→return result→LogBeforeAdvice: return result→TransactionInterceptor: commitTx→return result→最终result返回给JdkDynamicAopProxy→返回给调用方

### 2. 五种 Advice 的执行顺序 — 责任链的拦截层次

场景: 一个 Service 方法有 @Transactional(around-advice), @Cacheable(around-advice 未命中不调proceed返回缓存), 自定义 BeforeAdvice — 这三个advice的执行顺序: Around先→Before→Joinpoint→Around-after。@Cacheable 如果在 before 阶段命中 → 不调proceed → 后续所有advice和joinpoint全部跳过。

源码路径:
- `ReflectiveMethodInvocation.java:184` — **MethodInterceptor 调用点**: `((MethodInterceptor) interceptorOrInterceptionAdvice).invoke(this)` — 所有 around advice 统一入口
- `MethodBeforeAdviceInterceptor.java:56-58` — **BeforeAdvice→MethodInterceptor 适配**: invoke → 先调 `advice.before(method, args, target)`(L57) → 再 `mi.proceed()`(L58)
- `AfterReturningAdviceInterceptor.java:56-58` — **AfterReturning 适配**: 先 `mi.proceed()`(L57) → 成功后调 `afterReturning(retVal, ...)`(L58)
- `ThrowsAdviceInterceptor.java:135` — **Throws 适配**: try { mi.proceed() } catch(Throwable) → 匹配异常参数类型 → afterThrowing

Advice 执行顺序 (index递增):

| # | Advice 类型 | 拦截点 | 代表 |
|:--:|------|------|------|
| 1 | MethodInterceptor (Around) | proceed前/后 | @Transactional/@Async/@Cacheable |
| 2 | MethodBeforeAdvice | proceed前 | LogBeforeAdvice, SecurityBeforeAdvice |
| 3 | **invokeJoinpoint** | 目标方法 | method.invoke(target, args) |
| 4 | AfterReturningAdvice | proceed后(正常返回) | LogAfterAdvice, MetricsAfterAdvice |
| 5 | ThrowsAdvice | proceed抛异常时 | ExceptionTranslationAdvice |

关键设计: **Why 表格中 invokeJoinpoint 也算"一个拦截点"？** proceed 链的迭代模型: 每个"拦截器"要么调 proceed(继续)— 要么不调(短路)— 当 currentInterceptorIndex 到达链尾时 proceed 内部调用 invokeJoinpoint 执行目标方法 — 它是链的"终止符"。Before/AfterReturning/Throws 都通过适配器转成 MethodInterceptor 进入链(A-2 适配器)— Around 直接是 MethodInterceptor — 表格展示的是"执行顺序"而非"接口层级"。

数据流: AOP代理调目标方法→chain[TransactionInterceptor(Before+After), LogBeforeAdvice, ValidationAroundAdvice]→new ReflectiveMethodInvocation→proceed→index=0: TransactionInterceptor.invoke→beginTx→invocation.proceed→index=1: LogBeforeAdvice.before(method, args)→log→invocation.proceed→index=2: ValidationAroundAdvice.invoke→validator.validate(args)→invocation.proceed→all advices done→invokeJoinpoint→method.invoke(target)→throws IllegalArgumentException→回溯: ValidationAroundAdvice.catch→wrap→throw→回溯: LogBeforeAdvice no after (异常)→回溯: TransactionInterceptor→catch→rollbackTx→throw→最终异常返回给调用方

→ spring-aop 第二域完成。proceed递归 + 五种Advice顺序。引出 A-3: @AspectJ 解析 — ReflectiveAspectJAdvisorFactory 如何把 @Aspect/@Before/@After/@Around 注解类转换为 Advisor 链。
