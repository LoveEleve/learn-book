# S2-12 @Cacheable — 缓存抽象 + SpEL key + CacheManager 策略

> 项目: Spring Framework 6.x | 🟡 Working / 1 篇 | 9文件/~2369行
> 基线: S2-10 @Async — @Async 和 @Cacheable 都是 AOP MethodInterceptor 驱动——不同点: 异步编排 vs 缓存抽象

---

## §0.8

- 🟡 Working，1篇 — @EnableCaching→CacheInterceptor→execute模板方法→CacheManager→SpEL key→@Cacheable/@CacheEvict/@CachePut
- 设计模式: [模式: 模板方法]—execute()定义缓存处理骨架(inspectCacheables/inspectBeforeInvocation/inspectAfterInvocation); [模式: 策略模式]—CacheManager/CacheResolver 提供不同缓存后端
- vs @Async: @Cacheable 是同步增强(缓存查询/写入) — @Async 是异步编排

---

## 01 提取

| Source | Step | Inferred Knowledge Point | Confidence |
|--------|:--:|------|------------|
| EnableCaching.java:202行 | @EnableCaching | @Import(CachingConfigurationSelector)→根据mode(PROXY/ASPECTJ)→ProxyCachingConfiguration→@Bean CacheInterceptor→CacheManager→KeyGenerator | High |
| CacheInterceptor.java:50-65 | invoke() | **入口**: MethodInterceptor.invoke→MethodInvocation→execute(aopAllianceInvoker, target, method, args)→父类CacheAspectSupport.execute 模板方法 | High |
| CacheAspectSupport.java:402-435 | execute() | **模板方法核心**: ①解析@Cacheable/@CacheEvict/@CachePut→CacheOperationContexts ②inspectCacheables(cache.put)→返回缓存值(命中) ③inspectBeforeInvocation(evict → beforeInvocation=true) ④invoker.invoke()执行目标方法 ⑤collectPutRequests→cachePutAll ⑥inspectAfterInvocation(evict→afterInvocation) | High |
| CacheAspectSupport.java:inspectCacheables | @Cacheable处理 | 遍历 CacheOperation.@Cacheable → cache.get(key)→命中→return cached / 未命中→继续invoke | High |
| CacheOperationExpressionEvaluator.java:128行 | key SpEL | **SpEL key生成**: #root.method→#root.target→#root.args→条件 #result!=null → result作为缓存值的过滤 | High |
| @Cacheable.java:201行 | @Cacheable注解 | value/cacheNames(201)→key(SpEL)→keyGenerator→cacheManager→cacheResolver→condition(SpEL)→unless(SpEL)→sync | High |
| @CacheEvict.java:157行 | @CacheEvict注解 | allEntries(清空整个cache)→beforeInvocation(方法执行前/后清除)→key→condition | High |
| @CachePut.java:173行 | @CachePut注解 | 总是执行方法→总是缓存结果(不检查缓存) | High |

---

## 02-04 聚合+分类+聚类

### 聚合

**P1 核心 (3):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P1-1 | CacheAspectSupport.execute 模板方法 — inspectCacheables→invoke→collectPutRequests→inspectAfterInvocation | 🔴 | **为什么🔴**: @Cacheable/@CacheEvict/@CachePut 三种注解的执行都经execute()统一处理——这是"缓存抽象"的核心——与CacheManager解耦——任何一个CacheManager(Redis/Caffeine/ConcurrentMap)只需实现Cache接口即可工作 |
| P1-2 | @Cacheable 缓存查询流程 — cache.get(key)→命中→返回 / 未命中→invoke→cache.put | 🔴 | **为什么🔴**: 最常用的缓存注解——理解命中/未命中的分流逻辑——为什么@Cacheable在未命中时也缓存null值(sync=true除外) |
| P1-3 | SpEL key 生成 — CacheOperationExpressionEvaluator + SimpleKeyGenerator | 🔴 | **为什么🔴**: 缓存key如何从方法参数生成——默认SimpleKeyGenerator(param1+param2...)—自定义key=SpEL表达式—错误的key导致缓存穿透或数据错误 |

**P2 支持 (2):**

| # | KP | 🔴🟡🟢 | 为什么 |
|:--:|------|:--:|------|
| P2-1 | @CacheEvict allEntries+ beforeInvocation — 两种清除策略 | 🟡 | **为什么🟡**: 与@Cacheable互补——beforeInvocation=true=先清除后执行(防方法失败后cache仍有效)—allEntries=true=清空整个cache |
| P2-2 | @CachePut — 总是执行方法+总是缓存结果——更新缓存的专用注解 | 🟡 | **为什么🟡**: 与@Cacheable不同——@Cacheable先查缓存——@CachePut总是执行方法并缓存结果——用于"更新缓存而不清空"的场景 |

### 聚类 (1篇)

**1篇理由**: ~2369行/9文件 — 核心在CacheAspectSupport.execute(L402-435, 32行模板方法)+CacheInterceptor(72行)+annotation定义(533行)—主流逻辑在百余行模板方法中。1篇(~47行)覆盖execute模板→三种注解分流→SpEL key→CacheManager解耦。

**单篇结构**: §1 @EnableCaching→CacheInterceptor / execute 模板方法 → §2 @Cacheable 缓存查询+命中/未命中分流+SpEL key → §3 @CacheEvict/@CachePut + 条件(condition/unless) + CacheManager解耦
