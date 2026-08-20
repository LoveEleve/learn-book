# OF-6 熔断 — Pass 2 闭环 Q1: Targeter 三分支

> 核心: FeignCircuitBreakerTargeter | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 熔断 Targeter 怎么三分支? fallback 从哪来?**

## 机制链 (已实证)

```
implements Targeter (OF-2 targeter.target 的熔断实现!)
target (L44-70):
├── **非 FeignCircuitBreaker.Builder → feign.target(target)** (L48-49) — 普通路径 (无熔断)
├── name = **contextId 优先** (getContextId 无则 getName) (L52)
├── **fallback 非 void → targetWithFallback** (L54-56):
│   ├── fallback 实例 = getFromContext("fallback", name, context, fallback类, target.type()) (L71-72)
│   └── builder(feignClientName, builder).target(target, fallbackInstance) (L73)
└── **fallbackFactory → targetWithFallbackFactory** (L58-60):
    ├── factory = getFromContext("fallbackFactory", name, context, 类, FallbackFactory.class) (L65-66)
    └── builder(feignClientName, builder).target(target, fallbackFactory) (L67)
    ← 三分支: 普通 / fallback / fallbackFactory
getFromContext (L75+): 从客户端子上下文获取实例 (OF-7 关联)
```

## 关键设计 (why)

1. **三分支 = 降级方式三态**: 无降级 (普通) / 类降级 (fallback) / 工厂降级 (fallbackFactory) — 语义递增
2. **contextId 优先命名**: 多客户端同 name 时用 contextId 区分 — 熔断器名唯一
3. **fallback 从子上下文获取**: 与客户端配置隔离 (OF-7) — 每客户端独立降级实例
4. **FeignCircuitBreaker.Builder 判定**: 非熔断 Builder 直接透传 — 开关控制 (Builder 变体选择)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| 三分支 | FeignCircuitBreakerTargeter.java:48-60 |
| contextId 优先 | FeignCircuitBreakerTargeter.java:52 |
| targetWithFallback | FeignCircuitBreakerTargeter.java:69-73 |
| targetWithFallbackFactory | FeignCircuitBreakerTargeter.java:63-67 |
| getFromContext | FeignCircuitBreakerTargeter.java:75+ |

## 负面空间 (Q1 面)

- 不双降级 (fallback 与 fallbackFactory 互斥, 优先 fallback)
- 不降级实例缓存 (每次子上下文获取)
- 不自动选降级方式 (注解声明)
