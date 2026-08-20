# OF-6 熔断 — Pass 2 闭环 Q4: 配置面 (Builder + 开关 + 名字)

> 核心: FeignCircuitBreaker.Builder + DisabledConditions | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 熔断 Builder 怎么构建? 什么条件下熔断被禁用? 名字怎么定?**

## 机制链 (已实证)

```
FeignCircuitBreaker.Builder **extends Feign.Builder** (L33) — OF-2 circuitBreakerFeignBuilder!
├── 3 配置字段: CircuitBreakerFactory (L51) / circuitBreakerGroupEnabled (L55) / CircuitBreakerNameResolver (L57)
├── 链式设置: circuitBreakerFactory/GroupEnabled/NameResolver (L59-71)
└── **target(target, fallback) 覆写** (L) — 熔断代理创建入口

FeignCircuitBreakerDisabledConditions (L23-45) — **双条件开关 (AnyNestedCondition 任一满足)**:
├── @ConditionalOnMissingClass(CircuitBreaker) (L29) — 无 SCC 熔断类
└── @ConditionalOnProperty(circuitbreaker.enabled=false) (L34) — 显式关闭
→ 任一满足 → 禁用熔断 (FeignAutoConfiguration L168 消费 → 用普通 Builder)

CircuitBreakerNameResolver (接口):
└── resolveCircuitBreakerName(feignClientName, target, method) — 熔断器名 (可自定义)
```

## 关键设计 (why)

1. **Builder 继承扩展**: extends Feign.Builder — 复用 feign 构建链只加熔断能力 (OF-2 的 circuitBreakerFeignBuilder 选择此变体)
2. **双条件禁用**: 无类 (依赖缺失) 或显式关闭 — 降级到普通 Builder (不用熔断)
3. **名字可自定义**: NameResolver 接口 — 熔断器命名策略 (默认实现?)
4. **工厂注入**: CircuitBreakerFactory 从容器 — SCC 实现可换 (Resilience4J)

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Builder extends Feign.Builder | FeignCircuitBreaker.java:33 |
| 3 配置字段 | FeignCircuitBreaker.java:51-57 |
| 双条件开关 | FeignCircuitBreakerDisabledConditions.java:23-45 |
| NameResolver 接口 | CircuitBreakerNameResolver.java |

## 负面空间 (Q4 面)

- 不 Builder 参数自动发现 (显式设置)
- 不熔断器配置热更新 (创建时定)
- 不默认 NameResolver 语义 (需实现或默认)
