# OF-6 熔断 — Pass 1 轮廓记录 (入口展开追踪 00 §2)

> 日期: 2026-08-16 | 源码: 4.3.2 (FeignCircuitBreakerInvocationHandler 192 / FeignCircuitBreakerTargeter / FallbackFactory / CircuitBreakerNameResolver)
> 09 域级审计: OPENFEIGN-PLAN OF-6 (接受, 代际修正: 4.3.x 面向 Spring Cloud CircuitBreaker) — 断言 "Targeter 三分支 + 降级" 已 grep 验证

## 入口展开 (Level-1~3, 已读源码)

### Level-1: FeignCircuitBreakerTargeter 三分支 (L40-70)

```
implements Targeter (OF-2 targeter.target 的熔断实现!)
target (L44-70):
├── **非 FeignCircuitBreaker.Builder → feign.target(target)** (L48-49) — 普通路径
├── name = contextId 优先 (getContextId 无则 getName) (L52)
├── **fallback 非 void → targetWithFallback** (L54-56)
└── **fallbackFactory → targetWithFallbackFactory** (L58-60)
    ← 三分支: 普通 / fallback / fallbackFactory (OF-2 分支点兑现)
```

### Level-2: FeignCircuitBreakerInvocationHandler (L40-192)

```
构造 (L57-77):
├── CircuitBreakerFactory (SCC 抽象) + feignClientName + target + dispatch
├── **fallbackMethodMap = toFallbackMethod(dispatch)** (L66) — 方法映射
├── nullableFallbackFactory + circuitBreakerGroupEnabled
└── CircuitBreakerNameResolver (名字解析)
invoke (L120+):
├── **asSupplier 包装** (L135-158): 异步线程上下文传递
│   ├── RequestContextHolder.getRequestAttributes (L136)
│   ├── **isAsync = caller != Thread.currentThread()** (L140)
│   ├── isAsync → setRequestAttributes (L142-143) — 异步线程上下文恢复!
│   └── dispatch.get(method).invoke(args) (L144)
└── 异常 unwrap (L120-130): underlyingException Runtime/IllegalState
```

### Level-3: 降级与名字

```
FallbackFactory (接口): T create(Throwable cause) + **Default 内部类** (constant 兜底)
CircuitBreakerNameResolver (接口): resolveCircuitBreakerName(feignClientName, target, method)
FeignCircuitBreakerDisabledConditions: 开关 (FeignAutoConfiguration L168 引用)
SCC 抽象消费: CircuitBreakerFactory (create/reset) — C-8 交叉
```

## 09 域级审计表 (OPENFEIGN-PLAN OF-6 断言 vs 源码)

| 断言 | grep 证据 | 结论 |
|---|---|---|
| "Targeter 三分支" | L48-60 (target/fallback/fallbackFactory) | 接受 (数字 3 实证) |
| "invoke→resolveCircuitBreakerName→factory.create→降级" | 名字解析 + 降级面 | 接受 (invoke 核心待展开) |
| "FallbackFactory" | create(Throwable) + Default 内部类 | 接受 |
| 补锚: 异步线程上下文传递 | asSupplier isAsync + setRequestAttributes (L135-143) | 补锚 |
| 补锚: contextId 优先命名 | L52 (getContextId 无则 getName) | 补锚 |
| 补锚: 异常 unwrap | L120-130 (underlyingException) | 补锚 |
| 代际修正: 非 Hystrix/Sentinel | Spring Cloud CircuitBreaker (Resilience4J) — PLAN 已记 | 接受 |

## 待展开 (下一层)

1. invoke 核心完整 (factory.create + run + fallback 调用)
2. toFallbackMethod (fallback 方法映射)
3. targetWithFallback/targetWithFallbackFactory 完整 (builder.circuitBreaker 配置)
4. FeignCircuitBreaker.Builder (builder 变体 — OF-2 circuitBreakerFeignBuilder)
5. FeignCircuitBreakerDisabledConditions (开关语义)
