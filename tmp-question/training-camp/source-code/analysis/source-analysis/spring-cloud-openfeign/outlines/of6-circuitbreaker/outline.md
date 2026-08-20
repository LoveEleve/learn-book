# OF-6 熔断 — 三分支降级与熔断执行

> 前置: [[OF-2-代理创建与装配]] (targeter.target 分支点 + circuitBreakerFeignBuilder) | 引出: [[OF-7-配置隔离]] | 对照: SCC (C-8 CircuitBreaker) + Sentinel 熔断
> 🟡 B | 8 KP | [模式: 策略分支 + 熔断执行 + 降级工厂]
> Pass 2 闭环: q1(Targeter 三分支) q2(invoke 核心) q3(异步降级) q4(配置面)

**读者处境**: fallback/fallbackFactory 怎么让调用进熔断器? 熔断线程异步上下文怎么传? 这篇拆 FeignCircuitBreakerInvocationHandler (192) + Targeter + FallbackFactory。

### 1. Targeter 三分支 — 普通/fallback/fallbackFactory

场景: 降级方式怎么选?
源码路径:
- **target** (L44-70): 非 CircuitBreaker.Builder → feign.target (L48-49) / **name = contextId 优先** (L52) / **fallback → targetWithFallback** (L54-56: 子上下文获取实例 L71-72 + target(target, fallbackInstance)) / **fallbackFactory → targetWithFallbackFactory** (L58-60: getFromContext L65-66 + target(target, factory))
关键设计 (q1): **三分支 = 降级三态 (无/类/工厂) + contextId 命名 + 子上下文获取 (OF-7)**。[模式: 策略面]

### 2. invoke 核心 — create/run/降级函数

场景: 调用怎么进熔断器?
源码路径:
- **invoke** (L98-125): resolveCircuitBreakerName (L99) → **factory.create(name, group)** (L101-102, groupEnabled 可选) → asSupplier → **fallbackFunction** (L106-113: fallbackFactory.create(throwable) → fallbackMethodMap 方法调用) → **circuitBreaker.run(supplier, fallbackFunction)** (L114)
关键设计 (q2): **SCC 抽象 (C-8) + 组名可选 + run 一体 (主调用+降级)**。[模式: 执行面]

### 3. 异步与降级面 — asSupplier + FallbackFactory

场景: 熔断线程上下文? 降级兜底?
源码路径:
- **asSupplier** (L135-158): **isAsync 检测** (L140: caller != currentThread) → **setRequestAttributes** (L142-143 — 异步线程恢复 TraceId!) → dispatch.invoke (L144) → 异常语义 (Runtime 原样/其他包装)
- **FallbackFactory**: create(Throwable) + **Default 内部类** (constant 兜底); **toFallbackMethod** (L165+: 方法级降级映射)
关键设计 (q3): **异步线程上下文恢复 (生产关键) + constant 兜底 + 方法级降级**。[模式: 降级面]

### 4. 配置面 — Builder + 双条件开关

场景: 熔断怎么装配/禁用?
源码路径:
- **FeignCircuitBreaker.Builder extends Feign.Builder** (L33) — OF-2 circuitBreakerFeignBuilder! + 3 配置字段 (Factory/GroupEnabled/NameResolver); ⚠ **build(nullableFallbackFactory) 注入熔断 InvocationHandler** (L92-93: super.invocationHandlerFactory((target, dispatch) -> new FeignCircuitBreakerInvocationHandler(...)) — 熔断代理的注入点)
- **DisabledConditions 双条件** (L23-45): @ConditionalOnMissingClass(CircuitBreaker) 或 enabled=false — 任一满足禁用 (AnyNestedCondition)
- **CircuitBreakerNameResolver**: 名字可自定义; ⚠ **默认双实现** (FeignAutoConfiguration L195/L217-226): **DefaultCircuitBreakerNameResolver** + **AlphanumericCircuitBreakerNameResolver** (**alphanumeric-ids.enabled 开关选择** L); ⚠ **invoke 无降级分支** (L117: run(supplier)) + **unwrapAndRethrow 特判 NoFallbackAvailableException** (L118-122)
- ⚠ **熔断 Bean 装配条件** (FeignAutoConfiguration L180+): @ConditionalOnClass(CircuitBreaker) + **@ConditionalOnProperty(circuitbreaker.enabled=true)** → circuitBreakerFeignBuilder; **getFromContext FactoryBean unwrap** (L75-90: instance null → IllegalStateException "No X instance...found for feign client Z" + factoryBean.getObject())
关键设计 (q4): **Builder 继承扩展 + 双条件降级 (无类/显式关) + 命名策略**。[模式: 配置面]

## 代码类型
Architecture (策略+装饰) + 熔断语义

## 负面空间 (OF-6, 6 条)

| 不做 | 说明 |
|:--|:--|
| 不双降级 | fallback 与 fallbackFactory 互斥 (q1) |
| 不降级实例缓存 | 每次子上下文获取 (q1) |
| 不熔断器缓存 | 每次 create (q2) |
| 不跨线程池属性清洗 | 直接 set (q3) |
| 不 Builder 参数自动发现 | 显式设置 (q4) |
| 不熔断配置热更新 | 创建时定 (q4) |

## 结尾桥 OUTBOUND

- → [[OF-7-配置隔离]]: getFromContext 子上下文 (fallback 实例/工厂)
- → 对照: SCC C-8 CircuitBreaker (factory.create/run 抽象) / Sentinel 熔断 (规则驱动)
