# OF-6 熔断 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.0 (早期) | FeignCircuitBreaker.Builder + FeignCircuitBreakerTargeter 骨架 (三分支) + FallbackFactory |
| 2.1+ | FeignCircuitBreakerInvocationHandler (invoke + asSupplier); CircuitBreakerNameResolver; toFallbackMethod |
| 3.x | circuitBreakerGroupEnabled (组名); 异步线程上下文传递 (isAsync); DisabledConditions 双条件 |
| 4.x | Spring Cloud CircuitBreaker 抽象统一 (Resilience4J); Hystrix/Sentinel 直接集成移除 (代际) |

## 痕迹证据

- FeignCircuitBreaker.java:33: Builder extends Feign.Builder (2.x 锚)
- FeignCircuitBreakerTargeter.java:44-70: 三分支 (2.x 锚)
- FeignCircuitBreakerTargeter.java:52: contextId 优先 (2.1+ 锚)
- FeignCircuitBreakerInvocationHandler.java:99-114: create + run(supplier, fallbackFunction) (2.1+ 锚)
- FeignCircuitBreakerInvocationHandler.java:135-143: asSupplier isAsync + setRequestAttributes (3.x 锚)
- FeignCircuitBreakerDisabledConditions.java:23-45: 双条件 (3.x 锚)

## 推断标注

- "2.x 骨架" — Spring Cloud OpenFeign 公知版本线 (标注)
- "2.1+ invoke" — 类实证 (实证)
- "3.x 异步/开关" — 注释锚实证 (实证)
- "4.x 代际" — OPENFEIGN-PLAN 代际修正记录 (实证)
- **源码浅克隆 (单 commit)**: git log 时空考古受限 — 以注释锚 + 常量实证为主 (降级说明)

## 对照线 (已交付/待交付)

- SCC C-8 CircuitBreaker: factory.create/run 抽象 — 底座对照 (SCC-PLAN 交叉)
- Sentinel (ST-2 熔断): 规则驱动降级 vs OpenFeign 注解驱动 — 熔断对照
- Dubbo D-7: MockCluster 降级 vs Feign fallback — RPC 降级对照
