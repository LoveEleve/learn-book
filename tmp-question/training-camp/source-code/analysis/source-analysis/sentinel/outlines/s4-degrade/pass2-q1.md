# Pass 2 闭环笔记 Q1: DegradeSlot 与 DefaultCircuitBreakerSlot 的关系

## 验证过程

- `DegradeSlot` 使用 `DegradeRuleManager.getCircuitBreakers(...)`，消费用户显式配置的旧式 `DegradeRule` (`DegradeSlot.java:43-56`)。
- `DefaultCircuitBreakerSlot` 的 SPI order 是 `ORDER_DEFAULT_CIRCUIT_BREAKER_SLOT`，源码注释标注 `@since 2.0.0` (`DefaultCircuitBreakerSlot.java:30-42`)。
- 默认槽先检查 `DegradeRuleManager.hasConfig(resource)`：如果用户已经配置了旧式 degrade rule，默认断路器直接跳过 (`DefaultCircuitBreakerSlot.java:54-58`)。
- 因此两者不是“同一规则重复执行”：
  - 有旧式 `DegradeRule` → `DegradeSlot` 生效，默认槽让路
  - 没有旧式配置 → `DefaultCircuitBreakerRuleManager` 提供默认断路器
- 两个槽的 exit 侧都只在当前 entry 没有 `blockError` 时调用 `circuitBreaker.onRequestComplete(context)`，并且都继续 `fireExit` (`DegradeSlot.java:62-80`, `DefaultCircuitBreakerSlot.java:72-96`)。

## 结论
1.8.9 主路径中，`DegradeSlot` 是现行旧式 degrade rule 的执行入口；`DefaultCircuitBreakerSlot` 是后加入的默认断路器槽，并通过 `hasConfig` 让旧式配置优先，避免同一资源双重熔断。源码注释中的 `@since 2.0.0` 也说明它是兼容/演进层，不应和旧槽混成一个机制。