# S2-12 @Cacheable — Spring 缓存抽象的 execute 模板方法

> 依赖 S2-10 @Async + S2-6 BPP全景 | 🟡 Working | 3 KP | [模式: 模板方法 + 策略模式]

**读者处境**: @Async 和 @Cacheable 都由 AOP MethodInterceptor 驱动——但 @Scheduled 不同: 它由 ScheduledAnnotationBeanPostProcessor(MergedBeanDefinitionPostProcessor + SmartInitializingSingleton) 处理，不走 MethodInterceptor。@Cacheable 的核心不是"异步"或"定时"——而是"先查缓存——有就返回——没有就执行方法并缓存"。这个 execute() 模板方法是 Spring 缓存抽象的精髓。

### 1. @EnableCaching → CacheInterceptor + execute 模板方法

场景: `@EnableCaching` → Spring 自动配置 CacheManager(ConcurrentMapCacheManager/CaffeineCacheManager/RedisCacheManager) → @Bean CacheInterceptor → BPP postProcessAfterInitialization 扫描 Bean → 找到 @Cacheable 方法 → 创建 AOP 代理 → Controller 调 `userService.getUser(1)` → CacheInterceptor.invoke() → 进入 execute 模板方法。

源码路径:
- `EnableCaching.java:167-168` — **@EnableCaching**: @Import(CachingConfigurationSelector)→ProxyCachingConfiguration→@Bean CacheInterceptor
- `CacheInterceptor.java:46` — **类声明**: CacheInterceptor extends CacheAspectSupport implements MethodInterceptor — invoke()(L50): L51 取 method→L53-60 构造 aopAllianceInvoker lambda(内部 try 把 invocation.proceed() 包装成 ThrowableWrapper)→L62 `Object target = invocation.getThis()`→L65 `execute(aopAllianceInvoker, target, method, invocation.getArguments())`→调父类模板方法。cacheOperationSource 的获取在 CacheAspectSupport.execute() 内部(L406 getCacheOperationSource()→L408 getCacheOperations)
- `CacheAspectSupport.java:402-417` — **公共 execute()**: 只做 context 构建——L406 getCacheOperationSource()→L408 getCacheOperations(method, targetClass)→L411 new CacheOperationContexts(解析所有@Cacheable/@CacheEvict/@CachePut→分组)→L416 invokeOperation。真正的模板逻辑在私有 execute()(L435): ①L442 processCacheEvicts(evict, beforeInvocation=true)(@CacheEvict beforeInvocation=true→先清缓存) ②L446 findCachedValue(查@Cacheable缓存→命中→返回；未命中→继续) ③L448 evaluate(cacheHit, invoker, method, contexts)→evaluate()(L583): L601 invokeOperation(真正执行目标方法)→L602 unwrapReturnValue→L608/612 collectPutRequests(@Cacheable未命中+@CachePut→缓存结果)→L623 processCacheEvicts(evict, beforeInvocation=false)(方法成功后清缓存)

关键设计: **Why execute() 是模板方法——把"什么时候查缓存/什么时候执行方法/什么时候缓存结果"三个步骤固定为骨架——但"缓存存在哪里"(CacheManager)和"key怎么生成"(KeyGenerator)留给子系统和配置？** 这就是 Spring 缓存抽象的核心价值: 模板方法定义了"缓存的生命周期"——CacheManager 定义了"缓存的物理存储"。同一套 @Cacheable 注解可以用 ConcurrentMap(单机)→ Redis(分布式)→ Caffeine(本地高性能缓存)三种后端——不改一行业务代码。**如果不分模板方法和策略——每次切换缓存后端都要改 execute() 内部逻辑。**

数据流: @EnableCaching→CachingConfigurationSelector→ProxyCachingConfiguration→@Bean CacheInterceptor(cacheManager, keyGenerator)→Container: CacheInterceptor.invoke()(L50)→CacheAspectSupport.execute()(L402)→L406 getCacheOperationSource→L408 getCacheOperations(method, targetClass)→@Cacheable("users") key="#id"→L411 CacheOperationContexts(所有cache operations)→私有execute(L435)→L442 processCacheEvicts(beforeInvocation)→L446 findCachedValue→CacheResolver.resolveCaches→cacheManager.getCache("users")→cache.doGet(key=1)(L574)→null→继续→L583 evaluate→L601 invokeOperation→method.invoke(target, 1)→userService.getUser(1)→查询DB返回User{id=1,name="Alice"}→L602 unwrapReturnValue→L608 collectPutRequests→@Cacheable未命中→cache.put(1, User{1,"Alice"})→L623 processCacheEvicts(afterInvocation)→@CacheEvict afterInvocation? no→return User

### 2. @Cacheable 缓存查询 — 命中/未命中分流 + SpEL key

场景: `@Cacheable(value="users", key="#id")` 第一次调用 getUser(1) → 缓存未命中 → 执行 DB查询 → 缓存结果 → 第二次调用 getUser(1) → 缓存命中 → 直接返回缓存值 → 不查 DB。

