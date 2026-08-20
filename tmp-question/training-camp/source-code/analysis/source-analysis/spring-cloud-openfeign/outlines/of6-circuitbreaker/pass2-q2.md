# OF-6 熔断 — Pass 2 闭环 Q2: invoke 核心 (create/run/降级)

> 核心: FeignCircuitBreakerInvocationHandler.invoke | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 调用怎么进熔断器? 降级函数怎么接?**

## 机制链 (已实证)

```
invoke (L98-125):
├── Object 方法早退 (L79-82: equals 等)
├── **circuitName = circuitBreakerNameResolver.resolveCircuitBreakerName(feignClientName, target, method)** (L99)
├── **circuitBreaker = circuitBreakerGroupEnabled ? factory.create(circuitName, feignClientName)
│   : factory.create(circuitName)** (L101-102) — SCC CircuitBreakerFactory (组名可选)
├── supplier = asSupplier(method, args) (L103) — 主调用 (q3)
├── nullableFallbackFactory != null →
│   ├── **fallbackFunction** (L106-113): throwable → fallbackFactory.create(throwable)
│   │   → fallbackMethodMap.get(method).invoke(fallback, args) — 降级方法调用
│   │   → 异常 unwrapAndRethrow
│   └── **circuitBreaker.run(supplier, fallbackFunction)** (L114) — SCC 熔断执行!
└── fallbackFactory == null → circuitBreaker.run(supplier) (L117?) — 无降级仅熔断
```

## 关键设计 (why)

1. **SCC CircuitBreaker 抽象**: factory.create + circuitBreaker.run — Resilience4J 等实现可换 (C-8 交叉)
2. **组名可选**: circuitBreakerGroupEnabled → create(name, group) — 熔断器分组
3. **降级 = 工厂+方法映射**: fallbackFactory.create(throwable) → fallbackMethodMap 对应方法 — 每方法降级
4. **run(supplier, fallbackFunction)**: 主调用 + 降级函数一体 — 熔断语义完整

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Object 早退 | FeignCircuitBreakerInvocationHandler.java:79-82 |
| resolveCircuitBreakerName | FeignCircuitBreakerInvocationHandler.java:99 |
| create (组名可选) | FeignCircuitBreakerInvocationHandler.java:101-102 |
| fallbackFunction | FeignCircuitBreakerInvocationHandler.java:106-113 |
| run(supplier, fallbackFunction) | FeignCircuitBreakerInvocationHandler.java:114 |

## 负面空间 (Q2 面)

- 不熔断器缓存 (每次调用 create)
- 不降级方法缺省校验 (方法映射失败抛)
- 不做半开策略配置 (SCC 实现面)
