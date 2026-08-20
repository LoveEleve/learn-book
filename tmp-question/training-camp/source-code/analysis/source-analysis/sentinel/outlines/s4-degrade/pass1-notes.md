# S-4 熔断降级域 — Pass 1 轮廓记录

> 日期: 2026-08-17 | 范围: `slots/block/degrade/` + `circuitbreaker/`

## 核心骨架

- 入口槽: `DegradeSlot` (82)
- 默认槽: `DefaultCircuitBreakerSlot` (96)
- 规则模型/管理: `DegradeRule` / `DegradeRuleManager` / `DefaultCircuitBreakerRuleManager`
- 断路器抽象: `CircuitBreaker` / `AbstractCircuitBreaker`
- 两个实现: `ExceptionCircuitBreaker` / `ResponseTimeCircuitBreaker`
- 状态观察: `CircuitBreakerStateChangeObserver` / `EventObserverRegistry`
- 异常信号: `DegradeException`

## Pass 1 观察

- 规则执行可能有两层：旧式 `DegradeSlot` 与新式 `DefaultCircuitBreakerSlot`，需要确认是否并行、兼容或重复。
- 断路器状态至少包含 CLOSED、OPEN、HALF_OPEN；需要验证状态转换的 CAS 条件与探测请求语义。
- `ExceptionCircuitBreaker` 内部有 `SimpleErrorCounter`；`ResponseTimeCircuitBreaker` 内部有 `SlowRequestCounter`，两者都是滑窗统计的专用包装。
- `AbstractCircuitBreaker` 在 entry 时可能注册 `whenTerminate` 回调，exit 才能完成异常/慢调用统计。
- 规则管理很可能复用 `SentinelProperty + PropertyListener`，但 `DefaultCircuitBreakerRuleManager` 与旧 `DegradeRuleManager` 的职责需分开。

## 测试地图

- `ExceptionCircuitBreakerTest.java`
- `ResponseTimeCircuitBreakerTest.java`
- `CircuitBreakingIntegrationTest.java`
- `DefaultCircuitBreakerSlotTest.java`
- `DegradePartialIntegrationTest.java`
- `DegradeRuleManagerTest.java`
- `DefaultCircuitBreakerRuleManagerTest.java`
- `DegradeRuleTest.java`

## 标记问题

1. `DegradeSlot` 与 `DefaultCircuitBreakerSlot` 谁是 1.8.9 的现行入口?
2. `DegradeSlot` 是否只兼容旧规则，默认槽是否消费新 `CircuitBreaker`?
3. CLOSED → OPEN 的触发条件如何由异常比例/异常数/慢调用比例计算?
4. OPEN → HALF_OPEN 的时间窗口和 CAS 探测如何保证只有一个探测请求?
5. HALF_OPEN 探测成功/失败分别如何回到 CLOSED/OPEN?
6. `ExceptionCircuitBreaker` 的 SimpleErrorCounter 如何统计异常比例与异常数?
7. `ResponseTimeCircuitBreaker` 的 SlowRequestCounter 如何定义慢请求?
8. `whenTerminate` 回调与断路器统计的时序是什么?
9. 规则更新、断路器缓存和旧状态如何同步?
10. `EventObserverRegistry` 的观察者是状态变更前调用还是变更后调用?
