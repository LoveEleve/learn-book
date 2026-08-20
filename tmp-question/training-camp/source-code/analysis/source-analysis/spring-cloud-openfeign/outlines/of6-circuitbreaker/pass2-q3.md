# OF-6 熔断 — Pass 2 闭环 Q3: 异步与降级面

> 核心: asSupplier + FallbackFactory | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 熔断线程异步怎么传上下文? 降级工厂怎么兜底?**

## 机制链 (已实证)

```
asSupplier (L135-158) — 主调用包装:
├── RequestContextHolder.getRequestAttributes (L136) — 保存请求上下文
├── caller = Thread.currentThread() (L137)
└── () -> { (L138):
    ├── **isAsync = caller != Thread.currentThread()** (L140) — 熔断线程切换检测!
    ├── isAsync → **RequestContextHolder.setRequestAttributes(requestAttributes)** (L142-143)
    │   ← 异步线程恢复请求上下文 (TraceId 等)
    ├── dispatch.get(method).invoke(args) (L144) — feign 方法调用
    ├── RuntimeException 原样抛 (L146-147)
    └── Throwable → RuntimeException 包装 (L148-149)

FallbackFactory (接口):
├── T create(Throwable cause) (接口)
└── **Default 内部类** (constant 兜底): 构造传 constant → create 返回 constant (固定降级值)

toFallbackMethod (L165+): dispatch keySet → fallback 方法映射 (降级方法匹配)
```

## 关键设计 (why)

1. **异步线程上下文恢复**: isAsync 检测 + setRequestAttributes — 熔断器线程池执行时 TraceId/请求属性不丢 (生产关键!)
2. **异常语义**: Runtime 原样 / 其他包装 — 降级触发判定准确
3. **FallbackFactory.Default**: constant 兜底 — 固定降级值场景 (简单降级)
4. **方法级降级映射**: toFallbackMethod — 每个业务方法对应降级方法

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| isAsync 检测 | FeignCircuitBreakerInvocationHandler.java:140 |
| setRequestAttributes | FeignCircuitBreakerInvocationHandler.java:142-143 |
| dispatch.invoke | FeignCircuitBreakerInvocationHandler.java:144 |
| FallbackFactory.Default | FallbackFactory.java |
| toFallbackMethod | FeignCircuitBreakerInvocationHandler.java:165+ |

## 负面空间 (Q3 面)

- 不跨线程池属性清洗 (直接 set)
- 不降级值校验 (constant 直接返回)
- 不做异步降级 (降级同步执行)