源码路径:
- `CacheAspectSupport.java:519` — **findCachedValue()**: 遍历 Contexts 中的 @Cacheable 操作 → condition 通过→generateKey→findInCaches(L542)→对每个 cache→L574 `cache.doGet(key)`→非null→返回(命中)；全未命中→返回 null 交给 evaluate。sync=true 的缓存穿透锁在 executeSynchronized()(L455) 中处理: L496 `doGet(cache, key, () -> unwrapReturnValue(invokeOperation(invoker)))`(锁内调用方法并填充缓存)
- `CacheOperationExpressionEvaluator.java:49` — **NO_RESULT 常量**: 表达式求值哨兵。`#root.method`→当前Method对象; `#root.target`→target bean; `#root.args`→参数数组; `#id`→参数的id字段; `#result`→方法返回值(仅unless/condition可用)——#root 各变量文档在 CacheEvict.java:81-87
- `@Cacheable.java:condition/unless` — **条件过滤**: `condition="#id > 0"`(满足条件才查缓存) / `unless="#result == null"`(返回值null不缓存) / `sync=true`(缓存穿透锁)

关键设计: **Why 默认 SimpleKeyGenerator 而非直接方法签名哈希？** `getUser(1)` 和 `getUser(2)` 是不同的缓存条目——key 必须包含**参数**而非仅方法名。SimpleKeyGenerator 按参数生成 key——但单参数且非数组时直接返回参数本身(SimpleKeyGenerator.java:56-59: `if (params.length == 1) return param`)，因此 `getUser(1)` 的 key 就是 Integer 1，`getUser(2)` 是 2——不包装成 SimpleKey；只有多参数/数组参数才包装为 `new SimpleKey(params)`。如果只哈希方法名——所有 getUser() 调用的缓存值都是同一个——参数不同的请求全部返回同一错误缓存。**Spring 选择"包含参数"作为默认且安全的行为——开发者不需要显式指定 key 除非需要自定义逻辑。**

数据流: CacheInterceptor.invoke()(L50)→L65 execute()→公共execute(L402)→L406 getCacheOperationSource→L411 CacheOperationContexts→私有execute(L435)→L442 processCacheEvicts(beforeInvocation)→L446 findCachedValue→遍历contexts→@Cacheable(value="users", key="#id")→L314 getCaches→cacheResolver.resolveCaches→cacheManager.getCache("users")→findInCaches→L574 cache.doGet(key=1)(单参数直接返回参数本身)→User{1,"Alice"}→命中→返回cacheHit→跳过evaluate→用户收到缓存值→不调DB

### 3. @CacheEvict / @CachePut + condition/unless 条件过滤

场景: `@CacheEvict(value="users", key="#id")` 在 updateUser 成功后清除对应缓存——下次 getUser 查缓存未命中→查 DB→获取新值。`@CachePut(value="users", key="#result.id")` 在 createUser 成功后把新用户放入缓存——@CachePut 总是执行方法并缓存结果——不检查缓存是否存在。

源码路径:
- `CacheEvict.java:144,155` — **allEntries/beforeInvocation**: allEntries=true→清空整个cache / beforeInvocation=true→方法执行前清缓存 / beforeInvocation=false→方法成功后清缓存(默认)
- `CachePut.java:57` — **注解声明**: 总是缓存—不检查缓存—执行方法→collectPutRequests→cache.put(key, result)
- `condition="#id > 0"` vs `unless="#result == null"`: **condition = 方法执行前判断**(SpEL root=args+target+method)—不满足则跳过缓存检查——直接执行方法不缓存 / **unless = 方法执行后判断**(SpEL root额外有#result)—不满足则跳过缓存存储——方法已执行但结果不缓存

数据流: userService.updateUser(1, "Bob")→CacheInterceptor.invoke→execute→公共execute(L402)→L411 CacheOperationContexts(含@CacheEvict+@CachePut)→私有execute(L435)→L442 processCacheEvicts(beforeInvocation)→@CacheEvict(beforeInvocation=false)→跳过→L446 findCachedValue(无@Cacheable→null)→L583 evaluate→L601 invokeOperation→userService.updateUser(1,"Bob")→执行→L602 unwrapReturnValue→L612 collectPutRequests→@CachePut users key=#result.id→cachePutRequest.apply→cache.put(1, User{1,"Bob"})→L623 processCacheEvicts(afterInvocation)→@CacheEvict afterInvocation→cache.evict(1)→缓存中User{1,"Bob"}被清除→return result→下次getUser(1)→@Cacheable→cache.doGet(1)→null→查DB→取最新值

关键设计: **Why @CachePut 和 @CacheEvict 的执行时机不同？** @CachePut 总是"先执行方法再写缓存"(结果依赖方法返回值)— @CacheEvict 默认"方法成功后清缓存"(beforeInvocation=false)— 两者都走 collectPutRequests/processCacheEvicts 在 evaluate 内 L608/612 与 L623 的固定位置。beforeInvocation=true 的 evict 提前到 L442(方法执行前)— 因为"清空缓存"不依赖方法结果, 提前清可防止方法失败后缓存残留。时机差异由"是否依赖返回值"决定。[模式: 模板方法 — 缓存操作的时机在 execute 骨架中固定]

→ spring-context 第十二域完成。@Cacheable/@CacheEvict/@CachePut 三注解 + execute 模板方法 + CacheManager 策略解耦 — 四层架构。引出 S2-13: @Lazy + @DependsOn + @Primary — 三种 Bean 注册控制策略。
